# `36-tasks-consent` — repairing the `#2` drag defect the unblocked test layer found

**Session:** `36-tasks-consent` (third sitting), continuing from
[`CHANGELOG/2026-08-15/36-tasks-consent.md`](../2026-08-15/36-tasks-consent.md).
**Branch:** `feat/goalpilot-implementation`. **Mode:** `AUTO MODE`.
**Not this session's ticket:** the defect belongs to
[`#2`](https://github.com/idomarhaim/Android_Final_Project/issues/2), whose session had released.
Repaired **on Ido's instruction**, after he was shown the options and handed the decision back.

**In one line: drag-to-reorder worked again by deleting the gesture race instead of winning it — the
click target moved off the card and onto the card's content.**

---

## The defect

`#2` made the whole life-area card the click target — `GpCard(onClick = onOpen)` — which put a
`clickable` **around** the drag handle. The two then competed for the same press, the card won, and
dragging produced **nothing at all**:

```
LifeAreaReorderUiTest.dragging_theFirstHandleOntoTheSecondCommitsThatMove
  expected : [(0, 1)]     but was : []
```

Both `#2`'s changelog and a code comment asserted the opposite — *"the drag handle still works
because `detectDragGesturesAfterLongPress` consumes its own events"*. **That was reasoning, not a
result.** `change.consume()` runs inside `onDrag`, which is *after* the long press is recognised; it
does nothing about the enclosing `clickable` claiming the press first. The claim was unfalsifiable at
the time because `androidTest` could not compile.

## The fix, and why not the obvious one

The obvious fix is to make the handle **win** the race — consume the down, or hoist the gesture. It
was rejected: it leaves two things fighting over one press and working only as long as nobody
touches the arbitration again.

**The click moved inward instead.** `GpCard` carries no `onClick`; the clickable is a `Row` wrapping
the icon, name and subtitle — everything **between** the drag handle and the Edit/Delete buttons. The
race is now *structurally impossible* rather than carefully arbitrated.

It is also the better interaction: **tapping a drag handle should never navigate.** The card keeps a
target that is nearly its full width, and `onClickLabel = "Open <area>"` means a screen reader
announces the action rather than the app inventing a second content description on a node whose text
is already the label.

## What had no test, and now does

The regression shipped because the suite asserted *reordering works* and nothing asserted the
**boundary** between the two gestures. Three assertions now hold it:

| Test | Pins |
|---|---|
| `dragging_…CommitsThatMove` | the drag still commits `(0, 1)` — **plus `opened` is empty**, so a future click target that steals the press fails *here*, naming the cause |
| `tappingTheCardOpensThatArea` *(new)* | `#2`'s actual feature — the route into the area |
| `tappingTheDragHandleDoesNotOpenTheArea` *(new)* | the invariant whose absence let this ship |

`onOpen` in the test harness is now **recorded rather than stubbed**; a stub cannot tell "the drag
worked" from "the click also fired".

## 🧪 Tests

Run against an **isolated build directory** (`--init-script`, session scratchpad) so nothing raced a
sibling's `app/build`; counted from the result XML, not the console.

- **JVM unit — 323 tests, 0 failures, 0 errors, 0 skipped, 32 classes.**
- **Instrumented — 43 tests, 0 failures, 0 errors, 0 skipped** on `Pixel_10_Pro_XL (AVD) — 17`.
  Was **41 tests, 1 failure** before this change; +2 are the new boundary tests.
- **`:app:assembleDebug`** — green.
- **Firestore rules** — not owed: no rules file touched, and this change writes nothing.
- **`Untested:` Hebrew (§0.8).** No language picker exists yet (`#48`/`#51`), so the repaired row has
  not been seen in RTL. The click target is layout-direction agnostic, but that is an argument, not
  an observation.

## Push

**Held.** Ido was asked and answered *"hold the push"*; that answer stands even though the condition
it was given under — *if it is not fixed tonight* — no longer holds, because reinterpreting an
explicit instruction is not this session's call. The whole outgoing range is now green, including
`widget-pack`'s `d2cbaef`, which has been waiting since 22:05 on 2026-08-15. One word releases it.

## Concurrency

`49b-overall-progress` held the `SESSIONS.md` lease when this unit needed it. **Waited in the
background rather than asking** (§5.2), and did the code repair meanwhile — the ordering this
session got wrong earlier in the night and is now doing right. `widget-pack`'s 22:05 question was
answered on the board in `91f05c3`, four hours late, with the delay named rather than glossed.
