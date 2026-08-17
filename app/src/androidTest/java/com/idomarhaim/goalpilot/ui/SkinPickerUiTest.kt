package com.idomarhaim.goalpilot.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.components.SkinPicker
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the skin chooser. Runs on a device/emulator and
 * needs no Firebase — [SkinPicker] is a stateless component driven by lambdas.
 *
 * The skin's words came off the enum in issue #51's `ui/components/` sweep, so
 * these look them up the way the picker does — through `res/` — rather than
 * through `AppSkin.label`, which no longer exists. Reading the same resource the
 * composable reads keeps the test honest about *which* language is on screen:
 * `skinName` resolves against the instrumentation context, so when the device is
 * Hebrew the expectation is Hebrew too, and a regression to a hardcoded
 * "Aurora" fails instead of quietly matching.
 */
class SkinPickerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun skinName(skin: AppSkin): String = context.getString(
        when (skin) {
            AppSkin.AURORA -> R.string.components_skin_aurora
            AppSkin.BLOSSOM -> R.string.components_skin_blossom
        },
    )

    private fun skinTagline(skin: AppSkin): String = context.getString(
        when (skin) {
            AppSkin.AURORA -> R.string.components_skin_aurora_tagline
            AppSkin.BLOSSOM -> R.string.components_skin_blossom_tagline
        },
    )

    @Test
    fun picker_showsEverySkin() {
        composeRule.setContent {
            GoalPilotTheme {
                SkinPicker(selected = AppSkin.DEFAULT, onSelect = {})
            }
        }

        AppSkin.entries.forEach { skin ->
            composeRule.onNodeWithText(skinName(skin)).assertIsDisplayed()
            composeRule.onNodeWithText(skinTagline(skin)).assertIsDisplayed()
        }
    }

    @Test
    fun picker_marksTheActiveSkinSelected() {
        composeRule.setContent {
            GoalPilotTheme {
                SkinPicker(selected = AppSkin.BLOSSOM, onSelect = {})
            }
        }

        composeRule.onNodeWithText(skinName(AppSkin.BLOSSOM)).assertIsSelected()
        composeRule.onNodeWithText(skinName(AppSkin.AURORA)).assertIsNotSelected()
    }

    @Test
    fun picker_reportsAndReflectsASkinChange() {
        var chosen: AppSkin? = null
        composeRule.setContent {
            var selected by remember { mutableStateOf(AppSkin.AURORA) }
            GoalPilotTheme(skin = selected) {
                SkinPicker(
                    selected = selected,
                    onSelect = { chosen = it; selected = it },
                )
            }
        }

        composeRule.onNodeWithText(skinName(AppSkin.BLOSSOM)).performClick()

        assertThat(chosen).isEqualTo(AppSkin.BLOSSOM)
        composeRule.onNodeWithText(skinName(AppSkin.BLOSSOM)).assertIsSelected()
        composeRule.onNodeWithText(skinName(AppSkin.AURORA)).assertIsNotSelected()
    }
}
