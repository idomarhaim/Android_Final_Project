package com.idomarhaim.goalpilot.feature.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The **Connected apps** section of Settings: Google Tasks import, Health data.
 *
 * ## Why it is a slot and not part of `SettingsContent`
 *
 * `SettingsContent` is deliberately Hilt-free — every value in, every edit out —
 * so the whole settings surface can be driven from a `createComposeRule()` test
 * with no DI graph and no Firebase. This section cannot be: it registers two
 * `ActivityResultContract` launchers, which only a composable may do, and both
 * of them talk to Google. So it is handed in as a `@Composable` slot, exactly
 * the way `onReplayTutorial` is handed in as a nullable lambda, and the
 * signed-out branch passes `null` for the same reason it does there — there is
 * no account, so there is nothing to import into and no per-account sync.
 *
 * ## Both launchers live here, and that is not incidental
 *
 * They used to sit in `DashboardScreen`, which is the only reason the cards were
 * on Home at all: a permission contract has to be registered from a composable,
 * so whichever screen drew the card owned the launcher. Moving the card moves
 * the launcher, and the ViewModel is unchanged by either — which is the tell
 * that the coupling was always to the *screen* and never to the *dashboard*.
 */
@Composable
fun SyncSection(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val tasksImport by viewModel.tasksImport.collectAsStateWithLifecycle()
    val healthSync by viewModel.healthSync.collectAsStateWithLifecycle()
    val consentIntent by viewModel.consentIntent.collectAsStateWithLifecycle()
    val tasksConsent by viewModel.tasksConsent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.ensureHealthAvailability()
        // Re-read on every entry, not once: the life-areas screen grants the same
        // scope, and this ViewModel outlives a trip there and back (#36).
        viewModel.refreshTasksConsent()
    }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.consumeMessage() }
    }

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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GoogleTasksImportCard(
            isLoading = tasksImport.isLoading,
            consent = tasksConsent,
            onImport = viewModel::importGoogleTasks,
        )
        HealthConnectCard(
            state = healthSync,
            onSync = viewModel::syncHealth,
        )
    }

    if (tasksImport.isVisible) {
        GoogleTasksImportDialog(
            state = tasksImport,
            onToggle = viewModel::toggleImportProposal,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::dismissImport,
        )
    }
}
