package com.idomarhaim.goalpilot.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.MIN_INK_CONTRAST
import com.idomarhaim.goalpilot.ui.components.asInkOn
import com.idomarhaim.goalpilot.ui.components.cardTonesOf
import com.idomarhaim.goalpilot.ui.theme.accentsFor
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import com.idomarhaim.goalpilot.ui.theme.materialSpecFor
import com.idomarhaim.goalpilot.ui.theme.over
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
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
 *
 * ## `#57` b's third axis widens the GROUND cases, not the scheme cases
 *
 * The brief expected the matrix to multiply again. **It does not, and that is a
 * fact about the code rather than a reprieve:** `colorSchemeFor(skin, material,
 * dark)` does not take an `AppBackground`, so every scheme assertion above is
 * unchanged at fourteen. What the background changes is what gets painted
 * *behind* a panel, which no scheme role describes.
 *
 * So the widening is a **new pair of properties** rather than more of the old
 * ones, and they live in their own section at the bottom of this file. Both
 * exist because `#57` b lets a user put a lit ground under a material designed
 * for a flat one, and vice versa — twelve reachable combinations after
 * `resolveDark` collapses dark neo's pair, none of which any assertion here
 * previously reached.
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

    // ─────────────── the category palette (`#57` a) ───────────────
    //
    // Five properties, and they are deliberately five rather than one "looks
    // right": the set is authored once and then rendered by every chart, chip and
    // widget in the app, so the only thing that can catch a later ad-hoc edit is
    // an assertion on the property that edit would break.

    @Test
    fun `category fills are distinguishable from each other`() {
        // The analytics chart draws one bar per category; two categories closer
        // than this read as the same bar. The pre-2026-07-31 palette had three
        // greens that failed exactly this check.
        //
        // `#57` a's harmonised set does NOT weaken this: it measures **99.4** at
        // its worst pair (FITNESS/PROJECTS) against the previous set's 97.1, and
        // it does so on half the lightness scatter. The threshold is unchanged --
        // holding it is what forced the palette search to give up perfectly even
        // hue spacing, because ten colours at ONE lightness cannot clear it.
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
    fun `category dark twins are distinguishable from each other`() {
        // The dark set is pastel by construction -- it has to clear a dark ground,
        // which caps how far apart ten hues can sit in RGB terms -- so it cannot
        // meet the light set's 90, and this floor is lower ON PURPOSE.
        //
        // The number is not tuned to the values it guards. Dark mode before
        // `#57` a ran the light hexes through a fixed HSL-lightness lift; measured
        // over all forty-five pairs, that lift produces **37.2** on this light set
        // (and 57.6 on the one it replaced), while the authored twins produce
        // **66.2**. 62 sits above both, so this fails for anyone who reverts to the
        // derived set and passes for anything at least as separable as what
        // shipped.
        val entries = GoalCategory.entries.toList()
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                val a = entries[i]
                val b = entries[j]
                val distance = rgbDistance(a.darkColorHex.parseHex(), b.darkColorHex.parseHex())
                assertWithMessage("dark ${a.name} vs ${b.name}").that(distance).isGreaterThan(62.0)
            }
        }
    }

    @Test
    fun `a category keeps its identity across schemes`() {
        // The point of authoring the dark twin rather than computing it is that a
        // category stays recognisably itself. That is a claim about HUE, and hue is
        // the one thing a lightness transform cannot promise -- so assert it here
        // rather than trusting that two hand-picked hexes look related.
        //
        // The widest gap in the shipped set is FITNESS at 8.7 degrees, where the
        // prototype's own light and dark values disagree; 12 leaves that alone and
        // still catches a twin pasted into the wrong row.
        GoalCategory.entries.forEach { category ->
            val light = category.defaultColorHex.parseHex().hueDegrees()
            val dark = category.darkColorHex.parseHex().hueDegrees()
            val gap = abs(light - dark).let { min(it, 360.0 - it) }
            assertWithMessage(
                "${category.name}: light hue $light vs dark hue $dark",
            ).that(gap).isLessThan(12.0)
        }
    }

    @Test
    fun `category fills are visible as shapes on every card tone`() {
        // A FILL -- slice, bar, dot, icon tint -- is non-text, so WCAG 2.1 asks
        // 3:1 and not 4.5:1. This set is authored at that floor deliberately: ten
        // hues held at one lightness AND forced to 4.5:1 come out so dark they read
        // as mud, which is the failure one over from `#57`'s crayons.
        //
        // The ink that IS text is derived, and the next test guards it.
        cases.forEach { case ->
            val tones = cardTonesOf(case.scheme)
            GoalCategory.entries.forEach { category ->
                val fill = category.fillFor(case.scheme)
                tones.forEach { tone ->
                    assertPair(case, "${category.name} fill on tone", fill, tone, min = 3.0)
                }
            }
        }
    }

    @Test
    fun `derived category ink is readable on every card tone`() {
        // Runs the REAL `asInkOn` -- not a copy of its arithmetic -- over all
        // fourteen schemes. That is why the solver is pure Kotlin and sits beside a
        // composable wrapper rather than inside one: the thing shipped is the thing
        // asserted.
        cases.forEach { case ->
            val tones = cardTonesOf(case.scheme)
            GoalCategory.entries.forEach { category ->
                val ink = category.fillFor(case.scheme).asInkOn(tones)
                tones.forEach { tone ->
                    assertPair(case, "${category.name} ink on tone", ink, tone, min = MIN_INK_CONTRAST)
                }
            }
        }
    }

    @Test
    fun `derived ink still handles a colour that is not one of ours`() {
        // Life-area colours and user-picked hexes reach the same seam and have no
        // authored twin, so the solver has to be total. Both endpoints of the
        // lightness range are the interesting inputs: pure black cannot be darkened
        // and pure white cannot be lightened.
        cases.forEach { case ->
            val tones = cardTonesOf(case.scheme)
            listOf(Color.Black, Color.White, Color(0xFF5145CD), Color(0xFF00FF00)).forEach { odd ->
                val ink = odd.asInkOn(tones)
                tones.forEach { tone ->
                    assertPair(case, "arbitrary ink on tone", ink, tone, min = MIN_INK_CONTRAST)
                }
            }
        }
    }

    // ───────────── the ground, and what sits on it (`#57` b) ─────────────
    //
    // `#57` b made the background a user-chosen third axis, so a material can now
    // be asked to render on a ground it was not designed for. Two properties
    // follow, and neither is reachable from any scheme role: what a PANEL looks
    // like once the ground shows through it, and what the PAGE itself does to
    // text that inherits `onBackground`.
    //
    // Both run over the full 4 x 4 x 2 product -- every skin, every material,
    // every background, both brightnesses -- rather than over `cases`, because
    // the background is exactly the dimension `cases` does not have.

    /**
     * Every (skin, material, background, brightness) the picker can produce.
     *
     * Not deduplicated by `resolveDark` the way `cases` is: dark neo's two
     * brightnesses genuinely collapse and asserting both costs microseconds,
     * whereas leaving the loop rectangular is what makes it obvious that no
     * combination was skipped.
     */
    private data class Ground(
        val skin: AppSkin,
        val material: AppMaterial,
        val background: AppBackground,
        val dark: Boolean,
    ) {
        val scheme: ColorScheme get() = colorSchemeFor(skin, material, dark)
        val spec get() = materialSpecFor(material, background, scheme, material.resolveDark(dark))
        override fun toString() =
            skin.name + "/" + material.name + "/" + background.name +
                "/" + (if (dark) "dark" else "light")
    }

    private val grounds = AppSkin.entries.flatMap { skin ->
        AppMaterial.entries.flatMap { material ->
            AppBackground.entries.flatMap { background ->
                listOf(
                    Ground(skin, material, background, dark = false),
                    Ground(skin, material, background, dark = true),
                )
            }
        }
    }

    /**
     * A phone's page, sampled where TEXT actually is.
     *
     * The horizontal window is not `0f..1f`: the page carries 16 dp of padding
     * and a card adds roughly 18 more, on a page about 392 dp wide, so nothing
     * is ever drawn in the outer 9%. Sampling the full width would measure the
     * page's brightest corner and report a failure at a pixel no glyph can
     * occupy — which is the shape of a guard that gets weakened later because it
     * cried wolf. `Observed:` it makes almost no difference here (2.66 at the
     * corner against 2.78 inside the window, on the pre-fix values), and that is
     * itself the useful finding: the hot spot is **inside** the card area.
     */
    private val textWindow = (0..40).flatMap { i ->
        (0..26).map { j -> Pair(0.09f + 0.82f * i / 40f, j / 26f) }
    }

    @Test
    fun `body text on a panel survives every ground the picker offers`() {
        // THE assertion `#57` b owes. A translucent panel's contrast is a
        // property of what is behind it unless the material puts a floor under
        // it -- `GpGloss.tintFloor`'s own doc says so -- and until this session
        // the background was fixed per material, so "what is behind it" was
        // never a variable. It is one now, and a user can put the brightest
        // ground under the most transparent material in two taps.
        //
        // `Observed:` this caught a REAL, PRE-EXISTING failure rather than a
        // hypothetical one. Glass in dark mode measured **2.55-2.78:1** on its
        // OWN native ground -- shipped, and reachable before this session
        // existed -- because its `tintFloor` was `primary` at 0.06 alpha, a
        // bloom rather than a floor. The two fixes are in `MaterialSpec.kt`:
        // real floors for glass and liquid, and a ground alpha that had stopped
        // being "low".
        //
        // Worst cell after both: **5.83** (BLOSSOM/LIQUID_GLASS/MATCH/dark).
        grounds.forEach { ground ->
            val scheme = ground.scheme
            val spec = ground.spec
            val floor = spec.gloss?.tintFloor
            textWindow.forEach { (x, y) ->
                val page = spec.backdrop.colorAt(x, y, scheme.background, PAGE_ASPECT)
                // gpSurface paints the fill over the page, then drawGloss paints
                // tintFloor over the fill. The rims and the specular streak are
                // deliberately NOT modelled: both LIGHTEN, so including them
                // could only flatter a dark-on-light case and cannot rescue a
                // light-on-bright one, which is the failure this is looking for.
                listOf(spec.surface, spec.surfaceEnd).forEach { fill ->
                    var body = fill.over(page)
                    if (floor != null && floor != Color.Transparent) body = floor.over(body)
                    val ratio = contrastRatio(scheme.onSurface, body)
                    assertWithMessage("$ground — onSurface on panel at ($x, $y)")
                        .that(ratio).isAtLeast(4.5)
                }
            }
        }
    }

    @Test
    fun `the page itself stays a ground rather than becoming a subject`() {
        // 3.0 and not 4.5, and the reason is a fact about the app rather than a
        // concession: NOTHING paints body text straight onto the page.
        // `colorScheme.onBackground` is read nowhere in `app/src/main` outside
        // `Palettes.kt` itself -- it reaches the page only through Material 3's
        // default content colour under `MainActivity`'s `Surface`, which carries
        // section headings, not paragraphs. WCAG's 3:1 is the floor for exactly
        // that: large text and non-text.
        //
        // What this therefore guards is not the legibility of prose but the
        // ground staying a GROUND. `backdropFor`'s doc records the drift it
        // caught: the port took the prototype's hue SELECTION and not its
        // luminance, so the dark canvas ran at roughly twice the design's
        // brightness (scheme pastels at 0.564-0.572 against the prototype's
        // 0.194-0.446).
        //
        // Worst cell: **3.96** (BLOSSOM/NEO/SPECTRUM/dark) -- a soft material on
        // the busiest lit ground, which is the combination `#57` b invented.
        grounds.forEach { ground ->
            val scheme = ground.scheme
            val backdrop = ground.spec.backdrop
            textWindow.forEach { (x, y) ->
                val page = backdrop.colorAt(x, y, scheme.background, PAGE_ASPECT)
                val ratio = contrastRatio(scheme.onBackground, page)
                assertWithMessage("$ground — onBackground on page at ($x, $y)")
                    .that(ratio).isAtLeast(3.0)
            }
        }
    }

    @Test
    fun `MATCH reproduces exactly the ground each material used to own`() {
        // The default has one job: an install that never opens Settings must
        // render what it rendered before this axis existed. That is a claim
        // about EQUALITY, not about contrast, so it is asserted as equality --
        // and it is what fails if someone later "tidies" the material-to-ground
        // mapping in `AppBackground.resolve`.
        //
        // Written against GLOW / SPECTRUM / PLAIN by name rather than against a
        // second copy of the mapping, so the two cannot agree by both being
        // wrong.
        val expected = mapOf(
            AppMaterial.GLASS to AppBackground.GLOW,
            AppMaterial.LIQUID_GLASS to AppBackground.SPECTRUM,
            AppMaterial.NEO to AppBackground.PLAIN,
            AppMaterial.DARK_NEO to AppBackground.PLAIN,
        )
        AppSkin.entries.forEach { skin ->
            listOf(false, true).forEach { dark ->
                AppMaterial.entries.forEach { material ->
                    val scheme = colorSchemeFor(skin, material, dark)
                    val resolvedDark = material.resolveDark(dark)
                    val match = materialSpecFor(
                        material, AppBackground.MATCH, scheme, resolvedDark,
                    )
                    val native = materialSpecFor(
                        material, expected.getValue(material), scheme, resolvedDark,
                    )
                    assertWithMessage(skin.name + "/" + material.name)
                        .that(match).isEqualTo(native)
                }
            }
        }
    }

    /**
     * A lit ground under a soft material must not silently stay opaque.
     *
     * `AppBackground`'s doc tells the user that neumorphism *cannot* survive a
     * lit ground and that the panel becomes a translucent plate instead. That
     * sentence is a promise about pixels, and what would break it is an
     * ordinary-looking edit to `neoSpec` — so it is asserted rather than
     * trusted. The converse half is asserted too: on a plain ground the soft
     * surface must go back to being **exactly** the page colour, which is what
     * neumorphism *is*.
     */
    @Test
    fun `soft materials go translucent on a lit ground and opaque on a plain one`() {
        val soft = listOf(AppMaterial.NEO, AppMaterial.DARK_NEO)
        AppSkin.entries.forEach { skin ->
            soft.forEach { material ->
                listOf(false, true).forEach { dark ->
                    val scheme = colorSchemeFor(skin, material, dark)
                    val resolvedDark = material.resolveDark(dark)
                    AppBackground.entries.forEach { background ->
                        val spec = materialSpecFor(material, background, scheme, resolvedDark)
                        val lit = background.isLit(material)
                        assertWithMessage(skin.name + "/" + material.name + "/" + background.name)
                            .that(spec.isTranslucent).isEqualTo(lit)
                        if (!lit && material == AppMaterial.NEO) {
                            assertWithMessage("neo on a plain ground IS the page colour")
                                .that(spec.surface).isEqualTo(scheme.surface)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `an overlay is opaque in every material on every ground`() {
        // The same C22 rule the spec has always carried, now over the whole
        // product: the plate treatment is what made it newly breakable, because
        // an overlay built off a translucent `surface` would inherit the
        // transparency and the dimmed screen would read straight through it.
        grounds.forEach { ground ->
            assertWithMessage("$ground — overlay opacity")
                .that(ground.spec.overlay.alpha).isEqualTo(1f)
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

    /**
     * The fill this category renders as under [scheme] — chosen the way the app
     * chooses it, off the **rendered** surface.
     *
     * Not off `Case.dark`, which is a different question and gives a different
     * answer: `AppMaterial.resolveDark` forces dark neo dark in both brightnesses,
     * so the AURORA / DARK_NEO / **light** case carries an all-dark tone ladder. Reading
     * the requested brightness there would test the light hex against a charcoal
     * card and pass something the app never draws.
     */
    private fun GoalCategory.fillFor(scheme: ColorScheme) =
        if (scheme.surface.luminance() < 0.5f) darkColorHex.parseHex()
        else defaultColorHex.parseHex()

    /**
     * OKLab hue angle in degrees — the perceptual hue, not HSL's, because HSL hue
     * shifts visibly when lightness moves, and the whole point of the twin check is
     * that it survives a lightness move.
     */
    private fun Color.hueDegrees(): Double {
        fun lin(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val r = lin(red)
        val g = lin(green)
        val b = lin(blue)
        val l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b).pow(1.0 / 3.0)
        val m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b).pow(1.0 / 3.0)
        val s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b).pow(1.0 / 3.0)
        val aStar = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bStar = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        return (Math.toDegrees(atan2(bStar, aStar)) + 360.0) % 360.0
    }

    private companion object {
        /** A tall phone: 2.17 is roughly 19.5:9, which every device this ships to is near. */
        const val PAGE_ASPECT = 2.17f
    }

    /** Weighted RGB distance — plain Euclidean over-rates blue differences. */
    private fun rgbDistance(a: Color, b: Color): Double {
        val dr = (a.red - b.red) * 255.0
        val dg = (a.green - b.green) * 255.0
        val db = (a.blue - b.blue) * 255.0
        return sqrt(2 * dr * dr + 4 * dg * dg + 3 * db * db)
    }
}
