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
 * Orders leaderboard rows by points (highest first) and stamps 1-based ranks.
 *
 * Ranking happens *after* any friends-only filtering, so the "Friends" tab shows
 * #1..#n among friends rather than each friend's global position.
 */
fun List<LeaderboardEntry>.rankedByPoints(): List<LeaderboardEntry> =
    sortedWith(compareByDescending<LeaderboardEntry> { it.points }.thenBy { it.uid })
        .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

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
