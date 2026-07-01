package com.idomarhaim.goalpilot.feature.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * NICE-TO-HAVE (spec §6, §7): shared & competitive challenges. The [Challenge]
 * domain model + Firestore `challenges` collection already exist; this screen
 * previews the UI with sample data and is wired into navigation. Real
 * create/join/standings logic is tracked in TODO/TODO_OPTIONAL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Challenges") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Preview — challenges let you compete with friends on running, " +
                        "sleep, workouts and more. Coming next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(sampleChallenges) { c -> ChallengePreviewCard(c) }
        }
    }
}

private data class SampleChallenge(
    val title: String,
    val subtitle: String,
    val participants: Int,
    val icon: ImageVector,
)

private val sampleChallenges = listOf(
    SampleChallenge("7-day run streak", "Most km this week", 4, Icons.Filled.DirectionsRun),
    SampleChallenge("Sleep 8h challenge", "Best average sleep", 3, Icons.Filled.NightlightRound),
    SampleChallenge("Mindful minutes", "Most meditation minutes", 5, Icons.Filled.SelfImprovement),
)

@Composable
private fun ChallengePreviewCard(challenge: SampleChallenge) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                challenge.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    challenge.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(onClick = {}, label = { Text("${challenge.participants} in") })
        }
    }
}
