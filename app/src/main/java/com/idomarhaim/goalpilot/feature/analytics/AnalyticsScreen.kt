package com.idomarhaim.goalpilot.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.ui.components.BarItem
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.HorizontalBarChart
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.toComposeColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(inner))
            state.goals.isEmpty() -> EmptyState(
                title = "Nothing to chart yet",
                subtitle = "Add goals and complete tasks to see your analytics.",
                icon = Icons.Outlined.BarChart,
                modifier = Modifier.padding(inner),
            )

            else -> {
                val progressBars = state.goals.map { g ->
                    BarItem(
                        label = g.title.ifBlank { "Untitled" },
                        fraction = g.progressFraction,
                        color = g.colorHex.toComposeColor(MaterialTheme.colorScheme.primary),
                        trailing = "${g.progressPercent}%",
                    )
                }
                val doneByGoal = state.tasks
                    .filter { it.isDone && it.goalId != null }
                    .groupingBy { it.goalId!! }
                    .eachCount()
                val totalDone = doneByGoal.values.sum().coerceAtLeast(1)
                val focusBars = state.goals
                    .map { g ->
                        val count = doneByGoal[g.id] ?: 0
                        val share = count.toFloat() / totalDone
                        BarItem(
                            label = g.title.ifBlank { "Untitled" },
                            fraction = share,
                            color = g.colorHex.toComposeColor(MaterialTheme.colorScheme.tertiary),
                            trailing = "${(share * 100).roundToInt()}%",
                        )
                    }
                    .filter { it.fraction > 0f }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ChartCard("Progress by goal", "How far along each goal is.") {
                        HorizontalBarChart(items = progressBars)
                    }
                    ChartCard(
                        title = "Task focus",
                        subtitle = "Share of completed tasks per goal (where your effort goes).",
                    ) {
                        if (focusBars.isEmpty()) {
                            Text(
                                "Complete some tasks to see your focus split.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            HorizontalBarChart(items = focusBars)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
