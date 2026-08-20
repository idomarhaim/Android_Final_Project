package com.idomarhaim.goalpilot

import android.app.Application
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.notifications.GoalPilotChannels
import com.idomarhaim.goalpilot.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    }
}
