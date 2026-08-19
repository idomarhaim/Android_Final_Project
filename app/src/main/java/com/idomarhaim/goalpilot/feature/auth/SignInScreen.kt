package com.idomarhaim.goalpilot.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.ui.theme.BrandSystemBars
import com.idomarhaim.goalpilot.ui.theme.gpAccents

/**
 * @param onOpenSettings opens §4.9's Settings screen **with no account**.
 *
 * ⚠️ **This parameter is the ticket, not a convenience.** §4.9's claim is that
 * *Profile is the account, Settings is the device*, and the only thing that
 * demonstrates it is a device setting a signed-out user can actually reach.
 * §5.1 stores language per-device precisely because *the account is not known
 * until Auth resolves* — so a language control unreachable before sign-in is
 * unreachable exactly when its own justification says it is needed.
 */
@Composable
fun SignInScreen(
    onOpenSettings: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = MaterialTheme.gpAccents

    // This screen is the one place the brand gradient fills the whole window.
    BrandSystemBars()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result -> viewModel.onSignInResult(result.data) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The full brand sweep, not primary→primaryContainer: the old pair
            // put two tones of the same hue next to each other, which on a phone
            // screen just looked like a flat fill with a seam.
            .background(
                Brush.linearGradient(
                    colors = accents.heroGradient,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 1400f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = accents.onHero,
                )
            }
            Text(
                text = "GoalPilot",
                style = MaterialTheme.typography.displaySmall,
                color = accents.onHero,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Pilot your life goals — track, get AI guidance, and stay motivated with friends.",
                style = MaterialTheme.typography.bodyLarge,
                color = accents.onHeroVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
            )

            Spacer(Modifier.height(44.dp))

            Button(
                onClick = {
                    viewModel.onSignInLaunched()
                    launcher.launch(viewModel.signInIntent())
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(vertical = 16.dp),
                // A tonal button on a saturated gradient disappears into it. White
                // is the one fill guaranteed to read against every skin's sweep.
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = accents.heroGradient.first(),
                    disabledContainerColor = Color.White.copy(alpha = 0.7f),
                    disabledContentColor = accents.heroGradient.first().copy(alpha = 0.7f),
                ),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accents.heroGradient.first(),
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Text(
                        text = "Sign in with Google",
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accents.onHero,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        // A word beside the icon, not a bare gear: §0.8's surviving sub-rule is
        // form and words before iconography, and this is the only control on
        // the screen that is not the sign-in button.
        TextButton(
            onClick = onOpenSettings,
            // This screen is outside ui/root's Scaffold, so it insets itself --
            // without this the row renders under the status bar (AGENTS.md).
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(top = 4.dp, end = 4.dp)
                .testTag(TAG_SIGN_IN_SETTINGS),
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null,
                tint = accents.onHero,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Settings",
                color = accents.onHero,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** So the instrumented test can find the one door #48 exists to prove. */
const val TAG_SIGN_IN_SETTINGS = "sign_in_settings"
