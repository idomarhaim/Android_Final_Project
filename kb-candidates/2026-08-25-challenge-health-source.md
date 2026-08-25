# KB candidates — `challenge-health-source`, 2026-08-25

Each entry stands alone; nothing here leans on a transcript.

---

## 1 · A write path that overwrites the caller's timestamp makes a whole feature class impossible, and never fails

**Claim.** `ProgressRepositoryImpl.logProgress` built its DTO with
`createdAt = System.currentTimeMillis()` **unconditionally**, silently discarding
`entry.createdAtEpochMillis`. Every Health Connect reading was therefore stamped with *the
moment the sync ran*, not the day it was walked — so Monday's sync filed the whole weekend
as Monday.

**Why it survived for months.** *Nothing ever failed.* A goal's total is a plain sum over
its entries and does not care when any of them is dated, so every visible number was right.
The defect only becomes observable when something **filters by time** — here
`ScoringWindow.includes()` — and by then it presents as *"this feature returns zero"*
rather than as *"that write path is lossy"*.

**Two tells worth generalising, both present here.**
- **A comment describing a state the code cannot reach.** `ScoringWindow`'s KDoc already
  said *"a backfilled entry with an old timestamp correctly changes nothing"* — describing
  behaviour no entry could exhibit, because the repository overwrote every timestamp. When
  prose and code disagree about what is *possible*, the prose is often the design and the
  code is the bug.
- **A parameter nothing supplies.** `ProgressEntry.createdAtEpochMillis` existed, defaulted
  to `0L`, and had no caller that set it — which reads as *unused* and is really *unusable*.

**The fix shape:** honour the caller and let zero mean *now*, so every existing call site is
unchanged and the capability appears without a migration.

- **Destination** `kb/dev/` — a new page, or a section of `firestore-write-semantics.md`
- **Anchors** `ProgressRepositoryImpl.logProgress` · `SyncHealthDataUseCase` (the
  `createdAtEpochMillis` comment) · `CHANGELOG/2026-08-25/challenge-health-source.md` §B
- **Supersedes** nothing
- **Status** ready

---

## 2 · A rule duplicated in two languages needs a fixture pin *most* when the two are written in different shapes

**Claim.** `scoringWindow` exists in Kotlin and TypeScript, is the rule that decides **which
entries a challenge counts**, and until 2026-08-25 the repo's shared fixture pinned
**nothing** about it — while pinning the points and score arithmetic in detail.

**The selection error is the finding.** The fixture was built for the rules that *looked*
duplicated: same inputs, same outputs, same shape, obviously mirrored. `scoringWindow` was
skipped precisely because it looked incidental — a two-line bound calculation. But it is the
**likeliest** of the three to drift, because the two implementations are written in
different *shapes* (an `if/else` against a nested ternary), so an edit to one has no
mechanical or visual reason to reach the other. **Structural dissimilarity is a risk
multiplier, not a detail** — and it is invisible from either side alone.

**Corollary that paid off immediately:** after adding the cases, reverting the Kotlin rule
was tried on purpose and the fixture went red on the right case. *An instrument that has not
been shown to fail is not evidence* — and the mutation costs one `git checkout`.

**Second corollary, from the same session:** when a pair of tests covers a
*derive-or-preserve* branch, check which half could pass against a **no-op**. Here the
`RESET` case asserted `score == 0`, which is also what the projection produces from a deleted
fact — so only the `RELABEL` case (a number that *survives*) discriminated.

- **Destination** `kb/dev/` — wherever the `C20` / spec §5.2 shared-fixture material lives
- **Anchors** `shared-fixtures/derived-state.json` (`windowCases` and the `$comment` added
  with them) · `DerivedStateFixtureTest` · `functions/test/projection.test.mjs`
- **Supersedes** nothing
- **Status** ready

---

## 3 · "Do X directly" is often a UI complaint wearing an architecture costume — and the capability check settles it

**Claim.** Ido asked for challenge data *"pulled straight from Health Connect and not only
through a personal GOAL"*. Read as architecture, it demanded a second data pipe and
contradicted a decision shipped hours earlier. Read as a UI complaint — *"I should not have
to author a goal to join a steps race"* — it was fully grantable with no duplication.

**What told the two apart was not judgement, it was a capability check.** Health Connect is
an **on-device** API and the scoring Function runs **in the cloud**: the server cannot read
it at any price. So a literally-direct pipe would require the app to write the same readings
into Firestore a *second* time, under a second summer that can disagree with the first. That
is not a preference to weigh against the user's — it is a fact that removes the option, and
it converts *"the spec says no"* (which a user may simply overrule) into *"here is what
would actually happen"* (which they can act on).

**The general move:** before treating a product instruction as a conflict with a committed
decision, ask **which half of the ask is about capability**. Grant the experience where it
was asked for; keep the mechanism that the capability dictates; and say plainly which is
which, because the user is entitled to overrule the first and cannot overrule the second.

**And the residual belongs in the UI, not in a comment.** The auto-provisioned goal is
visible on the Goals screen, so the row says *"It sets up your 'Weekly steps' goal to hold
them"* **before** it happens — the mechanism is disclosed at the point of choice rather than
discovered afterwards.

- **Destination** `kb/` — alongside `rules/question-axis-naming.md`'s worked examples; **not
  `rules/`** and nothing here proposes a rule change
- **Anchors** `LinkChallengeToHealthUseCase` (header) ·
  `CHANGELOG/2026-08-25/challenge-health-source.md` §A
- **Supersedes** nothing
- **Status** ready

---

## 4 · A default that protects one case can be the sole cause of another case's impossibility

**Claim.** §6 opened a challenge's scoring window at `max(joinedAt, startAt)` to stop
*"joining with a year-old goal importing a year of history nobody raced for"*. That reason
is sound and the rule it produced still made **retroactive challenges score zero for
everybody** — accepting an invitation to last week's race gave a lower bound of *today*,
past the challenge's own end.

**The resolution is the transferable part.** The bound was doing its work in exactly one
situation: **when there is no start date to bound with**. Wherever the owner had set one,
`startAt` already excluded everything before the race and `joinedAt` contributed nothing but
the bug. So the rule splits on *the condition under which the protection is actually load-
bearing*, rather than being weakened, negotiated, or made configurable.

**How to find it:** ask **what would still be true if this guard were removed** — under each
branch separately, not in general. A guard that is redundant on one branch and essential on
the other is a guard that belongs on that branch alone.

**And name the consequence you are choosing**, because there is one: joining a dated
challenge late now credits the whole window. That is intended (*"who walked most in August"*
means August), and it is asserted in a test so nobody quietly restores the old behaviour as a
"fix".

- **Destination** `kb/dev/` — a short page on scoping guards, or an entry wherever GoalPilot's
  §6 scoring material lives
- **Anchors** `ScoringWindow`'s KDoc · `Challenge.scoringWindowFor` · `derived.ts#scoringWindow`
- **Supersedes** nothing; it narrows §6's rule and says so in place
- **Status** ready

---

## 5 · One render frame can carry three defects, and none of them are test-visible

**Claim.** The first frame of `ChallengeHealthSourceRenderPass` showed, simultaneously: a
sheet title reading *"Score this from a goal"* directly above a Health Connect row that is
**not** a goal; an intro paragraph reciting a scoring rule that had been **replaced that same
day**, in both of its halves; and *"Steps · steps"* from a naive label-and-unit join.

**Why all three were invisible to the test layer.** Every one is a **relationship between
two things that are individually correct** — a true title above a new option, a true sentence
about an old rule, a true label beside a true unit. Assertions check strings; frames check
*adjacency*.

**The pattern worth naming: when a feature is ADDED to an existing surface, the surface's own
prose becomes a claim about a world that no longer exists.** The new thing is reviewed
carefully because it is new; the sentence above it is not reviewed at all because it did not
change. That is the same failure as a stale comment, at UI scale, and the only instrument
that catches it is a picture of the whole surface.

**Cheap prophylactic:** when adding an option to a surface, re-read the surface's **title and
intro** as if the new option were the only thing there.

- **Destination** `kb/dev/look-at-your-own-output.md` — its subject is exactly verification
  that fails silently
- **Anchors** `docs/render-passes/2026-08-25-challenge-health-source/` ·
  `GoalLinkState.windowNote` (which exists because of this)
- **Supersedes** nothing
- **Status** ready
