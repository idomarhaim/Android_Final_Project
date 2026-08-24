package com.idomarhaim.goalpilot.ui.components

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * The **arrival** of a screen's blocks — a rise and a fade, staggered, once.
 *
 * ## What it reproduces
 *
 * `docs/prototypes/2026-08-10-calendar-surface/index.html` and
 * `2026-08-10-charts-presentation/index.html` both open the same way:
 *
 * ```css
 * --spring: cubic-bezier(.2,.9,.22,1);
 * @keyframes rise { from { opacity:0; transform:translateY(11px) } to { opacity:1; transform:none } }
 * .card { animation: rise .34s both }
 * // and, on repeated rows: style=animation-delay:${i*45}ms
 * ```
 *
 * Two things matter and only one of them is the fade. It is a rise **and** a
 * fade, driven together by one curve — the block comes up as it comes in. And
 * the list arrives as a **sequence**: the prototype's repeated rows carry a
 * per-index `animation-delay` of 40–45 ms, so the column ripples rather than
 * appearing in one slab.
 *
 * ⚠️ **The brief for this work read the `.34s` / `.36s` / `.38s` durations as
 * that stagger. They are not it.** Those three values belong to three different
 * *element classes* (an event chip, an agenda row, the hero deck), each of which
 * animates once; the thing that sequences a *list* is `animation-delay:
 * ${i*45}ms`, applied in the prototype's own render function. Ship the duration
 * spread and you have three blocks that finish at slightly different times
 * having all started together, which is not visible at all. So the stagger here
 * is a delay, and the duration is the single [ENTRANCE_DURATION_MS] the card
 * class actually uses.
 *
 * ## Why it is not [rememberChartProgress]
 *
 * That one is the house pattern for chart motion and stays so; this extends the
 * vocabulary rather than forking it. But the two are keyed on different things
 * and cannot share a handle: a chart's sweep restarts whenever **the data it is
 * drawn from** changes (that is the point of its `key`), while an entrance must
 * fire on **screen arrival and never again**. Keying an entrance on data is
 * precisely the flicker-on-every-edit failure this class exists to avoid.
 *
 * ## The three things it has to get right
 *
 * 1. **It must not re-run on recomposition.** A composable claims its slot in
 *    the wave exactly once, through a `remember` in its own composition scope
 *    ([Modifier.gpEntrance]); recomposing reads the same claim back.
 * 2. **It must not fight the scroll.** The trigger is **screen entry, not item
 *    entry** — a deliberate choice, because these columns are `LazyColumn`s and
 *    an item-entry trigger animates every card the user scrolls to, forever.
 *    [claim] answers `null` once [ENTRANCE_WINDOW_MS] has passed since the first
 *    block asked, so a card composed on scroll is simply *there*. Within the
 *    window a late arrival still joins the wave rather than starting a new one:
 *    the elapsed time is subtracted from its delay.
 * 3. **Reduce motion wins.** [rememberGpEntrance] reads the system's animator
 *    duration scale, and a scale of zero makes every [claim] answer `null`.
 *
 * The clock starts **lazily, on the first claim**, not at construction. That is
 * what lets a screen provide the entrance above its own loading gate without
 * spending the whole arrival on a spinner.
 */
@Stable
class GpEntrance(private val motion: Boolean = true) {

    /** Set by the first [claim]. Deliberately not snapshot state: it must not recompose anything. */
    private var startMillis: Long = UNSTARTED

    /** How many blocks have taken a slot in the wave so far. */
    private var claimed: Int = 0

    /**
     * Take the next slot in the arrival, and say how long this block should wait
     * before it rises.
     *
     * @return the delay in milliseconds, or `null` when this block must not
     *   animate at all — motion is off, or the arrival is over and the block
     *   belongs to a scroll rather than to the screen opening.
     */
    fun claim(nowMillis: Long): Int? {
        if (!motion) return null
        if (startMillis == UNSTARTED) startMillis = nowMillis

        val elapsed = nowMillis - startMillis
        if (elapsed > ENTRANCE_WINDOW_MS) return null

        val ordinal = claimed++
        val scheduled = (ordinal * ENTRANCE_STAGGER_MS).coerceAtMost(ENTRANCE_STAGGER_MAX_MS)
        // A block that composed late (an image measured slowly, a lazy item
        // prefetched a frame after its neighbours) joins the wave where the wave
        // has got to, instead of restarting it that far behind.
        return (scheduled - elapsed).coerceIn(0L, ENTRANCE_STAGGER_MAX_MS.toLong()).toInt()
    }

    /** Test seam: how many slots have been handed out. A recomposition must not move this. */
    val claimedCount: Int get() = claimed

    private companion object {
        const val UNSTARTED = -1L
    }
}

/**
 * The arrival the current subtree belongs to, or `null` — the default —
 * meaning **no arrival**: a card in a dialog, a sheet or a preview is simply
 * there. Static because a screen provides one value for its whole lifetime.
 */
val LocalGpEntrance = staticCompositionLocalOf<GpEntrance?> { null }

/**
 * A [GpEntrance] for this screen, with the system's reduce-motion setting
 * already applied.
 *
 * `Settings.Global.ANIMATOR_DURATION_SCALE` is the setting behind *Developer
 * options → Animator duration scale*, and the accessibility remove-animations
 * toggle sets it to zero along with the other two scales — so it is the value
 * to read whichever way the user turned motion off. Read once, at composition: a user who changes it mid-screen gets the
 * new value on their next screen, which is what every other animation in
 * Android does too.
 */
@Composable
fun rememberGpEntrance(): GpEntrance = rememberGpEntrance(key = Unit)

/**
 * As [rememberGpEntrance], but starting a **genuinely new arrival** whenever
 * [key] changes.
 *
 * Added 2026-08-24 by `visual-parity`. The no-argument form remembers on the
 * content resolver alone, which is stable for the whole process — correct when
 * the provider sits *inside* the screen, as `DashboardScreen` puts it, because
 * the screen's own composition is then the thing that comes and goes.
 *
 * It is **wrong** one level up. `NavHost` keeps a single composition position
 * for whichever destination is current, so a provider wrapped around it would
 * remember one [GpEntrance] for the whole session: the first screen would
 * arrive, its window would close, and **every screen after it would be simply
 * there** — the same silent no-op as having no provider at all, and harder to
 * notice because the first screen looks right.
 *
 * Pass the current back-stack entry's id and each navigation gets its own wave.
 */
@Composable
fun rememberGpEntrance(key: Any?): GpEntrance {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver, key) {
        val scale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        GpEntrance(motion = scale != 0f)
    }
}

/**
 * Rise and fade this block into the arrival provided by [LocalGpEntrance].
 *
 * A no-op with no entrance in scope, which is every call site outside a screen
 * that opted in. Already applied inside [GpCard], so the 30-odd cards in the app
 * need no change; put it on the non-card blocks that sit in the same column
 * (a [SectionHeader], say), or the column arrives with holes in it.
 */
@Composable
fun Modifier.gpEntrance(): Modifier {
    val entrance = LocalGpEntrance.current ?: return this

    // Claimed once per composition SLOT, not once per recomposition -- this
    // `remember` is the whole of requirement 1 above. It is keyed on the
    // entrance so that a screen genuinely re-entered gets a genuinely new wave.
    val delayMillis = remember(entrance) { entrance.claim(SystemClock.uptimeMillis()) }
        ?: return this

    val progress = remember(entrance) { Animatable(0f) }
    LaunchedEffect(entrance) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ENTRANCE_DURATION_MS,
                delayMillis = delayMillis,
                easing = GpSpring,
            ),
        )
    }

    // The lambda form: `progress.value` is read in the draw phase, so the wave
    // costs no recomposition at all -- which matters when a dozen blocks are
    // animating over a LazyColumn that is also measuring.
    //
    // ModulateAlpha is NOT a micro-optimisation here, it is a correctness fix,
    // and the render pass is what found it. The default strategy puts an alpha
    // below 1 into an offscreen buffer the size of the node -- and `gpSurface`
    // draws this app's shadow pair as round rects that deliberately extend
    // BEYOND the node, so the buffer cropped them. Every card spent the whole
    // arrival flat with a hard dark notch at its bottom-right corner, and its
    // real shadow appeared in one frame at alpha 1.0, when Compose stops
    // compositing. Modulating instead applies the alpha per draw call, so the
    // shadow fades in with the card it belongs to. Seen in
    // `EntranceRenderPass`'s 064/144/208 ms frames, before and after.
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * ENTRANCE_RISE.toPx()
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/**
 * `cubic-bezier(.2,.9,.22,1)` — the prototypes' `--spring`, used verbatim.
 *
 * Not a spring in the physics sense and not [androidx.compose.animation.core.Spring]:
 * it is a hard-out, long-settle curve that reaches nine tenths of the way in the
 * first fifth of its time. That front-loading is why the fade reads as *arriving*
 * rather than *dissolving in*.
 */
val GpSpring: Easing = CubicBezierEasing(0.2f, 0.9f, 0.22f, 1.0f)

/** `.34s`, the prototype's `.card` — the one duration, because the stagger is a delay. */
const val ENTRANCE_DURATION_MS = 340

/** `animation-delay: ${i*45}ms` from the prototype's agenda rows. */
const val ENTRANCE_STAGGER_MS = 45

/**
 * Cap on the accumulated delay. The prototype's lists are one screenful; a
 * dashboard is not, and an uncapped stagger has the tenth card still waiting
 * while the first has finished twice over.
 */
const val ENTRANCE_STAGGER_MAX_MS = 450

/**
 * How long the screen counts as *arriving*. After this, a block that composes
 * is a block the user scrolled to, and it does not animate.
 */
val ENTRANCE_WINDOW_MS: Long = (ENTRANCE_STAGGER_MAX_MS + ENTRANCE_DURATION_MS).toLong()

/** `translateY(11px)` from `@keyframes rise`, on a 392-px-wide phone mock, so 11.dp. */
val ENTRANCE_RISE = 11.dp
