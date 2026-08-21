package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §2.2's rung table, asserted as the spec it is — **four rungs, four different meanings of a
 * miss** ([#56](https://github.com/idomarhaim/Android_Final_Project/issues/56)).
 *
 * The brief for `#56` names the trap exactly: *"four rungs, four different meanings of a miss,
 * and the one you skip is the one that breaks."* So every rung gets its own miss assertion
 * here, and each one asserts the **meaning** (which state, and what that state implies about
 * failure and further reminding), not merely that something changed once the date went by.
 *
 * §2.3's derivation is a pure function of an occurrence and a clock, so all of this runs on the
 * JVM with an explicit `now` rather than on a device at a wall-clock time.
 */
class OccurrenceTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val duringMonday: LocalDateTime = monday.atTime(12, 0)
    private val tuesdayMorning: LocalDateTime = monday.plusDays(1).atTime(9, 0)
    private val sundayEvening: LocalDateTime = monday.minusDays(1).atTime(20, 0)

    // ── §2.2 rung 1 · ALL_DAY — "a day with no slot"; a miss means the day passed ──────────

    @Test
    fun `an all-day is scheduled before its day, underway during it, and day-passed after`() {
        val occurrence = AllDay(monday)

        assertThat(occurrence.stateAt(sundayEvening)).isEqualTo(OccurrenceState.SCHEDULED)
        assertThat(occurrence.stateAt(duringMonday)).isEqualTo(OccurrenceState.UNDERWAY)
        assertThat(occurrence.stateAt(tuesdayMorning)).isEqualTo(OccurrenceState.DAY_PASSED)
    }

    @Test
    fun `an all-day is still underway at the last minute of its day and past at midnight`() {
        val occurrence = AllDay(monday)

        // The half-open window, at the one boundary where an off-by-one is invisible on a
        // device: 23:59 is still today, 00:00 is not. Getting this wrong makes every all-day
        // task lapse a day early or a day late, and neither is noticeable until it is.
        assertThat(occurrence.stateAt(monday.atTime(23, 59))).isEqualTo(OccurrenceState.UNDERWAY)
        assertThat(occurrence.stateAt(monday.plusDays(1).atStartOfDay()))
            .isEqualTo(OccurrenceState.DAY_PASSED)
    }

    @Test
    fun `a passed all-day is neither a failure nor a thing that keeps reminding`() {
        val state = AllDay(monday).stateAt(tuesdayMorning)

        // §2.3 marks exactly one state a failure and it is not this one; and only OVERDUE
        // keeps reminding. A day that went by meets the user once, in the daily review.
        assertThat(state.countsAsFailure).isFalse()
        assertThat(state.keepsReminding).isFalse()
        assertThat(state.meetsUserInDailyReview).isTrue()
    }

    // ── §2.2 rung 2 · DEADLINE — "a moment you owe something by"; late, still owed ─────────

    @Test
    fun `a deadline is overdue once its moment passes, and is never underway`() {
        val occurrence = Deadline(monday.atTime(18, 0))

        assertThat(occurrence.stateAt(monday.atTime(17, 59))).isEqualTo(OccurrenceState.SCHEDULED)
        // At the deadline itself it is already owed: the window is half-open and has zero
        // width, so `UNDERWAY` is unrepresentable for this rung rather than merely unused.
        assertThat(occurrence.stateAt(monday.atTime(18, 0))).isEqualTo(OccurrenceState.OVERDUE)
        assertThat(occurrence.stateAt(tuesdayMorning)).isEqualTo(OccurrenceState.OVERDUE)
    }

    @Test
    fun `overdue is not a failure and is the one state that keeps reminding`() {
        val state = Deadline(monday.atTime(18, 0)).stateAt(tuesdayMorning)

        // §2.3, quoted: "a passed deadline is late and still owed -> NOT a failure; the one
        // state that keeps reminding". Both halves, because the split between OVERDUE and
        // MISSED earns its keep only if both are true.
        assertThat(state.countsAsFailure).isFalse()
        assertThat(state.keepsReminding).isTrue()
    }

    // ── §2.2 rung 3 · BLOCK — "a span of time you are inside"; the slot is gone ────────────

    @Test
    fun `an endorsed block that lapses is missed, and missed is the one failure`() {
        val occurrence = Block(monday.atTime(9, 0), monday.atTime(10, 0))

        assertThat(occurrence.stateAt(monday.atTime(8, 59))).isEqualTo(OccurrenceState.SCHEDULED)
        assertThat(occurrence.stateAt(monday.atTime(9, 30))).isEqualTo(OccurrenceState.UNDERWAY)

        val missed = occurrence.stateAt(monday.atTime(10, 0))
        assertThat(missed).isEqualTo(OccurrenceState.MISSED)
        assertThat(missed.countsAsFailure).isTrue()
        // Its slot is gone, so there is nothing left to do at the time it was about.
        assertThat(missed.keepsReminding).isFalse()
        assertThat(missed.meetsUserInDailyReview).isTrue()
    }

    @Test
    fun `an unconfirmed block expires silently instead of manufacturing a failure`() {
        val provisional = Block(
            start = monday.atTime(9, 0),
            end = monday.atTime(10, 0),
            placement = BlockPlacement.PROVISIONAL,
        )

        val state = provisional.stateAt(tuesdayMorning)
        assertThat(state).isEqualTo(OccurrenceState.EXPIRED)
        // §2.3: "counts for nothing, silently" -- without which "an over-eager agent
        // manufactures failures". The silence is the assertion that matters: it must not reach
        // the daily review at all.
        assertThat(state.countsAsFailure).isFalse()
        assertThat(state.meetsUserInDailyReview).isFalse()
    }

    @Test
    fun `a silently-placed block is endorsed, because SILENT differs by visibility not confidence`() {
        val silent = Block(
            start = monday.atTime(9, 0),
            end = monday.atTime(10, 0),
            placement = BlockPlacement.SILENT,
        )

        // §2.3: "SILENT and PROVISIONAL sit on the same day on purpose -- they differ by
        // whether the app could SEE the slot, not by how confident it is." So a SILENT block
        // was confirmed, and its lapse is a real miss rather than an expiry.
        assertThat(silent.stateAt(tuesdayMorning)).isEqualTo(OccurrenceState.MISSED)
    }

    // ── §2.2 rung 4 · SPAN — "days, not hours"; the window closed ──────────────────────────

    @Test
    fun `a span is underway across its whole last day and closes the day after`() {
        val occurrence = Span(from = monday, to = monday.plusDays(2))

        assertThat(occurrence.stateAt(sundayEvening)).isEqualTo(OccurrenceState.SCHEDULED)
        assertThat(occurrence.stateAt(duringMonday)).isEqualTo(OccurrenceState.UNDERWAY)
        // The final day is INSIDE the window -- a three-day span that ends at midnight on its
        // third morning would give the user two days.
        assertThat(occurrence.stateAt(monday.plusDays(2).atTime(23, 59)))
            .isEqualTo(OccurrenceState.UNDERWAY)
        assertThat(occurrence.stateAt(monday.plusDays(3).atStartOfDay()))
            .isEqualTo(OccurrenceState.WINDOW_CLOSED)
    }

    @Test
    fun `a closed window is a miss the user meets once, and is not a failure`() {
        val state = Span(from = monday, to = monday).stateAt(tuesdayMorning)

        assertThat(state).isEqualTo(OccurrenceState.WINDOW_CLOSED)
        assertThat(state.countsAsFailure).isFalse()
        assertThat(state.keepsReminding).isFalse()
        assertThat(state.meetsUserInDailyReview).isTrue()
    }

    // ── The table as a whole ───────────────────────────────────────────────────────────────

    @Test
    fun `the four rungs produce four different miss meanings`() {
        val misses = listOf(
            AllDay(monday),
            Deadline(monday.atTime(18, 0)),
            Block(monday.atTime(9, 0), monday.atTime(10, 0)),
            Span(monday, monday),
        ).map { it.missState }

        // The whole reason §2.2 has four rungs rather than one nullable date. If a future
        // change collapses two of them, this fails before anything subtler does.
        assertThat(misses).containsNoDuplicates()
        assertThat(misses).containsExactly(
            OccurrenceState.DAY_PASSED,
            OccurrenceState.OVERDUE,
            OccurrenceState.MISSED,
            OccurrenceState.WINDOW_CLOSED,
        )
    }

    @Test
    fun `exactly one state counts as a failure and exactly one keeps reminding`() {
        // Stated as a property over the whole enum rather than per constant, so a fifth state
        // added later cannot quietly join either set.
        assertThat(OccurrenceState.entries.filter { it.countsAsFailure })
            .containsExactly(OccurrenceState.MISSED)
        assertThat(OccurrenceState.entries.filter { it.keepsReminding })
            .containsExactly(OccurrenceState.OVERDUE)
    }

    @Test
    fun `every miss state is past and neither live state is`() {
        OccurrenceState.entries.forEach { state ->
            val live = state == OccurrenceState.SCHEDULED || state == OccurrenceState.UNDERWAY
            assertThat(state.isPast).isEqualTo(!live)
        }
    }

    // ── Malformed input, which can only come off the wire ──────────────────────────────────

    @Test
    fun `an inverted block is zero-length rather than an exception`() {
        val inverted = Block(start = monday.atTime(10, 0), end = monday.atTime(9, 0))

        assertThat(inverted.closesAt).isEqualTo(inverted.opensAt)
        assertThat(inverted.stateAt(monday.atTime(9, 30))).isEqualTo(OccurrenceState.SCHEDULED)
        assertThat(inverted.stateAt(monday.atTime(10, 0))).isEqualTo(OccurrenceState.MISSED)
    }

    @Test
    fun `an inverted span lasts its first day rather than ending before it starts`() {
        val inverted = Span(from = monday.plusDays(2), to = monday)

        assertThat(inverted.stateAt(monday.plusDays(2).atTime(12, 0)))
            .isEqualTo(OccurrenceState.UNDERWAY)
        assertThat(inverted.stateAt(monday.plusDays(3).atStartOfDay()))
            .isEqualTo(OccurrenceState.WINDOW_CLOSED)
    }

    // ── OccurrenceDraft: which rung the add row produces ───────────────────────────────────

    @Test
    fun `a date alone is an all-day and a date with a time is a deadline`() {
        val dateOnly = OccurrenceDraft().withDate(monday)
        assertThat(dateOnly.toOccurrence()).isEqualTo(AllDay(monday))

        val withTime = dateOnly.withTime(java.time.LocalTime.of(6, 0))
        assertThat(withTime.toOccurrence()).isEqualTo(Deadline(monday.atTime(6, 0)))

        // The demotion is a change of RUNG, so it must change what a miss means, not just the
        // label: back to "the day passed" from "late, still owed".
        assertThat(withTime.withoutTime().toOccurrence()?.missState)
            .isEqualTo(OccurrenceState.DAY_PASSED)
    }

    @Test
    fun `a time with no date is nothing, and never today`() {
        val timeOnly = OccurrenceDraft().withTime(java.time.LocalTime.of(6, 0))

        // Inventing "today" here would silently make a task overdue this evening because
        // somebody tapped the wrong control.
        assertThat(timeOnly.toOccurrence()).isNull()
        assertThat(timeOnly.isSet).isFalse()
    }

    @Test
    fun `an empty draft and a cleared draft both mean no occurrence at all`() {
        assertThat(OccurrenceDraft().toOccurrence()).isNull()
        assertThat(
            OccurrenceDraft()
                .withDate(monday)
                .withTime(java.time.LocalTime.of(6, 0))
                .cleared()
                .toOccurrence(),
        ).isNull()
    }
}
