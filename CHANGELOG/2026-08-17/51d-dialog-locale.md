# 51d-dialog-locale — the `LocalContext`-into-`Dialog` defect, fixed once for the whole app

> **Summary:** the `LocalContext`-into-`Dialog` defect, fixed once for the whole app

**Issue:** [#51](https://github.com/idomarhaim/Android_Final_Project/issues/51) (left **open** — this is item 3 of nine).
**Branch:** `feat/goalpilot-implementation`.
**Scope:** the dialog **plumbing** only. No literal sweep, no `formatMinutes`, no `"Unassigned"`.

---

## What was wrong

`51c-analytics-render` found it on the device and filed it: a Compose `Dialog`
hosts its content in its **own** `AbstractComposeView`, attached to its own
window, and that view's composition re-provides `LocalContext` from the
*dialog's* context — built from the Activity, never from `AppLocale`'s wrapper.
So `ui/locale/AppLocale.kt`'s language override is silently dropped at every
window boundary in the app.

`51c` fixed it inside `feature/analytics/` with a private `InheritLocale`, and
said so plainly: *"Every other dialog, bottom sheet and popup in the app still
has this defect."* That was 21 more sites.

## 📌 The rule, which outlives the fix

> **Correct RTL mirroring is _not_ evidence that the strings are localized.**

Direction and language travel on **separate rails**, and the easy one to get
right by accident is the visible one:

| | carried by | crosses a window boundary? |
|---|---|---|
| **Direction** | `LocalLayoutDirection` | **yes** |
| **Language** | `LocalContext.resources` | **no** |

So a broken window looks *more* finished than a half-done job: checkbox on the
right, buttons in RTL order, a flawless mirror — and every word English. It is
the identical signature to the `values-he` / `values-iw` defect one layer down,
where the widget mirrored perfectly and resolved nothing for weeks.

**This is now an assertion, not a comment.**
`AppLocaleDialogTest.aBrokenDialogMirrorsCorrectlyWhileSpeakingTheWrongLanguage`
reads both halves off one unwrapped dialog in one frame — direction `Rtl`,
strings English — so the misleading combination is *measured* rather than
described. It also lives in the header of `ui/locale/LocaleAwareWindows.kt`,
which is the file anyone touching a dialog has to open.

---

## What was built

### 1 · `LocalAppLocale` — the mechanism (`ui/locale/AppLocale.kt`)

The fix cannot be "provide `LocalContext` harder". A new window's composition
re-derives the platform's own locals (`LocalContext`, `LocalConfiguration`,
`LocalView`) from **its** context, and always wins.

But that window's composition is a **child** of ours, so *ordinary* locals flow
into it untouched. `AppLocale` therefore also publishes an **app-defined**
`LocalAppLocale`, carrying the localized context and direction under a key the
platform does not know about and will not overwrite. Being app-defined **is**
the mechanism, not a style choice.

`Observed:` verified, not assumed —
`AppLocaleDialogTest.anAppDefinedCompositionLocalDoesCrossTheWindowBoundary`
reads `LocalAppLocale.current` inside both a `Dialog` and a `Popup` and finds it
non-null. Every wrapper below is worthless if that ever stops being true, and it
is the one part of the design that is an assumption about Compose internals
rather than about our own code — so it is pinned on its own.

### 2 · `ui/locale/LocaleAwareWindows.kt` — the wrappers

- `InheritAppLocale { }` — re-applies context, configuration and direction.
- `AppAlertDialog`, `AppDialog`, `AppDropdownMenu`, `AppModalBottomSheet`,
  `AppDatePickerDialog` — façades that wrap **every content slot** internally.

**`InheritAppLocale` takes no context parameter, and that is the design.** `51c`'s
shape was `InheritLocale(localizedContext) { … }`, with the caller capturing
`LocalContext.current` outside the window. Its failure mode is *silence*:
capture inside the slot and you capture the already-reverted context, so the
wrapper compiles, reads correctly and does nothing. With no parameter there is
no wrong place to capture it. `capturingInsideTheSlotIsANoOp` keeps the rejected
shape alive in the test file purely to document what it costs.

**Why façades and not "remember to wrap the slots":** an `AlertDialog` has four
content slots and this app has fourteen of them — roughly fifty chances to be
quietly wrong, in Hebrew only. The façades turn 22 call sites into 22
one-token changes with no forgettable slots.

### 3 · `DialogLocaleGuardTest` — so site 23 cannot reintroduce it

A JVM file-scanning guard: **no raw `androidx` window constructor may appear
outside `ui/locale/`.** It catches the fully-qualified form too
(`androidx.compose.material3.AlertDialog(`), which `DashboardScreen` really did
contain and which a naive grep for `AlertDialog(` at line start would have
missed.

This matters more than the fix: the eight feature packages still owed a literal
sweep under #51 will each be editing exactly these lambdas, and a sweep that
turns `Text("Cancel")` into `Text(stringResource(…))` inside an unwrapped dialog
**reintroduces the defect while looking correct in an English render.**

`Untested:` an import alias (`import …AlertDialog as Foo`) would slip past. The
guard closes the accident, not the determined workaround, and its KDoc says so.

### 4 · The 22 call sites

| Package | `AlertDialog` | `DropdownMenu` | other |
|---|---|---|---|
| `analytics` | 1 | — | — |
| `challenges` | 4 | 1 | `ModalBottomSheet`, `DatePickerDialog` |
| `lifeareas` | 3 | 2 | — |
| `goals` | 2 | 1 | — |
| `dashboard` | 2 | — | — |
| `social` | 2 | 1 | `Dialog` (photo viewer) |

`feature/analytics/`'s private `InheritLocale` and its four hand-wrapped slots
are **deleted** — the shared wrapper replaces them, and `AnalyticsScreen.kt` is
108 lines lighter.

**No string was translated, moved or reworded in any of these packages.** The
change is locale plumbing; #51's sweep is untouched and still owed.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **356 / 0** — was 351 before `HebrewTerminologyTest` (+3) and this session (+2) |
| **Instrumented** (`:app:connectedDebugAndroidTest`) | **63 / 0** on `Pixel_10_Pro_XL`, API 37 — was 55 at `51c` |
| **Build** (`:app:assembleDebug`) | green |
| `firestore-tests/` | **not run** — no rules, no backend, no data-layer change |
| Screenshot / render | **not run** — see the honest limit below |

`AppLocaleDialogTest` grew 3 → 11 tests, in four groups: the defect (per window
type), the mechanism the remedy stands on, the remedy end-to-end, and the
rejected design.

### The guard was checked in both directions, not just observed green

A guard that has only ever passed is indistinguishable from a guard that cannot
fail. So:

1. green on the finished tree;
2. a raw `AlertDialog(` reintroduced in `SocialScreen.kt` → the **compiler**
   caught it, because the import was gone. That is not the realistic case;
3. the raw import restored as an IDE would auto-add it, so the defect
   **compiles** → `DialogLocaleGuardTest` **FAILED**, naming
   `SocialScreen.kt:401`;
4. reverted → green.

Step 3 is the one that matters: it is the only state that resembles what a
future session will actually do.

### Popup and bottom sheet were measured, not reasoned about

`51c` observed the defect in a `Dialog` only. That a `DropdownMenu` (a `Popup`)
and a `ModalBottomSheet` share it was my inference from them all being
`AbstractComposeView`-hosted — so each got its own failing-without-the-wrapper
assertion rather than being folded into the dialog's. All three confirmed on
device.

`englishIsUnaffectedByTheWrappers` covers the other direction: a fix for Hebrew
that regressed English would pass every Hebrew assertion above.

---

## Honest limits

1. **No render.** Instrumented tests and a signed-in device are mutually
   exclusive here — `connectedDebugAndroidTest` uninstalls the app and takes the
   Google account with it — and this unit was assigned the instrumented run. The
   assertions read resolved strings and layout direction off the real device, so
   the *mechanism* is verified on-device; what has not been **looked at** is a
   converted dialog in Hebrew on a signed-in screen.
2. **Almost nothing is visibly different today.** Outside `feature/analytics/`
   every dialog in this app is still hardcoded English literals, so there is
   nothing for the wrapper to redirect yet. This change is **prophylactic**: its
   value lands with each package's sweep, and the guard is what makes sure it is
   there when that happens rather than being re-derived eight times.
3. **`AppDatePickerDialog`'s calendar is `Inferred:`, not observed** — Material3
   takes the picker's locale from `LocalConfiguration`, so the month and weekday
   names should follow, but nothing has opened that picker in Hebrew. Marked as
   such in the KDoc.

## Still open on #51 — unchanged by this session

Items 1, 2, 4, 5, 6, 7, 8, 9. Explicitly **not** touched here: the remaining
feature packages' literal sweep, `formatMinutes`, `"Unassigned"`.
