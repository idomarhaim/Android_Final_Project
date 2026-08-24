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
