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

---

# Part 4 — the film was shot on Ido's real phone, and the emulator was the problem all along

## The root cause of everything above: the emulator could not reach Firestore

Ido asked why his phone showed *nothing yet* on goals the emulator showed as measured, and why
Save spun forever there. That question found the bug.

`Observed:` the emulator has raw IP connectivity (`ping 8.8.8.8` → 0% loss) but **broken DNS** —
`ping firestore.googleapis.com` → *unknown host*. So every write went into Firestore's **offline
cache** and never reached the server:

- The endless spinner is a save waiting for a server confirmation that can never arrive.
- His phone, reading the real server, showed the true state throughout.
- **Nothing done on the emulator ever touched his account** — confirmed by reading his phone: life
  areas still Hebrew, every goal still *no number yet*. The unintended `Drink 4 Liters` measure
  recorded in Part 3 **never left the emulator**.

⚠️ **And it explains the "un-automatable" chips.** Part 3 concluded Compose chips could not be
driven. On the phone the identical helper drove them first time (`Count took: Target/Unit are
present`). The chips were never the problem; the dead backend was.

**Neutralised:** `pm clear com.idomarhaim.goalpilot.debug` on the emulator discarded the cache and
its **8 pending writes**, so they can never sync if its DNS is ever fixed; then the AVD was shut
down to free RAM. Ido delegated this decision with a hardware-resources hint, and both halves were
taken rather than one.

## The phone as a recording device

USB never worked — Windows enumerated the S25 Ultra as MTP only (`VID_04E8&PID_6860`, `MI_00`),
with **no ADB interface**, despite USB debugging being on. **Wireless debugging** was already
enabled and paired first try. An `adb-phone.sh` wrapper pins `-s` on every call and reports a
single device to `preflight`, which requires exactly one.

## ✅ The take — 68 beats, and the recording actually covers it

`1080×2340`, 30 fps CFR, **11:00 (660.3 s)**, 60 MB, against a last beat at 667.2 s — a **6.9 s
tail**, versus the emulator take's **4 m 43 s hole**.

⚠️ **The emulator take of the same day is unusable and is why this one exists.** `screenrecord`
stopped at **13:05** while the choreography ran to **17:48**, so its last 4 m 43 s had no video and
its beat map addressed frames that did not exist — including a privacy range that named the wrong
seconds. `--time-limit 0` was set and did not hold. The tell is arithmetic and nothing else reports
it: **compare the last beat's timestamp against the encoded duration, every take.**

**Act 5 is whole** — the act that motivated the re-shoot: task field → typed → *the AI estimates
it* → *how demanding, how long, what it is worth* → *the date picker* → *late and still owed* →
*the task, added, with its points*. Five misses, all benign: three root-level `Back`, one optional
`Add a time`, one cosmetic material label.

**Verified by looking**: the English life areas render (`Health · 3 goals`, `Studies · 4 goals`,
`Career · 0 goals`), and the home screen carries real progress.

**Privacy window: `6:54.1` → `7:55.2`** (61 s). Ido chose *course submission only*, so it stays in.

⚠️ **SystemUI demo mode did not apply on the Samsung.** The status bar shows the real clock, the
real battery, and a **screen-recording indicator**, where the emulator take had a fixed 9:00 and a
clean bar. Cosmetic, and not fixable after the fact.

## The phone's real data beat the seeding

Two goals already carried real Health Connect measures the emulator never had —
**`65982/70000 steps` (94 %)** and **`42.5/56 hours` (75 %)**. Seeding stopped there: two measures
were added (`Sleep 7 hours` → 30 nights, which reads **5.5/30, 18 %** from real sleep data, and the
saxophone goal → 50 sessions), and several goals were deliberately left unmeasured so the *No
number* beat and `#65`'s offer card still fire.

## ⚠️ What could NOT be reverted, and why — this is owed to Ido

| item | state | why |
|---|---|---|
| 4 life-area names | **still English** | `adb shell input text` is **ASCII-only and cannot type Hebrew**. The field cleared, nothing typed, and the guard refused to save an empty name — which is the only reason four names were not wiped to blank |
| `Sleep 7 hours` → 30 nights | **still set** | `Nothing yet` selects (the *How do you log it?* section disappears, which is the real signal — checking for `Target` was the wrong test) and *Save changes* closes the form, but the value persists. Cause not established |
| saxophone → 50 sessions | **still set** | same |
| the tour's own writes | **present** | one smart-add task, one task on a goal, one calendar drag — this take reached **Google Calendar**, unlike the emulator's |

`Untested:` whether the app refuses to clear a measure with progress logged. The saxophone goal
reads `0/50` with no progress and failed identically, which weakens that theory; and
`Fitness • no number — 3 entries logged` proves *no number* can coexist with entries. Recorded as
unexplained rather than guessed.

**A second wrong-signal check, worth naming.** The `Nothing yet` guard originally required the
`Target` field to disappear. It does not — Target and Unit stay visible so their values survive
switching back. The section that actually disappears is *How do you log it?*. Reading the wrong
signal produced *"the app is broken"* as a conclusion twice; the diff that settled it was two
dumps, before and after, printed side by side.

## The lesson this session paid for twice

Every silent failure here was caught by **reading the artifact back**, never by the action
reporting success: four renames that "saved" and changed nothing; a take that "succeeded" with
4 m 43 s missing; a revert that closed its form and kept its value; a rename that would have
written an empty name. In each case the tool said `OK`. Only the read-back disagreed.

---

# Part 5 — the marketing film, and the model report

Ido redirected the deliverable: not a voiceover on a product tour, but *"a marketing film with a
story around the need for the product and the problem it answers"*, using the full potential of
OpenArt. That is a different film, and this is it.

## Correction owed: OpenArt HAS audio; the MCP does not expose it

The reply that said there is no TTS *"anywhere"* was too broad. Verified precisely: the MCP has
exactly two generate tools — `openart_generate_image` and `openart_generate_video`. **There is no
`openart_generate_audio`.** Audio appears only as an *input* (`openart_upload_sign` accepts
`mediaType: audio`) and as a list filter (`openart_creation_list`). OpenArt's Audio tab is real and
reachable in the browser; it is simply not wired into the connector.

## The film — `GoalPilot-marketing-film.mp4`, 1080×1920, 2:08

**Structure: every problem shot is answered by the same object, later.**

| # | segment | source | len |
|---|---|---|---|
| 1 | laptop closes, shoes gather dust, sax stays cased | generated | 12 s |
| 2 | the over-full calendar | generated (patch) | 5 s |
| 3 | the phone is picked up | generated | 4 s |
| 4 | **smart add** — a sentence, filed, shown before it saves | real footage | 19 s |
| 5 | the AI estimates the work | real | 10.5 s |
| 6 | goals as time | real | 11.5 s |
| 7 | drag it where it should have been | real | 11 s |
| 8 | the parts of your life | real | 8.7 s |
| 9 | where your time actually went | real | 17 s |
| 10 | **payoff** — the shoes go out the door | generated | 8 s |
| 11 | **payoff** — the saxophone is played | generated | 8 s |
| 12 | **payoff** — the evening comes back | generated | 8 s |
| 13 | closing card | ffmpeg `drawtext` | 5 s |

**The app footage is 78 s of the 11:00 tour, chosen off the 68 measured beats** — which is what the
beat map is for. **Every segment sits outside the privacy window** (`6:54.1`–`7:55.2`), so no
email, friend code or friend's name is in the marketing cut, even though Ido cleared the full tour
for course submission. The marketing film is the one that might travel.

### Two craft decisions, both derived

- **9:16 vertical.** The phone footage is 1080×2340 and the b-roll 1080×1920; anything else
  letterboxes the product. Not asked, recorded as mine.
- **Blur-fill rather than crop.** The tour is taller than 9:16. Cropping to 1920 would cut either
  the status bar or the **tab bar**, and the tab bar is product UI. So the phone footage is scaled
  to height and the sides filled with a blurred, darkened copy of itself.
- **The status bar IS cropped** (`crop=1080:2080:0:140`) — it carried the real clock, the battery,
  and a **red screen-recording indicator**, because SystemUI demo mode does not apply on the
  Samsung. Found by extracting a frame and looking at it, not by reasoning about it.
- **The closing card's text is drawn by ffmpeg, never generated.** Generated typography is exactly
  what produced `TANEES DAHDAY`.

## 📋 The model report — the deliverable `#62` asks for

| component | exact OpenArt model string | why | cost |
|---|---|---|---|
| 🎬 **video** | **`byte-plus-seedance-2-5`** (Seedance 2.5) | newest Seedance; single-shot up to 30 s, 1080p, synchronized audio, largest reference budget. The prior survived the roster call | **14,146** total |
| 🖼️ **image** | **`nano-banana-pro`** — *selected, never called* | the prior (`FLUX.2 Pro`) **does not exist on this roster**; picked by derivation from the brief's own rule (typography is the only thing that would select `gpt-image-2`, and no frame carries any). In the event every shot came from text-to-video, so no still was needed | 0 |
| 🗣️ **voice** | **not reached — the MCP exposes no audio-generation tool** | verified against the tool list, not inferred: only `generate_image` and `generate_video` exist. Ido drives OpenArt's Audio tab in the browser | 0 |

### Generations, in order

| what | model | mode | config | charged |
|---|---|---|---|---|
| opening b-roll | `byte-plus-seedance-2-5` | `text2video` | 1080p · 20 s · 9:16 · audio | 5,774 |
| calendar patch | `byte-plus-seedance-2-5` | `text2video` | 1080p · 5 s · 9:16 · audio | 1,445 |
| payoff ×3 | `byte-plus-seedance-2-5` | `text2video` | 1080p · 8 s · 9:16 · audio | 6,927 |

**Balance 24,000 → 9,854. Total 14,146.** Every charge matched
`round(listCredits × 0.9)` exactly, so these are measured, not estimated.

**Re-rolls: one.** The opening's calendar shot came back covered in garbled pseudo-text
(`TANEES DAHDAY`, `Menday`, `Bommtias`) because the prompt banned text *and* asked for dense
handwriting — a contradiction the model resolves by inventing letterforms. The patch describes the
ink as abstract marks on a defocused plane and cost 1,445 instead of 5,774 to re-roll the whole
20 s. **The three payoff shots needed no re-roll**, because that lesson was applied to the calendar
one up front.

## What is left

The **voiceover**. The narration is written and timed to these exact segments in
[`docs/marketing/marketing-film-cut.md`](../../docs/marketing/marketing-film-cut.md); Ido generates
it in OpenArt's Audio tab and it gets muxed in. The film stands on its own without it — the
generated shots carry their own ambient audio.
