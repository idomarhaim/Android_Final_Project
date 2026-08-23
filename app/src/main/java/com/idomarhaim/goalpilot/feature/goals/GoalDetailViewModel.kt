package com.idomarhaim.goalpilot.feature.goals

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalStructure
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskCompletion
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.ui.navigation.Routes
import java.time.LocalDateTime
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
    private val preferences: AppPreferencesRepository,
    lifeAreaRepository: LifeAreaRepository,
    occurrenceRepository: OccurrenceRepository,
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
        // `#67`: the toggles and the occurrence documents ride one arm, because `combine` is
        // typed to five and this screen already used all five. Same move `CalendarViewModel`
        // makes with its `Controls` -- and it keeps the two things that are *not* about the goal
        // together, which reads better than a sixth positional parameter would.
        combine(_pendingToggles, occurrenceRepository.observeOccurrences()) { pending, stored ->
            pending to stored
        },
    ) { goal, tasks, entries, areas, (pending, stored) ->
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
            occurrences = stored,
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
        difficulty: Difficulty,
        minutes: Int,
        durationSource: DurationSource,
        alreadyDone: Boolean = false,
        occurrence: Occurrence? = null,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = taskRepository.upsertTask(
                Task(
                    // §1.5, `#55`: one edge to this goal, declaring no contribution. The row
                    // asks how long and how hard, never what the task is worth **in the
                    // goal's own word** — and `1.0` in place of that silence is exactly the
                    // default §1.5 deleted.
                    goalEdges = goalEdgesOf(goalId),
                    title = title.trim(),
                    difficulty = difficulty,
                    // `#7`/`R6`, the same create-and-complete the dashboard's quick add makes,
                    // through the same one commit: `upsertTask` mints the fact with
                    // `TaskCompletion.of` and batches it with the task write. Never
                    // upsert-then-setDone. The placeholder below carries no timestamp on
                    // purpose — `TaskCompletion.of` supplies `now` at the write, which is the
                    // only place that knows it.
                    completion = if (alreadyDone) CompletionFact() else null,
                    estimatedMinutes = TaskDuration.sanitize(minutes),
                    durationSource = durationSource,
                    // §2.2, `#56`. Null for the great majority of tasks, which have no *when*
                    // at all -- nothing here invents one from the creation date. Arming the
                    // reminder is not this call's job: `GoalPilotApp` observes the task list
                    // and re-derives the whole schedule, so a task created on another device
                    // is armed here too.
                    occurrence = occurrence,
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
     * Asks the LLM (via the `scoreTask` Cloud Function) **how demanding** a task is and how
     * long it takes — spec §3.3 A's estimate group, plus the duration the time-allocation
     * chart is built from. The result lands in [GoalDetailActionState.suggestedDifficulty] /
     * [GoalDetailActionState.suggestedMinutes] for the add-task row to pick up.
     *
     * It used to be called `suggestPoints` and to carry a number. §3.3 A: *"There is no
     * `points` field, and there never will be"* — the model judges, the app computes (§0.5,
     * §1.4). On failure the repository returns `ROUTINE` with no minutes, which is the
     * **absence** of a judgement rather than a guess at one; there is no offline substitute,
     * because difficulty is a judgement about the work and the app cannot make one.
     */
    fun suggestEstimate(title: String) {
        if (title.isBlank()) {
            _action.update { it.copy(message = "Type the task first") }
            return
        }
        viewModelScope.launch {
            _action.update {
                it.copy(isScoring = true, suggestedDifficulty = null, suggestedMinutes = null)
            }
            val estimate = when (val result = recommendationRepository.scoreTask(title.trim())) {
                is Resource.Success -> result.data
                else -> null
            }
            _action.update {
                it.copy(
                    isScoring = false,
                    suggestedDifficulty = estimate?.difficulty,
                    suggestedMinutes = estimate?.minutes,
                )
            }
        }
    }

    fun consumeSuggestedEstimate() =
        _action.update { it.copy(suggestedDifficulty = null, suggestedMinutes = null) }

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

    // ── §1.3's measure proposal (`C22` #44, #65) ─────────────────────────────

    /**
     * The offer currently on screen, or `null` for §3.4's silent row.
     *
     * Held here rather than derived in the `combine` above, for one reason that is
     * the feature's whole tone rule: **the offer must not move under the user's
     * finger.** A derived value would recompute on every task edit, so ticking a
     * step could change the proposal's own number — or make it vanish — while it
     * is being read. §1.3 asks for an offer, and an offer that rewrites itself
     * mid-consideration is a nag.
     *
     * So it is computed **once per goal**, by [loadMeasureProposal], and then left
     * alone until the goal changes or the user answers it.
     */
    private val _measureProposal = MutableStateFlow<MeasureProposal?>(null)
    val measureProposal: StateFlow<MeasureProposal?> = _measureProposal.asStateFlow()

    /**
     * Whether the **absence note** should be drawn at all.
     *
     * A separate flag from [measureProposal] because §1.3 makes them separate
     * claims: *"the absence is stated as legal **before** anything is offered"*, so
     * the note is shown for every unmeasured goal — including one with no proposal
     * under it, which is the state that has to read as deliberate rather than
     * broken. It goes false the moment a measure exists.
     */
    private val _showUnmeasuredNote = MutableStateFlow(false)
    val showUnmeasuredNote: StateFlow<Boolean> = _showUnmeasuredNote.asStateFlow()

    /**
     * Guards the once-per-goal rule against recomposition and against the
     * snapshot listener firing repeatedly for one goal.
     *
     * The goal id it last ran for, so a genuine navigation to another goal still
     * loads — this is a *once per goal*, not a *once per ViewModel*.
     */
    private var measureProposalLoadedFor: String? = null

    /**
     * Decides whether to offer, and offers.
     *
     * The gate is three conditions and the order matters, because each is cheaper
     * than the one after it:
     *
     *  1. **Already answered for this goal in this ViewModel** — no work.
     *  2. **Dismissed** (§1.3, permanent) — no work, and specifically **no network
     *     call**: a dismissed goal must not cost a request against the free tier's
     *     30 RPM, or the dismissal would only be silencing the *screen*.
     *  3. **Not eligible** — the goal has a measure, or has no structure to compute
     *     a target from. §3.4's last row: nothing at all, silently.
     *
     * Only then does it call. And the call's failure is not a branch here — the
     * repository returns an empty list, and the `?:` below runs §3.4's mechanical
     * proposal over the same structure. That is the arrangement §0.1 asks for: the
     * non-AI half is the default path with the model as an improvement on its
     * wording, not a primary path with a degraded backup.
     */
    fun loadMeasureProposal() {
        val goal = uiState.value.goal ?: return
        if (measureProposalLoadedFor == goal.id) return
        measureProposalLoadedFor = goal.id

        val unmeasured = goal.measure == null
        _showUnmeasuredNote.value = unmeasured
        if (!unmeasured) {
            _measureProposal.value = null
            return
        }
        if (preferences.isMeasureProposalDismissed(goal.id)) {
            _measureProposal.value = null
            return
        }

        val structure = ProposeMeasureUseCase.structureOf(uiState.value.tasks, LocalDateTime.now())
        if (!ProposeMeasureUseCase.isEligible(goal, structure)) {
            _measureProposal.value = null
            return
        }

        viewModelScope.launch {
            val remote = recommendationRepository
                .proposeMeasures(listOf(goal), mapOf(goal.id to structure))
                .let { (it as? Resource.Success)?.data.orEmpty() }
                .firstOrNull { it.goalId == goal.id }
            // The model's wording if it produced a whole one, the app's arithmetic
            // otherwise. Both already carry a target computed here (§3.3 E), so the
            // screen cannot tell which path filled the number — which is exactly
            // §3.4's claim that this fallback is not a degraded version of itself.
            _measureProposal.value = remote ?: ProposeMeasureUseCase.mechanical(goal, structure)
        }
    }

    /**
     * Applies the offer — the **only** path from a proposal to a goal (§1.3: it
     * *never auto-applies*).
     *
     * Writes the kind and the word always, and the target **only when the app
     * computed one**. A `USER` proposal leaves [Goal.targetValue] untouched rather
     * than writing a placeholder: §3.3 E forbids inventing that number, and
     * writing one here would be inventing it at the last possible moment.
     *
     * Does **not** dismiss. An accepted goal stops being offered because it now
     * has a measure — a fact about the goal — and recording a suppression as well
     * would leave a second, invisible reason for the offer's absence that would
     * survive the measure being removed again.
     */
    fun acceptMeasureProposal() {
        val goal = uiState.value.goal ?: return
        val proposal = _measureProposal.value ?: return
        if (proposal.goalId != goal.id) return

        viewModelScope.launch {
            val updated = goal.copy(
                measure = proposal.toMeasure(),
                targetValue = if (proposal.hasTarget) proposal.target!! else goal.targetValue,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            when (val result = goalRepository.upsertGoal(updated)) {
                is Resource.Success -> {
                    _measureProposal.value = null
                    _showUnmeasuredNote.value = false
                }

                is Resource.Error -> _action.update { it.copy(message = result.message) }
                else -> Unit
            }
        }
    }

    /**
     * §1.3's dismissal: **permanent for this goal, not snoozed**, *because a
     * default that re-asks is not a default*.
     *
     * There is no un-dismiss and no timer. The manual path — the goal editor —
     * always exists, so nothing becomes unreachable; it is simply never
     * volunteered again. The absence note stays: dismissing the *offer* does not
     * make the absence stop being legal, and removing the line that says so would
     * leave the goal looking incomplete, which is the whole failure mode.
     */
    fun dismissMeasureProposal() {
        val goalId = uiState.value.goal?.id ?: return
        preferences.dismissMeasureProposal(goalId)
        _measureProposal.value = null
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
    return map { task ->
        // `isDone` is `completion != null` since `#55`, so an optimistic tick has to produce
        // a fact rather than flip a flag. `TaskCompletion.of` is the one minting function, so
        // the preview is priced by exactly the arithmetic the write is about to bank.
        inFlight[task.id]?.let { done ->
            if (done) task.copy(completion = TaskCompletion.of(task, System.currentTimeMillis()))
            else task.copy(completion = null)
        } ?: task
    }
        .sortedWith(compareBy({ it.isDone }, { -it.createdAtEpochMillis }))
}

/**
 * Moves the goal's progress by what the in-flight completions are worth, so the
 * ring and the "3 / 100 %" caption travel with the checkbox instead of lagging it.
 *
 * The arithmetic mirrors what the goal is about to read once the write lands —
 * `DerivedProgress` sums **the declared contribution of each edge** over completed tasks
 * (§1.5, `#55`), so an in-flight tick is worth exactly the edges pointing at this goal, and
 * **nothing at all** when they declare nothing — because anything else shows the user a
 * number the repository is not about to agree with. A task created after `#55` declares no
 * contribution, so ticking it moves the ring by zero; that is the spec's answer, not a
 * dropped write, and §1.5 puts the shortfall in words instead.
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
        if (task.isDone == done) return@sumOf 0.0
        val worth = task.goalEdges
            .filter { it.goalId == id }
            .sumOf { it.contribution ?: 0.0 }
        if (done) worth else -worth
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
    /**
     * §2.1's stored occurrences, **for one purpose only** — `#67`'s confirm.
     *
     * The whole collection, not this goal's: `Deletion.ofTask` takes it whole and filters by
     * task id, for the reason `BuildSuccessFailureRunUseCase` gives — pre-filtering here would
     * be a second place the join could be got wrong. Nothing on this screen renders it; a task
     * row's *when* is still `Task.occurrence`.
     */
    val occurrences: List<ScheduledOccurrence> = emptyList(),
    val error: String? = null,
)

data class GoalDetailActionState(
    val isSubmitting: Boolean = false,
    val isScoring: Boolean = false,
    /** One-shot LLM difficulty judgement; the add-task row consumes and clears it. */
    val suggestedDifficulty: Difficulty? = null,
    /** Duration estimate from the same call, in minutes. */
    val suggestedMinutes: Int? = null,
    val message: String? = null,
)
