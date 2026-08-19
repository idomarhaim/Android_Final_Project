package com.idomarhaim.goalpilot.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Small date/time helpers used across the UI and summary calculations. */
object DateTimeUtils {

    // `get()`, never `val` — see AppDateFormatters. A `val` here resolves
    // Locale.getDefault() once at class-init and is precisely §5.1's
    // "process-scoped vals no switch can move".
    private val dayFormatter get() = AppDateFormatters.of("MMM d, yyyy")

    // 24-hour and pattern-fixed on purpose: the as-of caption is a short clock
    // reading beside a number, and "9:14 AM" doubles its width for nothing. Still
    // goes through AppDateFormatters so the locale is read at call time.
    private val timeFormatter get() = AppDateFormatters.of("HH:mm")

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

    /**
     * The *as-of* caption's time (#50, spec §5.3 §3): `"09:14"` for a stamp from
     * today, `"Aug 17, 2026 09:14"` for anything older.
     *
     * The split is the whole point rather than a nicety. The caption exists to stop
     * a stale number reading as a current one, and a bare `"09:14"` on a stamp three
     * days old reads as *this morning* — it would make the caption say the opposite
     * of what it is for. [relative] is not used here for the same reason in reverse:
     * *"3d ago"* is right for a feed post, but a standings caption is read as a
     * claim about a clock time, and the ticket writes it as one.
     */
    fun formatAsOf(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val stamp = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val time = timeFormatter.format(stamp)
        val sameDay = stamp.toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return if (sameDay) time else "${formatDay(epochMillis)} $time"
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
