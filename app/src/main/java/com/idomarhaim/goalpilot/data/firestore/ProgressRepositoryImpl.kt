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
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.ProgressDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
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
class ProgressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProgressRepository {

    private fun progressCol(uid: String, goalId: String): CollectionReference =
        firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.GOALS).document(goalId)
            .collection(FirestorePaths.PROGRESS)

    override fun observeEntries(goalId: String): Flow<List<ProgressEntry>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                progressCol(uid, goalId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .snapshotsFlow()
                    .map { snap -> snap.toObjects(ProgressDto::class.java).map { it.toDomain() } }
            }
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
                    // WHEN THE THING HAPPENED, NOT WHEN IT WAS WRITTEN DOWN.
                    //
                    // This read `System.currentTimeMillis()` unconditionally until
                    // 2026-08-25, which silently discarded the one field a caller might
                    // legitimately want to set -- and it made a whole class of feature
                    // impossible without anything ever failing:
                    //
                    //  * A Health Connect reading for LAST TUESDAY was stamped with the
                    //    moment the sync ran. `SyncHealthDataUseCase` knows the reading's
                    //    own day (`HealthLogProposal.epochDay`) and had no way to say so.
                    //  * `ScoringWindow.includes()` filters a challenge's score by entry
                    //    timestamp, so a challenge whose window is in the past scored ZERO
                    //    for everybody -- which is exactly what Ido asked for on 2026-08-25
                    //    ("a retroactive challenge for last week") and could not have got.
                    //  * `ScoringWindow`'s own KDoc already claimed "a backfilled entry
                    //    with an old timestamp correctly changes nothing", describing a
                    //    state no entry could reach. The comment was ahead of the code.
                    //
                    // Zero still means now, so every existing caller is unchanged: the
                    // manual log dialog, the task-completion path and the tests all build a
                    // `ProgressEntry` without a timestamp and get one.
                    createdAt = entry.createdAtEpochMillis.takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                    sourceKey = entry.sourceKey,
                )
                ref.set(dto).await()

                // There is no step 3. The entry *is* the progress (#49): the goal's
                // current value is a sum over this collection, so the one write above
                // either happened or did not, and either way the two numbers agree.
                // The counter this used to advance had to be written second, which is
                // what made a crash in between corrupt the goal permanently — and made
                // a failure *after* the entry landed report a loss that had not
                // occurred, inviting the user to log it twice.
                Resource.Success(ref.id)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not log progress", e)
            }
        }
}
