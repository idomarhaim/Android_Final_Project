# `6-silent-filing` — 2026-08-20

> **Summary:** `#6` ships. The confirmation dialog that stood between every typed task and its goal is **deleted, not made optional** — `R3` asked for a setting and the triage promoted the ask out of settings, because §0.7 makes silence the rule and a toggle for it would contradict the rule. Filing under an existing goal is now one tap and one write, with a snackbar carrying **Undo** as the witness §0.7 demands. The one branch that speaks is the absent `suggestedGoalId`, and it **tells** rather than asking: it creates an `AI_SUGGESTED` goal that sits **pending** on the goals list with *Keep* / *Not a goal*, §1.1's lossless demotion built rather than described. Where the sorter is not confident enough even to propose, the task is filed with **no goal at all** — §3.5's *never invent a goal*, as a third branch rather than a caveat. Validation moved into the Cloud Function **singly** (§3.4): a new Firebase-free `classify.ts`, omit-never-substitute, the estimate group validated independently, and the fabricated 200 that used to answer a transport failure — and that took the **speaking** branch every time the network died — replaced by a rethrow. Two encodings had to be split that the obvious one collapses: an **absent** `declaredBy` means *predates `#6`* → `UNKNOWN`, and the demotion needs its own wire value or it silently undoes itself. JVM unit **506/0** (+25), instrumented **114/0** (+9), functions `node --test` **37/0** (+22), functions emulator **10/10** (+1, against the live model). **The brief's own "runs AFTER `#7`" was checked rather than obeyed** — `613b454` justified that pair on file adjacency alone, which binds concurrent sessions, not sequential ones. **Not deployed:** `firebase deploy --only functions` was not run, so the validation is not yet live.

**Session:** `6-silent-filing` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Brief:** [`sessions/6-silent-filing.md`](../../sessions/6-silent-filing.md) · **Issue:** [#6](https://github.com/idomarhaim/Android_Final_Project/issues/6)

---

## The ordering constraint, adjudicated rather than obeyed

The brief carried *"Runs AFTER `7-quickadd-complete`. Verified 2026-08-20"*, and `#7` had not run —
`sessions/7-quickadd-complete.md` was still `status: ready`. So the first unit of work was deciding
whether that binds.

`613b454` states its reason in full, and the reason is **file adjacency and nothing else**:

> `7-quickadd-complete` BEFORE `6-silent-filing` — Both edit
> `feature/dashboard/DashboardScreen.kt`. #7 adds a done-affordance to `SmartAddCard` (`:616`);
> #6 removes `SmartAddDialog` (`:275`, `:659`) from the existing-goal branch. Adjacent functions,
> one file.

Compare the pair immediately above it in the same commit, which is justified on **merit** —
*"#7 built first is built on a value about to be redefined"*. This one has no such clause. An
adjacency constraint is about two sessions writing at once; sequentially, whoever goes second
reads the file as it then stands. The board was empty and nothing held `DashboardScreen.kt`.

The substance was checked too, rather than stopping at the reason as written:

- `#7`'s own brief puts its affordance in `SmartAddCard`, the input row — **not** in
  `SmartAddDialog`. Removing the dialog does not remove `#7`'s site.
- The dependency, if any, runs the **other** way. With the dialog gone there is no confirmation
  step left to hang a *"...and it is already done"* checkbox on, so the card is the only surface
  left — which is where `#7` already intends to put it. `#6` first **forces** `#7`'s stated
  placement; `#7` first would leave a second, dialog-shaped placement available for `#6` to delete.

Recorded as this session's decision per `rules/derivable-decision.md`, in `7d60e90`.
`sessions/7-quickadd-complete.md` is updated in this commit with what actually moved under it.

---

## What shipped

### 1 · The dialog is gone, and it did not become optional

`R3` was *"it asks for approval on where to file every task you enter. The configuration default
should be that it does not ask and just does it."* The triage confirmed the ask and **promoted it
out of settings**, which deletes the thing it asked for: §0.7 says the app *may act silently on
instrumental structure*, so filing is silent **always** — no dialog, no toggle, no default to
configure. §4.9's settings surface has no such control and adding one would contradict §0.7.

| Before | After |
|---|---|
| type → *Sort* → **modal**: *"Add this task?"*, Add / Cancel | type → *Sort* → filed |
| `SmartAddState` carried a whole proposal (target goal, new goal, area, points, minutes, rationale, `isVisible`, `isSaving`) | `SmartAddState` is `isClassifying` + `taskTitle` |
| `confirmSmartAdd()`, `dismissSmartAdd()` | deleted |
| no feedback while the call was out — the dialog *was* the feedback | an in-place progress row on the card (`SmartAddTestTags.SORTING`) |

The progress row is not decoration and not a leftover of the dialog. The classify call is a round
trip to a Cloud Function; a tap that appears to do nothing for a second reads as a broken button,
and the snackbar that follows arrives too late to answer that. The text field is deliberately
**not** disabled while a task is in flight — the point of a quick-add is that the next thing can be
typed while the last one lands.

### 2 · The branch table, as a pure function

`domain/model/SmartFiling.kt`. Three rows, and the ticket is the difference between them:

| Classification | Decision | Speaks? |
|---|---|---|
| `suggestedGoalId` resolves to a goal the user has | `ExistingGoal` — filed there | **no** |
| no goal id, and a proposal above the confidence floor | `NewGoal` — an `AI_SUGGESTED` goal, **pending** | **yes** |
| no goal id, low or absent confidence, or nothing to propose | `NoGoal` — the task is filed with **`goalId = null`** | **yes** |

Three findings came out of writing it:

- **The third row is not in §3.4's table and is required by §3.5.** *"The sorter must never invent
  a goal; low confidence leaves `goalId` null"* has no home in a two-branch design, because the
  obvious fallback for *"no goal fits"* is to mint one out of the task's own title — which is
  exactly the invention §0.7 forbids, and it is worse than it looks: an invented goal is an
  intrinsic claim about what someone wants their life to be, minted from a sentence they typed in
  three seconds. `SmartFiling.decide` **does not take the task title as a parameter**, which makes
  that unwritable rather than merely discouraged.
- **The confidence floor is `0.5`, derived and stated rather than smuggled in.** No spec line fixes
  a number. `0.5` is *more likely than not*, and the two values the app itself produces sit either
  side of it: the offline heuristic reports `0.4` when it matched a goal by keyword (which takes
  the existing-goal branch and never consults the floor) and `0.2` when it matched nothing — the
  case that must not become a goal.
- **An existing goal is silent at any confidence.** §3.5's rule is about *inventing*. Routing a
  low-confidence match to the new-goal branch would invent **more** goals, not fewer.

### 3 · `declaredBy`, and the encoding that the obvious version collapses

§1.1's intrinsic marker: `USER | AI_SUGGESTED | UNKNOWN`, with **null meaning instrumental** — a
milestone, *a goal nobody wants for itself*. `AI_SUGGESTED` **is** the pending state; a second
`isPending` flag would be a stored restatement of it, free to disagree (§0.2).

**The trap, and it is a real one.** §7.1 says *backfill `UNKNOWN`* — so an **absent** field already
means *this document predates `#6`*. But §1.1 says an absent marker means *instrumental*. Both
cannot be true of the same absence, and §1.1's own text flags the soft spot: it marks that reading
`Inferred:`, not `Observed:`. If absence carries both meanings, a goal Ido has just told the app is
**not** a goal reads back as a goal on the very next snapshot, silently, with nothing in the UI to
say why — §1.1's *lossless demotion* undoing itself.

So the wire carries **three** values where the domain has two shapes:

| Stored | Reads as | Means |
|---|---|---|
| *(absent)* | `UNKNOWN` | written before `#6` |
| `"USER"` | `USER` | Ido wrote it, or kept a suggestion |
| `"AI_SUGGESTED"` | `AI_SUGGESTED` | proposed; **pending** |
| `"NONE"` | `null` | the marker was dropped — instrumental |

`"NONE"` is **not** the fourth enum value §1.1 rejects. That rejection is about the *domain*, where
a `NONE` constant would duplicate a null that already says it — and the domain still has exactly
three values plus null. This is a wire distinction between *never written* and *written as none*,
which no absence can carry, and it is the same shape as `GoalDto.unit`'s null, whose whole job is
to mean *this document predates nothing*.

**No backfill write runs.** Absence is mapped on **read**, which is safe today for a checkable
reason: nothing in the app can create an instrumental objective — `parentIds` does not exist on
`GoalDto` and no screen renders a sub-objective — so absence provably cannot already mean
*milestone*. `Observed:` by grep across `app/src`, 2026-08-20: zero hits for `parentIds`,
`declaredBy`, `AI_SUGGESTED` or `milestone` before this ticket. `C16` [#37] changes that and owes a
real backfill when it does. Same argument `#9` used for `DurationSource` yesterday, and it is the
same kind of argument: a migration settled by a fact about what the code can do, not by a
preference.

**A defect this ticket created and then caught, at the pre-commit gate's first question.** The read
mapper was written as `DeclaredBy.fromName(declaredBy) ?: DeclaredBy.UNKNOWN`, and that elvis fires
for **both** an absent field and the `NONE` sentinel — so a demoted goal read back as `UNKNOWN`, a
goal again. Three cases of `GoalDeclaredByMigrationTest` went red on the first run and named it.
The two cases must be split *before* the enum is consulted.

**Every goal-creation site now stamps something honest**, because a field that some writers leave
to a default is a field that lies:

| Site | Value | Why |
|---|---|---|
| the goal editor (`AddEditGoalViewModel.save`) | `USER` | it has been through *New goal* / *Edit goal*; that is the consent §0.7 asks for. Set on **every** save, so keeping a suggestion by editing it counts — and so an edit can never strip a marker back to `UNKNOWN` |
| quick-add's new-goal branch | `AI_SUGGESTED` | nobody has ruled on it |
| the Google Tasks import | `USER` | same sorter, but the import has a **review sheet**: the goal is on a list Ido ticked and confirmed |
| the Health Connect sync | *(default `UNKNOWN`)* | **a decision, not an omission — see below** |
| a legacy document | `UNKNOWN` | nothing recorded who made it |

### 4 · The witness, which is what makes the silence legitimate

*"Silent" is not "invisible".* §0.7 permits acting without asking; it does not permit acting without
a witness. Two surfaces:

- **A receipt per filing.** `DashboardViewModel.filed` emits a `SmartAddReceipt`; the screen turns
  it into a snackbar carrying **Undo**. Undo deletes the task, and the goal **only if this filing
  created it** — an existing goal is Ido's and predates the quick-add, so undoing a filing must
  never delete something the filing did not make. The receipt is consumed *before* `showSnackbar`
  is awaited, because that call suspends until dismissal and a second quick-add during those
  seconds would otherwise queue behind it or be swallowed.
- **The pending goal on the goals list.** `Suggested — not yet one of your goals`, with **Keep**
  (→ `USER`) and **Not a goal** (→ drop the marker). There is deliberately **no Delete**: the task
  underneath is real work Ido typed in, and throwing the goal away would take it with him.

Two placement decisions worth recording:

- The banner sits **under** the card, not as a chip on it. `GoalCard` is in `ui/components`, one of
  the two packages already swept for `#51`, so a raw English literal there would fail
  `AnalyticsLiteralSweepTest` — and AGENTS.md is explicit that opting a package in as a favour is
  the wrong move while Hebrew is parked. §0.8's surviving second sub-rule points the same way:
  form and words before iconography, and *Suggested* is more meaning than a badge can carry.
- **§1.1's *"the goals list filters to intrinsic only"* is deliberately NOT implemented.** With no
  milestone surface anywhere yet, filtering would make a demoted goal vanish from the only list
  that renders it — the exact opposite of a *lossless* demotion. It waits for `C16` [#37].

### 5 · Validation moved into the Cloud Function, singly

New `functions/src/classify.ts` — no Firebase imports, so `test/classify.test.mjs` runs it as plain
`node --test` in milliseconds, the same arrangement `derived.ts` has with `projection.test.mjs`.

- **Omit, never substitute.** Every field stands or falls alone and a failure is **absence**, never
  a null and never a clamp. Nothing clamps: reporting `480` for a model that said `900` would state
  a number nobody said, at a field the client branches on.
- **The estimate group is validated independently** (§3.3 D) — a nonsense duration does not cost
  the goal id, and a non-member goal id does not cost the duration.
- **`CATEGORIES` moved out of `index.ts` into the validator.** The list the prompt *offers* and the
  list the response is *checked against* must be one list, or the prompt drifts into offering a
  value the validator then silently drops.
- **The prompt now states the membership rule** — *never invent an id and never adapt one* — which
  is the failure `C11a` actually measured (silent id corruption, the only two failures in 248
  calls).
- **The client stopped re-checking membership.** `parseClassification` read
  `suggestedLifeAreaId?.takeIf { lifeAreas.any { … } }`; that is a second implementation of a rule
  §3.4 assigns to one place. What remains on the client is **resolution** in `SmartFiling.decide` —
  a lookup, which cannot disagree with anything because it either finds the object or does not.

**The fabricated fallback is deleted, and it was a live defect.** `classifyTask`'s catch used to
return a 200 carrying `suggestedGoalId: null`, `suggestedNewGoalTitle: taskTitle.slice(0, 40)`,
`estimatedPoints: 10`, `estimatedMinutes: 30`, `confidence: 0`. Two things were wrong with it. It
was a second implementation of the client's own §8 fallback, one the client could not tell apart
from a real answer — and because it nulled the goal id, **every transport failure took the
new-goal branch**, the one branch in §3.4's table that speaks. A dead network made the app announce
a new goal. It now rethrows, and the client's keyword-matching fallback runs.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **506 / 0** — +25 (`SmartFilingTest` 15, `GoalDeclaredByMigrationTest` 10) |
| **Instrumented** (`am instrument`, `Pixel_10_Pro_XL_B`) | **114 / 0** — +9 (`SilentFilingUiTest`), `OK (114 tests)` in 167.6 s |
| **Functions unit** (`npm test`, `node --test`) | **37 / 0** — +22 (`classify.test.mjs`) |
| **Functions emulator** (`npm run test:emulator`) | **10 / 0** — +1 (`classifyTask` through the callable, against the live model) |
| **Security rules** (`firestore-tests/`) | **not run — not applicable.** `users/{uid}/goals` is already covered by the owner-only `users/{uid}/{document=**}` match and `declaredBy` is a new field on an existing document, so no rule changed. |
| **Build** | `:app:assembleDebug` + `:app:assembleDebugAndroidTest` green |
| **Seen on a device** | ❌ **not done — the emulator is signed out.** See below. |

**The instrumented run went through `adb install -r` + `am instrument`, not
`connectedDebugAndroidTest`**, so it did not uninstall the app and a sign-in would have survived it
(`kb/dev/android-device-verification.md` §8). `Observed:` by `48-settings-surface` 2026-08-19, not
re-observed here — this AVD was already signed out when the session opened, so there was no
sign-in for this run to preserve. The order was chosen for it anyway: the stateless suite ran
**first**, while the device had nothing to lose.

**Three test designs worth not re-deriving:**

- **The emulator case asserts against an EMPTY goals list.** It calls the real model through the
  emulator, so the one thing it must not assert is what the model says. Membership against an
  empty list is the assertion that holds whatever comes back — no string is a member of nothing —
  and it proves the thing a pure test cannot: that the validator is actually wired into the
  deployed handler. It also branches on the status, so a GROQ outage does not turn it red: a
  non-200 asserts the *other* half of the change, that a transport failure arrives as a failure and
  not as a fabricated result. It took the 200 branch on the run recorded here (985 ms, a live
  call).
- **`SilentFilingUiTest` asserts what can no longer be shown** — `assertDoesNotExist` on
  *"Add this task?"* and *"Cancel"*. The ticket is a deletion, and a deletion's test is the absence
  it guarantees.
- **`UNKNOWN` must not render as pending**, and that has its own case. If it did, this ticket would
  ask Ido to re-declare every goal he has ever made.

---

## Open, and named rather than left to be discovered

1. **`firebase deploy --only functions` was NOT run.** The validation and the rethrow are tested
   against the emulator and are **not live**. A deploy touches `goalpilot-56e30`, which is a
   claimed singleton and not in this ticket's exit list, and this repo's recent history with
   functions deploys is not encouraging (`50-finish` r3, `c20-eventarc-fix`). Ido's call.
   **Behaviour against the old deployed function is still correct**, because the client resolves
   ids rather than trusting them: an invented goal id finds nothing and the task takes the
   new-goal branch. What is *not* live is the omit-never-substitute contract and the rethrow.
2. **The "seen" pass is owed and needs a sign-in.** Smart add writes a task and possibly a goal to
   Firestore, so it cannot be watched on a signed-out device. See the heading in the session reply.
3. **`estimatedPoints` survives, deliberately.** §3.3 A deletes `points` from the model's
   vocabulary outright — *"There is no `points` field, and there never will be"* — because §1.4
   computes it from minutes and difficulty. That inversion is `C1` [#19], and removing the field
   here would leave the client with no points at all in the interim. `#6` validates it (`5..50`);
   `#19` deletes it. The client's `?: 10` in `parseClassification` is a substitution that goes with
   it.
4. **The Health Connect sync's goals stay `UNKNOWN`, as a stated non-decision.** Both honest values
   are wrong: `USER` would claim a consent nobody gave — that sync runs on every foreground with
   **no review sheet**, so a goal appears with nobody watching — and `AI_SUGGESTED` would put this
   ticket's *Suggested / Not a goal* banner on the step goal of every user who has ever connected
   Health Connect, over a goal that is working. What §0.7 means for structure the **sync** asserts
   is a question `#6` does not answer, and inventing an answer would change a shipped feature
   nobody asked to change. The reason is written at the site.
5. **`C16` [#37] owes a real backfill** when milestones become creatable, plus §1.1's
   intrinsic-only filter on the goals list.

---

## What this push carried that is not mine

`git log @{u}..HEAD` at push time held **five** commits, and **two are `9-duration-box` r4's**:

| Commit | Session | What |
|---|---|---|
| `bc66295` | `9-duration-box` r4 | claim before giving the KB a withdrawal path (`SESSIONS.md`) |
| `ad7dd4f` | `9-duration-box` r4 | *the KB has a triggered deposit path and no withdrawal path at all* — `AGENTS.md`, `CHANGELOG/2026-08-20/9-duration-box.md`, `SESSIONS.md`, `kb-candidates/2026-08-20-9-duration-box.md` |

`git push` is **branch-scoped, not commit-scoped**, so they go up with this one whether or not
this session wants them to. They were adjudicated rather than assumed: r4 wrote an **explicit
release note** into `SESSIONS.md` (`> 🏁 9-duration-box r4 RELEASED 2026-08-20 — bc66295 (claim) →
this commit`), which is a positive signal that session wrote about itself and settles the question
without a transcript check; the working tree was clean at the same moment; and no path of theirs
overlaps anything here. Nothing of theirs was staged by this session — every commit above used an
explicit pathspec.

---

# Round 2 — the seen pass, and the defect only a device could find

Ido asked whether the account mattered for the sign-in. It did — but not the way the question
assumed, and the answer moved the **device** rather than the account: `#6`'s silent branch fires
only when `classify` resolves an **existing** goal, so on a fresh account every task falls through
to the new-goal branch and the thing this ticket is about never happens.

**And it needed nothing from Ido at all.** `CHANGELOG/2026-08-20/48-settings-surface.md` already
recorded both AVDs' state: `Pixel_10_Pro_XL` is signed in as **name.iddo@gmail.com** with 8 real
goals, and `Pixel_10_Pro_XL_B` was `rachil751@gmail.com` until `9-duration-box` r2's
`connectedDebugAndroidTest` took it. So the swap was **read out of the changelog**, then confirmed
on the device by reading the app's own persisted `FIREBASE_USER` before installing anything. The
sign-in request in round 1's reply was correct at the time and was withdrawn rather than paid.

## 🐞 The defect: the witness never rendered

**Found by looking. Every layer was green and nothing was shown.**

```kotlin
LaunchedEffect(filed) {
    val receipt = filed ?: return@LaunchedEffect
    viewModel.consumeFiled()          // ← nulls the state this effect is KEYED on
    val result = snackbarHost.showSnackbar(…)
```

`consumeFiled()` sets `_filed.value = null`, which changes `filed`, which **restarts the
`LaunchedEffect`** — cancelling the coroutine before it can reach `showSnackbar`. The task filed
correctly every time; the snackbar never appeared once.

**This is not a cosmetic failure.** §0.7 permits acting without asking *only because* the act is
visible afterwards and undoable. A receipt that never renders does not degrade the feature — it
removes the thing that made the silence legitimate. The unit would have shipped satisfying its own
spec on paper and violating it on the screen.

**The comment above it argued for the wrong order**, which is the part worth keeping: consuming
early was meant to stop a second quick-add queueing behind a ten-second snackbar. That concern is
real and is answered by the **key**, not by the ordering — a new receipt restarts the effect, which
dismisses the stale snackbar and shows the current one. So: **await first, consume after**, and
deliberately **not** in a `finally`, because on cancellation the flow already holds the next
receipt and clearing it there would swallow exactly the quick-add the wrong order was protecting.

### The regression test, verified in both directions — and the instrument needed fixing first

The effect is now `SmartAddReceiptSnackbar`, split out of `DashboardScreen` so
`SmartAddReceiptUiTest` drives the **real** wiring. Testing a copy of those six lines would have
re-tested the copy: the copy would have been written from the same wrong understanding and passed.

**Then the instrument itself was checked on the input it exists for, and failed it.** With the
harness's `onConsume` as an inert counter, the headline case `aFilingIsAnnouncedAtAll` **passed
against the broken order** — because nothing nulled the receipt, so the effect never restarted. An
instrument that does not model the callback cannot see a bug whose cause *is* the callback. Once
`onConsume` was made to null the state, the measurement was:

| Version | Result |
|---|---|
| broken order, inert-counter harness | 2 of 7 red — and **not** the headline case |
| broken order, harness that models the consume | **5 of 7 red**, headline case included |
| fixed order | **7 of 7 green** |

`Observed:` all three runs on `Pixel_10_Pro_XL`, 2026-08-20.

**Why the existing suites were blind to it.** `SilentFilingUiTest` drives `SmartAddCard` and
`GoalListRow` in isolation — every assertion in it passes either way, because none of them can see
a `ViewModel` flow reaching a `Scaffold`'s snackbar host. Isolated-component coverage cannot fail
on a wiring defect, and the bug lives in a **coroutine's lifetime**, not in any value, so no unit
test at any layer could have held it either.

## 🎨 The banner's attribution, revised after looking

First render: a bare row 2.dp under its card, in a list whose `Arrangement.spacedBy(12.dp)` puts
12.dp below it. On the device those gaps read as **near-equal** — the buttons carry Material's
48.dp minimum touch height, so the label floats in the middle of a tall row and the 2-versus-12
difference disappears. **Also not cosmetic:** *Not a goal* changes a goal's status, so a banner
that could belong to either neighbour demotes the wrong goal.

Revised to a tinted, inset row with square top corners and rounded bottom ones — a drawer pulled
out from under the card, welded to it. Re-rendered and looked at again: unambiguous.

⚠️ **A claim in the new code was then disproved by the next render and corrected.** The comment
said 16.dp was chosen because 24.dp wrapped the label; at 16.dp it wraps too — two buttons plus a
sentence do not fit one line on a phone. `maxLines = 2` is deliberate, the wrap is legible, and the
comment now says what the render actually shows.

## 👀 What was watched, in order

| # | Screenshot | What it shows |
|---|---|---|
| 1 | `seen-01-dashboard` | signed in as **עידו**, 8 goals, 70 pts — a real account with real goals |
| 2 | `seen-04-sorting` | *"Filing «Bench press 30 minutes at the gym»…"* **in place**, field already cleared for the next task. **No dialog.** |
| 3 | `seen-06-snackbar` | **`Added to "Strength Training"` · Undo** — the silent branch, with its witness |
| 4 | `seen-07-speaks` | **`No goal fitted — suggested "Learn to play the saxophone"`** — §3.4's one speaking row, telling rather than asking |
| 5 | `seen-11-banner-v3` | the pending goal on the list, `Suggested — not yet one of your goals` · *Not a goal* · *Keep* — **and no banner on any of the 8 legacy goals**, which is `UNKNOWN` reading correctly |
| 6 | `seen-12-demoted` | after *Not a goal*: the banner is gone and **the goal survives**, in its life area, with its task. §1.1's lossless demotion, on a real account. |

## 🧪 Tests — round 2

| Layer | Result |
|---|---|
| **JVM unit** | **506 / 0** (unchanged — the defect is unreachable from the JVM) |
| **Instrumented** | **121 / 0** — +7 (`SmartAddReceiptUiTest`), `OK (121 tests)` in 193.0 s |
| **Functions unit** | **37 / 0** |
| **Functions emulator** | **10 / 0** (round 1) |
| **Seen on a device** | ✅ six captures above, on `Pixel_10_Pro_XL`, signed in as Ido |

## ⚠️ Test data left on Ido's live account — his call, not this session's

The seen pass ran against **real data in `goalpilot-56e30`**, so it left real rows behind. Deletions
are always-ask, so nothing was removed:

- **3 tasks** — *"Bench press 30 minutes at the gym"* and *"Do 20 push ups and squats at the gym"*
  under **Strength Training**, *"Drink 2 liters of water today"* under **Drink 4 Liters of Water
  Daily**.
- **1 task** — *"Learn to play the saxophone"*, under the goal below.
- **1 goal** — *"Learn to play the saxophone"* in **לימודים**, created `AI_SUGGESTED` and then
  **demoted** by tapping *Not a goal*, so it now carries the `NONE` sentinel. It is the only object
  in the live project exercising that state, which makes it evidence as well as clutter.

The dashboard's *Overall progress* moved 25% → 22% as a result, which is the ninth goal averaging
in and not a defect.
