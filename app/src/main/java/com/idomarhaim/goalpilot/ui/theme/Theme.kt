package com.idomarhaim.goalpilot.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.idomarhaim.goalpilot.domain.model.AppSkin

/** The skin currently in force. Read it to draw skin-aware chrome; set it via preferences. */
val LocalAppSkin = staticCompositionLocalOf { AppSkin.DEFAULT }

/** Brand colours with no Material 3 role — see [GpAccents]. */
val LocalGpAccents = staticCompositionLocalOf { accentsFor(AppSkin.DEFAULT, darkTheme = false) }

/**
 * `null` = follow the theme's brightness; `false` = the window is filled with a
 * dark/saturated surface, so the system bars need light icons. Screens set this
 * through [BrandSystemBars] rather than touching the window directly.
 */
private val LocalSystemBarsOverride =
    compositionLocalOf<MutableState<Boolean?>> { mutableStateOf(null) }

/**
 * App theme.
 *
 * **No Material You dynamic colour.** It used to be on by default, which meant
 * the device wallpaper — not the app — decided every colour, and the brand
 * palette below was dead code on any Android 12+ device. That is also
 * incompatible with a user-chosen [skin]: two colour authorities cannot both
 * win. The skin is the authority now.
 */
@Composable
fun GoalPilotTheme(
    skin: AppSkin = AppSkin.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = colorSchemeFor(skin, darkTheme)
    val accents = accentsFor(skin, darkTheme)
    val barsOverride = remember { mutableStateOf<Boolean?>(null) }

    CompositionLocalProvider(
        LocalAppSkin provides skin,
        LocalGpAccents provides accents,
        LocalSystemBarsOverride provides barsOverride,
    ) {
        // A sibling leaf, not an effect in this function's body: it re-reads the
        // override on its own, so a screen flipping the system bars recomposes
        // one empty node instead of the whole app.
        SystemBarIcons(lightBackground = barsOverride.value ?: !darkTheme)
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GpShapes,
            content = content,
        )
    }
}

/**
 * Opt a full-bleed brand screen out of the theme's system-bar colouring.
 *
 * The app draws edge-to-edge, so on a saturated gradient the light-mode default
 * puts black status icons on mid-blue. Reverts automatically when the screen
 * leaves the composition.
 */
@Composable
fun BrandSystemBars() {
    val override = LocalSystemBarsOverride.current
    DisposableEffect(Unit) {
        override.value = false
        onDispose { override.value = null }
    }
}

@Composable
private fun SystemBarIcons(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }
}

/** Brand accents for the active skin: `MaterialTheme.gpAccents.heroGradient`. */
val MaterialTheme.gpAccents: GpAccents
    @Composable
    @ReadOnlyComposable
    get() = LocalGpAccents.current

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
