package com.idomarhaim.goalpilot.ui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.notifications.GoalPilotChannels
import com.idomarhaim.goalpilot.notifications.GoalPilotNotifier
import com.idomarhaim.goalpilot.notifications.NotificationDeepLink
import com.idomarhaim.goalpilot.ui.navigation.Routes
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * **The notification actually appearing.** The brief's trap, in its own words: *"a notification
 * you cannot see is a notification you have not built"* — every layer here is asynchronous and
 * time-dependent, so the tests pass while the thing never fires on a real phone.
 *
 * This suite is the answer to that, and it is deliberately **not** self-contained: it posts and
 * **leaves the notifications up**, so `adb shell dumpsys notification --noredact` run afterwards
 * shows them in the shade. That is why it does not `cancelAll` in a teardown, and why it must be
 * run through `adb shell am instrument` rather than `connectedDebugAndroidTest` — the Gradle
 * task **uninstalls the app when it finishes**, taking its notifications with it, so the
 * evidence would be gone before anyone could read it (`AGENTS.md`,
 * `kb/dev/android-device-verification.md` §8).
 *
 * The permission is granted from **outside**, by `adb shell pm grant`, and asserted here rather
 * than assumed: an ungranted run must fail loudly, because the failure mode this exists to
 * prevent is a green suite that posted nothing.
 */
class NotificationObservedFireTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifier = GoalPilotNotifier(context)

    @Test
    fun theFilingNotificationReallyAppearsInTheShade() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        notifier.notifyFiling(
            decision = FilingDecision.NewGoal(
                title = "Run better retrospectives",
                category = GoalCategory.CAREER,
                lifeAreaId = null,
            ),
            taskTitle = "Draft the retrospective",
            createdGoalId = "observed-fire-goal",
        )

        // BY ID, not by channel: with both notifications up the system adds its own
        // AUTOGROUP_SUMMARY record carrying the first one's channel, so a channel filter
        // matches two and this fails only in the run orders that leave both posted.
        val posted = active().single { it.id == GoalPilotNotifier.ID_FILING }
        assertThat(posted.notification.channelId()).isEqualTo(GoalPilotChannels.FILING)
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("No goal fitted")
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TEXT))
            .contains("Run better retrospectives")
        // The tap-through is the piece a screenshot cannot show.
        assertThat(posted.notification.contentIntent).isNotNull()
    }

    @Test
    fun thePlanTomorrowNotificationReallyAppearsInTheShade() {
        requirePermission()
        GoalPilotChannels.ensure(context)

        notifier.notifyPlanTomorrow()

        val posted = active().single { it.id == GoalPilotNotifier.ID_PLAN_TOMORROW }
        assertThat(posted.notification.channelId()).isEqualTo(GoalPilotChannels.REMINDERS)
        assertThat(posted.notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Plan tomorrow")
        assertThat(posted.notification.contentIntent).isNotNull()
    }

    @Test
    fun bothChannelsExistWithTheirUserFacingText() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        GoalPilotChannels.ensure(context)
        val manager = context.getSystemService(NotificationManager::class.java)

        val filing = manager.getNotificationChannel(GoalPilotChannels.FILING)
        val reminders = manager.getNotificationChannel(GoalPilotChannels.REMINDERS)

        // These strings are what the SYSTEM renders on a screen this app never draws, so an
        // empty description is invisible to every other layer of testing.
        assertThat(filing.name.toString()).isEqualTo("Smart filing")
        assertThat(filing.description).isNotEmpty()
        assertThat(reminders.name.toString()).isEqualTo("Reminders and planning")
        assertThat(reminders.description).isNotEmpty()
    }

    @Test
    fun theTapThroughCarriesTheRouteOfTheGoalItIsTalkingAbout() {
        // #8 piece 3. dumpsys can show that a contentIntent EXISTS; only this shows that it
        // carries the right destination, which is the half that can be wrong silently -- a
        // notification about a proposed goal that opens the dashboard has still "worked".
        val intent = NotificationDeepLink.intentFor(context, Routes.goalDetail("g-42"))
        NotificationDeepLink.offer(intent)
        assertThat(NotificationDeepLink.consume()).isEqualTo("goal_detail/g-42")
    }

    @Test
    fun theRouteIsConsumedOnceSoAConfigurationChangeCannotReNavigate() {
        NotificationDeepLink.offer(NotificationDeepLink.intentFor(context, Routes.DASHBOARD))
        assertThat(NotificationDeepLink.consume()).isEqualTo(Routes.DASHBOARD)
        // The second read is what a rotation would do. It must find nothing, or the user is
        // thrown back to a notification they already followed and walked away from.
        assertThat(NotificationDeepLink.consume()).isNull()
    }

    @Test
    fun anIntentWithNoRouteLeavesThePendingRouteAlone() {
        NotificationDeepLink.consume()
        NotificationDeepLink.offer(android.content.Intent())
        assertThat(NotificationDeepLink.consume()).isNull()
    }

    private fun requirePermission() {
        assertThat(notifier.hasPostPermission()).isTrue()
        assertThat(notifier.canPost()).isTrue()
    }

    private fun active(): List<StatusBarNotification> =
        context.getSystemService(NotificationManager::class.java).activeNotifications.toList()

    private fun Notification.channelId(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) channelId else null
}
