package com.idomarhaim.goalpilot.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BackfillDurationsUseCase
import com.idomarhaim.goalpilot.domain.usecase.BuildSuccessFailureRunUseCase
import com.idomarhaim.goalpilot.domain.usecase.DurationProposal
import com.idomarhaim.goalpilot.domain.usecase.SuccessFailureRun
import com.idomarhaim.goalpilot.domain.usecase.SuccessRange
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocation
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocationUseCase
import com.idomarhaim.goalpilot.domain.usecase.TimeTrend
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
    private val taskRepository: TaskRepository,
    lifeAreaRepository: LifeAreaRepository,
    occurrenceRepository: OccurrenceRepository,
    private val recommendationRepository: RecommendationRepository,
    private val timeAllocation: TimeAllocationUseCase,
    private val backfillDurations: BackfillDurationsUseCase,
) : ViewModel() {

    private val controls = MutableStateFlow(AnalyticsControls())

    /**
     * The latest tasks, for the actions rather than the chart. Re-estimation runs
     * off a button press, and reaching back into a snapshot flow at that moment
     * would make the button wait on a network round trip it does not need.
     */
    @Volatile private var lastTasks: List<Task> = emptyList()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
        lifeAreaRepository.observeLifeAreas(includeArchived = true),
        // §2.1's occurrence documents -- what `C19`'s run counts (§4.7, `#64`).
        occurrenceRepository.observeOccurrences(),
        controls,
    ) { goals, tasks, areas, occurrences, controls ->
        lastTasks = tasks
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
            // Same window, same numbers, cut into columns — the buckets come from
            // the range so the trend can never disagree with the pie above it.
            trend = timeAllocation.trend(
                buckets = controls.range.buckets(today = today),
                allocation = allocation,
                goals = goals,
                tasks = tasks,
            ),
            // A slice selected in one range can be absent from the next; keeping
            // the id would leave the chart dimmed with nothing highlighted.
            selectedSliceId = controls.selectedSliceId
                ?.takeIf { id -> allocation.slices.any { it.areaId.sliceKey() == id } },
            // §4.7's run, over EVERY goal -- which is what makes this screen the one
            // place `C17`'s asymmetry can be stated. The donut divides a task's
            // minutes between its areas; the run does not divide a success at all,
            // so the two totals are not meant to agree and the card says so.
            //
            // Its own range, not `controls.range`. The donut's window is a calendar
            // period (day/week/month/quarter/year) and the run's is §4.7's
            // `30 days · 8 weeks · 6 months`, which is a different question with a
            // different default -- tying them would silently answer one with the
            // other.
            run = BuildSuccessFailureRunUseCase(
                goals = goals,
                tasks = tasks,
                occurrences = occurrences,
                range = controls.successRange,
                today = today,
                now = LocalDateTime.now(),
                zone = ZoneId.systemDefault(),
            ),
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

    /** §4.7's window filter, which is separate from the donut's range on purpose. */
    fun selectSuccessRange(range: SuccessRange) {
        controls.update { it.copy(successRange = range) }
    }

    // ── Duration back-fill (making the pie measured, not inferred) ────

    private val _backfill = MutableStateFlow(BackfillState())
    val backfill = _backfill.asStateFlow()

    private val _message = MutableStateFlow<AnalyticsMessage?>(null)
    val message = _message.asStateFlow()

    /**
     * Asks the model how long each un-estimated task really takes, and opens a
     * review sheet with the answers.
     *
     * Nothing is written until the user confirms — identical policy to smart add,
     * the Google Tasks import and the Health Connect sync, and for the identical
     * reason (spec §8: LLM output can be inconsistent). Capped per run at
     * [BackfillDurationsUseCase.MAX_PER_RUN] because each row costs one `scoreTask`
     * call against a 30 requests/minute free tier.
     */
    fun reEstimateDurations() {
        if (_backfill.value.isLoading || _backfill.value.isSaving) return
        viewModelScope.launch {
            _backfill.value = BackfillState(isVisible = true, isLoading = true)
            val window = controls.value.range.window()
            // Unlimited first, so the sheet can say "15 of 42" rather than
            // implying the cap is the whole of the problem.
            val candidates = backfillDurations(lastTasks, window, limit = Int.MAX_VALUE)
            if (candidates.isEmpty()) {
                _backfill.value = BackfillState(
                    isVisible = true,
                    error = AnalyticsMessage.AllTasksAlreadyEstimated,
                )
                return@launch
            }
            val batch = candidates.take(BackfillDurationsUseCase.MAX_PER_RUN)
            val proposals = coroutineScope {
                batch.map { candidate ->
                    async {
                        val estimate =
                            when (val r = recommendationRepository.scoreTask(candidate.title)) {
                                is Resource.Success -> r.data
                                else -> null
                            }
                        backfillDurations.propose(candidate, estimate)
                    }
                }.awaitAll()
            }
            _backfill.value = BackfillState(
                isVisible = true,
                proposals = proposals,
                totalCandidates = candidates.size,
            )
        }
    }

    fun toggleDurationProposal(taskId: String) {
        _backfill.update { state ->
            state.copy(
                proposals = state.proposals.map {
                    if (it.taskId == taskId) it.copy(selected = !it.selected) else it
                },
            )
        }
    }

    /**
     * Writes the accepted durations, and only the durations.
     *
     * `upsertTask` replaces the whole document, so each write starts from the task
     * as it currently stands and changes one field. Points in particular are left
     * exactly as they were: they were already awarded when the task was ticked, and
     * rewriting them would make un-ticking refund a different number.
     */
    fun confirmBackfill() {
        val state = _backfill.value
        if (state.isSaving) return
        val chosen = state.proposals.filter { it.selected }
        if (chosen.isEmpty()) {
            _backfill.value = BackfillState()
            return
        }
        viewModelScope.launch {
            _backfill.update { it.copy(isSaving = true) }
            val byId = lastTasks.associateBy { it.id }
            var saved = 0
            for (proposal in chosen) {
                val task = byId[proposal.taskId] ?: continue
                val result = taskRepository.upsertTask(
                    task.copy(
                        estimatedMinutes = proposal.proposedMinutes,
                        // The whole point of the run: a duration that came from the
                        // model is stamped as one. Only unselected rows lack an
                        // answer, and those never reach here.
                        durationSource = DurationSource.AI,
                    ),
                )
                if (result is Resource.Success) saved++
            }
            _backfill.value = BackfillState()
            _message.value = if (saved == 0) {
                AnalyticsMessage.UpdateFailed
            } else {
                AnalyticsMessage.Updated(saved)
            }
        }
    }

    fun dismissBackfill() { _backfill.value = BackfillState() }

    fun consumeMessage() { _message.value = null }

    private data class AnalyticsControls(
        val range: AnalyticsRange = AnalyticsRange.WEEK,
        val selectedSliceId: String? = null,
        /** §4.7's `30 days · 8 weeks · 6 months`, default 8 weeks. */
        val successRange: SuccessRange = SuccessRange.DEFAULT,
    )
}

/**
 * The re-estimation review sheet, before anything is written.
 *
 * [totalCandidates] counts every task that could be re-estimated, not just the
 * ones in this run — the difference is the honest way to say "run it again".
 */
data class BackfillState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val proposals: List<DurationProposal> = emptyList(),
    val totalCandidates: Int = 0,
    val error: AnalyticsMessage? = null,
) {
    /** Rows a model actually answered for; the rest carry no duration at all. */
    val answeredCount: Int get() = proposals.count { !it.noModelAnswer }
    val selectedCount: Int get() = proposals.count { it.selected }
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
    val trend: TimeTrend = TimeTrend(),
    val selectedSliceId: String? = null,
    /** `C19`'s success/failure run over every goal — §4.7, `#64`. */
    val run: SuccessFailureRun = SuccessFailureRun(range = SuccessRange.DEFAULT),
    val error: String? = null,
) {
    /**
     * Completed tasks in this window whose duration is still a guess from their
     * difficulty. The number the re-estimation button exists to drive to zero.
     */
    val inferredTaskCount: Int
        get() = (allocation.completedTasks - allocation.estimatedTaskCount).coerceAtLeast(0)
}

/**
 * Something the analytics screen needs to say, **before** it is words.
 *
 * The ViewModel used to hold the English directly (`"Updated 3 task durations"`).
 * That is unreachable by a language switch and untranslatable besides — the
 * `when (saved) { 1 -> …; else -> … }` it replaced is an English plural rule
 * baked into Kotlin, and Hebrew has one/two/many/other.
 *
 * So the ViewModel names the *situation* and `AnalyticsScreen` resolves it
 * against `res/`, where the plural rules live per language and the count can be
 * direction-isolated on the way in (§4.8).
 */
sealed interface AnalyticsMessage {

    /** Nothing to re-estimate: every task already carries a model estimate. */
    data object AllTasksAlreadyEstimated : AnalyticsMessage

    /** The write failed for every chosen row. */
    data object UpdateFailed : AnalyticsMessage

    /** [count] task durations were written. Never zero — that is [UpdateFailed]. */
    data class Updated(val count: Int) : AnalyticsMessage
}
