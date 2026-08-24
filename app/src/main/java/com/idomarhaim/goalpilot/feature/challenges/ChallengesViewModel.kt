package com.idomarhaim.goalpilot.feature.challenges

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeStanding
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.canBeScoredFrom
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.feature.goals.label
import com.idomarhaim.goalpilot.feature.goals.wordHint
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
 * Competitive challenges — `docs/PRODUCT_v0.3.md` §6 (`C14` #23).
 *
 * Every write here goes through [ChallengeRepository], which aims each one at
 * whichever document `firestore.rules` actually permits — joining is a write to
 * your own participant row, never an edit to the challenge. See
 * `CHANGELOG/2026-08-04/challenges.md`.
 *
 * ### It reads goals, and that is §6's whole shape rather than a convenience
 *
 * > **A challenge scores from nothing of its own: it scores from each
 * > participant's goal.**
 *
 * So a challenge screen has to be able to show the user their own goals, because
 * picking one is the act that makes the number move. What it must **not** do is
 * grow a second pipe from Health Connect to a challenge: everything already flows
 * into a goal, and §6's fix *deletes one representation of the walk rather than
 * building a second one*.
 */
@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val repository: ChallengeRepository,
    private val goalRepository: GoalRepository,
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
        goalRepository.observeGoals(),
    ) { mine, discoverable, goals ->
        val now = clock()
        ChallengesUiState(
            isLoading = false,
            mine = mine.map { ChallengeCard(it, it.challenge.phaseAt(now)) },
            discoverable = discoverable.map { DiscoverableChallenge(it, it.phaseAt(now)) },
            goals = goals,
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

    private val _goalLink = MutableStateFlow(GoalLinkState())
    val goalLink = _goalLink.asStateFlow()

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

    // ── Linking a goal — §6's actual scoring path ─────────────────────

    /**
     * Opens the goal picker for [card], offering only the goals that can score it.
     *
     * **An empty list is a real state and gets its own message**, not a picker
     * with nothing in it. §6 says *joining links **or creates** a goal*, so a user
     * with no goal of the right kind has to be told what to make rather than left
     * looking at a blank sheet. Creating it from here is the half this session did
     * not build — [GoalLinkState.eligible] being empty is where that lands.
     */
    fun openGoalLink(card: ChallengeCard) {
        _goalLink.value = GoalLinkState(
            isVisible = true,
            challengeId = card.challenge.id,
            challengeTitle = card.challenge.title,
            metricWord = card.challenge.metricWord,
            kindLabel = card.challenge.measure?.kind?.label().orEmpty(),
            linkedGoalId = card.data.myLinkedGoalId,
            eligible = eligibleGoalsFor(card.challenge),
        )
    }

    /**
     * The goals that may score [challenge] — matched on the measure **kind**, never
     * on the word. See `Challenge.canBeScoredFrom` for why: the word is user
     * content in the user's own language, and matching on it would keep a Hebrew
     * user out of an English steps race for a reason that has nothing to do with
     * what is being counted.
     */
    @VisibleForTesting
    internal fun eligibleGoalsFor(challenge: Challenge): List<Goal> =
        uiState.value.goals.filter { challenge.canBeScoredFrom(it) }

    fun linkGoal(goalId: String) {
        val current = _goalLink.value
        if (current.isSaving) return
        viewModelScope.launch {
            _goalLink.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.linkGoal(current.challengeId, goalId)) {
                is Resource.Success -> {
                    _goalLink.value = GoalLinkState()
                    // Says what changed, not that a write succeeded: the point of
                    // linking is that nobody has to type a number again.
                    _message.value = "Linked — this challenge now scores itself"
                }

                is Resource.Error ->
                    _goalLink.update { it.copy(isSaving = false, error = result.message) }

                Resource.Loading -> Unit
            }
        }
    }

    fun dismissGoalLink() { _goalLink.value = GoalLinkState() }

    // ── Reporting a score ─────────────────────────────────────────────

    fun openScoreEntry(card: ChallengeCard) {
        _scoreEntry.value = ScoreEntryState(
            isVisible = true,
            challengeId = card.challenge.id,
            challengeTitle = card.challenge.title,
            metricWord = card.challenge.metricWord,
            // Whether typing this number will switch the challenge OFF its linked
            // goal. Said before the write, not after it — a linked challenge is
            // scoring itself, and silently replacing that with a typed number is
            // the one outcome nobody would have chosen deliberately.
            replacesLink = card.data.myScoreIsLinked,
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
     * Picking a kind re-suggests the word — but only while the user has not typed
     * one themselves, the same rule the life-area editor uses for its icon.
     *
     * The suggestion is `MeasureKind.wordHint()`, so the challenge editor and the
     * goal editor offer the same example for the same kind rather than keeping two
     * tables that drift.
     */
    fun onMeasureKindChange(kind: MeasureKind) {
        _editor.update {
            it.copy(
                measureKind = kind,
                measureWord = if (it.wordTouched) it.measureWord else kind.wordHint(),
                error = null,
            )
        }
    }

    fun onMeasureWordChange(value: String) =
        _editor.update { it.copy(measureWord = value, wordTouched = true, error = null) }

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
        // §6: a challenge has no optional measure. Checked here as well as in the
        // repository, because the dialog is where the kind was (not) picked.
        val kind = current.measureKind
        if (kind == null || current.measureWord.isBlank()) {
            _editor.update { it.copy(error = "Say what this challenge counts") }
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
                measure = Measure(kind = kind, word = current.measureWord.trim()),
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
        /** Trims the trailing `.0` so whole numbers do not read as measurements. */
        fun format(score: Double): String =
            if (score % 1.0 == 0.0) score.toLong().toString() else score.toString()
    }
}

data class ChallengesUiState(
    val isLoading: Boolean = true,
    val mine: List<ChallengeCard> = emptyList(),
    val discoverable: List<DiscoverableChallenge> = emptyList(),
    /**
     * The user's own live goals — the pool a challenge is linked to. Not rendered
     * as a list anywhere on this screen; it is what the goal picker filters.
     */
    val goals: List<Goal> = emptyList(),
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

    /** What the *participants* read knows about itself — the as-of caption (#50). */
    val standingsFreshness: Freshness get() = data.standingsFreshness

    /** Whether the current user's score moves on its own (§6). */
    val isLinked: Boolean get() = data.myScoreIsLinked

    /** Scoring an upcoming challenge, or one that is over, is not a thing. */
    val canReportScore: Boolean get() = phase == ChallengePhase.ACTIVE

    /**
     * Whether the *link a goal* action is offered.
     *
     * Gated on the challenge having a measure at all: a pre-§6 challenge whose
     * `metricUnit` was the `"points"` default has no kind to match a goal
     * against, and offering an empty picker would blame the user for a document
     * the migration deliberately would not guess at.
     */
    val canLinkGoal: Boolean get() = canReportScore && !challenge.isUnmeasured
}

/** A challenge the user is not in yet. */
data class DiscoverableChallenge(
    val challenge: Challenge,
    val phase: ChallengePhase,
) {
    /** You can join one that hasn't started; there is nothing to join once it's over. */
    val canJoin: Boolean get() = phase != ChallengePhase.ENDED
}

/**
 * The create sheet. [wordTouched] stops the kind-driven word guess once the user
 * types, the same rule the goal editor uses.
 */
data class ChallengeEditorState(
    val isVisible: Boolean = false,
    val title: String = "",
    val description: String = "",
    /** Null until picked — there is no sensible default kind, and §6 forbids `"points"`. */
    val measureKind: MeasureKind? = null,
    val measureWord: String = "",
    val wordTouched: Boolean = false,
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
    val metricWord: String = "",
    /** Whether saving this will switch the challenge off its linked goal. */
    val replacesLink: Boolean = false,
    val value: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** The "score this from a goal" picker for one challenge — §6's scoring path. */
data class GoalLinkState(
    val isVisible: Boolean = false,
    val challengeId: String = "",
    val challengeTitle: String = "",
    val metricWord: String = "",
    val kindLabel: String = "",
    /** The goal already linked, so the picker can show which one is current. */
    val linkedGoalId: String = "",
    val eligible: List<Goal> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
)
