package com.idomarhaim.goalpilot.data.widget

import android.content.Context
import androidx.core.content.edit
import com.idomarhaim.goalpilot.domain.model.WidgetSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last picture the widget pack drew, on disk.
 *
 * A widget update is not a screen opening: the launcher asks for a tree, the app
 * process may have been dead for hours, and whatever Firestore can answer in the
 * next second is all there is. Without a cache that produces a tile which
 * *blanks* whenever the network is slow — a home screen that flickers between
 * having data and not, on somebody else's schedule.
 *
 * With one, the contract is honest instead: the tile always shows the last thing
 * that was true, and says when that was (`WidgetTileContent.asOf`). That is
 * §5.3's shape — *an as-of stamp, not a connectivity story* — applied to the one
 * surface that is always offline in the sense that matters, because nobody is
 * looking at it when it refreshes.
 *
 * `SharedPreferences` rather than DataStore for the reason
 * `AppPreferencesRepositoryImpl` already gives: one small value, read
 * synchronously before anything can be drawn, and no new dependency. Here it is
 * sharper still — `provideGlance` is on a broadcast's time budget.
 */
@Singleton
class WidgetSnapshotStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Tolerant on purpose: an unknown key from a newer build is ignored rather
     * than thrown. A parse failure here would take out every tile on the home
     * screen after an update, which is the worst possible moment and the one
     * nobody tests.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** The last snapshot written, or a never-captured one. */
    fun read(): WidgetSnapshot {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return WidgetSnapshot()
        return runCatching { json.decodeFromString<WidgetSnapshot>(raw) }.getOrElse { WidgetSnapshot() }
    }

    fun write(snapshot: WidgetSnapshot) {
        prefs.edit { putString(KEY_SNAPSHOT, json.encodeToString(snapshot)) }
    }

    /**
     * Drops the cache. Called on sign-out: the tile must not keep showing the
     * previous account's week to whoever picks the phone up next, and "signed
     * out" is a state it already knows how to draw.
     */
    fun clear() {
        prefs.edit { remove(KEY_SNAPSHOT) }
    }

    private companion object {
        /** Its own file, not the UI prefs: this is a cache and is safe to delete. */
        const val PREFS_NAME = "goalpilot_widget_snapshot"
        const val KEY_SNAPSHOT = "snapshot_json"
    }
}
