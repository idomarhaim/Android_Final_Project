# KB candidates — `backlog-triage`, 2026-08-15

Repo: `c:\Dev\Android_Final_Project`. Branch: `feat/goalpilot-implementation`.
Session brief: `sessions/backlog-triage.md`. Changelog: `CHANGELOG/2026-08-15/backlog-triage.md`.

---

## 1 · A gating note predicts a dependency from the blocker's *scope*; the resolution is what decides it

**Claim.** When a brief is gated on tickets that have not resolved yet, its precondition note names
which downstream items each blocker will affect — and those names are predictions drawn from the
blocker's **title and scope**, not from its answer. They must be **re-checked against the resolved
artifact** before being propagated into the work, because a resolution routinely *removes* a
dependency the scope implied. Propagating one unchecked costs the downstream ticket a blocker it does
not have.

**Why.** Observed here, twice in one brief and in opposite directions:

- The brief's `C24` note said §4.9 (the settings surface) *"is a **precondition** of `#9` and `#36`,
  since both need settings that live on it."* Checked against the resolved §4.9: its contents are
  Appearance / Language & region / Your day / AI / Account. **`#9` needs none of them** — §1.4 answers
  `#9` *unconditionally and with no threshold*, so there is nothing left to configure. The prediction
  was reasonable when written (a duration box *could* have wanted an AI-estimate toggle) and the
  resolution removed it. Had it been propagated, `#9` — a small, fully unblocked UI ticket — would
  have been sequenced behind a whole screen.
- The same note's other half **held**: `#36`'s consent state does land on the Profile/Settings split.
  So the failure is not *"gating notes are wrong"*; it is that **a note contains verified and
  predicted claims side by side with nothing marking which is which.**

The cheap discriminator is that a precondition note is written **before** the answer exists, so every
dependency in it is `Inferred:` by construction, whatever tense it is written in. What makes it
expensive is that it reads as settled by the time the gated session opens — the blocker is closed, the
spec is written, and the note now sits *above* the resolved text in the same file.

**Rejected:** *"just re-read the whole spec"* — that is what happened here and it worked, but it does
not generalise: the check only fired because the note named specific issue numbers that could be
tested one by one. The transferable act is **testing the note's named dependencies against the
resolution, one at a time, and saying in the downstream ticket which prediction failed** — this
session wrote that correction into `#9`'s body and comment so the next reader cannot re-import it.

**Related, and it is the same shape one layer up:** `claim-provenance`'s `Observed:` / `Inferred:` /
`Untested:` hedges exist precisely because prose gives a later reader no way to tell a watched claim
from a guessed one, and an unhedged claim **propagates by copying, each copy reading as
corroboration**. A gating note is that hazard with a schedule attached.

**Destination.** `kb/dev/` — a page on brief/gating-note rot, or an existing page on precondition
handling if one exists.
**Anchors.** `sessions/backlog-triage.md` (the note, kept in the brief as written);
`CHANGELOG/2026-08-15/backlog-triage.md`; issue
[#9](https://github.com/idomarhaim/Android_Final_Project/issues/9)'s reconciled body, section
*Not a settings item*.
**Supersedes.** Nothing known.
**Status.** Pending — `AUTO MODE` is in effect, so this drains at the next commit trigger via
`/kb-ingest` unless it is judged too thin to write a page from.

---

## 2 · Verify a spec's code citations against `HEAD` before filing a ticket on them

**Claim.** A spec that names code sites by `file:line` is a **snapshot**, and its line numbers drift
the moment anything below them moves. When a downstream ticket is filed *from* those citations, the
citations must be re-read at `HEAD` first — otherwise the ticket sends a build session to a line that
no longer says what the spec claims, and the build session has no way to tell drift from a
misreading.

**Why.** `docs/PRODUCT_v0.3.md` §7.2 cites the two cross-boundary DTOs as `Dtos.kt:77` / `:118`. At
`HEAD` the file had moved to `data/firestore/dto/Dtos.kt` and they were at `:83` and `:124`. **The
finding was entirely correct** — `PublicProfileDto` has no timestamp of any kind, and
`ChallengeParticipantDto` has `joinedAt` and no `updatedAt` — so nothing about the decision changed;
only the coordinates had rotted, and a ticket repeating them would have looked wrong at exactly the
moment someone tried to check it.

The check is cheap and it pays twice: re-reading `ProgressRepositoryImpl.logProgress` for the same
reason **found a second failure path the spec had not recorded** — the entry is committed before the
counter moves, and the `catch` reports failure *after* the entry has landed, so an ordinary retry
writes two entries for one event with no crash involved at all. A defect ticket filed by copying §10's
sentence would have carried only the crash window, which is the harder repro.

**Rejected:** citing the spec section alone and omitting the line — it is the line that makes a ticket
actionable, and dropping it pushes the same search onto every reader instead of doing it once.

**Destination.** `kb/dev/` — alongside whatever covers spec-to-ticket handoff, or a short page of its
own.
**Anchors.** `docs/PRODUCT_v0.3.md` §7.2 and §10 defect 1; issues
[#49](https://github.com/idomarhaim/Android_Final_Project/issues/49) and
[#50](https://github.com/idomarhaim/Android_Final_Project/issues/50), which carry the verified
numbers; `CHANGELOG/2026-08-15/backlog-triage.md`.
**Supersedes.** Nothing known.
**Status.** Pending — drains at the next commit trigger under `AUTO MODE`.
