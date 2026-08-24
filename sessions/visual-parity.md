---
repo: C:\Dev\Android_Final_Project
branch: main
mode: AUTO MODE
status: ready
issue: "#57 (closed) — see 'Read this before you build anything' below"
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/ui/theme/**
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/**
  - app/src/main/java/com/idomarhaim/goalpilot/ui/widget/**
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/**
  - app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/**
  - sessions/visual-parity.md
  - CHANGELOG/2026-08-24/visual-parity.md
  - kb-candidates/2026-08-24-visual-parity.md
created: 2026-08-24
---

# `visual-parity` — make the app look like `docs/prototypes`

## Task

Ido, 2026-08-24, having installed the app and compared it with the prototypes:

> The app right now does not look like the PROTOTYPES we made in `docs\prototypes` — not in the
> colours, the backgrounds, the transparency or the textures (especially the background, the
> glassmorphism, the liquid glass). … including the **WIDGETS**, which right now look very bad.
>
> Also — note there are no **motion graphics**, the cards do not appear with a *"fade-in rising
> up"* or anything modern that makes it interesting, like in the first ticket of the PROTOTYPE in
> the WAYFINDER on GitHub (I don't know where that file disappeared to, but it is missing from the
> app).

He also instructed: another session is on the app in parallel, so **take the prototype screenshots
now and do nothing else**; when that session finishes, **continue automatically, without him**.

## ⛔ Read this before you build anything — the ticket he thinks vanished is `#57`, and it is CLOSED as SHIPPED

Ido's *"first ticket of the prototype in the wayfinder"* is
[**`#57` — *Presentation: the prototypes' backgrounds, chart volume, palette and entrance — none of
it is built*
**](https://github.com/idomarhaim/Android_Final_Project/issues/57), filed 2026-08-21 on almost
exactly the same complaint he is making again today. Nothing is missing from GitHub; it is closed,
and the map above it is [`#12`](https://github.com/idomarhaim/Android_Final_Project/issues/12).

**All six rows of its gap table were shipped, in four commits:**

| item | brief | commit |
|---|---|---|
| harmonised category palette | `57a` | `2c44b42` |
| per-material backgrounds + the background × material axis | `57b` | `9e9fdff` |
| 3D chart volume + raised-3D on all four materials | `57c` | `d48a7ee` |
| **staggered rise + fade entrance** | `57d` | `9f6b92b` |
| dark neo | — | already shipped (`#53`) |

So **this session is not a build-from-scratch. It is a fidelity, coverage and defaults
investigation**, and the first honest question is *why does shipped work not reach Ido's eyes?*
Anyone who opens this brief and starts writing a gradient has skipped the only thing that makes
this session different from the last one. `#57` b's own changelog records the trap in this exact
area: **the grounds were being drawn and then painted over by every screen's `Scaffold`**, and the
render pass had the same gap, so *the instrument agreed with the app*. Assume that class of defect
before assuming a missing feature.

### Three concrete leads, all found read-only on 2026-08-24 and none of them verified on a device

1. **`Untested:` the entrance animation is almost certainly only on ONE screen.**
   `ui/components/Entrance.kt` exists and is carried inside `GpCard`/`SectionHeader`, but the
   `CompositionLocalProvider` that switches it on is provided by **`DashboardScreen.kt` only**
   (`grep -rl` over `feature/` returns `DashboardScreen.kt`, `Common.kt`, `Entrance.kt`,
   `GpCard.kt`, `TutorialOverlay.kt` — no Goals, Calendar, Analytics, Challenges, Social or
   Settings). `#57` d's own closing comment says *"The dashboard opts in with one
   `CompositionLocalProvider`"* — as a description of what it built, not as a limitation it flagged.
   **If Ido looked at any screen other than the dashboard, he saw no motion and was right.**
   What would check it: drive each screen and record; see §6.2/§6.6 of
   `C:\Dev\JARVIS\kb\dev\android-device-verification.md` — and note §6.6's warning that
   `screenrecord` is the wrong instrument for a 340 ms animation. Freeze the Compose clock and
   write one PNG per instant instead.

2. **`Untested:` the shipped DEFAULT may resolve to a flat ground.** `AppBackground.MATCH` is the
   default and resolves per material: `GLASS → GLOW`, `LIQUID_GLASS → SPECTRUM`, everything else
   → **`PLAIN`, "one flat tone, no lights at all"**. So if the default material is neo or dark neo,
   a fresh install shows **no background art at all** — which is precisely *"the same backgrounds
   aren't there"*. **Find the default `AppMaterial` first; it was not located in the read-only
   pass** (it is not in `AppMaterial.kt`, `AppBackground.kt` or the pickers — look in the
   preferences repository / settings data class). If the default is a soft material, the cheapest
   fix in this whole session is a one-line default change, and it must be considered before any
   rendering work.

3. **`Inferred:` the widgets were never in `#57`'s scope at all.** Its gap table has six rows and
   none of them is a widget; `ui/widget/**` is Glance, which has **none** of the depth primitives
   (`Modifier.blur`, `RenderEffect`, hand-drawn `Canvas` shadows) that `MaterialSpec.kt`'s KDoc
   already says a widget does not get. So *"the widgets look very bad"* is most likely **genuinely
   unbuilt**, and is the one part of this session that probably is new work. The standard to hit is
   in the render library below (`02-widgets_*`), and `#10` is the widget-pack ticket.

## The reference library — already shot, phase 1 is done

**103 renders** of every meaningful combination of all seven prototypes, produced 2026-08-24 by
`visual-parity` phase 1:

```
C:\Users\namei\AppData\Local\Temp\claude\c--Dev-Android-Final-Project\
  fbe2721b-4e23-4e3a-91e8-60afa0845ef1\scratchpad\proto-shots\
```

⚠️ **That is a session scratchpad, so treat it as a cache and not as the record.** If it is gone,
regenerate it — the driver is committed nowhere, but it is thirty lines over
`docs/prototypes/tools/shoot.ps1`, whose own README explains the instrument. Naming is
`<NN>-<prototype>_<axis>_<axis>.png`.

| prefix | prototype | axes crossed |
|---|---|---|
| `01-styles_*` | `2026-08-11-visual-styles` | 4 materials + compare × dark/light, plus native-canvas, raised-arc, full-fill and Hebrew at dark |
| `02-charts_*`, `02-widgets_*` | `2026-08-10-charts-presentation` | home / home-before / analytics / notes × dark/light; **widgets × 2×2, 4×2, 2×4, 4×4, mixed × dark/light** |
| `03-cal_*` | `2026-08-10-calendar-surface` | answer variants D and E × agenda/3-day/week views × dark/light, plus the explored A/B/C |
| `04-area_*`, `05-log_*`, `07-measure_*` | area success-failure · log progress · measure proposal | 4 materials × dark/light |
| `06-settings_*` | `2026-08-15-c24-settings-surface` | 4 materials × dark/light × aurora/blossom skin |

**`01-styles_compare_dark.png` is the single most useful frame** — the same card in all four
materials on the shared canvas, which is the definition of the target.

### Two traps in the instrument, both paid for already

- **Do not put `2>$null` on the `shoot.ps1` call.** Edge always writes a benign
  `fallback_task_provider` line to stderr; redirecting a native command's stderr in Windows
  PowerShell 5.1 wraps each line in a `NativeCommandError`, and `shoot.ps1`'s own
  `$ErrorActionPreference='Stop'` turns that into a throw. The first run of the sweep reported
  **29/29 FAIL with one PNG actually on disk**. Same family as the `2>&1`-on-native-exe warning in
  `CLAUDE.md`.
- **Check the byte length, not `Test-Path`.** A zero-length or stale file passes `Test-Path` and
  reads as a successful render.

## Plan — phase 2, which is what this brief is for

0. **Re-read `SESSIONS.md`.** `challenge-scoring` must be gone from Active claims, and no new row
   may hold `ui/**`, `feature/dashboard/**` or `feature/analytics/**`. Also re-read the ⚠️ note
   under the `visual-parity` row addressed to `62-tour-assembly` / `62-tour-video-v2` — a visual
   overhaul invalidates their footage, and if that is still unresolved it is Ido's call, not yours.
1. **Answer lead 2 on paper** — find the default `AppMaterial`. Cheapest possible finding.
2. **Take the app screenshots**, the same way the prototypes were shot: every material × every
   background × dark/light, on the real screens. Claim a device and the Gradle daemon first.
   **Use `adb install -r` + `am instrument`, never `connectedDebugAndroidTest`** — that task
   uninstalls the app and takes the Google sign-in with it
   (`kb/dev/android-device-verification.md` §8).
3. **Diff app against prototype by eye, frame beside frame**, and write the gap list before
   writing any code. The rule this serves is `kb/dev/look-at-your-own-output.md`: when the
   acceptance criterion is visual, render and *look*, between revisions — a round whose render is
   unchanged is a result.
4. **Fix in this order**, cheapest and most-likely-to-be-the-whole-answer first: defaults → the
   entrance animation's screen coverage → surface/ground fidelity → chart volume → **widgets**.
5. **Widgets last and separately** — different renderer, different constraints, probably new work.

## Carries over

- The `#57` findings above — from the issue thread itself and from `CHANGELOG/2026-08-21/57a-*`,
  `CHANGELOG/2026-08-22/57c-*`, `CHANGELOG/2026-08-22/57d-*`, all committed.
- `TODO/TODO_OPTIONAL/Presentation.TODO.optional.md` — where the un-shipped presentation items were
  parked before `#57` existed; check what is still open there.
- The board row and its warning to the tour sessions — `SESSIONS.md`, committed `77a32f1`.

## Out of scope

- `feature/challenges/**`, the challenge data model, `functions/**`, `firestore.rules` — all
  `challenge-scoring`'s.
- `docs/marketing/**` and the tour film — `62-tour-assembly`'s.
- Re-deciding anything `#57` settled. In particular **raised-3D is an axis on all four materials**,
  by Ido's explicit overturn on 2026-08-21; the argument that *"height contradicts what glass is"*
  is a real argument and is already overruled. Do not re-collapse it.

## Exit

- The app, on a device, screenshot beside the prototype render for each material — and they match,
  or every remaining difference is named and costed.
- Entrance motion present on every screen that has cards, not only the dashboard.
- Widgets that look like `02-widgets_*`.
- JVM suite green (`ThemePaletteTest` especially — it guards these values).
- `CHANGELOG/2026-08-24/visual-parity.md`, and commit on the AUTO MODE trigger.
