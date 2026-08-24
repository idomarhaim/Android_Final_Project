# s25-verify-on-real-phone — 2026-08-24

> **Summary:** Ido's Galaxy S25 Ultra reachable over wireless debugging for the first time — all four v0.4.0 fixes verified on the real device at its real settings, and a fifth defect found there: the calendar's 3-day chip starves its title exactly as Analytics used to. Fixed, and the first fix for it crashed the app, which only the instrumented run caught.

## What this session was for

Ido asked one question — *"can I connect to my phone wirelessly; does anything need
uninstalling?"* — and gave one directive: *"do what's needed to fix the defects."*

The answer to the question is **no**, and the reason is worth writing down because it
is the thing people get wrong: the app on his phone and the test app are **different
packages**, and a release built with the same key installs **over** the existing one
with `adb install -r`, preserving data and the Firebase sign-in. Nothing has to be
removed for either verification or an update.

## 1 · The four v0.4.0 fixes, on the real device

Every earlier check was on an emulator forced to an **approximation** of his phone.
The real numbers, read off the device with `dumpsys window displays`:

| | reproduced on 2026-08-24 (earlier) | Ido's actual phone |
|---|---|---|
| width | 360 dp | **384 dp** |
| density | 480 dpi | **450 dpi** |
| font scale | 1.0 | **1.15** |

⚠️ **So the earlier changelog's *"his exact geometry"* was wrong** and is corrected
here. It was *an* equivalent-or-worse geometry — the bug reproduced pixel-for-pixel
on it — but it was not his. Both produce the defect; only one of them is a fact.

All four verified at the real settings, `SM_S938B`, over wireless debugging:

| fix | evidence |
|---|---|
| the starved Analytics row | `Schedule a date with Pippo` = **549 px wide, h=54 px** — one line. Was 40 px × 205 px. |
| the `#` marker | **0** `#` glyphs in the hierarchy; **3** `No number` chips |
| sync cards off Home | **0** hits for `Google Tasks` / `Health data` across the fully scrolled Home |
| Settings order | opens on `Help` → `Replay tutorial` (y=715), then `Connected apps` (y=1475) |

## 2 · The fifth defect, found on his phone in the first minute

The app opened on the Calendar tab — where he had left it — and a chip read:

```
due     Write
20:00   the p…
        • Stu…
```

`WideChip` puts a **fixed 42 dp** time column and a **20 dp** tick either side of
the title. With the chip's own 8 dp padding and a 4 dp spacer that is **74 dp of
chrome before the title gets anything**. A three-day lane on a 384 dp phone is
~101 dp, so the title got **125 px — 44 dp**.

**It is the same class as the Analytics bug and a different mechanism**, which is
why finding one did not find the other. There, an *unweighted* child grew without
limit. Here nothing grows: the chrome is fixed, correct, and simply larger than the
lane. `TimeColumn`'s own KDoc had already fixed the growing version — *"a leading
column that can grow is a leading column that wins"* — and that fix is intact.

**`StackedChip` had recorded this exact failure one zoom over**, for `WEEK`:
*"every time clipped to `07:0`, `09:3`, and every title reduced to an ellipsis or to
nothing."* The lesson was written down and applied **at one width**. What was
missing: the chip was chosen by **zoom**, and the zoom is a proxy for width that is
right on a wide phone and wrong on a narrow one. `AGENDA` is one column the full
width of the screen; `THREE_DAYS` is a third of it; both got the same layout.

### The fix

- **`ChipForm { STACKED, NARROW, WIDE }`**, decided once by `chipFormFor(zoom, screenWidthDp)`.
- **`NarrowChip`** — time on **one line above** the title, and **the tick stays**.
  Reusing `StackedChip` would have fixed the clipping by deleting the control Ido
  actually uses on that view; a 3-day lane has twice `WEEK`'s room, so it does not
  have to pay `WEEK`'s price. Title goes from ~35 dp to ~74 dp of a ~101 dp lane.
- **`WIDE_CHIP_MIN_DP = 155`**, derived rather than tuned: 74 dp of chrome plus
  ~80 dp of title.

### ⚠️ The first fix crashed the app, and only running it found that

`BoxWithConstraints` was the obvious way to ask *how much room is there*. It is a
`SubcomposeLayout`, and the calendar grid asks for intrinsic measurements:

```
java.lang.IllegalStateException: Asking for intrinsic measurements of SubcomposeLayout
layouts is not supported. This includes components that are built on top of
SubcomposeLayout, such as lazy lists, BoxWithConstraints, TabRow, etc.
```

**Process crashed** — the whole calendar screen, on the first instrumented run.
Measuring is the right instinct and it is *unavailable here*, so the width is
arithmetic over the two paddings `DayColumns` actually applies. That number is
*derived* from a layout and can drift from it, which is what the JVM test guards.

Not caught by the JVM suite, not by a screenshot, not by reading the diff. Had I
shipped on the strength of 1,101 green tests, the calendar would have crashed on
his phone.

## 3 · The tutorial tests v0.4.0 shipped without

Last session left these **explicitly open as `unverified`** because
`connectedDebugAndroidTest` uninstalls the app. With a free AVD they ran, and the
open issue was right to be open: **1 of 14 failed** —
`anInformationalStepBlocksTheAppEvenOverItsOwnSpotlight` asserted
`next == 1`, and v0.4.0 deliberately made a spotlight tap **stop** advancing the
tour. The test was reporting my own intended change. Updated to the new contract,
plus a new case pinning the half that survives — a step pointing at **nothing**
still advances on a tap anywhere. **15/15.**

## 🧪 Tests

**JVM — 1,105 tests, 0 failures** (`:app:testDebugUnitTest`), counted off the JUnit
XML. Was 1,093; **+12**.

**Instrumented — 31 tests across three suites, 0 failures**, run at Ido's exact
geometry (384 × 832 dp, 450 dpi, font scale 1.15) on `Pixel_10_Pro_XL_B` via
`adb install -r` + `am instrument`, never `connectedDebugAndroidTest`.

| suite | result |
|---|---|
| `CalendarSurfaceUiTest` | 16/16 |
| `TutorialOverlayUiTest` + `TutorialNavigationUiTest` | 15/15 |

**Tests added — 12:**

- `CalendarChipWidthTest` (new, 10) — exercises `chipFormFor` and `laneWidthDp`
  directly: NARROW at 320–440 dp, WIDE on a tablet, AGENDA always WIDE, WEEK always
  STACKED, the threshold leaves the title ≥70 dp, and the derived lane is checked
  **against the width measured on the phone** rather than against another copy of
  itself.
- `CalendarSurfaceUiTest` (2) — the title's node width in dp at three-day width, and
  a photograph beside the number.

⚠️ **One JVM test I wrote was wrong and the suite caught it.** It derived the lane
as `(screenDp - 26) / 3` → 119 dp against a measured ~101 dp. Invented arithmetic
asserted as fact; replaced with the measurement and its provenance.

⚠️ **One instrumented failure was NOT mine, and the discriminator is cheap.**
`aHandMadeGoogleEventDrawsBesideTheAppsOwnWork` went red at 384 × 832 and looked
exactly like a regression from the chip change. At **384 × 1064** — same width,
taller screen — it passes, so the variable is height, not the chip. (The width was
never in question either way: `chipFormFor` returns NARROW at both 384 dp and the
emulator's native 448 dp, so *both* runs were already drawing the new form.)
`Dentist` is a 15:00 block in a 44 dp-per-hour scrolling grid and starts below the
fold on a phone-shaped screen; `assertIsDisplayed` does not scroll. Fixed with
`performScrollTo()`.

## Not done, and why

**The Health Connect ↔ challenge question Ido asked mid-session is answered, not
built.** It is unrelated to sync timing: `ChallengeParticipant.score` has exactly
one writer in the codebase — the manual *Report score* dialog — and
`SyncHealthDataUseCase` never mentions challenges. That is `D1` /
[`C14` #23](https://github.com/idomarhaim/Android_Final_Project/issues/23), already
established on 2026-08-06 with his own *August Steps Race* as the device evidence,
and `docs/PRODUCT_v0.3.md` §6 has since **decided** it: *a challenge scores from
each participant's goal*, with a server-owned score, `ChallengeType` deleted, and
joining linking or creating a goal. That is a multi-session feature depending on
`C7` and `C3`, not a wiring fix, so it goes back to Ido as a scope call.
