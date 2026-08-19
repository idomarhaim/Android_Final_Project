package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.idomarhaim.goalpilot.ui.theme.gpAccents

/**
 * The brand surface: the skin's three-stop gradient with a soft light source in
 * the top-left, and [content] drawn on top in the skin's `onHero` colour.
 *
 * Used for the one element per screen that should carry the brand — the sign-in
 * backdrop and the dashboard's points card. Everything else stays on neutral
 * surfaces; a screen where every card is a gradient reads as noise.
 */
@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable BoxScope.() -> Unit,
) {
    val accents = MaterialTheme.gpAccents
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = accents.heroGradient,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset.Zero,
                    radius = 620f,
                ),
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides accents.onHero) {
            content()
        }
    }
}
