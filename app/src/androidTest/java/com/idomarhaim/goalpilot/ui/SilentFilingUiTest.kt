package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddCard
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddState
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddTestTags
import com.idomarhaim.goalpilot.feature.goals.GoalListRow
import com.idomarhaim.goalpilot.feature.goals.GoalsTestTags
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * `#6`'s two surfaces on the real Compose runtime —
 * [#6](https://github.com/idomarhaim/Android_Final_Project/issues/6).
 *
 * **What this owns and what it does not.** `SmartFilingTest` owns the branch table as
 * arithmetic and `GoalDeclaredByMigrationTest` owns the marker's round trip; neither can prove
 * that the *screens* read them. That is exactly where this ticket's defect would live, because
 * both of §0.7's obligations are visual: the app must not put a question in front of Ido, and
 * it must show him afterwards what it did. A green domain test is compatible with a screen that
 * shows the banner to every goal, or to none.
 *
 * Stateless and lambda-driven, so it needs no Firebase and no Hilt — the same shape as
 * [DurationBoxUiTest].
 */
class SilentFilingUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun goal(declaredBy: DeclaredBy?) = Goal(
        id = "g1",
        title = "Run a half marathon",
        category = GoalCategory.FITNESS,
        declaredBy = declaredBy,
    )

    private var kept = 0
    private var demoted = 0

    private fun setRow(declaredBy: DeclaredBy?) {
        composeRule.setContent {
            GoalPilotTheme {
                GoalListRow(
                    goal = goal(declaredBy),
                    onOpen = {},
                    onKeep = { kept++ },
                    onDemote = { demoted++ },
                )
            }
        }
    }

    // ── The pending-goal surface ─────────────────────────────────────

    @Test
    fun anAiSuggestedGoalIsMarkedAsSuggested() {
        setRow(DeclaredBy.AI_SUGGESTED)

        composeRule.onNodeWithTag(GoalsTestTags.SUGGESTED_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(GoalsTestTags.KEEP).assertIsDisplayed()
        composeRule.onNodeWithTag(GoalsTestTags.DEMOTE).assertIsDisplayed()
        // The goal itself is on the list, holding its task — pending is not hidden (§1.1).
        composeRule.onNodeWithText("Run a half marathon").assertIsDisplayed()
    }

    @Test
    fun aGoalIdoDeclaredCarriesNoBanner() {
        setRow(DeclaredBy.USER)

        composeRule.onNodeWithTag(GoalsTestTags.SUGGESTED_BANNER).assertDoesNotExist()
    }

    @Test
    fun aGoalWrittenBeforeTheFieldCarriesNoBanner() {
        // The whole of goalpilot-56e30 reads as UNKNOWN (§7.1). If UNKNOWN rendered as pending,
        // this ticket would ask Ido to re-declare every goal he has ever made.
        setRow(DeclaredBy.UNKNOWN)

        composeRule.onNodeWithTag(GoalsTestTags.SUGGESTED_BANNER).assertDoesNotExist()
    }

    @Test
    fun anAlreadyDemotedGoalCarriesNoBanner() {
        // The marker was dropped, so there is nothing left to rule on — and the goal is still
        // rendered, which is what makes the demotion lossless from where Ido is standing.
        setRow(null)

        composeRule.onNodeWithTag(GoalsTestTags.SUGGESTED_BANNER).assertDoesNotExist()
        composeRule.onNodeWithText("Run a half marathon").assertIsDisplayed()
    }

    // ── The two answers ──────────────────────────────────────────────

    @Test
    fun keepingASuggestionReportsItOnce() {
        setRow(DeclaredBy.AI_SUGGESTED)

        composeRule.onNodeWithTag(GoalsTestTags.KEEP).performClick()

        assertThat(kept).isEqualTo(1)
        assertThat(demoted).isEqualTo(0)
    }

    @Test
    fun theLosslessDemotionIsOneTapAndIsNotADelete() {
        setRow(DeclaredBy.AI_SUGGESTED)

        composeRule.onNodeWithTag(GoalsTestTags.DEMOTE).performClick()

        assertThat(demoted).isEqualTo(1)
        assertThat(kept).isEqualTo(0)
        // The banner offers exactly two answers. A `Delete` here would take the task Ido typed
        // in with it, which is the one thing §1.1's demotion exists to avoid.
        composeRule.onNodeWithText("Delete").assertDoesNotExist()
    }

    // ── The quick-add card, with the dialog gone ─────────────────────

    @Test
    fun sortingAtaskShowsProgressInPlaceRatherThanADialog() {
        composeRule.setContent {
            GoalPilotTheme {
                SmartAddCard(
                    state = SmartAddState(isClassifying = true, taskTitle = "Run 5 km on Friday"),
                    onClassify = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag(SmartAddTestTags.SORTING).assertIsDisplayed()
        // What used to be here was a modal asking "Add this task?" with Add and Cancel. R3 asked
        // for it to stop asking and §0.7 says it never should have; these two assertions are the
        // ticket, stated as what the user can no longer be shown.
        composeRule.onNodeWithText("Add this task?").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun anIdleCardSaysNothingAboutSorting() {
        composeRule.setContent {
            GoalPilotTheme { SmartAddCard(state = SmartAddState(), onClassify = { _, _ -> }) }
        }

        composeRule.onNodeWithTag(SmartAddTestTags.SORTING).assertDoesNotExist()
    }

    @Test
    fun typingAtaskAndPressingSortFilesItWithNoConfirmationStep() {
        var sorted: String? = null
        composeRule.setContent {
            GoalPilotTheme {
                // `#7` added the second parameter; this case is `#6`'s no-confirmation
                // path, so it reads only the title.
                SmartAddCard(state = SmartAddState(), onClassify = { t, _ -> sorted = t })
            }
        }

        composeRule.onNodeWithText("e.g. Run 5 km on Friday").performTextInput("Run 5 km")
        composeRule.onNodeWithText("Sort").performClick()

        // One tap, one call out, and nothing in between: the classification goes straight to
        // disk. There is no second button anywhere in this flow to press.
        assertThat(sorted).isEqualTo("Run 5 km")
        composeRule.onNodeWithText("Add this task?").assertDoesNotExist()
    }
}
