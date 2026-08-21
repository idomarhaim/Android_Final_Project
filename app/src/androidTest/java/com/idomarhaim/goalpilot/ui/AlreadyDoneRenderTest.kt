package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddCard
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddState
import com.idomarhaim.goalpilot.feature.dashboard.SmartAddTestTags
import com.idomarhaim.goalpilot.feature.goals.ALREADY_DONE_TAG
import com.idomarhaim.goalpilot.feature.goals.AddTaskRow
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for [#7](https://github.com/idomarhaim/Android_Final_Project/issues/7),
 * as a **file a human can open** rather than an assertion.
 *
 * `R6` asks for *a way* to complete a task from inside the add flow, and every remaining
 * question about that way is visual. [AlreadyDoneUiTest] proves the chip is present, passes
 * what it holds and clears afterwards; none of that answers whether it **reads** as an
 * affordance, whether its selected state is distinguishable from its unselected one at a
 * glance, or whether adding it crowds a card that was already a sentence, a field and a
 * button. Those are judged by looking.
 *
 * Two things this picture exists to let somebody rule on:
 *
 *  1. **Selected vs unselected, side by side.** A toggle whose two states look alike is worse
 *     than no toggle, because it silently completes tasks and looks like it did not. They are
 *     rendered adjacently for exactly that comparison — the same reason [DurationBoxRenderTest]
 *     puts its three states in one frame rather than driving one row through them.
 *  2. **The same control on both add surfaces.** `#7` decided the goal-detail row gets the
 *     affordance too, so that an add option present on one add row and absent from the other
 *     does not read as a bug. Whether the two actually look like one feature is a question the
 *     assertion comparing their two label constants cannot reach.
 *
 * Pulling the PNG needs `install -r` + `am instrument`, never
 * `./gradlew :app:connectedDebugAndroidTest` — that task **uninstalls the app** and takes the
 * external files dir, and the capture, with it. `kb/dev/android-device-verification.md` §8.
 *
 * ```bash
 * adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
 * adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb -s <serial> shell am instrument -w -e class \
 *     com.idomarhaim.goalpilot.ui.AlreadyDoneRenderTest \
 *     com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb -s <serial> pull \
 *     /storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/issue-7-quick-add.png
 * adb -s <serial> pull \
 *     /storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/issue-7-goal-detail-row.png
 * ```
 *
 * **Two frames, not one, and the reason is a measurement rather than taste.**
 *
 * The first version put all five states in one `Column`. It captured a PNG, it wrote a
 * file over the size floor, and the two width/height assertions passed — and the bottom
 * two cards were **not in it**: an unscrollable `Column` gives its overflowing children
 * zero height, so `add-task-already-done` came back as
 * `(l=84.0, t=3004.0, r=429.0, b=3004.0)px` and could not even be clicked.
 *
 * `Observed:` 2026-08-20 on `Pixel_10_Pro_XL`. What caught it was the floor assertion
 * asking whether the chip it had just tapped was **selected** — a check on the subject.
 * The three checks on the *artifact* (file length, bitmap width, bitmap height) were all
 * green against a picture missing the half it was made to show, which is
 * `kb/dev/look-at-your-own-output.md`'s point exactly: an instrument degrades silently on
 * the case it exists for. Assertions about a capture's *bytes* are not assertions about
 * its *contents*.
 */
class AlreadyDoneRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun Caption(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
        )
    }

    @Composable
    private fun RowCard() {
        GpCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                var difficulty by remember { mutableStateOf<Difficulty?>(null) }
                AddTaskRow(
                    isScoring = false,
                    suggestedDifficulty = difficulty,
                    suggestedMinutes = null,
                    onSuggestEstimate = { difficulty = null },
                    onSuggestionApplied = { difficulty = null },
                    onAdd = { _, _: Difficulty, _, _: DurationSource, _, _ -> },
                )
            }
        }
    }

    private fun capture(name: String): File {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)
        val out = File(dir, name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(200)
        return out
    }

    @Test
    fun theQuickAddCard_writtenToAPngIcanLookAt() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Column(Modifier.padding(16.dp)) {
                        Caption("1 · Ordinary — the chip is there and off")
                        SmartAddCard(state = SmartAddState(), onClassify = { _, _ -> })

                        Caption("2 · Already done — one tap, and Sort files it finished")
                        SmartAddCard(state = SmartAddState(), onClassify = { _, _ -> })

                        Caption("3 · In flight — the chip has cleared, so the row carries the news")
                        SmartAddCard(
                            state = SmartAddState(
                                isClassifying = true,
                                taskTitle = "Ran 5 km before work",
                                alreadyDone = true,
                            ),
                            onClassify = { _, _ -> },
                        )
                    }
                }
            }
        }

        // Driven, not constructed: the picture then shows what a person's own taps produce.
        composeRule.onAllNodesWithText("e.g. Run 5 km on Friday")[0].performTextInputAndSettle("Run 5 km")
        composeRule.onAllNodesWithText("e.g. Run 5 km on Friday")[1]
            .performTextInputAndSettle("Ran 5 km before work")
        composeRule.onAllNodesWithTag(SmartAddTestTags.ALREADY_DONE)[1].performClick()

        // The floor that actually holds: the state the frame exists to show is really in it,
        // and card 1 is really still in the other state, or there is nothing to compare.
        composeRule.onAllNodesWithTag(SmartAddTestTags.ALREADY_DONE)[0].assertIsNotSelected()
        composeRule.onAllNodesWithTag(SmartAddTestTags.ALREADY_DONE)[1].assertIsSelected()
        composeRule.onNodeWithText("Filing “Ran 5 km before work” as done…").assertExists()

        capture("issue-7-quick-add.png")
    }

    @Test
    fun theGoalDetailAddRow_writtenToAPngIcanLookAt() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Column(Modifier.padding(16.dp)) {
                        Caption("4 · The goal-detail add row — the same control, off")
                        RowCard()

                        Caption("5 · The goal-detail add row — the same control, on")
                        RowCard()
                    }
                }
            }
        }

        composeRule.onAllNodesWithText("Add a task")[0].performTextReplacementAndSettle("Stretch 10 minutes")
        composeRule.onAllNodesWithText("Add a task")[1].performTextReplacementAndSettle("Stretched already")
        composeRule.onAllNodesWithTag(ALREADY_DONE_TAG)[1].performClick()

        composeRule.onAllNodesWithTag(ALREADY_DONE_TAG)[0].assertIsNotSelected()
        composeRule.onAllNodesWithTag(ALREADY_DONE_TAG)[1].assertIsSelected()

        capture("issue-7-goal-detail-row.png")
    }
}
