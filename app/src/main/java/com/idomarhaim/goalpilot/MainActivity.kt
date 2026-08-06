package com.idomarhaim.goalpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.core.update.AppUpdateChecker
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.ui.root.GoalPilotRoot
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
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

        // Sideloaded builds get nothing from Play, so this is the app's only
        // update path. It lives here rather than in ui/root because the SDK
        // drives its own dialogs off the foreground Activity, outside Compose.
        AppUpdateChecker.checkOnce()

        setContent {
            val skin by appPreferences.skin.collectAsStateWithLifecycle()
            GoalPilotTheme(skin = skin) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GoalPilotRoot()
                }
            }
        }
    }
}
