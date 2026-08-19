package com.idomarhaim.goalpilot.feature.social

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.Leaderboard
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel-layer tests for deleting your own share (issue `#5`).
 *
 * The rules half of `#5` is proven in `firestore-tests/rules.test.mjs`, which is
 * the only layer that can reach `firestore.rules` and `storage.rules` at all.
 * What is left for the JVM is the part a rules test cannot see: that the app
 * hands the repository *both* halves of what has to be deleted, and that a
 * refusal reaches the user as the sentence the repository wrote.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<SocialRepository>(relaxed = true)

    private fun item(
        id: String = "share1",
        imageUrl: String? = null,
        isMine: Boolean = true,
    ) = SharedItem(
        id = id,
        authorUid = "me",
        authorName = "Ido",
        headline = "Weekly progress",
        message = "Earned 120 pts",
        points = 120,
        completedTasks = 4,
        imageUrl = imageUrl,
        isMine = isMine,
    )

    private fun viewModel(
        feed: List<SharedItem> = emptyList(),
        leaderboard: Leaderboard = Leaderboard(),
    ): SocialViewModel {
        every { repository.observeLeaderboard(any()) } returns flowOf(leaderboard)
        every { repository.observeFeed() } returns flowOf(feed)
        every { repository.observeFriendUids() } returns flowOf(emptySet())
        return SocialViewModel(repository)
    }

    @Test
    fun `deleting a post sends the image url along with the id`() = runTest {
        // The whole of #5 step 5 in one assertion: pass the id alone and the
        // photo survives the post that referenced it, invisible and permanent.
        coEvery { repository.deleteShare(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel()

        vm.deleteShare(item(id = "share1", imageUrl = "https://example.com/a.jpg"))

        coVerify { repository.deleteShare("share1", "https://example.com/a.jpg") }
    }

    @Test
    fun `a post with no photo deletes with a null image url`() = runTest {
        coEvery { repository.deleteShare(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel()

        vm.deleteShare(item(id = "share2", imageUrl = null))

        coVerify { repository.deleteShare("share2", null) }
    }

    @Test
    fun `a successful delete says so`() = runTest {
        coEvery { repository.deleteShare(any(), any()) } returns Resource.Success(Unit)
        val vm = viewModel()

        vm.deleteShare(item())

        assertThat(vm.message.first()).isEqualTo("Post deleted")
    }

    @Test
    fun `a refused delete surfaces the repository's own reason`() = runTest {
        // Same argument as the issue #3 sweep on removeFriend: announcing success
        // before looking at the result leaves the card on screen contradicting the
        // message. "You can only delete your own posts" is actionable;
        // "Could not delete" is not.
        coEvery { repository.deleteShare(any(), any()) } returns
            Resource.Error("You can only delete your own posts")
        val vm = viewModel()

        vm.deleteShare(item(isMine = false))

        assertThat(vm.message.first()).isEqualTo("You can only delete your own posts")
    }

    @Test
    fun `the feed carries the ownership flag the card needs`() = runTest {
        // isMine is stamped by the repository against the auth uid flow, not
        // derived in the UI from the leaderboard — which is bounded top-N, so a
        // user outside it would lose the delete on their own posts.
        val vm = viewModel(feed = listOf(item(id = "mine", isMine = true), item(id = "theirs", isMine = false)))

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.feed.single { it.id == "mine" }.isMine).isTrue()
        assertThat(state.feed.single { it.id == "theirs" }.isMine).isFalse()
    }

    // ── #50 · the as-of stamp and the never-loaded state ────────────────

    @Test
    fun `a never-loaded leaderboard reaches the screen as never-loaded, not as empty`() = runTest {
        // The heart of #50 item 3. `publicProfiles` is somebody else's data, so an
        // empty read that came from cache means this device has never seen the
        // collection — the app does not know whether anyone is there. The screen
        // has to be able to tell that from a genuine empty, or it renders "no one
        // here yet" about data it has never read.
        val vm = viewModel(
            leaderboard = Leaderboard(
                entries = emptyList(),
                freshness = Freshness(neverLoaded = true),
            ),
        )

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.leaderboard).isEmpty()
        assertThat(state.leaderboardFreshness.neverLoaded).isTrue()
    }

    @Test
    fun `an empty leaderboard that did reach the server is an ordinary empty`() = runTest {
        // The other half of the same discrimination, and the one that keeps the
        // first honest: empty-from-the-server means nobody is there, and must not
        // turn into "Not loaded yet".
        val vm = viewModel(leaderboard = Leaderboard(entries = emptyList()))

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.leaderboardFreshness.neverLoaded).isFalse()
    }

    @Test
    fun `the as-of stamp survives the trip to the screen`() = runTest {
        // The caption is drawn from this and nothing else — no clock, no radio.
        val vm = viewModel(
            leaderboard = Leaderboard(
                entries = listOf(LeaderboardEntry(uid = "u1", points = 10)),
                freshness = Freshness(asOfEpochMillis = 1_760_000_000_000L),
            ),
        )

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.leaderboardFreshness.asOfEpochMillis).isEqualTo(1_760_000_000_000L)
        assertThat(state.leaderboardFreshness.hasStamp).isTrue()
    }

    @Test
    fun `no stamp anywhere in the list means there is no caption to draw`() = runTest {
        // Rows written before #50 shipped carry no `updatedAt`. The caption is
        // unconditional on the *connection*, never on having something true to say.
        val vm = viewModel(
            leaderboard = Leaderboard(entries = listOf(LeaderboardEntry(uid = "u1"))),
        )

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.leaderboardFreshness.hasStamp).isFalse()
    }
}
