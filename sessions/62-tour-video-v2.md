---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: blocked
blocked_on: [59, 60, 61]
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
