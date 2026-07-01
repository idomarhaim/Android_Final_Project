package com.idomarhaim.goalpilot.feature.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.ui.components.Avatar
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    onOpenChallenges: () -> Unit,
    viewModel: SocialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showAddFriend by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Social") },
                actions = {
                    IconButton(onClick = { showAddFriend = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add friend")
                    }
                },
            )
        },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.friendsOnly,
                        onClick = { viewModel.setFriendsOnly(false) },
                        label = { Text("Everyone") },
                    )
                    FilterChip(
                        selected = state.friendsOnly,
                        onClick = { viewModel.setFriendsOnly(true) },
                        label = { Text("Friends") },
                    )
                }
            }
            item { SectionHeader(title = "Leaderboard") }
            if (state.leaderboard.isEmpty()) {
                item {
                    Text(
                        "No one here yet. Add a friend by their code to compare progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.leaderboard, key = { it.uid }) { entry ->
                    LeaderboardRow(
                        entry = entry,
                        onAddFriend = { viewModel.addFriend(entry.uid) },
                        onRemoveFriend = { viewModel.removeFriend(entry.uid) },
                    )
                }
            }
            item {
                SectionHeader(
                    title = "Friends feed",
                    action = { TextButton(onClick = onOpenChallenges) { Text("Challenges") } },
                )
            }
            if (state.feed.isEmpty()) {
                item {
                    Text(
                        "No shared summaries yet. Share your weekly progress from the Home tab!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.feed, key = { it.id }) { item -> FeedCard(item) }
            }
        }
    }

    if (showAddFriend) {
        AddFriendDialog(
            onDismiss = { showAddFriend = false },
            onAdd = { code -> viewModel.addFriend(code); showAddFriend = false },
        )
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
) {
    val highlight = if (entry.isCurrentUser) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = highlight) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
            )
            Avatar(photoUrl = entry.photoUrl, name = entry.displayName, size = 40.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = entry.displayName.ifBlank { "GoalPilot user" } +
                        if (entry.isCurrentUser) " (you)" else "",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Level ${entry.level} • ${entry.points} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!entry.isCurrentUser) {
                if (entry.isFriend) {
                    IconButton(onClick = onRemoveFriend) {
                        Icon(Icons.Filled.PersonRemove, contentDescription = "Remove friend")
                    }
                } else {
                    IconButton(onClick = onAddFriend) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add friend")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCard(item: SharedItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(photoUrl = item.authorPhotoUrl, name = item.authorName, size = 36.dp)
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        item.authorName.ifBlank { "GoalPilot user" },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        DateTimeUtils.relative(item.createdAtEpochMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                item.headline,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                item.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "  ${item.points} pts • ${item.completedTasks} tasks",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a friend") },
        text = {
            Column {
                Text(
                    "Paste your friend's code (their user id from the Profile tab).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Friend code") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(code) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
