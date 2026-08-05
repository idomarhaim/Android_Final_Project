# Changes — 05/08/2026 — session `submission`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** `firestore.rules` deployed to live `goalpilot-56e30` and the non-owner challenge join proven end-to-end with two real accounts on two emulators; the spec §7 sharing demo turned out to be largely already satisfied, and the backlog was wrong about it.

The last MUST item. No application code changed — this session is a **deploy and
a verification**, plus the backlog corrections that verification forced.

## 🚀 The rules deploy

`firebase deploy --only firestore:rules` moved the live release
`projects/goalpilot-56e30/releases/cloud.firestore` from ruleset
`8f80b66d-0970-4f72-80b1-59dcbd37ff80` (31/07) to
`d38c7248-f5c4-464a-b01b-d7edba01ce6b`. Confirmed by reading the released
ruleset back over the Firebase Rules API and checking it carries the
`participants` block — not by trusting the CLI's success line.

The deployed delta against what was live is **purely additive**: one
`match /participants/{uid}` block inside `challenges/{challengeId}`, plus
comments. No existing `allow` was loosened or removed. The 16 rules tests in
`firestore-tests/` were re-run against that exact file first, all green.

Written by the `challenges` session on 04/08 and deliberately left undeployed
since; this closes it.

## 🤝 The non-owner join, proven against the live backend

The point of the deploy. Creating a challenge auto-joins the owner, so a single
account cannot exercise the path that was broken — `firestore-tests/` was the
only evidence it worked. Now:

1. As `name.iddo@gmail.com` on `Pixel_10_Pro_XL` — created **"August Steps
   Race"** (`challenges/QAhNVr80WbJ1W2IBELAZ`, type `STEPS`, open-ended).
2. As `rachil751@gmail.com` on `Pixel_10_Pro_XL_B` — the challenge appeared
   under **Discover**, and **Join** wrote
   `challenges/QAhNVr80WbJ1W2IBELAZ/participants/n3X0X5RaEtefJR71jSTbVWeWAB72`.
3. As the non-owner — reported a score of **8200**, updating that same row.
4. Back on the owner's screen, with no interaction: the card re-ranked itself to
   **#2 · 0 steps**, and Standings showed `#1 רחיל עאדר 8200 / #2 עידו מר-חיים 0`.

Every step verified in Firestore over the REST API, not from the UI alone.

**The owner auto-join is itself proof the deploy took effect.** It writes
`challenges/{id}/participants/{ownerUid}`, and a subcollection is not covered by
the parent `match` — under the old ruleset that write matched no rule and was
denied. It succeeded, so the participants block is live.

## 📋 The backlog was wrong about the sharing demo

`Submission.TODO.must.md` claimed *"Only one account has ever signed in"* and
listed the friend-code exchange as work still to do. Live Firestore says
otherwise, and has since **02/08**:

| Backlog claim | Actual state of `goalpilot-56e30` |
|---|---|
| One account has ever signed in | Two `publicProfiles` — `NDXVJC` and `8ZFFSM` |
| Friend codes still to exchange | Both edges present, written 02/08/2026 |
| Leaderboard never seen with two rows | Ranks both (50 pts / 0 pts) |
| Shared feed unproven | One share by account A, rendering in the feed |

So spec §7 needed capturing, not building. The friends-only leaderboard showing
both users, and the feed item, were both screenshotted this session.

Also corrected: the governance note at the foot of that file said `AGENTS.md`
carried template marker v4 against a library at v7. It is at **v14** since
`f7ae3dd`, and verbatim projections are now swept in bulk by
`Update-TemplateConsumers.ps1` rather than a repo at a time.

## 🩺 Health Connect, re-checked on API 37

Not part of the claim, checked on request. On `Pixel_10_Pro_XL` the whole read
path works: the card correctly reported `PERMISSIONS_REQUIRED`, the Health
Connect grant dialog offered Steps and Sleep, and after granting, the card
flipped to *"Sync steps & sleep"*. Running it returned **"Health Connect has no
steps or sleep for the last week"** — the empty-store path, degrading exactly as
`HealthConnectManager` documents.

GoalPilot never talks to Samsung Health directly; Samsung Health is one of the
apps that *writes into* Health Connect on a real phone, and GoalPilot only reads.
So this confirms availability, permissions and the empty read — and leaves the
proposal → Firestore write path still unproven against real readings. **The
physical-phone follow-up stands**, now with the API-37 emulator half of it
closed.

## 🧪 Tests

| Layer | Result |
|---|---|
| Security rules (`firestore-tests/`) | **16/16 pass**, re-run against the deployed file |
| JVM unit | **not run** — no Kotlin, Gradle or resource file changed this session |
| Instrumented | **not run** — same reason; the APK installed on both AVDs is current `HEAD` |
| Live backend (manual, two accounts) | Challenge create · non-owner join · non-owner score update · live re-rank on the owner's device — each confirmed in Firestore over REST |

The two suites are skipped rather than failing: this session deployed a rules
file and drove the app, and changed no code that either suite covers.

## 🖥️ Devices

Both emulators booted from snapshot and had current `HEAD` installed
(`:app:installDebug`). Account A's app session had been signed out and was signed
back in via the device account; account B's Google account was added by Ido
mid-session, which was the one thing no agent could do.

One quirk worth recording: on `_B`, `adb shell screencap` served a **stale frame**
after the score dialog saved — byte-identical PNG, old contents. `uiautomator
dump` showed the true state (`#1 · 8200 steps`). When a screenshot looks frozen
on `_B`, trust the view hierarchy over the pixels.
