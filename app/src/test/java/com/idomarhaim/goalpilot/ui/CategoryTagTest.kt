package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.PaletteTransform
import com.idomarhaim.goalpilot.ui.components.categoryFill
import com.idomarhaim.goalpilot.ui.components.segmentLabelFits
import com.idomarhaim.goalpilot.ui.components.tangentRotation
import com.idomarhaim.goalpilot.ui.components.wedgeLabelFits
import com.idomarhaim.goalpilot.ui.components.wedgeLabelRoom
import org.junit.Test
import kotlin.math.sqrt

/**
 * §4.1's **`.tag`** rule, asserted as the two-part claim it actually is.
 *
 * > A category is **written in words** beside its dot, **because** dark neo
 * > collapses the six categorical hues into one ramp and colour stops carrying
 * > identity.
 *
 * The *because* is what makes this one test file rather than two. A test that only
 * checked the collapse would pass on the day the collapse shipped **without** the
 * words — which is the exact state `C12` refused to ship and left `#53` open to
 * avoid. A test that only checked the words would pass while `rampTint` still had
 * zero call sites, which is the state this file was written to end. So both halves
 * are here, and the second is asserted as a *consequence* of the first:
 *
 * 1. under `identity` two categories are told apart **by colour**;
 * 2. under the ramp they are **not**;
 * 3. therefore something else has to carry identity, and the only candidate is the
 *    word — so the fit rules that decide whether a word gets drawn have to say yes
 *    on the marks that matter.
 *
 * ## Why this runs on the JVM and not on a device
 *
 * `categoryFill`, `wedgeLabelFits` and `segmentLabelFits` are pure Kotlin for
 * exactly this reason — the same split `ThemePaletteTest` and
 * `StackedColumnChart.labelStride` already take. What an emulator adds is a real
 * font on a real screen, and the render pass is where that gets looked at.
 */
class CategoryTagTest {

    private val fallback = Color(0xFF718096)

    // ── 1 + 2. the collapse is real, and it happens only under the ramp ──────

    @Test
    fun `identity keeps every category apart and the ramp does not`() {
        AppSkin.entries.forEach { skin ->
            val identity = GoalCategory.entries.map {
                categoryFill(
                    hex = it.defaultColorHex,
                    transform = PaletteTransform.IDENTITY,
                    skin = skin,
                    darkSurface = false,
                    fallback = fallback,
                )
            }
            val ramped = GoalCategory.entries.map {
                categoryFill(
                    hex = it.defaultColorHex,
                    transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                    skin = skin,
                    darkSurface = true,
                    fallback = fallback,
                )
            }

            // The authored set is separable -- that is `#57` a's own guarantee, and
            // ThemePaletteTest owns the real threshold. Restated loosely here so
            // the contrast below is a comparison rather than an assertion in a
            // vacuum.
            assertWithMessage("$skin -- identity no longer keeps the categories apart")
                .that(minSeparation(identity))
                .isGreaterThan(IDENTITY_FLOOR)

            // And the ramp takes it away. This is the sentence §4.1 gives as the
            // REASON for `.tag`, in one number.
            assertWithMessage("$skin -- the ramp did NOT collapse the hues, so `.tag` would be unnecessary")
                .that(minSeparation(ramped))
                .isLessThan(RAMP_COLLAPSE_CEILING)
        }
    }

    @Test
    fun `identity and mute leave the palette alone`() {
        // A collapse that leaked into glass or neo would be a palette bug wearing
        // `.tag`'s clothes: the words would look decorative on three of the four
        // materials, and the next session would delete them.
        AppSkin.entries.forEach { skin ->
            listOf(PaletteTransform.IDENTITY, PaletteTransform.MUTE).forEach { transform ->
                val fills = GoalCategory.entries.map {
                    categoryFill(
                        hex = it.defaultColorHex,
                        transform = transform,
                        skin = skin,
                        darkSurface = false,
                        fallback = fallback,
                    )
                }
                assertWithMessage("$skin/$transform -- collapsed under a transform that must not")
                    .that(minSeparation(fills))
                    .isGreaterThan(IDENTITY_FLOOR)
            }
        }
    }

    @Test
    fun `the ramp is taken from the light fill, not from the even-lightness dark twin`() {
        // `#57` a authored the twins to a deliberately EVEN lightness, and rampTint
        // positions by lightness -- so ramping the twins lands the whole set on
        // nearly one point. Asserted rather than left as a comment, because the
        // wrong version of this function still produces a plausible-looking chart.
        AppSkin.entries.forEach { skin ->
            val fromLight = GoalCategory.entries.map {
                categoryFill(
                    hex = it.defaultColorHex,
                    transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                    skin = skin,
                    darkSurface = true,
                    fallback = fallback,
                )
            }
            val fromTwin = GoalCategory.entries.map {
                categoryFill(
                    hex = it.darkColorHex,
                    transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                    skin = skin,
                    darkSurface = true,
                    fallback = fallback,
                )
            }
            assertWithMessage("$skin -- ramping the dark twins kept as much spread as ramping the fills")
                .that(spread(fromTwin))
                .isLessThan(spread(fromLight))
        }
    }

    @Test
    fun `the ramp does not branch on the surface being dark`() {
        // Dark neo is brightness-locked, so `darkSurface` is true in practice.
        // Pinning the invariant anyway: the ramp's ends are already held in a
        // readable band by RAMP_BRIGHT / RAMP_DEEP, so the dark-twin lift must not
        // also run -- that would be one contrast rule undoing another.
        GoalCategory.entries.forEach { category ->
            val onDark = categoryFill(
                hex = category.defaultColorHex,
                transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                skin = AppSkin.AURORA,
                darkSurface = true,
                fallback = fallback,
            )
            val onLight = categoryFill(
                hex = category.defaultColorHex,
                transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                skin = AppSkin.AURORA,
                darkSurface = false,
                fallback = fallback,
            )
            assertWithMessage("${category.name} -- the ramp branched on the surface")
                .that(onDark).isEqualTo(onLight)
        }
    }

    @Test
    fun `the skin still reaches the collapsed palette`() {
        // §4.1's first named consequence, one axis down from where it was found:
        // "picking Blossom under dark neo silently renders Aurora". `rampFor` takes
        // the skin, so a collapsed category must still differ between skins -- or
        // the skin picker stops working for a quarter of the set at the exact
        // moment the categories stop carrying their own hue.
        GoalCategory.entries.forEach { category ->
            val aurora = categoryFill(
                hex = category.defaultColorHex,
                transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                skin = AppSkin.AURORA,
                darkSurface = true,
                fallback = fallback,
            )
            val blossom = categoryFill(
                hex = category.defaultColorHex,
                transform = PaletteTransform.SINGLE_ACCENT_RAMP,
                skin = AppSkin.BLOSSOM,
                darkSurface = true,
                fallback = fallback,
            )
            assertWithMessage("${category.name} -- the skin does not reach the ramped category fill")
                .that(distance(aurora, blossom))
                .isGreaterThan(IDENTITY_FLOOR)
        }
    }

    @Test
    fun `dark neo is the only material that declares the collapsing transform`() {
        // The sweep's blast radius, as a test: if a second material ever declares
        // the ramp, this fails and whoever added it reads `.tag` before shipping.
        assertThat(
            AppMaterial.entries.filter {
                it.paletteTransform == PaletteTransform.SINGLE_ACCENT_RAMP
            },
        ).containsExactly(AppMaterial.DARK_NEO)
    }

    // ── 3. so the words have to be drawable where it counts ─────────────────

    @Test
    fun `a donut wedge worth naming can carry its word`() {
        // The analytics donut's own geometry -- 220 dp across, 34 dp band -- in
        // pixels. A quarter of the ring is a wedge nobody should have to look up.
        assertWithMessage("a 90-degree wedge cannot hold a word -- `.tag` is unsatisfiable")
            .that(fitsAt(sweepDegrees = 90f))
            .isTrue()
    }

    @Test
    fun `a sliver is left unlabelled rather than given an ellipsis with nothing before it`() {
        // The floor is not about overflow -- the caller ellipsizes, so nothing can
        // overflow. It is about a word cut so short it names nothing: at 2 degrees
        // this wedge has room for about one glyph, and "N..." on a slice is worse
        // than a slice you can tap.
        assertThat(fitsAt(sweepDegrees = 2f)).isFalse()
    }

    @Test
    fun `the long names are labelled too, which is the whole reason for the ellipsis`() {
        // THE REGRESSION GUARD, and it comes from a frame rather than from a
        // theory. The version of this that drew only whole words labelled six of
        // ten equal wedges on the render pass and left Nutrition, Relationships,
        // Projects and Learning blank -- the four longest names, and under dark neo
        // the four wedges nothing else tells apart.
        //
        // Ten equal wedges is 36 degrees each. The rule must say yes there, and it
        // must say yes on a name of ANY length, because length is now the caller's
        // problem and not this function's -- which is exactly what this asserts by
        // passing it no width at all.
        assertWithMessage("ten equal wedges must every one be labelled")
            .that(fitsAt(sweepDegrees = 360f / 10f))
            .isTrue()
    }

    @Test
    fun `a band thinner than its own type is refused however wide the wedge`() {
        // The other half of the rule, and the one the arc cannot rescue: a word
        // needs the BAND's thickness for its height, so a full circle in a hairline
        // ring is still unlabellable.
        assertThat(
            wedgeLabelFits(
                roomPx = wedgeLabelRoom(360f, MID_RADIUS_PX, PADDING_PX),
                thicknessPx = WORD_HEIGHT_PX,
                textHeightPx = WORD_HEIGHT_PX,
                paddingPx = PADDING_PX,
            ),
        ).isFalse()
    }

    @Test
    fun `the room a wedge offers grows with the wedge and not with the ring`() {
        // `wedgeLabelRoom` is arc length, so doubling the sweep doubles it. Pinned
        // because the obvious wrong version -- the chord, or the outer arc -- also
        // grows with the sweep and would pass every other assertion here while
        // quietly claiming room the inner edge of the band does not have.
        val small = wedgeLabelRoom(30f, MID_RADIUS_PX, 0f)
        val double = wedgeLabelRoom(60f, MID_RADIUS_PX, 0f)
        assertThat(double).isWithin(TOLERANCE).of(small * 2f)
        assertThat(small).isWithin(TOLERANCE).of(
            (2.0 * Math.PI * MID_RADIUS_PX / 12.0).toFloat(),
        )
    }

    @Test
    fun `no word on the ring is ever drawn upside down`() {
        // A rotated label reads bottom-to-top over half the circle unless it is
        // flipped, and the half it fails on is the half nobody screenshots. Walked
        // in 5-degree steps rather than checked at two representative angles: the
        // first version of `tangentRotation` was correct at 12 and 6 on the clock
        // and wrong across the whole 270-to-360 quadrant.
        (0 until 360 step 5).forEach { degrees ->
            assertWithMessage("a wedge at $degrees degrees carries an upside-down word")
                .that(tangentRotation(degrees.toFloat()))
                .isIn(Range.closed(-90f, 90f))
        }
    }

    @Test
    fun `the rotation is the tangent, so a word at the top of the ring is level`() {
        // 12 on the clock is -90 in canvas convention and its tangent is
        // horizontal. If this ever comes back 90 then every word is vertical and
        // the upside-down assertion above still passes.
        assertThat(tangentRotation(-90f)).isWithin(TOLERANCE).of(0f)
        assertThat(tangentRotation(90f)).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `a dominant stack band carries its word and a thin one does not`() {
        // A seven-column week on a phone gives each column about 40 dp.
        val bandWidth = 40f * DENSITY
        assertWithMessage("the dominant band of a column cannot hold a word")
            .that(
                segmentLabelFits(
                    bandWidthPx = bandWidth,
                    bandHeightPx = 60f * DENSITY,
                    textHeightPx = WORD_HEIGHT_PX,
                    paddingPx = PADDING_PX,
                ),
            ).isTrue()
        assertWithMessage("a 6 dp band was claimed to hold a 12 dp word")
            .that(
                segmentLabelFits(
                    bandWidthPx = bandWidth,
                    bandHeightPx = 6f * DENSITY,
                    textHeightPx = WORD_HEIGHT_PX,
                    paddingPx = PADDING_PX,
                ),
            ).isFalse()
    }

    @Test
    fun `a column too narrow to hold four glyphs is refused however tall the band`() {
        // The stacked chart's own version of the sliver floor: a 12 dp column with
        // the whole height to itself still cannot name anything.
        assertThat(
            segmentLabelFits(
                bandWidthPx = 12f * DENSITY,
                bandHeightPx = 200f * DENSITY,
                textHeightPx = WORD_HEIGHT_PX,
                paddingPx = PADDING_PX,
            ),
        ).isFalse()
    }

    @Test
    fun `a text box with no height is never drawn`() {
        // Both predicates take the type's HEIGHT and no width, so a zero-height box
        // satisfies every inequality in them. Rejected explicitly, or a blank label
        // measures as "it fits" and the mark gets an empty `.tag`, which reads as a
        // rendering fault rather than as a missing word.
        assertThat(
            wedgeLabelFits(
                roomPx = wedgeLabelRoom(120f, MID_RADIUS_PX, PADDING_PX),
                thicknessPx = THICKNESS_PX,
                textHeightPx = 0f,
                paddingPx = PADDING_PX,
            ),
        ).isFalse()
        assertThat(segmentLabelFits(600f, 600f, 0f, PADDING_PX)).isFalse()
    }

    @Test
    fun `the documented cut-off is where the code actually puts it`() {
        // `wedgeLabelFits`'s KDoc claims wedges under about 18.5 degrees go
        // unlabelled at the analytics donut's geometry, and a prose number in a
        // KDoc is exactly the kind of claim that rots silently. Pinned from BOTH
        // sides, so it fails whether the rule loosens or tightens.
        assertWithMessage("a 19-degree wedge should be labelled").that(fitsAt(19f)).isTrue()
        assertWithMessage("an 18-degree wedge should not be").that(fitsAt(18f)).isFalse()
    }

    /** [wedgeLabelFits] at the analytics donut's geometry, for a wedge of this size. */
    private fun fitsAt(sweepDegrees: Float): Boolean = wedgeLabelFits(
        roomPx = wedgeLabelRoom(sweepDegrees, MID_RADIUS_PX, PADDING_PX),
        thicknessPx = THICKNESS_PX,
        textHeightPx = WORD_HEIGHT_PX,
        paddingPx = PADDING_PX,
    )

    // ── helpers ─────────────────────────────────────────────────────────────

    /** The closest two colours in [colors] get, in [distance]'s units. */
    private fun minSeparation(colors: List<Color>): Double {
        var min = Double.MAX_VALUE
        for (i in colors.indices) {
            for (j in i + 1 until colors.size) {
                min = minOf(min, distance(colors[i], colors[j]))
            }
        }
        return min
    }

    /** How far apart the whole set gets: the largest gap any two members reach. */
    private fun spread(colors: List<Color>): Double {
        var max = 0.0
        for (i in colors.indices) {
            for (j in i + 1 until colors.size) {
                max = maxOf(max, distance(colors[i], colors[j]))
            }
        }
        return max
    }

    /**
     * Weighted Euclidean distance in sRGB.
     *
     * **The same metric `ThemePaletteTest.rgbDistance` uses, deliberately** —
     * because that file already guards this exact palette with it at **90** for the
     * light fills and **62** for the dark twins, and those two numbers are quoted
     * in its comments, in `#57` a's changelog and on the issue. A second measure of
     * the same property would make this file's numbers incomparable with all of
     * them, and the first draft of it did exactly that: CIE L*a*b* put the worst
     * authored pair at **11.2** where the house metric puts it at **99.4**, which
     * reads as a palette that has already collapsed. Neither number is wrong; only
     * one of them is the one everything else here is written in.
     */
    private fun distance(a: Color, b: Color): Double {
        val dr = (a.red - b.red).toDouble() * 255.0
        val dg = (a.green - b.green).toDouble() * 255.0
        val db = (a.blue - b.blue).toDouble() * 255.0
        // Weighted, exactly as `ThemePaletteTest.rgbDistance` weights it: green
        // carries most of perceived luminance and blue least, and an unweighted
        // version of this line put the worst authored pair at 56.5 against that
        // file's documented 99.4 -- two files disagreeing about whether the same
        // palette is separable, which is the whole failure the KDoc above warns of,
        // arriving inside the fix for it.
        return sqrt(2 * dr * dr + 4 * dg * dg + 3 * db * db)
    }

    private companion object {
        /**
         * The separation the authored palette holds, and the ramp must not.
         *
         * **90 is not a number chosen here** — it is the floor
         * `ThemePaletteTest.category fills are distinguishable from each other`
         * already asserts over the same ten hexes, where the worst authored pair
         * measures 99.4. Reusing it rather than inventing one means the two files
         * cannot drift into disagreeing about whether this palette is separable.
         */
        const val IDENTITY_FLOOR = 90.0

        /**
         * The separation below which two categories are *not* tellable apart.
         *
         * MEASURED, not guessed, and the measurement came out better than the rule
         * needed it to be: over the ten fills the ramp's worst pair is **exactly
         * 0.0**, on both skins.
         *
         * `Observed:` NUTRITION `#347B47` and CAREER `#0861A7` share an HSL
         * lightness to the last digit — 0.343137, because (123+52)/2 and (167+8)/2
         * are both 87.5 — and `rampTint` positions a hue on the ramp **by its
         * lightness**. So dark neo renders two different life areas as the
         * identical pixel. That is not a near miss and not a threshold argument;
         * it is §4.1's `.tag` rule holding up its own worked example.
         *
         * 30 therefore sits 30 above the measured value and 60 below
         * [IDENTITY_FLOOR], so neither side of the assertion is anywhere near it. A
         * threshold either half only just clears is a test that fails on a palette
         * tweak instead of on a defect.
         */
        const val RAMP_COLLAPSE_CEILING = 30.0

        /**
         * How far the two skins must move a *collapsed* category.
         *
         * §4.1's first named consequence one axis down: `rampFor` takes the skin,
         * so Aurora's ramp and Blossom's must still be different colours even after
         * the hues have gone. Low on purpose — this asks whether the skin reaches
         * the value at all, and [IDENTITY_FLOOR] is a claim about a ten-member set
         * rather than about one pair.
         */
        const val SKIN_REACH_FLOOR = 20.0

        /** A phone at roughly 3 px per dp, which is every device this app is read on. */
        const val DENSITY = 3f

        /** The analytics donut: 220 dp across, 34 dp band. */
        const val THICKNESS_PX = 34f * DENSITY
        const val MID_RADIUS_PX = (220f - 34f) / 2f * DENSITY

        /**
         * About the line height `labelSmall` gives at this density.
         *
         * No word *width* appears anywhere below, and that is the shape of the
         * rule rather than an omission: the caller ellipsizes against the room, so
         * how long a name happens to be stopped being the fit test's business.
         */
        const val WORD_HEIGHT_PX = 12f * DENSITY
        const val PADDING_PX = 6f * DENSITY

        /** Float slack for an angle that arrives through two modulo operations. */
        const val TOLERANCE = 0.001f
    }
}
