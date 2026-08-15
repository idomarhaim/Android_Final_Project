package com.idomarhaim.goalpilot.feature.lifeareas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.TasksConsent
import com.idomarhaim.goalpilot.domain.usecase.LifeAreaProposal
import com.idomarhaim.goalpilot.domain.usecase.LifeAreaSyncAction
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.components.TasksConsentNotice
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toComposeColor
import com.idomarhaim.goalpilot.ui.components.toGoalAccent

/**
 * Define the areas your life is divided into, pull them from your Google Tasks
 * lists, and file goals under them (spec §1, §6 nice-to-have).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeAreasScreen(
    onBack: () -> Unit,
    onOpenArea: (String) -> Unit,
    viewModel: LifeAreasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val consentIntent by viewModel.consentIntent.collectAsStateWithLifecycle()
    val tasksConsent by viewModel.tasksConsent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<LifeArea?>(null) }
    val reorder = rememberLifeAreaReorderState()

    // Re-read on every entry, not once: the dashboard grants the same scope, and
    // this ViewModel outlives a trip there and back (#36).
    LaunchedEffect(Unit) { viewModel.refreshTasksConsent() }
    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }

    // Keyed on the rows alone, deliberately: at the instant of a drop the drag is
    // already over while the flow still holds the pre-drag list, and re-running
    // this then would snap the card back for the frame before Firestore echoes.
    LaunchedEffect(state.rows) { reorder.sync(state.rows) }

    // The Tasks scope is granted once per account, and sign-in offers it with the
    // box unticked (spec §2.6), so a missing scope is the normal case rather than
    // a legacy account. Backing out of Google's screen is recorded as a decline
    // so the card can say so instead of re-offering a generic prompt (#36).
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onConsentGranted()
        } else {
            viewModel.onConsentDeclined()
        }
    }
    LaunchedEffect(consentIntent) {
        consentIntent?.let { consentLauncher.launch(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Life areas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openEditor(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New area") },
            )
        },
    ) { inner ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(inner))
            return@Scaffold
        }

        LazyColumn(
            state = reorder.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GoogleSyncCard(
                    isLoading = sync.isLoading,
                    consent = tasksConsent,
                    onSync = viewModel::syncFromGoogleTasks,
                )
            }

            if (state.rows.isEmpty()) {
                item {
                    EmptyState(
                        title = "No life areas yet",
                        subtitle = "Add the parts your life is actually divided into — or " +
                            "sync them from your Google Tasks lists.",
                        icon = Icons.Outlined.Category,
                        modifier = Modifier.heightIn(min = 260.dp),
                    )
                }
            } else {
                item { SectionHeader("Your areas") }
                if (state.rows.size > 1) {
                    item {
                        Text(
                            "Hold the ⠿ handle to drag an area into place. The order " +
                                "here is the order your goals and analytics use.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                lifeAreaRows(
                    state = reorder,
                    onMove = viewModel::moveArea,
                    onOpen = { onOpenArea(it.id) },
                    onEdit = { viewModel.openEditor(it) },
                    onDelete = { pendingDelete = it },
                )
            }

            if (state.unfiledGoals.isNotEmpty() && state.rows.isNotEmpty()) {
                item { SectionHeader("Goals with no area") }
                item {
                    Text(
                        "Time spent on these shows up as “Unassigned” in your analytics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.unfiledGoals, key = { it.id }) { goal ->
                    UnfiledGoalCard(
                        goal = goal,
                        areas = state.rows.map { it.area },
                        onAssign = { areaId -> viewModel.assignGoal(goal.id, areaId) },
                    )
                }
            }
        }
    }

    if (editor.isVisible) {
        LifeAreaEditorDialog(
            state = editor,
            onName = viewModel::onNameChange,
            onColor = viewModel::onColorChange,
            onIcon = viewModel::onIconChange,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor,
        )
    }

    if (sync.isVisible) {
        GoogleListSyncDialog(
            state = sync,
            onToggle = viewModel::toggleProposal,
            onConfirm = viewModel::confirmSync,
            onDismiss = viewModel::dismissSync,
        )
    }

    pendingDelete?.let { area ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “${area.name}”?") },
            text = {
                Text(
                    "The goals filed under it are kept — they simply become unfiled, " +
                        "and their time moves to “Unassigned” in your analytics.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteArea(area.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

// ── Cards ────────────────────────────────────────────────────────────

/**
 * This card reads the *same* `tasks.readonly` scope as the dashboard import, so
 * it carries the same defect and the same fix (#36): when [consent] is
 * [TasksConsent.MISSING] it says the scope was not granted, rather than looking
 * identical to a first-ever run.
 */
@Composable
private fun GoogleSyncCard(isLoading: Boolean, consent: TasksConsent?, onSync: () -> Unit) {
    // Only a positively observed refusal speaks. Null is "not checked yet" and
    // NOT_SIGNED_IN is "never asked" — neither is a decline.
    val declined = consent == TasksConsent.MISSING
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sync from Google Tasks", style = MaterialTheme.typography.titleMedium)
            if (declined) {
                TasksConsentNotice(modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
            } else {
                Text(
                    "Your Google Tasks lists are already the areas of your life. Pull " +
                        "their names in as life areas — empty lists included — and review " +
                        "what gets added before anything is saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
            FilledTonalButton(onClick = onSync, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Link, contentDescription = null)
                }
                Text(
                    if (declined) {
                        stringResource(R.string.tasks_consent_grant_action)
                    } else {
                        "Sync my lists"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** A goal with no life area, plus a one-tap menu to file it. */
@Composable
private fun UnfiledGoalCard(goal: Goal, areas: List<LifeArea>, onAssign: (String?) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goal.title.ifBlank { "Untitled goal" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "File “${goal.title}”")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    areas.forEach { area ->
                        DropdownMenuItem(
                            text = { Text(area.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconForKey(area.iconKey),
                                    contentDescription = null,
                                    tint = area.colorHex.toGoalAccent(),
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onAssign(area.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────

@Composable
private fun LifeAreaEditorDialog(
    state: LifeAreaEditorState,
    onName: (String) -> Unit,
    onColor: (String) -> Unit,
    onIcon: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isEdit) "Edit life area" else "New life area") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Colour",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LifeAreaPalette.hexes.forEach { hex ->
                        val selected = hex.equals(state.colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(hex.toComposeColor())
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { onColor(hex) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected colour",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    "Icon",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LifeAreaPalette.iconKeys.forEach { key ->
                        val selected = key == state.iconKey
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        state.colorHex.toComposeColor().copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                )
                                .clickable { onIcon(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconForKey(key),
                                contentDescription = key,
                                tint = if (selected) {
                                    state.colorHex.toGoalAccent()
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp),
                            )
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
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving) {
                Text(if (state.isEdit) "Save" else "Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GoogleListSyncDialog(
    state: LifeAreaSyncState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync life areas") },
        text = {
            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Reading your Google Tasks lists…", modifier = Modifier.padding(start = 12.dp))
                }

                state.error != null -> Text(state.error)

                else -> Column {
                    val creating = state.proposals.count { it.action == LifeAreaSyncAction.CREATE }
                    val linking = state.proposals.size - creating
                    Text(
                        buildString {
                            append("From ${state.totalLists} Google Tasks list")
                            if (state.totalLists != 1) append("s")
                            append(": $creating new")
                            if (linking > 0) append(", $linking already named here")
                            append(". Tap a row to include or exclude it.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.proposals, key = { it.googleListId }) { proposal ->
                            SyncProposalRow(
                                proposal = proposal,
                                enabled = !state.isSaving,
                                onToggle = { onToggle(proposal.googleListId) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.error == null && !state.isLoading) {
                TextButton(
                    onClick = onConfirm,
                    enabled = !state.isSaving && state.proposals.any { it.selected },
                ) {
                    val count = state.proposals.count { it.selected }
                    Text(if (state.isSaving) "Saving…" else "Sync $count")
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

@Composable
private fun SyncProposalRow(proposal: LifeAreaProposal, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = proposal.selected,
            onCheckedChange = { onToggle() },
            enabled = enabled,
        )
        Icon(
            imageVector = iconForKey(proposal.iconKey),
            contentDescription = null,
            tint = proposal.colorHex.toGoalAccent(),
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                proposal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when (proposal.action) {
                    LifeAreaSyncAction.CREATE -> "New life area"
                    LifeAreaSyncAction.LINK -> "Link to the area you already have"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
