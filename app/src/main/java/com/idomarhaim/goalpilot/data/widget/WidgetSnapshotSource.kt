package com.idomarhaim.goalpilot.data.widget

import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import com.idomarhaim.goalpilot.domain.model.WidgetSnapshot
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetSnapshotUseCase
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocationUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads one snapshot for the widget pack.
 *
 * **The widget pulls; the app does not push.** That was a design decision and
 * not the obvious one, so it is worth stating. Pushing — the app writing a
 * snapshot on every foreground — would have needed a hook in `ui/root/`, which
 * belonged to another session the day this was written (`SESSIONS.md`), but it
 * is also simply worse: a tile that only ever updates when you open the app is
 * a tile that is always stale exactly when you are looking at the home screen
 * instead of the app, which is the entire situation a widget exists for.
 *
 * Pulling works because Firestore keeps a local cache and the repositories serve
 * it: a widget refresh with no network still gets the user's real goals and real
 * week, and the only thing missing is anything changed on another device since.
 * The tile stamps what it got (`WidgetTileContent.asOf`), so that gap is stated
 * rather than hidden.
 *
 * The window is [AnalyticsRange.WEEK] deliberately — the same window the
 * analytics screen defaults to, so the donut on the home screen and the donut in
 * the app are the same donut. A widget with its own window would be a second
 * number that quietly disagrees (§0.3) with nothing to reveal the disagreement.
 */
@Singleton
class WidgetSnapshotSource @Inject constructor(
    private val authRepository: AuthRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val lifeAreaRepository: LifeAreaRepository,
    private val timeAllocation: TimeAllocationUseCase,
    private val buildSnapshot: BuildWidgetSnapshotUseCase,
) {

    /**
     * @param nowEpochMillis stamped onto the snapshot. Passed in rather than read
     *   here so the caller owns the clock and this stays testable at a boundary.
     */
    suspend fun load(nowEpochMillis: Long): WidgetSnapshot = coroutineScope {
        val user = authRepository.authState().first()
        if (user == null) {
            // Not an error and not empty: somebody looked, and nobody was signed
            // in. The tile has a sentence for exactly that.
            return@coroutineScope buildSnapshot(nowEpochMillis, null, emptyList(), TIME_NONE, TREND_NONE)
        }

        // Concurrent because they are four independent reads against the same
        // cache, and a widget refresh is on a broadcast's clock.
        val goalsAsync = async { goalRepository.observeGoals().first() }
        val tasksAsync = async { taskRepository.observeTasks(null).first() }
        val areasAsync = async { lifeAreaRepository.observeLifeAreas(includeArchived = true).first() }

        val goals = goalsAsync.await()
        val tasks = tasksAsync.await()
        // Archived areas included, for the reason TimeAllocationUseCase gives:
        // the time really was spent there, and folding it into "Unassigned" would
        // silently rewrite the user's past.
        val areas = areasAsync.await()

        val today = LocalDate.now()
        val allocation = timeAllocation(
            window = AnalyticsRange.WEEK.window(today = today),
            lifeAreas = areas,
            goals = goals,
            tasks = tasks,
        )
        val trend = timeAllocation.trend(
            buckets = AnalyticsRange.WEEK.buckets(today = today),
            allocation = allocation,
            goals = goals,
            tasks = tasks,
        )

        buildSnapshot(nowEpochMillis, user, goals, allocation, trend)
    }

    private companion object {
        val TIME_NONE = com.idomarhaim.goalpilot.domain.usecase.TimeAllocation()
        val TREND_NONE = com.idomarhaim.goalpilot.domain.usecase.TimeTrend()
    }
}
