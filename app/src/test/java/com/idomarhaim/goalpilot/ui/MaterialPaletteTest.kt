package com.idomarhaim.goalpilot.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.theme.accentsFor
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import com.idomarhaim.goalpilot.ui.theme.rampFor
import com.idomarhaim.goalpilot.ui.theme.rampTint
import org.junit.Test
import kotlin.math.abs

/**
 * The **palette transforms** themselves — spec §4.1's `identity · mute ·
 * single-accent ramp`.
 *
 * `ThemePaletteTest` asks whether the sixteen generated schemes are *legible*.
 * This asks whether they are the schemes §4.1 asked for at all, and the
 * distinction matters because the defect §4.1 records is one a contrast test
 * cannot see:
 *
 * > `C24`'s prototype had it in **all four** materials until Ido asked to see
 * > Blossom. The skin picker changed nothing — `AppSkin` was a swatch and no
 * > material read it, so Aurora and Blossom rendered **identically**.
 *
 * Two identical schemes both pass WCAG. The only test that catches it is one
 * that asserts they **differ**, which is what most of this file does.
 */
class MaterialPaletteTest {

    // ── identity ───────────────────────────────────────────────────────────

    @Test
    fun `the two translucent materials leave the skin's scheme untouched`() {
        // Not "close to" — identical. Glass and liquid glass draw depth out of
        // translucency, so transforming the palette would transform it twice.
        AppSkin.entries.forEach { skin ->
            listOf(false, true).forEach { dark ->
                val base = colorSchemeFor(skin, dark)
                listOf(AppMaterial.GLASS, AppMaterial.LIQUID_GLASS).forEach { material ->
                    val scheme = colorSchemeFor(skin, material, dark)
                    assertWithMessage("$skin/$material/${brightness(dark)} primary")
                        .that(scheme.primary).isEqualTo(base.primary)
                    assertWithMessage("$skin/$material/${brightness(dark)} surface")
                        .that(scheme.surface).isEqualTo(base.surface)
                    assertWithMessage("$skin/$material/${brightness(dark)} background")
                        .that(scheme.background).isEqualTo(base.background)
                }
            }
        }
    }

    // ── mute ───────────────────────────────────────────────────────────────

    @Test
    fun `neo flattens every surface step into one ground`() {
        // Neumorphism's claim is that depth comes from a shadow pair on ONE flat
        // surface. A tonal step between the card and the page draws the boundary
        // the shadow pair is supposed to draw, and the extrusion stops reading.
        AppSkin.entries.forEach { skin ->
            listOf(false, true).forEach { dark ->
                val s = colorSchemeFor(skin, AppMaterial.NEO, dark)
                val steps = listOf(
                    s.surface, s.background, s.surfaceBright, s.surfaceDim,
                    s.surfaceContainerLowest, s.surfaceContainerLow, s.surfaceContainer,
                    s.surfaceContainerHigh, s.surfaceContainerHighest,
                )
                assertWithMessage("$skin/neo/${brightness(dark)} — one ground, not a tonal ramp")
                    .that(steps.toSet()).hasSize(1)
            }
        }
    }

    @Test
    fun `neo desaturates the accent rather than replacing it`() {
        // Mute, not recolour: a neo Blossom must still be recognisably Blossom,
        // or the skin picker has stopped working for a quarter of the set by a
        // different route than the one §4.1 names.
        AppSkin.entries.forEach { skin ->
            listOf(false, true).forEach { dark ->
                val base = colorSchemeFor(skin, dark).primary
                val neo = colorSchemeFor(skin, AppMaterial.NEO, dark).primary
                assertWithMessage("$skin/neo/${brightness(dark)} saturation drops")
                    .that(saturation(neo)).isLessThan(saturation(base))
                assertWithMessage("$skin/neo/${brightness(dark)} hue survives")
                    .that(hueGap(neo, base)).isLessThan(12f)
            }
        }
    }

    @Test
    fun `neo's ground is light in light and dark in dark`() {
        AppSkin.entries.forEach { skin ->
            val light = colorSchemeFor(skin, AppMaterial.NEO, dark = false).surface
            val dark = colorSchemeFor(skin, AppMaterial.NEO, dark = true).surface
            assertWithMessage("$skin/neo/light").that(light.luminance()).isGreaterThan(0.6f)
            assertWithMessage("$skin/neo/dark").that(dark.luminance()).isLessThan(0.1f)
        }
    }

    // ── single-accent ramp ─────────────────────────────────────────────────

    @Test
    fun `dark neo collapses primary, secondary and tertiary into one accent`() {
        // "one saturated gradient" is not a figure of speech: three roles, one
        // colour, which is precisely why §4.1 pairs this material with the
        // `.tag` rule rather than leaving categorical identity to colour.
        AppSkin.entries.forEach { skin ->
            val s = colorSchemeFor(skin, AppMaterial.DARK_NEO, dark = true)
            assertWithMessage("$skin/darkneo").that(s.secondary).isEqualTo(s.primary)
            assertWithMessage("$skin/darkneo").that(s.tertiary).isEqualTo(s.primary)
        }
    }

    @Test
    fun `dark neo's accent derives from the selected skin`() {
        // §4.1's first named consequence, and THE test this file exists for:
        //
        //   "Dark neo's accent must derive from the selected skin, or picking
        //    Blossom under dark neo silently renders Aurora and the skin picker
        //    stops working for a quarter of the set."
        //
        // Asserted as pairwise distinctness rather than against fixed hexes, so
        // it keeps working when a third skin is added — which is the case the
        // prototype's version of this defect was found in.
        val ramps = AppSkin.entries.associateWith { rampFor(it) }
        val skins = AppSkin.entries.toList()
        for (i in skins.indices) {
            for (j in i + 1 until skins.size) {
                val a = ramps.getValue(skins[i])
                val b = ramps.getValue(skins[j])
                assertWithMessage("${skins[i]} vs ${skins[j]} — bright end")
                    .that(hueGap(a.first, b.first)).isGreaterThan(20f)
                assertWithMessage("${skins[i]} vs ${skins[j]} — deep end")
                    .that(hueGap(a.second, b.second)).isGreaterThan(20f)
            }
        }
    }

    @Test
    fun `the whole dark neo scheme differs between skins, not only the ramp`() {
        // The prototype's defect was not that one token was shared — it was that
        // the skin reached nothing. So this checks the ground too: if a future
        // edit pins the charcoal to a constant, the skin stops reaching the
        // material's GROUND while the accent test above still passes.
        val aurora = colorSchemeFor(AppSkin.AURORA, AppMaterial.DARK_NEO, dark = true)
        val blossom = colorSchemeFor(AppSkin.BLOSSOM, AppMaterial.DARK_NEO, dark = true)
        assertThat(blossom.primary).isNotEqualTo(aurora.primary)
        assertThat(blossom.surface).isNotEqualTo(aurora.surface)
        assertThat(blossom.surfaceContainerHigh).isNotEqualTo(aurora.surfaceContainerHigh)
    }

    @Test
    fun `dark neo renders dark whatever brightness is asked for`() {
        // The lock, at the layer that draws rather than the layer that declares
        // it -- AppMaterialTest asserts the declaration, this asserts that the
        // scheme generator honours it.
        AppSkin.entries.forEach { skin ->
            val asked = colorSchemeFor(skin, AppMaterial.DARK_NEO, dark = false)
            val locked = colorSchemeFor(skin, AppMaterial.DARK_NEO, dark = true)
            // Compared token by token, not object to object: Material 3's
            // ColorScheme has no `equals`, so `isEqualTo` on two schemes with
            // identical contents fails with "non-equal instance of same class
            // with same string representation" -- a green-looking test would
            // have been just as easy to write in the other direction.
            assertWithMessage("$skin — a light request must produce the dark scheme")
                .that(tokens(asked)).isEqualTo(tokens(locked))
            assertWithMessage("$skin — and it must actually be dark")
                .that(asked.surface.luminance()).isLessThan(0.05f)
        }
    }

    @Test
    fun `dark neo's hero is the ramp, and its ink is dark`() {
        AppSkin.entries.forEach { skin ->
            val accents = accentsFor(skin, AppMaterial.DARK_NEO, dark = false)
            val (bright, deep) = rampFor(skin)
            assertWithMessage("$skin hero").that(accents.heroGradient)
                .containsExactly(deep, bright).inOrder()
            // White on the bright end is about 2.4:1, so the ink flips. If this
            // ever reverts to white the gradient test in ThemePaletteTest fails
            // too -- both are kept because this one says WHY.
            assertWithMessage("$skin ink").that(accents.onHero.luminance()).isLessThan(0.1f)
        }
    }

    @Test
    fun `the categorical ramp lands between the two ends and loses hue identity`() {
        // This pinned rampTint's meaning while it had no call sites, so `#53`'s
        // `.tag` sweep inherited a defined answer rather than inventing one. It
        // is wired now (`ColorExt.categoryFill`) and the assertion is unchanged --
        // which is the point of having written it early.
        AppSkin.entries.forEach { skin ->
            val (bright, deep) = rampFor(skin)
            listOf(Color(0xFF2F855A), Color(0xFF5145CD), Color(0xFFB83280)).forEach { category ->
                val tinted = rampTint(category, skin)
                assertWithMessage("$skin — $category stays on the ramp")
                    .that(tinted.luminance())
                    .isIn(
                        com.google.common.collect.Range.closed(
                            minOf(deep.luminance(), bright.luminance()) - 0.01f,
                            maxOf(deep.luminance(), bright.luminance()) + 0.01f,
                        ),
                    )
            }
        }
        // And the point of the rule: two categories that differed only in hue
        // are no longer told apart by colour.
        val red = rampTint(Color(0xFFCC3333), AppSkin.AURORA)
        val green = rampTint(Color(0xFF33CC33), AppSkin.AURORA)
        assertWithMessage("hue identity survives the ramp — .tag would be unnecessary")
            .that(hueGap(red, green)).isLessThan(5f)
    }

    private fun brightness(dark: Boolean) = if (dark) "dark" else "light"

    /** Every role that a transform can move, as comparable values. */
    private fun tokens(scheme: ColorScheme): List<Color> = with(scheme) {
        listOf(
            primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary,
            secondary, onSecondary, secondaryContainer, onSecondaryContainer,
            tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
            background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant,
            surfaceTint, inverseSurface, inverseOnSurface, error, onError,
            errorContainer, onErrorContainer, outline, outlineVariant, scrim,
            surfaceBright, surfaceDim, surfaceContainer, surfaceContainerHigh,
            surfaceContainerHighest, surfaceContainerLow, surfaceContainerLowest,
        )
    }

    /** HSL saturation, recomputed here so the test does not lean on the code it checks. */
    private fun saturation(color: Color): Float {
        val maxC = maxOf(color.red, color.green, color.blue)
        val minC = minOf(color.red, color.green, color.blue)
        val delta = maxC - minC
        if (delta == 0f) return 0f
        val l = (maxC + minC) / 2f
        return delta / (1f - abs(2f * l - 1f))
    }

    private fun hue(color: Color): Float {
        val maxC = maxOf(color.red, color.green, color.blue)
        val minC = minOf(color.red, color.green, color.blue)
        val delta = maxC - minC
        if (delta == 0f) return 0f
        val h = when (maxC) {
            color.red -> 60f * (((color.green - color.blue) / delta) % 6f)
            color.green -> 60f * (((color.blue - color.red) / delta) + 2f)
            else -> 60f * (((color.red - color.green) / delta) + 4f)
        }
        return if (h < 0f) h + 360f else h
    }

    /** Shortest angular distance between two hues — 350° and 10° are 20° apart, not 340°. */
    private fun hueGap(a: Color, b: Color): Float {
        val raw = abs(hue(a) - hue(b))
        return minOf(raw, 360f - raw)
    }
}
