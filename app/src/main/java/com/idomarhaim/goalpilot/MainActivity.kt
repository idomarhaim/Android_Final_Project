package com.idomarhaim.goalpilot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.update.AppUpdateChecker
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.notifications.NotificationDeepLink
import com.idomarhaim.goalpilot.ui.locale.AppLocale
import com.idomarhaim.goalpilot.ui.root.GoalPilotRoot
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Field-injected rather than read through a ViewModel: the skin has to be
     * known *outside* [GoalPilotTheme], and every `hiltViewModel()` in this app
     * is created inside it.
     */
    @Inject
    lateinit var appPreferences: AppPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // #8's tap-through. Read here rather than inside composition because the intent is
        // resolved before the nav controller exists -- the holder is a StateFlow so the nav
        // host can pick it up whenever it gets there.
        NotificationDeepLink.offer(intent)

        // Sideloaded builds get nothing from Play, so this is the app's only
        // update path. It lives here rather than in ui/root because the SDK
        // drives its own dialogs off the foreground Activity, outside Compose.
        AppUpdateChecker.checkOnce()

        setContent {
            val skin by appPreferences.skin.collectAsStateWithLifecycle()
            val language by appPreferences.language.collectAsStateWithLifecycle()
            val brightness by appPreferences.brightness.collectAsStateWithLifecycle()
            val material by appPreferences.material.collectAsStateWithLifecycle()
            val background by appPreferences.background.collectAsStateWithLifecycle()
            val relief by appPreferences.relief.collectAsStateWithLifecycle()

            // Outside the theme, because it redirects every `stringResource`
            // below it and sets the layout direction the theme's own surfaces
            // are measured in (spec §5.1).
            AppLocale(language = language) {
                // Read here rather than inside GoalPilotTheme's default
                // argument: the app's own setting is the authority now, and the
                // device only answers when the setting says SYSTEM.
                // The material is passed in beside the skin rather than being
                // read inside the theme: §4.1 makes it the SECOND axis, and the
                // brightness the window actually renders in is the material's
                // answer, not the setting's -- dark neo has no light scheme.
                GoalPilotTheme(
                    skin = skin,
                    material = material,
                    background = background,
                    relief = relief,
                    darkTheme = brightness.isDark(isSystemInDarkTheme()),
                ) {
                    // gpPage, not a flat colour: glass and liquid glass are
                    // translucent panels, and a translucent panel over a flat
                    // ground is not translucent -- it is grey.
                    //
                    // WHICH ground is no longer the material's answer (#57 b):
                    // `background` is a third axis the user sets, and MATCH --
                    // the default -- is what reproduces the per-material
                    // grounds this comment used to describe. Nothing else here
                    // changes, because the answer arrives inside the spec.
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .gpPage(
                                    spec = MaterialTheme.gpMaterial,
                                    background = MaterialTheme.colorScheme.background,
                                ),
                        ) {
                            GoalPilotRoot()
                        }
                    }
                }
            }
        }
    }

    /**
     * The activity is `singleTop`, so a notification tapped while the app is already running
     * arrives here and never through `onCreate`. An app that handled only `onCreate` would
     * route the first tap of a session and silently ignore every one after it.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationDeepLink.offer(intent)
    }
}
