---
session: session-titles
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
created: 2026-08-13
mode: AUTO MODE
transcript: ~/.claude/projects/c--Dev-Android-Final-Project/ab9b1f29-be41-4fca-b5bf-6771da6c6e03.jsonl   # breadcrumb, never a source
---

# KB candidates — `session-titles`, 2026-08-13

**Drained 2026-08-13 under `AUTO MODE`.** Entries 1, 2, 3 and 5 are ingested into
`C:\Dev\JARVIS\kb` — entries 1, 2 and 5 as one new *Session identity* section on
`dev/claude-code-surfaces.md`, entry 3 as an extension to `dev/agent-topology-and-routing.md`
§ *Concurrent sessions*. Journal: `kb/log/2026-08-13.md`. `Check-KbLinks` **CLEAN at 65 pages**.
Nothing superseded.

**This file is rewritten down to its survivor, not deleted** — entry 4 is always-ask in both
modes and keeps its original number.

## Standing — always-ask

## 4. ⛔ A sibling session's liveness lives in its transcript, not in its commits

- **Claim:** To judge whether another session is still working, read **its transcript**
  (`~/.claude/projects/<project>/<uuid>.jsonl` — last turn, mtime), not only its commits and
  the working tree. A session mid-question makes no commits for hours and leaves the tree
  clean, and is nonetheless live.
- **Why:** This session told Ido that `c6-log-progress` had been *"silent for 44h"* and that
  its claim on `#22` was probably stale — derived from `git log` alone, and **wrong**. The
  transcript showed it working two hours earlier, with its own last status line reading
  *"#22 is still open … the session is not finished."* Had the claim been treated as stale,
  a second session would have taken a ticket that was three-quarters resolved. The board rule
  already warns that an absent row is not proof (`SESSIONS.md`, `board-claim-scope`,
  2026-08-06) and prescribes *read the log and the working tree* — both of which returned the
  wrong answer here. The transcript is the signal that didn't.
- **Destination:** ⛔ **`rules/agent-topology-and-model-routing.md` §5.3** — a behaviour change,
  so **always-ask in both modes**, and the 🎬 walkthrough gate owns it.
- **Checked bundle 2026-08-13.** `rg 'stale lease|stale claim|liveness|still working'` across
  `kb/` → one hit, `dev/disclosure-is-not-a-gate.md`, a different subject. **Re-checked at the
  drain:** §5.3 *What a session may conclude from state it did not write* was **written hours
  earlier the same day** by `picker-queue-merge`, on exactly this diagnosis — *a conclusion
  about another session is true only at the instant it is drawn, and every artifact §5 offers
  for drawing one is lossy in a way its own contents do not reveal*. This is a **third clause**
  for that section, beside (a) *a present row's date lies* and (b) *commit-don't-push does not
  hold anything back*. It also supplies what §5.3 says it lacks: every sighting there is a
  **reader misjudging**, and so is this one.
- **Anchors:** `SESSIONS.md` (the `c6-log-progress` row and its release note),
  transcript `3086aeaa-4ea7-4ee9-a692-b05d4b372546`,
  `rules/agent-topology-and-model-routing.md` §5.3.
- **Supersedes:** partially — it **narrows** §5's *"read the log and the working tree before
  concluding it is released"* by adding a signal that outranks both. Superseding a standing
  claim is itself always-ask, so this entry is doubly gated.
- **Status:** ✅ **drained 2026-08-13** by session `liveness-from-transcript` in `C:\Dev\JARVIS`
  (`e0c80fb`; claim `72b36ba`). Shipped as **§5.3 clause (c)** on Ido's `waive` — the strong form
  — with a declined-branch fallback over **12 instances** recorded beside the clause.
  **The entry's claim held; two of its three prescriptions did not**, and the drafting session
  found it only by running them: `mtime` is falsified in the *dangerous* direction (a
  title-backfill set four transcripts' mtimes to *now*, two of them dead since 08-10), so the
  clause reads the last `user`/`assistant` record's own `timestamp` and bans `stat`; and
  `grep -l <label>` returns every session that **read the board** — 12 hits for one owner, the
  owner ranked 7th by recency — so the clause keys on `file-history-*` records instead. The
  fallback additionally caught that one label can own **several** transcripts, which the first
  draft got wrong. Account: `C:\Dev\JARVIS\CHANGELOG\2026-08-13\liveness-from-transcript.md`.
