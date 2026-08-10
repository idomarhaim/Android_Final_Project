# `kb-ingest-backlog-drain` — 2026-08-10

**Ingest only, Markdown only.** No Kotlin, Gradle, Firestore-rules or Functions
file created or modified; no issue written, no ticket resolved, no singleton taken,
live `goalpilot-56e30` never contacted.

Bare `/kb-ingest`. The folder sweep that opens the skill found **five un-ingested
candidate files, 21 entries** — and, unlike every previous drain in this repo,
**every owning session had already released**, so none of them was "a live
session's to drain" and all five were takeable at once.

Ido chose a **per-file drain, oldest first** (five passes, one commit each, so an
interruption costs one pass rather than the sitting), and chose to **park the
always-ask entries and be given the `rules/` proposals** they imply.

## What the sweep actually found

| File | Entries | Disposition |
|---|---|---|
| `2026-08-08-c9d-calendar-scopes.md` | 3 | drained 3/3 |
| `2026-08-08-fix-task-completion-feedback.md` | 5 | — |
| `2026-08-09-c9f-consent-screen-state.md` | 5 | — |
| `2026-08-09-entity-model-intake.md` | 3 | — |
| `2026-08-10-c4-goal-task-ontology.md` | 5 | — |

**One always-ask flag was resolved by checking rather than inheriting it.** The
`c4-goal-task-ontology` file predicted **three** always-ask entries. `c9f`'s entry 4
was flagged *"⚠️ Yes — check before ingesting"* on the possibility that some KB page
carried the "production hard-blocks sensitive scopes" claim. Grepped the whole
bundle: **no page carries it.** That claim only ever lived in this repo's own docs,
and `c9f-consent-screen-state` already corrected them there on 2026-08-09. So it
supersedes nothing in the KB and is ordinary to ingest. **Two always-ask entries
remain**, both `rules/`-shaped, both Ido's call: `c9f`#1 and `entity-model-intake`#1.

## Reconciliation — update-in-place beat "new page" in four places

Step 3 of the skill says update beats create, and the candidates had been written
without knowing what the bundle already held:

- **`firestore-write-semantics.md` already exists**, written from
  `product-device-pass`'s *reproduction* and ingested 2026-08-08. So
  `fix-task-completion-feedback`#1 is not a new page — it is the **fix-side** half
  of a page that already carries the diagnosis.
- **`android-device-verification.md` §6 already carries** the variable-frame-rate
  `screenrecord` / `pts_time` technique, so #5 extends it rather than superseding
  it — which also cleared that entry's conditional always-ask flag.
- **`review-intake-and-triage.md`** is the right home for
  `entity-model-intake`#2, not a new page.
- **`decision-map-charting.md`** absorbs **four** entries from three different
  files — the single most-contended destination in this drain.

## Pass 1 — `c9d-calendar-scopes`, 3/3

**New:** [`kb/dev/recovery-masks-failure.md`](../../../JARVIS/kb/dev/recovery-masks-failure.md)
— the better the recovery path, the weaker the signal. A recurring policy-level
fault inside correct error handling, whose recovered state is indistinguishable
from a legitimate first run, has no observer: the shipped Google Tasks import had
been re-consenting **weekly** because the project sat in `Testing`, and nobody could
have filed it by observation. The check is reading the policy that governs the
resource, not watching the system.

**New:** [`kb/dev/google-oauth-scopes-and-consent.md`](../../../JARVIS/kb/dev/google-oauth-scopes-and-consent.md)
— `calendar.app.created` is the whole dedicated-calendar loop in one narrow scope,
and its blindness to every other calendar is the design constraint rather than a
gap to work around. Never create the calendar on a service account: since the 2026
lifecycle change a secondary calendar has one **data owner** and orphans are deleted
outright, so a service-account calendar is an orphan by construction — and that is
exactly the architecture a project with existing Cloud Functions reaches for first.

**Updated in place:** `kb/dev/decision-map-charting.md` gains **§3** — a question
that reads like research can be unanswerable by research, because the answer is a
setting in a console you own. *If two projects using the same API can have different
answers, it is not a documentation question* → file a **task** ticket.

Candidate file `git rm`'d. `Check-KbLinks` **CLEAN at 38 pages**.

## Pass 2 — `fix-task-completion-feedback`, 5/5 (4 central, 1 project-local)

**The pass where reconciliation earned its place in the skill.** Three of the five
entries proposed **new** central pages that already existed — written two days
earlier from `product-device-pass`, the session that *reproduced* the same defect,
and ingested 2026-08-08. Entry 5 had hedged it explicitly (*"check first — may
already have been ingested"*), and it had been: as `android-device-verification.md`
§6, down to the letterboxing trap the entry offered as new. That hedge is the only
reason this pass did not create a duplicate page, and it is worth copying into
future candidates.

**New (central):** `kb/dev/optimistic-ui-patterns.md` — retire an optimistic
overlay against **observed data**, never against the write's completion. The two
travel on independent channels with no ordering guarantee, so clearing on
completion re-renders the **old** state for a few frames on every *successful*
action; retiring on agreement is a no-op and cannot flicker. Failures invert it —
clear immediately, because clearing *is* the undo.

**Updated in place (central):** `kb/dev/firestore-write-semantics.md` gains §5–§6 —
the fix side on the same instrument as the before-numbers, whose general lesson is
**measure how long a failure takes to arrive, not merely that it arrives** (7.9 s of
retry before `UNAVAILABLE`, so a correct rollback still shows a lie for eight
seconds); and why the standard swap was refused — `FieldValue.increment` expresses
neither a clamp nor a derived field, so batch+increment **deletes** the guarantee
rather than relocating it. `kb/dev/android-device-verification.md` §6.2 gains
`ffmpeg -vsync 0` and the point that the instrument must be **reused on the fix**,
not only on the defect.

**New (project-local):** [`knowledge/ui-error-conventions.md`](../../knowledge/ui-error-conventions.md)
— the `Resource.Error` boundary: repository text for refusals the **domain**
generated, a written message for failures the **network** generated. Kept local
deliberately; promotion waits for a second project showing the same shape.

Candidate file `git rm`'d. `Check-KbLinks` **CLEAN at 39 pages**.

## 🧪 Tests

**No suite run, and none applicable** — this session created or modified no Kotlin,
Gradle, `firestore.rules` or Cloud Functions file, so the JVM, instrumented and
rules layers all have nothing under test. Verification for an ingest is the
bundle linter: `Check-KbLinks.ps1` clean after every pass.
