package com.idomarhaim.goalpilot.domain.model

/**
 * An LLM-generated recommendation or encouragement (spec §1 level 2, §6 Core:
 * "Recommendations and encouragement via GROQ"). Produced by the Cloud Function
 * proxy so the GROQ key never touches the client.
 */
data class Recommendation(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: RecommendationType = RecommendationType.ENCOURAGEMENT,
    val relatedGoalId: String? = null,
)

enum class RecommendationType {
    ENCOURAGEMENT,
    SUGGESTION,
    WARNING,
    INSIGHT;

    companion object {
        fun fromName(name: String?): RecommendationType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ENCOURAGEMENT
    }
}

/**
 * The LLM's proposed mapping of a free-text task onto a goal (spec §6 Bonus:
 * "Automatic classification of tasks into goals ... including a suggestion to
 * create a new goal").
 */
data class TaskClassification(
    val suggestedGoalId: String? = null,
    val suggestedNewGoalTitle: String? = null,
    val suggestedCategory: GoalCategory = GoalCategory.OTHER,
    /**
     * Which of the user's [LifeArea]s the task belongs to, when the model can tell.
     * Only ever an id the caller sent in — an unknown id is dropped rather than
     * filing real time under an area that does not exist.
     */
    val suggestedLifeAreaId: String? = null,
    /**
     * How demanding the model judged the work — §3.3 D's estimate group, `#55`.
     *
     * **It replaced `estimatedPoints`, and that is a change of kind, not of unit.** §3.3 A:
     * *"There is no `points` field, and there never will be."* A number is something the
     * model can move by phrasing; one of three prompt-declared words is not, and the app
     * turns it into a currency itself (§0.5, §1.4). `C11a` measured this model's free numbers
     * swinging 2× run-to-run and 1.8× between languages, which is why the whole spec keeps
     * the arithmetic on this side of the wire.
     *
     * [Difficulty.ROUTINE] is the default and is ×1.0, so a classification that said nothing
     * about difficulty prices the task on its minutes alone.
     */
    val difficulty: Difficulty = Difficulty.ROUTINE,
    /**
     * Minutes the task is expected to take — the input to the time-allocation
     * chart — or **null when the model did not say** (#9, spec §3.4).
     *
     * Nullable since #9. It defaulted to [TaskDuration.DEFAULT_MINUTES], which made
     * a silent failure indistinguishable from a thirty-minute answer at the one
     * place the difference is recorded forever: the task's stored duration.
     */
    val estimatedMinutes: Int? = null,
    val confidence: Float = 0f,
    val rationale: String = "",
)
