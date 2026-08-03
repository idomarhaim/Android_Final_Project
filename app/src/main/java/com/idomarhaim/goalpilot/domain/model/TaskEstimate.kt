package com.idomarhaim.goalpilot.domain.model

/**
 * What the LLM thinks a task is worth and how long it takes
 * (spec §6 Core "point scoring", extended with the duration the
 * time-allocation chart is built on).
 *
 * The two travel together because they come from one `scoreTask` call: GROQ's
 * free tier allows 30 requests/minute, and asking twice for facts about the same
 * sentence would halve the number of tasks the Google Tasks import can process.
 */
data class TaskEstimate(
    val points: Int = 10,
    val minutes: Int = TaskDuration.DEFAULT_MINUTES,
)

/**
 * How long a completed task counts for when slicing the user's time.
 *
 * Every completed task must contribute *something*, including the ones created
 * before durations existed and the ones added while offline — otherwise the pie
 * silently under-reports whole areas of the user's life and looks like a bug.
 * [minutesOf] is the single place that decides, so the chart, the analytics
 * summary and the tests all agree on the same number.
 */
object TaskDuration {

    /** Floor and ceiling for anything stored or estimated. */
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 480

    /** Used when nothing at all is known — a "half-hour chore". */
    const val DEFAULT_MINUTES = 30

    /**
     * Offline estimate from a task's point value. Points are scored 5..50 by
     * difficulty, so ×3 spans 15..150 minutes, which is the right shape for
     * personal tasks: deterministic, monotonic in difficulty, and never zero.
     */
    fun fallbackMinutes(points: Int): Int = (points * 3).coerceIn(MIN_MINUTES, MAX_MINUTES)

    /** The stored estimate when there is one, otherwise the point-based fallback. */
    fun minutesOf(task: Task): Int =
        task.estimatedMinutes?.takeIf { it > 0 }?.coerceAtMost(MAX_MINUTES)
            ?: fallbackMinutes(task.points)

    /** Clamps whatever the model returned into the range the UI can render. */
    fun sanitize(minutes: Int?): Int? =
        minutes?.takeIf { it > 0 }?.coerceIn(MIN_MINUTES, MAX_MINUTES)
}
