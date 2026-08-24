package com.idomarhaim.goalpilot.feature.challenges

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeInvite
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeStanding
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.InviteCandidate
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.canBeScoredFrom
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.feature.goals.label
import com.idomarhaim.goalpilot.feature.goals.wordHint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val repository: ChallengeRepository,
    private val goalRepository: GoalRepository,
    /**
     * Only ever read, and only for **who your friends are** — the invite sheet has to
     * offer people by name and photo, and `publicProfiles` is where those live.
     *
     * `observeLeaderboard(friendsOnly = true)` is reused rather than a new
     * `observeFriendProfiles()` being added, because it already does exactly the join
     * this needs: friend uids from the private `users/{me}/friends` edges, resolved
     * against `publicProfiles` **by document id** rather than filtered out of a global
     * top-N. Its points and ranks are simply not read here.
     */
    private val socialRepository: SocialRepository,
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
        repository.observeIncomingInvites(),
    ) { mine, discoverable, goals, invites ->
        val now = clock()
        ChallengesUiState(
            isLoading = false,
            mine = mine.map { ChallengeCard(it, it.challenge.phaseAt(now)) },
            discoverable = discoverable.map { DiscoverableChallenge(it, it.phaseAt(now)) },
            goals = goals,
            invites = invites,
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
            measure = card.challenge.measure,
            metricWord = card.challenge.metricWord,
            kindLabel = card.challenge.measure?.kind?.label().orEmpty(),
            linkedGoalId = card.data.myLinkedGoalId,
            eligible = eligibleGoalsFor(card.challenge),
            // §2: the empty branch is now a FORM, not a message, so it needs a title to
            // start from. The challenge's own is the right seed -- somebody joining
            // "August Steps Race" wants a steps goal, and naming it after the race is what
            // they would have typed. Editable, because it is their goal and it will
            // outlive the challenge.
            createTitle = card.challenge.title,
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

    fun onCreateTitleChange(value: String) =
        _goalLink.update { it.copy(createTitle = value, error = null) }

    fun onCreateTargetChange(value: String) =
        _goalLink.update { it.copy(createTarget = value, error = null) }

    /**
     * §6's other half, and the one that did not ship with the first: *"joining links **or
     * creates** a goal, so a challenge hands you tracking you did not have."*
     *
     * Until now a user with no goal of the challenge's kind got a message naming the kind
     * and a trip to the Goals screen. Honest, and one screen short — and §1 makes it
     * urgent, because the whole point of inviting a friend is that they can actually
     * compete, and a friend asked into a Steps Race may well have no steps goal.
     *
     * **Creating and linking are one act, not two.** Two would leave a state nobody wants
     * — a goal made *for* a challenge that is not scoring it — reachable by closing the
     * sheet in between. So the link follows the create in the same call, and a failure to
     * link says so rather than reporting success because half of it worked.
     *
     * ⚠️ **It goes through `GoalRepository.upsertGoal`, not a second creation path.** The
     * repository is where a goal's id, ownership and defaults are decided, and a challenge
     * screen inventing its own would be a second writer of the same object — §0.3's
     * most-repeated finding. The only fields this screen supplies are the ones the
     * challenge actually determines: the **measure** (copied whole, so the kind matches by
     * construction and `canBeScoredFrom` cannot fail on a goal made for the purpose) and
     * the title and target the user just typed.
     */
    fun createAndLinkGoal() {
        val current = _goalLink.value
        if (current.isSaving) return
        val title = current.createTitle.trim()
        if (title.isBlank()) {
            _goalLink.update { it.copy(error = "Give the goal a name") }
            return
        }
        val measure = current.measure
        if (measure?.kind == null) {
            // Unreachable from the UI -- `canLinkGoal` is false without a measure -- but
            // stated rather than assumed, because a goal created with no kind could never
            // score the challenge it was made for and nothing downstream would say why.
            _goalLink.update { it.copy(error = "This challenge has no measure to copy") }
            return
        }
        // Accept a comma decimal, the same as the score field: a keypad on a Hebrew or
        // European locale offers one, and "12,5" parsing to null reads as the app
        // rejecting a real number.
        val target = current.createTarget.trim().replace(',', '.').toDoubleOrNull()
        if (target == null || target <= 0.0) {
            _goalLink.update { it.copy(error = "Set a target above zero") }
            return
        }
        viewModelScope.launch {
            _goalLink.update { it.copy(isSaving = true, error = null) }
            val created = goalRepository.upsertGoal(
                Goal(
                    title = title,
                    // Copied whole from the challenge. The KIND is what the scoring match
                    // is made on, and the WORD is the user's own language for it (§1.3) --
                    // taking the challenge's word means a Hebrew user joining an English
                    // race gets the race's word, which is the honest default for a goal
                    // created to run in it, and is one edit away on the Goals screen.
                    measure = measure,
                    targetValue = target,
                    // The user typed the title and the target and pressed Create, so this
                    // is a goal they declared. `AddEditGoalScreen` stamps the same value
                    // for the same reason; nothing else may claim it (§1.1, `#6`).
                    declaredBy = DeclaredBy.USER,
                ),
            )
            when (created) {
                is Resource.Success -> when (
                    val linked = repository.linkGoal(current.challengeId, created.data)
                ) {
                    is Resource.Success -> {
                        _goalLink.value = GoalLinkState()
                        // Names what the user now has, in the order they care about: a
                        // goal that exists, and a challenge that scores itself from it.
                        _message.value = "Goal created — this challenge now scores itself"
                    }

                    // THE HALF-DONE STATE, SAID OUT LOUD. The goal exists; the link does
                    // not. Reporting success here would leave somebody looking at a
                    // challenge that is not scoring and a Goals screen that gained a row
                    // they did not ask for, with nothing connecting the two.
                    is Resource.Error -> _goalLink.update {
                        it.copy(
                            isSaving = false,
                            error = "Goal created, but linking it failed: ${linked.message}",
                            // Re-derived so the new goal is now IN the picker: the user can
                            // finish the job by tapping it, instead of making a second one.
                            eligible = eligibleGoalsFor(
                                uiState.value.mine
                                    .firstOrNull { c -> c.challenge.id == current.challengeId }
                                    ?.challenge ?: Challenge(),
                            ),
                        )
                    }

                    Resource.Loading -> Unit
                }

                is Resource.Error ->
                    _goalLink.update { it.copy(isSaving = false, error = created.message) }

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

    // ── Invites — Ido's own report, and the first thing this session built ──
    //
    // > *"in the version I have on my phone I can create a CHALLENGE but I cannot invite
    // > a friend I have in the app to the CHALLENGE"* — 2026-08-24.
    //
    // Two halves, deliberately asymmetric. **Sending** is a sheet you go and open, so it
    // is state driven by [openInvite]. **Receiving** is a section that is simply there at
    // the top of the screen, so it rides on [uiState] like any other data — there is no
    // badge, no count and no notification, because an invite is an offer and an offer
    // that follows you around is a demand.

    /** Which challenge the invite sheet is offering, or null when it is closed. */
    private val _inviteTargetId = MutableStateFlow<String?>(null)

    /** The one row that is mid-write, so only its button spins. */
    private val _inviteBusyUid = MutableStateFlow<String?>(null)

    private val _inviteError = MutableStateFlow<String?>(null)

    /**
     * The invite sheet: this user's friends, each already knowing why they cannot be
     * asked, if they cannot.
     *
     * **`flatMapLatest` on the target id, not `combine` into [uiState]**, so the friends
     * read only runs while the sheet is actually open. It is a `publicProfiles` listener
     * over every friend, and keeping it alive for the whole time the Challenges screen is
     * on screen would buy nothing — nothing outside this sheet renders a friend.
     */
    val invite: StateFlow<InviteState> = _inviteTargetId.flatMapLatest { targetId ->
        if (targetId == null) {
            flowOf(InviteState())
        } else {
            combine(
                socialRepository.observeLeaderboard(friendsOnly = true),
                repository.observeSentInvites(),
                uiState,
                _inviteBusyUid,
                _inviteError,
            ) { friends, sent, state, busyUid, error ->
                val card = state.mine.firstOrNull { it.challenge.id == targetId }
                val participants = card?.standings?.map { it.uid }.orEmpty().toSet()
                val invited = sent
                    .filter { it.challengeId == targetId }
                    .map { it.toUid }
                    .toSet()
                InviteState(
                    isVisible = true,
                    challengeId = targetId,
                    challengeTitle = card?.challenge?.title.orEmpty(),
                    // `observeLeaderboard(friendsOnly = true)` returns the friends AND the
                    // signed-in user — it is a leaderboard, and you are on it. Dropping
                    // yourself here rather than asking Social for a friends-only variant
                    // keeps one read path; `isCurrentUser` is exactly the flag for it.
                    candidates = friends.entries
                        .filterNot { it.isCurrentUser }
                        .sortedBy { it.displayName.lowercase() }
                        .map { entry ->
                            InviteCandidate(
                                uid = entry.uid,
                                displayName = entry.displayName,
                                photoUrl = entry.photoUrl,
                                isParticipant = entry.uid in participants,
                                isInvited = entry.uid in invited,
                            )
                        },
                    busyUid = busyUid,
                    error = error,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InviteState())

    fun openInvite(card: ChallengeCard) {
        _inviteError.value = null
        _inviteBusyUid.value = null
        _inviteTargetId.value = card.challenge.id
    }

    fun dismissInvite() {
        _inviteTargetId.value = null
        _inviteBusyUid.value = null
        _inviteError.value = null
    }

    /**
     * Sends one invite.
     *
     * The sheet **stays open** afterwards, because inviting three people is one act with
     * three taps and closing after each would make it three trips. The row the user just
     * tapped turns into *"Invited"* on the next snapshot — the `observeSentInvites`
     * listener feeding [invite] is what does that, so the confirmation is the real state
     * of the database rather than a local flag that could disagree with it.
     */
    fun sendInvite(toUid: String) {
        val challengeId = _inviteTargetId.value ?: return
        if (_inviteBusyUid.value != null) return
        viewModelScope.launch {
            _inviteBusyUid.value = toUid
            _inviteError.value = null
            when (val result = repository.inviteToChallenge(challengeId, toUid)) {
                is Resource.Success -> _message.value = "Invite sent"
                is Resource.Error -> _inviteError.value = result.message
                Resource.Loading -> Unit
            }
            _inviteBusyUid.value = null
        }
    }

    /** Which invite row is mid-write, so only that row's buttons go quiet. */
    private val _invitePendingId = MutableStateFlow<String?>(null)
    val invitePendingId = _invitePendingId.asStateFlow()

    /**
     * Joins from an invite. Still the invitee's own write — see the repository.
     *
     * The row does not need dismissing: accepting deletes the invite in the same batch,
     * so the section it was in loses it on the next snapshot.
     */
    fun acceptInvite(inviteId: String) {
        if (_invitePendingId.value != null) return
        viewModelScope.launch {
            _invitePendingId.value = inviteId
            _message.value = when (val result = repository.acceptInvite(inviteId)) {
                is Resource.Success -> "You're in"
                is Resource.Error -> result.message
                Resource.Loading -> null
            }
            _invitePendingId.value = null
        }
    }

    /**
     * Declines, and says nothing to anybody.
     *
     * **No confirmation dialog**, unlike leaving or deleting. Those destroy something the
     * user built; this declines an offer, and the sender can simply make it again. A
     * dialog here would turn a shrug into a decision.
     */
    fun declineInvite(inviteId: String) {
        if (_invitePendingId.value != null) return
        viewModelScope.launch {
            _invitePendingId.value = inviteId
            when (val result = repository.dismissInvite(inviteId)) {
                // Deliberately silent on success: a snackbar for "I said no thank you"
                // is the app being pleased with itself about a non-event.
                is Resource.Success -> Unit
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _invitePendingId.value = null
        }
    }

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
    /**
     * Invites waiting for this user, newest first — rendered as a section at the **top**
     * of the screen and nowhere else.
     *
     * Not a badge, not a count, not a notification. An invite is an offer; a number on a
     * tab bar turns it into a chore, and `C9a` §6's consent story covers reminders about
     * the user's *own* work, not inbound requests from other people.
     */
    val invites: List<ChallengeInvite> = emptyList(),
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
    /**
     * The challenge's measure, carried whole so §2 can **copy** it onto a goal it creates.
     *
     * [metricWord] and [kindLabel] beside it are for display and are derived from this;
     * they are kept because the sheet renders both in sentences and re-deriving a label in
     * a composable would put a `when` over a domain enum inside the UI.
     */
    val measure: Measure? = null,
    val metricWord: String = "",
    val kindLabel: String = "",
    /** The goal already linked, so the picker can show which one is current. */
    val linkedGoalId: String = "",
    val eligible: List<Goal> = emptyList(),
    /**
     * §2's create form, shown where the *"you have no goal of this kind"* message used to
     * be. Seeded from the challenge's title on open; the target starts blank because there
     * is nothing to guess from — a challenge names a unit, never a finish line.
     */
    val createTitle: String = "",
    val createTarget: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * The invite sheet for one challenge — §1's sending half.
 *
 * [candidates] carries every friend, including the ones who cannot be asked, each
 * knowing why. See [InviteCandidate] for why a blocked friend is greyed rather than
 * filtered: a friend who silently vanishes reads as *the app does not know them*, which
 * is the one thing the user is certain is false.
 */
data class InviteState(
    val isVisible: Boolean = false,
    val challengeId: String = "",
    val challengeTitle: String = "",
    val candidates: List<InviteCandidate> = emptyList(),
    /** The one row mid-write, so the rest of the sheet stays usable. */
    val busyUid: String? = null,
    val error: String? = null,
) {
    /**
     * Whether there is nobody to offer.
     *
     * A real state with its own sentence, not an empty list: the fix is on a different
     * screen (add a friend), and a blank sheet does not say so.
     */
    val hasNoFriends: Boolean get() = candidates.isEmpty()
}
