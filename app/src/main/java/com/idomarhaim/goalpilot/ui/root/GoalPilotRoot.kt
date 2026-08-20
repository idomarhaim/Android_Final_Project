package com.idomarhaim.goalpilot.ui.root

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.idomarhaim.goalpilot.feature.analytics.AnalyticsScreen
import com.idomarhaim.goalpilot.feature.auth.SignInScreen
import com.idomarhaim.goalpilot.feature.challenges.ChallengesScreen
import com.idomarhaim.goalpilot.feature.dashboard.DashboardScreen
import com.idomarhaim.goalpilot.feature.goals.AddEditGoalScreen
import com.idomarhaim.goalpilot.feature.goals.GoalDetailScreen
import com.idomarhaim.goalpilot.feature.goals.GoalsScreen
import com.idomarhaim.goalpilot.feature.lifeareas.LifeAreaDetailScreen
import com.idomarhaim.goalpilot.feature.lifeareas.LifeAreasScreen
import com.idomarhaim.goalpilot.feature.profile.ProfileScreen
import com.idomarhaim.goalpilot.feature.settings.SettingsScreen
import com.idomarhaim.goalpilot.feature.social.SocialScreen
import com.idomarhaim.goalpilot.notifications.NotificationDeepLink
import com.idomarhaim.goalpilot.ui.components.LoadingBox
import com.idomarhaim.goalpilot.ui.components.gpCardContainerColor
import com.idomarhaim.goalpilot.ui.navigation.Routes
import com.idomarhaim.goalpilot.ui.navigation.TopLevelTab

/** Top-level entry: routes between the sign-in screen and the signed-in app. */
@Composable
fun GoalPilotRoot(viewModel: RootViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        AuthUiState.Loading -> LoadingBox()
        AuthUiState.SignedOut -> SignedOutGraph()
        is AuthUiState.SignedIn -> {
            // Health Connect syncs whenever the app comes forward. ON_START rather
            // than ON_RESUME because a permission dialog or the app switcher briefly
            // pauses the activity, and resuming from those is not "opening the app".
            // A lifecycle already past STARTED replays the event to a new observer,
            // so this also fires on cold start and immediately after sign-in.
            LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }
            MainScaffold()
        }
    }
}

/**
 * The signed-out area: the sign-in screen, and §4.9's Settings screen beside it.
 *
 * ⚠️ **This graph exists for one destination, and that destination is the point
 * of #48.** §4.9 makes Settings reachable *"from the sign-in screen, with no
 * account at all"*, because that is the only thing that actually proves the
 * split — a Settings screen you can only open once signed in is a second
 * Profile with a different title. §5.1's own justification for storing language
 * per-device is that *it must be known before the first frame, and the account
 * is not known until Auth resolves*; a control locked behind Auth is
 * unreachable exactly when its reason for existing says it is needed.
 *
 * A `NavHost` rather than a hoisted boolean, so the system back gesture returns
 * to sign-in without this file re-implementing a back stack.
 */
@Composable
private fun SignedOutGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SIGN_IN) {
        composable(Routes.SIGN_IN) {
            SignInScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                // No account, so no Profile to open. The Account section states
                // the boundary anyway -- on this branch it is the whole proof.
                onOpenProfile = null,
            )
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // #8's tap-through, consumed here because this is the first point in the tree that holds a
    // NavController. The route is taken (not just read), so a configuration change cannot
    // re-navigate to a notification the user has already followed and walked away from.
    val pendingRoute by NotificationDeepLink.pendingRoute.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute) {
        NotificationDeepLink.consume()?.let { navController.navigate(it) }
    }
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelTab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // Same fill as a card, so the bar reads as chrome lifted off the
                // tinted canvas rather than another band of background.
                NavigationBar(
                    containerColor = gpCardContainerColor(),
                    tonalElevation = 0.dp,
                ) {
                    TopLevelTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(inner),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenGoal = { navController.navigate(Routes.goalDetail(it)) },
                    onAddGoal = { navController.navigate(Routes.addEditGoal()) },
                    onSeeAllGoals = { navController.navigate(Routes.GOALS) },
                    onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.GOALS) {
                GoalsScreen(
                    onOpenGoal = { navController.navigate(Routes.goalDetail(it)) },
                    onAddGoal = { navController.navigate(Routes.addEditGoal()) },
                )
            }
            composable(
                route = "${Routes.GOAL_DETAIL}/{${Routes.ARG_GOAL_ID}}",
                arguments = listOf(navArgument(Routes.ARG_GOAL_ID) { type = NavType.StringType }),
            ) {
                GoalDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { goalId -> navController.navigate(Routes.addEditGoal(goalId)) },
                )
            }
            composable(
                route = "${Routes.ADD_EDIT_GOAL}?${Routes.ARG_GOAL_ID}={${Routes.ARG_GOAL_ID}}",
                arguments = listOf(
                    navArgument(Routes.ARG_GOAL_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                AddEditGoalScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.SOCIAL) {
                SocialScreen(onOpenChallenges = { navController.navigate(Routes.CHALLENGES) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                    onOpenChallenges = { navController.navigate(Routes.CHALLENGES) },
                    onOpenLifeAreas = { navController.navigate(Routes.LIFE_AREAS) },
                )
            }
            composable(Routes.CHALLENGES) {
                ChallengesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLifeAreas = { navController.navigate(Routes.LIFE_AREAS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                )
            }
            composable(Routes.LIFE_AREAS) {
                LifeAreasScreen(
                    onBack = { navController.popBackStack() },
                    onOpenArea = { navController.navigate(Routes.lifeAreaDetail(it)) },
                )
            }
            composable(
                route = "${Routes.LIFE_AREA_DETAIL}/{${Routes.ARG_LIFE_AREA_ID}}",
                arguments = listOf(
                    navArgument(Routes.ARG_LIFE_AREA_ID) { type = NavType.StringType },
                ),
            ) {
                LifeAreaDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenGoal = { navController.navigate(Routes.goalDetail(it)) },
                )
            }
        }
    }
}
