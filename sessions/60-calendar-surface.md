---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
issue: 60
owns:
  # A new feature package plus its route. The Occurrence model is READ, not written --
  # BLOCK and SPAN are already fully modelled; what is missing is a UI author for them.
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/**
  - app/src/main/java/com/idomarhaim/goalpilot/ui/navigation/Destinations.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/root/GoalPilotRoot.kt
  - app/src/test/java/com/idomarhaim/goalpilot/feature/calendar/**
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/CalendarSurfaceUiTest.kt
  - kb-candidates/2026-08-23-60-calendar-surface.md
  - CHANGELOG/2026-08-23/60-calendar-surface.md
  - sessions/60-calendar-surface.md
created: 2026-08-23
---

# `#60` — the calendar you can look at

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Read first:** [`AGENTS.md`](../AGENTS.md) · then
[`docs/PRODUCT_v0.3.md` §4.2 and §4.3](../docs/PRODUCT_v0.3.md) — **that is the spec and it
is closed** · then the prototype at
[`docs/prototypes/2026-08-10-calendar-surface/`](../docs/prototypes/2026-08-10-calendar-surface/)
· then [`#60`](https://github.com/idomarhaim/Android_Final_Project/issues/60).

**Task:** build §4.3's calendar surface.

## Why this exists, in one paragraph

Ido asked whether task scheduling into a calendar happens today. **Half of it does.** A task
takes a **day** (`ALL_DAY`) and optionally a **time** (`DEADLINE`) through the *When?* chip
on the add-task row. But `BLOCK` and `SPAN` are **fully modelled and fully reminded in
`Occurrence.kt` with no UI author at all**, and there is **no calendar to look at** — no
month, week or day grid anywhere in the app. `WhenPicker.kt` says so itself: it is *"the
smallest control that makes the model reachable rather than a calendar built in passing"*.
[`#26`](https://github.com/idomarhaim/Android_Final_Project/issues/26) is closed because the
**design** was decided; this is the build.

## Do not reopen the design — every line of it was won against a first answer

- **Default view is 3 days, and that is measurement, not taste.** Seven columns on a 390 dp
  phone is **~46 dp per day**; no Hebrew title and no time range survives it. At 3 days a
  column is ~110 dp and both fit. Week view stacks times **start over end** — the only thing
  that fits at 46 dp.
- **Fully actionable** — create by FAB **or by tapping a slot**, drag to move, tick to complete.
- **Shows** challenge windows, goal deadlines, hand-made Google events **in grey**, and
  **a strip for work due today that was never given a time**. Without that strip the calendar
  quietly lies about the day's real workload, which is the one thing it exists to tell the truth about.
- **A `DEADLINE` is only ever a banner in the all-day strip**, never a timed box.
- **The rung is carried by the form of the leading time column, never by a glyph on the chip.**
  The chip carries **only** the life area — a colour dot and its name. No legend, no symbol
  vocabulary. (§0.8's surviving sub-rule: one chip may not carry two axes.)
- **A per-day load bar** and a *booked/free* ring, **arithmetic not inference** — so they cost
  nothing against §0.1's free-model rule. Red past **75 % of waking hours**. **Spans contribute nothing.**
- **`OVERDUE` *and* `AWAY` are both carried forward from other days.** Both need action and
  neither waits for you to navigate to its date — an event that vanished from Thursday would
  otherwise surface when Thursday arrives, exactly too late to put it back.

## What to build

1. The **3-day** surface, with a week view, reachable from navigation as §4.2 decides.
2. **UI authors for `BLOCK` and `SPAN`** — creating by tapping a slot is what makes `BLOCK`
   reachable for the first time in the product's life.
3. The all-day strip, the untimed-work strip, the per-day load bar, the booked/free ring.
4. Wire the existing `Settings → Your day` waking hours into the 75 % threshold. It is already
   stored (`DaySchedule`, `AppPreferencesRepository.daySchedule`) — **do not re-invent it**.

## Exit

- **JVM tests** for everything arithmetic: the load bar, the booked/free ring, carry-forward
  of `OVERDUE`/`AWAY`, and the `DEADLINE`-is-never-a-box rule. This is where most of the
  value is, and it needs no device.
- **Instrumented test** for the surface, plus a **render pass** — and note §0.8 is suspended,
  so **English only**, no Hebrew render pass.
- ⚠️ **Use `adb install -r` + `am instrument`, never `connectedDebugAndroidTest`** — that task
  uninstalls the app and takes the Google sign-in with it
  (`C:\Dev\JARVIS\kb\dev\android-device-verification.md` §8).
- `CHANGELOG/2026-08-23/60-calendar-surface.md` with pass/fail counts verbatim.
- Commit on the auto-mode trigger.

## Carries over

- The whole design — [`docs/PRODUCT_v0.3.md` §4.2–§4.3](../docs/PRODUCT_v0.3.md).
- The prototype that answered *"what should this be"* — [`docs/prototypes/2026-08-10-calendar-surface/`](../docs/prototypes/2026-08-10-calendar-surface/).
- The rung semantics and why `BLOCK` has no author yet — [`Occurrence.kt`](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Occurrence.kt), especially line 346 and `OccurrenceDraft`.

## Out of scope

**Google Calendar, in either direction.** That is
[`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61), and this surface has
to be complete and useful with no Google account at all. The grey hand-made-Google-events
layer is specced here but has nothing to read until `#61` ships — build the slot for it,
leave it empty, and say so.
