# c12-charts-presentation — claimed #31, the first screen ticket under the new design standard

> **Summary:** claimed #31, the first screen ticket under the new design standard

**Session:** `c12-charts-presentation` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#31 · `C12`](https://github.com/idomarhaim/Android_Final_Project/issues/31) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked **bare**, so the frontier pick was the agent's and the reasoning is
recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block — every open child of `#12` queried for `blocked_by`:

| Ticket | Blocked by | Verdict |
|---|---|---|
| `#31 · C12` | `#18`, `#14` (both closed) | **frontier — claimed** |
| `#21 · C5` | `#13`, `#18` (both closed) | frontier — left |
| `#19 · C1` | `#18`, `#39` (both closed) | unblocked but **assigned and live** |
| `#28 · C9e` | `#27` (closed) | unblocked but **assigned and live** |
| `#20`, `#22`, `#24`, `#30`, `#35` | all wait on `#19`, directly or through `#24` | still blocked |

Map size verified against GitHub: **25 children, 16 closed, 9 open** — unchanged in size from
`c9e-event-lifecycle`'s count, but the **frontier has shrunk from three to two**, because that
session took one of them. **Seventh** derivation of the day.

**Why `#31`:** the single reason it was declined three times — *"a second concurrent prototype
contends for Ido"* — named [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26)
as the first one every time, and `#26` is now **closed and released**. There is no live
prototype on the board. What is left is that **every** frontier ticket is HITL, which
`c9e-event-lifecycle` already established discriminates nothing on its own — so the
discriminator has to be disjointness of subject, and there the two remaining tickets separate
cleanly. `#31`'s blockers `C3` (#18) and `C7` (#14) are both closed **and released**, so its
inputs are foreign state to read; it has exactly **one** live edge, `#19`'s re-scoring pass,
and *what the user sees when a number they relied on moves* is a fact to read off `#19`'s
resolution rather than a decision to co-author.

`#21 · C5` was declined on the objection four earlier sessions raised, now pointing at a
different live row: `C5` decides **where recurrence lives**, recurrence produces occurrences,
and `#28 · C9e` is **live right now** deciding what happens to a synced event *when its task
changes*. Moving recurrence onto a new concept between goal and task changes what *"its task"*
denotes — a live session's inputs changed mid-flight.

`#31` is also the only frontier ticket with leverage **outside** this map: issue
[#10](https://github.com/idomarhaim/Android_Final_Project/issues/10) (`U5`, the widget pack) is
explicitly waiting on it. The board's Unclaimed-work block independently says *"take this one
first"* — read **after** this derivation, not before, and recorded as agreement rather than as
the reason.

## Claim

- **GitHub:** `#31` assigned to `idomarhaim` **before any other work**, so concurrent sessions
  skip it. Assignment confirmed by reading it back out of GitHub.
- **`SESSIONS.md`:** row added to *Active claims* with paths and singletons, plus the frontier
  reasoning and three coupling points. The singleton column is **not** `none` on this row — see
  §3 below.

## Coupling points, named on claiming

1. **`#12`'s *Decisions so far* is a commons**, and the race it names has fired for real once
   (`c3-points-currency` records it from both sides). Re-fetch the body immediately before
   appending, write only this session's line, verify a pure insertion. `C13` (#32)'s index gap
   stays Ido's to assign.
2. **`C12` arrives with a standard and two hand-offs already binding it, all from released
   sessions — so they are inputs, never subjects.** `#12`'s Standing preferences now carry
   *"every screen is designed to a current UI/UX standard"* plus the three rules `C9b`'s eight
   revisions bought: **one chip may not carry two axes** · **form and words before
   iconography** · **a design is not finished until it has been seen in Hebrew**. `C9b` handed
   this ticket two concrete items besides — **where the daily review lives**, and that **spans
   contribute nothing** to the time-allocation chart, or one week-long renovation swamps every
   life area. Anything found here that bears on `#19` is **posted there**, not decided there.
3. **The singleton on this row is Ido himself, and the board cannot enforce it.** Two live
   grillings (`#19`, `#28`) already ask for his attention and this adds a **prototype**, the
   heavy kind — `#26` spent eight revisions of it. Named on claiming rather than discovered
   later: revisions ship **one at a time** and stop the moment he stops answering, and no
   revision waits on the other two sessions.

## `kb-candidates/`

Re-listed at session start, as the folder's existence requires — **six files**, one fewer than
`c9e-event-lifecycle` saw, because `c9b-calendar-surface` drained and deleted its own on
release. Each of the six was **opened and its `Destination` line read** rather than inherited
from the board's note. Five target `rules/`: `c3`, `c18` and `c14` are one accumulating
amendment to `rules/question-axis-naming.md` and should be read together; `c16` targets
`rules/agent-topology-and-model-routing.md` §5; `c9c` the ❓ Ambiguity picker guidance. The
sixth, `2026-08-09-c9f-consent-screen-state.md`, names `kb/dev/` but is **parked by Ido's own
call at the last drain** pending a `rules/` proposal. **All six are always-ask in both modes
and none is this session's** — `AUTO MODE` drains nothing here.

## 🧪 Tests

**None run, and none owed by this unit.** This is a `wayfinder:prototype` ticket on a planning
map — the map's standing preference is *plan, don't do*, and no ticket on it ships code.
Nothing in this unit touched `app/`, `functions/` or `firestore-tests/`, so no server, client,
endpoint, database or UI layer was exercised. The unit's own verification is structural and was
performed: the frontier was re-derived from the dependencies API rather than trusted, the
assignment was read back out of GitHub, every `kb-candidates/` destination was read from the
file rather than from the board's summary of it, and the map body will be re-fetched
immediately before its index line is appended.

*(The prototype this ticket produces will be standalone HTML under
`docs/prototypes/2026-08-10-charts-presentation/`, as `C9b`'s was — reviewed by eye, not by a
test layer. It compiles nothing and runs no Gradle task, which is why this row claims no build,
device or Firebase singleton.)*

## Revision 1 — the prototype

`docs/prototypes/2026-08-10-charts-presentation/` — standalone HTML, five variants, `←`/`→`,
**HE** flips to RTL, **Light/Dark**, and a **Committed palette** toggle that renders the shipped
`GoalCategory` hexes on the dark surface. Posted to `#31` as
[the prototype comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5245204579).

**The headline finding is a false fork, caught before options were drawn.** `A7` asks whether
Home should answer *what do I do now?* rather than *how am I doing?*. `C9b` already gave the
now-question its own home — a **Calendar tab** plus a **Home banner that opens the decision
stack** — so Home is not choosing between the two questions at all. It links to one and hosts
the other, and only the order was ever open. Drawing three rival dashboards would have repeated
exactly the failure `C9b` spent eight revisions discovering.

**And the arrangement defect was not the donut's position.** Counted off `DashboardScreen.kt`'s
item order rather than inferred from the complaint: two setup cards that never retire and five
generic tips are **four of the six blocks above the user's own goals**. `home-before` renders
that order so the comparison is a picture, not a claim.

**Chart set.** `DonutChart`, `StackedColumnChart` and `ProgressRing` stay. **`HorizontalBarChart`
is retired from Analytics — it loses both its users to decisions this map already took:**
*Progress by goal* is a bar list over goals that `C7` says may carry **no measure at all** (and
"no measure" is `C7`'s **default**), and *Task focus* weights by **count**, the weight `C16`
killed and `C3` re-killed. **One chart is added** — effort against outcome, which is `C3`'s own
sentence taken literally (*"the gap … is the most valuable thing this app can show a person"*)
and which nothing in the app draws.

**Its form is forced rather than chosen, and that is the second finding.** Effort is minutes,
outcome is percentage points, and `C17` ruled progress **owned, never pooled** — so there is no
legitimate total to take a share of, and any chart putting both on one scale performs arithmetic
the model forbids. Rankings survive incommensurable units, so it is two ranked columns joined by
lines. The two rejected forms are drawn beside it: paired bars invite a length comparison the
numbers cannot support, and a single `% per hour` score is **one mark carrying two axes** — the
chart form of the rule `C9b` bought — which deletes the gap it claims to measure.

**Widget rule for [#10](https://github.com/idomarhaim/Android_Final_Project/issues/10):** *a
chart whose honesty depends on a footnote may not be a widget.* The donut fails it because
`C17` §3 **requires** the divided-minutes disclosure. Next block, decisions waiting and one
measured goal's ring pass. `#10` is unblocked by a rule rather than by a list of drawings.

**Two things the prototype caught that prose would not have:** a stacked **time axis does not
mirror** under `dir="rtl"` — bars are drawn, not laid out, so Sunday stays on the left in Hebrew
unless the series is reversed in code; and the **dark palette bill falls due on this ticket**,
because every chart is built from `GoalCategory.defaultColorHex`, which `C9b` found is
light-mode-only.

Three items are tagged **yours** in the prototype and in the comment, not derived: demoting the
points banner (it touches `C10`'s motivation design), refusing a chart picker, and whether an
effort-versus-outcome picture is one Ido wants shown at all.

## Revision 2 — the three open questions were handed back, and one of the answers killed rev 1's own chart

Ido was shown the picker for the three items rev 1 tagged **yours** and answered all three
identically: *"I could not fully understand the options — explain simply and schematically,
choose the answer that gives the highest standard and quality for the app and its purpose,
UX/UI and the software, and improve it if you can."* That is a **delegation, not a waiver** —
the record distinguishes them — so all three are taken by the agent and on the record, and each
was re-checked for a false fork rather than picked out of its own option set. Posted as
[the rev 2 comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5245560781).

1. **Points on Home — the fork was false.** `C3` makes points `round(minutes / 3) × difficulty`,
   **computed from minutes**, and the time donut on the same screen is built from those same
   minutes. Home was showing **one quantity twice**, so removing the hero loses **no
   information**. What it would have lost is the motivation, which is kept rather than deleted:
   **the level becomes a ring around the avatar**, number on it — glanceable, zero vertical
   space, not the headline. The points widget falls with it, as the two always stood together.
2. **No chart picker, and the need behind one met a better way.** Cost was never the
   discriminator. The app already lets the user pick the **range** and the **focused slice** —
   *queries*, not layout — and the wish behind a layout picker is almost always *"this card is
   useless to me"*, which is a design failure to fix for everyone. Two improvements instead:
   **a card with nothing to say hides itself** (the effort card needs ≥1 measured goal, the
   trend needs >1 day), and **the range picker remembers the choice**, which is the honest use
   for `AppPreferencesRepository`.
3. **The effort card ships — but rev 1's form was wrong, and this is the real improvement.**
   Rev 1 argued rankings survive incommensurable units. They do; **that particular ranking did
   not survive its own test.** A percentage is a fraction of *its own* target, so one kilogram
   off *"lose 5 kg"* scores **+20%** while one book of *"read 12 books"* scores **+8%** — the
   same act of progress. Ranking by movement therefore partly ranks **how modest the goals
   are**, which is the failure `C16` killed as count-weighting and `C3` killed again as
   points-weighting. Rev 1's chart is retained in the `gap` variant as a **third rejected
   exhibit**.

**What replaced it: order only the quantity you may order.** Minutes are poolable and
comparable, so the app ranks them; movement is not, so the app **names** it — the goals that
actually moved, with their own numbers, beside the bar. No cross-goal ordering is asserted.
**And the honest render found a better headline than the ranking had:** the area taking most of
the week **has no measure at all**, so the app cannot say whether it moved — a fact about the
model rather than a verdict on the week, pointing at the one action that changes it. It also
disposes of the accusation risk that made the question Ido's in the first place: no area is
scored, so none is blamed.

## 🧪 Tests

**Still none owed — no ticket on this map ships code — but the mechanical assertions were run
against the artifact rather than eyeballed:**

- **JS parses.** The `<script>` block was extracted and passed through `node --check` → **OK**,
  at rev 1 and again after every rev 2 edit. A prototype that silently fails to render is worse
  than no prototype.
- **No Hebrew literal can reach the English render** — `C9b`'s finding 3, asserted absolutely
  rather than sampled, and **re-asserted after the rev 2 rewrite**. Every literal containing a
  character in the Hebrew Unicode block was enumerated: **41 found, 41 language-guarded**
  (behind `t(en, he)`, a `he:` / `meHe:` key, or an `L === 'he'` branch — those three are the
  complete taxonomy of where Hebrew appears in the file), **0 unguarded**, and **0 lines** carry
  Hebrew outside a string literal. Stated precisely: this is a **static** guard check, not a
  rendered-output test — no browser was driven.
- **No dead reference to the deleted chart.** `slopeChart` was rev 1's function; after the rev 2
  rewrite the file contains **0** references to it, checked rather than assumed.
- **Re-run after the rev 3 rebuild and again after the rev 4 rewrite**, each of which replaced
  the whole file: `node --check` → **OK** both times; Hebrew guard → **72 literals at rev 3, 71 at
  rev 4, 0 unguarded** either time. One Hebrew string now lives
  **outside** the phone frame — the prototype's own `עברית` toggle button, which is chrome and
  not app UI. Named here rather than folded into the count, because an assertion that quietly
  moves its own boundary is worth less than the number it reports.

Layer coverage is otherwise unchanged and unowed: nothing in this unit touched `app/`,
`functions/` or `firestore-tests/`, so no server, client, endpoint, database or UI layer was
exercised. **The cost is stated in the README rather than hidden: this cannot prove a Compose
chart lays out, animates, or survives a real `LazyColumn`** — that proof belongs to the build
session, and `ChartAnimation.kt` exists because `animateFloatAsState` initialises *at* its
target.

## Revision 3 — Ido's three notes, and the third overturned a rule this session wrote

Feedback given 2026-08-11 on rev 2, three items, all taken. Posted as
[the rev 3 comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5245983955).

1. **"Match prototype 1's colours and design quality."** Rebuilt on `C9b`'s design system rather
   than restyled toward it: the **committed Aurora tokens from `ui/theme/Color.kt`** in both
   schemes, the same phone chrome (punch-hole, raking gloss, `feTurbulence` grain), the same
   five-radial aurora canvas light and dark, the Hebrew-first font stack, tabular numerals, the
   `.ltr` isolation class. The category hues carry the most weight here because **charts are
   where that palette lives**: `C9b`'s **tone-80 dark set is kept verbatim** for its four
   categories and the same recipe cut for the six more this ticket needs, so the two prototypes
   now render the same life area in the same colour and `C9b`'s light-mode-only finding is
   answered rather than restated.
2. **"Widgets at every size class."** `2×2`, `4×2`, `2×4`, `4×4`, plus a mixed **home screen**
   on a wallpaper with a dock. Built from a 76 dp cell and a 12 dp gutter, so `4×4` lands at
   340 dp — the width a 392 dp phone actually has after margins. Each size is a **different
   design**, not one tile scaled.
3. **"Every card is also a widget."** All seven ship — Decisions, Today, Your goals, This week,
   Day by day, Effort and outcome, Level.

**The third note overturned rev 1's own rule, and the repair is the finding.** Rev 1 wrote *a
chart whose honesty depends on a footnote may not be a widget* and used it to **ban the donut**.
Ido overturned the ban. The right repair was not to drop the invariant but to **re-cut it as a
size rule**: *the disclosure shrinks to the smallest true sentence the tile can hold, and no size
ships without one.* `C17` §3 requires the donut to say it **divided** shared minutes, so it says
so in **three words at 2×2**, one clause at `4×2`/`2×4`, and the full sentence at `4×4`. **The
ban was the lazy way to keep the invariant.** Same treatment for the **Level** widget that rev 2
had also refused: it ships carrying the sentence that stops it lying — *points are your minutes,
scored, not a separate score*.

Worth keeping as a general result rather than a one-off: **"this cannot be a widget" was really
"this cannot be a widget at every size"**, and the two are different claims. Any future tile
carrying a derived or divided number inherits the same test.

**Also stated rather than hidden:** this prototype **cannot prove a Glance widget fits its real
cell** — Android hands a widget a dp size that varies by launcher and by device — on top of the
Compose-layout proof it already could not give.

## Revision 4 — five notes from Ido, and one of them was a property of the set rather than of any colour

Feedback 2026-08-11 on rev 3. Posted as
[the rev 4 comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5246235946).

**5 · Confirmed first, because it changes what is owed.** These are **real Android app widgets**,
placed by the user on the launcher's home screen, **outside the app**. Three consequences written
into the prototype's Design notes rather than left for the build session to discover: a widget is
**not a live screen** (Android renders a snapshot on a refresh schedule, so **nothing animates**,
and `ChartAnimation.kt` does not run there) · the **launcher decides the real dp** of a `2×2` or
`4×4`, varying by device and launcher, so every tile must survive being smaller than drawn · a tap
can only **open the app at a destination**, since a widget cannot show a dialog.

**1 · The palette, and the diagnosis is the finding.** The charts were using
`GoalCategory.defaultColorHex` — **seven primaries at full chroma with unevenly spaced hues**.
Each is fine alone; side by side in one donut nothing holds them together and the lightness jumps
per slice. **That is a property of the set, not of any one colour**, which is why "pick nicer
reds" would not have fixed it. Replaced with a set built as a set — one lightness, one chroma,
hues evenly spaced, dark variant holding the same relationship against `#0C1520`. **It replaces
committed values, so `ThemePaletteTest` is owed an update**; logged in the Design notes rather
than done quietly.

**Volume was the other half.** Every arc, bar and ring now carries a three-stop fill
(tint → hue → shade), a **specular bevel** (`feSpecularLighting`, point light above-left), a sheen
arc along the lit edge, a cast shadow, and a **`feTurbulence` grain pass** in soft-light. One fix
worth naming because it is the kind that ships wrong: the donut's slice caps are **butt, not
round** — a round cap adds half the stroke width past each endpoint, so at `thick = 20` the caps
swallow any gap small enough to still read as one ring.

**2 · Direct labels.** The donut names every life area beside its slice, with leader lines,
per-side vertical de-collision and the percentage under each name. The label side is geometric,
so **RTL needs no special case**.

**3 · The two "rejected" cards left the phone, and that was the actual mistake.** They were never
app features — they are shapes the effort card was drawn as and thrown away — and **rendering
them as app screens is what made them unreadable**. They now sit in a plain **Design notes**
panel outside the phone, one sentence each on what it was and why it lost, with revision 1's own
ranking added as a third.

**4 · Widgets rebuilt to the same level** — accent glow per subject, icon lozenge, texture pass,
denser content at every size, gradients on every bar and ring.

**Stated rather than hidden:** the bevel and the turbulence are **SVG filters, and Glance has no
equivalent** — in a real widget that depth must come from a pre-rendered bitmap or `Canvas`
drawing. Named here rather than left to be found.

## Revision 5 — the material is a separate question, so it got a separate prototype

Ido, 2026-08-11: rev 4's charts *"look like a balloon — dated, not pretty"*, with reference
images and a request for **one prototype per style: glassmorphism · metal · liquid glass · neo**.
Built as `docs/prototypes/2026-08-11-visual-styles/` and posted as
[the four-materials comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5267476085).
The C12 prototype is deliberately **untouched**: the material is a different question from the
layout, and mixing them would re-open decisions already taken.

**The balloon has a name, and naming it changed the fix.** It was
**`feSpecularLighting` over a fat stroke** — a filter that simulates a lit, rounded *solid*, so a
20 px arc inflates into a tube. That means the repair is not "less shadow" but **getting depth
from something other than simulated roundness**, which is exactly what separates the four
candidates:

| material | depth from |
|---|---|
| Glassmorphism | **blur** — the canvas stays legible through the panel |
| Metal | **anisotropic reflection** — sheen banded across the stroke, hairline edges, milled groove |
| Liquid glass | **refraction at the edge** — bright entry rim, dim counter-rim, one specular streak |
| Neo (soft UI) | **a shadow pair** on one flat surface — inset track, extruded arc, muted hues |

There is **no `feSpecularLighting` anywhere** in the new file; checked mechanically, not assumed.

**One file rather than four, and the reason is the question's shape.** Four separate files are
four pages looked at alone; this question is **comparative**. So the content is held absolutely
constant — same screen, same week, same harmonised palette — and only the surface varies, which
is also what makes the **Compare all four** view possible. Two things carry across all four
because they are decisions rather than styling: the donut's direct labels, and the palette.

**Each candidate's cost is stated, because it is part of choosing:** glass needs a busy canvas or
the frost has nothing to frost · metal is the only one that works with no background art but its
light scheme drifts to grey · liquid glass is closest to the references and the most expensive,
since real refraction needs `RenderEffect` (**API 31+**) and a fallback below it · neo is the
calmest and most legible in Hebrew but its shadow-only affordances **fail contrast checks**
without a real anchor.

**And the platform caveat is named rather than found later:** `backdrop-filter`, SVG filters and
CSS shadow pairs are **web** primitives; Compose's equivalents are `Modifier.blur`,
`RenderEffect` and hand-drawn `Canvas` shadows — and **a widget has none of them**, so whichever
material wins, the tiles need a version that survives being drawn into a `RemoteViews` bitmap.

**Consequence flagged for Ido rather than assumed:** once he picks, the material belongs in
`#12`'s **Standing preferences** beside the design standard, which makes it binding on
`C6` [#22](https://github.com/idomarhaim/Android_Final_Project/issues/22) — now live under
`c6-log-progress` — and every later screen, not only this ticket's.

## Revision 6 — one canvas for all four, and neo is the one it changes

Ido, 2026-08-11: put all four materials on the **glassmorphism background** so the comparison
isolates the surface, then he picks. Done and made the **default**, with a
**Shared canvas / Native canvas** toggle so the background each material was designed against is
one click away rather than deleted. Posted as
[the shared-canvas comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5268496000).

**The finding is that the request is not neutral for every candidate, and neo's case is
definitional rather than cosmetic.** Neumorphism **is** the surface being the same colour as what
is behind it — that identity is what makes the shadow pair read as an *extrusion of the
background* instead of a card floating over it. A gradient has no single such colour, so the
honest rendering on a shared canvas is a **neutral plate carrying the shadow pair**, which gives
the card an **edge it would not otherwise have**. On this background Ido is therefore choosing
between *soft UI on a canvas* and three others — not between neumorphism and three others. Told
to him plainly rather than rendered silently, because the two are different offers and the
difference only shows up after the choice.

**Metal shifts mildly** — designed against graphite where it reads as a machined object, it now
reads as a metal panel *placed on* something. **Glass and liquid glass are unaffected**: glass
was already this canvas, and liquid glass needs a colourful background anyway, since refraction
has nothing to refract without one.

Implementation note kept small on purpose: the shared canvas is **two class selectors**
(`.st.shared`), which outranks each material's own single-class background rule without touching
any of them — so the native rendering is preserved exactly rather than reconstructed.

## Revision 7 — a fifth material, and picking it would make an earlier decision load-bearing

Ido, 2026-08-11, with a reference image: add another style to the comparison — dark soft-UI with
a single neon accent. Added as **Dark neo · one neon accent**; *Compare all five* now shows it
beside the rest on the shared canvas. Posted as
[the fifth-material comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5268733102).

**The finding is a coupling nobody would see from the reference image.** The style's mechanism is
that **exactly one** saturated gradient exists and everything else is monochrome — that is what
makes the accent read as bright. But this app's donut has to show **six life areas**. Six
saturated hues would destroy the single-accent idea outright, so the honest rendering is a
**ramp of the accent**, and the consequence is that **colour stops carrying category identity**:
in the other four materials purple *means* Studies, here the slices are five shades of one hue.

**That is survivable only because of a decision already taken.** C12 rev 4 put the life-area
**name directly beside every slice**, so identity is carried by the label rather than the colour.
Without those labels this style would not be a candidate for this app at all — which means
**picking dark neo makes the direct labels load-bearing rather than a nicety**, and that is now
on the record for him before he chooses rather than after.

Two smaller facts stated rather than left to be discovered: it is **a dark-mode style with no
real light scheme** — forcing it light yields an ordinary grey soft-UI that is neither this nor
`neo`, so the light toggle deliberately keeps it dark, and choosing it is also choosing that
GoalPilot is dark-only or that light mode is a *different* material; and on the shared canvas it
takes **a plate**, like `neo` and for the same definitional reason, so it is the candidate that
loses the most on a colourful background.

## Revision 8 — the groove was built with the wrong filter, so no amount of tuning would have fixed it

Ido, 2026-08-11: wherever there should be a **recess** — the donut track, the day/week/month/year
bar — make it deeper, more three-dimensional, like something solid, per the Magnetic UI Kit
reference. Posted as
[the groove comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5269131005).

**The finding is that the previous version could not have worked.** It used `feDropShadow`, which
casts **outside** a shape — on a ring that is a dark halo *around* the track, never a channel
*cut into* it. The failure was the **mechanism**, not the parameters, which is why it looked flat
at every opacity tried. An inner shadow has to be built: offset the alpha, blur it, **subtract it
from the original** — the surviving band *is* the wall — then flood it, twice, in opposite
directions: dark where light is blocked, bright rim where it spills out.

Three more things the reference has and the old version did not, each of which is a separate
reason it read as printed rather than machined:

1. **The channel is wider than the arc in it** (`×1.30`), so its walls stay visible on both
   sides — in the reference the accent arc clearly sits *inside* a wider dark groove.
2. **The wall carries a gradient**, dark facing the light and lighter facing away; a cylinder cut
   into a solid is never one flat tone.
3. **Two hairlines**, bright on the outer lip and near-black on the inner. **Blur alone reads as
   a smudge; an edge is what says *cut*.**

Applied to both soft-UI materials (`darkneo` and `neo`), on the donut track **and** the goal
rings. The **segmented bar** gets the same reasoning in CSS: darker than the card it sits in — a
recess always is — with a hard lip at the top, a bright rim at the bottom, and the selected pill
**extruded above** it, so the contrast between pressed-in and raised-out carries the depth.

Still **no `feSpecularLighting` anywhere**: the depth here is channel geometry, not simulated
roundness, which is what the balloon complaint was about in the first place.

## Revision 9 — a groove you can only see under an arc cannot be judged

Ido, 2026-08-12: make a chart that is **not 100%**, so the channel underneath is visible and the
groove can be assessed; and add — **as an extra option, not a replacement** — an arc with
**realistic 3D height** on top of the channel's depth. Both shipped as top-bar toggles. Posted as
[the two-toggles comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5269367558).

**1 · Empty channel** (default on). The donut summed to 100% by construction, so the channel was
only ever visible *underneath* an arc — which made the request a genuine gap in the artifact
rather than a preference. Slices are now sized against a **26-hour week** instead of the tracked
total, so the ring stops at ~71% and the remainder is bare channel with the lip, the wall
gradient and both hairlines on show. **Stated as a rendering test, not a proposed metric**: the
centre caption reads *of a 26h week* so the picture never claims something the data does not, and
a real weekly-hours target would be `C1`/`C3`'s question, not this one.

**2 · Raised 3D arc** (default off, additional). SVG has no z-axis, so height is built the way
height is: a **stack of copies stepping toward the light** — that stack *is* the side wall — plus
a **lit top face** and a **bright top edge with a dark counter-edge**, because an edge is what
separates a face from a wall. Applied to the donut and the goal rings on both soft-UI materials.

**One decision named rather than left implicit:** when the arc is raised, its **cast shadow is
removed**, not kept. A stack that already carries its own wall, plus a drop shadow underneath,
reads as **two objects** — a bar and a separate dark smear — which is the same class of error as
the original halo. Dark neo keeps its glow, because that is light rather than shadow.

**Deliberately not applied to glass, liquid or metal.** Height would contradict what each of
those materials *is*: glass has no solid to extrude, liquid's whole claim is refraction through a
body, and metal's depth is reflection along a surface. The toggle is a **no-op** there rather
than a worse version of them.

Still **no `feSpecularLighting`** — worth re-checking on this revision specifically, since
"realistic 3D height" is exactly the request that would invite the balloon back. It did not: the
depth here is walls and edges, not simulated roundness.

## Revision 10 — three reported defects, one cause, and the fix is structural

Ido, 2026-08-12, on rev 9's raised arc: the blocks **climb over each other**, they are **wider
than the channel and cut its walls**, and each looks like **a pack of cards** rather than one
body. Posted as
[the rebuilt-raised comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5269554700).

**They are not three problems.** Rev 9 built height as **N translated copies of one stroke**, and
each symptom falls out of that single choice: copies offset in one direction spill over the
neighbouring slice (the climbing) · a stroked arc has no outline, so copies drift outside the
channel (the cutting) · N discrete strokes each keep their own edges (the cards). **Fewer or
smaller steps would have fixed none of them**, which is why the repair is structural:

1. **Each slice is a closed annular sector**, not a stroked arc — a stroke has no outline, and
   without an outline there is no body to extrude and no face to light. Everything else depends
   on this one change.
2. **The side wall is one filled path**, holding the face and its offset base as two subpaths
   under a single fill. One fill → one silhouette → **no banding**. The cure for the pack of
   cards is not fewer cards but **no cards**.
3. **The block is narrower than the channel and the group is clipped to the channel annulus**, so
   a body **cannot** cross a wall — not "is tuned not to".
4. **Two passes — every wall, then every face** — which is what stops a neighbour's wall landing
   on a face; and the inter-slice gap widens from 2.4° to 4.2° when raised, because a solid needs
   more clearance than a painted band.

Same rebuild applied to the goal rings: one base, one face, clipped — no stack anywhere. Verified
by counting: **zero** stacking loops remain in the file, checked rather than assumed.

**What this leaves as a genuine preference rather than a bug:** the extrusion vector. If the
height reads too shallow or too tall it is now a **single number**, and that is worth saying to
him explicitly so the next round is a one-line change instead of another rebuild.

## Revision 11 — a silhouette is not a face, and that is why the blocks still read flat

Ido, 2026-08-12: the blocks still do not read as one 3D body — *as if they have no envelope, no
side faces*. Posted as
[the envelope comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5269675482).

**He is right, and rev 10's own fix is what left it flat.** Rev 10 replaced the card stack with a
**union silhouette** behind the top face — which cured the banding but produced no side. **A
silhouette has no fold, no tone of its own and no end**, so it reads as a shadow *behind* the
block rather than as the *side of* it. A solid does not have a silhouette; it has **faces**.

Each block now carries four, drawn explicitly: an **outer wall** with its own gradient · an
**inner wall**, darker because it faces away · and an **end cap at each end of the slice**. The
caps carry most of the effect: **they are what make a block read as its own slab** rather than as
a segment of one long painted ring, and their absence was most of what he was seeing.

Two supporting changes, both structural rather than decorative:

- **A fold hairline** where each wall meets the top face. Without a fold the two merge into one
  gradient and the body flattens again — the same reason the groove needed an *edge* and not
  just blur.
- **The clip is widened by the extrusion.** A bar rising out of a groove genuinely extends past
  the groove's mouth, so clipping it to the channel exactly is what would make a raised block
  look **flush**. The *face* stays narrower than the channel, so this does **not** re-open the
  earlier "blocks cut the walls" defect. The extrusion also grew to `3.6 × 5.0`, because a wall
  two pixels tall is not a wall.

**Same treatment on the goal rings**, which were round-capped strokes and therefore could not
have an end cap at all. In raised mode they are now solid bars with slab ends, the same four
faces and the same fold, so a ring and the donut on one screen are made of the same material.

**What is left is tuning, and saying so is the point:** wall height is one vector, wall shade is
four constants. Neither is structural any more, which is the difference between this round and
the last two.

## Revision 12 — a screenshot loop, because reading the code was never going to catch this

Ido, 2026-08-12: still cutting the channel, still not a solid, *"maybe partly because the white
frame is only on the top face — check it yourself"* — and, decisively: **take the screenshot
myself after every change, judge it against his requirement without him in the loop, and keep
looping**, reporting result / remaining defects / planned fix at the end of each round.

**The instrument came first, and it is the finding.** Headless Edge renders the prototype and a
probe page that draws only the donut, so each round ends with an image that gets *looked at*
rather than reasoned about. Eight of the nine defects below were **invisible in the source** and
obvious in the render — including two nobody had reported.

| round | what the render showed | fix |
|---|---|---|
| 1 | blocks crossed the groove walls | extrusion budgeted **inside** the channel width, body centred, strict clip |
| 2 | walls too dark to register; blocks thin | margin is `H`, not `2H`; walls lightened |
| 3 | the white rim was a **frame** — Ido's own suspicion, confirmed | rim replaced by a **bevel wash**: light where the face turns to the light, shadow opposite, no outline |
| 4 | no change — the contact shadow was hidden **behind its own caster** | — |
| 5 | shadow now visible | shadow drawn **larger than the block**; block narrowed so channel shows |
| 6 | wall present but shallow | taller extrusion, **fold line** at face↔wall, **rim light** on the wall silhouette |
| 7 | rim light became a frame on small slices | rim drawn **only where a wall actually faces the viewer** (`normal · light > 0.12`) |
| 8 | *(full card)* left labels **collided** | one line per label instead of two |
| 9–10 | *(full card)* `Relationships` **ran off the card** | label clamped to real available width; leaders shortened; ring sized down |

**One light for the whole chart** also landed in round 1 and is the least visible, most important
of them: the face gradients were `objectBoundingBox`, i.e. **relative to each slice's own box**,
so every block was lit from a different direction and the ring could not read as one scene.

**What is still true and is not a defect:** on the lit side the blocks present only their face,
because there is one light and one view direction — that is what an extruded ring does. And
`Relationships` truncates at 310 dp, which is a width fact, not a rendering one.

**Verified after the final round:** `node --check` OK · Hebrew guard 0 unguarded · no
`feSpecularLighting` · all five materials re-rendered together with no regression.

## ⚠️ Push disclosure — four foreign commits rode up with rev 2, and the adjudication happened *after* the push

**What happened.** The rev 2 push (`8b6c36a`) carried four commits this session did not write:

| Commit | Session | Status on the board at push time |
|---|---|---|
| `705619f` | `c1-points-and-time` | **released** — [#19 · `C1`](https://github.com/idomarhaim/Android_Final_Project/issues/19) resolved and closed |
| `5b5e113` · `d9616b9` · `d805616` | `picker-rule-consolidation` (cross-repo visitor from JARVIS) | **released** — claimed and released the same day |

**The outcome is fine and the process was not.** Both sessions had released, the working tree
was clean, and precondition 5 permits a released-and-quiet session's commits to ride along — so
the content was legitimately publishable. But `git fetch`, `git log @{u}..HEAD`,
`git diff --stat` and `git push` were **chained in one shell command**, so the range printed and
the push landed in the same breath: there was no moment in which the adjudication could have
stopped anything. **Had `c1-points-and-time` still been live, this session would have published
over a live sibling and only noticed afterwards.** The ordering, not the outcome, is the defect.

**Not undone, and deliberately so.** Un-publishing needs a force-push, which is a destructive
shortcut and always-ask in both modes. It is Ido's call, and there is nothing here that wants
undoing on its merits.

**The fix for the rest of this session, applied immediately:** the range is read in one command
and the push issued in a **separate** one, so the adjudication sits between them where the rule
puts it.

**Third disclosure — the rev 8 push.** One foreign commit in the range, `c8b0ce3` from
`c8-ai-task-plans` (which resolved [#24 · `C8`](https://github.com/idomarhaim/Android_Final_Project/issues/24)).
Adjudicated before pushing, not after: the board shows that session **released**, and the working
tree was clean, so precondition 5 lets it ride. Its commit also **deletes**
`CHANGELOG/2026-08-10/c8-ai-task-plans.md` — moved to `CHANGELOG/2026-08-12/`, since the session
finished on the 12th — and a deletion inside a *foreign* commit is precondition 5's to judge
rather than precondition 2's, exactly as the rule splits them. Named here because a deletion
riding up in someone else's commit is the kind of thing worth being findable in a month.

**Second disclosure — the rev 4 push, and this time the fix did its job.** The range held two
foreign commits, `87c5acf` and `e65d48e`, both from `picker-delegation-clause` (a cross-repo
visitor from JARVIS that drained `c1`'s always-ask candidate). Because the read and the push were
separate commands there was a moment in which to adjudicate, and it was used: the board was
re-read, that session sits in **Recently released** with a `2026-08-11` release date and a full
*Landed in* column, and the working tree was clean. **Released and quiet, so precondition 5 lets
them ride** — named here rather than only noticed. Also noted while reading the board: two new
live rows have appeared, `c6-log-progress` on `#22` and `c8-ai-task-plans` on `#24`, so this
session is no longer the only one on the map.

**Also noticed while adjudicating, and it belongs to no ticket of this session's:** `#19` closed,
which unblocks `#20`, `#22` and `#24` — and through `#24`, `#30` and `#35`. The map's whole
blocked half has opened. Reported, not acted on.

## Revision 13 — resolved: ship all of them, and the interesting half is *why a picker is right here and wrong for the charts*

Ido, 2026-08-12: **ship all the materials as a user-selectable skin** — glassmorphism · liquid
glass · neo · dark neo — **delete metal**, **keep the raised-3D and empty-channel toggles**. Posted
as [the resolution comment](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5270370393)
and `#31` is **closed**.

**The finding is that this session refused a picker at revision 2 and accepted one now, and the two
are consistent for a reason worth keeping.** Rev 2 killed the **chart picker** because the wish
behind one is almost always *"this card is useless to me"* — a design failure to fix for everyone.
Cost was never the discriminator, and neither is it here. What separates the two is **whether a
wrong answer is a defect**: a layout a user dislikes is *evidence the layout is wrong*, and storing
their workaround preserves the defect while hiding it; a material a user dislikes is *evidence of
nothing*, because no measurement settles *frosted glass or soft shadows*. **A preference store
belongs exactly where there is nothing to be right about.** Stated as a rule: a picker over taste
is a feature; a picker over layout is a bug with a settings screen in front of it.

**The decision walks into a name collision the ticket never mentioned, and it was found by reading
the app rather than the prototype.** `AppSkin` **already ships** — `domain/model/AppSkin.kt`,
`AURORA`/`BLOSSOM`, persisted device-local by `AppPreferencesRepository`, chosen in
`ui/components/SkinPicker.kt` — and `C15` already pinned the language setting *"per-device beside
the skin"*. That skin is a **palette**; the thing decided here is a **surface**. Shipping this as
"a skin" without saying which is a collision waiting in the build session.

**And the two axes are not independent, which is the part a 2 × 4 grid would have got wrong.** The
material already reaches into the palette: `neo` **mutes every category hue 30 %** or the shadows
fight the fill, and `dark neo` **replaces the six categorical hues with a ramp of its one accent**.
So the model is not a product of two axes but **a palette and a transform** — each material declares
one (*identity · mute · single-accent ramp*) and the eight schemes are **generated, not
hand-authored**. Two consequences that would otherwise surface late:

- **Dark neo's accent must derive from the selected `AppSkin`.** The prototype pins it at
  `#5FD8FF → #1170E4`, which is a prototype simplification; left alone, **Blossom + dark neo renders
  Aurora** and the skin picker silently stops working for a quarter of the material set. Same shape
  as rev 7's finding that one accent leaves no room for six category hues — aimed at the skin axis
  this time instead of the category axis.
- **The product is ragged, not rectangular.** Dark neo has **no light scheme**, so *material ×
  brightness* has a hole in it: a material must be able to declare itself brightness-locked, and the
  picker must **say so** rather than letting the light switch quietly do nothing.

**Metal was deleted outright rather than hidden behind a flag**, and the reason it was the one
dropped is on the record: it is the only material that reads well against **no background art at
all** — so it never shared the canvas the other three were harmonised onto — and the only one whose
*light* scheme is hard rather than merely different. A candidate nobody can pick is dead code that
still has to be carried through every later revision, and this file has now had thirteen.

**"Keep the toggles" means two different things, and collapsing them would have been wrong.**
*Empty channel* is a **rendering test, not a metric** — it cannot become a user setting without the
app asserting a weekly-hours target, which is `C1`/`C3`'s question. *Raised 3D* is a **property of
the two soft-UI materials**, a no-op on glass and liquid, so a global switch would be one control
carrying two axes — the rule `C9b` bought. Keeping the toggle keeps the **instrument**; whether neo
and dark neo ship raised or flat is one look away and is filed as open, not decided here.

## Revision 13 · two Hebrew-only defects, found by rendering rather than reported — and a bug in the instrument itself

The rev 12 screenshot loop was re-run after the deletion, and it paid twice more. Both defects are
**invisible in English at the same geometry**, which is the standing rule (*a design is not finished
until it has been seen in Hebrew*) earning its place for the second time on this ticket:

1. **The donut's centre caption overruns its hole and collides with the left-hand labels.** `of a
   26h week` is short; `מתוך שבוע של 26 ש׳` is not, and at the phone's real `donut(310, 50, 18, …)`
   it runs out of the ring onto `ללא שיוך 3%`. The fix is a **budget**, not a shorter string — the
   caption needs measuring against the hole the way `clampLabel` already measures a label against
   the card.
2. **The slice percentage reorders under bidi.** The label is `<name> <pct>%` and the `%` run is
   neutral, so an RTL paragraph puts the number at the **visual start**: `27% לימודים` where English
   renders `Studies 27%`, which reads as the percentage belonging to the label above. This is
   `C9b`'s `09:00–12:00` finding in a new place — **any latin or numeric run inside a Hebrew string
   owes direction isolation** — and the `<tspan>` inside SVG `<text>` cannot inherit the `.ltr`
   class the HTML uses. **It carries straight into Compose**, where the string is assembled the same
   way, so it is a spec line and not only a prototype fix.

**And the instrument was broken in exactly the way that hides this class of defect.** `shoot.ps1
-Probe` copies the page before appending the probe, and it read that copy with `Get-Content -Raw`
and **no `-Encoding`** — so a BOM-less UTF-8 file was read in the machine's ANSI codepage and
written back as UTF-8, **double-encoding every non-ASCII character**. Every close-up probe render
showed mojibake where Hebrew should be: **the one tool built for judging a design closely could not
display the language the design standard requires it to be judged in.** It survived because
whole-page renders are never affected — only the path that rewrites the file. One word to fix
(`-Encoding UTF8`), verified by re-rendering the same probe, and both defects above were then legible.

## 🧪 Tests — revision 13

Same standing: **no test layer is owed** (no ticket on this map ships code; nothing here touched
`app/`, `functions/` or `firestore-tests/`, so no server, client, endpoint, database or UI layer was
exercised). The mechanical assertions were re-run against the artifact **after** the deletion rather
than assumed to survive it:

- **JS parses.** `<script>` extracted, `node --check` → **OK**.
- **Hebrew guard: 32 Hebrew-bearing string literals, 0 unguarded** — every one behind `t(en, he)`, a
  `he:`/`meHe:` key, or an `L === 'he'` branch. **One Hebrew string sits outside the phone frame**,
  the prototype's own `עברית` toggle, which is chrome rather than app UI; named rather than folded
  into the count. *(The README's earlier figure of 14 was a different enumeration from an earlier
  revision; this one counts every Hebrew-bearing quoted literal in the file.)*
- **No `metal` identifier survives** anywhere in the file except the changelog comment recording the
  deletion — counted, not assumed. The dead-reference check that caught `slopeChart` at rev 2, reused.
- **Rendered and looked at, twice**: `compare` in dark (four cards, no regression from the removed
  branch) and `neo` raised on its native canvas **in Hebrew** (which is where the two defects above
  came from), plus a probe close-up before and after the encoding fix.

**What this still cannot prove** is unchanged and worth repeating rather than quietly dropping: not
that a Compose chart lays out, not that it animates, not that a Glance tile fits its real cell.

## Files & artifacts — revision 13

- `docs/prototypes/2026-08-11-visual-styles/index.html` — metal deleted (CSS block, both chart
  branches, the button, the compare list, the keyboard cycle, `NAMES`/`BLURB`, and the three
  `style==='metal'` ternaries in `screen()`), section numbering re-flowed, rev 13 note in the header.
- `docs/prototypes/2026-08-11-visual-styles/README.md` — reframed from *five candidates* to *the four
  that ship*, with the deletion's cost stated and the verification block rewritten.
- `docs/prototypes/tools/shoot.ps1` + `README.md` — the `-Probe` encoding bug, fixed and written up.
- `TODO/TODO_OPTIONAL/Presentation.TODO.optional.md` *(new)* + `TODO/TODO.md` — six open prototype
  refinements and five items of build cost, so the remainder survives the ticket closing.

## Status

**Resolved.** [`#31`](https://github.com/idomarhaim/Android_Final_Project/issues/31) is **closed**;
its index line is written into `#12`'s *Decisions so far* (19 → 20 decisions) and the material set
is now one of `#12`'s **Standing preferences**, which makes it binding on
[`C6` #22](https://github.com/idomarhaim/Android_Final_Project/issues/22) and every later screen —
the consequence flagged for Ido at rev 5 rather than discovered now.
[`#10`](https://github.com/idomarhaim/Android_Final_Project/issues/10) is **unblocked** and was told
so in a comment, including the part that is new since it was filed: four shipped materials multiply
the tile work, and that decision is `#10`'s to take, not this ticket's. **No issue was filed**, and
no other session's ticket, row or file was edited.

**Both `#12` edits went through the commons discipline the board requires**, and it is worth
recording that the guard fired zero times rather than that it was skipped: the body was fetched,
built against, **re-fetched and `cmp`'d byte-for-byte immediately before the write** (unchanged, no
race), the patch **proved a pure insertion before sending** (155 → 178 lines, **0 deletions**, every
original line still present in order), and the result was **read back and diffed** — identical but
for the single trailing blank line GitHub appends, exactly as `c9e` and `c8` each recorded.

## ⚠️ Board disclosures — revision 13

Three, and the first is a deviation this session committed rather than one it found.

**1 · This resumption wrote before it claimed, and the claim was never written.** `#31` was
**assigned by Ido in this session's opening message**, and the board's own row for
`c12-charts-presentation` had been moved to *Recently released* by commit `8e2ba29` — timestamped
**20:37:53**, roughly one minute before this turn's first read. So the resumption began against a
board that showed **no active row for this work**, and the rule is unambiguous: claim before your
first write. It did not. **Nothing collided** — the one live sibling, `c6-log-progress`, owns
`CHANGELOG/2026-08-11/`, `docs/prototypes/2026-08-11-log-progress/` and `#22`, all disjoint from
every path touched here — but the absence of a collision is luck's work, not the rule's, and it is
recorded as a deviation rather than dissolved by its own good outcome.

**2 · The "handover hazard" that release note flagged was this session's own hand.** `8e2ba29`
warned that `docs/prototypes/2026-08-11-visual-styles/index.html` had **uncommitted edits removing
Metal by someone holding no row on this board**, and left them deliberately. Those edits were this
resumption's, seen mid-flight by a turn that was releasing at the same moment. The hazard is
therefore **closed rather than inherited**: the file is now edited to completion, verified, and
committed by the session that owns the ticket. Worth keeping as a small general result — **a
board row released while its own work is still in the tree reads to the next reader as an intruder**,
and the reader was right to flag it.

**3 · `SESSIONS.md` is deliberately NOT in this commit, and the board is therefore stale on
purpose.** The working tree holds **uncommitted edits that are not this session's**:
`SESSIONS.md` (+24 lines) and `kb-candidates/2026-08-09-c9f-consent-screen-state.md` (+29), both
from **`candidate-queue-audit`**, a cross-repo visitor from `C:\Dev\JARVIS` whose own note says it
has released and is **awaiting Ido's word** on deleting a fully-drained candidate file. Staging
`SESSIONS.md` would carry that session's finished-but-unpublished work into this commit, which the
staging rule forbids outright — and explicit-path staging cannot separate two sessions inside **one
file**. So this commit stages **eight explicit paths and neither of theirs**, and the board's
`c12-charts-presentation` row still reads *"`#31` stays OPEN"*, which is now false. **That is an
owed edit, not a forgotten one**, and it is named here and in the reply because the next session
reads the board before it reads this file.

**One decision derived rather than asked** (per `rules/derivable-decision.md`): this session's
changelog stays at `CHANGELOG/2026-08-10/` although it is being written on the **12th**. `c8`
corrected the opposite mistake by `git mv`, but its file was *only* mis-filed; this one has been
correctly filed since the 10th, when the session genuinely started, and is now cited by path from
`SESSIONS.md`, this repo's docs and several `#31` comments. Relocating it would break live
references for a cosmetic gain, and relocating a file is always-ask territory in any case. The
session's `kb-candidates/` file **was** renamed to `2026-08-12-…` earlier in its life and keeps that
name; the two are not inconsistent — the candidate file is drained by date, the changelog is
identified by the session.
