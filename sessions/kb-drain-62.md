---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
owns:
  - kb-candidates/2026-08-24-62-tour-video-v2.md
  - CHANGELOG/<the day you run>/kb-drain-62.md
  - sessions/kb-drain-62.md
  # CROSS-REPO: also writes into C:\Dev\JARVIS\kb\ and owes a claim on THAT board too
singletons: []
created: 2026-08-24 by 62-tour-video-v2
---

# Drain `62-tour-video-v2`'s KB candidates

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Task:** run `/kb-ingest` over
[`kb-candidates/2026-08-24-62-tour-video-v2.md`](../kb-candidates/2026-08-24-62-tour-video-v2.md) —
six entries, all `Status: ready`.

## Why this is its own session, and the deviation it records

Auto mode says candidates drain **at the commit trigger**, and `62-tour-video-v2` hit that trigger
and did not drain them. That was a deliberate call and it is recorded rather than hidden: the
session had run about two and a half hours across five recording takes, the ingest is **cross-repo**
into `C:\Dev\JARVIS\kb\` and owes a claim on that board as well as this one, and five of the six
entries land in a **single** page — so they are better written in one pass by a session that has
read that page than appended by one that has not.

**Nothing is lost by the delay**, which is the test the rule itself sets: every entry carries its own
Claim · Why · Destination · Anchors · Supersedes · Status, and the file is **committed** (`1d6ef09`),
so no transcript is a source for any of it.

## The six entries, and where they go

| # | claim | destination |
|---|---|---|
| 1 | `screenrecord` silently downgrades to 720×1280, exits 0, and the encoder's real ceiling is measurable (1152 × 2560 here) | `kb/dev/android-device-verification.md` — **sharpens** §6.2, which mentions the fallback in passing without saying it exits 0 |
| 2 | A substring match over a UI hierarchy needs a positional band, or it taps the wrong node | same page |
| 3 | Git Bash rewrites a **device** path, so `adb shell` writes somewhere you did not ask for | same page, cross-linked from this project's `CLAUDE.md` Windows-traps list |
| 4 | A build minutes old can be the shipped app on every screen but one, and a version string is evidence about neither | same page |
| 5 | Rehearse a UI script with writes disabled — the only cheap way to find selector rot | same page, as a method rather than an incident |
| 6 | A marketing film of an app is a film of its **data**, and the account can be empty of exactly the new features | **a new page.** Not device verification — *demo-data readiness*, which generalises to screenshots, store listings and examiner demos |

**Nothing here is destined for `rules/` and nothing supersedes a standing KB claim**, so all six are
ordinary auto-mode ingests. Entry 6 creates a page, which is a create rather than an overwrite.

## One thing entry 1 should pick up that is not in the candidates file

`CLAUDE.md` in this repo already carries a *"the file format treats your prose as syntax"* family —
the `local.properties` backslash trap, `--` inside an XML comment, `/*` inside a Kotlin KDoc. Entry
3 is the same family one layer out (the **shell** treats your data as syntax), and the page it lands
on should say so rather than letting the two drift apart.

## Carries over

- `kb-candidates/2026-08-24-62-tour-video-v2.md` — the six entries in full
- `CHANGELOG/2026-08-24/62-tour-video-v2.md` — the incidents behind them, with the measurements

## Out of scope

The film assembly — that is [`sessions/62-tour-assembly.md`](62-tour-assembly.md), and it needs the
OpenArt MCP connected. This brief needs no MCP, no device and no Gradle, so the two can run in
either order or side by side.

## Exit

- All six entries promoted, each reported as `📥 **Ingested:** <topic> → <bundle>/<page>`.
- The candidates file **deleted** in the same commit as the promotion — permitted without asking
  only when **every** entry has been promoted; a partial drain rewrites the file down to its
  survivors and never deletes it.
- The JARVIS board row released, and this repo's too.
- `CHANGELOG/<the day you run>/kb-drain-62.md`.
