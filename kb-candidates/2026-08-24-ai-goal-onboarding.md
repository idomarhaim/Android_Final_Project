# KB candidates — `ai-goal-onboarding`, 2026-08-24

Written per the memory-promotion rule. Each entry stands alone: no transcript is a source.

---

## 1 · A default value cannot record whether the user chose it

**Claim.** When code must distinguish *"the user picked this"* from *"the user left the
default"*, a `touched` flag is not defensive bookkeeping — it is the only representation of the
distinction, because the two states are the **same value**. Any branch that reads the value to
decide whether to overwrite it is wrong on exactly the inputs where the default and the choice
coincide, which is the commonest case rather than an edge one.

**Why.** `GoalForm.category` defaults to `HEALTH`. A silent AI filing that overwrote the
category *"only when the user had not chosen one"* would have had to test `category == HEALTH`,
which is also true of every user who deliberately picked `HEALTH` — so the model would win over
the user on precisely the goals where they agreed. The same shape appeared one field over and in
the opposite direction: `lifeAreaIds.isEmpty()` is both *"nobody said"* and *"unfiled,
deliberately"* (the *None* chip), and `docs/PRODUCT_v0.3.md` §1.2 makes the second a real choice.
**Rejected:** a sentinel default (`GoalCategory.UNSET`) — it widens an enum every screen reads for
a state only one form is ever in, which is the same cost `Measure.of` already declined to pay.
Related in kind to `#66`'s *"an unmeasured goal's `progressPercent` is `0` over a target nobody
set"*: a value that is the absence of an answer being read as an answer.

**Destination.** `kb/dev/` — a page on absence-vs-default in form and wire state, sibling to the
existing *enum-and-label* material.

**Anchors.** `docs/PRODUCT_v0.3.md` §0.7, §1.2 · `AddEditGoalViewModel.fileSilently` ·
`GoalForm.categoryTouched` / `lifingTouched`.

**Supersedes.** none.

**Status.** open.

---

## 2 · An enum constant's name is not the wire's name, and the mismatch is always-false rather than a crash

**Claim.** Comparing an incoming wire string against `SomeEnum.CONSTANT.name` is a **silent
always-false** whenever the app's vocabulary and the protocol's differ — it compiles, it reads
correctly under review, and it fails in whichever direction the `else` branch happens to go.
Name the wire token as its own constant, next to the reason it differs, and assert the
**positive** case in a test: a test that only checks *"garbage reads as the fallback"* passes
against code where **everything** reads as the fallback.

**Why.** `PlanStepKind.MILESTONE` is what the app calls it; §3.3 B's wire label is
`STATE_YOU_REACH`. `name.equals(MILESTONE.name, ignoreCase = true)` was therefore never true, so
every milestone would have arrived as `WORK` — priced, slotted into the day, and treated as work
the user does, with nothing crashing and nothing logging. It survived writing, reading back, and
a compile; it was caught only because the test asserted `fromName("state_you_reach") ==
MILESTONE` beside the two negative cases. **Rejected:** deriving the wire name from the enum (a
`@SerialName`-style annotation) — this app has no serialization framework on the domain models,
and adding one for one field trades a named constant for a dependency. Same family as
`kb/dev/look-at-your-own-output.md` §*a transformation you are not running*: the failure is in a
mapping nothing re-checks.

**Destination.** `kb/dev/` — append to the existing *enum and label* page if one covers wire
mapping; otherwise a short page of its own.

**Anchors.** `functions/src/plan.ts` `LABELS` · `domain/model/GoalPlan.kt`
`PlanStepKind.WIRE_MILESTONE` · `app/src/test/…/GoalPlanTest.kt`.

**Supersedes.** none.

**Status.** open.

---

## 3 · A model may author an offset; it must not author a date

**Claim.** When an LLM places work in time, have it answer in a **relative offset** resolved by
the app against its own clock, never an absolute date. The usual argument (models are bad at
numbers) is the weaker half. The stronger one is that any proposal a user can **leave and come
back to** carries dates that were true when it was *drawn*, and nothing on screen distinguishes a
stale date from a fresh one — so the artefact rots silently in the one direction nobody checks.

**Why.** `docs/PRODUCT_v0.3.md` §3.7 specifies a draft that *"persists exactly as left … no
expiry"*. A plan drawn Monday and accepted Friday would have scheduled Monday's dates. The offset
form also gives a cheap unit check the date form does not: a ceiling of 365 days catches a model
answering `20260901` — a date wearing an integer's clothes — which no range check on a date
string could. Generalises §3.3 E's `targetSource`, where the model names *which arithmetic the
app runs* and never the number. **Rejected:** sending the model today's date so it can compute
absolutes — that keeps the arithmetic on the wrong side of the wire *and* the staleness.

**Destination.** `kb/dev/` — the page on what an LLM may author, alongside the
*judges-vs-computes* material.

**Anchors.** `docs/PRODUCT_v0.3.md` §0.5, §3.3 B, §3.7 · `functions/src/plan.ts`
`MAX_DAY_OFFSET` · `PlanStep.occurrenceOn`.

**Supersedes.** none.

**Status.** open.

---

## 4 · A feature that schedules should write the app's own *when*, never a second copy in the calendar

**Claim.** When an app already mirrors its internal schedule into an external calendar, a new
feature that "puts things on the calendar" must write **only** the internal occurrence and let
the existing mirror carry it. Writing calendar events as well produces two writers of one fact —
the failure `docs/PRODUCT_v0.3.md` §0.3 calls the map's most-repeated finding — and the second
writer is the one that has no test, no retry and no consent story.

**Why.** `ApplyGoalPlanUseCase` writes a `Task` with an `Occurrence` and mentions Google exactly
once, to ask for a `MANUAL` sync so the user does not wait for the next foreground. Both
surfaces then follow for free: `TaskSchedule.occurrencesIn`'s fourth source is *"the anchor
itself, when the task has neither a rule nor any stored document"*, which is what the app's
calendar reads **and** what `SyncCalendarUseCase.entriesIn` walks. The test that proves it asks
`TaskSchedule` — the same type the sync uses — rather than asserting on the use case's own
output, which would have proved only that it did what it did. Second half of the same claim: the
sync's **outcome** is discarded, because `NeedsConsent` is §2.6's ordinary state for a user who
never opted in, and surfacing it as a failure of the plan would tell them their tasks were not
created when they were.

**Destination.** `kb/dev/` — the mirroring/derived-state page; strong candidate to be cross-linked
from `docs/ARCHITECTURE.md`'s calendar section.

**Anchors.** `docs/PRODUCT_v0.3.md` §0.3, §2.6, §2.7 · `ApplyGoalPlanUseCase` ·
`GoalPlanTest > a written step is visible to the same schedule the calendar and the sync read`.

**Supersedes.** none.

**Status.** open.

---

## 5 · A guard test can be blocked by a board claim, and that is the guard working

**Claim.** A repo-currency guard (*"every callable the backend exports is named in the
architecture doc"*) will eventually fire in a session that **may not edit the file it points
at**, because the doc is claimed by a sibling. The resolution is not to edit anyway and not to
weaken the guard: it is to leave the **exact replacement text** on the board under the holder's
row, report the red test as `blocked` with the holder named, and offer to make the edit on one
word. The guard has then done its whole job — it surfaced a doc that would otherwise have gone
quietly wrong.

**Why.** `DocsCurrencyTest` went red the moment `fileGoal` and `planGoal` were exported;
`docs/ARCHITECTURE.md` is `docs-repair`'s. §5.3(c)'s liveness check could not resolve their
transcript — the `file-history` grep matched **9** sessions, which is exactly the false positive
the rule warns about — and unresolved counts as live. The cost of getting this wrong is
asymmetric: editing their file risks their work, while a red guard costs one line in a status
block. **Rejected:** an allow-list or a `@Ignore` on the guard — that converts a one-line paste
into a permanently weaker check, and the next callable would land undocumented and silent.

**Destination.** `kb/dev/flows/` — the board / claim flow page, as the *what to do when a guard
lands inside someone else's claim* case.

**Anchors.** `SESSIONS.md` (the note under `docs-repair`'s row) ·
`app/src/test/…/docs/DocsCurrencyTest.kt` · `agent-topology-and-model-routing.md` §5.3(c).

**Supersedes.** none.

**Status.** open.
