# An unverified app in production, tested rather than assumed

Evidence asset for **[#33 · `C9f`](https://github.com/idomarhaim/Android_Final_Project/issues/33)**,
a `wayfinder:task` on the
**[#12 · GoalPilot v0.3 product model](https://github.com/idomarhaim/Android_Final_Project/issues/12)** map.
Run **2026-08-09** by the `c9f-consent-screen-state` session, on emulator
`Pixel_10_Pro_XL` against live `goalpilot-56e30` and the real `name.iddo@gmail.com`
account.

---

## 0. What was being tested, and why it needed testing

Since 2026-07-31 this repo has asserted, in three files, that:

> Publishing status **must be Testing**. An unverified app *in production* returns
> `Error 403: access_denied` with **no override**.

**It was never tested.** The only `access_denied` anyone actually observed
([`CHANGELOG/2026-08-01.md:252-259`](../../../CHANGELOG/2026-08-01.md#L252-L259)) was
the *owner-is-not-automatically-a-test-user* case — a **Testing**-mode failure, which
says nothing about production. From there the claim spread to
[`docs/OPERATIONS.md`](../../OPERATIONS.md),
[`TODO/TODO_MUST/Submission.TODO.must.md`](../../../TODO/TODO_MUST/Submission.TODO.must.md)
(as a standing instruction — *"leave it there"*) and
[`TODO/TODO_OPTIONAL/Integrations.TODO.optional.md`](../../../TODO/TODO_OPTIONAL/Integrations.TODO.optional.md).

It mattered because `C9d` had established that in `Testing`, *"authorizations by a
test user will expire seven days from the time of consent"* — a clock that is
survivable for a button-triggered Tasks import and fatal for a background calendar
sync. The obvious remedy was to publish to production. The repo said that remedy was
impossible.

**Result: the claim is false.** Production works, with a warning screen that has an
override.

---

## 1. Method

Publishing status and the grant revocation are console-side and were done by Ido; every
device step below was driven by the agent over `adb` and captured as a screenshot.

1. **Before-state read from the console** — `Testing` / `External`, recorded in
   [`docs/OPERATIONS.md`](../../OPERATIONS.md) §2 before anything changed.
2. **Published** to production (Google Auth Platform → Audience → *Publish app*).
3. **Revoked the existing grant** — myaccount.google.com → Linked apps → GoalPilot →
   *Delete all*. **This step is load-bearing and easy to get wrong:** the grant lives on
   the Google account, not in the app, so signing out in-app, clearing app data or even
   uninstalling all leave it intact and the test would pass without ever raising a
   consent screen. Proven incidentally here — the app was absent from the emulator
   after a snapshot loss, and the grant was still listed.
4. **Fresh sign-in**, screenshotting every screen.
5. **Ran the Tasks import**, because sign-in succeeding and a *sensitive scope* working
   are two different claims.
6. **Cancelled at the review dialog** — nothing written to live Firestore.

---

## 2. What Google actually showed

### Screen 1 — the warning ([`1-unverified-warning.png`](1-unverified-warning.png))

> ⚠️ **Google hasn't verified this app**
>
> The app is requesting access to sensitive info in your Google Account. Until the
> developer (name.iddo@gmail.com) verifies this app with Google, you shouldn't use it.
>
> If you're the developer, submit a verification request to remove this screen.
>
> **Advanced**  ·  **BACK TO SAFETY**

Two things at once. The wording is about **verification**, not testing, which confirms
the publish took effect — so this is the production screen, not a Testing screen
mistaken for one. And **`Advanced` is present**, which alone falsifies *"no override"*.

### Screen 2 — the override ([`2-advanced-override.png`](2-advanced-override.png))

> Continue only if you understand the risks and trust the developer
> (name.iddo@gmail.com).
>
> **Go to GoalPilot (unsafe)**

Present, tappable. No `403`.

### Screen 3 — the scope, and an unrelated finding ([`3-scope-checkbox-unchecked.png`](3-scope-checkbox-unchecked.png))

> **GoalPilot wants access to your Google Account**
>
> ⚠️ This app hasn't been verified by Google. If you're the app developer, you need to
> submit a request to have this app verified by Google. Otherwise, some of this app's
> access to Google user data may be lost.
>
> **Select what GoalPilot can access**
> ☐ View your tasks. *See access details*

**The checkbox arrives unchecked.** See §4 — this is a live defect in the shipped Tasks
import and has nothing to do with publishing status.

### Screen 4 — the proof ([`4-import-succeeded.png`](4-import-succeeded.png))

> **Import from Google Tasks** — Found 10 open task(s).

Real Hebrew tasks, listed. `GoogleAuthUtil.getToken` minted a token for
`tasks.readonly`, `tasks.googleapis.com` answered 200, and logcat carried no
`UserRecoverableAuthException`, no `access_denied` and no `403`. Cancelled at this
dialog; the dashboard still read `8 goals / 5 tasks done / 4 this week`.

> **The task rows in this screenshot are redacted, and deliberately so.** They are
> Ido's real Google Tasks — personal content — and **this repository is public**.
> What the screenshot is evidence *for* is that the scope worked: the dialog title,
> *"Found 10 open task(s)"* and the `Import 2` button all survive the redaction
> intact, so the claim is unweakened. The rows themselves proved nothing the count
> does not. Redacted 2026-08-10 by the `c7-what-is-a-unit` session, before this
> commit was ever pushed — caught by the pre-push read-what-you-are-sending check,
> not after the fact.

---

## 3. Verdict, and what it changes

| Claim | Verdict |
|---|---|
| Unverified app in production returns `403 access_denied` | ❌ **False** — it shows a warning with an `Advanced` override |
| …with **no override** | ❌ **False** — the override is on the first screen |
| Publishing status must therefore stay `Testing` | ❌ **Not for this reason.** It may still be preferred for a cleaner demo screen |
| Publishing is reversible | ✅ True, and now sourced — see [`docs/OPERATIONS.md`](../../OPERATIONS.md) §2 |

**The seven-day authorization clock is gone.** `goalpilot-56e30` is in production as of
2026-08-09, so grants no longer expire, and
[`#27 · C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) may specify
a calendar sync that stays true unattended — which is what `R17` asked for and what
`C9d` could not promise.

**What it costs, recorded so it is a decision and not a surprise:** first consent on any
account now requires `Advanced → Go to GoalPilot (unsafe)` — one extra tap, once per
account — and the 100-new-user lifetime counter for unverified production apps is now
running. At an audience of two, both are noise.

---

## 4. The finding nobody was looking for

**`tasks.readonly` arrives unchecked on the granular consent screen.** A user who taps
Continue without noticing signs in successfully and grants nothing; the Tasks import then
has no permission, and the failure surfaces as an ordinary "grant permission" prompt
rather than as "you declined this".

This is live today on a shipped feature. `C9d` §4 raised granular consent as a
*hypothetical* risk for the future calendar surface —
[`#26 · C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26) — on the
reasoning that the calendar scope would be independently declinable. It turns out the
same thing already applies to Tasks, and has since the scope was first requested.

Filed separately; it is build work on a shipped feature, not a decision on this map.

---

## 5. A second stale claim, flagged not fixed

[`docs/OPERATIONS.md`](../../OPERATIONS.md) §4 states that Google appends the project
number to unverified app names on the consent screen (`GoalPilot-297750736036`). Across
all four screens captured here the name read plain **GoalPilot**. Either Google changed
this behaviour or it applies to a surface this run did not reach. Recorded rather than
rewritten, because "not observed once" is weaker evidence than the claim deserves — but
it should not be relied on.

---

## 6. Sources

- [Manage app audience](https://support.google.com/cloud/answer/15549945) — Testing vs production, the 100-user caps, and the seven-day authorization expiry. **Silent on the return trip.**
- [Brand Approvals & Auto-Cancellations](https://support.google.com/cloud/answer/16868008) — the only page that states the round trip: *"If you switched to Testing or Internal, when you switch back to In Production or External, public users will immediately be able to sign in and access the previously verified configuration."*
- [Submitting your app for verification](https://support.google.com/cloud/answer/13461325) — also silent on returning to Testing.
- Prior asset: [`docs/research/2026-08-08-google-calendar-scopes-and-consent.md`](../2026-08-08-google-calendar-scopes-and-consent.md) — `C9d`, which surfaced this question.
