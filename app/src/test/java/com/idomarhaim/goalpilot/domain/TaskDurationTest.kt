package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import org.junit.Test

/**
 * The single source of "how long did that take", which every slice of the
 * time-allocation chart is weighted by.
 */
class TaskDurationTest {

    @Test
    fun `a stored estimate wins`() {
        val task = Task(points = 10, estimatedMinutes = 90)

        assertThat(TaskDuration.minutesOf(task)).isEqualTo(90)
    }

    @Test
    fun `no estimate falls back to three minutes per point`() {
        assertThat(TaskDuration.minutesOf(Task(points = 20))).isEqualTo(60)
        assertThat(TaskDuration.minutesOf(Task(points = 5))).isEqualTo(15)
    }

    @Test
    fun `a task always counts for something`() {
        // Zero and negative stored values are treated as "never estimated" rather
        // than as zero minutes — a completed task that contributes nothing would
        // silently shrink its whole life area.
        assertThat(TaskDuration.minutesOf(Task(points = 10, estimatedMinutes = 0)))
            .isEqualTo(30)
        assertThat(TaskDuration.minutesOf(Task(points = 0))).isAtLeast(TaskDuration.MIN_MINUTES)
    }

    @Test
    fun `absurd values are clamped instead of distorting the chart`() {
        assertThat(TaskDuration.minutesOf(Task(points = 10, estimatedMinutes = 100_000)))
            .isEqualTo(TaskDuration.MAX_MINUTES)
        assertThat(TaskDuration.fallbackMinutes(1_000)).isEqualTo(TaskDuration.MAX_MINUTES)
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
