package com.idomarhaim.goalpilot.feature.dashboard

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.core.util.StoragePaths
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksClient
import com.idomarhaim.goalpilot.data.tasks.TasksImportResult
import com.idomarhaim.goalpilot.domain.model.DerivedProgress
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.TasksConsent
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncOutcome
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncResult
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncTrigger
import com.idomarhaim.goalpilot.domain.usecase.SyncHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val recommendationRepository: RecommendationRepository,
    private val socialRepository: SocialRepository,
    private val storageRepository: StorageRepository,
    private val googleTasksClient: GoogleTasksClient,
    private val healthRepository: HealthRepository,
    private val lifeAreaRepository: LifeAreaRepository,
    private val buildSummary: BuildSummaryUseCase,
    private val syncHealthData: SyncHealthDataUseCase,
) : ViewModel() {

    @Volatile private var lastGoals: List<Goal> = emptyList()
    @Volatile private var lastTasks: List<Task> = emptyList()

    /**
     * The user's life areas, kept warm for the AI flows: a goal the assistant
     * creates has to be filed somewhere, and asking Firestore for the areas in the
     * middle of a classification would make the smart-add sheet wait on a network
     * round trip it does not need.
     */
    @Volatile private var lastLifeAreas: List<LifeArea> = emptyList()

    init {
        viewModelScope.launch {
            lifeAreaRepository.observeLifeAreas().collect { lastLifeAreas = it }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        authRepository.authState(),
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
    ) { user, goals, tasks ->
        lastGoals = goals
        lastTasks = tasks
        val windowStart = DateTimeUtils.windowStart(SummaryPeriod.WEEKLY)
        val completedLast7d = tasks.count {
            it.isDone && (it.completedAtEpochMillis ?: 0L) >= windowStart
        }
        DashboardUiState(
            isLoading = false,
            userName = user?.displayName?.substringBefore(' ').orEmpty(),
            points = user?.points ?: 0L,
            level = user?.level ?: 1,
            levelProgress = user?.levelProgress ?: 0f,
            pointsToNextLevel = user?.pointsToNextLevel ?: 0L,
            goals = goals,
            // Not a plain mean of `progressFraction` (§4.4's ⚠️, and observed on a
            // device as "Overall progress 16259%"): that averages unbounded
            // fractions, so one goal past a periodic target sets a headline about
            // everything. Clamped per goal at the aggregation site — the goal's own
            // screens still show the overshoot.
            averageProgress = DerivedProgress.overallCompletionOf(goals),
            completedTasksLast7d = completedLast7d,
            doneTasks = tasks.count { it.isDone },
            totalTasks = tasks.size,
        )
    }.catch { emit(DashboardUiState(isLoading = false, error = it.message)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardUiState(isLoading = true),
        )

    private val _recs = MutableStateFlow(RecommendationsState())
    val recommendations = _recs.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private var recommendationsLoaded = false

    /** Loads AI recommendations once per screen entry; call again to refresh. */
    fun ensureRecommendations() {
        if (recommendationsLoaded) return
        recommendationsLoaded = true
        refreshRecommendations()
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            _recs.update { it.copy(isLoading = true, error = null) }
            val state = uiState.value
            when (val result = recommendationRepository.getRecommendations(
                goals = lastGoals,
                completedTasksLast7d = state.completedTasksLast7d,
                totalPoints = state.points,
            )) {
                is Resource.Success ->
                    _recs.update { RecommendationsState(isLoading = false, items = result.data) }
                is Resource.Error ->
                    _recs.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    // ── Smart add (spec §6 Bonus: LLM task→goal classification) ──────

    private val _smartAdd = MutableStateFlow(SmartAddState())
    val smartAdd = _smartAdd.asStateFlow()

    /**
     * Runs a free-text task title through the `classifyTask` Cloud Function and
     * opens a confirmation sheet with the proposal: attach it to an existing goal,
     * or create the goal the LLM suggests. The user always confirms — the LLM
     * proposes, it never writes (spec §8: LLM output can be inconsistent).
     */
    fun classifyForSmartAdd(rawTitle: String) {
        val title = rawTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _smartAdd.value = SmartAddState(isVisible = true, isClassifying = true, taskTitle = title)
            val goals = lastGoals
            val areas = lastLifeAreas
            val classification =
                when (val r = recommendationRepository.classifyTask(title, goals, areas)) {
                    is Resource.Success -> r.data
                    else -> null
                }
            if (classification == null) {
                _smartAdd.value = SmartAddState()
                _message.value = "Could not analyse that task"
                return@launch
            }
            // The model can name a goal id that does not exist; only trust ids we
            // can actually resolve, otherwise fall through to the new-goal branch.
            val matched = goals.firstOrNull { it.id == classification.suggestedGoalId }
            // A new goal is filed under the area the model picked; an existing goal
            // keeps whatever areas the user already gave it. The sheet names one
            // area, so a goal serving several shows its first — the goal itself is
            // not re-filed from here, and this row is a "where will this land?"
            // label rather than a write.
            val area = areas.firstOrNull {
                it.id == (matched?.lifeAreaIds?.firstOrNull() ?: classification.suggestedLifeAreaId)
            }
            _smartAdd.value = SmartAddState(
                isVisible = true,
                isClassifying = false,
                taskTitle = title,
                targetGoalId = matched?.id,
                targetGoalTitle = matched?.title,
                newGoalTitle = if (matched == null) {
                    classification.suggestedNewGoalTitle?.takeIf { it.isNotBlank() } ?: title
                } else {
                    null
                },
                newGoalCategory = classification.suggestedCategory,
                lifeAreaId = area?.id,
                lifeAreaName = area?.name,
                points = classification.estimatedPoints.coerceIn(1, 1000),
                minutes = classification.estimatedMinutes,
                rationale = classification.rationale,
            )
        }
    }

    /** Applies the proposal: creates the goal if needed, then the task. */
    fun confirmSmartAdd() {
        val state = _smartAdd.value
        if (state.isClassifying || state.isSaving) return
        viewModelScope.launch {
            _smartAdd.update { it.copy(isSaving = true) }
            val goalId = state.targetGoalId ?: run {
                val newGoal = Goal(
                    title = state.newGoalTitle.orEmpty().ifBlank { state.taskTitle },
                    category = state.newGoalCategory,
                    // One area, or none — §1.2's empty collection. The sheet
                    // proposes a single area, and inventing a second here would
                    // be the app asserting a filing the user never saw.
                    lifeAreaIds = listOfNotNull(state.lifeAreaId),
                )
                when (val r = goalRepository.upsertGoal(newGoal)) {
                    is Resource.Success -> r.data
                    else -> null
                }
            }
            if (goalId == null) {
                _smartAdd.value = SmartAddState()
                _message.value = "Could not create the goal"
                return@launch
            }
            val saved = taskRepository.upsertTask(
                Task(
                    goalId = goalId,
                    title = state.taskTitle,
                    points = state.points,
                    estimatedMinutes = state.minutes,
                ),
            )
            _smartAdd.value = SmartAddState()
            _message.value = when (saved) {
                is Resource.Success ->
                    if (state.targetGoalId != null) {
                        "Added to “${state.targetGoalTitle}”"
                    } else {
                        "Created “${state.newGoalTitle}” and added the task"
                    }
                else -> "Could not add the task"
            }
        }
    }

    fun dismissSmartAdd() { _smartAdd.value = SmartAddState() }

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
     *
     * Deliberately *not* guarded the way [ensureRecommendations] is: that guard
     * stops an expensive network call re-firing on back-navigation, and this is a
     * local read. Guarding it would leave this card accusing a user who granted
     * the scope on the life-areas screen and navigated back — this ViewModel
     * survives in the back stack even though the composable is recreated.
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
                    // Re-running the import must not duplicate what is already here.
                    // Tasks carry no external id in Firestore, so title is the only
                    // handle we have — see the TODO note about a proper externalId.
                    val existing = lastTasks.map { it.title.trim().lowercase() }.toSet()
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

                    val goals = lastGoals
                    val areas = lastLifeAreas
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
                                    points = (classification?.estimatedPoints ?: 10)
                                        .coerceIn(1, 1000),
                                    minutes = classification?.estimatedMinutes
                                        ?: TaskDuration.DEFAULT_MINUTES,
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
            // Two proposals can ask for the same new goal — or the same new life
            // area, when several tasks come from one Google Tasks list. Create each
            // once and reuse the id.
            val createdGoals = mutableMapOf<String, String>()
            val createdAreas = mutableMapOf<String, String>()
            // Hexes handed out so far, so two lists imported in one pass do not come
            // out the same colour in the pie chart.
            val usedHexes = lastLifeAreas.map { it.colorHex }.toMutableList()
            var nextOrder = (lastLifeAreas.maxOfOrNull { it.sortOrder } ?: -1) + 1
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
                                lifeAreaIds = listOfNotNull(areaId),
                            ),
                        )
                        (created as? Resource.Success)?.data?.also { createdGoals[key] = it }
                    }
                }
                if (goalId == null) continue
                val result = taskRepository.upsertTask(
                    Task(
                        goalId = goalId,
                        title = proposal.title,
                        points = proposal.points,
                        source = TaskSource.GOOGLE_TASKS,
                        estimatedMinutes = proposal.minutes,
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

    fun shareWeeklySummary(imageUri: Uri?) {
        viewModelScope.launch {
            _message.value = null
            val summary = buildSummary(
                period = SummaryPeriod.WEEKLY,
                goals = lastGoals,
                tasks = lastTasks,
                windowStartMillis = DateTimeUtils.windowStart(SummaryPeriod.WEEKLY),
            )
            val imageUrl: String? = imageUri?.let { uri ->
                when (val up = storageRepository.uploadImage(StoragePaths.SUMMARY_IMAGES, uri)) {
                    is Resource.Success -> up.data
                    else -> null
                }
            }
            _message.value = when (socialRepository.shareSummary(summary, imageUrl)) {
                is Resource.Success -> "Shared your weekly summary!"
                else -> "Could not share summary"
            }
        }
    }

    fun consumeMessage() { _message.value = null }

    private companion object {
        /**
         * Cap on tasks imported per run. Each one costs a `classifyTask` call, and
         * GROQ's free tier allows 30 requests/minute — a 60-task list would blow
         * through it and half the classifications would silently fall back.
         */
        const val MAX_IMPORT = 15
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val points: Long = 0L,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val pointsToNextLevel: Long = 0L,
    val goals: List<Goal> = emptyList(),
    val averageProgress: Float = 0f,
    val completedTasksLast7d: Int = 0,
    val doneTasks: Int = 0,
    val totalTasks: Int = 0,
    val error: String? = null,
)

data class RecommendationsState(
    val isLoading: Boolean = false,
    val items: List<Recommendation> = emptyList(),
    val error: String? = null,
)

/**
 * The LLM's proposal for a free-text task, awaiting the user's confirmation.
 * Exactly one of [targetGoalId] (attach to an existing goal) or [newGoalTitle]
 * (create one) is set once classification finishes.
 */
data class SmartAddState(
    val isVisible: Boolean = false,
    val isClassifying: Boolean = false,
    val isSaving: Boolean = false,
    val taskTitle: String = "",
    val targetGoalId: String? = null,
    val targetGoalTitle: String? = null,
    val newGoalTitle: String? = null,
    val newGoalCategory: GoalCategory = GoalCategory.OTHER,
    /** Life area the resulting goal is filed under, when one could be resolved. */
    val lifeAreaId: String? = null,
    val lifeAreaName: String? = null,
    val points: Int = 10,
    /** Minutes the AI thinks the task takes — carried onto the saved task. */
    val minutes: Int = TaskDuration.DEFAULT_MINUTES,
    val rationale: String = "",
)

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
    val points: Int = 10,
    val minutes: Int = TaskDuration.DEFAULT_MINUTES,
    val selected: Boolean = true,
)
