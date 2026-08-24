package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.GoalPlan
import com.idomarhaim.goalpilot.domain.model.PlanStep
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What applying a plan actually did — facts only; the wording lives in the UI layer.
 *
 * [written] and [failed] are counted separately rather than folded into a boolean because
 * §3.7's draft is applied **step by step**: each step is its own `upsertTask`, so a plan can
 * genuinely land half-written and the sheet must be able to say so. Reporting *"it failed"* for
 * a plan where nine of ten steps are now in the user's list would send them looking for work
 * that is already there.
 */
data class GoalPlanOutcome(
    /** Tasks created, each carrying its *when*. */
    val written: Int = 0,
    /** Steps whose write did not land. Their titles are still in the draft. */
    val failed: Int = 0,
    /** How many of the written tasks carry an [com.idomarhaim.goalpilot.domain.model.Occurrence]. */
    val scheduled: Int = 0,
    /** The first error message, when there was one — for the sentence the sheet shows. */
    val message: String? = null,
) {
    val isCompleteSuccess: Boolean get() = failed == 0 && written > 0
}

/**
 * Turns an approved [GoalPlan] into tasks on the goal, each carrying its *when* — §3.7's
 * **commit**, and the only code in the app that writes a step the model proposed
 * ([`#24`](https://github.com/idomarhaim/Android_Final_Project/issues/24), Ido 2026-08-24).
 *
 * ## Every step the user kept, and nothing else
 *
 * §3.7: *"the draft gate is normative, not cosmetic: **nothing the model decides here may reach
 * Firestore without passing his eyes**."* This use case is the far side of that gate. It writes
 * [GoalPlan.kept] — the steps still carrying [PlanStep.keep] when the sheet was confirmed — and
 * has no path to any other step. A dropped step is not written and not remembered.
 *
 * ## The calendar is not a second write, and that is why nothing here knows about Google
 *
 * A step becomes a [Task] with an [com.idomarhaim.goalpilot.domain.model.Occurrence] on it, and
 * that is the whole of the scheduling. Everything downstream already follows:
 *
 * - **The app's calendar** reads `TaskSchedule.occurrencesIn`, whose fourth source is *"the
 *   anchor itself, when the task has neither a rule nor any stored document"* — which is exactly
 *   the shape written here. So a planned step appears on §4.3's calendar surface with no code
 *   in this file addressing it.
 * - **Google Calendar** is [SyncCalendarUseCase]'s, which walks those same occurrences and
 *   mirrors each into the **GoalPilot** calendar it created (§2.6). This use case triggers one
 *   [CalendarSyncTrigger.MANUAL] pass after the writes land so the user does not wait for the
 *   next foreground — and that is the only line here that mentions Google at all.
 *
 * That is deliberate and it is the reason this use case is short: a plan that wrote calendar
 * events *as well as* tasks would be a second writer of the same fact, which §0.3 spends its
 * whole section on. The occurrence **is** the fact; the event is a mirror of it.
 *
 * ## The sync failing is not the plan failing
 *
 * [GoalPlanOutcome] counts writes, and the sync's own outcome is deliberately discarded. A user
 * who never granted the calendar scope gets `NeedsConsent` on every sync, which is §2.6's
 * *ordinary state of a user who has not opted in* and not a fault of this feature — reporting it
 * as a failure of the plan would tell them their tasks were not created when they were.
 */
@Singleton
class ApplyGoalPlanUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val syncCalendar: SyncCalendarUseCase,
) {

    /**
     * Writes the kept steps and asks the calendar to catch up.
     *
     * @param plan the draft as the user left it; only [GoalPlan.kept] is written
     * @param appliedOn the day offsets are resolved against. A **default parameter** rather than
     *   an injected clock, matching [SyncCalendarUseCase] and [SummaryUseCase]: a use case that
     *   reads a hidden clock cannot be tested for the one thing that matters here, which is what
     *   date a step lands on.
     */
    suspend operator fun invoke(
        plan: GoalPlan,
        appliedOn: LocalDate = LocalDate.now(),
    ): GoalPlanOutcome {
        val kept = plan.kept
        if (kept.isEmpty()) return GoalPlanOutcome()

        var written = 0
        var failed = 0
        var scheduled = 0
        var message: String? = null

        for (step in kept) {
            val task = taskFor(step, plan.goalId, appliedOn)
            when (val result = tasks.upsertTask(task)) {
                is Resource.Success -> {
                    written++
                    if (task.occurrence != null) scheduled++
                }
                is Resource.Error -> {
                    failed++
                    if (message == null) message = result.message
                }
                Resource.Loading -> Unit
            }
        }

        // Only when something actually reached the calendar. A sync over zero new occurrences
        // is a network round trip that can only report that nothing changed.
        if (scheduled > 0) syncCalendar(CalendarSyncTrigger.MANUAL)

        return GoalPlanOutcome(
            written = written,
            failed = failed,
            scheduled = scheduled,
            message = message,
        )
    }

    /**
     * One step as the [Task] it becomes. **Pure** — no clock, no repository, no Firestore — so
     * the interesting half of this use case (which rung, which date, what is priced) is testable
     * without any of them.
     *
     * ### `durationSource` is [DurationSource.AI] and must stay that way
     *
     * [DurationSource.USER] is **sticky**: `BackfillDurationsUseCase` filters on this field, and
     * a plan step marked `USER` would be a duration the user never typed, permanently exempt from
     * re-estimation. `AI` is the truth — a model estimated it — and it is safe to re-estimate.
     *
     * ### `source` is [TaskSource.MANUAL], and that is not a lie
     *
     * [TaskSource] answers *"did this come from Google Tasks?"* — it has two values and the other
     * one is `GOOGLE_TASKS`. A planned step did not, so `MANUAL` is the honest answer to the
     * question the enum actually asks. What marks the step as the model's is
     * [DurationSource.AI] above, and — for the goal itself — `DeclaredBy`.
     */
    internal fun taskFor(step: PlanStep, goalId: String, appliedOn: LocalDate): Task = Task(
        title = step.title,
        goalEdges = goalEdgesOf(goalId),
        difficulty = step.difficulty,
        source = TaskSource.MANUAL,
        // Absent when the model did not say, exactly as `scoreTask` leaves it (#9, §3.4).
        // `TaskDuration.minutesOf` supplies the chart's fallback; nothing is invented here.
        estimatedMinutes = step.estimatedMinutes,
        durationSource = if (step.estimatedMinutes != null) {
            DurationSource.AI
        } else {
            DurationSource.UNKNOWN
        },
        occurrence = step.occurrenceOn(appliedOn),
    )
}
