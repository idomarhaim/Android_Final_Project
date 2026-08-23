package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.Doneness
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.startDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * How far back the run reports — `docs/PRODUCT_v0.3.md` §4.7's `30 days · 8 weeks · 6 months`
 * ([`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64)).
 *
 * ### It is a filter over history, never decay of it
 *
 * §4.7: *"Nothing ages out. History is permanent, and the view reports over a **window you
 * pick**… A window is a **filter over history, not decay of it**."* So nothing here is stored,
 * nothing expires, and changing the selection re-reads the same records over a different span.
 * `C5` §2 refused any value that moves on wall-clock time, and *"failures older than N weeks
 * stop counting"* is that value wearing a different hat.
 *
 * ### The windows roll from today; they are not calendar weeks or calendar months
 *
 * Window `k` back from today is `[today - (k+1)·unit + 1 day, today - k·unit]`, so the newest
 * window always ends today. The alternative — snapping to the region's week start, or to the
 * first of the month — makes the newest window a partial one whose length changes daily, and
 * makes the whole run shift under the user when they change region. `RepeatRule.datesFrom`
 * already refused the same dependency for the same reason: *"`AppRegion`'s week start governs
 * how a **calendar is drawn**; it has no business deciding when a commitment recurs."*
 */
enum class SuccessRange(
    /** How many windows the run draws. */
    val windowCount: Int,
) {
    THIRTY_DAYS(windowCount = 30),
    EIGHT_WEEKS(windowCount = 8),
    SIX_MONTHS(windowCount = 6),
    ;

    /**
     * The windows, **oldest first**, the last of them ending on [today].
     *
     * Each is a closed day range: `from` and `to` are both inside it, which is what makes
     * [SuccessRange.THIRTY_DAYS]'s windows exactly one day long rather than zero.
     */
    fun windowsEndingOn(today: LocalDate): List<ClosedRange<LocalDate>> =
        (windowCount - 1 downTo 0).map { back ->
            val end = endOfWindow(today, back)
            val start = endOfWindow(today, back + 1).plusDays(1)
            start..end
        }

    private fun endOfWindow(today: LocalDate, back: Int): LocalDate = when (this) {
        THIRTY_DAYS -> today.minusDays(back.toLong())
        EIGHT_WEEKS -> today.minusWeeks(back.toLong())
        SIX_MONTHS -> today.minusMonths(back.toLong())
    }

    companion object {
        /** §4.7: *"default **8 weeks**"*. */
        val DEFAULT: SuccessRange = EIGHT_WEEKS
    }
}

/**
 * What happened in **one window** — §4.7's five states, minus the one that is a property of a
 * goal rather than of a window (see [NoNextStepGoal]).
 *
 * ⚠️ **Never carried by hue.** §4.7: *"Outcome state never rides on hue either"* — kept is
 * filled, missed is hollow, still-owed is dashed with a centre pip, nothing-due is dotted, and
 * **there is no red on this screen at all**. The drawing is `ui/components/SuccessFailureRun.kt`'s;
 * what matters here is that this enum is the whole vocabulary the drawing has, so a sixth
 * outcome cannot be introduced by a colour.
 */
enum class WindowOutcome {

    /** §4.7's on-screen sentence: *everything due in it was done*. */
    KEPT,

    /** Something due in it was missed. The only state that counts against the user. */
    MISSED,

    /**
     * Nothing was missed, but something is **still owed** — `OVERDUE`, and §2.3 is explicit
     * that it is *"NOT a failure"*.
     *
     * §4.7's material note calls this *"the one state that must NOT read as a failure"*, which
     * is why it is its own constant rather than a flavour of [MISSED].
     */
    STILL_OWED,

    /**
     * Nothing was due in it at all.
     *
     * Counted in **neither** number: §4.7's headline is *"a goal with nothing due is missing a
     * **step**, not failing"*, and a window with nothing in it is the same sentence one level
     * down. An `EXPIRED` occurrence lands here too — §2.3 says it *"counts for nothing"*, and
     * a window holding only those held nothing anybody endorsed.
     */
    NOTHING_DUE,
}

/**
 * One window of the run.
 *
 * [dueCount] and [keptCount] travel with the outcome rather than being re-derivable: a caller
 * that wanted them would need the occurrences **and** a clock, and could then disagree with the
 * row it was handed — `MissedOccurrence` carries its own state for that reason, and §0.3's
 * *second number that quietly disagrees* is the defect in miniature.
 *
 * ⚠️ `Observed:` **the shipped component reads neither**, and that is deliberate rather than an
 * oversight. The obvious use would be a per-window *"3 of 4 done"*, and §4.7 forbids it — that
 * is the rate it refuses, computed per window instead of per screen, and
 * `SuccessFailureRunUiTest` asserts the string `" of "` appears nowhere on the card. They are
 * kept because they are the only way a caller can tell a window holding one commitment from one
 * holding twelve, which is a fact about the record and not about the drawing.
 */
data class SuccessWindow(
    val from: LocalDate,
    /** Inclusive — the last day inside the window. */
    val to: LocalDate,
    val outcome: WindowOutcome,
    /** Occurrences that closed inside it and counted for something. */
    val dueCount: Int,
    /** How many of those were done. */
    val keptCount: Int,
)

/**
 * Which step is actually missing — §4.7's table, and **not** a dormancy state.
 *
 * §4.7: *"There is no dormancy state, stored or named. Asleep · invisible · failed were three
 * labels for `C10`'s already-decided theme axis, whose `STARTING` value **is** 'never
 * scheduled'."* So nothing here is stored and nothing is called dormant; the goal is offered
 * the step it does not have.
 */
enum class NextStepOffer {

    /** `open work = 0` — `C8`'s feature, and §4.7 marks it **no new AI surface**. */
    BREAK_IT_INTO_STEPS,

    /** Work exists with no dates — `C9a`'s. */
    SCHEDULE_THE_FIRST_ONE,
}

/**
 * A goal with **no next step**, and the offer that fits it.
 *
 * ⚠️ **`Let it go` is not here, and its absence is the decision.** §4.7: *"`Let it go` stays a
 * command, never an inference — `C4` forbids the app asserting an intrinsic edge by itself."*
 * A command needs a user to issue it; an offer this type carried would be the app proposing
 * that a goal is over, which is exactly the inference `C4` refuses.
 */
data class NoNextStepGoal(
    val goalId: String,
    val title: String,
    val offer: NextStepOffer,
    /**
     * Days since anything happened on this goal — a completion, or a window that closed — or
     * `null` when nothing ever has.
     *
     * §4.7's row reads *"`no next step · idle 4 months`"*, and `null` is what makes the second
     * half of that sentence omissible rather than printed as `idle 0 days`, which would claim
     * activity that never happened.
     */
    val idleDays: Long?,
)

/**
 * **`C19`'s success/failure run** — `docs/PRODUCT_v0.3.md` §4.7, `#64`, built against the
 * prototype at `docs/prototypes/2026-08-13-area-success-failure/` (revision 5).
 *
 * > **A failure is a missed *window* and nothing else; a goal with nothing due is missing a
 * > *step*, not failing.**
 *
 * ### Two numbers, and they are a tally of the run itself
 *
 * §4.7: *"**Two numbers, never a rate.** A single 'success rate' is the tidying-away `C3` and
 * `C5` both refused."* There is no ratio anywhere in this file and no property that could be
 * mistaken for one — not a percentage, not a fraction, not a `keptOutOf`.
 *
 * [kept] and [missed] are **counts over [windows]**, so the pair and the run cannot disagree.
 * That is the prototype's round-5 defect made unrepresentable rather than fixed: *"Learning
 * showed 11 kept / 7 missed above a list with one active goal and two asleep. The numbers
 * contradicted the list they sat on top of."* Two independently-computed tallies is §0.3's
 * *second number that quietly disagrees*, and the cheapest way not to ship one is to have only
 * one number.
 *
 * ### There is no lifetime counter here, and there is no place to put one
 *
 * §4.7: *"There is no lifetime failure counter anywhere, because a number that can only rise
 * is the **list of the things you are bad at** this screen exists **not** to be."* Every count
 * on this object is derived from [windows], and [windows] is bounded by the [range] the user
 * picked — so an unbounded total is not merely absent, it has nothing to be computed from.
 */
data class SuccessFailureRun(
    val range: SuccessRange,
    /** Oldest first, so the row reads left-to-right as time passing. */
    val windows: List<SuccessWindow> = emptyList(),
    /** §4.7's table, one entry per goal that has no next step. */
    val noNextStep: List<NoNextStepGoal> = emptyList(),
) {

    /** Windows in which everything due was done. */
    val kept: Int get() = windows.count { it.outcome == WindowOutcome.KEPT }

    /** Windows in which something due was missed. */
    val missed: Int get() = windows.count { it.outcome == WindowOutcome.MISSED }

    /** Windows still owing something — §2.3's `OVERDUE`, and not a failure. */
    val stillOwed: Int get() = windows.count { it.outcome == WindowOutcome.STILL_OWED }

    /** Windows in which nothing was due. Counted in neither number above. */
    val nothingDue: Int get() = windows.count { it.outcome == WindowOutcome.NOTHING_DUE }

    /**
     * Whether anything at all was ever due in this span.
     *
     * The screen shows the pair and the run only when this is true. A run of nothing but
     * dotted windows above `0 kept · 0 missed` says nothing and looks like a verdict, which is
     * the tone §4.7 spends its whole length avoiding.
     */
    val hasRecord: Boolean get() = windows.any { it.outcome != WindowOutcome.NOTHING_DUE }
}

/**
 * Builds §4.7's run from what the repositories already hand every screen.
 *
 * ### Pure, and it takes its clock as an argument
 *
 * Nothing here reads `LocalDate.now()`. §2.3's temporal derivation is a function of an
 * occurrence and an instant, so every rule below — *a missed block counts, a passed deadline
 * does not, an unconfirmed block counts for nothing* — is a JVM test rather than a device and
 * a wait. `DailyMissReview` is the sibling this is modelled on and it gives the same reason.
 *
 * ### It knows nothing about life areas, and that is what makes `C17`'s asymmetry true
 *
 * §4.7: *"A success counts in **full** in every area the task serves, while its **minutes
 * divide**."* This function takes **the goals it should count** and nothing else, so the
 * life-area screen hands it one area's goals and the analytics screen hands it all of them.
 * A task serving two areas is therefore counted whole under each, by construction — there is
 * no share to divide because there is no arithmetic that could divide one.
 * `TimeAllocationUseCase` is the half that *does* divide, and it divides minutes.
 */
object BuildSuccessFailureRunUseCase {

    /**
     * @param goals the goals to count — already filtered to whatever *"here"* means to the
     *   caller. Also the population [SuccessFailureRun.noNextStep] is drawn from.
     * @param tasks every task the user has; the ones filed under [goals] are selected here.
     * @param occurrences the stored documents from `users/{uid}/occurrences` — the whole
     *   collection, as `OccurrenceRepository.observeOccurrences` hands it over.
     * @param now the instant §2.3's states are derived against.
     * @param zone the zone `Task.pausedUntil` is compared in — passed rather than defaulted,
     *   for the reason `TaskSchedule.occurrencesIn` gives.
     */
    operator fun invoke(
        goals: List<Goal>,
        tasks: List<Task>,
        occurrences: List<ScheduledOccurrence>,
        range: SuccessRange = SuccessRange.DEFAULT,
        today: LocalDate,
        now: LocalDateTime,
        zone: ZoneId,
    ): SuccessFailureRun {
        val goalIds = goals.map { it.id }.filter { it.isNotBlank() }.toSet()
        val storedByTask = occurrences.groupBy { it.taskId }
        val schedules = tasks
            .filter { task -> task.goalEdges.any { it.goalId in goalIds } }
            .map { task -> TaskSchedule(task = task, stored = storedByTask[task.id].orEmpty()) }

        val windows = range.windowsEndingOn(today)
        val first = windows.first().start
        val last = windows.last().endInclusive

        // Expanded once over the whole span rather than once per window. `occurrencesIn` is a
        // filter on the start date, so bucketing its output by that same date gives the
        // identical partition -- and expanding a daily rule 30 times over 30 windows is 30
        // times the work for it.
        val outcomes: List<Pair<LocalDate, WindowOutcome>> = schedules.flatMap { schedule ->
            val storedDoneness = schedule.doneness
            schedule.occurrencesIn(from = first, to = last, zone = zone).mapNotNull { instance ->
                outcomeOf(instance, storedDoneness, now)?.let { instance.occurrence.startDate to it }
            }
        }
        val byDate = outcomes.groupBy({ it.first }, { it.second })

        return SuccessFailureRun(
            range = range,
            windows = windows.map { window ->
                // Bucketed by the day the occurrence OPENS, which is how `occurrencesIn`
                // selects. A span straddling two windows therefore belongs to the one it
                // started in -- the same identity `ScheduledOccurrence.seriesDate` uses, so a
                // moved instance cannot be counted twice or lost between two windows.
                val inWindow = generateSequence(window.start) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(window.endInclusive) }
                    .flatMap { byDate[it].orEmpty().asSequence() }
                    .toList()
                SuccessWindow(
                    from = window.start,
                    to = window.endInclusive,
                    outcome = outcomeOfWindow(inWindow),
                    dueCount = inWindow.size,
                    keptCount = inWindow.count { it == WindowOutcome.KEPT },
                )
            },
            noNextStep = goals.mapNotNull { goal ->
                noNextStepFor(goal, schedules, today, now, zone)
            },
        )
    }

    /**
     * What one occurrence contributes, or `null` for *nothing at all*.
     *
     * ### The two `null`s are different sentences, and both are deliberate
     *
     * A **skipped** window and an **`EXPIRED`** one count for nothing because §2.1 and §2.3 say
     * so — *"a skip is not a miss"*, and an unconfirmed block that lapsed *"counts for nothing,
     * silently"*. A **`SCHEDULED`** or **`UNDERWAY`** one counts for nothing because it has not
     * happened yet: the newest window is today's and is still being written, and judging a
     * window on work that is not yet due would mark today missed every morning.
     *
     * ### Why this does not read `OccurrenceState.countsAsFailure`
     *
     * That property answers *"is this state a failure?"* with **`MISSED` alone**, and its own
     * KDoc names this surface as the reader it was written for. It is the wrong instrument
     * here, and using it would have been the expensive kind of wrong:
     *
     * - §2.2 gives four rungs four meanings of a miss, and `#56` gave two of them their own
     *   names — `DAY_PASSED` and `WINDOW_CLOSED` — precisely because they are **not** *"a block
     *   whose slot has gone"*. §2.3's three-word vocabulary predates that split, so *"`MISSED`
     *   is a failure"* is a sentence about **the block rung**, not about the other three.
     * - `OccurrenceState.meetsUserInDailyReview` already treats all four as misses, and
     *   `DailyMissReview` already shows them to the user as misses. A run that excluded two of
     *   them would say `0 missed` about windows the daily review had just named — §0.3's
     *   *second number that quietly disagrees*, manufactured by this file.
     * - `OccurrenceDraft.toOccurrence` can only produce `ALL_DAY` and `DEADLINE`: there is no
     *   way to **type** a block yet. So `countsAsFailure` yields a missed count that is
     *   structurally **always zero** in the app as it ships, and the component's whole subject
     *   is two numbers.
     *
     * So the three words are honoured as §4.7 states them — `MISSED` counts, `OVERDUE` is
     * [WindowOutcome.STILL_OWED] and not a failure, `EXPIRED` counts for nothing — and the two
     * names `#56` added take the meaning of the rung they belong to. `countsAsFailure` is left
     * exactly as it is, along with the whole-enum test that pins it: it is not wrong, it is
     * answering a narrower question than this one.
     */
    private fun outcomeOf(
        instance: ScheduledOccurrence,
        doneness: Doneness,
        now: LocalDateTime,
    ): WindowOutcome? = when (instance.outcome) {
        is OccurrenceOutcome.Skipped -> null
        is OccurrenceOutcome.Done -> WindowOutcome.KEPT
        // A task with no occurrence documents and no rule is §7.1's *stored* leg, and the
        // completion fact under `completionFacts/{taskId}` is the answer for it -- which is
        // every task in the database written before `#63`. Deliberately narrow: on a task that
        // HAS documents or a rule, `Doneness` is `Derived` or `Unanswerable` and this branch
        // does not fire, so one legacy flag can never mark a whole series kept.
        OccurrenceOutcome.Planned ->
            if (doneness is Doneness.Stored && doneness.done) {
                WindowOutcome.KEPT
            } else {
                when (instance.occurrence.stateAt(now)) {
                    OccurrenceState.SCHEDULED, OccurrenceState.UNDERWAY -> null
                    OccurrenceState.EXPIRED -> null
                    OccurrenceState.OVERDUE -> WindowOutcome.STILL_OWED
                    OccurrenceState.MISSED,
                    OccurrenceState.DAY_PASSED,
                    OccurrenceState.WINDOW_CLOSED,
                    -> WindowOutcome.MISSED
                }
            }
    }

    /**
     * §4.7's on-screen sentence, as code: *a window counts as kept when **everything** due in
     * it was done.*
     *
     * One miss makes the window missed however much else was kept — that is what *everything*
     * means, and softening it to a majority would be the rate §4.7 refuses, computed per window
     * instead of per screen.
     */
    private fun outcomeOfWindow(outcomes: List<WindowOutcome>): WindowOutcome = when {
        outcomes.isEmpty() -> WindowOutcome.NOTHING_DUE
        outcomes.any { it == WindowOutcome.MISSED } -> WindowOutcome.MISSED
        outcomes.any { it == WindowOutcome.STILL_OWED } -> WindowOutcome.STILL_OWED
        else -> WindowOutcome.KEPT
    }

    /**
     * §4.7's table for one goal, or `null` when it has a next step.
     *
     * *"Having a next step"* is **open work with a date still ahead of it**, which is the only
     * reading under which the two offers differ: a goal whose every date is in the past is not
     * scheduled, it *was*, and offering to *schedule the first one* is the honest thing to say
     * about it.
     */
    private fun noNextStepFor(
        goal: Goal,
        schedules: List<TaskSchedule>,
        today: LocalDate,
        now: LocalDateTime,
        zone: ZoneId,
    ): NoNextStepGoal? {
        val mine = schedules.filter { s -> s.task.goalEdges.any { it.goalId == goal.id } }
        val open = mine.filter { it.doneness.isDone != true }
        if (open.isEmpty()) {
            return NoNextStepGoal(
                goalId = goal.id,
                title = goal.title,
                offer = NextStepOffer.BREAK_IT_INTO_STEPS,
                idleDays = idleDaysFor(mine, today, now, zone),
            )
        }
        // A year ahead: enough to see a yearly rule's next instance, and bounded because
        // `RepeatRule.datesFrom` is a lazy infinite sequence and *"every caller states its own
        // bound"*.
        val scheduledAhead = open.any { schedule ->
            schedule.occurrencesIn(from = today, to = today.plusYears(1), zone = zone)
                .any { it.outcome !is OccurrenceOutcome.Skipped }
        }
        if (scheduledAhead) return null
        return NoNextStepGoal(
            goalId = goal.id,
            title = goal.title,
            offer = NextStepOffer.SCHEDULE_THE_FIRST_ONE,
            idleDays = idleDaysFor(mine, today, now, zone),
        )
    }

    /**
     * Days since the last thing that happened on this goal — a completion, or a window that
     * closed — or `null` when nothing ever has.
     *
     * Looks back two years rather than over all time. An unbounded look-back on an unbounded
     * rule does not terminate, and the sentence this feeds (*"idle 4 months"*) stops carrying
     * information long before two years anyway.
     */
    private fun idleDaysFor(
        schedules: List<TaskSchedule>,
        today: LocalDate,
        now: LocalDateTime,
        zone: ZoneId,
    ): Long? {
        val completions = schedules.mapNotNull { it.task.completedAtEpochMillis }
            .map { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val closed = schedules.flatMap { schedule ->
            schedule.occurrencesIn(from = today.minusYears(2), to = today, zone = zone)
                .filter { it.occurrence.stateAt(now).isPast }
                .map { it.occurrence.closesAt.toLocalDate() }
        }
        val last = (completions + closed).maxOrNull() ?: return null
        return ChronoUnit.DAYS.between(last, today).coerceAtLeast(0)
    }
}
