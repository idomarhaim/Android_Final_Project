# `C12` · Charts, presentation and widgets — prototype

Asset for [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 4.**

```powershell
start docs\prototypes\2026-08-10-charts-presentation\index.html
```

Buttons pick the view; `←` / `→` cycle. **עברית** flips the frame to RTL, **Light / Dark**
switches scheme, and the widgets view gets a second row for the size class. No build, no
emulator, no network.

| view | what it is |
|---|---|
| **Home** | Decisions → Today → your goals → this week |
| **Home today** | The shipped order, from `DashboardScreen.kt`, for comparison |
| **Analytics** | The chart set |
| **Widgets** | All seven tiles at `2×2`, `4×2`, `2×4`, `4×4`, and a mixed home screen |
| **Design notes** | Not a screen — what the rejected forms were, what a widget really is, what the palette costs |

## Rev 4 — Ido's five notes

**1 · A new palette, and real volume.** The committed `GoalCategory.defaultColorHex` set is seven
**primaries at full chroma with unevenly spaced hues** — individually fine, but side by side in
one donut nothing holds them together, which is why they read as crayons. Replaced with a set
built *as a set*: one lightness, one chroma, hues evenly spaced, and a dark variant holding the
same relationship against `#0C1520`. **This replaces committed values, so `ThemePaletteTest` is
owed an update** — logged in the Design notes view rather than done quietly.

Volume is not a gradient bolted on: every arc, bar and ring gets a **three-stop fill** (tint →
hue → shade), a **specular bevel** (`feSpecularLighting` with a point light above-left), a
**sheen arc** along the lit edge, a **cast shadow**, and a **`feTurbulence` grain pass** in
soft-light so the surface has texture rather than flat plastic. The donut's slice caps are
**butt, not round** — a round cap adds half the stroke width past each endpoint, which at
`thick = 20` swallows any gap small enough to still read as one ring.

**2 · The donut names every life area.** Direct labels with **leader lines and per-side vertical
de-collision**, plus the percentage under each name — so no legend round-trip. Works in both
directions; the label side is geometric, so RTL needs no special case.

**3 · The two "rejected" cards moved out of the phone.** They were never app features — they are
shapes the effort-and-outcome card was drawn as and then thrown away. Rendering them *as app
screens* is precisely what made them unreadable, so they now live in a plain **Design notes**
panel that says, in one sentence each, what the thing was and why it lost. A third was added:
**revision 1's own ranking**, which its own test killed.

**4 · Widgets rebuilt at the same level** — accent glow keyed to each tile's subject, an icon
lozenge, a texture pass, denser content, and real gradients on every bar and ring.

**5 · Confirmed, and it changes what the build session owes.** These are **real Android app
widgets**, placed by the user on the launcher's home screen, outside the app: long-press
wallpaper → Widgets → GoalPilot → drag → resize. Three consequences are written into the Design
notes because they are constraints rather than details:

- a widget is **not a live screen** — Android renders a snapshot and refreshes on a schedule, so
  **nothing animates**, and the entry animation `ui/components/ChartAnimation.kt` exists for does
  not run there;
- the **launcher decides the real dp** a `2×2` or `4×4` occupies, and it varies by device and
  launcher, so every tile must survive being smaller than it is drawn here;
- a tap can only **open the app at a destination** — a widget cannot show a dialog — so "tick it
  here" means the app opens on that task.

## Decisions on the record

**Derived** from closed tickets: the chart set (`C3`, `C7`, `C16`, `C17`) · the divided-minutes
disclosure (`C17` §3) · retiring `HorizontalBarChart` · the RTL time axis.

**Handed back by Ido, then taken** (2026-08-10): the level ring on the avatar instead of a points
hero · no chart picker · the effort card ranks **only minutes** and **names** movement.

**Overturned by Ido**: the widget ban (2026-08-11) — re-cut as a size rule, the disclosure
shrinking to the smallest true sentence a tile can hold, never dropped · the committed category
palette (2026-08-11).

## The cost, stated rather than hidden

HTML, because `#12`'s Notes say *no ticket on this map ships code* and a throwaway Compose route
would take the Gradle daemon and an emulator for a question about layout. **So it cannot prove a
Compose chart lays out or animates, and it cannot prove a Glance widget fits its real cell** —
and the specular bevel and turbulence used here are **SVG filters, which Glance has no equivalent
for**: in a widget that depth has to come from a pre-rendered bitmap or from `Canvas` drawing,
which is the build session's problem and is named rather than hidden.

**Verified mechanically after every revision:** `node --check` on the extracted script → **OK**.
Hebrew guard → **71 literals inside the app, 0 unguarded** (each is the second argument of
`t(en, he)`, a `he:` / `meHe:` key, or an `L === 'he'` branch). One Hebrew string sits **outside**
the phone frame — the prototype's own `עברית` toggle, which is chrome, not app UI. Static guard
check, not a rendered-output test.
