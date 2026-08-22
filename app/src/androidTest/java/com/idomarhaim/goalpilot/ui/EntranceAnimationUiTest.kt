package com.idomarhaim.goalpilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.ui.components.ENTRANCE_WINDOW_MS
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.GpEntrance
import com.idomarhaim.goalpilot.ui.components.LocalGpEntrance
import com.idomarhaim.goalpilot.ui.components.gpEntrance
import org.junit.Rule
import org.junit.Test

/**
 * `#57` d — the blocks **arrive** rather than being already there.
 *
 * ## Why the clock is frozen in every test here
 *
 * An entrance animation is over in a third of a second, so a test that lets the
 * clock run measures nothing but the resting state — and passes just as happily
 * against a build with no animation in it at all. `mainClock.autoAdvance = false`
 * is what makes the intermediate frames observable, and every assertion below
 * compares a frame *during* the arrival with the same block at rest.
 *
 * ## ⚠️ Two instruments that cannot see this animation, and were tried first
 *
 * The rise is a `graphicsLayer { translationY }`. It changes no layout, and the
 * two obvious ways to observe a position both fail on it — in opposite
 * directions, which is why neither is obviously wrong until it is run:
 *
 * - **`SemanticsNode.positionInRoot` does not follow the layer.** It reported the
 *   *same* y during the arrival as at rest (`expected greater than 0.0 but was
 *   0.0`), while the opacity assertion sitting beside it passed.
 * - **`onNodeWithTag(…).captureToImage()` follows it too well.** The capture
 *   rectangle is taken in window coordinates, which *do* include the layer
 *   transform, so the image moves with the block and its top row is the block's
 *   top row at every frame — measured `first=0` on every frame of a probe that
 *   showed the opacity changing underneath it.
 *
 * So the offset is measured against a **fixed** frame of reference: capture the
 * whole root and read how far down a known pixel column the block has got. It is
 * the only one of the three that has a zero to be greater than.
 *
 * ## What is asserted where
 *
 * The scheduling — who waits how long, and when the arrival is over — is
 * `GpEntranceTest` at the JVM layer, where the clock is an argument. This file
 * asserts the half only a real composition can answer: that the schedule reaches
 * a real block in both of the properties `@keyframes rise` animates, that a
 * [GpCard] carries it without asking, and that it lands back at exactly zero and
 * stays there.
 */
class EntranceAnimationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ───────────────────────────── instruments ─────────────────────────────

    /**
     * How far below its resting place the block in slot [index] currently sits,
     * in pixels — measured in **root** coordinates, for the reason in the class
     * comment. `-1` means the block is not visible at all yet, which is a real
     * state early in a staggered arrival and not a failure to find it.
     */
    private fun riseOf(index: Int): Int = risesNow(index + 1)[index]

    /** [riseOf] for the first [count] blocks, off a single capture of the root. */
    private fun risesNow(count: Int): List<Int> {
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val density = composeRule.density.density
        val depth = minOf(pixels.height, (BLOCK_DP * density).toInt())
        return (0 until count).map { index ->
            val x = ((index * (BLOCK_DP + GAP_DP) + BLOCK_DP / 2) * density).toInt()
            (0 until depth).firstOrNull { y -> pixels[x, y].green < NOT_THE_PAGE } ?: -1
        }
    }

    /** The colour at the middle of the block tagged [tag], page included where it shows through. */
    private fun centreOf(tag: String) = composeRule.onNodeWithTag(tag).captureToImage()
        .toPixelMap()
        .let { it[it.width / 2, it.height / 2] }

    /**
     * Wind the clock to a frame on which all three blocks have been let go and
     * none has arrived. The last of them waits 90 ms and the first takes 340 ms,
     * so this sits inside both.
     */
    private fun midArrival() = composeRule.mainClock.advanceTimeBy(96L)

    /** Run the arrival out to its end, so every block is at rest. */
    private fun settle() = composeRule.mainClock.advanceTimeBy(ENTRANCE_WINDOW_MS + 100L)

    /** A white page — the thing [riseOf] measures against — holding [content]. */
    private fun page(motion: Boolean = true, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalGpEntrance provides GpEntrance(motion = motion)) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) { content() }
            }
        }
    }

    /**
     * [count] blocks side by side.
     *
     * Laid out across rather than down purely so each gets its own pixel column
     * to be measured in; the stagger is about the order blocks arrive in, not
     * about which axis they are stacked on.
     */
    @Composable
    private fun RedBlocks(count: Int) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP_DP.dp)) {
            repeat(count) { i ->
                Box(
                    modifier = Modifier
                        .gpEntrance()
                        .size(BLOCK_DP.dp)
                        .background(Color.Red)
                        .testTag("block$i"),
                )
            }
        }
    }

    // ───────────────────────────── the arrival ─────────────────────────────

    @Test
    fun aBlockStartsBelowItsPlaceAndRisesIntoIt() {
        page { RedBlocks(1) }

        midArrival()
        val arriving = riseOf(0)
        settle()
        val atRest = riseOf(0)

        assertThat(arriving).isGreaterThan(0)
        assertThat(atRest).isEqualTo(0)
    }

    @Test
    fun theBlocksArriveAsASequence_notAsOneSlab() {
        // The whole point of the stagger. One shared duration and no delay would
        // put all three blocks at the same offset on every frame -- a fade-in that
        // is technically correct and visibly not the prototype.
        page { RedBlocks(3) }

        // Measured as "which frame does each block first appear on" rather than
        // "how far down is each block at time T". Both express the same stagger,
        // but the offsets are only a handful of pixels apart on any single frame
        // and the frame the animation actually starts on carries a couple of
        // frames of composition latency -- pick T by arithmetic and the third
        // block has not been let go yet. Observed exactly that: at 96 ms it read
        // as not present, while a frame-by-frame probe of the same layout showed
        // all three ordered correctly a little later. The frame index needs no T.
        val firstSeen = IntArray(3) { NOT_SEEN }
        for (frame in 0 until MAX_ARRIVAL_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            val rises = risesNow(3)
            rises.forEachIndexed { i, rise -> if (firstSeen[i] == NOT_SEEN && rise >= 0) firstSeen[i] = frame }
            if (firstSeen.none { it == NOT_SEEN }) break
        }

        assertThat(firstSeen.toList()).doesNotContain(NOT_SEEN)
        assertThat(firstSeen[2]).isGreaterThan(firstSeen[1])
        assertThat(firstSeen[1]).isGreaterThan(firstSeen[0])

        settle()
        assertThat(risesNow(3)).isEqualTo(listOf(0, 0, 0))
    }

    @Test
    fun theBlockFadesInAsItRises() {
        // The offset alone would pass against a build that dropped the opacity
        // half of `@keyframes rise`, so this one reads the colour: part-arrived
        // reads as red washed towards the white page, arrived reads as pure red.
        page { RedBlocks(1) }

        midArrival()
        val arriving = centreOf("block0")
        settle()
        val atRest = centreOf("block0")

        assertThat(atRest.toArgb()).isEqualTo(Color.Red.toArgb())
        assertThat(arriving.green).isGreaterThan(atRest.green)
        assertThat(arriving.green).isLessThan(1f) // ...but on its way, not absent
    }

    @Test
    fun aGpCardCarriesTheArrivalWithoutBeingAsked() {
        // The reason 30-odd call sites needed no change: the modifier lives inside
        // GpCard. An override fill makes the card a colour this test can tell from
        // the page; the gloss and the edge are the material's and are left alone,
        // which is why this asserts a direction rather than an exact colour.
        page {
            GpCard(
                modifier = Modifier.size(BLOCK_DP.dp).testTag("card"),
                colors = CardDefaults.cardColors(containerColor = Color.Red),
            ) {}
        }

        midArrival()
        val arriving = centreOf("card")
        settle()
        val atRest = centreOf("card")

        assertThat(arriving.toArgb()).isNotEqualTo(atRest.toArgb())
        assertThat(arriving.green).isGreaterThan(atRest.green)
    }

    // ────────────────── it must not re-run, and must not fight ─────────────

    @Test
    fun anOrdinaryStateChangeDoesNotReplayTheArrival() {
        // The failure this guards is a flicker on every edit: an entrance keyed on
        // anything the screen can change re-fires whenever a value updates.
        // Asserted rather than eyeballed, which is the point -- on a device this
        // is a thing that must NOT happen, and no screenshot can show that.
        var label by mutableStateOf("1")
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalGpEntrance provides GpEntrance(motion = true)) {
                // Read in the same scope that builds the modifier, so an edit
                // genuinely re-invokes `Modifier.gpEntrance()`. A read buried in a
                // child would recompose the child alone and test nothing.
                val current = label
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    Box(
                        modifier = Modifier
                            .gpEntrance()
                            .size(BLOCK_DP.dp)
                            .background(Color.Red)
                            .testTag("block0"),
                    ) { BasicText(text = current, modifier = Modifier.padding(4.dp)) }
                }
            }
        }
        settle()
        assertThat(riseOf(0)).isEqualTo(0)

        composeRule.runOnUiThread { label = "2" }
        composeRule.mainClock.advanceTimeByFrame()
        assertThat(riseOf(0)).isEqualTo(0)
        composeRule.mainClock.advanceTimeByFrame()
        assertThat(riseOf(0)).isEqualTo(0)
        assertThat(centreOf("block0").toArgb()).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun reduceMotion_meansTheBlocksAreSimplyThere() {
        page(motion = false) { RedBlocks(1) }
        composeRule.mainClock.advanceTimeByFrame()

        // No rise and no fade on the very first drawn frame -- the block is not
        // arriving, it is there. (With motion on, this frame shows nothing at all.)
        assertThat(riseOf(0)).isEqualTo(0)
        assertThat(centreOf("block0").toArgb()).isEqualTo(Color.Red.toArgb())
    }

    private companion object {
        const val BLOCK_DP = 64
        const val GAP_DP = 8

        /** Sentinel for a block that has not appeared yet. */
        const val NOT_SEEN = -1

        /** 30 frames is about half a second -- comfortably past 90 ms of delay + 340 ms of rise. */
        const val MAX_ARRIVAL_FRAMES = 30

        /**
         * A pixel is "not the page" once its green channel has come down off
         * white. Loose on purpose: the last block in the stagger is only about a
         * fifth opaque when it is measured, and a tight threshold would read it as
         * page and report it as never having arrived.
         */
        const val NOT_THE_PAGE = 0.9f
    }
}
