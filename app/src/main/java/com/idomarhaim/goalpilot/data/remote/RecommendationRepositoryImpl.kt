package com.idomarhaim.goalpilot.data.remote

import com.google.firebase.functions.FirebaseFunctions
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.CloudFunctions
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCallEnvelope
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.RecommendationType
import com.idomarhaim.goalpilot.domain.model.TaskClassification
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskEstimate
import com.idomarhaim.goalpilot.domain.repository.AiProviderRepository
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
 *
 * ## `C13`'s ladder, and where each rung is implemented (#54, #32 §5)
 *
 * ```
 * user key  →  free model (GROQ, project key)  →  local fallback (spec §8)
 * ```
 *
 * Only the **third** rung is this class's. The first two are one callable away:
 * the credential travels in the payload (#32 §2) and the Cloud Function decides
 * between them, so there is exactly **one** copy of every prompt and every
 * `C11b` schema no matter whose key pays. That was §2's deciding argument —
 * mechanism count, not security — and it is why a bring-your-own key adds no
 * outbound path from this app to any model provider.
 *
 * ## One switch, all four AI features
 *
 * §4: key present means key used, in the recommendation feed, in classification
 * and in scoring — the three calls below — and in `C10`'s daily practical line.
 * Per-feature opt-in was rejected as four opinions to hold for no gain.
 *
 * ## Every call reports who answered
 *
 * [AiProviderRepository.recordAnswer] fires on all three paths including the
 * exception one, because a status line that only updates on success would go
 * stale exactly when it matters. The `catch` blocks record [AiAnswer.Local]:
 * nothing reached a provider, so nothing was learned about the key either.
 *
 * ⚠️ **Nothing here logs.** The payloads below carry the user's key from #54's
 * one unit that handles a secret; `catch (e: Exception)` deliberately swallows
 * without a `Log` call, and adding one would put a request object one
 * interpolation away from a third-party secret.
 */
@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val aiProvider: AiProviderRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RecommendationRepository {

    override suspend fun getRecommendations(
        goals: List<Goal>,
        completedTasksLast7d: Int,
        totalPoints: Long,
    ): Resource<List<Recommendation>> = withContext(io) {
        val credential = aiProvider.credential.value
        try {
            val payload = hashMapOf<String, Any>(
                "goals" to goals.map {
                    mapOf(
                        "id" to it.id,
                        "title" to it.title,
                        "category" to it.category.name,
                        "progressPercent" to it.progressPercent,
                        // The wire key stays `unit` — the Cloud Function reads
                        // it — but what travels is §1.3's *word*, which is the
                        // only half of a measure the prompt was ever using.
                        "unit" to it.measureWord,
                    )
                },
                "completedTasksLast7d" to completedTasksLast7d,
                "totalPoints" to totalPoints,
            ).withCredential(credential)
            val result = functions.getHttpsCallable(CloudFunctions.GET_RECOMMENDATIONS)
                .call(payload).await()
            val data = result.getData()
            aiProvider.recordAnswer(AiCallEnvelope.answeredBy(data, credential))
            val parsed = parseRecommendations(data)
            Resource.Success(parsed.ifEmpty { fallbackRecommendations(goals, completedTasksLast7d, totalPoints) })
        } catch (e: Exception) {
            aiProvider.recordAnswer(AiAnswer.Local())
            Resource.Success(fallbackRecommendations(goals, completedTasksLast7d, totalPoints))
        }
    }

    override suspend fun classifyTask(
        taskTitle: String,
        goals: List<Goal>,
        lifeAreas: List<LifeArea>,
    ): Resource<TaskClassification> = withContext(io) {
        val credential = aiProvider.credential.value
        try {
            val payload = hashMapOf<String, Any>(
                "taskTitle" to taskTitle,
                "goals" to goals.map {
                    mapOf(
                        "id" to it.id,
                        "title" to it.title,
                        "category" to it.category.name,
                        "lifeAreaIds" to it.lifeAreaIds,
                    )
                },
                "lifeAreas" to lifeAreas.map { mapOf("id" to it.id, "name" to it.name) },
            ).withCredential(credential)
            val result = functions.getHttpsCallable(CloudFunctions.CLASSIFY_TASK)
                .call(payload).await()
            val data = result.getData()
            aiProvider.recordAnswer(AiCallEnvelope.answeredBy(data, credential))
            val parsed = parseClassification(data)
            Resource.Success(parsed ?: fallbackClassification(taskTitle, goals, lifeAreas))
        } catch (e: Exception) {
            aiProvider.recordAnswer(AiAnswer.Local())
            Resource.Success(fallbackClassification(taskTitle, goals, lifeAreas))
        }
    }

    override suspend fun scoreTask(taskTitle: String): Resource<TaskEstimate> = withContext(io) {
        val credential = aiProvider.credential.value
        try {
            val payload = hashMapOf<String, Any>("taskTitle" to taskTitle)
                .withCredential(credential)
            val result = functions.getHttpsCallable(CloudFunctions.SCORE_TASK)
                .call(payload).await()
            aiProvider.recordAnswer(AiCallEnvelope.answeredBy(result.getData(), credential))
            val data = result.getData() as? Map<*, *>
            // `#55`, §3.3 A: **there is no `points` field**. The model names one of three
            // difficulties and the app computes the currency from it and the minutes
            // (§0.5). What used to stand here read `data["points"]` and fell back to
            // `heuristicPoints` — a score derived from the title's WORD COUNT, which the
            // duration was then derived back out of. Both are gone.
            //
            // An unparseable or absent difficulty is ROUTINE, which is ×1.0: the task is
            // priced on its minutes alone, which is what a task nobody judged is worth.
            val difficulty = Difficulty.fromName(data?.get("difficulty") as? String)
            // #9, spec §3.4: a model that answered about difficulty but not duration has
            // NOT answered about the duration, and the honest value is absent. The caller
            // asks instead, and a skipped answer is stored as DurationSource.UNKNOWN.
            val minutes = TaskDuration.sanitize((data?.get("minutes") as? Number)?.toInt())
            Resource.Success(TaskEstimate(difficulty = difficulty, minutes = minutes))
        } catch (e: Exception) {
            aiProvider.recordAnswer(AiAnswer.Local())
            // Nothing spoke, so nothing is reported: no difficulty judgement and no
            // duration. `#55` deleted the offline point heuristic outright, and there is
            // no offline substitute for it — a difficulty is a judgement about the work,
            // and the app has no way to make one. ROUTINE here is the ABSENCE of a
            // judgement (×1.0), not a guess at one, which is the same shape as `null`
            // minutes meaning "not said" rather than "zero".
            Resource.Success(TaskEstimate(difficulty = Difficulty.ROUTINE, minutes = null))
        }
    }

    /**
     * Adds #32 §2's `provider · model · key` to a payload, or **nothing at all**
     * when no key is set.
     *
     * Written as one extension used by all three calls rather than inlined
     * three times: §4 decided *one switch, all four AI features*, and three
     * hand-copied insertions is exactly how a fourth call site gets added
     * without one. A call with no credential produces a payload byte-identical
     * to the one this app sent before `C13`.
     */
    private fun HashMap<String, Any>.withCredential(
        credential: AiCredential?,
    ): HashMap<String, Any> = apply { putAll(AiCallEnvelope.credentialFields(credential)) }

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

    private fun parseClassification(data: Any?): TaskClassification? {
        val m = data as? Map<*, *> ?: return null
        return TaskClassification(
            suggestedGoalId = m["suggestedGoalId"] as? String,
            suggestedNewGoalTitle = m["suggestedNewGoalTitle"] as? String,
            suggestedCategory = GoalCategory.fromName(m["suggestedCategory"] as? String),
            // Read, not re-checked. Membership is the Cloud Function's job and ONLY the
            // Cloud Function's (#6, §3.4: "validation lives in the Cloud Function, singly")
            // — a second implementation here is a second answer to "is this id real?", free
            // to drift from the first the moment either is edited. What still happens on the
            // client is RESOLUTION, in `SmartFiling.decide`: an id that names nothing the
            // user has finds nothing and the task lands unfiled, which cannot disagree with
            // anything because it is a lookup rather than a rule.
            suggestedLifeAreaId = m["suggestedLifeAreaId"] as? String,
            // `#55`: `estimatedPoints` is deleted from the model's vocabulary (§3.3 A/D) and
            // replaced by the same three-word judgement `scoreTask` returns. Absent or
            // unparseable reads as ROUTINE, which is ×1.0.
            difficulty = Difficulty.fromName(m["difficulty"] as? String),
            // Absent when the model did not say — same rule as `scoreTask` above.
            estimatedMinutes = TaskDuration.sanitize((m["estimatedMinutes"] as? Number)?.toInt()),
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
                // keeps an offline classification out of "Unassigned". The first
                // of several, because the classification carries one suggestion
                // and a task the user then files is filed by hand anyway.
                suggestedLifeAreaId = match.lifeAreaIds.firstOrNull(),
                // No judgement offline (`#55`). ROUTINE is ×1.0 — the absence of one.
                difficulty = Difficulty.ROUTINE,
                estimatedMinutes = null,
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
                difficulty = Difficulty.ROUTINE,
                estimatedMinutes = null,
                confidence = 0.2f,
                rationale = "No matching goal found (offline heuristic).",
            )
        }
    }
}
