package com.idomarhaim.goalpilot.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.idomarhaim.goalpilot.R

/**
 * The app's notification channels — #8 piece 1, created at app start.
 *
 * **Two channels, not one.** The system's own control is per channel, so a channel is the
 * finest grain at which a person can say *"stop telling me this"*. These two are things
 * someone genuinely wants to silence separately: turning off *"the sorter could not place
 * that task"* must not also turn off tonight's planning prompt, and a single channel makes
 * that one switch. Splitting further would be worse — a channel per notification is a
 * settings screen nobody reads.
 *
 * **Names and descriptions come from `res/`** because the *system* renders them, on a screen
 * this app never draws. That makes them the one class of user-facing text this package cannot
 * hold as a literal, whatever §0.8's freeze permits elsewhere.
 *
 * Idempotent by construction: `createNotificationChannel` on an existing id updates its name
 * and description and leaves the user's own importance and sound choices alone. That is why
 * this is safe to call on every process start rather than gated behind a "first run" flag,
 * which would be a stored fact that can disagree with the device (§0.2).
 */
object GoalPilotChannels {

    /** §3.4's speaking filing outcomes — `#6`'s witness, as a system notification (`R5`). */
    const val FILING = "gp_filing"

    /** §2.5's reminders and the nightly *plan tomorrow* prompt. */
    const val REMINDERS = "gp_reminders"

    /**
     * Creates or updates both channels. Safe to call repeatedly; a no-op below API 26, where
     * channels do not exist and every notification is posted without one.
     */
    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                FILING,
                context.getString(R.string.gp_channel_filing_name),
                // DEFAULT, not HIGH: §0.7 makes filing instrumental and silent, and the
                // in-app snackbar is the primary surface. This is the after-the-fact record
                // for a quick-add the user has already walked away from -- worth seeing in
                // the shade, never worth a heads-up card over whatever they are doing now.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.gp_channel_filing_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDERS,
                context.getString(R.string.gp_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.gp_channel_reminders_description)
            },
        )
    }
}
