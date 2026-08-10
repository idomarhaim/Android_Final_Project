# `C12` · Charts and presentation strategy — prototype

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 1.**

```powershell
start docs\prototypes\2026-08-10-charts-presentation\index.html
```

`←` / `→` cycle variants · **HE** flips the whole frame to RTL · **Light / Dark** ·
**Committed palette** renders the shipped `GoalCategory` hexes on the dark surface so the
defect is visible rather than argued. No build, no emulator, no network.

## Variants

| `?variant=` | What it is |
|---|---|
| `home` | **Home, proposed** — decisions banner → Today → your goals → this week → one setup row |
| `home-before` | **Home as it ships today**, rendered from `DashboardScreen.kt`'s item order, so the comparison is a picture rather than a claim |
| `analytics` | **The chart set** — donut, stacked column, and the new effort-and-outcome chart |
| `gap` | **The new chart on its own**, beside the two forms it deliberately is not |
| `widgets` | **The widget pack** ([#10](https://github.com/idomarhaim/Android_Final_Project/issues/10)) — what may and may not be one |

One week of data is shared by every variant: these are views of the same week, not five demos.
Minutes are already **divided** across goal edges (`C17` §3) and spans contribute **nothing**
(`C9a`, via `C9b`) — so the donut in `home` and the donut in `analytics` agree by construction.

## What is derived and what is yours

Every annotation beside the phone is tagged. **`derived`** means it follows from a closed
ticket and is logged rather than asked — overturn any of them. **`yours`** means the artifact
does not determine it.

Derived: the chart set (`C3`, `C7`, `C16`, `C17`) · the donut's divided-minutes disclosure
(`C17` §3) · retiring `HorizontalBarChart` from Analytics · the dark palette (`C9b`) ·
the RTL time axis · the widget rule.

Yours: demoting the points banner (it touches `C10`'s motivation design) · the refused chart
picker · whether an effort-versus-outcome picture is one you want to be shown at all.

## The cost, stated rather than hidden

This is HTML, for the reason `C9b` gave: `#12`'s Notes say *no ticket on this map ships code*,
and a throwaway Compose route would take the Gradle daemon and an emulator — two exclusive
singletons — for a question about layout. **So it cannot prove a Compose chart lays out,
animates, or survives a real `LazyColumn`.** That proof belongs to the build session, and
`ui/components/ChartAnimation.kt` exists because `animateFloatAsState` initialises *at* its
target — anything new here inherits that constraint.

The compensating gain is exact: `dir="rtl"` mirrors the frame in one attribute, so *"does this
chart survive Hebrew"* is something to look at. Two things it caught that prose would not have:
a stacked **time axis does not mirror** (the bars are drawn, not laid out, so Sunday stays on
the left unless you reverse them in code), and every number and range needs a **bidi isolate**
or `09:00–12:00` renders as `12:00–09:00`.

**Asserted mechanically, per `C9b`'s finding 3:** all 41 Hebrew literals in `index.html` are
language-guarded, so no Hebrew character can reach the English render.
