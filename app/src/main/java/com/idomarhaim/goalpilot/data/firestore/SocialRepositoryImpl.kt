package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.PublicProfileDto
import com.idomarhaim.goalpilot.data.firestore.dto.SharedItemDto
import com.idomarhaim.goalpilot.data.firestore.dto.resolvedLevel
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.FriendCode
import com.idomarhaim.goalpilot.domain.model.Leaderboard
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.model.merge
import com.idomarhaim.goalpilot.domain.model.rankedByPoints
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    // A share and the photo it carries die together, so one repository owns both
    // halves. Injecting the domain interface keeps this a data-to-domain edge and
    // introduces no cycle — nothing in storage knows about the feed.
    private val storage: StorageRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SocialRepository {

    private val publicCol get() = firestore.collection(FirestorePaths.PUBLIC_PROFILES)
    private val sharesCol get() = firestore.collection(FirestorePaths.SHARES)
    private fun friendsCol(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.FRIENDS)

    /**
     * "Everyone" reads the global top [LEADERBOARD_LIMIT] by points.
     *
     * "Friends" instead fetches the friends' profiles **by document id**. Filtering
     * the global top-N client-side would silently drop any friend who sits outside
     * it — exactly the friend a new user is most likely to have just added.
     */
    override fun observeLeaderboard(friendsOnly: Boolean): Flow<Leaderboard> =
        auth.uidFlow().flatMapLatest { uid ->
            observeFriendUids().flatMapLatest { friends ->
                val profiles = if (friendsOnly) {
                    observeProfilesByIds(friends + setOfNotNull(uid))
                } else {
                    publicCol
                        .orderBy("points", Query.Direction.DESCENDING)
                        .limit(LEADERBOARD_LIMIT)
                        // INCLUDE, not the default: without it an empty
                        // `publicProfiles` would render "Not loaded yet" and never
                        // leave it. See crossBoundaryFreshness.
                        .snapshotsFlow(MetadataChanges.INCLUDE)
                        .map { snap ->
                            ProfileRead(
                                snap.toObjects(PublicProfileDto::class.java),
                                snap.crossBoundaryFreshness(),
                            )
                        }
                }
                profiles.map { read ->
                    Leaderboard(
                        entries = read.profiles.toEntries(uid, friends).rankedByPoints(),
                        freshness = read.freshness,
                    )
                }
            }
        }

    /** One `publicProfiles` read: its rows, and what the snapshot knows about itself. */
    private data class ProfileRead(
        val profiles: List<PublicProfileDto>,
        val freshness: Freshness,
    )

    /**
     * Streams the given profiles. Firestore's `in` filter caps out at 30 values,
     * so larger friend lists are split across parallel listeners and merged.
     *
     * No ids at all is a genuine empty rather than a never-loaded one: the friend
     * set comes from `users/{uid}/friends`, which the reader owns and which is
     * therefore complete offline. "You have no friends yet" is a fact this device
     * holds; "we have never seen anyone's profile" is not.
     */
    private fun observeProfilesByIds(ids: Set<String>): Flow<ProfileRead> {
        val chunks = ids.filter { it.isNotBlank() }.chunked(IN_QUERY_LIMIT)
        if (chunks.isEmpty()) return flowOf(ProfileRead(emptyList(), Freshness()))
        val flows = chunks.map { chunk ->
            publicCol.whereIn(FieldPath.documentId(), chunk)
                .snapshotsFlow(MetadataChanges.INCLUDE)
                .map { snap ->
                    ProfileRead(
                        snap.toObjects(PublicProfileDto::class.java),
                        snap.crossBoundaryFreshness(),
                    )
                }
        }
        return combine(flows) { reads ->
            ProfileRead(
                profiles = reads.flatMap { it.profiles },
                freshness = reads.map { it.freshness }.merge(),
            )
        }
    }

    private fun List<PublicProfileDto>.toEntries(
        uid: String?,
        friends: Set<String>,
    ): List<LeaderboardEntry> = map { p ->
        LeaderboardEntry(
            uid = p.uid,
            displayName = p.displayName,
            photoUrl = p.photoUrl,
            points = p.points,
            level = p.resolvedLevel(),
            isCurrentUser = p.uid == uid,
            isFriend = p.uid in friends,
        )
    }

    /**
     * The feed, with each item stamped [SharedItem.isMine] against the signed-in
     * uid — the flag the card needs to decide whether to offer a delete.
     *
     * Built on [uidFlow] rather than a one-shot `auth.currentUser`: this Flow is
     * constructed once at ViewModel-creation time, so a one-shot read would pin
     * whichever account happened to be signed in then and go on marking that
     * account's posts as the current user's after a switch.
     */
    override fun observeFeed(): Flow<List<SharedItem>> =
        auth.uidFlow().flatMapLatest { uid ->
            sharesCol
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(FEED_LIMIT)
                .snapshotsFlow()
                .map { snap ->
                    snap.toObjects(SharedItemDto::class.java).map { dto ->
                        dto.toDomain().copy(isMine = uid != null && dto.authorUid == uid)
                    }
                }
        }

    override fun observeFriendUids(): Flow<Set<String>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptySet())
            } else {
                friendsCol(uid).snapshotsFlow()
                    .map { snap -> snap.documents.map { it.id }.toSet() }
            }
        }

    override suspend fun addFriend(uid: String): Resource<Unit> = withContext(io) {
        val me = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        if (uid.isBlank()) return@withContext Resource.Error("Enter a friend code")
        if (uid == me) return@withContext Resource.Error("You cannot add yourself")
        try {
            // Reject unknown ids up front — otherwise a typo writes a friend edge
            // that points at nobody and can never show up on the leaderboard.
            if (!publicCol.document(uid).get().await().exists()) {
                return@withContext Resource.Error("No GoalPilot user with that code")
            }
            friendsCol(me).document(uid)
                .set(mapOf("addedAt" to System.currentTimeMillis())).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not add friend", e)
        }
    }

    override suspend fun addFriendByCode(code: String): Resource<Unit> = withContext(io) {
        val normalized = FriendCode.normalize(code)
        if (!FriendCode.isValid(normalized)) {
            return@withContext Resource.Error(
                "A friend code is ${FriendCode.LENGTH} characters, e.g. 7KQ4RD",
            )
        }
        try {
            val match = publicCol.whereEqualTo("friendCode", normalized).limit(1).get().await()
            val uid = match.documents.firstOrNull()?.id
                ?: return@withContext Resource.Error("No GoalPilot user with that code")
            addFriend(uid)
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

    override suspend fun deleteShare(shareId: String, imageUrl: String?): Resource<Unit> =
        withContext(io) {
            val me = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            if (shareId.isBlank()) return@withContext Resource.Error("That post no longer exists")
            try {
                val snap = sharesCol.document(shareId).get().await()
                // Already deleted — by the same user on another device, or by a
                // half-finished earlier attempt. Reporting a failure here would
                // invite a retry that can only fail again, over a post that is
                // gone; the user's goal already holds.
                if (!snap.exists()) return@withContext Resource.Success(Unit)
                if (snap.getString("authorUid") != me) {
                    return@withContext Resource.Error("You can only delete your own posts")
                }
                sharesCol.document(shareId).delete().await()

                // Post first, photo second, and the order is the whole argument.
                // What the user asked for is that the post stop existing, so that
                // is what must not be left undone: a failed image cleanup leaves an
                // orphan nobody can see, while the other order can leave a visible
                // post pointing at a photo that has already been deleted.
                if (!imageUrl.isNullOrBlank()) storage.deleteImage(imageUrl)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not delete post", e)
            }
        }

    private companion object {
        const val LEADERBOARD_LIMIT = 100L
        const val FEED_LIMIT = 50L

        /** Firestore's hard cap on values in an `in` / `whereIn` filter. */
        const val IN_QUERY_LIMIT = 30
    }
}
