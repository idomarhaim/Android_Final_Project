package com.idomarhaim.goalpilot.domain.model

/**
 * An **edge** from a task to an objective, carrying what the task is worth to that
 * objective's measure — `docs/PRODUCT_v0.3.md` §1.5
 * ([#55](https://github.com/idomarhaim/Android_Final_Project/issues/55)).
 *
 * ### Why contribution moved off the task
 *
 * It was `Task.progressContribution: Double = 1.0`, and §1.5's verdict on that default is
 * the whole reason this type exists: *"`progressContribution`'s `1.0` was a silence, not a
 * value."* A number on the **task** says how much the task is worth *in general*, which is
 * not a thing that can be true — a 30-minute run is `1` to *"run 20 times"*, `5` to
 * *"run 100 km"* and meaningless to *"lose 5 kg"*. The quantity belongs to the **pair**, and
 * an edge is the pair.
 *
 * ### `null` is the honest default, and it means *nothing*
 *
 * §1.5: *"an edge declares its contribution **in the objective's own word**, or contributes
 * nothing to the measure"*. So [contribution] is nullable and defaults to absent, and
 * `DerivedProgress` adds nothing for an edge that declares nothing. It is not `0.0` dressed
 * up: `0.0` is a declaration that this work is worth nothing, which is a different sentence
 * from *nobody said*.
 *
 * **Documents written before `#55` are read at their stored value, not at `null`** — see
 * `TaskDto.progressContribution`. Rewriting a stored number as a silence would be a
 * migration that deletes data, and the default here governs what a *new* edge gets when
 * nothing declares one.
 */
data class GoalEdge(
    val goalId: String,
    /** What one completion adds to the objective's measure, or `null` for *undeclared*. */
    val contribution: Double? = null,
)

/**
 * The edge list for a task filed under **at most one** objective.
 *
 * Every write site in the app today produces zero or one edge, because the surfaces that
 * create tasks offer one goal picker. §1.2's many-to-many edges are a different ticket; this
 * helper exists so those sites read as *"filed under this goal"* rather than assembling a
 * list, and so that adding a second edge later is a change in one place.
 */
fun goalEdgesOf(goalId: String?, contribution: Double? = null): List<GoalEdge> =
    goalId?.takeIf { it.isNotBlank() }
        ?.let { listOf(GoalEdge(goalId = it, contribution = contribution)) }
        ?: emptyList()

/**
 * What is banked when a task is completed — `docs/PRODUCT_v0.3.md` §1.4, `#55`.
 *
 * ### It banks the **inputs**, never the number
 *
 * §1.4: *"Points are banked as their inputs, not as a number. On completion, `minutes` and
 * `difficulty` are stamped into a timestamped completion fact, and the lifetime total is a
 * sum over facts. So the arithmetic never branches, no stored number can disagree, and a
 * level can never fall."*
 *
 * The defect that argument is aimed at is re-pricing: tick a task worth 10, correct its
 * duration so it is now worth 30, untick — and any design that stores the *number* at one
 * end and re-reads it at the other loses 30 for a 10. Banking the inputs makes that
 * unrepresentable rather than guarded: the fact is written once and never recomputed, so the
 * points it is worth are the same number every time anybody asks.
 *
 * ### Why it is its own document and not more fields on the task
 *
 * `TaskRepositoryImpl.upsertTask` is a whole-document `set()`, so banked values living on
 * the task would be overwritten by an ordinary edit — retitle a completed task and its
 * history is re-priced at today's estimate. A separate document under
 * `users/{uid}/completionFacts/{taskId}` is not in that write's path at all, which is the
 * structural version of the same guarantee.
 *
 * ### The fact **is** the completion
 *
 * There is no `done` flag beside this. [Task.isDone] is `completion != null`, so the
 * half-written fact `TaskCompletion` used to normalise — done with no stamp, or a stamp with
 * no done — cannot be constructed. That was four readers disagreeing about which field was
 * the fact; now there is one field and it is this object.
 */
data class CompletionFact(
    /** When the completion happened. */
    val completedAtEpochMillis: Long = 0L,
    /** The duration as it stood at the tick — the effort half of the price. */
    val minutes: Int = TaskDuration.DEFAULT_MINUTES,
    /** The judgement as it stood at the tick — the multiplier half. */
    val difficulty: Difficulty = Difficulty.ROUTINE,
) {
    /** What this completion is worth, forever: a pure function of the banked inputs. */
    val points: Int get() = TaskScoring.pointsFor(minutes, difficulty)
}

/**
 * A task associated with an objective (spec §6 Core: "Associate tasks with goals").
 *
 * ### What `#55` changed, and why four fields became properties
 *
 * `points`, `goalId`, `isDone` and `completedAtEpochMillis` used to be **stored**. All four
 * are now computed, and each is §0.2's derive-don't-store rule applied to a value that had
 * grown a second home:
 *
 * - **`points`** is a *view of effort* (§1.4), `round(minutes / 3) × difficulty`. It was
 *   stored, and worse, `TaskDuration.minutesOf` derived the **minutes** back out of it — so
 *   an offline task invented a reward number from its word count and the time-allocation
 *   chart was computed downstream of a gamification currency. `#55` inverts that constant.
 * - **`isDone` / `completedAtEpochMillis`** are one fact and are now literally one object,
 *   [completion]. See [CompletionFact].
 * - **`goalId`** is the first edge's id: the link and what it contributes belong together on
 *   a [GoalEdge] (§1.5), and the property below keeps every reader compiling.
 */
data class Task(
    val id: String = "",
    val title: String = "",
    /**
     * The objectives this task serves, each with what it contributes to that objective's
     * measure (§1.5). Empty means unfiled, which is a legitimate state.
     */
    val goalEdges: List<GoalEdge> = emptyList(),
    /**
     * How demanding the work is — the multiplier half of [points] (§1.4).
     *
     * [Difficulty.ROUTINE] is `×1.0`, so a task nobody has judged is priced on its minutes
     * alone. That is why the default is safe: it is not a guess about the task, it is the
     * absence of one.
     */
    val difficulty: Difficulty = Difficulty.ROUTINE,
    val source: TaskSource = TaskSource.MANUAL,
    /**
     * How long the task takes, in minutes — typed by the user, or estimated by the
     * LLM when the task is classified or scored. The raw material of the
     * time-allocation chart **and, since `#55`, of the point value**.
     *
     * Null means "no duration at all"; [TaskDuration.minutesOf] supplies the fallback rather
     * than dropping the task from the chart. [durationSource] says which of the two wrote
     * it, and is the only thing that may be read to find out.
     */
    val estimatedMinutes: Int? = null,
    /**
     * Where [estimatedMinutes] came from (#9, spec §1.4).
     *
     * [DurationSource.USER] is **sticky**: no re-estimation may overwrite it,
     * unconditionally and with no threshold. Enforced structurally rather than by a
     * check on what comes back — such a task is never *sent* for re-estimation
     * (§3.3 A: *"those tasks are not in `tasks[]` at all"*), which is why
     * `BackfillDurationsUseCase` filters on this field rather than comparing numbers
     * afterwards.
     *
     * Every task written before #9 reads as [DurationSource.UNKNOWN], and that is
     * the honest value rather than a convenient one: no code path let a person type
     * a duration before this ticket, so no stored value can be a typed one, and
     * `UNKNOWN` is therefore safe to re-estimate.
     */
    val durationSource: DurationSource = DurationSource.UNKNOWN,
    /**
     * **When** the task is due, as one of §2.2's four rungs — or `null` for a task that is
     * simply on the list ([#56](https://github.com/idomarhaim/Android_Final_Project/issues/56)).
     *
     * `null` is the common case and a legitimate one: most tasks have no *when* at all, and
     * §2.2's rungs are for the ones that do. Nothing derives a due date from a creation date
     * or from anything else — an absent occurrence means nobody said, exactly as an absent
     * [estimatedMinutes] does.
     *
     * **Its temporal state is not here, and cannot be** (§2.3): ask
     * [Occurrence.stateAt] with the clock you have. There is no `isOverdue` on this class,
     * no `missedAt`, and no field a sweep would have to keep true — which is what makes
     * *"the reminder re-checks at fire time"* free rather than a second schedule to maintain.
     *
     * **At most one**, and §2.1 wants more: a *rule* on the task plus occurrence documents, so
     * that *"this occurrence, or all future ones?"* is askable. `#56` builds the occurrence
     * half; recurrence is the other half and is not here. See [Occurrence] for why arriving at
     * it is additive rather than a rewrite.
     */
    val occurrence: Occurrence? = null,
    val createdAtEpochMillis: Long = 0L,
    /**
     * The completion fact, or `null` when the task is not done (§1.4, `#55`).
     *
     * Written to `users/{uid}/completionFacts/{taskId}` and joined back in by
     * `TaskRepositoryImpl.observeTasks`; a task document written before `#55` carries
     * `done` + `completedAt` instead and is read into an equivalent fact by
     * `TaskDto.legacyCompletion()`, with no migrating write required to see it.
     */
    val completion: CompletionFact? = null,
) {

    /**
     * The objective this task is filed under, or `null` when it is unfiled.
     *
     * A **view of the first edge**, kept as a property so the dozen readers that ask
     * *"which goal is this?"* did not all have to learn about edges at once. The task's edge
     * set is the truth; `TaskDto.goalId` is a stored projection of it that exists only so
     * Firestore can index the query, and it is rewritten from [goalEdges] on every write.
     */
    val goalId: String? get() = goalEdges.firstOrNull()?.goalId

    /** Whether the task is done — which is exactly *whether a completion fact exists*. */
    val isDone: Boolean get() = completion != null

    /** When it was completed, or `null`. Cannot disagree with [isDone]; they are one field. */
    val completedAtEpochMillis: Long? get() = completion?.completedAtEpochMillis

    /**
     * What the task is worth, in points (§1.4).
     *
     * **A completed task is priced from what was banked**, never from what it looks like
     * today — that is [CompletionFact]'s whole job, and it is what stops a re-estimation
     * from rewriting history. An open task is priced from its current minutes and
     * difficulty, which is what the add row and the goal list show.
     */
    val points: Int
        get() = completion?.points
            ?: TaskScoring.pointsFor(TaskDuration.minutesOf(this), difficulty)
}

/** Where a task originated (spec §6 nice-to-have: Google Tasks import). */
enum class TaskSource {
    MANUAL,
    GOOGLE_TASKS;

    companion object {
        fun fromName(name: String?): TaskSource =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MANUAL
    }
}
