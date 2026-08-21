package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_DAY
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.ReminderPlan
import com.idomarhaim.goalpilot.domain.model.ReminderTiming
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.WakingHours
import java.time.LocalDateTime

/**
 * §2.5's **one reminder per occurrence, timed per rung** — the loop `#8` could not write
 * because there were no occurrences ([#56](https://github.com/idomarhaim/Android_Final_Project/issues/56)).
 *
 * ### There is exactly one piece of reminder arithmetic in this app, and it is not here
 *
 * Every rung below goes through [ReminderTiming.plan]. That was `#8`'s deliverable — the
 * backward computation, the waking clamp, and [ReminderPlan.movedForSleep] carrying *why it
 * moved* — and a second implementation of any of it would be the defect this ticket was
 * warned about, not a convenience. What each rung supplies is only **two numbers**: the moment
 * to time against, and how much lead-in it needs.
 *
 * | Rung | Timed against | Lead-in | Why |
 * |---|---|---|---|
 * | [Deadline] | the deadline itself | `TaskDuration.minutesOf(task)` | §2.5's differentiator: remind at the moment you would have to **start** |
 * | [Block] | the slot's start | none | the slot already holds the work; the reminder lands on it |
 * | [AllDay] | that day's first waking minute | none | its window opens at midnight, and a reminder at 00:00 is one nobody sees |
 * | [Span] | the first day's first waking minute | none | as [AllDay]; a span is days, not hours |
 *
 * **A lead-in of none is `plan(dueAt = moment, durationMinutes = 0)`**, which
 * [ReminderTiming.plan] answers with `ideal == dueAt`, still clamped into waking hours. So
 * three of the four rungs are the fourth with a zero, rather than a branch that skips the
 * arithmetic — and the clamp, which is the part that is easy to forget, cannot be forgotten
 * for any of them.
 *
 * ### The re-check at fire time is a pure function, and that is deliberate
 *
 * §2.5: *"A reminder re-checks at fire time whether it is still needed — free, precisely
 * because nothing is stored."* [decideAtFireTime] is that re-check, and it takes the task, the
 * setting and the clock as arguments so the whole of it is exercised on the JVM. The worker
 * around it does two things: read those three, and post or not. A re-check living inside the
 * worker would be a rule that can only be tested on a device at a wall-clock time.
 */
object OccurrenceReminders {

    /**
     * When to remind about [task], or `null` when there is nothing to remind about.
     *
     * `null` for a task with no occurrence and for one already done — a completed task's
     * reminder is not *late*, it is **not wanted**, and returning a plan the caller has to
     * remember to discard is how it eventually fires.
     */
    fun planFor(task: Task, schedule: DaySchedule): ReminderPlan? {
        if (task.isDone) return null
        val occurrence = task.occurrence ?: return null
        return planFor(occurrence, TaskDuration.minutesOf(task), schedule.waking)
    }

    /**
     * The plan for one [occurrence], given how long its task takes.
     *
     * [taskMinutes] is only read for [Deadline]. That is not an optimisation — it is §2.5:
     * the backward computation is *the deadline's*, because a deadline is the only rung that
     * says when you must be **finished** without saying when to start. Every other rung
     * already carries its own start.
     */
    fun planFor(
        occurrence: Occurrence,
        taskMinutes: Int,
        waking: WakingHours,
    ): ReminderPlan = when (occurrence) {
        is Deadline -> ReminderTiming.plan(
            dueAt = occurrence.at,
            durationMinutes = taskMinutes,
            waking = waking,
        )

        is Block -> ReminderTiming.plan(
            dueAt = occurrence.start,
            durationMinutes = 0,
            waking = waking,
        )

        is AllDay, is Span -> ReminderTiming.plan(
            dueAt = firstWakingMinuteOn(occurrence.remindAgainst, waking),
            durationMinutes = 0,
            waking = waking,
        )
    }

    /**
     * §2.5's re-check, run when the reminder wakes: **is this still the reminder to post?**
     *
     * Four answers, and each is a thing that can genuinely have changed between arming the
     * work and waking from it. Nothing here consults a stored schedule — the whole decision is
     * recomputed from the live task and the live setting, which is the sense in which §2.5
     * calls it free.
     *
     * @param task the task as it stands **now**, or `null` if it has been deleted.
     * @param scheduledFor the [ReminderPlan.fireAt] this run was enqueued for.
     */
    fun decideAtFireTime(
        task: Task?,
        scheduledFor: LocalDateTime,
        schedule: DaySchedule,
    ): ReminderDecision {
        if (task == null) return ReminderDecision.Skip(SkipReason.TASK_GONE)
        // Read before `occurrence`, because a done task with an occurrence and a done task
        // without one are the same non-event and should report the same reason.
        if (task.isDone) return ReminderDecision.Skip(SkipReason.TASK_DONE)
        val occurrence = task.occurrence ?: return ReminderDecision.Skip(SkipReason.NO_OCCURRENCE)

        val plan = planFor(occurrence, TaskDuration.minutesOf(task), schedule.waking)
        // The same staleness test `PlanTomorrowWorker` makes against the planning minute, and
        // for the same reason: the user may have moved *Awake between*, retyped the duration,
        // or moved the occurrence itself since this run was armed. Posting from a stale run
        // would deliver a reminder at a time the app no longer believes in.
        if (plan.fireAt != scheduledFor) return ReminderDecision.Rearm(plan)

        return ReminderDecision.Fire(plan, occurrence)
    }

    /**
     * The first minute of [waking] on the calendar day of [midnight].
     *
     * Forward from midnight, never backward — which is why this exists rather than another
     * call to [ReminderTiming.plan]. That function clamps a sleeping moment to the last waking
     * minute **at or before** it, correctly, because its input is the moment you must start;
     * midnight on the day of an all-day task is not that, and clamping it backwards would
     * remind the previous evening about a day that has not begun.
     *
     * A **zero-length** waking span leaves the moment where it is, which is the same answer
     * [ReminderTiming.plan] gives it: a user who is awake for zero minutes a day cannot be
     * reminded inside waking hours, and firing at the honest time beats not firing.
     */
    private fun firstWakingMinuteOn(midnight: LocalDateTime, waking: WakingHours): LocalDateTime {
        if (waking.lengthMinutes <= 0) return midnight
        return midnight.toLocalDate()
            .atStartOfDay()
            .plusMinutes(Math.floorMod(waking.startMinutes, MINUTES_PER_DAY).toLong())
    }
}

/**
 * What the worker should do when a reminder wakes — the outcome of [OccurrenceReminders.decideAtFireTime].
 *
 * A closed set rather than a nullable plan, because *"do not post"* has three different
 * consequences: two of them mean the work is finished and one means it has to be re-armed,
 * and a `null` return would have made the worker guess which.
 */
sealed interface ReminderDecision {

    /** Post it. [occurrence] is carried so the copy can be worded per rung. */
    data class Fire(val plan: ReminderPlan, val occurrence: Occurrence) : ReminderDecision {
        val rung: OccurrenceRung get() = occurrence.rung
    }

    /**
     * Post nothing, and enqueue [plan] instead — the reminder still exists, at a different
     * time. Not a failure and not a retry: WorkManager's backoff is the wrong clock entirely,
     * exactly as `PlanTomorrowWorker` records for the nightly prompt.
     */
    data class Rearm(val plan: ReminderPlan) : ReminderDecision

    /** Post nothing, and there is nothing left to arm. */
    data class Skip(val reason: SkipReason) : ReminderDecision
}

/**
 * Why a reminder said nothing. Distinct constants because they are distinct facts about the
 * user's data, and a single `false` would make a reminder that never fires indistinguishable
 * from one that fired correctly and was not needed.
 */
enum class SkipReason {
    /** The task was deleted between arming and waking. */
    TASK_GONE,

    /** It is done. §2.5's re-check in its most common form. */
    TASK_DONE,

    /** Its occurrence was removed — the task still exists, but it is no longer *when* anything. */
    NO_OCCURRENCE,
}
