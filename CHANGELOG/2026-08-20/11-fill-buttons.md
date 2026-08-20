# `11-fill-buttons` — 2026-08-20

> **Summary:** `#11` shipped — `Goal.unit`'s free-text `"%"` is replaced by §1.3's measure (a closed `MeasureKind` plus the user's free word), the fill-button ladder is arithmetic (`target/16` at `1× 2× 3× 4×`, reproducing §1.3's 4 L and 40 L examples exactly), and the goal screen carries a repeat-tappable 2×2 button row with a running tally that is a sum over entries and needs no second counter. The migration classifies **nothing** from user text: a defaulted `"%"` becomes absent, a free word keeps its word and leaves its kind open, and only a goal the app itself authored is classified — off its `healthSourceKey`, never off the string. JVM unit **459/0**, instrumented **91/0**, render pass looked at and committed. Two defects were found by looking rather than by testing: a `3 + 1` button wrap, and an `enabled` gate that would have disabled the row offline after one tap.

**Session:** `11-fill-buttons` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Issue:** [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) · **Brief:** [`sessions/done/11-fill-buttons.md`](../../sessions/done/11-fill-buttons.md)

---

## What shipped

### 1. The measure — §1.3, `C7` [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)

`domain/model/Measure.kt` *(new)*:

```kotlin
enum class MeasureKind { COUNT, DURATION, DISTANCE, VOLUME, MASS, MONEY, PERCENT }
data class Measure(val kind: MeasureKind?, val word: String)
enum class InputMode(val logging: LoggingRule) { BUTTONS, NUMBER, TICK, AUTO }
```

`Goal.unit: String = "%"` is **deleted**. `Goal.measure: Measure?` replaces it, with `null` the
default because §1.3 and `E6` make an unmeasured goal legal. `Goal.inputMode` is new and defaults to
`NUMBER`, which is what every goal did before the field existed.

Two boundaries are written into the type rather than left to be remembered:

- **The kind is app logic, the word is user content.** The kind's seven labels live in
  `feature/goals/GoalMeasureStrings.kt` and will be translated when `#51` resumes; the word never is
  (§5.1 `C15b`). There is **no `label` constructor argument** on `MeasureKind` — that is the exact
  defect `GoalCategory.label` is deprecated for, since a language switch cannot reach one
  (`kb/dev/untranslatable-idioms.md` §1).
- **`Measure.kind` is nullable and that is the migration's half-way state**, not a normal value.
  There is deliberately no eighth `UNKNOWN` member: §1.3 closes the list at seven and says its labels
  are translated, so an eighth would be an untranslatable label inside a closed set.

`InputMode.OFFERED = [NUMBER, BUTTONS]` — the same one-switch shape `AppLanguage.OFFERED` uses.
`TICK` is declared and **not offered**, because with `currentValue` a sum over entries (§4.6) a
`SETS` mode means something other than *append an entry*, and deciding what belongs to `#22`.
`AUTO` is **derived, not offered**: a goal carrying a `healthSourceKey` *is* in it, and choosing it
on a goal without one would not create a data source.

### 2. The ladder — `domain/model/FillLadder.kt` *(new)*

`target / 16`, snapped to a readable base, at `1× 2× 3× 4×`, with rungs past the target dropped
(the first always kept).

| Target | Kind | Ladder |
|---|---|---|
| 4 L | `VOLUME` | `0.25 · 0.5 · 0.75 · 1` — §1.3's own worked example, exactly |
| 40 L | `VOLUME` | `2.5 · 5 · 7.5 · 10` — *"stays right at a 40 L target"*, exactly |
| 100 | `VOLUME` | `5 · 10 · 15 · 20` |
| 70,000 | `COUNT` | `5000 · 10000 · 15000 · 20000` |
| 12 books | `COUNT` | `1 · 2 · 3 · 4` |
| 3 books | `COUNT` | `1 · 2 · 3` |

**The rounding rule is this file's own choice and is labelled as such.** §1.3 says *rounded* and
names two examples, and **both divide exactly** — so neither constrains the rule. `Inferred:` from
what a fill button is for: it is tapped repeatedly, so its label must be legible at a glance and its
repeat count trackable, and `6.3` is neither. The base snaps to the nearest **1 · 2 · 2.5 · 5 × 10ⁿ**
(the standard chart-axis tick ladder), and it is the `2.5` rung that makes §1.3's 4 L example land
*on* `0.25` rather than near it. `COUNT` then rounds to a whole number and floors at one, because
*"log half a book"* is a category error rather than a smaller amount of reading.

**`target/16 × 4 = target/4`**, so the big button always finishes the goal in four taps and the small
one in sixteen, at any target. That is the property that lets one formula work across three orders of
magnitude, and it is pinned by a test.

### 3. The row — `feature/goals/FillButtonRow.kt` *(new)*, wired into `GoalDetailScreen`

A 2×2 block of `FilledTonalButton`s above *Log progress* (not instead of it — §4.6 keeps the dialog
reachable for an amount the ladder does not offer, plus a note and a photo), with the tally beneath.
Every tap writes one `ProgressEntry` and nothing else; **there is no counter behind the tally**,
because §4.6 already makes `currentValue` a sum over entries.

The tally is **not** a percentage. Restating the ring's percentage under the buttons would put
§0.3's *second number* back one row lower — and a percentage is the exact defect this ticket opens
on. Past the target it keeps counting: overshoot is legal and shown (§1.5), and a tally that stopped
at the target would be a fifth clamp after the four `#49` deleted.

Labels and tally are bidi-isolated (§4.8) and carry a plain content description, which is what
TalkBack announces and what the instrumented suite addresses — `onNodeWithText("0.25 L")` matches
nothing, because the rendered text carries FSI/PDI marks.

### 4. The migration — the trap the brief named

**Decision, written down: map what is unambiguous, keep the word either way, classify nothing from
user text.** Four branches in `GoalDto.resolvedMeasure`, in order:

| Document | Reads as | Why |
|---|---|---|
| Has `measureKind`/`measureWord` | those | already migrated; never re-derived, same posture as `resolvedLifeAreaIds` |
| `unit` blank or `"%"` | **no measure** | §7.1. Nothing on the wire tells a *chosen* `"%"` from a *defaulted* one — the field was written identically in both cases — and absent is the **recoverable** direction, because the goal is then asked. A wrongly-kept `PERCENT` looks decided and is never asked again |
| `healthSourceKey` matches a `HealthMetric` | that metric's kind + the stored word | the app authored this goal and has always known what it counts; the key is stamped at birth and unreachable from the UI (`#47`), so this reads **no user text** |
| anything else | word kept, **kind open** | classifying `"litres"` is a string match — broken by construction the moment the word is Hebrew, abbreviated or misspelt |

`GoalDto.unit`'s default moved from `"%"` to `null`, and that matters: with the old default a
document written *after* this change (which carries no `unit` at all) would deserialize to `"%"` and
be indistinguishable from a real pre-migration percent goal, so every new goal would arrive looking
like something the migration had to rescue. A write now sets `unit = null` outright, the same
migrating-write rule `lifeAreaId` already follows.

**Consequence, stated plainly:** Ido's live *"Drink 4 Liters of Water Daily"* arrives **unmeasured**.
That is `#11` working, not failing — it stops claiming to be 1% of anything. Fill buttons appear once
it carries a `VOLUME` measure, which is one visit to the goal's edit screen today and `C22`
[#44](https://github.com/idomarhaim/Android_Final_Project/issues/44)'s offer when that lands.

### 5. `Double.trimNumber()` was wrong twice over, and `#11` is what found it

`ui/components/GoalCard.kt` had `"%.1f".format(this)`. The ladder produces `0.25`, which one decimal
renders as **`0.3`** — so the water goal would have read `0.3 / 4 L` after a 250 ml tap, a number the
app invented, on the one screen this ticket exists to fix. And `String.format` without an explicit
`Locale` follows the *default* locale, so the separator and even the digits moved with the device.
Now: round to three decimals, strip trailing zeros, no locale involved.

## Two defects found by looking, not by testing

Both suites were green before either was found. `kb/dev/look-at-your-own-output.md` is the rule and
this is two more instances of it.

1. **The buttons wrapped `3 + 1`.** Legible, and it reads as a layout accident rather than a
   decision, which §0.8 makes a defect in its own right. Fixed to a 2×2 block at equal width, which
   is stable at every screen width and every word length and degrades honestly (three rungs give
   `2 + 1`). Found by opening the PNG.
2. **An `enabled` gate would have disabled the row offline after one tap.** The obvious shape —
   disable while a write is in flight — is wrong here and wrong in the direction that destroys the
   feature. `Observed:` by reading `ProgressRepositoryImpl.logProgress` during the pre-commit
   self-review: it ends in `ref.set(dto).await()`, and a Firestore write task resolves on **server
   ack**, not on the cache write. So the first tap would have disabled all four buttons until the
   network answered — on a control whose entire premise is *"I can tap it more than once"* (`R25`),
   on the surface §5.3 says must work offline. The `enabled` parameter is **deleted** rather than
   defaulted, and the test that asserted the disable is inverted into one asserting it can never
   happen.

## What is deliberately not here

- **`C22` [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44)'s measure proposal** —
  the dashed square marker and the on-goal offer. `#11` makes the absence it offers to fill; the
  offer is its own ticket.
- **§4.6's entry edit history, soft delete and optional duration row** — `C6`
  [#22](https://github.com/idomarhaim/Android_Final_Project/issues/22). The brief names them as
  context and warns against re-simplifying them; nothing here touches them, and nothing here makes
  them harder.
- **Sub-unit prefixes.** §1.3 writes the example as `[250 ml] [500 ml] [750 ml] [1 L]`; the app
  renders `0.25 L · 0.5 L · 0.75 L · 1 L` — **the same amounts in the goal's own word**. Spelling
  `0.25 L` as `250 ml` requires knowing the user's word means litres, and §1.3 makes that word user
  content. The only two bridges are a unit table keyed on that text (the string matching this ticket
  forbids) or a dimensional model (§1.3 rejects it outright: *"buys conversions this app never
  performs"*). **This is a deviation from the ticket's literal wording and is flagged as one**, not
  slipped through.
- **§7.1's other new fields** — `start`, `declaredBy`, `parentIds`, `goalEdges`, `completionFacts`.
  Other tickets.
- **The AI half of §1.3** — *do buttons fit, and what is counted* (§3.3 E `measure`). The app half is
  what `#11` owns and the model still authors no numbers.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **459 / 0** — `+37` on the 422 at branch point. `FillLadderTest` (16), `GoalMeasureMigrationTest` (21). |
| **Instrumented** | **91 / 0** on `Pixel_10_Pro_XL_B`, `+9` on the 82 at branch point. `FillButtonRowUiTest` (8), `WaterGoalRenderTest` (1). |
| **Render pass** | **Looked at**, twice — the `3 + 1` wrap was found on the first look and the fix confirmed on the second. Artifact committed: [`docs/render-passes/2026-08-20-11-fill-buttons/water-goal.png`](../../docs/render-passes/2026-08-20-11-fill-buttons/water-goal.png). |
| **Security rules** | **Not applicable** — no `firestore.rules` change. `measureKind`/`measureWord`/`inputMode` are fields on `users/{uid}/goals`, already covered by the owner-only `users/{uid}/{document=**}` match. |
| **Cloud Functions** | **Not applicable, and not silently** — `functions/` has no test layer at all (spec §7.2 records this). The only change reaching it is `RecommendationRepositoryImpl` sending `measureWord` on the unchanged `unit` wire key. |

`FillLadderTest` is written mostly as *what the ladder refuses to do*, and `GoalMeasureMigrationTest`
mostly as *what the migration refuses to classify* — including nine words a string matcher would
"obviously" get right (`litres`, `L`, `km`, `kms`, `Litres`, `ק״מ`, `reps`, `₪`, `%`). A mapper that
classified them would pass every happy-path test anyone would think to write.

The render pass ran through `am instrument` rather than `connectedDebugAndroidTest`, because the
Gradle task **uninstalls the app when it finishes** and takes its external files dir — and the PNG —
with it. `Observed:` the first attempt pulled nothing and `/data/data` held no `goalpilot` package.

## ⚠️ The brief's independence claim was false, and `9-duration-box` is why this is recorded here

`sessions/11-fill-buttons.md` asserted *"Independent of `C20` and of #9 — verified 2026-08-20: no
file touching `Goal.unit` also touches `TaskEstimate`."* That is **true as stated and useless as a
conflict test**, and `9-duration-box` (`48e94bc`) found it the hard way: it opened, found this
session's live claim on five of the files it needed, and built nothing.

The grep asked *does one file mention both symbols today* — a question about the code as it stands.
A conflict is about the **edits a ticket is about to make**: `#11` added a per-goal input mode to
`GoalDetailScreen.kt` and `#9`'s duration box lives in the same file, and neither symbol ever had to
appear beside the other. **`TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` Wave 3 was right** —
`#6 → #7 → #9 → #11`, one working set, strictly sequential — and `505f083` overturned it on the
symbol grep. The roadmap stands; the brief has been struck and corrected on its way to
`sessions/done/`.

The general finding is already in the KB: `9-duration-box` ingested it as
`kb/dev/agent-topology-and-routing.md` § *The disjointness check has to be run at the granularity the
claim is made in*.

## 📋 Foreign commit carried by this push

`48e94bc` — `9-duration-box: blocked at kickoff`. It is in `git log @{u}..HEAD` and goes up with this
push. Adjudicated per the push precondition rather than assumed:

| Check | Result |
|---|---|
| Live row in `## 🔒 Active claims` | none — that session wrote no row here (it says so, and why) |
| Their paths dirty or staged | no — `sessions/9-duration-box.md`, `CHANGELOG/2026-08-20/9-duration-box.md` all clean |
| Their last commit | `48e94bc`, 2026-08-20 06:58:55 +0300 — its own wrap-up |
| Their last transcript turn | `2026-08-20T04:00:44Z`, a closing 🚦 status block reporting the session blocked and finished |

Released and quiet on both readings, so it rides along. It touches no file under `app/`.

## 📥 KB

**Nothing ingested, and one candidate is owed.** The `enabled`-gate defect is a general shape worth a
page — *a write task that resolves on server ack must not gate a control whose premise is repetition*
— and it is a sibling of `50-finish`'s finding about `runTransaction` and the offline cache. It is
written to `kb-candidates/2026-08-20-11-fill-buttons.md` rather than drained here, because it wants
to be merged into an existing page rather than appended blind.

**Two pre-existing candidate files are still parked and were NOT touched:**
`kb-candidates/2026-08-19-50-offline-stamps.md` and `kb-candidates/2026-08-20-48-settings-surface.md`.
Both are partly drained and what remains in each is **`rules/`-destined**, which is always-ask in
both modes — `AUTO MODE` does not open that gate. They are reported, not drained.

## Files

- **New:** `domain/model/Measure.kt`, `domain/model/FillLadder.kt`,
  `feature/goals/FillButtonRow.kt`, `feature/goals/GoalMeasureStrings.kt`,
  `app/src/test/.../domain/FillLadderTest.kt`, `app/src/test/.../data/GoalMeasureMigrationTest.kt`,
  `app/src/androidTest/.../ui/FillButtonRowUiTest.kt`,
  `app/src/androidTest/.../ui/WaterGoalRenderTest.kt`,
  `docs/render-passes/2026-08-20-11-fill-buttons/water-goal.png`, this file,
  `kb-candidates/2026-08-20-11-fill-buttons.md`
- **Edited:** `domain/model/Goal.kt`, `data/firestore/dto/Dtos.kt`, `data/firestore/dto/Mappers.kt`,
  `data/remote/RecommendationRepositoryImpl.kt`, `domain/usecase/BuildHealthProposalsUseCase.kt`,
  `domain/usecase/BuildWidgetSnapshotUseCase.kt`, `domain/usecase/SyncHealthDataUseCase.kt`,
  `feature/goals/AddEditGoalScreen.kt`, `feature/goals/AddEditGoalViewModel.kt`,
  `feature/goals/GoalDetailScreen.kt`, `feature/goals/GoalDetailViewModel.kt`,
  `feature/lifeareas/LifeAreaDetailScreen.kt`, `ui/components/GoalCard.kt`, four existing test
  suites, `SESSIONS.md`, `sessions/11-fill-buttons.md` → `sessions/done/`
- **Not touched:** `firestore.rules`, `functions/`, `TODO/`, the two parked candidate files.
