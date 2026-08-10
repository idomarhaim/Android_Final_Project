# `C12` · Charts and presentation strategy — prototype

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 2.**

> **Rev 2 · what changed.** Ido was asked the three questions rev 1 tagged *yours* and
> **handed all three back** with his standing instruction — *take the highest-quality answer and
> improve it if you can*. So all three are now taken by the agent and on the record, and the
> improvement was not cosmetic: **rev 1's effort-versus-outcome chart was killed by its own
> test**. It ranked areas by percentage moved, but a percentage is a fraction of *its own*
> target — one kilogram off a 5 kg goal scores 20% while one book of twelve scores 8%, for the
> same act of progress — so the ranking partly ranked *how modest the goals are*. It is kept in
> the `gap` variant as a rejected exhibit. What replaced it orders **only minutes**, and
> **names** movement instead of scoring it. Doing that surfaced a better headline than the
> ranking ever had: the area taking most of the week **has no measure at all**, so the app
> cannot say whether it moved.

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
| `analytics` | **The chart set** — donut, stacked column, and the new effort-and-outcome card |
| `gap` | **The new card on its own**, beside the three forms it deliberately is not — including rev 1's |
| `widgets` | **The widget pack** ([#10](https://github.com/idomarhaim/Android_Final_Project/issues/10)) — what may and may not be one |

One week of data is shared by every variant: these are views of the same week, not five demos.
Minutes are already **divided** across goal edges (`C17` §3) and spans contribute **nothing**
(`C9a`, via `C9b`) — so the donut in `home` and the donut in `analytics` agree by construction.

## What is derived and what is yours

Every annotation beside the phone is tagged. **`derived`** — it follows from a closed ticket and
is logged rather than asked. **`handed back`** — it was put to Ido, he declined to choose, and
the agent took it. Both are overturnable; neither was guessed silently.

Derived: the chart set (`C3`, `C7`, `C16`, `C17`) · the donut's divided-minutes disclosure
(`C17` §3) · retiring `HorizontalBarChart` from Analytics · the dark palette (`C9b`) ·
the RTL time axis · the widget rule.

Handed back, then taken: **the level ring on the avatar** instead of a points hero (the hero and
the donut were the same minutes twice) · **no chart picker**, replaced by cards that hide
themselves when they have nothing to say and a range picker that remembers · **the effort card
ships**, rebuilt so that it ranks only minutes and names movement rather than scoring it.

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

**Asserted mechanically, per `C9b`'s finding 3:** every Hebrew character in `index.html` sits in
one of exactly three guarded places — the second argument of `t(en, he)`, a `he:` / `meHe:` key,
or an `L === 'he'` branch. Re-run after every revision; **41 literals, 0 unguarded, 0 Hebrew
outside a string literal** as of rev 2. `node --check` on the extracted script: **OK**.
