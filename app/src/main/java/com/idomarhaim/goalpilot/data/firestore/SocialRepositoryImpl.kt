package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.firestore.dto.PublicProfileDto
import com.idomarhaim.goalpilot.data.firestore.dto.SharedItemDto
import com.idomarhaim.goalpilot.data.firestore.dto.resolvedLevel
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SocialRepository {

    private val publicCol get() = firestore.collection(FirestorePaths.PUBLIC_PROFILES)
    private val sharesCol get() = firestore.collection(FirestorePaths.SHARES)
    private fun friendsCol(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.FRIENDS)

    override fun observeLeaderboard(friendsOnly: Boolean): Flow<List<LeaderboardEntry>> {
        val uid = auth.currentUser?.uid
        val profiles = publicCol
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(100)
            .snapshotsFlow()
            .map { it.toObjects(PublicProfileDto::class.java) }

        return combine(profiles, observeFriendUids()) { list, friends ->
            list.asSequence()
                .filter { !friendsOnly || it.uid == uid || it.uid in friends }
                .mapIndexed { index, p ->
                    LeaderboardEntry(
                        uid = p.uid,
                        displayName = p.displayName,
                        photoUrl = p.photoUrl,
                        points = p.points,
                        level = p.resolvedLevel(),
                        rank = index + 1,
                        isCurrentUser = p.uid == uid,
                        isFriend = p.uid in friends,
                    )
                }
                .toList()
        }
    }

    override fun observeFeed(): Flow<List<SharedItem>> =
        sharesCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshotsFlow()
            .map { snap -> snap.toObjects(SharedItemDto::class.java).map { it.toDomain() } }

    override fun observeFriendUids(): Flow<Set<String>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptySet())
        return friendsCol(uid).snapshotsFlow()
            .map { snap -> snap.documents.map { it.id }.toSet() }
    }

    override suspend fun addFriend(uid: String): Resource<Unit> = withContext(io) {
        val me = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        if (uid == me) return@withContext Resource.Error("You cannot add yourself")
        try {
            friendsCol(me).document(uid)
                .set(mapOf("addedAt" to System.currentTimeMillis())).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not add friend", e)
        }
    }

    override suspend fun removeFriend(uid: String): Resource<Unit> = withContext(io) {
        val me = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            friendsCol(me).document(uid).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not remove friend", e)
        }
    }

    override suspend fun shareSummary(
        summary: ProgressSummary,
        imageUrl: String?,
    ): Resource<String> = withContext(io) {
        val user = auth.currentUser ?: return@withContext Resource.Error("Not signed in")
        try {
            val ref = sharesCol.document()
            val avgPercent = (summary.averageProgress * 100).roundToInt()
            val dto = SharedItemDto(
                id = ref.id,
                authorUid = user.uid,
                authorName = user.displayName.orEmpty(),
                authorPhotoUrl = user.photoUrl?.toString(),
                period = summary.period.name,
                headline = "${summary.period.label} progress",
                message = "Earned ${summary.totalPoints} pts • " +
                    "${summary.completedTasks} tasks done • avg $avgPercent% across " +
                    "${summary.activeGoals} goals",
                points = summary.totalPoints,
                completedTasks = summary.completedTasks,
                imageUrl = imageUrl,
                createdAt = System.currentTimeMillis(),
            )
            ref.set(dto).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not share summary", e)
        }
    }
}
