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

Files in `C:\Users\namei\Videos\GoalPilot-Tour\` — outside the repo, because they are binaries.

**Re-shot 2026-08-24 by session `62-tour-video-v2`, against `v0.4.0`.**

| file | what it is |
|---|---|
| **`GoalPilot-full-tour.mp4`** | **the deliverable.** `11:57.4`, **1152 × 2560**, H.264, **constant 30 fps**, 21 522 frames, 44 MB, silent. Use this one. |
| `GoalPilot-full-tour-raw.mp4` | the untouched `screenrecord` output, same picture and length but **variable frame rate** (5 275 frames, because `screenrecord` emits a frame only when the screen changes). Most editors and essentially every AI video tool mishandle VFR, which is why the CFR file exists. |
| `GoalPilot-highlights-60s.mp4` | the **2026-08-22** 60-second cut, kept. It is of `v0.3.2` and therefore predates the calendar, the Google Calendar sync, `#64`'s run and the `No number` chips — but it carries **no email, no friend code and nobody else's name**, which the full tour does. |

| | |
|---|---|
| **Recorded** | 2026-08-24, `emulator-5554` (`sdk_gphone64_x86_64`, Android 15), app **`v0.4.0`** (versionCode 9, debug variant) |
| **Account** | Ido's real account — 9 goals, 4 life areas, a friend, a live leaderboard |
| **Beat map** | [`tour-timecodes.md`](tour-timecodes.md) — **70 beats, every one measured** rather than reconstructed |
| **Produced by** | `scripts/record-tour.sh`, committed. The next take is one command. |

⚠️ **The 2026-08-22 cut was overwritten and is gone.** Its filename was this file's own Exit
criterion, so a new take wrote over it; nothing outside this folder held a copy, and a video is not
in git. `archive_previous` in the script now stamps and keeps whatever is already there, so this
costs one cut and not two. The 60-second highlights file survived only because its name differs.

⚠️ **Before this goes anywhere public**, read the last section of `tour-timecodes.md`: the full tour
shows Ido's email address, his friend code and a friend's real name, and it names the timecodes
where. The 60-second cut does not.

⚠️ **The UI is English; the data on screen is not.** The four life areas are named in Hebrew
(בריאות · לימודים · קריירה · זוגיות, synced from Ido's Google Tasks lists), the home screen greets
him in Hebrew, and the friend in the leaderboard has a Hebrew name. See §7 — this is a fact about
the footage, not about the app.

**Known gap in this take.** Act 5's add-task sub-flow did not fire: the AI estimate, the date picker
and the submit were all missed, because the measure-proposal hunt earlier in that act scrolls to the
bottom of the goal screen and the add-task row is at the **top** of it. Fixed in the script
(`scroll_to_top` before the hunt) but **not re-shot** — so the scheduling beat in the current file is
the goal screen and a date picker rather than a task being estimated and scheduled end to end.
Act 2's smart-add is unaffected and worked: the AI filed *"Practice saxophone for 20 minutes on
Sunday"* under *Learn to play the saxophone* at `20m · +7`, and it is visible on the goal screen
later in the same recording.

The status bar is in SystemUI demo mode (a clean 9:00 clock, full wifi, full battery) so no clutter
or notification dates the video. Everything below the status bar is the shipping app.

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

**Rewritten 2026-08-24 by session `62-tour-video-v2`, against `v0.4.0`.** The version this
replaced was written on 2026-08-22 against `v0.3.2`, and by the time it was read again seven
tickets had shipped. Two of its paragraphs were not merely dated, they were **false**: Act 3
narrated the Google Tasks and Health Connect cards as living on the home screen, and Act 5 was
worded around the app having no calendar grid and no Google Calendar sync. It has both.

Written to be read at a calm ~145 words per minute. Each block names the on-screen beat it belongs
over; the second-by-second timecodes are in [`tour-timecodes.md`](tour-timecodes.md), generated by
`scripts/record-tour.sh` from the recording itself.

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

> Sometimes it cannot tell. A task that belongs to no goal and has no date used to vanish —
> filed nowhere, listed on no screen, and impossible to delete because there was nowhere to
> delete it from. Now the app says so, in those words, and hands you the two questions
> separately: what goes, and what stays.
>
> That is a small thing to build and a large thing to promise. Nothing you put into this app
> can end up somewhere you cannot reach.
>
> The coach answers with something you could do today, rather than a motivational poster —
> and if you would rather it answered in your own model, you can hand it your own key.
> Everything works without one.
>
> And a week's progress is something you post, rather than something you have to explain.

⚠️ **What changed here, and why the old paragraph could not stay.** Until 2026-08-24 the Google
Tasks import and the Health Connect card were on this screen, and this act narrated them. Ido moved
both into Settings under *Connected apps* (`s25-layout-and-tour`, his own placement call), so they
are narrated in **Act 12** now. A narration that promises them here sends the viewer to a screen
that does not have them.

### Act 4 — goals, grouped by life area

> Your goals are not a flat list, because your life is not a flat list.
>
> They sit under **life areas** — the parts of your life you have decided are worth
> investing in. Health. Studies. Career. Relationships. Every goal belongs to one, and
> that single decision is what makes everything later possible.
>
> And where a goal has no number yet, it says *no number* — not nought per cent. An app that
> prints a zero for something you have simply not measured is lying to you in a way you will
> not notice, and this one refuses to.

### Act 5 — one goal, and how work actually gets scheduled

> Open a goal and you get the whole thing in one screen: the measure, the progress against
> it, the life area it serves, and every task underneath.
>
> If it has no measure, the app offers one — measure it in sessions, in kilometres, in pages
> — drawn from what you have actually been logging. You are not asked to invent a metric
> before you are allowed to start.
>
> Add a task and the AI estimates it for you — how demanding it is, how long it will
> take, and what it is worth. Three hours of your time, sixty points.
>
> Then schedule it. Pick the day, and it is due that day.
> Add a time, and it becomes a real deadline — because GoalPilot knows the difference
> between *the day passed* and *late, and still owed.*

### Act 6 — the calendar *(new; this is the join)*

> And here is where that hour goes.
>
> Everything with a time on it lands on the calendar. Not a separate calendar app holding a
> separate list — the same tasks, under the same goals, on the hours you gave them.
>
> The bar across the top of each day is how full that day already is, before you add to it.
> When it turns, that is the app telling you that you have promised away more hours than the
> day has in it.
>
> Press and hold a block and you can pick it up and move it. And when the thing you moved
> repeats, it asks the only question that matters: did you mean this one, or this one and
> every one after it?
>
> That is the join no other app makes. The task, the goal it serves and the hour it costs
> are the same object — so moving one of them moves all three.

### Act 7 — and it is your real calendar

> It is also *your* calendar. GoalPilot writes to Google Calendar — onto a calendar of its
> own, so it never touches anything you already keep there — and the hours turn up beside
> your lectures and your meetings, which is where they have to be if they are ever going to
> compete with them.
>
> And when something disappears from that calendar, the app notices, and asks you whether
> you meant it.

*Shot note:* this act's proof shot is **Google Calendar's own UI** with the GoalPilot entries in
it, not GoalPilot's. Stills already exist at
`docs/render-passes/2026-08-23-61-google-calendar/`. Showing this from inside GoalPilot *asserts*
the sync; showing it from inside Google Calendar *demonstrates* it, and that difference is the
whole value of the beat.

### Act 8 — life areas, and the record of what you kept

> Life areas are yours to define. Name them, order them, or pull them straight from the
> lists you already keep in Google Tasks — because those list names already *are* the
> areas of your life.
>
> And under each one is the honest record: window by window, what you kept, what you missed,
> and what is **still owed**.
>
> Not a score. Not a streak you can break once and never repair. Three different things,
> drawn as three different shapes — because a single percentage would have averaged away the
> only distinction worth keeping: that a window you have not reached yet is not a window you
> failed.

⚠️ **Three things the voiceover on this beat must not do**, because `#64` was built specifically to
avoid them: do not call it a **score**, do not call it a **rate**, and do not describe any part of
it as **red**. The run is drawn by *form* — filled, hollow, dashed with a pip, dotted, a dashed ring
with a plus — and carries no hue coding and no ratio anywhere on screen. A voiceover saying *"your
success rate"* over it contradicts the picture the viewer is looking at, which is worse than saying
nothing.

### Act 9 — analytics

> And here is what the whole structure was for.
>
> *Where your time goes.* Not how many boxes you ticked — what share of your actual life
> went into each area you said mattered. By day, week, month, quarter or year.
>
> Two thirds of this year went into studies. A fifth into health. And thirteen percent
> went somewhere that was never filed at all.
>
> Goals with no number are named here rather than charted as a zero — the same refusal as
> everywhere else. The app would rather show you a gap than a wrong number.
>
> That is not a judgement. It is just the first honest number you have had.

### Act 10 — social and challenges

> None of this has to be done alone.
>
> A leaderboard with your friends. A feed where a week's progress is something you post
> rather than something you explain — with a photo, if the week deserved one.
>
> And challenges. Pick a measure, invite people, and let the standings say what a promise
> to yourself never could.

### Act 11 — profile

> Your profile holds your level, your points, and a six-character code so a friend can add
> you without either of you typing an email address.

### Act 12 — settings

> Everything the app connects to lives in one place. Your Google Tasks, brought across and
> filed under the right goals — and you review every one of them before a single item is
> saved. Health Connect, so your steps and your sleep stop being a separate app you have to
> remember to open; they become progress on the goals you already set.
>
> The app looks how you want it to look. Four materials — frosted glass, liquid glass, a
> soft raised surface, and a dark one. Colour themes, background treatments, light or dark.
>
> Everything here stays on this phone. Sign out and it is untouched.
>
> Tell it your waking hours and it knows when your day is genuinely full — and when to ask
> you to plan tomorrow.

### Act 13 — the guided tour

> And nobody has to be taught any of it, because the app teaches itself. Seven steps over
> the real screen, on first launch — and replayable, from Settings, forever.
>
> It does not only point at things, either. When it rings a control and asks you to press
> it, pressing it opens what it opened — and the tour waits there while you look, instead of
> pulling you straight back.

⚠️ **That last sentence is only true from `v0.4.0`.** Before 2026-08-24 the spotlight ringed the
Calendar tab and then swallowed the press it had invited; the first fix opened the Calendar for
about one frame before steering back to Home. If this film is ever re-cut from older footage, that
line comes out with it.

### Close

> GoalPilot.
> Your life areas, your goals, your tasks and your hours — in one place,
> so progress is something you can see instead of something you have to feel.

---

## 4 · OpenArt Director prompt — the primary route

**Pin the models before you generate anything.** Ido has an OpenArt **PRO** subscription and
asked for the best model in each class, one each:

| | model | why |
|---|---|---|
| 🎬 **video** | **Seedance 2.0** | **#1 on the Artificial Analysis leaderboards** for both text-to-video and image-to-video as of June 2026, and the strongest at multi-shot continuity — which is what makes five separate b-roll clips read as one film. Runner-up **Kling 3 Omni** wins on native dialogue and a shared audio timeline; neither is needed, because **the narration comes from ElevenLabs on Director's timeline, not from the video model.** |
| 🖼️ **image** | **Nano Banana Pro** | strongest on quality *and* text, with 4–6 second turnaround, which matters when the opening gets iterated. Runner-up **GPT Image 2** measured **98.5 %** text accuracy and wins only if a frame must render exact typography — **it should not**: the title card belongs in the editor, where it is sharp and editable without a re-roll. |

Images are for **reference frames feeding Seedance** and at most one logo card — **never for the
app**. The UI on screen is the recorded UI, always.

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

**Re-checked 2026-08-24 against `v0.4.0` by session `62-tour-video-v2`.** Two entries in the
"not in the app" list below were **false** by then and have been moved up: the calendar surface
(`#60`) and Google Calendar sync (`#61`) both shipped on 2026-08-23. That is the failure mode this
section exists to prevent, arriving from the other direction — a narration that promises *less*
than the app does is a narration that throws away its two strongest beats.

**In the recording, and real:** life areas · goals with a measure and progress · tasks
with AI-estimated difficulty, duration and points · plain-language task filing (the AI
reads a sentence and files it under the right goal) · scheduling a task to a **date**, and
to a **date and time** as a deadline · **a three-day and week calendar grid, with an all-day strip
and a per-day load bar** · **press-and-hold drag to move a block, and a scope sheet where the thing
repeats** · **two-way Google Calendar sync onto GoalPilot's own calendar, and a card when an entry
disappears from it** · Google Tasks import for tasks *and* for life-area names · Health Connect ·
the AI coach · points, levels · friends leaderboard, friends feed, photo posts, challenges · friend
codes · the time-share analytics with day / week / month / quarter / year · **a per-life-area record
of kept, missed and still-owed windows** · **a measure the app proposes for a goal that has none** ·
**an explicit `No number` wherever a goal has no measure, instead of a fabricated nought** ·
**deleting anything, including a task filed under no goal and no date** · four materials, colour
themes, background treatments, light/dark · waking hours and a planning time · the seven-step
guided tour.

**Not in the app, and therefore not in the narration:**

- ❌ **Hebrew is deferred.** The language picker offers English and System only; `#51` is
  parked open by Ido's own decision, so the video is English and the UI in it is English.
  ⚠️ **The UI is English; the *data* on the account being filmed is not.** Ido's four life areas
  are named in Hebrew (בריאות · לימודים · קריירה · זוגיות, synced from his Google Tasks lists),
  the home screen greets him in Hebrew, and the friend in the leaderboard has a Hebrew name. That
  is a fact about the footage, not about the app, and it is settled before a take rather than
  after.
- ❌ Milestones (`E14`) are modelled in the brief but are not a screen.

**In the app, but with nothing on this account to show it with.** A third category, added
2026-08-24, because it is the one that actually bit: the feature ships, the narration is true, and
the footage shows an empty state. Measured on `emulator-5554` against Ido's live account:

| feature | what the screen actually says today |
|---|---|
| Overall progress (home) | *"No goal has a number yet"* — **no goal on the account carries a measure**, so there is no ring and no percentage |
| `#64`'s kept / missed / still-owed run | *"Nothing has been due here yet"* on **every** life area — no occurrence has a due window, so the run renders its empty state |
| The calendar (`#60`) | **one** entry, and the day reads *free*. Enough to show the grid and the drag; not enough to show a load bar filling |
| The scope sheet (`#68`) | needs a **recurring** block to appear at all, and there is not one |
| Analytics by **week** or **month** | *"Nothing completed in this week yet"* — **only the Year view has data** (67 % / 20 % / 13 % over 3h 45m), which is what the narration in §3 Act 9 describes and what the recording therefore shoots |

None of that is a bug and none of it is a doc error: it is an account that has not been used for
two weeks. It matters here only because a **marketing film** of an app is a film of its *data*, and
these five beats are the ones where this account has none.

