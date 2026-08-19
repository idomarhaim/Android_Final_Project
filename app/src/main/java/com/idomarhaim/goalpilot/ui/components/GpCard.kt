package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The app's card.
 *
 * Material 3's filled `Card` fills with `surfaceContainerHighest`, which in a
 * light scheme is *darker* than the page — so a screen made of stacked cards
 * reads as a field of grey blocks with white gaps, which is exactly how this app
 * looked before. [GpCard] inverts that relationship: the page is a tinted canvas
 * and cards are the lightest thing on it, so they read as objects lifted off the
 * background rather than holes cut into it.
 *
 * In dark mode the relationship is the usual one — the card is lighter than the
 * page, because there is nothing above white to lift towards.
 */
@Composable
fun GpCard(
    modifier: Modifier = Modifier,
    colors: CardColors = gpCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier, colors = colors, content = content)
}

/** Clickable variant — same fill, plus the M3 ripple and click semantics. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = gpCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(onClick = onClick, modifier = modifier, colors = colors, content = content)
}

@Composable
fun gpCardColors(): CardColors =
    CardDefaults.cardColors(containerColor = gpCardContainerColor())

/** The card fill for the current scheme. Also used by chrome that should read as "a card". */
@Composable
@ReadOnlyComposable
fun gpCardContainerColor(): Color = with(MaterialTheme.colorScheme) {
    if (surface.luminance() < 0.5f) surfaceContainerHigh else surfaceContainerLowest
}
