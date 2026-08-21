package com.idomarhaim.goalpilot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddCard
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddState
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddTestTags
import com.idomarhaim.goalpilot.feature.goals.ALREADY_DONE_LABEL
import com.idomarhaim.goalpilot.feature.goals.ALREADY_DONE_TAG
import com.idomarhaim.goalpilot.feature.goals.AddTaskRow
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * `#7`'s create-and-complete affordance on both add surfaces —
 * [#7](https://github.com/idomarhaim/Android_Final_Project/issues/7), `R6`.
 *
 * **What this owns and what it does not.** `TaskCompletionTest` owns the invariant — that a
 * done task leaves the repository carrying its timestamp, and that the weekly summary and the
 * time chart can then see it. What no JVM test can reach is the half living in two
 * composables: whether the toggle is **there**, whether pressing add actually **passes** what
 * it holds, and whether it **clears** afterwards.
 *
 * The last of those is the one worth having. A chip that stayed selected across an add is a
 * silent mode: the next task typed is completed without anybody asking for it, and every other
 * layer stays green, because the value handed to the ViewModel is *correct* — correct about a
 * state the user did not intend to still be in. It is a defect in what the screen remembers,
 * which is exactly what a domain test cannot hold.
 *
 * Both surfaces are covered because `#7` puts the same control on both, and an affordance that
 * silently regressed on one of them is the asymmetry the ticket set out not to ship.
 *
 * Stateless and lambda-driven, so it needs no Firebase and no Hilt — the same shape as
 * [DurationBoxUiTest] and [SilentFilingUiTest].
 */
class AlreadyDoneUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── The dashboard quick-add card ─────────────────────────────────

    private val sorted = mutableListOf<Pair<String, Boolean>>()

    private fun setCard(state: SmartAddState = SmartAddState()) {
        composeRule.setContent {
            GoalPilotTheme {
                SmartAddCard(state = state, onClassify = { t, done -> sorted += t to done })
            }
        }
    }

    private fun typeQuickAdd(text: String) =
        composeRule.onNodeWithText("e.g. Run 5 km on Friday").performTextInput(text)

    private fun quickAddChip() = composeRule.onNodeWithTag(SmartAddTestTags.ALREADY_DONE)

    @Test
    fun theQuickAddCardOffersTheAffordanceInWords() {
        setCard()

        quickAddChip().assertIsDisplayed()
        // §0.8's surviving sub-rule — form and words before iconography. A bare tick mark in
        // the input row would be smaller and would say nothing about what it does.
        composeRule.onNodeWithText(SmartAddTestTags.ALREADY_DONE_LABEL).assertIsDisplayed()
    }

    @Test
    fun theAffordanceStartsOff() {
        setCard()

        // Worth asserting rather than assuming: the opposite default would silently complete
        // every task ever typed into the app.
        quickAddChip().assertIsNotSelected()
    }

    @Test
    fun anOrdinaryQuickAddIsStillNotDone() {
        setCard()

        typeQuickAdd("Run 5 km")
        composeRule.onNodeWithText("Sort").performClick()

        assertThat(sorted).containsExactly("Run 5 km" to false)
    }

    @Test
    fun tickingTheChipCompletesTheTaskInTheSameAction() {
        setCard()

        typeQuickAdd("Run 5 km")
        quickAddChip().performClick()
        composeRule.onNodeWithText("Sort").performClick()

        // `R6` in one line: one action, and the task is filed *and* finished. The four
        // navigations the ticket names are gone because there is nothing left to navigate to.
        assertThat(sorted).containsExactly("Run 5 km" to true)
    }

    @Test
    fun theChipClearsAfterAnAddSoTheNextTaskIsNotSilentlyCompleted() {
        // THE ONE THAT MATTERS. A chip left selected is a mode, and a mode that completes
        // whatever is typed next is the app doing something unasked and unannounced — which
        // §0.7 does not permit even for filing, let alone for asserting that work was done.
        setCard()

        typeQuickAdd("Run 5 km")
        quickAddChip().performClick()
        composeRule.onNodeWithText("Sort").performClick()

        quickAddChip().assertIsNotSelected()

        typeQuickAdd("Read a chapter")
        composeRule.onNodeWithText("Sort").performClick()

        assertThat(sorted).containsExactly("Run 5 km" to true, "Read a chapter" to false).inOrder()
    }

    @Test
    fun theChipCanBeTickedAndUnticked() {
        setCard()

        quickAddChip().performClick()
        quickAddChip().assertIsSelected()
        quickAddChip().performClick()
        quickAddChip().assertIsNotSelected()
    }

    @Test
    fun theInFlightRowSaysACompletionIsBeingWritten() {
        // The chip has already cleared on the tap that started this, so for the length of a
        // round trip this row is the only thing on screen that agrees a completion is in
        // flight. Without it the user's own action disappears for a second or more.
        setCard(SmartAddState(isClassifying = true, taskTitle = "Run 5 km", alreadyDone = true))

        composeRule.onNodeWithTag(SmartAddTestTags.SORTING).assertIsDisplayed()
        composeRule.onNodeWithText("Filing “Run 5 km” as done…").assertIsDisplayed()
    }

    @Test
    fun anOrdinaryFilingDoesNotClaimToHaveCompletedAnything() {
        setCard(SmartAddState(isClassifying = true, taskTitle = "Run 5 km"))

        composeRule.onNodeWithText("Filing “Run 5 km”…").assertIsDisplayed()
        composeRule.onNodeWithText("Filing “Run 5 km” as done…").assertDoesNotExist()
    }

    // ── The goal-detail add row ──────────────────────────────────────

    private val addedDone = mutableListOf<Pair<String, Boolean>>()

    private fun setRow() {
        composeRule.setContent {
            var difficulty by remember { mutableStateOf<Difficulty?>(null) }
            GoalPilotTheme {
                AddTaskRow(
                    isScoring = false,
                    suggestedDifficulty = difficulty,
                    suggestedMinutes = null,
                    onSuggestEstimate = { difficulty = null },
                    onSuggestionApplied = { difficulty = null },
                    // `#56` added the sixth parameter, the occurrence. This suite is about
                    // `#7`'s already-done flag, so it ignores it; `WhenPickerUiTest` owns it.
                    onAdd = { t, _: Difficulty, _, _: DurationSource, done, _ -> addedDone += t to done },
                )
            }
        }
    }

    private fun rowChip() = composeRule.onNodeWithTag(ALREADY_DONE_TAG)

    private fun typeRowTitle(text: String) =
        composeRule.onNodeWithText("Add a task").performTextReplacement(text)

    private fun pressAdd() =
        composeRule.onNodeWithContentDescription("Add task").performClick()

    @Test
    fun theGoalDetailRowCarriesTheSameAffordance() {
        setRow()

        rowChip().assertIsDisplayed()
        composeRule.onNodeWithText(ALREADY_DONE_LABEL).assertIsDisplayed()
        // The same words on both surfaces, asserted rather than assumed: an add affordance
        // present on one add row and worded differently on the other reads as two features.
        assertThat(ALREADY_DONE_LABEL).isEqualTo(SmartAddTestTags.ALREADY_DONE_LABEL)
    }

    @Test
    fun anOrdinaryTaskAddedFromAGoalIsNotDone() {
        setRow()

        typeRowTitle("Run 5 km")
        pressAdd()

        assertThat(addedDone).containsExactly("Run 5 km" to false)
    }

    @Test
    fun tickingTheChipAddsTheTaskAlreadyCompleted() {
        setRow()

        typeRowTitle("Run 5 km")
        rowChip().performClick()
        pressAdd()

        assertThat(addedDone).containsExactly("Run 5 km" to true)
    }

    @Test
    fun theGoalDetailChipAlsoClearsAfterAnAdd() {
        // Same mode hazard, same answer. Logging three runs you already did costs three taps
        // of this chip rather than one, and that is the trade the ticket takes deliberately.
        setRow()

        typeRowTitle("Run 5 km")
        rowChip().performClick()
        pressAdd()

        rowChip().assertIsNotSelected()

        typeRowTitle("Plan next week")
        pressAdd()

        assertThat(addedDone)
            .containsExactly("Run 5 km" to true, "Plan next week" to false)
            .inOrder()
    }
}
