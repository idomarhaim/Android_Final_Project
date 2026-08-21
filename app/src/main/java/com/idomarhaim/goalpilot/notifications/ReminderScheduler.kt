package com.idomarhaim.goalpilot.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_DAY
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_HOUR
import com.idomarhaim.goalpilot.domain.model.ReminderPlan
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.OccurrenceReminders
import java.time.Duration
import java.time.LocalDateTime

/**
 * #8 piece 6 — **local scheduling**, and the choice of mechanism.
 *
 * ## WorkManager, not AlarmManager
 *
 * §2.7 settles the wider question first: `GoogleAuthUtil` mints only short-lived tokens with no
 * refresh token and `C9d` banned the service account, so *"there is no credential for a
 * background sync and cannot be one"*. Nothing server-pushed is available to this app at any
 * price. Every reminder is scheduled on the device or it does not happen.
 *
 * Given that, four reasons for WorkManager:
 *
 *  1. **Exactness costs a permission this app cannot honestly claim.** From Android 14 an exact
 *     alarm needs `SCHEDULE_EXACT_ALARM` (user-revocable, and the system may deny it) or
 *     `USE_EXACT_ALARM`, which is restricted to alarm-clock and calendar apps and asserts to
 *     Play that this *is* one. A nightly *plan tomorrow* prompt is not an alarm clock.
 *  2. **Nothing scheduled here needs the minute.** §2.5's nightly prompt is a nudge at about
 *     22:00. The one thing that would want exactness — a deadline reminder computed to the
 *     minute — has nothing to schedule against yet (see the note below).
 *  3. **Reboot and process death are free.** WorkManager restores its queue itself; an
 *     `AlarmManager` schedule is lost on reboot and needs a `RECEIVE_BOOT_COMPLETED` receiver
 *     of our own to rebuild, which is a second thing to keep correct.
 *  4. **Doze already governs both.** `setExactAndAllowWhileIdle` is rate-limited in Doze
 *     regardless, so the precision an exact alarm nominally buys is not reliably there anyway.
 *
 * ## What is scheduled
 *
 * Two things, and they are different shapes:
 *
 *  1. §2.5's **nightly plan-tomorrow prompt**, at
 *     [com.idomarhaim.goalpilot.domain.model.DaySchedule.planningMinutes] — one chain, re-armed
 *     by each run from the clock it woke at.
 *  2. §2.5's **one reminder per occurrence, timed per rung** (`#56`) — one unique work item per
 *     task, named by task id, replaced whenever that task's *when* or duration moves.
 *
 * ⚠️ **This KDoc said the second was impossible until `#56`, and it was right at the time**:
 * `Task` carried no due date and §2.2's rungs existed in no Kotlin file, so a loop over
 * occurrences would have meant inventing the occurrence model in passing on a ticket reviewed
 * for notifications. `#56` built the model; [syncOccurrenceReminders] is that loop, and the
 * arithmetic it calls is still `#8`'s [com.idomarhaim.goalpilot.domain.model.ReminderTiming],
 * untouched.
 *
 * ## Nothing already past is ever armed, and that is §2.5's rule rather than an optimisation
 *
 * A reminder whose moment has gone is not late — it is a **push saying you failed**, which
 * §2.5 forbids in as many words. What has already lapsed meets the user *once*, in the daily
 * review on app open (`DailyMissReview`). So [syncOccurrenceReminders] arms only what is still
 * ahead, and a task whose deadline passed while the phone was off simply appears in tomorrow's
 * review.
 */
object ReminderScheduler {

    /** Unique work name, so a reschedule replaces rather than stacks. */
    const val PLAN_TOMORROW_WORK = "gp_plan_tomorrow"

    /**
     * The minute-of-day this run was enqueued for, carried in the worker's input data.
     *
     * It exists so the worker can perform §2.5's *"re-checks at fire time whether it is still
     * needed"*: comparing this against the setting's current value is how a run enqueued before
     * the user moved *Plan tomorrow at* discovers that it is the stale one.
     */
    const val KEY_SCHEDULED_FOR_MINUTE = "scheduled_for_minute"

    /**
     * Schedules — or reschedules — the nightly prompt for [planningMinutes].
     *
     * [ExistingWorkPolicy.REPLACE] rather than `KEEP`: this is called on every app start *and*
     * whenever *Your day* changes, and `KEEP` would mean a settings change never takes effect
     * until the pending run fires at the old time.
     *
     * A chain of one-time requests rather than [androidx.work.PeriodicWorkRequest], for one
     * reason that matters: a periodic request's period is measured from when it was enqueued,
     * so it cannot be pinned to a wall-clock time, and it would drift off 22:00 across a DST
     * change. Each run enqueues the next from the clock it actually woke at.
     */
    fun schedulePlanTomorrow(
        context: Context,
        planningMinutes: Int,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val fireAt = nextOccurrence(now, planningMinutes)
        val delay = Duration.between(now, fireAt)
        WorkManager.getInstance(context).enqueueUniqueWork(
            PLAN_TOMORROW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PlanTomorrowWorker>()
                .setInitialDelay(delay)
                .setInputData(workDataOf(KEY_SCHEDULED_FOR_MINUTE to planningMinutes))
                .build(),
        )
    }

    /**
     * The next wall-clock moment at [minuteOfDay], strictly after [now].
     *
     * **Strictly** after: firing at exactly `now` would let a worker that woke on time enqueue
     * its successor with a zero delay and run again immediately, once per second, for a day.
     * The re-enqueue at the end of a run is precisely the caller that hits this boundary.
     *
     * Pure, and on `java.time` rather than millis, so a DST jump moves the reminder with the
     * clock instead of holding it 24 h from the last one.
     */
    fun nextOccurrence(now: LocalDateTime, minuteOfDay: Int): LocalDateTime {
        val target = Math.floorMod(minuteOfDay, MINUTES_PER_DAY)
        val today = now.toLocalDate()
            .atStartOfDay()
            .plusHours((target / MINUTES_PER_HOUR).toLong())
            .plusMinutes((target % MINUTES_PER_HOUR).toLong())
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /**
     * Brings the armed reminders into line with [tasks] — §2.5's *one reminder per occurrence*.
     *
     * Called whenever the task list or *Your day* changes, which is often, so it must be
     * **idempotent and cheap**: every arm is an `enqueueUniqueWork(REPLACE)` under a name
     * derived from the task id, so re-running it with an unchanged list replaces each pending
     * request with an identical one rather than stacking duplicates.
     *
     * Three groups, and the second is the one that is easy to forget:
     *
     *  1. A task with a **future** reminder is armed.
     *  2. A task with **no reminder any more** — done, or its occurrence removed — has its work
     *     cancelled *and* its notification taken down. Cancelling only the work would leave a
     *     reminder for a ticked task sitting in the shade until the user swiped it away.
     *  3. A task whose reminder is **already past** is left alone: see this object's KDoc.
     *
     * ⚠️ It cancels only for tasks it can **see**. A task deleted from another device is not in
     * [tasks] at all, so nothing here knows to cancel its work; that run wakes, finds no task,
     * and takes itself down through [ReminderDecision.Skip] — which is why the worker's
     * `TASK_GONE` branch cancels the notification rather than merely returning.
     */
    fun syncOccurrenceReminders(
        context: Context,
        tasks: List<Task>,
        schedule: DaySchedule,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        tasks.forEach { task ->
            if (task.id.isBlank()) return@forEach
            val plan = OccurrenceReminders.planFor(task, schedule)
            when {
                plan == null || plan.isPast(now) -> cancelOccurrenceReminder(context, task.id)
                else -> scheduleOccurrenceReminder(context, task.id, plan, now)
            }
        }
    }

    /**
     * Arms one task's reminder for [ReminderPlan.fireAt].
     *
     * The fire time travels in the input data as ISO-8601 text, and the worker compares it
     * against a freshly computed plan. That comparison **is** §2.5's re-check for the *timing*
     * half: a run armed before the user moved *Awake between* wakes holding the old minute,
     * sees it no longer matches, and re-arms instead of posting.
     */
    fun scheduleOccurrenceReminder(
        context: Context,
        taskId: String,
        plan: ReminderPlan,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        // Never negative: WorkManager treats a negative initial delay as zero anyway, and
        // coercing here means the one caller that can hand us a past plan (the worker's own
        // re-arm, if the recomputed time has since gone by) runs immediately and then decides
        // properly, rather than silently arming for the epoch.
        val delay = Duration.between(now, plan.fireAt).coerceAtLeast(Duration.ZERO)
        WorkManager.getInstance(context).enqueueUniqueWork(
            occurrenceWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<OccurrenceReminderWorker>()
                .setInitialDelay(delay)
                .setInputData(
                    workDataOf(
                        KEY_TASK_ID to taskId,
                        KEY_SCHEDULED_FOR to plan.fireAt.toString(),
                    ),
                )
                .build(),
        )
    }

    /** Disarms a task's reminder and removes any notification it already posted. */
    fun cancelOccurrenceReminder(context: Context, taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(occurrenceWorkName(taskId))
    }

    /**
     * The unique work name for a task's reminder.
     *
     * Derived from the document id rather than counted, so it is the same name on every
     * process start and a reschedule genuinely replaces the pending run. `internal` because
     * the tests assert on it: a name that silently changed shape would leave orphaned work
     * armed forever, and nothing else would notice.
     */
    internal fun occurrenceWorkName(taskId: String): String = OCCURRENCE_WORK_PREFIX + taskId

    /** Prefix for [occurrenceWorkName], distinct from [PLAN_TOMORROW_WORK]. */
    private const val OCCURRENCE_WORK_PREFIX = "gp_occurrence_"

    /** The task whose reminder this run is for. */
    const val KEY_TASK_ID = "task_id"

    /**
     * The [ReminderPlan.fireAt] this run was armed for, ISO-8601.
     *
     * The occurrence half of [KEY_SCHEDULED_FOR_MINUTE], and a full local date-time rather
     * than a minute-of-day because a reminder is armed for a *date*, not for a time of day
     * that recurs.
     */
    const val KEY_SCHEDULED_FOR = "scheduled_for"
}
