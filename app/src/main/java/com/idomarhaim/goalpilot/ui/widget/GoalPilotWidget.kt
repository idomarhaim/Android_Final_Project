package com.idomarhaim.goalpilot.ui.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.idomarhaim.goalpilot.data.widget.WidgetSnapshotSource
import com.idomarhaim.goalpilot.data.widget.WidgetSnapshotStore
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetSnapshot
import com.idomarhaim.goalpilot.domain.model.WidgetTile
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One tile of the pack, at whatever size the launcher gave it.
 *
 * `GlanceAppWidget` is not a `ViewModel` and there is no lifecycle here: the
 * system asks for a tree, this builds one, and the process may be killed
 * immediately after. So the read is done **before** `provideContent`, with a
 * ceiling on how long it may take, and a cache underneath it — see [load].
 *
 * Hilt reaches this through a [WidgetEntryPoint] rather than an injected
 * constructor, because the object is constructed by its receiver, which the
 * *system* instantiates.
 *
 * **Abstract, with one concrete subclass per tile** — and that is a Glance
 * constraint rather than taste. `GlanceAppWidgetManager` resolves placed widgets
 * by the *class* of the `GlanceAppWidget`, so `updateAll` walks every id whose
 * receiver declares that class. Five receivers sharing one class would therefore
 * make a refresh of any tile re-render **all** of them as that tile: place the
 * week donut and the level ring, refresh one, and both become the same card. The
 * subclasses in `WidgetReceivers.kt` exist to keep those identities apart.
 */
abstract class GoalPilotWidget(private val tile: WidgetTile) : GlanceAppWidget() {

    /**
     * `SizeMode.Exact` — the tile is rebuilt at the size it actually occupies.
     *
     * §4.5: *the launcher decides the real dp a `2×2` or `4×4` occupies, and it
     * varies by device and launcher.* `SizeMode.Responsive` would ask for a fixed
     * set of candidate sizes and pick the nearest, which is the same guess this
     * pack is trying not to make; `Exact` costs a rebuild on resize and buys a
     * tile that is never laid out for a cell it is not in.
     */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val snapshot = load(entry)
        val skin = entry.preferences().skin.value
        val buildTile = entry.buildTile()

        provideContent {
            val glanceContext = LocalContext.current
            val size: DpSize = LocalSize.current
            val palette = WidgetPalette.of(skin, glanceContext.isNightMode())
            val content = buildTile(
                snapshot = snapshot,
                tile = tile,
                size = WidgetSize.of(size.width.value.toInt(), size.height.value.toInt()),
                // The Glance context, not the application one: on Android 13+ a
                // per-app language override lives on the context, so reading
                // strings off the application context renders the widget in the
                // system language while the app is in the user's.
                AndroidWidgetStrings(glanceContext),
            )
            WidgetTile(content, palette)
        }
    }

    /**
     * Fresh if it can be had quickly, the last known picture otherwise.
     *
     * The timeout is the whole point. A widget refresh runs inside a broadcast,
     * and a repository read that hangs on a cold network would either be killed
     * by the system or leave the tile blank — both of which look to the user like
     * the widget is broken rather than like the network is slow. Falling back to
     * the cache means a stale-but-true tile, and the tile says how stale it is.
     */
    private suspend fun load(entry: WidgetEntryPoint): WidgetSnapshot {
        val store: WidgetSnapshotStore = entry.snapshotStore()
        val source: WidgetSnapshotSource = entry.snapshotSource()
        val fresh = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            runCatching { source.load(System.currentTimeMillis()) }.getOrNull()
        }
        return if (fresh != null) {
            store.write(fresh)
            fresh
        } else {
            store.read()
        }
    }

    private companion object {
        /**
         * Generous enough for a Firestore cache read plus a cold Hilt graph,
         * short enough to stay well inside the ~10 s a broadcast receiver gets
         * before the system considers it hung.
         */
        const val FETCH_TIMEOUT_MS = 4_000L
    }
}

/**
 * The device's own dark mode, which is what a home screen follows.
 *
 * `AppSkin` is the palette (§4.1) and brightness is a separate axis; nothing in
 * the app overrides the system's here, and nothing should — the tile sits on the
 * user's wallpaper beside every other widget, and a tile that stayed light on a
 * dark home screen would be the one thing on it that ignored the setting.
 */
internal fun Context.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
