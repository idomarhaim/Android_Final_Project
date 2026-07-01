package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.google.firebase.functions.FirebaseFunctions
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.remote.RecommendationRepositoryImpl
import com.idomarhaim.goalpilot.domain.model.Goal
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
    }
}
