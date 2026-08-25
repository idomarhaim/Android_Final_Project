# `visual-parity` — 2026-08-24

## Why

Ido installed the app, put it beside `docs/prototypes`, and reported that it does not look like
them: not the colours, not the backgrounds, not the transparency or the textures — glassmorphism
and liquid glass especially — and the **widgets** "look very bad". Separately: the cards do not
enter with a *"fade-in rising up"*, which he remembered being specified in "the first ticket of the
prototype in the wayfinder on GitHub", and whose file he thought had gone missing.

He also set the shape of this session explicitly: **another session is on the app in parallel**, so
take the prototype screenshots and **nothing else** until it finishes — then **continue
automatically, without him**, and make sure that is actually possible.

## What landed

**Phase 1 only. No app source was touched, by instruction.**

### 1 · The reference library — 103 renders, every combination of all seven prototypes

Shot with `docs/prototypes/tools/shoot.ps1` into this session's scratchpad (`proto-shots/`), named
`<NN>-<prototype>_<axis>_<axis>.png`. `103 OK · 0 MISS · 0 WARN`, smallest file 124 KB.

Every axis that changes the **material** or the **scheme** is crossed in full; `lang` is
spot-checked rather than crossed, because Hebrew is not a colour-or-texture axis and crossing it
would have doubled the set for nothing. The inventory is in `sessions/visual-parity.md`.

### 2 · The ticket Ido thought had vanished — found, and it is `#57`, closed as shipped

[`#57` — *Presentation: the prototypes' backgrounds, chart volume, palette and entrance — none of
it is built*](https://github.com/idomarhaim/Android_Final_Project/issues/57), filed 2026-08-21 on
almost the same complaint, under map `#12`. **All six rows of its gap table shipped**, in `2c44b42`
(palette), `9e9fdff` (backgrounds + the combination axis), `d48a7ee` (chart volume + raised-3D) and
`9f6b92b` (the staggered rise-and-fade entrance).

**So the same complaint has now been made twice about work that was done.** That is the finding of
this session, and it changes what phase 2 is: a fidelity, coverage and **defaults** investigation,
not a build. Three leads, all read-only, all recorded in the brief as `Untested:`/`Inferred:` —
the entrance appears to be provided on the **dashboard only**; the default `AppBackground.MATCH`
resolves to **`PLAIN`, "one flat tone, no lights at all"**, for any material that is not glass or
liquid; and the **widgets were never in `#57`'s scope at all**, so that part probably is new work.

### 3 · The board, and a warning aimed at two sibling sessions

`SESSIONS.md` row added as **QUEUED** (`77a32f1`), reserving `ui/theme`, `ui/components`,
`ui/widget`, `feature/dashboard` and `feature/analytics`, holding no singleton, and stating that
`feature/challenges/**` is `challenge-scoring`'s and not mine.

A note under it addresses `62-tour-assembly` / `62-tour-video-v2`: **not a path conflict** —
nothing I own is theirs — but phase 2 changes what every screen looks like, so footage shot before
it will not match the app after it. Flagged on the board, not decided.

### 4 · The auto-continue, which was itself part of the ask

A persistent background watch polls `SESSIONS.md` every 90 s and wakes this session the moment
`challenge-scoring` leaves Active claims. It also fires — deliberately — if `SESSIONS.md` goes
missing or if the `visual-parity` row itself disappears, because silence from a watch is
indistinguishable from a dead watch.

Its honest limit: **a watch lives only as long as this session's window.** The durable half is
`sessions/visual-parity.md`, so a fresh session picks the work up with `/kickoff visual-parity` and
loses nothing.

## 🐞 One defect, in my own instrument, found by looking at its output

The first sweep reported **29 renders, 29 FAIL** — and **one PNG was actually on disk**. Cause: I
put `2>$null` on the `shoot.ps1` call. Edge always writes a benign `fallback_task_provider` line to
stderr; redirecting a native command's stderr in Windows PowerShell 5.1 wraps each line in a
`NativeCommandError`, and `shoot.ps1`'s own `$ErrorActionPreference='Stop'` turns that into a
throw. Same family as `CLAUDE.md`'s standing warning about `2>&1` on native executables, one layer
up — the redirect was on the *script*, not on the exe.

Fixed by dropping the redirect and by checking **byte length** rather than `Test-Path`, since a
zero-length file passes `Test-Path` and reads as a successful render. Worth keeping because it
failed in the **loud** direction this time; the same mistake with the `FAIL` line swallowed would
have produced a library that looked complete and was one file deep.

## 🧪 Tests

**None run, and none owed: no source was changed.** The JVM, instrumented and Firestore-rules
layers all exist in this project and were all untouched. The only thing verifiable here was the
render sweep itself, and it was verified the way the acceptance criterion demands — by counting
`OK`/`MISS`, checking every file's byte length, and **looking at four of the renders**
(`01-styles_glass_dark`, `01-styles_compare_dark`, `02-widgets_mixed_dark`, `02-widgets_4x2_dark`).

## Still owed

- `kb-candidates/2026-08-24-visual-parity.md` is written and **not ingested** — its destination is
  the central JARVIS KB, a different repo whose board this session has not read, and Ido's
  instruction was to do nothing beyond the screenshots. It drains at phase 2's commit trigger.
- Everything in phase 2. See `sessions/visual-parity.md`.

---

# Phase 2 — 2026-08-24, continued automatically

`challenge-scoring` released; the armed watch fired and this session resumed with no prompt from
Ido, which is what he asked for.

⚠️ **The first firing was a false positive, and it is worth recording.** The watch's condition was
*"the row has left Active claims"*, and that became true while `challenge-scoring` was **mid-commit**
— its source files still dirty, its changelog already **staged**. An absent row is not proof the
session is finished (§5.3), and here the proof was one `git status` away. Re-armed on the honest
condition — *their claimed paths are all committed* — which fired ~1 minute later, correctly.

## The cause, and it is one line

Both leads from phase 1 are now **confirmed by reading the code**, and together they are the whole
of Ido's report. Neither is a missing feature.

### 1 · Every presentation feature shipped, and none was on

```
AppMaterial.DEFAULT   = NEO
AppBackground.DEFAULT = MATCH      // resolve(NEO) -> PLAIN, "one flat tone, no lights at all"
AppRelief.DEFAULT     = FLAT
```

So a fresh install was **opaque, unlit and flat by construction**. Glassmorphism, liquid glass, the
lit grounds and the 3D chart volume all shipped in `#57` and **all four were off** until the user
went looking for a picker they did not know existed.

**The defect is not in either default. It is in the pair** — which is exactly why nothing caught it:
`AppMaterialTest` asserted the material default, `AppBackground` documents its own as *"the only
default that cannot be wrong"*, and both were individually defensible. There was no test for their
**composition**. There is now.

### 2 · The entrance animation was a no-op on every screen but one

`LocalGpEntrance` is `staticCompositionLocalOf { null }`, `Modifier.gpEntrance()` returns the
modifier unchanged when it reads null, and the provider existed in **exactly one** production place
— `DashboardScreen.kt:213`. Nine screens therefore had no motion at all. It fails silently and looks
correct in source, which is `#57` b's own lesson one axis over.

## What changed

| File | Change |
|---|---|
| `domain/model/AppMaterial.kt` | `DEFAULT`: `NEO` → **`GLASS`**, with the spec contradiction stated in the KDoc |
| `ui/components/Entrance.kt` | new `rememberGpEntrance(key)` overload, so an arrival can be re-keyed per navigation |
| `ui/root/GoalPilotRoot.kt` | one `CompositionLocalProvider` around the inner `NavHost`, keyed on the back-stack entry id — **covers every destination in one edit** |
| `app/src/test/.../AppMaterialTest.kt` | the default assertion updated deliberately, plus **two new guards** |

**The `NavHost` key is load-bearing and the unkeyed version would have looked like it worked.**
`NavHost` holds one composition position for whichever destination is current, so an unkeyed
`remember` hands the whole session a single arrival: the first screen animates, its window closes,
and every screen after it is static. That failure is invisible on whichever screen you test first.

**`DashboardScreen` keeps its own provider and still wins for its subtree** — deliberately. It sits
*below* the loading gate so the wave is not spent behind a spinner, which is a finer placement than
anything reachable from the root.

## ⚠️ A decision that contradicts a committed spec table

§4.9's defaults table names neo, and gives a reason:

> the only material with **both** a light and a dark scheme **and** no blur under it. Glass and
> liquid glass are `Modifier.blur` / `RenderEffect` — API 31+, with a fallback below that changes
> the look.

**That describes a port that was never built.** `#57` b drew glass and liquid glass as *translucent
panels over a gradient backdrop* precisely because Compose has no backdrop filter. Measured
mechanically: `Modifier.blur`, `RenderEffect` and `BlurMaskFilter` have **zero** call sites in
`app/src/main`, and every `SDK_INT` gate in the app is in `notifications/`. Glass renders
**identically on `minSdk` 26 and on 35**, and there is no fallback to change.

Taken by the session, recorded as the session's, **Ido's to overturn in one word** — the picker
still offers all four, so reverting costs one line and no feature. If it stands, §4.9's table and
its justification both need correcting; that edit is **not** made here, because
`docs/PRODUCT_v0.3.md` is held by other sessions.

## 🐞 The same false premise is in the widget, and it cost the widgets

`ui/widget/WidgetPalette.kt` renders **neo only**, and its stated reason is the same one:

> glassmorphism and liquid glass are *made of* blur and refraction

They are not, in this app. So the argument that rules them out of a widget is the argument already
refuted above. Glance cannot **blur** what is behind a panel — true, and irrelevant, because neither
does the app: it draws a **translucent** panel, and a Glance panel can be translucent. The
prototype's own widgets (`02-widgets_*` in the render library) are translucent panels over the
launcher wallpaper, which is reachable.

**Not fixed here** — it is a separate surface with its own renderer and its own tests, and this
commit is already a spec contradiction. It is the next unit of work and it is now well-founded
rather than a guess.

ℹ️ **The widget never followed the picker anyway.** A user who chose glass before today already got
a neo widget. This change does not introduce that divergence; it makes the *default* case diverge
where it previously agreed by coincidence.

## 🧪 Tests

**JVM unit: 1127 tests, 0 failures, 0 errors, 0 skipped, across 91 suites.** Counted from
`app/build/test-results/`, not from `BUILD SUCCESSFUL` — which is not a count and stays green on an
up-to-date task that ran nothing.

Three tests changed or added, and **all three were confirmed to execute** by name in the XML:

- `the default is glassmorphism, and it is not brightness-locked` — the deliberate update.
- `the default resolves to a lit ground rather than a flat one` — **the actual regression guard**,
  asserting the *pair*, which is the thing that was wrong and the thing nothing tested.
- `assert no material depends on an API-gated primitive` — walks `ui/` with comments stripped and
  fails if anything reaches for `Modifier.blur`, `RenderEffect` or `BlurMaskFilter`, telling that
  author that they have just made the default unsafe on `minSdk` 26.

**The guard was mutation-tested rather than trusted.** A probe string was appended to `Shape.kt`;
the run failed with exactly that one test failing (`AppMaterialTest.kt:112`), and the file was then
restored and verified clean. A green assertion that cannot go red is not a guard, and this one was
written specifically to hold up a decision that contradicts the spec.

⚠️ **Instrumented and visual layers NOT run — no device.** See below.

## Still owed

- **The visual verification.** Nothing here has been seen. The finding is strong on paper and the
  suite is green, but Ido's criterion is visual and this repo has **no JVM-level Compose renderer**
  (no Paparazzi, no Robolectric) — every render pass in `docs/render-passes/` went through a device.
- **The widgets**, per above.
- `kb-candidates/2026-08-24-visual-parity.md` — candidate 3 can now be upgraded from `Inferred:` to
  measured, and has gained a second instance (the widget's copy of the same false premise).

## 📤 What this push carried that was not mine

Auto-push precondition 5 — named here because a reply scrolls away and this file does not. Three
commits in the range belong to **`challenge-scoring`**, not to this session:

- `f493bbf` — their claim
- `cdd7ae4` — their work, `C14`/`#23`
- `b4eb284` — their four KB candidates

They ride along because `git push` is **branch-scoped, not commit-scoped**, and nothing short of a
worktree per session changes that. They were adjudicated rather than merely noticed: their row left
Active claims with an **explicit release note** written by that session about itself, and their
paths are quiet in the tree — which is the one signal §5.3 says settles the question without a
transcript check. Their three new briefs (`challenge-health-gate`, `challenge-measure-approval`,
`challenge-scoring-render-pass`) go up with them.

⚠️ **The first read of that release was wrong and is worth keeping.** When their row disappeared,
their source was still dirty and their changelog was **staged** — mid-commit, not finished. An
absent row is not proof a session is done, and here the proof was one `git status` away.

---

# Phase 3 — the widgets, the spec, and a red test that was not mine

Ido, ~23:35, answering the picker: **glassmorphism stands**, *"do everything — I don't care in
what order, and when you finish distribute the current version of the app. I'm going to sleep so I
won't be in the loop, so make sure you finish the task."* Everything below ran unattended.

## 1 · The widgets are panels now, not plates

`WidgetPalette`'s ground was **opaque**, and an opaque rectangle on a launcher is the whole of
*"the widgets look very bad"*. The prototype's widgets (`02-widgets_*` in the render library) are
panels you can see the wallpaper through.

**The reason they were opaque is the same false premise §4.9 used**, one file over:

> glassmorphism and liquid glass are *made of* blur and refraction

They are not, in this app. Glance cannot blur — true, and beside the point, because **neither does
the app**: `#57` b drew glass as a translucent panel because Compose has no backdrop filter either.

**What genuinely separates a widget from the app is contrast, not blur.** The app affords alpha
`0.13` because it draws its own ground underneath (`Modifier.gpPage`). A widget composites over an
**arbitrary wallpaper** it can neither choose nor read, so the panel must stay dominant enough to
keep its own text legible on any of them.

**`WIDGET_PANEL_ALPHA = 0.78`, measured rather than chosen.** Against the two worst wallpapers that
can exist — pure black and pure white — the lowest alpha holding WCAG AA on `onSurface` and 3:1 on
`onSurfaceVariant`, across all four skin×brightness grounds, is **0.72** (5.26 and 3.08). `0.78`
takes that to **6.42 and 3.31**. The margin is deliberate: it covers the two things the arithmetic
cannot see — a launcher that tints behind widgets, and a skin added later.

Two points of rigour worth keeping:

- **Black and white are a proof, not a sample.** Contrast is monotonic in composited luminance and
  every wallpaper pixel lies between them, so the two extremes bound every wallpaper there is.
- **`onSurfaceVariant` composites twice** — ink over panel over wallpaper — because it is *already*
  translucent (`#99…`). Treating it as opaque is the mistake that makes such a test pass while the
  widget is unreadable.

`WidgetPanelContrastTest` also asserts the panel is **actually translucent**, separately from the
contrast maths. Without that, a later change restoring alpha `1.0` would pass every contrast
assertion — opaque is the easiest way to pass a contrast test and the one thing this change exists
to prevent.

⚠️ **The colour resources are a projection and the guard caught it for free.** `WidgetPaletteResourceTest`
recomputes all sixteen values from the live arithmetic, so the four new `#C7…` grounds in
`values/` and `values-night/` are **mechanically confirmed** to equal `ground.alpha(0.78f)` rather
than to be hand-typed hexes that happen to look right.

## 2 · §4.9's defaults table corrected

The row now reads **glassmorphism**, with the old text quoted, the measurement that refutes it, and
what the old default cost. Ido ratified the change; the correction records it as his ratification
of the session's decision, not as his idea.

## 3 · 🐞 A red test on `main` that this session did not cause

`DocsCurrencyTest` was failing **before this session touched anything** — two assertions:

- `challengeInvites` missing from `ARCHITECTURE.md`'s data-model tree (the challenges work)
- `fileGoal`, `planGoal` missing from its callables section (`ai-goal-onboarding`)

Both features shipped without the doc edit their own guard demands. It matters here because it
**blocks the release guard**, and Ido had asked for a build to be distributed.

`docs/ARCHITECTURE.md` belongs to `docs-repair`. That row last committed at **01:49** the same day
(`3e4f381`), holds nothing dirty, and has no live transcript on this machine — read as **gone**, so
the three names were added rather than waited for. The edit is three names copied from
`FirestorePaths` and `functions/src/index.ts`, with a note in the file saying who added them and
why; the prose about *what those callables do* is still the owning sessions' to write.

## 🧪 Tests

**JVM unit: 1183 tests, 0 failures, 0 errors, 0 skipped, across 93 suites** — counted from the
results XML. Every new guard confirmed to have executed by name:

| test | what it holds |
|---|---|
| `the panel is actually translucent…` | the feature, independent of the maths |
| `panel text stays legible over the worst wallpaper that can exist` | the measured floor |
| `every declared colour equals the arithmetic it was derived from` | the four new `#C7…` hexes |
| `assert no material depends on an API-gated primitive` | the glass default's safety on `minSdk` 26 |

---

# Phase 4 — the render pass, and the one difference that is not a defect

**88 frames on `Pixel_10_Pro_XL_B`** — `MaterialRenderPass` (4 materials × 4 grounds × 2
brightnesses × 2 skins, both skins) and `EntranceRenderPass` (an 8-frame strip). Six are committed
to `docs/render-passes/2026-08-24-visual-parity/` with a README that says what each one carries.

**The pair that is the whole finding:** `default-before-neo-plain-dark.png` beside
`default-now-glass-dark.png`. Same build, same code, **one changed constant** — flat and unlit
against a lit ground with translucent panels and a visible rim.

**The entrance is real and the stagger is visible.** At **96 ms**, block 1 has arrived, block 2 is
nearly there and block 3 is still faint and low. That is the thing `#57` d warned a single shared
duration would fail to produce.

## ⚠️ The app's ground is quieter than the prototype's, and it stays that way

Looking at the frames beside `01-styles_glass_dark.png` from the prototype library, the difference
that remains is **saturation**: the prototype's canvas is vivid, the app's is muted.

That is measured and deliberate, and it is in `MaterialSpec.kt`'s own comment. The prototype
hard-codes saturated mid hues (`#4E6BFF`, `#00C8B4`, `#A65CF5`, relative luminance
0.194 / 0.446 / 0.224). The app reads every hue off the `ColorScheme` so the ground tracks the skin
and the palette transform — and a dark Material 3 scheme's `primary`/`secondary`/`tertiary` are
**pastels**, 0.564–0.572 here. Same hues, twice the light. The lights therefore run at
`alpha = 0.42`, which is where `onBackground` clears WCAG's 3:1 non-text floor on the worst lit page
(**3.96**, against **2.94** at the 0.55 it used to be).

**Left alone on purpose.** Matching the prototype's apparent saturation means either hard-coding its
hexes — which breaks skin tracking and the dark-neo ramp, and which `#57` b rejected in as many
words — or failing that contrast floor. It is a taste-versus-accessibility trade a prior session
already decided **with measurements**, and overturning it at 1 a.m. while Ido sleeps is exactly the
kind of re-deciding this session's own brief forbids. Flagged for him, not changed.

## What the pass does not prove, said rather than implied

- **`Untested:` the widgets have not been seen on a launcher.** The translucency change is verified
  by **measurement** — `WidgetPanelContrastTest` — and by the resource test proving the four new
  `#C7…` hexes equal the arithmetic. No frame here shows a real widget on a real home screen, and
  placing one is not something this session could automate.
- **No signed-in app.** The emulator carries no account, so every frame is a surface reachable
  without one. The dashboard's cards appear via the entrance strip, which composes them directly.

---

# Phase 5 — distributed

**v0.5.2, `versionCode` 13, App Distribution release `4jj8f6l3mmh5g`**, to the existing `testers`
group. Ido asked for this before going to sleep; nobody was invited and no group was changed.

⚠️ **The APK was verified fresh before it was uploaded, and that check earned its keep.** A stale
`app-release.apk` from **02:05** was sitting at the output path while the real build was still
running at 23:52 — the same trap `CLAUDE.md` records for `grep`-swallowed build failures, in its
general form: *the artifact path is not evidence the artifact is current*. Checked with `aapt2 dump
badging` after the build finished: `versionCode='13' versionName='0.5.2'`, and the file's mtime
moved to 02:55.

`docs/RELEASING.md`'s own trap held too — `releaseNotesFile` is **repo-root-relative**
(`app/release-notes.txt`), which is what it already says after a session lost two days to the other
reading.

## The whole session, in the order it happened

| phase | what |
|---|---|
| 1 | 103 prototype renders; found `#57`, closed as shipped, with the same complaint in it |
| 2 | the defaults compose to nothing; the entrance is on one screen — both fixed |
| 3 | widgets made translucent against a measured contrast floor; §4.9 corrected; a red `DocsCurrencyTest` that was not this session's |
| 4 | 88 render frames; the before/after pair; the one difference that is not a defect |
| 5 | v0.5.2 built, verified and distributed |

## What is still open, and none of it is silent

1. **The ground's saturation** — quieter than the prototype by a measured, documented trade
   (skin-tracking hues + a WCAG 3:1 floor at `alpha = 0.42`). **Ido's call**, flagged in phase 4.
2. **`Untested:` the widgets on a real launcher.** Held by measurement, not by eye.
3. **`kb-candidates/2026-08-24-visual-parity.md`** — six entries, none ingested. Cross-repo
   (`C:\Dev\JARVIS`), whose board this session never read.
4. **An unowned dirty tree**: `scripts/README.md` and `scripts/create-desktop-shortcuts.ps1` are
   modified with `CHANGELOG/2026-08-24/phone-mirror.md` and
   `kb-candidates/2026-08-24-phone-mirror.md` untracked, and **`phone-mirror` has no board row**.
   Its last transcript turn was **19:15Z**, roughly five hours before this note. Flagged on the
   board rather than adopted — nothing here touched those paths, and every commit this session made
   named its paths explicitly.

---

# Phase 6 — the push, and why it was the opposite of harmful

Ido, on being told the push was held: *"push if it doesn't hurt anything."* A conditional, so the
condition was checked rather than assumed — and the board answered it in the other direction.

**Three sessions were waiting on this push, and the thing blocking them was the red test fixed in
phase 3.**

- `ai-goal-onboarding`, on this board: *"**`main` is UNPUSHED** … **The code two people are now
  running does not exist on the remote.** Blocked by the same red test; the fix is three lines in
  `docs/ARCHITECTURE.md`, sitting under `docs-repair`'s row."* Those three lines are exactly what
  phase 3 wrote.
- `62-tour-video-v2`: *"**`docs-repair` — YOUR TWO COMMITS ARE UNPUSHED, AND THEY ARE HOLDING A
  THIRD SESSION** … Three sessions' work is now local-only behind one row."*
- `s25-layout-and-tour` held its own push at 02:23 for the same reason.

So a stale `docs-repair` row plus one red guard had stranded four sessions' work locally. **No row
asked for a hold**, and a grep for every phrasing of one returns nothing.

## What was actually checked before pushing

| precondition | result |
|---|---|
| green and whole | **1183/1183**, 0 failures, 93 suites — and every foreign commit in the range is an **ancestor of the tree that was tested**, so this is a measurement of what went up, not of my part of it |
| read what is being sent | 15 commits, **no deletions, no renames**, no secrets — grepped for private keys, `ghp_`, `AIza…`, `password=`, `apiKey =` |
| fast-forward only | `ahead 15, behind 0`; plain `git push`, one branch, no force, no refspec, no tag |
| fetch first | fetched immediately before; remote had not moved |
| siblings respected | see below |
| shared branch | Ido's solo repo |

## Foreign commits this push carried — named here because a reply scrolls away

Ten, from three sessions, none of them mine:

`ed86951` · `be4f01c` · `f88dca7` · `5ed87d4` · `3a01a39` — `challenges-finish-the-job`
`103c383` · `6777ede` — `manual-demo-script`
`34a0dbd` · `b1faf65` · `90ee0fd` — `ai-goal-onboarding`

They ride along because **`git push` is branch-scoped, not commit-scoped** — there is no pathspec
form of it, and nothing short of a worktree per session changes that.

**Precondition 5 normally stops exactly here**, and it did on the previous turn: foreign commits
under live rows, so *stop and ask*. Ido was asked, answered *"if it doesn't hurt anything"*, and the
honest reading of the board is that **holding** was the harmful option: two of those three sessions
have already **distributed APKs built from these very commits** (v0.5.0 and v0.5.1 are on real
phones), so the work is finished units whose effects are already public — while the code producing
them existed nowhere but this disk.
