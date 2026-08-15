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
