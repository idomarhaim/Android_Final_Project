<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->
---
applyTo: "**"
description: "Layered testing discipline: every feature must produce, run, and document tests at every relevant layer."
---

# 🧪 Testing Discipline

## 🎯 Rule
Every **new / modified / planned** feature must produce tests at every layer the project actually has. Then the tests must be **run** and the results **documented in today's changelog**.

## 📚 Layers (skip a layer only if it doesn't exist in the project)

| Layer | What to test | Typical tools |
|-------|--------------|---------------|
| **Server (unit)** | Pure functions, helpers, business logic | `pytest`, `unittest`, `jest` |
| **Server (integration)** | Service ↔ DB, service ↔ external API (mocked) | `pytest`, `testcontainers` |
| **Endpoints (HTTP contract)** | Status codes, payload shape, auth, error paths | `httpx`/`requests`, `supertest` |
| **Database** | Migrations apply, queries return expected rows, repository methods | `pytest` + ephemeral DB |
| **Client (component)** | Render, props, conditional UI, hooks | `vitest` + `@testing-library/react` |
| **Client (page)** | Page-level state, routing, data-fetch glue | same as above |
| **UI (E2E)** | User flows: login → action → assertion | `playwright`, `cypress` |

## 🗂️ Folder layout
- Each major component owns a `tests/` folder mirroring its source layout.
- Each `tests/` folder has a `tests/README.md` documenting:
  - 🎯 Scope of this test suite
  - ▶️ How to run (exact command)
  - 🧪 Fixtures and how to refresh them
  - ⚠️ Known gaps / pending coverage

## ▶️ Execution
- After writing tests, **run them** — don't claim a feature done without seeing the results.
- Capture: pass count, fail count, skipped count, total time.
- For failures: copy the assertion message + the test name into the changelog.

## 📋 Changelog `🧪 Tests` section
Required format in today's `CHANGELOG/YYYY-MM-DD.md`:

```markdown
## 🧪 Tests
- **Layers covered**: server-unit, endpoints, client-component  *(skipped: e2e — Playwright not configured)*
- **Results**: 42 passed · 0 failed · 1 skipped in 3.4s
- **New / updated test files**:
  - `server/tests/test_inference.py`
  - `client/src/components/__tests__/PatientCard.test.tsx`
- **Failures**: none
```

If anything failed, list each failure as:
```markdown
- ❌ `test_name` — assertion / error message
```

## 🚫 Anti-patterns
- "It compiles, ship it." → ❌. Run the suite.
- Generating tests but not running them → ❌.
- Skipping a layer silently → ❌. Either run it or explicitly say "skipped because <reason>" in the changelog.
- Asserting on implementation details (private fields, internal call order) → ❌. Assert on observable behavior.
