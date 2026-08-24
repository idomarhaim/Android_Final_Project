package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.PlanStep
import com.idomarhaim.goalpilot.domain.model.PlanStepKind
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * §3.7's **draft gate**, as a sheet — `C8` [#24], and Ido's ask of 2026-08-24: *"give me a
 * suggested work plan with tasks and timings, and if I approve them they go into the goal and
 * onto the calendar."*
 *
 * ## This composable **is** the gate, and it holds no logic
 *
 * §3.7: *"the draft gate is normative, not cosmetic: nothing the model decides here may reach
 * Firestore without passing his eyes."* Everything on screen is a proposal; the only writes in
 * this feature are behind the confirm button, in
 * [com.idomarhaim.goalpilot.domain.usecase.ApplyGoalPlanUseCase]. Which step is kept, what date
 * it lands on and what it is priced at are all decided elsewhere and JVM-tested there — this file
 * renders them and reports taps.
 *
 * ## Two exits per step today, not §3.7's three
 *
 * §3.7 specifies **keep · already-done · delete**, where *already-done* is *"evidence flowing
 * backwards into the next plan"*. What ships here is **keep · drop**, because *already-done* only
 * earns its keep alongside the persisted draft and `Adjust Plan` that §3.7 also specifies and
 * that are not built: with nothing carrying evidence forward, an *already-done* mark would be a
 * third button that does exactly what *drop* does. It is named here rather than quietly omitted,
 * and it is the rest of `#24`.
 *
 * ## Hebrew is owed for this screen as a whole, not for this file
 *
 * ⚠️ Every string below is hard-coded English, which matches `AddEditGoalScreen` around it —
 * that screen has no `stringResource` call in it at all. §0.8 (*"not finished until seen in
 * Hebrew"*) and §5.1 are therefore owed for **the goal form**, and adding a localised sheet
 * inside an unlocalised screen would make the screen half-translate in a way that reads as a bug.
 * Recorded in this session's changelog rather than left for someone to discover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalPlanSheet(
    state: PlanState,
    goalTitle: String,
    onToggleStep: (Int) -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    /**
     * The day offsets are rendered against.
     *
     * The same value [com.idomarhaim.goalpilot.domain.usecase.ApplyGoalPlanUseCase] will resolve
     * them with, and a parameter for the same reason: a sheet that reads its own clock can show
     * *"Tue 26 Aug"* for a step that is then written to a different day, and nothing on screen
     * would say so.
     */
    today: LocalDate = LocalDate.now(),
) {
    if (state is PlanState.Idle) return

    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("A plan for \"$goalTitle\"", style = MaterialTheme.typography.titleLarge)

            when (state) {
                PlanState.Idle -> Unit

                PlanState.Loading -> {
                    Text(
                        "Working out a plan…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                }

                // Two different sentences on purpose (§0.4). The model answering "nothing"
                // is not the same event as nothing answering, and only one of them is worth
                // retrying — so only one of them offers a retry.
                PlanState.Empty -> {
                    Text(
                        "No plan this time — the goal is already concrete enough that a " +
                            "breakdown would not add anything. You can add steps yourself " +
                            "from the goal's own screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }

                is PlanState.Failed -> {
                    Text(
                        "The plan could not be fetched. Your goal is saved — this is only " +
                            "the suggestion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.message?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                            Text("Try again")
                        }
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Not now")
                        }
                    }
                }

                is PlanState.Draft -> DraftBody(
                    state = state,
                    today = today,
                    onToggleStep = onToggleStep,
                    onApply = onApply,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun DraftBody(
    state: PlanState.Draft,
    today: LocalDate,
    onToggleStep: (Int) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val kept = state.plan.kept.size
    Text(
        "Untick anything you do not want. What is left becomes tasks on this goal, on the " +
            "dates below — and on your calendar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .testTag(TAG_STEPS),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(state.plan.steps, key = { it.index }) { step ->
            StepRow(step = step, today = today, onToggle = { onToggleStep(step.index) })
            HorizontalDivider()
        }
    }

    // Only ever shown when a write was attempted and did not fully land. A partial write is
    // reported with its numbers rather than as a failure: the steps that landed are real, and
    // telling the user the plan failed would send them looking for tasks that already exist.
    state.outcome?.let { outcome ->
        Text(
            "${outcome.written} of ${outcome.written + outcome.failed} steps were added. " +
                (outcome.message ?: "The rest did not save — try again."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }

    // §3.3 B's `changeNotes`. Empty on a first proposal, which is every proposal today --
    // `Adjust Plan` is the half of #24 that is not built. Rendered anyway because the field is
    // on the wire and a note arriving with nowhere to show is worse than an unused branch.
    state.plan.changeNotes.forEach {
        Text(
            "• $it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onApply,
            enabled = !state.isApplying && kept > 0,
            modifier = Modifier
                .weight(1f)
                .testTag(TAG_APPLY),
        ) {
            if (state.isApplying) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
            } else {
                Text(if (kept == 1) "Add 1 step" else "Add $kept steps")
            }
        }
        TextButton(
            onClick = onDismiss,
            enabled = !state.isApplying,
            modifier = Modifier.weight(1f),
        ) {
            Text("Skip")
        }
    }
}

/**
 * One proposed step: what it is, when it falls, and what it costs.
 *
 * A dropped step is struck through rather than removed, so unticking is visibly reversible —
 * the list must not shuffle under a finger that is still deciding.
 */
@Composable
private fun StepRow(step: PlanStep, today: LocalDate, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = step.keep, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                step.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (step.keep) null else TextDecoration.LineThrough,
                color = if (step.keep) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                step.subtitle(today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The line under a step: **when**, then what kind of thing it is, then how long it takes.
 *
 * *"No date"* is written out rather than left blank, and *"Milestone"* is named rather than shown
 * as a missing duration. Both are §1.3's rule about absence applied to a sentence: a step with no
 * date is not a step with a broken date, and a milestone is not a task the model forgot to price
 * — `C18`'s container rule says it can never have one.
 */
private fun PlanStep.subtitle(today: LocalDate): String {
    val parts = mutableListOf<String>()
    parts += dayOffset?.let { offset ->
        val date = today.plusDays(offset.toLong())
        val day = date.format(DATE_FORMAT)
        timeOfDay?.let { "$day, $it" } ?: day
    } ?: "No date"
    parts += when (kind) {
        PlanStepKind.MILESTONE -> "Milestone"
        // The duration is what the time chart reads and what §1.4 prices the step on, so it is
        // named when it exists. Absent means the model did not say (#9) -- the app asks later
        // rather than inventing one here.
        PlanStepKind.WORK -> estimatedMinutes?.let { "$it min" } ?: "Duration not set"
    }
    return parts.joinToString(" · ")
}

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

/** Test tags, so the instrumented pass can reach the gate without matching on English. */
const val TAG_STEPS = "goal_plan_steps"
const val TAG_APPLY = "goal_plan_apply"
