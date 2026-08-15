# `c21-offline-story` — 2026-08-15

**Session:** `c21-offline-story` · branch `feat/goalpilot-implementation` · mode **`AUTO MODE`**
(declared by Ido in the opening message).
**Invocation:** `/wayfinder 12` — work through the v0.3 product-model map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12), no ticket named, so the
session picks the next decision.

---

## 🎯 What this session did

**The frontier was empty**, so there was nothing to claim. All 20 children of `#12` were closed
(`C20` #42 closed 2026-08-14 16:55 UTC and was the last). Under *Work through the map* step 5 that
makes graduating fog the unit of work, so this session **created the ticket it then resolved**:

> ## 🎟️ **[#43 · `C21` · Does v0.3 owe an offline story, now that only other people's numbers can be stale?](https://github.com/idomarhaim/Android_Final_Project/issues/43)** — claimed, resolved, closed

Graduated from the map's first **Not yet specified** bullet. Wired as a sub-issue of `#12` via
`addSubIssue`, labelled `wayfinder:grilling`, assigned to `idomarhaim` **before any work**, which is
the claim.

## 🧭 The resolution — the ticket's own question was the false premise

**v0.3 owes no offline story; it owes an *as-of* stamp.** `A6`'s two halves (*must the app say it is
offline*, *must a cached number look different*) both presume staleness is a property of the
**connection**. It is a property of the **data**: a leaderboard fetched forty minutes ago over
perfect Wi-Fi is exactly as old as one served from cache with the radio off. An affordance keyed to
the radio therefore over-fires and under-fires at once. This is
[`C9b` #26](https://github.com/idomarhaim/Android_Final_Project/issues/26)'s *`SILENT` and
`PROVISIONAL` differ by **visibility, not confidence*** in a new place — the fourth time on this map
the answer has sat outside its own ticket's option set.

Four spec lines and one deletion:

1. **No global connectivity banner and no per-number "cached" styling.** After
   [`C20` #42](https://github.com/idomarhaim/Android_Final_Project/issues/42) the owner's numbers are
   projections of facts the owner wrote, and `FirebaseModule.kt:22` confirms Firestore's persistent
   cache is on by default and never overridden (`grep FirebaseFirestoreSettings app/src/main/java`
   → no hits). So offline, the owner's whole app is **correct**, and a banner over it is a larger
   claim than the facts support — `C12`'s *smallest true sentence* rule failing in the opposite
   direction from the footnote it was originally cut for.
2. **Which surfaces can be stale is a grep, not a judgement.** `C20`'s ownership-boundary rule run
   over `firestore.rules` returns `publicProfiles`, `challenges/*/participants` and `shares` and
   nothing else, so the affordance lands on **exactly two screens** (`feature/social`,
   `feature/challenges`). The two rulings are one ruling from opposite ends: *a number needs a server
   writer iff it crosses the boundary* ⇔ *it needs an as-of stamp iff it crosses the boundary*.
3. **The stamp is unconditional and nearly free.** Neither projection can draw one today —
   `PublicProfileDto` (`Dtos.kt:77`) has **no timestamp at all**; `ChallengeParticipantDto`
   (`Dtos.kt:118`) has only `joinedAt`, which says nothing about when the score moved. Both gain a
   server `updatedAt` written by **`C20`'s projection function on the write that already sets the
   number**: one field, no new trigger, no new read path. `shares.createdAt` already suffices — a
   share is an immutable event.
4. **One case is genuinely connection-dependent, and it is an empty state rather than a banner.** A
   cross-boundary collection never fetched on this device returns an *empty* snapshot, so a
   first-run-offline Social tab asserts *"you have no friends"*. `C12`'s *a card with nothing to say
   hides itself* does **not** cover it — there the app knows, here it does not. Discriminator:
   `metadata.isFromCache && isEmpty`, **used nowhere in the codebase today**.
5. **`ConnectivityMonitor` is deleted, not repurposed.** Its own KDoc gives its only reason: `setDone`
   was a server-only transaction, so offline the optimistic tick had to be taken back after a measured
   **7.9 s**. `C20` removes the transaction; its one consumer (`GoalDetailViewModel.kt:168`) is the
   pre-check `C20` already deleted; and §4 needs a snapshot property, not the OS.

**Scope answered too:** this is a product decision on the map, not defect work — `C12` already made
*how a surface discloses the honesty of a number* a product rule, and §3 changes a **stored schema**
`C20`'s function must write, which no defect ticket can carry.

**No picker was raised.** Every question resolved against a closed ticket (`C20`, `C12`, `C9b`,
`C15`), `firestore.rules`, or the code, so the answers are derived and logged per the
derivable-decision rule — the precedent `c20-derived-state` and `c11b-output-formats` set. All of it
is Ido's to overturn; the one place taste could plausibly land differently is §3's *always* versus a
staleness threshold, rejected because a threshold is a second number nobody can source.

## 🧹 Map maintenance — one stale fog bullet, cleared as a separate act

`c20-derived-state` left the `A7`/dashboard patch as *"the next session's cheapest lead"*, suspecting
it was **un-owned rather than un-sharp**. It was un-owned, and the record proves it:
[`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31) closed **last** of the four
tickets that narrowed `A7` (2026-08-12 17:46 UTC, after `C10` 14:23, `C9a` 15:37 and `C9b` 19:23 on
08-10) and its §5 *Arrangement* rules `A7` a **false fork** outright. The bullet was simply never
trimmed when it closed. Rewritten down to its one true residue — *whether a goal card on Home is
tappable to complete* — rather than deleted, since no ticket has spoken to that.

## 🐛 A defect found on the way, filed as a spec line rather than fixed

`GoalDetailViewModel.kt:238` defines `OFFLINE_MESSAGE = "You're offline — task changes need a
connection"` as a **hardcoded English literal in Kotlin**; `grep -i offline
app/src/main/res/values/strings.xml` returns nothing. Under `C15` every word the app writes switches
with the language picker, so it is untranslatable where it stands. Deleted here anyway with
`ConnectivityMonitor` — but **kept in the record as evidence the class exists**: the build session
owes a sweep of `feature/` for user-facing literals outside `strings.xml`.

## 📐 `#12` commons discipline — clean run

Body fetched → patch built → **re-fetched and `cmp`-compared immediately before the write
(unchanged, no race)** → written with `--input map_patch.json` (110 KB; `-f body=` still cannot carry
it) → re-fetched and verified. **27 → 28 decisions, 4 → 3 fog bullets**, byte delta **+1**, which is
the trailing newline GitHub appends — exactly as `c6-log-progress`, `c15b-stored-ai-text`,
`c11b-output-formats` and `c20-derived-state` each recorded.

## 🧪 Tests

**No test layer applies.** This session wrote no code — per the map's standing *plan, don't do*
preference, `C21` ships spec lines and a deletion, both of which land in `docs/PRODUCT_v0.3.md` when
the spec session runs. The layers this project has (server unit, endpoints, client component, UI
E2E) are all untouched: no file under `app/src/`, `functions/` or `firestore-tests/` was modified.
Stating that explicitly rather than skipping the section.

## 🧭 Concurrency

`c20-derived-state` committed `c199185` at **13:55:07**, seventy seconds before this session's claim
row was written, so it was **live** despite the ✅ release note already on the board. Working sets are
disjoint by content but **share the `SESSIONS.md` file**, which a pathspec commit cannot separate.
`git diff -- SESSIONS.md` was run in its own tool call before committing, and any hunk of `c20`'s
riding along is named in the commit message rather than subtracted.

`C:\Dev\JARVIS` also has a live sibling — `sibling-wait-banner`, `50c1d79` at **13:58:10**, whose own
subject records claiming `rules/memory-promotion.md` while that board's Active-claims table read
**empty** at the same moment. Precondition 5's *an absent row is not proof the session is finished*,
observed again.

## 📥 KB candidates

**One filed, none drained.**
`kb-candidates/2026-08-15-c21-offline-story.md` — *Key a disclosure to the variable that moves the
fact, not the one that co-occurs with it.* 🟢 on its own merits (new page, supersedes nothing),
**held** on the JARVIS liveness fact above, since draining is a cross-repo write into a board that
session is actively editing. The candidate names its own bundle check **and its width limit**: three
near-neighbour pages were opened and read; three more with close titles were not, and the ingesting
session must open them first — the width failure `c20-derived-state` recorded earlier today.

**Five other candidate files remain in `kb-candidates/`, all pre-existing and none this session's.**
Every surviving entry in them is ⛔ always-ask in both modes (`rules/`-destined, or superseding a
standing KB claim), so `AUTO MODE` drains none of them.

## 📋 What is left of `#12`

- **0 open tickets** (`#43` closed) · **28 decisions** · **3 fog bullets**.
- The destination artifact **`docs/PRODUCT_v0.3.md` still does not exist**, and the brief for writing
  it is committed and ready at `sessions/product-v03-spec.md` (`status: ready`). `C21`'s four spec
  lines go into it; nothing about them needs re-deciding.
