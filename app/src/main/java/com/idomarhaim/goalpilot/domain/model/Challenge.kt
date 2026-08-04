package com.idomarhaim.goalpilot.domain.model

/**
 * A shared / competitive challenge between users (spec §6 nice-to-have and §7:
 * "Shared and competitive challenges (running / fitness / sleep, etc.)").
 *
 * **Participation is deliberately not a field here.** `firestore.rules` allows
 * writes to the challenge document only to its owner, so a joiner could never
 * maintain a `participantUids` array or a `standings` list stored on it — every
 * join would be denied. Participants are one document each under
 * `challenges/{id}/participants/{uid}`, which each user writes for themselves.
 * See `CHANGELOG/2026-08-04/challenges.md`.
 */
data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.CUSTOM,
    val metricUnit: String = "points",
    val ownerUid: String = "",
    val startAtEpochMillis: Long = 0L,
    val endAtEpochMillis: Long = 0L,
    val createdAtEpochMillis: Long = 0L,
)

/** One participant's self-owned row: `challenges/{challengeId}/participants/{uid}`. */
data class ChallengeParticipant(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val score: Double = 0.0,
    val joinedAtEpochMillis: Long = 0L,
)

/** A ranked participant, as the standings list renders them. */
data class ChallengeStanding(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val score: Double = 0.0,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false,
)

/** A challenge plus everything a screen needs to render it for the current user. */
data class ChallengeWithStandings(
    val challenge: Challenge = Challenge(),
    val standings: List<ChallengeStanding> = emptyList(),
    val isOwner: Boolean = false,
    val hasJoined: Boolean = false,
) {
    val participantCount: Int get() = standings.size

    /** The current user's own row, if they are in this challenge. */
    val myStanding: ChallengeStanding? get() = standings.firstOrNull { it.isCurrentUser }
}

enum class ChallengeType {
    RUNNING,
    STEPS,
    SLEEP,
    WORKOUTS,
    CUSTOM,
    ;

    companion object {
        fun fromName(name: String): ChallengeType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CUSTOM
    }
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
 * Orders participants by score (highest first) and stamps ranks.
 *
 * Unlike [rankedByPoints] on the leaderboard, this uses **standard competition
 * ranking**: equal scores share a rank and the next rank skips accordingly
 * (1, 1, 3). On a leaderboard an arbitrary tiebreak is only cosmetic, but a
 * challenge is the thing people argue about — showing two people on the same
 * score as #1 and #2 would be wrong, not merely untidy.
 *
 * `uid` breaks ties for *ordering* only, so the list is stable across snapshots.
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
            )
        }
}
