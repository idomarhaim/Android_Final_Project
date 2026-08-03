package com.idomarhaim.goalpilot.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

/**
 * 0f → 1f the moment a chart appears, and again whenever [key] changes.
 *
 * Charts in this app are *drawn*, not just shown: every bar grows out of the
 * baseline and every arc sweeps out of 12 o'clock, so the eye follows the
 * magnitude instead of being handed a finished picture.
 *
 * `animateFloatAsState` cannot do this. It initialises **at** its target on first
 * composition, so a bar whose value never changes never animates — which is why
 * the analytics screen used to snap fully-formed into place. An [Animatable]
 * started explicitly at zero is the difference.
 *
 * @param key restart handle. Pass the data the chart is drawn from, so switching
 *   the range (day → week → …) re-draws rather than silently morphing.
 * @param delayMillis stagger for lists — bar *n* passing `n * 70` reads as one
 *   sweep across the chart rather than every bar jumping at once.
 */
@Composable
fun rememberChartProgress(
    key: Any? = Unit,
    durationMillis: Int = DEFAULT_DURATION_MS,
    delayMillis: Int = 0,
): State<Float> {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            // Decelerating: fast enough to feel responsive, slow at the end so the
            // final value is legible rather than arriving with a snap.
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = EaseOutCubic,
            ),
        )
    }
    return progress.asState()
}

/** Default growth time for a chart element. Long enough to read, short enough not to wait on. */
const val DEFAULT_DURATION_MS = 900

/** Per-item stagger in a list of bars. */
const val STAGGER_STEP_MS = 70

/** Cap on the stagger so a twenty-goal chart does not take two seconds to finish. */
const val STAGGER_MAX_MS = 420

/** Stagger delay for item [index], clamped by [STAGGER_MAX_MS]. */
fun staggerDelay(index: Int): Int = (index * STAGGER_STEP_MS).coerceAtMost(STAGGER_MAX_MS)
