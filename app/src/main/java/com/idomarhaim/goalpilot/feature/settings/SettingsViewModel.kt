package com.idomarhaim.goalpilot.feature.settings

import androidx.lifecycle.ViewModel
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Spec §4.9's Settings surface: **Profile is the account, Settings is the
 * device, and sign-out is the test.**
 *
 * ⚠️ **It depends on `AppPreferencesRepository` and on nothing else, and that is
 * the whole design rather than a small mercy.** This screen is reachable from
 * the sign-in screen **with no account at all**, which is what proves the split;
 * an `AuthRepository` here would compile, work while signed in, and quietly
 * make the signed-out entry point either empty or broken — the same defect §4.9
 * exists to fix, one layer down.
 *
 * Every flow below mirrors the store directly. They are already hot
 * [StateFlow]s of the stored value, so the first composition holds the real
 * setting: a cold flow would render one frame of defaults and repaint, which on
 * a *settings* screen reads as the app forgetting what you chose.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    val skin: StateFlow<AppSkin> = appPreferences.skin

    fun setSkin(skin: AppSkin) = appPreferences.setSkin(skin)

    val brightness: StateFlow<AppBrightness> = appPreferences.brightness

    fun setBrightness(brightness: AppBrightness) = appPreferences.setBrightness(brightness)

    val material: StateFlow<AppMaterial> = appPreferences.material

    fun setMaterial(material: AppMaterial) = appPreferences.setMaterial(material)

    val language: StateFlow<AppLanguage> = appPreferences.language

    fun setLanguage(language: AppLanguage) = appPreferences.setLanguage(language)

    val region: StateFlow<AppRegion> = appPreferences.region

    fun setRegion(region: AppRegion) = appPreferences.setRegion(region)

    val daySchedule: StateFlow<DaySchedule> = appPreferences.daySchedule

    fun setWakingHours(wakingHours: WakingHours) = appPreferences.setWakingHours(wakingHours)

    /** `null` hands *Plan tomorrow at* back to the derivation. */
    fun setPlanningOverrideMinutes(minutes: Int?) =
        appPreferences.setPlanningOverrideMinutes(minutes)
}
