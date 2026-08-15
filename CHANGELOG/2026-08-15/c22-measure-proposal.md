# 2026-08-15 — `c22-measure-proposal`

`/wayfinder 12` in **work-through-the-map** mode, `AUTO MODE`. No ticket was named, so the session
picks one: *"take the first frontier ticket in order."*

## 🎯 Claimed — [`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44)

**`C22 · The measure proposal: what the agent offers an unmeasured goal, and in what format`**
— `wayfinder:prototype`, assigned to `idomarhaim`, which *is* the wayfinder claim.

**How the frontier was computed**, so the choice is checkable rather than asserted:

- The map [`#12`](https://github.com/idomarhaim/Android_Final_Project/issues/12) has **31 children**;
  **28 closed**, **3 open** — [`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
  [`C23` #45](https://github.com/idomarhaim/Android_Final_Project/issues/45),
  [`C24` #46](https://github.com/idomarhaim/Android_Final_Project/issues/46).
- All three were **unassigned** (unclaimed) and all three returned `[]` from
  `gh api repos/.../issues/<n>/dependencies/blocked_by` — **unblocked**. No `Blocked by:` line in any
  body either, so the fallback convention agrees with the native relation.
- Frontier = `{#44, #45, #46}`; first in order = **`#44`**.

`#12` itself stays **open**: closing the map is the last act and it is Ido's, per its own *What is left*.

## 🔎 Type of ticket, and what that means for this session

`#44` is **`prototype` → HITL**. Per the skill, *a HITL ticket only resolves through that live exchange;
the agent never stands in for the human's side of it.* So this session cannot close `#44` on its own —
it owes Ido a **concrete artifact to react to**, which is also what `#12`'s Standing preferences demand
(*"a ticket that produces a screen owes more than a list of what appears on it; it owes a concrete,
current design someone can react to"*).

Deliberately **not** read as a missing schema: `#44`'s own body says that reading is what lost the
hand-off in the first place (`C7` → `C11b`, by ordinal).

## 📋 Session-start sweep

- **`SESSIONS.md`** — Active claims read **empty** before the first write; row written under lease
  (`Lock-Path.ps1`, `SESSIONS.md` → `c22-measure-proposal`).
- **`kb-candidates/` — non-empty, 7 files.** One is a **live debt** the previous session flagged and
  did not drain: `2026-08-15-product-v03-spec.md` entry 2 is 🟢 `AUTO MODE`-eligible for
  `kb/dev/decision-map-charting.md`, and draining it is a cross-repo `C:\Dev\JARVIS` visit that owes a
  row on *that* board. Not this unit's work; reported, not silently carried.

---

# Unit 2 — the prototype, revision 1

`docs/prototypes/2026-08-15-measure-proposal/` — six frames, four materials × two themes × two
languages, on the repo's own prototype convention (`index.html` + `README.md`, renderable by
`shoot.ps1`). **`#44` is not closed:** it is `wayfinder:prototype`, therefore HITL, and the one
question that is Ido's is unanswered. Progress posted as
[a comment on `#44`](https://github.com/idomarhaim/Android_Final_Project/issues/44#issuecomment-5302800394).

## The design, in one line

Three **placements** for one offer — the goal's own screen, the `C19` life-area row, the daily review
— which disagree about **when a person is receptive**, and *not* about what the offer says. The offer
is deliberately one component in all three; one that had to be redrawn per placement would be three
features.

**The placement answer decides the call count**, which is why it is asked first: under **C** the call
rides `daily`'s envelope and there is **no fifth AI feature at all**; under **A** or **B** there is
one.

## Derived rather than asked *(each logged, each Ido's to overturn)*

| Decision | Per |
|---|---|
| the absence is stated as legal **before** anything is offered | `C7` — absence is the default; §0.4 |
| an offer never borrows the visual language of an outcome (all dashed, all hollow) | `C19`'s marker reasoning |
| the dismiss is a **peer** of the accept — same size, no colour | absence is the default, so it is not the lesser branch |
| dismissal is **permanent** for that goal, not snoozed | a default that re-asks is not a default |
| the leading indicator is preselected, and only where it has a real target | it is the only branch whose number the app can **compute** |
| **the model returns no number at all** — `targetSource` is an enum naming which arithmetic to run | `C11a`: free numbers swing **2×** run-to-run, **1.8×** between languages |
| the non-AI fallback is a **mechanical half**, not silence | `C18`/`C9a` already hold the counts; silence only where neither exists |

Schema, fallback table and the fifth-call analysis are in the prototype README and the `#44` comment.

## 🧪 Tests

**Rendering is this unit's test layer**, per `docs/prototypes/tools/README.md`: when the acceptance
criterion is visual, render and *look* between revisions. Four rounds, six defects, **five of the six
invisible in the source**:

1. every `.card g-row` stacked its marker above its title — `.card` sets `flex-direction:column` and
   wins at equal specificity. Three frames wrong, markup correct.
2. `.prov` as a flex row rendered its sentence as three narrow columns.
3. the sheet floated with no scrim, so nothing said what it had been opened *from*.
4. both action pairs in frame B floated under the list with three rows above them and nothing saying
   which goal either belonged to.
5. **in neo the sheet is transparent** — neo's surface *is* the page colour plus shadows, so the
   dimmed screen read straight through the sheet and collided with its title. Invisible in glass,
   where the blur hides it.
6. the Hebrew sheet title read `מספר ללהיכנס לכושר` — a double lamed. *A design is not finished until
   it has been seen in Hebrew*, a fourth time.

No code layer is touched (JVM unit, instrumented, `firestore-tests/` all untouched); `#12`'s Standing
preferences forbid a map ticket shipping code at all.

## 📥 KB candidates

`kb-candidates/2026-08-15-c22-measure-proposal.md` — **two entries, neither drained.** Both are
destined for the central KB, which makes draining them a cross-repo `C:\Dev\JARVIS` visit owing a row
on that board — the same reason the `product-v03-spec` entry is still owed. **Work left, not a
defect.**

1. a material defined as *"the page colour plus shadows"* has no opaque layer, so every overlay built
   on it leaks — and it hides in the majority case, because translucent materials blur the leak into
   looking intentional;
2. a prototype frame that *explains* an empty state cannot test whether that empty state reads as
   deliberate.

## 📦 Files

- `SESSIONS.md` — claim row.
- `CHANGELOG/2026-08-15/c22-measure-proposal.md` *(new)* — this file.
- `docs/prototypes/2026-08-15-measure-proposal/index.html` *(new)* — the six frames.
- `docs/prototypes/2026-08-15-measure-proposal/README.md` *(new)* — schema, derivations, rounds.
- `kb-candidates/2026-08-15-c22-measure-proposal.md` *(new)*.
- GitHub: `#44` assignee set; progress comment posted.

## 🚚 This push carries a foreign commit

`git log @{u}..HEAD` at push time held **two** commits, and one is not this session's:

- `82fb125` — **`c23-goal-category`**, *"#45 resolved and closed — the category is machinery, not a
  taxonomy the user sees"* (`CHANGELOG/2026-08-15/c23-goal-category.md`,
  `kb-candidates/2026-08-15-c23-goal-category.md`, `SESSIONS.md`).

`git push` is branch-scoped, not commit-scoped, so it goes up with mine whether or not this session
wants it. Adjudicated per auto-push precondition 5 rather than waved through: that session wrote an
**explicit release note** on `SESSIONS.md` (`d5a9d13` → `82fb125`), its Active-claims row is gone at
`HEAD`, and [`C23` #45](https://github.com/idomarhaim/Android_Final_Project/issues/45) is **closed**.
An explicit release is a positive signal a session writes about itself, so it settles the question
without the transcript check. Paths are disjoint from this session's throughout.

`#46` (`C24`, the settings surface) remains the only unclaimed frontier ticket on `#12`.

---

# Unit 3 — `#44` resolved and closed

Ido was asked the one question that was his — **which placement** — and **handed the decision back**,
in near-identical words to the two delegations this map received earlier the same day: *"I couldn't
fully understand you or the implications of each option — explain simply and schematically… choose
the solution that gives the highest standard and quality of the app (and its purpose), UX/UI and the
software. And if you can improve it, improve it. Should it be in several places?"*

`rules/question-axis-naming.md` forbids re-asking a delegated question, requires **deriving** the
answer, and warns it is often **not in the option set**. It was not.

## The decision — the agent's, and Ido's to overturn

> **It is two things, not one placement: a *marker*, silent, wherever the goal is listed, and an
> *offer*, only on the goal's own screen — because opening the goal is the consent.**

**The fork was false in a specific way, and naming the way is the point:** every option was a
*placement of one object*, and the object is **two**. No member of the set distinguished *stating a
fact* from *making an offer*, so none of them could be right — which is exactly the shape the rule
predicts for a question that gets handed back.

**§0.7 decides it and was already closed** — *intrinsic structure needs consent; instrumental
structure does not.* Breaking a goal into steps is instrumental, which is why `C19` may put its pair
inline in a list; a **measure defines what counts as progress on the goal**, which is intrinsic, and
`C7` already required consent for it (*never auto-applies, dismissible per goal*). So the offer may
not be pushed into a list being scanned for something else; the marker may, because stating a fact
asserts nothing.

**The daily review is ruled out on a second closed decision** — `C10` allocated that screen's three
slots, and it is the one surface that arrives **unasked**. Consequence: **there is a fifth AI call**
(`measure`); under the daily-review placement it would have ridden `daily`'s envelope, which is why
the placement had to be settled before the schema.

**Direct answer to his question — yes for the marker, no for the offer.**

## Rev 2, and the defect the rewrite exposed

Frame C deleted (not greyed out), frame B's offer removed so the row carries only the marker, and the
two rows now differ **on purpose** with `C19`'s pair still on the one below. Then a fifth render
round found the seventh defect: **the `#` marker was a dashed circle**, indistinguishable at a glance
from `C19`'s dashed circle carrying `+`. Every circle in this language is an occurrence or an
outcome, so the number slot is now a **square** — distinguished by **form**, never hue, verified in a
dark-neo Hebrew close-up.

## Written out of this session

- **`#44`** — resolution comment, **closed**.
- **`#12`** (the map body singleton this session held) — *Decisions so far* gains **two** lines and
  *What is left* drops to one open child. **The `C23` line is written here too**: the
  `c23-goal-category` session resolved `#45` and released **without** the `#12` singleton, so its
  index line was owed and the map's own discipline note sanctions a third party writing the gist
  (*"a gist of a public resolution comment, not a second opinion on it"*).
- **`docs/PRODUCT_v0.3.md`** — §1.3 gap marker cleared and the two-surface rule stated; §3.3 gains
  feature **E** with its schema and why it is a fifth call; §3.4 gains the mechanical-fallback row;
  §4.1 gains **an overlay component declares its own opacity**; §10.1 marked ✅ CLOSED with the
  original account kept verbatim, because *how* the hand-off was lost is the finding; §10.2 given a
  factual closed-pointer for `C23` **without** rewriting its spec sections, which are the `#45`
  resolution's own scope; §11 traceability gains `C22`, `C23`, `C24`.

## 🧪 Tests

Five render rounds total, **seven defects, six invisible in the source**. Full list in the prototype
README. No code layer touched.

## 📥 KB candidates

Both entries in `kb-candidates/2026-08-15-c22-measure-proposal.md` **strengthened by this unit rather
than superseded** — entry 1 (a material with no opaque layer) is now also a committed spec line, and
entry 2 (a frame that explains an empty state cannot test it) held again. Still **not drained**:
central-KB destination, cross-repo `C:\Dev\JARVIS` visit owing a row on that board.

## 🔒 Lease note

`SESSIONS.md` came back **BLOCKED by `c23-goal-category`** at release time, ~10 minutes after that
session had already released its *claim*. Per `§5.2` this session did **not** ask — it reordered onto
the changelog and spec work, which need no such path, and took the lease afterwards.

## 🚚 A second foreign commit, on the resolution push

- `6d2397a` — **`social-share-bugs` (reopened, 2nd)**, *"claim the emulator and `goalpilot-56e30` for
  the live round-trip"*. **One line, `SESSIONS.md` only.**

**Adjudicated, not waved through, and it lands differently from `82fb125`.** That session is **live
and mid-unit**, which is normally precondition 5's stop-and-ask trigger — but the trigger keys on *a
foreign commit whose **paths** sit under a live row*, and this commit's only path is the **board
itself**, which is a **commons** (leased, never claimed) and appears in no session's `Owns (paths)`.
What is being published is that session's **claim**, not its work: its Kotlin edits are uncommitted,
outside my pathspec and outside the range. Precondition 4 is met — the commit is fully accounted for,
and its message records that Ido authorised the underlying action. **A claim nobody can see protects
nobody**, so publishing it is the claim's own purpose.

The distinction is worth keeping: **a foreign commit of *work* under a live row stops the push; a
foreign commit of a *claim row* does not.** Both are named here either way.

---

# Unit 4 — the KB drain, which this session had twice deferred

**Ido asked why the ingest had not happened, and whether something was waiting on another session.
Nothing was.** The deferral reasoning — *a cross-repo drain owes a row on the JARVIS board* — is
**true and is a step, not a blocker**; the row costs one commit. `AUTO MODE` already authorises the
ingest, and neither entry was one of the two always-ask kinds. So the deferral was wrong, and it is
recorded as a **pattern rather than a slip**: this is the **second session in one day** to leave the
same debt for the same reason, because an in-repo drain is obviously cheap and a cross-repo one
*feels* expensive for naming a second board.

## 📥 Ingested — three entries into `C:\Dev\JARVIS\kb`

- 📥 **A material that expresses surface as elevation has no fill, so every overlay built on it leaks**
  → `kb/dev/elevation-is-not-a-fill.md` *(new)*
- 📥 **A prototype frame that *explains* the behaviour cannot test it** → `kb/dev/describing-is-not-exhibiting.md` *(new)*
- 📥 **A hand-off written as an *ordinal* evaporates, and no ticket closing can notice** →
  `kb/dev/decision-map-charting.md` **§11** *(from `product-v03-spec`'s entry 2, owed since this
  morning)*

JARVIS commits: `43ef6c5` (claim) → `24c2c28` (ingest) → `ee2c2a5` (release). Board row claimed and
released there, and the release note filed under that board's *Release notes* section — the first one
written under `board-readability-fix`'s new layout.

**The bundle check was *missing* on all three entries** — the *nobody-considered-it* signal, not
`not checked` — so the concept grep ran from scratch, and it found that the parent visual-review rule
(*when the acceptance criterion is visual, render and look between revisions*) **had never reached the
central bundle at all**: it lives only in this repo's `docs/prototypes/tools/README.md`. Nothing was
superseded; `Check-KbLinks` CLEAN at 71 pages.

## Candidate files closed out

- `kb-candidates/2026-08-15-c22-measure-proposal.md` — **fully drained → deleted.**
- `kb-candidates/2026-08-15-product-v03-spec.md` — **partially drained → rewritten down to its
  survivor.** Entry 1 keeps its original number and moves to `## Standing — always-ask`; it stays ⛔
  blocked by an existing `rules/`-shaped group gate, **not re-adjudicated here**. One observation was
  added for whoever eventually puts that question to Ido: both pages written today are **visual-half**
  members of that same family and landed as ordinary KB pages without difficulty — *evidence* the
  family is page-shaped rather than `rules/`-shaped, and explicitly **not** a decision.
- **Seven candidate files remain undrained**, named rather than walked past:
  `2026-08-12-c12-charts-presentation`, `2026-08-13-c15b-stored-ai-text`, `2026-08-13-c2-task-type`,
  `2026-08-13-session-titles`, `2026-08-15-c21-offline-story`, `2026-08-15-c23-goal-category`,
  `2026-08-15-session-identity-tabs`. **Two appeared during this session**, so the folder is filling
  faster than it drains.
