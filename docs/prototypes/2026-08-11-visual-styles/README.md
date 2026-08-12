# Visual language — the four that ship

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). Ido, 2026-08-11: rev 4's
charts *"look like a balloon — dated, not pretty"*, with reference images for
**glassmorphism · metal · liquid glass · neo**, and a request for one prototype per style.

## Resolved 2026-08-12 — this is the picker, not a shortlist

Ido's decision: **all four remaining materials ship as a user-selectable skin** —
**glassmorphism · liquid glass · neo · dark neo** — **metal is deleted**, and the **raised-3D**
and **empty-channel** toggles stay.

So this file stopped being a comparison and became a **specification of four surfaces**. Metal was
**removed rather than hidden behind a flag**: a candidate nobody can pick is dead code that still
has to be carried through every later revision, and this file has already had twelve. What it cost
to delete is recorded here because it was genuinely the odd one out — it was the only material that
read well against **no background art at all**, and the only one whose *light* scheme was hard
rather than merely different (brushed metal on white drifts to grey).

What remains open after the decision is filed as
[`TODO/TODO_OPTIONAL/Presentation.TODO.optional.md`](../../../TODO/TODO_OPTIONAL/Presentation.TODO.optional.md),
including two Hebrew-only defects **this session found by rendering the page** and nobody had
reported.

```powershell
start docs\prototypes\2026-08-11-visual-styles\index.html
```

Buttons pick the material; `←` / `→` cycle. **עברית** flips to RTL, **Light / Dark** switches
scheme. **Compare all four** puts the same card in every material at once.

## Shared canvas — the default, at Ido's request (2026-08-11)

All four materials now sit on the **glassmorphism background**, so the only variable left is the
surface itself. **Native canvas** toggles back to the background each material was designed for,
which is worth one look because one of them changes character:

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
height** on top of the channel's depth. It applies to the two soft-UI materials only — on glass or
liquid it would contradict what the material *is* — and it replaces their cast shadow
rather than adding to it, because a body that already carries its own wall plus a drop shadow
underneath reads as two objects.

### Rebuilt 2026-08-12, after three defects that share one cause

The first attempt faked height with a **stack of translated copies**, and Ido named all three
symptoms: the blocks **climbed over each other**, they were **wider than the channel and cut its
walls**, and each one looked like **a pack of cards** rather than one body. Every symptom follows
from the same mistake — *N discrete copies instead of one solid* — so the fix is structural
rather than a matter of fewer or smaller steps:

- **Each slice is a closed annular sector**, not a stroked arc. A stroke has no outline, and
  without an outline there is no body to extrude and no face to light.
- **The side wall is one filled path** holding the face and its offset base as two subpaths.
  One fill means one silhouette and **no banding**, which is what read as cards.
- **The block is narrower than the channel** and the whole group is **clipped to the channel
  annulus**, so a body physically cannot cross a wall of the groove.
- **Two passes: every wall first, every face second.** That ordering is what stops a neighbour's
  wall from landing on top of a face, and the gap between slices widens from 2.4° to 4.2° when
  raised, because a body needs more clearance than a band does.

### The envelope, added after the second look

Ido: the blocks still read as flat — *as if they have no envelope, no side faces*. Correct, and
the union silhouette from the first rebuild was the reason: **a silhouette is not a face.** It
has no fold, no tone of its own and no end, so it reads as a shadow behind the block. A solid
needs its faces **drawn as faces**, so each block now carries four:

- an **outer wall** with its own gradient,
- an **inner wall**, darker because it faces away,
- and an **end cap at each end of the slice** — these are what make a block read as its own slab
  rather than as a segment of one painted ring.

Two supporting changes, both of which the flat version did not need. A **fold hairline** where a
wall meets the top face, without which the two merge into one gradient and the body flattens
again. And the **clip is widened by the extrusion**: a bar that rises out of a groove really does
extend past the groove's mouth, so clipping it to the channel exactly is what would make a raised
block look flush. The *face* stays narrower than the channel, so this does not re-open the
"blocks cut the walls" defect.

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
| **Liquid glass** | **refraction at the edge** | translucent body, bright inner rim where light enters, dim outer counter-rim, one specular streak |
| **Neo (soft UI)** | **a shadow pair** on one flat surface | inset track, softly extruded arc, muted hues, no rim and no gloss |
| **Dark neo · one neon accent** | **a deep shadow pair plus one saturated gradient** | charcoal groove, softly extruded arc, and a single cyan→blue accent that everything monochrome around it makes look bright |

## Dark neo, added 2026-08-11 from Ido's reference

Charcoal canvas, deep soft shadows, very large radii, and **exactly one saturated gradient**
(cyan → blue) reserved for whatever matters most on the screen. It is the most *fashionable* of
the four and the closest to the reference image, and it has one structural cost that only shows
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

## Notes per material — now read as *what each one costs to ship*, not as how to choose

- **Glassmorphism** needs a busy canvas underneath or the frost has nothing to frost. Cheap in
  Compose (`Modifier.blur` + translucent surfaces) and it survives both schemes well.
- **Liquid glass** is the current Apple direction and the closest to your reference images. It is
  the most expensive to imitate faithfully: real refraction needs a runtime blur of *what is
  behind*, which in Compose means `RenderEffect` (API 31+) and a fallback below that.
- **Neo** is the calmest and the most legible in Hebrew, because nothing competes with the text.
  Its known weakness is **contrast** — shadow-only affordances can fail accessibility checks, so
  buttons need a real contrast anchor, not just an extrusion.

## The cost, stated

`backdrop-filter`, SVG filters and CSS shadow pairs are **web** primitives. In Compose the
equivalents are `Modifier.blur`, `RenderEffect`, and hand-drawn `Canvas` shadows — and **a widget
has none of them**, so the home-screen tiles need a version that survives being drawn into a
`RemoteViews` bitmap. Named here rather than found later — and the decision that **all four ship**
multiplies it rather than picking one, which is why it is now a TODO item and not a footnote.

## Verified

Re-run after the metal deletion (rev 13), against the file as it now stands:

- `node --check` on the extracted `<script>` → **OK**.
- **Hebrew guard: 32 Hebrew-bearing string literals, 0 unguarded.** One Hebrew string sits outside
  the phone frame — the prototype's own `עברית` toggle, which is chrome rather than app UI, and is
  named rather than folded into the count.
- **No `metal` identifier survives** anywhere in the file except the changelog comment recording
  the deletion; checked mechanically rather than assumed.
- **Rendered and looked at**: `compare` in dark, and `neo` raised on its native canvas **in
  Hebrew**. No regression from the deletion — and the Hebrew render is where the two open defects
  in [`Presentation.TODO.optional.md`](../../../TODO/TODO_OPTIONAL/Presentation.TODO.optional.md)
  came from.

**A bug in the instrument, fixed in the same pass.** `shoot.ps1 -Probe` read the page with
`Get-Content -Raw` and no `-Encoding`, so a UTF-8 file with no BOM was read in the ANSI codepage
and written back as UTF-8 — double-encoding every Hebrew character. Every close-up probe render
therefore showed mojibake where Hebrew should be, which means **the one instrument for judging a
Hebrew design close up could not display Hebrew**. One-word fix (`-Encoding UTF8`), verified by
re-rendering the same probe.
