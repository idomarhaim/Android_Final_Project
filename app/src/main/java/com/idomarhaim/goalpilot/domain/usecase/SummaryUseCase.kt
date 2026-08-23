package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalProgress
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import javax.inject.Inject

/**
 * Pure computation that turns the user's goals + tasks into a shareable
 * [ProgressSummary] for a time window (spec §7). Kept free of Android/Firebase
 * types so it is trivially unit-testable.
 */
class BuildSummaryUseCase @Inject constructor() {

    operator fun invoke(
        period: SummaryPeriod,
        goals: List<Goal>,
        tasks: List<Task>,
        windowStartMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProgressSummary {
        val activeGoals = goals.filterNot { it.isArchived }

        val tasksInWindow = tasks.filter {
            it.isDone && (it.completedAtEpochMillis ?: 0L) >= windowStartMillis
        }
        val completedCount = tasksInWindow.size
        // `Task.points` is derived since `#55` and, for a completed task, comes from what the
        // completion **banked** — so this total cannot be moved by re-estimating a task after
        // the fact. The person's own currency is still points; what left is points as a
        // property of a *goal*, below.
        val earnedPoints = tasksInWindow.sumOf { it.points.toLong() }

        // §1.4: "Points are never rendered as a property of an objective ... the goal
        // header's companion number becomes effort." So the per-goal slice carries MINUTES.
        // Summed over the task's edges to this goal rather than over `goalId`, because a task
        // may serve more than one objective (§1.5) -- and the minutes are NOT divided here:
        // §1.5's division rule governs the life-area pie, where the slices must sum to the
        // time that passed. This is a per-goal companion number, and "4h 20m of work logged
        // toward this" is true of the whole run for each goal it served.
        val minutesByGoal = HashMap<String, Int>()
        for (task in tasksInWindow) {
            val minutes = task.completion?.minutes ?: TaskDuration.minutesOf(task)
            for (edge in task.goalEdges) {
                minutesByGoal[edge.goalId] = (minutesByGoal[edge.goalId] ?: 0) + minutes
            }
        }

        val goalProgress = activeGoals.map { goal ->
            GoalProgress(
                goalId = goal.id,
                title = goal.title,
                category = goal.category,
                fraction = goal.progressFraction,
                effortMinutes = minutesByGoal[goal.id] ?: 0,
                // Carried, not filtered (`#66`). The slice keeps its effort --
                // §1.4 makes that a separate quantity and it is perfectly real for
                // an unmeasured goal -- and `ProgressSummary.averageProgress`
                // drops only the fraction, which is `currentValue` over a target
                // nobody set. That average is published by
                // `SocialRepositoryImpl.shareSummary`, so this is the flag that
                // stops a fiction leaving the device.
                isUnmeasured = goal.isUnmeasured,
            )
        }

        return ProgressSummary(
            period = period,
            totalPoints = earnedPoints,
            completedTasks = completedCount,
            activeGoals = activeGoals.size,
            goals = goalProgress,
            generatedAtEpochMillis = nowMillis,
        )
    }
}
