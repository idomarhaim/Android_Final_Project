package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskEstimate
import com.idomarhaim.goalpilot.domain.model.TaskScoring
import com.idomarhaim.goalpilot.domain.usecase.BackfillDurationsUseCase
import com.idomarhaim.goalpilot.domain.usecase.DurationCandidate
import org.junit.Test

/**
 * Which tasks the AI re-estimation spends its per-run budget on, and which of its
 * answers are worth writing.
 *
 * The second half is the one that matters: every AI path in this app falls back
 * silently, so a proposal that is really the offline heuristic has to be
 * recognised before it is written as an AI estimate — the analytics card counts
 * those, and the count is the whole point of the feature.
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
        points: Int = 10,
    ) = Task(
        id = id,
        title = title,
        points = points,
        isDone = done,
        estimatedMinutes = minutes,
        createdAtEpochMillis = createdAt,
        completedAtEpochMillis = completedAt,
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
        val candidates = useCase(listOf(task("t", points = 20)), window)

        // 20 points × 3 = 60 minutes, exactly what TaskDuration.minutesOf would give.
        assertThat(candidates.single().inferredMinutes)
            .isEqualTo(TaskDuration.fallbackMinutes(20))
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
        // Five words: the offline heuristic scores this 5 + 5×3 = 20 points → 60 min.
        title = "Run five kilometres before work",
        inferredMinutes = 30,
        inWindow = true,
    )

    @Test
    fun `a real answer is selected and shown as a change`() {
        val proposal = useCase.propose(candidate, TaskEstimate(points = 20, minutes = 75))

        assertThat(proposal.proposedMinutes).isEqualTo(75)
        assertThat(proposal.isFallback).isFalse()
        assertThat(proposal.selected).isTrue()
        assertThat(proposal.changesTheChart).isTrue()
    }

    @Test
    fun `an answer identical to the client's offline heuristic arrives unticked`() {
        // Exactly what the repository produces with no network at all.
        val offline = TaskEstimate(points = 20, minutes = 60)

        val proposal = useCase.propose(candidate, offline)

        assertThat(proposal.isFallback).isTrue()
        assertThat(proposal.selected).isFalse()
    }

    @Test
    fun `the Cloud Function's own fallback is caught too, though no word count makes it`() {
        // `functions/src/index.ts` returns a flat 10 points / 30 minutes when the
        // call reached the function but GROQ did not answer. The client heuristic
        // is 5 + 3×words, which is never 10 — so a check that knew only the client
        // rule would write this through as a genuine AI estimate.
        val serverFallback = TaskEstimate(points = 10, minutes = 30)
        assertThat(TaskScoring.heuristicPoints(candidate.title)).isNotEqualTo(10)

        val proposal = useCase.propose(candidate, serverFallback)

        assertThat(proposal.isFallback).isTrue()
        assertThat(proposal.selected).isFalse()
    }

    @Test
    fun `no answer at all keeps the duration the chart already infers`() {
        val proposal = useCase.propose(candidate, estimate = null)

        assertThat(proposal.isFallback).isTrue()
        assertThat(proposal.selected).isFalse()
        assertThat(proposal.proposedMinutes).isEqualTo(candidate.inferredMinutes)
        assertThat(proposal.changesTheChart).isFalse()
    }

    @Test
    fun `an out-of-range answer is clamped rather than written raw`() {
        val proposal = useCase.propose(candidate, TaskEstimate(points = 20, minutes = 5_000))

        assertThat(proposal.proposedMinutes).isEqualTo(TaskDuration.MAX_MINUTES)
        assertThat(proposal.isFallback).isFalse()
    }

    @Test
    fun `a nonsense answer of zero minutes falls back instead of erasing the task`() {
        val proposal = useCase.propose(candidate, TaskEstimate(points = 20, minutes = 0))

        assertThat(proposal.proposedMinutes).isEqualTo(candidate.inferredMinutes)
        assertThat(proposal.selected).isFalse()
    }
}
