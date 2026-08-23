package com.idomarhaim.goalpilot.feature.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.DateTimeUtils.formatMinutes
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import com.idomarhaim.goalpilot.notifications.FilingNotificationEffect
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.core.util.AppDateFormatters
import com.idomarhaim.goalpilot.domain.model.startDate
import com.idomarhaim.goalpilot.domain.usecase.CalendarEntry
import com.idomarhaim.goalpilot.domain.usecase.DisappearanceChoice
import com.idomarhaim.goalpilot.domain.usecase.MissedOccurrence
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.domain.model.TasksConsent
import com.idomarhaim.goalpilot.ui.components.Avatar
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.GpLinearProgress
import com.idomarhaim.goalpilot.ui.components.HeroSurface
import com.idomarhaim.goalpilot.ui.components.IconChip
import com.idomarhaim.goalpilot.ui.components.LocalGpEntrance
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.ProgressRing
import com.idomarhaim.goalpilot.ui.components.rememberGpEntrance
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.components.TasksConsentNotice
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet
import com.idomarhaim.goalpilot.ui.theme.gpAccents
import com.idomarhaim.goalpilot.ui.tutorial.TutorialAnchor
import com.idomarhaim.goalpilot.ui.tutorial.tutorialAnchor
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    onSeeAllGoals: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var avatarSheetOpen by remember { mutableStateOf(false) }
    val recs by viewModel.recommendations.collectAsStateWithLifecycle()
    val smartAdd by viewModel.smartAdd.collectAsStateWithLifecycle()
    val filed by viewModel.filed.collectAsStateWithLifecycle()
    val tasksImport by viewModel.tasksImport.collectAsStateWithLifecycle()
    val healthSync by viewModel.healthSync.collectAsStateWithLifecycle()
    val consentIntent by viewModel.consentIntent.collectAsStateWithLifecycle()
    val tasksConsent by viewModel.tasksConsent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val disappearances by viewModel.calendarDisappearances.collectAsStateWithLifecycle()
    val missReview by viewModel.missReview.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.ensureRecommendations()
        viewModel.ensureHealthAvailability()
        // Re-read on every entry, not once: the life-areas screen grants the same
        // scope, and this ViewModel outlives a trip there and back (#36).
        viewModel.refreshTasksConsent()
    }
    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }
    SmartAddReceiptSnackbar(
        receipt = filed,
        hostState = snackbarHost,
        onUndo = viewModel::undoFiling,
        onConsume = viewModel::consumeFiled,
    )

    // `R5`'s other half: the same event as a system notification, for a quick-add the user has
    // already walked away from. A SECOND, independent consumer of `filed` -- it reads and never
    // writes back, which is what keeps the snackbar above working when the permission is
    // refused (#8 piece 5, `notifications/GoalPilotNotifier`).
    FilingNotificationEffect(
        taskId = filed?.taskId,
        decision = filed?.decision,
        taskTitle = filed?.taskTitle,
        createdGoalId = filed?.createdGoalId,
    )

    val sharePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.shareWeeklySummary(uri) }

    // Google's own consent screen — reached either because the account predates
    // the Tasks scope, or (the normal case, spec §2.6) because "View your tasks"
    // arrived unticked at sign-in. Backing out of it is recorded as a decline so
    // the card can say so, rather than looking like a first-ever run (#36).
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

    // Health Connect runs its own permission UI; the contract below is the only
    // supported way to ask, and it has to be registered from a composable.
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted -> viewModel.onHealthPermissionsResult(granted) }
    LaunchedEffect(healthSync.requestPermissions) {
        if (!healthSync.requestPermissions) return@LaunchedEffect
        // Launching throws if the provider vanished between the availability check
        // and this call — uninstalling Health Connect mid-session does exactly that.
        runCatching { healthPermissionLauncher.launch(viewModel.healthPermissions) }
            .onFailure { viewModel.consumeHealthPermissionRequest() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GoalPilot",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Avatar(
                        photoUrl = state.userPhotoUrl,
                        name = state.userFullName,
                        size = 36.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(CircleShape)
                            // The guided tour's last step points here, because
                            // this is the door to Settings and Settings is where
                            // the tour itself lives from then on.
                            .tutorialAnchor(TutorialAnchor.AVATAR)
                            .clickable { avatarSheetOpen = true }
                            .semantics { this.contentDescription = "Your account" }
                            .testTag(TAG_HOME_AVATAR),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { inner ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(inner))
            return@Scaffold
        }
        // #57 d -- the column ARRIVES. Every GpCard and SectionHeader below
        // carries `Modifier.gpEntrance()` already, so this one provider is the
        // whole opt-in: each block claims the next slot in one staggered wave of
        // rise-and-fade.
        //
        // Deliberately below the loading gate. The entrance starts its clock on
        // the first block that claims, not when it is constructed, so the wave
        // begins when there is something to watch rather than being spent behind
        // the spinner -- but remembering it here means it is also a genuinely new
        // wave if the screen ever falls back to loading and returns.
        //
        // The trigger is SCREEN entry, not item entry: `GpEntrance` stops handing
        // out slots once the arrival window has passed, so the cards below the
        // fold that this LazyColumn composes on scroll are simply there.
        CompositionLocalProvider(LocalGpEntrance provides rememberGpEntrance()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // §2.5's daily review, ABOVE the points card and nowhere else: it is the first
                // thing the app has to say when it has something to say, and burying it under a
                // level-up animation is how a miss goes unread. Absent entirely on the great
                // majority of opens, which is the point -- an empty review is not an empty card.
                if (missReview.isVisible && missReview.misses.isNotEmpty()) {
                    item {
                        DailyMissReviewCard(
                            misses = missReview.misses,
                            onDismiss = viewModel::dismissMissReview,
                        )
                    }
                }
                // §2.7's batch sheet, immediately under the review it belongs to: "the
                // ambiguity is asked in the daily-review batch sheet -- Keep / Cancel / Put
                // back -- at the one moment Ido is holding the phone". Like the review above
                // it, absent on almost every open, which is the point.
                if (disappearances.isNotEmpty()) {
                    item {
                        CalendarDisappearanceCard(
                            entries = disappearances,
                            onChoose = viewModel::resolveDisappearance,
                        )
                    }
                }
                item {
                    PointsLevelCard(
                        userName = state.userName,
                        points = state.points,
                        level = state.level,
                        levelProgress = state.levelProgress,
                        pointsToNext = state.pointsToNextLevel,
                        modifier = Modifier.tutorialAnchor(TutorialAnchor.POINTS_CARD),
                    )
                }
                item {
                    OverviewCard(
                        averageProgress = state.averageProgress,
                        goalCount = state.goals.size,
                        doneTasks = state.doneTasks,
                        completedThisWeek = state.completedTasksLast7d,
                        onOpenAnalytics = onOpenAnalytics,
                    )
                }
                item {
                    SmartAddCard(
                        state = smartAdd,
                        onClassify = viewModel::classifyForSmartAdd,
                        // Step 3's target. It is the one anchored widget that can
                        // sit below the fold, which is why `tutorialAnchor` also
                        // scrolls its own node into view when its step arrives.
                        modifier = Modifier.tutorialAnchor(TutorialAnchor.QUICK_ADD),
                    )
                }
                item {
                    GoogleTasksImportCard(
                        isLoading = tasksImport.isLoading,
                        consent = tasksConsent,
                        onImport = viewModel::importGoogleTasks,
                    )
                }
                item {
                    HealthConnectCard(
                        state = healthSync,
                        onSync = viewModel::syncHealth,
                    )
                }
                item {
                    SectionHeader(
                        title = "AI coach",
                        action = {
                            IconButton(onClick = viewModel::refreshRecommendations) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh tips")
                            }
                        },
                    )
                }
                if (recs.isLoading) {
                    item { CircularProgressIndicator(Modifier.padding(8.dp)) }
                } else {
                    items(recs.items, key = { it.id }) { rec -> RecommendationCard(rec) }
                }
                item {
                    SectionHeader(
                        title = "Your goals",
                        action = { TextButton(onClick = onSeeAllGoals) { Text("See all") } },
                    )
                }
                if (state.goals.isEmpty()) {
                    item {
                        OutlinedButton(onClick = onAddGoal, modifier = Modifier.fillMaxWidth()) {
                            Text("Create your first goal")
                        }
                    }
                } else {
                    items(state.goals.take(3), key = { it.id }) { goal ->
                        GoalCard(goal = goal, onClick = { onOpenGoal(goal.id) })
                    }
                }
                item {
                    ShareSummaryCard(
                        onShare = { viewModel.shareWeeklySummary(null) },
                        onShareWithPhoto = {
                            sharePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
            }
        }
    }

    if (tasksImport.isVisible) {
        GoogleTasksImportDialog(
            state = tasksImport,
            onToggle = viewModel::toggleImportProposal,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::dismissImport,
        )
    }

    if (avatarSheetOpen) {
        AccountSheet(
            onOpenProfile = {
                avatarSheetOpen = false
                onOpenProfile()
            },
            onOpenSettings = {
                avatarSheetOpen = false
                onOpenSettings()
            },
            onDismiss = { avatarSheetOpen = false },
        )
    }

}

/**
 * Spec §4.2's avatar sheet: **two siblings, *Your profile* and *Settings*.**
 *
 * The siblinghood is the design. §4.9 splits one screen into two along a line
 * the user can actually test — *does it survive sign-out?* — and a sheet that
 * offered Profile with Settings tucked inside it would put the account back in
 * charge of the device. They are peers here because they are peers in the
 * model.
 *
 * Each row carries the boundary in a supporting line rather than in a help
 * link: the user picking between them is the one person who needs to know which
 * is which, and they need it now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = { Text("Your profile", style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text("Friend code, level, points — leaves with the account.") },
            leadingContent = {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .clickable(onClick = onOpenProfile)
                .testTag(TAG_SHEET_PROFILE),
        )
        ListItem(
            headlineContent = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
            // "AI" added by C13 #54, which built §4.9's fifth section. This line
            // enumerates the sections, so leaving it at four would have made the
            // door to the new one invisible from the one place it is opened.
            supportingContent = {
                Text("Appearance, language, your day, AI — stays on this phone.")
            },
            leadingContent = {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .clickable(onClick = onOpenSettings)
                .testTag(TAG_SHEET_SETTINGS),
        )
        Spacer(Modifier.height(24.dp))
    }
}

const val TAG_HOME_AVATAR = "home_avatar"
const val TAG_SHEET_PROFILE = "home_sheet_profile"
const val TAG_SHEET_SETTINGS = "home_sheet_settings"

/**
 * Status and manual override for the Health Connect sync (spec §5, §6 nice-to-have).
 *
 * The sync itself is automatic — it runs whenever the app comes forward, at most
 * once every fifteen minutes — so this card is mostly a window onto something that
 * has already happened. It exists because a feature that writes to your goals
 * without ever being visible is worse than one you have to press: "Synced 4
 * minutes ago" is how the user knows it is working, and the button is how they
 * skip the wait.
 *
 * The card is deliberately honest about absence: Health Connect is a separate app
 * below Android 14 and missing from most emulator images, so "not available here"
 * is a normal outcome that gets its own explanation rather than a dead button.
 */
@Composable
private fun HealthConnectCard(state: HealthSyncState, onSync: () -> Unit) {
    val availability = state.availability
    val canSync = availability == HealthAvailability.AVAILABLE ||
        availability == HealthAvailability.PERMISSIONS_REQUIRED
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Filled.MonitorHeart,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(12.dp))
                Text("Health data", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (availability) {
                    null -> "Checking whether Health Connect is available…"
                    HealthAvailability.AVAILABLE ->
                        "Your steps and sleep are pulled from Health Connect and logged " +
                            "against your goals automatically, every time you open GoalPilot."
                    else -> availability.explain()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Recomputed on every recomposition rather than ticking: the label is a
            // coarse "4 minutes ago", and a card that repaints once a second to keep
            // a number honest costs more than the honesty is worth.
            val ago = healthSyncAgoLabel(state.lastSyncAtMillis, System.currentTimeMillis())
            if (availability == HealthAvailability.AVAILABLE) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ago ?: "Not synced on this device yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onSync,
                enabled = canSync && !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    state.isSyncing -> {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing…")
                    }

                    availability == HealthAvailability.PERMISSIONS_REQUIRED ->
                        Text("Connect Health Connect")

                    else -> Text("Sync now")
                }
            }
        }
    }
}

/**
 * Entry point for the Google Tasks import (spec §6 nice-to-have). Pulls open
 * tasks from the signed-in account's Google Tasks lists and files them against
 * goals using the same classifier as [SmartAddCard].
 *
 * When [consent] is [TasksConsent.MISSING] the card says so *before* anything is
 * pressed (#36). Until this existed, a declined scope and a first-ever run were
 * pixel-identical: both landed on the same generic grant prompt after an import
 * had already failed.
 */
@Composable
private fun GoogleTasksImportCard(
    isLoading: Boolean,
    consent: TasksConsent?,
    onImport: () -> Unit,
) {
    // Only a positively observed refusal changes the card. Null is "not checked
    // yet" and NOT_SIGNED_IN is "never asked" — neither is a decline, and §0.4
    // only licenses speech about a failure the user can act on.
    val declined = consent == TasksConsent.MISSING
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(icon = Icons.Filled.CloudDownload)
                Spacer(Modifier.width(12.dp))
                Text("Import from Google Tasks", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            if (declined) {
                TasksConsentNotice()
            } else {
                Text(
                    "Pull your open Google Tasks in and let GoalPilot file each one " +
                        "under the right goal. You review everything before it is saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onImport,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Importing…")
                    }

                    declined -> Text(stringResource(R.string.tasks_consent_grant_action))

                    else -> Text("Import tasks")
                }
            }
        }
    }
}

/**
 * Review sheet for an import. Every row is opt-out-able and nothing is written
 * until "Import" is pressed — the LLM proposes, the user decides (spec §8).
 */
@Composable
private fun GoogleTasksImportDialog(
    state: TasksImportState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedCount = state.proposals.count { it.selected }
    AppAlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text("Import from Google Tasks") },
        text = {
            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Reading your tasks and sorting them…")
                }

                state.error != null -> Text(state.error)

                else -> Column {
                    Text(
                        "Found ${state.totalFound} open task(s). " +
                            "Tap a row to include or exclude it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.proposals, key = { it.externalId }) { proposal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.isSaving) {
                                        onToggle(proposal.externalId)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = proposal.selected,
                                    onCheckedChange = { onToggle(proposal.externalId) },
                                    enabled = !state.isSaving,
                                )
                                Column(Modifier.weight(1f)) {
                                    // Google Tasks titles are unbounded — people paste
                                    // whole messages in. Without a clamp one entry
                                    // pushes every other row off the dialog.
                                    Text(
                                        proposal.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        buildString {
                                            append(
                                                proposal.targetGoalTitle
                                                    ?.let { "→ $it" }
                                                    ?: "→ new goal “${proposal.newGoalTitle}”",
                                            )
                                            // `#55`: the row says what the model JUDGED, not
                                            // a currency it never emitted. Points are
                                            // computed from this and the duration beside it
                                            // at the write, so printing them here would be a
                                            // third number to keep in step.
                                            append(" · ${proposal.difficulty.name.lowercase()}")
                                            append(" · ${durationLabel(proposal.minutes)}")
                                            proposal.lifeAreaName?.let {
                                                append(
                                                    if (proposal.createsLifeArea) {
                                                        " · new area “$it”"
                                                    } else {
                                                        " · $it"
                                                    },
                                                )
                                            }
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
                TextButton(onClick = onConfirm, enabled = !state.isSaving && selectedCount > 0) {
                    Text(if (state.isSaving) "Importing…" else "Import $selectedCount")
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
 * Entry point for the LLM task→goal classifier (spec §6 Bonus). Type a task in
 * plain language; the `classifyTask` Cloud Function decides which goal it belongs
 * to — or proposes a new one — and estimates its point value.
 */
/**
 * §2.5's **daily miss review**: *"Misses meet Ido once, in a daily review on app open — never
 * as a push saying he failed"* (`#56`).
 *
 * ## The tone is the requirement, not a preference
 *
 * That sentence rules out two designs that would otherwise be obvious: a notification, and a
 * running tally of failures. So this card **states what lapsed and what each lapse means**, in
 * §2.2's own words, and offers no judgement of any kind — no count of failures, no streak
 * broken, no red. `MISSED` is the only state §2.3 marks a failure and even it gets the same
 * neutral row here; the distinction lives in the model, for §4.7's per-area surface to use, not
 * in a colour on a card the user cannot act on.
 *
 * ## Why an overdue row reads differently
 *
 * §2.3: a passed deadline is *"late, and still owed"*, and it is the one state that keeps
 * reminding. So it is the one row that says something is still **open**, and it is also the
 * one that comes back tomorrow — see `DailyMissReview`. Everything else has genuinely gone and
 * is shown once.
 */
@Composable
internal fun DailyMissReviewCard(misses: List<MissedOccurrence>, onDismiss: () -> Unit) {
    GpCard(modifier = Modifier
        .fillMaxWidth()
        .testTag(MISS_REVIEW_TAG)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(MISS_REVIEW_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(
                MISS_REVIEW_SUBTITLE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            misses.forEach { miss ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = miss.task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = missLabel(miss.state),
                        style = MaterialTheme.typography.labelMedium,
                        // ⚠️ The one row that is STILL OPEN does not look like history, and
                        // this was found by looking at the render pass: all four rows read
                        // identically, so §2.3's whole OVERDUE/MISSED split -- the thing that
                        // "earns its keep twice" -- was invisible on the one surface a person
                        // actually reads. `stillOwed` existed on the model and nothing used it.
                        //
                        // PRIMARY, never an error colour: this marks a thing the user can still
                        // do, not a thing they got wrong. Red here would be the "push saying he
                        // failed" §2.5 forbids, arriving as a swatch.
                        color = if (miss.stillOwed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(MISS_REVIEW_DISMISS_TAG),
            ) {
                Text(MISS_REVIEW_DISMISS_LABEL)
            }
        }
    }
}

/**
 * §2.2's table, as the four sentences it actually is.
 *
 * A `when` over the state and not a property on the enum: these are **speech**, and §5.1 keeps
 * speech out of `domain/` — the same rule that moved `AnalyticsRange`'s labels into
 * `AnalyticsStrings`. The two silent states never reach here, because
 * `OccurrenceState.meetsUserInDailyReview` filtered them out before the list was built.
 */
// `internal`, with `DailyMissReviewCard`, so `DailyMissReviewUiTest` drives the REAL card and
// the real labels. A test that re-implemented either would have been written from the same
// understanding as the code and would pass on the same mistake -- the reasoning
// `SmartAddReceiptUiTest` records after exactly that shipped.
internal fun missLabel(state: OccurrenceState): String = when (state) {
    OccurrenceState.OVERDUE -> "late, still owed"
    OccurrenceState.MISSED -> "the slot is gone"
    OccurrenceState.DAY_PASSED -> "the day passed"
    OccurrenceState.WINDOW_CLOSED -> "the window closed"
    // Unreachable: `meetsUserInDailyReview` excludes all three. Spelled out rather than
    // collapsed into an `else`, so adding a fifth rung makes this a compile error instead of a
    // row that silently reads "".
    OccurrenceState.SCHEDULED, OccurrenceState.UNDERWAY, OccurrenceState.EXPIRED -> ""
}

/**
 * §2.7's **Keep / Cancel / Put back** sheet — the one question this feature asks
 * ([`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61)).
 *
 * ## Why it is a question at all, when nothing else in this feature asks one
 *
 * §2.8 is emphatic that the per-action *"also update in Google?"* prompt is *"ceremony against
 * an unreal risk"* and that *"a dialog answered yes ten times stops being read by the
 * eleventh"*. This is the exception it names, and the reason is that here the app genuinely
 * **cannot know**: *"a move-out is indistinguishable from a delete (both read as `cancelled`,
 * and we see only our own calendar)"*. Every other decision in the sync is derivable; this one
 * is not, so it is the one thing worth a person's attention.
 *
 * ## The data is already safe, which is what lets there be no *"not now"*
 *
 * By the time a row appears here the occurrence has **kept its date and cleared its
 * `googleEventId`** — the sync did that, not this card. So ignoring the whole thing is a
 * legitimate answer with a legitimate outcome: the plan survives, unmirrored. That is why the
 * three buttons have no fourth escape and no dismiss: there is nothing to escape from.
 *
 * ## Neutral, for the daily review's reason
 *
 * §2.5 forbids anything that reads as *"you failed"*. A deleted event is not a miss and not a
 * failure — most of the time it is Ido tidying his own calendar — so no row here is coloured,
 * counted or ranked.
 */
@Composable
internal fun CalendarDisappearanceCard(
    entries: List<CalendarEntry>,
    onChoose: (CalendarEntry, DisappearanceChoice) -> Unit,
) {
    GpCard(modifier = Modifier
        .fillMaxWidth()
        .testTag(DISAPPEARED_TAG)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(DISAPPEARED_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(
                DISAPPEARED_SUBTITLE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            entries.forEach { entry ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = entry.task.title,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = disappearedWhen(entry),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onChoose(entry, DisappearanceChoice.KEEP) },
                            modifier = Modifier.testTag(DISAPPEARED_KEEP_TAG),
                        ) { Text(DISAPPEARED_KEEP_LABEL) }
                        TextButton(
                            onClick = { onChoose(entry, DisappearanceChoice.CANCEL) },
                            modifier = Modifier.testTag(DISAPPEARED_CANCEL_TAG),
                        ) { Text(DISAPPEARED_CANCEL_LABEL) }
                        TextButton(
                            onClick = { onChoose(entry, DisappearanceChoice.PUT_BACK) },
                            modifier = Modifier.testTag(DISAPPEARED_PUT_BACK_TAG),
                        ) { Text(DISAPPEARED_PUT_BACK_LABEL) }
                    }
                }
            }
        }
    }
}

/**
 * The day the occurrence still sits on — **its own date, not the deleted event's**.
 *
 * §2.7: the occurrence *"keeps its date"*. Reading the date off the vanished Google event would
 * show whatever Google last held, which after a drag-and-delete is not where GoalPilot thinks
 * the work is — and the whole question being asked is what to do about the thing GoalPilot
 * still has.
 *
 * `get()` and not a `val`, per `AppDateFormatters`' KDoc: a formatter captured once outlives a
 * language change.
 */
private val disappearedDay get() = AppDateFormatters.of("EEE d MMM")

private fun disappearedWhen(entry: CalendarEntry): String =
    disappearedDay.format(entry.occurrence.occurrence.startDate)

internal const val DISAPPEARED_TAG = "calendar-disappearance"
internal const val DISAPPEARED_KEEP_TAG = "calendar-disappearance-keep"
internal const val DISAPPEARED_CANCEL_TAG = "calendar-disappearance-cancel"
internal const val DISAPPEARED_PUT_BACK_TAG = "calendar-disappearance-put-back"

internal const val DISAPPEARED_TITLE = "Gone from your GoalPilot calendar"
internal const val DISAPPEARED_SUBTITLE =
    "Still planned here. Google cannot tell us whether you deleted it or moved it away."
internal const val DISAPPEARED_KEEP_LABEL = "Keep"
internal const val DISAPPEARED_CANCEL_LABEL = "Cancel"
internal const val DISAPPEARED_PUT_BACK_LABEL = "Put back"

internal const val MISS_REVIEW_TAG = "daily-miss-review"
internal const val MISS_REVIEW_DISMISS_TAG = "daily-miss-review-dismiss"

/** Shared with the tests, so an assertion cannot pass against a stale copy of the wording. */
internal const val MISS_REVIEW_TITLE = "What went by"
internal const val MISS_REVIEW_SUBTITLE = "Shown once. Nothing here counts against you."
internal const val MISS_REVIEW_DISMISS_LABEL = "Got it"

@Composable
internal fun SmartAddCard(
    state: SmartAddState,
    onClassify: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    // `#7`/`R6`. Held here rather than in the ViewModel because it is a property of what is
    // being typed, not of the app: it belongs to this half-finished sentence and dies with it.
    var alreadyDone by remember { mutableStateOf(false) }
    GpCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Filled.AutoAwesome,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "Smart add a task",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                // Says what it does, in the indicative. It used to promise a question —
                // the card described a proposal and a dialog then asked about it — and
                // #6 removed the question, so the sentence stops implying one.
                "Describe anything you want to do — GoalPilot files it under the right goal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("e.g. Run 5 km on Friday") },
                    singleLine = true,
                    // NOT disabled while a previous task is being sorted. The classify call
                    // is a round trip to a Cloud Function, so the field would die for a
                    // second or more every time — and the whole point of a quick-add is that
                    // you can type the next thing while the last one lands.
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onClassify(title, alreadyDone); title = ""; alreadyDone = false },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Sort") }
            }
            // `#7`, `R6`: *"there should be a way to complete the task from within quick add"*.
            // The four navigations it removes are add-on-dashboard, find-the-goal, open it, tick
            // the row — for a task typed in precisely because it is already finished.
            //
            // A CHIP UNDER THE ROW, NOT A THIRD CONTROL INSIDE IT. The row is already a text
            // field that must stay wide enough to read, a button, and their padding; a third
            // item makes the field too narrow on a phone to see what you are typing. And §0.8's
            // surviving sub-rule is *form and words before iconography*, so this cannot shrink
            // to a bare tick mark to fit.
            //
            // IT RESETS ON EVERY ADD, deliberately, and that is one extra tap when logging
            // several finished things in a row. A toggle that stayed on would be a mode that
            // silently completes the NEXT task typed — the app doing something unasked and
            // unannounced, which is the one thing §0.7 does not permit even for filing.
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = alreadyDone,
                    onClick = { alreadyDone = !alreadyDone },
                    label = { Text(SmartAddTestTags.ALREADY_DONE_LABEL) },
                    leadingIcon = if (alreadyDone) {
                        { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag(SmartAddTestTags.ALREADY_DONE),
                )
            }
            // The only thing left of the old dialog: while a task is in flight the card says
            // so, in place, without taking the screen. #6 removed the confirmation, not the
            // feedback — a tap that appears to do nothing for a second reads as a broken
            // button, and the snackbar that follows arrives too late to answer that.
            if (state.isClassifying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag(SmartAddTestTags.SORTING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        // Says "as done" while it is in flight, because the chip that said so
                        // has already cleared on the tap that started this. Without it there is
                        // a round trip's worth of seconds in which nothing on screen agrees
                        // that a completion is being written.
                        if (state.alreadyDone) {
                            "Filing “${state.taskTitle}” as done…"
                        } else {
                            "Filing “${state.taskTitle}”…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

/** Test handles for the surfaces `#6` replaced the confirmation dialog with. */
object SmartAddTestTags {
    const val SORTING = "smartAdd:sorting"

    /** `#7`'s create-and-complete toggle. */
    const val ALREADY_DONE = "smartAdd:alreadyDone"

    /**
     * The chip's own words, shared with the tests.
     *
     * Named rather than duplicated because a test asserting a *copy* of a label passes
     * against a screen whose label was changed — it re-tests the copy, which is the same
     * mistake `SmartAddReceiptUiTest`'s header records about copying a composable.
     */
    const val ALREADY_DONE_LABEL = "Already done"
}

/**
 * Shows `#6`'s receipt as a snackbar with **Undo**, and clears it afterwards.
 *
 * ⚠️ **The order of the two calls is the whole of this function, and getting it wrong ships a
 * silent filing with no witness at all.** The first version consumed the receipt *before*
 * awaiting the snackbar, reasoning that `showSnackbar` suspends for the full duration and a
 * second quick-add during those seconds would otherwise queue behind it. Every layer was green
 * and **nothing was ever shown**: `consumeFiled()` nulls the very state this effect is keyed on,
 * so `LaunchedEffect` restarts and cancels the coroutine before it can reach `showSnackbar`.
 * Found by looking at a device, which is the only instrument that could have found it.
 *
 * So: **await first, consume after.** The concern that motivated the wrong order is answered by
 * the key rather than by the ordering — a *new* receipt changes `receipt`, restarting the effect,
 * which dismisses the stale snackbar and shows the current one. That is the behaviour wanted, and
 * it is why the consume is **not** in a `finally`: on cancellation the flow already holds the next
 * receipt, and clearing it there would swallow exactly the quick-add this was trying to protect.
 *
 * Split out of [DashboardScreen] rather than inlined so `SmartAddReceiptUiTest` can drive the real
 * wiring. Testing a copy of these six lines would have re-tested the copy and not the defect.
 */
@Composable
internal fun SmartAddReceiptSnackbar(
    receipt: SmartAddReceipt?,
    hostState: SnackbarHostState,
    onUndo: (SmartAddReceipt) -> Unit,
    onConsume: () -> Unit,
) {
    LaunchedEffect(receipt) {
        if (receipt == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = receipt.sentence(),
            actionLabel = "Undo",
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo(receipt) else onConsume()
    }
}

/**
 * What the app says after it has filed something without asking — `#6`'s witness (§0.7).
 *
 * *"Silent" is not "invisible".* Two sentences, decided by [FilingDecision.speaks]:
 *
 *  - **filed under a goal you already have** — the instrumental case, so it states the outcome
 *    and offers to undo it, and nothing more. §3.4 calls this row silent, and a snackbar that
 *    can be ignored is what silent means for an act that has already happened.
 *  - **no existing goal fit** — §3.4's one row that *speaks*, because an absent goal id does
 *    not degrade the outcome, it changes it. It **tells**; it does not ask. A proposed goal is
 *    named as a suggestion, and an unfiled task says so plainly.
 *
 * **`#7` doubles the table rather than prefixing it.** Completion is independent of filing —
 * any of the three outcomes can happen to a task typed in as already finished — so it could
 * have been a `"Done — "` prefix over the three sentences above. Written out instead, because
 * two of the three then read wrong: *"Done — No goal fitted"* capitalises mid-sentence, and
 * *"Done — Added “x” — no goal fits it yet"* carries two dashes and says *added* about a thing
 * whose news is that it is **finished**. Six short strings cost less than a rule with two
 * exceptions.
 *
 * **Why it has to be said at all.** The filed task is under a goal the user is not looking at,
 * so its tick is not on screen either; the receipt is the only place this screen can show that
 * the completion took. Undo needs no second offer — deleting the task takes the completion
 * with it.
 */
internal fun SmartAddReceipt.sentence(): String = when (val d = decision) {
    is FilingDecision.ExistingGoal ->
        if (completed) "Done — added to “${d.goalTitle}”" else "Added to “${d.goalTitle}”"

    is FilingDecision.NewGoal ->
        if (completed) {
            "Done — no goal fitted, suggested “${d.title}”"
        } else {
            "No goal fitted — suggested “${d.title}”"
        }

    is FilingDecision.NoGoal ->
        if (completed) {
            "Done — “$taskTitle” fits no goal yet"
        } else {
            "Added “$taskTitle” — no goal fits it yet"
        }
}

/**
 * The one branded surface on this screen (see [HeroSurface]). Points are the
 * number the user opens the app for, so they get the display type and the
 * gradient; everything below stays on neutral cards.
 */
@Composable
private fun PointsLevelCard(
    userName: String,
    points: Long,
    level: Int,
    levelProgress: Float,
    pointsToNext: Long,
    modifier: Modifier = Modifier,
) {
    val accents = MaterialTheme.gpAccents
    HeroSurface(modifier = modifier) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (userName.isBlank()) "Welcome back!" else "Hi $userName 👋",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$points",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = accents.onHeroVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 5.dp),
                )
            }

            GpLinearProgress(
                progress = levelProgress,
                color = accents.onHero,
                trackColor = Color.White.copy(alpha = 0.28f),
                height = 8.dp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "$pointsToNext pts to level ${level + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = accents.onHeroVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun OverviewCard(
    averageProgress: Float,
    goalCount: Int,
    doneTasks: Int,
    completedThisWeek: Int,
    onOpenAnalytics: () -> Unit,
) {
    val ringBrush = Brush.linearGradient(MaterialTheme.gpAccents.heroGradient)
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = averageProgress,
                    size = 92.dp,
                    strokeWidth = 11.dp,
                    brush = ringBrush,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        "${(averageProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .weight(1f),
                ) {
                    Text("Overall progress", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Averaged across all your goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            // Three counts read as three facts; the old "7 goals • 1 tasks done •
            // 1 this week" run-on read as one sentence you had to parse.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric(value = goalCount.toString(), label = "Goals")
                Metric(value = doneTasks.toString(), label = "Tasks done")
                Metric(value = completedThisWeek.toString(), label = "This week")
            }
            TextButton(
                onClick = onOpenAnalytics,
                modifier = Modifier.align(Alignment.End),
            ) { Text("View analytics") }
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecommendationCard(rec: Recommendation) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            IconChip(
                icon = Icons.Filled.AutoAwesome,
                tint = MaterialTheme.colorScheme.tertiary,
                size = 36.dp,
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(rec.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    rec.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareSummaryCard(onShare: () -> Unit, onShareWithPhoto: () -> Unit) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Filled.Share,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "Share your weekly progress",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                "Post a summary to your friends feed and climb the leaderboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("Share", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = onShareWithPhoto) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Share with photo")
                }
            }
        }
    }
}

/**
 * How a proposed duration reads before it is written (#9, §3.4).
 *
 * Null means the model did not supply one, and the sheet says so rather than
 * printing thirty minutes as though it were an answer — the task will still be
 * stored as `DEFAULT_MINUTES`, but as `DurationSource.UNKNOWN`, so it stays
 * re-estimable and is never counted among the durations the analytics card
 * attributes to the AI. Asking *"how long?"* here is #7's surface, not this one's.
 */
private fun durationLabel(minutes: Int?): String =
    if (minutes == null) {
        "no estimate · counts as ${formatMinutes(TaskDuration.DEFAULT_MINUTES)}"
    } else {
        "about ${formatMinutes(minutes)}"
    }
