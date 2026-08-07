package com.idomarhaim.goalpilot.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _friendsOnly = MutableStateFlow(false)

    val uiState: StateFlow<SocialUiState> = _friendsOnly.flatMapLatest { friendsOnly ->
        combine(
            socialRepository.observeLeaderboard(friendsOnly),
            socialRepository.observeFeed(),
            socialRepository.observeFriendUids(),
        ) { leaderboard, feed, friends ->
            SocialUiState(
                isLoading = false,
                friendsOnly = friendsOnly,
                leaderboard = leaderboard,
                feed = feed,
                friendUids = friends,
            )
        }
    }.catch { emit(SocialUiState(isLoading = false, error = it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SocialUiState(isLoading = true))

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun setFriendsOnly(value: Boolean) { _friendsOnly.value = value }

    /** Adds a friend straight from a leaderboard row (uid already known). */
    fun addFriend(uid: String) {
        viewModelScope.launch { report(socialRepository.addFriend(uid), "Friend added") }
    }

    /** Adds a friend from the short code typed into the "Add a friend" dialog. */
    fun addFriendByCode(code: String) {
        viewModelScope.launch { report(socialRepository.addFriendByCode(code), "Friend added") }
    }

    fun removeFriend(uid: String) {
        // Went through report() in the issue #3 sweep: this used to announce
        // "Friend removed" before looking at the result, so a failed removal
        // claimed to have succeeded and the row stayed on screen contradicting it.
        viewModelScope.launch { report(socialRepository.removeFriend(uid), "Friend removed") }
    }

    /** Surfaces the repository's own error text — "no user with that code" is
     *  far more actionable than a generic failure message. */
    private fun report(result: Resource<Unit>, onSuccess: String) {
        _message.value = when (result) {
            is Resource.Success -> onSuccess
            is Resource.Error -> result.message
            Resource.Loading -> null
        }
    }

    fun consumeMessage() { _message.value = null }
}

data class SocialUiState(
    val isLoading: Boolean = true,
    val friendsOnly: Boolean = false,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val feed: List<SharedItem> = emptyList(),
    val friendUids: Set<String> = emptySet(),
    val error: String? = null,
)
