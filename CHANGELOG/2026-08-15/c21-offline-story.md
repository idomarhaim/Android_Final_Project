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

## ⛔ Committed, not pushed — precondition 5, and it is stated dated

`d187649` is committed and **held**. `git log @{u}..HEAD` carries a **foreign** commit —
`e416d61`, `product-v03-spec`, *Claim product-v03-spec: write docs/PRODUCT_v0.3.md…*, 14:02:42 —
touching `SESSIONS.md` and `sessions/product-v03-spec.md`. Both paths sit under that session's
**live row in Active claims**, and it is mid-unit: it has claimed `docs/PRODUCT_v0.3.md` and has not
yet written it. Precondition 5 is unambiguous there — *a foreign commit whose paths sit under a live
row → stop and ask* — and in `AUTO MODE` naming it in the reply is not a substitute, because a reply
is a disclosure and un-publishing needs a force-push, which is always-ask.

The other five preconditions pass: no tests apply and the changelog is written (1); the range holds
**no deletions and no renames**, no secrets, no binaries, nothing outside scope (2); the push would
be a plain fast-forward of one branch (3); `git fetch` shows the branch has not moved and
`HEAD..@{u}` is empty (4); this is a solo repo with no PR workflow (6).

**Dated, because the hold is not self-maintaining.** `git push` is branch-scoped, so
`product-v03-spec` publishing its own work will carry `d187649` up on its schedule with no gate of
mine involved. `Observed:` re-checked at **2026-08-15 14:10:12 +0300** — remote head is still
`c199185`, so `d187649` and `e416d61` are **both still unpublished as of that check**. If it is
already up by the time this is read, that is that mechanism, not a change of mind.

## 📋 What is left of `#12`

- **0 open tickets** (`#43` closed) · **28 decisions** · **3 fog bullets**.
- The destination artifact **`docs/PRODUCT_v0.3.md` still does not exist**, and the brief for writing
  it is committed and ready at `sessions/product-v03-spec.md` (`status: ready`). `C21`'s four spec
  lines go into it; nothing about them needs re-deciding.

---

## 📥 Addendum 19:0x — the KB drain happened after all, and it was not this session's candidate

Ido answered the question that had `kb-candidates/2026-08-12-c12-charts-presentation.md` parked for
**three days**, and answered it with an option that was on the menu but not the recommendation:
**both — a KB page *plus* a one-line `rules/` pointer**, rather than either alone.

**What landed** (JARVIS `ed6a69e`, pushed): `kb/dev/look-at-your-own-output.md` (entries 1 + 4) and
`kb/dev/faking-depth-in-2d.md` (entries 2 + 3), two index rows, one journal entry.
`Check-KbLinks` **CLEAN at 73 pages**. JARVIS's board read `_none_ active`, was claimed before the
first write there and released inside the same commit. Full account:
`C:\Dev\JARVIS\CHANGELOG\2026-08-15\c21-offline-story.md`.

**Why two pages rather than one or four.** Entries 1 and 4 are a rule and its own counter-example —
*the agent must look* and *the thing it looks through can lie* — so splitting them would have shipped
the rule on one page and its refutation on another. Entries 2 and 3 share a different shape
(*the appearance of the solution without its mechanism*) and both resist parameter tuning for the
same reason.

**The bundle check no longer matched, for the third day running — and that is the reusable part.**
The candidate's check was written 2026-08-12 and named overlap with pages that existed then. The two
**nearest** pages — `describing-is-not-exhibiting.md` and `elevation-is-not-a-fill.md` — were created
**twenty minutes before this ingest** by `c22-measure-proposal`. Both were read in full before
writing, and both turned out genuinely adjacent rather than duplicative, so the outcome was two new
pages **plus four cross-links that could not have been written yesterday**. That 18:56 journal entry
had predicted the hole exactly: it recorded that
`render and look|invisible in the source|acceptance criterion is visual` returned **nothing**.

**Not written: the `rules/` clause.** It alters the interaction protocol, so the 🎬 walkthrough rule
owns it. Offered, not shipped.

**Candidate file deleted, without asking, and the rule that permits it:** every entry is now
promoted (entry 5 on 2026-08-13, entries 1–4 today), which is `derivable-decision.md` §1's one
carve-out to the always-ask-before-deleting rule. It rides its own commit here rather than the
pages' commit in JARVIS — two repos, which is why the journal entry is the candidate↔page tie.

**Seven candidate files remain in this repo**, including this session's own, which is still filed and
still undrained.

## 🧭 Concurrency (addendum)

`c24-settings-surface` is live here on `#46`, with `docs/PRODUCT_v0.3.md`, its prototype folder and
its own changelog and candidate file dirty in the tree. All disjoint from this addendum's paths, and
none of them staged or committed by this session.

---

## 📥 Addendum 2 — 19:3x, `ingest first`

Ido answered the 🎬 offer with **`ingest first`** — the deferral. Four duties, all discharged; the
full account is in `C:\Dev\JARVIS\CHANGELOG\2026-08-15\c21-offline-story.md` (2nd visit), JARVIS
`9e52c2f`.

**Four entries drained**, from four of this repo's candidate files:
`2026-08-15-c21-offline-story.md` #1 → `kb/dev/stale-is-a-data-property.md` *(new)* ·
`2026-08-15-product-v03-spec.md` #1 → `kb/dev/look-at-your-own-output.md` **§5** *(new section)* ·
`2026-08-15-session-identity-tabs.md` #1 → `kb/dev/claude-code-surfaces.md` *(limit subsection)* ·
`2026-08-15-c23-goal-category.md` #2 → `kb/dev/display-attribute-is-not-an-identity.md` *(new)*.
`Check-KbLinks` **CLEAN at 75 pages**.

**`product-v03-spec` entry 1's gate lifted mid-session, by this session's own earlier commit** — it
was blocked on a page `ed6a69e` had created ninety minutes before. Its substance is that the parent
claim was **scoped too narrowly**: the discriminator is not *rendering* but whether the artefact you
produced is the artefact that will be **consumed**.

**Candidate files here:** `2026-08-15-product-v03-spec.md` and `2026-08-15-session-identity-tabs.md`
**fully drained → deleted**; `2026-08-15-c23-goal-category.md` **rewritten down to its two
`rules/` survivors**; `2026-08-15-c21-offline-story.md` **rewritten down to a new parked entry 2** —
the `rules/` half — with `Status: awaiting 🎬 — "ingest first" chosen 2026-08-15`. **Six files
remain**, one belonging to the live `c24-settings-surface`.

**⛔ JARVIS is committed but NOT pushed.** `git log @{u}..HEAD` there carries a foreign commit,
`b62618a` from `session-title-equals-label`, whose paths sit under its **live** Active row — and that
session is mid-unit, blocked on singletons this commit just released for it. Precondition 5 stops the
push; `AUTO MODE` does not lift it, because un-publishing needs a force-push.

---

## 📥 Addendum 3 — 19:3x, the second `ingest first`: **nothing left to drain, and why that is the finding**

Ido answered the re-made 🎬 offer with `ingest first` a second time. Duty 1 was run again and
**returned empty**, which is a result rather than a non-answer. Every entry now in this session's
reach is ⛔:

| File | State |
|---|---|
| `2026-08-13-c15b-stored-ai-text.md` #2 | ⛔ `rules/` |
| `2026-08-13-c2-task-type.md` #2 | ⛔ `rules/` |
| `2026-08-15-c23-goal-category.md` #1, #3 | ⛔ `rules/` |
| `2026-08-15-c21-offline-story.md` #2 | ⛔ `awaiting 🎬` — this session's own, parked by design |
| `2026-08-15-c24-settings-surface.md` | 🟢 ×2, but **claimed by a live session** — not this session's to take |
| `2026-08-13-session-titles.md` | ✅ **fully drained → deleted in this commit** |

**One file retired.** `2026-08-13-session-titles.md` held one surviving entry (#4), and its `Status`
already read *drained 2026-08-13 by `liveness-from-transcript`*. It had been sitting fully drained
for **two days** because the session that drained it worked in the other repo and never came back to
delete the file here — the exact failure the *trigger is the condition, not the skill* clause exists
for. Deleted without asking, per `derivable-decision.md` §1.

## 🔭 The finding: four parked entries are **one** walkthrough, not four

Reading the remaining `rules/` entries' **destinations** rather than their statuses shows they
converge on a single file — `C:\Dev\JARVIS\rules\question-axis-naming.md`:

- `c15b` #2 — *a hand-back repeated on the same subject means the **premise** is false, not the
  **form*** → a clause on the **tell table**.
- `c2-task-type` #2 — *the fork check must run against the **code**, not the ticket's own statement
  of the fork* → a clause on **The widening**.
- `c23` #1 — *a false fork is found in the duplicated **derivation**, not the duplicated field* →
  extends the fork-check.
- `c23` #3 — *the comprehension complaint fired three times in one day, and the third kills the
  standing diagnosis* → same file.

Each was filed independently as *"⛔ always-ask, `rules/`"* and parked on its own, the earliest on
**2026-08-13**. Nothing was wrong with any of those decisions; what nobody did was read the four
`Destination` lines side by side. They are **one amendment to one rule**, and therefore **one
walkthrough** — which is a materially different ask from four.

This is why repeating `ingest first` cannot move the backlog further: what remains does not need
ingesting, it needs **one decision**. Stated as a finding rather than acted on, because `rules/` is
always-ask in both modes and grouping four parked candidates into one proposal is still a proposal.

---

## ✅ Addendum 4 — `waive`, and this session is finished

Ido answered the re-made 🎬 offer with **`waive`**. The judgment half is his word; the **mechanical**
half was run alone and is recorded in `C:\Dev\JARVIS\CHANGELOG\2026-08-15\c21-offline-story.md`
(3rd visit), shipped as JARVIS **`7a5b3f9`** — *rules: verify by re-running whatever will consume
your output*, one bullet under 🧪 *Testing discipline*, with `Sync-AgentAssets.ps1` run.

**The pass was not a formality.** It found that the draft's *"an import path"* example had **no
recorded instance behind it** — `grep -n "compiler\|typecheck\|Gradle"` across the two relevant KB
pages returns nothing — and that it turned the bullet into a **tax on every code edit**, since the
compiler already recomputes import paths. Deleted, and replaced with an explicit silence clause. All
seven recorded instances still fire; the *stays-silent* half is the one that failed, which is the
half the rule says cannot be faked.

**What it could not test, because `waive` skipped the run:** the corpus is mine, written ninety
minutes earlier, so it cannot attack *"a rule you recite from memory of the request"*. A
fresh-context agent would close that gap; **not used** — it is a subagent, and `waive` does not grant
the 🧩 gate.

**`kb-candidates/2026-08-15-c21-offline-story.md` is deleted in this commit.** Entry 1 drained at
19:2x, entry 2 — the parked `rules/` half — shipped as `7a5b3f9`, so **every entry is promoted** and
`derivable-decision.md` §1's carve-out applies: deleted without asking.

**Nothing of this session is now dirty, held, parked or unpushed in either repo.** Four candidate
files remain here, none of them this session's:
`2026-08-13-c15b-stored-ai-text.md`, `2026-08-13-c2-task-type.md`, `2026-08-15-c23-goal-category.md`
(all ⛔ `rules/`, and **four of their entries converge on one file**, `rules/question-axis-naming.md`
— one walkthrough, not four, per Addendum 3) and `2026-08-15-c24-settings-surface.md`.
