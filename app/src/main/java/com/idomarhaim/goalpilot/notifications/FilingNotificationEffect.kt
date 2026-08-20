package com.idomarhaim.goalpilot.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.idomarhaim.goalpilot.domain.model.FilingDecision
import dagger.hilt.android.EntryPointAccessors

/**
 * The system-notification half of `R5`, and the moment #8 asks for `POST_NOTIFICATIONS`.
 *
 * Sits beside `SmartAddReceiptSnackbar` in `DashboardScreen` as a **second, independent**
 * consumer of the same `filed` flow. It reads that flow and writes nothing back, which is what
 * lets a refused permission change nothing: the snackbar is a different effect on a different
 * collector and never learns whether this one posted (see [GoalPilotNotifier]).
 *
 * **Why the ask lives here and not in `MainActivity`.** The permission dialog needs an
 * `ActivityResultLauncher`, so it has to be raised from composition — but more importantly, the
 * *decision* of when to raise it is [NotificationPermissionPolicy]'s and is keyed on a filing
 * outcome that speaks. This is the only place in the app that observes such an outcome, so
 * putting the launcher anywhere else would mean signalling back to it.
 */
@Composable
fun FilingNotificationEffect(
    taskId: String?,
    decision: FilingDecision?,
    taskTitle: String?,
    createdGoalId: String?,
    // Defaulted rather than required, so the one call site in DashboardScreen stays four lines.
    // Reached through the app-scoped entry point rather than a ViewModel: this composable owns
    // no state, and a ViewModel whose only job is to carry one @Singleton across the Hilt
    // boundary is a class that exists to satisfy an injection style.
    notifier: GoalPilotNotifier = rememberNotifier(),
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // The user has just answered. If they said yes, the event that prompted the question
        // is still the newest one, so post it now rather than making them do it again.
        if (granted && decision != null && taskTitle != null) {
            notifier.notifyFiling(decision, taskTitle, createdGoalId)
        }
    }

    // Keyed on the task id rather than the decision: two identical quick-adds in a row produce
    // equal decisions and equal titles, and keying on those would silently drop the second.
    LaunchedEffect(taskId) {
        if (taskId == null || decision == null || taskTitle == null) return@LaunchedEffect
        when (
            NotificationPermissionPolicy.decide(
                sdkInt = Build.VERSION.SDK_INT,
                granted = notifier.hasPostPermission(),
                askedThisProcess = NotificationAsk.askedThisProcess,
                eventSpeaks = decision.speaks,
            )
        ) {
            PermissionStep.ASK_NOW -> {
                NotificationAsk.askedThisProcess = true
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            PermissionStep.NOT_APPLICABLE, PermissionStep.ALREADY_GRANTED ->
                notifier.notifyFiling(decision, taskTitle, createdGoalId)

            // Refused, or nothing worth asking about. The snackbar has already said it.
            PermissionStep.ALREADY_ASKED, PermissionStep.WAIT_FOR_A_REASON -> Unit
        }
    }
}

/**
 * The in-memory *"we already raised the dialog"* guard.
 *
 * Process-scoped and deliberately not persisted — [NotificationPermissionPolicy] explains why a
 * stored flag would buy nothing. Its only job is to stop one session stacking two dialogs.
 */
internal object NotificationAsk {
    var askedThisProcess: Boolean = false
}

@Composable
private fun rememberNotifier(): GoalPilotNotifier {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationEntryPoint::class.java,
        ).notifier()
    }
}
