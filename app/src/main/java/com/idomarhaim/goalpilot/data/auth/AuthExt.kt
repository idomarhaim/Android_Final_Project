package com.idomarhaim.goalpilot.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Emits the signed-in uid, and re-emits whenever it changes (sign-in, sign-out,
 * or switching accounts).
 *
 * Repositories must build their snapshot flows on top of this rather than
 * reading `auth.currentUser` once: a `Flow` is constructed when the ViewModel is
 * created, so a one-shot read pins whichever user happened to be signed in at
 * that moment. Demoing two accounts on one device (spec §7) would otherwise
 * serve the first user's data to the second.
 */
fun FirebaseAuth.uidFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}.distinctUntilChanged()
