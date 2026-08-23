package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * **The author `#56` deliberately did not build** — §4.3's *create by tapping a slot*, and with it
 * the first way in this product's life to type a `BLOCK` or a `SPAN`
 * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * `OccurrenceDraft`'s KDoc states the gap: both rungs are *"modelled, stored, derived, reminded and
 * reviewed end to end; what they do not have is a way to **type** one"*. Its own justification for
 * living in the domain applies here unchanged — *"a rule that can only be exercised on a running
 * device is a rule whose branches do not all get tested"* — so every transition below is checked
 * with no emulator.
 */
class SlotDraftTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)

    // ── What a tap on a slot produces ────────────────────────────────────────────────────

    @Test
    fun `tapping a slot opens a block starting at that hour`() {
        val draft = SlotDraft.atSlot(monday, hour = 14)

        assertThat(draft.rung).isEqualTo(AuthoredRung.BLOCK)
        assertThat(draft.date).isEqualTo(monday)
        assertThat(draft.startTime).isEqualTo(LocalTime.of(14, 0))
    }

    @Test
    fun `a tapped slot is a half-hour chore by default, the same answer the duration box gives`() {
        // Two surfaces inventing two different defaults for "how long is a thing" is 0.3 in
        // miniature, so this reads TaskDuration.DEFAULT_MINUTES rather than a 60 of its own.
        val draft = SlotDraft.atSlot(monday, hour = 9)

        assertThat(draft.minutes).isEqualTo(TaskDuration.DEFAULT_MINUTES)
    }

    @Test
    fun `a tap in the last hour of the day gives a block that runs into tomorrow`() {
        // The defect this case was written to describe and then found: `LocalTime.plusMinutes`
        // WRAPS, so 23:45 + 30 min is 00:15, and read as a time on the same date that is fourteen
        // hours BEFORE the start -- which Block.closesAt then coerces to zero length. The draft
        // compiled and read perfectly, and produced a block invisible on the grid.
        val draft = SlotDraft.atSlot(monday, hour = 23, minute = 45)
        val occurrence = draft.toOccurrence() as Block

        assertThat(draft.crossesMidnight).isTrue()
        assertThat(draft.minutes).isEqualTo(TaskDuration.DEFAULT_MINUTES)
        assertThat(occurrence.start).isEqualTo(monday.atTime(23, 45))
        assertThat(occurrence.end).isEqualTo(monday.plusDays(1).atTime(0, 15))
        assertThat(occurrence.closesAt).isGreaterThan(occurrence.start)
    }

    @Test
    fun `a night block can be typed at all`() {
        // The half a rule that rejects an earlier end would have made unreachable: 23:00-01:00 is
        // an ordinary evening, and pushing it back to 23:05 would delete the shape rather than
        // correct it.
        val draft = SlotDraft.atSlot(monday, hour = 23).withEnd(LocalTime.of(1, 0))

        assertThat(draft.crossesMidnight).isTrue()
        assertThat(draft.minutes).isEqualTo(120)
        assertThat((draft.toOccurrence() as Block).end).isEqualTo(monday.plusDays(1).atTime(1, 0))
    }

    @Test
    fun `moving the start of a night block keeps its length across midnight`() {
        val draft = SlotDraft.atSlot(monday, hour = 22).withEnd(LocalTime.of(0, 30))

        val moved = draft.withStart(LocalTime.of(23, 0))

        assertThat(moved.minutes).isEqualTo(draft.minutes)
        assertThat(moved.endTime).isEqualTo(LocalTime.of(1, 30))
        assertThat(moved.crossesMidnight).isTrue()
    }

    // ── What it writes ───────────────────────────────────────────────────────────────────

    @Test
    fun `a block draft writes a BLOCK on the tapped day`() {
        val occurrence = SlotDraft.atSlot(monday, hour = 9).copy(title = "Deep work").toOccurrence()

        assertThat(occurrence.rung).isEqualTo(OccurrenceRung.BLOCK)
        assertThat(occurrence).isInstanceOf(Block::class.java)
        assertThat((occurrence as Block).start).isEqualTo(monday.atTime(9, 0))
    }

    @Test
    fun `a block a person typed is CONFIRMED, not PROVISIONAL`() {
        // 2.4 requires confirmation for an AGENT's placement, "because 09:00 may already be
        // taken". A block the person typed is one they endorsed by typing it -- the same reading
        // BlockPlacement.fromName already committed to. PROVISIONAL belongs to 3.7's batch sheet
        // and stays unreachable from any human-facing control.
        val block = SlotDraft.atSlot(monday, hour = 9).toOccurrence() as Block

        assertThat(block.placement).isEqualTo(BlockPlacement.CONFIRMED)
        // ...and therefore its miss is a real MISSED rather than a silent EXPIRED.
        assertThat(block.missState).isEqualTo(OccurrenceState.MISSED)
    }

    @Test
    fun `a span draft writes a SPAN over the chosen days`() {
        val occurrence = SlotDraft(date = monday, title = "Renovation")
            .withRung(AuthoredRung.SPAN)
            .withEndDate(monday.plusDays(6))
            .toOccurrence()

        assertThat(occurrence.rung).isEqualTo(OccurrenceRung.SPAN)
        assertThat(occurrence).isEqualTo(Span(monday, monday.plusDays(6)))
    }

    @Test
    fun `a draft always has a when, unlike the add-task row's`() {
        // The difference between the two authors: OccurrenceDraft.toOccurrence returns null for
        // "no occurrence at all", which is "the honest state for the majority of tasks, which are
        // simply on the list". This sheet is only ever opened BY placing something on a calendar.
        AuthoredRung.entries.forEach { rung ->
            assertThat(SlotDraft(date = monday).withRung(rung).toOccurrence().rung).isEqualTo(rung.rung)
        }
    }

    // ── Moving the start carries the end ─────────────────────────────────────────────────

    @Test
    fun `moving the start keeps the block's length`() {
        val draft = SlotDraft.atSlot(monday, hour = 9).withEnd(LocalTime.of(11, 0))

        val moved = draft.withStart(LocalTime.of(14, 0))

        assertThat(moved.startTime).isEqualTo(LocalTime.of(14, 0))
        assertThat(moved.endTime).isEqualTo(LocalTime.of(16, 0))
        assertThat(moved.minutes).isEqualTo(draft.minutes)
    }

    @Test
    fun `moving the start past the old end does not produce a zero-length block`() {
        // The failure this transition exists to prevent: a calendar that leaves the end where it
        // was turns "actually, start at 11" into a zero-length block, and the user's next action
        // is to fix a duration they never changed.
        val draft = SlotDraft.atSlot(monday, hour = 9).withEnd(LocalTime.of(10, 0))

        val moved = draft.withStart(LocalTime.of(15, 0))

        assertThat(moved.minutes).isEqualTo(60)
        assertThat(moved.endTime).isEqualTo(LocalTime.of(16, 0))
    }

    // ── The end is never before the start ────────────────────────────────────────────────

    @Test
    fun `an end before the start reads as the following morning, not as a negative`() {
        // The deliberate reading, and the cost is stated in withEnd's KDoc: a mistaken 23-hour
        // block is visible immediately (the sheet shows the duration, the bar reddens), while a
        // rule that "corrected" it would make a night block impossible to type at all.
        val draft = SlotDraft.atSlot(monday, hour = 9).withEnd(LocalTime.of(8, 0))

        assertThat(draft.crossesMidnight).isTrue()
        assertThat(draft.minutes).isEqualTo(23 * 60)
        assertThat((draft.toOccurrence() as Block).end).isEqualTo(monday.plusDays(1).atTime(8, 0))
    }

    @Test
    fun `an end equal to the start is pushed off it`() {
        // The one case with no second reading: a zero-length block is nobody's intent, and neither
        // is a twenty-four-hour one, so equality cannot mean "next day".
        val draft = SlotDraft.atSlot(monday, hour = 9).withEnd(LocalTime.of(9, 0))

        assertThat(draft.crossesMidnight).isFalse()
        assertThat(draft.minutes).isEqualTo(TaskDuration.MIN_MINUTES)
    }

    @Test
    fun `a later end is taken as given`() {
        val draft = SlotDraft.atSlot(monday, hour = 9).withEnd(LocalTime.of(17, 30))

        assertThat(draft.endTime).isEqualTo(LocalTime.of(17, 30))
        assertThat(draft.minutes).isEqualTo(8 * 60 + 30)
    }

    // ── A span's last day is never before its first ──────────────────────────────────────

    @Test
    fun `a span's end day is never before its start day`() {
        val draft = SlotDraft(date = monday).withRung(AuthoredRung.SPAN).withEndDate(monday.minusDays(3))

        assertThat(draft.endDate).isEqualTo(monday)
        assertThat(draft.toOccurrence()).isEqualTo(Span(monday, monday))
    }

    @Test
    fun `moving a span's first day carries its last`() {
        val draft = SlotDraft(date = monday).withRung(AuthoredRung.SPAN).withEndDate(monday.plusDays(4))

        val moved = draft.withDate(monday.plusDays(10))

        assertThat(moved.date).isEqualTo(monday.plusDays(10))
        assertThat(moved.endDate).isEqualTo(monday.plusDays(14))
    }

    // ── Switching rung keeps the day and nothing else ────────────────────────────────────

    @Test
    fun `switching to a span keeps the tapped day`() {
        // The day is what the user chose by tapping; the times were this sheet's defaults.
        val draft = SlotDraft.atSlot(monday, hour = 14).withRung(AuthoredRung.SPAN)

        assertThat(draft.date).isEqualTo(monday)
        assertThat(draft.endDate).isEqualTo(monday)
        assertThat(draft.toOccurrence()).isEqualTo(Span(monday, monday))
    }

    @Test
    fun `switching back to a block does not inherit the span's end day as a time`() {
        val draft = SlotDraft(date = monday)
            .withRung(AuthoredRung.SPAN)
            .withEndDate(monday.plusDays(5))
            .withRung(AuthoredRung.BLOCK)

        assertThat(draft.startTime).isEqualTo(SlotDraft.DEFAULT_START)
        assertThat(draft.minutes).isEqualTo(TaskDuration.DEFAULT_MINUTES)
        assertThat((draft.toOccurrence() as Block).start.toLocalDate()).isEqualTo(monday)
    }

    @Test
    fun `switching to the rung it already has changes nothing`() {
        val draft = SlotDraft.atSlot(monday, hour = 14).withEnd(LocalTime.of(18, 0))

        assertThat(draft.withRung(AuthoredRung.BLOCK)).isEqualTo(draft)
    }

    // ── A span books no hours, whatever times the draft happens to hold ──────────────────

    @Test
    fun `a span reports zero minutes even when block times are still on the draft`() {
        // 2.2: "spans contribute nothing". The draft keeps its block times so switching back is
        // lossless within one edit, and `minutes` must still answer for the rung in force.
        val draft = SlotDraft(date = monday, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(17, 0))
            .copy(rung = AuthoredRung.SPAN)

        assertThat(draft.minutes).isEqualTo(0)
    }

    // ── Validity ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `a draft with no title cannot be saved`() {
        assertThat(SlotDraft.atSlot(monday, hour = 9).isValid).isFalse()
        assertThat(SlotDraft.atSlot(monday, hour = 9).copy(title = "   ").isValid).isFalse()
        assertThat(SlotDraft.atSlot(monday, hour = 9).copy(title = "Gym").isValid).isTrue()
    }
}
