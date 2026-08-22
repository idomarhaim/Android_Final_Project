package com.idomarhaim.goalpilot.domain.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * **What happened to one window** — the per-occurrence outcome §2.8 asks for and §4.7 counts
 * ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * ### It is not a temporal state, and it must not become one
 *
 * §2.3 is categorical that temporal state is **derived, never stored**: `SCHEDULED`,
 * `UNDERWAY`, `MISSED`, `OVERDUE`, `EXPIRED` and the rest are all functions of an
 * [Occurrence] and a clock ([Occurrence.stateAt]), and *"if you find yourself writing one, the
 * thing you actually want is a different `Occurrence`"*. Nothing here overlaps that list. What
 * *is* stored is the two things the clock cannot tell you: whether the person **did** it, and
 * whether they deliberately **did not**.
 *
 * ### One object, not a flag beside a stamp
 *
 * `done: Boolean` beside `doneAt: Long?` is the shape `#55` deleted twice over and
 * [CompletionFact]'s KDoc names — *"the half-written fact `TaskCompletion` used to normalise —
 * done with no stamp, or a stamp with no done — cannot be constructed"*. Same argument, and it
 * applies harder here because a **third** state exists: a `skippedAt` beside a `doneAt` admits
 * an occurrence that was both.
 */
sealed interface OccurrenceOutcome {

    /** Nothing has happened to it yet. What every generated instance starts as. */
    data object Planned : OccurrenceOutcome

    /**
     * It was done, at this instant.
     *
     * ⚠️ **This is not a [CompletionFact] and it banks no points.** §1.4's fact carries the
     * *inputs* — the minutes and the difficulty as they stood at the tick — because points must
     * not be re-priced by a later edit, and it lives one per task at
     * `users/{uid}/completionFacts/{taskId}`. Points **per occurrence** is a real question and
     * it is [`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64)'s: it needs
     * that collection's key to widen, which is a migration on live data and is not this
     * ticket's. What this constant records is *the window was honoured*, which is what §4.7
     * counts and what a miss is counted against.
     */
    data class Done(val atEpochMillis: Long) : OccurrenceOutcome

    /**
     * It was deliberately not done — §2.1's *"a skip"*, the thing a rule-only model cannot
     * hold.
     *
     * **A skip is not a miss.** §2.3 marks a lapsed endorsed block a failure; a window the
     * person chose to drop is a decision, and counting it against them is the same defect as
     * §2.3's *"an over-eager agent manufactures failures"* from the other direction.
     */
    data class Skipped(val atEpochMillis: Long) : OccurrenceOutcome

    /** Whether anything has happened to this window yet. */
    val isSettled: Boolean get() = this !is Planned
}

/**
 * **One document in `users/{uid}/occurrences`** — §2.1's occurrence, stored.
 *
 * ### Flat, and what that word does and does not mean
 *
 * §2.1: *"Occurrences are **flat, not nested**: a `SPAN` does not contain its blocks."* The
 * collection is flat in the other sense too, and for a separate reason: it sits at
 * `users/{uid}/occurrences` rather than under `users/{uid}/tasks/{taskId}/occurrences`, so
 * §4.3's calendar surface can read *a date range across every task* as one query instead of a
 * collection-group query, and so `firestore.rules`' existing `users/{uid}/{document=**}` owner
 * rule covers it with no change (the same reason life areas needed none). [taskId] is a field.
 *
 * ### [seriesDate] is the whole recurrence mechanism
 *
 * A document exists for an instance the user **touched** — moved it, skipped it, did it, or
 * gave it a Google event id. Every other instance is generated from [Task.repeatRule] and
 * costs nothing. [seriesDate] says *which* generated instance this document stands in for: it
 * is the date the rule would have produced, **not** the date the occurrence now sits on, which
 * is exactly what lets a moved instance still be recognised as the same instance next time the
 * series is expanded. `null` means this document is not part of a series at all.
 *
 * That one field is the difference between *"this occurrence"* and *"all future ones"* being
 * answerable and not — see [ScheduleEdits].
 */
data class ScheduledOccurrence(
    val id: String = "",
    val taskId: String = "",
    /** The *when*, as one of §2.2's four rungs. Possibly moved away from [seriesDate]. */
    val occurrence: Occurrence,
    /**
     * The rule-generated date this document overrides, or `null` for an instance that belongs
     * to no series. See the class KDoc — this is not [Occurrence.startDate] once moved.
     */
    val seriesDate: LocalDate? = null,
    val outcome: OccurrenceOutcome = OccurrenceOutcome.Planned,
    /**
     * The Google Calendar event mirroring this occurrence, or `null` for one that has never
     * reached Google — [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61),
     * §2.6.
     *
     * It lives here rather than on the task because §2.7 syncs **per event**: a repeating task
     * is many events, a disappearance *"keeps its date, clears its `googleEventId`"* for one
     * instance and not the rest, and a rung change is cancel-and-recreate, which replaces this
     * id. None of those sentences can be said about a field on the task.
     */
    val googleEventId: String? = null,
) {
    /** Whether this document stands in for a generated instance of a series. */
    val isSeriesInstance: Boolean get() = seriesDate != null
}

/**
 * **The three ways `isDone` splits** — `docs/PRODUCT_v0.3.md` §7.1, and the migration's sharp
 * edge (`#63`).
 *
 * §7.1's row reads, verbatim:
 *
 * > `isDone` — **splits three ways** — stored with no occurrences, **derived** with them,
 * > **absent** on a recurring task
 *
 * Three *sources*, not three booleans, which is why this is a sealed type and not `Boolean?`:
 * the answer and where it came from are one fact, and a bare null cannot say which of *nobody
 * ticked it* and *the question does not apply* it means.
 *
 * ⚠️ **[Task.isDone] is unchanged and is still a `Boolean`.** It is the [Stored] leg, which is
 * correct for every task in the database today — none has an occurrence document, because this
 * collection is new. Widening that property would have rewritten every screen that reads it
 * for a case none of them can yet be in.
 */
sealed interface Doneness {

    /** No occurrences: the completion fact under `completionFacts/{taskId}` is the answer. */
    data class Stored(val done: Boolean) : Doneness

    /**
     * A finite set of occurrences: done when every window that was meant to happen has.
     *
     * [total] excludes skipped windows — a skip is a decision not to do it, so counting it as
     * outstanding would leave a task permanently unfinished for having been pruned. A task
     * whose every instance was skipped has `total == 0` and is **not** done: nothing was
     * completed, and reporting completion for it would be inventing an achievement.
     */
    data class Derived(val completed: Int, val total: Int) : Doneness {
        val done: Boolean get() = total > 0 && completed == total
    }

    /**
     * A series with no last instance — §7.1's *"absent on a recurring task"*.
     *
     * ⚠️ `Inferred:` §7.1 says *recurring*, and this constant fires on **unbounded**
     * recurrence alone. A rule that ends — `every Monday for 10 weeks`, `until 1 September` —
     * is recurring and *does* have a complete set of windows, so it has an answer, and
     * returning [Unanswerable] for it would be inventing an absence §0.4 forbids. The reading
     * resolves toward the thing the sentence is protecting: an infinite series cannot be
     * finished, so no boolean about it is true. Same move `#56` made for §2.3's missing two
     * miss names, and recorded here for the same reason.
     */
    data object Unanswerable : Doneness

    /**
     * The answer, or `null` where there is none — the one thing most callers want.
     *
     * Named for [Task.isDone], which it is the three-way form of, and deliberately **not**
     * `done`: [Stored] and [Derived] each carry a `done` of their own, and a supertype property
     * of the same name would shadow them. Reading `.isDone` therefore always gets the nullable
     * answer and `.done` always gets a leaf's certain one, which is the distinction worth
     * keeping visible at a call site.
     */
    val isDone: Boolean?
        get() = when (this) {
            is Stored -> done
            is Derived -> done
            is Unanswerable -> null
        }
}

/**
 * **A task and its stored occurrences** — the aggregate every scheduling question is asked of
 * (`#63`).
 *
 * §2.1 puts the rule on the task and the instances in their own collection, so neither half can
 * answer anything alone: the rule does not know which instance was moved, and the documents do
 * not know what comes next. This pairs them, and every function below is pure — no clock it
 * did not ask for, no Firestore, no zone it invented.
 */
data class TaskSchedule(
    val task: Task,
    /** The documents under `users/{uid}/occurrences` whose `taskId` is this task's. */
    val stored: List<ScheduledOccurrence> = emptyList(),
) {

    /** The template every generated instance is a copy of — §2.2's rung, `#56`'s four fields. */
    val anchor: Occurrence? get() = task.occurrence

    /** The first day of the series, or `null` for a task with no *when* at all. */
    val anchorDate: LocalDate? get() = anchor?.startDate

    private val exceptions: Map<LocalDate, ScheduledOccurrence>
        get() = stored.mapNotNull { o -> o.seriesDate?.let { it to o } }.toMap()

    /**
     * **Everything that happens between [from] and [to] inclusive**, stored documents winning
     * over generated instances.
     *
     * ### Four sources, in this order, and the order is the decision
     *
     * 1. **Generated** from [Task.repeatRule], anchored at [anchorDate] — free, unstored, and
     *    the reason `R18`'s fortnightly flowers are not 26 documents a year.
     * 2. **Overridden** by a document with the matching [ScheduledOccurrence.seriesDate]. A
     *    moved instance therefore leaves the day the rule put it on and appears on the day the
     *    user moved it to — including moving *into* this range from outside it, which is why
     *    stored documents are swept as well as generated ones.
     * 3. **One-offs** — documents with no `seriesDate`, which belong to no series.
     * 4. **The anchor itself**, when the task has neither a rule nor any stored document. This
     *    is `#56`'s shipped shape and it is what makes day one read identically: every task in
     *    the database today is exactly this case, and it produces exactly the one occurrence
     *    the task already had.
     *
     * ### [Task.pausedUntil] suppresses generated instances and never stored ones
     *
     * A pause is a statement about **what is still to come**. §2.3: *"a missed occurrence is
     * never edited — it is history"*, and a stored document is a record that something was
     * done, skipped or moved. Hiding those because the task is paused would delete evidence
     * §4.7 counts, so the pause reaches the free instances only.
     *
     * @param zone the zone [Task.pausedUntil] — an instant, per §7.1 — is compared in. Passed
     *   rather than defaulted, because a *day* read in the wrong zone is the exact defect
     *   `TaskDto.occurrenceStart`'s KDoc exists to prevent, and a hidden `systemDefault()`
     *   would put it back where nobody could see it.
     */
    fun occurrencesIn(from: LocalDate, to: LocalDate, zone: ZoneId): List<ScheduledOccurrence> {
        val inRange = { o: ScheduledOccurrence ->
            val d = o.occurrence.startDate
            !d.isBefore(from) && !d.isAfter(to)
        }
        val byDate = exceptions
        val generated = generatedDates(upTo = to)
            .filter { !it.isBefore(from) }
            .mapNotNull { date ->
                byDate[date] ?: instanceOn(date)?.takeUnless { suppressedByPause(it, zone) }
            }
        val generatedDates = generated.mapNotNull { it.seriesDate }.toSet()
        // Documents whose instance was moved into this range from a series date outside it,
        // plus every one-off. `distinctBy` is not needed: the two halves cannot overlap,
        // because the first is filtered on a series date this range's generation did not
        // produce and the second has no series date at all.
        val moved = stored.filter { o ->
            inRange(o) && (o.seriesDate == null || o.seriesDate !in generatedDates)
        }
        val all = (generated.filter(inRange) + moved)
        return all.sortedBy { it.occurrence.opensAt }
    }

    /**
     * §7.1's three-way `isDone`, decided by the shape of the schedule rather than by a caller.
     *
     * The branch order matters: an **unbounded** rule is checked first, because such a task can
     * also have stored occurrences and *those* would otherwise derive a confident answer about
     * a series that has no end. See [Doneness.Unanswerable] for the reading of *"recurring"*
     * this takes and why.
     */
    val doneness: Doneness
        get() {
            val rule = task.repeatRule
            return when {
                rule != null && rule.isUnbounded -> Doneness.Unanswerable
                rule != null -> derivedOver(fullSeries())
                stored.isNotEmpty() -> derivedOver(stored)
                else -> Doneness.Stored(task.isDone)
            }
        }

    /**
     * The whole series, for a bounded rule — every generated instance with its override.
     *
     * Safe to materialise **only** because [doneness] checks [RepeatRule.isUnbounded] first;
     * called on an unbounded rule this would not return.
     */
    private fun fullSeries(): List<ScheduledOccurrence> {
        val anchorAt = anchorDate ?: return stored
        val rule = task.repeatRule ?: return stored
        val byDate = exceptions
        // `instanceOn` is null only when there is no anchor, and `anchorAt` above is that
        // anchor -- so `mapNotNull` drops nothing here. It is `mapNotNull` rather than `!!`
        // because inventing a rung for a missing anchor is exactly what `Mappers.occurrence()`
        // refuses to do, and this file should not be the one place that does.
        return rule.datesFrom(anchorAt).mapNotNull { date ->
            byDate[date] ?: instanceOn(date)
        }.toList() + stored.filter { it.seriesDate == null }
    }

    private fun derivedOver(all: List<ScheduledOccurrence>): Doneness.Derived {
        val counted = all.filterNot { it.outcome is OccurrenceOutcome.Skipped }
        return Doneness.Derived(
            completed = counted.count { it.outcome is OccurrenceOutcome.Done },
            total = counted.size,
        )
    }

    /** The rule's dates up to [upTo], or the anchor alone when there is no rule. */
    internal fun generatedDates(upTo: LocalDate): List<LocalDate> {
        val anchorAt = anchorDate ?: return emptyList()
        val rule = task.repeatRule
            ?: return if (stored.isEmpty() && !anchorAt.isAfter(upTo)) listOf(anchorAt) else emptyList()
        return rule.datesUpTo(anchorAt, upTo)
    }

    /** The generated instance for [date] — the anchor moved there, with no stored override. */
    internal fun instanceOn(date: LocalDate): ScheduledOccurrence? {
        val template = anchor ?: return null
        return ScheduledOccurrence(
            taskId = task.id,
            occurrence = template.onDate(date),
            seriesDate = if (task.repeatRule == null) null else date,
        )
    }

    private fun suppressedByPause(o: ScheduledOccurrence, zone: ZoneId): Boolean {
        val until = task.pausedUntil ?: return false
        return o.occurrence.opensAt.atZone(zone).toInstant().toEpochMilli() < until
    }
}

/**
 * ***"This occurrence, or all future ones?"*** — §2.1's question, as a type (`#63`).
 *
 * ### Two answers, and there is deliberately no third
 *
 * §2.1 asks it in exactly these two, and the shape exists to make both expressible: *"a
 * field-only model always answers just this one; a rule-only model always answers all of
 * them."* An `ALL` that reached backwards is not among them, and §2.3 says why — *"a missed
 * occurrence is never edited — it is history"*, and *"every destructive effect splits by tense:
 * future events cancel, past events stay"* (§2.8). Editing what already happened is not a scope
 * this app offers.
 */
enum class EditScope {
    /** Change this one instance, leaving the rule and every other instance alone. */
    THIS_OCCURRENCE,

    /** Change this instance and everything the rule would produce after it. */
    THIS_AND_FUTURE,
}

/** What is being done to the occurrence [EditScope] scopes. */
sealed interface ScheduleEdit {

    /** Give it a different *when* — a different day, time, rung or duration. */
    data class MoveTo(val occurrence: Occurrence) : ScheduleEdit

    /** Drop it — §2.1's *"a skip"*. Not a miss; see [OccurrenceOutcome.Skipped]. */
    data object Skip : ScheduleEdit
}

/**
 * The writes an edit implies — **computed, then applied**, so the decision is testable without
 * a database.
 */
sealed interface SchedulePlan {

    /**
     * The task as it should be afterwards, plus the occurrence documents to write and remove.
     *
     * `OccurrenceRepository.apply` commits all of it in one `WriteBatch`, which is why
     * [writeCount] is checked before the plan is built rather than discovered at commit time.
     */
    data class Writes(
        val task: Task,
        val upserts: List<ScheduledOccurrence> = emptyList(),
        val deletes: List<String> = emptyList(),
    ) : SchedulePlan {
        /** Documents this plan touches, the task included. Firestore counts it the same way. */
        val writeCount: Int get() = upserts.size + deletes.size + 1
    }

    /**
     * The plan does not fit in one batch — **legal, but never silent** (§0.4).
     *
     * Reachable from one edit only: [EditScope.THIS_AND_FUTURE] with [ScheduleEdit.MoveTo] on a
     * series that has been running long enough that writing its past down exceeds Firestore's
     * limit — a daily task moved after a year and a half. The alternatives were to chunk the
     * write (losing the single failure mode that makes the plan safe) or to truncate it
     * (losing history, silently). Refusing says which occurrence is the problem and leaves the
     * data untouched.
     */
    data class TooLarge(val required: Int, val limit: Int) : SchedulePlan
}

/**
 * **Where the rule and the documents meet** — §2.1's question, answered (`#63`).
 *
 * Pure: give it a schedule, a target instance, an edit and a scope, and it returns the writes.
 * No clock it did not ask for, no Firestore, no ids it invented — a new document's id is blank
 * and the repository fills it, exactly as `TaskRepositoryImpl.upsertTask` already does.
 */
object ScheduleEdits {

    /**
     * Firestore's hard limit on operations in one `WriteBatch`. Named after the thing it is,
     * because a plan that exceeds it fails at commit time with an error that names neither the
     * task nor the edit.
     */
    const val MAX_BATCH_WRITES: Int = 500

    /**
     * The writes [edit] implies for the instance whose series date is [seriesDate], under
     * [scope].
     *
     * ### `THIS_OCCURRENCE` writes one document and never touches the rule
     *
     * A move stores the new *when* against the same [ScheduledOccurrence.seriesDate], so the
     * next expansion still recognises it as this instance; a skip stores
     * [OccurrenceOutcome.Skipped] against it. Anything already on that document — an outcome, a
     * `googleEventId` — is carried across rather than reset, because the instance is the same
     * instance.
     *
     * ### `THIS_AND_FUTURE` is two different operations, and only one of them is obvious
     *
     * A **skip** ends the series: the rule's end becomes the day before [seriesDate], and every
     * stored document from that date on is deleted. Skipping from the very first instance
     * removes the rule and the *when* together — a series with no instances is not a series.
     *
     * A **move** cannot simply re-anchor, and that is the subtle half. The rule generates from
     * [Task.occurrence], so moving the anchor moves the **past** as well, and §2.3 forbids
     * that: *"a missed occurrence is never edited — it is history"*. So the past is written
     * down first — one document per already-generated instance before [seriesDate], skipping
     * any the user already touched — and only then does the anchor move. Past instances keep
     * the day they actually fell on; the future follows the new *when*.
     *
     * [RepeatEnd.AfterCount] is decremented by exactly the number materialised, or the moved
     * series would silently grant itself a full new count. That is the branch worth a test.
     */
    fun apply(
        schedule: TaskSchedule,
        seriesDate: LocalDate,
        edit: ScheduleEdit,
        scope: EditScope,
        nowEpochMillis: Long,
    ): SchedulePlan {
        val task = schedule.task
        val existing = schedule.stored.firstOrNull { it.seriesDate == seriesDate }
            ?: schedule.instanceOn(seriesDate)
            ?: return SchedulePlan.Writes(task = task)

        return when (scope) {
            EditScope.THIS_OCCURRENCE -> when (edit) {
                is ScheduleEdit.MoveTo -> SchedulePlan.Writes(
                    task = task,
                    upserts = listOf(existing.copy(occurrence = edit.occurrence)),
                )

                ScheduleEdit.Skip -> SchedulePlan.Writes(
                    task = task,
                    upserts = listOf(
                        existing.copy(outcome = OccurrenceOutcome.Skipped(nowEpochMillis)),
                    ),
                )
            }

            EditScope.THIS_AND_FUTURE -> when (edit) {
                ScheduleEdit.Skip -> endSeries(schedule, seriesDate, nowEpochMillis)
                is ScheduleEdit.MoveTo -> moveSeries(schedule, seriesDate, edit.occurrence)
            }
        }
    }

    /**
     * The series stops before [seriesDate]: the rule ends the day before, and every stored
     * document at or after that date goes.
     *
     * A task with no rule degenerates to the same thing — a single instance skipped from here
     * on is a single instance skipped — so it takes the `THIS_OCCURRENCE` path's write rather
     * than a second special case.
     */
    private fun endSeries(
        schedule: TaskSchedule,
        seriesDate: LocalDate,
        nowEpochMillis: Long,
    ): SchedulePlan {
        val task = schedule.task
        val rule = task.repeatRule ?: return SchedulePlan.Writes(
            task = task,
            upserts = listOfNotNull(
                schedule.stored.firstOrNull { it.seriesDate == seriesDate }
                    ?: schedule.instanceOn(seriesDate),
            ).map { it.copy(outcome = OccurrenceOutcome.Skipped(nowEpochMillis)) },
        )
        val doomed = schedule.stored
            .filter { it.seriesDate != null && !it.seriesDate.isBefore(seriesDate) }
            .map { it.id }
            .filter { it.isNotBlank() }
        val anchorAt = schedule.anchorDate
        // Skipping from the first instance leaves a rule with nothing to generate. Rather than
        // store `end = the day before the start` -- a rule that is legal, inert and reads as a
        // bug -- the when goes with it.
        val ended = if (anchorAt == null || !seriesDate.isAfter(anchorAt)) {
            task.copy(occurrence = null, repeatRule = null)
        } else {
            task.copy(repeatRule = rule.copy(end = RepeatEnd.OnDate(seriesDate.minusDays(1))))
        }
        return SchedulePlan.Writes(task = ended, deletes = doomed)
    }

    /** See [apply] — the past is written down before the anchor moves. */
    private fun moveSeries(
        schedule: TaskSchedule,
        seriesDate: LocalDate,
        moved: Occurrence,
    ): SchedulePlan {
        val task = schedule.task
        val rule = task.repeatRule
            ?: return SchedulePlan.Writes(task = task.copy(occurrence = moved))

        val touched = schedule.stored.mapNotNull { it.seriesDate }.toSet()
        val past = schedule.generatedDates(upTo = seriesDate.minusDays(1))
            .filter { it.isBefore(seriesDate) }
        val materialised = past
            .filterNot { it in touched }
            .mapNotNull { schedule.instanceOn(it) }

        val doomed = schedule.stored
            .filter { it.seriesDate != null && !it.seriesDate.isBefore(seriesDate) }
            .map { it.id }
            .filter { it.isNotBlank() }

        val required = materialised.size + doomed.size + 1
        if (required > MAX_BATCH_WRITES) {
            return SchedulePlan.TooLarge(required = required, limit = MAX_BATCH_WRITES)
        }

        // The count is spent by what has already happened. Without this, "every day for 30
        // days", moved on day 20, would run for 30 more.
        val spent = past.size
        val end = when (val e = rule.end) {
            is RepeatEnd.AfterCount -> RepeatEnd.AfterCount(e.count - spent)
            else -> e
        }
        return SchedulePlan.Writes(
            task = task.copy(occurrence = moved, repeatRule = rule.copy(end = end)),
            upserts = materialised,
            deletes = doomed,
        )
    }
}
