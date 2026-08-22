# KB candidates — `57d-entrance-animation`, 2026-08-22

Repo: `C:\Dev\Android_Final_Project` · issue [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57) d

---

## 1 · Neither of the two obvious instruments can measure a Compose `graphicsLayer` translation

**Claim.** A Compose animation implemented as `Modifier.graphicsLayer { translationY = … }` changes
no layout, and the two instruments a test reaches for both fail on it — in **opposite** directions,
so neither looks wrong until it is run:

- `SemanticsNode.positionInRoot` (and `getBoundsInRoot`) **does not follow the layer**. `Observed:`
  2026-08-22, `EntranceAnimationUiTest` first draft — two offset assertions failed with
  `expected to be greater than: 0.0 but was: 0.0` on frames where the opacity assertion sitting
  beside them passed.
- `onNodeWithTag(…).captureToImage()` **follows it too well**. The capture rectangle is taken in
  window coordinates, which *do* include the layer transform, so the captured image moves with the
  node and its top row is the node's top row on every frame. `Observed:` a probe logged `first=0`
  at every one of ten frames while the same probe's centre-pixel green went `1.0 → 0.79 → 0.60 →
  0.44 → 0.31 → 0.22 → 0.16 → 0.0`.

The instrument that works is a **root** capture (`onRoot().captureToImage()`) read at fixed pixel
columns, because it is the only one of the three with a stationary frame of reference.

Two corollaries worth the same page:

- **Opacity is measurable by either node capture or root capture**; only the *position* is the
  problem. So a test that asserts only the fade passes against a build that has lost the rise.
- **Assert the stagger as "which frame does each block first appear on", not "how far apart are
  they at time T".** `Observed:` at T = 96 ms the three offsets were 7 / 13 / 29 px in a probe that
  stepped one frame at a time, but the same T reached by a single `advanceTimeBy(96)` put the third
  block at *not yet visible* — the animation start carries a couple of frames of composition
  latency, and a T picked by arithmetic lands on the wrong side of it. The frame index needs no T.

**Why (and what was rejected).** Rejected: giving up and asserting only the settled state (that
passes against a build with no animation at all, which is the defect `#57` d was opened about);
rejected: `assertIsDisplayed`, which says nothing about alpha or offset.

**Destination.** `kb/dev/` — either a new `compose-animation-testing.md` or a section of
`android-device-verification.md`. Prefer the latter: it is already the page this repo's sessions are
told to read before a device command, and §6 is explicitly about *instruments that answer where
pixels cannot*.

**Anchors.** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/EntranceAnimationUiTest.kt` (the
class comment records all three instruments); `CHANGELOG/2026-08-22/57d-entrance-animation.md` §5.

**Supersedes.** Nothing.

**Status.** Ready.

---

## 2 · `screenrecord` is not a usable instrument on this machine, and §6.2 says it is

**Claim.** `kb/dev/android-device-verification.md` §6.2 prescribes `screenrecord` + `ffprobe` +
`ffmpeg -vsync 0` as *the* way to verify a motion feature. **On this machine that path is dead**, and
a session that follows it discovers so only after recording:

- `ffmpeg` and `ffprobe` are **not installed** (`which` finds neither), so the `.mp4` cannot be
  turned into a timestamp list or into frames.
- This emulator's `screenrecord` is **v1.3 and has no `--output-format`** — the raw-frames escape
  hatch some builds offer is not there.
- `screencap` is **too slow to sample a 340 ms animation**, in either format. `Observed:` a 24-frame
  PNG burst across a cold open produced 3 pre-load frames and then 16 byte-identical settled ones —
  the whole arrival fell between two consecutive captures. A 14-frame **raw** burst (16 MB/frame,
  no PNG encode) did no better on a cold open.

**What works instead**, and it is the thing this repo already has a pattern for: a **frozen-clock
render pass** — `mainClock.autoAdvance = false`, one `captureToImage()` per chosen instant, written
as PNGs to the app's files dir. Reading the strip in order is watching the animation, at a
resolution no recording of this emulator gives. `Observed:` `EntranceRenderPass` (0/32/48/64/96/144/
208/800 ms) is what found a shadow-clipping defect that eight unit tests and six instrumented tests
were all green across.

The one thing a render pass does **not** prove is that the real screen is wired to the animation.
For that: re-enter the screen by tab navigation rather than a cold open — it re-creates the
composable, so the arrival fires on a moment you control to within one `input tap` — and burst raw
`screencap`s. `Observed:` two consecutive frames of the live dashboard caught the wave in progress.
A raw frame is trivially decodable by hand (16-byte header: `w`, `h`, `format`, `colorSpace`, then
RGBA8888 rows) and a PNG can be written from Python with `zlib` + `struct` alone, which matters
because **PIL is not installed either**.

**Why (and what was rejected).** Rejected: installing `ffmpeg` (`winget` hangs on elevation here —
already recorded in `CLAUDE.md` for `GitHub.cli`, same failure mode) — that is a machine change and
Ido's call, not a session's. Rejected: slowing the animation down for the capture.
`Inferred:` Compose animations are driven by a `MonotonicFrameClock` fed by the real frame
callback, so unlike a platform `ValueAnimator` they are not scaled by
`Settings.Global.ANIMATOR_DURATION_SCALE` and there is no device-side way to stretch one. `Untested:`
this was reasoned from how the clock is fed, not measured — the check would be to set the scale to
10 and re-run the burst, and it was not worth the device state it would have changed.

**Destination.** `kb/dev/android-device-verification.md` §6.2 — as an amendment in place, not a new
page. §6.2 currently reads as an available instrument and is not.

**Anchors.** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/EntranceRenderPass.kt`;
`CHANGELOG/2026-08-22/57d-entrance-animation.md` §6.

**Supersedes.** Narrows `kb/dev/android-device-verification.md` §6.2's *"`screenrecord` is the
instrument"* — the technique is sound and the tooling is absent. **This one contradicts a standing
KB claim, so it is always-ask in both modes** (`memory-promotion.md`).

**Status.** ⛔ Always-ask — held for Ido. It rewrites a standing claim in place.

---

## 3 · `graphicsLayer { alpha < 1 }` crops anything drawn outside the node's bounds

**Claim.** Compose's default compositing strategy puts a sub-1 alpha into an offscreen buffer the
size of the node, and **anything a `drawBehind` draws beyond that node is cropped**. A shadow is
exactly that thing. `CompositingStrategy.ModulateAlpha` applies the alpha per draw call instead and
crops nothing.

`Observed:` 2026-08-22, GoalPilot. `Modifier.gpSurface` draws the app's neo shadow pair as
concentric round rects grown by `blur` on every side (`drawSoftRect` in
`ui/theme/MaterialSpec.kt`). Fading a `GpCard` in through a default `graphicsLayer` made every card
render **flat with a hard dark square at its bottom-right corner** for the whole 340 ms, its real
shadow appearing in a single frame at `alpha == 1.0` — the point at which Compose stops
compositing, which is why the end of the animation read as a pop. Same three render-pass frames
before and after the strategy change: notch gone, soft shadow present throughout.

**The generalisable half:** this bites any design system whose depth is drawn *outside* the widget,
which is most of them (shadow, glow, halo, focus ring). It is invisible in a settled screenshot and
invisible to every semantics-level assertion, so **only a mid-animation frame finds it** — which
makes it a good argument for candidate 2's render-pass strip rather than a curiosity about
`CompositingStrategy`.

**Why (and what was rejected).** Rejected: padding the card so the layer bounds contain the shadow —
that changes layout at 30-odd call sites to fix a rendering artefact. Rejected: `clip = false` on
the layer — it is already the default and does not help; the crop is the offscreen buffer's extent,
not a clip.

**Destination.** `kb/dev/` — a Compose/Android rendering page. Fits beside candidate 1 if that one
becomes `compose-animation-testing.md`; otherwise its own short page.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/ui/components/Entrance.kt` (the comment at
the `graphicsLayer` call records the whole finding);
`CHANGELOG/2026-08-22/57d-entrance-animation.md` §3.

**Supersedes.** Nothing.

**Status.** Ready.

---

## 4 · A brief can be wrong about the artefact it is quoting, and reading the source is what catches it

**Claim.** `sessions/57d-entrance-animation.md` states that the prototype's `.34s` / `.36s` / `.38s`
durations **are** the stagger, and warns that shipping one shared duration gives "something that is
technically a fade-in and visibly not this". `Observed:` those three values are at lines 285, 322 and
350 of `docs/prototypes/2026-08-10-calendar-surface/index.html` and belong to three *different
element classes* — an event chip, an agenda row, the hero deck — each animating **once**. What
sequences a list there is `animation-delay:${k*40}ms` / `${i*45}ms`, emitted by that file's own
render functions at lines 693 and 796. Implementing the brief literally produces exactly the
non-visible result it warns against: three blocks that start together and finish 40 ms apart.

**Why this is worth a page and not just a changelog line.** The brief was **right about the method**
and wrong about the reading: *"Read the exact values out of the prototype rather than approximating
them"* is the instruction that caught its own error. That is the transferable part — a brief's
citations are a starting point for a grep, not a substitute for one — and it is the second time in
two sessions on `#57` that a brief's table of prototype facts was wrong on one row (`57c` found
`feSpecularLighting` deleted by a later prototype; changelog 2026-08-22).

**Why (and what was rejected).** Rejected: silently implementing the correct thing — a future
session reading the brief would re-derive the same error. Rejected: editing the brief's prose — it
is a closed work order and the correction belongs where it can be found, which is the code comment
and the changelog.

**Destination.** `kb/dev/` — `look-at-your-own-output.md` is the nearest existing home (same family:
a check that passes for the wrong reason), or a short page on reading design prototypes. **Not
`rules/`** — this is an observation about briefs, not a change to how anyone behaves.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/ui/components/Entrance.kt` (the ⚠️ block in
the class comment); `CHANGELOG/2026-08-22/57d-entrance-animation.md` §2.

**Supersedes.** Nothing.

**Status.** Ready.
