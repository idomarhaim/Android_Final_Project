package com.idomarhaim.goalpilot.feature.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.core.util.StoragePaths
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
