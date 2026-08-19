package com.idomarhaim.goalpilot.ui.widget

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.idomarhaim.goalpilot.data.widget.WidgetSnapshotSource
import com.idomarhaim.goalpilot.data.widget.WidgetSnapshotStore
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetTile
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One tile of the pack, at whatever size the launcher gave it.
 *
 * `GlanceAppWidget` is not a `ViewModel` and there is no lifecycle here: the
 * system asks for a tree, this builds one, and the process may be killed
 * immediately after. Hilt is reached through a [WidgetEntryPoint] rather than an
 * injected constructor, because the object is constructed by its receiver, which
 * the *system* instantiates.
 *
 * **Abstract, with one concrete subclass per tile** — a Glance constraint rather
 * than taste. `GlanceAppWidgetManager` resolves placed widgets by the *class* of
 * the `GlanceAppWidget`, so five receivers sharing one class would make a
 * refresh of any tile re-render **all** of them as that tile.
 */
abstract class GoalPilotWidget(private val tile: WidgetTile) : GlanceAppWidget() {

    /**
     * `SizeMode.Responsive`, with the four sizes §4.5 designs for.
     *
     * This started as `SizeMode.Exact` plus a dp threshold, and that was wrong in
     * a way only a device shows. `Observed:` 2026-08-16, `Pixel_10_Pro_XL`, API
     * 37 — a nominal `2×2` is about **190 dp** there, comfortably past the 180 dp
     * threshold, so the smallest tile rendered the **largest** layout and the
     * whole four-size ladder collapsed to one rung.
     *
     * The bug is not the number, it is the *idea* of a number: dp per cell varies
     * by device and launcher (§4.5 says exactly this), so no fixed threshold can
     * be right everywhere. Responsive inverts it — the pack declares the four
     * shapes it was designed at, the **launcher** picks the nearest, and
     * [LocalSize] then returns one of these four exactly. [WidgetSize.of] maps
     * them back by identity and never has to guess.
     *
     * The cost is that Glance builds all four trees per update instead of one.
     * That is real and it is worth it: it is paid once per refresh, off-screen,
     * to remove a class of error that is invisible until someone places the
     * widget on a phone you do not own.
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_SMALL, SIZE_WIDE, SIZE_TALL, SIZE_LARGE),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val store = entry.snapshotStore()
        val source = entry.snapshotSource()
        val buildTile = entry.buildTile()
        val skin = entry.preferences().skin.value

        // The cache is read BEFORE anything can block, and it is a
        // SharedPreferences hit, so it is effectively free.
        val cached = store.read()

        provideContent {
            var snapshot by remember { mutableStateOf(cached) }

            // The refresh happens INSIDE the composition, not before it. That
            // ordering is the whole fix. `Observed:` 2026-08-16 — with the fetch
            // in front, a cold process left the tile stuck on Glance's blank
            // loading layout for over thirty seconds, because nothing had been
            // emitted yet and the repository read had not returned. The cache
            // existed precisely to prevent that and could not, since it was only
            // consulted after the timeout. Now the last known picture is on
            // screen first and the fresh one replaces it when it arrives.
            LaunchedEffect(id) {
                val fresh = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                    runCatching { source.load(System.currentTimeMillis()) }.getOrNull()
                }
                if (fresh != null) {
                    store.write(fresh)
                    snapshot = fresh
                }
            }

            val glanceContext = LocalContext.current
            val size: DpSize = LocalSize.current
            val content = buildTile(
                snapshot = snapshot,
                tile = tile,
                size = WidgetSize.of(size.width.value.toInt(), size.height.value.toInt()),
                // The Glance context, not the application one: on Android 13+ a
                // per-app language override lives on the context, so reading
                // strings off the application context renders the widget in the
                // system language while the app is in the user's.
                //
                // Note that a correct context is necessary and NOT sufficient.
                // `Observed:` 2026-08-16 — this context reported he_IL on a Hebrew
                // phone and still returned English, because the strings had been
                // filed under values-he while the runtime looks up Hebrew by its
                // legacy code, iw. See values-iw/widget_strings.xml.
                AndroidWidgetStrings(glanceContext),
            )
            // The palette carries BOTH brightnesses; the launcher chooses. See
            // WidgetPalette's KDoc for why it cannot be resolved here.
            WidgetTile(content, WidgetPalette.of(skin))
        }
    }

    companion object {
        /**
         * The four shapes §4.5 designs for, in dp.
         *
         * These are the *nominal* dp of a 2- and 4-cell span under Android's own
         * `70n - 30` guidance. They do not have to match any real launcher — they
         * are candidates, and the launcher picks the nearest to what it actually
         * granted. That is the point: the arithmetic that varies by device is
         * done by the device.
         */
        val SIZE_SMALL: DpSize = DpSize(110.dp, 110.dp)
        val SIZE_WIDE: DpSize = DpSize(250.dp, 110.dp)
        val SIZE_TALL: DpSize = DpSize(110.dp, 250.dp)
        val SIZE_LARGE: DpSize = DpSize(250.dp, 250.dp)

        /**
         * Shorter than it was, because it no longer gates the first frame — the
         * cache does. This is only how long a *refresh* may take before the tile
         * keeps showing the last known picture, and a widget refresh runs inside
         * a broadcast with about ten seconds to live.
         */
        const val FETCH_TIMEOUT_MS = 3_000L
    }
}
