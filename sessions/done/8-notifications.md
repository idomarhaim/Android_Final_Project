---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 8
created: 2026-08-20
result: |
  SHIPPED 99d3e31 -- substrate complete, all six pieces.
  HELD, and it is not a deferral I chose: S2.5's "one reminder per occurrence,
  timed per rung" and the daily miss review need an OCCURRENCE MODEL that does not
  exist (Task has no due date; the four rungs are in no Kotlin file; S2.1 is
  C9a #25, CLOSED, with no open issue carrying the build). The arithmetic those
  reminders need IS shipped and tested (domain/model/ReminderTiming.kt).
  #8 stays OPEN for that reason -- see the issue comment.
---

# `#8` — the notification substrate: channel, permission, and local scheduling

**The biggest single item in the backlog. Independent of `C20`** — it can start today, in parallel
with anything. Needs the **Gradle daemon** and a **device or the cloud emulator**; a real device is
better, because notification behaviour is where emulators diverge most.

> ✅ **Collides with nothing. Verified 2026-08-20.** New package, plus `AndroidManifest.xml` and
> `GoalPilotApp.kt`, which no other open brief touches. This is the one that can run whenever a
> build slot is free.

## ⚠️ Size it before you plan it — this is not "add a notification"

`AndroidManifest.xml` today declares `INTERNET`, `ACCESS_NETWORK_STATE` and two Health permissions.
**There is no `POST_NOTIFICATIONS`, no notification channel, no `WorkManager`, no `AlarmManager` and
no FCM** — verified 2026-08-20 by grep across `app/src/main`. §2.5 states the same fact and draws
the consequence: **this ticket carries the substrate for every notification in v0.3**, not only the
one it was filed for.

If it does not fit one session, **split it and say where** — substrate first, then the two
consumers. Do not silently deliver half.

## What the ticket was filed for

`R5`: raise a notification — in-app **and** as a system notification — when the smart sorter fails
to find a home for a task and **invents a new goal** (`R4`). That is §0.7's one branch that *speaks*
rather than acting silently, and #6 owns the sorter side.

> ✅ **`#6` HAS SHIPPED (2026-08-20), and it built the in-app half — so piece 4 below is DONE and
> this ticket is smaller than its brief says.** What exists now:
>
> · The sorter's branch table is `domain/model/SmartFiling.kt`, and each outcome carries
>   `FilingDecision.speaks` — the trigger you need is a **property on the decision**, not
>   something to re-derive. `speaks` is true for `NewGoal` and `NoGoal` and false for
>   `ExistingGoal`.
> · `DashboardViewModel.filed` emits a `SmartAddReceipt` after every filing, and
>   `DashboardScreen` turns it into a snackbar with **Undo**. That is the in-app half.
> · There is **no notification channel and no `POST_NOTIFICATIONS`** — `#6` deliberately grew
>   neither, per this brief's own split. The seam it left is `filed`: a system notification is a
>   second consumer of that flow, and needs no change to the sorter.
>
> ⚠️ **`R4`'s word *invents* is not what `#6` built, and the difference matters for what you
> notify about.** `#6` does not invent a goal — §3.5 forbids it. It creates an `AI_SUGGESTED`
> goal that sits **pending** with a lossless demotion (`FilingDecision.NewGoal`), and where it
> is not confident enough even for that, it files the task with **no goal at all**
> (`FilingDecision.NoGoal`). Two speaking outcomes, not one, and a notification that says *"a new
> goal was created"* is wrong about the second.

## Six pieces, and none is optional

1. **A notification channel**, created at app start (`GoalPilotApp.kt`), named and described for the
   system settings screen — that text is user-facing, so it belongs in `res/`.
2. **The `POST_NOTIFICATIONS` runtime permission** (API 33+) — **including deciding *when* to ask.**
   Asking on first launch, before the user has done anything, is the pattern people deny. Decide it
   and write the reason down.
3. **A tap-through destination** — the newly-invented goal.
4. ~~**The in-app half, which is separate** — a snackbar or an inline dashboard marker.~~
   ✅ **Done by `#6`** — `DashboardViewModel.filed` → a snackbar with Undo. Read it before
   adding anything; a second in-app surface for the same event is one too many.
5. **Graceful behaviour when permission is refused** — the in-app half must still work. This is the
   piece most likely to be skipped, and the one a grader will try.
6. **Local scheduling** — `WorkManager` or `AlarmManager`. §2.7 establishes there is **no credential
   for a background sync and cannot be one**, so nothing server-pushed is available. Choose, and say
   why.

## What the scheduled half must do — §2.5

- **One reminder per occurrence, timed per rung** (`ALL_DAY · DEADLINE · BLOCK · SPAN`, §2.2).
- **The deadline's reminder is computed backwards from `minutesOf(task)`**, clamped to waking hours,
  and **says why it moved**: *"due at 06:00 and it takes about 4 hours — worth starting tonight"*.
  §2.5 calls this **the one thing this app knows that Google Calendar does not**. It is the feature,
  not a detail.
- **A reminder re-checks at fire time** whether it is still needed — free, precisely because nothing
  is stored (§2.3: temporal state is derived, never stored).
- **Misses meet Ido once, in a daily review on app open — never as a push saying he failed.**

## The trap

**A notification you cannot see is a notification you have not built.** Every layer here is
asynchronous and time-dependent, so the tests will pass while the thing never fires on a real
phone. Budget for an actual observed fire — schedule one seconds out, watch it appear, screenshot
it. `adb shell dumpsys notification --noredact` is how you check it without waiting.

## Exit

- JVM unit for the backwards-from-duration reminder arithmetic, including the clamp.
- Instrumented for the permission-refused path.
- **An observed notification**, evidenced — a screenshot or `dumpsys` output in the changelog, not
  "it should fire".
- Any layer that does not apply, said explicitly.
- `CHANGELOG/<today>/8-notifications.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. If you split this, say so on the line and name the follow-up slug —
a half-delivered substrate reported as done is the failure this brief opens by warning about.
