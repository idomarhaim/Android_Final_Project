package com.idomarhaim.goalpilot.domain.model

import com.idomarhaim.goalpilot.core.util.SummaryPeriod

/**
 * A computed, shareable snapshot of the user's achievement over a time window
 * (spec §7: daily / weekly / monthly / yearly summaries).
 */
data class ProgressSummary(
    val period: SummaryPeriod = SummaryPeriod.WEEKLY,
    val totalPoints: Long = 0L,
    val completedTasks: Int = 0,
    val activeGoals: Int = 0,
    val goals: List<GoalProgress> = emptyList(),
    val generatedAtEpochMillis: Long = 0L,
) {
    /**
     * Average completion across the included goals, `0f..1f`.
     *
     * Bounded again, and by construction rather than by a clamp on the goals: see
     * [DerivedProgress.overallCompletion]. It was briefly a plain mean of unbounded
     * fractions, between #49 deleting `progressFraction`'s clamp and the device
     * pass finding what that produced.
     *
     * **This one is the more urgent of the two sites**, because it does not stay on
     * the user's own screen: `SocialRepositoryImpl:189` rounds it into the text of a
     * **shared post**, so an absurd number here is published to other people rather
     * than merely displayed.
     */
    val averageProgress: Float
        get() = DerivedProgress.overallCompletion(goals.map { it.fraction })
}

/**
 * Per-goal slice used by summaries and the "percentage of time per goal" chart.
 *
 * ### `points` is gone, and `effortMinutes` is not a rename
 *
 * §1.4: *"Points are never rendered as a property of an objective. `GoalProgress.points` is
 * deleted; the goal header's companion number becomes **effort** — '4h 20m of work logged
 * toward this'."*
 *
 * Half of `R12` was exactly this layout fact: a goal % and `+40 pts` published side by side
 * as one object by `BuildSummaryUseCase`, with no stated relationship between them. Points
 * are a **view of effort** (`round(minutes / 3) × difficulty`), so putting them beside a
 * *fraction of a target* invited the reading that one explains the other. They measure
 * different things — [fraction] is outcome, [effortMinutes] is effort — and §1.4 calls the
 * gap between them the app's most valuable signal, which is only legible once both sides are
 * named in their own units.
 *
 * So this is not `points` renamed: it is the quantity underneath it, in the unit it was
 * actually measured in, with the currency left where it belongs — on the person's own
 * lifetime total.
 */
data class GoalProgress(
    val goalId: String,
    val title: String,
    val category: GoalCategory,
    /** Outcome: how far the measure has come, `0f..1f` before the goal is beaten. */
    val fraction: Float,
    /** Effort: minutes of completed work banked against this goal in the window. */
    val effortMinutes: Int = 0,
)
