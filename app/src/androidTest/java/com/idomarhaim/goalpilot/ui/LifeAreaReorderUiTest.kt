package com.idomarhaim.goalpilot.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.feature.lifeareas.LifeAreaRow
import com.idomarhaim.goalpilot.feature.lifeareas.lifeAreaRows
import com.idomarhaim.goalpilot.feature.lifeareas.rememberLifeAreaReorderState
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for drag-to-reorder on the life-areas list.
 *
 * The rows are driven straight from `lifeAreaRows`, so this needs no Firebase and
 * no Hilt — the same shape as [DonutChartUiTest]. What it is really pinning is the
 * wiring between a gesture and the `(from, to)` pair the repository is asked to
 * persist: the arithmetic that pair feeds is covered on the JVM by
 * `LifeAreaOrderingTest`.
 */
class LifeAreaReorderUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rows = listOf("Health", "Studies", "Career").mapIndexed { index, name ->
        LifeAreaRow(
            area = LifeArea(id = name.lowercase(), name = name, sortOrder = index),
            goalCount = index,
        )
    }

    private val moves = mutableListOf<Pair<Int, Int>>()

    @Composable
    private fun Subject(rows: List<LifeAreaRow>) {
        GoalPilotTheme {
            val state = rememberLifeAreaReorderState()
            LaunchedEffect(rows) { state.sync(rows) }
            LazyColumn(state = state.listState) {
                lifeAreaRows(
                    state = state,
                    onMove = { from, to -> moves += from to to },
                    // #2 made the whole card a click target; this suite is about
                    // reordering, so the route is stubbed rather than asserted.
                    onOpen = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
    }

    private fun customActions(areaName: String): List<CustomAccessibilityAction> =
        composeRule.onNodeWithContentDescription("Reorder $areaName")
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

    @Test
    fun handles_areOfferedForEveryAreaWhenThereIsMoreThanOne() {
        composeRule.setContent { Subject(rows) }

        composeRule.onNodeWithContentDescription("Reorder Health").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reorder Studies").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reorder Career").assertIsDisplayed()
    }

    @Test
    fun handle_isWithheldFromASingleArea() {
        composeRule.setContent { Subject(rows.take(1)) }

        // A handle that cannot move anything is worse than no handle.
        composeRule.onNodeWithContentDescription("Reorder Health").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Edit Health").assertIsDisplayed()
    }

    @Test
    fun accessibilityActions_moveTheAreaOneSlotAtATime() {
        composeRule.setContent { Subject(rows) }

        val middle = customActions("Studies")
        composeRule.runOnUiThread { middle.first { it.label == "Move up" }.action() }
        composeRule.runOnUiThread { middle.first { it.label == "Move down" }.action() }

        assertThat(moves).containsExactly(1 to 0, 1 to 2).inOrder()
    }

    @Test
    fun accessibilityActions_stopAtTheEndsOfTheList() {
        composeRule.setContent { Subject(rows) }

        // Nothing above the first area, nothing below the last.
        assertThat(customActions("Health").map { it.label }).containsExactly("Move down")
        assertThat(customActions("Career").map { it.label }).containsExactly("Move up")
    }

    @Test
    fun dragging_theFirstHandleOntoTheSecondCommitsThatMove() {
        composeRule.setContent { Subject(rows) }

        val first = composeRule.onNodeWithContentDescription("Reorder Health")
            .fetchSemanticsNode().positionInRoot.y
        val second = composeRule.onNodeWithContentDescription("Reorder Studies")
            .fetchSemanticsNode().positionInRoot.y
        val rowHeight = second - first

        composeRule.onNodeWithContentDescription("Reorder Health").performTouchInput {
            down(center)
            // A drag only starts after the long press, and the first movement has
            // to clear touch slop before the detector reports anything.
            moveBy(Offset(0f, 4f), delayMillis = 1_000)
            moveBy(Offset(0f, rowHeight * 0.5f), delayMillis = 32)
            moveBy(Offset(0f, rowHeight * 0.5f), delayMillis = 32)
            up()
        }
        composeRule.waitForIdle()

        assertThat(moves).containsExactly(0 to 1)
    }
}
