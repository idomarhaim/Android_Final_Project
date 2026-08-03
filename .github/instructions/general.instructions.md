<!-- SOURCE: user-template v14; do not edit in-project, edit user-level then re-sync -->
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
- **One folder per date, one file per session:** `CHANGELOG/YYYY-MM-DD/<session-label>.md`. Multiple rounds by the **same** session go into **its own** file as `## Round 1`, `## Round 2`, …
  - `<session-label>` is this session's row on `SESSIONS.md`; use `main` when the repo has no board or you are the only session. Kebab-case, no spaces.
  - **Always the folder, even when working alone.** You cannot know you are alone until you have read the board, and by then you have already picked a path. A conditional layout is a layout agents get wrong.
  - **Why:** `CHANGELOG/YYYY-MM-DD.md` was the one file *every* session had to write *every* day, so no session could claim it and the claim board was structurally unable to protect it. Sibling sessions overwrote and truncated each other's entries repeatedly (JARVIS, 2026-08-03, three repair commits in seven minutes). Disjoint paths make the collision impossible instead of merely forbidden — the same principle that already keeps code safe.
  - **Never write into another session's file.** If their entry is wrong, say so; don't fix it in their file.
  - **Migration, deterministic — no flag day:** if a flat `CHANGELOG/YYYY-MM-DD.md` **already exists** for the date you are writing, append to it as before; otherwise create the folder. So the last partly-flat day finishes flat and every later day is a folder, with no half-converted day and no rewriting of what sibling sessions already wrote. Never migrate an old flat file — that is history, not drift.
- **`SUMMARY.md` — the day's rollup**, at `CHANGELOG/YYYY-MM-DD/SUMMARY.md`. Written by the **first session of a later day**, never during the day it covers: by then nobody is writing those files, so the one file that *is* shared has zero contention. Check for a missing `SUMMARY.md` in the most recent past day folder as a session-start chore; it is a rollup of that day's files, not new analysis.
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

- `CHANGELOG/CHANGELOG_README.md` keeps an **Available Files** table — one row per **day** (link the folder), not per session file. It is a shared index, so re-read it immediately before editing and add only your own row.

## ✅ After completing a task
1. Update `README.md` / `ARCHITECTURE.md` if behavior or structure changed.
2. Append / create **your own** `CHANGELOG/YYYY-MM-DD/<session-label>.md`.
3. Update the **Available Files** table in `CHANGELOG_README.md` (one row per day; add the day's row if it isn't there yet).
4. If the task came from `TODO/`, mark its checkbox `[x]` only **after user confirmation**.

## 🤖 Agent operations
- **Committing follows the global _Commits & pushing_ rule.** When a unit of work is finished — tests passing if it has them, or the activity done if it needs none — update today's CHANGELOG first, then commit **using that CHANGELOG entry's text as the message (copy, don't rewrite)**. In **auto mode** commit automatically; in **normal mode** (the default) ask before committing. **The mode is sticky:** auto mode starts when a user message begins with `AUTO MODE` and stays on for the **rest of the session** — its absence from a follow-up message means nothing — until a message beginning with `NORMAL MODE`, or a new session. If you are unsure which mode you are in, you are in normal mode; and whenever auto mode makes you do something you would otherwise have asked about, say so in that reply. Auto mode never grants `OUTWARD AUTO`, which stays scoped to a single task. Where a `/finish-task` command exists, it runs this docs + CHANGELOG + commit flow.
- **Pushing is mode-gated too.** **Normal mode** (default): never push without explicit OK, and never push to the default branch without confirmation — prefer a feature branch + PR. **Auto mode** (`AUTO MODE`): push when the agent judges it right, without asking — including to `main` in a solo repo — but only while **all six preconditions** hold: (1) tests green at every layer the project has and today's CHANGELOG written; (2) the outgoing diff actually read (`git log @{u}..HEAD`, `git diff --stat @{u}..HEAD`) with no deleted/renamed file, secret, large binary, or out-of-scope change in it — deletions stay always-ask; (3) **fast-forward only** — `--force`, `--force-with-lease`, `--delete`, `+refspec`, moving a published tag, or pushing a rebase of already-pushed commits are destructive shortcuts, always-ask in both modes; (4) `git fetch` first, never push over commits you cannot account for; (5) sibling sessions respected, remembering that `git push` is **branch-scoped, not commit-scoped** — their *uncommitted* work must never ride along (that is the staging rule: explicit paths, never `git add -A`), while their *committed* work unavoidably will, so read the outgoing log and name in the reply any commit going up that is not yours; (6) a repo with other contributors or a PR workflow stays branch + PR. Pushing is the **dev half of outwardness only** — it neither needs nor grants `OUTWARD AUTO`, and opening/merging a PR, deleting a remote branch or tag, publishing a release, and changing repo settings are **not** pushes: they stay always-ask. Full rule: the global _Commits & pushing_ rule.
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
