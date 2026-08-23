package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.DeletionImpact
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog

/**
 * **The one confirm every delete in this app goes through** —
 * [`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67).
 *
 * ### What it is for
 *
 * §0.4: *legal, but never silent*. A deletion is irreversible and there is no undo and no trash
 * bin — deliberately, and `#67` says so outright — so the **only** moment the app has to tell
 * the person what is about to happen is before it happens. This states two things and they are
 * different questions: **what goes**, and **what stays**. A dialog that says only the first
 * leaves the person guessing about their own work, and guessing pessimistically: *"delete this
 * life area"* reads as *"and everything in it"* to anyone who has used a computer before, when
 * in fact the goals are kept and unfiled.
 *
 * ### Every sentence is a count, and every count comes from the domain
 *
 * The words are chosen here; the numbers are [DeletionImpact]'s, computed by `Deletion` from
 * the same data the repository is about to write. That split is the point. A confirm whose
 * consequences are literals drifts from the repository silently — the dialog composes, the
 * English render is perfect, and the sentence is false — so the claims live in a JVM-tested
 * domain type (`DeletionReachTest`) and this file renders them.
 *
 * ### One component, three subjects, no `when` at the call site
 *
 * Each screen hands over the [DeletionImpact] for the thing it is looking at and gets the right
 * sentences. That is what makes *"delete anything"* one behaviour rather than six dialogs that
 * drift apart: the goal detail's confirm and the analytics card's `Let it go` are the same
 * dialog with the same counts, because they are the same act.
 *
 * ### Two habits this package owes, and neither is optional here
 *
 * It goes through [AppAlertDialog], not a raw `AlertDialog` — `DialogLocaleGuardTest` is armed
 * app-wide even with §0.8 suspended, because a window opened outside the `ui/locale/` façades
 * renders in the *device* language while mirroring RTL perfectly, which looking at it does not
 * catch. And `ui/components/` is in `AnalyticsLiteralSweepTest.SWEPT_PACKAGES`, so every string
 * below is a resource with a real Hebrew translation, and every count is
 * [bidiIsolated][com.idomarhaim.goalpilot.core.util.bidiIsolated] so a number inside a Hebrew
 * sentence keeps its own direction.
 */
@Composable
fun DeleteConfirm(
    impact: DeletionImpact,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TAG_DELETE_CONFIRM),
        title = {
            Text(
                text = stringResource(
                    R.string.components_delete_title,
                    impact.subjectName.ifBlank { stringResource(R.string.components_delete_untitled) }
                        .bidiIsolated(),
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConsequenceBlock(
                    heading = stringResource(R.string.components_delete_goes),
                    lines = goesLines(impact),
                    tag = TAG_DELETE_GOES,
                )
                val stays = staysLines(impact)
                if (stays.isNotEmpty()) {
                    ConsequenceBlock(
                        heading = stringResource(R.string.components_delete_stays),
                        lines = stays,
                        tag = TAG_DELETE_STAYS,
                    )
                }
                // Last, and on its own. It is the sentence the person is agreeing to, and it
                // belongs after the specifics rather than instead of them -- "this cannot be
                // undone" over an unnamed consequence is a warning with nothing in it.
                Text(
                    text = stringResource(R.string.components_delete_irreversible),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TAG_DELETE_CONFIRM_BUTTON),
            ) {
                Text(
                    text = stringResource(R.string.components_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(TAG_DELETE_CANCEL_BUTTON)) {
                Text(stringResource(R.string.components_delete_cancel))
            }
        },
    )
}

/**
 * One heading and its bullets.
 *
 * Drawn as plain stacked lines rather than as a bulleted list, because the two blocks are read
 * against each other and a glyph column between them adds a second thing to scan on a dialog
 * whose whole job is to be read once, quickly, under a decision.
 */
@Composable
private fun ConsequenceBlock(heading: String, lines: List<String>, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag(tag)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        lines.forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * **What goes** — always at least one line, because something is always being deleted.
 *
 * The subject itself is first and is named as what it is (*this goal*, *this task*), not
 * repeated by title: the title is already in the heading a centimetre above, and saying it
 * twice pushes the counts below the fold on a small screen.
 */
@Composable
private fun goesLines(impact: DeletionImpact): List<String> = buildList {
    when (impact) {
        is DeletionImpact.OfGoal -> {
            add(stringResource(R.string.components_delete_goal_itself))
            if (impact.entryCount > 0) {
                add(
                    pluralStringResource(
                        R.plurals.components_delete_goal_entries,
                        impact.entryCount,
                        "${impact.entryCount}".bidiIsolated(),
                    ),
                )
            }
        }

        is DeletionImpact.OfTask -> {
            add(stringResource(R.string.components_delete_task_itself))
            if (impact.occurrenceCount > 0) {
                add(
                    pluralStringResource(
                        R.plurals.components_delete_task_occurrences,
                        impact.occurrenceCount,
                        "${impact.occurrenceCount}".bidiIsolated(),
                    ),
                )
            }
            // §2.3 keeps the past out of every *scoped* verb -- no move and no skip may reach
            // backwards. A delete does reach it, so the count of windows that already happened
            // is said out loud rather than folded into the number above, which would let the
            // person read a series of future plans and lose a month of record.
            if (impact.settledOccurrenceCount > 0) {
                add(
                    pluralStringResource(
                        R.plurals.components_delete_task_settled,
                        impact.settledOccurrenceCount,
                        "${impact.settledOccurrenceCount}".bidiIsolated(),
                    ),
                )
            }
            if (impact.bankedPoints > 0) {
                add(
                    pluralStringResource(
                        R.plurals.components_delete_task_points,
                        impact.bankedPoints,
                        "${impact.bankedPoints}".bidiIsolated(),
                    ),
                )
            }
        }

        is DeletionImpact.OfLifeArea -> add(stringResource(R.string.components_delete_area_itself))
    }
}

/**
 * **What stays** — often empty, and then the block is absent rather than reassuring.
 *
 * An empty *"what stays"* heading over nothing would be the worst of both: it implies something
 * survives and names none of it. A goal with no tasks filed under it simply has no second
 * block, and the dialog is shorter.
 */
@Composable
private fun staysLines(impact: DeletionImpact): List<String> = buildList {
    when (impact) {
        is DeletionImpact.OfGoal ->
            if (impact.unfiledTaskCount > 0) {
                add(
                    pluralStringResource(
                        R.plurals.components_delete_goal_tasks_kept,
                        impact.unfiledTaskCount,
                        "${impact.unfiledTaskCount}".bidiIsolated(),
                    ),
                )
            }

        // Nothing survives a task. Its occurrences and its banked completion are the whole of
        // what belonged to it, and both are named in the block above.
        is DeletionImpact.OfTask -> Unit

        is DeletionImpact.OfLifeArea ->
            if (impact.unfiledGoalCount > 0) {
                add(
                    pluralStringResource(
                        R.plurals.components_delete_area_goals_kept,
                        impact.unfiledGoalCount,
                        "${impact.unfiledGoalCount}".bidiIsolated(),
                    ),
                )
            }
    }
}

// Test tags. Named constants rather than literals at the assertion site, for `ScopeSheet`'s
// reason: a renamed tag should break the compile rather than the instrumented run.
//
// ⚠️ **camelCase, not snake_case, and that is not a style choice here.** `ui/components/` is a
// SWEPT package, and `AnalyticsLiteralSweepTest` calls any literal with two or more alphabetic
// words "prose" — which `"delete_confirm_cancel"` is, once the underscores are stripped. The
// first draft of this file used snake_case and the guard failed the build, correctly: it cannot
// tell a tag from a sentence, and the rule is crude on purpose (its own KDoc says so). Every
// other tag in this package is camelCase for the same reason.
const val TAG_DELETE_CONFIRM = "deleteConfirm"
const val TAG_DELETE_GOES = "deleteConfirmGoes"
const val TAG_DELETE_STAYS = "deleteConfirmStays"
const val TAG_DELETE_CONFIRM_BUTTON = "deleteConfirmDelete"
const val TAG_DELETE_CANCEL_BUTTON = "deleteConfirmCancel"
