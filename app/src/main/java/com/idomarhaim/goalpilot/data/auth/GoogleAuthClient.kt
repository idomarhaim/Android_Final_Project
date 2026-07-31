package com.idomarhaim.goalpilot.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over Google Sign-In. Produces the sign-in [Intent] the UI
 * launches and extracts the Google ID token from the returned result, which is
 * then exchanged for a Firebase credential by [AuthRepositoryImpl].
 *
 * The Web client id comes from R.string.gp_web_client_id, generated from the
 * GOOGLE_WEB_CLIENT_ID entry in local.properties (see docs/SETUP.md).
 */
@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val options: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.gp_web_client_id))
            .requestEmail()
            // Google Tasks import (spec §6 nice-to-have) rides the same sign-in.
            // Requesting it here means new sign-ins consent once; accounts that
            // signed in before this scope existed are handled at call time by
            // GoogleTasksClient, which surfaces the recovery intent.
            .requestScopes(Scope(GoogleTasksScopes.TASKS_READONLY))
            .build()

    private val client: GoogleSignInClient = GoogleSignIn.getClient(context, options)

    val signInIntent: Intent get() = client.signInIntent

    /** Extracts the Google ID token from the sign-in result Intent. */
    fun extractIdToken(data: Intent?): Result<String> = try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
        account.idToken
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("Google sign-in returned no ID token"))
    } catch (e: ApiException) {
        Result.failure(e)
    }

    suspend fun signOut() {
        runCatching { client.signOut().await() }
    }
}
