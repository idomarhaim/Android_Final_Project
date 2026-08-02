package com.idomarhaim.goalpilot.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Raw colour tokens for the two selectable skins (see AppSkin).
 *
 * Both palettes follow the Material 3 tonal contract — light schemes take the
 * accent at ~tone 35 with a tone-90 container, dark schemes take tone 80 with a
 * tone-30 container — and every accent used as a text/icon colour clears WCAG AA
 * (≥ 4.5:1) against the surface it sits on. `ThemePaletteTest` asserts that, so
 * these are not free-form: change one and the test tells you if it broke.
 *
 * **The canvas is the skin.** `background`/`surface` carry real chroma (37 for
 * Aurora, 29 for Blossom, measured max-min across RGB) rather than the almost-
 * neutral tint they started with — at chroma ~13 a tinted background is
 * indistinguishable from grey on a phone, which defeats the point of having
 * skins at all.
 *
 * That has a cost worth understanding before editing: a more saturated canvas is
 * a *darker* canvas, and every accent painted directly on it loses contrast.
 * Aurora's accents sit at ~tone 35 rather than Material's tone 40 specifically to
 * buy that headroom — at tone 40 the canvas cannot go past chroma ~14 without
 * `secondary on surface` dropping under 4.5:1. Darkening the canvas without also
 * darkening the accents will fail the test.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Aurora — ocean blue → teal → evergreen (default)
// ─────────────────────────────────────────────────────────────────────────────

// Light
val AuroraPrimary = Color(0xFF0A56C4)
val AuroraOnPrimary = Color(0xFFFFFFFF)
val AuroraPrimaryContainer = Color(0xFFD8E2FF)
val AuroraOnPrimaryContainer = Color(0xFF001A41)
val AuroraSecondary = Color(0xFF0A6045)
val AuroraOnSecondary = Color(0xFFFFFFFF)
val AuroraSecondaryContainer = Color(0xFFA8F2D3)
val AuroraOnSecondaryContainer = Color(0xFF002019)
val AuroraTertiary = Color(0xFF00596B)
val AuroraOnTertiary = Color(0xFFFFFFFF)
val AuroraTertiaryContainer = Color(0xFFADEDFF)
val AuroraOnTertiaryContainer = Color(0xFF001F27)
val AuroraBackground = Color(0xFFD0E2F5)
val AuroraOnBackground = Color(0xFF131A22)
val AuroraSurface = Color(0xFFD0E2F5)
val AuroraOnSurface = Color(0xFF131A22)
val AuroraSurfaceDim = Color(0xFFB8CBDF)
val AuroraSurfaceBright = Color(0xFFF4F9FF)
val AuroraSurfaceContainerLowest = Color(0xFFFFFFFF)
val AuroraSurfaceContainerLow = Color(0xFFF2F7FD)
val AuroraSurfaceContainer = Color(0xFFEBF2FB)
val AuroraSurfaceContainerHigh = Color(0xFFE4EDF8)
val AuroraSurfaceContainerHighest = Color(0xFFDDE8F5)
val AuroraSurfaceVariant = Color(0xFFD9E2EF)
val AuroraOnSurfaceVariant = Color(0xFF414A57)
val AuroraOutline = Color(0xFF6C7684)
val AuroraOutlineVariant = Color(0xFFBAC6D6)
val AuroraInverseSurface = Color(0xFF2A323D)
val AuroraInverseOnSurface = Color(0xFFEDF2FA)
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
val AuroraDarkBackground = Color(0xFF0C1520)
val AuroraDarkOnBackground = Color(0xFFDEE6F2)
val AuroraDarkSurface = Color(0xFF0C1520)
val AuroraDarkOnSurface = Color(0xFFDEE6F2)
val AuroraDarkSurfaceDim = Color(0xFF0C1520)
val AuroraDarkSurfaceBright = Color(0xFF333D4B)
val AuroraDarkSurfaceContainerLowest = Color(0xFF070C13)
val AuroraDarkSurfaceContainerLow = Color(0xFF141D29)
val AuroraDarkSurfaceContainer = Color(0xFF18212E)
val AuroraDarkSurfaceContainerHigh = Color(0xFF222C3A)
val AuroraDarkSurfaceContainerHighest = Color(0xFF2D3745)
val AuroraDarkSurfaceVariant = Color(0xFF404A58)
val AuroraDarkOnSurfaceVariant = Color(0xFFBFCADA)
val AuroraDarkOutline = Color(0xFF8A94A5)
val AuroraDarkOutlineVariant = Color(0xFF404A58)
val AuroraDarkInverseSurface = Color(0xFFDEE6F2)
val AuroraDarkInverseOnSurface = Color(0xFF2A323D)

// Hero gradient — blue → teal → evergreen. Every stop clears 4.5:1 against
// white, so a single onHero colour works across the whole sweep. Deliberately
// kept brighter than `primary`: the hero is the one element that should pop off
// the canvas rather than sit level with it.
val AuroraHeroStart = Color(0xFF0B62D6)
val AuroraHeroMid = Color(0xFF0C7CA4)
val AuroraHeroEnd = Color(0xFF0E7F63)
val AuroraPositive = Color(0xFF0A6045)
val AuroraDarkPositive = Color(0xFF8AD6B7)

// ─────────────────────────────────────────────────────────────────────────────
// Blossom — sunset rose → coral → amber
// ─────────────────────────────────────────────────────────────────────────────

// Light
val BlossomPrimary = Color(0xFFB8215A)
val BlossomOnPrimary = Color(0xFFFFFFFF)
val BlossomPrimaryContainer = Color(0xFFFFD9E2)
val BlossomOnPrimaryContainer = Color(0xFF40001C)
val BlossomSecondary = Color(0xFFA4460E)
val BlossomOnSecondary = Color(0xFFFFFFFF)
val BlossomSecondaryContainer = Color(0xFFFFDBC8)
val BlossomOnSecondaryContainer = Color(0xFF360F00)
val BlossomTertiary = Color(0xFF8A5400)
val BlossomOnTertiary = Color(0xFFFFFFFF)
val BlossomTertiaryContainer = Color(0xFFFFDDB3)
val BlossomOnTertiaryContainer = Color(0xFF2F1C00)
val BlossomBackground = Color(0xFFFBDEE4)
val BlossomOnBackground = Color(0xFF22161A)
val BlossomSurface = Color(0xFFFBDEE4)
val BlossomOnSurface = Color(0xFF22161A)
val BlossomSurfaceDim = Color(0xFFE4C4CC)
val BlossomSurfaceBright = Color(0xFFFFF7F9)
val BlossomSurfaceContainerLowest = Color(0xFFFFFFFF)
val BlossomSurfaceContainerLow = Color(0xFFFFF4F6)
val BlossomSurfaceContainer = Color(0xFFFFEDF0)
val BlossomSurfaceContainerHigh = Color(0xFFFDE7EB)
val BlossomSurfaceContainerHighest = Color(0xFFF8E0E5)
val BlossomSurfaceVariant = Color(0xFFF4DCE0)
val BlossomOnSurfaceVariant = Color(0xFF523F45)
val BlossomOutline = Color(0xFF86707A)
val BlossomOutlineVariant = Color(0xFFD9C0C7)
val BlossomInverseSurface = Color(0xFF3A2C31)
val BlossomInverseOnSurface = Color(0xFFFDECEF)
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
val BlossomDarkBackground = Color(0xFF221014)
val BlossomDarkOnBackground = Color(0xFFF2DEE3)
val BlossomDarkSurface = Color(0xFF221014)
val BlossomDarkOnSurface = Color(0xFFF2DEE3)
val BlossomDarkSurfaceDim = Color(0xFF221014)
val BlossomDarkSurfaceBright = Color(0xFF4A363C)
val BlossomDarkSurfaceContainerLowest = Color(0xFF180A0E)
val BlossomDarkSurfaceContainerLow = Color(0xFF2A1A1F)
val BlossomDarkSurfaceContainer = Color(0xFF301F24)
val BlossomDarkSurfaceContainerHigh = Color(0xFF3C2A2F)
val BlossomDarkSurfaceContainerHighest = Color(0xFF48353B)
val BlossomDarkSurfaceVariant = Color(0xFF54424A)
val BlossomDarkOnSurfaceVariant = Color(0xFFDAC0C7)
val BlossomDarkOutline = Color(0xFFA28C94)
val BlossomDarkOutlineVariant = Color(0xFF54424A)
val BlossomDarkInverseSurface = Color(0xFFF2DEE3)
val BlossomDarkInverseOnSurface = Color(0xFF3A2C31)

// Hero gradient — rose → coral → amber, same ≥ 4.5:1-on-white rule as Aurora.
val BlossomHeroStart = Color(0xFFC62A63)
val BlossomHeroMid = Color(0xFFC4453B)
val BlossomHeroEnd = Color(0xFFAE6206)
val BlossomPositive = Color(0xFF0A6045)
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
