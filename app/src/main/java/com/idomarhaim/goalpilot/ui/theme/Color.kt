package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Raw colour tokens for the two selectable skins (see AppSkin).
 *
 * Both palettes follow the Material 3 tonal contract — light schemes take the
 * accent at ~tone 40 with a tone-90 container, dark schemes take tone 80 with a
 * tone-30 container — and every accent used as a text/icon colour clears WCAG AA
 * (≥ 4.5:1) against the surface it sits on. `ThemeContrastTest` asserts that, so
 * these are not free-form: change one and the test tells you if it broke.
 *
 * Neutrals are *tinted*, not grey: Aurora's neutrals lean blue, Blossom's lean
 * warm. That tint is what makes a skin read as a skin on screens that are mostly
 * cards and text rather than saturated accent.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Aurora — ocean blue → teal → evergreen (default)
// ─────────────────────────────────────────────────────────────────────────────

// Light
val AuroraPrimary = Color(0xFF0B62D6)
val AuroraOnPrimary = Color(0xFFFFFFFF)
val AuroraPrimaryContainer = Color(0xFFD8E2FF)
val AuroraOnPrimaryContainer = Color(0xFF001A41)
val AuroraSecondary = Color(0xFF0F7A5F)
val AuroraOnSecondary = Color(0xFFFFFFFF)
val AuroraSecondaryContainer = Color(0xFFA8F2D3)
val AuroraOnSecondaryContainer = Color(0xFF002019)
val AuroraTertiary = Color(0xFF00687B)
val AuroraOnTertiary = Color(0xFFFFFFFF)
val AuroraTertiaryContainer = Color(0xFFADEDFF)
val AuroraOnTertiaryContainer = Color(0xFF001F27)
// The canvas is deliberately a tinted tone rather than near-white: GpCard fills
// with surfaceContainerLowest, so cards sit *above* the page instead of being
// darker rectangles cut into it. See GpCard's docs.
val AuroraBackground = Color(0xFFEAEEF7)
val AuroraOnBackground = Color(0xFF191C21)
val AuroraSurface = Color(0xFFEAEEF7)
val AuroraOnSurface = Color(0xFF191C21)
val AuroraSurfaceDim = Color(0xFFD5D9E3)
val AuroraSurfaceBright = Color(0xFFFBFCFF)
val AuroraSurfaceContainerLowest = Color(0xFFFFFFFF)
val AuroraSurfaceContainerLow = Color(0xFFF6F8FD)
val AuroraSurfaceContainer = Color(0xFFF1F3FA)
val AuroraSurfaceContainerHigh = Color(0xFFEBEEF6)
val AuroraSurfaceContainerHighest = Color(0xFFE5E9F2)
val AuroraSurfaceVariant = Color(0xFFDFE3EE)
val AuroraOnSurfaceVariant = Color(0xFF44474F)
val AuroraOutline = Color(0xFF74777F)
val AuroraOutlineVariant = Color(0xFFC4C7D1)
val AuroraInverseSurface = Color(0xFF2F3036)
val AuroraInverseOnSurface = Color(0xFFF1F0F7)
val AuroraInversePrimary = Color(0xFFAFC6FF)

// Dark
val AuroraDarkPrimary = Color(0xFFAFC6FF)
val AuroraDarkOnPrimary = Color(0xFF002E6A)
val AuroraDarkPrimaryContainer = Color(0xFF004494)
val AuroraDarkOnPrimaryContainer = Color(0xFFD8E2FF)
val AuroraDarkSecondary = Color(0xFF8AD6B7)
val AuroraDarkOnSecondary = Color(0xFF00382B)
val AuroraDarkSecondaryContainer = Color(0xFF00513E)
val AuroraDarkOnSecondaryContainer = Color(0xFFA8F2D3)
val AuroraDarkTertiary = Color(0xFF57D6F0)
val AuroraDarkOnTertiary = Color(0xFF003641)
val AuroraDarkTertiaryContainer = Color(0xFF004E5D)
val AuroraDarkOnTertiaryContainer = Color(0xFFADEDFF)
val AuroraDarkBackground = Color(0xFF111318)
val AuroraDarkOnBackground = Color(0xFFE2E2E9)
val AuroraDarkSurface = Color(0xFF111318)
val AuroraDarkOnSurface = Color(0xFFE2E2E9)
val AuroraDarkSurfaceDim = Color(0xFF111318)
val AuroraDarkSurfaceBright = Color(0xFF373A41)
val AuroraDarkSurfaceContainerLowest = Color(0xFF0C0E13)
val AuroraDarkSurfaceContainerLow = Color(0xFF191C20)
val AuroraDarkSurfaceContainer = Color(0xFF1D2024)
val AuroraDarkSurfaceContainerHigh = Color(0xFF282A2F)
val AuroraDarkSurfaceContainerHighest = Color(0xFF33353A)
val AuroraDarkSurfaceVariant = Color(0xFF44464F)
val AuroraDarkOnSurfaceVariant = Color(0xFFC4C6D0)
val AuroraDarkOutline = Color(0xFF8E9099)
val AuroraDarkOutlineVariant = Color(0xFF44464F)
val AuroraDarkInverseSurface = Color(0xFFE2E2E9)
val AuroraDarkInverseOnSurface = Color(0xFF2F3036)

// Hero gradient — blue → teal → evergreen. Every stop clears 4.5:1 against
// white, so a single onHero colour works across the whole sweep.
val AuroraHeroStart = Color(0xFF0B62D6)
val AuroraHeroMid = Color(0xFF0C7CA4)
val AuroraHeroEnd = Color(0xFF0E7F63)
val AuroraPositive = Color(0xFF0F7A5F)
val AuroraDarkPositive = Color(0xFF8AD6B7)

// ─────────────────────────────────────────────────────────────────────────────
// Blossom — sunset rose → coral → amber
// ─────────────────────────────────────────────────────────────────────────────

// Light
val BlossomPrimary = Color(0xFFC62A63)
val BlossomOnPrimary = Color(0xFFFFFFFF)
val BlossomPrimaryContainer = Color(0xFFFFD9E2)
val BlossomOnPrimaryContainer = Color(0xFF40001C)
val BlossomSecondary = Color(0xFFB24F14)
val BlossomOnSecondary = Color(0xFFFFFFFF)
val BlossomSecondaryContainer = Color(0xFFFFDBC8)
val BlossomOnSecondaryContainer = Color(0xFF360F00)
val BlossomTertiary = Color(0xFF9C5F00)
val BlossomOnTertiary = Color(0xFFFFFFFF)
val BlossomTertiaryContainer = Color(0xFFFFDDB3)
val BlossomOnTertiaryContainer = Color(0xFF2F1C00)
val BlossomBackground = Color(0xFFFCEEF0)
val BlossomOnBackground = Color(0xFF211A1C)
val BlossomSurface = Color(0xFFFCEEF0)
val BlossomOnSurface = Color(0xFF211A1C)
val BlossomSurfaceDim = Color(0xFFE6D7DA)
val BlossomSurfaceBright = Color(0xFFFFF8F9)
val BlossomSurfaceContainerLowest = Color(0xFFFFFFFF)
val BlossomSurfaceContainerLow = Color(0xFFFFF7F8)
val BlossomSurfaceContainer = Color(0xFFFFF1F3)
val BlossomSurfaceContainerHigh = Color(0xFFFBEBEE)
val BlossomSurfaceContainerHighest = Color(0xFFF5E5E8)
val BlossomSurfaceVariant = Color(0xFFF3DDE1)
val BlossomOnSurfaceVariant = Color(0xFF524346)
val BlossomOutline = Color(0xFF847376)
val BlossomOutlineVariant = Color(0xFFD6C2C5)
val BlossomInverseSurface = Color(0xFF372F30)
val BlossomInverseOnSurface = Color(0xFFFBEDEE)
val BlossomInversePrimary = Color(0xFFFFB1C7)

// Dark
val BlossomDarkPrimary = Color(0xFFFFB1C7)
val BlossomDarkOnPrimary = Color(0xFF641033)
val BlossomDarkPrimaryContainer = Color(0xFF8A2748)
val BlossomDarkOnPrimaryContainer = Color(0xFFFFD9E2)
val BlossomDarkSecondary = Color(0xFFFFB68E)
val BlossomDarkOnSecondary = Color(0xFF5A2200)
val BlossomDarkSecondaryContainer = Color(0xFF8B3C00)
val BlossomDarkOnSecondaryContainer = Color(0xFFFFDBC8)
val BlossomDarkTertiary = Color(0xFFF1C05C)
val BlossomDarkOnTertiary = Color(0xFF4F3100)
val BlossomDarkTertiaryContainer = Color(0xFF714800)
val BlossomDarkOnTertiaryContainer = Color(0xFFFFDDB3)
val BlossomDarkBackground = Color(0xFF191113)
val BlossomDarkOnBackground = Color(0xFFEFDFE1)
val BlossomDarkSurface = Color(0xFF191113)
val BlossomDarkOnSurface = Color(0xFFEFDFE1)
val BlossomDarkSurfaceDim = Color(0xFF191113)
val BlossomDarkSurfaceBright = Color(0xFF413739)
val BlossomDarkSurfaceContainerLowest = Color(0xFF140C0E)
val BlossomDarkSurfaceContainerLow = Color(0xFF221A1B)
val BlossomDarkSurfaceContainer = Color(0xFF261E1F)
val BlossomDarkSurfaceContainerHigh = Color(0xFF31282A)
val BlossomDarkSurfaceContainerHighest = Color(0xFF3C3335)
val BlossomDarkSurfaceVariant = Color(0xFF524346)
val BlossomDarkOnSurfaceVariant = Color(0xFFD6C2C5)
val BlossomDarkOutline = Color(0xFF9E8C90)
val BlossomDarkOutlineVariant = Color(0xFF524346)
val BlossomDarkInverseSurface = Color(0xFFEFDFE1)
val BlossomDarkInverseOnSurface = Color(0xFF372F30)

// Hero gradient — rose → coral → amber, same ≥ 4.5:1-on-white rule as Aurora.
val BlossomHeroStart = Color(0xFFC62A63)
val BlossomHeroMid = Color(0xFFC4453B)
val BlossomHeroEnd = Color(0xFFAE6206)
val BlossomPositive = Color(0xFF0F7A5F)
val BlossomDarkPositive = Color(0xFF8AD6B7)

// ─────────────────────────────────────────────────────────────────────────────
// Shared
// ─────────────────────────────────────────────────────────────────────────────

val GpError = Color(0xFFBA1A1A)
val GpOnError = Color(0xFFFFFFFF)
val GpErrorContainer = Color(0xFFFFDAD6)
val GpOnErrorContainer = Color(0xFF410002)
val GpDarkError = Color(0xFFFFB4AB)
val GpDarkOnError = Color(0xFF690005)
val GpDarkErrorContainer = Color(0xFF93000A)
val GpDarkOnErrorContainer = Color(0xFFFFDAD6)
val GpScrim = Color(0xFF000000)
