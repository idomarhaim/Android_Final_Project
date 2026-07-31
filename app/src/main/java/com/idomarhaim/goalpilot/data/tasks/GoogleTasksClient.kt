package com.idomarhaim.goalpilot.data.tasks

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** OAuth scopes this app may request beyond the default sign-in set. */
object GoogleTasksScopes {
    const val TASKS_READONLY = "https://www.googleapis.com/auth/tasks.readonly"

    /** The same scope in the `oauth2:` form [GoogleAuthUtil.getToken] expects. */
    const val OAUTH2_TASKS_READONLY = "oauth2:$TASKS_READONLY"
}

/**
 * Outcome of a Google Tasks fetch. [NeedsConsent] is not a failure: it means the
 * signed-in account has never granted the Tasks scope (e.g. it signed in before
 * the scope was added), and the carried [Intent] is Google's own consent screen.
 * Launch it, then retry.
 */
sealed interface TasksImportResult {
    data class Success(val tasks: List<ImportedTask>) : TasksImportResult
    data class NeedsConsent(val intent: Intent) : TasksImportResult
    data class Failure(val message: String) : TasksImportResult
}

/**
 * NICE-TO-HAVE (spec §5, §6): import open tasks from Google Tasks so they can be
 * filed against goals.
 *
 * Talks to the Tasks REST API directly with an OAuth access token minted by
 * [GoogleAuthUtil] from the existing Google Sign-In. That deliberately avoids the
 * `google-api-client-android` / `google-api-services-tasks` stack: those pull in
 * several megabytes and their own auth flow, when all this needs is two GET
 * requests against an endpoint we already have credentials for.
 *
 * Read-only by design — GoalPilot never writes back to Google Tasks.
 */
@Singleton
class GoogleTasksClient @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** True when a Google account is signed in and could be asked for a token. */
    fun isConnected(): Boolean =
        GoogleSignIn.getLastSignedInAccount(context)?.account != null

    /**
     * Fetches every incomplete task across all of the user's task lists.
     *
     * @param maxPerList cap per list, so a huge list cannot stall the import.
     */
    suspend fun fetchOpenTasks(maxPerList: Int = 50): TasksImportResult = withContext(io) {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: return@withContext TasksImportResult.Failure("Not signed in with Google")

        val token = try {
            GoogleAuthUtil.getToken(context, account, GoogleTasksScopes.OAUTH2_TASKS_READONLY)
        } catch (e: UserRecoverableAuthException) {
            // The account exists but has not granted the Tasks scope yet.
            val intent = e.intent
                ?: return@withContext TasksImportResult.Failure(
                    "Google Tasks access was not granted",
                )
            return@withContext TasksImportResult.NeedsConsent(intent)
        } catch (e: Exception) {
            return@withContext TasksImportResult.Failure(
                e.message ?: "Could not obtain a Google Tasks token",
            )
        }

        try {
            val lists = json.decodeFromString<TaskListsResponse>(
                get("$BASE/users/@me/lists?maxResults=100", token),
            ).items

            val imported = lists.flatMap { list ->
                val encoded = URLEncoder.encode(list.id, "UTF-8")
                val body = get(
                    "$BASE/lists/$encoded/tasks" +
                        "?showCompleted=false&showHidden=false&maxResults=$maxPerList",
                    token,
                )
                json.decodeFromString<TasksResponse>(body).items
                    // A Tasks "task" with a blank title is a placeholder row in the
                    // Google UI; importing it would create an untitled GoalPilot task.
                    .filter { it.title.isNotBlank() && it.status != STATUS_COMPLETED }
                    .map { dto ->
                        ImportedTask(
                            externalId = dto.id,
                            // Google Tasks titles are unbounded and people paste whole
                            // messages into them. An unclamped one becomes a giant
                            // Firestore task title — and, if classified as a new goal,
                            // a giant goal name too.
                            title = dto.title.trim().clampTitle(),
                            notes = dto.notes?.trim()?.takeIf { it.isNotEmpty() },
                            dueEpochMillis = dto.due?.let(::parseRfc3339),
                            listTitle = list.title,
                        )
                    }
            }
            TasksImportResult.Success(imported)
        } catch (e: Exception) {
            TasksImportResult.Failure(e.message ?: "Could not read Google Tasks")
        }
    }

    private fun get(url: String, token: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Google Tasks returned HTTP $code${detail.take(200)}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Google Tasks returns RFC-3339 timestamps; a bad one must not fail the import. */
    private fun parseRfc3339(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    /**
     * Trims a title to something usable as a task/goal name, cutting on a word
     * boundary where possible. Long titles also cost tokens on every
     * `classifyTask` call, so this is not purely cosmetic.
     */
    private fun String.clampTitle(): String {
        if (length <= MAX_TITLE) return this
        val cut = lastIndexOf(' ', MAX_TITLE).takeIf { it > MAX_TITLE / 2 } ?: MAX_TITLE
        return substring(0, cut).trimEnd() + "…"
    }

    private companion object {
        const val BASE = "https://tasks.googleapis.com/tasks/v1"
        const val TIMEOUT_MS = 15_000
        const val STATUS_COMPLETED = "completed"

        /** Longest imported title kept; Google Tasks itself allows far more. */
        const val MAX_TITLE = 120
    }
}

/** A task pulled from Google Tasks before it is mapped to a GoalPilot Task. */
data class ImportedTask(
    val externalId: String,
    val title: String,
    val notes: String? = null,
    val dueEpochMillis: Long? = null,
    /** Name of the Google Tasks list it came from — useful context when classifying. */
    val listTitle: String = "",
)

// ── Wire format (only the fields we consume) ──────────────────────────

@Serializable
private data class TaskListsResponse(val items: List<TaskListDto> = emptyList())

@Serializable
private data class TaskListDto(
    val id: String = "",
    val title: String = "",
)

@Serializable
private data class TasksResponse(val items: List<TaskDto> = emptyList())

@Serializable
private data class TaskDto(
    val id: String = "",
    val title: String = "",
    val notes: String? = null,
    val due: String? = null,
    val status: String? = null,
    @SerialName("hidden") val hidden: Boolean = false,
)
