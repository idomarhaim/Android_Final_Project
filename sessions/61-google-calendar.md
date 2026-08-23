---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
issue: 61
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/data/calendar/**
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/GoogleCalendar.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/CalendarRepository.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/CalendarSync.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SyncCalendarUseCase.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/AppPreferencesRepository.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/prefs/AppPreferencesRepositoryImpl.kt
  - app/src/main/java/com/idomarhaim/goalpilot/di/RepositoryModule.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/root/RootViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt
  - app/src/test/java/com/idomarhaim/goalpilot/domain/CalendarSyncTest.kt
  - kb-candidates/2026-08-23-61-google-calendar.md
  - CHANGELOG/2026-08-23/61-google-calendar.md
  - sessions/61-google-calendar.md
# Corrected on kickoff, 2026-08-23. DROPPED `data/firestore/dto/Mappers.kt` -- `#63` already
# maps `googleEventId` both ways (Mappers.kt:385, :423) and `ScheduleMappingTest` covers it.
# ADDED the two preference paths (per-uid pull stamp + created calendar id), `RootViewModel.kt`
# (the foreground trigger, and NOT `GoalPilotRoot.kt`, which `60-calendar-surface` holds), the
# two dashboard files (the Keep/Cancel/Put back sheet joins the daily-review card that lives
# there), and `domain/model/GoogleCalendar.kt`.
created: 2026-08-23
---

# `#61` — a GoalPilot calendar of Ido's own, in Google Calendar

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Read first:** [`AGENTS.md`](../AGENTS.md) · then
[`docs/PRODUCT_v0.3.md` §2.6, §2.7 and §2.8](../docs/PRODUCT_v0.3.md) — **the design is
closed across four tickets and this brief does not restate all of it** · then
[`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61) · then
[`GoogleTasksClient.kt`](../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt),
which is the shape a Google client takes in this codebase.

**Task:** create the dedicated GoalPilot calendar client-side and sync occurrences to it.

## Where the code is today

**Nowhere.** `grep` over `app/src/main` for `googleEventId`, `calendar.app.created` or any
calendar client returns **zero hits**. The only Google integration that ships is **Tasks**,
read-only. Four decisions are closed —
[`#17`](https://github.com/idomarhaim/Android_Final_Project/issues/17),
[`#27`](https://github.com/idomarhaim/Android_Final_Project/issues/27),
[`#28`](https://github.com/idomarhaim/Android_Final_Project/issues/28),
[`#33`](https://github.com/idomarhaim/Android_Final_Project/issues/33) — and none is built.

## The five decisions that will save you a day each

1. **One narrow scope buys everything: `calendar.app.created`.** It creates the calendar,
   writes its events and colours it. **The calendar is Ido's, not the app's**, and it is
   created **client-side** — a service-account owner was considered and is *actively wrong*.
2. **Consent is not a blocker, whatever older files say.** `Observed:` on a device
   2026-08-09 — the screen is **`In production`**, so the seven-day grant expiry is gone;
   the first screen shows *"Google hasn't verified this app"* with **Advanced → Go to
   GoalPilot (unsafe)**, and scoped calls work through it. The Calendar API is **enabled** on
   `goalpilot-56e30`. ⚠️ **The claim this repo carried in three files until then — that an
   unverified app in production returns `Error 403: access_denied` with no override — is
   FALSE.** If you meet it again in a stale file, it is stale.
3. **There is no background sync and there cannot be one.** `GoogleAuthUtil` mints only
   short-lived tokens with no refresh token, and the service account is banned — so **the
   conflict fork does not exist**. Pull runs on foreground, the window is minutes wide for
   one user, and **last-write-wins is correct rather than a compromise**. Do not build a
   conflict resolver.
4. **Times sync in both directions; state syncs in neither.** A Google event has no field
   for `MISSED`/`OVERDUE`/`EXPIRED`/`PROVISIONAL`, and every encoding — a ✓ in a title, a
   colour meaning *late* — is something nothing else respects and the user can destroy by
   typing. **Titles are written but never read back**, because a Google-side rename would
   silently replace a task title with no undo.
5. **A disappearance never deletes and never re-creates.** A move-out is indistinguishable
   from a delete, so the occurrence **keeps its date and clears its `googleEventId`**, and
   the ambiguity is *asked* — **Keep / Cancel / Put back** — in the daily-review batch sheet,
   at the one moment Ido is holding the phone.

## What to build

1. **Create the calendar** on first use, client-side, and colour it.
2. **Push** confirmed occurrences only. **A `DEADLINE` is an all-day banner** titled
   `Due 23:59 · Submit report` — because the Google event **does not remind** (§2.5's local
   notification does), so its only job is to be *seen*, which a banner does and a 23:59
   marker does not. A timed event would occupy a slot the app cannot check and collapse
   `DEADLINE` into `BLOCK`. It **may be paired with a real `BLOCK`** — the obligation and the
   plan, as two events.
3. **Pull** on foreground, honouring the shipped 15-minute per-uid throttle. **Push is not
   throttled** — a write must never lag the user.
4. **The Keep / Cancel / Put back sheet** for disappearances.
5. **Incremental authorization**, because Google cannot enforce per-calendar sharing (scopes
   are per-scope, never per-calendar) — so it is *a promise GoalPilot keeps*, and the
   restraint must be visible in **which call is made**, not as a filter after the fact:

   | Trigger | Scope asked |
   |---|---|
   | sign-in | `calendar.app.created` + the calendar **list** |
   | ticking calendars to avoid | `calendar.events.freebusy` — Google-enforced, **no titles ever reach the app** |
   | first setting one calendar to **Full** | `calendar.readonly`, **with that calendar named in the sentence** |

6. **Degrade legibly on a partial grant, and gate nothing.** The `View your tasks` consent
   checkbox arrives **unchecked** ([`#36`](https://github.com/idomarhaim/Android_Final_Project/issues/36)),
   so sign-in can succeed having granted nothing. **Every calendar feature degrades and none
   blocks the app.**

## Two things that must not happen

- **Sign-out must not delete the calendar Ido owns.** An account switch reads as *not
  mirrored*, never as events to patch.
- **An event Ido creates by hand inside the GoalPilot calendar is left alone.**

## Exit

- **JVM tests** for the sync rules: `DEADLINE`→banner, titles-written-never-read,
  disappearance clears `googleEventId` and keeps the date, last-write-wins, throttle on pull
  and none on push. All of this is pure logic and needs no device or network.
- A **device pass** proving the calendar is created and an event lands — and the shot worth
  keeping is **Google Calendar's own UI showing it**, which is also what `#62` needs.
- ⚠️ `adb install -r` + `am instrument`, **never `connectedDebugAndroidTest`** — it uninstalls
  the app and takes the sign-in with it.
- 📱 **This session needs Ido signed in on the device**, and needs him to grant the calendar
  scope at least once. Say so in a `## 📱 SIGN IN NEEDED` heading **before** the first device
  command, not afterwards.
- `CHANGELOG/2026-08-23/61-google-calendar.md`.

## Carries over

- The design — [`docs/PRODUCT_v0.3.md` §2.6–§2.8](../docs/PRODUCT_v0.3.md).
- The measured consent-screen state, and the false claim it replaced — §2.6, same file.
- The client shape to copy — [`GoogleTasksClient.kt`](../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt).

## Out of scope

The calendar **surface** — [`#60`](https://github.com/idomarhaim/Android_Final_Project/issues/60).
Best sequenced after it so there is somewhere in-app to see the result, but the write path
does not strictly need it; if `#60` has not shipped, verify in Google Calendar's own UI and
say that is what you did.

---

## Progress — 2026-08-23

**Status: `active`, and the row on `SESSIONS.md` is still live.** The client-side write path shipped;
one exit criterion is owed.

**Done** — `CHANGELOG/2026-08-23/61-google-calendar.md` is the account.

- The calendar is created **client-side** and coloured; `data/calendar/GoogleCalendarClient.kt`.
- All four rungs push, with `DEADLINE` as an all-day banner; both directions of the time sync.
- The disappearance path clears the link, keeps the date, and asks **Keep / Cancel / Put back** in
  the daily-review card.
- Pull throttled at the shipped fifteen minutes, per uid; push not throttled.
- Incremental authorization: one scope per call, `freeBusy` shipped with **no consumer yet** because
  §2.4's caller is `#24`'s and does not exist — said in the changelog rather than left to be found.
- **JVM 1010 / 2 failed**, and both failures are `66-unmeasured-percent`'s uncommitted work.
  `CalendarSyncTest` is **29 / 0**.

**Device pass — HALF DONE 2026-08-23**, once both siblings released the AVD.

- **262 instrumented tests, 0 failures** (`adb install -r` + `am instrument`, never
  `connectedDebugAndroidTest`). The sign-in survived all of it.
- The foreground trigger does not crash, and the sync **degrades and gates nothing**: no
  `calendar_id_*` preference is written, the dashboard renders, every other feature works.
- **A defect was found by doing this**: §2.7's incremental-authorization table row one — the
  calendar scope at **sign-in** — was not implemented. Fixed in `GoogleAuthClient`. Every JVM test
  passed both before and after, because the gap is between the client and the sign-in.

**Owed — the calendar half, and it is Ido's tap.**

- The AVD is now **claimed by this session** and free.
- Ido **is** signed in (`name.iddo@gmail.com`), and his cached grant carries
  `email`, `openid`, `profile`, `tasks.readonly` and **no calendar scope** — it predates the
  `GoogleAuthClient` fix above. So he must **sign out and back in inside the app** and tick the
  calendar box.
- The consent screen is `In production` and unverified, so it shows *"Google hasn't verified this
  app"* with **Advanced → Go to GoalPilot (unsafe)**; scoped calls work through it.
- ⛔ **Driving those taps with `adb shell input` is not an option.** A consent screen exists to be
  read by a person; automating it defeats the only protection the user has.
- ⚠️ `adb install -r` both APKs + `am instrument`. **Never `connectedDebugAndroidTest`** — it
  uninstalls the app and takes the sign-in with it.
- The keeper shot is **Google Calendar's own UI showing the created calendar and one event**, which
  is also what `#62` needs.

**Untested and settled only by that pass:** every claim about what Google's API *returns* — the
all-day `end.date` being exclusive, `showDeleted=true` surfacing a trashed event as
`status: "cancelled"`, and `calendar.app.created` scoping `calendarList` to this app's own calendars.
All three come from Google's published contract, not from an observation on this machine.
