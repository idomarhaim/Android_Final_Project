package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.google.firebase.functions.FirebaseFunctions
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.remote.RecommendationRepositoryImpl
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
    private val repo = RecommendationRepositoryImpl(functions, UnconfinedTestDispatcher())

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
    fun `scoreTask falls back to a local POINT estimate, and reports no duration`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        val result = repo.scoreTask("Run five kilometres before work")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val estimate = (result as Resource.Success).data
        // 5 words → 5 + 5*3 = 20, inside the 5..50 range the function is prompted for.
        // Points still fall back (spec §8); #9 does not touch scoring.
        assertThat(estimate.points).isEqualTo(20)
        // The duration does NOT. This asserted 60 before #9 — 20 points × 3 — which is
        // the app deriving how long your life took from a word count and storing it as
        // though a model had said so. Absence is what the caller can act on: the box
        // asks, and a skipped answer is stored as DurationSource.UNKNOWN (§3.4).
        assertThat(estimate.minutes).isNull()
    }

    @Test
    fun `scoreTask fallback stays within the 5 to 50 point range`() = runTest {
        every { functions.getHttpsCallable(any()) } throws RuntimeException("no network")

        val long = repo.scoreTask(List(40) { "word" }.joinToString(" "))
        val short = repo.scoreTask("Stretch")

        // 40 words → 5 + 120 clamped to 50; 1 word → 5 + 3 = 8.
        assertThat((long as Resource.Success).data.points).isEqualTo(50)
        assertThat((short as Resource.Success).data.points).isEqualTo(8)
    }
}
