# `49b-overall-progress` — 2026-08-16

Routed defect · *"the dashboard reads **Overall progress 16259%**"* — spotted by `widget-pack` on a
device (`d2cbaef`) and routed to [`#49`](https://github.com/idomarhaim/Android_Final_Project/issues/49)
· branch `feat/goalpilot-implementation` · mode `AUTO MODE`

## What was wrong, and it was two things

`widget-pack`'s note pointed at `DashboardViewModel.kt:103` and §4.4's ⚠️. That is one of the two
sites and the less serious one.

**1 · A cross-goal mean of unbounded fractions.** `#49` deleted `progressFraction`'s `0..1` clamp
because §1.5 makes overshoot legal and shown. Two places then averaged that number across goals:

| Site | Renders as |
|---|---|
| `DashboardViewModel.kt:103` | `DashboardScreen.kt:717` — the literal `"16259%"` |
| `ProgressSummary.averageProgress` | `SocialRepositoryImpl.kt:189` → the text of a **shared post** |

The second is worse and was not in the routing note: that number leaves the user's own screen and
is published to other people.

**Why nobody saw it before the device pass, and it is worth naming:** `DashboardScreen.kt:710`
feeds the same value to a `ProgressRing`, which clamps at the draw call. So **the ring looked
perfectly normal and only the text lied.** A screenshot check that glanced at the card would have
passed it; the defect was visible only in the one element that states a number instead of drawing
one.

**2 · The number underneath — not fixed here, recorded instead.** `HealthMetric.STEPS` creates
*"Weekly steps"* with a target of `70_000`, and the sync writes **one entry per day**. Since `#49`,
`currentValue` sums every entry ever, so that goal's fraction grows at about **one target per
week** without bound.

## What shipped

`DerivedProgress.overallCompletion(fractions)` — one seam, clamping **per goal at the aggregation
site**, and both call sites now use it so they cannot drift apart.

**This is not a fifth clamp undoing §1.5's four.** The clamps `#49` deleted were on the goal's own
number, which made a beaten goal unreadable *on the screen a human writes to*. This one is at an
aggregation, and the distinction is the one already committed in `Goal.progressFraction`'s KDoc —
*"callers that draw a bar clamp at the drawing, where the constraint actually is"*. `GpProgress` and
`ProgressRing` already do exactly this. A goal at 300% still says 300% everywhere it speaks for
itself, and there is a test that fails if someone "fixes" this by putting the clamp back on the
model.

**Derived, not asked** (§4.4 flags the mean but names no replacement): §4.4 already refuses this
exact shape one chart over — *"a percentage is a fraction of its own target, so ranking by movement
partly ranks how modest the goals are"*, which is why the effort/outcome chart orders only minutes.
An average of per-goal percentages has that flaw plus one the ranking does not: it is unbounded.
Clamping the summand is the smallest change that makes the headline mean what it says. **Overturn
it if you would rather the card showed *"3 of 7 goals complete"* or hid itself** — both are
defensible and both are bigger than a defect fix.

## Deliberately not done

**The periodic-target defect is recorded, not repaired** —
[`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`](../../TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md),
with the two interim options and why neither was taken. The real fix is §2 recurrence, which is
unbuilt: a weekly target needs a window to sum over, and the model has no period on a goal.

**Attribution, stated plainly:** `#49` did not create that defect. Before it, `addProgress` clamped
the stored counter to the target, so the same goal sat at a permanent and equally wrong **100%**.
Removing the clamp swapped a silent wrong answer for a loud one — better, and still wrong.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **323 passed, 0 failed, 0 skipped** (was 312; **6 new**) |
| **APK build** (`:app:assembleDebug`) | **BUILD SUCCESSFUL** |
| **Security rules** (`firestore-tests`) | not re-run — no rules, DTO or Firestore path changed in this unit |
| **Instrumented** | not run — see below |
| **Cloud Functions** | no test layer exists |

The six new cases in `DerivedProgressTest` (now 22):

- bounded above with a goal at 48 571% — the shape that produced the headline;
- bounded below by a goal whose progress has gone negative;
- **unchanged where nothing overshoots**, so the clamp is invisible in the ordinary case rather
  than a behaviour change dressed as a fix;
- empty list is `0f`, not a division by zero;
- **clamping the aggregate does not clamp the goal** — the regression guard against re-adding the
  model clamp;
- a **witness** for defect 2: 90 daily entries of 8 000 steps against a 70 000 weekly target reads
  `progressPercent == 1028`, and the headline stays at 100% anyway. It documents the open item
  numerically instead of describing it.

**Instrumented not run.** The emulator is an exclusive singleton, `36-tasks-consent` has a known
failing instrumented test in flight (`LifeAreaReorderUiTest`, `3e4a8ab`, a `#2` defect), and this
unit changes two arithmetic expressions with no androidTest source over either. Stated rather than
skipped.

`Untested:` the fix has **not** been seen on the device that produced the 16259%. The arithmetic is
bounded by construction and pinned by tests, but the screenshot that would close this loop is owed
and is not mine to take without the emulator.
