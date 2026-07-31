package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Authentication + the current user's identity (spec §4: Firebase Auth / Google Sign-In). */
interface AuthRepository {

    /** Emits the signed-in [User] (profile merged from Firestore) or null when signed out. */
    fun authState(): Flow<User?>

    /** Uid of the currently signed-in user, or null. */
    fun currentUid(): String?

    /** Completes Firebase sign-in using a Google ID token obtained from the Google Sign-In flow. */
    suspend fun signInWithGoogleIdToken(idToken: String): Resource<User>

    suspend fun signOut()
}
