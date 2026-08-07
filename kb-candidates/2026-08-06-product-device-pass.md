# KB candidates — `product-device-pass`, 2026-08-06

Un-ingested. Normal mode, so this list is a **proposal**: nothing is ingested
until Ido approves it. Each entry stands alone — no transcript is a source.

> Note for whoever drains this: `kb-candidates/2026-08-06-product-review.md` was
> **already sitting here un-drained** when this session started, and is still
> pending. Two files, two sessions, one approval conversation.

---

## 1. A Firestore transaction is server-only, so a transaction-backed write is slow online and a silent no-op offline

**Claim** — Wrapping a write in `firestore.runTransaction` costs you the offline
cache. Unlike `set()`/`update()`, which apply locally and fire the snapshot
listener immediately, a transaction does a server-side read-then-compare-and-set
and therefore cannot be served from the local cache at all. The consequence shows
up twice and looks like two unrelated bugs: **online** the UI waits a full server
round-trip before anything renders (~2 s measured on a healthy connection), and
**offline** the action does *nothing whatsoever* — not even the optimistic local
write an ordinary set would have given. If the caller also discards the returned
result, the offline case is completely silent.

So: reach for a transaction only when you genuinely need atomic
read-then-write across documents, and when you do, **budget for a UI that must
fake the result locally and reconcile afterwards** — a transaction is not a write
you can render optimistically for free.

**Why** — The concrete case: `TaskRepositoryImpl.setDone` runs a transaction
because ticking a task must update task, user points, user level and goal progress
consistently — which is a *correct* reason to want one. The cost was simply never
priced. `GoalDetailViewModel.toggleTask` compounds it by launching the call and
throwing the `Resource` away, so `Resource.Error` reaches nobody. Rejected: filing
the latency and the offline no-op as two issues — they are one defect, and fixing
only the latency (add an optimistic update) converts a silent no-op into a silent
**lie**, where the box ticks and the write never lands. Also rejected: assuming the
2 s was rendering or animation cost; the offline control experiment is what proved
it was the network.

**Destination** — central KB, `kb/dev/` — a Firebase/Android claim that
generalises past GoalPilot. Adjacent to whatever holds Firestore offline-behaviour
knowledge; cross-link from `knowledge/` in this repo if a page there covers the
data layer.

**Anchors** —
`app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt`
(`setDone`),
`app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailViewModel.kt`
(`toggleTask`, and `logProgress` directly below it as the contrasting shape that
*does* handle its result),
`CHANGELOG/2026-08-06/product-device-pass.md`,
GitHub issue #3.

**Supersedes** — nothing.

**Status** — pending Ido's approval.

---

## 2. Reproduce UI defects with the accessibility tree and a variable-rate screen recording, not with screenshots and a stopwatch

**Claim** — A device repro pass has two cheap instruments that turn opinions into
evidence, and neither is a screenshot:

1. **`uiautomator dump` for "X does not respond".** A screenshot can only show
   that nothing *appeared* to change; the accessibility tree shows **which nodes
   are `clickable`**, so "the row is not a link, only the Edit and Delete icons
   are" becomes a fact about the widget tree rather than an inference from a
   non-event. The same dump measures every tap target against the 48 dp minimum
   mechanically (48 dp = 144 px at 480 dpi), which finds the 38 dp control that
   eyeballing never will. It also reveals missing `contentDescription`s in passing.
2. **`adb shell screenrecord` as a latency profiler.** It writes a
   **variable-frame-rate** file — a frame is emitted only when the screen actually
   changes. So `ffprobe -show_entries frame=pts_time` gives a list whose **gaps are
   literal dead screen**, and the boundary between "the ripple animation" and "the
   state finally changed" is readable to ~15 ms without any instrumentation in the
   app.

And a third, for "did anything change at all": compare the two frames with
`ffmpeg … psnr` over a crop that excludes the status-bar clock. `MSE 0.00` is a
much stronger claim than "it looked the same to me".

**Why** — This pass had to convert five reported symptoms into verdicts a fix
session could act on, against a standing rule in the repo that a backlog premise
can be stale. Rejected: driving the app and describing what was seen, which is
what "reproduced" usually means and is exactly what cannot survive being read six
weeks later by someone else. Rejected: an instrumented (Espresso) test per defect —
far more work, and it proves the app's behaviour under a test harness rather than
under the user's finger. The two instruments above cost about a minute each and
produced numbers: 2.24 s / 1.94 s, 57 of 58 tap targets passing, PSNR ∞.

Caveat that belongs on the page: this measures **an emulator on a developer
machine against live Firestore**, so the absolute latency is indicative, not a
budget. What it establishes is the *shape* — where the dead time is and what ends
it.

**Destination** — central KB, `kb/dev/` — a testing/verification method,
Android-specific but project-agnostic. Adjacent to any existing page on verifying
behaviour on a device.

**Anchors** — `CHANGELOG/2026-08-06/product-device-pass.md` (the `## 🧪 Tests`
section states the method and its limits),
`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md` (`D2`, `D3`, `D4`, `D5`, `A8`),
GitHub issues #2, #3, #4, #5.

**Supersedes** — nothing. Complements the repo's existing testing-layer doctrine
(`.github/instructions/testing.instructions.md`) rather than replacing it: this is
what to do when the thing under test is a *reported symptom* rather than a change
you just wrote.

**Status** — pending Ido's approval.
