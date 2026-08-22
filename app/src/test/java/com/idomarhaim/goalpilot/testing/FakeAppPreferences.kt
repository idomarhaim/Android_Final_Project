package com.idomarhaim.goalpilot.testing

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
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for the SharedPreferences-backed [AppPreferencesRepository].
 *
 * ### Why it is shared rather than private to one suite
 *
 * It was private to `HealthSyncTest` until the guided tour needed a second one
 * (2026-08-22). Two hand-written fakes of a fifteen-member interface is a
 * standing invitation to drift: the next member added to the interface has to be
 * written twice, and the copy whose suite happens not to read it gets a
 * `TODO()`, a `0`, or a subtly different default — which then quietly changes
 * what the *other* suite is testing. One fake has one set of defaults, and every
 * suite that adopts it inherits the same ones.
 *
 * Every value starts at its production default, so a test that does not mention
 * a setting is testing the app as a fresh install has it.
 */
class FakeAppPreferences : AppPreferencesRepository {

    private val healthStamps = mutableMapOf<String, Long>()

    override val skin = MutableStateFlow(AppSkin.AURORA)
    override fun setSkin(skin: AppSkin) { this.skin.value = skin }

    override val language = MutableStateFlow(AppLanguage.DEFAULT)
    override fun setLanguage(language: AppLanguage) { this.language.value = language }

    override val brightness = MutableStateFlow(AppBrightness.DEFAULT)
    override fun setBrightness(brightness: AppBrightness) { this.brightness.value = brightness }

    override val material = MutableStateFlow(AppMaterial.DEFAULT)
    override fun setMaterial(material: AppMaterial) { this.material.value = material }

    override val background = MutableStateFlow(AppBackground.DEFAULT)
    override fun setBackground(background: AppBackground) { this.background.value = background }

    override val relief = MutableStateFlow(AppRelief.DEFAULT)
    override fun setRelief(relief: AppRelief) { this.relief.value = relief }

    override val region = MutableStateFlow(AppRegion.DEFAULT)
    override fun setRegion(region: AppRegion) { this.region.value = region }

    override val daySchedule = MutableStateFlow(DaySchedule.DEFAULT)
    override fun setWakingHours(wakingHours: WakingHours) {
        daySchedule.value = daySchedule.value.copy(waking = wakingHours)
    }

    override fun setPlanningOverrideMinutes(minutes: Int?) {
        daySchedule.value = daySchedule.value.copy(planningOverrideMinutes = minutes)
    }

    override fun healthLastSyncAt(uid: String): Long = healthStamps[uid] ?: 0L
    override fun setHealthLastSyncAt(uid: String, epochMillis: Long) {
        healthStamps[uid] = epochMillis
    }

    private var missReviewShownAt: Long = 0L
    override fun missReviewLastShownAt(): Long = missReviewShownAt
    override fun setMissReviewLastShownAt(epochMillis: Long) {
        missReviewShownAt = epochMillis
    }

    /** `0` is a fresh install: no tour has been seen. */
    override val tutorialSeenVersion = MutableStateFlow(0)
    override fun setTutorialSeenVersion(version: Int) {
        tutorialSeenVersion.value = version
    }

    /**
     * §1.3's permanent per-goal dismissal (`C22` #44, #65), as a plain growing set
     * — which is what the production store is too.
     *
     * Deliberately exposed so a suite can seed it: *this goal was already
     * dismissed* is a starting state a test needs, and there is no un-dismiss to
     * reach it through. Empty is a fresh install, which has dismissed nothing.
     */
    val dismissedMeasureProposals = mutableSetOf<String>()
    override fun isMeasureProposalDismissed(goalId: String): Boolean =
        goalId in dismissedMeasureProposals

    override fun dismissMeasureProposal(goalId: String) {
        dismissedMeasureProposals += goalId
    }
}
