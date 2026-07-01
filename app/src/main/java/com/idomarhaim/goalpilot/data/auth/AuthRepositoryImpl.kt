package com.idomarhaim.goalpilot.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.firestore.dto.UserDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.snapshotsFlow
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    private val usersCol get() = firestore.collection(FirestorePaths.USERS)
    private val publicCol get() = firestore.collection(FirestorePaths.PUBLIC_PROFILES)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun authState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.flatMapLatest { fbUser ->
        if (fbUser == null) {
            flowOf(null)
        } else {
            usersCol.document(fbUser.uid).snapshotsFlow().map { doc ->
                doc.toObject(UserDto::class.java)?.toDomain() ?: fbUser.toDomainUser()
            }
        }
    }

    override fun currentUid(): String? = auth.currentUser?.uid

    override suspend fun signInWithGoogleIdToken(idToken: String): Resource<User> =
        withContext(io) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val fbUser = result.user
                    ?: return@withContext Resource.Error("Firebase returned no user")
                ensureProfile(fbUser)
                Resource.Success(fbUser.toDomainUser())
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Google sign-in failed", e)
            }
        }

    override suspend fun signOut() {
        withContext(io) { auth.signOut() }
    }

    /**
     * Creates the private user doc + public leaderboard projection on first
     * sign-in; on subsequent sign-ins only the mutable profile fields are
     * merged so accumulated points/level are never reset.
     */
    private suspend fun ensureProfile(fbUser: FirebaseUser) {
        val userRef = usersCol.document(fbUser.uid)
        val existing = userRef.get().await()
        val name = fbUser.displayName.orEmpty()
        val email = fbUser.email.orEmpty()
        val photo = fbUser.photoUrl?.toString()
        if (!existing.exists()) {
            userRef.set(
                UserDto(
                    uid = fbUser.uid,
                    displayName = name,
                    email = email,
                    photoUrl = photo,
                    points = 0L,
                    createdAt = System.currentTimeMillis(),
                ),
            ).await()
            publicCol.document(fbUser.uid).set(
                mapOf(
                    "displayName" to name,
                    "photoUrl" to photo,
                    "points" to 0L,
                    "level" to 1,
                ),
            ).await()
        } else {
            userRef.set(
                mapOf("displayName" to name, "email" to email, "photoUrl" to photo),
                SetOptions.merge(),
            ).await()
            publicCol.document(fbUser.uid).set(
                mapOf("displayName" to name, "photoUrl" to photo),
                SetOptions.merge(),
            ).await()
        }
    }

    private fun FirebaseUser.toDomainUser(): User = User(
        uid = uid,
        displayName = displayName.orEmpty(),
        email = email.orEmpty(),
        photoUrl = photoUrl?.toString(),
    )
}
