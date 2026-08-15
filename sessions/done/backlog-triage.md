---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: done
commit: 373a8d5
completed: 2026-08-15 by backlog-triage
issue: https://github.com/idomarhaim/Android_Final_Project/issues/12
created: 2026-08-15 by c21-offline-story
---

# Triage the open backlog against `docs/PRODUCT_v0.3.md`, before any build session reads a stale ticket

## Why this brief exists

The map spent 21 tickets deciding the product model, and the backlog issues **were written before
those decisions** — `#2`–`#11` in 2026-08-06, from Ido's brief. Some are now wrong, one is almost
certainly superseded outright, and **nothing has reconciled them.** A build session that opens
`#34` today implements a design `C20` explicitly rejected.

**This runs after `docs/PRODUCT_v0.3.md` exists and before any build session.** That is the whole
point of its position: it is cheap, and it is the only thing standing between a finished spec and
someone building from a ticket the spec contradicts.

> ✅ **PRECONDITION MET, 2026-08-15 — all three are closed and this brief is runnable.** `C22` #44
> (`cded54e`), `C23` #45, and `C24` #46 (`7f9d032`, **approved by Ido**). `#12` has **zero** open
> children. **Two things `C24` adds to this session's input**, both of which change what the triage
> compares against:
>
> 1. **`§4.9` is a new spec section describing a screen that does not exist and that no issue
>    carries.** Under this brief's own *"any new issue the spec implies is filed"* clause, the
>    settings surface almost certainly wants an issue of its own — it is the one piece of build work
>    the map produced with no ticket behind it, and it is a **precondition of `#9` and `#36`** rather
>    than a peer of them, since both need settings that live on it.
> 2. **`§4.1` gained three material-contract rules** (a translucent surface tints toward the theme;
>    a skin owes a luminance contract; `AppSkin` must reach every material's accent, ground and
>    ramp). These bind `#10`'s widget pack and anything else that draws a surface.
>
> The original precondition text follows, kept because it records why this brief waited.

> ⚠️ **Precondition added 2026-08-15, after this brief was written.** `docs/PRODUCT_v0.3.md` now
> **exists** (`d271355`), but writing it **found three gaps and filed them as new map tickets** —
> [`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44) (the measure proposal
> surface), [`C23` #45](https://github.com/idomarhaim/Android_Final_Project/issues/45)
> (`GoalCategory`'s list and where its labels live), [`C24` #46](https://github.com/idomarhaim/Android_Final_Project/issues/46)
> (the settings surface v0.3 requires and does not have). **Wait for all three to close before
> running this brief.** Each changes a spec section this triage compares tickets against — `C23`
> bears on `#2`, `C24` on `#9` and `#36`, `C22` on `#6`/`#7` — so triaging now buys a second triage
> later. `Observed:` `gh issue list --state open` under `#12`; the brief's own first check
> (*does the spec exist*) is necessary but **no longer sufficient**, and that is the amendment.

## Read first

1. `AGENTS.md` and the rules it links.
2. **`docs/PRODUCT_v0.3.md`** — written by the `product-v03-spec` session. If it does not exist yet,
   **stop**: this brief has no input and the session should not start.
3. `#12`'s *Decisions so far* — 28 lines. The gist is enough for triage; zoom into a ticket's
   resolution comment only when a backlog issue looks affected.
4. Each open issue in full.

## Task

For each open issue — `#2`, `#4`, `#5`, `#6`, `#7`, `#8`, `#9`, `#10`, `#11`, `#34`, `#36` — decide
one of three, and **record the decision as a comment on the issue itself**, not only in a changelog:

- **Still correct** — leave it, with a one-line comment naming the spec section that confirms it.
- **Superseded** — close it, with a comment pointing at the decision that replaced it.
- **Needs rewording** — edit the body so it describes what the spec says, and say what changed.

**The success criterion for the whole session: after it, `/implement #N` is a sufficient first
message for every surviving issue.** Each one must name the spec section it builds against, so a
build session needs nothing but the ticket. **Write no briefs for these issues** — an issue is
already a committed work order, and a `sessions/*.md` beside it would be an *uncommitted duplicate
of a committed ticket*, which is the reason `product-v03-spec` declined to write briefs for `#44`–
`#46`. `sessions/` is for work **no ticket carries** — this brief itself, because no issue does.
`#4` and `#5` were the exception that proves it: they got a brief because they needed a device pass,
a rules deploy and a *"do not build `C21`'s lines here"* boundary that neither issue stated.

**A verified starting split** (grep of `#12`'s body for each issue link, 2026-08-15) — a lead, not a
verdict, because *not cited* is not *unaffected*:

| | Issues | What is already known |
|---|---|---|
| **The map ruled on them** | `#34`, `#10`, `#8`, `#9`, `#36` | **`#34` is the strongest supersede candidate** — `C20` adjudicated its *recompute-and-store* proposal and chose *project-from-facts* instead, and priced its stated cost at zero. **`#10` is unblocked and designed** by `C12`: all seven cards at `2×2`/`4×2`/`2×4`/`4×4`, each size carrying its own smallest true disclosure. |
| **The map never mentions them** | `#2`, `#4`, `#5`, `#6`, `#7`, `#11` | Presumed still as written. `#4` and `#5` may already be **fixed** by `social-share-bugs` — check before triaging them. |

**Two the spec adds that have no issue at all**, and they are the reason this is a triage rather
than a review: `C21` produced **four spec lines and one deletion** touching `feature/social` and
`feature/challenges` (as-of stamps, a *"Not loaded yet"* empty state, `updatedAt` on two DTOs,
`ConnectivityMonitor` deleted), and `C20` filed **four defects as spec lines** including a
non-atomic `logProgress` that leaves `currentValue` permanently wrong after a crash. Decide whether
each wants its own issue or rides in a build session; **`C20`'s `logProgress` defect is a live data
corruption and should not be left implicit.**

## Carries over

- **`#34` is referenced three times in the map body** — read all three before closing it, since one
  of them may be the reason to keep it open in a narrower form. Committed: `#12`'s body.
- **`RecommendationRepositoryImpl.kt:175`** filters *needs attention* on `progressFraction < 0.34f`,
  meaningless for a goal with no measure (`C7`'s default). Filed as an implementation gap, never as
  an issue — this session decides whether it becomes one. Committed:
  [`#41`'s resolution](https://github.com/idomarhaim/Android_Final_Project/issues/41).
- **`ThemePaletteTest` is owed an update** — `C12` replaced committed `GoalCategory.defaultColorHex`
  values. Committed: `docs/prototypes/2026-08-10-charts-presentation/README.md`.
- **`TODO/TODO_OPTIONAL/Presentation.TODO.optional.md`** holds six prototype refinements and five
  items of build cost from `C12`, deliberately kept out of the tracker. Committed: that file.

## Out of scope

- **No code.** This session edits tickets and writes comments. If it starts editing `app/src/`, it
  has become a build session and should stop and split.
- **No reopening closed decisions.** A resolution that looks wrong is a note to Ido, not a quiet
  rewrite of a ticket.
- **Closing `#12`.** That is Ido's call and belongs to whoever confirms the spec is whole.

## Exit

- Every open issue carries a triage comment, and the superseded ones are closed.
- Any new issue the spec implies is filed, or explicitly recorded as *deliberately not filed*.
- **No test layer applies** (tracker hygiene and Markdown) — say so in the changelog rather than
  skipping the section.
- `CHANGELOG/<the day you work>/backlog-triage.md` written, listing every issue and its verdict, so
  the build order in it is auditable later.
- **Closing issues is always-ask in normal mode** — deletions and outward-facing actions both. Bring
  Ido the list of proposed closes; do not close unilaterally unless he has said `AUTO MODE`.
- Commit on approval.
