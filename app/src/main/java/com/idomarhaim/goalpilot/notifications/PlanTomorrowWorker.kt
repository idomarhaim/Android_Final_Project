package com.idomarhaim.goalpilot.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime

/**
 * §2.5's nightly *plan tomorrow* prompt.
 *
 * ## The re-check at fire time
 *
 * §2.5: *"A reminder re-checks at fire time whether it is still needed — free, precisely
 * because nothing is stored."* For this reminder the question that can have changed between
 * enqueue and wake is **the time itself**: the user may have moved *Awake between* or pinned
 * *Plan tomorrow at* in the hours since. A run that woke for 22:00 when the setting now says
 * 20:00 is a stale run, and posting from it would deliver the prompt at a time the user has
 * explicitly changed away from.
 *
 * So the worker compares the minute it was enqueued for against the setting's current value.
 * Mismatch → **post nothing** and enqueue the correct next run. That is the whole re-check, and
 * it is free in exactly the sense §2.5 means: the answer is derived from the live setting, so
 * there is no second copy of the schedule to go stale.
 *
 * ## Why Hilt reaches it this way
 *
 * [NotificationEntryPoint] rather than `@HiltWorker` — the reasoning is on that interface.
 */
class PlanTomorrowWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors
            .fromApplication(applicationContext, NotificationEntryPoint::class.java)
        val currentMinute = deps.preferences().daySchedule.value.planningMinutes
        val scheduledFor = inputData.getInt(ReminderScheduler.KEY_SCHEDULED_FOR_MINUTE, -1)

        // The re-check. A stale run is not a failure -- Result.retry() would re-run it on
        // WorkManager's backoff, which is the wrong clock entirely -- so it succeeds having
        // done nothing but put the correct run in place.
        val stillNeeded = scheduledFor == currentMinute
        if (stillNeeded) deps.notifier().notifyPlanTomorrow()

        // Re-arm from the clock this run actually woke at, so the chain tracks wall time
        // across DST rather than drifting 24 h from whenever it was first enqueued.
        ReminderScheduler.schedulePlanTomorrow(
            context = applicationContext,
            planningMinutes = currentMinute,
            now = LocalDateTime.now(),
        )
        return Result.success()
    }
}
