package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.ui.theme.VolumeBar
import com.idomarhaim.goalpilot.ui.theme.drawVolumeBars
import com.idomarhaim.goalpilot.ui.theme.gpMaterial

/**
 * One band of a [StackedColumn]. [id] is the series it belongs to, so the chart
 * can dim everything the user did not select without knowing what a life area is.
 */
data class StackedSegment(
    val id: String,
    val color: Color,
    val value: Int,
)

/**
 * One column: its axis label and its segments, **bottom-first**. The caller's
 * order is the stacking order, which is how the trend ends up stacked in the same
 * order the donut's legend lists.
 */
data class StackedColumn(
    val label: String,
    val segments: List<StackedSegment>,
) {
    val total: Int get() = segments.sumOf { it.value }
}

/**
 * A stacked column chart — the trend the donut cannot draw.
 *
 * The donut answers *"what share?"*; this answers *"which way is it going?"*, and
 * the two are deliberately built from the same numbers in the same order so the
 * legend under one reads the other.
 *
 * Three decisions worth keeping:
 *
 * - **Every column is scaled against [maxValue], never against its own total.**
 *   Per-column normalisation would make a day with ten minutes on it look exactly
 *   as full as a day with ten hours, which inverts the one question the chart
 *   exists to answer.
 * - **An empty column still draws a baseline tick.** A gap where a column should
 *   be reads as a rendering bug; a flat line reads as a day off, which is what it
 *   is.
 * - **Labels thin out rather than shrink.** Thirteen weeks in a quarter cannot
 *   each carry a legible date on a phone, so [labelStride] shows every *n*th and
 *   leaves the rest blank — the columns stay evenly spaced either way.
 * - **Segments are bodies, not blocks** (`#57` c). Each carries a three-stop fill
 *   lit from one direction, a sheen down its lit side, a cast shadow and a grain
 *   pass; on a raised relief it grows a right-hand wall, and the **top** face is
 *   drawn only on the segment that actually has one. That is why a column is one
 *   [androidx.compose.foundation.Canvas] rather than a `Column` of coloured
 *   boxes: the old shape could not express *"this segment has another sitting on
 *   it"*, and a lit plate through the middle of a stack is what you get when it
 *   cannot.
 */
@Composable
fun StackedColumnChart(
    columns: List<StackedColumn>,
    maxValue: Int,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    height: Dp = 132.dp,
    contentDescription: String? = null,
) {
    // Keyed on the data, so switching range re-draws the sweep instead of morphing
    // one quarter's shape into another's. See `rememberChartProgress` for why this
    // cannot be `animateFloatAsState`.
    val progress by rememberChartProgress(key = columns)
    val stride = labelStride(columns.size)
    val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    // Captured before the semantics lambda, where the name belongs to the
    // SemanticsPropertyReceiver rather than to this parameter.
    val description = contentDescription
    val safeMax = maxValue.coerceAtLeast(1)
    val volume = MaterialTheme.gpMaterial.volume

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { description?.let { this.contentDescription = it } },
        horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        columns.forEachIndexed { index, column ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clipToBounds(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (column.total <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(BASELINE_HEIGHT)
                                .clip(RoundedCornerShape(BASELINE_HEIGHT / 2))
                                .background(baselineColor),
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val corner = CORNER.toPx()
                            val minSegment = MIN_SEGMENT_HEIGHT.toPx()
                            // Bottom-first, exactly the caller's order -- a Canvas
                            // has no top-down layout to fight, so the reversal the
                            // old Column needed goes away with it.
                            val drawn = column.segments.filter { it.value > 0 }
                            var bottom = this.size.height
                            val bodies = drawn.mapIndexed { position, segment ->
                                val fraction = segment.value.toFloat() / safeMax
                                val bodyHeight = (this.size.height * fraction * progress)
                                    .coerceAtLeast(minSegment * progress)
                                val top = bottom - bodyHeight
                                val isTop = position == drawn.lastIndex
                                val rect = Rect(0f, top, this.size.width, bottom)
                                bottom = top
                                VolumeBar(
                                    rect = rect,
                                    color = segment.color,
                                    alpha = if (selectedId == null || segment.id == selectedId) {
                                        1f
                                    } else {
                                        DIMMED_ALPHA
                                    },
                                    // Only the highest segment is rounded, and only
                                    // it is capped: everything below has a neighbour
                                    // sitting on it, so it has no top to show.
                                    topRadius = if (isTop) corner else 0f,
                                    bottomRadius = 0f,
                                    capped = isTop,
                                    // One cast for the column, from the segment
                                    // that actually touches the baseline.
                                    castsShadow = position == 0,
                                )
                            }
                            drawVolumeBars(
                                volume = volume,
                                // The column's OWN box, not the chart's. As far as
                                // the light is concerned a stacked chart is a row
                                // of charts -- one rect spanning all seven would
                                // light Sunday and Saturday from opposite ends of
                                // the same gradient.
                                bounds = Rect(Offset.Zero, this.size),
                                bars = bodies,
                            )
                        }
                    }
                }
                Text(
                    // Blank rather than absent: an omitted label would let the
                    // labelled columns grow taller than the unlabelled ones.
                    text = if (index % stride == 0) column.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Show every *n*th label, so at most [maxLabels] of [count] columns carry one.
 *
 * Free of Compose types on purpose: label crowding is arithmetic, and arithmetic
 * belongs in a JVM test rather than behind an emulator.
 */
internal fun labelStride(count: Int, maxLabels: Int = MAX_LABELS): Int {
    if (count <= maxLabels || maxLabels <= 0) return 1
    // Ceiling division: 13 columns into 7 labels is every 2nd, not every 1.86th.
    return (count + maxLabels - 1) / maxLabels
}

/** Most axis labels a phone-width chart can carry without them touching. */
internal const val MAX_LABELS = 7

/** Alpha left on the series that are not selected. */
private const val DIMMED_ALPHA = 0.22f

private val COLUMN_GAP = 4.dp
private val CORNER = 4.dp
private val BASELINE_HEIGHT = 2.dp

/** A non-zero segment never rounds away to nothing. */
private val MIN_SEGMENT_HEIGHT = 3.dp
