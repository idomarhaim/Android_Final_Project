package com.idomarhaim.goalpilot.ui.widget

import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color as ComposeColor
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetTileUseCase
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import kotlin.math.roundToInt

/**
 * The widget pack's colours — **neo**, wearing the selected [AppSkin].
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
 * under it, *"so neo's hand-drawn shadow pair is the only one a widget can
 * approximate."* The issue asked this ticket to decide; this is the decision,
 * and it is on the record rather than implicit in the code.
 *
 * Dark neo is *not* a second entry here. §4.1 makes it **brightness-locked** —
 * it has no light scheme — and a home screen follows the device's own light and
 * dark switch with nobody to tell it otherwise, so a brightness-locked material
 * would silently ignore that switch. Neo answers both.
 *
 * ## The skin has to actually arrive
 *
 * §4.1 carries an enforcement note found the hard way: *a skin picker which no
 * material reads is a control that does nothing, and it looks correct in
 * source.* `C24`'s prototype had exactly that defect in all four materials until
 * Ido asked to see Blossom. So every colour below is derived from
 * [colorSchemeFor] — the same function the app's own theme uses, so there is one
 * palette in this codebase rather than two that agree until one is edited.
 *
 * The neo *transform* on top of it is `mute` (§4.1's table): neo's hues are
 * muted, its depth comes from a shadow pair rather than saturation, and an
 * un-muted accent on an extruded surface reads as a sticker.
 */
data class WidgetPalette(
    /** The tile itself. Neo's surface **is** the page colour plus a shadow pair. */
    @ColorInt val ground: Int,
    /** The inset track a bar or ring sits in — neo's groove. */
    @ColorInt val groove: Int,
    @ColorInt val onSurface: Int,
    @ColorInt val onSurfaceVariant: Int,
    /** The skin's own accent, muted. Used where the subject is not a life area. */
    @ColorInt val accent: Int,
    /**
     * `--edge`: the hairline contrast anchor §4.1 requires on every control,
     * because *no affordance is ever shadow-only* — neo's known WCAG failure.
     */
    @ColorInt val edge: Int,
    /** Top-left highlight of the shadow pair. */
    @ColorInt val highlight: Int,
    /** Bottom-right shadow of the pair. */
    @ColorInt val shadow: Int,
    val isDark: Boolean,
) {
    val onSurfaceProvider: ColorProvider get() = ColorProvider(ComposeColor(onSurface))
    val onSurfaceVariantProvider: ColorProvider get() = ColorProvider(ComposeColor(onSurfaceVariant))
    val groundProvider: ColorProvider get() = ColorProvider(ComposeColor(ground))
    val accentProvider: ColorProvider get() = ColorProvider(ComposeColor(accent))

    /** A Glance provider for a life area's stored hex, already resolved by [resolve]. */
    fun providerFor(hex: String): ColorProvider = ColorProvider(ComposeColor(resolve(hex)))

    /**
     * Resolves a life area's stored hex, or the skin accent for
     * [BuildWidgetTileUseCase.SKIN_ACCENT].
     *
     * Life-area hues are **not** muted: they are categorical, chosen to stay
     * distinguishable side by side in one donut, and muting them toward a common
     * ground is precisely what would collapse them into one ramp — the failure
     * §4.1's `.tag` rule exists to survive. They are lifted for a dark ground
     * instead, which changes lightness and leaves hue alone.
     */
    @ColorInt
    fun resolve(hex: String): Int = when {
        hex == BuildWidgetTileUseCase.SKIN_ACCENT -> accent
        else -> parseHex(hex)?.let { if (isDark) it.lift() else it } ?: accent
    }

    companion object {

        fun of(skin: AppSkin, isDark: Boolean): WidgetPalette {
            val scheme = colorSchemeFor(skin, isDark)
            val primary = scheme.primary.toArgb()

            // Neo's ground is a near-neutral tinted TOWARD the skin rather than a
            // grey: that tint is the whole of how a skin reaches a material whose
            // surfaces are otherwise flat, and a grey ground is what made the
            // picker look broken in C24's prototype.
            val ground = if (isDark) {
                NEO_DARK_GROUND.mix(primary, 0.10f)
            } else {
                NEO_LIGHT_GROUND.mix(primary, 0.06f)
            }

            return WidgetPalette(
                ground = ground,
                groove = if (isDark) ground.darken(0.30f) else ground.darken(0.075f),
                onSurface = if (isDark) NEO_DARK_ON else NEO_LIGHT_ON,
                onSurfaceVariant = if (isDark) NEO_DARK_ON.alpha(0.62f) else NEO_LIGHT_ON.alpha(0.60f),
                accent = primary.mute(isDark),
                // A hairline that is a mix of the ink rather than a fixed grey, so
                // it keeps its contrast under every skin and both brightnesses.
                edge = if (isDark) NEO_DARK_ON.alpha(0.22f) else NEO_LIGHT_ON.alpha(0.16f),
                highlight = if (isDark) AndroidColor.WHITE.alpha(0.06f) else AndroidColor.WHITE.alpha(0.90f),
                shadow = if (isDark) AndroidColor.BLACK.alpha(0.55f) else AndroidColor.BLACK.alpha(0.16f),
                isDark = isDark,
            )
        }

        /** Neo's light page. Warm rather than pure white — a flat white ground makes an extrusion read as a cut-out. */
        private val NEO_LIGHT_GROUND = 0xFFEFF1F5.toInt()
        private val NEO_LIGHT_ON = 0xFF16202B.toInt()

        /** The charcoal §4.1 names for the dark scheme. */
        private val NEO_DARK_GROUND = 0xFF0C1520.toInt()
        private val NEO_DARK_ON = 0xFFEAF0F6.toInt()

        @ColorInt
        fun parseHex(hex: String): Int? = runCatching { AndroidColor.parseColor(hex) }.getOrNull()
    }
}

// ── colour arithmetic, kept here so nothing else has to know it ──

/** Blend [other] into this colour by [amount] (0f..1f), preserving alpha. */
@ColorInt
internal fun Int.mix(@ColorInt other: Int, amount: Float): Int {
    val a = amount.coerceIn(0f, 1f)
    fun ch(from: Int, to: Int) = (from + (to - from) * a).roundToInt().coerceIn(0, 255)
    return AndroidColor.argb(
        AndroidColor.alpha(this),
        ch(AndroidColor.red(this), AndroidColor.red(other)),
        ch(AndroidColor.green(this), AndroidColor.green(other)),
        ch(AndroidColor.blue(this), AndroidColor.blue(other)),
    )
}

@ColorInt
internal fun Int.lighten(amount: Float): Int = mix(AndroidColor.WHITE, amount)

@ColorInt
internal fun Int.darken(amount: Float): Int = mix(AndroidColor.BLACK, amount)

@ColorInt
internal fun Int.alpha(fraction: Float): Int =
    AndroidColor.argb((fraction.coerceIn(0f, 1f) * 255).roundToInt(), AndroidColor.red(this), AndroidColor.green(this), AndroidColor.blue(this))

/**
 * Neo's palette transform (§4.1): pull saturation down and lightness toward the
 * material's own range, so an accent sits *in* the surface instead of on it.
 *
 * Hue is untouched — that is what still makes Aurora look like Aurora and
 * Blossom look like Blossom after the transform, which is the whole point of a
 * transform rather than a second hand-authored palette per material.
 */
@ColorInt
internal fun Int.mute(isDark: Boolean): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this, hsv)
    hsv[1] = hsv[1] * if (isDark) 0.80f else 0.72f
    hsv[2] = if (isDark) hsv[2].coerceIn(0.55f, 0.86f) else hsv[2].coerceIn(0.30f, 0.62f)
    return AndroidColor.HSVToColor(AndroidColor.alpha(this), hsv)
}

/**
 * Lifts a categorical hue for a dark ground.
 *
 * The life-area palette's ten hexes all clear 4.5:1 against a *light* surface —
 * that is stated where they are defined, and it is why `String.toGoalAccent()`
 * exists in the app. A widget on a dark home screen needs the same lift, and it
 * has to be a **lightness** move: raising saturation instead would pull the ten
 * hues toward each other, which is exactly the collapse §4.1's `.tag` rule
 * exists to survive.
 */
@ColorInt
internal fun Int.lift(): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this, hsv)
    hsv[1] = (hsv[1] * 0.88f).coerceAtMost(0.82f)
    hsv[2] = hsv[2].coerceAtLeast(0.78f)
    return AndroidColor.HSVToColor(AndroidColor.alpha(this), hsv)
}
