# `challenge-scoring` — 2026-08-24

`C14` / [`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23) built:
**a challenge now scores itself from each participant's goal, and a typed score says who
typed it.** Kicked off from `sessions/challenge-scoring.md`, `AUTO MODE`.

---

## 0 · Step 0 first: the brief's blocker was already discharged

The brief's opening instruction was *"read `C7`'s state before writing any code"*, and
the honest first move if it were open was a note to Ido rather than a half-built scoring
path against free-text units. Both of `#23`'s blockers are **closed**:

- [`C7` #14](https://github.com/idomarhaim/Android_Final_Project/issues/14) — closed, and
  **built**: `Measure(kind, word)` over a closed seven-member `MeasureKind` ships in
  `domain/model/Measure.kt`.
- [`C3` #18](https://github.com/idomarhaim/Android_Final_Project/issues/18) — closed, and
  it says so on `#23` itself (comment, 2026-08-10 17:55): *"C7 discharged the measure half
  of your block; C3 has now discharged the scoring half."*

So nothing here was blocked, and the conditional note to Ido is not owed.

**And the unit was smaller than the brief thought, because [`#52`](https://github.com/idomarhaim/Android_Final_Project/issues/52)
had already shipped one of §6's six bullets.** `projectChallengeScore` existed,
`firestore.rules` already pinned `score` against client writes, and `reportScore` already
wrote a fact rather than the standing. §6's *"score becomes server-owned"* was **done**.
What was left is that the fact could only ever be **a number somebody typed**.

---

## 1 · What Ido asked for, and how it reconciles with §6

Verbatim, 2026-08-24:

> *"I want the challenge to be synced to HEALTH CONNECT (according to the challenge type)
> and generally to tasks. If someone updated manually and it was not updated through
> HEALTH CONNECT, then it should say there who performed the update and what they
> updated."*

Read quickly that collides with `docs/PRODUCT_v0.3.md` §6 — *"a challenge scores from
nothing of its own: it scores from each participant's goal."* It does not, and the
reconciliation is the whole design: **a goal is already the thing Health Connect and task
completions both feed.** `SyncHealthDataUseCase` writes a `ProgressEntry` against a goal;
ticking a task writes against a goal; a manual log writes against a goal. So *"synced to
Health Connect and to tasks"* is delivered by routing through the object that already
aggregates all three — which is why **no Health-Connect-to-challenge path was built**. That
would be the second representation of the same walk §6 exists to delete.

```
Health Connect ──┐
a completed task ┼──▶ ProgressEntry ──▶ Goal (kind + word)
a manual log ────┘                            │
                                              │  movement since you joined
                                              ▼
                                   [projectChallengeScoreOnProgress]
                                              │
                                              ▼
                     challenges/{id}/participants/{uid}.{score, scoreSource, reportedAt}
```

**Everything left of `Goal` already existed.** The two triggers on the right are this
session's, and so is everything that makes a participant able to point at a goal at all.

### The one tension the brief flagged, and why it needed no picker

The brief said §6 deletes `ChallengeType` while Ido said *"according to the challenge
type"*, and proposed asking him. **Derived instead, per the derivable-decision rule, and
recorded here so it can be overturned in one place.** Under `C7` a measure attaches to the
**object**, so a challenge's `kind` + `word` *is* what "according to the type" was reaching
for — `STEPS` is a `COUNT` of `"steps"`, `SLEEP` a `DURATION` of `"hours"`, `RUNNING` a
`DISTANCE` of `"km"`. The type carried no information the measure does not, and it sourced
nothing: **nothing in the codebase ever branched on it to produce a score.** Asking would
have been asking Ido to re-decide `C7`.

**The visible price, stated rather than buried:** the glyph on a challenge card now comes
from the kind, so a steps race and a books race are both `COUNT` and share one icon where
the old enum gave steps its own. That is the cost of deleting a second, decorative
classification beside the real one — and keeping it would be §0.3's most-repeated finding
in miniature.

---

## 2 · What shipped

### 2.1 A challenge carries a measure, not free text

`Challenge.type` and `Challenge.metricUnit` are **gone**; `Challenge.measure: Measure?`
replaces them, stored as `measureKind` + `measureWord`. `ChallengeType` is deleted from the
codebase entirely.

**Nullable in the model, never optional in the product**, and the two are not in tension.
§6 gives a challenge no optional measure — *"there is nothing to compare without a shared
unit"* — and both the ViewModel and the repository refuse to create one without a kind
**and** a word. Null is reserved for a document written **before** §6.

**The migration is on READ, with no backfill write** — the same shape and the same reasoning
as `GoalDto.resolvedMeasure`. It recovers what it can: the word survives verbatim from
`metricUnit`, and the kind comes from `ChallengeType`, the one field that ever said what the
number meant. **It deliberately refuses one case.** `metricUnit` defaulted to `"points"`,
and §6 rules that points may never be a challenge metric — points are a *view of effort*
(`C3` §1) and ranking on them ranks by **time logged**, which is the wrong race for anything
about an outcome. So §6 *deletes that default rather than re-homing it*, and such a
challenge comes back **unmeasured**: it cannot be linked to a goal, its card says so, and
its owner is asked what it counts. Guessing `COUNT` + `"points"` would have silently kept
the exact number `#23` was filed about.

### 2.2 One fact, two shapes — and they are mutually exclusive by construction

`users/{uid}/challengeReports/{challengeId}` is the fact a participant owns. It now carries
**either** `{ goalId, linkedAt }` — §6's scoring path — **or** `{ value, reportedAt }`, a
typed number. `linkGoal` and `reportScore` both write it with an **unmerged `set()`**, so
writing one *removes* the other rather than shadowing it.

**That exclusion is load-bearing, not tidiness.** If both could sit on one fact the
projection would have to pick a winner, and the standings badge — the whole of Ido's third
ask — would be reporting the outcome of a tiebreak rather than what the participant actually
did. Here there is nothing to break.

The collection keeps its `challengeReports` name: renaming a live collection is a migration
over every user's documents, for a word.

### 2.3 The score is movement since you joined, summed server-side

`scoreFromProgress(entries, window)` in `functions/src/derived.ts`, called from a shared
`republishStanding(uid, challengeId)` in `projection.ts` behind **two** triggers:

| trigger | fires on | why it exists |
|---|---|---|
| `projectChallengeScore` | `users/{uid}/challengeReports/{challengeId}` | the fact moved — a link, a typed number, or a delete |
| `projectChallengeScoreOnProgress` **(new)** | `users/{uid}/goals/{goalId}/progress/{entryId}` | **this is the one that makes `R1` go away** |

The second never mentions Health Connect, and that is the point: a health sync, a completed
task and a manual log all arrive at it identically because all three write a `ProgressEntry`
against a goal. Its fan-out is one `where("goalId","==",goalId)` over the user's **own**
facts — a handful of documents, no cross-boundary read, and a user in no challenge does one
empty query and writes nothing.

**Summed from timestamped entries, never stored as a delta**, which is §6's own choice and
the reason all four awkward cases stop being cases: re-linking re-sums a different list,
unlinking sums nothing, a backfilled entry lands inside or outside the window on its own
timestamp, and `ProgressEntry.sourceKey` already stopped a re-sync producing a second entry
to sum. A baseline would have had to be repaired in every one of them.

> ⚠️ **A derivation, flagged rather than smuggled.** §6 says *movement since you joined*.
> Taken literally, joining a challenge that **has not started yet** — which
> `ChallengePhase.UPCOMING` makes an ordinary thing to do — credits a September race with
> August's walking. So the window is `max(joinedAt, startAt)` to `endAt`, derived from §6's
> *movement* plus the phase model's position that a challenge's dates say when it is run.
> The upper bound has the same authority: `canReportScore` is already false once a challenge
> is `ENDED`, so a derived score that kept climbing afterwards would be two halves of one
> product rule disagreeing. Documented on `ScoringWindow`'s KDoc and asserted from both
> sides. **`Inferred:` from those two committed positions — Ido may read it the other way,
> and if so the change is one line in `Challenge.scoringWindowFor` and its TypeScript
> mirror.**

**Floored at zero, and that floor is load-bearing.** A weight-loss goal logs *downwards*, so
negative movement is real, and a negative score would sort a participant **below** somebody
who has done nothing while the standings offer no way to say why. §6 does not adjudicate
direction; a losing-weight race is named as owed work in §5 rather than guessed at here.

### 2.4 ⭐ Ido's third ask: a typed score says who and what

The participant row gains two **server-written** fields, `scoreSource` and `reportedAt`,
beside `score`.

**The absence of a badge is the honest default**, which is the way round the brief asked
for. A challenge is *meant* to score itself, so `DERIVED` is the ordinary case and a badge
on it would be noise on every row; `NONE` — nobody has scored yet — says nothing either.
**Only a number somebody typed speaks**, and it reads
*"Reported by Ann · 8200 steps · 24 Aug"*: who, what, when, and then it stops.

**The register is `C4`'s — the app never asserts an intrinsic edge by itself.** This is a
claim about *another user* rendered to everyone in the challenge, so: no warning icon, no
error colour, no word like *unverified*, and **the score is not re-ranked** — a typed number
sorts exactly where its value puts it.

⚠️ **And it is a label, never an attestation.** A participant writes only their own fact, so
`DERIVED` means *this person pointed the challenge at a goal of theirs* and nothing more.
§6's own honest residual stands unchanged: server-owned scoring stops a win being **typed**,
not a reading being **forged**. That sentence is in `ChallengeParticipant`'s KDoc, in
`firestore.rules`, and in `derived.ts`, because the one way this feature goes wrong is a
later surface treating the badge as proof.

**`scoreSource` is pinned by the rules for a reason `score` alone does not cover:** a
participant who could write their own label could type a number and mark it `DERIVED` — the
label would then assert exactly the thing it exists to deny. `serverOwns` now takes a
**list** (`['score', 'scoreSource', 'reportedAt']`), which is the only change to its
mechanism.

### 2.5 The screen

- The create dialog picks a **kind** from the seven and a **word** beside it — the same
  two-field shape the goal editor uses, deliberately, because a challenge and a goal are
  the same object measured the same way. The kind re-suggests the word until the user types
  one, reusing `MeasureKind.wordHint()` rather than keeping a second table that drifts.
- **"Score from a goal" is the primary action**; "Type a score" stays beside it, because
  somebody with no goal of the right kind still has to be able to compete today.
- The card says which it is — *"Scoring itself from your linked goal"* / *"Not linked yet —
  you are typing this score"*. A linked challenge moves **silently** by design, so the one
  place that has to be legible is the card of the person whose score it is; the standings
  row is a claim about somebody else, and only the exception speaks there.
- The score dialog warns **before** the write when typing would take the challenge off its
  goal. Discovering that afterwards from a badge on your own row would be the app having
  made the decision.
- The goal picker filters on **kind, never word**: two people racing on steps may have
  written `"steps"` and `"צעדים"`, and matching on the word would keep a Hebrew user out of
  an English race for a reason that has nothing to do with what is being counted. An empty
  result is a first-class state naming the kind to create, not a blank list.

---

## 3 · 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | ✅ **1125 / 1125**, 0 failed, 0 skipped. **+18** new across `ChallengeStandingsTest` and `ChallengesViewModelTest` |
| **Cloud Functions arithmetic** (`functions/`, `node --test`) | ✅ **105 / 105**, 0 failed — **+12** new |
| **Cloud Functions triggers** (`functions/`, real emulator) | ✅ **17 / 17**, 0 failed — **+7** new. Was 10 |
| **Security rules** (`firestore-tests/`, emulator) | ✅ **55 / 55**, 0 failed — **+5** new. Was 50 |
| **Instrumented UI** (`app/src/androidTest`) | ⚠️ **Compiles clean, NOT RUN** — the device is claimed. See §6 |
| **Device render pass** | ⚠️ **NOT RUN** — the device is claimed. See §6 |

**The trigger layer is where this was actually proven**, and it is worth saying why the
arithmetic suite was not enough. `projection.test.mjs` cannot answer whether a path pattern
matches a **three-segment** subcollection, whether the `where(goalId ==)` fan-out finds the
fact, or whether the Admin write lands on a row the writer may not touch. The seven new
emulator cases do: linking projects `DERIVED` and `0` before any progress exists; a log
moves it; a second entry adds and a delete subtracts; a year-old entry imports nothing; the
challenge's own dates bound both ends; an **unlinked** goal moves nothing; and typing over a
link — and linking over a typed number — each switch the row and clear the other's stamp.

### 3.1 The suite that reported 15 failures and had run nothing at all

`npm run test:emulator` came back **15 of 17 failed**, including every pre-existing points
test and `classifyTask` — none of which this session touched. That reads exactly like *the
new trigger broke everything*, and it is nothing of the kind. One warning line, above all
the output:

```
!! functions: Failed to load function definition from source: FirebaseError: User code
   failed to load. Cannot determine backend specification. Timeout after 10000.
```

**No function was registered, so no trigger fired**, and every assertion timed out at 15 s.
The two that "passed" are the two that assert a trigger does *nothing*. Refuted in one
command before touching any code, per `CLAUDE.md`'s own recipe for the same trap on
`firebase deploy`:

```
$ node -e "const t=Date.now();const m=require('./lib/index.js');console.log(Date.now()-t,'ms',Object.keys(m))"
212 ms [ 'projectPoints', 'projectPointsOnTaskWrite', 'projectChallengeScore',
         'projectChallengeScoreOnProgress', 'getRecommendations', 'classifyTask',
         'scoreTask', 'proposeMeasure' ]
```

`FUNCTIONS_DISCOVERY_TIMEOUT=120` is now **set by `run-emulator-tests.mjs` itself**, and the
runner prints it, so a silent no-function run cannot look like a real failure again. The
full diagnosis is in that file's header — this is a fix, not a workaround: `CLAUDE.md`
recorded the trap for `firebase deploy` only, and it bites `emulators:exec` identically.

### 3.2 The instrument was checked, not just run

Per `kb/dev/look-at-your-own-output.md`: a suite that passes proves nothing until it can be
shown to fail. `scoreFromProgress`'s end bound was mutated from `>=` to `>` — making the
window inclusive — and the suite went **104 / 1**, naming the case. Reverted, and green
again at 105 / 105.

---

## 4 · Files

**Domain** — `domain/model/Challenge.kt` (rewritten: `Measure`, `ScoreSource`,
`ScoringWindow`, `canBeScoredFrom`, provenance on the participant and the standing;
`ChallengeType` deleted), `domain/repository/ChallengeRepository.kt` (`linkGoal`,
`createChallenge` takes a `Measure`).

**Data** — `data/firestore/dto/Dtos.kt`, `dto/Mappers.kt` (read migration),
`data/firestore/ChallengeRepositoryImpl.kt`.

**UI** — `feature/challenges/ChallengesViewModel.kt`, `ChallengeDialogs.kt` (measure picker,
`ReportedBadge`, `GoalLinkSheet`), `ChallengesScreen.kt`.

**Server** — `functions/src/derived.ts`, `projection.ts`, `index.ts`; `firestore.rules`.

**Tests** — `app/src/test/.../ChallengeStandingsTest.kt`, `.../ChallengesViewModelTest.kt`,
`app/src/androidTest/.../ChallengesUiTest.kt`, `functions/test/projection.test.mjs`,
`functions/test/triggers.emulator.mjs`, `functions/test/run-emulator-tests.mjs`,
`firestore-tests/rules.test.mjs`.

---

## 5 · What §6 still owes, and it is named rather than quietly dropped

Four items, each with its own brief so none of them is a line in a changelog nobody
re-reads:

0. **`sessions/challenge-scoring-render-pass.md`** — the two device layers §6 below explains.
1. **`sessions/challenge-measure-approval.md`** — §6's *"changing the measure needs every
   participant's approval"*: owner writes `pendingMeasure`, each participant writes
   `approvedChangeId` in the one document they may write, the Function applies it when every
   row agrees. **Nothing today permits or prevents it** — an owner may still edit
   `measureKind` outright, and `firestore.rules` says so in a comment rather than implying
   otherwise.
2. **`sessions/challenge-health-gate.md`** — §6's *"no Health Connect connection → you
   cannot join a health-sourced challenge"*, Ido's own call, comparability over inclusion,
   with the gate made a route rather than a dead end. It needs a decision first: once
   `ChallengeType` is gone, **what makes a challenge "health-sourced"?** Not the measure
   kind — a `COUNT` of books is not health data.
3. **§6's *"joining links or creates a goal"* is half built.** Joining **links**; it does
   not yet **create**. A user with no goal of the challenge's kind is told which kind to make
   and sent to the Goals screen, which is honest but is one screen short of the promise.
   Carried in brief 2, whose gate it shares.

**Not owed, and worth saying so:** the brief's *"needs `C3`'s `start`"* turned out to be
about a **goal's own** baseline, not this. Since `#49` the entry **is** the progress — a
goal's current value is a sum over its `progress` collection — so *movement since you
joined* is a windowed sum over the same documents and needs no `start` field at all.

**Owed to [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51):** the new
strings are English literals in `feature/challenges`, which is **not** in
`AnalyticsLiteralSweepTest.SWEPT_PACKAGES` and so is unswept, not exempt (`AGENTS.md` §0.8).
`ReportedBadge` in particular builds a sentence by concatenation and will need restructuring,
not just extraction, when that sweep reaches this package.

---

## 6 · ⏳ The device is claimed, so two layers are `unverified`

The instrumented suite compiles clean and the render pass is written but **neither has been
run**. `emulator-5554` is up, and the board's Active-claims section gives it — with `adb` —
to **`62-tour-assembly`**, whose row also still owes *"Step 0: reverting the demo data seeded
on Ido's live account 2026-08-24 11:07"*. That is real pending work on a shared device, and
taking it could interfere with a revert against Ido's live account.

What was checked before concluding that, rather than assumed:

- **The board**, whole Active-claims section: 6 rows, one of them `62-tour-assembly` holding
  `emulator-5554` + `adb`. Nobody owns any path this session writes.
- **`s25-verify-on-real-phone`'s release note** (19:57) releases `Pixel_10_Pro_XL_B` and says
  it is left running — and its own 🚨 line establishes that **`emulator-5554` *is*
  `Pixel_10_Pro_XL_B`**. So the two statements about that serial disagree, which is exactly
  the case `unresolved counts as live` is for.
- **Liveness, per §5.3(c)**: `62-tour-assembly`'s last commit is `0737a18` at **16:56**, its
  last transcript turn **08:44** — quiet, but its row is *present*, not released, so the
  absent-row branch does not apply.

An addressed note is left on `SESSIONS.md` asking them to release the serial if they are done,
and the two layers are **`sessions/challenge-scoring-render-pass.md`** rather than a line here
that nobody re-reads. That brief carries the `install -r` + `am instrument` recipe (never
`connectedDebugAndroidTest`, which uninstalls the app and takes a Google account with it), the
four new `ChallengesUiTest` cases, and what to actually **look** at: whether the badge reads as
information or as an accusation, which no green assertion can answer.

---

## 7 · Board

`challenge-scoring` claimed at `f493bbf` and released in this commit. A second session,
`visual-parity`, claimed at `77a32f1` **queued behind this one** and reserves `ui/theme`,
`ui/components`, `ui/widget`, `feature/dashboard`, `feature/analytics` — it states that
`feature/challenges/**` is this session's, and no path is contended in either direction.

---

# Round 2 — the two device layers, run

Ido said *"I've just finished with 62"* mid-turn. That released `emulator-5554`, which is
the only thing §6 above was waiting on, so the run happened in the same session rather than
in the brief it had been deferred to.

## 8 · Both layers are now green, and the render pass earned its keep

| Layer | Result |
|---|---|
| **Instrumented UI** (`app/src/androidTest`, `adb install -r` + `am instrument`) | ✅ **327 / 327**, 0 failed. `ChallengesUiTest` alone is **13 / 13** |
| **Device render pass** | ✅ 6 frames, `docs/render-passes/2026-08-24-challenge-scoring/` |
| JVM unit, re-run after the fixes below | ✅ **1127 / 1127** — was 1125; **the +2 are `visual-parity`'s `AppMaterialTest`**, which arrived with `21ad2e0`, not this session's |

`connectedDebugAndroidTest` was **not** used, per `kb/dev/android-device-verification.md` §8
— it uninstalls the app and takes any Google account with it. Both APKs went on with
`adb install -r` and the runner was driven directly.

**`#58`'s order-dependence did not fire.** The full suite ran twice, in one order each time,
green both times.

## 9 · The defect the render pass found, and three ways the instrument nearly lied

### 9.1 The defect: a badged row broke the list's baseline

The badge lived in a `Column` beside the name, inside a `verticalAlignment =
CenterVertically` row — so for the one participant with a badge the row centred on a
**two-line** block while everybody else centred on one. Their **name floated up** and their
rank, avatar and score drifted down.

Every assertion was green. The words were right, the ranks were right, and the only thing
that showed it was opening the PNG.

**It matters beyond tidiness, which is why it was fixed rather than noted.** A row shaped
differently from its neighbours is *marked* — and this badge is a claim about another person.
`C4`'s register is that the app never asserts an intrinsic edge by itself, and singling a row
out by **geometry** does exactly that in a way no wording review can catch. The badge now
hangs below the row, indented to the name, and every row's first line shares a baseline.

**Also changed while looking:** the stamp was `formatDay` — *"Aug 24, 2025"* — which spends a
third of the line on a year nobody is in doubt about. Now `DateTimeUtils.relative`, so it
reads *"1d ago"* and falls back to a full date only when that is genuinely what you want.

### 9.2 Three instrument failures, each of which passed its own assertions

Worth more than the defect, because each one produced a **green** result that was false.

1. **A 71 px capture.** `AppModalBottomSheet` renders in a window of its own, so `onRoot()`
   matches two roots. Caught by the size floor inherited from `DurationBoxRenderTest`.
2. **A 1344×2992 rectangle of flat `#d3e3fb`.** Selecting the *tallest* root fixed (1) and
   introduced this: the tallest root is the **host** window, which — once the sheet has taken
   the content away — is empty. It passed **every** floor: big enough, wide enough, 22 kB on
   disk. **Nothing but opening the file caught it**, twice.
   - The fix is structural, not a better selector: `StandingsList` is now split out of
     `StandingsSheet`, so the thing under review renders in an ordinary window. **A surface
     that cannot be photographed cannot be reviewed**, which makes that seam part of the
     feature rather than a favour to a test.
   - And the floor is raised to the property a blank frame cannot have: **more than one
     colour**, sampled on a coarse grid.
3. **A dark frame that was light.** `StandingsList` paints no background — in the app a sheet
   does — so rendered bare it put dark-theme *foreground* colours on the host's *light*
   background. That reads as a real, serious product defect (*"the badge is unreadable in dark
   mode"*) and is false: the app never draws it there. Fixed by wrapping the pass in a
   `Surface`. **A render pass that photographs a composable out of its container measures the
   container's absence.**

### 9.3 A fourth, and it is the family `CLAUDE.md` already names

`StandingsList` was added to the **app** APK, and only the **androidTest** APK was rebuilt and
installed. The run died with `NoSuchMethodError: No static method StandingsList(...)`. Same
family as the `${PIPESTATUS[0]}` trap: the thing you install is not the thing you just built.
Both APKs go on together, every time.

## 10 · What was looked at, and the verdict

Six frames — standings, card-linked, card-unlinked — in **both** brightnesses:

- **Does the badged row read as information, or as an accusation?** Information. It is
  `labelSmall` in `onSurfaceVariant`, below the name, with no icon, no error colour and no
  word like *unverified*. Beside three unbadged rows it reads as a footnote, not a mark.
- **Is the absence of a badge the honest default?** Yes — `#1`, `#3` and `#4` say nothing at
  all and read as the ordinary case, which is what §6 means a standing to be.
- **Is it re-ranked?** No. Ann's **typed** 8200 sits at `#2`, above a derived 6050.
- **Long names?** `Yonatan Ben-Shimon` fits at 1344 px with room to spare; the badge is one
  line and ellipsizes rather than wrapping.
- **Both brightnesses?** Legible in both, after 9.2's third fix.

**Not done, and it stays owed:** the **Hebrew** look. `ReportedBadge` builds its sentence by
**concatenation**, which is the shape that breaks first under bidi, and
`feature/challenges` is unswept (`AGENTS.md` §0.8) so its literals are English by rule. It
belongs to [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) and is
named in §5 above rather than swept here as a favour, which that test explicitly forbids.

**Also still owed, unchanged by this round:** the run on **Ido's S25** at its real geometry
(384 dp / 450 dpi / font 1.15). He chose that instrument and he is right — the last four
layout defects were found there and not on an emulator — but it needs him to reconnect the
phone, and `visual-parity` is mid-way through changing what every screen looks like, so the
frames above would not survive their landing anyway.
`sessions/challenge-scoring-render-pass.md` stays **ready** for exactly that.
