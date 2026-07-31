<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->
---
mode: agent
description: "Produce a full branch summary at CHANGELOG/branch-summaries/<branch>.md aggregating every changelog and commit on the current branch."
---

# /summarize-branch

Generate a single, comprehensive summary of **everything done on the current branch** so far.

## 🎯 Output

Write to `CHANGELOG/branch-summaries/<sanitized-branch-name>.md`.

If the file exists, ask before overwriting (offer: overwrite / append-as-new-round / cancel).

## 📥 Inputs

1. **Branch name + merge-base**:
   - `git rev-parse --abbrev-ref HEAD` → branch
   - `git merge-base HEAD origin/main` (fall back to `origin/master` or repo default branch) → base
2. **Commits**: `git log --oneline --no-decorate <base>..HEAD`
3. **Changed files**: `git diff --name-status <base>..HEAD`
4. **Changelog entries**: every `CHANGELOG/YYYY-MM-DD.md` whose date is between the first commit date on this branch and today.
5. **TODO updates**: every `TODO/**/*.md` modified on this branch — note which items moved from `[ ]` → `[x]`.

## 📝 Output format

```markdown
# 🌿 Branch Summary — <branch>

> 📅 Generated: <YYYY-MM-DD>
> 🔢 Commits: <N>  ·  📁 Files touched: <M>
> 🏁 Base: <merge-base sha>

## 🎯 Purpose of this branch
<one paragraph synthesized from commit messages + changelog headlines>

## 🆕 New Features
<bulleted, deduplicated across all changelogs in range>

## 🔧 Bug Fixes
## 📚 Documentation
## 🔧 Technical Changes
## 🧪 Tests
- Total tests added / modified: <N>
- Layers covered: <list>
- Final run status (from latest changelog): <pass/fail summary>

## ✅ TODO items closed
- [x] <area> → <item> *(closed YYYY-MM-DD)*

## 📁 Files changed
| Status | Path |
|--------|------|
| A | <new files> |
| M | <modified> |
| D | <deleted> |

## 🔗 References
- Changelogs aggregated: [<date>](../<date>.md), …
- Commits: <first sha>..<HEAD sha>
```

## 🛡️ Rules
- If `CHANGELOG/branch-summaries/` doesn't exist, create it.
- Never invent content — every bullet must trace to a changelog entry, a commit, or a TODO diff.
- Date stamp must be **today**, never future.
- Bootstrap rule: if this prompt file is missing from the project, copy it from the user-level template before running.
