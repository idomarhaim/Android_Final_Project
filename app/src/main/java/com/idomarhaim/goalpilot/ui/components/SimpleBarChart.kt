package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.core.util.bidiIsolated

/**
 * One row of [HorizontalBarChart].
 *
 * [trailing] is the finished label (e.g. "72 %"); [countUpTo] optionally lets the
 * number climb with the bar instead of sitting at its final value while the bar
 * is still growing — the row renders `countUpTo * progress` plus [countSuffix].
 *
 * A plain `String` suffix rather than a formatter lambda on purpose: this class is
 * the animation's restart key, and two structurally identical lambdas are not
 * `equals`, so a formatter here would restart every bar on every recomposition.
 */
data class BarItem(
    val label: String,
    val fraction: Float,
    val color: Color,
    val trailing: String,
    val countUpTo: Int? = null,
    val countSuffix: String = "",
)

/**
 * A lightweight, dependency-free horizontal bar chart used for the analytics
 * view (spec §6 Bonus: "position along the way to a goal").
 *
 * Bars grow from zero and are staggered top to bottom, so the chart reads as one
 * sweep rather than a table that blinks into existence. See [rememberChartProgress]
 * for why an [androidx.compose.animation.core.Animatable] and not
 * `animateFloatAsState`.
 */
@Composable
fun HorizontalBarChart(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    animateOnAppear: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEachIndexed { index, item ->
            // Keyed on the whole list: changing the data (a new range, a new goal)
            // replays the sweep, which is the cue that the numbers moved.
            val progress by rememberChartProgress(
                key = items to index,
                delayMillis = if (animateOnAppear) staggerDelay(index) else 0,
                durationMillis = if (animateOnAppear) DEFAULT_DURATION_MS else 0,
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Was a fixed 160dp, which clipped mid-length titles on a
                    // narrow screen and left a gap on a wide one.
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        // Both branches isolated, and isolating HERE rather than
                        // at each caller is the point of doing it in a shared
                        // component: `trailing` arrives already formatted ("72 %",
                        // "3.2 / 4 km") from eight screens, and a mixed
                        // digit-and-symbol run re-orders inside an RTL paragraph.
                        // Bidi.isolate is idempotent, so a caller that has
                        // already isolated its own string is not double-wrapped.
                        text = item.countUpTo
                            ?.let { "${Math.round(it * progress)}${item.countSuffix}".bidiIsolated() }
                            ?: item.trailing.bidiIsolated(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = item.color,
                    )
                }
                GpLinearProgress(
                    progress = item.fraction * progress,
                    color = item.color,
                    height = 10.dp,
                    // The growth is this chart's own animation; letting the
                    // progress bar animate towards each frame's value on top of it
                    // makes the bar lag its own number.
                    animate = false,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
