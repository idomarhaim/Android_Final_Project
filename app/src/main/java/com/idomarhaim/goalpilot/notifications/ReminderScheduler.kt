package com.idomarhaim.goalpilot.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_DAY
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_HOUR
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
 * ## What is scheduled, and what is not
 *
 * Scheduled: §2.5's **nightly plan-tomorrow prompt**, at [com.idomarhaim.goalpilot.domain.model.DaySchedule.planningMinutes].
 *
 * ⚠️ **Not scheduled: §2.5's *one reminder per occurrence, timed per rung*, because there are
 * no occurrences.** `Task` carries no due date, §2.2's four rungs exist in no Kotlin file, and
 * §2.1's occurrence model is `C9a` #25 and unbuilt. The arithmetic those reminders need is
 * shipped and tested in [com.idomarhaim.goalpilot.domain.model.ReminderTiming]; the loop that
 * walks a list of occurrences calling it belongs to the ticket that creates the list. Writing
 * that loop here would have meant inventing the occurrence model in passing, on a ticket
 * reviewed for notifications.
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
}
