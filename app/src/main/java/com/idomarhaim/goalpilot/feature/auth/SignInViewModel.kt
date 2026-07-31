package com.idomarhaim.goalpilot.feature.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.auth.GoogleAuthClient
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val googleAuthClient: GoogleAuthClient,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    /** The Google Sign-In Intent the screen launches. */
    fun signInIntent(): Intent = googleAuthClient.signInIntent

    fun onSignInLaunched() = _state.update { it.copy(isLoading = true, error = null) }

    /** Called with the ActivityResult data from the Google Sign-In flow. */
    fun onSignInResult(data: Intent?) {
        googleAuthClient.extractIdToken(data).fold(
            onSuccess = { token -> exchangeToken(token) },
            onFailure = { e ->
                _state.update { it.copy(isLoading = false, error = e.message ?: "Sign-in cancelled") }
            },
        )
    }

    private fun exchangeToken(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            // On success the root auth-state flow flips to SignedIn and navigates away.
            when (val result = authRepository.signInWithGoogleIdToken(idToken)) {
                is Resource.Error ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

data class SignInState(
    val isLoading: Boolean = false,
    val error: String? = null,
)
