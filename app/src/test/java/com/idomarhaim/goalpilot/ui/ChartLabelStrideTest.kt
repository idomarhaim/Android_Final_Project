package com.idomarhaim.goalpilot.ui

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.ui.components.MAX_LABELS
import com.idomarhaim.goalpilot.ui.components.labelStride
import org.junit.Test

/**
 * Axis-label thinning for the trend chart. Pure arithmetic, deliberately kept out
 * of the composable so it can be checked here instead of on an emulator.
 */
class ChartLabelStrideTest {

    @Test
    fun `a chart that fits labels every column`() {
        (1..MAX_LABELS).forEach { count ->
            assertThat(labelStride(count)).isEqualTo(1)
        }
    }

    @Test
    fun `a quarter's thirteen weeks show every second label`() {
        assertThat(labelStride(13)).isEqualTo(2)
        assertThat(labelStride(14)).isEqualTo(2)
    }

    @Test
    fun `the stride never leaves more than the maximum number of labels showing`() {
        (1..60).forEach { count ->
            val stride = labelStride(count)
            val shown = (0 until count).count { it % stride == 0 }
            assertThat(shown).isAtMost(MAX_LABELS)
        }
    }

    @Test
    fun `the first column always keeps its label`() {
        (1..60).forEach { count ->
            assertThat(0 % labelStride(count)).isEqualTo(0)
        }
    }

    @Test
    fun `an empty chart does not divide by zero`() {
        assertThat(labelStride(0)).isEqualTo(1)
        assertThat(labelStride(10, maxLabels = 0)).isEqualTo(1)
    }
}
