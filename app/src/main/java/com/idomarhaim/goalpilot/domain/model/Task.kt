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
     * How long the task takes, in minutes — estimated by the LLM when the task is
     * classified or scored, and the raw material of the time-allocation chart.
     *
     * Null means "never estimated" (hand-typed task, or the model was unreachable
     * and nothing was written); [TaskDuration.minutesOf] supplies the fallback
     * rather than dropping the task from the chart.
     */
    val estimatedMinutes: Int? = null,
    val createdAtEpochMillis: Long = 0L,
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
