package com.idomarhaim.goalpilot.domain.model

/**
 * A concrete measure the app offers for a goal that has none — spec §1.3 (`C7`
 * #14) as `C22` [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44)
 * resolved it, and [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)'s
 * whole subject.
 *
 * ## It is an offer, and every field here exists to keep it one
 *
 * §1.3: unmeasured is **legal but never silent**, and absence is the **default**
 * (`E6`). So nothing that constructs this type applies it — it is rendered, and
 * [Goal.measure] changes only when the user presses the accept button.
 *
 * ## There is no number on the wire, and that is the whole design (§3.3 E)
 *
 * [targetSource] is a **prompt-declared enum naming which arithmetic the app
 * runs**, not a value. `C11a` measured this model's free numbers swinging **2×
 * run-to-run** and **1.8× between languages**, and the target is the one field
 * the feature would be judged on — so [target] is computed by
 * [com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase] from structure
 * the app already holds (§0.5, *the AI judges and the app computes*).
 *
 * That is why [target] is nullable and [targetSource] is not: `USER` is a real
 * answer meaning *ask him*, and it is not the same as *the arithmetic produced
 * nothing*. Both read as a null [target]; only [targetSource] says which.
 *
 * ## A proposal is whole or it does not exist
 *
 * §3.3 E: *"a proposal missing `measureKind`, `word` or `targetSource` is not a
 * proposal, so the element is dropped whole — there is no partial measure."*
 * Every field below except [target] is therefore non-null, and the Cloud
 * Function's validator (`functions/src/measure.ts`) drops the element rather
 * than handing over a half-built one. This differs from `classify`, whose every
 * field stands or falls alone — the contracts are per-feature (§3.4).
 */
data class MeasureProposal(
    /**
     * The goal being offered a measure. **Membership-checked in the Cloud
     * Function** against the ids the request carried — `C11a`'s only measured
     * failure class across 248 live calls was a plausible id that was not real.
     */
    val goalId: String,
    /** §1.3's closed kind. App logic, so its label is translated. */
    val kind: MeasureKind,
    /**
     * The goal's own word for what it counts — `"runs a week"`, `"steps"`.
     *
     * **Content, not speech** (§3.5): proposed once, and his the moment he
     * accepts it. Never re-rendered and never translated afterwards (`C15b`).
     * On a [ProposalOrigin.MECHANICAL] proposal it is the *app's* word rather
     * than a model's, which is the one case where a later language switch would
     * arguably be legal — and it still does not happen, because by then it is
     * sitting in his goal as content like any other.
     */
    val word: String,
    /** Whether this measures the goal itself or the behaviour that produces it. */
    val basis: MeasureBasis,
    /** Which arithmetic the app runs to get [target]. */
    val targetSource: TargetSource,
    /**
     * The number the app computed, or `null` when there was nothing to compute
     * from — which under [TargetSource.USER] is the *expected* state rather than
     * a failure.
     */
    val target: Double?,
    /** Whether a model phrased this, or the app counted it. */
    val origin: ProposalOrigin,
) {
    /**
     * Whether accepting this would give the goal a target, or only a kind and a
     * word with the number still to come from the user.
     *
     * The screen branches on it to decide what the accept button completes.
     */
    val hasTarget: Boolean get() = (target ?: 0.0) > 0.0

    /** The measure this proposal would become if accepted. */
    fun toMeasure(): Measure = Measure(kind = kind, word = word)
}

/**
 * Whether a proposed measure counts the **goal** or the **behaviour that
 * produces it** — §1.3's *"may propose a leading indicator rather than fake an
 * outcome number"*.
 *
 * The distinction is not cosmetic: it is what lets the app offer a number for a
 * goal whose outcome is genuinely unquantifiable. *Get fit* has no honest
 * outcome number, and inventing one is the failure §1.3 names; *three runs a
 * week* is a fact already sitting on the goal's schedule.
 */
enum class MeasureBasis {
    /** A number saying how far along the goal itself is. */
    OUTCOME,

    /** The recurring behaviour, not the result. */
    LEADING,
    ;

    companion object {
        /** The wire name back to a member, or `null` if unrecognised. */
        fun fromName(name: String?): MeasureBasis? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * **Which arithmetic the app runs** for the target — §3.3 E's forced enum.
 *
 * Forced rather than chosen, and the choice is `C11a`'s measurement rather than
 * a preference: enums came back **50/50 perfect** and free numbers swung 2×, so
 * the model names the *source* and the app does the sum. §0.5 at full strength —
 * *never let a value cross the wire whose failure you would have to detect.*
 */
enum class TargetSource {
    /** The occurrences already scheduled on the goal (`C9a`). */
    SCHEDULE,

    /** The count of the goal's own steps (`C18`). */
    STEPS,

    /**
     * Nothing to compute from — **ask him**.
     *
     * A legitimate answer, not a fallback. A goal with neither a schedule nor a
     * sub-tree can still carry a perfectly good *kind* and *word*; what it
     * cannot carry is a target the app made up.
     */
    USER,
    ;

    companion object {
        /** The wire name back to a member, or `null` if unrecognised. */
        fun fromName(name: String?): TargetSource? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * Who phrased the proposal — and it is shown, because §0.4 makes the app's own
 * arithmetic something it may say out loud.
 *
 * §3.4: *"`measure` is the one feature whose fallback is not a degraded version
 * of itself."* Where the goal already carries structure the proposal is **pure
 * arithmetic**, and the surface is unchanged — same component, same two buttons,
 * only the wording is the app's rather than the model's. So this is not an error
 * flag: a [MECHANICAL] proposal is a first-class one, and the copy says so
 * (*"nothing was generated here"*), which is a stronger claim than a model's and
 * not a weaker one.
 */
enum class ProposalOrigin {
    /** A model phrased the kind and the word (§3.3 E). */
    MODEL,

    /** The app counted what was already there (§3.4's mechanical proposal). */
    MECHANICAL,
}

/**
 * The structure a goal already carries, and the only input the target arithmetic
 * has — `C9a`'s schedule and `C18`'s sub-tree, which §3.3 E puts on the wire as
 * `occurrencesPerWeek` and `openStepCount`.
 *
 * ## [totalStepCount] is not on the wire, and it is not redundant
 *
 * §3.3 E sends the model `openStepCount`, and §3.4 gates the mechanical branch
 * on `openStepCount >= 2`. Both are obeyed unchanged. But the **target** is
 * [totalStepCount], because a target of *the open steps* shrinks every time one
 * is completed — a goal at `0/6` that reads `0/5` after real progress is §0.3's
 * *second number that quietly disagrees*, on the very screen this ticket exists
 * to fix. The prototype renders the same reading: frame 4's offer says
 * `2 of 8 done`, which is the total.
 *
 * At the moment §3.3 E fires the call — *when the goal first becomes eligible* —
 * nothing is done yet and the two counts are equal, so the divergence only ever
 * appears on a goal the offer reaches late.
 */
data class GoalStructure(
    /** Occurrences a week the goal's schedule already sets (`C9a`). */
    val occurrencesPerWeek: Int = 0,
    /** Steps listed under the goal and not yet done (`C18`). */
    val openStepCount: Int = 0,
    /** Steps listed under the goal, done or not. The stable target. */
    val totalStepCount: Int = 0,
) {
    /** §3.4's steps branch gate. */
    val hasSteps: Boolean get() = openStepCount >= 2

    /** §3.4's schedule branch gate. */
    val hasSchedule: Boolean get() = occurrencesPerWeek >= 1

    /**
     * Whether there is anything at all to compute a target from.
     *
     * `false` is §3.4's last row — **no proposal at all, silently**. Silent is
     * right by §0.4 as `C13` §5 refined it: *speak about a failure the user can
     * act on*. A goal with no structure is not broken; it simply has no number,
     * which §1.3 already made legal.
     */
    val hasAnything: Boolean get() = hasSteps || hasSchedule
}
