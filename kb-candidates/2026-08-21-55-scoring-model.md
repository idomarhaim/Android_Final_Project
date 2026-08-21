# KB candidates — `55-scoring-model`, 2026-08-21

Session `55-scoring-model` · ticket [#55](https://github.com/idomarhaim/Android_Final_Project/issues/55) ·
mode **AUTO** · account: `CHANGELOG/2026-08-21/55-scoring-model.md`

Each entry stands alone. A transcript is not a source.

---

## 1 · A schema migration needs a grep for *every reader*, not for every writer

- **Claim.** When a fact moves out of a document into a new collection, the dangerous
  omission is not a write path — it is a **second reader**. Writers are few, named after the
  thing they write, and fail loudly; readers are many, often named after what they *compute*,
  and fail by producing a plausible wrong number. So the migration checklist is
  `grep` for the **collection and the DTO class**, not for the field: every site that turns a
  document into a domain object is a site that has to learn the new shape.
- **Why.** `#55` moved a task's completion into `users/{uid}/completionFacts/{taskId}`. The
  join went into `TaskRepositoryImpl.observeTasks`, which is the obvious reader. It did not go
  into `GoalRepositoryImpl.tasksFlow`, which reads the same collection for the completed-task
  half of a goal's derived progress and is named after **goals**. Result, `Observed:` on
  `emulator-5554` 2026-08-21: a task ticked, struck through and showing `+35` in the list,
  above a goal ring reading **0% / 0 of 100** where it had read 1%.
  **Both test suites were green and neither could have caught it** — the JVM tests hand
  `DerivedProgress` a task list directly, and no test constructs two repositories over one
  database. `grep "FirestorePaths.TASKS\|TaskDto::class"` returns both sites in one line of
  output and would have.
  *Rejected:* copying the join into the second repository — a copy is two things to keep in
  step, which is the same shape as the defect. The fix is one seam (`TaskStream.kt`) that both
  go through, which is `#49`'s argument about `currentValue` applied at the read boundary.
- **Destination.** `kb/dev/` — likely a new page, *schema migrations and the second reader*,
  or a section on an existing data-modelling page.
- **Anchors.** `data/firestore/TaskStream.kt` KDoc · `CHANGELOG/2026-08-21/55-scoring-model.md` §4.1.
- **Supersedes.** Nothing.
- **Status.** ready.

---

## 2 · Deploy order is part of a migration's design, and it is observable before you ship

- **Claim.** When a client change alters the **shape a server function reads**, the two are not
  independently deployable, and which one goes first is a decision the ticket has to state.
  It is also **checkable on a device before shipping**: run the new client against the *old*
  deployed function and read the number.
- **Why.** `#55`'s client stops writing `done` and `points` on task documents. The deployed
  projection sums `points` over documents where `done === true`. `Observed:` 2026-08-21 against
  live `goalpilot-56e30` — completing one task took the stored lifetime total **70 → 40**,
  where the shipped-together pair gives 70 → 115. Nothing errored; the number simply fell, and
  it would have fallen further for every user with every tick.
  The recovery property is worth as much as the finding: because the projection is
  **structurally idempotent** and reads the union of both shapes, deploying it repairs the
  number on the next write with no data migration at all. So the answer is an **ordering**
  (functions first), not a backfill.
  *Rejected:* asserting the ordering in the changelog and not testing it. The 70 → 40 was
  visible in one screenshot and cost one tap; a stated risk that nobody has watched happen is
  exactly the unhedged claim `claim-provenance.md` is about.
- **Destination.** `kb/dev/` — deployment/verification family; pairs with
  `look-at-your-own-output.md` (the check is *run the consumer*, and here the consumer is a
  deployed function).
- **Anchors.** `docs/PRODUCT_v0.3.md` §1.4 status box · `CHANGELOG/2026-08-21/55-scoring-model.md` §4.2 ·
  `functions/src/derived.ts` `pointsFromFacts`.
- **Supersedes.** Nothing.
- **Status.** ready.

---

## 3 · Making an invariant unrepresentable deletes its tests, and that is the win

- **Claim.** When a normaliser exists to repair a state, the better change is usually to make
  the state unconstructable — and the tell that it worked is that **most of the normaliser's
  test suite becomes untestable rather than passing**. Deleting those cases is correct; porting
  them is a sign the state still exists somewhere.
- **Why.** `#7` built `TaskCompletion.stamp` to normalise four combinations of
  `isDone` + `completedAtEpochMillis` into the two that are legal, because four consumers
  disagreed about which field *was* the fact. `#55` replaced the pair with one nullable object,
  so `isDone` **reads** the completion. `TaskCompletionTest` lost most of its cases — there is
  no way to construct a done task with no stamp — and what replaced them asserts the
  unrepresentability itself through the constructor, plus the minting rule and the real
  downstream readers.
  **The counter-example in the same change is the useful half:** the new shape introduced a
  *placeholder* fact (`CompletionFact()` with no timestamp) so that born-done surfaces, which
  cannot know `now`, can say *complete this at the write*. `TaskCompletion.of`'s first version
  used `task.completion ?: …` and returned the placeholder untouched — banking a completion
  stamped at the epoch, invisible in every window-based reader. **Exactly the failure the
  object exists to prevent, arriving through the shape that was supposed to abolish it.**
  So: unrepresentability removes the *original* state, and any sentinel you add back is a new
  one to check. Caught by the first case of the rewritten suite.
- **Destination.** `kb/dev/` — design/testing family; adjacent to the deep-module vocabulary.
- **Anchors.** `domain/model/TaskCompletion.kt` KDoc (§*An unstamped fact is a request*) ·
  `app/src/test/.../domain/TaskCompletionTest.kt` KDoc.
- **Supersedes.** Nothing.
- **Status.** ready.

---

## 4 · A cross-language fixture only holds if both sides call the production code

- **Claim.** A shared `facts → expected` fixture read by two languages is worth exactly as much
  as the **thinness of the adapter** each side wraps around it. A reader that reimplements the
  rule in the test agrees with itself forever.
- **Why.** `shared-fixtures/derived-state.json` pins the derived-state arithmetic across Kotlin
  and TypeScript. Before `#55` the rule was *sum a stored number over the done ones*, so both
  readers reimplemented it in three lines and nothing was lost — the rule was too thin to get
  wrong. §1.4 replaced it with `round(minutes / 3) × difficulty`: a formula, a **rounding
  order** and a multiplier table. A hand-written Kotlin copy in the test would have passed
  while the app rounded in the other order.
  So `DerivedStateFixtureTest` now calls `TaskScoring.pointsFor` directly. Two of this
  session's own fixture expectations were **wrong** and were caught by that (65 not 90, level
  2 not 3) — which is the fixture working as designed, on its author.
  Second, smaller finding: an existing fixture case modelled double-crediting with **two rows
  sharing one id** and a note admitting the shape was not a real Firestore state. When the
  projection became keyed by task id, that case silently started asserting **de-duplication**
  instead of arithmetic. A fixture case whose meaning depends on a detail of the reader's
  adapter is a case that will drift.
- **Destination.** `kb/dev/` — testing family; extends whatever holds the shared-fixture
  pattern from `C20`.
- **Anchors.** `app/src/test/.../derived/DerivedStateFixtureTest.kt` KDoc ·
  `shared-fixtures/derived-state.json` `$comment` and the idempotence case's `$note`.
- **Supersedes.** Nothing — extends `C20`'s original argument for the fixture.
- **Status.** ready.

---

## 5 · `rules/` candidate — a device pass on live data needs a stated blast radius first

- **Claim.** Before the first tap of a device pass **against a live account**, the session
  states in one line what it will create, what it will touch, and how it will restore — and
  it does not tap anything it did not name. A tap is a write, and on a shared surface an
  unaimed one is somebody else's data.
- **Why.** `Observed:` 2026-08-21. The plan was: create a probe task, complete it, delete it.
  What happened: a `keyevent 4` navigated somewhere unintended, a tap at coordinates computed
  for the previous screen landed on a **different goal's existing task**, and the next two taps
  were a misdiagnosis and its correction — an untick and a re-tick of one of Ido's real
  completed tasks. Nothing was lost and the state was restored and verified, but the account
  now carries a task migrated a day earlier than it would have been, and the recovery took
  three screenshots to reason about because the *baseline was never written down*.
  The cheap remedy is not "be careful with coordinates" — it is naming the blast radius, which
  makes an off-plan tap **visible as off-plan at the moment it happens** rather than three
  screenshots later. It also produces the baseline the restore has to be checked against; here
  the pre-session dashboard screenshot was the only reason *"70 pts, 5 tasks done, goal at 1%"*
  could be restored and confirmed at all.
  *Rejected:* requiring an emulator with a throwaway account for every device pass. The
  signed-in live account is precisely what several exit criteria need (`kb/dev/android-device-verification.md`
  §8 exists to preserve it), and a rule that makes the honest check expensive gets skipped.
- **Destination.** **`rules/`** — always-ask in both modes; the 🎬 walkthrough rule owns it.
- **Anchors.** `CHANGELOG/2026-08-21/55-scoring-model.md` §4.2 *(Live data touched)*.
- **Supersedes.** Nothing; adjacent to `kb/dev/android-device-verification.md` §8.
- **Status.** **BLOCKED — destination `rules/`.** Always-ask in both modes, AUTO included.
  Parked here until Ido rules on it.
