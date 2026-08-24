# `62-tour-assembly` — 2026-08-24

Assemble the explainer film for [`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62)
and report the three models. Inherited from `62-tour-video-v2`, whose brief closed with the
assembly undone for one reason: the OpenArt MCP was connected mid-session and MCP connectors
load at session **start**, so that session never saw an `openart` tool.

## ✅ The connector is live in this session, and the roster tool exists

`62-tour-assembly` started **after** the connector was added, and picked it up. Sixteen
`openart` tools are reachable. The brief warned that a roster call was *not confirmed to
exist* and told me to enumerate before assuming — it does exist, and there are three of them:

| tool | what it answers |
|---|---|
| `openart_model_list` | the roster — 17 models, their modes, and what each is for |
| `openart_model_cost` | credit price of any (model, mode, config) — no arguments prices everything |
| `openart_model_form_get` | the exact input schema for one (model, mode) |

**Account:** `name.iddo@gmail.com`, plan **Pro**, **24,000 credits**. MCP-originated
generations on Plus-and-above get **10 % off**, applied as `round(unitCredits × 0.9) × quantity`
— so the actual charge is quoted below by that formula, not as a mental "10 % off".

## 🎬 Video — the prior SURVIVES the roster call, and the price does not

`Seedance 2.5` exists and its exact OpenArt id is **`byte-plus-seedance-2-5`**. The ticket's
two disagreeing sources (OpenArt's MCP page listing 2.0, a 2026 round-up listing 2.5) are
settled: **2.5 is on the roster**, alongside 2.0, 2.0 Fast and 2.0 Mini.

**But the roster also prices it, and that is the finding that matters.** The opening b-roll
the brief specifies is 15–20 s. One 20 s take:

| config | list | actual charge |
|---|---|---|
| `byte-plus-seedance-2-5`, 1080p, **20 s**, 9:16, audio | 6,415 | **5,774** |

That is **24 % of the whole credit balance for a single take**, before a single re-roll.
The same shot from the alternatives, at 10 s / 1080p / 9:16 (two of which cut together give
the full 20 s opening):

| model | id | list | actual | 2 × 10 s opening |
|---|---|---|---|---|
| **Wan 2.7** | `wan2-7` | 350 | **315** | **630** |
| Gemini Omni Flash | `gemini-omni-flash` | 500 | 450 | 900 |
| Seedance 2.5 @ 720p | `byte-plus-seedance-2-5` | 1,300 | 1,170 | 2,340 |
| Grok Imagine 1.5 | `grok-imagine-1-5` | 1,800 | 1,620 | 3,240 |

**A 9× spread on the same shot**, and the prior sits at the expensive end.

### Why the prior was chosen for a capability this shot does not use

`Untested:` — this is an argument from the roster's own descriptions plus the brief, not from
a generation anyone has run. Seedance 2.5's stated differentiators are **single-shot clips up
to 30 s**, a **30-image / 10-video / 10-audio reference budget**, and **dialogue with
lip-sync**, all aimed at "multi-shot storytelling and AI short dramas". The opening b-roll
is, per [`explainer-video-brief.md`](../../docs/marketing/explainer-video-brief.md) §5,
**15–20 s of abstract footage that dramatises the problem before the phone appears, under a
separate voiceover track**. It has no dialogue, no recurring character to keep consistent
across shots, and its audio is replaced in the edit. So the three things being paid for are
the three things the shot does not use — and Seedance 2.5 is additionally capped at **1080p**,
where Seedance 2.0 and Kling 3 Omni reach 4K.

**Recommendation: `wan2-7`** — "cinematic video with high image-to-video stability;
references and start/end-frame control; solid mid-tier choice for controlled camera and
framing", which is exactly what a b-roll opening needs. Two 10 s shots, **630 credits**
against 5,774. Held for Ido's word because it spends his credits — see the reply.

## 🖼️ Image — the prior is NOT on the roster at all

**`FLUX.2 Pro` does not exist in this MCP.** There is no FLUX model of any version. That is a
clean negative from the roster call, and it kills the brief's first pick outright.

Both of the brief's named fallbacks are present, at the same price:

| model | id | 1K, 1 image | actual |
|---|---|---|---|
| **Nano Banana Pro** | `nano-banana-pro` | 40 | **36** |
| GPT Image 2 | `gpt-image-2` | 40 | 36 |

**Decision — `nano-banana-pro`, and it is derived rather than asked.** The brief already
committed the reasoning: *"Nano Banana 2 Pro wins on instruction-following; GPT Image 2 only
where exact typography must render — and it never should, because on-screen words belong in
the editor."* Reference and start frames carry no typography, so the condition that would
select GPT Image 2 is never met. Decision recorded as mine, Ido's to overturn.

## 🗣️ Voice — not reachable, now OBSERVED rather than predicted

**There is no text-to-speech, narration or audio-generation model anywhere on the roster**,
and no Director or timeline tool either. The brief predicted this from Ido's connector
settings screenshot; the full roster confirms it against all 17 models rather than the six
that were above the fold.

⚠️ **One near-miss worth naming, because it looks like a yes and is not.** Seedance 2.x
`element2video` accepts an **audio element** (wav/mp3, 2–30 s) to give a subject a voice —
*"the woman in image 1 speaks with this voice"*. That is **voice transfer into generated
video**, which needs a voice clip you already have and produces a talking subject inside new
footage. It is not narration TTS, and it cannot lay a voiceover over recorded screen capture.
The row stands: **`not reached — the MCP exposes no TTS surface`**, and per the brief that is
a complete answer.

## Consequence for Step 3 — the middle row applies

No Director tool on the roster ⇒ **assembly is not MCP-reachable**. The route is the one the
brief called the likely case: generate the opening b-roll and its reference frames here, and
**Ido drives Director in the browser** for the timeline and the voiceover.

## 🔀 Board

Claimed `62-tour-assembly`, and **released `62-tour-video-v2`'s stale row** — its brief is
`status: done` in `sessions/done/`, a positive self-report; its tree is clean and fully
pushed; its last transcript turn (11:44:45) is a handoff reply; and its own `owns:` list in
my brief hands me `docs/marketing/**` and `scripts/record-tour.sh`. Nothing of its content
was touched. Full reasoning is in the note under its row on `SESSIONS.md`.

## 🧪 Tests

No test layer applies to this unit — it is a roster query, a board claim, and a changelog.
The project's test layers (server unit, endpoints, database, client component, UI E2E) are
untouched; nothing in `app/` or `functions/` changed.

---

# Part 2 — Ido's four answers, and three findings that changed the plan

| question | answer |
|---|---|
| b-roll spend | **Seedance 2.5, 1×20s, ~5,774** — my `wan2-7`/630 recommendation was **overruled**. Recorded as his call; he optimises for quality over credits, evidence I use again below |
| privacy | **Course submission only** — the 62 s window (`7:46.5`–`8:48.1`) stays in, no redaction pass |
| Hebrew data | **Rename the life areas to English first** |
| `#64` data | **Delegated** — *choose the best solution; filling in data for the demo is no problem, just don't change the software itself only for the video.* Data yes, app code no |

## Finding 1 — the `#64` seeding needs NO Firestore surgery, and the brief's claim is FALSE

The brief states the app's UI *cannot* create a missed past window, and concludes the only route is
writing occurrence documents directly into live Firestore. **That is wrong**, and it is what makes
the delegated answer cheap and safe.

`Observed:` there is **no `selectableDates` constraint anywhere in `app/src/main/`** — the grep
returns nothing. `WhenPicker.kt:124` calls `rememberDatePickerState(initialSelectedDateMillis = …)`
with **no** `selectableDates` argument, and Compose's default selects **every** date. A task can
therefore be given a past due date through the app's own form.

**Consequence:** `#64` is seedable exactly as Step 0's measures were — through the UI, creating only
documents the app itself creates, revertible through `#67`'s delete. Strictly the best of the three
routes offered, and precisely the boundary Ido drew: **data, not software.**

## Finding 2 — the life-area rename is safe, and does NOT need Ido to touch Google Tasks

The picker option said *I would need you to rename the four lists in Google Tasks*. The app can do
it instead, and the rename **sticks**:

- `LifeAreaRepository.linkGoogleList` exists to point an area at a list *without touching the name,
  colour or icon the user chose* — the sync is designed not to overwrite a name.
- `BuildLifeAreaProposalsUseCase` builds `linkedListIds` from existing areas' `googleListId` and
  filters those lists out **before** any name matching. An already-linked list produces **no
  proposal at all**, so a renamed area cannot generate a duplicate.
- Nothing is written without confirmation.

**So the rename is app-side only — Ido's real Google Tasks lists stay Hebrew and nothing on his
Google account changes.** A strictly smaller blast radius than the option he picked described, so I
am taking it: decision recorded as mine, his to overturn.

## Finding 3 — the Hebrew greeting is NOT app data and is not mine to change

`DashboardViewModel.kt:219` reads the **Google account profile name**, not a GoalPilot document.
Editing it changes Ido's real account identity, so it is his alone. **The home greeting stays
Hebrew** unless he changes it. The English-UI answer is therefore partly satisfiable and partly not,
and saying so is better than quietly delivering three quarters of it. Same for the friend's Hebrew
name in the leaderboard — another person's name, and the *course submission only* answer means it
was staying anyway.

## The rehearsal — 62 beats, zero selector failures

`--dry-run --no-writes` walked all thirteen acts: 62 beats against the take's 70, the difference
being the three write beats it skips. **Three `MISS desc=Back`, all benign** — the script taps Back
twice from two-deep screens and the second is a no-op at root.

⚠️ **The seeding cost a beat, measured rather than predicted.** `GOAL_TO_OPEN=saxophone` is one of
the three goals given a measure, so `#65`'s measure-offer card no longer fires there — the rehearsal
shows no such beat between 17 and 18. Net trade is clearly positive (six Act 5 beats plus progress
rings, for one beat). I am **not** repointing the script at an unmeasured goal: that would write a
real task to a different real goal and make the saxophone-themed narration incoherent.

## ⚠️ `--dry-run --no-writes` is NOT inert, and its own header says it is

The header promises it *leaves the account exactly as it found it* — true of the **account**, false
of the **output directory**. It ran `archive_previous` (renaming `GoalPilot-full-tour.mp4` to
`GoalPilot-full-tour-20260824-0421.mp4`) and **overwrote `beats.tsv`**, so the take's original
70-beat `beats.tsv` is gone. Nothing that matters was lost: the cut is renamed rather than deleted,
and `docs/marketing/tour-timecodes.md` still holds all 70 measured beats, because the timecodes doc
is only regenerated at the post-encode step that `--dry-run` skips.

## The b-roll — generated, verified by LOOKING at it, and four fifths good

`byte-plus-seedance-2-5`, `text2video`, 1080p / 20 s / 9:16 / audio, seed `-1`, `historyId`
`IofDeywRTvGxBino279M`. **Charged 5,774 credits** — balance 24,000 → 18,226, matching
`round(6415 × 0.9)` exactly and validating the pricing formula behind every quote above. Saved to
`GoalPilot-Tour\opening-broll-seedance25.mp4`, 1080×1920, 20.064 s, 24 fps, AAC stereo.

**Checked by extracting a frame per shot and looking at them, not by reading metadata.**

| shot | t | verdict |
|---|---|---|
| 1 laptop | 2 s | good mood; **a visible Apple logo**, and the laptop is being *used* rather than closed |
| 2 shoes | 6 s | excellent, matches Clip 2; **visible ASICS-style branding** |
| 3 sax case | 10 s | excellent, matches Clip 3 exactly |
| 4 calendar | 14 s | **BROKEN** — garbled pseudo-text: `TANEES DAHDAY`, `Menday`, `Bommtias`, `Tuesdar` |
| 5 phone | 18 s | good; **the screen renders dark, no fake app UI** — the brief's biggest stated risk |

**The failure this ticket was most likely to walk into did not happen** — nothing generative
re-rendered GoalPilot.

**Shot 4 is my authoring error, not the model's.** The prompt said *no text anywhere* **and** *every
weekday block dense with handwriting*. Those contradict: the second forces lettering, so the model
invented some. A prompt that bans text must not also describe writing.

**Patch rather than re-roll:** one 5 s replacement at 1,605 list → **1,445 charged**, against 5,774
to regenerate the whole 20 s. `historyId` `XbEZmvLiNJwYy0vROOFm`, prompted so no lettering is
representable (heavy defocus, ink as abstract strokes, explicit ban on letters/numbers/day names).
Spending it is derived from his own b-roll answer: he took 5,774 over my 630, which is evidence he
buys quality over credits.

## 🧪 Tests

No test layer applies — no file under `app/` or `functions/` was modified. The verification that did
run is the one that fits the artifact: the choreography re-run against the live app (62 beats, 0
selector failures), and the generated video checked by extracting and viewing frames rather than by
trusting its metadata.

## The patch — generated, looked at, and it worked

`historyId` `XbEZmvLiNJwYy0vROOFm`, 1080×1920, 5.056 s, 24 fps, saved to
`GoalPilot-Tour\opening-broll-calendar-patch.mp4`. **Charged 1,445** — balance 18,226 → 16,781,
matching `round(1605 × 0.9)` exactly. That is the **second** exact hit for the discount formula, so
the credit numbers quoted in this changelog are measured, not estimated.

**Verified by extracting frames and looking, the same way the defect was found.** At `t=1 s` and
`t=3 s` the ink is abstract grey scribble with **no legible words anywhere** — the `TANEES DAHDAY` /
`Menday` / `Bommtias` failure is gone. The day *numbers* (19, 25, 26) render as clean real numerals
rather than gibberish, which is better than the ban asked for: it reads as a calendar without
inventing vocabulary. The crowded-left / empty-right contrast that carries the meaning survived, and
the pen lands a stroke in a square on camera.

**The lesson, which is the reusable part:** a generative prompt that forbids text must not also
describe writing. *No text anywhere* + *dense with handwriting* is a contradiction, and the model
resolves it by inventing letterforms. Describe the ink as **abstract marks** and defocus the plane it
sits on, and the same shot comes back clean for a quarter of the price of a full re-roll.

### Running total

| item | model | charged |
|---|---|---|
| opening b-roll, 20 s | `byte-plus-seedance-2-5` | 5,774 |
| calendar patch, 5 s | `byte-plus-seedance-2-5` | 1,445 |
| **total** | | **7,219** |

Balance 24,000 → **16,781**. Re-rolls remain affordable.

## What is left, and why it stopped here

The two generated assets are done and verified. The remaining work is **one continuous device
session**, and it must run in this order because each step depends on the last:

1. **Rename the four life areas to English** through the app UI, recording the Hebrew originals
   (בריאות · לימודים · קריירה · זוגיות) for the revert. Safe per Finding 2.
2. **Seed `#64`** through the app UI — tasks with past due dates across several life areas, some
   completed and some not, so the run card shows kept / missed / still-owed instead of an empty
   state. Possible per Finding 1; every created task recorded for the revert.
3. **Re-shoot** — `bash scripts/record-tour.sh`, about twelve minutes. One take covering both the
   rename and the `#64` data, plus Act 5 whole and the progress rings.
4. **Revert everything** — the `#64` tasks, the life-area names, and Step 0's three goal measures.
   Then confirm the home screen reads *No goal has a number yet* again.

`scripts/record-tour.sh` is **sourceable** (its `main` is guarded at line 1170), so steps 1 and 2
should reuse its helpers — `tap_text`, `tap_desc`, `scroll_to_text`, `scroll_to_top`, `type_text`,
`commit_text` — rather than re-deriving the traps it already encodes (the Back key exiting the app,
Git Bash rewriting device paths, fixed swipe counts).

⚠️ **Step 4 is owed whatever else happens.** Ido's live account currently carries three seeded
measures from `62-tour-video-v2` and will carry renamed life areas and seeded tasks after steps 1–2.
Nothing has reached Google Calendar.
