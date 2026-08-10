# c12-charts-presentation — claimed #31, the first screen ticket under the new design standard

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

## Status

**Revision 4 is out; the ticket is not resolved.** `#31` is HITL by type — arrangement is
visual, so it resolves against something Ido reacts to, not against an argument. The three
questions the prototype could not settle have been put to him once and handed back, so they are
answered on the record and remain overturnable; what is still owed is his reaction to the
screens. Nothing was filed, no other session's ticket, row or file was edited, and `#12`'s index
line is deliberately **not** written yet: it is written on resolution, after a re-fetch.
