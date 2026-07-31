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
    /** Average completion across the included goals, 0f..1f. */
    val averageProgress: Float
        get() = if (goals.isEmpty()) 0f else goals.map { it.fraction }.average().toFloat()
}

/** Per-goal slice used by summaries and the "percentage of time per goal" chart. */
data class GoalProgress(
    val goalId: String,
    val title: String,
    val category: GoalCategory,
    val fraction: Float,
    val points: Long = 0L,
)
