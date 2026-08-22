package com.idomarhaim.goalpilot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.ui.theme.VolumeArc
import com.idomarhaim.goalpilot.ui.theme.drawVolumeArcs
import com.idomarhaim.goalpilot.ui.theme.gpMaterial

/**
 * An animated circular progress ring (spec §6 Core: "visual progress display
 * (rings/bars)"). Optional [center] content is drawn in the middle.
 *
 * Pass [brush] to sweep the arc through the brand gradient; [color] stays the
 * fallback for goal-accent rings, which are a single user-chosen hue.
 *
 * ## Volume, and the one case that opts out (`#57` c)
 *
 * The arc is a **body**: a three-stop fill lit from the top left, a sheen along
 * the lit edge, a cast shadow and a grain pass — and a solid with real side walls
 * when the relief is raised. It comes from `MaterialTheme.gpMaterial.volume`, so
 * this file never asks which material it is in.
 *
 * ⚠️ **A [brush] caller keeps the flat stroke, deliberately.** The brand gradient
 * is a two-stop sweep the caller authored to mean something (dark neo's accent
 * *is* that gradient), and the volume pass would repaint it as a shade of one
 * colour — so where a brush is given, the brush wins and the arc is drawn as it
 * always was. That is a real gap and it is one call: `GoalDetailScreen`'s hero
 * ring. Stated here rather than left to be found.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.15f),
    brush: Brush? = null,
    center: @Composable (() -> Unit)? = null,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "ringProgress",
    )
    val volume = androidx.compose.material3.MaterialTheme.gpMaterial.volume
    Box(
        modifier = modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            // The TRACK keeps its round cap; only the arc becomes a body. A track
            // is a groove, and a groove has no ends to show.
            val trackStroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            val inset = strokePx / 2
            val arcSize = Size(
                width = this.size.width - strokePx,
                height = this.size.height - strokePx,
            )
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = trackStroke,
            )
            // A round-capped arc under ~half a percent paints as a lone dot at
            // 12 o'clock, which reads as a rendering artefact next to a "0%"
            // label. Below that, draw nothing.
            if (animated < 0.005f) return@Canvas
            if (brush != null) {
                // See the header: an authored gradient outranks the volume pass.
                drawArc(
                    brush = brush,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = trackStroke,
                )
                return@Canvas
            }
            drawVolumeArcs(
                volume = volume,
                bounds = Rect(Offset.Zero, this.size),
                center = Offset(this.size.width / 2f, this.size.height / 2f),
                radius = (this.size.minDimension - strokePx) / 2f,
                channel = strokePx,
                arcs = listOf(
                    VolumeArc(
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        color = color,
                        thickness = strokePx,
                    ),
                ),
            )
        }
        center?.invoke()
    }
}
