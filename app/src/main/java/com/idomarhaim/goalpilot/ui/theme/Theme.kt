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
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin

/** The skin currently in force. Read it to draw skin-aware chrome; set it via preferences. */
val LocalAppSkin = staticCompositionLocalOf { AppSkin.DEFAULT }

/**
 * The material currently in force — spec §4.1's second axis.
 *
 * Read this only to answer *"which material is this?"* for the **one** control
 * allowed to ask: the material picker, which must paint each tile in its own
 * material rather than the current one. Everything else reads
 * [LocalGpMaterial], because a `when (material)` on a screen is the
 * draw-it-four-times cost the contract exists to avoid.
 */
val LocalAppMaterial = staticCompositionLocalOf { AppMaterial.DEFAULT }

/** The four answers — `surface · groove · elevation · accent`. See [GpMaterialSpec]. */
val LocalGpMaterial = staticCompositionLocalOf {
    materialSpecFor(
        material = AppMaterial.DEFAULT,
        scheme = colorSchemeFor(AppSkin.DEFAULT, AppMaterial.DEFAULT, dark = false),
        dark = false,
    )
}

/** Brand colours with no Material 3 role — see [GpAccents]. */
val LocalGpAccents = staticCompositionLocalOf {
    accentsFor(AppSkin.DEFAULT, AppMaterial.DEFAULT, dark = false)
}

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
    material: AppMaterial = AppMaterial.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // §4.1: "a material must be able to declare itself brightness-locked".
    // Resolved through the material rather than beside it, so the theme cannot
    // render a brightness the picker has told the user is impossible.
    val resolvedDark = material.resolveDark(darkTheme)
    // remember-ed on the three inputs, and that is not micro-optimisation:
    // MaterialTheme's LocalColorScheme, LocalGpAccents and LocalGpMaterial are
    // all `staticCompositionLocalOf`, which invalidates the ENTIRE subtree
    // whenever the provided value is a different instance. Recomputing these
    // three on every composition of this function would rebuild the whole app
    // each time -- and the generated schemes below make them more expensive
    // than the hand-authored ones they replaced.
    val colorScheme = remember(skin, material, darkTheme) {
        colorSchemeFor(skin, material, darkTheme)
    }
    val accents = remember(skin, material, darkTheme) {
        accentsFor(skin, material, darkTheme)
    }
    val materialSpec = remember(material, colorScheme, resolvedDark) {
        materialSpecFor(material, colorScheme, resolvedDark)
    }
    val barsOverride = remember { mutableStateOf<Boolean?>(null) }

    CompositionLocalProvider(
        LocalAppSkin provides skin,
        LocalAppMaterial provides material,
        LocalGpMaterial provides materialSpec,
        LocalGpAccents provides accents,
        LocalSystemBarsOverride provides barsOverride,
    ) {
        // A sibling leaf, not an effect in this function's body: it re-reads the
        // override on its own, so a screen flipping the system bars recomposes
        // one empty node instead of the whole app.
        SystemBarIcons(lightBackground = barsOverride.value ?: !resolvedDark)
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

/** The material contract for the active material: `MaterialTheme.gpMaterial.groove`. */
val MaterialTheme.gpMaterial: GpMaterialSpec
    @Composable
    @ReadOnlyComposable
    get() = LocalGpMaterial.current

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
