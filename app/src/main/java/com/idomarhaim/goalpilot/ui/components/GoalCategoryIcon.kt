package com.idomarhaim.goalpilot.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.idomarhaim.goalpilot.domain.model.GoalCategory

/** Maps a [GoalCategory]'s icon key to a Material icon (keeps Compose out of the domain layer). */
fun GoalCategory.icon(): ImageVector = when (iconKey) {
    "favorite" -> Icons.Filled.Favorite
    "fitness" -> Icons.Filled.FitnessCenter
    "sleep" -> Icons.Filled.Bedtime
    "nutrition" -> Icons.Filled.Restaurant
    "people" -> Icons.Filled.People
    "work" -> Icons.Filled.Work
    "project" -> Icons.AutoMirrored.Filled.Assignment
    "school" -> Icons.Filled.School
    "finance" -> Icons.Filled.Savings
    else -> Icons.Filled.Flag
}
