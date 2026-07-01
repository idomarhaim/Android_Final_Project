package com.idomarhaim.goalpilot.domain.model

/**
 * A shared / competitive challenge between users (spec §6 nice-to-have and §7:
 * "Shared and competitive challenges (running / fitness / sleep, etc.)").
 *
 * NOTE: Modeled fully but only surfaced through a stub screen in this build.
 * See TODO/TODO_OPTIONAL and feature/challenges for activation notes.
 */
data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.CUSTOM,
    val metricUnit: String = "points",
    val ownerUid: String = "",
    val participantUids: List<String> = emptyList(),
    val startAtEpochMillis: Long = 0L,
    val endAtEpochMillis: Long = 0L,
    val standings: List<ChallengeStanding> = emptyList(),
)

data class ChallengeStanding(
    val uid: String = "",
    val displayName: String = "",
    val score: Double = 0.0,
    val rank: Int = 0,
)

enum class ChallengeType {
    RUNNING,
    STEPS,
    SLEEP,
    WORKOUTS,
    CUSTOM,
}
