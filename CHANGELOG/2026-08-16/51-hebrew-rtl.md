# `51-hebrew-rtl` — 2026-08-16

> **Summary:** **This ships the foundation, not the whole ticket.** The `feature/` literal sweep — #51's item 4 and by far its largest part — is **not** in this unit.

`/implement` [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) — *Hebrew locale
and RTL* · branch `feat/goalpilot-implementation` · mode `AUTO MODE`

**This ships the foundation, not the whole ticket.** The `feature/` literal sweep — #51's item 4 and
by far its largest part — is **not** in this unit. What is here is everything the sweep needs to
exist first, plus three defects found along the way. The end of this entry says exactly what is left.

## 1 · `values-he/` was dead, and `HEAD` was red because of it

`widget-pack` found on a device (`a4e5c36`) that `res/values-he/` renders nothing and wrote the
warning on `SESSIONS.md`, naming `36-tasks-consent`'s file as having the same defect. Two things were
still outstanding when this session started:

- `res/values-he/strings.xml` (`fba4197`, #36's three Hebrew strings) was **still in the dead
  bucket** and had therefore never rendered once since it shipped.
- `widget-pack`'s own deletion of `res/values-he/widget_strings.xml` was **staged but never
  committed** — so `HEAD` failed that session's own `WidgetHebrewResourceTest`, while the working
  tree passed. A fresh clone was red.

Both are now resolved: `values-he/` is gone entirely, `strings.xml` moved to `values-iw/`, and the
staged deletion is committed. The owner was established as **gone** before touching it — explicit
release note on the board, last commit 02:08, transcript's last turn 02:08:57, all ~7¾ hours before
this session's first write.

## 2 · The explanation for `values-iw` is wrong, and it is wrong in a dangerous direction

Everyone — the tutorials, this ticket's body, `widget-pack`'s notes, and this session's own first
three files' worth of KDoc — says the bucket is `iw` *because Java/Android reports Hebrew with the
legacy code `iw`*. **Measured, that is false.**

| runtime | `Locale.forLanguageTag("he").language` |
|---|---|
| JDK 21.0.12 (unit tests) | `"he"` — JDK 17 flipped `java.locale.useOldISOCodes` to `false` |
| Android 17 / API 37 | `"he"` — `Observed:` on the project emulator, 2026-08-16 |

Both say `he`. **And `res/values-iw/` resolves correctly on that same device anyway** — proved by
`AppLocaleInstrumentedTest`, which pulls real Hebrew strings out of a Hebrew-configured context. So
the bucket is a fact about **AAPT2 and the resource system**, and `Locale` has nothing to do with it.

**Why this is worth three paragraphs rather than a footnote: the folk explanation is self-defeating
as it ages.** It names a checkable fact; that fact has now flipped; and checking it returns `"he"`,
which reads as *"the legacy wart is gone, rename this directory to `values-he`"*. The measurement
that appears to authorise the rename is the measurement that causes the outage. Every KDoc that
carried the old story has been corrected, and the assertion that survives is
`isAnyOf("he", "iw")` **with a message forbidding anything from branching on it** — a test whose job
is to prevent a dependency, not to pin a value.

This was found by writing the obvious unit test, watching it fail, "correcting" it to the equally
obvious *"but Android kept the mapping"*, and watching **that** fail on the device too.

## 3 · What shipped

**The bucket and its guard.** `res/values-iw/` is the only Hebrew bucket.
`HebrewLocaleResourceTest` (new, 6 tests) asserts: no `he`-qualified directory exists at all · every
translatable English key has a Hebrew counterpart · no Hebrew key is orphaned · no Hebrew literal
appears in `values/` (§4.8 asserts this *absolutely*, which is what caught three instances beyond
the one Ido spotted) · no Hebrew string is an untranslated copy of its English original.

That last check needed a rule rather than an allowlist, and the naive rule is wrong: *"does it
contain a letter?"* fails on `%1$d%%`, because the conversion character `d` **is** a letter. Format
specifiers are stripped first; what is left decides. Brand names go through aapt's own
`translatable="false"`, which correctly excuses them from parity too.

**The Language setting (§5.1).** `AppLanguage` (`SYSTEM`/`ENGLISH`/`HEBREW`), stored per-device
beside the skin for the reason §5.1 gives — it must be known before the first frame and the account
is not known until Auth resolves. `ui/locale/AppLocale.kt` applies it, and `LanguagePicker` on
Profile makes it reachable in one tap. Direction is **declared on the enum constant**, not derived
from `locale.language`, precisely because of §2 above.

**Hebrew is now reachable without changing the whole device**, which is what §0.8 needs in order to
be cheap enough to actually do.

**Locale-aware date formatters — §5.1's second filed defect, closed.** §5.1: *"all ten date
formatters are process-scoped `val`s no switch can move."* `AppDateFormatters.of(pattern)` re-reads
the default per call and caches on `(pattern, locale)`; the ten sites become `get()` accessors. The
contract is `get()`, never `val` — a `val` re-freezes the locale one layer in and is the same bug.
`AppDateFormattersTest` includes an executable statement of the defect, so a "simplification" back
to a stored `val` fails a test that explains itself.

**§4.8 range isolation.** `AnalyticsRange.windowLabel` is now direction-isolated. Its `WEEK` branch
is §4.8's named defect exactly — `Aug 3 – Aug 9` renders as `Aug 9 – Aug 3` in an RTL paragraph, a
wrong week that looks like a right one — and the `QUARTER` bucket label `d/M` is the same defect one
scale down (`1/7` → `7/1`).

## 4 · The review found a 🔴 that the entire test suite had passed

`/adversarial-review` asked what else walks `LocalContext`. `AppLocale` was providing
`context.createConfigurationContext(config)` straight into `LocalContext`, and that returns a bare
`android.app.ContextImpl` — not a wrapper around the Activity. `hiltViewModel()` inside a `NavHost`
builds its factory from `LocalContext.current` and walks it for an Activity:

```
IllegalStateException: Expected an activity context for creating a HiltViewModelFactory
  but instead found: android.app.ContextImpl
    at HiltViewModelFactory.create(HiltNavBackStackEntry.kt:70)
    at GoalPilotRoot(GoalPilotRoot.kt:200)
```

**The app did not reach its first frame — and 348 unit tests plus 47 instrumented tests were green
against that build**, because none of them composes through `MainActivity`, which is the only place
the override is installed.

Two things about this are worth keeping. First, the KDoc I had written claimed the override was
*"safe, checked rather than assumed: nothing in this app casts it to an `Activity`"* — and the grep
behind that claim was real but **scoped to `app/src`**, while the consumer that broke was
`androidx.hilt`. A library is not in `app/src`. Second, it was caught only by **installing and
launching the app**, which is the one check that runs what actually consumes the output.

Fixed with `LocalizedContext`, a `ContextWrapper` that serves locale-overridden `Resources` while
leaving the Activity reachable through `baseContext`. `AppLocaleActivityContextTest` (new) asserts
the **property** — an Activity is still reachable — rather than naming Hilt, because the next
consumer will be some other library. **The guard was checked against the broken implementation**: it
fails there, with a diagnostic naming the cause, and passes after the fix.

Also from the review: `SYSTEM` restored a `Locale.getDefault()` captured once at class-init, which
goes stale if the device language changes while the process lives (the Activity is recreated, the
process is not). It now reads the Activity's live configuration. And the resource-guard regex
matched `<string-array` via `\b`, since `\b` sits between `string` and `-`.

## 🧪 Tests

Run with an isolated Gradle invocation on the claimed emulator `Pixel_10_Pro_XL` (API 37).

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`, `--rerun-tasks`) | **348 pass, 0 fail**, 36 classes — was **326** |
| **Instrumented / UI E2E** (`:app:connectedDebugAndroidTest`) | **51 pass, 0 fail** — was **43** |
| **Build** (`:app:assembleDebug`) | green |
| **Real launch** (install + `am start` + logcat) | **no crash, process alive** |
| **`firestore-tests`** | **not owed** — no `firestore.rules` change in this unit |
| **Cloud Functions** | **not touched** — see *what is left*, item 4 |

New tests, **30** in total — 22 JVM (`HebrewLocaleResourceTest` 6 · `AppLanguageTest` 8 ·
`AppDateFormattersTest` 6 · `AnalyticsRangeTest` +2) and 8 device-only
(`AppLocaleInstrumentedTest` 4 · `AppLocaleActivityContextTest` 4). The counts reconcile:
348 − 326 = 22 and 51 − 43 = 8.

Two existing `AnalyticsRangeTest` assertions were updated to `Bidi.strip(...)` because the labels are
now isolated; the marks themselves are asserted separately, so stripping cannot quietly permit their
removal.

## 5 · What is left of #51, and why

Named as remaining work, not as defects — none of this was attempted and failed.

1. **The `feature/` literal sweep (#51 item 4) — the bulk.** `res/values/strings.xml` holds **9**
   strings; `feature/` has ~578 candidate literals across 27 files and 8,393 lines. **So the app is
   not yet in Hebrew**: switching the picker today changes direction, mirroring, the tagline, #36's
   consent strings and the two settings cards, and leaves every screen's own words in English. This
   is genuinely multi-session and wants one session per feature package, which the parity test now
   makes safe to do incrementally.
2. **Terminology (#51 item 6)** — `Goal` is **יעד** and the four tabs are `בית · מטרות · לוח שנה ·
   חברתי`. Deferred *with* the sweep rather than separately: the words do not exist as resources yet.
3. **Bidi isolation beyond the range labels.** Done where §4.8 names a concrete defect; the general
   sweep over every count, percentage and duration travels with item 1.
4. **§5.1's first filed defect — no prompt states an output language.** A one-line prompt change
   priced 0/10 → 3/3, but it needs `language` threaded from client to `functions/`, and it only takes
   effect on a **`firebase deploy`**, which is always-ask. Left as its own unit rather than shipped
   half-wired.
5. **The donut's Hebrew caption overrun (#51 item 3).** Found by rendering, invisible in source, and
   needs a Hebrew render of the analytics screen to confirm and to check the fix.

## Board

`SESSIONS.md` row claimed before the first write; emulator `Pixel_10_Pro_XL` and the Gradle daemon
claimed for the test runs. `kb-candidates/2026-08-16-51-hebrew-rtl.md` written — 3 entries, all
`kb/`-destined and therefore cross-repo, none drained here.
