package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.material3.Icon
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.icon
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the category → Material icon mapping. Guards the
 * `Icons.Filled` → `Icons.AutoMirrored.Filled` migration: an icon key that no
 * longer resolves would silently fall through to the generic flag.
 */
class GoalCategoryIconTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyCategoryMapsToADistinctIcon() {
        val icons = GoalCategory.entries.map { it.icon() }

        assertThat(icons).hasSize(GoalCategory.entries.size)
        assertThat(icons.map { it.name }.toSet()).hasSize(GoalCategory.entries.size)
    }

    @Test
    fun projectCategoryRendersItsAutoMirroredIcon() {
        val projectIcon = GoalCategory.PROJECTS.icon()
        assertThat(projectIcon.name).contains("Assignment")

        composeRule.setContent {
            GoalPilotTheme {
                Icon(projectIcon, contentDescription = "Projects category")
            }
        }

        composeRule.onNodeWithContentDescription("Projects category").assertIsDisplayed()
    }
}
