# `#67` — the acceptance test its own session could not run

**Session** `67-close-or-finish` · **2026-08-24** · ticket
[`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67) · brief
[`sessions/done/67-delete-anything.md`](../../sessions/done/67-delete-anything.md)

Ido: *"if `#67` is complete then close it — and if it is not, complete it."*

**It was not complete, and it is now. `#67` is CLOSED.**

---

## The gap was one thing, and it was named honestly

`#67`'s code shipped on 2026-08-23 (`c11c629`, `ec9996e`) with 12 new JVM tests, 15 instrumented and
six render PNGs. Its brief sits in `sessions/done/` with `status: done`. **Both facts are true and
the ticket was still open on purpose** — that session's own changelog says so:

> ⚠️ **`Untested:` end to end on a phone, and that is stated rather than glossed.** […] Nobody has
> watched a quick-add produce an orphan and then deleted it from the dashboard card. The reason is
> that the end-to-end path needs a signed-in account and a live Firestore.

That is the ticket's **acceptance test** — scope item 1 — and it is the one thing a hand-built
instrumented test cannot stand in for, because the claim under test is *the classifier really does
produce this state and the new card really does catch it*.

**This session had what that one lacked:** `emulator-5554` signed in, live Firestore, and
`c11c629` already inside the installed APK. So no build was needed and the Gradle daemon —
held by `docs-currency-guard` — was never contended. Verified by reading `#67`'s own strings out of
the installed APK (`WHAT GOES`, `This cannot be undone.`) rather than by trusting the commit graph.

## The run, on real data

| Step | Result |
|---|---|
| Smart-add *"Return the neighbours ladder"* — unrelated to all 9 goals, no date | classifier returned no goal; task written with `goalEdges = goalEdgesOf(null)` and no occurrence |
| Dashboard | **`Filed nowhere` appeared**, holding exactly that task. Absent before the add |
| Every other surface | goal detail lists `observeTasks(goalId)`; calendar draws only what has a *when*. Neither could list it |
| Delete icon → confirm | **`WHAT GOES` present, `WHAT STAYS` absent** — the designed behaviour when nothing survives, not an empty heading |
| Confirm → `Delete` | task gone; **9 goals, 5 tasks done, 1 this week all unchanged** |
| Card afterwards | **still present** — and correctly so, see below |

So the defect `#67` was filed for — *a task that cannot be deleted from the UI at any point in its
life* — is closed by a front door that was watched working, on data the app itself produced.

## 🔎 The card is surfacing a REAL orphan that predates this session

After my own task was deleted, `Filed nowhere` did not disappear. It holds **`בדיקה - לימודים`** —
a task that is not mine, that I did not create, and that was **not in the card when the run
started**. It is Ido's own data, in exactly the invisible state the ticket describes: filed under
no goal, no date, listed on no other screen, and until this feature existed, undeletable.

**Left alone.** It is real data and deleting it is Ido's call, not mine. But it is worth saying
plainly: **the feature found a live instance of its own bug on first contact with a real account**,
which is stronger evidence than the test that was written for it.

`Untested:` where it came from. It may predate the `deleteGoal` fix (`ec9996e`), which is precisely
the *"every task kept its edge to the deleted goal"* consequence that changelog records — in which
case it is a survivor of the old behaviour and the new card is what makes it reachable at all.

## ⚠️ A near-miss, and it is mine

**I came one tap from deleting a real goal.** Driving the UI by coordinates, one tap landed on the
Analytics screen's `Let it go` for **`Prepare for upcoming exam`** instead of my own task's delete
icon, and opened its confirm. **It was cancelled; nothing was deleted** — the goal is still there
and the count still reads 8 no-next-step goals.

The cause is method, not luck: I computed bounds from a `uiautomator` dump taken in a **previous
tool call**, so the coordinates described a screen that was no longer in front of me. The fix is the
one the rules already give for gates — **dump and tap in the same call, and echo the target node's
`content-desc` before tapping it** — which is what every interaction after the near-miss did.

**What made this survivable is `#67`'s own design.** Scope item 2 required a confirm that states
what goes and what stays; scope item 3 forbade `Delete` being any row's default action. A single
stray tap therefore opened a dialog instead of destroying a goal. **The ticket's safety requirement
caught the accident that the ticket's own verification produced** — which is the best argument for
that requirement anybody is going to get.

It also, accidentally, verified two scope items on real data with live counts:

> **Delete "Prepare for upcoming exam"?**
> **WHAT GOES** This goal, and its measure.
> **WHAT STAYS** 1 task, which stays and becomes unfiled.
> This cannot be undone.

That is scope item 2 (per-entity sentences, live count) and scope item 4 (`Let it go` wired on the
`SuccessFailureRunCard`) working in the real app.

## Scope, item by item

| # | Item | Verdict |
|---|---|---|
| 1 | The unfiled task — the ticket's acceptance test | **Done, and watched end to end this session** |
| 2 | One confirm in `ui/components/`, stating what goes and what stays with live counts | Done (`DeleteConfirm.kt`); seen on a goal *and* on a bare task, with and without the `WHAT STAYS` block |
| 3 | `Archive` and `Delete` stay two verbs; `Delete` is no row's default | Done — `setArchived` untouched, and the near-miss above is the proof the row is not click-to-delete |
| 4 | `C19`'s `Let it go` | Done — present on `SuccessFailureRunCard`, opens the confirm |
| — | `deleteTask` orphaning occurrences / `completionFacts` | Fixed in `ec9996e`; `deleteGoal` was also unfiling tasks and deleting `progressEntries` |

## 🧪 Tests

| Layer | Result |
|---|---|
| **End-to-end on device** *(the thing that was missing)* | **PASS** — signed-in account, live Firestore, `emulator-5554` |
| Screenshots | 7, looked at |
| JVM / instrumented | **not re-run** — no code changed this session, and `docs-currency-guard` holds the Gradle daemon. `c11c629`'s 1084 JVM / 319 instrumented stand |

**No code was written this session.** That is the correct outcome for *"close it if it is done"*:
the work was done, the evidence was not.

📱 **`emulator-5554` was used and NO sign-in was needed or destroyed.** No install, no build, no
`connectedDebugAndroidTest` — the APK was already there. One task was created and deleted, both mine.
