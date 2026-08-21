package com.idomarhaim.goalpilot.ui

import android.app.NotificationManager
import android.content.Context
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddReceipt
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddReceiptSnackbar
import com.idomarhaim.goalpilot.notifications.FilingNotificationEffect
import com.idomarhaim.goalpilot.notifications.GoalPilotNotifier
import com.idomarhaim.goalpilot.notifications.NotificationAsk
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * #8 piece 5 — **the app when the user says no to notifications.**
 *
 * The brief calls this *"the piece most likely to be skipped, and the one a grader will try"*,
 * and the reason it is skippable is that nothing goes red: a refused permission produces no
 * crash, no exception and no failing assertion anywhere else. The app simply stops saying one
 * of the two things it was supposed to say, and the remaining one looks fine.
 *
 * So this suite asserts the **positive** half, which is the half that can rot: with the
 * permission refused, `#6`'s in-app witness still renders. §0.7 permits silent filing *only*
 * because the filing is visible afterwards — if a notification permission the user declined
 * could take the snackbar down with it, declining would remove the thing that made the silence
 * legitimate.
 *
 * It runs the real [FilingNotificationEffect] beside the real [SmartAddReceiptSnackbar], in one
 * composition, the way `DashboardScreen` has them. Testing either alone would prove nothing
 * about the claim, which is precisely that the two are independent.
 *
 * **The refusal is injected, not arranged on the device.** [RefusingNotifier] reports no
 * permission, so the state under test is the same on every API level and in any run order. The
 * two alternatives are both worse: revoking `POST_NOTIFICATIONS` for real can restart the app
 * process — which is the test process — mid-run, and *assuming* the permission is absent makes
 * the result depend on whichever suite the runner happened to schedule first, which is how a
 * test passes vacuously.
 */
class NotificationPermissionRefusedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** The app after the user has declined, or after two dismissals stopped the dialog. */
    private class RefusingNotifier(context: Context) : GoalPilotNotifier(context) {
        override fun hasPostPermission(): Boolean = false
        override fun canPost(): Boolean = false
    }

    private val refused = RefusingNotifier(context)

    private val receipt = SmartAddReceipt(
        taskId = "t-1",
        taskTitle = "Draft the retrospective",
        decision = FilingDecision.NewGoal(
            title = "Run better retrospectives",
            category = GoalCategory.CAREER,
            lifeAreaId = null,
        ),
        createdGoalId = "g-1",
    )

    @Before
    fun startFromARefusedState() {
        // ALREADY_ASKED rather than ASK_NOW: the state a user who has refused is actually in,
        // and the one that does not raise a system dialog no instrumented run could answer.
        NotificationAsk.askedThisProcess = true
    }

    @Test
    fun aRefusedNotifierReportsThatItCannotPost() {
        assertThat(refused.hasPostPermission()).isFalse()
        assertThat(refused.canPost()).isFalse()
    }

    @Test
    fun aRefusedNotificationIsNotPostedAndDoesNotThrow() {
        // The call must be safe as well as silent: it is made from a Compose effect on the
        // dashboard, so an exception here would take down the screen the snackbar lives on —
        // turning a declined permission into a crash on the app's home screen.
        val before = shadeFingerprint()
        refused.notifyFiling(receipt.decision, receipt.taskTitle, receipt.createdGoalId)
        refused.notifyPlanTomorrow()

        assertThat(shadeFingerprint()).isEqualTo(before)
    }

    @Test
    fun theInAppWitnessStillRendersWithTheNotificationRefused() {
        composeRule.setContent {
            GoalPilotTheme {
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { SnackbarHost(host) }) {
                    // Both consumers of the same event, exactly as DashboardScreen wires them.
                    SmartAddReceiptSnackbar(
                        receipt = receipt,
                        hostState = host,
                        onUndo = {},
                        onConsume = {},
                    )
                    FilingNotificationEffect(
                        taskId = receipt.taskId,
                        decision = receipt.decision,
                        taskTitle = receipt.taskTitle,
                        createdGoalId = receipt.createdGoalId,
                        notifier = refused,
                    )
                }
            }
        }

        // The sentence `#6` ships for a NewGoal outcome. If the notifier's inability to post
        // could reach the snackbar, this is the assertion that would fail.
        composeRule
            .onNodeWithText("No goal fitted — suggested “Run better retrospectives”")
            .assertIsDisplayed()
    }

    @Test
    fun theEffectPostsNothingWhenItMayNot() {
        val before = shadeFingerprint()
        composeRule.setContent {
            GoalPilotTheme {
                FilingNotificationEffect(
                    taskId = receipt.taskId,
                    decision = receipt.decision,
                    taskTitle = receipt.taskTitle,
                    createdGoalId = receipt.createdGoalId,
                    notifier = refused,
                )
            }
        }
        composeRule.waitForIdle()
        assertThat(shadeFingerprint()).isEqualTo(before)
    }

    @Test
    fun aSilentFilingOutcomeIsNotNotifiedEvenWhenPostingIsAllowed() {
        // FilingDecision.ExistingGoal does not speak (§3.4). This is the one assertion that
        // needs a notifier which CAN post, or it would pass for the wrong reason.
        val allowed = object : GoalPilotNotifier(context) {
            override fun hasPostPermission(): Boolean = true
            override fun canPost(): Boolean = true
        }
        val before = shadeFingerprint()
        allowed.notifyFiling(
            FilingDecision.ExistingGoal(goalId = "g-9", goalTitle = "Ship v0.3"),
            taskTitle = "Write the release notes",
            createdGoalId = null,
        )
        assertThat(shadeFingerprint()).isEqualTo(before)
    }

    /**
     * Every notification currently in the shade, identified so that a **re-post**
     * of one that is already up is distinguishable from leaving it alone.
     *
     * ### Why this replaced `assertThat(activeNotifications).isEmpty()` — issue #58
     *
     * The three assertions here all claim the same thing: *the refused notifier
     * posted nothing.* Spelled as "the shade is empty" that claim is only true if
     * the shade **started** empty, which is why this class used to `cancelAll()`
     * in `@Before` — and that was the defect.
     *
     * [NotificationObservedFireTest] runs immediately before this class (the
     * runner orders classes by name, and `Observed` < `Permission`) and is
     * *deliberately* not self-contained: it leaves its notifications posted so a
     * human can read them afterwards with `adb shell dumpsys notification
     * --noredact`. Its own KDoc says so, and says it is why that suite has no
     * teardown. `cancelAll()` here then wiped exactly that evidence — one file
     * away from the class whose KDoc explains why it must not be, and shipped in
     * the same commit as it (`99d3e31`). `Observed:` 2026-08-21 — after a full
     * green suite run, `dumpsys notification --noredact` showed **zero** GoalPilot
     * notifications.
     *
     * `#58`'s option 4 proposed `cancelAll()` in `@Before` for the *other* suite
     * and was withdrawn on the ticket for this reason. It had already shipped
     * here.
     *
     * ### Why the fingerprint carries `postTime`
     *
     * Id and tag alone are not enough. `ID_FILING` and `ID_PLAN_TOMORROW` are
     * usually already in the shade when this class runs, so a notifier that
     * posted despite being refused would **replace** them: same id, same tag, and
     * a set of keys that did not change — a pass for the wrong reason. `postTime`
     * moves on every post, so a replacement shows up as a difference.
     *
     * The result is a strictly stronger assertion than the old one *and* one that
     * needs nothing from the shade's prior state.
     */
    private fun shadeFingerprint(): Set<String> =
        notificationManager().activeNotifications
            .map { "id=${it.id} tag=${it.tag} postTime=${it.postTime}" }
            .toSet()

    private fun notificationManager(): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
