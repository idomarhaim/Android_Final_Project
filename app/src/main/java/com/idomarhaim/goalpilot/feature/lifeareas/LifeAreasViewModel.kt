package com.idomarhaim.goalpilot.feature.lifeareas

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksClient
import com.idomarhaim.goalpilot.data.tasks.TaskListsResult
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.TasksConsent
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildLifeAreaProposalsUseCase
import com.idomarhaim.goalpilot.domain.usecase.LifeAreaProposal
import com.idomarhaim.goalpilot.domain.usecase.LifeAreaSyncAction
import com.idomarhaim.goalpilot.domain.usecase.ReorderLifeAreasUseCase
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

/**
 * The life-areas screen: define the areas of your life, sync them from your
 * Google Tasks lists, and file goals under them (spec §1, §6 nice-to-have).
 *
 * Same write policy as everywhere else in this app: the Google sync *proposes*,
 * the user confirms, nothing is written before that.
 */
@HiltViewModel
class LifeAreasViewModel @Inject constructor(
    private val lifeAreaRepository: LifeAreaRepository,
    private val goalRepository: GoalRepository,
    private val googleTasksClient: GoogleTasksClient,
    private val buildProposals: BuildLifeAreaProposalsUseCase,
    private val reorderAreas: ReorderLifeAreasUseCase,
) : ViewModel() {

    @Volatile private var lastAreas: List<LifeArea> = emptyList()

    val uiState: StateFlow<LifeAreasUiState> = combine(
        lifeAreaRepository.observeLifeAreas(),
        goalRepository.observeGoals(),
    ) { areas, goals ->
        lastAreas = areas
        LifeAreasUiState(
            isLoading = false,
            // A goal serving two areas is counted by both — §4.7's "counts in full
            // in every area", and the reason this is a per-area filter rather than
            // one `groupBy`.
            rows = areas.map { area ->
                LifeAreaRow(area = area, goalCount = goals.count { area.id in it.lifeAreaIds })
            },
            // A goal whose areas were all deleted elsewhere counts as unfiled too,
            // which is why this is "no id that resolves" rather than "empty".
            unfiledGoals = goals.filter { goal ->
                goal.lifeAreaIds.none { id -> areas.any { it.id == id } }
            },
        )
    }.catch { emit(LifeAreasUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LifeAreasUiState(isLoading = true),
        )

    private val _editor = MutableStateFlow(LifeAreaEditorState())
    val editor = _editor.asStateFlow()

    private val _sync = MutableStateFlow(LifeAreaSyncState())
    val sync = _sync.asStateFlow()

    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent = _consentIntent.asStateFlow()

    /**
     * Whether the Tasks scope is actually held. Null until the first check comes
     * back. This screen reads the *same* scope as the dashboard import, so it
     * carries the same defect (#36) and the same fix: sign-in can succeed with
     * *"View your tasks"* left unticked, and the sync card must say so rather
     * than re-offering a generic grant prompt.
     */
    private val _tasksConsent = MutableStateFlow<TasksConsent?>(null)
    val tasksConsent = _tasksConsent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    /**
     * Re-reads the consent state on **every** screen entry (#36).
     *
     * Deliberately *not* guarded by a once-per-ViewModel flag the way
     * `ensureRecommendations` is: that guard exists to stop an expensive network
     * call re-firing on back-navigation, and this is a local read. Guarding it
     * would leave the card accusing a user who granted the scope on the other
     * surface and navigated back — the ViewModel survives in the back stack even
     * though the composable is recreated.
     */
    fun refreshTasksConsent() {
        viewModelScope.launch { _tasksConsent.value = googleTasksClient.consentState() }
    }

    // ── Editor ────────────────────────────────────────────────────────

    /** Pass null to create. A new area gets the least-crowded colour up front. */
    fun openEditor(area: LifeArea?) {
        _editor.value = if (area == null) {
            LifeAreaEditorState(
                isVisible = true,
                colorHex = LifeAreaPalette.nextHex(lastAreas.map { it.colorHex }),
                sortOrder = (lastAreas.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            )
        } else {
            LifeAreaEditorState(
                isVisible = true,
                isEdit = true,
                id = area.id,
                name = area.name,
                colorHex = area.colorHex,
                iconKey = area.iconKey,
                sortOrder = area.sortOrder,
                googleListId = area.googleListId,
                createdAt = area.createdAtEpochMillis,
            )
        }
    }

    fun onNameChange(value: String) {
        _editor.update {
            it.copy(
                name = value,
                // Only while the user has not overridden it: retyping the name of
                // a synced list should still land on a sensible icon, but a chosen
                // icon must never be swapped out from under them.
                iconKey = if (it.iconTouched) it.iconKey else LifeAreaPalette.iconKeyFor(value),
                error = null,
            )
        }
    }

    fun onColorChange(hex: String) = _editor.update { it.copy(colorHex = hex) }

    fun onIconChange(key: String) = _editor.update { it.copy(iconKey = key, iconTouched = true) }

    fun saveEditor() {
        val current = _editor.value
        if (current.name.isBlank()) {
            _editor.update { it.copy(error = "Give the area a name") }
            return
        }
        val clash = lastAreas.any {
            it.id != current.id && it.name.trim().equals(current.name.trim(), ignoreCase = true)
        }
        if (clash) {
            _editor.update { it.copy(error = "You already have an area with that name") }
            return
        }
        viewModelScope.launch {
            _editor.update { it.copy(isSaving = true, error = null) }
            val result = lifeAreaRepository.upsertLifeArea(
                LifeArea(
                    id = current.id,
                    name = current.name.trim(),
                    colorHex = current.colorHex,
                    iconKey = current.iconKey,
                    sortOrder = current.sortOrder,
                    googleListId = current.googleListId,
                    createdAtEpochMillis = current.createdAt,
                ),
            )
            when (result) {
                is Resource.Success -> {
                    _editor.value = LifeAreaEditorState()
                    _message.value = if (current.isEdit) "Life area updated" else "Life area added"
                }
                is Resource.Error ->
                    _editor.update { it.copy(isSaving = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun dismissEditor() { _editor.value = LifeAreaEditorState() }

    fun deleteArea(areaId: String) {
        viewModelScope.launch {
            _message.value = when (lifeAreaRepository.deleteLifeArea(areaId)) {
                is Resource.Success -> "Life area deleted; its goals are now unfiled"
                else -> "Could not delete that life area"
            }
        }
    }

    /**
     * Commits a drag (or a "move up"/"move down" accessibility action). Indices
     * are positions in the displayed list — the same order [uiState] emits.
     *
     * The screen has already moved the card locally, so this is only the write;
     * a no-op move never reaches Firestore at all.
     */
    fun moveArea(fromIndex: Int, toIndex: Int) {
        val changes = reorderAreas(lastAreas, fromIndex, toIndex)
        if (changes.isEmpty()) return
        viewModelScope.launch {
            if (lifeAreaRepository.reorderLifeAreas(changes) !is Resource.Success) {
                _message.value = "Could not save the new order"
            }
        }
    }

    /**
     * Files a goal under an area, or unfiles it when [areaId] is null.
     *
     * This caller only ever reaches goals in `unfiledGoals`, which by definition
     * hold no area that resolves, so replacing the list with the single chosen
     * area loses nothing — a dangling id it may still carry is exactly what should
     * not survive the write.
     */
    fun assignGoal(goalId: String, areaId: String?) {
        viewModelScope.launch {
            val areas = listOfNotNull(areaId)
            if (goalRepository.setLifeAreas(goalId, areas) !is Resource.Success) {
                _message.value = "Could not move that goal"
            }
        }
    }

    // ── Google Tasks list sync ────────────────────────────────────────

    /**
     * Reads the user's Google Tasks *lists* — the names in the sidebar — and
     * proposes one life area per list.
     */
    fun syncFromGoogleTasks() {
        if (_sync.value.isLoading) return
        viewModelScope.launch {
            // A known-missing scope is about to bounce off Google's consent
            // screen, so the review sheet would flash open and shut on the way
            // there. The card's own spinner covers that case; every terminal
            // branch below re-opens the sheet when there is something to review.
            _sync.value = LifeAreaSyncState(
                isVisible = _tasksConsent.value != TasksConsent.MISSING,
                isLoading = true,
            )
            when (val result = googleTasksClient.fetchTaskLists()) {
                is TaskListsResult.NeedsConsent -> {
                    // Authoritative, unlike the cached-account probe.
                    _tasksConsent.value = TasksConsent.MISSING
                    _sync.value = LifeAreaSyncState()
                    _consentIntent.value = result.intent
                }

                is TaskListsResult.Failure ->
                    _sync.value = LifeAreaSyncState(isVisible = true, error = result.message)

                is TaskListsResult.Success -> {
                    _tasksConsent.value = TasksConsent.GRANTED
                    val proposals = buildProposals(result.lists, lastAreas)
                    _sync.value = LifeAreaSyncState(
                        isVisible = true,
                        proposals = proposals,
                        totalLists = result.lists.size,
                        error = when {
                            result.lists.isEmpty() -> "No task lists found in your Google account"
                            proposals.isEmpty() ->
                                "Every one of your ${result.lists.size} Google Tasks lists is " +
                                    "already a life area here"
                            else -> null
                        },
                    )
                }
            }
        }
    }

    fun toggleProposal(googleListId: String) {
        _sync.update { state ->
            state.copy(
                proposals = state.proposals.map {
                    if (it.googleListId == googleListId) it.copy(selected = !it.selected) else it
                },
            )
        }
    }

    /** Creates the CREATE rows and links the LINK ones. */
    fun confirmSync() {
        val state = _sync.value
        if (state.isSaving) return
        val chosen = state.proposals.filter { it.selected }
        if (chosen.isEmpty()) {
            _sync.value = LifeAreaSyncState()
            return
        }
        viewModelScope.launch {
            _sync.update { it.copy(isSaving = true) }
            var created = 0
            var linked = 0
            for (proposal in chosen) {
                when (proposal.action) {
                    LifeAreaSyncAction.LINK -> {
                        val id = proposal.existingAreaId ?: continue
                        if (lifeAreaRepository.linkGoogleList(id, proposal.googleListId)
                            is Resource.Success
                        ) {
                            linked++
                        }
                    }

                    LifeAreaSyncAction.CREATE -> {
                        val result = lifeAreaRepository.upsertLifeArea(
                            LifeArea(
                                name = proposal.name,
                                colorHex = proposal.colorHex,
                                iconKey = proposal.iconKey,
                                sortOrder = proposal.sortOrder,
                                googleListId = proposal.googleListId,
                            ),
                        )
                        if (result is Resource.Success) created++
                    }
                }
            }
            _sync.value = LifeAreaSyncState()
            _message.value = when {
                created == 0 && linked == 0 -> "Could not sync those lists"
                linked == 0 -> "Added $created life area${if (created == 1) "" else "s"}"
                created == 0 -> "Linked $linked existing area${if (linked == 1) "" else "s"}"
                else -> "Added $created and linked $linked life areas"
            }
        }
    }

    fun dismissSync() { _sync.value = LifeAreaSyncState() }

    /**
     * The user backed out of Google's consent screen — a decision, not a dropped
     * intent, so the card says so rather than reverting to the generic prompt (#36).
     */
    fun onConsentDeclined() {
        _consentIntent.value = null
        _tasksConsent.value = TasksConsent.MISSING
    }

    /** Called after the user completes Google's consent screen. */
    fun onConsentGranted() {
        _consentIntent.value = null
        syncFromGoogleTasks()
    }

    fun consumeMessage() { _message.value = null }
}

data class LifeAreasUiState(
    val isLoading: Boolean = true,
    val rows: List<LifeAreaRow> = emptyList(),
    val unfiledGoals: List<Goal> = emptyList(),
    val error: String? = null,
)

data class LifeAreaRow(val area: LifeArea, val goalCount: Int)

/**
 * The add/edit sheet. [iconTouched] exists so the name-driven icon guess stops the
 * moment the user picks one themselves.
 */
data class LifeAreaEditorState(
    val isVisible: Boolean = false,
    val isEdit: Boolean = false,
    val id: String = "",
    val name: String = "",
    val colorHex: String = LifeAreaPalette.DEFAULT_HEX,
    val iconKey: String = "flag",
    val iconTouched: Boolean = false,
    val sortOrder: Int = 0,
    val googleListId: String? = null,
    val createdAt: Long = 0L,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Review sheet for a Google Tasks list sync, before anything is written. */
data class LifeAreaSyncState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val proposals: List<LifeAreaProposal> = emptyList(),
    /** How many lists Google returned, before the already-synced ones were dropped. */
    val totalLists: Int = 0,
    val error: String? = null,
)
