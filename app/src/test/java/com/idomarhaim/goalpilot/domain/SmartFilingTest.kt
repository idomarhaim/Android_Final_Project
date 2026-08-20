package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.SmartFiling
import com.idomarhaim.goalpilot.domain.model.TaskClassification
import org.junit.Test

/**
 * `#6`'s branch table — `docs/PRODUCT_v0.3.md` §0.7, §3.4, §3.5.
 *
 * Three rows, and the ticket is the difference between them:
 *
 *  1. a `suggestedGoalId` that resolves → filed under it, **silently**;
 *  2. no goal id, but a proposal worth making → a **pending** goal, and the app **speaks**;
 *  3. no goal id and low confidence → `goalId` null, **no goal invented**, and it speaks.
 *
 * Every case here is a pure function call. That is not a convenience: §0.7's rule is about what
 * the app is allowed to *decide*, and a rule that can only be checked by driving a screen is a
 * rule that gets checked once.
 *
 * **Membership is not re-tested here**, and its absence is the design (§3.4 — validation lives
 * in the Cloud Function, singly; `functions/test/classify.test.mjs` is where it is checked).
 * What these cases exercise is *resolution*: whether an id names something the user actually
 * has. The two are different questions and a test that conflated them would be asserting a
 * second validator into existence.
 */
class SmartFilingTest {

    private val goals = listOf(
        Goal(id = "g-run", title = "Run a half marathon", category = GoalCategory.FITNESS),
        Goal(id = "g-read", title = "Read 20 books", category = GoalCategory.LEARNING),
    )
    private val areas = listOf(
        LifeArea(id = "a-health", name = "Health"),
        LifeArea(id = "a-mind", name = "Mind"),
    )

    /** A confident classification onto an existing goal. Spoil one field per case. */
    private fun onto(goalId: String?) = TaskClassification(
        suggestedGoalId = goalId,
        suggestedNewGoalTitle = "Get fit",
        suggestedCategory = GoalCategory.FITNESS,
        suggestedLifeAreaId = "a-health",
        confidence = 0.9f,
        rationale = "Matches your running goal.",
    )

    // ── Row 1 — an existing goal. Silent. ────────────────────────────

    @Test
    fun `a goal id that resolves files the task there, and says nothing`() {
        val decision = SmartFiling.decide(onto("g-run"), goals, areas)

        assertThat(decision).isEqualTo(FilingDecision.ExistingGoal("g-run", "Run a half marathon"))
        assertThat(decision.speaks).isFalse()
    }

    @Test
    fun `the existing-goal branch ignores the suggested new title entirely`() {
        // The model routinely answers with both. Taking the goal id and then ALSO minting the
        // proposed goal would create an intrinsic edge on a call that needed none.
        val decision = SmartFiling.decide(onto("g-run"), goals, areas)

        assertThat(decision).isInstanceOf(FilingDecision.ExistingGoal::class.java)
    }

    @Test
    fun `an existing goal is silent even when the sorter is barely sure`() {
        // §3.5's confidence rule is about INVENTING a goal. Filing under one the user already
        // has is instrumental (§0.7), so it is silent at any confidence — and routing a
        // low-confidence match to the new-goal branch would invent MORE goals, not fewer.
        val decision = SmartFiling.decide(onto("g-run").copy(confidence = 0.05f), goals, areas)

        assertThat(decision).isInstanceOf(FilingDecision.ExistingGoal::class.java)
        assertThat(decision.speaks).isFalse()
    }

    // ── Row 2 — a proposal. Speaks, and sits pending. ────────────────

    @Test
    fun `no goal id and a confident proposal takes the new-goal branch, which speaks`() {
        val decision = SmartFiling.decide(onto(null), goals, areas)

        assertThat(decision).isEqualTo(
            FilingDecision.NewGoal("Get fit", GoalCategory.FITNESS, "a-health"),
        )
        assertThat(decision.speaks).isTrue()
    }

    @Test
    fun `a goal id naming nothing the user has is the same as no goal id`() {
        // The Function drops a non-member id before it ever gets here (§3.4). This is the
        // second line: an id that survived validation and still resolves to nothing — the goal
        // was deleted between the request and the reply — must not file the task nowhere.
        val decision = SmartFiling.decide(onto("g-deleted"), goals, areas)

        assertThat(decision).isInstanceOf(FilingDecision.NewGoal::class.java)
    }

    @Test
    fun `the proposed title is taken as authored, trimmed and not otherwise touched`() {
        // §3.3 D: it is CONTENT the moment it lands in his list — never translated, never
        // re-rendered, and certainly never rewritten by the app that displays it.
        val hebrew = "לרוץ חצי מרתון"
        val decision = SmartFiling.decide(
            onto(null).copy(suggestedNewGoalTitle = "  $hebrew  "),
            goals,
            areas,
        )

        assertThat((decision as FilingDecision.NewGoal).title).isEqualTo(hebrew)
    }

    @Test
    fun `an unresolvable life area leaves the new goal unfiled rather than guessing`() {
        val decision = SmartFiling.decide(
            onto(null).copy(suggestedLifeAreaId = "a-deleted"),
            goals,
            areas,
        )

        assertThat((decision as FilingDecision.NewGoal).lifeAreaId).isNull()
    }

    // ── Row 3 — low confidence. No goal is invented. ─────────────────

    @Test
    fun `low confidence leaves goalId null and invents no goal`() {
        val decision = SmartFiling.decide(onto(null).copy(confidence = 0.2f), goals, areas)

        assertThat(decision).isInstanceOf(FilingDecision.NoGoal::class.java)
        assertThat(decision.speaks).isTrue()
    }

    @Test
    fun `the threshold is inclusive at its own boundary and exclusive below it`() {
        val at = onto(null).copy(confidence = SmartFiling.MIN_CONFIDENCE_FOR_NEW_GOAL)
        val below = onto(null).copy(confidence = SmartFiling.MIN_CONFIDENCE_FOR_NEW_GOAL - 0.01f)

        assertThat(SmartFiling.decide(at, goals, areas)).isInstanceOf(FilingDecision.NewGoal::class.java)
        assertThat(SmartFiling.decide(below, goals, areas)).isInstanceOf(FilingDecision.NoGoal::class.java)
    }

    @Test
    fun `an absent confidence is read as low, so nothing is invented on a number nobody stated`() {
        // §3.3's contract makes absence the signal for a field that failed validation, and
        // TaskClassification defaults it to 0f. The direction matters more than the default:
        // resolving silence as "sure enough" would let a malformed response mint a life goal.
        val decision = SmartFiling.decide(
            TaskClassification(suggestedNewGoalTitle = "Get fit"),
            goals,
            areas,
        )

        assertThat(decision).isInstanceOf(FilingDecision.NoGoal::class.java)
    }

    @Test
    fun `a confident classification with no title to propose still invents nothing`() {
        val decision = SmartFiling.decide(
            onto(null).copy(suggestedNewGoalTitle = null),
            goals,
            areas,
        )

        assertThat(decision).isInstanceOf(FilingDecision.NoGoal::class.java)
    }

    @Test
    fun `a blank proposed title is not a proposal`() {
        val decision = SmartFiling.decide(
            onto(null).copy(suggestedNewGoalTitle = "   "),
            goals,
            areas,
        )

        assertThat(decision).isInstanceOf(FilingDecision.NoGoal::class.java)
    }

    @Test
    fun `an unfiled task still keeps the life area the sorter could resolve`() {
        // The task lands with no goal, but the area is a fact about where the time went and it
        // is not thrown away with the goal that did not exist.
        val decision = SmartFiling.decide(onto(null).copy(confidence = 0.1f), goals, areas)

        assertThat((decision as FilingDecision.NoGoal).suggestedLifeAreaId).isEqualTo("a-health")
    }

    // ── The property the whole ticket rests on ───────────────────────

    @Test
    fun `exactly one branch is silent, and it is the one that files under an existing goal`() {
        val silent = listOf(
            SmartFiling.decide(onto("g-run"), goals, areas),
            SmartFiling.decide(onto("g-read"), goals, areas),
        )
        val speaking = listOf(
            SmartFiling.decide(onto(null), goals, areas),
            SmartFiling.decide(onto(null).copy(confidence = 0f), goals, areas),
        )

        assertThat(silent.map { it.speaks }).containsExactly(false, false)
        assertThat(speaking.map { it.speaks }).containsExactly(true, true)
    }

    @Test
    fun `a user with no goals at all never gets one invented for them`() {
        // The empty-list case is the first run of the app, and it is the worst possible moment
        // to assert that the first sentence someone typed is one of their life goals.
        val decision = SmartFiling.decide(onto(null).copy(confidence = 0.1f), emptyList(), areas)

        assertThat(decision).isInstanceOf(FilingDecision.NoGoal::class.java)
    }
}
