# 48-settings-surface — Profile is the account, Settings is the device, and the split is now testable

> **Summary:** §4.9's Settings screen ships with four of its five sections — Appearance, Language &
> region, Your day, Account — reachable from Home's new avatar sheet **and** from the sign-in
> screen with no account. `ProfileScreen`'s skin and language pickers move out, which is the defect
> the ticket opens with. **Material (4 tiles) and the whole AI section are not built:** both
> presuppose subsystems (`C12` §4.1's material contract, `C13`'s encrypted key store) that do not
> exist at HEAD and that no open issue schedules.

**Issue:** [#48](https://github.com/idomarhaim/Android_Final_Project/issues/48) · **Session:**
`48-settings-surface` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` (brief front matter)
**Branch:** `main` · **Brief:** [`sessions/done/48-settings-surface.md`](../../sessions/done/48-settings-surface.md)
**Singletons held:** Gradle daemon, emulators `Pixel_10_Pro_XL` and — unavoidably, see *Tests* —
`Pixel_10_Pro_XL_B`. Both released; `_B` is running and `Pixel_10_Pro_XL` is stopped, which is how
this session found them.

> **Every control that feeds arithmetic elsewhere states that arithmetic under itself, with live
> values.**

---

## 1 · The defect the ticket opens with — device controls leave Profile

`ProfileScreen.kt`'s `AppearanceCard` hosted the skin picker and `LanguagePicker` beside the friend
code and Sign out. §5.1 stores Language per-device for one stated reason — *it must be known before
the first frame, and the account is not known until Auth resolves* — so a Language control on the
**account** screen is unreachable exactly when its own justification says it is needed.

Both pickers moved to `feature/settings/`. `ProfileViewModel` no longer injects
`AppPreferencesRepository` at all, and its KDoc says why: **the absence is the fix**, and anything
device-scoped arriving there again is the defect coming back.

The two strings survive the move rather than being re-authored — they are already swept and already
translated. One of them had to change: `settings_appearance_description` ended *"Light and dark mode
still follow your system setting"*, which the new Brightness control makes **false**. The clause is
deleted in `values/` and in `values-iw/`; no new Hebrew was authored.

## 2 · The five sections — four built, two controls held, and the reason is checkable

| §4.9 section | State |
|---|---|
| **Appearance** — material · colour · brightness | **colour + brightness built; material held** |
| **Language & region** — language · region · derived week start | **built** |
| **Your day** — awake between · plan tomorrow at | **built** |
| **AI** — provider · your own key · status line | **held, whole section** |
| **Account** — one row to Profile, stating the boundary | **built** |

**Material is held because §4.1's contract does not exist.** There is no `AppMaterial` anywhere in
`app/`, no palette transform, and no open issue scheduling one — `grep` over the open-issue list
(#6–#11, #48, #50, #51) finds nothing for `C12` §4.1. A picker over four materials that nothing
renders is a control that changes nothing, which is precisely §0.3's *second number that quietly
disagrees* installed in the screen built to prevent it.

**The AI section is held because `C13` is a design, not a build.** Its three controls need an
`EncryptedSharedPreferences` store (`androidx.security:security-crypto` is not a dependency here), a
provider abstraction, and a status line naming which provider answered. Today every model call goes
through the Cloud Function proxy and **the client holds no key at all**; `#32` resolved `C13` as a
decision on 2026-08-10 and shipped no code.

Neither is drawn as a disabled row. §4.9's own rule is that **a lock is a word, never a dimming**,
and the honest word for a section whose subsystem does not exist belongs in the backlog — it is
here, and in `SettingsScreen`'s KDoc, where the next author will be standing.

**Consequence for the scope line.** §4.9 justifies the scope line by `C13`'s encrypted key
outliving sign-out. With no key, the line states the scope that **is** true — *"Everything here
belongs to this phone, not to your account. It stays exactly as it is when you sign out."* It is a
sentence to rewrite the day `C13` lands, not one to delete, and `ScopeLine`'s KDoc says so.

## 3 · The consequence line — the screen's one new component

`feature/settings/ConsequenceLine.kt`. Its KDoc carries the two things it must not become: not a
*description* of the control (if the sentence is still true with the setting on any other value, it
is the wrong sentence), and not an icon (§0.8's surviving sub-rule is *form and words before
iconography*, and a ⓘ is a tooltip by another name).

A second component, `SettingDescription`, exists deliberately **beside** it rather than being the
same thing: a description is chrome and is true whatever the control says. Sharing one component
would let a description quietly take a consequence's place, which is the failure §4.9's table
exists to prevent.

What each line says, all computed rather than described:

| Control | Its line |
|---|---|
| Region | `Week starts on Sunday · today reads 8/19/26` |
| Awake between | `16 h awake — your day's load bar reddens past 12 h of planned work.` |
| Plan tomorrow at, untouched | `Follows your waking hours — one hour before they end, at 11:00 PM. Move them and this moves too.` |
| Plan tomorrow at, pinned | `Pinned to 7:00 PM. It no longer follows your waking hours.` |
| Your profile | `Your friend code, level, points and streak live there and leave with the account. Nothing on this screen does.` |

## 4 · Week start is derived, and the derivation is measured twice

`AppRegion.firstDayOfWeek` is `WeekFields.of(locale).firstDayOfWeek` — not a hand-written table of
countries, which is the same shape of defect `AppLanguage.isRtl`'s KDoc already warns about: wrong
for every country nobody thought of, and silently.

**It is asserted on both runtimes on purpose.** The JVM reads its own CLDR copy and Android resolves
through ICU, so the two agreeing is a *measurement*, not a deduction — and because week start is
derived and never stored, **nothing in the app would disagree with a wrong answer**. `AppRegionTest`
pins IL/US/GB/FR on the JVM; `SettingsScreenTest.weekStart_forIsrael_isSundayOnDevice` pins Israel
on the device. Both say Sunday for `IL`, which is the combination Ido asked for.

`AppRegionTest.englishInIsrael…` pins the §5.1 decoupling itself: **words from Language, order from
Region**, composed by `formattingLocale` rather than by taking either setting's locale whole.

**Region is stored and stated, and wired to nothing else.** The ten process-scoped date formatters
are `#51`'s and `#51` is deferred — the brief rules that out of scope, and `AppRegion`'s KDoc records
it so the next reader does not read the gap as an omission.

## 5 · `DaySchedule` — the relationship, not two loose numbers

`Plan tomorrow at` is `planningOverrideMinutes: Int?`. Unset means *derived*, and that is a state
the type can be in rather than a value somebody has to keep refreshing. Two independent fields can
only be **written** to agree and then drift, which is §0.3 with the disagreement pre-installed.
`planningFollowsWaking` is what the consequence line reads, which is what lets the screen tell the
truth in **both** directions and offer *"Follow my waking hours again"* in one of them.

`WakingHours.loadBarRedMinutes` lives on the type rather than at the bar, per §0.2. §4.9 states one
pair — a 16 h day reddens at 12 h — so the ¾ fraction is marked `Inferred:` in the KDoc and
`Untested:` as to its consumer, which is `#8`'s scheduled half.

A span **may wrap past midnight**, because `22:00 – 06:00` is a real answer and a clamp that
rejects it silently clamps a night-shift user's reminders into the hours they are asleep.
`start == end` reads as **zero**, not 1440: a consumer dividing by it should see zero rather than a
plausible-looking full day.

## 6 · Two entry points, and the second one is the ticket

- **Home's avatar sheet** (§4.2) — `DashboardScreen` gains an avatar in the top bar opening an
  `AppModalBottomSheet` with **two siblings**, *Your profile* and *Settings*. They are peers because
  they are peers in the model; a sheet with Settings tucked inside Profile would put the account
  back in charge of the device.
- **The sign-in screen** — `GoalPilotRoot`'s signed-out branch was a bare `SignInScreen()` with no
  `NavHost` at all. It is now a two-destination graph, so the system back gesture works without this
  file re-implementing a back stack. `SignInScreen` carries a **word beside the icon**, not a bare
  gear, and insets itself (`safeDrawingPadding`) because it sits outside `ui/root`'s Scaffold.

`SettingsViewModel` injects `AppPreferencesRepository` **and nothing else**. An `AuthRepository`
there would compile, work while signed in, and quietly make the signed-out entry point broken — the
same defect §4.9 exists to fix, one layer down.

## 7 · `AppTimePickerDialog`, built on the wrapper rather than beside it

`DialogLocaleGuardTest` bans a raw `TimePickerDialog(`, and material3 1.3.1 (BOM 2024.12.01) has no
public one anyway. The new façade is composed out of `AppAlertDialog`, so `ui/locale/` never opens a
**second** raw window: when a later BOM ships `TimePickerDialog`, the guard already names it and
nothing here has to be remembered.

## 8 · The 24 h track — §4.9's third design rule, and the reason for it

`feature/settings/DayTrack.kt`. §4.9: *a mark that carries meaning is filled **and** outlined*,
generalising `C6`'s `--edge` — the prototype's awake band was fill-only and vanished in neo light.
Both the band and the planning marker are drawn twice, fill plus a stroke in a different role
colour. A wrapping span draws as two segments rather than as nothing.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **419 tests, 0 failures, 0 errors, 0 skipped**, 44 classes — 3 of them new |
| **Build** (`:app:assembleDebug`) | **green** |
| **Instrumented** (`am instrument`, `Pixel_10_Pro_XL_B`, API 35) | **82 tests, 0 failures** — 12 of them new |
| **Render pass** (device, both AVDs) | **seen** — Home avatar → sheet → Settings → both halves of the screen; Brightness → Dark repaints a light-system device and was set back to *Device* |
| **Firestore rules** (`firestore-tests/`) | **not applicable** — no `firestore.rules` change; this screen writes only to SharedPreferences |
| **Cloud Functions** (`functions/`) | **not applicable** — no function touched |

**New JVM classes:** `AppBrightnessTest` (6), `AppRegionTest` (16), `DayScheduleTest` (13) — **35
cases** across three classes, counted off the JUnit XML rather than off the source. **New instrumented class:** `SettingsScreenTest` (12), driving the
hoisted `SettingsContent` so it needs no Hilt graph and no Firebase.

### Three instrumented failures, found and fixed

All three were the same root cause and it is worth recording, because two of them looked like
different bugs. The content scrolls, and Compose's `performClick` on a node below the fold does not
throw — it lands elsewhere. `theOverrideCanBeHandedBackToTheDerivation` therefore reported the state
as *unchanged*, which reads as a defect in `DaySchedule` rather than in the test. `performScrollTo()`
before every `performClick()` / `assertIsDisplayed()` on the lower half fixed all three.

### The instrumented run did **not** use `connectedDebugAndroidTest`, and could not have

The task **uninstalls the app**, which takes the app's Firebase session with it. Both AVDs on this
machine are signed in — `Pixel_10_Pro_XL_B` as `rachil751@gmail.com` (the two-account demo, with a
`FIREBASE_USER` persisted an hour before this session started) and `Pixel_10_Pro_XL` as **עידו**,
which makes the brief's *"`Pixel_10_Pro_XL` is already signed out, so nothing is lost"* **stale**;
`new-machine-checkup` signed in on it on 2026-08-19. With **0.5 GB of 15.7 GB free**, a third
device was not available either.

So the suite ran through `adb install -r` + `am instrument`, which preserves app data. Afterwards
the store still held its `FIREBASE_USER` at the same size and the app still landed on the dashboard.
📌 **KB candidate**, parked and always-ask — it narrows a standing rule.

### What is **not** verified, and exactly what would close it

**The sign-in screen's Settings button has not been seen on a device.** Seeing it needs a
**signed-out** app, and both AVDs are signed in; signing Ido out of his own primary device is his
call, not this session's. What *is* verified: the signed-out shape of the screen renders and states
its boundary (`signedOut_theScreenStillRendersAndStillStatesTheBoundary`), and the button and its
route are in the graph. **Cheapest close:** the cloud emulator
([`docs/CLOUD-DEVICE.md`](../../docs/CLOUD-DEVICE.md)) boots a runner with **no Google account at
all** — a genuinely signed-out device — and photographs the running app. One click in the Actions
tab. `gh` is not installed on this machine, so the dispatch is Ido's.

---

## 📥 KB candidates

One, **parked**: [`kb-candidates/2026-08-20-48-settings-surface.md`](../../kb-candidates/2026-08-20-48-settings-surface.md).
Always-ask on two independent grounds — its destination includes `rules/`, and it modifies a
standing claim.

`kb-candidates/2026-08-19-50-offline-stamps.md` was already there and is **not a debt**: it is
correctly parked at its own always-ask entry 3, its other two entries having been drained on
2026-08-19. Left untouched.

## 🔀 Sibling check

Board read to its next `## ` heading, rows counted mechanically: **0 active rows** at session start.
Nothing unpushed, tree clean at that point. Blanket staging not used; every commit takes explicit
paths. `SESSIONS.md`'s own diff was read in its own tool call before each commit that touched it.

**One foreign commit rides this push, and it is named here because a reply is not findable in a
month.** `9ee547c` — *"50-offline-stamps r2: the answer was on neither menu"* — landed **after**
this session's claim commit `ee4603e` and was still unpushed when this unit finished. `git push` is
branch-scoped, so it goes up with mine whether or not I want it to.

It was adjudicated rather than assumed: that session wrote its **own** release note into
*Recently released*, which is a positive signal about itself and settles the question without a
transcript check; the Active-claims section holds no row for it; and its paths
(`docs/PRODUCT_v0.3.md`, `sessions/50b-transaction-guard.md`,
`CHANGELOG/2026-08-20/50-offline-stamps-r2.md`) are committed and quiet in the working tree.
Released and quiet, so it rides.

**It also explains a line in its own note.** That session deliberately left its red test uncommitted
*because this session held the Gradle daemon* — so `sessions/50b-transaction-guard.md` is a real,
compile-owing brief, and the daemon is free from now.
