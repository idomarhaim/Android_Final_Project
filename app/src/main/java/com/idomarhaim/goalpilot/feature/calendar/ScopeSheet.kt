package com.idomarhaim.goalpilot.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.EditScope
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet

/**
 * **The two windows that make `#63`'s machinery reachable**
 * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
 *
 * `#63` shipped the complete answer to §2.1's *"this occurrence, or all future ones?"* —
 * `ScheduleEdits.apply`, `EditScope`, `ScheduleEdit.MoveTo` / `Skip`, `SchedulePlan` and its
 * `TooLarge` refusal, all pure and all tested — and **nothing in the app could reach any of it**.
 * That is worse than absent: it reviews as done, its tests are green, and nothing fails. This file
 * is the door.
 *
 * ### Two sheets, because the two questions are asked at different moments
 *
 * [EntryActionSheet] is *what do you want to do with this row* — the menu a long press opens, and
 * the only place `Skip` has ever been reachable from. [ScopeSheet] is *and to which instances*, and
 * it appears **only where a rule exists** ([MoveScope.isAsked]). Folding the two into one sheet
 * would put the scope question in front of a one-off, which §2.1's own argument forbids: the
 * question exists because *"a field-only model always answers just this one; a rule-only model
 * always answers all of them"*, and a task with no rule has no second instance for the answers to
 * differ over.
 *
 * ### Both go through [AppModalBottomSheet], not a raw `ModalBottomSheet`
 *
 * §0.8's Hebrew freeze is suspended but `DialogLocaleGuardTest` stays armed and app-wide, for the
 * reason that test spells out: a window opened outside the `ui/locale/` façades renders in the
 * *device* language while mirroring RTL perfectly, so **looking at it does not catch it**. Same
 * habit `SlotSheet` already keeps, one file over.
 */

/** Which verb a [ScopeSheet] is scoping. The two read differently and mean different damage. */
enum class ScopedVerb(val title: String, val thisOne: String, val andFuture: String) {

    /** §4.3's *drag to move*, already dropped somewhere — the sheet decides what it applied to. */
    MOVE(
        title = "Move",
        thisOne = "Only this one",
        andFuture = "This and all future",
    ),

    /**
     * §2.1's *skip* — and note the second label is **not** *"skip all future ones"*.
     *
     * `ScheduleEdits.endSeries` does not mark a run of instances skipped; it **ends the series**,
     * so nothing after this day is generated at all. Labelling it *skip them all* would promise a
     * pile of skipped windows and deliver a rule that stops, which is a different fact about the
     * user's week.
     */
    SKIP(
        title = "Skip",
        thisOne = "Only this one",
        andFuture = "This one, and stop repeating",
    ),
}

/**
 * ***"This occurrence, or all future ones?"*** — §2.1's question, asked.
 *
 * It offers **two** answers and there is deliberately no third. `EditScope`'s own KDoc says why an
 * `ALL` reaching backwards is not among them: §2.3's *"a missed occurrence is never edited — it is
 * history"* and §2.8's *"every destructive effect splits by tense: future events cancel, past
 * events stay"*. Editing what already happened is not a scope this app offers, so it is not a
 * button that can be pressed by accident.
 *
 * The two are drawn as equal-weight outlined buttons rather than as a primary and a secondary. A
 * filled *This and all future* would nominate the more destructive answer as the expected one, on
 * a question whose whole point is that the app does not know which the person meant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeSheet(
    entry: CalendarEntry,
    verb: ScopedVerb,
    onPick: (EditScope) -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag(TAG_SCOPE_SHEET),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${verb.title} “${entry.title}”",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "This task repeats. Which instances?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { onPick(EditScope.THIS_OCCURRENCE) },
                modifier = Modifier.fillMaxWidth().testTag(TAG_SCOPE_THIS),
            ) {
                Text(verb.thisOne, textAlign = TextAlign.Center)
            }
            OutlinedButton(
                onClick = { onPick(EditScope.THIS_AND_FUTURE) },
                modifier = Modifier.fillMaxWidth().testTag(TAG_SCOPE_FUTURE),
            ) {
                Text(verb.andFuture, textAlign = TextAlign.Center)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TAG_SCOPE_CANCEL)) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * What a long press on an entry offers — **§2.1's *skip*, and `#67`'s delete**.
 *
 * ### Why a long press, and why it shares the gesture with the drag
 *
 * The chip has no room for a third control. §4.3's own measurement spends a three-day column
 * (~110 dp) on the leading time column, the title and the tick, and `TimeColumn`'s KDoc already
 * records what happened the one time something on this row was allowed to grow. So one long press
 * serves both verbs and the release decides between them: put back where it started it opens this
 * sheet, carried somewhere else it moves the row ([DragToMove.PRESS_SLOP_PX]).
 *
 * Rows outside the hour grid — banners, the untimed strip, the agenda list — have no geometry to
 * drop onto, so their long press only ever opens this. That asymmetry is deliberate and is stated
 * in [CalendarEntry.isDraggable]; both routes reach the same sheet.
 *
 * ### `Move` is not an item here
 *
 * It would be a second author for the same fact, and a worse one — a *when* picked out of a dialog
 * rather than pointed at on the calendar the person is already looking at. §4.3 asked for a drag.
 *
 * ### `Delete` **is** an item, and it is scoped to the whole task rather than to this window
 *
 * `#67`: every entity needs a delete reachable from where the person is looking at it, and for a
 * dated task this is that place. It is deliberately **not** offered a [ScopeSheet]: the two
 * answers that sheet asks between exist because a *move* and a *skip* can honestly apply to one
 * instance or to the rest of a series, and *"delete only this occurrence"* is not a third such
 * answer — it is `Skip`, one button up, which `OccurrenceOutcome.Skipped` already records as a
 * decision rather than a failure. So `Delete` names the task, and `DeleteConfirm` says how many
 * occurrences go with it and how many of those already happened.
 *
 * It is **absent for a row with no task** — a challenge window, an `EXTERNAL` Google event. Those
 * are drawn here and owned elsewhere, and a delete on one would either do nothing or delete
 * something the person was not looking at.
 *
 * ### `Skip` stays first and `Delete` is last, with a divider between them
 *
 * They are not two strengths of one verb. A skip drops a window and keeps the task; a delete ends
 * the task and takes its record. Putting the destructive one at the bottom, past a rule, is what
 * stops a hurried thumb finding it where `Skip` was a moment ago.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryActionSheet(
    entry: CalendarEntry,
    onSkip: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag(TAG_ACTION_SHEET),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (entry.isRepeating) "Repeats" else "Does not repeat",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().testTag(TAG_ACTION_SKIP),
            ) {
                Text("Skip")
            }
            // A skip is a DECISION, not a failure -- `OccurrenceOutcome.Skipped`'s KDoc, and §2.3's
            // "an over-eager agent manufactures failures" read from the other direction. Saying so
            // here is the only place the person finds out, and it is the difference between
            // dropping a window and admitting defeat.
            Text(
                text = "A skip is a decision, not a miss. It is not counted against you.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (entry.taskId != null) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag(TAG_ACTION_DELETE),
                ) {
                    Text("Delete task")
                }
                // It says *task*, not *this*, because that is what it does -- and the sentence
                // below is the one thing that stops it reading as a stronger Skip. The counts
                // themselves are `DeleteConfirm`'s; this is what makes someone open it.
                Text(
                    text = "Removes the task itself, on every day it appears.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TAG_ACTION_CANCEL)) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Test tags. Named constants rather than literals at the assertion site, for `CalendarScreen`'s
// reason: a renamed tag should break the compile rather than the instrumented run.
const val TAG_SCOPE_SHEET = "calendar_scope_sheet"
const val TAG_SCOPE_THIS = "calendar_scope_this"
const val TAG_SCOPE_FUTURE = "calendar_scope_future"
const val TAG_SCOPE_CANCEL = "calendar_scope_cancel"
const val TAG_ACTION_SHEET = "calendar_action_sheet"
const val TAG_ACTION_SKIP = "calendar_action_skip"
const val TAG_ACTION_DELETE = "calendar_action_delete"
const val TAG_ACTION_CANCEL = "calendar_action_cancel"
const val TAG_NOTICE = "calendar_notice"
