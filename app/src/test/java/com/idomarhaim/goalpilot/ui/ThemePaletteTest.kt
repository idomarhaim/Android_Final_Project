package com.idomarhaim.goalpilot.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.theme.accentsFor
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import org.junit.Test
import kotlin.math.sqrt

/**
 * Guards every **generated** colour scheme against a colour edit that looks fine
 * in an IDE swatch and is unreadable on a phone.
 *
 * Runs on the JVM: `ColorScheme`, `Color` and `luminance()` are pure Kotlin, so
 * the arithmetic half of theming needs no emulator.
 *
 * Thresholds are WCAG 2.1 — 4.5:1 for normal text, 3:1 for non-text boundaries
 * such as `outline`.
 *
 * ## Why the matrix and not the four hand-authored schemes
 *
 * `C12` #53 made the material a second axis, so the app now renders
 * **four materials × two skins × two brightnesses**. Twelve of those sixteen
 * are *generated* by `MaterialPalettes.kt` from the four that are written by
 * hand — and a transform is exactly the thing that passes review and fails
 * contrast, because nobody can read a desaturation in their head. Iterating
 * `AppMaterial.entries` here rather than naming the base four is what makes a
 * new material inherit every assertion below on the day it is added.
 *
 * `AppMaterial.resolveDark` collapses dark neo's two cases into one, so the
 * matrix is **fourteen** distinct schemes rather than sixteen. That is §4.1's
 * *"the product is ragged, not rectangular"*, arriving in a test.
 */
class ThemePaletteTest {

    private data class Case(val skin: AppSkin, val material: AppMaterial, val dark: Boolean) {
        val scheme: ColorScheme get() = colorSchemeFor(skin, material, dark)
        override fun toString() =
            "${skin.name}/${material.name}/${if (dark) "dark" else "light"}"
    }

    private val cases = AppSkin.entries.flatMap { skin ->
        AppMaterial.entries.flatMap { material ->
            listOf(
                Case(skin, material, dark = false),
                Case(skin, material, dark = true),
            )
        }
    }

    @Test
    fun `on-colours are readable on their own role`() {
        cases.forEach { case ->
            val s = case.scheme
            assertPair(case, "onPrimary/primary", s.onPrimary, s.primary)
            assertPair(case, "onSecondary/secondary", s.onSecondary, s.secondary)
            assertPair(case, "onTertiary/tertiary", s.onTertiary, s.tertiary)
            assertPair(case, "onError/error", s.onError, s.error)
            assertPair(case, "onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer)
            assertPair(case, "onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer)
            assertPair(case, "onTertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer)
            assertPair(case, "onErrorContainer", s.onErrorContainer, s.errorContainer)
            assertPair(case, "onSurface/surface", s.onSurface, s.surface)
            assertPair(case, "onBackground/background", s.onBackground, s.background)
            assertPair(case, "onSurfaceVariant/surfaceVariant", s.onSurfaceVariant, s.surfaceVariant)
        }
    }

    @Test
    fun `body text is readable on every card tone`() {
        // Cards sit on surface, surfaceContainerLow or surfaceContainerHigh
        // depending on emphasis; all of them carry onSurface / onSurfaceVariant text.
        cases.forEach { case ->
            val s = case.scheme
            listOf(
                "surfaceContainerLowest" to s.surfaceContainerLowest,
                "surfaceContainerLow" to s.surfaceContainerLow,
                "surfaceContainer" to s.surfaceContainer,
                "surfaceContainerHigh" to s.surfaceContainerHigh,
                "surfaceContainerHighest" to s.surfaceContainerHighest,
            ).forEach { (name, container) ->
                assertPair(case, "onSurface/$name", s.onSurface, container)
                assertPair(case, "onSurfaceVariant/$name", s.onSurfaceVariant, container)
            }
        }
    }

    @Test
    fun `accents used as text are readable on surfaces`() {
        // "10 pts", "+15", the section rule and the AI sparkle all paint an accent
        // straight onto a surface rather than onto its container.
        cases.forEach { case ->
            val s = case.scheme
            assertPair(case, "primary on surface", s.primary, s.surface)
            assertPair(case, "secondary on surface", s.secondary, s.surface)
            assertPair(case, "tertiary on surface", s.tertiary, s.surface)
            assertPair(case, "error on surface", s.error, s.surface)
            assertPair(case, "primary on surfaceContainerLow", s.primary, s.surfaceContainerLow)
        }
    }

    @Test
    fun `outlines are visible without being text`() {
        cases.forEach { case ->
            assertPair(case, "outline on surface", case.scheme.outline, case.scheme.surface, min = 3.0)
        }
    }

    @Test
    fun `hero gradient carries its on-colour across every stop`() {
        cases.forEach { case ->
            val accents = accentsFor(case.skin, case.material, case.dark)
            assertThat(accents.heroGradient).isNotEmpty()
            accents.heroGradient.forEachIndexed { index, stop ->
                assertPair(case, "onHero on gradient stop $index", accents.onHero, stop)
            }
        }
    }

    @Test
    fun `positive accent is readable on surface`() {
        cases.forEach { case ->
            val accents = accentsFor(case.skin, case.material, case.dark)
            assertPair(case, "positive on surface", accents.positive, case.scheme.surface)
        }
    }

    @Test
    fun `category colours are distinguishable from each other`() {
        // The analytics chart draws one bar per category; two categories closer
        // than this read as the same bar. The pre-2026-07-31 palette had three
        // greens that failed exactly this check.
        val entries = GoalCategory.entries.toList()
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                val a = entries[i]
                val b = entries[j]
                val distance = rgbDistance(a.defaultColorHex.parseHex(), b.defaultColorHex.parseHex())
                assertWithMessage("${a.name} vs ${b.name}").that(distance).isGreaterThan(90.0)
            }
        }
    }

    @Test
    fun `category colours are readable as text on a light surface`() {
        // They are used as the percentage label and the icon tint, not only as fills.
        GoalCategory.entries.forEach { category ->
            val ratio = contrastRatio(category.defaultColorHex.parseHex(), Color.White)
            assertWithMessage("${category.name} on white").that(ratio).isAtLeast(4.5)
        }
    }

    private fun assertPair(
        case: Case,
        label: String,
        foreground: Color,
        background: Color,
        min: Double = 4.5,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertWithMessage("$case — $label").that(ratio).isAtLeast(min)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun String.parseHex(): Color =
        Color(0xFF000000L or removePrefix("#").toLong(16))

    /** Weighted RGB distance — plain Euclidean over-rates blue differences. */
    private fun rgbDistance(a: Color, b: Color): Double {
        val dr = (a.red - b.red) * 255.0
        val dg = (a.green - b.green) * 255.0
        val db = (a.blue - b.blue) * 255.0
        return sqrt(2 * dr * dr + 4 * dg * dg + 3 * db * db)
    }
}
