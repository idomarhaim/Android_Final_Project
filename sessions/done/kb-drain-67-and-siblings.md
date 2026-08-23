---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
owns:
  - kb-candidates/2026-08-23-67-delete-anything.md
  - kb-candidates/2026-08-23-68-drag-to-move.md
  - kb-candidates/2026-08-23-70-verify-dashboard-average.md
  # Added 2026-08-23 by 69-one-off-occurrence-edits, which wrote its candidates at
  # close-out and did not drain them, on this brief's own precedent. Its entry 1 EXTENDS
  # 70-verify-dashboard-average's entry 1 -- they want merging, not two sections.
  - kb-candidates/2026-08-23-69-one-off-occurrence-edits.md
  - CHANGELOG/2026-08-24/kb-drain-67-and-siblings.md  # the day the session actually ran
  - sessions/kb-drain-67-and-siblings.md
  - "cross-repo: C:\\Dev\\JARVIS\\kb\\dev\\** and C:\\Dev\\JARVIS\\kb\\log\\**"
created: 2026-08-23 by 67-delete-anything
closed: 2026-08-24 by kb-drain-67-and-siblings
result: |
  10 of 11 entries ingested; JARVIS 64fc76f. Check-KbLinks CLEAN (117 pages).
  Three candidate files deleted (fully drained); 67-delete-anything.md rewritten
  down to entry 4, which is ALWAYS-ASK and still open -- one line as an example
  under look-at-your-own-output.md visual-acceptance, or nothing. Ido's call.
  No new page was needed: every destination already existed, and three entries
  proposed one that was wrong. See CHANGELOG/2026-08-24/kb-drain-67-and-siblings.md.
  DEVIATION: #69/1 and #70/1 were drained as two sections, not the one the brief
  required -- the cheap half was already committed as 4c-ii. Reasoned in the
  changelog and in kb/log/2026-08-24.md.
---

# Drain `kb-candidates/` — three files, one of them this session's

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

⚠️ **This is CROSS-REPO and owes `C:\Dev\JARVIS` a board row of its own.** The candidates live in
this repo; the pages land in `C:\Dev\JARVIS\kb\`. A `/kb-ingest` into the central bundle *is* a
cross-repo visit, and claiming only where you started is not a lesser claim but no claim, one repo
over. Read `C:\Dev\JARVIS\SESSIONS.md` before the first write there.

**Read first:** [`AGENTS.md`](../AGENTS.md) · the three candidate files themselves ·
`C:\Dev\JARVIS\skills\kb-ingest\SKILL.md`

## Why this is its own session

`67-delete-anything` wrote its candidates and **did not drain them**, deliberately: the drain is a
different repo, a different board and a different unit, and it opened at the end of a session that
had already shipped a ticket, run 1084 JVM plus 320 instrumented tests, and captured a render pass.
The candidate files are committed and **each entry stands alone** — that is the whole point of the
format — so nothing was lost by stopping.

Two of the three files predate it and were found by the session-start sweep, which is the mechanism
working: nobody had to remember.

## What is there

| File | State |
|---|---|
| `2026-08-23-67-delete-anything.md` | **4 entries, undrained.** Entries 1–3 ready; entry 4 is thin and marked *ask before promoting*. |
| `2026-08-23-68-drag-to-move.md` | **Partly drained twice already.** One survivor (`№4`, clamp-before-snap), kept with its original numbering and its reasons written into the file. |
| `2026-08-23-70-verify-dashboard-average.md` | Written by that session, **undrained**. Read it before assuming; it may hold more than the one entry visible at the top. |
| `2026-08-23-69-one-off-occurrence-edits.md` | **3 entries, undrained.** Added after this brief was written. **Entry 1 EXTENDS `70-verify-dashboard-average`'s entry 1** — read the two together and write **one** section, not two. Entry 3 is thin and marked *merge or drop*. |

## Two entries that must be drained as ONE

`2026-08-23-70-verify-dashboard-average.md` entry 1 (*`UP-TO-DATE` replays another session's test
run*) and `2026-08-23-69-one-off-occurrence-edits.md` entry 1 (*… replays another session's **APK***)
are the same failure at two points on one pipeline. The second was found **because** the first was
read that morning, and it carries a measurement the first does not have: the replayed androidTest
APK **shrank by 133,739 bytes** when rebuilt, which is independent evidence the artifact was a
*sibling's* rather than merely *stale* — a distinction a timestamp cannot make and which needs the
opposite response.

Writing them as two sections would put the cheaper half first and leave a reader who stops there
believing `--rerun-tasks` on the build is the whole remedy. It is not: the APK case fails **across a
process boundary**, so the habit is *check the artifact's mtime and size before installing*, not
*check the build log*.

## The entry that matters most

**`#67`'s entry 1 — a bidi isolate splits every substring matcher that spans a number.** It is not
GoalPilot-specific in its mechanism and it is repo-wide in its blast radius: every swept-package
string in this app interpolates a `bidiIsolated()` count, so **any** future instrumented assertion
written the obvious way fails with a message about *layout*. `Observed:` 8 of 15 tests, 2026-08-23.

Its destination is deliberately left open in the candidate: it is a **testing** finding, so
`android-device-verification.md` is the wrong home unless nothing better exists. **Look for an
existing Compose-testing page before creating one** — `grep` the bundle for the phenomenon's name
and for the words a reader would use, not for the shape of the answer.

## Task

Drain all three through `/kb-ingest`, honouring what each entry's own `Destination` and `Status`
say. Report each with the standard `📥 **Ingested:** <topic> → <bundle>/<page>` line.

## The decisions that are already taken — do not reopen them

- **Entry 4 (`#67`) is thin and stays always-ask.** It is one instance of *look at the render*, which
  the KB already argues at length. One line as an example, or nothing. Do not write it a page.
- **A fully-drained file is deleted in the same commit as the promotion it produced**, and a partly
  drained one is **rewritten down to its survivors and never deleted**
  (`rules/derivable-decision.md` §1). `2026-08-23-68-drag-to-move.md` is already in the second state
  and its numbering is load-bearing.
- **Anything that supersedes or contradicts a standing KB claim stays always-ask in both modes**, as
  does anything destined for `rules/`. `#67`'s entry 2 **corroborates** rather than supersedes — it
  is an `Observed:` line under an existing section, not a rewrite.

## Exit

- All three files drained or explicitly rewritten to their survivors, with a reason in each `Status`.
- `Check-KbLinks` clean, with the page count in the changelog.
- A journal entry in `kb/log/2026-08-23.md` naming **the source file and its repo** — that is the
  candidate→page tie, and it is the only thing that survives when the bundle lives elsewhere.
- Rows released on **both** boards.
- `CHANGELOG/YYYY-MM-DD/kb-drain-67-and-siblings.md` with counts verbatim.

## Out of scope

- **Pushing `Android_Final_Project`.** Six commits are held on precondition 5 and that is Ido's call,
  not this session's. Ask before assuming it was resolved.
- **`#67`'s own held item** — the end-to-end device confirmation of the unfiled-task defect. It is
  named on the ticket and needs a signed-in account.
