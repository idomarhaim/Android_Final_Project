<!-- SOURCE: user-template v10; do not edit in-project, edit user-level then re-sync -->
---
applyTo: "**"
description: "Cross-cutting project conventions: language, documentation, changelogs, TODOs."
---

# 📌 General Conventions

## 🌐 Language
- All output (code, comments, file contents, commit messages, docs) is in **English**.
- **Always reply to the user in English**, even if they write in another language. No exceptions.

## 📄 Required project docs
At the project root, ensure these exist (create if missing, with minimal stubs):
- `README.md`
- `ARCHITECTURE.md`
- `AGENTS.md`
- `CHANGELOG/CHANGELOG_README.md`
- `TODO/TODO.md`
- `.github/authoring-instructions.md`
- `.github/instruction-file-catalog.md`

## 📋 Changelog Rules
- One file per date: `CHANGELOG/YYYY-MM-DD.md`. Multiple change rounds on the same date go into the same file as `## Round 1`, `## Round 2`, …
- **Use today's date only — never future-date a changelog.**
- **⚠️ MANDATORY branch line.** Every changelog file MUST have, on line 3 (one blank line after the H1 title):
  ```
  > **Branch:** `<branch-name>`
  ```
  Get the branch by running `git rev-parse --abbrev-ref HEAD` **before** writing the file — never guess, never skip. If rounds span multiple branches, list them comma-separated in chronological order. This rule fires on **every** changelog creation or round-append, regardless of whether the `/new-changelog-entry` slash-command was used. After writing, re-read the first 5 lines and verify the branch line is present; fix immediately if missing.
- Each file follows this structure:

  ```markdown
  # Changes — DD/MM/YYYY

  > **Branch:** `<branch-name>`

  ## 🆕 New Features
  ## 🔧 Bug Fixes
  ## 📚 Documentation
  ## 📁 New / Modified Files
  ## 🔧 Technical Changes
  ## 🧪 Tests
  ```

- `CHANGELOG/CHANGELOG_README.md` keeps an **Available Files** table — append every new entry.

## ✅ After completing a task
1. Update `README.md` / `ARCHITECTURE.md` if behavior or structure changed.
2. Append / create today's `CHANGELOG/YYYY-MM-DD.md`.
3. Update the **Available Files** table in `CHANGELOG_README.md`.
4. If the task came from `TODO/`, mark its checkbox `[x]` only **after user confirmation**.

## 🤖 Agent operations
- **Committing follows the global _Commits & pushing_ rule.** When a unit of work is finished — tests passing if it has them, or the activity done if it needs none — update today's CHANGELOG first, then commit **using that CHANGELOG entry's text as the message (copy, don't rewrite)**. In **auto mode** (the user's message starts with `AUTO MODE`) commit automatically; in **normal mode** (default) ask before committing. Where a `/finish-task` command exists, it runs this docs + CHANGELOG + commit flow.
- **Never push** without explicit OK, and never push to the default branch (`main`/`master`) without confirmation — prefer a feature branch + PR.
- **Committed docs outrank memory.** If a recalled memory contradicts a committed doc (AGENTS.md, README, `docs/`), the doc wins — correct or delete the stale memory.
- **Graduate durable insights.** When a Q&A yields a reusable, non-obvious insight, save it to the knowledge layer (`/kb-ingest` where available, else `/save-to-kb`) rather than leaving decision-grade facts only in private agent memory. Cross-project insights → the central KB (`C:\Dev\JARVIS\kb`); project insights → the project's `knowledge/` bundle. Vendor agent memory holds pointers, not substance — full rule: `C:\Dev\JARVIS\rules\memory-promotion.md`.
- **Flag KB candidates as they happen — don't wait for the session end.** The moment something durable lands (a decision, work worth recording, or an explanation the user asked for and learned from), append one line to that reply: `📌 **KB candidate:** <topic> → <destination>` — then carry on. No ingest yet; flags collect into the session's candidate list, which the finish protocol ingests (approved items) and `/handoff` carries (unresolved ones). The user can add candidates any time with `/kb-flag <topic>`, or run `/kb-flag` bare after a Q&A to make the agent list every candidate it sees with a proposed destination.
- **Climb the decision ladder before writing code** — *lazy about the solution, never about reading*: (1) does it need to exist at all? (2) already in this codebase? (3) standard library? (4) platform feature? (5) already-installed dependency? (6) can it be one line? (7) only then write the minimum that works. Bug fix = root cause, not symptom. Full rule: `C:\Dev\JARVIS\rules\decision-ladder.md`.

## 🗂️ TODO discipline
- Backlog lives under `TODO/`. Index is `TODO/TODO.md`.
- Per-area files are partitioned into three priority subfolders next to the index:
  - `TODO/TODO_MUST/<Area>.TODO.must.md`
  - `TODO/TODO_OPTIONAL/<Area>.TODO.optional.md`
  - `TODO/TODO_FUTURE/<Area>.TODO.future.md`
- Status legend: `[ ]` todo · `[~]` in-progress · `[x]` done · `[-]` deferred.
- Finish all `must` items before any `optional`; `future` is roadmap.
- New tasks go in the file under the matching subfolder; create a new `<Area>.TODO.<priority>.md` if no area fits, then link it from `TODO/TODO.md`.
- **Tracker hybrid (one-way TODO→Issue).** `TODO/` is the **pre-commitment funnel**: ideas and not-yet-committed work. When an item graduates to committed execution, open a **GitHub Issue** for it (`gh issue create`), annotate the TODO entry with the issue number, and close the TODO entry. Issue state is **never mirrored back** into `TODO/` — the issue tracker owns committed work from that point on.

## 🎨 Style
- Use emojis in Markdown headings (📦, 🏗️, 📝, 🚀, 🆕, 🔧, 📚, 📁, 🧪).
- Keep replies terse — the user can read diffs.

## ❓ Ambiguity
- If a task is unclear or under-specified, ask before starting. Don't guess.

## 🛡️ Safety
- No destructive shortcuts (`--no-verify`, `git reset --hard`, force-push, `rm -rf` outside build artifacts) without explicit confirmation.
- No git worktrees unless the user asks for one.
