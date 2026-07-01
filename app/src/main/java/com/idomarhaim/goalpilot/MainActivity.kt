package com.idomarhaim.goalpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.idomarhaim.goalpilot.ui.root.GoalPilotRoot
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GoalPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GoalPilotRoot()
                }
            }
        }
    }
}
