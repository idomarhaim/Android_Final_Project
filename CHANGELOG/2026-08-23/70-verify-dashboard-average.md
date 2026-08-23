# `#70` — the run `f25cca5` could not take

**Session** `70-verify-dashboard-average` · **2026-08-23** · brief
[`sessions/70-verify-dashboard-average.md`](../../sessions/done/70-verify-dashboard-average.md) ·
ticket [`#70`](https://github.com/idomarhaim/Android_Final_Project/issues/70)

`f25cca5` shipped the dashboard's *Overall progress* card **unverified** and said so three times
over — commit message, changelog section, board note. This session is the run it owed. Nothing here
re-decides what the card should say; `f25cca5` decided that, and the render agrees with it.

**Verdict: the change is correct in all three states, on both brightnesses, on both axes of the
geometry nobody had checked.** Three findings came out of running it, none of them a defect in
`f25cca5`, and all recorded below rather than fixed.

---

## ⚠️ The first result was a non-result, and it looked exactly like a pass

The first `:app:testDebugUnitTest` returned **`BUILD SUCCESSFUL in 4s`** with every task
`UP-TO-DATE`. That is not this session's run — it is `68-drag-to-move`'s, replayed out of Gradle's
cache. The evidence is in the results themselves:

| | |
|---|---|
| `app/build/test-results/testDebugUnitTest/*.xml` | **88 files, all stamped 16:40:45** — before this session opened |
| tests in them | **1068**, where the brief predicted **1018** |

The 50-test gap *is* the tell: those are `68-drag-to-move`'s calendar tests, which did not exist when
the brief was written. So the cache was answering a question about **another session's tree** — the
precise §4p failure `f25cca5` refused to risk, arriving anyway one step later and wearing a green
tick. `look-at-your-own-output.md` §4k's habit is what caught it: read the line count and the
timestamps beside the verdict, never the verdict alone.

Re-run with `--rerun-tasks`: **2 m 21 s**, which is what a real compile-and-test costs here.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest --rerun-tasks`) | **1068 tests, 88 classes, 0 failures, 0 errors** — genuinely re-executed, results stamped 17:38 |
| — of which `UnmeasuredPercentTest` | **21 tests, 0 failures**, including all three cases `f25cca5` added |
| **Instrumented — the render pass** (`OverviewCardRenderTest`, new) | **7 tests, 0 failures** |
| **Instrumented — full regression** (`am instrument`, no class filter) | **303 tests, 1 failure** — `ChartVolumeRenderPass.blossom`, **pre-existing**, bisected to clean `HEAD`; see below |
| **Look** | 3 PNGs, three states light + dark + a fourth state, read by eye |

`f25cca5`'s own prediction — *"the honest expectation is green, which is exactly why it must be said
rather than assumed"* — held. Its ranked list of likely failures scored **1, 2, 3 all clear**.

⚠️ **`connectedDebugAndroidTest` was never used.** `adb install -r` on both APKs then
`adb shell am instrument -w`, per `kb/dev/android-device-verification.md` §8. **No sign-in was
needed and none was destroyed** — `OverviewCardRenderTest` uses a bare `createComposeRule()` with no
Hilt, no Firebase and no account.

---

## What the geometry actually does — the one thing this session existed to look at

The claim under test: `UnmeasuredMarker(size = 56.dp, modifier = Modifier.padding(18.dp))` occupies
the same box as the `ProgressRing(size = 92.dp)` it replaces, because `56 + 18 + 18 = 92`.

**That sum is true, but not for the reason the arithmetic suggests, and the reason is in a different
file.** `UnmeasuredMarker` composes `modifier.size(size)` — caller's modifier **first** — which puts
the padding *outside* the sized box. `ProgressRing` does exactly the same. Had either written
`Modifier.size(size).then(modifier)` the same three numbers would have produced a **56 dp** node with
20 dp of usable middle, and the card would have lost 36 dp in one state out of three. So the answer
depended on a modifier order two files away from the change, which is why reading `f25cca5` was never
going to settle it.

**Measured, both axes:**

| Axis | all measured | some measured | none measured |
|---|---|---|---|
| card height | `236.33334 dp` | `236.33334 dp` | `236.33331 dp` |
| left edge of `Overall progress` | identical | identical | identical |

The `none` column differs from the other two by **0.00003 dp** — about one ten-thousandth of a pixel
at this device's density, and simply the px→dp round trip landing on a neighbouring float.

⚠️ **The first version of the height assertion used exact equality and therefore FAILED on this.**
Recorded rather than quietly relaxed: the assertion now allows `0.05 dp`, which is **700× smaller**
than the 36 dp gap it exists to catch and **1600× larger** than the observed noise. A tolerance
nobody can justify in a number is how a geometry test stops testing geometry, so the justification
is in the test's own comment.

### The width axis was not in the brief, and the render is what added it

Looking at the light PNG, the dashed square *appears* to sit left of where the ring sits. It does
not. A progress arc is **open on the left** — at 50 % it paints only the right half — so its **ink**
is off-centre while its **box** is not. `Observed:` in the light PNG the `#` glyph and the `50%`
label read as sharing a centre line — by eye, which is exactly the instrument that had just been
wrong, so it is **not** what the claim rests on. What settles it is the assertion: the text column's
left edge is where the ring/marker node's width **ends**, and it is identical in all three states, so
the two nodes occupy the same horizontal box whatever their ink does inside it. This is the failure
mode where an eye disagrees with the layout and the eye loses, so it became
`theTextColumnStartsAtTheSamePlaceInAllThreeStates` rather than a sentence in a changelog.

**And it covers a case the height assertion structurally cannot.** If the padding had gone *inside*
the 56 dp, the marker node would be 56 dp tall — shorter than the text column beside it — so the
`Row` height would be set by the text and the card would **not** change height at all. The height
test would have reported green over the exact defect it was written for. Two axes, because one of
them has a blind spot in the direction of the thing being hunted.

---

## The card, in the three states

| State | Caption rendered | Ring |
|---|---|---|
| every goal measured | `Averaged across all your goals` | `50%`, unchanged |
| 1 of 2 measured | `Averaged across the 1 goal that has a number` | `40%`, over the measured one |
| none measured | `No goal has a number yet` | **no ring** — dashed `#` marker |

Singular and plural are separate branches and both are grammatical (`the 1 goal that has`,
`the 2 goals that have`) — asserted, because a count interpolated into prose is where that normally
goes wrong.

**Dark is not a duplicate frame.** The marker is a 1.5 dp dashed stroke at `onSurfaceVariant`, so
whether it reads as a deliberate placeholder is a **contrast** question rather than a hue one — and
`#66`'s original defect was found in a dark render. It holds: the square is legible against the dark
card surface without competing with the text beside it.

---

## 📌 Finding 1 — a fourth state the brief did not list, and it is reachable

`DashboardScreen` renders `OverviewCard` **unconditionally**; the `state.goals.isEmpty()` branch sits
further down the same `LazyColumn`. So an account with **no goals at all** takes the
`measuredGoalCount == 0` arm and is shown:

> **Overall progress**
> *No goal has a number yet* — beside a dashed marker and **`0` Goals**

It is not false. It is aimed at the wrong problem: a brand-new user is told to put a **number** on
their goals when what they have is **no goals**. `#66`'s own standard is that a label must name the
thing it is actually about, which is the standard this arm does not quite meet.

**Not fixed here** — re-deciding what the card says is explicitly out of this session's scope, and
this is a wording decision rather than a defect in `f25cca5`. Captured as
`issue-70-overview-no-goals.png` so it can be decided from a picture. Recorded for whoever owns the
empty-account pass.

## 📌 Finding 2 — one instrumented test is RED on `main`, and it was red before this session

`ChartVolumeRenderPass.blossom_everyMaterialFlatAndRaised` fails with

```
androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 2000 ms
  at ...WindowCapture_androidKt.forceRedraw(WindowCapture.android.kt:124)
  at ...captureToImage(AndroidImageHelpers.android.kt:97)
  at ChartVolumeRenderPass.capture(ChartVolumeRenderPass.kt:171)
```

**It is not this session's, and that was established by bisect rather than by argument.** The
temptation was to reason it away — the only `main/` edit here is a visibility keyword on a dashboard
card, and `ChartVolumeRenderPass` renders charts — but *"I cannot see a mechanism"* is exactly the
reasoning this repo's own notes tell you not to bank. So:

1. `OverviewCardRenderTest.kt` moved out of the tree, `git checkout -- DashboardScreen.kt`;
2. both APKs rebuilt and installed from that clean `HEAD`;
3. the class run twice — **`Tests run: 2, Failures: 1`**, same test, same stack, 15:14:17 and
   15:15:17.

`Observed:` it reproduces **4 times out of 4** (twice with this session's changes, twice without), so
it is not the ordinary busy-emulator flake either. `aurora_everyMaterialFlatAndRaised`, the sibling
method, passes every time.

`Inferred:` the mechanism is the class's **paused main clock**. `capture()` drives
`composeRule.mainClock.advanceTimeBy(SETTLE_MS)` — deliberately, because the charts animate and
`waitForIdle` returns long before the sweep finishes — while `captureToImage` internally calls
`forceRedraw`, which waits on a frame callback the paused clock is not delivering. The class's own
KDoc already anticipates the family (*"a hang in the second leaves the first skin's frames on the
device rather than nothing at all"*). `Untested:` that diagnosis — nothing here changed the clock
handling to confirm it.

⚠️ **`68-drag-to-move` reported this same suite as 296 / 0 failures earlier today.** Either the
device state differed or it was genuinely passing then; this session cannot tell which, and did not
try, because the bisect already answered the only question in its scope. **Not fixed here** —
`ChartVolumeRenderPass` is nobody's claimed path this session and is well outside `#70`. Worth its
own ticket.

## 📌 Finding 3 — the brief's expected test count was stale on arrival

The brief and `#70` both say *"expect **1018**"*. The real figure at this `HEAD` is **1068**. Nothing
is wrong: `68-drag-to-move` landed ~50 tests between the brief being written and being run. Worth
saying because a predicted count in a brief is read as an **assertion** by the session that runs it,
and a session less inclined to check the timestamps could have gone hunting for 50 missing tests —
or, worse, accepted the 4-second cached `1068` as confirmation that its own run had happened.

---

## Regression

`private fun OverviewCard` → **`internal fun OverviewCard`**, so the render pass can import it. This
is the file's own existing convention, not a new one: `DailyMissReviewCard`, `SmartAddCard`,
`SmartAddReceiptSnackbar` and `CalendarDisappearanceCard` in this same file are all `internal` for
exactly this reason, and `GoalHeaderCard` and `ProgressByGoalCard` are elsewhere. Nothing outside the
module can see it either way; a KDoc on the declaration says why it is not `private`, so the next
reader does not "tidy" it back.

**Decision taken per that convention rather than asked about** — the alternative was to copy the card
into the test, which would have rendered a *replica* and proved nothing about the shipped composable.

## Files

`app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt` — visibility + KDoc ·
`app/src/androidTest/java/com/idomarhaim/goalpilot/ui/OverviewCardRenderTest.kt` *(new, 7 tests)* ·
`sessions/70-verify-dashboard-average.md` *(closed)* · `SESSIONS.md`

**No production behaviour changed in this session.** The only `main/` edit is a visibility keyword.
