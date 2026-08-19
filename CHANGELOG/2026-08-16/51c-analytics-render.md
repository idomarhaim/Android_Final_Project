# `51c-analytics-render` — 2026-08-16

> **Summary:** Ido signed in on `emulator-5554`, which closed the `unverified` `51b` left open: *"the analytics screen itself was not seen by eye"*.

`/implement` [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) — **render and
look at the analytics screen in Hebrew** · branch `feat/goalpilot-implementation` · mode `AUTO MODE`

Ido signed in on `emulator-5554`, which closed the `unverified` `51b` left open: *"the analytics
screen itself was not seen by eye"*. Not a sweep — no new package.

## 1 · #51 item 3 does not reproduce, and that is the headline

The ticket records two defects as *found by rendering and invisible in source*:

> The donut's centre caption **overruns its hole and collides with the labels** in Hebrew but not in
> English, and **the slice percentage reorders** (`27% לימודים`).

**Neither reproduces.** Rendered on API 37, app language Hebrew, three slices
(`לימודים` / `בריאות` / `Unassigned`):

- **No overrun.** Unselected, the centre reads `3h 45m` over `נמדד`. Selected, it reads `67%` /
  `לימודים` / `2h 30m` — three lines, comfortably inside the hole, no collision with the legend.
  What prevents it is already in the code and predates this pass: `padding(horizontal = 28.dp)`,
  `maxLines = 2` and `TextOverflow.Ellipsis` on the area name.
- **No percentage reorder.** `67%` renders with the sign to the right of the digits. It cannot
  reorder here, because this screen never concatenates percent and name into one string — they are
  two stacked `Text`s. The one place they *are* combined is the TalkBack description, and there the
  percent is isolated: `לימודים ⁨67%⁩`.

`Observed:` with the data on Ido's account. The honest limit: a **long** Hebrew area name was not
among it, and that is the input most likely to produce an overrun. The `maxLines`/ellipsis guard
covers it in principle; nobody has seen it fail.

## 2 · The §4.8 categories, checked at the runtime rather than by eye

Dumping the rendered view tree beats squinting at a screenshot, and it is what the checks below are.
Every count, percentage, duration and range carries exactly **one balanced `FSI…PDI` pair**:

```
'⁨2026⁩ · חלקו של הזמן שנמדד בכל תחום חיים'
'⁨4⁩ מתוך ⁨5⁩ משכי זמן הוערכו על ידי הבינה המלאכותית; …'
'הערכה מחדש של ⁨1⁩ משך זמן'          'הזמן מחולק בין ⁨3⁩ תחומי חיים, לימודים ⁨67%⁩,…'
'⁨2h 30m⁩'  '⁨67%⁩'  '⁨45m⁩'  '⁨20%⁩'  '⁨30m⁩'  '⁨13%⁩'
```

The range label sits **rightmost** in its line, which is the start position in RTL — verified by
cropping, after a first reading of the full screenshot got it backwards. Hebrew month names
(`ינו׳ פבר׳ מרץ … אוג׳`) confirm the locale-aware formatters from the foundation pass work on-device.

## 3 · Two defects the render found in this package — both fixed

**1 · `analytics_a11y_separator` lost its trailing space.** aapt strips leading/trailing whitespace
from an **unquoted** resource value, so `, ` resolved as `,` and TalkBack read the life-area list as
`לימודים ⁨67%⁩,בריאות` with no pause. Invisible in the XML (the space is *there*), invisible on
screen (the string is only ever spoken), and it survived authoring, review and a full render pass —
caught only by dumping the rendered `content-description`. Fixed by quoting the value; guarded at
**both** layers, `HebrewLocaleResourceTest` on the authoring and `AppLocaleInstrumentedTest` on what
aapt actually produced.

**2 · 🔴 A Compose `Dialog` does not inherit the locale override.** The re-estimate dialog rendered
**entirely in English** while the screen behind it was Hebrew.

> **And it laid out right-to-left correctly while doing so** — checkbox on the right, RTL button
> order. `LocalLayoutDirection` is inherited; `LocalContext` is not.

That is the *same signature* as the original `values-he` defect one layer up, and it generalises to a
rule worth carrying: **correct RTL mirroring is not evidence that the strings are localized.** The
cause is that a `Dialog` hosts its content in its own `AbstractComposeView`, whose composition
re-provides `LocalContext` from the dialog's window — built from the Activity, not from `AppLocale`'s
wrapper.

Fixed for this package's dialog by capturing the context in the **caller's** composition and
re-providing it in each slot (`InheritLocale`). `AppLocaleDialogTest` pins three things: that the
platform really does drop it, that the remedy restores it, and that the remedy's own failure mode —
capturing `LocalContext.current` *inside* a slot, which compiles, reads sensibly and does nothing —
is a no-op.

**Every other dialog, bottom sheet and popup in the app has this defect.** Filed on #51, not fixed.

## 4 · The stray full stop: not the tagline, and the sweep is the fix

`51b` spotted the auth tagline rendering `.stay motivated with friends`. Confirmed, and it is **not
tagline-specific** — the dashboard's smart-add hint does it too:

| String (logical) | Renders as |
|---|---|
| `…and stay motivated with friends.` | `.stay motivated with friends` |
| `…files it under the right goal.` | `.right goal` |

Both are **English** sentences ending in a neutral character, under an RTL paragraph: the trailing
`.` takes the paragraph direction and lands at the left. The decisive comparison is a **Hebrew**
sentence in the same position on the swept screen —
`…סמנו משימה כבוצעה והזמן שלה יופיע כאן.` — whose full stop sits correctly at the line end.

**So the remaining sweep inherits neither a per-string fix nor a shared wrapper: translating the
string removes the defect**, because the defect *is* the string being untranslated under RTL. The
one residue is text that stays English by design — §5.1's user-authored content — which would show
it if it ended in punctuation; that belongs to whichever package renders such content.

## 5 · Found outside this package — filed, not fixed

1. **`formatMinutes` is not localized.** `core/util/DateTimeUtils.kt` hardcodes `h`/`m`, so Hebrew
   reads `3h 45m` / `2h 30m`. The widget already has the Hebrew form
   (`gp_widget_duration_hm` = `%1$d ש׳ %2$d ד׳`), so the app contradicts itself.
2. **`"Unassigned"` is a hardcoded English literal** — `TimeAllocationUseCase.UNASSIGNED_NAME`,
   rendered as a life-area name on this screen and in the widget.
3. **The dashboard has two *different* RTL defects** — worth distinguishing, because the remedies
   differ: `30 pts to level 2` renders `pts to level 2 30` (**bidi reorder inside one string**, needs
   isolation), while `70` + `pts` renders `pts 70` (**RTL order of two sibling `Text`s in a `Row`**,
   needs a combined string, not an isolate).
4. **The widget's `מטרה`/`יעד` contradiction and its `ל־%1$d` prefix defect** — carried over from
   `51b`, still open.

## 6 · Two operational findings

**The file-scanning guards can silently not run.** `HebrewLocaleResourceTest` and
`AnalyticsLiteralSweepTest` read `res/` and `src/` straight off disk, so Gradle does not know those
files are inputs to `testDebugUnitTest`. A **resource-only** change leaves the task `UP-TO-DATE` and
the guard does not execute — `Observed:` this session, where the whitespace guard passed on a
deliberately broken resource until `--rerun-tasks` was added. The guards are only as good as their
invocation; declaring the inputs on the test task would fix it, and `app/build.gradle.kts` is outside
this unit, so it is filed.

**`connectedDebugAndroidTest` uninstalls the app, wiping the signed-in session.** Ido signed in for
this pass; running the instrumented suite afterwards removed the package and the account with it. The
suite and a signed-in device are mutually exclusive, which matters because #51's remaining render
checks need an account. The app has been reinstalled and set back to Hebrew; **the sign-in has not
been restored, because that is not mine to do.**

## ⚠️ Disclosure — this pass wrote to Ido's data

Driving the re-estimate dialog on-device ran the AI backfill and **updated one task's estimated
duration**: the footnote moved from `4 מתוך 5` to `כל 5 המשימות`. That is the feature's own action
and it was reached by my taps, not his. It also means the dialog is no longer reachable by data,
which is why the fix is pinned by `AppLocaleDialogTest` rather than by a second screenshot.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`--rerun-tasks`) | **351 pass, 0 fail**, 37 classes — was 350 |
| **Instrumented / UI E2E** | **55 pass, 0 fail** — was 51 |
| **Build** (`:app:assembleDebug`) | green |
| **Render-and-look, Hebrew, signed in** | 5 screenshots + 4 view-tree dumps; findings above |
| **`firestore-tests`** | **not owed** — no `firestore.rules` change |

New: `AppLocaleDialogTest` (3, device-only) · `AppLocaleInstrumentedTest` +1 (the separator) ·
`HebrewLocaleResourceTest` +1 (the whitespace guard, verified to fail on the unquoted form).

One narrowing, argued rather than assumed: `no hebrew literal appears in the default resources` now
strips **XML comments** first. §4.8 asks for the assertion "absolutely" and that absoluteness is what
caught three instances — but the absolute form produced a false positive the first time somebody
documented a Hebrew defect *in the English file*, which makes that file unable to explain its own
Hebrew-driven decisions. A comment provably cannot reach a render.

## 7 · What is left of #51

1. **Eight packages unswept** — `lifeareas` 93, `dashboard` 91, `goals` 73, `challenges` 71,
   `social` 39, `health` 14, `profile` 11, `auth` 4.
2. **`ui/components/` unswept** and shared by every screen — do it *before* the per-package sweeps.
3. **Dialogs/sheets/popups app-wide** drop the locale override — §3 above.
4. **`formatMinutes`, `"Unassigned"`, the dashboard's two RTL defects, the widget's terminology** —
   §5 above.
5. **§5.1's AI output-language prompt line** — needs a `firebase deploy`, always-ask.
6. **A long Hebrew life-area name** has still not been rendered — the one input that could still
   produce #51 item 3's overrun.
