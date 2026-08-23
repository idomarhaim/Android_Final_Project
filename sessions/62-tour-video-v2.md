---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
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
  - kb-candidates/2026-08-23-62-tour-video-v2.md
  - CHANGELOG/2026-08-23/62-tour-video-v2.md
  - sessions/62-tour-video-v2.md
  # Video artifacts land OUTSIDE the repo, in C:\Users\namei\Videos\GoalPilot-Tour\
singletons:
  - the AVD and adb, for the whole recording pass
created: 2026-08-23
---

# `#62` — record the tour again, then make the film

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

✅ **UNBLOCKED 2026-08-23 — all three named blockers are closed.** `#59` (`70bf805`), `#61`
(`1ccdb3e`) and now **`#60`**, whose calendar surface shipped. The front matter is `ready`.
**Verify all three yourself rather than trusting this line**, which is the instruction the
paragraph below gives and it is still the right habit.

⚠️ **THE TOUR ITSELF CHANGED ON 2026-08-24 — the script this brief assumes is stale.**
`tour-refresh` reworked `ui/tutorial/`: step 6 is no longer *"Social … Profile …"* over the whole
nav bar (that step named a tab `#60` had removed) but the **Calendar** tab, and step 7 now carries
Profile as well. `TUTORIAL_VERSION` went `1` → `2`, so **the tour re-runs by itself on any device
that has already seen it** — which is convenient for recording and surprising if you are not
expecting it. Read `CHANGELOG/2026-08-24/tour-refresh.md` and walk the tour once before setting up
a shot list; `docs/marketing/tour-timecodes.md` describes the **old** seven steps.

⚠️ **BUT DO `#67` FIRST — this is an ordering call, not a blocker.** `#67` (*delete anything*) is
**the last functional change on the board**: at the time of writing the repo has exactly three
open issues — `#51` (Hebrew, **parked by Ido's own decision** until functionality works), `#62`
(this one) and `#67`. So `#67` is the only thing left that will change what the app **looks
like**, and this brief exists precisely because features landed after the first tour was shot —
that is what the `v2` in its slug means. Recording before `#67` lands buys a `v3`, and `#67`
touches the dashboard, the calendar, the goals list, the life-area screen and `C19`'s run card:
**five surfaces this tour walks through.**

**Amended the same day: `#68` (drag to move) joins `#67` ahead of this one**, and it was filed
after this note was first written — so the count above is *"the last functional change"* as it
stood at that moment, not a standing claim. `#68` builds §4.3's third verb on the **calendar**,
which is one of this tour's headline surfaces. Order: **`#67` and `#68` in either order, then
this.** Neither blocks the other.

*Decision taken by session `64-area-success-failure`, 2026-08-23, per `rules/derivable-decision.md`
— it follows from this brief's own purpose rather than from anything Ido said. One message
reverses it, and nothing technical stops you starting today.*

📌 **Historical, now fully satisfied — kept because its instruction is still right:** [#59](https://github.com/idomarhaim/Android_Final_Project/issues/59)
has shipped and closed** (`70bf805`, the data repair ran). **`#61` has shipped and closed too**
(`1ccdb3e`, 2026-08-23 — the GoalPilot calendar exists in Ido's account and its banners are
photographed in Google Calendar's own UI at `docs/render-passes/2026-08-23-61-google-calendar/`,
which is a shot **this brief wants**). **Still blocked on #60 alone**, whose surface shipped and
whose ticket stays open for drag-to-move. The paragraph below is kept as written and still lists all three,
because its instruction is *check them before claiming* — do that; do not take either this note or
that list as the current state.

⚠️ **`status: blocked`. Do not start this until
[#59](https://github.com/idomarhaim/Android_Final_Project/issues/59),
[#60](https://github.com/idomarhaim/Android_Final_Project/issues/60) and
[#61](https://github.com/idomarhaim/Android_Final_Project/issues/61) have shipped.** Check
the three before claiming; if any is open, this brief is not ready and saying so is the
right outcome of opening it.

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

Everything the last take covered, plus:

- **the calendar surface** (#60) — the 3-day view, creating by tapping a slot, dragging to
  move, the all-day strip, the load bar going red;
- **the Google calendar** (#61) — created, written to, and then **shown in Google Calendar's
  own UI**, which is the shot that proves it rather than asserting it;
- `Strength Training` reading a **sane percentage** (#59). The current recording has
  `245613%` on screen at `1:23`, which alone makes it unusable as marketing.

**Update the narration.** It currently states outright that there is no calendar grid and no
Google Calendar sync, and Act 5 is worded around their absence. That paragraph becomes the
strongest thirty seconds in the film instead — write it that way.

**Keep the production details that worked:** SystemUI demo mode for a clean status bar,
`screenrecord --time-limit 0` for one continuous take, and a CFR re-encode afterwards
(`-fps_mode cfr -r 30`) because `screenrecord` output is variable-frame-rate and most tools
mishandle it. All three are written up in the changelog and in this session's KB candidates.

## The assembly, and the honest unknown

Ido has an OpenArt **PRO** subscription and asked for the MCP route.

⚠️ **Find out what the MCP can actually reach before planning around it.** `Observed:`
2026-08-23 — OpenArt runs an MCP server at **`https://mcp.openart.ai/mcp`**, OAuth, no API
key, supporting Claude / ChatGPT / Cursor. Its published tool list is *generate images and
video, discover available models, track results, **upload reference files**, check account
and credits, switch workspaces, organize projects* — a **model-calling** surface.
**Whether Director's timeline, voiceover and captions are drivable through it is stated
nowhere and is unconfirmed.** So the first unit of work is: connect it, **enumerate the
tools**, and then choose.

| if | then |
|---|---|
| Director is MCP-reachable | drive the whole assembly from the session |
| only model calls are reachable | generate the opening b-roll through MCP; Ido drives Director in the browser for timeline and voiceover |
| neither carries the real footage into the cut | the manual fallback — §6 of the brief: TTS per act, dropped on the timecodes, in any free editor |

📱 **The MCP is Ido's to connect** — OAuth needs a browser and cannot be driven from a tool
shell. Ask for it in a heading at the top of the reply, before you need it, not after.

⚠️ **Do not let a generative model re-render the app.** Whatever route is taken, the UI on
screen must be the recorded UI. A five-minute Kling generation that *looks like* GoalPilot is
worse than useless as a product demo, and it is the failure this ticket is most likely to
walk into, because it is the easiest thing for the tool to do.

## Which OpenArt models to use — one for video, one for images

Ido: *"use the best models there — only the best video model and the best image model."*

### 🎬 Video — **Seedance 2.0**

`Observed:` as of **June 2026** Seedance 2.0 ranks **#1 on the Artificial Analysis leaderboards
for both text-to-video and image-to-video** (the default, audio-included view), and it is
reported as **the best model available for strict character continuity and multi-shot director
control**. That second property is the one that matters here even though there are no
characters: the opening is **five separate clips that have to read as one film**, and continuity
across shots is exactly what a b-roll montage lives or dies on.

**The runner-up, and when it would win instead.** **Kling 3 Omni** is *"arguably the most capable
general-purpose video model available right now"* and is stronger specifically at **multi-shot
sequences with a shared audio timeline and native dialogue in five languages**. Neither of those
is needed here: **the narration comes from ElevenLabs through Director's timeline, not from the
video model**, and nobody speaks on camera. So the dialogue advantage buys nothing and the
leaderboard position decides it.

⚠️ **There is no single best video model in 2026 — there is a best model per task.** That is why
this section names the task before it names the model. If a later ticket needs someone speaking
on camera, re-open this choice; for silent atmospheric b-roll, Seedance 2.0.

### 🖼️ Images — **Nano Banana Pro**

`Observed:` reported as **the strongest on both quality and text**, returning Pro-level results
in **4–6 seconds**, which matters because the opening will be iterated on several times.

**The runner-up, and when it would win instead.** **GPT Image 2** measured **98.5 % text
accuracy** against Seedream 5 Pro's 89.5 % in Atlas Cloud's 2026 API benchmark — so if a frame
has to render **exact typography**, it wins. It should not have to: **the title card and any
on-screen words belong in the editor, not in a generated image**, where they are sharp, correct,
and editable without a re-roll.

### What images are even for here

Not the app — **never** the app. The UI on screen is the recorded UI, always. Images are for
**reference frames** feeding Seedance (a start frame for a clip, a style reference), and for the
one **logo/title card** if it is not simply drawn in the editor.

⚠️ **Do not let a generative model re-render GoalPilot.** A five-minute generation that *looks
like* the app is worse than useless as a product demo, and it is the easiest thing for the tool
to do — which is why it is the failure this ticket is most likely to walk into.

### Sources

[Artificial Analysis rankings via Hedra's 2026 model round-up](https://www.hedra.com/blog/best-ai-video-models) ·
[Seedance vs Veo vs Kling](https://pixo.video/blog/seedance-vs-veo-vs-kling) ·
[Atlas Cloud's 2026 image API benchmark](https://www.atlascloud.ai/blog/tips/2026-ai-image-api-benchmark-gpt-image-2-vs-nano-banana-2-pro-vs-seedream-5-0) ·
[OpenArt MCP](https://openart.ai/mcp/)

## Exit

- A new `GoalPilot-full-tour.mp4` in `C:\Users\namei\Videos\GoalPilot-Tour\`, CFR, with a
  regenerated `tour-timecodes.md` — this time **measured, not reconstructed**.
- `scripts/record-tour.sh` committed, so the take after this one is one command.
- `docs/marketing/explainer-video-brief.md` updated: the narration, and §7's *what is and is
  not in the app* table, which currently lists the calendar as absent.
- The finished film, or — if the tooling will not carry the footage — an honest statement of
  which of the three routes above applies and what is left for Ido.
- `CHANGELOG/2026-08-23/62-tour-video-v2.md`.

## Device state

📱 **Needs Ido signed in.** No install/uninstall is required — `adb install -r` if the app
must be updated, and **never `connectedDebugAndroidTest`**, which uninstalls the app and takes
the sign-in with it.

⚠️ **The recording shows real data.** The last one put `name.iddo@gmail.com`, the friend code
`NDXVJC` and a friend's real name on screen. If this cut is going anywhere public, settle
that with Ido **before** shooting — trimming after the fact costs another pass.

## Out of scope

**Google Flow.** Its ceiling on an uploaded video is 60 s, forcibly trimmed to 30 s, of which
a ≤10 s segment is usable, and `Extend` works on Veo-generated video only. It keeps exactly
one job, already prompted in §5 of the brief: the cinematic opening b-roll, which has no
screen recording behind it.
