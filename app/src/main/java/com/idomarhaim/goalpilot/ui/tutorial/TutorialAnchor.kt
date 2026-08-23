package com.idomarhaim.goalpilot.ui.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.flow.filter

/**
 * The elements the guided tour can point at.
 *
 * ## Why the tour names elements instead of holding them
 *
 * A coach mark has to draw a hole in a full-screen scrim exactly where a real
 * widget is, and the widget is six layers down someone else's screen. The two
 * ways to bridge that are to **move the widget into the overlay** (draw a copy
 * of the nav bar inside the tutorial) or to **let the widget report where it
 * is**. This package takes the second, and the reason is the first one's
 * failure mode: a copy drifts. It renders the nav bar as it looked the day the
 * tour was written, keeps rendering it after the real one changes, and nothing
 * fails — the tour simply starts lying, in a way only a person looking at both
 * can see.
 *
 * So an anchor is an **identifier**, the screens tag their own widgets with it
 * ([Modifier.tutorialAnchor]), and the overlay reads coordinates. The tour can
 * only point at something that is genuinely on screen, which is the invariant
 * worth having.
 *
 * ## The enum is the contract, and it is deliberately small
 *
 * Six anchors for seven steps — the welcome step points at nothing, because
 * there is nothing to point at until the user has been told what the app is.
 * Every entry here is a promise that some screen tags that widget, and
 * `TutorialStepsTest` holds the other half: no anchor may go untagged, and no
 * step may name one that no source file applies.
 */
enum class TutorialAnchor {

    /** Dashboard's hero card — points, level and the progress bar (`PointsLevelCard`). */
    POINTS_CARD,

    /** Dashboard's smart-add card: type a task, the model files it (`SmartAddCard`). */
    QUICK_ADD,

    /** Dashboard's top-bar avatar, which opens Profile and Settings. */
    AVATAR,

    /** The **Goals** item in the bottom navigation bar, on its own. */
    TAB_GOALS,

    /**
     * The **Calendar** item in the bottom navigation bar, on its own.
     *
     * There was a `NAV_BAR` here until `#60`, spotlighting the whole bar for a
     * step that named *Social and Profile*. That step could not survive the tab
     * swap: Profile left the bar for the avatar and Calendar took its place, so
     * half of what the hole contained was no longer in it. Pointing at one item
     * is also the honest shape — a hole around four tabs says *look at these*
     * and the card then has to say which.
     */
    TAB_CALENDAR,

    /** The Goals screen's extended FAB — *New goal*. */
    NEW_GOAL,
}

/**
 * Where each tagged widget currently is, in the coordinate space of the
 * composition root.
 *
 * ## Root coordinates, and why that is the only workable space
 *
 * [Modifier.tutorialAnchor] writes `positionInRoot()`, and the overlay is a
 * sibling of the whole scaffold inside the *same* root. Both therefore measure
 * from the same origin, and nothing in between — a `Scaffold`'s inset padding,
 * a `LazyColumn`'s scroll offset, the nav bar's own placement — has to be known
 * by either side. Window coordinates would work equally well right up until the
 * app is rendered in a multi-window or picture-in-picture frame, where the
 * window origin is not the app's; local coordinates would not work at all.
 *
 * ## `positionInRoot()` + `size`, NOT `boundsInRoot()`
 *
 * `boundsInRoot()` is **clipped by every parent**, so a card scrolled half out
 * of a `LazyColumn` reports half of itself and a spotlight drawn from it cuts
 * the widget in two — while looking, on the frame you happen to screenshot,
 * like a slightly-off cutout rather than like a bug. The unclipped pair keeps
 * the hole the shape of the widget; a widget scrolled fully away is handled one
 * layer up, where a target outside the visible window falls back to a centred
 * card ([TutorialOverlay]).
 *
 * ## Not saved across process death, deliberately
 *
 * These are coordinates of live layout nodes. Restoring stale ones would draw
 * the first frame's hole in last week's position; the tour's own progress *is*
 * saved, and re-measuring costs one frame.
 */
@Stable
class TutorialAnchors {

    private val bounds = mutableStateMapOf<TutorialAnchor, Rect>()

    /**
     * The anchor the tour is currently pointing at, or `null`.
     *
     * Written by the overlay and read by [Modifier.tutorialAnchor], which is the
     * wrong direction for a registry — and it is what lets a tagged widget
     * **scroll itself into view** when its step arrives. The alternative is for
     * the overlay to reach into someone else's `LazyListState`, which means the
     * tour would have to know the index of every anchored item on every screen.
     */
    var activeAnchor: TutorialAnchor? by mutableStateOf(null)
        internal set

    operator fun get(anchor: TutorialAnchor): Rect? = bounds[anchor]

    fun put(anchor: TutorialAnchor, rect: Rect) {
        // Guarded because `onGloballyPositioned` fires on every frame a parent
        // scrolls, and an unconditional write into snapshot state would
        // recompose the overlay on each one even when nothing moved.
        if (bounds[anchor] != rect) bounds[anchor] = rect
    }

    fun remove(anchor: TutorialAnchor) {
        bounds.remove(anchor)
    }

    /** Test seam: how many anchors are currently on screen. */
    val size: Int get() = bounds.size
}

/**
 * The registry the current subtree reports into, or `null` — the default —
 * meaning **no tour is possible here**, which is every preview, every test that
 * has not opted in, and the signed-out graph.
 *
 * Static because one registry is provided per app scaffold and never swapped.
 */
val LocalTutorialAnchors = staticCompositionLocalOf<TutorialAnchors?> { null }

/**
 * Report this widget's position to the guided tour, and scroll it into view when
 * its step arrives.
 *
 * A no-op with no registry in scope, so a `@Preview` of a tagged screen composes
 * exactly as it did before.
 *
 * ### The `bringIntoView` half is not a nicety
 *
 * Two of the six anchors sit inside a `LazyColumn`. On a short phone the
 * smart-add card is below the fold, and a tour that spotlights a widget the user
 * cannot see draws its hole off the bottom edge of the screen — the overlay's
 * own fallback then hides the spotlight entirely and the step reads as a
 * paragraph about nothing. `BringIntoViewRequester` asks whatever scrollable
 * parent exists to reveal this node, without the tour knowing there is one; with
 * no scrollable parent it does nothing, which is the correct answer for the nav
 * bar and the FAB.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tutorialAnchor(anchor: TutorialAnchor): Modifier {
    val registry = LocalTutorialAnchors.current ?: return this
    val requester = remember { BringIntoViewRequester() }

    DisposableEffect(registry, anchor) {
        onDispose { registry.remove(anchor) }
    }

    // `snapshotFlow` rather than reading `registry.activeAnchor` in composition:
    // reading it here would subscribe EVERY tagged widget in the tree to it, so
    // each step change would recompose all six. Collected in an effect, the
    // step change reaches only the two anchors whose scroll actually matters.
    LaunchedEffect(registry, anchor) {
        snapshotFlow { registry.activeAnchor }
            .filter { it == anchor }
            .collect {
                // A node detached between the step arriving and this running
                // throws rather than returning; the tour survives it as "no
                // scroll happened", which is what a disposed anchor deserves.
                runCatching { requester.bringIntoView() }
            }
    }

    return this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coordinates ->
            val origin = coordinates.positionInRoot()
            registry.put(
                anchor,
                Rect(
                    left = origin.x,
                    top = origin.y,
                    right = origin.x + coordinates.size.width,
                    bottom = origin.y + coordinates.size.height,
                ),
            )
        }
}
