package com.idomarhaim.goalpilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.SpaceDashboard
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
    const val LIFE_AREAS = "life_areas"

    /**
     * Spec 4.3's calendar surface, on the tab 4.2 freed
     * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
     *
     * A top-level tab and not a screen inside Goals, because 4.2 gives it a job of its own:
     * "Home answers *what needs me?* ... Calendar answers *when?*". The three-way A/B/C choice
     * the prototype opened with was a false fork -- they were three zoom levels of one thing --
     * and collapsing them is what left exactly two surfaces to navigate between.
     */
    const val CALENDAR = "calendar"

    /**
     * Spec §4.9's Settings surface. Registered in **both** graphs: the
     * signed-in one below the tabs, and the signed-out one beside the sign-in
     * screen — reachability with no account is what proves Profile is the
     * account and Settings is the device.
     */
    const val SETTINGS = "settings"

    const val ARG_GOAL_ID = "goalId"
    const val GOAL_DETAIL = "goal_detail"
    const val ADD_EDIT_GOAL = "add_edit_goal"

    /**
     * One life area's own screen: the goals filed under it (spec §4.7). The id
     * travels in the path rather than as a query argument because the screen is
     * meaningless without it — a nullable argument would compile a
     * "life area with no area" state into every caller.
     */
    const val ARG_LIFE_AREA_ID = "lifeAreaId"
    const val LIFE_AREA_DETAIL = "life_area_detail"

    fun lifeAreaDetail(lifeAreaId: String) = "$LIFE_AREA_DETAIL/$lifeAreaId"

    fun goalDetail(goalId: String) = "$GOAL_DETAIL/$goalId"
    fun addEditGoal(goalId: String? = null) =
        if (goalId.isNullOrBlank()) ADD_EDIT_GOAL else "$ADD_EDIT_GOAL?$ARG_GOAL_ID=$goalId"
}

/**
 * Bottom navigation tabs shown in the signed-in area.
 *
 * Each tab carries both weights: outlined when idle, filled when selected. That
 * pairing is what makes the active tab obvious at a glance — the pill indicator
 * alone is easy to miss on a small screen.
 *
 * ### Four tabs, and Profile is deliberately not one of them (spec §4.2, `#60`)
 *
 * §4.2: *"Five is a crowded bar, so **Profile moves to an avatar in Home's top-right**
 * — what Gmail, YouTube and Google Calendar all do — and Calendar takes the freed
 * tab."* The avatar half shipped first: [Routes.DASHBOARD]'s top bar already opens a
 * sheet with *Your profile* and *Settings*, so — measured, not assumed — swapping the
 * tab removed **no** route and left Profile reachable in one tap from Home.
 *
 * ⚠️ **[Routes.PROFILE] is still registered in the graph and must stay.** `ProfileScreen`
 * is reached from that avatar sheet; deleting the route because the tab went would break
 * the thing that made the swap safe.
 */
enum class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, "Home", Icons.Filled.SpaceDashboard, Icons.Outlined.SpaceDashboard),
    GOALS(Routes.GOALS, "Goals", Icons.Filled.Flag, Icons.Outlined.Flag),
    CALENDAR(Routes.CALENDAR, "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    SOCIAL(Routes.SOCIAL, "Social", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
}
