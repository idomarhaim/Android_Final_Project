# Changes — 06/08/2026 · session `kb-audit`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** Drained this repo's share of a cross-repo KB-candidate sweep — the `release-distribution` session's durable knowledge became `knowledge/release-distribution.md`; its cross-project half went to the central KB from the JARVIS side.

No application code, no rules, no Gradle file. This session is an **ingest**: the
sweep that answered *"are there KB candidates I missed?"* found that two 2026-08-05
sessions in this repo had produced durable knowledge that never reached any bundle.

## 📥 Ingested here

**`knowledge/release-distribution.md` *(new)*** — from
[CHANGELOG/2026-08-05/release-distribution.md](../2026-08-05/release-distribution.md)
(`5316782`). Off-Play means Android provides **no** update mechanism, so the
mechanism is the deliverable; the signing key is unrecoverable and therefore
precedes the first hand-out; the updater is release-only by construction
(`releaseImplementation`); the workflow triggers on a **tag**, not a push, because
`versionCode` is manual and a push-triggered job would notify testers about builds
that are not installable updates. Also what was proven (both signing paths
exercised in order) and what cannot be proven from this machine (that a tester
receives the prompt).

## 📤 Ingested centrally, not here

Three claims from this repo's sessions generalize past GoalPilot and went to
`C:\Dev\JARVIS\kb` instead, so they reach every project:

- **PowerShell 5.1 encoding traps** — `new-release-keystore.ps1` and
  `run-goalpilot.ps1` both failed to parse because a BOM-less `.ps1` is decoded as
  CP1252 and an em dash ends in a closing quote. The same root cause hit a third
  session in another repo the same day, in the opposite direction (`-Encoding utf8`
  writing a BOM). Restated here only as the local *scripts are ASCII* convention.
- **Second-AVD mechanics** (`second-avd`) — the 2 GB snapshot floor for an API 37 +
  Play Services image, `-Avd` as a demand rather than a hint, and why two emulators
  do not parallelise instrumented testing.
- **An authorization rule needs a second real identity** (`submission`) — green
  rules tests are not evidence about a *deployed* ruleset, and a backlog is itself
  a source-read claim about a runtime.

## 🧾 Housekeeping

- `CHANGELOG/CHANGELOG_README.md` — added the missing index row for
  `2026-08-05/release-distribution.md` (the session file was committed without
  one), plus this session's row.

## 🧪 Tests

No test layer applies: nothing in `app/`, `firestore.rules`, `functions/` or
`scripts/` changed — this session touched Markdown only. The JVM unit, instrumented
and security-rules suites were **not run**, and neither emulator nor the Gradle
daemon was claimed. Verification was that every claim on the new page resolves to
the committed changelog it cites.

## 🧭 Sessions

Claimed `kb-audit` before the first write and released it in the same commit.
Noted, not touched: the `release-distribution` row is still on the Active board
although its work is committed and pushed — a stale claim to release or confirm.
