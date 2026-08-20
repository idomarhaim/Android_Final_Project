package com.idomarhaim.goalpilot.notifications

import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * How this package reaches its app-scoped dependencies from the two places Hilt cannot inject
 * into by constructor: a [androidx.work.CoroutineWorker], which WorkManager instantiates, and a
 * `@Composable`, which nothing instantiates at all.
 *
 * The alternative for the worker is `androidx.hilt:hilt-work` — a second KSP processor, a
 * `Configuration.Provider` on the Application, and WorkManager's default initializer disabled —
 * bought for constructor injection into one worker. The alternative for the composable is a
 * ViewModel that holds no state and exists only to carry a `@Singleton` across the boundary.
 * One entry point replaces both.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun notifier(): GoalPilotNotifier
    fun preferences(): AppPreferencesRepository
}
