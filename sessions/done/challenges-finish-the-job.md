---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 23
supersedes:
  - sessions/challenge-measure-approval.md
  - sessions/challenge-health-gate.md
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Challenge.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/ChallengeRepository.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/ChallengeRepositoryImpl.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Dtos.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Mappers.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/**
  - app/src/main/java/com/idomarhaim/goalpilot/core/util/Constants.kt
  - functions/src/**
  - functions/test/**
  - firestore.rules
  - firestore-tests/rules.test.mjs
  - app/src/test/java/com/idomarhaim/goalpilot/domain/ChallengeStandingsTest.kt
  - app/src/test/java/com/idomarhaim/goalpilot/feature/challenges/**
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/ChallengesUiTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/ChallengeProvenanceRenderPass.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/ChallengeInviteRenderPass.kt
  - docs/render-passes/<the day you run>-challenges-finish/**
  - CHANGELOG/<the day you run>/challenges-finish-the-job.md
  - kb-candidates/<the day you run>-challenges-finish-the-job.md
  - sessions/challenges-finish-the-job.md
  - sessions/challenge-measure-approval.md
  - sessions/challenge-health-gate.md
singletons:
  - Gradle daemon
  - a device / emulator, for the render passes
  - the Firestore + Functions emulators
created: 2026-08-24 by challenge-scoring
---

# Finish challenges: invite a friend, and close everything §6 still owes

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

## 📱 Device state

**SIGN IN NOT NEEDED, and DO NOT SIGN OUT.** Every render pass here composes components
directly with hand-built state — no account. **§6 below wants a signed-in device**, and if it
is already signed in, keep it that way.

**Never `connectedDebugAndroidTest`** — it uninstalls the app and takes any Google account with
it. Always:

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell am instrument -w com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
```

⚠️ **Build BOTH APKs and install BOTH, every time.** A production symbol added to the app APK
while only the androidTest APK is reinstalled dies with `NoSuchMethodError` mid-run —
`challenge-scoring` lost a cycle to exactly that on 2026-08-24.

---

## ⛔ Ido is unavailable for this entire session. Decide; do not ask.

He said so explicitly: *"I'll be driving, so I need this to be in one session without my
involvement."*

So the ❓ rule's **derive-and-decide** default is not merely preferred here, it is the only
option. Two decisions that the superseded briefs left open are **taken below** so nothing
stalls on them. Record every further decision as *yours* in the changelog, say plainly it is
his to overturn, and keep going. **Do not end a turn with a question.**

---

## Why this brief exists, and what it replaces

Ido, 2026-08-24, looking at the four briefs on his screen:

> *"What are all these kickoffs? Are they still not finished? If not, make ONE kickoff that
> finishes all of them … Also, in the version I have on my phone I can create a CHALLENGE but
> I cannot invite a friend I have in the app to the CHALLENGE — make sure the unified kickoff
> lets me do that."*

| brief | state |
|---|---|
| `challenge-scoring.md` | **done** — `cdd7ae4` + `71c67fc`, moved to `sessions/done/` |
| `challenge-measure-approval.md` | open → **§5 here** |
| `challenge-health-gate.md` | open → **§4 here** |
| `challenge-scoring-render-pass.md` | open, **stays open** — it is the pass on *Ido's own S25* at 384 dp / 450 dpi / font 1.15, which needs him to reconnect the phone. Not runnable while he is driving. The emulator passes are §6 here. |

**And one thing no brief covered at all: §1, the invite.** It is his own report, it is first,
and it is the only item here that a user has actually asked for twice.

---

## §1 · Invite a friend to a challenge — DO THIS FIRST

`Observed:` there is **no invite mechanism anywhere in the app**. `grep -rni 'invite'` over
`app/src/main` returns ten hits and every one is a KDoc using the English word. Ido's report is
exactly right.

Today a friend can only reach a challenge through **Discover**, which
`observeDiscoverable()` fills with *every* challenge in the database, newest 50, for every
signed-in user. So it is not that joining is impossible — it is that there is no way to say
*"join mine"*, and no way for the other person to be told.

### Where the invite has to live, and why the two obvious homes are unreachable

This is the whole design problem, so do not rediscover it:

- **Not `users/{friendUid}/…`** — `firestore.rules` gives `users/{uid}/{document=**}` to
  `isOwner(uid)` alone. You cannot write into your friend's space at all.
- **Not their participant row** — `challenges/{id}/participants/{uid}` is `isOwner(uid)` too,
  and a row conjured for someone who never joined is the exact bug
  `ChallengeRepositoryImpl` already documents.
- **Not a Cloud Function with the Admin SDK.** It would work, and it is the wrong reach: it
  buys a deploy, a second write path and a trigger for something the rules partition already
  models. Keep the Function layer for *derived numbers*, which is what `C20` scoped it to.

**Build it as a top-level collection, mirroring `shares/{shareId}`** — the one existing shape
in this file for a document that two different users must both reach:

```
challengeInvites/{inviteId}
  challengeId, challengeTitle, fromUid, fromName, toUid, createdAt
```

```
match /challengeInvites/{inviteId} {
  // NOT `isSignedIn()`: an invite names two people and is nobody else's business.
  allow read: if isSignedIn()
    && (resource.data.toUid == request.auth.uid || resource.data.fromUid == request.auth.uid);
  allow create: if isSignedIn()
    && request.resource.data.fromUid == request.auth.uid
    && request.resource.data.toUid != request.auth.uid;
  // Either party may remove it: the invitee accepts or declines, the inviter withdraws.
  allow delete: if isSignedIn()
    && (resource.data.toUid == request.auth.uid || resource.data.fromUid == request.auth.uid);
  allow update: if false;   // an invite is not edited; it is created and consumed
}
```

⚠️ **A `read` rule that reads `resource.data` constrains every QUERY, not just every get.**
Firestore rejects a query it cannot prove is inside the rule, so the client must always ask
`whereEqualTo("toUid", myUid)` (or `fromUid`). An unconstrained
`collection("challengeInvites")` listener will fail with `PERMISSION_DENIED` and the message
will not tell you why. **Put a rules test on exactly that**, both directions.

### The flow

- **Sending.** On a challenge the user is in, an **Invite** action opens a sheet listing their
  friends. Friends are `SocialRepository.observeFriendUids()` (a private edge under
  `users/{me}/friends/{uid}`) joined to `publicProfiles/{uid}` for a name and photo. Exclude
  anyone already a participant, and anyone already invited — a second invite is noise, not
  emphasis.
- **Receiving.** A section at the **top** of the Challenges screen: *"`<name>` invited you to
  `<challenge>`"*, with **Join** and **Dismiss**. Join runs the existing `joinChallenge` and
  deletes the invite in the same batch; Dismiss deletes it alone.
- **An invite is not an obligation.** No badge count, no red dot, no nagging. It is a row that
  is there and then is not.
- **Nothing is written to the invitee.** Joining is still their own act, which is what keeps
  the whole participants partition honest.

### Where it does NOT go

**Do not put invites in the notification system**, tempting as it is. Notifications in this app
are scheduled reminders for the user's *own* work; an invite from another person is a different
class with a different consent story, and `C9a` §6 does not cover it. Name it as owed if you
think it is right — do not build it here.

---

## §2 · Joining creates a goal — §6's half-built promise

> **Joining links or creates a goal**, so a challenge hands you tracking you did not have.

Linking shipped. Creating did not: a user with no goal of the challenge's kind currently gets a
message naming the kind and a trip to the Goals screen. Honest, and one screen short — and
**§1 makes it urgent**, because the whole point of inviting a friend is that they can actually
compete, and a friend invited to a Steps Race may well have no steps goal.

`GoalLinkSheet`'s empty branch is where it belongs. Create from the challenge's own measure,
pre-titled from the challenge, and **link it in the same act** so the user lands back on a
challenge that is already scoring itself.

Reuse `GoalRepository.upsertGoal`; do not invent a second creation path. `HealthMetric`
(`domain/usecase/`) already maps a health source to a `MeasureKind` and is what a future
auto-fed goal would need.

---

## §3 · The measure-change approval flow

§6, verbatim, and the shape is decided — it is not yours to re-open:

> the owner writes `pendingMeasure` on the challenge document, **each participant writes
> `approvedChangeId` in the one document they are permitted to write**, and the Function
> applies it when every row agrees.

It exists because a challenge's measure is **the unit every participant's score is expressed
in**, so changing it mid-flight silently re-denominates other people's numbers.

Three things it has to settle, and **you settle them**:

1. **Reset or adapt.** Ido's `C7` §5 answer offers the owner both. *Reset* is easy. *Adapt* is
   not obviously well-defined: a `DERIVED` score is a sum over the linked goal's entries, so
   re-denominating means either a unit conversion — which `Measure`'s KDoc records this app
   deliberately does **not** perform — or **re-linking**, i.e. every participant picks a new
   goal. Decide which *adapt* means and say why.
2. **While it is pending**, the challenge keeps scoring in the **old** unit. A half-approved
   change must not stop the race.
3. **Leaving during a pending change.** A participant who leaves has no row to write
   `approvedChangeId` in. The quorum is *everyone who is still here* — otherwise one person
   walking away freezes the challenge forever. Write that in the rule's comment.

`firestore.rules` currently carries a comment on `match /challenges/{challengeId}` saying this
is not built and naming the superseded brief. **Delete that comment when you ship, and do not
leave it half-true.**

`republishStanding` in `functions/src/projection.ts` already reads the challenge document, so
the Function that applies an approved change has a natural home beside it.

---

## §4 · The Health Connect gate — WRITE THE RECOMMENDATION, DO NOT BUILD IT

**This is a decision taken for this session, and it is the one most worth reading.**

§6 carries Ido's own call: *"No Health Connect connection → you cannot join a health-sourced
challenge — comparability over inclusion."* The superseded brief made that Step 0 and told the
session to ask him what "health-sourced" even means once `ChallengeType` is deleted. He is
unavailable, so here is the derivation, and the answer is **do not build the gate yet**:

1. **Its premise was weakened by §6 itself.** The gate was decided when the model was *score
   from a raw health metric*, where a reading and a typed number genuinely were not comparable.
   §6 routes every score through **a goal of the same kind**, whatever feeds that goal — so the
   quantity being compared is already the same quantity.
2. **And the residual difference is now LABELLED rather than excluded.** Ido's own third ask
   shipped on 2026-08-24: a typed score says *"Reported by Ann · 8200 steps · 1d ago"* and a
   derived one says nothing. The gate and the badge **adjudicate the same risk** — is this
   number a reading or a claim? — and one of them is already in the product. Building the other
   would exclude people from a race to prevent something the app now simply *says*.
3. **Its subject no longer exists.** Nothing left on a challenge means *health*. A measure
   `kind` will not do it: a `COUNT` of books and a `COUNT` of steps are the same kind. The only
   candidate that is knowable at **join** time is an owner-ticked boolean — a self-asserted flag
   with none of the authority the gate was supposed to have.

**So: write it up, do not code it.** Produce a section in your changelog with these three
points, the two options (*owner-declared flag* vs *delete the gate, the badge supersedes it*),
and a recommendation. **Recommend deleting it**, and say plainly that this reverses a call Ido
made himself and is his to reverse back. That is the deliverable for §4 — a decision paper, not
a feature. **Do not treat §4 as "not done".**

---

## §5 · Render passes, as you go — not at the end

Every UI unit above gets frames in **both** brightnesses before you move to the next one.
`ChallengeProvenanceRenderPass` is the working template; copy its shape.

⚠️ **Read `CHANGELOG/2026-08-24/challenge-scoring.md` §9 before writing a render pass in this
repo.** Three instrument failures there each produced a **green** result that was false:

- a **71 px** capture (a modal sheet renders in a window of its own, so `onRoot()` matches two);
- a **1344×2992 rectangle of flat colour** that passed *every* size floor, twice — only opening
  the PNG caught it. The floor is now *more than one colour*;
- a **"dark" frame that was light**, because a composable that paints no background puts
  dark-theme foreground on the host's light window. Wrap the pass in a `Surface`.

And the structural lesson, which applies directly to §1's invite sheet: **a surface that
cannot be photographed cannot be reviewed.** `StandingsList` was split out of `StandingsSheet`
for that reason. If your invite UI is a bottom sheet, split its content out the same way, in
the same commit — not as a follow-up.

**Judge, do not only capture.** For §1 the question is: does an invite row read as an
*offer*, or as an *obligation*? It must read as an offer.

---

## §6 · Put it on Ido's phone

He asked for this because he wants to **use** it, and the build on his phone is `v0.4.1`,
which predates every one of the changes above.

The Gradle App Distribution plugin **authenticates separately from the CLI** and works even
when `firebase projects:list` does not (`Observed:` 2026-08-22):

```bash
./gradlew :app:appDistributionUploadRelease
```

This is covered by his **standing authorisation** for Firebase actions that cost nothing —
`docs/OPERATIONS.md` § *Standing authorisation*. **Deploy; do not wait.** Bump the
`versionCode`, and verify the APK you distributed is the one you just built: a stale APK nearly
shipped a crash on 2026-08-24 (`9b595f4`).

⚠️ **If any Cloud Function changed** (§3 will), deploy it too, or the client writes a document
shape the deployed functions cannot read — which is exactly how `#55` left Ido's live points
reading 40 instead of 70:

```bash
export FUNCTIONS_DISCOVERY_TIMEOUT=120     # the 10 s default is not enough on this machine
firebase deploy --only functions --non-interactive
```

---

## Order, and what to do if you run out of session

**§1 → §2 → §3 → §4 → §6**, with §5 folded into each. That order is by value to Ido, and §1 is
the only item he has asked for in his own words.

**Commit each section as its own unit** with its own tests green — do not hold one commit for
the whole brief. If the session ends early, everything committed is real and the remainder is
one brief, not a rollback. **Write that remainder brief before you stop.**

## Read first

1. `AGENTS.md`
2. `CHANGELOG/2026-08-24/challenge-scoring.md` — **§2 is the model you are extending, §9 is
   the render-pass traps, §5 is what was already known to be owed**
3. `docs/PRODUCT_v0.3.md` §6, and §1.3 for what a measure is
4. `firestore.rules` in full — §1 and §3 both add to it, and the partition is the whole design
5. `data/firestore/SocialRepositoryImpl.kt` — `observeFriendUids`, `addFriend`; §1 joins to it
6. `C:\Dev\JARVIS\kb\dev\android-device-verification.md` §8

## Carries over

- `sessions/done/challenge-scoring.md` — what shipped, and its `result:` block
- `sessions/challenge-measure-approval.md`, `sessions/challenge-health-gate.md` — superseded by
  this brief; their reasoning is folded into §3 and §4 and they carry a banner saying so
- `sessions/challenge-scoring-render-pass.md` — **still open**, and deliberately: the pass on
  Ido's S25 needs him
- `docs/render-passes/2026-08-24-challenge-scoring/` — six frames of the current standings and
  card, to diff your changes against

## Out of scope

- The S25 render pass and the Hebrew read — `challenge-scoring-render-pass.md` and
  [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51). **Do not sweep
  `feature/challenges` into `AnalyticsLiteralSweepTest.SWEPT_PACKAGES`** as a favour; that test
  forbids it in as many words.
- `ui/theme`, `ui/components`, `ui/widget`, `feature/dashboard`, `feature/analytics` —
  `visual-parity` reserved those. **Check the board**: if they are still live, a render pass of
  yours may show their work in flight, which is worth a line in your changelog, not a fix.
- Notifications for invites. Named in §1, deliberately not built.

## Exit

Green at every layer each section touches — JVM, `functions/` arithmetic **and** its emulator
trigger suite, `firestore-tests/`, the instrumented suite — plus render frames per UI unit, a
`CHANGELOG/<day>/challenges-finish-the-job.md`, a build distributed to Ido's phone, and commits
throughout (auto mode: commit and push without asking, subject to the six push preconditions).

**Then close the ticket, or say why not.** [`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23)
is already closed as a *decision* ticket, so what is owed there is a **comment** naming what
shipped — never a re-close. Grep `sessions/` and `sessions/done/` for `issue: 23` first:
`challenge-scoring-render-pass.md` will still be open, and that is remaining work whatever your
own Exit says.
