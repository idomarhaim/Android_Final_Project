# KB candidates — `c4-goal-task-ontology`, 2026-08-10

Written per `rules/memory-promotion.md`. **Normal mode**, so this is a proposal:
nothing here has been ingested. Each entry stands alone — a reader with no access to
this session's transcript has everything needed to write the page.

**Folder listed before this session's first unit of work, and it was not empty — but
the listing this session first reported was wrong, and Ido caught it.** It named two
files; there are **five**, and all four of the pre-existing ones belong to sessions
that have **already released**, so none of them is "a live session's to drain" any
more:

| File | Entries | Owning session |
|---|---|---|
| `2026-08-08-c9d-calendar-scopes.md` | 3 | released |
| `2026-08-08-fix-task-completion-feedback.md` | 5 | released — its **#5** is **always-ask** (may supersede a standing KB claim) |
| `2026-08-09-c9f-consent-screen-state.md` | 5 | released — its **#1** is **always-ask** (arguably a `rules/` change) |
| `2026-08-09-entity-model-intake.md` | 3 | released — its **#1** is **always-ask** (arguably a `rules/` change) |
| `2026-08-10-c4-goal-task-ontology.md` *(this file)* | 5 | this session |

**21 entries across 5 files, at least 3 of them always-ask in both modes.** Reported,
not absorbed: draining another session's list means rewriting or deleting a file this
session does not own.

The board already knew: the `entity-model-intake` row says *"three pending files are
now unowned"* and the `c9f-consent-screen-state` row says *"the two files pending …
are now unowned"*. Both were readable at session start, and the first report
contradicted them without noticing — the same failure as candidate 3 below, one
directory instead of one folder of source documents.

---

## 1. A discriminator between two entity kinds can be a property of the *relationship*, not of the object — and object-property proxies for it may anti-correlate

**Claim.** When domain modelling produces the question *"what makes this thing an X
rather than a Y?"*, the candidate answers that come to mind first are almost always
properties of the **object** — is it measurable, is it big, does it end, how much
effort. Before settling on one, test whether the real line is a property of the
**relationship** between the user and the object. If it is, every object-property
answer is a proxy, and a proxy can be not merely weak but **inverted**: it can classify
the domain expert's own canonical examples backwards, in both directions at once.

The test that exposes it is cheap and should be run before adopting any discriminator:
**take the two examples the domain expert wrote down unprompted, and check whether the
proposed rule assigns them the labels they gave them.**

**Why.** The concrete case, and it is a clean one. A goal-tracking app needed to
separate a *goal* from a *task*. Four discriminators were proposed — measured vs done,
size/effort, endures vs completes, and "let a classifier decide" — all four properties
of the object. The user's own definition was neither: **a goal is what matters to you
in its own right; a task is a means, "not necessarily important to you in life in its
own right".** Intrinsic vs instrumental — a property of *his relationship to the thing*.

Run the test on his two examples and the best proxy inverts on both:

| Example | "measurable ⇒ goal" says | He says |
|---|---|---|
| "Understand real estate" | unmeasurable → task | **goal** |
| "Finish year 1 of the degree" | measurable → goal | **milestone, explicitly not a goal** |

Two examples, two wrong answers, opposite directions. Not a rule needing a threshold —
a different axis.

The consequence that generalises past the domain: **a relationship-property
discriminator is not merely hard for a classifier to infer, it is absent from the
input.** A title carries no information about whether the user wants the thing for its
own sake. So no amount of model capability closes the gap, which converts a modelling
observation into a hard product rule: *the system may act silently on instrumental
structure, but must ask before asserting an intrinsic edge.* Rejected framing:
"the model just needs better prompting or a bigger model" — the information is not in
the input at any model size.

**Destination.** Central KB — `kb/dev/`, a domain-modelling page. Adjacent to entry 2
below, which is the structural half of the same session's finding; likely the same page.
Not project-local: nothing here is about Android, Firestore or goal trackers.

**Anchors.** [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13)
(the resolution comment, §1); `Product and UX Reviews/2026-08-09-entity-model-brief.md`
`E5`, `E6`, `E7`, `E12`, `E14`, `E19`; `CHANGELOG/2026-08-10/c4-goal-task-ontology.md`.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 2. "Can an X contain an X?" is usually one object with two edges, not containment — model the role as an edge and promotion costs nothing

**Claim.** When a domain expert asks whether an entity can nest inside itself — a goal
inside a goal, a team inside a team, a category inside a category — check first whether
what they are describing is **one object holding two different relationships** rather
than a containment tree. The tell is a sentence of the form *"it can be an X of that
thing and also an X in its own right."* That is not nesting; it is two edges into the
same node, and only one of them is the "nesting" one.

Modelling it as an edge rather than a type has a concrete, measurable payoff:
**promotion and demotion become adding or dropping one edge, with no object created,
moved, or destroyed.** Model it as a type — two collections, or a `kind` field — and
every one of those transitions becomes a document migration over live data.

**Why.** The concrete case: a goal-tracking app whose user asked *"can a goal live
inside a goal? If so I need an example"* — and then supplied one that answered it
differently than the question implied. His example had "do a software-engineering
degree" as a goal in its own right **and** a milestone of "be worth $100M", with
nothing about the object differing between the two readings. What differed was the
edge: an **intrinsic** edge from the user ("I want this for itself") versus an
**instrumental** edge from another object ("I need this to get that"). So *goal* and
*milestone* are **roles carried by edges, not types carried by objects** — and the
nesting question dissolves rather than being answered.

Worth a page because the wrong model is the one you reach for first and it is expensive
to leave: a `kind: GOAL | MILESTONE` enum reads as obviously correct, satisfies every
example on day one, and only bills you later, once per promotion, as a migration over
data a real person depends on. The second reason it is worth writing down is that the
*question as asked* pointed at the wrong model — a faithful reading of "can a goal live
inside a goal?" produces a `parentGoalId`, which is the answer the user's own example
contradicts.

Rejected alternatives, both of which also satisfy the examples: `parentGoalId` on the
object (arbitrary-depth tree — creates cycles, and cannot express "goal in its own
right AND milestone of another" at all, because a node has one parent); and two
entities with a conversion operation (works, but pays a migration per conversion and
makes "both at once" unrepresentable).

**Destination.** Central KB — `kb/dev/`, the domain-modelling page from entry 1. These
two are the same finding seen from the semantic and structural sides.

**Anchors.** [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13)
(resolution §2–§3); [#37](https://github.com/idomarhaim/Android_Final_Project/issues/37)
(the ticket that now carries the edge-vs-type choice);
`Product and UX Reviews/2026-08-09-entity-model-brief.md` `E14`, `E16`, `E19`.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 3. A tracker does not know what its own inputs are — a session working a ticket must list the source folder and read recent commit subjects, not just the ticket

**Claim.** When work is organised on an issue tracker, a session that loads *the map,
the ticket, and the repo conventions* can still be working from a superseded premise,
because **the tracker has no way to know that a new source document arrived after it
was written.** Tickets are written from sources; sources keep arriving; nothing links
back. So the session-start read must include the **source folder** the tickets were
written from and the **recent commit subjects**, not only the tracker and the
governance files.

The cheap discriminator: if a repo has a folder of intake or source documents that
tickets *cite*, list it and compare against what the ticket cites. A source the ticket
does not mention is either irrelevant or the reason the ticket is wrong.

**Why.** The concrete case. A decision-map ticket ("what is the goal↔task ontology?")
was charted on 2026-08-08 from a source document dated 08-06. On 08-09 the user wrote a
**second** source document defining the same entities, and a separate session
transcribed and committed it. On 08-10 a session opened the ticket, read `AGENTS.md`,
the claim board, the KB-candidate folder, the map, the ticket, the 08-06 source and the
backlog file — a thorough read by any normal standard — built a question picker from
first principles, and put it to the user. He stopped it: the answer had been written
the day before, and **none of the four options offered was even on the right axis.**

What makes it worth a page is how visible the miss was. The two most recent commits at
session start were both from the intake session, and one subject line read: *"the
entity definitions arrive mid-map, and they answer C4 with a word no ticket used."*
The session read that log and did not act on it. So the failure is not "insufficient
context" — it is having **no rule that makes source-document arrival a session-start
check**, which is exactly the sort of gap that a per-ticket workflow hides, because
every individual session's read looks complete from inside.

Rejected fix: "the intake session should have commented on every affected ticket." It
deliberately did not, and was right not to — all four affected tickets were in live
sessions' `Owns` columns, and it recorded the routing in the committed transcription
instead. The duty belongs to the **reader**, not the writer, because the writer cannot
know which tickets will be worked next.

**Destination.** Central KB — `kb/dev/decision-map-charting.md`, as a section on what a
session working a map owes at start. That page already exists and already covers ticket
types and edge direction, so this is an **update in place**, not a new page.

**Anchors.** `CHANGELOG/2026-08-10/c4-goal-task-ontology.md` → *"The session was
overturned mid-flight"*; commits `e5916be` and `eb34522`;
`Product and UX Reviews/2026-08-09-entity-model-brief.md` (its own *"Where these items
land"* routing table);
[#13](https://github.com/idomarhaim/Android_Final_Project/issues/13).

**Supersedes.** Nothing, but it **extends** `kb/dev/decision-map-charting.md` — read
that page before writing, and add to it rather than contradicting it.

**Status.** Proposed, not ingested.

---

## 4. "The data already flows there" is an inference from the call site — a state can be counted everywhere and rendered nowhere

**Claim.** Before claiming that an app already handles some state, check the **render
site**, not the query. A repository call that fetches a superset (`observeTasks(null)`,
`SELECT *`, an unfiltered subscription) proves the data reaches the ViewModel; it proves
nothing about whether any screen shows it. The two are routinely confused because the
query is the memorable line of code and the aggregation that consumes it is not.

The specific shape to watch for: a screen that reduces a collection to **counters**
(`list.size`, `list.count { … }`) consumes every element while displaying none of them
individually. Grep for the query and you conclude the state is handled; read the
`uiState` builder and you find it is invisible.

**Why.** The concrete case, caught one step before it was written into a decision. A
product decision hinged on whether an app could leave a task unfiled (no parent goal)
without new UI. The evidence looked conclusive: the field was already nullable, the
dashboard already called `observeTasks(null)`, and the analytics chart already had an
"Unassigned" bucket that catches exactly this. Draft conclusion: *the inbox is free.*

Reading `DashboardViewModel.uiState` rather than trusting the call site showed the
dashboard turns the task list into `doneTasks`, `totalTasks` and `completedTasksLast7d`
— three integers — and renders no task list at all, while the goal-detail screen filters
by parent id. An unfiled task was therefore **counted on the dashboard, present in the
analytics denominator, and reachable from no screen.** The honest cost was one new
surface, not zero, and a decision was about to be recorded on the wrong number.

Worth a page because the wrong inference is *well-evidenced* — three independent true
facts pointing at a false conclusion — and because the fix is a five-line read that is
easy to skip precisely when the conclusion is convenient.

**Destination.** Central KB — `kb/dev/`. It generalises to any
query-language/UI-layer split (Redux selectors, React Query, LiveData, SQL views), not
just Compose or Firestore. Adjacent to any page on verifying claims about existing
behaviour.

**Anchors.** [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13)
(resolution §6, which states the corrected cost);
`app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt`
(the `uiState` builder — the query at the top, the three counters at the bottom);
`CHANGELOG/2026-08-10/c4-goal-task-ontology.md` → *"Two things found by checking rather
than assuming"*.

**Supersedes.** Nothing.

**Status.** Proposed, not ingested.

---

## 5. Resolving a decision map's root can leave the map *more* blocked — and that is a healthy outcome, not a regression

**Claim.** On a dependency-ordered decision map, closing the root ticket is expected to
release the tickets behind it. It can do the opposite: if the resolution introduces new
entities, the tickets it "unblocked" acquire fresh blockers from the same resolution and
stay shut. Do not read that as the map going backwards. A map whose root resolution
*deepens* it was under-specified before, and the new edges are the map catching up with
what is actually known.

Practical consequence for whoever reports on the work: **report the frontier as
re-queried, never as predicted.** "Closing this unblocks the four tickets it blocks" is
an inference from the `blocking` edge list and it is wrong whenever those tickets have
other blockers — including ones the same resolution just added.

**Why.** The concrete case: closing a map's root ontology ticket. It was blocking four
children. Afterwards exactly **one** was takeable — one of the other three was blocked
by a fourth ticket all along, and two acquired new blockers from the three tickets the
resolution itself filed. Net effect: the root closed, three tickets became *more*
blocked, and the frontier stayed the same size only because the three new tickets were
themselves unblocked.

The reason it deepened is the interesting part and it is general: the resolution
introduced a structural layer (an intermediate entity) and a cardinality change
(one-to-many becoming many-to-many). A downstream ticket asking *"do these two numbers
share a currency?"* genuinely cannot be answered until it knows whether a layer sits
between them and whether one child can feed two parents. Those blockers are true; their
absence beforehand was the defect.

**Destination.** Central KB — `kb/dev/decision-map-charting.md`, alongside entry 3. Same
page, and both are about the same thing: what the map's shape does and does not tell you.

**Anchors.** `CHANGELOG/2026-08-10/c4-goal-task-ontology.md` → *"What the graph actually
did"* (the four-row before/after table);
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12);
[#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) and
[#24](https://github.com/idomarhaim/Android_Final_Project/issues/24) (the two that
gained blockers).

**Supersedes.** Nothing; **extends** `kb/dev/decision-map-charting.md` with entry 3.

**Status.** Proposed, not ingested.
