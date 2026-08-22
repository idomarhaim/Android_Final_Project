package com.idomarhaim.goalpilot.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.feature.settings.SettingsContent
import com.idomarhaim.goalpilot.feature.settings.TAG_BRIGHTNESS_LOCK
import com.idomarhaim.goalpilot.feature.settings.TAG_MATERIAL_CONSEQUENCE
import com.idomarhaim.goalpilot.feature.settings.TAG_MATERIAL_PICKER
import com.idomarhaim.goalpilot.ui.components.MaterialPicker
import com.idomarhaim.goalpilot.ui.components.materialLockTag
import com.idomarhaim.goalpilot.ui.components.materialSpecTag
import com.idomarhaim.goalpilot.ui.locale.AppLocale
import com.idomarhaim.goalpilot.ui.components.materialTileTag
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * Spec §4.9's material picker, on a device — `C12` #53.
 *
 * ## What is asserted here and what deliberately is not
 *
 * **Not** whether the four materials *look* right. Four materials × two
 * brightnesses × two skins is a visual matrix, and the brief for this unit says
 * so plainly: *"This cannot be verified by tests… Render them and look."* The
 * render pass is the changelog's, and it is not replaceable by anything below.
 *
 * What a device test *can* answer is the half that is structural and silent —
 * the disclosures §4.1 and §4.9 require, each of which reads correctly in a
 * screenshot even when it has stopped tracking the thing it describes:
 *
 * - every material is offered, and picking one is reported back;
 * - the brightness **lock is a word**, on the tile and under the control;
 * - the struck-through control is genuinely inert rather than merely styled;
 * - the stored brightness **survives** the lock, which is the sentence the
 *   consequence line makes and the only claim here a user could be misled by.
 *
 * Reads the words through `res/` like `SkinPickerUiTest` does, so a Hebrew
 * device asserts Hebrew rather than quietly matching a hardcoded English
 * string.
 */
class MaterialPickerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun materialName(material: AppMaterial): String = context.getString(
        when (material) {
            AppMaterial.GLASS -> R.string.components_material_glass
            AppMaterial.LIQUID_GLASS -> R.string.components_material_liquid
            AppMaterial.NEO -> R.string.components_material_neo
            AppMaterial.DARK_NEO -> R.string.components_material_darkneo
        },
    )

    private val darkOnly: String get() = context.getString(R.string.components_material_dark_only)

    /**
     * The caption a tile is supposed to render — §4.1's own name for the
     * material, inside the translated frame, with the same isolate the call site
     * adds. Built through `res/` for the same reason [materialName] is, and
     * assembled the same way `ComponentStrings.specName` assembles it: an
     * expectation spelled without the isolate marks passes on output that never
     * had them, which is the defect (`ComponentsLocaleTest` pinned that shape).
     */
    private fun specName(material: AppMaterial): String = context.getString(
        R.string.components_material_spec_name,
        context.getString(
            when (material) {
                AppMaterial.GLASS -> R.string.components_material_glass_spec
                AppMaterial.LIQUID_GLASS -> R.string.components_material_liquid_spec
                AppMaterial.NEO -> R.string.components_material_neo_spec
                AppMaterial.DARK_NEO -> R.string.components_material_darkneo_spec
            },
        ).bidiIsolated(),
    )

    /** §4.1's word alone, without the frame — what must be identical in both locales. */
    private fun specNameOnly(material: AppMaterial): String = context.getString(
        when (material) {
            AppMaterial.GLASS -> R.string.components_material_glass_spec
            AppMaterial.LIQUID_GLASS -> R.string.components_material_liquid_spec
            AppMaterial.NEO -> R.string.components_material_neo_spec
            AppMaterial.DARK_NEO -> R.string.components_material_darkneo_spec
        },
    )

    // ------------------------------------------------------ the picker alone

    @Test
    fun picker_offersEveryMaterial() {
        composeRule.setContent {
            GoalPilotTheme {
                MaterialPicker(
                    selected = AppMaterial.DEFAULT,
                    skin = AppSkin.DEFAULT,
                    brightnessIsDark = false,
                    // #57 b made the ground a third axis. MATCH is the default
                    // and reproduces the per-material grounds these tiles were
                    // written against, so every assertion below is unchanged.
                    background = AppBackground.DEFAULT,
                    onSelect = {},
                )
            }
        }
        AppMaterial.entries.forEach { material ->
            composeRule.onNodeWithTag(materialTileTag(material)).assertIsDisplayed()
            composeRule.onNodeWithText(materialName(material)).assertIsDisplayed()
        }
    }

    @Test
    fun picker_reportsTheChoiceAndMarksItSelected() {
        var chosen: AppMaterial? = null
        composeRule.setContent {
            var selected by remember { mutableStateOf(AppMaterial.NEO) }
            GoalPilotTheme(material = selected) {
                MaterialPicker(
                    selected = selected,
                    skin = AppSkin.DEFAULT,
                    brightnessIsDark = false,
                    background = AppBackground.DEFAULT,
                    onSelect = {
                        chosen = it
                        selected = it
                    },
                )
            }
        }

        composeRule.onNodeWithTag(materialTileTag(AppMaterial.NEO)).assertIsSelected()
        composeRule.onNodeWithTag(materialTileTag(AppMaterial.GLASS)).assertIsNotSelected()

        composeRule.onNodeWithTag(materialTileTag(AppMaterial.GLASS)).performClick()

        assertThat(chosen).isEqualTo(AppMaterial.GLASS)
        composeRule.onNodeWithTag(materialTileTag(AppMaterial.GLASS)).assertIsSelected()
        composeRule.onNodeWithTag(materialTileTag(AppMaterial.NEO)).assertIsNotSelected()
    }

    @Test
    fun everyTileNamesItselfInTheSpecsVocabulary() {
        // `#53`, 2026-08-21: §4.1 calls these materials neo and dark neo, the
        // picker calls two of them Soft and Soft dark, and the word "neo"
        // appeared nowhere in the UI -- so a user who had read the spec could
        // not find the control and a bug report could not be matched to a tile.
        //
        // Asserted on the TILE's merged text rather than by tag, for the reason
        // the lock test below records: a tile is `Modifier.selectable`, so its
        // descendants are merged and the caption is not a separate node in the
        // merged tree. That form is the better claim anyway -- it says dark
        // neo's OWN tile carries "Dark neo", which a swapped pair would fail and
        // a screen-wide search would not.
        composeRule.setContent {
            GoalPilotTheme {
                MaterialPicker(
                    selected = AppMaterial.DEFAULT,
                    skin = AppSkin.DEFAULT,
                    brightnessIsDark = false,
                    background = AppBackground.DEFAULT,
                    onSelect = {},
                )
            }
        }
        AppMaterial.entries.forEach { material ->
            assertWithMessage("${material.name} must carry §4.1's own name for it")
                .that(tileTexts(material)).contains(specName(material))
            // And it is reachable by tag through the unmerged tree, so a later
            // test can point at the caption itself.
            composeRule.onNodeWithTag(materialSpecTag(material), useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun theSpecNameIsVisible_notOnlySpoken() {
        // The half a contentDescription would not fix. #53's failure is that a
        // reader of the spec cannot FIND the control; a description nobody sees
        // answers only the screen-reader case. `assertIsDisplayed` on the
        // caption node is what separates the two.
        composeRule.setContent {
            GoalPilotTheme {
                MaterialPicker(
                    selected = AppMaterial.DEFAULT,
                    skin = AppSkin.DEFAULT,
                    brightnessIsDark = false,
                    background = AppBackground.DEFAULT,
                    onSelect = {},
                )
            }
        }
        AppMaterial.entries.forEach { material ->
            composeRule.onNodeWithTag(materialSpecTag(material), useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun theSpecNameSurvivesTheLanguageSwitch_andItsLatinRunIsIsolated() {
        // The other locale, and the one decision in this unit that could only be
        // wrong there. The NAME is translatable="false" on purpose -- its job is
        // to be the same token as the design of record, which is English, so a
        // Hebrew rendering would name the control after a word appearing in no
        // document. The FRAME around it is translated.
        //
        // That leaves a Latin run inside an RTL paragraph, which the bidi
        // algorithm reorders exactly as it reorders `5/10` into `10/5` -- so the
        // call site isolates it, and the isolate is asserted here rather than in
        // the resource, because the resource cannot carry it (the same argument
        // ComponentsLocaleTest.theMeasureRunIsIsolated makes for the measure).
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                GoalPilotTheme {
                    MaterialPicker(
                        selected = AppMaterial.DEFAULT,
                        skin = AppSkin.DEFAULT,
                        brightnessIsDark = false,
                        background = AppBackground.DEFAULT,
                        onSelect = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        AppMaterial.entries.forEach { material ->
            val texts = tileTexts(material)
            val caption = texts.firstOrNull { it.contains(Bidi.FSI) }
            assertWithMessage("${material.name} rendered no isolated caption at all")
                .that(caption).isNotNull()
            // The spec's own word, unchanged by the language switch...
            assertWithMessage("${material.name}: §4.1's word must survive into Hebrew")
                .that(caption).contains("${Bidi.FSI}${specNameOnly(material)}${Bidi.PDI}")
            // ...and the frame around it must NOT be the English one.
            assertWithMessage("${material.name}: the frame is language-dependent and must translate")
                .that(HEBREW.containsMatchIn(caption.orEmpty())).isTrue()
        }
        // And the label itself still translates -- otherwise this unit could
        // have "fixed" the naming gap by freezing the whole tile in English.
        assertWithMessage("the plain-English label is still the translated one")
            .that(tileTexts(AppMaterial.NEO).none { it == "Soft" }).isTrue()
    }

    @Test
    fun theLockIsAWordOnTheTile_andOnlyOnTheLockedOne() {
        // §4.9: "A lock is a word, never a dimming." A dimmed tile renders
        // identically whether the lock is wired to the material or hardcoded;
        // the word is the part that cannot be faked by opacity.
        composeRule.setContent {
            GoalPilotTheme {
                MaterialPicker(
                    selected = AppMaterial.DEFAULT,
                    skin = AppSkin.DEFAULT,
                    brightnessIsDark = false,
                    // #57 b made the ground a third axis. MATCH is the default
                    // and reproduces the per-material grounds these tiles were
                    // written against, so every assertion below is unchanged.
                    background = AppBackground.DEFAULT,
                    onSelect = {},
                )
            }
        }
        AppMaterial.entries.forEach { material ->
            // Asserted on the TILE's own merged text, not on the badge's tag.
            // A tile is `Modifier.selectable`, which merges its descendants'
            // semantics, so the badge is not a separate node in the merged tree
            // -- and this form is the better assertion anyway: it says the word
            // is on the *dark neo* tile rather than somewhere on the screen.
            // `Observed:` 2026-08-20, the tag form failed with "The component is
            // not displayed!" while the word was rendering perfectly.
            val texts = tileTexts(material)
            if (material.isBrightnessLocked) {
                assertWithMessage("${material.name} must carry the lock word")
                    .that(texts).contains(darkOnly)
                // And the badge is still reachable by tag for anything that wants
                // it -- through the unmerged tree, which is where it lives.
                composeRule.onNodeWithTag(materialLockTag(material), useUnmergedTree = true)
                    .assertExists()
            } else {
                assertWithMessage("${material.name} must not carry it")
                    .that(texts).doesNotContain(darkOnly)
                assertThat(nodeCount(materialLockTag(material))).isEqualTo(0)
            }
        }
    }

    // --------------------------------------------- the lock, on the screen

    @Test
    fun choosingTheLockedMaterial_captionsTheBrightnessControl() {
        setSettings(initialMaterial = AppMaterial.NEO)

        // Unlocked: no caption at all. An always-present caption would read the
        // same in a screenshot and prove nothing.
        assertThat(nodeCount(TAG_BRIGHTNESS_LOCK)).isEqualTo(0)

        composeRule.onNodeWithTag(materialTileTag(AppMaterial.DARK_NEO))
            .performScrollTo()
            .performClick()

        // performScrollTo before the display assertion, added by #57 c. The
        // caption used to be on screen after the tile click; the Appearance card
        // gained a FIFTH control (Chart relief) between Background and
        // Brightness, so it now sits below the fold. The claim under test is
        // "choosing the locked material captions the brightness control" -- not
        // "the caption fits on one screen" -- and a scroll keeps the first while
        // dropping only the second, which was never asserted on purpose.
        composeRule.onNodeWithTag(TAG_BRIGHTNESS_LOCK).performScrollTo().assertIsDisplayed()
        val caption = textOf(TAG_BRIGHTNESS_LOCK)
        assertThat(caption).contains(materialName(AppMaterial.DARK_NEO))
        // The claim a user could be misled by: the setting is suspended, not
        // overwritten. If the lock ever starts writing DARK into preferences,
        // this sentence becomes a lie and nothing else in the suite notices.
        assertThat(caption).contains("remembered")
    }

    @Test
    fun theLockedBrightnessControlIsInert_andTheStoredValueSurvives() {
        setSettings(initialMaterial = AppMaterial.DARK_NEO)

        // SYSTEM is the default and the segment that would silently do nothing.
        composeRule.onNodeWithText("Light").performScrollTo().performClick()

        // Still SYSTEM: the click did not land, and nothing wrote DARK either.
        assertThat(currentBrightness).isEqualTo(AppBrightness.SYSTEM)
    }

    @Test
    fun theMaterialConsequenceLineNamesTheLockBeforeItIsChosen() {
        // §4.9's table: Material's consequence line states "that dark neo is
        // brightness-locked". A line that only appeared once dark neo was
        // already selected would be a report, not a consequence.
        setSettings(initialMaterial = AppMaterial.NEO)
        val before = textOf(TAG_MATERIAL_CONSEQUENCE)
        assertThat(before).contains(materialName(AppMaterial.DARK_NEO))

        composeRule.onNodeWithTag(materialTileTag(AppMaterial.DARK_NEO))
            .performScrollTo()
            .performClick()

        // And it moves when the setting does -- the whole claim of a live line.
        assertThat(textOf(TAG_MATERIAL_CONSEQUENCE)).isNotEqualTo(before)
    }

    @Test
    fun thePickerIsOnTheAppearanceSection() {
        setSettings()
        composeRule.onNodeWithTag(TAG_MATERIAL_PICKER).performScrollTo().assertIsDisplayed()
    }

    // ---------------------------------------------------------------- harness

    private var currentBrightness: AppBrightness = AppBrightness.DEFAULT

    /** The whole §4.9 screen with real state behind it, as `SettingsScreenTest` drives it. */
    private fun setSettings(initialMaterial: AppMaterial = AppMaterial.DEFAULT) {
        composeRule.setContent {
            var skin by remember { mutableStateOf(AppSkin.DEFAULT) }
            var brightness by remember { mutableStateOf(AppBrightness.DEFAULT) }
            var material by remember { mutableStateOf(initialMaterial) }
            var region by remember { mutableStateOf(AppRegion.SYSTEM) }
            var schedule by remember { mutableStateOf(DaySchedule.DEFAULT) }
            var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
            currentBrightness = brightness

            GoalPilotTheme(skin = skin, material = material) {
                SettingsContent(
                    skin = skin,
                    onSkin = { skin = it },
                    brightness = brightness,
                    onBrightness = { brightness = it },
                    material = material,
                    onMaterial = { material = it },
                    // #57 b's third axis. Explicit rather than defaulted, for the
                    // same reason the AI state is: a default lets a real screen
                    // forget the control and render one that silently does nothing.
                    background = AppBackground.DEFAULT,
                    onBackground = {},
                    // #57 c's fourth axis. Explicit, like the third: a default
                    // lets a real screen forget the control and render one that
                    // silently does nothing. This suite is about the MATERIAL
                    // picker, so the relief is held at its own default.
                    relief = AppRelief.DEFAULT,
                    onRelief = {},
                    language = language,
                    onLanguage = { language = it },
                    region = region,
                    onRegion = { region = it },
                    schedule = schedule,
                    onWakingHours = { schedule = schedule.copy(waking = it) },
                    onPlanningOverrideMinutes = {
                        schedule = schedule.copy(planningOverrideMinutes = it)
                    },
                    // C13 (#54) added §4.9's fifth section. Passed explicitly
                    // rather than defaulted: a default would let a real screen
                    // forget them and render an AI section that silently does
                    // nothing, which is the one thing that section must not be.
                    // This file is about the MATERIAL contract, so the AI state
                    // is the app's default — no key, nothing asked yet.
                    aiCredential = null,
                    aiLastAnswer = null,
                    onAiCredential = {},
                    onClearAiCredential = {},
                    onBack = {},
                    onOpenProfile = {},
                    onReplayTutorial = {},
                )
            }
        }
    }

    /**
     * How many nodes carry [tag]. **Zero is the assertion here, not an error** —
     * `onNodeWithTag(...).assertDoesNotExist()` would do, but a count reads the
     * same way in both directions and says which case failed.
     */
    private fun nodeCount(tag: String): Int =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    /** Every string a tile renders, read off its merged semantics node. */
    private fun tileTexts(material: AppMaterial): List<String> =
        composeRule.onNodeWithTag(materialTileTag(material))
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .map { it.text }

    private fun textOf(tag: String): String =
        composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config
            .first { it.key.name == "Text" }
            .value
            .toString()

    private companion object {
        val HEBREW = Regex("""\p{IsHebrew}""")
    }
}
