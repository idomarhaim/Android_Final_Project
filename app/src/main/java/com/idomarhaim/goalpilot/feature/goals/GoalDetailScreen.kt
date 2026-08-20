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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
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
import com.idomarhaim.goalpilot.domain.model.DurationEntry
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.FillLadder
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
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppDropdownMenu

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
                        AppDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                            lifeAreaNames = state.lifeAreas.map { it.name },
                            current = goal.currentValue.trimNumber(),
                            target = goal.targetValue.trimNumber(),
                            unit = goal.measureWord,
                            description = goal.description,
                            // §1.3's row, and it is empty for every goal that is
                            // not in BUTTONS mode with a classified measure — the
                            // ladder decides, so the screen never has to.
                            fillAmounts = FillLadder.forGoal(goal),
                            currentValue = goal.currentValue,
                            targetValue = goal.targetValue,
                            onFill = viewModel::logFill,
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
                            ProgressEntryRow(entry = entry, unit = goal.measureWord)
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        LogProgressDialog(
            unit = state.goal?.measureWord.orEmpty(),
            isSubmitting = action.isSubmitting,
            onDismiss = { showLogDialog = false },
            onConfirm = { value, note, uri ->
                viewModel.logProgress(value, note, uri)
                showLogDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AppAlertDialog(
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
    lifeAreaNames: List<String>,
    current: String,
    target: String,
    unit: String,
    description: String,
    fillAmounts: List<Double>,
    currentValue: Double,
    targetValue: Double,
    onFill: (Double) -> Unit,
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
            // Which parts of the user's life this goal counts towards — plural
            // since §1.2. Absent when the goal is unfiled, and then it says so,
            // because unfiled time shows up as "Unassigned" in the analytics pie
            // and that should not be a mystery.
            Text(
                text = when (lifeAreaNames.size) {
                    0 -> "No life area yet"
                    1 -> "Life area: ${lifeAreaNames.single()}"
                    else -> "Life areas: ${lifeAreaNames.joinToString(", ")}"
                },
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
            // Above "Log progress", not instead of it: §4.6 keeps the dialog
            // reachable so an amount the ladder does not offer — and a note, and
            // a photo — is still loggable on a buttons goal.
            FillButtonRow(
                amounts = fillAmounts,
                word = unit,
                current = currentValue,
                target = targetValue,
                onLog = onFill,
                modifier = Modifier.padding(top = 16.dp),
            )
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
 * Add-task row with an LLM point estimate and `R8`'s **duration box** (spec §6
 * Core "point scoring for tasks", §1.4, #9).
 *
 * The estimate arrives asynchronously via [suggestedPoints]/[suggestedMinutes]; the
 * row writes it into the editable fields so the user stays in control. The duration
 * half is held as a [DurationEntry] rather than reconstructed here — every
 * transition it makes is §1.4's precedence rule, and that rule is tested on the JVM
 * instead of only on a device.
 *
 * What replaced what: the row used to carry an `aiMinutes` variable that was *"the
 * AI's answer while the title still matches, and a function of the points the moment
 * the user types something new"*. There was no way to type a duration at all, and a
 * point-derived number was written to the task as though it were an estimate.
 */
@Composable
internal fun AddTaskRow(
    isScoring: Boolean,
    suggestedPoints: Int?,
    suggestedMinutes: Int?,
    onSuggestPoints: (String) -> Unit,
    onSuggestionApplied: () -> Unit,
    onAdd: (String, Int, Int, DurationSource) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("10") }
    var duration by remember { mutableStateOf(DurationEntry()) }

    LaunchedEffect(suggestedPoints, suggestedMinutes) {
        suggestedPoints?.let {
            points = it.toString()
            // Not an assignment: withEstimate is where §1.4 lives, and a duration the
            // user typed comes back out of it unchanged. Re-estimating is exactly the
            // event the rule is about, so the rule is applied at the event.
            duration = duration.withEstimate(suggestedMinutes)
            onSuggestionApplied()
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    // An AI estimate belonged to the old wording; keeping it would
                    // credit a rewritten task with the previous one's time. A typed
                    // duration survives — see DurationEntry.withRetitle.
                    duration = duration.withRetitle()
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
                    val (storedMinutes, storedSource) = duration.resolve()
                    onAdd(title, points.toIntOrNull() ?: 10, storedMinutes, storedSource)
                    title = ""
                    points = "10"
                    duration = DurationEntry()
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }

        // R8's box. Optional by design: empty is a legitimate state and is stored as
        // DEFAULT_MINUTES with UNKNOWN provenance (§3.4) rather than as a guess
        // derived from the title's word count, which is what happened before #9.
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = duration.text(),
                onValueChange = { duration = duration.typed(it) },
                label = { Text("Minutes") },
                placeholder = { Text("How long?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                // "an icon inside the box for as long as the person has not entered
                // a number" — R8 literally. It reads stored provenance rather than
                // comparing the number against what a fallback would have produced.
                //
                // OUTLINED and muted, deliberately, where the button above is filled
                // and tertiary-tinted. Found by looking at the render pass, not by a
                // test: the same filled glyph in the same colour sat inches from the
                // AI *button*, so one row carried two identical marks meaning "press
                // me" and "nobody typed this". That is §0.8's surviving sub-rule —
                // one chip may not carry two axes — and it is invisible to every
                // assertion here, because both nodes are correct in isolation.
                trailingIcon = if (duration.showsEstimateIcon) {
                    {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = AI_ESTIMATE_ICON_LABEL,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(140.dp)
                    .testTag(DURATION_BOX_TAG),
            )
            // The duration is what this task will contribute to the analytics pie,
            // so it is said before the task is created rather than discovered later.
            Text(
                text = durationCaption(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}

/**
 * What the line under the box says — **derived from what will actually be stored**,
 * not from what is in the field.
 *
 * That distinction is the whole ticket in miniature, and reading the field directly
 * produced the exact defect #9 exists to remove: typing `0` left `isTyped` true and
 * `minutes` non-null, so the caption read *"You said about 0m"* while
 * [DurationEntry.resolve] correctly stored `30` as nobody's answer. A caption that
 * disagrees with the stored value is §0.3's *second number that quietly disagrees*,
 * one line below the box built to end it. Going through `resolve()` makes the
 * disagreement unrepresentable rather than merely fixed.
 */
private fun durationCaption(duration: DurationEntry): String {
    val (minutes, source) = duration.resolve()
    return when (source) {
        DurationSource.USER -> "You said about ${DateTimeUtils.formatMinutes(minutes)}"
        DurationSource.AI ->
            "AI estimate: about ${DateTimeUtils.formatMinutes(minutes)} of your time"
        DurationSource.UNKNOWN -> "Not set — counts as ${DateTimeUtils.formatMinutes(minutes)}"
    }
}

/** Stable handles for the instrumented test; the icon's label is also its a11y text. */
internal const val DURATION_BOX_TAG = "add-task-duration-box"
internal const val AI_ESTIMATE_ICON_LABEL = "Duration estimated by AI"

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

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    // "Amount (%)" on a goal that never chose percent is the
                    // map's most-repeated finding at its first site (§4.6, `R14`).
                    // With the placeholder gone the unmeasured goal has no word,
                    // and the honest label is the bare one.
                    label = { Text(if (unit.isBlank()) "Amount" else "Amount ($unit)") },
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
