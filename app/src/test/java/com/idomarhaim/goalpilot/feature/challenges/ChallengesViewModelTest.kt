package com.idomarhaim.goalpilot.feature.challenges

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeInvite
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.Leaderboard
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureChangeConsequence
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ScoreSource
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.canBeScoredFrom
import com.idomarhaim.goalpilot.domain.model.isRetroactive
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.domain.model.scoringWindowFor
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import com.idomarhaim.goalpilot.domain.usecase.LinkChallengeToHealthUseCase
import com.idomarhaim.goalpilot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ChallengeRepository>(relaxed = true)
    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val socialRepository = mockk<SocialRepository>(relaxed = true)
    private val healthRepository = mockk<HealthRepository>(relaxed = true)
    private val linkToHealth = mockk<LinkChallengeToHealthUseCase>(relaxed = true)

    private val now = 1_000_000_000L

    /** One day in millis — the retroactive cases below are all in days. */
    private val DAY = 24L * 60 * 60 * 1000

    /**
     * A REAL instant, for the retroactive cases only.
     *
     * The class's [now] is the sentinel `1_000_000_000L`, which is about eleven and a half
     * days after the epoch — so `now - 14.days` is **negative**, and a negative
     * `startAtEpochMillis` correctly fails `scoringWindowFor`'s `> 0` test for *no start
     * date set*. The first draft of these cases used it and failed with the window falling
     * back to `joinedAt`, which reads exactly like the bug they exist to pin. The guard is
     * right; the fixture was eleven days old.
     */
    private val realNow = 1_760_000_000_000L

    private fun challenge(
        id: String = "c1",
        title: String = "Most km this week",
        startAt: Long = 0L,
        endAt: Long = 0L,
        measure: Measure? = Measure(MeasureKind.DISTANCE, "km"),
    ) = Challenge(
        id = id,
        title = title,
        measure = measure,
        ownerUid = "me",
        startAtEpochMillis = startAt,
        endAtEpochMillis = endAt,
    )

    private fun goal(
        id: String = "g1",
        title: String = "Run 100 km",
        measure: Measure? = Measure(MeasureKind.DISTANCE, "km"),
        archived: Boolean = false,
    ) = Goal(id = id, title = title, measure = measure, isArchived = archived)

    private fun friend(uid: String, name: String) = LeaderboardEntry(uid = uid, displayName = name)

    private fun viewModel(
        mine: List<ChallengeWithStandings> = emptyList(),
        discoverable: List<Challenge> = emptyList(),
        goals: List<Goal> = emptyList(),
        invites: List<ChallengeInvite> = emptyList(),
        sent: List<ChallengeInvite> = emptyList(),
        friends: List<LeaderboardEntry> = emptyList(),
        health: HealthAvailability = HealthAvailability.AVAILABLE,
    ): ChallengesViewModel {
        every { repository.observeMyChallenges() } returns flowOf(mine)
        every { repository.observeDiscoverable() } returns flowOf(discoverable)
        every { goalRepository.observeGoals(any()) } returns flowOf(goals)
        // ⚠️ EVERY FLOW `uiState` COMBINES MUST BE STUBBED, RELAXED OR NOT.
        //
        // `mockk(relaxed = true)` hands back a Flow that never emits, and `combine` waits
        // for a first value from all four -- so forgetting this line does not fail, it
        // HANGS, and `loaded()` times out with a message about the coroutine rather than
        // about the stub.
        every { repository.observeIncomingInvites() } returns flowOf(invites)
        every { repository.observeSentInvites() } returns flowOf(sent)
        every { socialRepository.observeLeaderboard(any()) } returns
            flowOf(Leaderboard(entries = friends))
        coEvery { healthRepository.availability() } returns health
        return ChallengesViewModel(
            repository,
            goalRepository,
            socialRepository,
            healthRepository,
            linkToHealth,
        ).apply { clock = { now } }
    }

    private suspend fun ChallengesViewModel.loaded() = uiState.first { !it.isLoading }

    // ── Phase stamping ────────────────────────────────────────────────

    @Test
    fun `a challenge with no dates is active, not expired in 1970`() = runTest {
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))

        val card = vm.loaded().mine.single()

        assertThat(card.phase).isEqualTo(ChallengePhase.ACTIVE)
        assertThat(card.canReportScore).isTrue()
    }

    @Test
    fun `an upcoming challenge cannot be scored yet`() = runTest {
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge(startAt = now + 5_000))),
        )

        val card = vm.loaded().mine.single()

        assertThat(card.phase).isEqualTo(ChallengePhase.UPCOMING)
        assertThat(card.canReportScore).isFalse()
    }

    @Test
    fun `an ended challenge cannot be scored`() = runTest {
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge(endAt = now - 1))),
        )

        assertThat(vm.loaded().mine.single().canReportScore).isFalse()
    }

    @Test
    fun `an ended challenge offers no join button, an upcoming one does`() = runTest {
        val vm = viewModel(
            discoverable = listOf(
                challenge(id = "over", endAt = now - 1),
                challenge(id = "soon", startAt = now + 5_000),
            ),
        )

        val discoverable = vm.loaded().discoverable.associateBy { it.challenge.id }

        assertThat(discoverable.getValue("over").canJoin).isFalse()
        assertThat(discoverable.getValue("soon").canJoin).isTrue()
    }

    @Test
    fun `the end bound is exclusive, so a challenge is still active on its final day`() = runTest {
        // Ends "today": the stored bound is tomorrow's midnight, so right now is
        // inside the challenge, not after it.
        val today = LocalDate.now()
        val endBound = DateTimeUtils.startOfDay(today.plusDays(1))
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge(endAt = endBound))))
            .apply { clock = { System.currentTimeMillis() } }

        assertThat(vm.loaded().mine.single().phase).isEqualTo(ChallengePhase.ACTIVE)
    }

    // ── Standings pass through untouched ──────────────────────────────

    @Test
    fun `ranks arrive already stamped and are not re-derived from position`() = runTest {
        val participants = listOf(
            ChallengeParticipant(uid = "a", displayName = "Ann", score = 10.0),
            ChallengeParticipant(uid = "b", displayName = "Ben", score = 10.0),
            ChallengeParticipant(uid = "c", displayName = "Cy", score = 4.0),
        )
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(),
                    standings = participants.rankedByScore(currentUid = "c"),
                    hasJoined = true,
                ),
            ),
        )

        val card = vm.loaded().mine.single()

        // Joint ranks survive: 1, 1, 3 — not 1, 2, 3.
        assertThat(card.standings.map { it.rank }).containsExactly(1, 1, 3).inOrder()
        assertThat(card.myStanding?.uid).isEqualTo("c")
        assertThat(card.participantCount).isEqualTo(3)
    }

    // ── Reporting a score ─────────────────────────────────────────────

    @Test
    fun `a comma decimal is accepted, because a European keypad offers one`() = runTest {
        val card = ChallengeCard(
            ChallengeWithStandings(challenge = challenge(), hasJoined = true),
            ChallengePhase.ACTIVE,
        )
        val vm = viewModel()
        coEvery { repository.reportScore(any(), any()) } returns Resource.Success(Unit)

        vm.openScoreEntry(card)
        vm.onScoreChange("12,5")
        vm.submitScore()

        coVerify { repository.reportScore("c1", 12.5) }
        assertThat(vm.scoreEntry.value.isVisible).isFalse()
    }

    @Test
    fun `a non-numeric score is refused before it reaches the repository`() = runTest {
        val vm = viewModel()

        vm.openScoreEntry(
            ChallengeCard(ChallengeWithStandings(challenge = challenge()), ChallengePhase.ACTIVE),
        )
        vm.onScoreChange("lots")
        vm.submitScore()

        assertThat(vm.scoreEntry.value.error).isEqualTo("Enter a number")
        coVerify(exactly = 0) { repository.reportScore(any(), any()) }
    }

    @Test
    fun `a negative score is refused`() = runTest {
        val vm = viewModel()

        vm.openScoreEntry(
            ChallengeCard(ChallengeWithStandings(challenge = challenge()), ChallengePhase.ACTIVE),
        )
        vm.onScoreChange("-3")
        vm.submitScore()

        assertThat(vm.scoreEntry.value.error).isEqualTo("A score cannot be negative")
        coVerify(exactly = 0) { repository.reportScore(any(), any()) }
    }

    @Test
    fun `the score dialog opens pre-filled with what was already reported`() = runTest {
        val standings = listOf(ChallengeParticipant(uid = "me", score = 7.0)).rankedByScore("me")
        val vm = viewModel()

        vm.openScoreEntry(
            ChallengeCard(
                ChallengeWithStandings(challenge = challenge(), standings = standings),
                ChallengePhase.ACTIVE,
            ),
        )

        // "7", not "7.0" — a whole number should not read as a measurement.
        assertThat(vm.scoreEntry.value.value).isEqualTo("7")
    }

    @Test
    fun `a zero score opens blank rather than pre-filled with 0`() = runTest {
        val standings = listOf(ChallengeParticipant(uid = "me", score = 0.0)).rankedByScore("me")
        val vm = viewModel()

        vm.openScoreEntry(
            ChallengeCard(
                ChallengeWithStandings(challenge = challenge(), standings = standings),
                ChallengePhase.ACTIVE,
            ),
        )

        assertThat(vm.scoreEntry.value.value).isEmpty()
    }

    @Test
    fun `a repository refusal keeps the dialog open and shows its own message`() = runTest {
        val vm = viewModel()
        coEvery { repository.reportScore(any(), any()) } returns
            Resource.Error("Join the challenge before reporting a score")

        vm.openScoreEntry(
            ChallengeCard(ChallengeWithStandings(challenge = challenge()), ChallengePhase.ACTIVE),
        )
        vm.onScoreChange("5")
        vm.submitScore()

        assertThat(vm.scoreEntry.value.isVisible).isTrue()
        assertThat(vm.scoreEntry.value.isSaving).isFalse()
        assertThat(vm.scoreEntry.value.error)
            .isEqualTo("Join the challenge before reporting a score")
    }

    // ── Creating ──────────────────────────────────────────────────────

    @Test
    fun `a blank title never reaches the repository`() = runTest {
        val vm = viewModel()

        vm.openEditor()
        vm.onTitleChange("   ")
        vm.saveEditor()

        assertThat(vm.editor.value.error).isEqualTo("Give the challenge a name")
        coVerify(exactly = 0) { repository.createChallenge(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a challenge that would end before it starts is refused in the dialog`() = runTest {
        val vm = viewModel()

        vm.openEditor()
        vm.onTitleChange("Backwards")
        // A valid measure, so this reaches the DATE check: the dialog reports the first
        // problem in reading order (name, then what it counts, then dates), and without
        // one this would fail on the measure instead and prove nothing about the dates.
        vm.onMeasureKindChange(MeasureKind.DISTANCE)
        vm.onStartChange(now + 10_000)
        vm.onEndChange(now)
        vm.saveEditor()

        assertThat(vm.editor.value.error).isEqualTo("The challenge would end before it starts")
        coVerify(exactly = 0) { repository.createChallenge(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `picking a kind re-suggests the word until the user types one`() = runTest {
        val vm = viewModel()

        vm.openEditor()
        vm.onMeasureKindChange(MeasureKind.DISTANCE)
        assertThat(vm.editor.value.measureWord).isEqualTo("km")

        vm.onMeasureKindChange(MeasureKind.DURATION)
        assertThat(vm.editor.value.measureWord).isEqualTo("hours")

        // Once it is theirs, changing the kind must not take it back.
        vm.onMeasureWordChange("naps")
        vm.onMeasureKindChange(MeasureKind.COUNT)
        assertThat(vm.editor.value.measureWord).isEqualTo("naps")
    }

    @Test
    fun `a challenge with no measure never reaches the repository`() = runTest {
        // §6: "A challenge has no optional measure" -- there is nothing to compare
        // without a shared unit, and the kind is what a participant's goal is matched
        // against. The old editor defaulted to `metricUnit = "points"`, which is the
        // default that produced #23; there is deliberately no default kind now.
        val vm = viewModel()

        vm.openEditor()
        vm.onTitleChange("Nameless units")
        vm.saveEditor()

        assertThat(vm.editor.value.error).isEqualTo("Say what this challenge counts")
        coVerify(exactly = 0) { repository.createChallenge(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a kind with a blanked-out word is still refused`() = runTest {
        // Picking a kind fills the word in, so the only way here is deleting it again --
        // which is a user saying "not that word" and not a user saying "no word".
        val vm = viewModel()

        vm.openEditor()
        vm.onTitleChange("Step it up")
        vm.onMeasureKindChange(MeasureKind.COUNT)
        vm.onMeasureWordChange("   ")
        vm.saveEditor()

        assertThat(vm.editor.value.error).isEqualTo("Say what this challenge counts")
        coVerify(exactly = 0) { repository.createChallenge(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a created challenge carries the measure the user picked`() = runTest {
        val vm = viewModel()
        coEvery {
            repository.createChallenge(any(), any(), any(), any(), any())
        } returns Resource.Success("new-id")

        vm.openEditor()
        vm.onTitleChange("Step it up")
        vm.onMeasureKindChange(MeasureKind.COUNT)
        vm.onMeasureWordChange(" steps ")
        vm.saveEditor()

        coVerify {
            repository.createChallenge(
                "Step it up",
                "",
                Measure(MeasureKind.COUNT, "steps"),
                0L,
                0L,
            )
        }
        assertThat(vm.editor.value.isVisible).isFalse()
    }

    // ── §6: linking a goal is what makes the number move ──────────────

    @Test
    fun `only goals of the challenge's kind can score it`() = runTest {
        val vm = viewModel(
            goals = listOf(
                goal(id = "km", measure = Measure(MeasureKind.DISTANCE, "km")),
                goal(id = "miles", measure = Measure(MeasureKind.DISTANCE, "miles")),
                goal(id = "books", measure = Measure(MeasureKind.COUNT, "books")),
                goal(id = "unmeasured", measure = null),
            ),
        )
        vm.loaded()

        val eligible = vm.eligibleGoalsFor(challenge()).map { it.id }

        // MATCHED ON THE KIND, NEVER THE WORD. "miles" is a distance goal in somebody's
        // own word, and §1.3 says the word is user content that is never translated --
        // matching on it would keep a Hebrew user out of an English steps race for a
        // reason that has nothing to do with what is being counted.
        assertThat(eligible).containsExactly("km", "miles").inOrder()
    }

    @Test
    fun `an archived goal is not offered, even of the right kind`() = runTest {
        val vm = viewModel(goals = listOf(goal(id = "old", archived = true)))
        vm.loaded()

        assertThat(vm.eligibleGoalsFor(challenge())).isEmpty()
    }

    @Test
    fun `a challenge with no measure can match no goal at all`() = runTest {
        // The pre-§6 document whose `metricUnit` was the "points" default. There is no
        // kind to match against, so the picker must not be offered rather than offering
        // every goal the user has.
        val vm = viewModel(goals = listOf(goal()))
        vm.loaded()

        assertThat(vm.eligibleGoalsFor(challenge(measure = null))).isEmpty()
    }

    @Test
    fun `the goal picker knows which goal is already linked`() = runTest {
        val card = ChallengeWithStandings(
            challenge = challenge(),
            hasJoined = true,
            myLinkedGoalId = "g1",
        )
        val vm = viewModel(mine = listOf(card), goals = listOf(goal()))

        vm.openGoalLink(vm.loaded().mine.single())

        assertThat(vm.goalLink.value.isVisible).isTrue()
        assertThat(vm.goalLink.value.linkedGoalId).isEqualTo("g1")
        assertThat(vm.goalLink.value.eligible.map { it.id }).containsExactly("g1")
    }

    @Test
    fun `linking says the challenge now scores itself, not that a write succeeded`() = runTest {
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge(), hasJoined = true)),
            goals = listOf(goal()),
        )
        coEvery { repository.linkGoal("c1", "g1") } returns Resource.Success(Unit)

        vm.openGoalLink(vm.loaded().mine.single())
        vm.linkGoal("g1")

        coVerify { repository.linkGoal("c1", "g1") }
        assertThat(vm.goalLink.value.isVisible).isFalse()
        assertThat(vm.message.value).isEqualTo("Linked — this challenge now scores itself")
    }

    @Test
    fun `a link failure keeps the sheet open with the repository's own wording`() = runTest {
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge(), hasJoined = true)),
            goals = listOf(goal()),
        )
        coEvery { repository.linkGoal(any(), any()) } returns Resource.Error("Not signed in")

        vm.openGoalLink(vm.loaded().mine.single())
        vm.linkGoal("g1")

        assertThat(vm.goalLink.value.isVisible).isTrue()
        assertThat(vm.goalLink.value.error).isEqualTo("Not signed in")
    }

    @Test
    fun `the card says whether this challenge is scoring itself`() = runTest {
        val linked = ChallengeWithStandings(
            challenge = challenge(id = "linked"),
            hasJoined = true,
            myLinkedGoalId = "g1",
        )
        val typed = ChallengeWithStandings(
            challenge = challenge(id = "typed"),
            hasJoined = true,
        )
        val unmeasured = ChallengeWithStandings(
            challenge = challenge(id = "legacy", measure = null),
            hasJoined = true,
        )
        val vm = viewModel(mine = listOf(linked, typed, unmeasured))

        val cards = vm.loaded().mine.associateBy { it.challenge.id }

        assertThat(cards.getValue("linked").isLinked).isTrue()
        assertThat(cards.getValue("typed").isLinked).isFalse()
        // Both of the measured ones may link; the pre-§6 one has no kind to match on,
        // so the action is withheld rather than opening an empty picker.
        assertThat(cards.getValue("linked").canLinkGoal).isTrue()
        assertThat(cards.getValue("typed").canLinkGoal).isTrue()
        assertThat(cards.getValue("legacy").canLinkGoal).isFalse()
    }

    @Test
    fun `typing a score warns first when it would take the challenge off its goal`() = runTest {
        val linked = ChallengeWithStandings(
            challenge = challenge(id = "linked"),
            hasJoined = true,
            myLinkedGoalId = "g1",
        )
        val typed = ChallengeWithStandings(challenge = challenge(id = "typed"), hasJoined = true)
        val vm = viewModel(mine = listOf(linked, typed))
        val cards = vm.loaded().mine.associateBy { it.challenge.id }

        vm.openScoreEntry(cards.getValue("linked"))
        // Said BEFORE the write. A linked challenge is scoring itself, and discovering
        // afterwards from a badge on your own row that you switched it off would be the
        // app having made the decision.
        assertThat(vm.scoreEntry.value.replacesLink).isTrue()

        vm.dismissScoreEntry()
        vm.openScoreEntry(cards.getValue("typed"))
        assertThat(vm.scoreEntry.value.replacesLink).isFalse()
    }

    @Test
    fun `the score dialog labels its field in the challenge's own word`() = runTest {
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.COUNT, "steps")),
                    hasJoined = true,
                ),
            ),
        )

        vm.openScoreEntry(vm.loaded().mine.single())

        assertThat(vm.scoreEntry.value.metricWord).isEqualTo("steps")
    }

    // ── §6 / Ido's third ask: a typed score says so ───────────────────

    @Test
    fun `only a reported standing is badged`() {
        // The absence of a badge is the honest default: a challenge is MEANT to score
        // itself from a goal, so a derived row is the ordinary case and a badge on every
        // row would be noise. A row nobody has scored yet says nothing either.
        val standings = listOf(
            ChallengeParticipant(uid = "a", score = 9.0, source = ScoreSource.DERIVED),
            ChallengeParticipant(uid = "b", score = 8.0, source = ScoreSource.REPORTED),
            ChallengeParticipant(uid = "c", score = 0.0, source = ScoreSource.NONE),
        ).rankedByScore().associateBy { it.uid }

        assertThat(standings.getValue("a").isReported).isFalse()
        assertThat(standings.getValue("b").isReported).isTrue()
        assertThat(standings.getValue("c").isReported).isFalse()
    }

    @Test
    fun `a create failure keeps the dialog open with the typed values intact`() = runTest {
        val vm = viewModel()
        coEvery {
            repository.createChallenge(any(), any(), any(), any(), any())
        } returns Resource.Error("Not signed in")

        vm.openEditor()
        vm.onTitleChange("Most km")
        vm.onMeasureKindChange(MeasureKind.DISTANCE)
        vm.saveEditor()

        assertThat(vm.editor.value.isVisible).isTrue()
        assertThat(vm.editor.value.title).isEqualTo("Most km")
        assertThat(vm.editor.value.measureWord).isEqualTo("km")
        assertThat(vm.editor.value.error).isEqualTo("Not signed in")
    }

    // ── Joining and leaving ───────────────────────────────────────────

    @Test
    fun `a join failure surfaces the repository's own wording`() = runTest {
        val vm = viewModel()
        coEvery { repository.joinChallenge("gone") } returns
            Resource.Error("That challenge no longer exists")

        vm.join("gone")

        assertThat(vm.message.value).isEqualTo("That challenge no longer exists")
    }

    @Test
    fun `leaving closes the standings sheet it was opened from`() = runTest {
        val vm = viewModel()
        coEvery { repository.leaveChallenge("c1") } returns Resource.Success(Unit)

        vm.openDetail("c1")
        vm.leave("c1")

        assertThat(vm.detailId.value).isNull()
        assertThat(vm.message.value).isEqualTo("You left the challenge")
    }

    @Test
    fun `leaving one challenge does not close another challenge's sheet`() = runTest {
        val vm = viewModel()
        coEvery { repository.leaveChallenge(any()) } returns Resource.Success(Unit)

        vm.openDetail("c1")
        vm.leave("c2")

        assertThat(vm.detailId.value).isEqualTo("c1")
    }

    // ── Failure of the streams themselves ─────────────────────────────

    @Test
    fun `a stream failure stops the spinner instead of loading forever`() = runTest {
        every { repository.observeMyChallenges() } returns
            kotlinx.coroutines.flow.flow { throw IllegalStateException("PERMISSION_DENIED") }
        every { repository.observeDiscoverable() } returns flowOf(emptyList())
        every { goalRepository.observeGoals(any()) } returns flowOf(emptyList())
        every { repository.observeIncomingInvites() } returns flowOf(emptyList())
        val vm = ChallengesViewModel(
            repository,
            goalRepository,
            socialRepository,
            healthRepository,
            linkToHealth,
        )

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.error).isEqualTo("PERMISSION_DENIED")
        assertThat(state.mine).isEmpty()
    }

    // ── Formatting ────────────────────────────────────────────────────

    @Test
    fun `whole scores lose their trailing zero, fractional ones keep it`() {
        assertThat(ChallengesViewModel.format(7.0)).isEqualTo("7")
        assertThat(ChallengesViewModel.format(12.5)).isEqualTo("12.5")
        assertThat(ChallengesViewModel.format(0.0)).isEqualTo("0")
    }

    // ── Date conversion ───────────────────────────────────────────────

    @Test
    fun `a picked day becomes local midnight, not the UTC instant the picker returned`() {
        val picked = LocalDate.of(2026, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val start = localStartOfPickedDay(picked)

        assertThat(start).isEqualTo(DateTimeUtils.startOfDay(LocalDate.of(2026, 8, 10)))
    }

    @Test
    fun `an end date runs through the chosen day rather than ending at its midnight`() {
        val picked = LocalDate.of(2026, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val end = exclusiveEndOfPickedDay(picked)

        // The bound is the *next* midnight, so noon on the 10th is still inside it.
        assertThat(end).isEqualTo(DateTimeUtils.startOfDay(LocalDate.of(2026, 8, 11)))
        assertThat(DateTimeUtils.startOfDay(LocalDate.of(2026, 8, 10)) < end).isTrue()
    }

    @Test
    fun `a same-day challenge is active during its one day, not already over`() {
        val picked = LocalDate.of(2026, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val start = localStartOfPickedDay(picked)
        val end = exclusiveEndOfPickedDay(picked)
        val noon = start + 12 * 60 * 60 * 1000L

        val phase = Challenge(startAtEpochMillis = start, endAtEpochMillis = end).phaseAt(noon)

        assertThat(phase).isEqualTo(ChallengePhase.ACTIVE)
    }

    // ── #50 · what the standings read knows about itself ────────────────

    @Test
    fun `the standings as-of stamp reaches the card the sheet draws from`() = runTest {
        // The caption is drawn from this and from nothing else — no clock reading,
        // no connectivity check. Participant rows are somebody else's documents, so
        // this is one of exactly two surfaces in v0.3 that owes a stamp at all.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(),
                    standingsFreshness = Freshness(asOfEpochMillis = 1_760_000_000_000L),
                ),
            ),
        )

        val card = vm.loaded().mine.single()

        assertThat(card.standingsFreshness.asOfEpochMillis).isEqualTo(1_760_000_000_000L)
        assertThat(card.standingsFreshness.hasStamp).isTrue()
    }

    @Test
    fun `standings never fetched on this device are never-loaded, not empty`() = runTest {
        // A challenge whose participants have never been read renders
        // "Not loaded yet" rather than an empty standings list, which would state
        // that nobody has joined — a fact about other people's data the app has
        // never read (#50 item 3).
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(),
                    standingsFreshness = Freshness(neverLoaded = true),
                ),
            ),
        )

        val card = vm.loaded().mine.single()

        assertThat(card.standings).isEmpty()
        assertThat(card.standingsFreshness.neverLoaded).isTrue()
    }

    @Test
    fun `standings that reached the server and found nobody are an ordinary empty`() = runTest {
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))

        val card = vm.loaded().mine.single()

        assertThat(card.standingsFreshness.neverLoaded).isFalse()
        assertThat(card.standingsFreshness.hasStamp).isFalse()
    }

    // ── §1 · inviting a friend to a challenge ───────────────────────────
    //
    // Ido, 2026-08-24: *"in the version I have on my phone I can create a CHALLENGE but I
    // cannot invite a friend I have in the app to the CHALLENGE"*. These are the
    // behaviours that report turns into, minus the two that only a picture can settle
    // (does the row read as an offer? is the icon findable?) — those are
    // `ChallengeInviteRenderPass`.

    private fun invite(
        id: String = "i1",
        challengeId: String = "c1",
        challengeTitle: String = "Most km this week",
        fromUid: String = "friend-a",
        fromName: String = "Ann",
        toUid: String = "me",
    ) = ChallengeInvite(
        id = id,
        challengeId = challengeId,
        challengeTitle = challengeTitle,
        fromUid = fromUid,
        fromName = fromName,
        toUid = toUid,
        createdAtEpochMillis = now,
    )

    private suspend fun ChallengesViewModel.openedInvite(card: ChallengeCard): InviteState {
        openInvite(card)
        return invite.first { it.isVisible }
    }

    @Test
    fun `an invite waiting for me reaches the screen state`() = runTest {
        val vm = viewModel(invites = listOf(invite()))

        val state = vm.loaded()

        assertThat(state.invites.map { it.id }).containsExactly("i1")
        assertThat(state.invites.single().senderLabel).isEqualTo("Ann")
    }

    @Test
    fun `a sender who never set a name still reads as somebody, not as a blank`() = runTest {
        val vm = viewModel(invites = listOf(invite(fromName = "")))

        assertThat(vm.loaded().invites.single().senderLabel).isEqualTo("Someone")
    }

    @Test
    fun `the invite sheet offers my friends and never me`() = runTest {
        // `observeLeaderboard(friendsOnly = true)` returns the friends AND the signed-in
        // user, because it is a leaderboard and you are on it. Inviting yourself into a
        // challenge you are already in is the one row that must never appear.
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge())),
            friends = listOf(
                friend("f1", "Ann"),
                friend("f2", "Boaz"),
                LeaderboardEntry(uid = "me", displayName = "Ido", isCurrentUser = true),
            ),
        )
        val card = vm.loaded().mine.single()

        val sheet = vm.openedInvite(card)

        assertThat(sheet.candidates.map { it.uid }).containsExactly("f1", "f2").inOrder()
        assertThat(sheet.challengeTitle).isEqualTo("Most km this week")
    }

    @Test
    fun `a friend already in the challenge is shown blocked, not filtered away`() = runTest {
        // A friend who silently vanishes reads as "the app does not know them", which is
        // the one thing the user is certain is false. See `InviteCandidate`.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(),
                    standings = listOf(
                        ChallengeParticipant(uid = "f1", displayName = "Ann"),
                    ).rankedByScore("me"),
                ),
            ),
            friends = listOf(friend("f1", "Ann"), friend("f2", "Boaz")),
        )
        val card = vm.loaded().mine.single()

        val sheet = vm.openedInvite(card)

        val ann = sheet.candidates.single { it.uid == "f1" }
        assertThat(ann.isParticipant).isTrue()
        assertThat(ann.canInvite).isFalse()
        assertThat(sheet.candidates.single { it.uid == "f2" }.canInvite).isTrue()
    }

    @Test
    fun `a friend I already invited cannot be invited again`() = runTest {
        // A second invite is noise, not emphasis.
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge())),
            sent = listOf(invite(id = "s1", fromUid = "me", toUid = "f1")),
            friends = listOf(friend("f1", "Ann")),
        )
        val card = vm.loaded().mine.single()

        val ann = vm.openedInvite(card).candidates.single()

        assertThat(ann.isInvited).isTrue()
        assertThat(ann.canInvite).isFalse()
    }

    @Test
    fun `an invite I sent for a DIFFERENT challenge does not block this one`() = runTest {
        // `observeSentInvites` is one listener for every outstanding invite, so the
        // per-challenge filter is this layer's job and is the thing that would silently
        // over-block if it were dropped.
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge(id = "c1"))),
            sent = listOf(invite(id = "s1", challengeId = "other", fromUid = "me", toUid = "f1")),
            friends = listOf(friend("f1", "Ann")),
        )
        val card = vm.loaded().mine.single()

        assertThat(vm.openedInvite(card).candidates.single().canInvite).isTrue()
    }

    @Test
    fun `no friends at all is a state with a sentence, not an empty list`() = runTest {
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()

        assertThat(vm.openedInvite(card).hasNoFriends).isTrue()
    }

    @Test
    fun `sending an invite aims it at the open challenge and keeps the sheet up`() = runTest {
        // Inviting three people is one act with three taps; closing after each would make
        // it three trips.
        coEvery { repository.inviteToChallenge(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge(id = "c1"))),
            friends = listOf(friend("f1", "Ann"), friend("f2", "Boaz")),
        )
        val card = vm.loaded().mine.single()
        vm.openedInvite(card)

        vm.sendInvite("f2")

        coVerify { repository.inviteToChallenge("c1", "f2") }
        assertThat(vm.invite.value.isVisible).isTrue()
        assertThat(vm.message.value).isEqualTo("Invite sent")
    }

    @Test
    fun `a refused invite says why in the sheet, not in a snackbar behind it`() = runTest {
        coEvery { repository.inviteToChallenge(any(), any()) } returns
            Resource.Error("They are already in this challenge")
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge())),
            friends = listOf(friend("f1", "Ann")),
        )
        val card = vm.loaded().mine.single()
        vm.openedInvite(card)

        vm.sendInvite("f1")

        assertThat(vm.invite.value.error).isEqualTo("They are already in this challenge")
        assertThat(vm.message.value).isNull()
    }

    @Test
    fun `closing the sheet clears the error it was showing`() = runTest {
        coEvery { repository.inviteToChallenge(any(), any()) } returns Resource.Error("no")
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge())),
            friends = listOf(friend("f1", "Ann")),
        )
        val card = vm.loaded().mine.single()
        vm.openedInvite(card)
        vm.sendInvite("f1")

        vm.dismissInvite()

        assertThat(vm.invite.value.isVisible).isFalse()
        assertThat(vm.invite.value.error).isNull()
    }

    @Test
    fun `accepting an invite joins through the invite, not through a bare join`() = runTest {
        // The distinction is the whole point: `acceptInvite` batches the participant row
        // with the invite's deletion, so accepting cannot leave the offer standing beside
        // the membership it just produced.
        coEvery { repository.acceptInvite(any()) } returns Resource.Success(Unit)
        val vm = viewModel(invites = listOf(invite(id = "i1")))
        vm.loaded()

        vm.acceptInvite("i1")

        coVerify { repository.acceptInvite("i1") }
        coVerify(exactly = 0) { repository.joinChallenge(any()) }
        assertThat(vm.message.value).isEqualTo("You're in")
    }

    @Test
    fun `declining says nothing at all when it works`() = runTest {
        // A snackbar for "I said no thank you" is the app being pleased with itself about
        // a non-event. A failure still speaks.
        coEvery { repository.dismissInvite(any()) } returns Resource.Success(Unit)
        val vm = viewModel(invites = listOf(invite(id = "i1")))
        vm.loaded()

        vm.declineInvite("i1")

        coVerify { repository.dismissInvite("i1") }
        assertThat(vm.message.value).isNull()
        assertThat(vm.invitePendingId.value).isNull()
    }

    @Test
    fun `a decline that fails does speak`() = runTest {
        coEvery { repository.dismissInvite(any()) } returns Resource.Error("Offline")
        val vm = viewModel(invites = listOf(invite(id = "i1")))
        vm.loaded()

        vm.declineInvite("i1")

        assertThat(vm.message.value).isEqualTo("Offline")
    }

    // ── §2 · joining CREATES a goal, not only links one ─────────────────
    //
    // §6: "joining links or creates a goal, so a challenge hands you tracking you did not
    // have." Only the linking half shipped; a user with no goal of the challenge's kind
    // got a message and a trip to the Goals screen. §1 made the gap urgent -- the point of
    // inviting a friend is that they can compete, and a friend asked into a Steps Race may
    // well have no steps goal.

    private suspend fun ChallengesViewModel.openedLink(card: ChallengeCard): GoalLinkState {
        openGoalLink(card)
        return goalLink.value
    }

    @Test
    fun `the create form starts from the challenge's own title and measure`() = runTest {
        // Somebody joining "Most km this week" wants a km goal, and naming it after the
        // race is what they would have typed. The measure is carried whole so §2 can copy
        // it rather than make the user pick a matching kind out of a dropdown.
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()

        val link = vm.openedLink(card)

        assertThat(link.createTitle).isEqualTo("Most km this week")
        assertThat(link.measure).isEqualTo(Measure(MeasureKind.DISTANCE, "km"))
        // Blank on purpose: a challenge names a UNIT, never a finish line, so any
        // pre-filled number would be the app inventing an ambition on the user's behalf.
        assertThat(link.createTarget).isEmpty()
        assertThat(link.eligible).isEmpty()
    }

    @Test
    fun `creating copies the challenge's measure, so the new goal can score it`() = runTest {
        val saved = slot<Goal>()
        coEvery { goalRepository.upsertGoal(capture(saved)) } returns Resource.Success("g-new")
        coEvery { repository.linkGoal(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()
        vm.openedLink(card)
        vm.onCreateTargetChange("100")

        vm.createAndLinkGoal()

        // The whole point: scoreable BY CONSTRUCTION. If this ever diverges, the user gets
        // a goal that cannot score the challenge it was made for and nothing says why.
        assertThat(saved.captured.measure).isEqualTo(challenge().measure)
        assertThat(challenge().canBeScoredFrom(saved.captured)).isTrue()
        assertThat(saved.captured.title).isEqualTo("Most km this week")
        assertThat(saved.captured.targetValue).isEqualTo(100.0)
        // They typed it and pressed Create, so they declared it (§1.1, `#6`). Nothing else
        // may claim that.
        assertThat(saved.captured.declaredBy).isEqualTo(DeclaredBy.USER)
    }

    @Test
    fun `creating and linking are ONE act -- the link follows in the same call`() = runTest {
        coEvery { goalRepository.upsertGoal(any()) } returns Resource.Success("g-new")
        coEvery { repository.linkGoal(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge(id = "c1"))))
        val card = vm.loaded().mine.single()
        vm.openedLink(card)
        vm.onCreateTargetChange("100")

        vm.createAndLinkGoal()

        // Two acts would leave "a goal made for a challenge that is not scoring it"
        // reachable by closing the sheet in between.
        coVerify { repository.linkGoal("c1", "g-new") }
        assertThat(vm.goalLink.value.isVisible).isFalse()
        assertThat(vm.message.value).isEqualTo("Goal created — this challenge now scores itself")
    }

    @Test
    fun `a create that saves but fails to link says BOTH halves`() = runTest {
        // The half-done state, said out loud. Reporting success would leave somebody
        // looking at a challenge that is not scoring and a Goals screen that gained a row
        // they did not ask for, with nothing connecting the two.
        coEvery { goalRepository.upsertGoal(any()) } returns Resource.Success("g-new")
        coEvery { repository.linkGoal(any(), any()) } returns Resource.Error("Offline")
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()
        vm.openedLink(card)
        vm.onCreateTargetChange("100")

        vm.createAndLinkGoal()

        val state = vm.goalLink.value
        assertThat(state.isVisible).isTrue()
        assertThat(state.error).isEqualTo("Goal created, but linking it failed: Offline")
        assertThat(state.isSaving).isFalse()
    }

    @Test
    fun `a blank name is refused before anything is written`() = runTest {
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()
        vm.openedLink(card)
        vm.onCreateTitleChange("   ")
        vm.onCreateTargetChange("100")

        vm.createAndLinkGoal()

        assertThat(vm.goalLink.value.error).isEqualTo("Give the goal a name")
        coVerify(exactly = 0) { goalRepository.upsertGoal(any()) }
    }

    @Test
    fun `a target of zero or nonsense is refused, and nothing is written`() = runTest {
        // `Goal.progressFraction` divides by the target, and `hasMeasure` is false at zero
        // -- a goal created here with no target would render as unmeasured on the Goals
        // screen, which is the opposite of "tracking you did not have".
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()
        vm.openedLink(card)

        vm.onCreateTargetChange("0")
        vm.createAndLinkGoal()
        assertThat(vm.goalLink.value.error).isEqualTo("Set a target above zero")

        vm.onCreateTargetChange("banana")
        vm.createAndLinkGoal()
        assertThat(vm.goalLink.value.error).isEqualTo("Set a target above zero")

        coVerify(exactly = 0) { goalRepository.upsertGoal(any()) }
    }

    @Test
    fun `a comma decimal target is a real number, as on the score field`() = runTest {
        // A keypad on a Hebrew or European locale offers a comma, and "12,5" parsing to
        // null reads as the app rejecting a number the user can see is fine.
        val saved = slot<Goal>()
        coEvery { goalRepository.upsertGoal(capture(saved)) } returns Resource.Success("g")
        coEvery { repository.linkGoal(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        val card = vm.loaded().mine.single()
        vm.openedLink(card)
        vm.onCreateTargetChange("12,5")

        vm.createAndLinkGoal()

        assertThat(saved.captured.targetValue).isEqualTo(12.5)
    }

    @Test
    fun `an existing goal of the right kind is still offered, and no form is needed`() =
        runTest {
            // §2 changes the EMPTY branch only. A user who already has a suitable goal must
            // still land on the picker -- creating a second one for the same walk is the
            // duplicate `#47` is about.
            val vm = viewModel(
                mine = listOf(ChallengeWithStandings(challenge = challenge())),
                goals = listOf(goal(id = "g1", title = "Run 100 km")),
            )
            val card = vm.loaded().mine.single()

            val link = vm.openedLink(card)

            assertThat(link.eligible.map { it.id }).containsExactly("g1")
        }

    // ── §3 · changing the measure, with everybody's consent ─────────────
    //
    // §6: "the owner writes `pendingMeasure` on the challenge document, each participant
    // writes `approvedChangeId` in the one document they are permitted to write, and the
    // Function applies it when every row agrees."
    //
    // The three things §6 left open are settled here and asserted below: what "adapt"
    // means (nothing -- the consequence is DERIVED, not chosen), that the challenge keeps
    // scoring in the old unit while a change is pending, and that the quorum is everyone
    // who is STILL HERE.

    private fun pending(
        changeId: String = "chg-1",
        kind: MeasureKind = MeasureKind.COUNT,
        word: String = "steps",
    ) = challenge().copy(
        pendingChangeId = changeId,
        pendingMeasure = Measure(kind, word),
        pendingProposedAtEpochMillis = now,
    )

    @Test
    fun `while a change is pending the challenge still scores in the OLD unit`() = runTest {
        // §6's second open question, and the one a half-built version gets wrong: a
        // half-approved change must not stop the race. The pending fields sit BESIDE the
        // live measure, and nothing on the scoring path reads them.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = pending(kind = MeasureKind.COUNT, word = "steps"),
                    approvals = listOf("chg-1"),
                ),
            ),
            goals = listOf(goal(id = "g1")),
        )

        val card = vm.loaded().mine.single()

        assertThat(card.hasPendingMeasureChange).isTrue()
        // Still km, still DISTANCE, still linkable to a km goal, still reportable.
        assertThat(card.challenge.metricWord).isEqualTo("km")
        assertThat(card.canLinkGoal).isTrue()
        assertThat(card.canReportScore).isTrue()
        assertThat(vm.eligibleGoalsFor(card.challenge).map { it.id }).containsExactly("g1")
    }

    @Test
    fun `a KIND change is a reset, because there is no conversion to adapt with`() = runTest {
        // `C7` §5 offered the owner "reset or adapt". Adapt would need either a unit
        // conversion -- which `Measure`'s KDoc records this app deliberately does NOT do --
        // or a re-link, which restarts the number anyway. So a kind change IS a reset and
        // there is no second option to offer.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(challenge = pending(kind = MeasureKind.COUNT)),
            ),
        )

        assertThat(vm.loaded().mine.single().pendingConsequence)
            .isEqualTo(MeasureChangeConsequence.RESET)
    }

    @Test
    fun `a WORD-only change is a relabel -- every number and link survives`() = runTest {
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = pending(kind = MeasureKind.DISTANCE, word = "kilometres"),
                ),
            ),
        )

        assertThat(vm.loaded().mine.single().pendingConsequence)
            .isEqualTo(MeasureChangeConsequence.RELABEL)
    }

    @Test
    fun `the quorum counts everyone who is STILL HERE, not who used to be`() = runTest {
        // §6's third open question. A participant who leaves has no row to write
        // `approvedChangeId` in, so counting them would let one person walking away freeze
        // the challenge forever. Two rows, both agreeing.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = pending(),
                    standings = listOf(
                        ChallengeParticipant(uid = "me", displayName = "Ido"),
                        ChallengeParticipant(uid = "b", displayName = "Boaz"),
                    ).rankedByScore("me"),
                    approvals = listOf("chg-1", "chg-1"),
                    myApprovedChangeId = "chg-1",
                ),
            ),
        )

        val card = vm.loaded().mine.single()

        assertThat(card.participantCount).isEqualTo(2)
        assertThat(card.pendingApprovals).isEqualTo(2)
        assertThat(card.iApprovedPending).isTrue()
    }

    @Test
    fun `an approval of a DIFFERENT change does not count`() = runTest {
        // The replay defence, and the reason `pendingChangeId` exists at all: without it,
        // withdrawing and re-proposing would inherit consent nobody gave to the second one.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = pending(changeId = "chg-2"),
                    standings = listOf(
                        ChallengeParticipant(uid = "me"),
                        ChallengeParticipant(uid = "b"),
                    ).rankedByScore("me"),
                    approvals = listOf("chg-1", "chg-1"),
                    myApprovedChangeId = "chg-1",
                ),
            ),
        )

        val card = vm.loaded().mine.single()

        assertThat(card.pendingApprovals).isEqualTo(0)
        assertThat(card.iApprovedPending).isFalse()
    }

    @Test
    fun `a HALF-written proposal reads as NO proposal, exactly as the function reads it`() =
        runTest {
            // `functions/src/measureChange.ts#pendingFrom` refuses all-three-or-nothing.
            // If the client disagreed it would draw an approval banner for a change the
            // function will never apply -- one document, two languages, and they have to
            // read it the same way.
            val vm = viewModel(
                mine = listOf(
                    ChallengeWithStandings(
                        challenge = challenge().copy(pendingChangeId = "chg-1"),
                    ),
                ),
            )

            assertThat(vm.loaded().mine.single().hasPendingMeasureChange).isFalse()
        }

    @Test
    fun `the propose dialog opens seeded with what it counts NOW`() = runTest {
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = challenge())),
        )
        val card = vm.loaded().mine.single()

        vm.openMeasureChange(card)
        val d = vm.measureChange.value

        assertThat(d.isVisible).isTrue()
        assertThat(d.kind).isEqualTo(MeasureKind.DISTANCE)
        assertThat(d.word).isEqualTo("km")
        // Seeded, so it is not yet a change and the button is inert.
        assertThat(d.isAChange).isFalse()
    }

    @Test
    fun `the dialog says which consequence the edit carries, BEFORE anybody is asked`() =
        runTest {
            // "Everyone's score restarts at zero" is a thing to find out before asking four
            // people to agree to it, not after.
            val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
            vm.openMeasureChange(vm.loaded().mine.single())

            vm.onChangeWord("kilometres")
            assertThat(vm.measureChange.value.consequence)
                .isEqualTo(MeasureChangeConsequence.RELABEL)

            vm.onChangeKind(MeasureKind.COUNT)
            assertThat(vm.measureChange.value.consequence)
                .isEqualTo(MeasureChangeConsequence.RESET)
        }

    @Test
    fun `proposing nothing is refused before anything is written`() = runTest {
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))
        vm.openMeasureChange(vm.loaded().mine.single())

        vm.proposeMeasureChange()

        assertThat(vm.measureChange.value.error).isEqualTo("That is what it counts already")
        coVerify(exactly = 0) { repository.proposeMeasureChange(any(), any()) }
    }

    @Test
    fun `proposing on a shared challenge says it has to be agreed, not that it changed`() =
        runTest {
            coEvery { repository.proposeMeasureChange(any(), any()) } returns
                Resource.Success(Unit)
            val vm = viewModel(
                mine = listOf(
                    ChallengeWithStandings(
                        challenge = challenge(id = "c1"),
                        standings = listOf(
                            ChallengeParticipant(uid = "me"),
                            ChallengeParticipant(uid = "b"),
                        ).rankedByScore("me"),
                    ),
                ),
            )
            vm.openMeasureChange(vm.loaded().mine.single())
            vm.onChangeWord("miles")

            vm.proposeMeasureChange()

            coVerify {
                repository.proposeMeasureChange("c1", Measure(MeasureKind.DISTANCE, "miles"))
            }
            assertThat(vm.message.value).isEqualTo("Proposed — it changes when everyone agrees")
        }

    @Test
    fun `on a SOLO challenge it just happens, and the wording says so`() = runTest {
        // The owner's approval rides in the proposal's own batch, so a one-row challenge is
        // unanimous the instant it is proposed -- which is why the function also triggers
        // on the challenge document and not only on participant rows.
        coEvery { repository.proposeMeasureChange(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(),
                    standings = listOf(ChallengeParticipant(uid = "me")).rankedByScore("me"),
                ),
            ),
        )
        vm.openMeasureChange(vm.loaded().mine.single())
        vm.onChangeWord("miles")

        assertThat(vm.measureChange.value.needsOthers).isFalse()
        vm.proposeMeasureChange()

        assertThat(vm.message.value).isEqualTo("Measure updated")
    }

    @Test
    fun `approving passes the change id from the card, never a re-read`() = runTest {
        // So a stale screen cannot approve a proposal the user never saw: the function
        // compares this against the challenge's own `pendingChangeId`, and an approval of a
        // withdrawn proposal counts for nothing rather than carrying to its replacement.
        coEvery { repository.approveMeasureChange(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel(
            mine = listOf(ChallengeWithStandings(challenge = pending(changeId = "chg-7"))),
        )
        val card = vm.loaded().mine.single()

        vm.approveMeasureChange(card)

        coVerify { repository.approveMeasureChange("c1", "chg-7") }
        // Deliberately silent: the banner either disappears or re-renders with a new count,
        // and both say more than a snackbar would.
        assertThat(vm.message.value).isNull()
    }

    @Test
    fun `withdrawing is the owner's, and says so`() = runTest {
        coEvery { repository.withdrawMeasureChange(any()) } returns Resource.Success(Unit)
        val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = pending())))
        vm.loaded()

        vm.withdrawMeasureChange("c1")

        coVerify { repository.withdrawMeasureChange("c1") }
        assertThat(vm.message.value).isEqualTo("Change withdrawn")
    }

    // ── Health Connect as a first-class choice (Ido, 2026-08-25) ────────
    //
    // > "if I make a steps competition, there should also be an option to pull the logs
    // > straight into the CHALLENGE and not only through a personal GOAL of mine"
    //
    // He named the conflict with §6 himself and ruled the new instruction wins. What could
    // NOT move is a capability, not a preference: the scoring Function runs in the cloud
    // and cannot read Health Connect, so the option find-or-creates the canonical
    // `healthSourceKey` goal. These assert the CHOICE, which is the half he asked about.

    private fun steps(id: String = "hcg") = Goal(
        id = id,
        title = "Weekly steps",
        measure = Measure(MeasureKind.COUNT, "steps"),
        healthSourceKey = HealthMetric.STEPS.goalSourceKey,
    )

    @Test
    fun `a COUNT challenge is offered Health Connect steps`() = runTest {
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.COUNT, "steps")),
                ),
            ),
        )
        val card = vm.loaded().mine.single()

        val options = vm.healthOptionsFor(card)

        assertThat(options.map { it.metric }).containsExactly(HealthMetric.STEPS)
        // Nothing of the user's tracks steps yet, so taking it makes the goal -- and the
        // row has to say so before it happens, not after.
        assertThat(options.single().createsGoal).isTrue()
        assertThat(options.single().isCurrent).isFalse()
    }

    @Test
    fun `a DURATION challenge is offered sleep, not steps`() = runTest {
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.DURATION, "hours")),
                ),
            ),
        )

        val options = vm.healthOptionsFor(vm.loaded().mine.single())

        assertThat(options.map { it.metric }).containsExactly(HealthMetric.SLEEP)
    }

    @Test
    fun `a DISTANCE challenge is offered nothing -- Health Connect does not track it here`() =
        runTest {
            val vm = viewModel(mine = listOf(ChallengeWithStandings(challenge = challenge())))

            assertThat(vm.healthOptionsFor(vm.loaded().mine.single())).isEmpty()
        }

    @Test
    fun `an existing Health Connect goal is reused, not offered as a new one`() = runTest {
        // `#47` in miniature: matching on anything the user can edit produced a duplicate
        // goal on the next sync. `healthSourceKey` is the identity they cannot reach.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.COUNT, "steps")),
                ),
            ),
            goals = listOf(steps()),
        )

        assertThat(vm.healthOptionsFor(vm.loaded().mine.single()).single().createsGoal)
            .isFalse()
    }

    @Test
    fun `the option knows when it is ALREADY the thing scoring this challenge`() = runTest {
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.COUNT, "steps")),
                    myLinkedGoalId = "hcg",
                ),
            ),
            goals = listOf(steps(id = "hcg")),
        )

        assertThat(vm.healthOptionsFor(vm.loaded().mine.single()).single().isCurrent).isTrue()
    }

    @Test
    fun `an ARCHIVED health goal is not reused -- it was put away on purpose`() = runTest {
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.COUNT, "steps")),
                ),
            ),
            goals = listOf(steps().copy(isArchived = true)),
        )

        assertThat(vm.healthOptionsFor(vm.loaded().mine.single()).single().createsGoal)
            .isTrue()
    }

    @Test
    fun `an unavailable Health Connect still OFFERS the row, with the reason`() = runTest {
        // A choice that vanishes teaches nothing. The row stays, greyed, and the sheet says
        // "Health Connect is not set up on this phone" -- the sentence that actually helps.
        val vm = viewModel(
            mine = listOf(
                ChallengeWithStandings(
                    challenge = challenge(measure = Measure(MeasureKind.COUNT, "steps")),
                ),
            ),
            health = HealthAvailability.NOT_SUPPORTED,
        )
        val card = vm.loaded().mine.single()

        vm.openGoalLink(card)
        val link = vm.goalLink.first { it.healthAvailability != null }

        assertThat(link.healthOptions).hasSize(1)
        assertThat(link.healthAvailability).isEqualTo(HealthAvailability.NOT_SUPPORTED)
    }

    @Test
    fun `picking it goes through the use case, with the challenge and the user's goals`() =
        runTest {
            coEvery { linkToHealth(any(), any(), any()) } returns Resource.Success(Unit)
            val vm = viewModel(
                mine = listOf(
                    ChallengeWithStandings(
                        challenge = challenge(
                            id = "c1",
                            measure = Measure(MeasureKind.COUNT, "steps"),
                        ),
                    ),
                ),
                goals = listOf(steps()),
            )
            vm.openGoalLink(vm.loaded().mine.single())

            vm.linkHealthConnect(HealthMetric.STEPS)

            coVerify {
                linkToHealth(
                    match { it.id == "c1" },
                    HealthMetric.STEPS,
                    match { gs -> gs.any { it.healthSourceKey == HealthMetric.STEPS.goalSourceKey } },
                )
            }
            assertThat(vm.message.value).isEqualTo("Scoring from Health Connect — nothing to log")
            assertThat(vm.goalLink.value.isVisible).isFalse()
        }

    // ── Retroactive challenges (Ido, 2026-08-25) ────────────────────────
    //
    // > "I created a steps challenge from the start of last week to its end and invited
    // > rachil. If she accepted it, the challenge pulls both our data for that week and
    // > decides the winner."
    //
    // This could not work before: the window opened at max(joinedAt, startAt), so accepting
    // today gave a lower bound past the challenge's own end and it scored zero for everyone.

    @Test
    fun `a RETROACTIVE challenge scores its own week, however late you accepted`() {
        val weekStart = realNow - 14 * DAY
        val weekEnd = realNow - 7 * DAY
        val past = Challenge(startAtEpochMillis = weekStart, endAtEpochMillis = weekEnd)

        // rachil accepts today -- a month after the race, in the worst case.
        val window = past.scoringWindowFor(ChallengeParticipant(joinedAtEpochMillis = realNow))

        assertThat(window.fromEpochMillis).isEqualTo(weekStart)
        assertThat(window.untilEpochMillis).isEqualTo(weekEnd)
        // The point of the whole change: that week's readings are inside it.
        assertThat(window.includes(weekStart + 3 * DAY)).isTrue()
        // And the week before it still is not -- the race is the dates on the tin.
        assertThat(window.includes(weekStart - DAY)).isFalse()
    }

    @Test
    fun `an OPEN-ENDED challenge still opens at joinedAt -- §6's own protection`() {
        // What §6's rule was actually for: "joining with a year-old goal imports a year of
        // history nobody raced for." With no start date there is no other floor, so this
        // half is unchanged and has to stay unchanged.
        val open = Challenge(startAtEpochMillis = 0L, endAtEpochMillis = 0L)

        val window = open.scoringWindowFor(ChallengeParticipant(joinedAtEpochMillis = realNow))

        assertThat(window.fromEpochMillis).isEqualTo(realNow)
        assertThat(window.includes(realNow - DAY)).isFalse()
    }

    @Test
    fun `joining a DATED challenge late credits the whole window, deliberately`() {
        // The visible consequence of the change, asserted so nobody "fixes" it back. Join a
        // month-long race on the 20th and your first three weeks count -- which is what
        // "who walked most in August" means, and the only reading under which an invitation
        // to a finished week is worth accepting at all.
        val month = Challenge(startAtEpochMillis = realNow - 20 * DAY, endAtEpochMillis = realNow + DAY)

        val window = month.scoringWindowFor(ChallengeParticipant(joinedAtEpochMillis = realNow))

        assertThat(window.fromEpochMillis).isEqualTo(realNow - 20 * DAY)
        assertThat(window.includes(realNow - 15 * DAY)).isTrue()
    }

    @Test
    fun `a retroactive challenge is recognised as one, and is not scoreable by typing`() {
        val past = Challenge(
            startAtEpochMillis = realNow - 14 * DAY,
            endAtEpochMillis = realNow - 7 * DAY,
        )

        assertThat(past.isRetroactive(realNow)).isTrue()
        // A finished week is not something anybody should be typing a number into, so
        // `canReportScore` stays false for it -- the readings are the whole point.
        assertThat(ChallengeCard(ChallengeWithStandings(past), past.phaseAt(realNow)).canReportScore)
            .isFalse()
    }

    @Test
    fun `a live or upcoming challenge is not retroactive`() {
        assertThat(Challenge(startAtEpochMillis = realNow - DAY).isRetroactive(realNow)).isFalse()
        assertThat(
            Challenge(startAtEpochMillis = realNow + DAY, endAtEpochMillis = realNow + 8 * DAY)
                .isRetroactive(realNow),
        ).isFalse()
        // Undated: nothing to be retroactive about.
        assertThat(Challenge().isRetroactive(realNow)).isFalse()
    }
}
