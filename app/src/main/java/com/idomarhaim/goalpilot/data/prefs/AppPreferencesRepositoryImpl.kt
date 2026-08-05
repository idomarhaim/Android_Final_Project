package com.idomarhaim.goalpilot.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AppPreferencesRepository] backed by [android.content.SharedPreferences].
 *
 * Deliberately not DataStore: this is a single enum read once at startup, and
 * the theme has to be known *before* the first frame. SharedPreferences loads
 * its (tiny) backing file on the first `getSharedPreferences` call and serves
 * every later read from memory, so the initial value is available synchronously
 * and costs no new dependency. Writes go through `edit { }`, which commits
 * asynchronously — the in-memory [StateFlow] is what the UI actually observes.
 */
@Singleton
class AppPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AppPreferencesRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _skin = MutableStateFlow(AppSkin.fromId(prefs.getString(KEY_SKIN, null)))
    override val skin: StateFlow<AppSkin> = _skin.asStateFlow()

    override fun setSkin(skin: AppSkin) {
        if (_skin.value == skin) return
        prefs.edit { putString(KEY_SKIN, skin.id) }
        _skin.value = skin
    }

    override fun healthLastSyncAt(uid: String): Long = prefs.getLong(healthSyncKey(uid), 0L)

    override fun setHealthLastSyncAt(uid: String, epochMillis: Long) {
        prefs.edit { putLong(healthSyncKey(uid), epochMillis) }
    }

    private companion object {
        const val PREFS_NAME = "goalpilot_ui_prefs"
        const val KEY_SKIN = "app_skin"

        /** One key per account, so switching users does not inherit a sync clock. */
        fun healthSyncKey(uid: String) = "health_last_sync_$uid"
    }
}
