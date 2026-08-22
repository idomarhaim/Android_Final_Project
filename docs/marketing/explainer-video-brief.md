<!-- Written 2026-08-22 by session `tour-video`. -->

# GoalPilot explainer video — the brief, the script, and the tool prompts

This file is the **one place** the narrated explainer video is specified: what problem
the app solves, what the narration says, which second of the screen recording each line
sits over, and the exact prompt to paste into the tool that assembles it.

**The footage it refers to** is `GoalPilot-full-tour.mp4` — a single continuous, silent
screen recording of the real app on a Pixel-class emulator, signed in to a real account
with real data. It is not a mock-up and nothing in it is staged beyond the choice of what
to tap. Location and length are in [§1](#1--the-footage).

---

## ⚠️ Read this first — Google Flow cannot do this job

The plan this file was commissioned for was: *upload the screen recording to
[Google Flow](https://labs.google/fx/tools/flow), paste a prompt, get a narrated
explainer back.* **Flow's documented limits make that impossible**, and the wall is far
lower than a "length problem":

| Flow's limit on an uploaded video | Source |
|---|---|
| **60 seconds** maximum length | [Flow help — *Edit videos & build scenes*](https://support.google.com/labs/answer/16935718) |
| **1 GB** maximum size, `.mov` `.mp4` `.avi` `.wmv` | same |
| Longer than **30 s** → you are forced to trim it to 30 s | same |
| Inside that, only a **≤ 10-second segment** can actually be used | same |
| **Extend works only on Veo-generated video**, never on yours | same |

So a multi-minute product tour cannot enter Flow at all, and even a 60-second cut is
reduced to a ten-second fragment. Flow is a **generative** tool — it makes new footage
from prompts and reference images. It is not an editor for footage you already have, and
it will not read a screen recording and describe what is in it.

**What Flow *is* still good for here**, and the recommended use: generating the
**cinematic opening** — the 15–20 seconds that dramatise the *problem* before the phone
appears. That part has no screen recording behind it, is pure b-roll, and is exactly what
Veo does well. Prompts for it are in [§5](#5--google-flow-prompts--the-opening-b-roll-only).

**The tool that can do the whole job is [OpenArt Director](https://openart.ai/features/director/)**
— it generates up to **five minutes** in one piece, has a timeline, and does voiceover
(ElevenLabs) and captions. Its prompt is in [§4](#4--openart-director-prompt--the-primary-route).

> ⚠️ `Untested:` OpenArt's own copy says you can give Director *"even a video to guide
> it"*, which reads as **style/motion guidance**, not *"cut my footage into the
> timeline"*. Nobody here has run it. If Director turns out to only *reference* the
> upload rather than *include* it, the fallback is in [§6](#6--if-neither-tool-will-carry-your-own-footage)
> and it is not a bad outcome — it is fifteen minutes in any editor.

---

## 1 · The footage

Three files, all in `C:\Users\namei\Videos\GoalPilot-Tour\` — outside the repo, because they
are binaries.

| file | what it is |
|---|---|
| **`GoalPilot-full-tour.mp4`** | **the deliverable.** `6:40.7`, 1080 × 2400, H.264, **constant 30 fps**, 21 MB, silent. Use this one. |
| `GoalPilot-full-tour-raw.mp4` | the untouched `screenrecord` output, same picture and length but **variable frame rate** (1 676 frames, because `screenrecord` emits a frame only when the screen changes). Kept as the original; some editors and most AI tools mishandle VFR, which is why the CFR file exists. |
| `GoalPilot-highlights-60s.mp4` | a **60-second** chronological cut of seven moments — home, smart add, a goal, scheduling, the analytics donut, the theme switch. Deliberately skips the Profile and Social beats, so it carries **no email, no friend code and nobody else's name**. |

| | |
|---|---|
| **Recorded** | 2026-08-22, `emulator-5554` (`sdk_gphone64_x86_64`, Android 15), app `v0.3.2` |
| **Account** | Ido's real account — 9 goals, 4 life areas, a friend, a live leaderboard |
| **Beat map** | [`tour-timecodes.md`](tour-timecodes.md) — all 63 beats with their second |

⚠️ **Before this goes anywhere public**, read the last section of `tour-timecodes.md`: the
full tour shows Ido's email address, his friend code, and a friend's real name, and it names
the two timecode ranges where. The 60-second cut does not.

The status bar is in SystemUI demo mode (a clean 9:00 clock, full wifi, full battery) so
no clutter or notification dates date the video. Everything below the status bar is the
shipping app.

---

## 2 · The problem the video has to land

Everything before the first screen has to earn the app. Three beats, in this order:

1. **The goals that matter have no deadline.** Get fit. Learn an instrument. Be a better
   partner. Finish the degree. None of them is due Tuesday, so all of them lose — every
   single day — to whatever *is* due Tuesday.
2. **The tools split the problem into pieces that never meet.** A to-do list holds the
   task but not the reason. A calendar holds the hour but not the goal. A habit tracker
   holds the streak but not the outcome. So the one question you actually want answered —
   *is my life moving in the direction I chose?* — is the one question none of them can
   answer.
3. **What is missing is the connection, not another list.** A goal is only real when the
   task under it, the time it costs, and the progress it produces are the same object.

That is GoalPilot's claim: **your life areas, your goals, your tasks and your hours in one
structure, so progress is something the app can compute rather than something you have to
feel.**

---

## 3 · The narration script

Written to be read at a calm ~145 words per minute. Each block names the on-screen beat
it belongs over; the second-by-second timecodes are in
[`tour-timecodes.md`](tour-timecodes.md), generated from the recording itself.

### Act 0 — the problem *(no screen recording; b-roll or a title card)*

> Everyone has a list of things they mean to do with their life.
> Get fit. Learn the instrument. Finish the degree. Be better to the people you love.
>
> None of them is due on Tuesday.
> So every one of them loses, quietly, to whatever is.
>
> And the apps do not help, because they each hold one piece and never the others.
> Your to-do list has the task but not the reason. Your calendar has the hour but not the
> goal. Your habit tracker has the streak but not the outcome.
>
> So the one question worth asking — *is my life actually moving where I chose?* —
> is the one question none of them can answer.
>
> GoalPilot was built to answer it.

### Act 1 — the home screen

> This is GoalPilot. It opens on the only summary that matters: where you are, and how far
> that is from where you were.
>
> Points and a level, because finishing something should feel like it counted.
> One number for overall progress, averaged across every goal you hold.
> Nine goals. Five tasks done. One this week.

### Act 2 — smart add *(the headline feature)*

> Now watch the part that removes the friction.
>
> You do not file anything. You just say what you want to do, the way you would say it to
> a person.
>
> *"Practice saxophone for twenty minutes on Sunday."*
>
> GoalPilot reads it, works out which of your goals it belongs to, and files it there —
> under *Learn to play the saxophone* — with a point value and an estimate of how long it
> will take. Nothing is saved until you have seen where it went, and one tap undoes it.

### Act 3 — the rest of the home screen

> The tasks you already keep somewhere else come in the same way. Pull your open Google
> Tasks across and GoalPilot sorts each one under the right goal — you review everything
> before a single item is saved.
>
> Connect Health Connect and your steps and your sleep stop being a separate app you have
> to remember to open. They become progress on the goals you already set.
>
> And the coach answers with something you could do today, rather than a motivational
> poster — and if you would rather it answered in your own model, you can hand it your own
> key. Everything works without one.

### Act 4 — goals, grouped by life area

> Your goals are not a flat list, because your life is not a flat list.
>
> They sit under **life areas** — the parts of your life you have decided are worth
> investing in. Health. Studies. Career. Relationships. Every goal belongs to one, and
> that single decision is what makes everything later possible.

### Act 5 — one goal, and how work actually gets scheduled

> Open a goal and you get the whole thing in one screen: the measure, the progress against
> it, the life area it serves, and every task underneath.
>
> Add a task and the AI estimates it for you — how demanding it is, how long it will
> take, and what it is worth. Three hours of your time, sixty points.
>
> Then schedule it. Pick the day, and it is due that day.
> Add a time, and it becomes a real deadline — because GoalPilot knows the difference
> between *the day passed* and *late, and still owed.*
>
> That is the task on your calendar, attached to the goal it serves — which is the join
> no other app makes.

### Act 6 — life areas

> Life areas are yours to define. Name them, order them, or pull them straight from the
> lists you already keep in Google Tasks — because those list names already *are* the
> areas of your life.

### Act 7 — analytics

> And here is what the whole structure was for.
>
> *Where your time goes.* Not how many boxes you ticked — what share of your actual life
> went into each area you said mattered. By day, week, month, quarter or year.
>
> Two thirds of this year went into studies. A fifth into health. And thirteen percent
> went somewhere that was never filed at all.
>
> That is not a judgement. It is just the first honest number you have had.

### Act 8 — social and challenges

> None of this has to be done alone.
>
> A leaderboard with your friends. A feed where a week's progress is something you post
> rather than something you explain — with a photo, if the week deserved one.
>
> And challenges. Pick a measure, invite people, and let the standings say what a promise
> to yourself never could.

### Act 9 — profile

> Your profile holds your level, your points, and a six-character code so a friend can add
> you without either of you typing an email address.

### Act 10 — settings

> The app looks how you want it to look. Four materials — frosted glass, liquid glass, a
> soft raised surface, and a dark one. Colour themes, background treatments, light or dark.
>
> Everything here stays on this phone. Sign out and it is untouched.
>
> Tell it your waking hours and it knows when your day is genuinely full — and when to ask
> you to plan tomorrow.

### Act 11 — the guided tour

> And nobody has to be taught any of it, because the app teaches itself. Seven steps over
> the real screen, on first launch — and replayable, from Settings, forever.

### Close

> GoalPilot.
> Your life areas, your goals, your tasks and your hours — in one place,
> so progress is something you can see instead of something you have to feel.

---

## 4 · OpenArt Director prompt — the primary route

Paste this into Director's chat after uploading `GoalPilot-full-tour.mp4`, and attach
[the narration script](#3--the-narration-script) as the voiceover text.

```text
Make a 3-minute product explainer for an Android app called GoalPilot, using the screen
recording I have uploaded as the on-screen footage for the whole middle section.

STRUCTURE
1. 0:00-0:25 — COLD OPEN, no screen recording. Cinematic b-roll, warm and human, no text
   on screen except the final title. Shots: a person's hand closing a laptop late at
   night; a running shoe by a door, unused; a musical instrument in its case under a bed;
   a calendar page full of meetings and nothing personal. Slow, shallow depth of field,
   natural light, no faces in close-up. End the section on a black card with the word
   GoalPilot and a target-and-arrow logo mark in blue and yellow.
2. 0:25-2:40 — the uploaded screen recording, cut to the narration. Keep the phone
   footage as the picture. Present it as a phone screen: portrait, centred, with a soft
   neutral background and a very slow push-in. Cut between sections rather than playing
   it straight through — the narration below names each section, and each cut should land
   on the sentence that introduces it.
3. 2:40-3:00 — CLOSE. Return to the black card with the logo and the final line.

VOICE
One narrator. Warm, calm, low-key confident — a person explaining something they built,
not an advertisement. Mid-pace, around 145 words per minute. Never excited. Never
salesy. English.

PACE AND STYLE
Unhurried. Let the screen recording breathe — a beat of silence before each new section.
No stock-music swell, no whooshes. A quiet, sparse, warm underscore at low volume, and
drop it out entirely under the analytics section.

CAPTIONS
Burned-in captions for the narration, bottom third, sans-serif, high contrast.

WHAT NOT TO DO
- Do not regenerate, restyle or "improve" the app footage. It is a real product and the
  UI must appear exactly as recorded.
- Do not add a presenter, an avatar, or a talking head.
- Do not add emoji, stickers, arrows, zoom-bursts or kinetic-typography effects.
- Do not invent features or captions that are not in the narration.

NARRATION SCRIPT
[paste §3 of docs/marketing/explainer-video-brief.md here, Act 0 through Close]
```

---

## 5 · Google Flow prompts — the opening b-roll only

Flow will not take the tour. It **will** generate Act 0. Each of these is one Veo clip —
keep every one under eight seconds and stitch them in your editor, or use Flow's Scene
Builder to chain them.

**Clip 1 — the day that ate the evening**
```text
Close-up, shallow depth of field: a pair of hands closing a laptop lid in a dim home
office, late evening. A cold monitor glow fades as the lid shuts. Warm desk lamp is the
only light left. Static camera, slight handheld drift. Cinematic, 35mm, muted colour
grade. No people's faces. No text. No music.
```

**Clip 2 — the goal that never got started**
```text
Static macro shot of a pair of running shoes by a front door, laces still tied, a thin
layer of dust on them. Morning light through a frosted glass door moves slowly across the
floor. Melancholy but not bleak. Cinematic, shallow focus, 50mm. No people. No text.
```

**Clip 3 — the instrument in the case**
```text
Slow push-in on a saxophone case under a bed, half in shadow. Dust motes in a shaft of
afternoon light. The clasps are closed. Warm, nostalgic, quiet. Cinematic, 35mm, shallow
depth of field. No people. No text.
```

**Clip 4 — the calendar that has no room for you**
```text
Overhead shot of a paper wall calendar, every weekday block filled with dense handwriting,
the weekend squares completely empty. A hand enters frame and adds one more entry to an
already-full Tuesday, then leaves. Cool daylight, top-down, static camera. Cinematic. No
faces. No text.
```

**Clip 5 — the turn**
```text
A phone face-down on a wooden table in warm morning light is picked up by a hand and
turned over; the screen is off. Camera holds as the hand lifts it out of frame. Hopeful,
warm, unhurried. Cinematic, shallow depth of field, 50mm. No visible UI on the screen. No
text.
```

Cut clip 5 straight into the first frame of the screen recording and the join reads as
one shot.

---

## 6 · If neither tool will carry your own footage

Then the assembly is manual, and it is genuinely small — the two hard parts (the footage
and the words) are already done.

1. Generate the voiceover from [§3](#3--the-narration-script) in ElevenLabs (or any TTS)
   as one WAV per act.
2. Generate Act 0's five clips in Flow ([§5](#5--google-flow-prompts--the-opening-b-roll-only)).
3. In any editor — CapCut, Clipchamp, DaVinci Resolve, all free — lay the b-roll, then the
   screen recording, and drop each narration WAV on the beat named in
   [`tour-timecodes.md`](tour-timecodes.md).

The timecode file exists precisely so that this route costs an afternoon rather than a
rebuild.

---

## 7 · What is in the app, and what is deliberately not

Written down because a narration that promises a feature the examiner then cannot find is
worse than one that promises less.

**In the recording, and real:** life areas · goals with a measure and progress · tasks
with AI-estimated difficulty, duration and points · plain-language task filing (the AI
reads a sentence and files it under the right goal) · scheduling a task to a **date**, and
to a **date and time** as a deadline · Google Tasks import for tasks *and* for life-area
names · Health Connect · the AI coach · points, levels · friends leaderboard, friends
feed, photo posts, challenges · friend codes · the time-share analytics with day / week /
month / quarter / year · four materials, colour themes, background treatments, light/dark
· waking hours and a planning time · the seven-step guided tour.

**Not in the app, and therefore not in the narration:**

- ❌ **There is no calendar grid, and no Google Calendar sync.** Scheduling exists — a task
  takes a date, and adding a time promotes it to a deadline — but there is no month or
  week view to look at, and nothing is written to Google Calendar. The in-app calendar
  surface is [`#26`](https://github.com/idomarhaim/Android_Final_Project/issues/26) and is
  still a prototype; the only Google integration that ships is **Tasks**, read-only.
  The narration in §3 Act 5 is worded to say exactly what the app does — *"the task on
  your calendar"* means the task carries its day and hour, and nothing stronger.
- ❌ **Hebrew is deferred.** The language picker offers English and System only; `#51` is
  parked open by decision, so the video is English and the UI in it is English.
- ❌ Milestones (`E14`) are modelled in the brief but are not a screen.
