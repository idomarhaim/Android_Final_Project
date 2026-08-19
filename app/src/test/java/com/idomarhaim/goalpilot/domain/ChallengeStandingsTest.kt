package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import org.junit.Test

/** Domain (unit) tests for challenge standings and phase (spec §6 nice-to-have, §7). */
class ChallengeStandingsTest {

    private fun participant(uid: String, score: Double) =
        ChallengeParticipant(uid = uid, displayName = uid, score = score)

    @Test
    fun `orders by score descending and stamps 1-based ranks`() {
        val ranked = listOf(
            participant("b", 12.0),
            participant("a", 30.0),
            participant("c", 21.0),
        ).rankedByScore()

        assertThat(ranked.map { it.uid }).containsExactly("a", "c", "b").inOrder()
        assertThat(ranked.map { it.rank }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `equal scores share a rank and the next rank skips`() {
        // Standard competition ranking. This is the one place the challenge
        // deliberately differs from the leaderboard: two people on the same score
        // are joint first, not first and second.
        val ranked = listOf(
            participant("a", 30.0),
            participant("b", 30.0),
            participant("c", 10.0),
        ).rankedByScore()

        assertThat(ranked.map { it.rank }).containsExactly(1, 1, 3).inOrder()
    }

    @Test
    fun `a three-way tie still leaves the next competitor at rank four`() {
        val ranked = listOf(
            participant("a", 5.0),
            participant("b", 5.0),
            participant("c", 5.0),
            participant("d", 1.0),
        ).rankedByScore()

        assertThat(ranked.map { it.rank }).containsExactly(1, 1, 1, 4).inOrder()
    }

    @Test
    fun `ties order deterministically so rows do not jump between snapshots`() {
        val first = listOf(participant("z", 10.0), participant("a", 10.0)).rankedByScore()
        val second = listOf(participant("a", 10.0), participant("z", 10.0)).rankedByScore()

        assertThat(first.map { it.uid }).isEqualTo(second.map { it.uid })
    }

    @Test
    fun `the current user is flagged, and only them`() {
        val ranked = listOf(
            participant("me", 8.0),
            participant("them", 9.0),
        ).rankedByScore(currentUid = "me")

        assertThat(ranked.single { it.isCurrentUser }.uid).isEqualTo("me")
    }

    @Test
    fun `nobody is the current user when signed out`() {
        val ranked = listOf(participant("a", 1.0)).rankedByScore(currentUid = null)

        assertThat(ranked.none { it.isCurrentUser }).isTrue()
    }

    @Test
    fun `a participant who has not scored still appears`() {
        // Joining writes score 0. Dropping zero-score rows would make a challenge
        // look empty until someone reported, and hide that you had joined at all.
        val ranked = listOf(participant("a", 0.0), participant("b", 4.0)).rankedByScore()

        assertThat(ranked).hasSize(2)
        assertThat(ranked.last().uid).isEqualTo("a")
    }

    @Test
    fun `empty input stays empty`() {
        assertThat(emptyList<ChallengeParticipant>().rankedByScore()).isEmpty()
    }

    // ── Phase ──────────────────────────────────────────────────────

    private fun challenge(startAt: Long, endAt: Long) = Challenge(
        id = "c",
        title = "7-day run streak",
        type = ChallengeType.RUNNING,
        startAtEpochMillis = startAt,
        endAtEpochMillis = endAt,
    )

    @Test
    fun `a challenge before its start date is upcoming`() {
        assertThat(challenge(startAt = 100, endAt = 200).phaseAt(50))
            .isEqualTo(ChallengePhase.UPCOMING)
    }

    @Test
    fun `a challenge inside its window is active`() {
        assertThat(challenge(startAt = 100, endAt = 200).phaseAt(150))
            .isEqualTo(ChallengePhase.ACTIVE)
    }

    @Test
    fun `the end instant itself is already ended`() {
        // Half-open, matching AnalyticsRange: an instant belongs to exactly one phase.
        assertThat(challenge(startAt = 100, endAt = 200).phaseAt(200))
            .isEqualTo(ChallengePhase.ENDED)
    }

    @Test
    fun `the start instant itself is already active`() {
        assertThat(challenge(startAt = 100, endAt = 200).phaseAt(100))
            .isEqualTo(ChallengePhase.ACTIVE)
    }

    @Test
    fun `a challenge with no dates is open-ended, not expired in 1970`() {
        assertThat(challenge(startAt = 0, endAt = 0).phaseAt(1_700_000_000_000))
            .isEqualTo(ChallengePhase.ACTIVE)
    }

    @Test
    fun `an open-ended challenge with only a start date still starts`() {
        val c = challenge(startAt = 100, endAt = 0)

        assertThat(c.phaseAt(50)).isEqualTo(ChallengePhase.UPCOMING)
        assertThat(c.phaseAt(9_999)).isEqualTo(ChallengePhase.ACTIVE)
    }
}
