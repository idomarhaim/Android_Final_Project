package com.idomarhaim.goalpilot.feature.sync

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksClient
import com.idomarhaim.goalpilot.data.tasks.TasksImportResult
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.durationSource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.TasksConsent
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncOutcome
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncResult
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncTrigger
import com.idomarhaim.goalpilot.domain.usecase.SyncHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The two **connections to other apps**: Google Tasks import, and the Health
 * Connect sync — spec §5 and §6.
 *
 * ## Why this is its own ViewModel, and why it moved off Home
 *
 * `Observed:` 2026-08-24 — Ido asked for the two sync cards to live in
 * **Settings** rather than on the dashboard. That is a placement decision and it
 * is his, but the reason it was cheap to grant is that the coupling being undone
 * was **accidental in the first place**: this state has nothing to do with the
 * dashboard's. It described *the device's relationship with two other
 * applications*, and it was sitting inside a ViewModel about *today's goals and
 * tasks* only because the cards happened to be drawn on that screen. Roughly 400
 * lines and six of `DashboardViewModel`'s constructor dependencies came here.
 *
 * That is also exactly the cut `SettingsScreen`'s own `ScopeLine` already draws
 * for the user in words — *Profile is the account, Settings is the device*. A
 * Health Connect grant and a Google Tasks scope are facts about **this phone**,
 * not about the account: they survive signing out, and they are already spelt
 * per-device by the operating system. Home is the screen you open to see what you
 * owe today; a card there that says *Synced 4 minutes ago* is answering a
 * question nobody arrived with.
 *
 * ## Nav-entry scoped, and that is now safe
 *
 * `SettingsScreen` is the only consumer, so an ordinary `hiltViewModel()` inside
 * the Settings destination is the whole lifetime this needs. It was **not** safe
 * while the dashboard also drew these cards — two destinations reaching for one
 * `@HiltViewModel` through their own back-stack entries get two instances, each
 * running its own Firestore reads and each holding half the truth about a
 * consent state.
 *
 * ⚠️ **The automatic health sync does not live here and never did.** It is fired
 * from [com.idomarhaim.goalpilot.ui.root.RootViewModel] on
 * [HealthSyncTrigger.APP_FOREGROUND], which is why moving this card off Home
 * changes nothing about whether steps and sleep are logged. What this class owns
 * is the **manual** press and the card's window onto a sync somebody else
 * started.
 *
 * ## No warm caches, unlike the dashboard
 *
 * `DashboardViewModel` keeps `lastGoals` / `lastTasks` / `lastLifeAreas` warm
 * because it classifies on a keystroke and a Firestore round trip inside the
 * smart-add sheet would be felt. An import is a deliberate button press that
 * already waits on Google's servers, so this reads the three collections
 * **once, at the moment of import** ([snapshot]). Fresher, and it removes three
 * long-lived collectors from a screen the user is usually not on.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val googleTasksClient: GoogleTasksClient,
    private val recommendationRepository: RecommendationRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val lifeAreaRepository: LifeAreaRepository,
    private val healthRepository: HealthRepository,
    private val syncHealthData: SyncHealthDataUseCase,
) : ViewModel() {

    /** Snackbar text for whatever last finished. Consumed by the screen. */
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    // ── Google Tasks import (spec §6 nice-to-have) ────────────────────

    private val _tasksImport = MutableStateFlow(TasksImportState())
    val tasksImport = _tasksImport.asStateFlow()

    /** Set when Google needs the user to grant the Tasks scope; the screen launches it. */
    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent = _consentIntent.asStateFlow()

    /**
     * Whether the Tasks scope is actually held. Null until the first check comes
     * back, so the card renders its ordinary self rather than accusing the user
     * of declining something nobody has looked at yet.
     */
    private val _tasksConsent = MutableStateFlow<TasksConsent?>(null)
    val tasksConsent = _tasksConsent.asStateFlow()

    /**
     * Re-reads the consent state on **every** screen entry (#36). Google's
     * granular consent screen arrives with *"View your tasks"* unticked, so
     * sign-in can succeed while granting nothing — and before this, the only way
     * to discover that was to press Import and watch it fail.
     */
    fun refreshTasksConsent() {
        viewModelScope.launch { _tasksConsent.value = googleTasksClient.consentState() }
    }

    /**
     * Pulls open tasks from Google Tasks, runs each through the same
     * `classifyTask` function the "Smart add" card uses, and opens a review sheet.
     * Nothing is written until the user confirms — identical policy to smart add.
     */
    fun importGoogleTasks() {
        if (_tasksImport.value.isLoading) return
        viewModelScope.launch {
            // A known-missing scope is about to bounce off Google's consent
            // screen, so the review sheet would flash open and shut on the way
            // there — incoherent under a button now labelled "Grant access". The
            // card's own spinner covers it; every terminal branch below re-opens
            // the sheet when there is actually something to review.
            _tasksImport.value = TasksImportState(
                isVisible = _tasksConsent.value != TasksConsent.MISSING,
                isLoading = true,
            )
            when (val result = googleTasksClient.fetchOpenTasks()) {
                is TasksImportResult.NeedsConsent -> {
                    // Authoritative, unlike the cached-account probe: Google
                    // refused to mint a token for this scope.
                    _tasksConsent.value = TasksConsent.MISSING
                    _tasksImport.value = TasksImportState()
                    _consentIntent.value = result.intent
                }

                is TasksImportResult.Failure ->
                    _tasksImport.value =
                        TasksImportState(isVisible = true, error = result.message)

                is TasksImportResult.Success -> {
                    // A token was minted, so the scope is held however the cached
                    // sign-in account reads — a grant made through Google's own
                    // recovery screen need not write itself back there.
                    _tasksConsent.value = TasksConsent.GRANTED
                    val (goals, tasks, areas) = snapshot()
                    // Re-running the import must not duplicate what is already here.
                    // Tasks carry no external id in Firestore, so title is the only
                    // handle we have — see the TODO note about a proper externalId.
                    val existing = tasks.map { it.title.trim().lowercase() }.toSet()
                    val fresh = result.tasks
                        .filter { it.title.trim().lowercase() !in existing }
                        .take(MAX_IMPORT)

                    if (fresh.isEmpty()) {
                        _tasksImport.value = TasksImportState(
                            isVisible = true,
                            error = if (result.tasks.isEmpty()) {
                                "No open tasks found in Google Tasks"
                            } else {
                                "Everything in Google Tasks is already here"
                            },
                        )
                        return@launch
                    }

                    val proposals = coroutineScope {
                        fresh.map { imported ->
                            async {
                                val classification = when (
                                    val r = recommendationRepository.classifyTask(
                                        imported.title,
                                        goals,
                                        areas,
                                    )
                                ) {
                                    is Resource.Success -> r.data
                                    else -> null
                                }
                                val matched =
                                    goals.firstOrNull { it.id == classification?.suggestedGoalId }
                                // The Google Tasks list a task lives in *is* an area
                                // of the user's life — that is the whole premise of
                                // the life-area sync — so the list wins over the
                                // model's guess when the two disagree.
                                val listArea = areas.firstOrNull {
                                    it.googleListId == imported.listId
                                } ?: areas.firstOrNull {
                                    it.name.equals(imported.listTitle.trim(), ignoreCase = true)
                                }
                                val area = listArea ?: areas.firstOrNull {
                                    it.id == classification?.suggestedLifeAreaId
                                }
                                ImportProposal(
                                    externalId = imported.externalId,
                                    title = imported.title,
                                    listId = imported.listId,
                                    listTitle = imported.listTitle,
                                    targetGoalId = matched?.id,
                                    targetGoalTitle = matched?.title,
                                    newGoalTitle = if (matched == null) {
                                        classification?.suggestedNewGoalTitle
                                            ?.takeIf { it.isNotBlank() }
                                            ?: imported.listTitle.ifBlank { imported.title }
                                    } else {
                                        null
                                    },
                                    newGoalCategory = classification?.suggestedCategory
                                        ?: GoalCategory.OTHER,
                                    lifeAreaId = area?.id,
                                    // No area yet for this list: offer to create one
                                    // named after it, so an import can bring the
                                    // user's life areas across in the same pass.
                                    lifeAreaName = area?.name
                                        ?: imported.listTitle.trim().takeIf { it.isNotBlank() },
                                    createsLifeArea = area == null &&
                                        imported.listTitle.isNotBlank(),
                                    difficulty = classification?.difficulty
                                        ?: Difficulty.ROUTINE,
                                    // Not `?: DEFAULT_MINUTES` any more (#9): the
                                    // substitution happens once, at the write, where
                                    // the provenance is recorded beside it.
                                    minutes = classification?.estimatedMinutes,
                                )
                            }
                        }.awaitAll()
                    }
                    _tasksImport.value = TasksImportState(
                        isVisible = true,
                        proposals = proposals,
                        totalFound = result.tasks.size,
                    )
                }
            }
        }
    }

    fun toggleImportProposal(externalId: String) {
        _tasksImport.update { state ->
            state.copy(
                proposals = state.proposals.map {
                    if (it.externalId == externalId) it.copy(selected = !it.selected) else it
                },
            )
        }
    }

    /** Creates any goals the proposals need, then the selected tasks. */
    fun confirmImport() {
        val state = _tasksImport.value
        if (state.isSaving) return
        val chosen = state.proposals.filter { it.selected }
        if (chosen.isEmpty()) {
            _tasksImport.value = TasksImportState()
            return
        }
        viewModelScope.launch {
            _tasksImport.update { it.copy(isSaving = true) }
            val areasNow = lifeAreaRepository.observeLifeAreas().first()
            // Two proposals can ask for the same new goal — or the same new life
            // area, when several tasks come from one Google Tasks list. Create each
            // once and reuse the id.
            val createdGoals = mutableMapOf<String, String>()
            val createdAreas = mutableMapOf<String, String>()
            // Hexes handed out so far, so two lists imported in one pass do not come
            // out the same colour in the pie chart.
            val usedHexes = areasNow.map { it.colorHex }.toMutableList()
            var nextOrder = (areasNow.maxOfOrNull { it.sortOrder } ?: -1) + 1
            var saved = 0
            var newAreas = 0
            for (proposal in chosen) {
                val areaId = proposal.lifeAreaId
                    ?: proposal.takeIf { it.createsLifeArea }?.let { p ->
                        createdAreas[p.listId] ?: run {
                            val hex = LifeAreaPalette.nextHex(usedHexes)
                            val created = lifeAreaRepository.upsertLifeArea(
                                LifeArea(
                                    name = p.lifeAreaName.orEmpty(),
                                    colorHex = hex,
                                    iconKey = LifeAreaPalette.iconKeyFor(p.lifeAreaName.orEmpty()),
                                    googleListId = p.listId,
                                    sortOrder = nextOrder,
                                ),
                            )
                            (created as? Resource.Success)?.data?.also {
                                createdAreas[p.listId] = it
                                usedHexes += hex
                                nextOrder++
                                newAreas++
                            }
                        }
                    }
                val goalId = proposal.targetGoalId ?: run {
                    val key = proposal.newGoalTitle.orEmpty().lowercase()
                    createdGoals[key] ?: run {
                        val created = goalRepository.upsertGoal(
                            Goal(
                                title = proposal.newGoalTitle.orEmpty().ifBlank { proposal.title },
                                category = proposal.newGoalCategory,
                                // USER, not AI_SUGGESTED, even though the same sorter proposed
                                // the title (#6, §0.7). The import has a **review sheet**: this
                                // goal is on a list Ido ticked and confirmed, so the intrinsic
                                // edge is his assertion, not the app's. Quick-add has no such
                                // moment, which is exactly why its new goals sit pending and
                                // these do not.
                                declaredBy = DeclaredBy.USER,
                                lifeAreaIds = listOfNotNull(areaId),
                            ),
                        )
                        (created as? Resource.Success)?.data?.also { createdGoals[key] = it }
                    }
                }
                if (goalId == null) continue
                val result = taskRepository.upsertTask(
                    Task(
                        // The import's review sheet asks which goal, never what the task is
                        // worth to it — so the edge declares nothing (§1.5).
                        goalEdges = goalEdgesOf(goalId),
                        title = proposal.title,
                        difficulty = proposal.difficulty,
                        source = TaskSource.GOOGLE_TASKS,
                        estimatedMinutes = proposal.minutes ?: TaskDuration.DEFAULT_MINUTES,
                        durationSource = proposal.minutes.durationSource(),
                    ),
                )
                if (result is Resource.Success) saved++
            }
            _tasksImport.value = TasksImportState()
            _message.value = when {
                saved == 0 -> "Could not import those tasks"
                newAreas > 0 -> "Imported $saved task${if (saved == 1) "" else "s"} and " +
                    "$newAreas life area${if (newAreas == 1) "" else "s"}"
                saved == 1 -> "Imported 1 task from Google Tasks"
                else -> "Imported $saved tasks from Google Tasks"
            }
        }
    }

    fun dismissImport() { _tasksImport.value = TasksImportState() }

    /**
     * The user backed out of Google's consent screen. That is a *decision*, not a
     * dropped intent, so the card says so instead of silently reverting to the
     * generic prompt it showed before (#36).
     */
    fun onConsentDeclined() {
        _consentIntent.value = null
        _tasksConsent.value = TasksConsent.MISSING
    }

    /**
     * Called after the user completes Google's consent screen. The retried import
     * is what settles [tasksConsent] — completing the screen is not the same as
     * having ticked the box on it.
     */
    fun onConsentGranted() {
        _consentIntent.value = null
        importGoogleTasks()
    }

    /**
     * The three collections an import reasons over, read once.
     *
     * `first()` on each observer rather than three held caches — see this class's
     * KDoc. They are read together so a proposal cannot be built against a goal
     * list from one instant and an area list from another.
     */
    private suspend fun snapshot(): Triple<List<Goal>, List<Task>, List<LifeArea>> =
        coroutineScope {
            val goals = async { goalRepository.observeGoals().first() }
            val tasks = async { taskRepository.observeTasks().first() }
            val areas = async { lifeAreaRepository.observeLifeAreas().first() }
            Triple(goals.await(), tasks.await(), areas.await())
        }

    // ── Health Connect sync (spec §5, §6 nice-to-have) ────────────────

    private val _healthSync = MutableStateFlow(HealthSyncState())
    val healthSync = _healthSync.asStateFlow()

    /** Handed straight to the Health Connect permission contract by the screen. */
    val healthPermissions: Set<String> get() = healthRepository.requiredPermissions

    private var healthChecked = false

    /**
     * Syncs are fired from the root scaffold as well as from this screen's card, so
     * the card mirrors the shared state rather than owning it.
     *
     * This `init` sits *below* [_healthSync] on purpose: property initialisers run
     * in source order, and [SyncHealthDataUseCase.status] is a `StateFlow`, so
     * collecting it delivers the current value synchronously on the main
     * dispatcher — from a block placed above, that lands on a field that is still
     * null and the app dies before its first frame.
     */
    init {
        viewModelScope.launch {
            syncHealthData.status.collect { status ->
                _healthSync.update {
                    it.copy(
                        isSyncing = status.isSyncing,
                        lastSyncAtMillis = status.lastSyncAtMillis,
                    )
                }
            }
        }
        viewModelScope.launch {
            syncHealthData.results.collect(::onHealthSyncResult)
        }
    }

    /** Resolves the card's state once per screen entry. */
    fun ensureHealthAvailability() {
        if (healthChecked) return
        healthChecked = true
        refreshHealthAvailability()
    }

    private fun refreshHealthAvailability() {
        viewModelScope.launch {
            val availability = healthRepository.availability()
            _healthSync.update { it.copy(availability = availability) }
        }
    }

    /**
     * Syncs on demand, bypassing the fifteen-minute throttle: the user pressed the
     * button, so "you synced eight minutes ago" is not an answer.
     *
     * The result is not handled here — every sync, whoever started it, comes back
     * through [SyncHealthDataUseCase.results], which this ViewModel already
     * collects. Handling it in both places is how the two paths drift apart.
     */
    fun syncHealth() {
        if (_healthSync.value.isSyncing) return
        viewModelScope.launch { syncHealthData(HealthSyncTrigger.MANUAL) }
    }

    /**
     * Turns a finished sync into a snackbar and keeps the card honest.
     *
     * An automatic sync only ever speaks when it *wrote* something — a failure or
     * an empty week that the user did not ask about is not worth a snackbar on
     * every app launch. A manual one reports whatever happened, including nothing.
     */
    private fun onHealthSyncResult(result: HealthSyncResult) {
        val outcome = result.outcome
        val manual = result.trigger == HealthSyncTrigger.MANUAL

        _healthSync.update {
            when (outcome) {
                is HealthSyncOutcome.Logged, HealthSyncOutcome.UpToDate ->
                    it.copy(availability = HealthAvailability.AVAILABLE)
                HealthSyncOutcome.PermissionsRequired -> it.copy(
                    availability = HealthAvailability.PERMISSIONS_REQUIRED,
                    // Only a manual press may raise the system permission dialog:
                    // one that appears by itself on every launch is an ambush.
                    requestPermissions = manual,
                )
                is HealthSyncOutcome.Unavailable -> it.copy(availability = outcome.availability)
                else -> it
            }
        }

        val message = when (outcome) {
            is HealthSyncOutcome.Logged -> outcome.describe()
            HealthSyncOutcome.UpToDate ->
                "Health Connect is already up to date".takeIf { manual }
            is HealthSyncOutcome.Unavailable -> outcome.availability.explain().takeIf { manual }
            is HealthSyncOutcome.Failed ->
                (outcome.message ?: "Could not sync Health Connect").takeIf { manual }
            // Throttled, AlreadyRunning, NotSignedIn and a permission request are
            // all either invisible or already answered by the card itself.
            else -> null
        }
        if (message != null) _message.value = message
    }

    fun consumeHealthPermissionRequest() {
        _healthSync.update { it.copy(requestPermissions = false) }
    }

    /** Called with whatever Health Connect actually granted, which may be a subset. */
    fun onHealthPermissionsResult(granted: Set<String>) {
        _healthSync.update { it.copy(requestPermissions = false) }
        if (granted.containsAll(healthRepository.requiredPermissions)) {
            syncHealth()
        } else {
            refreshHealthAvailability()
            _message.value = "GoalPilot was not given access to your steps and sleep"
        }
    }

    private companion object {
        /**
         * Cap on tasks imported per run. Each one costs a `classifyTask` call, and
         * GROQ's free tier allows 30 requests/minute — a 60-task list would blow
         * through it and half the classifications would silently fall back.
         */
        const val MAX_IMPORT = 15
    }
}

/** Review sheet for a Google Tasks import, before anything is written. */
data class TasksImportState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val proposals: List<ImportProposal> = emptyList(),
    /** How many open tasks Google returned, before dedupe and the import cap. */
    val totalFound: Int = 0,
    val error: String? = null,
)

/**
 * One Google Tasks entry plus the LLM's filing proposal. Exactly one of
 * [targetGoalId] (attach to an existing goal) or [newGoalTitle] (create one) is set.
 */
data class ImportProposal(
    val externalId: String,
    val title: String,
    val listId: String = "",
    val listTitle: String = "",
    val targetGoalId: String? = null,
    val targetGoalTitle: String? = null,
    val newGoalTitle: String? = null,
    val newGoalCategory: GoalCategory = GoalCategory.OTHER,
    /** Existing life area for this task's Google Tasks list, if there is one. */
    val lifeAreaId: String? = null,
    /** Name shown on the row: the existing area, or the one that will be created. */
    val lifeAreaName: String? = null,
    /** True when confirming will also create a life area for this task's list. */
    val createsLifeArea: Boolean = false,
    /** How demanding the model judged the row (§1.4, `#55`). Was `points: Int`. */
    val difficulty: Difficulty = Difficulty.ROUTINE,
    /** What the model said the task takes, or **null when it did not say** (#9). */
    val minutes: Int? = null,
    val selected: Boolean = true,
)

/**
 * The health card's state. There is no review sheet any more — the sync writes
 * whatever is not logged yet — so this only has to describe the card itself.
 *
 * [availability] is null only before the first check has come back; the card
 * renders a neutral "checking" state until then rather than claiming the feature
 * is unsupported.
 */
data class HealthSyncState(
    val availability: HealthAvailability? = null,
    val isSyncing: Boolean = false,
    /** Epoch millis of the last successful read; 0 until this account has synced. */
    val lastSyncAtMillis: Long = 0L,
    /** Set when the screen must launch the Health Connect permission contract. */
    val requestPermissions: Boolean = false,
)

/** e.g. "Logged 3 readings from Health Connect · 1 topped up". */
fun HealthSyncOutcome.Logged.describe(): String = buildString {
    append("Logged $entries reading${if (entries == 1) "" else "s"} from Health Connect")
    if (createdGoals > 0) {
        append(" · created ${createdGoals} goal${if (createdGoals == 1) "" else "s"}")
    }
    if (topUps > 0) append(" · $topUps topped up")
}

/** "Just now" / "12 minutes ago" / "Yesterday" for the card's footer. */
fun healthSyncAgoLabel(lastSyncAtMillis: Long, nowMillis: Long): String? {
    if (lastSyncAtMillis <= 0L) return null
    val minutes = ((nowMillis - lastSyncAtMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "Synced just now"
        minutes < 60 -> "Synced $minutes minute${if (minutes == 1L) "" else "s"} ago"
        minutes < 24 * 60 -> {
            val hours = minutes / 60
            "Synced $hours hour${if (hours == 1L) "" else "s"} ago"
        }
        else -> {
            val days = minutes / (24 * 60)
            "Synced $days day${if (days == 1L) "" else "s"} ago"
        }
    }
}

/** Why the health card cannot sync right now, in the user's terms. */
fun HealthAvailability.explain(): String = when (this) {
    HealthAvailability.NOT_SUPPORTED ->
        "Health Connect is not available on this device"
    HealthAvailability.PROVIDER_UPDATE_REQUIRED ->
        "Update Health Connect from the Play Store to sync your steps and sleep"
    HealthAvailability.PERMISSIONS_REQUIRED ->
        "GoalPilot needs permission to read your steps and sleep"
    HealthAvailability.AVAILABLE ->
        "Health Connect is ready"
}
