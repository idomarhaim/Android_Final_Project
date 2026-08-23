# `62-kickoff-refresh` — the brief and the ticket, brought up to the app they describe

> **Summary:** Ido asked, before opening the `#62` session, that
> [`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62) and its kickoff brief carry
> the updates the newly-closed tickets made — and that the OpenArt instruction name **the best model
> per component (video, voice, image)** and **report which model was used**. Both artifacts were
> rewritten. The ticket body had not been touched since **2026-08-22** and was two days and **seven
> tickets** behind; the brief was one day behind and had **no voice component at all**.
>
> **Date:** 2026-08-24 · **Session:** `62-kickoff-refresh` · **Mode:** AUTO · **Issue:**
> [#62](https://github.com/idomarhaim/Android_Final_Project/issues/62) (stays **open** — this session
> updates it, it does not do it)

## What was out of date, and in which direction

Both artifacts were stale in the **flattering** direction: they read as ready, and every line in
them was true **when written**. Nothing announced itself.

| | Ticket `#62` | The brief |
|---|---|---|
| Last written | 2026-08-22 | 2026-08-23, plus one paragraph from `tour-refresh` at 00:26 today |
| Blockers | named `#59`/`#60`/`#61` as live blockers — **all three closed** | already corrected |
| Ordering | silent | *"`#67` and `#68` in either order, then this"* — **`#68` closed 2026-08-23** |
| Shot list | three bullets: calendar, Google calendar, sane percentage | the same three |
| Model policy | none at all | two components — video and image |
| Video pick | — | **Seedance 2.0**, from a June 2026 ranking |
| Image pick | — | **Nano Banana Pro** |
| Voice | **absent** | **absent** |
| Reporting | not asked for | not asked for |

## What the shot list was missing — seven tickets, and one of them changed the tour itself

The brief's list named three surfaces. Between them, `#63`–`#70` and `tour-refresh` put **eight
more** on screen, all after the take that this ticket exists to replace:

- **`#68` drag to move** — the gesture, the scope sheet where a rule exists, *skip* as the second
  entry point, and the `TooLarge` snackbar naming both numbers. It is a *showable* gesture, which is
  rare in this app, and it was not in the shot list at all.
- **`#64`'s success/failure run** — `kept · missed · still-owed`, drawn by **form** and never hue, no
  red anywhere and **no ratio anywhere**. The brief now says outright that if the narration calls it
  a score or a rate, the narration is wrong.
- **`#65` + `#66`** — the measure proposal and *no number — 11 entries logged*. Recorded as **one
  shot**: they are one story on screen.
- **`#69`**, **`#70`**, **`#63`** — smaller, listed with what they are worth to a camera.
- **`tour-refresh`** — the tutorial itself is v2: step 6 is the Calendar tab and carries `#68`'s drag
  as its second sentence, step 7 absorbed Profile, `TUTORIAL_VERSION` `1` → `2` so it **re-runs by
  itself** on any device that has seen it. `docs/marketing/tour-timecodes.md` describes the **old**
  seven steps and is now flagged as a format to copy, not a map to follow.

## The model section: two components became three, and both picks moved

Ido's words: *"use the best model found for each component (video, voice, image) — like Seedance 2.5
or FLUX — and write which model you used."*

| Component | Now | Was | Why it moved |
|---|---|---|---|
| 🎬 Video | **Seedance 2.5** | Seedance 2.0 | 2.5 shipped **2026-07-31**, after the ranking the brief cited. 30 s in a **single take** (the old opening was five clips stitched), native audio, 4K, region-level editing, up to 50 multimodal references |
| 🗣️ Voice | **ElevenLabs v3**, with **Multilingual v2** as the long-form fallback | **nothing** | The component did not exist in the brief. OpenArt's TTS *is* ElevenLabs; v3 is the expressive ceiling, Multilingual v2 the stable long-form model — mixing per act is allowed, and saying which act got which is required |
| 🖼️ Image | **FLUX.2 Pro** | Nano Banana Pro | Photoreal texture and the cheapest to iterate, which is what a start-frame workflow does. **Nano Banana 2 Pro** kept as the runner-up for a frame that must obey a precise instruction; **GPT Image 2** only for exact typography, which should never be needed because on-screen words belong in the editor |

**The roster wins, not the table.** The MCP has a *discover available models* tool and OpenArt adds
models to it automatically, so the first model call of the session is the roster call and the three
picks are labelled a **prior**.

⚠️ **And the picks are hedged, because none of this was measured here.** `Inferred:` every ranking
is trade-press and vendor-page reporting read today. `Untested:` **whether OpenArt's roster actually
carries Seedance 2.5** — OpenArt's own MCP page still lists **Seedance 2.0** among its video models
while a 2026 platform round-up lists 2.5, and 2.5 is three weeks old, which is recent enough for a
roster to lag. Those two sources disagree and **only the roster call settles it**. Writing that down
is the difference between the next session spending one call and spending an afternoon.

## 📣 The report is now a deliverable, not a courtesy

Per component: the **exact model string as OpenArt names it** (not the marketing name), why it in one
line, what it cost in credits and re-rolls, and for voice the preset and which acts got it. It goes
in the reply, the changelog and a closing comment on the ticket. **A component that was not reached
still owes its row** — *"voice: not reached, the MCP has no TTS surface"* is a complete answer;
silence is not.

## One correction to the brief's own MCP paragraph

It said Director's timeline and voiceover were *"stated nowhere and unconfirmed"*. Re-checked today:
OpenArt's MCP page makes **no mention of voice, text-to-speech, audio or timeline control**, and
lists *Character* and *Smart Shot* as roadmap. That is **weaker than unconfirmed** — it is an absence
in the vendor's own tool list. The three-route table now marks *"only model calls are reachable"* as
**the likely case** rather than one of three equals.

## Two smaller repairs

- **The front matter's dated paths were pinned to `2026-08-23`** — `CHANGELOG/2026-08-23/…`,
  `kb-candidates/2026-08-23-…`. A row carrying a date the session did not work sends the next reader
  to a path that does not exist and it reads the row as abandoned. They now say `<the day you run>`,
  with the reason inline.
- **The three stacked "UNBLOCKED / DO #67 FIRST / status: blocked" notices** were contradicting each
  other at the top of the file. They are kept **verbatim** under a *Historical* heading — nothing
  deleted — with one current state block above them.

## The path was contested, and the sibling released it

`sessions/62-tour-video-v2.md` sat on `tour-refresh`'s **live** row for one paragraph. It was not
touched: that session was messaged, and it answered *"take it"* and released the row (`4ddbced`,
whose message says *"62's brief is free"*). It also volunteered two things this session did not know
— that step 6 carries `#68`'s drag as its second sentence, and that `emulator-5554` still holds a
live sign-in from its own `adb install -r` pass. Both are now in the brief.

## 🧪 Tests

**No code changed**, so no test layer applies. What was verified instead, mechanically rather than by
reading:

| Check | Result |
|---|---|
| Ticket body round-trips byte-identical after `gh issue edit` (read back from a file with explicit `encoding='utf-8'`, never through `stdin`) | **identical: True**, both writes — 14 056 then 14 710 chars |
| Every markdown table in both artifacts has a consistent cell count | **0 mismatches**, 315-line brief and 120-line ticket |
| Front matter still parses, all eight keys present | ✅ |
| Every `../` relative link in the brief resolves on disk | **none broken** |
| Superseded model names survive only inside supersession sentences | ✅ — 3 hits in the brief, 2 in the ticket, all deliberate |

## Not done, and it is deliberate

- **No KB candidate was written.** The one finding worth carrying — that a quoted heredoc eats a
  backslash before Python sees it, which cost two attempts here — is **already in the KB**, at
  `kb/dev/escapes-die-in-transit.md` §8 with the identical `'\\'` → `'\'` case and the same remedy
  (write the script to a file). The bundle check cost one `grep` and saved a page nobody needed. That
  is the finding the last drain asked for, applied.
- **`docs/marketing/tour-timecodes.md` and `explainer-video-brief.md` are stale and were left
  alone.** They are `#62`'s own to rewrite — `docs/marketing/**` is on that brief's `owns` list — and
  both are now flagged in the brief as stale rather than silently trusted.
- **`#62` stays open.** This session updates the ticket; it does not do the work in it.

## Files

| File | What |
|---|---|
| `sessions/62-tour-video-v2.md` | rewritten: state block, shot list, model section (3 components), assembly table, device state, exit, front-matter dates. 14 509 → 24 393 chars |
| GitHub issue `#62` — body and title | rewritten to match; title dropped *"after #59/#60/#61 land"*, which is now history |
| `SESSIONS.md` | claim, then release |
| `CHANGELOG/2026-08-24/62-kickoff-refresh.md` *(new)* | this file |
