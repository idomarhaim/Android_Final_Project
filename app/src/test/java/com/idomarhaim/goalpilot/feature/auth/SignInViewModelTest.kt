package com.idomarhaim.goalpilot.feature.auth

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.data.auth.GoogleAuthClient
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val googleAuthClient = mockk<GoogleAuthClient>()
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `a failed token extraction surfaces the error and stops loading`() {
        every { googleAuthClient.extractIdToken(any()) } returns
            Result.failure(Exception("Sign-in cancelled"))
        val viewModel = SignInViewModel(googleAuthClient, authRepository)

        viewModel.onSignInResult(null)

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Sign-in cancelled")
    }

    @Test
    fun `a repository error during exchange is surfaced`() {
        every { googleAuthClient.extractIdToken(any()) } returns Result.success("id-token")
        coEvery { authRepository.signInWithGoogleIdToken("id-token") } returns
            Resource.Error("Firebase rejected the token")
        val viewModel = SignInViewModel(googleAuthClient, authRepository)

        viewModel.onSignInResult(null)

        assertThat(viewModel.state.value.error).isEqualTo("Firebase rejected the token")
    }

    @Test
    fun `a successful exchange leaves no error`() {
        every { googleAuthClient.extractIdToken(any()) } returns Result.success("id-token")
        coEvery { authRepository.signInWithGoogleIdToken("id-token") } returns
            Resource.Success(User(uid = "u1", displayName = "Ido"))
        val viewModel = SignInViewModel(googleAuthClient, authRepository)

        viewModel.onSignInResult(null)

        assertThat(viewModel.state.value.error).isNull()
    }
}
