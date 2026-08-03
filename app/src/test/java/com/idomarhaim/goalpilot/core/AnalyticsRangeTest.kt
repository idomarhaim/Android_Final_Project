package com.idomarhaim.goalpilot.core

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The windows behind the analytics range picker. These are **calendar-aligned**,
 * not rolling — see the class doc for why — and the quarter boundaries in
 * particular are the kind of arithmetic that is wrong forever if it is wrong once.
 */
class AnalyticsRangeTest {

    private val utc = ZoneId.of("UTC")

    // A Monday, so the week cases can be read without a calendar to hand.
    private val monday = LocalDate.of(2026, 8, 3)

    @Test
    fun `day is the single calendar day`() {
        assertThat(AnalyticsRange.DAY.startDate(monday, DayOfWeek.MONDAY))
            .isEqualTo(LocalDate.of(2026, 8, 3))
        assertThat(AnalyticsRange.DAY.endDateExclusive(monday, DayOfWeek.MONDAY))
            .isEqualTo(LocalDate.of(2026, 8, 4))
    }

    @Test
    fun `week starts on the locale's first day`() {
        val wednesday = LocalDate.of(2026, 8, 5)

        assertThat(AnalyticsRange.WEEK.startDate(wednesday, DayOfWeek.MONDAY))
            .isEqualTo(LocalDate.of(2026, 8, 3))
        // Israel's week starts on Sunday; hard-coding Monday would file a Sunday
        // workout into the previous week for this app's actual user.
        assertThat(AnalyticsRange.WEEK.startDate(wednesday, DayOfWeek.SUNDAY))
            .isEqualTo(LocalDate.of(2026, 8, 2))
    }

    @Test
    fun `a Sunday belongs to the week that is starting, not the one that ended`() {
        val sunday = LocalDate.of(2026, 8, 2)

        assertThat(AnalyticsRange.WEEK.startDate(sunday, DayOfWeek.SUNDAY)).isEqualTo(sunday)
        assertThat(AnalyticsRange.WEEK.endDateExclusive(sunday, DayOfWeek.SUNDAY))
            .isEqualTo(LocalDate.of(2026, 8, 9))
    }

    @Test
    fun `month runs first to first`() {
        val midMonth = LocalDate.of(2026, 8, 17)

        assertThat(AnalyticsRange.MONTH.startDate(midMonth)).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(AnalyticsRange.MONTH.endDateExclusive(midMonth))
            .isEqualTo(LocalDate.of(2026, 9, 1))
    }

    @Test
    fun `december rolls the month window into the next year`() {
        val december = LocalDate.of(2026, 12, 20)

        assertThat(AnalyticsRange.MONTH.endDateExclusive(december))
            .isEqualTo(LocalDate.of(2027, 1, 1))
    }

    @Test
    fun `quarters start in January, April, July and October`() {
        val expected = mapOf(
            LocalDate.of(2026, 1, 4) to LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31) to LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 8, 3) to LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 10, 1),
        )
        expected.forEach { (day, start) ->
            assertThat(AnalyticsRange.QUARTER.startDate(day)).isEqualTo(start)
        }
        assertThat(AnalyticsRange.QUARTER.endDateExclusive(LocalDate.of(2026, 11, 30)))
            .isEqualTo(LocalDate.of(2027, 1, 1))
    }

    @Test
    fun `quarter labels are Q1 to Q4`() {
        assertThat(AnalyticsRange.QUARTER.windowLabel(LocalDate.of(2026, 2, 9)))
            .isEqualTo("Q1 2026")
        assertThat(AnalyticsRange.QUARTER.windowLabel(LocalDate.of(2026, 8, 3)))
            .isEqualTo("Q3 2026")
        assertThat(AnalyticsRange.YEAR.windowLabel(LocalDate.of(2026, 8, 3))).isEqualTo("2026")
    }

    @Test
    fun `year runs January to January`() {
        val midYear = LocalDate.of(2026, 8, 3)

        assertThat(AnalyticsRange.YEAR.startDate(midYear)).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(AnalyticsRange.YEAR.endDateExclusive(midYear))
            .isEqualTo(LocalDate.of(2027, 1, 1))
    }

    @Test
    fun `the window is half-open, so a midnight completion belongs to exactly one day`() {
        val window = AnalyticsRange.DAY.window(monday, utc, DayOfWeek.MONDAY)
        val nextDay = AnalyticsRange.DAY.window(monday.plusDays(1), utc, DayOfWeek.MONDAY)

        assertThat(window.endMillisExclusive).isEqualTo(nextDay.startMillis)
        assertThat(window.contains(window.startMillis)).isTrue()
        assertThat(window.contains(window.endMillisExclusive)).isFalse()
        assertThat(nextDay.contains(window.endMillisExclusive)).isTrue()
    }

    @Test
    fun `every range widens the one before it`() {
        val lengths = AnalyticsRange.entries.map { range ->
            val w = range.window(monday, utc, DayOfWeek.MONDAY)
            w.endMillisExclusive - w.startMillis
        }
        assertThat(lengths).isInOrder()
    }
}
