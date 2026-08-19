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
     * The user-defined [LifeArea]s this goal serves. **Unfiled is the empty
     * collection** — never a made-up default, because that would put real time
     * into the wrong slice of the time-allocation chart; "Unassigned" is an
     * honest answer and one the user can act on.
     *
     * Plural since `PRODUCT_v0.3` §1.2: *"a goal reaches many areas"*, so one
     * goal legitimately appears under several. The empty list is a direct
     * translation of the old nullable `lifeAreaId`, which preserves the rule that
     * deleting an area must not silently rewrite the past — see
     * `GoalDto.lifeAreaId` for the read-side backfill.
     */
    val lifeAreaIds: List<String> = emptyList(),
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
    /**
     * How far the goal has come — **derived, never stored** (spec §5.2, #49).
     *
     * It is the sum of the goal's progress entries and the `progressContribution`
     * of its completed tasks, computed by
     * [DerivedProgress][com.idomarhaim.goalpilot.domain.model.DerivedProgress] and put
     * here by `withDerivedProgress` at the repository boundary. The field survives
     * as a **view** so every screen can keep reading it; what changed is that
     * nothing writes it to Firestore.
     *
     * It used to be a stored counter advanced by two client writers, one of which
     * did it in a second, non-atomic step after writing the entry it was crediting
     * — so a crash in between left the goal reading low forever. There is no
     * counter to fall out of step with the facts any more.
     *
     * A `Goal` built by hand (a test, a form in progress) carries `0.0` and that is
     * honest: nothing has been logged against it.
     */
    val currentValue: Double = 0.0,
    val unit: String = "%",
    val colorHex: String = category.defaultColorHex,
    val deadlineEpochMillis: Long? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
) {
    /**
     * Progress as a fraction of the target. `0f` when there is no target to
     * measure against, because a fraction of nothing means nothing.
     *
     * **Not clamped, and that is the point** (spec §1.5, #49). It was pinned to
     * `0f..1f`, which made two real states unsayable: a goal you have *beaten*
     * read as merely finished, and a goal whose progress had legitimately fallen —
     * a correcting entry, an unticked task — read as untouched. Past the target the
     * app stops speaking in percent and says how far you beat it by; below zero it
     * says so. Callers that draw a bar clamp at the drawing, where the constraint
     * actually is, and callers that state a number state the real one.
     */
    val progressFraction: Float
        get() = if (targetValue <= 0.0) 0f
        else (currentValue / targetValue).toFloat()

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
    /**
     * ⚠️ **Superseded for display — use `GoalCategory.localizedLabel()` in
     * `ui/components/ComponentStrings.kt` instead** (issue #51).
     *
     * This is a constructor argument, and *a language switch cannot reach a
     * constructor argument* (`kb/dev/untranslatable-idioms.md` §1), so anything
     * rendering it stays English on a Hebrew device. The Hebrew for all ten
     * categories is authored once in `res/values-iw/components_strings.xml`.
     *
     * Still declared because three call sites in `feature/dashboard` and
     * `feature/goals` read it and **those packages are unswept** — deleting it
     * would drag them into #51's `ui/components/` unit half-done. Each switches
     * to `localizedLabel()` when its own sweep lands, and this property goes
     * with the last of them.
     */
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
