<!-- SOURCE: user-template v4; do not edit in-project, edit user-level then re-sync -->
---
mode: agent
description: "Create or append to this session's CHANGELOG/YYYY-MM-DD/<session-label>.md following the canonical format."
---

# /new-changelog-entry

Create today's changelog entry (or append a `## Round N` block to an existing one) using the canonical format defined in `general.instructions.md`.

## 📥 Inputs (collect from the user if not obvious)
- Brief summary of what was done in this round.
- Sections that have content: `🆕 New Features`, `🔧 Bug Fixes`, `📚 Documentation`, `📁 New / Modified Files`, `🔧 Technical Changes`, `🧪 Tests`.
- Test results (from latest `pytest` / `vitest` / etc. run).

## 🛠️ Steps
1. Resolve today's date in `YYYY-MM-DD` (system clock, never future).
2. **Resolve the active git branch** by running `git rev-parse --abbrev-ref HEAD`. This is mandatory — never skip it, never guess. If multiple rounds were authored on different branches, list them comma-separated in chronological order.
3. If `CHANGELOG/YYYY-MM-DD/<session-label>.md` exists (one folder per day, one file per session — never write in another session's file):
   - Append `## Round <N+1>` with all populated sections.
   - If the current branch differs from the one in the existing `> **Branch:**` line, update that line to the comma-separated list.
4. Else → create the file with the canonical header, the branch line **on line 3** and the summary line **on line 4**, exactly:
   ```markdown
   # Changes — DD/MM/YYYY

   > **Branch:** `<branch-name>`
   > **Summary:** <one sentence — what this session changed>
   ```
   then the populated sections. Neither line is optional. The summary is one line, no line breaks and no `|` — it is lifted verbatim into a table cell in the day index, and it is the **only** thing this session contributes there.
5. Refresh `CHANGELOG/CHANGELOG_README.md`'s **Available Files** table — it is **derived content, never hand-written prose**:
   - Where a generator exists, run it and change nothing by hand (JARVIS: `powershell -File scripts\New-ChangelogIndex.ps1`; a `-Check` run is wired into pre-commit, so a stale index blocks the commit).
   - Otherwise re-read the file, then append **only your own** `[\`<session-label>\`](<day>/<session-label>.md) — <your summary>` segment to the day row, creating the row if the day is not listed yet. Never touch another session's segment.
   - Prose covering the whole day goes in `CHANGELOG/YYYY-MM-DD/SUMMARY.md`, not in the index.
6. **Self-check before declaring done**: re-open the file and confirm line 1 is `# Changes — …`, line 3 is `> **Branch:** \`…\`` and line 4 is `> **Summary:** …`. If not, fix it.
7. Confirm with the user before saving if anything is ambiguous.
