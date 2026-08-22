package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.ui.components.EmptyState
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.components.toGoalInk
import com.idomarhaim.goalpilot.ui.tutorial.TutorialAnchor
import com.idomarhaim.goalpilot.ui.tutorial.tutorialAnchor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("My Goals") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGoal,
                // Step 5 of the guided tour points here. Tagged on the FAB
                // itself rather than on the Scaffold slot, so the spotlight is
                // the button's shape and not the slot's bounding box.
                modifier = Modifier.tutorialAnchor(TutorialAnchor.NEW_GOAL),
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
                        GoalListRow(
                            goal = goal,
                            onOpen = { onOpenGoal(goal.id) },
                            onKeep = { viewModel.keepSuggestion(goal.id) },
                            onDemote = { viewModel.demoteSuggestion(goal.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * One row of the goals list: the card, and — for a goal the sorter proposed — the two answers
 * to it (`#6`).
 *
 * Stateless and lambda-driven so `GoalsPendingSuggestionUiTest` can drive it with no Hilt, no
 * Firebase and no ViewModel. The behaviour worth testing is which goals get the banner at all,
 * and that lives here rather than in the banner itself.
 *
 * The banner sits **under** the card rather than as a chip on it. `GoalCard` is in
 * `ui/components`, which is one of the two packages already swept for `#51`, so a raw English
 * literal there would fail `AnalyticsLiteralSweepTest` — and AGENTS.md is explicit that opting
 * a package in as a favour is the wrong move while Hebrew is parked. §0.8's surviving second
 * sub-rule points the same way anyway: form and words before iconography, and *Suggested* is
 * two verbs' worth of meaning that no badge can carry.
 */
@Composable
internal fun GoalListRow(
    goal: Goal,
    onOpen: () -> Unit,
    onKeep: () -> Unit,
    onDemote: () -> Unit,
) {
    Column {
        GoalCard(goal = goal, onClick = onOpen)
        if (goal.isPendingSuggestion) {
            SuggestedGoalBanner(onKeep = onKeep, onDemote = onDemote)
        }
    }
}

/**
 * A goal the sorter proposed, and the two answers to it — §1.1 (`#6`).
 *
 * §0.7 lets the app file a task under an existing goal without asking, and forbids it to invent
 * a goal. `#6` keeps both halves by letting an `AI_SUGGESTED` objective **sit pending** instead
 * of silently joining the list: it holds the task, it is legible as the model's, and Ido settles
 * it in one tap.
 *
 * **Neither answer destroys anything.** *Keep* stamps the marker to `USER`; *Not a goal* drops
 * it, and the object and all its edges survive — that is §1.1's *lossless demotion*, and the
 * reason there is no *Delete* here. The task underneath is real work he typed in.
 */
@Composable
private fun SuggestedGoalBanner(onKeep: () -> Unit, onDemote: () -> Unit) {
    // ⚠️ **Welded to the card above it, and that is a correctness requirement rather than
    // polish.** The first version was a bare row with 2.dp of top padding, sitting in a list
    // whose `Arrangement.spacedBy(12.dp)` puts 12.dp below it. On a device those two gaps read
    // as **near-equal** — the buttons carry Material's 48.dp minimum touch height, so the text
    // floats in the middle of a tall row and the 2-versus-12 difference all but disappears.
    // A banner that could belong to either neighbour is not a cosmetic problem here: *Not a
    // goal* changes a goal's status, so misreading which card it belongs to demotes the wrong
    // one. Found by looking at it on `Pixel_10_Pro_XL`, which is the only instrument that could
    // have: every assertion in `SilentFilingUiTest` passes either way, because they ask whether
    // the banner exists and never where it appears to point.
    //
    // So it is inset, tinted, and its top corners are square where they meet the card — one
    // object with a drawer pulled out from under it, rather than two objects with a gap.
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                // Inset from the card above, which is what makes it read as subordinate to it
                // rather than as a row of its own. `Observed:` the label wraps to two lines at
                // this width and did at 24.dp too — two buttons plus a sentence do not fit one
                // line on a phone, and 16.dp is simply the widest inset that still reads as an
                // inset. Two lines is legible and `maxLines = 2` is deliberate; the earlier
                // comment here claimed 16.dp fixed the wrap, and looking at the render says it
                // does not.
                .padding(start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                .padding(start = 12.dp, end = 4.dp)
                .testTag(GoalsTestTags.SUGGESTED_BANNER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Suggested — not yet one of your goals",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            TextButton(onClick = onDemote, modifier = Modifier.testTag(GoalsTestTags.DEMOTE)) {
                Text("Not a goal")
            }
            TextButton(onClick = onKeep, modifier = Modifier.testTag(GoalsTestTags.KEEP)) {
                Text("Keep")
            }
        }
    }
}

/** Test handles for `#6`'s pending-goal surface. */
object GoalsTestTags {
    const val SUGGESTED_BANNER = "goals:suggested"
    const val KEEP = "goals:suggested:keep"
    const val DEMOTE = "goals:suggested:demote"
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
    // The icon takes the fill, the name takes the ink -- see `String.toGoalInk`.
    val ink = area?.colorHex?.toGoalInk() ?: MaterialTheme.colorScheme.onSurfaceVariant
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
            color = ink,
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
