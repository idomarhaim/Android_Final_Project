package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.ChallengeDto
import com.idomarhaim.goalpilot.data.firestore.dto.ChallengeParticipantDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.Measure
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

    /**
     * The current user's own score reports: `users/{uid}/challengeReports/{challengeId}`.
     *
     * Private to them, and the fact `challenges/{id}/participants/{uid}.score` is
     * projected from (`C20` #42, spec 5.2). See [FirestorePaths.CHALLENGE_REPORTS].
     */
    private fun reportsCol(uid: String) = firestore.collection(FirestorePaths.USERS)
        .document(uid)
        .collection(FirestorePaths.CHALLENGE_REPORTS)

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
                // INCLUDE, not the default: a participants collection that is empty
                // on the server changes no documents when it stops being served
                // from cache, so without this the never-loaded state would stick.
                participantsCol(challengeId).snapshotsFlow(MetadataChanges.INCLUDE),
                // The caller's OWN fact, so the screen can say whether their score
                // moves on its own. Private to them: nobody else's link is read
                // here, and none is published on the world-readable row either.
                if (uid == null) {
                    flowOf<com.google.firebase.firestore.DocumentSnapshot?>(null)
                } else {
                    reportsCol(uid).document(challengeId).snapshotsFlow().map { it }
                },
            ) { challengeSnap, participantsSnap, myFactSnap ->
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
                    myLinkedGoalId = myFactSnap?.getString(GOAL_ID).orEmpty(),
                    // Only the participants read is stamped. The challenge document
                    // beside it is cross-boundary too, but it holds owner-authored
                    // title and dates rather than a moving number, so an as-of
                    // caption on it would answer a question nobody is asking (#50).
                    standingsFreshness = participantsSnap.crossBoundaryFreshness(),
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
        measure: Measure,
        startAtEpochMillis: Long,
        endAtEpochMillis: Long,
    ): Resource<String> = withContext(io) {
        val user = auth.currentUser ?: return@withContext Resource.Error("Not signed in")
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return@withContext Resource.Error("Give the challenge a name")
        // §6: a challenge has NO optional measure -- "there is nothing to compare
        // without a shared unit" -- and the kind is what a participant's goal is
        // matched against, so a word on its own is not enough either.
        if (measure.kind == null || measure.word.isBlank()) {
            return@withContext Resource.Error("Say what this challenge counts")
        }
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
                        measureKind = measure.kind.name,
                        measureWord = measure.word.trim().take(UNIT_MAX),
                        // The pre-§6 pair is never written, not even as a courtesy
                        // for an older client: two fields saying what one number
                        // counts is §0.3's most-repeated finding.
                        type = null,
                        metricUnit = null,
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

    override suspend fun linkGoal(challengeId: String, goalId: String): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            if (goalId.isBlank()) return@withContext Resource.Error("Pick a goal")
            try {
                // ONE FACT DOCUMENT, TWO MUTUALLY EXCLUSIVE SHAPES.
                //
                // The fact a participant owns is `users/{uid}/challengeReports/{id}`, and
                // §6 gives it a second shape: a LINK to one of their own goals, instead of
                // a typed number. `set()` without merge is what makes the exclusion
                // structural -- writing the link REPLACES the document, so the `value`
                // that was there is gone rather than shadowed.
                //
                // That matters more than tidiness. If both could sit on one fact, the
                // projection would have to pick, and the standings badge -- the whole of
                // Ido's third ask -- would be reporting the outcome of a tiebreak rather
                // than what the participant actually did. Here there is nothing to break:
                // a fact carries a `goalId` or a `value`, never both.
                //
                // The collection keeps its `challengeReports` name. Renaming a live
                // collection is a migration over every user's documents, for a word.
                reportsCol(uid).document(challengeId)
                    .set(
                        mapOf(
                            GOAL_ID to goalId,
                            LINKED_AT to System.currentTimeMillis(),
                        ),
                    ).await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not link that goal", e)
            }
        }

    override suspend fun reportScore(challengeId: String, score: Double): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            if (score < 0) return@withContext Resource.Error("A score cannot be negative")
            try {
                // THE REPORT IS THE FACT; THE STANDING IS ITS PROJECTION (`C20` #42,
                // spec §5.2).
                //
                // This used to write `score` straight onto the public participant row.
                // It cannot any more, and that is enforced rather than agreed:
                // `firestore.rules` now carries the file's first field-level condition
                // and refuses a client write that moves `score`. The number crosses the
                // ownership boundary — the people reading the standings are not the
                // person who measured it — which is precisely §5.2's test for a derived
                // number that needs a stored writer, and why this was the one quantity
                // of the map's seven that kept one.
                //
                // So the write goes to a fact the reporter owns, and
                // `functions/src/projection.ts` puts the number on the standing. Two
                // things fall out of that, both wanted:
                //
                //  * it works offline. This path is under `users/{uid}`, so it lands in
                //    the Firestore cache like any other fact; the old write went to a
                //    world-readable collection and the standing is cross-boundary data
                //    the reader is already told the age of (#50).
                //  * `set()` is safe here where it was not before. The comment this
                //    replaces warned that a merging set could create a participant row
                //    with no mirror edge — that hazard moved to the function, which uses
                //    `update()` and writes nothing at all if the row is absent.
                //
                // A report from somebody who never joined therefore no longer fails: it
                // is stored and simply projects nowhere. The join check that message
                // used to stand for lives in the UI, which only offers the field to a
                // participant.
                //
                // §6 ADDS ONE THING: this un-links. The unmerged `set` drops any `goalId`
                // that was on the fact, so typing a number is a deliberate move OFF the
                // automatic path and not a second number sitting quietly beside it. That
                // is what the standings badge is then able to say honestly.
                reportsCol(uid).document(challengeId)
                    .set(
                        mapOf(
                            VALUE to score,
                            REPORTED_AT to System.currentTimeMillis(),
                        ),
                    ).await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not report your score", e)
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

    /**
     * A brand-new participant row. `updatedAt` is left null on purpose: it is
     * annotated `@ServerTimestamp`, so Firestore fills it in on this very write —
     * see [ChallengeParticipantDto.updatedAt].
     */
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
        const val UNIT_MAX = 24
        const val JOINED_AT = "joinedAt"

        /**
         * Fields of a `users/{uid}/challengeReports/{challengeId}` fact.
         *
         * [VALUE] and [GOAL_ID] are the two shapes of the same document and never
         * appear together — see `linkGoal`. Mirrored by name in
         * `functions/src/derived.ts`; one document, two languages.
         */
        const val VALUE = "value"
        const val REPORTED_AT = "reportedAt"
        const val GOAL_ID = "goalId"
        const val LINKED_AT = "linkedAt"
    }
}
