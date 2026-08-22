# `#53` — the material picker names itself in §4.1's vocabulary

Two frames from `MaterialRenderPass.aurora_everyMaterialInBothBrightnesses`, re-run
2026-08-22 by session `53-material-naming`. They are here as **evidence of the caption**,
not as a new matrix — the full 16-frame material matrix is
[`../2026-08-20-c12-material-contract/`](../2026-08-20-c12-material-contract/) and nothing
about the materials themselves changed.

| Frame | What it is evidence of |
|---|---|
| `aurora-neo-light.png` | All four tiles carry §4.1's own name under the label a user reads — `Spec: Glassmorphism` · `Spec: Liquid glass` · `Spec: Neo` · `Spec: Dark neo`. **The word "neo" now appears in the UI**, which is the whole of `#53`'s 2026-08-21 gap. |
| `aurora-darkneo-dark.png` | The same four captions on dark neo's charcoal ground, in `onSurfaceVariant` — the contrast case, since a caption legible on a light page can vanish on this one. |

## What the frames answered that a test could not

1. **Every caption fits on one line at half-tile width.** `Spec: Glassmorphism` is the longest
   and it fits; a wrapped designation would have read as a broken label. This is the only claim
   in the unit that no assertion could make.
2. **The lock word survives being pushed down a line.** Dark neo's tile now reads
   *Soft dark · Spec: Dark neo · **Dark only** · Charcoal, with one bright accent*. §4.9 requires
   the lock to be **a word on the tile**; it still is, still bold, and the order groups naming
   (label + spec name) above behaviour (the lock), which is the hierarchy that reads correctly.
3. **Liquid glass repeats itself, and that is the intended output.** Its two vocabularies genuinely
   coincide, so its caption restates its label. A caption that appeared only where the words differ
   would be unlearnable — a reader could not tell *"the same"* from *"not stated"*.

## What they do NOT show

**Hebrew.** `AppLanguage.OFFERED` does not carry `HEBREW` (`#51` is frozen) and this harness pins
`AppLanguage.ENGLISH`, so no frame here is RTL. The Hebrew half is asserted **mechanically** instead,
by `MaterialPickerUiTest.theSpecNameSurvivesTheLanguageSwitch_andItsLatinRunIsIsolated`: the frame
around the name translates, the name itself does not, and the Latin run carries its bidi isolate.
`Untested:` how that line **looks** in an RTL paragraph — what would check it is one render method
walking `AppLanguage.HEBREW`, and it is owed when `#51` resumes, not before.
