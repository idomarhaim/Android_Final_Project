package com.idomarhaim.goalpilot.domain.model

/**
 * A task associated with a goal (spec §6 Core: "Associate tasks with goals").
 * Completing a task awards [points] and advances the linked goal's progress.
 */
data class Task(
    val id: String = "",
    val goalId: String? = null,
    val title: String = "",
    val points: Int = 10,
    val isDone: Boolean = false,
    val source: TaskSource = TaskSource.MANUAL,
    /** How much this task advances its goal's [Goal.currentValue] when completed. */
    val progressContribution: Double = 1.0,
    /**
     * How long the task takes, in minutes — typed by the user, or estimated by the
     * LLM when the task is classified or scored. The raw material of the
     * time-allocation chart.
     *
     * Null means "no duration at all"; [TaskDuration.minutesOf] supplies the chart's
     * fallback rather than dropping the task from it. [durationSource] says which of
     * the two wrote it, and is the only thing that may be read to find out.
     */
    val estimatedMinutes: Int? = null,
    /**
     * Where [estimatedMinutes] came from (#9, spec §1.4).
     *
     * [DurationSource.USER] is **sticky**: no re-estimation may overwrite it,
     * unconditionally and with no threshold. Enforced structurally rather than by a
     * check on what comes back — such a task is never *sent* for re-estimation
     * (§3.3 A: *"those tasks are not in `tasks[]` at all"*), which is why
     * `BackfillDurationsUseCase` filters on this field rather than comparing numbers
     * afterwards.
     *
     * Every task written before #9 reads as [DurationSource.UNKNOWN], and that is
     * the honest value rather than a convenient one: no code path let a person type
     * a duration before this ticket, so no stored value can be a typed one, and
     * `UNKNOWN` is therefore safe to re-estimate.
     */
    val durationSource: DurationSource = DurationSource.UNKNOWN,
    val createdAtEpochMillis: Long = 0L,
    /**
     * When the completion happened — **one fact with [isDone], never one without the
     * other** ([TaskCompletion]).
     *
     * Four readers disagree about which of the two fields *is* the fact (the projection
     * function counts `done`; the weekly summary, the dashboard's done-this-week count and
     * the time-allocation chart all require this stamp), so a task carrying one and not the
     * other awards points while being invisible everywhere it could be checked against.
     * `TaskRepositoryImpl.upsertTask` runs `TaskCompletion.stamp` over every write for that
     * reason; `setDone` has always written both together.
     */
    val completedAtEpochMillis: Long? = null,
)

/** Where a task originated (spec §6 nice-to-have: Google Tasks import). */
enum class TaskSource {
    MANUAL,
    GOOGLE_TASKS;

    companion object {
        fun fromName(name: String?): TaskSource =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MANUAL
    }
}
