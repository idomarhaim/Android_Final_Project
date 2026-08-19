package com.idomarhaim.goalpilot.locale

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppDropdownMenu
import com.idomarhaim.goalpilot.ui.locale.AppLocale
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet
import com.idomarhaim.goalpilot.ui.locale.InheritAppLocale
import com.idomarhaim.goalpilot.ui.locale.LocalAppLocale
import org.junit.Rule
import org.junit.Test

/**
 * A window of its own **does not inherit** `AppLocale`'s `LocalContext`, and the
 * tell is the one that made `values-he` so hard to see.
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
 * screen's composition. A `Dialog`, `Popup` or `ModalBottomSheet` hosts its
 * content in its **own** `AbstractComposeView`, attached to its own window, and
 * that view's composition re-provides `LocalContext` from the *window's*
 * context — built from the Activity, not from our wrapper. Direction is copied
 * onto the new window's `View` by Compose itself; the context is not.
 *
 * **The lesson generalises beyond dialogs: correct RTL mirroring is not evidence
 * that the strings are localized.** That inference failed for `values-he` (the
 * widget mirrored and spoke English) and it fails again here, one layer up.
 *
 * ### What this class pins
 *
 * 1. The **platform behaviour**, per window type, so a future reader knows it is
 *    real and not a bug in our code — and so a Compose upgrade that fixes it is
 *    noticed rather than silently making our wrappers dead weight.
 * 2. The **mechanism the remedy stands on** — that an *app-defined*
 *    CompositionLocal does cross the boundary the platform's own does not. Every
 *    wrapper in `LocaleAwareWindows.kt` is worthless if that is ever untrue, and
 *    it is the one part of the design that is an assumption about Compose
 *    internals rather than about our own code.
 * 3. The **remedy**, end to end, through the real wrappers.
 * 4. The **rejected design**, and why — see [capturingInsideTheSlotIsANoOp].
 */
class AppLocaleDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---------------------------------------------------------------- 1. the defect

    @Test
    fun aDialogDoesNotInheritTheLocaleOverride() {
        // Asserted so the next reader does not "fix" the wrappers by deleting
        // them, having reasoned that CompositionLocals are inherited. They are —
        // but the dialog's own ComposeView re-provides this one.
        var insideDialog: String? = null
        var outsideDialog: String? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                outsideDialog = stringResource(R.string.settings_language_title)
                Dialog(onDismissRequest = {}) {
                    insideDialog = stringResource(R.string.settings_language_title)
                }
            }
        }
        composeRule.waitForIdle()

        assertWithMessage("the screen itself must be Hebrew")
            .that(outsideDialog).isEqualTo(HEBREW_LANGUAGE)
        assertWithMessage(
            "If this now equals the Hebrew title, the platform has started inheriting the " +
                "context into dialogs. Good news — but re-read LocaleAwareWindows.kt before " +
                "removing it, and check the minSdk range still needs it.",
        ).that(insideDialog).isNotEqualTo(HEBREW_LANGUAGE)
    }

    @Test
    fun aBrokenDialogMirrorsCorrectlyWhileSpeakingTheWrongLanguage() {
        // THE RULE ITSELF, as an assertion rather than a comment:
        //
        //   correct RTL mirroring is NOT evidence that the strings are localized
        //
        // Both halves measured on the same dialog in the same frame, because it
        // is their COMBINATION that misleads. Direction crosses the window
        // boundary; language does not. A reviewer looking at this dialog sees a
        // flawless right-to-left mirror and concludes the screen is done.
        var direction: LayoutDirection? = null
        var text: String? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                Dialog(onDismissRequest = {}) {
                    direction = LocalLayoutDirection.current
                    text = stringResource(R.string.settings_language_title)
                }
            }
        }
        composeRule.waitForIdle()

        assertWithMessage("the dialog mirrors correctly — the half that misleads")
            .that(direction).isEqualTo(LayoutDirection.Rtl)
        assertWithMessage("…and speaks the device language regardless — the half that matters")
            .that(text).isNotEqualTo(HEBREW_LANGUAGE)
    }

    @Test
    fun aPopupDoesNotInheritTheLocaleOverrideEither() {
        // A DropdownMenu is a Popup, not a Dialog. Asserted separately because
        // "it is the same class of bug" was a guess until it was run.
        var insideMenu: String? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                DropdownMenu(expanded = true, onDismissRequest = {}) {
                    insideMenu = stringResource(R.string.settings_language_title)
                }
            }
        }
        composeRule.waitForIdle()

        assertThat(insideMenu).isNotEqualTo(HEBREW_LANGUAGE)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun aModalBottomSheetDoesNotInheritTheLocaleOverrideEither() {
        var insideSheet: String? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                ModalBottomSheet(onDismissRequest = {}) {
                    insideSheet = stringResource(R.string.settings_language_title)
                }
            }
        }
        composeRule.waitForIdle()

        assertThat(insideSheet).isNotEqualTo(HEBREW_LANGUAGE)
    }

    // ------------------------------------------------- 2. the mechanism it stands on

    @Test
    fun anAppDefinedCompositionLocalDoesCrossTheWindowBoundary() {
        // THE LOAD-BEARING ASSUMPTION of every wrapper in LocaleAwareWindows.kt.
        //
        // The window's composition is a CHILD of ours (Compose passes the parent
        // CompositionContext down), so ordinary locals flow in untouched. Only
        // the platform's own handful — LocalContext, LocalConfiguration,
        // LocalView — are re-derived from the new window. LocalAppLocale is ours,
        // so nothing re-derives it.
        //
        // If this ever fails, InheritAppLocale silently becomes a no-op and every
        // dialog in the app goes back to English while still mirroring perfectly.
        var overrideInsideDialog: Any? = null
        var overrideInsidePopup: Any? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                Dialog(onDismissRequest = {}) {
                    overrideInsideDialog = LocalAppLocale.current
                }
                DropdownMenu(expanded = true, onDismissRequest = {}) {
                    overrideInsidePopup = LocalAppLocale.current
                }
            }
        }
        composeRule.waitForIdle()

        assertWithMessage(
            "LocalAppLocale did not reach inside a Dialog. InheritAppLocale is now a " +
                "no-op and every dialog renders in the device language — see " +
                "ui/locale/LocaleAwareWindows.kt.",
        ).that(overrideInsideDialog).isNotNull()
        assertWithMessage("LocalAppLocale did not reach inside a Popup.")
            .that(overrideInsidePopup).isNotNull()
    }

    // ---------------------------------------------------------------- 3. the remedy

    @Test
    fun inheritAppLocaleRestoresTheLanguageInsideADialog() {
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                Dialog(onDismissRequest = {}) {
                    InheritAppLocale {
                        Text(stringResource(R.string.settings_language_title))
                    }
                }
            }
        }

        composeRule.onNodeWithText(HEBREW_LANGUAGE).assertExists()
    }

    @Test
    fun appAlertDialogKeepsEverySlotInTheAppLanguage() {
        // The façade, end to end. A slot missed inside AppAlertDialog fails
        // silently and only in Hebrew, so each one is named here.
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                AppAlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.analytics_backfill_title)) },
                    text = { Text(stringResource(R.string.settings_language_title)) },
                    confirmButton = {
                        TextButton(onClick = {}) {
                            Text(stringResource(R.string.analytics_backfill_close))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {}) {
                            Text(stringResource(R.string.analytics_backfill_cancel))
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText(HEBREW_TITLE).assertExists()      // title
        composeRule.onNodeWithText(HEBREW_LANGUAGE).assertExists()   // text
        composeRule.onNodeWithText(HEBREW_CLOSE).assertExists()      // confirmButton
        composeRule.onNodeWithText(HEBREW_CANCEL).assertExists()     // dismissButton
    }

    @Test
    fun appDropdownMenuKeepsItsItemsInTheAppLanguage() {
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                AppDropdownMenu(expanded = true, onDismissRequest = {}) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_language_title)) },
                        onClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(HEBREW_LANGUAGE).assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun appModalBottomSheetKeepsItsContentInTheAppLanguage() {
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                AppModalBottomSheet(onDismissRequest = {}) {
                    Text(stringResource(R.string.settings_language_title))
                }
            }
        }

        composeRule.onNodeWithText(HEBREW_LANGUAGE).assertExists()
    }

    @Test
    fun englishIsUnaffectedByTheWrappers() {
        // The wrappers must be invisible when the app is in English — otherwise
        // a "fix" for Hebrew would be a regression for everyone else, and no
        // Hebrew assertion above would notice.
        composeRule.setContent {
            AppLocale(language = AppLanguage.ENGLISH) {
                AppAlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.settings_language_title)) },
                    confirmButton = {},
                )
            }
        }

        composeRule.onNodeWithText("Language").assertExists()
    }

    // -------------------------------------------------------- 4. the rejected design

    @Test
    fun capturingInsideTheSlotIsANoOp() {
        // WHY InheritAppLocale TAKES NO CONTEXT PARAMETER.
        //
        // The first version of this remedy (51c, feature/analytics only) was
        // `InheritLocale(localizedContext) { … }`, with the caller capturing
        // LocalContext.current OUTSIDE the dialog. [CapturedContextLocale] below
        // reproduces it. Its failure mode is silence: capture inside the slot and
        // you capture the already-reverted context, so the wrapper compiles,
        // reads correctly, and does nothing.
        //
        // The shipped signature has no parameter, so there is nothing to capture
        // in the wrong place. This test is the record of the design it replaced —
        // if anyone reintroduces a context argument, this is what it costs.
        var resolved: String? = null
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                Dialog(onDismissRequest = {}) {
                    val alreadyReverted = LocalContext.current
                    CapturedContextLocale(alreadyReverted) {
                        resolved = stringResource(R.string.settings_language_title)
                    }
                }
            }
        }
        composeRule.waitForIdle()

        assertThat(resolved).isNotEqualTo(HEBREW_LANGUAGE)
    }

    /** The rejected, parameterized shape. Kept only to demonstrate its failure. */
    @Composable
    private fun CapturedContextLocale(
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
        const val HEBREW_CLOSE = "סגירה"
        const val HEBREW_LANGUAGE = "שפה"
    }
}
