package com.idomarhaim.goalpilot.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Half-open time window `[startMillis, endMillisExclusive)`.
 *
 * Exclusive on the right so a task completed at exactly midnight belongs to
 * exactly one bucket — the alternative double-counts it in both the day that
 * ended and the day that began.
 */
data class TimeWindow(val startMillis: Long, val endMillisExclusive: Long) {
    fun contains(epochMillis: Long): Boolean =
        epochMillis >= startMillis && epochMillis < endMillisExclusive
}

/**
 * One column of the trend chart — a sub-window of the selected
 * [AnalyticsRange], with the label that goes under it.
 *
 * Buckets **tile** their range: contiguous, non-overlapping, and covering it
 * exactly. That is what lets the trend's totals be checked against the pie's,
 * and it inherits the half-open convention from [TimeWindow], so a task
 * completed at midnight belongs to exactly one column.
 */
data class TimeBucket(val label: String, val window: TimeWindow)

/**
 * The zoom levels of the analytics screen (day / week / month / quarter / year).
 *
 * **Calendar-aligned, not rolling.** [SummaryPeriod] windows are rolling — "the
 * last 7 days" — because a shareable summary is about recent momentum. A question
 * like *"what share of my life went into Health this month?"* is about a calendar
 * month; a rolling window would answer a question nobody asked and would keep
 * moving under the user between two glances at the same screen.
 *
 * Everything is computed from an injected [LocalDate] and [ZoneId] so the whole
 * thing is testable on the JVM without touching the system clock.
 */
enum class AnalyticsRange(val label: String, val bucketNoun: String) {
    DAY("Day", "4 hours"),
    WEEK("Week", "day"),
    MONTH("Month", "week"),
    QUARTER("Quarter", "week"),
    YEAR("Year", "month");

    /** First day of the calendar period [today] falls in. */
    fun startDate(today: LocalDate, firstDayOfWeek: DayOfWeek = defaultFirstDayOfWeek()): LocalDate =
        when (this) {
            DAY -> today
            WEEK -> today.minusDays(daysSince(today.dayOfWeek, firstDayOfWeek).toLong())
            MONTH -> today.withDayOfMonth(1)
            QUARTER -> today.withMonth(firstMonthOfQuarter(today.monthValue)).withDayOfMonth(1)
            YEAR -> today.withDayOfYear(1)
        }

    /** First day *after* the period — the exclusive end. */
    fun endDateExclusive(
        today: LocalDate,
        firstDayOfWeek: DayOfWeek = defaultFirstDayOfWeek(),
    ): LocalDate = when (this) {
        DAY -> startDate(today, firstDayOfWeek).plusDays(1)
        WEEK -> startDate(today, firstDayOfWeek).plusWeeks(1)
        MONTH -> startDate(today, firstDayOfWeek).plusMonths(1)
        QUARTER -> startDate(today, firstDayOfWeek).plusMonths(3)
        YEAR -> startDate(today, firstDayOfWeek).plusYears(1)
    }

    fun window(
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = defaultFirstDayOfWeek(),
    ): TimeWindow = TimeWindow(
        startMillis = startDate(today, firstDayOfWeek).atStartOfDay(zone).toInstant().toEpochMilli(),
        endMillisExclusive = endDateExclusive(today, firstDayOfWeek)
            .atStartOfDay(zone).toInstant().toEpochMilli(),
    )

    /**
     * The range split into the columns of the trend chart, in order.
     *
     * The unit is one step finer than the range itself — days in a week, weeks
     * in a month or quarter, months in a year — so every range answers *"is this
     * area growing or shrinking?"* at a resolution somebody can actually read.
     * A day is the exception: it has no finer calendar unit, so it is cut into
     * six four-hour blocks, which is also the only view that shows *when* in the
     * day the time went.
     *
     * Boundaries are derived from [startDate]/[endDateExclusive] rather than
     * re-derived here, so the buckets can never disagree with the pie they sit
     * under. Week-aligned buckets are clipped at both ends: a month almost never
     * begins on the first day of a week, and a partial first column is honest
     * where a column reaching back into the previous month would not be.
     */
    fun buckets(
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = defaultFirstDayOfWeek(),
    ): List<TimeBucket> {
        val start = startDate(today, firstDayOfWeek)
        val end = endDateExclusive(today, firstDayOfWeek)
        return when (this) {
            DAY -> hourBlocks(start, end, zone)
            WEEK -> dateBuckets(datesEvery(start, end) { it.plusDays(1) }, zone, dayName::format)
            MONTH -> dateBuckets(weekStarts(start, end, firstDayOfWeek), zone) {
                it.dayOfMonth.toString()
            }
            // `d/M` is §4.8's defect in miniature: two digit runs either side of
            // a neutral slash, so 3/8 renders as 8/3 in an RTL paragraph — a
            // wrong date that looks like a valid one.
            QUARTER -> dateBuckets(weekStarts(start, end, firstDayOfWeek), zone) {
                dayAndMonth.format(it).bidiIsolated()
            }
            YEAR -> dateBuckets(datesEvery(start, end) { it.plusMonths(1) }, zone, shortMonth::format)
        }
    }

    /**
     * Human label for the window itself, e.g. "Aug 3, 2026", "Q3 2026", "2026".
     *
     * **Direction-isolated, per spec §4.8**, and the `WEEK` branch is the exact
     * defect §4.8 names: `Aug 3 – Aug 9` is a Latin-digit *range* dropped into
     * whatever paragraph renders it, and the Unicode bidi algorithm resolves a
     * neutral run's direction from that paragraph — so in Hebrew it renders as
     * `Aug 9 – Aug 3`, silently reporting the wrong week. Every other branch is
     * a date or number in the same position, so the isolate is applied to the
     * whole result rather than to the one branch that happens to have been
     * noticed; `Bidi.isolate` is idempotent, so this cannot double-wrap.
     */
    fun windowLabel(
        today: LocalDate = LocalDate.now(),
        firstDayOfWeek: DayOfWeek = defaultFirstDayOfWeek(),
    ): String {
        val start = startDate(today, firstDayOfWeek)
        return when (this) {
            DAY -> fullDay.format(start)
            WEEK -> {
                val end = endDateExclusive(today, firstDayOfWeek).minusDays(1)
                "${shortDay.format(start)} – ${shortDay.format(end)}"
            }
            MONTH -> monthYear.format(start)
            QUARTER -> "Q${quarterOf(start.monthValue)} ${start.year}"
            YEAR -> start.year.toString()
        }.bidiIsolated()
    }

    companion object {
        /**
         * The week's first day for the device's locale — Sunday in Israel, Monday
         * across most of Europe. Hard-coding Monday would put a Sunday workout in
         * "last week" for this app's actual user.
         */
        fun defaultFirstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
            WeekFields.of(locale).firstDayOfWeek

        fun quarterOf(monthValue: Int): Int = (monthValue - 1) / 3 + 1

        private fun firstMonthOfQuarter(monthValue: Int): Int = (quarterOf(monthValue) - 1) * 3 + 1

        /** Days elapsed since [first] as of [day], always 0..6. */
        private fun daysSince(day: DayOfWeek, first: DayOfWeek): Int =
            (day.value - first.value + 7) % 7

        /** How many blocks a [AnalyticsRange.DAY] is cut into, and how long each is. */
        private const val DAY_BLOCKS = 6
        private const val DAY_BLOCK_HOURS = 4

        /**
         * Boundary dates walking [start] to [endExclusive] by [next]. n boundaries
         * describe n-1 buckets, and the last step is clipped rather than allowed to
         * run past the end — which is what keeps a partial final week inside its
         * own month.
         */
        private fun datesEvery(
            start: LocalDate,
            endExclusive: LocalDate,
            next: (LocalDate) -> LocalDate,
        ): List<LocalDate> {
            val dates = mutableListOf<LocalDate>()
            var cursor = start
            while (cursor.isBefore(endExclusive)) {
                dates += cursor
                cursor = next(cursor)
            }
            return dates + endExclusive
        }

        /**
         * Week boundaries inside `[start, endExclusive)`, aligned to
         * [firstDayOfWeek]. The first column is whatever is left of the week the
         * range begins in — a month starting on a Thursday opens with a 3-day
         * column, not with one reaching back into the month before.
         */
        private fun weekStarts(
            start: LocalDate,
            endExclusive: LocalDate,
            firstDayOfWeek: DayOfWeek,
        ): List<LocalDate> {
            val dates = mutableListOf(start)
            var cursor = start.plusDays((7 - daysSince(start.dayOfWeek, firstDayOfWeek)).toLong())
            while (cursor.isBefore(endExclusive)) {
                dates += cursor
                cursor = cursor.plusWeeks(1)
            }
            return dates + endExclusive
        }

        private fun dateBuckets(
            boundaries: List<LocalDate>,
            zone: ZoneId,
            label: (LocalDate) -> String,
        ): List<TimeBucket> = boundaries.zipWithNext { from, to ->
            TimeBucket(
                label = label(from),
                window = TimeWindow(
                    startMillis = from.atStartOfDay(zone).toInstant().toEpochMilli(),
                    endMillisExclusive = to.atStartOfDay(zone).toInstant().toEpochMilli(),
                ),
            )
        }

        /**
         * A single day as [DAY_BLOCKS] blocks of [DAY_BLOCK_HOURS] hours.
         *
         * Built by adding hours to a `ZonedDateTime` and then clamping to the day's
         * real end, because a DST day is 23 or 25 hours long: without the clamp the
         * last block of a short day would run past midnight into the next one, and
         * a long day would lose its final hour entirely.
         */
        private fun hourBlocks(
            start: LocalDate,
            endExclusive: LocalDate,
            zone: ZoneId,
        ): List<TimeBucket> {
            val dayStart = start.atStartOfDay(zone)
            val dayEnd = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
            return (0 until DAY_BLOCKS).mapNotNull { index ->
                val from = dayStart.plusHours((index * DAY_BLOCK_HOURS).toLong())
                val fromMillis = from.toInstant().toEpochMilli()
                if (fromMillis >= dayEnd) return@mapNotNull null
                val isLast = index == DAY_BLOCKS - 1
                val toMillis = if (isLast) {
                    dayEnd
                } else {
                    from.plusHours(DAY_BLOCK_HOURS.toLong()).toInstant().toEpochMilli()
                        .coerceAtMost(dayEnd)
                }
                TimeBucket(
                    label = blockHour.format(from),
                    window = TimeWindow(fromMillis, toMillis),
                )
            }
        }

        // `get()`, never `val` — see AppDateFormatters. A `val` here resolves
        // Locale.getDefault() once at class-init and is precisely §5.1's
        // "process-scoped vals no switch can move".
        private val fullDay get() = AppDateFormatters.of("MMM d, yyyy")
        private val shortDay get() = AppDateFormatters.of("MMM d")
        private val monthYear get() = AppDateFormatters.of("MMMM yyyy")
        private val dayName get() = AppDateFormatters.of("EEE")
        private val shortMonth get() = AppDateFormatters.of("MMM")
        private val dayAndMonth get() = AppDateFormatters.of("d/M")
        private val blockHour get() = AppDateFormatters.of("HH")
    }
}
