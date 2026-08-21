---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 58
created: 2026-08-21
---

# `#58` — the instrumented suite is order-dependent

**Written by `55-scoring-model`**, which hit this while verifying `#55` and diagnosed it far
enough to be worth writing down, but deliberately did not fix it: `#55` was already shipped,
and folding an unrelated diagnosis into a closed ticket's session is how a clean ticket gets
muddy.

> ⚠️ **RUN THIS AFTER [`56-occurrence-model`](56-occurrence-model.md) AND BEFORE the `#57`
> presentation briefs** (`57a`–`57d`). Not a preference — a collision and a compounding cost:
> `#56` holds `app/src/androidTest/**` and the AVD **right now**, and all four `#57` briefs
> need device/render work, so running them on a suite that fails a random test per run means
> four sessions each re-diagnosing the same flake. The gap between `#56` and `#57` is the only
> window in which this conflicts with nobody.

> 🔒 **Singletons.** Needs the **Gradle daemon**, **adb**, and an **AVD** — and needs to run
> the *full* suite repeatedly, so it holds them for a while. Check the board first.

> 📱 **The device account.** Use `install -r` + `adb shell am instrument`, **never**
> `connectedDebugAndroidTest` — that task uninstalls the app and takes the signed-in Google
> account with it. `C:\Dev\JARVIS\kb\dev\android-device-verification.md` §8.

## Read first

1. [AGENTS.md](../AGENTS.md)
2. `curl -s https://api.github.com/repos/idomarhaim/Android_Final_Project/issues/58` — the
   ticket carries both runs' data, the isolation results, the hypothesis and four suggested
   directions. **It is the substance; this brief is the framing.**
3. [`docs/OPERATIONS.md`](../docs/OPERATIONS.md) § *Driving the emulator with adb* — it already
   documents the IME behaviour the hypothesis rests on, for humans. That it was never carried
   into the test helpers is the likely root cause.
4. [`C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md`](file:///C:/Dev/JARVIS/kb/dev/look-at-your-own-output.md)
   §4i — *an isolated run is a different, weaker experiment than the suite*. Here the isolated
   run is the one that **passes**, so *"it passes alone"* must not be accepted as clearing it.

## Task

**Make the full instrumented suite trustworthy as a gate**, so a red run means a regression.

Two different tests failed on two different full runs and each passed alone:
`NotificationObservedFireTest.thePlanTomorrowNotificationReallyAppearsInTheShade` and
`AiSectionUiTest.replacingTheKeyChangesTheMaskedTail`. That is order- or state-dependence
between tests, not a defect in either test's subject.

**The hypothesis to test first, because it is concrete and cheap:** `performTextInput` raises
the soft keyboard, the following `performClick()` on the save button is consumed dismissing it,
and whether the IME is up depends on what the *previous* test left behind. `Inferred:` — the
run was not instrumented to prove the click was swallowed. **Prove or kill it before fixing
anything**; a fix applied to an unconfirmed cause is indistinguishable from a fix that changed
the timing by accident.

## Carries over

- **Fix the class, not the two tests.** Patching the two suites that happened to show it leaves
  every other `performTextInput`-then-click pair carrying the same landmine. The ticket's
  option 2 (a shared test helper that closes the keyboard) and option 3 (disable the emulator
  IME for instrumented runs) are both class-level; option 3 makes the failure
  **unrepresentable** rather than handled, which is the stronger shape — at the cost of an AVD
  setting that persists and affects every other session on that device. **If you take option 3,
  say so loudly in the changelog and the board note**, because the next session inherits it.
- ⚠️ **`NotificationObservedFireTest` almost certainly has a DIFFERENT cause, and the ticket's
  option 4 is withdrawn.** Both facts come from that file's own KDoc, committed in `99d3e31`
  25 hours before the ticket was opened and not read when it was written. See the correction
  comment on `#58`; in short: the suite **deliberately** leaves notifications posted so a human
  can read the shade with `dumpsys` afterwards, so `cancelAll()` in `@Before` would destroy the
  property it exists to provide — and the neighbouring test already documents a **second**
  order-dependence, the system's own `AUTOGROUP_SUMMARY` record, which *"fails only in the run
  orders that leave both posted."* **Two causes, not one.** Read that KDoc before reproducing
  anything.
- **The GitHub Actions emulator workflow inherits this.**
  `.github/workflows/instrumented-tests.yml` runs the same suite, so whatever fixes it locally
  has to hold there — and CI is the surface where *"just re-run it"* is not available.
- **This is the first run of `#55`'s corrected `docs/OPERATIONS.md` deploy wording** if any
  Firebase action comes up. It almost certainly will not; noted so the record is complete.

## Out of scope

- **Anything in `#56`'s or `#57`'s working set.** If a fix wants `Task.kt`, stop and re-read
  the brief — it does not.
- **Rewriting either failing test's assertions.** They assert correct things. The suite is what
  is wrong.
- **Hunting other flakes speculatively.** Two instances are recorded; find their cause. A
  broader audit is a different ticket.

## Exit

- **The cause is named and evidenced**, not inferred — a log, a screenshot, or a deterministic
  reproduction showing the click was swallowed (or showing it was something else). **Two causes
  are expected**, one per failing test; a fix that explains only one is not finished.
- **The notification suite still leaves its notifications posted** at the end of a run. That is
  the property `dumpsys` evidence collection depends on, and no ordering fix may trade it away.
- **The full suite runs green N consecutive times**, N ≥ 3, via `install -r` +
  `am instrument`. *One* green run is exactly the weak experiment that produced this ticket, so
  it is not an exit.
- **A test that would have caught it** — or, if the fix is option 3 and no test can observe an
  AVD setting, an explicit statement of that with the reason, rather than silence.
- **`NotificationObservedFireTest` is either explained by the same cause or separately
  diagnosed** — not left as "it passed this time".
- `CHANGELOG/<today>/58-instrumented-order.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · **close `#58` with the evidence** ·
  commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Say whether the `#57` presentation briefs may now start on a trustworthy suite, and **whether
you changed any AVD or device setting they will inherit**.
