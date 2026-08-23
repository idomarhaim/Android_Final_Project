# 65-measure-proposal — `C7`'s fifth AI feature, five months after it was handed on

> **Summary:** [`#65`](https://github.com/idomarhaim/Android_Final_Project/issues/65) ships whole — §1.3's **two surfaces**, §3.3 E's **schema** and §3.4's **mechanical fallback**. A silent dashed-square marker wherever an unmeasured goal is listed; the offer itself only on the goal's own screen, because opening the goal is the consent §0.7 requires. The `proposeMeasure` Cloud Function is **deployed and live** (`Successful create operation`, `us-central1`). **876 JVM unit tests, 0 failures** (18 this session's) · **93 functions tests, 0 failures** (26 new) · **11 instrumented tests, 0 failures**, with three render-pass PNGs looked at, two of which changed the copy.

**Date:** 2026-08-23 · **Session:** `65-measure-proposal` · **Mode:** AUTO · **Issue:** [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)

## The two questions the brief held for Ido — neither was asked, and here is why

The brief said *"Two questions for Ido, and they come first."* Both were **derived** instead
(`rules/derivable-decision.md`; `rules/question-axis-naming.md`). Both decisions are **mine**, and
both are Ido's to overturn.

**1 · "Does he agree with the two-surface answer at all?"** — **Not asked, because asking it is
forbidden.** §10.1 records that this was *decided by the agent on Ido's hand-back*, and
`question-axis-naming.md`'s hand-back rule is explicit: *"Do not re-ask — not smaller, not as a
situation, not a narrower picker; re-asking a delegated question is not a smaller question, it is a
refusal to accept the delegation."* The brief's own next section says **do not reopen it**. So it is
built as decided; §10.1's *"all of it is his to overturn"* is the standing offer, and it does not
need re-issuing per ticket.

**2 · "Would he rather fold this into `estimate` than give it a fifth AI call?"** — **Derived: keep
it separate.** It turns on the artifact, not on Ido, so the ownership sort sends it to derivation.
Three reasons, any one sufficient:

- **Different subject.** `estimate` is *per task* and answers *how demanding, how long*. `measure`
  is *per goal* and answers *what should this count*. §3.2's *one wide call per feature* is wide
  over one feature's own subject; a call wide over tasks cannot carry a goal-shaped question.
- **Different trigger.** `estimate` fires on task creation — the app's highest-volume moment.
  `measure` fires when a goal *first becomes eligible*, and because dismissal is permanent a goal is
  proposed **at most once, ever**.
- **The cost argument runs backwards.** Folding it in would attach a once-per-goal question to a
  once-per-task call, spending a request every time a task is added to any goal. The separate call
  is the *cheaper* of the two against the free tier's 30 RPM.

## Why nothing existed to build on

`C7` handed this on in so many words — *"a **fifth AI feature** for `C11b` to write an output format
for."* `C11b` wrote four schemas and reached *"five"* by counting `classify`. **Two tickets each said
"fifth" about a different feature, and `C7`'s never got written.** `Observed:` before this session,
`grep -rn "MeasureProposal\|measureProposal"` over `app/src/main` returned **zero hits**.

## What shipped

| Layer | File | What it is |
|---|---|---|
| domain | `domain/model/MeasureProposal.kt` *(new)* | the proposal, `MeasureBasis`, `TargetSource`, `ProposalOrigin`, `GoalStructure` |
| domain | `domain/usecase/ProposeMeasureUseCase.kt` *(new)* | §3.4's mechanical proposal, the target arithmetic, and `structureOf` |
| wire | `functions/src/measure.ts` *(new)* | §3.3 E's validator — membership, enums, the 24-char cap, whole-element drop |
| wire | `functions/src/index.ts` | the `proposeMeasure` callable and its prompt |
| client | `data/remote/RecommendationRepositoryImpl.kt` | `proposeMeasures`, and the target computed on arrival |
| store | `data/prefs/AppPreferencesRepositoryImpl.kt` | §1.3's permanent per-goal dismissal |
| UI | `ui/components/UnmeasuredMarker.kt` *(new)* | the dashed square — surface 1 |
| UI | `feature/goals/MeasureProposalCard.kt` *(new)* | the absence note and the offer — surface 2 |
| UI | `ui/components/GoalCard.kt`, `feature/goals/GoalDetail*.kt` | the two wiring sites |

## The three decisions worth arguing with

**1 · The steps target is the TOTAL step count, not `openStepCount`.** §3.4's row reads
*"`openStepCount >= 2` → count the steps you already listed — target = the count"*, and the literal
reading is the open count. That reading produces a target that **shrinks every time a step is
completed** — a goal at `0/6` reading `0/5` after real progress, which is §0.3's *second number that
quietly disagrees*, on the very screen this ticket exists to fix. The prototype's frame 4 renders the
other reading (*"2 of 8 done"*). So the **gate** is `openStepCount >= 2`, exactly as specced, and the
**target** is the total. At the moment §3.3 E fires the call — *when the goal first becomes eligible* —
nothing is done yet and the two are equal; they only diverge on a goal the offer reaches late.
`GoalStructure`'s KDoc and `the steps target does not shrink as steps are completed` carry it.

**2 · Dismissal lives in `SharedPreferences`, not on the goal document.** #65's own exit criterion is
*"a dismissed goal never offers again, **across process death**"*, which this satisfies exactly.
`data/firestore` was also held by a live sibling (`63-occurrences-and-recurrence`) and was not this
session's to widen. **The honest limit:** a second device re-offers on a goal dismissed on this one.
`Untested:` whether that is felt as a defect — this app has one user and one signed-in device.
Moving it onto `GoalDto` is additive and costs one nullable field the day someone decides it is.

**3 · The call is wide by contract and sends one goal today.** The wire carries `goals[]` and
`proposals[]` per §3.2, so batching costs nothing to add. The only caller sends the goal being
**opened**, because §1.3 puts the offer on the goal's own screen — firing over the whole list would
spend calls on goals nobody opens. The ceiling is identical either way: at most one call per goal, ever.

## 🧪 Tests

| Layer | Result |
|---|---|
| **Cloud Function (`node --test`)** | **93 pass, 0 fail** — 26 new in `functions/test/measure.test.mjs` |
| **JVM unit** | **876 pass, 0 fail, 0 skipped** — **18** of them this session's, in `MeasureProposalTest` |
| **Instrumented (device)** | **11 pass, 0 fail** — `MeasureProposalUiTest` on `emulator-5554` |
| **Render pass** | 3 PNGs pulled and **looked at**; two rounds of copy changed as a result |
| **Firestore rules** | not touched — `users/{uid}/**` is already `isOwner(uid)` and carries no field whitelist |

Run with `adb install -r` + `am instrument`, **never** `connectedDebugAndroidTest`: no uninstall, so
**no sign-in was needed and none was destroyed**.

⚠️ **The JVM total moved under me mid-session and the number here is the honest one.** It read **860**
at the first full run and **876** at the last, and none of the 16 are mine: `63-occurrences-and-recurrence`
is live in this tree and landed `ScheduleMappingTest`, `RepeatRuleTest` and their siblings in between.
Reporting 876 as *this session's suite* would be true and misleading; **18** is this session's
contribution, and the suite is green with both sessions' work in it.

The one wire assertion worth naming: **`there is no numeric field anywhere in a validated proposal`**.
It is the whole of §3.3 E in one test — a future field carrying a number would pass every other test
in that file.

## What the render pass caught, which nothing else could

Three defects, all invisible in the source, all found by opening the PNG.

1. **Frame 5 was not in the bitmap.** All five states in one column, captured with `onRoot()`, which
   is the activity window — so the bottom fell off, and what fell off was *the app offering nothing*,
   the single frame §10.1's argument turns on. A capture that silently drops the control frame is
   **worse than no capture, because it reports green.** Now two PNGs, each holding a comparison that
   fits, with the silent state beside a live offer.
2. **The offer had grown a third block of policy prose.** A draft carried §1.3's *"changing a kind is
   never silent"* under the buttons; rendered, the card was three stacked paragraphs of app policy
   over a two-line offer — which is what a nag looks like, and is the exact failure #65 exists to
   avoid. The prototype's own `offer()` renders title, **one** `why` line and two buttons; its
   `changeable` string belongs to the confirm sheet. Deleted.
3. **`"Count kg lost"` is not English.** The model-path title was `"Count <word>"`, and the schema's
   `word` is a **unit** that need not be a plural noun. Now *"Measure it in \<word\>"*, which survives
   `kg lost`, `a week` and `pages` alike. `MeasureBasis.LEADING` moved to a two-word tag rather than
   another sentence.

And one the **test** caught for the same reason: `onNodeWithText("Measure it in kg lost")` found
nothing, because `bidiIsolated()` wraps the word in U+2068/U+2069 — so the rendered string is not the
one the source spells, and the failure reads as *the title never rendered*. Matching on the frame,
which carries no isolates.

## Traps this session paid for, recorded so nobody pays twice

- **`/**` inside a KDoc opens a nested Kotlin block comment.** A doc line mentioning the
  `data/firestore/**` glob made the compiler report *"Missing '}'"* at one line and *"Unclosed
  comment"* at EOF — neither naming the cause. Same family as `CLAUDE.md`'s `--`-in-XML-comment trap:
  the file format treats your prose as syntax. Checked **mechanically** afterwards by walking every
  touched file with a nesting counter, not by eye.
- **`--` inside an XML comment** was written and caught the same way — a mechanical scan over comment
  bodies plus an `ElementTree` parse of every `values*/*.xml`, before the build ever ran.
- **Git Bash rewrites `/storage/...` in an `adb pull`.** The error reads
  *"failed to stat remote object 'C:/Program Files/Git/storage/...'"*, which looks like *the test
  never wrote the file*. `MSYS_NO_PATHCONV=1`. Now in the test's own KDoc.

## Deployed

`firebase deploy --only functions` with `FUNCTIONS_DISCOVERY_TIMEOUT=120`, under
[`docs/OPERATIONS.md`](../../docs/OPERATIONS.md) § *Standing authorisation* — **deployed, not waited
on**. `+ functions[proposeMeasure(us-central1)] Successful create operation.` The six existing
functions updated cleanly alongside it.

## Found and NOT fixed — it belongs to the measure model

⚠️ **An unmeasured goal still renders a percentage.** The dark render shows it plainly: *Get fit*
carries the marker (*no number yet*) and `0%` and `0/100` on the same row, while the measured *Drink
water* reads `25%` and `1/4 L`. The marker does not create this — it **makes it visible**, which is
arguably it working. But a row that says *no number* and prints a percentage of a target it does not
have is §0.3's finding again, and §1.3 already rules that `"%"` survives *only as a chosen `PERCENT`
measure*. The fix is to suppress the percent for `measure == null`, and that is the **measure model**,
which #65's brief puts explicitly out of scope (`#11` / §1.3). Filed here rather than silently fixed.

## Not done, and it is Ido's move

**`#65` is not closed on GitHub.** A `gh issue close` is an outward write, and this project's
`CLAUDE.md` keeps those behind Ido's word regardless of auto mode. The work is complete and pushed;
the ticket needs one command.

---

## Follow-up pass, same day — the defect got a brief, and the report of it was wrong twice

Ido asked whether the defect needed its own kickoff. **It does**, and answering it corrected two
things in the report above.

**1 · `#11` is CLOSED, so "next: on #11" pointed the fix at a finished ticket.** Its brief is already
in `sessions/done/11-fill-buttons.md`. Of the seven open issues (`#51`, `#59`–`#62`, `#64`, `#65`),
none covers this. The finding had no home at all, which is exactly the state a `next:` line is
supposed to rule out.

**2 · It is six sites, not one, and two of them are worse than "a stray number".** Each was verified
by reading it rather than by grep, and every cited line number was then re-checked mechanically
against the file:

- `BuildWidgetSnapshotUseCase` is **half-fixed** — `measureLabel()` already gates on `hasMeasure`,
  with a KDoc making precisely this argument (*"trains the eye to read two numbers as two facts"*),
  while `percent` is passed unconditionally one line down. So the reasoning is **already settled in
  this codebase** and was applied at one site and not the rest. That is the strongest single argument
  in the new brief and it is not mine.
- `RecommendationRepositoryImpl` **speaks it**, and its filter is
  `goals.filter { it.progressFraction < 0.34f }`. An unmeasured goal reads `currentValue / 100.0` and
  a goal nobody has logged against sits at **exactly `0.0`** — so the offline nudge feed
  **preferentially selects goals that have no number** and then quotes their percentage.
  `Observed:` by reading the filter and `Goal.progressFraction`. `Untested:` on a live account.

**Written:** `sessions/unmeasured-percent.md`, carrying the brief and the ticket body in one file.
The ticket is **not posted** — `gh issue create` is an outward write and stays behind Ido's word — so
the brief's `issue:` field says `unassigned` rather than guessing a number, and the body sits below a
`---` ready to paste.

**The one design call is left open on purpose:** what a row shows where the percentage was. The `C22`
prototype's life-area frame already draws one answer (*"no number — 11 sessions logged"*), which is
why the brief points at it rather than proposing a fourth option.

**No code changed in this pass.** Prose only: no build, no device, no emulator.

