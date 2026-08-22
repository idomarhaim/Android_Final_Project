package com.idomarhaim.goalpilot.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // offeredFromId, not fromId: a device that selected עברית before #51 was
    // deferred still has "he" stored, and a faithful read would hold the freeze
    // everywhere except the phones that had already used the feature. The stored
    // value is left alone rather than rewritten -- #51 resumes by widening
    // AppLanguage.OFFERED, and the preference is then honoured again untouched.
    private val _language =
        MutableStateFlow(AppLanguage.offeredFromId(prefs.getString(KEY_LANGUAGE, null)))
    override val language: StateFlow<AppLanguage> = _language.asStateFlow()

    override fun setLanguage(language: AppLanguage) {
        if (_language.value == language) return
        prefs.edit { putString(KEY_LANGUAGE, language.id) }
        _language.value = language
    }

    private val _brightness =
        MutableStateFlow(AppBrightness.fromId(prefs.getString(KEY_BRIGHTNESS, null)))
    override val brightness: StateFlow<AppBrightness> = _brightness.asStateFlow()

    override fun setBrightness(brightness: AppBrightness) {
        if (_brightness.value == brightness) return
        prefs.edit { putString(KEY_BRIGHTNESS, brightness.id) }
        _brightness.value = brightness
    }

    private val _material =
        MutableStateFlow(AppMaterial.fromId(prefs.getString(KEY_MATERIAL, null)))
    override val material: StateFlow<AppMaterial> = _material.asStateFlow()

    override fun setMaterial(material: AppMaterial) {
        if (_material.value == material) return
        prefs.edit { putString(KEY_MATERIAL, material.id) }
        _material.value = material
    }

    private val _background =
        MutableStateFlow(AppBackground.fromId(prefs.getString(KEY_BACKGROUND, null)))
    override val background: StateFlow<AppBackground> = _background.asStateFlow()

    override fun setBackground(background: AppBackground) {
        if (_background.value == background) return
        prefs.edit { putString(KEY_BACKGROUND, background.id) }
        _background.value = background
    }

    // Stored as an id rather than a boolean, which is `AppRelief`'s own note:
    // `getBoolean` cannot tell ABSENT from FALSE, so a key renamed later would
    // read as `flat` rather than as `the default` -- and the two differ the day
    // the default changes.
    private val _relief =
        MutableStateFlow(AppRelief.fromId(prefs.getString(KEY_RELIEF, null)))
    override val relief: StateFlow<AppRelief> = _relief.asStateFlow()

    override fun setRelief(relief: AppRelief) {
        if (_relief.value == relief) return
        prefs.edit { putString(KEY_RELIEF, relief.id) }
        _relief.value = relief
    }

    private val _region = MutableStateFlow(AppRegion.fromId(prefs.getString(KEY_REGION, null)))
    override val region: StateFlow<AppRegion> = _region.asStateFlow()

    override fun setRegion(region: AppRegion) {
        if (_region.value == region) return
        prefs.edit { putString(KEY_REGION, region.id) }
        _region.value = region
    }

    // The planning override is stored as a sentinel rather than by absence:
    // SharedPreferences has no nullable Int, and `contains()` plus `getInt()` is
    // two reads that can disagree if a write lands between them. NO_OVERRIDE is
    // outside 0..1439, so it can never collide with a real minute-of-day.
    private val _daySchedule = MutableStateFlow(
        DaySchedule(
            waking = WakingHours(
                startMinutes = prefs.getInt(KEY_WAKING_START, WakingHours.DEFAULT.startMinutes),
                endMinutes = prefs.getInt(KEY_WAKING_END, WakingHours.DEFAULT.endMinutes),
            ),
            planningOverrideMinutes = prefs.getInt(KEY_PLANNING_OVERRIDE, NO_OVERRIDE)
                .takeIf { it != NO_OVERRIDE },
        ),
    )
    override val daySchedule: StateFlow<DaySchedule> = _daySchedule.asStateFlow()

    override fun setWakingHours(wakingHours: WakingHours) {
        if (_daySchedule.value.waking == wakingHours) return
        prefs.edit {
            putInt(KEY_WAKING_START, wakingHours.startMinutes)
            putInt(KEY_WAKING_END, wakingHours.endMinutes)
        }
        _daySchedule.value = _daySchedule.value.copy(waking = wakingHours)
    }

    override fun setPlanningOverrideMinutes(minutes: Int?) {
        if (_daySchedule.value.planningOverrideMinutes == minutes) return
        prefs.edit { putInt(KEY_PLANNING_OVERRIDE, minutes ?: NO_OVERRIDE) }
        _daySchedule.value = _daySchedule.value.copy(planningOverrideMinutes = minutes)
    }

    override fun healthLastSyncAt(uid: String): Long = prefs.getLong(healthSyncKey(uid), 0L)

    override fun setHealthLastSyncAt(uid: String, epochMillis: Long) {
        prefs.edit { putLong(healthSyncKey(uid), epochMillis) }
    }

    /** `0` reads as *never shown*, which is what a fresh install honestly is. */
    override fun missReviewLastShownAt(): Long = prefs.getLong(KEY_MISS_REVIEW_SHOWN_AT, 0L)

    override fun setMissReviewLastShownAt(epochMillis: Long) {
        prefs.edit { putLong(KEY_MISS_REVIEW_SHOWN_AT, epochMillis) }
    }

    // A StateFlow like the appearance axes above, not a plain getter like the
    // two stamps above it: this one is read to decide whether the guided tour
    // opens on the first composition after sign-in, and the tour finishing has
    // to move it without a restart -- `setTutorialSeenVersion` fires while the
    // overlay that reads it is still on screen.
    private val _tutorialSeenVersion =
        MutableStateFlow(prefs.getInt(KEY_TUTORIAL_SEEN_VERSION, 0))
    override val tutorialSeenVersion: StateFlow<Int> = _tutorialSeenVersion.asStateFlow()

    override fun setTutorialSeenVersion(version: Int) {
        if (_tutorialSeenVersion.value == version) return
        prefs.edit { putInt(KEY_TUTORIAL_SEEN_VERSION, version) }
        _tutorialSeenVersion.value = version
    }

    private companion object {
        const val PREFS_NAME = "goalpilot_ui_prefs"
        const val KEY_SKIN = "app_skin"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_BRIGHTNESS = "app_brightness"
        const val KEY_MATERIAL = "app_material"
        const val KEY_BACKGROUND = "app_background"
        const val KEY_RELIEF = "app_relief"
        const val KEY_REGION = "app_region"
        const val KEY_WAKING_START = "day_waking_start_minutes"
        const val KEY_WAKING_END = "day_waking_end_minutes"
        const val KEY_PLANNING_OVERRIDE = "day_planning_override_minutes"
        const val KEY_MISS_REVIEW_SHOWN_AT = "miss_review_last_shown_at"

        /**
         * `0` -- the absent default -- reads as *this install has seen no tour*,
         * which a fresh install honestly has not. Deliberately NOT a boolean;
         * see `AppPreferencesRepository.tutorialSeenVersion` for why.
         */
        const val KEY_TUTORIAL_SEEN_VERSION = "tutorial_seen_version"

        /** Outside `0..1439`, so it cannot collide with a real minute-of-day. */
        const val NO_OVERRIDE = -1

        /** One key per account, so switching users does not inherit a sync clock. */
        fun healthSyncKey(uid: String) = "health_last_sync_$uid"
    }
}
