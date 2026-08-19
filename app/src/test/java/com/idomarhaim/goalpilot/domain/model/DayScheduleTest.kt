package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

/**
 * [DaySchedule] and [WakingHours] — spec §4.9's *Your day*.
 *
 * ### The one thing these tests exist for
 *
 * §4.9 says *Plan tomorrow at* is **"derived, so it is sane untouched and moves
 * when waking hours move"**. That is a claim about a relationship, and a
 * relationship is what a pair of independent fields cannot hold: two loose
 * `Int`s can be *written* to agree and then drift, which is §0.3's *second
 * number that quietly disagrees* with the disagreement pre-installed.
 *
 * So the assertions below are mostly about **which of the two states the
 * schedule is in**, not about arithmetic. `planningFollowsWaking` is what the
 * screen's consequence line reads, and it is the difference between the screen
 * telling the truth and the screen telling a story.
 */
class DayScheduleTest {

    // ------------------------------------------------ §4.9's defaults table

    @Test
    fun `the default day is 07-00 to 23-00`() {
        val waking = WakingHours.DEFAULT
        assertThat(waking.start).isEqualTo(LocalTime.of(7, 0))
        assertThat(waking.end).isEqualTo(LocalTime.of(23, 0))
    }

    @Test
    fun `the default day is sixteen hours, which is what §4_9 states`() {
        assertThat(WakingHours.DEFAULT.lengthMinutes).isEqualTo(16 * 60)
    }

    @Test
    fun `the default load bar reddens at twelve hours, which is what §4_9 states`() {
        // §4.9 gives exactly one pair -- a 16 h day reddens at 12 h. This is the
        // assertion that pins the fraction inferred from it.
        assertThat(WakingHours.DEFAULT.loadBarRedMinutes).isEqualTo(12 * 60)
    }

    @Test
    fun `the default planning time is 22-00, one hour before waking hours end`() {
        assertThat(DaySchedule.DEFAULT.planningTime).isEqualTo(LocalTime.of(22, 0))
    }

    @Test
    fun `the default schedule is untouched, so it still follows waking hours`() {
        assertThat(DaySchedule.DEFAULT.planningFollowsWaking).isTrue()
    }

    // ------------------------------------------- the relationship §4.9 claims

    @Test
    fun `moving waking hours moves the planning time with them`() {
        val moved = DaySchedule.DEFAULT.copy(
            waking = WakingHours(startMinutes = 6 * 60, endMinutes = 21 * 60),
        )
        assertThat(moved.planningTime).isEqualTo(LocalTime.of(20, 0))
        assertThat(moved.planningFollowsWaking).isTrue()
    }

    @Test
    fun `an override pins the planning time and says so`() {
        val pinned = DaySchedule.DEFAULT.copy(planningOverrideMinutes = 19 * 60 + 30)

        assertThat(pinned.planningTime).isEqualTo(LocalTime.of(19, 30))
        assertThat(pinned.planningFollowsWaking).isFalse()
    }

    @Test
    fun `once pinned, moving waking hours no longer moves it`() {
        val pinned = DaySchedule.DEFAULT.copy(planningOverrideMinutes = 19 * 60)
        val moved = pinned.copy(waking = WakingHours(startMinutes = 5 * 60, endMinutes = 20 * 60))

        assertThat(moved.planningTime).isEqualTo(LocalTime.of(19, 0))
        // The screen must be able to *say* this, which is why it is state and
        // not a value someone has to compare two numbers to discover.
        assertThat(moved.planningFollowsWaking).isFalse()
    }

    @Test
    fun `clearing the override hands the time back to the derivation`() {
        val released = DaySchedule.DEFAULT
            .copy(planningOverrideMinutes = 19 * 60)
            .copy(planningOverrideMinutes = null)

        assertThat(released.planningFollowsWaking).isTrue()
        assertThat(released.planningTime).isEqualTo(LocalTime.of(22, 0))
    }

    // ------------------------------------------------------ the wrapping day

    @Test
    fun `a night shift wraps past midnight instead of going negative`() {
        val night = WakingHours(startMinutes = 22 * 60, endMinutes = 6 * 60)
        assertThat(night.lengthMinutes).isEqualTo(8 * 60)
    }

    @Test
    fun `a planning time an hour before a post-midnight end wraps too`() {
        val night = DaySchedule(WakingHours(startMinutes = 22 * 60, endMinutes = 30))
        assertThat(night.planningTime).isEqualTo(LocalTime.of(23, 30))
    }

    @Test
    fun `a start equal to an end is an empty day, not a full one`() {
        // 1440 would be "plausible and wrong": a consumer dividing by it gets a
        // sane-looking answer for a span the user has said is empty.
        val degenerate = WakingHours(startMinutes = 9 * 60, endMinutes = 9 * 60)
        assertThat(degenerate.lengthMinutes).isEqualTo(0)
        assertThat(degenerate.loadBarRedMinutes).isEqualTo(0)
    }

    // ------------------------------------------------ tolerance of the store

    @Test
    fun `a corrupt stored minute lands on a wrong-but-usable time, never an exception`() {
        // The caller is a preference read that runs before the first frame.
        assertThat(minutesToTime(-30)).isEqualTo(LocalTime.of(23, 30))
        assertThat(minutesToTime(MINUTES_PER_DAY)).isEqualTo(LocalTime.of(0, 0))
        assertThat(minutesToTime(MINUTES_PER_DAY + 90)).isEqualTo(LocalTime.of(1, 30))
    }
}
