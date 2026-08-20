package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpSurface

/**
 * The app's card — and, since `C12`, **the app's one raised panel**.
 *
 * Material 3's filled `Card` fills with `surfaceContainerHighest`, which in a
 * light scheme is *darker* than the page — so a screen made of stacked cards
 * reads as a field of grey blocks with white gaps, which is exactly how this
 * app looked before. [GpCard] inverts that relationship: the page is a tinted
 * canvas and cards are the lightest thing on it, so they read as objects lifted
 * off the background rather than holes cut into it.
 *
 * ## Why it no longer draws an M3 `Card`
 *
 * Spec §4.1's material contract says a screen specifies
 * `surface · groove · elevation · accent` and **each material answers those
 * four its own way**. An M3 `Card` answers all four itself — one opaque tonal
 * fill and one tonal elevation — which is a fifth answer competing with the
 * four, and under neo it is the wrong one outright: neo's panel is the *same*
 * colour as the page, extruded by a shadow pair, and a tonal card fill destroys
 * that reading by drawing the boundary the shadow was supposed to draw.
 *
 * So the fill, the depth and the hairline `--edge` all come from
 * `Modifier.gpSurface`, and this composable is what makes the material reach
 * every screen without any of them naming a material. There are 30-odd call
 * sites and none of them changed.
 *
 * @param colors `null` — the default — means **the material decides**. Pass a
 *   value only where the card is deliberately emphasised against its siblings;
 *   the shadow, the corner and the edge still come from the material, so an
 *   override changes the fill and nothing else.
 */
@Composable
fun GpCard(
    modifier: Modifier = Modifier,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GpPanel(modifier = modifier, colors = colors, content = content)
}

/** Clickable variant — same surface, plus click semantics. */
@Composable
fun GpCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GpPanel(modifier = modifier, colors = colors, onClick = onClick, content = content)
}

@Composable
private fun GpPanel(
    modifier: Modifier,
    colors: CardColors?,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spec = MaterialTheme.gpMaterial
    val shape = RoundedCornerShape(spec.corner)

    // An override replaces the FILL and nothing else -- the depth, the corner
    // and the edge are the material's answers and stay the material's.
    val resolved = colors?.let {
        spec.copy(surface = it.containerColor, surfaceEnd = it.containerColor)
    } ?: spec

    val contentColor = colors?.contentColor?.takeIf { it != Color.Unspecified }
        ?: MaterialTheme.colorScheme.onSurface

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = modifier
                .gpSurface(resolved, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            content = content,
        )
    }
}

/**
 * The M3 `CardColors` for the current scheme.
 *
 * Kept for the two places that need a card-shaped *fill* without being a
 * [GpCard] — a `ModalBottomSheet` container and one emphasised social row. Not
 * the default any more; see [GpCard]'s `colors` parameter.
 */
@Composable
fun gpCardColors(): CardColors =
    CardDefaults.cardColors(containerColor = gpCardContainerColor())

/** The card fill for the current scheme. Also used by chrome that should read as "a card". */
@Composable
@ReadOnlyComposable
fun gpCardContainerColor(): Color = with(MaterialTheme.colorScheme) {
    if (surface.luminance() < 0.5f) surfaceContainerHigh else surfaceContainerLowest
}
