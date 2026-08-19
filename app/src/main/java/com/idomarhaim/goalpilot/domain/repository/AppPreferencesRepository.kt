package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppSkin
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
