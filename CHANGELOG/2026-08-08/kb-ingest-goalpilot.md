# 2026-08-08 — `kb-ingest-goalpilot` (GoalPilot side)

> **Summary:** A cross-repo `/kb-ingest`.

A cross-repo `/kb-ingest`. **The pages landed in the central JARVIS bundle**, so the
substance of this entry is in
`C:\Dev\JARVIS\CHANGELOG\2026-08-08\kb-ingest-goalpilot.md` and the journal entry
at `C:\Dev\JARVIS\kb\log\2026-08-08.md`. This file records only what changed *in
this repo*.

## What changed here

Both candidate files were **fully drained** — all four entries ingested, none
blocked, none always-ask — and removed per `/kb-ingest` §7.5:

- `kb-candidates/2026-08-06-product-review.md` (2 entries)
- `kb-candidates/2026-08-06-product-device-pass.md` (2 entries)

They became, in `C:\Dev\JARVIS\kb\`:

| entry | destination |
|---|---|
| review split by ceremony tier · a bug can be an unspecified model | **new** `dev/review-intake-and-triage.md` (merged — the second is the first's sharpest evidence) |
| a Firestore transaction is server-only | **new** `dev/firestore-write-semantics.md` |
| instrumenting a device reproduction pass | `dev/android-device-verification.md` **§6** (in place) |

The candidate→page tie lives in the JARVIS journal entry, which names both source
files **with this repo's path** — necessary because no single commit can hold both
sides of a cross-repo ingest.

## Not claimed on the board, deliberately

`SESSIONS.md` was **not** touched. When this ran, the `product-model-map` session
had a complete unit staged in this working tree — including `SESSIONS.md` itself —
so editing the board would have meant either colliding with a live sibling or
carrying their uncommitted work into this commit. The two file removals and this
note are a single-commit sweep into paths nobody else owns; per
`C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md` that is the mechanical-sweep
carve-out, and a claim created and cleared inside one commit protects nothing.

**The commit is path-scoped** (`git commit -- <paths>`) for the same reason: it
takes only these three paths and leaves the sibling's staged index exactly as
found.

## Noted, not taken

`kb-candidates/2026-08-08-product-model-map.md` appeared in this repo while the
ingest was running — a **new, un-drained** candidate file from the `product-model-map`
session. It is not this session's to drain and was left alone. The next
`/kb-ingest` in this repo should pick it up.

## 🧪 Tests

**No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file
was touched; this repo's change is two file removals and this note. The bundle side
was verified with `Check-KbLinks.ps1`: **28 pages, clean — no broken links, no
orphans, no wikilinks.**
