package com.idomarhaim.goalpilot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/** One wedge of a [DonutChart]. [fraction] is its share of the whole, 0f..1f. */
data class DonutSlice(
    val id: String,
    val label: String,
    val fraction: Float,
    val color: Color,
)

/**
 * An animated, tappable donut chart.
 *
 * Three things make it worth ~150 lines of Canvas instead of a bar list:
 *
 * - **It draws itself.** The ring sweeps clockwise out of 12 o'clock over
 *   [rememberChartProgress]'s window, so the eye reads the proportions being laid
 *   down rather than being handed a finished picture.
 * - **It answers questions.** Tapping a wedge selects it: the wedge thickens and
 *   the rest fade back, and the caller renders the detail in [center]. Tapping the
 *   hole (or the selected wedge again) clears the selection.
 * - **It stays honest.** Butt caps, not round: a rounded cap would add a couple of
 *   degrees to every wedge and quietly inflate the small ones. The inter-wedge gap
 *   is taken *out of* each wedge for the same reason, and is dropped entirely when
 *   there is only one wedge, which would otherwise show a notch in a full circle.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    thickness: Dp = 34.dp,
    selectedId: String? = null,
    onSelect: (String?) -> Unit = {},
    contentDescription: String? = null,
    center: @Composable () -> Unit = {},
) {
    val sweepProgress by rememberChartProgress(key = slices)
    // One shared "is something selected" value: the wedge that owns it grows, the
    // others recede. Animating per-wedge would fight itself when selection moves.
    val selection by animateFloatAsState(
        targetValue = if (selectedId != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "donutSelection",
    )

    val density = LocalDensity.current
    val trackColor = Color.Gray.copy(alpha = 0.12f)
    // Captured before the semantics lambda: inside it, `contentDescription` is the
    // SemanticsPropertyReceiver's own write-only property, not this parameter.
    val description = contentDescription

    Box(
        modifier = modifier
            .size(size)
            // `this.` is load-bearing: the composable's own `contentDescription`
            // parameter would otherwise shadow the semantics property.
            .semantics { description?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(slices, selectedId) {
                    detectTapGestures { offset ->
                        val hit = sliceAt(
                            offset = offset,
                            canvasSize = this.size.width.toFloat() to this.size.height.toFloat(),
                            ringThicknessPx = with(density) { thickness.toPx() } * SELECTED_SCALE,
                            slices = slices,
                        )
                        // Tapping the selected wedge again clears it, so the chart
                        // can always be returned to its "everything" state without
                        // hunting for the hole.
                        onSelect(if (hit == null || hit == selectedId) null else hit)
                    }
                },
        ) {
            val baseStroke = with(density) { thickness.toPx() }
            val maxStroke = baseStroke * SELECTED_SCALE
            // Inset by the *largest* stroke any wedge can reach, so a wedge does
            // not clip against the canvas edge at the moment it is selected.
            val inset = maxStroke / 2f
            val arcSize = Size(this.size.width - maxStroke, this.size.height - maxStroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = baseStroke, cap = StrokeCap.Butt),
            )

            val gap = if (slices.size > 1) GAP_DEGREES else 0f
            val drawnSoFar = 360f * sweepProgress
            var cursor = 0f

            slices.forEach { slice ->
                val full = slice.fraction.coerceIn(0f, 1f) * 360f
                // Clip this wedge to however much of the circle has been drawn.
                val visible = (minOf(cursor + full, drawnSoFar) - cursor).coerceAtLeast(0f)
                if (visible > 0f) {
                    val isSelected = slice.id == selectedId
                    val stroke = if (isSelected) {
                        baseStroke + (maxStroke - baseStroke) * selection
                    } else {
                        baseStroke
                    }
                    drawArc(
                        color = slice.color.copy(
                            alpha = if (selectedId == null || isSelected) {
                                1f
                            } else {
                                1f - DIMMED_BY * selection
                            },
                        ),
                        startAngle = -90f + cursor,
                        sweepAngle = max(visible - gap, MIN_SWEEP_DEGREES.coerceAtMost(visible)),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                cursor += full
            }
        }
        center()
    }
}

/**
 * Which wedge a tap landed on, or null for the hole / outside the ring.
 *
 * Kept out of the composable and free of Compose types so the geometry can be
 * reasoned about (and tested) on its own: angles run clockwise from 12 o'clock,
 * which is where the chart starts drawing.
 */
internal fun sliceAt(
    offset: Offset,
    canvasSize: Pair<Float, Float>,
    ringThicknessPx: Float,
    slices: List<DonutSlice>,
): String? {
    val (width, height) = canvasSize
    val cx = width / 2f
    val cy = height / 2f
    val outer = minOf(width, height) / 2f
    val inner = (outer - ringThicknessPx).coerceAtLeast(0f)

    val dx = offset.x - cx
    val dy = offset.y - cy
    val radius = hypot(dx, dy)
    if (radius < inner || radius > outer) return null

    // atan2 gives -180..180 measured from 3 o'clock counter-clockwise; the chart
    // measures clockwise from 12 o'clock.
    val degrees = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f

    var cursor = 0f
    slices.forEach { slice ->
        val sweep = slice.fraction.coerceIn(0f, 1f) * 360f
        if (degrees >= cursor && degrees < cursor + sweep) return slice.id
        cursor += sweep
    }
    return null
}

/** How much thicker the selected wedge gets. */
private const val SELECTED_SCALE = 1.22f

/** Alpha removed from the wedges that are not selected. */
private const val DIMMED_BY = 0.6f

/** Breathing room between wedges, in degrees. */
private const val GAP_DEGREES = 1.6f

/** A wedge narrower than the gap still gets a hairline, so it is not invisible. */
private const val MIN_SWEEP_DEGREES = 0.8f
