package com.idomarhaim.goalpilot.core.util

import javax.inject.Qualifier

/** Marks the injected [kotlinx.coroutines.CoroutineDispatcher] as IO-bound. */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

/** Marks the injected [kotlinx.coroutines.CoroutineDispatcher] as the default (CPU) dispatcher. */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher
