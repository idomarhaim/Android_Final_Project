# KB candidates — `57d-entrance-animation`, 2026-08-22

Repo: `C:\Dev\Android_Final_Project` · issue [#57](https://github.com/idomarhaim/Android_Final_Project/issues/57) d

**Drained 2026-08-22** by `/kb-ingest` — entries **1**, **3** and **4** landed in the central bundle
(`C:\Dev\JARVIS`, commit `c211c59`) and their original text is gone from here, per
`/kb-ingest` §7.5. Where they went:

| was | landed |
|---|---|
| 1 · Compose animation instruments | `kb/dev/android-device-verification.md` §14 |
| 3 · `graphicsLayer` alpha crops out-of-bounds draws | `kb/dev/compose-soft-shadows-below-api-28.md` |
| 4 · A brief's citation is not the artifact | `kb/dev/look-at-your-own-output.md` §4l |

Journal entry: `kb/log/2026-08-22.md`. **This file is kept, not deleted**, because one entry
survives — a partial drain is rewritten down to its survivors and never removed.

---

## Standing — always-ask

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

**Status.** ⛔ Always-ask — held for Ido, **2026-08-22**. It rewrites a standing claim in place
(`/kb-ingest` §3's rewrite is a deletion of committed knowledge), so neither mode drains it.
Everything needed to write the amendment is above; do not go looking for a transcript.
