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

The actionable half is in
[`TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO_OPTIONAL/ProductReview.TODO.optional.md).

---

> ## 🗺️ Graduated to a map on 2026-08-08 — this file is no longer the source of truth
>
> Every decision below is now a ticket on
> **[#12 · GoalPilot v0.3 product model — wayfinder map](https://github.com/idomarhaim/Android_Final_Project/issues/12)**.
> **The funnel is one-way**: from here on the map and its children hold the state,
> and this file is the historical record of how the questions were first written
> down. Do not tick a box here, and do not add a `C15`, `C16`… here — add a ticket
> to the map. What this file is still good for is the trail back to `R1`–`R28`.
>
> **What Ido fixed while charting**, because it is not derivable from the text below:
> the destination is a written **v0.3 product spec** (`docs/PRODUCT_v0.3.md`), not
> merely a set of answers; the audience is **one real user, Ido, daily**; the free
> model is a **permanent** constraint, so every AI feature is specced with a non-AI
> fallback beside it; **`C9` is fully in scope** and was split into five tickets
> rather than deferred to a second map; and **localization is in scope** as an
> in-app language picker — a requirement that appears nowhere in `R1`–`R28`.
>
> | Decision | Ticket | Decision | Ticket |
> |---|---|---|---|
> | `C1` points-and-time model | [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) | `C9b` in-app calendar | [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) |
> | `C2` task types | [#20](https://github.com/idomarhaim/Android_Final_Project/issues/20) | `C9c` sync direction | [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) |
> | `C3` points ↔ progress | [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) | `C9d` scopes & consent | [#17](https://github.com/idomarhaim/Android_Final_Project/issues/17) ⭐ |
> | `C4` goal↔task ontology | [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13) ⭐ | `C9e` event lifecycle | [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) |
> | `C5` maintenance goals | [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21) | `C10` daily quote feed | [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29) |
> | `C6` LOG PROGRESS editing | [#22](https://github.com/idomarhaim/Android_Final_Project/issues/22) | `C11a` free-model probe | [#16](https://github.com/idomarhaim/Android_Final_Project/issues/16) ⭐ |
> | `C7` what is a unit | [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14) ⭐ | `C11b` output formats | [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30) |
> | `C8` numbered plans | [#24](https://github.com/idomarhaim/Android_Final_Project/issues/24) | `C12` charts | [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) |
> | `C9a` scheduling model | [#25](https://github.com/idomarhaim/Android_Final_Project/issues/25) | `C13` BYO API key | [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) |
> | `C14` challenge scoring | [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23) | `C15` localization *(new)* | [#15](https://github.com/idomarhaim/Android_Final_Project/issues/15) ⭐ |
>
> ⭐ = **on the frontier**: open, unblocked, unclaimed. `C11a` and `C9d` are AFK and
> need nobody in the room; the other three are grilling tickets and need Ido.

---

## The scoring & ontology knot

These four are one knot; none can be answered alone.

- [x] **C1 · What is the points-and-time model, and who is allowed to author it?** (`R7`, `R9`, `R10`)
  AI-only point authorship, a fixed evaluation format, and re-scoring every task
  under a goal when one of them changes. Upstream of C2, C3 and U4.
  **✅ DECIDED** — [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) closed
  2026-08-10; the answer is `docs/PRODUCT_v0.3.md` §1.4.
  **⛔ NOT BUILT, AND UNOWNED** *(audited 2026-08-20)*. The checkbox above is ticked for the
  **decision**, which is what this file tracks — and that is exactly the ambiguity that let four
  later artifacts defer *implementation* here as though a ticket were carrying it. There is no
  such ticket: the `difficulty` enum, the `5..50` cap deletion, `heuristicPoints`' retirement,
  the `completionFacts` collection and §1.5's `goalEdges` are **all absent at `HEAD`**, 6 open
  issues hold none of them, and no brief in `sessions/` names them. If it is wanted in v0.3 it
  needs its own issue — Ido's call, deliberately not filed. Full clause-by-clause audit in the
  box at the top of §1.4.

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

## Challenges

- [ ] **C14 · What does a challenge score from?** (`R1`) — *added 2026-08-07, moved
  here from `D1` in [`TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO_OPTIONAL/ProductReview.TODO.optional.md)
  on Ido's re-assignment.* Reported as a defect ("a shared challenge does not sync
  with my tasks or Health Connect") and reproduced on the device, but it is not a
  wiring that broke — it is one that was **never specified**. `ChallengeParticipant.score`
  has exactly one writer in the codebase, the manual "Report score" dialog;
  `ChallengeType` (`RUNNING`, `STEPS`, `SLEEP`, `WORKOUTS`) is purely presentational
  and nothing branches on it to source a score; `SyncHealthDataUseCase` writes against
  a `Goal` and never mentions challenges. So Ido's own "August Steps Race" reads
  `#2 · 0 steps` while his steps flow into goals. The decision is whether a challenge
  scores from a goal's progress, a task count, a raw health metric, or a free number
  — and it cannot be taken without **C7**, because `Challenge.metricUnit` is free text
  with the identical disease as `Goal.unit` (`A3`). Full evidence stays under `D1`.

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

## Suggested destination for the map — *tested 2026-08-08, and partly overturned*

The proposal was:

> A v0.3 product spec for GoalPilot fixing the points-and-time model, the
> goal↔task ontology, goal kinds including maintenance, AI-assisted planning and
> the calendar surface, the motivation feed, presentation strategy, and the
> free-model constraint that underlies all of them.

**The destination survived; the charting order did not.** Recorded because a
proposal that was wrong is worth more to the next reader than one quietly
replaced:

- **`C11` was proposed as the root that prices everything.** It is not one
  question. *What can the free model do* is measurable today; *what are the
  formats* cannot be written before the features it serves exist — you cannot test
  a format nobody has designed yet. Split into [#16](https://github.com/idomarhaim/Android_Final_Project/issues/16)
  (unblocked, AFK, on the frontier) and [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30)
  (blocked on `C1`, `C2`, `C8`, `C10` — the most-blocked ticket on the map).
- **The "C1–C4 knot" was ordered, not merged.** Four questions that "none can be
  answered alone" is usually a signal they are one ticket; here it was a signal the
  chain had never been drawn. It is `C4` → `C3` → `C1` → `C2`: what a task and a
  goal *are* precedes whether their numbers join, which precedes who authors those
  numbers, which precedes whether a type feeds the authoring. `C4` is the map's
  true root, not `C11`.
- **`C7` turned out to be unblocked**, not a consequence of the knot. It is
  self-contained, and it is the fastest route to unblocking already-filed work
  ([#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)) and
  [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23).
- **`C9` was expected to earn its own map.** Ido ruled it fully in scope instead,
  so it is five tickets here — and `C9d` (Google's scopes and consent) is AFK
  research that was takeable from the moment the map existed.
- **Localization was missing entirely.** It is in none of `R1`–`R28`; it came from
  the device pass finding `A1` and Ido's answer during charting, and it is now
  [#15](https://github.com/idomarhaim/Android_Final_Project/issues/15) — with a
  real edge into the free-model probe, because a small model's Hebrew is not its
  English.
