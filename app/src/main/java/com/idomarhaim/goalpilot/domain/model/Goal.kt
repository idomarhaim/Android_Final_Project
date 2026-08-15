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
    /**
     * The user-defined [LifeArea] this goal belongs to, or null while it is
     * unfiled. Nullable rather than defaulted because a made-up default would put
     * real time into the wrong slice of the time-allocation chart; "Unassigned"
     * is an honest answer and one the user can act on.
     */
    val lifeAreaId: String? = null,
    /**
     * The automatic data source this goal belongs to — `"hc:goal:steps"` for the
     * Health Connect step goal — or null for a goal nobody syncs into.
     *
     * It exists because the sync has to answer *"does a goal for this metric
     * already exist?"* on every foreground with no human watching, and it used to
     * answer by matching [category]. The category is a **chip the user can edit**,
     * so one edit silently orphaned the goal and the next sync created a duplicate
     * (#47). An identity the user cannot reach is the only safe key for that
     * question; a display attribute is not an identity.
     *
     * Set when the sync creates a goal, and stamped onto a goal it matched by the
     * older heuristic, so the pinning is one-way and happens at most once.
     */
    val healthSourceKey: String? = null,
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
 *
 * The colours are a **categorical** palette: ten hues spread around the wheel so
 * no two categories can be confused when they sit next to each other as bars in
 * the analytics chart. The previous set had three greens (Fitness, Nutrition,
 * Finance) that were indistinguishable at bar width. Every value clears 4.5:1
 * against a light surface — they are used as text, not just fills — and
 * `String.toGoalAccent()` lifts them for dark surfaces.
 *
 * [Goal.colorHex] is persisted per goal, so changing a default here does not
 * rewrite existing documents — an untouched goal keeps the colour it was created
 * with. Note that `AddEditGoalViewModel.save()` re-derives `colorHex` from the
 * category on *every* save, so an old goal picks the new default up the next time
 * it is edited.
 */
enum class GoalCategory(
    val label: String,
    val iconKey: String,
    val defaultColorHex: String,
) {
    HEALTH("Health", "favorite", "#CF3636"),
    FITNESS("Fitness", "fitness", "#B85107"),
    SLEEP("Sleep", "sleep", "#3B3BA8"),
    NUTRITION("Nutrition", "nutrition", "#26804A"),
    RELATIONSHIPS("Relationships", "people", "#D6246E"),
    CAREER("Career", "work", "#0F6FCB"),
    PROJECTS("Projects", "project", "#7C4A21"),
    LEARNING("Learning", "school", "#8B39C4"),
    FINANCE("Finance", "finance", "#0B7285"),
    OTHER("Other", "flag", "#64748B");

    companion object {
        fun fromName(name: String?): GoalCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
}
