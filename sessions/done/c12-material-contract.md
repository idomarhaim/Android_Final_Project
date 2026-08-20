---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 53
created: 2026-08-20
result: shipped 2026-08-20 in `05ec6aa` (claim `e1324c1`) — #53 left OPEN, see below
---

# `C12` build half — the four-material contract, so #48's Appearance section can exist

**One of the two holes in [#48](https://github.com/idomarhaim/Android_Final_Project/issues/48).**
Independent of `C20`. Needs the **Gradle daemon** and a **device or the cloud emulator** — this is
the most visual unit in the plan and cannot be judged from source.

> ⚠️ **Runs BEFORE `c13-key-store`, and never beside it. Verified 2026-08-20.** Both edit
> `feature/settings/SettingsScreen.kt` — this adds the Material tiles to `AppearanceCard` (`:275`),
> `c13` adds the AI section to `SettingsScreen` (`:103`). This one goes first because the material
> contract decides how **every** section renders, the AI section included.

## The issue is filed

[**#53**](https://github.com/idomarhaim/Android_Final_Project/issues/53) *(2026-08-20)*, and
[#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) — the closed `C12` decision —
now carries a comment pointing at it, so the *decided-read-as-built* trap
(`C:\Dev\JARVIS\kb\dev\decision-map-charting.md` §12) is closed for this one.

## Why it exists

`48-settings-surface` shipped four of §4.9's five sections and **deliberately did not build the
Material picker**. Its own words, at `feature/settings/SettingsScreen.kt:85`:

> §4.1's four-material contract does not exist in this codebase — no `AppMaterial`, no palette
> transform, and no open issue scheduling one. **A picker over materials nothing renders is a
> control that changes nothing, which is §0.3's defect installed in the screen built to prevent
> it.**

That was the right call. This unit removes the reason.

## What ships — §4.1

**Four materials: glassmorphism · liquid glass · neo · dark neo. Metal is deleted.**

**No screen may depend on a property a single material has.** A screen specifies
**`surface · groove · elevation · accent`**, and each material answers those four its own way —
otherwise every design gets drawn four times, which is the cost that makes four materials
unaffordable.

| | Depth from | Arcs |
|---|---|---|
| Glassmorphism | **blur** | thin, flat, slightly transparent, coloured bloom instead of a bevel |
| Liquid glass | **refraction at the edge** | translucent body, bright inner rim, dim outer counter-rim, one specular streak |
| Neo | **a shadow pair** on one flat surface | inset track, softly extruded arc, muted hues, no rim, no gloss |
| Dark neo | **deep shadow pair + one saturated gradient** | charcoal groove, extruded arc, one cyan→blue accent |

## The three things that get found late if you do not build them first

1. **Material is a *second axis*, not `AppSkin`.** `AppSkin` (`AURORA`, `BLOSSOM`) is a **palette**;
   material is a **surface**. They do **not** multiply freely, so each material declares a **palette
   transform** — `identity · mute · single-accent ramp` — and the schemes are **generated, not
   hand-authored**.
2. **Dark neo's accent must derive from the selected skin.** Otherwise picking Blossom under dark
   neo silently renders Aurora, and the skin picker stops working for a quarter of the set.
3. **The product is ragged, not rectangular.** Dark neo has **no light scheme**, so a material must
   be able to declare itself **brightness-locked**, and the picker must **say so** rather than
   letting the light switch quietly do nothing. `48-settings-surface` already built the Appearance
   section's brightness control — check how it behaves before you add the lock.

## Two rules every screen inherits — each from a real failure

- **`--edge`** — every control carries a **hairline contrast anchor**; no affordance is ever
  shadow-only. This is neo's known **WCAG failure**, not a preference.
- **`.tag`** — a category is **written in words** beside its dot, because dark neo collapses the six
  categorical hues into one ramp and **colour stops carrying identity**.
- **An overlay component declares its own opacity** — sheet, dialog, menu. Neo's surface *is* the
  page colour plus a shadow pair, so a component that inherits the material's surface rule inherits
  **transparency**, and the dimmed screen reads straight through it.

## Then, and only then, the picker

Add the four Material tiles to §4.9's Appearance section. `SettingsScreen.kt` already has the
section and the `ConsequenceLine` component — the tiles slot in.

## The trap

**This cannot be verified by tests.** Four materials × two brightnesses × two skins is a visual
matrix, and every one of the three "found late" items above is invisible to a green suite. Render
them and **look**, and put the images in the changelog. `docs/CLOUD-DEVICE.md` has the click-path;
a manual `workflow_dispatch` with `capture-screenshots` is the cheapest way to get them
(`.github/workflows/instrumented-tests.yml:197` — the screenshot job does **not** run on push).

## Exit

- JVM unit for the palette transforms and the brightness-lock declaration.
- Instrumented for the picker.
- **Every material seen, in both brightnesses, with the images in the changelog.** A material
  contract signed off from source is not signed off.
- `CHANGELOG/<today>/c12-material-contract.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. Name `/kickoff c13-key-store` — the other half of #48.

---

## Result — 2026-08-20

**Shipped in `05ec6aa`.** Everything under *Exit* landed: JVM unit for the palette transforms
and the brightness-lock declaration (`AppMaterialTest`, `MaterialPaletteTest`, and
`ThemePaletteTest` widened from 4 schemes to the 14 distinct cells), instrumented for the
picker (`MaterialPickerUiTest`, 7 tests), and **every material seen in both brightnesses and
both skins** — 16 frames in `docs/render-passes/2026-08-20-c12-material-contract/`.

**`#53` is left OPEN, and the held item is named rather than implied.** §4.1's `.tag` rule —
*a category is written in words beside its dot, **because** dark neo collapses the six
categorical hues into one ramp* — is listed in the issue's scope. The collapse arithmetic is
built and unit-tested (`rampTint`) and is **deliberately not applied** at the call sites: the
words are what make the collapse survivable, so shipping the collapse first installs the exact
identity failure `.tag` exists to prevent, in every chart at once. That sweep belongs with
`C12` §4.4's charts.

**The order this brief insisted on held.** `c13-key-store` did not run beside this; its own
session corrected its false independence claim in `ba3c763` and deferred. `/kickoff c13-key-store`
is now unblocked — `SettingsScreen.kt` is free.
