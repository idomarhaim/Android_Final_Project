# `C12` #53 — the four-material contract, seen

16 frames: **4 materials × 2 brightnesses × 2 skins**, of spec §4.9's Appearance section drawn by
the real `GoalPilotTheme`. Captured 2026-08-20 on AVD `Pixel_10_Pro_XL` by
`app/src/androidTest/.../MaterialRenderPass.kt`, at half resolution.

Named `<skin>-<material>-<brightness>.png`, where `<brightness>` is the brightness **asked for**,
not necessarily the one rendered — which is the whole of what the dark-neo pair shows.

## Why a render pass at all

The brief for this unit rules out doing it any other way:

> **This cannot be verified by tests.** Four materials × two brightnesses × two skins is a visual
> matrix, and every one of the three "found late" items is invisible to a green suite. Render them
> and **look**, and put the images in the changelog.

## What the frames show

| Claim | Frames | Reading |
|---|---|---|
| **Four materials are four materials** | `aurora-*-light` | Glass and liquid glass are translucent panels over a tinted page; neo is one flat ground with cards extruded by a shadow pair; dark neo is charcoal with a saturated ramp. No two are the same surface |
| **§4.1's first consequence: dark neo derives its accent from the skin** | `aurora-darkneo-dark` vs `blossom-darkneo-dark` | Cyan→blue against rose→coral. The failure §4.1 names — *"picking Blossom under dark neo silently renders Aurora"* — would have made this pair identical |
| **§4.1's second consequence: the product is ragged** | `aurora-darkneo-light` | Brightness was set to **Light** and the screen rendered **dark**. The segments are struck through, greyed and inert, and the caption says why |
| **The lock is a word** | any `*-darkneo-*`, and every other frame | `Dark only` sits on the dark-neo tile in all sixteen, and on no other tile in any of them |
| **The picker does not obey the material** | `aurora-neo-light` | The screen is neo and three of the four tiles are not — which is §4.9's documented exception. Drawn against the contract like everything else, all four tiles would read as neo |
| **The tiles follow the skin** | `blossom-*` vs `aurora-*` | Every tile's preview repaints when the colour is changed, not just the selected one |
| **`--edge` survives every material** | all | Each panel carries its hairline. Neo light is the case that needed it: a pale card on a pale ground with only a shadow pair is neo's known WCAG failure |

## The dark-neo pair is *not* byte-identical, and that is correct

`<skin>-darkneo-light.png` and `<skin>-darkneo-dark.png` differ by about 3 KB. The **theme** in
them is the same — same charcoal, same ramp, same everything the lock governs. What differs is
which brightness segment carries the selection tick, because the *stored setting* is genuinely
still Light in one and Dark in the other. That is the lock behaving as designed: it suspends the
setting, it does not overwrite it.

## Honest limits

- **Glass and liquid glass are close at tile size.** What separates them — the bright inner rim,
  the dim counter-rim, the specular streak, the coloured glow — is small-scale, and a 62 dp
  preview does not have room for it. At full-screen size (compare `aurora-glass-light` with
  `aurora-liquid-light`) they read apart clearly.
- **No backdrop blur.** Compose has none, and `Modifier.blur` blurs a composable's own content
  rather than what is behind it. The two glass materials are translucent panels over a gradient
  page, which keeps the *reading* §4.1 specifies and drops the blur it prices. See
  `ui/theme/MaterialSpec.kt`'s header.
- **One screen.** These frames exercise the palette transform, the `GpMaterialSpec` surface, the
  page backdrop and every `GpCard` on the Appearance screen — not the rest of the app.
- **English only.** §0.8's Hebrew sub-rule is suspended (`AGENTS.md`), so this pass is English.
