package com.idomarhaim.goalpilot.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.usecase.CalendarSyncTrigger
import com.idomarhaim.goalpilot.domain.usecase.HealthSyncTrigger
import com.idomarhaim.goalpilot.domain.usecase.SyncCalendarUseCase
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
    private val syncCalendar: SyncCalendarUseCase,
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
     *
     * §2.7's calendar sync rides the same trigger and is the **only** one it can ride: *"there
     * is no credential for a background sync and cannot be one"* — `GoogleAuthUtil` mints
     * short-lived tokens with no refresh token, and `C9d` banned the service account. So this
     * lifecycle callback is not one option among several; it is the whole schedule.
     *
     * The two launches are deliberately separate coroutines. They share no state, and a Health
     * Connect read that stalls on a cold permission check must not hold the calendar pull
     * behind it — nor the reverse, where a network round-trip to Google would delay a reading
     * that is already on the device.
     */
    fun onAppForegrounded() {
        viewModelScope.launch {
            syncHealthData(HealthSyncTrigger.APP_FOREGROUND)
        }
        viewModelScope.launch {
            syncCalendar(CalendarSyncTrigger.APP_FOREGROUND)
        }
    }
}

/** Top-level gate: are we still checking, signed out, or signed in? */
sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val user: User) : AuthUiState
}
