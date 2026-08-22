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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.ui.theme.VolumeBar
import com.idomarhaim.goalpilot.ui.theme.drawVolumeBars
import com.idomarhaim.goalpilot.ui.theme.gpMaterial

/**
 * One band of a [StackedColumn]. [id] is the series it belongs to, so the chart
 * can dim everything the user did not select without knowing what a life area is.
 *
 * [label] is §4.1's `.tag` — *a category is written in words beside its dot* — and
 * it is **required rather than defaulted**. A default of `""` would let a new call
 * site produce a wordless band and compile, which is precisely the state this
 * chart was in before `#53`'s sweep: the donut carried a `label` it never drew and
 * this class had no `label` at all, so under dark neo a stack of ten bands was ten
 * shades of one ramp with nothing to tell them apart. The three-argument
 * constructor breaking is the point of the change.
 */
data class StackedSegment(
    val id: String,
    val label: String,
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
 * - **A band that can hold its word holds it** (`#53`, §4.1's `.tag`). The band is
 *   the categorical mark here, and before this it carried no word at any size --
 *   [StackedSegment] had no `label` field to carry one with. Under dark neo the
 *   whole stack is one ramp, so *which band is which* had no answer on the chart
 *   at all. [segmentLabelFits] decides per band, per column, at the size the
 *   device is actually rendering type.
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
    // Measured, not budgeted by character count -- see `DonutChart`'s note: the
    // device's font scale is the variable, and it is the large-text device where
    // a wordless band matters most.
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    val labelPaddingPx = with(LocalDensity.current) { LABEL_PADDING.toPx() }

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

                            // `.tag`, after the bodies: the volume pass finishes
                            // with a grain tile over the whole column, and a word
                            // under it is a word behind a texture.
                            drawn.forEachIndexed { position, segment ->
                                if (segment.label.isBlank()) return@forEachIndexed
                                val rect = bodies[position].rect
                                // Constrained and ellipsized for the reason
                                // `DonutChart.wedgeLabelFits` gives at length: a
                                // long name must be SHORTENED, not dropped, or the
                                // rule goes quiet on exactly the bands nothing else
                                // tells apart.
                                val measured = textMeasurer.measure(
                                    text = segment.label,
                                    style = labelStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    constraints = Constraints(
                                        maxWidth = (rect.width - labelPaddingPx)
                                            .toInt().coerceAtLeast(0),
                                    ),
                                )
                                val fits = segmentLabelFits(
                                    bandWidthPx = rect.width,
                                    bandHeightPx = rect.height,
                                    textHeightPx = measured.size.height.toFloat(),
                                    paddingPx = labelPaddingPx,
                                )
                                if (!fits) return@forEachIndexed
                                drawText(
                                    textLayoutResult = measured,
                                    // Solved against the band's own fill by the
                                    // same solver `String.toGoalInk` uses, so one
                                    // rule serves every material.
                                    color = segment.color.asInkOn(listOf(segment.color)),
                                    topLeft = Offset(
                                        rect.left + (rect.width - measured.size.width) / 2f,
                                        rect.top + (rect.height - measured.size.height) / 2f,
                                    ),
                                    alpha = bodies[position].alpha,
                                )
                            }
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
 * Whether a band is big enough to hold its own word.
 *
 * Simpler than `DonutChart.wedgeLabelFits` -- a band is an axis-aligned rectangle
 * and so is the text box, so there is no rotation and no projection -- and it is a
 * separate named function for the same reason that one is: the interesting part is
 * not the arithmetic, it is that the answer is **no** for most bands on a phone,
 * and that fact should be visible and testable rather than buried in a draw pass.
 *
 * The caller measures against the band's width with `TextOverflow.Ellipsis`, so
 * this judges the **room** rather than the finished string. What it therefore
 * needs from the text is only its height, which is why no width reaches it.
 *
 * ## The residual this leaves, stated rather than hidden
 *
 * Seven columns across a phone gives each about 40 dp, and a week with ten life
 * areas in it gives most bands a few dp of height. So in practice this labels the
 * **dominant** band of a column and little else, and a quarter view (thirteen
 * columns) labels almost nothing. That is a real limit of a stacked column at
 * phone width rather than a shortfall in the sweep: there is no font size at which
 * a name fits in a 6 dp band, and shrinking the type until it did would trade a
 * legibility rule for a legibility failure.
 *
 * What carries identity for the unlabelled bands is **not** the legend: the chart
 * shares `selectedId` with the donut, so choosing a series dims every other band
 * to [DIMMED_ALPHA] and the shape that stays lit is the answer to *which one is
 * this*. The full per-band breakdown is in the chart's `contentDescription`. Named
 * in `CHANGELOG/2026-08-22/53-tag-sweep.md` as the one site where `.tag` cannot be
 * satisfied inline at every mark.
 *
 * Free of Compose types, exactly as [labelStride] is.
 */
internal fun segmentLabelFits(
    bandWidthPx: Float,
    bandHeightPx: Float,
    textHeightPx: Float,
    paddingPx: Float,
): Boolean {
    if (textHeightPx <= 0f) return false
    return textHeightPx + paddingPx <= bandHeightPx &&
        bandWidthPx - paddingPx >= textHeightPx * MIN_WORD_HEIGHTS
}

/**
 * The shortest a shortened word may get, in multiples of its own **height**.
 *
 * The same floor `DonutChart` uses and for the reason its KDoc gives: a glyph's
 * advance is about half a line height, so this is four or five glyphs plus the
 * ellipsis. Restated rather than shared because the two charts could legitimately
 * diverge on it, and a shared constant would hide that they had.
 */
private const val MIN_WORD_HEIGHTS = 2f

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

/** Breathing room a `.tag` word keeps from its band's edges. */
private val LABEL_PADDING = 6.dp
