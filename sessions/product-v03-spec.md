---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: AUTO MODE          # brief said `normal`; Ido opened this session with `AUTOMODE`, and this session's message wins
status: active
issue: https://github.com/idomarhaim/Android_Final_Project/issues/12
created: 2026-08-13
---

# Write `docs/PRODUCT_v0.3.md` — the map's destination, and the only thing left of it

## Why this brief exists

**Map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) has no open tickets
left.** `C15b` (#35), `C11b` (#30) and `C19` (#41) all closed on 2026-08-13 between 01:0x and 01:5x;
`#12`'s *Decisions so far* holds **26** lines. But the map's own Destination is not "every ticket
closed" — it is:

> A **v0.3 product spec** — `docs/PRODUCT_v0.3.md` — that a build session can implement from without
> reopening a decision. […] The map is done when the spec is whole and no ticket is open.

**That file does not exist.** So the map is one artifact short of its destination, and writing it is
the next unit. It is also the point where the map's *plan, don't do* rule stops binding: the spec is
the handoff **to** building, not building.

## Read first

1. `AGENTS.md` (and the rules it links) — methodology as it stands then, not as it stood now.
2. **`#12`'s body in full** — Destination, Notes (scope, standing preferences), and all 26 lines of
   *Decisions so far*. It is ~100 KB; fetch it to a file rather than reading it through a pager.
3. Each closed ticket's **resolution comment**, on demand. The index line is a gist; the ticket holds
   the detail, and several resolutions overrode their own ticket's framing.
4. `Product and UX Reviews/2026-08-06-brief-review.md` (`R1`–`R28`) and
   `2026-08-09-entity-model-brief.md` (`E1`–`E19`) — the two sources every ticket traces to.
5. `docs/prototypes/*/README.md` — five prototypes are the design of record for the screens
   (`C9b` calendar, `C12` charts/widgets, `C6` log-progress, `C19` area success/failure, plus
   `2026-08-11-visual-styles` for the material contract).

## Task

Write `docs/PRODUCT_v0.3.md` so that a build session can implement v0.3 without reopening a
decision. Suggested spine, but the shape is yours to argue:

- **The model** — goal/task/milestone ontology and the edge-carried roles (`C4`, `C16`, `C17`,
  `C18`), what a unit is (`C7`), points and time (`C1`, `C3`), goal kinds as *views* rather than a
  stored enum (`C5`), task typing (`C2`).
- **Scheduling and the calendar** — `C9a`–`C9f`.
- **AI** — the five output schemas and the failure contract (`C11b`), what the model may and may not
  author (`C1`, `C8`, `C10`, `C13`, `C14`), the free-model constraint and the fallback rule that
  binds every AI feature.
- **Screens and presentation** — the material contract, the Hebrew/RTL rules, `C12`'s palette and
  widgets, `C6`, `C19`.
- **Localization** — `C15` and `C15b`.
- **What is explicitly out of scope**, copied from `#12` rather than re-derived.

Two rules the spec must carry rather than restate loosely, because both were won against a first
answer: **an AI feature that cannot run reliably on the free tier is specced with a non-AI fallback
beside it or is not specced**, and **a stored judgement that is derivable from per-item facts is a
defect** (`kb/dev/enum-and-label.md` §5 — it killed `GoalKind` in `C5` and shaped `C19`).

## Carries over

- **`#12`'s 26 decisions and its fog** — the map body, plus the two fog entries added on 2026-08-13
  (derived-state ownership; whether long idleness may ever retire a goal by itself). Committed:
  GitHub `#12`.
- **`RecommendationRepositoryImpl.kt:175`** filters *needs attention* on `progressFraction < 0.34f`,
  which is meaningless for a goal with no measure — `C7`'s default. Filed in `C19`'s resolution
  comment as an implementation gap, not a new decision. Committed:
  [#41's resolution](https://github.com/idomarhaim/Android_Final_Project/issues/41).
- **`ThemePaletteTest` is owed an update** — `C12` replaced committed `GoalCategory.defaultColorHex`
  values. Committed: `docs/prototypes/2026-08-10-charts-presentation/README.md`.
- **`GoalCategory`'s fate** — hardcoded English labels in `domain/model/`, routed to `C5` by
  `c2-task-type`. Committed: `CHANGELOG/2026-08-13/c2-task-type.md` and `#21`.

## Out of scope

- **No code.** This session writes a spec. If it starts editing `app/src/`, it has become a build
  session and should stop and split.
- **No reopening closed decisions.** A resolution that looks wrong is a **new ticket on `#12`** or a
  note to Ido — not a quiet rewrite in the spec.
- **`#2`–`#11`, `#34`, `#36`** — the UX/defect backlog is not part of the map and not part of v0.3's
  product model.

## Exit

- `docs/PRODUCT_v0.3.md` written and coherent, with every section traceable to the ticket that
  decided it.
- **No test layer applies** (a Markdown spec); say so explicitly in the changelog rather than
  skipping the section.
- `CHANGELOG/2026-08-13/<session-label>.md` (or that day's date) written.
- `#12`'s body updated to say the destination is reached, and **`#12` closed** — closing the map is
  the last act, and it is Ido's call to confirm.
- Commit on approval.
