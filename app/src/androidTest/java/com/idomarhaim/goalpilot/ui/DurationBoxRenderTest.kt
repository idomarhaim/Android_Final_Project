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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.feature.goals.AddTaskRow
import com.idomarhaim.goalpilot.feature.goals.DURATION_BOX_TAG
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9),
 * as a **file a human can open** rather than an assertion.
 *
 * `R8`'s acceptance criterion is visual and is about an icon: *"there should be an
 * icon inside the box for as long as the person has not entered a number."* Whether
 * that icon is **inside** the box, whether it reads as an affordance or as clutter,
 * and whether its disappearance is noticeable are all questions no assertion answers.
 * [DurationBoxUiTest] proves the icon's *presence rule*; this writes the three states
 * side by side to a PNG so the rule can be judged rather than only checked.
 *
 * The three states are the whole feature: **empty** (nothing known, icon showing),
 * **estimated** (the model answered, icon still showing because the person has not
 * entered anything), and **typed** (icon gone, and now sticky forever).
 *
 * **Getting the PNG off the device is not `adb pull` on its own, and this sentence
 * used to say it was.** `./gradlew :app:connectedDebugAndroidTest` **uninstalls the
 * app when it finishes**, and the app's external files dir goes with it — so the
 * capture is deleted before anyone can fetch it, and `adb pull` then reports a path
 * that does not exist. Run the instrumentation directly instead, which uninstalls
 * nothing:
 *
 * ```bash
 * adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
 * adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb -s <serial> shell am instrument -w -e class <thisClass> \
 *     com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb -s <serial> pull /storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/<name>.png
 * ```
 *
 * Note `.debug` — the debug build carries an `applicationIdSuffix`, so the directory
 * is **not** the one `applicationId` would suggest, and `pm list instrumentation`
 * gives the runner rather than guessing at `AndroidJUnitRunner`. Full reasoning:
 * `kb/dev/android-device-verification.md` §8, which had all of this before #9 and was
 * not consulted.
 *
 * The assertions are a floor, not the point — a green test that produced a blank bitmap
 * would satisfy them and fail the criterion, which is why the file is pulled and
 * **looked at**. Same shape, and the same reasoning, as [WaterGoalRenderTest].
 */
class DurationBoxRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Three independent add-task rows, one per state.
     *
     * Independent rather than one row driven through the states, because the point of
     * the picture is the **comparison** — an icon that vanishes is only judgeable next
     * to the state it vanished from.
     */
    @Composable
    private fun Subject() {
        GoalPilotTheme {
            Surface {
                Column(Modifier.padding(16.dp)) {
                    StateCard("1 · Nothing known yet — the box asks", suggest = null)
                    StateCard("2 · The AI answered — icon stays, it is not your number", suggest = 90)
                    StateCard("3 · You typed one — icon gone, and now sticky", suggest = null)
                }
            }
        }
    }

    @Composable
    private fun StateCard(caption: String, suggest: Int?) {
        GpCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(caption, style = MaterialTheme.typography.labelMedium)
                var points by remember { mutableStateOf<Int?>(null) }
                var minutes by remember { mutableStateOf<Int?>(null) }
                AddTaskRow(
                    isScoring = false,
                    suggestedPoints = points,
                    suggestedMinutes = minutes,
                    // The estimate arrives when the AI button is pressed, exactly as
                    // on the real screen. Seeding it at composition instead would
                    // have produced a row estimated for an EMPTY title, and — found
                    // by running this — typing the title afterwards correctly clears
                    // the estimate, so the picture would have been of nothing.
                    onSuggestPoints = { points = 20; minutes = suggest },
                    onSuggestionApplied = { points = null; minutes = null },
                    onAdd = { _, _, _, _, _ -> },
                )
            }
        }
    }

    @Test
    fun theThreeStatesOfTheBox_writtenToAPngIcanLookAt() {
        composeRule.setContent { Subject() }

        // The third row is seeded by typing into it rather than by constructing the
        // state directly: the picture then shows what a person's own input actually
        // looks like, icon included — or not included, which is the deliverable.
        // Titles first, then the estimate — the real order, and the order that
        // matters: an estimate asked for BEFORE the title is written is cleared by
        // writing it, which is #9's own retitle rule doing its job.
        composeRule.onAllNodesWithText("Add a task")[1].performTextReplacement("Run 5km before work")
        composeRule.onAllNodesWithText("Add a task")[2].performTextReplacement("Write the report")
        composeRule.onAllNodesWithContentDescription("Estimate points with AI")[1].performClick()
        composeRule.onAllNodesWithTag(DURATION_BOX_TAG)[2].performTextInput("45")

        // A floor under the capture: the second row must really be carrying the
        // model's 90, or the picture is of three empty boxes and proves nothing.
        composeRule.onNodeWithText("AI estimate: about 1h 30m of your time").assertExists()
        composeRule.onNodeWithText("You said about 45m").assertExists()
        composeRule.onNodeWithText("Not set — counts as 30m").assertExists()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)
        val out = File(dir, "issue-9-duration-box.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(200)
    }
}
