package com.idomarhaim.goalpilot.ui.locale

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties

/**
 * Every window this app opens — dialog, sheet, menu — in a form that keeps
 * [AppLocale]'s language.
 *
 * ---
 *
 * ## 📌 THE RULE, WHICH IS WORTH MORE THAN THIS FILE
 *
 * **Correct RTL mirroring is _not_ evidence that the strings are localized.**
 *
 * The two travel on completely separate rails and one of them is much easier to
 * get right by accident:
 *
 * | | carried by | crosses a window boundary? |
 * |---|---|---|
 * | **Direction** | `LocalLayoutDirection` — `Inferred:` via the new window's `View.layoutDirection`, which Compose sets from the parent composition | **yes** |
 * | **Language** | `LocalContext.resources`, which every new `AbstractComposeView` re-derives from *its own* window | **no** |
 *
 * `Observed:` the *combination* is measured, not reasoned about —
 * `AppLocaleDialogTest.aBrokenDialogMirrorsCorrectlyWhileSpeakingTheWrongLanguage`
 * reads both off one unwrapped dialog in one frame: direction `Rtl`, strings
 * English. Only the mechanism in the table's middle column is inference.
 *
 * So a broken window looks like this: **the checkbox sits on the right, the
 * buttons are in RTL order, the layout is a perfect mirror — and every word is
 * English.** It looks *more* finished than a half-done job, which is why nobody
 * catches it by glancing at a screenshot.
 *
 * `Observed:` twice, at two different layers, by two different sessions:
 *
 * 1. **`values-he` vs `values-iw`** — the home-screen widget mirrored perfectly
 *    and spoke English for weeks, because AAPT2 files Hebrew under the legacy
 *    `iw` qualifier and the resources sat in a bucket nothing ever resolved.
 *    (2026-08-16, on a he-IL Samsung. See `res/values-iw/strings.xml`.)
 * 2. **This defect** — the analytics re-estimate dialog, one layer up.
 *    (2026-08-16, session `51c-analytics-render`, API 37 emulator.)
 *
 * Same signature, same wrong inference, two layers apart. **Whenever you verify
 * Hebrew by looking at a screen, the mirroring tells you nothing about the
 * words — read the words.**
 *
 * ---
 *
 * ## Why these wrappers exist rather than a note telling you to remember
 *
 * The remedy is three lines ([InheritAppLocale]) and the failure mode of the
 * remedy is **silence**: a slot you forget to wrap renders in the device
 * language and nothing anywhere goes red. An `AlertDialog` has *four* such
 * slots, and this app has fourteen `AlertDialog`s, so "remember to wrap the
 * slots" is fifty-odd chances to be quietly wrong — and the eight
 * still-unswept feature packages of #51 will each be editing exactly these
 * lambdas.
 *
 * So the slots are wrapped **here, once**, and `DialogLocaleGuardTest` fails
 * the build if a raw `androidx` window constructor appears anywhere outside
 * this file. That turns a discipline nobody can keep into an invariant nobody
 * has to.
 */

/**
 * Re-applies [AppLocale]'s language and direction inside a window that dropped
 * them.
 *
 * Reads [LocalAppLocale] — an **app-defined** CompositionLocal, which is the
 * entire trick: ordinary locals flow into a dialog's composition untouched, and
 * only the platform's own (`LocalContext`, `LocalConfiguration`, `LocalView`, …)
 * are re-derived from the new window. See [LocalAppLocale] for the mechanism.
 *
 * ### It takes no context parameter, deliberately
 *
 * The obvious signature is `InheritAppLocale(localizedContext) { … }`, with the
 * caller capturing `LocalContext.current` **outside** the window. That was the
 * first version of this remedy, and it has a failure mode that compiles, reads
 * correctly and does nothing: capture the context *inside* the slot and you
 * capture the already-reverted one, so the wrapper is a no-op wearing the
 * costume of a fix. `AppLocaleDialogTest.capturingInsideTheSlotIsANoOp` still
 * pins that, as a record of why this signature has no parameter — with nothing
 * to capture, there is no wrong place to capture it.
 *
 * Outside an [AppLocale] (a `@Preview`, a bare test) this composes [content]
 * unchanged. There is no override to restore, and inventing one would make a
 * preview disagree with the app.
 */
@Composable
fun InheritAppLocale(content: @Composable () -> Unit) {
    val override = LocalAppLocale.current
    if (override == null) {
        content()
    } else {
        CompositionLocalProvider(
            LocalContext provides override.context,
            LocalConfiguration provides override.configuration,
            // Already correct in a Dialog — Compose copies the direction onto
            // the new window's View. Re-provided anyway so this wrapper's
            // contract is "everything AppLocale provides", and does not depend
            // on which window type it lands in.
            LocalLayoutDirection provides override.layoutDirection,
            content = content,
        )
    }
}

/**
 * [AlertDialog], with all five content slots kept in the app's language.
 *
 * Slot nullability is preserved exactly — a wrapped `null` would draw an empty
 * title where Material draws none.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { InheritAppLocale(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { slot -> { InheritAppLocale(slot) } },
        icon = icon?.let { slot -> { InheritAppLocale(slot) } },
        title = title?.let { slot -> { InheritAppLocale(slot) } },
        text = text?.let { slot -> { InheritAppLocale(slot) } },
        properties = properties,
    )
}

/**
 * [DatePickerDialog], in the app's language.
 *
 * `Inferred:` the `content` slot should matter more here than the buttons do —
 * Material3's `DatePicker` takes its calendar locale from `LocalConfiguration`,
 * so re-providing it ought to localize the month and weekday names themselves.
 * Read out of Material3's `defaultLocale()`, **not observed**: the only
 * `DatePickerDialog` in this app is the challenge date picker, and no test or
 * render has yet opened it in Hebrew. `Untested:` open it on a Hebrew device
 * and read the month row. The wrapper is correct either way — this is a claim
 * about how much it buys, not about whether it works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { InheritAppLocale(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { slot -> { InheritAppLocale(slot) } },
    ) {
        val columnScope = this
        InheritAppLocale { columnScope.content() }
    }
}

/** [ModalBottomSheet], in the app's language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
    ) {
        val columnScope = this
        InheritAppLocale { columnScope.content() }
    }
}

/**
 * [DropdownMenu], in the app's language.
 *
 * A menu is a `Popup`, not a `Dialog`, and it is affected for the same reason:
 * `PopupLayout` is an `AbstractComposeView` with its own window and its own
 * `LocalContext`.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
    ) {
        val columnScope = this
        InheritAppLocale { columnScope.content() }
    }
}

/** A bare [Dialog], in the app's language. */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        InheritAppLocale(content)
    }
}
