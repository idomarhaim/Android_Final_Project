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

## 🧪 Tests

None — this unit writes no code. The layers this repo has (JVM unit, instrumented, `firestore-tests/`)
are untouched, and `#12`'s Standing preferences forbid a map ticket shipping code at all.

## 📦 Files

- `SESSIONS.md` — claim row.
- `CHANGELOG/2026-08-15/c22-measure-proposal.md` *(new)* — this file.
- GitHub: `#44` assignee set.
