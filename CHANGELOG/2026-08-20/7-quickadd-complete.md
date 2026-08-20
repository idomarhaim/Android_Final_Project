# `7-quickadd-complete` — 2026-08-20

> **Summary:** `#7` ships. `R6`'s *"a way to complete the task from within quick add"* is an **Already done** chip on **both** add surfaces, and the completion rides the task's **own single `set()`** — never `upsertTask` then `setDone`, which is two writes and a window in which the task exists un-completed. The rule that makes a born-done task legal is `TaskCompletion`, applied inside `upsertTask` rather than at the two call sites, because that is the one function every task write passes through. It exists because `isDone` and `completedAtEpochMillis` are **one fact read by consumers that disagree about which field is the fact**: the projection function counts `done`, while the weekly summary, the dashboard's done-this-week count and the time chart all require the stamp — so a half-written fact **awards points and is invisible everywhere the user could check**. The chip **clears after every add**, deliberately, because a toggle that survived would be a mode that silently completes the next task typed. JVM unit **515/0** (+9), instrumented **139/0** (+18), functions `node --test` **37/0** (unchanged), `assembleDebug` green, render pass looked at on two frames. **Two findings beyond the ticket:** the brief's *corrected* precondition grep is **itself a false negative**, for a reason that generalises; and **the §1.4 points inversion that this ticket and `#9` both defer to "`C1` #19" has no implementation owner** — #19 is a *decision* ticket, closed 2026-08-10, and nothing open carries the build.

**Session:** `7-quickadd-complete` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Brief:** [`sessions/7-quickadd-complete.md`](../../sessions/7-quickadd-complete.md) · **Issue:** [#7](https://github.com/idomarhaim/Android_Final_Project/issues/7)

---

## The precondition, and why checking it twice was not wasted

The brief carries a precondition — `#3`'s connectivity pre-check and the multi-document
transaction in `setDone` must be gone — plus a **correction** to it from `50-finish`, which had
run the original and found it false:

| Attempt | Check | Result |
|---|---|---|
| The brief's own | `grep -c runTransaction …/TaskRepositoryImpl.kt` must be `0` | returns **3** — all prose |
| `50-finish`'s correction | `grep -n "firestore.runTransaction" …` must be empty | returns **line 31** — also prose |

Line 31 is a KDoc line reading `` * writes in one `firestore.runTransaction`: the tick, … ``.
**The correction inherits the exact defect it was written to fix.**

The precondition is nevertheless **met**, established by reading the code rather than grepping
it: `setDone` is one `tasksCol(uid).document(taskId).update(mapOf(…))`, and no transaction
exists in that file in any form.

**The general shape is worth more than the instance.** A grep over a source file cannot
distinguish code from the comment that explains why the code is gone — and a removal
well-documented enough to be safe is precisely the removal that defeats a grep testing for its
absence. **Comment density is correlated with the thing being absent**, so this failure mode
gets *more* likely the better the removal was documented. `TaskRepositoryImpl.kt` narrates the
deleted transaction across three separate passages exactly *because* `C20` removed it.

Both greps fail in the **alarming** direction (a false *positive* for the transaction, i.e. a
false negative on the precondition), so nothing was at risk here beyond a session concluding it
was blocked when it was not. That is the same family as `kb/dev/decision-map-charting.md` §12b,
which the brief already cites — a mechanical check frozen at authoring time.

---

## What shipped

### 1 · `TaskCompletion` — the invariant, in the domain

`domain/model/TaskCompletion.kt`, new. One rule:

> **`isDone` and `completedAtEpochMillis` are one fact, and they are written together or not at
> all.**

```kotlin
fun stamp(task: Task, nowMillis: Long): Task = when {
    !task.isDone                        -> task without any completedAt
    task.completedAtEpochMillis != null -> task, untouched            // never re-dated
    else                                -> task stamped with nowMillis // #7's born-done task
}
```

**Why it needed stating at all.** Until `#7` the only writer of `done` was `setDone`, which
writes both fields in one `update` and nulls the stamp on an untick — `Observed:` back to the
initial commit `80ba07f`, where the line is already
`mapOf("done" to done, "completedAt" to if (done) now else null)`. So the invariant held by
having exactly **one writer**, and nothing anywhere stated it. `#7` adds the second writer.

**What a half-written fact actually does.** Not "a field is missing" — the four readers
disagree about which field *is* the fact:

| Reader | Reads | A done task with no stamp |
|---|---|---|
| `functions/src/derived.ts` `pointsFromTasks` | `done` only | **counts** — points are awarded |
| `BuildSummaryUseCase` | `isDone && (completedAt ?: 0) >= windowStart` | silently **dropped** |
| `DashboardViewModel` done-this-week | same shape | silently **dropped** |
| `TimeAllocationUseCase` | `isDone && completedAt != null`, then `!!` | silently **dropped** |

So the points move and **the task that moved them is invisible in every place the user could go
and reconcile them** — no error, nothing red, a total that cannot be checked against anything on
screen. `Observed:` by reading those four call sites at `HEAD`, 2026-08-20 — **not** by hitting
it, because `#7` is the first ticket that could.

**Applied in `TaskRepositoryImpl.upsertTask`**, not at the two add surfaces. That function is
the single choke point every task write passes through, and it already normalises
`createdAtEpochMillis` the same way, so this is its existing habit rather than a new
responsibility. Applying it at the call sites would be correct today and quietly wrong at the
third add surface somebody builds — which is where invariants go to be forgotten.

**The third branch is the one that had to be spelled out.** `!isDone` **clears** the stamp
rather than preserving it: there is no completion, so there is no time at which it happened.
That mirrors `setDone(false)`, so after this the create path and the tick path produce the same
shape — which is what makes the four readers above safe to trust. Checked against every existing
`upsertTask` caller first: `AnalyticsViewModel`'s duration backfill re-saves **completed** tasks
routinely and must not be re-dated (it is the second branch), and the Google Tasks import
creates open tasks with no stamp.

### 2 · The affordance, on both add surfaces

`SmartAddCard` (dashboard quick add) and `AddTaskRow` (goal detail) each gain an **Already
done** `FilterChip`.

**A chip under the input row, not a third control inside it.** The quick-add row is already a
text field that has to stay wide enough to read plus a button; a third item makes the field too
narrow on a phone to see what is being typed. In `AddTaskRow` it gets its own row rather than
sitting beside the duration box, whose caption already runs to *"Not set — counts as 30m"* and
would ellipsise away the half that says what will be stored — which is the whole of `#9`.
§0.8's surviving sub-rule (*form and words before iconography*) is also why it cannot shrink to
a bare tick to fit.

**It resets on every add.** This costs one extra tap per already-done task when logging several
in a row, and it is deliberate: a toggle that stayed on is a **mode** that silently completes
the *next* task typed — the app doing something unasked and unannounced, which §0.7 does not
permit even for filing, let alone for asserting that work was done.

**Because it resets on the tap that starts the classify**, `SmartAddState` carries
`alreadyDone` and the in-flight row says *"Filing “…” as done…"*. Without it there is a network
round trip during which nothing on screen agrees that a completion is being written, and the
user's own action appears to vanish.

### 3 · The receipt says it

`#6`'s witness exists because §0.7 permits filing without asking **only** if the app says
afterwards what it did — and with `#7` it did two things. The filed task sits under a goal the
user is not looking at, so its tick is not on screen either; the snackbar is the only place the
dashboard can show that the completion took.

`SmartAddReceipt` gains `completed`, and `sentence()` doubles its branch table:

| Filing outcome | Ordinary | Also completed |
|---|---|---|
| existing goal | `Added to “X”` | `Done — added to “X”` |
| new goal proposed | `No goal fitted — suggested “X”` | `Done — no goal fitted, suggested “X”` |
| no goal at all | `Added “X” — no goal fits it yet` | `Done — “X” fits no goal yet` |

**Doubled rather than prefixed**, though a `"Done — "` prefix over the existing three was the
obvious move: two of the three then read wrong — *"Done — No goal fitted"* capitalises
mid-sentence, and *"Done — Added “x” — no goal fits it yet"* carries two dashes and says
**added** about a thing whose news is that it is **finished**. Six short strings cost less than
a rule with two exceptions.

`completed` is a field beside `decision`, not a fourth `FilingDecision` branch: filing and
completing are independent, every filing outcome can happen to an already-done task, and folding
it in would double a sealed hierarchy for a fact no branch of it decides. Undo needs no second
offer — deleting the task takes its `done`/`completedAt` pair with it, which is itself a
consequence of writing the fact **onto the task** rather than into a second place.

---

## The decision the brief asked for explicitly: does `AddTaskRow` get it too?

**Yes.** The brief required this be decided and reasoned rather than assumed.

- **For:** an add affordance present on one add row and absent from the other reads as a bug
  rather than as a decision. And goal detail is where somebody logs three runs they already did
  into one goal — the tap it saves is *per task*, so the case is if anything stronger there.
- **Against, and it is real:** on goal detail the task list is on screen, so the new row's
  checkbox is one tap away and `R6`'s four navigations do not arise at all.

The for-side wins because it is the **same control writing through the same seam** — one
feature with two doors, rather than two features that have to be kept in step. The render pass
below is what confirms they actually look like one feature; the assertion comparing their two
label constants cannot reach that.

---

## Deliberately not built

**§1.4's points inversion** — `points = round(minutes/3) × difficulty`, the `difficulty` enum,
the `5..50` cap deletion, points banked as rows in a `completionFacts` collection — and
**§1.5's `goalEdges`**. Neither exists in the codebase (`grep` for `goalEdges` and for a
`difficulty` enum both return nothing in `app/src/main`), and `9-duration-box` and
`11-fill-buttons` each deferred the same work.

**What `"that same fact"` therefore means here.** The ticket's §1.4 clause is a constraint about
**plumbing**, not arithmetic: *emit that same fact, not a second pipe*. Today the completion
fact **is** `done` + `completedAt` on the task document. So the clause is satisfied by the
create carrying the completion through the one write everything else already goes through —
and when `C1` moves the fact into its own collection, this path moves with it, because it is
not a second path.

### ⚠️ That deferral now points at a closed ticket, and nothing open carries the work

Both this brief and `9-duration-box`'s changelog say the inversion *"is `C1`
[#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) and **not this ticket**"*.
`Observed:` 2026-08-20 — **#19 is CLOSED**, `state_reason: completed`, closed 2026-08-10.

It is a **decision** ticket on the `C1`–`C22` decision map, and it was closed because the
decision was *made*: its resolution comment carries the fact-vs-judgement split and the formula
itself. It was never an implementation ticket and it will not build anything.

Checked for another owner and there is none:

| Where | Result |
|---|---|
| Open issues on the repo | **6**, and none is the inversion (`#7`, `#8`, `#48`, `#51`, `#53`, `#54`) |
| `TODO/` | `ProductModel.TODO.future.md` lists `C1` as a **question** answered by #19 — map material, not a build item |
| `sessions/` and `sessions/done/` | no brief names it |

So the §1.4 model exists as a **decision with no implementation owner**: it lives in
`docs/PRODUCT_v0.3.md` §1.4 and in #19's resolution comment, and in three changelogs that each
hand it onward to a ticket that is shut. This session did **not** file an issue for it —
creating one is an outward-facing write and is Ido's call — and did not change its own scope,
because building it is a model migration far outside `#7`. It is reported so that the next
session deferring to `C1` learns it is deferring to nobody.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **515 / 0**, 0 skipped — **+9** on `6-silent-filing`'s 506. New `TaskCompletionTest` (9) |
| **Instrumented** (`am instrument`, `Pixel_10_Pro_XL`) | **139 / 0** — **+18** on 121. New `AlreadyDoneUiTest` (12), `AlreadyDoneRenderTest` (2), `SmartAddReceiptUiTest` +4. `OK (139 tests)` in 172.7 s |
| **Build** | `:app:assembleDebug` + `:app:assembleDebugAndroidTest` green |
| **Render pass** | **Looked at**, two frames — [`quick-add.png`](../../docs/render-passes/2026-08-20-7-quickadd-complete/quick-add.png), [`goal-detail-row.png`](../../docs/render-passes/2026-08-20-7-quickadd-complete/goal-detail-row.png) |
| **Cloud Functions** (`functions/`, `npm test`) | **37 / 0**, unchanged. Nothing here touches `functions/` — and `projectPoints` needed no change, see below |
| **`firestore.rules`** (`firestore-tests/`) | **Not applicable, stated rather than skipped.** No rule change: task documents live under `users/{uid}` with no field allowlist, so `isDone`/`completedAt` on a *created* document are already covered by the owner-only `users/{uid}/{document=**}` match |

**The one thing that could have silently broken and did not.** A born-done task is a task
**created** already complete, so the points projection only fires if its trigger covers creates.
`projectPoints` is `onDocumentWritten` over `users/{uid}/tasks/{taskId}` and re-reads the whole
collection on every run — its own KDoc says *"including a create and a delete"*. Verified by
reading it before writing any of this, rather than discovered afterwards by a total that did not
move.

`TaskCompletionTest`'s last two cases drive **`BuildSummaryUseCase` and `TimeAllocationUseCase`
themselves** rather than re-typing their filters. Both are pure with no-arg constructors, so
there is no reason to test a copy — and a copy is exactly what stays green when the predicate
upstream changes shape. Each asserts in **both** directions: the unstamped task really is
dropped (0 tasks, 0 points; empty allocation), the stamped one really is seen (1 task, 15
points, 45 minutes).

### The render pass found a defect — in the instrument, not the feature

The first `AlreadyDoneRenderTest` put all five states in one `Column`. It captured a PNG, wrote
a file well over the size floor, and **passed** its width and height assertions — while the
bottom two cards were **not in the picture at all**. An unscrollable `Column` gives overflowing
children zero height, so the goal-detail chip came back as
`(l=84.0, t=3004.0, r=429.0, b=3004.0)px` — `b == t` — and could not even be clicked.

What caught it was the floor assertion asking whether the chip it had just tapped was
**selected**: a check on the **subject**. All three checks on the **artifact** — file length,
bitmap width, bitmap height — were green against a picture missing the half it existed to show.
That is `kb/dev/look-at-your-own-output.md` exactly: **assertions about a capture's bytes are
not assertions about its contents**, and the instrument degraded silently on the one case it was
built for. Split into two frames, each asserting that the state it exists to show is really in
it *and* that its comparison partner is really in the other state.

### What the two frames were judged on

1. **Selected vs unselected are unmistakable.** Unselected is **outlined**; selected is a filled
   container with a leading check. That asymmetry also answers the one adjacency worth worrying
   about — the selected chip's container colour is close to the `Sort` button's, and `#9`'s
   render pass found real trouble in exactly that shape (two marks in one row meaning *press me*
   and *nobody typed this*). It is acceptable here for a reason visible in frame 1: the state
   **every user sees first** is the unselected one, where the chip is outlined and therefore
   unmistakably not a button. By the time it is filled, the user is the one who filled it.
2. **The two surfaces read as one feature** — same words, same shape, same position relative to
   the inputs.

---

## Files

**Added:** `domain/model/TaskCompletion.kt` · `app/src/test/…/domain/TaskCompletionTest.kt` ·
`app/src/androidTest/…/ui/AlreadyDoneUiTest.kt` · `app/src/androidTest/…/ui/AlreadyDoneRenderTest.kt` ·
`docs/render-passes/2026-08-20-7-quickadd-complete/{quick-add,goal-detail-row}.png`

**Changed:** `data/firestore/TaskRepositoryImpl.kt` · `domain/model/Task.kt` ·
`feature/dashboard/DashboardViewModel.kt` · `feature/dashboard/DashboardScreen.kt` ·
`feature/goals/GoalDetailViewModel.kt` · `feature/goals/GoalDetailScreen.kt` ·
`app/src/androidTest/…/ui/{SmartAddReceiptUiTest,SilentFilingUiTest,DurationBoxUiTest,DurationBoxRenderTest}.kt`

The last three androidTest files changed only because `onClassify` and `onAdd` each gained a
parameter; each keeps ignoring it with a named `_`, and a comment says which suite owns the flag.

---

## Foreign commits carried by this session's push

`git log @{u}..HEAD` held two commits that are not this session's, and precondition 5 adjudicates
them here rather than as *"outside the task's scope"*:

| Commit | Session | Paths |
|---|---|---|
| `db1597b` | `ticket-close-gap` | `CHANGELOG/2026-08-20/ticket-close-gap.md`, `CHANGELOG/CHANGELOG_README.md`, `SESSIONS.md` |
| `7c7ef77` | `ticket-close-gap` | `kb-candidates/2026-08-20-ticket-close-gap.md` |

**Both ride along, and the board says so positively rather than by silence.** `db1597b` carries
that session's own **explicit release note** — *"🏁 `ticket-close-gap` RELEASED 2026-08-20 — this
commit. No singletons held."* — which is a signal it wrote about itself, so no transcript
escalation is needed. Its paths do not intersect this session's at all, and the working tree held
nothing of theirs.

**They were considerate in a way worth recording, and this session can now discharge it.**
`ticket-close-gap` closed `#6`, `#9` and `#11` and stated in each closing comment that **the
suites were not re-run** — deliberately, because this session held the Gradle daemon and its
uncommitted edits were in the tree, so a run would have tested work-in-progress rather than
`HEAD`. That gap is now closed from this side: the full JVM suite (**515 / 0**) and the full
instrumented suite (**139 / 0**) both ran green on a tree that is `HEAD` **plus** this ticket's
additive changes — including the exact suites their evidence tables cite, `SmartFilingTest`,
`DurationEntryTest` and `FillLadderTest`.

`ticket-close-gap` is also why `/kickoff` §5 grew a **step 4** mid-session (close the ticket, or
say why not). This session obeys the version at `HEAD`, which is the one with step 4 in it.

---

## Device and singletons

Ran on **`Pixel_10_Pro_XL`** — the AVD already booted, rather than starting the second one
(`kb/dev/android-device-verification.md` §5's RAM floor; §2's wedge test passed first,
`sys.boot_completed` = 1 and `adb shell` returning rather than hanging). The board claim was
corrected from `Pixel_10_Pro_XL_B` to the running device **before** the first `adb install`.

**Nothing was needed from Ido and nothing on the device was destroyed.** The run was
`install -r` + `am instrument`, never `connectedDebugAndroidTest` — §8's data-preserving
reinstall — so any signed-in account survives, and the two PNGs survived to be pulled, which
the Gradle task's uninstall would have deleted first.

One trap worth the line: `adb pull /storage/emulated/0/…` from **Git Bash** silently rewrote the
device path into `C:/Program Files/Git/storage/emulated/0/…` and reported *"failed to stat
remote object"* — a message that reads as *the capture was never written*. It had been.
`MSYS_NO_PATHCONV=1` fixes it.
