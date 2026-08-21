package com.idomarhaim.goalpilot.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import com.idomarhaim.goalpilot.domain.model.FriendCode
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.ui.components.Avatar
import com.idomarhaim.goalpilot.ui.components.FreshnessNote
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.gpCardColors
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppDropdownMenu

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
    var openedPhoto by remember { mutableStateOf<SharedItem?>(null) }
    var pendingDelete by remember { mutableStateOf<SharedItem?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
            when {
                // Never fetched on this device, so the app does not know whether
                // there is anyone to show. Saying "no one here yet" would be the app
                // stating a fact about other people's data it has never read (#50).
                state.leaderboardFreshness.neverLoaded -> item { FreshnessNote("Not loaded yet") }

                state.leaderboard.isEmpty() -> item {
                    Text(
                        "No one here yet. Add a friend by their code to compare progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    if (state.leaderboardFreshness.hasStamp) {
                        item {
                            FreshnessNote(
                                "Leaderboard as of " +
                                    DateTimeUtils.formatAsOf(
                                        state.leaderboardFreshness.asOfEpochMillis,
                                    ),
                            )
                        }
                    }
                    items(state.leaderboard, key = { it.uid }) { entry ->
                        LeaderboardRow(
                            entry = entry,
                            onAddFriend = { viewModel.addFriend(entry.uid) },
                            onRemoveFriend = { viewModel.removeFriend(entry.uid) },
                        )
                    }
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
                items(state.feed, key = { it.id }) { item ->
                    FeedCard(
                        item = item,
                        onOpenPhoto = { openedPhoto = item },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    if (showAddFriend) {
        AddFriendDialog(
            onDismiss = { showAddFriend = false },
            onAdd = { code -> viewModel.addFriendByCode(code); showAddFriend = false },
        )
    }

    openedPhoto?.let { item ->
        FullScreenPhotoDialog(
            imageUrl = item.imageUrl.orEmpty(),
            contentDescription = photoDescription(item),
            onDismiss = { openedPhoto = null },
        )
    }

    pendingDelete?.let { item ->
        DeletePostDialog(
            item = item,
            onConfirm = { viewModel.deleteShare(item); pendingDelete = null },
            onDismiss = { pendingDelete = null },
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
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        gpCardColors()
    }
    GpCard(modifier = Modifier.fillMaxWidth(), colors = highlight) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A podium place should look like one. Ranks 1-3 get the tertiary
            // accent disc; everyone else keeps a quiet numeral.
            val isPodium = entry.rank in 1..3
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPodium) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            Color.Transparent
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPodium) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
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

/**
 * One post in the friends feed.
 *
 * Until issues `#4` and `#5` this card contained **no interactive node at all** —
 * the accessibility tree's last `clickable` on the screen was the "Challenges"
 * link above the feed. It now carries two, and they are separate fixes rather
 * than one: the photo opens *and* is announced (making it tappable would not have
 * given it a label), and a post you wrote offers to delete itself.
 */
@Composable
internal fun FeedCard(
    item: SharedItem,
    onOpenPhoto: () -> Unit,
    onDelete: () -> Unit,
) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(photoUrl = item.authorPhotoUrl, name = item.authorName, size = 36.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    Text(
                        authorLabel(item),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        DateTimeUtils.relative(item.createdAtEpochMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Only on your own post. There is no moderation story here and no
                // rule that would permit one — `firestore.rules` scopes delete to
                // the author — so showing the menu on a friend's post would offer
                // an action the backend is guaranteed to refuse.
                if (item.isMine) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Post options")
                        }
                        AppDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete post") },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
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
                    // Was `null`, which is the API's way of saying "decorative" —
                    // so a screen reader announced the post with the picture
                    // simply missing. It is content, not decoration.
                    contentDescription = photoDescription(item),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        // clickable after clip, or the ripple spills past the
                        // rounded corners.
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Open photo full screen", onClick = onOpenPhoto),
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

/** The name to show for a post's author, for both sighted and screen readers. */
internal fun authorLabel(item: SharedItem): String =
    item.authorName.ifBlank { "GoalPilot user" }

/**
 * What a screen reader says about an attached photo.
 *
 * Deliberately not the headline or the message: those are their own text nodes
 * on the card and would be read twice. What is missing without this is the fact
 * that a picture is there at all, and whose it is.
 */
internal fun photoDescription(item: SharedItem): String =
    "Photo shared by ${authorLabel(item)}"

/**
 * Confirms deleting your own post — and names the second consequence.
 *
 * Deleting a share also deletes the image it carried, which is not something the
 * word "delete" on a text post implies. A user who would have kept the photo
 * needs to hear that before the tap, not after.
 */
@Composable
internal fun DeletePostDialog(
    item: SharedItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this post?") },
        text = {
            Text(
                if (item.imageUrl.isNullOrBlank()) {
                    "It will disappear from everyone's feed. This cannot be undone."
                } else {
                    "It will disappear from everyone's feed, and the attached photo " +
                        "will be deleted too. This cannot be undone."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a friend") },
        text = {
            Column {
                Text(
                    "Type your friend's ${FriendCode.LENGTH}-character code — they'll " +
                        "find it on their Profile tab.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    // Normalising as the user types means a lowercase or spaced-out
                    // code still matches, and the field can't exceed the code length.
                    value = code,
                    onValueChange = { code = FriendCode.normalize(it).take(FriendCode.LENGTH) },
                    label = { Text("Friend code") },
                    placeholder = { Text("7KQ4RD") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(code) },
                enabled = FriendCode.isValid(code),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
