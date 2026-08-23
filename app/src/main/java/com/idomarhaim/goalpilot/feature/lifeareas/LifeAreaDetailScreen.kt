package com.idomarhaim.goalpilot.feature.lifeareas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.GpLinearProgress
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.components.SuccessFailureRunCard
import com.idomarhaim.goalpilot.ui.components.UnmeasuredMarkerIfNeeded
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.components.toGoalInk
import com.idomarhaim.goalpilot.ui.components.trimNumber
import com.idomarhaim.goalpilot.ui.locale.AppDropdownMenu

/**
 * One life area, and the goals filed under it (`PRODUCT_v0.3` §4.7).
 *
 * This screen is the destination issue #2 was missing: from the life-areas list,
 * *"3 goals"* now goes somewhere. Before it, the only route into a goal was the
 * Goals tab, and the count read as a link while being a label.
 *
 * **No `GoalCategory` is rendered here** (`C23`, #45) — the area screen shows
 * areas, not categories — which is why the rows are drawn locally rather than with
 * the shared `GoalCard`, whose icon and meta line are both the goal's *category*.
 * Every accent on this screen is the **area's** colour, so the one thing the
 * screen is about is the one thing that is coloured.
 *
 * ✅ **The success/failure run is here now** — `C19`
 * ([#64](https://github.com/idomarhaim/Android_Final_Project/issues/64)), between the
 * header and the goal list exactly as §4.7 puts it. This file used to carry a note
 * saying it was *"left out rather than mocked up"* because the run counts **missed
 * windows** and windows are `occurrences`, a collection §7.1 marked **new**; that
 * collection shipped with [#63](https://github.com/idomarhaim/Android_Final_Project/issues/63),
 * so the reason expired and the note with it. The component itself is
 * [SuccessFailureRunCard][com.idomarhaim.goalpilot.ui.components.SuccessFailureRunCard]
 * in `ui/components/`, shared with analytics so the two placements cannot drift into
 * two answers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeAreaDetailScreen(
    onBack: () -> Unit,
    onOpenGoal: (String) -> Unit,
    viewModel: LifeAreaDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }

    val accent = state.area?.colorHex?.toGoalAccent() ?: MaterialTheme.colorScheme.primary
    val ink = state.area?.colorHex?.toGoalInk() ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        text = state.area?.name ?: "Life area",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(inner))
            return@Scaffold
        }

        if (!state.areaExists) {
            EmptyState(
                title = "That life area is gone",
                subtitle = "It was deleted — on this device or another one. Its goals were " +
                    "kept and are now unfiled; you can file them again from Life areas.",
                icon = Icons.Outlined.Category,
                modifier = Modifier.padding(inner),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AreaHeaderCard(state = state, accent = accent) }

            // §4.7: "above the goal list on the life-area screen". Above the empty
            // state too -- an area with no goals filed is precisely the case the
            // run's `no next step` half is about, and hiding it there would leave
            // the screen silent about the one thing it could usefully offer.
            item {
                SuccessFailureRunCard(
                    run = state.run,
                    onSelectRange = viewModel::selectSuccessRange,
                    onOpenGoal = onOpenGoal,
                    accent = accent,
                    // §4.7: the asymmetry sentence lives beside the time donut on
                    // analytics AND NOWHERE ELSE. Revision 1 of the prototype put it
                    // on every area frame and said the same thing twice.
                    showAsymmetryNote = false,
                )
            }

            if (state.goals.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing filed here yet",
                        subtitle = "Goals you file under this area show up here. File one from " +
                            "the list below, or pick this area while editing a goal.",
                        icon = Icons.Outlined.Flag,
                        modifier = Modifier.heightIn(min = 220.dp),
                    )
                }
            } else {
                item { SectionHeader("Goals in this area") }
                items(state.goals, key = { it.id }) { goal ->
                    AreaGoalCard(
                        goal = goal,
                        accent = accent,
                        ink = ink,
                        onClick = { onOpenGoal(goal.id) },
                        onRemove = { viewModel.removeGoalFromArea(goal) },
                    )
                }
            }

            if (state.unfiledGoals.isNotEmpty()) {
                item { SectionHeader("Goals with no area") }
                item {
                    Text(
                        "A goal can serve more than one area, so filing it here does not " +
                            "take it away from anywhere else.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.unfiledGoals, key = { "unfiled-${it.id}" }) { goal ->
                    UnfiledGoalRow(goal = goal, onFile = { viewModel.fileGoalHere(goal) })
                }
            }
        }
    }
}

/**
 * The area's own identity, and its count.
 *
 * The count is direction-isolated (§4.8): `"2 goals"` inside a Hebrew paragraph
 * reorders under the bidi algorithm, and this is a number embedded in a sentence,
 * which is exactly the shape the rule names.
 */
@Composable
private fun AreaHeaderCard(state: LifeAreaDetailUiState, accent: Color) {
    val area = state.area ?: return
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForKey(area.iconKey),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = area.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = goalCountLabel(state.goals.size) +
                        if (area.isLinkedToGoogleTasks) " · synced from Google Tasks" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A goal row for the area screen. Carries no category: the accent, and therefore
 * the only colour on the row, is the **area's** (`C23`).
 */
@Composable
internal fun AreaGoalCard(
    goal: Goal,
    accent: Color,
    /** The readable twin of [accent] -- `#57` a split fill from ink; see `String.toGoalInk`. */
    ink: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GpCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title.ifBlank { "Untitled goal" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    // §1.3's marker, which `#65` put on `GoalCard` and not here —
                    // and §1.3 says *wherever the goal is listed*, which this is
                    // (`#66`). It takes the trailing slot rather than sharing it:
                    // the percentage that used to sit here was `currentValue`
                    // over `targetValue`'s 100.0 default, so the row asserted a
                    // fraction of a target nobody set.
                    UnmeasuredMarkerIfNeeded(measureIsAbsent = goal.isUnmeasured)
                    if (!goal.isUnmeasured) {
                        Text(
                            // A percentage inside a bidi paragraph is the §4.8 shape.
                            text = "${goal.progressPercent}%".bidiIsolated(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                        )
                    }
                }
                // The bar is the same claim drawn instead of printed, so it goes
                // with the digit rather than staying behind as a 1%-full sliver.
                if (!goal.isUnmeasured) {
                    GpLinearProgress(
                        progress = goal.progressFraction,
                        color = accent,
                        height = 8.dp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                // Nothing at all for a goal that CHOSE percent: this row's only
                // meta line IS the ratio, and the trailing `45%` above already
                // said it. `BuildWidgetSnapshotUseCase.measureLabel()` has made
                // the same call since `#11`; `#66` found the row had not.
                if (!goal.restatesPercent) Text(
                    text = if (goal.isUnmeasured) {
                        // The honest count, which is what the `C22` prototype's own
                        // life-area frame draws: `no number — 11 sessions logged`.
                        // This package is unswept (§0.8 is suspended; AGENTS.md), so
                        // the copy is a plain English literal, like every other
                        // string on this screen.
                        when (goal.loggedEntryCount) {
                            0 -> "No number yet"
                            1 -> "No number — 1 entry logged"
                            else -> "No number — ${goal.loggedEntryCount} entries logged"
                        }
                    } else {
                        ("${goal.currentValue.trimNumber()}/${goal.targetValue.trimNumber()} " +
                            goal.measureWord).trim()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (goal.lifeAreaIds.size > 1) {
                    Text(
                        // Plural areas are legal (§1.2), so removing a goal from
                        // here is not the same as unfiling it — say which it is
                        // before the menu is opened, not after the write.
                        text = "Also in ${"${goal.lifeAreaIds.size - 1}".bidiIsolated()} other " +
                            if (goal.lifeAreaIds.size == 2) "area" else "areas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More for “${goal.title}”")
                }
                AppDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove from this area") },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnfiledGoalRow(goal: Goal, onFile: () -> Unit) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goal.title.ifBlank { "Untitled goal" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFile) { Text("File here") }
        }
    }
}

/**
 * `"3 goals"`, with the number isolated so it cannot be reordered out of the
 * phrase in an RTL paragraph (§4.8).
 */
internal fun goalCountLabel(count: Int): String =
    "${"$count".bidiIsolated()} goal" + if (count == 1) "" else "s"
