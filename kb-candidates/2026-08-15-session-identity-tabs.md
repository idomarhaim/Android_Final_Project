---
session: session-identity-tabs
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
created: 2026-08-15
mode: AUTO MODE
transcript: ~/.claude/projects/c--Dev-Android-Final-Project/a54d79b9-c8df-496e-a34b-9ba532942035.jsonl   # breadcrumb, never a source
---

# KB candidates — `session-identity-tabs`, 2026-08-15

## 1. A transcript's *first* message identifies the session; it does not identify the session's *current task*

- **Claim:** `§5.3(c)`'s transcript check answers **"is this session alive?"** and nothing else.
  To learn **what a live session is working on**, read its **last** turns. The opening user
  message — and therefore the VSCode tab title, which is a summary of that message — says only
  what the session was asked *first*, which for a resumed session may be days stale. Two sessions
  whose tab titles are near-identical can be doing entirely unrelated work, and one session's
  title can name a task it finished and moved on from.
- **Why:** This session was given `/wayfinder 12` and, finding the map's frontier empty, ran the
  §5.3(c) transcript check to see who held the `#12` singleton. It found **four** transcripts
  whose first user message was the byte-identical prompt Ido had just sent, all with recent last
  turns, and reported to Ido that he had *"fired this prompt into four sessions"* with *"three
  racing an empty frontier"* — recommending he close two of them. **That was wrong.** Reading the
  last turns instead showed `17b2f09b` and `6e0244ad` had **opened with that prompt on
  2026-08-12, three days earlier**, and had long since moved to other work — `6e0244ad` was in
  `C:\Dev\JARVIS` editing `rules/agent-topology-and-model-routing.md` with an armed watch on a
  `sibling-wait-banner` session, nothing to do with `#12` at all. Only `e27c382c`
  (`c21-offline-story`) had the assignment as live work, and it had already resolved and closed
  [`#43`](https://github.com/idomarhaim/Android_Final_Project/issues/43) nine minutes before this
  session opened. **The recommendation would have killed a session mid-unit.**
  The failure is specific and repeatable: Ido **reuses prompt phrasings**, so an opening message
  is not even a unique key, let alone a current one — and the tab strip renders the collision as
  visual triplets, which is what prompted his *"which of these are doing the same task?"* and
  exposed the error.
- **Destination:** `kb/dev/claude-code-surfaces.md` — the **Session identity** section already
  there (written 2026-08-13 from `session-titles` entries 1, 2 and 5). This is a **limit** on
  that section, not a correction of it.
- **Anchors:** transcripts `17b2f09b`, `6e0244ad`, `e27c382c`, `80fd41f6` (all
  `~/.claude/projects/c--Dev-Android-Final-Project/`);
  `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.3(c);
  `kb-candidates/2026-08-13-session-titles.md` entry 4 (the parent claim this bounds).
- **Supersedes:** nothing. §5.3(c) is **correct as written** and this does not narrow it — it
  bounds what a reader may *additionally* conclude from the same artifact. Filed as a separate
  entry rather than an amendment for exactly that reason.
- **Status:** 🟢 pending — `AUTO MODE`-eligible (a KB page, not `rules/`, and supersedes nothing).
  **Not drained by this session:** the write lands in `C:\Dev\JARVIS`, which needs a board read
  and claim there, and this session produced no other unit to carry it. Next session in that repo
  takes it.
