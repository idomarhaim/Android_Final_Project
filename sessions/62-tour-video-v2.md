---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
# Was `blocked_on: [60]`, and before that `[59, 60, 61]`. ALL THREE ARE CLOSED as of
# 2026-08-23 (#60 closed with the calendar surface shipped), so the declared blocker is
# stale and cleared -- same repair as c14435b made for #61. Verify rather than trust this.
#
# NOT blocked on #67, and read the note below before deciding the order anyway: nothing
# stops this brief running today, but running it BEFORE #67 buys a third recording.
issue: 62
owns:
  - docs/marketing/**
  - scripts/record-tour.sh
  - kb-candidates/<the day you run>-62-tour-video-v2.md
  - CHANGELOG/<the day you run>/62-tour-video-v2.md
  # Dated paths, deliberately not pinned to 2026-08-23. Write them from the day you
  # ACTUALLY work: a row carrying someone else's date sends the next session to a
  # path that does not exist, and it reads the row as abandoned.
  - sessions/62-tour-video-v2.md
  # Video artifacts land OUTSIDE the repo, in C:\Users\namei\Videos\GoalPilot-Tour\
singletons:
  - the AVD and adb, for the whole recording pass
created: 2026-08-23
---

# `#62` — record the tour again, then make the film

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

## 📍 State as of 2026-08-24 — this block supersedes the three stacked notices kept below

**Amended by session `62-kickoff-refresh` at Ido's request, before he opens the #62 session.**
[The ticket](https://github.com/idomarhaim/Android_Final_Project/issues/62) was rewritten in the
same pass and now says the same things; where the two disagree, the ticket is the later copy.

1. ✅ **All three declared blockers are CLOSED** — `#59` (`70bf805`), `#61` (`1ccdb3e`), `#60`.
   The front matter is `ready`. **Verify them yourself rather than trusting this line.**
2. ⚠️ **Only `#67` is still ahead of this one, and it is an ordering call, not a blocker.**
   `#68` closed on 2026-08-23, so the note below reading *"`#67` and `#68` in either order, then
   this"* is down to one ticket. The repo's whole open list is now `#51` (Hebrew, parked by Ido),
   `#62` (this) and `#67`. `#67` touches the dashboard, the calendar, the goals list, the
   life-area screen and `C19`'s run card — five surfaces this tour walks through — so recording
   before it lands buys a `v3`.
3. ⚠️ **The app moved a long way, and so did the tour itself.** Seven tickets landed after this
   brief was written. The shot list in *The recording* below has been rewritten for them; read it
   rather than the ticket's original three bullets.
4. 🎬🗣️🖼️ **The model section is now three components, not two.** Ido: *"use the best model found
   for each component — video, voice, image — and write which model you used."* **Voice was
   missing entirely**, the video pick has been superseded (Seedance 2.0 → **2.5**, released
   2026-07-31), the image pick has changed to **FLUX.2 Pro**, and **reporting the model used is
   now a deliverable** rather than a courtesy. See *Which OpenArt model to use* below.

⚠️ **THE TOUR ITSELF CHANGED ON 2026-08-24 — the script this brief assumes is stale.**
`tour-refresh` reworked `ui/tutorial/`: step 6 is no longer *"Social … Profile …"* over the whole
nav bar (that step named a tab `#60` had removed) but the **Calendar** tab, and step 7 now carries
Profile as well. `TUTORIAL_VERSION` went `1` → `2`, so **the tour re-runs by itself on any device
that has already seen it** — which is convenient for recording and surprising if you are not
expecting it. Read `CHANGELOG/2026-08-24/tour-refresh.md` and walk the tour once before setting up
a shot list; `docs/marketing/tour-timecodes.md` describes the **old** seven steps. **v0.3.3** is
the build with all of it in.

### Historical — superseded by the block above, kept because the instruction in them is still right

> ✅ **UNBLOCKED 2026-08-23 — all three named blockers are closed.** `#59` (`70bf805`), `#61`
> (`1ccdb3e`) and now **`#60`**, whose calendar surface shipped. The front matter is `ready`.
> **Verify all three yourself rather than trusting this line**, which is the instruction the
> paragraph below gives and it is still the right habit.
>
> ⚠️ **BUT DO `#67` FIRST — this is an ordering call, not a blocker.** `#67` (*delete anything*) is
> **the last functional change on the board**: at the time of writing the repo has exactly three
> open issues — `#51` (Hebrew, **parked by Ido's own decision** until functionality works), `#62`
> (this one) and `#67`. So `#67` is the only thing left that will change what the app **looks
> like**, and this brief exists precisely because features landed after the first tour was shot —
> that is what the `v2` in its slug means.
>
> **Amended the same day: `#68` (drag to move) joins `#67` ahead of this one**, and it was filed
> after this note was first written — so the count above is *"the last functional change"* as it
> stood at that moment, not a standing claim. Order: **`#67` and `#68` in either order, then
> this.** Neither blocks the other. *(`#68` closed 2026-08-23 — see item 2 above.)*
>
> *Decision taken by session `64-area-success-failure`, 2026-08-23, per `rules/derivable-decision.md`
> — it follows from this brief's own purpose rather than from anything Ido said. One message
> reverses it, and nothing technical stops you starting today.*
>
> 📌 **Historical, now fully satisfied — kept because its instruction is still right:** `#59`
> has shipped and closed (`70bf805`, the data repair ran). **`#61` has shipped and closed too**
> (`1ccdb3e`, 2026-08-23 — the GoalPilot calendar exists in Ido's account and its banners are
> photographed in Google Calendar's own UI at `docs/render-passes/2026-08-23-61-google-calendar/`,
> which is a shot **this brief wants**). **Still blocked on #60 alone**, whose surface shipped and
> whose ticket stays open for drag-to-move.
>
> ⚠️ **`status: blocked`. Do not start this until `#59`, `#60` and `#61` have shipped.** Check
> the three before claiming; if any is open, this brief is not ready and saying so is the
> right outcome of opening it.


**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`docs/marketing/explainer-video-brief.md`](../docs/marketing/explainer-video-brief.md) ·
[`docs/marketing/tour-timecodes.md`](../docs/marketing/tour-timecodes.md) ·
[`CHANGELOG/2026-08-22/tour-video.md`](../CHANGELOG/2026-08-22/tour-video.md) — the last two
sections of that changelog are the traps, and reading them is cheaper than rediscovering them.

**Task:** re-record the full-app tour on the AVD with the new features in it, update the
narration, and assemble the narrated explainer in OpenArt Director.

## Why one session and not two

Ido asked. The answer is that **the assembly is driven by the beat timecodes, and the
timecodes are a by-product of the recording.** Split them and the second session either
re-derives the beat map by scrubbing a video or trusts a map it did not make. The session
that shot the footage already knows where everything is.

## What you inherit — do not rebuild it

- **The narration script, the problem framing, both tool prompts and a manual fallback** —
  `docs/marketing/explainer-video-brief.md`.
- **The beat map format and how it is produced** — `docs/marketing/tour-timecodes.md`. It was
  reconstructed from the choreography and scaled to the measured duration, because the take
  was shot blind. ⚠️ **Do not repeat that: log `date +%s.%N` inside the `beat` function and
  the whole reconstruction disappears.** That is the one change worth making to the method.
- **The previous cut** — `C:\Users\namei\Videos\GoalPilot-Tour\`.

## The two traps that cost the last session a dry run each

1. **This AVD reports a hardware keyboard.** Gboard shows a small floating toolbar instead of
   an on-screen keyboard, so **there is no IME window for Back to dismiss** and
   `input keyevent 4` after `input text` is consumed as navigation — from a root screen it
   **exits the app**. `Observed:` the next tap landed on the launcher and opened **YouTube**,
   and nine screenshots later the run still reported success. Commit typed text with the
   toolbar's own tick, at `(107, 1436)` on a 1344 × 2992 screen.
2. **Never locate a control by a fixed swipe count.** A Compose scroll flings, so the same
   swipe lands on a different offset run to run. Bottom the list out with several short
   swipes and use the stable anchor — *Replay tutorial* is at `(346, 1144)` bottomed out, and
   was missed entirely by a count-based tap.

**Commit the choreography this time**, as `scripts/record-tour.sh`. Last time it lived in a
session scratchpad and died with the session, which is why re-recording is a whole ticket
rather than one command.

## The recording

Everything the last take covered, **plus everything in the table below** — all of which landed
*after* the first tour was shot, which is what the `v2` in this slug means.

| Shipped since | What is now on screen, and what to shoot |
|---|---|
| [`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63) | the occurrences collection and recurrence — no screen of its own, but it is the machinery under every row below |
| [`#60`](https://github.com/idomarhaim/Android_Final_Project/issues/60) | **the calendar surface** — the 3-day view, creating by tapping a slot, the all-day strip, the load bar going red |
| [`#68`](https://github.com/idomarhaim/Android_Final_Project/issues/68) | **drag to move** — the gesture itself, the **scope sheet** that appears only where a rule exists, *skip* as the second entry point, and the **`TooLarge` snackbar naming both numbers**. §4.3's third verb, and a *showable* gesture, which is rare in this app |
| [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61) | **the Google calendar** — created, written to, and then **shown in Google Calendar's own UI**, which is the shot that proves it rather than asserting it. Already photographed at `docs/render-passes/2026-08-23-61-google-calendar/` |
| [`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64) | **`C19`'s success/failure run** — `kept · missed · still-owed`, on the life-area screen *and* under analytics' donut. Drawn by **form** (filled · hollow · dashed-with-a-pip · dotted · dashed ring with a `+`), never hue. **There is no red in it and no ratio anywhere** — if the narration calls it a score or a rate, the narration is wrong |
| [`#65`](https://github.com/idomarhaim/Android_Final_Project/issues/65) | **`C22`'s measure proposal** — the dashed `UnmeasuredMarker` in the list, and the offer card on the goal |
| [`#66`](https://github.com/idomarhaim/Android_Final_Project/issues/66) | **an unmeasured goal states no number** — *no number — 11 entries logged* where a percentage used to be, across eight surfaces including the text of a shared post. Shoot this **together with `#65`**: they are one story on screen |
| [`#59`](https://github.com/idomarhaim/Android_Final_Project/issues/59) | `Strength Training` reading a **sane percentage**. The current recording has `245613%` on screen at `1:23`, which alone makes it unusable as marketing |
| [`#69`](https://github.com/idomarhaim/Android_Final_Project/issues/69) | editing a single occurrence of a one-off no longer fails silently — worth a beat if the film shows editing at all |
| [`#70`](https://github.com/idomarhaim/Android_Final_Project/issues/70) | the dashboard's overall-progress card, verified in its three states (and a fourth nobody had listed) |
| `tour-refresh` | **the in-app tutorial itself** — the v2 seven steps. Step 6 is the **Calendar** tab (*"Your goals, as time"*) and carries `#68`'s drag-to-move as its **second sentence**; step 7 absorbed Profile. If the film shows onboarding, this is what it must show |

⚠️ **`#67` is still open**, and it touches five of these surfaces. See item 2 of the state block at
the top of this file before deciding when to shoot.

**Update the narration.** It currently states outright that there is no calendar grid and no
Google Calendar sync, and Act 5 is worded around their absence. That paragraph becomes the
strongest thirty seconds in the film instead — write it that way. `#64`'s run and `#66`'s honest
*no number* are the two beats the old script could not have had, and they are the ones that say
something about the product rather than about a feature.

**Keep the production details that worked:** SystemUI demo mode for a clean status bar,
`screenrecord --time-limit 0` for one continuous take, and a CFR re-encode afterwards
(`-fps_mode cfr -r 30`) because `screenrecord` output is variable-frame-rate and most tools
mishandle it. All three are written up in the changelog and in that session's KB candidates.

⚠️ **`ffmpeg` and `ffprobe` are installed now** — `%LOCALAPPDATA%\Programs\ffmpeg\bin`, on the user
`PATH` but **not** in a tool shell opened before 2026-08-22, so use the absolute path. And
**`-vsync 0` has been removed from current ffmpeg and is a hard error** — it is `-fps_mode
passthrough` now, whatever the recipes say. Both traps are in `CLAUDE.md`.

## The assembly, and the honest unknown

Ido has an OpenArt **PRO** subscription and asked for the MCP route.

⚠️ **Find out what the MCP can actually reach before planning around it — and plan for the
middle row.** `Observed:` 2026-08-23, re-checked 2026-08-24 — OpenArt runs an MCP server at
**`https://mcp.openart.ai/mcp`**, OAuth, no API key, supporting Claude / ChatGPT / Cursor. Its
published surface is **text-to-image, text-to-video, image-to-video, reference uploads, model
discovery, library / project / workspace access and credit checks** — a **model-calling** surface.

⚠️ **Its own page makes no mention of voice, text-to-speech, audio, or Director timeline
control**, and lists *Character* and *Smart Shot* as roadmap. That is weaker than *"unconfirmed"*,
which is what this brief said before: it is an absence in the vendor's own tool list. Treat the
voice component as **probably Ido's to drive in Director's browser UI**, and be pleasantly
surprised if the roster call says otherwise. **Enumerate the tools first**, either way.

| if | then |
|---|---|
| Director *is* MCP-reachable | drive the whole assembly from the session |
| only model calls are reachable — **the likely case** | generate the opening b-roll and its reference frames through MCP; **Ido drives Director in the browser** for the timeline and the voiceover — and the model report still owes its 🗣️ voice row |
| neither carries the real footage into the cut | the manual fallback — §6 of the brief: TTS per act, dropped on the timecodes, in any free editor |

📱 **The MCP is Ido's to connect** — OAuth needs a browser and cannot be driven from a tool
shell. Ask for it in a heading at the top of the reply, before you need it, not after.

⚠️ **Do not let a generative model re-render the app.** Whatever route is taken, the UI on
screen must be the recorded UI. A five-minute Kling generation that *looks like* GoalPilot is
worse than useless as a product demo, and it is the failure this ticket is most likely to
walk into, because it is the easiest thing for the tool to do.

## Which OpenArt model to use — one per component: 🎬 video · 🗣️ voice · 🖼️ image

Ido, 2026-08-24: *"use the best model found for each component — video, voice, image — and write
which model you used."*

**This section was rewritten on 2026-08-24 and the three picks are not the ones that stood before.**
What changed: **voice was missing entirely** (the brief had two components, not three); the video
pick is **superseded** — Seedance **2.5** shipped 2026-07-31, two months after the ranking the old
text cited; the image pick moved to **FLUX.2 Pro**; and **reporting the model is now a deliverable**.

### Discover before deciding — the roster wins, this table is only the prior

The MCP exposes a **discover available models** tool, and OpenArt states that **new models appear in
it automatically as they ship**. So the first model call of the session is the roster call, on Ido's
PRO account, and the names below are a **prior**, not an answer. If the roster carries something
newer or the named model is absent, take the better one and **say what the roster showed** in the
report.

| Component | Use | Why | Runner-up, and when it would win instead |
|---|---|---|---|
| 🎬 **Video** | **Seedance 2.5** | Released **2026-07-31**, superseding the Seedance 2.0 this brief used to name. **30 s in a single take** — the old cut's opening was five clips stitched, and stitching is where a b-roll montage falls apart; **native audio**; up to **4K**; **region-level editing**, which changes part of a frame without re-rolling the clip; and up to **50 multimodal references** (30 image / 10 video / 10 audio), which is what makes a start-frame workflow work. An **Ultra-Long beta** reaches 180 s. Reference handling and multi-scene continuity are exactly this job. | **Kling 3 Omni** — a unified video/audio/voice system, stronger on multi-shot sequences with a shared audio timeline and native dialogue in several languages. **Neither buys anything here**: nobody speaks on camera, and the narration comes from the TTS on Director's timeline, **not** from the video model. **Veo 3.1** if prompt adherence on a single cinematic shot ever matters more than continuity across five. |
| 🗣️ **Voice** | **ElevenLabs v3** — OpenArt's text-to-speech **is** ElevenLabs | The expressive ceiling: emotionally rich, context-aware delivery with real control over tone, pacing and intent, across 29 languages. An act-by-act narration is exactly the long-form dramatic case it is built for. | **ElevenLabs Multilingual v2** is the **most stable long-form** model. This narration runs minutes, so if v3's delivery drifts across a long take, render **per act** and drop the acts that need steadiness to Multilingual v2. **Mixing per act is legitimate** — just say which act got which. **Flash v2.5** only if latency or credit cost ever becomes the constraint, which for a pre-rendered film it is not. |
| 🖼️ **Image** | **FLUX.2 Pro** | The default for **reference and start frames** feeding the video model: photoreal texture with a tactile quality that still edges the field at the Pro/Max tiers, strong stylization, and the cheapest of the three to iterate on — and the opening **will** be iterated. | **Nano Banana 2 Pro** wins on instruction-following, character consistency across references, and anatomy/faces/lighting — take it for a frame that has to **obey a precise instruction** rather than merely look good. **GPT Image 2** wins only where **exact typography** must be rendered (98.5 % against Seedream 5 Pro's 89.5 % in Atlas Cloud's 2026 benchmark) — and it should never have to, because **on-screen words belong in the editor**, where they are sharp, correct and editable without a re-roll. |

⚠️ **Provenance, because none of this was measured here.** `Inferred:` every ranking in the
table above is **reported by trade press and vendor pages read on 2026-08-24**, not observed on
Ido's account. `Untested:` **whether OpenArt's roster actually carries Seedance 2.5** — its own MCP
page still lists **Seedance 2.0** among its video models, while a 2026 round-up of the platform
lists 2.5. Those two sources disagree, and 2.5 shipped on 2026-07-31, which is recent enough for a
roster to lag. **The roster call settles it and nothing else does**, which is why it is the first
model call of the session and why the picks above are labelled a prior.

⚠️ **There is no single best model in 2026 — there is a best model per task.** Every row above names
the job before it names the model, and that ordering is the point. If a later ticket needs someone
speaking on camera, or a frame that must render a logotype, re-open the relevant row.

### 📣 Report which model you used — this is a deliverable, not a courtesy

Ido asked for it in the same sentence that asked for the models. For **each** of the three
components:

1. the **exact model string as OpenArt names it** in the roster call — not the marketing name. If
   the roster says `seedance-2.5-pro`, that is what goes in the report;
2. **why that one**, in one line — and if it is not the model named above, **what the roster showed
   that changed the choice**;
3. **what it cost** in credits, and how many re-rolls it took;
4. for voice, the **voice preset or clone** as well as the model, and **which acts got which** if
   more than one was used.

Three places: the reply to Ido, `CHANGELOG/<the day you run>/62-tour-video-v2.md`, and a closing
comment on the ticket. **A component you did not reach still owes its row** — *"voice: not reached,
the MCP has no TTS surface and Director was driven by Ido"* is a complete answer; silence is not.

### What images are even for here

Not the app — **never** the app. The UI on screen is the recorded UI, always. Images are for
**reference frames** feeding the video model (a start frame for a clip, a style reference), and for
the one **logo/title card** if it is not simply drawn in the editor.

⚠️ **Do not let a generative model re-render GoalPilot.** A five-minute generation that *looks like*
the app is worse than useless as a product demo, and it is the easiest thing for the tool to do —
which is why it is the failure this ticket is most likely to walk into.

### Sources

[ByteDance launches Seedance 2.5 — TechNode, 2026-07-31](https://technode.com/2026/07/31/bytedance-launches-seedance-2-5-video-generation-model/) ·
[Seedance 2.5's 30-second clips with built-in audio — The Decoder](https://the-decoder.com/bytedances-seedance-2-5-generates-30-second-video-clips-with-built-in-audio/) ·
[Seedream 5.0 Pro vs Nano Banana Pro vs Flux.2 Pro — PixMind](https://www.pixmind.io/posts/seedream-5-0-pro-vs-nano-banana-pro-vs-flux-2-pro-2026-image-generation-tripartite-showdown) ·
[Eleven v3](https://elevenlabs.io/v3) ·
[Best AI text-to-speech 2026 — Storytool](https://storytool.io/blogs/best-ai-voice-generators-2026) ·
[OpenArt's model roster](https://openart.ai/blog/best-ai-generators/) ·
[OpenArt MCP](https://openart.ai/mcp)

## Exit

- A new `GoalPilot-full-tour.mp4` in `C:\Users\namei\Videos\GoalPilot-Tour\`, CFR, with a
  regenerated `tour-timecodes.md` — this time **measured, not reconstructed**.
- `scripts/record-tour.sh` committed, so the take after this one is one command.
- `docs/marketing/explainer-video-brief.md` updated: the narration, and §7's *what is and is
  not in the app* table, which currently lists the calendar as absent.
- The finished film, or — if the tooling will not carry the footage — an honest statement of
  which of the three routes above applies and what is left for Ido.
- **The model report** — 🎬 video, 🗣️ voice and 🖼️ image, each with the exact OpenArt
  model string, why it, what it cost, and (for voice) the preset and which acts got it. In the
  reply, in the changelog, and as a closing comment on the ticket. A component you did not
  reach still owes its row.
- `CHANGELOG/<the day you run>/62-tour-video-v2.md`.

## Device state

📱 **Needs Ido signed in.** No install/uninstall is required — `adb install -r` if the app
must be updated, and **never `connectedDebugAndroidTest`**, which uninstalls the app and takes
the sign-in with it.

✅ **`emulator-5554` still holds a live sign-in.** `tour-refresh` used it 00:18–00:24 on
2026-08-24 and released it, via `adb install -r` on both APKs — **never**
`connectedDebugAndroidTest` — so nothing was uninstalled and the Firebase auth store survived.
Check before assuming you need Ido; ask in the heading if you do.

⚠️ **The recording shows real data.** The last one put `name.iddo@gmail.com`, the friend code
`NDXVJC` and a friend's real name on screen. If this cut is going anywhere public, settle
that with Ido **before** shooting — trimming after the fact costs another pass.

## Out of scope

**Google Flow.** Its ceiling on an uploaded video is 60 s, forcibly trimmed to 30 s, of which
a ≤10 s segment is usable, and `Extend` works on Veo-generated video only. It keeps exactly
one job, already prompted in §5 of the brief: the cinematic opening b-roll, which has no
screen recording behind it.

⚠️ **And check whether it still keeps even that job.** Seedance 2.5 produces **30 s in a
single take** with native audio, which is the whole of the opening b-roll — so Flow may have
nothing left to do here. Decide it after the roster call, not before.
