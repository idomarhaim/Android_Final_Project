package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRelief
import kotlin.math.sqrt

/**
 * Spec §4.1's **material contract** — the four answers every screen is allowed
 * to ask for.
 *
 * > **No screen may depend on a property a single material has.** Translucency
 * > exists in glass and liquid only; a shadow-pair extrusion exists in neo and
 * > dark neo only. **A screen specifies `surface · groove · elevation ·
 * > accent`, and each material answers those four its own way** — otherwise
 * > every design has to be drawn four times, which is the cost that would make
 * > four materials unaffordable.
 *
 * So this type is the *whole* vocabulary. A screen that reaches past it — for a
 * blur radius, for a rim colour, for "is this neo?" — has re-introduced exactly
 * the four-times cost, and the giveaway in review is a `when (material)` that
 * decides **how something is drawn**, outside this file.
 *
 * The qualifier is load-bearing rather than a hedge: `ComponentStrings.kt`
 * branches on `AppMaterial` too, to map each one to its `R.string`, and that is
 * correct — it is the same split `AppSkin` takes, identity in `domain/`, words
 * in `res/`. What the rule forbids is a **second place that knows what a
 * material looks like**.
 *
 * ## The three rules every screen inherits, and where each one lives here
 *
 * | Rule | Here |
 * |---|---|
 * | **`--edge`** — every control carries a hairline contrast anchor; no affordance is ever shadow-only (neo's known WCAG failure) | [edge] is **not nullable**, and [gpSurface] always strokes it |
 * | **an overlay declares its own opacity** — a sheet, dialog or menu that inherits neo's surface *is* the page colour plus a shadow pair, so the dimmed screen reads straight through it | [overlay] is always opaque, and is a different token from [surface] |
 * | **`.tag`** — a category is written in words beside its dot, because dark neo collapses six hues into one ramp | authoring, not a token: the arithmetic is `MaterialPalettes.rampTint`, applied at `ui/components/ColorExt.kt`'s `categoryFill`, and the words are drawn by `DonutChart` and `StackedColumnChart` (`#53`) |
 *
 * ## The cost §4.1 already priced, paid in Compose
 *
 * > `backdrop-filter`, SVG filters and CSS shadow pairs are **web** primitives.
 * > In Compose the equivalents are `Modifier.blur`, `RenderEffect` (API 31+,
 * > with a fallback below), and hand-drawn `Canvas` shadows — and **a widget
 * > has none of them**.
 *
 * Two consequences are visible in this file and both are deliberate:
 *
 * - **No backdrop blur.** Compose has no backdrop filter, and `Modifier.blur`
 *   blurs a composable's *own* content, not what is behind it. Glass and liquid
 *   glass are therefore drawn as **translucent panels over a gradient
 *   backdrop** ([backdrop]) rather than as blurred ones. The reading survives —
 *   the page shows through the panel — and the blur does not.
 * - **The shadow pair is stacked, not blurred.** `BlurMaskFilter` is
 *   unsupported on a hardware canvas below API 28 and this app is `minSdk 26`,
 *   so [drawShadowPair] sums translucent rounded rects into a linear falloff.
 *   It costs six draws and works on every device the app ships to.
 */
@Immutable
data class GpMaterialSpec(

    val material: AppMaterial,

    // ── surface — the fill of a raised panel: card, sheet, button face ──────
    /** The panel fill. May be **translucent**, so it must be drawn over [backdrop]. */
    val surface: Color,
    /** The panel's second gradient stop. Equal to [surface] where the fill is flat. */
    val surfaceEnd: Color,

    // ── groove — the fill of a recessed track: something you READ ───────────
    /**
     * Neo inverts the shadow pair here rather than changing the fill: *"read,
     * not pressed"*. A status line and a 24 h track are things you read, so they
     * are recessed; a segmented control is a thing you press, so it stays
     * raised.
     */
    val groove: Color,

    // ── elevation ──────────────────────────────────────────────────────────
    /** Material 3 tonal elevation. **Zero** wherever depth is drawn by hand. */
    val elevation: Dp,
    /** The hand-drawn pair, or `null` where depth is not extruded at all. */
    val shadow: GpShadowPair?,
    /** The translucent highlight stack, or `null` where the surface is opaque. */
    val gloss: GpGloss?,

    // ── accent ─────────────────────────────────────────────────────────────
    /** What a primary action is painted with. */
    val accent: Color,
    val onAccent: Color,
    /**
     * The accent as a sweep. One stop for the three materials whose accent is a
     * colour; **two** for dark neo, whose accent *is* a gradient.
     */
    val accentStops: List<Color>,

    // ── the rules every screen inherits ────────────────────────────────────
    /** `--edge`. Never transparent — that is the rule, not a default. */
    val edge: Color,
    val edgeWidth: Dp,
    /** `C22`'s overlay rule. Always opaque, and never the same token as [surface]. */
    val overlay: Color,

    // ── shape and ground ───────────────────────────────────────────────────
    val corner: Dp,
    /**
     * The page behind everything — **the third axis**, [AppBackground],
     * resolved into something drawable.
     *
     * It used to be `List<Color>?`, a hue list owned by the material, with
     * `null` meaning *flat*. `#57` b made the ground a **user choice**, so it
     * is no longer a property of the material at all: [materialSpecFor] takes
     * the selected [AppBackground] and puts the answer here.
     *
     * It stays **inside the spec** rather than being handed to screens
     * separately, and that is the contract holding: `Modifier.gpPage` is the
     * only reader, at its two call sites. A background a screen had to know
     * about individually would re-open the draw-it-four-times cost — now
     * draw-it-twelve-times.
     */
    val backdrop: GpBackdrop,

    /**
     * How a **chart body** is built — the fifth answer, and the **fourth axis**,
     * [AppRelief], resolved into something drawable.
     *
     * It sits inside the spec for exactly the reason [backdrop] does: a chart
     * that had to know which material it was in would re-open the
     * draw-it-four-times cost this type exists to close — now
     * draw-it-sixteen-times, because raised multiplies it again. The four chart
     * components ask for this and draw; none of them names a material.
     *
     * See [GpChartVolume] for the one prototype layer that is deliberately not
     * ported, and whose authority for dropping it is the prototype itself.
     */
    val volume: GpChartVolume,
) {
    /** Whether this material's panels let the page through — glass and liquid only. */
    val isTranslucent: Boolean get() = surface.alpha < 1f
}

/** One neumorphic pair, or a plain drop when [light] is transparent. */
@Immutable
data class GpShadowPair(
    val dark: Color,
    val darkOffset: DpOffset,
    val light: Color,
    val lightOffset: DpOffset,
    val blur: Dp,
)

/**
 * The translucent layers drawn *over* a panel fill.
 *
 * `tintFloor` is the one that was earned by a defect rather than by taste —
 * §4.1:
 *
 * > A translucent surface must tint **toward** the theme, not only add
 * > highlight. A dark-theme glass panel built from white layers alone can only
 * > **lighten** what is behind it, so wherever a background hue peaks, the
 * > panel's own white type goes illegible — its contrast is a property of the
 * > **wallpaper**, not of the component.
 */
@Immutable
data class GpGloss(
    /** The bright inner rim along the top edge. */
    val topRim: Color,
    /** The dim outer counter-rim underneath. `Transparent` where there is none. */
    val bottomRim: Color,
    /** One specular streak across the upper third. `Transparent` for glassmorphism. */
    val specular: Color,
    /** The low-alpha floor that makes contrast a property of the panel, not the page. */
    val tintFloor: Color,
)

/**
 * One radial light on the page, positioned and sized as a **fraction** of the
 * page rather than in `dp`.
 *
 * Fractions and not pixels because the ground has to read the same on a phone,
 * a foldable and an 86 dp picker tile — the prototype's `700px 460px at 8% -6%`
 * is authored against one 392 px frame, and pasting those numbers would put the
 * whole ground off-screen inside a tile. The proportions survive the port; the
 * units do not.
 */
@Immutable
data class GpGlow(
    val hue: Color,
    /** Centre, as a fraction of page width. May sit outside `0..1`. */
    val x: Float,
    /** Centre, as a fraction of page height. May sit outside `0..1`. */
    val y: Float,
    /** Radius at which the light reaches transparent, as a fraction of the page's min dimension. */
    val radius: Float,
)

/**
 * A ground — [AppBackground] resolved into a base wash plus its lights.
 *
 * ## What was lost in the port, said rather than hidden
 *
 * The prototype's grounds are **web primitives**: layered CSS
 * `radial-gradient`s over a `linear-gradient`, under panels carrying
 * `backdrop-filter: blur(22px) saturate(1.7)`. Three things did not survive,
 * and each is a deliberate choice rather than an omission:
 *
 * 1. **No backdrop blur**, as [GpMaterialSpec]'s header already records —
 *    Compose has no backdrop filter and `Modifier.blur` blurs a composable's
 *    *own* content. Panels are translucent over this ground, not blurred over
 *    it. The *reading* survives; the softness does not.
 * 2. **The lights are circles, not ellipses.** CSS gives each radial an x and
 *    a y radius (`700px 460px`); `DrawScope.drawCircle` takes one. Matching
 *    would mean a scaled layer per light, six draws instead of three, for a
 *    difference visible only where two lights overlap.
 * 3. **The prototype's own hexes are not here at all**, and that is `#57` a's
 *    finding one axis down: every hue is read off the [ColorScheme], so the
 *    ground tracks the skin *and* the material's palette transform. Concretely
 *    — under neo the lights are muted with everything else, and under dark neo
 *    they collapse onto the accent ramp, so *"exactly one saturated gradient"*
 *    survives a lit ground. Hard-coded `#4E6BFF` would have broken both.
 *
 * The one place that costs something real is [AppBackground.SPECTRUM]: liquid
 * glass's ground has a **fourth, warm** light (`#FFB25C`) and the scheme has no
 * warm role to take it from. Inventing one would be either a hard-coded hex
 * (rejected above) or a hue rotation that puts a second saturated colour on
 * dark neo's page and kills the one thing that material is. So the fourth light
 * repeats `primary`, smaller and dimmer, and what distinguishes SPECTRUM from
 * [AppBackground.GLOW] is **density — four tight lights against three wide
 * ones — not an extra hue.**
 */
@Immutable
data class GpBackdrop(
    /**
     * The diagonal wash under the lights. **Empty** means *the scheme's own
     * flat background*, which is what [AppBackground.PLAIN] is.
     */
    val base: List<Color>,
    /** The lights over [base]. **Empty** for a plain ground. */
    val glows: List<GpGlow>,
) {
    /** Whether this ground is a single flat tone — `true` only for [AppBackground.PLAIN]. */
    val isPlain: Boolean get() = base.isEmpty() && glows.isEmpty()

    /**
     * The colour this ground actually paints at `(x, y)`, both fractions of the
     * page, over an opaque [background].
     *
     * ## Why this exists rather than the test reading pixels
     *
     * A lit ground is the one thing in the theme whose **contrast is a property
     * of the page rather than of the component** — `GpGloss.tintFloor`'s doc
     * already names that as a defect class, and `#57` b makes it reachable for
     * two more materials by letting soft surfaces sit on a lit ground at all. So
     * it has to be assertable, and `ThemePaletteTest` runs on the JVM where
     * there is no canvas.
     *
     * ⚠️ **This is a MODEL of [gpPage], not [gpPage] itself, and the distinction
     * is the honest limit of the guard built on it.** The arithmetic is exact
     * — a two-stop `Brush.radialGradient` interpolates linearly from the hue to
     * transparent across its radius, and a two-stop `Brush.linearGradient` does
     * the same along its axis, which is what the two loops below compute — but
     * it is *restated* here rather than shared, because [gpPage] hands brushes
     * to Compose and never evaluates a colour itself. **What would make the two
     * drift:** a third gradient stop, a non-linear tile mode, a blend mode other
     * than the default `SrcOver`, or a change to the wash's start/end offsets.
     * Any of those has to be made in both places, and the test would keep
     * passing while the page changed. Same shape as `ThemePaletteTest`'s note
     * that it runs the *real* `asInkOn` — this one cannot, and says so.
     *
     * @param aspect page height ÷ width, so the radial falloff is measured in
     *   real distance rather than in fractions of two different axes.
     */
    fun colorAt(x: Float, y: Float, background: Color, aspect: Float): Color {
        if (isPlain) return background
        // 1 · the diagonal wash, which is opaque and covers everything.
        var current = when {
            base.isEmpty() -> background
            base.size == 1 -> base.first()
            else -> {
                // gpPage runs the wash from (0.18w, 0) to (0.82w, h); project
                // the sample onto that axis and clamp, exactly as a linear
                // gradient's default `TileMode.Clamp` does.
                val dx = 0.64f
                val dy = aspect
                val t = (((x - 0.18f) * dx + y * aspect * dy) / (dx * dx + dy * dy))
                    .coerceIn(0f, 1f)
                lerpColor(base.first(), base.last(), t)
            }
        }
        // 2 · each light in turn, source-over, nearest-first is not a thing --
        // gpPage draws them in list order and so does this.
        glows.forEach { glow ->
            val dx = x - glow.x
            val dy = (y - glow.y) * aspect
            val distance = sqrt(dx * dx + dy * dy)
            if (distance >= glow.radius) return@forEach
            val alpha = glow.hue.alpha * (1f - distance / glow.radius)
            current = glow.hue.copy(alpha = alpha).over(current)
        }
        return current
    }

    companion object {
        /** One flat tone: the ground neo and dark neo were designed for. */
        val Plain = GpBackdrop(base = emptyList(), glows = emptyList())
    }
}

/**
 * The four answers, for [material] on [background] against an
 * already-transformed [scheme].
 *
 * Reads the scheme rather than the raw palette, so the material's *surface*
 * decisions sit on top of its own *palette* transform — the axes compose here
 * and nowhere else.
 *
 * [background] arrives resolved-or-not: [AppBackground.MATCH] is resolved
 * against [material] inside, through [AppBackground.resolve], so no caller has
 * to know the mapping.
 */
fun materialSpecFor(
    material: AppMaterial,
    background: AppBackground,
    scheme: ColorScheme,
    dark: Boolean,
    relief: AppRelief = AppRelief.DEFAULT,
): GpMaterialSpec {
    val ground = background.resolve(material)
    val backdrop = backdropFor(ground, scheme, dark)
    val volume = chartVolumeFor(material, scheme, dark, relief)
    return when (material) {
        AppMaterial.GLASS -> glassSpec(scheme, dark, backdrop, volume)
        AppMaterial.LIQUID_GLASS -> liquidSpec(scheme, dark, backdrop, volume)
        AppMaterial.NEO -> neoSpec(scheme, dark, backdrop, volume)
        AppMaterial.DARK_NEO -> darkNeoSpec(scheme, backdrop, volume)
    }
}

// ─────────────────────────── 1 · glassmorphism ─────────────────────────────

private fun glassSpec(
    scheme: ColorScheme,
    dark: Boolean,
    backdrop: GpBackdrop,
    volume: GpChartVolume,
): GpMaterialSpec {
    val panel = if (dark) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.62f)
    return GpMaterialSpec(
        material = AppMaterial.GLASS,
        surface = panel,
        surfaceEnd = panel,
        groove = if (dark) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.07f),
        elevation = 0.dp,
        shadow = GpShadowPair(
            dark = Color.Black.copy(alpha = if (dark) 0.30f else 0.13f),
            darkOffset = DpOffset(0.dp, 10.dp),
            light = Color.Transparent,
            lightOffset = DpOffset.Zero,
            blur = 22.dp,
        ),
        gloss = GpGloss(
            topRim = Color.White.copy(alpha = if (dark) 0.42f else 0.90f),
            bottomRim = Color.Transparent,
            specular = Color.Transparent,
            // A coloured bloom instead of a bevel — §4.1's arc description for
            // this material, and the only place its accent touches the panel.
            //
            // ⚠️ **The dark branch was a real WCAG failure and is fixed here
            // (`#57` b).** `GpGloss` says what this layer is for: a dark-theme
            // glass panel built from white layers alone can only LIGHTEN what
            // is behind it, so wherever a background hue peaks the panel's own
            // white type goes illegible — its contrast becomes a property of
            // the wallpaper rather than of the component. `primary` at 0.06 is
            // a bloom and not a floor, so it did not do that job: `Observed:`
            // body text at **2.55–2.78:1** against `onSurface` at page point
            // (0.91, 0.08) — under glow 2, inside a card, on the material's own
            // native ground — across both skins, i.e. reachable today by
            // picking Glass and dark. Measured 2026-08-22 by `GpBackdrop.colorAt`
            // over a 41x27 grid of the text region.
            //
            // The fix keeps the bloom's HUE and gives it a floor's LIGHTNESS:
            // `atLightness(0.05f)` is the accent as a near-black, so the accent
            // still touches the panel and the panel now owns its own contrast
            // on ANY ground -- which is what `#57` b needs, since the ground is
            // a user choice from now on. 0.34 rather than the 0.28 that first
            // clears 4.5: the model's aspect ratio and sample grid are
            // approximations, so the threshold is cleared with margin (5.12)
            // rather than sat on. The light branch is untouched -- it measures
            // 10.3-16.0 and never needed a floor.
            tintFloor = if (dark) {
                scheme.primary.atLightness(0.05f).copy(alpha = 0.34f)
            } else {
                scheme.primary.copy(alpha = 0.03f)
            },
        ),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentStops = listOf(scheme.primary),
        edge = scheme.onSurface.copy(alpha = if (dark) 0.34f else 0.26f),
        edgeWidth = 1.dp,
        overlay = panel.over(scheme.background),
        corner = 26.dp,
        backdrop = backdrop,
        volume = volume,
    )
}

// ─────────────────────────── 2 · liquid glass ──────────────────────────────

private fun liquidSpec(
    scheme: ColorScheme,
    dark: Boolean,
    backdrop: GpBackdrop,
    volume: GpChartVolume,
): GpMaterialSpec {
    val panel = if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.66f)
    return GpMaterialSpec(
        material = AppMaterial.LIQUID_GLASS,
        surface = panel,
        surfaceEnd = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.42f),
        groove = if (dark) Color.Black.copy(alpha = 0.26f) else Color.Black.copy(alpha = 0.09f),
        elevation = 0.dp,
        shadow = GpShadowPair(
            // The coloured glow cast on the ground — refraction, not a drop.
            dark = scheme.primary.copy(alpha = if (dark) 0.34f else 0.22f),
            darkOffset = DpOffset(0.dp, 12.dp),
            light = Color.Transparent,
            lightOffset = DpOffset.Zero,
            blur = 26.dp,
        ),
        gloss = GpGloss(
            topRim = Color.White.copy(alpha = if (dark) 0.92f else 1f),
            bottomRim = Color.White.copy(alpha = if (dark) 0.26f else 0.60f),
            specular = Color.White.copy(alpha = if (dark) 0.36f else 0.72f),
            // Liquid already had a real floor -- which is exactly why it
            // measured 4.12-4.28 where glass measured 2.55-2.78, the two
            // materials differing in nothing else that touches this. Two
            // changes, both small (`#57` b): 0.26 -> 0.34, because 4.12 does
            // not clear 4.5 either; and the fixed `#080A14` becomes the accent
            // as a near-black, so the floor tracks the skin like every other
            // colour in this file rather than being one hardcoded hex
            // (`#57` a's finding, applied to the last place it had not reached).
            tintFloor = if (dark) {
                scheme.primary.atLightness(0.04f).copy(alpha = 0.34f)
            } else {
                Color.Transparent
            },
        ),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentStops = listOf(scheme.primary, scheme.tertiary),
        edge = scheme.onSurface.copy(alpha = if (dark) 0.46f else 0.30f),
        edgeWidth = 1.dp,
        overlay = panel.over(scheme.background),
        corner = 32.dp,
        backdrop = backdrop,
        volume = volume,
    )
}

// ───────────────────────────────── 3 · neo ─────────────────────────────────

private fun neoSpec(
    scheme: ColorScheme,
    dark: Boolean,
    backdrop: GpBackdrop,
    volume: GpChartVolume,
): GpMaterialSpec {
    // ── the definitional case, and it is the whole reason this parameter exists ──
    //
    // Neumorphism IS the surface being the same colour as what is behind it --
    // that is what makes the shadow pair read as an extrusion rather than as a
    // floating card. A lit ground has no single such colour, so on one, neo
    // CANNOT be neo. `AppBackground` says this to the user in words; here is
    // where the pixels answer for it.
    //
    // Two answers were available and only one is honest. Painting the opaque
    // page colour anyway gives an opaque grey slab sitting on a gradient -- the
    // shadow pair then reads as a drop shadow under a card, which is the exact
    // look neumorphism is defined against. The prototype's answer, ported here,
    // is a NEUTRAL PLATE: the page tone kept but made slightly translucent, so
    // the ground reads through it and the pair still reads as depth. The card
    // gains an EDGE it would not otherwise have. That is a real difference, not
    // a rendering shortcut -- it is soft UI on a canvas, not neumorphism, and
    // `AppBackground`'s doc says so before the user picks it.
    //
    // Alphas are the prototype's own (.76 dark / .80 light,
    // `.st-neo.shared .card`), which is the one number in this file that WAS
    // copyable from it: an alpha is a ratio, not a hue, so `#57` a's finding
    // that the prototype's hexes could not be ported does not reach it.
    val plate = when {
        backdrop.isPlain -> scheme.surface
        dark -> scheme.surface.copy(alpha = 0.76f)
        else -> scheme.surface.copy(alpha = 0.80f)
    }
    return GpMaterialSpec(
    material = AppMaterial.NEO,
    // Opaque, and the SAME colour as the page: neo's whole claim is that the
    // panel is not a different tone, it is the same tone extruded. On a lit
    // ground that claim is unavailable and `plate` is translucent instead.
    surface = plate,
    surfaceEnd = plate,
    // The groove stays OPAQUE even on a lit ground. A recess is a hole cut into
    // the plate, and a hole you can see the wallpaper through is not a recess --
    // it is a window, and the inset pair then reads as a frame around it.
    groove = scheme.surface,
    elevation = 0.dp,
    shadow = GpShadowPair(
        dark = if (dark) Color.Black.copy(alpha = 0.60f) else scheme.onSurface.copy(alpha = 0.22f),
        darkOffset = DpOffset(6.dp, 6.dp),
        light = if (dark) Color.White.copy(alpha = 0.055f) else Color.White,
        lightOffset = DpOffset((-6).dp, (-6).dp),
        blur = 14.dp,
    ),
    gloss = null,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    accentStops = listOf(scheme.primary),
    // Neo's known WCAG failure is the shadow-only affordance, so this is the
    // one material where --edge is doing load-bearing work rather than trim.
    edge = scheme.onSurface.copy(alpha = 0.26f),
    edgeWidth = 1.dp,
    // C22: neo's surface IS the page colour, so an overlay that inherited it
    // would be transparent and the dimmed screen would read straight through.
    // Built off `scheme.surface` and never off `plate`, which on a lit ground
    // is translucent -- an overlay is the one token that may never be.
    overlay = scheme.surface.shiftedLightness(if (dark) 0.055f else -0.045f),
    corner = 22.dp,
    backdrop = backdrop,
    volume = volume,
    )
}

// ─────────────────────────────── 4 · dark neo ──────────────────────────────

private fun darkNeoSpec(
    scheme: ColorScheme,
    backdrop: GpBackdrop,
    volume: GpChartVolume,
): GpMaterialSpec {
    // The same problem as neo, one shade darker: dark neo's shadows need a
    // charcoal to sit on, and a lit ground is not charcoal. The prototype's
    // answer is the same plate one shade down (`.st-darkneo.shared`, .88/.90),
    // and it keeps the two-stop gradient rather than flattening it -- the
    // gradient is what the very large radii are read against.
    val top = if (backdrop.isPlain) {
        scheme.surfaceContainerHighest
    } else {
        scheme.surfaceContainerHighest.copy(alpha = 0.88f)
    }
    val bottom = if (backdrop.isPlain) {
        scheme.surfaceContainerLow
    } else {
        scheme.surfaceContainerLow.copy(alpha = 0.90f)
    }
    return GpMaterialSpec(
    material = AppMaterial.DARK_NEO,
    surface = top,
    surfaceEnd = bottom,
    groove = scheme.surfaceContainerLow,
    elevation = 0.dp,
    shadow = GpShadowPair(
        dark = Color.Black.copy(alpha = 0.72f),
        darkOffset = DpOffset(9.dp, 9.dp),
        light = Color.White.copy(alpha = 0.045f),
        lightOffset = DpOffset((-7).dp, (-7).dp),
        blur = 20.dp,
    ),
    gloss = GpGloss(
        topRim = Color.White.copy(alpha = 0.05f),
        bottomRim = Color.Transparent,
        specular = Color.Transparent,
        tintFloor = Color.Transparent,
    ),
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    // The one material whose accent is a GRADIENT rather than a colour — which
    // is why `accentStops` exists at all rather than a single `accent`.
    accentStops = listOf(scheme.inversePrimary, scheme.primary),
    edge = scheme.onSurface.copy(alpha = 0.22f),
    edgeWidth = 1.dp,
    // Opaque by C22, and therefore off the scheme rather than off `top`.
    overlay = scheme.surfaceContainerHighest,
    corner = 28.dp,
    backdrop = backdrop,
    volume = volume,
    )
}

// ────────────────────────── 5 · chart volume ───────────────────────────────

/**
 * How [material] builds a chart body, on [relief].
 *
 * ## Why this is one function and not four fields on the four specs above
 *
 * The four material builders each answer `surface · groove · elevation ·
 * accent` from scratch, because those four genuinely differ in kind. Volume does
 * not: every material wants the same five layers, and what differs is **how
 * hard each is pushed**. Written as four independent literal blocks, the fact
 * that dark neo's cast is four times glass's would be invisible — it is a
 * *relationship*, and a relationship is only readable where the values sit
 * together.
 *
 * ## The ink, and why it is not `#0A101A`
 *
 * The prototype mixes every shade toward one hard-coded near-black. That hex is
 * `#57` a's finding: it does not track the skin, and here it would be worse than
 * there, because this one is mixed into **every category hue at once** — a wrong
 * ink tints the whole palette rather than one swatch. So it is the scheme's own
 * background taken down to near-black, which is the same colour on the default
 * skin and the right one on the others.
 *
 * ## The numbers
 *
 * Read as a table, because that is what they are:
 *
 * | | tint | shade | bevel | fold | sheen | cast α | grain |
 * |---|---|---|---|---|---|---|---|
 * | glass | .34 | .26 | .20 | .16 | .42 | .16 | — |
 * | liquid | .38 | .24 | .26 | .14 | .55 | .24 | .05 |
 * | neo | .30 | .30 | .13 | .20 | .18 | .22 | .10 |
 * | dark neo | .46 | .34 | .30 | .32 | .30 | .55 | .14 |
 *
 * Three of the four rows are the material's own §4.1 sentence, one axis over:
 * glass is *depth from blur*, so it gets sheen and no grain — a grain pass on a
 * translucent panel reads as dirt on the glass rather than as the body's own
 * surface. Liquid is *depth from refraction at the edge*, so its sheen is the
 * strongest of the four and its cast is **coloured** rather than black, matching
 * the refracted glow its panels already drop. Neo is *depth from a shadow pair
 * on one flat surface* and has no gloss at all, so its sheen is barely there and
 * its work is done by shade and fold. Dark neo is *a deep shadow pair plus one
 * saturated gradient*: every number is the largest in its column, which is what
 * that material is.
 */
private fun chartVolumeFor(
    material: AppMaterial,
    scheme: ColorScheme,
    dark: Boolean,
    relief: AppRelief,
): GpChartVolume {
    // The prototype's `ch * 0.34`. Uniform across the four materials on purpose:
    // `AppRelief`'s doc records that the height being a material property is the
    // decision Ido OVERTURNED, so varying it here would re-collapse the axis by
    // arithmetic after it was re-opened by ruling.
    val height = if (relief.isRaised) 0.34f else 0f
    val ink = scheme.background.atLightness(0.05f)
    return when (material) {
        AppMaterial.GLASS -> GpChartVolume(
            tint = 0.34f,
            shade = 0.26f,
            ink = ink,
            bevel = 0.20f,
            fold = 0.16f,
            sheen = 0.42f,
            cast = Color.Black.copy(alpha = if (dark) 0.22f else 0.16f),
            castOffset = 5.dp,
            castSpread = 7.dp,
            grain = 0f,
            relief = height,
        )

        AppMaterial.LIQUID_GLASS -> GpChartVolume(
            tint = 0.38f,
            shade = 0.24f,
            ink = ink,
            bevel = 0.26f,
            fold = 0.14f,
            sheen = 0.55f,
            // Coloured, not black: liquid's panels already cast refraction rather
            // than a drop (see `liquidSpec`'s shadow pair), and a chart body that
            // dropped plain black on that material would be the one object in the
            // scene lit by a different physics.
            cast = scheme.primary.copy(alpha = if (dark) 0.30f else 0.24f),
            castOffset = 6.dp,
            castSpread = 8.dp,
            grain = 0.05f,
            relief = height,
        )

        AppMaterial.NEO -> GpChartVolume(
            tint = 0.30f,
            shade = 0.30f,
            ink = ink,
            bevel = 0.13f,
            fold = 0.20f,
            sheen = 0.18f,
            cast = if (dark) {
                Color.Black.copy(alpha = 0.34f)
            } else {
                scheme.onSurface.copy(alpha = 0.22f)
            },
            castOffset = 4.dp,
            castSpread = 6.dp,
            grain = 0.10f,
            relief = height,
        )

        AppMaterial.DARK_NEO -> GpChartVolume(
            tint = 0.46f,
            shade = 0.34f,
            ink = ink,
            bevel = 0.30f,
            fold = 0.32f,
            sheen = 0.30f,
            cast = Color.Black.copy(alpha = 0.55f),
            castOffset = 6.dp,
            castSpread = 9.dp,
            grain = 0.14f,
            relief = height,
        )
    }
}

// ───────────────────────────── draw modifiers ──────────────────────────────

/**
 * Draw a **raised panel** in the current material: the shadow pair, then the
 * fill, then the gloss, then `--edge`.
 *
 * The order is the contract. The edge is stroked **last and always**, so no
 * amount of translucency or shadow tuning can leave an affordance without its
 * hairline anchor.
 */
fun Modifier.gpSurface(spec: GpMaterialSpec, shape: Shape): Modifier = this
    .drawBehind { spec.shadow?.let { drawShadowPair(it, spec.corner.toPx()) } }
    .clip(shape)
    .background(
        if (spec.surface == spec.surfaceEnd) {
            Brush.linearGradient(listOf(spec.surface, spec.surface))
        } else {
            Brush.linearGradient(listOf(spec.surface, spec.surfaceEnd))
        },
    )
    .drawBehind { spec.gloss?.let { drawGloss(it) } }
    .border(spec.edgeWidth, spec.edge, shape)

/**
 * Draw a **recessed track** — a thing you read, not press.
 *
 * Neo and dark neo invert their shadow pair into the shape; the two glass
 * materials darken instead, because a translucent panel has no inside to cast
 * into. Same call site either way, which is the point of the contract.
 */
fun Modifier.gpGroove(spec: GpMaterialSpec, shape: Shape): Modifier = this
    .clip(shape)
    .background(spec.groove)
    .drawBehind { spec.shadow?.let { drawInsetShadow(it) } }
    .border(spec.edgeWidth, spec.edge, shape)

/**
 * The page the whole app sits on — the **only** reader of
 * [GpMaterialSpec.backdrop], and one of exactly two call sites in the app
 * (`MainActivity` and the pickers in `ui/components/MaterialPicker.kt`).
 *
 * Which ground gets drawn is [AppBackground]'s answer now, not the material's:
 * a flat tone for [AppBackground.PLAIN], a diagonal wash carrying soft radial
 * lights for the other two. [background] stays a parameter rather than being
 * folded into the spec because it is the *scheme's* background — what a plain
 * ground is made of, and what the wash is anchored to.
 */
fun Modifier.gpPage(spec: GpMaterialSpec, background: Color): Modifier {
    val backdrop = spec.backdrop
    if (backdrop.isPlain) return this.background(background)
    return this
        // Painted first and always, even under the wash: the wash's stops are
        // derived from this colour but a gradient is not guaranteed opaque
        // everywhere, and a page that is transparent anywhere shows the window.
        .background(background)
        .drawBehind {
            if (backdrop.base.isNotEmpty()) {
                drawRect(
                    // 170deg in CSS, i.e. very nearly straight down with a lean
                    // to the left -- the lean is what stops the wash reading as
                    // a horizon line behind the radial lights.
                    brush = Brush.linearGradient(
                        colors = backdrop.base,
                        start = Offset(size.width * 0.18f, 0f),
                        end = Offset(size.width * 0.82f, size.height),
                    ),
                )
            }
            backdrop.glows.forEach { glow ->
                val centre = Offset(size.width * glow.x, size.height * glow.y)
                val radius = size.minDimension * glow.radius
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.hue, Color.Transparent),
                        center = centre,
                        radius = radius,
                    ),
                    radius = radius,
                    center = centre,
                )
            }
        }
}

/**
 * A stacked soft shadow.
 *
 * `BlurMaskFilter` is unsupported on a hardware canvas below API 28 and this
 * app is `minSdk 26`, so the falloff is summed from [STEPS] translucent rounded
 * rects rather than blurred. Each ring carries `alpha / STEPS`, so the stack
 * ramps linearly from the core outwards.
 */
private fun DrawScope.drawShadowPair(pair: GpShadowPair, corner: Float) {
    drawSoftRect(pair.dark, pair.darkOffset, pair.blur, corner)
    if (pair.light != Color.Transparent) drawSoftRect(pair.light, pair.lightOffset, pair.blur, corner)
}

private const val STEPS = 6

private fun DrawScope.drawSoftRect(color: Color, offset: DpOffset, blur: Dp, corner: Float) {
    val dx = offset.x.toPx()
    val dy = offset.y.toPx()
    val spread = blur.toPx()
    val step = color.alpha / STEPS
    for (i in 1..STEPS) {
        val grow = spread * i / STEPS
        drawRoundRect(
            color = color.copy(alpha = step),
            topLeft = Offset(dx - grow, dy - grow),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            cornerRadius = CornerRadius(corner + grow),
        )
    }
}

/** The same falloff, pointing inwards — the groove. */
private fun DrawScope.drawInsetShadow(pair: GpShadowPair) {
    val spread = pair.blur.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(pair.dark.copy(alpha = pair.dark.alpha * 0.9f), Color.Transparent),
            startY = 0f,
            endY = spread,
        ),
    )
    if (pair.light != Color.Transparent) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, pair.light.copy(alpha = pair.light.alpha * 0.8f)),
                startY = size.height - spread,
                endY = size.height,
            ),
        )
    }
}

/** The rim, the counter-rim, the streak and the tint floor — in that order. */
private fun DrawScope.drawGloss(gloss: GpGloss) {
    if (gloss.tintFloor != Color.Transparent) drawRect(gloss.tintFloor)
    if (gloss.specular != Color.Transparent) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(gloss.specular, Color.Transparent),
                startY = 0f,
                endY = size.height * 0.46f,
            ),
        )
    }
    val rim = 1.6f * density
    if (gloss.topRim != Color.Transparent) {
        drawRect(color = gloss.topRim, size = Size(size.width, rim))
    }
    if (gloss.bottomRim != Color.Transparent) {
        drawRect(
            color = gloss.bottomRim,
            topLeft = Offset(0f, size.height - rim),
            size = Size(size.width, rim),
        )
    }
}

// ───────────────────────────────── helpers ─────────────────────────────────

/** Composite a translucent colour onto an opaque one — how an overlay stops being see-through. */
internal fun Color.over(background: Color): Color = Color(
    red = red * alpha + background.red * (1f - alpha),
    green = green * alpha + background.green * (1f - alpha),
    blue = blue * alpha + background.blue * (1f - alpha),
    alpha = 1f,
)

private fun Color.shiftedLightness(delta: Float): Color =
    atLightness((hsl()[2] + delta).coerceIn(0f, 1f))

// ─────────────────────────── the three grounds ─────────────────────────────

/**
 * [background] — already resolved past [AppBackground.MATCH] — as something
 * drawable.
 *
 * Every hue is taken off the [ColorScheme] rather than the raw skin, so a
 * ground tracks the palette transform like everything else. That is what makes
 * a lit ground survivable under the two materials that were never designed for
 * one: under neo the lights arrive muted with the rest of the palette, and
 * under dark neo they have already collapsed onto the accent ramp, so a lit
 * page does not put a second saturated colour on the one material whose whole
 * claim is that there is exactly one.
 *
 * Kept low-alpha for the same reason it always was: the page has to stay a
 * **ground**. It is the thing a panel is translucent about, not a second
 * subject.
 *
 * ## Where the anchors and radii come from
 *
 * The prototype, converted from its 392 px frame to fractions (see [GpGlow]).
 * A CSS `radial-gradient(700px 460px at 8% -6%, C 0%, transparent 62%)` reaches
 * transparent at `0.62 × 700 px = 434 px`, which on a 392 px-wide frame is
 * `1.11` — so the radii below are `stop × px / 392`, rounded to two places, and
 * the anchors are the percentages unchanged.
 */
private fun backdropFor(
    background: AppBackground,
    scheme: ColorScheme,
    dark: Boolean,
): GpBackdrop {
    if (background == AppBackground.PLAIN) return GpBackdrop.Plain
    // One alpha for both schemes, and the dark branch used to be 0.55f.
    //
    // ⚠️ **The port took the prototype's HUE SELECTION but not its luminance,
    // and in dark mode that doubled the ground's brightness.** The prototype's
    // dark canvas is built from *saturated mid* hues -- `#4E6BFF`, `#00C8B4`,
    // `#A65CF5`, relative luminance **0.194 / 0.446 / 0.224** -- while a dark
    // Material 3 scheme's `primary`/`secondary`/`tertiary` are *pastels*, and
    // this app's measure **0.564-0.572** on both skins. Same hues, roughly twice
    // the light. At 0.55 that is not a ground any more; `backdropHues` (this
    // function's ancestor) already said the page "has to stay a **ground**: it
    // is the thing the panel is translucent about, not a second subject", and
    // the number had drifted away from its own doc.
    //
    // 0.42 is where `onBackground` clears WCAG's 3:1 non-text floor on the
    // worst lit page (**3.96**, against **2.94** at 0.55) with the panel floors
    // above in place. It is deliberately NOT pushed to 4.5:1 -- doing so needs
    // roughly 0.30, which is dimmer than the prototype's own hues and would be
    // a taste change rather than a correction. Nothing in the app paints body
    // text straight onto the page; `onBackground` reaches it only through
    // Material 3's default content colour under `MainActivity`'s `Surface`,
    // which carries section headings rather than paragraphs.
    val alpha = 0.42f
    // The diagonal wash the lights sit on. The prototype authors it as two
    // hexes per scheme (`#131A34 -> #0C1120`); here it is the scheme's own
    // background nudged in both directions, so it tracks the skin and cannot
    // drift from the flat ground PLAIN draws.
    val base = listOf(
        scheme.background.shiftedLightness(if (dark) 0.02f else 0.012f),
        scheme.background.shiftedLightness(if (dark) -0.03f else -0.022f),
    )
    return when (background) {
        // Glassmorphism's ground -- and the prototype's SHARED CANVAS, which is
        // why this value is what `#57`'s "combinations" mostly means. Three
        // wide lights: top-left, off the right edge, and one rising from below.
        AppBackground.GLOW -> GpBackdrop(
            base = base,
            glows = listOf(
                GpGlow(scheme.primary.copy(alpha = alpha), x = 0.08f, y = -0.06f, radius = 1.11f),
                GpGlow(scheme.tertiary.copy(alpha = alpha), x = 1.04f, y = 0.08f, radius = 0.98f),
                GpGlow(scheme.secondary.copy(alpha = alpha), x = 0.44f, y = 1.08f, radius = 1.20f),
            ),
        )

        // Liquid glass's ground. Four lights instead of three and every one of
        // them smaller, which is the whole difference -- see [GpBackdrop] for
        // why the fourth is not the prototype's warm `#FFB25C`. It is dimmer
        // (x0.8) as well as smaller so that repeating `primary` reads as a
        // fill-light rather than as a second copy of the first one.
        AppBackground.SPECTRUM -> GpBackdrop(
            base = base,
            glows = listOf(
                GpGlow(scheme.primary.copy(alpha = alpha), x = 0.12f, y = 0.00f, radius = 0.89f),
                GpGlow(scheme.tertiary.copy(alpha = alpha), x = 0.96f, y = 0.14f, radius = 0.80f),
                GpGlow(scheme.secondary.copy(alpha = alpha), x = 0.40f, y = 1.04f, radius = 1.01f),
                GpGlow(
                    scheme.primary.copy(alpha = alpha * 0.8f),
                    x = 0.88f,
                    y = 0.74f,
                    radius = 0.77f,
                ),
            ),
        )

        // Unreachable: PLAIN returned above, MATCH is resolved before the call.
        AppBackground.PLAIN, AppBackground.MATCH -> GpBackdrop.Plain
    }
}
