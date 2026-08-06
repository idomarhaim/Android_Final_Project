<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 🗂️ TODO Index — GoalPilot

Backlog index. Per-area files live under three priority subfolders next to this index:

- `TODO_MUST/` — `<Area>.TODO.must.md`
- `TODO_OPTIONAL/` — `<Area>.TODO.optional.md`
- `TODO_FUTURE/` — `<Area>.TODO.future.md`

## 🚦 Priority order
1. Finish every `must` item across all areas first.
2. Then tackle `optional` items.
3. `future` is roadmap — only worked on after explicit user go-ahead.

## ✅ Status legend
`[ ]` todo · `[~]` in-progress · `[x]` done · `[-]` deferred

## 📋 Per-area files

### 🔴 MUST — [`TODO_MUST/`](TODO_MUST/)
- [x] **Wire real credentials** and verify end-to-end on a device — done 31/07/2026.
  Firebase project `goalpilot-56e30` on Blaze; Firestore + Storage + auth + rules
  + all three Cloud Functions live in `us-central1`. Verified on the emulator:
  sign-in → goal → task → completion transaction → photo upload → GROQ, each
  confirmed against Firestore/Storage/function logs rather than the UI. See
  `CHANGELOG/2026-07-31.md`.
- [Submission.TODO.must.md](TODO_MUST/Submission.TODO.must.md) — the two items
  that block handing the project in. Both are largely **manual**: an agent cannot
  sign into Google as a second account, nor supply your name/ID/course number.
  - [x] **Demo the sharing requirement (spec §7) with two Google accounts** —
    done 05/08/2026. Friends-only leaderboard with both accounts, the shared
    feed item, and a challenge created by A / joined and scored by B as a
    non-owner, with A's screen re-ranking live. Most of it turned out to have
    been in place since 02/08 — the item's premise was stale.
    See `CHANGELOG/2026-08-05/submission.md`.
  - [x] **Fill in the spec title page** — done by Ido; **confirmed and closed
    06/08/2026.** `GoalPilot_spec_EN.docx` now reads
    `Submitted by: Ido · [Ido Mar-Chaim 209497072] · [10208]`; the
    `[Full name & ID] · [Course number]` placeholder is gone. The template's
    square brackets are still around the name/ID and the course number — **Ido
    ruled them cosmetic**, so this is closed, not deferred. The file stays
    *Frozen / off-limits* in [AGENTS.md](../AGENTS.md): no agent edited it, and
    the docx itself is **not** in this session's commit — it is Ido's to commit.

  🎉 **With this, every MUST item is closed and nothing blocks submission.**

### 🟡 OPTIONAL — [`TODO_OPTIONAL/`](TODO_OPTIONAL/)
- [ProductReview.TODO.optional.md](TODO_OPTIONAL/ProductReview.TODO.optional.md) —
  the **actionable** half of the 2026-08-06 product/UX brief: 5 defects to
  reproduce and fix, 6 single-session UX items, and 4 additions from the agent's
  own static pass. Source text transcribed to
  [`Product and UX Reviews/2026-08-06-brief-review.md`](../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md)
  as `R1`–`R28`; everything here cites those ids. **Nothing in it blocks
  submission**, and nothing graduates to a GitHub issue until it has been
  reproduced on a device.
- [Integrations.TODO.optional.md](TODO_OPTIONAL/Integrations.TODO.optional.md)
  - [x] **Health Connect (fitness/sleep)** — shipped 02/08/2026 as a dashboard
    card; made **automatic** 05/08/2026. Read-only. Syncs on every app
    foreground, throttled to once per 15 minutes (stamp persisted per uid in
    SharedPreferences), and writes every unsynced reading with no review sheet —
    dedupe via `ProgressEntry.sourceKey` is what makes that safe, and today is
    **topped up by the difference** rather than skipped once seen. One follow-up:
    verify on a **physical phone with real step data** — the emulator's Health
    Connect store is empty, so the write path has never run against real
    readings. Re-checked 05/08/2026 on API 37: permission grant works, the card
    flips to "Synced just now", the throttle was proven both ways against the
    on-device stamp, and an empty store degrades to *"Health Connect is already
    up to date"* — so what remains unproven is specifically the reading →
    Firestore write against real data.
    (Samsung Health is not integrated directly; it is one of the apps that
    *writes into* Health Connect, which is all GoalPilot reads.)
  - [x] Google Tasks import — shipped 31/07/2026.
  - [x] **Competitive challenges** — shipped 05/08/2026: a live screen with
    standings, discover/join/leave, score reporting and a create flow, over the
    domain/data layers and participants security rule built 04/08/2026. **The
    last §6 nice-to-have.** Two follow-ups deliberately left open:
    - [x] **Deploy `firestore.rules` to live `goalpilot-56e30`** — done
      05/08/2026. Release `cloud.firestore` moved to ruleset `d38c7248…`, read
      back over the Rules API to confirm the `participants` block is live.
    - [x] **Verify a *non-owner* join end-to-end** — done 05/08/2026 with both
      emulators up: A created a challenge, B joined and scored 8200, A re-ranked
      to #2 live. No longer proven by `firestore-tests` alone.
  - [x] LLM task→goal classification UI — shipped as the "Smart add a task" card
    on the dashboard; `scoreTask` is wired to the ✨ button on the add-task row.
  - [x] **Life areas + time-allocation analytics** — shipped **and verified**;
    closed 04/08/2026 on your confirmation, with every follow-up done. What
    shipped: user-defined areas synced from Google Tasks list names, LLM duration
    estimates on tasks, and the interactive "Where your time goes" donut at
    day / week / month / quarter / year. See `CHANGELOG/2026-08-03/lifeareas.md`.
    The four follow-ups it deliberately left out, all landed 04/08/2026:
    - [x] **Reordering life areas** — drag handle plus accessibility actions,
      writing only the `sortOrder` values that actually moved.
      See `CHANGELOG/2026-08-04/lifearea-polish.md`.
    - [x] **Show the life area on the goals list** — the list is banded by area
      rather than carrying another chip per row.
      See `CHANGELOG/2026-08-04/lifearea-polish.md`.
    - [x] **Back-fill durations for old tasks** — a "Re-estimate N durations"
      action on the analytics card, capped at 15 per run like the Tasks import,
      review-before-write. Verified against the live model: it returned 105
      minutes for a five-word title, which neither the client heuristic (ceiling
      60 for five words) nor the Cloud Function's flat 30 can produce, and the
      card moved from "1 of 2 durations estimated by AI" to "all 2".
      See `CHANGELOG/2026-08-04/time-insights.md`.
    - [x] **A trend chart over time** — "How it moves", a stacked column per
      bucket, with buckets that tile the selected range exactly (days in a week,
      weeks in a month or quarter, months in a year, four-hour blocks in a day).
      See `CHANGELOG/2026-08-04/time-insights.md`.

- [Distribution.TODO.optional.md](TODO_OPTIONAL/Distribution.TODO.optional.md) —
  loose ends from putting the app on Firebase App Distribution (05/08/2026).
  None blocks submission; they are the cost of shipping outside Google Play.
  - [ ] **Show the app version in the UI** — it appears nowhere, and sideloaded
    that matters: after an in-app update, Settings → Apps is the only way to know
    what you are running. Kept out of `v0.2.2` so nothing muddied the
    update-prompt test.
  - [ ] **Give the debug build a distinct launcher name** — both variants use
    `@string/app_name`, so a release build installed beside a script-installed
    debug one yields two icons both reading "GoalPilot", and only one of them
    can ever prompt for updates.
  - [ ] **Decide whether `gradlew` carries its exec bit in git** — CI does
    `chmod +x` after checkout, because `gradlew*` is frozen in `AGENTS.md`. Needs
    a decision so nobody "fixes" it twice.
  - [ ] **Clean up the burned `v0.2.0` tag** — it published nothing and still
    sends a failure email. Deleting a remote tag is destructive; user's call.

### 🟢 FUTURE — [`TODO_FUTURE/`](TODO_FUTURE/)
- [ProductModel.TODO.future.md](TODO_FUTURE/ProductModel.TODO.future.md) — the
  **13 product-model decisions** from the same brief (`C1`–`C13`): the
  points/percentage knot, the goal↔task ontology, maintenance goals, AI planning
  and the calendar, the quote feed, presentation, and what a free model can be
  trusted to do. These are decision tickets for a `/wayfinder` map, not build
  items — none is worked before the map exists.
- **Bump the build toolchain, then take Health Connect `1.1.0` stable.** The app
  is pinned to `connect-client:1.1.0-beta01` purely because stable `1.1.0` (and
  every `1.1.0-rc*`) requires **compileSdk 36 + AGP 8.9.1+**, while this project
  is on compileSdk 35 / AGP 8.7.3 / Gradle 8.10.2. The three move together —
  AGP 8.9.1 also needs Gradle 8.11.1+. SDK Platform 36 is already installed on
  this machine, so the blocker is risk, not tooling: it is a whole-build change
  and was deliberately kept out of the Health Connect feature.
- Move points/level computation to a Firestore-triggered Cloud Function (anti-cheat).
- Cascade-delete a goal's tasks/progress (Cloud Function or batched client delete).
- **Migrate Google Sign-In → Credential Manager + Sign in with Google.** This is
  now the *only* remaining source of deprecation warnings in the build
  (9 warnings from `data/auth/GoogleAuthClient.kt`). Google has said the legacy
  `com.google.android.gms.auth.api.signin` package will be removed from
  play-services-auth "in a future release". **Its stated blocker is now gone** —
  a real Firebase project exists and legacy sign-in is verified working against
  it, so the migration can be attempted and regression-tested whenever you give
  the go-ahead. Still `future` tier: it replaces the entire sign-in flow, which
  is currently the one part of the app proven end-to-end.
- Replace `createdAt` client timestamps with `@ServerTimestamp`.
- Instrumented E2E tests against the Firebase Emulator Suite.

## ✍️ Conventions
- When you complete a task, **wait for user confirmation** before flipping `[ ]` → `[x]`.
- Closing a task requires a matching entry in today's `CHANGELOG/YYYY-MM-DD.md`.
- Adding a new task: place the file under the matching priority subfolder (`TODO_MUST/`, `TODO_OPTIONAL/`, `TODO_FUTURE/`); create a new `<Area>.TODO.<priority>.md` if no area fits, and link it from this index.
