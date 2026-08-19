package com.idomarhaim.goalpilot.feature.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme

/**
 * The privacy rationale Health Connect requires of every app that reads health
 * data. Health Connect launches it from its own settings screens — GoalPilot
 * never navigates here itself — so it is a standalone exported activity rather
 * than a route in the main nav graph.
 *
 * Declaring it is not optional: an app that handles neither
 * `ACTION_SHOW_PERMISSIONS_RATIONALE` nor (on Android 14+) the
 * `VIEW_PERMISSION_USAGE` alias is refused the health permissions outright.
 *
 * No Hilt here on purpose — it injects nothing, and `@AndroidEntryPoint` on an
 * activity the system can launch before the app has otherwise started is a
 * failure mode for no benefit. It therefore renders the default skin rather than
 * the user's chosen one, which is the right trade for a screen shown once.
 */
class HealthPermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GoalPilotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HealthRationaleContent()
                }
            }
        }
    }
}

@Composable
private fun HealthRationaleContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // This activity has no Scaffold to inset for it, and `enableEdgeToEdge`
            // means the heading would otherwise sit underneath the status bar.
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("How GoalPilot uses your health data", style = MaterialTheme.typography.headlineSmall)

        Text(
            "GoalPilot reads two things from Health Connect, and only when you " +
                "tap “Sync health data” yourself:",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "• Steps — your daily step count for the last week.\n" +
                "• Sleep — how long each sleep session lasted, for the same week.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("What happens to it", style = MaterialTheme.typography.titleMedium)
        Text(
            "Readings are shown to you first. Nothing is saved until you review " +
                "the list and confirm it. What you confirm becomes a progress entry " +
                "against one of your own goals, stored in your private GoalPilot " +
                "account — the same place your manual progress logs go.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("What GoalPilot never does", style = MaterialTheme.typography.titleMedium)
        Text(
            "It never writes anything back to Health Connect, never reads any " +
                "health data type beyond the two above, never shares your health " +
                "readings with other users, and never sends them to the AI coach. " +
                "You can revoke access at any time from the Health Connect app; " +
                "GoalPilot keeps working without it.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
