package com.idomarhaim.goalpilot.notifications

import android.os.Build

/**
 * #8 piece 2 — **when** to ask for `POST_NOTIFICATIONS`, as a pure decision.
 *
 * ## The decision, and why
 *
 * **Ask at the first filing outcome that speaks, never at launch.** §3.4 has exactly two rows
 * that say anything (`FilingDecision.speaks` — `NewGoal` and `NoGoal`), and that moment is the
 * first time this app has ever had something to tell the user when they are not looking at it.
 * Asking before then is asking for a capability with nothing to spend it on.
 *
 * Three reasons, in order of how much they cost if ignored:
 *
 *  1. **A denial is close to permanent, and a launch-time prompt is the one people deny.**
 *     From Android 13 the system stops showing the dialog after two dismissals — the app can
 *     still call `requestPermission`, the callback just returns `false` with nothing rendered,
 *     and the only remaining route is the system settings screen. So the first ask is very
 *     nearly the only ask, and spending it on a cold launch — before the user has seen the app
 *     do anything — spends it at its weakest.
 *  2. **In context, the question answers itself.** The user has just quick-added a task and
 *     watched a snackbar say no goal fitted. *"Tell you about this next time?"* is legible at
 *     that instant and abstract at launch.
 *  3. **§0.4 — speak about a failure the user can act on.** The permission dialog is itself an
 *     interruption, and this ruleset spends interruptions on things the user can do something
 *     about. Being asked to allow notifications while looking at a sign-in screen is not one.
 *
 * ## What is deliberately *not* stored
 *
 * There is no *"we already asked"* preference, and that is a choice rather than an omission.
 * Android's own API cannot distinguish **never asked** from **permanently denied** —
 * `shouldShowRequestPermissionRationale` is `false` for both — so a stored flag is the only way
 * to tell them apart, and it buys nothing here: a permanently-denied request renders no dialog,
 * so re-asking on a later cold start is invisible to the user. Paying a persisted field, its
 * migration and its capacity to disagree with the device (§0.2) to avoid an invisible no-op is
 * the wrong trade. [askedThisProcess] is an in-memory guard, and its only job is to stop the
 * same launch firing twice while one dialog is up.
 */
object NotificationPermissionPolicy {

    /**
     * What to do about `POST_NOTIFICATIONS` right now.
     *
     * [sdkInt] is a parameter rather than a read of [Build.VERSION.SDK_INT] so this is testable
     * on the JVM across the API-32/33 boundary, which is the whole reason the function exists.
     */
    fun decide(
        sdkInt: Int,
        granted: Boolean,
        askedThisProcess: Boolean,
        eventSpeaks: Boolean,
    ): PermissionStep = when {
        // Below API 33 the permission does not exist and notifications are allowed unless the
        // user turned them off in settings -- which is not something an app may ask about.
        sdkInt < Build.VERSION_CODES.TIRAMISU -> PermissionStep.NOT_APPLICABLE
        granted -> PermissionStep.ALREADY_GRANTED
        !eventSpeaks -> PermissionStep.WAIT_FOR_A_REASON
        askedThisProcess -> PermissionStep.ALREADY_ASKED
        else -> PermissionStep.ASK_NOW
    }
}

/** The four states [NotificationPermissionPolicy.decide] can land in. */
enum class PermissionStep {
    /** API < 33: nothing to request, and posting is allowed. */
    NOT_APPLICABLE,

    /** The user has allowed it. */
    ALREADY_GRANTED,

    /** Not granted, but nothing has happened that would justify interrupting. */
    WAIT_FOR_A_REASON,

    /** Asked once already in this process; do not stack a second dialog. */
    ALREADY_ASKED,

    /** Not granted, and something worth being told about just happened. */
    ASK_NOW;

    /** Whether a notification posted right now could actually be seen. */
    val canPost: Boolean get() = this == NOT_APPLICABLE || this == ALREADY_GRANTED
}
