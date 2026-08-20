package com.idomarhaim.goalpilot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.feature.goals.AI_ESTIMATE_ICON_LABEL
import com.idomarhaim.goalpilot.feature.goals.AddTaskRow
import com.idomarhaim.goalpilot.feature.goals.DURATION_BOX_TAG
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * `R8`'s duration box on the real Compose runtime —
 * [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9).
 *
 * **What this owns and what it does not.** `DurationEntryTest` owns the precedence
 * rule as arithmetic; it can prove that `withEstimate` returns a typed value
 * untouched, and it cannot prove that the composable *calls* `withEstimate` rather
 * than assigning. That gap is the whole reason this file exists, and it is the gap
 * where the defect would actually live: the rule is correct in the domain and
 * bypassed in the UI. So every test here drives the row the way a person does —
 * type, press, retitle — and reads what the row shows and hands back.
 *
 * Stateless and lambda-driven, so it needs no Firebase and no Hilt — the same shape
 * as [FillButtonRowUiTest].
 */
class DurationBoxUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Added(
        val title: String,
        val points: Int,
        val minutes: Int,
        val source: DurationSource,
    )

    private val added = mutableListOf<Added>()
    private var suggestRequestedFor: String? = null

    /**
     * The row wired to a suggestion that arrives when [suggest] is called, which is
     * how the real screen delivers one: asynchronously, into a parameter, after the
     * user has pressed the AI button and possibly typed in the meantime.
     */
    private fun setRow() {
        composeRule.setContent {
            var points by remember { mutableStateOf<Int?>(null) }
            var minutes by remember { mutableStateOf<Int?>(null) }
            GoalPilotTheme {
                AddTaskRow(
                    isScoring = false,
                    suggestedPoints = points,
                    suggestedMinutes = minutes,
                    onSuggestPoints = { title ->
                        suggestRequestedFor = title
                        points = pendingPoints
                        minutes = pendingMinutes
                    },
                    onSuggestionApplied = { points = null; minutes = null },
                    onAdd = { t, p, m, s -> added += Added(t, p, m, s) },
                )
            }
        }
    }

    private var pendingPoints: Int? = 20
    private var pendingMinutes: Int? = 90

    private fun typeTitle(text: String) =
        composeRule.onNodeWithText("Add a task").performTextReplacement(text)

    private fun pressEstimate() =
        composeRule.onNodeWithContentDescription("Estimate points with AI").performClick()

    private fun pressAdd() =
        composeRule.onNodeWithContentDescription("Add task").performClick()

    private fun box() = composeRule.onNodeWithTag(DURATION_BOX_TAG)

    private fun icon() = composeRule.onNodeWithContentDescription(AI_ESTIMATE_ICON_LABEL)

    /**
     * The box holds no number.
     *
     * **Not** `assertTextContains("How long?")`, which is what this file asserted
     * first and what three of these tests failed on: an `OutlinedTextField` renders
     * its *placeholder* only while focused, and its **label** otherwise — so the
     * empty box reads `Text = [Minutes]`, `EditableText = ''`. The placeholder
     * assertion therefore passed or failed on where the cursor happened to be, which
     * is a property of the test and not of the box. Read the value instead.
     */
    private fun assertBoxIsEmpty() {
        val typed = box().fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
        assertThat(typed).isEmpty()
    }

    // ── R8: the icon appears and disappears ─────────────────────────

    @Test
    fun theIconIsThereBeforeAnyoneHasTypedANumber() {
        setRow()

        icon().assertIsDisplayed()
        assertBoxIsEmpty()
    }

    @Test
    fun theIconSurvivesAnAiEstimate_becauseTheEstimateIsNotTheUsersNumber() {
        setRow()
        typeTitle("Run five kilometres before work")

        pressEstimate()

        box().assertTextContains("90")
        // R8 is worded as "has not entered a number", not "has no number".
        icon().assertIsDisplayed()
    }

    @Test
    fun theIconGoesTheMomentThePersonTypesANumber() {
        setRow()

        box().performTextInput("45")

        icon().assertDoesNotExist()
    }

    @Test
    fun clearingTheBoxBringsTheIconBack() {
        setRow()
        box().performTextInput("45")

        box().performTextClearance()

        icon().assertIsDisplayed()
    }

    // ── §1.4: the typed value wins, in the UI and not only in the domain ──

    @Test
    fun anEstimateDoesNotOverwriteATypedDuration() {
        setRow()
        typeTitle("Run five kilometres before work")
        box().performTextInput("45")

        pressEstimate()

        // The call still went out — this is not "the button stopped working".
        assertThat(suggestRequestedFor).isEqualTo("Run five kilometres before work")
        box().assertTextContains("45")
        icon().assertDoesNotExist()
    }

    @Test
    fun theOtherDirection_anUntypedBoxTakesTheEstimate() {
        setRow()
        typeTitle("Run five kilometres before work")

        pressEstimate()

        box().assertTextContains("90")
    }

    @Test
    fun aTypedDurationSurvivesARetitle() {
        setRow()
        box().performTextInput("45")

        typeTitle("An entirely different task")

        box().assertTextContains("45")
    }

    @Test
    fun anAiEstimateDoesNotSurviveARetitle() {
        setRow()
        typeTitle("Run five kilometres before work")
        pressEstimate()
        box().assertTextContains("90")

        typeTitle("An entirely different task")

        assertBoxIsEmpty()
    }

    // ── What is actually written ────────────────────────────────────

    @Test
    fun aTypedDurationIsHandedBackStampedAsTheUsers() {
        setRow()
        typeTitle("Swim")
        box().performTextInput("45")

        pressAdd()

        assertThat(added).hasSize(1)
        assertThat(added.single().minutes).isEqualTo(45)
        assertThat(added.single().source).isEqualTo(DurationSource.USER)
    }

    @Test
    fun anEstimatedDurationIsHandedBackStampedAsTheAis() {
        setRow()
        typeTitle("Run five kilometres before work")
        pressEstimate()

        pressAdd()

        assertThat(added.single().minutes).isEqualTo(90)
        assertThat(added.single().source).isEqualTo(DurationSource.AI)
    }

    @Test
    fun aSkippedBoxIsStoredAsTheDefaultAndAsNobodysAnswer() {
        // §3.4: DEFAULT_MINUTES if skipped — and recorded as a skip, so the task
        // stays re-estimable and is never counted among the AI's durations.
        setRow()
        typeTitle("Swim")

        pressAdd()

        assertThat(added.single().minutes).isEqualTo(30)
        assertThat(added.single().source).isEqualTo(DurationSource.UNKNOWN)
    }

    @Test
    fun aModelThatAnswersWithNoDurationLeavesTheBoxEmptyRatherThanGuessing() {
        // The concrete #9 change: this used to fill in `points × 3`, storing a
        // word-count derivative as though the model had said it.
        pendingMinutes = null
        setRow()
        typeTitle("Run five kilometres before work")

        pressEstimate()

        assertBoxIsEmpty()
        pressAdd()
        assertThat(added.single().source).isEqualTo(DurationSource.UNKNOWN)
    }

    @Test
    fun addingClearsTheBoxSoTheNextTaskDoesNotInheritTheLastOnesDuration() {
        setRow()
        typeTitle("Swim")
        box().performTextInput("45")
        pressAdd()

        assertBoxIsEmpty()
        icon().assertIsDisplayed()
    }
}
