package com.idomarhaim.goalpilot.core.result

/**
 * A lightweight wrapper describing the state of an asynchronous value that can
 * be loading, successful, or failed. Used by repositories and ViewModels to
 * carry results together with error information across layers.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>
}

/** Maps the success payload while preserving Loading / Error states. */
inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> this
    Resource.Loading -> Resource.Loading
}

/** Returns the payload if this is [Resource.Success], otherwise null. */
fun <T> Resource<T>.getOrNull(): T? = (this as? Resource.Success)?.data
