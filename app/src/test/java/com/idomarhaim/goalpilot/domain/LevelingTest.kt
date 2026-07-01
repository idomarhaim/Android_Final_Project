package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Leveling
import org.junit.Test

class LevelingTest {

    @Test
    fun `points thresholds follow the quadratic curve`() {
        assertThat(Leveling.pointsForLevel(1)).isEqualTo(0)
        assertThat(Leveling.pointsForLevel(2)).isEqualTo(100)
        assertThat(Leveling.pointsForLevel(3)).isEqualTo(300)
        assertThat(Leveling.pointsForLevel(4)).isEqualTo(600)
    }

    @Test
    fun `levelForPoints maps points into the correct level`() {
        assertThat(Leveling.levelForPoints(0)).isEqualTo(1)
        assertThat(Leveling.levelForPoints(99)).isEqualTo(1)
        assertThat(Leveling.levelForPoints(100)).isEqualTo(2)
        assertThat(Leveling.levelForPoints(299)).isEqualTo(2)
        assertThat(Leveling.levelForPoints(300)).isEqualTo(3)
    }

    @Test
    fun `progressWithinLevel is a fraction between 0 and 1`() {
        // Level 2 spans [100, 300). 200 points is exactly halfway.
        assertThat(Leveling.progressWithinLevel(200)).isWithin(0.001f).of(0.5f)
        assertThat(Leveling.progressWithinLevel(100)).isWithin(0.001f).of(0f)
    }

    @Test
    fun `pointsToNextLevel counts remaining points`() {
        assertThat(Leveling.pointsToNextLevel(100)).isEqualTo(200) // to reach 300
        assertThat(Leveling.pointsToNextLevel(0)).isEqualTo(100)
    }
}
