package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Why a sync is running, which decides whether the 15-minute throttle applies. */
enum class HealthSyncTrigger {
    /** The app came to the foreground. Throttled. */
    APP_FOREGROUND,

    /** The user pressed "Sync now". Never throttled — they asked. */
    MANUAL,
}

/** What a sync attempt did. Facts only; the wording lives in the UI layer. */
sealed interface HealthSyncOutcome {
    /** Readings were written. [topUps] of them extended a day that was already logged. */
    data class Logged(val entries: Int, val topUps: Int, val createdGoals: Int) : HealthSyncOutcome

    /** Health Connect was read successfully and had nothing new to add. */
    data object UpToDate : HealthSyncOutcome

    /** Less than the throttle window has passed since the last successful read. */
    data object Throttled : HealthSyncOutcome

    /** Another sync is mid-flight; this one stood down rather than double-writing. */
    data object AlreadyRunning : HealthSyncOutcome

    data object NotSignedIn : HealthSyncOutcome

    /**
     * Permission has not been granted. Auto-sync stops here in silence — asking is
     * a system dialog, and one popping up on every app launch is hostile. The
     * dashboard card is where the user grants it.
     */
    data object PermissionsRequired : HealthSyncOutcome

    /** No usable Health Connect on this device. */
    data class Unavailable(val availability: HealthAvailability) : HealthSyncOutcome

    data class Failed(val message: String?) : HealthSyncOutcome
}

/** A finished attempt, paired with what set it off, for anyone observing. */
data class HealthSyncResult(
    val trigger: HealthSyncTrigger,
    val outcome: HealthSyncOutcome,
)

/** Enough for a card to say "Last synced 4 minutes ago" and spin while it works. */
data class HealthSyncStatus(
    val isSyncing: Boolean = false,
    /** Epoch millis of the last successful read, or 0 if this account never synced. */
    val lastSyncAtMillis: Long = 0L,
)

/**
 * Reads Health Connect and writes every reading that is not in Firestore yet.
 *
 * **Nothing is reviewed.** Earlier revisions opened a sheet listing each day with a
 * checkbox; that made sense when a sync was a deliberate act, but the app now syncs
 * whenever it comes forward, and a dialog on every launch is a dialog nobody reads.
 * Correctness therefore rests entirely on the dedupe: every entry carries a
 * [BuildHealthProposalsUseCase.sourceKey], and a day is only ever topped up by the
 * difference between what Health Connect reports and what has already been written
 * for it. That also means the two lookups this needs — the user's goals and their
 * existing entries — are **not optional**: if either times out the sync fails
 * rather than proceeding, because proceeding blind would create a second "Weekly
 * steps" goal or log Tuesday twice, and there is no longer a human in the loop to
 * catch it.
 *
 * A singleton because the throttle and the in-flight guard have to be shared by
 * every caller: the root scaffold triggers it on foreground, the dashboard card on
 * demand, and those two must not race each other into the same write.
 */
@Singleton
class SyncHealthDataUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferencesRepository,
    private val buildProposals: BuildHealthProposalsUseCase,
) {

    private val _status = MutableStateFlow(HealthSyncStatus())
    val status: StateFlow<HealthSyncStatus> = _status.asStateFlow()

    private val _results = MutableSharedFlow<HealthSyncResult>(extraBufferCapacity = 4)

    /**
     * Every finished attempt. Replayless on purpose — a snackbar for a sync that
     * happened while the dashboard was off-screen would arrive out of nowhere
     * minutes later.
     */
    val results: SharedFlow<HealthSyncResult> = _results.asSharedFlow()

    private val inFlight = Mutex()

    suspend operator fun invoke(
        trigger: HealthSyncTrigger,
        nowMillis: Long = System.currentTimeMillis(),
    ): HealthSyncOutcome {
        val outcome = run(trigger, nowMillis)
        _results.tryEmit(HealthSyncResult(trigger, outcome))
        return outcome
    }

    private suspend fun run(trigger: HealthSyncTrigger, nowMillis: Long): HealthSyncOutcome {
        val uid = authRepository.currentUid() ?: return HealthSyncOutcome.NotSignedIn
        val lastSync = preferences.healthLastSyncAt(uid)
        _status.update { it.copy(lastSyncAtMillis = lastSync) }

        // A clock that has gone backwards (timezone edit, NTP correction) would
        // otherwise park the next sync up to fifteen minutes in the future.
        val elapsed = nowMillis - lastSync
        if (trigger == HealthSyncTrigger.APP_FOREGROUND &&
            elapsed in 0 until THROTTLE_MILLIS
        ) {
            return HealthSyncOutcome.Throttled
        }

        if (!inFlight.tryLock()) return HealthSyncOutcome.AlreadyRunning
        _status.update { it.copy(isSyncing = true) }
        return try {
            sync(uid, nowMillis)
        } finally {
            _status.update { it.copy(isSyncing = false) }
            inFlight.unlock()
        }
    }

    private suspend fun sync(uid: String, nowMillis: Long): HealthSyncOutcome {
        when (val availability = healthRepository.availability()) {
            HealthAvailability.AVAILABLE -> Unit
            HealthAvailability.PERMISSIONS_REQUIRED -> return HealthSyncOutcome.PermissionsRequired
            else -> return HealthSyncOutcome.Unavailable(availability)
        }

        val snapshot = when (val read = healthRepository.readSnapshot()) {
            is Resource.Success -> read.data
            is Resource.Error -> return HealthSyncOutcome.Failed(read.message)
            Resource.Loading -> return HealthSyncOutcome.Failed(null)
        }

        // The read succeeded, so the throttle window starts now even if there was
        // nothing to write. Otherwise a device whose Health Connect is empty would
        // re-read on every single foreground, forever.
        preferences.setHealthLastSyncAt(uid, nowMillis)
        _status.update { it.copy(lastSyncAtMillis = nowMillis) }

        if (snapshot.isEmpty) return HealthSyncOutcome.UpToDate

        val goals = withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { goalRepository.observeGoals().first() }
            ?: return HealthSyncOutcome.Failed("Could not read your goals")

        // Two passes: the first decides which goals are involved, the second sees
        // how much those goals have already been credited per day. The filing rules
        // live in the use case either way — this only supplies the facts it cannot
        // read for itself.
        val provisional = buildProposals(snapshot, goals)
        if (provisional.isEmpty()) return HealthSyncOutcome.UpToDate

        val credited = mutableMapOf<String, Double>()
        for (goalId in provisional.mapNotNull { it.targetGoalId }.distinct()) {
            val perGoal = creditedPerSourceKey(goalId)
                ?: return HealthSyncOutcome.Failed("Could not check what is already logged")
            // A day can only be credited to one goal in practice; on the freak
            // overlap (the old goal was archived and a new one took over) take the
            // larger, which under-writes rather than double-counts.
            perGoal.forEach { (key, value) ->
                credited[key] = maxOf(credited[key] ?: 0.0, value)
            }
        }

        val proposals = buildProposals(snapshot, goals, credited)
        if (proposals.isEmpty()) return HealthSyncOutcome.UpToDate

        // Only once there is something to write: a stamp is a document write, and a
        // sync that turns out to owe nothing should cost nothing.
        pinMatchedGoals(goals)

        return write(proposals)
    }

    /**
     * Stamps [Goal.healthSourceKey] on a goal that was matched by the old
     * category-and-unit heuristic, so it can never be matched that way again.
     *
     * This is the repair half of #47. Matching on the category meant that editing a
     * goal's category orphaned it from a sync that runs on every foreground with no
     * human watching, and the next run created a second "Weekly steps" goal. Pinning
     * moves the answer onto an identity the user cannot reach.
     *
     * Runs at most once per goal — a pinned goal is matched by its key and skipped
     * here — and never blocks the sync: a failed stamp just means the heuristic is
     * used again next time, which is exactly today's behaviour.
     */
    private suspend fun pinMatchedGoals(goals: List<Goal>) {
        for (metric in HealthMetric.entries) {
            val matched = buildProposals.match(goals, metric) ?: continue
            if (matched.healthSourceKey != null) continue
            goalRepository.upsertGoal(matched.copy(healthSourceKey = metric.goalSourceKey))
        }
    }

    private suspend fun write(proposals: List<HealthLogProposal>): HealthSyncOutcome {
        // Steps and sleep each need at most one new goal, however many days are
        // being logged — create it once and reuse the id.
        val createdGoals = mutableMapOf<HealthMetric, String>()
        var saved = 0
        var topUps = 0
        for (proposal in proposals) {
            val goalId = proposal.targetGoalId
                ?: createdGoals[proposal.metric]
                ?: run {
                    val created = goalRepository.upsertGoal(
                        Goal(
                            title = proposal.newGoalTitle.orEmpty()
                                .ifBlank { proposal.metric.defaultGoalTitle },
                            category = proposal.metric.category,
                            // Pinned at birth, so this goal is never matched by a
                            // category the user is free to edit (#47).
                            healthSourceKey = proposal.metric.goalSourceKey,
                            unit = proposal.metric.unit,
                            targetValue = proposal.metric.defaultGoalTarget,
                        ),
                    )
                    (created as? Resource.Success)?.data
                        ?.also { createdGoals[proposal.metric] = it }
                }
            if (goalId == null) continue
            val result = progressRepository.logProgress(
                ProgressEntry(
                    goalId = goalId,
                    value = proposal.value,
                    note = proposal.noteText(),
                    sourceKey = proposal.sourceKey,
                ),
                imageUri = null,
            )
            if (result is Resource.Success) {
                saved++
                if (proposal.isTopUp) topUps++
            }
        }
        return if (saved == 0) {
            HealthSyncOutcome.Failed("Could not log those readings")
        } else {
            HealthSyncOutcome.Logged(
                entries = saved,
                topUps = topUps,
                createdGoals = createdGoals.size,
            )
        }
    }

    /**
     * How much each source key has been credited on this goal so far. Null when the
     * lookup did not come back — a Firestore snapshot flow can hang on a cold
     * network, and guessing "nothing is logged yet" would re-log the whole week.
     */
    private suspend fun creditedPerSourceKey(goalId: String): Map<String, Double>? =
        withTimeoutOrNull(LOOKUP_TIMEOUT_MS) {
            progressRepository.observeEntries(goalId).first()
                .mapNotNull { entry -> entry.sourceKey?.let { it to entry.value } }
                .groupingBy { it.first }
                .fold(0.0) { sum, (_, value) -> sum + value }
        }

    companion object {
        /**
         * How long a successful read counts as fresh. Fifteen minutes is short
         * enough that a walk taken during lunch is on the dashboard by the time the
         * user looks, and long enough that flicking between apps does not turn into
         * a Firestore write per switch.
         */
        const val THROTTLE_MILLIS = 15 * 60 * 1000L

        /** Ceiling on each Firestore lookup before the sync gives up unfinished. */
        private const val LOOKUP_TIMEOUT_MS = 5_000L
    }
}
