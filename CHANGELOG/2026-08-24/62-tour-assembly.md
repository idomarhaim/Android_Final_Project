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
