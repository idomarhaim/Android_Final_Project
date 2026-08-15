package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.ui.components.TasksConsentNotice
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for issue #36. Runs on a device/emulator; needs no
 * Firebase and no Google account.
 *
 * This is the layer that can assert what #36 actually ships — **a sentence the
 * user reads**. The JVM suite pins the state machine that decides *when* to show
 * it (`feature/lifeareas/TasksConsentTest`); nothing there proves it renders.
 */
class TasksConsentNoticeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun notice_saysTheScopeWasNotGranted() {
        val title = resources.getString(R.string.tasks_consent_missing_title)
        val body = resources.getString(R.string.tasks_consent_missing_body)

        composeRule.setContent { GoalPilotTheme { TasksConsentNotice() } }

        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(body).assertIsDisplayed()
    }

    @Test
    fun notice_namesTheCheckboxTheUserHasToTick() {
        // §0.4: speak about a failure the user can act on. "Permission denied"
        // is not actionable; the name of the box on Google's screen is. If this
        // ever fails, the sentence has drifted back to a generic grant prompt —
        // which is the entire defect #36 was filed against.
        val body = resources.getString(R.string.tasks_consent_missing_body)

        assertTrue(
            "The declined sentence must name the consent checkbox, not just " +
                "report a missing permission. Was: $body",
            body.contains("View your tasks"),
        )
    }
}
