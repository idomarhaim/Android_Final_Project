# Visual language — four candidates

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). Ido, 2026-08-11: rev 4's
charts *"look like a balloon — dated, not pretty"*, with reference images for
**glassmorphism · metal · liquid glass · neo**, and a request for one prototype per style.

```powershell
start docs\prototypes\2026-08-11-visual-styles\index.html
```

Buttons pick the material; `←` / `→` cycle. **עברית** flips to RTL, **Light / Dark** switches
scheme. **Compare all four** puts the same card in every material at once.

## Why one file rather than four

Four separate files would each be a page you look at alone, and the question here is
*comparative* — which material do you prefer. So the content is held **absolutely constant**
(same screen, same week, same harmonised palette from C12 rev 4) and only the surface changes.
That is also what makes "Compare all four" possible, which is the view the choice is actually
made in.

## The balloon, named

It was one specific thing: **`feSpecularLighting` over a fat stroke**. That filter simulates a
lit, rounded solid, so a 20 px arc becomes an inflated tube — the look of a 2010 web chart. There
is **no `feSpecularLighting` anywhere in this file**; each material gets its depth from somewhere
else, and the four differ precisely in *where*.

| | depth comes from | how the arcs are drawn |
|---|---|---|
| **Glassmorphism** | **blur** — the canvas stays legible through the panel | thin, flat, slightly transparent, with a coloured bloom instead of a bevel |
| **Metal** | **anisotropic reflection** — sheen banded across the stroke | hard light/dark banding, hairline bright and dark edges, a milled groove for the track |
| **Liquid glass** | **refraction at the edge** | translucent body, bright inner rim where light enters, dim outer counter-rim, one specular streak |
| **Neo (soft UI)** | **a shadow pair** on one flat surface | inset track, softly extruded arc, muted hues, no rim and no gloss |

Two things carry across all four because they are **decisions, not styling**: the donut's
**direct labels** with leader lines, and the **harmonised category palette**. Only the material
is in question here.

## Notes per material, for choosing

- **Glassmorphism** needs a busy canvas underneath or the frost has nothing to frost. Cheap in
  Compose (`Modifier.blur` + translucent surfaces) and it survives both schemes well.
- **Metal** is the most distinctive and the least conventional for a personal-goals app; it is
  also the only one that reads well with **no** background art. Its light scheme is genuinely
  hard — brushed metal on white tends toward grey.
- **Liquid glass** is the current Apple direction and the closest to your reference images. It is
  the most expensive to imitate faithfully: real refraction needs a runtime blur of *what is
  behind*, which in Compose means `RenderEffect` (API 31+) and a fallback below that.
- **Neo** is the calmest and the most legible in Hebrew, because nothing competes with the text.
  Its known weakness is **contrast** — shadow-only affordances can fail accessibility checks, so
  buttons need a real contrast anchor, not just an extrusion.

## The cost, stated

`backdrop-filter`, SVG filters and CSS shadow pairs are **web** primitives. In Compose the
equivalents are `Modifier.blur`, `RenderEffect`, and hand-drawn `Canvas` shadows — and **a widget
has none of them**, so whichever material wins, the home-screen tiles need a version that
survives being drawn into a `RemoteViews` bitmap. Named here rather than found later.

**Verified:** `node --check` on the extracted script → **OK**. Hebrew guard → **14 literals, 0
unguarded**; one Hebrew string outside the phone frame, the prototype's own `עברית` toggle.
