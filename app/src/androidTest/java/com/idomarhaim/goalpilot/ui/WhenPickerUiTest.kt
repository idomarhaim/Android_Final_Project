package com.idomarhaim.goalpilot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceDraft
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.feature.goals.NO_WHEN_LABEL
import com.idomarhaim.goalpilot.feature.goals.WHEN_CHIP_TAG
import com.idomarhaim.goalpilot.feature.goals.WHEN_CLEAR_TAG
import com.idomarhaim.goalpilot.feature.goals.WHEN_TIME_TAG
import com.idomarhaim.goalpilot.feature.goals.WhenPicker
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The add row's ***when*** control — the only way a person can put a task on §2.2's rungs
 * ([#56](https://github.com/idomarhaim/Android_Final_Project/issues/56)).
 *
 * ### What this layer can see that the JVM cannot
 *
 * `OccurrenceTest` already proves the rule — a date alone is an `ALL_DAY`, a date with a time
 * is a `DEADLINE`. What it cannot prove is that the **controls reach that rule**: that the time
 * button only appears once there is a day to hang a time on, that pressing it a second time
 * demotes the rung rather than clearing the date, and that clearing really does return the row
 * to *no when at all* rather than to a draft carrying a stale time.
 *
 * Those are the transitions where a person can end up with a rung they did not choose, and
 * §2.2 discriminates rungs by **what a miss means** — so the cost of getting one wrong is that
 * the app tells them they are *late, still owed* when they meant *the day passed*.
 *
 * The dialogs themselves are not driven here: they are Material's, and this app's only claim
 * about them is that they go through `ui/locale`, which `DialogLocaleGuardTest` asserts
 * statically over the source. What is driven is everything around them.
 */
class WhenPickerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day: LocalDate = LocalDate.of(2026, 8, 22)

    private var draft by mutableStateOf(OccurrenceDraft())

    @Composable
    private fun Subject() {
        GoalPilotTheme {
            WhenPicker(draft = draft, onChange = { draft = it })
        }
    }

    private fun occurrence(): Occurrence? = draft.toOccurrence()

    @Test
    fun anEmptyRowOffersOnlyTheWhenChipAndProducesNoOccurrence() {
        draft = OccurrenceDraft()
        composeRule.setContent { Subject() }

        composeRule.onNodeWithText(NO_WHEN_LABEL).assertIsDisplayed()
        // Neither the time nor the clear control exists yet: a time with no date is not a
        // deadline, so offering the button would be offering a tap that does nothing.
        composeRule.assertTagCount(WHEN_TIME_TAG, expected = 0)
        composeRule.assertTagCount(WHEN_CLEAR_TAG, expected = 0)
        assertThat(occurrence()).isNull()
    }

    @Test
    fun aDayAloneIsAnAllDayAndTheTimeControlThenAppears() {
        draft = OccurrenceDraft().withDate(day)
        composeRule.setContent { Subject() }

        composeRule.onNodeWithTag(WHEN_CHIP_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(WHEN_TIME_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(WHEN_CLEAR_TAG).assertIsDisplayed()

        assertThat(occurrence()).isEqualTo(AllDay(day))
        assertThat(occurrence()!!.missState).isEqualTo(OccurrenceState.DAY_PASSED)
    }

    @Test
    fun addingATimePromotesTheRungAndPressingAgainDemotesItWithoutLosingTheDay() {
        draft = OccurrenceDraft().withDate(day).withTime(LocalTime.of(6, 0))
        composeRule.setContent { Subject() }

        assertThat(occurrence()).isEqualTo(Deadline(day.atTime(6, 0)))
        assertThat(occurrence()!!.missState).isEqualTo(OccurrenceState.OVERDUE)

        // The second press is the DEMOTION, and it must keep the day. Clearing the date here
        // instead would look almost right on screen and silently unschedule the task.
        composeRule.onNodeWithTag(WHEN_TIME_TAG).performClick()

        composeRule.waitForIdle()
        assertThat(draft.date).isEqualTo(day)
        assertThat(draft.time).isNull()
        assertThat(occurrence()).isEqualTo(AllDay(day))
        assertThat(occurrence()!!.missState).isEqualTo(OccurrenceState.DAY_PASSED)
    }

    @Test
    fun clearingReturnsTheRowToNoWhenAtAll() {
        draft = OccurrenceDraft().withDate(day).withTime(LocalTime.of(6, 0))
        composeRule.setContent { Subject() }

        composeRule.onNodeWithTag(WHEN_CLEAR_TAG).performClick()
        composeRule.waitForIdle()

        // Not merely "no date": a leftover time would be invisible on screen and would turn the
        // next date the user picks into a deadline they never asked for.
        assertThat(draft.date).isNull()
        assertThat(draft.time).isNull()
        assertThat(occurrence()).isNull()
        composeRule.onNodeWithText(NO_WHEN_LABEL).assertIsDisplayed()
    }
}

/**
 * Asserts how many nodes carry [tag].
 *
 * Counting rather than `assertDoesNotExist`: the honest form for a tag that is absent from the
 * tree entirely, and it also fails loudly if a control is ever duplicated.
 */
private fun ComposeContentTestRule.assertTagCount(tag: String, expected: Int) {
    assertThat(onAllNodesWithTag(tag).fetchSemanticsNodes()).hasSize(expected)
}
