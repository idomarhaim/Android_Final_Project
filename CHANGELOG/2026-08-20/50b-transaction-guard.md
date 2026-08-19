# 50b-transaction-guard — the build now refuses the deletion, and the negative control found the guard wrong on its first draft

> **Summary:** One JVM source-reading unit test now goes red if `ConnectivityMonitor` or the
> `connectivity.isOnline()` pre-check is deleted while `TaskRepositoryImpl.setDone` is still
> `firestore.runTransaction` — and reports **skipped, not passed**, the day that stops being true.
> The three-direction verification the brief demanded was not a formality: **direction 2 failed the
> first time**, and the guard as briefed passed with the pre-check fully commented out.

**Session:** `50b-transaction-guard` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE`
**Branch:** `main` · Executes [`sessions/done/50b-transaction-guard.md`](../../sessions/done/50b-transaction-guard.md),
written by [`50-offline-stamps` round 2](50-offline-stamps-r2.md)
**Singletons:** `#gradle-daemon` held for five JVM unit runs, released with this commit. **No device
touched, no emulator started, nothing signed in or out.**

---

## Why this exists

[#50](https://github.com/idomarhaim/Android_Final_Project/issues/50) §5 authorises deleting
`core/net/ConnectivityMonitor.kt` on the stated premise that *"`C20` removes the transaction"*.
**That premise is false at `HEAD`,** re-verified in this working tree before the board was claimed:

```
app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt:98
    firestore.runTransaction { txn ->
```

A Firestore transaction cannot be served from cache, so `setDone` is still server-only, the offline
pre-check is still load-bearing, and deleting it re-opens closed
[#3](https://github.com/idomarhaim/Android_Final_Project/issues/3) — the **7.9 s** optimistic tick
that is then taken back (measured on a device;
[`CHANGELOG/2026-08-08/fix-task-completion-feedback.md`](../2026-08-08/fix-task-completion-feedback.md)).

`50-offline-stamps` had already annotated the spec and argued it on the issue. The deliverable here
is the half that cannot be skimmed.

## What shipped

**One file**, `app/src/test/java/com/idomarhaim/goalpilot/guards/OfflineWriteGuardTest.kt` *(new
package `guards/`, deliberately outside `domain/` and `feature/`)*. It reads three source files and:

1. **Skips** — via `assumeTrue`, not an early return — if `runTransaction` has left
   `TaskRepositoryImpl.kt`. That skip **is** the signal that #50 item 5 is unblocked.
2. Otherwise asserts `core/net/ConnectivityMonitor.kt` exists and `GoalDetailViewModel.kt` still
   calls `connectivity.isOnline()`, with the whole argument in the failure message.

Source-reading is the established idiom here, not an invention: `resources/AnalyticsLiteralSweepTest`
already does it, and this file reuses its `stripComments` helper **byte-for-byte**.

## 🧪 Tests

**Layers.** JVM unit only, which is the whole of this unit — the test reads files off disk and has no
Android, UI or database surface, so **instrumented, endpoint, database, client-page and UI E2E layers
are not applicable and were not run**. The cloud instrumented workflow fires on push anyway
(`.github/workflows/instrumented-tests.yml`, `push:` on `app/**`); that is expected and free.

**Full JVM unit suite, at the committed state:**

```
suites=45  tests=420  skipped=0  failures=0  errors=0
./gradlew :app:testDebugUnitTest → BUILD SUCCESSFUL
```

`419/44` before this unit, `420/45` after — one new suite, one new case, nothing else moved.

### The three-direction verification, verbatim

Counts read from `app/build/test-results/testDebugUnitTest/TEST-…OfflineWriteGuardTest.xml`, never
from the console summary.

**1 — green as-is.**

```xml
<testsuite name="…OfflineWriteGuardTest" tests="1" skipped="0" failures="0" errors="0" time="0.086">
  <testcase name="while setDone is a transaction, the offline pre-check must survive" …/>
```

**2 — red when the thing it guards is removed.** Two shapes, and **the first one failed.**

*(2a, first attempt — the guard exactly as the brief wrote it.)* `connectivity.isOnline()` commented
out in `GoalDetailViewModel.toggleTask`, everything else untouched:

```xml
<testsuite … tests="1" skipped="0" failures="0" errors="0" time="0.074">   ← FALSE PASS
```

**The guard could not fail on the input it most exists for.** The brief's body compared the *raw*
file text, and a commented-out call still contains the string `connectivity.isOnline()`. Commenting
a check out is the commonest way one is disabled in practice, and this is the standing *check the
instrument on the hardest input it exists for* failure exactly: it degraded silently on the case
that motivates it, while every other input kept saying it worked.

**Fix:** both source checks now read `code(path)` = `stripComments(source(path))`, the identical
regex pair `AnalyticsLiteralSweepTest` already uses. Its known crudeness (a `//` inside a string
literal is also stripped) can only make the test fire when it should *not* — loud, and the safe
direction.

*(2a, re-run after the fix.)*

```xml
<testsuite … tests="1" skipped="0" failures="1" errors="0" time="0.197">
  <failure message="TaskRepositoryImpl.setDone is still firestore.runTransaction, which cannot be
   served from the Firestore cache. Offline it fails after a measured 7.9 s, … Deleting this
   re-opens closed #3. …" type="com.google.common.truth.AssertionErrorWithFacts"/>
```

That run also showed Truth dumping all 240 lines of `GoalDetailViewModel` beneath the message, so the
second assertion now tests a `Boolean` rather than the file text — the `why` is the part a reader
needs, and it was buried.

*(2b — `ConnectivityMonitor.kt` renamed to `NetworkMonitor.kt`.* Kotlin does not require the file name
to match the class name, so this compiles cleanly and isolates the file-existence check.*)*

```xml
<testsuite … tests="1" skipped="0" failures="1" errors="0" time="0.226">
```

**3 — skipped, not passed, when the premise flips.** `runTransaction` removed from
`TaskRepositoryImpl.kt` **compile-safely**, via a throwaway `FirebaseFirestore.runTxn` alias in a
temporary file, so the literal left the file under test without gutting `setDone`:

```xml
<testsuite name="…OfflineWriteGuardTest" tests="1" skipped="1" failures="0" errors="0" time="0.091">
  <testcase name="while setDone is a transaction, the offline pre-check must survive" …>
    <skipped/>
  </testcase>
```

`skipped="1"`, `failures="0"`, and a real `<skipped/>` element. **The expiry mechanism is not
decorative** — this was the direction the brief called most likely to be skipped and most likely to
be lying, and it holds.

**Every control was reverted and each revert verified by `git status --porcelain`**, which returned
only the new untracked test directory afterwards. The temporary alias file was deleted.

## Two deviations from the brief, both deliberate

1. **The test body is not verbatim.** `stripComments` / `code()` were added and the last assertion
   was made a `Boolean`. Both came out of running the verification the brief itself prescribed; the
   briefed body would have shipped a guard that passes when the guard is commented out.
2. **The KDoc gained two sections** — why the checks are comment-stripped, and why this is not
   `GoalDetailViewModelTest` twice.

## Why this is not the same test twice

`GoalDetailViewModelTest.an offline tap is refused outright and never fakes a tick` *does* go red
when the pre-check is removed. It is not a substitute: **#50 §5 authorises removing the pre-check**,
so whoever executes that ticket deletes that case in the same breath and is right to. Only this file
ties the permission to a **checkable condition** instead of to a sentence in a ticket, and it is the
one test whose own deletion has a stated precondition — the skip.

**What it still cannot see:** a call left in place but made unreachable, or moved out of
`toggleTask`. Not chased on purpose — a guard that tries to know which branch runs is a guard nobody
can predict, which is why `AnalyticsLiteralSweepTest` stays crude too.

## Scope held

- **Nothing was deleted.** This session prevents a deletion and performs none.
- `guards/` was **not** added to `SWEPT_PACKAGES` — #51 owns that sweep, and these strings are
  assertion messages read by developers, not user-facing copy.
- `C20`'s build half is still unbuilt and still untracked by any issue; the ready-to-paste body is in
  [`50-offline-stamps-r2.md`](50-offline-stamps-r2.md). **That is the thing that unblocks #50 item
  5**, and it is nobody's yet.

## 🕐 One note on dates

The machine clock read `2026-08-19T22:0x` during these runs while the harness date is `2026-08-20`;
the XML timestamps above are quoted as the machine wrote them. Folder and board dates follow the repo
convention already set by the two sessions that committed today.

---

# Rounds 3–4 — `gh`, the ten briefs, and three decision issues that were being read as built

> **Summary:** The session continued past its unit on Ido's direction. `gh` installed and found to
> need no `gh auth login` at all; **ten session briefs written** so every open issue has one; **three
> build-half issues filed** (#52, #53, #54) and their closed decision issues cross-referenced; and the
> ordering question answered by opening files rather than asserting — which found **four real
> conflicts** and killed one coupling I had claimed that did not exist.

**Singletons:** none held in rounds 3–4 — no Gradle build, no device. `c20-build-half` ran in a
parallel session throughout and nothing here touched a path it owns.

## `gh` — and the bullet that was wrong within an hour of being written

Round 3 corrected `CLAUDE.md`'s *"`gh` is NOT INSTALLED on this machine at all"* (true 2026-08-19,
false once installed) and replaced it with three bullets. **One of those three was itself wrong**,
and round 4 replaced it:

| Claim | Verdict |
|---|---|
| `winget install --id GitHub.cli` hangs silently | **true** — 1.1 s CPU over 12 min, zero output, waiting on an elevation prompt it cannot display. The tell is CPU time, not the absence of output |
| a tool shell inherits a pre-install environment | **true** — same shape as `JAVA_HOME` |
| *"Authentication is Ido's and nobody else's"* | **FALSE.** `git push` works, so a credential exists; `git credential fill` returns it with `repo` + `gist` + `workflow` scope |

`gh auth login` is therefore not merely unnecessary but mildly harmful — it writes a **second copy**
of the same secret into `~/.config/gh/hosts.yml`, to be rotated separately and forgotten. Same family
as `kb/dev/redaction-leaves-a-second-copy.md`. The token is now read per command and persisted
nowhere.

**The permission gate is unchanged**, and that is the half that was never mechanical: binary and
token are mechanics; Ido's word is still required before a write. He gave it by naming the action.

**Two `gh` installs existed briefly.** A portable zip was installed while `winget` appeared hung;
`winget` then completed after Ido approved the prompt. The portable copy and its User `PATH` entry
were removed — two copies of one binary means whichever wins the `PATH` race decides the version, and
only one of them is upgradable.

## Ten briefs — every open issue now has one

| Brief | Issue |
|---|---|
| `c20-build-half` | **#52** *(filed here)* |
| `50-finish` | #50 |
| `7-quickadd-complete` · `9-duration-box` · `11-fill-buttons` · `8-notifications` · `6-silent-filing` | #7 · #9 · #11 · #8 · #6 |
| `c12-material-contract` · `c13-key-store` | **#53** · **#54** *(filed here)* |
| `51-freeze-verify` | #51 *(pre-existing)* |

**#48 is the one open issue with no brief, deliberately** — its remainder *is* #53 and #54. That is
now recorded on #48 itself rather than only in a reply.

### `c20-build-half` was written against HEAD, and the spec turned out to be stale

`/kickoff c20-derived-state` failed in a parallel session: that is a **past session label**
(`CHANGELOG/2026-08-14/`, `2026-08-15/`), not a brief. It halted correctly at `/kickoff` §1, took
nothing, and noted that `sessions/` was claimed here — so this session wrote the brief.

Writing it surfaced a **defect in the design of record**. `docs/PRODUCT_v0.3.md` §5.2 says *"two
client transactions already write `goal.currentValue` (`GoalRepositoryImpl.kt:87`,
`TaskRepositoryImpl.kt:135`)"*. **Both are gone** — removed by #49, recorded in `TaskRepositoryImpl`'s
own KDoc at `:33-36`; `grep -n '"currentValue"'` returns nothing in either file. A brief that
repeated it would have sent a session hunting two writers that do not exist.

The real remainder is **one function** — `setDone`'s three writes, of which `:114` is a fact that
stays and `:122`/`:123` are derived numbers that go to the server. #52's body omits the stale
sentence and says so, so the new ticket does not re-seed the claim its own brief exists to correct.

**§5.2 itself is still uncorrected** and is this session's one open issue — `docs/` was not in this
session's claim.

## Three decision issues were being read as built — in this tracker, three times over

#42 (`C20`), #31 (`C12`) and #32 (`C13`) all closed as **decided**, all shipped no code, and none
carried a pointer to a build half. That is exactly `kb/dev/decision-map-charting.md` §12, live.
Each now carries a comment naming its build half and why the distinction matters, with the #50
near-miss as the worked example.

## The ordering question — checked, not asserted

Ido asked whether everything after `c20-build-half` could run in parallel with order irrelevant.
**No**, and the previous answer was partly wrong:

- **Retracted:** *"#9 and #11 are one wave — split them and you migrate twice."* False. Different
  documents; `grep -rl unit … | xargs grep -l looksLikeFallback` returns **empty**. They are
  independent.
- **Found, by opening the files:** `9 → 7` (both land in `AddTaskRow`, which holds the very
  `aiMinutes` value #9 redefines) · `7 → 6` (both edit `DashboardScreen.kt`) · `c12 → c13` (both edit
  `SettingsScreen.kt`) · `11` after `C20` (both edit `Dtos.kt`) · `50-finish` alone.
- **`8-notifications` collides with nothing** and is the only true free-floater.
- **The Gradle daemon is exclusive anyway**, so the list is a queue rather than a parallel plan.

Each constraint is written into the brief that needs it, so a session opening only its own file
still sees it.

## 🧪 Tests

**Nothing was built or run in rounds 3–4** — no Kotlin, no resources, no `app/**` path. The unit's
own suite (**420/0, 45 suites**) is recorded above and unchanged.

**The cloud emulator ran twice and both were green**, which is a real result rather than a formality:

- **run #5**, push-triggered on `a632ad2` — `success`, 7m24s. The guard test shipped in this
  session's unit ran on a cloud runner and passed.
- **run 32313901157**, dispatched here with `capture-screenshots=true` — `success`, 7m07s.

**#48's last owed item is closed by observation.** `48-settings-surface` finished with *"the sign-in
screen's Settings button has not been seen on a device"*. Both runs' screenshot artifacts were
downloaded and **looked at**: the Settings control is present on the sign-in screen on a runner with
**no Google account** — the exact condition it needed. Navigation is covered by `SettingsScreenTest`
(*"the signed-out branch — §4.9's proof that Settings is the device"*), green on both runs.

**Finding worth keeping:** the screenshot job is `workflow_dispatch`-only —
`.github/workflows/instrumented-tests.yml:197` gates it on
`github.event_name == 'workflow_dispatch' && inputs.capture-screenshots`. **A push-triggered run
never photographs anything**, which is why run #5 carried only a test report.

## Errors made and fixed in these rounds

1. **A `\r` written into a candidate file** — a Windows path `JARVIS\rules\` in a code path that
   collapsed the escape, so `\r` became a carriage return mid-word. Found by scanning every file the
   session wrote for control characters, **not** by reading them.
2. **The literal word `PATH` destroyed in `CLAUDE.md`** — a placeholder token chosen for backslash
   substitution collided with the word it was standing next to. Caught by re-reading the rendered
   line, which is the only thing that would have caught it.

Both are the same class as `kb/dev/escapes-die-in-transit.md`: the transformation, not the text.
