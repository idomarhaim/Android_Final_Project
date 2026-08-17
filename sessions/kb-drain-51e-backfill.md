---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: auto
status: ready
created: 2026-08-17
---

# Drain the two pending `kb-candidates/` files into the central KB

**Runs in parallel with any feature session** — no build, no device, no `app/src/`.
But it is **cross-repo**, so it owes a row on **two** boards.

## Read first

1. [AGENTS.md](../AGENTS.md)
2. [kb-candidates/2026-08-17-51e-sweep-components.md](../kb-candidates/2026-08-17-51e-sweep-components.md) — 4 candidates
3. [kb-candidates/2026-08-17-changelog-index-backfill.md](../kb-candidates/2026-08-17-changelog-index-backfill.md) — 3 candidates
4. [kb-candidates/2026-08-17-completion-roadmap.md](../kb-candidates/2026-08-17-completion-roadmap.md) — 2 candidates, added after this brief was written. Its own deviation note explains why AUTO MODE's commit-trigger drain routed here instead: both are cross-repo, which is a session and not a step.
4. `C:\Dev\JARVIS\SESSIONS.md` — read the **whole** Active-claims section before your first write there
5. [CHANGELOG/2026-08-17/51d-dialog-locale.md](../CHANGELOG/2026-08-17/51d-dialog-locale.md) — the precedent drain (`dad8f12` → JARVIS `74b00c2`)

## Task

Run `/kb-ingest` over both candidate files. Pages belong in `C:\Dev\JARVIS\kb\`; this
repo's established pattern is a separate `kb-drain-*` session, which is what this is.

**Claim on both boards before your first write in each** — a row here for
`kb-candidates/`, a row on `C:\Dev\JARVIS\SESSIONS.md` for `kb/`. A `/kb-ingest` into
the central bundle *is* cross-repo by construction.

## Three things that will bite

1. **`kb/log/2026-08-17.md` is contended.** `/kb-ingest` appends to it, and
   `kb-drain-jarvis-own` was holding it live in JARVIS as of 51e's report. Re-check that
   board's liveness before writing; if it is still held, this is a
   `## ⏳ WAITING FOR SESSION <label>` situation, not a reason to write anyway.
2. **At least one candidate is always-ask and must not be drained silently.** 51d's entry 1
   narrowed a standing claim in `kb/dev/jvm-vs-android-locale-codes.md` §2 — check whether
   51e's four carry the same shape. **Anything that supersedes or contradicts a standing
   KB claim is always-ask in both modes**, because rewriting committed knowledge is a
   deletion. Park such an entry and ask; do not drop it.
3. **One candidate is about a governance failure, not a technique** — `changelog-index-backfill`'s
   candidate 2 records that *a hook installed mid-flight changed a live session's behaviour
   with no artifact in the repo to warn them*, which forced 51e to fold a sibling file back
   into its changelog. That may be destined for `rules/` rather than `kb/`, and
   **`rules/` is always-ask under the 🎬 walkthrough rule.** If so, park it, say so, and
   let Ido decide — do not draft a rule change in this session.

## Carries over

- **Deleting a fully-drained candidate file is permitted without asking**, in the same
  commit as the promotion it produced. A **partly**-drained file is rewritten down to its
  survivors and **never** deleted.
- **Verify each promotion actually exists** at its destination commit rather than trusting
  your own message — `changelog-index-backfill` did exactly this for `dad8f12` and it is
  the cheap check that makes the deletion safe.
- **Every entry must stand alone.** If one is too thin to write a page from, stop and ask;
  never reconstruct it from a transcript.
- **`CHANGELOG_README.md` is generated** — run `scripts/New-ChangelogIndex.ps1`.

## Out of scope

- Drafting or editing anything under `C:\Dev\JARVIS\rules\`.
- Any `app/src/` change, any build, any device.
- Draining candidate files that do not exist yet (later sessions write their own).

## Exit

- One `📥 **Ingested:** <topic> → <bundle>/<page>` line per candidate that landed, and one
  explicit line per candidate **parked** with the reason.
- `/kb-lint` clean on the bundle you wrote into.
- `CHANGELOG/<today>/kb-drain-51e-backfill.md` written, index regenerated.
- **Both** board rows released. Brief closed to `sessions/done/` with `status: done` in the
  same commit. **Commit and push under AUTO MODE** — run the sibling checklist in the roadmap's
  §🔀 first, in **both** repos, and say in your reply that the mode acted.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

End your final reply with **exactly one** of these headings, below the three file lists.
Full definition — the **seven** `GO` conditions, and which reply carries the heading:
[TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
§🚥. Ido must never have to work out for himself whether the next kickoff is safe.
**This brief runs in AUTO MODE** (`mode: auto`, Ido's standing instruction of 2026-08-17), so
condition 1 is met by **you having committed**, not by his approval — `GO` is the ordinary case
here. **But auto mode changes nothing about this session's two always-ask candidates**: a
`rules/`-destined entry, and anything superseding a standing KB claim, both stay parked and
named. Auto mode drains the *ordinary* ones without asking; it does not promote those two.

- **On success:** `## 🚥 GO — NEXT: /kickoff docs-hygiene-backfill`, and name whichever
  build lane (`50-offline-stamps` / `48-settings-surface`) is still unrun.
- **A parked always-ask candidate is still a `GO`** — it is remaining work awaiting Ido's
  decision, not a fault. Name it on the line so he knows a decision is waiting.
- **Both** board rows must be released, here **and** in `C:\Dev\JARVIS`. One released and
  one live is a `STOP` — say which repo still holds a row.
- **If you ended up waiting on `kb/log/2026-08-17.md`:** a `## ⏳ WAITING` banner makes it a
  `STOP` for the kickoff, even though the wait clears itself.
