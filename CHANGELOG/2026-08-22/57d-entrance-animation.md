# `57d-entrance-animation` — 2026-08-22

> **Summary:** `#57` d — the dashboard's blocks now **arrive**: a spring-eased rise and fade, staggered by arrival order, keyed to screen entry and to nothing the screen can change. The brief's reading of the prototype is **wrong on the stagger** — `.34s`/`.36s`/`.38s` are three *element classes*, not a sequence, and the thing that sequences a list there is `animation-delay: ${i*45}ms`; ship the duration spread and nothing is visible. One real rendering defect was found **by looking at the frames**, not by a test: `graphicsLayer` with `alpha < 1` cropped every card's neo shadow to its own bounds, so cards spent the whole arrival flat with a hard dark notch at one corner — fixed with `CompositingStrategy.ModulateAlpha`. JVM **729/0** (+8 tests), instrumented **204/0** (+7). Seen twice on the device: an 8-frame frozen-clock strip, and the live dashboard mid-wave.

**[#57](https://github.com/idomarhaim/Android_Final_Project/issues/57) d** — the last of four
briefs. Brief: [`sessions/done/57d-entrance-animation.md`](../../sessions/done/57d-entrance-animation.md).

---

## 1 · What shipped

`ui/components/Entrance.kt` (new) — one small vocabulary, three pieces:

- **`GpEntrance`** — a screen's arrival. Hands out one slot per block, in composition order, and
  stops handing them out once the arrival window has passed. Takes the clock as an argument, so
  every timing property it has is checkable at the JVM layer.
- **`LocalGpEntrance`** — `null` by default, meaning *no arrival*. A card in a dialog, a sheet or a
  preview is simply there; a screen opts in with one `CompositionLocalProvider`.
- **`Modifier.gpEntrance()`** — the rise and the fade, applied inside `GpCard` and `SectionHeader`
  so the 30-odd card call sites needed no change at all.

`DashboardScreen` provides the entrance around its `LazyColumn`, below the loading gate.

### The values, read out of the prototype rather than approximated

| | prototype | here |
|---|---|---|
| easing | `--spring: cubic-bezier(.2,.9,.22,1)` | `GpSpring = CubicBezierEasing(.2f,.9f,.22f,1f)` |
| rise | `@keyframes rise { translateY(11px) → none }` | `ENTRANCE_RISE = 11.dp` |
| fade | same keyframe, same curve | one progress drives both |
| duration | `.card { animation: rise .34s both }` | `ENTRANCE_DURATION_MS = 340` |
| stagger | `style="animation-delay:${i*45}ms"` | `ENTRANCE_STAGGER_MS = 45`, capped at `450` |

## 2 · ⚠️ The brief is wrong about where the stagger lives, and it matters

The brief says:

> **The durations differ per card on purpose.** `.34` / `.36` / `.38` is a **stagger** […] Ship a
> single shared duration and you have implemented something that is technically a fade-in and
> visibly not this.

`Observed:` those three values are in
`docs/prototypes/2026-08-10-calendar-surface/index.html` at lines 285, 322 and 350, and they belong
to **three different element classes** — `.ev` (an event chip), `.row` (an agenda row) and `.deck`
(the hero). Each animates **once**. Three blocks that all start together and finish 40 ms apart is
not a sequence; it is invisible.

The thing that actually sequences a *list* is in that file's own render functions — line 693
(`animation-delay:${k*40}ms`) and line 796 (`animation-delay:${i*45}ms`). So the stagger is a
**delay**, and the duration is the single value `.card` uses. Implementing the brief literally
would have produced exactly the "technically a fade-in and visibly not this" it warns against.

The brief's *instruction* — read the exact values out of the prototype — is what caught this.

## 3 · The rendering defect the frames found, and the tests did not

`EntranceRenderPass` freezes the clock and writes one PNG per instant. The 064/144/208 ms frames
showed every card **flat, with a hard dark square at its bottom-right corner**, and its real neo
shadow appearing in a single frame at the end.

**Cause:** `Modifier.graphicsLayer { alpha = p }` with `alpha < 1` composites through an offscreen
buffer the size of the node — and `gpSurface` draws this app's shadow pair as concentric round rects
that deliberately extend **beyond** the node (`drawSoftRect` grows by `blur` on every side). The
buffer cropped them. At `alpha == 1.0` Compose stops compositing and the shadow reappeared, which is
why the end of the arrival read as a pop.

**Fix:** `compositingStrategy = CompositingStrategy.ModulateAlpha` — the alpha is applied per draw
call instead of to a buffer, so nothing is cropped and the shadow fades in with the card it belongs
to. `Observed:` the same three frames before and after; the notch is gone and the soft shadow is
present throughout.

Six instrumented tests and eight unit tests were green **across that defect**. It was found by
looking.

## 4 · The three things the brief said to watch

1. **It must not re-run on recomposition.** A block claims its slot through a `remember` in its own
   composition scope, keyed on the entrance instance. Recomposing reads the same claim back.
   Asserted at both layers: `GpEntranceTest` checks the counter does not move, and
   `EntranceAnimationUiTest.anOrdinaryStateChangeDoesNotReplayTheArrival` edits a value that is read
   in the same scope that builds the modifier — a read buried in a child would recompose the child
   alone and test nothing — then asserts the block has not moved on either of the next two frames.
2. **It must not fight the scroll. The trigger is SCREEN entry, not item entry** — stated here
   because the brief asked for it to be. The dashboard is a `LazyColumn`, so an item-entry trigger
   would animate every card the user ever scrolls to. `GpEntrance` answers `null` once
   `ENTRANCE_WINDOW_MS` (790 ms) has passed since the first block asked, and a block outside the
   window takes no slot, so it cannot inflate a later block's delay either. Within the window a late
   arrival **joins** the wave rather than restarting it: the elapsed time is subtracted from its
   delay.
3. **Reduce motion.** `rememberGpEntrance()` reads `Settings.Global.ANIMATOR_DURATION_SCALE`; a
   scale of zero makes every claim answer `null`, so there is no layer, no rise and no fade.

And the two the brief was right to warn about: `ChartAnimation.kt` was **read and not written** —
its `rememberChartProgress` is keyed on the *data*, which is precisely the flicker-on-every-edit
failure an entrance must not have, so the two cannot share a handle — and the widget pack is
untouched.

## 5 · 🧪 Tests

| layer | result |
|---|---|
| JVM unit (`:app:testDebugUnitTest`) | **729 passed, 0 failed, 0 skipped** (68 classes) — +8 in `ui/GpEntranceTest` |
| Instrumented (`adb install -r` + `am instrument`) | **204 passed, 0 failed** — +6 in `ui/EntranceAnimationUiTest`, +1 render pass |
| Security rules (`firestore-tests/`) | not run — this change touches no rule, no document and no read path |
| Cloud Functions | not run — same reason |
| Device render pass | 8 frames, `EntranceRenderPass`, looked at |

Both suites were re-run **after** the `ModulateAlpha` fix, not only before it.

### ⚠️ Two instruments that cannot see a Compose translation animation

Recorded because the first draft of the instrumented test was built on the first of them and it
failed in the *flattering* direction — it reported `0.0`, which reads as "no animation" rather than
as "wrong instrument".

- **`SemanticsNode.positionInRoot` does not follow a `graphicsLayer` translation.** It reported the
  same `y` during the arrival as at rest (`expected greater than 0.0 but was 0.0`) while the opacity
  assertion beside it passed.
- **`onNodeWithTag(…).captureToImage()` follows it too well.** The capture rectangle is taken in
  window coordinates, which *do* include the layer transform, so the image moves with the block:
  a probe measured `first=0` on every frame while watching the opacity change underneath it.

The working instrument is a **root** capture read at fixed pixel columns. And the stagger is
asserted as *which frame each block first appears on* rather than *how far down each block is at
time T*: the per-frame offsets are a handful of pixels apart, and the frame the animation actually
starts on carries a couple of frames of composition latency, so a T picked by arithmetic put the
third block before it had been let go.

## 6 · 📱 Seen on a device — twice, and not by screenrecord

The brief names `screenrecord` as the instrument (`kb/dev/android-device-verification.md` §6.2).
**It is not usable on this machine: `ffmpeg`/`ffprobe` are not installed**, so an `.mp4` cannot be
turned into frames or a timestamp list here, and this emulator's `screenrecord` v1.3 has no
`--output-format` to emit raw frames instead. `screencap` is also too slow to sample a 340 ms
animation — PNG and raw both, measured: a 24-frame burst over a cold open put the entire arrival
between two consecutive captures.

What was done instead, and both were **looked at**:

1. **`EntranceRenderPass`** — the clock frozen, one PNG per instant (0/32/48/64/96/144/208/800 ms)
   over a dashboard-shaped column of real `GpCard`s on the real page. Reading the strip in order is
   watching the arrival, at a resolution no recording of this emulator would give. This is what
   found the shadow defect in §3.
2. **The live dashboard**, re-entered from the Goals tab (which re-creates the screen and so
   re-fires the arrival) with a raw `screencap` burst decoded to PNG by hand. Two consecutive frames
   caught the wave in progress: in the first the hero is strongest, *Overall progress* fainter,
   *Smart add* fainter still and *Import from Google Tasks* barely present; in the second the first
   three have arrived and the fourth is still coming. That is the stagger, on the real screen, in
   the real app.

**No sign-in was needed and none was destroyed.** Every run used `adb install -r` + `am instrument`,
never `connectedDebugAndroidTest`; the `com.idomarhaim.goalpilot.debug` package was reinstalled in
place and never removed, and the device is still signed in. Animation scales were read **before** the
first device command and are unchanged at `1.0` / `1.0` / `1.0`.

## 7 · The brief's `owns:` was corrected before claiming

`/kickoff` step 2. The brief listed `ui/components/ChartAnimation.kt` as a write target; it is read,
not written, for the reason in §4. Added: `ui/components/Entrance.kt`, `ui/components/Common.kt`
(`SectionHeader` sits inside the same column and a motionless header in a rippling column reads as a
fault), `app/src/test/…/GpEntranceTest.kt` and `EntranceRenderPass.kt`.

## 8 · 📥 Ingested (AUTO MODE)

Four candidates were flagged; three drained at the commit trigger into the central bundle
(`C:\Dev\JARVIS`, commit `c211c59`, lint CLEAN over 109 pages), and one is held.

- 📥 **Ingested:** two instruments that cannot see a Compose translation animation →
  `kb/dev/android-device-verification.md` §14
- 📥 **Ingested:** `graphicsLayer { alpha < 1 }` crops anything drawn outside the node →
  `kb/dev/compose-soft-shadows-below-api-28.md`
- 📥 **Ingested:** a brief's citation is a pointer to an instrument, not a substitute for running it
  → `kb/dev/look-at-your-own-output.md` §4l
- ⛔ **Held, always-ask:** *`screenrecord` is not a usable instrument on this machine.* It rewrites
  `android-device-verification.md` §6.2 in place, and overwriting committed knowledge is a deletion.
  Parked whole in `kb-candidates/2026-08-22-57d-entrance-animation.md` under
  *Standing — always-ask*, with the substitute that worked, so nothing is lost if this session ends
  first. **This is the one thing on this brief that needs Ido's word.**
