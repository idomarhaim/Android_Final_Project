package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.idomarhaim.goalpilot.ui.components.StackedColumn
import com.idomarhaim.goalpilot.ui.components.StackedColumnChart
import com.idomarhaim.goalpilot.ui.components.StackedSegment
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the time trend. Stateless and data-driven, so it
 * needs no Firebase — same shape as [DonutChartUiTest].
 */
class StackedColumnChartUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun column(label: String, health: Int, study: Int) = StackedColumn(
        label = label,
        segments = listOf(
            StackedSegment(id = "health", label = "Health", color = Color.Red, value = health),
            StackedSegment(id = "study", label = "Studies", color = Color.Blue, value = study),
        ),
    )

    private val week = listOf(
        column("Mon", 60, 30),
        column("Tue", 0, 0),
        column("Wed", 20, 90),
    )

    @Test
    fun trend_labelsEveryColumnWhenTheyFit() {
        composeRule.setContent {
            GoalPilotTheme {
                StackedColumnChart(
                    columns = week,
                    maxValue = 110,
                    contentDescription = "Tracked time per day",
                )
            }
        }

        composeRule.onNodeWithContentDescription("Tracked time per day").assertIsDisplayed()
        listOf("Mon", "Tue", "Wed").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun trend_keepsAnEmptyColumnInPlaceRatherThanDroppingIt() {
        composeRule.setContent {
            GoalPilotTheme {
                StackedColumnChart(columns = week, maxValue = 110, contentDescription = "chart")
            }
        }

        // A day with nothing on it is still a labelled column; a missing column
        // would read as a rendering fault rather than as a day off.
        composeRule.onNodeWithText("Tue").assertIsDisplayed()
    }

    @Test
    fun trend_thinsLabelsWhenAQuarterOfWeeksWillNotFit() {
        val quarter = (1..13).map { column("w$it", it * 10, 0) }
        composeRule.setContent {
            GoalPilotTheme {
                StackedColumnChart(columns = quarter, maxValue = 130, contentDescription = "chart")
            }
        }

        // Every second label: w1, w3, … w13 are drawn, the even ones are blanked.
        composeRule.onAllNodesWithText("w1").assertCountEquals(1)
        composeRule.onAllNodesWithText("w2").assertCountEquals(0)
        composeRule.onAllNodesWithText("w3").assertCountEquals(1)
    }
}
