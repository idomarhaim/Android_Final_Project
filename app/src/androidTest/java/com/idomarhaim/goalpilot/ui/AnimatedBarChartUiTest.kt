package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.idomarhaim.goalpilot.ui.components.BarItem
import com.idomarhaim.goalpilot.ui.components.HorizontalBarChart
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the analytics bars.
 *
 * The point of the animation is that the numbers *arrive* at their value rather
 * than starting there — so what has to be guaranteed is that they arrive. The test
 * clock runs the entry animation to completion during `waitForIdle`, and the final
 * labels are then asserted; a count-up that stopped short (or a stagger that never
 * fired for later rows) fails here.
 */
class AnimatedBarChartUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bars_countUpToTheirFinalValue() {
        composeRule.setContent {
            GoalPilotTheme {
                HorizontalBarChart(
                    items = listOf(
                        BarItem("Health", 0.75f, Color.Red, "75%", countUpTo = 75, countSuffix = "%"),
                        BarItem("Studies", 0.2f, Color.Blue, "20%", countUpTo = 20, countSuffix = "%"),
                        BarItem("Career", 0.05f, Color.Green, "5%", countUpTo = 5, countSuffix = "%"),
                    ),
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Health").assertIsDisplayed()
        composeRule.onNodeWithText("75%").assertIsDisplayed()
        composeRule.onNodeWithText("20%").assertIsDisplayed()
        // The last row carries the largest stagger delay — if the stagger were
        // unbounded or never scheduled, this is the label that would be missing.
        composeRule.onNodeWithText("5%").assertIsDisplayed()
    }

    @Test
    fun bars_withoutACountUpShowTheirTrailingLabelVerbatim() {
        composeRule.setContent {
            GoalPilotTheme {
                HorizontalBarChart(
                    items = listOf(BarItem("Sleep", 0.4f, Color.Magenta, "3h 20m")),
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("3h 20m").assertIsDisplayed()
    }
}
