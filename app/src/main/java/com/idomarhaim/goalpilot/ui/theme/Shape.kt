package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner scale. Rounder than the Material 3 baseline (12dp → 20dp at `medium`,
 * which is what `Card` uses) because the app is almost entirely cards stacked in
 * a scroll: softer corners separate them visually without needing heavier
 * elevation or dividers.
 */
val GpShapes = Shapes(
    // Text fields and menus read from `extraSmall`; the Material default of 4dp
    // looked boxy sitting inside a 20dp card.
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
