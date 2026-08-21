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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.DurationEntry
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.FillLadder
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskScoring
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
                            suggestedDifficulty = action.suggestedDifficulty,
                            suggestedMinutes = action.suggestedMinutes,
                            onSuggestEstimate = viewModel::suggestEstimate,
                            onSuggestionApplied = viewModel::consumeSuggestedEstimate,
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
 * Add-task row: `R8`'s **duration box** and §1.4's **difficulty**, with the LLM filling in
 * both (spec §3.3 A, §1.4, #9, `#55`).
 *
 * ### There is no points field here any more, and that is the ticket
 *
 * The row used to carry a `Pts` text box the user typed a number into, seeded from what the
 * model returned. §1.4 makes points a **view of effort** — `round(minutes / 3) × difficulty`
 * — so there is nothing left for anybody to type: the two inputs are on screen, and the
 * currency falls out of them. Keeping the box would have made it a third input that has to
 * agree with the other two, which is §0.3's *second number that quietly disagrees* in its
 * purest form.
 *
 * The estimate arrives asynchronously via [suggestedDifficulty]/[suggestedMinutes]; the row
 * writes it into the editable controls so the user stays in control. The duration half is
 * held as a [DurationEntry] rather than reconstructed here — every transition it makes is
 * §1.4's precedence rule, and that rule is tested on the JVM instead of only on a device.
 *
 * What replaced what, one layer back: the row used to carry an `aiMinutes` variable that was
 * *"the AI's answer while the title still matches, and a function of the points the moment
 * the user types something new"*. There was no way to type a duration at all, and a
 * point-derived number was written to the task as though it were an estimate.
 */
@Composable
internal fun AddTaskRow(
    isScoring: Boolean,
    suggestedDifficulty: Difficulty?,
    suggestedMinutes: Int?,
    onSuggestEstimate: (String) -> Unit,
    onSuggestionApplied: () -> Unit,
    onAdd: (String, Difficulty, Int, DurationSource, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(Difficulty.ROUTINE) }
    var duration by remember { mutableStateOf(DurationEntry()) }
    // `#7`. Belongs to the sentence being typed, so it lives with `title` and dies with it.
    var alreadyDone by remember { mutableStateOf(false) }

    LaunchedEffect(suggestedDifficulty, suggestedMinutes) {
        suggestedDifficulty?.let {
            // Assigned outright, unlike the duration below. There is no sticky-difficulty
            // rule: §1.4 makes only the **typed number** sticky, because it is a fact about
            // the user's own day. Difficulty is a judgement about the work, which is the one
            // §0.5 lets the model make — and the chips are right there to overrule it.
            difficulty = it
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
            IconButton(
                onClick = { onSuggestEstimate(title) },
                enabled = !isScoring,
                modifier = Modifier.padding(start = 2.dp),
            ) {
                if (isScoring) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "Estimate difficulty and duration with AI",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            IconButton(
                onClick = {
                    val (storedMinutes, storedSource) = duration.resolve()
                    onAdd(title, difficulty, storedMinutes, storedSource, alreadyDone)
                    title = ""
                    difficulty = Difficulty.ROUTINE
                    duration = DurationEntry()
                    // Cleared with everything else, for the reason SmartAddCard's copy of this
                    // chip records: a "done" that survived the add would be a mode that
                    // silently completes whatever is typed next.
                    alreadyDone = false
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

        // §1.4's difficulty, and the other half of what used to be the `Pts` box.
        //
        // ITS OWN ROW, for the reason the `#7` chip below records about itself: the duration
        // row is already a 140dp box plus a caption that runs to "no estimate · counts as
        // 30m", and three chips beside it would ellipsise the half of that caption which says
        // what will be stored.
        //
        // Chips rather than a dropdown: three values, all visible, one tap to change — and
        // §0.8's surviving sub-rule (one chip may not carry two axes) is satisfied because
        // these three are one axis, exclusive, and carry no icon. The row also states what
        // the choice is worth, because a currency the user cannot predict is one they cannot
        // trust: `AppDropdownMenu` would have hidden two of the three options behind a tap.
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Difficulty.entries.forEach { option ->
                FilterChip(
                    selected = difficulty == option,
                    onClick = { difficulty = option },
                    label = { Text(difficultyLabel(option)) },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag(difficultyTag(option)),
                )
            }
        }
        // What the two inputs above add up to, said before the task is created rather than
        // discovered afterwards -- the same argument as the duration caption. Points are a
        // VIEW of effort (§1.4), and a view nobody can see is indistinguishable from a
        // stored number that might disagree.
        Text(
            text = "Worth ${TaskScoring.pointsFor(duration.resolve().first, difficulty)} pts",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).testTag(POINTS_PREVIEW_TAG),
        )

        // `#7`'s create-and-complete, the same control the dashboard's quick-add card carries.
        // See `GoalDetailViewModel.addTask` for why this surface has it at all, `R6` naming
        // only quick add.
        //
        // ITS OWN ROW, not squeezed in beside the duration box. The row above is a 140dp box
        // plus a caption that already runs to "no estimate · counts as 30m"; a chip there
        // leaves the caption about fifty pixels on a phone, which ellipsises away the half of
        // it that says what will be stored — and that caption is the whole of `#9`.
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = alreadyDone,
                onClick = { alreadyDone = !alreadyDone },
                label = { Text(ALREADY_DONE_LABEL) },
                leadingIcon = if (alreadyDone) {
                    { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                modifier = Modifier.testTag(ALREADY_DONE_TAG),
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

/**
 * The word on a difficulty chip (§1.4, `#55`).
 *
 * The **multiplier is not on the label**, deliberately. §1.4 keeps the multipliers in the app
 * so the currency cannot be moved by phrasing; putting `×1.5` on a chip would invite the user
 * to shop for a number rather than describe the work, which is the same failure one layer up.
 * The points preview beside the row says what the choice is worth, once, for the task
 * actually being typed.
 */
private fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.LIGHT -> "Light"
    Difficulty.ROUTINE -> "Routine"
    Difficulty.DEMANDING -> "Demanding"
}

/** Stable per-chip handle, so an assertion names one chip rather than a position. */
private fun difficultyTag(difficulty: Difficulty): String =
    "add-task-difficulty-${difficulty.name.lowercase()}"

/** Stable handles for the instrumented test; the icon's label is also its a11y text. */
internal const val DURATION_BOX_TAG = "add-task-duration-box"

/** What the two inputs above it add up to — §1.4's points, as a view. */
internal const val POINTS_PREVIEW_TAG = "add-task-points-preview"

/** `#7`'s create-and-complete toggle on this surface. */
internal const val ALREADY_DONE_TAG = "add-task-already-done"

/** Its words, shared with the tests so an assertion cannot pass against a stale copy. */
internal const val ALREADY_DONE_LABEL = "Already done"
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
                // The BANKED minutes for a completed task, not today's estimate (`#55`). The
                // number beside it is `task.points`, which for a done task comes from the
                // completion fact -- so reading the current estimate here would print a
                // duration and a price that do not compute against each other, which is
                // exactly the disagreement §1.4 banks the inputs to prevent.
                text = DateTimeUtils.formatMinutes(
                    task.completion?.minutes ?: TaskDuration.minutesOf(task),
                ),
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
