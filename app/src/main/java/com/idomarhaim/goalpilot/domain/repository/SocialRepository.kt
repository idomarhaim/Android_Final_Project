package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Leaderboard
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.SharedItem
import kotlinx.coroutines.flow.Flow

/** Sharing + friends leaderboard (spec §7, a core course requirement). */
interface SocialRepository {

    /**
     * Global/friends leaderboard ordered by points, with rank + current-user flag.
     *
     * Returns a [Leaderboard] rather than a bare list because `publicProfiles` is
     * cross-boundary: the caption and the *"Not loaded yet"* state both need what
     * the **read** knows, and only this layer ever sees the snapshot (#50).
     */
    fun observeLeaderboard(friendsOnly: Boolean = false): Flow<Leaderboard>

    /**
     * The shared achievement feed (own + friends' posts), newest first, each item
     * stamped with [SharedItem.isMine] against the signed-in uid.
     */
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

    /**
     * Deletes a post the signed-in user authored, **together with the image it
     * carried** — the photo must not outlive the share that referenced it, and
     * nothing else in the app would ever reach it again.
     *
     * Author-only, and enforced twice on purpose: `firestore.rules` is the half
     * that actually holds (a client can be modified), and the check here is the
     * half that can say *why* in a sentence instead of `PERMISSION_DENIED`.
     *
     * Deleting a post that is already gone succeeds — see [deleteShare]'s impl for
     * why that is the honest answer rather than a swallowed failure.
     */
    suspend fun deleteShare(shareId: String, imageUrl: String? = null): Resource<Unit>
}
