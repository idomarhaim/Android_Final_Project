package com.idomarhaim.goalpilot.feature.dashboard

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.core.util.StoragePaths
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksClient
import com.idomarhaim.goalpilot.data.tasks.TasksImportResult
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildHealthProposalsUseCase
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import com.idomarhaim.goalpilot.domain.usecase.HealthLogProposal
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val recommendationRepository: RecommendationRepository,
    private val socialRepository: SocialRepository,
    private val storageRepository: StorageRepository,
    private val progressRepository: ProgressRepository,
    private val googleTasksClient: GoogleTasksClient,
    private val healthRepository: HealthRepository,
    private val buildSummary: BuildSummaryUseCase,
    private val buildHealthProposals: BuildHealthProposalsUseCase,
) : ViewModel() {

    @Volatile private var lastGoals: List<Goal> = emptyList()
    @Volatile private var lastTasks: List<Task> = emptyList()

    val uiState: StateFlow<DashboardUiState> = combine(
        authRepository.authState(),
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
    ) { user, goals, tasks ->
        lastGoals = goals
        lastTasks = tasks
        val windowStart = DateTimeUtils.windowStart(SummaryPeriod.WEEKLY)
        val completedLast7d = tasks.count {
            it.isDone && (it.completedAtEpochMillis ?: 0L) >= windowStart
        }
        DashboardUiState(
            isLoading = false,
            userName = user?.displayName?.substringBefore(' ').orEmpty(),
            points = user?.points ?: 0L,
            level = user?.level ?: 1,
            levelProgress = user?.levelProgress ?: 0f,
            pointsToNextLevel = user?.pointsToNextLevel ?: 0L,
            goals = goals,
            averageProgress = if (goals.isEmpty()) 0f
            else goals.map { it.progressFraction }.average().toFloat(),
            completedTasksLast7d = completedLast7d,
            doneTasks = tasks.count { it.isDone },
            totalTasks = tasks.size,
        )
    }.catch { emit(DashboardUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardUiState(isLoading = true),
        )

    private val _recs = MutableStateFlow(RecommendationsState())
    val recommendations = _recs.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private var recommendationsLoaded = false

    /** Loads AI recommendations once per screen entry; call again to refresh. */
    fun ensureRecommendations() {
        if (recommendationsLoaded) return
        recommendationsLoaded = true
        refreshRecommendations()
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            _recs.update { it.copy(isLoading = true, error = null) }
            val state = uiState.value
            when (val result = recommendationRepository.getRecommendations(
                goals = lastGoals,
                completedTasksLast7d = state.completedTasksLast7d,
                totalPoints = state.points,
            )) {
                is Resource.Success ->
                    _recs.update { RecommendationsState(isLoading = false, items = result.data) }
                is Resource.Error ->
                    _recs.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    // ── Smart add (spec §6 Bonus: LLM task→goal classification) ──────

    private val _smartAdd = MutableStateFlow(SmartAddState())
    val smartAdd = _smartAdd.asStateFlow()

    /**
     * Runs a free-text task title through the `classifyTask` Cloud Function and
     * opens a confirmation sheet with the proposal: attach it to an existing goal,
     * or create the goal the LLM suggests. The user always confirms — the LLM
     * proposes, it never writes (spec §8: LLM output can be inconsistent).
     */
    fun classifyForSmartAdd(rawTitle: String) {
        val title = rawTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _smartAdd.value = SmartAddState(isVisible = true, isClassifying = true, taskTitle = title)
            val goals = lastGoals
            val classification = when (val r = recommendationRepository.classifyTask(title, goals)) {
                is Resource.Success -> r.data
                else -> null
            }
            if (classification == null) {
                _smartAdd.value = SmartAddState()
                _message.value = "Could not analyse that task"
                return@launch
            }
            // The model can name a goal id that does not exist; only trust ids we
            // can actually resolve, otherwise fall through to the new-goal branch.
            val matched = goals.firstOrNull { it.id == classification.suggestedGoalId }
            _smartAdd.value = SmartAddState(
                isVisible = true,
                isClassifying = false,
                taskTitle = title,
                targetGoalId = matched?.id,
                targetGoalTitle = matched?.title,
                newGoalTitle = if (matched == null) {
                    classification.suggestedNewGoalTitle?.takeIf { it.isNotBlank() } ?: title
                } else {
                    null
                },
                newGoalCategory = classification.suggestedCategory,
                points = classification.estimatedPoints.coerceIn(1, 1000),
                rationale = classification.rationale,
            )
        }
    }

    /** Applies the proposal: creates the goal if needed, then the task. */
    fun confirmSmartAdd() {
        val state = _smartAdd.value
        if (state.isClassifying || state.isSaving) return
        viewModelScope.launch {
            _smartAdd.update { it.copy(isSaving = true) }
            val goalId = state.targetGoalId ?: run {
                val newGoal = Goal(
                    title = state.newGoalTitle.orEmpty().ifBlank { state.taskTitle },
                    category = state.newGoalCategory,
                )
                when (val r = goalRepository.upsertGoal(newGoal)) {
                    is Resource.Success -> r.data
                    else -> null
                }
            }
            if (goalId == null) {
                _smartAdd.value = SmartAddState()
                _message.value = "Could not create the goal"
                return@launch
            }
            val saved = taskRepository.upsertTask(
                Task(goalId = goalId, title = state.taskTitle, points = state.points),
            )
            _smartAdd.value = SmartAddState()
            _message.value = when (saved) {
                is Resource.Success ->
                    if (state.targetGoalId != null) {
                        "Added to “${state.targetGoalTitle}”"
                    } else {
                        "Created “${state.newGoalTitle}” and added the task"
                    }
                else -> "Could not add the task"
            }
        }
    }

    fun dismissSmartAdd() { _smartAdd.value = SmartAddState() }

    // ── Google Tasks import (spec §6 nice-to-have) ────────────────────

    private val _tasksImport = MutableStateFlow(TasksImportState())
    val tasksImport = _tasksImport.asStateFlow()

    /** Set when Google needs the user to grant the Tasks scope; the screen launches it. */
    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent = _consentIntent.asStateFlow()

    /**
     * Pulls open tasks from Google Tasks, runs each through the same
     * `classifyTask` function the "Smart add" card uses, and opens a review sheet.
     * Nothing is written until the user confirms — identical policy to smart add.
     */
    fun importGoogleTasks() {
        if (_tasksImport.value.isLoading) return
        viewModelScope.launch {
            _tasksImport.value = TasksImportState(isVisible = true, isLoading = true)
            when (val result = googleTasksClient.fetchOpenTasks()) {
                is TasksImportResult.NeedsConsent -> {
                    _tasksImport.value = TasksImportState()
                    _consentIntent.value = result.intent
                }

                is TasksImportResult.Failure ->
                    _tasksImport.value =
                        TasksImportState(isVisible = true, error = result.message)

                is TasksImportResult.Success -> {
                    // Re-running the import must not duplicate what is already here.
                    // Tasks carry no external id in Firestore, so title is the only
                    // handle we have — see the TODO note about a proper externalId.
                    val existing = lastTasks.map { it.title.trim().lowercase() }.toSet()
                    val fresh = result.tasks
                        .filter { it.title.trim().lowercase() !in existing }
                        .take(MAX_IMPORT)

                    if (fresh.isEmpty()) {
                        _tasksImport.value = TasksImportState(
                            isVisible = true,
                            error = if (result.tasks.isEmpty()) {
                                "No open tasks found in Google Tasks"
                            } else {
                                "Everything in Google Tasks is already here"
                            },
                        )
                        return@launch
                    }

                    val goals = lastGoals
                    val proposals = coroutineScope {
                        fresh.map { imported ->
                            async {
                                val classification = when (
                                    val r = recommendationRepository.classifyTask(
                                        imported.title,
                                        goals,
                                    )
                                ) {
                                    is Resource.Success -> r.data
                                    else -> null
                                }
                                val matched =
                                    goals.firstOrNull { it.id == classification?.suggestedGoalId }
                                ImportProposal(
                                    externalId = imported.externalId,
                                    title = imported.title,
                                    listTitle = imported.listTitle,
                                    targetGoalId = matched?.id,
                                    targetGoalTitle = matched?.title,
                                    newGoalTitle = if (matched == null) {
                                        classification?.suggestedNewGoalTitle
                                            ?.takeIf { it.isNotBlank() }
                                            ?: imported.listTitle.ifBlank { imported.title }
                                    } else {
                                        null
                                    },
                                    newGoalCategory = classification?.suggestedCategory
                                        ?: GoalCategory.OTHER,
                                    points = (classification?.estimatedPoints ?: 10)
                                        .coerceIn(1, 1000),
                                )
                            }
                        }.awaitAll()
                    }
                    _tasksImport.value = TasksImportState(
                        isVisible = true,
                        proposals = proposals,
                        totalFound = result.tasks.size,
                    )
                }
            }
        }
    }

    fun toggleImportProposal(externalId: String) {
        _tasksImport.update { state ->
            state.copy(
                proposals = state.proposals.map {
                    if (it.externalId == externalId) it.copy(selected = !it.selected) else it
                },
            )
        }
    }

    /** Creates any goals the proposals need, then the selected tasks. */
    fun confirmImport() {
        val state = _tasksImport.value
        if (state.isSaving) return
        val chosen = state.proposals.filter { it.selected }
        if (chosen.isEmpty()) {
            _tasksImport.value = TasksImportState()
            return
        }
        viewModelScope.launch {
            _tasksImport.update { it.copy(isSaving = true) }
            // Two proposals can ask for the same new goal; create it once.
            val createdGoals = mutableMapOf<String, String>()
            var saved = 0
            for (proposal in chosen) {
                val goalId = proposal.targetGoalId ?: run {
                    val key = proposal.newGoalTitle.orEmpty().lowercase()
                    createdGoals[key] ?: run {
                        val created = goalRepository.upsertGoal(
                            Goal(
                                title = proposal.newGoalTitle.orEmpty().ifBlank { proposal.title },
                                category = proposal.newGoalCategory,
                            ),
                        )
                        (created as? Resource.Success)?.data?.also { createdGoals[key] = it }
                    }
                }
                if (goalId == null) continue
                val result = taskRepository.upsertTask(
                    Task(
                        goalId = goalId,
                        title = proposal.title,
                        points = proposal.points,
                        source = TaskSource.GOOGLE_TASKS,
                    ),
                )
                if (result is Resource.Success) saved++
            }
            _tasksImport.value = TasksImportState()
            _message.value = when (saved) {
                0 -> "Could not import those tasks"
                1 -> "Imported 1 task from Google Tasks"
                else -> "Imported $saved tasks from Google Tasks"
            }
        }
    }

    fun dismissImport() { _tasksImport.value = TasksImportState() }

    fun consumeConsentIntent() { _consentIntent.value = null }

    /** Called after the user completes Google's consent screen. */
    fun onConsentGranted() {
        _consentIntent.value = null
        importGoogleTasks()
    }

    // ── Health Connect sync (spec §5, §6 nice-to-have) ────────────────

    private val _healthSync = MutableStateFlow(HealthSyncState())
    val healthSync = _healthSync.asStateFlow()

    /** Handed straight to the Health Connect permission contract by the screen. */
    val healthPermissions: Set<String> get() = healthRepository.requiredPermissions

    private var healthChecked = false

    /** Resolves the card's state once per screen entry. */
    fun ensureHealthAvailability() {
        if (healthChecked) return
        healthChecked = true
        refreshHealthAvailability()
    }

    private fun refreshHealthAvailability() {
        viewModelScope.launch {
            val availability = healthRepository.availability()
            _healthSync.update { it.copy(availability = availability) }
        }
    }

    /**
     * Reads the last week of steps and sleep and opens the review sheet.
     *
     * When the permissions are missing this asks the *screen* to request them —
     * a ViewModel cannot launch an Android permission flow, and the Health
     * Connect contract has to be registered from a composable.
     */
    fun syncHealth() {
        if (_healthSync.value.isLoading) return
        viewModelScope.launch {
            val availability = healthRepository.availability()
            _healthSync.update { it.copy(availability = availability) }
            when (availability) {
                HealthAvailability.PERMISSIONS_REQUIRED -> {
                    _healthSync.update { it.copy(requestPermissions = true) }
                    return@launch
                }

                HealthAvailability.AVAILABLE -> Unit

                else -> {
                    _message.value = availability.explain()
                    return@launch
                }
            }

            _healthSync.update { it.copy(isVisible = true, isLoading = true, error = null) }
            when (val read = healthRepository.readSnapshot()) {
                is Resource.Error ->
                    _healthSync.update { it.copy(isLoading = false, error = read.message) }

                Resource.Loading -> Unit

                is Resource.Success -> {
                    val snapshot = read.data
                    if (snapshot.isEmpty) {
                        _healthSync.update {
                            it.copy(
                                isLoading = false,
                                error = "Health Connect has no steps or sleep for the last week",
                            )
                        }
                        return@launch
                    }
                    // Two passes: the first decides which goals are involved, the
                    // second drops the days those goals were already given. The
                    // filing rules live in the use case either way — this only
                    // supplies the facts it cannot read for itself.
                    val provisional = buildHealthProposals(snapshot, lastGoals)
                    val alreadyLogged = provisional.mapNotNull { it.targetGoalId }
                        .distinct()
                        .flatMap { goalId -> loggedSourceKeys(goalId) }
                        .toSet()
                    val proposals = buildHealthProposals(snapshot, lastGoals, alreadyLogged)

                    _healthSync.update {
                        it.copy(
                            isLoading = false,
                            proposals = proposals,
                            skippedCount = provisional.size - proposals.size,
                            error = if (proposals.isEmpty()) {
                                "Every reading from the last week is already logged"
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Source keys already written against a goal. Bounded by a timeout: this is a
     * Firestore snapshot flow, and a dedupe lookup that never returns would leave
     * the review sheet spinning forever. Losing the lookup is recoverable — the
     * user still reviews every row before it is written.
     */
    private suspend fun loggedSourceKeys(goalId: String): List<String> =
        withTimeoutOrNull(DEDUPE_TIMEOUT_MS) {
            progressRepository.observeEntries(goalId).first().mapNotNull { it.sourceKey }
        }.orEmpty()

    fun toggleHealthProposal(sourceKey: String) {
        _healthSync.update { state ->
            state.copy(
                proposals = state.proposals.map {
                    if (it.sourceKey == sourceKey) it.copy(selected = !it.selected) else it
                },
            )
        }
    }

    /** Creates whichever goals the selection needs, then logs a progress entry per reading. */
    fun confirmHealthSync() {
        val state = _healthSync.value
        if (state.isSaving) return
        val chosen = state.proposals.filter { it.selected }
        if (chosen.isEmpty()) {
            _healthSync.value = HealthSyncState(availability = state.availability)
            return
        }
        viewModelScope.launch {
            _healthSync.update { it.copy(isSaving = true) }
            // Steps and sleep each need at most one new goal, however many days
            // are selected — create it once and reuse the id.
            val createdGoals = mutableMapOf<HealthMetric, String>()
            var saved = 0
            for (proposal in chosen) {
                val goalId = proposal.targetGoalId
                    ?: createdGoals[proposal.metric]
                    ?: run {
                        val created = goalRepository.upsertGoal(
                            Goal(
                                title = proposal.newGoalTitle.orEmpty()
                                    .ifBlank { proposal.metric.defaultGoalTitle },
                                category = proposal.metric.category,
                                unit = proposal.metric.unit,
                                targetValue = proposal.metric.defaultGoalTarget,
                            ),
                        )
                        (created as? Resource.Success)?.data
                            ?.also { createdGoals[proposal.metric] = it }
                    }
                if (goalId == null) continue
                val result = progressRepository.logProgress(
                    ProgressEntry(
                        goalId = goalId,
                        value = proposal.value,
                        note = proposal.noteText(),
                        sourceKey = proposal.sourceKey,
                    ),
                    imageUri = null,
                )
                if (result is Resource.Success) saved++
            }
            _healthSync.value = HealthSyncState(availability = state.availability)
            _message.value = when (saved) {
                0 -> "Could not log those readings"
                1 -> "Logged 1 reading from Health Connect"
                else -> "Logged $saved readings from Health Connect"
            }
        }
    }

    fun dismissHealthSync() {
        _healthSync.value = HealthSyncState(availability = _healthSync.value.availability)
    }

    fun consumeHealthPermissionRequest() {
        _healthSync.update { it.copy(requestPermissions = false) }
    }

    /** Called with whatever Health Connect actually granted, which may be a subset. */
    fun onHealthPermissionsResult(granted: Set<String>) {
        _healthSync.update { it.copy(requestPermissions = false) }
        if (granted.containsAll(healthRepository.requiredPermissions)) {
            syncHealth()
        } else {
            refreshHealthAvailability()
            _message.value = "GoalPilot was not given access to your steps and sleep"
        }
    }

    fun shareWeeklySummary(imageUri: Uri?) {
        viewModelScope.launch {
            _message.value = null
            val summary = buildSummary(
                period = SummaryPeriod.WEEKLY,
                goals = lastGoals,
                tasks = lastTasks,
                windowStartMillis = DateTimeUtils.windowStart(SummaryPeriod.WEEKLY),
            )
            val imageUrl: String? = imageUri?.let { uri ->
                when (val up = storageRepository.uploadImage(StoragePaths.SUMMARY_IMAGES, uri)) {
                    is Resource.Success -> up.data
                    else -> null
                }
            }
            _message.value = when (socialRepository.shareSummary(summary, imageUrl)) {
                is Resource.Success -> "Shared your weekly summary!"
                else -> "Could not share summary"
            }
        }
    }

    fun consumeMessage() { _message.value = null }

    private companion object {
        /**
         * Cap on tasks imported per run. Each one costs a `classifyTask` call, and
         * GROQ's free tier allows 30 requests/minute — a 60-task list would blow
         * through it and half the classifications would silently fall back.
         */
        const val MAX_IMPORT = 15

        /** Ceiling on the per-goal dedupe lookup before the sync proceeds without it. */
        const val DEDUPE_TIMEOUT_MS = 5_000L
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val points: Long = 0L,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val pointsToNextLevel: Long = 0L,
    val goals: List<Goal> = emptyList(),
    val averageProgress: Float = 0f,
    val completedTasksLast7d: Int = 0,
    val doneTasks: Int = 0,
    val totalTasks: Int = 0,
    val error: String? = null,
)

data class RecommendationsState(
    val isLoading: Boolean = false,
    val items: List<Recommendation> = emptyList(),
    val error: String? = null,
)

/**
 * The LLM's proposal for a free-text task, awaiting the user's confirmation.
 * Exactly one of [targetGoalId] (attach to an existing goal) or [newGoalTitle]
 * (create one) is set once classification finishes.
 */
data class SmartAddState(
    val isVisible: Boolean = false,
    val isClassifying: Boolean = false,
    val isSaving: Boolean = false,
    val taskTitle: String = "",
    val targetGoalId: String? = null,
    val targetGoalTitle: String? = null,
    val newGoalTitle: String? = null,
    val newGoalCategory: GoalCategory = GoalCategory.OTHER,
    val points: Int = 10,
    val rationale: String = "",
)

/** Review sheet for a Google Tasks import, before anything is written. */
data class TasksImportState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val proposals: List<ImportProposal> = emptyList(),
    /** How many open tasks Google returned, before dedupe and the import cap. */
    val totalFound: Int = 0,
    val error: String? = null,
)

/**
 * Review sheet for a Health Connect sync, plus the card's own state.
 *
 * [availability] is null only before the first check has come back; the card
 * renders a neutral "checking" state until then rather than claiming the feature
 * is unsupported.
 */
data class HealthSyncState(
    val availability: HealthAvailability? = null,
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** Set when the screen must launch the Health Connect permission contract. */
    val requestPermissions: Boolean = false,
    val proposals: List<HealthLogProposal> = emptyList(),
    /** Readings dropped because they were already logged — worth telling the user. */
    val skippedCount: Int = 0,
    val error: String? = null,
)

/** Why the health card cannot sync right now, in the user's terms. */
fun HealthAvailability.explain(): String = when (this) {
    HealthAvailability.NOT_SUPPORTED ->
        "Health Connect is not available on this device"
    HealthAvailability.PROVIDER_UPDATE_REQUIRED ->
        "Update Health Connect from the Play Store to sync your steps and sleep"
    HealthAvailability.PERMISSIONS_REQUIRED ->
        "GoalPilot needs permission to read your steps and sleep"
    HealthAvailability.AVAILABLE ->
        "Health Connect is ready"
}

private val healthDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/** e.g. "Sat, Aug 1" — the day the reading belongs to, not the day it was synced. */
fun HealthLogProposal.dayLabel(): String =
    healthDayFormatter.format(LocalDate.ofEpochDay(epochDay))

fun HealthLogProposal.valueLabel(): String = when (metric) {
    HealthMetric.STEPS -> "%,d steps".format(value.toLong())
    HealthMetric.SLEEP -> "%.1f hours".format(value)
}

/** What gets written to the progress entry the user will read back later. */
fun HealthLogProposal.noteText(): String =
    "Health Connect · ${valueLabel()} · ${dayLabel()}"

/**
 * One Google Tasks entry plus the LLM's filing proposal. Exactly one of
 * [targetGoalId] (attach to an existing goal) or [newGoalTitle] (create one) is set.
 */
data class ImportProposal(
    val externalId: String,
    val title: String,
    val listTitle: String = "",
    val targetGoalId: String? = null,
    val targetGoalTitle: String? = null,
    val newGoalTitle: String? = null,
    val newGoalCategory: GoalCategory = GoalCategory.OTHER,
    val points: Int = 10,
    val selected: Boolean = true,
)
