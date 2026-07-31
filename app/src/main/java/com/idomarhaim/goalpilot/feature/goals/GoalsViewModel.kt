package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    goalRepository: GoalRepository,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = goalRepository.observeGoals()
        .map<List<Goal>, GoalsUiState> { GoalsUiState(isLoading = false, goals = it) }
        .catch { emit(GoalsUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalsUiState(isLoading = true),
        )
}

data class GoalsUiState(
    val isLoading: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val error: String? = null,
)
