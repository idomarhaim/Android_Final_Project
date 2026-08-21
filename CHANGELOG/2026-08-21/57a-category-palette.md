# `57a-category-palette` — 2026-08-21

> **Summary:** `#57` a — the harmonised category palette ships, with an authored dark twin per category and a fill/ink split; the prototype's own hexes could not be copied verbatim and the measurement says why, and "no dark blue neo" is confirmed as a *different* defect this brief does not fix.

**`#57` a — the harmonised category palette.** The first of four briefs on
[`#57`](https://github.com/idomarhaim/Android_Final_Project/issues/57), first
because it is a value change every other brief in the ticket renders.

Three things ship, and only the first was in the brief as written.

---

## 1. The brief asked for the prototype's hexes verbatim. They could not be copied, and that is the finding

`docs/prototypes/2026-08-10-charts-presentation/index.html` is the source of
truth, and the brief is explicit: *read the values out of it, do not re-derive
them by eye.* Two facts about **this app** stopped a verbatim port, and neither is
a judgement about the design:

1. **The prototype has seven categories; the app has ten.** `PROJECTS`,
   `NUTRITION` and `SLEEP` have no counterpart there at all.
2. **Six of its seven light hexes miss 3:1 against `AuroraSurface` (`#D0E2F5`)**,
   the app's own light ground. Measured, worst first: `FITNESS` **2.48**,
   `SLEEP` 2.68, `OTHER` 2.75, `NUTRITION` 2.88, `HEALTH` 2.97, `PROJECTS` 2.98.
   `Observed:` the prototype's own card is a `#FFFFFF` → `#EFF5FF` gradient
   (its `.card` rule, line 129), so nothing in it ever had to clear a ground as
   deep as this app's.

**What ported is everything except lightness.** Each of the seven keeps the
prototype's OKLCH **hue to within 0.7°** and its **chroma to within 0.007**; only
lightness moves, down far enough to clear the floor. Six of the seven hold chroma
to within 0.002 — the exception is `FITNESS` at 0.0064, where the sRGB gamut
simply has less chroma available at the lower lightness, not a choice. The three extras are synthesised numerically on
hues that fill the prototype's widest gaps — `PROJECTS` 107.6°, `NUTRITION`
149.9°, `SLEEP` 266.0°. Nothing was picked by eye.

**The prototype's own prose overstates what its values do**, which is worth
recording because it is what a later reader will trust. It says *"one lightness,
one chroma, hues evenly spaced"*; the actual hexes have an OKLCH lightness spread
of **0.105**, a chroma spread of **0.162**, and hue gaps from **35.7° to 121.4°**.
It is a much *more* harmonised set than the committed one, not a uniform one.

**What shipped, measured against what it replaced:**

| | committed set | prototype | **shipped** |
|---|---|---|---|
| OKLCH lightness spread | 0.155 | 0.105 | **0.057** |
| hue gaps (9 chromatic) | 3.8°–95.6° | 35.7°–121.4° | **15.8°–64.6°** |
| min pairwise separation | 97.1 | — (7 colours) | **99.4** |
| min contrast on any light card tone | — | 2.48 | **3.83** |

So the complaint — *"nothing holds them together"* — turns out to have been
mostly about **lightness scatter**, not hue: the shipped set cuts that by nearly
two thirds while *raising* separation above the set it replaces.

## 2. The fill/ink split — because ten text-safe hues at one lightness are mud

The old palette cleared 4.5:1 on white because every value was painted **both** as
a slice and as 14 sp bold text (`AnalyticsScreen` 630/701, `GoalsScreen` 259,
`GoalCard` 98, `LifeAreaDetailScreen` 271 — all `labelLarge`/`titleSmall`, i.e.
14 sp, well under WCAG's 14 pt-bold large-text threshold, so that exemption does
not apply). Holding ten evenly-spaced hues at one
lightness *and* at 4.5:1 was searched numerically and the best it produces is a
set of dark olives and browns — the failure one over from crayons.

So the two roles are separated:

- **`GoalCategory.defaultColorHex` / `darkColorHex` are fills** — slice, bar, dot,
  icon tint, gradient — guarded at the repo's own existing non-text floor of
  **3:1**, the same one `ThemePaletteTest` already asks of `outline`.
- **`String.toGoalInk()` derives the ink** — same hue, same saturation, lightness
  walked until it clears 4.5:1 against **every** card tone in the scheme. Derived
  rather than authored because life-area colours and user-picked hexes reach the
  same seam and have no table.

The solver (`Color.asInkOn`) is **pure Kotlin and non-composable on purpose**, so
`ThemePaletteTest` runs the shipped function over the real fourteen schemes on the
JVM instead of asserting a copy of its arithmetic.

## 3. The dark twin is authored — and harmonising the light set is exactly what made that necessary

Before this, dark mode ran every stored hex through a fixed HSL-lightness lift to
0.72. Measured over all forty-five pairs:

| dark set | min pairwise | OKLCH lightness spread |
|---|---|---|
| HSL lift of the **old** hexes (what shipped) | 57.6 | 0.158 |
| HSL lift of the **new** hexes | **37.2** | 0.228 |
| **authored twins** (what ships now) | **66.2** | **0.084** |

**The lift gets worse the more harmonised its input is**, because its damage
scales with how little saturation variety it has to work with: flatten `SLEEP`
`#516AA6` to one lightness and it lands on top of the neutral `OTHER`, 37 apart.
That is the argument for authoring the twins rather than computing them, and it
is not one I expected to find — it only appears once the light set is fixed.

The lookup is `GoalCategory.darkTwinOf(hex)`, consulted by `toGoalAccent()`;
anything unrecognised still takes the algorithmic lift, because a persisted column
needs a total function.

---

## 🧪 Tests

**JVM unit — `712 / 712 green, 0 failures, 0 errors, 0 skipped` across 67 classes.**
`ThemePaletteTest` is 12 cases (was 9): the two category tests became five.

| guard | threshold | measured |
|---|---|---|
| light fills distinguishable | > 90 (unchanged) | **99.4** (`FITNESS`/`PROJECTS`) |
| dark twins distinguishable | > 62 | **66.2** (`SLEEP`/`CAREER`) |
| category keeps its hue across schemes | < 12° | **8.7°** (`FITNESS`) |
| fills visible on every card tone | ≥ 3:1 | **3.83** light, **4.87** dark |
| derived ink readable on every card tone | ≥ 4.5:1 | passes ×14 schemes ×10 categories ×6 tones |
| derived ink total for a non-category hex | ≥ 4.5:1 | passes for black, white, `#5145CD`, `#00FF00` |

**The guard caught something on its first run, as the brief predicted it would.**
`HEALTH` at **2.965** against `AuroraSurface` — which is how the prototype's
shortfall above was found rather than assumed.

**It also caught a defect in itself.** The first draft picked light-vs-dark hexes
off `Case.dark`, the *requested* brightness. `AppMaterial.resolveDark` forces dark
neo dark in both, so the `AURORA / DARK_NEO / light` case carries an all-charcoal
tone ladder — that draft was testing the light hex against a charcoal card and
would have passed something the app never draws. It now reads the **rendered**
surface's luminance, which is what `toGoalAccent()` itself does.

**Instrumented — render pass, `2 / 2` green, 16 frames.**
`CategoryPaletteRenderPass` renders the real `DonutChart` with all ten categories
plus a legend row per category (fill as dot, fill as icon tint, ink as type),
across 2 skins × 4 materials × 2 brightnesses. Frames in
[`docs/render-passes/2026-08-21-57a-category-palette/`](../../docs/render-passes/2026-08-21-57a-category-palette/).
Slices are deliberately **equal tenths** — no size cue, so the only thing
separating two wedges is colour.

It needs **no sign-in**, which matters because `#58` left this AVD's Firebase auth
store wiped. Run via `adb install -r` + `am instrument`, so the app was not
uninstalled and no device state was changed.

**Seen, not just green.** The donut reads as one set in all four light frames —
deep, even in weight, no crayon effect — and the pastel twins hold together in
dark. The one pair the numbers flag as closest, `SLEEP`/`CAREER` at 66.2, is
visibly close **as two legend dots** while remaining distinct as two wedges; §4.1's
`.tag` rule (a category is written in words beside its dot) is what carries it.

---

## ❓ "No dark blue neo" — confirmed, and this brief does **not** fix it

The brief flagged Ido's `v0.3.0` report and asked whether this was the fix. It is
not, and the cause is measurable rather than impressionistic.

`AppMaterial.DARK_NEO` ships and is selectable. Its ground comes from
`MaterialPalettes.ramped()`, which rebuilds the surface as
`hslColor(hue, RAMP_GROUND_SATURATION = 0.08f, 0.115f)`. Aurora's own dark surface
`#0C1520` carries a saturation of **0.45**; dark neo clamps that to **0.08**, so
the rendered ground is `#1B1D20` — a near-neutral charcoal. Compare
`aurora-glass-dark.png` (visibly navy) with `aurora-darkneo-dark.png` (grey) in
the frames above: **dark neo is not missing, it is not blue.**

Nothing in that path touches `GoalCategory`. Left alone rather than fixed here: it
is a one-constant change to a material transform, it belongs to `#57`'s `b`/`c`
briefs or a ticket of its own, and changing it would move every dark-neo surface
under three sibling briefs that have not run yet.

---

## 📁 Files

**Changed** — `domain/model/Goal.kt` (palette + `darkColorHex` + `darkTwinOf`),
`ui/components/ColorExt.kt` (authored-twin lookup, `toGoalInk`, `asInkOn`,
`cardTonesOf`), `ui/components/GoalCard.kt`, `feature/analytics/AnalyticsScreen.kt`,
`feature/goals/GoalsScreen.kt`, `feature/lifeareas/LifeAreaDetailScreen.kt`,
`ui/ThemePaletteTest.kt`.

**New** — `androidTest/.../CategoryPaletteRenderPass.kt`, 16 PNGs under
`docs/render-passes/2026-08-21-57a-category-palette/`.

**Not touched** — `ui/theme/MaterialPalettes.kt` and `ui/theme/Theme.kt`, both of
which the brief's `owns:` listed. Nothing in the palette change needed them; the
seam that adapts a stored hex for a dark surface is `ColorExt.kt`, which the brief
did not list. `owns:` was corrected before claiming (`4a8c512`), along with
`domain/model/GoalCategory.kt`, a path that does not exist.
