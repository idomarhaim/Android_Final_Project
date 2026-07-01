package com.idomarhaim.goalpilot.data.firestore

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.core.util.StoragePaths
import com.idomarhaim.goalpilot.data.firestore.dto.ProgressDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository,
    private val goalRepository: GoalRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProgressRepository {

    private fun progressCol(uid: String, goalId: String): CollectionReference =
        firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.GOALS).document(goalId)
            .collection(FirestorePaths.PROGRESS)

    override fun observeEntries(goalId: String): Flow<List<ProgressEntry>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return progressCol(uid, goalId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshotsFlow()
            .map { snap -> snap.toObjects(ProgressDto::class.java).map { it.toDomain() } }
    }

    override suspend fun logProgress(entry: ProgressEntry, imageUri: Uri?): Resource<String> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                // 1) Upload the optional image first so its URL can be persisted.
                val imageUrl: String? = if (imageUri != null) {
                    when (val up = storageRepository.uploadImage(StoragePaths.PROGRESS_IMAGES, imageUri)) {
                        is Resource.Success -> up.data
                        is Resource.Error -> return@withContext Resource.Error(up.message, up.throwable)
                        Resource.Loading -> null
                    }
                } else {
                    null
                }

                // 2) Persist the progress entry.
                val ref = progressCol(uid, entry.goalId).document()
                val dto = ProgressDto(
                    id = ref.id,
                    goalId = entry.goalId,
                    value = entry.value,
                    note = entry.note,
                    imageUrl = imageUrl,
                    createdAt = System.currentTimeMillis(),
                )
                ref.set(dto).await()

                // 3) Advance the goal's current value by the logged amount.
                if (entry.value != 0.0) {
                    goalRepository.addProgress(entry.goalId, entry.value)
                }
                Resource.Success(ref.id)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not log progress", e)
            }
        }
}
