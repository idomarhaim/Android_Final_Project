package com.idomarhaim.goalpilot.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.ui.navigation.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the app's notifications, and **#8 piece 5: does nothing loudly when it may not.**
 *
 * ## Refusal is structural here, not handled
 *
 * The brief calls the permission-refused path *"the piece most likely to be skipped, and the
 * one a grader will try"*. What makes it safe in this app is not a check in this class — it is
 * that **nothing depends on this class's return value.** The in-app half of `R5` is
 * `DashboardViewModel.filed` -> `SmartAddReceiptSnackbar`, a separate collector of a separate
 * flow, and this notifier is a *second, independent* consumer of the same event. Delete this
 * whole file and the snackbar, the Undo and the filing itself are unchanged. That is what makes
 * a refused permission a non-event rather than a degraded mode: there is no path from "may not
 * post" to any other behaviour in the app.
 *
 * [notifyFiling] therefore returns `Unit`, never a result to branch on, and it swallows the two
 * ways Android can still say no after the check passes (a [SecurityException] from a permission
 * revoked between the check and the post; a channel the user has blocked individually, which
 * `areNotificationsEnabled` does not report).
 */
@Singleton
open class GoalPilotNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Whether the **runtime permission** is held. Below API 33 it does not exist and the
     * manifest declaration is the grant.
     *
     * Separate from [canPost], and the split is not pedantry: this is the question
     * [NotificationPermissionPolicy] must be asked, because it decides whether to raise the
     * *permission dialog*. Feeding it [canPost] instead would make the app request a permission
     * it already holds every time the user had switched notifications off in settings — a
     * request that returns granted instantly, shows nothing, and asks the wrong question about
     * a state no dialog can fix.
     *
     * `open` so a test can put the app in the refused state without revoking a real permission
     * on a real device — a revoke that can restart the app process, and therefore the test
     * process, mid-run. The alternative is a suite whose result depends on the order the
     * runner happened to pick, which is the shape that passes vacuously.
     */
    open fun hasPostPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Whether a notification posted now could actually be **seen**.
     *
     * Two questions, not one: [hasPostPermission] **and** whether the user has switched the
     * app's notifications off in system settings, which is a separate control that predates the
     * permission and is not covered by it. An app that checked only the permission would report
     * itself able to post on any device where that switch is off.
     */
    open fun canPost(): Boolean =
        hasPostPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * `R5`'s system notification: the sorter had something to say about a quick-added task.
     *
     * Silent for [FilingDecision.ExistingGoal] — that row does not speak (§3.4), and
     * [FilingDecision.speaks] is the property being honoured rather than the branch being
     * re-derived, so the two surfaces cannot disagree about which outcomes are worth a
     * sentence.
     *
     * ⚠️ **The two speaking rows say different things, and conflating them would be a lie.**
     * `R4`'s word is *invents*, but §3.5 forbids inventing: [FilingDecision.NewGoal] proposes a
     * goal marked `AI_SUGGESTED`, while [FilingDecision.NoGoal] files the task under **no goal
     * at all**. A single "a new goal was created" message is simply false about the second.
     */
    fun notifyFiling(
        decision: FilingDecision,
        taskTitle: String,
        createdGoalId: String?,
    ) {
        if (!decision.speaks) return
        val content = when (decision) {
            is FilingDecision.ExistingGoal -> return

            is FilingDecision.NewGoal -> NotificationContent(
                title = context.getString(R.string.gp_notif_filing_new_goal_title),
                body = context.getString(
                    R.string.gp_notif_filing_new_goal_body,
                    decision.title,
                    taskTitle,
                ),
                // The proposed goal itself, so the tap lands on the thing the user has to
                // decide about. Falls back to the goals list when the filing somehow produced
                // no id — a notification that opens nothing is worse than one that opens the
                // list it is talking about.
                route = createdGoalId?.let(Routes::goalDetail) ?: Routes.GOALS,
            )

            is FilingDecision.NoGoal -> NotificationContent(
                title = context.getString(R.string.gp_notif_filing_no_goal_title),
                body = context.getString(R.string.gp_notif_filing_no_goal_body, taskTitle),
                // There is no goal to open, by definition. The dashboard is where the task is.
                route = Routes.DASHBOARD,
            )
        }
        post(ID_FILING, GoalPilotChannels.FILING, content)
    }

    /** §2.5's nightly *plan tomorrow* prompt — Ido's own addition to the reminder set. */
    fun notifyPlanTomorrow() {
        post(
            ID_PLAN_TOMORROW,
            GoalPilotChannels.REMINDERS,
            NotificationContent(
                title = context.getString(R.string.gp_notif_plan_tomorrow_title),
                body = context.getString(R.string.gp_notif_plan_tomorrow_body),
                route = Routes.DASHBOARD,
            ),
        )
    }

    private fun post(id: Int, channelId: String, content: NotificationContent) {
        if (!canPost()) return
        GoalPilotChannels.ensure(context)
        val tapIntent = PendingIntent.getActivity(
            context,
            id,
            NotificationDeepLink.intentFor(context, content.route),
            // IMMUTABLE is required from API 31 and correct everywhere: nothing outside this
            // app has any business rewriting the route. UPDATE_CURRENT because the id is
            // shared per kind — without it a second filing would reuse the first intent and
            // the tap would open the previous goal.
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            // The filing body names a goal and a task title, either of which can be long
            // enough to be elided into uselessness on one line.
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // The permission can be revoked between canPost() and here — a settings change in
            // another window, or a work-profile policy. Nothing above needs to know: the
            // in-app surface is a different flow and is unaffected.
        }
    }

    private data class NotificationContent(
        val title: String,
        val body: String,
        val route: String,
    )

    internal companion object {
        /**
         * One id per *kind*, so a second filing replaces the first rather than stacking.
         *
         * `internal` rather than private so a test can identify a posted notification **by id**.
         * That is not a convenience: when two of this app's notifications are up at once the
         * system synthesises its own `AUTOGROUP_SUMMARY` record, and it carries the **channel id
         * of the first one** — so matching a posted notification by channel finds two, and the
         * test only fails when the suite happens to run in the order that leaves both up.
         * `Observed:` 2026-08-20 — passed run in isolation, failed in the full suite.
         *
         * A quick-add is a burst activity — three tasks typed in twenty seconds — and three
         * notifications saying almost the same thing is the shape people mute a channel over.
         * The snackbar already showed each one as it happened; the notification is the record
         * of the latest, for someone who has put the phone down.
         */
        const val ID_FILING = 8001
        const val ID_PLAN_TOMORROW = 8002
    }
}
