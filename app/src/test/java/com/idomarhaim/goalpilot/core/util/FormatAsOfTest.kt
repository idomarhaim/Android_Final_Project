package com.idomarhaim.goalpilot.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * `DateTimeUtils.formatAsOf` — the as-of caption's time (#50, spec §5.3 §3).
 *
 * The one thing worth pinning is the same-day split. The ticket writes the caption
 * as *"Standings as of 09:14"*, and a bare clock reading on a stamp three days old
 * reads as *this morning* — which would make the caption assert the opposite of
 * what it exists to say.
 *
 * Built from local date-times rather than fixed epoch millis so the assertion does
 * not silently depend on the machine's zone; the split is a *local calendar day*
 * boundary, which is the thing a reader actually experiences.
 */
class FormatAsOfTest {

    private fun millis(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val now = LocalDateTime.of(2026, 8, 19, 14, 30)

    @Test
    fun `a stamp from today is a bare clock reading`() {
        val stamp = millis(LocalDateTime.of(2026, 8, 19, 9, 14))

        assertThat(DateTimeUtils.formatAsOf(stamp, millis(now))).isEqualTo("09:14")
    }

    @Test
    fun `a stamp from yesterday carries its day, or it reads as this morning`() {
        val stamp = millis(LocalDateTime.of(2026, 8, 18, 9, 14))

        val text = DateTimeUtils.formatAsOf(stamp, millis(now))

        assertThat(text).contains("09:14")
        assertThat(text).contains("18")
        assertThat(text).isNotEqualTo("09:14")
    }

    @Test
    fun `earlier the same day still counts as today, however wide the gap`() {
        // 00:01 vs 14:30 is over fourteen hours and one calendar day. The split is
        // the day boundary, not an elapsed-time threshold — a reader reads "00:01"
        // against today's date, which is what they are holding.
        val stamp = millis(LocalDateTime.of(2026, 8, 19, 0, 1))

        assertThat(DateTimeUtils.formatAsOf(stamp, millis(now))).isEqualTo("00:01")
    }

    @Test
    fun `midnight the next day is a different day even one minute later`() {
        val stamp = millis(LocalDateTime.of(2026, 8, 19, 23, 59))
        val later = millis(LocalDateTime.of(2026, 8, 20, 0, 1))

        assertThat(DateTimeUtils.formatAsOf(stamp, later)).isNotEqualTo("23:59")
    }
}
