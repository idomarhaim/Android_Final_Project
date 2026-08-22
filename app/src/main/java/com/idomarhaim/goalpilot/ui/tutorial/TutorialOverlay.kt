package com.idomarhaim.goalpilot.ui.tutorial

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.ui.components.GpSpring
import com.idomarhaim.goalpilot.ui.components.gpCardContainerColor
import kotlin.math.roundToInt

/**
 * The tour, drawn: a dimmed app with a hole cut in it, and a card beside the
 * hole saying what is under it.
 *
 * ## It is a sibling of the app, not a window over it
 *
 * The instinctive build is a `Dialog` or a `Popup`, and both are wrong here for
 * the same reason `DialogLocaleGuardTest` bans them outright: a new window
 * re-derives `LocalContext` from itself, so everything inside it renders in the
 * **device** language while mirroring right-to-left perfectly — a defect that
 * looks more finished than a half-done job. This overlay is an ordinary
 * composable inside the app's own composition, so `AppLocale` reaches it
 * untouched and there is no window boundary to lose anything at.
 *
 * It buys a second thing a window could not give at any price: the hole has to
 * sit exactly over a widget six layers down a *different* screen, and both sides
 * only agree on where that is because they share one composition root. A popup's
 * coordinates are its own.
 *
 * ## The cutout is a real hole, not a ring drawn around the widget
 *
 * `BlendMode.Clear` needs somewhere to clear *to*, so the scrim is composited
 * offscreen ([CompositingStrategy.Offscreen]) and the hole is punched out of
 * that layer. Four opaque rectangles arranged around the target would look
 * identical on a screenshot and behave differently the moment the target has
 * rounded corners or moves — which it does, on every step change.
 *
 * ## Where the card goes
 *
 * Below the spotlight when there is room, above it when there is not, centred
 * when there is neither — decided inside a single [Layout] pass, because the
 * decision needs the card's measured height and a two-pass version would place
 * the first frame wrong and correct it on the second. That flash is exactly the
 * kind of thing that reads as *this app is cheap*.
 *
 * ⚠️ **No caret/beak pointing at the target, deliberately.** The card is clamped
 * to the screen's margins, so a caret drawn at the card's own centre points at
 * nothing whenever the target is near an edge — and near an edge is where the
 * nav bar and the FAB always are. The pulsing ring around the hole does that job
 * without being able to lie about it.
 */
@Composable
fun TutorialOverlay(
    step: TutorialStep,
    pendingAction: TutorialAction?,
    target: Rect?,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = rememberTutorialMotion()

    Layout(
        modifier = modifier.fillMaxSize().testTag(TAG_TUTORIAL_OVERLAY),
        content = {
            // Slot 0 — the scrim, the hole and the ring. Takes no pointer input
            // of its own; the blockers do that, so an action step can leave one
            // rectangle of the screen live.
            TutorialScrim(target = target, motion = motion)

            // Slot 1 — the touch blockers. Also full-size; what changes between
            // steps is how many holes they leave.
            TutorialBlockers(
                target = target,
                // An informational step is dismissed by tapping anywhere, which
                // is the gesture people try first. A step with an action must
                // NOT be, or the one thing the tour asks for can be skipped by a
                // tap meant for the app underneath.
                onTapThrough = if (pendingAction == null) onNext else null,
            )

            // Slot 2 — the card, placed last so it is on top of both and its
            // buttons win every tap.
            TutorialCard(
                step = step,
                pendingAction = pendingAction,
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip,
                onCreateGoal = onCreateGoal,
            )
        },
    ) { measurables, constraints ->
        val full = Constraints.fixed(constraints.maxWidth, constraints.maxHeight)
        val scrim = measurables[0].measure(full)
        val blockers = measurables[1].measure(full)

        val margin = CARD_MARGIN.roundToPx()
        val gap = CARD_GAP.roundToPx()
        val cardMaxWidth = minOf(constraints.maxWidth - margin * 2, CARD_MAX_WIDTH.roundToPx())
        val card = measurables[2].measure(
            Constraints(
                minWidth = 0,
                maxWidth = cardMaxWidth.coerceAtLeast(0),
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ),
        )

        val placement = placeCard(
            target = target,
            cardWidth = card.width,
            cardHeight = card.height,
            screenWidth = constraints.maxWidth,
            screenHeight = constraints.maxHeight,
            margin = margin,
            gap = gap,
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            scrim.place(0, 0)
            blockers.place(0, 0)
            card.place(placement.x, placement.y)
        }
    }
}

/**
 * Where the card lands, in pixels.
 *
 * Pure arithmetic and `internal`, so `TutorialPlacementTest` can drive it on the
 * JVM with no device — the three cases that matter (below, above, neither) are
 * decided by numbers, and the only honest way to check them is to run the
 * numbers rather than to look at one phone and infer the other two.
 */
internal fun placeCard(
    target: Rect?,
    cardWidth: Int,
    cardHeight: Int,
    screenWidth: Int,
    screenHeight: Int,
    margin: Int,
    gap: Int,
): CardPlacement {
    val centreX = ((screenWidth - cardWidth) / 2).coerceAtLeast(margin)
    val centreY = ((screenHeight - cardHeight) / 2).coerceAtLeast(margin)

    if (target == null) return CardPlacement(x = centreX, y = centreY)

    val below = target.bottom.roundToInt() + gap
    val above = target.top.roundToInt() - gap - cardHeight

    val y = when {
        below + cardHeight <= screenHeight - margin -> below
        above >= margin -> above
        // The target fills the screen, or nearly. Centring over it is the least
        // bad answer: the ring still says which thing is being described, and
        // the alternative — clamping to an edge — hides the ring under the card.
        else -> centreY
    }

    val alignedToTarget = (target.center.x - cardWidth / 2f).roundToInt()
    val x = alignedToTarget.coerceIn(margin, (screenWidth - cardWidth - margin).coerceAtLeast(margin))

    return CardPlacement(x = x, y = y)
}

internal data class CardPlacement(val x: Int, val y: Int)

/**
 * The dim, the hole, and the ring that pulses around it.
 *
 * The hole travels between steps rather than jumping, because a jump gives the
 * eye nothing to follow and the user has to re-find the target every time. It
 * **snaps** on first appearance, though — animating in from `Rect.Zero` would
 * open the tour with a hole sliding out of the top-left corner, which reads as a
 * glitch rather than as motion.
 */
@Composable
private fun TutorialScrim(target: Rect?, motion: Boolean) {
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val ringColor = MaterialTheme.colorScheme.primary

    // The last place the hole was, so a step with no target has something to
    // hold position at and the step after it has somewhere to travel FROM.
    // Written in a SideEffect, never during composition: a composition that
    // writes the state it reads is the classic non-converging recomposition.
    var previousTarget by remember { mutableStateOf<Rect?>(null) }
    SideEffect { if (target != null) previousTarget = target }

    val geometry = target ?: previousTarget ?: Rect.Zero
    val spec: AnimationSpec<Float> =
        if (previousTarget != null && motion) tween(SPOTLIGHT_TRAVEL_MS, easing = GpSpring) else snap()

    val left by animateFloatAsState(geometry.left, spec, label = "spotlightLeft")
    val top by animateFloatAsState(geometry.top, spec, label = "spotlightTop")
    val right by animateFloatAsState(geometry.right, spec, label = "spotlightRight")
    val bottom by animateFloatAsState(geometry.bottom, spec, label = "spotlightBottom")

    // A breathing ring, not a flashing one: `Reverse` on a slow tween reads as
    // attention rather than as an alert, and an alert is the wrong register for
    // something the user asked to see. Off entirely when the system's animator
    // scale is zero — the accessibility remove-animations toggle sets it there.
    val pulse = if (motion) {
        val transition = rememberInfiniteTransition(label = "spotlightPulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(PULSE_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "spotlightPulseValue",
        ).value
    } else {
        1f
    }

    val density = LocalDensity.current
    val padding = with(density) { SPOTLIGHT_PADDING.toPx() }
    val corner = with(density) { SPOTLIGHT_CORNER.toPx() }
    val ringWidth = with(density) { RING_WIDTH.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Offscreen, not the default: `BlendMode.Clear` has nothing to clear
            // unless this subtree is composited into its own layer first.
            // Without it the "hole" draws as an opaque black rectangle — a
            // spotlight with its polarity inverted, which looks deliberate.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                drawRect(color = scrimColor)
                if (target == null) return@drawBehind

                val hole = Rect(left, top, right, bottom).inflate(padding)
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(hole.left, hole.top),
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(corner, corner),
                    blendMode = BlendMode.Clear,
                )

                // After the clear, so the ring survives the hole it rings.
                val glow = hole.inflate(RING_SPREAD_PX * pulse)
                drawRoundRect(
                    color = ringColor.copy(alpha = RING_ALPHA_MAX - RING_ALPHA_SWING * pulse),
                    topLeft = Offset(glow.left, glow.top),
                    size = Size(glow.width, glow.height),
                    cornerRadius = CornerRadius(corner + RING_SPREAD_PX, corner + RING_SPREAD_PX),
                    style = Stroke(width = ringWidth),
                )
            },
    )
}

/**
 * The part of the screen the user may not touch while a step is showing.
 *
 * ## Four rectangles, not one blocker that "lets taps through"
 *
 * Compose delivers a pointer event to every node under it and lets consumption
 * decide who acts, so a full-screen handler that declines to consume inside the
 * hole *does* let the widget beneath fire — and that is exactly why it is not
 * used here. It makes the tour's most important behaviour depend on a
 * consumption subtlety no reader of this file can verify by looking at it, and
 * that any `clickable` added anywhere in the stack can break silently. Four
 * rectangles that simply are not there over the hole cannot be got wrong: the
 * live region is live because nothing is on top of it.
 *
 * @param onTapThrough what a tap on the blocked area does — advance, on an
 *   informational step, or nothing at all when the step is waiting for the user
 *   to tap the target itself. `null` still **blocks**; it only declines to act.
 */
@Composable
private fun TutorialBlockers(target: Rect?, onTapThrough: (() -> Unit)?) {
    val padding = with(LocalDensity.current) { SPOTLIGHT_PADDING.toPx() }

    // The hole stays blocked on an informational step: there, tapping anywhere
    // is *the* dismiss gesture, and leaving one live rectangle in the middle of
    // it would make part of the screen do something else for no visible reason.
    val hole = if (onTapThrough == null) target?.inflate(padding) else null

    if (hole == null) {
        Box(Modifier.fillMaxSize().blockTaps(onTapThrough))
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val left = hole.left.coerceIn(0f, width)
        val top = hole.top.coerceIn(0f, height)
        val right = hole.right.coerceIn(0f, width)
        val bottom = hole.bottom.coerceIn(0f, height)

        BlockedRect(0f, 0f, width, top)
        BlockedRect(0f, bottom, width, height - bottom)
        BlockedRect(0f, top, left, bottom - top)
        BlockedRect(right, top, width - right, bottom - top)
    }
}

/** One band of blocked screen. Degenerate bands are not emitted at all. */
@Composable
private fun BlockedRect(x: Float, y: Float, width: Float, height: Float) {
    if (width <= 0f || height <= 0f) return
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() },
            )
            .blockTaps(null),
    )
}

/**
 * Swallow taps, with no ripple.
 *
 * `indication = null` because a ripple on a transparent full-screen box is a
 * grey wash appearing under the user's thumb from nowhere; the interaction
 * source is required all the same, and remembering one is what keeps it from
 * allocating on every frame the spotlight moves.
 */
@Composable
private fun Modifier.blockTaps(onTap: (() -> Unit)?): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = { onTap?.invoke() },
    )
}

/** The coach mark itself: counter, progress dots, title, body, and the controls. */
@Composable
private fun TutorialCard(
    step: TutorialStep,
    pendingAction: TutorialAction?,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onCreateGoal: () -> Unit,
) {
    // The live region reads THIS node's own description, so it has to carry the
    // step's words — a description holding only the boilerplate would announce
    // "guided tour, skip it at any time" on every step change and never say what
    // the step is. The children keep their own semantics and stay individually
    // focusable, which is why this is a description and not `clearAndSetSemantics`.
    //
    // `Inferred:` from how TalkBack sources a live-region announcement; no
    // screen-reader pass has been run on this overlay. `Untested:` the
    // announcement itself. What is certain either way is that the words a blind
    // user needs are now on the node rather than absent from it.
    val stepDescription = listOfNotNull(
        stringResource(R.string.tutorial_step_counter, step.displayNumber, TutorialStep.count),
        stringResource(step.titleRes),
        stringResource(step.bodyRes),
        pendingAction?.let { stringResource(it.hintRes) },
        stringResource(R.string.tutorial_overlay_description),
    ).joinToString(separator = ". ")

    Surface(
        shape = RoundedCornerShape(CARD_CORNER),
        color = gpCardContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = CARD_ELEVATION,
        modifier = Modifier
            .testTag(TAG_TUTORIAL_CARD)
            .semantics {
                // Assertive, not Polite: the step has just replaced the previous
                // one and the previous one is gone. A polite announcement queues
                // behind whatever the screen underneath is saying and arrives
                // after the user has already been told to tap something.
                liveRegion = LiveRegionMode.Assertive
                contentDescription = stepDescription
            },
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.tutorial_step_counter,
                        step.displayNumber,
                        TutorialStep.count,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TAG_TUTORIAL_COUNTER),
                )
                Spacer(Modifier.width(10.dp))
                TutorialDots(current = step.ordinal)
            }

            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = stringResource(step.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            pendingAction?.let { action ->
                // The imperative gets its own line, its own colour and an arrow,
                // because it is the one sentence on the card that is an
                // instruction rather than an explanation.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(action.hintRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag(TAG_TUTORIAL_HINT),
                    )
                }
            }

            if (step.isLast) {
                TextButton(
                    onClick = onCreateGoal,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag(TAG_TUTORIAL_CREATE_GOAL),
                ) {
                    Text(stringResource(R.string.tutorial_finish_create_goal))
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Skip stays on every step, low-emphasis and first. It is the
                // promise that makes the whole overlay acceptable: an
                // interruption you cannot end is an interruption you resent.
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag(TAG_TUTORIAL_SKIP),
                ) {
                    Text(stringResource(R.string.tutorial_skip))
                }

                Spacer(Modifier.weight(1f))

                if (!step.isFirst) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(TAG_TUTORIAL_BACK),
                    ) {
                        Text(stringResource(R.string.tutorial_back))
                    }
                }
                // A step waiting on the user has no Next — offering one would
                // make the instruction optional, and an optional instruction is
                // an instruction nobody follows.
                if (pendingAction == null) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.testTag(TAG_TUTORIAL_NEXT),
                    ) {
                        Text(
                            stringResource(
                                when {
                                    step.isLast -> R.string.tutorial_done
                                    step.isFirst -> R.string.tutorial_start
                                    else -> R.string.tutorial_next
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The progress dots.
 *
 * Redundant with the *Step 3 of 7* counter beside them, and kept anyway: the
 * number answers *where am I* and the dots answer *how much is left*, and the
 * second question is the one that decides whether somebody skips. They carry no
 * semantics of their own — TalkBack already has the counter, in words.
 */
@Composable
private fun TutorialDots(current: Int) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(TutorialStep.count) { index ->
            Box(
                Modifier
                    .size(if (index == current) DOT_ACTIVE else DOT_IDLE)
                    .background(if (index == current) active else idle, CircleShape),
            )
        }
    }
}

/**
 * Whether this overlay may animate.
 *
 * The same `ANIMATOR_DURATION_SCALE` read as
 * [com.idomarhaim.goalpilot.ui.components.rememberGpEntrance], and deliberately
 * not a call to it: that function returns a `GpEntrance`, whose whole job is
 * handing staggered slots to a column of cards, and there is no column here.
 * Borrowing the type to read one boolean off it would tie this file to a
 * stagger it does not use.
 */
@Composable
private fun rememberTutorialMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

const val TAG_TUTORIAL_OVERLAY = "tutorial_overlay"
const val TAG_TUTORIAL_CARD = "tutorial_card"
const val TAG_TUTORIAL_COUNTER = "tutorial_counter"
const val TAG_TUTORIAL_SKIP = "tutorial_skip"
const val TAG_TUTORIAL_NEXT = "tutorial_next"
const val TAG_TUTORIAL_BACK = "tutorial_back"
const val TAG_TUTORIAL_HINT = "tutorial_hint"
const val TAG_TUTORIAL_CREATE_GOAL = "tutorial_create_goal"

/** How dark the app goes behind the mark: dark enough to read the card, light enough to recognise the screen. */
private const val SCRIM_ALPHA = 0.72f

/** Long enough that the eye can follow the hole between two steps; short enough not to be a wait. */
private const val SPOTLIGHT_TRAVEL_MS = 420

private const val PULSE_PERIOD_MS = 1100

private const val RING_ALPHA_MAX = 0.9f
private const val RING_ALPHA_SWING = 0.5f

/** In pixels rather than dp: it is a fraction of a stroke, not a layout dimension. */
private const val RING_SPREAD_PX = 6f

private val RING_WIDTH = 2.dp

/** Breathing room between the widget and the edge of the hole. */
private val SPOTLIGHT_PADDING = 8.dp

private val SPOTLIGHT_CORNER = 16.dp

private val CARD_MARGIN = 16.dp
private val CARD_GAP = 12.dp
private val CARD_MAX_WIDTH = 380.dp
private val CARD_CORNER = 20.dp
private val CARD_ELEVATION = 8.dp

private val DOT_ACTIVE = 7.dp
private val DOT_IDLE = 5.dp
