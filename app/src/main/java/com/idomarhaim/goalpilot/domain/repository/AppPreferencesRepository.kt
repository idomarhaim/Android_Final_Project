package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.domain.model.AppSkin
import kotlinx.coroutines.flow.StateFlow

/**
 * Device-local UI preferences. Nothing here is synced to Firestore — a skin is a
 * property of *this install*, not of the account.
 *
 * [skin] is a [StateFlow] rather than a cold `Flow` so the very first composition
 * already holds the stored value; a cold flow would render one frame in the
 * default skin and then visibly repaint.
 */
interface AppPreferencesRepository {

    val skin: StateFlow<AppSkin>

    fun setSkin(skin: AppSkin)
}
