package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.core.util.bidiIsolated
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
 *
 * ⚠️ **The trailing labels are direction-isolated since issue #51's
 * `ui/components/` sweep**, so the expectations here are wrapped in
 * [bidiIsolated] rather than spelled as plain strings. That is not test noise —
 * it is the assertion. `"75%"` and `"3h 20m"` contain no strong directional
 * character, so in an RTL paragraph the bidi algorithm resolves them from the
 * paragraph and renders `%75`; the isolate is what pins them to their own
 * direction. Spelling these expectations without the marks is exactly how the
 * fix would get reverted by someone making a red test green.
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

        // The row LABEL is not isolated: it is a life-area name, ordinary prose
        // in whatever script the user wrote it, and it owns its whole Text.
        composeRule.onNodeWithText("Health").assertIsDisplayed()
        composeRule.onNodeWithText("75%".bidiIsolated()).assertIsDisplayed()
        composeRule.onNodeWithText("20%".bidiIsolated()).assertIsDisplayed()
        // The last row carries the largest stagger delay — if the stagger were
        // unbounded or never scheduled, this is the label that would be missing.
        composeRule.onNodeWithText("5%".bidiIsolated()).assertIsDisplayed()
    }

    @Test
    fun bars_withoutACountUpShowTheirTrailingLabelIsolated() {
        composeRule.setContent {
            GoalPilotTheme {
                HorizontalBarChart(
                    items = listOf(BarItem("Sleep", 0.4f, Color.Magenta, "3h 20m")),
                )
            }
        }

        composeRule.waitForIdle()

        // The caller's string reaches the screen unchanged apart from the two
        // zero-width isolate marks — strip them and it is byte-identical.
        composeRule.onNodeWithText("3h 20m".bidiIsolated()).assertIsDisplayed()
        assertThat(Bidi.strip("3h 20m".bidiIsolated())).isEqualTo("3h 20m")
    }
}
