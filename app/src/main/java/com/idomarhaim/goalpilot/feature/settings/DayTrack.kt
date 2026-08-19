package com.idomarhaim.goalpilot.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_DAY

/**
 * A midnight-to-midnight track with the awake span banded on it and the
 * planning time marked — the picture under §4.9's *Your day* section.
 *
 * ## §4.9's third design rule, which this component is the reason for
 *
 * > **A mark that carries meaning is filled *and* outlined**, generalising
 * > `C6`'s `--edge` from affordances: the 24 h track's awake band was fill-only
 * > and vanished in neo light.
 *
 * So the band is drawn twice — a fill and a stroke of a *different* role colour
 * — and the planning marker likewise. A fill alone survives exactly the
 * palettes it was drawn against; the outline is what keeps it a mark rather
 * than a slightly different shade of card.
 *
 * ## Why it wraps rather than refusing to
 *
 * `22:00 – 06:00` is a real answer, so a span whose end precedes its start is
 * drawn as **two** segments rather than as nothing. The alternative — clamping
 * the picker so the case cannot arise — silently tells a night-shift user their
 * day is wrong; see [com.idomarhaim.goalpilot.domain.model.WakingHours].
 *
 * The whole thing is one semantics node carrying a sentence, because a screen
 * reader gets nothing from a `Canvas` and the sentence is already written on
 * the consequence line beside it.
 */
@Composable
fun DayTrack(
    schedule: DaySchedule,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.surfaceVariant
    val bandFill = scheme.primaryContainer
    val bandEdge = scheme.primary
    val markerFill = scheme.tertiaryContainer
    val markerEdge = scheme.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .semantics { this.contentDescription = contentDescription },
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = trackColor, cornerRadius = radius)

            segmentsOf(schedule.waking.startMinutes, schedule.waking.endMinutes)
                .forEach { (fromFraction, toFraction) ->
                    drawBand(fromFraction, toFraction, bandFill, bandEdge, radius)
                }

            drawMarker(
                fraction = schedule.planningMinutes.toFloat() / MINUTES_PER_DAY,
                fill = markerFill,
                edge = markerEdge,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TrackTick("00")
            TrackTick("06")
            TrackTick("12")
            TrackTick("18")
            TrackTick("24")
        }
    }
}

@Composable
private fun TrackTick(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * The awake span as `0f..1f` fractions of the day — one segment normally, two
 * when it wraps past midnight, none when start and end coincide (an empty span,
 * which is what the user said; see `WakingHours.lengthMinutes`).
 */
private fun segmentsOf(startMinutes: Int, endMinutes: Int): List<Pair<Float, Float>> {
    val start = Math.floorMod(startMinutes, MINUTES_PER_DAY)
    val end = Math.floorMod(endMinutes, MINUTES_PER_DAY)
    val day = MINUTES_PER_DAY.toFloat()
    return when {
        start == end -> emptyList()
        start < end -> listOf(start / day to end / day)
        else -> listOf(start / day to 1f, 0f to end / day)
    }
}

private fun DrawScope.drawBand(
    fromFraction: Float,
    toFraction: Float,
    fill: androidx.compose.ui.graphics.Color,
    edge: androidx.compose.ui.graphics.Color,
    radius: CornerRadius,
) {
    val left = size.width * fromFraction
    val width = size.width * (toFraction - fromFraction)
    if (width <= 0f) return
    val topLeft = Offset(left, 0f)
    val bandSize = Size(width, size.height)
    drawRoundRect(color = fill, topLeft = topLeft, size = bandSize, cornerRadius = radius)
    drawRoundRect(
        color = edge,
        topLeft = topLeft,
        size = bandSize,
        cornerRadius = radius,
        style = Stroke(width = EDGE_STROKE.toPx()),
    )
}

private fun DrawScope.drawMarker(
    fraction: Float,
    fill: androidx.compose.ui.graphics.Color,
    edge: androidx.compose.ui.graphics.Color,
) {
    val markerWidth = MARKER_WIDTH.toPx()
    // Kept inside the track at both ends: a marker at 23:59 drawn on its centre
    // would sit half outside the rounded cap and read as clipped rather than late.
    val centre = (size.width * fraction).coerceIn(markerWidth, size.width - markerWidth)
    val topLeft = Offset(centre - markerWidth / 2f, 0f)
    val markerSize = Size(markerWidth, size.height)
    drawRect(color = fill, topLeft = topLeft, size = markerSize)
    drawRect(
        color = edge,
        topLeft = topLeft,
        size = markerSize,
        style = Stroke(width = EDGE_STROKE.toPx()),
    )
}

private val TRACK_HEIGHT = 22.dp
private val EDGE_STROKE = 1.5.dp
private val MARKER_WIDTH = 5.dp
