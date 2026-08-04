# Changelog

**One folder per day, one file per session:** `CHANGELOG/YYYY-MM-DD/<session>.md`,
so parallel sessions never write the same file. Entries before 2026-08-03 predate
that convention and are flat `YYYY-MM-DD.md` files; both forms are listed below.
Each entry summarises what changed, why, and — per the testing discipline — a
`## 🧪 Tests` section with pass/fail counts and covered layers.

- [2026-07-01.md](2026-07-01.md) — Initial GoalPilot implementation (Core MVP + full scaffold).
- [2026-07-02.md](2026-07-02.md) — Align commit-workflow docs with the new Commits & pushing rule.
- [2026-07-15.md](2026-07-15.md) — `/jarvis-ize` governance pass (scaffold v10/v4, `knowledge/` bundle).
- [2026-07-31.md](2026-07-31.md) — Fix pass: GROQ/Node deprecations, LLM features shipped, social correctness fixes, instrumented tests run for the first time.
- [2026-08-01.md](2026-08-01.md) — `scripts/` one-click launchers: run the emulator/phone loop without Android Studio.
- [2026-08-02.md](2026-08-02.md) — Emulator black screen + SystemUI ANR diagnosed; AVD hardware profile retuned for this host (no discrete GPU). · Health Connect integration shipped: steps & sleep, read-only, review-before-write, `ProgressEntry.sourceKey` for exact dedupe.
- [2026-08-03.md](2026-08-03.md) — `scaffold` session: template-library upgrade (`AGENTS.md` v8→v10, `general.instructions.md` v10→v12, `SESSIONS.md` v1→v2). *(Flat file: written before day folders.)*
- [2026-08-03/lifeareas.md](2026-08-03/lifeareas.md) — Life areas (user-defined + synced from Google Tasks list names), LLM task-duration estimates, the interactive "where your time goes" donut at day/week/month/quarter/year, and entry animations for every chart.
- [2026-08-04/challenges.md](2026-08-04/challenges.md) — `challenges` session: the security-rules change that makes joining a challenge possible (participants subcollection), and `firestore-tests/`, the project's first security-rules test layer.
- [2026-08-04/time-insights.md](2026-08-04/time-insights.md) — `time-insights` session: a stacked-column trend beside the time-allocation donut (buckets that tile every range exactly), and an AI re-estimation pass for tasks that never had a duration — which surfaced a second, undocumented `scoreTask` fallback signature that would have been written as a genuine AI estimate.
