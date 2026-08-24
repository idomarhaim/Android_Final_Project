package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.Measure
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
     *
     * [measure] is **required and must carry a kind**: §6 gives a challenge no
     * optional measure, because there is nothing to compare without a shared
     * unit, and the kind is what a participant's goal is matched against.
     */
    suspend fun createChallenge(
        title: String,
        description: String,
        measure: Measure,
        startAtEpochMillis: Long,
        endAtEpochMillis: Long,
    ): Resource<String>

    /** Writes the user's participant row and their `users/{uid}/challenges` mirror edge. */
    suspend fun joinChallenge(challengeId: String): Resource<Unit>

    /** Removes both. The challenge itself is untouched — leaving is not deleting. */
    suspend fun leaveChallenge(challengeId: String): Resource<Unit>

    /**
     * Points this challenge at one of the user's own goals — §6's *"joining links
     * or creates a goal"*, and the whole of what makes a challenge's number move
     * on its own.
     *
     * It writes the **fact**, never the score: `users/{uid}/challengeReports/{challengeId}`
     * gains a `goalId` and loses any typed `value`, and `projectChallengeScore`
     * sums the goal's progress over the participant's scoring window. Linking and
     * typing are therefore **mutually exclusive by construction** — which is what
     * makes the standings badge answerable at all, rather than a guess about
     * which of two numbers won.
     */
    suspend fun linkGoal(challengeId: String, goalId: String): Resource<Unit>

    /**
     * Types an absolute score for a challenge the user has joined, and **clears
     * any goal link**.
     *
     * This is the path that earns a badge on the standings row (`ScoreSource
     * .REPORTED`): the number is the participant's own claim rather than a sum of
     * their logged progress, and §6's honest residual is that server-owned
     * scoring stops a win being typed only once somebody has said which of the
     * two this is.
     */
    suspend fun reportScore(challengeId: String, score: Double): Resource<Unit>

    /** Owner-only. Deletes the challenge document; see the impl for what it cannot reach. */
    suspend fun deleteChallenge(challengeId: String): Resource<Unit>
}
