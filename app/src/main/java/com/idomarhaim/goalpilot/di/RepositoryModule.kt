package com.idomarhaim.goalpilot.di

import com.idomarhaim.goalpilot.data.auth.AuthRepositoryImpl
import com.idomarhaim.goalpilot.data.calendar.GoogleCalendarClient
import com.idomarhaim.goalpilot.data.firestore.ChallengeRepositoryImpl
import com.idomarhaim.goalpilot.data.firestore.GoalRepositoryImpl
import com.idomarhaim.goalpilot.data.firestore.LifeAreaRepositoryImpl
import com.idomarhaim.goalpilot.data.firestore.OccurrenceRepositoryImpl
import com.idomarhaim.goalpilot.data.firestore.ProgressRepositoryImpl
import com.idomarhaim.goalpilot.data.firestore.SocialRepositoryImpl
import com.idomarhaim.goalpilot.data.firestore.TaskRepositoryImpl
import com.idomarhaim.goalpilot.data.health.HealthConnectManager
import com.idomarhaim.goalpilot.data.prefs.AppPreferencesRepositoryImpl
import com.idomarhaim.goalpilot.data.remote.RecommendationRepositoryImpl
import com.idomarhaim.goalpilot.data.security.AiCredentialStore
import com.idomarhaim.goalpilot.data.security.DefaultAiProviderRepository
import com.idomarhaim.goalpilot.data.security.EncryptedAiCredentialStore
import com.idomarhaim.goalpilot.data.storage.StorageRepositoryImpl
import com.idomarhaim.goalpilot.domain.repository.AiProviderRepository
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.CalendarRepository
import com.idomarhaim.goalpilot.domain.repository.ChallengeRepository
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.SocialRepository
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * `C13`'s provider abstraction (#54). Bound like every other repository, so
     * the credential is reachable only through the domain interface — no caller
     * can hold the store and read the key off it.
     */
    @Binds
    @Singleton
    abstract fun bindAiProviderRepository(
        impl: DefaultAiProviderRepository,
    ): AiProviderRepository

    /**
     * The Keystore half, bound separately so the repository above stays free of
     * `Context` and testable on the JVM. Substituting this one binding is the
     * whole seam — see `AiCredentialStore`.
     */
    @Binds
    @Singleton
    abstract fun bindAiCredentialStore(impl: EncryptedAiCredentialStore): AiCredentialStore

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindLifeAreaRepository(impl: LifeAreaRepositoryImpl): LifeAreaRepository

    /**
     * §2.1's occurrence documents (`#63`). Bound separately from `TaskRepository` for the
     * reason `#55` gave the completion fact its own collection: `upsertTask` is a
     * whole-document `set()`, and an occurrence's outcome and `googleEventId` must survive an
     * ordinary retitle.
     */
    @Binds
    @Singleton
    abstract fun bindOccurrenceRepository(impl: OccurrenceRepositoryImpl): OccurrenceRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(impl: SocialRepositoryImpl): SocialRepository

    @Binds
    @Singleton
    abstract fun bindChallengeRepository(impl: ChallengeRepositoryImpl): ChallengeRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(impl: RecommendationRepositoryImpl): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(impl: AppPreferencesRepositoryImpl): AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthConnectManager): HealthRepository

    /**
     * §2.6's Google Calendar (`#61`). Bound like every other Google integration, so nothing
     * above the domain interface can reach the REST client, the OAuth token, or the
     * `Intent` `GoogleCalendarClient` wraps for a refused scope.
     *
     * That the binding exists at all is not a statement that the scope is granted: §2.6 says a
     * partial grant is the normal case, and every method answers `NeedsConsent` rather than
     * failing when it is not.
     */
    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: GoogleCalendarClient): CalendarRepository
}
