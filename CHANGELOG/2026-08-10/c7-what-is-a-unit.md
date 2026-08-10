# `c7-what-is-a-unit` — the question was "which units", the answer was "a measure is optional"

**Session:** `c7-what-is-a-unit` · **Invocation:** `/wayfinder 12 14` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL throughout) ·
2026-08-10.

One ticket resolved, which is the skill's limit. **No code was touched** — this map
ships no code, and that held: every file under `app/` was read and none was edited.

## What changed

| | |
|---|---|
| Resolved | [#14 · `C7` What is a unit?](https://github.com/idomarhaim/Android_Final_Project/issues/14) — closed, with the full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*; the **Firestore-migration fog narrowed** from five dependent tickets to four |
| Tickets created | **none** — the first resolution on this map where every hand-off landed on a ticket that already existed |
| Hand-offs commented | [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) (unblocked) · [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23) `C14` (block discharged + one new clause) · [#37](https://github.com/idomarhaim/Android_Final_Project/issues/37) `C16` · [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) `C17` |
| Unblocked | [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) only. **Neither** [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23) nor [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) became takeable — both are still blocked by [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) (`C3`) |
| Frontier now | [#37](https://github.com/idomarhaim/Android_Final_Project/issues/37) `C16` · [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) `C17` · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` · [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) `C13` · [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29) `C10` *(claimed by a live sibling)* |

## The decision

The ticket asked for "the enumerated set of units a goal can be measured in". Half of
that framing survived.

**A measure is two fields, not one** — a closed **kind** that owns all arithmetic, and a
free **word** that owns everything the user reads:

| Kind | Stored as | Displayed as | Words |
|---|---|---|---|
| `COUNT` | whole units | `12 books` | books, workouts, chapters, pushups, steps |
| `DURATION` | hours | `45 min`, `2.5 h` | hours, minutes |
| `DISTANCE` | km | `4 km` | km |
| `VOLUME` | litres | `250 ml`, `1 L` | litres |
| `MASS` | kg | `0.5 kg` | kg |
| `MONEY` | major unit | `₪2,000` | ₪ |
| `PERCENT` | 0–100 | `40 %` | % |

**And a goal may carry no measure at all — that is the default.** Absence of the field,
not a `NONE` entry in the list. This is the part the ticket's own premise assumed away,
and it did not come from the ticket: it came from `E6` in a brief written the day after
this ticket was charted, and it is why `"%"` had to stop being the default. **The disease
was never the percent sign** — it was that the lazy path produced a goal that measured
nothing while claiming to measure something. Absence claims nothing.

Everything else follows: storage is canonical per kind and display is *formatting*, not
conversion (`0.25` volume prints as `250 ml`), so there is no conversion table and no
stored number that later needs rewriting.

## The principle the session found twice

Two unrelated sub-questions — *what numbers go on the fill buttons* and *how do you
reinterpret logged history when a measure changes* — produced the same split, and it is
the reusable part of this ticket:

> **The model gets the categorical half. The code gets the arithmetic half.**

- **Buttons.** Ido asked for buttons to appear automatically where they fit and assumed
  only an AI could judge that. He was half right: the model answers *"do buttons fit, and
  what is being counted"* — categorical, and `C11a` measured it at **50/50** on
  prompt-declared enums. The **ladder** is computed: `target / 16`, rounded, at
  `1× 2× 3× 4×`. Had the model authored the numbers, `C11a`'s measured **2× run-to-run
  swing** would give the same goal `250 / 500 / 750 / 1 L` today and `0.5 / 1 / 2 L`
  tomorrow, for no visible reason.
- **Changing a measure.** Percent → litres against a known target is **division**, and a
  model has no business being asked. `"12 books"` → `"pages"` has **no** arithmetic
  relationship, and that is exactly where a proposal ("≈ 25 pages per book") earns its
  place.

## What Ido added that the questions did not ask for

Three of the four answers came back with a requirement attached, and all three are now
spec:

1. **Unmeasured is legal but never silent.** *If the user may choose an unmeasured goal,
   the agent must at least recommend defining something measurable — and recommend how,
   in terms of the goal he actually chose.* His stated reason is a product position:
   goals in life usually need to be measurable in some form. So the proposal must be
   **concrete** (never *"consider adding a metric"*), it never auto-applies, it is
   dismissible per goal, and it has a non-AI fallback. **This session's addition to
   that:** the proposal may offer a **leading indicator** — measure the recurring
   behaviour that produces the outcome — rather than fake an outcome number for a goal
   like *"understand real estate"*. `E6` reaches for exactly that on its own without
   naming it.
2. **Changing a measure gets a second option beside reset** — an *adaptation* of the
   logged history, *"not necessarily 100% right, but some sufficient adaptation"*, shown
   before it applies, with the user choosing between it and a clean reset.
3. **On a shared challenge, the other participant must approve too.** Which surfaced
   something nobody had listed: a challenge's measure is the unit **every participant's
   score is expressed in**, and there is nowhere for a pending multi-party approval to
   live — `firestore.rules` lets only the owner write the challenge document and each
   participant write only their own row. Handed to `C14`.

## A hole named on the way past

**Every measure in the app assumes accumulation toward a target.** *"Lose 5 kg"*,
*"spend under ₪2,000 a month"*, *"cut screen time to 1 h"* all count **down**, and none
is expressible: `progressFraction = currentValue / targetValue`, clamped to `0..1`. So a
measure carries a **direction** (`up-to` / `down-to`) — `C7`'s to state, `C3`'s to
compute, arriving beside the clamp removal `C4` already sent there for `E11`'s decay.
Both are the same defect seen twice: the arithmetic only knows how to go up and stop.

## What this ticket deliberately did not decide

- **The period.** `E18`'s *"run 4 km a week"* is two decisions: `4 km` is settled here,
  `a week` is [`C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21).
- **One task, two goals, two kinds.** `Task.progressContribution` is one `Double` and
  cannot be right in two kinds at once — `1.0` means *4 km* to one parent and *one
  activity* to the other, with no scaling factor reconciling them. Named and handed to
  [`C17` #38](https://github.com/idomarhaim/Android_Final_Project/issues/38) on Ido's
  explicit instruction, rather than specced around.
- **Whether a milestone shows a measure** — [`C16` #37](https://github.com/idomarhaim/Android_Final_Project/issues/37)'s.
  One observation offered: under `C4`'s roles-not-types finding the *modelling* half
  dissolves, since a measure sits on the object and an object neither gains nor loses one
  by acquiring an edge.
- **The arithmetic** — [`C3` #18](https://github.com/idomarhaim/Android_Final_Project/issues/18)'s.

## The live-data consequence is that there isn't one

`"%"` → `PERCENT`, `"steps"` → `COUNT`, `"hours"` → `DURATION` — every legacy string maps
losslessly, and re-measuring a goal is a per-goal, user-approved conversion rather than a
bulk rewrite. The map's Firestore-migration fog loses this ticket as a dependency.

## The session was overtaken twice, and both times the newer source won

- **`C4` landed mid-grilling.** Its roles-not-types finding reframed the challenge
  question from *"is a challenge like a goal or like a milestone"* to *"does a challenge
  measure like any object"* — and the answer stopped depending on the goal/milestone
  distinction at all. The picker was re-popped with the deltas marked rather than
  answered against a stale map.
- **The 08-09 entity brief invalidated the ticket's premise.** `E6` made *unmeasured*
  a legal state after this ticket had been charted around the assumption that every goal
  needs a unit. Ido flagged it explicitly before the resolution was written.

## 🧪 Tests

**No suite was run, and none is applicable.** No Kotlin, Gradle, `firestore.rules` or
Cloud Functions file was created or modified — the entire output is GitHub issue text,
this changelog and a KB-candidate file. The layers that exist in this project (JVM unit,
instrumented, `firestore-tests/`) all test code that was not touched.

**Verification was structural**, in the shape the previous map sessions used:

- The map body was **fetched and hashed before the first edit and re-hashed immediately
  before writing** (`b9d5c8ee…`, unchanged both times), because `#12` carries no lease and
  a sibling session was live throughout.
- The graph was **queried back out of GitHub after closing**: `#14` `CLOSED` and still
  assigned; the two tickets it was blocking (`#23`, `#31`) re-read and confirmed **still
  blocked** by `#18` — so the honest claim is that this resolution unblocked `#11` and
  nothing else.
- All four hand-off comments were **verified as landed** — the first attempt posted
  nothing and reported no error, and the comment counts were `0/0/0/0` until it was re-run.

## Singletons and live state

**None taken.** No `#gradle-daemon`, neither AVD, no GROQ call, and live
`goalpilot-56e30` was never contacted — the Firestore claims in the resolution come from
reading `domain/model/` and `firestore.rules`, not from querying the project.

**Recorded rather than papered over:** this session's board row was written before its
first write, but was **committed by the concurrent `c4-goal-task-ontology` session**
(`ca35c4c`) rather than by one of its own — the same commons-lease hazard `SESSIONS.md`
already records twice. The row was also **widened mid-session**, before the first write
to any of them, from `#14 + #12` to include the four hand-off comment targets; rule 1
allows a session to correct its own row, and writing to `#23`/`#37`/`#38` without
declaring them would have been exactly what `c11a-free-model-probe` refused to do.
