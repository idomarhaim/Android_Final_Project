package com.idomarhaim.goalpilot.feature.settings

import androidx.lifecycle.ViewModel
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.domain.repository.AiProviderRepository
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

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
    private val aiProvider: AiProviderRepository,
) : ViewModel() {

    val skin: StateFlow<AppSkin> = appPreferences.skin

    fun setSkin(skin: AppSkin) = appPreferences.setSkin(skin)

    val brightness: StateFlow<AppBrightness> = appPreferences.brightness

    fun setBrightness(brightness: AppBrightness) = appPreferences.setBrightness(brightness)

    val material: StateFlow<AppMaterial> = appPreferences.material

    fun setMaterial(material: AppMaterial) = appPreferences.setMaterial(material)

    val background: StateFlow<AppBackground> = appPreferences.background

    fun setBackground(background: AppBackground) = appPreferences.setBackground(background)

    val relief: StateFlow<AppRelief> = appPreferences.relief

    fun setRelief(relief: AppRelief) = appPreferences.setRelief(relief)

    val language: StateFlow<AppLanguage> = appPreferences.language

    fun setLanguage(language: AppLanguage) = appPreferences.setLanguage(language)

    val region: StateFlow<AppRegion> = appPreferences.region

    fun setRegion(region: AppRegion) = appPreferences.setRegion(region)

    val daySchedule: StateFlow<DaySchedule> = appPreferences.daySchedule

    fun setWakingHours(wakingHours: WakingHours) = appPreferences.setWakingHours(wakingHours)

    /** `null` hands *Plan tomorrow at* back to the derivation. */
    fun setPlanningOverrideMinutes(minutes: Int?) =
        appPreferences.setPlanningOverrideMinutes(minutes)

    // ── C13's AI section (#54) ─────────────────────────────────────
    //
    // A SECOND repository on a screen whose KDoc above says depending on one
    // and only one is "the whole design rather than a small mercy" — so the
    // reason it is allowed here is worth stating rather than assuming. That
    // rule is about `AuthRepository` specifically: this screen is reachable
    // from the sign-in screen with NO ACCOUNT, and a repository that needs one
    // would make the signed-out entry point empty or broken. `C13`'s key store
    // is device-local exactly as the preferences are (#32 §1 rejected Firestore
    // on that ground), needs no account, and works identically signed out. It
    // is on the same side of §4.9's sign-out test, not the other one.

    /** `null` — the default — means the Cloud Function proxy answers. */
    val aiCredential: StateFlow<AiCredential?> = aiProvider.credential

    /** Who answered the last model call this process made; drives §5's status line. */
    val aiLastAnswer: StateFlow<AiAnswer?> = aiProvider.lastAnswer

    fun setAiCredential(credential: AiCredential) = aiProvider.save(credential)

    /** A real delete (#54). The app returns to the proxy path with no restart. */
    fun clearAiCredential() = aiProvider.clear()
}
