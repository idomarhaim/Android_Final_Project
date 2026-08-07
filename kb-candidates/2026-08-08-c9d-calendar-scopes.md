# KB candidates — `c9d-calendar-scopes`, 2026-08-08

Un-ingested. Normal mode, so this list is a **proposal**: nothing is ingested until
Ido approves it. Each entry stands alone — no transcript is a source.

Note: `kb-candidates/2026-08-08-product-model-map.md` was still un-drained when this
session listed the folder, and is reported rather than absorbed — it is not this
session's to drain. (`2026-08-06-product-review.md` and
`2026-08-06-product-device-pass.md` were drained by `kb-ingest` in `3d9ecfc`.)
The concurrent `c11a-free-model-probe`, `c15-language-switching` and
`fix-task-completion-feedback` sessions will each add their own file.

---

## 1. Graceful degradation makes a recurring failure unobservable — so "it still works" is not evidence the grant is durable

**Claim** — When a system handles a failure *well* — catching it, recovering, and
re-prompting the user in a way that is indistinguishable from normal first use — it
stops emitting evidence that the failure is happening. A recurring, policy-level
failure can then run for months inside a well-written recovery path and never be
filed as a defect, because at no point does anyone see something broken. The check
that catches it is not observation but **reading the policy that governs the
resource**: when a dependency has an expiry, quota, or lifecycle rule, look the rule
up rather than inferring it from the fact that the feature keeps working.

**Why** — The concrete case: a Google OAuth app whose consent screen is in
`Testing` publishing status has *every* authorization expire **seven days from the
time of consent** — the clock is on the grant, not on the access token, so a client
that mints per-call tokens is hit just as hard as one holding a refresh token.
GoalPilot mints per-call tokens with `GoogleAuthUtil.getToken`, catches the
resulting `UserRecoverableAuthException`, and surfaces Google's own consent Intent
as a `NeedsConsent` result the dashboard offers politely. That is *correct* error
handling and it is exactly why, if the project is in `Testing`, weekly re-consent
has been happening to the shipped Google Tasks import and looks identical to a first
run. Nobody would file it; there is nothing to see.

The generalisation matters more than the Google specifics: the better the recovery
path, the weaker the signal. This is the inverse of the usual intuition that robust
code makes problems visible. Rejected framing: *"the app has a bug in its Tasks
import"* — it does not; the code is right and the **project configuration** is
unread, which is a different class of defect and lives in a console, not a repo.

**Destination** — central KB, `kb/dev/`. It is an observability/diagnosis principle,
not Android- or Google-specific. Adjacent to any page on third-party integration
failure modes; also relevant to the existing GoalPilot note that a retired GROQ model
id *fails silently* and just serves local fallbacks — the same shape (a good fallback
hiding a real outage), which is worth cross-linking as a second instance rather than
being written up twice.

**Anchors** — [#33](https://github.com/idomarhaim/Android_Final_Project/issues/33),
[#17](https://github.com/idomarhaim/Android_Final_Project/issues/17),
`docs/research/2026-08-08-google-calendar-scopes-and-consent.md` §3,
`app/src/main/java/com/idomarhaim/goalpilot/data/tasks/GoogleTasksClient.kt:164-178`,
`CHANGELOG/2026-08-08/c9d-calendar-scopes.md`.

**Supersedes** — nothing. Cross-references the GROQ silent-failure note in
`AGENTS.md` ("The GROQ model id rots") as the same pattern seen elsewhere.

**Status** — pending Ido's approval.

---

## 2. `calendar.app.created` is the whole dedicated-calendar loop in one narrow scope — and its blind spot is the design constraint

**Claim** — An app that wants its own calendar inside a user's Google Calendar
should request exactly one scope,
`https://www.googleapis.com/auth/calendar.app.created`. It authorizes
`Calendars.insert`, `Events.insert`/`patch`/`delete`, **and** `CalendarList.patch`
(so the calendar can be coloured and visually distinguished) — the complete loop,
with no second scope needed. Three consequences worth knowing before designing:

- **It is blind to every other calendar.** Anything phrased as *"schedule around
  what they already have"* or *"don't double-book"* is not implementable under it
  and costs a second, broader scope — `calendar.events.freebusy` for availability
  only, or `calendar.readonly` for everything. Decide that trade deliberately.
- **Create the calendar client-side, on the user's own credential — never a service
  account.** Google: *"Don't use a service account for authentication… the service
  account is the data owner, which can lead to unexpected behavior."* Since the 2026
  lifecycle change, secondary calendars have a single **data owner**, only the data
  owner can delete them, and orphans are deleted outright (from **2026-04-27** for
  personal Google accounts, **2026-10-05** for Workspace). A service-account-owned
  calendar is an orphan by construction.
- **Google publishes no per-scope sensitivity table.** The non-sensitive / sensitive
  / restricted category is shown only in the Cloud Console when the scope is added
  (Google Auth Platform → Data access → Add or remove scopes). Do not expect the API
  docs to answer it.

**Why** — This is the sort of fact that is re-derived from scratch every time
someone integrates a calendar, because the twenty Calendar scopes are presented as a
flat table with no guidance on which combination fits which shape of app, and the
narrow-scope advice ("choose the most narrowly focused scope possible") is given
without saying which scope *is* the narrow one for the common "my app keeps its own
calendar" case. Getting it wrong costs either an over-broad consent line the user
cannot distinguish from a real need, or a two-scope request where one would do. The
service-account trap is worse than untidy — it is newly load-bearing because of the
2026 ownership change, and it is precisely the architecture a project with existing
Cloud Functions reaches for first. Rejected: writing this up as
`calendar.calendars` + `calendar.events.owned`, which also works and is what a
reading of the scope table alone suggests.

**Destination** — central KB, `kb/dev/`. A third-party API-capability fact that
generalises past GoalPilot and past Android.

**Caveat for whoever ingests it** — this is a capability claim about a third-party
product with dated lifecycle rules in it, so it carries its date and is the kind of
page that rots. Checked against Google's documentation **2026-08-08**.

**Anchors** — `docs/research/2026-08-08-google-calendar-scopes-and-consent.md`
(§1, §5, §8 — with every primary source in its §10),
[#17](https://github.com/idomarhaim/Android_Final_Project/issues/17),
[#27](https://github.com/idomarhaim/Android_Final_Project/issues/27).

**Supersedes** — nothing.

**Status** — pending Ido's approval.

---

## 3. A research ticket's best output is sometimes a *task* ticket — the fact that decides everything often lives in an account, not in documentation

**Claim** — When planning work against a third-party platform, expect a class of
question that reads like research but cannot be answered by research: the answer is
a **setting in a console you own**, not a fact in anyone's documentation. Route it
as a *task* — go and read it, and change it if needed — rather than letting a
research ticket close with the question quietly unanswered or, worse, filled in with
the documented default. The discriminator is simple: *if two projects using the same
API can have different answers, it is not a documentation question.*

**Why** — The concrete case: a research ticket asked which OAuth scopes a Google
Calendar integration needs and whether it could ride the consent the app already
had. The scope half was fully answerable from documentation. The consent half was
not — it depends on the project's *publishing status*, which is per-project, is
visible only in the Cloud Console, and swings the answer from "works indefinitely"
to "every authorization expires after seven days". Documentation states all three
possible states perfectly well and cannot tell you which one you are in. Closing the
research ticket with "yes, it can ride the existing consent" would have been true
and useless.

The same shape recurs: which APIs are enabled on the project, which OAuth clients
and signing fingerprints are registered, which quota tier the account sits in,
whether billing is attached. All of them are decision-grade, none are researchable,
and all of them are cheap once someone opens the console — which is exactly what
makes them easy to leave as an unexamined assumption instead.

**Destination** — central KB, `kb/dev/`. A planning/decomposition method, adjacent
to the two decomposition claims flagged by the `product-model-map` session on the
same day (split a constraint ticket on measurable-vs-downstream; order a "knot"
rather than merging it) — likely the same page or a sibling of it, since all three
are about getting ticket *types* and *edges* right on a wayfinder map.

**Anchors** — [#33](https://github.com/idomarhaim/Android_Final_Project/issues/33)
(the task ticket this produced), [#17](https://github.com/idomarhaim/Android_Final_Project/issues/17)
(the research ticket that produced it),
`docs/research/2026-08-08-google-calendar-scopes-and-consent.md` §2 and §9,
`kb-candidates/2026-08-08-product-model-map.md` candidates 1 and 2.

**Supersedes** — nothing.

**Status** — pending Ido's approval.
