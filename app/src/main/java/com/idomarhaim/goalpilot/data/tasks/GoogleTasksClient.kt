package com.idomarhaim.goalpilot.data.tasks

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.domain.usecase.GoogleTaskList
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
 * Outcome of reading the user's Google Tasks *lists* — the names that become
 * life areas. Same three cases as [TasksImportResult] and deliberately a separate
 * type rather than a shared generic one: the two calls are consumed by different
 * screens, and a generic sealed interface behind a typealias reads worse at both
 * call sites than four extra lines here.
 */
sealed interface TaskListsResult {
    data class Success(val lists: List<GoogleTaskList>) : TaskListsResult
    data class NeedsConsent(val intent: Intent) : TaskListsResult
    data class Failure(val message: String) : TaskListsResult
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
     * Fetches the user's task lists — the names shown down the side of Google
     * Tasks — so they can be turned into life areas (spec §1 + §6 nice-to-have).
     *
     * Deliberately *not* derived from [fetchOpenTasks]: a list with no open tasks
     * returns no tasks at all, and empty lists are exactly the ones a user has
     * just created for an area of life they are about to start working on. Reading
     * the lists endpoint keeps them.
     */
    suspend fun fetchTaskLists(): TaskListsResult = withContext(io) {
        when (val token = accessToken()) {
            is TokenResult.NeedsConsent -> TaskListsResult.NeedsConsent(token.intent)
            is TokenResult.Failure -> TaskListsResult.Failure(token.message)
            is TokenResult.Success -> try {
                val lists = json.decodeFromString<TaskListsResponse>(
                    get("$BASE/users/@me/lists?maxResults=100", token.value),
                ).items
                TaskListsResult.Success(
                    lists.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                        .map { GoogleTaskList(id = it.id, title = it.title.trim().clampTitle()) },
                )
            } catch (e: Exception) {
                TaskListsResult.Failure(e.message ?: "Could not read your Google Tasks lists")
            }
        }
    }

    /**
     * Fetches every incomplete task across all of the user's task lists.
     *
     * @param maxPerList cap per list, so a huge list cannot stall the import.
     */
    suspend fun fetchOpenTasks(maxPerList: Int = 50): TasksImportResult = withContext(io) {
        val token = when (val t = accessToken()) {
            is TokenResult.NeedsConsent -> return@withContext TasksImportResult.NeedsConsent(t.intent)
            is TokenResult.Failure -> return@withContext TasksImportResult.Failure(t.message)
            is TokenResult.Success -> t.value
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
                            listId = list.id,
                            listTitle = list.title,
                        )
                    }
            }
            TasksImportResult.Success(imported)
        } catch (e: Exception) {
            TasksImportResult.Failure(e.message ?: "Could not read Google Tasks")
        }
    }

    /**
     * Mints an OAuth access token for the signed-in account, or explains why it
     * could not. Shared by both endpoints so the consent flow behaves identically
     * whether the user asked for lists or for tasks — the scope is the same one,
     * and a user who grants it from the life-areas screen must not be asked again
     * from the dashboard.
     */
    private fun accessToken(): TokenResult {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: return TokenResult.Failure("Not signed in with Google")
        return try {
            TokenResult.Success(
                GoogleAuthUtil.getToken(context, account, GoogleTasksScopes.OAUTH2_TASKS_READONLY),
            )
        } catch (e: UserRecoverableAuthException) {
            // The account exists but has not granted the Tasks scope yet.
            e.intent?.let { TokenResult.NeedsConsent(it) }
                ?: TokenResult.Failure("Google Tasks access was not granted")
        } catch (e: Exception) {
            TokenResult.Failure(e.message ?: "Could not obtain a Google Tasks token")
        }
    }

    private sealed interface TokenResult {
        data class Success(val value: String) : TokenResult
        data class NeedsConsent(val intent: Intent) : TokenResult
        data class Failure(val message: String) : TokenResult
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
    /**
     * Id of the Google Tasks list it came from. This is what ties an imported task
     * to a life area: the area stores the same id, so the link survives the user
     * renaming either side.
     */
    val listId: String = "",
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
