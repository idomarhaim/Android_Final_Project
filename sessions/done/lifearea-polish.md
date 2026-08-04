---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: done
created: 2026-08-03
completed: 2026-08-04
commit: 6f4a749
---

# Life areas: ordering and visibility

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`. (The session that built life areas ran under `AUTO MODE`; that
was scoped to it. Say `AUTO MODE` again if you want this one to commit and push
without asking.)

**Read first** — [`AGENTS.md`](../AGENTS.md), then the "Life areas and the
time-allocation chart" section of [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md),
then the OPTIONAL block in [`TODO/TODO.md`](../TODO/TODO.md).

**Task** — the first two follow-ups under **"Life areas + time-allocation
analytics"** in `TODO/TODO.md`:

1. **Reordering life areas.** `LifeArea.sortOrder` is already persisted, already
   honoured by `LifeAreaRepositoryImpl.observeLifeAreas` (`compareBy(sortOrder,
   name)`), and already assigned sensibly on create and on sync. Only the drag
   handle is missing, plus a repository call that writes a reordered block in one
   batch. Reordering must not renumber every area on every drag — a batch write of
   the affected span is enough.
2. **Show the life area on the goals list.** Today an area is visible on a goal's
   own screen (`GoalDetailScreen` header) and in the add/edit chip row, but the
   goals list shows nothing, so "my goals belong to areas" is invisible where a
   user actually looks at their goals. `GoalsViewModel` needs the life-areas flow;
   decide between a chip per `GoalCard` and grouping headers per area — the TODO
   leans towards headers, but that is a judgement call to make against the real
   list, not in the abstract.

**Carries over**

- The model, repository and palette all exist and are documented:
  `app/src/main/java/com/idomarhaim/goalpilot/domain/model/LifeArea.kt`,
  `.../domain/repository/LifeAreaRepository.kt`,
  `.../data/firestore/LifeAreaRepositoryImpl.kt`.
- The screen to extend: `app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/`.
- Why an area's colour and icon are what they are (categorical palette, bilingual
  icon guesser): `CHANGELOG/2026-08-03/lifeareas.md`.
- `ui/components/GoalCategoryIcon.kt` already resolves a `LifeArea.iconKey` via
  `iconForKey` / `LifeArea.icon()` — do not add a second icon table.
- Chart-animation traps, if you touch anything animated:
  the `AGENTS.md` pitfalls about `animateFloatAsState` and about `BarItem`'s
  restart key.

**Out of scope**

- The other two follow-ups in the same TODO block — AI re-estimation of old task
  durations and the trend chart. They have their own brief
  (`sessions/time-insights.md`) and a disjoint working set; do not pull them in.
- Anything in `feature/analytics/` beyond reading it.
- The two MUST items (two-account demo, spec title page).

**Exit**

- `:app:testDebugUnitTest` green, and `:app:connectedDebugAndroidTest` green if you
  touch a composable (the emulator `Pixel_10_Pro_XL` is an exclusive singleton —
  claim it on [`SESSIONS.md`](../SESSIONS.md) first).
- Your own `CHANGELOG/2026-08-03/<session-label>.md` (or today's folder), written
  before the commit and used verbatim as the commit message.
- Commit on approval; flip the TODO checkboxes only once Ido confirms.
- Release your row on `SESSIONS.md`, and move this file to `sessions/done/` with
  the commit hash.
