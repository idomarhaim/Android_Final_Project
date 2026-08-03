<!-- SOURCE: user-template v3; do not edit in-project, edit user-level then re-sync -->
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
4. Else → create the file with the canonical header and the branch line **on the second line**, exactly:
   ```markdown
   # Changes — DD/MM/YYYY

   > **Branch:** `<branch-name>`
   ```
   then the populated sections. The branch line is **not optional**.
5. Update `CHANGELOG/CHANGELOG_README.md`'s **Available Files** table:
   - If today's row exists, extend its description with the new round summary.
   - Else, prepend a new row at the top.
6. **Self-check before declaring done**: re-open the file and confirm line 1 is `# Changes — …` and line 3 is `> **Branch:** \`…\``. If not, fix it.
7. Confirm with the user before saving if anything is ambiguous.
