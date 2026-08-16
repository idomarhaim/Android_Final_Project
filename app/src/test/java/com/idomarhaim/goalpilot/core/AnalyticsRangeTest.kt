package com.idomarhaim.goalpilot.core

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import com.idomarhaim.goalpilot.core.util.Bidi
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
        // Stripped, because windowLabel is direction-isolated (§4.8). The marks
        // themselves are asserted by `window labels are direction-isolated`
        // below — stripping here would otherwise quietly permit their removal.
        assertThat(Bidi.strip(AnalyticsRange.QUARTER.windowLabel(LocalDate.of(2026, 2, 9))))
            .isEqualTo("Q1 2026")
        assertThat(Bidi.strip(AnalyticsRange.QUARTER.windowLabel(LocalDate.of(2026, 8, 3))))
            .isEqualTo("Q3 2026")
        assertThat(Bidi.strip(AnalyticsRange.YEAR.windowLabel(LocalDate.of(2026, 8, 3))))
            .isEqualTo("2026")
    }

    @Test
    fun `window labels are direction-isolated`() {
        // §4.8: `Aug 3 – Aug 9` reorders to `Aug 9 – Aug 3` in a Hebrew
        // paragraph — a wrong week that looks like a right one. Every range is
        // checked, not just WEEK, because each one renders in the same slot.
        AnalyticsRange.entries.forEach { range ->
            val label = range.windowLabel(LocalDate.of(2026, 8, 3), DayOfWeek.SUNDAY)
            assertThat(label.first()).isEqualTo(Bidi.FSI)
            assertThat(label.last()).isEqualTo(Bidi.PDI)
        }
    }

    @Test
    fun `a slash-separated bucket label is direction-isolated`() {
        // `d/M`: two digit runs either side of a neutral slash, so 1/7 renders
        // as 7/1 in RTL — the same defect one scale down.
        val label = AnalyticsRange.QUARTER.buckets(monday, utc, DayOfWeek.SUNDAY).first().label
        assertThat(label.first()).isEqualTo(Bidi.FSI)
        assertThat(label.last()).isEqualTo(Bidi.PDI)
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

    // ── Trend buckets ────────────────────────────────────────────────

    @Test
    fun `buckets tile their range exactly, with no gap and no overlap`() {
        AnalyticsRange.entries.forEach { range ->
            val window = range.window(monday, utc, DayOfWeek.SUNDAY)
            val buckets = range.buckets(monday, utc, DayOfWeek.SUNDAY)

            assertThat(buckets).isNotEmpty()
            assertThat(buckets.first().window.startMillis).isEqualTo(window.startMillis)
            assertThat(buckets.last().window.endMillisExclusive)
                .isEqualTo(window.endMillisExclusive)
            buckets.forEach { bucket ->
                assertThat(bucket.window.startMillis).isLessThan(bucket.window.endMillisExclusive)
            }
            buckets.zipWithNext { a, b ->
                assertThat(a.window.endMillisExclusive).isEqualTo(b.window.startMillis)
            }
        }
    }

    @Test
    fun `a week is seven day-long buckets`() {
        val buckets = AnalyticsRange.WEEK.buckets(monday, utc, DayOfWeek.MONDAY)

        assertThat(buckets).hasSize(7)
        val oneDay = 24 * 60 * 60 * 1000L
        buckets.forEach {
            assertThat(it.window.endMillisExclusive - it.window.startMillis).isEqualTo(oneDay)
        }
    }

    @Test
    fun `a year is twelve month buckets, however unequal they are`() {
        val buckets = AnalyticsRange.YEAR.buckets(monday, utc, DayOfWeek.SUNDAY)

        assertThat(buckets).hasSize(12)
        // February is shorter than January; equal-width buckets would be a bug.
        assertThat(buckets[1].window.endMillisExclusive - buckets[1].window.startMillis)
            .isLessThan(buckets[0].window.endMillisExclusive - buckets[0].window.startMillis)
    }

    @Test
    fun `a day is six four-hour blocks, labelled by the hour they start`() {
        val buckets = AnalyticsRange.DAY.buckets(monday, utc, DayOfWeek.MONDAY)

        assertThat(buckets).hasSize(6)
        assertThat(buckets.map { it.label })
            .containsExactly("00", "04", "08", "12", "16", "20").inOrder()
    }

    @Test
    fun `a month opens with whatever is left of the week it starts in`() {
        // August 2026 starts on a Saturday; with a Sunday week start that is a
        // single-day first column, not one reaching back into July.
        val buckets = AnalyticsRange.MONTH.buckets(monday, utc, DayOfWeek.SUNDAY)

        assertThat(buckets.map { it.label }).containsExactly("1", "2", "9", "16", "23", "30")
            .inOrder()
        val oneDay = 24 * 60 * 60 * 1000L
        assertThat(buckets.first().window.endMillisExclusive - buckets.first().window.startMillis)
            .isEqualTo(oneDay)
        assertThat(buckets[1].window.endMillisExclusive - buckets[1].window.startMillis)
            .isEqualTo(7 * oneDay)
    }

    @Test
    fun `a quarter is cut into weeks`() {
        val buckets = AnalyticsRange.QUARTER.buckets(monday, utc, DayOfWeek.SUNDAY)

        // Q3 is 13 weeks; a quarter not starting on the first day of a week opens
        // with a partial column, so 13 or 14 are both correct — 12 or 15 are not.
        assertThat(buckets.size).isIn(13..14)
        assertThat(Bidi.strip(buckets.first().label)).isEqualTo("1/7")
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
