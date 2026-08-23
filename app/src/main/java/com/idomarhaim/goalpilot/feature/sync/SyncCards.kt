package com.idomarhaim.goalpilot.feature.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.TasksConsent
// Imported rather than copied. The wording of a proposed duration is one
// sentence the user reads in two places -- the smart-add sheet and this
// import's review row -- and two copies of it drift the first time one is
// tuned. `feature.sync` reaching into `feature.dashboard` for it is the
// smaller of the two smells; the alternative that removes both is a shared
// UI helper, and `ui/components` is a SWEPT package, so a literal reading
// "no estimate, counts as ..." would fail AnalyticsLiteralSweepTest there.
import com.idomarhaim.goalpilot.feature.dashboard.durationLabel
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.IconChip
import com.idomarhaim.goalpilot.ui.components.TasksConsentNotice
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog

/**
 * Status and manual override for the Health Connect sync (spec §5, §6 nice-to-have).
 *
 * The sync itself is automatic — it runs whenever the app comes forward, at most
 * once every fifteen minutes — so this card is mostly a window onto something that
 * has already happened. It exists because a feature that writes to your goals
 * without ever being visible is worse than one you have to press: "Synced 4
 * minutes ago" is how the user knows it is working, and the button is how they
 * skip the wait.
 *
 * The card is deliberately honest about absence: Health Connect is a separate app
 * below Android 14 and missing from most emulator images, so "not available here"
 * is a normal outcome that gets its own explanation rather than a dead button.
 */
@Composable
internal fun HealthConnectCard(state: HealthSyncState, onSync: () -> Unit) {
    val availability = state.availability
    val canSync = availability == HealthAvailability.AVAILABLE ||
        availability == HealthAvailability.PERMISSIONS_REQUIRED
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Filled.MonitorHeart,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(12.dp))
                Text("Health data", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (availability) {
                    null -> "Checking whether Health Connect is available…"
                    HealthAvailability.AVAILABLE ->
                        "Your steps and sleep are pulled from Health Connect and logged " +
                            "against your goals automatically, every time you open GoalPilot."
                    else -> availability.explain()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Recomputed on every recomposition rather than ticking: the label is a
            // coarse "4 minutes ago", and a card that repaints once a second to keep
            // a number honest costs more than the honesty is worth.
            val ago = healthSyncAgoLabel(state.lastSyncAtMillis, System.currentTimeMillis())
            if (availability == HealthAvailability.AVAILABLE) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ago ?: "Not synced on this device yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onSync,
                enabled = canSync && !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    state.isSyncing -> {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing…")
                    }

                    availability == HealthAvailability.PERMISSIONS_REQUIRED ->
                        Text("Connect Health Connect")

                    else -> Text("Sync now")
                }
            }
        }
    }
}

/**
 * Entry point for the Google Tasks import (spec §6 nice-to-have). Pulls open
 * tasks from the signed-in account's Google Tasks lists and files them against
 * goals using the same classifier as [SmartAddCard].
 *
 * When [consent] is [TasksConsent.MISSING] the card says so *before* anything is
 * pressed (#36). Until this existed, a declined scope and a first-ever run were
 * pixel-identical: both landed on the same generic grant prompt after an import
 * had already failed.
 */
@Composable
internal fun GoogleTasksImportCard(
    isLoading: Boolean,
    consent: TasksConsent?,
    onImport: () -> Unit,
) {
    // Only a positively observed refusal changes the card. Null is "not checked
    // yet" and NOT_SIGNED_IN is "never asked" — neither is a decline, and §0.4
    // only licenses speech about a failure the user can act on.
    val declined = consent == TasksConsent.MISSING
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(icon = Icons.Filled.CloudDownload)
                Spacer(Modifier.width(12.dp))
                Text("Import from Google Tasks", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            if (declined) {
                TasksConsentNotice()
            } else {
                Text(
                    "Pull your open Google Tasks in and let GoalPilot file each one " +
                        "under the right goal. You review everything before it is saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onImport,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Importing…")
                    }

                    declined -> Text(stringResource(R.string.tasks_consent_grant_action))

                    else -> Text("Import tasks")
                }
            }
        }
    }
}

/**
 * Review sheet for an import. Every row is opt-out-able and nothing is written
 * until "Import" is pressed — the LLM proposes, the user decides (spec §8).
 */
@Composable
internal fun GoogleTasksImportDialog(
    state: TasksImportState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedCount = state.proposals.count { it.selected }
    AppAlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text("Import from Google Tasks") },
        text = {
            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Reading your tasks and sorting them…")
                }

                state.error != null -> Text(state.error)

                else -> Column {
                    Text(
                        "Found ${state.totalFound} open task(s). " +
                            "Tap a row to include or exclude it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.proposals, key = { it.externalId }) { proposal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.isSaving) {
                                        onToggle(proposal.externalId)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = proposal.selected,
                                    onCheckedChange = { onToggle(proposal.externalId) },
                                    enabled = !state.isSaving,
                                )
                                Column(Modifier.weight(1f)) {
                                    // Google Tasks titles are unbounded — people paste
                                    // whole messages in. Without a clamp one entry
                                    // pushes every other row off the dialog.
                                    Text(
                                        proposal.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        buildString {
                                            append(
                                                proposal.targetGoalTitle
                                                    ?.let { "→ $it" }
                                                    ?: "→ new goal “${proposal.newGoalTitle}”",
                                            )
                                            // `#55`: the row says what the model JUDGED, not
                                            // a currency it never emitted. Points are
                                            // computed from this and the duration beside it
                                            // at the write, so printing them here would be a
                                            // third number to keep in step.
                                            append(" · ${proposal.difficulty.name.lowercase()}")
                                            append(" · ${durationLabel(proposal.minutes)}")
                                            proposal.lifeAreaName?.let {
                                                append(
                                                    if (proposal.createsLifeArea) {
                                                        " · new area “$it”"
                                                    } else {
                                                        " · $it"
                                                    },
                                                )
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.error == null && !state.isLoading) {
                TextButton(onClick = onConfirm, enabled = !state.isSaving && selectedCount > 0) {
                    Text(if (state.isSaving) "Importing…" else "Import $selectedCount")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(if (state.error != null) "Close" else "Cancel")
            }
        },
    )
}
