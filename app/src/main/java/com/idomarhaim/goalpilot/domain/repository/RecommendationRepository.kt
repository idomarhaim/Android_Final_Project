package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalStructure
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
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

    /**
     * §3.3 E's `measure` — ask for a concrete measure for goals that have none
     * (spec §1.3, §3.4; `C7` #14, `C22` #44, #65).
     *
     * ## What comes back has no number in it, and the caller must supply one
     *
     * §3.3 E: the model names a `targetSource` — *which arithmetic the app runs* —
     * and never a target. The returned [MeasureProposal]s therefore arrive with
     * their [MeasureProposal.target] **already computed** by this repository from
     * the [structures] passed in, because that is the only place both halves are
     * in hand at once. A caller that ignored [structures] would get proposals
     * with null targets, which is a legible state rather than a wrong one.
     *
     * ## Wide by contract, one goal per call today
     *
     * §3.2. The wire carries a list because batching must cost nothing to add,
     * but §1.3 puts the offer only on a goal's **own screen**, so the only caller
     * asks about the goal being opened. Load is unchanged either way: dismissal
     * is permanent, so a goal is proposed at most once ever.
     *
     * ## Failure is an empty list, never an exception
     *
     * §3.4's `measure` row falls back to the **mechanical proposal**, which is
     * arithmetic the caller can run offline
     * ([com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase]). An empty
     * list is both the honest report of a call that did not happen and exactly
     * the input that fallback wants, so there is nothing for a caller to catch.
     *
     * @param structures the schedule and sub-tree counts per goal id, keyed by id
     */
    suspend fun proposeMeasures(
        goals: List<Goal>,
        structures: Map<String, GoalStructure>,
    ): Resource<List<MeasureProposal>>
}
