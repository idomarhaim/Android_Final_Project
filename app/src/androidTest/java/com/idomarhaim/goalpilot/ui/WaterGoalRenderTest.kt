package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FillLadder
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.feature.goals.FillButtonRow
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11),
 * as a **file a human can open** rather than an assertion.
 *
 * #11's acceptance criterion is visual and is stated about one real goal: *"Drink
 * 4 Liters of Water Daily"* is live on Ido's account and reads `Health · 1/100 %`
 * — tracked in percent, not litres. The brief asks for it *reproduced in a
 * fixture, reading in litres*, and that is what this writes to a PNG.
 *
 * **Why the fixture already carries the measure, when the migration would not
 * give it one.** §7.1 turns a defaulted `"%"` into an **absent** measure, so the
 * live document arrives unmeasured on purpose and stays that way until somebody
 * answers `C22` #44's offer. This renders the state *after* that answer — which
 * is the state #11 is responsible for and the only one in which fill buttons
 * exist at all. Rendering the unanswered goal would be a picture of #44.
 *
 * The PNG lands in the app's external files dir; `adb pull` fetches it. The
 * assertions are a floor, not the point: a green test that produced a blank
 * bitmap would satisfy them and fail the criterion, which is why the file is
 * pulled and **looked at**.
 */
class WaterGoalRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** The live goal as it reads today: no measure, so the app can only say `%`. */
    private val before = Goal(
        id = "water-before",
        title = "Drink 4 Liters of Water Daily",
        targetValue = 100.0,
        currentValue = 1.0,
    )

    /** The same goal once it counts litres and logs by button. */
    private val after = Goal(
        id = "water-after",
        title = "Drink 4 Liters of Water Daily",
        targetValue = 4.0,
        measure = Measure(MeasureKind.VOLUME, "L"),
        inputMode = InputMode.BUTTONS,
    )

    @Composable
    private fun Subject() {
        GoalPilotTheme {
            Surface(modifier = Modifier.width(360.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Today — the defect", style = MaterialTheme.typography.labelMedium)
                            Text(before.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${before.category.label} · " +
                                    "${before.currentValue.toInt()}/${before.targetValue.toInt()} %",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    GpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            var current by remember { mutableStateOf(0.0) }
                            Text("With #11 — litres", style = MaterialTheme.typography.labelMedium)
                            Text(after.title, style = MaterialTheme.typography.titleMedium)
                            FillButtonRow(
                                amounts = FillLadder.forGoal(after),
                                word = after.measureWord,
                                current = current,
                                target = after.targetValue,
                                onLog = { current += it },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun waterGoal_readsInLitresAndIsWrittenToAPngIcanLookAt() {
        composeRule.setContent { Subject() }

        // Two taps, so the capture shows a tally that has actually moved rather
        // than a zero that would also be produced by a row wired to nothing.
        composeRule.onNodeWithContentDescription("Log 0.75 L").performClick()
        composeRule.onNodeWithContentDescription("Log 0.5 L").performClick()
        composeRule.onNodeWithContentDescription("1.25 / 4 L").assertExists()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)
        val out = File(dir, "issue-11-water-goal.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // A floor under the capture itself: an empty or one-pixel bitmap would
        // still write a file, and the file is the deliverable.
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(200)
    }
}
