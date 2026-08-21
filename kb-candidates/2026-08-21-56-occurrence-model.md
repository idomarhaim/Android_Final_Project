# KB candidates — `56-occurrence-model`, 2026-08-21

Session `56-occurrence-model` · ticket [#56](https://github.com/idomarhaim/Android_Final_Project/issues/56) ·
mode **AUTO** · account: `CHANGELOG/2026-08-21/56-occurrence-model.md`

---

## 1 · A posted notification is not readable on the next line — and the flake lands on a *different* test each run

- **Claim.** `NotificationManagerCompat.notify` (and `cancel`) return before
  `NotificationManager.activeNotifications` reflects them: they are binder hops to
  `NotificationManagerService`. Any instrumented assertion that posts and then reads the shade is a
  race. **Wait for the condition being asserted, bounded** — for a post, until the tag appears; for
  a cancel, until it *disappears*, which is not the same wait and is the one that gets missed.
- **Why.** `Observed:` 2026-08-21, `emulator-5554`, three consecutive full-suite runs, each failing
  a **different** case: `twoRemindersForTwoTasksBothStayUp` (post not yet visible), then
  `cancellingOneTasksReminderLeavesTheOthersUp` — where the first fix was applied but waited on the
  *surviving* tag, which was already present, so the poll returned instantly and still caught the
  cancelled one on its way out — then `theFilingNotificationReallyAppearsInTheShade`, a case
  **unchanged since `#8`** that had been green for a day. Every one passed in isolation (verified
  `3/3` on the first).
  **Two things make this worth a page.** (a) The signature is *"the suite is order-dependent"*,
  which invites a hypothesis about test **ordering** — this repo already had one filed as `#58`,
  blaming an IME — when the actual cause is asynchrony that simply becomes likelier as the shade
  fills. Adding four notifications to the app made a year-old latent race start firing.
  (b) **A bounded wait is not a weakened assertion**, and the instinct to reject it as one is what
  leaves the race in: the assertion still runs, still fails when the thing genuinely never happens,
  and only the false red goes away. An `assumeTrue` *would* be the weakening move.
  *Rejected:* a fixed `Thread.sleep` before each read — it is slower on every run and still wrong
  on a slow one.
- **Destination.** `kb/dev/android-device-verification.md` — a new section, beside §8's
  `install -r` finding.
- **Anchors.** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/NotificationObservedFireTest.kt`
  (`awaitPosted`, `awaitReminderTags`, `awaitReminderTagsGone`) ·
  `CHANGELOG/2026-08-21/56-occurrence-model.md` §3.
- **Supersedes.** Nothing. Adjacent to `#58`, which it does **not** close: the IME hypothesis there
  is a separate claim and was not tested.
- **Status.** Ready.

---

## 2 · A discriminating test needs its two inputs on different sides of the thing that discriminates

- **Claim.** When a test exists to prove that **X, not Y, drives an outcome**, the two inputs must
  produce **different outcomes**. Where the function has a clamp, a floor, a ceiling or a default,
  two different inputs routinely land in the same saturated region — and the test then passes
  whichever input wins, which is the one thing it was written to detect. Assert the difference
  explicitly (`assertThat(a).isNotEqualTo(b)`), so the vacuity fails rather than hides.
- **Why.** `Observed:` 2026-08-21. A test for `#9`'s sticky typed duration used the spec's own
  worked example — a deadline at 06:00, typed 240 minutes vs an estimated 30. Both clamp into the
  same waking-hours boundary (`22:59` the previous evening), because `06:00 − 30 min = 05:30` is
  *also* inside the user's sleep. The test passed and proved nothing. Moving the deadline to 09:00
  put the two on opposite sides of sleep (previous evening vs `08:30`) and made it real.
  **The trap is that the example came from the specification**, which is exactly where a careful
  author looks for test data — the spec's example is chosen to illustrate the *feature*, not to
  discriminate between two implementations of it. This is the general form of
  `look-at-your-own-output.md`'s *"check the instrument on the hardest input it exists for"*,
  arriving one layer up: here the instrument is the test, and its hardest input is the one where
  the wrong answer would look identical to the right one.
- **Destination.** `kb/dev/mechanism-vs-compliance.md` — a new section; it is the same family as
  §9's cross-language fixture.
- **Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/domain/OccurrenceRemindersTest.kt`
  (`a typed duration drives the reminder…`, which carries the vacuity as an inline warning).
- **Supersedes.** Nothing.
- **Status.** Ready.

---

## 3 · A negative assertion needs a positive control **in the same run**

- **Claim.** *"X was not posted / not shown / not called"* is trivially true on a broken instrument,
  and stays green for years while proving nothing. Make the same run **do the positive thing with
  an unrelated subject first, and assert it worked** — then the absence of X is evidence rather
  than a coincidence of a dead channel, a missing permission or a broken collaborator.
- **Why.** `Observed:` 2026-08-21, building §2.5's *"never as a push saying he failed"*. The
  obvious test — show the review, assert the notification shade holds nothing — passes identically
  on a device where `POST_NOTIFICATIONS` was never granted, where the channel is blocked, or where
  the notifier throws. All three are states this app's own test suite has been in. The fix is
  three lines: post a real reminder for a control task, assert it is in the shade, *then* assert
  the reviewed tasks are not.
  This generalises past notifications to every *"and it must not also do Y"* clause — the shape
  where a spec forbids something, which is precisely where the test is hardest to make honest.
  *Rejected:* asserting the notifier's `canPost()` instead. It answers a different question (may
  it) from the one at issue (did the shade receive something), and the gap between them is where
  the defect would live.
- **Destination.** `kb/dev/mechanism-vs-compliance.md`, beside entry 2 — same page, same family.
- **Anchors.** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/DailyMissReviewUiTest.kt`
  (`theReviewShowsOnScreenAndNeverInTheShade`, and the class KDoc's *"non-vacuous on purpose"*).
- **Supersedes.** Nothing.
- **Status.** Ready.

---

## 4 · A specification's vocabulary can be incomplete for its own normative table, and only enumeration finds it

- **Claim.** When one section of a spec gives a **table of cases** and another gives the
  **vocabulary** those cases resolve into, write every case out and map it. The mismatch does not
  show up when reading either section: each is internally coherent, and the reader supplies the
  missing name without noticing. Resolve toward the section that defines **meaning**, and record
  the resolution in the spec rather than only in code.
- **Why.** `Observed:` 2026-08-21, `docs/PRODUCT_v0.3.md`. §2.2 tabulates **four** rungs
  *discriminated by what a miss means*; §2.3 lists the derived states and names **two** of those
  four meanings (`MISSED` for a block, `OVERDUE` for a deadline) plus `EXPIRED`. *The day passed*
  and *the window closed* have no name. Nothing in either section reads as wrong, and the obvious
  implementation — fold both into `MISSED` — would have marked them **failures**, which §2.3
  explicitly reserves for `MISSED` alone. The defect would then have surfaced as the user being
  told they failed at things the spec never called failures.
  It was found by writing the four test cases, not by reading. That is the actionable part: a
  four-row table is a four-case enumeration, and the enumeration is cheap.
- **Destination.** `kb/dev/` — a page on reading specs by enumeration; possibly a section on an
  existing spec-reading page if one exists (check `kb/index.md` before creating).
- **Anchors.** `docs/PRODUCT_v0.3.md` §2.2 (the ✅/⚠️ note) ·
  `app/src/main/java/com/idomarhaim/goalpilot/domain/model/Occurrence.kt` (`OccurrenceState` KDoc) ·
  `app/src/test/java/com/idomarhaim/goalpilot/domain/model/OccurrenceTest.kt`
  (`the four rungs produce four different miss meanings`).
- **Supersedes.** Nothing.
- **Status.** Ready.

---

## 5 · A date is not an instant, and storing one as epoch millis moves it between time zones

- **Claim.** A domain value that means **a day** (an all-day commitment, a date range, a birthday)
  must be stored as ISO-8601 **local text**, never as epoch millis. Millis is an instant; *which
  day it is* then depends on the reading device's zone, so the same document reads as a different
  day after a flight. The discriminator is the question the field answers: *when did this happen*
  is an instant, *which day is this for* is not.
- **Why.** `Observed:` 2026-08-21 by design rather than by defect, while giving §2.2's `ALL_DAY`
  and `SPAN` rungs a wire form in a codebase where **every** other temporal field is `Long` millis
  (`createdAt`, `completedAt`, `deadline`). Following the house convention would have been the
  natural move and would have put the bug in: all temporal state is derived from that value at read
  time, so an all-day task's *miss* would move with the reader's zone.
  The same boundary reappeared immediately in the UI: Material's `DatePicker` returns **UTC
  midnight** for the tapped day by documented contract, and reading it back with the system zone
  lands on the previous day east of UTC — a task filed for Friday quietly becoming Thursday.
  *Rejected:* storing millis plus a zone id. It makes the document self-consistent and still lets
  two readers disagree about what to render, which is the second answer the whole codebase's §0.3
  rule is against.
- **Destination.** `kb/dev/` — a page or section on date-vs-instant modelling. Check `kb/index.md`
  for an existing temporal-modelling page first.
- **Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Dtos.kt`
  (`TaskDto.occurrenceStart` KDoc) · `app/src/main/java/com/idomarhaim/goalpilot/feature/goals/WhenPicker.kt`
  (`toUtcLocalDate`) · `app/src/test/java/com/idomarhaim/goalpilot/data/TaskOccurrenceMappingTest.kt`.
- **Supersedes.** Nothing.
- **Status.** Ready.
