package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskScoring
import org.junit.Test

/**
 * §1.4's points formula — `round(minutes / 3) × difficulty`, `#55`.
 *
 * ### What is actually at stake here
 *
 * This is a **currency**, and the ticket changes how every point in the app is computed. Three
 * separate claims in §1.4 are only true if this arithmetic is exactly right, and each has its
 * own case below:
 *
 * 1. *"Today's anchor survives exactly"* — a 30-minute routine task is still worth 10. §1.4
 *    inverts an existing constant rather than introducing one precisely so that the user's
 *    history is not re-priced by the change.
 * 2. *"The `5..50` cap is deleted"* — in **both** directions. The ceiling is the interesting
 *    half in the spec, and the floor is the one a reader forgets: a short, light task is worth
 *    4 and is not rounded up to 5.
 * 3. *"The levelling ceiling rises 50 → 240"* — 240 is not a constant anybody wrote down. It
 *    is what the formula yields at the storable duration maximum, so it moves if that maximum
 *    moves, which a written-down `MAX_POINTS` would not have done.
 *
 * The same rule exists in TypeScript (`functions/src/derived.ts`, `pointsOf`) because the
 * projection function has to reach the same total; `shared-fixtures/derived-state.json` is
 * what holds the two together, and `DerivedStateFixtureTest` is the reader on this side.
 * These cases are the ones that are only interesting in one language — boundaries and
 * rounding order — and would be noise in a cross-language fixture.
 */
class TaskScoringTest {

    // ── The anchor ───────────────────────────────────────────────────

    @Test
    fun `a thirty-minute routine task is still worth ten points`() {
        assertThat(TaskScoring.pointsFor(30, Difficulty.ROUTINE)).isEqualTo(10)
    }

    // ── Every difficulty ─────────────────────────────────────────────

    @Test
    fun `each difficulty multiplies the same effort by its own factor`() {
        // 60 minutes is round(60/3) = 20 effort points, before any judgement is applied.
        assertThat(TaskScoring.pointsFor(60, Difficulty.LIGHT)).isEqualTo(15)
        assertThat(TaskScoring.pointsFor(60, Difficulty.ROUTINE)).isEqualTo(20)
        assertThat(TaskScoring.pointsFor(60, Difficulty.DEMANDING)).isEqualTo(30)
    }

    @Test
    fun `the multipliers are the three names section 1_4 gives, and they are ordered`() {
        // Asserted against the enum rather than against three literals, so a multiplier
        // edited in `Difficulty` fails here rather than only in whatever screen renders it.
        assertThat(Difficulty.entries.map { it.multiplier }).containsExactly(0.75, 1.0, 1.5).inOrder()
        assertThat(Difficulty.ROUTINE.multiplier).isEqualTo(1.0)
    }

    @Test
    fun `points rise with both inputs, monotonically`() {
        // A currency that is not monotonic in effort is one the user cannot reason about:
        // a longer task must never be worth less than a shorter one at the same difficulty.
        for (difficulty in Difficulty.entries) {
            var previous = -1
            for (minutes in TaskDuration.MIN_MINUTES..TaskDuration.MAX_MINUTES) {
                val points = TaskScoring.pointsFor(minutes, difficulty)
                assertThat(points).isAtLeast(previous)
                previous = points
            }
        }
        for (minutes in listOf(5, 30, 90, 480)) {
            assertThat(TaskScoring.pointsFor(minutes, Difficulty.LIGHT))
                .isAtMost(TaskScoring.pointsFor(minutes, Difficulty.ROUTINE))
            assertThat(TaskScoring.pointsFor(minutes, Difficulty.ROUTINE))
                .isAtMost(TaskScoring.pointsFor(minutes, Difficulty.DEMANDING))
        }
    }

    // ── The cap the ticket deletes, at both ends ─────────────────────

    @Test
    fun `a 240-point task is expressible, which the old ceiling made impossible`() {
        // §1.4: "the levelling ceiling rises 50 to 240". 480 minutes is the storable maximum
        // and DEMANDING is ×1.5, so this is what the formula YIELDS — there is no constant.
        assertThat(TaskScoring.pointsFor(TaskDuration.MAX_MINUTES, Difficulty.DEMANDING))
            .isEqualTo(240)
    }

    @Test
    fun `a four-point task is not raised to five`() {
        // The floor half of "the 5..50 cap is deleted", and the one a reader forgets.
        // round(15/3) = 5, ×0.75 = 3.75, rounded = 4. `MIN_POINTS` would have made it 5.
        assertThat(TaskScoring.pointsFor(15, Difficulty.LIGHT)).isEqualTo(4)
    }

    @Test
    fun `an eight-hour task is no longer priced like a ninety-minute one`() {
        // §1.4's stated reason for deleting the cap. Under `5..50` both of these were 50.
        val ninetyMinutes = TaskScoring.pointsFor(90, Difficulty.ROUTINE)
        val eightHours = TaskScoring.pointsFor(480, Difficulty.ROUTINE)

        assertThat(ninetyMinutes).isEqualTo(30)
        assertThat(eightHours).isEqualTo(160)
    }

    @Test
    fun `the smallest storable task is still worth something`() {
        // Never zero: a completed task worth nothing is indistinguishable from one that was
        // not counted, which is the failure `TaskDuration.minutesOf` guards at the chart.
        for (difficulty in Difficulty.entries) {
            assertThat(TaskScoring.pointsFor(TaskDuration.MIN_MINUTES, difficulty)).isAtLeast(1)
        }
    }

    // ── The rounding order §1.4 writes the formula in ────────────────

    @Test
    fun `the effort half is rounded before the judgement is applied`() {
        // `round(minutes / 3) × difficulty`, not `round(minutes / 3 × difficulty)`. On 50
        // minutes the two disagree: round(50/3) = 17, ×1.5 = 25.5 -> 26, whereas the other
        // order gives round(25.0) = 25. §1.4 writes it the first way, and the reason is that
        // the effort half is a whole number of points in its own right before anybody judges
        // the work — which is also what makes the 30-minute anchor exact rather than
        // floating-point-dependent.
        assertThat(TaskScoring.pointsFor(50, Difficulty.DEMANDING)).isEqualTo(26)
    }

    @Test
    fun `three minutes is one point, which is the constant that was inverted`() {
        assertThat(TaskScoring.MINUTES_PER_POINT).isEqualTo(3)
        assertThat(TaskScoring.pointsFor(3, Difficulty.ROUTINE)).isEqualTo(1)
    }

    // ── The lifetime total is a sum over facts ───────────────────────

    @Test
    fun `the lifetime total is a sum over banked facts, and an untick removes exactly one`() {
        // §1.4's whole argument for banking the inputs, as arithmetic. The defect `C20` fixed
        // was a running accumulator that read `task.points` AT UNTICK TIME — tick at 10,
        // re-score to 30, untick, and the total lost 30 for a 10. A sum over facts cannot
        // drift because it never remembers what it added last time, and the fact it removes
        // carries the number it was worth when it was added.
        val a = CompletionFact(completedAtEpochMillis = 1L, minutes = 30) // 10
        val b = CompletionFact(completedAtEpochMillis = 2L, minutes = 90) // 30

        val both = listOf(a, b).sumOf { it.points }
        val afterUntickingB = listOf(a).sumOf { it.points }

        assertThat(both).isEqualTo(40)
        assertThat(both - afterUntickingB).isEqualTo(b.points)
        assertThat(afterUntickingB).isEqualTo(10)
    }

    @Test
    fun `re-estimating a task after the tick cannot move what its fact is worth`() {
        // The same defect from the other side, and the reason the fact carries minutes rather
        // than a task id. There is no path from a later edit to this number.
        val banked = CompletionFact(completedAtEpochMillis = 1L, minutes = 30)

        assertThat(banked.points).isEqualTo(10)
        assertThat(banked.copy().points).isEqualTo(10)
    }
}
