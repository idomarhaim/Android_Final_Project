package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.DailySteps
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthSnapshot
import com.idomarhaim.goalpilot.domain.model.SleepNight
import com.idomarhaim.goalpilot.domain.usecase.BuildHealthProposalsUseCase
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import com.idomarhaim.goalpilot.domain.usecase.noteText
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * The filing rules for a Health Connect sync: which goal a reading lands in, and
 * which readings are skipped because they are already there.
 *
 * All of it is pure, so none of this needs an emulator, a provider app or a
 * granted permission — which is the entire reason the logic lives in `domain/`.
 */
class HealthProposalsTest {

    private val build = BuildHealthProposalsUseCase()

    // The note text is formatted for the user's locale, so the thousands separator
    // in "8,432 steps" is whatever this machine happens to use. Pin it, or the
    // assertions below pass in Tel Aviv and fail in Berlin.
    private val originalLocale: Locale = Locale.getDefault()

    @Before
    fun pinLocale() = Locale.setDefault(Locale.US)

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    private val day1 = LocalDate.of(2026, 8, 1).toEpochDay()
    private val day2 = LocalDate.of(2026, 8, 2).toEpochDay()

    private fun goal(
        id: String,
        title: String,
        category: GoalCategory,
        unit: String = "%",
        archived: Boolean = false,
    ) = Goal(id = id, title = title, category = category, unit = unit, isArchived = archived)

    @Test
    fun `steps go to a fitness goal and sleep to a sleep goal`() {
        val goals = listOf(
            goal("g-fit", "Move more", GoalCategory.FITNESS),
            goal("g-sleep", "Rest properly", GoalCategory.SLEEP),
        )
        val snapshot = HealthSnapshot(
            steps = listOf(DailySteps(day1, 8_000)),
            sleep = listOf(SleepNight(day1, 450)),
        )

        val proposals = build(snapshot, goals)

        assertThat(proposals).hasSize(2)
        assertThat(proposals.single { it.metric == HealthMetric.STEPS }.targetGoalId)
            .isEqualTo("g-fit")
        assertThat(proposals.single { it.metric == HealthMetric.SLEEP }.targetGoalId)
            .isEqualTo("g-sleep")
    }

    @Test
    fun `a goal whose unit already matches wins over one that merely shares the category`() {
        // Logging 8,000 steps into a goal measured in "workouts" would move it from
        // 2 workouts to its target in a single sync.
        val goals = listOf(
            goal("g-workouts", "Gym sessions", GoalCategory.FITNESS, unit = "workouts"),
            goal("g-steps", "Step count", GoalCategory.FITNESS, unit = "steps"),
        )
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 8_000)))

        val proposals = build(snapshot, goals)

        assertThat(proposals.single().targetGoalId).isEqualTo("g-steps")
    }

    @Test
    fun `archived goals are never proposed`() {
        val goals = listOf(goal("g-old", "Old fitness goal", GoalCategory.FITNESS, archived = true))
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 8_000)))

        val proposals = build(snapshot, goals)

        assertThat(proposals.single().targetGoalId).isNull()
        assertThat(proposals.single().newGoalTitle).isEqualTo(HealthMetric.STEPS.defaultGoalTitle)
    }

    @Test
    fun `with no suitable goal a new one is proposed instead`() {
        val snapshot = HealthSnapshot(
            steps = listOf(DailySteps(day1, 8_000)),
            sleep = listOf(SleepNight(day1, 450)),
        )

        val proposals = build(snapshot, goals = emptyList())

        assertThat(proposals.map { it.newGoalTitle })
            .containsExactly(
                HealthMetric.STEPS.defaultGoalTitle,
                HealthMetric.SLEEP.defaultGoalTitle,
            )
        assertThat(proposals.all { it.targetGoalId == null }).isTrue()
    }

    @Test
    fun `readings already logged in full are dropped so a re-sync cannot double count`() {
        val goals = listOf(goal("g-fit", "Move more", GoalCategory.FITNESS, unit = "steps"))
        val snapshot = HealthSnapshot(
            steps = listOf(DailySteps(day1, 8_000), DailySteps(day2, 9_000)),
        )
        val alreadyLogged = mapOf(
            BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day1) to 8_000.0,
        )

        val proposals = build(snapshot, goals, alreadyLogged)

        assertThat(proposals).hasSize(1)
        assertThat(proposals.single().epochDay).isEqualTo(day2)
    }

    @Test
    fun `a day that has grown since it was logged is topped up by the difference`() {
        // The whole point of syncing on every app entry: today at 09:00 is not
        // today at 18:00, and freezing it at the first reading loses the walk.
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 11_000)))
        val alreadyLogged = mapOf(
            BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day1) to 2_000.0,
        )

        val proposal = build(snapshot, goals = emptyList(), alreadyLogged = alreadyLogged).single()

        assertThat(proposal.value).isWithin(0.0001).of(9_000.0)
        assertThat(proposal.total).isWithin(0.0001).of(11_000.0)
        assertThat(proposal.isTopUp).isTrue()
    }

    @Test
    fun `a day that has shrunk is skipped rather than logged as a negative`() {
        // Health Connect can revise a day down when a duplicate record is removed.
        // Subtracting from a goal the user has been watching climb is worse than
        // leaving it slightly high.
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 6_000)))
        val alreadyLogged = mapOf(
            BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day1) to 8_000.0,
        )

        assertThat(build(snapshot, goals = emptyList(), alreadyLogged)).isEmpty()
    }

    @Test
    fun `a trivial change is not worth an entry of its own`() {
        // 7h29m recomputed as 7h30m: a 0.1-hour row in the goal's history is noise,
        // and at one sync per app entry there would be a lot of it.
        val snapshot = HealthSnapshot(sleep = listOf(SleepNight(day1, 452)))
        val alreadyLogged = mapOf(
            BuildHealthProposalsUseCase.sourceKey(HealthMetric.SLEEP, day1) to 7.5,
        )

        assertThat(build(snapshot, goals = emptyList(), alreadyLogged)).isEmpty()
    }

    @Test
    fun `a first sync is not a top-up and its note says so`() {
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 8_432)))

        val proposal = build(snapshot, goals = emptyList()).single()

        assertThat(proposal.isTopUp).isFalse()
        assertThat(proposal.alreadyLogged).isEqualTo(0.0)
        assertThat(proposal.noteText()).contains("8,432 steps")
        assertThat(proposal.noteText()).doesNotContain("total")
    }

    @Test
    fun `a top-up note names both the delta and the day's running total`() {
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 11_000)))
        val alreadyLogged = mapOf(
            BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day1) to 2_000.0,
        )

        val note = build(snapshot, goals = emptyList(), alreadyLogged).single().noteText()

        assertThat(note).contains("+9,000 steps")
        assertThat(note).contains("11,000 total")
    }

    @Test
    fun `days with no activity are not proposed`() {
        val snapshot = HealthSnapshot(
            steps = listOf(DailySteps(day1, 0)),
            sleep = listOf(SleepNight(day1, 0)),
        )

        assertThat(build(snapshot, goals = emptyList())).isEmpty()
    }

    @Test
    fun `sleep is proposed in hours rounded to one decimal`() {
        // 7h 29m — an unrounded 7.483333… would land in the goal's running total.
        val snapshot = HealthSnapshot(sleep = listOf(SleepNight(day1, 449)))

        val proposal = build(snapshot, goals = emptyList()).single()

        assertThat(proposal.value).isWithin(0.0001).of(7.5)
    }

    @Test
    fun `steps are proposed as their raw count`() {
        val snapshot = HealthSnapshot(steps = listOf(DailySteps(day1, 8_432)))

        assertThat(build(snapshot, goals = emptyList()).single().value)
            .isWithin(0.0001).of(8_432.0)
    }

    @Test
    fun `source keys are stable, unique per metric-day, and human readable`() {
        val stepsDay1 = BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day1)

        assertThat(stepsDay1).isEqualTo("hc:steps:2026-08-01")
        assertThat(BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day1))
            .isEqualTo(stepsDay1)
        assertThat(BuildHealthProposalsUseCase.sourceKey(HealthMetric.SLEEP, day1))
            .isNotEqualTo(stepsDay1)
        assertThat(BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day2))
            .isNotEqualTo(stepsDay1)
    }

    @Test
    fun `proposals come back newest first`() {
        val snapshot = HealthSnapshot(
            steps = listOf(DailySteps(day1, 8_000), DailySteps(day2, 9_000)),
        )

        val proposals = build(snapshot, goals = emptyList())

        assertThat(proposals.map { it.epochDay }).containsExactly(day2, day1).inOrder()
    }

}
