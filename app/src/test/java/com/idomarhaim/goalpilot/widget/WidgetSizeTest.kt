package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import org.junit.Test

/**
 * §4.5: *the launcher decides the real dp a `2×2` or `4×4` occupies, and it
 * varies by device and launcher, so every tile must survive being smaller than
 * it is drawn.*
 *
 * So the size class is a function of the space actually granted, never of the
 * cell count requested — and these are the boundaries that decision turns on.
 */
class WidgetSizeTest {

    @Test
    fun `nominal cell sizes resolve to their own class`() {
        // 70 dp per cell less 30 dp of margin is the platform's own arithmetic:
        // two cells land near 110 dp, four near 250 dp.
        assertThat(WidgetSize.of(110, 110)).isEqualTo(WidgetSize.SMALL)
        assertThat(WidgetSize.of(250, 110)).isEqualTo(WidgetSize.WIDE)
        assertThat(WidgetSize.of(110, 250)).isEqualTo(WidgetSize.TALL)
        assertThat(WidgetSize.of(250, 250)).isEqualTo(WidgetSize.LARGE)
    }

    @Test
    fun `the threshold is inclusive on the larger side`() {
        val t = WidgetSize.WIDE_THRESHOLD_DP
        assertThat(WidgetSize.of(t - 1, t - 1)).isEqualTo(WidgetSize.SMALL)
        assertThat(WidgetSize.of(t, t - 1)).isEqualTo(WidgetSize.WIDE)
        assertThat(WidgetSize.of(t - 1, t)).isEqualTo(WidgetSize.TALL)
        assertThat(WidgetSize.of(t, t)).isEqualTo(WidgetSize.LARGE)
    }

    @Test
    fun `an odd cell count a launcher allows still resolves, and resolves upward`() {
        // A 3x3 is legal on every launcher and is impossible to design for. It has
        // to land somewhere, and it lands on the larger layout deliberately: a wide
        // layout in a narrow cell truncates text, a narrow layout in a wide cell
        // only wastes space, and only one of those loses information.
        assertThat(WidgetSize.of(180, 180)).isEqualTo(WidgetSize.LARGE)
    }

    @Test
    fun `a launcher reporting nothing does not crash the tile`() {
        // Some launchers hand a zero-size AppWidgetOptions bundle on first placement.
        assertThat(WidgetSize.of(0, 0)).isEqualTo(WidgetSize.SMALL)
    }

    @Test
    fun `isWide and isTall agree with the class`() {
        assertThat(WidgetSize.SMALL.isWide).isFalse()
        assertThat(WidgetSize.SMALL.isTall).isFalse()
        assertThat(WidgetSize.WIDE.isWide).isTrue()
        assertThat(WidgetSize.WIDE.isTall).isFalse()
        assertThat(WidgetSize.TALL.isWide).isFalse()
        assertThat(WidgetSize.TALL.isTall).isTrue()
        assertThat(WidgetSize.LARGE.isWide).isTrue()
        assertThat(WidgetSize.LARGE.isTall).isTrue()
    }
}
