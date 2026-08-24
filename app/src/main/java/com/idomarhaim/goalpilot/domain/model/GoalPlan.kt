package com.idomarhaim.goalpilot.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Where a **goal** belongs, judged from its title alone — the answer to `fileGoal`
 * (`docs/PRODUCT_v0.3.md` §3.3 D's schema, §0.7's silence rule; Ido 2026-08-24).
 *
 * ### Both fields are nullable, and both nulls mean *nobody said*
 *
 * §3.3: *"a field that fails validation is **absent**"*. So [lifeAreaId] absent leaves the goal
 * **unfiled** — §1.2 makes that a legitimate state, not a degraded one — and [category] absent
 * leaves whatever the form already had.
 *
 * [category] in particular must **not** be defaulted to [GoalCategory.OTHER] on the way in, the
 * way [GoalCategory.fromName] does for a stored value. Reading it that way would let a call that
 * said nothing overwrite a category the user picked by hand with `OTHER`, which is a silent
 * downgrade dressed as an answer. `null` here is the honest read, and the caller keeps its own
 * value.
 *
 * ### It is applied silently, and that is §0.7 rather than taste
 *
 * > The app may act silently on **instrumental** structure, but must ask before asserting an
 * > **intrinsic** edge.
 *
 * Filing decides which column of the time chart the goal reports into and is one tap to change;
 * deciding the goal **exists** is the user's own act, because they typed the title. Same verdict
 * [FilingDecision] already reached for tasks, and the same reason there is no toggle for it.
 */
data class GoalFiling(
    /** A [LifeArea.id] the caller sent in, or `null` — resolved, never re-validated (§3.4). */
    val lifeAreaId: String? = null,
    /** The category the model picked, or `null` for *it did not say*. */
    val category: GoalCategory? = null,
    /** 0..1. Speech, kept for the same reason `TaskClassification.confidence` is. */
    val confidence: Float = 0f,
    /** Speech — why it filed the goal there. Never rendered as a claim about the user. */
    val rationale: String = "",
) {
    /** Whether this filing says anything at all. A call that answered nothing is not applied. */
    val isEmpty: Boolean get() = lifeAreaId == null && category == null
}

/**
 * What one step of an AI-proposed plan **is** — §3.3 B's `label`, as two values.
 *
 * §3.7: *"a stage is simply a **milestone or a task**, decided per item by *state-you-reach* vs
 * *work-you-do*"*, and *"the AI picks the plan's shape per goal, but never as a separate
 * judgement: the model labels **each item**, and the shape **falls out**."*
 *
 * The distinction is not cosmetic and it is not a display hint — it decides three things
 * downstream, all of them in [PlanStep]:
 *
 * | | [MILESTONE] | [WORK] |
 * |---|---|---|
 * | priced? | never — `C18`'s container rule | yes, on its minutes × difficulty (§1.4) |
 * | rung | [Deadline] — a moment you owe it by | [Block] with a time, else [AllDay] |
 * | can hold a slot? | no | yes |
 */
enum class PlanStepKind {

    /**
     * A **state you reach**. Never priced: `C18`'s container rule, and §3.3 B enforces it in the
     * Cloud Function by stripping `difficulty`/`estimatedMinutes`/`timeOfDay` from such an item
     * rather than by trusting the prompt.
     */
    MILESTONE,

    /** **Work you do** — a thing the person sits down and does, and the only kind that is priced. */
    WORK;

    companion object {

        /**
         * §3.3 B's `label` for [MILESTONE], as the **wire** spells it.
         *
         * ⚠️ **It is not this enum's own name, and that is the trap.** The constant is
         * `MILESTONE` because that is what it is called throughout the app; the wire says
         * `STATE_YOU_REACH` because that is what §3.3 B calls it. Matching on
         * `MILESTONE.name` compiles, reads correctly and is **always false** — every milestone
         * silently arrives as [WORK], which is the safe direction and therefore the one nothing
         * would have complained about. Caught by `GoalPlanTest`, not by the compiler.
         */
        private const val WIRE_MILESTONE = "STATE_YOU_REACH"

        /**
         * The wire's `label`, or [WORK] when it is unusable.
         *
         * [WORK] rather than [MILESTONE] is the safe direction and worth stating: an
         * unrecognised label reaching here at all would already be a validator failure, and of
         * the two readings, *"a step you do"* leaves the user a task they can tick, while
         * *"a state you reach"* would silently make it unpriceable.
         */
        fun fromName(name: String?): PlanStepKind =
            if (name.equals(WIRE_MILESTONE, ignoreCase = true)) MILESTONE else WORK
    }
}

/**
 * One step of a proposed plan, **before** it is anything in the database.
 *
 * ### It carries no id from the model, and that is structural (§3.3 B)
 *
 * > The model may not mint an id. A new step carries **no `id` at all** and is identified by its
 * > position; the client assigns the id on receipt — making the truncation failure
 * > **structurally unrepresentable** rather than merely checked.
 *
 * [index] is that position and is assigned here, on receipt. Nothing echoed from the model is
 * ever an identifier.
 *
 * ### It carries an offset, not a date
 *
 * [dayOffset] is **days from the day the plan is applied**, which is the client's own clock. §3.7
 * says a draft *"persists exactly as left … no expiry"*, so a plan drawn on Monday and accepted
 * on Friday must not schedule Monday's dates — an offset survives that and an absolute date does
 * not. The resolution happens in exactly one place, [PlanStep.occurrenceOn].
 *
 * ### [keep] is the user's, and it is the whole gate
 *
 * §3.7: *"nothing the model decides here may reach Firestore without passing his eyes."* A step
 * is proposed with `keep = true` and dropping it is one tap; nothing is written until the sheet
 * is confirmed, and what is written is exactly the steps still carrying `keep`.
 */
data class PlanStep(
    /** Position in the proposed plan, assigned on receipt. The step's only identity. */
    val index: Int,
    /** The step, as the model wrote it, in the language it was asked in. Content (`C15b`). */
    val title: String,
    val kind: PlanStepKind,
    /**
     * How demanding the work is — the multiplier half of §1.4's points. Always
     * [Difficulty.ROUTINE] (×1.0) on a [PlanStepKind.MILESTONE], which is the absence of a
     * judgement rather than a guess at one.
     */
    val difficulty: Difficulty = Difficulty.ROUTINE,
    /** Minutes for one sitting, or `null` for *the model did not say* (§3.4, `#9`). */
    val estimatedMinutes: Int? = null,
    /** Days from the day the plan is applied, or `null` for a step with no *when* at all. */
    val dayOffset: Int? = null,
    /** The slot in the day, or `null` for a step that is simply due that day. */
    val timeOfDay: LocalTime? = null,
    /** Whether the user is keeping this step. The draft gate, and it is theirs. */
    val keep: Boolean = true,
) {

    /**
     * The **when** this step becomes, resolved against the day the plan is applied — or `null`
     * for a step that is simply on the list.
     *
     * ### The rung falls out of the label; it is never a fifth thing the model chose
     *
     * §3.7 again: *"the model labels each item, and the shape falls out"*. So:
     *
     * - a [PlanStepKind.MILESTONE] is a [Deadline] — §2.2's *"a moment you owe something by"*,
     *   whose miss is `OVERDUE`, which is **not a failure**. That is the right reading of a
     *   milestone slipping: it is still owed, and nobody failed at 09:00 on a Tuesday.
     * - a [PlanStepKind.WORK] with a slot is a [Block] — §2.2's *"a span of time you are
     *   inside"*, whose miss is a real `MISSED`.
     * - a [PlanStepKind.WORK] with no slot is an [AllDay] — the day passed, nothing more.
     *
     * ### The block is [BlockPlacement.PROVISIONAL], and this is the first code that writes one
     *
     * [BlockPlacement]'s own KDoc names this: *"The agent that would write `PROVISIONAL` is
     * §3.7's proposed plan (`#24`) and does not exist"*. It does now. §2.4 is why — *"a `BLOCK`
     * needs confirmation, because 09:00 may already be taken"* — and §2.3 is what it buys: an
     * unconfirmed block whose time passes is `EXPIRED` and **silent**, so a plan the user
     * accepted and then ignored does not manufacture a wall of failures against them.
     *
     * A milestone's [Deadline] and an [AllDay] need no such state: §2.4 says those rungs *"occupy
     * no slot and cannot collide → the agent sets them silently"*.
     *
     * @param appliedOn the day the plan is being applied — the caller's clock, never a default
     */
    fun occurrenceOn(appliedOn: LocalDate): Occurrence? {
        val date = dayOffset?.let { appliedOn.plusDays(it.toLong()) } ?: return null
        if (kind == PlanStepKind.MILESTONE) return Deadline(date.atTime(MILESTONE_DUE_AT))
        val start = timeOfDay ?: return AllDay(date)
        return Block(
            start = LocalDateTime.of(date, start),
            end = LocalDateTime.of(date, start).plusMinutes(
                (estimatedMinutes ?: TaskDuration.DEFAULT_MINUTES).toLong(),
            ),
            placement = BlockPlacement.PROVISIONAL,
        )
    }

    private companion object {
        /**
         * When a milestone falls due on its day.
         *
         * End of the working day rather than midnight, because a [Deadline] at `00:00` is owed
         * before the day it names has started — which reads as a whole day late to anyone
         * looking at it, and would fire §2.5's reminder against the wrong day entirely.
         */
        val MILESTONE_DUE_AT: LocalTime = LocalTime.of(18, 0)
    }
}

/**
 * A whole proposed plan for one goal — §3.7's **draft**, held in memory until the user accepts it.
 *
 * ### Why this is not a Firestore document (yet), and what that costs
 *
 * §3.7 wants a persisted draft: *"the draft persists exactly as left — a real object, one per
 * goal, **no expiry**"*, which is what makes *"the duplicate check runs on open as well as on
 * commit"* possible. This type is deliberately **not** stored: what shipped is the propose →
 * review → accept path Ido asked for, and persisting a draft adds a collection, a migration and
 * a second answer to *"what is this goal's plan?"* that nothing yet reads.
 *
 * `Untested:` the consequence is that closing the sheet loses the draft and the plan must be
 * re-requested — one call against a 30-RPM ceiling, on a user-invoked feature. §3.7's `Adjust
 * Plan`, its three-exits-per-step and its duplicate detection all sit behind that persistence and
 * are **not** here; they are the rest of `#24`.
 */
data class GoalPlan(
    /** The goal this plan is for. */
    val goalId: String,
    /** The steps, in the order the model proposed them. Position is identity (§3.3 B). */
    val steps: List<PlanStep> = emptyList(),
    /** §3.3 B's `changeNotes` — what an adjustment changed. Empty on a first proposal. */
    val changeNotes: List<String> = emptyList(),
) {
    /** The steps the user is keeping — exactly what [GoalPlan] would write. */
    val kept: List<PlanStep> get() = steps.filter { it.keep }

    /** Whether the model proposed nothing. The client **reports** this rather than going quiet. */
    val isEmpty: Boolean get() = steps.isEmpty()
}
