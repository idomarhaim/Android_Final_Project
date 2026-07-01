package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.TaskClassification

/**
 * LLM-backed analysis (spec §5 GROQ, §6 Core + Bonus). Calls are proxied through
 * a Firebase Cloud Function so the GROQ API key never ships in the app.
 */
interface RecommendationRepository {

    /** Recommendations + encouragement based on the user's goals and recent activity. */
    suspend fun getRecommendations(
        goals: List<Goal>,
        completedTasksLast7d: Int,
        totalPoints: Long,
    ): Resource<List<Recommendation>>

    /** Classify a free-text task title onto an existing goal or suggest a new one. */
    suspend fun classifyTask(taskTitle: String, goals: List<Goal>): Resource<TaskClassification>
}
