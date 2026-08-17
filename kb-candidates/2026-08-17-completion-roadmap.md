# KB candidates — `completion-roadmap`, 2026-08-17

Two candidates. Both are cross-repo (pages belong in `C:\Dev\JARVIS\kb\`), so they are
routed to the already-briefed [`sessions/kb-drain-51e-backfill.md`](../sessions/kb-drain-51e-backfill.md)
rather than drained inline — see the deviation note at the bottom.

---

## 1 · Enumerating entry points to a state, and missing the one that is already-stored state

- **Claim.** When you gate access to a state by filtering its *inputs*, enumerate the
  **already-stored** value as an entry point of equal standing. It is the one that reads as
  history rather than as an input, so it is the one that gets left out — and it is also the
  only one whose victims are **exactly the users who used the feature**, which inverts the
  usual severity ordering: the gate holds for everyone who never touched it and fails for
  everyone who did.
- **Why.** `hebrew-defer-freeze` (GoalPilot, 2026-08-17) had to make Hebrew unreachable. Its
  brief — written by a session that had grepped every `AppLanguage` consumer — named two doors:
  the picker iterating `entries`, and `AppLocale`'s `SYSTEM` branch reading the live device
  locale. Both are *inputs*. The third door was **persistence**:
  `AppPreferencesRepositoryImpl` read `AppLanguage.fromId(stored)`, and `fromId` is faithful by
  design — it returns `HEBREW` for a stored `"he"` however the picker is filtered. So the freeze
  would have held on every device except those that had actually selected Hebrew, including the
  owner's, which is also where the verification render pass was scheduled to happen.
  **Rejected framing:** *"the grep was too narrow"* — it was not; all three consumers were in
  the brief's own file list. The miss is conceptual, not a coverage gap, which is why *grep
  wider* is not the remedy and *enumerate stored state as a door* is.
- **Destination.** `kb/dev/` — a new page. Nearest existing neighbour is the
  derivation-closure idea in `rules/question-axis-naming.md` (run the closure one hop wider),
  but that rule is about **questions**, and this is about **gates**, so it is a sibling and not
  an edit.
- **Anchors.** GoalPilot `7baf120`; `domain/model/AppLanguage.kt` (`OFFERED`, `offeredFromId`,
  `clampToOffered`); `CHANGELOG/2026-08-17/hebrew-defer-freeze.md`.
- **Supersedes.** Nothing.
- **Status.** Pending — routed to `kb-drain-51e-backfill`.

---

## 2 · Two blockers that are indistinguishable from outside, and the flattering repair

- **Claim.** When two different causes produce the same observed failure, fixing one and
  recording the pair as one thing leaves a reader believing the other cured itself. So when a
  blocker clears, **name which cause cleared** and re-assert the other explicitly, in the same
  sentence. The failure is silent and lands in the flattering direction: everyone reads
  *unblocked*.
- **Why.** On 2026-08-17 GoalPilot hit a GitHub partial outage (GraphQL 503, REST healthy) at
  the same time the harness classifier was denying `gh` **writes**. Both surfaced as *"the
  `gh` command for `#51` did not work"*. When the outage cleared, *"GitHub is healthy"* and
  *"the three `#51` writes can now be posted"* read as one claim, and only the first was true —
  the writes needed the owner's permission, which no service recovery supplies. Recorded as two
  facts on `SESSIONS.md` and in the roadmap for that reason. **Rejected framing:** *"just say
  which command failed"* — the commands were identical; the discriminator is the *cause*, which
  is invisible at the call site.
- **Destination.** `kb/dev/` — likely a section on an existing page about blocked-work
  reporting if one exists; otherwise a short page of its own. **Check for the concept before
  creating**: grep `kb/` and `rules/` for *blocker*, *conflated*, *silent in the flattering
  direction*.
- **Anchors.** GoalPilot `SESSIONS.md` (the ✅ GitHub-healthy note and the ⚠️ beneath it);
  `CHANGELOG/2026-08-17/completion-roadmap.md` addendum 2;
  `sessions/51-freeze-verify.md`.
- **Supersedes.** Nothing.
- **Status.** Pending — routed to `kb-drain-51e-backfill`.

---

## Deviation note — AUTO MODE says drain at the commit trigger; this did not

`memory-promotion.md` puts the drain on the commit trigger under AUTO MODE, and this file is
riding a commit undrained. Reason, stated rather than glossed: both candidates are **cross-repo**
— pages belong in `C:\Dev\JARVIS\kb\`, which needs a claim on **that** repo's board, an index
update and an append to `kb/log/2026-08-17.md`, a file `kb-drain-jarvis-own` was recently
holding. That is a session's work, not a step, and
[`sessions/kb-drain-51e-backfill.md`](../sessions/kb-drain-51e-backfill.md) is already briefed
and queued for exactly this — it now covers three candidate files instead of two. The entries
above are written to stand alone, so nothing depends on this session's transcript surviving.
