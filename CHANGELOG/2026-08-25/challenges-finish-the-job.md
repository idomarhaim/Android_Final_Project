# `challenges-finish-the-job` — 2026-08-25

> **Summary:** Ido can invite a friend to a challenge — a top-level `challengeInvites`
> collection, an offer-shaped row at the top of the Challenges screen, and the rules
> partition that makes both reachable without ever writing into somebody else's space.

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
