package com.idomarhaim.goalpilot.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.data.auth.GoogleAuthClient
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile is the **account** surface (spec §4.9).
 *
 * ⚠️ **It no longer holds `AppPreferencesRepository`, and that absence is the
 * point of #48.** The skin and language pickers lived here because no Settings
 * screen existed; §5.1's own reason for storing language per-device is that
 * *it must be known before the first frame, and the account is not known until
 * Auth resolves* — so a device control on the account screen was unreachable
 * exactly when its justification said it was needed. Both moved to
 * `feature/settings/`. Anything device-scoped arriving here again is that
 * defect coming back.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient,
) : ViewModel() {

    val user: StateFlow<User?> = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signOut() {
        viewModelScope.launch {
            // Sign out of Google first so the account picker shows next time,
            // then out of Firebase (which flips the root auth state).
            googleAuthClient.signOut()
            authRepository.signOut()
        }
    }
}
