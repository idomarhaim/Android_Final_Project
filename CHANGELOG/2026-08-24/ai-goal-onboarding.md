# `ai-goal-onboarding` — 2026-08-24

`C8` / [`#24`](https://github.com/idomarhaim/Android_Final_Project/issues/24) built, plus a
sixth callable that had no ticket: **a new goal now files itself under a life area from its
title alone, and then offers a work plan you approve step by step — and what you approve
becomes dated tasks on the goal, on the app's calendar, and in the GOALPILOT calendar inside
Google Calendar.** Started from Ido's report, `AUTO MODE`.

---

## 0 · What Ido reported, and what was actually missing

Verbatim, 2026-08-24 (Hebrew, translated):

> *"A critical problem I need you to handle. When I create a new GOAL, the app's AI feature
> does not do the following things I need it to do: **(1)** assign it to the relevant life
> area — on the basis of the GOAL's name alone, without my defining what the life area is;
> **(2)** give me a suggestion for a work plan with tasks and timings, and if I approve them
> then they enter the GOAL and are placed on the app's calendar, and if I am synced to Google
> Calendar then in the GOALPILOT calendar inside Google Calendar."*

Both were true, and the shape of the gap is worth recording because it is not what *"the AI
feature does not work"* usually means.

**Nothing was broken. Goal creation had no AI in it at all.** `AddEditGoalViewModel` took a
`GoalRepository`, a `LifeAreaRepository` and a `SavedStateHandle`, and called no model on any
path. The app has had four AI callables since `#54` — `getRecommendations`, `classifyTask`,
`scoreTask`, `proposeMeasure` — and **not one of them fires when a goal is created**:

| callable | what it answers | when it fires |
|---|---|---|
| `classifyTask` | which goal / life area a **task** belongs to | quick-add, Google Tasks import |
| `scoreTask` | how demanding a **task** is, and how long | the ✨ button on the add row |
| `proposeMeasure` | a measure for an **unmeasured goal** | opening a goal that has none |
| `getRecommendations` | the coach feed | the dashboard |

So a life area was hand-picked from chips on the form, and **`plan` had never been built**:
`docs/PRODUCT_v0.3.md` §3.3's feature table lists it as **B**, marked *"Today: (none) · After:
new"*, and §3.7 is a full page of design for it. Half of what Ido asked for was already
specified and simply unimplemented; the other half — filing a **goal** rather than a task —
had no line in the spec at all.

---

## 1 · Half two was already specced, and this ships the part he asked for

§3.7 is `C8`'s and is larger than the report. What it specifies and what shipped:

| §3.7 | shipped |
|---|---|
| the model labels each item, and the plan's **shape falls out** | ✅ `PlanStepKind`, and the rung derives from it |
| **the draft gate** — nothing the model decides reaches Firestore unseen | ✅ `GoalPlanSheet` + `ApplyGoalPlanUseCase` |
| an approved step becomes real work on the goal | ✅ a `Task` carrying an `Occurrence` |
| **three** exits per step — keep · already-done · delete | ⚠️ **two**: keep · drop |
| the draft **persists**, one per goal, no expiry | ❌ in memory only |
| `Adjust Plan`, `changeNotes`, `Renumber` | ❌ (`changeNotes` renders if one arrives) |
| duplicate detection on open and on commit | ❌ |

**The four omissions are one omission.** *Already-done* is §3.7's *"evidence flowing backwards
into the next plan"*, and the duplicate check is *"the draft persists … so the check runs on
open as well as on commit"* — both hang off the persisted draft, and `Adjust Plan` is the call
that consumes it. With nothing carrying evidence forward, an *already-done* button would do
exactly what *drop* does, and a duplicate check would have no second opening to run at. They
are named in the KDocs rather than quietly skipped, and they are the rest of `#24`.

---

## 2 · Half one has no spec line, so it reuses `classify`'s rather than inventing one

There is no *"file a **goal**"* feature in §3.3. But §3.3 **D** already answers *"which goal,
**which life area**, or a new goal"* — for a task — and Ido's first ask is that schema's
middle third asked about a goal.

So `fileGoal` runs **`classify.ts`'s validator, unchanged**, with `goals[]` simply absent. That
is not a shortcut: with an empty goal-id list every `suggestedGoalId` fails membership, so the
field is **structurally unreachable** rather than merely unused, and there is exactly one
implementation of *"is this life-area id real?"* in the repo. §3.4's *"validation lives in the
Cloud Function, singly"* is kept by writing no second validator at all.

### It is silent, and that is §0.7 rather than a UX preference

> The app may act silently on **instrumental** structure, but must ask before asserting an
> **intrinsic** edge.

Filing decides which column of the time chart the goal reports into and is one tap to change —
instrumental. The goal's **existence** is intrinsic and is the user's own act: they typed the
title and pressed the button, which is why `declaredBy` stays `USER`. Same verdict
`SmartFiling` already reached for tasks, and the reason there is no toggle for this.

### It only ever fills a silence — and one of the three guards is not derivable

Three conditions, and the third is the one that would have been a defect:

1. **Never on an edit.** A stored goal's filing survived a save; it is the user's.
2. **Never over a chosen life area, including *None*.** Choosing *none* is a decision, so
   `GoalForm.lifingTouched` and not `lifeAreaIds.isEmpty()` is the test — an empty list is both
   *"nobody said"* and *"unfiled, deliberately"*, and §1.2 makes the second a real choice.
3. **Never over a chosen category** — and `category` **cannot answer this**. The form's default
   is `HEALTH`, so *"they left it alone"* and *"they picked HEALTH"* are the same value. Without
   `categoryTouched` the model would win over the user on exactly the goals where the two agree,
   which is §0.7's asymmetry running backwards.

The form also **says** it will file, before the fact, on the one row where leaving it alone is
otherwise indistinguishable from declining.

---

## 3 · The two things the model is not allowed to author, and how each is enforced

§0.5 — *the AI judges, the app computes*. Both restraints are structural, in
`functions/src/plan.ts`, not prompt instructions:

**No ids.** §3.3 B: *"a new step carries no `id` at all and is identified by its position; the
client assigns the id on receipt — making the truncation failure **structurally
unrepresentable** rather than merely checked."* `ValidatedPlanItem` has no id field to read, and
`parsePlan` assigns `index` walking the array. A test asserts an echoed `id` never survives.

**No dates.** The model answers in **`dayOffset`** — whole days from the day the plan is
*applied* — and the app resolves it against its own clock. Two reasons, and the second is the
one that bites:

- §0.5's usual argument: `C11a` measured this model's free numbers swinging **2× run-to-run**.
- §3.7 says a draft *"persists exactly as left … **no expiry**"*. An absolute date would pin the
  plan to the day it was **drawn**, and nothing on screen would say so. There is a test for
  exactly this: the same draft applied four days later lands four days later.

`MAX_DAY_OFFSET = 365` exists to catch a model answering in a different unit — `dayOffset:
20260901` — not to judge a step eleven months out.

---

## 4 · The rung falls out of the label, and the calendar needs no second write

§3.7: *"the model labels each item, and the shape falls out."* So there is no fifth field where
the model picks a rung:

| label | rung | a miss means (§2.2) |
|---|---|---|
| `STATE_YOU_REACH` (milestone) | `Deadline`, at **18:00** | `OVERDUE` — still owed, **not a failure** |
| `WORK_YOU_DO` **with** a time | `Block`, `PROVISIONAL` | `EXPIRED` — silent (§2.3) |
| `WORK_YOU_DO` **without** | `AllDay` | the day passed |

Three notes, each of which is a decision rather than a default:

- **18:00, not midnight.** A `Deadline` at `00:00` is owed before the day it names has started,
  which reads as a whole day late and would fire §2.5's reminder against the wrong day.
- **`PROVISIONAL` is written here for the first time in the app.** `BlockPlacement`'s own KDoc
  said so: *"the agent that would write `PROVISIONAL` is §3.7's proposed plan (`#24`) and does
  not exist."* It does now. §2.4 is why (*"a `BLOCK` needs confirmation, because 09:00 may
  already be taken"*), and §2.3 is what it buys: **an accepted-then-ignored plan does not
  manufacture a wall of failures against the user.**
- **A milestone is never priced.** `C18`'s container rule, enforced in the Cloud Function by
  *stripping* `difficulty`/`estimatedMinutes`/`timeOfDay` from a container rather than dropping
  it — §3.3 B calls it *"a validation failure of those two fields, not of the item"*.

### And that is the whole of the scheduling

`ApplyGoalPlanUseCase` writes a `Task` with an `Occurrence` and **nothing else**:

```
a kept step ──▶ Task { goalEdges = [goal], occurrence = Block | Deadline | AllDay }
                                   │
                                   ├──▶ TaskSchedule.occurrencesIn ──▶ §4.3's calendar surface
                                   └──▶ SyncCalendarUseCase ──▶ the GOALPILOT Google calendar
```

`TaskSchedule.occurrencesIn`'s fourth source is *"the anchor itself, when the task has neither a
rule nor any stored document"* — exactly the shape written here. So both calendars are reached
with **no code in this feature mentioning either of them**, except one line asking for a
`MANUAL` sync so the user does not wait for the next foreground. A plan that wrote calendar
events as well as tasks would be a second writer of one fact, which is §0.3's whole section.
There is a test that asks `TaskSchedule` — the same type the sync walks — what it sees.

**The sync's outcome is deliberately discarded.** A user who never granted the calendar scope
gets `NeedsConsent` on every pass, which §2.6 calls the ordinary state of someone who has not
opted in. Reporting it as a failure of the plan would tell them their tasks were not created
when they were.

---

## 5 · Failure is reported here, and that is the odd one out in this file

Four of the five AI calls in `RecommendationRepositoryImpl` succeed with something on failure —
a local nudge feed, a keyword classification, a neutral estimate, an empty proposal list. Each
has a real offline answer. `planGoal` **does not**: there is no arithmetic over the user's data
that produces a plan for *"run a marathon"*, and the user pressed a button. §0.4 — *speak about
a failure the user can act on* — so it returns `Resource.Error` and the sheet offers a retry.

**`Empty` and `Failed` are two states on purpose.** The model saying *"I have nothing to
propose"* is not the event *nothing answered*, and only one of them is worth retrying — so only
one offers a retry.

`fileGoal` goes the other way and has **no fallback at all**, which is also a decision: the
obvious offline heuristic (match the goal's words against life-area names) is a worse copy of
the exact judgement being asked for, and it would be indistinguishable downstream from a real
answer — the cost `classifyTask`'s catch block already spends a paragraph on. §1.2 makes
*unfiled* legitimate, so an empty filing is both the honest report and a state the app knows.

---

## 6 · A real defect the tests caught, and the compiler could not

`PlanStepKind.fromName` was written as:

```kotlin
fun fromName(name: String?): PlanStepKind = when {
    name.equals(MILESTONE.name, ignoreCase = true) -> MILESTONE
    else -> WORK
}
```

It compiles, it reads correctly, and it is **always false**. The enum constant is `MILESTONE`
because that is what the app calls it; the **wire** says `STATE_YOU_REACH` because that is what
§3.3 B calls it. Every milestone would have arrived silently as `WORK` — which is the *safe*
direction, so nothing would have crashed, nothing would have logged, and milestones would just
quietly have been priced and slotted like tasks.

Caught by `GoalPlanTest > an unrecognised label reads as WORK`, which asserted the positive case
alongside the negative one. The fix names the wire string as a constant with the trap written
next to it.

---

## 6b · A second defect, found by the pre-commit self-review rather than by a test

`dismissPlan()` and the success branch of `applyPlan()` both ended with
`_form.update { it.copy(saved = true) }`, which fires the screen's `LaunchedEffect(form.saved)`
and navigates away. That is exactly right after a **create** — the goal is written and the sheet
is the last step — and **wrong on the edit form**, where the plan is a button the user pressed
*without having saved*. Closing the sheet there would have discarded every edit they had typed,
silently, with the plan's tasks written and the goal's own changes gone.

The plan's state cannot tell the two apart: it is identical in both cases. So the view model
carries `planFinishesScreen`, set by whoever opened the draft. Found by the *pre-commit
self-review* rule's second question — *which of my own arguments does my output contradict* —
against the KDoc one paragraph above it saying the plan is offered on both surfaces.

`Untested:` there is no JVM test for it, because the branch is a navigation side effect behind a
`StateFlow` the screen observes; the check that would catch it is a Compose UI test, and no
device was claimed this session.

---

## 7 · 🧪 Tests

| Layer | Result |
|---|---|
| **Cloud Function (`node --test`, no emulator)** | **174 / 174 green** — 69 of them new in `functions/test/plan.test.mjs` |
| **JVM unit (`:app:testDebugUnitTest`)** | **1146 / 1147** — 20 new in `GoalPlanTest`; the one red is `DocsCurrencyTest`, see below |
| **Compose UI / instrumented** | **not run** — no device claimed this session; `GoalPlanSheet` carries `testTag`s (`goal_plan_steps`, `goal_plan_apply`) so a pass can reach the gate without matching on English |
| **Firestore rules** | **not run, and not affected** — no new collection, no rules change; a plan step is an ordinary task |
| **Live end-to-end against GROQ** | **run, and it found two defects** — both callables deployed and called for real, English and Hebrew; see §10 |

### The one red test, and why it is not fixed here

```
DocsCurrencyTest > every callable the backend exports is named in ARCHITECTURE FAILED
```

The guard is right and is doing its job: `functions/src/index.ts` now exports **six** callables
and `docs/ARCHITECTURE.md` names four. The file is **claimed on `SESSIONS.md` by
`docs-repair`**, whose row owns all six files under `docs/`.

`Observed:` their `docs/` paths are **clean in the working tree**, so nothing of theirs is at
risk — but §5.3(c)'s liveness check could not resolve their transcript (the `file-history` grep
matches **9** sessions, which is the false positive the rule warns about, and one of them last
spoke at **19:48**). **Unresolved counts as live**, so the file was left alone. The exact
three-line edit is on the board under their row, ready to paste, with an offer to make it myself
on one word from them.

---

## 8 · What is not done, and what has not been proven

- ⚠️ **The app has not been run.** Both callables are deployed and verified by direct HTTPS call
  (§10), but **no build was installed on a device this session** — no instrumented pass, no render
  pass, and no one has watched the sheet open on a phone. What is proven is the wire and the
  arithmetic; what is unproven is the screen.
- ⚠️ **`planGoal` fails roughly one call in six, and the client is right about it.** `Observed:`
  §10. The sheet says *"could not be fetched"* and offers a retry, which is the correct behaviour
  for a user-invoked call — but a user who presses once and gets that will reasonably conclude the
  feature is broken.
- ⚠️ **Hebrew is owed for the whole goal form, not for this feature.** Every string added is
  hard-coded English, which matches `AddEditGoalScreen` around it — that screen contains **no
  `stringResource` call at all**. §0.8 (*"not finished until seen in Hebrew"*) and §5.1 are owed
  for the screen; localising only the new sheet would make it half-translate, which reads as a
  bug rather than as progress.
- **The draft does not persist.** Closing the sheet loses it and the plan must be re-requested —
  one call against a 30-RPM ceiling, on a user-invoked feature. §3.7 wants it stored; see §1.
- **A plan is offered automatically only on a *new* goal.** On an existing one it is a button on
  the edit form, and never automatic: re-proposing a plan every time a title is corrected is the
  *"over-eager agent"* §2.3 warns about.

---

## 9 · Files

**New**
- `functions/src/plan.ts` — §3.3 B's validator, Firebase-free
- `functions/test/plan.test.mjs` — 69 tests
- `app/…/domain/model/GoalPlan.kt` — `GoalFiling`, `PlanStepKind`, `PlanStep`, `GoalPlan`
- `app/…/domain/usecase/ApplyGoalPlanUseCase.kt` — the far side of the draft gate
- `app/…/feature/goals/GoalPlanSheet.kt` — the gate itself
- `app/src/test/…/domain/GoalPlanTest.kt` — 20 tests

**Changed**
- `functions/src/index.ts` — `fileGoal` and `planGoal`
- `app/…/core/util/Constants.kt` — the two callable names
- `app/…/domain/repository/RecommendationRepository.kt` — two methods
- `app/…/data/remote/RecommendationRepositoryImpl.kt` — the two calls and their parsers
- `app/…/feature/goals/AddEditGoalViewModel.kt` — silent filing, `PlanState`, the two touched flags
- `app/…/feature/goals/AddEditGoalScreen.kt` — the sheet, the filing notice, the plan button
- `SESSIONS.md` — claim, two addressed notes, daemon borrowed and released

---

## 10 · Deployed, then actually called — which is where the real defects were

The commit above (`90ee0fd`) was written with both callables **undeployed and never called**, and
that changelog said so. Then they were deployed under the standing authorisation
(`docs/OPERATIONS.md` — a functions deploy costs nothing and reaches nobody) and called for real.
**Looking at the output found three things, and no test could have found any of them.**

### 10.1 · The deploy carried a foreign function, and that is disclosed rather than noticed later

`firebase deploy --only functions` created **three** functions, not two:

```
+ functions[fileGoal(us-central1)]                        Successful create
+ functions[planGoal(us-central1)]                        Successful create
+ functions[projectChallengeScoreOnProgress(us-central1)] Successful create   <-- NOT MINE
```

`projectChallengeScoreOnProgress` is **`challenge-scoring`'s**, committed earlier today and never
deployed. `firebase deploy --only functions` is whole-target, not per-symbol — there is no
pathspec form of it, so this is the deploy layer's version of *"`git push` is branch-scoped"*: it
publishes a sibling's work on my schedule, whatever I stage. It is **wanted** work (their
changelog treats the trigger as shipped), it is additive, and their own tests are green — but it
went live because of my deploy, so it is named here. **A later `--only functions:<name>` was used
for every redeploy below**, which is the pathspec form and does not carry anyone.

### 10.2 · Every milestone came back with no date

The first live plan — *"Run a marathon successfully"*, 19 items — was structurally perfect on
every invariant the 69 validator tests check: offsets ascending, no priced container, no id, under
the cap. And **all 7 `STATE_YOU_REACH` items had no `dayOffset` at all**, so every milestone would
have landed on the goal's list undated while every ordinary step got a date. Ido asked for *"a
work plan with tasks **and timings**"*; the items that mark progress had none.

**The validator could not have caught it, and this is the interesting part.** A step with no date
is **legal** — §2.2, and it is exactly what `Task.occurrence = null` means. There is no rule to
violate. It is a **prompt** defect: the sentence *"difficulty, estimatedMinutes and timeOfDay are
for `WORK_YOU_DO` items ONLY and MUST be omitted on a `STATE_YOU_REACH` item"* was generalised by
the model to cover `dayOffset`, which is not on that list. The fix is one sentence saying so
explicitly. This is `kb/dev/look-at-your-own-output.md` exactly: the failure was in a
transformation nothing re-runs, and only reading the real answer finds it.

### 10.3 · And then two calls in three failed outright

```
GROQ HTTP 400: {"code":"json_validate_failed","failed_generation":""}
```

Not visible in `firebase functions:log` — which returned **2 lines** for `planGoal` — and read out
of `gcloud logging read` instead. Two separate causes, both real:

1. **Length.** A 19-item plan overran the output budget and truncated mid-object, so GROQ rejected
   its own reply. `MAX_ITEMS` is now **12**, which is a *budget* and not a taste — and twelve steps
   fit a sheet the user has to read and tick, where nineteen do not.
2. **Empty generations.** `openai/gpt-oss-20b` is a **reasoning** model: with a long system prompt
   it can spend its whole budget reasoning and emit **no content**, which is what
   `"failed_generation":""` says. It is a per-generation dice roll, not a property of the request —
   the same request succeeds next time.

So `planGoal` — and **only** `planGoal` — retries **once** on that specific error.
`answerRetryingEmptyJson` argues its case against §3.4 in its KDoc: §3.4's `transport` class is
*no key, `5xx`, timeout, `429`, a retired model id*, all of which mean the request did not get
through and repeating it learns nothing. This is a `400` about the provider's **own output being
empty**, which the next attempt genuinely re-rolls. Once, not until-it-works.

### 10.4 · Measured, before and after

| | before | after |
|---|---|---|
| `planGoal` success, same goal | **1 / 3** | **5 / 6** |
| items per plan | 19 | 8–12 |
| undated milestones | **7 of 7** | **0**, across all 5 |
| offsets ascending · no priced milestone · no id | ✅ | ✅ across all 5 |

`fileGoal`, first call, no fix needed:

```json
{ "suggestedLifeAreaId": "a1", "suggestedCategory": "FITNESS", "confidence": 0.95,
  "rationale": "Running a marathon is a physical training and endurance goal, directly
                related to health and fitness. It does not align with career, family,
                or finance life areas." }
```

Given four areas — Health, Career, Family, Finance — for *"Run a marathon successfully"*. That is
Ido's first ask, working, from the goal's name alone.

**Hebrew works and is authored in Hebrew** — `language: "he"` for *"ללמוד לנגן בגיטרה"* returned
9 items with Hebrew titles (`d+0 יש לי גיטרה`, `d+1 למד את התווים הבסיסיים`, `d+13 נגן שיר ראשון
בהצלחה`). Those titles are **content** and are stored as authored, never re-rendered (§3.5,
`C15b`). The **chrome** around them is still English — see §8.

### 10.5 · What is still not proven

`Untested:` **nobody has seen this on a phone.** No APK was built or installed this session, so the
sheet, the checkbox row, the dates as rendered, and the tasks appearing on the calendar are all
unobserved. The wire is verified end to end and the arithmetic is JVM-tested; the **screen** is
not. That is the next session's, and it wants a device.
