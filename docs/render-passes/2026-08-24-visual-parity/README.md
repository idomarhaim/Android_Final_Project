# Render pass — `visual-parity`, 2026-08-24

Ido, that morning: the app does not look like `docs/prototypes` — not the colours, backgrounds,
transparency or textures — the widgets look very bad, and the cards do not fade in.

He had said almost exactly the same thing on 2026-08-21, which became
[`#57`](https://github.com/idomarhaim/Android_Final_Project/issues/57) — and `#57` **shipped all
six of its items** in `2c44b42`, `9e9fdff`, `d48a7ee` and `9f6b92b`. So this pass exists to answer
a different question from the usual one: not *does the new thing look right*, but **why did the
thing that shipped never reach him**.

These frames are the answer, and the first pair is the whole of it.

| frame | what it shows |
|---|---|
| `default-before-neo-plain-dark.png` | **What a fresh install looked like until 2026-08-24.** `AppMaterial.DEFAULT` was `NEO`; `AppBackground.MATCH` — also the default — resolves neo to `PLAIN`, *"one flat tone, no lights at all"*; `AppRelief.DEFAULT` is `FLAT`. Three defaults, each defensible alone, composing to an app with no ground, no translucency and no volume. |
| `default-now-glass-dark.png` | **What it looks like now.** Same build, same code, one changed constant: `DEFAULT = GLASS`, so `MATCH` resolves to `GLOW`. Lit ground, translucent panels, visible rim. |
| `default-now-glass-light.png` | The same default in the light scheme. |
| `glass-on-plain-dark.png` | The honest edge of the combination axis — `#57` b's own claim that **glass on `PLAIN` stops being glass**, because a translucent panel over a flat ground has nothing to be transparent about. Kept because the app says this in words on the settings screen and this is where the words can be checked. |
| `entrance-096ms.png` | The staggered rise-and-fade **96 ms in**: block 1 arrived, block 2 nearly, block 3 still faint and low. The stagger is the feature; a single shared duration is a fade-in and visibly not this. |
| `entrance-800ms.png` | The same column settled, for the comparison. |

Produced by `MaterialRenderPass` and `EntranceRenderPass` (`app/src/androidTest`), on
`Pixel_10_Pro_XL_B` at 1344×2992 / 480 dpi. The full sweep is 88 frames — 4 materials × 4 grounds ×
2 brightnesses × 2 skins, plus the 8-frame entrance strip; these six are the ones that carry the
argument.

## The one difference from the prototype that is NOT a defect

The app's lit ground is **quieter** than the prototype's, and that is deliberate and measured. The
prototype hard-codes saturated mid hues (`#4E6BFF`, `#00C8B4`, `#A65CF5` — relative luminance
0.194 / 0.446 / 0.224). The app reads every hue off the `ColorScheme` instead, so the ground tracks
the skin and the material's palette transform — and a dark Material 3 scheme's
`primary`/`secondary`/`tertiary` are **pastels**, measuring 0.564–0.572 here. Same hues, roughly
twice the light.

`MaterialSpec.kt` therefore runs the lights at `alpha = 0.42`, which is where `onBackground` clears
WCAG's 3:1 non-text floor on the worst lit page (**3.96**, against **2.94** at the 0.55 it used to
be). Matching the prototype's apparent saturation means either hard-coding its hexes — which breaks
skin tracking and the dark-neo ramp — or failing that floor.

**Left alone on purpose.** It is a taste-versus-accessibility trade that a prior session already
decided with measurements, and overturning it is Ido's call, not a session's.

## What these frames do not prove

- **The widgets.** `WidgetPalette`'s panel became translucent in the same commit, and that change
  is verified by **measurement, not by eye** — `WidgetPanelContrastTest` proves the panel is
  translucent and that its text clears WCAG over a pure black *and* a pure white wallpaper, which
  bound every wallpaper there is. No frame here shows a real widget on a real launcher.
- **A signed-in app.** This emulator has no account, so every frame is a surface reachable without
  one. The dashboard's own cards are represented by the entrance strip, which composes them
  directly.
