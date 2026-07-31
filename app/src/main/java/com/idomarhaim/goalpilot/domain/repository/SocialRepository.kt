package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.SharedItem
import kotlinx.coroutines.flow.Flow

/** Sharing + friends leaderboard (spec §7, a core course requirement). */
interface SocialRepository {

    /** Global/friends leaderboard ordered by points, with rank + current-user flag. */
    fun observeLeaderboard(friendsOnly: Boolean = false): Flow<List<LeaderboardEntry>>

    /** The shared achievement feed (own + friends' posts), newest first. */
    fun observeFeed(): Flow<List<SharedItem>>

    fun observeFriendUids(): Flow<Set<String>>

    /**
     * Adds a friend by uid (used by the leaderboard rows). Fails with a message if
     * the uid has no public profile, so a bad id can never create a dangling edge.
     */
    suspend fun addFriend(uid: String): Resource<Unit>

    /** Adds a friend from the short code shown on their Profile tab (see `FriendCode`). */
    suspend fun addFriendByCode(code: String): Resource<Unit>

    suspend fun removeFriend(uid: String): Resource<Unit>

    /** Publishes a computed [ProgressSummary] (optionally with an image URL) to the feed. */
    suspend fun shareSummary(summary: ProgressSummary, imageUrl: String?): Resource<String>
}
