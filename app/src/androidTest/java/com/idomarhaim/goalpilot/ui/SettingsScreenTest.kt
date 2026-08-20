package com.idomarhaim.goalpilot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import com.idomarhaim.goalpilot.feature.settings.SettingsContent
import com.idomarhaim.goalpilot.feature.settings.TAG_ACCOUNT_CONSEQUENCE
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_STATUS
import com.idomarhaim.goalpilot.feature.settings.TAG_PLANNING_CONSEQUENCE
import com.idomarhaim.goalpilot.feature.settings.TAG_PROFILE_ROW
import com.idomarhaim.goalpilot.feature.settings.TAG_REGION_CONSEQUENCE
import com.idomarhaim.goalpilot.feature.settings.TAG_SCOPE_LINE
import com.idomarhaim.goalpilot.feature.settings.TAG_WAKING_CONSEQUENCE
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * Spec §4.9's Settings surface, on a device.
 *
 * ### Why these assertions and not others
 *
 * §4.9's one new component is the **consequence line**, and its whole claim is
 * that it carries *live values* — the arithmetic moves in front of the person
 * moving the setting. A line that has quietly stopped tracking its control
 * still renders, still reads plausibly, and is invisible in a screenshot. So
 * every test below moves a setting and reads the sentence.
 *
 * ### The one thing only a device can answer
 *
 * `AppRegionTest` asserts on the JVM that Israel starts the week on Sunday.
 * Android resolves `WeekFields` through **ICU** and the JVM through its own
 * CLDR copy, so the two agreeing is a measurement, not a deduction — and week
 * start is *derived and never stored*, so nothing in the app would disagree
 * with a wrong answer. `weekStart_forIsrael_isSundayOnDevice` is that
 * measurement, for the exact combination Ido asked for (English + Israel).
 *
 * Drives [SettingsContent] rather than `SettingsScreen`: state is hoisted
 * there, so this needs no Hilt graph and no Firebase.
 */
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * The screen with real, editable state behind it — a `remember`ed
     * [DaySchedule] and friends standing in for the preference store.
     */
    private fun setContent(
        initialRegion: AppRegion = AppRegion.SYSTEM,
        initialMaterial: AppMaterial = AppMaterial.DEFAULT,
        initialSchedule: DaySchedule = DaySchedule.DEFAULT,
        initialAiCredential: AiCredential? = null,
        aiLastAnswer: AiAnswer? = null,
        signedIn: Boolean = true,
        onOpenProfile: () -> Unit = {},
    ) {
        composeRule.setContent {
            var region by remember { mutableStateOf(initialRegion) }
            var schedule by remember { mutableStateOf(initialSchedule) }
            var skin by remember { mutableStateOf(AppSkin.DEFAULT) }
            var brightness by remember { mutableStateOf(AppBrightness.DEFAULT) }
            var material by remember { mutableStateOf(initialMaterial) }
            var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
            var aiCredential by remember { mutableStateOf(initialAiCredential) }

            GoalPilotTheme(skin = skin, material = material) {
                SettingsContent(
                    skin = skin,
                    onSkin = { skin = it },
                    brightness = brightness,
                    onBrightness = { brightness = it },
                    material = material,
                    onMaterial = { material = it },
                    language = language,
                    onLanguage = { language = it },
                    region = region,
                    onRegion = { region = it },
                    schedule = schedule,
                    onWakingHours = { schedule = schedule.copy(waking = it) },
                    onPlanningOverrideMinutes = {
                        schedule = schedule.copy(planningOverrideMinutes = it)
                    },
                    // C13 (#54): real editable state, like everything else here,
                    // so `MaterialPickerUiTest`-style save/replace/delete flows
                    // are exercised end to end rather than asserted on a
                    // constant. See `AiSectionUiTest` for those.
                    aiCredential = aiCredential,
                    aiLastAnswer = aiLastAnswer,
                    onAiCredential = { aiCredential = it },
                    onClearAiCredential = { aiCredential = null },
                    onBack = {},
                    onOpenProfile = if (signedIn) onOpenProfile else null,
                )
            }
        }
    }

    private fun textOf(tag: String): String =
        composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config
            .first { it.key.name == "Text" }
            .value
            .toString()

    // ------------------------------------------------------- the scope line

    @Test
    fun theScreenOpensWithAScopeLineAndNotATitleAlone() {
        setContent()
        composeRule.onNodeWithTag(TAG_SCOPE_LINE).assertIsDisplayed()
        assertThat(textOf(TAG_SCOPE_LINE)).contains("sign out")
    }

    /**
     * §0.4 forbids the app to be silent about what outlives sign-out, and after
     * `C13` #54 the encrypted API key is the one thing here a user might *want*
     * gone when they sign out. The scope line names it, and names the way out.
     */
    @Test
    fun theScopeLineNamesTheKeyThatOutlivesSignOut() {
        setContent()
        val line = textOf(TAG_SCOPE_LINE)
        assertThat(line).contains("API key")
        assertThat(line).contains("remove")
    }

    // ------------------------------------------------- §4.9's fifth section

    /**
     * `#48` shipped four of §4.9's five sections and recorded the fifth as
     * missing. `#54` built it, so the assertion that matters on the *screen* is
     * that the section is there at all — the controls inside it are
     * `AiSectionUiTest`'s.
     */
    @Test
    fun theAiSectionIsOnTheScreenAndSpeaksBeforeAnyKeyExists() {
        setContent()
        composeRule.onNodeWithTag(TAG_AI_STATUS).performScrollTo().assertIsDisplayed()
        // The default state: no key, and the row still says which model answers.
        assertThat(textOf(TAG_AI_STATUS)).contains("free model")
    }

    // ----------------------------------------- week start, measured on device

    @Test
    fun weekStart_forIsrael_isSundayOnDevice() {
        setContent(initialRegion = AppRegion("IL"))
        assertThat(textOf(TAG_REGION_CONSEQUENCE)).contains("Sunday")
    }

    @Test
    fun weekStart_forBritain_isMondayOnDevice() {
        setContent(initialRegion = AppRegion("GB"))
        assertThat(textOf(TAG_REGION_CONSEQUENCE)).contains("Monday")
    }

    @Test
    fun theRegionConsequenceAlsoShowsHowADateReads() {
        setContent(initialRegion = AppRegion("GB"))
        val british = textOf(TAG_REGION_CONSEQUENCE)
        assertThat(british).contains("today reads")
        // A digit, i.e. an actual rendered date rather than a description of one.
        assertThat(british.any { it.isDigit() }).isTrue()
    }

    // --------------------------------------- the awake span's live arithmetic

    @Test
    fun theDefaultDayStatesSixteenHoursAndATwelveHourRedThreshold() {
        setContent()
        val line = textOf(TAG_WAKING_CONSEQUENCE)
        assertThat(line).contains("16 h")
        assertThat(line).contains("12 h")
    }

    @Test
    fun aShorterDayRestatesBothNumbers() {
        setContent(
            initialSchedule = DaySchedule(
                waking = WakingHours(startMinutes = 8 * 60, endMinutes = 20 * 60),
            ),
        )
        val line = textOf(TAG_WAKING_CONSEQUENCE)
        assertThat(line).contains("12 h")
        assertThat(line).contains("9 h")
        assertThat(line).doesNotContain("16 h")
    }

    @Test
    fun anEmptySpanSaysSoRatherThanRenderingZero() {
        setContent(
            initialSchedule = DaySchedule(
                waking = WakingHours(startMinutes = 9 * 60, endMinutes = 9 * 60),
            ),
        )
        assertThat(textOf(TAG_WAKING_CONSEQUENCE)).contains("No awake hours")
    }

    // ------------------------------- plan-tomorrow-at: derived vs overridden

    @Test
    fun untouched_thePlanningLineSaysItFollowsWakingHours() {
        setContent()
        assertThat(textOf(TAG_PLANNING_CONSEQUENCE)).contains("Follows your waking hours")
    }

    @Test
    fun overridden_thePlanningLineSaysItNoLongerFollowsThem() {
        setContent(initialSchedule = DaySchedule.DEFAULT.copy(planningOverrideMinutes = 19 * 60))
        val line = textOf(TAG_PLANNING_CONSEQUENCE)
        assertThat(line).contains("no longer follows")
        assertThat(line).doesNotContain("Follows your waking hours —")
    }

    @Test
    fun theOverrideCanBeHandedBackToTheDerivation() {
        setContent(initialSchedule = DaySchedule.DEFAULT.copy(planningOverrideMinutes = 19 * 60))

        // Scrolled to first: the button sits below the fold on a phone, and a
        // performClick on an off-screen node lands on whatever is at that
        // coordinate instead -- which is how this test passed a click through
        // to nothing and reported the state as unchanged.
        composeRule.onNodeWithText("Follow my waking hours again")
            .performScrollTo()
            .performClick()

        assertThat(textOf(TAG_PLANNING_CONSEQUENCE)).contains("Follows your waking hours")
    }

    // ------------------------------------------------- the account boundary

    @Test
    fun signedIn_theAccountSectionLinksToProfileAndStatesTheBoundary() {
        var opened = false
        setContent(signedIn = true, onOpenProfile = { opened = true })

        composeRule.onNodeWithTag(TAG_PROFILE_ROW).performScrollTo().assertIsDisplayed()
        assertThat(textOf(TAG_ACCOUNT_CONSEQUENCE)).contains("Nothing on this screen does")

        composeRule.onNodeWithTag(TAG_PROFILE_ROW).performClick()
        assertThat(opened).isTrue()
    }

    /**
     * The signed-out branch — §4.9's proof that Settings is the device.
     *
     * The whole screen must still render, and the boundary must still be
     * *stated*: with no account, the sentence about what leaves with one is the
     * only thing explaining why these settings are still here.
     */
    @Test
    fun signedOut_theScreenStillRendersAndStillStatesTheBoundary() {
        setContent(signedIn = false)

        composeRule.onNodeWithTag(TAG_SCOPE_LINE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_WAKING_CONSEQUENCE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Not signed in").performScrollTo().assertIsDisplayed()
        assertThat(textOf(TAG_ACCOUNT_CONSEQUENCE)).contains("Nothing on this screen does")
    }
}
