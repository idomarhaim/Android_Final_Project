package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.PaletteTransform
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Spec §4.1's palette transforms — how a **material** rewrites the **skin**.
 *
 * > The material is a **second axis**, not the `AppSkin` the app already has.
 * > `AppSkin` is a **palette**; the material is a **surface**. They do **not**
 * > multiply freely, so each material declares a **palette transform** —
 * > `identity · mute · single-accent ramp` — and the schemes are **generated,
 * > not hand-authored**.
 *
 * ## Generated is the load-bearing word
 *
 * Four materials × two skins × two brightnesses is **sixteen** cells.
 * `Palettes.kt` hand-authors **four** schemes at roughly 40 tokens each, and
 * every one of the sixteen cells comes from them through one of the three
 * transforms below. Counted exactly, because the round number is wrong: glass
 * and liquid glass take `identity`, so their **eight** cells *are* the authored
 * four; the **eight** neo and dark-neo cells are genuinely computed, and they
 * are **six** distinct schemes rather than eight, because dark neo's brightness
 * lock collapses its four into two.
 *
 * A hand-authored sixteen is not a bigger version of the same thing — it is the
 * point at which adding a third skin costs eight new schemes nobody will keep
 * consistent, and §4.1 names the failure that follows: *a skin picker which no
 * material reads is a control that does nothing, and it looks correct in
 * source.*
 *
 * ## What is deliberately NOT transformed
 *
 * - **`error` and `positive`.** They are *semantic*, not brand: a muted error
 *   is a less visible error, and a dark-neo ramp that swallowed `positive`
 *   would leave the app with one colour for *"done"* and *"this is the
 *   accent"*. §4.1 collapses **categorical** hues, never these two.
 * - **The category palette.** Dark neo's ramp *does* collapse the six
 *   `GoalCategory` hues — that is exactly why §4.1's `.tag` rule requires a
 *   category to be **written in words** beside its dot. The collapse is declared
 *   here as [rampTint] and applied by `ui/components/ColorExt.kt`'s
 *   `categoryFill`, which is the one seam every categorical fill in the app
 *   passes through. **`#53` wired it, and only after the words were there** —
 *   `DonutChart` and `StackedColumnChart` draw a band's own name in the same
 *   commit. The order was the whole risk: applying the collapse first installs
 *   the identity failure the `.tag` rule exists to prevent, in every chart at
 *   once, and it looks correct on the three materials it is not for.
 * - **`swatchFor`.** The skin swatch shows what it *would* do, so it takes no
 *   material — the same exception the material picker takes, one axis over.
 */

/** The Material 3 scheme for [skin] rendered in [material], at the requested brightness. */
fun colorSchemeFor(skin: AppSkin, material: AppMaterial, dark: Boolean): ColorScheme {
    // The lock is resolved HERE and nowhere else, so the theme cannot render a
    // brightness the picker says is impossible.
    val resolvedDark = material.resolveDark(dark)
    val base = colorSchemeFor(skin, resolvedDark)
    return when (material.paletteTransform) {
        PaletteTransform.IDENTITY -> base
        PaletteTransform.MUTE -> base.muted(resolvedDark)
        PaletteTransform.SINGLE_ACCENT_RAMP -> base.ramped(skin)
    }
}

/** The off-Material brand accents for [skin] rendered in [material]. */
fun accentsFor(skin: AppSkin, material: AppMaterial, dark: Boolean): GpAccents {
    val resolvedDark = material.resolveDark(dark)
    val base = accentsFor(skin, resolvedDark)
    return when (material.paletteTransform) {
        PaletteTransform.IDENTITY -> base

        // The brand sweep stays the brand sweep; it loses saturation so it does
        // not sit on a flat muted ground as the one saturated object on screen.
        // Darkened in step, because desaturating alone RAISES luminance and
        // onHero is white — the gradient would quietly stop clearing 4.5:1.
        PaletteTransform.MUTE -> base.copy(
            heroGradient = base.heroGradient.map {
                it.saturated(MUTE_SATURATION).scaledLightness(MUTE_HERO_LIGHTNESS)
            },
            positive = base.positive.asAccent(dark = resolvedDark),
        )

        // Dark neo's hero IS the ramp — one saturated gradient is the whole
        // material. onHero therefore flips from white to the ramp's own ink:
        // the ramp is bright by construction, so white on it is about 2.4:1.
        PaletteTransform.SINGLE_ACCENT_RAMP -> {
            val (bright, deep) = rampFor(skin)
            val ink = inkOn(bright)
            base.copy(
                heroGradient = listOf(deep, bright),
                onHero = ink,
                onHeroVariant = ink.copy(alpha = 0.78f),
            )
        }
    }
}

/**
 * Dark neo's two-stop accent, **derived from [skin]** — §4.1's first named
 * consequence, and the one it says gets found late:
 *
 * > Dark neo's accent must derive from the selected skin, or picking Blossom
 * > under dark neo silently renders Aurora and the skin picker stops working
 * > for a quarter of the set.
 *
 * Derived from the skin's own hero sweep rather than picked per skin, so a
 * third skin gets a ramp **by existing**. The two hues are the sweep's *mid*
 * and *start* — adjacent enough to read as one ramp, far enough apart to read
 * as a gradient rather than a fill.
 *
 * @return the bright end first, then the deep end.
 */
fun rampFor(skin: AppSkin): Pair<Color, Color> {
    val sweep = swatchFor(skin)
    val bright = sweep[min(1, sweep.lastIndex)].atHsl(saturation = 0.92f, lightness = RAMP_BRIGHT)
    val deep = sweep.first().atHsl(saturation = 0.86f, lightness = RAMP_DEEP)
    return bright to deep
}

/**
 * Both ends are held above a floor, and the floor is a **contrast** constraint
 * rather than a taste one.
 *
 * The ramp is a *filled* surface — the hero, a primary button — so one ink has
 * to be readable on **every** stop of it, and the ink is dark because the
 * bright end cannot carry white (about 2.4:1). That makes the **deep** end the
 * binding constraint, and lowering it for a moodier gradient is what breaks the
 * pair. `Observed:` at `RAMP_DEEP = 0.46f` the Aurora ramp's deep stop came out
 * at **3.54:1** against its own ink, caught by `ThemePaletteTest`'s
 * *hero gradient carries its on-colour across every stop* — a defect that is
 * invisible in a swatch and invisible in a render where the button happens to
 * be short.
 */
private const val RAMP_BRIGHT = 0.72f
private const val RAMP_DEEP = 0.55f

/**
 * Where a categorical hue lands once dark neo has collapsed the six of them
 * into one ramp — the arithmetic behind §4.1's `.tag` rule.
 *
 * Position on the ramp is the hue's **own lightness**, so two categories that
 * differed only in hue land on top of each other. That is not a defect in this
 * function; it is the fact `.tag` exists to survive: **colour stops carrying
 * identity**, so the word beside the dot has to.
 *
 * Wired by `#53` at `ui/components/ColorExt.kt`'s `categoryFill`, which feeds it
 * the **stored light fill** rather than `#57` a's dark twin — the twins were
 * authored to an even lightness on purpose, and this function reads lightness, so
 * feeding them in would land the whole set on one point of the ramp. See that
 * function's KDoc.
 */
fun rampTint(base: Color, skin: AppSkin): Color {
    val (bright, deep) = rampFor(skin)
    return lerpColor(deep, bright, base.hsl()[2])
}

// ─────────────────────────── mute — neo ────────────────────────────────────

private const val MUTE_SATURATION = 0.50f
private const val MUTE_HERO_LIGHTNESS = 0.85f
private const val MUTE_GROUND_SATURATION = 0.16f
private const val MUTE_GROUND_LIGHT = 0.925f
private const val MUTE_GROUND_DARK = 0.155f

/** The darkest an accent may be in dark mode / the lightest in light mode, after muting. */
private const val MUTE_ACCENT_LIGHT_MAX = 0.38f
private const val MUTE_ACCENT_DARK_MIN = 0.70f

/**
 * *One desaturated accent, one warmed flat ground.*
 *
 * The **ground is the part that matters** and it is not decoration:
 * neumorphism's claim is that depth comes from a shadow pair on **one flat
 * surface**, so every `surfaceContainer*` step collapses to a single tone. A
 * tonal step would draw the boundary the shadow pair is supposed to draw, and
 * the extrusion stops reading.
 *
 * Accents move in the **safe direction only** — darker in light, lighter in
 * dark — so that their own `on*` colours (white in light, near-black in dark)
 * keep the contrast they were authored with. Containers get saturation taken
 * off but their lightness left alone, because a container's `on*` colour is the
 * *opposite* polarity and clamping both would invert the pair.
 */
private fun ColorScheme.muted(dark: Boolean): ColorScheme {
    val ground = surface.atHsl(
        saturation = min(surface.hsl()[1], MUTE_GROUND_SATURATION),
        lightness = if (dark) MUTE_GROUND_DARK else MUTE_GROUND_LIGHT,
    )
    return copy(
        primary = primary.asAccent(dark),
        secondary = secondary.asAccent(dark),
        tertiary = tertiary.asAccent(dark),
        inversePrimary = inversePrimary.asAccent(!dark),
        surfaceTint = primary.asAccent(dark),

        primaryContainer = primaryContainer.saturated(MUTE_SATURATION),
        secondaryContainer = secondaryContainer.saturated(MUTE_SATURATION),
        tertiaryContainer = tertiaryContainer.saturated(MUTE_SATURATION),

        background = ground,
        surface = ground,
        surfaceBright = ground,
        surfaceDim = ground,
        surfaceVariant = ground,
        surfaceContainerLowest = ground,
        surfaceContainerLow = ground,
        surfaceContainer = ground,
        surfaceContainerHigh = ground,
        surfaceContainerHighest = ground,
    )
}

/** Saturation off, and lightness pushed into the band that stays legible on the flat ground. */
private fun Color.asAccent(dark: Boolean): Color {
    val muted = saturated(MUTE_SATURATION)
    val l = muted.hsl()[2]
    return muted.atLightness(
        if (dark) max(l, MUTE_ACCENT_DARK_MIN) else min(l, MUTE_ACCENT_LIGHT_MAX),
    )
}

// ──────────────────── single-accent ramp — dark neo ────────────────────────

private const val RAMP_GROUND_SATURATION = 0.08f
private const val RAMP_GROUND_LIGHTNESS = 0.115f
private const val RAMP_CARD_LIGHTNESS = 0.165f
private const val RAMP_CARD_BRIGHT_LIGHTNESS = 0.205f

/**
 * *One two-stop ramp built from the skin, on charcoal.*
 *
 * `primary`, `secondary` and `tertiary` all become the **same** colour — that
 * is what "one saturated accent" means, and it is why §4.1 pairs this material
 * with the `.tag` rule rather than leaving it implicit. The containers become
 * the charcoal card so a filled chip still reads as an object.
 *
 * Always applied on top of the **dark** base scheme; [AppMaterial.resolveDark]
 * has already forced the brightness by the time this is called.
 */
private fun ColorScheme.ramped(skin: AppSkin): ColorScheme {
    val (bright, deep) = rampFor(skin)
    val ink = inkOn(bright)
    val hue = surface.hsl()[0]
    val ground = hslColor(hue, RAMP_GROUND_SATURATION, RAMP_GROUND_LIGHTNESS)
    val card = hslColor(hue, RAMP_GROUND_SATURATION, RAMP_CARD_LIGHTNESS)
    val cardBright = hslColor(hue, RAMP_GROUND_SATURATION, RAMP_CARD_BRIGHT_LIGHTNESS)
    return copy(
        primary = bright,
        onPrimary = ink,
        secondary = bright,
        onSecondary = ink,
        tertiary = bright,
        onTertiary = ink,
        inversePrimary = deep,
        surfaceTint = bright,

        primaryContainer = card,
        onPrimaryContainer = bright,
        secondaryContainer = card,
        onSecondaryContainer = bright,
        tertiaryContainer = card,
        onTertiaryContainer = bright,

        background = ground,
        surface = ground,
        surfaceDim = ground,
        surfaceBright = cardBright,
        surfaceVariant = card,
        surfaceContainerLowest = ground,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = cardBright,
    )
}

/** Near-black or near-white, whichever the ramp end can carry — never a mid grey. */
private fun inkOn(accent: Color): Color =
    if (accent.hsl()[2] > 0.42f) hslColor(accent.hsl()[0], 0.62f, 0.06f) else Color.White

// ───────────────────────────── colour maths ────────────────────────────────
//
// Pure Kotlin on purpose. androidx.core.graphics.ColorUtils would do the same
// arithmetic and would drag the whole matrix onto an emulator: Color and
// luminance() are JVM types, so every transform above is unit-testable exactly
// as ThemePaletteTest already tests the hand-authored four.

/** `[hue 0..360, saturation 0..1, lightness 0..1]`. */
internal fun Color.hsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val l = (maxC + minC) / 2f
    if (delta == 0f) return floatArrayOf(0f, 0f, l)
    val s = delta / (1f - abs(2f * l - 1f))
    val h = when (maxC) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return floatArrayOf(if (h < 0f) h + 360f else h, s.coerceIn(0f, 1f), l)
}

internal fun hslColor(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val hp = ((hue % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs((hp % 2f) - 1f))
    val (r1, g1, b1) = when (hp.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = lightness - c / 2f
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/** Same hue and lightness, saturation scaled by [factor]. */
internal fun Color.saturated(factor: Float): Color {
    val h = hsl()
    return hslColor(h[0], (h[1] * factor).coerceIn(0f, 1f), h[2], alpha)
}

/** Same hue and saturation, at an absolute [lightness]. */
internal fun Color.atLightness(lightness: Float): Color {
    val h = hsl()
    return hslColor(h[0], h[1], lightness.coerceIn(0f, 1f), alpha)
}

/** Same hue, lightness scaled by [factor] — keeps a dark colour dark. */
internal fun Color.scaledLightness(factor: Float): Color = atLightness(hsl()[2] * factor)

/** Same hue, at absolute saturation and lightness. */
internal fun Color.atHsl(saturation: Float, lightness: Float): Color =
    hslColor(hsl()[0], saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f), alpha)

internal fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = from.alpha + (to.alpha - from.alpha) * f,
    )
}
