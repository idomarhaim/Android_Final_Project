package com.idomarhaim.goalpilot.ui

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
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.components.SkinPicker
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the skin chooser. Runs on a device/emulator and
 * needs no Firebase — [SkinPicker] is a stateless component driven by lambdas.
 */
class SkinPickerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun picker_showsEverySkin() {
        composeRule.setContent {
            GoalPilotTheme {
                SkinPicker(selected = AppSkin.DEFAULT, onSelect = {})
            }
        }

        AppSkin.entries.forEach { skin ->
            composeRule.onNodeWithText(skin.label).assertIsDisplayed()
            composeRule.onNodeWithText(skin.tagline).assertIsDisplayed()
        }
    }

    @Test
    fun picker_marksTheActiveSkinSelected() {
        composeRule.setContent {
            GoalPilotTheme {
                SkinPicker(selected = AppSkin.BLOSSOM, onSelect = {})
            }
        }

        composeRule.onNodeWithText(AppSkin.BLOSSOM.label).assertIsSelected()
        composeRule.onNodeWithText(AppSkin.AURORA.label).assertIsNotSelected()
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

        composeRule.onNodeWithText(AppSkin.BLOSSOM.label).performClick()

        assertThat(chosen).isEqualTo(AppSkin.BLOSSOM)
        composeRule.onNodeWithText(AppSkin.BLOSSOM.label).assertIsSelected()
        composeRule.onNodeWithText(AppSkin.AURORA.label).assertIsNotSelected()
    }
}
