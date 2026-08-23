package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.GoalDto
import com.idomarhaim.goalpilot.data.firestore.dto.ProgressDto
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.withDerivedProgress
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : GoalRepository {

    private fun goalsCol(uid: String): CollectionReference =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.GOALS)

    private fun tasksCol(uid: String): CollectionReference =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.TASKS)

    private fun progressCol(uid: String, goalId: String): CollectionReference =
        goalsCol(uid).document(goalId).collection(FirestorePaths.PROGRESS)

    override fun observeGoals(includeArchived: Boolean): Flow<List<Goal>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                goalsCol(uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .snapshotsFlow()
                    .map { snap ->
                        snap.toObjects(GoalDto::class.java)
                            .map { it.toDomain() }
                            .filter { includeArchived || !it.isArchived }
                    }
                    .flatMapLatest { goals ->
                        if (goals.isEmpty()) {
                            flowOf(goals)
                        } else {
                            combine(
                                entriesFlow(uid, goals.map { it.id }),
                                tasksFlow(uid),
                            ) { entries, tasks -> goals.withDerivedProgress(entries, tasks) }
                        }
                    }
            }
        }

    override fun observeGoal(goalId: String): Flow<Goal?> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                combine(
                    goalsCol(uid).document(goalId).snapshotsFlow()
                        .map { it.toObject(GoalDto::class.java)?.toDomain() },
                    entriesFlow(uid, listOf(goalId)),
                    tasksFlow(uid, goalId),
                ) { goal, entries, tasks ->
                    goal?.withDerivedProgress(entries, tasks)
                }
            }
        }

    /**
     * The user's tasks, for the completed-task half of the sum. Optionally narrowed
     * to one goal — the detail screen needs no others, and the query is the same
     * one `TaskRepositoryImpl.observeTasks` already runs, so Firestore serves both
     * from a single listen target.
     *
     * ⚠️ **It goes through [TaskStream], and that is the whole of a `#55` defect.** Since the
     * completion moved into its own document (§1.4), reading the tasks collection alone gives
     * back tasks that are all **open** — so this sum, which counts only completed ones, read
     * zero for every migrated task. `Observed:` on a device, 2026-08-21, as a goal ring
     * dropping to 0% while the task list above it showed the task ticked. Never read
     * `TaskDto` here directly; the seam is what stops the two screens disagreeing.
     */
    private fun tasksFlow(uid: String, goalId: String? = null): Flow<List<Task>> =
        TaskStream.observe(
            firestore.collection(FirestorePaths.USERS).document(uid),
            goalId,
        )

    /**
     * Every progress entry belonging to [goalIds], flattened.
     *
     * **One listener per goal, and that is not an oversight.** Progress entries are
     * a subcollection of each goal, so the single-query alternative is
     * `collectionGroup("progressEntries")` — and a collection-group query needs its
     * own `match /{path=**}/progressEntries/{id}` rule, inside which the path
     * segments are not addressable, so binding it to the signed-in user means
     * denormalising `uid` onto every entry document. That is a schema change and a
     * backfill, and #49's whole claim is that it needs neither: the entries are
     * already the record.
     *
     * The fan-out is rebuilt whenever the goal list emits, which sounds worse than
     * it is — and is *less* frequent than before this change, because deleting the
     * two `currentValue` writers means a goal document no longer changes every time
     * something is logged against it or a task under it is ticked.
     */
    private fun entriesFlow(uid: String, goalIds: List<String>): Flow<List<ProgressEntry>> {
        val ids = goalIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return flowOf(emptyList())
        return combine(
            ids.map { goalId ->
                progressCol(uid, goalId).snapshotsFlow()
                    .map { snap -> snap.toObjects(ProgressDto::class.java).map { it.toDomain() } }
            },
        ) { perGoal -> perGoal.asList().flatten() }
    }

    override suspend fun upsertGoal(goal: Goal): Resource<String> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            val col = goalsCol(uid)
            val ref = if (goal.id.isBlank()) col.document() else col.document(goal.id)
            val now = System.currentTimeMillis()
            val toSave = goal.copy(
                id = ref.id,
                createdAtEpochMillis = if (goal.createdAtEpochMillis == 0L) now else goal.createdAtEpochMillis,
                updatedAtEpochMillis = now,
            )
            ref.set(toSave.toDto()).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not save goal", e)
        }
    }

    // `addProgress` used to sit here: a read-modify-write transaction on
    // `currentValue`, called by `logProgress` *after* it had already committed the
    // entry it was crediting. It is deleted rather than made atomic (#49, §5.2) —
    // the number it maintained is now summed from the entries themselves, so there
    // is no second write to sequence and no fourth clamp to make legal overshoot
    // unreachable on the one screen a human writes to.

    override suspend fun setLifeAreas(goalId: String, lifeAreaIds: List<String>): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                // The legacy singular field is cleared in the same update, not left
                // behind: a document that carried both would read back one answer
                // through the mapper and a different one through any query still
                // written against `lifeAreaId`.
                goalsCol(uid).document(goalId)
                    .update(
                        mapOf(
                            "lifeAreaIds" to lifeAreaIds.filter { it.isNotBlank() }.distinct(),
                            "lifeAreaId" to null,
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                    .await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not change the goal's life areas", e)
            }
        }

    override suspend fun setDeclaredBy(goalId: String, declaredBy: DeclaredBy?): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                // `null` is written as the NONE sentinel, never as a Firestore null and never
                // by deleting the field: absence means *this document predates `#6`*, so
                // erasing the marker would make a demoted goal read back as UNKNOWN — a goal
                // again — on the very next snapshot. See `GoalDto.declaredBy`.
                goalsCol(uid).document(goalId)
                    .update(
                        mapOf(
                            "declaredBy" to (declaredBy?.name ?: GoalDto.DECLARED_BY_NONE),
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                    .await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not change the goal", e)
            }
        }

    override suspend fun setArchived(goalId: String, archived: Boolean): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                goalsCol(uid).document(goalId)
                    .update("archived", archived, "updatedAt", System.currentTimeMillis())
                    .await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not archive goal", e)
            }
        }

    /**
     * Removes the goal, **unfiles its tasks**, and takes its progress log with it (`#67`).
     *
     * ### Before `#67` this deleted one document, and both halves of that were defects
     *
     * `Observed:` 2026-08-23, mechanically. It removed `goals/{goalId}` and nothing else.
     *
     * **The tasks kept an edge to it.** A task whose only edge points at a deleted goal reads
     * as *filed* — `Task.goalEdges` is not empty — while being listed on no screen: the goal
     * detail that would show it cannot be opened, and the goals list has nothing to show. That
     * is `#67`'s founding defect, manufactured by the app's own delete. `Deletion.unreachableTasks`
     * is the predicate that catches an existing one; this is what stops new ones being made.
     *
     * **The progress log became unreachable.** Deleting a Firestore document does not delete
     * its subcollections, and `entriesFlow` above fans out over *the goals that exist* — so
     * `goals/{goalId}/progressEntries` had no reader left, and no route to one. The same dead
     * storage an orphan occurrence is (`TaskRepositoryImpl.deleteTask`).
     *
     * ### The tasks are kept, not deleted, and that is derived rather than chosen here
     *
     * §1.1's *lossless demotion* — *"the task underneath is real work he typed in"* — is the
     * clause that already forbids `GoalsScreen`'s suggested-goal banner offering a delete. The
     * work outlives the objective it was filed under, so the edge goes and the task stays,
     * which is exactly what `deleteLifeArea` already does one level up when it unfiles an
     * area's goals rather than taking them down. `DeletionImpact.OfGoal` states both halves
     * before the act, with the counts.
     *
     * ### Order, and what an interrupted run leaves behind
     *
     * Edges first, then entries, then the goal document — the same reasoning `deleteLifeArea`
     * writes down: if a later step fails the goal survives, which is recoverable, whereas
     * removing the goal first would leave tasks pointing at nothing and a log nobody can reach,
     * which is the state this method exists to prevent. Chunked for the same reason
     * `deleteTask` is chunked, and idempotent for the same reason.
     */
    override suspend fun deleteGoal(goalId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            // `TaskDto.goalId` is the indexed projection of the edge list, rewritten from
            // `goalEdges` on every write -- so it is what a query can filter on, and the edge
            // array is what has to be corrected. Both are updated together for the reason
            // `setLifeAreas` gives about its own legacy singular: a document carrying one
            // answer in the array and another in the projection reads differently through the
            // mapper than through a query.
            val filed = tasksCol(uid).whereEqualTo(TASK_GOAL_ID, goalId).get().await()
            filed.documents.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                val now = System.currentTimeMillis()
                chunk.forEach { doc ->
                    val remaining = (doc.get(TASK_GOAL_EDGES) as? List<*>)
                        .orEmpty()
                        .filterIsInstance<Map<*, *>>()
                        .filterNot { it[EDGE_GOAL_ID] == goalId }
                    batch.update(
                        doc.reference,
                        mapOf(
                            TASK_GOAL_EDGES to remaining,
                            // The projection follows the array it projects: first surviving
                            // edge, or absent. `Task.goalId` reads exactly this way.
                            TASK_GOAL_ID to (remaining.firstOrNull()?.get(EDGE_GOAL_ID)),
                            "updatedAt" to now,
                        ),
                    )
                }
                batch.commit().await()
            }

            val entries = progressCol(uid, goalId).get().await()
            entries.documents.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            goalsCol(uid).document(goalId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not delete goal", e)
        }
    }

    private companion object {
        /**
         * `TaskDto`'s two goal fields and `GoalEdgeDto`'s id, as query and update keys.
         *
         * Named rather than typed inline for `TaskRepositoryImpl.OCCURRENCE_TASK_ID`'s reason: a
         * rename would leave the query matching nothing, and a delete that unfiles no tasks is
         * indistinguishable from a goal that had none.
         */
        const val TASK_GOAL_ID = "goalId"
        const val TASK_GOAL_EDGES = "goalEdges"
        const val EDGE_GOAL_ID = "goalId"

        /** Firestore's per-batch write cap, with headroom. See `TaskRepositoryImpl.BATCH_LIMIT`. */
        const val BATCH_LIMIT = 450
    }
}
