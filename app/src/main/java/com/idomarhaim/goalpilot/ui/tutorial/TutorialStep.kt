package com.idomarhaim.goalpilot.ui.tutorial

import androidx.annotation.StringRes
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.ui.navigation.Routes

/**
 * The guided tour, as data.
 *
 * ## Why the tour is a list and not a screen
 *
 * The obvious build for "explain the app on first run" is a carousel of
 * illustrated pages before the dashboard. It is also the one every piece of
 * onboarding research argues against, and for a reason that survives taste:
 * a carousel teaches the app **while the app is not there**, so the user is
 * memorising instead of doing, and nothing they read is attached to anything
 * they can see. Progressive onboarding — a mark on the real widget, in place —
 * has the opposite property: there is nothing to remember, because the thing
 * being described is under the hole.
 *
 * So the tour is a walk over the app that already exists, and this file is the
 * route. It holds **no UI**: [TutorialOverlay] draws, [TutorialController]
 * sequences, and this decides only what is said and where.
 *
 * ## Seven steps, and why the count is a design decision
 *
 * Every study of coach marks says the same thing about length, and it is the
 * one number worth defending: attention falls off a cliff somewhere past five
 * to seven marks, and a tour that outstays it is skipped *in bulk* — the user
 * does not skip step six, they skip the whole feature and never see it again.
 * This app has enough surface to fill twenty steps and seven is what it gets.
 *
 * The cut that produced seven: **one step per thing the user cannot discover by
 * looking.** A labelled tab with an icon is discoverable, so Social gets no step
 * at all; the Calendar tab gets one anyway, because what you can *do* there —
 * drag a block to another time — is invisible from the label. Life areas,
 * challenges, analytics, Health Connect and Google Tasks import are all one tap
 * behind a step that *is* here, and each of them is a better fit for a first-use
 * tip on its own screen than for a line in a tour the user is trying to get out
 * of.
 *
 * ## What `#60` did to this file, and why the count did not move
 *
 * The tour was written on 2026-08-22 with Profile in the bottom bar. `#60`
 * shipped the next day, took Profile out of the bar (it lives behind the Home
 * avatar) and gave the freed tab to Calendar — which left step six naming a
 * destination that no longer existed, while the app's newest surface went
 * unmentioned. That step is now [CALENDAR], and Profile moved into the last
 * step, where the avatar was already being pointed at. Seven steps in, seven
 * steps out: the tour did not grow, it stopped being wrong.
 *
 * ## One required action, in the middle
 *
 * [GOALS_TAB] does not advance on Next; it advances when the user actually
 * opens the Goals tab ([action]). That is the tour's one piece of doing, and
 * its position is deliberate — early enough that a user who bails afterwards
 * has still performed the gesture, late enough that they know why they are
 * being asked. Every other step is Next, because a tour that demands six
 * gestures is a chore and gets skipped, and a skipped tour teaches nothing at
 * all.
 */
enum class TutorialStep(
    /**
     * The widget this step points at, or `null` for a step with no target —
     * rendered as a centred card with no spotlight.
     */
    val anchor: TutorialAnchor?,

    /**
     * The route this step is shown on.
     *
     * The tour **navigates there itself** for an informational step, so the
     * user is never asked to be somewhere they are not. The one exception is a
     * step with an [action]: there, arriving is the thing being taught, and
     * navigating for them would answer the question the step is asking.
     */
    val route: String,

    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,

    /** `null` — the default — means *Next* advances this step. */
    val action: TutorialAction? = null,
) {

    WELCOME(
        anchor = null,
        route = Routes.DASHBOARD,
        titleRes = R.string.tutorial_welcome_title,
        bodyRes = R.string.tutorial_welcome_body,
    ),

    PROGRESS(
        anchor = TutorialAnchor.POINTS_CARD,
        route = Routes.DASHBOARD,
        titleRes = R.string.tutorial_progress_title,
        bodyRes = R.string.tutorial_progress_body,
    ),

    QUICK_ADD(
        anchor = TutorialAnchor.QUICK_ADD,
        route = Routes.DASHBOARD,
        titleRes = R.string.tutorial_quick_add_title,
        bodyRes = R.string.tutorial_quick_add_body,
    ),

    /** The tour's one required action: the user opens the Goals tab themselves. */
    GOALS_TAB(
        anchor = TutorialAnchor.TAB_GOALS,
        route = Routes.DASHBOARD,
        titleRes = R.string.tutorial_goals_title,
        bodyRes = R.string.tutorial_goals_body,
        action = TutorialAction(
            hintRes = R.string.tutorial_goals_hint,
            completedOnRoute = Routes.GOALS,
        ),
    ),

    NEW_GOAL(
        anchor = TutorialAnchor.NEW_GOAL,
        route = Routes.GOALS,
        titleRes = R.string.tutorial_new_goal_title,
        bodyRes = R.string.tutorial_new_goal_body,
    ),

    /**
     * `#60`'s tab, and `#68`'s gesture, in one step — which is one step and not
     * two for a reason that is about the user's calendar rather than about
     * length. **A first-run calendar is empty.** A step of its own for *drag to
     * move* would spotlight a lane with nothing in it and ask for a gesture with
     * no target, on the one run of the app where that is guaranteed. So the tab
     * is what gets pointed at, and the drag is a promise about what is done
     * there once there is something to drag.
     *
     * Anchored on the Calendar **item** rather than the bar: see
     * [TutorialAnchor.TAB_CALENDAR].
     */
    CALENDAR(
        anchor = TutorialAnchor.TAB_CALENDAR,
        route = Routes.GOALS,
        titleRes = R.string.tutorial_calendar_title,
        bodyRes = R.string.tutorial_calendar_body,
    ),

    /**
     * Last, and it is the step that makes the whole feature safe to ship: it
     * tells the user where the tour lives now, so *Skip* costs nothing and the
     * replay entry in Settings is not a control nobody knows about.
     *
     * Since `#60` it carries Profile as well. That is not a second idea bolted
     * on — the avatar under the spotlight is *the* way to Profile now that the
     * tab is gone, so the step that points at it is the only place the two facts
     * can honestly be said together.
     */
    WHERE_SETTINGS(
        anchor = TutorialAnchor.AVATAR,
        route = Routes.DASHBOARD,
        titleRes = R.string.tutorial_settings_step_title,
        bodyRes = R.string.tutorial_settings_step_body,
    ),
    ;

    val isFirst: Boolean get() = ordinal == 0
    val isLast: Boolean get() = ordinal == entries.lastIndex

    /** 1-based, for the *Step 3 of 7* counter — humans do not count from zero. */
    val displayNumber: Int get() = ordinal + 1

    companion object {
        val count: Int get() = entries.size
    }
}

/**
 * A step the user has to *do* rather than read.
 *
 * ### Completion is a route, not a callback
 *
 * The alternative is for the tagged widget to tell the tour it was tapped — a
 * lambda threaded from the nav bar into the controller. That version is broken
 * in a way that only shows up in use: it fires on the **tap**, and a tap that
 * is swallowed (a disabled control, a nav call the graph refuses, a double tap
 * during a transition) would advance the tour past a thing that did not happen.
 * Watching the route watches the **outcome**, so the tour advances exactly when
 * the user is looking at the screen the step promised them — including when
 * they get there by some other means, which is a user being ahead of the tour
 * rather than a user cheating it.
 *
 * @param hintRes the imperative — *Tap Goals to continue*. Separate from the
 *   step's body because it must survive the body being skimmed.
 * @param completedOnRoute the route whose arrival advances the step.
 */
data class TutorialAction(
    @StringRes val hintRes: Int,
    val completedOnRoute: String,
)

/**
 * Bumped when the tour changes enough that somebody who has seen the old one
 * should be shown the new one.
 *
 * ### Why a version and not a boolean
 *
 * A `hasSeenTutorial` flag answers *this install has run the tour* and can
 * never answer *this install has run **this** tour*. The day a step is added
 * for a feature that did not exist, every existing user is the one group that
 * has not seen it and the flag says they have. Storing the version costs the
 * same four bytes and makes that a one-line change here — which is the whole
 * reason the preference is an `Int` rather than a `Boolean`
 * ([com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository.tutorialSeenVersion]).
 *
 * ⚠️ **Bumping this re-runs the tour for everybody**, so bump it for a genuinely
 * new step, never for a typo in one.
 *
 * `1` → `2` on 2026-08-24: [CALENDAR] replaced a step that named a tab `#60`
 * had removed. Both halves earn the re-run on their own — a surface nobody was
 * told about, and a sentence that was false — and this is exactly the case the
 * paragraph above describes, where a `hasSeenTutorial` boolean would have said
 * *yes* for every existing install.
 */
const val TUTORIAL_VERSION = 2
