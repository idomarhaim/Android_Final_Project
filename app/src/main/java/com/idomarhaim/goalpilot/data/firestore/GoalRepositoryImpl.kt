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
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : GoalRepository {

    private fun goalsCol(uid: String): CollectionReference =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.GOALS)

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
            }
        }

    override fun observeGoal(goalId: String): Flow<Goal?> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                goalsCol(uid).document(goalId).snapshotsFlow()
                    .map { it.toObject(GoalDto::class.java)?.toDomain() }
            }
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

    override suspend fun addProgress(goalId: String, delta: Double): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                val ref = goalsCol(uid).document(goalId)
                firestore.runTransaction { txn ->
                    val snap = txn.get(ref)
                    val target = snap.getDouble("targetValue") ?: 100.0
                    val current = snap.getDouble("currentValue") ?: 0.0
                    val next = (current + delta).coerceIn(0.0, target)
                    txn.update(
                        ref,
                        mapOf("currentValue" to next, "updatedAt" to System.currentTimeMillis()),
                    )
                }.await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not update progress", e)
            }
        }

    override suspend fun setLifeArea(goalId: String, lifeAreaId: String?): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                goalsCol(uid).document(goalId)
                    .update("lifeAreaId", lifeAreaId, "updatedAt", System.currentTimeMillis())
                    .await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not change the goal's life area", e)
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

    override suspend fun deleteGoal(goalId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            goalsCol(uid).document(goalId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not delete goal", e)
        }
    }
}
