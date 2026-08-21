---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 57
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialSpec.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/theme/Theme.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/AppBackground.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/AppPreferencesRepository.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/prefs/AppPreferencesRepositoryImpl.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/MaterialPicker.kt
  - app/src/main/java/com/idomarhaim/goalpilot/MainActivity.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/settings/SettingsScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/settings/SettingsViewModel.kt
  - app/src/test/java/com/idomarhaim/goalpilot/ui/ThemePaletteTest.kt
  - CHANGELOG/2026-08-22/57b-backgrounds-and-combinations.md
  - sessions/57b-backgrounds-and-combinations.md
created: 2026-08-21
closed: 2026-08-22
commit: 9e9fdff
result: |
  Shipped as A -- a background x material grid -- because the A/B fork COLLAPSED under the
  derivation-closure check rather than being adjudicated: both designs write one quantity
  (`GpMaterialSpec.backdrop`), so B's two states are two VALUES of A's enum. MATCH is native
  canvas, GLOW is shared canvas. Decision taken by the session per derivable-decision.md and
  recorded as the session's, Ido's to overturn.

  Neo does NOT survive a lit ground -- by design, stated in AppBackground's KDoc and in a live
  consequence line, and seen on device. The converse (glass on a plain ground) ships too and was
  not in the brief.

  TWO PRE-EXISTING DEFECTS found and fixed, neither in the brief:
   1. every screen's Scaffold painted an opaque containerColor over gpPage, so no ground had
      ever been visible app-wide -- the literal cause of "the same backgrounds aren't there".
      MaterialRenderPass had the same gap, so the two instruments agreed with each other.
   2. glass in dark mode: body text at 2.55-2.78:1 on its own native ground.

  JVM 717/0, instrumented 194/0, 80 render frames. #57 stays OPEN -- 57c and 57d are still ready.
---

# `#57` b — backgrounds, and letting the user combine them with the blocks

**The second of four briefs on [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57).**
Runs **after `57a-category-palette`** — that brief changes the values everything here renders.

This is the brief that answers the largest half of Ido's complaint: *"the same backgrounds aren't
there, and there's no option to choose different combinations between the backgrounds and the
blocks."*

## What the prototype actually has

From `docs/prototypes/2026-08-11-visual-styles/README.md`:

> All four materials now sit on the **glassmorphism background**, so the only variable left is the
> surface itself. **Native canvas** toggles back to the background each material was designed for.

So there are **two** things the app is missing, and they are different:

1. **Per-material background art.** Each material was designed against its own ground. The app
   currently draws every material on one flat theme background — `gpPage` exists in
   `ui/theme/MaterialSpec.kt`, but there is no *art*, per material or otherwise.
2. **A user choice between grounds.** The prototype has a toggle; Ido asked for *"combinations"*.

## ⚠️ The design question that must be answered before any of it is built

**Is the background a property of the material, or an independent axis?** The prototype has a
two-state toggle (shared glass ground vs native ground). Ido's word was *"combinations"*, and in the
same answer he made raised-3D an axis available on **all four** materials — *"an option that can be
implemented in addition on each of the design types"*. That is a strong signal he wants
presentation **composable**, not a second list of presets.

**Do not decide this alone.** Sketch both against the real screens and put them to him:

- **A · background × material grid.** Any ground under any surface. Most freedom, and it is what
  "combinations" most plainly means. Cost: the contrast matrix multiplies again — `ThemePaletteTest`
  is already 14 cells, and this is another dimension on top of the raised axis brief `c` adds.
- **B · the prototype's toggle.** *Shared ground* or *native ground*, two states. Cheap, matches
  what was actually designed and rendered, and every combination it offers has been looked at by a
  human at least once.

**Neo is the case that decides it, and it is definitional rather than aesthetic.** The prototype
README:

> **Neo changes the most, and the reason is definitional.** Neumorphism *is* the surface being the
> same colour as what is behind it — that is what makes the shadow pair read as an extrusion.

So under option A there exist combinations where **neo stops being neo**. That is not a reason to
refuse A — Ido has already overruled exactly that kind of argument on raised-3D — but it *is*
something he should be told before choosing, because he will see it and think it is a bug.

## What ships

1. Per-material background art, ported to Compose from the prototype's grounds.
2. Whichever of A or B he picks, persisted beside `material` in `AppPreferencesRepository` and
   exposed in §4.9's Appearance section.
3. Whatever `ThemePaletteTest` needs to keep covering the widened matrix.

## Watch for

- ⚠️ **The prototype's grounds are web primitives.** `backdrop-filter` and layered gradients are
  what they are made of, and the TODO already flags this as *"the bill the material decision sends
  to the build session"*. Find the cheapest honest Compose approximation and **say what was lost**,
  rather than shipping something that quietly is not the design.
- **The Appearance section is already three controls deep.** Adding a fourth to a card Ido has just
  seen is a real UX cost. `feature/settings/AiCard.kt` is a worked example of the alternative — a
  summary row that opens an editor.
- `ui/theme/MaterialSpec.kt`'s `GpMaterialSpec` is the contract every screen asks for. A background
  that screens must know about individually breaks it; it belongs in the spec.

## Out of scope

Chart volume and raised-3D (`57c`). Entrance animation (`57d`). The palette (`57a`, must be done).

## Exit

- The background choice persists across a process kill, and defaults to today's look for anyone who
  never touches it.
- Contrast suite green across the widened matrix.
- **Seen** on a device, all four materials, both schemes — and specifically neo, against the
  definitional note above.
- `CHANGELOG/<today>/57b-backgrounds-and-combinations.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory

Say which of A or B Ido chose and why, and whether neo survives the combinations the choice allows.
