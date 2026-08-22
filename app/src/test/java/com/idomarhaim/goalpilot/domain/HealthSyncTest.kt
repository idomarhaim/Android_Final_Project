package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DailySteps
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.HealthSnapshot
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.SleepNight
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildHealthProposalsUseCase
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncOutcome
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncTrigger
import com.idomarhaim.goalpilot.domain.usecase.SyncHealthDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The automatic Health Connect sync: when it is allowed to run, and what it writes
 * when it does.
 *
 * This is the layer that used to be a human pressing "Log" in a review sheet. With
 * that sheet gone, these tests are the only thing standing between a step count and
 * a double-counted goal, so they lean on the awkward cases — a day that grew since
 * the last sync, a lookup that never comes back, two triggers at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthSyncTest {

    private val health = mockk<HealthRepository>(relaxed = true)
    private val goals = mockk<GoalRepository>(relaxed = true)
    private val progress = mockk<ProgressRepository>(relaxed = true)
    private val auth = mockk<AuthRepository>(relaxed = true)
    private val preferences = FakePreferences()

    private val day = LocalDate.of(2026, 8, 5).toEpochDay()
    private val stepsKey = BuildHealthProposalsUseCase.sourceKey(HealthMetric.STEPS, day)
    private val now = 1_754_000_000_000L

    private val useCase = SyncHealthDataUseCase(
        healthRepository = health,
        goalRepository = goals,
        progressRepository = progress,
        authRepository = auth,
        preferences = preferences,
        buildProposals = BuildHealthProposalsUseCase(),
    )

    /** Signed in, Health Connect ready, one goal, and [steps] walked today. */
    private fun ready(
        steps: Long = 8_000,
        sleep: SleepNight? = null,
        existingGoals: List<Goal> = listOf(
            Goal(id = "g-fit", title = "Move more", category = GoalCategory.FITNESS, measure = Measure(MeasureKind.COUNT, "steps")),
        ),
        logged: List<ProgressEntry> = emptyList(),
    ) {
        every { auth.currentUid() } returns "u1"
        coEvery { health.availability() } returns HealthAvailability.AVAILABLE
        coEvery { health.readSnapshot(any()) } returns Resource.Success(
            HealthSnapshot(
                steps = if (steps > 0) listOf(DailySteps(day, steps)) else emptyList(),
                sleep = listOfNotNull(sleep),
            ),
        )
        every { goals.observeGoals(any()) } returns flowOf(existingGoals)
        every { progress.observeEntries(any()) } returns flowOf(logged)
        coEvery { progress.logProgress(any(), any()) } returns Resource.Success("p1")
        coEvery { goals.upsertGoal(any()) } returns Resource.Success("g-new")
    }

    private fun entry(sourceKey: String, value: Double) =
        ProgressEntry(goalId = "g-fit", value = value, sourceKey = sourceKey)

    // ── The throttle ──────────────────────────────────────────────────

    @Test
    fun `a foreground sync inside the fifteen-minute window does not read at all`() = runTest {
        ready()
        preferences.setHealthLastSyncAt("u1", now - 60_000)

        val outcome = useCase(HealthSyncTrigger.APP_FOREGROUND, now)

        assertThat(outcome).isEqualTo(HealthSyncOutcome.Throttled)
        coVerify(exactly = 0) { health.readSnapshot(any()) }
    }

    @Test
    fun `a foreground sync once the window has passed writes`() = runTest {
        ready()
        preferences.setHealthLastSyncAt("u1", now - 16 * 60_000)

        val outcome = useCase(HealthSyncTrigger.APP_FOREGROUND, now)

        assertThat(outcome).isInstanceOf(HealthSyncOutcome.Logged::class.java)
        coVerify(exactly = 1) { progress.logProgress(any(), any()) }
    }

    @Test
    fun `the first sync on a device is never throttled`() = runTest {
        ready()

        assertThat(useCase(HealthSyncTrigger.APP_FOREGROUND, now))
            .isInstanceOf(HealthSyncOutcome.Logged::class.java)
    }

    @Test
    fun `a manual sync ignores the throttle because the user asked`() = runTest {
        ready()
        preferences.setHealthLastSyncAt("u1", now - 1_000)

        assertThat(useCase(HealthSyncTrigger.MANUAL, now))
            .isInstanceOf(HealthSyncOutcome.Logged::class.java)
    }

    @Test
    fun `a clock that jumped backwards does not park the next sync in the future`() = runTest {
        // A timezone change or an NTP correction can leave a "last sync" stamp
        // ahead of now; treating that as fresh would freeze the feature.
        ready()
        preferences.setHealthLastSyncAt("u1", now + 60 * 60_000)

        assertThat(useCase(HealthSyncTrigger.APP_FOREGROUND, now))
            .isInstanceOf(HealthSyncOutcome.Logged::class.java)
    }

    @Test
    fun `an empty Health Connect still starts the throttle window`() = runTest {
        // Otherwise a device whose store is empty re-reads on every foreground.
        ready(steps = 0)

        val outcome = useCase(HealthSyncTrigger.APP_FOREGROUND, now)

        assertThat(outcome).isEqualTo(HealthSyncOutcome.UpToDate)
        assertThat(preferences.healthLastSyncAt("u1")).isEqualTo(now)
    }

    @Test
    fun `the throttle is per account, so a new sign-in does not inherit the wait`() = runTest {
        ready()
        preferences.setHealthLastSyncAt("someone-else", now - 1_000)

        assertThat(useCase(HealthSyncTrigger.APP_FOREGROUND, now))
            .isInstanceOf(HealthSyncOutcome.Logged::class.java)
    }

    // ── What gets written ─────────────────────────────────────────────

    @Test
    fun `a day already logged in full is not written again`() = runTest {
        ready(steps = 8_000, logged = listOf(entry(stepsKey, 8_000.0)))

        val outcome = useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(outcome).isEqualTo(HealthSyncOutcome.UpToDate)
        coVerify(exactly = 0) { progress.logProgress(any(), any()) }
    }

    @Test
    fun `a day that grew is topped up by the difference only`() = runTest {
        ready(steps = 11_000, logged = listOf(entry(stepsKey, 2_000.0)))
        val written = slot<ProgressEntry>()
        coEvery { progress.logProgress(capture(written), any()) } returns Resource.Success("p1")

        val outcome = useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(written.captured.value).isWithin(0.0001).of(9_000.0)
        assertThat(written.captured.sourceKey).isEqualTo(stepsKey)
        assertThat((outcome as HealthSyncOutcome.Logged).topUps).isEqualTo(1)
    }

    @Test
    fun `several top-ups of one day sum to that day's total, not to three of them`() = runTest {
        // Three entries under one source key is the intended shape — their sum is
        // what the day has been credited, which is exactly what the next sync reads.
        ready(
            steps = 11_000,
            logged = listOf(
                entry(stepsKey, 2_000.0),
                entry(stepsKey, 3_000.0),
                entry(stepsKey, 4_000.0),
            ),
        )
        val written = slot<ProgressEntry>()
        coEvery { progress.logProgress(capture(written), any()) } returns Resource.Success("p1")

        useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(written.captured.value).isWithin(0.0001).of(2_000.0)
    }

    @Test
    fun `a missing goal is created once and reused for the same metric`() = runTest {
        ready(
            steps = 8_000,
            existingGoals = emptyList(),
            sleep = SleepNight(day, 450),
        )

        val outcome = useCase(HealthSyncTrigger.MANUAL, now) as HealthSyncOutcome.Logged

        // Two readings, two metrics, two goals — not one goal per reading.
        assertThat(outcome.entries).isEqualTo(2)
        assertThat(outcome.createdGoals).isEqualTo(2)
        coVerify(exactly = 2) { goals.upsertGoal(any()) }
    }

    // ── #47: pinning, so a category edit cannot orphan a goal ─────────

    @Test
    fun `a goal the sync creates is pinned to its metric at birth`() = runTest {
        ready(steps = 8_000, existingGoals = emptyList())
        val created = slot<Goal>()
        coEvery { goals.upsertGoal(capture(created)) } returns Resource.Success("g-new")

        useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(created.captured.healthSourceKey).isEqualTo(HealthMetric.STEPS.goalSourceKey)
    }

    @Test
    fun `a goal matched by the old heuristic is stamped, so it is matched by key next time`() =
        runTest {
            // g-fit is the user's own goal: right category, right unit, no key. The
            // sync logs into it and pins it on the way past, which is what makes the
            // heuristic a one-time path rather than a standing exposure.
            ready(steps = 8_000)
            val stamped = slot<Goal>()
            coEvery { goals.upsertGoal(capture(stamped)) } returns Resource.Success("g-fit")

            useCase(HealthSyncTrigger.MANUAL, now)

            assertThat(stamped.captured.id).isEqualTo("g-fit")
            assertThat(stamped.captured.healthSourceKey).isEqualTo(HealthMetric.STEPS.goalSourceKey)
        }

    @Test
    fun `an already-pinned goal is not stamped again`() = runTest {
        ready(
            steps = 8_000,
            existingGoals = listOf(
                Goal(
                    id = "g-fit",
                    title = "Move more",
                    category = GoalCategory.FITNESS,
                    measure = Measure(MeasureKind.COUNT, "steps"),
                    healthSourceKey = HealthMetric.STEPS.goalSourceKey,
                ),
            ),
        )

        useCase(HealthSyncTrigger.MANUAL, now)

        coVerify(exactly = 0) { goals.upsertGoal(any()) }
    }

    @Test
    fun `manual entries the user typed are left out of the dedupe`() = runTest {
        // A hand-logged entry carries no source key, so it must not be mistaken for
        // a Health Connect write and suppress today's reading.
        ready(steps = 8_000, logged = listOf(ProgressEntry(goalId = "g-fit", value = 5_000.0)))

        assertThat(useCase(HealthSyncTrigger.MANUAL, now))
            .isInstanceOf(HealthSyncOutcome.Logged::class.java)
    }

    // ── Refusing to guess ─────────────────────────────────────────────

    @Test
    fun `a goals lookup that never returns aborts instead of creating a duplicate goal`() =
        runTest {
            ready()
            // Never emits — a Firestore snapshot flow on a cold network, not an
            // empty result (which is a perfectly good answer and arrives promptly).
            every { goals.observeGoals(any()) } returns MutableSharedFlow()

            val outcome = useCase(HealthSyncTrigger.MANUAL, now)

            assertThat(outcome).isInstanceOf(HealthSyncOutcome.Failed::class.java)
            coVerify(exactly = 0) { goals.upsertGoal(any()) }
            coVerify(exactly = 0) { progress.logProgress(any(), any()) }
        }

    @Test
    fun `a dedupe lookup that never returns aborts instead of re-logging the week`() = runTest {
        ready()
        every { progress.observeEntries(any()) } returns MutableSharedFlow()

        val outcome = useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(outcome).isInstanceOf(HealthSyncOutcome.Failed::class.java)
        coVerify(exactly = 0) { progress.logProgress(any(), any()) }
    }

    @Test
    fun `missing permissions never write and never prompt on their own`() = runTest {
        ready()
        coEvery { health.availability() } returns HealthAvailability.PERMISSIONS_REQUIRED

        val outcome = useCase(HealthSyncTrigger.APP_FOREGROUND, now)

        assertThat(outcome).isEqualTo(HealthSyncOutcome.PermissionsRequired)
        coVerify(exactly = 0) { health.readSnapshot(any()) }
        // The window is not stamped, so granting permission and coming back syncs
        // straight away rather than waiting out a window nothing happened in.
        assertThat(preferences.healthLastSyncAt("u1")).isEqualTo(0L)
    }

    @Test
    fun `a device without Health Connect reports why and writes nothing`() = runTest {
        ready()
        coEvery { health.availability() } returns HealthAvailability.NOT_SUPPORTED

        val outcome = useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(outcome).isEqualTo(
            HealthSyncOutcome.Unavailable(HealthAvailability.NOT_SUPPORTED),
        )
        coVerify(exactly = 0) { progress.logProgress(any(), any()) }
    }

    @Test
    fun `a signed-out session syncs nothing`() = runTest {
        ready()
        every { auth.currentUid() } returns null

        assertThat(useCase(HealthSyncTrigger.MANUAL, now))
            .isEqualTo(HealthSyncOutcome.NotSignedIn)
        coVerify(exactly = 0) { health.availability() }
    }

    @Test
    fun `the last-sync stamp is what the card reads back`() = runTest {
        ready()

        useCase(HealthSyncTrigger.MANUAL, now)

        assertThat(useCase.status.value.lastSyncAtMillis).isEqualTo(now)
        assertThat(useCase.status.value.isSyncing).isFalse()
    }

    /** In-memory stand-in for the SharedPreferences-backed implementation. */
    private class FakePreferences : AppPreferencesRepository {
        private val stamps = mutableMapOf<String, Long>()
        override val skin = MutableStateFlow(AppSkin.AURORA)
        override fun setSkin(skin: AppSkin) { this.skin.value = skin }
        override val language = MutableStateFlow(AppLanguage.DEFAULT)
        override fun setLanguage(language: AppLanguage) { this.language.value = language }
        override val brightness = MutableStateFlow(AppBrightness.DEFAULT)
        override fun setBrightness(brightness: AppBrightness) {
            this.brightness.value = brightness
        }
        override val material = MutableStateFlow(AppMaterial.DEFAULT)
        override fun setMaterial(material: AppMaterial) { this.material.value = material }
        // #57 b. Unread by anything this suite exercises; here because the interface has it.
        override val background = MutableStateFlow(AppBackground.DEFAULT)
        override fun setBackground(background: AppBackground) {
            this.background.value = background
        }
        // #57 c. Same: unread here, present because the interface has it.
        override val relief = MutableStateFlow(AppRelief.DEFAULT)
        override fun setRelief(relief: AppRelief) { this.relief.value = relief }
        override val region = MutableStateFlow(AppRegion.DEFAULT)
        override fun setRegion(region: AppRegion) { this.region.value = region }
        override val daySchedule = MutableStateFlow(DaySchedule.DEFAULT)
        override fun setWakingHours(wakingHours: WakingHours) {
            daySchedule.value = daySchedule.value.copy(waking = wakingHours)
        }
        override fun setPlanningOverrideMinutes(minutes: Int?) {
            daySchedule.value = daySchedule.value.copy(planningOverrideMinutes = minutes)
        }
        override fun healthLastSyncAt(uid: String): Long = stamps[uid] ?: 0L
        override fun setHealthLastSyncAt(uid: String, epochMillis: Long) {
            stamps[uid] = epochMillis
        }
        // #56. Unread by anything this suite exercises; here because the interface has it.
        private var missReviewShownAt: Long = 0L
        override fun missReviewLastShownAt(): Long = missReviewShownAt
        override fun setMissReviewLastShownAt(epochMillis: Long) {
            missReviewShownAt = epochMillis
        }
    }
}
