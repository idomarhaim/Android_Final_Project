package com.idomarhaim.goalpilot.ui.tutorial

import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Where the coach mark's card lands relative to the hole.
 *
 * ### Why this is arithmetic in a JVM test and not a look at a phone
 *
 * The rule has three branches — below the target, above it, centred over it —
 * and which one fires depends on the *screen*, not on the code. A phone tall
 * enough to put the card below every anchor exercises one branch and reports
 * that placement works; the other two are only reachable on a device or a font
 * scale nobody in this project has. Looking at one screen and inferring the
 * other two is exactly the failure `kb/dev/look-at-your-own-output.md` is about.
 *
 * The numbers below are a 1080 × 2400 phone at 3× density, which is what the
 * project's own AVD is.
 */
class TutorialPlacementTest {

    private val margin = 48   // 16.dp
    private val gap = 36      // 12.dp
    private val screenWidth = 1080
    private val screenHeight = 2400
    private val cardWidth = 900
    private val cardHeight = 600

    private fun place(target: Rect?) = placeCard(
        target = target,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        margin = margin,
        gap = gap,
    )

    @Test
    fun `a step with no target is centred`() {
        val placement = place(null)

        assertThat(placement.x).isEqualTo((screenWidth - cardWidth) / 2)
        assertThat(placement.y).isEqualTo((screenHeight - cardHeight) / 2)
    }

    @Test
    fun `a target near the top puts the card below it`() {
        val target = Rect(100f, 200f, 980f, 600f)

        val placement = place(target)

        assertThat(placement.y).isEqualTo(600 + gap)
    }

    @Test
    fun `a target near the bottom puts the card above it`() {
        // The bottom navigation bar, which two of the seven steps point at. This
        // is the branch a tall test phone never reaches.
        val target = Rect(0f, 2200f, 1080f, 2400f)

        val placement = place(target)

        assertThat(placement.y).isEqualTo(2200 - gap - cardHeight)
        assertThat(placement.y).isAtLeast(margin)
    }

    @Test
    fun `a target that fills the screen leaves the card centred over it`() {
        val target = Rect(0f, 0f, 1080f, 2400f)

        val placement = place(target)

        assertThat(placement.y).isEqualTo((screenHeight - cardHeight) / 2)
    }

    @Test
    fun `the card follows the target sideways`() {
        val narrowCard = 400
        val target = Rect(700f, 200f, 900f, 300f)

        val placement = placeCard(
            target = target,
            cardWidth = narrowCard,
            cardHeight = cardHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin,
            gap = gap,
        )

        // Centred on the target's centre (800), not on the screen's.
        assertThat(placement.x).isEqualTo(800 - narrowCard / 2)
    }

    @Test
    fun `the card never leaves the margins, however near the edge the target is`() {
        val narrowCard = 400
        val atTheLeftEdge = Rect(0f, 200f, 60f, 300f)
        val atTheRightEdge = Rect(1020f, 200f, 1080f, 300f)

        fun x(target: Rect) = placeCard(
            target = target,
            cardWidth = narrowCard,
            cardHeight = cardHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin,
            gap = gap,
        ).x

        assertThat(x(atTheLeftEdge)).isEqualTo(margin)
        assertThat(x(atTheRightEdge)).isEqualTo(screenWidth - narrowCard - margin)
    }

    @Test
    fun `a card wider than the screen still starts inside it`() {
        // Reachable at the largest accessibility font scale, where the card's
        // measured width can exceed what the margins leave. Clamping the range
        // the wrong way round would return a NEGATIVE x and draw the card
        // half off the left edge, which is the failure `coerceAtLeast` on the
        // upper bound exists to prevent.
        val placement = placeCard(
            target = Rect(100f, 200f, 980f, 600f),
            cardWidth = screenWidth,
            cardHeight = cardHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin,
            gap = gap,
        )

        assertThat(placement.x).isAtLeast(0)
    }
}
