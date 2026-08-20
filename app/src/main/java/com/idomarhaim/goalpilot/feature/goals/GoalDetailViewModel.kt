package com.idomarhaim.goalpilot.feature.goals

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
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
    lifeAreaRepository: LifeAreaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String = savedStateHandle[Routes.ARG_GOAL_ID] ?: ""

    /**
     * Task completions drawn on screen before Firestore has confirmed them, as
     * `task id -> the done state we optimistically rendered`.
     *
     * **The premise this was built on has expired, and the overlay is kept
     * deliberately rather than by inertia.** It existed because
     * [TaskRepository.setDone] was a **server-only** `runTransaction`, which never
     * touches the offline cache — so there was *no local write for the snapshot
     * listener to render* and the screen sat still for the whole round trip
     * (measured 2.24 s on a device; issue #3). `C20` (#42, spec §5.2) made `setDone`
     * a single `update()` on one document, so that is no longer true: the write
     * lands in the cache synchronously and the listener renders the tick at once,
     * radio on or off. **`Observed:` 2026-08-20** by reading
     * `TaskRepositoryImpl.setDone` at `731961b`.
     *
     * What still argues for keeping it: `update().await()` resolves on **server
     * ack**, not on the cache write, so a rejected write (rules, a stale doc) comes
     * back long after the tick is drawn, and [toggleTask] needs somewhere to take it
     * back from. `Inferred:` that the optimistic half is now largely redundant —
     * `inFlight` retires an entry the moment the cached snapshot arrives, which is
     * immediately — but the undo half is not. `Untested:` whether removing the
     * overlay entirely is safe; that is a behaviour change and its own ticket, not
     * part of #50 item 5.
     */
    private val _pendingToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    val uiState: StateFlow<GoalDetailUiState> = combine(
        goalRepository.observeGoal(goalId),
        taskRepository.observeTasks(goalId),
        progressRepository.observeEntries(goalId),
        lifeAreaRepository.observeLifeAreas(includeArchived = true),
        _pendingToggles,
    ) { goal, tasks, entries, areas, pending ->
        // An entry the snapshot listener has caught up with is retired here rather
        // than when setDone returns. That ordering matters: setDone's own
        // completion callback and the snapshot that reflects it arrive on two
        // different channels, so dropping the overlay on completion can re-render
        // the *old* state for the frames in between — a visible flicker on every
        // successful tap. Retiring it against the observed data cannot flicker,
        // because the observed data is already what the overlay was claiming.
        val inFlight = pending.filterNot { (id, done) ->
            tasks.any { it.id == id && it.isDone == done }
        }
        GoalDetailUiState(
            isLoading = false,
            goal = goal?.withOptimisticProgress(tasks, inFlight),
            tasks = tasks.withOptimisticDone(inFlight),
            entries = entries,
            // Resolved in the goal's own order, not the area list's: the order the
            // user filed them in is the order they read back.
            lifeAreas = goal?.lifeAreaIds
                ?.mapNotNull { id -> areas.firstOrNull { it.id == id } }
                .orEmpty(),
        )
    }.catch { emit(GoalDetailUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalDetailUiState(isLoading = true),
        )

    private val _action = MutableStateFlow(GoalDetailActionState())
    val action = _action.asStateFlow()

    /**
     * Creates the task, with the duration **and where it came from** (#9).
     *
     * [durationSource] is carried rather than inferred: the add-task row is the one
     * place that knows whether the number in the box was typed or estimated, and
     * §1.4 makes that difference permanent — a `USER` duration is never re-estimated
     * again for the life of the task.
     *
     * [alreadyDone] is `#7`. **Why this surface has it too, since `R6` names quick add:** an
     * add affordance that exists on one of two add rows and not the other reads as a bug
     * rather than as a decision. The narrower argument against it is real — here the task list
     * is on screen, so the new row's checkbox is one tap away and `R6`'s four navigations do
     * not arise — but this is also the surface where somebody logs three runs they have
     * already done into one goal, and the tap it saves is per task. It is the *same* control
     * writing through the *same* seam, so it is one feature with two doors rather than two
     * features that must be kept in step.
     */
    fun addTask(
        title: String,
        points: Int,
        minutes: Int,
        durationSource: DurationSource,
        alreadyDone: Boolean = false,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = taskRepository.upsertTask(
                Task(
                    goalId = goalId,
                    title = title.trim(),
                    points = points.coerceIn(1, 1000),
                    // `#7`/`R6`, the same create-and-complete the dashboard's quick add makes,
                    // through the same one `set()`: `TaskCompletion.stamp` inside `upsertTask`
                    // pairs `completedAtEpochMillis` with this flag. Never upsert-then-setDone.
                    isDone = alreadyDone,
                    estimatedMinutes = TaskDuration.sanitize(minutes),
                    durationSource = durationSource,
                ),
            )
            // No optimistic row is needed here, unlike toggleTask: upsertTask is an
            // ordinary set(), so Firestore applies it to the offline cache and the
            // snapshot listener renders it immediately, online or off. Only the
            // failure needed surfacing.
            if (result is Resource.Error) {
                _action.update { it.copy(message = result.message) }
            }
        }
    }

    /**
     * Asks the LLM (via the `scoreTask` Cloud Function) what a task is worth and
     * how long it takes — spec §6 Core "point scoring for tasks", plus the duration
     * the time-allocation chart is built from. The result lands in
     * [GoalDetailActionState.suggestedPoints] / [GoalDetailActionState.suggestedMinutes]
     * for the add-task row to pick up; on any failure the repository returns a
     * local estimate instead.
     */
    fun suggestPoints(title: String) {
        if (title.isBlank()) {
            _action.update { it.copy(message = "Type the task first") }
            return
        }
        viewModelScope.launch {
            _action.update { it.copy(isScoring = true, suggestedPoints = null, suggestedMinutes = null) }
            val estimate = when (val result = recommendationRepository.scoreTask(title.trim())) {
                is Resource.Success -> result.data
                else -> null
            }
            _action.update {
                it.copy(
                    isScoring = false,
                    suggestedPoints = estimate?.points,
                    suggestedMinutes = estimate?.minutes,
                )
            }
        }
    }

    fun consumeSuggestedPoints() =
        _action.update { it.copy(suggestedPoints = null, suggestedMinutes = null) }

    /**
     * Ticks the task on screen straight away, then asks Firestore to make it true —
     * and takes the tick back, with a message, if it could not.
     *
     * See [_pendingToggles] for what the two halves are now for. **There is no
     * offline pre-check here any more** — it was deleted with `ConnectivityMonitor`
     * under #50 item 5, because the reason for it (`runTransaction` could not reach
     * the cache and took a measured 7.9 s to fail) died with `C20`. An offline tap
     * is now an ordinary cached write: it ticks instantly and syncs when the radio
     * comes back. The undo below stays for a write the **server** rejects.
     */
    fun toggleTask(task: Task) {
        val target = !task.isDone
        viewModelScope.launch {
            _pendingToggles.update { it + (task.id to target) }
            val result = taskRepository.setDone(task.id, target)
            if (result is Resource.Error) {
                _pendingToggles.update { it - task.id }
                // Deliberately *not* result.message. Unlike a repository refusal
                // the user can act on ("no user with that code"), setDone's failure
                // text is a gRPC string — the device pass surfaced
                // "UNAVAILABLE: Unable to resolve host firestore.googleapis.com"
                // in a snackbar. The detail stays in logcat, where it is useful.
                _action.update { it.copy(message = SAVE_FAILED_MESSAGE) }
            }
            // On success the entry stays put and is retired by the uiState transform
            // once the snapshot listener catches up — see the comment there for why
            // clearing it here would flicker.
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            // delete() is an ordinary write, so the row disappears from the cache
            // immediately and needs no optimistic handling — but the failure was
            // being discarded exactly as toggleTask's was (issue #3).
            val result = taskRepository.deleteTask(taskId)
            if (result is Resource.Error) {
                _action.update { it.copy(message = result.message) }
            }
        }
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

    /**
     * One tap of a fill button — `R25`, #11.
     *
     * It is [logProgress] with no note, no photo and no dialog, and that is the
     * whole feature: the amount comes from
     * [FillLadder][com.idomarhaim.goalpilot.domain.model.FillLadder] rather than
     * from a keyboard, so a repeat tap costs one gesture instead of five.
     *
     * **No optimistic overlay, unlike [toggleTask].** `logProgress` is an ordinary
     * `add()`, so Firestore writes it to the offline cache synchronously and the
     * snapshot listener redraws the tally on the next frame whether the radio is
     * on or not — the same reason [addTask] needs none. What the tally shows is
     * the sum over entries every other screen shows (§4.6), so there is no second
     * counter that could disagree with it.
     *
     * **The snackbar is deliberately not fired on success**, unlike the dialog's
     * *"Progress logged"*. A button meant to be tapped four times in a row would
     * otherwise queue four identical snackbars over the row being tapped. The
     * tally moving **is** the confirmation; only a failure needs words.
     *
     * **And [GoalDetailActionState.isSubmitting] is deliberately not touched.**
     * It gates the dialog's confirm button, which is right there — the dialog can
     * upload an image and must not be double-submitted. A fill tap is the
     * opposite case: `logProgress` resolves on **server ack** while the cached
     * write and the tally are immediate, so raising the flag here would disable
     * the row until the network answered, on the one control whose premise is
     * that it is tapped repeatedly. [FillButtonRow] carries the full reasoning.
     */
    fun logFill(amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val entry = ProgressEntry(goalId = goalId, value = amount)
            val result = progressRepository.logProgress(entry, imageUri = null)
            if (result is Resource.Error) {
                _action.update { it.copy(message = result.message) }
            }
        }
    }

    fun archiveGoal() {
        viewModelScope.launch {
            when (goalRepository.setArchived(goalId, true)) {
                is Resource.Error -> _action.update { it.copy(message = "Could not archive goal") }
                else -> Unit
            }
        }
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

    companion object {
        const val SAVE_FAILED_MESSAGE = "Couldn't save that — check your connection"
    }
}

/**
 * Applies the not-yet-confirmed completions over the observed task list, then
 * re-sorts on the same key the repository uses.
 *
 * Re-sorting is deliberate: the optimistic list should be a faithful preview of
 * what the server is about to return, so the row settles into its final position
 * as it is ticked rather than jumping a second time two seconds later.
 */
private fun List<Task>.withOptimisticDone(inFlight: Map<String, Boolean>): List<Task> {
    if (inFlight.isEmpty()) return this
    return map { task -> inFlight[task.id]?.let { task.copy(isDone = it) } ?: task }
        .sortedWith(compareBy({ it.isDone }, { -it.createdAtEpochMillis }))
}

/**
 * Moves the goal's progress by what the in-flight completions are worth, so the
 * ring and the "3 / 100 %" caption travel with the checkbox instead of lagging it.
 *
 * The arithmetic mirrors what the goal is about to read once the write lands —
 * `DerivedProgress` sums `progressContribution` over completed tasks, so an in-flight
 * tick is worth exactly that — because anything else shows the user a number the
 * repository is not about to agree with.
 *
 * **No clamp**, and that is the third of the four §1.5 deletes. It used to pin the
 * preview to `0..targetValue` to match a write-site clamp that no longer exists;
 * keeping it would have made the optimistic number disagree with the settled one
 * on precisely the goals that are past their target — the case the clamp was
 * hiding in the first place.
 */
private fun Goal.withOptimisticProgress(tasks: List<Task>, inFlight: Map<String, Boolean>): Goal {
    if (inFlight.isEmpty()) return this
    val byId = tasks.associateBy { it.id }
    val delta = inFlight.entries.sumOf { (taskId, done) ->
        val task = byId[taskId] ?: return@sumOf 0.0
        if (task.isDone == done) 0.0
        else if (done) task.progressContribution else -task.progressContribution
    }
    if (delta == 0.0) return this
    return copy(currentValue = currentValue + delta)
}

data class GoalDetailUiState(
    val isLoading: Boolean = false,
    val goal: Goal? = null,
    val tasks: List<Task> = emptyList(),
    val entries: List<ProgressEntry> = emptyList(),
    /**
     * The areas the goal is filed under, resolved. Empty when unfiled *or* when
     * every id it carries points at an area that is gone — the two are the same
     * thing to a reader, and §1.2 makes the empty collection the honest answer.
     */
    val lifeAreas: List<LifeArea> = emptyList(),
    val error: String? = null,
)

data class GoalDetailActionState(
    val isSubmitting: Boolean = false,
    val isScoring: Boolean = false,
    /** One-shot LLM point estimate; the add-task row consumes and clears it. */
    val suggestedPoints: Int? = null,
    /** Duration estimate from the same call, in minutes. */
    val suggestedMinutes: Int? = null,
    val message: String? = null,
)
