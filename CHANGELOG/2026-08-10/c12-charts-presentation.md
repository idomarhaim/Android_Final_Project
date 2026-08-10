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

## 🧪 Tests

**Still none owed — no ticket on this map ships code — but two mechanical assertions were run
against the artifact rather than eyeballed:**

- **JS parses.** The `<script>` block was extracted and passed through `node --check` → **OK**.
  A prototype that silently fails to render is worse than no prototype.
- **No Hebrew literal can reach the English render** — `C9b`'s finding 3, asserted absolutely
  rather than sampled. Every single-quoted literal containing a character in the Hebrew Unicode
  block was enumerated: **41 found, 41 language-guarded** (behind `t(en, he)`, a `he:` /`meHe:`
  key, or an `L === 'he'` conditional), **0 unguarded**, and **0 lines** carry Hebrew outside a
  string literal.

Layer coverage is otherwise unchanged and unowed: nothing in this unit touched `app/`,
`functions/` or `firestore-tests/`, so no server, client, endpoint, database or UI layer was
exercised. **The cost is stated in the README rather than hidden: this cannot prove a Compose
chart lays out, animates, or survives a real `LazyColumn`** — that proof belongs to the build
session, and `ChartAnimation.kt` exists because `animateFloatAsState` initialises *at* its
target.

## Status

**Revision 1 is out; the ticket is not resolved.** `#31` is HITL by type — arrangement is
visual, so it resolves against something Ido reacts to, not against an argument. Awaiting his
reaction. Nothing was filed, no other session's ticket, row or file was edited, and `#12`'s
index line is deliberately **not** written yet: it is written on resolution, after a re-fetch.
