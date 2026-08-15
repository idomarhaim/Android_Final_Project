# GoalPilot — product spec v0.3

**Status:** the destination artifact of wayfinder map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12), written from its **27 closed
decisions** on 2026-08-15. It exists so a build session can implement v0.3 **without reopening a
decision**.

**How to read it.** Every section header carries the ticket that decided it. The prose here is a
*restatement for building*, not a second opinion — where this file and a resolution comment
disagree, **the resolution comment wins** and this file is wrong and should be fixed. Nothing here
is new product design; the two places where a decision is genuinely *missing* are marked
**⚠️ GAP** and listed together in [§10](#10--gaps-defects-and-open-work).

**What this file is not.** It is not a build plan, a task breakdown, or an estimate. It says what
v0.3 *is*; sequencing is the build session's.

> ### ⚠️ Two live caveats on this document's own completeness
>
> 1. **The map is not closed.** [`C21` #43](https://github.com/idomarhaim/Android_Final_Project/issues/43)
>    — *does v0.3 owe an offline story* — was **opened on 2026-08-15**, graduated from the fog
>    patch in [§9](#9--not-yet-specified-fog-on-12). It is being resolved by a live session as this
>    file is written. So `#12`'s own rule — *"the map is done when the spec is whole and **no ticket
>    is open**"* — is **not satisfied yet**, and this spec is one decision short by construction.
>    When `#43` closes, [§5.3](#53-offline-c20-and-c21-43--open) is the section that needs
>    amending.
> 2. **This file was written against `#12` as read at 14:0x on 2026-08-15**, at 27 decisions. It has
>    not been reconciled against decision 28.

---

## Contents

| § | Section |
|---|---|
| [0](#0--the-rules-that-bind-everything) | The rules that bind everything |
| [1](#1--the-model) | The model — objectives, edges, measure, effort, outcome |
| [2](#2--scheduling-and-the-calendar) | Scheduling and the calendar |
| [3](#3--ai) | AI — five features, four schemas, one failure contract |
| [4](#4--screens-and-presentation) | Screens and presentation |
| [5](#5--cross-cutting-behaviour) | Cross-cutting behaviour — localization, derived state, offline |
| [6](#6--challenges-c14-23) | Challenges |
| [7](#7--data-shape-and-migration) | Data shape and migration |
| [8](#8--out-of-scope) | Out of scope |
| [9](#9--not-yet-specified-fog-on-12) | Not yet specified (fog on `#12`) |
| [10](#10--gaps-defects-and-open-work) | Gaps, defects and open work |
| [11](#11--traceability) | Traceability — decision → section |

---

## 0 · The rules that bind everything

These are not preferences. Each was won **against a first answer** — the obvious answer was tried,
shipped in a proposal, and then killed by evidence. A build session that violates one is not making
a style choice; it is re-introducing a defect the map already removed.

### 0.1 The free-model rule *(`#12` Notes, scope fixed by Ido 2026-08-07/08)*

> **An AI feature that cannot run reliably on the free tier is specced with a non-AI fallback beside
> it, or it is not specced.**

Nobody may ever have to pay, or supply an API key, to use GoalPilot. Bring-your-own-key
([§3.6](#36-bring-your-own-key-c13-32)) is a *bonus*; nothing may be specced that **requires** it. The
30-RPM ceiling on the free tier is therefore a **hard budget**, not a soft one — it is why every AI
feature here is **one wide call**, never three narrow ones ([§3.2](#32-one-wide-call-per-feature-c11b-30)).

Every AI feature in [§3](#3--ai) names its non-AI fallback. If you add a sixth, it names one too.

### 0.2 The derive-don't-store rule *(`C5`, generalised — `kb/dev/enum-and-label.md` §5)*

> **A stored judgement that is derivable from per-item facts is a defect.**

It killed a `GoalKind` enum in `C5`, killed a dormancy state in `C19`, and is the reason
[§5.2](#52-who-owns-a-derived-number-c20-42) has *zero client writers of derived state*. Its converse
is also load-bearing: **a stored field can lie** — a user edit, a migration default — and the facts
in front of you cannot. That is why `C15b` derives a plan's language from its **script** rather than
from a stamp.

### 0.3 The map's most-repeated finding — *a second number that quietly disagrees*

Found and killed **five times**, each time wearing a different hat:

| # | Site | The silent second number |
|---|---|---|
| 1 | `C7` | `Goal.unit = "%"` — a goal that measured nothing while claiming to measure something |
| 2 | `C3` | `Task.progressContribution = 1.0` — never a value, always a silence |
| 3 | `C18` | *residual* work on a parent task — read backwards by every natural input |
| 4 | `C1` | `R8`'s duration box storing a number disagreeing with the one on screen |
| 5 | `C11b` | `TaskScoring.looksLikeFallback` — reconstructing provenance by recomputing the fallback, which its own KDoc concedes is *"evidence, not proof"* |

**The house rule that falls out:** when a value is unknown, it is **absent**. Never a default that
looks like an answer, never a substitute, never a sentinel meaning *"I tried"*.

### 0.4 Legal, but never silent *(`C7`, refined by `C13` §5)*

A state may be permitted and still owe the user a sentence. But the refinement matters:
**speak about a failure the user can act on; stay silent about one they cannot.** A model being
wrong is not actionable → silent fallback. A dead API key is → a permanent status line.

### 0.5 The AI judges, the app computes *(`C7`, reused by `C1`, `C3`, `C10`, `C2`, `C8`)*

The model is asked for **prompt-declared enums** and never for free numbers or opaque tokens. This is
not taste — `C11a`'s 248 live calls measured enums at **50/50 perfect**, free numbers swinging **2×
run-to-run** and **1.8× between languages**, and the one observed failure mode was **silent id
corruption**. Every arithmetic result is the app's.

### 0.6 Fact vs judgement decides authorship *(`C1`)*

- A **fact about Ido's life** (`minutes` spent, `granularity` of how he works, an outcome he logged)
  — **he is its authority**, his value is *sticky*, and no re-estimation may ever overwrite it,
  unconditionally and with no threshold.
- A **judgement about the work** (`difficulty`) — **only the model makes it**.
- **Nobody authors a product** (`points`). It is computed from the two above.

### 0.7 Intrinsic structure needs consent; instrumental structure does not *(`C4` §9)*

> **The app may act silently on instrumental structure, but must ask before asserting an intrinsic
> edge.**

So: the agent may file, schedule, link and break down freely. It may **never** invent a goal.

### 0.8 Every screen is designed, and is not finished until seen in Hebrew *(`#12` Standing preferences)*

Three sub-rules, each bought by a defect Ido caught:

1. **One chip may not carry two axes.** `C9b`'s unreadable rung glyphs were a *decomposition*
   failure, not an icon-choice failure.
2. **Form and words before iconography.** A symbol earns its place only where no word fits.
3. **A design is not finished until it has been seen in Hebrew.** RTL and bidi break layouts that are
   correct in English — the bidi algorithm silently renders `09:00–12:00` as `12:00–09:00`.

---

## 1 · The model

### 1.1 There is one kind of objective, and "goal" is a role carried by an edge *(`C4` #13, `C16` #37)*

The discriminator between a goal and a means is **intrinsic vs instrumental**, and it is a property
of the **edge**, not the object.

- A **goal** is what the user wants for its own sake (`E7`).
- A **milestone** and a **task** are both **means** (`E12`, `E14`).

All four object-property discriminators the map first considered — measured/done, size,
endures/completes, let-the-AI-decide — **fail in both directions on Ido's own examples**:
*"understand real estate"* is unmeasurable and a goal; *"finish year 1 of the degree"* is perfectly
measurable and explicitly **not** one.

**Goals do not nest; milestones join them** (`E16`, `E19`). The same object is a goal **and** a
milestone of another goal, and promotion either way is **adding or dropping one edge** — never a
document migration.

#### Storage — one collection, one marker

Objectives live in **`users/{uid}/goals`**. A milestone is *a goal nobody wants for itself*: same
collection, same shape.

What makes an objective a **goal** is an **intrinsic marker** on the document, and the marker
**carries provenance rather than being a boolean**:

```
declaredBy: USER | AI_SUGGESTED | UNKNOWN     // absent ⇒ purely instrumental (a milestone)
```

> `Observed:` `C16` fixes both the field's role (*"an `intrinsic` marker on the document"*) and its
> three values (*"the marker carries provenance, not a boolean"*). `Inferred:` that **absence** is
> how a pure milestone is encoded — `C16` says a milestone is *"a goal nobody wants for itself"* in
> the same collection, so something must distinguish them, and a fourth `NONE` value would be a
> stored judgement where a null already says it.

Same query cost as a boolean, and it is the only shape where [§0.7](#07-intrinsic-structure-needs-consent-instrumental-structure-does-not-c4-9) has a **witness in the
data**:

- the goals list can mark what the sorter invented, and offer a **lossless demotion** (drop the
  marker; the object and all its edges survive);
- an `AI_SUGGESTED` goal can sit **pending** rather than silently appear;
- `UNKNOWN` is the backfill for everything already in `goalpilot-56e30`, because nothing in the
  schema records who made those goals and **the migration must not pretend otherwise**.

**Two rejected storage shapes, recorded so they are not re-proposed.** *Two collections* fails both
of `E19`'s tests — a copy-repoint-delete migration whose partial failure corrupts live data, and two
documents that can silently disagree. *One `nodes` collection holding tasks too* passes both tests
and was **still rejected**: it merges two field sets that are never both valid, enforces neither,
rewrites every DTO/query/screen in a working app, and **erases in storage the line `E7`/`E12` draws
in prose**.

**The goals list filters to intrinsic only.** So *"Do a SWE degree"* renders **twice** — once as
Ido's own goal, once as a step under *"$100M"*. That is `E16` behaving as written, not a bug.

### 1.2 Edges *(`C16` §3, `C17` #38, `C18` #39)*

Three edges, and each one's cardinality was decided separately and for a stated reason.

| Edge | Field | Stored on | Cardinality | Why |
|---|---|---|---|---|
| objective → objective | `parentIds` | **the child** | **many** | A parent-side `childIds` is unbounded in one document and rewrites the parent on every add. Plural because restricting goal→goal alone would give the same *serves* edge a different cardinality depending on the types at its ends |
| task → objective | `goalEdges: [{ goalId, contribution }]` | the task | **many** | `E17`/`E18`. It is a **record, not an id**, because one `Double` cannot be right for *"4 km"* and *"one shared activity"* at once — `C7` put the measure on the object at the **far end** of the edge |
| task → parent task | one nullable `parentTaskId` | the child | **one** | The many-to-many `C17` settled is task→*objective*; a second plural edge here would sum the same work twice under one goal |
| goal → life area | `lifeAreaIds` | the goal | **many** | `E17`. *Unassigned* is the **empty collection** — a direct translation of today's `null`, preserving the rule that deleting an area must not silently rewrite the past |

**Nesting is that same edge repeated.** `E19`'s sub-milestone `1.1` is *depth*, not a concept.

**A task attaches at any level** — `E8`'s *may*, read permissively. A leaf-only rule would invent a
fake milestone for every loose task.

**Depth is capped at 10**, meaning **10 levels from an intrinsic goal down to a leaf task** — not 10
task levels on top of an objective depth. `E19`'s own most elaborate chain runs **6** deep, so 10
leaves room for four more.

- **The cap binds the user, never the agent.** An AI-proposed plan is **flattened to fit** at the
  deepest legal level rather than refused, and gives way to structure the user built.
- **A cycle defeats a depth cap**, so a parent edge reachable from the child is **rejected at the
  write site** — client-side, no extra reads.

### 1.3 The measure — optional, two fields, on the object *(`C7` #14)*

A measure is **a closed kind plus a free word**:

```
kind: COUNT | DURATION | DISTANCE | VOLUME | MASS | MONEY | PERCENT   // fixes every arithmetic
word: String                                                          // "books", "chapters", "pushups"
```

The kind is **app logic** (its seven labels are translated); the word is **user content** (it is
not). It is the only shape where *nothing is unsayable and nothing is unknowable* — a fixed unit
list is knowledgeable but mute, free text is expressive but stupid, and a dimensional model buys
conversions this app never performs.

**A goal may carry no measure at all, and absence is the default** (`E6`). `"%"` survives as a
*chosen* hand-nudged bar; it stops being what a goal gets for saying nothing.

**Unmeasured is legal but never silent.** The agent:

- proposes a **concrete** measure for that specific goal — never *"consider adding a metric"*;
- may propose a **leading indicator** (measure the recurring behaviour that produces the outcome)
  rather than fake an outcome number;
- **never auto-applies**, and the offer is **dismissible per goal**;
- has a **non-AI fallback**. ⚠️ **GAP** — see [§10.1](#101--the-measure-proposal-has-no-schema).

**Changing a kind is never silent.** Either **reset**, or a **proposed adaptation of logged history
shown before it applies** — with **arithmetic first and the model only where arithmetic cannot
answer** (percent → litres against a known target is division; *"12 books"* → pages is not). On a
shared challenge, **every participant must approve** ([§6](#6--challenges-c14-23) has the mechanism).

**Input mode**, per goal: `buttons · number · tick · auto`. Whether logging **adds or sets** rides
this — per goal, because a global rule is the granularity error.

**Fill buttons** (`R25`, [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)): the
**AI judges, the app computes**. The model answers only *do buttons fit, and what is counted*; the
ladder is **`target / 16` rounded, at `1× 2× 3× 4×`**, because free numbers swing 2×. On *"drink 4 L
a day"* that yields `[250 ml] [500 ml] [750 ml] [1 L]` exactly, and it stays right at a 40 L target.
Buttons are **repeat-tappable with a running tally**.

### 1.4 Effort and outcome are two quantities, and points are neither *(`C3` #18, `C1` #19)*

> **The gap between effort and outcome is the app's most valuable signal, not a bug to tidy away.**

- **Effort** = `minutes`. A **fact** Ido owns.
- **Outcome** = the measure's current value. What he logs.
- **Points** = a **view of effort**: `points = round(minutesOf(task) / 3) × difficulty`, with
  `difficulty ∈ LIGHT · ROUTINE · DEMANDING` at **×0.75 / ×1.0 / ×1.5**. The multipliers live in the
  **app**, never in the prompt — the model must not be able to move a currency by phrasing.

Today's anchor survives exactly: a 30-minute routine task is still worth 10 points. **The `5..50` cap
is deleted** (it priced an eight-hour task like a ninety-minute one); the levelling ceiling rises
**50 → 240** and no threshold changes.

**The inversion this fixes.** `TaskEstimate.kt:40` today asserts `minutes = points × 3`, so on every
offline task the app invents a reward number from a **word count** and then derives *how long your
life took* from it — putting the time-allocation chart downstream of a gamification currency. The fix
**inverts a constant rather than adding one**, and it retires `heuristicPoints` (`5 + 3×words`)
outright.

**A hand-typed duration is sticky.** `R8`'s box wins: **the typed number *is* the duration and points
recompute from it**, and no re-estimation ever overwrites it — which answers
[#9](https://github.com/idomarhaim/Android_Final_Project/issues/9) **unconditionally rather than with
a threshold**, because any threshold makes the app judge when Ido is wrong about his own day. `R8`'s
placeholder icon becomes **provenance stored as data**, replacing what `looksLikeFallback` tries to
reconstruct.

**Points are banked as their inputs, not as a number.** On completion, `minutes` and `difficulty` are
stamped into a **timestamped completion fact**, and the lifetime total is a **sum over facts**. So the
arithmetic never branches, no stored number can disagree, and **a level can never fall**. This closes
a live defect: `TaskRepositoryImpl.kt:120-127` keeps the total as a running accumulator reading
`task.points` *at untick time* — tick at 10, re-score to 30, untick, and the total loses 30 for a 10,
with `.coerceAtLeast(0)` silently absorbing the drift.

**Points are never rendered as a property of an objective.** `GoalProgress.points` is deleted; the
goal header's companion number becomes **effort** — *"4h 20m of work logged toward this"*. Half of
`R12` was exactly this layout fact: a goal % and `+40 pts` on one screen with no stated relationship,
published as one object by `SummaryUseCase.kt:41-42`.

**Shared work is paid once, in full, and said out loud.** No edge-count bonus — `C4` §9 lets the app
add edges silently, and a currency growing with edge count is one the app inflates on your behalf.
Instead: **recognition at the tick**, counting only edges with a **declared contribution**. Points
compute from the **whole** duration, since `C17`'s division never leaves the pie.

### 1.5 Progress arithmetic *(`C3`, `C16` §4, `C17`, `C18`)*

```
progress = (current − start) / (target − start)
```

- **`start` is the missing field, and it is an origin, not a direction.** With it, *"lose 5 kg"* is
  expressible and **no `DIRECTION` enum is needed**.
- **Progress can fall**, which today it structurally cannot.
- **Overshoot is legal and shown.** Past the target the app **stops speaking in percent** and uses
  `C7`'s word — *"beat it by 1.5 kg"* — since `130%` of a loss parses for nobody.
- **Four clamps are deleted, not three.** `Goal.progressFraction`'s `0..1`
  (`Goal.kt:32-38`), the write-site clamp at `TaskRepositoryImpl.kt:135-141`,
  `GoalDetailViewModel.kt:275`, and — the fourth, found by `C6` —
  `GoalRepositoryImpl.kt:91`, which made legal overshoot unreachable on the one screen a human
  writes to.

**An objective has exactly one progress number.** With no target it is simply *how much of the work
below is done*. Two rejected alternatives, each killed by a specific failure:

- *show both* — defers, and the deferral resurfaces at **every** ancestor;
- *children win* — splitting one task into three would drop progress 33% → 25%, **punishing you for
  planning properly**.

**When the listed work sums to less than the target, say so** — *"everything you have planned adds up
to 3 of 10"*. That is **subtraction, not inference**, so it costs nothing against
[§0.1](#01-the-free-model-rule-12-notes-scope-fixed-by-ido-2026-08-0708).

**`progressContribution`'s `1.0` was a silence, not a value.** On the edge it defaults to
**undefined**: an edge declares its contribution **in the objective's own word**, or contributes
nothing to the measure — and the shortfall is disclosed as above.

**Every roll-up sums over leaves** (`C18`). A parent task is a **container, never a second worker**:

- a parent's number is its children's sum, and it **may not disagree** with them;
- **residual work becomes a child**, so nothing is unsayable;
- the collapse from 120 → 15 when one small sub-task is added is **disclosed with one tap to close
  it** — *"your sub-tasks add up to 15 of the 120 you estimated"*;
- **ticking a parent writes its children**, so the gesture survives and the arithmetic never
  branches;
- a `MISSED` child **never holds its parent hostage**.

**A milestone may measure and so may disagree with the work below it; a parent task may only sum and
cannot.** That disagreement *is* the effort-vs-outcome gap, and it is a property of **objectives
only**.

**Many-to-many arithmetic — divide what is drawn from one pool, duplicate what each destination
owns** (`C17`):

| Quantity | Shared task under N edges | Why |
|---|---|---|
| **minutes** | **divided** | pooled — one afternoon happened once |
| **points** | **paid once** | pooled, via minutes |
| **goal progress** | **every edge advances fully**, by its own contribution | owned by each objective |
| **a success** (`E4`) | **counts in full in both areas** | owned |

**Why *divide* and not *credit both*** — and the reason is not the one the ticket gave. *Credit both*
was rejected because it is **the only option an automated agent can inflate**: `C4` §9 lets the app
add instrumental edges silently, so a silently-added area would raise that area's share with **no
work done**, making the app's central chart reward re-filing over doing. *Primary* meanwhile demands
a ranking `C4` §9 forbids the AI to make, and prints **0** for an area genuinely served. (The
ticket's headline objection — *"the total exceeds the time that actually passed"* — is **weaker than
it looks and is not the reason**: the chart counts only completed tasks and substitutes a fallback
duration for unestimated ones, so it was never an audit of elapsed life.)

Three rules on top: the chart **discloses that it divided** (*"40 of your 100 tracked minutes served
more than one life area"*); **the division never leaves the pie** (a life-area *detail* screen shows
the whole 40-minute run); and the integer remainder is distributed by **largest-remainder**, because
rounding each slice independently breaks the sum-to-total invariant that chose the option in the
first place.

### 1.6 Goal kinds are **views**, not a stored enum *(`C5` #21)*

**There is no third goal kind, and nothing decays.** `E9`'s invitation to add one is **declined** —
a `GoalKind` enum is a stored judgement derivable from per-item facts
([§0.2](#02-the-derive-dont-store-rule-c5-generalised--kbdevenum-and-labelmd-5)) and would be wrong
the moment a repeat rule is added.

Both kinds are already sayable with **zero new fields on `Goal`**:

- **Endless** = an intrinsic objective with **no measure**, whose instrumental tasks **recur**.
- **Maintenance** = the same, **plus a measure reached at least once**.

**Ido's decay proposal is overridden**, on four committed grounds rather than taste. It invents a
second vocabulary for a quantity `C9a` already derives (*55% of what?*); it contradicts `C3`; it
**breaks charts that have already shipped** (`DashboardViewModel.kt:103`'s mean and
`RecommendationRepositoryImpl.kt:175`'s `< 0.34f` filter would **drift while Ido sleeps**); and it
**manufactures failures** — *you cannot fail to do something you never agreed to*.

**So an endless goal shows two numbers, never one:**

| | What it is | Shape |
|---|---|---|
| **Outcome** | the attainment bar, `(current − start)/(target − start)` | history that **does not decay** |
| **Upkeep** | derived from the occurrence stream, **nothing stored** | **never a percentage** — `fresh · due · overdue — 7 days`, plus the window run `● ● ● ● ○ ○` |

**An endless goal has no percentage at all, and that is not degraded** — `C10`'s themes key on *days
idle, open work, age*, so it is **well-aimed, not degraded**. It must **never** be sent to the model
as `progressPercent: 0`, which reads as *"you have done nothing"*.

**An infinite or maintenance goal can fail — per window, never as a whole.** `MISSED` is a failure,
`OVERDUE` is not, `EXPIRED` counts for nothing. **Points are never clawed back** — a view of effort,
and the minutes were spent.

**One stored field is added, and it is not on `Goal`:** `pausedUntil: Long?` **on the repeat rule** —
without which the design manufactures the failure it just refused the moment life legitimately
intervenes.

### 1.7 Task typing — one axis, two values *(`C2` #20)*

**`R11`'s nine task kinds do not ship as schema.** Killed by three decisions at once: `C11a` priced
enums at 50/50 and *nine near-synonyms are worse*; `C12` retired the count-weighted chart that would
have read them; and `C1` fixed the payload with the model **never emitting a point value**, so a type
would be a fourth field arguing with `difficulty`. The nine survive as the **prompt's reasoning
vocabulary, never stored state**.

What ships:

```
granularity: DEEP | FRAGMENTED | null      // no OTHER — an escape hatch on a two-value enum makes it one
```

It is the **only** cut nothing else covers: `estimatedMinutes` says how long, `difficulty` says how
hard, and **neither says whether the work survives interruption**. A 90-minute run and a 90-minute
report are identical on both existing axes and opposite on this one; all four cells populate, so it
does not collapse into `difficulty`.

**Authorship lands opposite to `difficulty`** ([§0.6](#06-fact-vs-judgement-decides-authorship-c1)):
fragmentability is a **fact about how Ido works**, so **he** is its authority, may correct it, and a
corrected value is **sticky, unconditionally and with no threshold**. (`R7` bans authoring a
*product*; this is an *input*.)

**One consumer, and nothing computes from it:** `C9b`'s daily review answering *"I have twenty
minutes — what can I finish?"*. That is `C12`'s discriminator in its converse form — **a field may
only feed a surface where being wrong is cheap** — and every other purpose the ticket proposed fails
it.

**Non-AI fallback:** `estimatedMinutes` alone. **Cost:** rides `C1`'s existing wide call at **zero
extra requests**.

**Second axis, not a replacement.** `LifeArea` (user-authored, open, Hebrew, coloured) and
`GoalCategory` (app-authored, closed at ten, English labels hardcoded in `domain/model/`) are **two
answers to one question already on the goal**, quietly resolved by `Mappers.kt:29`. Replacement was
never available: `C15` puts user content outside what the app may rewrite, and after `C17` a task
reaches an area only **through its goals**. ⚠️ **GAP** —
see [§10.2](#102--goalcategorys-fate-was-routed-to-c5-and-c5-did-not-decide-it).

---

## 2 · Scheduling and the calendar

### 2.1 A schedule is a set of occurrences; the task carries only the rule *(`C9a` #25)*

`R17` reads as one feature and is six decisions, none of which is the one that matters most: **how
many independent *whens* one piece of work may have, and what remembers the outcome of each.**

- A date **on** `Task` gives one — so `R18`'s flowers become **26 duplicate documents a year**, and a
  miss has nowhere to live (`isDone` is a latching `Boolean`).
- A **rule alone**, computed and unstored, cannot hold a moved instance, a skip, or a Google event
  id.

**Both, and that combination is what makes *"this occurrence, or all future ones?"* askable** — a
field-only model always answers *just this one*; a rule-only model always answers *all of them*.

Occurrences are **flat, not nested**: a `SPAN` does not contain its blocks. A span that later wants a
container becomes a **milestone**, not a third mechanism.

### 2.2 Four rungs, discriminated by what a miss means *(`C9a` §2)*

| Rung | What it is | A miss means | Time column reads as *(`C9b` rev 2)* |
|---|---|---|---|
| `ALL_DAY` | a day with no slot | the day passed | the words **all-day**, no time |
| `DEADLINE` | a moment you owe something by | **late, still owed** | `due` + time, then a **single point** |
| `BLOCK` | a span of time you are inside | the slot is gone | start over end with a **filled rail** |
| `SPAN` | days, not hours | the window closed | a date range + a **soft capsule** |

**Spans contribute nothing to the time-allocation chart**, or one week-long renovation swamps every
life area. (The 480-minute ceiling governs `estimatedMinutes` — *effort* — so it never touches a
span's elapsed dates.)

### 2.3 Temporal state is derived, never stored *(`C9a` §5)*

Following `Challenge.phaseAt(now)`: no sweep, nothing deployed, nothing to go stale, no cost against
the ~$1 budget alert. The available `onSchedule` sweep was rejected because it buys only a stored
field that can disagree with the dates.

```
PROVISIONAL   agent-placed, not yet confirmed, not synced to Google — drawn dashed
SILENT        agent-placed and already confirmed, because the slot was visibly free
CONFIRMED     endorsed; may reach Google
MISSED        a block whose slot has gone            → a failure
OVERDUE       a passed deadline, late and still owed → NOT a failure; the one state that keeps reminding
EXPIRED       an unconfirmed block whose time passed → counts for nothing, silently
MOVED         dragged to a new time in Google
AWAY          it left the GoalPilot calendar — indistinguishable from a delete
EXTERNAL      you made it by hand inside the GoalPilot calendar; no task behind it
```

- **`SILENT` and `PROVISIONAL` sit on the same day on purpose** — they differ by whether the app
  could *see* the slot, **not** by how confident it is.
- **`OVERDUE` split from `MISSED` earns its keep twice**: a passed deadline is late and still owed,
  and it is the one state that **keeps reminding**, where a missed block goes silent because its slot
  is gone.
- **A missed occurrence is never edited — it is history.** Rescheduling creates a **new** occurrence
  under the rule. Auto-roll-forward was rejected for erasing exactly the record `E4` needs.
- **An unconfirmed block `EXPIRE`s silently, counting for nothing** — without which an over-eager
  agent **manufactures failures**.

### 2.4 Who schedules, and what needs asking *(`C9a` §4, `C9c` §5)*

Decided by **what the app cannot see**. `C9d` bought `calendar.app.created`, so GoalPilot is **blind
to every other calendar Ido owns**:

- `ALL_DAY`, `DEADLINE`, `SPAN` occupy no slot and **cannot collide** → the agent sets them
  **silently** ([§0.7](#07-intrinsic-structure-needs-consent-instrumental-structure-does-not-c4-9):
  a schedule is pure instrumental structure).
- A **`BLOCK` needs confirmation**, because 09:00 may already be taken — **unless** the slot is free
  on every calendar Ido has chosen to share, in which case it is placed **`SILENT`**. *The agent gets
  quieter exactly in proportion to what Ido chose to show it.*

**Confirmation is per plan, not per block** — one **batch sheet**, reusing the shipped Google Tasks
import dialog idiom rather than inventing a second. A mid-plan crash therefore loses nothing.

### 2.5 Reminders *(`C9a` §6)*

One reminder **per occurrence**, timed **per rung**. The deadline's is computed **backwards from
`minutesOf(task)`**, clamped to waking hours, and **says why it moved** — *"due at 06:00 and it takes
about 4 hours — worth starting tonight"*. That is the one thing this app knows that Google Calendar
does not.

A reminder **re-checks at fire time** whether it is still needed — free, precisely because nothing is
stored.

**Misses meet Ido once, in a daily review on app open** — never as a push saying he failed. Plus
**Ido's own addition**: a nightly *plan-tomorrow* notification.

**Substrate:** the app has **no `WorkManager`, no `AlarmManager` and no FCM** today, so every reminder
here needs local scheduling and the nightly one rides it. This widens
[#8](https://github.com/idomarhaim/Android_Final_Project/issues/8) to **scheduled**, not only
immediate, notifications.

⚠️ **GAP** — the app has **no daily-planning-hour setting and no waking-hours setting**, and §2.5
needs both. See [§10.3](#103--three-settings-this-spec-requires-that-do-not-exist).

### 2.6 Google Calendar — scope and consent *(`C9d` #17, `C9f` #33)*

**One narrow scope buys the whole thing:** `calendar.app.created` creates the calendar, writes its
events and colours it. **The calendar is Ido's, not the app's**, and it must be created
**client-side** — a service-account owner is actively wrong.

**Consent state, verified on a device 2026-08-09:** the screen is **`In production`**, so the
seven-day grant expiry is **gone**. Production shows *"Google hasn't verified this app"* with
**Advanced → Go to GoalPilot (unsafe)** on the first screen, and scoped calls work through it. The
claim this repo asserted in three files since 31/07 — *"an unverified app in production returns
`Error 403: access_denied` with no override"* — is **false**. Publishing is **reversible**. The
Google Calendar API is now **enabled** on `goalpilot-56e30`.

**Partial grants are the normal case, not an edge case:** the `View your tasks` consent checkbox
**arrives unchecked** ([#36](https://github.com/idomarhaim/Android_Final_Project/issues/36)), so
sign-in can succeed while granting nothing. **Every calendar feature therefore degrades legibly and
none gates the app.**

### 2.7 Sync — Google holds the *when*, GoalPilot holds *what happened* *(`C9c` #27)*

A Google event has a start, an end and a title, and **no field for `MISSED`/`OVERDUE`/`EXPIRED`/
`PROVISIONAL`**. So the sync carries **times in both directions and state in neither**. Every
alternative ends in an encoding — a ✓ in a title, a colour meaning *late* — that nothing else
respects and the user can destroy by typing.

- **Two-way, at no extra scope** — `calendar.app.created` already reads back what it wrote.
- **The conflict fork did not exist.** `GoogleAuthUtil` mints only short-lived tokens with no refresh
  token, and `C9d` banned the service account, so **there is no credential for a background sync and
  cannot be one**. The pull runs on foreground, the window is minutes wide for one user, and
  **last-write-wins is correct rather than a compromise**.
- **A disappearance never deletes and never re-creates.** A move-out is indistinguishable from a
  delete (both read as `cancelled`, and we see only our own calendar), so the occurrence **keeps its
  date, clears its `googleEventId`**, and the ambiguity is *asked* in the daily-review batch sheet —
  **Keep / Cancel / Put back** — at the one moment Ido is holding the phone.
- **Titles are written but never read back.** A Google-side rename would silently replace a task
  title with no undo; the reverse failure is visible and harmless.
- **A `DEADLINE` is an all-day banner** titled `Due 23:59 · Submit report`, on one criterion: **the
  Google event does not remind** (§2.5's local notification does), so its only job is to be **seen**,
  which a banner does and a 23:59 marker does not. A timed event would occupy a slot the app cannot
  check and collapse `DEADLINE` into `BLOCK`. It **may be paired with a real `BLOCK`** in a genuinely
  free slot — the obligation and the plan as two events.
- **Pull** is foreground + the shipped 15-minute per-uid throttle; **push is not throttled** (a write
  must not lag the user).
- Only **confirmed** occurrences reach Google. **Sign-out does not delete** the calendar Ido owns. An
  account switch reads as *not mirrored*, not as events to patch. An event Ido creates **by hand**
  inside the GoalPilot calendar is **left alone**.

**Per-calendar sharing, and the honest limit.** Ido chose some calendars shared in full and others
only as busy/free — and **Google cannot enforce that split** (scopes are per-scope, never
per-calendar). So it is **a promise GoalPilot keeps**, honoured as far as possible by **incremental
authorization**:

| Trigger | Scope asked |
|---|---|
| sign-in | `calendar.app.created` + the calendar **list** |
| ticking calendars to avoid | `calendar.events.freebusy` — Google-enforced, **no titles ever reach the app** |
| first setting one calendar to **Full** | `calendar.readonly`, **with that calendar named in the sentence** |

If Ido never uses Full, the promise is never made — and the restraint is visible in **which call is
made**, rather than as a filter after the fact.

### 2.8 Event lifecycle *(`C9e` #28)*

**The app never asks and never loses an event.** The per-action *"also update in Google?"* prompt the
ticket assumed is **ceremony against an unreal risk**: the app's entire blast radius is the calendar
it created itself, and a dialog answered yes ten times stops being read by the eleventh.

- **Changes write immediately, with Undo.**
- **Deletion is cancellation** — Google's trash, restorable 30 days — so a wrong deletion is
  recoverable **twice**.
- **Every destructive effect splits by tense:** **future events cancel, past events stay** as the
  record of time actually spent. That one split answers *"removed, or left as a record?"* as **both**.
- **A rung change `BLOCK` → `DEADLINE` is cancel-and-recreate**, not a patch, so it replaces
  `googleEventId`.
- **§3a, verified after release and it changed the spec:** the 30-day trash is real, but Google
  **does not trash a *this-and-following* delete at all** — so **GoalPilot never uses that shape**; it
  cancels occurrences **one at a time**.
- **One prompt survives, once ever**, beside the incremental scope grant: *Keep it automatic* /
  *Ask me each time*.
- **Bulk is `C1`'s re-scoring pass**: 40 blocks write as **one batch** into the daily review
  (`40 blocks moved · view · undo all`) with one batch-scoped undo — no new modal.
- **Orphaned events are surfaced there and never auto-deleted** — silent cleanup is the one operation
  with no undo affordance.
- Survives the scope being absent: no access, no prompt, no write, reconcile later.

---

## 3 · AI

### 3.1 What was measured, so nothing here is assumed *(`C11a` #16)*

248 live calls at production temperature, in **Hebrew and English**:

| Finding | Number |
|---|---|
| clean JSON | **170/170** |
| valid on every field | 168/170 |
| **Hebrew beat English** | both failures in the whole run were English |
| one wide call vs three narrow | **1.7× faster, ~30% cheaper, 3 requests lighter** on the 30-RPM ceiling |
| prompt-declared enums | **50/50 perfect** |
| free numbers | swing **2× run-to-run**, **1.8×** between languages |
| **the one failure mode** | **silent id corruption** — a 20-char id came back 18 chars long, passing every check except **membership in the list sent with it** |
| Hebrew coach text, no instruction | **0/10** |
| Hebrew coach text, **one prompt line** asking | **3/3** |

**Format is not where the risk is.** Strict `json_schema` is available, but a prose control obeyed
the same absurd constraint — so it buys a guarantee that **survives a model swap**, not reliability.

### 3.2 One wide call per feature *(`C11b` #30)*

**The wide-vs-narrow fork is false.** *"One call means one failure"* describes the **`catch`**, not
the call. Validate **per field group** and one wide call yields independent per-group outcomes — which
is the only thing splitting was ever buying:

| | wide, whole-response | wide, **per-group** | narrow (3 calls) |
|---|---|---|---|
| usable estimates | 18/20 | **20/20** | 10/10 |
| usable routing | 18/20 | 18/20 | 10/10 |
| median latency | 735 ms | **735 ms** | 1,363 ms |
| requests vs 30 RPM | **1** | **1** | 3 |

### 3.3 The features and their schemas *(`C11b` §1, §3)*

**Every request carries the same envelope:** `language: "he" | "en"` · optional
`provider · model · key` (`C13`) · **the membership lists** every echoed id must be drawn from.

**Every response obeys one rule: a field that fails validation is *absent*.** No `null` sentinel
meaning *"I tried"*, no default, no substitute. (`granularity` is the single exception, because `C2`
made **explicit absence** its modelled value.)

| | Feature | Deciding ticket | Today | After |
|---|---|---|---|---|
| **A** | **`estimate`** — difficulty + minutes + granularity | `C1` §5, `C2` §6 | `scoreTask` | **reshaped** — no `points`, `difficulty` an enum, `granularity` appended, wide over a task list |
| **B** | **`plan`** — a proposed draft, and *Adjust Plan* | `C8` §10 | *(none)* | **new** |
| **C** | **`daily`** — theme + practical line per intrinsic edge | `C10` | `getRecommendations` | **absorbed** — the four coach cards go |
| **D** | **`classify`** — which goal, which life area, or a new goal | `R3` / [#6](https://github.com/idomarhaim/Android_Final_Project/issues/6) — **no `C` ticket** | `classifyTask` | **survives, minus `estimatedPoints`** |

`classify` is the feature the map never named, and it is **the highest-volume one** (one call per
Google-Tasks row on import, one per quick-add) **and the one where the only measured failure lives**.

#### A · `estimate`

```jsonc
// request: { language, tasks: [ { id, title } ], … }
{ "estimates": [ {
    "taskId":           "<echoed — MUST be a member of tasks[].id>",
    "difficulty":       "LIGHT" | "ROUTINE" | "DEMANDING",
    "estimatedMinutes": 5..480,                  // integer
    "granularity":      "DEEP" | "FRAGMENTED" | null
} ] }
```

**There is no `points` field, and there never will be.** This deletes `estimatedPoints` at
`RecommendationRepositoryImpl.kt:129` and `points` at `:95`. It **never re-prices a completed task or
a hand-typed duration** — those tasks are not in `tasks[]` at all, which makes the rule **structural
rather than a prompt instruction**.

#### B · `plan`

```jsonc
// request: { language, goal, items: [ { id, … } ],
//            adjustments: { deleted: [ { id, reason } ], alreadyDone: [ id ], userAdded: [ { id, text } ] } }
{ "items": [ {
    "id":               "<echoed — MUST be a member of items[].id — or ABSENT for a new step>",
    "label":            "STATE_YOU_REACH" | "WORK_YOU_DO",
    "difficulty":       …,   // leaves only — MUST be absent when label = STATE_YOU_REACH
    "estimatedMinutes": …,   // leaves only
    "granularity":      …    // leaves only
  } ],
  "changeNotes": [ "<what Adjust Plan changed>" ] }
```

- **The model may not mint an id.** A new step carries **no `id` at all** and is identified by its
  position; the client assigns the id on receipt — making the truncation failure **structurally
  unrepresentable** rather than merely checked.
- **`label` gates pricing**, and it is a cross-field rule no JSON schema can express: a
  `STATE_YOU_REACH` item is a container, so `difficulty`/`estimatedMinutes` present on one is a
  validation failure **of those two fields**, not of the item.
- **Every `userAdded` id must appear in the response.** A response missing one fails validation for
  the **whole `items` group** — the one place a whole-group failure is correct, because a plan with
  the user's own step silently dropped is worse than no plan.
- **`Renumber` is not in this schema and never calls the model.** It is the feature's offline half,
  and it is mechanical — which is how `C8` satisfies
  [§0.1](#01-the-free-model-rule-12-notes-scope-fixed-by-ido-2026-08-0708) for free.

#### C · `daily`

```jsonc
// request: { language, edges: [ { id, category, hasMeasure, … } ] }   // one call per day, all edges
{ "lines": [ {
    "edgeId":  "<echoed — MUST be a member of edges[].id>",
    "theme":   "STARTING"|"CONSISTENCY"|"SETBACK"|"PATIENCE"|"FINISHING"|"DISCIPLINE"|"PERSPECTIVE"|"REST",
    "message": "<the practical line>"
} ] }
```

- **No quote identifier ever crosses the wire, in either direction.** The app resolves
  `corpus.filter(category ∧ theme)` then `hash(today + edgeId)`. The model is given the job measured
  **perfect** (a prompt-declared enum) and never the job measured **failing** (reproducing an opaque
  token). **The failure mode is not caught by a check — it is unreachable.**
- **`FINISHING` is only valid for an edge sent with `hasMeasure: true`** — every other theme is
  computable without a measure.
- **`progressPercent` is sent as absent, never `0`.** The failure is not a missing sentence but a
  **discouraging false one**.
- **The prompt carries the prohibition — name no person, cite no book, quote nobody.** This is **the
  one contract in this spec with no validator**, and it is named rather than papered over. The
  structural mitigation is that `Recommendation` carries **no `author` and no `source` field**, so a
  fabricated attribution has nowhere to *render* as an attribution.

#### D · `classify`

```jsonc
// request: { language, taskTitle, goals: [ { id, title } ], lifeAreas: [ { id, name } ] }
{ "suggestedGoalId":       "<MUST be a member of goals[].id — else ABSENT>",
  "suggestedNewGoalTitle": "<content — authored once in the picker language, never re-rendered>",
  "suggestedLifeAreaId":   "<MUST be a member of lifeAreas[].id — else ABSENT>",
  "suggestedCategory":     "<GoalCategory>",
  "confidence":            0.0..1.0,
  "rationale":             "<speech>",
  // …and the whole of schema A's estimate group, validated INDEPENDENTLY
  "difficulty": …, "estimatedMinutes": …, "granularity": … }
```

`suggestedNewGoalTitle` is **content, not speech** — a goal title the sorter wrote *became content
the moment it appeared among his goals*, so it is never translated and is exempt from the script
check. `rationale` and `confidence` are **speech** and are kept.

### 3.4 The failure contract *(`C11b` §4)*

| Class | What it is | Observed in 248 calls | Contract |
|---|---|---|---|
| **transport** | no key, `5xx`, timeout, `429`, a retired model id | **0** | whole-response fallback, **silent**, **no retry** |
| **structural** | unparseable, missing required field, out-of-enum | **0/170** | should be unreachable under native enforcement; if seen, treat as transport |
| **semantic** | a plausible value that is *wrong* — a non-member id, out-of-range, cross-field violation | **2 — the only failures in the run** | **that field is omitted, and only that field** |

**No retries, and it is a derivation.** A retry aims at the structural class, which never occurred; it
costs a request against the 30-RPM ceiling the wide call exists to protect; and `429`/`5xx` already
ride the fallback silently.

**Per-feature fallbacks** — each already specced by its own ticket:

| Feature | Absent field | Falls back to |
|---|---|---|
| `estimate` | `difficulty` | `ROUTINE` |
| `estimate` | `estimatedMinutes` | **ask the user *how long?***, `DEFAULT_MINUTES` if skipped — the app never guesses a duration from a word count |
| `estimate` | `granularity` | absent; the daily review falls back to `estimatedMinutes` alone |
| `plan` | any | **no proposal at all**; `Renumber`, curation and commit still work |
| `daily` | `theme` | the app derives the theme **by rule** — the same feature with one input substituted, and the screen is indistinguishable |
| `daily` | `message` | the quote alone, from the corpus in the APK |
| `classify` | `suggestedLifeAreaId` | on import the Google Tasks list wins anyway; on quick-add, unfiled |
| `classify` | `suggestedGoalId` | **the new-goal branch — and this one speaks** |

**Every row is silent except the last**, per [§0.4](#04-legal-but-never-silent-c7-refined-by-c13-5).
An absent `suggestedGoalId` does not *degrade* the outcome, it **changes** it — so it **tells and
never asks**, which is exactly
[#8](https://github.com/idomarhaim/Android_Final_Project/issues/8)'s trigger, with
[#6](https://github.com/idomarhaim/Android_Final_Project/issues/6) already fixing the register.

**Validation lives in the Cloud Function, singly.** The four provider adapters are server-side only
(`C13`), the membership lists already travel client → Function, and the client keeps exactly one job:
spec §8's fallback, which has to work offline anyway. Native `json_schema` enforcement stays **as
well** — **structural** in the adapter, **semantic** in the Function; no schema at any provider can
catch a well-formed id that was not in the list.

### 3.5 What the model may and may not author

| Thing | Author | Rule |
|---|---|---|
| `minutes` | **Ido** (model estimates by default) | a fact; a typed value is **sticky forever** |
| `difficulty` | **the model, only** | a judgement about the work |
| `granularity` | **Ido** (model proposes) | a fact about how he works; sticky |
| `points` | **nobody** | computed: `round(minutes/3) × difficulty` |
| the plan's **shape** | falls out of per-item labels | never asked as a separate judgement — *"is this goal big enough for milestones?"* is the 2×-swinging class |
| an **intrinsic edge** (a goal) | **Ido, always** | the sorter must **never** invent a goal; low confidence leaves `goalId` null |
| **instrumental structure** | the app, silently | filing, scheduling, linking, breaking down |
| a **quote** | the curated corpus | only a shipped corpus may name a real human |
| the **practical line** | the model | **names nobody** |
| a **goal title** from the sorter | the model, once | it is **content** the moment it lands in his list |

**Where a format could be specced either as one global judgement or as N per-item enums, prefer the
enums.**

### 3.6 Bring-your-own key *(`C13` #32)*

> **The key buys a different *credential*, not a different *pipeline*.**

This is `C10`'s argument inverted: `C10` rejected an alternative whose **degraded** path grew a second
mechanism; `C13` rejects one whose **enhanced** path does — because for an audience of one, **the
copy that drifts is the one exercised only when a key is present.**

- **Four named adapters and nothing else:** GROQ (the free default), OpenAI, Anthropic, Gemini. No
  generic *"any OpenAI-compatible URL"* escape hatch.
- **Stored on the device, encrypted** — Keystore `EncryptedSharedPreferences`, beside `C15`'s
  per-device settings, and explicitly **not** in Firestore. Owner-only rules would have covered it
  free; a third-party secret at rest in a backed-up, exportable store is a different posture. Costs
  one new dependency: `androidx.security:security-crypto`.
- **The key reaches the Functions per call and is held nowhere**, so the client still gains **no
  outbound path to any provider** — spec §5's property is **kept, not spent**.
- **One switch for all four AI features** — not per-feature opt-in, and not a repair rung (which
  would have left the prose he reads daily still the free model's, so he would pay and see nothing).
- **The ladder is `user key → free model → local fallback`, and it never degrades below today.**
- **Failures are classed:** `401`/`403` **speaks once at the point of use** (only he can fix it);
  `429`/`5xx` ride the fallback **silently**; the latch clears when the key is edited or any call
  succeeds. Paired with a **permanent status line in Settings** so a dead key is not invisible three
  weeks later.
- **Quality only, never behaviour.** Derived from
  [§0.1](#01-the-free-model-rule-12-notes-scope-fixed-by-ido-2026-08-0708), not asked.
- **Every adapter enforces the schema natively *while* app-side validation stays** — they catch
  different failures.
- **Never log a provider error body verbatim** on a user-key call. `functions/src/index.ts:52-55`
  currently throws the provider's raw body into Cloud Logging.
- **Extend the status line to the project key** — *which provider answered last*. `AGENTS.md` already
  records that a retired GROQ model id **fails silently** into the local fallback, and under
  omit-never-substitute it still would, because a dead pin is a *transport* failure.

### 3.7 The AI-proposed plan *(`C8` #24)*

> **A proposed plan is a persisted draft with three exits per step, and the draft gate is what makes
> the AI's latitude affordable.**

- **`#24`'s own enumeration of a "stage" was obsolete before it was read.** `C4` had already made
  goal and milestone **roles carried by an edge**, so a stage is simply a **milestone or a task**,
  decided per item by *state-you-reach* vs *work-you-do* — and `C18`'s container rule means a *state*
  stage is **never priced**.
- **The AI picks the plan's shape per goal**, but never as a separate judgement: the model labels
  **each item**, and the shape **falls out**.
- **Three exits per step, not two: keep · already-done · delete.** *Already-done is not a soft
  delete* — it is **evidence flowing backwards** into the next plan.
- **Renumber-or-replan has no policy answer:** it is **two buttons the user chooses between after the
  fact** — `Renumber` (mechanical, no model, and therefore the non-AI fallback) and an
  **`explain delete` free-text** feeding **`Adjust Plan`**, which is **one batched call** carrying
  every deletion, reason, done-mark and typed step.
- **The draft gate is normative, not cosmetic: nothing the model decides here may reach Firestore
  without passing his eyes.** That is what makes *"the AI decides"* how much curation `Adjust Plan`
  may overwrite affordable.
- **An already-done mark is a fact** — the model may reorder around it but never un-mark it. **The
  adjusted draft discloses what changed** (`changeNotes`).
- **An already-done step pays like any completed task** — stamping `minutes` + `difficulty` —
  **unless it duplicates a task already in the app**. The app **computes the likely collisions
  itself** and asks only about those, rather than making him query his own data.
- **User-typed steps: never deleted or replaced, always estimated, rewording only ever shown beside
  his words.** A typed step is the highest-signal object in the draft, so `Adjust Plan` carries it as
  **context**, letting the model propose the neighbours it now knows it missed. (Its *existence* is
  the user's assertion; its *treatment* is the model's judgement — which is why *frozen* and *in the
  pot* were both wrong.)
- **The draft persists exactly as left** — a real object, one per goal, **no expiry**. So the
  duplicate check runs **on open as well as on commit**.
- **Re-proposal adds no second mechanism** — a re-plan just opens a draft.

### 3.8 The daily line and the quote feed *(`C10` #29)*

> **`R21` is two sentences wearing one hat, and the seam is attribution.**

- **Only a curated corpus shipped in the APK may name a real human.** The model writes the practical
  line and **names nobody**.
- **Selection is *the AI judges, the app computes*, reused unchanged:** the model returns **one word**
  from a closed theme list, and the app resolves `category ∧ theme → hash(today + edgeId) → quote`.
- **The deciding argument was the fallback, not the safety.** With no network the app derives the
  theme **by rule** and *the rest of the pipeline is identical* — the degraded feature is the same
  feature with one input substituted. The rejected *"model picks from a shortlist"* alternative
  degrades into a **different mechanism**, for nuance that is worthless once every candidate is
  already apt.
- **Tagging is `GoalCategory` + a theme**, and the theme axis keys on **days idle · open work · age**
  — signals **every** goal has — so **an unmeasured goal is well-aimed, not degraded**.

**Themes, fixed:** `STARTING · CONSISTENCY · SETBACK · PATIENCE · FINISHING · DISCIPLINE ·
PERSPECTIVE · REST`, plus a **ninth from `Goal.deadlineEpochMillis`** — a field that existed and was
used for nothing, which now becomes a theme **and an urgency weight**.

**The corpus is two tiers, and Hebrew-first.** Public domain **including natively-Hebrew sources**
(Pirkei Avot, Tanakh), plus a hand-added modern tier, because it must fill a **10 × 8 grid on day
one**. **A Hebrew translation of a public-domain work is *not* public domain** — a translation is a
separately copyrighted derivative — so lapsed editions are verified **per edition, not per author**,
and **the Hebrew pool is the one that sets the repeat interval** in Ido's own default language. A
quote is **never machine-translated**: published translation, or that language only.

**Verify-by-sampling was put on the table so its rejection is on the record:** the errors are
plausible and silent, so a clean sample proves a low rate and **identifies nothing**.

**Attachment is per *intrinsic* edge** (`C4`), so `E19`'s Goal 2 gets **one** sentence and not two;
an object that is only a milestone gets **none of its own** and shows its goal's; and the feed stays
**bounded** under `C18`'s depth.

**`R22` answered yes as a socket, not a dependency.** The line names **the task with the smallest
`estimatedMinutes`** — *the one thing you could do now* — not the earliest created, which on a stale
goal is the task that has been **avoided**. Ordering wins over the heuristic once `C8` supplies one.
**The app chooses the task; the model only phrases it**, so naming a task that does not exist is
impossible.

**Also decided:** stored **on the phone** by date (the quote itself needs no storage); a **logical
day starting at 04:00** rather than midnight; a **recently-shown ring per edge**, because
`hash(today + edgeId)` can repeat a quote within the week; a **life area resolves to the union of its
goals' categories** at theme `PERSPECTIVE`; **today's four coach cards are absorbed** (a coach card
*is* the practical line with no goal attached); and the goal screen shows **two blocks** where `R21`
asked for one sentence.

**Home shows the quote plus the 2–3 goals that most need attention — and one of those slots is
reserved for a goal in a *good* state**, at identical cost, because the feed as first specced is **a
daily list of the goals you are failing at**.

**`C2` must not introduce a second vocabulary for a goal's moment.** The two axes are **orthogonal**
— a theme derives from the goal's *state*, a task type from the task's *content* — so `C2` may phrase
the practical half and no more.

---

## 4 · Screens and presentation

### 4.1 The material contract *(`C12` #31, `#12` Standing preferences)*

**Four materials ship as a user-selectable skin — glassmorphism · liquid glass · neo · dark neo.
Metal is deleted.**

The discriminator for *why this is a picker and the layout is not*: **a layout a user dislikes is
evidence the layout is wrong** (storing their workaround preserves the defect and hides it), while
**a material a user dislikes is evidence of nothing**. So a **preference store belongs exactly where
there is nothing to be right about**.

**No screen may depend on a property a single material has.** Translucency exists in glass and liquid
only; a shadow-pair extrusion exists in neo and dark neo only. **A screen specifies
`surface · groove · elevation · accent`, and each material answers those four its own way** —
otherwise every design has to be drawn four times, which is the cost that would make four materials
unaffordable.

| | Depth comes from | How arcs are drawn |
|---|---|---|
| **Glassmorphism** | **blur** — the canvas stays legible through the panel | thin, flat, slightly transparent, a coloured bloom instead of a bevel |
| **Liquid glass** | **refraction at the edge** | translucent body, bright inner rim, dim outer counter-rim, one specular streak |
| **Neo** | **a shadow pair** on one flat surface | inset track, softly extruded arc, muted hues, no rim, no gloss |
| **Dark neo** | **a deep shadow pair + one saturated gradient** | charcoal groove, softly extruded arc, one cyan→blue accent |

**The material is a *second axis*, not the `AppSkin` the app already has.** `AppSkin`
(`AURORA`, `BLOSSOM`) is a **palette**; the material is a **surface**. They do **not** multiply
freely, so each material declares a **palette transform** — `identity · mute · single-accent ramp` —
and the schemes are **generated, not hand-authored**.

Two consequences that would otherwise be found late:

1. **Dark neo's accent must derive from the selected skin**, or picking Blossom under dark neo
   silently renders Aurora and the skin picker stops working for a quarter of the set.
2. **The product is ragged, not rectangular.** Dark neo has **no light scheme**, so a material must be
   able to declare itself **brightness-locked**, and the picker must **say so** rather than letting
   the light switch quietly do nothing.

**Two rules every screen inherits, each from a real WCAG or identity failure:**

- **`--edge`** — every control carries a **hairline contrast anchor**; no affordance is ever
  shadow-only (neo's known WCAG failure).
- **`.tag`** — a category is **written in words** beside its dot, because dark neo collapses the six
  categorical hues into one ramp and **colour stops carrying identity**.

**The cost, stated:** `backdrop-filter`, SVG filters and CSS shadow pairs are **web** primitives. In
Compose the equivalents are `Modifier.blur`, `RenderEffect` (API 31+, with a fallback below), and
hand-drawn `Canvas` shadows — and **a widget has none of them**. Four materials **multiplies** this
rather than picking one.

### 4.2 Navigation and the two surfaces *(`C9b` #26, `C12`)*

**The three-way calendar choice was a false fork:** `A`/`B`/`C` were not rival screens but **three
zoom levels of one thing** — `C` is *now*, `B` is *today*, `A` is *this week*. So any single pick
threw away two.

**Two surfaces, one job each:**

- **Home answers *what needs me?*** It leads with the decision stack when decisions are waiting and
  falls back to the ordinary dashboard when none are — **adaptive, so it is never a permanent nag**.
  Home **summarises and links rather than hosting**; the first attempt put the same decision deck on
  both and produced two screens Ido could not tell apart.
- **Calendar answers *when?*** with a zoom: **agenda ⇄ 3 days ⇄ week**. The agenda is **not a third
  screen**; it is the calendar's lowest zoom, which is what collapses three surfaces into two.

**Four bottom tabs.** Five is a crowded bar, so **Profile moves to an avatar in Home's top-right** —
what Gmail, YouTube and Google Calendar all do — and **Calendar takes the freed tab**:
`בית · מטרות · לוח שנה · חברתי`.

### 4.3 The calendar surface *(`C9b` #26)*

- **Default view is 3 days, and that is measurement not taste.** Seven columns on a 390 dp phone is
  **~46 dp per day** — no Hebrew title and no time range survives it. Week view stacks the times
  **start over end**, the only thing that fits at 46 dp; at 3 days a column is ~110 dp and both fit.
- **Fully actionable:** create by FAB or by tapping a slot, drag to move, tick to complete.
- **Shows** challenge windows, goal deadlines, **hand-made Google events in grey** (readable at no
  extra scope, and hiding them would make the app's own calendar look empty), and **a strip for work
  due today that was never given a time** — without which the calendar quietly lies about the day's
  real workload.
- **A `DEADLINE` is only ever a banner in the all-day strip**, never a timed box.
- **The rung is carried by the form of the leading time column, never by a glyph on the chip** — the
  chip carries **only** the life area (a colour dot and its name). No legend, no symbol vocabulary.
- **A per-day load bar** and a *booked/free* ring, **arithmetic not inference**, so they cost nothing
  against the free-model rule. Turns red past **75% of waking hours**. **Spans contribute nothing.**
- **`OVERDUE` *and* `AWAY` are both carried forward from other days.** Both need action, and neither
  waits for you to navigate to its date — an event that vanished from Thursday's calendar would
  otherwise surface only when Thursday arrived, exactly too late to put it back.

### 4.4 Charts and the dashboard *(`C12` #31)*

**No chart picker.** The wish behind one is almost always *"this card is useless to me"*, which is a
design failure to fix for everyone. Replaced by: **a card with nothing to say hides itself**, and **a
range picker that remembers**.

**The chart set:**

| Chart | Fate |
|---|---|
| `DonutChart` | **stays** — with **direct labels and leader lines**, per-side vertical de-collision, and the percentage under each name, so there is no legend round-trip. Label side is geometric, so RTL needs no special case |
| `StackedColumnChart` | stays |
| `ProgressRing` | stays |
| `HorizontalBarChart` | **retired from Analytics** — both its users were killed by decisions this map already took (`C7` permits a goal with no measure; count-weighting died with `C16` and again with `C3`) |
| **effort against outcome** | **added**, and **its form was forced rather than chosen** |

**Why the effort/outcome chart's form was forced:** a percentage is a fraction of *its own* target, so
ranking by movement partly ranks **how modest the goals are**. So the app **orders only the quantity
it may order (minutes)** and **names** the rest — and the honest render found the better headline
anyway: *the area taking most of the week has no measure*, so no area is scored and none is blamed.

**Arrangement was a false fork** — `A7`'s *what do I do now?* already had a home from `C9b`, so Home
hosts one question and links to the other. **What was actually wrong was the order**: two
never-retiring setup cards and five generic tips sat **above the user's own goals**.

**The points hero is demoted to a level ring on the avatar**, because points are
`round(minutes/3) × difficulty` and the donut beside them is built from the same minutes — Home was
showing **one quantity twice**, so removing it loses no information.

⚠️ **A plain mean of `progressFraction` (`DashboardViewModel.kt:103`) breaks once overshoot is legal.**

### 4.5 Widgets *(`C12`, `R24`, #10)*

All **seven** cards ship at `2×2` / `4×2` / `2×4` / `4×4`.

Revision 1's rule *"a chart whose honesty depends on a footnote may not be a widget"* was **overturned
by Ido and re-cut as a size rule**: **the disclosure shrinks to the smallest true sentence the tile
can hold, and no size ships without one** — because *"this cannot be a widget"* was really *"this
cannot be a widget at every size"*.

Three constraints, confirmed rather than assumed:

- a widget is **not a live screen** — Android renders a snapshot and refreshes on a schedule, so
  **nothing animates**, and `ui/components/ChartAnimation.kt` does not run there;
- the **launcher decides the real dp** a `2×2` or `4×4` occupies, and it varies by device and
  launcher, so every tile must survive being **smaller than it is drawn**;
- a tap can only **open the app at a destination** — a widget cannot show a dialog — so *"tick it
  here"* means the app opens on that task.

**Glance has no equivalent of the SVG specular bevel and turbulence** used in the prototype: in a
widget that depth must come from a pre-rendered bitmap or from `Canvas` drawing.

### 4.6 LOG PROGRESS *(`C6` #22)*

> **A person sets the outcome, never the effort — and `R14`'s premise is false: there is no
> percentage field and never was.**

The dialog takes an **Amount in the goal's unit** and `ProgressRepositoryImpl.kt:87` **adds** it, so
the app already records an *entry*. What read as *"changing the percentage myself"* was
`Goal.unit`'s `"%"` default labelling the box **Amount (%)** — the map's most-repeated finding at its
first site, already deleted by `C7`.

**Ido overturned the session's recommendation twice, and the pair is coherent:**

- **an entry is editable *forever***, and
- **every edit is *always marked***, with the original recoverable one tap away and a **delete kept
  struck through** rather than vanishing.

That is **not an affordance but an arithmetic change**: `currentValue` **stops being a stored
aggregate and becomes a sum over entries** — `C14`'s move for `score`, `C1`'s for the points total,
`C9a`'s for temporal state, and `C20`'s general rule.

**Derived, not asked:** a `sourceKey`-bearing (Health Connect) reading is **read-only and is corrected
by logging beside it**; and **whether logging adds or sets is per goal**, riding `C7`'s input mode.

**The optional duration row** — never pre-filled and never guessed. An unplanned run moves the goal
but not effort, because `TimeAllocationUseCase` sums minutes over *completed tasks* and
`ProgressEntry` has no duration. So the log carries an **optional duration**, and when filled it emits
**the same timestamped completion fact a ticked task emits** (`difficulty` assumed `ROUTINE`) — one
sum instead of a second pipe. All three offered alternatives were rejected: **guessing** violates
*minutes is a fact Ido owns*; **asking every time** is meaningless on most kinds; **silence** is not
neutral, since this is a **missing fact** rather than an effort-vs-outcome *gap*.

**A retroactive task is deliberately not created** — `C4` §9 permits silent instrumental structure, it
does not recommend inventing occurrences, `isDone` and parentage for a run that already happened.

### 4.7 Per-life-area success and failure *(`C19` #41, `E4`)*

> **A failure is a missed *window* and nothing else; a goal with nothing due is missing a *step*, not
> failing.**

`C9a`'s three words are the whole vocabulary: **`MISSED` is a failure, `OVERDUE` is not, `EXPIRED`
counts for nothing.**

- **A success counts in full in every area** the task serves, while its **minutes divide** — and both
  numbers sit on **one screen deliberately**, because that asymmetry is the point.
- **Two numbers, never a rate.** A single "success rate" is the tidying-away `C3` and `C5` both
  refused.
- **Nothing ages out.** History is permanent, and the view reports over a **window you pick** —
  `30 days · 8 weeks · 6 months`, default **8 weeks**. A window is a **filter over history, not decay
  of it**.
- **There is no lifetime failure counter anywhere**, because a number that can only rise is the *list
  of the things you are bad at* this screen exists **not** to be.
- **What a window is** is answered **on the screen**, under the run: *a window counts as kept when
  everything due in it was done*. The numbers are meaningless without it, so it is not spec-only text.

**There is no dormancy state, stored or named.** *Asleep · invisible · failed* were three labels for
`C10`'s already-decided theme axis, whose `STARTING` value **is** *"never scheduled"*. Instead the row
shows **`no next step · idle 4 months`** and offers **the step that is actually missing**:

| Situation | Offer | Whose feature |
|---|---|---|
| `open work = 0` | **Break it into steps** | `C8` — no new AI surface |
| work exists, no dates | **Schedule the first one** | `C9a` |

Counted in **neither** number, and **the screen says so in words**. **`Let it go` stays as a command,
never an inference** — `C4` forbids the app asserting an intrinsic edge by itself.

**One component, two placements:** above the goal list on the life-area screen, and beside the time
donut on analytics — where the asymmetry sentence lives **and nowhere else**.

**Outcome state never rides on hue either:**

```
kept          filled
missed        hollow
still-owed    dashed with a centre pip     ← the one state that must NOT read as a failure
nothing-due   dotted
no next step  a dashed ring carrying a +   ← deliberately unlike all four: an invitation, not an outcome
```

The run therefore reads in dark neo, in greyscale, and to a colour-blind eye — and **there is no red
on this screen at all**, which is a tone decision as much as an accessibility one.

### 4.8 Bidi and RTL — a defect class, not a mockup artefact

**Every time, date, number and range string owes direction isolation.** The Unicode bidi algorithm
reorders a Latin-digit run inside an RTL paragraph, so `09:00–12:00` renders as `12:00–09:00`, and
`2 of 8` reorders too. In HTML that is `direction:ltr; unicode-bidi:isolate` / `<bdi>`; **the same bug
appears in Compose** unless the build session isolates the same strings.

Found independently on **three** screens (`C9b`, `C12`, `C6`, `C19`), which is why it is stated here
once as a rule rather than four times as a note.

**Two more Hebrew-only defects, both found by rendering and invisible in the source:**

- **A Hebrew prefix on a Latin run lays out on the far side of it** — `מ‑Health Connect` renders as
  `Health Connect‑מ`. Rewrite as `מקור: Health Connect`.
- **The donut's centre caption overruns its hole and collides with the labels in Hebrew but not in
  English**, and the slice percentage reorders (`27% לימודים`).

**No Hebrew literal may survive into an English render.** Asserting that **absolutely** caught three
instances beyond the one Ido spotted.

---

## 5 · Cross-cutting behaviour

### 5.1 Localization *(`C15` #15, `C15b` #35)*

> **"Language" is three independent settings, not one.**

| Setting | Owns | Default | Stored |
|---|---|---|---|
| **Language** | every *word* — chrome, AI text, the §8 fallback, month names | the **device language** | **per-device, beside the skin** — it must be known before the first frame, and the account is not known until Auth resolves |
| **Region** | **first day of week** and date order | the **device country** | **user-overridable and decoupled from Language** |
| **Direction** | RTL/LTR | **follows Language, not Region** | derived |

**Region is decoupled on Ido's call**, overturning a proposal to pin it to Sunday: **Israelis in
hi-tech often work in English yet still start the week on Sunday.** So English-in-Israel is
left-to-right with a Sunday week start.

**AI text follows the picker**, at **one prompt line** — `C11a` priced it at 0/10 → 3/3. `C15`'s
per-feature Hebrew veto is **declined and rebuilt as a per-response script-share check**, because
**bad Hebrew is a property of an answer, not of a feature**: a per-feature veto is a permanent global
switch thrown on evidence from a sample; a per-response check catches the specific bad answer and lets
the next good one through.

**The §8 fallback is authored natively per language, never translated** — generalised to a spec rule
for **any app-authored sentence embedding user content or a number**.

**The trend chart is exempt from mirroring** — time stays left-to-right — because `DonutChart` and
`ProgressRing` are `Canvas` arcs in absolute coordinates and **cannot** mirror, so a mirrored timeline
would contradict them on one screen.

**Terminology:** the Hebrew label for the `Goal` entity is **יעד**, not מטרה (`E1`). Nothing in the
English UI changes.

#### Speech vs content — what a language switch touches *(`C15b`)*

> **The fork was almost empty, and the discriminator is *speech vs content*.**

**Grounded fact, and it answers the question:** **no AI prose is persisted server-side at all.**
`Recommendation` (every coach card, encouragement, nudge and the practical line) is parsed straight
into ViewModel state; there is **no `recommendations` collection**; `Task` has no `description`; task
titles are the **user's own words**. **Zero language stamps exist** — `locale|language|lang` across
`domain/model/` returns no matches.

- **Speech** — the app talking — **follows the picker for free.** Speech that outlives a view keys its
  cache by **`(date, language)`** instead of `date`, so a switch is a **miss**: zero invalidation
  logic, zero stale-flag bugs, no field, no migration.
- **Content** — Ido's own list — **is never translated.** The only AI prose reaching Firestore today is
  a **goal title** from the smart sorter, and it became content **the moment it appeared among his
  goals** — which dissolves the *"does editing it make it his?"* question: **landing in his list did**,
  not the edit.

**The `C8` draft — the longest-lived instance of the problem — needs no field.** A `languageTag` was
proposed and **withdrawn**: a stale draft needs an unfinished draft **and** a switch inside that
window **and** a return to it, for a **per-device setting an audience of one sets once**, and an
unendorsed proposal already counts for nothing. **So: no field, no dialog, no translate step, no
regeneration, zero model calls at switch time** — the draft is simply stale, and Ido uses `C8`'s
existing Discard/Generate.

**The improvement is to derive the language from the text rather than store it.** Hebrew and English
differ by **script**, so one `\p{Hebrew}` test carries the *legal but never silent* line — *"This plan
was written in Hebrew."* It is **better** than a stamp rather than merely cheaper, because **a stored
field can lie** (a user edit, a migration default) **and the script in front of you cannot**.
**Boundary, stated rather than assumed:** a third **Latin-script** language reopens this, and that is
`C15`'s scope, not a defect here.

**Net for `C15b`: schema change none, new mechanism none, new field none** — one cache key gains a
component, one screen gains one sentence.

**Two defects filed as spec lines rather than fixed** *(see [§10](#10--gaps-defects-and-open-work))*:
no prompt states an output language; all ten date formatters are **process-scoped `val`s** no switch
can move.

### 5.2 Who owns a derived number *(`C20` #42)*

> **A derived number gets a stored writer if and only if somebody who cannot read its inputs has to
> read it.**

The ticket's own trichotomy — *one Function · one trigger per site · a shared module* — was **false**,
because all three presume every derived number needs a writer. And the rule is **checkable rather than
a matter of taste**, because `firestore.rules` already draws that boundary at `isOwner(uid)`.

Of the map's **seven** derived quantities:

| Quantity | Verdict |
|---|---|
| `User.level` | **already** a computed property (`User.kt:14`) — never stored, never read from Firestore. It is the **worked example**, not a site needing a server owner |
| `points` | a **sum over completion facts** — no writer |
| `goal.currentValue` | a **sum over progress entries** — no writer |
| `C5`'s upkeep | derived-never-stored |
| `C19`'s occurrence state | derived-never-stored |
| `publicProfiles.level` | **deleted outright** — a stored function of `points` **in the same document**, whose `resolvedLevel()` fallback **can never fire** because both writers write ≥ 1 |
| `challengeParticipant.score` | **survives** — it crosses the ownership boundary |

**So: one projection function, two trigger registrations, and zero client writers of derived state.**
Neither *"one Function"* nor *"three triggers"* is literally what ships.

**`C1`'s shape generalises and #34's does not**, decided by **#34's own stated risk**: *project from
facts* is idempotent **structurally** — running it twice writes the same number — while
*recompute-and-store* makes double-crediting something the function must be **careful** about. That is
also why `FieldValue.increment` stays rejected: **`increment` *is* the accumulator.**

**Two client transactions already write `goal.currentValue`** (`GoalRepositoryImpl.kt:87`,
`TaskRepositoryImpl.kt:135`), so the pattern was never three sites converging on a server — it was
**two client writers of one field**.

**The honest residual, not buried:** the arithmetic now exists in **Kotlin and TypeScript** — a second
*implementation* that can disagree. Accepted, because avoiding it costs the offline win entirely, and
**pinned by a shared `facts → expected numbers` fixture both test layers run**.

**`firestore.rules` gets its first field-level condition** (the participant row keeps three writers
with different rights), which is the one place this makes the rules harder.

### 5.3 Offline *(`C20`, and `C21` #43 — **open**)*

**The offline win is free, and it is the product half of `C20`.**
[#34](https://github.com/idomarhaim/Android_Final_Project/issues/34) priced its proposal at *"a second
or two before the donut moves"*, and under `C20`'s resolution **that cost is not paid at all**: facts
are ordinary writes that hit the offline cache, so

- **completing a task offline works for real** (`A5`),
- **the donut moves immediately**, and
- [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)'s optimistic overlay, undo
  message and connectivity pre-check are **deleted rather than kept**.

**Only other people's numbers are eventually consistent, which they always were.**

⚠️ **What is left is `A6` and it is an open ticket, not a spec section.**
[`C21` #43](https://github.com/idomarhaim/Android_Final_Project/issues/43) — *whether the app must
**say** it is offline, and whether a cached number must look different from a live one* — was opened
**2026-08-15** and is live. **This section is incomplete until it closes.**

---

## 6 · Challenges *(`C14` #23)*

> **A challenge scores from nothing of its own: it scores from each participant's goal.**

The ticket's enumeration — *(a) goal progress · (b) task count · (c) a health metric · (d) a typed
number · (e) per `ChallengeType`* — **needed no adjudication**, because **(a) is the answer and (b),
(c), (d) are how a goal is fed**, which this map decided elsewhere.

**Put a goal and a challenge side by side after `C4` and `C7` and they are the same object** — title,
a `kind` + `word` measure, a start, a current value. So `R1`'s *"a challenge does not sync with my
tasks or Health Connect"* is **not missing wiring but two representations of the same walk**, and the
fix **deletes one rather than building a second pipe into it**. `SyncHealthDataUseCase` already writes
a `ProgressEntry` against a **goal**, and `sourceKey` already dedupes a re-sync — **the entire
left-hand side existed.**

- **The score is *movement since you joined*, not the goal's current value** — made possible by
  `C3`'s new `start`, and without it a weight-loss race ranks by **who is heaviest**. Summed from
  timestamped entries rather than stored as a delta, so **relink, unlink, backfill and dedup all fall
  out**, and joining with a year-old goal **imports no history**.
- **A challenge has no optional measure.** `C7`'s optionality does not carry: there is nothing to
  compare without a shared unit.
- **`points` may never be a challenge metric** — that ranks by time logged. `metricUnit = "points"`,
  the default that produced the ticket, is **deleted rather than re-homed**.
- **Joining links or creates a goal**, so a challenge hands you tracking you did not have.
- **`score` becomes server-owned** — the client writes the **fact**, a trigger owns the **derived
  number**. The honest residual: this stops a win being **typed**, not a reading being **forged**.
- **`ChallengeType` is deleted.** It was purely presentational; nothing branched on it to source a
  score.
- **No Health Connect connection → you cannot join a health-sourced challenge.** Ido's own call,
  honoured verbatim: **comparability over inclusion**, with the gate made **a route rather than a dead
  end**.

**Changing the measure needs every participant's approval** — `C7` said this had nowhere to live, and
it is **representable in the existing rules partition unchanged**: the owner writes `pendingMeasure`
on the challenge document, **each participant writes `approvedChangeId` in the one document they are
permitted to write**, and the Function applies it when every row agrees.

---

## 7 · Data shape and migration

### 7.1 The delta, by document

> **Field *names* here are illustrative where the deciding ticket fixed a shape but not a name.**
> `declaredBy`, `goalEdges`, `parentIds` and `pausedUntil` are the tickets' own words and are
> normative. `measureKind` / `measureWord`, `parentTaskId`, `start`, and the `occurrences` /
> `completionFacts` / `planDrafts` collection names are **this file's**, chosen to make the table
> readable — the *shape* is decided, the spelling is the build session's.

| Collection | Field | Change | Migration |
|---|---|---|---|
| `users/{uid}/goals` | `declaredBy` | **new** — `USER \| AI_SUGGESTED \| UNKNOWN`, absent ⇒ milestone | backfill **`UNKNOWN`** — nothing records who made the existing goals, and the migration must not pretend otherwise |
| | `parentIds` | **new**, plural, on the child | backfill `[]` |
| | `lifeAreaIds` | `lifeAreaId` → plural | backfill `[current]` / `[]` |
| | `start` | **new** — the origin for `(current − start)/(target − start)` | backfill **`0.0`** |
| | `measureKind` + `measureWord` | replaces free-text `unit` | `"%"` survives as a **chosen** `PERCENT` measure; a defaulted `"%"` becomes **absent** |
| | `currentValue` | **stops being stored** — a sum over entries | derived; the two client writers are deleted |
| | `progressFraction` | **clamp removed** (`0..1`) | — |
| `…/tasks` | `goalEdges: [{goalId, contribution}]` | replaces `goalId` | backfill `[{goalId, contribution: undefined}]` |
| | `parentTaskId` | **new**, nullable, cardinality one | backfill **`null`** — every existing task is a leaf, so **day one reads identically** |
| | `difficulty` | **new** enum | backfill **`ROUTINE`** |
| | `granularity` | **new**, nullable, **never defaulted** | backfill `null` |
| | `points` | **stops being stored** — a view of effort | derived |
| | `isDone` | **splits three ways** — stored with no occurrences, **derived** with them, **absent** on a recurring task | — |
| | `progressContribution` | **deleted** — moves onto the edge as `contribution`, defaulting to **undefined** | — |
| `…/tasks/…/repeatRule` | the rule | **new** | — |
| | `pausedUntil: Long?` | **new** | backfill `null` |
| `…/occurrences` | the whole entity | **new** — flat, one per *when*, holding `googleEventId`, the confirmation state and the outcome | — |
| `…/progressEntries` | edit history, soft delete, optional duration | **extended** | one nullable field, backfilled `null` |
| `…/completionFacts` | `minutes` + `difficulty` + `completedAt` | **new** — the sum the points total is taken over | derived from the `completedAt` already stored |
| `…/planDrafts` | one per goal, no expiry | **new** | — |
| `challenges/{id}` | `metricUnit = "points"` | **deleted** | — |
| | `type` (`ChallengeType`) | **deleted** | — |
| | `pendingMeasure` | **new** | — |
| `…/participants/{uid}` | `approvedChangeId` | **new** | — |
| | `score` | **server-owned**; client writes the fact | — |
| `publicProfiles/{uid}` | `level` | **deleted outright** | `resolvedLevel()` and `SERVER_FALLBACK` go with it |
| *(device, not Firestore)* | language, region, week start, skin, material, **the encrypted API key**, the quote cache, the daily-planning hour, waking hours | **per-device** | — |

**Migration posture, everywhere: additive with a readable half-way state.** Every new field is
nullable or has a backfill that reads identically on day one. This is what **retires** the map's
standing fear that `C18` would restructure every goal and task, and it is why `C5` could report **zero
new fields on `Goal`** for endless and maintenance goals.

**`firestore.rules`:** life areas needed no change (`users/{uid}/{document=**}` already covers them);
the participant row gains the rules file's **first field-level condition**; and **a subcollection is
not covered by its parent's `match`** — `challenges/{id}/participants/{uid}` needs its own block, and
that is exactly what lets a non-owner join something they cannot edit.

### 7.2 Code sites this spec changes or deletes

Named by the decisions, so the build session does not re-find them:

| Site | What |
|---|---|
| `TaskEstimate.kt:40` `fallbackMinutes` | **inverted** — derives minutes from points; `C3`/`C1` make points a product of minutes, and under `C1` §5 there is no `points` on the wire to run backwards from |
| `TaskEstimate.kt:74` `heuristicPoints` | **deleted** — `5 + 3×words`, a reward number from a word count |
| `TaskEstimate.kt:100` `looksLikeFallback` | **deleted**, with `SERVER_FALLBACK` — its own KDoc concedes it is *"evidence, not proof"* |
| `TaskRepositoryImpl.kt:120-127` | running accumulator over `task.points`, losing 30 for a 10 on an untick, with `.coerceAtLeast(0)` absorbing the drift |
| `TaskRepositoryImpl.kt:135-141` | becomes a **multi-document** transaction whose read set grows with linking and **which nothing bounds**; also a second, independent clamp |
| `GoalRepositoryImpl.kt:87` + `TaskRepositoryImpl.kt:135` | the **two client writers of `goal.currentValue`** |
| `GoalRepositoryImpl.kt:91` | the **fourth** clamp — made legal overshoot unreachable on the one screen a human writes to |
| `GoalDetailViewModel.kt:275` | the **third** clamp |
| `GoalDetailScreen.kt:154` / `:435` | a goal % and `+40 pts` on one screen with **no stated relationship** |
| `SummaryUseCase.kt:41-42` | **publishes** that contradiction into the shared §7 summary |
| `DashboardViewModel.kt:103` | a plain mean of `progressFraction`, broken once overshoot is legal |
| `RecommendationRepositoryImpl.kt:129` / `:95` | `estimatedPoints` / `points` — **deleted** |
| `RecommendationRepositoryImpl.kt:136`, `DashboardViewModel.kt:178` + the import path | **one contract, three enforcement sites, two layers** — a contract enforced in three places is not enforced, it is re-derived |
| `RecommendationRepositoryImpl.kt:175` | `< 0.34f` *needs attention* filter — **meaningless for a goal with no measure**, which is `C7`'s default |
| `TimeAllocationUseCase.kt:136` / `:204` | sum a **flat list**; must sum **leaves** |
| `TaskDuration.minutesOf` | a **non-leaf** returns its children's sum and **never falls back** |
| `GoogleTasksClient.kt:145` | parses Google's `due` into a field **no other line in the repo reads** — the import has been discarding a *when* for want of somewhere to put it |
| `Mappers.kt:29` | quietly resolves `LifeArea` vs `GoalCategory` — the duplication `C2` found |
| `Mappers.kt:176` `resolvedLevel()` | dead fallback that **can never fire** |
| `firestore.rules:53` | its own comment already names this as the same defect |
| `functions/src/index.ts:52-55` | throws the provider's **raw error body** into Cloud Logging |
| `GoalCategory.defaultColorHex` | replaced by the harmonised set; **light-mode-only today**, so a dark tone is owed per category |
| `ThemePaletteTest` | **owed an update** — `C12` replaces committed palette values |
| `functions/` | **has no test layer at all** — no `test/` dir, no `test` script. This spec creates the single most testable object on the map: a pure function from `(response, membership lists, language)` to `(validated fields, omitted fields)`, with `C11a`'s 248 recorded calls as fixtures |

---

## 8 · Out of scope

**Copied from `#12` rather than re-derived. These are closed and never graduate.**

- **Translating user-authored content.** The language picker switches text the *app* wrote. Goal
  titles, task titles and life-area names stay exactly as Ido typed them.
- **A paid model tier as the default path.** Follows directly from the permanent free-model
  constraint. Bring-your-own-key stays as a bonus, but **nothing may be specced that *requires* it**.

**Also not part of v0.3's product model** *(the UX/defect backlog is a separate track, not this map)*:
[#2](https://github.com/idomarhaim/Android_Final_Project/issues/2)–[#11](https://github.com/idomarhaim/Android_Final_Project/issues/11),
[#34](https://github.com/idomarhaim/Android_Final_Project/issues/34),
[#36](https://github.com/idomarhaim/Android_Final_Project/issues/36). Several are **narrowed or
deleted** by decisions above and are cited where that happens — notably #3 (deleted by `C20`), #9
(answered unconditionally by `C1`), #10 (unblocked by `C12`), #11 (specced by `C7`), #8 (widened by
`C9a`, triggered by `C11b`).

**Also excluded, and stated because a build session will reach for it:**

- **No `values-he` and no RTL support exists today.** It is *in scope* to add — but the migration off
  the deprecated `GoogleSignIn` stack is **not** (see [§9](#9--not-yet-specified-fog-on-12)).
- **Health Connect stays read-only.** Only `READ_STEPS`/`READ_SLEEP` are declared, and adding a write
  permission — even to seed test data — contradicts what the rationale screen promises on a surface the
  system itself shows.
- **Material You dynamic colour is never re-enabled.** It cannot coexist with the `AppSkin` palette,
  and after `C12` it cannot coexist with the material transform either.

---

## 9 · Not yet specified (fog on `#12`)

In-scope, real, and **deliberately not sharp enough to spec**. Recorded so a build session knows these
are *open*, not *forgotten*.

1. **Whether v0.3 owes an offline story.** **Half discharged, half now a ticket.** `A5` (completing a
   task offline) is discharged by `C20` as a side effect. `A6` — must the app *say* it is offline, and
   must a cached number look different from a live one — **is now
   [`C21` #43](https://github.com/idomarhaim/Android_Final_Project/issues/43), open and live.** See
   [§5.3](#53-offline-c20-and-c21-43--open).
2. **Whether the dashboard should answer *"what do I do now?"*** rather than *"how am I doing?"*
   (`A7`). Narrowed twice — `C10` put the quote + 2–3 goals needing attention on Home, and `C9a`'s
   **daily review on app open** is a second concrete answer that arrives with a reason the other blocks
   lack: because `C9a` stores no state and fires no miss-notification, **that review is the only place
   a missed or overdue thing surfaces at all**. What stays open is whether the *rest* of the dashboard
   reorients, and whether the review lives on the dashboard or inside the Calendar tab.
   ⚠️ `C20` flagged this as **possibly un-owned rather than un-sharp**: `C12` and `C9b` are both
   **closed**, and whether either actually answered it was **not read**. **The cheapest lead on this
   list.**
3. **Whether v0.3 carries the migration off the deprecated `GoogleSignIn` stack.** `GoogleSignIn` /
   `GoogleSignInOptions` are deprecated in favour of **Credential Manager** for authentication plus
   **`AuthorizationClient`** for authorization — which is also the modern way to ask for an extra OAuth
   scope. Nothing is broken today and it is **build work, not a product decision** — but `C9c`'s
   **incremental authorization** ([§2.7](#27-sync--google-holds-the-when-goalpilot-holds-what-happened-c9c-27))
   is *"a recurring interaction the user drives"*, which is precisely what `AuthorizationClient` exists
   for and what bolting another `GoogleSignIn` scope request onto the deprecated stack would have to
   fake **three times over**. So it is now the **main** consumer of the migration rather than an
   incidental one.
4. **Whether very long idleness should eventually retire a goal by itself.** `C19` settled what a goal
   with nothing due *shows* and deliberately did **not** settle this. `C4` points at **never**; an area
   whose list is nine `no next step` rows is its own kind of accusation, which points at **something**.
   **It cannot be phrased sharply until the `STARTING` offer has been lived with**, so it stays fog.

---

## 10 · Gaps, defects and open work

### The two genuine gaps in this spec

#### 10.1 · The measure proposal has no schema

`C7` specced an agent that **proposes a concrete measure** for an unmeasured goal, and handed it on in
so many words: *"This is a **fifth AI feature** for [`C11b` #30] to write an output format for."*

**`C11b` §3 writes four schemas — `estimate`, `plan`, `daily`, `classify` — and none of them is it.**
`C11b`'s own inventory reaches *"five"* by a **different** route: it counts `classify` as the fifth
feature (*"an AI feature the map never named"*), because `C2`'s task typing folded into `estimate` as
a field. **So the two tickets each say "fifth" about a different feature, and `C7`'s never got
written.**

`Observed:` `#14`'s resolution comment carries the handoff verbatim (one match); `#30`'s full
resolution body defines exactly four schemas and mentions `C7` only twice, neither time about a
measure proposal. `Inferred:` that this is an omission rather than a deliberate absorption — nothing
in `#30` says the measure proposal was considered and dropped. `Untested:` whether Ido would rather
fold it into `estimate` than give it its own call.

So the measure proposal is a specced **feature** with **no wire format and no stated fallback**, and
`C7` requires it to have one under
[§0.1](#01-the-free-model-rule-12-notes-scope-fixed-by-ido-2026-08-0708).

**This is not a decision to take here.** Per `#12`'s *no reopening closed decisions*, it is either a
**new ticket on `#12`** or Ido's call. **Recommendation:** a new child ticket — it is a format
question, which is `C11b`'s subject, and the shape is heavily constrained already (a prompt-declared
`measureKind` enum + a free word + a leading-indicator flag, with the non-AI fallback being *no
proposal at all*, exactly as `plan`'s is).

#### 10.2 · `GoalCategory`'s fate was routed to `C5`, and `C5` did not decide it

`C2` ruled `GoalCategory`'s fate **out of scope for itself** and **posted it to
[`C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21)** as a goal-model question,
carrying a third instance of a defect class `C15` had already filed twice: **hardcoded English labels
in `domain/model/`**. **`C5`'s resolution does not mention `GoalCategory`.**

What *is* decided and can be relied on:

- it is a **second axis** beside `LifeArea`, not a replacement (`C2`);
- its labels are **app speech**, so they are **translated**, and `C15b`'s content exemption does not
  reach them (`C11b` §8);
- its `defaultColorHex` values are **replaced** by `C12`'s harmonised set, and a **dark tone is owed
  per category** (`C9b`);
- `OTHER` is named as the tell that an escape hatch turns an enum into a smaller enum (`C2`).

What is **not** decided: whether the enum stays closed at ten, and where its labels live once they are
translated. **Same disposition as 10.1** — a note to Ido or a new ticket, not a quiet decision here.

#### 10.3 · Three settings this spec requires that do not exist

Each was **named by the ticket that needed it** and none was filed:

| Setting | Required by | Without it |
|---|---|---|
| **week start** | `C15` | Region owns first-day-of-week and has nowhere to store it |
| **daily planning hour** | `C9a` §6 | the nightly *plan-tomorrow* notification has no time |
| **waking hours** | `C9a` §6 | the backwards-computed deadline reminder cannot be clamped, and `C9b`'s load bar has no denominator for its 75% threshold |

These are **build work with an obvious shape**, not product decisions — but they are three fields on a
per-device settings store that does not exist yet, so they are named here rather than discovered
mid-build.

### Defects filed as spec lines, never fixed *(the map ships no code)*

Consolidated from every resolution. All are in [§7.2](#72-code-sites-this-spec-changes-or-deletes)
with their sites; these are the ones that are **live bugs today** rather than planned changes:

1. **`logProgress` writes the fact and *then* mutates the counter, in two non-atomic steps with
   nothing reconciling them** — a crash between them leaves `currentValue` **permanently wrong**.
   Found by `C20` at a site no ticket had named.
2. **The points accumulator drifts on untick** (`TaskRepositoryImpl.kt:120-127`), with
   `.coerceAtLeast(0)` silently absorbing it at the floor.
3. **Four clamps** make legal overshoot and falling progress unreachable —
   [§1.5](#15-progress-arithmetic-c3-c16-4-c17-c18).
4. **One membership contract, three enforcement sites, two layers.**
5. **The client substitutes plausible values and then tries to reconstruct which were real**
   (`looksLikeFallback`).
6. **`RecommendationRepositoryImpl.kt:175`'s `< 0.34f` *needs attention* filter is meaningless for a
   goal with no measure** — `C7`'s **default** case.
7. **An unmeasured goal is sent to the model as `progressPercent: 0`**, which reads as *"you have done
   nothing"* — a **discouraging false sentence**, not a missing one.
8. **`Recommendation` has no `author` and no `source` field**, so an attributed quote has nowhere to
   live.
9. **No prompt states an output language**, and **all ten date formatters are process-scoped `val`s**
   no switch can move.
10. **The dead `resolvedLevel()` fallback** and **`publicProfiles.level`**, a stored function of a field
    in the same document.
11. **The unfiled-task inbox costs one surface**: an unfiled task is **counted on the dashboard and
    listed on no screen**.
12. **`TaskRepositoryImpl.kt:135-141` becomes a multi-document transaction whose read set grows with
    the user's linking and which nothing bounds.**
13. **`GoogleTasksClient.kt:145` parses Google's `due` into a field nothing reads** — now it has
    somewhere to go.

### Named, not specced

- **A per-edge `weight`** — equal weighting is its `null` case, added later as a **feature, not a
  migration**.
- **Whether a milestone *shows* a measure.**
- **The fabricated-attribution prohibition has no validator.** The only unenforced clause in the AI
  spec, and it is why `C10` built a curated corpus instead of generating quotes.
- **A third Latin-script language reopens `C15b` §6.**

---

## 11 · Traceability

Every decision on `#12`, and where it lands here. **This table is the completeness check**: `#12`'s
27 closed decisions get 27 rows, and the **28th row is `C21`, which is open** and is the one section
this file cannot finish.

| Ticket | Decision | Section |
|---|---|---|
| [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19) | points-and-time, and who authors it | [1.4](#14-effort-and-outcome-are-two-quantities-and-points-are-neither-c3-18-c1-19), [3.5](#35-what-the-model-may-and-may-not-author) |
| [`C2` #20](https://github.com/idomarhaim/Android_Final_Project/issues/20) | task typing — `granularity` | [1.7](#17-task-typing--one-axis-two-values-c2-20) |
| [`C3` #18](https://github.com/idomarhaim/Android_Final_Project/issues/18) | effort vs outcome; `start` | [1.4](#14-effort-and-outcome-are-two-quantities-and-points-are-neither-c3-18-c1-19), [1.5](#15-progress-arithmetic-c3-c16-4-c17-c18) |
| [`C4` #13](https://github.com/idomarhaim/Android_Final_Project/issues/13) | the goal↔task ontology | [1.1](#11-there-is-one-kind-of-objective-and-goal-is-a-role-carried-by-an-edge-c4-13-c16-37), [0.7](#07-intrinsic-structure-needs-consent-instrumental-structure-does-not-c4-9) |
| [`C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21) | endless and maintenance goals | [1.6](#16-goal-kinds-are-views-not-a-stored-enum-c5-21) |
| [`C6` #22](https://github.com/idomarhaim/Android_Final_Project/issues/22) | LOG PROGRESS | [4.6](#46-log-progress-c6-22) |
| [`C7` #14](https://github.com/idomarhaim/Android_Final_Project/issues/14) | what a unit is | [1.3](#13-the-measure--optional-two-fields-on-the-object-c7-14) |
| [`C8` #24](https://github.com/idomarhaim/Android_Final_Project/issues/24) | AI-proposed plans | [3.7](#37-the-ai-proposed-plan-c8-24) |
| [`C9a` #25](https://github.com/idomarhaim/Android_Final_Project/issues/25) | what scheduling means | [2.1](#21-a-schedule-is-a-set-of-occurrences-the-task-carries-only-the-rule-c9a-25)–[2.5](#25-reminders-c9a-6) |
| [`C9b` #26](https://github.com/idomarhaim/Android_Final_Project/issues/26) | the in-app calendar surface | [4.2](#42-navigation-and-the-two-surfaces-c9b-26-c12), [4.3](#43-the-calendar-surface-c9b-26) |
| [`C9c` #27](https://github.com/idomarhaim/Android_Final_Project/issues/27) | sync direction and conflicts | [2.7](#27-sync--google-holds-the-when-goalpilot-holds-what-happened-c9c-27) |
| [`C9d` #17](https://github.com/idomarhaim/Android_Final_Project/issues/17) | scopes and the dedicated calendar | [2.6](#26-google-calendar--scope-and-consent-c9d-17-c9f-33) |
| [`C9e` #28](https://github.com/idomarhaim/Android_Final_Project/issues/28) | event lifecycle | [2.8](#28-event-lifecycle-c9e-28) |
| [`C9f` #33](https://github.com/idomarhaim/Android_Final_Project/issues/33) | the OAuth consent screen's state | [2.6](#26-google-calendar--scope-and-consent-c9d-17-c9f-33) |
| [`C10` #29](https://github.com/idomarhaim/Android_Final_Project/issues/29) | the daily quote feed | [3.8](#38-the-daily-line-and-the-quote-feed-c10-29) |
| [`C11a` #16](https://github.com/idomarhaim/Android_Final_Project/issues/16) | what the free model can do | [3.1](#31-what-was-measured-so-nothing-here-is-assumed-c11a-16) |
| [`C11b` #30](https://github.com/idomarhaim/Android_Final_Project/issues/30) | output formats and the failure contract | [3.2](#32-one-wide-call-per-feature-c11b-30)–[3.4](#34-the-failure-contract-c11b-4) |
| [`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31) | charts, materials, widgets | [4.1](#41-the-material-contract-c12-31-12-standing-preferences), [4.4](#44-charts-and-the-dashboard-c12-31), [4.5](#45-widgets-c12-r24-10) |
| [`C13` #32](https://github.com/idomarhaim/Android_Final_Project/issues/32) | bring-your-own key | [3.6](#36-bring-your-own-key-c13-32) |
| [`C14` #23](https://github.com/idomarhaim/Android_Final_Project/issues/23) | what a challenge scores from | [6](#6--challenges-c14-23) |
| [`C15` #15](https://github.com/idomarhaim/Android_Final_Project/issues/15) | in-app language switching | [5.1](#51-localization-c15-15-c15b-35) |
| [`C15b` #35](https://github.com/idomarhaim/Android_Final_Project/issues/35) | already-generated AI text on a switch | [5.1](#51-localization-c15-15-c15b-35) |
| [`C16` #37](https://github.com/idomarhaim/Android_Final_Project/issues/37) | how a milestone is modelled | [1.1](#11-there-is-one-kind-of-objective-and-goal-is-a-role-carried-by-an-edge-c4-13-c16-37), [1.2](#12-edges-c16-3-c17-38-c18-39) |
| [`C17` #38](https://github.com/idomarhaim/Android_Final_Project/issues/38) | many-to-many linkage | [1.2](#12-edges-c16-3-c17-38-c18-39), [1.5](#15-progress-arithmetic-c3-c16-4-c17-c18) |
| [`C18` #39](https://github.com/idomarhaim/Android_Final_Project/issues/39) | sub-tasks at arbitrary depth | [1.2](#12-edges-c16-3-c17-38-c18-39), [1.5](#15-progress-arithmetic-c3-c16-4-c17-c18) |
| [`C19` #41](https://github.com/idomarhaim/Android_Final_Project/issues/41) | per-area success and failure | [4.7](#47-per-life-area-success-and-failure-c19-41-e4) |
| [`C20` #42](https://github.com/idomarhaim/Android_Final_Project/issues/42) | who owns derived state | [5.2](#52-who-owns-a-derived-number-c20-42), [5.3](#53-offline-c20-and-c21-43--open) |
| [`C21` #43](https://github.com/idomarhaim/Android_Final_Project/issues/43) | **OPEN** — does v0.3 owe an offline story | [5.3](#53-offline-c20-and-c21-43--open) ⚠️ |

### Sources

- **`R1`–`R28`** — [`Product and UX Reviews/2026-08-06-brief-review.md`](../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md), the product/UX observation backlog.
- **`E1`–`E19`** — [`Product and UX Reviews/2026-08-09-entity-model-brief.md`](../Product%20and%20UX%20Reviews/2026-08-09-entity-model-brief.md), the entity definitions.
- **`A`/`D`/`U`** — the device pass, on the `#2`–`#11` backlog.
- **Design of record for the screens** — the five prototypes:
  [calendar](prototypes/2026-08-10-calendar-surface/README.md) ·
  [charts and widgets](prototypes/2026-08-10-charts-presentation/README.md) ·
  [the four materials](prototypes/2026-08-11-visual-styles/README.md) ·
  [log progress](prototypes/2026-08-13-log-progress/README.md) ·
  [area success/failure](prototypes/2026-08-13-area-success-failure/README.md).
- **Research assets** — [`docs/research/`](research/): the free-model format probe (248 calls), the
  Google Calendar scopes study, and the OAuth production test.

---

<sub>Written by the `product-v03-spec` session, 2026-08-15, from brief `sessions/product-v03-spec.md`
and the 27 closed decisions on [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).
**`#12` is not closed** — [`C21` #43](https://github.com/idomarhaim/Android_Final_Project/issues/43)
is open. Where this file and a resolution comment disagree, the resolution comment wins.</sub>
