# c20-build-half — the projection ships, `setDone` is one write, and the guard has gone quiet

> **Summary:** `C20`'s build half is built. Derived state left the client: one projection in
> `functions/`, registered on two fact paths, now owns `publicProfiles.points` and
> `challengeParticipant.score`; `publicProfiles.level` is deleted outright; `firestore.rules` gets
> its first two field-level conditions and a test suite that proves them; and
> `TaskRepositoryImpl.setDone` is a single-document write of a fact. `OfflineWriteGuardTest` reports
> **`<skipped/>`** in the results XML — the receipt this unit was built to produce, and the signal
> that **#50 item 5 is unblocked**.

**Session:** `c20-build-half` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE`
**Branch:** `main` · Executes [`sessions/done/c20-build-half.md`](../../sessions/done/c20-build-half.md),
written by [`50b-transaction-guard` round 3](50b-transaction-guard.md)
**Singletons:** `#gradle-daemon` (six JVM runs) and `#firebase-emulator` (three suites), both
released with this commit. **No device touched, nothing signed in or out.**

---

## Why this exists

[#42](https://github.com/idomarhaim/Android_Final_Project/issues/42) (`C20`) closed on 2026-08-14 as
a **decision**. Its build half was never written, and until 2026-08-20 no issue tracked it — the
*decided-read-as-built* failure recorded at `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` §12,
with this ticket as the worked example. It now has a number:
[#52](https://github.com/idomarhaim/Android_Final_Project/issues/52). Design of record:
[`docs/PRODUCT_v0.3.md`](../../docs/PRODUCT_v0.3.md) §5.2, whose rule is the whole unit:

> **A derived number gets a stored writer if and only if somebody who cannot read its inputs has to
> read it.**

`firestore.rules` already draws that boundary at `isOwner(uid)`, which is what makes the rule a
grep rather than a matter of taste.

---

## What shipped

### 1 · One projection, two trigger registrations

`functions/src/index.ts` held **three `onCall` callables and zero Firestore triggers**. It now
re-exports two, both from `functions/src/projection.ts`:

| Trigger path | Writes | Why it needs a stored writer |
|---|---|---|
| `users/{uid}/tasks/{taskId}` | `users/{uid}.points`, `publicProfiles/{uid}.points` + `updatedAt` | a leaderboard reader cannot read `users/{uid}/tasks` |
| `users/{uid}/challengeReports/{challengeId}` | `challenges/{cid}/participants/{uid}.score` + `updatedAt` | the standings reader is not the person who measured it |

**Project from facts; never recompute-and-store.** Each run reads the *whole* fact set and writes
the total, so a redelivered event, a retry and a manual re-run all write the same number.
Idempotence is **structural**, not something a caller has to be careful about — which is exactly
why §5.2 rejected `FieldValue.increment`: *`increment` is the accumulator.* The arithmetic itself
lives in `functions/src/derived.ts` with **no Firebase imports**, so it is unit-testable without an
emulator.

`projectChallengeScore` uses `update()`, never a merging `set()`. A report from somebody who never
joined, or who has left, writes **nothing** — it must not resurrect a participant row with no
mirror edge, which is the hazard `ChallengeRepositoryImpl.reportScore` used to document in its own
comment. That hazard moved to the server with the write.

### 2 · A new fact path, because a projection needs facts to project from

`challengeParticipant.score` was **self-reported directly onto the public row**. Under §5.2's ruling
the client may no longer write it, so the report needed somewhere to live that the reporter owns:

- **`users/{uid}/challengeReports/{challengeId}`** — `{ value, reportedAt }`, covered by the
  existing `users/{uid}/{document=**}` owner rule, so it needed no new rule and it **works
  offline** like every other fact.
- `ChallengeRepositoryImpl.reportScore` writes that instead of the standing.

One user-visible consequence, and it is an improvement: reporting into a challenge you never joined
used to fail with *"Join the challenge before reporting a score"*. It now succeeds and simply
projects nowhere. The join check that message stood for is in the UI, which only offers the field
to a participant.

### 3 · `publicProfiles.level` is deleted

A stored function of `points` **in the same document**, so nobody who could read it was unable to
compute it. Three sites, all gone:

- `dto/Dtos.kt` — `var level: Int = 1`
- `dto/Mappers.kt` — `resolvedLevel()`, whose fallback could never fire because both writers wrote ≥ 1
- `data/firestore/SocialRepositoryImpl.kt` — its only caller, now `Leveling.levelForPoints(p.points)`
- `data/auth/AuthRepositoryImpl.kt` — the `"level" to 1` in the sign-up write

`User.level` (`domain/model/User.kt:14`) is untouched. It is §5.2's **worked example** of the right
shape, not a site to change.

### 4 · `setDone` reduced to one write

```kotlin
tasksCol(uid).document(taskId)
    .update(mapOf("done" to done, "completedAt" to if (done) System.currentTimeMillis() else null))
```

Three writes in one `runTransaction` became one write to one document, and **the transaction had
nothing left to be atomic about**. That is the offline win (§5.3): a single-document write goes
straight into the Firestore cache and completes with the radio off, where `runTransaction` cannot
touch the cache at all and failed after a measured **7.9 s** (closed
[#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)).

The idempotent-no-op guard went with it and is not missed: with no read-then-write, setting `done`
to the value it already holds writes the same document twice, which is the same state.

### 5 · `firestore.rules` — its first field-level conditions

One helper, two uses:

```
function serverOwns(field) {
  return resource != null
    && !request.resource.data.diff(resource.data).affectedKeys().hasAny([field]);
}
```

`affectedKeys()` rather than a field comparison, because it catches an **add, an edit and a
removal**, and because `request.resource.data.field == resource.data.field` errors out on a
document where the key is absent — denying for the wrong reason, which reads in the emulator log as
a permissions bug. The `resource != null` guard is the same concern one step out: Firestore
evaluates both the create and the update clause for a `set()`, so on a missing document `diff()`
would fail and log an evaluation error indistinguishable from a broken rule.

Both `publicProfiles/{uid}` and `challenges/{cid}/participants/{uid}` now split `write` into
`create` / `update` / `delete`: create at zero, update anything **but** the projected field, delete
freely. The projection reaches both through the **Admin SDK, which bypasses rules entirely**, so
the server needs no exemption written into the file and there is no service-account uid to keep in
step with a deployment.

The two NOTEs that said *"points/level are written by the client for this course project — in
production, compute them in a Cloud Function and lock writes down"* are gone, because that is now
what happens.

### 6 · The shared fixture — §5.2's honest residual, not skipped

> *"the arithmetic now exists in **Kotlin and TypeScript** — a second implementation that can
> disagree. Accepted, because avoiding it costs the offline win entirely, and **pinned by a shared
> `facts → expected numbers` fixture both test layers run**."*

**`shared-fixtures/derived-state.json`** — 10 points cases, 4 score cases, one file, two readers:

- `functions/test/projection.test.mjs` → `../shared-fixtures/derived-state.json`
- `app/src/test/java/com/idomarhaim/goalpilot/derived/DerivedStateFixtureTest.kt` → the same file

Neither owns it; both walk up out of their own module. It is **not** two copies of the same
numbers, which is the disagreement it exists to prevent.

Two details worth keeping:

- **`kotlinx.serialization`, not `org.json`,** on the JVM side. `org.json` is stubbed in the Android
  unit-test classpath and every call throws `Stub!` — a parser that fails only inside the layer it
  was chosen for. `kotlinx-serialization-json` is already an `implementation` dependency.
- **The fixture is now a declared Gradle input** (`app/build.gradle.kts`), beside the two
  file-scanning-guard inputs that were already there for the same reason. See *Tests* below: it was
  not, and that silently broke the negative control.

---

## 🧪 Tests

Every layer this repo has that can reach this change was run. `Observed:` all counts below are from
this working tree on 2026-08-20.

| Layer | Result |
|---|---|
| **`functions/` unit** (`node:test`, new layer) | **17 / 17 pass**, 0 fail |
| **Security rules** (`firestore-tests`, emulator) | **41 / 41 pass**, 0 fail — was 29 |
| **JVM unit** (`:app:testDebugUnitTest`) | **425 tests, 0 failures, 0 errors, 1 skipped**, 46 suites — was 420 / 45 |
| **`assembleDebug`** | **BUILD SUCCESSFUL** |
| Android instrumented | **not run** — no device, and this session's brief forbids taking one. Nothing here is UI. |
| UI E2E | same |

`functions/` had **no test layer at all** before this. `package.json` now carries
`"test": "npm run build && node --test \"test/*.test.mjs\""` — the compiled `lib/`, not the source,
so the tests exercise the exact JavaScript that deploys.

### The deliverable's own receipt

`app/build/test-results/testDebugUnitTest/TEST-com.idomarhaim.goalpilot.guards.OfflineWriteGuardTest.xml`,
verbatim:

```xml
<testsuite name="com.idomarhaim.goalpilot.guards.OfflineWriteGuardTest" tests="1" skipped="1" failures="0" errors="0" ...>
  <testcase name="while setDone is a transaction, the offline pre-check must survive" ...>
    <skipped/>
  </testcase>
```

A real `<skipped/>` element, read out of the XML rather than off the console summary — which is
what `50b-transaction-guard` built that test to produce and what its `assumeTrue` was waiting for.
**#50 item 5 is unblocked.** This session deliberately does **not** act on it; see *Scope held*.

### Negative controls — three, and one of them found a defect

A green suite proves nothing until it is shown to go red. Each control was run, observed, and
reverted.

**1 · The fixture, from the TypeScript side.** Changed one expected total, `75 → 76`.
→ **1 fail / 16 pass**, and the failure was *exactly* `points: only the done ones are summed`.
Fires where it should, silent where it should not.

**2 · The fixture, from the JVM side — this one failed first.** Same broken number, and
`DerivedStateFixtureTest` went red under an explicit `--tests` filter. Then the **restore** was
verified with a full run and it reported `BUILD SUCCESSFUL in 1s` — **up to date**, having run
nothing.

`shared-fixtures/` is outside the `app` module, so Gradle could not see it as an input: **editing
the fixture alone did not invalidate the test task.** The suite would have reported green on the
previous run's numbers, which is precisely the silent degradation the fixture exists to prevent —
in the one file it exists for. Fixed by declaring it in `app/build.gradle.kts`, then re-verified in
both directions: fixture broken → `FAILED` on a fixture-only edit; fixture restored → 425 / 0 / 1.

**3 · The rules helper.** `serverOwns()` neutered to `return true`. → **4 fail / 37 pass**, and the
four are exactly the four that depend on it:

```
✖ C20: a participant cannot move their own score any more
✖ C20: a whole-document set that drops `score` is refused too
✖ C20: the owner cannot award themselves points
✖ C20: nor quietly drop the field on a whole-document write
```

The two *create*-clause tests stayed **green**, which is the result that matters most: the create
guard and the update guard are independently tested rather than one rule being asserted twice.

### Every negative test here is paired with a positive

`AGENTS.md`'s standing warning is that a pure negative passes vacuously when nothing matches at
all — delete a whole `match` block and every `assertFails` still passes, because an unmatched path
is denied by default. So `a participant can still edit their own row -- just not the score` and
`the owner can still refresh their display name and photo` are load-bearing, not symmetry: the
second is what `AuthRepositoryImpl` does on every sign-in, and if it goes red, signing in stops
updating the leaderboard row.

One existing test was **inverted rather than deleted**: `a participant can update their own score`
was correct until today. It is now two tests — the participant can still edit their row, and cannot
move that one field. The pair *is* the argument.

---

## Decisions taken here, recorded so they can be overturned

**1 · `users/{uid}.points` keeps a server writer, and §5.2's table row says it should not.**
The table says `points` — *a sum over completion facts — no writer*, and the brief's §1 says the
projection writes *"`users/{uid}.points` **and** `publicProfiles/{uid}.points`"*. The brief is the
work order and it was followed. Making the owner's private copy purely computed means changing
every reader of it — profile, dashboard, widget, sharing — which is a separate and much larger unit
than the one briefed, and none of it is what unblocks #50. The client writes it nowhere, which is
the half of the rule that carries the offline win. **Flagged, not hidden.**

**2 · The participant row has two enforced writers, not the three §5.2 names.** The participant
(everything but `score`) and the projection (via the Admin SDK). The third — the challenge owner —
is a **reader**: there is still no "kick a participant", because granting it needs a `get()` on the
parent challenge inside the rule, billing a document read on every evaluation. That is a documented
limitation with a test of its own, and it predates this change.

**3 · No `metricUnit` validation on a report.** A report is a bare number, as it was before. Out of
scope.

---

## Scope held

- ⛔ **Nothing deleted that #50 authorises.** `ConnectivityMonitor`, the `GoalDetailViewModel`
  pre-check, `OFFLINE_MESSAGE` and `OfflineWriteGuardTest` are all **untouched**. A build that also
  performed the deletion it just authorised is indistinguishable from the failure that guard
  exists to prevent — `decision-map-charting.md` §12a. The skip is reported; the next session acts.
- ⛔ **`docs/PRODUCT_v0.3.md` untouched.** §5.2 still carries the stale sentence about two
  `goal.currentValue` writers at `GoalRepositoryImpl.kt:87` / `TaskRepositoryImpl.kt:135` (both
  removed by #49). `50b-transaction-guard` **round 5 claimed that file** to fix exactly this, mid-
  session, so it is theirs and was left alone.
- ⛔ **No device, no sign-in, no sign-out.** No instrumented run, so nothing was uninstalled.
- ⛔ **`goal.currentValue`** — already done by #49; nothing to remove.
- ⛔ **#7, #9, #11** — downstream, separately briefed.

---

## The ticket — [#52](https://github.com/idomarhaim/Android_Final_Project/issues/52), and it is ready to close

The brief said this build half was tracked by no issue and handed over a body to paste.
**That was true when the brief was written and is not true now:** `50b-transaction-guard` filed
[#52](https://github.com/idomarhaim/Android_Final_Project/issues/52) while this session was working.
Checked here rather than taken from the brief — the unauthenticated REST read path
(`curl -s https://api.github.com/repos/idomarhaim/Android_Final_Project/issues/52`) returns it
**open**, with the same four scope items this session shipped. The brief's front matter already
carries `issue: 52`.

So the paste-ready body is not reproduced here; #52 *is* it. What is owed instead:

**#52 is ready to close, and this session did not close it.** Closing an issue is an outward-facing
action and stays always-ask in both modes — and `gh` is still unreachable from a tool shell here
(`gh: command not found`; the portable install is on the **User** `PATH`, which a shell captured
before it was added does not have, and `gh auth login` is an interactive device flow that is Ido's
anyway). Its own *"Done looks like"* is the `<skipped/>` element quoted above, which is now in the
results XML.

Also worth a comment on #52 when it is closed: **its scope item 1 and this changelog's *Decisions
taken here* item 1 disagree with §5.2's table** about whether `users/{uid}.points` keeps a writer.
The ticket and the brief both say it does; the table's row says *no writer*. The ticket was
followed. That is a real open question about the design of record, not an implementation detail.

---

## ⚠️ One deployment fact worth knowing before this reaches a phone

**Points stop moving until the projection is deployed.** These are v2 Firestore triggers; they do
not run in production until `firebase deploy --only functions` has been run against
`goalpilot-56e30`, and the repo's three existing `onCall` functions are v2 as well, so the project
is already on the tier that allows them. Until that deploy:

- completing a task still works, offline and on — the fact is written and the tick is instant;
- the owner's own screens are still right, because they read facts;
- **the leaderboard row and challenge standings freeze** at their last value.

That is the trade §5.2 made deliberately. It is called out here because it is the one consequence
that is invisible in a green build.
