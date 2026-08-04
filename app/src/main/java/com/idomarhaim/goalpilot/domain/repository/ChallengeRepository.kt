package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import kotlinx.coroutines.flow.Flow

/**
 * Shared & competitive challenges (spec §6 nice-to-have, §7).
 *
 * Joining is not an edit to the challenge — see [Challenge] for why. Each of
 * these calls writes only documents the signed-in user is allowed to write:
 * their own participant row, and their own mirror edge.
 */
interface ChallengeRepository {

    /** Challenges the signed-in user owns or has joined, with live standings. */
    fun observeMyChallenges(): Flow<List<ChallengeWithStandings>>

    /**
     * Challenges the user could join but hasn't. Bounded, because every
     * challenge is world-readable to signed-in users and this is a browse list,
     * not a search.
     */
    fun observeDiscoverable(): Flow<List<Challenge>>

    /** A single challenge with live standings, or null once it no longer exists. */
    fun observeChallenge(challengeId: String): Flow<ChallengeWithStandings?>

    /**
     * Creates a challenge owned by the signed-in user and joins them to it —
     * an owner who is not a participant would not appear in their own standings.
     * Returns the new challenge id.
     */
    suspend fun createChallenge(
        title: String,
        description: String,
        type: ChallengeType,
        metricUnit: String,
        startAtEpochMillis: Long,
        endAtEpochMillis: Long,
    ): Resource<String>

    /** Writes the user's participant row and their `users/{uid}/challenges` mirror edge. */
    suspend fun joinChallenge(challengeId: String): Resource<Unit>

    /** Removes both. The challenge itself is untouched — leaving is not deleting. */
    suspend fun leaveChallenge(challengeId: String): Resource<Unit>

    /** Updates the user's own score in a challenge they have joined. */
    suspend fun reportScore(challengeId: String, score: Double): Resource<Unit>

    /** Owner-only. Deletes the challenge document; see the impl for what it cannot reach. */
    suspend fun deleteChallenge(challengeId: String): Resource<Unit>
}
