# 2026-08-15 — `c23-goal-category`

`/wayfinder 12` in **work-through-the-map** mode, `AUTO MODE`. No ticket was named, so the session
picks one: *"take the first frontier ticket in order."*

## 🎯 Claimed — [`C23` #45](https://github.com/idomarhaim/Android_Final_Project/issues/45)

**`C23 · GoalCategory: does it stay closed at ten, and where do its labels live once they translate?`**
— `wayfinder:grilling`, assigned to `idomarhaim`, which *is* the wayfinder claim.

**How the frontier was computed**, so the choice is checkable rather than asserted:

- Map [`#12`](https://github.com/idomarhaim/Android_Final_Project/issues/12) has **three open children** —
  [`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
  [`C23` #45](https://github.com/idomarhaim/Android_Final_Project/issues/45),
  [`C24` #46](https://github.com/idomarhaim/Android_Final_Project/issues/46).
- **`#44` is already claimed** — assigned to `idomarhaim` by the live sibling `c22-measure-proposal`
  (`e15c1d7`, 16:58). So it is off this session's frontier: the assignee *is* the claim.
- `#45` and `#46` were both **unassigned** and both returned `0` from
  `gh api repos/.../issues/<n>/dependencies/blocked_by` — **unblocked**. No `Blocked by:` line in either
  body, so the fallback convention agrees with the native relation.
- Frontier for this session = `{#45, #46}`; first in order = **`#45`**. **`#46` is left unclaimed** for
  whoever comes third.

`#12` itself stays **open**: closing the map is the last act and it is Ido's, per its own *What is left*.

## 🧭 The sibling was checked, not assumed

A row on the board is not proof its session is live, in either direction — so `c22-measure-proposal`'s
claim on `#44` was verified before it was honoured, and the check is recorded because the conclusion
(*do not take `#44`*) rests on it:

- `CHANGELOG/2026-08-15/c22-measure-proposal.md` exists and its claim commit `e15c1d7` is 45 minutes old
  — suggestive of live, proof of nothing (a session can claim and die).
- The deciding evidence is its **transcript**: found by the `file-history` records naming its changelog
  path (never `grep -l <label>`, which returns every session that merely *read* the board),
  `d076a45e-….jsonl`, whose last `user`/`assistant` record carries `timestamp` **`2026-08-15T14:45:17Z`**
  — one minute before this session wrote its own row. **Live.**
- `Observed:` the timestamp above, read from the record body — **not** the file's mtime, which is bumped
  by records the session never produced and so reports the dead as live.

## 🔎 Type of ticket, and what that means for this session

`#45` is **`grilling` → HITL**. Per the skill, *a HITL ticket only resolves through that live exchange;
the agent never stands in for the human's side of it.* So this session cannot answer `#45` on its own,
and specifically may not decide **which ten categories fit Ido's life** — `C1` makes him the authority
on facts about his own life, and the ticket's question 1 is exactly such a fact.

What the session **can** carry alone is the half the artifact already determines: the `domain/` layer may
hold no Android types, so *where the labels live* has a small closed set of answers with measurable costs,
and those are prepared as inputs to the exchange rather than put to Ido as a menu of mechanisms.

## 📋 Session-start sweep

- **`SESSIONS.md`** — Active claims read **one live row** (`c22-measure-proposal`) before the first write;
  this session's row added under lease (`Lock-Path.ps1`, `SESSIONS.md` → `c23-goal-category`), with both
  overlaps (`#12` map body, `sessions/`) named on the board rather than assumed away.
- **`kb-candidates/` — non-empty, 7 files**, and one is a **live debt** carried by two sessions now:
  `2026-08-15-product-v03-spec.md` entry 2 is 🟢 `AUTO MODE`-eligible for `kb/dev/decision-map-charting.md`
  and still undrained — draining it is a cross-repo `C:\Dev\JARVIS` visit that owes a row on *that* board.
  Not this unit's work; reported, not silently carried.

## 🤝 Ido handed the decision back, so `C23` is resolved by the agent

The one question put to him — *what work does the app's category stamp do for you next to your own life
areas* — came back as a **delegation plus a comprehension complaint**, in near-identical wording to the
one `product-v03-spec` received three hours earlier: *"I couldn't fully understand you or the implications
of each option — explain simply, schematically, short. And choose the solution that gives the highest
standard and quality of the app (and its purpose), UX/UI and the software. And if you think the solution
can be improved — improve it."*

Handled per `rules/question-axis-naming.md`: **no re-ask** (not smaller, not as a situation, no second
picker), the comprehension half paid **once in the reply as an explanation**, the decision **derived** and
recorded as **the agent's** on the durable record, and `Untested:` whether Ido agrees with any of it.

**And the answer was not one of the four options offered** — which is what that rule predicts a delegated
question usually does.

### What decided it

`C23` looked like *"which of these two taxonomies wins"*, and the fork was **false**. The two models grep
clean against each other; the collapse is in the **palette**: `LifeAreaPalette` holds the *same ten hexes,
copy-pasted*, plus `iconKeyFor(name)` — a **bilingual** guesser that already maps `בריאות`→favorite and
`ריצה`→fitness. So colour, icon and grouping — three of the four options — were already delivered by the
user-authored object, and delivered better, because it reads Hebrew and the enum cannot.

### The resolution, in one line each — full text in [#45](https://github.com/idomarhaim/Android_Final_Project/issues/45)

1. **The enum survives as machinery, never as a taxonomy the user sees.** Where a goal has a life area,
   the area owns its colour, icon, chip and grouping. No screen shows two taxonomies for one goal.
2. **Ten → seven**; `SLEEP`, `NUTRITION` and `OTHER` deleted; `Goal.category` becomes **nullable**,
   inheriting the sentence the codebase already wrote one field up for `lifeAreaId` (*"a made-up default
   would put real time into the wrong slice"*). Zero-write migration — `fromName` maps retired strings to
   `null`.
3. **The model classifies into Ido's own areas** (closed at call time, so the prompt's closed-enum
   requirement holds) with an explicit *"none of these"*; the built-in seven are the cold-start fallback.
   This is the improvement he asked for: a new goal lands in `אימון ריצה`, not in `Fitness`.
4. **The seven double as the first-run seed list**, which is where `C15`'s app-speech / user-content line
   actually falls: the app *proposes* in the current language, and the moment one is accepted it is user
   content and frozen.
5. **`GoalCategory.label` is deleted, not moved** — `strings.xml` + `values-he` keyed by constant name,
   the `iconKey` precedent. Deleting the field is what makes shipping English into a Hebrew screen a
   compile error instead of a review item. Five call sites lose it, one of them a `contentDescription`.
6. **Colours: one palette, both brightnesses, generated** by `C12`'s transform rather than hand-authored;
   `ThemePaletteTest` owed an extension and a re-pin from ten to seven.

## 🐞 A defect the ticket had to look at — [#47](https://github.com/idomarhaim/Android_Final_Project/issues/47) *(new)*

**`BuildHealthProposalsUseCase` matches an existing goal by `category == metric.category` (`:167`), and
the category is a chip the user can edit.** Editing a fitness goal's category orphans it from the Health
Connect sync, which then creates a **duplicate** "Weekly steps" goal — and that sync is automatic,
unreviewed and 15-minute-throttled, so nobody is watching when it happens. `Observed:` the code path.
`Untested:` not reproduced on a device.

Filed as an ordinary `bug`, per `#12`'s taxonomy (defects are not map children). **It makes the shrink a
strict hand-off, not a suggestion:** deleting `SLEEP` under the current matcher points the sleep metric at
a constant that no longer exists, so the two land in one change or neither does. Named this way because
`#12`'s amended Destination now requires *every hand-off named in a resolution to resolve somewhere* —
this map's own scar, earned three hours ago.

## 🗺️ The map's *Decisions so far* line is **owed, not written**

`#12`'s body is a **claimed singleton** and the live sibling `c22-measure-proposal` holds it. So the append
was not taken; the exact line to paste is posted as a comment on
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12#issuecomment-5302797690) for whoever
holds the body next. `#12` stays open; **`C24` #46 is the remaining unclaimed frontier ticket.**

## 🧪 Tests

None — this unit writes no code, and `#12`'s Standing preferences forbid a map ticket shipping code at all.
The layers this repo has (JVM unit `:app:testDebugUnitTest`, instrumented `:app:connectedDebugAndroidTest`,
`firestore-tests/`) are untouched and were not run.

**What the resolution *owes* the test layers when a build session implements it**, stated here so it is not
discovered late: `ThemePaletteTest` re-pinned from ten categories to seven and extended to both
brightnesses × four materials; `HealthProposalsTest` and `HealthSyncTest` rewritten off `GoalCategory.SLEEP`
and onto the metric identity (they reference the deleted constants today); and a new test that the health
matcher survives a category edit, which is [#47](https://github.com/idomarhaim/Android_Final_Project/issues/47)'s
regression test.

## 📦 Files

- `SESSIONS.md` — claim row + the two-overlap note, then release.
- `CHANGELOG/2026-08-15/c23-goal-category.md` *(new)* — this file.
- `kb-candidates/2026-08-15-c23-goal-category.md` *(new)* — three entries, one 🟢 eligible and two ⛔
  always-ask (`rules/` destination).
- GitHub: `#45` assigned, resolution comment, **closed**; `#47` **created**; `#12` comment with the owed
  index line.

---

# Unit 2 — [#47](https://github.com/idomarhaim/Android_Final_Project/issues/47) fixed *(session reopened)*

## 🤝 A third delegation, and this one was not a product question

Asked which session to open next — four thin options, concrete actions, a table of consequences above the
picker — Ido replied with the **same words a third time**: *"I couldn't fully understand you … explain
simply and schematically … choose the solution that gives the highest standard and quality … and if you
can improve it, improve it."*

That is the informative instance. The first two were product decisions and were diagnosed as picker-design
faults (dense options, a false premise). This one had neither, so **the diagnosis was wrong**: the variable
is not the question's density, axis or premise — the standing request is for *a schematic explanation and a
decision, not a menu*. Recorded as a third data point in `kb-candidates/`, which is `rules/`-destined and
therefore always-ask.

**Derived, and the answer was again not one of the options offered.** The picker's own recommendation was
*"open a new session on `#47`"*. Opening a session is ceremony: a brief, a `/kickoff`, a claim, a fresh
agent re-deriving from the issue body — for a one-field fix whose full reasoning was already in this
session's head, with both singletons free. So the decision was **do it here, now**, and the reply carried
the explanation. Decision taken per `rules/scale-adaptive-ceremony.md`; recorded as the agent's.

## 🔑 The fix: a display attribute is not an identity

`BuildHealthProposalsUseCase` answered *"does a goal for this metric already exist?"* by matching
`goal.category == metric.category`. The category is a **chip the user can edit**, so one edit orphaned the
goal from a sync that runs on every foreground with **no human watching**, and the next run created a
second "Weekly steps" goal.

- **`Goal.healthSourceKey: String?`** *(new)* — `"hc:goal:steps"` / `"hc:goal:sleep"`, from
  `HealthMetric.goalSourceKey`. Per-**metric**, unlike the existing per-**reading**
  `BuildHealthProposalsUseCase.sourceKey`.
- **`match()` is pinned-first**: a goal carrying the key wins outright, whatever its category now says.
- **The old heuristic survives, for unpinned goals only** — a goal the *user* made ("Move more") carries no
  key and must still be found the first time. A goal pinned to the **other** metric is excluded from it
  outright, which is also what keeps steps and sleep apart once `C23`'s shrink deletes `GoalCategory.SLEEP`.
- **`pinMatchedGoals()`** stamps whatever the heuristic matched, so each goal goes through it **at most
  once** — the exposure is one-time rather than standing. Placed after the dedupe lookup, so a sync that
  owes nothing writes nothing.
- **Additive schema, zero migration.** `GoalDto.healthSourceKey` defaults to null; an existing document
  reads back null and is stamped on the next sync.

## 🧪 Tests — `:app:testDebugUnitTest`, **225 tests, 0 failures**, 24 classes

Run under `JAVA_HOME=…/jdk-21.0.12.8-hotspot` (the machine default still points at the wrecked
`jdk-21.0.11.10-hotspot` — `social-share-bugs` found this and it is still Ido's to fix).

**+7 new cases**, `HealthProposalsTest` 15→**19**, `HealthSyncTest` 18→**21**:

- *a pinned goal is still matched after the user edits its category* — **the regression test for #47**.
- *a goal pinned to sleep is never taken by steps, whatever its category says* — the guard `C23`'s shrink
  will depend on.
- *a pinned goal wins over an unpinned one that matches the old way*.
- *an unpinned goal is still found the first time* — proves the heuristic was preserved, not replaced.
- *a goal the sync creates is pinned to its metric at birth*.
- *a goal matched by the old heuristic is stamped, so it is matched by key next time*.
- *an already-pinned goal is not stamped again*.

Layers not exercised, stated rather than skipped silently: **instrumented** (`connectedDebugAndroidTest`) —
nothing here touches UI, and the emulator is `social-share-bugs`'s; **`firestore-tests/`** — no rules
change, the field is inside a document already covered by the owner-only match.

`Observed:` the 225/0 run above. `Untested:` the live round-trip — no Firestore document has actually been
stamped, because that needs the signed-in app on a device.

## 🧭 Singletons

**Gradle daemon taken as a lease, not a claim** (`Lock-Path.ps1`, granted 15:26Z, released 15:39Z) —
`social-share-bugs` was mid device pass and its row does not name the daemon, so a claim would have blocked
it for the unit while a lease blocked nobody. No emulator, no live project.

## 📦 Files — unit 2

- `app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt` — `healthSourceKey`.
- `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildHealthProposalsUseCase.kt` —
  `HealthMetric.goalSourceKey`, pinned-first `match()`.
- `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SyncHealthDataUseCase.kt` — `pinMatchedGoals()`,
  pinned at creation.
- `app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Dtos.kt`, `.../Mappers.kt` — the field, both
  directions.
- `app/src/test/java/com/idomarhaim/goalpilot/domain/HealthProposalsTest.kt`, `.../HealthSyncTest.kt` — +7.
- `SESSIONS.md` — reopened claim, then release.
