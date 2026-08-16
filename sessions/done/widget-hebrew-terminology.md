---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: AUTO MODE
status: done
issue: 51
created: 2026-08-16
closed: 2026-08-16 by `widget-hebrew-terminology` — `cd49bda`
---

> **Closed `cd49bda`.** Account:
> [`CHANGELOG/2026-08-16/widget-hebrew-terminology.md`](../CHANGELOG/2026-08-16/widget-hebrew-terminology.md).
>
> Two corrections to this brief, both found by reading the strings against the
> code that fills them:
>
> 1. **The proposed `נכון לתאריך %1$s` was factually wrong.** The argument is
>    `AndroidWidgetStrings.asOfShort` = `DateFormat.getTimeFormat`, a **clock
>    reading and never a date**. Shipped as `נכון לשעה %1$s`.
> 2. **The nine defective strings were ten.** `gp_widget_effort_lead`
>    (`ו־%1$s נמצאת ב־%2$s.`) carries §4.8's defect **twice** — `%1$s` is a
>    user-authored goal title that may be Latin, `%2$s` is a percentage — and was
>    not in the list. Fixed as part of the brief's own stated defect class.
>
> The blocking question was answered **negatively and deliberately**: Hebrew has
> no second noun for a measure's numeric target once יעד names the entity, so the
> file names the **measure** instead and never the target. `היעד המספרי` was
> rejected. Research and sources are in the changelog and in the file's header.

# `widget-hebrew-terminology` — fix the widget pack's Hebrew, and research the wording first

**Read first:** [`AGENTS.md`](../AGENTS.md), then
[`#51` comment recording Ido's decision](https://github.com/idomarhaim/Android_Final_Project/issues/51#issuecomment-5308165658).

## Task

Fix the nine defective strings in `app/src/main/res/values-iw/widget_strings.xml`. Two independent
defects live in that one file.

**1 · Terminology — decided by Ido, 2026-08-16.** `Goal` is **יעד**, never **מטרה**. Spec §5.1 /
`E1` already said so; he ratified it. Six strings violate it (lines 27, 28, 42, 51–53, 76), and
`gp_widget_goals_ring_meaning` (line 49) uses **both** words in one sentence **with the meanings
swapped**.

**2 · §4.8 bidi — never attach a Hebrew prefix to a Latin or digit run.** `ל־%1$d` (lines 51–53)
and `ל־%1$s` (line 37, `gp_widget_as_of`) render on the **far side** of the run, the same class as
`מ‑Health Connect` → `Health Connect‑מ`.

## Why this is a session and not a ten-minute edit

**Research the Hebrew before you write it.** This is the reason Ido asked for a fresh session
rather than letting the previous one do it inline.

The blocking question: **once `יעד` is taken by `Goal`, what is the Hebrew for a measure's numeric
target?** `יעד` is the natural word for *target* too, which is exactly why the existing string
collides. Look up how Hebrew-language habit/goal/fitness apps actually say it — do not invent a
phrase, and do not calque the English.

Same for the two bidi rewrites: `נכון לתאריך %1$s` and a standalone-word form for the plural, but
**check they read naturally** rather than merely parsing.

Ido pre-approved this set as a starting point. Treat it as a floor, not a ceiling — if research
says better, take the better wording and say why:

| String | English | Proposed |
|---|---|---|
| `gp_widget_goals_ring_meaning` | *Each ring is progress toward that goal's own target.* | `כל טבעת מראה את ההתקדמות אל היעד המספרי שהוגדר.` |
| `gp_widget_as_of` | *as of %1$s* | `נכון לתאריך %1$s` |
| `gp_widget_goals_ring_none` ×3 | *…%1$d of yours has none.* | `…ולחלק מהיעדים שלכם (%1$d) אין.` |
| `gp_widget_no_goals` | *No goals yet — tap to add one* | `אין עדיין יעדים — הקישו כדי להוסיף` |

## Carries over

- **The decision itself** — `#51` comment above. It is the only durable record; it was made in
  chat.
- **The prior art for the bidi remedy** — `res/values-iw/analytics_strings.xml`, whose header
  states all three rules and which `51b-sweep-analytics` already got right. Copy that approach.
- **The KB page that generalises both defects** —
  `C:\Dev\JARVIS\kb\dev\untranslatable-idioms.md` §3.

## Out of scope

- **Every other package.** `ui/components/` is the next sweep unit and is not this one.
- **The `LocalContext`-into-Dialog defect** (`51c` filed it on `#51`) — app-wide, wants a shared
  `ui/locale/` wrapper, and is a bigger unit than this.
- **`formatMinutes` hardcoding `h`/`m`, and `"Unassigned"`** — filed on `#51`, outside this file.
- **Changing the English.** Only `values-iw/` is in scope.

## Exit

- `:app:testDebugUnitTest` green. **No `--rerun-tasks` needed** — `resource-guard-inputs` fixed
  that on 2026-08-16 (`ced0561`): `res/` and `src/` are now declared inputs to every `Test` task.
  *(This brief said the opposite when it was written; corrected once the fix landed.)*

  **Read `C:\Dev\JARVIS\kb\dev\scanned-files-are-not-task-inputs.md` before you trust a green run
  anyway.** The mechanism was nastier than "Gradle doesn't watch `res/`": `R.jar` is keyed on
  resource **names**, not values, so **adding** a key invalidated the test task while **changing
  what a key says** did not. The blindness was selective and flattering, and it was off on exactly
  the commit shape this unit produces — a resource-only value edit. Your unit is the first that
  gets an honest green.
- A Hebrew render of the widgets on a device is **owed but may not be reachable** — see below.
- Your own `CHANGELOG/2026-08-16/widget-hebrew-terminology.md`.
- Commit on the unit; push under `AUTO MODE` once the six preconditions hold.

## ⚠️ Two emulator traps, from `51c`

1. **The sign-in is gone again.** `connectedDebugAndroidTest` **uninstalls the app**, which takes
   the account with it. Ido has to sign in by hand each time.
2. **Therefore an instrumented run and a render pass are mutually exclusive on one device.** Decide
   which you need *before* you start, and if you need the render, ask him to sign in and do **not**
   run `connectedDebugAndroidTest` afterwards.
