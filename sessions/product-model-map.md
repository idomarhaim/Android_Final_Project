---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: ready
created: 2026-08-06
---

# Chart the GoalPilot v0.3 product-model map

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`, and it must stay that way. This is a **HITL** session: the
map's first two steps are grilling Ido about the destination and the frontier, and
an agent that answers its own grilling questions has broken the skill. `AUTO MODE`
is wrong here.

**Read first** — [`AGENTS.md`](../AGENTS.md), then
[`TODO/TODO_FUTURE/ProductModel.TODO.future.md`](../TODO/TODO_FUTURE/ProductModel.TODO.future.md)
(the 13 candidate decisions, `C1`–`C13`, with a proposed destination and charting
order), then
[`Product and UX Reviews/2026-08-06-brief-review.md`](../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md)
(the source text those cite), then the wayfinder skill itself at
`C:\Users\namei\.claude\skills\wayfinder\SKILL.md`.

**Task** — run `/wayfinder` in **chart** mode over the 13 product-model decisions
and stop. Concretely: grill Ido to name the destination, grill again breadth-first
to map the frontier, create the map issue (label `wayfinder:map`) with Destination,
Notes, an empty Decisions-so-far and the fog written into *Not yet specified*, then
create the tickets you can specify now as sub-issues and wire the blocking edges in
a second pass.

Ido chose the **wide** destination — all 13, not just the scoring knot. The draft
in `ProductModel.TODO.future.md` is a **proposal to test, not a decision already
taken**; the map's own first act is naming the destination, and the grilling may
well redraw it. Three things the charting should press on:

- **`C11` may belong first.** What a free-tier model can reliably do against a
  fixed output format prices `C1`, `C2`, `C8` and `C10`. If that holds, it is the
  root of the blocking graph, not one ticket among thirteen.
- **`C9` (planner + in-app calendar + Google Calendar sync) is probably its own
  map.** It carries independent fog — OAuth scopes, sync direction, conflict
  resolution, what happens to a synced event when its task is deleted. Expect to
  chart one ticket that decides whether it stays in this map or spawns a second.
- **`C5` is a schema change, not a feature.** Neither `Task` nor `Goal` has any
  recurrence field, so maintenance/endless goals mean a Firestore migration. Price
  it that way.

This repo has **no GitHub issues at all** — the map will be issue #1. `gh` is
authenticated with `repo` scope for `idomarhaim/Android_Final_Project`.

**Carries over**

- The 13 decisions, their dependencies, and the proposed destination —
  [`TODO/TODO_FUTURE/ProductModel.TODO.future.md`](../TODO/TODO_FUTURE/ProductModel.TODO.future.md).
- Why these were split from the buildable half —
  [`CHANGELOG/2026-08-06/product-review.md`](../CHANGELOG/2026-08-06/product-review.md).
- The grounded facts the tickets rest on, so they need not be re-derived:
  `Task.points` is independent of goal progress, which moves by
  `Task.progressContribution` (default `1.0`, invisible in the UI); `Goal.unit` is
  free-text defaulted to `"%"`; there is no recurrence field anywhere in
  `domain/model/`.
- The ceremony rule that put this work on a map at all —
  `C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md`.

**Out of scope**

- **Resolving any ticket.** Charting is one session's work; the skill says so
  explicitly. Resolve tickets one per session afterwards.
- Everything in
  [`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md)
  — the defects and `U1`–`U6` are ordinary issues, filed by
  [`product-device-pass`](product-device-pass.md). If a `U` item turns out to be
  blocked on a `C` decision (`U6` already is, on `C7`), note the dependency; do not
  pull it onto the map.
- Any code change at all.

**Exit** — a `wayfinder:map` issue exists with its destination named by Ido;
specifiable tickets exist as sub-issues with blocking edges wired and
`wayfinder:<type>` labels; the fog is written into *Not yet specified*;
`ProductModel.TODO.future.md` is annotated with the map's issue number (the funnel
is one-way — the map, not the TODO file, is the source of truth from then on);
`CHANGELOG/<day>/<this-session>.md` written with an explicit "no test layer
applies" note; claim released on [`SESSIONS.md`](../SESSIONS.md); commit on
approval.
