package com.idomarhaim.goalpilot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FillLadder
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.feature.goals.FillButtonRow
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the repeat-tappable fill buttons and their running
 * tally — `R25`, [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11).
 *
 * Stateless and lambda-driven, so it needs no Firebase and no Hilt — the same
 * shape as [DonutChartUiTest] and [LifeAreaReorderUiTest]. What it pins is the
 * half the JVM cannot see: that a tap actually reaches the log lambda, that the
 * **same** button survives being tapped four times, and that the tally the screen
 * draws is the number the logs add up to. `FillLadderTest` owns the arithmetic
 * those buttons are labelled with.
 *
 * **The fixture is the ticket's own goal.** *"Drink 4 Liters of Water Daily"* is
 * live on Ido's account and reads `Health · 1/100 %` today; four litres is also
 * §1.3's worked example, so the labels asserted here are the spec's.
 */
class FillButtonRowUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val waterGoal = Goal(
        id = "water",
        title = "Drink 4 Liters of Water Daily",
        targetValue = 4.0,
        measure = Measure(MeasureKind.VOLUME, "L"),
        inputMode = InputMode.BUTTONS,
    )

    private val logged = mutableListOf<Double>()

    /**
     * The row wired to a tally that is a **sum over what was logged**, which is
     * exactly what §4.6 makes `currentValue` — so the test drives the same
     * arithmetic the repository does rather than a stand-in for it.
     */
    private fun setRow(goal: Goal = waterGoal) {
        composeRule.setContent {
            var current by remember { mutableStateOf(0.0) }
            GoalPilotTheme {
                FillButtonRow(
                    amounts = FillLadder.forGoal(goal),
                    word = goal.measureWord,
                    current = current,
                    target = goal.targetValue,
                    onLog = { amount ->
                        logged += amount
                        current += amount
                    },
                )
            }
        }
    }

    @Test
    fun row_showsTheLadderTheSpecNames() {
        setRow()

        // §1.3's four buttons for a 4 L target, in the goal's own word.
        //
        // Found by the content description, never by the text: every visible
        // string here is bidi-isolated for §4.8, so `onNodeWithText("0.25 L")`
        // matches nothing — the rendered text carries FSI/PDI marks around it.
        // The description is the plain reading of the same amount, which is what
        // TalkBack announces and what this suite can address.
        listOf("0.25 L", "0.5 L", "0.75 L", "1 L").forEach { label ->
            composeRule.onNodeWithContentDescription("Log $label").assertIsDisplayed()
        }
    }

    @Test
    fun row_startsTheTallyInLitresNotPercent() {
        setRow()

        // The defect the ticket opens on, stated as an assertion: this goal reads
        // `1/100 %` on Ido's screen today, and what it must read is litres.
        composeRule.onNodeWithContentDescription("0 / 4 L").assertIsDisplayed()
    }

    @Test
    fun button_isRepeatTappableAndTheTallyFollows() {
        setRow()

        // The whole of `R25`: "several fill buttons I can tap more than once".
        // The SAME node is clicked four times — a row that recomposed the button
        // out of existence, or disabled it after one log, fails here.
        repeat(4) { composeRule.onNodeWithContentDescription("Log 0.25 L").performClick() }

        assertThat(logged).containsExactly(0.25, 0.25, 0.25, 0.25)
        composeRule.onNodeWithContentDescription("1 / 4 L").assertIsDisplayed()
    }

    @Test
    fun tally_isASumAcrossDifferentButtons() {
        setRow()

        composeRule.onNodeWithContentDescription("Log 1 L").performClick()
        composeRule.onNodeWithContentDescription("Log 0.5 L").performClick()
        composeRule.onNodeWithContentDescription("Log 0.25 L").performClick()

        // 1.75, not "1.8" — the old `trimNumber` rounded to one decimal, which
        // would have made the tally disagree with the buttons that produced it.
        composeRule.onNodeWithContentDescription("1.75 / 4 L").assertIsDisplayed()
    }

    @Test
    fun tally_keepsCountingPastTheTarget() {
        setRow()

        // Overshoot is legal and shown (§1.5). A tally that stopped at 4 would be
        // the fifth clamp, after the four #49 deleted.
        repeat(5) { composeRule.onNodeWithContentDescription("Log 1 L").performClick() }

        composeRule.onNodeWithContentDescription("5 / 4 L").assertIsDisplayed()
    }

    @Test
    fun everyButtonStaysEnabledForever() {
        setRow()

        // The inverse of the test that used to stand here, and the change is a
        // pre-commit-review finding rather than a preference. `logProgress` ends
        // in `set().await()`, which resolves on **server ack**; gating the row on
        // a write in flight therefore disables all four buttons offline after one
        // tap, on the control `R25` defines as repeat-tappable. There is no state
        // in which a fill button is disabled, and this says so.
        repeat(3) { composeRule.onNodeWithContentDescription("Log 1 L").performClick() }

        listOf("0.25 L", "0.5 L", "0.75 L", "1 L").forEach { label ->
            composeRule.onNodeWithContentDescription("Log $label").assertIsEnabled()
        }
    }

    @Test
    fun row_isAbsentForAGoalWithNoClassifiedMeasure() {
        // The migration's half-way state — a word survived, a kind never existed.
        // The row must draw nothing at all rather than an empty strip, because
        // `C22` #44's offer is what belongs in that space.
        setRow(waterGoal.copy(measure = Measure(kind = null, word = "litres")))

        composeRule.onNodeWithContentDescription("Log 1 litres").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("0 / 4 litres").assertDoesNotExist()
    }

    @Test
    fun row_isAbsentForAGoalNotInButtonsMode() {
        setRow(waterGoal.copy(inputMode = InputMode.NUMBER))

        composeRule.onNodeWithContentDescription("0 / 4 L").assertDoesNotExist()
    }
}
