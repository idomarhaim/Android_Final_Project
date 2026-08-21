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
import org.junit.Before
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
 * The permission is asserted here rather than assumed: an ungranted run must fail loudly, because
 * the failure mode this exists to prevent is a green suite that posted nothing.
 *
 * ⚠️ **It is also granted here, from `@Before`, and that is a change to this file's original
 * procedure — made 2026-08-21 by `c13-key-store`, not by `#8`.** The KDoc used to say the grant
 * came from **outside**, by `adb shell pm grant`. That is true of a human running it, and it is
 * **false of CI**: `.github/workflows/instrumented-tests.yml` has no grant step, so from the
 * moment this suite landed (2026-08-20 12:57) **every push to `main` went red** — four runs,
 * across three sessions' commits, none of which had anything to do with notifications.
 *
 * **Granting does not weaken the assertion, and that is why this is the fix rather than an
 * `assumeTrue`.** `uiAutomation.grantRuntimePermission` performs the real grant; if it does not
 * take, `requirePermission()` still fails exactly as loudly as before. What changes is only that
 * the common CI case stops being a false red. A skip would have been the weakening move — it
 * turns *"nothing was posted"* into a green run, which is the precise failure this suite exists
 * to catch.
 *
 * The other half of the original note still stands and is untouched: for a **human** collecting
 * evidence, run it through `adb shell am instrument`, because `connectedDebugAndroidTest`
 * uninstalls the app at the end and takes the posted notifications with it. CI does not read the
 * shade afterwards, so that concern does not reach it — the assertions here are what CI checks.
 */
class NotificationObservedFireTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifier = GoalPilotNotifier(context)

    /**
     * POST_NOTIFICATIONS is a runtime permission from API 33, and nothing in CI grants it.
     * Guarded on the API level because the permission does not exist below 33 and the grant
     * would fail on a device where none was ever needed.
     */
    @Before
    fun grantPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
    }

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
