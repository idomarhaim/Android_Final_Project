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
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.ui.components.Avatar
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
    onType: (ChallengeType) -> Unit,
    onMetricUnit: (String) -> Unit,
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

                Text(
                    "Type",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChallengeType.entries.forEach { type ->
                        FilterChip(
                            selected = type == state.type,
                            onClick = { onType(type) },
                            label = { Text(labelFor(type)) },
                            leadingIcon = { TypeIconBadge(type) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.metricUnit,
                    onValueChange = onMetricUnit,
                    label = { Text("Measured in") },
                    singleLine = true,
                    supportingText = { Text("Scores are reported in this unit — km, hours, reps…") },
                    modifier = Modifier.fillMaxWidth(),
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
                    label = { Text("Total ${state.metricUnit}") },
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
                "${participantSummary(card.participantCount)} · scored in " +
                    card.challenge.metricUnit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

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
                            modifier = Modifier.padding(start = 12.dp).weight(1f),
                        )
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
