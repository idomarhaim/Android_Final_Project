# tour-video — a full-app screen recording, and the prompt that turns it into a narrated explainer

> **Summary:** Recorded a continuous 1080×2400 screen recording of the whole app on the AVD — every screen, the AI, and task scheduling included — delivered it to `C:\Users\namei\Videos\GoalPilot-Tour\`, and wrote `docs/marketing/explainer-video-brief.md`: the problem framing, the narration script, per-beat timecodes, and the tool prompts. **Google Flow cannot do the job Ido planned** — its documented ceiling on an uploaded video is 60 s, forcibly trimmed to 30 s, of which only a 10 s segment is usable — so OpenArt Director is the recommended route and Flow is scoped to the opening b-roll.

**Date:** 2026-08-22 · **Session:** `tour-video` · **Mode:** AUTO

## What was asked

Ido asked for three things: a screen recording of the emulator walking through a tutorial
of the whole app — *"all the components, including scheduling in the calendar"* — the file
downloaded to his machine, and a prompt he could paste into Google Flow beside that video
so it would produce a narrated explainer covering the need the app answers and a full
explanation of it. He named OpenArt Director as the fallback *"if there is a length
problem with Google Flow"*, and said OpenArt has an MCP where Flow does not.

## What shipped

### 1 · The recording

`C:\Users\namei\Videos\GoalPilot-Tour\GoalPilot-full-tour.mp4` — one continuous take, no
cuts, 1080×2400 H.264, silent, of the shipping app signed in to Ido's real account.
Nothing is mocked. It covers, in order: cold launch · the points card and overall progress
· **Smart add a task** typed in plain English and filed by the AI under *Learn to play the
saxophone* · Google Tasks import · Health Connect · the AI coach · weekly-progress sharing
· goals grouped by life area · one goal in full, with the AI estimating a task's difficulty,
duration and points · **scheduling that task to a date and then to a deadline time** · life
areas · analytics (day/week/month/quarter/year, the time-share donut and the stacked bar)
· the social leaderboard, friends feed and a challenge's standings · profile and friend
code · four materials, backgrounds and colour themes in Settings · and the app's own
seven-step guided tour, replayed from Settings.

### 2 · The brief

[`docs/marketing/explainer-video-brief.md`](../../docs/marketing/explainer-video-brief.md)
— the problem the video has to land, the full narration script act by act, the OpenArt
Director prompt, five Google Flow clip prompts for the opening b-roll, and a manual-assembly
fallback. [`docs/marketing/tour-timecodes.md`](../../docs/marketing/tour-timecodes.md) maps
every beat in the recording to its second.

## The finding that changed the plan

**Google Flow cannot ingest this footage, and the wall is much lower than "the video is too
long".** From [Flow's own help page](https://support.google.com/labs/answer/16935718): an
uploaded video may be at most **60 seconds** and 1 GB; anything over **30 seconds** must be
trimmed to 30; and within that only a **≤ 10-second segment** can be used. `Extend` is
documented as working on **Veo-generated video only**, never on yours. So a multi-minute
product tour has no path into Flow at all, and even a one-minute cut survives as a ten-second
fragment.

Flow is a *generative* tool. It will not read a screen recording and narrate what it sees.
What it is genuinely good for here is the **cinematic opening** — the 15–20 seconds
dramatising the problem before the phone appears — and §5 of the brief carries five clip
prompts for exactly that.

**OpenArt Director** is the recommended route for the assembly: five-minute output in one
piece, a timeline, and ElevenLabs voiceover.
`Untested:` OpenArt's copy says you may give Director *"even a video to guide it"*, which
reads as style guidance rather than *"cut my footage into the timeline"*; nobody here has
run it, and the brief says so and carries a manual fallback.

**There is no OpenArt MCP available to this session.** The only MCP server configured for
this repo is `graphify-android-final-project` (`.mcp.json`), and the user-level list is
empty. Driving OpenArt from here is not currently possible.

## 🧪 Tests

**No code changed, so no test layer applies** — this session wrote two Markdown files and
produced a video. The Kotlin, instrumented, Firestore-rules and functions suites were not
run and are not affected; nothing in `app/`, `functions/` or `firestore-tests/` was touched.

What *was* verified, and how:

- **The choreography was dry-run twice before the take**, screenshotting all 63 beats, and
  both runs found real defects that would have ruined a blind recording:
  1. the floating IME toolbar means the AVD reports a **hardware keyboard**, so a Back press
     after typing does not dismiss a keyboard — it **exits the app**. In the first dry run
     the next tap landed on the launcher and opened **YouTube**. Fixed by committing the
     text with the toolbar's own tick at `(107,1436)` instead.
  2. reaching *Replay tutorial* by a fixed number of swipes lands on a scroll offset that
     flings differently every run; the second dry run missed it and the last nine beats
     recorded the Settings screen instead of the tour. Fixed by **bottoming the list out**
     and tapping the stable `(346,1144)`.
- **The recording itself was probed after the take** (`ffprobe`) for duration, resolution
  and codec, and spot-checked frame by frame rather than assumed.

## Device state

📱 **No sign-in was needed and none was destroyed.** The AVD was already signed in
(`FIREBASE_USER` present) and still is. No install, no uninstall, no `connectedDebugAndroidTest`,
no Gradle. Animation scales untouched at `1.0`.

Two device settings were changed and **both are restored**: SystemUI **demo mode**
(a clean 9:00 clock, full wifi and battery, no notification icons) was entered for the
recording and exited afterwards, and `secure show_ime_with_hard_keyboard` was toggled and
put back to `1`.

## What was left in Ido's account

The demo was performed against real data, so it left real data. Nothing was deleted —
deletions are always-ask and this is his account:

- **two tasks** on *Learn to play the saxophone* named "Practice saxophone for 20 minutes on
  Sunday" (the smart-add demo, run in the take and once in a dry run), and
- **two or three tasks** named "Write the project book chapter" on *Submit Android final
  project*, one of them scheduled for **Aug 24, 2026, 8:00 PM**.

None is marked done, so **no points moved** and the leaderboard is unchanged. They are one
tap each to delete.

## Defect observed, not caused

⚠️ **`Strength Training` reads `245613/100` — 245613%** on the Goals screen, and the number
**grew during the session** (245358 → 245612 → 245613). `Sleep 7 hours` reads `165.5/100`.
`Inferred:` a Health Connect metric — a raw step count — is being written as progress
against a 0–100 measure rather than being scaled to it. It is visible in the recording.
`Untested:` not traced to a code path; this session did not read the Health sync, and the
finding is filed rather than fixed because it is outside this task.

## Privacy note for Ido, before this goes anywhere public

The recording contains, on screen and legible: **`name.iddo@gmail.com`**, his friend code
**`NDXVJC`**, and a **friend's real name**. That is fine for a course submission and is not
fine for a public upload without a decision. The Profile beat's timecode is in
`tour-timecodes.md` so the region can be blurred or the beat trimmed in one pass.
