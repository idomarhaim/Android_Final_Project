<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 🤖 GitHub Copilot — Project Entry Point

**Read [AGENTS.md](../AGENTS.md) first.** It is the cross-agent source of truth for this project.

## 📚 What lives where

- **Cross-agent rules** → [AGENTS.md](../AGENTS.md)
- **Path-scoped rules** → `.github/instructions/*.instructions.md` (auto-loaded by Copilot per `applyTo` pattern)
- **Slash-commands** → `.github/prompts/*.prompt.md` (e.g., `/summarize-branch`, `/summarize-so-far`)
- **TODO backlog** → [TODO/TODO.md](../TODO/TODO.md)
- **Changelog** → `CHANGELOG/YYYY-MM-DD.md` (today's date only)
- **Instruction authoring guide** → [.github/authoring-instructions.md](authoring-instructions.md)
- **Instruction file catalog** → [.github/instruction-file-catalog.md](instruction-file-catalog.md)

## 🔁 Session-start checks

1. Verify every file in the user-level `templates/` exists at the matching repo path. If missing → copy.
2. Compare each repo file's `SOURCE: user-template v<N>` marker against user-level. If user-level is newer → show diff and re-sync after user confirmation.
3. If `AGENTS.md` doesn't exist, scaffold it from `templates/AGENTS.md.template` and ask the user for the project-specific sections.

Keep this file **short** — it is loaded into context on every turn.
