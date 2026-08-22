package com.idomarhaim.goalpilot.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
import com.idomarhaim.goalpilot.ui.tutorial.LocalTutorialAnchors
import com.idomarhaim.goalpilot.ui.tutorial.TutorialAnchor
import com.idomarhaim.goalpilot.ui.tutorial.TutorialAnchors
import com.idomarhaim.goalpilot.ui.tutorial.TutorialHost
import com.idomarhaim.goalpilot.ui.tutorial.TutorialViewModel
import com.idomarhaim.goalpilot.ui.tutorial.tutorialAnchor

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
                // The tour walks the signed-in app -- its very first step points
                // at the dashboard's hero card, which does not exist here. A
                // Replay control on this branch would be a button that navigates
                // nowhere, so §4.9's own rule applies: the honest answer is not
                // to draw the row, not to draw it dimmed.
                onReplayTutorial = null,
            )
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // The guided tour's two halves, both hoisted here because both outlive the
    // screen: the registry is what lets a widget six layers down report where it
    // is, and the view model is reached by Settings' Replay control as well as
    // by the overlay itself. `hiltViewModel()` here resolves against the ACTIVITY
    // rather than a nav entry, which is the lifetime the tour needs -- it walks
    // the user across two destinations and must not reset when it does.
    val anchors = remember { TutorialAnchors() }
    val tutorial: TutorialViewModel = hiltViewModel()

    // #8's tap-through, consumed here because this is the first point in the tree that holds a
    // NavController. The route is taken (not just read), so a configuration change cannot
    // re-navigate to a notification the user has already followed and walked away from.
    val pendingRoute by NotificationDeepLink.pendingRoute.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute) {
        NotificationDeepLink.consume()?.let { navController.navigate(it) }
    }
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelTab.entries.any { it.route == currentRoute }

    // One provider for the whole signed-in app: every screen below can tag its
    // own widgets, and nothing below has to be given a tutorial parameter.
    CompositionLocalProvider(LocalTutorialAnchors provides anchors) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (showBottomBar) {
                        // Same fill as a card, so the bar reads as chrome lifted off the
                        // tinted canvas rather than another band of background.
                        NavigationBar(
                            modifier = Modifier.tutorialAnchor(TutorialAnchor.NAV_BAR),
                            containerColor = gpCardContainerColor(),
                            tonalElevation = 0.dp,
                        ) {
                            TopLevelTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    // The tour's one interactive step spotlights THIS
                                    // item and waits for it to be tapped, so it needs a
                                    // hole of its own inside the bar's.
                                    modifier = if (tab == TopLevelTab.GOALS) {
                                        Modifier.tutorialAnchor(TutorialAnchor.TAB_GOALS)
                                    } else {
                                        Modifier
                                    },
                                    selected = selected,
                                    onClick = { navController.navigateToTab(tab.route) },
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
                            // Replaying from here leaves the user on Settings, which is
                            // not where step one lives -- the host navigates to the
                            // step's own route, so the tour opens on the dashboard and
                            // this screen is popped by the same tab move the bar makes.
                            onReplayTutorial = tutorial::restart,
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

            // A sibling ABOVE the scaffold, not content inside it. Two of the
            // seven steps point at the bottom bar, and an overlay inside the
            // scaffold's content slot would stop at the bar's top edge -- so the
            // one thing being described would be the one thing left undimmed.
            TutorialHost(
                anchors = anchors,
                currentRoute = currentRoute,
                onNavigate = navController::navigateForTutorial,
                onCreateGoal = { navController.navigate(Routes.addEditGoal()) },
                viewModel = tutorial,
            )
        }
    }
}

/**
 * Switch to a top-level destination, the way the bottom bar does.
 *
 * `saveState`/`restoreState` are the pair that makes a tab remember its scroll
 * position, and they are why this is **not** what the guided tour calls — see
 * [navigateForTutorial].
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Put a route on screen for the guided tour.
 *
 * ⚠️ **This is deliberately NOT [navigateToTab], and the difference is a silent
 * no-op that reached the emulator.** `Observed:` 2026-08-22 — replaying the tour
 * from Settings logged `navigating to dashboard`, called `navigate()` without
 * throwing, and left the app on **Settings**, with the tour running over it. The
 * same call from the Goals tab worked, which is what makes it worth a second
 * function instead of a fix: it fails on exactly one of the two screens the tour
 * navigates from, so a session that tested the tour by walking it forwards would
 * never see it.
 *
 * `Observed:` the `saveState`/`restoreState` pair is what does it — dropping it
 * is the entire change, and the same walk then reports `route=dashboard` where
 * it reported `route=settings` before. `Inferred:` the mechanism, from
 * `NavController`'s shape rather than from reading its source — `navigate` takes
 * a *restore* branch when a saved-state entry exists for the destination, and a
 * branch that finds nothing to restore does not fall through to an ordinary
 * navigate. `Untested:` that reading; what is tested is the behaviour, in
 * `TutorialNavigationUiTest`, which drives this function from a non-tab
 * destination on a real `NavHost`.
 *
 * The bottom bar keeps that pair, because remembering a tab's scroll position is
 * exactly what it is for. The tour drops it: it is not switching tabs, it is
 * putting one screen in front of the user, and it has nothing to remember.
 */
internal fun NavHostController.navigateForTutorial(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}
