# KB candidates — `61-google-calendar`, 2026-08-23

Three entries. Each stands alone: no entry below depends on this session's transcript, because a
transcript is machine-local, invisible to the other agent, and truncated by compaction — which is
precisely when it would be reached for.

---

## 1 · When **absence** is your signal, you must ask wider than you judge

**Claim.** Any sync that detects deletion by *"the record did not come back"* has a range bug
waiting in it: the **query's own edge manufactures the signal**. An item moved just past the
boundary of the range you asked about comes back absent, is read as deleted, and the correction is
not a bigger range — it is **two** ranges, a wide one you *query* and a narrower one you are willing
to *judge*, with the gap between them absorbing exactly the movement the narrow one would
misread.

`Observed:` 2026-08-23, building `#61`'s Google Calendar pull. The first draft used one range for
both, and the defect is invisible in every test written from the happy path — an event inside the
range, deleted, is correctly detected; an event inside the range, moved inside the range, is
correctly detected. Only an event **moved out of the range** distinguishes the two designs, and
nothing about a single-range implementation suggests that case exists.

**Why it generalises past calendars.** The shape is *"I infer a fact from what a bounded query did
not return"*, and it recurs wherever a bounded read is used to detect removal: a paginated API where
the last page is assumed to be the end, a directory listing over a date-partitioned prefix, a diff
of two filtered sets. The fix is the same each time and costs one constant. What makes it worth a
page rather than a comment is the **failure direction**: it fails toward *confident wrongness* —
you do not get an error, you get a deletion you invent, and downstream that becomes a question in
front of a user or a row removed from a database.

**The distinction that keeps it honest.** There is usually a *real* ambiguity nearby that no
widening fixes — in `#61`'s case §2.7's *"a move-out is indistinguishable from a delete"*, because
the app can only see the one calendar it created. Widening the query does nothing about that one and
is not meant to. The point is to **stop manufacturing a second ambiguity on top of the real one**,
so the question the user is eventually asked is only the question that genuinely had no answer.

**Destination.** New page, `kb/dev/absence-as-a-signal.md`. Nothing in the bundle covers it:
`look-at-your-own-output.md` is about verification of your own artifacts, not about inference from a
bounded read.

**Anchors.** `CalendarSync.pullPlan`'s KDoc and
`SyncCalendarUseCase.QUERY_MARGIN_DAYS` carry the argument in code; `CalendarSyncTest`'s
*"an event missing from the response is only a disappearance inside the judged window"* is the test.

**Supersedes.** Nothing.

**Status.** **ready to ingest** — new page, no standing claim contradicted.

---

## 2 · In a shared tree, a red test is not evidence about **your** change — and `git status` at
session start cannot tell you so

**Claim.** When several sessions work one working tree, the moment to run `git status` is the moment
you **read a test result**, not the moment you started. A suite that comes back red is a statement
about the tree *as it was when the task ran*, and in a shared tree that tree contains whatever a
sibling had saved seconds earlier.

`Observed:` 2026-08-23, `61-google-calendar`. `git status --short` at the start of the session
returned exactly one line — an untracked directory belonging to `60-calendar-surface`. Twenty
minutes later `:app:testDebugUnitTest` returned **1010 tests, 2 failed**, and the instinctive
reading was that something in my own diff had broken two unrelated suites. Re-running `git status`
against the specific failing files showed **six modified files I had never opened**, all belonging
to a third session, all changed in the intervening minutes. Both failures were theirs, mid-edit.
Attribution took one command; without it the next step would have been an hour spent bisecting my
own correct code.

**Why.** This is the shared-tree analogue of a finding the bundle already carries at the *build*
layer — `BUILD SUCCESSFUL` returning `UP-TO-DATE` over test classes a sibling had already run, so a
green belonged to their tree rather than yours (recorded by `63-occurrences-and-recurrence`,
2026-08-23). That one is about a **false green**; this is the **false red**, and it is the more
expensive of the two because a green is at worst ignored while a red is acted on. The two are one
claim: *in a shared tree, a test result names the tree, not the author.*

**The cheap procedure.** On any failure, before reading the stack trace: `git status --short -- <the
files the failing test touches>`. Anything modified that is not yours, and not on your board row, is
a sibling's — and then §5.4's fork applies (live → announce and touch nothing).

**Destination.** `kb/dev/look-at-your-own-output.md`, as a new subsection beside the
`BUILD SUCCESSFUL`/`UP-TO-DATE` entry — same page, same family, opposite sign.

**Anchors.** `kb/dev/look-at-your-own-output.md` §4 (the *read the task line, not the last line*
entry); `SESSIONS.md`'s `63-occurrences-and-recurrence` release note, which records the false-green
half.

**Supersedes.** Nothing. It **widens** the existing entry rather than replacing it — the existing
text says *"read the task line, not the last line"*, which is correct and does not reach a red.

**Status.** ⏸️ **held — the page is claimed on another board.** `65-measure-proposal` holds
`kb/dev/look-at-your-own-output.md` and `kb/log/` on the **JARVIS** board. Nothing is lost: this file
is committed, and the next session that lists `kb-candidates/` finds it.

---

## 3 · A brief with `status: active` is a liveness surface the claim board does not have

**Claim.** `grep '^status: active' sessions/*.md` finds live sessions that `SESSIONS.md` does not,
and it should be part of the session-start read rather than a thing one notices by accident.

`Observed:` 2026-08-23. Working `#61`, I attributed two failing tests to a sibling and went looking
for its owner. `SESSIONS.md`'s **Active claims** held exactly one row — `60-calendar-surface` — and
none of the six dirty files was on it. The owner was `66-unmeasured-percent`: its brief said
`status: active`, its `owns:` list named five of the six files, and it had **no board row at all**.
So a session that had been running long enough to edit six files across three packages was invisible
to the one artifact whose entire job is to say who is working on what.

**Why it happens, structurally.** `/kickoff` §3 writes the row, so a brief-driven session normally
claims. But `status: active` is set by the *brief*, and the two writes are separate: a session that
was started another way, or that set the status and was interrupted before committing its row, ends
up live and unclaimed. The board cannot detect this, because the board's only evidence is rows.
The brief can, because a brief is per-session by construction and its status is a claim about
liveness in its own right.

**Why it is worth a rule and not just a habit.** The board rule already says *"an absent row is not
proof the session is finished"* and escalates to a transcript scan — a machine-local artifact that
does not exist on Copilot or on another machine. `sessions/*.md` is **committed**, so it works on
every surface the transcript check does not, and it is one grep. It does not replace the transcript
check (a brief left `active` by a dead session is a false positive, exactly as a stale row is), but
it is strictly cheaper and strictly earlier.

**Destination.** ⚠️ **`rules/agent-topology-and-model-routing.md` §5** — which makes it always-ask
in both modes, and puts it under the 🎬 walkthrough gate, because it changes what a session does
before its first edit.

**Anchors.** `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.3(c) (the transcript
procedure this sits in front of); `~/.claude/skills/kickoff/SKILL.md` §3.

**Supersedes.** Nothing; it adds a step ahead of §5.3(c) rather than changing it.

**Status.** ⏸️ **blocked — always-ask, twice over.** Destination `rules/` is always-ask in both
modes, and the change alters the interaction protocol (what a session reads before its first edit),
so it owes a 🎬 walkthrough offer. Neither is a reason to drop it: the entry is committed here and
survives this session.
