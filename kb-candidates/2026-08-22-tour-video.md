# KB candidates — session `tour-video`, 2026-08-22

Every entry stands alone: another session's transcript is not a source, so each carries its
own Claim, Why, Destination, Anchors, Supersedes and Status.

---

## 1 · Google Flow cannot ingest a product screen recording — 60 s in, 10 s usable

**Claim.** [Google Flow](https://labs.google/fx/tools/flow) accepts an uploaded video of at
most **60 seconds** and **1 GB**, in `.mov` / `.mp4` / `.avi` / `.wmv`; a video longer than
**30 seconds must be trimmed to 30**; and within that only a **≤ 10-second segment** can be
used for editing. `Extend` is documented as operating on **Veo-generated video only**, never
on an upload. So a multi-minute screen recording has **no path into Flow at all**, and Flow
cannot be used to narrate footage you already have. Flow is a *generative* tool — it makes
new footage from prompts, reference images and start/end frames.

**Why this is worth a page.** The intuition — *"it is Google's video AI, it will take my
video and narrate it"* — is wrong in a way the marketing does not correct, and the failure
is discovered only after the footage has been produced. Ido asked for exactly this workflow
on 2026-08-22 and pre-emptively asked *"tell me if there is a length problem"*; the real
limit is not length but **input type**, and it would have cost a whole session to find by
trying. **What was rejected:** believing the aggregator blogs, several of which state flatly
that *"Google Flow supports uploading your own video footage to combine with AI-generated
clips"* — true only in the ten-second-fragment sense above, which is not what a reader takes
from that sentence. The official help page is the only source that gives the numbers.

**What to use instead**, for the *assembly* job: **OpenArt Director** — up to **five minutes**
of output in one piece, a timeline, and ElevenLabs voiceover and captions. What Flow *is*
still right for: generating **b-roll** for the cinematic opening, which has no screen
recording behind it and is one 8-second Veo clip per shot.

`Untested:` whether OpenArt Director places an **uploaded video into the finished cut** or
only uses it as **style/motion guidance**. Its own copy says you may give it *"even a video
to guide it"*, which reads as the latter. Nobody has run it.

**Destination.** `kb/dev/tool-adoptions.md` — a new *"AI video tools"* row, or a page of its
own if the section grows.
**Anchors.** [Flow help — *Edit videos & build scenes*](https://support.google.com/labs/answer/16935718) ·
[OpenArt Director](https://openart.ai/features/director/) ·
`C:\Dev\Android_Final_Project\docs\marketing\explainer-video-brief.md`
**Supersedes.** nothing.
**Status.** pending ingest.

---

## 2 · This AVD reports a HARDWARE keyboard, so `input keyevent 4` after typing EXITS THE APP

**Claim.** On `emulator-5554` (`Pixel`-class, Android 15) Gboard shows a small **floating
toolbar** rather than an on-screen keyboard when a text field is focused, because the AVD
advertises a hardware keyboard. There is therefore **no IME window for Back to dismiss**, and
`adb shell input keyevent 4` after `input text` is consumed as ordinary navigation — it pops
the back stack and, from a root screen, **leaves the app**. Toggling
`settings put secure show_ime_with_hard_keyboard 0/1` does **not** change this; disabling
Gboard only hands the field to the voice IME, and disabling that re-enables Gboard.

**The fix that works:** commit the text with the floating toolbar's own **tick button**, at
`(107, 1436)` on a 1344 × 2992 screen. It closes the toolbar, keeps the typed text, keeps
the field focused, and navigates nowhere.

**Why.** The failure is silent and it does not look like a keyboard problem. `Observed:`
2026-08-22 — a scripted UI walk typed a sentence, pressed Back, and the **next tap landed on
the launcher and opened YouTube**; nine subsequent screenshots were of the wrong app, and the
run reported success (exit 0) throughout. Anything driving this AVD by `adb` — a recording,
a demo, a scripted repro — hits it the first time it types into a field.

**Destination.** `kb/dev/android-device-verification.md`, beside §8's `adb install -r` note.
**Anchors.** `docs/marketing/explainer-video-brief.md` · `CHANGELOG/2026-08-22/tour-video.md`
**Supersedes.** nothing.
**Status.** pending ingest.

---

## 3 · Reaching a control by a fixed number of swipes is unreliable — bottom the list out instead

**Claim.** In a scripted UI walk, locating a control by *"scroll N times, then tap (x, y)"*
fails intermittently, because a Compose scroll **flings** and the same swipe lands on a
different offset run to run. The reliable form is to **scroll the list until it stops** — five
short swipes rather than three long ones — and then tap the control's position **relative to
the bottomed-out state**, which is fixed.

**Why.** `Observed:` 2026-08-22 — *Replay tutorial* in GoalPilot's Settings sat at
`y = 2802` before the last swipe and at `y = 1428` after it; the script tapped `1768`, hit
nothing, and the final **nine beats of a 63-beat run recorded the wrong screen**. Bottomed
out, it is at `(346, 1144)` every time. The failure mode is the point: the tap **succeeds**
(it lands on the scrim or on inert text), the script exits 0, and only a screenshot shows
the walk went somewhere else — which is `look-at-your-own-output.md`'s thesis in a new place.

**Destination.** `kb/dev/android-device-verification.md` (UI-automation section), with a
cross-link from `kb/dev/look-at-your-own-output.md`.
**Anchors.** `CHANGELOG/2026-08-22/tour-video.md`
**Supersedes.** nothing.
**Status.** pending ingest.

---

## 4 · `screenrecord` recipe for a presentable product recording: no time limit, demo mode, and CFR afterwards

**Claim.** Three things turn `adb shell screenrecord` into footage that can be handed to an
editor or an AI video tool:

1. **`--time-limit 0` removes the 180-second cap** (`screenrecord v1.3`, Android 15). One
   continuous 6:40 take needs no stitching, which removes every concat and timing problem.
2. **SystemUI demo mode** gives a clean status bar — a fixed clock, full wifi, full battery,
   no notification icons — and is fully reversible:
   ```bash
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide -e wifi show -e level 4 -e fully true
   adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
   # ... and afterwards
   adb shell am broadcast -a com.android.systemui.demo -e command exit
   adb shell settings put global sysui_demo_allowed 0
   ```
3. **The output is variable frame rate and that matters.** `screenrecord` emits a frame only
   when the screen changes: `Observed:` a 400.7-second recording contained **1 676 frames**
   — an average of 4.2 fps. `ffprobe` reports `r_frame_rate=1/0` and no duration on a mostly
   static clip. Re-encode before handing it on:
   `ffmpeg -i in.mp4 -fps_mode cfr -r 30 -c:v libx264 -crf 20 -pix_fmt yuv420p -movflags +faststart out.mp4`
   → 12 022 frames over the same 400.73 s.

**Why.** §6.2 of the device page already says `screenrecord` is the wrong instrument for a
**short animation** (it falls back to 720 × 1280 and a 340 ms animation is ~20 frames). This
is the opposite case — a **long product walkthrough**, where it is the right instrument and
these three details are what make the output usable. Note `-fps_mode passthrough` is the
2026 replacement for the removed `-vsync 0`, already recorded on that page; `-fps_mode cfr`
is its sibling and is what this case wants.

**Destination.** `kb/dev/android-device-verification.md` §6.2, as a second recipe beside the
frame-by-frame one.
**Anchors.** `docs/marketing/tour-timecodes.md` · `CHANGELOG/2026-08-22/tour-video.md`
**Supersedes.** nothing — it **extends** §6.2 rather than contradicting it.
**Status.** pending ingest.

---

## 5 · A blind take can still be timecoded — model the script, then scale to the real duration

**Claim.** When a UI walk is recorded as one continuous take with no per-beat clock, the
beat timecodes can be rebuilt afterwards by **summing the choreography script's own sleeps**
plus a constant per-`adb`-call overhead, then **linearly scaling** the model so its total
lands on the video's measured duration. `Observed:` 2026-08-22 the model predicted **414.3 s**
against a real **400.7 s** — **3.4 % out** before scaling — and three scaled timecodes,
checked by extracting that exact frame and looking at it, all landed on the predicted beat.
Accurate to about ± 1 s: enough to place a narration line, not enough to cut on a frame.

**Why, and the cheaper thing to do next time.** The reconstruction exists because the take
was recorded without timestamps; **logging `date +%s.%N` inside the `beat` function costs
nothing and removes the need for all of it.** Record the claim anyway, because the
reconstruction is what rescues a take you have already shot — and because the parser had a
defect worth remembering: written to match one command per line, it found **2 beats out of
63**, since the script chains `tap …; beat "…"` on one line. Split on `;` and process each
fragment in order.

**Destination.** `kb/dev/android-device-verification.md` (UI-automation section), or
`kb/dev/look-at-your-own-output.md` for the parser half — the defect is a verification
failure that reported success.
**Anchors.** `docs/marketing/tour-timecodes.md`
**Supersedes.** nothing.
**Status.** pending ingest.
