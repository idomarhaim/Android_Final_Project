# c11b-output-formats — claiming #30 · `C11b`, the map's terminal ticket, after three declines

**Session:** `c11b-output-formats` · **Date:** 2026-08-13 · **Mode:** `AUTO MODE`
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#30 · `C11b`](https://github.com/idomarhaim/Android_Final_Project/issues/30)
**Invoked as:** `/wayfinder 12` — the **map**, no ticket named, so the pick was the agent's.

## The claim

**[#30 · `C11b` — *The output-format spec for every AI feature*](https://github.com/idomarhaim/Android_Final_Project/issues/30)**,
assigned to `idomarhaim` on GitHub **before any other work**. Per the wayfinder skill the assignee
*is* the claim, so the claim does not depend on this commit.

## Frontier derivation — and it ran the two-directional check the last two sessions established

Both halves were run, because each catches what the other cannot:

```
gh api …/issues/12/sub_issues   → 26 children, 23 closed, 3 open   (aggregate — state is untrusted)
gh issue view 30|35|41          → each open child confirmed DIRECTLY (c15b's rule)
gh issue list --state open      → 15 open in the repo, all accounted for (c19's cross-check)
```

The three open children (`#30`, `#35`, `#41`) plus the map `#12` plus eleven non-map issues
(`#2`–`#11`, `#34`, `#36`) = **15**, matching the repo-wide query exactly. Nothing hiding behind a
stale `closed`, nothing reported open that had closed. `blocked_by` queried directly per ticket.

| Ticket | `blocked_by` | Assignee | Verdict |
|---|---|---|---|
| `#30 · C11b` | `#19` ✅ `#20` ✅ `#24` ✅ `#29` ✅ | — | **frontier — CLAIMED** |
| `#35 · C15b` | `#24` ✅ `#29` ✅ | `idomarhaim` | claimed by `c15b-stored-ai-text` (live at derivation; **closed minutes later**) |
| `#41 · C19` | *(none)* | `idomarhaim` | claimed by `c19-area-success-failure` (live) |

**The frontier — open, unblocked, unassigned children — was exactly `{#30}`.** Not a pick among
candidates: the other two open tickets were both under live claim, so there was one takeable ticket
or none.

## Why `#30` is taken now, after `c5-endless-goals`, `c15b-stored-ai-text` and `c19-area-success-failure` each declined it

Three sessions declined it as *terminal by design*, on the map's own sentence — *"you cannot test a
format nobody has designed yet."* **That sentence is a sequencing rule, not a prohibition, and
sequencing rules expire by being satisfied.** `#30`'s body names its condition precisely: *"It is
deliberately blocked on all four features it serves"* — `C1` (#19), `C2` (#20), `C8` (#24), `C10`
(#29). **All four are closed, and `C2` §6 recorded #30 as "now fully unblocked — this was its last
open blocker."**

The two declines that were **not** about the four blockers were checked rather than inherited:

1. **`c15b`'s ground — *"`#41` is an open, unresolved, AI-touching ticket."*** `#41`'s body was
   read. Its four questions are *is an abandoned goal a failure*, *does a failure age out*, *what is
   the view*, and *how does it avoid being a list of things you are bad at*. **None asks the model
   for anything**; it is a view over `C9a` occurrence states, labelled `wayfinder:prototype`. It
   adds no fifth AI feature, so it is not one of the four `#30` waits on.
2. **`c19`'s ground — *"`#30` fixes a field contract `#35` is mid-way through deciding."*** Real at
   the time, and **it expired during this session's fact pass: `#35` closed.** Its resolution is now
   an input rather than a collision, and it is a strong one — *no AI prose is persisted server-side
   at all*, so `#30` writes **response** schemas over nothing `#35` still owns. `C15`'s resolution
   had already drawn that boundary explicitly, leaving `#30` only *"the per-feature veto where the
   model's Hebrew is not good enough."*

**What is honestly conceded:** `#30` is `wayfinder:grilling`, so it is HITL, and Ido's attention is
contended — `c19-area-success-failure` holds a live prototype ticket and a 🎬 offer is owed from
`picker-queue-merge`. That is recorded in the board's Singletons column rather than glossed, and it
is the reason the fact pass ran **first**: `#30`'s four bullets are largely answered by closed
tickets, so what reaches him should be small.

**Leverage, stated plainly:** `#30` is the **last** ticket on this map. The destination is *"the map
is done when the spec is whole and no ticket is open"*, and closing `#30` and `#41` is all that
stands between here and that. A terminal ticket that every session declines is a map that never
finishes.

## The `#12` commons — the coupling named on claiming, not discovered later

The map's *Decisions so far* is a commons whose race has fired for real three times (`c3`, `c1`,
`c2-task-type`). Discipline for the resolution append, unchanged: **re-fetch `#12`'s body
immediately before writing, `cmp` against the copy the line was built on, write only this session's
line, verify a pure insertion.** The body is **~99 KB**, so `gh api --method PATCH -f body="$(cat …)"`
**dies with `Argument list too long` after you believe you have written it** — two sessions have paid
for that. Use `--input <file.json>` and verify the round-trip. State read tonight: **24 decision
lines** (23 plus `C15b`'s, appended minutes ago), **4 fog bullets**.

## Staging — a clean fix was built, verified, and then **defeated**, and the defeat is the finding

`c15b-stored-ai-text` wrote its **57-line release note** into `SESSIONS.md` while this row was being
written. `git add SESSIONS.md` would have swept it — plus their own row-removal — into this claim
commit, which is exactly the failure `c5-endless-goals`, `picker-queue-merge` and
`c19-area-success-failure` each recorded suffering tonight, and which per-file staging **cannot**
prevent, because it stops *you* sweeping a sibling in and says nothing about one commons file
holding two sessions' edits.

So this session built the fix that appears to close it — mechanical, non-destructive, never touching
the working tree:

```bash
git show HEAD:SESSIONS.md > head.md          # reconstruct the committed file
#   …insert this session's row into head.md, and nothing else…
SHA=$(git hash-object -w staged.md)          # write a blob that is HEAD + one row
git update-index --cacheinfo 100644,$SHA,SESSIONS.md
```

It worked exactly as intended and was **verified**: `git diff --cached --stat` reported
`SESSIONS.md | 1 +`, a single insertion, with their note left in the worktree as theirs to commit.

**And it made no difference, because between that verification and this commit, `c15b` committed.**
Their `git add SESSIONS.md` read the **working tree** — which held both their note and this row —
so `406874d` (*"c15b: resolve #35"*) carries **this session's claim row**, and by the time
`git commit` ran here the index had been refreshed out from under it and only the changelog
remained. Confirmed rather than assumed: `406874d` adds both `c11b-output-formats` and their own
release note to `SESSIONS.md`, and `git show HEAD:SESSIONS.md` contains this row exactly once.

**The finding, and it corrects the section this replaced.** The index is a **shared singleton**, and
a sibling committing from the same working tree reads the *tree*, not your *index*. So index-level
surgery is not a fix for the two-sessions-one-commons problem at all — it is a **strictly one-sided**
guard, protecting a sibling from you, precisely like per-file staging, and failing in precisely the
same direction. It is the fifth instance tonight and the first in which a deliberate countermeasure
was tried and lost. **The only thing that actually partitions this is a worktree per session**,
which is heavier and off by default under the standing no-worktrees rule.

**Nothing was lost and nothing is rewritten** — un-picking it would need a history rewrite, which is
always-ask in both modes. The cost is provenance: a claim row filed under another session's commit
message. Filed as a KB candidate.

## 📥 `kb-candidates/` listed before the first unit of work — six files, each opened

`Status` and `Destination` read out of the files themselves, not inherited from board notes. The
three that `picker-queue-merge` drained (`c16`, `c9e`, `c8`) are **gone**, deleted at `912d4bc`.

| File | Status |
|---|---|
| [`c12-charts-presentation`](../../kb-candidates/2026-08-12-c12-charts-presentation.md) | entry 1 ⏸️ always-ask; entries 2–4 🟢 held by their own text |
| [`c2-task-type`](../../kb-candidates/2026-08-13-c2-task-type.md) | entry 1 🟢 held on a cross-repo hold into `C:\Dev\JARVIS`; entry 2 ⛔ `rules/` |
| [`c5-endless-goals`](../../kb-candidates/2026-08-13-c5-endless-goals.md) | ⛔ `rules/question-axis-naming.md`, parked, 🎬 offer owed |
| [`session-titles`](../../kb-candidates/2026-08-13-session-titles.md) | ⛔ `rules/agent-topology-and-model-routing.md` §5.3, 🎬 offer owed |
| [`c15b-stored-ai-text`](../../kb-candidates/2026-08-13-c15b-stored-ai-text.md) | entry 1 🟢 cross-repo hold; entry 2 ⛔ `rules/` — the **eighth** parked amendment to one file |
| [`c19-area-success-failure`](../../kb-candidates/2026-08-13-c19-area-success-failure.md) | 🟢 cross-repo hold, same section as `c2`'s and `c15b`'s entry 1 |

**None of the six is this session's**, so `AUTO MODE` drains nothing here — the auto-ingest gate
covers the candidates *the committing unit produced*.

## 🧪 Tests

**None, and none applicable.** This commit is Markdown plus GitHub metadata; the map's standing
preference is **plan, don't do**, and no ticket on `#12` ships code. No build, no device, no
Firebase, no Gradle daemon, no emulator — no singleton taken.

---

# Resolution — #30 closed

**Posted:** [issue #30, resolution comment](https://github.com/idomarhaim/Android_Final_Project/issues/30#issuecomment-5273699910) · **Closed:** 2026-08-13 · **Map line:** appended to `#12` *Decisions so far*

## No picker was raised, and that is a decision rather than an omission

`#30` is labelled `wayfinder:grilling`, so HITL by the skill's definition. **The fact pass found
every question on it answered by a closed ticket, by `C11a`'s 248 live calls, or by the code**, so
per the derivable-decision rule the answers were derived and logged for overturning rather than put
to Ido. The ownership sort was run explicitly and **nothing survived on his side of it** — every
question turns on the artifact.

This matters because the **last four decision tickets on this map all ended in a hand-back in
near-identical words** — `C1`, `C2`, `C8` and `C15b` (twice) — with `C3`, `C14` and `C17` recording
the same pattern before them by their own accounts. `c15b-stored-ai-text` concluded three hours ago
that the failure was **premise**, not form. Manufacturing a sixth picker out of derivable material
would have been the failure this map keeps recording, not the remedy for it.

## What was decided

1. **The ticket's own inventory was wrong — five AI features, not four.** `classifyTask` is one no
   `C` ticket owned, it is the **highest-volume** call in the app, and it is where `C11a`'s only
   measured failure mode occurred.
2. **The wide-vs-narrow fork is false.** *One call means one failure* describes the `catch`, not the
   call. **Per-field-group validation** buys independent failure at zero extra requests, so the split
   axis `C11a` offered is retired on `C11a`'s own numbers: 20/20 usable estimates instead of 18/20,
   at one request instead of three.
3. **Five schemas written out**, fields, types and enumerated values — `estimate`, `plan`, `daily`,
   `classify`, plus the shared envelope (`language`, optional `provider·model·key`, membership lists).
   **No `points` field anywhere, ever.** The model may not mint an id; a new plan step carries no id
   at all and is identified by array position, which makes `C11a`'s truncation failure structurally
   unrepresentable rather than merely checked.
4. **The failure contract: omit, never substitute.** Three classes — transport, structural, semantic
   — with **no retries** (a retry aims at a class that did not occur once in 248 calls and spends the
   30-RPM budget the wide call exists to save). Only the failing field is dropped; the client's
   fallback for an absent field is its own ticket's specced one.
5. **Validation lives in the Cloud Function, singly**, because `C13` put all four provider adapters
   server-side; native `json_schema` enforcement stays alongside it, catching a different class.
6. **`C15`'s per-feature Hebrew veto is declined and rebuilt as a per-response script-share check.**
   `C11a` measured bad Hebrew as a missing prompt line (0/10 → 3/3), not a ceiling, so vetoing a
   feature on the Hebrew of a prompt that never asked for Hebrew would be exactly the assumption
   `C11a` exists to prevent. `C15b`'s `\p{Hebrew}` test — filed three hours ago — is the instrument,
   and it applies to **speech and never to content**.

## Defects found in live code — filed as spec lines, not fixed

1. **One membership contract, three enforcement sites, two layers.** `suggestedLifeAreaId` is checked
   in `RecommendationRepositoryImpl.kt:136`; `suggestedGoalId` in `DashboardViewModel.kt:178` and
   again in the import path.
2. **The client substitutes plausible values, then reconstructs which were real.**
   `TaskScoring.looksLikeFallback` (`TaskEstimate.kt:100`) recomputes the fallbacks and compares —
   and **its own KDoc concedes the method is unsound**: *"Evidence, not proof… a model is free to
   land on the same two numbers by agreement rather than by failure."* Fifth site of the map's
   most-repeated defect.
3. **`TaskDuration.fallbackMinutes` (`TaskEstimate.kt:40`) derives minutes from points**, while
   `C3`/`C1` make points a product **of** minutes — the fallback runs the app's own arithmetic
   backwards, and under `C1` §5 there is no `points` on the wire for it to invert.
4. **`functions/` has no test layer** — no `test/` directory, no `test` script in `package.json`.
   This ticket creates the most testable object on the map, with `C11a`'s 248 recorded calls sitting
   ready as fixtures.

## The `#12` commons — a clean run

Body fetched → line built → **re-fetched and `cmp`'d byte-for-byte immediately before the write:
identical, no race** → written with `--input map_patch.json` (102 KB; `-f body=` still dies with
`Argument list too long`) → **verified a pure insertion: 0 lines removed, 24 → 25 decision lines,
`C11b` present once, fog unchanged at 4 bullets.** The single extra added line is the trailing
newline GitHub appends.

## Frontier after this ticket

**Filed nothing. Graduated nothing. Ruled nothing out of scope. Unblocked nothing** — `#30` was
terminal, re-checked live against the `blocked_by` of every open issue (`#6`, `#8`, `#34`, `#36`,
`#41`): none lists it.

**[`C19` #41](https://github.com/idomarhaim/Android_Final_Project/issues/41) is now the only open
ticket on the map**, claimed and live under `c19-area-success-failure`. When it closes, `#12`'s
destination is reached and what remains is writing `docs/PRODUCT_v0.3.md` from twenty-six resolutions
— a build-session hand-off, not a decision.

## 🧪 Tests — resolution unit

**None, and none applicable.** Markdown, GitHub metadata, and read-only greps of Kotlin and
TypeScript. `#12`'s standing preference is **plan, don't do**; no ticket on this map ships code. The
missing `functions/` test layer is named above as a spec line for the build session — it is *this
ticket's finding*, not a layer this session skipped.

---

## 📥 The `AUTO MODE` drain — attempted, and correctly refused

`AUTO MODE` drains the candidates *the committing unit produced*, so both entries were taken to the
drain. **Neither landed, and reading the destinations is what stopped them — both Status blocks as
first written were wrong.**

**Entry 1 was filed as new `kb/dev/` material and is not.** `picker-queue-merge` committed the
governing block into `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5 hours earlier
(`843a0b4`), already carrying the one-direction-only finding and its three remedies. What survives is
**one clause that corrects that text**: the rule locates the exposure window at *"the moment you
`git add`"*, and this session's row shipped inside a sibling's commit **having never been
`git add`-ed** — their `git add` reads the **working tree**. Exposure opens when the content reaches
the file, which kills the fourth remedy (index surgery) and weakens the first (*stage as late as
possible* measures lateness from the wrong event). That makes entry 1 ⛔ always-ask **three times
over**: `rules/` destination, a contradiction of a standing claim, and a file under the live
`liveness-from-transcript` claim.

**Entry 2 is 🟢 and `AUTO MODE`-eligible, and was still not drained — on a singleton, not on its
merits.** Its destination is a new section beside `kb/dev/decision-map-charting.md` §8 (checked: the
page exists, and §8 is the near neighbour — *a ticket body never ages*; this is the sibling case
where a previous session's *verdict* never ages). But every `/kb-ingest` writes `kb/index.md` and
`kb/log/2026-08-13.md`, and both sit **uncommitted in the working tree of a live JARVIS visitor** —
`c15b-stored-ai-text`, mid-drain on `runtime-verification.md`. Racing a second ingest through those
two commons files is precisely the contamination entry 1 is about, so it waits for the next session.

**And the drain corrected a claim this session had already committed to the board:** that the
cross-repo hold parking `c2-task-type`'s, `c15b`'s and `c19`'s entries *"has expired"* because
`picker-queue-merge` released. It has not — **two** sessions are live in `C:\Dev\JARVIS`. The hold
moved; it did not lift. It was asserted from a release commit without reading that board, which is
the same read-it-don't-infer-it failure this map has now recorded three nights running.
