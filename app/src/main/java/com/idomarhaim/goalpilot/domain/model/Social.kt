package com.idomarhaim.goalpilot.domain.model

import com.idomarhaim.goalpilot.core.util.SummaryPeriod

/**
 * A public leaderboard row (spec §7: "A friends leaderboard by points and
 * levels"). Backed by the world-readable `publicProfiles` projection.
 */
data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val points: Long = 0L,
    val level: Int = 1,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false,
    val isFriend: Boolean = false,
)

/**
 * A shared achievement/summary posted to the friends feed (spec §7: "Sharing
 * goal-achievement summaries" and "Sharing images attached to a summary").
 */
data class SharedItem(
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val period: SummaryPeriod = SummaryPeriod.WEEKLY,
    val headline: String = "",
    val message: String = "",
    val points: Long = 0L,
    val completedTasks: Int = 0,
    val imageUrl: String? = null,
    val createdAtEpochMillis: Long = 0L,
)
