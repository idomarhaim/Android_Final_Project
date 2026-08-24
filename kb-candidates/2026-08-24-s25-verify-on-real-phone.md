# KB candidates — `s25-verify-on-real-phone`, 2026-08-24

Three entries. All three are about the **instrument**, not about Compose.

---

## 1 · `BoxWithConstraints` is a `SubcomposeLayout`, and under an intrinsic measurement it CRASHES

**Claim.** *"Measure the space you have"* is the right instinct for an adaptive layout
and it is **unavailable** anywhere an ancestor asks for intrinsic measurements.
`BoxWithConstraints` — like `LazyColumn`, `LazyRow`, `TabRow` and anything else built
on `SubcomposeLayout` — throws:

```
java.lang.IllegalStateException: Asking for intrinsic measurements of SubcomposeLayout
layouts is not supported.
```

It is not a warning, a fallback, or a bad frame. **The process dies.**

**Observed:** 2026-08-24, `Android_Final_Project`. A calendar chip needed to know
whether its lane was wide enough for a 42 dp time column beside the title. The
obvious build — wrap the chip in `BoxWithConstraints` and branch on `maxWidth` —
compiled, passed **1,101 JVM tests**, and took the entire calendar screen down on the
first instrumented run. The grid above it uses `Modifier.height(IntrinsicSize.…)`.

**Why it is worth a page.** The failure is invisible to everything cheap:

| check | would it have caught this? |
|---|---|
| the JVM suite (1,101 tests) | **no** — no Compose layout runs |
| reading the diff | **no** — every line is idiomatic |
| a screenshot / render pass | **no** — there is no frame; the process is gone |
| an instrumented run | **yes, immediately** |

So it belongs beside `look-at-your-own-output.md`'s *re-run whatever will consume your
output*: the consumer of a layout is **a layout pass**, and nothing short of one
executes it.

**The remedy when you cannot measure.** Compute the width from the paddings the parent
actually applies and the column count, and pass **one decision** down rather than
letting each child ask. That is cheaper too — no subcomposition per row — and it is
JVM-testable, which the measured version never was. The cost is honest and worth
naming: a *derived* number can drift from the layout it was derived from, so it needs a
test that checks it against a **measurement taken on a real device**, not against
another copy of itself.

**Rejected:** *use `Layout` instead* — same problem, you still cannot choose which
composable to emit without subcomposing. *Wrap the grid to stop it asking for
intrinsics* — changes the grid's own sizing to work around a chip.

**Destination.** `kb/dev/` — a Compose page, or a § on `look-at-your-own-output.md`.
**Anchors.** `feature/calendar/CalendarScreen.kt` `laneWidthDp` KDoc carries the
worked instance and the exception text.
**Supersedes.** Nothing.
**Status.** Ready.

---

## 2 · An enum standing in for a measurement is right on the device it was written on

**Claim.** When a layout branches on a **mode** (`WEEK` / `THREE_DAYS` / `AGENDA`, a
tab, a form factor name) where the thing that actually decides is a **width**, the
branch is a proxy — and a proxy is not wrong, it is *wrong somewhere else*. It holds
on the developer's device and fails on a narrower one, which is the worst available
distribution of correctness because the failing case is the user's.

**Observed:** 2026-08-24. `val stacked = state.zoom == CalendarZoom.WEEK` chose the
compact chip. That lumped `AGENDA` — one column, the full width of the screen —
together with `THREE_DAYS`, a third of it, and gave them the same layout. On a 448 dp
emulator the three-day lane was survivable; on Ido's 384 dp phone the title got 44 dp
and wrapped to one word per line.

**The tell, and it is checkable before shipping.** For every mode-based layout branch,
ask **what quantity the mode is standing in for**, then ask **what range that quantity
takes across supported devices**. Here the quantity is lane width and the range is
~101 dp to ~257 dp across phone-to-tablet — a 2.5× spread inside a single enum value.
An enum value that spans a 2.5× range of the deciding quantity is not a category, it
is an average.

**The half that makes this worth writing down:** the same codebase had **already
recorded this exact failure**, one zoom over. `StackedChip`'s KDoc says of `WEEK`:
*"every time clipped to `07:0`, `09:3`, and every title reduced to an ellipsis or to
nothing."* The lesson was found by a render pass, written up carefully, and **applied
at one width**. Nobody asked whether the neighbouring value had the same problem. A
fix bound to the instance that produced it is a fix that will be re-earned.

**Rejected:** *measure it instead* — correct in general and unavailable here; see
entry 1.

**Destination.** `kb/dev/` — pairs with entry 1; the same page could hold both, since
one is *why the proxy exists* and the other is *why you cannot simply measure*.
**Anchors.** `CalendarScreen.kt` `chipFormFor`, `WideChip`'s KDoc,
`CalendarChipWidthTest`.
**Supersedes.** Nothing.
**Status.** Ready.

---

## 3 · Reproduce the reporter's device by *asking* it, and re-check the reproduction when the device arrives

**Claim.** A reproduction built from a screenshot's pixel dimensions is a **guess with
a number in it**, and it reads as a measurement in every later document. When the real
device becomes reachable, the first thing to do is re-read the numbers and **correct
the earlier claim**, even when the conclusion does not change.

**Observed:** 2026-08-24, both halves in one session. A screenshot 1080 × 2340 was
reproduced as `wm size 1080x2340` + `wm density 480` → 360 dp, and the resulting
changelog said *"his exact geometry"*. When the phone came online:
`dumpsys window displays` reported **384 dp at 450 dpi**, with **`font_scale` 1.15** —
a dimension the reproduction had not considered at all. The bug reproduced
pixel-for-pixel on the guess, so nothing was withdrawn; but *equivalent or worse* is
what it had been, and only one of the two is a fact.

**The cheap right way**, once a device is reachable at all:

```bash
adb -s <serial> shell dumpsys window displays | grep -o 'sw[0-9]*dp w[0-9]*dp h[0-9]*dp [0-9]*dpi'
adb -s <serial> shell settings get system font_scale
```

Two commands, and they replace three inferences — pixels → density → dp, plus the font
scale nobody thinks of because it does not appear in a screenshot's metadata.

**And the corollary that actually caught something:** re-running the check at the real
numbers is what surfaced the **height**-driven test failure that looked exactly like a
regression. Same width, taller screen → passes. A reproduction that only matches one
axis will mislead you on the other.

**Destination.** `kb/dev/android-device-verification.md` — the same § as this session's
sibling entry on forcing a reporter's geometry, as its second half: *and re-measure
when the real device arrives.*
**Anchors.** `CHANGELOG/2026-08-24/s25-verify-on-real-phone.md` §1;
the correction inserted into `CHANGELOG/2026-08-24/s25-layout-and-tour.md`.
**Supersedes.** The *"Ido's exact geometry"* claim in the 2026-08-24
`s25-layout-and-tour` changelog — corrected in place rather than left standing.
**Status.** Ready.
