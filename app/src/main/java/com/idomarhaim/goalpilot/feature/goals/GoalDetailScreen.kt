package com.idomarhaim.goalpilot.feature.goals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.idomarhaim.goalpilot.ui.theme.gpAccents
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.ProgressRing
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.components.icon
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.components.trimNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }

    LaunchedEffect(action.message) {
        action.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(state.goal?.title ?: "Goal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.goal != null) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { menuOpen = false; onEdit(state.goal!!.id) },
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                onClick = { menuOpen = false; viewModel.archiveGoal() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { menuOpen = false; showDeleteDialog = true },
                            )
                        }
                    }
                },
            )
        },
    ) { inner ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(inner))
            state.goal == null -> EmptyState(
                title = "Goal not found",
                subtitle = "It may have been deleted.",
                modifier = Modifier.padding(inner),
            )

            else -> {
                val goal = state.goal!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        GoalHeaderCard(
                            percent = goal.progressPercent,
                            fraction = goal.progressFraction,
                            accentHex = goal.colorHex,
                            categoryLabel = goal.category.label,
                            lifeAreaName = state.lifeArea?.name,
                            current = goal.currentValue.trimNumber(),
                            target = goal.targetValue.trimNumber(),
                            unit = goal.unit,
                            description = goal.description,
                            onLogProgress = { showLogDialog = true },
                        )
                    }
                    item { SectionHeader(title = "Tasks") }
                    item {
                        AddTaskRow(
                            isScoring = action.isScoring,
                            suggestedPoints = action.suggestedPoints,
                            suggestedMinutes = action.suggestedMinutes,
                            onSuggestPoints = viewModel::suggestPoints,
                            onSuggestionApplied = viewModel::consumeSuggestedPoints,
                            onAdd = viewModel::addTask,
                        )
                    }
                    if (state.tasks.isEmpty()) {
                        item {
                            Text(
                                "No tasks yet — add small tasks that move this goal forward.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onDelete = { viewModel.deleteTask(task.id) },
                            )
                        }
                    }
                    item { SectionHeader(title = "Progress log") }
                    if (state.entries.isEmpty()) {
                        item {
                            Text(
                                "No entries yet. Use “Log progress” to record a step (and attach a photo).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.entries, key = { it.id }) { entry ->
                            ProgressEntryRow(entry = entry, unit = goal.unit)
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        LogProgressDialog(
            unit = state.goal?.unit ?: "",
            isSubmitting = action.isSubmitting,
            onDismiss = { showLogDialog = false },
            onConfirm = { value, note, uri ->
                viewModel.logProgress(value, note, uri)
                showLogDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete goal?") },
            text = { Text("This permanently removes the goal. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteGoal(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GoalHeaderCard(
    percent: Int,
    fraction: Float,
    accentHex: String,
    categoryLabel: String,
    lifeAreaName: String?,
    current: String,
    target: String,
    unit: String,
    description: String,
    onLogProgress: () -> Unit,
) {
    val accent = accentHex.toGoalAccent()
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressRing(progress = fraction, color = accent, size = 140.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(categoryLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            // Which part of the user's life this goal counts towards. Absent when
            // the goal is unfiled — and then it says so, because unfiled time shows
            // up as "Unassigned" in the analytics pie and that should not be a
            // mystery.
            Text(
                text = lifeAreaName?.let { "Life area: $it" } ?: "No life area yet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            FilledTonalButton(
                onClick = onLogProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
                Text("Log progress", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * Add-task row with an LLM point estimate (spec §6 Core: "point scoring for
 * tasks"). The estimate arrives asynchronously via [suggestedPoints]; the row
 * writes it into the editable field and clears it so the user stays in control.
 */
@Composable
private fun AddTaskRow(
    isScoring: Boolean,
    suggestedPoints: Int?,
    suggestedMinutes: Int?,
    onSuggestPoints: (String) -> Unit,
    onSuggestionApplied: () -> Unit,
    onAdd: (String, Int, Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("10") }
    // Held separately from the points field because it has no input of its own:
    // it is the AI's answer while the title still matches, and a function of the
    // points the moment the user types something new.
    var aiMinutes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(suggestedPoints, suggestedMinutes) {
        suggestedPoints?.let {
            points = it.toString()
            aiMinutes = suggestedMinutes
            onSuggestionApplied()
        }
    }

    val minutes = aiMinutes ?: TaskDuration.fallbackMinutes(points.toIntOrNull() ?: 10)

    Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                // The estimate belonged to the old wording; keeping it would credit
                // a rewritten task with the previous one's time.
                aiMinutes = null
            },
            label = { Text("Add a task") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = points,
            onValueChange = { points = it.filter { c -> c.isDigit() } },
            label = { Text("Pts") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .padding(start = 8.dp)
                .width(72.dp),
        )
        IconButton(
            onClick = { onSuggestPoints(title) },
            enabled = !isScoring,
            modifier = Modifier.padding(start = 2.dp),
        ) {
            if (isScoring) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "Estimate points with AI",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        IconButton(
            onClick = {
                onAdd(title, points.toIntOrNull() ?: 10, minutes)
                title = ""
                points = "10"
                aiMinutes = null
            },
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add task")
        }
    }
        // The duration is what this task will contribute to the analytics pie, so
        // it is shown before the task is created rather than discovered later.
        Text(
            text = if (aiMinutes != null) {
                "AI estimate: about ${DateTimeUtils.formatMinutes(minutes)} of your time"
            } else {
                "Counts as about ${DateTimeUtils.formatMinutes(minutes)} of your time"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            // A done task should recede rather than disappear — struck through and
            // muted keeps it countable without competing with what's still open.
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = DateTimeUtils.formatMinutes(TaskDuration.minutesOf(task)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = "+${task.points}",
                style = MaterialTheme.typography.labelLarge,
                color = if (task.isDone) {
                    MaterialTheme.gpAccents.positive
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressEntryRow(entry: ProgressEntry, unit: String) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!entry.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (entry.imageUrl.isNullOrBlank()) 0.dp else 12.dp),
            ) {
                Text(
                    text = "+${entry.value.trimNumber()} $unit",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = DateTimeUtils.relative(entry.createdAtEpochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LogProgressDialog(
    unit: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Uri?) -> Unit,
) {
    var value by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> imageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount ($unit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Text(
                        if (imageUri == null) "Attach photo" else "Change photo",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = { onConfirm(value.toDoubleOrNull() ?: 0.0, note, imageUri) },
            ) { Text(if (isSubmitting) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
