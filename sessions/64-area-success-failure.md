---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
# Was `blocked_on: [63]`. Cleared 2026-08-23: #63 shipped in 7c457c4 and closed
# 2026-08-22T23:37Z; its brief is in sessions/done/ with status: done.
issue: 64
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/SuccessFailureRun.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/AnalyticsScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/AnalyticsViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildSuccessFailureRunUseCase.kt
  - app/src/test/java/com/idomarhaim/goalpilot/domain/SuccessFailureRunTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/SuccessFailureRunUiTest.kt
  - kb-candidates/2026-08-23-64-area-success-failure.md
  - CHANGELOG/2026-08-23/64-area-success-failure.md
  - sessions/64-area-success-failure.md
created: 2026-08-23
---

# `#64` — `kept · missed · still-owed`, and never a rate

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

✅ **UNBLOCKED 2026-08-23 — [#63](https://github.com/idomarhaim/Android_Final_Project/issues/63) has
shipped** (`7c457c4`, closed 2026-08-22T23:37Z, brief in `sessions/done/` with `status: done`). The
paragraph below is kept as written because its instruction — *check #63 has shipped before claiming* —
is still the right habit; it has simply been carried out. Verify rather than trust this line.

⚠️ **COLLIDES with [`#66`](https://github.com/idomarhaim/Android_Final_Project/issues/66)** — both own
`feature/lifeareas/LifeAreaDetailScreen.kt` and `feature/analytics/AnalyticsScreen.kt`, so **these two
cannot run in parallel.** Prefer `#66` first: it *removes* a meaningless percentage from those two
screens, and this ticket *adds* a run to them — doing the removal first means the new component lands
beside a screen that already tells the truth, instead of beside a number that then has to be revisited.

**Historical, now satisfied:**
The run counts **missed windows**, a window *is* an occurrence, and that collection does not
exist yet. Check #63 has shipped before claiming.

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`docs/PRODUCT_v0.3.md` §4.7](../docs/PRODUCT_v0.3.md) — **the spec, closed** · the prototype at
[`docs/prototypes/2026-08-13-area-success-failure/`](../docs/prototypes/2026-08-13-area-success-failure/)
(revision 5, which is what `#41` was resolved against) ·
[`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64).

**Task:** build `C19`'s success/failure component, in both its placements.

## Why nothing is there today, in the codebase's own words

[`LifeAreaDetailScreen.kt`](../app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailScreen.kt)
already carries the note:

> ⚠️ *"**The success/failure run belongs above the list and is not built here.** … It is left out
> rather than mocked up: the run counts **missed windows**, and windows are `occurrences`, a
> collection §7.1 marks **new**. A placeholder drawn from what exists today would be the map's
> most-repeated finding — **a second number that quietly disagrees.**"*

**Honour that.** If #63 has not landed, the right outcome of opening this brief is to say so and
stop — not to draw something approximate.

## The decisions — every one of them was won against a first answer

> **A failure is a missed *window* and nothing else; a goal with nothing due is missing a *step*,
> not failing.**

- **`C9a`'s three words are the whole vocabulary: `MISSED` is a failure, `OVERDUE` is not,
  `EXPIRED` counts for nothing.**
- **A success counts in full in every area** the task serves, while its **minutes divide** — and
  both numbers sit on **one screen deliberately**, because that asymmetry *is* the point. The
  sentence explaining it lives **beside the time donut on analytics and nowhere else**.
- **Two numbers, never a rate.** A single "success rate" is the tidying-away `C3` and `C5` both
  refused. Do not add one, even as a subtitle.
- **Nothing ages out.** History is permanent; the view reports over a window you pick —
  `30 days · 8 weeks · 6 months`, default **8 weeks**. A window is **a filter over history, not
  decay of it**.
- **There is no lifetime failure counter anywhere.** A number that can only rise is *the list of
  the things you are bad at*, which this screen exists **not** to be.
- **What a window is** is answered **on the screen**, under the run: *a window counts as kept when
  everything due in it was done*. It is **not** spec-only text — the numbers are meaningless
  without it.

**No dormancy state, stored or named.** *Asleep · invisible · failed* were three labels for
`C10`'s already-decided theme axis, whose `STARTING` value **is** *"never scheduled"*. The row
shows **`no next step · idle 4 months`** and offers the step that is actually missing:

| Situation | Offer | Whose feature |
|---|---|---|
| `open work = 0` | **Break it into steps** | `C8` — **no new AI surface** |
| work exists, no dates | **Schedule the first one** | `C9a` |

Counted in **neither** number, **and the screen says so in words**. **`Let it go` stays a command,
never an inference** — `C4` forbids the app asserting an intrinsic edge by itself.

## One component, two placements

Above the goal list on the **life-area screen**, and beside the time donut on **analytics**. One
component, so it cannot drift into two answers.

## Exit

- **JVM tests carry this ticket.** The counting rules are the feature: `MISSED` counts and
  `OVERDUE` does not, `EXPIRED` counts for nothing, a success counts whole in each area while
  minutes divide, the window is a filter, and a goal with nothing due lands in **neither** number.
  Write them against the rules above, one test per sentence.
- **Instrumented test + render pass** for both placements. §0.8 is suspended, so **English only**.
- ⚠️ `adb install -r` + `am instrument`, **never `connectedDebugAndroidTest`** — it uninstalls the
  app and takes the Google sign-in with it.
- `CHANGELOG/2026-08-23/64-area-success-failure.md` with counts verbatim.

## Out of scope

The `occurrences` collection (#63) and any **new** AI surface — *Break it into steps* is `C8`'s
existing feature and must be reused, not re-specced.
