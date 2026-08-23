package com.idomarhaim.goalpilot.domain.model

/**
 * **What a deletion takes, and what it leaves** —
 * [`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67).
 *
 * ### Why this is a domain type and not three sentences in three dialogs
 *
 * §0.4 forbids the app being silent about an irreversible act, so every delete in this app is
 * behind a confirm that states its own consequences. Those consequences are **not the same
 * sentence per entity** — deleting a life area *unfiles its goals and keeps them*, deleting a
 * goal *takes its edges with it*, deleting a task *takes its history with it* — and each of
 * them is a claim about what the repository will actually do. Written as literals inside three
 * dialogs, a claim can drift from the write it describes and nothing notices: the dialog reads
 * perfectly, the English render is right, and the sentence is a lie. Written here it is a
 * function of the same data the repository acts on, and it is JVM-tested (`DeletionReachTest`).
 *
 * That is the same argument `OccurrenceDraft` and `SlotDraft` already make one layer over — *a
 * rule that can only be exercised against a live Firestore is a rule whose branches do not all
 * get tested* — applied to the one part of a deletion that has rules in it.
 *
 * ### Nothing here deletes anything
 *
 * These are pure descriptions. The writes are `GoalRepository.deleteGoal`,
 * `TaskRepository.deleteTask` and `LifeAreaRepository.deleteLifeArea`, all three of which
 * existed before this ticket — `#67`'s gap was **reach**, not capability.
 */
object Deletion {

    /**
     * **The tasks no screen in this app lists** — `#67`'s founding defect.
     *
     * `Observed:` 2026-08-23, mechanically over `app/src/main`. Exactly two surfaces render a
     * task the user can act on. `GoalDetailScreen` reads `observeTasks(goalId)`, so it lists
     * only tasks filed under *that* goal. `CalendarScreen` reads every task but draws one only
     * where [TaskSchedule.occurrencesIn] produces an instance for it, and that function has
     * nothing to expand for a task with neither an anchor nor a stored document. A task in
     * neither set is on no screen at all, and before this ticket it therefore could not be
     * deleted at any point in its life.
     *
     * **It is reachable through the ordinary front door**, which is what makes it a defect and
     * not a curiosity: `DashboardViewModel.classifyForSmartAdd` files a quick-add under
     * `FilingDecision.NoGoal` — no edge — and sets no `occurrence`. The undo snackbar that
     * follows is the only control that has ever been able to remove one, and it lives for a few
     * seconds.
     *
     * ### The goal list is a parameter, and that is the whole of the second case
     *
     * A task can hold an edge to a goal that **no longer exists** — before this ticket
     * `deleteGoal` deleted one document and left every edge pointing at it. Such a task reads
     * as *filed* ([Task.goalEdges] is not empty) while being listed nowhere, which is the same
     * defect wearing a disguise. Resolving edges against the live goals rather than trusting
     * their emptiness is what catches it, and it is why this takes the goal list instead of
     * asking each task about itself.
     *
     * @param tasks every task the user has, as `observeTasks(null)` hands it over.
     * @param goals the goals that exist. An **archived** goal still exists and is still
     *   openable from the goals list, so it counts as reach — archiving is §1.1's reversible
     *   verb and hiding a task behind it would be deleting by omission.
     * @param occurrences every stored document from `users/{uid}/occurrences`. A task with one
     *   is on the calendar even with no anchor of its own, because a document with no
     *   `seriesDate` is a one-off that [TaskSchedule.occurrencesIn] emits on its own date.
     */
    fun unreachableTasks(
        tasks: List<Task>,
        goals: List<Goal>,
        occurrences: List<ScheduledOccurrence> = emptyList(),
    ): List<Task> {
        val liveGoalIds = goals.map { it.id }.filter { it.isNotBlank() }.toSet()
        val scheduledTaskIds = occurrences.map { it.taskId }.filter { it.isNotBlank() }.toSet()
        return tasks.filter { task ->
            val filed = task.goalEdges.any { it.goalId in liveGoalIds }
            val dated = task.occurrence != null || task.id in scheduledTaskIds
            !filed && !dated
        }
    }

    /**
     * What deleting [goal] will do, given every task and how many entries its log holds.
     *
     * [entryCount] defaults to [Goal.loggedEntryCount], which `GoalRepositoryImpl` fills at the
     * repository boundary — so any screen holding a goal has the right number without also
     * holding its entries. A caller that is **rendering** the log passes its own list's size
     * instead, so the dialog and the section behind it are one number rather than two that agree
     * today.
     */
    fun ofGoal(
        goal: Goal,
        tasks: List<Task>,
        entryCount: Int = goal.loggedEntryCount,
    ): DeletionImpact.OfGoal =
        DeletionImpact.OfGoal(
            title = goal.title,
            unfiledTaskCount = tasks.count { task ->
                goal.id.isNotBlank() && task.goalEdges.any { it.goalId == goal.id }
            },
            entryCount = entryCount,
        )

    /**
     * What deleting [task] will do, given the whole occurrence collection.
     *
     * The collection is passed whole rather than pre-filtered for the reason
     * `BuildSuccessFailureRunUseCase` gives: `observeOccurrences` hands it over that way, and a
     * caller that filtered first would be a second place the join could be got wrong.
     */
    fun ofTask(task: Task, occurrences: List<ScheduledOccurrence>): DeletionImpact.OfTask {
        val mine = occurrences.filter { task.id.isNotBlank() && it.taskId == task.id }
        return DeletionImpact.OfTask(
            title = task.title,
            occurrenceCount = mine.size,
            settledOccurrenceCount = mine.count { it.outcome.isSettled },
            // An OPEN task has never been awarded anything -- `Task.points` prices it from its
            // current minutes so the add row can show what it *would* be worth. Reporting that
            // number as something the deletion takes away would name a loss that cannot happen.
            bankedPoints = if (task.isDone) task.points else 0,
        )
    }

    /** What deleting [area] will do, given every goal. */
    fun ofLifeArea(area: LifeArea, goals: List<Goal>): DeletionImpact.OfLifeArea =
        DeletionImpact.OfLifeArea(
            name = area.name,
            unfiledGoalCount = goals.count { goal ->
                area.id.isNotBlank() && area.id in goal.lifeAreaIds
            },
        )
}

/**
 * **One deletion, described in the app's own words** — the input to
 * `ui/components/DeleteConfirm.kt`.
 *
 * Each case carries the counts its own sentences need and no others, which is why this is a
 * sealed hierarchy rather than one record with nullable fields: a life area has no banked
 * points and a task has no goals to unfile, and a shape that admitted both would let a confirm
 * render a count that means nothing for the thing being deleted.
 *
 * ⚠️ **Every count here is a claim about a write.** If a repository stops doing what one of
 * these says, the confirm becomes a lie that nothing in an English render can show — see
 * `DeletionReachTest`, which is where each of them is pinned.
 */
sealed interface DeletionImpact {

    /** What the person is about to delete, for the confirm's heading. */
    val subjectName: String

    /**
     * A goal.
     *
     * **Its tasks survive.** §1.1's *lossless demotion* is the principle — *"the task
     * underneath is real work he typed in"* — so `deleteGoal` strips the goal's edge from each
     * of them and leaves the tasks standing, exactly as `deleteLifeArea` already unfiles the
     * goals filed under an area rather than taking them down with it.
     *
     * **Its progress log does not.** The entries live in `goals/{goalId}/progressEntries` and
     * are read by a fan-out over the goals that exist, so an entry whose goal is gone is
     * reachable by nothing — the same dead storage an orphan occurrence would be. They are
     * deleted with the goal, and this count is what says so out loud.
     */
    data class OfGoal(
        val title: String,
        val unfiledTaskCount: Int,
        val entryCount: Int,
    ) : DeletionImpact {
        override val subjectName: String get() = title
    }

    /**
     * A task — the only one of the three whose deletion reaches **history**.
     *
     * §2.3's *"a missed occurrence is never edited — it is history"* governs the scoped verbs
     * on the calendar: `EditScope` offers no `ALL` that reaches backwards, so no *move* and no
     * *skip* can rewrite what already happened. A **delete** is not one of those verbs. It is
     * the person saying the task should not exist, and half a task — no document, plus a pile
     * of occurrence records nothing can join back to — is not a lesser deletion, it is a leak.
     * So the occurrences go, and [settledOccurrenceCount] is how the confirm says which of them
     * had already happened, before it happens.
     */
    data class OfTask(
        val title: String,
        val occurrenceCount: Int,
        val settledOccurrenceCount: Int,
        val bankedPoints: Int,
    ) : DeletionImpact {
        override val subjectName: String get() = title
    }

    /**
     * A life area.
     *
     * The mildest of the three, and the only one whose behaviour predates this ticket:
     * `deleteLifeArea` already unfiles every goal filed under the area and keeps them, and
     * `LifeAreaDetailScreen`'s own empty state already tells anyone who arrives after the fact.
     * This count moves that sentence to **before** the act, which is where §0.4 wants it.
     */
    data class OfLifeArea(
        val name: String,
        val unfiledGoalCount: Int,
    ) : DeletionImpact {
        override val subjectName: String get() = name
    }
}
