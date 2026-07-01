package com.idomarhaim.goalpilot.data.tasks

import com.idomarhaim.goalpilot.core.result.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NICE-TO-HAVE (spec §5, §6): import tasks from the Google Tasks API to
 * auto-populate progress. The same Google Sign-In can request the
 * `tasks.readonly` scope (see [com.idomarhaim.goalpilot.data.auth.GoogleAuthClient]).
 *
 * Compiling stub — no Google API client dependency yet so the Core build stays
 * lean.
 *
 * TO ACTIVATE:
 *  1. Add deps:
 *       com.google.api-client:google-api-client-android
 *       com.google.apis:google-api-services-tasks:<ver>
 *       com.google.auth:google-auth-library-oauth2-http
 *  2. Add `.requestScopes(Scope(TasksScopes.TASKS_READONLY))` to GoogleAuthClient.
 *  3. Build a Tasks service with GoogleAccountCredential from the signed-in
 *     account and list task lists / tasks here.
 *  4. Map imported tasks to [com.idomarhaim.goalpilot.domain.model.Task] with
 *     source = GOOGLE_TASKS and let the LLM classify them into goals.
 *
 * See TODO/TODO_OPTIONAL/Integrations.TODO.optional.md.
 */
@Singleton
class GoogleTasksClient @Inject constructor() {

    fun isConnected(): Boolean = false

    suspend fun fetchOpenTasks(): Resource<List<ImportedTask>> =
        Resource.Error("Google Tasks import is not enabled in this build (nice-to-have).")
}

/** A task pulled from Google Tasks before it is mapped to a GoalPilot Task. */
data class ImportedTask(
    val externalId: String,
    val title: String,
    val notes: String? = null,
    val dueEpochMillis: Long? = null,
)
