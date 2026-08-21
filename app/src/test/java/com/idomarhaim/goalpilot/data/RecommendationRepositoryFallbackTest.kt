package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.google.firebase.functions.FirebaseFunctions
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.remote.RecommendationRepositoryImpl
import com.idomarhaim.goalpilot.data.security.AiCredentialStore
import com.idomarhaim.goalpilot.data.security.DefaultAiProviderRepository
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationRepositoryFallbackTest {

    private val functions = mockk<FirebaseFunctions>()

    /**
     * `C13` (#54) gave this repository a second dependency. A real
     * [DefaultAiProviderRepository] over an empty in-memory store rather than a
     * mock, because that is the state every install is in by default — **no key
     * set** — and it is the state these fallback tests are about: spec §8's
     * local guidance must be reached identically whether or not `C13` exists.
     */
    private val emptyStore = object : AiCredentialStore {
        override fun read(): AiCredential? = null
        override fun write(credential: AiCredential) = Unit
        override fun clear() = Unit
    }
    private val aiProvider = DefaultAiProviderRepository(emptyStore)
    private val repo =
        RecommendationRepositoryImpl(functions, aiProvider, UnconfinedTestDispatcher())

    private val goals = listOf(
        Goal(id = "g1", title = "Run 5k", currentValue = 10.0, targetValue = 100.0),
        Goal(id = "g2", title = "Read books", currentValue = 90.0, targetValue = 100.0),
    )

    private val areas = listOf(
        LifeArea(id = "study", name = "Studies"),
        LifeArea(id = "a-studio", name = "Studio"),
    )

    @Test
    fun `getRecommendations falls back to local guidance when the function fails`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        val result = repo.getRecommendations(goals, completedTasksLast7d = 3, totalPoints = 120)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val recommendations = (result as Resource.Success).data
        assertThat(recommendations).isNotEmpty()
        // The low-progress goal should be surfaced as a nudge.
        assertThat(recommendations.any { it.relatedGoalId == "g1" }).isTrue()
    }

    @Test
    fun `classifyTask falls back to a keyword match when the function fails`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        val result = repo.classifyTask("Read a book tonight", goals)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val classification = (result as Resource.Success).data
        // "Read"/"book" (>3 chars) matches goal g2 ("Read books").
        assertThat(classification.suggestedGoalId).isEqualTo("g2")
        // #9, spec §3.4: no model spoke, so there is NO duration to report. It used
        // to arrive as `fallbackMinutes(10)` — a number invented from a point score
        // that is itself derived from the title's word count. The task still lands in
        // the time chart, via `TaskDuration.minutesOf`, which is the chart's own
        // business and is not written to the document as an estimate.
        assertThat(classification.estimatedMinutes).isNull()
    }

    @Test
    fun `classifyTask offline inherits the matched goal's life area`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        val filed = goals.map {
            if (it.id == "g2") it.copy(lifeAreaIds = listOf("study", "health")) else it
        }
        val result = repo.classifyTask("Read a book tonight", filed, areas)

        // The classification carries one suggestion, so a goal serving several
        // areas offers its first — never a silent pick from the middle.
        assertThat((result as Resource.Success).data.suggestedLifeAreaId).isEqualTo("study")
    }

    @Test
    fun `classifyTask offline can still match a life area by name`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        // Nothing in `goals` matches, so the new-goal branch runs; the area name does.
        val result = repo.classifyTask("Fix the studio lighting", goals, areas)

        val classification = (result as Resource.Success).data
        assertThat(classification.suggestedGoalId).isNull()
        assertThat(classification.suggestedLifeAreaId).isEqualTo("a-studio")
    }

    @Test
    fun `scoreTask offline reports no judgement and no duration`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        val result = repo.scoreTask("Run five kilometres before work")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val estimate = (result as Resource.Success).data
        // `#55`. This used to assert 20 — `5 + 3×words`, the offline point heuristic — and
        // §1.4 retired it outright: a title's word count is not evidence about the work.
        // ROUTINE is ×1.0, so what comes back here is the ABSENCE of a judgement rather
        // than a guess at one, and the task is priced on whatever minutes the user gives.
        assertThat(estimate.difficulty).isEqualTo(Difficulty.ROUTINE)
        // The duration is absent for the same reason it has been since #9: nobody spoke.
        // The box asks, and a skipped answer is stored as DurationSource.UNKNOWN (§3.4).
        assertThat(estimate.minutes).isNull()
    }

    @Test
    fun `the offline answer does not vary with the title, because it is not an estimate`() =
        runTest {
            every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

            val long = repo.scoreTask(List(40) { "word" }.joinToString(" "))
            val short = repo.scoreTask("Stretch")

            // The old heuristic gave these 50 and 8 — two different "estimates" of work it
            // had no information about. That divergence WAS the defect: it made a fabricated
            // number look like a considered one. Now they are identical, and identical is
            // what "nothing was measured" should look like.
            assertThat((long as Resource.Success).data.difficulty).isEqualTo(Difficulty.ROUTINE)
            assertThat((short as Resource.Success).data.difficulty).isEqualTo(Difficulty.ROUTINE)
            assertThat(long.data.minutes).isNull()
            assertThat((short as Resource.Success).data.minutes).isNull()
        }

    @Test
    fun `a call that never reaches a provider is reported as the local rung`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        repo.scoreTask("write the report")

        // #54 piece 3: the status line must not be able to say "the free model
        // answered" about a call no model saw. This is the client-side half of
        // that — the Cloud Function's `answeredBy: "none"` is the other one.
        assertThat(aiProvider.lastAnswer.value).isEqualTo(AiAnswer.Local())
    }
}
