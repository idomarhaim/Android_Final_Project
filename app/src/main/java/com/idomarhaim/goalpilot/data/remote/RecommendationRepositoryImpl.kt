package com.idomarhaim.goalpilot.data.remote

import com.google.firebase.functions.FirebaseFunctions
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.CloudFunctions
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.RecommendationType
import com.idomarhaim.goalpilot.domain.model.TaskClassification
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskEstimate
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the GROQ proxy Cloud Functions (spec §5, §6). If the network/LLM call
 * fails or returns nothing usable, it falls back to deterministic, locally
 * generated guidance so the feature never blocks or crashes (spec §8).
 */
@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RecommendationRepository {

    override suspend fun getRecommendations(
        goals: List<Goal>,
        completedTasksLast7d: Int,
        totalPoints: Long,
    ): Resource<List<Recommendation>> = withContext(io) {
        try {
            val payload = hashMapOf(
                "goals" to goals.map {
                    mapOf(
                        "id" to it.id,
                        "title" to it.title,
                        "category" to it.category.name,
                        "progressPercent" to it.progressPercent,
                        "unit" to it.unit,
                    )
                },
                "completedTasksLast7d" to completedTasksLast7d,
                "totalPoints" to totalPoints,
            )
            val result = functions.getHttpsCallable(CloudFunctions.GET_RECOMMENDATIONS)
                .call(payload).await()
            val parsed = parseRecommendations(result.getData())
            Resource.Success(parsed.ifEmpty { fallbackRecommendations(goals, completedTasksLast7d, totalPoints) })
        } catch (e: Exception) {
            Resource.Success(fallbackRecommendations(goals, completedTasksLast7d, totalPoints))
        }
    }

    override suspend fun classifyTask(
        taskTitle: String,
        goals: List<Goal>,
        lifeAreas: List<LifeArea>,
    ): Resource<TaskClassification> = withContext(io) {
        try {
            val payload = hashMapOf(
                "taskTitle" to taskTitle,
                "goals" to goals.map {
                    mapOf(
                        "id" to it.id,
                        "title" to it.title,
                        "category" to it.category.name,
                        "lifeAreaId" to it.lifeAreaId,
                    )
                },
                "lifeAreas" to lifeAreas.map { mapOf("id" to it.id, "name" to it.name) },
            )
            val result = functions.getHttpsCallable(CloudFunctions.CLASSIFY_TASK)
                .call(payload).await()
            val parsed = parseClassification(result.getData(), lifeAreas)
            Resource.Success(parsed ?: fallbackClassification(taskTitle, goals, lifeAreas))
        } catch (e: Exception) {
            Resource.Success(fallbackClassification(taskTitle, goals, lifeAreas))
        }
    }

    override suspend fun scoreTask(taskTitle: String): Resource<TaskEstimate> = withContext(io) {
        try {
            val result = functions.getHttpsCallable(CloudFunctions.SCORE_TASK)
                .call(hashMapOf("taskTitle" to taskTitle)).await()
            val data = result.getData() as? Map<*, *>
            val points = (data?.get("points") as? Number)?.toInt()?.coerceIn(MIN_POINTS, MAX_POINTS)
                ?: fallbackPoints(taskTitle)
            // A model that answers with points but no duration is common enough
            // that it must not cost the task its slice of the chart.
            val minutes = TaskDuration.sanitize((data?.get("minutes") as? Number)?.toInt())
                ?: TaskDuration.fallbackMinutes(points)
            Resource.Success(TaskEstimate(points = points, minutes = minutes))
        } catch (e: Exception) {
            val points = fallbackPoints(taskTitle)
            Resource.Success(
                TaskEstimate(points = points, minutes = TaskDuration.fallbackMinutes(points)),
            )
        }
    }

    // ── Parsing ────────────────────────────────────────────────────
    private fun parseRecommendations(data: Any?): List<Recommendation> {
        val root = data as? Map<*, *> ?: return emptyList()
        val list = root["recommendations"] as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            Recommendation(
                id = (m["id"] as? String) ?: UUID.randomUUID().toString(),
                title = (m["title"] as? String).orEmpty(),
                message = (m["message"] as? String).orEmpty(),
                type = RecommendationType.fromName(m["type"] as? String),
                relatedGoalId = m["relatedGoalId"] as? String,
            )
        }.filter { it.message.isNotBlank() }
    }

    private fun parseClassification(data: Any?, lifeAreas: List<LifeArea>): TaskClassification? {
        val m = data as? Map<*, *> ?: return null
        val points = (m["estimatedPoints"] as? Number)?.toInt() ?: 10
        return TaskClassification(
            suggestedGoalId = m["suggestedGoalId"] as? String,
            suggestedNewGoalTitle = m["suggestedNewGoalTitle"] as? String,
            suggestedCategory = GoalCategory.fromName(m["suggestedCategory"] as? String),
            // Models invent ids. Only an area the caller actually sent survives —
            // an invented one would file real minutes under nothing.
            suggestedLifeAreaId = (m["suggestedLifeAreaId"] as? String)
                ?.takeIf { id -> lifeAreas.any { it.id == id } },
            estimatedPoints = points,
            estimatedMinutes = TaskDuration.sanitize((m["estimatedMinutes"] as? Number)?.toInt())
                ?: TaskDuration.fallbackMinutes(points),
            confidence = (m["confidence"] as? Number)?.toFloat() ?: 0f,
            rationale = (m["rationale"] as? String).orEmpty(),
        )
    }

    // ── Deterministic fallbacks (spec §8) ─────────────────────────
    private fun fallbackRecommendations(
        goals: List<Goal>,
        completed: Int,
        points: Long,
    ): List<Recommendation> {
        if (goals.isEmpty()) {
            return listOf(
                Recommendation(
                    id = "fallback-start",
                    title = "Start with one goal",
                    message = "Add your first goal — health, fitness, career, anything. " +
                        "Small, specific goals are easiest to keep.",
                    type = RecommendationType.SUGGESTION,
                ),
            )
        }
        val recs = mutableListOf<Recommendation>()
        recs += Recommendation(
            id = "fallback-encourage",
            title = "Keep the streak alive",
            message = if (completed > 0) {
                "You completed $completed task(s) recently and earned $points points. " +
                    "Great momentum — keep going!"
            } else {
                "You have ${goals.size} active goal(s). Complete one small task today to get moving."
            },
            type = RecommendationType.ENCOURAGEMENT,
        )
        goals.filter { it.progressFraction < 0.34f }
            .take(2)
            .forEach { g ->
                recs += Recommendation(
                    id = "fallback-${g.id}",
                    title = "Nudge: ${g.title}",
                    message = "\"${g.title}\" is at ${g.progressPercent}%. " +
                        "Break it into a tiny next step you can do today.",
                    type = RecommendationType.SUGGESTION,
                    relatedGoalId = g.id,
                )
            }
        return recs
    }

    /**
     * Offline point estimate: a longer, more specific task title generally
     * describes more work. Deterministic so the UI never jumps around.
     */
    private fun fallbackPoints(taskTitle: String): Int {
        val words = taskTitle.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return (5 + words * 3).coerceIn(MIN_POINTS, MAX_POINTS)
    }

    private fun fallbackClassification(
        taskTitle: String,
        goals: List<Goal>,
        lifeAreas: List<LifeArea>,
    ): TaskClassification {
        val words = taskTitle.split(" ").filter { it.length > 3 }
        val match = goals.firstOrNull { g -> words.any { g.title.contains(it, ignoreCase = true) } }
        return if (match != null) {
            TaskClassification(
                suggestedGoalId = match.id,
                suggestedCategory = match.category,
                // The goal already knows where it belongs; inheriting its area
                // keeps an offline classification out of "Unassigned".
                suggestedLifeAreaId = match.lifeAreaId,
                estimatedPoints = 10,
                estimatedMinutes = TaskDuration.fallbackMinutes(10),
                confidence = 0.4f,
                rationale = "Matched by keyword to \"${match.title}\" (offline heuristic).",
            )
        } else {
            val areaMatch = lifeAreas.firstOrNull { area ->
                words.any { area.name.contains(it, ignoreCase = true) }
            }
            TaskClassification(
                suggestedNewGoalTitle = taskTitle.take(40),
                suggestedCategory = GoalCategory.OTHER,
                suggestedLifeAreaId = areaMatch?.id,
                estimatedPoints = 10,
                estimatedMinutes = TaskDuration.fallbackMinutes(10),
                confidence = 0.2f,
                rationale = "No matching goal found (offline heuristic).",
            )
        }
    }

    private companion object {
        /** Matches the 5..50 range the `scoreTask` Cloud Function is prompted for. */
        const val MIN_POINTS = 5
        const val MAX_POINTS = 50
    }
}
