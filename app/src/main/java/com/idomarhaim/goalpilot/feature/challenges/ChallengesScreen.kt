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
import androidx.compose.material.icons.filled.PersonAddAlt1
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
import com.idomarhaim.goalpilot.domain.model.MeasureKind
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
    val goalLink by viewModel.goalLink.collectAsStateWithLifecycle()
    val invite by viewModel.invite.collectAsStateWithLifecycle()
    val invitePendingId by viewModel.invitePendingId.collectAsStateWithLifecycle()
    val measureChange by viewModel.measureChange.collectAsStateWithLifecycle()
    val approvingId by viewModel.approvingChallengeId.collectAsStateWithLifecycle()
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

            // `invites` counts here too: a screen that says "No challenges yet"
            // above a live invitation is telling the user the opposite of what the
            // row beneath it says.
            if (state.mine.isEmpty() && state.discoverable.isEmpty() &&
                state.invites.isEmpty()
            ) {
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

            // INVITES GO FIRST, AND NOWHERE ELSE (§1's receiving half).
            //
            // An invite is somebody asking you a question, so it outranks the list of
            // things you are already doing -- but it is an OFFER, so it gets no badge, no
            // count and no notification. It is a row that is there, and then is not.
            //
            // No section header when there is exactly one: "Invites" above a single row
            // that already says who invited you to what is a label for a list of one.
            if (state.invites.isNotEmpty()) {
                if (state.invites.size > 1) item { SectionHeader("Invites") }
                items(state.invites, key = { it.id }) { invitation ->
                    ChallengeInviteRow(
                        invite = invitation,
                        isBusy = invitePendingId == invitation.id,
                        onJoin = { viewModel.acceptInvite(invitation.id) },
                        onDismiss = { viewModel.declineInvite(invitation.id) },
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
                        onLinkGoal = { viewModel.openGoalLink(card) },
                        onInvite = { viewModel.openInvite(card) },
                        onChangeMeasure = { viewModel.openMeasureChange(card) },
                        onApproveMeasure = { viewModel.approveMeasureChange(card) },
                        onWithdrawMeasure = {
                            viewModel.withdrawMeasureChange(card.challenge.id)
                        },
                        isApproving = approvingId == card.challenge.id,
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
            onMeasureKind = viewModel::onMeasureKindChange,
            onMeasureWord = viewModel::onMeasureWordChange,
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

    if (goalLink.isVisible) {
        GoalLinkSheet(
            state = goalLink,
            onLink = viewModel::linkGoal,
            onCreateTitle = viewModel::onCreateTitleChange,
            onCreateTarget = viewModel::onCreateTargetChange,
            onCreate = viewModel::createAndLinkGoal,
            onDismiss = viewModel::dismissGoalLink,
        )
    }

    if (measureChange.isVisible) {
        MeasureChangeDialog(
            state = measureChange,
            onKind = viewModel::onChangeKind,
            onWord = viewModel::onChangeWord,
            onPropose = viewModel::proposeMeasureChange,
            onDismiss = viewModel::dismissMeasureChange,
        )
    }

    if (invite.isVisible) {
        InviteSheet(
            state = invite,
            onInvite = viewModel::sendInvite,
            onDismiss = viewModel::dismissInvite,
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
    onLinkGoal: () -> Unit,
    onInvite: () -> Unit,
    onChangeMeasure: () -> Unit,
    onApproveMeasure: () -> Unit,
    onWithdrawMeasure: () -> Unit,
    isApproving: Boolean,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GpCard(onClick = onOpenStandings, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconFor(card.challenge.measure?.kind),
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
                // AN ICON BUTTON IN THE HEADER, NOT A FOURTH BUTTON IN THE ACTION ROW
                // AND NOT A MENU ITEM.
                //
                // Ido's complaint was that he *could not find a way* to invite anybody, so
                // burying this behind the overflow would answer the letter of the report
                // and not its substance. The action row below is the other candidate and
                // it is already at its width limit on his own phone (384 dp, font 1.15):
                // "Score from a goal" + "Type a score" + "Standings" is three, and a
                // fourth wraps. An icon costs no row width and sits where the card's other
                // affordance already is.
                IconButton(onClick = onInvite) {
                    Icon(
                        Icons.Filled.PersonAddAlt1,
                        contentDescription = "Invite a friend to “${card.challenge.title}”",
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
                            // THE OVERFLOW IS RIGHT FOR THIS ONE AND WAS WRONG FOR THE
                            // INVITE, and the difference is not consistency.
                            //
                            // Ido's report was that he could not FIND a way to invite
                            // anybody, so that affordance had to be visible. Changing what
                            // a challenge counts is a rare, owner-only, consequential act
                            // that nobody has reported being unable to find -- and putting
                            // it on the card beside "Type a score" would invite a mis-tap
                            // that asks four people to reset their scores.
                            DropdownMenuItem(
                                text = { Text("Change what this counts") },
                                onClick = { menuOpen = false; onChangeMeasure() },
                                enabled = !card.hasPendingMeasureChange,
                            )
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
                            card.challenge.metricWord,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // WHERE THIS CHALLENGE'S NUMBER COMES FROM, ON THE USER'S OWN CARD.
            //
            // §6 makes a linked challenge score itself, which is silent by design -- so the
            // one place it has to be legible is here, on the card of the person whose score
            // it is. Not on the standings row: that is a claim about somebody else and only
            // the exception speaks there (see `ReportedBadge`).
            if (card.canLinkGoal) {
                Text(
                    if (card.isLinked) {
                        "Scoring itself from your linked goal."
                    } else {
                        "Not linked yet — you are typing this score."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else if (card.challenge.isUnmeasured && card.canReportScore) {
                // A pre-§6 challenge whose `metricUnit` was the "points" default. §6
                // deletes that rather than re-homing it, so there is no kind to match a
                // goal against and the honest thing is to say so rather than offer an
                // empty picker.
                Text(
                    "This challenge was made before scores could come from a goal, so " +
                        "it has no measure. Its owner can set one.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Linking is the PRIMARY action once it is available: it is what §6 means a
                // challenge to do, and typing a number is the fallback. Reporting keeps its
                // place rather than being demoted into a menu -- somebody with no goal of
                // the right kind still has to be able to compete today.
                if (card.canLinkGoal) {
                    Button(onClick = onLinkGoal, enabled = card.canReportScore) {
                        Text(if (card.isLinked) "Change goal" else "Score from a goal")
                    }
                    TextButton(onClick = onReportScore, enabled = card.canReportScore) {
                        Text("Type a score")
                    }
                } else {
                    Button(onClick = onReportScore, enabled = card.canReportScore) {
                        Text("Report score")
                    }
                }
                TextButton(onClick = onOpenStandings) { Text("Standings") }
            }
            // §3's banner sits BELOW the actions, not above them. A pending measure
            // change is real but not urgent -- the challenge is still scoring in the old
            // unit while it waits -- so it must not push the thing the user came to do off
            // the bottom of the card.
            if (card.hasPendingMeasureChange) {
                PendingMeasureBanner(
                    card = card,
                    isBusy = isApproving,
                    onApprove = onApproveMeasure,
                    onWithdraw = onWithdrawMeasure,
                )
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
                imageVector = iconFor(entry.challenge.measure?.kind),
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

/**
 * A glyph for a challenge, from its measure **kind**.
 *
 * `ChallengeType` used to supply this, and §6 deleted the enum: it was purely
 * presentational — nothing ever branched on it to source a score — and a [MeasureKind]
 * covers the half of it that meant anything (`STEPS` is a `COUNT`, `SLEEP` a `DURATION`,
 * `RUNNING` a `DISTANCE`). One consequence is visible and accepted: a steps race and a
 * books race are both `COUNT` and now share a glyph, where the old enum gave steps its own.
 * That is the honest price of deleting a field nothing computed with — the alternative is
 * keeping a second, decorative classification beside the real one, which is §0.3's
 * most-repeated finding in miniature.
 *
 * A null kind is a pre-§6 challenge with no measure; the trophy is the neutral default the
 * old `CUSTOM` used, so nothing about it reads as an error.
 */
internal fun iconFor(kind: MeasureKind?): ImageVector = when (kind) {
    MeasureKind.DISTANCE -> Icons.AutoMirrored.Filled.DirectionsRun
    MeasureKind.COUNT -> Icons.AutoMirrored.Filled.DirectionsWalk
    MeasureKind.DURATION -> Icons.Filled.NightlightRound
    MeasureKind.MASS -> Icons.Filled.FitnessCenter
    else -> Icons.Filled.EmojiEvents
}

internal fun participantSummary(count: Int): String =
    if (count == 1) "1 person in" else "$count people in"

@Composable
internal fun MeasureIconBadge(kind: MeasureKind, modifier: Modifier = Modifier) {
    Icon(
        imageVector = iconFor(kind),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(20.dp),
    )
}
