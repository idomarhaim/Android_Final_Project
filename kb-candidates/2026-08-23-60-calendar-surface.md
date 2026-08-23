# KB candidates — `60-calendar-surface` (2026-08-23)

Session `60-calendar-surface`, ticket `#60`. Written per `rules/memory-promotion.md`; each entry
stands alone, because a transcript is machine-local and is missing exactly when it is reached for.

---

## 1 · A render pass catches a class of defect no assertion can: **layout agreement between siblings**

- **Claim.** When several instances of one widget are drawn side by side and are meant to share a
  coordinate system — calendar columns sharing hour lines, bars sharing a baseline, cards sharing a
  fold — **every per-instance assertion can pass while the shared frame is broken**, because each
  instance is individually correct. The check that finds it is *looking at the picture*; the fix is
  usually a layout that makes the shared frame explicit rather than emergent (`IntrinsicSize.Max`
  bands, a shared measure, a constraint set), so the agreement is structural rather than incidental.
- **Why.** `Observed:` 2026-08-23, GoalPilot `#60`. `CalendarSurfaceUiTest` was **14/14 green** and
  every node was present and `assertIsDisplayed()`; the first `issue-60-calendar-3day.png` showed
  **Monday's 07:00 sitting below Tuesday's 09:00**, because each day was its own `Column` and a day
  carrying four banners pushed its own grid four rows down. An hour grid whose rows do not align is
  not a grid — and comparing two days is the only thing a multi-day calendar is for, so the *whole
  feature* was broken while every test agreed it worked.
  **Rejected:** *"assert the y-coordinates match"* — a semantics-tree assertion on positions can be
  written, and it would have caught this one, but it is a test per pair of things that ought to
  agree and nobody writes it before seeing the failure. The general instrument is the picture.
  This is the *positive* case for `look-at-your-own-output.md`'s visual clause: not a nicety, but
  the only affordable detector for a defect class that is invisible node by node.
- **Destination.** `kb/dev/look-at-your-own-output.md` — extend the visual-verification section with
  *the sibling-agreement class*.
- **Anchors.** `look-at-your-own-output.md` §"render it and look".
- **Supersedes.** Nothing. Extends.
- **Status.** ready

---

## 2 · `LocalTime.plusMinutes` wraps silently, and downstream coercion turns the wrap into a *zero*

- **Claim.** Arithmetic on a **time-of-day** type wraps at midnight with no error, so
  `LocalTime.of(23,45).plusMinutes(30)` is `00:15`. Combined with a domain type that **coerces an
  inverted range rather than rejecting it** — which is a good rule on its own — the two produce a
  *silently empty* object: a block that reads perfectly in the debugger and cannot be seen on
  screen. Whenever a wall-clock time is advanced and then paired with a **date**, decide explicitly
  whether the result is the same day or the next one.
- **Why.** `Observed:` 2026-08-23, GoalPilot `#60`, `SlotDraft`. A tap on the 23:45 slot produced
  `Block(start = Mon 23:45, end = Mon 00:15)`; `Block.closesAt` coerces `end < start` to `start`
  (correct, deliberate, KDoc'd), giving a zero-length block. Found by *writing a test for the last
  hour of the day*, not by reading — the code reads correctly at every line. Fix was an explicit
  `crossesMidnight` and `Math.floorMod` duration.
  The general shape: **a defensive coercion downstream converts a wrap into a plausible value**, so
  neither layer reports anything. Both layers are individually right.
  **Rejected:** forbidding an end before a start in the UI — that makes a night block (23:00–01:00)
  impossible to type, deleting a legitimate shape instead of correcting a typo.
- **Destination.** `kb/dev/` — new page, `time-of-day-arithmetic-wraps.md`, or a section on an
  existing date/time page if one exists.
- **Anchors.** none.
- **Supersedes.** Nothing.
- **Status.** ready

---

## 3 · A spec sentence that states a *measurement* is a layout instruction, and it is routinely read one level too shallow

- **Claim.** When a spec says *"X does not fit at N dp, so do Y"*, `Y` is a statement about **what
  the space is for**, not a styling hint to apply inside the old layout. Implementing `Y` as a
  variant of the existing arrangement reproduces the measured failure exactly, and the render pass
  then shows the very symptom the spec quoted.
- **Why.** `Observed:` 2026-08-23, GoalPilot `#60`. §4.3: *"Seven columns on a 390 dp phone is ~46 dp
  per day — no Hebrew title and no time range survives it. Week view stacks the times **start over
  end**, the only thing that fits at 46 dp."* First implementation stacked the times **inside a
  narrow leading column beside the title** — i.e. kept the three-day arrangement and stacked within
  it. `issue-60-calendar-week.png` came back with every time clipped to `07:0`, `09:3`, `12:3` and
  every title an ellipsis: the measured failure, reproduced. The correct reading is *"the stacked
  time is what the column is for"* — a separate chip layout for that zoom, title beneath, tick
  dropped.
  Worth carrying because the spec had **already predicted the exact rendering defect** and it still
  shipped once: the sentence was read as a property of the times rather than as a budget for the
  column.
- **Destination.** `kb/dev/` — the page on reading specs / turning decisions into code, if one
  exists; otherwise a section on `look-at-your-own-output.md` beside entry 1, since the *detector*
  was the same render pass.
- **Anchors.** none.
- **Supersedes.** Nothing.
- **Status.** ready

---

## 4 · `BUILD SUCCESSFUL` + a green device run can be **the previous APK**, and `${PIPESTATUS[0]}` does not save you

- **Claim.** `CLAUDE.md` already records that a Gradle failure hidden behind a pipe lets `adb install
  -r` re-install the *previous* APK and report the last build's results. The addition: **reading the
  exit code is necessary and not sufficient** — the failure survives when the exit code is read
  **and then the green run is believed anyway**, because a passing suite is powerful evidence that
  overrides a number seen thirty seconds earlier. Make the install *conditional on the build* in one
  command (`build && install && run`), so no human judgement sits between them.
- **Why.** `Observed:` 2026-08-23, GoalPilot `#60`, twice. `assembleDebug` failed with the Windows
  KSP lock (`Could not delete …/generated/ksp/debug/resources`); `BUILD=1` was printed and read; the
  instrumented suite then reported **14/14 green** — for the build *before*, since the APK was still
  at the output path. The second occurrence was caught only because the first had happened.
  The generalisation: a **stale artefact at a well-known path** turns a build failure into a
  *silently passing* verification, and the strength of the false signal is what defeats the correct
  one.
- **Destination.** `kb/dev/android-device-verification.md` (§ on instrumented runs) with a
  cross-reference from `look-at-your-own-output.md`.
- **Anchors.** `android-device-verification.md` §8.
- **Supersedes.** Nothing — sharpens the existing `${PIPESTATUS[0]}` note in this repo's `CLAUDE.md`.
- **Status.** ready

---

## 5 · A cross-cutting sweep written by an earlier session caught a defect this one could not have seen

- **Claim.** `ImeSettleSweepTest` — a **JVM** test that greps `androidTest/` sources for a banned
  call — failed the build on two call sites in a brand-new instrumented test that was **passing**.
  The pattern generalises: for a hazard whose symptom is *intermittent and lands somewhere else*, a
  source-level sweep in a cheap layer is a better guard than any assertion in the expensive layer,
  because it fires on the **shape** rather than on the failure.
- **Why.** `Observed:` 2026-08-23, GoalPilot `#60`. `CalendarSurfaceUiTest` used raw
  `performTextInput` then clicked Save inside a `ModalBottomSheet`; it went green 5 runs out of 5.
  `#58`'s sweep flagged both lines with the reason: focusing a field raises the soft keyboard, whose
  window-inset animation moves the sheet while Compose reports itself **idle**, so the next click is
  aimed at where the target used to be and is silently lost — `#58` measured **1 full-suite run in
  4**. A new session could not have discovered this; it inherited it for free.
- **Destination.** `kb/dev/` — the testing-strategy page, as the *sweep-in-a-cheap-layer* pattern.
- **Anchors.** none.
- **Supersedes.** Nothing.
- **Status.** ready
