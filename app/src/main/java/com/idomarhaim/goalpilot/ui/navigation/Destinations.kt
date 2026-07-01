package com.idomarhaim.goalpilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes + argument keys in one place. */
object Routes {
    const val SIGN_IN = "sign_in"
    const val DASHBOARD = "dashboard"
    const val GOALS = "goals"
    const val SOCIAL = "social"
    const val PROFILE = "profile"
    const val CHALLENGES = "challenges"
    const val ANALYTICS = "analytics"

    const val ARG_GOAL_ID = "goalId"
    const val GOAL_DETAIL = "goal_detail"
    const val ADD_EDIT_GOAL = "add_edit_goal"

    fun goalDetail(goalId: String) = "$GOAL_DETAIL/$goalId"
    fun addEditGoal(goalId: String? = null) =
        if (goalId.isNullOrBlank()) ADD_EDIT_GOAL else "$ADD_EDIT_GOAL?$ARG_GOAL_ID=$goalId"
}

/** Bottom navigation tabs shown in the signed-in area. */
enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD(Routes.DASHBOARD, "Home", Icons.Filled.SpaceDashboard),
    GOALS(Routes.GOALS, "Goals", Icons.Filled.Flag),
    SOCIAL(Routes.SOCIAL, "Social", Icons.Filled.EmojiEvents),
    PROFILE(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
