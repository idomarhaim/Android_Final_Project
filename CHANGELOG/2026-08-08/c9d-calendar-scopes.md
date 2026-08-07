# `c9d-calendar-scopes` — one scope buys the whole calendar, and the consent behind it has never been read

**Session:** `c9d-calendar-scopes` · **Invoked as:** `/wayfinder 12 17` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal ·
**Ran and landed** 2026-08-08.

One ticket resolved, as the map requires. **AFK** — Google's published documentation
and this repo's own code; no call to a Google API, no build, no device, no live
Firebase write. Planning only: nothing here ships code.

| | |
|---|---|
| Ticket resolved | [#17 · `C9d` Google Calendar: scopes, a dedicated calendar, and the consent the app already has](https://github.com/idomarhaim/Android_Final_Project/issues/17) — **closed** |
| Asset | [`docs/research/2026-08-08-google-calendar-scopes-and-consent.md`](../../docs/research/2026-08-08-google-calendar-scopes-and-consent.md) |
| Ticket filed | [#33 · `C9f` What state is GoalPilot's OAuth consent screen in?](https://github.com/idomarhaim/Android_Final_Project/issues/33) — `wayfinder:task`, blocking [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — first entry in *Decisions so far*; one new patch of fog |
| Also commented | [#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27), where the scope choice bends the decision |

## The answer

**`https://www.googleapis.com/auth/calendar.app.created`, alone.** It authorizes
`Calendars.insert` (make the dedicated calendar), `Events.insert` / `patch` /
`delete` (keep it current), and `CalendarList.patch` (colour it, so the events read
as GoalPilot's). Three calls, one consent line, and Google's own words on ownership:
*"the authenticated user for the request is made the data owner of the new
calendar"* — so the calendar is Ido's and outlives the app.

The two alternatives that also work — `calendar`, or `calendar.calendars` +
`calendar.events.owned` — buy nothing extra the design wants and reach data
GoalPilot has no intention of touching.

Verification, which the ticket expected to be the expensive part, is not a
constraint: the documented **personal-use exception** covers an app whose audience
the map has already fixed at one. *"If you are the only user of your app or if your
app is used by only a few users, all of whom are known personally to you."*

## The finding the ticket did not know it was asking for

The ticket asked whether Calendar could ride the consent the app already has.
Technically yes, and the mechanism is written — `GoogleTasksClient` already mints
per-call tokens and turns a missing grant into Google's own consent `Intent`. But
*whether that consent keeps working* turns on a Cloud Console setting **nobody has
read**, and one of its three states is disqualifying:

> Authorizations by a test user will expire seven days from the time of consent.

The clock is on the **grant**, not the token. So if `goalpilot-56e30` is in
`Testing`, Play Services stops minting after a week, `GoogleAuthUtil.getToken`
throws `UserRecoverableAuthException`, and the app — handling it *well* — quietly
offers the grant prompt again. **Which means it would have been happening weekly to
the existing Google Tasks import, looking exactly like normal first-use behaviour.
Good error handling is hiding the symptom, and nobody would ever have filed it.**

For an import that is an inconvenience. For `R17`'s calendar it is fatal to the
premise: a sync is supposed to stay true while nobody is looking, and one that dies
every seventh day is worse than none, because its stale events stay on screen
looking current.

Nothing to design there — a reading and possibly one console click — so it became
[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33), the map's
first `wayfinder:task`, rather than being resolved inside a research ticket.

## What it prices downstream

**[#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) is
the one that moves, and it moves against the obvious design.**
`calendar.app.created` can see **only** the calendar GoalPilot created — not Ido's
work calendar, not his personal one, not his free/busy. So *"schedule the task where
he is free"* and *"don't double-book him"* are **not implementable under the
recommended scope**; they cost a second, broader scope
(`calendar.events.freebusy` for availability only, or `calendar.readonly` for
everything). That is a real product trade, and `C9c` now makes it with the price
known instead of assumed. `C9e` is fully covered by the one scope; `C9b` gains a
skin-colour hook and a degrade-gracefully requirement; `C9a` is untouched.

## Two things that were nearly designed wrong

- **"Let the Cloud Function keep the calendar in sync" is off the table.** Google:
  *"Don't use a service account for authentication… the service account is the data
  owner, which can lead to unexpected behavior."* This repo has Functions and a
  service account, so that architecture was the obvious reach — and it is **newly**
  wrong rather than merely untidy: the 2026 secondary-calendar lifecycle change gave
  those calendars a single data owner and began deleting orphans (2026-04-27 for
  personal accounts). A service-account-owned calendar is an orphan with extra steps.
- **Ask for the scope at first calendar use, not at sign-in.** `tasks.readonly` sits
  on `GoogleSignInOptions`, so every sign-in consents to Google Tasks whether or not
  it is ever used. That was fine for a small read-only scope; Calendar is a write
  scope on a surface people guard, attached to an opt-in feature. Taken here per the
  narrow-scope principle rather than escalated, because the recovery path already
  exists — an account with no Calendar grant is just the `NeedsConsent` branch,
  which today handles the exceptional case and would handle the normal one.

## Quotas, and the second moving free tier

1,000,000 queries/day/project, 10,000/min/project, **600/min/user/project**; Calendar
itself caps at 100,000 events and 60 calendars created in a short period. At one user
with dozens of tasks none of it binds — except that 600/min/user is reachable **by a
defect, not a feature**, so the sync wants the throttle-stamp pattern the Health
Connect sync already uses, not a per-recomposition trigger.

Recorded and deliberately not designed around: Calendar API usage is free under the
daily threshold today, and *"full billing details will be shared later in 2026 with
at least 90 days' notice."* That makes Calendar the **second** free Google tier this
app leans on whose terms move, after GROQ's rolling model retirements. Against the
map's permanent-free constraint that is worth watching, not worth a redesign.

Firebase App Distribution changes nothing: verification and consent belong to the
Cloud project's OAuth client and consent screen, not to the channel the APK arrives
through. The only distribution-sensitive part — the Android OAuth client matching on
package name **plus signing SHA-1** — was already handled by `release-distribution`,
and is recorded because its failure mode is consent working in debug and failing only
in release.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, `firestore.rules`,
`functions/` or `scripts/` file was touched — the session wrote three Markdown files
and edited GitHub issues. The project's four layers (JVM unit, instrumented,
`firestore-tests/`, Functions) have nothing to assert about a research asset, and
running them would prove only that a sibling session's uncommitted Kotlin compiles.

Verification was **source discipline** instead, which is what a research ticket can
actually be held to: every external claim in the asset resolves to a primary Google
page listed in its §10, every claim about this app resolves to a file and line in
this repo, and the two questions documentation could not answer are named as
unanswered in §2 and §9 rather than filled in with a plausible guess. The map wiring
was verified by querying it back out of GitHub — #33 is a child of #12 and appears in
#27's `blockedBy`.

## Singletons and siblings

**None taken.** No `#gradle-daemon`, neither AVD, no live `goalpilot-56e30`, no GROQ
call — so no contention with `fix-task-completion-feedback` (device + Firebase),
`c11a-free-model-probe` (GROQ quota), or `c15-language-switching` (Ido's attention).
`SESSIONS.md` was taken and released under a `Lock-Path.ps1` lease.

**One shared artifact has no lease and now has three writers:** the map body,
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12), which every
wayfinder session appends to. This session re-read it immediately before editing and
confirmed *Decisions so far* was still empty; that is a check, not a guarantee, and
the next session should do the same.
