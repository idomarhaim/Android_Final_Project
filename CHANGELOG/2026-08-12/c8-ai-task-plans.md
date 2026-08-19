# c8-ai-task-plans — claimed #24, and the re-derivation corrected a released session's own summary

> **Summary:** claimed #24, and the re-derivation corrected a released session's own summary

**Session:** `c8-ai-task-plans` · **Date:** 2026-08-12 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#24 · `C8`](https://github.com/idomarhaim/Android_Final_Project/issues/24) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked with the **map**, not a ticket, so the frontier pick was the agent's
and the reasoning is recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block — every open child of `#12` queried for `blocked_by`:

| Ticket | Blocked by | Verdict |
|---|---|---|
| `#24 · C8` | `#13`, `#19`, `#37` (all closed) | **frontier — claimed** |
| `#20 · C2` | `#19` (closed) | frontier — left |
| `#21 · C5` | `#13`, `#18` (both closed) | frontier — left |
| `#22 · C6` | `#19`, `#18` (both closed) | frontier — left |
| `#31 · C12` | `#18`, `#14` (both closed) | unblocked but **assigned and live** |
| `#30 · C11b` | `#20` **open**, `#24` **open** (+ `#19`, `#29` closed) | still blocked |
| `#35 · C15b` | `#24` **open** (+ `#29` closed) | still blocked |

Map size verified against GitHub: **25 children, 18 closed, 7 open**. **Eighth** derivation
of the day, and the frontier has **doubled from two to four** now that `#19` has closed.

## The correction the re-derivation bought

`c1-points-and-time`'s release note on the board states that closing `#19` unblocked
*"`#20`, `#22`, `#24`, and through `#24` both `#30` and `#35` … nothing on `#12` is blocked
any more — the whole remaining map is frontier."*

**The last clause is false.** `#30` is blocked by `#20` **and** `#24`; `#35` is blocked by
`#24`. All three blockers are **open**, so neither `#30` nor `#35` is takeable. `C1`
unblocked **three** tickets, not five.

Nothing in that session's note was edited — a released session's row is not this session's
to rewrite — so the correction lives in this session's own claiming banner. The board's
**Unclaimed-work** block is separately stale: it was queried before `#19` closed and still
lists five tickets as blocked behind it.

This is the whole argument for re-deriving rather than inheriting: the inherited number was
wrong in the direction that would have made a session claim a blocked ticket.

## Why `#24`, and why not the other three

1. **`#24 · C8` is disjoint and has the only leverage on the frontier.** Both blockers —
   `C4` [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13) and
   `C1` [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) — are closed
   **and released**, so its inputs are foreign state to read. It is also a blocker of both
   remaining blocked tickets: `#35` waits on `#24` **alone**, `#30` on `#20` + `#24`.
   Closing it frees `#35` outright and halves `#30`. No other frontier ticket frees
   anything.
2. **`#20 · C2` — declined: it would change a live session's inputs mid-flight.** `C2` asks
   whether an AI-assigned task type is a second axis **or a replacement for life areas**,
   and its body names *"it drives the time-allocation analytics that already ship"* as a
   candidate purpose. `c12-charts-presentation` is live on
   [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) deciding the chart
   set and dashboard arrangement, with a `C9b` hand-off about that exact chart. Re-cutting
   what the charts group by while they are being drawn is what this board has refused five
   times.
3. **`#22 · C6` — declined on prototype contention.** `C6` decides what a user may edit in a
   **screen**, which under the now-normative design standard is prototype-grade work. `c12`
   is already at revision 2 of a prototype burning Ido's attention. Every frontier ticket is
   HITL, so HITL-ness discriminates nothing — a second *screen* does.
4. **`#21 · C5` — declined on subject overlap, not on the ground four earlier sessions
   used.** Their objection (too near the live `#19`) has **expired**. What remains: `C5`'s
   decay mechanic changes what a goal's **percentage** means, and a goal's percentage is
   what `#31`'s charts render. It is also the heaviest ticket left — a Firestore schema
   change over live data, migration still fog.

## Claim

- **`#24` assigned to `idomarhaim` on GitHub before any work** — the assignee *is* the
  claim, per the skill.
- Row written on `SESSIONS.md` with three coupling points named on claiming: the `#12`
  *Decisions so far* commons (re-fetch immediately before appending; the race has fired
  twice for real), the released decisions `C8` inherits as inputs (`C4`, `C1`, `C16`,
  `C11a`), and the one live edge into `#31`, which is **posted as a comment, never taken**.

## 📥 KB candidates

`kb-candidates/` listed before the first unit of work. **Four files, each opened and its
*Destination* line read** rather than inherited from the board notes:

| File | Destination | Drainable here? |
|---|---|---|
| `2026-08-10-c1-points-and-time.md` | `rules/question-axis-naming.md` | ⛔ always-ask |
| `2026-08-10-c9e-event-lifecycle.md` | `rules/question-axis-naming.md` | ⛔ always-ask |
| `2026-08-10-c16-milestone-model.md` | `rules/agent-topology-and-model-routing.md` §5 | ⛔ always-ask |
| `2026-08-09-c9f-consent-screen-state.md` | `kb/dev/` — **parked by Ido** pending a `rules/` proposal | ⛔ always-ask |

All four are always-ask in **both** modes, and none is this session's. **`AUTO MODE` drains
nothing here.** The two `question-axis-naming.md` entries should be read together.

## 🧪 Tests

**None run, and none applicable.** This is a planning session: Markdown and GitHub issues
only, no code touched. The project's test layers (server unit, instrumented, UI) are
untouched by this unit of work.

## The grilling — three rounds, eight questions, and two answers from outside the option set

**Round 1** — the AI picks the plan's shape per goal · the plan is a **draft until confirmed** ·
and question 2 was **answered off-menu**: a proposed step has **three** exits, not two.
*"If the tasks were already carried out, the user can mark them as already done in the
proposal — they are not deleted, and the agent takes that into account in its planning."* Plus
two buttons rather than one policy — `Renumber`, and `explain delete` free-text feeding
`Adjust Plan`.

**Round 2** — question 1 **answered off-menu again**, and it found a **duplicate-commit
vector** none of the three options had: an already-done step pays like any completed task
*unless it duplicates a task already in the app*. Question 2: **"the AI decides what it keeps
and what it updates"** — none of the three offered scopes. Question 3: no flatten toggle.

**Round 3** — the draft persists **exactly where he left it**. Question 2 was **handed back**:
*"I couldn't fully follow you and what each option means — explain it simply and
schematically. And choose the solution that gives the highest standard and quality for the app
(and its purpose), UX/UI and the software. And if you think the solution you chose can be
improved — improve it."*

**That is the ❓ hand-back rule, and it was executed as written:** the question was **not
re-asked** in any form; the *"couldn't understand"* half was paid once, in the reply, as a
schematic explanation and **not** as a preamble to another picker; the decision was made by
**deriving**, not by picking the agent's own Recommended; and it is recorded as **the agent's**
in the resolution's §7, exactly as `C3`, `C14`, `C17` and `C1` each recorded earlier on this
map.

**The derivation went outside the option set, which is what the rule warns to expect.** All
three options treated a user-typed step as indivisible. It is **two things** — its *existence*
is the user's assertion (`C4` §1 puts intrinsic/instrumental entirely on his side) and its
*treatment* is the model's judgement (`C1` §1). *Frozen* would commit a task with no estimate,
which `C1` §5 forbids; *in the pot* would let a model delete what a human typed, which `C1`
§2.1's **unconditional** sticky rule forbids. So: never deleted or replaced, always estimated,
rewording only shown **beside** his words — plus the improvement he asked for, that a typed
step is the **highest-signal object in the draft** and `Adjust Plan` carries it as *context*,
letting the model propose the neighbours it now knows it missed.

## What was written

- **Resolution comment on [#24](https://github.com/idomarhaim/Android_Final_Project/issues/24#issuecomment-5268916168)**, ten sections; issue **closed**.
- **`#12`'s *Decisions so far* index line**, under the commons discipline: body fetched, then
  **re-fetched immediately before the write and `cmp`'d byte-for-byte** against the copy the
  line was built on — **unchanged, no race** — patch proved a **pure insertion before sending**
  (151 → 153 lines, **0 deleted**, 18 → 19 decision lines), then read back and diffed against
  what was sent: identical but for one trailing blank line GitHub appends. All 19 decisions
  verified present. **`C13`'s index gap is closed** — not by this session; `c9e` wrote it in
  `5e4af0f` while this ticket was being resolved.
- **Three hand-off comments, posted not decided** — [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30)
  (the plan call's schema, and the enum-over-judgement rule), [#35](https://github.com/idomarhaim/Android_Final_Project/issues/35)
  (a persisted draft is generated AI text that outlives a language switch — the longest-lived
  instance of that problem on the map), [#20](https://github.com/idomarhaim/Android_Final_Project/issues/20)
  (if a type is AI-assigned, the plan call is where it is assigned).

## Frontier after closing, re-derived rather than predicted

`#35` **unblocked outright**, `#30` now blocked by `#20` **alone** — exactly as the claim
predicted. New frontier (open · unblocked · unassigned) = **`#20 · C2`, `#21 · C5`,
`#35 · C15b`**. `#31 · C12` and `#22 · C6` are unblocked but **assigned and live** — `#22` was
claimed by a sibling **during** this session (`CHANGELOG/2026-08-11/c6-log-progress.md`), which
is the second reason nothing of theirs was touched.

## Issues

- **deviation — the changelog and candidate files were first written under `2026-08-10`; today
  is `2026-08-12`.** Cause: the folder was chosen by matching the sibling files this map's
  earlier sessions left behind, rather than from the date. Caught when the JARVIS board showed
  `2026-08-12` rows. **Fixed** — `git mv` to `CHANGELOG/2026-08-12/`, candidate renamed to
  `kb-candidates/2026-08-12-c8-ai-task-plans.md`, `SESSIONS.md` row corrected. The claim commit
  `752e6ac` carries the wrong path in history; the move is recorded here rather than rewritten,
  since it is already pushed.
- **defect (pre-existing, not fixed)** — `c1-points-and-time`'s release note and its `#12`
  index line both state that closing `#19` left *"every remaining ticket on this map on the
  frontier"*. It was false when written: `#30` and `#35` were still blocked behind `#20`/`#24`.
  Not corrected — a released session's own line is not this session's to rewrite.
- **defect (pre-existing, not fixed)** — the board's *Unclaimed work* block is stale, listing
  five tickets as blocked behind `#19`.

## 🧪 Tests

**None run, and none applicable.** Planning session: Markdown and GitHub issues only, no code
touched. The project's test layers (server unit, instrumented, UI) are untouched.

## 📥 KB candidates

[`kb-candidates/2026-08-12-c8-ai-task-plans.md`](../../kb-candidates/2026-08-12-c8-ai-task-plans.md)
*(new)* — **two entries, one drained and one parked.**

1. **The framing tell can fire with the axis right and the enumeration short** — ⛔ always-ask,
   destination `rules/question-axis-naming.md`. Round 1's question 2 named the correct axis and
   still missed a member of the action set it quantified over, because *"already done, in a
   proposal not yet committed"* exists nowhere in the code. Neither the *granularity* nor the
   *framing* row's remedy covers it.
2. **Don't buy a global judgement you can derive from per-item enums** — ✅ ordinary,
   `AUTO MODE`-eligible, destination `kb/dev/enum-and-label.md`.
