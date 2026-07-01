package com.idomarhaim.goalpilot.ui.components

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/** Parses a "#RRGGBB" / "#AARRGGBB" hex string into a Compose [Color], or [fallback]. */
fun String.toComposeColor(fallback: Color = Color(0xFF718096)): Color =
    runCatching { Color(this.toColorInt()) }.getOrDefault(fallback)
