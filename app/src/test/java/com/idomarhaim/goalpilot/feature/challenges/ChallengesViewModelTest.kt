package com.idomarhaim.goalpilot.feature.challenges

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ScoreSource
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.phaseAt
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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

    private val now = 1_000_000_000L

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

    private fun viewModel(
        mine: List<ChallengeWithStandings> = emptyList(),
        discoverable: List<Challenge> = emptyList(),
        goals: List<Goal> = emptyList(),
    ): ChallengesViewModel {
        every { repository.observeMyChallenges() } returns flowOf(mine)
        every { repository.observeDiscoverable() } returns flowOf(discoverable)
        every { goalRepository.observeGoals(any()) } returns flowOf(goals)
        return ChallengesViewModel(repository, goalRepository).apply { clock = { now } }
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
        val vm = ChallengesViewModel(repository, goalRepository)

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
}
