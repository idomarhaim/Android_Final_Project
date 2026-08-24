package com.idomarhaim.goalpilot.domain.model

/**
 * A shared / competitive challenge between users — `docs/PRODUCT_v0.3.md` §6
 * (`C14` [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23)).
 *
 * > **A challenge scores from nothing of its own: it scores from each
 * > participant's goal.**
 *
 * **Participation is deliberately not a field here.** `firestore.rules` allows
 * writes to the challenge document only to its owner, so a joiner could never
 * maintain a `participantUids` array or a `standings` list stored on it — every
 * join would be denied. Participants are one document each under
 * `challenges/{id}/participants/{uid}`, which each user writes for themselves.
 * See `CHANGELOG/2026-08-04/challenges.md`.
 *
 * ### What §6 changed here, and why the shape is a [Measure]
 *
 * Put a goal and a challenge side by side after `C4` and `C7` and they are the
 * same object — a title, a measure, a start, a current value. So `R1`'s *"a
 * shared challenge does not sync with my tasks or with Health Connect"* was never
 * missing wiring: the app was keeping **two representations of the same walk**,
 * and the fix deletes one rather than building a second pipe into it. A
 * participant links one of their own goals, everything that already feeds a goal
 * — Health Connect, a completed task, a manual log — feeds the challenge for
 * free, and `ProgressEntry.sourceKey` already stops a re-sync counting twice.
 *
 * That is why `metricUnit` and `ChallengeType` are both gone. The free-text unit
 * had `Goal.unit`'s exact disease (`A3`, `C7` #14) and its `"points"` default is
 * what produced the ticket; the type was **purely presentational** — nothing ever
 * branched on it to source a score, and a [MeasureKind] now covers the part of it
 * that meant anything (`STEPS` is `COUNT`, `SLEEP` is `DURATION`, `RUNNING` is
 * `DISTANCE`).
 */
data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    /**
     * What every participant's score is expressed in — §6's *"a challenge names a
     * measure"*.
     *
     * **Nullable in the model, never optional in the product**, and the two are
     * not in tension. §6 says a challenge has **no optional measure** — "there is
     * nothing to compare without a shared unit" — and `ChallengeRepository`'s
     * create path enforces exactly that: a challenge cannot be created without
     * one. Null is reserved for a **document written before §6** whose legacy
     * `metricUnit` was the `"points"` default, which §6 **deletes rather than
     * re-homes** because points rank by *time logged*, and a race for who logged
     * the most minutes is not the race anybody entered.
     *
     * So null means *this challenge predates the measure and its owner has to say
     * what it counts* — see [isUnmeasured], and `ChallengeDto.resolvedMeasure` for
     * what the migration can and cannot recover.
     */
    val measure: Measure? = null,
    val ownerUid: String = "",
    val startAtEpochMillis: Long = 0L,
    val endAtEpochMillis: Long = 0L,
    val createdAtEpochMillis: Long = 0L,
) {
    /** The user's own word for the unit — `"steps"`, `"km"` — or empty. */
    val metricWord: String get() = measure?.word.orEmpty()

    /**
     * A pre-§6 challenge with nothing to compare on. It cannot be scored from a
     * goal, because there is no kind to match a goal against, and it cannot be
     * joined until its owner says what it counts.
     */
    val isUnmeasured: Boolean get() = measure?.kind == null
}

/**
 * Where a participant's published [ChallengeParticipant.score] came from — Ido's
 * own third ask on `#23`, 2026-08-24:
 *
 * > *"If someone updated manually and it was not updated through HEALTH CONNECT,
 * > then it should say there who performed the update and what they updated."*
 *
 * On a shared leaderboard a **reading** and a **typed number** are not the same
 * claim, and before this they were pixel identical. They are still not ranked
 * differently and nothing here accuses anybody: this is a **label on a number**,
 * not an attestation about a person.
 */
enum class ScoreSource {
    /**
     * Nothing has been scored yet — the participant has neither linked a goal nor
     * typed a number. The honest state of a row that has just joined, and the
     * default so that a missing field never reads as a claim.
     */
    NONE,

    /**
     * Summed by the server from the participant's linked goal. §6's intended
     * path, and the one that needs **no badge**: it is what a standings row means
     * by default.
     */
    DERIVED,

    /** Typed by the participant. The exception, and the one that says so. */
    REPORTED,
    ;

    companion object {
        fun fromName(name: String?): ScoreSource =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
    }
}

/**
 * One participant's self-owned row: `challenges/{challengeId}/participants/{uid}`.
 *
 * **The identity fields are the participant's; the number and its provenance are
 * the server's.** `firestore.rules` pins [score], [source] and
 * [reportedAtEpochMillis] against every client write — they are projected by
 * `functions/src/projection.ts` from a fact the participant owns at
 * `users/{uid}/challengeReports/{challengeId}` (`C20` #42, spec §5.2).
 *
 * ⚠️ **[source] is self-asserted by construction, and this KDoc says so on
 * purpose.** A participant writes only their own fact, so *reported* means *this
 * person typed this number* and *derived* means *this person pointed the
 * challenge at a goal of theirs*. Neither is evidence about the walk itself:
 * §6's own honest residual is that server-owned scoring stops a win being
 * **typed**, not a reading being **forged**. The badge is a **label**, never an
 * attestation — do not let a surface imply otherwise.
 */
data class ChallengeParticipant(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val score: Double = 0.0,
    val joinedAtEpochMillis: Long = 0L,
    /** Server-written. See [ScoreSource]. */
    val source: ScoreSource = ScoreSource.NONE,
    /**
     * When the typed number behind [score] was reported, or `0` when it was not
     * typed. Server-written, and **`0` whenever [source] is not
     * [ScoreSource.REPORTED]** — so the badge never has a date to show for a
     * number nobody typed.
     */
    val reportedAtEpochMillis: Long = 0L,
)

/** A ranked participant, as the standings list renders them. */
data class ChallengeStanding(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val score: Double = 0.0,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false,
    val source: ScoreSource = ScoreSource.NONE,
    val reportedAtEpochMillis: Long = 0L,
) {
    /**
     * Whether this row carries a **typed** number, and so whether the standings
     * draw a badge on it.
     *
     * **The absence of a badge is the honest default**, which is the way round
     * the brief asked for: `DERIVED` is what the product means a standing to be,
     * `NONE` is a row nobody has scored yet, and neither is remarkable. Only a
     * number somebody typed is.
     */
    val isReported: Boolean get() = source == ScoreSource.REPORTED
}

/** A challenge plus everything a screen needs to render it for the current user. */
data class ChallengeWithStandings(
    val challenge: Challenge = Challenge(),
    val standings: List<ChallengeStanding> = emptyList(),
    val isOwner: Boolean = false,
    val hasJoined: Boolean = false,
    /**
     * The goal the **current user** has pointed this challenge at, or empty when
     * they have not linked one.
     *
     * Read from their own fact at `users/{uid}/challengeReports/{challengeId}`,
     * which is private to them — a participant sees **their own** link and never
     * anybody else's. Publishing it would put a goal's identity on a
     * world-readable row for a badge that only ever needed to say *derived*.
     */
    val myLinkedGoalId: String = "",
    /**
     * What the *participants* read knows about itself — every row of it is written
     * by somebody else (#50, spec §5.3 §4). The challenge document beside it gets
     * no stamp: its title and dates are owner-authored prose, not a derived number
     * anyone can be misled about.
     */
    val standingsFreshness: Freshness = Freshness(),
) {
    val participantCount: Int get() = standings.size

    /** The current user's own row, if they are in this challenge. */
    val myStanding: ChallengeStanding? get() = standings.firstOrNull { it.isCurrentUser }

    /** Whether the current user's score moves on its own — §6's intended state. */
    val myScoreIsLinked: Boolean get() = myLinkedGoalId.isNotBlank()
}

/** Where a challenge sits relative to now — drives what the UI offers to do. */
enum class ChallengePhase { UPCOMING, ACTIVE, ENDED }

/**
 * A challenge with no dates set (both zero) counts as [ChallengePhase.ACTIVE]:
 * "no deadline" is an open-ended challenge, not one that ended in 1970.
 */
fun Challenge.phaseAt(nowEpochMillis: Long): ChallengePhase = when {
    startAtEpochMillis > 0L && nowEpochMillis < startAtEpochMillis -> ChallengePhase.UPCOMING
    endAtEpochMillis > 0L && nowEpochMillis >= endAtEpochMillis -> ChallengePhase.ENDED
    else -> ChallengePhase.ACTIVE
}

/**
 * The window a participant's movement is counted over — the arithmetic behind
 * §6's *"the score is **movement since you joined**, not the goal's current
 * value"*.
 *
 * Without it a weight-loss race ranks by **who is heaviest**, and joining with a
 * year-old goal imports a year of history nobody raced for.
 *
 * ⚠️ **The lower bound is `max(joinedAt, startAt)`, and the `startAt` half is a
 * DERIVATION, not §6's words.** §6 says *since you joined*, and on its own that
 * would credit a September race with August's walking whenever somebody joins a
 * challenge that has not started yet — which `ChallengePhase.UPCOMING` makes an
 * ordinary thing to do, not a corner case. Taken per the derivable-decision rule
 * from the two things already committed: §6's *movement*, and the phase model's
 * position that a challenge's dates say when it is being run. Recorded here and
 * in `CHANGELOG/2026-08-24/challenge-scoring.md` so it can be overturned in one
 * place if Ido reads it the other way.
 *
 * The upper bound is `endAt` for the same reason and with the same authority —
 * `canReportScore` already refuses a typed score once a challenge is `ENDED`, so
 * a *derived* score that kept climbing afterwards would be the two halves of one
 * product rule disagreeing.
 */
data class ScoringWindow(val fromEpochMillis: Long, val untilEpochMillis: Long?) {
    /** Whether an entry stamped [atEpochMillis] falls inside the window. */
    fun includes(atEpochMillis: Long): Boolean =
        atEpochMillis >= fromEpochMillis &&
            (untilEpochMillis == null || atEpochMillis < untilEpochMillis)
}

/** The window this challenge scores [participant] over. See [ScoringWindow]. */
fun Challenge.scoringWindowFor(participant: ChallengeParticipant): ScoringWindow =
    ScoringWindow(
        fromEpochMillis = maxOf(participant.joinedAtEpochMillis, startAtEpochMillis),
        untilEpochMillis = endAtEpochMillis.takeIf { it > 0L },
    )

/**
 * Whether [goal] may be linked to this challenge — §6's *"each participant links
 * one of their own goals of the same kind"*.
 *
 * **The kind, not the word.** Two people racing on steps may have written
 * `"steps"` and `"צעדים"`, and §1.3 says the word is user content that is never
 * translated; matching on it would exclude a Hebrew user from an English
 * challenge for a reason that has nothing to do with what is being counted. The
 * **kind** is the closed, app-owned half, and it is what fixes the arithmetic.
 *
 * An **archived** goal is excluded: it is a goal the user has put away, and
 * pointing a live race at it would keep it silently scoring.
 */
fun Challenge.canBeScoredFrom(goal: Goal): Boolean {
    val kind = measure?.kind ?: return false
    return !goal.isArchived && goal.measure?.kind == kind
}

/**
 * Orders participants by score (highest first) and stamps ranks.
 *
 * Unlike `rankedByPoints` on the leaderboard, this uses **standard competition
 * ranking**: equal scores share a rank and the next rank skips accordingly
 * (1, 1, 3). On a leaderboard an arbitrary tiebreak is only cosmetic, but a
 * challenge is the thing people argue about — showing two people on the same
 * score as #1 and #2 would be wrong, not merely untidy.
 *
 * `uid` breaks ties for *ordering* only, so the list is stable across snapshots.
 *
 * **Provenance rides along and never re-ranks.** A typed score and a derived one
 * sort identically: §6 makes the number server-owned and this makes it
 * *labelled*, and neither is a penalty.
 */
fun List<ChallengeParticipant>.rankedByScore(
    currentUid: String? = null,
): List<ChallengeStanding> {
    var previousScore: Double? = null
    var previousRank = 0
    return sortedWith(compareByDescending<ChallengeParticipant> { it.score }.thenBy { it.uid })
        .mapIndexed { index, participant ->
            val rank = if (participant.score == previousScore) {
                previousRank
            } else {
                (index + 1).also { previousRank = it }
            }
            previousScore = participant.score
            ChallengeStanding(
                uid = participant.uid,
                displayName = participant.displayName,
                photoUrl = participant.photoUrl,
                score = participant.score,
                rank = rank,
                isCurrentUser = participant.uid == currentUid,
                source = participant.source,
                reportedAtEpochMillis = participant.reportedAtEpochMillis,
            )
        }
}
