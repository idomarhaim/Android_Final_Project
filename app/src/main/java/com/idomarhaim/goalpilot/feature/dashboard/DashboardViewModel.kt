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
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val recommendationRepository: RecommendationRepository,
    private val socialRepository: SocialRepository,
    private val storageRepository: StorageRepository,
    private val googleTasksClient: GoogleTasksClient,
    private val buildSummary: BuildSummaryUseCase,
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
