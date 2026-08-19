package com.idomarhaim.goalpilot.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.idomarhaim.goalpilot.domain.model.WidgetArea
import com.idomarhaim.goalpilot.domain.model.WidgetDay
import kotlin.math.max
import kotlin.math.min

/**
 * The widget pack's charts, drawn with [Canvas] into bitmaps.
 *
 * ## Why bitmaps at all
 *
 * §4.5 states the constraint and this file is the consequence: *Glance has no
 * equivalent of the SVG specular bevel and turbulence used in the prototype — in
 * a widget that depth must come from a pre-rendered bitmap or from `Canvas`
 * drawing.* A Glance tree becomes a `RemoteViews`, which the launcher inflates
 * in **its own process**, so nothing from `ui/components/` reaches here: no
 * `DrawScope`, no `Modifier.blur`, and no `rememberChartProgress` — §4.5 again:
 * *a widget is not a live screen, so nothing animates, and
 * `ui/components/ChartAnimation.kt` does not run there.* These charts are drawn
 * once, at their finished value.
 *
 * ## Why they are small
 *
 * A `RemoteViews` and every bitmap in it cross a Binder transaction, and an
 * oversized one is not slow — it throws, on somebody else's launcher, where
 * nobody is watching. So every chart is capped at [MAX_PX] and left to scale:
 * a chart drawn at 320 px and stretched to 360 is indistinguishable, and a
 * chart that never renders is not.
 *
 * ## Why the ink is theme-neutral
 *
 * A `RemoteViews` can carry a day colour and a night colour for a text or a
 * background and let the launcher choose; it cannot carry two bitmaps. Baking a
 * scheme into these pixels therefore means being wrong half the time — which is
 * exactly what happened on 2026-08-16 before [ChartInk] existed. So every chart
 * is drawn on a **transparent** ground in translucent neutrals that read against
 * both grounds, and only the categorical life-area hues are opaque.
 *
 * ## What makes it neo
 *
 * Each shape is drawn three times — a **shadow** offset down-right, the fill
 * itself, and a **highlight** offset up-left — which is §4.1's *shadow pair on
 * one flat surface*, and the arcs sit in an **inset groove**. Every one then
 * takes a hairline `--edge` stroke, because §4.1 forbids an affordance that is
 * shadow-only: that is neo's known WCAG failure and it is the reason the rule
 * exists at all.
 */
object WidgetCharts {

    /**
     * Ceiling on any chart's largest dimension, in pixels.
     *
     * 320 px of `ARGB_8888` is 400 KB at worst. The launcher's own budget is
     * generous but shared across every widget on the screen, and this pack can
     * legitimately put five tiles on one home screen.
     */
    const val MAX_PX = 320

    /**
     * The time donut (§4.4 — it *stays*, with the honesty its footnote carries).
     *
     * Slice caps are **butt, not round**, which is the prototype's own note and
     * a real geometric constraint rather than taste: a round cap adds half the
     * stroke width past each endpoint, so at any thickness worth drawing the
     * caps swallow the gap that makes two neighbouring slices read as two.
     */
    fun donut(
        sizePx: Int,
        slices: List<WidgetArea>,
        ink: ChartInk,
    ): Bitmap {
        val size = sizePx.coerceIn(48, MAX_PX)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val thickness = max(8f, size * 0.19f)
        val inset = thickness / 2f + size * 0.045f
        val box = RectF(inset, inset, size - inset, size - inset)

        // The groove first: an inset track the arcs sit in, so a donut that is
        // mostly one area still reads as a ring rather than as a floating comma.
        val groove = paint(ink.groove).apply {
            style = Paint.Style.STROKE
            strokeWidth = thickness
            strokeCap = Paint.Cap.BUTT
        }
        canvas.drawOval(box, groove)

        val total = slices.sumOf { it.minutes }.toFloat()
        if (total <= 0f) return bitmap

        // A gap in degrees, not a fraction: at six slices a proportional gap eats
        // the smallest slice entirely, and the smallest slice is the one the user
        // is most likely to be surprised by.
        val gap = if (slices.size > 1) 2.5f else 0f
        var angle = START_ANGLE

        val shadowOffset = size * 0.012f
        for (slice in slices) {
            val sweep = (slice.minutes / total) * 360f - gap
            if (sweep <= 0.4f) {
                angle += (slice.minutes / total) * 360f
                continue
            }
            val color = ink.resolve(slice.colorHex)

            canvas.withTranslation(shadowOffset, shadowOffset) {
                drawArc(box, angle, sweep, false, arcPaint(ink.shadow, thickness))
            }
            canvas.drawArc(box, angle, sweep, false, arcPaint(color, thickness))
            // The lit edge: a thin bright arc along the top of the stroke rather
            // than a gloss over it, which is the difference between an extrusion
            // and a sticker.
            canvas.drawArc(
                RectF(box).apply { inset(thickness * 0.30f, thickness * 0.30f) },
                angle,
                sweep,
                false,
                arcPaint(ink.highlight, thickness * 0.22f),
            )
            angle += sweep + gap
        }

        // --edge, last so it sits over every arc.
        canvas.drawOval(
            RectF(box).apply { inset(-thickness / 2f, -thickness / 2f) },
            paint(ink.edge).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, size * 0.004f)
            },
        )
        return bitmap
    }

    /**
     * The stacked trend (§4.4 — `StackedColumnChart` *stays*).
     *
     * Stacks in [series] order, which is the donut's slice order, so the columns
     * add up to the donut by construction rather than by two implementations
     * agreeing. Columns are drawn against the **tallest column**, not against a
     * rounded axis maximum: a widget has no room for an axis, and a bar drawn
     * against an invisible scale is a bar that means nothing.
     */
    fun columns(
        widthPx: Int,
        heightPx: Int,
        series: List<WidgetArea>,
        days: List<WidgetDay>,
        ink: ChartInk,
    ): Bitmap {
        val w = widthPx.coerceIn(48, MAX_PX)
        val h = heightPx.coerceIn(24, MAX_PX)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (days.isEmpty()) return bitmap

        val gap = w * 0.035f / max(1, days.size - 1).toFloat()
        val slot = (w - gap * (days.size - 1)) / days.size
        val barWidth = min(slot, h * 0.42f)
        val radius = barWidth * 0.22f
        val peak = days.maxOf { it.totalMinutes }.coerceAtLeast(1)

        days.forEachIndexed { index, day ->
            val left = index * (slot + gap) + (slot - barWidth) / 2f
            val right = left + barWidth

            // Every column gets its groove, including an empty one — a missing
            // track reads as "no data was collected", a present empty one reads
            // as "that day, you did nothing", and only the second is true.
            canvas.drawRoundRect(RectF(left, 0f, right, h.toFloat()), radius, radius, paint(ink.groove))

            var y = h.toFloat()
            day.minutes.forEachIndexed { areaIndex, minutes ->
                if (minutes <= 0) return@forEachIndexed
                val segment = (minutes.toFloat() / peak) * h
                val top = (y - segment).coerceAtLeast(0f)
                val color = series.getOrNull(areaIndex)?.let { ink.resolve(it.colorHex) } ?: ink.accent
                canvas.drawRect(RectF(left, top, right, y), paint(color))
                y = top
            }

            val filled = (day.totalMinutes.toFloat() / peak) * h
            if (filled > 0f) {
                val top = (h - filled).coerceAtLeast(0f)
                // Round only the cap, and clip the stack to it, so a two-colour
                // column still ends in one shape.
                canvas.save()
                canvas.clipPath(
                    Path().apply {
                        addRoundRect(RectF(left, top, right, h.toFloat()), radius, radius, Path.Direction.CW)
                    },
                )
                canvas.drawRect(RectF(left, top, right, h.toFloat()), paint(ink.highlight.alphaScaled(0.35f)))
                canvas.restore()

                canvas.drawRoundRect(
                    RectF(left, top, right, h.toFloat()),
                    radius,
                    radius,
                    paint(ink.edge).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = max(1f, w * 0.004f)
                    },
                )
            }
        }
        return bitmap
    }

    /**
     * A progress ring — goals at `2×2`, and the level ring §4.4 demoted the
     * points hero into.
     */
    fun ring(
        sizePx: Int,
        fraction: Float,
        colorInt: Int,
        ink: ChartInk,
    ): Bitmap {
        val size = sizePx.coerceIn(32, MAX_PX)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val thickness = max(6f, size * 0.13f)
        val inset = thickness / 2f + size * 0.04f
        val box = RectF(inset, inset, size - inset, size - inset)

        canvas.drawOval(box, arcPaint(ink.groove, thickness))

        val sweep = fraction.coerceIn(0f, 1f) * 360f
        if (sweep > 0.5f) {
            val offset = size * 0.014f
            canvas.withTranslation(offset, offset) {
                drawArc(box, START_ANGLE, sweep, false, arcPaint(ink.shadow, thickness))
            }
            canvas.drawArc(box, START_ANGLE, sweep, false, arcPaint(colorInt, thickness))
            canvas.drawArc(
                RectF(box).apply { inset(thickness * 0.30f, thickness * 0.30f) },
                START_ANGLE,
                sweep,
                false,
                arcPaint(ink.highlight, thickness * 0.20f),
            )
        }

        canvas.drawOval(
            RectF(box).apply { inset(-thickness / 2f, -thickness / 2f) },
            paint(ink.edge).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, size * 0.005f)
            },
        )
        return bitmap
    }

    /**
     * One horizontal effort bar in its groove — the row body of the effort tile.
     *
     * Small enough (a few KB) that one per row costs nothing, which is why these
     * are not folded into a single strip: the rows carry text between them and a
     * shared bitmap would have to guess the text's height.
     */
    fun bar(
        widthPx: Int,
        heightPx: Int,
        fraction: Float,
        colorInt: Int,
        ink: ChartInk,
    ): Bitmap {
        val w = widthPx.coerceIn(16, MAX_PX)
        val h = heightPx.coerceIn(4, 64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = h / 2f

        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint(ink.groove))

        val filled = (fraction.coerceIn(0f, 1f) * w).coerceAtLeast(h.toFloat())
        canvas.drawRoundRect(RectF(0f, 0f, filled, h.toFloat()), radius, radius, paint(colorInt))
        canvas.drawRoundRect(
            RectF(0f, 0f, filled, h * 0.5f),
            radius,
            radius,
            paint(ink.highlight.alphaScaled(0.30f)),
        )
        canvas.drawRoundRect(
            RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f),
            radius,
            radius,
            paint(ink.edge).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
            },
        )
        return bitmap
    }

    /** 12 o'clock, so the biggest slice starts where the eye does. */
    private const val START_ANGLE = -90f

    private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun arcPaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.BUTT
    }
}

private inline fun Canvas.withTranslation(dx: Float, dy: Float, block: Canvas.() -> Unit) {
    save()
    translate(dx, dy)
    block()
    restore()
}

/** Scales a colour's existing alpha rather than replacing it. */
private fun Int.alphaScaled(factor: Float): Int {
    val a = (android.graphics.Color.alpha(this) * factor).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(
        a,
        android.graphics.Color.red(this),
        android.graphics.Color.green(this),
        android.graphics.Color.blue(this),
    )
}
