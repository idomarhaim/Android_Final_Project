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
import com.idomarhaim.goalpilot.domain.model.GoalFiling
import com.idomarhaim.goalpilot.domain.model.GoalPlan
import com.idomarhaim.goalpilot.domain.model.GoalStructure
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.MeasureBasis
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
import com.idomarhaim.goalpilot.domain.model.PlanStep
import com.idomarhaim.goalpilot.domain.model.PlanStepKind
import com.idomarhaim.goalpilot.domain.model.ProposalOrigin
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.RecommendationType
import com.idomarhaim.goalpilot.domain.model.TaskClassification
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TargetSource
import com.idomarhaim.goalpilot.domain.model.TaskEstimate
import com.idomarhaim.goalpilot.domain.repository.AiProviderRepository
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import java.time.LocalTime
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
    /**
     * Read for exactly one field, and only by `proposeMeasures`: §3.3 E's `word`
     * is **content**, authored once in the language that was current when it was
     * proposed and never re-rendered afterwards (§3.5, `C15b`). The model cannot
     * be left to infer that from a goal title, which may be in either language —
     * or in neither, as `Read Clean Architecture` is on a Hebrew device.
     *
     * Not read by the other three calls on purpose: their outputs are **speech**,
     * which the app already renders in the current language at display time.
     */
    private val preferences: AppPreferencesRepository,
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
                    buildMap<String, Any> {
                        put("id", it.id)
                        put("title", it.title)
                        put("category", it.category.name)
                        // ABSENT, not zero, for a goal with no measure (`#66`).
                        // `functions/src/index.ts` puts this map into the prompt
                        // with a bare `JSON.stringify`, so a key that is present
                        // is a fact the model is told — and `progressPercent` on
                        // an unmeasured goal is `currentValue` over
                        // `targetValue`'s 100.0 default, a target nobody set.
                        // Sending `0` there does not merely mislead: it reads as
                        // *this goal has gone nowhere*, which is the sentence the
                        // model then writes back in speech no fallback touches.
                        // Omitting the key says nothing at all, which is true.
                        if (!it.isUnmeasured) put("progressPercent", it.progressPercent)
                        // The wire key stays `unit` — the Cloud Function reads
                        // it — but what travels is §1.3's *word*, which is the
                        // only half of a measure the prompt was ever using.
                        put("unit", it.measureWord)
                    }
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
     * §3.3 E's `measure` call, and the arithmetic §3.3 E forbids the model doing.
     *
     * ## The shape of this method is the feature's design
     *
     * Everything before `parseProposals` is an ordinary callable invocation like the
     * three above it. Everything after is §0.5 — *the AI judges, the app computes* —
     * and it is why there is no `fallbackProposals` beside the other three fallbacks
     * in this file: §3.4's fallback for `measure` is
     * [ProposeMeasureUseCase.mechanical], which is **arithmetic over structure the
     * caller already holds** and has nothing to do with a network call. Putting a
     * copy of it here would be the second implementation `classifyTask`'s catch
     * block already carries a paragraph explaining the cost of.
     *
     * So the failure path returns an **empty list**, not a manufactured proposal.
     * The caller reads empty and runs the mechanical path itself, offline, with no
     * way to mistake one for the other.
     *
     * ⚠️ **Nothing here logs**, like the three calls above: the payload carries the
     * user's provider key.
     */
    override suspend fun proposeMeasures(
        goals: List<Goal>,
        structures: Map<String, GoalStructure>,
    ): Resource<List<MeasureProposal>> = withContext(io) {
        if (goals.isEmpty()) return@withContext Resource.Success(emptyList())
        val credential = aiProvider.credential.value
        try {
            val payload = hashMapOf<String, Any>(
                // The language the WORD is authored in. It is content, so it is written
                // once in whatever language is current and never re-rendered (§3.5,
                // `C15b`) — which is exactly why it has to travel: the model cannot be
                // left to infer it from a goal title that may be in either.
                "language" to preferences.language.value.id,
                "goals" to goals.map { goal ->
                    val structure = structures[goal.id] ?: GoalStructure()
                    mapOf(
                        "id" to goal.id,
                        "title" to goal.title,
                        // §3.3 E's two optional structure hints. They are what the model
                        // reads to choose a `targetSource`; it never sees the resulting
                        // number, and neither does this payload.
                        "occurrencesPerWeek" to structure.occurrencesPerWeek,
                        "openStepCount" to structure.openStepCount,
                    )
                },
            ).withCredential(credential)
            val result = functions.getHttpsCallable(CloudFunctions.PROPOSE_MEASURE)
                .call(payload).await()
            val data = result.getData()
            aiProvider.recordAnswer(AiCallEnvelope.answeredBy(data, credential))
            Resource.Success(parseProposals(data, structures))
        } catch (e: Exception) {
            aiProvider.recordAnswer(AiAnswer.Local())
            // Empty, never a substitute. §3.4's mechanical proposal is the caller's.
            Resource.Success(emptyList())
        }
    }

    /**
     * `fileGoal` — where a goal belongs, from its title alone (§3.3 D's schema, §0.7).
     *
     * ⚠️ **There is no `fallbackGoalFiling` beside the three fallbacks below, and that is a
     * decision.** The obvious offline heuristic — match the goal's words against the life-area
     * names — is a worse copy of the exact judgement being asked for, and it would be
     * indistinguishable downstream from a real answer, which is the cost
     * `classifyTask`'s catch block already spends a paragraph on. §1.2 makes *unfiled* a
     * legitimate state, so an empty [GoalFiling] is both the honest report of a call that did
     * not happen and a state the app already knows how to be in.
     *
     * ⚠️ **Nothing here logs**, like every call above: the payload carries the user's key.
     */
    override suspend fun fileGoal(
        goalTitle: String,
        lifeAreas: List<LifeArea>,
    ): Resource<GoalFiling> = withContext(io) {
        if (goalTitle.isBlank() || lifeAreas.isEmpty()) {
            return@withContext Resource.Success(GoalFiling())
        }
        val credential = aiProvider.credential.value
        try {
            val payload = hashMapOf<String, Any>(
                "language" to preferences.language.value.id,
                "goalTitle" to goalTitle,
                "lifeAreas" to lifeAreas.map { mapOf("id" to it.id, "name" to it.name) },
                // Deliberately NOT sent, and the omission is the design: with no `goals[]` the
                // Cloud Function's `listsFromRequest` yields an empty goal-id list, so every
                // `suggestedGoalId` fails membership and the field cannot appear. Reusing
                // `classify`'s validator is therefore safe by construction rather than by a
                // promise about what this payload contains.
            ).withCredential(credential)
            val result = functions.getHttpsCallable(CloudFunctions.FILE_GOAL)
                .call(payload).await()
            val data = result.getData()
            aiProvider.recordAnswer(AiCallEnvelope.answeredBy(data, credential))
            Resource.Success(parseFiling(data))
        } catch (e: Exception) {
            aiProvider.recordAnswer(AiAnswer.Local())
            Resource.Success(GoalFiling())
        }
    }

    /**
     * `planGoal` — §3.3 B's `plan`, and §3.7's proposed draft (`C8` #24).
     *
     * ## The one call in this file whose failure is an [Resource.Error]
     *
     * The other four all succeed with something: a local nudge feed, a keyword classification,
     * a neutral estimate, an empty proposal list. Each has a real offline answer to the question
     * it asks. This one does not — there is no arithmetic over the user's data that produces a
     * plan for *"run a marathon"* — and the user pressed a button for it. §0.4: *speak about a
     * failure the user can act on*, and pressing it again is an action. So a dead network here
     * says so, rather than handing back an empty plan that reads as *the model had nothing to
     * suggest*.
     *
     * ⚠️ **Nothing here logs**, for the same reason as every call above.
     */
    override suspend fun planGoal(goal: Goal): Resource<GoalPlan> = withContext(io) {
        val credential = aiProvider.credential.value
        try {
            val payload = hashMapOf<String, Any>(
                // The language every step TITLE is authored in. Content, like `measure`'s
                // `word`: written once in whatever language is current and never re-rendered
                // (§3.5, `C15b`). It has to travel for the same reason — the model cannot infer
                // it from a goal title that may be in either language, or in neither.
                "language" to preferences.language.value.id,
                "goalTitle" to goal.title,
                "goalDescription" to goal.description,
                // §1.3's word, when the goal has one. It is what the plan should add up to, and
                // it is the only part of the measure the prompt can use: the TARGET is a number
                // and §0.5 keeps numbers on this side of the wire.
                "measureWord" to goal.measureWord,
            ).withCredential(credential)
            val result = functions.getHttpsCallable(CloudFunctions.PLAN_GOAL)
                .call(payload).await()
            val data = result.getData()
            aiProvider.recordAnswer(AiCallEnvelope.answeredBy(data, credential))
            Resource.Success(parsePlan(data, goal.id))
        } catch (e: Exception) {
            aiProvider.recordAnswer(AiAnswer.Local())
            Resource.Error(e.message ?: "The plan could not be fetched.")
        }
    }

    /**
     * Reads a `fileGoal` response.
     *
     * **`category` is read nullably**, unlike everywhere else in this file. `GoalCategory.fromName`
     * resolves an unknown name to `OTHER`, which is right for a *stored* value and wrong here: it
     * would let a call that said nothing about the category overwrite one the user picked by hand
     * with `OTHER` — a silent downgrade wearing an answer's clothes. Absent stays absent, and the
     * caller keeps what it had.
     */
    private fun parseFiling(data: Any?): GoalFiling {
        val m = data as? Map<*, *> ?: return GoalFiling()
        val categoryName = m["suggestedCategory"] as? String
        return GoalFiling(
            // Read, not re-checked — membership is the Cloud Function's job and only its
            // (§3.4). What happens on this side is RESOLUTION: an id naming no area of the
            // user's simply finds nothing, and the goal stays unfiled.
            lifeAreaId = m["suggestedLifeAreaId"] as? String,
            category = GoalCategory.entries.firstOrNull { it.name.equals(categoryName, true) },
            confidence = (m["confidence"] as? Number)?.toFloat() ?: 0f,
            rationale = (m["rationale"] as? String).orEmpty(),
        )
    }

    /**
     * Reads a `plan` response into a draft.
     *
     * **The index is assigned here**, walking the list the Cloud Function returned in order —
     * §3.3 B's *"a new step … is identified by its position; the client assigns the id on
     * receipt"*. Nothing echoed from the model is read as an identifier, and there is no `id`
     * field to read even if one arrived.
     *
     * A step with no title is dropped rather than defaulted: the validator already rejects one,
     * so reaching this branch means the response did not come from the validator at all.
     */
    private fun parsePlan(data: Any?, goalId: String): GoalPlan {
        val m = data as? Map<*, *> ?: return GoalPlan(goalId = goalId)
        val raw = m["items"] as? List<*> ?: emptyList<Any?>()
        val steps = raw.mapNotNull { it as? Map<*, *> }
            .mapNotNull { item ->
                val title = (item["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                val kind = PlanStepKind.fromName(item["label"] as? String)
                PlanStep(
                    // Placeholder; replaced by the real position below, after the drops.
                    index = 0,
                    title = title,
                    kind = kind,
                    // A milestone is never priced (`C18`'s container rule). The Cloud Function
                    // already strips these three from one, so this is belt-and-braces on a rule
                    // that would be invisible if it broke — and ROUTINE is ×1.0, the absence of
                    // a judgement rather than a guess at one.
                    difficulty = if (kind == PlanStepKind.MILESTONE) {
                        Difficulty.ROUTINE
                    } else {
                        Difficulty.fromName(item["difficulty"] as? String)
                    },
                    estimatedMinutes = if (kind == PlanStepKind.MILESTONE) {
                        null
                    } else {
                        TaskDuration.sanitize((item["estimatedMinutes"] as? Number)?.toInt())
                    },
                    dayOffset = (item["dayOffset"] as? Number)?.toInt()?.takeIf { it >= 0 },
                    timeOfDay = if (kind == PlanStepKind.MILESTONE) {
                        null
                    } else {
                        parseTimeOfDay(item["timeOfDay"] as? String)
                    },
                )
            }
            .mapIndexed { index, step -> step.copy(index = index) }
        val notes = (m["changeNotes"] as? List<*>).orEmpty().mapNotNull { it as? String }
        return GoalPlan(goalId = goalId, steps = steps, changeNotes = notes)
    }

    /**
     * `HH:MM` into a [LocalTime], or `null`.
     *
     * `LocalTime.parse` **throws** on a malformed value, and this one runs inside a `map` over
     * every step — so an unparseable time would cost the whole plan rather than the one step's
     * slot. The validator already rejects anything this could throw on; catching is what makes
     * that a belt rather than a dependency.
     */
    private fun parseTimeOfDay(value: String?): LocalTime? = value?.let {
        runCatching { LocalTime.parse(it.trim()) }.getOrNull()
    }

    /**
     * The response into domain proposals, with the target computed here.
     *
     * **Resolution, not validation.** §3.4 puts validation in the Cloud Function
     * *singly*, and the membership check that matters — `goalId` against the ids the
     * request carried — has already run there. What is left is turning three
     * validated strings back into three Kotlin enums, which cannot disagree with
     * anything: it either resolves or it does not. An element that fails to resolve
     * is dropped whole, matching §3.3 E's *"there is no partial measure"* rather
     * than inventing a default kind.
     *
     * The target is put on last, by [ProposeMeasureUseCase.withComputedTarget], and
     * a goal with no structure entry gets an empty one — which yields a null target,
     * which the screen renders as *the app will not invent a number*.
     */
    private fun parseProposals(
        data: Any?,
        structures: Map<String, GoalStructure>,
    ): List<MeasureProposal> {
        val proposals = (data as? Map<*, *>)?.get("proposals") as? List<*> ?: return emptyList()
        return proposals.mapNotNull { element ->
            val m = element as? Map<*, *> ?: return@mapNotNull null
            val goalId = (m["goalId"] as? String)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val kind = MeasureKind.fromName(m["measureKind"] as? String)
                ?: return@mapNotNull null
            val word = (m["word"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val targetSource = TargetSource.fromName(m["targetSource"] as? String)
                ?: return@mapNotNull null
            ProposeMeasureUseCase.withComputedTarget(
                MeasureProposal(
                    goalId = goalId,
                    kind = kind,
                    word = word,
                    // The Function already substitutes OUTCOME for an unusable basis,
                    // so this second default is only reached by a response that never
                    // went through it — an older deployment. Same value, so the two
                    // cannot disagree.
                    basis = MeasureBasis.fromName(m["basis"] as? String) ?: MeasureBasis.OUTCOME,
                    targetSource = targetSource,
                    target = null,
                    origin = ProposalOrigin.MODEL,
                ),
                structures[goalId] ?: GoalStructure(),
            )
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
        // MEASURED GOALS ONLY, and the filter's own arithmetic is why (`#66`).
        //
        // `progressFraction` is `currentValue / targetValue`, and an unmeasured
        // goal's target is the `100.0` default it gets for saying nothing (§1.3,
        // `E6`). A goal nobody has logged against therefore sits at **exactly
        // 0.0** — below every measured goal that has moved at all — so
        // `< 0.34f` did not merely include unmeasured goals, it **preferred**
        // them, and `.take(2)` then handed the whole offline nudge feed to goals
        // with no number, quoting each one a percentage of a target nobody set.
        // The app said it out loud, which is what makes this the worst of the
        // ticket's sites rather than another stray digit.
        //
        // There is deliberately no unmeasured branch here. The thing worth saying
        // about such a goal is §1.3's measure OFFER, and §0.7 puts that on the
        // goal's own screen behind the consent of opening it — never in a feed
        // the user is scanning for something else. So the honest recommendation
        // count for an all-unmeasured account is the encouragement above and
        // nothing more.
        goals.filter { !it.isUnmeasured && it.progressFraction < 0.34f }
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
