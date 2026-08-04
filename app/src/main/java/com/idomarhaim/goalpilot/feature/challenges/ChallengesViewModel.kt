package com.idomarhaim.goalpilot.feature.challenges

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeStanding
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
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
 * Competitive challenges (spec §6 nice-to-have, §7): the ones you are in with
 * live standings, the ones you could join, and the flows that move you between
 * those two lists.
 *
 * Every write here goes through [ChallengeRepository], which aims each one at
 * whichever document `firestore.rules` actually permits — joining is a write to
 * your own participant row, never an edit to the challenge. See
 * `CHANGELOG/2026-08-04/challenges.md`.
 */
@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val repository: ChallengeRepository,
) : ViewModel() {

    /**
     * Read at emission time, not at construction, so every re-emission re-derives
     * the phase. Overridable because a phase boundary is otherwise only assertable
     * by waiting for one.
     */
    @VisibleForTesting
    internal var clock: () -> Long = System::currentTimeMillis

    /**
     * Phases are stamped from [clock] as each snapshot arrives. That is enough
     * without a ticker: `WhileSubscribed` restarts the upstream flows whenever the
     * screen is re-entered after a pause, so returning to it re-derives every
     * phase — and while it is open, a challenge starting or ending mid-view is far
     * rarer than the recomposition-per-minute a ticker would cost.
     */
    val uiState: StateFlow<ChallengesUiState> = combine(
        repository.observeMyChallenges(),
        repository.observeDiscoverable(),
    ) { mine, discoverable ->
        val now = clock()
        ChallengesUiState(
            isLoading = false,
            mine = mine.map { ChallengeCard(it, it.challenge.phaseAt(now)) },
            discoverable = discoverable.map { DiscoverableChallenge(it, it.phaseAt(now)) },
        )
    }.catch { emit(ChallengesUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChallengesUiState(isLoading = true),
        )

    private val _editor = MutableStateFlow(ChallengeEditorState())
    val editor = _editor.asStateFlow()

    private val _scoreEntry = MutableStateFlow(ScoreEntryState())
    val scoreEntry = _scoreEntry.asStateFlow()

    /** Which challenge the standings sheet is showing, if any. */
    private val _detailId = MutableStateFlow<String?>(null)
    val detailId = _detailId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    // ── Joining and leaving ───────────────────────────────────────────

    fun join(challengeId: String) {
        viewModelScope.launch {
            _message.value = when (val result = repository.joinChallenge(challengeId)) {
                is Resource.Success -> "You're in"
                // The repository's own text ("That challenge no longer exists") is
                // far more use than a generic failure.
                is Resource.Error -> result.message
                Resource.Loading -> null
            }
        }
    }

    fun leave(challengeId: String) {
        viewModelScope.launch {
            // Leaving closes the sheet: the challenge drops out of `mine` on the
            // next snapshot and the sheet would have nothing left to render.
            if (_detailId.value == challengeId) _detailId.value = null
            _message.value = when (val result = repository.leaveChallenge(challengeId)) {
                is Resource.Success -> "You left the challenge"
                is Resource.Error -> result.message
                Resource.Loading -> null
            }
        }
    }

    /** Owner-only. Leaves other members' participant rows orphaned — see the repository. */
    fun deleteChallenge(challengeId: String) {
        viewModelScope.launch {
            if (_detailId.value == challengeId) _detailId.value = null
            _message.value = when (val result = repository.deleteChallenge(challengeId)) {
                is Resource.Success -> "Challenge deleted"
                is Resource.Error -> result.message
                Resource.Loading -> null
            }
        }
    }

    // ── Standings sheet ───────────────────────────────────────────────

    fun openDetail(challengeId: String) { _detailId.value = challengeId }

    fun dismissDetail() { _detailId.value = null }

    // ── Reporting a score ─────────────────────────────────────────────

    fun openScoreEntry(card: ChallengeCard) {
        _scoreEntry.value = ScoreEntryState(
            isVisible = true,
            challengeId = card.challenge.id,
            challengeTitle = card.challenge.title,
            metricUnit = card.challenge.metricUnit,
            // Pre-filled with what they already reported, so "add a bit more" does
            // not mean retyping it. Blank at zero — a pre-filled 0 reads as noise.
            value = card.myStanding?.score?.takeIf { it > 0.0 }?.let { format(it) }.orEmpty(),
        )
    }

    fun onScoreChange(value: String) = _scoreEntry.update { it.copy(value = value, error = null) }

    fun submitScore() {
        val current = _scoreEntry.value
        if (current.isSaving) return
        // Accept a comma decimal: a keypad on a Hebrew or European locale offers
        // one, and "12,5" parsing to null reads as the app rejecting a real number.
        val score = current.value.trim().replace(',', '.').toDoubleOrNull()
        if (score == null) {
            _scoreEntry.update { it.copy(error = "Enter a number") }
            return
        }
        if (score < 0.0) {
            _scoreEntry.update { it.copy(error = "A score cannot be negative") }
            return
        }
        viewModelScope.launch {
            _scoreEntry.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.reportScore(current.challengeId, score)) {
                is Resource.Success -> {
                    _scoreEntry.value = ScoreEntryState()
                    _message.value = "Score updated"
                }

                is Resource.Error ->
                    _scoreEntry.update { it.copy(isSaving = false, error = result.message) }

                Resource.Loading -> Unit
            }
        }
    }

    fun dismissScoreEntry() { _scoreEntry.value = ScoreEntryState() }

    // ── Creating ──────────────────────────────────────────────────────

    fun openEditor() { _editor.value = ChallengeEditorState(isVisible = true) }

    fun onTitleChange(value: String) = _editor.update { it.copy(title = value, error = null) }

    fun onDescriptionChange(value: String) = _editor.update { it.copy(description = value) }

    /**
     * Picking a type re-suggests the unit — but only while the user has not typed
     * one themselves, the same rule the life-area editor uses for its icon.
     */
    fun onTypeChange(type: ChallengeType) {
        _editor.update {
            it.copy(
                type = type,
                metricUnit = if (it.unitTouched) it.metricUnit else defaultUnitFor(type),
            )
        }
    }

    fun onMetricUnitChange(value: String) =
        _editor.update { it.copy(metricUnit = value, unitTouched = true) }

    /** Both dates are optional; pass null to clear one. Zero means "not set". */
    fun onStartChange(epochMillis: Long?) =
        _editor.update { it.copy(startAtEpochMillis = epochMillis ?: 0L, error = null) }

    fun onEndChange(epochMillis: Long?) =
        _editor.update { it.copy(endAtEpochMillis = epochMillis ?: 0L, error = null) }

    fun saveEditor() {
        val current = _editor.value
        if (current.isSaving) return
        if (current.title.isBlank()) {
            _editor.update { it.copy(error = "Give the challenge a name") }
            return
        }
        // Checked here as well as in the repository: the dialog is where the two
        // dates were picked, so it is where the complaint belongs.
        if (current.endAtEpochMillis > 0L &&
            current.startAtEpochMillis > current.endAtEpochMillis
        ) {
            _editor.update { it.copy(error = "The challenge would end before it starts") }
            return
        }
        viewModelScope.launch {
            _editor.update { it.copy(isSaving = true, error = null) }
            val result = repository.createChallenge(
                title = current.title,
                description = current.description,
                type = current.type,
                metricUnit = current.metricUnit.ifBlank { defaultUnitFor(current.type) },
                startAtEpochMillis = current.startAtEpochMillis,
                endAtEpochMillis = current.endAtEpochMillis,
            )
            when (result) {
                is Resource.Success -> {
                    _editor.value = ChallengeEditorState()
                    _message.value = "Challenge created — you're in it already"
                }

                is Resource.Error ->
                    _editor.update { it.copy(isSaving = false, error = result.message) }

                Resource.Loading -> Unit
            }
        }
    }

    fun dismissEditor() { _editor.value = ChallengeEditorState() }

    fun consumeMessage() { _message.value = null }

    companion object {
        /** What each challenge type is naturally measured in. */
        fun defaultUnitFor(type: ChallengeType): String = when (type) {
            ChallengeType.RUNNING -> "km"
            ChallengeType.STEPS -> "steps"
            ChallengeType.SLEEP -> "hours"
            ChallengeType.WORKOUTS -> "workouts"
            ChallengeType.CUSTOM -> "points"
        }

        /** Trims the trailing `.0` so whole numbers do not read as measurements. */
        fun format(score: Double): String =
            if (score % 1.0 == 0.0) score.toLong().toString() else score.toString()
    }
}

data class ChallengesUiState(
    val isLoading: Boolean = true,
    val mine: List<ChallengeCard> = emptyList(),
    val discoverable: List<DiscoverableChallenge> = emptyList(),
    val error: String? = null,
)

/**
 * One of the user's own challenges, with its phase stamped at emission time.
 *
 * The standings inside are **already ranked** by `rankedByScore` — joint ranks
 * for ties, current user flagged. Nothing here re-sorts or re-ranks them.
 */
data class ChallengeCard(
    val data: ChallengeWithStandings,
    val phase: ChallengePhase,
) {
    val challenge: Challenge get() = data.challenge
    val standings: List<ChallengeStanding> get() = data.standings
    val myStanding: ChallengeStanding? get() = data.myStanding
    val isOwner: Boolean get() = data.isOwner
    val participantCount: Int get() = data.participantCount

    /** Scoring an upcoming challenge, or one that is over, is not a thing. */
    val canReportScore: Boolean get() = phase == ChallengePhase.ACTIVE
}

/** A challenge the user is not in yet. */
data class DiscoverableChallenge(
    val challenge: Challenge,
    val phase: ChallengePhase,
) {
    /** You can join one that hasn't started; there is nothing to join once it's over. */
    val canJoin: Boolean get() = phase != ChallengePhase.ENDED
}

/** The create sheet. [unitTouched] stops the type-driven unit guess once the user types. */
data class ChallengeEditorState(
    val isVisible: Boolean = false,
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.CUSTOM,
    val metricUnit: String = "points",
    val unitTouched: Boolean = false,
    val startAtEpochMillis: Long = 0L,
    val endAtEpochMillis: Long = 0L,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** The "report your score" dialog for one challenge. */
data class ScoreEntryState(
    val isVisible: Boolean = false,
    val challengeId: String = "",
    val challengeTitle: String = "",
    val metricUnit: String = "",
    val value: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)
