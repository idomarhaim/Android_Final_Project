# s25-layout-and-tour — 2026-08-24

> **Summary:** Ido's four defects from his Galaxy S25 Ultra, fixed and each verified on a device at his own geometry — a row that starved goal titles to one glyph, a `#` nobody could read, the two sync cards moved Home → Settings, and a tour that ringed controls it would not let him press. Shipped as v0.4.0.

## What this was

One message from Ido, with six screenshots and his phone plugged in. Four
complaints, all of them real, none of them caught by the green suite:

1. Things render differently on his S25 Ultra than on the emulator.
2. A picture of `#` he could not interpret.
3. The Health and Google sync cards should be in **Settings**, not on Home.
4. The tutorial: no replay button he could find, and when it marked a button to
   press (*"for example the calendar"*), he never saw it open what he pressed.

## 1 · The starved row — `ui/components/SuccessFailureRun.kt`

### The defect, measured rather than inferred

`NoNextStepSection` drew each goal as one `Row`: a 20.dp mark, a
`Modifier.weight(1f)` column holding the title and meta line, then two
unweighted `TextButton`s. A Compose `Row` measures **unweighted children first,
against the full incoming constraints**, and hands the remainder to the weighted
ones. *Schedule the first one* + *Let it go* will take everything they want.

**Reproduced on the emulator at Ido's exact geometry** (`wm size 1080x2340`,
`wm density 480` → `sw360dp w360dp`, confirmed in `dumpsys window displays`),
against the **pre-fix APK already installed**, via `uiautomator dump`:

| node | before | after |
|---|---|---|
| `'Learn to play the saxophone'` | **40px wide** (13dp), h=49px | **524px** (175dp), h=49px |
| `'no next step'` | 40px wide × **205px tall** — a vertical letter stack | **197px** × 39px, one line |
| `'Schedule the first one'` | 410px | 410px — unchanged |
| `'Let it go'` | 153px | 153px — unchanged |

The emulator's own default is **448dp**, which is why nothing looked wrong here:
at 448dp the title still got ~100px and merely truncated (`Learn to play t…`),
which reads as tight rather than as broken. 88dp of width is the whole
difference between *tight* and *one letter per line*.

Frames: `before-360dp.png`, `after-360dp.png` (scratchpad; not committed —
they are a reproduction, not an artefact anything depends on).

### The fix, and the three that were rejected

The actions get their own line, end-aligned, under the title. Title `maxLines`
1 → 2, because it now has a full row.

- ❌ **Weight the buttons too** — the labels truncate instead, and a control
  reading `Schedule the fi…` is worse than a wrapped title.
- ❌ **Make them icons** — §0.8 *form and words before iconography* forbids it,
  and it is the same rule that sent the `#` to a word in this same commit.
- ❌ **Truncate the title harder** — it was *already* `maxLines = 1` with an
  ellipsis. That did nothing, because the column was never given the width to
  ellipsise **into**. Worth recording: the obvious remedy was already in place
  and was invisible in the code review that let this ship.

## 2 · The `#` — `ui/components/UnmeasuredMarker.kt`

The glyph stood for *a number slot*, the way `+` does on `C19`'s marker beside
it. Ido is the person the app is for and he could not read it, which is the only
test a glyph has to pass. §0.8 already had the answer.

Split by **what is beside it**, and neither half is a symbol now:

- **`UnmeasuredChip`** (new) — the inline list slot, where the marker *replaces
  a percentage* and no sentence is in the same glance. Dashed hollow outline,
  the words `No number`. This is what `UnmeasuredMarkerIfNeeded` renders, so
  `GoalCard` and the life-area rows get it.
- **`UnmeasuredMarker`** — the hero sizes (goal header 72.dp, home card 56.dp).
  Now an **empty** dashed square. Both call sites already print the whole
  sentence — *No number yet — nothing logged* — so the object's job is to be a
  visibly empty slot. The glyph was never carrying meaning the sentence did not;
  it was competing with it.

Both draw from one `Modifier.dashedSlot`, because §1.3's claim is that they are
*the same object at two sizes* and two `drawBehind` blocks drift.

TalkBack is unchanged: `components_goal_unmeasured` was always the accessible
text and the visible words are `contentDescription = ""` so it is not read
twice.

*Rejected:* a tooltip (a mark needing one has failed, and there is no hover on a
phone); `?` or `—` (one symbol for another, and a dash reads as **zero**, the one
thing §1.3 exists to stop the app claiming).

## 3 · The sync cards moved Home → Settings

A placement decision, and Ido's. What made it cheap is that the coupling being
undone was **accidental**: this state describes *the device's relationship with
two other applications* and it was living in a ViewModel about *today's goals and
tasks*, purely because the cards happened to be drawn on that screen.

New package `feature/sync/`:

- **`SyncViewModel`** — ~400 lines and six constructor dependencies out of
  `DashboardViewModel`. Nav-entry scoped, which is **only safe now**: two
  destinations reaching for one `@HiltViewModel` through their own back-stack
  entries get two instances, each running its own Firestore reads and each
  holding half the truth about a consent state.
- **`SyncCards`** — the two cards and the import dialog, moved verbatim.
- **`SyncSection`** — a `@Composable` slot `SettingsContent` takes. It has to be
  a slot rather than state-in/edits-out like the rest of that screen: it
  registers two `ActivityResultContract` launchers, and only a composable may.
  `null` on the signed-out branch, exactly as `onReplayTutorial` already is.

**No warm caches**, unlike the dashboard: an import is a deliberate button press
that already waits on Google, so the three collections are read once at the
moment of import (`snapshot()`). Fresher, and three long-lived collectors fewer
on a screen the user is usually not on.

⚠️ **The automatic health sync was never here.** `RootViewModel` fires it on
`APP_FOREGROUND`, so moving the card changes nothing about whether steps and
sleep are logged. Checked before moving anything, because getting that wrong
would have silently stopped the feature.

`Int?.durationSource()` moved to `domain/model/TaskEstimate.kt`. It was private
to `DashboardViewModel` with a KDoc saying *"one function rather than two call
sites so the quick-add sheet and the Google Tasks import cannot drift"* — and
the move made that sentence false in the same commit unless it got a shared
home.

## 4 · The tour

### 4a · The replay control existed; nobody could find it

It was the **fourth** of six sections in Settings, below three tall cards. A
replay control nobody can find is what makes *Skip tour* a one-way door, and
that door is the single promise the whole overlay rests on. **Help is now the
first section in Settings.** Costs Appearance one scroll; it is one short card.

### 4b · The spotlight lied, and then ate the gesture — twice

**First defect.** `TutorialBlockers` derived *is the hole live* from
`onTapThrough == null`. On an informational step with a spotlight that meant the
hole was **covered**, so a tap on the pulsing ring hit the scrim, whose
informational behaviour was *advance the tour*. Ringing the Calendar tab, then
swallowing the press it invited.

Two parameters now, because they are two facts: `holeIsLive` (the step is
offering the widget) and `onTapThrough` (which now fires **only** where the step
points at nothing — `WELCOME`). `CALENDAR` gained an action with
`required = false`: the hole is live, and Next stays on the card.

**Second defect, and it was only found by running it.** The first fix worked —
the Calendar opened and the tour advanced — and step seven lives on the
dashboard, so `TutorialHost` navigated **straight back to Home**. The calendar
was up for about one frame. That fails *"I want to see the result of what I
press"* exactly as completely as never opening it. **The first fix was not
wrong; it was half**, and reading the code would not have shown it.

So `TutorialUiState` gained `route: String?`, where `null` means *stop
steering*. Performing an **invited** action does not advance: it clears the
imperative, puts Next on the card, and nulls the route, so the tour waits with
its explanation on screen while the user looks at what they opened. A
**required** action still advances on the spot and needs none of this — the step
after `GOALS_TAB` lives on the very route that tap reaches, which is why that one
never showed the defect.

`TUTORIAL_VERSION` 2 → 3. It is a different tour, and the one person who ran the
old one is the person who reported it.

### What is deliberately NOT pressable

`PROGRESS` points at a card that does nothing. `QUICK_ADD`, `NEW_GOAL` and
`WHERE_SETTINGS` point at controls that open a keyboard, a form and a bottom
sheet — the overlay is drawn **above** the scaffold, so each would surface under
the scrim, and `NEW_GOAL` is worse than cosmetic: the next step lives on `GOALS`,
so the host would navigate straight back out of a half-typed goal. A live hole is
honest only where the press produces a **destination the tour can follow you to**.

## 🧪 Tests

**JVM unit — 1,093 tests, 1,093 pass, 0 fail** (`:app:testDebugUnitTest`),
counted off the JUnit XML rather than read off the console, which only
prints a total when something fails.

Three failures were hit and fixed on the way, and each was the suite doing its
job rather than noise:

| failure | what it was actually reporting |
|---|---|
| `HebrewLocaleResourceTest > every translatable english string has a hebrew counterpart` | the three new strings had no `values-iw/` entry. §0.8 is suspended but this guard is app-wide and stays armed. Added. |
| `TutorialControllerTest > finishing the last step ends the tour and records the version` | **a real behaviour change**: an invited step now takes two beats (perform, then Next), so `repeat(count)` ends one short. The helper was fixed to mean *one step*, not *one call*. |
| `:app:kspDebugKotlin FAILED` with no `e:` lines | the known Windows KSP file lock (`CLAUDE.md`). Passed unchanged on re-run. |

**Tests added — 5:**

- `TutorialControllerTest`: *an INVITED action does not advance — it stops the
  tour steering* · *a REQUIRED action still advances on the spot* · *Next past a
  resolved invitation moves on and steers again*.
- `TutorialStepsTest`: *every step that points at a live control offers to open
  it* — a step anchored on a bottom-bar tab must carry an action, because the
  overlay opens the hole exactly when there is one.
- `TutorialStepsTest`: *the tour DEMANDS exactly one gesture* — **narrowed**
  from `action != null` to `action?.required == true`. Before today those were
  the same fact. Counting the invited one would forbid the fix Ido asked for
  while claiming to protect a tour nobody is being made to work through.

**Instrumented — NOT RUN.** `TutorialNavigationUiTest` and
`TutorialOverlayUiTest` are the two suites most likely to be affected by the
overlay change, and they were not executed: the emulator was in use for the
manual verification below, and `connectedDebugAndroidTest` uninstalls the app,
which would have destroyed the signed-in state every one of these checks needed.
**Open, and named as such in the reply.**

**Manual, on a device, at Ido's geometry — 6 checks, all pass.** This is the
layer that mattered, because every defect here was invisible to the suite:

1. Pre-fix APK at 360dp reproduces his screenshot exactly (measured above).
2. Post-fix, every goal title renders on one readable line.
3. `#` gone; `No number` chips render in the trailing slot, TalkBack text intact.
4. Home carries neither sync card (`grep` over the full scrolled hierarchy: 0).
5. Settings opens on **Help → Replay tutorial**, then **Connected apps** with
   both cards.
6. The tour: tapping the spotlighted Calendar tab opens the Calendar **with his
   real entries on it**, the tour stays on step 6, and Next moves on.

## Docs corrected in passing

Both were assertions about code that this commit made false, and both are the
`kb/dev/product-copy-describes-code.md` shape:

- **The avatar sheet's own subtitle** — *"Appearance, language, your day, AI"*.
  It listed four sections; Settings now has six, and the tour is the one a
  person looking at that sheet is most likely to be hunting for.
- **`docs/ARCHITECTURE.md`** — "Eleven feature packages" → twelve, and `sync/`
  gets a sentence saying it is the one that is not a screen.
- **`AGENTS.md` §Where things live** — the `feature/` list was missing
  `calendar`, `settings` and `health` before today, and `sync` after it.

## Shipped

`versionCode` 8 → 9, `versionName` 0.3.3 → **0.4.0**. Release notes rewritten
for the four fixes, in Ido's terms rather than the code's.

**Distributed by the local route**, not the tag route — `:app:assembleRelease`
then `:app:appDistributionUploadRelease`. Firebase App Distribution release
**`5sgpd1si43tu0`**.

Signature verified before upload rather than assumed, because a debug-signed
build is the failure that is only discovered months later:

```
Signer #1 certificate DN: CN=Ido Marhaim, OU=GoalPilot, O=GoalPilot, L=Tel Aviv, C=IL
Signer #1 certificate SHA-1 digest: e7d5534cb6ce2fd81a48af9d1304be254dfc9062
package: versionCode='9' versionName='0.4.0'
```

That SHA-1 is the one `RELEASING.md` §2.1a records as registered with Firebase,
so this is the real key and the build installs over what testers already have.

**Both recipients were already testers** — `firebase appdistribution:testers:list`
shows `name.iddo@gmail.com` and `rachil751@gmail.com` both in the `testers` group,
rachil since 2026-08-06. So one upload reached both and **nobody was invited, no
new email address was added anywhere**. Ido asked for it to go to rachil by email;
App Distribution is that email, and the account was already there.

**Not pushed.** Precondition 5 — the range carries `3e4f381` and `59283d0` from
`docs-repair`, which holds a **live** row and is mid-unit. Held for Ido's word,
and still unpublished as of the `git fetch` run at the end of this session.
