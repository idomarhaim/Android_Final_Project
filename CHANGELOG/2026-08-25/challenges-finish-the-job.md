# `challenges-finish-the-job` — 2026-08-25

> **Summary:** Ido can invite a friend to a challenge — a top-level `challengeInvites`
> collection, an offer-shaped row at the top of the Challenges screen, and the rules
> partition that makes both reachable without ever writing into somebody else's space.
> Joining now **creates** the goal it needs, too, so an invited friend with no steps goal
> is one form away from racing rather than one screen away.

One brief replacing four (`sessions/challenges-finish-the-job.md`, `#23`). **Ido was
unavailable for this entire session** — *"I'll be driving, so I need this to be in one
session without my involvement"* — so every open decision below is **taken and recorded as
mine**, and every one of them is his to overturn.

---

## §1 · Invite a friend to a challenge

His own report, 2026-08-24:

> *"in the version I have on my phone I can create a CHALLENGE but I cannot invite a
> friend I have in the app to the CHALLENGE"*

`Observed:` he was exactly right. `grep -rni 'invite' app/src/main` returned ten hits and
**every one was a KDoc using the English word** — there was no invite mechanism anywhere
in the app. The only route into somebody else's challenge was **Discover**, which
`observeDiscoverable()` fills with every challenge in the database, newest 50, for every
signed-in user. So joining was never impossible; there was simply no way to say *"join
mine"*, and no way for the other person to be told.

### Where it had to live, and why the tidy homes are unreachable

This is the whole design problem and it is dictated by `firestore.rules`, not by taste:

| candidate | why not |
|---|---|
| `users/{friendUid}/…` | `users/{uid}/{document=**}` is `isOwner(uid)`. You cannot write into your friend's space **at all** — which is the property the rest of the app depends on. |
| their participant row | `challenges/{id}/participants/{uid}` is `isOwner(uid)` too. And a row conjured for somebody who never joined would put a competitor in the standings who never agreed to compete. |
| a Cloud Function with the Admin SDK | It would work, and it is the wrong reach: a deploy, a second write path and a trigger for something the rules partition already models. `C20` scoped the Function layer to **derived numbers**. |

What is left is the one shape this file already has for a document two different users must
each reach on their own account: **`shares/{shareId}`**, one level up. So:

```
challengeInvites/{inviteId}
  challengeId · challengeTitle · fromUid · fromName · fromPhotoUrl · toUid · createdAt
```

`challengeTitle`, `fromName` and `fromPhotoUrl` are **copied at send time**. They are
captions, not sources of truth — a row that had to resolve two more documents to render one
sentence would flash a title-less line first, and `acceptInvite` reads the real challenge
anyway and fails honestly if it is gone.

### The one clause that is different from `shares`, and the trap it buys

`shares` reads `allow read: if isSignedIn()`, because a share is published to a feed. An
invite names **two people and is nobody else's business**, so its read rule inspects
`resource.data`. That has a consequence the client cannot ignore:

> ⚠️ **A read rule that inspects `resource.data` constrains every QUERY, not only every
> GET.** Firestore will not evaluate a query document-by-document; it rejects up front any
> query it cannot *prove* is inside the rule. An unconstrained
> `collection('challengeInvites')` listener fails with `PERMISSION_DENIED` **on the query
> itself**, and nothing in the message says the missing filter was the cause.

So every listener carries `whereEqualTo("toUid", myUid)` or `("fromUid", myUid)`, and the
rules suite asserts **both directions** — because the success alone would pass just as
happily against a rule that let everybody read everything. The test that matters is *the
invitee's own unfiltered query is denied*: the same user who may read the one document in
the collection, refused, which is what makes it a statement about the **query**.

`allow update: if false`. An invite is created and consumed, never edited — an editable one
would let a sender repoint a standing offer at a different challenge after the fact, and
the copied caption is exactly what the invitee is reading when they decide.

### The flow

- **Sending** — a person-add icon in the challenge card's header opens a sheet listing the
  user's friends (`observeLeaderboard(friendsOnly = true)`, reused rather than a new
  repository call: it already joins private friend edges to `publicProfiles` **by document
  id**, which is precisely the read needed). Friends already in the challenge, or already
  holding an invite, are **greyed with their reason rather than filtered away** — a friend
  who silently vanishes reads as *the app does not know them*, which is the one thing the
  user is certain is false.
- **Receiving** — a row at the **top** of the Challenges screen: *"Ann invited you to
  “August Steps Race”"*, with **Join** and **Dismiss**. Joining runs the ordinary
  participant write **under the invitee's own uid** and deletes the invite in the same
  batch; dismissing deletes it alone and says nothing to anybody.
- **Nothing is ever written to the invitee.** Joining stays their own act, which is what
  keeps the whole participants partition honest — and there is a rules test asserting the
  *sender* still cannot write the invitee's participant row.

### Decisions taken here, all mine to overturn

1. **The affordance is an icon in the card header, not a fourth action button and not an
   overflow item.** Ido's complaint was that he *could not find* a way to invite anybody,
   so the overflow answers the letter of the report and not its substance. The action row
   is the other candidate and `card-with-invite-light.png` shows it is already at its width
   limit — *Change goal · Type a score · Standings* is three, and a fourth wraps at his
   384 dp / font 1.15. An icon costs no row width.
2. **No badge, no count, no red dot, and no notification.** An invite is an **offer**; a
   number on a tab bar turns it into a chore. Notifications here schedule reminders for the
   user's *own* work, and `C9a` §6's consent story does not cover an inbound request from
   another person. Named as owed, deliberately not built — the brief says the same.
3. **Declining gets no confirmation dialog**, unlike leaving or deleting. Those destroy
   something the user built; declining an offer destroys nothing and the sender can make it
   again. A dialog would turn a shrug into a decision.
4. **A second invite to the same person is refused, not duplicated** — checked against the
   sender's *own* invites, which is the only slice the rules let them query. Two different
   people may each invite the same friend, and that is correct: they are two offers.
5. **The sender must be in the challenge, and that check lives in the repository, not the
   rules.** A rule sees the invite document and nothing else, so it cannot reach into
   `participants` to ask. It is a courtesy check rather than a defence — the worst a
   modified client achieves is an offer to join a challenge the recipient could already
   have found in Discover.

### 🧪 Tests

| layer | result |
|---|---|
| **JVM unit** — `ChallengesViewModelTest` | **53 pass, 0 fail** (was 40; **+13** for §1) |
| **JVM unit** — whole suite | **1160 tests, 2 failing** — both in `DocsCurrencyTest`, see *Open* below |
| **Security rules** — `firestore-tests/rules.test.mjs` | **73 pass, 0 fail** (was 55; **+18** for §1) |
| **Instrumented render pass** — `ChallengeInviteRenderPass` | **1 test, 10 frames, green** |
| Compose UI behaviour | covered by the existing `ChallengesUiTest`, which still builds and was extended only where the `MyChallengeCard` signature changed |

The 18 rules tests are the design argument made executable, the same job the existing
`regression` / `the fix` pair does for joining: the two-party partition, the query trap in
both directions, forged senders, self-invites, the update ban, and the two that say what an
invite does **not** buy.

### 📸 Render pass — `docs/render-passes/2026-08-25-challenges-finish/challenge-invite/`

Five frames in both brightnesses, and **every one was opened and looked at**, which is the
rule this repo learned on 2026-08-24 when three instrument failures each produced a *green*
result that was false.

| frame | what it was composed to expose | verdict |
|---|---|---|
| `invite-row` | the ordinary inbound invite | **Reads as an offer.** Filled *Join* beside a text *Dismiss*, one plain sentence, sender's initial in the avatar. Nothing about it demands. |
| `invite-row-long` | the sentence is built by **concatenation** of two unbounded strings — a Google display name and a user-authored title | Wraps to three lines and holds; the fourth would ellipsise, which is the right fallback |
| `invite-sheet` | one invitable friend, one **already in**, one **already invited** — a frame of three live rows would prove nothing about how a greyed row reads beside a live one | The blocked rows are legibly secondary and each carries its reason. The decision to grey rather than filter survives the picture |
| `invite-sheet-empty` | no friends at all; the fix is on another screen | The sentence says which screen. A blank list would not |
| `card-with-invite` | **is the icon discoverable?** — the actual substance of Ido's report | The person-add glyph sits beside the overflow, clearly distinct, and the frame also confirms decision 1: three actions already fill the row |

The `Surface` wrapper is in the pass for the reason the file beside it records — without it
the dark frames render dark foreground on the host's light window and read as a product
defect that does not exist. `invite-row-dark.png` is genuinely dark, so it worked.

The floors are size **and more than one colour**: a 1344×2992 rectangle of flat background
passed every size assertion twice on 2026-08-24 before anybody opened the file. This pass
samples a 24×24 grid and fails under three distinct colours.

### ⚠️ One trap paid for in a failed build

`local.properties` backslashes and `--` inside XML comments are already in this repo's
`CLAUDE.md`. This session found the third member of that family the hard way and it is
already recorded there too: **`/*` inside a KDoc opens a NESTED block comment in Kotlin.**
Writing the Firestore path `users/{friendUid}/**` inside a doc comment in `Constants.kt`
swallowed the rest of the file; the compiler reported `Missing '}'` at line 80 and
`Unclosed comment` at line 141, naming neither the KDoc nor the token. Spelt out as
`users/{friendUid}/{document=**}` and annotated in place. Checked **mechanically**
afterwards, not by eye — a depth-counting scan over all ten touched files, which is what
found it and confirmed the fix.

---

## §2 · Joining creates a goal — §6's half-built promise, finished

> **Joining links or creates a goal**, so a challenge hands you tracking you did not have.

Linking shipped with `challenge-scoring`; creating did not. A user with no goal of the
challenge's kind got a sentence naming the kind and a trip to the Goals screen — honest,
and one screen short.

**§1 is what made it urgent.** The whole point of inviting a friend is that they can
actually compete, and a friend asked into a Steps Race may well have no steps goal. The
shortest path from *"someone invited me"* to *"I am racing"* must not detour through
another screen.

`GoalLinkSheet`'s empty branch is now a **form** rather than a message: a title seeded from
the challenge, a target, and one button.

### The three decisions in it, all mine to overturn

1. **The measure is not a field.** It is the challenge's, **copied whole**, which makes the
   new goal scoreable *by construction* rather than by the user picking a matching kind out
   of a dropdown — the one thing they could get wrong here with no way to diagnose it. A
   test asserts `challenge.canBeScoredFrom(theCreatedGoal)` directly, so a future
   divergence fails loudly instead of producing a goal that silently cannot score the
   challenge it was made for.
2. **The target starts blank.** A challenge names a **unit**, never a finish line —
   *"most steps this month"* has no target in it — so a pre-filled number would be the app
   inventing an ambition on the user's behalf, on the one object §1.1 says needs their
   declaration. They typed it and pressed Create, so `declaredBy = USER`, the same value
   `AddEditGoalScreen` stamps and for the same reason.
3. **Creating and linking are one act, not two.** Two would leave *"a goal made for a
   challenge that is not scoring it"* reachable by closing the sheet in between. So the
   link follows the create in the same call — and the half-done state is **said out loud**
   rather than reported as success: *"Goal created, but linking it failed: …"*, with the
   picker re-derived so the new goal is now in it and one tap finishes the job.

It goes through **`GoalRepository.upsertGoal`**, not a second creation path. The repository
is where a goal's id, ownership and defaults are decided, and a challenge screen inventing
its own would be a second writer of the same object — §0.3's most-repeated finding.

### 🧪 Tests

| layer | result |
|---|---|
| **JVM unit** — `ChallengesViewModelTest` | **61 pass, 0 fail** (was 53; **+8** for §2) |
| **Instrumented render pass** — `ChallengeGoalCreateRenderPass` | **1 test, 6 frames, green** |
| Security rules | **unchanged and untouched by §2** — creating a goal writes `users/{uid}/goals`, already covered by `isOwner(uid)`, and linking writes the `challengeReports` fact §6 already built. No rules change, and that is the point: this feature needed none. |

### 📸 Render pass — `docs/render-passes/2026-08-25-challenges-finish/challenge-goal-create/`

Three frames in both brightnesses, all opened. The risky question here is not a string
either: **does a form appearing where a message used to be read as *help*, or as
*paperwork*?** The user came to join a race, not to fill in a goal editor.

**Verdict: help.** Three controls, the unit shown rather than typed, and one button whose
verb names the whole act. `goal-link-picker` is in the pass to prove the **non**-empty
branch still reads as a picker — this session edited the composable those rows live in, and
a frame is the only thing that shows it.

⚠️ **And the pass found a defect no test would have.** The empty-branch sentence read

> *"None of your goals measures **count** in steps."*

— `MeasureKind.COUNT.label()` lowercased and dropped into prose. It is app machinery and it
reads as a typo. Now *"None of your goals is measured in steps."* The **kind** is still what
the matching is done on; that precision moved into a comment, where it belongs, rather than
into the user's sentence. Pre-existing wording, inherited from the message this form
replaced — and it took putting it above a form somebody actually reads to notice.

---

## §3 · The measure-change approval flow

§6, verbatim, and the shape was not mine to re-open:

> the owner writes `pendingMeasure` on the challenge document, **each participant writes
> `approvedChangeId` in the one document they are permitted to write**, and the Function
> applies it when every row agrees.

It exists because a challenge's measure is **the unit every participant's score is expressed
in**. A leaderboard that said *8200 steps* yesterday and *8200 km* today has not been
corrected, it has been **falsified**, and nobody who is not looking for it will notice.

**As of this commit there is no other path.** `firestore.rules` pins `measureKind` and
`measureWord` against every client write — the file's *second* field-level condition, the
same shape `publicProfiles.points` and the participant's `score` already use. The comment
that said *"THAT IS NOT BUILT YET ... an owner may still edit `measureKind` outright"* is
gone, replaced by what actually ships.

### The three things §6 left open, and how each is settled

**1 - What "adapt" means, and the answer is that it is not a choice.**

`C7` §5 offered the owner *reset* or *adapt*. Working out what **adapt** could mean is what
settles the design:

- **A change of `kind`** invalidates every participant's link, because `canBeScoredFrom`
  matches on kind. Adapting it needs either a **unit conversion** — which `Measure`'s own
  KDoc records this app deliberately does **not** perform — or a **re-link**, which
  necessarily restarts the number in the new unit. So a kind change **is** a reset. There is
  no second option to offer.
- **A change of `word` alone** changes no arithmetic at all. That **is** "adapt", and it is
  free.

So the consequence is **derived from the change**, not read off a mode the owner picked —
`Challenge.pendingConsequence` in Kotlin, `consequenceOf` in TypeScript, one rule in two
languages. The owner is *told* which one their edit carries, live, as they type.

⚠️ **Both still need unanimous approval, and the relabel is the one that can lie hardest.**
Renaming `km` to `miles` leaves every stored number alone while changing what all of them
claim. **The gate is on the claim, not on the arithmetic** — which is why a word-only change
is not waved through, tempting as that was.

**2 - While pending, the challenge keeps scoring in the OLD unit.** The pending fields sit
*beside* the live measure, never on top of it, and nothing on the scoring path reads them. A
JVM test asserts the whole of it: with a change pending, the card still says `km`, still
offers linking, still offers reporting, and still filters goals by the old kind.

**3 - Leaving during a pending change.** The quorum is **everyone who is still here**. A
participant who leaves has no row to write `approvedChangeId` in, so counting them would let
one person walking away freeze the challenge forever. The function reads whatever rows exist
when it runs — and **a departure re-triggers it**, because the last hold-out leaving is
exactly the event that completes agreement. Written into the rule's comment, as the brief
asked.

### Two registrations, and the second is not optional

`applyMeasureChangeOnApproval` fires on a participant row; `applyMeasureChangeOnProposal`
fires on the challenge document. The last act before a change applies is **sometimes the
owner proposing** — a solo challenge has one row, approved in the proposal's own batch, so
nothing further is ever written to a participant row and the first registration alone would
wait forever. There is an emulator test for exactly that case.

### Why this is a Function when §1's invites deliberately were not

The same test, answered opposite ways in one session, which is worth recording:

| | invites (§1) | measure change (§3) |
|---|---|---|
| what the write needs to see | one document, two named parties | **every participant's row**, then the challenge document |
| does a client have that reach? | yes — the rules partition models it | **no**, and no single client can |
| verdict | rules, no Function | Function, `C20`'s own criterion |

### 🧪 Tests

| layer | result |
|---|---|
| **JVM unit** — `ChallengesViewModelTest` | **74 pass, 0 fail** (was 61; **+13** for §3) |
| **JVM unit** — whole suite | **1181 tests, 2 failing** — both `DocsCurrencyTest`, see *Open* |
| **Functions arithmetic** — `functions/test/*` | **190 pass, 0 fail** (**+17**, `measureChange.test.mjs`) |
| **Functions emulator triggers** — `triggers.emulator.mjs` | **23 pass, 0 fail** (**+6**) |
| **Security rules** — `firestore-tests/rules.test.mjs` | **83 pass, 0 fail** (was 73; **+10**) |
| **Instrumented render pass** — `ChallengeMeasureChangeRenderPass` | **1 test, 6 frames, green** |
| **Deployed** | `firestore:rules` released; `functions` deployed — `applyMeasureChangeOnApproval` and `applyMeasureChangeOnProposal` both *"Successful create operation"* |

### 📸 Render pass, and the two things only pictures found

`docs/render-passes/2026-08-25-challenges-finish/challenge-measure-change/`. The risky
question: **does *"every score restarts at zero"* arrive in time to stop somebody doing it
by accident?** This is the one action in the app that destroys other people's numbers.

1. ⚠️ **`Agree` was a filled primary button, directly under the red warning.** Same visual
   weight as *Change goal* two rows above it. §1's own principle is that the balance a user
   reads off a row is *which action looks like the default* — and a filled button made
   agreeing look like the default for the one action that wipes your score. The red sentence
   was doing all the work and the button was quietly undoing it. Now the button's weight
   **follows the consequence**: outlined on a `RESET`, filled on a `RELABEL`. A relabel
   costs nobody anything, and making a harmless yes/no look grave is the other way to train
   people to ignore it.
2. ⚠️ **A dialog cannot be photographed the way a sheet can, and the flat-colour floor is
   the only thing that said so.** The pass first composed the whole `MeasureChangeDialog`
   and captured `isRoot() and hasAnyDescendant(hasText(...))` — the selector that *does*
   rescue a sheet's content — and got back **one flat colour**, the scrim. The frame was
   full-screen, 1344 px wide and weighed something on disk; only *"1 distinct colour,
   expected at least 3"* caught it. The body was split into `MeasureChangeContent` in the
   same commit, which is the rule this repo learned on 2026-08-24: **a surface that cannot
   be photographed cannot be reviewed.**

### ⚠️ And a third finding, in the test layer, worth more than either

The emulator suite's first fixture wrote `score: 3000` onto a participant row **by hand**,
and also gave that participant a `goalId` link. Writing the link fires
`projectChallengeScore`, which sums that user's own progress against the goal — of which
there is none — and correctly republishes **0** over the hand-written 3000. The RELABEL case
then failed `0 !== 3000`, reading as a bug in a code path that was behaving perfectly. The
first fix aimed at the wrong cause (a cascade from the previous test) and did not help.

The fixture now writes a **typed report**, waits for the projection to publish it, and only
then proposes — so the numbers under test are ones the system actually produced.

**The general shape:** *in a suite whose whole subject is a projection, a hand-written
derived value is a fixture the system is entitled to overwrite.*

It also exposed that the `RESET` case alone proves nothing — `0` is what the projection
produces anyway from a deleted report. `RELABEL` is the discriminating half of the pair:
`3000` survives only because `participantUpdate("RELABEL")` deliberately writes nothing but
`approvedChangeId`.

---

## §4 · The Health Connect gate — a decision paper, and the deliverable is this section

**§4 is not "not done".** The brief scoped it as a paper rather than a feature, and this is
it. It concerns a call **Ido made himself**, recorded in §6:

> *"No Health Connect connection → you cannot join a health-sourced challenge —
> comparability over inclusion."*

**Recommendation: delete the gate. Do not build it.** That reverses his own call, so it is
his to reverse back, and the argument is below in full so he can.

### 1. Its premise was weakened by §6 itself

The gate was decided when the model was *score from a raw health metric*, where a **reading**
and a **typed number** genuinely were not comparable — one is a measurement of a walk, the
other is an assertion about one. §6 changed the model underneath it: every score now routes
through **a goal of the same kind**, whatever feeds that goal. So the quantity being
compared is already the same quantity, and the incomparability the gate existed to prevent
is largely gone.

### 2. The residual difference is now LABELLED rather than excluded

Ido's own third ask shipped on 2026-08-24: a typed score says *"Reported by Ann · 8200
steps · 1d ago"*, and a derived one says nothing, because the absence of a badge is the
honest default.

**The gate and the badge adjudicate the same risk** — *is this number a reading or a
claim?* — and **one of them is already in the product**. Building the other would exclude
people from a race in order to prevent something the app now simply **says**. That is the
strongest of the three points: it is not that the gate is hard, it is that it is
**redundant against shipped behaviour**.

### 3. Its subject no longer exists

`ChallengeType` is deleted, and **nothing left on a challenge means *health***. Three
candidates and why each fails:

| candidate | why it cannot carry the gate |
|---|---|
| the measure `kind` | A `COUNT` of **books** and a `COUNT` of **steps** are the same kind. Gating on `COUNT` would lock somebody out of a reading challenge for not having Health Connect. |
| the linked goal's `healthSourceKey` | Knowable only **after** linking, and the gate has to fire at **join**. It also describes the joiner's own goal, not the challenge. |
| an owner-ticked boolean | Knowable at join, and **self-asserted** — the owner ticks *"this is a health challenge"* with none of the authority the gate was supposed to have. A gate whose input is a claim is a badge with extra steps. |

### The two options, stated fairly

**A — owner-declared flag.** Add `isHealthSourced` to the challenge, tick it in the create
dialog, and refuse `joinChallenge` when it is set and Health Connect is not connected.
*Cost:* a model field, a rules clause, a create-dialog control, a join-time permission
check, and a new failure mode on the one action the product most wants to succeed.
*Honest value:* it enforces a rule whose input the owner asserts, against a difference the
badge already reports.

**B — delete the gate; the badge supersedes it.** Nothing is built. `#23`'s §6 line is
marked superseded with this reasoning, so the next reader finds the decision rather than
the gap.

### Recommendation, and what would change it

**B.** The gate would exclude people from a race to prevent something the app already
labels, using a flag its own owner asserts, on a model that no longer distinguishes health
from anything else.

**What would change my mind, concretely:** if challenges ever become **public and
competitive between strangers** — a leaderboard people do not personally know each other
on — then a label stops being enough, because a label works by social accountability and
strangers have none. At that point the right answer is still probably not this gate but
**server-side verification of the reading**, which is a different and much larger piece of
work. Today every challenge is reached through Discover or an invite from a friend, so the
accountability the badge relies on is real.

**Nothing was coded for §4, deliberately.** No model field, no rules clause, no UI.

---

## Open at the time of this entry

- **`DocsCurrencyTest` is red on `main`, 2 of 1160, and I did not fix it.**
  `docs/ARCHITECTURE.md` is under a **live board claim** by `docs-repair`, so it is not
  mine to write. Measured against `HEAD` = `34a0dbd` by replaying the guard's own regexes:
  - `every callable the backend exports is named in ARCHITECTURE` — **already red before
    this session**, missing `fileGoal` and `planGoal` from `ai-goal-onboarding`'s
    `90ee0fd` / `b1faf65`. Nothing of mine is in it.
  - `every Firestore collection the client writes is named in ARCHITECTURE` — **clean at
    `HEAD`; this one is mine**, and `challengeInvites` is the only new name.

  Naming all three anywhere in that file closes both tests — the guard is a `contains`
  check. A note saying exactly that is on `SESSIONS.md` below `docs-repair`'s row.
