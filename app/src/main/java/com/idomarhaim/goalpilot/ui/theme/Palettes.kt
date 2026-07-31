package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.idomarhaim.goalpilot.domain.model.AppSkin

/**
 * Brand colours that have no Material 3 role — gradients and the "good news"
 * accent. Supplied through [LocalGpAccents] so a screen reads them the same way
 * it reads `MaterialTheme.colorScheme`, and they re-resolve automatically when
 * the skin or dark mode changes.
 */
@Immutable
data class GpAccents(
    /** Three-stop brand sweep used by the sign-in screen and the dashboard hero. */
    val heroGradient: List<Color>,
    /** Text/icon colour that clears AA against *every* stop of [heroGradient]. */
    val onHero: Color,
    /** Same, dimmed for supporting copy. */
    val onHeroVariant: Color,
    /** Streaks, completions, "you're ahead" — never reuse `primary` for these. */
    val positive: Color,
)

private val AuroraLightScheme = lightColorScheme(
    primary = AuroraPrimary,
    onPrimary = AuroraOnPrimary,
    primaryContainer = AuroraPrimaryContainer,
    onPrimaryContainer = AuroraOnPrimaryContainer,
    inversePrimary = AuroraInversePrimary,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    secondaryContainer = AuroraSecondaryContainer,
    onSecondaryContainer = AuroraOnSecondaryContainer,
    tertiary = AuroraTertiary,
    onTertiary = AuroraOnTertiary,
    tertiaryContainer = AuroraTertiaryContainer,
    onTertiaryContainer = AuroraOnTertiaryContainer,
    background = AuroraBackground,
    onBackground = AuroraOnBackground,
    surface = AuroraSurface,
    onSurface = AuroraOnSurface,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = AuroraOnSurfaceVariant,
    surfaceTint = AuroraPrimary,
    inverseSurface = AuroraInverseSurface,
    inverseOnSurface = AuroraInverseOnSurface,
    error = GpError,
    onError = GpOnError,
    errorContainer = GpErrorContainer,
    onErrorContainer = GpOnErrorContainer,
    outline = AuroraOutline,
    outlineVariant = AuroraOutlineVariant,
    scrim = GpScrim,
    surfaceBright = AuroraSurfaceBright,
    surfaceDim = AuroraSurfaceDim,
    surfaceContainer = AuroraSurfaceContainer,
    surfaceContainerHigh = AuroraSurfaceContainerHigh,
    surfaceContainerHighest = AuroraSurfaceContainerHighest,
    surfaceContainerLow = AuroraSurfaceContainerLow,
    surfaceContainerLowest = AuroraSurfaceContainerLowest,
)

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraDarkPrimary,
    onPrimary = AuroraDarkOnPrimary,
    primaryContainer = AuroraDarkPrimaryContainer,
    onPrimaryContainer = AuroraDarkOnPrimaryContainer,
    inversePrimary = AuroraPrimary,
    secondary = AuroraDarkSecondary,
    onSecondary = AuroraDarkOnSecondary,
    secondaryContainer = AuroraDarkSecondaryContainer,
    onSecondaryContainer = AuroraDarkOnSecondaryContainer,
    tertiary = AuroraDarkTertiary,
    onTertiary = AuroraDarkOnTertiary,
    tertiaryContainer = AuroraDarkTertiaryContainer,
    onTertiaryContainer = AuroraDarkOnTertiaryContainer,
    background = AuroraDarkBackground,
    onBackground = AuroraDarkOnBackground,
    surface = AuroraDarkSurface,
    onSurface = AuroraDarkOnSurface,
    surfaceVariant = AuroraDarkSurfaceVariant,
    onSurfaceVariant = AuroraDarkOnSurfaceVariant,
    surfaceTint = AuroraDarkPrimary,
    inverseSurface = AuroraDarkInverseSurface,
    inverseOnSurface = AuroraDarkInverseOnSurface,
    error = GpDarkError,
    onError = GpDarkOnError,
    errorContainer = GpDarkErrorContainer,
    onErrorContainer = GpDarkOnErrorContainer,
    outline = AuroraDarkOutline,
    outlineVariant = AuroraDarkOutlineVariant,
    scrim = GpScrim,
    surfaceBright = AuroraDarkSurfaceBright,
    surfaceDim = AuroraDarkSurfaceDim,
    surfaceContainer = AuroraDarkSurfaceContainer,
    surfaceContainerHigh = AuroraDarkSurfaceContainerHigh,
    surfaceContainerHighest = AuroraDarkSurfaceContainerHighest,
    surfaceContainerLow = AuroraDarkSurfaceContainerLow,
    surfaceContainerLowest = AuroraDarkSurfaceContainerLowest,
)

private val BlossomLightScheme = lightColorScheme(
    primary = BlossomPrimary,
    onPrimary = BlossomOnPrimary,
    primaryContainer = BlossomPrimaryContainer,
    onPrimaryContainer = BlossomOnPrimaryContainer,
    inversePrimary = BlossomInversePrimary,
    secondary = BlossomSecondary,
    onSecondary = BlossomOnSecondary,
    secondaryContainer = BlossomSecondaryContainer,
    onSecondaryContainer = BlossomOnSecondaryContainer,
    tertiary = BlossomTertiary,
    onTertiary = BlossomOnTertiary,
    tertiaryContainer = BlossomTertiaryContainer,
    onTertiaryContainer = BlossomOnTertiaryContainer,
    background = BlossomBackground,
    onBackground = BlossomOnBackground,
    surface = BlossomSurface,
    onSurface = BlossomOnSurface,
    surfaceVariant = BlossomSurfaceVariant,
    onSurfaceVariant = BlossomOnSurfaceVariant,
    surfaceTint = BlossomPrimary,
    inverseSurface = BlossomInverseSurface,
    inverseOnSurface = BlossomInverseOnSurface,
    error = GpError,
    onError = GpOnError,
    errorContainer = GpErrorContainer,
    onErrorContainer = GpOnErrorContainer,
    outline = BlossomOutline,
    outlineVariant = BlossomOutlineVariant,
    scrim = GpScrim,
    surfaceBright = BlossomSurfaceBright,
    surfaceDim = BlossomSurfaceDim,
    surfaceContainer = BlossomSurfaceContainer,
    surfaceContainerHigh = BlossomSurfaceContainerHigh,
    surfaceContainerHighest = BlossomSurfaceContainerHighest,
    surfaceContainerLow = BlossomSurfaceContainerLow,
    surfaceContainerLowest = BlossomSurfaceContainerLowest,
)

private val BlossomDarkScheme = darkColorScheme(
    primary = BlossomDarkPrimary,
    onPrimary = BlossomDarkOnPrimary,
    primaryContainer = BlossomDarkPrimaryContainer,
    onPrimaryContainer = BlossomDarkOnPrimaryContainer,
    inversePrimary = BlossomPrimary,
    secondary = BlossomDarkSecondary,
    onSecondary = BlossomDarkOnSecondary,
    secondaryContainer = BlossomDarkSecondaryContainer,
    onSecondaryContainer = BlossomDarkOnSecondaryContainer,
    tertiary = BlossomDarkTertiary,
    onTertiary = BlossomDarkOnTertiary,
    tertiaryContainer = BlossomDarkTertiaryContainer,
    onTertiaryContainer = BlossomDarkOnTertiaryContainer,
    background = BlossomDarkBackground,
    onBackground = BlossomDarkOnBackground,
    surface = BlossomDarkSurface,
    onSurface = BlossomDarkOnSurface,
    surfaceVariant = BlossomDarkSurfaceVariant,
    onSurfaceVariant = BlossomDarkOnSurfaceVariant,
    surfaceTint = BlossomDarkPrimary,
    inverseSurface = BlossomDarkInverseSurface,
    inverseOnSurface = BlossomDarkInverseOnSurface,
    error = GpDarkError,
    onError = GpDarkOnError,
    errorContainer = GpDarkErrorContainer,
    onErrorContainer = GpDarkOnErrorContainer,
    outline = BlossomDarkOutline,
    outlineVariant = BlossomDarkOutlineVariant,
    scrim = GpScrim,
    surfaceBright = BlossomDarkSurfaceBright,
    surfaceDim = BlossomDarkSurfaceDim,
    surfaceContainer = BlossomDarkSurfaceContainer,
    surfaceContainerHigh = BlossomDarkSurfaceContainerHigh,
    surfaceContainerHighest = BlossomDarkSurfaceContainerHighest,
    surfaceContainerLow = BlossomDarkSurfaceContainerLow,
    surfaceContainerLowest = BlossomDarkSurfaceContainerLowest,
)

private val AuroraGradient = listOf(AuroraHeroStart, AuroraHeroMid, AuroraHeroEnd)
private val BlossomGradient = listOf(BlossomHeroStart, BlossomHeroMid, BlossomHeroEnd)

/** The Material 3 scheme for [skin] in the requested brightness. */
fun colorSchemeFor(skin: AppSkin, darkTheme: Boolean): ColorScheme = when (skin) {
    AppSkin.AURORA -> if (darkTheme) AuroraDarkScheme else AuroraLightScheme
    AppSkin.BLOSSOM -> if (darkTheme) BlossomDarkScheme else BlossomLightScheme
}

/**
 * The off-Material brand accents for [skin].
 *
 * The gradient itself does not change between light and dark: it is the brand
 * mark, and it already carries its own contrast. Only [GpAccents.positive]
 * lightens in dark mode, where a tone-40 green would be unreadable.
 */
fun accentsFor(skin: AppSkin, darkTheme: Boolean): GpAccents = when (skin) {
    AppSkin.AURORA -> GpAccents(
        heroGradient = AuroraGradient,
        onHero = Color.White,
        onHeroVariant = Color.White.copy(alpha = 0.82f),
        positive = if (darkTheme) AuroraDarkPositive else AuroraPositive,
    )

    AppSkin.BLOSSOM -> GpAccents(
        heroGradient = BlossomGradient,
        onHero = Color.White,
        onHeroVariant = Color.White.copy(alpha = 0.82f),
        positive = if (darkTheme) BlossomDarkPositive else BlossomPositive,
    )
}

/** Swatch colours for the skin picker — the brand sweep, no extra vocabulary. */
fun swatchFor(skin: AppSkin): List<Color> = when (skin) {
    AppSkin.AURORA -> AuroraGradient
    AppSkin.BLOSSOM -> BlossomGradient
}
