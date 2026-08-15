package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Goal
import org.junit.Test

class GoalTest {

    @Test
    fun `progressFraction is current over target`() {
        assertThat(Goal(currentValue = 25.0, targetValue = 100.0).progressFraction)
            .isWithin(0.001f).of(0.25f)
    }

    @Test
    fun `progressFraction is not clamped above, because overshoot is legal`() {
        // §1.5 / #49: the clamp used to report a beaten goal as merely finished.
        assertThat(Goal(currentValue = 150.0, targetValue = 100.0).progressFraction)
            .isWithin(0.001f).of(1.5f)
    }

    @Test
    fun `progressFraction is not clamped below, because progress can fall`() {
        // A correcting entry or an unticked task can take the sum negative; the
        // old `coerceIn` reported that as untouched.
        assertThat(Goal(currentValue = -5.0, targetValue = 100.0).progressFraction)
            .isWithin(0.001f).of(-0.05f)
    }

    @Test
    fun `zero or negative target yields zero progress`() {
        // Still zero, and for a different reason than a clamp: a fraction of
        // nothing means nothing, so there is no number to report.
        assertThat(Goal(currentValue = 10.0, targetValue = 0.0).progressFraction).isEqualTo(0f)
    }

    @Test
    fun `isComplete when at or past target`() {
        assertThat(Goal(currentValue = 100.0, targetValue = 100.0).isComplete).isTrue()
        assertThat(Goal(currentValue = 99.0, targetValue = 100.0).isComplete).isFalse()
        assertThat(Goal(currentValue = 150.0, targetValue = 100.0).isComplete).isTrue()
    }

    @Test
    fun `progressPercent is an integer percentage and may exceed 100`() {
        assertThat(Goal(currentValue = 33.0, targetValue = 100.0).progressPercent).isEqualTo(33)
        assertThat(Goal(currentValue = 120.0, targetValue = 100.0).progressPercent).isEqualTo(120)
    }
}
