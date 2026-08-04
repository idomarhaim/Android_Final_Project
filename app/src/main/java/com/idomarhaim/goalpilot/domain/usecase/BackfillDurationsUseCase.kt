package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskEstimate
import com.idomarhaim.goalpilot.domain.model.TaskScoring
import javax.inject.Inject

/**
 * A task the time chart is currently guessing about: no stored duration, so
 * [TaskDuration.minutesOf] is inferring one from its difficulty.
 *
 * [inWindow] marks the ones completed inside the range on screen — the only ones
 * whose re-estimation moves the "x of y durations estimated by AI" line the user
 * is looking at, which is why they are asked about first.
 */
data class DurationCandidate(
    val taskId: String,
    val title: String,
    /** What the chart uses today: the point-based fallback. */
    val inferredMinutes: Int,
    val inWindow: Boolean,
)

/**
 * One candidate plus what the model said, awaiting the user's confirmation.
 *
 * [isFallback] means the estimate is indistinguishable from one of the two
 * silent fallbacks (see [TaskScoring.looksLikeFallback]) — no model answered.
 * Such a row arrives **unselected**: writing it would increment the count of
 * durations the analytics card attributes to the AI, which is the one number this
 * whole feature exists to make honest.
 */
data class DurationProposal(
    val taskId: String,
    val title: String,
    val inferredMinutes: Int,
    val proposedMinutes: Int,
    val isFallback: Boolean,
    val inWindow: Boolean,
    val selected: Boolean,
) {
    /** True when confirming would actually move the task's slice of the pie. */
    val changesTheChart: Boolean get() = proposedMinutes != inferredMinutes
}

/**
 * Picks which tasks to re-estimate, and turns each answer into a reviewable
 * proposal (spec §6 Bonus: the durations behind the time-allocation chart).
 *
 * Pure — the fan-out of `scoreTask` calls and the Firestore writes belong to the
 * ViewModel; what is decided here is *which* tasks and *whether the answer is
 * worth writing*, which is exactly the part worth testing on the JVM.
 */
class BackfillDurationsUseCase @Inject constructor() {

    /**
     * Tasks with no stored duration, best candidates first, capped at [limit].
     *
     * Order is deliberate: tasks completed inside the window on screen come
     * first, then other completed tasks, then tasks still open — and inside each
     * group, most recent first. Under a cap, order *is* the feature: the run has
     * to spend its budget on the tasks whose durations the user can see being
     * wrong.
     */
    operator fun invoke(
        tasks: List<Task>,
        window: TimeWindow,
        limit: Int = MAX_PER_RUN,
    ): List<DurationCandidate> = tasks
        .filter { it.id.isNotBlank() && it.title.isNotBlank() }
        .filter { (it.estimatedMinutes ?: 0) <= 0 }
        .sortedWith(
            compareByDescending<Task> { it.isInWindow(window) }
                .thenByDescending { it.isDone }
                .thenByDescending { it.completedAtEpochMillis ?: it.createdAtEpochMillis },
        )
        .take(limit.coerceAtLeast(0))
        .map { task ->
            DurationCandidate(
                taskId = task.id,
                title = task.title,
                inferredMinutes = TaskDuration.minutesOf(task),
                inWindow = task.isInWindow(window),
            )
        }

    private fun Task.isInWindow(window: TimeWindow): Boolean {
        val completedAt = completedAtEpochMillis ?: return false
        return isDone && window.contains(completedAt)
    }

    /**
     * The model's answer, sanitised and judged.
     *
     * Only the minutes are ever taken. `scoreTask` also returns points, and
     * rewriting those would be a bug with a long fuse: completing a task already
     * awarded [Task.points] to the user and to the public leaderboard projection,
     * so a task whose points changed afterwards refunds a different number than it
     * paid when it is un-ticked.
     */
    fun propose(candidate: DurationCandidate, estimate: TaskEstimate?): DurationProposal {
        val minutes = TaskDuration.sanitize(estimate?.minutes)
        val isFallback = estimate == null ||
            minutes == null ||
            TaskScoring.looksLikeFallback(candidate.title, estimate)
        return DurationProposal(
            taskId = candidate.taskId,
            title = candidate.title,
            inferredMinutes = candidate.inferredMinutes,
            proposedMinutes = minutes ?: candidate.inferredMinutes,
            isFallback = isFallback,
            inWindow = candidate.inWindow,
            selected = !isFallback,
        )
    }

    companion object {
        /**
         * Cap on tasks re-estimated per run. One `scoreTask` call each against
         * GROQ's free tier, which allows 30 requests/minute — the same reasoning,
         * and deliberately the same number, as `DashboardViewModel.MAX_IMPORT`.
         */
        const val MAX_PER_RUN = 15
    }
}
