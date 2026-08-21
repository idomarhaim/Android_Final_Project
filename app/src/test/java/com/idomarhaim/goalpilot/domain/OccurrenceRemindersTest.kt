package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.ReminderTiming
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.usecase.OccurrenceReminders
import com.idomarhaim.goalpilot.domain.usecase.ReminderDecision
import com.idomarhaim.goalpilot.domain.usecase.SkipReason
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §2.5's reminders: **one per occurrence, timed per rung**, the deadline's computed backwards
 * from how long the work takes and clamped to waking hours (`#56`).
 *
 * ### The brief's named defect is asserted directly
 *
 * *"using `ReminderTiming`'s existing arithmetic rather than a second implementation. If you
 * write a second one, that is the defect."* So the deadline cases below do not merely assert a
 * time — they assert **the same value [ReminderTiming.plan] returns**, computed independently
 * in the test. A second implementation inside `OccurrenceReminders` would have to reproduce
 * `#8`'s clamp exactly to pass, and the first divergence fails here.
 */
class OccurrenceRemindersTest {

    private val day: LocalDate = LocalDate.of(2026, 8, 21)
    private val schedule = DaySchedule.DEFAULT
    private val waking: WakingHours = schedule.waking

    private fun task(
        id: String = "t1",
        minutes: Int? = 240,
        source: DurationSource = DurationSource.AI,
        occurrence: com.idomarhaim.goalpilot.domain.model.Occurrence? = null,
        done: Boolean = false,
    ) = Task(
        id = id,
        title = "Write the report",
        estimatedMinutes = minutes,
        durationSource = source,
        occurrence = occurrence,
        completion = if (done) CompletionFact(completedAtEpochMillis = 1L, minutes = 30) else null,
    )

    // ── §2.5's differentiator: computed backwards, clamped, and saying why ─────────────────

    @Test
    fun `the deadline reminder is computed backwards from how long the work takes`() {
        // Due at 14:00, four hours of work: the moment you would have to START is 10:00, and
        // 10:00 is inside 07:00-23:00, so nothing is clamped.
        val plan = OccurrenceReminders.planFor(
            occurrence = Deadline(day.atTime(14, 0)),
            taskMinutes = 240,
            waking = waking,
        )

        assertThat(plan.idealAt).isEqualTo(day.atTime(10, 0))
        assertThat(plan.fireAt).isEqualTo(day.atTime(10, 0))
        assertThat(plan.movedForSleep).isFalse()
    }

    @Test
    fun `spec section 2 point 5's own example lands on the previous evening and says why it moved`() {
        // §2.5, verbatim: "due at 06:00 and it takes about 4 hours -- worth starting tonight."
        // Ideal start is 02:00, which is asleep, so it moves EARLIER to the last waking minute
        // before it: 22:59 the previous evening. Later would be a reminder about a deadline
        // already lost.
        val plan = OccurrenceReminders.planFor(
            occurrence = Deadline(day.atTime(6, 0)),
            taskMinutes = 240,
            waking = waking,
        )

        assertThat(plan.idealAt).isEqualTo(day.atTime(2, 0))
        assertThat(plan.fireAt).isEqualTo(day.minusDays(1).atTime(22, 59))
        // The flag IS "why it moved" -- the notification copy branches on it, and without it
        // the reminder can say THAT it moved but never from where.
        assertThat(plan.movedForSleep).isTrue()
        assertThat(plan.idealAt.isAfter(plan.fireAt)).isTrue()
    }

    @Test
    fun `the deadline reminder is exactly what ReminderTiming would return, not a second implementation`() {
        val deadline = Deadline(day.atTime(6, 0))
        val minutes = 240

        val throughUseCase = OccurrenceReminders.planFor(deadline, minutes, waking)
        val throughEightsArithmetic = ReminderTiming.plan(
            dueAt = deadline.at,
            durationMinutes = minutes,
            waking = waking,
        )

        // The brief's named defect, asserted as an identity rather than as a coincidence of
        // two numbers that happen to agree today.
        assertThat(throughUseCase).isEqualTo(throughEightsArithmetic)
    }

    @Test
    fun `a night-shift waking span clamps into the hours that user is actually awake`() {
        // 22:00-06:00. A deadline at 12:00 taking an hour would ideally start at 11:00, which
        // is when this user is asleep, so it moves back to 05:59 -- the last minute of their
        // waking span. A clamp that assumed a daytime span would remind them in their sleep.
        val nightShift = WakingHours(startMinutes = 22 * 60, endMinutes = 6 * 60)

        val plan = OccurrenceReminders.planFor(Deadline(day.atTime(12, 0)), 60, nightShift)

        assertThat(plan.idealAt).isEqualTo(day.atTime(11, 0))
        assertThat(plan.fireAt).isEqualTo(day.atTime(5, 59))
        assertThat(plan.movedForSleep).isTrue()
    }

    // ── #9: a typed duration drives the reminder, end to end ───────────────────────────────

    @Test
    fun `a typed duration drives the reminder, and a shorter estimate cannot move it`() {
        // #9's stickiness, seen from the reminder's end, on a deadline chosen so that the two
        // numbers land on DIFFERENT SIDES of sleep. Due at 09:00: four hours of typed work has
        // to start at 05:00, which is asleep, so the reminder moves to the previous evening --
        // §2.5's "worth starting tonight". The model's half hour would have started at 08:30,
        // the same morning. So if anything downstream ever prefers the estimate, the user is
        // told about a four-hour job ninety minutes before it is due.
        //
        // ⚠️ The obvious pairing -- §2.5's own 06:00 example -- is VACUOUS here, and was
        // written that way first: 06:00 minus 30 minutes is 05:30, which is also asleep, so
        // both durations clamp to the same 22:59 and the test passes whichever number wins.
        val deadline = Deadline(day.atTime(9, 0))
        val typed = task(minutes = 240, source = DurationSource.USER, occurrence = deadline)
        val estimated = task(minutes = 30, source = DurationSource.AI, occurrence = deadline)

        val fromTyped = OccurrenceReminders.planFor(typed, schedule)!!
        val fromEstimate = OccurrenceReminders.planFor(estimated, schedule)!!

        assertThat(fromTyped.durationMinutes).isEqualTo(240)
        assertThat(fromTyped.idealAt).isEqualTo(day.atTime(5, 0))
        assertThat(fromTyped.fireAt).isEqualTo(day.minusDays(1).atTime(22, 59))
        assertThat(fromTyped.movedForSleep).isTrue()

        // The two genuinely differ, so the assertion above is not vacuous: the reminder really
        // is a function of WHICH number won, and of nothing else.
        assertThat(fromEstimate.durationMinutes).isEqualTo(30)
        assertThat(fromEstimate.fireAt).isEqualTo(day.atTime(8, 30))
        assertThat(fromTyped.fireAt).isNotEqualTo(fromEstimate.fireAt)
    }

    @Test
    fun `a task with no duration at all is reminded on the default half hour, not on zero`() {
        val noDuration = task(minutes = null, occurrence = Deadline(day.atTime(12, 0)))

        // `TaskDuration.minutesOf` supplies DEFAULT_MINUTES rather than dropping the task, so
        // the reminder is 30 minutes before rather than at the deadline itself.
        assertThat(OccurrenceReminders.planFor(noDuration, schedule)!!.fireAt)
            .isEqualTo(day.atTime(11, 30))
    }

    // ── Timed per rung: the other three ────────────────────────────────────────────────────

    @Test
    fun `an all-day reminds at the first waking minute of its day, never at midnight`() {
        val plan = OccurrenceReminders.planFor(AllDay(day), taskMinutes = 240, waking = waking)

        // Its window opens at 00:00, and clamping that BACKWARDS -- which is what the deadline
        // arithmetic does -- would remind the previous evening about a day that has not begun.
        assertThat(plan.fireAt).isEqualTo(day.atTime(7, 0))
        assertThat(plan.movedForSleep).isFalse()
    }

    @Test
    fun `an all-day ignores how long the task takes`() {
        val short = OccurrenceReminders.planFor(AllDay(day), taskMinutes = 5, waking = waking)
        val long = OccurrenceReminders.planFor(AllDay(day), taskMinutes = 480, waking = waking)

        // §2.5's backward computation is the DEADLINE's, because a deadline is the only rung
        // that says when you must be finished without saying when to start.
        assertThat(short.fireAt).isEqualTo(long.fireAt)
    }

    @Test
    fun `a block reminds at the start of its slot`() {
        val plan = OccurrenceReminders.planFor(
            occurrence = Block(day.atTime(9, 0), day.atTime(11, 0)),
            taskMinutes = 240,
            waking = waking,
        )

        // The slot already holds the work, so there is nothing to compute backwards from.
        assertThat(plan.fireAt).isEqualTo(day.atTime(9, 0))
    }

    @Test
    fun `a block starting while the user is asleep is pulled back into waking hours`() {
        val plan = OccurrenceReminders.planFor(
            occurrence = Block(day.atTime(3, 0), day.atTime(5, 0)),
            taskMinutes = 60,
            waking = waking,
        )

        // Every rung goes through the same clamp, which is the point of routing them all
        // through `ReminderTiming.plan` with a zero lead-in rather than branching around it.
        assertThat(plan.fireAt).isEqualTo(day.minusDays(1).atTime(22, 59))
        assertThat(plan.movedForSleep).isTrue()
    }

    @Test
    fun `a span reminds on the first waking minute of its first day`() {
        val plan = OccurrenceReminders.planFor(
            occurrence = Span(from = day, to = day.plusDays(6)),
            taskMinutes = 240,
            waking = waking,
        )

        assertThat(plan.fireAt).isEqualTo(day.atTime(7, 0))
    }

    @Test
    fun `a task with no occurrence has no reminder`() {
        assertThat(OccurrenceReminders.planFor(task(occurrence = null), schedule)).isNull()
    }

    @Test
    fun `a completed task has no reminder even though its occurrence is still ahead`() {
        val done = task(occurrence = Deadline(day.atTime(14, 0)), done = true)

        // Returning a plan the caller has to remember to discard is how a reminder for a
        // finished task eventually fires.
        assertThat(OccurrenceReminders.planFor(done, schedule)).isNull()
    }

    // ── §2.5's re-check at fire time ───────────────────────────────────────────────────────

    private fun armedPlanFor(t: Task): LocalDateTime =
        OccurrenceReminders.planFor(t, schedule)!!.fireAt

    @Test
    fun `a reminder fires when the task is still open and nothing has moved`() {
        val open = task(occurrence = Deadline(day.atTime(14, 0)))

        val decision = OccurrenceReminders.decideAtFireTime(open, armedPlanFor(open), schedule)

        assertThat(decision).isInstanceOf(ReminderDecision.Fire::class.java)
        assertThat((decision as ReminderDecision.Fire).occurrence).isEqualTo(open.occurrence)
    }

    @Test
    fun `a reminder does not fire for a task that has since been completed`() {
        val open = task(occurrence = Deadline(day.atTime(14, 0)))
        val armedFor = armedPlanFor(open)

        // The task is ticked between arming the work and waking from it. This is §2.5's
        // re-check in its commonest form, and the whole reason it is free: nothing was stored,
        // so the answer is simply re-read.
        val done = open.copy(completion = CompletionFact(completedAtEpochMillis = 1L, minutes = 240))

        val decision = OccurrenceReminders.decideAtFireTime(done, armedFor, schedule)

        assertThat(decision).isEqualTo(ReminderDecision.Skip(SkipReason.TASK_DONE))
    }

    @Test
    fun `a reminder does not fire for a task deleted since it was armed`() {
        val armedFor = day.atTime(10, 0)

        assertThat(OccurrenceReminders.decideAtFireTime(null, armedFor, schedule))
            .isEqualTo(ReminderDecision.Skip(SkipReason.TASK_GONE))
    }

    @Test
    fun `a reminder does not fire once the occurrence has been taken off the task`() {
        val open = task(occurrence = Deadline(day.atTime(14, 0)))
        val armedFor = armedPlanFor(open)

        val unscheduled = open.copy(occurrence = null)

        assertThat(OccurrenceReminders.decideAtFireTime(unscheduled, armedFor, schedule))
            .isEqualTo(ReminderDecision.Skip(SkipReason.NO_OCCURRENCE))
    }

    @Test
    fun `a run armed before the waking hours moved re-arms instead of posting at the old time`() {
        val open = task(minutes = 240, occurrence = Deadline(day.atTime(6, 0)))
        val armedFor = armedPlanFor(open)
        assertThat(armedFor).isEqualTo(day.minusDays(1).atTime(22, 59))

        // The user moves *Awake between* to 05:00-21:00 after the reminder was armed. Its ideal
        // start of 02:00 is still asleep, but the last waking minute before it is now 20:59.
        val moved = DaySchedule(waking = WakingHours(startMinutes = 5 * 60, endMinutes = 21 * 60))

        val decision = OccurrenceReminders.decideAtFireTime(open, armedFor, moved)

        assertThat(decision).isInstanceOf(ReminderDecision.Rearm::class.java)
        assertThat((decision as ReminderDecision.Rearm).plan.fireAt)
            .isEqualTo(day.minusDays(1).atTime(20, 59))
    }

    @Test
    fun `a run armed before the duration was retyped re-arms rather than posting early`() {
        val estimated = task(minutes = 30, occurrence = Deadline(day.atTime(14, 0)))
        val armedFor = armedPlanFor(estimated)
        assertThat(armedFor).isEqualTo(day.atTime(13, 30))

        // The user types four hours. The reminder they need is at 10:00, and a run that woke
        // at 13:30 and posted anyway would be three and a half hours too late to act on.
        val retyped = estimated.copy(estimatedMinutes = 240, durationSource = DurationSource.USER)

        val decision = OccurrenceReminders.decideAtFireTime(retyped, armedFor, schedule)

        assertThat(decision).isInstanceOf(ReminderDecision.Rearm::class.java)
        assertThat((decision as ReminderDecision.Rearm).plan.fireAt).isEqualTo(day.atTime(10, 0))
    }
}
