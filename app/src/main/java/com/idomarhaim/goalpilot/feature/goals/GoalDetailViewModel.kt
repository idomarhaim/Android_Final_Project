package com.idomarhaim.goalpilot.feature.goals

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.ui.navigation.Routes
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
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val progressRepository: ProgressRepository,
    private val recommendationRepository: RecommendationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String = savedStateHandle[Routes.ARG_GOAL_ID] ?: ""

    val uiState: StateFlow<GoalDetailUiState> = combine(
        goalRepository.observeGoal(goalId),
        taskRepository.observeTasks(goalId),
        progressRepository.observeEntries(goalId),
    ) { goal, tasks, entries ->
        GoalDetailUiState(isLoading = false, goal = goal, tasks = tasks, entries = entries)
    }.catch { emit(GoalDetailUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalDetailUiState(isLoading = true),
        )

    private val _action = MutableStateFlow(GoalDetailActionState())
    val action = _action.asStateFlow()

    fun addTask(title: String, points: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.upsertTask(
                Task(goalId = goalId, title = title.trim(), points = points.coerceIn(1, 1000)),
            )
        }
    }

    /**
     * Asks the LLM (via the `scoreTask` Cloud Function) what a task is worth
     * — spec §6 Core, "point scoring for tasks". The result lands in
     * [GoalDetailActionState.suggestedPoints] for the add-task row to pick up;
     * on any failure the repository returns a local estimate instead.
     */
    fun suggestPoints(title: String) {
        if (title.isBlank()) {
            _action.update { it.copy(message = "Type the task first") }
            return
        }
        viewModelScope.launch {
            _action.update { it.copy(isScoring = true, suggestedPoints = null) }
            val points = when (val result = recommendationRepository.scoreTask(title.trim())) {
                is Resource.Success -> result.data
                else -> null
            }
            _action.update { it.copy(isScoring = false, suggestedPoints = points) }
        }
    }

    fun consumeSuggestedPoints() = _action.update { it.copy(suggestedPoints = null) }

    fun toggleTask(task: Task) {
        viewModelScope.launch { taskRepository.setDone(task.id, !task.isDone) }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { taskRepository.deleteTask(taskId) }
    }

    fun logProgress(value: Double, note: String, imageUri: Uri?) {
        viewModelScope.launch {
            _action.update { it.copy(isSubmitting = true, message = null) }
            val entry = ProgressEntry(goalId = goalId, value = value, note = note.trim())
            when (val result = progressRepository.logProgress(entry, imageUri)) {
                is Resource.Success ->
                    _action.update { it.copy(isSubmitting = false, message = "Progress logged") }
                is Resource.Error ->
                    _action.update { it.copy(isSubmitting = false, message = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun archiveGoal() {
        viewModelScope.launch { goalRepository.setArchived(goalId, true) }
    }

    fun deleteGoal(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (goalRepository.deleteGoal(goalId)) {
                is Resource.Success -> onDeleted()
                else -> _action.update { it.copy(message = "Could not delete goal") }
            }
        }
    }

    fun consumeMessage() = _action.update { it.copy(message = null) }
}

data class GoalDetailUiState(
    val isLoading: Boolean = false,
    val goal: Goal? = null,
    val tasks: List<Task> = emptyList(),
    val entries: List<ProgressEntry> = emptyList(),
    val error: String? = null,
)

data class GoalDetailActionState(
    val isSubmitting: Boolean = false,
    val isScoring: Boolean = false,
    /** One-shot LLM point estimate; the add-task row consumes and clears it. */
    val suggestedPoints: Int? = null,
    val message: String? = null,
)
