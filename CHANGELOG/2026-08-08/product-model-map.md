# `product-model-map` — the 13 undecided product questions become a navigable map

> **Summary:** the 13 undecided product questions become a navigable map

**Session:** `product-model-map` · **Brief:** `sessions/product-model-map.md` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL throughout) ·
**Started** 2026-08-06, **landed** 2026-08-08.

Charting only. **No ticket was resolved, and no code was touched** — that is the
skill's own boundary and the brief's, and it held.

## What exists now that did not before

**[#12 · GoalPilot v0.3 product model — wayfinder map](https://github.com/idomarhaim/Android_Final_Project/issues/12)**,
labelled `wayfinder:map`, with **20 decision tickets** as native GitHub sub-issues
and **25 blocking edges** between them. Five tickets are on the frontier — open,
unblocked, unclaimed — and GitHub renders the blocked/unblocked split in its own
UI, so what is takeable is visible without opening the map.

| | |
|---|---|
| Map | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) |
| Frontier (takeable now) | [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13) `C4` ontology · [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14) `C7` units · [#15](https://github.com/idomarhaim/Android_Final_Project/issues/15) `C15` localization · [#16](https://github.com/idomarhaim/Android_Final_Project/issues/16) `C11a` free-model probe **(AFK)** · [#17](https://github.com/idomarhaim/Android_Final_Project/issues/17) `C9d` Google scopes **(AFK)** |
| Blocked | [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18)–[#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) |
| Labels created | `wayfinder:map`, `:grilling`, `:research`, `:prototype`, `:task` |

## The five things Ido decided, none of which were derivable

The map's first act is naming the destination, and the brief was explicit that the
TODO file's proposal was *a proposal to test, not a decision already taken*. Two of
the five answers changed the map's shape substantially.

1. **The destination is a written v0.3 product spec** (`docs/PRODUCT_v0.3.md`), not
   merely a set of closed tickets. A build session reads one document, not twenty
   issues.
2. **The audience is one real user — Ido, daily.** Not a course artifact, not a
   product for strangers. This is what makes quote licensing (`C10`) a small
   question and bring-your-own-key (`C13`) a genuine bonus.
3. **The free model is a *permanent* constraint**, not a current budget. Nobody
   should ever have to pay or supply a key. Consequence, now binding on every AI
   ticket: *an AI feature that cannot run reliably on the free tier is specced with
   a non-AI fallback beside it, or it is not specced.*
4. **The calendar (`C9`) is fully in scope** and charted here — the brief expected
   it to earn its own map. It became five tickets, and the map grew by a third.
5. **Localization is in scope**, and this is the one nobody saw coming. Asked
   whether Hebrew/RTL was in or out, Ido answered with a requirement instead: a
   language picker in the app, everything the app wrote then written in that
   language, all of it switching at one button press. **It appears nowhere in
   `R1`–`R28`** — it came from the device pass's `A1` plus his own answer — and it
   has a real edge into the free-model probe, because a small model's Hebrew is not
   its English.

## Where the proposed charting order was wrong

Recorded rather than quietly replaced, because a proposal that was wrong is worth
more to the next reader than one that vanishes. Full version in
`TODO/TODO_FUTURE/ProductModel.TODO.future.md`.

- **`C11` was proposed as the root that prices everything. It is two questions.**
  *What can the free model do* is measurable today; *what are the formats* cannot
  be written before the features exist — **you cannot test a format nobody has
  designed yet.** Split into [#16](https://github.com/idomarhaim/Android_Final_Project/issues/16)
  (unblocked, AFK, frontier) and [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30)
  (blocked on four tickets — the most-blocked on the map). Had it stayed one
  ticket, it would have had to invent the formats it was meant to be testing.
- **The "C1–C4 knot" was ordered, not merged.** Four questions where "none can be
  answered alone" usually means they are one ticket; here it meant the chain had
  never been drawn. It is `C4` → `C3` → `C1` → `C2`. **`C4` is the map's true
  root, not `C11`.**
- **`C7` (what is a unit) turned out to be unblocked**, not a consequence of the
  knot — and it is the fastest route to unblocking already-filed work
  ([#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)) and
  [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23).

## Two things found, not built

- **The tracker supports both native primitives.** `gh issue edit --parent /
  --add-sub-issue` exists in gh 2.96, and the GraphQL API exposes `addBlockedBy` /
  `blockedBy` / `blocking`. The wayfinder skill treats native blocking as
  essential — it is what renders the frontier in the tracker's own UI — and this
  repo did not have to fall back to a `Blocked by: #N` body convention.
- **`D1` became `C14`.** The concurrent `product-device-pass` session reclassified
  it as an undecided model and left it for Ido to re-assign, since
  `TODO_FUTURE/` is this session's path. He approved the move; it is now
  [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23), blocked on
  `C7` because `Challenge.metricUnit` has the identical free-text disease as
  `Goal.unit`.

## Concurrency note, recorded rather than papered over

The `SESSIONS.md` claim row for this session **never made it into a commit of its
own.** It was written into the working tree, and `product-device-pass` staged
`SESSIONS.md` moments later, so the row rode into **`9466990`** — their commit.
The row on `main` is this session's and is correct, and `e249235` records why.
This is exactly the commons-lease hazard `AGENTS.md` describes: the board is a file
every session touches for ten seconds, which is why it is meant to be *leased*
rather than claimed. No lease was taken here, by either session.

Also: the brief said *"this repo has no GitHub issues at all — the map will be
issue #1."* True when written, false eight hours later — `product-device-pass`
filed #2–#11 in between. The map is **#12**. The content partition the two sessions
agreed held perfectly; only the numbering assumption rotted.

## 🧪 Tests

**No test layer applies, and this is not a layer being skipped.**

This session produced **no Kotlin, TypeScript, Gradle, or `firestore.rules`
change** — its entire output is GitHub issues plus three Markdown files. The
repo's four layers (JVM unit, instrumented, `firestore-tests/` security rules, and
the Cloud Functions build) have nothing to assert against it. Running any of them
would have proven only that the previous session's code still compiles.

Verification was structural instead, and it did run:

- **The graph was queried back out of GitHub after wiring**, not assumed from the
  mutations succeeding: all 20 tickets confirmed as children of #12, every
  blocked-by edge present, and the frontier query returning exactly the five
  intended tickets — no cycles, no orphans, nothing accidentally unblocked.
- Every ticket carries exactly one `wayfinder:<type>` label.

**Singletons:** none taken. No Gradle daemon, neither AVD, and the live
`goalpilot-56e30` project was never touched.

## Files

- **New:** `CHANGELOG/2026-08-08/product-model-map.md`
- **Edited:** `TODO/TODO_FUTURE/ProductModel.TODO.future.md` (the `C14` entry, the
  graduation banner and ticket table, the overturned charting order),
  `SESSIONS.md`, `sessions/product-model-map.md`, `CHANGELOG/CHANGELOG_README.md`
- **Untouched:** every file under `app/`, `functions/`, `firestore-tests/`,
  `scripts/`, and `firestore.rules`
