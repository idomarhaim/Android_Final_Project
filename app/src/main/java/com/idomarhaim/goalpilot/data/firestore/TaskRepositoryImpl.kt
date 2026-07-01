package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.Leveling
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tasks live under users/{uid}/tasks. Completing a task is transactional:
 * it flips `done`, awards/rescinds points on the user + public leaderboard
 * projection, recomputes the level, and advances/retracts the linked goal's
 * progress — all atomically (spec §6 Core: point scoring, progress from tasks).
 */
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

    private fun goalsCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.GOALS)

    private fun publicDoc(uid: String) =
        firestore.collection(FirestorePaths.PUBLIC_PROFILES).document(uid)

    override fun observeTasks(goalId: String?): Flow<List<Task>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        val query = if (goalId != null) {
            tasksCol(uid).whereEqualTo("goalId", goalId)
        } else {
            tasksCol(uid)
        }
        // Sort client-side to avoid requiring a composite Firestore index.
        return query.snapshotsFlow().map { snap ->
            snap.toObjects(TaskDto::class.java)
                .map { it.toDomain() }
                .sortedWith(compareBy({ it.isDone }, { -it.createdAtEpochMillis }))
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
                firestore.runTransaction { txn ->
                    val taskRef = tasksCol(uid).document(taskId)
                    val userRef = userDoc(uid)

                    // ── All reads first (Firestore requires reads before writes) ──
                    val taskSnap = txn.get(taskRef)
                    val task = taskSnap.toObject(TaskDto::class.java)
                        ?: throw IllegalStateException("Task not found")
                    if (task.done == done) return@runTransaction // idempotent no-op

                    val userSnap = txn.get(userRef)
                    val goalRef = task.goalId?.takeIf { it.isNotBlank() }
                        ?.let { goalsCol(uid).document(it) }
                    val goalSnap = goalRef?.let { txn.get(it) }

                    // ── Writes ──
                    val sign = if (done) 1 else -1
                    val now = System.currentTimeMillis()

                    txn.update(
                        taskRef,
                        mapOf("done" to done, "completedAt" to if (done) now else null),
                    )

                    val currentPoints = userSnap.getLong("points") ?: 0L
                    val newPoints = (currentPoints + sign * task.points).coerceAtLeast(0)
                    val newLevel = Leveling.levelForPoints(newPoints)
                    txn.update(userRef, "points", newPoints)
                    txn.set(
                        publicDoc(uid),
                        mapOf(
                            "points" to newPoints,
                            "level" to newLevel,
                            "displayName" to (userSnap.getString("displayName") ?: ""),
                            "photoUrl" to userSnap.getString("photoUrl"),
                        ),
                        com.google.firebase.firestore.SetOptions.merge(),
                    )

                    if (goalRef != null && goalSnap != null) {
                        val target = goalSnap.getDouble("targetValue") ?: 100.0
                        val current = goalSnap.getDouble("currentValue") ?: 0.0
                        val next = (current + sign * task.progressContribution)
                            .coerceIn(0.0, target)
                        txn.update(goalRef, mapOf("currentValue" to next, "updatedAt" to now))
                    }
                }.await()
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
