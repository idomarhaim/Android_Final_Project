package com.idomarhaim.goalpilot.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * The full-screen destination a shared photo opens into (issue `#4`).
 *
 * A `Dialog` rather than a nav route on purpose: the only argument is a download
 * URL, and a URL in a route has to be encoded on the way in and decoded on the way
 * out — two places for a query string or a signed-token `?alt=media` to be
 * mangled, in exchange for a back-stack entry nobody wants for a photo. The
 * dialog keeps the feed's scroll position underneath it and dismisses on Back for
 * free.
 *
 * [contentDescription] is required, not nullable: this composable exists because
 * the feed's photo was unreachable to a screen reader, and a viewer that repeated
 * that omission would have fixed one half of `#4` by adding a second instance of
 * the other half.
 */
@Composable
internal fun FullScreenPhotoDialog(
    imageUrl: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableFloatStateOf(MIN_SCALE) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var boxSize by remember { mutableStateOf(IntSize.Zero) }

        // Panning is only meaningful once the image is bigger than its box, and it
        // must stop at the point where the edge of the image reaches the edge of
        // the screen — an unclamped pan lets a pinch-zoomed photo be flung out of
        // view entirely, leaving a black rectangle and no way back but Back.
        fun clamp(candidate: Offset): Offset {
            val maxX = (boxSize.width * (scale - 1f) / 2f).coerceAtLeast(0f)
            val maxY = (boxSize.height * (scale - 1f) / 2f).coerceAtLeast(0f)
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { boxSize = it },
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    // Two gesture detectors, two pointerInput blocks: a single
                    // block can only host one detector, and the second would
                    // never see a pointer.
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            offset = if (scale == MIN_SCALE) Offset.Zero else clamp(offset + pan)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            // Double-tap is the gesture people actually use on a
                            // photo; pinch alone is a discoverability problem.
                            onDoubleTap = {
                                if (scale > MIN_SCALE) {
                                    scale = MIN_SCALE
                                    offset = Offset.Zero
                                } else {
                                    scale = DOUBLE_TAP_SCALE
                                }
                            },
                        )
                    },
            )
            IconButton(
                onClick = onDismiss,
                // The dialog covers the status bar, and nothing outside ui/root/
                // insets for it.
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(4.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close photo",
                    tint = Color.White,
                )
            }
        }
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
