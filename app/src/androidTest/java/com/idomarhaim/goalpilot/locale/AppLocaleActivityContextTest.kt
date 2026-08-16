package com.idomarhaim.goalpilot.locale

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.ui.locale.AppLocale
import org.junit.Rule
import org.junit.Test

/**
 * The invariant that `AppLocale`'s `LocalContext` override must not break.
 *
 * ### Written because it already happened
 *
 * `AppLocale` first provided `context.createConfigurationContext(config)`
 * directly into `LocalContext`. That returns a bare `android.app.ContextImpl`,
 * so every consumer that walks a context looking for its `Activity` stops dead.
 * `hiltViewModel()` is one: inside a `NavHost` the store owner is a
 * `NavBackStackEntry`, so it builds its factory from `LocalContext.current` and
 * needs that walk to succeed. The app crashed on the **first frame of every
 * screen**:
 *
 * ```
 * IllegalStateException: Expected an activity context for creating a
 *   HiltViewModelFactory but instead found: android.app.ContextImpl
 * ```
 *
 * **348 unit tests and 47 instrumented tests were green against that build.**
 * None of them composed through `MainActivity`, which is the only place the
 * override is installed — so the whole suite agreed the change was fine while
 * the app would not start. That gap is what this class closes.
 *
 * It deliberately asserts the **property** (an `Activity` is still reachable)
 * rather than mentioning Hilt: Hilt is one consumer of the context chain, the
 * next one will be some other library, and a test naming Hilt would go on
 * passing while the real invariant broke.
 */
class AppLocaleActivityContextTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anActivityIsStillReachableThroughTheLocalizedContext() {
        var resolved: Activity? = null
        var contextClass: String? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                val context = LocalContext.current
                contextClass = context.javaClass.name
                resolved = context.findActivity()
            }
        }
        composeRule.waitForIdle()

        assertWithMessage(
            "LocalContext no longer walks to an Activity (it is $contextClass). " +
                "hiltViewModel() and anything else resolving an Activity from the " +
                "context chain will throw on the first frame. See AppLocale.LocalizedContext.",
        ).that(resolved).isNotNull()
    }

    @Test
    fun theSystemLanguageAlsoKeepsTheActivityReachable() {
        // SYSTEM takes the same construction path, so it can break independently
        // of HEBREW if the null-locale branch is ever special-cased.
        var resolved: Activity? = null
        composeRule.setContent {
            AppLocale(language = AppLanguage.SYSTEM) {
                resolved = LocalContext.current.findActivity()
            }
        }
        composeRule.waitForIdle()
        assertThat(resolved).isNotNull()
    }

    @Test
    fun hebrewResolvesHebrewStringsAndLaysOutRightToLeft() {
        // The two things the override exists to do, asserted through the real
        // composable rather than through a hand-built Configuration.
        var direction: LayoutDirection? = null
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                direction = LocalLayoutDirection.current
                Text(stringResource(R.string.settings_language_title))
            }
        }

        composeRule.onNodeWithText("שפה").assertExists()
        assertThat(direction).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun englishResolvesEnglishStringsAndLaysOutLeftToRight() {
        var direction: LayoutDirection? = null
        composeRule.setContent {
            AppLocale(language = AppLanguage.ENGLISH) {
                direction = LocalLayoutDirection.current
                Text(stringResource(R.string.settings_language_title))
            }
        }

        composeRule.onNodeWithText("Language").assertExists()
        assertThat(direction).isEqualTo(LayoutDirection.Ltr)
    }

    /** The same walk `androidx.hilt` and `ui/theme/Theme.kt` both perform. */
    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
