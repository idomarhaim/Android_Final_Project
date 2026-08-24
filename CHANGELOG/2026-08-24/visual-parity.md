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
