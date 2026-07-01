<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# ✍️ Authoring Instruction Files

Meta-guide for writing **effective** AI-agent instruction, prompt, and skill files. Optimized for **agent behavior quality** and **token efficiency**.

## 🧭 Pick the right file type

| Need | File type | Where | Auto-loaded? |
|------|-----------|-------|--------------|
| Rule that applies to every turn | `general.instructions.md` (`applyTo: "**"`) | `.github/instructions/` | ✅ always |
| Rule that applies only to certain files | `<scope>.instructions.md` with narrow `applyTo` | `.github/instructions/` | ✅ when matched files are in context |
| Reusable command the user types as `/name` | `<name>.prompt.md` | `.github/prompts/` | ❌ invoked explicitly |
| Multi-step workflow with conditionals & tool restrictions | `SKILL.md` under a `skills/` folder | project or user-level | ❌ invoked via skill matcher |
| Cross-agent project context (Claude, Codex, Cursor, Copilot) | `AGENTS.md` at repo root | repo root | depends on agent |

## 🪙 Token budget

Always-loaded files (`copilot-instructions.md` + every `applyTo: "**"` instruction + `AGENTS.md`) get added to **every turn**. Keep them small:

- `copilot-instructions.md`: ≤ ~20 lines. Just a pointer.
- `general.instructions.md`: ≤ ~80 lines. Pure rules, no examples.
- `testing.instructions.md`: ≤ ~80 lines.
- `AGENTS.md`: ≤ ~150 lines. Links to docs, doesn't restate them.

If a rule is long, narrow its `applyTo` so it only loads when relevant.

## ✅ Effective rule patterns

- **Imperative, not narrative**: "Use `pathlib.Path` over `os.path`." not "We tend to prefer using pathlib in our codebase…"
- **One rule per bullet**. No nested compound rules.
- **Cite the why only when non-obvious** — agents follow rules better when they understand the cost of breaking them.
- **Link, don't restate**. `See [ARCHITECTURE.md]` beats pasting the architecture.
- **Concrete > abstract**. "Don't catch broad Exception" beats "handle errors gracefully".

## 🚫 Anti-patterns

- ❌ Duplicating rules across `AGENTS.md`, `copilot-instructions.md`, and `general.instructions.md` → pick one home.
- ❌ Vague rules like "write clean code" → unenforceable, wastes tokens.
- ❌ Lists of "don't"s with no positive guidance → agent won't know what to do instead.
- ❌ Multi-page instruction files → split by `applyTo` scope.
- ❌ Hand-editing a synced template inside a project → edit the user-level template and re-sync.

## 🔁 Sync workflow for generic files

1. Edit the file in `c:/Users/namei/AppData/Roaming/Code/User/prompts/templates/...`
2. Bump `v<N>` in the SOURCE marker.
3. Add a row to `templates/CHANGES.md`.
4. Open each affected project → agent detects the version drift and offers a diff → confirm re-sync.

## 🗂️ Project-specific files

Don't template these — they describe one project:
- `AGENTS.md` (instantiated from `AGENTS.md.template`, then maintained in-repo)
- `.github/instructions/<domain>.instructions.md` (e.g., `ai-pipeline`, `server`, `react-client`)
- `TODO/<Area>.TODO.<priority>.md`

## 📐 `applyTo` glob patterns

| Goal | Pattern |
|------|---------|
| Everything | `**` |
| All Python | `**/*.py` |
| All notebooks | `**/*.ipynb` |
| Server-side Python | `server/**/*.py` |
| React client | `client/src/**/*.{js,jsx,ts,tsx}` |
| Tests only | `**/tests/**` |

## 📋 Reviewing an instruction file
Before committing a new or edited instruction file, check:
- [ ] SOURCE marker present (if it's a synced template).
- [ ] `applyTo` is as narrow as the rule allows.
- [ ] No rule is repeated in another always-loaded file.
- [ ] Every "don't" has a "do this instead" counterpart.
- [ ] Line count is within budget for its scope.
- [ ] An entry exists in `.github/instruction-file-catalog.md`.
