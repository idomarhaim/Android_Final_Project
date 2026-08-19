package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [AppBrightness] — spec §4.9's third Appearance control.
 *
 * The only interesting behaviour is [AppBrightness.isDark], and it is
 * interesting for one reason: **two of the three cases must ignore the device**.
 * A picker that renders three options while the device silently keeps deciding
 * is exactly the "control that changes nothing" §4.9 forbids, and it looks
 * completely correct on a phone whose system setting happens to agree.
 */
class AppBrightnessTest {

    @Test
    fun `system follows the device, in both directions`() {
        assertThat(AppBrightness.SYSTEM.isDark(systemIsDark = true)).isTrue()
        assertThat(AppBrightness.SYSTEM.isDark(systemIsDark = false)).isFalse()
    }

    @Test
    fun `light overrides a dark device`() {
        assertThat(AppBrightness.LIGHT.isDark(systemIsDark = true)).isFalse()
        assertThat(AppBrightness.LIGHT.isDark(systemIsDark = false)).isFalse()
    }

    @Test
    fun `dark overrides a light device`() {
        assertThat(AppBrightness.DARK.isDark(systemIsDark = false)).isTrue()
        assertThat(AppBrightness.DARK.isDark(systemIsDark = true)).isTrue()
    }

    @Test
    fun `every id round-trips`() {
        AppBrightness.entries.forEach { brightness ->
            assertThat(AppBrightness.fromId(brightness.id)).isEqualTo(brightness)
        }
    }

    @Test
    fun `an unknown or absent id falls back to the default rather than throwing`() {
        assertThat(AppBrightness.fromId(null)).isEqualTo(AppBrightness.DEFAULT)
        assertThat(AppBrightness.fromId("")).isEqualTo(AppBrightness.DEFAULT)
        assertThat(AppBrightness.fromId("sepia")).isEqualTo(AppBrightness.DEFAULT)
    }

    @Test
    fun `the default is follow-the-device, per §4_9's defaults table`() {
        assertThat(AppBrightness.DEFAULT).isEqualTo(AppBrightness.SYSTEM)
    }
}
