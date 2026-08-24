package com.idomarhaim.goalpilot.feature.challenges

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ChallengeStanding
import com.idomarhaim.goalpilot.feature.goals.label
import com.idomarhaim.goalpilot.ui.components.Avatar
import com.idomarhaim.goalpilot.ui.components.FreshnessNote
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppDatePickerDialog
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet

// ── Create ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengeEditorDialog(
    state: ChallengeEditorState,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onMeasureKind: (MeasureKind) -> Unit,
    onMeasureWord: (String) -> Unit,
    onStart: (Long?) -> Unit,
    onEnd: (Long?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var picking by remember { mutableStateOf<DateField?>(null) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New challenge") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitle,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescription,
                    label = { Text("What are you competing on?") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                // §6: a challenge names a MEASURE, and it is not optional — "there is
                // nothing to compare without a shared unit". The closed kind is what a
                // participant's goal is matched against; the word beside it is theirs.
                // This is the same two-field shape the goal editor uses, deliberately:
                // a challenge and a goal are the same object measured the same way.
                Text(
                    "What does it count?",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeasureKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kind == state.measureKind,
                            onClick = { onMeasureKind(kind) },
                            label = { Text(kind.label()) },
                            leadingIcon = { MeasureIconBadge(kind) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.measureWord,
                    onValueChange = onMeasureWord,
                    label = { Text("Measured in") },
                    singleLine = true,
                    // Deliberately not "points". §6 deletes that default rather than
                    // re-homing it: points rank by TIME LOGGED, and that is the wrong
                    // race for anything about an outcome.
                    supportingText = {
                        Text("Everyone's score is in this unit — steps, km, hours…")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Each person scores this from one of their own goals, so it moves " +
                        "on its own — from Health Connect, from tasks, from anything " +
                        "that already logs progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Text(
                    "Dates (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Text(
                    "Leave both empty for an open-ended challenge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton(
                        label = "Starts",
                        epochMillis = state.startAtEpochMillis,
                        onPick = { picking = DateField.START },
                        onClear = { onStart(null) },
                        modifier = Modifier.weight(1f),
                    )
                    DateButton(
                        label = "Ends",
                        // The stored bound is exclusive, so the day the user chose
                        // is the millisecond before it — never the bound itself.
                        epochMillis = state.endAtEpochMillis.takeIf { it > 0L }?.minus(1) ?: 0L,
                        onPick = { picking = DateField.END },
                        onClear = { onEnd(null) },
                        modifier = Modifier.weight(1f),
                    )
                }

                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Creating…" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )

    picking?.let { field ->
        val pickerState = rememberDatePickerState()
        AppDatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utc ->
                            when (field) {
                                DateField.START -> onStart(localStartOfPickedDay(utc))
                                DateField.END -> onEnd(exclusiveEndOfPickedDay(utc))
                            }
                        }
                        picking = null
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class DateField { START, END }

@Composable
private fun DateButton(
    label: String,
    epochMillis: Long,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val set = epochMillis > 0L
    OutlinedButton(
        onClick = { if (set) onClear() else onPick() },
        modifier = modifier.semantics {
            this.contentDescription =
                if (set) "$label ${DateTimeUtils.formatDay(epochMillis)}, tap to clear"
                else "Set the $label date"
        },
    ) {
        Text(
            text = if (set) DateTimeUtils.formatDay(epochMillis) else label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Report a score ───────────────────────────────────────────────────

@Composable
internal fun ScoreEntryDialog(
    state: ScoreEntryState,
    onValue: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report your score") },
        text = {
            Column {
                Text(
                    state.challengeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.value,
                    onValueChange = onValue,
                    label = { Text("Total ${state.metricWord}") },
                    singleLine = true,
                    isError = state.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "This replaces your total, it does not add to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                // Said BEFORE the write, not after it. A linked challenge is scoring
                // itself; replacing that with a typed number is a real decision, and
                // discovering it afterwards from a badge on your own row would be the
                // app having made it for you.
                if (state.replacesLink) {
                    Text(
                        "This challenge is currently scoring itself from one of your " +
                            "goals. Typing a number takes it off that goal, and your " +
                            "row will say the score was reported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )
}

// ── Standings ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StandingsSheet(card: ChallengeCard, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text(card.challenge.title, style = MaterialTheme.typography.titleLarge)
            if (card.challenge.description.isNotBlank()) {
                Text(
                    card.challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                if (card.challenge.isUnmeasured) {
                    // A pre-§6 challenge whose `metricUnit` was the "points" default.
                    // §6 deletes that rather than re-homing it, so the honest caption
                    // says the measure is missing instead of inventing a unit.
                    "${participantSummary(card.participantCount)} · no measure set"
                } else {
                    "${participantSummary(card.participantCount)} · scored in " +
                        card.challenge.metricWord
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            // The as-of caption, unconditional — drawn the same online and offline,
            // because a standings row fetched forty minutes ago over perfect Wi-Fi
            // is exactly as stale as one served from cache (#50, spec §5.3 §2).
            // Suppressed only when there is no stamp to state; see [FreshnessNote].
            if (card.standingsFreshness.hasStamp) {
                FreshnessNote(
                    "Standings as of " +
                        DateTimeUtils.formatAsOf(card.standingsFreshness.asOfEpochMillis),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            // Never fetched on this device: the participants are somebody else's
            // documents, so an empty cached read means the app does not know whether
            // anyone is in this challenge — not that nobody is (#50, spec §5.3 §3).
            if (card.standingsFreshness.neverLoaded) {
                FreshnessNote("Not loaded yet", modifier = Modifier.padding(vertical = 16.dp))
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(card.standings, key = { it.uid }) { standing ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Ranks arrive already stamped, with ties sharing one —
                        // 1, 1, 3. Nothing here re-derives them from position.
                        Text(
                            "#${standing.rank}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(width = 38.dp, height = 24.dp),
                        )
                        Avatar(
                            photoUrl = standing.photoUrl,
                            name = standing.displayName.ifBlank { "?" },
                            size = 32.dp,
                        )
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                text = standing.displayName.ifBlank { "Someone" } +
                                    if (standing.isCurrentUser) " (you)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (standing.isCurrentUser) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            ReportedBadge(standing, card.challenge.metricWord)
                        }
                        Text(
                            ChallengesViewModel.format(standing.score),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The provenance line under a standings row — Ido's third ask on `#23`, 2026-08-24:
 *
 * > *"If someone updated manually and it was not updated through HEALTH CONNECT, then it
 * > should say there who performed the update and what they updated."*
 *
 * **It draws nothing at all unless the score was typed**, and that way round is the whole
 * design. A challenge is *meant* to score itself from each participant's goal (§6), so a
 * derived row is the ordinary case and a badge on it would be noise on every row; a row
 * nobody has scored yet has nothing to say either. Only the exception speaks.
 *
 * ⚠️ **The register is factual and never accusatory.** It says *what happened* — this person
 * typed this number on this day — and stops. `C4`'s rule is the one to match: the app never
 * asserts an intrinsic edge by itself, and it must not here either, because this is a claim
 * about *another user* rendered to everyone in the challenge. Two things follow:
 *
 *  * no icon that reads as a warning, no error colour, no "unverified" — the row is drawn in
 *    `onSurfaceVariant` like every other secondary caption on this screen;
 *  * the number is **not re-ranked**. A typed score sorts exactly where its value puts it.
 *
 * And what it is **not**: proof. A participant writes only their own row, so this is
 * self-asserted by construction — see `ChallengeParticipant`'s KDoc. It is a **label on a
 * number**, not an attestation about a person. §6's own honest residual is that server-owned
 * scoring stops a win being *typed*, not a reading being *forged*.
 */
@Composable
internal fun ReportedBadge(standing: ChallengeStanding, metricWord: String) {
    if (!standing.isReported) return
    val who = if (standing.isCurrentUser) {
        "You reported"
    } else {
        "Reported by ${standing.displayName.ifBlank { "this member" }}"
    }
    // "what they updated" is the number itself, in the challenge's own word. The date is
    // dropped rather than faked when the projection wrote no stamp -- an older row, or one
    // written before this field existed, has no reported-at to show and must not borrow the
    // as-of stamp beside it.
    val what = buildString {
        append(who)
        append(" · ")
        append(ChallengesViewModel.format(standing.score))
        if (metricWord.isNotBlank()) {
            append(' ')
            append(metricWord)
        }
        if (standing.reportedAtEpochMillis > 0L) {
            append(" · ")
            append(DateTimeUtils.formatDay(standing.reportedAtEpochMillis))
        }
    }
    Text(
        what,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// ── Link a goal ──────────────────────────────────────────────────────

/**
 * §6's scoring path, as a sheet: *each participant links one of their own goals of the same
 * kind*, and the challenge then moves on its own.
 *
 * **The empty case is a first-class state, not a blank list.** §6 says joining links **or
 * creates** a goal, so a user with no goal of the right kind is told what to make rather
 * than shown a picker with nothing in it. Creating one from here is the half this session
 * did not build; the message names the kind so the trip to the goals screen is one step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalLinkSheet(
    state: GoalLinkState,
    onLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text("Score this from a goal", style = MaterialTheme.typography.titleLarge)
            Text(
                state.challengeTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Your score becomes how far you move that goal from the moment you " +
                    "joined — so Health Connect, a completed task and a manual log all " +
                    "count, and nothing before you joined does.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            if (state.eligible.isEmpty()) {
                Text(
                    "None of your goals measures ${state.kindLabel.lowercase()} in " +
                        "${state.metricWord}. Make one on the Goals screen and come " +
                        "back — the challenge will pick it up.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(state.eligible, key = { it.id }) { goal ->
                    val isLinked = goal.id == state.linkedGoalId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isLinked) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (isLinked) {
                                    "Currently scoring this challenge"
                                } else {
                                    "Measured in ${goal.measureWord}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { onLink(goal.id) },
                            enabled = !state.isSaving && !isLinked,
                        ) { Text(if (isLinked) "Linked" else "Use this") }
                    }
                }
            }

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
