package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One miss, as the daily review shows it: the task, and **which kind of miss** (§2.2).
 *
 * The state travels with the task rather than being re-derived by the screen, because the
 * screen would need a clock to do it and would then be able to disagree with the list it was
 * given — two answers to one question, one frame apart.
 */
data class MissedOccurrence(
    val task: Task,
    val occurrence: Occurrence,
    val state: OccurrenceState,
) {
    /** §2.3: the passed deadline is the one that is still owed, and the one that keeps asking. */
    val stillOwed: Boolean get() = state.keepsReminding
}

/**
 * §2.5's **daily review**: *"Misses meet Ido once, in a daily review on app open — never as a
 * push saying he failed."*
 *
 * ### The whole design is in that one sentence, and each clause is load-bearing
 *
 * - **"meet"** — they are shown, not counted. Every miss here names a task the user can open.
 * - **"once"** — a miss that has already been through a review does not come back. That is
 *   [since] below, and it is why this takes a boundary rather than filtering on *is it past*.
 * - **"in a daily review on app open"** — the trigger is opening the app. There is no schedule,
 *   no worker and nothing to arm; a user who does not open the app is told nothing, which is
 *   the correct behaviour and not a gap.
 * - **"never as a push saying he failed"** — nothing in this file touches `GoalPilotNotifier`,
 *   and `androidTest/.../DailyMissReviewUiTest` asserts the shade stays empty while a review is
 *   on screen. That assertion is the clause; a comment saying *we do not post here* is not.
 *
 * ### `OVERDUE` is exempt from "once", because §2.3 says so
 *
 * *"`OVERDUE` … the one state that keeps reminding"* — a passed deadline is **late and still
 * owed**, so it is still true tomorrow and belongs in tomorrow's review too. A missed block is
 * not: its slot is gone, there is nothing left to do at the time it was about, and repeating it
 * would be the *"push saying he failed"* in a different costume.
 *
 * ### Pure, and takes its clock as an argument
 *
 * Nothing here reads `LocalDateTime.now()`. §2.3's derivation is a function of the occurrence
 * and an instant, so every case below — including *"the review at 00:01 must not re-show what
 * closed at 23:59"* — is a JVM test rather than a device and a wait.
 */
object DailyMissReview {

    /**
     * The misses to put in front of the user.
     *
     * @param since when the last review was shown, or `null` if there has never been one. A
     *   miss whose window closed **before** this has already been met once and is left out —
     *   unless [OccurrenceState.keepsReminding], which is `OVERDUE` and only `OVERDUE`.
     */
    fun of(
        tasks: List<Task>,
        now: LocalDateTime,
        since: LocalDateTime? = null,
    ): List<MissedOccurrence> =
        tasks.mapNotNull { task ->
            // A completed task has no miss to report, whatever its dates say. This is the
            // same question §2.5's fire-time re-check asks, and it must be asked here too:
            // the review is a second surface onto the same fact, and a task ticked after the
            // deadline passed is a task that was DONE, not one that was missed.
            if (task.isDone) return@mapNotNull null
            val occurrence = task.occurrence ?: return@mapNotNull null
            val state = occurrence.stateAt(now)
            if (!state.meetsUserInDailyReview) return@mapNotNull null
            val alreadyMet = since != null && occurrence.closesAt.isBefore(since)
            if (alreadyMet && !state.keepsReminding) return@mapNotNull null
            MissedOccurrence(task = task, occurrence = occurrence, state = state)
        }
            // Most recently closed first: the thing that lapsed last night is the thing the
            // user still has a decision to make about, and a review that opens on a window
            // that closed in March buries it.
            .sortedByDescending { it.occurrence.closesAt }

    /**
     * Whether to show a review at all, given when the last one was shown.
     *
     * **Daily, by calendar day and not by elapsed hours.** An app opened at 23:50 and again at
     * 00:10 is on its second day and gets its second review; one opened twice in an afternoon
     * gets one. Hours since would make the boundary depend on when the user happened to look,
     * which is exactly the thing *"once, on app open"* is trying not to be.
     */
    fun isDue(lastShownOn: LocalDate?, today: LocalDate): Boolean = lastShownOn != today
}
