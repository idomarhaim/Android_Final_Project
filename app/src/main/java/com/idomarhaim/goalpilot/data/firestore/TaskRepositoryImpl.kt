package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tasks live under users/{uid}/tasks. **Completing one writes exactly one document, and
 * what it writes is a fact.**
 *
 * This class used to be the last client writer of derived state. `setDone` held three
 * writes in one `firestore.runTransaction`: the tick, `users/{uid}.points`, and the
 * `publicProfiles/{uid}` leaderboard row including its `level`. `C20` (#42, spec §5.2)
 * moved the last two to `functions/src/projection.ts` under one rule — *a derived number
 * gets a stored writer if and only if somebody who cannot read its inputs has to read it*
 * — and deleted `publicProfiles.level` outright, because a stored function of `points`
 * in the same document is readable by everyone who can read the thing it is computed from.
 *
 * **Two consequences worth knowing before editing this file.**
 *
 * 1. There is no transaction here any more, which is the offline win (spec §5.3): one
 *    single-document write lands in the Firestore cache with the radio off, and the tick
 *    is instant. `runTransaction` could not be served from the cache and took a measured
 *    7.9 s to fail (closed #3). `app/src/test/.../guards/OfflineWriteGuardTest.kt` is
 *    watching for exactly this and reports **skipped** from the moment it is true.
 * 2. Points do not move until the projection function runs. On a device with no functions
 *    deployed, the tick still works and the owner's own totals — which are summed from
 *    these facts on the device — are still right; the public leaderboard row is what
 *    goes stale. That is the trade §5.2 made deliberately, not a regression.
 *
 * It also does not advance the linked goal (#49, spec §5.2): the goal sums
 * `progressContribution` over its completed tasks, so the tick alone carries the progress
 * and there is no derived number stored anywhere to fall out of step.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TaskRepository {

    private fun userDoc(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid)

    private fun tasksCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.TASKS)


    override fun observeTasks(goalId: String?): Flow<List<Task>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                val query = if (goalId != null) {
                    tasksCol(uid).whereEqualTo("goalId", goalId)
                } else {
                    tasksCol(uid)
                }
                // Sort client-side to avoid requiring a composite Firestore index.
                query.snapshotsFlow().map { snap ->
                    snap.toObjects(TaskDto::class.java)
                        .map { it.toDomain() }
                        .sortedWith(compareBy({ it.isDone }, { -it.createdAtEpochMillis }))
                }
            }
        }

    override suspend fun upsertTask(task: Task): Resource<String> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            val col = tasksCol(uid)
            val ref = if (task.id.isBlank()) col.document() else col.document(task.id)
            val toSave = task.copy(
                id = ref.id,
                createdAtEpochMillis = if (task.createdAtEpochMillis == 0L) {
                    System.currentTimeMillis()
                } else {
                    task.createdAtEpochMillis
                },
            )
            ref.set(toSave.toDto()).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not save task", e)
        }
    }

    override suspend fun setDone(taskId: String, done: Boolean): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                // ONE WRITE, TO ONE DOCUMENT, AND IT IS A FACT (`C20` #42, spec §5.2).
                //
                // `done` and `completedAt` are the only things here that record something
                // the user *did*. The two writes that used to sit beside them —
                // `users/{uid}.points` and the `publicProfiles/{uid}` row — were derived
                // numbers, and a derived number gets a stored writer only when somebody
                // who cannot read its inputs has to read it. The owner can read their own
                // tasks, so their own total is summed on the device; a leaderboard reader
                // cannot, so the public copy is written by the projection function
                // (`functions/src/projection.ts`) from this very fact.
                //
                // With those gone there is nothing left for a transaction to be atomic
                // about, and dropping it is the whole offline win (spec §5.3): a single
                // document write goes straight into the Firestore cache and completes
                // immediately with the radio off, where `runTransaction` cannot touch the
                // cache at all and failed after a measured 7.9 s (closed #3).
                //
                // No read-then-write, so the idempotent no-op that used to guard against
                // double-crediting is not needed either: setting `done` to the value it
                // already holds writes the same document twice, which is the same state.
                // Nothing accumulates anywhere, which is exactly what §5.2 bought.
                tasksCol(uid).document(taskId)
                    .update(
                        mapOf(
                            "done" to done,
                            "completedAt" to if (done) System.currentTimeMillis() else null,
                        ),
                    ).await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not update task", e)
            }
        }

    override suspend fun deleteTask(taskId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            tasksCol(uid).document(taskId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not delete task", e)
        }
    }
}
