package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.usecase.GoalGroup
import com.idomarhaim.goalpilot.domain.usecase.GroupGoalsByLifeAreaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    goalRepository: GoalRepository,
    lifeAreaRepository: LifeAreaRepository,
    groupGoals: GroupGoalsByLifeAreaUseCase,
) : ViewModel() {

    // Both flows, because the list groups by area: a rename or a reorder on the
    // life-areas screen has to reach this list without a round-trip through the
    // goals collection, which never changed.
    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.observeGoals(),
        lifeAreaRepository.observeLifeAreas(),
    ) { goals, areas ->
        GoalsUiState(
            isLoading = false,
            groups = groupGoals(goals, areas),
            totalGoals = goals.size,
        )
    }.catch { emit(GoalsUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalsUiState(isLoading = true),
        )
}

data class GoalsUiState(
    val isLoading: Boolean = false,
    /** Life-area bands, in the user's own area order; see [GroupGoalsByLifeAreaUseCase]. */
    val groups: List<GoalGroup> = emptyList(),
    /** Kept separately so "no goals at all" stays one cheap check, not a fold over groups. */
    val totalGoals: Int = 0,
    val error: String? = null,
)
