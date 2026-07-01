package com.idomarhaim.goalpilot.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * BONUS (spec §6): rich analytics — progress per goal + share of completed
 * tasks per goal (an approximation of "percentage of time per goal").
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    goalRepository: GoalRepository,
    taskRepository: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
    ) { goals, tasks ->
        AnalyticsUiState(isLoading = false, goals = goals, tasks = tasks)
    }.catch { emit(AnalyticsUiState(isLoading = false, error = it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState(isLoading = true))
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val goals: List<Goal> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
)
