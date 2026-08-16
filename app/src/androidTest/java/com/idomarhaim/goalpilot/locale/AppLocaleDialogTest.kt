package com.idomarhaim.goalpilot.locale

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.ui.locale.AppLocale
import org.junit.Rule
import org.junit.Test

/**
 * A Compose `Dialog` **does not inherit** `AppLocale`'s `LocalContext` override,
 * and the tell is the one that made `values-he` so hard to see.
 *
 * ### What was observed
 *
 * `Observed:` 2026-08-16, on the device, app language Hebrew. Opening the
 * analytics re-estimate dialog produced a dialog that **laid out right-to-left
 * correctly — checkbox on the right, RTL button order — while every word in it
 * was English.** The screen behind it was fully Hebrew.
 *
 * ### Why
 *
 * `ui/locale/AppLocale.kt` provides a locale-overridden `LocalContext` for the
 * screen's composition. A `Dialog` hosts its content in its **own**
 * `AbstractComposeView`, attached to its own window, and that view's composition
 * re-provides `LocalContext` from the *dialog's* context — built from the
 * Activity, not from our wrapper. `LocalLayoutDirection` is inherited; the
 * context is not.
 *
 * **The lesson generalises beyond dialogs: correct RTL mirroring is not evidence
 * that the strings are localized.** That inference failed for `values-he` (the
 * widget mirrored and spoke English) and it fails again here, one layer up.
 *
 * ### What this class pins
 *
 * Both halves — the platform behaviour, so a future reader knows it is real and
 * not a bug in our code, and the remedy, so the fix cannot silently rot. The
 * remedy is applied at every dialog in `feature/analytics/`; every other dialog,
 * bottom sheet and popup in the app still has the defect and it is filed on #51.
 */
class AppLocaleDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aDialogDoesNotInheritTheLocaleOverride() {
        // Asserted so the next reader does not "fix" the remedy below by
        // deleting it, having reasoned that CompositionLocals are inherited.
        // They are — but the dialog's own ComposeView re-provides this one.
        var insideDialog: String? = null
        var outsideDialog: String? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                outsideDialog = stringResource(R.string.analytics_backfill_title)
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = { insideDialog = stringResource(R.string.analytics_backfill_title) },
                )
            }
        }
        composeRule.waitForIdle()

        assertWithMessage("the screen itself must be Hebrew")
            .that(outsideDialog).isEqualTo(HEBREW_TITLE)
        assertWithMessage(
            "If this now equals the Hebrew title, the platform has started inheriting the " +
                "context into dialogs. Good news — but re-read InheritLocale before removing it, " +
                "and check the minSdk range still needs it.",
        ).that(insideDialog).isNotEqualTo(HEBREW_TITLE)
    }

    @Test
    fun reProvidingTheCapturedContextRestoresIt() {
        // The remedy `feature/analytics/AnalyticsScreen.InheritLocale` applies:
        // capture the context in the CALLER's composition, where the override is
        // still live, and re-provide it inside each dialog slot.
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                val captured = LocalContext.current
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        InheritLocale(captured) {
                            TextButton(onClick = {}) {
                                Text(stringResource(R.string.analytics_backfill_cancel))
                            }
                        }
                    },
                    title = {
                        InheritLocale(captured) {
                            Text(stringResource(R.string.analytics_backfill_title))
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText(HEBREW_TITLE).assertExists()
        composeRule.onNodeWithText(HEBREW_CANCEL).assertExists()
    }

    @Test
    fun capturingInsideTheSlotIsANoOpThatLooksCorrect() {
        // The failure mode of the remedy itself: reading LocalContext.current
        // *inside* a slot returns the already-reverted context, so the wrapper
        // compiles, reads sensibly, and does nothing.
        var resolved: String? = null
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = {
                        val wrong = LocalContext.current // already reverted
                        InheritLocale(wrong) {
                            resolved = stringResource(R.string.analytics_backfill_title)
                        }
                    },
                )
            }
        }
        composeRule.waitForIdle()

        assertThat(resolved).isNotEqualTo(HEBREW_TITLE)
    }

    /** Mirrors `feature/analytics/AnalyticsScreen.InheritLocale`, which is private. */
    @Composable
    private fun InheritLocale(
        localizedContext: android.content.Context,
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedContext.resources.configuration,
            content = content,
        )
    }

    private companion object {
        const val HEBREW_TITLE = "הערכה מחדש של משכי זמן"
        const val HEBREW_CANCEL = "ביטול"
    }
}
