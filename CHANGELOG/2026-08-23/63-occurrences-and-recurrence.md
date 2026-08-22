# `63-occurrences-and-recurrence` — 2026-08-23

> **Summary:** `#63` closes the half `#56` deliberately left out. A task can now **repeat**, and
> *"this occurrence, or all future ones?"* is a question the model can answer differently — which
> §2.1 says is the whole reason the shape exists, because *"a field-only model always answers just
> this one; a rule-only model always answers all of them."* The rule (`RepeatRule`) lives on the
> task and generates the series for free; the instances somebody **touched** — moved, skipped,
> done, or given a Google event id — are documents in the new `users/{uid}/occurrences`. So `R18`'s
> fortnightly flowers are one rule and **zero** documents rather than 26 a year, and this ticket
> lands the foundation `#64` cannot start without and `#61` wants `googleEventId` on.
>
> **Two decisions worth reading before the code.** The collection is **flat and per-user**, not
> nested under the task, which is what lets §4.3 read a date range across every task in one query —
> and it is why the collection needed **no `firestore.rules` change at all**: the owner-only
> `users/{uid}/{document=**}` match already covers it, the same finding life areas produced. And
> §7.1's *"`isDone` … absent on a recurring task"* fires on **unbounded** recurrence alone: a rule
> that ends has a complete set of windows and therefore an answer, so returning *no answer* for it
> would invent an absence §0.4 forbids.
>
> ⚠️ **The subtle branch is `THIS_AND_FUTURE` with a move.** The rule generates from the task's own
> occurrence, so re-anchoring it moves the **past** as well — and §2.3 forbids that outright:
> *"a missed occurrence is never edited — it is history."* So the past is written down as documents
> **before** the anchor moves, and an `AfterCount` bound is decremented by exactly what was
> materialised, or a moved series silently grants itself a full new count. A move whose past does
> not fit in one `WriteBatch` is **refused with both numbers** rather than chunked or truncated.
>
> JVM **876 / 0** across 79 classes (**+68 tests, +4 classes**). Security rules **50 / 0**
> (**+6**), run against the local emulator — and **mutation-checked**: narrowing the wildcard so it
> no longer covers `occurrences` makes **1 of the 6 fail**, and that one is the owner-succeeds case.
> The other five pass vacuously, which is §4 below.
>
> ✅ **Nothing rendered and no device was touched** — no `adb`, no AVD, no instrumented run. `#60`
> is explicitly not blocked by this and should not wait for it.

---

## 1. What was missing, and what the two halves are

[`#56`](https://github.com/idomarhaim/Android_Final_Project/issues/56) shipped four fields on the
task — `occurrenceRung` / `Start` / `End` / `Placement` — and said in its own KDoc that this was
*at most one* `when`, with recurrence *"the other half and not here."* `Observed:` at the start of
this session, `grep -rn occurrences` over `core/`, `data/firestore/` and `firestore.rules` returned
**zero hits**.

§2.1 wants both, and is explicit about why neither alone works:

> *"A date **on** `Task` gives one — so `R18`'s flowers become **26 duplicate documents a year**…
> A **rule alone**, computed and unstored, cannot hold a moved instance, a skip, or a Google event
> id. **Both, and that combination is what makes *"this occurrence, or all future ones?"*
> askable.**"*

So:

| Half | Where it lives | What it is for |
|---|---|---|
| **the rule** | `Task.repeatRule`, a nested map on the task document | generates the series; costs nothing per instance |
| **the documents** | `users/{uid}/occurrences/{id}` | the instances somebody **touched** — moved, skipped, done, synced |

`Task.occurrence` stops being *the* occurrence and becomes the series' **template and start**: its
rung, its time of day and its duration are copied onto every generated instance
(`Occurrence.onDate`), and its date is where the series begins. A task with no rule is unchanged in
every respect, which is every task in the database today.

## 2. The naming decisions, stated because §7.1 asked for them

§7.1 is explicit that its collection names are *"this file's own"* and that *"the **shape** is
decided, the spelling is the build session's."* What was chosen, and why:

| Thing | Name chosen | Why |
|---|---|---|
| the collection | **`occurrences`**, kept as §7.1 spells it | the domain type shipped by `#56` is already `Occurrence`; a second word for one concept is the defect §0.3 keeps naming |
| its path | **`users/{uid}/occurrences/{id}`**, flat and per-user | §4.3 reads a date range across **every** task — one query here, a collection-group query if nested under the task. And a per-user subcollection inherits the owner-only wildcard, so there is no rules change |
| the link to the task | **`taskId` field** | forced by the flat path, and it is what `whereEqualTo` narrows on for a single task's schedule |
| the rule | **`RepeatRule`**, `{unit, interval, weekdays, end}` | `unit` + `interval` rather than a `FORTNIGHTLY` constant, which would be `WEEK` × 2 wearing a second name |
| its bound | **`RepeatEnd`**, a sealed `Never` / `OnDate` / `AfterCount` | RFC 5545 has `UNTIL` **and** `COUNT` and every consumer re-implements the tie-break; one of three makes the disagreement unrepresentable |
| the pause | **`pausedUntil: Long?`** | §7.1 names it and types it, and calls the name normative |
| what happened to a window | **`OccurrenceOutcome`**, `Planned` / `Done(at)` / `Skipped(at)` | one object, not a flag beside a stamp — `#55`'s argument, and it applies harder with three states, where a `skippedAt` beside a `doneAt` admits both |
| the three-way `isDone` | **`Doneness`**, `Stored` / `Derived` / `Unanswerable` | a bare `Boolean?` cannot say which of *nobody ticked it* and *the question does not apply* the null means |
| which instance a document is | **`seriesDate`** | the date the **rule** produced, not the date the occurrence now sits on — that difference is the entire recurrence mechanism |
| the scope question | **`EditScope`**, `THIS_OCCURRENCE` / `THIS_AND_FUTURE` | §2.1's own two answers, verbatim |

**No third scope.** An `ALL` reaching backwards is not among §2.1's answers, and §2.3 says why —
*"a missed occurrence is never edited — it is history"* — as does §2.8's *"future events cancel,
past events stay."*

## 3. The sharp edge: `isDone` splits three ways

§7.1's row reads *"stored with no occurrences, **derived** with them, **absent** on a recurring
task"*, and everything that reads `isDone` today assumes the first.

**`Task.isDone` was left alone.** It is still `completion != null`, which is correct for every task
in the database — the collection is new and nothing is in it — and widening it to `Boolean?` would
have rewritten every screen that reads it for a case none of them can be in. The three-way answer is
`TaskSchedule.doneness`, which says *which* of the three it gave you.

**`Inferred:` *"recurring"* was read as *unbounded*.** A rule that ends — *ten times*, *until 1
September* — is recurring and has a complete set of windows, so it **has** an answer; returning
*no answer* for it would be inventing an absence §0.4 forbids. `Doneness.Unanswerable` therefore
fires on `RepeatEnd.Never` alone, and the branch is checked **first**, because such a task can also
have stored occurrences and those would otherwise derive a confident *1 of 1* about a series with
no end. This is the same move `#56` made for §2.3's two missing miss names, and it is recorded in
the constant's own KDoc and asserted in two tests.

A **skipped** window is excluded from the denominator — a skip is a decision not to do it, so
counting it as outstanding leaves a task permanently unfinished for having been pruned. A task whose
every instance was skipped is `Derived(0, 0)` and is **not** done: nothing was completed, and
reporting completion would invent an achievement.

## 4. Security rules — the file did not change, and that is the finding

`users/{uid}/{document=**}` already matches every per-user subcollection with an owner-only rule, so
a new one under that path is a client-side change. AGENTS.md records the same result for life areas.
Nothing in an occurrence is server-owned either: no Cloud Function reads the collection (`Observed:`
`grep -rni occurrence functions/src/` returns **3** hits, all of them the unrelated LLM prompt field
`occurrencesPerWeek`), and `googleEventId` is written by the **client**, because §2.6 buys
`calendar.app.created` client-side and §2.7 says outright that *"there is no credential for a
background sync and cannot be one."* So a field-level condition like `serverOwns('score')` would
have had nothing to protect, and §5.2's own test — *does this quantity cross an ownership
boundary?* — says no. **Decision taken per that principle rather than asked.**

What is owed instead is a **test**, because this is the only layer that can read `firestore.rules`
at all: six cases were added covering owner CRUD, stranger read, stranger write, stranger
edit-and-delete of an existing document, the repeat rule's privacy on the task, and a
batch-shaped write.

⚠️ **The mutation check found that five of those six are decorative, and it is worth the sentence.**
Replacing the recursive wildcard with three explicit per-collection matches that omit `occurrences`
produced **1 failure out of 6** — the owner-succeeds case. Every denial case stayed green, because
a path matching **no rule** is denied, which is observationally identical to a path denied *by* a
rule. AGENTS.md carries this claim in one clause with no number; the number is the part that changes
behaviour. `firestore.rules` was restored and re-verified at 50/50 afterwards.

## 5. `THIS_AND_FUTURE` with a move — the branch that is not obvious

Skipping this-and-future is the easy half: the rule's end becomes the day before, and stored
documents from that date on are deleted. (Skipping from the very first instance takes the *when*
with it — a rule ending the day before its own start is legal, inert, and reads as a bug.)

Moving is not, and re-anchoring naively is **wrong**: the rule generates from `Task.occurrence`, so
moving that moves the past too. §2.3 forbids editing history. So `ScheduleEdits.moveSeries`:

1. materialises every already-generated instance **before** the target date as documents, skipping
   any the user already touched (or the write would carry a fresh `Planned` over a `Done`);
2. moves the anchor;
3. deletes stored documents from the target date on — they belonged to the old series;
4. **decrements an `AfterCount` bound by exactly what it materialised.** Without this, *"every other
   week, ten times"*, moved after three, runs for ten more. That branch has its own test.

**The cost, stated:** the past is finite but not small, and Firestore's `WriteBatch` limit is 500
operations. A plan that would exceed it returns `SchedulePlan.TooLarge(required, limit)` and the
repository refuses it, naming both numbers — **legal, but never silent** (§0.4). Chunking would buy
a plan that can be half-applied, which is the one property the batch is here for; truncating would
drop history silently. `Untested:` no plan has hit the limit against a live Firestore — the test
asserts the refusal at 701 required and the acceptance at exactly 500.

## 6. What this ticket did **not** build, named rather than left to be discovered

- **Points per occurrence.** `completionFacts` is keyed `{taskId}`, one per task, so a repeating
  task banks its points once. `OccurrenceOutcome.Done` records *the window was honoured* and banks
  nothing; its KDoc says so. Widening that collection's key is a migration on live data and belongs
  to [`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64), which is the reader
  that needs it. §7.1 gained the same note.
- **Any UI.** Nothing renders. `EditScope` is a type and a pure function; the sheet that asks the
  question is the calendar surface's ([`#60`](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
- **Google sync.** `googleEventId` has a home and a `linkGoogleEvent` that can also clear it (§2.7:
  *"a disappearance never deletes and never re-creates"*). Nothing writes it —
  [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61).
- **A range query.** `observeOccurrences` reads the whole (small) collection, cache-served, as the
  `completionFacts` join in `TaskStream` already does. §4.3's range is applied in the domain by
  `TaskSchedule.occurrencesIn`, which has to expand the rule anyway and therefore cannot be replaced
  by a query.

## 7. Two calendar branches that are silent when wrong

Both are decisions `RepeatRule` states, both are asserted, and both are what a naive implementation
gets wrong by default:

- **A month with no such day is skipped, never clamped.** `LocalDate.plusMonths` turns 31 January
  into 28 February — a kindness for a display, and for a *commitment* it silently moves the thing to
  a day nobody chose and then moves it back in March. A monthly rule on the 31st lands only in
  months that have one; a yearly rule on 29 February lands only in leap years.
- **The fortnight is measured from the anchor, not from Monday and not from the locale.** Week `k`
  is `[anchor + 7k, anchor + 7k + 6]`. An ISO-week reading puts the boundary somewhere the user did
  not choose; a locale-week reading **moves when they change region**, which is the failure that
  cannot be reproduced on the machine that reports it. `AppRegion`'s week start governs how a
  calendar is drawn and has no business deciding when a commitment recurs.

## 8. Files

**New**

- `domain/model/RepeatRule.kt` — `RepeatUnit`, `RepeatEnd`, `RepeatRule` + expansion, plus
  `Occurrence.startDate` and `Occurrence.onDate`.
- `domain/model/Schedule.kt` — `OccurrenceOutcome`, `ScheduledOccurrence`, `Doneness`,
  `TaskSchedule`, `EditScope`, `ScheduleEdit`, `SchedulePlan`, `ScheduleEdits`.
- `domain/repository/OccurrenceRepository.kt` · `data/firestore/OccurrenceRepositoryImpl.kt`.
- `app/src/test/.../domain/model/RepeatRuleTest.kt` (22) · `TaskScheduleTest.kt` (16) ·
  `ScheduleEditsTest.kt` (14) · `app/src/test/.../data/ScheduleMappingTest.kt` (16).

**Changed**

- `domain/model/Task.kt` — `repeatRule`, `pausedUntil`; the *"at most one"* KDoc paragraph is now
  false and was rewritten rather than left; `isDone` gained the three-way pointer.
- `domain/model/Occurrence.kt` — *"What this deliberately is not"* became *"where it now lives"*.
  The section predicted arriving would be additive; it was, and nothing else in the file changed.
- `data/firestore/dto/Dtos.kt` — `OccurrenceDto`, `RepeatRuleDto`; `TaskDto` gained two fields.
- `data/firestore/dto/Mappers.kt` — the four-field occurrence codec was **extracted** from
  `TaskDto.occurrence()` into a shared `decodeOccurrence`, because §7.1's migration is *"the four
  task fields become the first occurrence in it"* — a sentence that is only true while both sides
  spell it the same way, and a second copy of that `when` would make it false silently, on one
  rung, months later.
- `core/util/Constants.kt` — `FirestorePaths.OCCURRENCES`.
- `di/RepositoryModule.kt` · `firestore-tests/rules.test.mjs` · `docs/PRODUCT_v0.3.md` §7.1.

**Not changed:** `firestore.rules` — see §4.

## 9. 🧪 Tests

| Layer | Result |
|---|---|
| **Server unit / integration / endpoints** | ⚪ **not applicable** — this ticket adds no Cloud Function. `functions/` still has no test layer of its own beyond `#65`'s in-flight `measure.test.mjs`; §7.2 records that gap and it is not this ticket's. |
| **Database (security rules)** | ✅ `firestore-tests/` **50 / 0**, **+6** new, against the local emulator on `demo-goalpilot`. Mutation-checked — see §4. |
| **Client unit (JVM)** | ✅ `:app:testDebugUnitTest` **876 / 0** across **79** classes, **+68** tests and **+4** classes. |
| **Client component / page** | ⚪ **not applicable** — no composable, no ViewModel. Nothing in this ticket renders. |
| **UI E2E (instrumented)** | ⚪ **deliberately not run.** No UI changed, and the brief says no device work is required. Not skipped for cost: there is nothing on a screen to assert. |

⚠️ **`BUILD SUCCESSFUL` was read once and it was not the run.** `:app:testDebugUnitTest` reported
`UP-TO-DATE` in 7 s over test classes that had never executed — a sibling session had run the task
in between, so the green belonged to **their** run over a tree containing **their** uncommitted
work. `--rerun` gave `1 executed` in 27 s. Every count above comes from a run whose task line reads
executed, and from the XML under `build/test-results/`, not from the console summary.

## 10. Notes on the tree this was built in

Four sessions were live on this repo today. `65-measure-proposal`'s uncommitted work broke
`:app:compileDebugUnitTestKotlin` for a while — an interface member their fake had not caught up
with, and a constructor that had gained a parameter — with an error naming three files, none of them
this session's. Nothing of theirs was touched; the `firestore-tests/` half of this ticket was done
while it cleared, and the compile succeeded unchanged later. Both sessions edit
`core/util/Constants.kt`, in different objects (`FirestorePaths` here, `CloudFunctions` there);
whoever commits that path second carries the other's line, which is named here rather than
prevented.
