package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalStructure
import com.idomarhaim.goalpilot.domain.model.MeasureBasis
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
import com.idomarhaim.goalpilot.domain.model.ProposalOrigin
import com.idomarhaim.goalpilot.domain.model.TargetSource
import com.idomarhaim.goalpilot.domain.model.Task
import java.time.LocalDateTime

/**
 * §3.4's **mechanical proposal**, and the target arithmetic every proposal runs
 * through — spec §1.3, §3.3 E, §3.4 (`C22`
 * [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
 * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)).
 *
 * ## Two jobs, and the second one is why the first is not a fallback
 *
 * 1. [mechanical] builds a proposal **with no model behind it at all**.
 * 2. [withComputedTarget] takes a proposal a model *did* phrase and computes its
 *    number here, because §3.3 E forbids the number crossing the wire.
 *
 * Both end in the same [MeasureProposal], and job 2 goes through job 1's own
 * arithmetic. That is §3.4's claim made structural: *"`measure` is the one
 * feature whose fallback is not a degraded version of itself"* — the two paths
 * differ only in who authored [MeasureProposal.word], and the target is the same
 * sum either way. A mechanical proposal cannot be a *worse* number than a
 * model's, because the model never supplies a number.
 *
 * ## §0.1 is what makes job 1 mandatory
 *
 * *An AI feature that cannot run reliably on the free tier ships with a non-AI
 * fallback beside it, or is not specced at all.* Here that fallback is cheaper
 * than the feature it backs: `C18` and `C9a` already hold the counts, so it
 * needs no network, no key and no provider.
 *
 * ## Pure, and deliberately so
 *
 * Nothing here reads a repository, touches Firestore or applies anything. §1.3
 * says the offer **never auto-applies**, so a function that could write a goal
 * would be one refactor away from doing it silently. The caller renders what
 * this returns and the user's finger is the only thing that commits it.
 */
object ProposeMeasureUseCase {

    /**
     * The app's own word for §3.4's steps branch.
     *
     * English, and legal here: `domain/usecase` is not in
     * `AnalyticsLiteralSweepTest.SWEPT_PACKAGES`, and `AGENTS.md` §0.8's
     * suspension permits a plain literal in an unswept package. It becomes a
     * resource lookup at the call site when #51 reaches this package — which is
     * the *only* correct time, because the word is **content the moment it is
     * accepted** (§3.5) and must never be re-rendered afterwards.
     */
    const val STEPS_WORD: String = "steps"

    /** The app's own word for §3.4's schedule branch. Same rule as [STEPS_WORD]. */
    const val SCHEDULE_WORD: String = "a week"

    /**
     * Whether this goal may be offered a measure at all.
     *
     * Two conditions, and they are different questions:
     *
     * - **The goal has no measure.** §1.3 makes absence the default, so this is
     *   the whole population the feature addresses. It is `measure == null` and
     *   not [Goal.hasMeasure], which additionally requires a positive target —
     *   a goal carrying a word and a zero target has *answered* the question
     *   this offer asks, and re-asking it would be the offer arriving as a
     *   correction.
     * - **There is structure to compute from.** [GoalStructure.hasAnything];
     *   without it §3.4's last row applies and the app says nothing.
     *
     * **Dismissal is not checked here**, because it is not a property of the
     * goal — it is a per-install fact held by
     * `AppPreferencesRepository.isMeasureProposalDismissed`, and folding it in
     * would make this function untestable without a preference store for no
     * gain.
     */
    fun isEligible(goal: Goal, structure: GoalStructure): Boolean =
        goal.measure == null && structure.hasAnything

    /**
     * The structure a goal's tasks already carry, read as §3.3 E's two counts.
     *
     * ## `occurrencesPerWeek` is read from the tasks, because the recurrence rule
     * does not exist yet
     *
     * §2.1 makes a schedule *a set of occurrences*, and `#56` shipped **at most one
     * `when` per task** as four fields and said so; the flat `occurrences`
     * collection and the repeat rule are
     * [#63](https://github.com/idomarhaim/Android_Final_Project/issues/63) and are
     * not built. So what is countable today is *how many of this goal's open tasks
     * are scheduled inside the coming week*, which is the same number for every
     * goal `#63` will later describe with a rule — a goal running three times a
     * week has three such tasks now and will carry one weekly rule then.
     *
     * `Untested:` whether the two counts agree once `#63` lands. They should, and
     * this function is the one site that changes when it does; the target
     * arithmetic above never sees the difference.
     *
     * ## Done tasks are counted for the total and not for the window
     *
     * [GoalStructure.totalStepCount] is *the steps you already listed*, done
     * included, because it is the target and a target that shrinks as work is
     * completed is §0.3's second number that quietly disagrees.
     * [GoalStructure.openStepCount] is the wire's field and §3.4's gate, so it is
     * the open ones. The schedule window counts only tasks still open — an
     * occurrence already done is not a rhythm the goal is committed to, it is one
     * that has already happened.
     *
     * @param now the clock, passed in rather than read, so this stays pure and the
     *   week window is testable without freezing a system clock.
     */
    fun structureOf(tasks: List<Task>, now: LocalDateTime): GoalStructure {
        val weekEnd = now.plusDays(WEEK_DAYS)
        val open = tasks.filterNot { it.isDone }
        return GoalStructure(
            occurrencesPerWeek = open.count { task ->
                val opensAt = task.occurrence?.opensAt ?: return@count false
                !opensAt.isBefore(now) && opensAt.isBefore(weekEnd)
            },
            openStepCount = open.size,
            totalStepCount = tasks.size,
        )
    }

    /** §2.1's week, as the window [structureOf] counts occurrences in. */
    private const val WEEK_DAYS: Long = 7

    /**
     * §3.4's mechanical proposal — *the steps you already listed*, else *the
     * occurrences you already schedule*, else **nothing, silently**.
     *
     * ## The branch order is the spec's and it is not arbitrary
     *
     * Steps first. A goal with both a sub-tree and a schedule has two honest
     * answers, and the steps one is an [MeasureBasis.OUTCOME] — it says how far
     * along *the goal* is. The schedule one is a [MeasureBasis.LEADING]
     * indicator, which §1.3 offers precisely where an outcome number would have
     * to be faked. Given a real outcome available, offering the proxy instead
     * would be choosing the weaker of two true things.
     *
     * @return `null` for §3.4's silent row — no structure, so no proposal.
     */
    fun mechanical(goal: Goal, structure: GoalStructure): MeasureProposal? = when {
        structure.hasSteps -> MeasureProposal(
            goalId = goal.id,
            kind = MeasureKind.COUNT,
            word = STEPS_WORD,
            basis = MeasureBasis.OUTCOME,
            targetSource = TargetSource.STEPS,
            target = targetFor(TargetSource.STEPS, structure),
            origin = ProposalOrigin.MECHANICAL,
        )

        structure.hasSchedule -> MeasureProposal(
            goalId = goal.id,
            kind = MeasureKind.COUNT,
            word = SCHEDULE_WORD,
            basis = MeasureBasis.LEADING,
            targetSource = TargetSource.SCHEDULE,
            target = targetFor(TargetSource.SCHEDULE, structure),
            origin = ProposalOrigin.MECHANICAL,
        )

        else -> null
    }

    /**
     * Puts the app's own number on a proposal a model phrased — §3.3 E's
     * *"there is no number in the response"*.
     *
     * The model named a [TargetSource]; this runs it. Where that source has
     * nothing behind it — the model said `SCHEDULE` on a goal with no schedule —
     * the result is a proposal with a **null target**, which the screen renders
     * as *"the app will not invent a target"* rather than as an error. That is
     * the same shape §3.4 gives every other absence: a missing value is
     * distinguishable from a wrong one, and only the missing one is safe.
     */
    fun withComputedTarget(
        proposal: MeasureProposal,
        structure: GoalStructure,
    ): MeasureProposal =
        proposal.copy(target = targetFor(proposal.targetSource, structure))

    /**
     * The arithmetic itself, and the whole of it.
     *
     * `STEPS` reads [GoalStructure.totalStepCount] rather than the wire's
     * `openStepCount` — a target that shrinks as steps are completed is §0.3's
     * second number that quietly disagrees. [GoalStructure] carries the full
     * argument.
     *
     * `USER` is `null` by definition and not by failure, so it is listed rather
     * than left to an else: the reader should be able to see that the app
     * declines to answer here on purpose.
     */
    private fun targetFor(source: TargetSource, structure: GoalStructure): Double? =
        when (source) {
            TargetSource.STEPS ->
                structure.totalStepCount.takeIf { it > 0 }?.toDouble()

            TargetSource.SCHEDULE ->
                structure.occurrencesPerWeek.takeIf { it > 0 }?.toDouble()

            TargetSource.USER -> null
        }
}
