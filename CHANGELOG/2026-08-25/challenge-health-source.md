# `challenge-health-source` — 2026-08-25

> **Summary:** Health Connect becomes a first-class choice in a challenge — pick it and you
> never author a goal — and a challenge can now be **retroactive**: a race for a week that
> has already finished scores both participants over that week and names a winner. Both are
> Ido's instructions, both override decisions shipped hours earlier, and one of them
> uncovered a latent bug that had been silently discarding every health reading's date.

Two asks, hours after `challenges-finish-the-job` shipped §6, and Ido named the conflict
himself: *"I understand there is a conflict with what I said before — update per what I said
now."*

---

## §A · Health Connect, as a choice rather than a goal

> *"if I make a steps competition, there should also be an option to pull the logs straight
> into the CHALLENGE and not only through a personal GOAL of mine"*

### The half that was his to decide, and the half that was not

**His:** whether Health Connect appears as a first-class option. §6 said *"a challenge scores
from nothing of its own: it scores from each participant's goal"*. He has overruled that at
the product level, and the picker now leads with Health Connect.

**Not his, and not mine either — a capability:**

> **The scoring Function runs in the cloud and cannot read Health Connect.** It is an
> on-device API. The only way a reading has ever reached Firestore, or can, is
> `SyncHealthDataUseCase` writing a `ProgressEntry` against a goal, which
> `projectChallengeScoreOnProgress` then sums.

So a *literally* separate pipe would mean the app writing the same steps into Firestore
**twice**, under two summers that can disagree — `R1` rebuilt, visible as a challenge and a
goal reporting different totals for the same day. What ships instead delivers the thing he
asked for **where he asked for it — in the choice**: `LinkChallengeToHealthUseCase`
find-or-creates the canonical `healthSourceKey` goal and links it. He picks *"Steps"*, authors
nothing, logs nothing.

### Decisions, all mine to overturn

1. **Matched on measure `kind` alone, and the row says what it is.** A `COUNT` challenge might
   be counting steps or books and the app cannot tell. Matching the measure **word** would be
   a string match over user content, which §1.3 forbids and which would shut a Hebrew user's
   `"צעדים"` out of an English steps race. So the offer appears whenever the kind fits,
   labelled, and somebody running a books race does not take it. **That is §4's conclusion
   arriving from the other side**: the app has no honest way to know a challenge is *a health
   challenge*, which is exactly why §4 recommended deleting the proposed health **gate** — and
   why this is an offer with a label rather than a filter pretending to knowledge.
2. **An unavailable provider never removes the row.** It stays, greyed, with the reason. A
   choice that vanishes teaches nothing; *"Health Connect is not set up on this phone"* is the
   sentence that helps.
3. **"It sets up your *Weekly steps* goal to hold them" is said BEFORE it happens.** A row
   appearing on the Goals screen unannounced is the one thing about this design a user could
   fairly call a surprise. It is the same goal the sync would have made on its own first run.
4. **An archived health goal is not revived.** It was put away on purpose, and
   `canBeScoredFrom` already refuses it.

---

## §B · Retroactive challenges — and the bug underneath them

> *"I created a steps challenge from the start of last week to its end and invited rachil. If
> she accepted it, the challenge pulls both our data for that week and decides the winner."*

This was **impossible twice over**, and the second reason was a real defect.

### 1 · The window opened at `max(joinedAt, startAt)`

So rachil accepting today got a lower bound of *today* — past the challenge's own `endAt` —
and the race scored **zero for everybody**.

Changed in both languages: **a dated challenge scores its own window for everyone; `joinedAt`
bounds an open-ended one only.** What §6's rule was actually protecting — *"joining with a
year-old goal imports a year of history nobody raced for"* — can only happen when there is no
start date to bound with. Where the owner set one, `startAt` already excludes everything
before the race and `joinedAt` was adding nothing but this bug.

**The visible consequence is intended**: joining a dated challenge late credits you for the
whole window. Join a month-long race on the 20th and your first three weeks count — which is
what *"who walked most in August"* means, and the only reading under which an invitation to a
finished week is worth accepting.

### 2 · ⚠️ Every health reading was stamped with the moment the sync ran

`ProgressRepositoryImpl.logProgress` wrote `createdAt = System.currentTimeMillis()`
**unconditionally**, discarding whatever the caller passed. `SyncHealthDataUseCase` knows each
reading's own `epochDay` and had no way to say so, so **Monday's sync filed the whole weekend
as Monday**.

Nothing ever failed, which is why it survived: a goal's total is a plain sum and does not care
when an entry is dated. **A challenge cares** — `ScoringWindow.includes()` filters by exactly
this timestamp — so a retroactive race would have seen nothing even after fix 1.

`ScoringWindow`'s own KDoc already claimed *"a backfilled entry with an old timestamp correctly
changes nothing"*, describing a state no entry could reach. **The comment was ahead of the
code.** Both are now true: `logProgress` honours a supplied timestamp (zero still means now, so
every existing caller is unchanged), and the sync stamps each reading at **noon of its own day**
— noon rather than midnight so a day lands unambiguously inside its own window whichever end a
bound falls on.

### 3 · The window rule now has a shared-fixture pin, which it never had

`scoringWindow` decides *which entries a challenge counts*, exists in Kotlin and TypeScript, and
was pinned by **nothing** in `shared-fixtures/derived-state.json` — while being the likeliest of
all the shared rules to drift, because the two are written in different **shapes** (an `if/else`
against a nested ternary). Five `windowCases` now run through both readers, including the
retroactive one, the open-ended one §6 was protecting, and the `NaN` guard.

**Mutation-checked rather than assumed:** reverting the Kotlin rule to `max(joinedAt, startAt)`
turns the fixture red on the right case. An instrument that has not been shown to fail is not
evidence.

One existing test asserted the **old** rule and was rewritten rather than deleted — its third
line is the one that changed, and the case it was protecting is still the first line and still
passes.

---

## 🧪 Tests

| layer | result |
|---|---|
| **JVM unit** — whole suite | **1197 / 1197, 0 failing** (was 1183; **+14**) |
| **JVM unit** — `ChallengesViewModelTest` | **87 pass** (was 74; **+13**) |
| **JVM unit** — `DerivedStateFixtureTest` | **6 pass**, including the new shared window cases |
| **Functions arithmetic** | **195 / 195** (**+5** window cases, **+1** rewritten) |
| **Functions emulator triggers** | **23 / 23** |
| **Instrumented render pass** — `ChallengeHealthSourceRenderPass` | **1 test, 8 frames** |

## 📸 Render pass — and it found three defects in one frame

`docs/render-passes/2026-08-25-challenge-health-source/`. The risky question: **does "Health
Connect" read as the obvious answer, or as one more thing to understand?** He did not ask for a
new capability — a steps race could already be scored from a steps goal — he asked **not to have
to think about a goal**. A row that is merely one more item in a list has shipped and failed.

The first frame showed three things no test would have caught, all in the same picture:

1. **The sheet's title still said *"Score this from a goal"*** — directly above a Health Connect
   row that is not a goal. Now *"Where your score comes from"*.
2. **The intro still recited §6's rule verbatim** — *"how far you move that goal from the moment
   you joined"* — which had gone stale in **both** halves that day: *"that goal"* names something
   the reader may not have picked, and *"from the moment you joined"* is the rule §B replaced, so
   a retroactive race would have been described by its own sheet as scoring nothing. Replaced by a
   computed `windowNote` that states the challenge's real dates.
3. **"Steps · steps"**, from a naive label-and-unit join.

Verdict after the fixes: Health Connect sits at the top with a *"…or score it from a goal of your
own"* divider under it, and reads as the answer rather than as an option.

## Shipped — v0.5.3, versionCode 14

**Backend first, and it was not optional.** `derived.ts#scoringWindow` is the copy that
decides the winner — the client's is for display — so shipping the app without redeploying
would have left every retroactive challenge scoring zero on the server while the card said
otherwise. All twelve functions redeployed, *Successful update operation*.

`visual-parity` had taken versionCode 13 for v0.5.2, so this is **14 / 0.5.3**, and the
artifact was read rather than the build log:

```
package: name='com.idomarhaim.goalpilot' versionCode='14' versionName='0.5.3'
```

with its dex searched for one symbol per feature — `Straight from Health Connect`,
`Where your score comes from`, `hc:goal:`, `challengeInvites`, `pendingMeasureKind`. All
present. Distributed to the `testers` group (Ido **and both examiners**).

## 🧪 Final numbers, all after the changes above

| layer | result |
|---|---|
| **JVM unit** — whole suite | **1197 / 1197** |
| **Instrumented** — whole suite on `emulator-5554` | **331 / 331**, 435 s |
| **Functions arithmetic** | **195 / 195** |
| **Functions emulator triggers** | **23 / 23** |
| **Security rules** | **83 / 83**, unchanged this round |
| **Render frames** | 8, every one opened |

## Not done

- **The S25 render pass** (`sessions/challenge-scoring-render-pass.md`) stays open. Ido's
  phone (`R5CY21NM30D`, `SM_S938B`) was **attached to this machine** while this work ran, so
  it is now possible for the first time — but it is a different brief and was not taken. Every
  `adb` call this session targeted `-s emulator-5554` explicitly once two devices were
  present; nothing was installed on his phone.
- **`docs/exam-prep/gemini notebook output/`** — four files, **77 MB**, including a 45 MB
  `.mp4`. Ido said *"push everything"* with the file list on screen, and these are held back
  anyway pending one explicit word: git keeps them forever, and the push gate names large
  binaries specifically. Everything else in that dirty set went up.
- **The retroactive path is proven by tests, not by a real week.** The window arithmetic and
  the back-dating are covered on both sides, but nobody has yet created a real retroactive
  challenge, had a second person accept it, and watched the server pick a winner. That needs
  two accounts and a week of real Health Connect history — named here rather than implied.
