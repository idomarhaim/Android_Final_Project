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
 * The leaderboard as one read: its rows, plus what the read knows about itself.
 *
 * `publicProfiles` is cross-boundary — somebody else writes every row here — so
 * this is one of exactly two surfaces in v0.3 that owes an as-of caption and a
 * *"Not loaded yet"* state (#50, spec §5.3 §4). The other is challenge standings.
 */
data class Leaderboard(
    val entries: List<LeaderboardEntry> = emptyList(),
    val freshness: Freshness = Freshness(),
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
    /**
     * Whether the signed-in user wrote this post — the feed's only ownership
     * signal, and what decides whether the card offers to delete it.
     *
     * Stamped by the repository from the auth uid *flow*, exactly as
     * [LeaderboardEntry.isCurrentUser] is, and deliberately not derived in the UI
     * from the leaderboard: the leaderboard is a bounded top-N, so a user outside
     * it would silently lose the ability to delete their own posts.
     */
    val isMine: Boolean = false,
)
