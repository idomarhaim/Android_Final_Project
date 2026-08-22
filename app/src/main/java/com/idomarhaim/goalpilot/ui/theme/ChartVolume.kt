package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * **Volume** — the fifth answer in the material contract, added by `#57` c.
 *
 * ## What was missing, in Ido's words
 *
 * > *"no 3D graph and everything from the second prototype"*
 *
 * A flat fill is why a chart reads as a diagram rather than as an object.
 * Revision 4 of `docs/prototypes/2026-08-10-charts-presentation/index.html`
 * spells the fix out as **five compositing layers on every arc, bar and ring** —
 * a three-stop fill, a specular bevel, a sheen along the lit edge, a cast
 * shadow, and a grain pass so the surface reads as a surface.
 *
 * ## ⚠️ One of those five is NOT ported, and the prototype itself is why
 *
 * The brief for this session listed `feSpecularLighting` as the bevel layer.
 * **The later prototype deletes it.** `docs/prototypes/2026-08-11-visual-styles/`
 * — authored a day after rev 4, and rebuilt again on 2026-08-12 — carries this
 * verbatim above its own donut:
 *
 * > *NO `feSpecularLighting` anywhere: that filter over a fat stroke is what
 * > inflated rev 4's ring into a balloon.*
 *
 * So the specular bevel is dropped **on the prototype's authority, not on
 * Compose's limits**, and what replaces it is the thing the 2026-08-12 rebuild
 * replaced it with: a **directional bevel wash clipped to the face** — brightest
 * where the face turns toward the light, gone within a third of the body, with
 * the matching shadow wash from the opposite side. That is a plain linear
 * gradient, which Compose draws natively and exactly.
 *
 * This matters beyond one layer: it is the only one of the five whose Compose
 * port would have been a *fake*, and the newer artifact had already thrown it
 * away. See [drawVolumeArcs] for what each remaining layer costs.
 *
 * ## What each material answers
 *
 * A chart never asks *"is this neo?"* — the same rule `GpMaterialSpec`'s header
 * states. It asks for [GpMaterialSpec.volume] and draws. Glass answers with a
 * shallow tint and no grain; dark neo answers with deep shade and a real cast.
 */
@Immutable
data class GpChartVolume(

    // ── layer 1 · the three-stop fill ──────────────────────────────────────
    /** How far the top stop is lifted toward white. The prototype's `lite()`. */
    val tint: Float,
    /** How far the bottom stop is pushed toward [ink]. The prototype's `dark()`. */
    val shade: Float,
    /**
     * What *darker* means for this material.
     *
     * The prototype hard-codes `#0A101A`. Here it comes off the scheme, for
     * `#57` a's reason one axis down: a fixed hex does not track the skin, and
     * this one is mixed into **every** category hue, so a wrong one tints the
     * entire palette at once.
     */
    val ink: Color,

    // ── layer 2 · the directional bevel (see the header: NOT a specular filter) ──
    /** Peak alpha of the white wash where a face turns toward the light. */
    val bevel: Float,
    /** Peak alpha of the dark wash on the opposite side. */
    val fold: Float,

    // ── layer 3 · the sheen along the lit edge ─────────────────────────────
    /** Peak alpha of the sheen. `0f` skips the pass — glass's rim already is one. */
    val sheen: Float,

    // ── layer 4 · the cast shadow ──────────────────────────────────────────
    /** What the body drops onto the track under it. Alpha carried on the colour. */
    val cast: Color,
    /** How far below the body the cast sits. */
    val castOffset: Dp,
    /** How far the cast spreads past the body — the stand-in for a blur radius. */
    val castSpread: Dp,

    // ── layer 5 · the grain ────────────────────────────────────────────────
    /**
     * Alpha of the baked noise laid over a body. `0f` skips the pass entirely.
     *
     * **Baked once**, into a 64x64 tile reused by every chart in the process —
     * the brief's named performance trap is a `feTurbulence` equivalent
     * evaluated per draw, and [grainBrush] is why that cannot happen here.
     */
    val grain: Float,

    // ── the raised axis ────────────────────────────────────────────────────
    /**
     * Extrusion height as a fraction of the **channel**, or `0f` when
     * [com.idomarhaim.goalpilot.domain.model.AppRelief.FLAT].
     *
     * A fraction of the channel and not of the body, because the height comes
     * **out of** the width budget rather than on top of it — that is the
     * 2026-08-12 rebuild's central correction, and [ArcGeometry] is where it is
     * spent.
     */
    val relief: Float,
) {
    /** Whether bodies are solids rather than paint. */
    val isRaised: Boolean get() = relief > 0f

    /** [hue] lifted toward white by [amount] — the prototype's `lite()`. */
    fun lit(hue: Color, amount: Float = tint): Color = hue.mixToward(Color.White, amount)

    /** [hue] pushed toward [ink] by [amount] — the prototype's `dark()`. */
    fun shaded(hue: Color, amount: Float = shade): Color = hue.mixToward(ink, amount)

    /**
     * The three-stop fill for [hue], anchored to **[bounds]** rather than to the
     * body.
     *
     * ⚠️ **User space, always — this is the one that goes wrong quietly.** The
     * prototype spells the failure out: an `objectBoundingBox` gradient is
     * relative to each slice's own box, *"so every block ends up lit from a
     * different direction and the ring stops being one scene."* Compose's
     * `Brush.linearGradient` defaults to the draw's own size, which is the same
     * trap; passing the whole chart's rect is what makes it one light.
     */
    fun faceBrush(hue: Color, bounds: Rect): Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to lit(hue),
            0.48f to hue,
            1f to shaded(hue),
        ),
        start = bounds.topLeft,
        end = bounds.bottomRight,
    )
}

/**
 * Where the light is, for every chart in the app.
 *
 * One direction, stated once: **from the top-left**, so bodies extrude down-right
 * and faces catch light on their up-left border. The two components are the
 * prototype's own `HX = H*0.58, HY = H*0.81`, i.e. a vector about 54° below the
 * horizontal — steep enough to read as height rather than as a slide.
 */
private const val LIGHT_X = 0.58f
private const val LIGHT_Y = 0.81f

/** One body in a set of arcs drawn together. */
@Immutable
data class VolumeArc(
    /** Degrees, `0` at 3 o'clock, positive clockwise — Compose's own convention. */
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    /** Stroke width when flat; channel width when raised. */
    val thickness: Float,
    /** Fades a body the user did not select. Multiplies every layer. */
    val alpha: Float = 1f,
)

/**
 * Everything a set of arcs needs from one another, computed once.
 *
 * ## Why the height comes out of the width
 *
 * The 2026-08-12 rebuild's own account of what its first attempt got wrong:
 *
 * > The first attempt faked height with a stack of translated copies. Ido saw
 * > exactly what that is: a pack of cards, wider than the channel, cutting its
 * > walls, and spilling over the neighbouring slice.
 *
 * So the body is **narrower** than the channel by exactly the height it swings
 * through, and it is **centred** in the channel — which is why the margin owed
 * is `H` and not `2H`. Budgeting `2H` is what made the blocks thin.
 */
private class ArcGeometry(
    val center: Offset,
    val radius: Float,
    val channel: Float,
    val height: Float,
) {
    /** The extrusion vector: down and right, because the light is up and left. */
    val vx = height * LIGHT_X
    val vy = height * LIGHT_Y

    /** The face sits half a height **up-light**, so the solid is centred in the channel. */
    val faceCenter = Offset(center.x - vx / 2f, center.y - vy / 2f)

    /** The channel annulus, as a clip. Nothing may cross a wall of the groove. */
    val clipOuter = radius + channel / 2f
    val clipInner = max(1f, radius - channel / 2f)

    /**
     * How wide one body is.
     *
     * **Per arc, not per chart**, because the donut thickens the wedge the user
     * selected — a single width would either freeze that gesture or size every
     * other wedge to the selected one. The raised branch is where the width
     * budget is spent: `channel - height` is the whole of the 2026-08-12
     * correction, and the `min` is what stops a selected wedge buying its extra
     * thickness out of the wall clearance.
     */
    fun bodyWidth(arc: VolumeArc): Float = if (height > 0f) {
        min(arc.thickness, channel - height).coerceAtLeast(1f)
    } else {
        arc.thickness
    }

    fun outer(width: Float) = radius + width / 2f
    fun inner(width: Float) = max(1f, radius - width / 2f)
}

/**
 * Draw a set of arcs as **bodies** — the whole of [GpChartVolume] in one call.
 *
 * ## Why a list rather than one call per arc
 *
 * Two of the five layers are **not per-body**, and calling per body gets both
 * wrong:
 *
 * - **Walls before faces, across the whole set.** The prototype: *"walls are
 *   drawn for every slice first and faces second, in two passes, so no
 *   neighbour's wall can ever land on top of a face."* One call per slice
 *   interleaves them and the ring grows the *"blocks climbing over each other"*
 *   defect that rebuild exists to fix.
 * - **Grain once.** A pass per slice is seven shader draws where one does, and
 *   the seams between them are visible on a repeating tile.
 *
 * @param bounds the whole chart, so every body is lit from the same direction.
 */
fun DrawScope.drawVolumeArcs(
    volume: GpChartVolume,
    bounds: Rect,
    center: Offset,
    radius: Float,
    channel: Float,
    arcs: List<VolumeArc>,
) {
    if (arcs.isEmpty()) return
    val geometry = ArcGeometry(
        center = center,
        radius = radius,
        channel = channel,
        height = channel * volume.relief,
    )

    // ── layer 4 · the cast, first and under everything ──────────────────────
    drawArcCast(volume, geometry, arcs)

    if (volume.isRaised) {
        clipPath(annulus(center, geometry.clipOuter, geometry.clipInner)) {
            arcs.forEach { drawArcWalls(volume, geometry, it) }
            arcs.forEach { drawArcFace(volume, geometry, bounds, it) }
        }
    } else {
        arcs.forEach { drawFlatArc(volume, geometry, bounds, it) }
    }

    // ── layer 3 · the sheen, one ribbon along the whole lit edge ────────────
    val widest = arcs.maxOf { geometry.bodyWidth(it) }
    if (volume.sheen > 0f) {
        val sheenRadius = radius + widest * 0.30f
        val sheenWidth = widest * 0.34f
        val first = arcs.first()
        val last = arcs.last()
        drawArc(
            brush = sheenBrush(volume, bounds),
            startAngle = first.startAngle,
            sweepAngle = (last.startAngle + last.sweepAngle) - first.startAngle,
            useCenter = false,
            topLeft = Offset(center.x - sheenRadius, center.y - sheenRadius),
            size = Size(sheenRadius * 2f, sheenRadius * 2f),
            style = Stroke(width = sheenWidth, cap = StrokeCap.Butt),
        )
    }

    // ── layer 5 · the grain, once, over the channel ─────────────────────────
    if (volume.grain > 0f) {
        drawArc(
            brush = grainBrush,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            alpha = volume.grain,
            style = Stroke(width = channel, cap = StrokeCap.Butt),
        )
    }
}

/** The shadow the ring drops on the track under it. */
private fun DrawScope.drawArcCast(
    volume: GpChartVolume,
    geometry: ArcGeometry,
    arcs: List<VolumeArc>,
) {
    if (volume.cast.alpha <= 0f) return
    val widest = arcs.maxOf { geometry.bodyWidth(it) }
    val offset = volume.castOffset.toPx()
    val spread = volume.castSpread.toPx()
    // Stacked, not blurred: `BlurMaskFilter` is unsupported on a hardware canvas
    // below API 28 and DOES NOT THROW -- it silently renders hard-edged
    // (`kb/dev/compose-soft-shadows-below-api-28.md`). `drawShadowPair` in
    // MaterialSpec.kt sums rounded rects for the same reason; this is the same
    // integration around a circle.
    val steps = 3
    val step = volume.cast.alpha / steps
    for (i in 1..steps) {
        val grow = spread * i / steps
        val castRadius = geometry.radius
        drawArc(
            color = volume.cast.copy(alpha = step),
            startAngle = arcs.first().startAngle,
            sweepAngle = (arcs.last().startAngle + arcs.last().sweepAngle) -
                arcs.first().startAngle,
            useCenter = false,
            topLeft = Offset(
                geometry.center.x - castRadius,
                geometry.center.y - castRadius + offset,
            ),
            size = Size(castRadius * 2f, castRadius * 2f),
            style = Stroke(width = widest + grow * 2f, cap = StrokeCap.Butt),
        )
    }
}

/** A painted arc: the three-stop fill, then the bevel wash, on a stroke. */
private fun DrawScope.drawFlatArc(
    volume: GpChartVolume,
    geometry: ArcGeometry,
    bounds: Rect,
    arc: VolumeArc,
) {
    val half = arc.thickness / 2f
    val topLeft = Offset(
        geometry.center.x - geometry.radius,
        geometry.center.y - geometry.radius,
    )
    val size = Size(geometry.radius * 2f, geometry.radius * 2f)
    drawArc(
        brush = volume.faceBrush(arc.color, bounds),
        startAngle = arc.startAngle,
        sweepAngle = arc.sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        alpha = arc.alpha,
        style = Stroke(width = arc.thickness, cap = StrokeCap.Butt),
    )
    // The bevel: a highlight riding the OUTER half of the stroke, which is the
    // edge that turns toward a top-left light on the upper arc. Drawn as a
    // narrower concentric stroke rather than as an outline -- a line all the way
    // round a shape is a frame, and a frame is what guarantees it reads flat.
    if (volume.bevel > 0f) {
        val bevelRadius = geometry.radius + half * 0.62f
        drawArc(
            color = Color.White.copy(alpha = volume.bevel * arc.alpha),
            startAngle = arc.startAngle,
            sweepAngle = arc.sweepAngle,
            useCenter = false,
            topLeft = Offset(
                geometry.center.x - bevelRadius,
                geometry.center.y - bevelRadius,
            ),
            size = Size(bevelRadius * 2f, bevelRadius * 2f),
            style = Stroke(width = max(1f, half * 0.34f), cap = StrokeCap.Butt),
        )
    }
}

/**
 * The side walls, the end caps and the contact shadow of one solid.
 *
 * Every piece here is a **face of a body**, not an outline of a shape — the
 * distinction the rebuild spends its longest comment on. A silhouette behind the
 * top face has no fold, no separate tone and no end, so it reads as a shadow;
 * what makes a wedge a slab is that its ends are drawn.
 */
private fun DrawScope.drawArcWalls(
    volume: GpChartVolume,
    geometry: ArcGeometry,
    arc: VolumeArc,
) {
    val a0 = arc.startAngle
    val a1 = arc.startAngle + arc.sweepAngle
    val face = geometry.faceCenter
    val hue = arc.color
    val width = geometry.bodyWidth(arc)
    val outer = geometry.outer(width)
    val inner = geometry.inner(width)

    // CONTACT SHADOW. Half the ring never shows a wall -- the light comes from
    // one direction, so blocks on the lit side present only their face, and
    // without this they read as flat plates. An object in a scene casts onto
    // what it sits on; without that it is a decal.
    if (volume.cast.alpha > 0f) {
        drawPath(
            path = sector(
                center = Offset(face.x + geometry.vx * 0.75f, face.y + geometry.vy * 0.75f),
                outer = outer + geometry.height * 0.5f,
                inner = max(1f, inner - geometry.height * 0.5f),
                startAngle = a0 - 1.4f,
                sweepAngle = arc.sweepAngle + 2.8f,
            ),
            color = volume.cast.copy(alpha = volume.cast.alpha * 0.55f * arc.alpha),
        )
    }

    drawPath(
        path = wallStrip(face, outer, a0, arc.sweepAngle, geometry.vx, geometry.vy),
        color = volume.shaded(hue, volume.shade * 0.85f),
        alpha = arc.alpha,
    )
    drawPath(
        path = wallStrip(face, inner, a0, arc.sweepAngle, geometry.vx, geometry.vy),
        color = volume.shaded(hue, min(0.92f, volume.shade * 1.7f)),
        alpha = arc.alpha,
    )
    drawPath(
        path = endCap(face, outer, inner, a0, geometry.vx, geometry.vy),
        color = volume.shaded(hue, volume.shade * 0.9f),
        alpha = arc.alpha,
    )
    drawPath(
        path = endCap(face, outer, inner, a1, geometry.vx, geometry.vy),
        color = volume.shaded(hue, min(0.92f, volume.shade * 1.3f)),
        alpha = arc.alpha,
    )
}

/** The top face of one solid: the fill, then the two directional washes. */
private fun DrawScope.drawArcFace(
    volume: GpChartVolume,
    geometry: ArcGeometry,
    bounds: Rect,
    arc: VolumeArc,
) {
    val width = geometry.bodyWidth(arc)
    val facePath = sector(
        center = geometry.faceCenter,
        outer = geometry.outer(width),
        inner = geometry.inner(width),
        startAngle = arc.startAngle,
        sweepAngle = arc.sweepAngle,
    )
    drawPath(
        path = facePath,
        brush = volume.faceBrush(arc.color, bounds),
        alpha = arc.alpha,
    )
    // The bevel and the fold, clipped to the face. Not strokes: "a bevel is not
    // a line -- it is the face itself catching more light near the edge that
    // turns toward the source", brightest at the up-light border and gone within
    // a third of the block. Both are washes over the whole chart's rect, so
    // every face is lit from the same place.
    clipPath(facePath) {
        if (volume.bevel > 0f) {
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = volume.bevel),
                        0.34f to Color.Transparent,
                    ),
                    start = bounds.topLeft,
                    end = bounds.bottomRight,
                ),
                topLeft = bounds.topLeft,
                size = bounds.size,
                alpha = arc.alpha,
            )
        }
        if (volume.fold > 0f) {
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to volume.ink.copy(alpha = volume.fold),
                        0.30f to Color.Transparent,
                    ),
                    start = bounds.bottomRight,
                    end = bounds.topLeft,
                ),
                topLeft = bounds.topLeft,
                size = bounds.size,
                alpha = arc.alpha,
            )
        }
    }
}

// ─────────────────────────────── bars ──────────────────────────────────────

/** One body in a set of bars drawn together. */
@Immutable
data class VolumeBar(
    val rect: Rect,
    val color: Color,
    val alpha: Float = 1f,
    /** Corner radius on the two ends the caller wants rounded. */
    val topRadius: Float = 0f,
    val bottomRadius: Float = 0f,
    /**
     * Whether this body's **top** face is visible.
     *
     * `false` for every segment of a stack except the highest: a segment with
     * another one sitting on it has no top to show, and drawing one anyway puts
     * a lit plate through the middle of the column.
     */
    val capped: Boolean = true,
    /**
     * Whether this body drops a cast shadow.
     *
     * `false` for every segment of a stack except the **lowest**: a cast falls on
     * the ground a body stands on, and an interior segment's ground is the
     * segment under it, which is already dark. Casting per segment paints a band
     * across every seam in the column — the tell being a stack that looks
     * *striped* rather than *lit*.
     */
    val castsShadow: Boolean = true,
)

/**
 * Draw a set of bars as bodies — the rectangular half of [drawVolumeArcs], same
 * five layers and the same two-pass rule.
 *
 * The extrusion runs **up and right** here rather than down and right: a column
 * stands on a baseline, so the faces a viewer sees are its top and its right
 * side. Same light, read from the other side of the object.
 */
fun DrawScope.drawVolumeBars(
    volume: GpChartVolume,
    bounds: Rect,
    bars: List<VolumeBar>,
) {
    if (bars.isEmpty()) return
    // ONE height for the whole set, taken off the narrowest body's WIDTH -- a
    // per-body height would light the chart from a different distance in every
    // column, and taking it off the height instead would make a three-pixel
    // segment extrude by nothing while its neighbour extrudes by ten. The clamp
    // is the other half of that: a body may not be extruded further than it is
    // tall, or the walls swallow the face.
    val height = (bars.minOf { it.rect.width } * volume.relief)
        .coerceAtMost(bars.minOf { it.rect.height } * 0.8f)
    val vx = height * LIGHT_X
    val vy = -height * LIGHT_Y

    if (volume.cast.alpha > 0f) {
        val offset = volume.castOffset.toPx()
        val spread = volume.castSpread.toPx()
        val steps = 3
        val step = volume.cast.alpha / steps
        bars.forEach { bar ->
            if (!bar.castsShadow) return@forEach
            for (i in 1..steps) {
                val grow = spread * i / steps
                drawRoundedBody(
                    rect = Rect(
                        left = bar.rect.left - grow,
                        top = bar.rect.top - grow + offset,
                        right = bar.rect.right + grow,
                        bottom = bar.rect.bottom + grow + offset,
                    ),
                    topRadius = bar.topRadius + grow,
                    bottomRadius = bar.bottomRadius + grow,
                    color = volume.cast.copy(alpha = step),
                    alpha = bar.alpha,
                )
            }
        }
    }

    if (volume.isRaised) {
        // Walls first for every body, faces second -- the same rule the arcs
        // follow, and for the same reason: a neighbour's wall must never land on
        // top of a face.
        bars.forEach { drawBarWalls(volume, it, vx, vy) }
    }
    bars.forEach { drawBarFace(volume, bounds, it) }

    if (volume.grain > 0f) {
        bars.forEach { bar ->
            drawRoundedBody(
                rect = bar.rect,
                topRadius = bar.topRadius,
                bottomRadius = bar.bottomRadius,
                brush = grainBrush,
                alpha = volume.grain * bar.alpha,
            )
        }
    }
}

/** The top surface and the right wall of one body. */
private fun DrawScope.drawBarWalls(
    volume: GpChartVolume,
    bar: VolumeBar,
    vx: Float,
    vy: Float,
) {
    val r = bar.rect
    // 1 · the TOP SURFACE, as the whole body offset by the extrusion vector.
    //
    // Two defects were found here in one render pass and the second is why this
    // is a silhouette rather than two quads. Sweeping the rect's **right edge**
    // into a parallelogram is correct for a square-cornered column and **wrong
    // for a pill**, whose right edge is entirely round: the quad lies outside the
    // shape and paints a black spike off the end of every bar. Replacing it with
    // a silhouette in the WALL tone then put a dark wedge in each column's
    // top-right corner -- the one region no quad covers -- which reads as a hole
    // rather than as a fillet.
    //
    // Both go away by drawing what is actually there. An extruded body's top
    // surface is its whole outline swept, corners included, and it is the surface
    // facing the light -- so it is **lit**, at the body's own radii, and there is
    // no separate top-face quad to leave a gap beside. `Observed:` 2026-08-22,
    // two consecutive passes.
    drawRoundedBody(
        rect = Rect(r.left + vx, r.top + vy, r.right + vx, r.bottom + vy),
        topRadius = bar.topRadius,
        bottomRadius = bar.bottomRadius,
        color = if (bar.capped) {
            volume.lit(bar.color, volume.tint * 0.7f)
        } else {
            // A segment with another sitting on it has no top to catch light, so
            // its silhouette is the side of the slab and takes the wall's tone.
            volume.shaded(bar.color, min(0.92f, volume.shade * 1.5f))
        },
        alpha = bar.alpha,
    )
    // 2 · the right wall, over the span that is actually a straight edge. Inset
    // by the radii so it stops where the caps begin; on a pill both insets meet
    // and this draws nothing, which is correct -- a pill has no flat side.
    val wallTop = r.top + bar.topRadius
    val wallBottom = r.bottom - bar.bottomRadius
    if (wallBottom > wallTop) {
        drawPath(
            path = quad(
                Offset(r.right, wallTop),
                Offset(r.right + vx, wallTop + vy),
                Offset(r.right + vx, wallBottom + vy),
                Offset(r.right, wallBottom),
            ),
            color = volume.shaded(bar.color, min(0.92f, volume.shade * 1.5f)),
            alpha = bar.alpha,
        )
    }
}

/** The front face: the three-stop fill, then the sheen strip along the lit edge. */
private fun DrawScope.drawBarFace(volume: GpChartVolume, bounds: Rect, bar: VolumeBar) {
    drawRoundedBody(
        rect = bar.rect,
        topRadius = bar.topRadius,
        bottomRadius = bar.bottomRadius,
        brush = volume.faceBrush(bar.color, bounds),
        alpha = bar.alpha,
    )
    if (volume.sheen > 0f) {
        // The prototype's own proportion: a strip 30% of the body's width down
        // its lit side, not a wash over the whole face.
        drawRoundedBody(
            rect = Rect(
                left = bar.rect.left,
                top = bar.rect.top,
                right = bar.rect.left + bar.rect.width * 0.30f,
                bottom = bar.rect.bottom,
            ),
            topRadius = bar.topRadius,
            bottomRadius = bar.bottomRadius,
            brush = sheenBrush(volume, bounds),
            alpha = bar.alpha,
        )
    }
}

// ──────────────────────────── path builders ────────────────────────────────

/**
 * A **closed annular sector** — the shape a slice actually is.
 *
 * A stroked arc has no outline, so it cannot have walls, an end or a fold. This
 * is the difference between painting a ring and building one, and it is why the
 * raised path cannot reuse `drawArc`.
 */
private fun sector(
    center: Offset,
    outer: Float,
    inner: Float,
    startAngle: Float,
    sweepAngle: Float,
): Path = Path().apply {
    val outerRect = Rect(center = center, radius = outer)
    val innerRect = Rect(center = center, radius = inner)
    arcTo(outerRect, startAngle, sweepAngle, forceMoveTo = true)
    lineTo(
        center.x + inner * cos(radians(startAngle + sweepAngle)),
        center.y + inner * sin(radians(startAngle + sweepAngle)),
    )
    arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, forceMoveTo = false)
    close()
}

/** One side wall: an arc at [radius], swept by the extrusion vector. */
private fun wallStrip(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    vx: Float,
    vy: Float,
): Path = Path().apply {
    val near = Rect(center = center, radius = radius)
    val far = Rect(center = Offset(center.x + vx, center.y + vy), radius = radius)
    arcTo(near, startAngle, sweepAngle, forceMoveTo = true)
    lineTo(
        center.x + vx + radius * cos(radians(startAngle + sweepAngle)),
        center.y + vy + radius * sin(radians(startAngle + sweepAngle)),
    )
    arcTo(far, startAngle + sweepAngle, -sweepAngle, forceMoveTo = false)
    close()
}

/**
 * The flat end of a slab.
 *
 * *"The caps are what make each block read as its own slab rather than as a
 * segment of one painted ring."*
 */
private fun endCap(
    center: Offset,
    outer: Float,
    inner: Float,
    angle: Float,
    vx: Float,
    vy: Float,
): Path {
    val c = cos(radians(angle))
    val s = sin(radians(angle))
    return quad(
        Offset(center.x + outer * c, center.y + outer * s),
        Offset(center.x + inner * c, center.y + inner * s),
        Offset(center.x + inner * c + vx, center.y + inner * s + vy),
        Offset(center.x + outer * c + vx, center.y + outer * s + vy),
    )
}

private fun quad(a: Offset, b: Offset, c: Offset, d: Offset): Path = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    lineTo(d.x, d.y)
    close()
}

/**
 * The channel, as a clip.
 *
 * `EvenOdd` and two circles: *"the clip is the channel annulus itself, so a body
 * physically cannot cross a wall of the groove."* It is a guarantee rather than a
 * crop — the body was already sized to fit — and that is exactly why it is cheap
 * to keep.
 */
private fun annulus(center: Offset, outer: Float, inner: Float): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addOval(Rect(center = center, radius = outer))
    addOval(Rect(center = center, radius = inner))
}

/** A rounded body whose two ends may take different radii. */
private fun DrawScope.drawRoundedBody(
    rect: Rect,
    topRadius: Float,
    bottomRadius: Float,
    color: Color? = null,
    brush: Brush? = null,
    alpha: Float = 1f,
) {
    val path = Path().apply {
        val t = min(topRadius, min(rect.width, rect.height) / 2f).coerceAtLeast(0f)
        val b = min(bottomRadius, min(rect.width, rect.height) / 2f).coerceAtLeast(0f)
        moveTo(rect.left, rect.bottom - b)
        lineTo(rect.left, rect.top + t)
        if (t > 0f) {
            arcTo(
                Rect(rect.left, rect.top, rect.left + t * 2f, rect.top + t * 2f),
                180f, 90f, false,
            )
        }
        lineTo(rect.right - t, rect.top)
        if (t > 0f) {
            arcTo(
                Rect(rect.right - t * 2f, rect.top, rect.right, rect.top + t * 2f),
                270f, 90f, false,
            )
        }
        lineTo(rect.right, rect.bottom - b)
        if (b > 0f) {
            arcTo(
                Rect(rect.right - b * 2f, rect.bottom - b * 2f, rect.right, rect.bottom),
                0f, 90f, false,
            )
        }
        lineTo(rect.left + b, rect.bottom)
        if (b > 0f) {
            arcTo(
                Rect(rect.left, rect.bottom - b * 2f, rect.left + b * 2f, rect.bottom),
                90f, 90f, false,
            )
        }
        close()
    }
    when {
        brush != null -> drawPath(path, brush, alpha = alpha)
        color != null -> drawPath(path, color, alpha = alpha)
    }
}

// ───────────────────────────── the two brushes ─────────────────────────────

/** White, hot at the lit corner and gone by 42% — the prototype's own stops. */
private fun sheenBrush(volume: GpChartVolume, bounds: Rect): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.White.copy(alpha = volume.sheen),
        0.42f to Color.White.copy(alpha = volume.sheen * 0.11f),
        1f to Color.Transparent,
    ),
    start = bounds.topLeft,
    end = Offset(bounds.left + bounds.width * 0.4f, bounds.bottom),
)

/**
 * The grain — **one 64x64 tile, built once per process, tiled for ever after.**
 *
 * ## The trap this exists to close
 *
 * `#57` c's brief names it: *"Do not let the grain pass cost a frame. A
 * `feTurbulence` equivalent per draw is the obvious performance trap; bake it
 * once."* So the noise is generated into a bitmap by `by lazy` and handed to an
 * [ImageShader] with [TileMode.Repeated]; a chart's grain layer is then a single
 * shader draw whatever its size, and the arithmetic below runs exactly once no
 * matter how many charts the app shows.
 *
 * `by lazy` and not a top-level eager `val` is also what keeps `ThemePaletteTest`
 * on the JVM: it builds every [GpChartVolume] in the matrix and never draws one,
 * so this bitmap is never constructed there and no Android class is loaded.
 *
 * ## Why it is neutral noise over `SrcOver`, and not soft-light
 *
 * The prototype composites its turbulence with `mix-blend-mode: soft-light`.
 * ⚠️ **Compose cannot promise that below API 29.** `BlendMode.Softlight` has no
 * `PorterDuff` equivalent, and Compose falls back to `PorterDuffXfermode` on
 * older canvases — this app is `minSdk 26`, so on a real device the mode would
 * be **silently dropped**, which is the same failure class as `BlurMaskFilter`
 * in `kb/dev/compose-soft-shadows-below-api-28.md`: no exception, just the wrong
 * picture. So the tile carries **both light and dark speckles around a neutral
 * mid** and is drawn plainly at low alpha, which is what soft-light over neutral
 * noise approximates anyway — and it renders identically on every device the app
 * ships to.
 */
private val grainBrush: ShaderBrush by lazy {
    val size = 64
    val pixels = IntArray(size * size)
    // A cheap deterministic hash rather than `Random`: the tile must be identical
    // in every process, or two screenshots of the same chart differ in the grain.
    var seed = 0x9E3779B9.toInt()
    for (i in pixels.indices) {
        seed = seed * 1664525 + 1013904223
        // Two octaves, coarse over fine, so it reads as a surface rather than as
        // television static.
        val fine = (seed ushr 24) and 0xFF
        val coarse = ((i / size) * 31 + (i % size) * 17 + (seed ushr 16)) and 0xFF
        val value = (fine * 3 + coarse) / 4
        // Centred on mid-grey: half the tile lightens, half darkens.
        val level = 96 + (value * 64) / 255
        pixels[i] = (0xFF shl 24) or (level shl 16) or (level shl 8) or level
    }
    val bitmap = android.graphics.Bitmap.createBitmap(
        pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888,
    )
    ShaderBrush(
        ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated),
    )
}

// ───────────────────────────────── helpers ─────────────────────────────────

private fun radians(degrees: Float): Float = (degrees * Math.PI / 180.0).toFloat()

/**
 * Linear mix toward [other], keeping this colour's alpha.
 *
 * The prototype's `mix()`, and deliberately **not** an HSL interpolation: the
 * stops it authors are RGB mixes toward white and toward a near-black, and an
 * HSL path between the same two endpoints bends through a different hue.
 */
internal fun Color.mixToward(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha,
    )
}
