package com.idomarhaim.goalpilot.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.PaletteTransform
import com.idomarhaim.goalpilot.ui.theme.LocalAppMaterial
import com.idomarhaim.goalpilot.ui.theme.LocalAppSkin
import com.idomarhaim.goalpilot.ui.theme.atLightness
import com.idomarhaim.goalpilot.ui.theme.hsl
import com.idomarhaim.goalpilot.ui.theme.rampTint

/**
 * Parses a `"#RRGGBB"` / `"#AARRGGBB"` hex string into a Compose [Color], or
 * returns [fallback].
 *
 * ## Why this is hand-rolled and not `String.toColorInt()`
 *
 * `toColorInt` goes through `android.graphics.Color.parseColor`, which **throws
 * on the JVM** — so with it in place every category in this file resolved to
 * [fallback] under a unit test, and every colour came out *identical*. That is a
 * silent failure in the worst direction: a test asserting that two categories are
 * distinguishable would fail, and a test asserting they had *collapsed* would
 * **pass for the wrong reason**. `#53` hit both, one after the other, writing the
 * `.tag` guard.
 *
 * The rest of this file was already pure Kotlin for exactly this reason — see the
 * note at the bottom about `ColorUtils` — and the parser was the one android
 * dependency left in the middle of it. Removing it is what lets `CategoryTagTest`
 * run `categoryFill` itself rather than a JVM-safe copy of it.
 *
 * **Behaviour is the same for everything this app stores.** `parseColor` also
 * accepts a handful of colour *names* (`"red"`), which now fall back instead —
 * and nothing writes one: every colour string here comes from
 * `GoalCategory.defaultColorHex`, `LifeAreaPalette.hexes` or the swatch picker,
 * all of which are `#RRGGBB`.
 */
fun String.toComposeColor(fallback: Color = Color(0xFF718096)): Color {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return fallback
    val value = hex.toLongOrNull(radix = 16) ?: return fallback
    // A six-digit string is opaque; an eight-digit one carries its own alpha.
    // `Color(Long)` reads the low 32 bits as ARGB -- the same overload the default
    // `fallback` literal above uses.
    return Color(if (hex.length == 6) value or ALPHA_OPAQUE else value)
}

/**
 * A goal's stored accent as a **fill** — the donut slice, the bar, the legend
 * dot, the icon tint, the progress ring.
 *
 * Goal colours are persisted hex chosen for a *light* background. On a dark
 * surface a tone-40 hex like `#5145CD` lands around 2.8:1 and stops reading as
 * an object at all, so dark mode needs a lighter twin of the same category.
 *
 * ## Two ways to get that twin, and the authored one wins
 *
 * `#57` a gave every [GoalCategory] a hand-authored [GoalCategory.darkColorHex],
 * built as a set against `#0C1520`. When the stored hex is one of ours, that is
 * what comes back — a category then keeps its *identity* across schemes instead
 * of becoming whatever a per-colour transform happens to produce.
 *
 * Everything else — a life area's colour, a hex the user picked, a goal created
 * before `#57` a and never re-saved — still goes through the fixed-lightness
 * lift below. It is the weaker of the two (measured over the ten categories it
 * gave a minimum pairwise separation of **57.6** against the authored set's
 * **66.2**, because HSL lightness is blind to how differently hues carry it),
 * but it is total, and a persisted column needs a total function.
 *
 * Light mode returns the stored colour untouched.
 *
 * For the places that paint a category as **type** rather than as a shape, use
 * [toGoalInk] — these fills clear the 3:1 non-text floor and deliberately not
 * 4.5:1.
 */
@Composable
@ReadOnlyComposable
fun String.toGoalAccent(fallback: Color = MaterialTheme.colorScheme.primary): Color =
    categoryFill(
        hex = this,
        transform = LocalAppMaterial.current.paletteTransform,
        skin = LocalAppSkin.current,
        darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        fallback = fallback,
    )

/**
 * [toGoalAccent] with the composition read out, so the whole rule is one pure
 * function over four values.
 *
 * Split out for the same reason [asInkOn] is: `ThemePaletteTest` runs the **real**
 * function over the real fourteen schemes on the JVM instead of asserting a copy
 * of it, and a `@Composable` cannot be called from a unit test.
 *
 * ## `#53`: this is where dark neo's collapse actually happens
 *
 * §4.1 says a material rewrites the skin through one of three palette transforms,
 * and that dark neo's — [PaletteTransform.SINGLE_ACCENT_RAMP] — *collapses the six
 * categorical hues into one ramp*. `MaterialPalettes.rampTint` has been the
 * arithmetic for that since `C12` and had **zero call sites**: it was built and
 * unit-tested and deliberately left unwired, because applying a collapse before
 * the words are there installs exactly the identity failure §4.1's `.tag` rule
 * exists to prevent. The words landed in this same commit — `DonutChart` and
 * `StackedColumnChart` now draw them — so the collapse is safe to switch on, and
 * this line is what switches it on.
 *
 * ## Why the ramp is taken from the LIGHT fill and not from the dark twin
 *
 * `rampTint` positions a hue on the ramp by the hue's **own lightness**, and `#57`
 * a authored the ten [GoalCategory.darkColorHex] twins to a deliberately *even*
 * lightness so ten slices in one donut hold together. Feeding the twins in would
 * therefore land every category on nearly the same point of the ramp — a total
 * collapse, not §4.1's. The stored light fill is the category's canonical hue and
 * spreads across the ramp as far as one accent allows, which is the most identity
 * a single-accent material is permitted to keep.
 *
 * The dark-twin lift is skipped under the ramp for the same reason it exists
 * elsewhere: the ramp's two ends are already held in a readable band against dark
 * neo's charcoal by `RAMP_BRIGHT` / `RAMP_DEEP`, so lifting first and ramping
 * after would be one contrast rule undoing another.
 *
 * ## Why it branches on the TRANSFORM and not on the material
 *
 * `MaterialSpec.kt`'s rule is that nothing outside it may hold a `when (material)`
 * that decides how something is drawn. This is not that: it is the palette layer,
 * one axis over, and it reads the transform each material **declares** — the same
 * value `MaterialPalettes.colorSchemeFor` switches on. A fifth material that
 * declares the ramp inherits this by existing, which is the property §4.1 asks for
 * a paragraph after it names the rule.
 */
fun categoryFill(
    hex: String,
    transform: PaletteTransform,
    skin: AppSkin,
    darkSurface: Boolean,
    fallback: Color,
): Color {
    val stored = hex.toComposeColor(fallback)
    if (transform == PaletteTransform.SINGLE_ACCENT_RAMP) return rampTint(stored, skin)
    if (!darkSurface) return stored
    val authored = GoalCategory.darkTwinOf(hex)
    return authored?.toComposeColor(fallback) ?: stored.atLightness(DARK_SURFACE_LIGHTNESS)
}

/**
 * The same accent as **ink** — readable 14 sp text on whatever card it lands on.
 *
 * ## Why this is not just [toGoalAccent]
 *
 * A categorical palette has two jobs and they pull in opposite directions. As a
 * *fill* it wants chroma and an even lightness across the set, so ten slices in
 * one donut hold together; as *type* it wants 4.5:1 against the card, which for
 * ten hues at one lightness forces the whole set so dark it reads as mud. Before
 * `#57` a the app resolved that by making every category dark enough to be text,
 * and paid for it in the chart — the complaint the ticket opens with.
 *
 * So the fill is authored and the ink is **derived**: same hue, same saturation,
 * lightness walked until the contrast clears. Deriving rather than authoring a
 * second table is what keeps it working for life-area colours and user-picked
 * hexes, which have no table.
 *
 * ## It is checked against every tone, not against `surface`
 *
 * The five `surfaceContainer*` steps straddle `surface` in *both* directions, and
 * in dark mode the highest of them (`#48353B` under Blossom) is far lighter than
 * the ground. An ink solved against `surface` alone is therefore too dim exactly
 * where a card is raised, which is where the percentages live. [cardTonesOf]
 * hands the solver the whole ladder and it satisfies the hardest rung.
 */
@Composable
@ReadOnlyComposable
fun String.toGoalInk(fallback: Color = MaterialTheme.colorScheme.primary): Color =
    toGoalAccent(fallback).asInkOn(cardTonesOf(MaterialTheme.colorScheme))

/** The card tones a category accent can be painted on. Public for the palette guard. */
fun cardTonesOf(scheme: ColorScheme): List<Color> = listOf(
    scheme.surface,
    scheme.surfaceContainerLowest,
    scheme.surfaceContainerLow,
    scheme.surfaceContainer,
    scheme.surfaceContainerHigh,
    scheme.surfaceContainerHighest,
)

/**
 * This colour, moved along HSL lightness until it clears [min] against **every**
 * background in [backgrounds].
 *
 * Hue and saturation are held, so the category stays recognisable; only the
 * lightness moves, and only in the one direction the grounds allow. Pure Kotlin
 * and non-composable on purpose — `ThemePaletteTest` runs the real function over
 * the real fourteen schemes on the JVM rather than asserting a copy of it.
 *
 * Falls back to plain black or white if even the endpoint cannot clear [min],
 * which no scheme in this app needs but a future one might.
 */
fun Color.asInkOn(backgrounds: List<Color>, min: Double = MIN_INK_CONTRAST): Color {
    if (backgrounds.isEmpty()) return this
    if (backgrounds.all { contrastRatio(this, it) >= min }) return this

    // Which way to walk is a property of the grounds, not of this colour: on a
    // dark card only a lighter ink can clear, on a light card only a darker one.
    // Averaging the ladder rather than reading `surface` keeps a single outlier
    // rung from flipping the direction for the whole set.
    val towardsWhite = backgrounds.map { it.luminance().toDouble() }.average() < 0.5

    val start = hsl()[2]
    var lo = if (towardsWhite) start else 0f
    var hi = if (towardsWhite) 1f else start
    // 24 halvings resolve HSL lightness far below one 8-bit step; the loop is
    // bounded rather than convergence-tested so it cannot spin on a flat region.
    repeat(INK_SEARCH_STEPS) {
        val mid = (lo + hi) / 2f
        val candidate = atLightness(mid)
        val clears = backgrounds.all { contrastRatio(candidate, it) >= min }
        if (towardsWhite) {
            if (clears) hi = mid else lo = mid
        } else {
            if (clears) lo = mid else hi = mid
        }
    }
    val solved = atLightness(if (towardsWhite) hi else lo)
    if (backgrounds.all { contrastRatio(solved, it) >= min }) return solved
    return if (towardsWhite) Color.White else Color.Black
}

/** WCAG 2.1 relative-luminance contrast, the same arithmetic `ThemePaletteTest` asserts with. */
private fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** WCAG 2.1 for normal-size text. The category percentages are 14 sp, so 3:1 does not apply. */
const val MIN_INK_CONTRAST: Double = 4.5

private const val INK_SEARCH_STEPS = 24
private const val DARK_SURFACE_LIGHTNESS = 0.72f

/** The alpha byte a six-digit hex does not carry. */
private const val ALPHA_OPAQUE = 0xFF000000L

// `hsl()` and `atLightness()` are the pure-Kotlin pair in `ui/theme/MaterialPalettes.kt`.
// androidx.core.graphics.ColorUtils would do the same arithmetic through
// android.graphics.Color, which throws under a JVM unit test -- and the whole point
// of deriving ink rather than authoring it is that ThemePaletteTest can run the real
// function over the real fourteen schemes without an emulator.
