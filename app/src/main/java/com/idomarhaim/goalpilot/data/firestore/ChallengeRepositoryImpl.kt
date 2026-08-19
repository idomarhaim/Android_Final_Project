package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.ChallengeDto
import com.idomarhaim.goalpilot.data.firestore.dto.ChallengeParticipantDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
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

/**
 * Firestore-backed challenges.
 *
 * Three collections, and which one a write goes to is dictated by the rules:
 * - `challenges/{id}` — owner-only writes. Title, dates, type.
 * - `challenges/{id}/participants/{uid}` — each user writes their own row only.
 *   This is what makes joining possible at all; see [Challenge].
 * - `users/{uid}/challenges/{id}` — a private mirror edge, so "my challenges" is
 *   one query instead of a collection-group scan. Same shape the app already
 *   uses for friends, and already covered by the owner-only `users/{uid}` rule.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ChallengeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ChallengeRepository {

    private val challengesCol get() = firestore.collection(FirestorePaths.CHALLENGES)

    private fun challengeDoc(id: String) = challengesCol.document(id)

    private fun participantsCol(challengeId: String) =
        challengeDoc(challengeId).collection(FirestorePaths.PARTICIPANTS)

    /** The current user's mirror edges: `users/{uid}/challenges/{challengeId}`. */
    private fun myEdgesCol(uid: String) = firestore.collection(FirestorePaths.USERS)
        .document(uid)
        .collection(FirestorePaths.CHALLENGES)

    // ── Reads ──────────────────────────────────────────────────────

    override fun observeMyChallenges(): Flow<List<ChallengeWithStandings>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                myEdgesCol(uid).snapshotsFlow()
                    .map { snap -> snap.documents.map { it.id } }
                    .flatMapLatest { ids -> observeEach(ids) }
            }
        }

    /**
     * One challenge listener plus one participants listener per id. A user is in
     * a handful of challenges, so this stays cheaper and far simpler than a
     * chunked `whereIn` — and the participants listeners are needed for the
     * standings regardless, so the `whereIn` would only have merged the other half.
     */
    private fun observeEach(ids: List<String>): Flow<List<ChallengeWithStandings>> {
        if (ids.isEmpty()) return flowOf(emptyList())
        return combine(ids.map { observeChallenge(it) }) { results ->
            results.filterNotNull().sortedByDescending { it.challenge.createdAtEpochMillis }
        }
    }

    override fun observeChallenge(challengeId: String): Flow<ChallengeWithStandings?> =
        auth.uidFlow().flatMapLatest { uid ->
            combine(
                challengeDoc(challengeId).snapshotsFlow(),
                participantsCol(challengeId).snapshotsFlow(),
            ) { challengeSnap, participantsSnap ->
                val dto = challengeSnap.toObject(ChallengeDto::class.java)
                    ?: return@combine null
                val participants = participantsSnap
                    .toObjects(ChallengeParticipantDto::class.java)
                    .map { it.toDomain() }
                ChallengeWithStandings(
                    challenge = dto.toDomain(),
                    standings = participants.rankedByScore(uid),
                    isOwner = uid != null && dto.ownerUid == uid,
                    hasJoined = participants.any { it.uid == uid },
                )
            }
        }

    override fun observeDiscoverable(): Flow<List<Challenge>> =
        auth.uidFlow().flatMapLatest { uid ->
            val open = challengesCol
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(DISCOVER_LIMIT)
                .snapshotsFlow()
                .map { snap -> snap.toObjects(ChallengeDto::class.java).map { it.toDomain() } }

            if (uid == null) {
                open
            } else {
                // Exclude what the user is already in, rather than showing a
                // "Join" button that would immediately become a no-op.
                combine(open, myEdgesCol(uid).snapshotsFlow()) { challenges, edges ->
                    val mine = edges.documents.map { it.id }.toSet()
                    challenges.filter { it.id !in mine }
                }
            }
        }

    // ── Writes ─────────────────────────────────────────────────────

    override suspend fun createChallenge(
        title: String,
        description: String,
        type: ChallengeType,
        metricUnit: String,
        startAtEpochMillis: Long,
        endAtEpochMillis: Long,
    ): Resource<String> = withContext(io) {
        val user = auth.currentUser ?: return@withContext Resource.Error("Not signed in")
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return@withContext Resource.Error("Give the challenge a name")
        if (endAtEpochMillis > 0L && startAtEpochMillis > endAtEpochMillis) {
            return@withContext Resource.Error("The challenge would end before it starts")
        }
        try {
            val ref = challengesCol.document()
            val now = System.currentTimeMillis()
            // Create and join in one batch: an owner missing from their own
            // standings looks like a bug the first time anyone else joins.
            firestore.batch().apply {
                set(
                    ref,
                    ChallengeDto(
                        id = ref.id,
                        title = cleanTitle.take(TITLE_MAX),
                        description = description.trim().take(DESCRIPTION_MAX),
                        type = type.name,
                        metricUnit = metricUnit.trim().ifBlank { "points" },
                        ownerUid = user.uid,
                        startAt = startAtEpochMillis,
                        endAt = endAtEpochMillis,
                        createdAt = now,
                    ),
                )
                set(participantsCol(ref.id).document(user.uid), meAsParticipant(now))
                set(myEdgesCol(user.uid).document(ref.id), mapOf(JOINED_AT to now))
            }.commit().await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not create the challenge", e)
        }
    }

    override suspend fun joinChallenge(challengeId: String): Resource<Unit> = withContext(io) {
        val user = auth.currentUser ?: return@withContext Resource.Error("Not signed in")
        try {
            // Reject unknown ids up front, or a typo leaves a mirror edge
            // pointing at nothing that "my challenges" would try to render.
            if (!challengeDoc(challengeId).get().await().exists()) {
                return@withContext Resource.Error("That challenge no longer exists")
            }
            val now = System.currentTimeMillis()
            firestore.batch().apply {
                set(participantsCol(challengeId).document(user.uid), meAsParticipant(now))
                set(myEdgesCol(user.uid).document(challengeId), mapOf(JOINED_AT to now))
            }.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not join the challenge", e)
        }
    }

    override suspend fun leaveChallenge(challengeId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            firestore.batch().apply {
                delete(participantsCol(challengeId).document(uid))
                delete(myEdgesCol(uid).document(challengeId))
            }.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not leave the challenge", e)
        }
    }

    override suspend fun reportScore(challengeId: String, score: Double): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            if (score < 0) return@withContext Resource.Error("A score cannot be negative")
            try {
                // update(), not a merging set(): a merge would happily create a
                // participant row with no mirror edge, leaving the user scoring
                // in a challenge that never appears in their own list.
                participantsCol(challengeId).document(uid)
                    .update(SCORE, score).await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error("Join the challenge before reporting a score", e)
            }
        }

    override suspend fun deleteChallenge(challengeId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            // Firestore does not cascade. The participant rows survive as
            // orphans, and other members' mirror edges are theirs to write, not
            // ours — both are unreachable once the parent is gone, and both are
            // covered by the existing cascade-delete item in TODO → FUTURE.
            challengeDoc(challengeId).delete().await()
            myEdgesCol(uid).document(challengeId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not delete the challenge", e)
        }
    }

    private fun meAsParticipant(now: Long): ChallengeParticipantDto {
        val user = auth.currentUser
        return ChallengeParticipantDto(
            uid = user?.uid.orEmpty(),
            displayName = user?.displayName.orEmpty(),
            photoUrl = user?.photoUrl?.toString(),
            score = 0.0,
            joinedAt = now,
        )
    }

    private companion object {
        const val DISCOVER_LIMIT = 50L
        const val TITLE_MAX = 80
        const val DESCRIPTION_MAX = 240
        const val SCORE = "score"
        const val JOINED_AT = "joinedAt"
    }
}
