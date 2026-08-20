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
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.SmartFiling
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
            userFullName = user?.displayName.orEmpty(),
            userPhotoUrl = user?.photoUrl,
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

    // ── Smart add — silent filing (#6, R3; spec §0.7, §3.4, §3.5) ────

    private val _smartAdd = MutableStateFlow(SmartAddState())
    val smartAdd = _smartAdd.asStateFlow()

    private val _filed = MutableStateFlow<SmartAddReceipt?>(null)

    /**
     * The witness for a filing that happened without asking (`#6`).
     *
     * §0.7 permits acting without asking; it does **not** permit acting without a witness.
     * The first time the sorter is wrong, this receipt is how Ido finds out what it did and
     * takes it back — so it is not a courtesy snackbar, it is the thing that makes the silence
     * legitimate. The screen turns it into a message with an **Undo**.
     */
    val filed = _filed.asStateFlow()

    /**
     * Files a free-text task, and asks nobody where it goes.
     *
     * `R3` was *"it asks for approval on where to file every task you enter; the default should
     * be that it does not ask and just does it"*, and the triage promoted it out of settings —
     * §0.7 makes silence the rule rather than a preference, so **there is no dialog and no
     * toggle**. What used to sit here was `SmartAddDialog`: classify, then show the proposal and
     * wait for *Add* or *Cancel*, on every single task.
     *
     * The branch table is [SmartFiling.decide], which is pure and tested on the JVM. This
     * function is the part that cannot be: one call out, one or two writes, one receipt.
     *
     * **One write when the goal exists, two when it does not** — and the second case creates
     * the goal *before* the task, so a failure leaves nothing rather than a task pointing at a
     * goal that was never written.
     *
     * [alreadyDone] is `#7`/`R6` — *"there should be a way to complete the task from within
     * quick add"*. It **adds no write**: the count above is unchanged, because the completion
     * rides the task's own `set()` rather than a `setDone` after it (§1.4's *"that same fact,
     * not a second pipe"*). It does not touch the filing decision either — where a task goes
     * and whether it is finished are independent questions, and a done task still belongs
     * under the goal it serves.
     */
    fun classifyForSmartAdd(rawTitle: String, alreadyDone: Boolean = false) {
        val title = rawTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _smartAdd.value =
                SmartAddState(isClassifying = true, taskTitle = title, alreadyDone = alreadyDone)
            val goals = lastGoals
            val areas = lastLifeAreas
            val classification =
                when (val r = recommendationRepository.classifyTask(title, goals, areas)) {
                    is Resource.Success -> r.data
                    else -> null
                }
            if (classification == null) {
                _smartAdd.value = SmartAddState()
                _message.value = "Could not add that task"
                return@launch
            }

            val decision = SmartFiling.decide(classification, goals, areas)
            val goalId = when (decision) {
                is FilingDecision.ExistingGoal -> decision.goalId
                is FilingDecision.NoGoal -> null
                is FilingDecision.NewGoal -> {
                    val created = goalRepository.upsertGoal(
                        Goal(
                            title = decision.title,
                            category = decision.category,
                            // §1.1's pending state. The app has NOT asserted that Ido wants this
                            // for its own sake — that is his to say, and until he does the goals
                            // list marks it and offers to drop the marker (§0.7, §3.5).
                            declaredBy = DeclaredBy.AI_SUGGESTED,
                            // One area, or none — §1.2's empty collection.
                            lifeAreaIds = listOfNotNull(decision.lifeAreaId),
                        ),
                    )
                    when (created) {
                        is Resource.Success -> created.data
                        else -> {
                            _smartAdd.value = SmartAddState()
                            _message.value = "Could not add that task"
                            return@launch
                        }
                    }
                }
            }

            val saved = taskRepository.upsertTask(
                Task(
                    goalId = goalId,
                    title = title,
                    points = classification.estimatedPoints,
                    // `#7`/`R6`: a task typed in because it is already finished is created
                    // done, in THIS write. Not upsert-then-tick — see TaskCompletion, which
                    // stamps `completedAtEpochMillis` inside `upsertTask` so the fact leaves
                    // here whole. Nothing else on this path changes: the same classify, the
                    // same filing decision, the same single `set()`.
                    isDone = alreadyDone,
                    estimatedMinutes = classification.estimatedMinutes ?: TaskDuration.DEFAULT_MINUTES,
                    // §3.4: a duration nobody supplied is recorded as unsupplied. It still counts
                    // as DEFAULT_MINUTES so the task keeps its slice of the pie, but it is not
                    // attributed to the model, and it stays re-estimable — which a USER value
                    // never is.
                    durationSource = classification.estimatedMinutes.durationSource(),
                ),
            )
            _smartAdd.value = SmartAddState()
            if (saved !is Resource.Success) {
                // A goal may already exist at this point, and it is left alone rather than
                // rolled back: it is marked AI_SUGGESTED, so it shows up as a proposal Ido can
                // drop in one tap. A second write on a path that has just proved it cannot
                // write is not a repair.
                _message.value = "Could not add that task"
                return@launch
            }
            _filed.value = SmartAddReceipt(
                taskId = saved.data,
                taskTitle = title,
                decision = decision,
                // Only a goal THIS filing created may be taken back with the task. An existing
                // goal is Ido's and predates the quick-add; undoing a filing must never delete
                // something the filing did not make.
                createdGoalId = if (decision is FilingDecision.NewGoal) goalId else null,
                // §0.7's witness has to say what the app DID, and "and recorded it as done"
                // is half of what it did. A receipt that omitted it would leave the one thing
                // the user cannot otherwise see from this screen unsaid — the task is filed
                // under a goal they are not looking at, so its tick is not on screen either.
                // Undo already covers it: deleting the task removes the completion with it.
                completed = alreadyDone,
            )
        }
    }

    /** Clears the receipt once the screen has shown it. */
    fun consumeFiled() { _filed.value = null }

    /**
     * Takes back the last silent filing — the *undoable* half of §0.7's witness.
     *
     * Deletes the task, and the goal **only if this filing created it**. A goal that was
     * already there keeps every task it had; a goal the sorter minted seconds ago that now
     * holds nothing is not worth keeping, and leaving it behind would turn *undo* into *half of
     * what you just did*.
     */
    fun undoFiling(receipt: SmartAddReceipt) {
        viewModelScope.launch {
            _filed.value = null
            val removed = taskRepository.deleteTask(receipt.taskId)
            if (removed !is Resource.Success) {
                _message.value = "Could not undo that"
                return@launch
            }
            receipt.createdGoalId?.let { goalRepository.deleteGoal(it) }
            _message.value = "Undone"
        }
    }


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
                        goalId = goalId,
                        title = proposal.title,
                        points = proposal.points,
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
    /**
     * The name and photo behind Home's avatar (spec §4.2). Separate from
     * [userName], which is the *first* name the greeting card uses: an avatar
     * falls back to initials, and one initial from a truncated name is a worse
     * fallback than two from the whole one.
     */
    val userFullName: String = "",
    val userPhotoUrl: String? = null,
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
 * The quick-add card's in-flight state — **and nothing else** (`#6`).
 *
 * It used to be a whole proposal awaiting confirmation: a target goal, a proposed new goal, a
 * life area, points, minutes, a rationale, `isVisible`, `isSaving`. All of it existed to fill a
 * dialog that asked *"Add this task?"* about every task the user typed, which is what `R3`
 * asked to be rid of and what §0.7 says was never legitimate. The proposal now goes straight to
 * [SmartFiling.decide] and then to disk, so the only thing the card still has to show is that
 * it is thinking.
 */
data class SmartAddState(
    val isClassifying: Boolean = false,
    val taskTitle: String = "",
    /**
     * Whether the task in flight was typed in as **already finished** (`#7`).
     *
     * It is here, and not only in the card's own `remember`, so that the in-flight row can
     * say so. The card clears its toggle on the tap that starts the classify — a sticky
     * "done" mode would silently complete the *next* task somebody types — which would
     * otherwise leave a second or two of a round trip during which nothing on screen agrees
     * that a completion is being written.
     */
    val alreadyDone: Boolean = false,
)

/**
 * What the app did, after it did it — `#6`'s witness (§0.7).
 *
 * *"Silent" is not "invisible".* Every silent filing must be visible after the fact and
 * undoable, or the first time the sorter is wrong there is no way to find what it did. This
 * carries exactly what is needed to say so and to take it back, and nothing more.
 */
data class SmartAddReceipt(
    val taskId: String,
    val taskTitle: String,
    val decision: FilingDecision,
    /**
     * The goal this filing **created**, or null when it filed under one that already existed.
     *
     * The distinction is what keeps undo safe: it is the difference between removing what the
     * app just made and deleting something of Ido's.
     */
    val createdGoalId: String? = null,
    /**
     * Whether the filing also **completed** the task — `#7`'s half of the witness.
     *
     * Separate from [decision] on purpose: filing and completing are independent, and every
     * one of the three filing outcomes can happen to a task that is already done. Folding it
     * into the sealed hierarchy would double it for a fact that no branch of it decides.
     */
    val completed: Boolean = false,
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

/**
 * The provenance of a proposed duration: the model's if it supplied one, otherwise
 * nobody's (#9, §3.4).
 *
 * One function rather than two call sites so the quick-add sheet and the Google
 * Tasks import cannot drift into disagreeing about what an absent minute count means.
 */
private fun Int?.durationSource(): DurationSource =
    if (this == null) DurationSource.UNKNOWN else DurationSource.AI

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
    /**
     * What the model said the task takes, or **null when it did not say** (#9,
     * §3.4). Null is stored as [TaskDuration.DEFAULT_MINUTES] with
     * `DurationSource.UNKNOWN`, never as an AI estimate.
     */
    val minutes: Int? = null,
    val selected: Boolean = true,
)
