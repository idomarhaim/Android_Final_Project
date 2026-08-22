package com.idomarhaim.goalpilot.ui

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.ui.components.ENTRANCE_STAGGER_MAX_MS
import com.idomarhaim.goalpilot.ui.components.ENTRANCE_STAGGER_MS
import com.idomarhaim.goalpilot.ui.components.ENTRANCE_WINDOW_MS
import com.idomarhaim.goalpilot.ui.components.GpEntrance
import org.junit.Test

/**
 * The arrival's bookkeeping — who gets which slot in the wave, and when the wave
 * is over.
 *
 * All of it is deliberately outside the composable, in a class that takes the
 * clock as an argument, because the two properties that actually matter here are
 * *timing* properties and neither is watchable on a device:
 *
 * - **Nothing re-animates on an ordinary state change.** On a device that is a
 *   negative over an interval — a thing that did not happen — and the honest
 *   version of it is [claimedCount] not moving when a block is asked again. A
 *   screenshot cannot say it and a render pass cannot say it.
 * - **A block composed after the arrival must not animate**, which is what stops
 *   a stagger keyed to position from firing on every card the user scrolls to.
 *   Reaching that state on an emulator means waiting out a real second.
 *
 * The instrumented half (`EntranceAnimationUiTest`) checks the other claim: that
 * the offset this schedules is really applied to a real card and really lands
 * back at zero.
 */
class GpEntranceTest {

    // ─────────────────────────── the wave itself ───────────────────────────

    @Test
    fun `the first block does not wait, and each one after it waits one step longer`() {
        val entrance = GpEntrance()
        val now = 1_000L

        val delays = (0 until 5).map { entrance.claim(now) }

        assertThat(delays).containsExactly(
            0,
            ENTRANCE_STAGGER_MS,
            2 * ENTRANCE_STAGGER_MS,
            3 * ENTRANCE_STAGGER_MS,
            4 * ENTRANCE_STAGGER_MS,
        ).inOrder()
    }

    @Test
    fun `the stagger stops growing at the cap`() {
        // Without this a twenty-block screen has its last card still waiting long
        // after the first has finished -- the prototype's lists are one screenful
        // and a dashboard is not.
        val entrance = GpEntrance()
        val now = 1_000L

        val delays = (0 until 40).map { entrance.claim(now) }

        assertThat(delays.filterNotNull().maxOrNull()).isEqualTo(ENTRANCE_STAGGER_MAX_MS)
        assertThat(delays.last()).isEqualTo(ENTRANCE_STAGGER_MAX_MS)
    }

    @Test
    fun `a block that composes late joins the wave instead of restarting it behind`() {
        val entrance = GpEntrance()
        val start = 1_000L

        entrance.claim(start) // ordinal 0, sets the clock
        // Ordinal 1 would normally wait one step; it composed a step and a half
        // late, so the wave has already passed it and it goes now.
        val late = entrance.claim(start + ENTRANCE_STAGGER_MS + ENTRANCE_STAGGER_MS / 2)

        assertThat(late).isEqualTo(0)
    }

    @Test
    fun `the clock starts at the first claim, not at construction`() {
        // The dashboard provides its entrance above a loading spinner. If the
        // clock ran from construction the whole arrival would be spent behind
        // that spinner and the cards would appear fully formed.
        val entrance = GpEntrance()
        val muchLater = 9_999_999L

        assertThat(entrance.claim(muchLater)).isEqualTo(0)
        assertThat(entrance.claim(muchLater)).isEqualTo(ENTRANCE_STAGGER_MS)
    }

    // ──────────────────── it must not fight the scroll ─────────────────────

    @Test
    fun `a block composed after the arrival window does not animate at all`() {
        val entrance = GpEntrance()
        val start = 1_000L
        entrance.claim(start)

        val scrolledTo = entrance.claim(start + ENTRANCE_WINDOW_MS + 1)

        assertThat(scrolledTo).isNull()
    }

    @Test
    fun `a block outside the window takes no slot, so it cannot push the wave along`() {
        // The counter is what a later block's delay is computed from. If a
        // scrolled-to card still incremented it, an arrival that happened to
        // overlap a scroll would hand the next real block an inflated delay.
        val entrance = GpEntrance()
        val start = 1_000L
        entrance.claim(start)
        val after = start + ENTRANCE_WINDOW_MS + 1

        repeat(10) { entrance.claim(after) }

        assertThat(entrance.claimedCount).isEqualTo(1)
    }

    // ───────────────────────── reduce motion wins ──────────────────────────

    @Test
    fun `motion off means every block is simply there`() {
        val entrance = GpEntrance(motion = false)

        val delays = (0 until 5).map { entrance.claim(1_000L) }

        assertThat(delays).containsExactly(null, null, null, null, null)
        assertThat(entrance.claimedCount).isEqualTo(0)
    }

    // ─────────────────── it must not re-run on recomposition ───────────────

    @Test
    fun `two screens are two independent waves`() {
        // `Modifier.gpEntrance` keys its remember on the entrance instance, so a
        // screen genuinely re-entered gets a genuinely new arrival -- and must
        // not inherit the previous screen's ordinals.
        val first = GpEntrance()
        repeat(4) { first.claim(1_000L) }

        val second = GpEntrance()

        assertThat(second.claim(50_000L)).isEqualTo(0)
    }
}
