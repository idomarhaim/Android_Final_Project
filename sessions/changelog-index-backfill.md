---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: AUTO MODE
status: ready
created: 2026-08-16
---

# `changelog-index-backfill` — the changelog index died on 2026-08-10

**Read first:** [`AGENTS.md`](../AGENTS.md), then
[`CHANGELOG/CHANGELOG_README.md`](../CHANGELOG/CHANGELOG_README.md).

## Task

`CHANGELOG/CHANGELOG_README.md` stops at **2026-08-10**. There are **21 day folders** and the last
~20 session entries have **no row** — including every session from 2026-08-15 and 2026-08-16, which
is the day the repo did most of its work. Found and filed by `resource-guard-inputs`.

Backfill the missing rows, and decide whether this file should be generated from now on.

## ⚠️ The trap — do not run JARVIS's generator at this file

`C:\Dev\JARVIS\scripts\New-ChangelogIndex.ps1` takes `-RepoRoot`, so it *looks* portable. It is
not, yet:

```
FAIL: generated-region markers not found in …\CHANGELOG\CHANGELOG_README.md
  expected: <!-- CHANGELOG-INDEX:BEGIN … --> … <!-- CHANGELOG-INDEX:END -->
```

**And adding those markers is not a formatting nicety — it is destructive.** JARVIS's generator
emits a **table**, one row per day, with a short `detail` per session. GoalPilot's file is a
**hand-written list**, `- [path](path) — description`, and several of those descriptions are
**hundreds of words of Ido's own prose** carrying findings that exist nowhere else. Wrapping the
existing list in markers means the next run **overwrites all of it**.

So there are three options and the choice is the real work of this session:

1. **Backfill by hand, keep the format.** Preserves everything. Costs ~20 summaries, and the file
   dies again the next time somebody forgets.
2. **Generate, and accept a table.** The prose has to move somewhere first — it is not
   reconstructible from the session files' titles, so *nothing may be deleted before it is moved*.
3. **Generate a *day* index, keep the hand-written *session* notes.** Markers wrap only a
   generated day-level table; the rich list survives beside it. Two structures in one file, which
   the generator's own design deliberately avoids — but it may be the honest fit here.

**Recommendation, not a decision: option 3.** The complaint is *"I cannot find today's entries"*,
which a day-level table fixes; the prose is what makes this file worth having and is the one thing
a generator cannot produce. But read the file before agreeing — 44 lines, and lines 9–44 are the
asset.

## Carries over

- The finding, filed by `resource-guard-inputs` — see its changelog entry, and its own note that
  *"adding only my row would make a stale index read as current"*, which is why nobody has patched
  it piecemeal.
- **JARVIS solved the sibling problem already**: `New-ChangelogIndex.ps1 -Staged`, plus a
  pre-commit hook that blocks a commit whose changelog file is not in the index. If GoalPilot
  adopts generation, adopt the hook too — otherwise it rots again, which is exactly what happened
  here.
- 21 day folders exist; `ls CHANGELOG/2026-*` is the ground truth, not the index.

## Out of scope

- Any app code, any resource, any test.
- Rewriting or shortening existing descriptions. **Move them if you must; never trim them.**
- JARVIS's own copy of the script — if it needs a change to be reusable, that is a separate
  cross-repo unit owing a row on that board.

## Exit

- Every day folder and every session file reachable from the index — verified by counting, not by
  reading: `ls CHANGELOG/2026-*/*.md | wc -l` against the rows.
- If you adopt generation: prove it round-trips **before** committing — run it twice and confirm
  the second run is a no-op, and confirm no hand-written line was lost by diffing against `HEAD`.
- Your own `CHANGELOG/2026-08-16/changelog-index-backfill.md` — and it must appear in the index
  you just fixed, which is its own smoke test.
- Commit; push under `AUTO MODE` once the six preconditions hold.
