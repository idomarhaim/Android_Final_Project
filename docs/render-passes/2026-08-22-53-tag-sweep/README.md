# `#53` — §4.1's `.tag` rule, seen

Three frames from the 16 written by `app/src/androidTest/.../CategoryPaletteRenderPass.kt` on
2026-08-22, AVD `Pixel_10_Pro_XL`, at half resolution. The other thirteen are the rest of the
matrix and say nothing this trio does not.

The chart is the **real** `DonutChart` with the **real** ten `GoalCategory` values through the
**real** `GoalPilotTheme`, at ten **equal** slices — no size cue, so colour is the only thing
separating two wedges. That is the hardest arrangement on purpose.

## What the frames show

| Frame | Claim |
|---|---|
| `aurora-darkneo-dark.png` | **The collapse, and why the words are load-bearing.** Ten wedges, one blue ramp; ten legend dots, one blue. Colour has stopped carrying identity entirely — and every wedge still says what it is. |
| `blossom-darkneo-dark.png` | **The skin still reaches the collapsed palette.** Rose-to-coral against Aurora's cyan-to-blue. §4.1's first named consequence — *"picking Blossom under dark neo silently renders Aurora"* — would make this frame identical to the one above it. |
| `aurora-glass-light.png` | **The control: nothing collapsed where nothing should.** Ten distinct hues under `PaletteTransform.IDENTITY`, and the same ten words. The `.tag` labels are not a dark-neo patch — they are drawn on every material, and the ink is solved per wedge, so a word on a pale wedge goes dark and a word on a deep one goes light. |

## The one thing to look for that a test cannot see

**Nutrition and Career are the same pixel** in the dark-neo frames. `#347B47` and `#0861A7` both
have an HSL lightness of `0.343137` — `(123+52)/2` and `(167+8)/2` are both 87.5 — and `rampTint`
positions a hue on the ramp by its lightness. `CategoryTagTest` measures the distance between them
after the collapse as **exactly 0.0**. Their words are the only difference left, which is the rule
holding up its own worked example.

## Why the words are shortened rather than dropped

An earlier version of this pass drew only words that fitted **whole**, and labelled **six** of the
ten wedges — leaving Nutrition, Relationships, Projects and Learning blank, the four longest names
and, under dark neo, the four wedges nothing else tells apart. That frame is what changed the rule:
long names are now ellipsized against the arc they have. *Relation…* carries identity; a blank wedge
does not.

Below about 18.5° — a 5% share — a wedge is left unlabelled rather than given an ellipsis with
nothing before it. Those keep the path that was already there and is **not** the legend: tapping a
wedge writes its full name into the hole.

See `CHANGELOG/2026-08-22/53-tag-sweep.md` for the whole unit.
