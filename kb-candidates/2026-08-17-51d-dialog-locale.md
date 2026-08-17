# KB candidates — session `51d-dialog-locale`, 2026-08-17

Repo: `C:\Dev\Android_Final_Project` · Branch: `feat/goalpilot-implementation` ·
Issue [#51](https://github.com/idomarhaim/Android_Final_Project/issues/51) (item 3).
Account: [`CHANGELOG/2026-08-17/51d-dialog-locale.md`](../CHANGELOG/2026-08-17/51d-dialog-locale.md).

Mode: **AUTO MODE**. Entry 1 is **always-ask** regardless of mode — it contradicts a
standing KB claim. Entries 2 and 3 are held behind it because they are its supporting
detail and would land referencing a claim that is still under revision.

---

## 1 · Correct RTL mirroring is not evidence that the strings are localized — and the split signal has **more than one** cause

**Status:** ⛔ **ALWAYS-ASK — supersedes a standing claim.** Not drained.

**Claim.** In an RTL locale, *direction* and *language* are carried by different
mechanisms and fail independently. So "the screen mirrors correctly" is worthless as
evidence that its words were localized — and worse than worthless, because a
correctly-mirrored English screen looks *more* finished than a half-done one.

`Observed:` twice, at two layers, with **two different causes**:

| | layer | direction came from | language failed because |
|---|---|---|---|
| 2026-08-16, `widget-pack` | resources | the device was `he-IL`, so the host mirrored | strings sat in `values-he/`, a bucket AAPT2 never resolves |
| 2026-08-16, `51c-analytics-render` | composition | Compose copies `LocalLayoutDirection` onto a new window's `View` | a `Dialog` re-provides `LocalContext` from **its own** window, dropping `AppLocale`'s override |

**Why this supersedes something.**
[`kb/dev/jvm-vs-android-locale-codes.md`](C:/Dev/JARVIS/kb/dev/jvm-vs-android-locale-codes.md)
§2 ("The on-device tell") currently reads:

> A split signal like that points at the **resource bucket**, not at the locale plumbing.

That inference is now known to be **unsound**. The second instance is precisely a
locale-plumbing failure with a correct resource bucket — the diagnostic points at the
wrong place exactly half the time it has ever been used. The sentence was written
when only one instance existed, and it generalised from it.

**Why (reasoning, and what was rejected).** Rejected: filing this as a second symptom
under the `values-iw` page. That page is about **AAPT2 qualifiers**, and the dialog
defect has nothing to do with resources — folding them together is what produced the
over-narrow §2 diagnostic in the first place. The durable claim is the *verification*
one (the tell is real, its cause is not determined), and it belongs where verification
discipline lives, not under one of its two causes.

Also rejected: leaving §2 alone and adding a "see also". A reader hitting the split
signal reads §2, follows it to the bucket, finds the bucket correct, and concludes the
tell was a false alarm — which is the failure mode, not a mitigation.

**Destination.** Two edits, one decision:
1. **New page** `kb/dev/mirroring-is-not-localization.md` — the rule, both instances,
   and the *"check the words, never the layout"* verification duty.
2. **Rewrite** `jvm-vs-android-locale-codes.md` §2 in place — narrow it from *"points
   at the resource bucket"* to *"is consistent with at least two causes; the bucket is
   one"*, linking the new page.

Relates closely to [`kb/dev/describing-is-not-exhibiting.md`](C:/Dev/JARVIS/kb/dev/describing-is-not-exhibiting.md)
and to the user rule *"verify by re-running whatever consumes your output"* — this is
that rule's failure case where the **render itself** is the misleading instrument.

**Anchors.**
- `app/src/main/java/com/idomarhaim/goalpilot/ui/locale/LocaleAwareWindows.kt` — the rule in the file header.
- `app/src/androidTest/.../AppLocaleDialogTest.kt#aBrokenDialogMirrorsCorrectlyWhileSpeakingTheWrongLanguage` — the rule as an executable assertion: direction `Rtl`, strings English, one dialog, one frame.
- `res/values-iw/strings.xml` header — the first instance's own warning.

**Supersedes.** `kb/dev/jvm-vs-android-locale-codes.md` §2 (narrowing, not deletion).

---

## 2 · An **app-defined** CompositionLocal crosses a Compose window boundary; the platform's own do not

**Status:** held behind entry 1 (it is that entry's mechanism half). Not drained.

**Claim.** A `Dialog`, `Popup`/`DropdownMenu` or `ModalBottomSheet` composes into a new
`AbstractComposeView` whose composition is a **child** of the caller's. Ordinary
CompositionLocals therefore flow in untouched. What does *not* survive is the handful
the platform re-provides for every Android composition — `LocalContext`,
`LocalConfiguration`, `LocalView` — which are re-derived from the **new window's**
context.

**The consequence, which is the useful part:** any app-wide override installed via
`LocalContext` (a locale, a theme context, a fake for testing) is silently lost inside
every dialog, sheet and menu. It **cannot** be fixed by providing `LocalContext`
harder. The remedy is to publish the value under an **app-defined** key that the
platform will not overwrite, and re-provide the platform locals from it on the far
side.

`Observed:` API 37, 2026-08-17 — `LocalAppLocale.current` (app-defined) is non-null
inside both a `Dialog` and a `Popup`, while `stringResource` in the same composition
returns the device language. All three window types measured separately rather than
inferred from the dialog.

**Why (reasoning, and what was rejected).** Rejected: **capturing the context in the
caller and passing it in** (`InheritLocale(ctx) { … }`), which was the first shape of
this remedy. It works, but its failure mode is silence — capture inside the slot and
you capture the already-reverted context, so the wrapper compiles, reads correctly and
does nothing. A zero-argument wrapper reading an app-defined local makes the wrong
capture **unrepresentable**.

Rejected: overriding at the Activity/`View` level so windows inherit naturally. The
dialog's context comes from `LocalView.current.context`, not `LocalContext`, so this
would work — but the language here is a runtime user setting, and moving it to the
Activity configuration means activity recreation on every switch. Out of scope, and a
different design than the one this app already committed to.

**Destination.** New page `kb/dev/compose-window-boundary-locals.md`, linked from
entry 1's page as its mechanism.

**Anchors.** `ui/locale/AppLocale.kt` (`LocalAppLocale`), `ui/locale/LocaleAwareWindows.kt`,
`AppLocaleDialogTest#anAppDefinedCompositionLocalDoesCrossTheWindowBoundary`.

**Supersedes.** Nothing.

---

## 3 · A guard must be broken in a form that **compiles**, or the break tests the compiler

**Status:** held behind entry 1. Not drained.

**Claim.** When proving a source-scanning guard actually fires, the deliberate defect
must be introduced in the shape a real contributor would produce — **including the
imports their IDE adds**. Otherwise the compiler rejects it first and the run proves
only that the code did not build.

`Observed:` 2026-08-17. Reintroducing a raw `AlertDialog(` into `SocialScreen.kt`
after its import had been removed failed at `compileDebugKotlin` with *"Unresolved
reference"* — the guard never ran. Restoring the import (which any IDE auto-adds when
you type the call) made the defect compile, and only then did
`DialogLocaleGuardTest` fail and name the line.

**Why this is not obvious.** The first attempt *looked* like a successful negative
test: the build went red, at the right file, on the right line. Nothing in the output
says the guard was never reached. It is the same flattering-failure shape as
`resource-guard-inputs`' finding that a task can be green because it never ran.

**Why (reasoning, and what was rejected).** Rejected: extending
[`scanned-files-are-not-task-inputs.md`](C:/Dev/JARVIS/kb/dev/scanned-files-are-not-task-inputs.md)'s
four-state discipline with a fifth state. That page is about **task invalidation** —
whether the guard *re-ran*. This is about whether the **defect was expressible** at
all, which is upstream of invalidation and applies to guards whose inputs never cache.
Two different questions with the same symptom (a red build that proves nothing).

**Destination.** A section on `scanned-files-are-not-task-inputs.md` **or** its own
short page — `/kb-ingest` to decide once entry 1 settles whether these three become
one cluster.

**Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/locale/DialogLocaleGuardTest.kt`;
the four-state sequence recorded in `CHANGELOG/2026-08-17/51d-dialog-locale.md` § Tests.

**Supersedes.** Nothing.
