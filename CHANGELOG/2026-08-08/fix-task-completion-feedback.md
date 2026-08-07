# 2026-08-08 — `fix-task-completion-feedback`

Opened with `/kickoff fix-task-completion-feedback` from the brief
`product-device-pass` wrote on 2026-08-07. Closes
[#3](https://github.com/idomarhaim/Android_Final_Project/issues/3) — completing a
task was **~2 s of dead screen online and a silent no-op offline**.

Both halves had to land together, which is why they were one issue: an optimistic
tick *on its own* turns the offline no-op into a **silent lie** — box ticked,
points raised, write never landed.

## The decision the brief demanded be taken deliberately

`TaskRepositoryImpl.setDone` is a `firestore.runTransaction`, and a transaction is
**server-only** — it never touches the offline cache, which is why there was no
local write to render and the screen sat still for the whole round trip.

**Ido's call: keep the transaction.** It is what holds four things in agreement —
`task.done`, `user.points`, the `user.level` derived from them, and the clamped
`goal.currentValue`. The usual swap (a batched write with `FieldValue.increment`)
does not relocate that guarantee, it deletes it: `increment` can express **neither
clamp** (`points >= 0`, `currentValue <= targetValue`) **nor a level derived from
points** — and `level` is projected onto `publicProfiles`, which the leaderboard
reads. So the fix went into the UI, and the architecturally better answer was
filed rather than smuggled in (see #34 below).

## What changed

- **`GoalDetailViewModel`** — a `_pendingToggles` overlay draws the completion
  immediately and the `uiState` transform applies it to both the task list *and*
  the goal's progress, using the same arithmetic and the same clamp the
  transaction uses, so the ring never shows a number the server will contradict.
  The list re-sorts on the repository's own key, so the row settles into its final
  position as it is ticked rather than jumping again two seconds later.
- **The overlay retires against observed data, not against the write's return.**
  The transaction's completion callback and the snapshot reflecting it arrive on
  two different channels; clearing the overlay on completion would re-render the
  old state for the frames in between — a flicker on every successful tap. An
  entry is dropped only once the snapshot listener agrees with it, which cannot
  flicker by construction.
- **A failure undoes the tick and says so** — this is the half that stops the
  optimistic update becoming a lie.
- **`core/net/ConnectivityMonitor`** *(new)* — an offline tap is now **refused up
  front** instead of being drawn and taken back. This was an escalation the plan
  made conditional on measurement, and the measurement demanded it: see below.
- **The sweep.** The brief said to sweep, not patch one call site. The audit found
  the discarded-`Resource` shape at **five** sites, not the two #3 names:
  `toggleTask`, `deleteTask`, `addTask`, `archiveGoal`, and
  `SocialViewModel.removeFriend` — which was worse than silent, announcing
  *"Friend removed"* before looking at the result, so a failed removal claimed
  success while the row stayed on screen contradicting it. Every other ViewModel
  in the app already handled its `Resource`; `ProfileViewModel.signOut` discards
  legitimately, since the root auth-state flow is its feedback.

## Measured on a device, before and after

Same technique as `product-device-pass`: `screenrecord` is variable-frame-rate, so
it emits a frame only when the screen actually changes — a gap in
`ffprobe -show_entries frame=pts_time` is literal stillness, not sampling error.
`Pixel_10_Pro_XL`, real build, Ido's live data.

### Online

| | before (#3) | after |
|---|---|---|
| tap → checkbox changes | **2.24 s** | **first frame after the tap** |
| tap → checkbox fully drawn | 2.24 s | **0.178 s** (Material's own check animation) |
| dead screen after the ripple | **1.20 s** | **none** |
| donut / `n / 100 %` | moved at 2.24 s, with the box | moves in the **same frame as the tap** (`1%` → `2%`) |

The server confirmation lands ~2.5 s later and changes **nothing visible** except
the progress ring's indicator dot — no flicker, no jump, no double-count. That is
the retirement logic working.

### Offline — and why the pre-check was added

The first offline build did exactly what it was designed to do, and the numbers
said it was not enough:

| | first build (undo only) | after the pre-check |
|---|---|---|
| tap → optimistic tick | immediate | **never — the tap is refused** |
| tap → the lie is corrected | **7.9 s** | n/a, nothing was faked |
| tap → user is told | 7.9 s | **0.19 s** |

**7.9 seconds** is how long Firestore spends resolving DNS and retrying before it
reports `UNAVAILABLE`. The undo worked, the snackbar appeared, the state came back
correct — but eight seconds of a ticked box over a write that will never land is a
lie the correction only eventually catches. So the conditional escalation in the
plan fired, and `ConnectivityMonitor` now refuses the tap outright.

The undo path **stays** behind it: the pre-check proves there is a validated
network, not that Firestore answered. A captive portal or an unreachable backend
still gets past it.

### A second defect the measurement exposed

The undo's snackbar read **`UNAVAILABLE: Unable to resolve host
firestore.googleapis.com`** — the raw gRPC string, straight from the repository,
in front of the user. Passing repository text through is right where it is
actionable (*"no user with that code"*) and wrong for a transport stack trace. The
toggle now shows *"Couldn't save that — check your connection"* and the detail
stays in logcat, with a test asserting the words `UNAVAILABLE` and `googleapis`
never reach the UI.

## 🧪 Tests

| layer | result |
|---|---|
| JVM unit (`:app:testDebugUnitTest`) | **213 passed, 0 failed** — 197 baseline + **16 new** in `GoalDetailViewModelTest` |
| Instrumented (`:app:connectedDebugAndroidTest`) | **29 passed, 0 failed** on `Pixel_10_Pro_XL` — run despite no composable changing, because `core/net/ConnectivityModule` is a **new Hilt module** and could have broken the `HiltTestRunner` graph. It did not. |
| Security rules (`firestore-tests/`) | **not run, not applicable** — `firestore.rules` untouched |
| Cloud Functions | **not run, not applicable** — `functions/` untouched |

`GoalDetailViewModelTest` is new and covers both halves deliberately, because
shipping either alone is a regression: the optimistic tick, the goal progress
travelling with it, the clamp at target, the undo, the undo taking the goal
progress with it, the anti-flicker hold, the retirement without double-counting,
the offline refusal never reaching the repository, the raw-gRPC-text guard, and
the four swept call sites.

Behavioural verification was the device pass above — frame-timed recordings, not
an eyeball.

## Live data

`goalpilot-56e30` **was** written to: the online measurement really completed
*"לשתות 4 ליטר מים ביום"* (+20 pts) on the *"Drink 4 Liters of Water Daily"* goal.
**Restored and verified** — task unticked, goal back to `1 / 100 %`, dashboard back
to `70 pts`, Level 1, 7 goals, 5 tasks done, 24 % overall: exactly as found.

## Filed rather than smuggled in

[#34](https://github.com/idomarhaim/Android_Final_Project/issues/34) — *task
completion should write one field and let a Cloud Function own the derived state*.
The client would write `task.done` only (an ordinary `update()`, so instant,
offline-capable and queued), and a Firestore-triggered function would recompute
points, level and goal progress server-side. That is genuinely better — real
offline completion, and derived state with exactly one writer — but it is a new
backend trigger, a deploy and a leaderboard re-verification, so it is its own
session. The reasoning, including why the batched-write swap was rejected, is in
the issue rather than only here.

## Notes for whoever is next

- **A cold, cacheless start still shows a near-empty dashboard** (`A10`, found by
  `product-device-pass`). Restarting the app during this session reproduced it
  incidentally. Untouched — not this issue.
- The Windows KSP lock bit once (`Could not delete .../ksp/debug/classes`);
  `rm -rf app/build/generated/ksp` and re-run, exactly as `CLAUDE.md` says.
