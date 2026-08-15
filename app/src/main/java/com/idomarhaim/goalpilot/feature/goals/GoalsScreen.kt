package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toGoalAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Goals") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGoal,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New goal") },
            )
        },
    ) { inner ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(inner))
            state.totalGoals == 0 -> EmptyState(
                title = "No goals yet",
                subtitle = "Tap “New goal” to define your first life goal and start tracking.",
                icon = Icons.Outlined.Flag,
                modifier = Modifier.padding(inner),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                // The extended FAB floats over this list; without the extra
                // bottom room the last goal card sat underneath it.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.groups.forEach { group ->
                    // A single unfiled band gets no header: a lone "No life area"
                    // over the whole list tells a user who has not adopted areas
                    // nothing they did not already know.
                    val showHeader = group.area != null || state.groups.size > 1
                    if (showHeader) {
                        item(key = "area-${group.area?.id ?: "unfiled"}") {
                            LifeAreaGroupHeader(area = group.area, goalCount = group.goals.size)
                        }
                    }
                    // Keyed by band **and** goal, not by goal alone. Since spec
                    // §1.2 a goal serves many areas, so the same goal legitimately
                    // appears in two bands of this one list — and a `LazyColumn`
                    // throws outright on a repeated key, which would crash the
                    // Goals tab the first time anyone ticks a second area in the
                    // goal editor.
                    items(
                        group.goals,
                        key = { "${group.area?.id ?: "unfiled"}-${it.id}" },
                    ) { goal ->
                        GoalCard(goal = goal, onClick = { onOpenGoal(goal.id) })
                    }
                }
            }
        }
    }
}

/**
 * The band header on the goals list. Deliberately a header rather than another
 * chip on every [GoalCard]: the card's colour and meta line already belong to the
 * goal's *category*, and a second differently-coloured token per row read as
 * noise against the real list. A header states the area once and gives the list
 * the shape the user filed their goals into.
 */
@Composable
private fun LifeAreaGroupHeader(area: LifeArea?, goalCount: Int) {
    val accent = area?.colorHex?.toGoalAccent() ?: MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconForKey(area?.iconKey ?: "flag"),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = area?.name ?: "No life area",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Text(
            // Direction-isolated (§4.8): a Latin digit run in an RTL paragraph is
            // reordered by the bidi algorithm.
            text = "${"$goalCount".bidiIsolated()} goal${if (goalCount == 1) "" else "s"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
