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

/**
 * The offline half of task scoring, in the domain rather than in the repository
 * that calls the model.
 *
 * It lives here because two callers need it for opposite reasons: the repository
 * uses it to *produce* an estimate when GROQ is unreachable, and the duration
 * back-fill uses it to *recognise* one. Every AI path in this app falls back
 * silently by design (spec §8), so a returned estimate carries no flag saying
 * which it was — [looksLikeFallback] reconstructs the answers the fallbacks would
 * have given and compares. Two copies of a formula would make that comparison
 * quietly meaningless the first time either drifted.
 */
object TaskScoring {

    /** Matches the 5..50 range the `scoreTask` Cloud Function is prompted for. */
    const val MIN_POINTS = 5
    const val MAX_POINTS = 50

    /**
     * Offline point estimate: a longer, more specific task title generally
     * describes more work. Deterministic so the UI never jumps around.
     */
    fun heuristicPoints(taskTitle: String): Int {
        val words = taskTitle.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return (MIN_POINTS + words * 3).coerceIn(MIN_POINTS, MAX_POINTS)
    }

    /**
     * True when [estimate] is one of the two answers that mean *no model spoke*.
     *
     * There are two, and missing the second is an easy mistake:
     *
     * 1. **The client's**, when the call never left the device — `5 + 3×words`
     *    points and three minutes per point, produced by
     *    [heuristicPoints] + [TaskDuration.fallbackMinutes].
     * 2. **The Cloud Function's**, when the call arrived but GROQ did not answer —
     *    a flat `10 points / 30 minutes` from the `catch` in
     *    `functions/src/index.ts`. It is *not* reachable by the client heuristic
     *    (`5 + 3w` is never 10), so a check that only knew rule 1 would wave the
     *    server's failure through as a genuine estimate.
     *
     * **Evidence, not proof**: a model is free to land on the same two numbers by
     * agreement rather than by failure, and this cannot tell the two apart. It is
     * used only to keep such an estimate from being *written* as an AI estimate —
     * the cost of a false positive is one task the user can still tick by hand,
     * and the cost of a false negative is the analytics card claiming a duration
     * came from the model when it came from a `catch` block.
     */
    fun looksLikeFallback(taskTitle: String, estimate: TaskEstimate): Boolean {
        val clientHeuristic = estimate.points == heuristicPoints(taskTitle) &&
            estimate.minutes == TaskDuration.fallbackMinutes(estimate.points)
        return clientHeuristic || estimate == SERVER_FALLBACK
    }

    /**
     * What `scoreTask` returns when the function ran but GROQ did not answer.
     * Mirrors the `catch` in `functions/src/index.ts`; if that changes, this does.
     */
    private val SERVER_FALLBACK = TaskEstimate(points = 10, minutes = TaskDuration.DEFAULT_MINUTES)
}
