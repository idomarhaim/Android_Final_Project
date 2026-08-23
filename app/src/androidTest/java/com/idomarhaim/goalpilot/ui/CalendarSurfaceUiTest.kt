package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.feature.calendar.CalendarBuilder
import com.idomarhaim.goalpilot.feature.calendar.CalendarEntry
import com.idomarhaim.goalpilot.feature.calendar.CalendarSurface
import com.idomarhaim.goalpilot.feature.calendar.CalendarUiState
import com.idomarhaim.goalpilot.feature.calendar.CalendarZoom
import com.idomarhaim.goalpilot.feature.calendar.EntryKind
import com.idomarhaim.goalpilot.feature.calendar.SlotDraft
import com.idomarhaim.goalpilot.feature.calendar.TAG_FAB
import com.idomarhaim.goalpilot.feature.calendar.TAG_GRID
import com.idomarhaim.goalpilot.feature.calendar.TAG_LOAD_BAR
import com.idomarhaim.goalpilot.feature.calendar.TAG_SAVE
import com.idomarhaim.goalpilot.feature.calendar.TAG_SHEET
import com.idomarhaim.goalpilot.feature.calendar.TAG_SPAN_DAYS
import com.idomarhaim.goalpilot.feature.calendar.TAG_SURFACE
import com.idomarhaim.goalpilot.feature.calendar.TAG_TITLE
import com.idomarhaim.goalpilot.feature.calendar.TAG_UNTIMED
import com.idomarhaim.goalpilot.feature.calendar.AuthoredRung
import com.idomarhaim.goalpilot.feature.calendar.dayTag
import com.idomarhaim.goalpilot.feature.calendar.rungTag
import com.idomarhaim.goalpilot.feature.calendar.slotTag
import com.idomarhaim.goalpilot.feature.calendar.zoomTag
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * §4.3's calendar surface, on a device
 * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * ### What this layer is for, and what it deliberately leaves to the JVM suite
 *
 * Every rule §4.3 states — the lanes, the load bar, the booked/free ring, carry-forward, *a
 * `DEADLINE` is never a timed box* — is arithmetic over pure types and is asserted in
 * `app/src/test/.../feature/calendar/`, with no device. Repeating any of it here would buy a slower
 * copy of a check that already exists.
 *
 * What only a device can answer is whether the surface **composes and responds**: that three lanes
 * and four rung forms can be on screen together without one swallowing another, that the zoom
 * control really swaps the layout, and that §4.3's *create by tapping a slot* reaches the sheet that
 * authors a `BLOCK` — which is the affordance this whole ticket exists to add.
 *
 * ### It drives [CalendarSurface], not `CalendarScreen`
 *
 * The Hilt-wired screen needs a signed-in account and a live Firestore, so a test of it would be a
 * test of the network with a calendar attached — non-deterministic, and empty on a fresh device
 * exactly when it matters. The stateless surface takes its state as a parameter, so the fixture
 * below can put every lane and every rung on screen at once, including an `EXTERNAL` event nothing
 * in the app can yet produce.
 *
 * ⚠️ **Run it with `adb install -r` + `am instrument`, never `connectedDebugAndroidTest`** — that
 * task uninstalls the app and takes the Google sign-in with it
 * (`kb/dev/android-device-verification.md` §8).
 */
class CalendarSurfaceUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val noon: LocalDateTime = monday.atTime(12, 0)

    /** An hour of Monday with nothing on it -- see `tappingASlotOpensTheSheet…` for why that matters. */
    private val FREE_HOUR = 20

    private val health = LifeArea(id = "la1", name = "Health", colorHex = "#22C55E")
    private val work = LifeArea(id = "la2", name = "Work", colorHex = "#3B82F6")
    private val runGoal = Goal(id = "g1", title = "Run 100 km", lifeAreaIds = listOf(health.id))
    private val shipGoal = Goal(
        id = "g2",
        title = "Ship v1",
        lifeAreaIds = listOf(work.id),
        deadlineEpochMillis = monday.plusDays(1).atTime(18, 0).atZone(zone).toInstant().toEpochMilli(),
    )

    private fun task(id: String, title: String, occurrence: com.idomarhaim.goalpilot.domain.model.Occurrence, goalId: String?) =
        TaskSchedule(
            Task(
                id = id,
                title = title,
                occurrence = occurrence,
                goalEdges = goalId?.let { listOf(GoalEdge(goalId = it)) } ?: emptyList(),
            ),
        )

    /**
     * One week with **all four rungs, all three lanes and a carried-forward row** on it.
     *
     * Deliberately not a happy path. The prototype's own note applies unchanged: *"a mockup that
     * only shows the happy path settles nothing"*, and the two things most likely to be wrong on a
     * real screen are a lane swallowing another and a long title crushing a time column — neither
     * of which an empty calendar can show.
     */
    private fun fixture(zoom: CalendarZoom = CalendarZoom.THREE_DAYS): CalendarUiState {
        val schedules = listOf(
            task("t1", "Morning run", Block(monday.atTime(7, 0), monday.atTime(8, 0)), runGoal.id),
            task("t2", "Deep work on the release", Block(monday.atTime(9, 30), monday.atTime(12, 30)), shipGoal.id),
            task("t3", "Tax return", Deadline(monday.atTime(17, 0)), null),
            task("t4", "Call the plumber", AllDay(monday), null),
            task("t5", "Redecorate the kitchen", Span(monday, monday.plusDays(2)), null),
            task("t6", "Standup", Block(monday.plusDays(1).atTime(9, 0), monday.plusDays(1).atTime(9, 15)), shipGoal.id),
            // Carried forward: overdue from three days ago, and it must reach today's column.
            task("t7", "Rent", Deadline(monday.minusDays(3).atTime(9, 0)), null),
        )
        // #61's slot, fed by hand -- nothing in the app produces one yet.
        val handMade = CalendarEntry(
            key = "gcal:1",
            title = "Dentist",
            kind = EntryKind.EXTERNAL,
            occurrence = Block(monday.atTime(14, 0), monday.atTime(15, 0)),
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
                goals = listOf(runGoal, shipGoal),
                lifeAreas = listOf(health, work),
                challenges = listOf(
                    Challenge(
                        id = "c1",
                        title = "August streak",
                        startAtEpochMillis = monday.minusDays(5).atStartOfDay(zone).toInstant().toEpochMilli(),
                        endAtEpochMillis = monday.plusDays(5).atStartOfDay(zone).toInstant().toEpochMilli(),
                    ),
                ),
                external = listOf(handMade),
                waking = WakingHours.DEFAULT,
                zone = zone,
            ),
            goals = listOf(runGoal, shipGoal),
            isLoading = false,
        )
    }

    private fun setSurface(
        state: CalendarUiState = fixture(),
        onCreate: (SlotDraft) -> Unit = {},
    ) {
        composeRule.setContent {
            GoalPilotTheme { CalendarSurface(state = state, onCreate = onCreate) }
        }
    }

    /**
     * @param node what to photograph. Defaults to the root — but a
     *   [com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet] renders in a **window of its own**,
     *   so with a sheet open `onRoot()` matches TWO roots and `captureToImage` refuses: *"expected
     *   exactly 1 node but found 2 that satisfy (isRoot)"*. The sheet's own node is the picture
     *   wanted anyway.
     */
    private fun write(
        name: String,
        node: androidx.compose.ui.test.SemanticsNodeInteraction = composeRule.onRoot(),
    ) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        val out = File(dir, name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // The floor DurationBoxRenderTest records: a render pass that photographed three empty
        // boxes proved nothing. A picture of a blank screen is not evidence.
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(100)
    }

    // ── The surface composes, with everything on it at once ──────────────────────────────

    @Test
    fun theSurfaceDrawsThreeColumnsWithEveryLaneOccupied() {
        setSurface()

        composeRule.onNodeWithTag(TAG_SURFACE).assertIsDisplayed()
        (0..2).forEach { composeRule.onNodeWithTag(dayTag(monday.plusDays(it.toLong()))).assertIsDisplayed() }

        // GRID -- a block, drawn in the hour grid.
        composeRule.onNodeWithText("Morning run").assertIsDisplayed()
        // ALL_DAY -- a deadline as a banner, a span capsule, a challenge window.
        composeRule.onNodeWithText("Tax return").assertIsDisplayed()
        // A three-day span draws on THREE columns, and a challenge window spanning the range
        // draws on all three too. `onNodeWithText` was the wrong selector here and said so:
        // "expected at most 1 node but found 3". That is CalendarEntry.covers working -- keying a
        // column on the start date alone is what made a month-long window vanish after day one.
        assertThat(composeRule.onAllNodesWithText("Redecorate the kitchen").fetchSemanticsNodes()).hasSize(3)
        assertThat(composeRule.onAllNodesWithText("August streak").fetchSemanticsNodes()).hasSize(3)
        // UNTIMED -- 4.3's strip for work due today with no time.
        composeRule.onNodeWithTag(TAG_UNTIMED).assertIsDisplayed()
        composeRule.onNodeWithText("Call the plumber").assertIsDisplayed()
    }

    @Test
    fun theChipCarriesTheLifeAreaName() {
        setSurface()

        // 4.3: the chip carries ONLY the life area -- a colour dot and its name. This asserts the
        // name is legible at three-day width, which is the measurement 4.3 chose the default for.
        composeRule.onNodeWithText("Health").assertIsDisplayed()
    }

    @Test
    fun overdueWorkFromAnotherDayReachesTodaysColumn() {
        setSurface()

        // 4.3: OVERDUE is carried forward, because it "keeps reminding" and needs action now. The
        // sweep is JVM-tested; what this says is that the row survives onto a real screen rather
        // than being computed and then drawn nowhere.
        composeRule.onNodeWithText("Rent").assertIsDisplayed()
    }

    @Test
    fun aHandMadeGoogleEventDrawsBesideTheAppsOwnWork() {
        setSurface()

        // #61's slot. Nothing in the app can produce one yet, so this is fed by hand -- which is
        // the point: the day #61 ships, the change is a flow, not a lane.
        composeRule.onNodeWithText("Dentist").assertIsDisplayed()
    }

    @Test
    fun everyColumnCarriesALoadBar() {
        setSurface()

        assertThat(composeRule.onAllNodesWithTag(TAG_LOAD_BAR).fetchSemanticsNodes()).hasSize(3)
    }

    // ── The zoom really changes the layout ───────────────────────────────────────────────

    // `setContent` may be called only ONCE per test, so the two zooms are two tests rather than
    // one that swaps. Driving the control instead would test the control; what is being asserted
    // here is that each zoom produces a structurally different layout.

    @Test
    fun theAgendaZoomDrawsNoHourGridAtAll() {
        setSurface(fixture(CalendarZoom.AGENDA))

        // 4.2 calls the agenda the calendar's lowest zoom rather than a third screen, and the
        // visible difference is exactly this: a list, with no hour grid behind it.
        composeRule.onNodeWithText("Morning run").assertIsDisplayed()
        assertThat(composeRule.onAllNodesWithTag(TAG_GRID).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun theWeekZoomDrawsAGridPerColumn() {
        setSurface(fixture(CalendarZoom.WEEK))

        assertThat(composeRule.onAllNodesWithTag(TAG_GRID).fetchSemanticsNodes()).hasSize(7)
    }

    // ── 4.3's create-by-tapping-a-slot, which is what makes BLOCK reachable ──────────────

    @Test
    fun tappingASlotOpensTheSheetAndAuthorsABlock() {
        var created: SlotDraft? = null
        setSurface(onCreate = { created = it })

        // 20:00 on Monday, and BOTH qualifiers are load-bearing.
        // * `performScrollTo` -- the grid sits below three lanes of banners and is off the fold.
        // * a FREE hour -- entry boxes are drawn OVER the hour cells, so a tap at an occupied hour
        //   lands on the entry and opens it instead. That is right (tapping an event opens the
        //   event) and it is why the first draft of this case failed: it tapped 10:00, which sits
        //   under "Deep work on the release" (09:30-12:30).
        composeRule.onNodeWithTag(slotTag(monday, FREE_HOUR)).performScrollTo().performClick()
        composeRule.onNodeWithTag(TAG_SHEET).assertIsDisplayed()
        // ...AndSettle, never the raw `performTextInput` -- issue #58. Focusing a field raises the
        // soft keyboard, whose window-inset animation slides the SHEET upward while Compose reports
        // itself idle, so the Save click below lands where Save used to be and is silently lost.
        // `ImeSettleSweepTest` caught both of this file's call sites; they had passed anyway, which
        // is exactly the failure mode -- 1 full-suite run in 4.
        composeRule.onNodeWithTag(TAG_TITLE).performTextInputAndSettle("Write the report")
        composeRule.onNodeWithTag(TAG_SAVE).performClick()

        assertThat(created).isNotNull()
        assertThat(created!!.rung).isEqualTo(AuthoredRung.BLOCK)
        assertThat(created!!.date).isEqualTo(monday)
        assertThat(created!!.startTime.hour).isEqualTo(FREE_HOUR)
        assertThat(created!!.toOccurrence()).isInstanceOf(Block::class.java)
    }

    @Test
    fun theSheetCanAuthorASpanToo() {
        var created: SlotDraft? = null
        setSurface(onCreate = { created = it })

        composeRule.onNodeWithTag(TAG_FAB).performClick()
        composeRule.onNodeWithTag(TAG_TITLE).performTextInputAndSettle("Renovation")
        composeRule.onNodeWithTag(rungTag(AuthoredRung.SPAN)).performClick()
        composeRule.onNodeWithTag(TAG_SPAN_DAYS).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_SAVE).performClick()

        assertThat(created!!.rung).isEqualTo(AuthoredRung.SPAN)
        assertThat(created!!.toOccurrence()).isInstanceOf(Span::class.java)
    }

    @Test
    fun theSheetWillNotSaveWithoutATitle() {
        var created: SlotDraft? = null
        setSurface(onCreate = { created = it })

        composeRule.onNodeWithTag(TAG_FAB).performClick()
        composeRule.onNodeWithTag(TAG_SAVE).performClick()

        assertThat(created).isNull()
    }

    // ── The render pass 4.3's own rules cannot assert ────────────────────────────────────

    /**
     * §0.8: *"Every screen is designed, and is not finished until it has been seen."*
     *
     * What a test cannot say about this surface is the thing §4.3 spent its whole prototype on:
     * whether the **form of the time column** reads as four different kinds of commitment with no
     * legend — a rail, a point, a capsule and two words — and whether three lanes stacked above an
     * hour grid still read as one day rather than as four unrelated bands.
     *
     * **English only.** `AGENTS.md`'s freeze block: §0.8's *"not finished until seen in Hebrew"*
     * sub-rule is suspended, and a design is finished for now when it has been seen in English.
     */
    @Test
    fun theThreeDayCalendar_writtenToAPngIcanLookAt() {
        setSurface()

        composeRule.onNodeWithText("Morning run").assertIsDisplayed()
        composeRule.onNodeWithText("Tax return").assertIsDisplayed()

        write("issue-60-calendar-3day.png")
    }

    @Test
    fun theWeekCalendar_writtenToAPngIcanLookAt() {
        setSurface(fixture(CalendarZoom.WEEK))

        // The floor, and it is the measurement 4.3 turns on: at ~46 dp a column, the times have to
        // be stacked start-over-end. If nothing is on screen the picture cannot show whether they
        // survived the width.
        composeRule.onNodeWithTag(dayTag(monday)).assertIsDisplayed()

        write("issue-60-calendar-week.png")
    }

    @Test
    fun theAgenda_writtenToAPngIcanLookAt() {
        setSurface(fixture(CalendarZoom.AGENDA))

        composeRule.onNodeWithText("Morning run").assertIsDisplayed()

        write("issue-60-calendar-agenda.png")
    }

    @Test
    fun theSlotSheet_writtenToAPngIcanLookAt() {
        setSurface()

        composeRule.onNodeWithTag(slotTag(monday, FREE_HOUR)).performScrollTo().performClick()
        composeRule.onNodeWithTag(TAG_SHEET).assertIsDisplayed()

        write("issue-60-slot-sheet.png", composeRule.onNodeWithTag(TAG_SHEET))
    }
}
