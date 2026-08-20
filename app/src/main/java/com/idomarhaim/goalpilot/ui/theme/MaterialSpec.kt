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
import com.idomarhaim.goalpilot.domain.model.AppMaterial

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
 * | **`.tag`** — a category is written in words beside its dot, because dark neo collapses six hues into one ramp | authoring, not a token; the collapse arithmetic is `MaterialPalettes.rampTint` |
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
     * The page behind everything. `null` means *the scheme's flat background* —
     * which is neo and dark neo, whose ground is flat by definition. Glass and
     * liquid glass supply hues here, because a translucent panel over a flat
     * ground is not translucent, it is grey.
     */
    val backdrop: List<Color>?,
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
 * The four answers, for [material] against an already-transformed [scheme].
 *
 * Reads the scheme rather than the raw palette, so the material's *surface*
 * decisions sit on top of its own *palette* transform — the two axes compose
 * here and nowhere else.
 */
fun materialSpecFor(material: AppMaterial, scheme: ColorScheme, dark: Boolean): GpMaterialSpec =
    when (material) {
        AppMaterial.GLASS -> glassSpec(scheme, dark)
        AppMaterial.LIQUID_GLASS -> liquidSpec(scheme, dark)
        AppMaterial.NEO -> neoSpec(scheme, dark)
        AppMaterial.DARK_NEO -> darkNeoSpec(scheme)
    }

// ─────────────────────────── 1 · glassmorphism ─────────────────────────────

private fun glassSpec(scheme: ColorScheme, dark: Boolean): GpMaterialSpec {
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
            tintFloor = scheme.primary.copy(alpha = if (dark) 0.06f else 0.03f),
        ),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentStops = listOf(scheme.primary),
        edge = scheme.onSurface.copy(alpha = if (dark) 0.34f else 0.26f),
        edgeWidth = 1.dp,
        overlay = panel.over(scheme.background),
        corner = 26.dp,
        backdrop = scheme.backdropHues(dark),
    )
}

// ─────────────────────────── 2 · liquid glass ──────────────────────────────

private fun liquidSpec(scheme: ColorScheme, dark: Boolean): GpMaterialSpec {
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
            tintFloor = if (dark) Color(0xFF080A14).copy(alpha = 0.26f) else Color.Transparent,
        ),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentStops = listOf(scheme.primary, scheme.tertiary),
        edge = scheme.onSurface.copy(alpha = if (dark) 0.46f else 0.30f),
        edgeWidth = 1.dp,
        overlay = panel.over(scheme.background),
        corner = 32.dp,
        backdrop = scheme.backdropHues(dark),
    )
}

// ───────────────────────────────── 3 · neo ─────────────────────────────────

private fun neoSpec(scheme: ColorScheme, dark: Boolean): GpMaterialSpec = GpMaterialSpec(
    material = AppMaterial.NEO,
    // Opaque, and the SAME colour as the page: neo's whole claim is that the
    // panel is not a different tone, it is the same tone extruded.
    surface = scheme.surface,
    surfaceEnd = scheme.surface,
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
    overlay = scheme.surface.shiftedLightness(if (dark) 0.055f else -0.045f),
    corner = 22.dp,
    backdrop = null,
)

// ─────────────────────────────── 4 · dark neo ──────────────────────────────

private fun darkNeoSpec(scheme: ColorScheme): GpMaterialSpec = GpMaterialSpec(
    material = AppMaterial.DARK_NEO,
    surface = scheme.surfaceContainerHighest,
    surfaceEnd = scheme.surfaceContainerLow,
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
    overlay = scheme.surfaceContainerHighest,
    corner = 28.dp,
    backdrop = null,
)

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
 * The page the whole app sits on.
 *
 * Flat for neo and dark neo — their ground is flat by definition. For the two
 * translucent materials it is the skin's hues as soft radials, because a
 * translucent panel over a flat ground has nothing to be translucent *about*.
 */
fun Modifier.gpPage(spec: GpMaterialSpec, background: Color): Modifier {
    val hues = spec.backdrop ?: return this.background(background)
    return this
        .background(background)
        .drawBehind {
            val anchors = listOf(
                Offset(size.width * 0.08f, -size.height * 0.06f),
                Offset(size.width * 1.04f, size.height * 0.10f),
                Offset(size.width * 0.44f, size.height * 1.06f),
            )
            hues.forEachIndexed { index, hue ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(hue, Color.Transparent),
                        center = anchors[index % anchors.size],
                        radius = size.minDimension * 1.15f,
                    ),
                    radius = size.minDimension * 1.15f,
                    center = anchors[index % anchors.size],
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

/**
 * The three radial hues a translucent material's page is built from.
 *
 * Taken off the scheme rather than the raw skin, so the backdrop tracks the
 * palette transform like everything else — and kept low-alpha, because the page
 * has to stay a *ground*: it is the thing the panel is translucent about, not a
 * second subject.
 */
private fun ColorScheme.backdropHues(dark: Boolean): List<Color> {
    val alpha = if (dark) 0.55f else 0.42f
    return listOf(
        primary.copy(alpha = alpha),
        tertiary.copy(alpha = alpha),
        secondary.copy(alpha = alpha),
    )
}
