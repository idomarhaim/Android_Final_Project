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
import com.idomarhaim.goalpilot.domain.model.CalendarScope
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
            // §2.7's incremental-authorization table, ROW ONE: "sign-in ->
            // `calendar.app.created` + the calendar list" (`#61`). One scope covers both,
            // because that scope's own `calendarList` is limited to calendars this app
            // created -- see `GoogleCalendarClient.listCalendars`.
            //
            // ⚠️ This is a checkbox in the sign-in sheet, NOT a gate. §2.6: "the consent
            // checkbox arrives UNCHECKED, so sign-in can succeed while granting nothing" --
            // so declining it here is the normal case and every calendar feature degrades
            // rather than blocking. What asking here buys is that the ask lands *where the
            // user is already granting things*, instead of as an interstitial the first time
            // they open a calendar surface.
            //
            // The other two rows of that table are deliberately NOT here: `freeBusy` and
            // `calendar.readonly` are asked for at their own trigger, because §2.7 requires
            // the restraint to be "visible in which call is made, rather than as a filter
            // after the fact". Adding them to this builder would be exactly the omnibus
            // request that sentence rejects.
            //
            // Accounts that signed in before this line existed keep their old scope set;
            // `GoogleCalendarClient` mints per-scope and surfaces the recovery intent for
            // them, the same fallback the Tasks scope above already relies on.
            .requestScopes(Scope(CalendarScope.APP_CREATED.url))
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
