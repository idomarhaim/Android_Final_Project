package com.idomarhaim.goalpilot.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt

/** Parses a "#RRGGBB" / "#AARRGGBB" hex string into a Compose [Color], or [fallback]. */
fun String.toComposeColor(fallback: Color = Color(0xFF718096)): Color =
    runCatching { Color(this.toColorInt()) }.getOrDefault(fallback)

/**
 * A goal's stored accent, adapted to the surface it will be drawn on.
 *
 * Goal colours are persisted hex chosen for a *light* background and then used as
 * a text colour ("72 %", the bar fill, the category icon). On a dark surface a
 * tone-40 hex like `#5145CD` lands around 2.8:1 — unreadable. Raising lightness
 * to a fixed tone keeps the hue (so a goal is still recognisably "the purple
 * one") while restoring contrast.
 *
 * Light mode returns the stored colour untouched.
 */
@Composable
@ReadOnlyComposable
fun String.toGoalAccent(fallback: Color = MaterialTheme.colorScheme.primary): Color {
    val base = toComposeColor(fallback)
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDarkSurface) base.atLightness(DARK_SURFACE_LIGHTNESS) else base
}

private const val DARK_SURFACE_LIGHTNESS = 0.72f

private fun Color.atLightness(lightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}
