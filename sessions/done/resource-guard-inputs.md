---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: AUTO MODE
status: done
issue: 51
created: 2026-08-16
landed: c477557 (GoalPilot) · b23eba4 (JARVIS, the KB half)
---

# `resource-guard-inputs` — the localization guards can silently not run

**Read first:** [`AGENTS.md`](../AGENTS.md), then `CHANGELOG/2026-08-16/51c-analytics-render.md`.

## Task

`51c-analytics-render` found this and correctly left it — it lives in `app/build.gradle.kts`,
outside that unit's package.

> **Gradle does not treat `res/` or `src/` as inputs to `testDebugUnitTest`, so a resource-only
> change leaves the task `UP-TO-DATE` and the file-scanning guards do not run at all.**

Declare the inputs so they do.

## Why this is first in the queue despite being small

**It makes every other localization guard unreliable, and it fails in the flattering direction.**
`HebrewLocaleResourceTest` (parity), `AnalyticsLiteralSweepTest` (prose literals) and
`WidgetHebrewResourceTest` all scan files rather than exercise code. A resource-only edit — which
is *precisely* what a sweep session produces — reports **green without executing a single
assertion**.

So the guard the sweep depends on is off exactly when the sweep is what changed. Every remaining
`#51` package inherits this, and so does `widget-hebrew-terminology`, which is a resource-only unit
by construction.

The current workaround is `--rerun-tasks` after resource edits, which works and which nobody will
remember.

## Carries over

- The finding and its evidence — `CHANGELOG/2026-08-16/51c-analytics-render.md`, filed on `#51`.
- The three guards that depend on it: `app/src/test/java/com/idomarhaim/goalpilot/resources/`
  (both files) and `.../widget/WidgetHebrewResourceTest.kt`.
- The general form is already in the KB —
  `C:\Dev\JARVIS\kb\dev\jvm-vs-android-locale-codes.md` §4a, *a sweep is an event, not a state*.
  **This is that section's own guard being unenforced**, which is worth a line back to the page if
  the fix turns out to be non-obvious.

## Out of scope

- Any string, resource or sweep work. This is a build-configuration unit only.
- The other two defects `51c` filed (`LocalContext` into dialogs, `formatMinutes`) — separate
  units, both bigger.

## Exit

- **Prove the fault before you fix it**, then prove the fix: edit a `values-iw/` string so a guard
  *should* fail, confirm `:app:testDebugUnitTest` currently reports `UP-TO-DATE` and green, apply
  the input declaration, and confirm the same edit now fails. Revert the deliberate break.
  A guard whose fix is not checked against its own fault is the thing this session exists to stop.
- Full suite green, and green **without** `--rerun-tasks`, which is the whole point.
- Your own `CHANGELOG/2026-08-16/resource-guard-inputs.md`.
- Commit; push under `AUTO MODE` once the six preconditions hold.
