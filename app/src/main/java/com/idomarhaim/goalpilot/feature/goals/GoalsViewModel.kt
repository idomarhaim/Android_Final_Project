package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
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

    /**
     * Ido keeps a goal the sorter proposed — §1.1, `#6`.
     *
     * The marker becomes [DeclaredBy.USER], which is the whole transition: nothing moves, no
     * document is rewritten, and every task already filed under it stays where it is. That is
     * the point of putting provenance on the goal instead of in a separate *suggestions*
     * collection — accepting a proposal costs one field.
     */
    fun keepSuggestion(goalId: String) {
        viewModelScope.launch { goalRepository.setDeclaredBy(goalId, DeclaredBy.USER) }
    }

    /**
     * Ido says it is not a goal — §1.1's **lossless demotion**.
     *
     * Drops the marker and nothing else. The object survives, its tasks survive, its life
     * areas survive; what it loses is the claim that he wants it for its own sake, which is
     * the only thing the sorter ever asserted. **Not a delete**, and deliberately not offered
     * as one: the task that caused the goal to exist is still real work he typed in, and
     * throwing the goal away would take that with it.
     */
    fun demoteSuggestion(goalId: String) {
        viewModelScope.launch { goalRepository.setDeclaredBy(goalId, null) }
    }
}

data class GoalsUiState(
    val isLoading: Boolean = false,
    /** Life-area bands, in the user's own area order; see [GroupGoalsByLifeAreaUseCase]. */
    val groups: List<GoalGroup> = emptyList(),
    /** Kept separately so "no goals at all" stays one cheap check, not a fold over groups. */
    val totalGoals: Int = 0,
    val error: String? = null,
)
