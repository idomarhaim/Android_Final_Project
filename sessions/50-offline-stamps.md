---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: ready
issue: 50
created: 2026-08-17
---

# #50 — Offline as-of stamps, "Not loaded yet", and delete `ConnectivityMonitor`

## Read first

1. [AGENTS.md](../AGENTS.md)
2. `gh api repos/:owner/:repo/issues/50 --jq '.body'` — the full ticket (GraphQL is 503'ing; REST works)
3. [CHANGELOG/2026-08-15/c20-derived-state.md](../CHANGELOG/2026-08-15/c20-derived-state.md) — the projection function this rides
4. [TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)

## Task

Build #50 as filed. Its own body is the spec and it is unusually complete — four items
and one deletion, with the *why* already argued so nothing needs re-deciding:

> **Staleness is a property of the data, not of the connection.**

1. **`updatedAt` on the two cross-boundary DTOs**, server-set, written by C20's
   projection on the same write that sets the number — `PublicProfileDto` and
   `ChallengeParticipantDto` in `data/firestore/dto/Dtos.kt`. One field, no new trigger,
   no new read path. **Verify the line numbers at HEAD** — the ticket notes they have
   already drifted once.
2. **The as-of caption, unconditional** — *"Standings as of 09:14"*, online and offline
   alike. A caption, never a warning. No connectivity banner, no per-number "cached"
   styling; the ticket argues both down and they are not to be reintroduced.
3. **`empty` and `never loaded` render differently** — discriminate on
   `snapshot.metadata.isFromCache && snapshot.isEmpty` (currently **0 usages** in the
   app) and show **"Not loaded yet"**, never *"No friends"*.
4. **Exactly two screens** — `feature/social` and `feature/challenges`. The ticket's
   read-rule table proves no other surface owes anything about offline. Do not widen it.
5. **Delete `core/net/ConnectivityMonitor.kt`** plus
   `feature/goals/GoalDetailViewModel.kt`'s pre-check and its `OFFLINE_MESSAGE`.

## Carries over

- **The deletion in item 5 is prescribed by the ticket, so it is authorised — but name
  it explicitly** in your reply and changelog, and do not extend it by one file.
  Deletions are otherwise always-ask.
- **`OFFLINE_MESSAGE` is a hardcoded English literal, and deleting it does NOT
  discharge #51's sweep.** #51 is deferred by decision (see
  `sessions/done/hebrew-defer-freeze.md`); you may write plain English literals in
  `feature/social` and `feature/challenges`, both of which are unswept. Do **not** add
  them to `SWEPT_PACKAGES`.
- **New copy still goes through `stringResource` where the surrounding file already
  does** — match the file, don't convert it.
- **`DialogLocaleGuardTest` is armed app-wide.** If you open a dialog, sheet or dropdown,
  use the `App*` façade from `ui/locale/LocaleAwareWindows.kt`. A raw `AlertDialog(`
  fails the build.
- **`CHANGELOG_README.md` is generated** — run `scripts/New-ChangelogIndex.ps1` or the
  pre-commit hook refuses you.
- **`functions/` has no test layer at all** (#50's own §7.2 note). Say so explicitly in
  the changelog's `## 🧪 Tests` section rather than skipping silently.

## Out of scope

- **Anything about the radio.** No `ConnectivityManager`, no banner, no retry UI.
- **`shares/{shareId}`** — an immutable event; an as-of stamp fixes nothing there.
- **Owner-side surfaces** (`users/{uid}/**`) — the reader is the writer, so they are
  complete and correct offline after C20. Do not stamp them.
- **Week start, region, or any setting.** #48 owns those.

## Exit

- JVM unit green; `assembleDebug` green. Instrumented only if you touched Compose —
  and if you run it, announce `## 📱 DO NOT SIGN IN` first, because it uninstalls the app.
- The *Not loaded yet* state exercised in a test; the `Untested:` first-run-offline
  behaviour hedged in the changelog exactly as the ticket hedges it, not asserted.
- `CHANGELOG/<today>/50-offline-stamps.md` written, index regenerated.
- Board row released; brief closed to `sessions/done/` with `status: done` in the same
  commit. Commit on Ido's approval.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

End your final reply with **exactly one** of these headings, below the three file lists.
Full definition — the **seven** `GO` conditions, and which reply carries the heading:
[TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
§🚥. Ido must never have to work out for himself whether the next kickoff is safe.
**In normal mode your commit needs his approval first, so the honest heading is usually
`STOP` naming that approval *and* the slug that follows it** (condition 1). Name the slug
either way.

- **If `48-settings-surface` has not run yet:**
  `## 🚥 GO — NEXT: /kickoff 48-settings-surface`, plus a Lane C session alongside.
- **If it has already run:** `## 🚥 STOP — DO NOT KICKOFF YET` — waves 3–4 have no briefs.
  **next:** one short session to write them against HEAD, then kickoff the first.
- **Anything not green, not committed, or still holding the Gradle daemon:** `STOP`, and
  say which of the six conditions failed.

Check the board and `sessions/done/` for #48's state — do not guess it.
