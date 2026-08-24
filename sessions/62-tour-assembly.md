---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 62
blocked_on_human: "Ido must connect the OpenArt MCP connector in his claude.ai connector settings. Nothing in this brief can start until `openart` tools are visible to the session -- check FIRST and stop if they are not."
owns:
  - docs/marketing/**
  - scripts/record-tour.sh
  - CHANGELOG/<the day you run>/62-tour-assembly.md
  - kb-candidates/<the day you run>-62-tour-assembly.md
  - sessions/62-tour-assembly.md
  # Video artifacts land OUTSIDE the repo, in C:\Users\namei\Videos\GoalPilot-Tour\
singletons:
  - emulator-5554 and adb, ONLY if you choose to re-shoot (see step 1)
created: 2026-08-24 by 62-tour-video-v2
---

# `#62` — assemble the film, and report the three models

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

The footage exists. This brief is the half `62-tour-video-v2` could not do, and it could not do it
for exactly one reason: **the OpenArt MCP is not connected**.

## ⛔ Check this before anything else

Enumerate the session's tools for `openart`. On 2026-08-24 there was **no `openart` tool of any
kind** — not connected, not even listed as needing authorisation. If that is still true, **stop and
say so**: every model call below is unreachable and there is nothing here that can be worked around.
OAuth needs a browser and cannot be driven from a tool shell, so this is Ido's move, in his claude.ai
connector settings.

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`docs/marketing/explainer-video-brief.md`](../docs/marketing/explainer-video-brief.md) —
§1 (the footage as it now stands), §3 (the thirteen-act narration, rewritten for `v0.4.0`), §4 (the
Director prompt), §7 (what is and is not in the app, and what the account has no data for) ·
[`docs/marketing/tour-timecodes.md`](../docs/marketing/tour-timecodes.md) — 70 measured beats ·
[`CHANGELOG/2026-08-24/62-tour-video-v2.md`](../CHANGELOG/2026-08-24/62-tour-video-v2.md)

## What you inherit — do not rebuild any of it

- **`GoalPilot-full-tour.mp4`** in `C:\Users\namei\Videos\GoalPilot-Tour\` — `11:57.4`,
  1152 × 2560, CFR 30 fps, silent, shot against **`v0.4.0`** on 2026-08-24.
- **A measured beat map** — `docs/marketing/tour-timecodes.md`, 70 beats, each one an *observed*
  elapsed second rather than a reconstruction. It also names the two privacy ranges.
- **The narration** — thirteen acts, rewritten against `v0.4.0`. Three warnings are embedded in it
  and they are not stylistic: `#64`'s run must not be called a **score**, a **rate**, or described
  as **red**; and Act 13's *"the tour waits where you pressed"* is only true from `v0.4.0`.
- **`scripts/record-tour.sh`** — the choreography. A re-shoot is one command.

## Step 1 — decide whether to re-shoot, and it is cheap either way

**One act of the current take is degraded.** Act 5's add-task sub-flow did not fire — no AI
estimate, no date picker, no submit — because the measure-proposal hunt scrolls to the bottom of the
goal screen and the add-task row is at the top. **The script is already fixed** (`scroll_to_top`
before the hunt); it was simply not re-shot.

So: `bash scripts/record-tour.sh` costs about twelve minutes and gets that act back. It also
**archives** the current cut rather than overwriting it (`archive_previous`), so re-shooting risks
nothing. Do it if Ido wants Act 5 whole; skip it if he does not, and say which.

⚠️ **If you re-shoot, read the device-state section at the bottom first.**

## Step 2 — the roster call, and it is the first model call

The MCP exposes a **discover available models** tool, and OpenArt states new models appear in it
automatically. The picks in `sessions/done/62-tour-video-v2.md` are a **prior, not an answer**:

| component | prior | the honest limit on it |
|---|---|---|
| 🎬 video | **Seedance 2.5** | `Untested:` whether the roster carries it. OpenArt's own MCP page still lists **2.0**; a 2026 round-up lists 2.5. Those sources disagree and **only the roster call settles it** |
| 🗣️ voice | **ElevenLabs v3**, Multilingual v2 per-act if delivery drifts over a long take | OpenArt's published MCP surface lists **no TTS, audio or timeline control at all**. Expect this to be Ido's to drive in Director's browser UI |
| 🖼️ image | **FLUX.2 Pro** for reference and start frames | Nano Banana 2 Pro wins on instruction-following; GPT Image 2 only where exact typography must render — and it never should, because on-screen words belong in the editor |

If the roster carries something better, **take it and say what the roster showed**.

## Step 3 — assemble, by whichever route is actually open

| if | then |
|---|---|
| Director is MCP-reachable | drive the whole assembly from the session |
| only model calls are reachable — **the likely case** | generate the opening b-roll and its reference frames through MCP; **Ido drives Director in the browser** for the timeline and the voiceover |
| neither carries the real footage into the cut | the manual fallback — §6 of the brief: TTS per act, dropped on the measured timecodes, in any free editor |

⚠️ **Do not let a generative model re-render the app.** The UI on screen must be the recorded UI.
A five-minute generation that *looks like* GoalPilot is worse than useless as a product demo, and it
is the easiest thing for the tool to do — which is why it is the failure this ticket is most likely
to walk into.

## Step 4 — the model report is a deliverable, not a courtesy

For **each** of the three: the **exact model string as OpenArt names it** in the roster (not the
marketing name) · **why that one**, and if it differs from the prior, what the roster showed ·
**what it cost** in credits and how many re-rolls · for voice, the **preset or clone**, and which
acts got which if more than one was used.

Three places: the reply to Ido, the changelog, and a closing comment on `#62`. **A component you did
not reach still owes its row.**

## Carries over

- `docs/marketing/explainer-video-brief.md` — narration, Director prompt, fallback, §7
- `docs/marketing/tour-timecodes.md` — 70 measured beats, and the privacy ranges
- `scripts/record-tour.sh` — the choreography, fixed
- `CHANGELOG/2026-08-24/62-tour-video-v2.md` — the five takes and what each one taught
- `C:\Users\namei\Videos\GoalPilot-Tour\GoalPilot-full-tour.mp4` — the footage itself

## Two answers from Ido that shape the film, and were open when this was written

Neither blocks the assembly; both change what the film should be. **Ask before cutting, not after.**

1. **The account is empty of exactly the new features.** No goal carries a measure, so there is no
   progress ring anywhere; `#64`'s run says *"Nothing has been due here yet"* on **every** life area;
   the calendar holds one entry and reads *free*; only the **Year** analytics view has data. `#64`
   is one of the two beats the original brief called *"the ones that say something about the product
   rather than about a feature"*, and it renders an empty state.
2. **The UI is English and the data is not.** The four life areas are Hebrew (בריאות · לימודים ·
   קריירה · זוגיות, synced from his Google Tasks lists), the home screen greets him in Hebrew, and
   the friend in the leaderboard has a Hebrew name.

Both are in `docs/marketing/explainer-video-brief.md` §7 with the measurements behind them.

## Out of scope

- **The KB ingest** — `kb-candidates/2026-08-24-62-tour-video-v2.md` is owed a drain and has its
  own brief, [`sessions/kb-drain-62.md`](kb-drain-62.md). Cross-repo into `C:\Dev\JARVIS\kb\`.
- **Deleting the rehearsal tasks.** The recording created *"Practice saxophone for 20 minutes on
  Sunday"* on Ido's real account. Deleting it is a deletion and therefore his call; `#67` makes it
  reachable from the UI.
- **Google Flow.** Its one job was the cinematic opening b-roll. Seedance 2.5 does 30 s in a single
  take with native audio, which is the whole of that opening — so decide after the roster call
  whether Flow has anything left to do here.

## Exit

- The finished film, **or** an honest statement of which of the three routes applied and what is
  left for Ido.
- **The model report** — three rows, each with the exact OpenArt model string, why it, what it cost,
  and for voice the preset and which acts got it. A component not reached still owes its row.
- `CHANGELOG/<the day you run>/62-tour-assembly.md`.
- **`#62` closed**, or a comment saying what still holds it open.

## Device state

📱 **Only if you re-shoot (step 1).** `emulator-5554` holds a live sign-in and **`v0.4.0-debug`**
(versionCode 9). Update it with `adb install -r` and **never `connectedDebugAndroidTest`**, which
uninstalls the app and takes the Firebase auth store with it.

⚠️ **`uiautomator` wedges, and the recovery is to WAIT, not to kill.** Killing an instance is
precisely what leaves its accessibility service registered and guarantees the next wedge. The script
handles this; if you drive the device by hand, do not reach for `kill`. A reboot clears it and app
data survives one.

⚠️ **The recording shows real data** — `name.iddo@gmail.com`, the friend code `NDXVJC`, and a
friend's real name. `tour-timecodes.md` names the exact ranges. Settle with Ido **before** the cut
if this is going anywhere public.
