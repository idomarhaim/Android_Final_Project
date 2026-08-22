# tour-video — four kickoffs, four tickets, and the KB drained

> **Summary:** Turned the video session's findings into work anyone can pick up: GitHub issues [#59](https://github.com/idomarhaim/Android_Final_Project/issues/59)–[#62](https://github.com/idomarhaim/Android_Final_Project/issues/62) with matching briefs in `sessions/`, covering the Health Connect mismatch that reads **245613%**, the in-app calendar surface, the dedicated Google Calendar, and re-recording the tour plus assembling it in OpenArt Director. Also answered the question underneath all of it — **task scheduling exists as data and not as a surface** — and drained this session's five KB candidates into the central bundle (`5890864` in `C:\Dev\JARVIS`).

**Date:** 2026-08-23 · **Session:** `tour-video` · **Mode:** AUTO

## The question that shaped two of the four

Ido asked whether task scheduling into a calendar happens today — *"at least in the app's own
calendar?"*. Checked rather than recalled, and the answer is **half**:

| | today |
|---|---|
| a task takes a **day** (`ALL_DAY`) | ✅ the *When?* chip on the add-task row |
| …and a **time**, promoting it to `DEADLINE` | ✅ the clock button beside it |
| `BLOCK` and `SPAN` | ⚠️ **fully modelled and fully reminded in `Occurrence.kt`, with no UI author at all** |
| a calendar to **look at** | ❌ nothing — no month, week or day grid anywhere |
| Google Calendar | ❌ `grep` for `googleEventId`, `calendar.app.created` or any calendar client over `app/src/main` returns **zero hits**; only Google **Tasks**, read-only, ships |

So the data is scheduled and the surface is missing — which is exactly what `WhenPicker.kt`
says of itself: *"the smallest control that makes the model reachable rather than a calendar
built in passing"*. That produced **two** tickets rather than one, because the in-app surface
and the Google mirror are independent and the first must be useful with no Google account at all.

**Both designs are already closed and neither ticket reopens them.** `#26` decided the surface
(spec §4.3), and `#17`/`#27`/`#28`/`#33` decided the Google side (§2.6–§2.8). They are closed
because the **decision** was taken, not because anything was built — so each new ticket carries
the decisions forward verbatim and says *do not reopen this*.

## What shipped

| ticket | brief | state |
|---|---|---|
| [#59](https://github.com/idomarhaim/Android_Final_Project/issues/59) — steps credited to an unrelated Fitness goal | [`sessions/59-health-metric-mismatch.md`](../../sessions/59-health-metric-mismatch.md) | ready |
| [#60](https://github.com/idomarhaim/Android_Final_Project/issues/60) — build the in-app calendar surface | [`sessions/60-calendar-surface.md`](../../sessions/60-calendar-surface.md) | ready |
| [#61](https://github.com/idomarhaim/Android_Final_Project/issues/61) — the dedicated GoalPilot calendar in Google Calendar | [`sessions/61-google-calendar.md`](../../sessions/61-google-calendar.md) | ready |
| [#62](https://github.com/idomarhaim/Android_Final_Project/issues/62) — re-record the tour, then assemble the film | [`sessions/62-tour-video-v2.md`](../../sessions/62-tour-video-v2.md) | **blocked on #59, #60, #61** |

### #59's cause, found rather than guessed

`BuildHealthProposalsUseCase.match()` prefers a goal whose **unit** agrees with the metric and
then falls back to `candidates.firstOrNull()` — **any** unpinned active goal in the category. A
daily step count therefore lands on a Fitness goal measured out of 100. `SyncHealthDataUseCase`
**pins** the result, so the pairing becomes permanent and every sync tops it up, which is why
the number grew while the last session watched it.

**The KDoc directly above that line already names the hazard** — *"preferring one whose unit
already agrees so steps do not get added to a "workouts" goal and inflate it by four thousand"*.
The preference is real; the fallback is unguarded. The brief says so, and says the **data
repair is always-ask** and Ido's to decide.

### Why #62 is one session and not two

Ido asked. The assembly is driven by the **beat timecodes**, and the timecodes are a by-product
of the **recording** — split them and the second session either re-derives the beat map by
scrubbing a video or trusts one it did not make.

`Observed:` 2026-08-23 — OpenArt runs an MCP server at `https://mcp.openart.ai/mcp`, OAuth, no
API key. `Untested:` its published tool list is a **model-calling** surface (generate, discover
models, upload reference files, credits, workspaces) with **no mention of Director's timeline,
voiceover or captions**, so whether Director itself is MCP-reachable is unconfirmed. The brief
makes enumerating the tools the first unit of work and tables the three routes that follow.

## 📥 Ingested

Five candidates → `C:\Dev\JARVIS`, commit `5890864`:

- 📥 **`screenrecord` for a long walkthrough** → `kb/dev/android-device-verification.md` §6.7
- 📥 **`keyevent 4` after typing exits the app on this AVD** → same page, §16.1
- 📥 **a fixed swipe count is not a location** → §16.2
- 📥 **timecoding a blind take, and the parser defect in doing it** → §16.3
- 📥 **Google Flow cannot narrate footage you already have** → `kb/dev/tool-adoptions.md`

`kb-candidates/2026-08-22-tour-video.md` is fully drained and deleted in this commit.
`kb-candidates/2026-08-22-tutorial-onboarding.md` is **not** mine — session
`tutorial-onboarding` holds a live board row — and was left alone.

🎬 **The walkthrough Ido waived was moot:** no candidate was destined for `rules/`, so the gate
never fired. Recorded because *"waived"* and *"never applicable"* are different facts about the
same commit.

## 🧪 Tests

**No code changed** — this session wrote four briefs, four issue bodies, two KB pages and two
changelogs. The Kotlin, instrumented, Firestore-rules and functions suites were not run and
nothing under `app/`, `functions/` or `firestore-tests/` was touched.

What was verified mechanically:

- **`Check-KbLinks.ps1` over the central bundle — 112 pages, CLEAN**, no broken links, orphans
  or wikilinks.
- **Anchors recomputed, because the link checker does not check fragments.** Every heading slug
  in the bundle was re-derived and diffed against every in-bundle deep link: **9 checked, all
  resolve.**
- **The section number in the candidate file was wrong and the append caught it.** The
  candidates targeted *§15* of the device page, written when it ended at §14 — and `## 15.` had
  landed the same day from another session. Asserting the heading was **absent** before writing
  turned a silent duplicate into a loud failure; the section shipped as **§16**.

## Device

Not touched this session. No `adb`, no emulator, no Gradle. The AVD singleton claimed yesterday
is released with this commit.
