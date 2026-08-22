# `tutorial-onboarding` — 2026-08-22

> **Summary:** The app now teaches itself. A **seven-step guided tour** runs once on first launch —
> a dimmed screen with a real hole cut over the widget being described, a card beside the hole, a
> Skip on every step and a system-back that means *one step back*. It is **progressive onboarding**
> rather than the instinctive carousel-before-the-app, which the research is unanimous against for a
> reason that survives taste: a carousel teaches the app while the app is not there. Step four is the
> tour's one piece of **doing** — it waits for the user to open the Goals tab and follows them — and
> it advances on the **route reached**, never on the tap, so a swallowed tap cannot advance the tour
> past something that did not happen. Skip records the tour as seen, which is only humane because
> **Settings → Help → Replay tutorial** brings it back.
>
> ⚠️ **The defect worth reading is §4.2**, because every test and all seven screenshots passed over
> it: replaying from Settings started the tour and left the app **on Settings**, silently, with no
> exception and no log — and the same call from the Goals tab worked, so walking the tour forwards
> could never find it. Found on the device, root-caused with temporary logging, fixed, and the
> regression test was then checked by reverting the fix.
>
> JVM **782 / 0** across 73 classes (+30 tests, +3 classes). Instrumented on `emulator-5554`:
> `TutorialOverlayUiTest` **10 / 0** (new), `TutorialNavigationUiTest` **4 / 0** (new),
> `SettingsScreenTest` **17 / 0** (+2), full suite **223 / 0**. Seven steps, the replay path and the
> stored preference all verified by hand on the device.

---

## 1. What Ido asked for, and what each half became

| His words | What shipped |
|---|---|
| *"tutorial inside the app, like apps usually do"* | `ui/tutorial/` — a spotlight overlay drawn over the real app, not a separate screen |
| *"pop-up boxes explaining every part"* | seven coach marks, each anchored to a live widget by `Modifier.tutorialAnchor` |
| *"guide the user to do things the first time"* | step 4 blocks everything except the Goals tab and waits; the last step offers **Create my first goal** |
| *"look at the highest standards on the web and implement accordingly"* | §2 |
| *"the user can SKIP"* | Skip on **every** step, plus tap-anywhere and system-back |
| *"a TUTORIAL button that can pop it up again"* | Settings → **Help** → *Replay tutorial* |

---

## 2. What the research actually says, and the four places it changed the build

Sources are listed at the end. Four findings changed a decision rather than confirming one:

1. **Progressive beats front-loaded.** Material's own onboarding guidance and every current
   coach-mark write-up put contextual, in-place guidance above a pre-app carousel. So the tour walks
   the app that exists; `TutorialStep` is a route over live screens and holds no illustrations.
2. **Length is the failure mode, not clarity.** Attention falls off a cliff past five to seven
   marks, and what a user skips is never step six — it is the whole feature, once and for good.
   Seven is a **budget**, enforced by `TutorialStepsTest.the tour is short enough to be watched`.
   The cut rule: one step per thing that cannot be discovered by looking. Social and Profile share
   one step because the bottom bar is self-explanatory; life areas, challenges, analytics, Health
   Connect and Google Tasks are each one tap behind a step that *is* here.
3. **The mark must sit on the real element** — a high-contrast overlay with the target revealed, not
   a screenshot and not a copy of the widget. Hence the anchor registry: a drawn copy of the nav bar
   would keep rendering the nav bar as it looked the day the tour was written and start lying, in a
   way only a person looking at both can see.
4. **One required action, not six.** Guided-action coach marks are what turn a tour into an
   activation, and they are also what turn it into a chore. One is the budget, spent on the gesture
   that matters most, and `TutorialStepsTest.the tour asks for exactly one action` holds it.

Accessibility is from the same reading. The card is an `Assertive` live region carrying the step's
**own words** as its description — a live region reads its node's description, so one holding only
*"guided tour, skip at any time"* would announce that on every step and never say what the step is.
The counter is words as well as dots. And the spotlight's pulse is off entirely when
`ANIMATOR_DURATION_SCALE` is zero — the value the accessibility remove-animations toggle sets, and
the same one `rememberGpEntrance` already reads.

⚠️ `Untested:` **no screen-reader pass was run.** TalkBack was not turned on, so what is verified is
that the words are on the node rather than absent from it; that the announcement fires as intended
is `Inferred:` from how live regions are sourced. Turning TalkBack on and listening to a step change
is what would close it, and it is a few minutes on the AVD already in use.

---

## 3. The five decisions worth reading

### 3.1 An overlay inside the composition, never a `Dialog` or a `Popup`

`DialogLocaleGuardTest` bans both outright, and its reason is exactly this feature's: a new window
re-derives `LocalContext` from itself, so everything inside renders in the **device** language while
mirroring right-to-left perfectly — a defect that looks *more* finished than a half-done job. The
overlay is an ordinary composable, so `AppLocale` reaches it untouched.

It buys a second thing no window could give at any price. The hole has to sit over a widget six
layers down a *different* screen, and both sides only agree on where that is because they share one
composition root. A popup's coordinates are its own.

### 3.2 `positionInRoot() + size`, not `boundsInRoot()`

`boundsInRoot()` is **clipped by every parent**, so a card scrolled half out of a `LazyColumn`
reports half of itself — and a hole drawn from it cuts the widget in two while looking, on the frame
you happen to screenshot, like a slightly-off cutout rather than a bug.

### 3.3 The blockers are four rectangles, not one handler that declines to consume

Compose delivers a pointer event to every node under it and lets consumption decide who acts, so a
full-screen handler that declines to consume inside the hole *does* let the widget beneath fire.
That version was rejected: it makes the feature's most important behaviour depend on a consumption
subtlety no reader of the file can verify by looking, and that any `clickable` added anywhere in the
stack can break silently. Four rectangles that are simply **not there** over the hole cannot be got
wrong. `TutorialOverlayUiTest` asserts both directions on a real device.

### 3.4 `pendingAction` is state, and it exists to close a dead end reachable in one gesture

Step 4 asks the user to open the Goals tab. If the card read its imperative straight off the step,
stepping **back** into it from step 5 would demand the tap again — from the Goals screen, where the
tab is already open, so the tap changes no route, so nothing completes and the card has **no Next
button on it**. Found by writing `TutorialControllerTest`, not by using the app. So *has the user
done this yet* is recorded, and a replay clears it, because a replay that silently skipped its one
interactive moment would be a different tour from the one the user asked to see again.

### 3.5 The preference is an `Int`, not a `Boolean`

`hasSeenTutorial` can answer *this install has run the tour* and can never answer *this install has
run **this** tour*. The day a step is added for a feature that did not exist, every existing user is
precisely the group that has not seen it — and the flag says they have. `TUTORIAL_VERSION` costs the
same four bytes.

---

## 4. Two defects. One was designed out; the other reached the emulator

### 4.1 A race, designed out

Completing an action step and navigating for an informational one are two rules over the same two
inputs. As two `LaunchedEffect`s they race: the frame the user taps *Goals*, one advances the step
and the other — still holding the old step, which says it belongs on the dashboard — navigates
straight back. Which one wins depends on composition order, so it would have worked on the machine
it was written on. `TutorialHost` runs them as **one ordered effect**, where the question does not
arise.

### 4.2 ⚠️ A silent no-op that every test passed, and only the device found

`Observed:` on `emulator-5554`, after the suite was green and the seven steps had each been
screenshotted. **Replaying the tour from Settings started the tour and left the app on Settings**,
with step one's overlay drawn over the Settings screen it was describing the dashboard's card on.

Everything upstream was correct. Temporary logging in `TutorialHost` showed the decision taken
(`effect state=WELCOME route=settings` → `navigating to dashboard`), `navigate()` called, no
exception, and the route unchanged one frame later (`after=settings`). The tour's navigation had
been sharing the bottom bar's move — `popUpTo(start) { saveState = true }` + `restoreState = true`
— and that pair makes `navigate` do **nothing at all**, silently, when the target is the start
destination and there is a saved-state entry for it.

**Why the whole suite missed it, which is the part worth keeping.** The identical call from the
**Goals tab** works. So a session that walks the tour forwards — which is what testing a tour
means, and what the seven screenshots did — passes every step, because the tour's own two
navigations are both from a tab. Only the entry point a real user reaches it from is broken, and
it is broken with no exception, no log and no visible symptom other than the wrong screen behind
the scrim.

**The fix is a second function, not an edit to the first.** `navigateForTutorial` drops the
save/restore pair; `navigateToTab` keeps it, because remembering a tab's scroll position is exactly
what the bottom bar is for. Two callers with genuinely different requirements now say so.

**The regression test was checked against the defect, not just written.** `TutorialNavigationUiTest`
drives the real function over a real `NavHost` from a screen pushed above the start destination.
Reverting `navigateForTutorial` to the tab options and re-running gives `expected: dashboard but
was: settings` on that case and **passes the other three** — which is the same asymmetry the defect
had, reproduced mechanically.

`Inferred:` the mechanism inside `NavController` (a restore branch that finds nothing and does not
fall through to an ordinary navigate) is read off the symptom, not off androidx's source.
`Untested:` that reading. What is tested is the behaviour.

---

## 5. Files

**New — `app/src/main/java/com/idomarhaim/goalpilot/ui/tutorial/`**

| File | What it owns |
|---|---|
| `TutorialAnchor.kt` | the six anchors, the registry, `Modifier.tutorialAnchor` (which also scrolls its own node into view via `BringIntoViewRequester`) |
| `TutorialStep.kt` | the seven steps as data, `TutorialAction`, `TUTORIAL_VERSION` |
| `TutorialController.kt` | the sequence — `@Singleton`, no Android types, JVM-testable |
| `TutorialOverlay.kt` | scrim, cutout, pulse ring, blockers, card, single-pass placement |
| `TutorialHost.kt` | the one piece of wiring; `TutorialViewModel` |

**New — resources and tests**

- `app/src/main/res/values/tutorial_strings.xml`, `values-iw/tutorial_strings.xml`
- `app/src/test/.../ui/tutorial/{TutorialControllerTest,TutorialStepsTest,TutorialPlacementTest}.kt`
- `app/src/test/.../testing/FakeAppPreferences.kt` — extracted from `HealthSyncTest`'s private copy
- `app/src/androidTest/.../ui/TutorialOverlayUiTest.kt` — the overlay as an obstacle
- `app/src/androidTest/.../ui/TutorialNavigationUiTest.kt` — §4.2's regression

**Changed**

- `ui/root/GoalPilotRoot.kt` — anchor registry provided app-wide, overlay hoisted above the
  scaffold, `navigateToTab` extracted for the bottom bar and `navigateForTutorial` added beside it
  (§4.2)
- `domain/repository/AppPreferencesRepository.kt` + `data/prefs/AppPreferencesRepositoryImpl.kt` —
  `tutorialSeenVersion`
- `feature/dashboard/DashboardScreen.kt` — three anchors; `PointsLevelCard` and `SmartAddCard` gain
  a `modifier` parameter
- `feature/goals/GoalsScreen.kt` — the FAB is an anchor
- `feature/settings/SettingsScreen.kt` — the **Help** section
- `feature/settings/SettingsViewModel.kt` — *unchanged, deliberately*: the replay is a callback from
  the root, so §4.9's one-repository rule is not spent on a tour
- `androidTest/.../{SettingsScreenTest,MaterialPickerUiTest,MaterialRenderPass}.kt` — the new
  `SettingsContent` parameter, plus two cases for the Help row
- `test/.../domain/HealthSyncTest.kt` — uses the shared fake

---

## 6. 🧪 Tests

| Layer | Result |
|---|---|
| Server unit / integration / endpoints | **n/a** — this change touches no Cloud Function |
| Database (`firestore-tests/`) | **n/a** — the tour writes nothing to Firestore; its one stored value is device-local `SharedPreferences` |
| Client component (JVM) | **782 / 0** across 73 classes — `TutorialControllerTest` (15), `TutorialStepsTest` (8), `TutorialPlacementTest` (7) are new |
| Client page (JVM) | covered by the three above; the overlay is stateless, so its logic is `placeCard` |
| UI E2E (instrumented) | `TutorialOverlayUiTest` **10 / 0** (new) · `TutorialNavigationUiTest` **4 / 0** (new) · `SettingsScreenTest` **17 / 0** (+2) · full suite **223 / 0** on `emulator-5554` |

**What the JVM layer cannot reach, and why the device cases are the ones they are.** The scrim's
job is to be an obstacle, and *did this tap reach the app* is a hit-testing question that no unit
test can ask. So `TutorialOverlayUiTest` puts a counting tap target under the overlay and clicks
through it: inside the spotlight with an action pending (**reaches the app**), outside it (**does
not**), and inside it on an informational step (**does not** — the hole is a hole in the dim, not in
the blocking).

**And what the device cannot reach, which is why `placeCard` is a JVM test.** The card's placement
has three branches — below the target, above it, centred over it — and which one fires depends on
the *screen*, not the code. The project's AVD is tall enough to put the card below every anchor, so
looking at it exercises one branch and reports that placement works.

---

## 7. A sibling landed mid-session, and what it actually cost

`53-material-naming` committed **and pushed** `ccb927e` + `df27072` while this work was running — a
re-claim of its own row for a KB drain, then that row released again. So `git log @{u}..HEAD` before
this commit is **empty**: their work was already on the remote, and this push carries none of it.
Recorded anyway, because the check that established it is the one that matters and it was run at the
gate rather than assumed.

**The real hazard was the shared file, not the commits.** This session's `SESSIONS.md` was a copy
read *before* those two commits landed, and writing it back would have destroyed their release note
— 22 lines of somebody else's account of their own work, with nobody watching who could notice. It
was rebuilt from `HEAD` and the one row re-inserted; `git diff -- SESSIONS.md` then reads **1
insertion**, which is the check, not the intention.

That is the pathspec remedy's known hole: `git commit -- <paths>` protects a sibling's *other*
files and cannot protect one that is in both pathspecs, and `SESSIONS.md` is that file by
construction.

---

## 8. Sources

- [Onboarding — Material Design](https://m2.material.io/design/communication/onboarding.html)
- [Coach Marks: Definition, Examples & Best Practices](https://www.docsie.io/blog/glossary/coach-marks/)
- [Mobile App Onboarding Best Practices](https://www.eleken.co/blog-posts/mobile-app-onboarding-best-practices)
- [Mastering Coach Marks for Effective Mobile App Onboarding](https://www.go2blog.com/2026/05/mastering-coach-marks-for-effective-mobile-app-onboarding/)
- [Reveal — coach marks for Compose Multiplatform](https://github.com/svenjacobs/reveal)
- [A Deep Dive into Compose's `onGloballyPositioned`](https://proandroiddev.com/from-pixels-to-perfection-a-deep-dive-into-composes-ongloballypositioned-cc3ad0d8ab01)
