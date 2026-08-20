package com.idomarhaim.goalpilot.domain.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The amounts a goal's fill buttons are worth — spec §1.3, `R25`,
 * [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11).
 *
 * > **Fill buttons:** the **AI judges, the app computes**. The model answers only
 * > *do buttons fit, and what is counted*; the ladder is **`target / 16` rounded,
 * > at `1× 2× 3× 4×`**, because free numbers swing 2×.
 *
 * ### Why it is arithmetic and not a table
 *
 * §3.1 measured a model's free numbers swinging **2× run to run** and **1.8×
 * between languages**, so a ladder the model authors is a different ladder every
 * time it is asked — and a hard-coded millilitre table is worse still, because it
 * is right for one unit in one language and silently absent for every other. One
 * formula is right at any target, in any unit, in any language, and can be
 * checked by a test. The model's remaining job is the judgement it is good at:
 * *do buttons suit this goal at all*, which is [Goal.inputMode], and *what is
 * counted*, which is [Measure].
 *
 * ### The rounding rule is this file's, and it is a choice
 *
 * §1.3 says *rounded* and names two worked examples — 4 L and 40 L — and **both
 * divide exactly**, so neither constrains the rule. `Inferred:` from what a fill
 * button is for. A button is *tapped*, repeatedly, so its label has to be legible
 * at a glance and its repeat count has to be trackable in the head; `6.3` is
 * neither. So the base is snapped to the nearest **1 · 2 · 2.5 · 5 × 10ⁿ** — the
 * standard chart-axis tick ladder, chosen for exactly this readability property —
 * and it is the `2.5` rung that makes §1.3's own 4 L example land on
 * `0.25 / 0.5 / 0.75 / 1` rather than near it.
 *
 * `Observed:` the two spec examples reproduce exactly; `FillLadderTest` pins both.
 * `Untested:` whether Ido prefers a coarser ladder on large counts — that is a
 * judgement, not arithmetic, and it is one line here when he says.
 *
 * ### What it does not do: sub-unit prefixes
 *
 * §1.3 writes the 4 L example as `[250 ml] [500 ml] [750 ml] [1 L]`; this returns
 * `0.25 · 0.5 · 0.75 · 1`, **the same amounts in the goal's own word**. Rendering
 * `0.25 L` as `250 ml` needs the app to know that the user's word means litres,
 * and §1.3 makes the word *user content* — it may be `"ליטר"`, `"L"`, or a typo.
 * The only mechanisms that would bridge it are a unit table keyed on that text,
 * which is the string matching this ticket forbids, or a dimensional model, which
 * §1.3 rejects outright ("buys conversions this app never performs"). So the
 * numbers are the spec's and the spelling is one step short of it, deliberately.
 */
object FillLadder {

    /** §1.3: the base is `target / 16`. */
    const val DIVISOR = 16.0

    /** §1.3: the rungs are `1× 2× 3× 4×` the base. */
    const val RUNGS = 4

    /**
     * The mantissas the base snaps to, ascending. Ties go to the smaller — more
     * taps is a recoverable annoyance, a button too big for the goal is not.
     */
    private val NICE = doubleArrayOf(1.0, 2.0, 2.5, 5.0, 10.0)

    /**
     * The buttons for [goal], or empty when it has none to offer.
     *
     * Empty in three cases, and each is a real state rather than a failure:
     * a goal not in [InputMode.BUTTONS]; a goal with no measure or one whose kind
     * was never recorded (`C22` #44 asks, this does not guess); and a goal with
     * no positive target, which is nothing to divide.
     */
    fun forGoal(goal: Goal): List<Double> {
        if (goal.inputMode != InputMode.BUTTONS) return emptyList()
        val kind = goal.measure?.kind ?: return emptyList()
        return of(goal.targetValue, kind)
    }

    /**
     * `target / 16` snapped to a readable base, at `1× 2× 3× 4×`, with any rung
     * past the target dropped.
     *
     * **Rungs past the target are dropped rather than clamped**, and the first is
     * always kept. On *"read 3 books"* the ladder would otherwise offer a `4`
     * button on a 3-book goal: overshoot is legal (§1.5) and is not the objection
     * — four buttons where two are usable is clutter, and clutter on a row this
     * small is the whole feature. Keeping the first rung unconditionally means a
     * goal always has *some* button rather than an empty row.
     */
    fun of(targetValue: Double, kind: MeasureKind): List<Double> {
        if (!targetValue.isFinite() || targetValue <= 0.0) return emptyList()
        val base = baseFor(targetValue / DIVISOR, kind)
        if (base <= 0.0) return emptyList()
        val rungs = (1..RUNGS).map { tidy(base * it) }
        return rungs.filterIndexed { index, rung -> index == 0 || rung <= targetValue }
    }

    /**
     * The first rung, snapped.
     *
     * Every kind snaps to the readable ladder first — a 70,000-step goal wants
     * `5000` buttons, not `4375` ones, exactly as a 100-litre goal wants `5`
     * rather than `6.25`.
     *
     * [MeasureKind.COUNT] then takes one further step, because it is the one kind
     * that cannot carry a fractional base: *"log 0.25 books"* is not a smaller
     * amount of reading, it is a category error. So a count rounds to a whole
     * number and floors at one, which is what turns a 12-book goal's `0.5` into a
     * `[1] [2] [3] [4]` ladder rather than into halves of a book.
     */
    private fun baseFor(raw: Double, kind: MeasureKind): Double {
        val exponent = floor(log10(raw))
        val power = 10.0.pow(exponent)
        val mantissa = raw / power
        val snapped = tidy((NICE.minByOrNull { abs(it - mantissa) } ?: 1.0) * power)
        return if (kind == MeasureKind.COUNT) {
            snapped.roundToLong().coerceAtLeast(1L).toDouble()
        } else {
            snapped
        }
    }

    /**
     * Rounds off binary-representation noise — `0.1 * 3` is `0.30000000000000004`,
     * and a fill button labelled that is worse than any rounding rule could be.
     *
     * Six decimals is far below anything a person logs and far above anything the
     * snapping above can produce, so it removes the artefact without moving a
     * value the ladder meant.
     */
    private fun tidy(value: Double): Double = (value * 1_000_000.0).roundToLong() / 1_000_000.0
}
