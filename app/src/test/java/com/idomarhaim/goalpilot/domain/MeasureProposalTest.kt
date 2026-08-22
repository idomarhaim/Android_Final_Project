package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalStructure
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureBasis
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
import com.idomarhaim.goalpilot.domain.model.ProposalOrigin
import com.idomarhaim.goalpilot.domain.model.TargetSource
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §3.4's mechanical proposal and §3.3 E's target arithmetic — spec §1.3, `C22`
 * [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
 * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65).
 *
 * ## What this suite is for, and what it deliberately is not
 *
 * §0.1 makes the non-AI half **mandatory**, so this is not a test of a degraded
 * path — it is the test of the path that runs when there is no key, no network
 * and no free-tier budget left. Everything here is pure: no repository, no
 * emulator, no Firebase, and no clock read (the week window takes its `now`).
 *
 * The **wire** half is tested next door in `functions/test/measure.test.mjs`,
 * where §3.4 puts validation *singly*. There is no Kotlin twin of the validator
 * here on purpose: two implementations of a membership test are two answers to
 * *"is this id real?"* that drift the first time one is edited.
 *
 * ## The dismissal's other half is NOT here, and could not be
 *
 * §1.3's *permanent, not snoozed* is stored in `SharedPreferences`, which needs a
 * device — so *a dismissed goal never offers again across process death* is
 * proved in `MeasureProposalUiTest`, on a real prefs file re-read through a fresh
 * repository instance. What **is** here is the half that is pure: that
 * [ProposeMeasureUseCase.isEligible] never consults a dismissal at all, so the
 * two concerns cannot be conflated.
 */
class MeasureProposalTest {

    private val unmeasured = Goal(id = "g1", title = "Get fit", measure = null)
    private val monday = LocalDateTime.of(2026, 8, 24, 9, 0)

    private fun step(id: String, done: Boolean = false, on: LocalDate? = null) = Task(
        id = id,
        title = "step $id",
        completion = if (done) CompletionFact(completedAtEpochMillis = 1L) else null,
        occurrence = on?.let { AllDay(it) },
    )

    // ── §3.4's three rows ───────────────────────────────────────────────────────

    @Test
    fun `two open steps propose counting the steps, as an outcome`() {
        val structure = GoalStructure(openStepCount = 2, totalStepCount = 2)
        val proposal = ProposeMeasureUseCase.mechanical(unmeasured, structure)!!

        assertThat(proposal.kind).isEqualTo(MeasureKind.COUNT)
        assertThat(proposal.word).isEqualTo(ProposeMeasureUseCase.STEPS_WORD)
        assertThat(proposal.targetSource).isEqualTo(TargetSource.STEPS)
        // The steps say how far along the GOAL is, so this is not a proxy.
        assertThat(proposal.basis).isEqualTo(MeasureBasis.OUTCOME)
        assertThat(proposal.origin).isEqualTo(ProposalOrigin.MECHANICAL)
        assertThat(proposal.target).isEqualTo(2.0)
    }

    @Test
    fun `one open step is not enough, and falls through to the schedule`() {
        // §3.4's gate is `openStepCount >= 2`, not `>= 1`: one step is a note, not a
        // structure to count. The boundary is the whole content of this test.
        val structure = GoalStructure(occurrencesPerWeek = 3, openStepCount = 1, totalStepCount = 1)
        val proposal = ProposeMeasureUseCase.mechanical(unmeasured, structure)!!

        assertThat(proposal.targetSource).isEqualTo(TargetSource.SCHEDULE)
        assertThat(proposal.target).isEqualTo(3.0)
    }

    @Test
    fun `a schedule alone proposes counting the occurrences, as a leading indicator`() {
        val structure = GoalStructure(occurrencesPerWeek = 3)
        val proposal = ProposeMeasureUseCase.mechanical(unmeasured, structure)!!

        assertThat(proposal.kind).isEqualTo(MeasureKind.COUNT)
        assertThat(proposal.word).isEqualTo(ProposeMeasureUseCase.SCHEDULE_WORD)
        assertThat(proposal.targetSource).isEqualTo(TargetSource.SCHEDULE)
        // §1.3: measure the recurring behaviour rather than fake an outcome number.
        assertThat(proposal.basis).isEqualTo(MeasureBasis.LEADING)
        assertThat(proposal.target).isEqualTo(3.0)
    }

    @Test
    fun `no structure at all proposes nothing, silently`() {
        assertThat(ProposeMeasureUseCase.mechanical(unmeasured, GoalStructure())).isNull()
        // One step and no schedule is still nothing: neither gate opens.
        assertThat(
            ProposeMeasureUseCase.mechanical(unmeasured, GoalStructure(openStepCount = 1)),
        ).isNull()
    }

    @Test
    fun `steps win over a schedule when the goal has both`() {
        // Both branches are true statements; the outcome is the stronger of the two,
        // and offering the proxy instead would be choosing the weaker on purpose.
        val structure = GoalStructure(occurrencesPerWeek = 3, openStepCount = 4, totalStepCount = 6)
        val proposal = ProposeMeasureUseCase.mechanical(unmeasured, structure)!!

        assertThat(proposal.targetSource).isEqualTo(TargetSource.STEPS)
        assertThat(proposal.basis).isEqualTo(MeasureBasis.OUTCOME)
    }

    // ── The target is the TOTAL, not the remainder ──────────────────────────────

    @Test
    fun `the steps target does not shrink as steps are completed`() {
        // §0.3's most-repeated finding, on the screen this ticket exists to fix: a
        // target computed from OPEN steps would read 6, then 5, then 4 as real work
        // happened — a number that quietly disagrees with itself. The prototype
        // renders the same reading ("2 of 8 done").
        val early = GoalStructure(openStepCount = 8, totalStepCount = 8)
        val later = GoalStructure(openStepCount = 6, totalStepCount = 8)

        assertThat(ProposeMeasureUseCase.mechanical(unmeasured, early)!!.target).isEqualTo(8.0)
        assertThat(ProposeMeasureUseCase.mechanical(unmeasured, later)!!.target).isEqualTo(8.0)
    }

    // ── §3.3 E: the app computes the number, always ─────────────────────────────

    @Test
    fun `a model proposal arrives with no number and leaves with the app's`() {
        // The whole design in one test. The wire carries a targetSource naming an
        // arithmetic; nothing that crossed it carries a value.
        val fromModel = MeasureProposal(
            goalId = "g1",
            kind = MeasureKind.COUNT,
            word = "runs a week",
            basis = MeasureBasis.LEADING,
            targetSource = TargetSource.SCHEDULE,
            target = null,
            origin = ProposalOrigin.MODEL,
        )
        val computed = ProposeMeasureUseCase
            .withComputedTarget(fromModel, GoalStructure(occurrencesPerWeek = 3))

        assertThat(computed.target).isEqualTo(3.0)
        // The model's own wording survives untouched — it is content (§3.5).
        assertThat(computed.word).isEqualTo("runs a week")
        assertThat(computed.origin).isEqualTo(ProposalOrigin.MODEL)
    }

    @Test
    fun `a USER source computes no target, and that is not a failure`() {
        val fromModel = MeasureProposal(
            goalId = "g1",
            kind = MeasureKind.MASS,
            word = "kg lost",
            basis = MeasureBasis.OUTCOME,
            targetSource = TargetSource.USER,
            target = null,
            origin = ProposalOrigin.MODEL,
        )
        // Even with structure sitting right there, USER means *ask him*. The app does
        // not quietly substitute a number it happens to have.
        val computed = ProposeMeasureUseCase.withComputedTarget(
            fromModel,
            GoalStructure(occurrencesPerWeek = 3, openStepCount = 5, totalStepCount = 5),
        )

        assertThat(computed.target).isNull()
        assertThat(computed.hasTarget).isFalse()
        // Still a whole, usable proposal: only the number was ever in doubt.
        assertThat(computed.toMeasure()).isEqualTo(Measure(MeasureKind.MASS, "kg lost"))
    }

    @Test
    fun `an arithmetic with nothing behind it yields a null target rather than a zero`() {
        // A model naming SCHEDULE on a goal with no schedule. Zero would be a number
        // the app made up and would render as a real target; null renders as *yours
        // to set*, which is the honest branch.
        val fromModel = MeasureProposal(
            goalId = "g1",
            kind = MeasureKind.COUNT,
            word = "sessions",
            basis = MeasureBasis.LEADING,
            targetSource = TargetSource.SCHEDULE,
            target = null,
            origin = ProposalOrigin.MODEL,
        )
        assertThat(
            ProposeMeasureUseCase.withComputedTarget(fromModel, GoalStructure()).target,
        ).isNull()
    }

    // ── Eligibility ─────────────────────────────────────────────────────────────

    @Test
    fun `a goal that already measures something is never offered one`() {
        val measured = unmeasured.copy(measure = Measure(MeasureKind.VOLUME, "L"))
        assertThat(
            ProposeMeasureUseCase.isEligible(measured, GoalStructure(occurrencesPerWeek = 3)),
        ).isFalse()
    }

    @Test
    fun `a word with a zero target counts as answered, not as unmeasured`() {
        // `hasMeasure` is false here (it needs a positive target), and using it as the
        // gate would re-offer a measure on a goal that has one — the offer arriving as
        // a correction, which is the ticket's named failure mode.
        val zeroTarget = unmeasured.copy(
            measure = Measure(MeasureKind.COUNT, "books"),
            targetValue = 0.0,
        )
        assertThat(zeroTarget.hasMeasure).isFalse()
        assertThat(
            ProposeMeasureUseCase.isEligible(zeroTarget, GoalStructure(occurrencesPerWeek = 3)),
        ).isFalse()
    }

    @Test
    fun `an unmeasured goal with no structure is not eligible`() {
        assertThat(ProposeMeasureUseCase.isEligible(unmeasured, GoalStructure())).isFalse()
        assertThat(
            ProposeMeasureUseCase.isEligible(unmeasured, GoalStructure(occurrencesPerWeek = 1)),
        ).isTrue()
    }

    // ── Reading structure off the tasks ─────────────────────────────────────────

    @Test
    fun `the week window counts occurrences inside the next seven days only`() {
        val tasks = listOf(
            step("a", on = monday.toLocalDate()),
            step("b", on = monday.toLocalDate().plusDays(2)),
            step("c", on = monday.toLocalDate().plusDays(6)),
            // Outside the window in both directions.
            step("d", on = monday.toLocalDate().plusDays(9)),
            step("e", on = monday.toLocalDate().minusDays(1)),
            // No occurrence at all — a step, not a scheduled thing.
            step("f"),
        )
        val structure = ProposeMeasureUseCase.structureOf(tasks, monday.toLocalDate().atStartOfDay())

        assertThat(structure.occurrencesPerWeek).isEqualTo(3)
        assertThat(structure.openStepCount).isEqualTo(6)
        assertThat(structure.totalStepCount).isEqualTo(6)
    }

    @Test
    fun `a done step counts toward the total and not toward the open count or the week`() {
        val tasks = listOf(
            step("a", done = true, on = monday.toLocalDate()),
            step("b", on = monday.toLocalDate().plusDays(1)),
            step("c", done = true),
        )
        val structure = ProposeMeasureUseCase.structureOf(tasks, monday.toLocalDate().atStartOfDay())

        // A completed occurrence is not a rhythm the goal is committed to.
        assertThat(structure.occurrencesPerWeek).isEqualTo(1)
        assertThat(structure.openStepCount).isEqualTo(1)
        assertThat(structure.totalStepCount).isEqualTo(3)
    }

    @Test
    fun `no tasks is an empty structure and therefore silence`() {
        val structure = ProposeMeasureUseCase.structureOf(emptyList(), monday)
        assertThat(structure.hasAnything).isFalse()
        assertThat(ProposeMeasureUseCase.mechanical(unmeasured, structure)).isNull()
    }

    // ── The proposal is an offer and nothing else ───────────────────────────────

    @Test
    fun `a proposal converts to a measure carrying exactly its kind and word`() {
        val proposal = ProposeMeasureUseCase
            .mechanical(unmeasured, GoalStructure(openStepCount = 3, totalStepCount = 3))!!
        assertThat(proposal.toMeasure())
            .isEqualTo(Measure(MeasureKind.COUNT, ProposeMeasureUseCase.STEPS_WORD))
    }

    @Test
    fun `every mechanical proposal names the goal it was built for`() {
        // Membership is the Cloud Function's job on the model path; on THIS path the
        // id cannot be wrong, and asserting it is what keeps the two paths
        // interchangeable at the call site.
        val other = Goal(id = "g2", title = "Read more")
        assertThat(
            ProposeMeasureUseCase.mechanical(other, GoalStructure(occurrencesPerWeek = 2))!!.goalId,
        ).isEqualTo("g2")
    }

    @Test
    fun `hasTarget is false for a zero or negative target as well as a null one`() {
        val base = ProposeMeasureUseCase
            .mechanical(unmeasured, GoalStructure(occurrencesPerWeek = 2))!!
        assertThat(base.hasTarget).isTrue()
        assertThat(base.copy(target = null).hasTarget).isFalse()
        // Zero would render as a real target and divide into a meaningless fraction.
        assertThat(base.copy(target = 0.0).hasTarget).isFalse()
        assertThat(base.copy(target = -1.0).hasTarget).isFalse()
    }
}
