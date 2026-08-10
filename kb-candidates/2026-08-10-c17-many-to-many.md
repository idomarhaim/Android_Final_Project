# KB candidates — `c17-many-to-many`, 2026-08-10

Written during `/wayfinder 12` (resolved
[#38](https://github.com/idomarhaim/Android_Final_Project/issues/38), `C17`).

---

## 1. When work serves several parents, divide the pooled and duplicate the owned

**Claim.** Once a one-to-many edge becomes many-to-many, every aggregate over it has to
be re-decided — and they do **not** all get the same answer. The test is one question
per quantity: **is it drawn from a single pool, or does each destination own its own?**
Pooled quantities (elapsed minutes, a points balance, anything conserved) are **divided**
across the edges; owned quantities (each objective's own progress number, a per-category
success count) are **duplicated** — counted in full on every edge. Getting this wrong in
either direction is invisible in the schema and shows up only as a chart nobody trusts:
divide an owned quantity and every destination reads understated; duplicate a pooled one
and the total exceeds reality.

The corollary is the useful part: **the same completion legitimately appears as *20
minutes* in one view and *one whole success* in another, on the same screen.** That
asymmetry looks like an inconsistency and is not one, so it needs to be written down
where the second reader will find it, or someone will "fix" it.

**Why.** GoalPilot's `E17` turned `Goal.lifeAreaId` and `Task.goalId` into collections.
The ticket framed all five consequences as one question ("where is it counted?"), which
implies one answer. Four of the five fell out of the pool/owned test in a line each,
and they split 2–2. What made this visible was refusing to answer "does completing a
shared task advance both goals *and* pay its points twice?" as a single question — they
look parallel and are not: progress is owned, points are pooled.

Rejected framings: *"mark one edge primary and account only there"* — pushes a ranking
decision onto the user for every multi-linked object and prints a zero for a destination
genuinely served; *"credit every edge in full, uniformly"* — makes the total exceed
reality and, worse, see entry 2.

**Destination.** `kb/dev/` — likely a new page on many-to-many aggregation. Check for
overlap with `dev/one-metric-and-its-mechanism.md` (ingested 2026-08-10 from `C16`),
which is the neighbouring finding: *the work below an objective is the mechanism of its
number, not a second opinion about it*. This entry is what happens when that mechanism
has several parents; they may want to be one page or two, and that is `/kb-ingest`'s call
on reading the existing text.

**Anchors.** [#38 resolution §2](https://github.com/idomarhaim/Android_Final_Project/issues/38#issuecomment-5243435565) ·
`CHANGELOG/2026-08-10/c17-many-to-many.md` · `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/TimeAllocationUseCase.kt:119-138`
(the two single hops it replaces).

**Supersedes.** Nothing. Extends `dev/one-metric-and-its-mechanism.md` rather than
contradicting it.

**Status.** 🟢 Ready to ingest.

---

## 2. Prefer the aggregation an autonomous agent cannot inflate

**Claim.** When an agent is permitted to create structure on the user's behalf, any
metric that **grows with the number of links** is a metric that agent will eventually
inflate — not maliciously, just by doing its job. So in a system with silent
agent-authored structure, "which aggregation is most flattering to each part" is the
wrong criterion and **"which aggregation is invariant under adding a link"** is the right
one. It is checkable before implementation: add one edge on paper, and see whether any
displayed number goes up without new work being done.

This is a **stronger** decider than the arithmetic objections that usually carry the
argument. In the concrete case, the textbook objection to duplication — *"the slices sum
to more than the time that actually passed"* — turned out to be nearly worthless, because
the chart already counted only completed tasks and substituted fallback durations for
unestimated ones, so it had never been an audit of elapsed time in the first place. The
argument that survived contact with the code was inflatability.

**Why.** GoalPilot's `C4` §9 lets the app add *instrumental* structure silently (and must
ask only before asserting what the user values). Under "credit every life area in full",
an AI-added second area raises that area's share of the time chart with no extra work
done — so the app's central chart would reward **re-filing** over **doing**, in an app
whose stated purpose is catching neglect. That is a purpose-level failure reachable from
a purely arithmetic choice, which is why the criterion deserves to be named rather than
rediscovered.

Rejected: *"just forbid the agent from creating those edges"* — it removes the feature
that made the structure worth having, and it fails silently the moment a future ticket
re-permits it. The invariance property holds regardless of who writes the edge, which is
the point.

**Destination.** `kb/dev/` — same page as entry 1, or its own. It generalises past
analytics to any agent-writable structure, so it may deserve separating.

**Anchors.** [#38 resolution §3](https://github.com/idomarhaim/Android_Final_Project/issues/38#issuecomment-5243435565) ·
`C4` §9 as recorded on [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).

**Supersedes.** Nothing.

**Status.** 🟢 Ready to ingest.
