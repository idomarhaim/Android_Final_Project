# c9e-event-lifecycle — resolved #28, and the prompt the ticket assumed did not survive its own scope model

**Session:** `c9e-event-lifecycle` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#28 · `C9e`](https://github.com/idomarhaim/Android_Final_Project/issues/28) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked **bare**, so the frontier pick was the agent's and the reasoning is
recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block — every open child of `#12` queried for `blocked_by`:

| Ticket | Blocked by | Verdict |
|---|---|---|
| `#28 · C9e` | `#27` (closed) | **frontier — claimed** |
| `#21 · C5` | `#13`, `#18` (both closed) | frontier — left |
| `#31 · C12` | `#18`, `#14` (both closed) | frontier — left |
| `#19 · C1` | `#18`, `#39` (both closed) | unblocked but **assigned and live** |
| `#20`, `#22`, `#24`, `#30`, `#35` | all wait on `#19`, directly or through `#24` | still blocked |

Map size verified against GitHub: **25 children, 16 closed, 9 open** — one more closed than
`c1-points-and-time` counted, because `#26` closed in between. **Sixth** derivation of the
day, and the first whose *membership* differs from the previous one.

**Why `#28`:** the single reason it was declined four times — *"the calendar half is live,
`c9b-calendar-surface` is mid-prototype on `#26`"* — **expired 101 seconds before this
claim**. `#26` closed at `19:23:52Z` (verified via `closedAt`) and its `C9b` line is already
in `#12`'s index. Every calendar predecessor is now closed *and* released (`C9d` #17,
`C9a` #25, `C9c` #27, `C9b` #26), which makes `#28` the calendar's **last open ticket** —
closing it finishes a subsystem rather than opening one.

`#21 · C5` was declined because it sits **nearer** the live `#19` than `C9e` does: it models
a goal with no target, and what effort and progress mean for such a goal is exactly what
`c1-points-and-time` is deciding right now. `#31 · C12` was declined on the prototype
contention — every frontier ticket here is HITL, so HITL-ness discriminates nothing, but a
*prototype* is the heavy kind and `#26` just spent **eight revisions** of Ido's attention.

**`c9b-calendar-surface`'s row was left untouched.** Ticket closed, index line written,
working tree clean — that reads as a session **mid-release**, not mid-work, and releasing its
row is that session's move. A row edited for another session is a report, not a claim.

## Claim

- **GitHub:** `#28` assigned to `idomarhaim` **before any other work**, so concurrent
  sessions skip it. Assignment confirmed by reading it back out of GitHub.
- **`SESSIONS.md`:** row added to *Active claims* with paths and singletons (**none** — a
  grilling ticket ships no code, so no build, no device, no Firebase), plus the frontier
  reasoning and two coupling points.

## Coupling points, named on claiming

1. **`#12`'s *Decisions so far* is a commons**, and the race it names has fired for real once
   (`c3-points-currency` records it from both sides). Re-fetch the body immediately before
   appending, write only this session's line, verify a pure insertion. The standing *"`#26`'s
   line is still owed"* note in the older board banners is now **discharged** — that line is
   written. `C13` (#32)'s index gap stays Ido's to assign.
2. **`C9e` arrives with four rules inherited and exactly one live edge.** The inherited four
   are `C9c`'s, from a **released** session, so they are inputs and never subjects: matching
   is by `googleEventId` · times cross the sync and state never does · titles are written but
   never read back · a cancelled event unsyncs and never deletes. The one live edge is `#19`'s
   **bulk re-scoring pass** — if re-scoring can move times, it is a bulk write into Ido's real
   calendar. Anything found here that bears on `#19` gets **posted there**, not decided there.

## `kb-candidates/`

Re-listed at session start, as the folder's existence requires — **seven files**, agreeing
with `c1-points-and-time`'s correction. Nothing is drainable by this session: six are
**always-ask** (five target `rules/`, four of those `rules/question-axis-naming.md`, one
accumulating amendment to be read together); the seventh,
`2026-08-10-c9b-calendar-surface.md`, is ordinary and `AUTO MODE`-eligible but is **owned by a
row still on the board**, so it drains with that session's release.

## 🧪 Tests

**None run, and none owed.** This is a `wayfinder:grilling` ticket on a planning map — the
map's standing preference is *plan, don't do*, and no ticket on it ships code. Nothing in this
unit touched `app/`, `functions/` or `firestore-tests/`, so no server, client, endpoint,
database or UI layer was exercised. The unit's own verification is structural and was
performed: the frontier was re-derived from the dependencies API rather than trusted, `#26`'s
closure was verified by timestamp rather than assumed from the board, the assignment was read
back out of GitHub, and the map body will be re-fetched immediately before its index line is
appended.

## The resolution

**`#28` is resolved and closed.** *A synced event is never asked about and never lost.*

Two rounds of `AskUserQuestion`, and **Ido answered neither the way the options were shaped** —
which turned out to be the finding rather than a hiccup.

**Round 1** offered *remove the event / keep it / retitle it* for a deletion and a re-estimate.
He answered both identically and from outside the set: **"the app asks the user whether to also
delete / also update in the synced calendars."** Recorded as a rule rather than two rulings.

**Round 2** asked how often it may ask, and what it does when `C1`'s re-scoring moves 40 blocks
at once. He could not answer either, in almost the words `c14-challenge-scoring` parked against
`rules/question-axis-naming.md`: *"I couldn't understand what the implications of each option
are — explain simply and schematically, choose the solution that gives the highest standard for
the app, its UX/UI and the software, and if it can be improved, improve it."* That is a
delegation, not a stall, and it was taken as one: no third picker.

### What was decided, and the argument that decided it

`C9d` had already bought **`calendar.app.created`** and a **dedicated GoalPilot calendar**. The
app therefore cannot see, let alone write to, any other calendar — so a per-action *"also update
in Google?"* is the app asking permission to **edit its own sandbox**, and a dialog answered yes
ten times stops being read by the eleventh. **Ido's own answer was priced against a risk his
earlier ticket had already removed.**

So the prompt is replaced by the current standard for reversible destructive action:

| Task-side change | Google-side effect |
|---|---|
| Retimed / re-estimated | event **patched in place**, `googleEventId` preserved · `Moved to 09:00–10:30 · Undo` |
| Rung change `BLOCK` → `DEADLINE` | **cancel + recreate** (not a patch of the same shape), `googleEventId` replaced |
| Renamed / moved to another goal | title written, never read back (`C9c`) / nothing |
| **Completed** | **nothing** — `C9c` already ruled state never crosses |
| **Deleted** | **future** events cancel, **past** events **stay** |
| **Goal archived** | same split by tense |
| **40 blocks re-scored** | **one batch** → one entry in `C9b`'s daily review, one batch-scoped undo |
| **Orphaned event** | surfaced in that same review, **never auto-deleted** |

Three things earn the removal of the gate rather than merely asserting it:
1. **Deletion is cancellation** — the event lands in Google's trash, restorable for **30 days**,
   so the ticket's own premise (*"a wrong deletion is not recoverable from inside GoalPilot"*)
   is falsified rather than argued with.
2. **Every destructive effect splits by tense.** Past events record time actually spent;
   erasing them rewrites the user's history of their own week. Future events are a claim on time
   no longer claimed. That single split answers *"removed, or left as a record?"* as **both** —
   the bullet was one question wearing two hats.
3. **Ido's instinct survives as exactly one prompt**, shown once ever, beside the incremental
   scope grant `C9c` §2 already interrupts with: *Keep it automatic* / **Ask me each time**. The
   second is his original answer, permanent and reversible in Settings — offered rather than
   defaulted, because undo protects on the day you are not reading.

**Filed nothing, graduated nothing.** Every consequence routed to a ticket that already exists.
The `GoogleSignIn`-migration fog bullet mentions the calendar scope but was not sharpened by
this resolution, so it was left untouched.

### One unverified claim — flagged at commit, then **checked**, and it was half wrong

The pre-commit re-read caught it: §3's *"restorable from Google's trash for 30 days"* was stated
from general knowledge and is load-bearing, being half the reason the confirmation prompt was
removed. A caveat naming exactly that was appended to the resolution comment first, so nobody
would inherit it silently.

**Then Ido asked how such an issue actually gets closed, and the honest answer was to close
it** — it is a documentation lookup, not a session. Result, against Google's own help page:

- ✅ **Confirmed:** *"When you delete an event or mark it as spam, it stays in that calendar's
  trash for **30 days**."* Restorable by anyone with *Make changes to events*, which is Ido on
  a calendar he owns.
- ❌ **A hole, in the worst possible place:** *"If you choose **This and following events** or
  **All following**, the deleted events are **not moved to the trash and can't be restored**."*
  That is precisely the shape `C5`'s repeat rules reach for, and `C9e` §5 had routed it to a
  batch write as if it were ordinary.

**§3a was added to the resolution:** GoalPilot **never** uses Google's *this-and-following*
delete. It cancels the affected occurrences **one at a time**, which does trash them — more API
calls on a long series, in exchange for the guarantee the whole no-prompt bargain rests on. The
change is purely how the batch executes against the API; `C9e` §5's machinery (one batch, one
review entry, one undo) is unchanged. A [correction was posted to `C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5245723024)
because it makes series length an operational cost in its model, not only `C9e`'s problem.

**Still open, and marked as such on the ticket:** whether `events.delete` **via the API**
trashes rather than erases. Google's page documents the UI. §3a is the cheap hedge — it is the
recoverable shape *as documented*, and the in-app Undo does not depend on the answer — but a
build session touching the delete path owes one empirical check against a real calendar.

### Hand-off comments posted (flow one-way, nothing another session owns was edited)

- [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19#issuecomment-5245582791) — a re-scoring pass is a **bulk write into Ido's real calendar**; `C9e` gave it a home, but `C1` should know it is user-visible outside the app.
- [`C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5245583067) — *"all future occurrences"* is a batch write, and a rule edit **never reaches backwards** into past occurrences.
- [`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5245583361) — two more screens for the map's design standard (the undo toast, the review batch entry), both Hebrew-critical: `הועבר ל־09:00–10:30 · בטל` needs **direction isolation** or bidi flips the range.

### The `#12` commons, discharged

Body **re-fetched immediately before the write**, compared byte-for-byte against the copy the
line was built on (`cmp` — unchanged, no race), then written and verified: **144 → 147 lines,
16 → 17 decision lines, 0 lines removed.** The only non-inserted difference is a trailing blank
line GitHub appends. `C13` (#32)'s index gap was left alone — still Ido's to assign.

## `kb-candidates/` — two flagged, neither drained, both **re-based mid-session**

Listed at session start (**7 files**) and again before filing — and the folder had changed
underneath: `picker-rule-consolidation`, a JARVIS visitor session, **drained the four parked
picker candidates into `rules/question-axis-naming.md` while this ticket was being resolved**
(`d9616b9`, `5b5e113`). The folder is now **4 files**, and the consolidated rule already
carries **Mode 6 — form** and **The widening — the fork check over the derivation closure**,
which is most of what this session was about to file.

Both entries were **rewritten against the committed text** rather than shipped as drafted.
What survives is what those sections do *not* cover
([`kb-candidates/2026-08-10-c9e-event-lifecycle.md`](../../kb-candidates/2026-08-10-c9e-event-lifecycle.md)):

1. **Mode 6's test is stated on the question and needs to be stated on the options** — round 2
   used **scenario stems** over **mechanism forks** (prompt cadence, dialog shape) and was
   refused outright, which means an author can satisfy Mode 6 by rewriting the stem and change
   nothing. Second half: the consolidation table admits *form* as a cause **"only if some
   question in this batch was answered"**, and here nothing in round 2 was answered — the
   answered/refused split ran **across two pickers**, so the gate excludes the one correct
   diagnosis and routes to *density* instead. The comparison window wants to be the session.
2. **The widening reaches derivation closures in code; it does not reach a closed sibling
   decision.** Round 1's options were **actions**, not quantities — no closure to intersect and
   no grep that would fire — and what falsified them was `C9d`'s scope decision on another
   ticket. On a wayfinder map this is structural: **ticket bodies are written at charting time
   and the frontier moves while the body does not**, so any ticket that waited behind a blocker
   may carry a premise its own blocker has since deleted.

Both are **always-ask twice over**: destination `rules/`, and (1) rewrites a claim committed
30 minutes earlier by another session.

## Status

**Done.** `#28` resolved, closed, indexed on the map; three hand-off comments posted; board row
released. The **calendar half of the map is now complete** — `C9a` #25, `C9b` #26, `C9c` #27,
`C9d` #17 and `C9e` #28 all closed.

---

## Addendum — `/kb-ingest`, run on request as a **partial drain**

Ido asked for the ingest with one condition: *"just make sure it doesn't harm anything,
including a session working in parallel."* That condition is what made it partial.

**Not drained — both numbered entries in this session's candidate file.** Both target
`rules/question-axis-naming.md` (always-ask in both modes), and entry 1 additionally
**rewrites a standing claim** `picker-rule-consolidation` committed ~40 minutes earlier
(`bc3b31e`). They survive with their **original numbers** under a new
`## Standing — always-ask` section, so the next drain does not re-reason about the
destination. **A 🎬 walkthrough is owed before either is written** — amending how pickers are
drafted alters the interaction protocol.

**Drained instead — two claims that were not in the file at flag time**, having emerged while
verifying the §3 caveat after release:

- 📥 **undo replaces a confirmation only where every variant is recoverable** → **new**
  `C:\Dev\JARVIS\kb\dev\undo-replaces-confirm-only-if-recoverable.md`
- 📥 **a map ticket's body is written at charting time and never ages** →
  `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` **§8** (update in place)

`kb/index.md` +1 row, journal entry in `kb/log/2026-08-10.md` naming this candidate file
**with its repo**. `Check-KbLinks` **CLEAN** at 62 pages.

**Parallel-session safety, since it was the condition.** `kb/` is a singleton on JARVIS's
board and was claimed (`8d73d39`) before the first write, then released. **One defect was
made and repaired here:** the ingest commit initially carried
`sessions/picker-delegation-clause.md`, a brief `c1-points-and-time` had left **staged but
uncommitted** in JARVIS's index — `git commit` takes the whole index, so explicit `git add`
paths were not enough. Caught immediately on reading the commit's own `--stat`, repaired with
`git reset --soft` (nothing lost, nothing pushed) and recommitted with `git commit --only
<paths>`, which leaves the sibling's file staged exactly as it was. Final commit `ace7bd9`
carries 7 files, all this session's.

**The three other candidate files** (`c9f`, `c1`, `c16`) belong to other sessions and were
listed, not touched.

## 🧪 Tests — addendum

`Check-KbLinks.ps1 -BundlePath C:\Dev\JARVIS\kb` — **CLEAN**, 62 pages (61 before), no broken
links, no orphans, no wikilinks. Still the only test layer a Markdown bundle has.
