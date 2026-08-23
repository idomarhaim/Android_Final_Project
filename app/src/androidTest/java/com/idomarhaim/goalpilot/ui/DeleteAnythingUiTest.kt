package com.idomarhaim.goalpilot.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.DeletionImpact
import com.idomarhaim.goalpilot.feature.calendar.CalendarEntry
import com.idomarhaim.goalpilot.feature.calendar.EntryActionSheet
import com.idomarhaim.goalpilot.feature.calendar.EntryKind
import com.idomarhaim.goalpilot.feature.calendar.TAG_ACTION_DELETE
import com.idomarhaim.goalpilot.feature.calendar.TAG_ACTION_SKIP
import com.idomarhaim.goalpilot.ui.components.DeleteConfirm
import com.idomarhaim.goalpilot.ui.components.TAG_DELETE_CANCEL_BUTTON
import com.idomarhaim.goalpilot.ui.components.TAG_DELETE_CONFIRM_BUTTON
import com.idomarhaim.goalpilot.ui.components.TAG_DELETE_GOES
import com.idomarhaim.goalpilot.ui.components.TAG_DELETE_STAYS
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * `#67` on a device — **the confirm, and the one control that decides whether a delete is
 * reachable at all**.
 *
 * ## What the JVM suite already settles, and what it cannot
 *
 * `DeletionReachTest` pins every count and the reach predicate, so nothing here re-asserts
 * arithmetic. What it cannot see is whether the dialog **renders those counts** — a
 * `DeletionImpact` with the right numbers and a `when` branch that drops one of them is green in
 * the JVM layer and silent on screen. So each test below drives the real `DeleteConfirm` with a
 * hand-built impact and reads what appears.
 *
 * ## The negative assertions are the point, and they are paired
 *
 * Two of the sentences this component must **not** say are the ones a person would notice least:
 * a *WHAT STAYS* heading over a task (nothing survives a task), and a points line for an **open**
 * task (`Task.points` prices an open task from its minutes, and that number was never awarded).
 * Each is asserted alongside a positive that proves the matcher still works — a bare
 * count-is-zero reports green on a broken matcher, which is `OverviewCardRenderTest`'s recorded
 * reasoning one ticket ago.
 *
 * ## No account, no Firestore, no Hilt
 *
 * A bare `createComposeRule()` driving stateless composables, so this suite says nothing about
 * the network and needs no sign-in. Run it with `adb install -r` + `am instrument`, never
 * `connectedDebugAndroidTest`, which uninstalls the app and takes the Google account with it.
 *
 * §0.8 is suspended, so this is English only.
 */
class DeleteAnythingUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val goalImpact = DeletionImpact.OfGoal(
        title = "Run 100 km",
        unfiledTaskCount = 3,
        entryCount = 4,
    )

    private val doneTaskImpact = DeletionImpact.OfTask(
        title = "Morning run",
        occurrenceCount = 12,
        settledOccurrenceCount = 5,
        bankedPoints = 40,
    )

    private val openTaskImpact = DeletionImpact.OfTask(
        title = "Book the dentist",
        occurrenceCount = 0,
        settledOccurrenceCount = 0,
        bankedPoints = 0,
    )

    private val areaImpact = DeletionImpact.OfLifeArea(name = "Health", unfiledGoalCount = 2)

    // ------------------------------------------------------------- the assertions

    /** A goal's deletion has both halves, and the counts are the ones it was handed. */
    @Test
    fun aGoalSaysWhatGoesAndWhatStays() {
        composeRule.setContent { Page(dark = false) { Confirm(goalImpact) } }

        says("Run 100 km").assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DELETE_GOES).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DELETE_STAYS).assertIsDisplayed()
        says("4 entries in its progress log")
            .assertIsDisplayed()
        says("3 tasks, which stay and become unfiled")
            .assertIsDisplayed()
    }

    /**
     * **Nothing survives a task, so the second block is absent rather than empty.**
     *
     * Paired with a positive on the same dialog: without it, a `DeleteConfirm` that failed to
     * compose at all would satisfy the negative and report green.
     */
    @Test
    fun aTaskHasNoWhatStaysBlock() {
        composeRule.setContent { Page(dark = false) { Confirm(doneTaskImpact) } }

        composeRule.onNodeWithTag(TAG_DELETE_GOES).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DELETE_STAYS).assertDoesNotExist()
    }

    /** §2.3's history, named before it goes — and named apart from the total. */
    @Test
    fun aTaskSaysHowManyOfItsWindowsAlreadyHappened() {
        composeRule.setContent { Page(dark = false) { Confirm(doneTaskImpact) } }

        says("12 scheduled occurrences").assertIsDisplayed()
        says("Including 5 that already happened")
            .assertIsDisplayed()
        says("The 40 points it earned").assertIsDisplayed()
    }

    /**
     * **An open task has banked nothing, so no points line.**
     *
     * The defect this rules out is not hypothetical: `Task.points` returns a non-zero number for
     * an open task by design, so the natural implementation prints it and tells the person they
     * are about to lose points that were never awarded.
     */
    @Test
    fun anOpenTaskNamesNoPointsAtAll() {
        composeRule.setContent { Page(dark = false) { Confirm(openTaskImpact) } }

        // The positive that proves the dialog composed and the matcher works.
        says("This task.").assertIsDisplayed()
        sayCount("it earned").assertCountEquals(0)
        sayCount("occurrence").assertCountEquals(0)
    }

    /** An area keeps its goals, and says so **before** the act rather than in an empty state. */
    @Test
    fun anAreaSaysItsGoalsAreKept() {
        composeRule.setContent { Page(dark = false) { Confirm(areaImpact) } }

        says("Health").assertIsDisplayed()
        says("2 goals, which stay and become unfiled")
            .assertIsDisplayed()
    }

    /** Cancel writes nothing, and the dialog is dismissed rather than confirmed. */
    @Test
    fun cancelConfirmsNothing() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            Page(dark = false) {
                DeleteConfirm(
                    impact = goalImpact,
                    onConfirm = { confirmed++ },
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeRule.onNodeWithTag(TAG_DELETE_CANCEL_BUTTON).performClick()

        assertThat(confirmed).isEqualTo(0)
        assertThat(dismissed).isEqualTo(1)
    }

    /** And the delete button reports exactly once. */
    @Test
    fun deleteConfirmsOnce() {
        var confirmed = 0
        composeRule.setContent {
            Page(dark = false) {
                DeleteConfirm(impact = goalImpact, onConfirm = { confirmed++ }, onDismiss = {})
            }
        }

        composeRule.onNodeWithTag(TAG_DELETE_CONFIRM_BUTTON).performClick()

        assertThat(confirmed).isEqualTo(1)
    }

    /**
     * **The calendar's sheet offers `Delete` for a task and for nothing else.**
     *
     * A challenge window and an `EXTERNAL` Google event are drawn on this surface and owned
     * elsewhere; a delete on one would either do nothing or remove something the person is not
     * looking at. `CalendarEntry.taskId` is `null` for all three non-task kinds, which is the
     * whole of the condition — asserted here because it is one `!= null` in a composable and
     * nothing else in the suite reads it.
     */
    @Test
    fun theCalendarSheetOffersDeleteOnlyForATask() {
        composeRule.setContent {
            Page(dark = false) {
                EntryActionSheet(entry = taskEntry(), onSkip = {}, onDelete = {}, onDismiss = {})
            }
        }
        composeRule.onNodeWithTag(TAG_ACTION_SKIP).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ACTION_DELETE).assertIsDisplayed()
    }

    @Test
    fun theCalendarSheetHidesDeleteForSomethingThatIsNotATask() {
        composeRule.setContent {
            Page(dark = false) {
                EntryActionSheet(entry = externalEntry(), onSkip = {}, onDelete = {}, onDismiss = {})
            }
        }
        // Paired, as above: `Skip` present is what proves the sheet composed at all.
        composeRule.onNodeWithTag(TAG_ACTION_SKIP).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ACTION_DELETE).assertDoesNotExist()
    }

    // ---------------------------------------------------------------- the captures

    @Test
    fun theGoalConfirm_light() {
        composeRule.setContent { Page(dark = false) { Confirm(goalImpact) } }
        capture("issue-67-confirm-goal-light.png", probe = "3 tasks, which stay")
    }

    /**
     * Not a duplicate of the light frame. The two block headings are `labelSmall` at
     * `onSurfaceVariant` over the dialog's own container, so whether *WHAT GOES* and *WHAT STAYS*
     * read as structure or as noise is a **contrast** question, and this dialog's whole job is to
     * be scanned once under a decision.
     */
    @Test
    fun theGoalConfirm_dark() {
        composeRule.setContent { Page(dark = true) { Confirm(goalImpact) } }
        capture("issue-67-confirm-goal-dark.png", probe = "3 tasks, which stay")
    }

    /** The one that reaches history, and the only one with no second block. */
    @Test
    fun theTaskConfirm_light() {
        composeRule.setContent { Page(dark = false) { Confirm(doneTaskImpact) } }
        capture("issue-67-confirm-task-light.png", probe = "Including 5 that already happened")
    }

    @Test
    fun theTaskConfirm_dark() {
        composeRule.setContent { Page(dark = true) { Confirm(doneTaskImpact) } }
        capture("issue-67-confirm-task-dark.png", probe = "Including 5 that already happened")
    }

    /** The mildest of the three — one line each side. */
    @Test
    fun theAreaConfirm_light() {
        composeRule.setContent { Page(dark = false) { Confirm(areaImpact) } }
        capture("issue-67-confirm-area-light.png", probe = "2 goals, which stay")
    }

    /**
     * **The calendar sheet, because `Delete` sits under `Skip` and must not read as one.**
     *
     * The divider, the error tint and the sentence under it are the whole of what stops a hurried
     * thumb finding a delete where `Skip` was a moment ago, and none of that is checkable by a
     * matcher.
     */
    @Test
    fun theCalendarSheet_light() {
        composeRule.setContent {
            Page(dark = false) {
                EntryActionSheet(entry = taskEntry(), onSkip = {}, onDelete = {}, onDismiss = {})
            }
        }
        capture("issue-67-calendar-sheet-light.png", probe = "Removes the task itself")
    }

    // ----------------------------------------------------------------- scaffolding

    /**
     * **`onNodeWithText(…, substring = true)` cannot find a count in this dialog, and the reason
     * is invisible.**
     *
     * `Observed:` 2026-08-23 on `emulator-5554`, by dumping the semantics tree rather than by
     * reading the code — 8 of this file's 15 tests failed on the first run and every one of them
     * was matching a string with a number in it. Every count here goes through
     * `bidiIsolated()` (§4.8), so what the node actually holds is
     *
     * ```
     * ⁨4⁩ entries in its progress log.       codes: 8296, 52, 8297, 32, 101, …
     * ```
     *
     * — `U+2069 POP DIRECTIONAL ISOLATE` sits **between the digit and the space**, so the
     * substring `"4 entries"` is not present. The text renders perfectly, the isolate marks are
     * zero-width, and the failure message is `The component is not displayed!`, which points at
     * layout. Nothing about it suggests the string.
     *
     * The wrong fix is to match `"entries in its progress log"` and stop asserting the number:
     * the number is the whole point of the dialog, and dropping it would leave the one thing
     * `#67` added untested while every test went green. So the **matcher** is fixed instead —
     * `Bidi.strip` is already in the codebase for exactly this, and its KDoc says so: *"for tests
     * and for logging, never for display."*
     *
     * `useUnmergedTree` because that is the tree the dump was read from, and matching what was
     * actually inspected is worth more than the default.
     */
    private fun hasStrippedText(substring: String) =
        SemanticsMatcher("text (isolates stripped) contains '$substring'") { node ->
            node.config.getOrNull(SemanticsProperties.Text)
                .orEmpty()
                .any { Bidi.strip(it.text).contains(substring) }
        }

    private fun says(substring: String) =
        composeRule.onNode(hasStrippedText(substring), useUnmergedTree = true)

    private fun sayCount(substring: String) =
        composeRule.onAllNodes(hasStrippedText(substring), useUnmergedTree = true)

    private fun taskEntry() = CalendarEntry(
        key = "occ:1",
        title = "Morning run",
        kind = EntryKind.TASK,
        occurrence = Block(DAY.atTime(7, 0), DAY.atTime(7, 45)),
        taskId = "t1",
        occurrenceId = "o1",
        isRepeating = true,
    )

    private fun externalEntry() = CalendarEntry(
        key = "gcal:1",
        title = "Dentist",
        kind = EntryKind.EXTERNAL,
        occurrence = AllDay(DAY),
    )

    @Composable
    private fun Confirm(impact: DeletionImpact) {
        DeleteConfirm(impact = impact, onConfirm = {}, onDismiss = {})
    }

    @Composable
    private fun Page(dark: Boolean, content: @Composable () -> Unit) {
        GoalPilotTheme(darkTheme = dark) {
            Surface {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Something behind the dialog, so a capture that somehow grabbed the wrong
                    // window is obvious rather than plausibly blank.
                    Text("#67 — delete confirm", style = MaterialTheme.typography.labelSmall)
                    content()
                }
            }
        }
    }

    /**
     * Captures the **dialog's own window**, not the activity's.
     *
     * `onRoot()` would grab the page behind it — a `Dialog` and a `ModalBottomSheet` each compose
     * into a separate window, so the bitmap would come back the right size, the right shape and
     * completely without the thing under test. That is `kb/dev/look-at-your-own-output.md` §4g's
     * failure exactly, and the reason the probe below reads something from the **content** rather
     * than merely checking the file is non-empty.
     */
    private fun capture(name: String, probe: String) {
        says(probe).assertIsDisplayed()

        val bitmap = composeRule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val out = File(context.getExternalFilesDir(null), name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(200)
    }

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 8, 24)
    }
}
