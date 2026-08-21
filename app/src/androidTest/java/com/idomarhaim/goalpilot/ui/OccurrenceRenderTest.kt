package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceDraft
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.DailyMissReview
import com.idomarhaim.goalpilot.feature.dashboard.DailyMissReviewCard
import com.idomarhaim.goalpilot.feature.goals.WhenPicker
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * §0.8: *"Every screen is designed, and is not finished until seen"* — `#56`'s two new surfaces,
 * written to PNGs a human can actually look at.
 *
 * These are **not** a substitute for the assertions in `WhenPickerUiTest` and
 * `DailyMissReviewUiTest`. They exist because the thing that matters about both surfaces is not
 * assertable: whether the *when* chip reads as one control in three states rather than as three
 * unrelated buttons, and whether the review reads as a statement of fact rather than as a
 * scolding. A test can only say the nodes are there.
 *
 * Each case still asserts a floor before it captures, so the picture cannot be of an empty
 * screen — the failure mode `DurationBoxRenderTest` records: a render pass that proves nothing
 * because it photographed three empty boxes.
 *
 * Pull them with:
 * `adb shell run-as com.idomarhaim.goalpilot.debug cat files/../../../../sdcard/Android/data/…`
 * or, simply, `adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/`.
 */
class OccurrenceRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day: LocalDate = LocalDate.of(2026, 8, 22)
    private val monday: LocalDate = LocalDate.of(2026, 8, 17)

    private fun write(name: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        val out = File(dir, name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
    }

    /**
     * The *when* control in all three of its states at once — **unset, all-day, deadline**.
     *
     * Side by side on purpose. Each state is unremarkable alone; what has to be legible is that
     * they are the same control, and that the difference between the second and the third
     * (which is §2.2's difference between *the day passed* and *late, still owed*) is visible
     * without reading a tooltip.
     */
    @Test
    fun theThreeStatesOfTheWhenControl_writtenToAPngIcanLookAt() {
        composeRule.setContent {
            GoalPilotTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Labelled("No when at all — the default", OccurrenceDraft())
                    Labelled("A day: ALL_DAY, a miss means the day passed", OccurrenceDraft().withDate(day))
                    Labelled(
                        "A day and a time: DEADLINE, a miss means late and still owed",
                        OccurrenceDraft().withDate(day).withTime(LocalTime.of(6, 0)),
                    )
                }
            }
        }

        // The floor: the unset and set states must really be rendering differently, or the
        // picture is of three identical chips and shows nothing.
        composeRule.onNodeWithText("When?").assertIsDisplayed()

        write("issue-56-when-control.png")
    }

    @Composable
    private fun Labelled(caption: String, initial: OccurrenceDraft) {
        var draft by mutableStateOf(initial)
        Text(caption, style = MaterialTheme.typography.labelSmall)
        WhenPicker(draft = draft, onChange = { draft = it })
    }

    /**
     * §2.5's daily review, with all four of §2.2's miss meanings on it at once.
     *
     * The one thing worth looking at: *"never as a push saying he failed"* is about **tone**, and
     * tone is exactly what no assertion in `DailyMissReviewUiTest` can check. Four rows of
     * neutral sentences, one acknowledgement, and no red.
     */
    @Test
    fun theDailyMissReview_writtenToAPngIcanLookAt() {
        val tasks = listOf<Pair<String, Occurrence>>(
            "Water the plants" to AllDay(monday),
            "File the tax return" to Deadline(monday.atTime(18, 0)),
            "Gym session" to Block(monday.atTime(9, 0), monday.atTime(10, 0)),
            "Clear the garage" to Span(monday.minusDays(6), monday),
        ).map { (title, occurrence) -> Task(id = title, title = title, occurrence = occurrence) }

        val misses = DailyMissReview.of(tasks, monday.plusDays(1).atTime(9, 0))
        assertThat(misses).hasSize(4)

        composeRule.setContent {
            GoalPilotTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                ) {
                    DailyMissReviewCard(misses = misses, onDismiss = {})
                }
            }
        }

        // The floor: all four meanings really are on screen, so the picture is of the whole
        // table rather than of whichever row happened to render.
        composeRule.onNodeWithText("the day passed").assertIsDisplayed()
        composeRule.onNodeWithText("late, still owed").assertIsDisplayed()
        composeRule.onNodeWithText("the slot is gone").assertIsDisplayed()
        composeRule.onNodeWithText("the window closed").assertIsDisplayed()

        write("issue-56-daily-miss-review.png")
    }
}
