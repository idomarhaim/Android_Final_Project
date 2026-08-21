package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskScoring
import org.junit.Test

/**
 * The single source of "how long did that take", which every slice of the
 * time-allocation chart is weighted by.
 */
class TaskDurationTest {

    @Test
    fun `a stored estimate wins`() {
        val task = Task(estimatedMinutes = 90)

        assertThat(TaskDuration.minutesOf(task)).isEqualTo(90)
    }

    @Test
    fun `no estimate is a half-hour chore, and is no longer derived from points`() {
        // `#55`, §1.4. This used to read `minutesOf(Task(points = 20)) == 60` — the app
        // deriving how long your life took from a reward number that was itself
        // `5 + 3×words` offline. The inversion runs the other way now, so there is no
        // point value to fall back to and the honest answer is DEFAULT_MINUTES: exactly
        // what `DurationEntry.resolve` already stores for a skipped duration box.
        assertThat(TaskDuration.minutesOf(Task())).isEqualTo(TaskDuration.DEFAULT_MINUTES)
        assertThat(TaskDuration.minutesOf(Task(estimatedMinutes = null)))
            .isEqualTo(TaskDuration.DEFAULT_MINUTES)
    }

    @Test
    fun `a task always counts for something`() {
        // Zero and negative stored values are treated as "never estimated" rather
        // than as zero minutes — a completed task that contributes nothing would
        // silently shrink its whole life area.
        assertThat(TaskDuration.minutesOf(Task(estimatedMinutes = 0))).isEqualTo(30)
        assertThat(TaskDuration.minutesOf(Task())).isAtLeast(TaskDuration.MIN_MINUTES)
    }

    @Test
    fun `absurd values are clamped instead of distorting the chart`() {
        assertThat(TaskDuration.minutesOf(Task(estimatedMinutes = 100_000)))
            .isEqualTo(TaskDuration.MAX_MINUTES)
    }

    @Test
    fun `a legacy point value round-trips to itself through minutes`() {
        // The identity the whole `#55` migration rests on, asserted rather than asserted
        // about: a task completed before the change carries only a stored `points`, and
        // reconstructing `3p` minutes at ROUTINE prices it back at exactly `p`. If this
        // goes red, upgrading the app silently re-prices everybody's history.
        for (legacyPoints in listOf(1, 5, 10, 20, 37, 50)) {
            val minutes = TaskDuration.legacyMinutesFromPoints(legacyPoints)
            assertThat(TaskScoring.pointsFor(minutes, Difficulty.ROUTINE))
                .isEqualTo(legacyPoints)
        }
    }

    @Test
    fun `the legacy reconstruction is deliberately unclamped`() {
        // Clamping to MIN/MAX_MINUTES would break the identity above at both ends and
        // silently re-price the oldest and largest tasks — which is the one thing that
        // function exists to avoid. Its input is a stored point value, never user input.
        assertThat(TaskDuration.legacyMinutesFromPoints(1_000)).isEqualTo(3_000)
        assertThat(TaskDuration.legacyMinutesFromPoints(1)).isEqualTo(3)
        assertThat(TaskDuration.legacyMinutesFromPoints(0)).isEqualTo(1)
    }

    @Test
    fun `sanitize keeps null meaning never estimated`() {
        assertThat(TaskDuration.sanitize(null)).isNull()
        assertThat(TaskDuration.sanitize(0)).isNull()
        assertThat(TaskDuration.sanitize(-5)).isNull()
        assertThat(TaskDuration.sanitize(2)).isEqualTo(TaskDuration.MIN_MINUTES)
        assertThat(TaskDuration.sanitize(45)).isEqualTo(45)
    }

    @Test
    fun `durations are formatted the way people say them`() {
        assertThat(DateTimeUtils.formatMinutes(0)).isEqualTo("0m")
        assertThat(DateTimeUtils.formatMinutes(45)).isEqualTo("45m")
        assertThat(DateTimeUtils.formatMinutes(60)).isEqualTo("1h")
        assertThat(DateTimeUtils.formatMinutes(135)).isEqualTo("2h 15m")
    }
}
