package com.idomarhaim.goalpilot.domain.model

/**
 * A life goal the user is working toward (spec §1, §6 Core).
 *
 * Progress is tracked as a numeric [currentValue] moving toward [targetValue]
 * in a [unit] (e.g. "workouts", "hours", "books"). For simple habit-style goals
 * the target can be left at a nominal value and progress logged manually.
 */
data class Goal(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: GoalCategory = GoalCategory.OTHER,
    val targetValue: Double = 100.0,
    val currentValue: Double = 0.0,
    val unit: String = "%",
    val colorHex: String = category.defaultColorHex,
    val deadlineEpochMillis: Long? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
) {
    /** Progress in the range 0f..1f. */
    val progressFraction: Float
        get() = if (targetValue <= 0.0) 0f
        else (currentValue / targetValue).coerceIn(0.0, 1.0).toFloat()

    val isComplete: Boolean get() = progressFraction >= 1f

    val progressPercent: Int get() = (progressFraction * 100).toInt()
}

/**
 * The life areas a goal can belong to (spec §1). Each carries a default accent
 * color and an [iconKey] the UI maps to a Material icon, so the domain layer
 * stays free of any Compose/Android types.
 */
enum class GoalCategory(
    val label: String,
    val iconKey: String,
    val defaultColorHex: String,
) {
    HEALTH("Health", "favorite", "#EF6461"),
    FITNESS("Fitness", "fitness", "#0B6E4F"),
    SLEEP("Sleep", "sleep", "#5A67D8"),
    NUTRITION("Nutrition", "nutrition", "#38A169"),
    RELATIONSHIPS("Relationships", "people", "#D53F8C"),
    CAREER("Career", "work", "#2A9DF4"),
    PROJECTS("Projects", "project", "#DD6B20"),
    LEARNING("Learning", "school", "#805AD5"),
    FINANCE("Finance", "finance", "#2F855A"),
    OTHER("Other", "flag", "#718096");

    companion object {
        fun fromName(name: String?): GoalCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
}
