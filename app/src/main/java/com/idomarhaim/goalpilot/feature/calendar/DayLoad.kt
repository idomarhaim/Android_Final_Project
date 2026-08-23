package com.idomarhaim.goalpilot.feature.calendar

import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_DAY
import com.idomarhaim.goalpilot.domain.model.WakingHours
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §4.3's ***per-day load bar and booked/free ring, "arithmetic not inference"*** — `#60`.
 *
 * ### Why this costs nothing against §0.1's free-model rule
 *
 * Every number below is a function of block start/end times the calendar already has on screen and
 * of a preference the user already set. No model call, no network, no stored field to go stale —
 * the same property §2.3 gives the temporal states, for the same reason: two readers asking at the
 * same instant cannot disagree, because there is no second copy to disagree with.
 *
 * ### The three decisions this file makes, and where each is derived from
 *
 * 1. **Only [com.idomarhaim.goalpilot.domain.model.OccurrenceRung.BLOCK] books time.**
 *    [RungPresentation.booksTime], derived from §2.4 (*the other three "occupy no slot"*) and
 *    §2.2 (*"spans contribute nothing"*).
 * 2. **Booked minutes are a set **union**, not a sum.** Two blocks at the same hour are two
 *    commitments and *one* booked hour; summing them yields a day that reads 26 hours booked out
 *    of 16, which is a number no reading of *booked/free* makes true. Union is no less
 *    "arithmetic" than addition — it is the arithmetic that answers the question actually asked.
 * 3. **The numerator is clipped to the waking window, because the denominator already is.**
 *    §4.3 reddens *"past 75% of waking hours"*, so waking hours are the frame; a 03:00 block
 *    counted into a 07:00–23:00 denominator is §0.3's *second number that quietly disagrees*
 *    with the first — the two would be measuring different windows and their ratio would mean
 *    nothing. A block outside waking hours is still drawn in the grid; it just cannot consume
 *    waking time it never touched.
 */
data class DayLoad(
    /** Minutes of the waking day that blocks occupy — unioned, clipped, never double-counted. */
    val bookedMinutes: Int,
    /** The denominator: [WakingHours.lengthMinutes], which wraps past midnight and may be zero. */
    val wakingMinutes: Int,
    /** Where the bar reddens — [WakingHours.loadBarRedMinutes], §4.3's *75% of waking hours*. */
    val redAtMinutes: Int,
) {

    /**
     * What is left of the waking day. Floored at zero rather than allowed negative: [bookedMinutes]
     * is already clipped to the same window, so a negative here would be a bug, and a ring drawn
     * from a negative sweep fails silently rather than loudly.
     */
    val freeMinutes: Int get() = (wakingMinutes - bookedMinutes).coerceAtLeast(0)

    /**
     * Booked as a fraction of the waking day, **uncapped**.
     *
     * Zero when [wakingMinutes] is zero. [WakingHours.lengthMinutes]' own KDoc requires exactly
     * this — a start equal to an end *"reads as zero, not twenty-four hours"*, and *"a consumer
     * dividing by it should see zero rather than a silently plausible 1440"*. Dividing anyway is
     * how a `NaN` reaches a canvas and draws nothing with no error.
     */
    val fraction: Float
        get() = if (wakingMinutes <= 0) 0f else bookedMinutes.toFloat() / wakingMinutes

    /** [fraction] capped for drawing. A bar cannot be 130% long; the number below it still says so. */
    val barFraction: Float get() = fraction.coerceIn(0f, 1f)

    /**
     * §4.3's red threshold: ***past*** 75% of waking hours.
     *
     * Strictly past — a day booked to exactly the threshold has not passed it. The distinction is
     * worth keeping because [WakingHours.loadBarRedMinutes] is integer-truncated, so the boundary
     * is a value a real day lands on rather than a measure-zero edge.
     */
    val isOverloaded: Boolean get() = bookedMinutes > redAtMinutes

    /** Nothing booked at all — what an empty day reports, and what the ring draws as wholly free. */
    val isEmpty: Boolean get() = bookedMinutes == 0

    companion object {

        /**
         * The load of one day, from the entries drawn on it.
         *
         * Carried-forward entries are excluded by [CalendarEntry.lane]: they land in
         * [CalendarLane.ALL_DAY], and only [CalendarLane.GRID] entries reach the union below. That
         * is the right answer rather than a convenience — an overdue deadline carried onto today
         * books no hour of today, and letting it redden today's bar would report work as scheduled
         * that nobody has scheduled.
         */
        fun of(date: LocalDate, entries: List<CalendarEntry>, waking: WakingHours): DayLoad {
            val awake = wakingIntervals(waking)
            val booked = entries
                .filter { it.lane == CalendarLane.GRID }
                .filter { RungPresentation.booksTime(it.occurrence.rung) }
                .flatMap { clipToDay(date, it.occurrence.opensAt, it.occurrence.closesAt) }
                .let { union(it) }
                .flatMap { block -> awake.mapNotNull { block intersect it } }
                .let { union(it) }
                .sumOf { it.length }
            return DayLoad(
                bookedMinutes = booked,
                wakingMinutes = waking.lengthMinutes,
                redAtMinutes = waking.loadBarRedMinutes,
            )
        }

        /**
         * The waking span as minute ranges **inside one day**, which is one range or two.
         *
         * [WakingHours] may wrap past midnight (`22:00 – 06:00`) and its own KDoc says that is a
         * night-shift user rather than an edge case to forbid. Read onto a single calendar column
         * that span is two disjoint pieces — the tail of the previous night and the run up to
         * bedtime — and treating it as one `start..end` range would silently produce a *negative*
         * length. A start equal to an end is **empty**, matching [WakingHours.lengthMinutes]' zero.
         */
        internal fun wakingIntervals(waking: WakingHours): List<MinuteRange> {
            val start = Math.floorMod(waking.startMinutes, MINUTES_PER_DAY)
            val end = Math.floorMod(waking.endMinutes, MINUTES_PER_DAY)
            return when {
                start == end -> emptyList()
                start < end -> listOf(MinuteRange(start, end))
                else -> listOf(MinuteRange(0, end), MinuteRange(start, MINUTES_PER_DAY))
            }
        }

        /**
         * The part of `[from, to)` that falls on [date], as minutes since that date's midnight.
         *
         * A block may start the night before or run past midnight; only the part on this column can
         * consume this column's day. Returns empty for a window that misses the day entirely and for
         * a zero-length one — a zero-length block is legal ([com.idomarhaim.goalpilot.domain.model.Block.closesAt]
         * coerces an inverted pair to it) and books nothing.
         */
        internal fun clipToDay(date: LocalDate, from: LocalDateTime, to: LocalDateTime): List<MinuteRange> {
            val dayStart = date.atStartOfDay()
            val dayEnd = date.plusDays(1).atStartOfDay()
            if (!from.isBefore(dayEnd) || !to.isAfter(dayStart)) return emptyList()
            val lo = if (from.isBefore(dayStart)) 0 else minutesBetween(dayStart, from)
            val hi = if (to.isAfter(dayEnd)) MINUTES_PER_DAY else minutesBetween(dayStart, to)
            return if (hi > lo) listOf(MinuteRange(lo, hi)) else emptyList()
        }

        /**
         * The disjoint cover of [ranges] — the sweep that makes two overlapping blocks one booked
         * hour. Empty in, empty out; already-disjoint in, the same ranges out, merged where they
         * touch.
         */
        internal fun union(ranges: List<MinuteRange>): List<MinuteRange> {
            if (ranges.isEmpty()) return emptyList()
            val sorted = ranges.sortedBy { it.fromMinutes }
            val out = mutableListOf(sorted.first())
            for (next in sorted.drop(1)) {
                val last = out.last()
                if (next.fromMinutes <= last.toMinutes) {
                    if (next.toMinutes > last.toMinutes) out[out.lastIndex] = MinuteRange(last.fromMinutes, next.toMinutes)
                } else {
                    out += next
                }
            }
            return out
        }

        private fun minutesBetween(from: LocalDateTime, to: LocalDateTime): Int =
            java.time.Duration.between(from, to).toMinutes().toInt()
    }
}

/**
 * A half-open `[from, to)` run of minutes since midnight — the same convention
 * [com.idomarhaim.goalpilot.domain.model.Occurrence.closesAt] and `core/util/TimeWindow` use, so a
 * range and its successor never both contain the same minute and a union of two adjacent blocks is
 * their exact total.
 */
internal data class MinuteRange(val fromMinutes: Int, val toMinutes: Int) {

    val length: Int get() = (toMinutes - fromMinutes).coerceAtLeast(0)

    /** The overlap with [other], or `null` where they do not overlap or touch only at a boundary. */
    infix fun intersect(other: MinuteRange): MinuteRange? {
        val lo = maxOf(fromMinutes, other.fromMinutes)
        val hi = minOf(toMinutes, other.toMinutes)
        return if (hi > lo) MinuteRange(lo, hi) else null
    }
}
