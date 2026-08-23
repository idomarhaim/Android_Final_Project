package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.EditScope
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.RepeatEnd
import com.idomarhaim.goalpilot.domain.model.RepeatRule
import com.idomarhaim.goalpilot.domain.model.RepeatUnit
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.feature.calendar.CalendarBuilder
import com.idomarhaim.goalpilot.feature.calendar.CalendarEntry
import com.idomarhaim.goalpilot.feature.calendar.CalendarNotice
import com.idomarhaim.goalpilot.feature.calendar.CalendarSurface
import com.idomarhaim.goalpilot.feature.calendar.CalendarUiState
import com.idomarhaim.goalpilot.feature.calendar.CalendarZoom
import com.idomarhaim.goalpilot.feature.calendar.DragToMove
import com.idomarhaim.goalpilot.feature.calendar.TAG_ACTION_CANCEL
import com.idomarhaim.goalpilot.feature.calendar.TAG_ACTION_SHEET
import com.idomarhaim.goalpilot.feature.calendar.TAG_ACTION_SKIP
import com.idomarhaim.goalpilot.feature.calendar.TAG_SCOPE_FUTURE
import com.idomarhaim.goalpilot.feature.calendar.TAG_SCOPE_SHEET
import com.idomarhaim.goalpilot.feature.calendar.TAG_SCOPE_THIS
import com.idomarhaim.goalpilot.feature.calendar.entryTag
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * §4.3's *drag to move*, on a device
 * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
 *
 * ### What only a device can answer, and what is deliberately left to the JVM
 *
 * Everything about **where a drag lands** — the column arithmetic, the snap, the clamps, which
 * `EditScope` a one-off gets — is a pure function of a geometry and is asserted in
 * `app/src/test/.../feature/calendar/DragToMoveTest.kt` with no emulator. Repeating any of it here
 * would buy a slower copy of a check that already exists, and `DragToMove`'s own KDoc has the
 * argument for why that split is the point rather than a convenience.
 *
 * What only a device can answer is whether **the gesture arrives at all**: that a long press on a
 * chip inside a `verticalScroll` is not eaten by the scroll, that the same press means two
 * different things depending on whether the finger travelled, and that §2.1's question really does
 * appear over a repeating row and really does not over a one-off. Every one of those is a fact
 * about the Compose gesture graph, and none of them is expressible in a unit test.
 *
 * ### It drives [CalendarSurface], not `CalendarScreen`
 *
 * `CalendarSurfaceUiTest`'s reason, unchanged: the Hilt-wired screen needs a signed-in account and
 * a live Firestore, so a test of it would be a test of the network with a calendar attached. The
 * stateless surface takes its state as a parameter and reports through callbacks, which is exactly
 * what makes *"was the right scope passed?"* an assertion rather than a screenshot.
 *
 * ⚠️ **Run it with `adb install -r` + `am instrument`, never `connectedDebugAndroidTest`** — that
 * task uninstalls the app and takes the Google sign-in with it
 * (`kb/dev/android-device-verification.md` §8).
 */
class DragToMoveUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val noon: LocalDateTime = monday.atTime(12, 0)

    private val health = LifeAreaFixture.health
    private val runGoal = Goal(id = "g1", title = "Run 100 km", lifeAreaIds = listOf(health.id))

    /** What the surface reported. Each list is one callback, so a silent gesture is a visible fail. */
    private val moved = mutableListOf<Triple<String, DragToMove.Target, EditScope>>()
    private val skipped = mutableListOf<Pair<String, EditScope>>()
    private val opened = mutableListOf<String>()
    private var noticesShown = 0

    /**
     * Two blocks on Monday and one banner: **a repeating one, a one-off, and a row with no
     * schedule at all**.
     *
     * The three exist so the *branches* can be told apart on one screen — which is the same
     * instinct `CalendarSurfaceUiTest`'s fixture has about lanes, applied to editability rather
     * than to presentation.
     */
    private fun fixture(zoom: CalendarZoom = CalendarZoom.THREE_DAYS): CalendarUiState {
        val schedules = listOf(
            // Repeating: daily, so §2.1's question has two real answers.
            TaskSchedule(
                Task(
                    id = "t1",
                    title = "Morning run",
                    occurrence = Block(monday.atTime(7, 0), monday.atTime(8, 0)),
                    repeatRule = RepeatRule(unit = RepeatUnit.DAY, interval = 1),
                ),
            ),
            // A one-off: no rule, so the scope sheet must NOT appear.
            TaskSchedule(Task(id = "t2", title = "Dentist", occurrence = Block(monday.atTime(10, 0), monday.atTime(11, 0)))),
            // A banner, and it is not in the grid -- menu yes, drag no.
            TaskSchedule(Task(id = "t3", title = "Tax return", occurrence = Deadline(monday.atTime(17, 0)))),
        )
        return CalendarUiState(
            zoom = zoom,
            anchor = monday,
            today = monday,
            days = CalendarBuilder.build(
                range = CalendarBuilder.daysFor(monday, zoom),
                today = monday,
                now = noon,
                schedules = schedules,
                goals = listOf(runGoal),
                lifeAreas = listOf(health),
                challenges = emptyList(),
                waking = WakingHours.DEFAULT,
                zone = zone,
            ),
            goals = listOf(runGoal),
            isLoading = false,
        )
    }

    private fun setSurface(state: CalendarUiState = fixture()) {
        composeRule.setContent {
            GoalPilotTheme {
                CalendarSurface(
                    state = state,
                    onOpenTask = { opened += it },
                    onMove = { entry, target, scope -> moved += Triple(entry.title, target, scope) },
                    onSkip = { entry, scope -> skipped += entry.title to scope },
                    onNoticeShown = { noticesShown++ },
                )
            }
        }
    }

    /**
     * One repeating task whose instance is **stored**, so its entry key is the document id and
     * survives a move. Everything else about the surface is empty, so the single grid row is
     * unambiguous.
     */
    private fun oneStoredInstance(stored: ScheduledOccurrence): CalendarUiState {
        val schedule = TaskSchedule(
            task = Task(
                id = "t1",
                title = "Morning run",
                occurrence = Block(monday.atTime(7, 0), monday.atTime(8, 0)),
                // Bounded to ONE instance, so `days.flatMap { it.all }.single()` below is
                // unambiguous -- an unbounded daily rule would draw Tuesday and Wednesday too.
                // Still a rule, so §2.1's question is still asked, which is the point.
                repeatRule = RepeatRule(unit = RepeatUnit.DAY, interval = 1, end = RepeatEnd.AfterCount(1)),
            ),
            stored = listOf(stored),
        )
        // THREE_DAYS, not AGENDA: the agenda level draws a list and offers no drag at all
        // (`AgendaColumn`), so a gesture test there would assert nothing.
        return CalendarUiState(
            zoom = CalendarZoom.THREE_DAYS,
            anchor = monday,
            today = monday,
            days = CalendarBuilder.build(
                range = CalendarBuilder.daysFor(monday, CalendarZoom.THREE_DAYS),
                today = monday,
                now = noon,
                schedules = listOf(schedule),
                goals = emptyList(),
                lifeAreas = emptyList(),
                challenges = emptyList(),
                waking = WakingHours.DEFAULT,
                zone = zone,
            ),
            isLoading = false,
        )
    }

    /** Two hours of drawn grid, measured off the screen rather than recomputed from `dp`. */
    private fun hourPitchPx(): Float {
        val seven = composeRule.onNodeWithTag(com.idomarhaim.goalpilot.feature.calendar.slotTag(monday, 7))
            .fetchSemanticsNode().positionInRoot.y
        val nine = composeRule.onNodeWithTag(com.idomarhaim.goalpilot.feature.calendar.slotTag(monday, 9))
            .fetchSemanticsNode().positionInRoot.y
        return nine - seven
    }

    /** The entry on the surface with this title, whatever lane it landed in. */
    private fun entry(title: String): CalendarEntry =
        fixture().days.flatMap { it.all }.first { it.title == title }

    /**
     * A **one-off whose occurrence document already exists** -- `#61`'s shape, and the row that
     * [`#69`](https://github.com/idomarhaim/Android_Final_Project/issues/69) made reachable.
     *
     * `SyncCalendarUseCase.link` mints the document when it pushes a one-off to Google:
     * `seriesDate = null` because it belongs to no series, a `googleEventId`, and the outcome still
     * `Planned`. Until `#69` widened `ScheduleEdits.apply`'s parameter, `CalendarEntry.isEditable`
     * refused this row outright rather than let both scopes fail silently on it -- so the gesture
     * below did not arrive at all. Deliberately its **own** state rather than a fourth row in
     * [fixture]: what is being asserted is one property of one row, and putting it in the shared
     * fixture would change what every other test on this surface is exercising.
     */
    private fun linkedOneOff(): CalendarUiState {
        val at = Block(monday.atTime(10, 0), monday.atTime(11, 0))
        val schedule = TaskSchedule(
            task = Task(id = "t2", title = "Dentist", occurrence = at),
            stored = listOf(
                ScheduledOccurrence(
                    id = "occ-1",
                    taskId = "t2",
                    occurrence = at,
                    seriesDate = null,
                    googleEventId = "gcal-1",
                ),
            ),
        )
        return CalendarUiState(
            zoom = CalendarZoom.THREE_DAYS,
            anchor = monday,
            today = monday,
            days = CalendarBuilder.build(
                range = CalendarBuilder.daysFor(monday, CalendarZoom.THREE_DAYS),
                today = monday,
                now = noon,
                schedules = listOf(schedule),
                goals = emptyList(),
                lifeAreas = emptyList(),
                challenges = emptyList(),
                waking = WakingHours.DEFAULT,
                zone = zone,
            ),
            isLoading = false,
        )
    }

    @Test
    fun aGoogleLinkedOneOffCanBeDraggedNowThatItsDocumentIsAddressable() {
        // #69, end to end on the surface. The row carries an `occurrenceId` and a NULL
        // `seriesDate` -- exactly the pair `isEditable`'s removed third condition refused -- so
        // before the fix this gesture reported nothing at all, silently. What the resulting plan
        // then writes is asserted on the JVM in `DragToMoveTest`; what this test buys is that the
        // gesture is offered and arrives.
        val state = linkedOneOff()
        val row = state.days.flatMap { it.all }.first { it.title == "Dentist" }
        assertWithMessage("the fixture must really be the guarded shape: a document id AND no series date")
            .that(row.occurrenceId to row.seriesDate).isEqualTo("occ-1" to null)

        setSurface(state)
        val column = columnPitchPx()

        composeRule.onNodeWithTag(entryTag(row)).performScrollTo().performTouchInput {
            down(center)
            moveBy(Offset(4f, 0f), delayMillis = 1_000)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            up()
        }
        composeRule.waitForIdle()

        assertWithMessage("the guard is gone, so a carried long press on a linked one-off must report a move")
            .that(moved).hasSize(1)
        val (title, landing, scope) = moved.single()
        assertThat(title).isEqualTo("Dentist")
        assertThat(landing.date).isEqualTo(monday.plusDays(1))
        // Still a one-off, so §2.1's question is not asked and `whenNotAsked` decides.
        assertThat(scope).isEqualTo(EditScope.THIS_AND_FUTURE)
        assertThat(composeRule.onAllNodesWithTag(TAG_SCOPE_SHEET).fetchSemanticsNodes()).isEmpty()
    }

    // ── The gesture arrives ─────────────────────────────────────────────────────────────

    @Test
    fun holdingAGridRowWithoutMovingItOpensTheEntryMenu() {
        setSurface()

        composeRule.onNodeWithTag(entryTag(entry("Dentist"))).performScrollTo().performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_ACTION_SHEET).assertIsDisplayed()
        // The half that would otherwise pass silently: a long press that ALSO opened the task
        // would mean the press is being read twice, which is how the reorder handle broke in `#2`.
        assertThat(opened).isEmpty()
        assertThat(moved).isEmpty()
    }

    @Test
    fun holdingAndCarryingAGridRowMovesIt() {
        setSurface()

        val target = entry("Dentist")
        val column = columnPitchPx()

        composeRule.onNodeWithTag(entryTag(target)).performScrollTo().performTouchInput {
            down(center)
            // A drag only starts after the long press, and the first movement has to clear touch
            // slop before the detector reports anything -- `LifeAreaReorderUiTest`'s idiom, which
            // is the one shape known to work on this device.
            moveBy(Offset(4f, 0f), delayMillis = 1_000)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            up()
        }
        composeRule.waitForIdle()

        // A one-off, so §2.1's question is not asked and the move commits straight through.
        assertWithMessage("a carried long press must report a move").that(moved).hasSize(1)
        val (title, landing, scope) = moved.single()
        assertThat(title).isEqualTo("Dentist")
        assertWithMessage("one column of travel is the next day").that(landing.date).isEqualTo(monday.plusDays(1))
        assertThat(scope).isEqualTo(EditScope.THIS_AND_FUTURE)
        assertThat(opened).isEmpty()
    }

    @Test
    fun aTapStillOpensTheTaskRatherThanStartingADrag() {
        setSurface()

        composeRule.onNodeWithTag(entryTag(entry("Dentist"))).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(opened).containsExactly("t2")
        assertThat(moved).isEmpty()
        assertThat(composeRule.onAllNodesWithTag(TAG_ACTION_SHEET).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun aRowBeingCarriedLooksCarried() {
        // The one claim in this feature that no assertion can reach: `DraggableEntry` offsets the
        // chip by the finger's travel and dims it, and the comment there says a drag without that
        // "looks like the calendar redrawing itself under a stationary finger". That is a statement
        // about a picture, so it is checked by taking one -- mid-gesture, with the pointer still
        // down, which is the only moment it is true.
        setSurface()

        val target = entry("Dentist")
        val before = composeRule.onNodeWithTag(entryTag(target)).fetchSemanticsNode().positionInRoot

        composeRule.onNodeWithTag(entryTag(target)).performScrollTo().performTouchInput {
            down(center)
            moveBy(Offset(4f, 0f), delayMillis = 1_000)
            moveBy(Offset(0f, 160f), delayMillis = 32)
            moveBy(Offset(0f, 160f), delayMillis = 32)
            // No `up()`. The chip is still in the air.
        }
        composeRule.waitForIdle()

        val during = composeRule.onNodeWithTag(entryTag(target)).fetchSemanticsNode().positionInRoot
        write("issue-68-mid-drag.png")

        assertWithMessage("the row the finger is holding must be the row that moves")
            .that(during.y).isGreaterThan(before.y)
        assertThat(moved).isEmpty()

        // Let go, so the fixture is not left mid-gesture for whatever runs next in this process.
        composeRule.onNodeWithTag(entryTag(target)).performTouchInput { up() }
        composeRule.waitForIdle()
    }

    // ── §2.1's question: asked where a rule exists, and nowhere else ─────────────────────

    @Test
    fun movingARepeatingRowAsksWhichInstancesAndPassesTheAnswerThrough() {
        setSurface()

        val target = entry("Morning run")
        val column = columnPitchPx()

        composeRule.onNodeWithTag(entryTag(target)).performScrollTo().performTouchInput {
            down(center)
            moveBy(Offset(4f, 0f), delayMillis = 1_000)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            up()
        }
        composeRule.waitForIdle()

        // Nothing has been committed yet -- the sheet is the whole point.
        assertWithMessage("a repeating row must not move until the scope is answered").that(moved).isEmpty()
        composeRule.onNodeWithTag(TAG_SCOPE_SHEET).assertIsDisplayed()
        write("issue-68-scope-sheet.png", composeRule.onNodeWithTag(TAG_SCOPE_SHEET))

        composeRule.onNodeWithTag(TAG_SCOPE_FUTURE).performClick()
        composeRule.waitForIdle()

        assertThat(moved).hasSize(1)
        assertThat(moved.single().third).isEqualTo(EditScope.THIS_AND_FUTURE)
        assertThat(moved.single().second.date).isEqualTo(monday.plusDays(1))
    }

    @Test
    fun theOtherAnswerReachesTheSurfaceToo() {
        setSurface()

        val column = columnPitchPx()
        composeRule.onNodeWithTag(entryTag(entry("Morning run"))).performScrollTo().performTouchInput {
            down(center)
            moveBy(Offset(4f, 0f), delayMillis = 1_000)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            up()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_SCOPE_THIS).performClick()
        composeRule.waitForIdle()

        assertThat(moved.single().third).isEqualTo(EditScope.THIS_OCCURRENCE)
    }

    @Test
    fun aOneOffIsNeverAskedWhichInstances() {
        // The clause §2.1's own argument requires: the question exists only where a rule does, and
        // asking it anyway teaches the person that the sheet means nothing.
        setSurface()

        val column = columnPitchPx()
        composeRule.onNodeWithTag(entryTag(entry("Dentist"))).performScrollTo().performTouchInput {
            down(center)
            moveBy(Offset(4f, 0f), delayMillis = 1_000)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            moveBy(Offset(column * 0.5f, 0f), delayMillis = 32)
            up()
        }
        composeRule.waitForIdle()

        assertThat(composeRule.onAllNodesWithTag(TAG_SCOPE_SHEET).fetchSemanticsNodes()).isEmpty()
        assertThat(moved).hasSize(1)
    }

    @Test
    fun draggingTheSameStoredInstanceTwiceReadsItsNewTimeTheSecondTime() {
        // ⚠️ The regression this exists for is INVISIBLE, and no other test in this file can reach
        // it. `pointerInput` restarts only when its keys change, and this row's key is its
        // **document id**, which does not change when the instance moves. So a `CalendarEntry`
        // captured inside that block survives the first drag and describes the row's OLD time; a
        // second drag computed from it lands the row back near where it started, which reads as the
        // calendar fighting the finger. Reaching it needs three things at once — a stored instance
        // (stable key), a state that actually updates after the move, and two drags — which is why
        // it is one long test rather than an assertion added to an existing one.
        val stored = ScheduledOccurrence(
            id = "occ-1",
            taskId = "t1",
            occurrence = Block(monday.atTime(7, 0), monday.atTime(8, 0)),
            seriesDate = monday,
        )
        var live by mutableStateOf(oneStoredInstance(stored))
        composeRule.setContent {
            GoalPilotTheme {
                CalendarSurface(
                    state = live,
                    onMove = { entry, target, scope ->
                        moved += Triple(entry.title, target, scope)
                        // What the repository would have written, fed straight back in: same
                        // document, same series date, new *when*.
                        live = oneStoredInstance(
                            stored.copy(
                                occurrence = Block(
                                    start = target.date.atTime(target.time),
                                    end = target.date.atTime(target.time).plusHours(1),
                                ),
                            ),
                        )
                    },
                )
            }
        }

        val twoHours = hourPitchPx()
        repeat(2) {
            composeRule.onNodeWithTag(entryTag(live.days.flatMap { d -> d.all }.single()))
                .performScrollTo()
                .performTouchInput {
                    down(center)
                    moveBy(Offset(0f, 4f), delayMillis = 1_000)
                    moveBy(Offset(0f, twoHours * 0.5f), delayMillis = 32)
                    moveBy(Offset(0f, twoHours * 0.5f), delayMillis = 32)
                    up()
                }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(TAG_SCOPE_THIS).performClick()
            composeRule.waitForIdle()
        }

        assertThat(moved).hasSize(2)
        assertWithMessage("the first drag moves it two hours down from 07:00")
            .that(moved[0].second.minuteOfDay).isEqualTo(9 * 60)
        assertWithMessage(
            "the second drag must start from where the row NOW is (09:00), not from where the " +
                "gesture detector last saw it (07:00)",
        ).that(moved[1].second.minuteOfDay).isEqualTo(11 * 60)
    }

    // ── Skip, the second entry point to the same machinery ───────────────────────────────

    @Test
    fun skippingAOneOffCommitsWithNoSecondQuestion() {
        setSurface()

        composeRule.onNodeWithTag(entryTag(entry("Dentist"))).performScrollTo().performTouchInput { longClick() }
        composeRule.waitForIdle()
        write("issue-68-action-sheet.png", composeRule.onNodeWithTag(TAG_ACTION_SHEET))
        composeRule.onNodeWithTag(TAG_ACTION_SKIP).performClick()
        composeRule.waitForIdle()

        assertThat(skipped).containsExactly("Dentist" to EditScope.THIS_AND_FUTURE)
        assertThat(composeRule.onAllNodesWithTag(TAG_SCOPE_SHEET).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun skippingARepeatingRowAsksWhichInstancesFirst() {
        setSurface()

        composeRule.onNodeWithTag(entryTag(entry("Morning run"))).performScrollTo().performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_ACTION_SKIP).performClick()
        composeRule.waitForIdle()

        assertWithMessage("a repeating skip must not commit until the scope is answered")
            .that(skipped).isEmpty()
        composeRule.onNodeWithTag(TAG_SCOPE_SHEET).assertIsDisplayed()

        composeRule.onNodeWithTag(TAG_SCOPE_THIS).performClick()
        composeRule.waitForIdle()

        assertThat(skipped).containsExactly("Morning run" to EditScope.THIS_OCCURRENCE)
    }

    @Test
    fun aBannerHasNoDragButStillReachesSkip() {
        // `CalendarEntry.isDraggable`'s asymmetry, end to end: the deadline is drawn in the all-day
        // strip, which has no geometry to drop onto -- but §2.1's skip must still be reachable, or
        // a whole lane of work can never be dropped.
        setSurface()

        composeRule.onNodeWithTag(entryTag(entry("Tax return"))).performScrollTo().performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_ACTION_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ACTION_SKIP).performClick()
        composeRule.waitForIdle()

        assertThat(skipped).containsExactly("Tax return" to EditScope.THIS_AND_FUTURE)
        assertThat(moved).isEmpty()
    }

    @Test
    fun cancellingTheMenuCommitsNothing() {
        setSurface()

        composeRule.onNodeWithTag(entryTag(entry("Dentist"))).performScrollTo().performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_ACTION_CANCEL).performClick()
        composeRule.waitForIdle()

        assertThat(skipped).isEmpty()
        assertThat(moved).isEmpty()
        assertThat(composeRule.onAllNodesWithTag(TAG_ACTION_SHEET).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun aSettledRowOffersNothingToEdit() {
        // §2.3: a settled window is the record of what happened. Asserted through the real
        // composable rather than only on `isEditable`, because the property is only worth having if
        // the surface actually honours it.
        val done = fixture().let { base ->
            base.copy(
                days = base.days.map { day ->
                    day.copy(timed = day.timed.map { if (it.title == "Dentist") it.copy(outcome = OccurrenceOutcome.Done(1L)) else it })
                },
            )
        }
        setSurface(done)

        val row = done.days.flatMap { it.all }.first { it.title == "Dentist" }
        composeRule.onNodeWithTag(entryTag(row)).performScrollTo().performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertThat(composeRule.onAllNodesWithTag(TAG_ACTION_SHEET).fetchSemanticsNodes()).isEmpty()
        assertThat(skipped).isEmpty()
    }

    // ── §0.4: legal, but never silent ────────────────────────────────────────────────────

    @Test
    fun aRefusedMoveSaysSoWithBothNumbers() {
        // `SchedulePlan.TooLarge` is otherwise INVISIBLE: nothing is committed, so nothing on
        // screen changes, and the drag reads as a gesture the calendar failed to notice.
        setSurface(fixture().copy(notice = CalendarNotice.TooLarge(required = 812, limit = 500)))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("812", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("500", substring = true).assertIsDisplayed()
        write("issue-68-too-large.png")
        assertWithMessage("a shown notice must be cleared, or it re-fires on every recomposition")
            .that(noticesShown).isEqualTo(1)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────

    /**
     * The measured distance between two day columns, taken off the screen rather than computed.
     *
     * A test that recomputed the pitch from `dp` would be asserting its own arithmetic against
     * itself — the exact failure `DragToMove`'s KDoc exists to avoid one layer down. Reading two
     * drawn columns is the only source of that number the test does not own.
     */
    private fun columnPitchPx(): Float {
        val first = composeRule.onNodeWithTag(com.idomarhaim.goalpilot.feature.calendar.dayTag(monday))
            .fetchSemanticsNode().positionInRoot.x
        val second = composeRule.onNodeWithTag(com.idomarhaim.goalpilot.feature.calendar.dayTag(monday.plusDays(1)))
            .fetchSemanticsNode().positionInRoot.x
        return second - first
    }

    private fun write(
        name: String,
        node: androidx.compose.ui.test.SemanticsNodeInteraction = composeRule.onRoot(),
    ) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        val out = File(dir, name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // `DurationBoxRenderTest`'s floor: a picture of a blank screen is not evidence.
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
    }
}

/** Shared with `CalendarSurfaceUiTest`'s palette so the two render passes are comparable. */
private object LifeAreaFixture {
    val health = com.idomarhaim.goalpilot.domain.model.LifeArea(id = "la1", name = "Health", colorHex = "#22C55E")
}
