# `changelog-index-backfill` — the index is generated now, and a hook keeps it that way

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** The changelog index is generated rather than hand-written: 46 of 75 session files had no row since 2026-08-10, so the day table now sits behind CHANGELOG-INDEX markers and a pre-commit hook refuses any commit whose changelog file it does not list.

`/kickoff changelog-index-backfill` · brief [`sessions/changelog-index-backfill.md`](../../sessions/changelog-index-backfill.md) ·
mode `AUTO MODE`. Found and filed by `resource-guard-inputs`. No app code, no
resource, no test touched.

## 1 · What was actually wrong — the brief undercounted it

`CHANGELOG/CHANGELOG_README.md` was a hand-written list, last extended on **2026-08-10**.

| | Brief said | Measured |
|---|---|---|
| Sessions with no row | "~20" | **46** |
| Day-folder session files on disk | — | 75 |
| Rows in the index | — | 36 (7 flat + 29 day-folder) |
| Day folders | 21 | **15** day folders + 7 flat files (the 21 counted both, plus `CHANGELOG_README.md`) |

Every session from **2026-08-15 and 2026-08-16** was missing — the repo's two
busiest days. A missing row is indistinguishable from a quiet day, which is why
nothing noticed for a week.

## 2 · The decision, derived rather than asked

The brief offered three options and asked the session to choose. It did not have to
be a choice: **this repo's own committed convention already answers it.**
`.github/instructions/general.instructions.md`, § Changelog Rules:

- **line 58** — *"`CHANGELOG/CHANGELOG_README.md`'s Available Files table is derived
  content — **never write prose into it**."*
- **line 59** — *"Where a generator exists, run it and never hand-edit the table."*
- **line 37** — *"⚠️ MANDATORY summary line, on line 4"* — the one line a session
  contributes to the shared index, from inside its own file.

So **option 1 (hand-backfill, keep the format) was never available**: it would have
added 46 more rows of prose to a table the repo has committed to keeping derived.
Decision taken per `rules/derivable-decision.md` — logged, not asked.

**Option 3 was adopted, and the brief's objection to it is wrong.** The brief warned
that two structures in one file is *"what the generator's own design deliberately
avoids."* The generator's header says the opposite:

> Legacy flat days (`CHANGELOG/YYYY-MM-DD.md`, pre-2026-08-04) are NOT touched: they
> keep their hand-written rows in **the static Archive table below the generated
> region**. Nothing migrates — that is history, not drift.

A generated region with hand-written sections beneath it is the shape the script
already ships for. The prose did not have to move out of the file at all.

**Where the prose did *not* go, and why.** Two destinations were considered and
rejected:

- **Each session's own `> **Summary:**` line** — forbidden twice over. It is writing
  into another session's file (line 28), and the generator joins a summary into a
  single table cell, so the ~700-word row for `c9c-calendar-sync` would have become
  one unreadable cell. The convention asks for ~200 characters.
- **Per-day `SUMMARY.md`** — line 60 sends *a day's* prose there, but these 29
  descriptions are per-**session**, and a `SUMMARY.md` is a rollup written by a later
  day. Assembling one from index rows would be inventing a rollup nobody wrote.

## 3 · What the file looks like now

Three parts, and only the first is machine-owned:

1. **Generated day table**, between `<!-- CHANGELOG-INDEX:BEGIN … -->` and
   `<!-- CHANGELOG-INDEX:END -->` — 15 day folders, newest first, one link per
   session. Complete by construction.
2. **`## 🗄️ Archive — flat day files (before 2026-08-04)`** — the 7 pre-folder rows,
   verbatim. The generator walks day *folders* and cannot reach these, so they stay
   hand-written and load-bearing: delete them and 7 files become unreachable.
3. **`## 📜 Long-form notes — sessions up to 2026-08-10`** — all 29 hand-written
   day-folder descriptions, **verbatim**. Moved, never trimmed. They are now
   explicitly supplementary: the table above owns *coverage*, so their stopping at
   2026-08-10 is no longer a fault.

One line of the file's own header prose was corrected, not a description: it said
entries *"before 2026-08-03"* are flat, but `2026-08-03.md` is itself flat. The
migration finishes the last partly-flat day flat, so **2026-08-03 has both forms** —
which the header now says.

## 4 · Enforcement, because a generator nobody runs rots like a list nobody updates

- `scripts/New-ChangelogIndex.ps1` — copied **byte-for-byte** from
  `C:\Dev\JARVIS\scripts\`, hash-verified, unmodified. The brief said it "looks
  portable, but is not, yet"; the only thing blocking it was the missing markers, so
  once those exist it needs no change. JARVIS's copy was not touched (out of scope).
- `scripts/Install-GitHooks.ps1` — likewise verbatim.
- `scripts/git-hooks/pre-commit` — **written for this repo, not copied.** JARVIS's
  hook also calls `Sync-AgentAssets.ps1` and `Check-KbLinks.ps1`; neither exists here,
  and a hook calling a missing script fails open in the confusing direction. This one
  runs the changelog check alone.
- Installed into `.git/hooks/`. `.git/hooks` is not version-controlled, so a fresh
  clone runs `powershell -File scripts\Install-GitHooks.ps1` once — recorded in
  `scripts/README.md` and in the index's own header.

`-Check -Staged`, never the working tree: this repo runs parallel sessions, and a
sibling's untracked changelog file must be invisible to my commit gate or one
session's work in progress freezes everyone's commit stream.

**The residual, exercised live during this session — stated rather than implied.** The
git index is shared and is *not* partitioned by path, so the generator's input moves
under you. While this commit was being prepared, `51e-sweep-components` committed
(`bc5ef69`), then staged and unstaged a further `51e-sweep-components.issue-comment.md`,
and a `git add` of mine failed outright on that session's `index.lock`. **Observed:**
the input to the table changed three times between generating it and committing it.
**Inferred:** had that extra file still been staged at commit time, this commit would
have published a row linking a file nobody else has, healing only when they committed.
**Untested:** whether that transient actually renders as a dead link in any tool that
follows it. No script can close this — the generator's own header says so, and only a
worktree per session would. It is recorded because a transient dead link in a
*generated* file reads as a generator bug to whoever finds it.

That is also why the last check before committing was re-run rather than trusted: the
first draft of this very paragraph asserted the dead link as fact, and was wrong by the
time it was written.

## 5 · The Summary-line gap, reported and deliberately not filled

Only **6** day-folder session files carry a `> **Summary:**` line. Their generated rows
carry prose; every other row is a **bare link**.

The line has been mandatory since `general.instructions.md` **v16**, synced into this
repo on 2026-08-05 (`f7ae3dd`) — *verified by `git log -S`, after this entry first
claimed v14 from memory and was wrong.* The six that have it are exactly the sessions
of 2026-08-05 and 2026-08-06, i.e. the days either side of the sync; **every session
since has skipped it**, which is the second silent drift this page was hiding.

This was **not** backfilled, for the reason the generator itself states:

> No Summary line (a file written before this convention, or one that skipped it):
> emit the bare link rather than inventing prose. **The gap is visible.**

Writing 70 summaries would mean authoring prose in 70 other sessions' files. The gap
is honest, the files are all reachable, and for 29 of them the long-form note sits
in the same page. Going forward the convention already binds — the hook does not
enforce it, so a session that skips it gets a visibly bare row.

## 6 · What this push carries that is not mine

Precondition 5 says name them here, not only in the reply, because in a month this
file is where it is still findable. `git log @{u}..HEAD` at push time held **three
foreign commits**, both owning sessions carrying an **explicit release note** on the
board — which settles liveness on its own and needs no further check:

| Commit | Session | Adjudication |
|---|---|---|
| `bc5ef69` | `51e-sweep-components` | released, with a full account in *Recently released* |
| `55bd5f4` | `51e-sweep-components` | its claim commit |
| `dad8f12` | `kb-drain-51d` | released; drain-only, its pages landed in JARVIS at `74b00c2` |

**The one deletion in the range is `dad8f12`'s**, of
`kb-candidates/2026-08-17-51d-dialog-locale.md`. Deletions are always-ask, and this is
the one carve-out: a `kb-candidates/` file whose every entry was promoted, deleted in
the same commit as the drain. **Checked rather than taken on trust** — `74b00c2` exists
in `C:\Dev\JARVIS` and carries the three pages its message claims.

**`51e-sweep-components` has work left unpublished, and it is not mine to finish.**
That session's own `CHANGELOG/2026-08-17/51e-sweep-components.md` sits **modified in the
working tree** (+135 lines) after its release note was written — it adds the `Summary:`
line it had been missing, and a section recording that its `#51` issue comment is
**owed**, blocked by a GitHub 503 rather than skipped. Nothing of theirs was staged,
committed or pushed by this session; explicit pathspecs throughout.

**Their edit and this one interlock, in the direction that proves the design.** Their
new `Summary:` line means the row this session generated for them — currently a bare
link — becomes stale the moment they commit, and the hook will make them regenerate it.
That is the mechanism working, not a conflict. Their file also records that
`51e-sweep-components.issue-comment.md` was folded back into the changelog **because
this session's hook objected to it**: a hook installed mid-flight changed a live
sibling's behaviour, with no artifact in the repo to warn them. Filed as KB candidate 2.

## 🧪 Tests

**No app code changed, so no Kotlin/Firestore layer applies** — JVM unit,
instrumented, and `firestore-tests/` were not run and are not relevant to this
change. The layers that *do* exist for this change were exercised:

| Check | Result |
|---|---|
| Script copies identical to JARVIS source | **PASS** — SHA-256 equal for both files |
| All 36 hand-written rows present verbatim after restructure | **PASS** — 0 missing / 36 |
| …and the checker provably detects a loss | **PASS** — positive control on a doctored file reported exactly 1 |
| Generator round-trips (second run is a no-op) | **PASS** — run 2 and `-Check` both report *current* |
| Every day-folder session file linked from the index | **PASS** — 0 unlinked |
| Every flat legacy file linked | **PASS** — 7 of 7 |
| Day folders on disk vs generated rows | **PASS** — 15 = 15 |
| Every old row's *first* link resolves to a real file | **PASS** — 0 broken of 29 |
| Line endings / BOM preserved | **PASS** — 0 CR bytes, no BOM, as before |
| Hook **blocks** a commit whose changelog file is not in the index | **PASS** — exit 1, recipe printed |
| `-Check -Staged` passes once regenerated from the index | **PASS** — exit 0 |

**Two of this session's own verification instruments were broken, and both were caught
only by testing the instrument rather than reading its output.**

1. `grep -qxF "$line"` parsed a row beginning `- [` as an option bundle and reported
   **all 36** rows missing. Loud, so harmless — but the fix (`-e`) shipped with a
   **positive control**: the checker was re-run against a deliberately doctored file
   and had to report exactly 1 loss. "0 missing" means nothing until the instrument has
   been shown to detect a real one.
2. A greedy `sed 's/.*](\([^)]*\)).*/\1/'` extracted the **last** link on each row —
   an issue URL — and reported 13 of 29 rows as broken. This one was **quiet**: 13/29
   is a plausible number for a rotted index, and it would have been believed. The
   non-greedy form reports 0 of 29.
3. `grep -c $'\r'` was silently matching **every** line, because the pattern expanded
   empty. Replaced with a byte count via .NET, which is what actually proved 0 CR.

Three broken checks in one session, all on the *verification* side and none on the
work being verified — which is the argument for re-running what consumes the output
rather than reading it.
