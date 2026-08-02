package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.SleepInterval
import com.idomarhaim.goalpilot.domain.model.toSleepNights
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Bucketing raw Health Connect sleep sessions into nights.
 *
 * Fixed to UTC so the test asserts the bucketing rule rather than the CI
 * machine's timezone — every instant below is built through the same zone.
 */
class SleepNightsTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun epochDay(year: Int, month: Int, day: Int): Long =
        java.time.LocalDate.of(year, month, day).toEpochDay()

    @Test
    fun `a night crossing midnight is filed under the waking day`() {
        val sessions = listOf(
            SleepInterval(at(2026, 8, 1, 23, 0), at(2026, 8, 2, 7, 0)),
        )

        val nights = sessions.toSleepNights(zone)

        assertThat(nights).hasSize(1)
        // Slept on the 1st, woke on the 2nd — "last night" means the 2nd.
        assertThat(nights.first().epochDay).isEqualTo(epochDay(2026, 8, 2))
        assertThat(nights.first().minutes).isEqualTo(8 * 60)
    }

    @Test
    fun `overlapping sessions from two apps are merged, not summed`() {
        // A watch and a phone tracker both recorded the same night with slightly
        // different edges. Summing would report over fifteen hours of sleep.
        val sessions = listOf(
            SleepInterval(at(2026, 8, 1, 23, 0), at(2026, 8, 2, 7, 0)),
            SleepInterval(at(2026, 8, 1, 23, 30), at(2026, 8, 2, 7, 30)),
        )

        val nights = sessions.toSleepNights(zone)

        assertThat(nights).hasSize(1)
        // Union of 23:00→07:30 is eight and a half hours.
        assertThat(nights.first().minutes).isEqualTo(8 * 60 + 30)
    }

    @Test
    fun `separate naps on the same day are added together`() {
        val sessions = listOf(
            SleepInterval(at(2026, 8, 2, 1, 0), at(2026, 8, 2, 5, 0)),
            SleepInterval(at(2026, 8, 2, 14, 0), at(2026, 8, 2, 15, 30)),
        )

        val nights = sessions.toSleepNights(zone)

        assertThat(nights).hasSize(1)
        assertThat(nights.first().minutes).isEqualTo(4 * 60 + 90)
    }

    @Test
    fun `distinct nights stay distinct and come back in date order`() {
        val sessions = listOf(
            SleepInterval(at(2026, 8, 2, 23, 0), at(2026, 8, 3, 6, 0)),
            SleepInterval(at(2026, 7, 31, 23, 0), at(2026, 8, 1, 7, 0)),
        )

        val nights = sessions.toSleepNights(zone)

        assertThat(nights.map { it.epochDay })
            .containsExactly(epochDay(2026, 8, 1), epochDay(2026, 8, 3))
            .inOrder()
    }

    @Test
    fun `zero-length and inverted sessions are dropped rather than counted`() {
        val sessions = listOf(
            SleepInterval(at(2026, 8, 2, 3, 0), at(2026, 8, 2, 3, 0)),
            SleepInterval(at(2026, 8, 2, 8, 0), at(2026, 8, 2, 2, 0)),
        )

        assertThat(sessions.toSleepNights(zone)).isEmpty()
    }

    @Test
    fun `hours are derived from minutes`() {
        val sessions = listOf(SleepInterval(at(2026, 8, 1, 23, 0), at(2026, 8, 2, 6, 30)))

        assertThat(sessions.toSleepNights(zone).first().hours).isWithin(0.001).of(7.5)
    }
}
