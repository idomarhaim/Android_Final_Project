# `c9f-consent-screen-state` — the claim that blocked the fix turned out to be untested, and false

> **Summary:** the claim that blocked the fix turned out to be untested, and false

**Session:** `c9f-consent-screen-state` · **Date:** 2026-08-09 (charted work begun 08-08)
**Invocation:** `/wayfinder 12 33` — resolve
**[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33)** (`C9f`), the
first `wayfinder:task` on the
**[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)** product-model map.
**Planning plus one manual task; no application code written or changed.**

| | |
|---|---|
| Ticket resolved | [#33 · `C9f`](https://github.com/idomarhaim/Android_Final_Project/issues/33) — closed |
| Ticket filed | [#36](https://github.com/idomarhaim/Android_Final_Project/issues/36) — Tasks consent checkbox unchecked by default (ordinary issue, not a map ticket) |
| Live project changed | Calendar API enabled · consent screen published to production |
| Asset | [`docs/research/2026-08-09-oauth-production-test/`](../../docs/research/2026-08-09-oauth-production-test/README.md) |

---

## The headline

**The publishing status was `Testing`, so `C9d`'s worst case was the live case: every
authorization this app holds was expiring seven days after consent.** It is now
`In production`, and that clock is gone.

Getting there meant disproving something this repo had been asserting as fact since
2026-07-31, in three files, one of them as a standing instruction to future sessions:

> Publishing status **must be Testing**. An unverified app *in production* returns
> `Error 403: access_denied` with **no override**.

**Nobody ever tested it.** The only `access_denied` anyone actually observed
(`CHANGELOG/2026-08-01.md:252-259`) was the *owner-is-not-automatically-a-test-user*
case — a **Testing**-mode failure that says nothing about production. It then spread by
copying: `docs/OPERATIONS.md`, `TODO/TODO_MUST/Submission.TODO.must.md`
(*"leave it there"*) and `TODO/TODO_OPTIONAL/Integrations.TODO.optional.md`.

**Run on a device on 09/08: it is false.**

| Claim | Verdict |
|---|---|
| `403 access_denied` in production | ❌ shows *"Google hasn't verified this app"* |
| …with **no override** | ❌ **Advanced → Go to GoalPilot (unsafe)**, on the first screen |
| Sensitive scope then blocked | ❌ a live import found **10 open tasks** — no `UserRecoverableAuthException`, no 403, nothing in logcat |

All three files corrected. Four screenshots committed, because a claim that survived nine
days and cost a session to disprove should not be re-litigated from prose.

## What the AFK half answered before anyone was asked for anything

- **The Google Calendar API was not enabled** on `goalpilot-56e30`. Verified against both
  `gcloud services list --enabled` and `--available`: `tasks.googleapis.com` present,
  `calendar-json.googleapis.com` absent. Enabled on Ido's approval. Missing it yields
  HTTP 403 `accessNotConfigured`, which does not read as a consent problem — a build
  session would have lost an afternoon.
- **The release OAuth client is registered**, as `C9d` §4 assumed —
  `app/google-services.json` carries three Android clients and one web client, the release
  package holding both the debug and release SHA-1 entries.
- **The publishing status was already answered in-repo**, five times over
  (`CHANGELOG/2026-07-31.md`, `docs/OPERATIONS.md`, `docs/SETUP.md`, both TODO files).
  The console reading confirmed it rather than discovered it.

## The scope category: answered by being immaterial, not left open

The ticket asked for the category of `calendar.app.created`. **The console does not show
it** — the *Add or remove scopes* dialog has API / Scope / User-facing description and no
Category column; a category only appears once a scope is **added** to the project, which
is a live mutation for a label.

Not worth taking, because the label cannot change anything. The app already ships
`tasks.readonly`, which Google itself labels sensitive — the account's Linked-apps page
lists *"View your tasks"* under *"This access might include sensitive info."* Calendar
cannot move the app into a regime it is not already in. `C9d` §2 reached the same
conclusion from the documentation side; this session confirmed the premise from the
account side, read-only.

## Reversibility, and why it took four fetches

The session recommended publishing, then caught itself: it had promised Ido the change was
revertible **without having checked**. That is the same sin as the claim it was busy
disproving, so the recommendation was withdrawn and the question researched.

Neither [Manage app audience](https://support.google.com/cloud/answer/15549945) nor
[Submitting your app for verification](https://support.google.com/cloud/answer/13461325)
mentions the return trip at all — read only those and publishing looks one-way. The
statement lives on a page about something else,
[Brand Approvals & Auto-Cancellations](https://support.google.com/cloud/answer/16868008):

> If you switched to Testing or Internal, when you switch back to In Production or
> External, public users will immediately be able to sign in and access the previously
> verified configuration.

Recorded in `docs/OPERATIONS.md` §2 with the quote and the source, filed under a heading
nobody would search.

## The step that would have faked a pass

**Revoking the grant is load-bearing, and the obvious procedure skips it.** The first
procedure this session handed Ido said "drive a fresh sign-in" — which would have proved
nothing: the OAuth grant lives on the **Google account**, not in the app, so signing out
in-app, clearing app data, or uninstalling all leave it intact and no consent screen ever
appears.

Caught before running. Then confirmed by accident: the emulator lost the app entirely to a
snapshot loss, and the grant was still listed on the account afterwards.

## The finding nobody was looking for

**The `View your tasks` consent checkbox arrives unchecked.** Tap Continue without ticking
it and sign-in **succeeds** while granting nothing; the import then fails as an ordinary
`NeedsConsent` → "grant permission" prompt rather than as *"you declined this"*. Live on a
shipped feature, and invisible for the same reason `C9f` itself was invisible — the
recovery path is good enough to hide the cause.

It also promotes a hypothetical to a fact: `C9d` §4 raised granular consent as a risk the
future calendar surface would design around
([#26 · `C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26)). It already
applies to Tasks. Filed as [#36](https://github.com/idomarhaim/Android_Final_Project/issues/36)
rather than fixed — build work on a shipped feature, not a decision on this map.

## A second stale claim, flagged not rewritten

`docs/OPERATIONS.md` §4 says Google appends the project number to unverified app names
(`GoalPilot-297750736036`). All four consent screens captured today read plain
**GoalPilot**. Left in place with a warning rather than deleted — one run is weak evidence
against a claim, and this session is in no position to replace an untested assertion with
another one.

## What it prices downstream

**[#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) loses one
of its two blockers** — `#33` is closed, but `#25` (`C9a`) is still open, so **`C9c` is
not on the frontier yet.** What it gains is that the constraint is **removed rather than
absorbed**: when it becomes takeable it may specify a calendar sync that stays true while
nobody is looking, which is what `R17` asked for and what `C9d` could not promise.

Worth carrying forward anyway: the **foreground-sync idiom this app already uses for
Health Connect** (sync on app foreground, per-uid throttle stamp) would have made the
seven-day clock harmless *without* publishing. Publishing removes the failure mode; that
pattern would have absorbed it. Both remain good design — and `#36` shows a declined scope
is a separate failure the calendar surface must handle regardless, since publishing status
does nothing for a checkbox left unticked.

**Cost of production, recorded so it is a decision and not a surprise:** first consent on
any account now costs one extra tap through the warning screen, and the 100-new-user
lifetime counter for unverified production apps is running. At an audience of two, both
are noise. Switching back to `Testing` for a cleaner demo recording is a 30-second job, at
the price of the clock returning; both directions are in `docs/OPERATIONS.md` §2.

## 🧪 Tests

**No suite was run, and none is applicable.** No Kotlin, Gradle, `firestore.rules` or
Cloud Functions file was created or modified — the repo diff is Markdown, four PNGs and
GitHub issues. `:app:installDebug` was run twice, but as a *delivery* step to get the
existing `HEAD` build onto the emulator, not as a change under test.

Verification was **behavioural and structural** instead:

- **Behavioural, on device.** Every consent screen captured as a screenshot and quoted
  verbatim rather than summarised. The scope was proven *working*, not merely *granted* —
  sign-in succeeding and a sensitive scope minting a token are two different claims, so
  the Tasks import was run and returned **10 open tasks**. `logcat` was cleared before the
  import and checked after for `UserRecoverableAuthException`, `access_denied` and `403`:
  none present.
- **Nothing was written to live Firestore.** The import was **cancelled at the review
  dialog**; the dashboard read `8 goals / 5 tasks done / 4 this week` before and after.
- **Structural.** The map was queried back out of GitHub after editing: 22 children, 4
  closed (`#17`, `#16`, `#15`, `#33`), and the frontier re-derived as `#32` and `#29`,
  with `#13` and `#14` correctly showing as assigned to the two live sibling sessions.
- **One error caught by that query and fixed:** the resolution comment on `#33` first
  claimed `C9c` was unblocked. `#27`'s `blockedBy` is `{33 CLOSED, 17 CLOSED, 25 OPEN}`,
  so it is not. The published comment was corrected in place rather than left standing.

## Singletons and siblings

- **`#emulator Pixel_10_Pro_XL` and `#gradle-daemon` taken and released.** Neither of the
  two live sibling sessions (`c7-what-is-a-unit`, `c4-goal-task-ontology`) holds a
  singleton, so there was no contention. The second AVD was never touched.
- **Live `goalpilot-56e30` was written**, both changes on Ido's explicit approval and both
  recorded: the Calendar API enabled, and the consent screen published to production. The
  before-state was written to `docs/OPERATIONS.md` **before** anything changed.
- **Ido's Google account was changed** — the GoalPilot grant revoked and re-granted. Both
  his clicks, both necessary, and his data was verified intact afterwards.
- **The emulator died once**, between a successful install and the first tap: no `qemu`
  process, empty `adb devices`. Rebooted rather than diagnosed; it held for the rest of the
  run. Recorded rather than omitted.
- **Gradle reported *"1 busy and 1 incompatible Daemons could not be reused"*** and started
  a third. No sibling claimed the daemon, so most likely a leftover from 08/08.
- **The map body `#12` is shared with both sibling sessions and carries no lease.** It was
  re-fetched and hashed immediately before writing and compared against the copy the edit
  was built from — no drift, no clobber.
- **`kb-candidates/` was listed before the first unit of work**, as required. Two files
  were already pending from sessions that have since released
  (`2026-08-08-c9d-calendar-scopes.md`, `2026-08-08-fix-task-completion-feedback.md`);
  both are now unowned and still owed an ingest. This session added its own rather than
  draining theirs.
