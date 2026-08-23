package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.EditScope
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.RepeatEnd
import com.idomarhaim.goalpilot.domain.model.RepeatRule
import com.idomarhaim.goalpilot.domain.model.RepeatUnit
import com.idomarhaim.goalpilot.domain.model.ScheduleEdit
import com.idomarhaim.goalpilot.domain.model.ScheduleEdits
import com.idomarhaim.goalpilot.domain.model.SchedulePlan
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.startDate
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * §4.3's *drag to move*, and the mapping from a gesture to an `EditScope`
 * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
 *
 * ### What this layer is for, and why it is the layer that matters
 *
 * `ScheduleEdits` is pure and `#63` already tested it. What `#68` adds is everything **in front**
 * of it: which day and minute a finger's travel means, whether §2.1's question is asked at all, and
 * which scope is used when it is not. The brief names the hazard exactly — *"that is where a wrong
 * answer is silent"* — and it is silent because every wrong answer here still renders as a block
 * on a calendar. Nobody looking at the screen can tell 09:00-moved-to-Tuesday from
 * 09:00-moved-to-Wednesday unless they already knew which they asked for.
 *
 * So the assertions below are about **arithmetic and rules**, with no device and no Compose. The
 * instrumented suite's job is the complementary one: that the gesture arrives at all.
 *
 * ### Several of these run the real `ScheduleEdits`
 *
 * Not to re-test it — to test that the scope this package chooses produces the write the product
 * wants. A test that stopped at *"we passed `THIS_AND_FUTURE`"* would pass just as happily if that
 * were the wrong constant, which is the entire failure mode being guarded.
 */
class DragToMoveTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val week: List<LocalDate> = (0L until 7L).map { monday.plusDays(it) }

    /** One column of the three-day zoom, in pixels: ~110 dp at 3x, and an hour row of 44 dp at 3x. */
    private val geometry = DragToMove.Geometry(
        columnPitchPx = 330f,
        hourHeightPx = 132f,
        days = week.take(3),
    )

    private fun entry(
        occurrence: Occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 30)),
        kind: EntryKind = EntryKind.TASK,
        outcome: OccurrenceOutcome = OccurrenceOutcome.Planned,
        seriesDate: LocalDate? = null,
        isRepeating: Boolean = false,
        occurrenceId: String? = null,
        carriedForward: Boolean = false,
    ) = CalendarEntry(
        key = "k",
        title = "Morning run",
        kind = kind,
        occurrence = occurrence,
        taskId = "t1",
        occurrenceId = occurrenceId,
        outcome = outcome,
        seriesDate = seriesDate,
        isRepeating = isRepeating,
        carriedForward = carriedForward,
    )

    // ── Where a drag lands ───────────────────────────────────────────────────────────────

    @Test
    fun `a drag of nothing lands where it started`() {
        val target = DragToMove.targetOf(entry(), dragXPx = 0f, dragYPx = 0f, geometry = geometry)

        assertThat(target).isEqualTo(DragToMove.Target(monday, 9 * 60))
    }

    @Test
    fun `one column of travel is one day`() {
        val target = DragToMove.targetOf(entry(), dragXPx = 330f, dragYPx = 0f, geometry = geometry)!!

        assertThat(target.date).isEqualTo(monday.plusDays(1))
        assertThat(target.time).isEqualTo(LocalTime.of(9, 0))
    }

    @Test
    fun `half a column rounds to the nearer one, in both directions`() {
        // The pitch is what the rounding is against, and it is why `Geometry` takes a pitch rather
        // than a drawn width -- see its KDoc. At 0.6 of a column the finger is visibly over the
        // next day and the answer has to agree with the eye.
        val forward = DragToMove.targetOf(entry(), 0.6f * 330f, 0f, geometry)!!
        val back = DragToMove.targetOf(entry(occurrence = Block(monday.plusDays(1).atTime(9, 0), monday.plusDays(1).atTime(10, 0))), -0.6f * 330f, 0f, geometry)!!

        assertThat(forward.date).isEqualTo(monday.plusDays(1))
        assertThat(back.date).isEqualTo(monday)
    }

    @Test
    fun `one hour row of travel is one hour`() {
        val target = DragToMove.targetOf(entry(), 0f, 132f, geometry)!!

        assertThat(target.time).isEqualTo(LocalTime.of(10, 0))
    }

    @Test
    fun `a landing time is snapped to the quarter hour`() {
        // Twenty minutes down. Un-snapped that is 09:20; the grid draws hours, so the honest answer
        // is the nearest grain it can express.
        val target = DragToMove.targetOf(entry(), 0f, 132f / 3f, geometry)!!

        assertThat(target.minuteOfDay % DragToMove.SNAP_MINUTES).isEqualTo(0)
        assertThat(target.time).isEqualTo(LocalTime.of(9, 15))
    }

    @Test
    fun `a drag past the last drawn column stops on it rather than doing nothing`() {
        val target = DragToMove.targetOf(entry(), dragXPx = 40f * 330f, dragYPx = 0f, geometry = geometry)!!

        assertThat(target.date).isEqualTo(geometry.days.last())
    }

    @Test
    fun `a drag past the first drawn column stops on it`() {
        val target = DragToMove.targetOf(entry(), dragXPx = -40f * 330f, dragYPx = 0f, geometry = geometry)!!

        assertThat(target.date).isEqualTo(geometry.days.first())
    }

    @Test
    fun `a landing never leaves the drawn hours, at either end`() {
        // Both ends, and the far end is the one with the subtle answer: the last LEGAL START, not
        // the grid's closing edge, or the block opens on a row with no height.
        val up = DragToMove.targetOf(entry(), 0f, -100f * 132f, geometry)!!
        val down = DragToMove.targetOf(entry(), 0f, 100f * 132f, geometry)!!

        assertThat(up.minuteOfDay).isEqualTo(geometry.hourFrom * 60)
        assertThat(down.minuteOfDay).isEqualTo(geometry.hourTo * 60 - geometry.snapMinutes)
    }

    @Test
    fun `a clamped landing is still on the grain`() {
        // The regression this guards: snapping BEFORE clamping can round a landing back off the
        // grain, because the last legal start need not be a multiple of it. Asserted as a sweep so
        // it survives someone changing the crop or the grain.
        val offGrain = DragToMove.Geometry(
            columnPitchPx = 330f,
            hourHeightPx = 132f,
            days = week.take(3),
            hourTo = 23,
            snapMinutes = 20,
        )
        val down = DragToMove.targetOf(entry(), 0f, 100f * 132f, offGrain)!!

        assertWithMessage("a landing clamped to the grid's end must still be on the grain")
            .that(down.minuteOfDay % offGrain.snapMinutes).isEqualTo(0)
    }

    @Test
    fun `an unmeasured geometry places nothing`() {
        val unmeasured = DragToMove.Geometry(columnPitchPx = 0f, hourHeightPx = 0f, days = emptyList())

        assertThat(DragToMove.targetOf(entry(), 100f, 100f, unmeasured)).isNull()
        assertThat(unmeasured.isMeasured).isFalse()
    }

    @Test
    fun `an entry whose own day is not drawn places nothing`() {
        val elsewhere = entry(occurrence = Block(monday.minusDays(9).atTime(9, 0), monday.minusDays(9).atTime(10, 0)))

        assertThat(DragToMove.targetOf(elsewhere, 0f, 0f, geometry)).isNull()
    }

    // ── What the move produces ───────────────────────────────────────────────────────────

    @Test
    fun `a moved block keeps its duration and its placement`() {
        val ninety = entry(
            occurrence = Block(
                start = monday.atTime(9, 0),
                end = monday.atTime(10, 30),
                placement = BlockPlacement.PROVISIONAL,
            ),
        )

        val moved = DragToMove.movedTo(ninety, DragToMove.Target(monday.plusDays(2), 14 * 60 + 15)) as Block

        assertThat(moved.start).isEqualTo(monday.plusDays(2).atTime(14, 15))
        assertThat(moved.end).isEqualTo(monday.plusDays(2).atTime(15, 45))
        assertThat(moved.placement).isEqualTo(BlockPlacement.PROVISIONAL)
    }

    @Test
    fun `every rung survives a move with its own kind intact`() {
        // A sweep rather than a case per rung, for `CalendarLaneTest`'s reason: a fifth rung added
        // later must not land here with an invented hour. Only BLOCK is ever in the grid today, and
        // only BLOCK's time moves -- the rest keep their shape and change day.
        OccurrenceRung.entries.forEach { rung ->
            val sample = when (rung) {
                OccurrenceRung.BLOCK -> Block(monday.atTime(9, 0), monday.atTime(10, 0))
                OccurrenceRung.DEADLINE -> Deadline(monday.atTime(18, 0))
                OccurrenceRung.ALL_DAY -> AllDay(monday)
                OccurrenceRung.SPAN -> Span(monday, monday.plusDays(2))
            }
            val moved = DragToMove.movedTo(entry(occurrence = sample), DragToMove.Target(monday.plusDays(1), 14 * 60))

            assertWithMessage("$rung must not change rung when moved").that(moved.rung).isEqualTo(rung)
            assertWithMessage("$rung must land on the day it was dropped on")
                .that(moved.startDate).isEqualTo(monday.plusDays(1))
        }
    }

    @Test
    fun `a press is told from a move by how far it travelled`() {
        assertThat(DragToMove.isMove(0f, 0f)).isFalse()
        assertThat(DragToMove.isMove(3f, 4f)).isFalse()
        assertThat(DragToMove.isMove(0f, DragToMove.PRESS_SLOP_PX)).isTrue()
        assertThat(DragToMove.isMove(-200f, 0f)).isTrue()
    }

    // ── Whether the question is asked at all ─────────────────────────────────────────────

    @Test
    fun `a repeating task is asked which scope`() {
        val instance = entry(seriesDate = monday, isRepeating = true)

        assertThat(MoveScope.isAsked(instance)).isTrue()
    }

    @Test
    fun `a one-off is never asked, because there is nothing to choose between`() {
        assertThat(MoveScope.isAsked(entry(isRepeating = false))).isFalse()
    }

    @Test
    fun `nothing but a task is editable`() {
        // §2.1's question is about an occurrence document behind a schedule. A goal deadline, a
        // challenge window and an EXTERNAL event have none.
        EntryKind.entries.filterNot { it == EntryKind.TASK }.forEach { kind ->
            assertWithMessage("$kind has no schedule to edit")
                .that(entry(kind = kind, isRepeating = true, seriesDate = monday).isEditable).isFalse()
        }
    }

    @Test
    fun `a settled occurrence is history and is not edited`() {
        // §2.3 -- "a missed occurrence is never edited; it is history" -- and §2.8's "past events
        // stay as the record of time actually spent". Both directions of settled, because a skip
        // is as much a decision as a tick.
        val done = entry(seriesDate = monday, isRepeating = true, outcome = OccurrenceOutcome.Done(1L))
        val skipped = entry(seriesDate = monday, isRepeating = true, outcome = OccurrenceOutcome.Skipped(1L))

        assertThat(done.isEditable).isFalse()
        assertThat(skipped.isEditable).isFalse()
        assertThat(MoveScope.isAsked(done)).isFalse()
    }

    @Test
    fun `a one-off that already has a document is editable, now that apply can address it`() {
        // The inversion of the guard `#68` put here and `#69` took off, kept as a test rather than
        // deleted because it is the assertion that would fail if the third condition were ever put
        // back. Until `ScheduleEdits.apply` could look an instance up by a NULL seriesDate, a
        // one-off's document -- which carries `seriesDate = null` by construction -- was
        // unreachable, and `CalendarEntry.isEditable` refused the row rather than let both scopes
        // fail silently. The lookup was widened, so the refusal went with it.
        val linked = entry(isRepeating = false, occurrenceId = "occ-1", seriesDate = null)

        assertThat(linked.isEditable).isTrue()
        assertWithMessage("the Google-linked one-off is the case the guard actually cost -- #61 mints its document")
            .that(linked.isDraggable).isTrue()
    }

    @Test
    fun `a one-off with no document yet is editable`() {
        assertThat(entry(isRepeating = false, occurrenceId = null).isEditable).isTrue()
    }

    @Test
    fun `only the grid offers a drag`() {
        // Everything else reaches Skip through the menu; nothing else has a geometry to drop onto.
        val inGrid = entry(occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 0)))
        val banner = entry(occurrence = Deadline(monday.atTime(18, 0)))
        val untimed = entry(occurrence = AllDay(monday))
        val carried = inGrid.copy(carriedForward = true)

        assertThat(inGrid.isDraggable).isTrue()
        assertThat(banner.isDraggable).isFalse()
        assertThat(untimed.isDraggable).isFalse()
        assertWithMessage("a carried entry is drawn in the banner strip, so it has no slot to move within")
            .that(carried.isDraggable).isFalse()
        assertThat(banner.isEditable).isTrue()
        assertThat(untimed.isEditable).isTrue()
    }

    // ── The scope chosen when nothing is asked, run through the real ScheduleEdits ───────

    @Test
    fun `moving a one-off writes the task's own anchor and manufactures no document`() {
        // The decision `MoveScope.whenNotAsked` exists for. A one-off's *when* IS `Task.occurrence`
        // -- every other surface reads it -- so a move that left it alone and wrote a document
        // instead would be §0.3's second number that quietly disagrees.
        val task = Task(id = "t1", title = "Dentist", occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 0)))
        val schedule = TaskSchedule(task = task)
        val row = entry(isRepeating = false)
        val moved = DragToMove.movedTo(row, DragToMove.Target(monday.plusDays(1), 14 * 60))

        val plan = ScheduleEdits.apply(
            schedule = schedule,
            seriesDate = MoveScope.seriesDateOf(row),
            edit = ScheduleEdit.MoveTo(moved),
            scope = MoveScope.whenNotAsked,
            nowEpochMillis = 0L,
        ) as SchedulePlan.Writes

        assertThat(plan.task.occurrence).isEqualTo(moved)
        assertWithMessage("a one-off must not gain an occurrence document -- §2.1 rejects that shape")
            .that(plan.upserts).isEmpty()
        assertThat(plan.deletes).isEmpty()
    }

    @Test
    fun `the rejected scope for a one-off really does produce the wrong write`() {
        // The negative half, and it is what makes the test above mean something: `THIS_OCCURRENCE`
        // is not merely a different route to the same place. It leaves the anchor stale AND creates
        // the document §2.1 refuses. If this ever stops being true, `whenNotAsked` can be revisited.
        val task = Task(id = "t1", title = "Dentist", occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 0)))
        val row = entry(isRepeating = false)
        val moved = DragToMove.movedTo(row, DragToMove.Target(monday.plusDays(1), 14 * 60))

        val plan = ScheduleEdits.apply(
            schedule = TaskSchedule(task = task),
            seriesDate = MoveScope.seriesDateOf(row),
            edit = ScheduleEdit.MoveTo(moved),
            scope = EditScope.THIS_OCCURRENCE,
            nowEpochMillis = 0L,
        ) as SchedulePlan.Writes

        assertThat(plan.task.occurrence).isEqualTo(task.occurrence)
        assertThat(plan.upserts).hasSize(1)
    }

    // -- A one-off that ALREADY has a document (#69) ----------------------------------------

    @Test
    fun `moving a linked one-off through THIS_OCCURRENCE overwrites its document`() {
        // `#69`'s first direction. The lookup used to be
        // `stored.firstOrNull { it.seriesDate == seriesDate }` over a NON-NULL seriesDate, so it
        // could never match a document whose seriesDate is null; control fell through to
        // `instanceOn`, whose result has a BLANK id, and a blank id makes the repository CREATE.
        // The calendar then drew the row twice.
        val row = linkedRow()
        val moved = DragToMove.movedTo(row, DragToMove.Target(monday.plusDays(1), 14 * 60))

        val plan = ScheduleEdits.apply(
            schedule = linkedOneOff(),
            seriesDate = MoveScope.seriesDateOf(row),
            edit = ScheduleEdit.MoveTo(moved),
            scope = EditScope.THIS_OCCURRENCE,
            nowEpochMillis = 0L,
        ) as SchedulePlan.Writes

        val upsert = plan.upserts.single()
        assertWithMessage("a blank id makes the repository create a SECOND document, and the row is drawn twice")
            .that(upsert.id).isEqualTo("occ-1")
        assertThat(upsert.occurrence).isEqualTo(moved)
        assertWithMessage("the same Google event is still the same event -- the link may not be dropped by a move")
            .that(upsert.googleEventId).isEqualTo("gcal-1")
        assertThat(plan.deletes).isEmpty()
    }

    @Test
    fun `moving a linked one-off through the default scope moves its document too`() {
        // `#69`'s second direction, and the quieter one. `moveSeries`' no-rule branch writes
        // `Task.occurrence` and used to touch no document at all -- but `TaskSchedule.occurrencesIn`
        // draws a one-off THAT HAS a document from that document (source 3), so the anchor moved
        // underneath a calendar that kept drawing the old time. The move read as a no-op.
        val row = linkedRow()
        val moved = DragToMove.movedTo(row, DragToMove.Target(monday.plusDays(1), 14 * 60))

        val plan = ScheduleEdits.apply(
            schedule = linkedOneOff(),
            seriesDate = MoveScope.seriesDateOf(row),
            edit = ScheduleEdit.MoveTo(moved),
            scope = MoveScope.whenNotAsked,
            nowEpochMillis = 0L,
        ) as SchedulePlan.Writes

        assertWithMessage("the anchor is still the one-off's *when*, and every other surface reads it")
            .that(plan.task.occurrence).isEqualTo(moved)
        val upsert = plan.upserts.single()
        assertWithMessage("the document is what the calendar draws, so leaving it behind is the no-op")
            .that(upsert.id).isEqualTo("occ-1")
        assertThat(upsert.occurrence).isEqualTo(moved)
        assertThat(upsert.googleEventId).isEqualTo("gcal-1")
        assertThat(plan.deletes).isEmpty()
    }

    @Test
    fun `after either scope the calendar draws the linked one-off once, in its new place`() {
        // The user-visible half, and the reason the two tests above are not enough on their own:
        // each of them asserts about a PLAN, and what went wrong was what the plan did to the
        // DOCUMENTS. Both defects are visible here and nowhere else -- a second document draws the
        // row twice, an untouched one draws it in the old place.
        val row = linkedRow()
        val moved = DragToMove.movedTo(row, DragToMove.Target(monday.plusDays(1), 14 * 60))

        listOf(EditScope.THIS_OCCURRENCE, MoveScope.whenNotAsked).forEach { scope ->
            val before = linkedOneOff()
            val plan = ScheduleEdits.apply(
                schedule = before,
                seriesDate = MoveScope.seriesDateOf(row),
                edit = ScheduleEdit.MoveTo(moved),
                scope = scope,
                nowEpochMillis = 0L,
            ) as SchedulePlan.Writes
            val after = TaskSchedule(task = plan.task, stored = storedAfter(before.stored, plan))

            val drawn = after.occurrencesIn(monday, monday.plusDays(6), ZoneId.of("UTC"))
            assertWithMessage("under %s the calendar must draw exactly one row", scope)
                .that(drawn).hasSize(1)
            assertWithMessage("under %s the one row must be at the new time", scope)
                .that(drawn.single().occurrence).isEqualTo(moved)
        }
    }

    @Test
    fun `skipping a linked one-off settles its document rather than minting a second`() {
        // Skip travels the other leg -- `endSeries`' no-rule branch -- and it took the same lookup,
        // so it had the same blank-id hole. Both scopes, because `endSeries` says in its own KDoc
        // that a task with no rule degenerates to one write whichever scope is used.
        val row = linkedRow()

        listOf(EditScope.THIS_OCCURRENCE, MoveScope.whenNotAsked).forEach { scope ->
            val plan = ScheduleEdits.apply(
                schedule = linkedOneOff(),
                seriesDate = MoveScope.seriesDateOf(row),
                edit = ScheduleEdit.Skip,
                scope = scope,
                nowEpochMillis = 99L,
            ) as SchedulePlan.Writes

            val upsert = plan.upserts.single()
            assertWithMessage("under %s the skip must settle the document that exists", scope)
                .that(upsert.id).isEqualTo("occ-1")
            assertThat(upsert.outcome).isEqualTo(OccurrenceOutcome.Skipped(99L))
            assertThat(upsert.googleEventId).isEqualTo("gcal-1")
        }
    }

    @Test
    fun `skipping a one-off is the same write whichever scope is used`() {
        // `ScheduleEdits.endSeries` says so in its own KDoc; asserted here because `whenNotAsked`
        // is shared by both verbs and this is the half that has to be unaffected by it.
        val task = Task(id = "t1", title = "Dentist", occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 0)))
        val row = entry(isRepeating = false)

        val plans = listOf(EditScope.THIS_OCCURRENCE, EditScope.THIS_AND_FUTURE).map { scope ->
            ScheduleEdits.apply(
                schedule = TaskSchedule(task = task),
                seriesDate = MoveScope.seriesDateOf(row),
                edit = ScheduleEdit.Skip,
                scope = scope,
                nowEpochMillis = 99L,
            )
        }

        assertThat(plans[0]).isEqualTo(plans[1])
        assertThat((plans[0] as SchedulePlan.Writes).upserts.single().outcome)
            .isEqualTo(OccurrenceOutcome.Skipped(99L))
    }

    // ── The scope chosen when the sheet IS shown ─────────────────────────────────────────

    @Test
    fun `this occurrence overrides one instance and leaves the rule alone`() {
        val schedule = repeating()
        val instance = seriesRow(monday.plusDays(2))
        val moved = DragToMove.movedTo(instance, DragToMove.Target(monday.plusDays(3), 14 * 60))

        val plan = ScheduleEdits.apply(
            schedule = schedule,
            seriesDate = MoveScope.seriesDateOf(instance),
            edit = ScheduleEdit.MoveTo(moved),
            scope = EditScope.THIS_OCCURRENCE,
            nowEpochMillis = 0L,
        ) as SchedulePlan.Writes

        assertThat(plan.task).isEqualTo(schedule.task)
        assertThat(plan.upserts.single().occurrence).isEqualTo(moved)
        assertWithMessage("the override must stay pinned to the day the RULE produced it on")
            .that(plan.upserts.single().seriesDate).isEqualTo(monday.plusDays(2))
    }

    @Test
    fun `this and future moves the anchor and writes the past down first`() {
        val schedule = repeating()
        val instance = seriesRow(monday.plusDays(3))
        val moved = DragToMove.movedTo(instance, DragToMove.Target(monday.plusDays(3), 14 * 60))

        val plan = ScheduleEdits.apply(
            schedule = schedule,
            seriesDate = MoveScope.seriesDateOf(instance),
            edit = ScheduleEdit.MoveTo(moved),
            scope = EditScope.THIS_AND_FUTURE,
            nowEpochMillis = 0L,
        ) as SchedulePlan.Writes

        assertThat(plan.task.occurrence).isEqualTo(moved)
        assertWithMessage("the three days before the move must be materialised, or moving the anchor rewrites history")
            .that(plan.upserts.map { it.seriesDate })
            .containsExactly(monday, monday.plusDays(1), monday.plusDays(2))
    }

    @Test
    fun `a series long enough to overflow one batch is refused with both numbers`() {
        // §0.4's *legal, but never silent*, and the one edit that can reach it. Ten years of a daily
        // rule is far past Firestore's 500-op batch, so the plan is a refusal carrying what it would
        // have cost -- which is what `CalendarNotice.TooLarge` exists to say out loud.
        val task = Task(
            id = "t1",
            title = "Daily",
            occurrence = Block(monday.minusYears(10).atTime(9, 0), monday.minusYears(10).atTime(10, 0)),
            repeatRule = RepeatRule(unit = RepeatUnit.DAY, interval = 1),
        )
        val instance = seriesRow(monday)

        val plan = ScheduleEdits.apply(
            schedule = TaskSchedule(task = task),
            seriesDate = MoveScope.seriesDateOf(instance),
            edit = ScheduleEdit.MoveTo(Block(monday.atTime(14, 0), monday.atTime(15, 0))),
            scope = EditScope.THIS_AND_FUTURE,
            nowEpochMillis = 0L,
        )

        assertThat(plan).isInstanceOf(SchedulePlan.TooLarge::class.java)
        val refusal = plan as SchedulePlan.TooLarge
        assertThat(refusal.limit).isEqualTo(ScheduleEdits.MAX_BATCH_WRITES)
        assertThat(refusal.required).isGreaterThan(refusal.limit)

        val notice = CalendarNotice.TooLarge(required = refusal.required, limit = refusal.limit)
        assertThat(notice.required).isEqualTo(refusal.required)
        assertThat(notice.limit).isEqualTo(refusal.limit)
    }

    @Test
    fun `a moved instance is still identified by the day the rule produced it`() {
        // The whole reason `seriesDate` is carried onto the surface. A row already moved to Friday
        // must still name Wednesday when it is moved again, or the second drag edits nothing.
        val movedRow = CalendarEntry(
            key = "occ-9",
            title = "Morning run",
            kind = EntryKind.TASK,
            occurrence = Block(monday.plusDays(4).atTime(9, 0), monday.plusDays(4).atTime(10, 0)),
            taskId = "t1",
            occurrenceId = "occ-9",
            seriesDate = monday.plusDays(2),
            isRepeating = true,
        )

        assertThat(MoveScope.seriesDateOf(movedRow)).isEqualTo(monday.plusDays(2))
        assertThat(movedRow.date).isEqualTo(monday.plusDays(4))
    }

    /** The one-off's *when*, shared by its task's anchor and its document so the two agree. */
    private val oneOffAt = Block(monday.atTime(9, 0), monday.atTime(10, 0))

    /**
     * A one-off that ALREADY has an occurrence document -- the shape `#61` produces.
     *
     * `SyncCalendarUseCase.link` calls `commit` when `entry.occurrence.id.isBlank()`, which mints
     * the document with `seriesDate = null` (a one-off belongs to no series), a `googleEventId`,
     * and the outcome still `Planned`. That is the reachable case `#69`'s guard actually cost --
     * the other way a one-off gets a document is being ticked, and a settled window is already
     * refused by `isSettled` on its own account.
     */
    private fun linkedOneOff(): TaskSchedule = TaskSchedule(
        task = Task(id = "t1", title = "Dentist", occurrence = oneOffAt),
        stored = listOf(
            ScheduledOccurrence(
                id = "occ-1",
                taskId = "t1",
                occurrence = oneOffAt,
                seriesDate = null,
                googleEventId = "gcal-1",
            ),
        ),
    )

    /** The row the calendar draws for [linkedOneOff] -- drawn from the document, so it has its id. */
    private fun linkedRow() = entry(
        occurrence = oneOffAt,
        isRepeating = false,
        occurrenceId = "occ-1",
        seriesDate = null,
    )

    /**
     * What `OccurrenceRepository.apply` does to the stored documents, in memory.
     *
     * A blank id means *create* and the repository mints one (`OccurrenceRepositoryImpl.apply`); a
     * known id replaces in place; a delete removes. Written out rather than asserted around,
     * because the whole of `#69` is about which of the first two a plan lands on.
     */
    private fun storedAfter(
        before: List<ScheduledOccurrence>,
        plan: SchedulePlan.Writes,
    ): List<ScheduledOccurrence> {
        var minted = 0
        val kept = before.filterNot { it.id in plan.deletes }.toMutableList()
        plan.upserts.forEach { up ->
            val at = kept.indexOfFirst { it.id.isNotBlank() && it.id == up.id }
            if (at >= 0) kept[at] = up else kept += up.copy(id = "minted-" + (++minted))
        }
        return kept
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────

    /** A daily block from Monday, bounded, with nothing stored — the ordinary series. */
    private fun repeating(): TaskSchedule = TaskSchedule(
        task = Task(
            id = "t1",
            title = "Morning run",
            occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 0)),
            repeatRule = RepeatRule(
                unit = RepeatUnit.DAY,
                interval = 1,
                end = RepeatEnd.AfterCount(10),
            ),
        ),
        stored = emptyList<ScheduledOccurrence>(),
    )

    private fun seriesRow(on: LocalDate) = CalendarEntry(
        key = "t1@$on",
        title = "Morning run",
        kind = EntryKind.TASK,
        occurrence = Block(on.atTime(9, 0), on.atTime(10, 0)),
        taskId = "t1",
        seriesDate = on,
        isRepeating = true,
    )
}
