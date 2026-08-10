# `C12` · Charts, presentation and widgets — prototype

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 3.**

```powershell
start docs\prototypes\2026-08-10-charts-presentation\index.html
```

Buttons pick the screen; `←` / `→` also cycle. **עברית** flips the whole frame to RTL,
**Light / Dark** switches scheme, and on the widgets screen a second row picks the size class.
No build, no emulator, no network.

## Rev 3 — Ido's three notes, and what each one changed

**1 · "Match the colours and the design quality of prototype 1."** Rebuilt on `C9b`'s design
system rather than restyled toward it: the **committed Aurora tokens from `ui/theme/Color.kt`**
in both schemes, the same phone chrome (punch-hole, raking gloss, `feTurbulence` grain), the
same five-radial aurora canvas in light and dark, the Hebrew-first font stack, tabular numerals,
and the `.ltr` isolation class. The ten category hues keep `C9b`'s **tone-80 dark set verbatim**
for the four it defined and extend the same recipe to the six this ticket also needs — so the
two prototypes now render the same life area in the same colour.

**2 · "Widgets at every size class."** `2×2`, `4×2`, `2×4`, `4×4`, plus a **Home screen** view
mixing sizes on a real wallpaper with a dock. Sizes are built from a 76 dp cell and a 12 dp
gutter, so `4×4` lands at 340 dp — the width a 392 dp phone actually has after margins.

**3 · "Every card is also a widget."** All seven are: Decisions · Today · Your goals · This week
(donut) · Day by day (trend) · Effort and outcome · Level. **This overturns rev 1's rule** that
*a chart whose honesty depends on a footnote may not be a widget*, which had banned the donut
outright.

> **The rule became a size rule instead of a ban, which is the better answer.**
> The donut's disclosure is required by `C17` §3 — it *divides* shared minutes and has to say
> so. So every tile carries it, sized to the tile: **three words at 2×2** (*"shared time
> divided"*), one clause at `4×2` and `2×4`, the full sentence at `4×4`. No size ships without
> one. The ban was the lazy way to keep the invariant; sizing the sentence keeps it *and* gives
> Ido the widget he asked for.

Same treatment for the **Level** widget, which rev 2 also refused: it ships, and it carries the
one sentence that stops it lying — *points are your minutes, scored, not a separate score*.

## Screens

| button | what it is |
|---|---|
| **Home** | Decisions banner → Today → your goals → this week → one setup row |
| **Home today** | The shipped order, from `DashboardScreen.kt`, so the comparison is a picture |
| **Analytics** | Donut · day-by-day trend · where the time went and what moved |
| **Effort · rejected forms** | The chosen card beside the three it deliberately is not |
| **Widgets** | All seven, at four size classes and on a mixed home screen |

One week of data is shared by every screen and every widget — minutes already **divided** across
goal edges (`C17` §3), spans contributing **nothing** (`C9a` via `C9b`) — so a donut in a widget
cannot disagree with the donut on Home.

## Decisions on the record

**Derived** from closed tickets, logged rather than asked: the chart set (`C3`, `C7`, `C16`,
`C17`) · the divided-minutes disclosure · retiring `HorizontalBarChart` from Analytics · the
dark palette · the RTL time axis.

**Handed back by Ido, then taken** (2026-08-10): the level ring on the avatar instead of a points
hero, because the hero and the donut were the same minutes twice · no chart picker, replaced by
cards that hide themselves when they have nothing to say and a range picker that remembers ·
the effort card ships, rebuilt to rank **only minutes** and to **name** movement rather than
score it.

**Overturned by Ido** (2026-08-11): the widget ban, as above.

## The cost, stated rather than hidden

HTML, for the reason `C9b` gave — `#12`'s Notes say *no ticket on this map ships code*, and a
throwaway Compose route would take the Gradle daemon and an emulator for a question about
layout. **So this cannot prove a Compose chart lays out, animates, or survives a real
`LazyColumn`, and it cannot prove a Glance widget fits its real cell** — Android gives a widget
a size in dp that varies by launcher and by device. That proof belongs to the build session;
`ui/components/ChartAnimation.kt` exists because `animateFloatAsState` initialises *at* its
target, and anything new here inherits that.

**Verified mechanically after every revision, not by eye:** `node --check` on the extracted
script → **OK**. Hebrew guard → **72 literals inside the app, 0 unguarded** (each is the second
argument of `t(en, he)`, a `he:` / `meHe:` key, or an `L === 'he'` branch — those three are the
complete taxonomy). One Hebrew string exists **outside** the phone frame: the prototype's own
`עברית` toggle button, which is chrome, not app UI. It is a static guard check, not a rendered
-output test — no browser was driven.
