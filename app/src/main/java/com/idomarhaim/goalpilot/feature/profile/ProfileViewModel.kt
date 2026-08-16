package com.idomarhaim.goalpilot.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.data.auth.GoogleAuthClient
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    val user: StateFlow<User?> = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Mirrors the store directly — it is already a hot [StateFlow] of the stored value. */
    val skin: StateFlow<AppSkin> = appPreferences.skin

    fun setSkin(skin: AppSkin) = appPreferences.setSkin(skin)

    /** Same mirroring as [skin]; see [AppPreferencesRepository.language] (spec §5.1). */
    val language: StateFlow<AppLanguage> = appPreferences.language

    fun setLanguage(language: AppLanguage) = appPreferences.setLanguage(language)

    fun signOut() {
        viewModelScope.launch {
            // Sign out of Google first so the account picker shows next time,
            // then out of Firebase (which flips the root auth state).
            googleAuthClient.signOut()
            authRepository.signOut()
        }
    }
}
