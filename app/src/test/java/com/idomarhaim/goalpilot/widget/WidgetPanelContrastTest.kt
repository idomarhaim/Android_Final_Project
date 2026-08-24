package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.widget.WidgetPalette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Test

/**
 * The floor under `WidgetPalette.WIDGET_PANEL_ALPHA`.
 *
 * A widget panel became **translucent** on 2026-08-24, because an opaque plate
 * on a launcher is the whole of Ido's *"the widgets look very bad"* and the
 * prototype's widgets are panels you can see the wallpaper through.
 *
 * That trade has one real cost and it is **not** the one the old code cited.
 * The app can afford alpha `0.13` on its glass because it draws its own ground
 * underneath (`Modifier.gpPage`). A widget composites over **whatever wallpaper
 * the user has**, which this app neither controls nor can read — so the panel
 * has to stay dominant enough that its own text is legible over *any* of them.
 *
 * ## Why pure black and pure white are the whole test
 *
 * Contrast is monotonic in the composited luminance, and every wallpaper pixel
 * lies between black and white, so if the two extremes hold, everything between
 * them holds. That is what makes a two-point check a proof rather than a sample
 * — and it is why this test does not need a corpus of real wallpapers.
 *
 * ⚠️ **What it still cannot see**, said rather than implied: a launcher that
 * dims or tints behind widgets (several do), a live wallpaper that animates past
 * the extremes, and any skin added later — the last of which this test *will*
 * catch, because it iterates `AppSkin.entries` rather than a fixed list.
 * `WIDGET_PANEL_ALPHA` ships at `0.78` against a measured minimum of `0.72`
 * precisely to leave room for the first two.
 */
class WidgetPanelContrastTest {

    /** WCAG AA for body text. */
    private val bodyFloor = 4.5

    /** WCAG AA for large text and non-text; what the dimmer secondary ink is held to. */
    private val secondaryFloor = 3.0

    @Test
    fun `the panel is actually translucent, on every skin and both brightnesses`() {
        // Guards the FEATURE, not the number. Without this, a later change that
        // quietly restores alpha 1.0 would still pass every contrast assertion
        // below -- opaque is the easiest way to pass a contrast test and the one
        // thing this change exists to stop.
        for (skin in AppSkin.entries) {
            for (isDark in listOf(false, true)) {
                val ground = WidgetPalette.computed(skin, isDark).ground
                assertWithMessage("panel opacity for ${skin.id} dark=$isDark")
                    .that(alphaOf(ground)).isLessThan(255)
            }
        }
        assertThat(WidgetPalette.WIDGET_PANEL_ALPHA).isLessThan(1f)
    }

    @Test
    fun `panel text stays legible over the worst wallpaper that can exist`() {
        val failures = mutableListOf<String>()

        for (skin in AppSkin.entries) {
            for (isDark in listOf(false, true)) {
                val s = WidgetPalette.computed(skin, isDark)
                for ((name, wallpaper) in listOf("black" to 0x000000, "white" to 0xFFFFFF)) {
                    // The panel over the wallpaper is what the ink actually sits on.
                    val panel = composite(s.ground, wallpaper)

                    val onSurface = contrast(composite(s.onSurface, panel), panel)
                    if (onSurface < bodyFloor) {
                        failures += "${skin.id} dark=$isDark on $name wallpaper: " +
                            "onSurface %.2f < %.1f".format(onSurface, bodyFloor)
                    }

                    // Already translucent in its own right, so it composites TWICE
                    // -- ink over panel over wallpaper. Reading it as opaque is the
                    // mistake that would make this test pass while the widget is
                    // unreadable.
                    val variant = contrast(composite(s.onSurfaceVariant, panel), panel)
                    if (variant < secondaryFloor) {
                        failures += "${skin.id} dark=$isDark on $name wallpaper: " +
                            "onSurfaceVariant %.2f < %.1f".format(variant, secondaryFloor)
                    }
                }
            }
        }

        assertWithMessage(
            "WIDGET_PANEL_ALPHA = ${WidgetPalette.WIDGET_PANEL_ALPHA} no longer keeps widget text " +
                "legible over an arbitrary wallpaper. Raise it, or darken/lighten the ink. " +
                "Failures",
        ).that(failures).isEmpty()
    }

    // ── colour maths, kept local: this test must not share a helper with the
    //    code it is checking, or a bug in the helper hides itself. ────────────

    private fun alphaOf(argb: Int) = (argb ushr 24) and 0xFF

    /** Source-over: [fg] at its own alpha onto an opaque [bg]. Returns opaque RGB. */
    private fun composite(fg: Int, bg: Int): Int {
        val a = alphaOf(fg) / 255.0
        fun ch(shift: Int): Int {
            val f = (fg ushr shift) and 0xFF
            val b = (bg ushr shift) and 0xFF
            return (a * f + (1 - a) * b).toInt().coerceIn(0, 255)
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun luminance(rgb: Int): Double {
        fun lin(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin((rgb ushr 16) and 0xFF) +
            0.7152 * lin((rgb ushr 8) and 0xFF) +
            0.0722 * lin(rgb and 0xFF)
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
