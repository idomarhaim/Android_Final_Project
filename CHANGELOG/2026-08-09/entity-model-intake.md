# `entity-model-intake` — the entity definitions arrive mid-map, and they answer C4 with a word no ticket used

**Session:** `entity-model-intake` · **Date:** 2026-08-09
**Invocation:** direct request from Ido — transcribe the new entity-definition
`.docx` the way `product-review` transcribed the 08-06 brief, and record the new
`docs/pre-injested-docs/` folder plus the `.docx` move everywhere the repo points
at it.
**Intake only. No application code, no issue written, no ticket resolved.**

| | |
|---|---|
| New asset | [`Product and UX Reviews/2026-08-09-entity-model-brief.md`](../../Product%20and%20UX%20Reviews/2026-08-09-entity-model-brief.md) — `E1`–`E19` |
| Source recorded | `docs/pre-injested-docs/` (new folder, Ido's) |
| Ticket resolved | **none** — `#12`, `#13`, `#14`, `#29` are live sessions' claims and were not touched |
| Live project changed | **none** |

---

## The headline

**Ido's entity document gives a goal↔task discriminator that is none of the four
options `C4`'s question picker offered him** — and that is why the picker felt
like it was not understanding him.

`E7`: a **goal** is an objective that matters to you *in its own right*, "not as
a means to achieve something else". `E12`: a **task** is "something that must be
done in order to fulfil that goal … not necessarily important to you in life in
its own right". The discriminator is **intrinsic versus instrumental** — a
judgement about the user's own values. The picker offered *measured vs done*,
*size/effort*, *endures vs completes*, and *no fixed rule*. None of them is this,
and the recommended one (*measured vs done*) is a mechanical proxy for it at best.

Two consequences that a `C4` session should not have to re-derive:

1. **Goals do not nest inside goals — they are joined by milestones.** `#13` asks
   *"can a goal live inside a goal, and Ido wants a worked example if so"*.
   `E14`–`E16` and `E19` answer it with a third entity: a **milestone**
   (*אבן דרך*) is a significant sub-goal on the instrumental side of `E7`'s line,
   and the *same object* can be a milestone of one goal while being a goal in its
   own right. `E19` is the worked example the ticket asked for, verbatim: a
   software-engineering degree is a goal *and* a milestone of "be worth $100M";
   "finish year 1" is a milestone *only*.
2. **The measurable/unmeasurable question is left deliberately open by Ido
   himself** (`E6`) — the AI *may* advise sharpening a goal into something
   measurable, "on the other hand, maybe not necessarily". That is a live
   constraint on `C7` (#14), which is also in flight.

## What this document introduces that is on nobody's ticket

Flagged, not filed — filing is a charting act and this session owns no map paths:

| New scope | `E`-ids | Why it is not an implementation detail |
|---|---|---|
| **The milestone entity** | `E14`–`E16`, `E19` | A third structural level, itself nestable, optionally identical to a goal. Nothing in `domain/model/` or on the map has it |
| **Many-to-many linkage** | `E17`, `E18` | Goal↔life area and task↔goal both become collections. A task under two goals in two life areas breaks the time-allocation chart's arithmetic — a decision, not a detail |
| **Sub-tasks at arbitrary depth** | `E13` | `Task` is flat and has no parent field; every roll-up (`points`, `progressContribution`, `estimatedMinutes`) assumes one level |
| **A possible third goal kind** | `E9` | Ido explicitly asks whether one should exist. A question addressed to the agent, unanswered |
| **Success/failure visualisation per life area** | `E4` | Not in `R1`–`R28`, not on `C12` (#31) |

Five of the eight schema rows checked in the brief are **changes over live data
in `goalpilot-56e30`** — the Firestore-migration fog the map already records.

## What was documented (Ido's item 11)

- **[`AGENTS.md`](../../AGENTS.md) → *Where things live*** — `docs/pre-injested-docs/`
  added, with the rule that nothing downstream may cite a file in it directly
  (binary, Hebrew, unquotable); and `Product and UX Reviews/` documented for the
  first time as the transcription home, naming both files and their id ranges.
- **[`Product and UX Reviews/2026-08-06-brief-review.md`](../../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md)** —
  its source-file reference was stale in two ways at once: the `.docx` had moved
  folders *and* been renamed with its date. Fixed, with a note saying so.
- **The transcription stays in `Product and UX Reviews/`, and the sources move
  out.** Decision taken per the derivable-decision rule rather than asked:
  `2026-08-06-brief-review.md` is linked from **seven** places — `TODO/TODO.md`,
  both product TODO files, two `sessions/done/` briefs, an 08-06 changelog entry,
  and the body of map issue **#12** — while the `.docx` is linked from one. Moving
  the linked half to sit beside the unlinked half would break seven references,
  one of them inside an issue three live sessions are reading.

## What was deliberately **not** done

- **No comment on `#13`, `#14` or `#29`, and no edit to the map body `#12`.** All
  four are in live sessions' `Owns` columns on [`SESSIONS.md`](../../SESSIONS.md).
  This session hands those sessions a document; it does not answer their tickets.
  §5 rule 2 — if you need a path another session owns, say so and let Ido
  re-assign.
- **No new issues filed** for the milestone entity, many-to-many linkage or
  sub-tasks. Wiring new tickets into a map is charting, and the map is claimed.
- **No `TODO/` edit.** `TODO/TODO_FUTURE/ProductModel.TODO.future.md` is
  explicitly superseded by `#12`; adding to it would write into a file the map
  declares historical.

## Decisions Ido took at the close

1. **The live `c4-goal-task-ontology` session is fed this brief and made to
   re-ask** — not killed and re-run. It keeps everything it has already grounded;
   what it must be told explicitly is that its **question picker is superseded**,
   not supplemented, or it will fold the new axis into its four old options
   instead of replacing them.
2. **That same session files the new scope**, as it resolves `#13` — with a
   deliberately bounded list, because two of the five are not new tickets at all:
   - **Files as tickets:** the milestone entity, many-to-many linkage, arbitrary
     sub-task nesting.
   - **`E9`'s third goal kind** folds into **`C5`** ([#21](https://github.com/idomarhaim/Android_Final_Project/issues/21)),
     which already owns "how many kinds of goal are there" — a ticket of its own
     would be the same question twice, which is the *knot* failure
     `kb/dev/decision-map-charting.md` already records.
   - **`E4`'s per-life-area success/failure visualisation** goes to the map's
     **"Not yet specified"** fog, not a ticket — it is a presentation
     requirement and sharpens only once `C12`
     ([#31](https://github.com/idomarhaim/Android_Final_Project/issues/31)) lands.

   **Why this and not a dedicated re-charting session first:** the repo already
   runs this pattern and it is proven — `c9d-calendar-scopes` surfaced and filed
   `C9f` (#33) while resolving `#17`; `c15-language-switching` graduated `C15b`
   (#35) while resolving `#15`. A resolving session filing what it surfaces is
   the design. A separate charting pass would also have to phrase a milestone
   ticket *before* `C4` decides what a milestone is relative to a goal, which is
   the same question in two tickets. And the `c4-goal-task-ontology` session
   already holds both `#13` and the map body `#12`, so it is the only option with
   **zero claim conflict**.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, `firestore.rules` or
Cloud Functions file was created or modified — this session wrote Markdown and
moved a `.docx`. The layers that exist in this project and were therefore *not*
run: JVM unit (`:app:testDebugUnitTest`), instrumented
(`:app:connectedDebugAndroidTest`), and security rules (`firestore-tests/`).

Verification instead was **source discipline**: every claim in the brief's
schema-delta table was read out of `domain/model/` at a named line rather than
recalled, and the ticket-routing table was built against `#12`'s live sub-issue
list and `#13`'s live body, both queried out of GitHub rather than from the
board's summary of them.

## Singletons

**None taken.** No `#gradle-daemon`, neither AVD, no contact with live
`goalpilot-56e30`, no GROQ call, no write to any GitHub issue.

**Leases:** `SESSIONS.md`, `AGENTS.md` and `CHANGELOG/CHANGELOG_README.md` were
acquired together before the first write and held to the commit, per §5.2. All
three were free on the first attempt.

## Recorded rather than papered over

- **Three live sessions were on the board when this one claimed** —
  `c7-what-is-a-unit` (#14), `c4-goal-task-ontology` (#13) and `c10-quote-feed`
  (#29). The `c10-quote-feed` row was **uncommitted** when this session arrived,
  so any commit touching `SESSIONS.md` carries it. Named here rather than left
  silent; the row is that session's, not this one's.
- **`kb-candidates/` was listed before the first unit of work**, as required.
  **Three files are pending and all three are unowned** — every session that
  wrote them has released:
  `2026-08-08-c9d-calendar-scopes.md`, `2026-08-08-fix-task-completion-feedback.md`
  and `2026-08-09-c9f-consent-screen-state.md`. This session did not drain them
  (normal mode, and they are not its candidates to approve).
