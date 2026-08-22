package com.idomarhaim.goalpilot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.ui.theme.GpChartVolume
import com.idomarhaim.goalpilot.ui.theme.VolumeBar
import com.idomarhaim.goalpilot.ui.theme.drawVolumeBars

/**
 * The app's progress bar: fully rounded, thicker than Material's 4dp hairline,
 * and animated on change.
 *
 * Material 3's own `LinearProgressIndicator` draws a square-capped track with a
 * stop dot; at the sizes this app uses it reads as a loading spinner rather than
 * an achievement. This one is a plain filled pill, which is what "72 % of the
 * way to your goal" should look like.
 *
 * Carries [ProgressBarRangeInfo] semantics so TalkBack still announces it as a
 * progress bar, exactly as the Material widget would.
 *
 * ## [volume] is opt-in, and that is the whole design (`#57` c)
 *
 * Pass a [GpChartVolume] and the fill becomes a **body** — graded, sheened, cast,
 * and extruded on a raised relief. Leave it `null` and nothing changes.
 *
 * Opt-in rather than read off `MaterialTheme.gpMaterial` here, because this pill
 * is **not only a chart**: it is the goal card's progress, the milestone row's,
 * the day-plan meter's. `#57` c's ticket is about *graphs*, and a setting that
 * silently extruded every progress bar in the app would be that axis reaching
 * past what Ido asked it to move. So [HorizontalBarChart] passes it and the other
 * eight call sites do not.
 */
@Composable
fun GpLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.16f),
    brush: Brush? = null,
    height: Dp = 10.dp,
    animate: Boolean = true,
    volume: GpChartVolume? = null,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (animate) 700 else 0),
        label = "gpLinearProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        // A zero-width rounded box still paints two half-caps, i.e. a visible dot
        // at 0 %. Skip the fill entirely instead.
        if (animated > 0f) {
            if (volume != null && brush == null) {
                // A body, drawn by the same helper the donut and the columns use.
                // `brush == null` for the same reason `ProgressRing` gives: an
                // authored gradient means something the volume pass would repaint.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val fillWidth = this.size.width * animated
                    if (fillWidth <= 0f) return@Canvas
                    val radius = this.size.height / 2f
                    drawVolumeBars(
                        volume = volume,
                        bounds = Rect(Offset.Zero, this.size),
                        bars = listOf(
                            VolumeBar(
                                rect = Rect(0f, 0f, fillWidth, this.size.height),
                                color = color,
                                topRadius = radius,
                                bottomRadius = radius,
                            ),
                        ),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .then(
                            if (brush != null) {
                                Modifier.background(brush)
                            } else {
                                Modifier.background(color)
                            },
                        ),
                )
            }
        }
    }
}
