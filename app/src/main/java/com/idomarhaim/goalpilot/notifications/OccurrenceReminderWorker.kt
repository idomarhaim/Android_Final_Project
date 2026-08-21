package com.idomarhaim.goalpilot.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.idomarhaim.goalpilot.domain.usecase.OccurrenceReminders
import com.idomarhaim.goalpilot.domain.usecase.ReminderDecision
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * §2.5's **one reminder per occurrence**, at the moment it was armed for (`#56`).
 *
 * ## What this class is allowed to decide: nothing
 *
 * It reads three things — the task as it stands now, the live *Your day* setting, and the
 * minute it was enqueued for — hands them to [OccurrenceReminders.decideAtFireTime], and does
 * what comes back. Every rule about *whether the reminder is still wanted* lives in that pure
 * function, which is why §2.5's re-check is a JVM test rather than a device and a wait.
 *
 * The temptation is to put *"skip it if the task is done"* here, because it is one line. That
 * line is the whole feature's correctness, and here it can only be exercised by running a real
 * worker at a real wall-clock time on a real device — which is the shape that ends up untested
 * and then wrong.
 *
 * ## The re-check is free because nothing is stored
 *
 * §2.5: *"A reminder re-checks at fire time whether it is still needed — free, precisely
 * because nothing is stored."* There is no schedule document, no `nextReminderAt` field, and
 * nothing a sweep has to keep true. The reminder is recomputed from the task's occurrence and
 * the current setting, so a duration retyped or waking hours moved since this was armed are
 * simply *read*, and the run re-arms itself at the new time
 * ([ReminderDecision.Rearm]) rather than posting at the old one.
 *
 * ## Failure is success, deliberately
 *
 * Every path returns [Result.success]. `Result.retry()` would put the run on WorkManager's
 * backoff, which is the wrong clock entirely — the same reasoning [PlanTomorrowWorker] records
 * for the nightly prompt. A reminder that cannot be posted now is not a reminder to post in
 * thirty seconds; it is one whose moment has been re-derived or has gone.
 */
class OccurrenceReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(ReminderScheduler.KEY_TASK_ID)
            ?: return Result.success()
        val scheduledFor = inputData.getString(ReminderScheduler.KEY_SCHEDULED_FOR)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: return Result.success()

        val deps = EntryPointAccessors
            .fromApplication(applicationContext, NotificationEntryPoint::class.java)
        val schedule = deps.preferences().daySchedule.value
        // `first()` on the tasks flow rather than a one-shot get: `observeTasks` is a snapshot
        // listener served from Firestore's local cache, so this resolves without a round trip
        // and answers correctly offline, which is the state a phone is in at 22:59.
        val task = deps.taskRepository().observeTasks().first().firstOrNull { it.id == taskId }

        return when (val decision = OccurrenceReminders.decideAtFireTime(task, scheduledFor, schedule)) {
            is ReminderDecision.Fire -> {
                deps.notifier().notifyOccurrenceReminder(
                    task = requireNotNull(task) { "Fire implies a task" },
                    plan = decision.plan,
                    occurrence = decision.occurrence,
                )
                Result.success()
            }

            is ReminderDecision.Rearm -> {
                ReminderScheduler.scheduleOccurrenceReminder(
                    context = applicationContext,
                    taskId = taskId,
                    plan = decision.plan,
                    now = LocalDateTime.now(),
                )
                Result.success()
            }

            // Done, deleted, or no longer *when* anything. Nothing to post and nothing to
            // re-arm — and the notification already in the shade, if any, comes down with it:
            // a reminder for a task the user has since ticked is the app arguing with them.
            is ReminderDecision.Skip -> {
                deps.notifier().cancelOccurrenceReminder(taskId)
                Result.success()
            }
        }
    }
}
