package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §2.1's recurrence rule, asserted where the value is — **the expansion**
 * ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * Every branch here is a pure function of a rule, an anchor and a bound, so all of it runs on
 * the JVM with explicit dates rather than on a device at a wall-clock time — the same argument
 * `OccurrenceTest` makes for §2.3's derivation.
 *
 * The two cases worth reading first are the ones a calendar library gets wrong by default:
 * **a month with no 31st is skipped, not clamped**, and **the fortnight is measured from the
 * anchor, not from Monday**. Both are decisions [RepeatRule] states and both are silent when
 * wrong — a series that quietly moved to the 28th looks like a series.
 */
class RepeatRuleTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)

    private fun RepeatRule.take(n: Int, anchor: LocalDate = monday): List<LocalDate> =
        datesFrom(anchor).take(n).toList()

    // ── The four units ────────────────────────────────────────────────────────────────────

    @Test
    fun `a daily rule steps by its interval and includes the anchor`() {
        val every3 = RepeatRule(unit = RepeatUnit.DAY, interval = 3)

        assertThat(every3.take(4)).containsExactly(
            monday,
            monday.plusDays(3),
            monday.plusDays(6),
            monday.plusDays(9),
        ).inOrder()
    }

    @Test
    fun `a weekly rule with no weekdays repeats on the anchor's own day`() {
        val fortnightly = RepeatRule(unit = RepeatUnit.WEEK, interval = 2)

        assertThat(fortnightly.take(3)).containsExactly(
            monday,
            monday.plusWeeks(2),
            monday.plusWeeks(4),
        ).inOrder()
        assertThat(fortnightly.take(3).map { it.dayOfWeek }).containsExactly(
            DayOfWeek.MONDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY,
        )
    }

    @Test
    fun `R18's fortnightly flowers are a rule and not 26 documents a year`() {
        // §2.1's own example, and the sentence this whole shape exists to make false:
        // "R18's flowers become 26 duplicate documents a year".
        val fortnightly = RepeatRule(unit = RepeatUnit.WEEK, interval = 2)

        val aYear = fortnightly.datesUpTo(monday, monday.plusYears(1))

        assertThat(aYear).hasSize(27)
        assertThat(aYear.first()).isEqualTo(monday)
        // Nothing is stored for any of them: this list came from four fields on the task.
        assertThat(aYear.all { it.dayOfWeek == DayOfWeek.MONDAY }).isTrue()
    }

    @Test
    fun `a monthly rule keeps the anchor's day of month`() {
        val monthly = RepeatRule(unit = RepeatUnit.MONTH)

        assertThat(monthly.take(3, LocalDate.of(2026, 1, 15))).containsExactly(
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 2, 15),
            LocalDate.of(2026, 3, 15),
        ).inOrder()
    }

    @Test
    fun `a yearly rule keeps the month and the day`() {
        val yearly = RepeatRule(unit = RepeatUnit.YEAR)

        assertThat(yearly.take(3, LocalDate.of(2026, 3, 1))).containsExactly(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2028, 3, 1),
        ).inOrder()
    }

    // ── The two silent-when-wrong branches ────────────────────────────────────────────────

    @Test
    fun `a monthly rule anchored on the 31st skips the months that have none`() {
        // `LocalDate.plusMonths` would clamp 31 January to 28 February, moving a commitment to
        // a day nobody chose -- and then moving it back in March, so the series drifts.
        val monthly = RepeatRule(unit = RepeatUnit.MONTH)

        val dates = monthly.take(5, LocalDate.of(2026, 1, 31))

        assertThat(dates).containsExactly(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 5, 31),
            LocalDate.of(2026, 7, 31),
            LocalDate.of(2026, 8, 31),
        ).inOrder()
        assertThat(dates.map { it.dayOfMonth }.distinct()).containsExactly(31)
    }

    @Test
    fun `a yearly rule anchored on 29 February lands only in leap years`() {
        val yearly = RepeatRule(unit = RepeatUnit.YEAR)

        assertThat(yearly.take(3, LocalDate.of(2024, 2, 29))).containsExactly(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2028, 2, 29),
            LocalDate.of(2032, 2, 29),
        ).inOrder()
    }

    @Test
    fun `the fortnight is measured from the anchor, not from Monday and not from the locale`() {
        // "Every other week on Monday and Thursday", anchored on a TUESDAY. Week 0 is
        // [Tue 18 .. Mon 24], so its Thursday is the 20th and its Monday is the 24th -- and the
        // next pair is a fortnight later. An ISO-week reading would put the boundary on the
        // 24th and produce a different series; a locale-week reading would move when the user
        // changes region, which is the failure that cannot be reproduced on the machine that
        // reports it.
        val tuesday = LocalDate.of(2026, 8, 18)
        val rule = RepeatRule(
            unit = RepeatUnit.WEEK,
            interval = 2,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        )

        assertThat(rule.take(4, tuesday)).containsExactly(
            LocalDate.of(2026, 8, 20),
            LocalDate.of(2026, 8, 24),
            LocalDate.of(2026, 9, 3),
            LocalDate.of(2026, 9, 7),
        ).inOrder()
    }

    @Test
    fun `a weekly rule never emits a day earlier in the anchor's own week`() {
        // Anchored on Wednesday, wanting Mondays: the Monday two days BEFORE the anchor is not
        // part of the series, or the rule would start before the task's own when.
        val wednesday = LocalDate.of(2026, 8, 19)
        val rule = RepeatRule(unit = RepeatUnit.WEEK, weekdays = setOf(DayOfWeek.MONDAY))

        assertThat(rule.take(2, wednesday)).containsExactly(
            LocalDate.of(2026, 8, 24),
            LocalDate.of(2026, 8, 31),
        ).inOrder()
    }

    // ── RepeatEnd — exactly one bound ─────────────────────────────────────────────────────

    @Test
    fun `Never is unbounded and every other end is not`() {
        assertThat(RepeatRule(RepeatUnit.DAY).isUnbounded).isTrue()
        assertThat(RepeatRule(RepeatUnit.DAY, end = RepeatEnd.AfterCount(3)).isUnbounded).isFalse()
        assertThat(
            RepeatRule(RepeatUnit.DAY, end = RepeatEnd.OnDate(monday)).isUnbounded,
        ).isFalse()
    }

    @Test
    fun `AfterCount counts instances and includes the first`() {
        val rule = RepeatRule(unit = RepeatUnit.DAY, end = RepeatEnd.AfterCount(3))

        // `take(10)` on a bounded rule returns the bound, not ten -- the sequence ends.
        assertThat(rule.take(10)).containsExactly(
            monday, monday.plusDays(1), monday.plusDays(2),
        ).inOrder()
    }

    @Test
    fun `OnDate is inclusive of its last day`() {
        val rule = RepeatRule(
            unit = RepeatUnit.DAY,
            end = RepeatEnd.OnDate(monday.plusDays(2)),
        )

        assertThat(rule.take(10)).hasSize(3)
        assertThat(rule.take(10).last()).isEqualTo(monday.plusDays(2))
    }

    @Test
    fun `a bound already spent produces an empty series rather than one instance`() {
        // Reachable from an edit: `ScheduleEdits` spends an `AfterCount` down as it materialises
        // the past, and a series ended before its own anchor is what a `THIS_AND_FUTURE` skip
        // on the first instance would leave behind if it kept the rule.
        assertThat(RepeatRule(RepeatUnit.DAY, end = RepeatEnd.AfterCount(0)).take(5)).isEmpty()
        assertThat(
            RepeatRule(RepeatUnit.DAY, end = RepeatEnd.OnDate(monday.minusDays(1))).take(5),
        ).isEmpty()
    }

    @Test
    fun `an interval below one is coerced rather than looping forever`() {
        // A stored 0 would make the sequence emit the anchor for ever, inside a lazy generator
        // -- the one failure here that is neither loud nor recoverable.
        val rule = RepeatRule(unit = RepeatUnit.DAY, interval = 0)

        assertThat(rule.step).isEqualTo(1)
        assertThat(rule.take(3)).containsExactly(
            monday, monday.plusDays(1), monday.plusDays(2),
        ).inOrder()
    }

    // ── Moving an occurrence to another day preserves what the rung means ─────────────────

    @Test
    fun `an all-day moves as a day`() {
        assertThat(AllDay(monday).onDate(monday.plusDays(5))).isEqualTo(AllDay(monday.plusDays(5)))
    }

    @Test
    fun `a deadline keeps its hour`() {
        val moved = Deadline(monday.atTime(18, 30)).onDate(monday.plusWeeks(1))

        assertThat(moved).isEqualTo(Deadline(monday.plusWeeks(1).atTime(18, 30)))
    }

    @Test
    fun `a block keeps its start time and its duration`() {
        val block = Block(monday.atTime(9, 0), monday.atTime(10, 30))

        val moved = block.onDate(monday.plusDays(2)) as Block

        assertThat(moved.start).isEqualTo(monday.plusDays(2).atTime(9, 0))
        assertThat(moved.end).isEqualTo(monday.plusDays(2).atTime(10, 30))
    }

    @Test
    fun `a block that crosses midnight keeps its length rather than its end date`() {
        val overnight = Block(monday.atTime(23, 0), monday.plusDays(1).atTime(1, 0))

        val moved = overnight.onDate(monday.plusWeeks(1)) as Block

        assertThat(moved.start).isEqualTo(monday.plusWeeks(1).atTime(23, 0))
        assertThat(moved.end).isEqualTo(monday.plusWeeks(1).plusDays(1).atTime(1, 0))
    }

    @Test
    fun `a span keeps its length in days`() {
        val moved = Span(monday, monday.plusDays(2)).onDate(monday.plusWeeks(1)) as Span

        assertThat(moved.from).isEqualTo(monday.plusWeeks(1))
        assertThat(moved.to).isEqualTo(monday.plusWeeks(1).plusDays(2))
    }

    @Test
    fun `an inverted span moves as the zero-length span it already reads as`() {
        // `Span.closesAt` coerces an inverted stored pair rather than throwing, and moving one
        // must not undo that by producing a negative length.
        val moved = Span(from = monday, to = monday.minusDays(3)).onDate(monday.plusDays(1)) as Span

        assertThat(moved.from).isEqualTo(monday.plusDays(1))
        assertThat(moved.to).isEqualTo(monday.plusDays(1))
    }

    @Test
    fun `startDate is the day the window opens, whatever the rung`() {
        assertThat(AllDay(monday).startDate).isEqualTo(monday)
        assertThat(Deadline(monday.atTime(23, 59)).startDate).isEqualTo(monday)
        assertThat(Block(monday.atTime(9, 0), monday.atTime(10, 0)).startDate).isEqualTo(monday)
        assertThat(Span(monday, monday.plusDays(4)).startDate).isEqualTo(monday)
    }

    @Test
    fun `a generated instance carries the rung's miss semantics unchanged`() {
        // The point of moving a template rather than storing a date: every instance is still a
        // BLOCK, so a lapsed one is still §2.2's "the slot is gone" and still a failure.
        val block = Block(monday.atTime(9, 0), monday.atTime(10, 0))
        val next = block.onDate(monday.plusWeeks(1))

        assertThat(next.rung).isEqualTo(OccurrenceRung.BLOCK)
        assertThat(next.missState).isEqualTo(OccurrenceState.MISSED)
        assertThat(next.stateAt(LocalDateTime.of(2026, 8, 25, 12, 0)))
            .isEqualTo(OccurrenceState.MISSED)
    }
}
