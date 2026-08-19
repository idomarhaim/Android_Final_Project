package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.TaskClassification
import com.idomarhaim.goalpilot.domain.model.TaskEstimate

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

    /**
     * Classify a free-text task title onto an existing goal or suggest a new one,
     * and estimate what it costs in points and minutes.
     *
     * [lifeAreas] are passed so a *new* goal can be filed under the right area of
     * the user's life straight away; without them every AI-created goal lands
     * unassigned and the time chart cannot see it.
     */
    suspend fun classifyTask(
        taskTitle: String,
        goals: List<Goal>,
        lifeAreas: List<LifeArea> = emptyList(),
    ): Resource<TaskClassification>

    /**
     * Estimates what a task is worth (5..50 points) and how long it takes
     * (spec §6 Core: "point scoring for tasks", extended with the duration the
     * time-allocation chart needs). One call returns both because GROQ's free tier
     * allows 30 requests/minute. Falls back to a local heuristic.
     */
    suspend fun scoreTask(taskTitle: String): Resource<TaskEstimate>
}
