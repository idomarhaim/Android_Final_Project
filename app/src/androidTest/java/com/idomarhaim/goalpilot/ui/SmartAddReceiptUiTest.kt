package com.idomarhaim.goalpilot.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddReceipt
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddReceiptSnackbar
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * `#6`'s witness actually reaching the screen — [SmartAddReceiptSnackbar].
 *
 * **This suite exists because of a defect that shipped past every other layer.** §0.7 permits
 * filing without asking *only* because the filing is visible afterwards and undoable; the
 * snackbar is that visibility, so a snackbar that never renders does not degrade the feature, it
 * removes the thing that made the silence legitimate. The first version consumed the receipt
 * before awaiting `showSnackbar`, which nulls the state the effect is keyed on — `LaunchedEffect`
 * restarts and cancels the coroutine before it can show anything. JVM unit, functions, emulator
 * and the isolated Compose tests were all green; **the device was the only instrument that could
 * see it**, because the bug is in a coroutine's lifetime and not in any value.
 *
 * So these cases drive the **real** composable inside a real `Scaffold`, not a copy of its six
 * lines. A copy would have been written from the same wrong understanding and passed.
 */
class SmartAddReceiptUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun receipt(id: String, goalTitle: String) = SmartAddReceipt(
        taskId = id,
        taskTitle = "Bench press 30 minutes",
        decision = FilingDecision.ExistingGoal("g-$id", goalTitle),
    )

    private var undone: SmartAddReceipt? = null
    private var consumed = 0

    /** The wiring as `DashboardScreen` has it: a Scaffold, its host, and the effect. */
    private fun setHost(initial: SmartAddReceipt?): () -> Unit {
        var current by mutableStateOf(initial)
        composeRule.setContent {
            GoalPilotTheme {
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { SnackbarHost(host) }) {
                    SmartAddReceiptSnackbar(
                        receipt = current,
                        hostState = host,
                        onUndo = { undone = it },
                        // Nulls the state the effect is keyed on, because that is what
                        // `DashboardViewModel.consumeFiled` does — and it is the ENTIRE
                        // mechanism of the defect. Measured 2026-08-20: with this lambda as an
                        // inert counter, `aFilingIsAnnouncedAtAll` passed against the broken
                        // order. An instrument that does not model the consume cannot see a bug
                        // whose cause is the consume.
                        onConsume = { consumed++; current = null },
                    )
                }
            }
        }
        return { current = receipt("2", "Sleep 7 hours") }
    }

    @Test
    fun aFilingIsAnnouncedAtAll() {
        // The regression, end to end. `Observed:` 2026-08-20 — red against the broken order and
        // green against the fix, both runs on `Pixel_10_Pro_XL`, and it only became red once the
        // harness above modelled the consume. Verified in both directions rather than assumed.
        setHost(receipt("1", "Strength Training"))

        composeRule.onNodeWithText("Added to “Strength Training”").assertIsDisplayed()
    }

    @Test
    fun theAnnouncementCarriesAnUndo() {
        setHost(receipt("1", "Strength Training"))

        composeRule.onNodeWithText("Undo").assertIsDisplayed()
    }

    @Test
    fun theReceiptIsNotConsumedBeforeItIsShown() {
        // Naming the mechanism rather than the symptom: the old order called `onConsume` first,
        // which nulls the key this effect is keyed on. Nothing may be consumed while the
        // snackbar is still up.
        setHost(receipt("1", "Strength Training"))
        composeRule.onNodeWithText("Added to “Strength Training”").assertIsDisplayed()

        assertThat(consumed).isEqualTo(0)
    }

    @Test
    fun undoReportsTheReceiptItWasShownFor() {
        setHost(receipt("1", "Strength Training"))

        composeRule.onNodeWithText("Undo").performClick()

        assertThat(undone?.taskId).isEqualTo("1")
        // Undo owns the clearing, so the effect must not also consume — that would be two
        // writers of one piece of state, racing.
        assertThat(consumed).isEqualTo(0)
    }

    @Test
    fun noReceiptSaysNothing() {
        setHost(null)

        composeRule.onNodeWithText("Undo").assertDoesNotExist()
        assertThat(consumed).isEqualTo(0)
    }

    @Test
    fun aSecondFilingReplacesTheFirstAnnouncementRatherThanQueueingBehindIt() {
        // The concern that motivated the wrong order in the first place, answered by the KEY
        // rather than by consuming early: a new receipt restarts the effect, which dismisses the
        // stale snackbar and shows the current one.
        val fileASecond = setHost(receipt("1", "Strength Training"))
        composeRule.onNodeWithText("Added to “Strength Training”").assertIsDisplayed()

        composeRule.runOnUiThread { fileASecond() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Added to “Sleep 7 hours”").assertIsDisplayed()
        composeRule.onNodeWithText("Added to “Strength Training”").assertDoesNotExist()
    }

    @Test
    fun theBranchThatSpeaksSaysSomethingDifferent() {
        composeRule.setContent {
            GoalPilotTheme {
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { SnackbarHost(host) }) {
                    SmartAddReceiptSnackbar(
                        receipt = SmartAddReceipt(
                            taskId = "t",
                            taskTitle = "Learn to sail",
                            decision = FilingDecision.NewGoal(
                                title = "Sailing",
                                category = GoalCategory.OTHER,
                                lifeAreaId = null,
                            ),
                            createdGoalId = "g-new",
                        ),
                        hostState = host,
                        onUndo = { undone = it },
                        onConsume = { consumed++ },
                    )
                }
            }
        }

        // It TELLS — it does not ask, and it does not claim to have filed under something that
        // fitted. §3.4's one speaking row.
        composeRule.onNodeWithText("No goal fitted — suggested “Sailing”").assertIsDisplayed()
        composeRule.onNodeWithText("Add this task?").assertDoesNotExist()
    }

    // ── `#7` — the receipt for a task that was also completed ─────────

    /**
     * Drives the same real composable with a receipt whose `completed` flag is set.
     *
     * **Why the witness has to carry this at all.** §0.7 permits filing without asking only
     * because the app says afterwards what it did, and with `#7` it did two things: it filed
     * the task, and it recorded the task as finished. The filed task sits under a goal the
     * user is not looking at, so its tick is not on screen either — this snackbar is the only
     * place the dashboard can show that the completion took. A receipt that said only
     * *"Added to X"* would be silent about the half the user pressed the chip for.
     */
    private fun setDoneReceipt(decision: FilingDecision, taskTitle: String = "Bench press") {
        composeRule.setContent {
            GoalPilotTheme {
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { SnackbarHost(host) }) {
                    SmartAddReceiptSnackbar(
                        receipt = SmartAddReceipt(
                            taskId = "t",
                            taskTitle = taskTitle,
                            decision = decision,
                            completed = true,
                        ),
                        hostState = host,
                        onUndo = { undone = it },
                        onConsume = { consumed++ },
                    )
                }
            }
        }
    }

    @Test
    fun aTaskFiledAndCompletedLeadsWithTheCompletion() {
        setDoneReceipt(FilingDecision.ExistingGoal("g1", "Strength Training"))

        // "Done" first, because that is the news — the user typed this in BECAUSE it was
        // already finished, and where it was filed is the secondary fact.
        composeRule.onNodeWithText("Done — added to “Strength Training”").assertIsDisplayed()
        composeRule.onNodeWithText("Added to “Strength Training”").assertDoesNotExist()
    }

    @Test
    fun aCompletedTaskThatFittedNoGoalSaysBothThings() {
        setDoneReceipt(
            FilingDecision.NewGoal(title = "Sailing", category = GoalCategory.OTHER, lifeAreaId = null),
        )

        composeRule.onNodeWithText("Done — no goal fitted, suggested “Sailing”").assertIsDisplayed()
    }

    @Test
    fun aCompletedTaskWithNoGoalAtAllSaysSo() {
        setDoneReceipt(FilingDecision.NoGoal(suggestedLifeAreaId = null), taskTitle = "Tidy the shed")

        composeRule.onNodeWithText("Done — “Tidy the shed” fits no goal yet").assertIsDisplayed()
    }

    @Test
    fun aCompletedFilingIsStillUndoableInOneTap() {
        // Undo needs no second offer for the completion: deleting the task takes its
        // `done`/`completedAt` pair with it, which is the whole reason `#7` writes the fact
        // onto the task rather than into a second place.
        setDoneReceipt(FilingDecision.ExistingGoal("g1", "Strength Training"))

        composeRule.onNodeWithText("Undo").performClick()

        assertThat(undone?.taskId).isEqualTo("t")
        assertThat(undone?.completed).isTrue()
    }
}
