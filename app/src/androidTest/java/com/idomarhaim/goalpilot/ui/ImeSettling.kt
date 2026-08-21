package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement

/**
 * **Type into a field, then wait for the soft keyboard to stop moving the layout.**
 *
 * Use this everywhere instead of [performTextInput] — and its siblings
 * [performTextReplacementAndSettle] and [performTextClearanceAndSettle] instead
 * of theirs. `ImeSettleSweepTest` (JVM layer) fails the build if any raw member
 * of that family reappears anywhere in `androidTest/`, because the landmine
 * below is invisible at the call site: the test reads perfectly, passes on its
 * own, and fails intermittently inside the full suite. `Observed:` 2026-08-21 on
 * `emulator-5554` — **1 full-suite run in 4**, and **4 cycles in 18** of a
 * two-class harness that ran just the boundary where it bites.
 *
 * ### The defect this exists to make unrepresentable — issue #58
 *
 * `performTextInput` focuses the field, and focusing a text field **raises the
 * soft keyboard**. On Android 11+ the keyboard arrives as a *window inset
 * animation*: over the next few hundred milliseconds the system resizes the
 * window and everything inside it slides upward. A `ModalBottomSheet` moves a
 * long way. `Observed:` on `emulator-5554`, API 35, the semantics tree captured
 * at the moment of failure has the AI editor sheet's content box at `0..1984px`
 * inside a `2992px` window — a bottom sheet sitting nowhere near the bottom,
 * because the keyboard is holding the lower third.
 *
 * **Compose's idling resource cannot see that animation.** It tracks
 * recompositions, its own frame clock, and pending measure/layout passes; a
 * system inset animation is none of those. So `waitForIdle()` returns *true*
 * while the layout is still travelling, and the next line —
 *
 * ```kotlin
 * composeRule.onNodeWithTag(TAG_AI_KEY_FIELD).performTextInput(key)
 * composeRule.onNodeWithTag(TAG_AI_SAVE).performClick()   // ← lands on nothing
 * ```
 *
 * — reads the Save button's bounds, injects a touch at their centre, and by the
 * time the event is dispatched the button has moved out from under it. The click
 * is **silently lost**: no exception, no failed assertion at that line. The test
 * dies later, somewhere harmless-looking, because the save never happened.
 *
 * ### Why the failure looked like two unrelated bugs
 *
 * The two shapes seen on `#58` are both this one click:
 *
 * * `replacingTheKeyChangesTheMaskedTail` — the sheet had an existing credential,
 *   so Save was enabled and *would* have fired; the click missed, so the mask
 *   still ended `9876` instead of `1111`.
 * * `aBlankModelRendersTheProvidersDefaultRatherThanNothing` — no credential, so
 *   the missed click left the sheet open and `settings_ai_model` never rendered:
 *   *"Expected exactly '1' node but could not find any"*.
 *
 * `Observed:` 2026-08-21, session `58-instrumented-order`. The semantics tree
 * dumped at the moment of failure showed the key field holding all 28 typed
 * characters and Save carrying an enabled `OnClick` — so the text landed and the
 * button was live. The click simply did not reach it.
 *
 * ### Why the two failing tests were always the two failing tests
 *
 * JUnit's default method order is by `String.hashCode()`, so it is fixed per
 * build. Those two are positions **1 and 2** in `AiSectionUiTest` — the tests
 * that run immediately after the *previous class* tore its activity down, when
 * the IME's state is whatever the last test left. So this half of `#58`'s
 * "order-dependence" is not a dependency between tests at all — it is a
 * dependency on how far the keyboard had got. (`#58` had a second, unrelated
 * cause in `NotificationObservedFireTest`, which `#56` had already fixed by
 * adding a bounded wait for the shade to reflect a post.)
 *
 * ### Why not just disable the emulator's keyboard
 *
 * `#58` offered that as its option 3. `Untested:` it was never tried here, so
 * whether it works is unmeasured — but it is a device setting that persists on the
 * AVD and silently changes the ground under every other session sharing it, which
 * is reason enough not to reach for it first. This waits instead, so it needs
 * nothing from the device: it holds on CI, and it holds for a human running
 * `adb shell am instrument` by hand.
 */
fun SemanticsNodeInteraction.performTextInputAndSettle(text: String) {
    performTextInput(text)
    settleAfterImeChange()
}

/**
 * [performTextReplacement], then the same wait. See [performTextInputAndSettle]
 * — this is the identical landmine: replacing a field's text focuses it first,
 * and focusing is what raises the keyboard.
 */
fun SemanticsNodeInteraction.performTextReplacementAndSettle(text: String) {
    performTextReplacement(text)
    settleAfterImeChange()
}

/**
 * [performTextClearance], then the same wait. Clearing focuses the field too, so
 * it raises the keyboard exactly like typing does — and, being the call that
 * *looks* like it is taking something away, it is the one where a reader is least
 * likely to expect a keyboard.
 */
fun SemanticsNodeInteraction.performTextClearanceAndSettle() {
    performTextClearance()
    settleAfterImeChange()
}

/**
 * Blocks until this node's bounds have been identical across [STABLE_SAMPLES]
 * consecutive reads, or [SETTLE_TIMEOUT_MS] has passed.
 *
 * `boundsInRoot` is deliberately the quantity sampled: it is exactly what
 * `performTouchInput` converts into the coordinates it injects at, so "stable"
 * here means "a click computed now will still be aimed correctly when it
 * arrives". `fetchSemanticsNode` calls `waitForIdle()` on the way, so each
 * sample also drains everything Compose *can* see.
 *
 * **Bounded on purpose.** A layout that genuinely never settles — an infinite
 * animation — must fail the assertion that follows, not hang the suite until the
 * instrumentation is killed. Returning quietly on timeout leaves the original
 * assertion as the thing that reports the problem.
 */
private fun SemanticsNodeInteraction.settleAfterImeChange() {
    var previous: Rect? = null
    var stable = 0
    val deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MS

    while (System.currentTimeMillis() < deadline) {
        val bounds = runCatching { fetchSemanticsNode().boundsInRoot }.getOrNull()
            // The node can legitimately disappear — an ImeAction that submits and
            // closes a sheet. Nothing is moving that this caller can wait for.
            ?: return

        if (bounds == previous) {
            if (++stable >= STABLE_SAMPLES) return
        } else {
            previous = bounds
            stable = 0
        }
        Thread.sleep(SAMPLE_INTERVAL_MS)
    }
}

/**
 * Two frames at 60 Hz. Shorter than the inset animation by an order of
 * magnitude, so a moving layout cannot alias into looking stable.
 */
private const val SAMPLE_INTERVAL_MS = 32L

/**
 * Three identical reads, ~96 ms apart end to end. Two would be satisfied by the
 * pause at the top of an animation that has not started yet.
 */
private const val STABLE_SAMPLES = 3

/**
 * Generous by design: it costs nothing on the runs that settle at once, and the
 * runs that do not are exactly the ones worth waiting for. `Inferred:` the
 * platform's own IME show animation is a few hundred milliseconds; this was not
 * measured here, and the bound is set well above any plausible value rather than
 * tuned to one.
 */
private const val SETTLE_TIMEOUT_MS = 2_000L
