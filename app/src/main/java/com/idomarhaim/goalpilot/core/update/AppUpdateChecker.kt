package com.idomarhaim.goalpilot.core.update

import android.util.Log
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException

/**
 * The "a new version is available" prompt for builds handed out through Firebase
 * App Distribution.
 *
 * GoalPilot is sideloaded, not installed from Google Play, so it gets **no**
 * update mechanism from the platform: no auto-update, no notification, nothing.
 * This is that mechanism. [FirebaseAppDistribution.updateIfNewReleaseAvailable]
 * owns the whole flow — tester sign-in, the version comparison, the "Update"
 * dialog, the download, and handing the APK to the package installer.
 *
 * ### Why this compiles in debug builds but does nothing there
 *
 * Only the `firebase-appdistribution-api` stub is on the debug classpath; the
 * real implementation is a `releaseImplementation` (see `app/build.gradle.kts`).
 * The stub's [FirebaseAppDistribution.getInstance] returns an instance whose
 * every task fails with [FirebaseAppDistributionException] — deliberately, so a
 * developer running from source is never interrupted by an update prompt and no
 * updater code ships in a build that was never distributed. Failures are
 * therefore logged and swallowed: on debug they are the expected outcome, and on
 * release an unreachable Firebase must not block someone from using the app.
 *
 * ### Why it only fires once per process
 *
 * `MainActivity` is recreated on every configuration change — rotation, theme
 * switch, font-size change. Without the guard, rotating the phone mid-download
 * would start the flow again on top of itself.
 */
object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"

    @Volatile
    private var alreadyChecked = false

    /**
     * Asks App Distribution whether a newer release exists and, if so, walks the
     * tester through installing it. Safe to call from `Activity.onCreate`; safe
     * to call on any build variant; a no-op after the first call in a process.
     */
    fun checkOnce() {
        if (alreadyChecked) return
        alreadyChecked = true

        runCatching {
            FirebaseAppDistribution.getInstance()
                .updateIfNewReleaseAvailable()
                .addOnFailureListener { e ->
                    // Expected on debug (no implementation on the classpath) and
                    // whenever the device is offline or the user is not a tester.
                    Log.d(TAG, "No update flow ran: ${e.message}")
                }
        }.onFailure { t ->
            // getInstance() itself throws when the Firebase app is not
            // initialised — worth a louder log, still not worth a crash.
            Log.w(TAG, "App Distribution unavailable", t)
        }
    }
}
