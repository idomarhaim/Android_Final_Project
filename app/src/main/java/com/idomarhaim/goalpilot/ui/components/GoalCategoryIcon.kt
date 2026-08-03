package com.idomarhaim.goalpilot.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.LifeArea

/**
 * Maps an icon key to a Material icon — the one place Compose types meet the
 * domain layer's plain-string [GoalCategory.iconKey] / [LifeArea.iconKey].
 *
 * Life areas share the table with categories because a user who picks "gym" for
 * their own area expects the same dumbbell the Fitness category shows; keeping two
 * tables would let them drift.
 */
fun iconForKey(iconKey: String): ImageVector = when (iconKey) {
    "favorite" -> Icons.Filled.Favorite
    "fitness" -> Icons.Filled.FitnessCenter
    "sleep" -> Icons.Filled.Bedtime
    "nutrition" -> Icons.Filled.Restaurant
    "people" -> Icons.Filled.People
    "work" -> Icons.Filled.Work
    "project" -> Icons.AutoMirrored.Filled.Assignment
    "school" -> Icons.Filled.School
    "finance" -> Icons.Filled.Savings
    "home" -> Icons.Filled.Home
    "spa" -> Icons.Filled.Spa
    else -> Icons.Filled.Flag
}

fun GoalCategory.icon(): ImageVector = iconForKey(iconKey)

fun LifeArea.icon(): ImageVector = iconForKey(iconKey)
