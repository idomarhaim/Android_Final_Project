---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 57
owns:
  # CORRECTED at /kickoff 2026-08-21: there is no GoalCategory.kt -- the enum is
  # declared inside Goal.kt. And the dark variant has to be wired at the seam that
  # already lifts stored hexes for dark surfaces, which is ColorExt.kt, not Theme.kt.
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/ColorExt.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialPalettes.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/theme/Theme.kt
  - app/src/test/java/com/idomarhaim/goalpilot/ui/ThemePaletteTest.kt
  - CHANGELOG/2026-08-21/57a-category-palette.md
  - sessions/57a-category-palette.md
created: 2026-08-21
result: |
  Shipped 2026-08-21 in 2c44b42 (claim 4a8c512).
  Everything under "What ships" landed, with one substitution the brief did not
  anticipate: the prototype's hexes could NOT be copied verbatim -- six of its seven
  miss 3:1 against AuroraSurface (#D0E2F5) and it has seven categories to the app's
  ten -- so its OKLCH hue and chroma ported and lightness moved down. A fill/ink split
  (String.toGoalInk) was added because that is what made a ten-colour harmonised set
  possible at all.
  NOT fixed, and answered as the brief asked: "no dark blue neo" is a DIFFERENT defect
  -- MaterialPalettes.ramped() clamps the ground to saturation 0.08 against the skin's
  own 0.45, so dark neo renders neutral charcoal. It has no owner yet.
  Tests: JVM 712/712; instrumented render pass 2/2, 16 frames.
---

# `#57` a — the harmonised category palette, first, because everything else renders it

**The first of four briefs on [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57).**
It is first for one reason: it is a **value change**, and every other brief in this ticket draws
things using these values. Do it after them and their screenshots are all wrong.

Small, self-contained, and it has a guard already pointing at it.

## Why it exists

`C12`'s charts prototype, revision 4, in Ido's words:

> The committed `GoalCategory.defaultColorHex` set is seven **primaries at full chroma with
> unevenly spaced hues** — individually fine, but side by side in one donut nothing holds them
> together, which is why they read as crayons.

The prototype replaced it with a set built **as a set**: one lightness, one chroma, hues evenly
spaced, and a **dark variant holding the same relationship against `#0C1520`**.

That has never been ported. `docs/prototypes/2026-08-10-charts-presentation/index.html` is the
source of truth for the values — read them out of it, do not re-derive them by eye.

## The one that may fix a bug you were not asked to fix

Ido reported *"no dark blue neo"* on `v0.3.0`. `AppMaterial.DARK_NEO` **does** ship and is
selectable, so it is not missing — but the prototype's dark variant is the one built against
`#0C1520`, and the app never got it. **Check this first, on a device:** if dark neo looks wrong
rather than absent, this brief is the fix, and it should be verified as such rather than assumed.

## What ships

1. `GoalCategory.defaultColorHex` replaced with the harmonised light set.
2. The **dark variant** wired to the same relationship against the dark ground, so a category keeps
   its identity across schemes rather than becoming a different colour.
3. `ThemePaletteTest` updated — the TODO already records it as **owed**, and it is the guard on
   this palette, so it must move with the values rather than after them.

## Watch for

- ⚠️ **`ThemePaletteTest` grows by a dimension whenever a material or scheme is added**, and
  `C12` already widened it from 4 schemes to **14 cells**, where it caught a real WCAG failure at
  **3.54:1** on its first run. Expect it to catch something here too; that is the point of it.
- **The contrast rule that defect taught, restated because it binds here:** a ramp used as a
  *filled* surface needs **one** ink readable on **every** stop, and the ink must be dark, so the
  **deep** end binds. *Deepen it for drama* is the intuitive edit and it breaks the end nobody
  looks at.
- These are **committed values**, so the change moves every chart, every category chip and every
  widget at once. Look at more than the donut.

## Out of scope

Backgrounds, chart volume, raised-3D and entrance animation — briefs `b`, `c` and `d`. Anything in
`#55`/`#56`'s model work.

## Exit

- `ThemePaletteTest` green across the full matrix, including the deep end of every ramp.
- **Seen** on a device: the donut, a category chip, and dark neo — the last specifically against
  Ido's report.
- `CHANGELOG/<today>/57a-category-palette.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · commit and push under AUTO MODE.
- Say plainly whether this fixed *"no dark blue neo"* or whether that is still open.
