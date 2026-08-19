package com.idomarhaim.goalpilot.ui.locale

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import java.util.Locale

/**
 * Applies spec §5.1's **Language** setting to everything composed inside it, and
 * derives §5.1's **Direction** from it.
 *
 * Three things have to move together, and missing any one produces a
 * half-translated app that looks deliberate:
 *
 * 1. **`LocalContext`** — `stringResource` resolves through
 *    `LocalContext.current.resources`, so a context built on a locale-overridden
 *    [Configuration] is what actually redirects lookups into `res/values-iw/`.
 * 2. **`LocalLayoutDirection`** — Compose does *not* infer direction from the
 *    context. Without this the words become Hebrew and the layout stays
 *    left-to-right, which is the inverse of the defect `widget-pack` found on
 *    the device and reads just as broken.
 * 3. **[Locale.setDefault]** — everything outside the composition
 *    (`DateTimeFormatter`, `NumberFormat`, anything in `domain/`) reads the
 *    process default and cannot see a `CompositionLocal`. Paired with
 *    `AppDateFormatters`, which re-reads the default per call, this is what
 *    makes §5.1's *"all ten date formatters are process-scoped `val`s no switch
 *    can move"* defect actually movable.
 *
 * **Why a configuration context rather than `AppCompatDelegate.setApplicationLocales`:**
 * this app has no `appcompat` dependency (it is `ComponentActivity` + Compose),
 * and adding one to reach a single API is a large dependency for a small need —
 * the platform's own `createConfigurationContext` does the whole job on
 * `minSdk 26` with nothing added. The platform `LocaleManager` alternative is
 * API 33+, so it would leave everything below Android 13 unlocalized.
 *
 * ⚠️ **Overriding `LocalContext` is the dangerous part of this file, and the
 * danger is not in this app's own code.** A grep for `as Activity` /
 * `findActivity()` across `app/src` comes back clean — and that grep is
 * **insufficient**, which was established by the app crashing on its first
 * frame. The consumer that broke was `androidx.hilt`: `hiltViewModel()` walks
 * `LocalContext.current` for an `Activity`, and libraries are not in `app/src`.
 * See [LocalizedContext], which is what makes the override survivable, and
 * `AppLocaleActivityContextTest`, which pins it.
 *
 * The two contexts this deliberately does *not* touch: `ui/theme/Theme.kt`
 * reaches the window through `LocalView.current.context`, and the widget
 * package's `LocalContext` is Glance's own, in a separate composition.
 *
 * ⚠️ **This override stops at the edge of the window.** A `Dialog`, a
 * `ModalBottomSheet` and a `DropdownMenu` each host their content in their own
 * `AbstractComposeView`, which re-provides `LocalContext` from *its* window —
 * so everything below re-enters the device language. That is what
 * [LocalAppLocale] and `InheritAppLocale` exist to carry across. Do not open a
 * window without one; `DialogLocaleGuardTest` fails the build if you do.
 */
@Composable
fun AppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    /**
     * For [AppLanguage.SYSTEM], the device's *current* locale — read live from
     * the Activity's own configuration, which the system keeps up to date and
     * which this file never overrides (the override lives on a wrapper).
     *
     * Deliberately **not** a `Locale.getDefault()` captured once at class-init.
     * That is the obvious implementation and it goes stale: [Locale.setDefault]
     * below mutates the process, so the captured value has to be the *original*
     * — and if the user changes the device language while this process is alive,
     * the Activity is recreated but the process is not, so "System" would go on
     * restoring a language the device stopped using until the next cold start.
     *
     * ⚠️ **Clamped to [AppLanguage.OFFERED], and this is the half of the `#51`
     * freeze that a picker fix does not reach.** [AppLanguage.DEFAULT] is
     * `SYSTEM`, so this is the branch nearly every user is on and *nobody
     * chooses*: with Hebrew merely withheld from the picker, a Hebrew-locale
     * phone would still open a half-translated app (two packages swept of ten)
     * without anyone having touched a setting.
     *
     * Deliberately **not** solved by moving [AppLanguage.DEFAULT] to `ENGLISH` —
     * that leaves `SYSTEM` selectable and still resolving Hebrew, and throws away
     * the follow-the-device semantics `#51` wants back intact.
     */
    val target = language.locale
        ?: AppLanguage.clampToOffered(context.resources.configuration.locales[0])

    // Not a LaunchedEffect: this must land before the frame that reads it, and
    // it is idempotent, so re-running it on recomposition costs nothing.
    SideEffect {
        if (Locale.getDefault() != target) Locale.setDefault(target)
    }

    val localizedContext = remember(context, target) {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(target)
            setLayoutDirection(target)
        }
        LocalizedContext(context, configuration)
    }

    val direction = when (language.isRtl) {
        true -> LayoutDirection.Rtl
        false -> LayoutDirection.Ltr
        // SYSTEM: the platform already resolved a direction for the device
        // locale, so read it back rather than guessing one.
        null -> when (localizedContext.resources.configuration.layoutDirection) {
            android.view.View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides direction,
        // The same three values again, under a key the platform does not know
        // about — which is the only reason they survive into a dialog. See
        // [LocalAppLocale].
        LocalAppLocale provides AppLocaleOverride(localizedContext, direction),
        content = content,
    )
}

/**
 * What [AppLocale] resolved, in a form that can be re-applied inside a window
 * of its own.
 */
@Immutable
data class AppLocaleOverride(
    val context: Context,
    val layoutDirection: LayoutDirection,
) {
    val configuration: Configuration get() = context.resources.configuration
}

/**
 * [AppLocale]'s decision, carried across window boundaries that strip
 * `LocalContext`.
 *
 * ### Why a second, redundant-looking CompositionLocal
 *
 * A `Dialog`, `DropdownMenu` or `ModalBottomSheet` composes its content into a
 * **new `AbstractComposeView` attached to its own window**. That view's
 * composition is a *child* of ours — `rememberCompositionContext()` is passed
 * down — so ordinary CompositionLocals flow into it untouched. What does *not*
 * flow is the handful of locals the platform re-provides for every Android
 * composition (`ProvideAndroidCompositionLocals`): `LocalContext`,
 * `LocalConfiguration`, `LocalView` and friends are re-derived from the **new
 * window's** context, which was built from the Activity and knows nothing about
 * [AppLocale]'s wrapper.
 *
 * So the fix cannot be "provide `LocalContext` harder". It has to be a key the
 * platform will not overwrite — an app-defined local — read back on the far
 * side by `InheritAppLocale`. Being app-defined is the whole mechanism, not a
 * stylistic choice.
 *
 * `null` means no [AppLocale] above this point (a `@Preview`, or a test that
 * composes a screen bare). `InheritAppLocale` then does nothing, which is
 * correct: there is no override to restore.
 */
val LocalAppLocale = staticCompositionLocalOf<AppLocaleOverride?> { null }

/**
 * A [ContextWrapper] that serves locale-overridden [Resources] while **keeping
 * the Activity reachable through `baseContext`**.
 *
 * ### Why this class exists — it is not ceremony around `createConfigurationContext`
 *
 * The obvious implementation is to provide `context.createConfigurationContext(…)`
 * straight into `LocalContext`. It returns a bare `android.app.ContextImpl`,
 * **not** a wrapper around the Activity, so every consumer that walks a context
 * looking for its Activity stops dead. `hiltViewModel()` is exactly such a
 * consumer: inside a `NavHost` the store owner is a `NavBackStackEntry`, so it
 * builds a `HiltViewModelFactory` from `LocalContext.current` and requires that
 * walk to succeed. The result was a crash on the **first frame of every screen**:
 *
 * ```
 * IllegalStateException: Expected an activity context for creating a
 *   HiltViewModelFactory but instead found: android.app.ContextImpl
 *     at HiltViewModelFactory.create(HiltNavBackStackEntry.kt:70)
 *     at GoalPilotRoot(GoalPilotRoot.kt:200)
 * ```
 *
 * `Observed:` 2026-08-16 on the API 37 emulator — the app did not reach its
 * first frame. **348 unit tests and 47 instrumented tests passed against that
 * build**, because none of them composes through `MainActivity`, which is where
 * the override is installed. `AppLocaleActivityContextTest` now pins the
 * invariant directly.
 *
 * Subclassing `ContextWrapper` restores the chain: this is not an `Activity`, so
 * a walker follows `baseContext` and lands on the real one, while `getResources`
 * still hands back the Hebrew table.
 */
private class LocalizedContext(
    base: android.content.Context,
    configuration: Configuration,
) : android.content.ContextWrapper(base) {

    private val localizedResources = base.createConfigurationContext(configuration).resources

    override fun getResources(): android.content.res.Resources = localizedResources
}
