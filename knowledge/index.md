# GoalPilot Knowledge Bundle (OKF)

**Format:** Open Knowledge Format (OKF) — Markdown pages plus this `index.md`
(curated index — every page must be reachable from here) and a [log.md](log.md)
(append-only ingest journal). Operated with the loop:
**ingest** (`/kb-ingest <this bundle>`) · **query** (grep + this index) · **lint** (`/kb-lint`).

Pages capture **durable "why" knowledge** — decisions, conventions, evaluations —
that outlives any single session and travels with this repo.

**Boundary vs other sources (link, don't restate):**

| Source | Owns |
|---|---|
| [AGENTS.md](../AGENTS.md) | How to work in this repo — rules, pitfalls, commands. |
| [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) | What the system is — layers, data model, data flow. |
| [docs/SETUP.md](../docs/SETUP.md) | Credentials + environment wiring. |
| Central KB (`C:\Dev\JARVIS\kb`) | Cross-project dev decisions and conventions. |
| **This bundle** | GoalPilot-local why-knowledge: project decisions, trade-offs, evaluations. |

## Pages

| Page | Topic |
|---|---|
| [deployment-conventions.md](deployment-conventions.md) | Cross-project deployment decision matrix (synced template, v1) — applies here to Firebase rules + Cloud Functions deploys. |
