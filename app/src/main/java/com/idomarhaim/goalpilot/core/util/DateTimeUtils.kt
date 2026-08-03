package com.idomarhaim.goalpilot.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Small date/time helpers used across the UI and summary calculations. */
object DateTimeUtils {

    private val dayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun formatDay(epochMillis: Long): String =
        dayFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    /** Human friendly relative label, e.g. "just now", "3h ago", "2d ago". */
    fun relative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - epochMillis).coerceAtLeast(0)
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> formatDay(epochMillis)
        }
    }

    /** Inclusive start-of-day epoch millis for the given local date. */
    fun startOfDay(date: LocalDate = LocalDate.now()): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * "45m", "2h 15m", "18h" — the shape people say out loud, not "1.75 hours".
     *
     * Used anywhere a task duration is shown: the AI's estimate on the smart-add
     * sheet, the import review rows, and every label on the time-allocation chart.
     * One implementation so the same 90 minutes never reads as "1.5h" on one screen
     * and "1h 30m" on the next.
     */
    fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h ${rest}m"
        }
    }

    /** Start epoch millis for the [SummaryPeriod] window ending now. */
    fun windowStart(period: SummaryPeriod, now: LocalDate = LocalDate.now()): Long {
        val date = when (period) {
            SummaryPeriod.DAILY -> now
            SummaryPeriod.WEEKLY -> now.minus(7, ChronoUnit.DAYS)
            SummaryPeriod.MONTHLY -> now.minus(1, ChronoUnit.MONTHS)
            SummaryPeriod.YEARLY -> now.minus(1, ChronoUnit.YEARS)
        }
        return startOfDay(date)
    }
}

/** Time windows for shareable progress summaries (spec §7). */
enum class SummaryPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}
