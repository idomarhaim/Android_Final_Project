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
    val estimatedPoints: Int = 10,
    /** Minutes the task is expected to take — the input to the time-allocation chart. */
    val estimatedMinutes: Int = TaskDuration.DEFAULT_MINUTES,
    val confidence: Float = 0f,
    val rationale: String = "",
)
