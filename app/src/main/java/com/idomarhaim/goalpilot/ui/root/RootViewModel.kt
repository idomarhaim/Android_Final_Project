package com.idomarhaim.goalpilot.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncTrigger
import com.idomarhaim.goalpilot.domain.usecase.SyncHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val syncHealthData: SyncHealthDataUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = authRepository.authState()
        .map { user -> if (user == null) AuthUiState.SignedOut else AuthUiState.SignedIn(user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState.Loading)

    /**
     * Health Connect is re-read whenever the app comes forward — cold start, a
     * return from the recents list, or the moment sign-in completes.
     *
     * The trigger lives here, at the root, rather than on the dashboard: coming
     * back to the app on the profile tab is still coming back to the app, and a
     * dashboard-scoped effect would miss it. Everything that makes this safe to
     * fire that often — the fifteen-minute throttle, the in-flight guard, the
     * per-day dedupe — is inside [SyncHealthDataUseCase], so calling it too eagerly
     * costs one map lookup.
     */
    fun onAppForegrounded() {
        viewModelScope.launch {
            syncHealthData(HealthSyncTrigger.APP_FOREGROUND)
        }
    }
}

/** Top-level gate: are we still checking, signed out, or signed in? */
sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val user: User) : AuthUiState
}
