package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import kotlinx.coroutines.flow.StateFlow

/**
 * Device-local UI preferences. Nothing here is synced to Firestore — a skin is a
 * property of *this install*, not of the account.
 *
 * [skin] and [language] are [StateFlow]s rather than cold `Flow`s so the very
 * first composition already holds the stored value; a cold flow would render one
 * frame in the default skin — or, worse, one frame in the wrong language and
 * direction — and then visibly repaint.
 */
interface AppPreferencesRepository {

    val skin: StateFlow<AppSkin>

    fun setSkin(skin: AppSkin)

    /**
     * The language the app speaks, per spec §5.1.
     *
     * **Device-local and beside the skin, on the same reasoning:** it must be
     * known before the first frame, and the account is not known until Auth
     * resolves. §5.1 states this directly — *"per-device, beside the skin"*.
     *
     * Only *speech* follows this. User-authored content — goal titles, task
     * titles, life-area names — is never translated (§8, closed scope): §5.1's
     * discriminator is speech vs content, and content never moves.
     */
    val language: StateFlow<AppLanguage>

    fun setLanguage(language: AppLanguage)

    /**
     * Light or dark, per spec §4.9's Appearance section.
     *
     * A [StateFlow] for the same reason [skin] is, and more sharply: brightness
     * is applied *outside* the theme in `MainActivity`, so a cold flow would
     * render the first frame at the system's brightness and then repaint the
     * whole window.
     */
    val brightness: StateFlow<AppBrightness>

    fun setBrightness(brightness: AppBrightness)

    /**
     * The surface the app is drawn out of, per spec §4.1 — the second axis
     * above [skin].
     *
     * A [StateFlow] for the same reason [skin] and [brightness] are, and it
     * inherits one extra duty from being *above* brightness: the material
     * decides whether the brightness setting can move at all
     * ([AppMaterial.resolveDark]), so a cold flow here would render the first
     * frame in a brightness the material forbids and then repaint the window.
     *
     * Device-local, like everything else here. §4.9's sign-out test puts
     * material in the left column beside skin and brightness.
     */
    val material: StateFlow<AppMaterial>

    fun setMaterial(material: AppMaterial)

    /**
     * Where the user is, for week start and date order — spec §5.1's **Region**,
     * decoupled from [language] on Ido's call (see [AppRegion]).
     *
     * **Stored and read out, not yet wired.** The app's date formatters are
     * `#51`'s and `#51` is deferred; §4.9's Settings screen states what this
     * choice means using [AppRegion]'s own arithmetic rather than asserting a
     * change nothing has made.
     */
    val region: StateFlow<AppRegion>

    fun setRegion(region: AppRegion)

    /**
     * Waking hours and the nightly planning time — spec §4.9's **Your day**.
     *
     * One value rather than three keys, because *Plan tomorrow at* is a
     * function of *Awake between* until somebody overrides it; see [DaySchedule].
     * The two setters below are the only two edits the screen can make, which is
     * what keeps "unset" a reachable state rather than a value that has to be
     * maintained.
     *
     * Neither consumer is built: `#8`'s scheduled half owns the reminder clamp
     * and `C9b`'s load bar. §4.9 builds the controls; wiring is theirs.
     */
    val daySchedule: StateFlow<DaySchedule>

    fun setWakingHours(wakingHours: WakingHours)

    /** `null` restores the derived time — one hour before waking hours end. */
    fun setPlanningOverrideMinutes(minutes: Int?)

    /**
     * When Health Connect was last read for [uid], in epoch millis, or 0 if never.
     *
     * Per-account, because the throttle it drives decides whether *this* user's
     * readings are already in Firestore — a shared timestamp would make a freshly
     * signed-in account wait out the other one's window. Device-local like
     * everything else here: it describes this install's sync clock, not the user.
     */
    fun healthLastSyncAt(uid: String): Long

    fun setHealthLastSyncAt(uid: String, epochMillis: Long)
}
