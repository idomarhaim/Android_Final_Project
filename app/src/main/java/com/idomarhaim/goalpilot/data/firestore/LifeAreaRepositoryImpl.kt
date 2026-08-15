package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.LifeAreaDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
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
 * Life areas live at `users/{uid}/lifeAreas`, under the same owner-only rule as
 * goals and tasks (`firestore.rules` matches `users/{uid}/{document=**}`), so no
 * rules change was needed to add them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LifeAreaRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LifeAreaRepository {

    private fun userDoc(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid)

    private fun areasCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.LIFE_AREAS)

    private fun goalsCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.GOALS)

    override fun observeLifeAreas(includeArchived: Boolean): Flow<List<LifeArea>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                // Sorted client-side: ordering by (sortOrder, name) server-side
                // would need a composite index for a collection that holds a
                // handful of documents.
                areasCol(uid).snapshotsFlow().map { snap ->
                    snap.toObjects(LifeAreaDto::class.java)
                        .map { it.toDomain() }
                        .filter { includeArchived || !it.isArchived }
                        .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))
                }
            }
        }

    override suspend fun upsertLifeArea(area: LifeArea): Resource<String> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        if (area.name.isBlank()) return@withContext Resource.Error("A life area needs a name")
        try {
            val col = areasCol(uid)
            val ref = if (area.id.isBlank()) col.document() else col.document(area.id)
            val now = System.currentTimeMillis()
            val toSave = area.copy(
                id = ref.id,
                name = area.name.trim(),
                createdAtEpochMillis =
                    if (area.createdAtEpochMillis == 0L) now else area.createdAtEpochMillis,
                updatedAtEpochMillis = now,
            )
            ref.set(toSave.toDto()).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not save life area", e)
        }
    }

    override suspend fun deleteLifeArea(areaId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            // Unfile the goals first. If this half fails the area survives, which
            // is recoverable; doing it the other way round would leave goals
            // pointing at an area the user can no longer see or repair.
            //
            // Two queries, because a goal written before §1.2 made the edge plural
            // still carries the singular `lifeAreaId` and matches neither
            // `arrayContains` nor the other way round. A document is deduplicated
            // by path, so one that somehow matched both is still updated once.
            val plural = goalsCol(uid).whereArrayContains("lifeAreaIds", areaId).get().await()
            val legacy = goalsCol(uid).whereEqualTo("lifeAreaId", areaId).get().await()
            val affected = (plural.documents + legacy.documents).distinctBy { it.reference.path }
            if (affected.isNotEmpty()) {
                val batch = firestore.batch()
                val now = System.currentTimeMillis()
                affected.forEach { doc ->
                    // `arrayRemove` on a document that has no `lifeAreaIds` yet
                    // writes the empty array, which is exactly the `[]` §7.1 asks
                    // an unfiled goal to be backfilled to.
                    batch.update(
                        doc.reference,
                        mapOf(
                            "lifeAreaIds" to FieldValue.arrayRemove(areaId),
                            "lifeAreaId" to null,
                            "updatedAt" to now,
                        ),
                    )
                }
                batch.commit().await()
            }
            areasCol(uid).document(areaId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not delete life area", e)
        }
    }

    override suspend fun reorderLifeAreas(newSortOrders: Map<String, Int>): Resource<Unit> =
        withContext(io) {
            // A drop that changed nothing (or was cancelled) is a success with no
            // write, not an error the user has to see.
            if (newSortOrders.isEmpty()) return@withContext Resource.Success(Unit)
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                val col = areasCol(uid)
                val now = System.currentTimeMillis()
                val batch = firestore.batch()
                newSortOrders.forEach { (areaId, sortOrder) ->
                    batch.update(
                        col.document(areaId),
                        mapOf("sortOrder" to sortOrder, "updatedAt" to now),
                    )
                }
                // One batch, so the list can never be observed half-reordered —
                // which would show the dragged card in two places at once.
                batch.commit().await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not save the new order", e)
            }
        }

    override suspend fun linkGoogleList(areaId: String, googleListId: String): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                areasCol(uid).document(areaId)
                    .update(
                        "googleListId", googleListId,
                        "updatedAt", System.currentTimeMillis(),
                    )
                    .await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not link the Google Tasks list", e)
            }
        }
}
