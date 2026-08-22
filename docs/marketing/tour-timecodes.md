<!-- Written 2026-08-22 by session `tour-video`. Generated, then spot-checked against the video. -->

# `GoalPilot-full-tour.mp4` — beat timecodes

Every beat in the recording and the second it happens, so a narration line from
[`explainer-video-brief.md` §3](explainer-video-brief.md#3--the-narration-script) can be
placed without scrubbing.

| | |
|---|---|
| **Duration** | `6:40.7` (400.73 s) |
| **Picture** | 1080 × 2400, H.264, **constant 30 fps**, 12 022 frames, 21 MB |
| **Audio** | none — the recording is silent by design; the narration is added by the tool |
| **Source** | `GoalPilot-full-tour-raw.mp4` beside it is the untouched `screenrecord` output. It is **variable-frame-rate** (1 676 frames over the same 400.7 s — `screenrecord` emits a frame only when the screen changes), which some editors and most AI tools mishandle. Use the CFR file; the raw one is kept as the original. |

## How these numbers were produced, and how far to trust them

The take was recorded blind — one continuous `screenrecord` with no per-beat clock — so
each timecode is **reconstructed** from the choreography script's own sleeps plus a
measured per-`adb`-call overhead, then scaled so the model's total lands on the video's
real duration. The model came out **414.3 s against a real 400.7 s before scaling — 3.4 %
out**, which is what makes the scaled result trustworthy.

`Observed:` three timecodes were checked by extracting that exact frame from the finished
file and looking at it — `0:44.6` shows the *"Added to "Learn to play the saxophone""*
snackbar, `3:21.7` shows the Year donut reading 67 / 20 / 13 %, and `5:39.5` shows *Step 1
of 7*. All three matched.

**Treat them as ± 1 second.** They are accurate enough to place a narration line or find a
section, and not accurate enough to cut on a single frame.

| # | timecode | on-screen beat |
|---|---|---|
| 1 | `0:07.1` | launcher |
| 2 | `0:10.4` | splash |
| 3 | `0:18.1` | dashboard lands |
| 4 | `0:22.0` | points card / level |
| 5 | `0:25.9` | overall progress |
| 6 | `0:28.0` | field focused |
| 7 | `0:36.5` | sentence typed |
| 8 | `0:37.9` | keyboard away |
| 9 | `0:40.7` | Sort tapped |
| 10 | `0:42.7` | AI thinking |
| 11 | `0:44.6` | filed under the right goal |
| 12 | `0:51.7` | Google Tasks import |
| 13 | `0:57.8` | Health Connect |
| 14 | `1:03.9` | AI coach |
| 15 | `1:10.5` | your goals |
| 16 | `1:16.6` | share weekly progress |
| 17 | `1:22.8` | goals tab |
| 18 | `1:29.9` | more goals |
| 19 | `1:40.6` | goal detail |
| 20 | `1:46.4` | task field |
| 21 | `1:52.6` | task typed |
| 22 | `1:55.8` | AI estimate tapped |
| 23 | `2:01.6` | difficulty and minutes filled in |
| 24 | `2:07.4` | date picker |
| 25 | `2:12.9` | Monday chosen |
| 26 | `2:17.4` | date set |
| 27 | `2:23.1` | time picker |
| 28 | `2:28.4` | 8 o'clock |
| 29 | `2:32.6` | deadline -- Aug 24, 8:00 PM |
| 30 | `2:39.3` | task added with its points |
| 31 | `2:46.4` | profile tab |
| 32 | `2:52.4` | life areas |
| 33 | `2:59.5` | unfiled goals |
| 34 | `3:08.6` | analytics -- where your time goes |
| 35 | `3:15.9` | month |
| 36 | `3:21.7` | year -- three life areas |
| 37 | `3:30.7` | how it moves |
| 38 | `3:41.0` | social -- leaderboard |
| 39 | `3:48.0` | friends feed |
| 40 | `3:58.1` | challenges |
| 41 | `4:04.7` | standings |
| 42 | `4:14.7` | profile -- friend code |
| 43 | `4:23.7` | account sheet |
| 44 | `4:29.4` | settings |
| 45 | `4:35.1` | Glass |
| 46 | `4:40.9` | Liquid glass |
| 47 | `4:46.6` | Soft dark |
| 48 | `4:52.8` | back to Soft |
| 49 | `4:57.8` | Glow background |
| 50 | `5:02.7` | Match background |
| 51 | `5:07.9` | colour themes |
| 52 | `5:14.0` | language and region |
| 53 | `5:20.6` | your day |
| 54 | `5:32.3` | help -- replay tutorial |
| 55 | `5:39.5` | tour step 1 |
| 56 | `5:46.6` | tour step 2 |
| 57 | `5:53.8` | tour step 3 |
| 58 | `6:01.0` | tour step 4 -- your move |
| 59 | `6:08.2` | tour step 5 |
| 60 | `6:15.3` | tour step 6 |
| 61 | `6:22.5` | tour step 7 |
| 62 | `6:30.2` | tour done |
| 63 | `6:35.9` | home again |

## The section boundaries, for cutting

| section | from | to | narration act |
|---|---|---|---|
| Cold open — launch and the home screen | `0:00` | `0:28` | Act 1 |
| Smart add a task (the AI) | `0:28` | `0:51` | Act 2 |
| The rest of the home screen | `0:51` | `1:23` | Act 3 |
| Goals, grouped by life area | `1:23` | `1:40` | Act 4 |
| One goal — tasks, effort, points, scheduling | `1:40` | `2:46` | Act 5 |
| Life areas | `2:46` | `3:08` | Act 6 |
| Analytics | `3:08` | `3:41` | Act 7 |
| Social, feed and challenges | `3:41` | `4:14` | Act 8 |
| Profile and friend code | `4:14` | `4:29` | Act 9 |
| Settings — materials, colour, your day | `4:29` | `5:39` | Act 10 |
| The app's own seven-step guided tour | `5:39` | `6:40` | Act 11 |

## ⚠️ The two beats to look at before this is published anywhere

- **`4:14.7` – `4:29.4`, Profile.** On screen and legible: **`name.iddo@gmail.com`** and the
  friend code **`NDXVJC`**.
- **`3:41.0` – `4:14.7`, Social.** A **friend's real name** appears in the leaderboard, in
  the standings, and beside a shared photo.

Fine for a course submission. For anything public, either trim those beats or blur the
region — both are one pass in any editor, and this table is where the timecodes for it are.
