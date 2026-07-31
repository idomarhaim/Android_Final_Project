package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test. Verifies a shared component renders its content.
 * Runs on a device/emulator; does not require Firebase.
 */
class EmptyStateUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_showsTitleAndSubtitle() {
        composeRule.setContent {
            GoalPilotTheme {
                EmptyState(title = "No goals yet", subtitle = "Add your first goal")
            }
        }

        composeRule.onNodeWithText("No goals yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add your first goal").assertIsDisplayed()
    }
}
