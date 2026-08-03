package com.idomarhaim.goalpilot.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocation
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

/**
 * BONUS (spec §6): rich analytics.
 *
 * The headline chart answers *"what share of my life am I actually spending on
 * each area of it?"* — completed tasks, weighted by the LLM's duration estimate,
 * followed up their goal to the life area the goal belongs to, over a chosen
 * calendar window (day / week / month / quarter / year).
 *
 * The arithmetic lives in [TimeAllocationUseCase]; this class only decides *which*
 * window and keeps the selected slice honest.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    goalRepository: GoalRepository,
    taskRepository: TaskRepository,
    lifeAreaRepository: LifeAreaRepository,
    private val timeAllocation: TimeAllocationUseCase,
) : ViewModel() {

    private val controls = MutableStateFlow(AnalyticsControls())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
        lifeAreaRepository.observeLifeAreas(includeArchived = true),
        controls,
    ) { goals, tasks, areas, controls ->
        // Resolved per emission rather than once at construction, so the window
        // follows the calendar for a session left open across midnight.
        val today = LocalDate.now()
        val allocation = timeAllocation(
            window = controls.range.window(today = today),
            lifeAreas = areas,
            goals = goals,
            tasks = tasks,
        )
        AnalyticsUiState(
            isLoading = false,
            range = controls.range,
            rangeLabel = controls.range.windowLabel(today),
            goals = goals,
            tasks = tasks,
            lifeAreas = areas.filterNot { it.isArchived },
            allocation = allocation,
            // A slice selected in one range can be absent from the next; keeping
            // the id would leave the chart dimmed with nothing highlighted.
            selectedSliceId = controls.selectedSliceId
                ?.takeIf { id -> allocation.slices.any { it.areaId.sliceKey() == id } },
        )
    }.catch { emit(AnalyticsUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalyticsUiState(isLoading = true),
        )

    fun selectRange(range: AnalyticsRange) {
        controls.update { it.copy(range = range, selectedSliceId = null) }
    }

    /** Pass null to clear. Selection is a property of the data, not of the canvas. */
    fun selectSlice(sliceId: String?) {
        controls.update { it.copy(selectedSliceId = sliceId) }
    }

    private data class AnalyticsControls(
        val range: AnalyticsRange = AnalyticsRange.WEEK,
        val selectedSliceId: String? = null,
    )
}

/**
 * Stable id for a slice, including the unassigned bucket whose area id is null.
 * The donut needs a non-null handle to compare against.
 */
fun String?.sliceKey(): String = this ?: UNASSIGNED_SLICE_ID

const val UNASSIGNED_SLICE_ID = "__unassigned__"

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val rangeLabel: String = "",
    val goals: List<Goal> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val lifeAreas: List<LifeArea> = emptyList(),
    val allocation: TimeAllocation = TimeAllocation(),
    val selectedSliceId: String? = null,
    val error: String? = null,
)
