package com.idomarhaim.goalpilot.feature.challenges

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppDropdownMenu

/**
 * Shared & competitive challenges (spec §6 nice-to-have, §7): the ones you are
 * in, with live standings, and the ones you could join.
 *
 * Joining writes your own participant row, never an edit to the challenge — the
 * data layer handles that; see `CHANGELOG/2026-08-04/challenges.md` for why the
 * distinction is load-bearing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    viewModel: ChallengesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val scoreEntry by viewModel.scoreEntry.collectAsStateWithLifecycle()
    val detailId by viewModel.detailId.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var pendingLeave by remember { mutableStateOf<ChallengeCard?>(null) }
    var pendingDelete by remember { mutableStateOf<ChallengeCard?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Challenges") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openEditor,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New challenge") },
            )
        },
    ) { inner ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(inner))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { error ->
                item {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (state.mine.isEmpty() && state.discoverable.isEmpty()) {
                item {
                    EmptyState(
                        title = "No challenges yet",
                        subtitle = "Start one and share it — anyone signed in can find it " +
                            "here and join. Whoever reports the highest score leads.",
                        icon = Icons.Outlined.EmojiEvents,
                        modifier = Modifier.heightIn(min = 320.dp),
                    )
                }
            }

            if (state.mine.isNotEmpty()) {
                item { SectionHeader("Your challenges") }
                items(state.mine, key = { it.challenge.id }) { card ->
                    MyChallengeCard(
                        card = card,
                        onOpenStandings = { viewModel.openDetail(card.challenge.id) },
                        onReportScore = { viewModel.openScoreEntry(card) },
                        onLeave = { pendingLeave = card },
                        onDelete = { pendingDelete = card },
                    )
                }
            }

            if (state.discoverable.isNotEmpty()) {
                item { SectionHeader("Discover") }
                items(state.discoverable, key = { it.challenge.id }) { entry ->
                    DiscoverChallengeCard(
                        entry = entry,
                        onJoin = { viewModel.join(entry.challenge.id) },
                    )
                }
            }
        }
    }

    if (editor.isVisible) {
        ChallengeEditorDialog(
            state = editor,
            onTitle = viewModel::onTitleChange,
            onDescription = viewModel::onDescriptionChange,
            onType = viewModel::onTypeChange,
            onMetricUnit = viewModel::onMetricUnitChange,
            onStart = viewModel::onStartChange,
            onEnd = viewModel::onEndChange,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor,
        )
    }

    if (scoreEntry.isVisible) {
        ScoreEntryDialog(
            state = scoreEntry,
            onValue = viewModel::onScoreChange,
            onSubmit = viewModel::submitScore,
            onDismiss = viewModel::dismissScoreEntry,
        )
    }

    // Looked up rather than held: the sheet then follows the live snapshot, so a
    // score someone else reports moves the rows while it is open.
    detailId?.let { id ->
        state.mine.firstOrNull { it.challenge.id == id }?.let { card ->
            StandingsSheet(card = card, onDismiss = viewModel::dismissDetail)
        }
    }

    pendingLeave?.let { card ->
        AppAlertDialog(
            onDismissRequest = { pendingLeave = null },
            title = { Text("Leave “${card.challenge.title}”?") },
            text = {
                Text(
                    "Your score is removed from the standings. The challenge itself " +
                        "stays, and you can join it again from Discover.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leave(card.challenge.id)
                    pendingLeave = null
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { pendingLeave = null }) { Text("Cancel") }
            },
        )
    }

    pendingDelete?.let { card ->
        AppAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “${card.challenge.title}”?") },
            text = {
                Text(
                    "It disappears for everyone who joined. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChallenge(card.challenge.id)
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

@Composable
internal fun MyChallengeCard(
    card: ChallengeCard,
    onOpenStandings: () -> Unit,
    onReportScore: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GpCard(onClick = onOpenStandings, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconFor(card.challenge.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(
                        card.challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        participantSummary(card.participantCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More for “${card.challenge.title}”",
                        )
                    }
                    AppDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Leave") },
                            onClick = { menuOpen = false; onLeave() },
                        )
                        if (card.isOwner) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PhaseChip(card.phase, card.challenge)
                card.myStanding?.let { standing ->
                    Text(
                        "#${standing.rank} · ${ChallengesViewModel.format(standing.score)} " +
                            card.challenge.metricUnit,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReportScore, enabled = card.canReportScore) {
                    Text("Report score")
                }
                TextButton(onClick = onOpenStandings) { Text("Standings") }
            }
            if (!card.canReportScore) {
                Text(
                    when (card.phase) {
                        ChallengePhase.UPCOMING -> "Scores open when the challenge starts."
                        else -> "This challenge is over — the standings are final."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
internal fun DiscoverChallengeCard(entry: DiscoverableChallenge, onJoin: () -> Unit) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconFor(entry.challenge.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    entry.challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.challenge.description.isNotBlank()) {
                    Text(
                        entry.challenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                PhaseChip(entry.phase, entry.challenge)
            }
            // Disabled rather than hidden: an ended challenge still belongs in the
            // list, and a card with no action at all reads as a broken row. The
            // label stays "Join" either way — the chip beside it already says
            // "Ended", and saying it twice on one card reads as two states.
            Button(
                onClick = onJoin,
                enabled = entry.canJoin,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("Join")
            }
        }
    }
}

@Composable
internal fun PhaseChip(phase: ChallengePhase, challenge: Challenge) {
    val label = when (phase) {
        ChallengePhase.UPCOMING -> "Starts ${DateTimeUtils.formatDay(challenge.startAtEpochMillis)}"
        // `endAt` is the exclusive bound — the following midnight — so the last
        // day the challenge actually runs is the millisecond before it.
        ChallengePhase.ACTIVE ->
            if (challenge.endAtEpochMillis > 0L) {
                "Ends ${DateTimeUtils.formatDay(challenge.endAtEpochMillis - 1)}"
            } else {
                "Open-ended"
            }

        ChallengePhase.ENDED -> "Ended"
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.height(28.dp),
    )
}

// ── Shared bits ──────────────────────────────────────────────────────

internal fun iconFor(type: ChallengeType): ImageVector = when (type) {
    ChallengeType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    ChallengeType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
    ChallengeType.SLEEP -> Icons.Filled.NightlightRound
    ChallengeType.WORKOUTS -> Icons.Filled.FitnessCenter
    ChallengeType.CUSTOM -> Icons.Filled.EmojiEvents
}

internal fun labelFor(type: ChallengeType): String = when (type) {
    ChallengeType.RUNNING -> "Running"
    ChallengeType.STEPS -> "Steps"
    ChallengeType.SLEEP -> "Sleep"
    ChallengeType.WORKOUTS -> "Workouts"
    ChallengeType.CUSTOM -> "Custom"
}

internal fun participantSummary(count: Int): String =
    if (count == 1) "1 person in" else "$count people in"

@Composable
internal fun TypeIconBadge(type: ChallengeType, modifier: Modifier = Modifier) {
    Icon(
        imageVector = iconFor(type),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(20.dp),
    )
}
