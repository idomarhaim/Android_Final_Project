package com.idomarhaim.goalpilot.feature.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.core.util.StoragePaths
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksClient
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Deletion
import com.idomarhaim.goalpilot.domain.model.DerivedProgress
import com.idomarhaim.goalpilot.domain.model.durationSource
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.SmartFiling
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import com.idomarhaim.goalpilot.domain.usecase.CalendarEntry
import com.idomarhaim.goalpilot.domain.usecase.DailyMissReview
import com.idomarhaim.goalpilot.domain.usecase.DisappearanceChoice
import com.idomarhaim.goalpilot.domain.usecase.SyncCalendarUseCase
import com.idomarhaim.goalpilot.domain.usecase.MissedOccurrence
import com.idomarhaim.goalpilot.domain.usecase.SyncHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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
    private val occurrenceRepository: OccurrenceRepository,
    private val buildSummary: BuildSummaryUseCase,
    private val syncHealthData: SyncHealthDataUseCase,
    private val syncCalendar: SyncCalendarUseCase,
    private val appPreferences: AppPreferencesRepository,
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

    private val _missReview = MutableStateFlow(MissReviewState())

    /**
     * §2.5's **daily miss review** — *"Misses meet Ido once, in a daily review on app open,
     * never as a push saying he failed"* (`#56`).
     *
     * The dashboard is *"on app open"*: it is the start destination, so its first composition
     * is the event. There is no worker, no notification and nothing armed — which is not an
     * economy, it is the clause. A push about a miss is the one thing §2.5 rules out by name.
     */
    val missReview: StateFlow<MissReviewState> = _missReview.asStateFlow()

    /**
     * §2.7's disappearances, awaiting **Keep / Cancel / Put back**
     * ([`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61)).
     *
     * ### It sits beside the miss review because §2.7 says where it goes, not because it fits
     *
     * *"The ambiguity is **asked** in the daily-review batch sheet — Keep / Cancel / Put back —
     * at the one moment Ido is holding the phone."* So this is the same surface and the same
     * moment as `missReview`, and it is deliberately **not** a notification: an event vanishing
     * from a calendar is not news worth interrupting anyone for, and §2.5 already rules out
     * pushing about things that went wrong.
     *
     * ### Passed straight through from the use case, with no snapshot taken
     *
     * Unlike [missReview] — which is a `first()` snapshot precisely so a deadline lapsing
     * mid-session cannot make a card appear — this list only grows when a **sync** finds a
     * disappearance, and a sync only runs on foreground or on a tap. So there is no moment it
     * could ambush anyone, and a live flow means an answer removes its own row immediately.
     */
    val calendarDisappearances: StateFlow<List<CalendarEntry>> = syncCalendar.disappearances

    /**
     * True once the review has been evaluated for this ViewModel.
     *
     * The task list arrives asynchronously and then **re-emits** on every unrelated write, so
     * without this the review would be recomputed after each one; and since showing it stamps
     * the preference, the second pass would find its own stamp and conclude the misses had
     * already been met. One evaluation per app open, which is what the sentence says.
     */
    @Volatile private var missReviewEvaluated: Boolean = false

    init {
        viewModelScope.launch {
            // `first()` and not `collect`: the review is a snapshot of the moment the app
            // opened, and a live one would appear mid-session the instant a deadline passed --
            // which is the push §2.5 forbids, wearing a card instead of a notification.
            val tasks = taskRepository.observeTasks(null).first()
            openDailyMissReview(tasks)
        }
    }

    /**
     * Works out today's review and shows it, or does nothing.
     *
     * Everything decidable is decided in [DailyMissReview], which takes its clock as an
     * argument; this method's whole job is to read the stamp, hand over three values, and
     * write the stamp back.
     *
     * The stamp moves **when the review is shown**, not when it is dismissed. §2.5 says the
     * misses *meet* him once; a stamp that waited for a tap would re-show everything to
     * somebody who opened the app, read the card and switched away, which is the commonest way
     * anybody actually reads it.
     */
    internal fun openDailyMissReview(
        tasks: List<Task>,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        if (missReviewEvaluated) return
        missReviewEvaluated = true

        val lastShownAt = appPreferences.missReviewLastShownAt()
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        if (!DailyMissReview.isDue(lastShownAt?.toLocalDate(), now.toLocalDate())) return

        val misses = DailyMissReview.of(tasks = tasks, now = now, since = lastShownAt)
        // Nothing missed is not a review worth showing, and it must not move the stamp either:
        // stamping today would make tomorrow's review skip everything that lapsed today.
        if (misses.isEmpty()) return

        _missReview.value = MissReviewState(misses = misses, isVisible = true)
        appPreferences.setMissReviewLastShownAt(
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }

    /**
     * Takes the review down.
     *
     * It does not touch the stamp — that moved when the card appeared — so this is presentation
     * only, and dismissing it cannot re-arm anything.
     */
    fun dismissMissReview() {
        _missReview.update { it.copy(isVisible = false) }
    }

    /**
     * Answers §2.7's disappearance question for one occurrence.
     *
     * There is nothing to dismiss and no *"not now"*: the link is **already cleared** by the
     * time the row exists, so an unanswered row costs nothing and a user who ignores the whole
     * sheet keeps every occurrence on its original date. That is what lets this be three
     * buttons rather than three buttons and an escape.
     */
    fun resolveDisappearance(entry: CalendarEntry, choice: DisappearanceChoice) {
        viewModelScope.launch { syncCalendar.resolve(entry, choice) }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        authRepository.authState(),
        goalRepository.observeGoals(),
        taskRepository.observeTasks(null),
        // `#67`: the occurrence collection is here for ONE reason -- a task with a stored
        // occurrence is drawn on the calendar even with no anchor of its own, so it is reachable
        // and must not be listed as lost. Nothing else on this screen reads it.
        occurrenceRepository.observeOccurrences(),
    ) { user, goals, tasks, occurrences ->
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
            unreachableTasks = Deletion.unreachableTasks(
                tasks = tasks,
                goals = goals,
                occurrences = occurrences,
            ),
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
                    // §1.5, `#55`: the link and what it contributes are one edge. A quick-add
                    // declares no contribution — nobody was asked what this task is worth in
                    // the goal's own word — so the edge is silent and adds nothing to the
                    // measure until something declares it.
                    goalEdges = goalEdgesOf(goalId),
                    title = title,
                    difficulty = classification.difficulty,
                    // `#7`/`R6`: a task typed in because it is already finished is created
                    // done, in THIS write. Not upsert-then-tick — `upsertTask` mints the
                    // completion fact through `TaskCompletion.of` and commits it in the same
                    // batch as the task, so the fact leaves here whole. Nothing else on this
                    // path changes: the same classify, the same filing decision, one commit.
                    completion = if (alreadyDone) CompletionFact() else null,
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

    /**
     * Deletes one of [DashboardUiState.unreachableTasks] — `#67`'s first item.
     *
     * ### Why the dashboard is where this lives
     *
     * The reach gap is not evenly spread: `GoalDetailScreen` and `CalendarScreen` each list
     * tasks and each can grow a delete, but a task that is filed under nothing *and* dated
     * nothing appears on neither, so there is no row anywhere to hang a control on. It gets a
     * card here because this is the screen that **creates** them — `classifyForSmartAdd` files
     * a `FilingDecision.NoGoal` with no `occurrence`, and [undoFiling]'s snackbar was the only
     * control that has ever been able to remove one.
     *
     * ### It removes the task and offers nothing else, deliberately
     *
     * Filing it, dating it or renaming it from here would be a second author for edits that
     * already have screens, which is the *"second way to do one thing"* §0.3 keeps naming.
     * `#67` is about **reach**, and the missing reach is a delete.
     */
    fun deleteUnreachableTask(taskId: String) {
        viewModelScope.launch {
            _message.value = when (taskRepository.deleteTask(taskId)) {
                is Resource.Success -> "Task deleted"
                else -> "Could not delete that task"
            }
        }
    }


    // ── Google Tasks import and Health Connect: MOVED OUT, 2026-08-24 ──
    //
    // Both now live in `feature/sync/SyncViewModel.kt`, drawn on Settings
    // rather than here. Ido asked for the placement (2026-08-24); what made
    // it cheap is that the coupling was accidental -- this ViewModel is about
    // today's goals and tasks, and that state was about the device's
    // relationship with two other applications. Six constructor dependencies
    // went with it.
    //
    // The AUTOMATIC health sync never lived here: `RootViewModel` fires it on
    // APP_FOREGROUND, so nothing about steps and sleep changed.

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
    /**
     * **The tasks no screen lists** — `#67`'s founding defect, surfaced here.
     *
     * Computed by [Deletion.unreachableTasks] rather than filtered here, because *"which tasks
     * are on no screen"* is a claim about every screen at once and belongs where it can be
     * JVM-tested (`DeletionReachTest`). This screen renders it; it does not decide it.
     *
     * Almost always empty, and that is the point — an empty list draws no card at all.
     */
    val unreachableTasks: List<Task> = emptyList(),
    val error: String? = null,
) {
    /**
     * How many goals actually have a number — the population [averageProgress] is
     * a mean **over**, and therefore the one the ring's caption has to name
     * (`#66` follow-on).
     *
     * `#66` moved [DerivedProgress.overallCompletionOf] to skip goals with no
     * measure, because an unmeasured goal's fraction is against §1.3's `100.0`
     * default — a target nobody set. That was right and it left the **caption**
     * behind: *"Averaged across all your goals"* over a mean taken across a
     * subset is §0.3's *second number that quietly disagrees*, reintroduced by
     * the fix that removed it. `ProgressSummary.measuredGoals` is the same
     * accessor, added in the same ticket for the same reason, on the summary that
     * gets **published**; this screen could not have it then because the file was
     * another session's.
     */
    val measuredGoalCount: Int get() = goals.count { !it.isUnmeasured }
}

/**
 * §2.5's daily review, as the dashboard holds it (`#56`).
 *
 * [misses] is kept when [isVisible] goes false rather than cleared, so dismissing the card
 * cannot be mistaken for *there was nothing to review* by anything reading this later in the
 * session — including a test asserting that the review happened.
 */
data class MissReviewState(
    val misses: List<MissedOccurrence> = emptyList(),
    val isVisible: Boolean = false,
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
