package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Goal
import org.junit.Test

class GoalTest {

    @Test
    fun `progressFraction is current over target, clamped to 0_1`() {
        assertThat(Goal(currentValue = 25.0, targetValue = 100.0).progressFraction)
            .isWithin(0.001f).of(0.25f)
        assertThat(Goal(currentValue = 150.0, targetValue = 100.0).progressFraction)
            .isEqualTo(1f)
        assertThat(Goal(currentValue = -5.0, targetValue = 100.0).progressFraction)
            .isEqualTo(0f)
    }

    @Test
    fun `zero or negative target yields zero progress`() {
        assertThat(Goal(currentValue = 10.0, targetValue = 0.0).progressFraction).isEqualTo(0f)
    }

    @Test
    fun `isComplete when at or past target`() {
        assertThat(Goal(currentValue = 100.0, targetValue = 100.0).isComplete).isTrue()
        assertThat(Goal(currentValue = 99.0, targetValue = 100.0).isComplete).isFalse()
    }

    @Test
    fun `progressPercent is an integer percentage`() {
        assertThat(Goal(currentValue = 33.0, targetValue = 100.0).progressPercent).isEqualTo(33)
    }
}
