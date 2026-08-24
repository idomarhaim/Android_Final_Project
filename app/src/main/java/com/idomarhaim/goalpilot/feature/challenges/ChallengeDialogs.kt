package com.idomarhaim.goalpilot.feature.challenges

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.MeasureChangeConsequence
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ChallengeStanding
import com.idomarhaim.goalpilot.domain.model.InviteCandidate
import com.idomarhaim.goalpilot.feature.goals.label
import com.idomarhaim.goalpilot.ui.components.Avatar
import com.idomarhaim.goalpilot.ui.components.FreshnessNote
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppDatePickerDialog
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet

// ── Create ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengeEditorDialog(
    state: ChallengeEditorState,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onMeasureKind: (MeasureKind) -> Unit,
    onMeasureWord: (String) -> Unit,
    onStart: (Long?) -> Unit,
    onEnd: (Long?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var picking by remember { mutableStateOf<DateField?>(null) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New challenge") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitle,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescription,
                    label = { Text("What are you competing on?") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                // §6: a challenge names a MEASURE, and it is not optional — "there is
                // nothing to compare without a shared unit". The closed kind is what a
                // participant's goal is matched against; the word beside it is theirs.
                // This is the same two-field shape the goal editor uses, deliberately:
                // a challenge and a goal are the same object measured the same way.
                Text(
                    "What does it count?",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeasureKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kind == state.measureKind,
                            onClick = { onMeasureKind(kind) },
                            label = { Text(kind.label()) },
                            leadingIcon = { MeasureIconBadge(kind) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.measureWord,
                    onValueChange = onMeasureWord,
                    label = { Text("Measured in") },
                    singleLine = true,
                    // Deliberately not "points". §6 deletes that default rather than
                    // re-homing it: points rank by TIME LOGGED, and that is the wrong
                    // race for anything about an outcome.
                    supportingText = {
                        Text("Everyone's score is in this unit — steps, km, hours…")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Each person scores this from one of their own goals, so it moves " +
                        "on its own — from Health Connect, from tasks, from anything " +
                        "that already logs progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Text(
                    "Dates (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Text(
                    "Leave both empty for an open-ended challenge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton(
                        label = "Starts",
                        epochMillis = state.startAtEpochMillis,
                        onPick = { picking = DateField.START },
                        onClear = { onStart(null) },
                        modifier = Modifier.weight(1f),
                    )
                    DateButton(
                        label = "Ends",
                        // The stored bound is exclusive, so the day the user chose
                        // is the millisecond before it — never the bound itself.
                        epochMillis = state.endAtEpochMillis.takeIf { it > 0L }?.minus(1) ?: 0L,
                        onPick = { picking = DateField.END },
                        onClear = { onEnd(null) },
                        modifier = Modifier.weight(1f),
                    )
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
                Text(if (state.isSaving) "Creating…" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )

    picking?.let { field ->
        val pickerState = rememberDatePickerState()
        AppDatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utc ->
                            when (field) {
                                DateField.START -> onStart(localStartOfPickedDay(utc))
                                DateField.END -> onEnd(exclusiveEndOfPickedDay(utc))
                            }
                        }
                        picking = null
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class DateField { START, END }

@Composable
private fun DateButton(
    label: String,
    epochMillis: Long,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val set = epochMillis > 0L
    OutlinedButton(
        onClick = { if (set) onClear() else onPick() },
        modifier = modifier.semantics {
            this.contentDescription =
                if (set) "$label ${DateTimeUtils.formatDay(epochMillis)}, tap to clear"
                else "Set the $label date"
        },
    ) {
        Text(
            text = if (set) DateTimeUtils.formatDay(epochMillis) else label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Report a score ───────────────────────────────────────────────────

@Composable
internal fun ScoreEntryDialog(
    state: ScoreEntryState,
    onValue: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report your score") },
        text = {
            Column {
                Text(
                    state.challengeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.value,
                    onValueChange = onValue,
                    label = { Text("Total ${state.metricWord}") },
                    singleLine = true,
                    isError = state.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "This replaces your total, it does not add to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                // Said BEFORE the write, not after it. A linked challenge is scoring
                // itself; replacing that with a typed number is a real decision, and
                // discovering it afterwards from a badge on your own row would be the
                // app having made it for you.
                if (state.replacesLink) {
                    Text(
                        "This challenge is currently scoring itself from one of your " +
                            "goals. Typing a number takes it off that goal, and your " +
                            "row will say the score was reported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )
}

// ── Standings ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StandingsSheet(card: ChallengeCard, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        StandingsList(card)
    }
}

/**
 * The standings themselves, without the sheet around them.
 *
 * **Split out for a reason that is not style.** `AppModalBottomSheet` renders in a
 * **window of its own**, so nothing in a Compose test can photograph it: `onRoot()`
 * refuses with *"expected exactly 1 node but found 2 that satisfy (isRoot)"*, and every
 * other root selector lands on the host window, which — once the sheet has taken the
 * content away — is a full-screen rectangle of flat background. `Observed:` 2026-08-24,
 * twice: a 1344x2992 blank that passed every size floor a render pass had.
 *
 * The badge below is a claim about **another user** shown to everyone in the challenge,
 * so *"does it read as information or as an accusation?"* is the question this feature is
 * actually risky for — and it is answerable only by looking. A surface that cannot be
 * photographed cannot be reviewed, which makes this seam part of the feature rather than
 * a favour to a test. `ChallengeProvenanceRenderPass` is the camera.
 */
@Composable
internal fun StandingsList(card: ChallengeCard, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text(card.challenge.title, style = MaterialTheme.typography.titleLarge)
            if (card.challenge.description.isNotBlank()) {
                Text(
                    card.challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                if (card.challenge.isUnmeasured) {
                    // A pre-§6 challenge whose `metricUnit` was the "points" default.
                    // §6 deletes that rather than re-homing it, so the honest caption
                    // says the measure is missing instead of inventing a unit.
                    "${participantSummary(card.participantCount)} · no measure set"
                } else {
                    "${participantSummary(card.participantCount)} · scored in " +
                        card.challenge.metricWord
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            // The as-of caption, unconditional — drawn the same online and offline,
            // because a standings row fetched forty minutes ago over perfect Wi-Fi
            // is exactly as stale as one served from cache (#50, spec §5.3 §2).
            // Suppressed only when there is no stamp to state; see [FreshnessNote].
            if (card.standingsFreshness.hasStamp) {
                FreshnessNote(
                    "Standings as of " +
                        DateTimeUtils.formatAsOf(card.standingsFreshness.asOfEpochMillis),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            // Never fetched on this device: the participants are somebody else's
            // documents, so an empty cached read means the app does not know whether
            // anyone is in this challenge — not that nobody is (#50, spec §5.3 §3).
            if (card.standingsFreshness.neverLoaded) {
                FreshnessNote("Not loaded yet", modifier = Modifier.padding(vertical = 16.dp))
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(card.standings, key = { it.uid }) { standing ->
                    // THE BADGE HANGS BELOW THE ROW; IT IS NOT INSIDE IT.
                    //
                    // Putting the badge in a Column beside the name is the obvious
                    // arrangement and it is wrong, because a centred Row then centres on a
                    // block that is two lines tall for one participant and one line tall
                    // for everybody else — so the badged person's NAME floats up and their
                    // rank, avatar and score drift down, and the list loses the single
                    // baseline that makes standings scannable.
                    //
                    // `Observed:` 2026-08-24, the first frame of
                    // `ChallengeProvenanceRenderPass` — `Ann`'s name sat visibly higher
                    // than `Yonatan Ben-Shimon`'s and `Ido (you)`'s in the same list. Every
                    // assertion was green: the words were right, the ranks were right, and
                    // nothing but the picture showed it.
                    //
                    // It matters beyond tidiness. A row that is shaped differently from its
                    // neighbours is *marked*, and this badge is a claim about another
                    // person — `C4`'s register is that the app never asserts an intrinsic
                    // edge by itself, and singling a row out by geometry does exactly that
                    // in a way no wording review would catch.
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Ranks arrive already stamped, with ties sharing one —
                            // 1, 1, 3. Nothing here re-derives them from position.
                            Text(
                                "#${standing.rank}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(width = 38.dp, height = 24.dp),
                            )
                            Avatar(
                                photoUrl = standing.photoUrl,
                                name = standing.displayName.ifBlank { "?" },
                                size = 32.dp,
                            )
                            Text(
                                text = standing.displayName.ifBlank { "Someone" } +
                                    if (standing.isCurrentUser) " (you)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (standing.isCurrentUser) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                            )
                            Text(
                                ChallengesViewModel.format(standing.score),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        // Indented to the name, so it reads as a footnote to this row
                        // rather than as a second row of its own.
                        ReportedBadge(
                            standing = standing,
                            metricWord = card.challenge.metricWord,
                            modifier = Modifier.padding(start = 82.dp),
                        )
                    }
                }
            }
        }
    }

/**
 * The provenance line under a standings row — Ido's third ask on `#23`, 2026-08-24:
 *
 * > *"If someone updated manually and it was not updated through HEALTH CONNECT, then it
 * > should say there who performed the update and what they updated."*
 *
 * **It draws nothing at all unless the score was typed**, and that way round is the whole
 * design. A challenge is *meant* to score itself from each participant's goal (§6), so a
 * derived row is the ordinary case and a badge on it would be noise on every row; a row
 * nobody has scored yet has nothing to say either. Only the exception speaks.
 *
 * ⚠️ **The register is factual and never accusatory.** It says *what happened* — this person
 * typed this number on this day — and stops. `C4`'s rule is the one to match: the app never
 * asserts an intrinsic edge by itself, and it must not here either, because this is a claim
 * about *another user* rendered to everyone in the challenge. Two things follow:
 *
 *  * no icon that reads as a warning, no error colour, no "unverified" — the row is drawn in
 *    `onSurfaceVariant` like every other secondary caption on this screen;
 *  * the number is **not re-ranked**. A typed score sorts exactly where its value puts it.
 *
 * And what it is **not**: proof. A participant writes only their own row, so this is
 * self-asserted by construction — see `ChallengeParticipant`'s KDoc. It is a **label on a
 * number**, not an attestation about a person. §6's own honest residual is that server-owned
 * scoring stops a win being *typed*, not a reading being *forged*.
 */
@Composable
internal fun ReportedBadge(
    standing: ChallengeStanding,
    metricWord: String,
    modifier: Modifier = Modifier,
) {
    if (!standing.isReported) return
    val who = if (standing.isCurrentUser) {
        "You reported"
    } else {
        "Reported by ${standing.displayName.ifBlank { "this member" }}"
    }
    // "what they updated" is the number itself, in the challenge's own word. The date is
    // dropped rather than faked when the projection wrote no stamp -- an older row, or one
    // written before this field existed, has no reported-at to show and must not borrow the
    // as-of stamp beside it.
    val what = buildString {
        append(who)
        append(" · ")
        append(ChallengesViewModel.format(standing.score))
        if (metricWord.isNotBlank()) {
            append(' ')
            append(metricWord)
        }
        if (standing.reportedAtEpochMillis > 0L) {
            append(" · ")
            // `relative`, not `formatDay`: a challenge runs for weeks, so "yesterday" is
            // what a competitor actually wants to know and "Aug 24, 2025" spends a third
            // of the line on a year nobody is in doubt about. `Observed:` the first render
            // frame, where the full date pushed the sentence to two-thirds of the row.
            append(DateTimeUtils.relative(standing.reportedAtEpochMillis))
        }
    }
    Text(
        what,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

// ── Link a goal ──────────────────────────────────────────────────────

/**
 * §6's scoring path, as a sheet: *each participant links one of their own goals of the same
 * kind*, and the challenge then moves on its own.
 *
 * **The empty case is where §6's other half lives**, and as of 2026-08-25 it is a **form**
 * rather than a message. §6 says joining links **or creates** a goal — *"so a challenge
 * hands you tracking you did not have"* — and until now only the linking half shipped: a
 * user with no goal of the challenge's kind got a sentence naming the kind and a trip to
 * the Goals screen. Honest, and one screen short.
 *
 * §1 is what made it urgent. The whole point of inviting a friend is that they can
 * actually compete, and a friend asked into a Steps Race may well have no steps goal —
 * so the shortest path from *"someone invited me"* to *"I am racing"* must not detour
 * through another screen.
 *
 * The measure is **not** offered as a choice here: it is the challenge's, copied whole, so
 * the goal is scoreable by construction rather than by the user getting a dropdown right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalLinkSheet(
    state: GoalLinkState,
    onLink: (String) -> Unit,
    onCreateTitle: (String) -> Unit,
    onCreateTarget: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        GoalLinkContent(
            state = state,
            onLink = onLink,
            onCreateTitle = onCreateTitle,
            onCreateTarget = onCreateTarget,
            onCreate = onCreate,
        )
    }
}

/**
 * The picker and §2's create form, without the sheet around them.
 *
 * **Split out for the reason [StandingsList] and [InviteList] were**, and in the same
 * commit as the change that made it worth photographing: `AppModalBottomSheet` renders in
 * a window of its own, so nothing inside it can be captured — `onRoot()` matches two nodes
 * and refuses, and every other root selector lands on the host window the sheet has just
 * emptied. `ChallengeGoalCreateRenderPass` is the camera.
 */
@Composable
internal fun GoalLinkContent(
    state: GoalLinkState,
    onLink: (String) -> Unit,
    onCreateTitle: (String) -> Unit,
    onCreateTarget: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    run {
        Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text("Score this from a goal", style = MaterialTheme.typography.titleLarge)
            Text(
                state.challengeTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Your score becomes how far you move that goal from the moment you " +
                    "joined — so Health Connect, a completed task and a manual log all " +
                    "count, and nothing before you joined does.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            if (state.eligible.isEmpty()) {
                CreateGoalForThisChallenge(
                    state = state,
                    onTitle = onCreateTitle,
                    onTarget = onCreateTarget,
                    onCreate = onCreate,
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(state.eligible, key = { it.id }) { goal ->
                    val isLinked = goal.id == state.linkedGoalId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isLinked) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (isLinked) {
                                    "Currently scoring this challenge"
                                } else {
                                    "Measured in ${goal.measureWord}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { onLink(goal.id) },
                            enabled = !state.isSaving && !isLinked,
                        ) { Text(if (isLinked) "Linked" else "Use this") }
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
    }
}

// ── Invite a friend ──────────────────────────────────────────────────

/**
 * §1's sending half: *"join mine"*, as a sheet.
 *
 * Ido, 2026-08-24: *"in the version I have on my phone I can create a CHALLENGE but I
 * cannot invite a friend I have in the app to the CHALLENGE"*. Before this there was no
 * invite mechanism at all — a friend could only arrive through **Discover**, which lists
 * every challenge in the database to everybody, so there was no way to point at one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InviteSheet(
    state: InviteState,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        InviteList(state = state, onInvite = onInvite)
    }
}

/**
 * The invite list itself, without the sheet around it.
 *
 * **Split out for the same reason [StandingsList] is, and in the same commit** — that is
 * the rule this file learned on 2026-08-24, not a favour to a test.
 * `AppModalBottomSheet` renders in a **window of its own**, so `onRoot()` matches two
 * nodes and refuses, while every other root selector lands on the host window, which the
 * sheet has already emptied. Two frames got as far as being filed before that was
 * understood: a **71 px** strip, and a **1344x2992 rectangle of flat colour** that passed
 * every size floor a render pass had.
 *
 * And the question this surface is risky for is not a string either:
 *
 * > **Does an invite row read as an *offer*, or as an *obligation*?**
 *
 * It must read as an offer. Only a picture settles that, so
 * `ChallengeInviteRenderPass` photographs this composable directly.
 */
@Composable
internal fun InviteList(
    state: InviteState,
    onInvite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
        Text("Invite a friend", style = MaterialTheme.typography.titleLarge)
        Text(
            state.challengeTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            // Says what the other person gets, not what this button does. An invite is an
            // offer, so the sentence that introduces it should sound like one — and the
            // second half is load-bearing rather than reassurance: nothing is written to
            // the invitee, and joining stays their own act.
            "They will see an invite at the top of their own Challenges screen, and can " +
                "join or ignore it. Nothing is added to their account either way.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()

        if (state.hasNoFriends) {
            // A real state with its own sentence, and the fix is on another screen — a
            // blank list would not say so. See `InviteState.hasNoFriends`.
            Text(
                "You have not added anyone yet. Add a friend from their friend code on " +
                    "the Profile tab, and they will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }

        LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
            items(state.candidates, key = { it.uid }) { candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(
                        photoUrl = candidate.photoUrl,
                        name = candidate.label,
                        size = 36.dp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            candidate.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // WHY A BLOCKED FRIEND IS GREYED AND NOT FILTERED OUT.
                        //
                        // A friend who silently vanishes from this list reads as "the app
                        // does not know them", which is the one thing the user is certain
                        // is false. A line saying "Already in" answers the question they
                        // actually had -- and it keeps the list's length stable, so
                        // inviting three people in a row does not make it jump under the
                        // finger. See `InviteCandidate`.
                        val note = when {
                            candidate.isParticipant -> "Already in this challenge"
                            candidate.isInvited -> "Invited — waiting for them"
                            else -> null
                        }
                        note?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(
                        onClick = { onInvite(candidate.uid) },
                        // Only the row being written to goes quiet, not the sheet: this is
                        // a list somebody taps three times in a row.
                        enabled = candidate.canInvite && state.busyUid == null,
                    ) {
                        Text(
                            when {
                                state.busyUid == candidate.uid -> "Sending…"
                                candidate.isParticipant -> "In"
                                candidate.isInvited -> "Invited"
                                else -> "Invite"
                            },
                        )
                    }
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
}

/**
 * §1's receiving half: one inbound invite, at the top of the Challenges screen.
 *
 * **Not in a sheet, and that is the point** — it is a row that is simply there, and then
 * is not. It is also why this one needs no `…List` split: it renders in the host window
 * like any other card and photographs directly.
 *
 * ### It must read as an offer, not as an obligation
 *
 * Everything here is chosen against that one criterion, so none of it is arbitrary:
 *
 *  * **`Join` is a filled button and `Dismiss` a text button**, because the balance a
 *    user reads off a row is which action looks like the default. Two filled buttons
 *    would make it a decision to be got right; two text buttons would bury an offer they
 *    might want.
 *  * **No badge, no count, no red dot, no notification.** Those turn an offer into a
 *    chore. `C9a` §6's consent story covers reminders about the user's *own* work.
 *  * **Dismiss asks nothing.** Leaving and deleting get confirmation dialogs because they
 *    destroy something the user built; declining an offer destroys nothing and the sender
 *    can make it again. A dialog would turn a shrug into a decision.
 *  * **The sender is named and the challenge is named**, in that order, because *who is
 *    asking* is what decides the answer. An unattributed *"You have been invited to X"*
 *    is a system notice; this is a person.
 */
@Composable
internal fun ChallengeInviteRow(
    invite: com.idomarhaim.goalpilot.domain.model.ChallengeInvite,
    isBusy: Boolean,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GpCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(photoUrl = invite.fromPhotoUrl, name = invite.senderLabel, size = 36.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        // One sentence, both names, sender first. Built by concatenation,
                        // so a long display name is the shape that breaks it -- which is
                        // why `ChallengeInviteRenderPass` photographs one.
                        "${invite.senderLabel} invited you to “${invite.challengeTitle}”",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onJoin, enabled = !isBusy) { Text("Join") }
                TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Dismiss") }
            }
        }
    }
}

/**
 * §2's create-and-link form — *"joining links **or creates** a goal"*.
 *
 * ### Why this is three controls and not a goal editor
 *
 * The measure is **not** a field. It is the challenge's, copied whole, which is what makes
 * the new goal scoreable by construction instead of by the user picking a matching kind
 * out of a dropdown — the one thing they could get wrong here and would have no way to
 * diagnose. Only the title and the target are theirs, because only those two are things
 * the challenge cannot know.
 *
 * The target starts **blank** rather than at a guess. A challenge names a **unit**, never a
 * finish line — *"most steps this month"* has no target in it — so any pre-filled number
 * would be the app inventing an ambition on the user's behalf, on an object §1.1 says
 * needs their declaration.
 *
 * The sentence above the fields says the goal is **theirs and outlives the race**, because
 * that is the part a user cannot see and would reasonably fear: a goal quietly made by a
 * challenge, that vanishes with it, is not tracking they can rely on.
 */
@Composable
private fun CreateGoalForThisChallenge(
    state: GoalLinkState,
    onTitle: (String) -> Unit,
    onTarget: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        // THE KIND NAME IS NOT A WORD FOR A SENTENCE, AND A FRAME IS WHAT SHOWED IT.
        //
        // This line read "None of your goals measures count in steps" until 2026-08-25 --
        // `MeasureKind.COUNT.label()` lowercased, dropped into prose. It is app machinery
        // and it reads as a typo. The user's own word for the unit is the half they
        // recognise, so that is the half the sentence uses; the KIND is still what the
        // matching is actually done on (`Challenge.canBeScoredFrom`), which is why the
        // picker can be empty while a goal counting something else in the same kind would
        // have filled it. That precision belongs in this comment, not in the sentence.
        Text(
            "None of your goals is measured in ${state.metricWord}. Make one here and " +
                "it will start scoring straight away.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "It is an ordinary goal on your Goals screen — yours to edit, and it stays " +
                "when the challenge ends.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.createTitle,
            onValueChange = onTitle,
            label = { Text("Goal name") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.createTarget,
            onValueChange = onTarget,
            label = { Text("Target") },
            // The unit is the challenge's word, shown rather than typed -- it is not the
            // user's to choose here, and showing it is what tells them what number to put.
            suffix = { Text(state.metricWord) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onCreate,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ONE VERB FOR ONE ACT. Creating and linking are a single call in the view
            // model for a reason -- two would leave "a goal made for a challenge that is
            // not scoring it" reachable by closing the sheet in between -- and a button
            // saying only "Create" would describe half of what happens.
            Text(if (state.isSaving) "Creating…" else "Create and start scoring")
        }
    }
}

// ── §3 · changing what a challenge counts ────────────────────────────

/**
 * The **only** way a challenge's measure ever changes — §6's approval flow, proposed.
 *
 * `firestore.rules` pins `measureKind` and `measureWord` against every client write as of
 * 2026-08-25, so there is no "edit the challenge" that bypasses this. The owner asks; the
 * participants agree; a Cloud Function applies it.
 *
 * ### The consequence is stated before anybody is asked, and it is derived
 *
 * `C7` §5 offered the owner *reset* or *adapt*. This dialog offers **neither**, because
 * working out what *adapt* could mean collapses the choice: a change of **kind** cannot be
 * adapted without a unit conversion this app deliberately does not perform, so it *is* a
 * reset; a change of **word** alone touches no arithmetic, so there is nothing to adapt.
 * The dialog therefore *reports* which one the owner's edit carries, live, as they type —
 * and the alarming one is spelled out in full, because "everyone's score restarts at zero"
 * is a thing to find out **before** asking four people to agree to it, not after.
 */
@Composable
internal fun MeasureChangeDialog(
    state: MeasureChangeState,
    onKind: (MeasureKind) -> Unit,
    onWord: (String) -> Unit,
    onPropose: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change what this counts") },
        text = { MeasureChangeContent(state = state, onKind = onKind, onWord = onWord) },
        confirmButton = {
            TextButton(onClick = onPropose, enabled = !state.isSaving && state.isAChange) {
                Text(
                    when {
                        state.isSaving -> "Saving…"
                        state.needsOthers -> "Ask everyone"
                        else -> "Change it"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )
}

/**
 * The dialog's body, without the dialog around it.
 *
 * **Split out for the reason [StandingsList], [InviteList] and [GoalLinkContent] were, and
 * the instrument is what forced it.** An `AppAlertDialog` renders in a **window of its
 * own**, exactly like a modal sheet — and unlike a sheet, the selector that rescues a
 * sheet's content does **not** rescue this one. `ChallengeMeasureChangeRenderPass` first
 * tried `onNode(isRoot() and hasAnyDescendant(hasText("Change what this counts")))`, the
 * same selector `ChallengeProvenanceRenderPass` uses, and got back a **single flat colour**
 * — the scrim. `Observed:` 2026-08-25, `dialog-reset-light`, *"1 distinct colour, expected
 * at least 3"*.
 *
 * That failure is the whole argument for this seam. The frame **looked** like it worked —
 * it was full-screen, it was 1344 px wide, it weighed something on disk — and only the
 * more-than-one-colour floor said otherwise. Had the pass carried a size assertion alone,
 * as this repo's did until 2026-08-24, it would have filed a picture of a grey rectangle as
 * evidence that the warning copy reads clearly.
 *
 * And the copy is what has to be reviewed: this is the one action in the app that destroys
 * other people's numbers. A surface that cannot be photographed cannot be reviewed.
 */
@Composable
internal fun MeasureChangeContent(
    state: MeasureChangeState,
    onKind: (MeasureKind) -> Unit,
    onWord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    run {
        run {
            Column(modifier = modifier.verticalScroll(rememberScrollState())) {
                Text(
                    state.challengeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    // WHY ANYBODY HAS TO AGREE AT ALL, IN ONE SENTENCE. Without it the
                    // approval step reads as bureaucracy; with it, it reads as the reason
                    // the leaderboard can be trusted.
                    "The measure is the unit everyone's score is written in, so changing " +
                        "it re-labels other people's numbers as well as yours. That is " +
                        "why they all have to agree.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )

                Text(
                    "What does it count?",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeasureKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kind == state.kind,
                            onClick = { onKind(kind) },
                            label = { Text(kind.label()) },
                            leadingIcon = { MeasureIconBadge(kind) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.word,
                    onValueChange = onWord,
                    label = { Text("Measured in") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.isAChange) {
                    Spacer(Modifier.height(14.dp))
                    when (state.consequence) {
                        MeasureChangeConsequence.RESET -> Text(
                            // The alarming one, said in full and in the error colour. This
                            // is the sentence that stops somebody discovering after the
                            // fact that they wiped four people's scores.
                            "This changes the kind, so every score restarts at zero and " +
                                "everyone re-picks a goal. There is no way to convert " +
                                "${state.currentWord.ifBlank { "the old unit" }} into " +
                                "${state.word.trim().ifBlank { "the new one" }}, so the " +
                                "race starts again from here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )

                        MeasureChangeConsequence.RELABEL -> Text(
                            "This only changes the word. Every score and every linked " +
                                "goal stays exactly as it is.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (state.needsOthers) {
                            "All ${state.participantCount} of you have to agree before " +
                                "anything changes."
                        } else {
                            "You are the only one in this challenge, so it changes " +
                                "straight away."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
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
        }
    }
}

/**
 * The pending-change banner, on the card of everybody who has to agree.
 *
 * ### It is on the card, not in a dialog, and that is the decision
 *
 * A modal would demand an answer the moment the screen opened, from somebody who came to
 * log their steps. A banner on the challenge it concerns is **there until it is answered**,
 * and answering it is one tap — which is the right weight for a question that is real but
 * not urgent. Nothing is blocked while it waits: the challenge keeps scoring in the old
 * unit, which is the whole reason the pending fields sit beside the live measure rather
 * than on top of it.
 *
 * ### It names the count, never the hold-outs
 *
 * *"2 of 4 agreed"*, never *"waiting for Ann and Boaz"*. The count answers the only
 * question anybody has — *is this going to happen?* — and naming names turns a unit change
 * into a thing people are seen to be blocking.
 */
@Composable
internal fun PendingMeasureBanner(
    card: ChallengeCard,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending = card.challenge.pendingMeasure ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        HorizontalDivider()
        Text(
            if (card.isOwner) {
                "You asked to change this to ${pending.word}."
            } else {
                "${card.challenge.title} would be scored in ${pending.word} instead."
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            when (card.pendingConsequence) {
                // Said to the people who would lose the number, in their own terms: the
                // dialog told the owner, and everybody else finds out here.
                MeasureChangeConsequence.RESET ->
                    "Every score restarts at zero and everyone re-picks a goal — the old " +
                        "unit cannot be converted into the new one."

                MeasureChangeConsequence.RELABEL ->
                    "Only the wording changes. Every score and every linked goal stays."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (card.pendingConsequence == MeasureChangeConsequence.RESET) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            // The count, never the hold-outs.
            "${card.pendingApprovals} of ${card.participantCount} agreed",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            if (!card.iApprovedPending) {
                // THE BUTTON'S WEIGHT FOLLOWS THE CONSEQUENCE, AND A FRAME IS WHAT SHOWED
                // IT HAD TO.
                //
                // `banner-reset-light.png`, 2026-08-25: a FILLED `Agree` sat directly under
                // "Every score restarts at zero", carrying the same visual weight as
                // "Change goal" two rows above it. §1's own principle is that the balance a
                // user reads off a row is which action LOOKS like the default -- and a
                // filled button makes agreeing look like the default for the one action in
                // this app that destroys other people's numbers. The red sentence was doing
                // all the work and the button was quietly undoing it.
                //
                // An outlined button on a RESET is still perfectly findable -- there is
                // nothing else to press -- and it stops the row reading as a prompt. A
                // relabel keeps the filled one: it costs nobody anything, and making a
                // harmless yes/no look grave is the other way to train people to ignore it.
                if (card.pendingConsequence == MeasureChangeConsequence.RESET) {
                    OutlinedButton(onClick = onApprove, enabled = !isBusy) { Text("Agree") }
                } else {
                    Button(onClick = onApprove, enabled = !isBusy) { Text("Agree") }
                }
            } else {
                Text(
                    "You agreed — waiting for the others.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            if (card.isOwner) {
                TextButton(onClick = onWithdraw, enabled = !isBusy) { Text("Withdraw") }
            }
        }
    }
}
