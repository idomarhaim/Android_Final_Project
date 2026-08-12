# Visual language — five candidates

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). Ido, 2026-08-11: rev 4's
charts *"look like a balloon — dated, not pretty"*, with reference images for
**glassmorphism · metal · liquid glass · neo**, and a request for one prototype per style.

```powershell
start docs\prototypes\2026-08-11-visual-styles\index.html
```

Buttons pick the material; `←` / `→` cycle. **עברית** flips to RTL, **Light / Dark** switches
scheme. **Compare all four** puts the same card in every material at once.

## Shared canvas — the default, at Ido's request (2026-08-11)

All four materials now sit on the **glassmorphism background**, so the only variable left is the
surface itself. **Native canvas** toggles back to the background each material was designed for,
which is worth one look because two of them change character:

- **Metal** was designed against graphite, where it reads as a machined object. On the colourful
  canvas it reads as a metal panel *placed on* something — still good, but a different claim.
- **Neo changes the most, and the reason is definitional.** Neumorphism *is* the surface being
  the same colour as what is behind it — that is what makes the shadow pair read as an extrusion
  rather than a floating card. A gradient has no single such colour, so on the shared canvas the
  honest rendering is a **neutral plate carrying the shadow pair**, which gives the card an
  **edge it would not otherwise have**. Real difference, not a rendering shortcut — and worth
  knowing before choosing, because "neo on a colourful background" is not the same offer as
  "neo".

## Why one file rather than four

Four separate files would each be a page you look at alone, and the question here is
*comparative* — which material do you prefer. So the content is held **absolutely constant**
(same screen, same week, same harmonised palette from C12 rev 4) and only the surface changes.
That is also what makes "Compare all four" possible, which is the view the choice is actually
made in.

## Two toggles for judging the groove (2026-08-12)

**Empty channel** (on by default) sizes the slices against a **26-hour week** instead of against
the tracked total, so the ring stops at ~71% and **the bare channel is visible** for the rest of
the circle. That is the point: a groove you can only see *under* an arc cannot be judged. It is a
**rendering test, not a proposed metric** — the centre caption says *of a 26h week* so the
picture never claims something the data does not.

**Raised 3D arc** (off by default, and *additional* rather than instead) gives the arc **real
height** on top of the channel's depth. SVG has no z-axis, so the height is built the way height
actually is: a **stack of copies stepping toward the light**, dark at the bottom of the wall and
lighter as it climbs, then a **lit top face** and a **bright top edge** with a dark counter-edge.
The result is a solid bar *sitting in* a groove rather than a coloured band *painted into* one.
It applies to the two soft-UI materials only — on glass or liquid it would contradict the
material — and it replaces their cast shadow rather than adding to it, because a stack that
already carries its own wall plus a drop shadow reads as two objects.

## The groove, rebuilt (2026-08-11)

Ido: wherever there should be a **recess** — the donut's track, the day/week/month/year bar —
it should look **deeper, more three-dimensional, like something solid**, per the Magnetic UI Kit
reference.

**The old one could not have worked, and the reason is mechanical.** It used `feDropShadow`,
which casts **outside** a shape. On a ring that produces a dark halo around the track, never a
channel cut into it. An inner shadow has to be **built**: offset the alpha, blur it, subtract it
from the original — the band that survives *is* the wall of the channel — then flood it. Twice,
in opposite directions: **dark where the light is blocked** (top-left) and a **bright rim where
it spills out** (bottom-right).

Three more moves, all of which the reference has and the old version did not:

1. **The channel is wider than the arc sitting in it** (`×1.30`), so its walls stay visible on
   both sides. This is most of what makes it read as solid rather than as a two-tone ring.
2. **The wall carries a gradient**, dark where it faces the light and lighter where it faces
   away — a cylinder cut into a solid is never one flat tone.
3. **Two hairlines**, a bright one on the outer lip and a near-black one on the inner lip.
   Blur alone reads as a smudge; an edge is what says *cut*.

The **segmented bar** gets the same treatment in CSS: a background **darker than the card it sits
in**, a hard near-black lip at the top, a bright rim at the bottom, and the selected pill
extruded above it with its own shadow and glow.

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
| **Dark neo · one neon accent** | **a deep shadow pair plus one saturated gradient** | charcoal groove, softly extruded arc, and a single cyan→blue accent that everything monochrome around it makes look bright |

## The fifth candidate, added 2026-08-11 from Ido's reference

Charcoal canvas, deep soft shadows, very large radii, and **exactly one saturated gradient**
(cyan → blue) reserved for whatever matters most on the screen. It is the most *fashionable* of
the five and the closest to the reference image, and it has one structural cost that only shows
up when you draw this app in it:

> **One accent leaves no room for six category hues.** The whole reason the accent reads as
> bright is that everything around it is monochrome — so a donut with six saturated life-area
> colours destroys the idea the style is built on. The honest rendering is a **ramp of the
> accent**, cyan through deep blue, and the consequence is that **colour stops carrying category
> identity**. The direct labels added in C12 rev 4 are what make that survivable: the name is on
> the slice either way.

It also has **no light scheme worth the name** — the style is a dark-mode style, and forcing it
light produces an ordinary grey soft-UI that is neither this nor `neo`. The light toggle keeps it
dark on purpose rather than pretending otherwise.

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
