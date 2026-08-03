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
enum class AnalyticsRange(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    QUARTER("Quarter"),
    YEAR("Year");

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

    /** Human label for the window itself, e.g. "Aug 3, 2026", "Q3 2026", "2026". */
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
        }
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

        private val fullDay = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        private val shortDay = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
}
