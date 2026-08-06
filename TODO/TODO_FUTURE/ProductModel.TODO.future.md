# Product model — open decisions (wayfinder map material)

The part of the 2026-08-06 product/UX brief that **cannot be implemented as
written**, because each item's answer depends on answers not yet given. Source
text, numbered `R1`–`R28`:
[`Product and UX Reviews/2026-08-06-brief-review.md`](../../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md).

These are the intended tickets of a `/wayfinder` map — **decision tickets**, one
question each, resolved one per session until the way to a v0.3 product spec is
clear. Per `scale-adaptive-ceremony.md`, the map is the right tier here precisely
because the questions interlock: answer the points model and three others change
shape; answer it wrong and the "score mismatch" defect comes back as a design
flaw rather than a bug.

**Nothing here is worked on before the map exists** — that is what `future` means
in this repo. The actionable half is in
[`TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO_OPTIONAL/ProductReview.TODO.optional.md).

---

## The scoring & ontology knot

These four are one knot; none can be answered alone.

- [ ] **C1 · What is the points-and-time model, and who is allowed to author it?** (`R7`, `R9`, `R10`)
  AI-only point authorship, a fixed evaluation format, and re-scoring every task
  under a goal when one of them changes. Upstream of C2, C3 and U4.

- [ ] **C2 · Should tasks carry an AI-assigned type, and what is the taxonomy?** (`R11`)
  Ido's straw list: writing, thinking, analytical, creative, interpersonal,
  communication, physical activity, leisure, chores. Interacts with the existing
  life-area concept — decide whether this is a second axis or a replacement.

- [ ] **C3 · How do task points and goal percentage relate?** (`R12`)
  Currently they do not: `Task.points` is a currency, goal progress is
  `currentValue / targetValue` advanced by `Task.progressContribution`, which
  defaults to `1.0` and is invisible in the UI. Reported as a defect; it is this
  decision. See **D6** in the optional file.

- [ ] **C4 · What is the goal↔task ontology?** (`R4`, `R19`)
  Converting a goal into a task and back, goals nested inside goals, and the agent
  proposing a re-plan when a task has outgrown its size. Ido's own example — "buy
  flowers for Rachel" as a task under "make Rachel feel courted" — is the test case
  to design against, and he explicitly asked for a worked example of a goal inside
  a goal.

## Kinds of goals

- [ ] **C5 · How are endless and maintenance goals modelled?** (`R18`, `R20`)
  A goal that can hit 100% but decays without upkeep, and a recurring obligation
  with no end state. Ido's proposal — the percentage drops when the next instance
  comes due and recovers on completion — is a concrete candidate to evaluate, not
  a decision already taken. **Note:** the domain model has no recurrence field of
  any kind today (`A2`), so whatever is chosen is a schema change.

- [ ] **C6 · What may the user edit in LOG PROGRESS?** (`R14`)
  Being able to type your own percentage makes every other number in the app
  advisory. Depends on C1 and C3.

- [ ] **C7 · What is a unit?** (`R15`)
  `Goal.unit` is free text defaulted to `"%"` (`A3`), which is why the picker is
  unreadable. Decide the enumerated set, what each means for progress arithmetic,
  and how quantity units support increments — U6's fill buttons are blocked on this.

## Planning and the calendar

- [ ] **C8 · AI-proposed numbered task plans for a goal** (`R16`)
  The agent proposes staged tasks, the user keeps the ones they like, numbering
  re-flows. Depends on C4 (what a stage *is*) and C1 (what each stage is worth).

- [ ] **C9 · The planner, the in-app calendar, and Google Calendar sync** (`R17`)
  The largest single item in the brief: scheduling tasks with reminders at event
  and hour granularity, an in-app calendar, a dedicated GoalPilot calendar synced
  to Google, and events kept up to date as reality moves. Carries its own fog —
  OAuth scopes, sync direction and conflict resolution, what happens to a synced
  event when its task is deleted. Expect it to need several tickets of its own; it
  may well earn a second map once its first ticket is resolved.

## Motivation, presentation, and the model budget

- [ ] **C10 · The daily quote feed** (`R21`, `R22`)
  One practical, inspiring line per goal and per life area, in the home feed and
  inside each goal, sourced from real books and real people. Three sub-questions
  the ticket must not skip: where the corpus comes from and whether quoting it is
  licensed, whether an LLM may generate quotes it attributes to real figures
  (it must not — fabricated attribution is the failure mode here), and Ido's own
  open question: should the line track the *current numbered task*?

- [ ] **C11 · What can a free model be trusted to do, and what are the formats?** (`R23`)
  The cross-cutting constraint sitting under C1, C2, C8 and C10 — the default model
  is free-tier, so every AI feature above is really a question about what a small
  model can do reliably against a fixed output format. Arguably the ticket that
  should resolve first, because it prices all the others.

- [ ] **C12 · Charts and presentation strategy** (`R26`)
  Which chart types, whether the user chooses, and how they are arranged. The app
  already ships a donut, a simple bar, a stacked column and a progress ring — so
  this is a consolidation decision, not a greenfield one. Also governs what the
  widget pack (U5) can show.

- [ ] **C13 · Bring-your-own LLM API key** (`R28`)
  Marked "bonus" in the brief. A real decision: where a user key is stored, whether
  it ever reaches the Cloud Functions, which providers are supported, and what
  happens to the free-tier defaults when a key is present. Interacts with C11.

---

## Suggested destination for the map

> A v0.3 product spec for GoalPilot fixing the points-and-time model, the
> goal↔task ontology, goal kinds including maintenance, AI-assisted planning and
> the calendar surface, the motivation feed, presentation strategy, and the
> free-model constraint that underlies all of them.

Charting order to consider when the map session runs: **C11 first** (it prices
everything), then the C1–C4 knot, then C5–C7 which fall out of it, then C8–C9,
then C10, C12, C13. That is a proposal for the charting session to test, not a
decision — the map's own first act is naming the destination.
