package com.idomarhaim.goalpilot.feature.challenges

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeInvite
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeStanding
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.InviteCandidate
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureChangeConsequence
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.canBeScoredFrom
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import com.idomarhaim.goalpilot.domain.usecase.LinkChallengeToHealthUseCase
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
    /**
     * Read for **one** thing: whether Health Connect can actually serve this device, so the
     * option below can say *why* rather than offering a choice that silently never moves.
     */
    private val healthRepository: HealthRepository,
    private val linkChallengeToHealth: LinkChallengeToHealthUseCase,
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
            healthOptions = healthOptionsFor(card),
            windowNote = windowNoteFor(card.challenge),
        )
        // Availability is a suspending device call, so the sheet opens immediately with the
        // options listed and fills in "why not" a beat later. The alternative -- waiting --
        // would make the commonest case (Health Connect present and permitted) pay for the
        // rarest one.
        viewModelScope.launch {
            val availability = healthRepository.availability()
            _goalLink.update {
                if (it.challengeId == card.challenge.id) {
                    it.copy(healthAvailability = availability)
                } else {
                    it
                }
            }
        }
    }

    /**
     * The Health Connect choices for [card] — Ido, 2026-08-25:
     *
     * > *"if I make a steps competition, there should also be an option to pull the logs
     * > straight into the CHALLENGE and not only through a personal GOAL of mine"*
     *
     * **Matched on the measure KIND alone, and the row says what it is.** A `COUNT`
     * challenge might be counting steps or books and the app cannot tell; matching the
     * measure *word* would be a string match over user content, which §1.3 forbids and
     * which would shut a Hebrew user's `"צעדים"` out of an English steps race. So the offer
     * appears whenever the kind fits, labelled *"Steps · from Health Connect"*, and
     * somebody running a books challenge simply does not take it.
     *
     * That is also §4's conclusion arriving from the other side: the app has no honest way
     * to know a challenge is *a health challenge*, which is why §4 recommended deleting the
     * proposed health **gate** — and why this is an offer with a label rather than a filter
     * pretending to knowledge it does not have.
     */
    /**
     * The one sentence that says what will be counted — derived from the challenge's own
     * dates, because that is what `scoringWindowFor` now derives from.
     */
    @VisibleForTesting
    internal fun windowNoteFor(challenge: Challenge): String = when {
        challenge.startAtEpochMillis > 0L && challenge.endAtEpochMillis > 0L ->
            "Everything you count between " +
                DateTimeUtils.formatDay(challenge.startAtEpochMillis) + " and " +
                DateTimeUtils.formatDay(challenge.endAtEpochMillis - 1) +
                " — the same days for everyone, whenever each of you joined."

        challenge.startAtEpochMillis > 0L ->
            "Everything you count from " +
                DateTimeUtils.formatDay(challenge.startAtEpochMillis) +
                " onwards — the same start for everyone, whenever each of you joined."

        // No dates at all, so `joinedAt` is the only floor there is -- §6's original rule,
        // and the case it was really protecting.
        else -> "Everything you count from the moment you joined — nothing before it."
    }

    @VisibleForTesting
    internal fun healthOptionsFor(card: ChallengeCard): List<HealthLinkOption> {
        val goals = uiState.value.goals
        return HealthMetric.forKind(card.challenge.measure?.kind).map { metric ->
            val its = goals.firstOrNull {
                it.healthSourceKey == metric.goalSourceKey && !it.isArchived
            }
            HealthLinkOption(
                metric = metric,
                // Already the one scoring this challenge, so the row says "Scoring this"
                // rather than offering to do again what is already done.
                isCurrent = its != null && its.id == card.data.myLinkedGoalId,
                // Whether picking it will make a goal. Said out loud in the row, because a
                // new row appearing on the Goals screen unannounced is the one thing about
                // this design a user could reasonably call a surprise.
                createsGoal = its == null,
            )
        }
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

    /**
     * Ido's own ask, granted where he asked for it: **a choice that is not a goal.**
     *
     * What it does underneath is find-or-create the canonical Health-Connect-owned goal and
     * link that — see [LinkChallengeToHealthUseCase] for why *literally* direct is not
     * available at any price: the scoring Function runs in the cloud and cannot read Health
     * Connect, so a second pipe would mean writing the same readings into Firestore twice.
     */
    fun linkHealthConnect(metric: HealthMetric) {
        val current = _goalLink.value
        if (current.isSaving) return
        val challenge = uiState.value.mine
            .firstOrNull { it.challenge.id == current.challengeId }
            ?.challenge
            ?: return
        viewModelScope.launch {
            _goalLink.update { it.copy(isSaving = true, error = null) }
            when (val result = linkChallengeToHealth(challenge, metric, uiState.value.goals)) {
                is Resource.Success -> {
                    _goalLink.value = GoalLinkState()
                    _message.value = "Scoring from Health Connect — nothing to log"
                }

                is Resource.Error ->
                    _goalLink.update { it.copy(isSaving = false, error = result.message) }

                Resource.Loading -> Unit
            }
        }
    }

    fun dismissGoalLink() { _goalLink.value = GoalLinkState() }

    // ── §3 · changing the measure, with everybody's consent ───────────
    //
    // §6: "the owner writes `pendingMeasure` on the challenge document, each participant
    // writes `approvedChangeId` in the one document they are permitted to write, and the
    // Function applies it when every row agrees."
    //
    // The measure is the unit every participant's score is expressed in, so an owner who
    // could change it alone would silently re-denominate other people's numbers. As of
    // 2026-08-25 `firestore.rules` pins the live measure against every client write, so
    // this is the only path there is -- there is no "edit the challenge" that bypasses it.

    private val _measureChange = MutableStateFlow(MeasureChangeState())
    val measureChange = _measureChange.asStateFlow()

    fun openMeasureChange(card: ChallengeCard) {
        _measureChange.value = MeasureChangeState(
            isVisible = true,
            challengeId = card.challenge.id,
            challengeTitle = card.challenge.title,
            currentKind = card.challenge.measure?.kind,
            currentWord = card.challenge.metricWord,
            // Seeded with what it counts NOW, so the owner edits rather than retypes -- and
            // so the dialog can say, live, which of the two consequences their edit has.
            kind = card.challenge.measure?.kind,
            word = card.challenge.metricWord,
            participantCount = card.participantCount,
        )
    }

    fun onChangeKind(kind: MeasureKind) =
        _measureChange.update { it.copy(kind = kind, error = null) }

    fun onChangeWord(value: String) =
        _measureChange.update { it.copy(word = value, error = null) }

    fun dismissMeasureChange() { _measureChange.value = MeasureChangeState() }

    fun proposeMeasureChange() {
        val current = _measureChange.value
        if (current.isSaving) return
        val kind = current.kind
        if (kind == null || current.word.isBlank()) {
            _measureChange.update { it.copy(error = "Say what it should count") }
            return
        }
        if (!current.isAChange) {
            _measureChange.update { it.copy(error = "That is what it counts already") }
            return
        }
        viewModelScope.launch {
            _measureChange.update { it.copy(isSaving = true, error = null) }
            val result = repository.proposeMeasureChange(
                current.challengeId,
                Measure(kind = kind, word = current.word.trim()),
            )
            when (result) {
                is Resource.Success -> {
                    _measureChange.value = MeasureChangeState()
                    // Says what happens NEXT, not that a write succeeded. The owner has not
                    // changed anything yet -- they have asked -- and on a solo challenge
                    // the function has already applied it by the time this is read, which
                    // is why the wording works for both.
                    _message.value = if (current.participantCount > 1) {
                        "Proposed — it changes when everyone agrees"
                    } else {
                        "Measure updated"
                    }
                }

                is Resource.Error ->
                    _measureChange.update { it.copy(isSaving = false, error = result.message) }

                Resource.Loading -> Unit
            }
        }
    }

    /** Which challenge's pending change is mid-vote, so only that banner goes quiet. */
    private val _approvingChallengeId = MutableStateFlow<String?>(null)
    val approvingChallengeId = _approvingChallengeId.asStateFlow()

    /**
     * Records this user's agreement.
     *
     * The `changeId` comes from the card the user is looking at, not from a re-read, so a
     * stale screen cannot approve a proposal they never saw — the function compares it
     * against the challenge's own and an approval of a withdrawn proposal counts for
     * nothing rather than carrying over to its replacement.
     */
    fun approveMeasureChange(card: ChallengeCard) {
        if (_approvingChallengeId.value != null) return
        val challengeId = card.challenge.id
        val changeId = card.challenge.pendingChangeId
        viewModelScope.launch {
            _approvingChallengeId.value = challengeId
            when (val result = repository.approveMeasureChange(challengeId, changeId)) {
                // Deliberately silent. The banner it was on either disappears (the change
                // landed) or re-renders saying who is still to agree -- both of which say
                // more than a snackbar would, and neither needs one.
                is Resource.Success -> Unit
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _approvingChallengeId.value = null
        }
    }

    fun withdrawMeasureChange(challengeId: String) {
        viewModelScope.launch {
            _message.value = when (val r = repository.withdrawMeasureChange(challengeId)) {
                is Resource.Success -> "Change withdrawn"
                is Resource.Error -> r.message
                Resource.Loading -> null
            }
        }
    }

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

    /** §3: a measure change is waiting on the participants. */
    val hasPendingMeasureChange: Boolean get() = challenge.hasPendingMeasureChange

    /** How many have agreed, out of everyone who is still here. */
    val pendingApprovals: Int get() = data.pendingApprovals

    /** Whether this user has already voted, so the banner offers a button or a wait. */
    val iApprovedPending: Boolean get() = data.iApprovedPending

    /**
     * What the pending change would do — **derived from the change**, not chosen.
     *
     * See `Challenge.pendingConsequence`: a kind change cannot be adapted without a unit
     * conversion this app does not perform, and a word-only change needs no adapting.
     */
    val pendingConsequence: MeasureChangeConsequence get() = challenge.pendingConsequence

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
    /**
     * Health Connect, offered as a **first-class choice beside the goals** — Ido's
     * instruction of 2026-08-25, which overrides §6's *"a challenge scores from each
     * participant's goal"* at the product level. Empty when the challenge counts something
     * Health Connect does not track.
     */
    val healthOptions: List<HealthLinkOption> = emptyList(),
    /** Null until the device has answered; see `openGoalLink` for why the sheet opens first. */
    val healthAvailability: HealthAvailability? = null,
    /**
     * What this challenge actually counts, in one sentence — **computed, not fixed prose.**
     *
     * The sheet used to state §6's rule verbatim (*"how far you move that goal from the
     * moment you joined"*), which stopped being true on 2026-08-25 in two ways at once: a
     * **dated** challenge now scores its own window for everyone, so a retroactive race can
     * be joined after it ended; and Health Connect is now a choice beside the goals, so
     * *"that goal"* names something the reader may not have picked. A render frame is what
     * showed it — the old sentence sat directly above an option it contradicted.
     */
    val windowNote: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * One Health Connect metric, offered as a way to score a challenge.
 *
 * The row exists whenever the metric's kind matches the challenge's; whether it can
 * actually be taken depends on [GoalLinkState.healthAvailability], which arrives a beat
 * later and is deliberately **not** allowed to make the row disappear — a choice that
 * vanishes teaches nothing, and *"Health Connect is not set up on this phone"* is the
 * sentence the user needs.
 */
data class HealthLinkOption(
    val metric: HealthMetric,
    /** This metric's goal is already the one scoring this challenge. */
    val isCurrent: Boolean = false,
    /**
     * Taking this will create the Health Connect goal, because the user has never synced.
     *
     * Said in the row rather than discovered afterwards: a new row appearing on the Goals
     * screen unannounced is the one thing about this design somebody could fairly call a
     * surprise. It is the same goal the sync would have made on its own first run.
     */
    val createsGoal: Boolean = false,
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

/**
 * §3's propose dialog — the **only** way a challenge's measure ever changes.
 *
 * Seeded with what the challenge counts now, so the owner edits rather than retypes, and so
 * the dialog can say **live** which of the two consequences their edit carries.
 */
data class MeasureChangeState(
    val isVisible: Boolean = false,
    val challengeId: String = "",
    val challengeTitle: String = "",
    /** What it counts today — kept so [consequence] and [isAChange] can compare. */
    val currentKind: MeasureKind? = null,
    val currentWord: String = "",
    val kind: MeasureKind? = null,
    val word: String = "",
    /** How many people would have to agree, so the dialog can say so before they ask. */
    val participantCount: Int = 0,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Whether the edit differs from what the challenge already counts. */
    val isAChange: Boolean
        get() = kind != currentKind || word.trim() != currentWord.trim()

    /**
     * What proposing this would do — the same derivation `Challenge.pendingConsequence`
     * makes, but on a measure that has not been written yet, so the owner sees the
     * consequence **before** they ask everybody to agree to it.
     */
    val consequence: MeasureChangeConsequence
        get() = if (currentKind == null || currentKind == kind) {
            MeasureChangeConsequence.RELABEL
        } else {
            MeasureChangeConsequence.RESET
        }

    /** Whether anybody else has to agree, or the owner is alone and it just happens. */
    val needsOthers: Boolean get() = participantCount > 1
}
