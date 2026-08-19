package com.idomarhaim.goalpilot.ui.widget

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import androidx.glance.unit.ColorProvider
import com.idomarhaim.goalpilot.R
import androidx.compose.ui.graphics.Color as ComposeColor
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetTileUseCase
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import kotlin.math.roundToInt

/**
 * The widget pack's colours — **neo**, wearing the selected [AppSkin], in both
 * brightnesses at once.
 *
 * ## Why neo, and why only neo
 *
 * §4.1 ships four materials as a user-selectable skin, and it is explicit that
 * *no screen may depend on a property a single material has*. A widget is the
 * one surface where that cannot be honoured as written: Glance renders into a
 * `RemoteViews`, so there is no `backdrop-filter`, no `RenderEffect`, no
 * `Modifier.blur` and no SVG filter on the other side of the boundary — and
 * glassmorphism and liquid glass are *made of* blur and refraction. §4.9 already
 * saw this coming and defaulted the material to neo **partly for this ticket**:
 * it is the only material with both a light and a dark scheme **and** no blur
 * under it. The issue asked this ticket to decide; this is the decision.
 *
 * Dark neo is *not* a second entry here. §4.1 makes it **brightness-locked**, and
 * a home screen follows the device's own switch with nobody to tell it
 * otherwise, so a brightness-locked material would silently ignore that switch.
 *
 * ## Why this holds BOTH schemes, and takes no `isDark`
 *
 * `Observed:` 2026-08-16, emulator `Pixel_10_Pro_XL`, API 37. The first version
 * took `isDark` and resolved one scheme while building the tree. Switching the
 * device to dark left every tile **light**, and it stayed light through a forced
 * `APPWIDGET_UPDATE` — because a `RemoteViews` is inflated *later, by the
 * launcher*, and whichever single colour was baked in at build time is simply
 * the wrong one by then. A widget cannot ask what the theme is; it has to ship
 * **both answers** and let the host choose. A colour **resource** is exactly
 * that: `values/` and `values-night/` both define it, and the launcher resolves
 * whichever its own configuration calls for, at inflate time, with no refresh
 * and no code. Glance reaches it through `ColorProvider(resId)`.
 * (`androidx.glance.color.DayNightColorProvider` would say this directly and is
 * annotated `RestrictTo`, so it is not ours to use.)
 *
 * Bitmaps cannot carry two answers, so they do not try — see [ChartInk].
 *
 * ## The skin has to actually arrive
 *
 * §4.1 carries an enforcement note found the hard way: *a skin picker which no
 * material reads is a control that does nothing, and it looks correct in
 * source.* So every colour below derives from [colorSchemeFor] — the same
 * function the app's own theme uses, so there is one palette in this codebase
 * rather than two that agree until one is edited. The neo *transform* on top of
 * it is `mute` (§4.1's table).
 */
data class WidgetPalette(
    val light: NeoScheme,
    val dark: NeoScheme,
    private val res: SkinColorRes,
) {
    // ColorProvider(resId), NOT ColorProvider(color): the launcher resolves the
    // resource against ITS configuration when it inflates, which is the only
    // moment that knows whether the device is dark. See the class KDoc.
    val onSurfaceProvider: ColorProvider get() = ColorProvider(res.onSurface)
    val onSurfaceVariantProvider: ColorProvider get() = ColorProvider(res.onSurfaceVariant)
    val groundProvider: ColorProvider get() = ColorProvider(res.ground)
    val accentProvider: ColorProvider get() = ColorProvider(res.accent)

    /**
     * A Glance provider for a life area's stored hex.
     *
     * Life-area hues are **not** muted: they are categorical, chosen to stay
     * distinguishable side by side in one donut, and muting them toward a common
     * ground is precisely the collapse §4.1's `.tag` rule exists to survive. They
     * are only *lifted* for the dark scheme, which moves lightness and leaves hue
     * alone.
     */
    fun providerFor(hex: String): ColorProvider = when (hex) {
        BuildWidgetTileUseCase.SKIN_ACCENT -> accentProvider
        // A life area's hue is stored per area and cannot be a resource. It is
        // categorical rather than thematic, chosen to stay distinguishable side
        // by side in one donut, so a single value is the right answer here as
        // well as the only available one -- and it sits on top of the ground
        // rather than being read against it.
        else -> parseHex(hex)?.let { ColorProvider(ComposeColor(it)) } ?: accentProvider
    }

    /** The ink a [WidgetCharts] bitmap is drawn with — one answer, on purpose. */
    fun chartInk(): ChartInk = ChartInk(
        // Midway between the two schemes' accents: a bitmap gets one colour, and
        // the honest one is wrong by the same amount either way.
        accent = light.accent.mix(dark.accent, 0.5f),
    )

    companion object {

        /** Both schemes for [skin]. No brightness argument — see the class KDoc. */
        fun of(skin: AppSkin): WidgetPalette = WidgetPalette(
            light = scheme(skin, isDark = false),
            dark = scheme(skin, isDark = true),
            res = SkinColorRes.of(skin),
        )

        /**
         * The computed schemes, exposed so `WidgetPaletteResourceTest` can assert
         * that `values/` and `values-night/` `widget_colors.xml` still equals this arithmetic. The
         * resources are a projection of these values; the test is what stops the
         * projection drifting away from the thing it projects.
         */
        fun computed(skin: AppSkin, isDark: Boolean): NeoScheme = scheme(skin, isDark)

        private fun scheme(skin: AppSkin, isDark: Boolean): NeoScheme {
            val primary = colorSchemeFor(skin, isDark).primary.toArgb()

            // Neo's ground is a near-neutral tinted TOWARD the skin rather than a
            // grey: that tint is the whole of how a skin reaches a material whose
            // surfaces are otherwise flat.
            val ground =
                if (isDark) NEO_DARK_GROUND.mix(primary, 0.10f)
                else NEO_LIGHT_GROUND.mix(primary, 0.06f)

            return NeoScheme(
                ground = ground,
                onSurface = if (isDark) NEO_DARK_ON else NEO_LIGHT_ON,
                onSurfaceVariant = if (isDark) NEO_DARK_ON.alpha(0.62f) else NEO_LIGHT_ON.alpha(0.60f),
                accent = primary.mute(isDark),
            )
        }

        /** Neo's light page. Warm rather than pure white — a flat white ground makes an extrusion read as a cut-out. */
        private val NEO_LIGHT_GROUND = 0xFFEFF1F5.toInt()
        private val NEO_LIGHT_ON = 0xFF16202B.toInt()

        /** The charcoal §4.1 names for the dark scheme. */
        private val NEO_DARK_GROUND = 0xFF0C1520.toInt()
        private val NEO_DARK_ON = 0xFFEAF0F6.toInt()

        @ColorInt
        /**
         * `#RRGGBB` or `#AARRGGBB`, parsed without `android.graphics.Color` so the
         * whole palette stays JVM-testable. Unparseable input is null rather than
         * an exception: the hex comes from a user-editable life area.
         */
        fun parseHex(hex: String): Int? {
            val body = hex.removePrefix("#")
            val value = body.toLongOrNull(16) ?: return null
            return when (body.length) {
                6 -> (0xFF000000L or value).toInt()
                8 -> value.toInt()
                else -> null
            }
        }
    }
}

/**
 * The four colour resources one skin resolves to.
 *
 * A skin cannot be a resource *qualifier* — it is a runtime preference, not a
 * device configuration — so the skin picks the resource and the configuration
 * picks its value. Two axes, each handled by whatever can actually see it.
 */
data class SkinColorRes(
    val ground: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val accent: Int,
) {
    companion object {
        fun of(skin: AppSkin): SkinColorRes = when (skin) {
            AppSkin.AURORA -> SkinColorRes(
                ground = R.color.gp_widget_aurora_ground,
                onSurface = R.color.gp_widget_aurora_on_surface,
                onSurfaceVariant = R.color.gp_widget_aurora_on_surface_variant,
                accent = R.color.gp_widget_aurora_accent,
            )

            AppSkin.BLOSSOM -> SkinColorRes(
                ground = R.color.gp_widget_blossom_ground,
                onSurface = R.color.gp_widget_blossom_on_surface,
                onSurfaceVariant = R.color.gp_widget_blossom_on_surface_variant,
                accent = R.color.gp_widget_blossom_accent,
            )
        }
    }
}

/** One brightness of the neo material, already wearing the skin. */
data class NeoScheme(
    @ColorInt val ground: Int,
    @ColorInt val onSurface: Int,
    @ColorInt val onSurfaceVariant: Int,
    @ColorInt val accent: Int,
)

/**
 * The ink a chart bitmap is drawn with — **deliberately theme-neutral**.
 *
 * A `RemoteViews` can carry two colours for a text or a background and let the
 * launcher pick between them; it cannot carry two bitmaps. So rather than bake a
 * scheme into the pixels and be wrong half the time, the charts are drawn on a
 * **transparent** ground in ink that reads against both: neutrals defined by
 * *alpha* rather than by lightness, so they darken a light tile and lighten a
 * dark one by the same amount.
 *
 * It costs a little contrast against each individual ground and buys a chart
 * that is never wrong — which, on a surface nobody is watching when it
 * refreshes, is the better trade. Life-area hues pass through unchanged: the
 * categorical palette already stays distinguishable, and it sits *on top of*
 * these neutrals rather than being blended into them.
 */
data class ChartInk(
    @ColorInt val accent: Int,
) {
    /** The inset track. A ~14% grey reads as a groove on white and on charcoal alike. */
    @ColorInt val groove: Int = 0x24808080

    /** `--edge`: the hairline contrast anchor §4.1 requires on every control. */
    @ColorInt val edge: Int = 0x59808080

    /** Top-left of neo's shadow pair. */
    @ColorInt val highlight: Int = 0x4DFFFFFF

    /** Bottom-right of the pair. */
    @ColorInt val shadow: Int = 0x38000000

    /** Resolves a stored hex, or the skin accent for the level ring's sentinel. */
    @ColorInt
    fun resolve(hex: String): Int = when (hex) {
        BuildWidgetTileUseCase.SKIN_ACCENT -> accent
        else -> WidgetPalette.parseHex(hex) ?: accent
    }
}

// ── colour arithmetic ────────────────────────────────────────────
//
// Pure Kotlin, deliberately: `android.graphics.Color` is an unmocked stub in a
// JVM unit test, so an implementation that used it could not be verified without
// Robolectric — and this is exactly the arithmetic that must be verified, since
// `widget_colors.xml` is a hand-written projection of it and §4.1's whole point
// is that a palette which no material actually reads still looks correct in
// source. `WidgetPaletteResourceTest` runs every line below on the JVM.

private fun alphaOf(c: Int) = (c ushr 24) and 0xFF
private fun redOf(c: Int) = (c ushr 16) and 0xFF
private fun greenOf(c: Int) = (c ushr 8) and 0xFF
private fun blueOf(c: Int) = c and 0xFF

private fun argbOf(a: Int, r: Int, g: Int, b: Int): Int =
    (a.coerceIn(0, 255) shl 24) or
        (r.coerceIn(0, 255) shl 16) or
        (g.coerceIn(0, 255) shl 8) or
        b.coerceIn(0, 255)

/** Blend [other] into this colour by [amount] (0f..1f), preserving alpha. */
@ColorInt
internal fun Int.mix(@ColorInt other: Int, amount: Float): Int {
    val a = amount.coerceIn(0f, 1f)
    fun ch(from: Int, to: Int) = (from + (to - from) * a).roundToInt()
    return argbOf(
        alphaOf(this),
        ch(redOf(this), redOf(other)),
        ch(greenOf(this), greenOf(other)),
        ch(blueOf(this), blueOf(other)),
    )
}

@ColorInt
internal fun Int.alpha(fraction: Float): Int =
    argbOf((fraction.coerceIn(0f, 1f) * 255).roundToInt(), redOf(this), greenOf(this), blueOf(this))

/**
 * Neo's palette transform (§4.1): pull saturation down and lightness toward the
 * material's own range, so an accent sits *in* the surface instead of on it. Hue
 * is untouched — that is what still makes Aurora look like Aurora and Blossom
 * look like Blossom after the transform, and it is why this is a *transform*
 * rather than a second hand-authored palette per material.
 */
@ColorInt
internal fun Int.mute(isDark: Boolean): Int = withHsv { h, s, v ->
    Triple(
        h,
        s * if (isDark) 0.80f else 0.72f,
        if (isDark) v.coerceIn(0.55f, 0.86f) else v.coerceIn(0.30f, 0.62f),
    )
}

/**
 * Lifts a categorical hue for a dark ground.
 *
 * The life-area palette's ten hexes all clear 4.5:1 against a *light* surface —
 * stated where they are defined, and why `String.toGoalAccent()` exists in the
 * app. The lift has to be a **lightness** move: raising saturation instead would
 * pull the ten hues toward each other, which is the collapse §4.1's `.tag` rule
 * exists to survive.
 */
@ColorInt
internal fun Int.lift(): Int = withHsv { h, s, v ->
    Triple(h, (s * 0.88f).coerceAtMost(0.82f), v.coerceAtLeast(0.78f))
}

private inline fun Int.withHsv(transform: (Float, Float, Float) -> Triple<Float, Float, Float>): Int {
    val (h, s, v) = rgbToHsv(redOf(this), greenOf(this), blueOf(this))
    val (h2, s2, v2) = transform(h, s, v)
    val (r, g, b) = hsvToRgb(h2, s2.coerceIn(0f, 1f), v2.coerceIn(0f, 1f))
    return argbOf(alphaOf(this), r, g, b)
}

/** Hue in degrees 0..360, saturation and value 0f..1f — the standard conversion. */
private fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / delta) % 6f)
        max == gf -> 60f * (((bf - rf) / delta) + 2f)
        else -> 60f * (((rf - gf) / delta) + 4f)
    }
    return Triple(if (h < 0f) h + 360f else h, if (max == 0f) 0f else delta / max, max)
}

private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
    val c = v * s
    val hh = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - kotlin.math.abs((hh % 2f) - 1f))
    val (r1, g1, b1) = when (hh.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = v - c
    return Triple(((r1 + m) * 255).roundToInt(), ((g1 + m) * 255).roundToInt(), ((b1 + m) * 255).roundToInt())
}
