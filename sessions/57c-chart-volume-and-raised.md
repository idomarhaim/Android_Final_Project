---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 57
created: 2026-08-21
---

# `#57` c — chart volume, and raised-3D as an axis on all four materials

**The third of four briefs on [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57),
and the largest.** Runs **after `57a-category-palette`** (it renders those values) and **after
`57b-backgrounds-and-combinations`** (both widen the same test matrix, and doing them in the other
order means widening it twice).

This is Ido's *"no 3D graph and everything from the second prototype"*.

## ⛔ Read this first: a recorded decision was overturned

`TODO/TODO_OPTIONAL/Presentation.TODO.optional.md` used to say raised-3D is a **property of the two
soft-UI materials**, *not* a user setting, because *"height would contradict what glass is"*.

**Ido overturned that on 2026-08-21**, asked directly:

> *"neo, dark-neo both. 3d graphs is an option that can be implemented **in addition on each of the
> design types** (not only the two mentioned)."*

So:

1. **neo and dark neo ship raised.**
2. **Raised-3D is a separate user-selectable axis, available on all four materials** — glass and
   liquid glass included.

The old argument was a real one and is overruled as a **product call**. Do not re-collapse it. If
raised glass looks wrong to you, that is a note for Ido, not a licence to restrict the picker.

## What "volume" means, precisely

Revision 4 of `docs/prototypes/2026-08-10-charts-presentation/index.html` specifies it as five
compositing layers on **every arc, bar and ring** — not a gradient:

| Layer | Prototype primitive |
|---|---|
| three-stop fill | tint → hue → shade |
| specular bevel | `feSpecularLighting`, point light above-left |
| sheen arc | along the lit edge |
| cast shadow | below |
| grain | `feTurbulence` in soft-light, so it reads as surface rather than plastic |

Slice caps are **butt, not round** — a round cap adds half the stroke width past each endpoint,
which at these widths visibly overshoots.

## ⚠️ The hard part, and it is not the geometry

**Every one of those is a web primitive, and Compose has none of them.** The TODO says so already:

> Every material's depth primitive is a web primitive. `backdrop-filter`, SVG filters and layered
> box-shadows are what the prototype is made of.

There is no `feSpecularLighting` in Compose, and `kb/dev/compose-soft-shadows-below-api-28.md`
records the last time this project met that wall: `BlurMaskFilter` is **unsupported on a hardware
canvas below API 28 and does not throw** — it silently renders hard-edged. What worked there was
hand-integrating the falloff: sum N rounded rects, each inflated `spread·i/N` at `alpha/N`.

**So the first unit of work is not building — it is finding the cheapest honest approximation and
pricing it.** Do that on one chart, show it, and only then do the rest. Twelve revisions of geometry
already exist; do not spend the session re-deriving them.

## What ships

1. A volume treatment for the chart primitives, as close to rev 4 as Compose honestly reaches.
2. The **raised axis**: persisted, exposed in Appearance, orthogonal to material.
3. Contrast coverage across the widened matrix.

## Watch for

- ⚠️ **The matrix is now material × scheme × raised**, on top of whatever `57b` added.
  `ThemePaletteTest` was 14 cells after `C12` and caught a real WCAG failure at **3.54:1** on its
  first widened run. Iterate the **axes**, never the authored data — `kb/dev/generated-values-need-matrix-guards.md`.
- **Charts are drawn into bitmaps for the widget pack** (`ui/widget/`), which has no Canvas, no
  blur and no animation. Whatever you build must degrade there or be deliberately absent, and
  §4.5's contract decides which.
- **Do not let the grain pass cost a frame.** A `feTurbulence` equivalent per draw is the obvious
  performance trap; bake it once.

## Out of scope

Backgrounds (`57b`). Entrance animation (`57d`). The palette (`57a`, must already be done).

## Exit

- Volume renders on the donut, the bars and the rings, in all four materials, both schemes, raised
  and flat.
- Contrast suite green across the full widened matrix.
- **Seen** on a device — this is a *look* feature, so a passing test proves almost nothing.
- A stated answer to *"what did Compose not reach, and what was substituted"*. Name it; do not let
  the difference be discovered later by Ido.
- `CHANGELOG/<today>/57c-chart-volume-and-raised.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · commit and push under AUTO MODE.
