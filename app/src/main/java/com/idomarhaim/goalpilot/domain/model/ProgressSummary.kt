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

/** Per-goal slice used by summaries and the "percentage of time per goal" chart. */
data class GoalProgress(
    val goalId: String,
    val title: String,
    val category: GoalCategory,
    val fraction: Float,
    val points: Long = 0L,
)
