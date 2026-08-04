package com.idomarhaim.goalpilot.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import com.idomarhaim.goalpilot.core.util.DateTimeUtils.formatMinutes
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocation
import com.idomarhaim.goalpilot.domain.usecase.TimeSlice
import com.idomarhaim.goalpilot.domain.usecase.TimeTrend
import com.idomarhaim.goalpilot.ui.components.BarItem
import com.idomarhaim.goalpilot.ui.components.DonutChart
import com.idomarhaim.goalpilot.ui.components.DonutSlice
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.HorizontalBarChart
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.StackedColumn
import com.idomarhaim.goalpilot.ui.components.StackedColumnChart
import com.idomarhaim.goalpilot.ui.components.StackedSegment
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.rememberChartProgress
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    onOpenLifeAreas: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backfill by viewModel.backfill.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (backfill.isVisible) {
        DurationBackfillDialog(
            state = backfill,
            onToggle = viewModel::toggleDurationProposal,
            onConfirm = viewModel::confirmBackfill,
            onDismiss = viewModel::dismissBackfill,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(inner))
            state.goals.isEmpty() -> EmptyState(
                title = "Nothing to chart yet",
                subtitle = "Add goals and complete tasks to see your analytics.",
                icon = Icons.Outlined.BarChart,
                modifier = Modifier.padding(inner),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RangePicker(selected = state.range, onSelect = viewModel::selectRange)

                TimeAllocationCard(
                    allocation = state.allocation,
                    rangeLabel = state.rangeLabel,
                    range = state.range,
                    hasLifeAreas = state.lifeAreas.isNotEmpty(),
                    selectedSliceId = state.selectedSliceId,
                    inferredTaskCount = state.inferredTaskCount,
                    isReEstimating = backfill.isLoading,
                    onSelectSlice = viewModel::selectSlice,
                    onOpenLifeAreas = onOpenLifeAreas,
                    onReEstimate = viewModel::reEstimateDurations,
                )

                TimeTrendCard(
                    trend = state.trend,
                    range = state.range,
                    selectedSliceId = state.selectedSliceId,
                )

                ProgressByGoalCard(state)
                TaskFocusCard(state)
            }
        }
    }
}

// ── Range picker ─────────────────────────────────────────────────────

/**
 * Day / week / month / quarter / year.
 *
 * A scrolling chip row rather than segmented buttons: five segments with a word
 * as long as "Quarter" do not fit a phone without truncating to initials, and
 * "Q" is not a label anybody reads.
 */
@Composable
private fun RangePicker(selected: AnalyticsRange, onSelect: (AnalyticsRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnalyticsRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label) },
            )
        }
    }
}

// ── Time allocation (the headline chart) ─────────────────────────────

@Composable
private fun TimeAllocationCard(
    allocation: TimeAllocation,
    rangeLabel: String,
    range: AnalyticsRange,
    hasLifeAreas: Boolean,
    selectedSliceId: String?,
    inferredTaskCount: Int,
    isReEstimating: Boolean,
    onSelectSlice: (String?) -> Unit,
    onOpenLifeAreas: () -> Unit,
    onReEstimate: () -> Unit,
) {
    ChartCard(
        title = "Where your time goes",
        subtitle = "$rangeLabel · share of your tracked time per life area",
    ) {
        when {
            !hasLifeAreas -> NoLifeAreasHint(onOpenLifeAreas)

            allocation.isEmpty -> Text(
                "Nothing completed in this ${range.label.lowercase()} yet. " +
                    "Tick a task off and its time lands here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> {
                val donutSlices = allocation.slices.map { slice ->
                    DonutSlice(
                        id = slice.areaId.sliceKey(),
                        label = slice.name,
                        fraction = slice.fraction,
                        color = slice.colorHex.toGoalAccent(),
                    )
                }
                val selected = allocation.slices
                    .firstOrNull { it.areaId.sliceKey() == selectedSliceId }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DonutChart(
                        slices = donutSlices,
                        selectedId = selectedSliceId,
                        onSelect = onSelectSlice,
                        contentDescription = allocation.describe(),
                    ) {
                        DonutCenter(allocation = allocation, selected = selected)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (selected == null) {
                            "Tap a slice for the detail"
                        } else {
                            "${selected.taskCount} task${if (selected.taskCount == 1) "" else "s"} " +
                                "· tap again to clear"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    allocation.slices.forEach { slice ->
                        LegendRow(
                            slice = slice,
                            isSelected = slice.areaId.sliceKey() == selectedSliceId,
                            isDimmed = selectedSliceId != null &&
                                slice.areaId.sliceKey() != selectedSliceId,
                            onClick = {
                                onSelectSlice(
                                    slice.areaId.sliceKey().takeIf { it != selectedSliceId },
                                )
                            },
                        )
                    }

                    val unassigned = allocation.slices.firstOrNull { it.areaId == null }
                    if (unassigned != null && unassigned.fraction > 0.15f) {
                        UnfiledHint(percent = unassigned.percent, onOpenLifeAreas = onOpenLifeAreas)
                    }
                    EstimateFootnote(allocation)
                    if (inferredTaskCount > 0) {
                        ReEstimateButton(
                            inferredTaskCount = inferredTaskCount,
                            isRunning = isReEstimating,
                            onClick = onReEstimate,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Offers to replace the inferred durations with the model's.
 *
 * Only shown when there is something to fix, and it says how much — an always-on
 * "re-estimate with AI" button invites a run that spends the per-run cap on
 * nothing.
 */
@Composable
private fun ReEstimateButton(inferredTaskCount: Int, isRunning: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isRunning,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        if (isRunning) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Asking the AI…")
        } else {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "Re-estimate $inferredTaskCount duration${if (inferredTaskCount == 1) "" else "s"}",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// ── The trend (what the pie cannot say) ──────────────────────────────

/**
 * A stacked column per bucket of the selected range.
 *
 * Same data as the donut, same order, same colours — this one is about direction
 * rather than share, which is the question a single pie can never answer.
 */
@Composable
private fun TimeTrendCard(trend: TimeTrend, range: AnalyticsRange, selectedSliceId: String?) {
    ChartCard(
        title = "How it moves",
        subtitle = "Tracked time per ${range.bucketNoun}, stacked by life area.",
    ) {
        if (trend.isEmpty) {
            Text(
                "Nothing tracked in this ${range.label.lowercase()} yet — " +
                    "the trend fills in as you tick tasks off.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@ChartCard
        }

        val columns = trend.buckets.map { bucket ->
            StackedColumn(
                label = bucket.label,
                segments = trend.series.mapIndexed { index, series ->
                    StackedSegment(
                        id = series.areaId.sliceKey(),
                        color = series.colorHex.toGoalAccent(),
                        value = bucket.minutes.getOrElse(index) { 0 },
                    )
                },
            )
        }

        Column {
            StackedColumnChart(
                columns = columns,
                maxValue = trend.maxBucketMinutes,
                selectedId = selectedSliceId,
                contentDescription = trend.describe(range),
            )
            trend.busiest?.let { busiest ->
                Text(
                    text = "Busiest: ${busiest.label} · ${formatMinutes(busiest.totalMinutes)}" +
                        if (selectedSliceId != null) " (all areas)" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

// ── Re-estimation review sheet ───────────────────────────────────────

/**
 * Every proposed duration, opt-out-able, with nothing written until "Update" is
 * pressed — the same policy as the Google Tasks import and the Health Connect
 * sync, and for the same reason.
 *
 * Rows the model did not really answer arrive unticked and say so. That is the
 * whole point of the sheet: the analytics card counts how many durations came
 * from the AI, and a silent fallback written as an AI estimate would make that
 * count a lie.
 */
@Composable
private fun DurationBackfillDialog(
    state: BackfillState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text("Re-estimate durations") },
        text = {
            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Asking the AI how long these take…")
                }

                state.error != null -> Text(state.error)

                else -> Column {
                    Text(
                        buildString {
                            append("${state.proposals.size} task")
                            append(if (state.proposals.size == 1) "" else "s")
                            if (state.totalCandidates > state.proposals.size) {
                                append(" of ${state.totalCandidates} without an AI estimate")
                                append(" — run it again for the rest")
                            }
                            append(". ")
                            val unanswered = state.proposals.size - state.answeredCount
                            if (unanswered > 0) {
                                append("The AI did not answer for $unanswered of them, ")
                                append("so those start unticked.")
                            } else {
                                append("Tap a row to include or exclude it.")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.proposals, key = { it.taskId }) { proposal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.isSaving) {
                                        onToggle(proposal.taskId)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = proposal.selected,
                                    onCheckedChange = { onToggle(proposal.taskId) },
                                    enabled = !state.isSaving,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        proposal.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = when {
                                            proposal.isFallback ->
                                                "The AI did not answer — this is a fallback value"
                                            proposal.changesTheChart ->
                                                "${formatMinutes(proposal.inferredMinutes)} → " +
                                                    formatMinutes(proposal.proposedMinutes)
                                            else ->
                                                "${formatMinutes(proposal.proposedMinutes)}" +
                                                    " · confirms the current guess"
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
                TextButton(
                    onClick = onConfirm,
                    enabled = !state.isSaving && state.selectedCount > 0,
                ) {
                    Text(if (state.isSaving) "Updating…" else "Update ${state.selectedCount}")
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

/**
 * The middle of the donut: the window's total while nothing is selected, and the
 * chosen area's own numbers once something is. The total counts up with the ring
 * so the two read as one animation.
 */
@Composable
private fun DonutCenter(allocation: TimeAllocation, selected: TimeSlice?) {
    val progress by rememberChartProgress(key = allocation)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 28.dp),
    ) {
        if (selected == null) {
            Text(
                text = formatMinutes((allocation.totalMinutes * progress).roundToInt()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "tracked",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "${selected.percent}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = selected.colorHex.toGoalAccent(),
            )
            Text(
                text = selected.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatMinutes(selected.minutes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendRow(
    slice: TimeSlice,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit,
) {
    val accent = slice.colorHex.toGoalAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (isDimmed) 0.4f else 1f)),
        )
        Icon(
            imageVector = iconForKey(slice.iconKey),
            contentDescription = null,
            tint = accent.copy(alpha = if (isDimmed) 0.4f else 1f),
            modifier = Modifier
                .padding(start = 10.dp)
                .size(18.dp),
        )
        Text(
            text = slice.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        Text(
            text = formatMinutes(slice.minutes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${slice.percent}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun NoLifeAreasHint(onOpenLifeAreas: () -> Unit) {
    Column {
        Text(
            "Life areas are the slices of this chart — health, studies, career, " +
                "whatever your life is actually made of. Define them once (or pull " +
                "them straight from your Google Tasks lists) and every goal you " +
                "file under one starts reporting here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = onOpenLifeAreas,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(Icons.Outlined.Category, contentDescription = null)
            Text("Set up life areas", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun UnfiledHint(percent: Int, onOpenLifeAreas: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$percent% of this time isn't filed under a life area.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenLifeAreas) { Text("Fix") }
    }
}

/**
 * Says how many of the durations were the model's and how many were inferred.
 * "62 % on Health" means something different when half the minutes were guessed
 * from a point value, and the screen should not pretend otherwise.
 */
@Composable
private fun EstimateFootnote(allocation: TimeAllocation) {
    val estimated = allocation.estimatedTaskCount
    val total = allocation.completedTasks
    Text(
        text = when {
            total == 0 -> ""
            estimated == total -> "Durations estimated by AI for all $total task" +
                if (total == 1) "" else "s"
            estimated == 0 -> "Durations inferred from task difficulty ($total task" +
                (if (total == 1) ")" else "s)")
            else -> "$estimated of $total durations estimated by AI; the rest " +
                "inferred from task difficulty"
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

// ── The two goal-level charts ────────────────────────────────────────

@Composable
private fun ProgressByGoalCard(state: AnalyticsUiState) {
    val bars = state.goals.map { g ->
        BarItem(
            label = g.title.ifBlank { "Untitled" },
            fraction = g.progressFraction,
            color = g.colorHex.toGoalAccent(),
            trailing = "${g.progressPercent}%",
            countUpTo = g.progressPercent,
            countSuffix = "%",
        )
    }
    ChartCard("Progress by goal", "How far along each goal is.") {
        HorizontalBarChart(items = bars)
    }
}

@Composable
private fun TaskFocusCard(state: AnalyticsUiState) {
    val doneByGoal = state.tasks
        .filter { it.isDone && it.goalId != null }
        .groupingBy { it.goalId!! }
        .eachCount()
    val totalDone = doneByGoal.values.sum().coerceAtLeast(1)
    val bars = state.goals
        .map { g ->
            val share = (doneByGoal[g.id] ?: 0).toFloat() / totalDone
            BarItem(
                label = g.title.ifBlank { "Untitled" },
                fraction = share,
                color = g.colorHex.toGoalAccent(MaterialTheme.colorScheme.tertiary),
                trailing = "${(share * 100).roundToInt()}%",
                countUpTo = (share * 100).roundToInt(),
                countSuffix = "%",
            )
        }
        .filter { it.fraction > 0f }

    ChartCard(
        title = "Task focus",
        subtitle = "Share of completed tasks per goal (where your effort goes).",
    ) {
        AnimatedVisibility(
            visible = bars.isNotEmpty(),
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(150)),
        ) {
            HorizontalBarChart(items = bars)
        }
        if (bars.isEmpty()) {
            Text(
                "Complete some tasks to see your focus split.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            content()
        }
    }
}

// ── Formatting ───────────────────────────────────────────────────────

/** One-line summary of the whole chart, for TalkBack. */
private fun TimeAllocation.describe(): String =
    "Time split across ${slices.size} life areas, " +
        slices.joinToString(", ") { "${it.name} ${it.percent}%" }

/**
 * TalkBack's version of the trend. A screen reader cannot follow a column height,
 * so it is given the shape in words: the unit, the busiest bucket, and the totals
 * that make "up" or "down" a claim rather than a picture.
 */
private fun TimeTrend.describe(range: AnalyticsRange): String {
    val busiestLabel = busiest?.let { "busiest ${it.label} at ${formatMinutes(it.totalMinutes)}" }
    return listOfNotNull(
        "Tracked time per ${range.bucketNoun} across ${buckets.size} periods",
        busiestLabel,
        buckets.joinToString(", ") { "${it.label} ${formatMinutes(it.totalMinutes)}" },
    ).joinToString(". ")
}
