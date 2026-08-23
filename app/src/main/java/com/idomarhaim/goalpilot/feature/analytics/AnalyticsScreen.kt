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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.R
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
import com.idomarhaim.goalpilot.ui.components.SuccessFailureRunCard
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.components.rememberChartProgress
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.components.toGoalInk
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    onOpenLifeAreas: () -> Unit,
    /** For `C19`'s no-next-step offers (§4.7, `#64`) — C8's and C9a's existing surfaces. */
    onOpenGoal: (String) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backfill by viewModel.backfill.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolved here rather than inside the effect: stringResource is a composable
    // read, and LaunchedEffect's body is not a composition.
    val messageText = message?.resolve()
    LaunchedEffect(messageText) {
        messageText?.let {
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
        // #57 b -- TRANSPARENT, and this is the fix for "the same backgrounds
        // aren't there" rather than a tidy-up.
        //
        // `Modifier.gpPage` draws the ground, and it is called in exactly one
        // place that matters: `MainActivity`, under `GoalPilotRoot`. Every screen
        // then puts a `Scaffold` on top of it, and `Scaffold`'s containerColor
        // defaults to `colorScheme.background` -- an OPAQUE fill over the whole
        // window. So the ground was drawn and then painted over, on every screen,
        // since the day `gpPage` was written: glass and liquid glass have been
        // rendering translucent panels against a flat colour, which is the exact
        // look `MaterialSpec.kt` says they are defined against ("a translucent
        // panel over a flat ground is not translucent, it is grey").
        //
        // `Observed:` on the Settings screen, 2026-08-22 -- the same render pass
        // frame before and after this one line, with and without the ground.
        // `Inferred:` for the other ten screens, from the same mechanism: none of
        // the twelve `Scaffold(` call sites in `app/src/main` passed a
        // `containerColor` before this commit (checked mechanically, not by eye),
        // so they all took the same opaque default. Not separately rendered --
        // `MaterialRenderPass` photographs one screen.
        //
        // It survived a render pass because `MaterialRenderPass` did not apply
        // `gpPage` either, so its frames agreed with the app -- two instruments
        // wrong in the same direction. Both are fixed together.
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                // #57 b -- the ground has to run behind the bar too, or the
                // Scaffold fix above just moves the seam up by one bar height.
                // `DashboardScreen` already did this; the other ten did not,
                // which is why it never looked like a rule.
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.analytics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.analytics_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(inner))
            state.goals.isEmpty() -> EmptyState(
                title = stringResource(R.string.analytics_empty_title),
                subtitle = stringResource(R.string.analytics_empty_subtitle),
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

                // §4.7: "beside the time donut on analytics -- where the asymmetry
                // sentence lives AND NOWHERE ELSE." Directly under the donut card,
                // which is what "beside" means on a phone: `C17` deliberately put
                // the DIVIDED number (minutes) and the UNDIVIDED one (successes) on
                // one screen, and the note between them is the only place that
                // asymmetry can be stated where both are visible.
                SuccessFailureRunCard(
                    run = state.run,
                    onSelectRange = viewModel::selectSuccessRange,
                    onOpenGoal = onOpenGoal,
                    showAsymmetryNote = true,
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

/** Turns a [AnalyticsMessage] into words, with its count direction-isolated. */
@Composable
@ReadOnlyComposable
private fun AnalyticsMessage.resolve(): String = when (this) {
    AnalyticsMessage.AllTasksAlreadyEstimated ->
        stringResource(R.string.analytics_error_all_estimated)

    AnalyticsMessage.UpdateFailed ->
        stringResource(R.string.analytics_update_failed)

    is AnalyticsMessage.Updated ->
        pluralStringResource(R.plurals.analytics_updated, count, count.isolated())
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
                label = { Text(range.label()) },
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
        title = stringResource(R.string.analytics_allocation_title),
        // rangeLabel arrives already isolated from AnalyticsRange.windowLabel —
        // it is a date range, and `Aug 3 – Aug 9` reverses without it (§4.8).
        subtitle = stringResource(R.string.analytics_allocation_subtitle, rangeLabel),
    ) {
        when {
            !hasLifeAreas -> NoLifeAreasHint(onOpenLifeAreas)

            allocation.isEmpty -> Text(
                stringResource(R.string.analytics_allocation_empty, range.labelInline()),
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
                            stringResource(R.string.analytics_tap_slice)
                        } else {
                            pluralStringResource(
                                R.plurals.analytics_slice_selected,
                                selected.taskCount,
                                selected.taskCount.isolated(),
                            )
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
            Text(stringResource(R.string.analytics_asking_ai))
        } else {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                pluralStringResource(
                    R.plurals.analytics_reestimate,
                    inferredTaskCount,
                    inferredTaskCount.isolated(),
                ),
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
        title = stringResource(R.string.analytics_trend_title),
        subtitle = stringResource(R.string.analytics_trend_subtitle, range.bucketNoun()),
    ) {
        if (trend.isEmpty) {
            Text(
                stringResource(R.string.analytics_trend_empty, range.labelInline()),
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
                        // §4.1's `.tag`. The band draws it wherever it fits; see
                        // `StackedColumnChart.segmentLabelFits` for the residual.
                        label = series.name,
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
                // Two whole strings rather than one plus an appended "(all areas)":
                // a trailing parenthetical is not a suffix in every language.
                val duration = formatMinutes(busiest.totalMinutes).isolated()
                Text(
                    text = if (selectedSliceId != null) {
                        stringResource(
                            R.string.analytics_trend_busiest_all_areas,
                            busiest.label,
                            duration,
                        )
                    } else {
                        stringResource(R.string.analytics_trend_busiest, busiest.label, duration)
                    },
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
    // The dialog would otherwise render in the DEVICE language while mirroring
    // right-to-left perfectly — see ui/locale/LocaleAwareWindows.kt, which is
    // also the only place that opens a window in this app.
    AppAlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = {
            Text(stringResource(R.string.analytics_backfill_title))
        },
        text = {
            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.analytics_backfill_loading))
                }

                state.error != null -> Text(state.error.resolve())

                else -> Column {
                    Text(
                        backfillIntro(state),
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
                                            proposal.noModelAnswer -> stringResource(
                                                R.string.analytics_backfill_fallback,
                                            )
                                            // A duration RANGE — §4.8's named
                                            // defect. Both ends isolated so the
                                            // arrow cannot swap them in Hebrew.
                                            proposal.changesTheChart -> stringResource(
                                                R.string.analytics_backfill_change,
                                                formatMinutes(proposal.inferredMinutes).isolated(),
                                                formatMinutes(proposal.proposedMinutes).isolated(),
                                            )
                                            else -> stringResource(
                                                R.string.analytics_backfill_confirms,
                                                formatMinutes(proposal.proposedMinutes).isolated(),
                                            )
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
                    Text(
                        if (state.isSaving) {
                            stringResource(R.string.analytics_backfill_updating)
                        } else {
                            stringResource(
                                R.string.analytics_backfill_update,
                                state.selectedCount.isolated(),
                            )
                        },
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(
                    if (state.error != null) {
                        stringResource(R.string.analytics_backfill_close)
                    } else {
                        stringResource(R.string.analytics_backfill_cancel)
                    },
                )
            }
        },
    )
}

/**
 * The sheet's opening sentence, as **one** resource per situation.
 *
 * This replaced a `buildString { append(…) }` of six fragments. Fragment
 * concatenation cannot be translated: the order of "N tasks", "of M", "run it
 * again" and "those start unticked" is a property of the language, and Hebrew
 * does not use English's. Four situations, four complete sentences.
 */
@Composable
@ReadOnlyComposable
private fun backfillIntro(state: BackfillState): String {
    val shown = state.proposals.size
    val isPartial = state.totalCandidates > shown
    val unanswered = shown - state.answeredCount

    return when {
        isPartial && unanswered > 0 -> pluralStringResource(
            R.plurals.analytics_backfill_intro_partial_unanswered,
            shown,
            shown.isolated(),
            state.totalCandidates.isolated(),
            unanswered.isolated(),
        )

        isPartial -> pluralStringResource(
            R.plurals.analytics_backfill_intro_partial,
            shown,
            shown.isolated(),
            state.totalCandidates.isolated(),
        )

        unanswered > 0 -> pluralStringResource(
            R.plurals.analytics_backfill_intro_unanswered,
            shown,
            shown.isolated(),
            unanswered.isolated(),
        )

        else -> pluralStringResource(R.plurals.analytics_backfill_intro, shown, shown.isolated())
    }
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
                text = formatMinutes((allocation.totalMinutes * progress).roundToInt()).isolated(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.analytics_donut_tracked),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = percentText(selected.percent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                // Ink, not the slice fill: `#57` a's palette is authored for fills
                // and clears 3:1, and this is type on the card behind the donut.
                color = selected.colorHex.toGoalInk(),
            )
            Text(
                text = selected.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatMinutes(selected.minutes).isolated(),
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
    val ink = slice.colorHex.toGoalInk()
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
            text = formatMinutes(slice.minutes).isolated(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = percentText(slice.percent),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ink,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun NoLifeAreasHint(onOpenLifeAreas: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.analytics_no_life_areas),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = onOpenLifeAreas,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(Icons.Outlined.Category, contentDescription = null)
            Text(
                stringResource(R.string.analytics_set_up_life_areas),
                modifier = Modifier.padding(start = 8.dp),
            )
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
            stringResource(R.string.analytics_unfiled, percentText(percent)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenLifeAreas) {
            Text(stringResource(R.string.analytics_unfiled_fix))
        }
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
            estimated == total -> pluralStringResource(
                R.plurals.analytics_estimates_all,
                total,
                total.isolated(),
            )
            estimated == 0 -> pluralStringResource(
                R.plurals.analytics_estimates_none,
                total,
                total.isolated(),
            )
            else -> stringResource(
                R.string.analytics_estimates_mixed,
                estimated.isolated(),
                total.isolated(),
            )
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

// ── The two goal-level charts ────────────────────────────────────────

/**
 * *Progress by goal* — and it charts **measured goals only** (`#66`).
 *
 * An unmeasured goal used to get a bar whose length, `trailing` label and
 * count-up animation were all `currentValue / targetValue`, where `targetValue`
 * is the `100.0` default a goal gets for saying nothing (§1.3, `E6`). So the
 * chart ranked goals by a fraction of a target nobody set, and animated the
 * digit up to it, which is §0.3's *second number that quietly disagrees* with a
 * count-up attached.
 *
 * §4.4 already refuses a neighbouring version of this shape — *"a percentage is
 * a fraction of its own target, so ranking by movement partly ranks how modest
 * the goals are"* — which is why the effort chart orders minutes. With no target
 * at all there is not even a modest ranking left to make.
 *
 * **Excluded, then counted.** Dropping the goals silently would make the chart
 * claim to describe every goal while describing a subset, so the footnote states
 * how many are missing. That is the same answer
 * [BuildWidgetSnapshotUseCase][com.idomarhaim.goalpilot.domain.usecase.BuildWidgetSnapshotUseCase]
 * already reached independently with `goalsWithoutMeasure`, which is a reason to
 * trust it rather than a coincidence.
 *
 * `TaskFocusCard` below is deliberately **not** given the same treatment: its
 * share is `completed tasks on this goal / all completed tasks`, which is
 * arithmetic on facts a goal has whether or not it counts anything.
 */
@Composable
internal fun ProgressByGoalCard(state: AnalyticsUiState) {
    val untitled = stringResource(R.string.analytics_untitled_goal)
    val percentFormat = stringResource(R.string.analytics_percent)
    val measured = state.goals.filterNot { it.isUnmeasured }
    val unmeasuredCount = state.goals.size - measured.size
    val bars = measured.map { g ->
        BarItem(
            label = g.title.ifBlank { untitled },
            fraction = g.progressFraction,
            color = g.colorHex.toGoalAccent(),
            trailing = percentFormat.formatPercent(g.progressPercent),
            countUpTo = g.progressPercent,
            countSuffix = "%",
        )
    }
    ChartCard(
        stringResource(R.string.analytics_progress_title),
        stringResource(R.string.analytics_progress_subtitle),
    ) {
        HorizontalBarChart(items = bars)
        if (unmeasuredCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.analytics_progress_unmeasured,
                    unmeasuredCount,
                    unmeasuredCount.isolated(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun TaskFocusCard(state: AnalyticsUiState) {
    val untitled = stringResource(R.string.analytics_untitled_goal)
    val percentFormat = stringResource(R.string.analytics_percent)
    val doneByGoal = state.tasks
        .filter { it.isDone && it.goalId != null }
        .groupingBy { it.goalId!! }
        .eachCount()
    val totalDone = doneByGoal.values.sum().coerceAtLeast(1)
    val bars = state.goals
        .map { g ->
            val share = (doneByGoal[g.id] ?: 0).toFloat() / totalDone
            val percent = (share * 100).roundToInt()
            BarItem(
                label = g.title.ifBlank { untitled },
                fraction = share,
                color = g.colorHex.toGoalAccent(MaterialTheme.colorScheme.tertiary),
                trailing = percentFormat.formatPercent(percent),
                countUpTo = percent,
                countSuffix = "%",
            )
        }
        .filter { it.fraction > 0f }

    ChartCard(
        title = stringResource(R.string.analytics_focus_title),
        subtitle = stringResource(R.string.analytics_focus_subtitle),
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
                stringResource(R.string.analytics_focus_empty),
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

/**
 * Applies an already-resolved `%1$d%%` pattern and isolates the result.
 *
 * A plain function rather than [percentText] wherever the value is produced
 * inside a `map` — the lambda is not a composition, so the pattern is read once
 * outside it and applied here.
 */
private fun String.formatPercent(percent: Int): String =
    String.format(Locale.getDefault(), this, percent).isolated()

/** One-line summary of the whole chart, for TalkBack. */
@Composable
@ReadOnlyComposable
private fun TimeAllocation.describe(): String {
    val separator = stringResource(R.string.analytics_a11y_separator)
    val sliceFormat = stringResource(R.string.analytics_a11y_slice)
    val percentFormat = stringResource(R.string.analytics_percent)
    val sliceList = slices.joinToString(separator) { slice ->
        String.format(
            Locale.getDefault(),
            sliceFormat,
            slice.name,
            percentFormat.formatPercent(slice.percent),
        )
    }
    return stringResource(R.string.analytics_a11y_allocation, slices.size.isolated(), sliceList)
}

/**
 * TalkBack's version of the trend. A screen reader cannot follow a column height,
 * so it is given the shape in words: the unit, the busiest bucket, and the totals
 * that make "up" or "down" a claim rather than a picture.
 */
@Composable
@ReadOnlyComposable
private fun TimeTrend.describe(range: AnalyticsRange): String {
    val separator = stringResource(R.string.analytics_a11y_separator)
    val bucketFormat = stringResource(R.string.analytics_a11y_bucket)
    val head = stringResource(
        R.string.analytics_a11y_trend,
        range.bucketNoun(),
        buckets.size.isolated(),
    )
    val busiestLabel = busiest?.let {
        stringResource(
            R.string.analytics_a11y_busiest,
            it.label,
            formatMinutes(it.totalMinutes).isolated(),
        )
    }
    val bucketList = buckets.joinToString(separator) { bucket ->
        String.format(
            Locale.getDefault(),
            bucketFormat,
            bucket.label,
            formatMinutes(bucket.totalMinutes).isolated(),
        )
    }
    return listOfNotNull(head, busiestLabel, bucketList).joinToString(". ")
}
