package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FillLadder
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.InputMode
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import org.junit.Test

/**
 * The fill-button ladder — spec §1.3, `R25`,
 * [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11).
 *
 * **The two named targets are the reason this suite exists.** §1.3 asserts the
 * formula yields `250 ml · 500 ml · 750 ml · 1 L` on *"drink 4 L a day"* and
 * *"stays right at a 40 L target"*, and those are the only two claims in the spec
 * that a test can actually falsify. Everything else here defends the rounding
 * rule — which is [FillLadder]'s own choice, not the spec's — against the one
 * failure mode #11's brief names outright: *"if you find yourself hard-coding a
 * millilitre table, you have rebuilt the thing the formula exists to replace."*
 */
class FillLadderTest {

    // ── The two targets the ticket names ───────────────────────────

    @Test
    fun `four litres yields the ladder the spec names`() {
        // §1.3, verbatim: "On 'drink 4 L a day' that yields [250 ml] [500 ml]
        // [750 ml] [1 L] exactly." In the goal's own word that is 0.25 / 0.5 /
        // 0.75 / 1 — the same amounts; see FillLadder on why the app does not
        // spell the first three in millilitres.
        assertThat(FillLadder.of(4.0, MeasureKind.VOLUME))
            .containsExactly(0.25, 0.5, 0.75, 1.0).inOrder()
    }

    @Test
    fun `forty litres stays right, which is the point of a formula`() {
        // The second half of §1.3's claim, and the half a hard-coded millilitre
        // table would fail: a table right at 4 L offers 250 ml buttons on a 40 L
        // goal, which is 160 taps.
        assertThat(FillLadder.of(40.0, MeasureKind.VOLUME))
            .containsExactly(2.5, 5.0, 7.5, 10.0).inOrder()
    }

    @Test
    fun `the top rung is a quarter of the target at both named targets`() {
        // Why 16 and 4 hang together: `target/16 x 4` is `target/4`, so the big
        // button always finishes the goal in four taps and the small one in
        // sixteen, whatever the target is. This is the property that makes one
        // formula usable across three orders of magnitude.
        listOf(4.0, 40.0, 400.0).forEach { target ->
            assertThat(FillLadder.of(target, MeasureKind.VOLUME).last())
                .isWithin(1e-9).of(target / 4.0)
        }
    }

    // ── The rounding rule ──────────────────────────────────────────

    @Test
    fun `a target that does not divide evenly snaps to a readable base`() {
        // 100/16 = 6.25, which nobody wants on a button. The nearest rung of the
        // 1-2-2.5-5 ladder is 5.
        assertThat(FillLadder.of(100.0, MeasureKind.VOLUME))
            .containsExactly(5.0, 10.0, 15.0, 20.0).inOrder()
    }

    @Test
    fun `the ladder scales across orders of magnitude without a table`() {
        // 70,000 steps — the Health Connect weekly step target. 70000/16 = 4375,
        // which snaps to 5000: the readability rule is what earns the formula its
        // keep at this end of the range, where an unrounded base is unreadable in
        // a different way than 6.25 was.
        assertThat(FillLadder.of(70_000.0, MeasureKind.COUNT))
            .containsExactly(5_000.0, 10_000.0, 15_000.0, 20_000.0).inOrder()
    }

    @Test
    fun `no rung carries binary representation noise`() {
        // 0.1 * 3 is 0.30000000000000004, and a button labelled that is worse
        // than any rounding rule could be. Every rung must round-trip through a
        // 6-decimal grid unchanged.
        MeasureKind.entries.forEach { kind ->
            listOf(1.6, 4.8, 12.3, 0.48).forEach { target ->
                FillLadder.of(target, kind).forEach { rung ->
                    assertThat(rung).isEqualTo(Math.round(rung * 1_000_000.0) / 1_000_000.0)
                }
            }
        }
    }

    // ── COUNT is the kind that cannot take fractions ───────────────

    @Test
    fun `a count ladder is whole things, never quarters of a book`() {
        // 12/16 = 0.75, which snaps to 0.5. "Log half a book" is not a smaller
        // amount of reading, it is a category error — so a count rounds the
        // snapped base to a whole number and floors it at one.
        assertThat(FillLadder.of(12.0, MeasureKind.COUNT))
            .containsExactly(1.0, 2.0, 3.0, 4.0).inOrder()
    }

    @Test
    fun `every count rung is a whole number at every target`() {
        listOf(1.0, 3.0, 7.0, 12.0, 50.0, 365.0, 10_000.0).forEach { target ->
            FillLadder.of(target, MeasureKind.COUNT).forEach { rung ->
                assertThat(rung % 1.0).isEqualTo(0.0)
            }
        }
    }

    @Test
    fun `the same target gives a fractional base for a kind that allows one`() {
        // The COUNT rule is a rule about COUNT, not a global floor: 12 litres
        // snaps to half-litre buttons perfectly well, where 12 books cannot.
        assertThat(FillLadder.of(12.0, MeasureKind.VOLUME).first()).isEqualTo(0.5)
        assertThat(FillLadder.of(12.0, MeasureKind.COUNT).first()).isEqualTo(1.0)
    }

    // ── Rungs past the target ──────────────────────────────────────

    @Test
    fun `rungs past the target are dropped rather than offered`() {
        // Three books: the ladder's base is 1, so a fourth button would be worth
        // more than the whole goal. Overshoot is legal (§1.5) — the objection is
        // clutter on a row of four, not illegality.
        assertThat(FillLadder.of(3.0, MeasureKind.COUNT))
            .containsExactly(1.0, 2.0, 3.0).inOrder()
    }

    @Test
    fun `the first rung is kept even when it exceeds the target`() {
        // A one-book goal still gets a button. An empty row on a goal the user
        // explicitly put into BUTTONS mode would read as a broken screen.
        assertThat(FillLadder.of(1.0, MeasureKind.COUNT)).containsExactly(1.0)
    }

    // ── Nothing to divide ──────────────────────────────────────────

    @Test
    fun `a goal with no positive target has no ladder`() {
        listOf(0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { target ->
            assertThat(FillLadder.of(target, MeasureKind.VOLUME)).isEmpty()
        }
    }

    // ── forGoal: the three ways a goal has no buttons ──────────────

    private fun goal(
        target: Double = 4.0,
        measure: Measure? = Measure(MeasureKind.VOLUME, "L"),
        mode: InputMode = InputMode.BUTTONS,
    ) = Goal(id = "g", title = "Drink water", targetValue = target, measure = measure, inputMode = mode)

    @Test
    fun `a buttons goal with a classified measure gets the ladder`() {
        assertThat(FillLadder.forGoal(goal()))
            .containsExactly(0.25, 0.5, 0.75, 1.0).inOrder()
    }

    @Test
    fun `a goal in any other input mode gets no buttons`() {
        listOf(InputMode.NUMBER, InputMode.TICK, InputMode.AUTO).forEach { mode ->
            assertThat(FillLadder.forGoal(goal(mode = mode))).isEmpty()
        }
    }

    @Test
    fun `a goal with no measure gets no buttons`() {
        assertThat(FillLadder.forGoal(goal(measure = null))).isEmpty()
    }

    @Test
    fun `an unclassified measure gets no buttons rather than a guess`() {
        // The migration's half-way state: the word survived a pre-#11 document,
        // the kind never existed. `C22` #44 asks; this does not guess, because
        // guessing is what would put quarter-books on a reading goal.
        assertThat(FillLadder.forGoal(goal(measure = Measure(null, "litres")))).isEmpty()
    }
}
