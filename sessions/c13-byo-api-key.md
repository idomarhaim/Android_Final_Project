---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: active
created: 2026-08-10
issue: https://github.com/idomarhaim/Android_Final_Project/issues/32
---

# Resolve `C13` — bring-your-own LLM API key

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`, and it should stay that way. [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) carries the `wayfinder:grilling` label, so it is **HITL**: it resolves through a live exchange with Ido, and an agent that answers its own grilling questions has broken the skill. `AUTO MODE` is wrong here.

**Read first** — [`AGENTS.md`](../AGENTS.md), then the map body [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) (low resolution — do **not** open every ticket), then the ticket itself, [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32).

**Task** — `/wayfinder 12 32`. Resolve `C13 · Bring-your-own LLM API key`, the map's **only unclaimed frontier ticket**. Planning only; this map ships no code.

## Carries over

Each with the committed path that proves it — nothing here rests on a transcript.

- **The free model is a *permanent* design constraint, and `C13` is explicitly the bonus beside it.** The map's Notes fix this, and *Out of scope* already rules out "a paid model tier as the default path": bring-your-own-key may be specced as an enhancement, but **nothing may be specced that requires it**. Source: map body [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12), §Notes and §Out of scope.
- **What the free model can actually do is measured, not guessed** — 248 live calls, 170/170 clean JSON parses, Hebrew no worse than English, one failure mode (silent id corruption), and one wide call beating three narrow ones. This is the baseline any "a better model would…" argument has to beat. Source: [`docs/research/2026-08-08-free-model-format-probe.md`](../docs/research/2026-08-08-free-model-format-probe.md) and the `C11a` gist on the map.
- **The GROQ key never ships in the client today.** Every model call goes through a Cloud Function proxy ([`functions/src/index.ts`](../functions/src/index.ts)), and the client only ever calls `getRecommendations` / `classifyTask` / `scoreTask` ([`RecommendationRepositoryImpl.kt`](../app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt)). A user-supplied key has to decide whether it breaks that — a key held on the device is a different security posture from a key held server-side, and both are different again from a key stored in Firestore.
- **Every AI feature already owes a non-AI fallback**, so a user key can only ever be a third rung: model-with-user-key → free model → local fallback. Whatever `C13` decides must not let a missing key degrade anything below where it already is.
- **Four AI features are now specced or being specced** and each would inherit a user key differently: the recommendation feed, task classification, task scoring, and — as of 2026-08-10 — the daily sentence, whose *quote* half needs no model at all. Source: [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29) resolution.
- **The map body carries no lease.** Re-fetch and hash `#12` immediately before writing to it; that is how four resolutions in a row have avoided clobbering a sibling.

## Out of scope

- **Every other ticket on the map.** Never resolve more than one per session. In particular [#25](https://github.com/idomarhaim/Android_Final_Project/issues/25) (`C9a`) and [#37](https://github.com/idomarhaim/Android_Final_Project/issues/37) (`C16`) were assigned and live on the board on 2026-08-10 — check `SESSIONS.md` before assuming they are free.
- **Writing any code.** The destination is `docs/PRODUCT_v0.3.md`; this map produces decisions.
- **Making a paid tier the default.** Already ruled out on the map; re-litigating it is out of scope, not a question.
- **Draining another session's `kb-candidates/` file.** Two partially-drained files were pending on 2026-08-10, each down to one **parked always-ask** entry awaiting Ido. Report them; do not ingest them without his word.

## Exit

- [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) resolved with a resolution comment, closed, and one gist line appended to the map's *Decisions so far*; any fog the answer sharpens graduated into tickets and cleared from *Not yet specified*.
- `CHANGELOG/2026-08-10/c13-byo-api-key.md` written (adjust the date folder to the day it actually runs), plus its `CHANGELOG/CHANGELOG_README.md` row.
- **No test layer applies** unless the session touches code, which it should not — say so explicitly in the changelog rather than skipping the section.
- `SESSIONS.md` row claimed before the first write and released at the end; commons files (`SESSIONS.md`, the changelog index) **leased** via `Lock-Path.ps1` before that first write, held through the commit.
- `kb-candidates/YYYY-MM-DD-c13-byo-api-key.md` written for anything durable; not ingested without Ido's approval in normal mode.
- Commit on Ido's approval, explicit paths only — blanket staging is banned outright in this repo.
