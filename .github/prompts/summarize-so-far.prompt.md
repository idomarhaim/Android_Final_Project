<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->
---
mode: agent
description: "Incremental 'so-far' summary on the current branch, starting from the previous so-far checkpoint (or branch root)."
---

# /summarize-so-far

Generate an **incremental** summary covering work done since the last `--so-far-*` checkpoint on this branch (or since the branch root if none exists).

## 🎯 Output

Write to `CHANGELOG/branch-summaries/<branch>--so-far-<YYYY-MM-DD>.md` (today's date, never future).

## 📥 Inputs

1. **Branch**: `git rev-parse --abbrev-ref HEAD`
2. **Checkpoint** (in order of preference):
   1. Most recent existing `CHANGELOG/branch-summaries/<branch>--so-far-*.md` — use its generation date as the start.
   2. Otherwise: branch merge-base with the default branch.
3. **Commits since checkpoint**: `git log --oneline --no-decorate <checkpoint-ref>..HEAD`
4. **Changelogs since checkpoint date**: every `CHANGELOG/YYYY-MM-DD.md` with date ≥ checkpoint date and ≤ today.
5. **TODO diffs since checkpoint**.

## 📝 Output format

```markdown
# ⏱️ So-Far Summary — <branch>

> 📅 Generated: <YYYY-MM-DD>
> 🔁 Previous checkpoint: <prior so-far filename or "branch root">
> 🔢 New commits since checkpoint: <N>

## 🎯 What changed since the checkpoint
<one short paragraph>

## 🆕 New Features
## 🔧 Bug Fixes
## 📚 Documentation
## 🔧 Technical Changes
## 🧪 Tests
## ✅ TODO items closed since checkpoint

## 📁 Files changed since checkpoint
| Status | Path |
|--------|------|
| A/M/D | <path> |

## 🔗 References
- Previous checkpoint: <filename or "n/a — branch root">
- Changelogs aggregated: [<date>](../<date>.md), …
```

## 🛡️ Rules
- Always write a new file — don't overwrite previous so-far snapshots. They form a timeline.
- If today already has a `<branch>--so-far-<today>.md`, append a `## Round N` section instead.
- Date stamp must be today.
- Bootstrap rule: if this prompt is missing from the project, copy it from user-level templates first.
