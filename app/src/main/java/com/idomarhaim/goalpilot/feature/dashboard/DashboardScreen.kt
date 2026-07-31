package com.idomarhaim.goalpilot.feature.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.domain.model.Recommendation
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.ProgressRing
import com.idomarhaim.goalpilot.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    onSeeAllGoals: () -> Unit,
    onOpenAnalytics: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recs by viewModel.recommendations.collectAsStateWithLifecycle()
    val smartAdd by viewModel.smartAdd.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.ensureRecommendations() }
    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }

    val sharePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.shareWeeklySummary(uri) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { TopAppBar(title = { Text("GoalPilot") }) },
    ) { inner ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(inner))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PointsLevelCard(
                    userName = state.userName,
                    points = state.points,
                    level = state.level,
                    levelProgress = state.levelProgress,
                    pointsToNext = state.pointsToNextLevel,
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
            item { SmartAddCard(onClassify = viewModel::classifyForSmartAdd) }
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

    if (smartAdd.isVisible) {
        SmartAddDialog(
            state = smartAdd,
            onConfirm = viewModel::confirmSmartAdd,
            onDismiss = viewModel::dismissSmartAdd,
        )
    }
}

/**
 * Entry point for the LLM task→goal classifier (spec §6 Bonus). Type a task in
 * plain language; the `classifyTask` Cloud Function decides which goal it belongs
 * to — or proposes a new one — and estimates its point value.
 */
@Composable
private fun SmartAddCard(onClassify: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "Smart add a task",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                "Describe anything you want to do — GoalPilot files it under the right goal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("e.g. Run 5 km on Friday") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onClassify(title); title = "" },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Sort") }
            }
        }
    }
}

@Composable
private fun SmartAddDialog(
    state: SmartAddState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isClassifying) "Analysing…" else "Add this task?") },
        text = {
            if (state.isClassifying) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("“${state.taskTitle}”", modifier = Modifier.padding(start = 12.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.taskTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.targetGoalId != null) {
                            "Goal: ${state.targetGoalTitle}"
                        } else {
                            "New goal: ${state.newGoalTitle} (${state.newGoalCategory.label})"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Worth ${state.points} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.rationale.isNotBlank()) {
                        Text(
                            state.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !state.isClassifying && !state.isSaving,
            ) { Text(if (state.isSaving) "Saving…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PointsLevelCard(
    userName: String,
    points: Long,
    level: Int,
    levelProgress: Float,
    pointsToNext: Long,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (userName.isBlank()) "Welcome back!" else "Hi $userName 👋",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Level $level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$points pts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { levelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Text(
                text = "$pointsToNext pts to level ${level + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(progress = averageProgress, size = 96.dp, strokeWidth = 10.dp) {
                Text(
                    "${(averageProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.padding(start = 20.dp).fillMaxWidth()) {
                Text("Overall progress", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$goalCount goals • $doneTasks tasks done • $completedThisWeek this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(
                    onClick = onOpenAnalytics,
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("View analytics") }
            }
        }
    }
}

@Composable
private fun RecommendationCard(rec: Recommendation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Share your weekly progress", style = MaterialTheme.typography.titleMedium)
            Text(
                "Post a summary to your friends feed and climb the leaderboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
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
