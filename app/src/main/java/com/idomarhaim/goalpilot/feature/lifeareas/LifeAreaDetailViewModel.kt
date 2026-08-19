package com.idomarhaim.goalpilot.feature.lifeareas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One life area's own screen (`PRODUCT_v0.3` §4.7): the area, and the goals filed
 * under it.
 *
 * This is the destination issue #2 was missing. Before it, the life-areas list
 * showed a goal count that read like a link and was a label — the row's only
 * clickable nodes were *Edit* and *Delete*.
 *
 * **The goal list is filtered here rather than grouped upstream.** Since §1.2 made
 * the edge plural a goal can serve several areas, so there is no partition of the
 * goals to hand out — "the goals of this area" is a membership test, and asking it
 * of one area is the whole question this screen has.
 */
@HiltViewModel
class LifeAreaDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    lifeAreaRepository: LifeAreaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val areaId: String = savedStateHandle[Routes.ARG_LIFE_AREA_ID] ?: ""

    val uiState: StateFlow<LifeAreaDetailUiState> = combine(
        // Archived areas included: this screen is reachable from a link that may
        // outlive the area's visibility on the list, and "the area you asked for
        // is archived" is a better answer than "no such area".
        lifeAreaRepository.observeLifeAreas(includeArchived = true),
        goalRepository.observeGoals(),
    ) { areas, goals ->
        val area = areas.firstOrNull { it.id == areaId }
        LifeAreaDetailUiState(
            isLoading = false,
            area = area,
            // Deliberately computed even when the area is gone, so the screen can
            // say how many goals a deletion would unfile rather than showing an
            // empty page.
            goals = goals.filter { areaId in it.lifeAreaIds },
            unfiledGoals = goals.filter { goal ->
                goal.lifeAreaIds.none { id -> areas.any { it.id == id } }
            },
            areaExists = area != null,
        )
    }.catch { emit(LifeAreaDetailUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LifeAreaDetailUiState(isLoading = true),
        )

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    /**
     * Adds this area to [goal]'s areas, keeping the ones it already has.
     *
     * The union is built here rather than in the repository because
     * [GoalRepository.setLifeAreas] replaces the list — a repository that merged
     * would have no way to express *unfile*, and the two callers that need
     * replacement (the editor, an area deletion) would each have to work around it.
     */
    fun fileGoalHere(goal: Goal) {
        if (areaId.isBlank() || areaId in goal.lifeAreaIds) return
        viewModelScope.launch {
            val result = goalRepository.setLifeAreas(goal.id, goal.lifeAreaIds + areaId)
            _message.value = if (result is Resource.Success) {
                "“${goal.title}” filed here"
            } else {
                "Could not file that goal"
            }
        }
    }

    /** Takes this area off [goal], leaving every other area it serves alone. */
    fun removeGoalFromArea(goal: Goal) {
        viewModelScope.launch {
            val remaining = goal.lifeAreaIds.filterNot { it == areaId }
            val result = goalRepository.setLifeAreas(goal.id, remaining)
            _message.value = when {
                result !is Resource.Success -> "Could not move that goal"
                remaining.isEmpty() -> "“${goal.title}” is now unfiled"
                else -> "“${goal.title}” removed from this area"
            }
        }
    }

    fun consumeMessage() { _message.value = null }
}

data class LifeAreaDetailUiState(
    val isLoading: Boolean = true,
    val area: LifeArea? = null,
    /** The goals filed under this area, newest first — the repository's order. */
    val goals: List<Goal> = emptyList(),
    /** Goals no area claims, offered for one-tap filing into this one. */
    val unfiledGoals: List<Goal> = emptyList(),
    /**
     * False when the id in the route resolves to nothing — an area deleted on
     * another device, or a stale link. Distinct from `area == null` while loading.
     */
    val areaExists: Boolean = false,
    val error: String? = null,
)
