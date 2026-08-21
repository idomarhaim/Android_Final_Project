package com.idomarhaim.goalpilot

import android.app.Application
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.notifications.GoalPilotChannels
import com.idomarhaim.goalpilot.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation and
 * creates the application-level dependency container.
 */
@HiltAndroidApp
class GoalPilotApp : Application() {

    /**
     * Field-injected for the same reason `MainActivity` does it: this runs before any
     * ViewModel exists, and the nightly schedule has to be armed whether or not the user
     * ever reaches a screen that would create one.
     */
    @Inject
    lateinit var appPreferences: AppPreferencesRepository

    /**
     * The task list §2.5's per-occurrence reminders are armed from (`#56`).
     *
     * Field-injected beside [appPreferences] and for the same reason: the reminders have to
     * track the tasks whether or not a screen that would observe them is ever opened.
     */
    @Inject
    lateinit var taskRepository: TaskRepository

    /**
     * Lives as long as the process, which is the honest scope for *"keep the nightly reminder
     * in step with the setting"* — there is no lifecycle shorter than the app that owns it.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // #8 piece 1. Idempotent, and cheap: on an existing channel this updates the name and
        // description and leaves the user's own importance and sound choices alone. Doing it
        // unconditionally rather than behind a first-run flag keeps the channel text correct
        // after an app update that reworded it, which a stored flag would freeze.
        GoalPilotChannels.ensure(this)

        // §2.5's nightly *plan tomorrow* prompt, re-armed whenever §4.9's *Your day* moves.
        // distinctUntilChanged over the derived minute rather than over DaySchedule: the
        // waking-hours handles can move without changing the planning time (an earlier start
        // does not), and rescheduling on every touch of the settings slider would replace the
        // pending work on every frame of a drag.
        appScope.launch {
            appPreferences.daySchedule
                .map { it.planningMinutes }
                .distinctUntilChanged()
                .collect { minute ->
                    ReminderScheduler.schedulePlanTomorrow(this@GoalPilotApp, minute)
                }
        }

        // §2.5's *one reminder per occurrence, timed per rung* (`#56`), kept in step with both
        // of its inputs: the tasks themselves, and §4.9's *Your day* (which moves the waking
        // clamp every reminder is computed against).
        //
        // Here rather than in a ViewModel, for the reason the block above is here: a reminder
        // must be armed whether or not the user reaches a screen, and `observeTasks` is a
        // cache-served snapshot listener, so this costs no round trip on start.
        //
        // `distinctUntilChanged` over the (tasks, schedule) pair and not inside the combine:
        // Firestore re-emits the same list on every unrelated document write, and each
        // re-emission would otherwise replace every pending reminder in the queue. The
        // comparison is a data-class equality over a list the app already holds in memory.
        appScope.launch {
            combine(
                taskRepository.observeTasks(),
                appPreferences.daySchedule,
            ) { tasks, schedule -> tasks to schedule }
                .distinctUntilChanged()
                .collect { (tasks, schedule) ->
                    ReminderScheduler.syncOccurrenceReminders(
                        context = this@GoalPilotApp,
                        tasks = tasks,
                        schedule = schedule,
                    )
                }
        }
    }
}
