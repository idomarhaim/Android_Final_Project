package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskEstimate
import com.idomarhaim.goalpilot.domain.usecase.BackfillDurationsUseCase
import com.idomarhaim.goalpilot.domain.usecase.DurationCandidate
import org.junit.Test

/**
 * Which tasks the AI re-estimation spends its per-run budget on, and which of its
 * answers are worth writing.
 *
 * The second half is the one that matters: a proposal that no model answered must
 * not be written as an AI estimate — the analytics card counts those, and the count
 * is the whole point of the feature. Since #9 that is **read**, not reconstructed:
 * `TaskEstimate.minutes` is absent when nobody answered.
 */
class BackfillDurationsUseCaseTest {

    private val useCase = BackfillDurationsUseCase()

    private val window = TimeWindow(startMillis = 1_000L, endMillisExclusive = 9_000L)

    private fun task(
        id: String,
        title: String = "Run five kilometres before work",
        minutes: Int? = null,
        done: Boolean = false,
        completedAt: Long? = null,
        createdAt: Long = 0L,
        durationSource: DurationSource = DurationSource.UNKNOWN,
    ) = Task(
        id = id,
        title = title,
        estimatedMinutes = minutes,
        durationSource = durationSource,
        createdAtEpochMillis = createdAt,
        completion = if (done) {
            CompletionFact(completedAtEpochMillis = completedAt ?: createdAt, minutes = minutes ?: 30)
        } else {
            null
        },
    )

    @Test
    fun `only tasks without a stored duration are candidates`() {
        val tasks = listOf(
            task("has-one", minutes = 45),
            task("zero", minutes = 0),
            task("none"),
            task("blank-title", title = "   "),
            task("").copy(title = "no id"),
        )

        val candidates = useCase(tasks, window)

        assertThat(candidates.map { it.taskId }).containsExactly("zero", "none")
    }

    @Test
    fun `a candidate carries the duration the chart is currently inferring`() {
        val candidates = useCase(listOf(task("t")), window)

        // `#55`: what the chart infers for a task with no duration is DEFAULT_MINUTES, full
        // stop. This asserted `fallbackMinutes(20)` — 20 points × 3 — which is the app
        // deriving how long your life took from a score that was itself a word count. The
        // inversion runs the other way now and that function is gone from the live path.
        assertThat(candidates.single().inferredMinutes)
            .isEqualTo(TaskDuration.DEFAULT_MINUTES)
    }

    @Test
    fun `tasks completed inside the window are asked about first`() {
        val tasks = listOf(
            task("open", createdAt = 8_000L),
            task("done-outside", done = true, completedAt = 50L),
            task("done-inside", done = true, completedAt = 2_000L),
        )

        val candidates = useCase(tasks, window)

        assertThat(candidates.map { it.taskId })
            .containsExactly("done-inside", "done-outside", "open").inOrder()
        assertThat(candidates.first().inWindow).isTrue()
        assertThat(candidates.last().inWindow).isFalse()
    }

    @Test
    fun `the cap keeps the candidates whose durations are on screen`() {
        val tasks = List(20) { index -> task("open-$index", createdAt = index.toLong()) } +
            task("visible", done = true, completedAt = 2_000L)

        val candidates = useCase(tasks, window, limit = 3)

        assertThat(candidates).hasSize(3)
        assertThat(candidates.first().taskId).isEqualTo("visible")
    }

    @Test
    fun `a run against the default cap matches the Google Tasks import cap`() {
        val tasks = List(40) { task("t-$it") }

        assertThat(useCase(tasks, window)).hasSize(BackfillDurationsUseCase.MAX_PER_RUN)
        assertThat(BackfillDurationsUseCase.MAX_PER_RUN).isEqualTo(15)
    }

    // ── Proposals ────────────────────────────────────────────────────

    private val candidate = DurationCandidate(
        taskId = "t",
        title = "Run five kilometres before work",
        inferredMinutes = 30,
        inWindow = true,
    )

    @Test
    fun `a real answer is selected and shown as a change`() {
        val proposal = useCase.propose(candidate, TaskEstimate(minutes = 75))

        assertThat(proposal.proposedMinutes).isEqualTo(75)
        assertThat(proposal.noModelAnswer).isFalse()
        assertThat(proposal.selected).isTrue()
        assertThat(proposal.changesTheChart).isTrue()
    }

    @Test
    fun `an answer that happens to match the old fallback numbers is now written`() {
        // Before #9 this arrived UNTICKED. `looksLikeFallback` recomputed the client
        // heuristic (5 + 3×words = 20 points, ×3 = 60 minutes) and rejected any
        // estimate that matched it — "evidence, not proof" by its own KDoc, because a
        // model is free to land on those numbers by agreement rather than by failure.
        // The repository now reports absence directly, so a real 60-minute answer is
        // a real answer and the false positive is gone rather than tolerated.
        //
        // `#55` removed the heuristic itself, so the coincidence this case is named for
        // can no longer be manufactured at all. Kept because what it asserts — that a
        // real answer is believed — is the behaviour, not the coincidence.
        val proposal = useCase.propose(candidate, TaskEstimate(minutes = 60))

        assertThat(proposal.noModelAnswer).isFalse()
        assertThat(proposal.selected).isTrue()
    }

    @Test
    fun `the server's flat ten-and-thirty is no longer special-cased, because it cannot arrive`() {
        // `functions/src/index.ts` USED to return 10 points / 30 minutes when the call
        // reached the function but GROQ did not answer, and the client pattern-matched
        // that pair. It no longer needs to: whatever the transport does, a duration the
        // model did not produce arrives as null. A genuine thirty-minute answer is
        // therefore believed. (`#55` went one further and deleted the fabricated pair at
        // the source — the function now returns a difficulty and no minutes at all.)
        val proposal = useCase.propose(candidate, TaskEstimate(minutes = 30))

        assertThat(proposal.noModelAnswer).isFalse()
        assertThat(proposal.selected).isTrue()
    }

    @Test
    fun `no answer at all keeps the duration the chart already infers`() {
        val proposal = useCase.propose(candidate, estimate = null)

        assertThat(proposal.noModelAnswer).isTrue()
        assertThat(proposal.selected).isFalse()
        assertThat(proposal.proposedMinutes).isEqualTo(candidate.inferredMinutes)
        assertThat(proposal.changesTheChart).isFalse()
    }

    @Test
    fun `an out-of-range answer is clamped rather than written raw`() {
        val proposal = useCase.propose(candidate, TaskEstimate(minutes = 5_000))

        assertThat(proposal.proposedMinutes).isEqualTo(TaskDuration.MAX_MINUTES)
        assertThat(proposal.noModelAnswer).isFalse()
    }

    // ── §1.4: a hand-typed duration is sticky ───────────────────────

    @Test
    fun `a hand-typed duration is never offered for re-estimation`() {
        val typed = task(
            id = "typed",
            minutes = 45,
            done = true,
            completedAt = 5_000L,
            durationSource = DurationSource.USER,
        )

        assertThat(useCase(listOf(typed), window)).isEmpty()
    }

    @Test
    fun `a hand-typed duration is still excluded when the filter is asked for everything`() {
        // The structural half of §3.3 A: such a task is not in `tasks[]` AT ALL, so
        // raising the cap must not reach it either. Without the explicit provenance
        // filter this passes for the wrong reason — a typed value implies a stored
        // one — and would start failing silently the day the candidate set widens.
        val typed = task(
            id = "typed",
            minutes = 45,
            done = true,
            completedAt = 5_000L,
            durationSource = DurationSource.USER,
        )

        assertThat(useCase(listOf(typed), window, limit = Int.MAX_VALUE)).isEmpty()
    }

    @Test
    fun `the other direction - an untyped task with no duration IS still re-estimated`() {
        // The direction the edit breaks if it is written as "skip everything with a
        // source". A legacy row reads UNKNOWN, which is not USER, and re-estimating
        // it is the entire point of the backfill feature.
        val untyped = task(
            id = "legacy",
            minutes = null,
            done = true,
            completedAt = 5_000L,
            durationSource = DurationSource.UNKNOWN,
        )

        val candidates = useCase(listOf(untyped), window)

        assertThat(candidates.map { it.taskId }).containsExactly("legacy")
    }

    @Test
    fun `a task the AI already estimated is left alone too, having a duration already`() {
        val estimated = task(
            id = "ai",
            minutes = 45,
            done = true,
            completedAt = 5_000L,
            durationSource = DurationSource.AI,
        )

        assertThat(useCase(listOf(estimated), window)).isEmpty()
    }

    @Test
    fun `a nonsense answer of zero minutes falls back instead of erasing the task`() {
        val proposal = useCase.propose(candidate, TaskEstimate(minutes = 0))

        assertThat(proposal.proposedMinutes).isEqualTo(candidate.inferredMinutes)
        assertThat(proposal.selected).isFalse()
    }
}
