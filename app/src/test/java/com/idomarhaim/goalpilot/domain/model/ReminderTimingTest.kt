package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

/**
 * [ReminderTiming] — §2.5's *"the one thing this app knows that Google Calendar does not"*.
 *
 * Two behaviours carry the feature and both are invisible on a device until the exact night
 * they matter: the **backwards** computation from the task's own duration, and the **clamp**
 * into waking hours. A reminder that fires at the wrong time is not a crash and not a red
 * test — it is a notification that arrives while the user is asleep, or one that arrives after
 * the moment it was telling them to start.
 */
class ReminderTimingTest {

    private val waking7to23 = WakingHours(startMinutes = 7 * 60, endMinutes = 23 * 60)

    // ── The backwards computation ───────────────────────────────────

    @Test
    fun `reminds you when you would have to start, not a fixed interval before the deadline`() {
        // Due 17:00, takes two hours -> start at 15:00. A calendar would say "15 minutes
        // before"; this is the whole difference.
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 20, 17, 0),
            durationMinutes = 120,
            waking = waking7to23,
        )
        assertThat(plan.fireAt).isEqualTo(LocalDateTime.of(2026, 8, 20, 15, 0))
        assertThat(plan.idealAt).isEqualTo(LocalDateTime.of(2026, 8, 20, 15, 0))
        assertThat(plan.movedForSleep).isFalse()
    }

    @Test
    fun `a longer task is reminded earlier for the same deadline`() {
        val due = LocalDateTime.of(2026, 8, 20, 17, 0)
        val short = ReminderTiming.plan(due, durationMinutes = 30, waking = waking7to23)
        val long = ReminderTiming.plan(due, durationMinutes = 240, waking = waking7to23)
        assertThat(long.fireAt).isLessThan(short.fireAt)
    }

    // ── The clamp ───────────────────────────────────────────────────

    @Test
    fun `the spec's own example - due at 0600 taking four hours reminds the evening before`() {
        // §2.5: "due at 06:00 and it takes about 4 hours - worth starting tonight".
        // Ideal start is 02:00, which is asleep, so it clamps back to the last waking minute.
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 21, 6, 0),
            durationMinutes = 4 * 60,
            waking = waking7to23,
        )
        assertThat(plan.idealAt).isEqualTo(LocalDateTime.of(2026, 8, 21, 2, 0))
        assertThat(plan.fireAt).isEqualTo(LocalDateTime.of(2026, 8, 20, 22, 59))
        assertThat(plan.movedForSleep).isTrue()
    }

    @Test
    fun `the clamp moves the reminder EARLIER, never later`() {
        // The tempting direction is to push it to the next morning -- 07:00 on the due date,
        // which is one hour before a 06:00 deadline that has already passed. This is the
        // assertion that would catch that.
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 21, 6, 0),
            durationMinutes = 4 * 60,
            waking = waking7to23,
        )
        assertThat(plan.fireAt).isLessThan(plan.idealAt)
        assertThat(plan.fireAt).isLessThan(plan.dueAt)
    }

    @Test
    fun `a reminder already inside waking hours is not moved and says so`() {
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 20, 12, 0),
            durationMinutes = 60,
            waking = waking7to23,
        )
        assertThat(plan.fireAt).isEqualTo(plan.idealAt)
        assertThat(plan.movedForSleep).isFalse()
    }

    @Test
    fun `an ideal moment before the morning start clamps to the previous evening`() {
        // Due 09:00, takes 4 h -> 05:00, which is before the 07:00 start on the SAME day. The
        // clamp must not find 07:00 that morning (after the ideal) or 22:59 that evening
        // (after the deadline); it is the previous evening.
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 21, 9, 0),
            durationMinutes = 4 * 60,
            waking = waking7to23,
        )
        assertThat(plan.fireAt).isEqualTo(LocalDateTime.of(2026, 8, 20, 22, 59))
    }

    @Test
    fun `waking hours that wrap past midnight clamp into the night, not out of it`() {
        // A night-shift day: awake 22:00 - 06:00. 03:00 is a waking hour here and 12:00 is not.
        val nightShift = WakingHours(startMinutes = 22 * 60, endMinutes = 6 * 60)

        val awakeAtThree = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 21, 4, 0),
            durationMinutes = 60,
            waking = nightShift,
        )
        assertThat(awakeAtThree.fireAt).isEqualTo(LocalDateTime.of(2026, 8, 21, 3, 0))
        assertThat(awakeAtThree.movedForSleep).isFalse()

        // Ideal 12:00 is asleep on this schedule -> back to the last waking minute, 05:59.
        val asleepAtNoon = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 21, 14, 0),
            durationMinutes = 120,
            waking = nightShift,
        )
        assertThat(asleepAtNoon.fireAt).isEqualTo(LocalDateTime.of(2026, 8, 21, 5, 59))
        assertThat(asleepAtNoon.movedForSleep).isTrue()
    }

    @Test
    fun `an empty waking span does not move the reminder and does not hang`() {
        // Both handles on the same time: the user has said they are awake for zero minutes.
        // There is no waking minute to clamp into, so the honest answer is the computed time.
        // The failure this guards is a search for a waking minute that never terminates.
        val empty = WakingHours(startMinutes = 9 * 60, endMinutes = 9 * 60)
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 21, 6, 0),
            durationMinutes = 4 * 60,
            waking = empty,
        )
        assertThat(plan.fireAt).isEqualTo(LocalDateTime.of(2026, 8, 21, 2, 0))
        assertThat(plan.movedForSleep).isFalse()
    }

    // ── Degenerate durations ────────────────────────────────────────

    @Test
    fun `a zero or negative duration reminds at the deadline rather than throwing`() {
        val due = LocalDateTime.of(2026, 8, 20, 12, 0)
        assertThat(ReminderTiming.plan(due, 0, waking7to23).fireAt).isEqualTo(due)
        assertThat(ReminderTiming.plan(due, -30, waking7to23).fireAt).isEqualTo(due)
    }

    @Test
    fun `a duration longer than the deadline is away still lands before it`() {
        val due = LocalDateTime.of(2026, 8, 20, 12, 0)
        val plan = ReminderTiming.plan(due, durationMinutes = 40 * 60, waking = waking7to23)
        assertThat(plan.fireAt).isLessThan(due)
    }

    // ── isPast ──────────────────────────────────────────────────────

    @Test
    fun `isPast is true at the fire moment itself, not only after it`() {
        val plan = ReminderTiming.plan(
            dueAt = LocalDateTime.of(2026, 8, 20, 12, 0),
            durationMinutes = 60,
            waking = waking7to23,
        )
        assertThat(plan.isPast(LocalDateTime.of(2026, 8, 20, 11, 0))).isTrue()
        assertThat(plan.isPast(LocalDateTime.of(2026, 8, 20, 10, 59))).isFalse()
    }

    // ── The predicate the clamp is built on ─────────────────────────

    @Test
    fun `end is exclusive, and lengthMinutes agrees with the predicate`() {
        // The two must not disagree: a span whose start equals its end is empty in both, and
        // 23:00 is asleep on a 07:00-23:00 day by the same rule that makes 22:59 awake.
        assertThat(waking7to23.containsMinuteOfDay(22 * 60 + 59)).isTrue()
        assertThat(waking7to23.containsMinuteOfDay(23 * 60)).isFalse()
        assertThat(waking7to23.containsMinuteOfDay(7 * 60)).isTrue()
        assertThat(waking7to23.containsMinuteOfDay(6 * 60 + 59)).isFalse()

        val empty = WakingHours(startMinutes = 9 * 60, endMinutes = 9 * 60)
        assertThat(empty.lengthMinutes).isEqualTo(0)
        assertThat(empty.containsMinuteOfDay(9 * 60)).isFalse()
    }
}
