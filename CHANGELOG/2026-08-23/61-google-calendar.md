# `61-google-calendar` — 2026-08-23

> **Summary:** `#61` builds §2.6–§2.8's Google Calendar client-side: the app now **creates a
> calendar Ido owns**, mirrors confirmed occurrences into it, and reads their times back on
> foreground. Before this, `grep` over `app/src/main` for `googleEventId` outside `#63`'s model
> returned nothing — four closed decisions (`#17`, `#27`, `#28`, `#33`) and no code. One narrow
> scope buys the whole feature: `calendar.app.created` creates, writes, reads back and colours, so
> the app's entire blast radius is the calendar it made itself.
>
> **Everything that decides anything is pure, and that is the shape worth reading.** `CalendarSync`
> takes an occurrence, a remote event and a clock and returns what to do; `GoogleCalendarClient`
> performs and decides nothing. So §2.7 — a rung that must not change on a pull, a title that is
> written and never read back, a deletion that neither re-creates nor patches — is **29 JVM tests**
> rather than a device, a Google account and a deleted event.
>
> ⚠️ **Two decisions the spec implies and does not spell out.** A pull must never change a **rung**:
> drag an all-day onto 09:00 in Google and `DAY_PASSED` — which §2.3 does not count as a failure —
> would silently become `MISSED`, which it does. And the query range must be **wider** than the range
> the sync is willing to judge, or an event dragged just past the edge comes back absent and gets
> read as a delete — the query's own boundary manufacturing §2.7's ambiguity on top of the real one.
>
> JVM **1010 tests, 2 failed** — and **both failures are a live sibling's uncommitted work**, not
> this ticket's. `CalendarSyncTest` is **29 / 0**. See §5.
>
> ⏸️ **The device pass is OWED and `#61` stays OPEN.** `60-calendar-surface` holds
> `emulator-5554`; nothing here touched a device, an AVD or `adb`, and **no sign-in was needed or
> destroyed**.

---

## 1. Where the code was, and what `#63` had already put in place

Nowhere, and more than expected respectively.

`#63` shipped `ScheduledOccurrence.googleEventId`, mapped it in both directions
(`Mappers.kt:385`, `:423`), covered blank-reads-as-absent in `ScheduleMappingTest`, and put
`linkGoogleEvent(occurrenceId, googleEventId: String?)` on `OccurrenceRepository` with a KDoc
explaining that **clearing is a first-class operation because §2.7 requires it**. So the storage half
of this ticket existed before it started — the brief's `owns:` list named `Mappers.kt` and there was
nothing left to do there, which is recorded on the board and in the brief.

What did not exist: any calendar client, any scope constant, any sync rule, and any way for the app
to know a Google event had gone.

## 2. What shipped

**`domain/model/GoogleCalendar.kt`** — the vocabulary, free of Android and of Google's wire format.
`CalendarScope` is §2.7's incremental-authorization table as three values, and it is an enum
precisely because that section requires the restraint to be *"visible in **which call is made**,
rather than as a filter after the fact"*. `CalendarCall<T>` is one generic result type where
`GoogleTasksClient` deliberately hand-wrote two concrete ones — its KDoc's reason (*"the two calls
are consumed by different screens"*) inverts at six calls all consumed by one sync path.
`ConsentRecovery` is the seam that keeps `android.content.Intent` out of the domain, which is the one
thing the Tasks client could not do and says so.

**`domain/usecase/CalendarSync.kt`** — every rule, pure:

| Rung | What Google holds |
|---|---|
| `ALL_DAY` | an all-day event on its day |
| `SPAN` | an all-day event across its days |
| `BLOCK` | a real timed event — the only rung that occupies a slot |
| `DEADLINE` | an **all-day banner**, `Due 23:59 · Submit report` |

`draftFor` returns `null` — *not a plan Google should hold* — for a skipped occurrence, a
`PROVISIONAL` block, and a blank title. It reads `placement.isEndorsed` rather than
`== CONFIRMED`, because §2.3 makes `SILENT` a **confirmed** block (*"already confirmed, because the
slot was visibly free"*) and treating it as unconfirmed would hide half the agent's plan from the
calendar.

`pushPlan` produces Insert / Update / Cancel, and three of its branches are decisions rather than
plumbing: a **cancelled** remote copy is left strictly alone (§2.7's *"never deletes and never
re-creates"*, both halves); a window that stops being a plan is cancelled **only if it has not
closed** (§2.8's tense split); and an unlinked occurrence whose window has already closed is **not
inserted**, which is the same split read the other way — history is preserved, never created.
Without that last one, granting the scope backfills the whole look-back window.

`pullPlan` produces retimes, disappearances and externals. `retimed` moves the *when* and **never**
the rung, per the table in its KDoc — and the `DEADLINE` row is the one that matters, because its
banner is all-day by construction, so Google holds no hour for it and a pull must not invent one.
Taking the remote start would make every synced deadline due at midnight.

**`domain/repository/CalendarRepository.kt`** — six methods, and deliberately no seventh. There is no
`readCalendar`, no `search`, and **no `deleteCalendar`**: §2.7 says sign-out does not delete the
calendar Ido owns, so removing it is his to do in Google's own UI and the interface should not offer
the verb.

**`data/calendar/GoogleCalendarClient.kt`** — REST, for `GoogleTasksClient`'s reason weighed a second
time. Every method mints its token for **one** scope, so `freeBusy` cannot run on a `calendar.readonly`
token it happened to have. `listEvents` sends `showDeleted=true` (how a disappearance is seen at all)
and `singleEvents=true` (so a series someone made by hand arrives as comparable instances).

**`domain/usecase/SyncCalendarUseCase.kt`** — I/O and ordering only. **Pull first, then push**, and
the order is the decision: pushing first re-creates events the user just deleted.

**The Keep / Cancel / Put back sheet** — `feature/dashboard/DashboardScreen.kt`, immediately under
the daily miss review, because §2.7 says *"the ambiguity is asked in the daily-review batch sheet …
at the one moment Ido is holding the phone."* The link is **already cleared** by the time a row
appears, which is what lets the card have three buttons and no fourth escape: ignoring it keeps every
occurrence on its original date, unmirrored.

**The foreground trigger** — `ui/root/RootViewModel.kt`, beside the health sync and in its own
coroutine. §2.7 is explicit that this is not one option among several: *"there is no credential for
a background sync and cannot be one."*

**Two preferences** — `calendarLastPullAt(uid)` and `goalPilotCalendarId(uid)`, both per-uid. The
first is named *pull* rather than *sync* on purpose: a stamp called `calendarLastSyncAt` reads as
though it gated both directions, and the first person to use it that way makes a user's edit wait a
quarter of an hour to reach their own calendar. The second is what stops a second calendar called
*GoalPilot* appearing in Ido's list.

## 3. The two things this ticket must never do, and how each is held

**Sign-out must not delete the calendar.** Held structurally rather than by a check: there is no
method on `CalendarRepository` that deletes a calendar. An account switch is held by the per-uid
preference key — a new uid has no calendar id and no `googleEventId` on any occurrence, so the first
sync sees an empty local set. `CalendarSyncTest`'s *"an account switch reads as not mirrored, never as
events to patch"* asserts that this produces **zero** writes and marks every existing event
`external`.

**An event Ido creates by hand is left alone.** `pullPlan` classifies it as `external` and the use
case ignores that list entirely — the code comment says so, because *doing nothing* is invisible
otherwise. §2.8: *"orphaned events are surfaced there and never auto-deleted — silent cleanup is the
one operation with no undo affordance."*

## 4. The two decisions the spec implies and does not state

**A pull may not change a rung.** §2.8 says a rung change is *"cancel-and-recreate, not a patch"* —
the app's own operation. Letting a drag in someone else's UI do it would change what a **miss means**
(§2.2) as a side effect of a sync. So `retimed` takes from the remote event only what the local rung
can express, and every row of that table is a test.

**The judged window is narrower than the query window.** `pullPlan` only calls an *absent* event a
disappearance inside `judgedFrom…judgedTo`, and `SyncCalendarUseCase` queries
`QUERY_MARGIN_DAYS` wider on both sides. This is not §2.7's real ambiguity — that one is a move to
**another calendar**, which the scope cannot see — it is a second ambiguity a single-range
implementation manufactures on top of it. The two constants are one decision, and
`CalendarSyncTest` asserts them together rather than trusting them apart.

## 5. 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest --rerun`) | **1010 completed, 2 failed** — both a sibling's, see below |
| ↳ `CalendarSyncTest` (new) | **29 / 0** |
| **Compile** (`:app:compileDebugKotlin :app:compileDebugUnitTestKotlin`) | `BUILD SUCCESSFUL`, 4 warnings (`GoogleSignIn` deprecation, the same class `GoogleTasksClient` already uses) |
| **Instrumented / UI** | **not run** — `60-calendar-surface` holds `emulator-5554` |
| **Security rules** (`firestore-tests/`) | **not run and none owed** — this ticket adds no collection and no rule; `#63` already covered `users/{uid}/occurrences` |
| **Device pass** | ⏸️ **OWED** — see §7 |

`--rerun` is deliberate. `63-occurrences-and-recurrence` recorded on 2026-08-23 that
`:app:testDebugUnitTest` returned `UP-TO-DATE` in 7 s over test classes that had never executed,
because a sibling had run the task over a tree holding **their** uncommitted work.

### The two failures are a live sibling's, and the attribution took one command

```
RecommendationRepositoryFallbackTest > falls back to local guidance when the function fails   FAILED
HebrewTerminologyTest > no Hebrew prefix is bonded to a format argument                        FAILED
```

Neither reads anything this ticket touched. `git status --short` over the files those two tests
consume returned **six modified files I never opened**:

```
 M app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt
 M app/src/main/res/values-iw/analytics_strings.xml
 M app/src/main/res/values-iw/components_strings.xml
 M app/src/main/res/values/analytics_strings.xml
 M app/src/main/res/values/components_strings.xml
 M app/src/test/java/com/idomarhaim/goalpilot/data/RecommendationRepositoryFallbackTest.kt
```

Five of the six are on `sessions/66-unmeasured-percent.md`'s `owns:` list, and the Hebrew failure
names `analytics_strings.xml`'s two new plural forms — `#66`'s work, mid-edit. **`git status` at
session start could not have told me this**: at 03:37 the tree held exactly one untracked directory,
and all six appeared in the twenty minutes that followed. That is `kb-candidates/` entry 2.

### ⚠️ And the owner has **no board row** — which is how it stayed invisible

`SESSIONS.md`'s Active claims held one row (`60-calendar-surface`) and none of the six paths was on
it. `66-unmeasured-percent` has `status: active` in its brief and never claimed. A one-line grep over
`sessions/*.md` finds it; nothing in the board rule tells anyone to run that grep. `kb-candidates/`
entry 3, destination `rules/`, and therefore always-ask.

## 6. What is NOT in this ticket, and why

- **The `freeBusy` consumer.** `CalendarRepository.freeBusy` exists and is correct, but §2.4's
  *"place it `SILENT` if the slot is visibly free"* needs §3.7's proposed plan (`#24`), which does
  not exist. The method ships because the **scope table** is the design and a client with two of
  three triggers unreachable would read as an oversight; nothing calls it yet, and that is said here
  rather than left to be discovered.
- **The settings surface for choosing which calendars to avoid** — §4.9's, `#46`'s.
- **The calendar *surface*** — `#60`, live in a sibling session as this shipped.

## 7. ⏸️ What is owed

**A device pass**, and it is the brief's own exit criterion: prove the calendar is created and an
event lands, with the shot worth keeping being **Google Calendar's own UI showing it** — which is
also what `#62` needs. It needs `emulator-5554`, held by `60-calendar-surface` for the duration of
its session, and AGENTS.md is explicit that two sessions driving one AVD corrupts its quickboot
snapshot. When it runs it takes the `adb install -r` + `am instrument` path and **never**
`connectedDebugAndroidTest`, which uninstalls the app and takes the sign-in with it.

It also needs **Ido signed in on the device and the calendar scope granted at least once** — §2.6's
consent screen is `In production` and unverified, so it shows *"Google hasn't verified this app"*
with **Advanced → Go to GoalPilot (unsafe)**, and scoped calls work through it.

So `#61` gets a comment naming what shipped and **stays open**, and this brief stays `active`.

**Untested:** every claim in §2 about what Google's API *returns* — the all-day `end.date` being
exclusive, `showDeleted=true` surfacing a trashed event as `status: "cancelled"`, and
`calendar.app.created` scoping `calendarList` to this app's own calendars. All three are read from
Google's published contract and are the reason the device pass is owed rather than optional. Every
claim about what **GoalPilot does with** those answers is `Observed:` — that is what the 29 tests
are.
