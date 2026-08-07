package com.idomarhaim.goalpilot.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A one-shot "can we reach the network right now?" check.
 *
 * Added for issue #3, and only after measuring: completing a task goes through
 * `TaskRepositoryImpl.setDone`, which is a **server-only** Firestore transaction.
 * Offline it cannot touch the cache, so the optimistic tick has to be taken back
 * — and on a real device that undo was measured at **7.9 seconds** after the tap,
 * because Firestore spends that long resolving DNS and retrying before it reports
 * `UNAVAILABLE`. Eight seconds of a ticked box over a write that will never land
 * is a lie, even though it eventually corrects itself. This lets the tap be
 * refused up front instead.
 *
 * It is a **fast path, not a guarantee**: this answers "is there a validated
 * network", not "did Firestore answer". A captive portal or a dead backend still
 * gets past it, which is why the undo-and-tell path stays in place behind it.
 */
interface ConnectivityMonitor {
    fun isOnline(): Boolean
}

@Singleton
class AndroidConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityMonitor {

    override fun isOnline(): Boolean {
        // Missing service: assume online rather than blocking the user. Being
        // wrong that way costs the slow undo we already handle; being wrong the
        // other way would refuse writes on a perfectly good connection.
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        // VALIDATED, not just INTERNET: a Wi-Fi network that has not passed its
        // captive-portal check advertises INTERNET and still resolves nothing.
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityMonitor(impl: AndroidConnectivityMonitor): ConnectivityMonitor
}
