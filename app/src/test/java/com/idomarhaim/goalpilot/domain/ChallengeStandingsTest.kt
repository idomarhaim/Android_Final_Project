package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ScoreSource
import com.idomarhaim.goalpilot.domain.model.canBeScoredFrom
import com.idomarhaim.goalpilot.domain.model.scoringWindowFor
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
        measure = Measure(MeasureKind.DISTANCE, "km"),
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

    // ── §6: what a goal has to be to score a challenge ─────────────

    private fun goal(
        id: String = "g",
        kind: MeasureKind? = MeasureKind.DISTANCE,
        word: String = "km",
        archived: Boolean = false,
    ) = Goal(id = id, measure = Measure.of(kind, word), isArchived = archived)

    @Test
    fun `a goal of the same kind may score the challenge, whatever word it uses`() {
        val c = challenge(startAt = 0, endAt = 0)

        // The word is user content and is never translated (§1.3, §5.1 `C15b`), so
        // matching on it would keep a Hebrew user out of an English race for a reason
        // that has nothing to do with what is being counted.
        assertThat(c.canBeScoredFrom(goal(word = "km"))).isTrue()
        assertThat(c.canBeScoredFrom(goal(word = "miles"))).isTrue()
        assertThat(c.canBeScoredFrom(goal(word = "\u05e7\"\u05de"))).isTrue()
    }

    @Test
    fun `a goal of another kind, an unclassified one, and an archived one cannot`() {
        val c = challenge(startAt = 0, endAt = 0)

        assertThat(c.canBeScoredFrom(goal(kind = MeasureKind.COUNT, word = "runs"))).isFalse()
        // A pre-§1.3 goal whose word survived but whose kind nobody recorded. §1.3's rule
        // is that a null kind is *not yet answerable*, never a guess -- so it cannot score
        // a challenge either, and `C22`'s measure proposal is the route out.
        assertThat(c.canBeScoredFrom(goal(kind = null, word = "km"))).isFalse()
        assertThat(c.canBeScoredFrom(goal(archived = true))).isFalse()
    }

    @Test
    fun `a challenge with no measure can be scored from nothing`() {
        // The pre-§6 document whose `metricUnit` was the "points" default, which §6
        // deletes rather than re-homes.
        val c = Challenge(id = "legacy", measure = null)

        assertThat(c.canBeScoredFrom(goal())).isFalse()
    }

    // ── §6: the scoring window ─────────────────────────────────────

    private fun joinedAt(at: Long) = ChallengeParticipant(uid = "u", joinedAtEpochMillis = at)

    @Test
    fun `the window opens when you joined, so a year-old goal imports no history`() {
        val window = challenge(startAt = 0, endAt = 0).scoringWindowFor(joinedAt(500))

        assertThat(window.includes(499)).isFalse()
        assertThat(window.includes(500)).isTrue()
        assertThat(window.includes(10_000)).isTrue()
    }

    @Test
    fun `joining an upcoming challenge does not credit it with what you did first`() {
        // THE DERIVED HALF, and it is not §6's own words -- §6 says "since you joined".
        // On its own that credits a September race with August's walking whenever
        // somebody joins one that has not started, which `ChallengePhase.UPCOMING` makes
        // an ordinary thing to do. Taken from the phase model's position that a
        // challenge's dates say when it is being run; see `ScoringWindow`'s KDoc.
        val window = challenge(startAt = 1_000, endAt = 0).scoringWindowFor(joinedAt(500))

        assertThat(window.fromEpochMillis).isEqualTo(1_000)
        assertThat(window.includes(999)).isFalse()
        assertThat(window.includes(1_000)).isTrue()
    }

    @Test
    fun `the window closes at the end bound, so an ended challenge stops moving`() {
        // Half-open, and the same instant convention `phaseAt` uses. `canReportScore` is
        // already false once a challenge is ENDED, so a DERIVED score that kept climbing
        // afterwards would be two halves of one product rule disagreeing.
        val window = challenge(startAt = 0, endAt = 2_000).scoringWindowFor(joinedAt(500))

        assertThat(window.includes(1_999)).isTrue()
        assertThat(window.includes(2_000)).isFalse()
    }

    @Test
    fun `an open-ended challenge has no upper bound at all`() {
        val window = challenge(startAt = 0, endAt = 0).scoringWindowFor(joinedAt(0))

        assertThat(window.untilEpochMillis).isNull()
        assertThat(window.includes(Long.MAX_VALUE)).isTrue()
    }

    // ── §6 / Ido's third ask: provenance rides the standing ────────

    @Test
    fun `provenance rides onto the standing and never re-ranks it`() {
        val ranked = listOf(
            ChallengeParticipant(
                uid = "typed",
                score = 100.0,
                source = ScoreSource.REPORTED,
                reportedAtEpochMillis = 42L,
            ),
            ChallengeParticipant(uid = "derived", score = 90.0, source = ScoreSource.DERIVED),
        ).rankedByScore()

        // A typed score sorts exactly where its value puts it. §6 makes the number
        // server-owned and this makes it LABELLED; neither is a penalty, and ranking one
        // below the other would be the app asserting something it cannot know.
        assertThat(ranked.map { it.uid }).containsExactly("typed", "derived").inOrder()
        assertThat(ranked[0].isReported).isTrue()
        assertThat(ranked[0].reportedAtEpochMillis).isEqualTo(42L)
        assertThat(ranked[1].isReported).isFalse()
        assertThat(ranked[1].reportedAtEpochMillis).isEqualTo(0L)
    }
}
