# `c9a-schedule-a-task` — a schedule is not a field on a task, it is a set of occurrences

> **Summary:** a schedule is not a field on a task, it is a set of occurrences

**Session:** `c9a-schedule-a-task` · **Invocation:** `/wayfinder 12` (bare — no ticket
named) · **Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL
throughout) · 2026-08-10.

One ticket resolved, which is the skill's limit. **No code was touched** — this map
ships no code, and that held: eleven files under `app/` and `functions/` were read and
none was edited.

## What changed

| | |
|---|---|
| Resolved | [#25 · `C9a` What does it mean to schedule a task?](https://github.com/idomarhaim/Android_Final_Project/issues/25) — closed, with the full resolution plus a reconciliation addendum as comments |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*; the **notification-substrate fog cleared and removed**; the Firestore-migration, `E4` success/failure and `A7` dashboard patches each narrowed or widened |
| Tickets created | **none** — the second resolution on this map where every hand-off landed on a ticket that already existed |
| Hand-offs commented | [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) `C3` · [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21) `C5` · [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) `C9b` · [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) `C9c` · [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) `C9e` · [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) `C12` · [#8](https://github.com/idomarhaim/Android_Final_Project/issues/8) |
| Unblocked | [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) and [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) — **the calendar half of the map is now open.** [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) is still behind `#27` |
| Frontier now | [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) `C9b` · [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) `C9c` · [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) `C17` · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` — re-derived out of GitHub after closing, not predicted. **25 children, 10 closed** |

## The decision

**`R17` reads as one feature and is six decisions.** The load-bearing one is in none of
its words: *how many independent `when`s one piece of work may have, and what remembers
the outcome of each.*

1. **A rule on the task, occurrences beside it.** `Task` never carries a date. A repeat
   rule is optional; each occurrence is its own record with its own outcome. A one-off
   task is this model with an empty rule, so "a date field on the task" is not a third
   option — it is the degenerate case.
2. **Four rungs** — `ALL_DAY` · `DEADLINE` · `BLOCK` · `SPAN` — discriminated by **what a
   miss means**, not by precision.
3. **Flat, not nested.** A `SPAN` does not contain its blocks.
4. **Silent unless it takes a slot.** The agent sets all-day, deadline and span alone; a
   block needs confirmation.
5. **Temporal state derived, never stored**, following `Challenge.phaseAt(now)`.
6. **One reminder per occurrence**, timed per rung, with the deadline's computed backwards
   from how long the task takes.

## Why the two cheaper shapes lose, in Ido's own examples

A date **on** the task gives one `when`, so `R18`'s *"buy flowers for Rachel every two
weeks"* becomes **26 duplicate documents a year**, and a missed instance has nowhere to
live — `Task.isDone` is a latching `Boolean` that never un-sets.

A **rule alone**, with dates computed and nothing stored, cannot hold a moved instance, a
skip, or a Google event id — which is *"it updates the calendar events as needed,
following what actually happens in life"*, `R17`'s own closing clause.

**What only the combination buys:** the question *"this occurrence, or all future ones?"*
A field-only model always answers *just this one*; a rule-only model always answers *all
of them*. Neither is right every time, and Ido already knows the interaction from Google
Calendar.

## The question nobody had asked, and the fact that answered each one

Every decision here was forced by something looked up, not by taste:

| Decision | The fact that forced it |
|---|---|
| Occurrences, not fields | `Task` has **no date at all**, and `GoogleTasksClient.kt:145` parses Google's `due` into a field **no other line in the repo reads** — the import has been discarding a `when` for want of somewhere to put it |
| A block's end is free | `TaskDuration.minutesOf(task)` **never returns null** — stored estimate, else `points × 3`, clamped 5–480 |
| Blocks need confirmation | `C9d` chose `calendar.app.created`, so the app is **blind to every other calendar Ido owns** and cannot see the 09:00 meeting |
| The rung is not derivable from task type | `C1` (#19) and `C2` (#20) are **both blocked**, so #25's third candidate decider was unavailable, not rejected |
| A confidence threshold buys nothing | `C4` found `TaskClassification.confidence` is written and **never read** |
| Batch review sheet, not per-block prompts | The idiom **already ships** — `DashboardScreen.kt:377-440`, the Google Tasks import dialog |
| Derive state, never store it | `Challenge.phaseAt(now)` already does exactly this, recomputed per render, with no sweep and nothing that can go stale |
| The nightly reminder needs no server job | There is **no `WorkManager`, no `AlarmManager` and no FCM**, so *every* reminder needs local scheduling and the nightly one rides it |
| All-day stores local midnight | `ChallengeDates.kt` documents the trap already paid for once: the Material date picker reports **UTC midnight** |

## Ido overturned the recommendation once, and one objection was simply wrong

This session recommended **three** rungs and argued the multi-day span away on two
grounds. Ido chose **four**, and checking afterwards showed:

- **"The 480-minute ceiling forbids it" — false.** `MAX_MINUTES` is applied only to
  `estimatedMinutes`, via `sanitize` and `minutesOf`. That measures **effort**. A
  five-day span is elapsed time, not 7,200 minutes of work, so the clamp never touches
  it. The objection was withdrawn in the resolution rather than quietly dropped.
- **"Question 1 already expresses it" — true but beside the point.** It does, as several
  block occurrences; the fourth rung says the *period itself* is a thing Ido wants to name.

**And the objection being wrong is what surfaced the defect risk.** Because a span is
elapsed time, `TimeAllocationUseCase.kt:163` — which builds the "what share of my life
went here" chart by summing `estimatedMinutes` — would let **one week-long renovation
swamp every other life area at once**. Spec line handed to `C12` (#31): spans contribute
nothing to time-allocation arithmetic.

## Ido asked three times for it simpler, and three times for it improved

Three of the six questions came back with the same instruction: *explain it plainly and
schematically, choose the answer that gives the highest standard for the app, and if you
can improve the answer you chose, improve it.* That produced material the pickers had not
offered, and it is most of the resolution's substance:

**On who schedules —** confirmation is per **plan**, not per block (one sheet, reusing the
shipped import dialog); an agent-placed block is written **`PROVISIONAL`**, drawn dashed
and **not synced to Google until confirmed**; and an unconfirmed block **`EXPIRED`s
silently, counting for nothing**. That last rule matters more than it looks: without it an
over-eager agent **manufactures failures** against Ido, and they flow straight into the
`E4` per-area failure picture. **You cannot fail to do something you never agreed to.**

**On what a miss means —** the notification `R17` actually asked for is a **reminder**
(before), not a miss-alert (after), which **dissolves the only real argument for a stored
status**; `OVERDUE` is split from `MISSED`, because an assignment handed in late is late
and **not failed**; and misses meet Ido **once, in a daily review on app open** rather
than as a push telling him he failed.

**On reminders —** the computed deadline time is **clamped to waking hours and says why it
moved** (*"due at 06:00 and it takes about 4 hours — worth starting tonight"*); `OVERDUE`
**keeps** reminding while `MISSED` goes silent, which is what makes question 5's split
load-bearing rather than decorative; and a reminder **re-checks at fire time** whether it
is still needed — free, precisely because nothing is stored.

**Ido's own addition, beyond anything offered:** a nightly *plan-tomorrow* notification.

## The model tying itself together

The three parts are not independent, and that is the finding worth keeping. **Not storing
status in question 5 is what makes reminders trustworthy in question 6** — because state
is computed, a reminder can simply ask *"is this still open?"* at the moment it fires, for
nothing. A stored-status design would have had to invalidate scheduled notifications on
every completion.

## What this ticket deliberately did not decide

- **Whether a miss moves goal progress, or triggers maintenance decay.** `C3` (#18) and
  `C5` (#21) own that arithmetic and both are blocked. `C9a` hands them a clean fact to
  read and stays out of what they do with it.
- **How a `DEADLINE` maps onto a Google Calendar event.** Google has two shapes and a
  deadline is neither; three of the four rungs map 1:1 and this one is `C9c`'s decision,
  because whatever it picks is what appears in Ido's real calendar.
- **Occurrence containment.** Deferred to `C16` (#37) and `C18` (#39) rather than
  answered twice in two vocabularies.

**Named, not specced** (Ido's standing pattern from `C7`): `Task.isDone` is the wrong
field for a scheduled task, since it latches while the occurrence carries the truth (→
#18); and the app has **no daily-planning-hour and no waking-hours setting**, which §6
needs — the same shape as `C15`'s finding that a week-start setting does not exist.

## `C5` inherits rather than agrees, and that was Ido's call

#25's body required that this schema answer and `C5`'s **agree**. `C5` (#21) is blocked
behind `C3` (#18) and could not answer, so the choice was: bind a blocked ticket, or leave
the entire calendar half of the map shut. Ido chose to bind it, knowingly — the option was
put to him explicitly as *"Defer — don't bind C5"* and rejected.

Re-checked after three siblings closed mid-session: `#18`'s blocker is now `#38` alone,
`#37` having closed — so `#21` is **still blocked** and the inheritance still stands.

## The session was overtaken by four siblings, and one of them vindicated a decision

Between the board being read and the resolution being written, **`c7-what-is-a-unit`,
`c10-quote-feed`, `c16-milestone-model` and `c13-byo-api-key` all committed and released.**
Everything below was found by re-querying rather than by assuming:

- **`#37` was a live claim, not a pointer.** The session opened by reporting `#37` as
  *assigned 15 minutes ago with no board row*, could not tell whether that was a claim or
  Ido pointing at it, and **asked instead of guessing**. Ido chose `#25`. `#37` was then
  resolved by `c16-milestone-model` — so leaving it alone was correct, and taking it would
  have been a two-session collision on one issue body.
- **`C16` agreed with §3.** §3 deferred occurrence containment to `#37` as a *bet*. `#37`
  landed on **one edge stored on the child, repeated at every depth** — the same instinct,
  containment carried by an edge with storage kept flat. An addendum was posted converting
  the forward reference into a settled link rather than leaving it dangling.
- **Every blocked-state claim in the resolution was re-verified after the fact** and all
  held: `#18` still blocked (by `#38` now), `#21` behind it, `#19` by `#39`+`#18`, `#20` by
  `#19`, `#30` by `#24`+`#20`+`#19`, `#35` by `#24`.
- **`c7`'s uncommitted tree resolved itself.** The session opened by reporting five paths
  carrying `c7`'s uncommitted release-and-drain edits as an open issue; `c7` committed them
  in `7aedf9f` while this session worked. Reported, not silently dropped.

## One gap found in the map, and deliberately not fixed here

**`C13` (#32) is closed, with a resolution comment, and has no line in the map's
*Decisions so far* index.** `c13-byo-api-key` closed the ticket and released its board row
(`f7c0c63`) without adding it. The map's own contract is that it is an **index** — one
line per closed ticket — so a closed ticket with no line is invisible to the next session
reading the map at low resolution.

**Not fixed by this session**, and the reason is the one the repo has just been writing
down: an index line written *for* another session is a **report, not a claim**, and
summarising a decision this session did not take risks putting a wrong gist into the
canonical index. Raised for Ido instead.

## 🧪 Tests

**No suite was run, and none is applicable.** No Kotlin, Gradle, `firestore.rules` or
Cloud Functions file was created or modified — the entire output is GitHub issue text,
this changelog, a KB-candidate file and two `SESSIONS.md` edits. The layers this project
has (JVM unit, instrumented, `firestore-tests/`) all test code that was not touched.

**Verification was structural**, in the shape the previous map sessions established:

- The map body was **fetched and hashed before the first edit** (`672b45e1…`) and
  **re-fetched and byte-compared immediately before writing** — identical, no drift —
  because `#12` carries no lease and four siblings were live during the session.
- The written body was then **read back and compared against what was sent**: one trailing
  newline added by GitHub, BOM intact, **no textual diff**, and the semantic checks passed
  (9 decision lines where there were 8, notification fog absent, `#25` linked once).
- **All seven hand-off comments were verified as landed** by re-reading comment counts —
  `c7` recorded that a previous attempt posted nothing and reported no error, so counts
  were checked rather than trusted. `#25`'s own three comments likewise.
- **The frontier was re-derived out of GitHub after closing**, not predicted: `#26` and
  `#27` became takeable, `#28` did not (still behind `#27`). 25 children, 10 closed.
- The five map-body edits were applied by a script with an **`assert` on every anchor**, so
  a silently-missed replacement would have failed loudly rather than shipping a
  half-updated map.

## Singletons and live state

**No singleton was taken at all.** No `#gradle-daemon`, neither AVD, no `adb`, no GROQ
call, and **live `goalpilot-56e30` was never contacted** — no read, no write. Nothing was
built, installed or deployed.

`SESSIONS.md` was **claimed before the first write and widened once mid-session** — from
`#25 + #12` to include the seven comment targets — **before the first write to any of
them**, because the resolution handed work to four issues the original row did not name.
The board was **re-read at that point and had changed on disk**: `c10-quote-feed`'s row was
gone, so this session became the only active claim.

## KB candidates

**5 written, none ingested** — normal mode, so the list is a proposal and Ido's call. See
`kb-candidates/2026-08-10-c9a-schedule-a-task.md`.

`kb-candidates/` was listed before the first unit of work: **two files pending**, both
`2026-08-09`, both already **rewritten down to one always-ask entry each** by
`kb-ingest-backlog-drain` and waiting on Ido rather than on a session — so there was no
backlog for this session to drain. A third file (`c7`'s) was mid-drain at session start and
has since been committed and removed by its owner.
