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

    fun addFriend(code: String) {
        val uid = code.trim()
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _message.value = when (socialRepository.addFriend(uid)) {
                is Resource.Success -> "Friend added"
                is Resource.Error -> "Could not add friend"
                Resource.Loading -> null
            }
        }
    }

    fun removeFriend(uid: String) {
        viewModelScope.launch {
            socialRepository.removeFriend(uid)
            _message.value = "Friend removed"
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
