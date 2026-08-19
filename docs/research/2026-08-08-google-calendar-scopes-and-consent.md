# Google Calendar: scopes, a dedicated calendar, and the consent GoalPilot already has

Research asset for **[#17 · `C9d`](https://github.com/idomarhaim/Android_Final_Project/issues/17)**,
a `wayfinder:research` ticket on the
**[#12 · GoalPilot v0.3 product model](https://github.com/idomarhaim/Android_Final_Project/issues/12)** map.
Gathered **2026-08-08** by the `c9d-calendar-scopes` session. AFK — no device, no build,
no live Firebase write.

**Method.** Google's own documentation only, plus this repo's code for the "what the
app already has" half. Every external claim below carries its source in §10; every
claim about GoalPilot carries a file path. Where documentation does not answer a
question, this file says so rather than guessing — see §2 and §3, which are the two
places it happens and both matter.

---

## 0. Headline

**One scope covers the entire dedicated-calendar design, and it is the narrowest one
that does:** `https://www.googleapis.com/auth/calendar.app.created`.

It authorizes all three calls the design needs and nothing else:

| What the design needs | Call | `calendar.app.created` authorizes it? |
|---|---|---|
| Create the "GoalPilot" calendar | `Calendars.insert` | ✅ |
| Put a task's event in it | `Events.insert` (and `patch` / `delete`) | ✅ |
| Colour it so it reads as GoalPilot's | `CalendarList.patch` | ✅ |
| Read the user's *other* calendars | `Events.list` on `primary` | ❌ — **by design** |

That last row is not a gap to work around. It is the single most consequential fact
this ticket produced, and §8 hands it to `C9c` as a decision rather than an obstacle.

**The second finding is not about Calendar at all.** The ticket asked whether Calendar
can ride the consent the app already asks for. Technically yes, and the mechanism is
already written (§4). But *whether that consent keeps working* depends on a Cloud
Console setting nobody has read — and in one of its three possible states, **every
authorization this app holds expires seven days after it is granted**, which would make
a calendar sync die weekly and has been silently applying to the existing Google Tasks
import for as long as it has shipped. That is §3, and it is why this ticket files one
new ticket rather than none.

---

## 1. The scope list

Google Calendar API v3 publishes twenty OAuth scopes. All twenty, verbatim:

| Scope | Google's description |
|---|---|
| `.../auth/calendar` | See, edit, share, and permanently delete all the calendars you can access using Google Calendar. |
| `.../auth/calendar.readonly` | See and download any calendar you can access using your Calendar. |
| `.../auth/calendar.freebusy` | View your availability in your calendars. |
| `.../auth/calendar.events` | View and edit events on all your calendars. |
| `.../auth/calendar.events.readonly` | View events on all your calendars. |
| `.../auth/calendar.events.owned` | See, create, change, and delete events on Google calendars you own. |
| `.../auth/calendar.events.owned.readonly` | See the events on Google calendars you own. |
| `.../auth/calendar.events.freebusy` | See the availability on Google calendars you have access to. |
| `.../auth/calendar.events.public.readonly` | See the events on public calendars. |
| **`.../auth/calendar.app.created`** | **Make secondary Google calendars, and see, create, change, and delete events on them.** |
| `.../auth/calendar.calendars` | See and change the properties of Google calendars you have access to, and create secondary calendars. |
| `.../auth/calendar.calendars.readonly` | See the title, description, default time zone, and other properties of Google calendars you have access to. |
| `.../auth/calendar.calendarlist` | See, add, and remove Google calendars you're subscribed to. |
| `.../auth/calendar.calendarlist.readonly` | See the list of Google calendars you're subscribed to. |
| `.../auth/calendar.acls` | See and change the sharing permissions of Google calendars you own. |
| `.../auth/calendar.acls.readonly` | See the sharing permissions of Google calendars you own. |
| `.../auth/calendar.settings.readonly` | View your Calendar settings. |
| `.../auth/calendar.addons.execute` | Run as a Calendar add-on. |
| `.../auth/calendar.addons.current.event.read` | See the events you open in Google Calendar. |
| `.../auth/calendar.addons.current.event.write` | Edit the events you open in Google Calendar. |

### Recommendation: `calendar.app.created`, alone

Google's own guidance is *"choose the most narrowly focused scope possible"* and
*"users more readily grant access to limited, clearly described scopes."* Three
combinations can build a dedicated calendar. Only one of them is narrow:

| Option | Reaches | Verdict |
|---|---|---|
| **`calendar.app.created`** | **Only calendars this app created** | ✅ **Take this** |
| `calendar.calendars` + `calendar.events.owned` | Every calendar Ido owns, and every event on them | ❌ Two scopes, and a far wider blast radius for no gain |
| `calendar` | Everything, including deleting calendars and changing sharing | ❌ Never |

`calendar.app.created` is the scope Google added for exactly this shape of app: it
creates its own calendar and stays inside it. The consent line the user reads is a
single sentence that describes the feature accurately, which is worth something on its
own — the alternatives ask permission to touch data GoalPilot has no intention of
touching, and the user has no way to tell the difference.

---

## 2. Sensitivity and verification — what is documented, and what is not

**What is documented.** Google sorts scopes into *non-sensitive*, *sensitive* and
*restricted*. Sensitive and restricted scopes normally require app verification before
a public launch; non-sensitive scopes never do. Reading a user's Calendar events is
Google's own worked example of a sensitive scope.

**What is not documented, and this is the honest limit of this research.** Google
publishes **no per-scope sensitivity table**. Neither the Calendar auth page, the
master OAuth scopes list, nor the verification help centre states the category of any
individual Calendar scope. Google's stated mechanism is the console:

> When you add scopes to your project, scope categories (non-sensitive, sensitive, or
> restricted) are indicated automatically in the Google Cloud Console.

So the classification of `calendar.app.created` is **observable, but only in the
console** — Google Cloud Console → project `goalpilot-56e30` → **Google Auth Platform →
Data access → Add or remove scopes**, then read the *Category* column beside the scope.
This is a 30-second check, it needs Ido's console, and it is folded into the new ticket
in §9 rather than left as an unanswered line here.

**Why the answer barely changes the plan either way.** Whatever category comes back,
GoalPilot is covered by a documented exception, quoted in full:

> **Personal Use** — verification is not required *"if you are the only user of your
> app or if your app is used by only a few users, all of whom are known personally to
> you."*

That is precisely this map's fixed scope: *one real user — Ido, daily.* The app is also
**already** past whatever threshold Calendar would introduce: it ships
`tasks.readonly`, a Google user-data scope, today
([`GoogleAuthClient.kt:37`](../../app/src/main/java/com/idomarhaim/goalpilot/data/auth/GoogleAuthClient.kt#L37)).
Adding Calendar lengthens the scope list; it does not move the app into a regime it is
not already in.

**And for completeness, what verification would cost if it were ever pursued** — the
process is real work and this is why the exception matters:

- Verify domain ownership in Google Search Console.
- Publish branding verification first (minutes).
- Declare every scope, with *"a detailed justification for each requested sensitive
  scope, as well as an explanation for why a narrower scope isn't sufficient."*
- Upload an unlisted YouTube video showing the OAuth grant, the consent screen with the
  correct app name, the browser address bar showing the OAuth client ID, and the
  functionality each sensitive scope enables.
- *"The sensitive scope verification process can take up to 10 days to complete."*

For a one-user app, disproportionate — take the exception.

---

## 3. The consent regime is a *state*, and nobody has read it

This is the finding the ticket did not know it was asking for. The consent screen of
`goalpilot-56e30` is in one of three states, and they are not variations on a theme:

| State | New-user cap | Grant lifetime | What the user sees |
|---|---|---|---|
| **Testing** | 100 listed test users | **Authorizations expire 7 days from consent** | "Google hasn't verified an app that's undergoing testing" |
| **In production, unverified** | **100 new users total, over the project's lifetime, never resets** | Indefinite | The "Unverified apps" warning screen |
| **In production, verified** | None | Indefinite | Nothing unusual |

Verbatim, because the consequence is severe and easy to misread as a bug:

> Authorizations by a test user will expire seven days from the time of consent, and if
> your OAuth client requests an offline access type and receives a refresh token, that
> token will also expire.

**Why this bites GoalPilot specifically.** The app does not hold a refresh token — it
mints a short-lived access token per call through `GoogleAuthUtil.getToken`
([`GoogleTasksClient.kt:169`](../../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt#L169)).
But the seven-day clock is on the **grant**, not the token, so when it lapses Play
Services stops minting and throws `UserRecoverableAuthException` — which this app
catches and turns into a consent Intent
([`GoogleTasksClient.kt:171-174`](../../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt#L171-L174)).
The Tasks import handles it *gracefully*: the dashboard simply offers the grant prompt
again ([`DashboardScreen.kt:108`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt#L108)).

Which means: **if the project is in Testing, this has been happening every week and
looks exactly like normal first-use behaviour.** Good error handling is hiding the
symptom. Nobody would have filed it.

The consequence for the calendar is worse than for Tasks, and that asymmetry is the
point. A Tasks import is a thing Ido presses; a re-prompt is an inconvenience. A
calendar sync is a thing that is supposed to *keep being true* while he is not
looking — R17's "a dedicated GoalPilot calendar synced to Google" is a background
promise. A background promise that silently stops every seventh day is worse than no
promise, because the calendar keeps showing the stale events it wrote before it died.

**This is not a design decision, it is a fact to be read and possibly a setting to be
changed** — so §9 files it as a `wayfinder:task`, the one ticket type that does rather
than decides.

---

## 4. Can Calendar ride the consent the app already has? Yes — the mechanism is built

The pattern this app would need for Calendar is the pattern it already runs for Tasks.
Grounded in the code, not assumed:

| Piece | Where it is today | For Calendar |
|---|---|---|
| Scope constant | `GoogleTasksScopes.TASKS_READONLY` + its `oauth2:` twin ([`GoogleTasksClient.kt:24-30`](../../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt#L24-L30)) | An identical pair for `calendar.app.created` |
| Token minting | `GoogleAuthUtil.getToken(context, account, "oauth2:…")` | Same call, different scope string |
| Missing grant | `UserRecoverableAuthException` → `NeedsConsent(intent)` → screen launches Google's own consent UI | Same three-case result type |
| Transport | Raw `HttpURLConnection` + `Authorization: Bearer` against `tasks.googleapis.com`, deliberately avoiding the multi-megabyte `google-api-services-*` stack ([`GoogleTasksClient.kt:57-68`](../../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt#L57-L68)) | `www.googleapis.com/calendar/v3` is the same shape of REST — the existing precedent holds |

**What the user sees.** One extra consent screen, once: a granular checkbox reading
*"Make secondary Google calendars, and see, create, change, and delete events on
them."* Under Google's granular consent, it is a checkbox the user can decline
independently — so the calendar feature must degrade rather than break when it is
unticked. (That is a `C9b` surface question, flagged not decided.)

**One decision worth taking now, because it is derivable.** Request the Calendar scope
**at first calendar use**, *not* at sign-in. The Tasks scope sits on
`GoogleSignInOptions` ([`GoogleAuthClient.kt:37`](../../app/src/main/java/com/idomarhaim/goalpilot/data/auth/GoogleAuthClient.kt#L37)),
so every sign-in consents to Google Tasks whether or not that person will ever use the
import. That was a reasonable call for a small read-only scope. Calendar is a write
scope on a surface people are protective of, and it belongs to an opt-in feature.
Incremental authorization is what this is for, and the recovery path is already
built — an account with no Calendar grant is the *normal* case, handled by the exact
`NeedsConsent` branch that today handles the exceptional one.

**Two Cloud-project prerequisites, neither of them decisions:**

1. **Enable the Google Calendar API** on `goalpilot-56e30`. Not enabled by the Tasks
   API being enabled; a separate toggle. Symptom if missed: HTTP 403 with
   `accessNotConfigured`, which does *not* look like a consent problem.
2. **The Android OAuth client is keyed on package name + signing SHA-1.** The release
   SHA-1 was registered by the `release-distribution` session, so this is already
   satisfied — recorded because the failure mode is nasty: consent works in debug and
   fails only in the release build people actually install.

**A caveat that is not this ticket's to settle.** `GoogleSignIn` /
`GoogleSignInOptions` — the API this app uses — is **deprecated**; Google's current
guidance is Credential Manager for authentication plus `AuthorizationClient` for
authorization, and `AuthorizationClient` is the modern way to ask for an extra scope.
Nothing is broken today, and rewriting the sign-in stack is not a product-model
decision. But the calendar scope request is the first place the deprecation would be
felt, so it is added to the map's fog rather than left to surprise a build session.

---

## 5. The dedicated calendar itself

**Creation and ownership.** `Calendars.insert` makes a secondary calendar, and Google
is explicit about who ends up owning it:

> The authenticated user for the request is made the data owner of the new calendar.

So a calendar created from the app, on Ido's Google Sign-In, is Ido's — visible in his
Google Calendar beside his others, and his to keep if GoalPilot is ever uninstalled.
That is the right answer for R17.

**One hard prohibition, and GoalPilot is exactly the app that would trip it:**

> Don't use a service account for authentication. If you use a service account for
> authentication, the service account is the data owner, which can lead to unexpected
> behavior. For example, if a service account is the data owner, data ownership cannot
> be transferred.

GoalPilot has Cloud Functions and a service account already
(`functions/src/index.ts`), so "let the backend keep the calendar in sync" is the
obvious architecture to reach for. **It is wrong**, and it is newly wrong: as of the
2026 lifecycle changes, secondary calendars have a single **data owner**, only the data
owner can delete a calendar, and calendars left orphaned are deleted outright —
**from 2026-04-27 for personal Google accounts** (2026-10-05 for Workspace). A
service-account-owned calendar is an orphan with extra steps. The calendar must be
created **client-side, on the user's own credential.**

**Visual distinction — yes, and it costs no extra scope.** `CalendarList.patch` sets
`colorId` (Google's palette index), or `backgroundColor` / `foregroundColor` for an
arbitrary RGB when `colorRgbFormat=true` is passed. `calendar.app.created` authorizes
it. So the GoalPilot calendar can be coloured to match the selected `AppSkin` — a real
hook for `C9b`, and the cheapest possible answer to "can the user tell which events are
ours."

---

## 6. Quotas — not a constraint at this scale, but one number can be hit by a bug

| Limit | Value |
|---|---|
| Queries per day per project | 1,000,000 |
| Queries per minute per project | 10,000 |
| **Queries per minute per user per project** | **600** |
| Over quota | HTTP `403` or `429` `usageLimits` → exponential backoff with randomization |

Calendar's product-side limits, which are separate from the API's:

- No more than **100,000 events** created in a short period, or event creation is
  reduced *"for possibly several months."*
- No more than **60 calendars** created in a short period.

**Sizing this against GoalPilot.** One user, tasks in the dozens. A full resync is a
couple of requests. Nothing here binds — with one exception worth designing against:
**600/min/user is reachable by a defect, not by a feature.** A sync fired from a
recomposition, or a retry loop with no backoff, gets there in seconds. The app already
has the right pattern for this and it is worth reusing rather than reinventing: the
Health Connect sync runs on app foreground behind a per-uid SharedPreferences throttle
stamp. A calendar sync should be triggered the same way.

**Billing is coming, and it deserves a line rather than a redesign.** Usage under the
daily threshold incurs no charges today, and Google says *"full billing details will be
shared later in 2026 with at least 90 days' notice before any changes take effect."*
At one user and a handful of requests a day, this is noise. It is recorded because it
makes Calendar the **second** Google free tier this app depends on whose terms are a
moving target — GROQ's rolling model retirements being the first — and the map's
permanent-free constraint is easier to keep if both are watched rather than assumed.

---

## 7. Firebase App Distribution vs Play — changes nothing that matters

**Verification and consent are properties of the Cloud project's OAuth client and
consent screen, not of the distribution channel.** Shipping through Firebase App
Distribution neither triggers verification nor exempts the app from it; the state in §3
is the same state whether the APK arrives from App Distribution, Play, or `adb install`.
Having no Play listing likewise buys no exemption — the exception GoalPilot relies on is
the *personal use* one from §2, which is about how many people use the app, not how they
got it.

The one thing that *is* distribution-sensitive is already handled and is called out in
§4: the Android OAuth client matches on package name **+ signing SHA-1**, so the release
key's SHA-1 must be registered or Google Sign-In consent succeeds in debug builds and
fails in the release build that people actually install.

---

## 8. What this prices for the rest of the `C9` family

**[#27 · `C9c` sync direction and conflict resolution](https://github.com/idomarhaim/Android_Final_Project/issues/27) — the sharp one.**
`calendar.app.created` can see **only** the calendar GoalPilot made. It cannot read
Ido's work calendar, his personal calendar, or his free/busy. So any design phrased as
*"schedule the task where he is free"* or *"don't double-book"* is **not implementable
under the recommended scope**, and buying it means a second, broader, unambiguously
sensitive scope — `calendar.readonly`, or `calendar.events.freebusy` for the narrow
availability-only case. That is a genuine product trade and it is `C9c`'s to make, with
the price now known rather than guessed:

| What the sync may do | Scope needed | Cost |
|---|---|---|
| Write GoalPilot's plan into its own calendar | `calendar.app.created` | One narrow consent line |
| …and avoid clashing with his real commitments | `+ calendar.events.freebusy` or `calendar.readonly` | A second consent line, reaching data GoalPilot never writes |

**[#28 · `C9e` event lifecycle](https://github.com/idomarhaim/Android_Final_Project/issues/28)** — unblocked
by facts: `Events.insert`, `patch` and `delete` are all inside `calendar.app.created`, so
every lifecycle answer C9e might choose is purchasable at the same price. No scope
consequence to weigh.

**[#26 · `C9b` the in-app surface](https://github.com/idomarhaim/Android_Final_Project/issues/26)** —
gains two hooks: the calendar can be coloured to the active `AppSkin` (§5), and the
surface must degrade gracefully when the granular consent checkbox is declined (§4).

**[#25 · `C9a` what it means to schedule a task](https://github.com/idomarhaim/Android_Final_Project/issues/25)** —
nothing here blocks or bends it.

---

## 9. Left open, deliberately

Two facts live only in the Google Cloud Console and cannot be read from documentation
or from this repo. Both are folded into one new ticket rather than left dangling here:

1. **The category of `calendar.app.created`** — sensitive or non-sensitive (§2).
2. **The publishing status of `goalpilot-56e30`'s consent screen** — and therefore
   whether every grant this app holds is on a seven-day clock (§3).

→ **[#33 · `C9f` · What state is GoalPilot's OAuth consent screen in?](https://github.com/idomarhaim/Android_Final_Project/issues/33)**,
a `wayfinder:task`, blocking `C9c`.

---

## 10. Sources

All fetched 2026-08-08.

- [Choose Google Calendar API scopes](https://developers.google.com/workspace/calendar/api/auth) — the twenty scopes, and the narrow-scope guidance.
- [Calendars.insert](https://developers.google.com/workspace/calendar/api/v3/reference/calendars/insert) — authorizing scopes, data-owner rule, service-account prohibition.
- [Events.insert](https://developers.google.com/workspace/calendar/api/v3/reference/events/insert) — authorizing scopes.
- [CalendarList.patch](https://developers.google.com/workspace/calendar/api/v3/reference/calendarList/patch) — authorizing scopes, `colorId` / `backgroundColor` / `colorRgbFormat`.
- [Calendar API usage limits](https://developers.google.com/workspace/calendar/api/guides/quota) — 1M/day, 10k/min, 600/min/user, `usageLimits`, forthcoming billing.
- [Avoid Calendar use limits](https://knowledge.workspace.google.com/admin/calendar/avoid-calendar-use-limits) — 100,000 events, 60 calendars.
- [Sensitive scope verification](https://developers.google.com/identity/protocols/oauth2/production-readiness/sensitive-scope-verification) — process, 10-day timeline, and the personal-use exception quoted in §2.
- [Manage app audience](https://support.google.com/cloud/answer/15549945) — Testing vs production, 100-user caps, the seven-day authorization expiry.
- [OAuth App Verification Help Center](https://support.google.com/cloud/answer/13463073) — scope categories are indicated in the Cloud Console.
- [An update on secondary calendar lifecycle changes and a new API](https://workspaceupdates.googleblog.com/2026/03/an-update-on-secondary-calendar-lifecycle-changes-and-a-new-API.html) — single data owner, orphan deletion from 2026-04-27 (personal) / 2026-10-05 (Workspace).
- [Google Calendar API release notes](https://developers.google.com/workspace/calendar/release-notes) — 2025-10-27 data-ownership model; 2026-06-18 `Calendars.transferOwnership`.
- [About the migration from legacy Google Sign-In](https://developer.android.com/identity/sign-in/legacy-gsi-migration) and [Credential Manager replaces legacy APIs](https://android-developers.googleblog.com/2024/09/streamlining-android-authentication-credential-manager-replaces-legacy-apis.html) — `GoogleSignIn` deprecation, `AuthorizationClient` for scope requests.

In-repo, read for §4 and §5:
[`GoogleAuthClient.kt`](../../app/src/main/java/com/idomarhaim/goalpilot/data/auth/GoogleAuthClient.kt) ·
[`GoogleTasksClient.kt`](../../app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt) ·
[`DashboardScreen.kt`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt) ·
[`docs/RELEASING.md`](../RELEASING.md).
