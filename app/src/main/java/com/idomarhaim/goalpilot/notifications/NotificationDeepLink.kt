package com.idomarhaim.goalpilot.notifications

import android.content.Context
import android.content.Intent
import com.idomarhaim.goalpilot.MainActivity
import com.idomarhaim.goalpilot.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * #8 piece 3 — **the tap-through destination**.
 *
 * A notification that opens the app on whatever screen it was last on has told the user
 * something happened and then made them go and find it. `R5`'s notification is about a
 * *specific* goal the sorter proposed, so its tap lands on that goal.
 *
 * **Why a process-scoped holder rather than `navDeepLink`.** Compose Navigation's own deep
 * links need a URI scheme registered in the manifest and a matching `<intent-filter>`, which
 * makes every route this app can reach externally addressable — a public surface bought for an
 * internal notification. This carries the route in an extra on an explicit [MainActivity]
 * intent instead: nothing outside the app can construct it, no route becomes web-addressable,
 * and the nav graph is untouched.
 *
 * The holder is an `object` rather than an injected singleton because its writer is
 * [MainActivity], which resolves the intent *before* any Hilt-scoped composable exists, and its
 * reader is the nav host. A `StateFlow` rather than a plain field so the reader can wait: on a
 * cold start the intent is known long before the nav controller is.
 */
object NotificationDeepLink {

    /** The extra carrying a [Routes] value on an intent built by this package. */
    const val EXTRA_ROUTE = "com.idomarhaim.goalpilot.extra.ROUTE"

    private val _pendingRoute = MutableStateFlow<String?>(null)

    /** The route a notification tap asked for, or null. Cleared by [consume]. */
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /**
     * Records the route on [intent], if it carries one.
     *
     * Called from both `onCreate` and `onNewIntent`: with `singleTop` a tap on a second
     * notification while the app is already open delivers through the latter only, and an app
     * that handled just the former would silently ignore every tap after the first.
     */
    fun offer(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE) ?: return
        if (route.isNotBlank()) _pendingRoute.value = route
    }

    /** Takes the pending route, clearing it so a configuration change cannot re-navigate. */
    fun consume(): String? = _pendingRoute.value?.also { _pendingRoute.value = null }

    /**
     * An explicit intent that opens [route] when the notification is tapped.
     *
     * `SINGLE_TOP` rather than `CLEAR_TASK`: a tap should bring the running app forward and
     * move it, not tear down a back stack the user was in the middle of.
     */
    fun intentFor(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
        }
}
