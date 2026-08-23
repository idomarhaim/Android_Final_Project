package com.idomarhaim.goalpilot.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.EditScope
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.ScheduleEdit
import com.idomarhaim.goalpilot.domain.model.ScheduleEdits
import com.idomarhaim.goalpilot.domain.model.SchedulePlan
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What §4.3's surface is showing right now.
 *
 * [days] is the whole of it — the columns, their lanes and their loads — because [CalendarBuilder]
 * is a pure function of everything below and a second derived field here would be a copy that can
 * disagree (§0.3).
 */
data class CalendarUiState(
    val zoom: CalendarZoom = CalendarZoom.DEFAULT,
    /** The first drawn column. Moving the calendar moves this and nothing else. */
    val anchor: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val days: List<CalendarDay> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val isLoading: Boolean = true,
    /**
     * Something the surface has to **say** — §0.4's *legal, but never silent*.
     *
     * On the state rather than in a one-shot event channel, because the only thing that can put
     * one here is an edit the user just asked for and is still looking at. A refusal that survived
     * a rotation and a refusal that did not would be two different products, and this is the one
     * where the sentence is still on screen.
     */
    val notice: CalendarNotice? = null,
) {
    val hasAnything: Boolean get() = days.any { !it.isEmpty }
}

/**
 * §4.3's calendar, wired ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * ### Everything that decides anything is in [CalendarBuilder], and this holds no derived state
 *
 * The view model owns exactly two mutable things — the zoom and the anchor date — and both are
 * things the *user* moved. Every number on screen is recomputed from the streams by a pure
 * function, so there is no cached column, no stored load, and nothing a sweep would have to keep
 * true. That is §2.3's argument for deriving temporal state, applied one layer up: two readers
 * asking at the same instant cannot disagree, because there is no second copy.
 *
 * ### `#61`'s slot is fed with an empty list here, and that is the whole of its wiring
 *
 * §4.3's grey hand-made Google events have nothing to read until
 * [#61](https://github.com/idomarhaim/Android_Final_Project/issues/61) ships. The parameter is
 * passed rather than omitted so that the day it does ship, the change is one flow — not a lane, a
 * fill, an ordering and a set of tests.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val occurrences: OccurrenceRepository,
    private val goals: GoalRepository,
    private val lifeAreas: LifeAreaRepository,
    private val challenges: ChallengeRepository,
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    /**
     * Not injected, deliberately: nothing else in this app binds a [Clock], and adding a Hilt
     * module for one reader would put a machine-wide seam in place to serve a screen whose logic
     * is tested elsewhere. Every rule worth pinning to a fixed instant lives in [CalendarBuilder],
     * [DayLoad] and [CarryForward], all of which take their `now` as an argument and are tested
     * that way. What is left here is *read the wall clock*, which has nothing to assert about it.
     */
    private val clock: Clock = Clock.systemDefaultZone()

    private val zoom = MutableStateFlow(CalendarZoom.DEFAULT)
    private val anchor = MutableStateFlow(LocalDate.now(clock))
    private val notice = MutableStateFlow<CalendarNotice?>(null)

    /** What the user moved or was told, gathered so [state]'s outer `combine` stays at five arms. */
    private data class Controls(
        val zoom: CalendarZoom,
        val anchor: LocalDate,
        val waking: WakingHours,
        val notice: CalendarNotice?,
    )

    /**
     * A tick of the wall clock, taken **once per emission** rather than read inside the builder.
     *
     * §2.3's states are functions of a `now`, and a `now` read separately by the column builder,
     * the load bar and the carry-forward sweep is three clocks that can straddle a minute boundary.
     * One read, passed down.
     */
    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    private val schedules: kotlinx.coroutines.flow.Flow<List<TaskSchedule>> =
        combine(tasks.observeTasks(), occurrences.observeOccurrences()) { allTasks, stored ->
            val byTask = stored.groupBy { it.taskId }
            allTasks.map { TaskSchedule(task = it, stored = byTask[it.id].orEmpty()) }
        }

    val state: StateFlow<CalendarUiState> = combine(
        schedules,
        goals.observeGoals(),
        lifeAreas.observeLifeAreas(),
        challenges.observeMyChallenges().map { it.map(ChallengeWithStandings::challenge) },
        combine(zoom, anchor, appPreferences.daySchedule, notice) { z, a, day, notice ->
            Controls(zoom = z, anchor = a, waking = day.waking, notice = notice)
        },
    ) { schedules, goals, areas, myChallenges, controls ->
        buildState(schedules, goals, areas, myChallenges, controls)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CalendarUiState(),
    )

    private fun buildState(
        schedules: List<TaskSchedule>,
        allGoals: List<Goal>,
        areas: List<LifeArea>,
        myChallenges: List<Challenge>,
        controls: Controls,
    ): CalendarUiState {
        val (zoom, anchor, waking, notice) = controls
        val today = LocalDate.now(clock)
        return CalendarUiState(
            zoom = zoom,
            anchor = anchor,
            today = today,
            days = CalendarBuilder.build(
                range = CalendarBuilder.daysFor(anchor, zoom),
                today = today,
                now = now(),
                schedules = schedules,
                goals = allGoals,
                lifeAreas = areas,
                challenges = myChallenges,
                // #61's slot. See the class KDoc -- passed, not omitted.
                external = emptyList(),
                waking = waking,
                zone = clock.zone,
            ),
            goals = allGoals.filterNot { it.isArchived },
            isLoading = false,
            notice = notice,
        )
    }

    /** The surface has shown what [notice] held. */
    fun dismissNotice() {
        notice.value = null
    }

    fun setZoom(next: CalendarZoom) {
        zoom.value = next
    }

    /** Steps by a whole screenful, so the columns never half-overlap what was just read. */
    fun shift(screensful: Int) {
        anchor.value = anchor.value.plusDays((screensful * zoom.value.dayCount).toLong())
    }

    fun goToToday() {
        anchor.value = LocalDate.now(clock)
    }

    fun openAt(date: LocalDate) {
        anchor.value = date
    }

    /**
     * §4.3's *create by tapping a slot* — writes the task **and** its occurrence.
     *
     * The occurrence goes on the task itself ([Task.occurrence]) rather than into a document,
     * because §2.1 puts the anchor there and [TaskSchedule] generates from it. A document for a
     * one-off with no series would be the *"26 duplicate documents a year"* shape §2.1 rejects,
     * arriving one at a time.
     */
    fun create(draft: SlotDraft, onDone: (Boolean) -> Unit = {}) {
        if (!draft.isValid) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val task = Task(
                title = draft.title.trim(),
                occurrence = draft.toOccurrence(),
                estimatedMinutes = draft.minutes.takeIf { it > 0 },
                goalEdges = draft.goalId?.let { listOf(GoalEdge(goalId = it)) } ?: emptyList(),
                createdAtEpochMillis = clock.millis(),
            )
            onDone(tasks.upsertTask(task) is Resource.Success)
        }
    }

    /**
     * §4.3's *tick to complete*, for one window.
     *
     * ### Two paths, because §2.1's model has two kinds of instance
     *
     * An instance the user has **touched** already has a document, so its outcome is one field
     * write. A **generated** one has none — that is the whole reason `R18`'s flowers are not 26
     * documents a year — so ticking it is what brings its document into existence, carrying the
     * [ScheduledOccurrence.seriesDate] that lets the series recognise it next time it is expanded.
     *
     * ⚠️ **This banks no points, and that is not an omission.** §1.4's completion fact is a separate
     * document with its own inputs, and points per occurrence is
     * [#64](https://github.com/idomarhaim/Android_Final_Project/issues/64)'s — it needs that
     * collection's key to widen, which is a migration on live data. What is recorded here is *the
     * window was honoured*, which is what §4.7 counts.
     */
    fun setDone(entry: CalendarEntry, done: Boolean, onDone: (Boolean) -> Unit = {}) {
        val taskId = entry.taskId ?: return onDone(false)
        val outcome = if (done) OccurrenceOutcome.Done(clock.millis()) else OccurrenceOutcome.Planned
        viewModelScope.launch {
            val existing = entry.occurrenceId
            val result = if (existing != null) {
                occurrences.setOutcome(existing, outcome)
            } else {
                val task = taskOf(taskId)
                if (task == null) {
                    Resource.Error("Task not found")
                } else {
                    occurrences.apply(
                        SchedulePlan.Writes(
                            task = task,
                            upserts = listOf(
                                ScheduledOccurrence(
                                    taskId = taskId,
                                    occurrence = entry.occurrence,
                                    // Null for a one-off; the rule's own date for a series
                                    // instance, which is what makes a moved instance still
                                    // recognisable as the same instance (Schedule.kt).
                                    seriesDate = seriesDateOf(taskId, entry),
                                    outcome = outcome,
                                ),
                            ),
                        ),
                    )
                }
            }
            onDone(result is Resource.Success)
        }
    }

    /**
     * §4.3's *drag to move*, committed
     * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
     *
     * [target] is where the finger landed, already resolved to a day and a minute by
     * [DragToMove.targetOf] — a pure function of the grid's geometry, tested on the JVM. Turning it
     * into an [Occurrence][com.idomarhaim.goalpilot.domain.model.Occurrence] is
     * [DragToMove.movedTo], which is pure for the same reason. Nothing about *where* is decided
     * here.
     */
    fun move(
        entry: CalendarEntry,
        target: DragToMove.Target,
        scope: EditScope,
        onDone: (Boolean) -> Unit = {},
    ) = edit(entry, ScheduleEdit.MoveTo(DragToMove.movedTo(entry, target)), scope, onDone)

    /**
     * §2.1's *skip*, and the second entry point to the machinery `#63` built.
     *
     * ⚠️ **A skip is not a miss.** [OccurrenceOutcome.Skipped]'s KDoc is the authority: a window the
     * person chose to drop is a decision, it is excluded from `Doneness`' totals and from `#64`'s
     * success/failure run, and counting it against them is §2.3's *"an over-eager agent
     * manufactures failures"* read from the other direction. Nothing here needs to enforce that —
     * it is enforced where the totals are computed — but this is the call site that creates the
     * outcome, so it is where a future reader will come looking.
     */
    fun skip(entry: CalendarEntry, scope: EditScope, onDone: (Boolean) -> Unit = {}) =
        edit(entry, ScheduleEdit.Skip, scope, onDone)

    /**
     * **Compute the plan, then commit it** — the split `#63` designed and this ticket must not
     * undo.
     *
     * `ScheduleEdits.apply` is pure and takes its clock as an argument, so the decision about what
     * an edit implies is testable without a database and is asserted in `DragToMoveTest`. What
     * happens here is only the two things a view model is for: reading the aggregate out of the
     * stream, and handing the result to the repository.
     *
     * ⚠️ **[SchedulePlan.TooLarge] is surfaced and never swallowed.** §0.4 forbids the app being
     * silent about a refusal, and this one is otherwise **invisible**: the plan is not committed,
     * so nothing changes on screen, so the drag reads as a gesture the calendar failed to notice.
     * It already names both numbers ([SchedulePlan.TooLarge.required] and `limit`) and they are
     * carried through to the message rather than reduced to *"could not move"*, because the person
     * cannot act on the refusal without knowing it is about the size of their own history.
     */
    private fun edit(
        entry: CalendarEntry,
        change: ScheduleEdit,
        scope: EditScope,
        onDone: (Boolean) -> Unit,
    ) {
        // A row with no task behind it should never reach here -- `CalendarEntry.isEditable` is
        // false for every non-task kind -- so this is a guard and not a path, and it stays silent.
        val taskId = entry.taskId ?: return onDone(false)
        // This one is NOT unreachable: the task can leave the stream while a sheet is open over its
        // row, and then answering the scope question would do nothing at all. §0.4 forbids that
        // being silent for the same reason `TooLarge` may not be -- nothing on screen changes
        // either way, so silence is indistinguishable from the app ignoring the answer.
        val schedule = scheduleOf(taskId) ?: run {
            notice.value = CalendarNotice.EditFailed
            return onDone(false)
        }
        viewModelScope.launch {
            val plan = ScheduleEdits.apply(
                schedule = schedule,
                seriesDate = MoveScope.seriesDateOf(entry),
                edit = change,
                scope = scope,
                nowEpochMillis = clock.millis(),
            )
            when (plan) {
                is SchedulePlan.TooLarge -> {
                    notice.value = CalendarNotice.TooLarge(required = plan.required, limit = plan.limit)
                    onDone(false)
                }

                is SchedulePlan.Writes -> {
                    val ok = occurrences.apply(plan) is Resource.Success
                    if (!ok) notice.value = CalendarNotice.EditFailed
                    onDone(ok)
                }
            }
        }
    }

    private var cachedSchedules: List<TaskSchedule> = emptyList()

    init {
        viewModelScope.launch { schedules.collect { cachedSchedules = it } }
    }

    private fun scheduleOf(taskId: String): TaskSchedule? = cachedSchedules.firstOrNull { it.task.id == taskId }

    private fun taskOf(taskId: String): Task? = scheduleOf(taskId)?.task

    private fun seriesDateOf(taskId: String, entry: CalendarEntry): LocalDate? =
        if (taskOf(taskId)?.repeatRule == null) null else entry.date

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
