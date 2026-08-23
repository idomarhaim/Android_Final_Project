package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.Span
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §4.3: ***"`OVERDUE` and `AWAY` are both carried forward from other days"*** — the one clause the
 * prototype's own checks caught as a **bug** rather than a preference
 * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * Rev 4, verbatim: *"B carried `OVERDUE` items forward from other days but not `AWAY` ones, so an
 * event that vanished from Thursday's calendar would surface only when Thursday arrived — exactly
 * too late to put it back."*
 *
 * ### Both branches are tested although only one can occur today
 *
 * `AWAY` needs [#61](https://github.com/idomarhaim/Android_Final_Project/issues/61)'s Google sync
 * to know an occurrence left the calendar, and `#61` has not shipped — see
 * [CalendarEntry.isAway] for why it is a parameter rather than a derivation. So the *source* is
 * empty and the *rule* is not: an untested `AWAY` branch is exactly the state the prototype was in
 * when the defect was found, and the whole point of the finding is that nobody noticed until the
 * two states were written out side by side.
 */
class CarryForwardTest {

    private val thursday: LocalDate = LocalDate.of(2026, 8, 20)
    private val today: LocalDate = thursday.plusDays(2)
    private val now: LocalDateTime = today.atTime(10, 0)

    private fun entry(
        occurrence: Occurrence,
        key: String = "k",
        isAway: Boolean = false,
        outcome: OccurrenceOutcome = OccurrenceOutcome.Planned,
    ) = CalendarEntry(
        key = key,
        title = key,
        kind = EntryKind.TASK,
        occurrence = occurrence,
        isAway = isAway,
        outcome = outcome,
    )

    // ── The predicate, state by state ─────────────────────────────────────────────────────

    @Test
    fun `an overdue deadline is carried forward`() {
        assertThat(CarryForward.carries(OccurrenceState.OVERDUE, isAway = false)).isTrue()
    }

    @Test
    fun `an away occurrence is carried forward whatever its state`() {
        // The rev-4 bug, in one assertion. AWAY is not a temporal state -- it is a fact about the
        // Google calendar -- so it has to carry on its own, and an implementation that keyed only
        // on OccurrenceState would pass every other test in this file.
        OccurrenceState.entries.forEach { state ->
            assertWithMessage("$state")
                .that(CarryForward.carries(state, isAway = true))
                .isTrue()
        }
    }

    @Test
    fun `nothing else is carried forward`() {
        // The negative half, and it is the half that matters: a rule that carries every past state
        // turns today into a backlog of everything that ever lapsed, which is the permanent nag
        // 4.2 says the app must not become. 2.3 is explicit about each exclusion --
        // MISSED is history, EXPIRED "counts for nothing, silently".
        val carried = OccurrenceState.entries.filter { CarryForward.carries(it, isAway = false) }

        assertThat(carried).containsExactly(OccurrenceState.OVERDUE)
    }

    @Test
    fun `a missed block is not carried forward`() {
        // 2.3: MISSED is a failure whose slot is gone, and "a missed occurrence is never edited --
        // it is history". Rescheduling creates a NEW occurrence; it does not drag the old one here.
        assertThat(CarryForward.carries(OccurrenceState.MISSED, isAway = false)).isFalse()
    }

    @Test
    fun `an expired provisional block is not carried forward`() {
        // 2.3: "counts for nothing, silently" -- without which "an over-eager agent manufactures
        // failures". Carrying it forward would put that manufactured failure on today's column.
        assertThat(CarryForward.carries(OccurrenceState.EXPIRED, isAway = false)).isFalse()
    }

    // ── The sweep: which entries land on today, and where ─────────────────────────────────

    @Test
    fun `an overdue deadline from another day lands on today`() {
        val overdue = entry(Deadline(thursday.atTime(18, 0)), key = "overdue")

        val carried = CarryForward.onto(today, listOf(overdue), now)

        assertThat(carried.map { it.key }).containsExactly("overdue")
        assertThat(carried.single().carriedForward).isTrue()
    }

    @Test
    fun `a carried entry keeps its own date and is marked carried`() {
        // The date is NOT rewritten to today. It is still the day the work was due, which is what
        // the row has to say -- "overdue since Thursday" is the useful sentence, and rewriting the
        // date would make it "due today", which is a different and false claim.
        val overdue = entry(Deadline(thursday.atTime(18, 0)))

        val carried = CarryForward.onto(today, listOf(overdue), now).single()

        assertThat(carried.date).isEqualTo(thursday)
        assertThat(carried.carriedForward).isTrue()
    }

    @Test
    fun `a carried entry is drawn as a banner, never in the grid`() {
        // Even when the thing carried is a BLOCK: it is on today's column because its own slot is
        // gone, so drawing it at an hour of today would place it at a time that means nothing --
        // and DayLoad would then count hours nobody scheduled.
        val awayBlock = entry(
            Block(thursday.atTime(9, 0), thursday.atTime(11, 0), BlockPlacement.CONFIRMED),
            isAway = true,
        )

        val carried = CarryForward.onto(today, listOf(awayBlock), now).single()

        assertThat(carried.lane).isEqualTo(CalendarLane.ALL_DAY)
    }

    @Test
    fun `an entry already on today is not carried onto today`() {
        // Otherwise it appears twice on one column -- once in its own lane and once as a demand.
        val overdueToday = entry(Deadline(today.atTime(8, 0)), key = "this morning")

        val carried = CarryForward.onto(today, listOf(overdueToday), now)

        assertThat(carried).isEmpty()
    }

    @Test
    fun `a settled window is never carried forward`() {
        // Done or deliberately skipped: 2.1's skip is "a decision", and counting it against the
        // user is the same defect as manufacturing a failure, from the other direction.
        val done = entry(Deadline(thursday.atTime(18, 0)), outcome = OccurrenceOutcome.Done(1L))
        val skipped = entry(Deadline(thursday.atTime(18, 0)), key = "s", outcome = OccurrenceOutcome.Skipped(1L))

        assertThat(CarryForward.onto(today, listOf(done, skipped), now)).isEmpty()
    }

    @Test
    fun `a future deadline is not carried forward`() {
        val future = entry(Deadline(today.plusDays(3).atTime(9, 0)))

        assertThat(CarryForward.onto(today, listOf(future), now)).isEmpty()
    }

    @Test
    fun `a lapsed all-day is not carried forward`() {
        // DAY_PASSED is not OVERDUE. 2.2 gives them different meanings on purpose, and 2.3 marks
        // only the deadline as "the one state that keeps reminding".
        val lapsed = entry(AllDay(thursday))

        assertThat(CarryForward.onto(today, listOf(lapsed), now)).isEmpty()
    }

    @Test
    fun `a closed span is not carried forward`() {
        val closed = entry(Span(thursday.minusDays(4), thursday))

        assertThat(CarryForward.onto(today, listOf(closed), now)).isEmpty()
    }

    @Test
    fun `an away span from another day is carried even though a closed one is not`() {
        // The pair that shows AWAY is orthogonal to the temporal state rather than a synonym for
        // some subset of it.
        val closed = entry(Span(thursday.minusDays(4), thursday), key = "closed")
        val away = entry(Span(thursday.minusDays(4), thursday), key = "away", isAway = true)

        val carried = CarryForward.onto(today, listOf(closed, away), now)

        assertThat(carried.map { it.key }).containsExactly("away")
    }
}
