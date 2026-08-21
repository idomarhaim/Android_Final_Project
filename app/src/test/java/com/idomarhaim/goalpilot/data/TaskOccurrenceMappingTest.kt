package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import org.junit.Test
import java.time.LocalDate

/**
 * §2.2's occurrence across the wire (`#56`) — **and what a document written before it reads
 * as**, which is the half a round-trip test does not cover on its own.
 *
 * The migration posture is the same one `#55` took and for the same reason: the field is
 * **additive**, absence means *this task has no when*, and that is already true of every task
 * in the database. So there is no backfill, and this suite's job is to prove that the absence
 * really does read as absence rather than as a rung with a default date.
 */
class TaskOccurrenceMappingTest {

    private val day: LocalDate = LocalDate.of(2026, 8, 22)

    private fun roundTrip(occurrence: Occurrence?): Occurrence? =
        Task(id = "t1", title = "x", occurrence = occurrence).toDto().toDomain().occurrence

    // ── Round trip, per rung ───────────────────────────────────────────────────────────────

    @Test
    fun `every rung survives a round trip unchanged`() {
        assertThat(roundTrip(AllDay(day))).isEqualTo(AllDay(day))
        assertThat(roundTrip(Deadline(day.atTime(6, 0)))).isEqualTo(Deadline(day.atTime(6, 0)))
        assertThat(roundTrip(Block(day.atTime(9, 0), day.atTime(10, 30))))
            .isEqualTo(Block(day.atTime(9, 0), day.atTime(10, 30)))
        assertThat(roundTrip(Span(day, day.plusDays(4)))).isEqualTo(Span(day, day.plusDays(4)))
    }

    @Test
    fun `a block's placement survives, so a provisional block does not become a failure`() {
        val provisional = Block(day.atTime(9, 0), day.atTime(10, 0), BlockPlacement.PROVISIONAL)

        // If placement were dropped it would read back as CONFIRMED, and its lapse would turn
        // from a silent EXPIRED into a MISSED that counts against the user -- §2.3's
        // "over-eager agent manufactures failures", arriving through the mapper.
        assertThat(roundTrip(provisional)).isEqualTo(provisional)
    }

    // ── The stored shape, which the KDoc makes a claim about ───────────────────────────────

    @Test
    fun `a day rung is stored as a date and an instant rung as a local date-time`() {
        val allDay = Task(id = "t", title = "x", occurrence = AllDay(day)).toDto()
        assertThat(allDay.occurrenceStart).isEqualTo("2026-08-22")
        // No end: an all-day's end is implied by its start, and a stored one would be a second
        // value with nothing to keep it true.
        assertThat(allDay.occurrenceEnd).isNull()
        assertThat(allDay.occurrencePlacement).isNull()

        val deadline = Task(id = "t", title = "x", occurrence = Deadline(day.atTime(6, 0))).toDto()
        assertThat(deadline.occurrenceStart).isEqualTo("2026-08-22T06:00")

        // Zone-free by construction: nothing here is an instant, so "which day" cannot move
        // when the document is read on a device in another zone.
        assertThat(allDay.occurrenceStart).doesNotContain("Z")
        assertThat(deadline.occurrenceStart).doesNotContain("Z")
    }

    @Test
    fun `placement is written for a block and for nothing else`() {
        assertThat(Task(id = "t", title = "x", occurrence = Block(day.atTime(9, 0), day.atTime(10, 0)))
            .toDto().occurrencePlacement).isEqualTo("CONFIRMED")
        assertThat(Task(id = "t", title = "x", occurrence = Span(day, day)).toDto().occurrencePlacement)
            .isNull()
    }

    // ── Absence, and documents written before #56 ──────────────────────────────────────────

    @Test
    fun `a document written before this ticket reads as no occurrence, with no backfill`() {
        val legacy = TaskDto(id = "t1", title = "Old task", createdAt = 1L)

        assertThat(legacy.occurrenceRung).isNull()
        assertThat(legacy.toDomain().occurrence).isNull()
    }

    @Test
    fun `a task whose occurrence is removed writes four nulls over the old ones`() {
        val scheduled = Task(id = "t1", title = "x", occurrence = Deadline(day.atTime(6, 0)))
        val unscheduled = scheduled.copy(occurrence = null).toDto()

        // `upsertTask` is a whole-document set(), so a retained start beside an absent rung
        // would be §0.3's second answer that quietly disagrees -- and `occurrence()` reads that
        // pair as no occurrence, so the task would be silently un-schedulable.
        assertThat(unscheduled.occurrenceRung).isNull()
        assertThat(unscheduled.occurrenceStart).isNull()
        assertThat(unscheduled.occurrenceEnd).isNull()
        assertThat(unscheduled.occurrencePlacement).isNull()
    }

    // ── Malformed documents: nothing guesses, nothing throws ───────────────────────────────

    @Test
    fun `an unrecognised rung reads as no occurrence rather than as an invented one`() {
        val future = TaskDto(id = "t", title = "x", occurrenceRung = "FORTNIGHTLY", occurrenceStart = "2026-08-22")

        // A rung this build does not know is a rung whose miss semantics it cannot honour.
        assertThat(future.toDomain().occurrence).isNull()
    }

    @Test
    fun `an unparseable start reads as no occurrence rather than throwing`() {
        val corrupt = TaskDto(id = "t", title = "x", occurrenceRung = "DEADLINE", occurrenceStart = "tomorrow-ish")

        // A DateTimeParseException between a snapshot and a frame would take the whole task
        // list down over one bad string.
        assertThat(corrupt.toDomain().occurrence).isNull()
    }

    @Test
    fun `a rung whose start is a date where an instant is required reads as no occurrence`() {
        val wrongShape = TaskDto(id = "t", title = "x", occurrenceRung = "DEADLINE", occurrenceStart = "2026-08-22")

        assertThat(wrongShape.toDomain().occurrence).isNull()
    }

    @Test
    fun `a block or span with no end reads as no occurrence, because half a window is not a window`() {
        val halfBlock = TaskDto(
            id = "t",
            title = "x",
            occurrenceRung = "BLOCK",
            occurrenceStart = "2026-08-22T09:00",
        )
        val halfSpan = TaskDto(
            id = "t",
            title = "x",
            occurrenceRung = "SPAN",
            occurrenceStart = "2026-08-22",
        )

        assertThat(halfBlock.toDomain().occurrence).isNull()
        assertThat(halfSpan.toDomain().occurrence).isNull()
    }

    @Test
    fun `an absent placement reads as confirmed, which is what everything typed today is`() {
        val noPlacement = TaskDto(
            id = "t",
            title = "x",
            occurrenceRung = "BLOCK",
            occurrenceStart = "2026-08-22T09:00",
            occurrenceEnd = "2026-08-22T10:00",
        )

        assertThat((noPlacement.toDomain().occurrence as Block).placement)
            .isEqualTo(BlockPlacement.CONFIRMED)
    }

    // ── The rest of the task is untouched ──────────────────────────────────────────────────

    @Test
    fun `adding an occurrence changes nothing else about how a task round-trips`() {
        val base = Task(id = "t1", title = "Write the report", estimatedMinutes = 240)

        val withOccurrence = base.copy(occurrence = Deadline(day.atTime(6, 0)))
        val backWithout = base.toDto().toDomain()
        val backWith = withOccurrence.toDto().toDomain()

        assertThat(backWith.copy(occurrence = null)).isEqualTo(backWithout)
    }
}
