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
> ✅ **The device pass is DONE and `#61` is CLOSED.** Ido granted the scope; the calendar was created
> in his account and three `DEADLINE` banners landed in it, photographed in **Google Calendar's own
> UI** — and still three after a second sync, which is what proves the push idempotent. His sign-in
> survived every install and all 262 instrumented tests.

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
| **JVM unit** (`:app:testDebugUnitTest --rerun`) | **1015 completed, 0 failed, 0 errors** across 86 classes, re-run after `#66` committed. The first run at 00:40 read `1010 / 2 failed` and **both were that sibling's uncommitted work** — see below |
| ↳ `CalendarSyncTest` (new) | **29 / 0** |
| **Compile** (`:app:compileDebugKotlin :app:compileDebugUnitTestKotlin`) | `BUILD SUCCESSFUL`, 4 warnings (`GoogleSignIn` deprecation, the same class `GoogleTasksClient` already uses) |
| **Instrumented / UI** (`adb install -r` + `am instrument`) | **OK (262 tests)**, 0 failures, 852 s — run 2026-08-23 11:22 once both siblings released the AVD |
| **Security rules** (`firestore-tests/`) | **not run and none owed** — this ticket adds no collection and no rule; `#63` already covered `users/{uid}/occurrences` |
| **Device pass** | ✅ **DONE** — the calendar exists in Ido's account and three events landed in it, shown in **Google Calendar's own UI**. `docs/render-passes/2026-08-23-61-google-calendar/`. See §5e |

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

## 5b. 📥 KB drained — and both candidates named the wrong destination

Cross-repo into `C:\Dev\JARVIS`, commit `3365980`. Two of three ingested, **both as sections on
pages that already existed**:

- 📥 `kb/dev/indistinguishable-at-the-boundary.md` **§5c** — *the ambiguity you manufacture*. That
  page was written on **2026-08-10** from the *design* of this very sync; §5c comes from building
  it, and adds a third row to the page's own §1 table: *dragged to a date outside the range you
  asked about* is indistinguishable from a delete **from inside the code**, and unlike the other two
  rows it is a bug.
- 📥 `kb/dev/look-at-your-own-output.md` **§4p** — *the false red*, beside §4c-ii's false green,
  which `63-occurrences-and-recurrence` added from this same repo eight hours earlier.

⚠️ **The finding is worth more than either section.** Entry 1 proposed a **new** page and stated in
its own Destination field that nothing in the bundle covered it. One `grep` found the identical
worked case — same repo, same feature, same calendar — already on a page from three months before.
That is `/kb-ingest` §3 observed exactly as written, and the **bundle-check field was missing from
both entries** rather than `not checked`, which is the weaker of the two signals that section
distinguishes.

In the other direction: entry 2's `Status` said *held* on a reading of the **JARVIS** board that was
~40 minutes stale — that row had released, and the deferral would have been for nothing. A
candidate's picture of the bundle is a snapshot from the moment it was flagged.

⏸️ **Entry 3 stays held** — *a brief with `status: active` is a liveness surface the claim board does
not have*, destination `rules/`, therefore always-ask in both modes and owing a 🎬 walkthrough offer
besides. Its **diagnostic** half is §4p above and stands on its own; the **procedural** half — telling
every session to run `grep '^status: active' sessions/*.md` — is Ido's call.
`kb-candidates/2026-08-23-61-google-calendar.md` is rewritten down to that one survivor, not deleted.

## 5c. The defect the device found, and it was mine

**§2.7's incremental-authorization table has three rows. Row one was not implemented.**

```
| Trigger | Scope asked |
|---|---|
| sign-in | `calendar.app.created` + the calendar list |   <- NOT BUILT
| ticking calendars to avoid | `calendar.events.freebusy` |
| first setting one calendar to Full | `calendar.readonly` |
```

`GoogleAuthClient`'s `GoogleSignInOptions` requested `tasks.readonly` and nothing else, so a new
sign-in never offered the calendar checkbox and **every** calendar call fell through to the
`UserRecoverableAuthException` recovery path — an interstitial the first time someone opens a
calendar surface, instead of a box in the sheet where they are already granting things.

**How it was found, and it is not how it should have been found.** By reading the emulator's cached
sign-in on the way to the device pass:

```
"scopes":["email","https://www.googleapis.com/auth/tasks.readonly","openid","profile"]
```

Four scopes, no calendar. **Every JVM test still passed**, and they were right to — the sync rules
are correct and the recovery path genuinely works, so nothing in `CalendarSyncTest` could see it.
The gap is between the client and the **sign-in**, which is one file neither the tests nor the sync
touches.

**The self-review question it should have failed.** `rules/pre-commit-self-review.md` asks *"which
of my own arguments does my output contradict?"* — and `CalendarScope`'s KDoc **contains that
table**, quoted from §2.7, immediately above an enum whose first member the sign-in never asked
for. A table in a KDoc is an argument; skipping its first row is a contradiction of it. Answering
that question honestly against the file I had just written would have caught this before the
commit, with no device involved.

**The fix, and what it deliberately does not do.** One `requestScopes` line for
`CalendarScope.APP_CREATED` and **only** that one. Rows two and three stay where they are, asked at
their own trigger, because §2.7 requires the restraint to be *"visible in **which call is made**,
rather than as a filter after the fact"* — adding all three to the builder would be exactly the
omnibus request that sentence rejects, and it would ask for `calendar.readonly` from a user who may
never use the feature that needs it.

**It changes nothing for accounts that already exist.** A cached grant is not re-negotiated by
editing the options; Ido's stays as it is and `GoogleCalendarClient` keeps surfacing the recovery
intent for it — the same fallback the Tasks scope above it has relied on since `#36`.

## 5d. What the device pass proved, and the one thing it could not

Run on `emulator-5554` (`Pixel_10_Pro_XL`), 2026-08-23, after `60-calendar-surface` (`5d5e2a3`) and
`66-unmeasured-percent` (`72fb296`) released both singletons.

**✅ Proved without the calendar grant:**

- **262 instrumented tests, 0 failures** — including `DailyMissReviewUiTest`, which drives the real
  dashboard card the new Keep / Cancel / Put back sheet sits beside, and which therefore exercises
  `DashboardViewModel`'s widened constructor through **real Hilt injection on a device**. That is
  the layer a JVM test cannot reach: a missing binding for `SyncCalendarUseCase` is a runtime
  failure, not a compile one.
- **The foreground trigger does not crash.** `RootViewModel.onAppForegrounded` now launches a
  second coroutine into a network round-trip that immediately fails for want of a scope. App
  launched, `pidof` alive, **zero `FATAL EXCEPTION` in logcat**.
- **It degrades exactly as §2.6 requires and gates nothing.** `goalpilot_ui_prefs.xml` holds **no**
  `calendar_id_*` key after a full launch — `ensureCalendar()` returned `NeedsConsent` and the sync
  stopped having written nothing, while the dashboard rendered normally and every other feature
  worked. *"Every calendar feature degrades legibly and none blocks the app"*, observed rather than
  asserted.
- **The sheet is correctly absent.** No disappearances exist, so no card — an empty review is not
  an empty card, the same rule the miss review already follows.
- **The sign-in survived everything.** `install -r` for both APKs, then 262 instrumented tests, and
  `FIREBASE_USER` is still in the Firebase store afterwards; the dashboard greets *עידו*.
  `connectedDebugAndroidTest` was **not** used, which is the whole reason that is true
  (`kb/dev/android-device-verification.md` §8).

**⏸️ Could not be proved, and it is one thing:** that a calendar is actually created in Google and
an event lands in it. The emulator's account grant predates §5c's fix and carries no calendar
scope, so the app correctly refuses. Granting it is a consent screen requiring a human — including
Google's *Advanced → Go to GoalPilot (unsafe)*, since the app is `In production` and unverified —
and **driving those taps programmatically is not an option that was considered**: a consent screen
exists to be read by a person, and automating it would defeat the only protection the user has.

So §7's `Untested:` list is unchanged and still needs one sign-out/sign-in with the box ticked.

## 5e. ✅ The calendar half — proven in Google's own UI

Ido granted `calendar.app.created` at the app's own consent screen. Everything below is a
**device observation**, and the shots are committed at
`docs/render-passes/2026-08-23-61-google-calendar/`.

**The calendar was created, client-side, in his account.**

```
calendar_id_cTmjUK6FDGf…  =  7cec978e…@group.calendar.google.com
```

A `@group.calendar.google.com` id is Google's form for a **secondary** calendar — which is the whole
of §2.6's *"the calendar is Ido's, not the app's … a service-account owner is actively wrong"*, and
the reason his own note that *"in Google Calendar you can create several calendars for one
account"* is exactly the mechanism this uses.

**Three occurrences went in, three banners came out.** GoalPilot's own surface (`#60`) held three
`DEADLINE`s on Mon 24 Aug, *due 20:00*. Google Calendar shows three all-day banners on Mon 24 titled

```
Due 20:00 · Write the project book chapter
```

and opening one says **`Tomorrow`** — all-day, no slot — on calendar **GoalPilot**, account
`name.iddo@gmail.com`. That is §2.7's deadline rule end to end: *"its only job is to be **seen**,
which a banner does and a 23:59 marker does not."* Had this been built as a timed event it would sit
at 20:00 in a slot the app cannot check, which §2.7 says collapses `DEADLINE` into `BLOCK`.

### The check worth more than the screenshot: a **second** sync

A picture of three events proves an insert happened. It does **not** prove the insert will not
happen again — and if `link()` had failed to store `googleEventId`, every future foreground would
add three more, forever, silently, and the first screenshot would look identical.

So the app was force-stopped and relaunched, and the shot was retaken:

| After sync 1 | After sync 2 |
|---|---|
| 3 banners | **3 banners** |

Still three. The stored pull stamp was **unchanged** across the second run — §2.7's fifteen-minute
throttle doing its job — so `remote` was empty and the push ran under `UnknownRemote.ASSUME_STALE`,
which **patches a linked occurrence and inserts an unlinked one**. Three rather than six is
therefore a direct observation that the link was stored on the first run, taken at the one moment
the two designs are distinguishable.

`Observed:` this is the hardest input the instrument has, in `look-at-your-own-output.md`'s sense:
the failing case and the passing case produce **identical** first screenshots.

### And what the same frame proves about restraint

Ido's own Hebrew events surround the banners — his other calendars, rendered by Google Calendar.
GoalPilot cannot see any of them: `calendar.app.created` reaches only calendars this app created, so
the blindness is Google-enforced rather than a filter the app applies. That is §2.7's promise
holding at the only layer where a promise is worth anything.

### The one honest gap left, and it is not this ticket's

Ido asked for the app to be able to **view** his other calendars. It currently cannot — §2.7 asks
for that at its own trigger (busy/free when he ticks calendars to avoid, full read only if he sets
one to *Full*, with that calendar named). Neither trigger has a consumer yet: the first is §2.4's
agent placement, which is `#24`'s. Recorded here because he asked for it in so many words, and
because *"we did not build it"* and *"we deliberately deferred it"* look identical from outside.

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
