# KB candidates — `c9f-consent-screen-state`, 2026-08-09

Written during `/wayfinder 12 33` (resolved
[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33), `C9f`).
**Not yet ingested.** Normal mode, so this list is a proposal — `/kb-ingest` drains it on
Ido's approval. Each entry stands alone; no transcript is required to write the page.

---

## 1. An untested claim written as fact propagates by copying, and ends up as an order

**Claim.** A single unhedged sentence in a docs file — asserting a *counterfactual* nobody
had observed — spread to three files in nine days, and in one of them became a standing
instruction to future sessions (*"leave it there"*). It blocked the correct fix for a real
problem until someone spent a session disproving it. **The tell is structural and
checkable: a claim of the form "if we did X, it would fail" that nobody could have
observed, because observing it requires doing X.** Prose gives no way to distinguish that
from a claim someone watched happen — so the hedge has to be written at the moment of
authorship, when the difference is still known.

**Why.** The concrete case: `docs/OPERATIONS.md` said *"an unverified app in production
returns `Error 403: access_denied` with no override."* What was actually observed on
2026-07-31 was a **Testing**-mode 403 (project owner is not automatically a test user) —
true, and about a different regime. The generalisation was plausible (it *is* true of
Google's **restricted** scopes) and wrong for **sensitive** ones. Rejected alternative:
"require a source for every claim" — too blunt, and the original claim was partly derived
from real experience. What actually distinguishes them is whether the author *ran* the
thing, which is cheap to state and impossible to reconstruct later.

**Destination.** `kb/dev/` — a page on documentation discipline / claim provenance. Likely
new; check for overlap with the existing `mechanism-vs-compliance.md`, which is about a
neighbouring failure (an observation that looked like proof of a mechanism).

**Anchors.** `CHANGELOG/2026-07-31.md:337-346` (origin) ·
`CHANGELOG/2026-08-01.md:252-259` (what was actually observed) ·
`docs/research/2026-08-09-oauth-production-test/README.md` §0 and §3 (disproof).

**Supersedes.** Nothing.

**Status.** Proposed. ⚠️ **Note the tension:** this could be argued as a `rules/` change
(how agents write documentation) rather than a KB page. `rules/` is always-ask in both
modes and owned by the walkthrough rule, so it is deliberately **not** proposed as one
here — that is Ido's call to make separately.

---

## 2. When vendor docs are silent on "can I undo this", search the exact sentence, not the concept

**Claim.** Google's documentation on OAuth publishing status covers Testing, production,
user caps and the seven-day expiry across two pages, and **neither mentions reverting**.
The statement that the round trip is possible lives on a third page about an unrelated
topic — Brand Approvals & Auto-Cancellations. **Silence on the obvious page is not
evidence of a one-way door**, and concept-search ("how to revert publishing status") does
not find it; searching a distinctive quoted phrase from the answer does.

**Why.** Cost four fetches and two searches, and in the meantime it changed a
recommendation: a change believed irreversible was recommended against, then recommended
once sourced. Rejected alternative: infer reversibility from the console UI showing a
"Back to testing" control — that is exactly the reasoning that produced candidate 1.

**Destination.** `kb/dev/` — research technique; possibly a section on an existing
research-method page rather than a page of its own.

**Anchors.** [Manage app audience](https://support.google.com/cloud/answer/15549945) ·
[Submitting your app for verification](https://support.google.com/cloud/answer/13461325) ·
[Brand Approvals & Auto-Cancellations](https://support.google.com/cloud/answer/16868008)
(the one that answers it).

**Supersedes.** Nothing.

**Status.** Proposed.

---

## 3. An OAuth grant lives on the account, not in the app — so uninstalling proves nothing

**Claim.** On Android, a Google OAuth grant is held against the **user's Google account**,
not in app storage. Signing out in-app, clearing app data, `pm clear`, and a full
uninstall/reinstall **all leave it intact**. Any test of a consent flow must revoke at the
account level (myaccount.google.com → Linked apps → *Delete all*) or it will silently
skip the consent screen and pass without testing anything.

**Why.** This nearly produced a false pass: the first written procedure said "drive a
fresh sign-in". It was caught by reasoning, then confirmed by accident — the emulator lost
the app entirely to a snapshot loss and the grant was still listed on the account
afterwards. The generalisation matters beyond Android: any test whose subject is a
*consent* rather than a *session* has to invalidate the consent, and the app is the wrong
place to look for it.

**Destination.** `kb/dev/` — a page on Google OAuth on Android, alongside the reference
facts in candidate 4.

**Anchors.** `data/tasks/GoogleTasksClient.kt:169-174` (`GoogleAuthUtil.getToken` →
`UserRecoverableAuthException` → `NeedsConsent`) ·
`docs/research/2026-08-09-oauth-production-test/README.md` §1 step 3.

**Supersedes.** Nothing.

**Status.** Proposed.

---

## 4. Google OAuth publishing status — the three states, measured

**Claim.** Reference facts, all confirmed against a real project on 2026-08-09 rather than
quoted:

| State | New users | Grant lifetime | First-consent experience |
|---|---|---|---|
| `Testing` | 100 listed test users | **expires 7 days from consent** | "app is being tested" warning |
| `In production`, unverified | 100 total, lifetime, never resets | indefinite | *"Google hasn't verified this app"* → **Advanced** → **Go to … (unsafe)** |
| `In production`, verified | unlimited | indefinite | nothing unusual |

**Production-unverified does not block sensitive scopes.** The override is on the first
screen, and the scope works through it. The seven-day clock is on the **grant**, not the
access token, so it presents as a re-consent prompt indistinguishable from first use —
tolerable for a user-pressed action, fatal for a background sync.

**Why.** The middle row is the one that is widely believed to be a hard block and is not;
believing otherwise blocked the right fix here for nine days. Verification is separately
waived for personal-use apps, so for a single-user app the whole verification track is
avoidable.

**Destination.** `kb/dev/` — Google OAuth reference, same page as candidate 3.

**Anchors.** `docs/research/2026-08-09-oauth-production-test/README.md` §2 (four
screenshots, verbatim wording) · `docs/OPERATIONS.md` §2.

**Supersedes.** ⚠️ **Yes — check before ingesting.** If any existing KB page carries the
"production hard-blocks sensitive scopes" claim inherited from this repo, this rewrites
it. Superseding a standing claim is always-ask in both modes.

**Status.** Proposed.

---

## 5. Granular consent arrives unchecked, so sign-in success is not scope success

**Claim.** On Google's granular consent screen, an individually-declinable scope checkbox
is **unchecked by default**. A user who taps Continue signs in successfully and grants
nothing. Any code that infers "we have the scope" from "sign-in returned an account" is
wrong; the grant must be checked explicitly (`GoogleSignIn.hasPermissions`) or the failure
surfaces later, disguised as a first-run prompt.

**Why.** Found live on a shipped feature that has behaved this way since the scope was
first requested, and it was invisible because the app's error handling turns a declined
scope into the same UI as a fresh install. Generalises past Google: **wherever consent is
per-item and optional, the default is off, and "the user completed the flow" is not
"the user granted the thing".** Requesting the scope at sign-in makes it worse — that is
the moment the user is least likely to read.

**Destination.** `kb/dev/` — same Google OAuth page, or a broader page on consent-flow
design.

**Anchors.** `docs/research/2026-08-09-oauth-production-test/3-scope-checkbox-unchecked.png` ·
[#36](https://github.com/idomarhaim/Android_Final_Project/issues/36) ·
`data/auth/GoogleAuthClient.kt:37` (scope requested at sign-in).

**Supersedes.** Nothing.

**Status.** Proposed.
