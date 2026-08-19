# c20-derived-state — the map had no ticket left, so the fog patch `C1` discharged became one

> **Summary:** the map had no ticket left, so the fog patch `C1` discharged became one

**Session:** `c20-derived-state` · **Date:** 2026-08-14 · **Mode:** `AUTO MODE`
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#42 · `C20`](https://github.com/idomarhaim/Android_Final_Project/issues/42) *(filed by this session)*
**Invoked as:** `/wayfinder 12` — the **map**, no ticket named, so the pick was the agent's.

## The frontier was empty, and that is a verified result rather than a lookup failure

`/issues/12/sub_issues` reports **26 children, 26 closed, 0 open**.

That endpoint is the one `c15b-stored-ai-text` caught serving a stale `state`, and `c19-area-success-failure`
closed the half of that hole its author's rule could not reach: confirming every child the listing calls
**open** cannot catch an **open** child reported **closed**, because such a child is never queried. So the
listing was reconciled against a collection-wide authoritative query:

```
gh api …/issues/12/sub_issues   → 26 children, 26 closed, 0 open
gh issue list --state open      → 12 open issues in the repo
```

The map `#12` itself plus eleven non-map issues (`#2`, `#4`–`#11`, `#34`, `#36`) = **12 — every open issue
accounted for.** Nothing is hiding behind a stale `closed`. True at claim time.

## Why that did not mean the session was over

`#12`'s own Destination: *"a **v0.3 product spec** — `docs/PRODUCT_v0.3.md` — … The map is done when the
spec is whole **and** no ticket is open."* The second half is now true; `docs/PRODUCT_v0.3.md` does not
exist, so the first is not. `c19-area-success-failure` wrote [`sessions/product-v03-spec.md`](../../sessions/product-v03-spec.md)
for that half.

This session took the other half. The map's **Not yet specified** block still held **five** fog bullets, and
the wayfinder skill's own step 5 makes graduating them part of resolving a ticket — a step the last several
sessions each reported as *"graduated nothing"*.

**Bullet 1 stated its own precondition and the precondition was discharged.** In its words: *"it is not sharp
until `C1` decides whether `points` moves at all."* [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19)
closed deciding **`points` moves server-side**, and closed having **filed nothing**. So a session discharged a
fog patch's stated blocker and did not graduate the patch — which is exactly the gap step 5 exists to prevent,
one step upstream of where anyone was looking.

The other four were each checked and left:

| Fog bullet | Verdict |
|---|---|
| offline story (`A5`/`A6`) | no stated precondition changed — **narrowed by this ticket instead**, see below |
| dashboard reorientation (`A7`) | ⚠️ **the weakest of the four, and flagged rather than settled** — see below |
| `GoogleSignIn` → Credential Manager | says of itself it is **build work, not a product decision** |
| idleness retiring a goal | says of itself it cannot be phrased *"until the `STARTING` offer has been lived with"* — a precondition on **use**, not on a ticket, so nothing can discharge it here |

⚠️ **The dashboard bullet is the honest weak point in that table, and it is the next session's cheapest lead.**
Unlike bullet 1 it states no *"not sharp until X"* clause; instead it says the remaining question *"is
[`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31)'s"* and asks *"whether it lives on
the dashboard or inside [`C9b` #26](https://github.com/idomarhaim/Android_Final_Project/issues/26)."*
**Both of those tickets are closed.** `Observed:` both are `closed` in `/issues/12/sub_issues`, this session.
`Untested:` whether either resolution actually answered the question — their decision lines were **not read**,
because one ticket per session is the rule and bullet 1's discharge was unambiguous while this one needs a read
first. So the patch may be *un-owned* rather than *un-sharp*, which is a different condition from bullet 1's and
was not resolved here. Named so the next session starts there rather than re-deriving it.

## What `C20` decided

Filed as [#42](https://github.com/idomarhaim/Android_Final_Project/issues/42), claimed by assignee before any
work, resolved in [this comment](https://github.com/idomarhaim/Android_Final_Project/issues/42#issuecomment-5295950668).

**The ticket's own trichotomy was false.** *One Function · one trigger per site · a shared module* all assume
every derived number needs a writer. One rule kills the premise:

> **A derived number gets a stored writer if and only if somebody who cannot read its inputs has to read it.**

It is checkable rather than a matter of taste, because `firestore.rules` already draws that boundary at
`isOwner(uid)`. Applied to the seven derived quantities this map has produced, **five want no stored writer**,
one is already correct and untouched, and one is deleted. Exactly one class survives — **a number that crosses
the ownership boundary** — so the answer is **one projection function, two trigger registrations, zero client
writers of derived state**. Neither *"one Function"* nor *"three triggers"* is literally what ships.

Three further results, each decided by a closed ticket rather than by preference:

- **`C1`'s shape generalises and `#34`'s does not — decided by `#34`'s own stated risk.** It named idempotency
  *"the failure that would be hardest to notice"*. Project-from-facts is idempotent **structurally**; running
  it twice writes the same number. Recompute-and-store makes idempotency something the function must be
  careful about. Same argument keeps `FieldValue.increment` rejected: `increment` **is** the accumulator.
- **The offline win is free, and it is the one genuinely *product* half.** `#34` priced its proposal at
  *"a second or two before the donut moves"*. Under this resolution that cost is **not paid at all** — facts
  are ordinary writes that hit the offline cache, so completing a task offline works **for real** (`A5`), the
  donut moves **immediately**, and `#3`'s optimistic overlay, undo message and connectivity pre-check are
  **deleted rather than kept**. Only other people's numbers are eventually consistent, which they always were.
- **The residual is stated, not buried.** The arithmetic ends up in **Kotlin and TypeScript** — a second
  *implementation* that can disagree. Accepted because avoiding it costs the offline win entirely, and pinned
  by a shared `facts → expected numbers` fixture both test layers run.

**No picker was raised.** Every question resolved to a closed ticket, the rules file or the code, so per the
derivable-decision rule the answers were derived and logged. Following `c11b-output-formats`'s precedent, and
its ground rather than its habit: `Inferred:` from the release notes of `c11b-output-formats` and
`c19-area-success-failure` — `C1`, `C2`, `C8`, `C15b` (twice) and `C19` each ended in a hand-back, and `C11b`
deliberately raised no picker after `c15b-stored-ai-text` concluded the failure was **premise, not form**.
Manufacturing a picker out of derivable material is the failure, not the remedy. Everything is Ido's to
overturn.

## Four facts found in the code, three of which contradict the ticket this session wrote an hour earlier

The ticket body was written before the code was read, and the fact pass falsified three of its own claims —
recorded rather than quietly corrected, because the ticket is committed on GitHub as filed.

1. **`User.level` was listed as a site needing a server owner. It is the worked example.** `User.kt:14` is
   `val level: Int get() = Leveling.levelForPoints(points)` — a computed property; `users/{uid}` has **no
   `level` field** at all (`AuthRepositoryImpl.kt:94` writes six fields, none of them `level`).
2. **`publicProfiles.level` is a stored function of `points` in the same document**, and
   `Mappers.kt:176`'s `resolvedLevel()` = `if (level > 0) level else Leveling.levelForPoints(points)` is a
   **fallback that can never fire** — both writers write ≥ 1. Dead code shaped like a safety net; the map's
   *"evidence, not proof"* pattern at a **sixth** site.
3. **The fact stream for goals already exists, and `currentValue` is already a redundant accumulator over it.**
   `ProgressRepositoryImpl.logProgress` writes the `ProgressEntry` and **then**, as a separate step 3, mutates
   the counter. **The two writes are not atomic and nothing reconciles them** — a crash between them leaves
   `currentValue` permanently disagreeing with the entries it claims to summarise, with no repair path. A new
   live defect at a site no ticket had named.
4. **Two independent client transactions write `goal.currentValue`** (`GoalRepositoryImpl.kt:87`,
   `TaskRepositoryImpl.kt:135`), each with its own clamp. The pattern was never three sites converging on a
   server owner; it was two client writers of one field.

All four are filed as spec lines. **This map ships no code and none was written.**

## ⚠️ One session framed and answered the same ticket, which is unusual here

Every other ticket on `#12` was framed by one session and answered by another. The skill's limit — *never
resolve more than one ticket per session* — is respected (one ticket), but the **independent-framing** property
that limit incidentally protects is not, and §0 above is the visible cost: three of the ticket's four listed
sites were wrong, and nobody but the resolving session was there to notice. Recorded rather than glossed.

## The `#12` commons discipline — a clean run

Body fetched, patch built, **re-fetched and `cmp`-compared immediately before the write — unchanged, no race**
— written with **`--input map_patch.json`** (107 KB; `-f body=` still cannot carry it) and verified:
**26 → 27 decisions**, **5 → 4 fog bullets**, **`issues/42` present twice**, and the only two pre-existing
lines lost are the two intended (fog bullet 1 deleted, fog bullet 2 replaced with its narrowed text). The
195 → 196 line delta is the trailing newline GitHub appends, exactly as `c6-log-progress`, `c15b-stored-ai-text`
and `c11b-output-formats` each recorded.

Fog bullet 2 was **narrowed, not answered**: `A5` is discharged as a side effect of `C20`; what remains is
`A6` — whether the app must *say* it is offline — which is presentation.

## 🧪 Tests

**None run, and none applicable.** This unit produced Markdown, a GitHub issue and two GitHub API writes.
`#12`'s standing preference is *plan, don't do* and no source file was modified — read-only greps of Kotlin,
TypeScript and `firestore.rules` only. The project's layers (`app/src/test/`, `app/src/androidTest/`,
`firestore-tests/`) are untouched and were not run; nothing in this commit could change their result.

`functions/` still has **no test layer and no `test` script** — `C11b` named it, `C20` §7 names it again, and
`C20`'s projector is the object that would justify creating one.

## Board

`SESSIONS.md` — claim row written and committed **before the first repo write** (`f08192d`), released in this
commit. **No singleton taken:** no Gradle, no build, no device or emulator, no Firebase deploy, nothing written
in `C:\Dev\JARVIS`. Active claims was empty at claim time and the tree was clean.

## 📥 KB candidates

[`kb-candidates/2026-08-14-c20-derived-state.md`](../../kb-candidates/2026-08-14-c20-derived-state.md) — see
that file for status per entry.

Six other sessions' candidate files were listed before the first unit of work and **none was touched**.

## Push: held, and the drain is held on the same fact

**Not pushed.** `git log @{u}..HEAD` carries three commits, and one is **foreign**:

| Commit | Session | Verdict |
|---|---|---|
| `f08192d` | `c20-derived-state` | this session's claim |
| `478769d` | **`c11b-output-formats`** | *kb-candidates: entry 2 drained — it shipped as JARVIS decision-map-charting §9* |
| `5533bc1` | `c20-derived-state` | this session's resolution |

`c11b-output-formats` has **no live row** on this board, but precondition 5 is explicit that an absent row is
not proof a session is finished — and their commit is timestamped **19:51:57**, minutes before this check. A
recent commit of theirs means **live**, so the range stops here. Nothing is broken and nothing was swept: their
commit landed between this session's two, both of which staged and committed by **explicit path**, and
`git diff --cached` before the commit showed this session's three files only.

`Observed:` re-checked at the moment of reporting — `git log HEAD..@{u}` is empty and all three commits are
**still unpublished**. Stated dated rather than bare, because a sibling's push is branch-scoped and would carry
them without any gate of this session's.

**📥 The KB drain is held on the same fact, not on a second one.** All three candidates are 🟢 and the JARVIS
board's Active claims is empty with a clean tree there — so the hold is **not** cross-repo logistics. It is
that every `/kb-ingest` writes `kb/index.md` and `kb/log/2026-08-14.md`, and entry 3's destination is
`kb/dev/decision-map-charting.md`, whose **§9 `c11b-output-formats` created at `3f59fe9` today**. Racing an
ingest through those three files against a session that committed three minutes ago is the exact
cross-contamination this board has now recorded five times — and it is the same ground `c11b-output-formats`
itself used yesterday to hold an `AUTO MODE`-eligible entry. The candidates are **committed**, so nothing is
lost by the wait; entry 3 should land as **§10**, sibling to their §9.
