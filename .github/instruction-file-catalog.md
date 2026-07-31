<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 📇 Instruction File Catalog

Audit table for every instruction / prompt / skill file in this project. Source column distinguishes synced user-level templates from project-specific files.

| Path | Type | `applyTo` | Source | Last reviewed | Owner |
|------|------|-----------|--------|---------------|-------|
| `.github/copilot-instructions.md` | copilot entry | (always) | user-template v1 | YYYY-MM-DD | |
| `.github/instructions/general.instructions.md` | instructions | `**` | user-template v1 | YYYY-MM-DD | |
| `.github/instructions/testing.instructions.md` | instructions | `**` | user-template v1 | YYYY-MM-DD | |
| `.github/instructions/python.instructions.md` | instructions | `**/*.py` | user-template v1 | YYYY-MM-DD | |
| `.github/instructions/notebooks.instructions.md` | instructions | `**/*.ipynb` | user-template v1 | YYYY-MM-DD | |
| `.github/prompts/summarize-branch.prompt.md` | prompt | (slash) | user-template v1 | YYYY-MM-DD | |
| `.github/prompts/summarize-so-far.prompt.md` | prompt | (slash) | user-template v1 | YYYY-MM-DD | |
| `.github/prompts/new-changelog-entry.prompt.md` | prompt | (slash) | user-template v1 | YYYY-MM-DD | |
| `AGENTS.md` | cross-agent entry | (always) | project-specific (from template) | YYYY-MM-DD | |
| `CLAUDE.md` | claude entry | (Claude only) | project-specific (from template) | YYYY-MM-DD | |
| (project-specific instruction files) | instructions | (varies) | project-specific | YYYY-MM-DD | |

## 🔁 Re-sync flag
Mark a row with `⚠️ drift` in the **Source** column when the user-level template has bumped its `v<N>` and the in-repo copy hasn't been refreshed yet.

## 📋 Add a row
Every time a new instruction / prompt / skill file is added, append a row.
