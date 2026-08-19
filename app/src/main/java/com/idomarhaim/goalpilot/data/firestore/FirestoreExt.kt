package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.idomarhaim.goalpilot.domain.model.Freshness
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Emits query snapshots as a cold [Flow], removing the listener on cancellation.
 *
 * [metadataChanges] defaults to [MetadataChanges.EXCLUDE], which is right for
 * owner-side reads: the rows are what matters and a pending-write flag flipping
 * is not news. **Cross-boundary reads must pass [MetadataChanges.INCLUDE]** — see
 * [crossBoundaryFreshness] for the case that goes wrong without it.
 */
fun Query.snapshotsFlow(
    metadataChanges: MetadataChanges = MetadataChanges.EXCLUDE,
): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener(metadataChanges) { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** Emits document snapshots as a cold [Flow], removing the listener on cancellation. */
fun DocumentReference.snapshotsFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/**
 * The server-set as-of stamp carried by the two **cross-boundary** collections,
 * `publicProfiles/{uid}` and `challenges/{id}/participants/{uid}` (#50, spec
 * §5.3 §3). Named once here because both the writers and [crossBoundaryFreshness]
 * have to agree on it.
 */
const val UPDATED_AT = "updatedAt"

/**
 * What this snapshot knows about **itself** — for a collection somebody other
 * than the reader writes.
 *
 * Both facts come off the snapshot and neither asks the OS about the radio; see
 * [Freshness] for why that is the whole point. Read straight off the documents
 * rather than off a parsed DTO so that a row written before #50 shipped, and a
 * row whose `serverTimestamp()` is still pending, both simply contribute nothing
 * instead of contributing a zero that would win a `min` and lose a `max`.
 *
 * Only ever call this on a cross-boundary read. On owner-side data it would be
 * noise: the reader is the writer, so after `C20` that data is complete and
 * correct offline and there is nothing to be as-of about.
 *
 * ### The listener must be registered with [MetadataChanges.INCLUDE]
 *
 * `EXCLUDE` raises no event when **only** metadata changes — and an empty result
 * set that goes from cache-served to server-confirmed changes no documents, so it
 * is exactly such an event. Without `INCLUDE`, a collection that is genuinely
 * empty on the server would be read once from cache, render *"Not loaded yet"*,
 * and stay there until somebody else wrote a document. The never-loaded state
 * would then be a trap rather than a transient, which is the opposite of what it
 * is for. Nothing downstream pays for the extra emissions: they carry an equal
 * value, and `StateFlow` drops those.
 */
fun QuerySnapshot.crossBoundaryFreshness(): Freshness = Freshness(
    asOfEpochMillis = documents
        .mapNotNull { it.getTimestamp(UPDATED_AT)?.toDate()?.time }
        .maxOrNull() ?: 0L,
    // `isFromCache` alone is not it, and neither is `isEmpty` alone: served-from-
    // cache-and-non-empty is ordinary offline reading, and empty-from-the-server
    // is a genuine "nobody is here". Only the conjunction means the device has
    // never actually seen this collection.
    neverLoaded = metadata.isFromCache && documents.isEmpty(),
)
