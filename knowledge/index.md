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
| [release-distribution.md](release-distribution.md) | Getting an APK to other people and updating it afterwards: off-Play means no update mechanism, the signing key is unrecoverable so it precedes the first hand-out, the updater is release-only by construction, and why the workflow triggers on a tag rather than a push (`versionCode` is manual). |
| [ui-error-conventions.md](ui-error-conventions.md) | The `Resource.Error` house rule refined at its boundary: surface the repository's own text for refusals the **domain** generated, substitute a written message for failures the **network** generated. The convention was right and the call site followed it correctly, and a raw gRPC `UNAVAILABLE: Unable to resolve host…` still reached a real screen — a boundary case inside the rule, not a misapplication of it, and one only a real screen catches. |
| [goal-measurement.md](goal-measurement.md) | When a goal resists measurement, measure the **behaviour that produces it** — a leading indicator (*"read 2 market reports a week"*) rather than a forced outcome number or silence. `E6` reaches for the construction without naming it; `C7` makes unmeasured goals legal but never silent, so the suggestion must be concrete, dismissible, non-auto-applying, and have a non-AI fallback. |
