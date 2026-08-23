package com.idomarhaim.goalpilot.feature.lifeareas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildSuccessFailureRunUseCase
import com.idomarhaim.goalpilot.domain.usecase.SuccessFailureRun
import com.idomarhaim.goalpilot.domain.usecase.SuccessRange
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
    private val lifeAreaRepository: LifeAreaRepository,
    taskRepository: TaskRepository,
    occurrenceRepository: OccurrenceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val areaId: String = savedStateHandle[Routes.ARG_LIFE_AREA_ID] ?: ""

    /**
     * The `30 days · 8 weeks · 6 months` filter (§4.7, `#64`), default 8 weeks.
     *
     * A control rather than a stored preference: §4.7 is explicit that the window
     * is *"a filter over history, not decay of it"*, and a persisted one would be
     * a second place the same question is answered.
     */
    private val successRange = MutableStateFlow(SuccessRange.DEFAULT)

    val uiState: StateFlow<LifeAreaDetailUiState> = combine(
        // Archived areas included: this screen is reachable from a link that may
        // outlive the area's visibility on the list, and "the area you asked for
        // is archived" is a better answer than "no such area".
        lifeAreaRepository.observeLifeAreas(includeArchived = true),
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
        // §2.1's occurrence documents. Deliberately the whole collection: it holds
        // one document per instance the user TOUCHED, not one per instance, so it
        // stays small by construction and the snapshot is cache-served
        // (`OccurrenceRepository.observeOccurrences`).
        occurrenceRepository.observeOccurrences(),
        successRange,
    ) { areas, goals, tasks, occurrences, range ->
        val area = areas.firstOrNull { it.id == areaId }
        // Resolved per emission rather than once at construction, so the window
        // follows the calendar for a screen left open across midnight -- the same
        // reason AnalyticsViewModel resolves its own `today` here.
        val today = LocalDate.now()
        val mine = goals.filter { areaId in it.lifeAreaIds }
        LifeAreaDetailUiState(
            isLoading = false,
            area = area,
            // Deliberately computed even when the area is gone, so the screen can
            // say how many goals a deletion would unfile rather than showing an
            // empty page.
            goals = mine,
            unfiledGoals = goals.filter { goal ->
                goal.lifeAreaIds.none { id -> areas.any { it.id == id } }
            },
            areaExists = area != null,
            tasks = tasks,
            // §4.7's run, over THIS area's goals. A task serving two areas is
            // counted whole here and whole under the other one, because each
            // screen asks about its own goals and nothing divides a success.
            run = BuildSuccessFailureRunUseCase(
                goals = mine,
                tasks = tasks,
                occurrences = occurrences,
                range = range,
                today = today,
                now = LocalDateTime.now(),
                zone = ZoneId.systemDefault(),
            ),
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

    /**
     * Deletes the area this screen is about — `#67`.
     *
     * ### It reports nothing on success, and that is deliberate
     *
     * The screen navigates back before this runs, so a success message would be posted to a
     * `SnackbarHostState` that is being torn down and would never be read. What the person needs
     * to know is the list they land on, which no longer has the area in it. A **failure** is
     * different and is not silent — but it cannot be shown here either, so it rides
     * `LifeAreasViewModel.deleteArea`'s own channel on the screen that is still alive. That is
     * why this returns rather than posting: see the screen's `onConfirm`.
     *
     * ### Nothing is unfiled here
     *
     * `LifeAreaRepositoryImpl.deleteLifeArea` already strips the area from every goal that
     * carries it, in the right order and with the reasoning written down. Repeating any of it
     * here would be a second author for one write.
     */
    fun deleteArea() {
        if (areaId.isBlank()) return
        viewModelScope.launch { lifeAreaRepository.deleteLifeArea(areaId) }
    }

    /**
     * §4.7's `Let it go` for one of this area's goals — `#67`.
     *
     * ### Deleting a goal is not the same act as removing it from this area
     *
     * [removeGoalFromArea] is one field write and the goal survives; this ends the goal. They sit
     * on the same screen and must never be reached by the same gesture, which is why one is a row
     * menu item reading *Remove from this area* and the other is a red button on the run card
     * reading `Let it go`, behind a confirm that names what goes.
     */
    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _message.value = when (goalRepository.deleteGoal(goalId)) {
                is Resource.Success -> "Goal deleted"
                else -> "Could not delete that goal"
            }
        }
    }

    /** §4.7's window filter. Re-reads the same history over a different span. */
    fun selectSuccessRange(range: SuccessRange) {
        successRange.update { range }
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
     * Every task the user has — read by the run above, and by `#67`'s confirm.
     *
     * The whole list rather than this area's, because `Deletion.ofGoal` counts the tasks filed
     * under ONE goal and a pre-filtered copy would be a second place that join could be got
     * wrong. Nothing renders it.
     */
    val tasks: List<Task> = emptyList(),
    /**
     * False when the id in the route resolves to nothing — an area deleted on
     * another device, or a stale link. Distinct from `area == null` while loading.
     */
    val areaExists: Boolean = false,
    /**
     * `C19`'s success/failure run for this area — §4.7, `#64`.
     *
     * Defaulted to an empty run rather than made nullable: the screen shows the
     * card while loading nothing rather than branching, and an empty run reads
     * as *nothing has been due here yet*, which is the honest thing to say about
     * an area whose goals have not arrived from the snapshot yet.
     */
    val run: SuccessFailureRun = SuccessFailureRun(range = SuccessRange.DEFAULT),
    val error: String? = null,
)
