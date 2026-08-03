package com.idomarhaim.goalpilot.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.ui.components.DonutChart
import com.idomarhaim.goalpilot.ui.components.DonutSlice
import com.idomarhaim.goalpilot.ui.components.sliceAt
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the interactive time-allocation donut. Stateless and
 * lambda-driven, so it needs no Firebase — same shape as [SkinPickerUiTest].
 */
class DonutChartUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val slices = listOf(
        DonutSlice(id = "health", label = "Health", fraction = 0.5f, color = Color.Red),
        DonutSlice(id = "study", label = "Studies", fraction = 0.3f, color = Color.Blue),
        DonutSlice(id = "career", label = "Career", fraction = 0.2f, color = Color.Green),
    )

    @Test
    fun donut_rendersItsCentreAndDescription() {
        composeRule.setContent {
            GoalPilotTheme {
                DonutChart(
                    slices = slices,
                    contentDescription = "Time split across 3 life areas",
                ) { Text("12h") }
            }
        }

        composeRule.onNodeWithContentDescription("Time split across 3 life areas")
            .assertIsDisplayed()
        composeRule.onNodeWithText("12h").assertIsDisplayed()
    }

    @Test
    fun donut_tappingTheRingSelectsTheWedgeUnderTheFinger() {
        var selected: String? = null
        composeRule.setContent {
            GoalPilotTheme {
                Box {
                    DonutChart(
                        slices = slices,
                        onSelect = { selected = it },
                        contentDescription = "chart",
                    )
                }
            }
        }

        // 3 o'clock is 90° clockwise from the top, inside the first (50 %) wedge.
        composeRule.onNodeWithContentDescription("chart").performTouchInput {
            click(Offset(centerX + width * 0.42f, centerY))
        }
        composeRule.waitForIdle()

        assertThat(selected).isEqualTo("health")
    }

    @Test
    fun donut_tappingTheHoleClearsTheSelection() {
        var selected: String? = "health"
        composeRule.setContent {
            GoalPilotTheme {
                DonutChart(
                    slices = slices,
                    selectedId = "health",
                    onSelect = { selected = it },
                    contentDescription = "chart",
                )
            }
        }

        composeRule.onNodeWithContentDescription("chart").performTouchInput { click(center) }
        composeRule.waitForIdle()

        assertThat(selected).isNull()
    }

    @Test
    fun sliceAt_measuresAnglesClockwiseFromTwelveOClock() {
        val size = 200f to 200f
        val thickness = 40f // ring spans radius 60..100

        // Wedges, clockwise from the top: Health 0–180°, Studies 180–288°, Career 288–360°.
        // 12 o'clock is 0°.
        assertThat(sliceAt(Offset(100f, 20f), size, thickness, slices)).isEqualTo("health")
        // 6 o'clock is 180° — the first degree of Studies.
        assertThat(sliceAt(Offset(100f, 180f), size, thickness, slices)).isEqualTo("study")
        // 9 o'clock is 270°, still Studies.
        assertThat(sliceAt(Offset(20f, 100f), size, thickness, slices)).isEqualTo("study")
        // 315° (up and to the left) is Career.
        assertThat(sliceAt(Offset(44f, 44f), size, thickness, slices)).isEqualTo("career")
        // The hole and the outside are not part of any wedge.
        assertThat(sliceAt(Offset(100f, 100f), size, thickness, slices)).isNull()
        assertThat(sliceAt(Offset(0f, 0f), size, thickness, slices)).isNull()
    }
}
