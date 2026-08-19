package com.idomarhaim.goalpilot.domain.model

/**
 * What a read of **somebody else's** collection knows about itself (#50, spec
 * §5.3).
 *
 * > Staleness is a property of the *data*, not of the *connection*.
 *
 * A leaderboard fetched forty minutes ago over perfect Wi-Fi is exactly as old as
 * one served from cache with the radio off, so neither of these two facts is
 * about the radio and nothing here asks the OS about it. Both come off the
 * Firestore snapshot itself.
 *
 * It rides only the **cross-boundary** reads — `publicProfiles/{uid}` and
 * `challenges/{id}/participants/{uid}`, the two collections `firestore.rules`
 * lets somebody other than `isOwner(uid)` read. Owner-side data is complete and
 * correct offline and is deliberately not wrapped in this.
 */
data class Freshness(
    /**
     * The **newest** `updatedAt` across the rows we are holding, or `0L` if not one
     * of them carries a stamp.
     *
     * Newest, not oldest, because that is what "as of" claims: *nothing here
     * reflects a write after this time*. Each row's stamp says when its owner last
     * wrote **the copy we hold** — it never promises they have not written since,
     * which is exactly why the caption is a caption and not a freshness guarantee.
     */
    val asOfEpochMillis: Long = 0L,

    /**
     * The read came back **empty and from cache** — this device has never actually
     * seen the collection, so the app does not know whether there is anything in
     * it.
     *
     * Distinct from an ordinary empty, and that distinction is the whole of #50
     * item 3: rendering *"No friends"* here would be the app stating a fact about
     * someone else's data it has never read.
     */
    val neverLoaded: Boolean = false,
) {
    /** Whether there is a time to put in a caption at all. */
    val hasStamp: Boolean get() = asOfEpochMillis > 0L
}

/**
 * Folds the per-listener readings of one logical list into one.
 *
 * The friends leaderboard splits across parallel listeners because Firestore's
 * `whereIn` caps at 30 ids, and the split is an implementation detail the caption
 * must not leak: the newest stamp anywhere in the list is the list's stamp, and
 * the list counts as never-loaded only when **every** chunk came back empty from
 * cache — one chunk that reached the server proves the device has seen this data.
 *
 * An empty receiver is *not* never-loaded: no listener at all is what "you have
 * no friends" looks like, and that is owner-side data the device genuinely owns.
 */
fun List<Freshness>.merge(): Freshness = Freshness(
    asOfEpochMillis = maxOfOrNull { it.asOfEpochMillis } ?: 0L,
    neverLoaded = isNotEmpty() && all { it.neverLoaded },
)
