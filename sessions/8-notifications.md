---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 8
created: 2026-08-20
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

## Six pieces, and none is optional

1. **A notification channel**, created at app start (`GoalPilotApp.kt`), named and described for the
   system settings screen — that text is user-facing, so it belongs in `res/`.
2. **The `POST_NOTIFICATIONS` runtime permission** (API 33+) — **including deciding *when* to ask.**
   Asking on first launch, before the user has done anything, is the pattern people deny. Decide it
   and write the reason down.
3. **A tap-through destination** — the newly-invented goal.
4. **The in-app half, which is separate** — a snackbar or an inline dashboard marker.
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
