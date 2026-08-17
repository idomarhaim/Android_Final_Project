# `51e-sweep-components` — 2026-08-17

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** `#51`'s literal sweep for `ui/components/` — 18 keys, 7 defects — plus two holes found in the sweep guard itself: a false positive that fired precisely on the remedy, and a complement test hardcoded to one package that reported green for a package it never read.

`/implement` [`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) — the literal
sweep, **one package: `ui/components/`** · branch `feat/goalpilot-implementation` · mode `AUTO MODE`

Ido's constraint: this package **before any remaining feature package**, because its components are
shared by eight screens, so a literal here would otherwise be fixed eight times. `51b`'s pattern is
the template; `kb/dev/untranslatable-idioms.md` is what the work actually is.

## 1 · The premise was half right, and the half that was wrong changed the unit

**`EmptyState`, `LoadingBox` and every chart in this package hold no user-facing literals at all.**
They take their copy as **parameters** — `EmptyState(title:, subtitle:)`, `BarItem(label:,
trailing:)`, `DonutChart(contentDescription:)` — so their words belong to the eight callers and are
each caller's own sweep. Sweeping this package does **not** translate the eight screens' empty
states.

What *is* genuinely shared, and what this unit turned out to be about, is copy hanging off **two
domain enums** that several packages render:

| Source | Strings | Rendered by |
|---|---|---|
| `GoalCategory.label` | 10 | `ui/components/GoalCard.kt` **+ 3 unswept feature call sites** |
| `AppSkin.label` / `.tagline` | 4 | `ui/components/SkinPicker.kt` only |

That is the fourth idiom in `untranslatable-idioms.md` §1 — *a language switch cannot reach a
constructor argument* — and it is where the fix-once-not-eight-times argument actually cashes out:
ten category labels authored once in Hebrew instead of once per feature sweep.

**The unit was not widened into any feature package.** It did reach two files in `domain/model/`
(`AppSkin.kt`, `Goal.kt`) — unavoidably, since removing speech from an enum is where this idiom's fix
lives — plus the three test files that assert on those two enums. Those paths were **missing from
this session's original board claim**, which listed `ui/components/`, `res/` and the guard; the
release note below carries the corrected list. The finding is reported so the "shared components"
rationale is not carried into the next package on a premise that does not hold there either.

## 2 · The two enums got different answers, and the discriminator is not the idiom

The idiom is identical in both. What differs is **who else reads it**:

- **`AppSkin`** — one production consumer, in this package. The copy is **gone from the enum**; the
  persisted `id` stays and is now the only instance member.
- **`GoalCategory`** — three more consumers, in `feature/dashboard` and `feature/goals`, and **those
  packages are unswept**. Deleting `label` would drag two feature packages into this unit half-swept.
  So `localizedLabel()` is the replacement, `label` stays with a KDoc pointer, and it goes with the
  last feature sweep that stops reading it.

`AppSkinTest`'s `every skin has picker copy` — which asserted the *opposite* of what is now wanted,
and was green the entire time the picker was untranslatable — is replaced by a reflection assertion
that the enum declares **only** `id`. It guards the shape rather than the two old names, because the
failure mode is a *third* string field, not a restored pair.

## 3 · What shipped

**18 keys**, `res/values/components_strings.xml` + `res/values-iw/components_strings.xml`. Sibling
file per package, following `analytics_strings.xml`; the parity test pairs **by name**.

**Seven** defects fixed, by class — counted off the table below, not from prose. (The first draft of
this sentence said *six* above a seven-row table, which is `untranslatable-idioms.md` §3's own
finding landing on the changelog that cites it.)

| # | Site | Class |
|---|---|---|
| 1 | `GoalCard` `"Untitled goal"` | literal |
| 2 | `GoalCard` `"Goal complete"` (TalkBack) | literal |
| 3 | `GoalCard` `goal.category.label` ×2 | idiom 4 — speech on a domain type |
| 4 | `GoalCard` `"${goal.progressPercent}%"` | bidi — digits + sign |
| 5 | `GoalCard` meta line | idiom 1 — **fragment concatenation** + bidi |
| 6 | `SimpleBarChart` count/trailing labels | bidi |
| 7 | `SkinPicker` `skin.label` / `.tagline` | idiom 4 |

**The meta line is the one worth reading.** It was
`"${category.label} • ${current}/${target} ${unit}"` — four pieces glued in Kotlin, which no resource
file can reorder. It is now one resource with three arguments, and each argument was read **against
the code that fills it** (`untranslatable-idioms.md` §5), which is what decides isolation:

- `%1$s` category label — app speech, from `res/`.
- `%2$s` the `current/target` ratio — **one** isolate, not two. Isolating the numbers separately
  would still let the `/` migrate.
- `%3$s` the unit — **user-authored** (`"workouts"`, `"hours"`, the `"%"` default). Never translated
  (§8: content never moves) and isolated because its script is unknown at authorship.

`SimpleBarChart` isolates `trailing` **in the component rather than at each caller**, which is the
one place the shared-component argument genuinely paid: `"75%"` and `"3h 20m"` contain no strong
directional character, so the bidi algorithm resolves them from the surrounding paragraph.

> `Inferred:` that the **unisolated** form renders `%75` in Hebrew — from the Unicode bidi algorithm
> (digits are `EN`, `%` is `ET`; with no strong character the run takes the paragraph's direction) and
> from `core/util/Bidi.kt`'s own account of the same defect. **Not observed**: seeing it would mean
> removing the isolate and re-rendering, which this session did not do. What **was** observed is the
> isolated form rendering correctly in Hebrew (§6). `Untested:` a before/after capture of one bar
> label with the isolate removed would close it.

### Terminology

**יעד, never מטרה** (§5.1 / `E1`). `components_goal_untitled` is `יעד ללא שם`,
`components_goal_complete` is `היעד הושלם`.

Note what is **absent** as a consequence, per `untranslatable-idioms.md` §4: Hebrew has one word for
both `Goal` the entity and `target` the number, so once the entity takes יעד there is nothing left
for the number. `components_goal_meta` never needs that noun — it states the ratio and the unit and
names neither. That is the §4 remedy (restructure so the missing noun is never needed) rather than
coining `היעד המספרי`.

Skin names are **translated, not transliterated**: `זוהר` and `פריחה` are the ordinary Hebrew words,
where `אורורה` / `בלוסום` would be a calque that parses and nobody says. Leaving them Latin would put
a bare Latin run inside an otherwise Hebrew list — §4.8's problem for no gain. The persisted ids are
unchanged, so no stored preference moves.

## 4 · A hole in the shared guard, found by being caught in it

Adding `ui/components` to `SWEPT_PACKAGES` left the guard **red on two literals the sweep had already
fixed**:

```
GoalCard.kt: "${goal.currentValue.trimNumber()}/${goal.targetValue.trimNumber()}"
SimpleBarChart.kt: "${Math.round(it * progress)}${item.countSuffix}"
```

Neither carries a word. `isProse` counted alphabetic runs over the raw literal, so the **identifiers
inside `${…}`** — `currentValue`, `trimNumber` — read as prose. That is a false positive **that fires
precisely on the remedy**, which is the shape that gets a guard routed around rather than obeyed.

Fixed by stripping interpolations (brace-matched, so a lambda inside one does not end it early)
before counting. It does not weaken the check — copy *between* interpolations survives, so
`"Completed ${n} of ${m} tasks"` still counts three words and still fails.

**The instrument was then checked on the hardest inputs it exists for**, in both directions, because
loosening a predicate is exactly how a guard stops firing while every other input keeps saying it
works: `the prose rule fires on copy and stays silent on code` asserts five copy strings are caught
**and** eight code/punctuation strings are not.

**A second hole, same file, no false positive to announce it.** The complement test
(`the swept package resolves its words through resources`) was hardcoded to `feature/analytics`.
Adding a package to `SWEPT_PACKAGES` extended the offender scan automatically and left this half
silently covering one package — a guard that grows on one side and not the other reports green for a
package it never read. It now loops `SWEPT_PACKAGES` against a `RESOURCE_FLOOR` map, with a third
test failing if a swept package has no floor declared.

`AnalyticsLiteralSweepTest` keeps its name though it now guards two packages; the KDoc says so. A
rename buys a tidier name for a fistful of stale pointers in the issue, the changelog and
`AnalyticsStrings.kt`.

## 5 · The guard was proven by a break that **compiles**

`51d`'s lesson, applied: *a guard proven only by a break that does not compile has proven nothing.*
A `private val REINTRODUCED_DEFECT = "Try again later"` was added to `IconChip.kt` — valid Kotlin,
compiles clean — and the guard went red naming the exact file and literal:

```
IconChip.kt: "Try again later"
```

Reverted; suite green again.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **358 / 0** (`:app:testDebugUnitTest`) — was 356 before this session |
| **Instrumented** | **70 / 0** (`:app:connectedDebugAndroidTest`, `Pixel_10_Pro_XL`, API 37) — was 63 |
| **Build** | `:app:assembleDebug` green |
| **Server unit / integration / endpoints / database** | **layer does not exist** — this is a client-only Android app; no server module |
| **`firestore-tests/`** | **not run** — no rules, DTO or Firestore path touched |

No `--rerun-tasks` needed (`ced0561` declared `res/` an input to every `Test` task).

New: **`ComponentsLocaleTest`** (7 instrumented) renders the swept components in Hebrew and reads the
composed output off the semantics tree. It exists because the two JVM guards are both satisfied by a
sweep that moved every string to `res/` and then wired the wrong key, or that isolated nothing.

It re-asserts `51d`'s rule at this layer — `aMirroredCardIsNotEvidenceOfATranslatedOne` measures
direction **and** language off one frame — and adds this package's own version of the same trap: a
card can be in flawless Hebrew and still render `10/5`, which is not a missing translation and which
no amount of reading the resource file shows.

**Two pre-existing tests were invalidated deliberately.** `AnimatedBarChartUiTest` asserted
`onNodeWithText("75%")`; the rendered label now carries the isolate marks. Updated to
`"75%".bidiIsolated()` **rather than to a substring match**, and the KDoc says why: an expectation
spelled without the marks passes on the *unfixed* output, so relaxing it is how the fix gets reverted
by someone making a red test green. `bars_withoutACountUpShowTheirTrailingLabelVerbatim` was renamed
— "verbatim" is now false.

## 6 · Rendered and looked at, because §0.8 requires it

*"A design is not finished until it has been seen in Hebrew."* The assertions above prove the isolate
marks are in the right **positions**; they do not prove the glyphs come out in the right **order**,
which is a claim about the platform's bidi implementation and was `Inferred:` until it was looked at.

`Observed:` 2026-08-17, `Pixel_10_Pro_XL` API 37, both languages captured to PNG:

- Hebrew — `יעד ללא שם` · `לימודים • 5/10 hours` renders **`5/10`, not `10/5`** · `50%`, not `%50` ·
  `כספים • 250/1000 ₪` correct · skin tiles `זוהר` / `פריחה` with the selected tile correctly on the
  **right**.
- English — byte-identical to before: `Learning • 5/10 hours`, `Aurora`, `Ocean blue & evergreen`.
  The isolates are zero-width and invisible, which is what stops the fix becoming an artefact.
- User-authored Hebrew goal titles (`ריצה`, `חיסכון`) render as typed in the **English** UI —
  §8's *content never moves*, confirmed rather than assumed.

The capture harness was a throwaway and is **not** committed; it was created, run and deleted in this
session.

## 6a · ⚠️ The `#51` comment is OWED — GitHub outage, not a skip

**`gh issue comment 51` failed with HTTP 503 twice** (2026-08-17, after the commit). By then
`gh issue view 51` was failing too, though it had **succeeded at the start of this session** — so the
outage widened from writes to reads while the unit was being worked.

**Nobody should assume this was posted.** The full comment body is preserved verbatim in the
**appendix at the foot of this file**; posting it is a paste of everything below the horizontal rule,
not a rewrite. The next session in this repo that finds GitHub healthy should post it and replace
this section with the comment link.

*(It was briefly a sibling file, `51e-sweep-components.issue-comment.md`. `changelog-index-backfill`'s
new pre-commit hook rejected it — `CHANGELOG/<day>/` is one file per session and the generated index
owes each one a row, so a sidecar has no row and fails the gate. The hook was right and the file is
now an appendix. Recorded because the gate landed **between** this session's two commits: the first
predates the hook's installation.)*

Not retried further, per Ido's instruction for this outage: record it rather than looping or skipping
silently.

## 7 · What #51 still owes

`#51` stays **OPEN**. Unswept: `feature/dashboard`, `feature/goals`, `feature/lifeareas`,
`feature/challenges`, `feature/social`, `feature/health`, `feature/profile`, `feature/auth`.

Two things the next sweeper inherits from this unit:

1. **`GoalCategory.localizedLabel()` already exists** — the ten Hebrew labels are authored. A feature
   sweep switches its call sites to it rather than re-translating; `GoalCategory.label` is deleted by
   whichever sweep removes the last reader.
2. **`51d`'s dialog constraint still binds.** Turning a `Text("Cancel")` into
   `Text(stringResource(…))` inside an unwrapped `AlertDialog` reintroduces the composition defect
   and looks perfect in an English render. Use the `App*` wrappers.

---

## Appendix · the unposted `#51` comment, verbatim

Kept inline rather than as a sibling file: `CHANGELOG/<day>/` is one file per session and the
generated index owes each one a row, so a sidecar breaks `changelog-index-backfill`'s convention
and its pre-commit hook (which is how this was found). Posting it is a paste of everything below
this line.

## `ui/components/` swept — `51e-sweep-components`, 2026-08-17

The shared package, taken before any remaining feature package. **18 keys**,
`res/values/components_strings.xml` + `values-iw/`. Commit `bc5ef69`.
Account: `CHANGELOG/2026-08-17/51e-sweep-components.md`.

**JVM unit 358 / 0 · instrumented 70 / 0 · `assembleDebug` green.**

### The sequencing rationale was half wrong, and it matters for the next package

The argument for doing this package first was that its components are used by eight
screens, so a literal here would otherwise be fixed eight times. **`EmptyState`,
`LoadingBox`, `DonutChart`, `StackedColumnChart` and `SimpleBarChart` hold no
user-facing literals at all** — they take their copy as *parameters*, so every word
belongs to the eight callers and is each caller's own sweep. Sweeping this package did
**not** translate the eight screens' empty states.

A well-factored presentational component is *defined* by taking its copy from outside,
so "shared UI package" and "shared copy" pull in opposite directions. The saving lives
where a value is **constructed** or where a **transformation** is applied — never where
a string passes through as a parameter.

What was genuinely shared, and what this unit turned out to be about:

| Source | Strings | Rendered by |
|---|---|---|
| `GoalCategory.label` | 10 | `ui/components/GoalCard.kt` **+ 3 unswept feature call sites** |
| `AppSkin.label` / `.tagline` | 4 | `ui/components/SkinPicker.kt` only |

Plus direction-isolation of caller-supplied strings: `SimpleBarChart` isolating
`trailing` fixes eight callers at once.

### The two enums got different answers, and the idiom is not the discriminator

Both are §1's fourth idiom (*speech on a domain type*). What differs is **who else reads
the property**:

- **`AppSkin`** — one production consumer, so the copy is **gone from the enum**; only the
  persisted `id` remains.
- **`GoalCategory`** — three more, in `feature/dashboard` and `feature/goals`, **unswept**.
  `label` stays behind a KDoc pointer; `localizedLabel()` is the replacement. Deleting it
  would drag two feature packages in half-swept.

**The Hebrew is authored once either way** — only the English call sites migrate later.

### For whoever takes the next package

1. **`GoalCategory.localizedLabel()` already exists and all ten Hebrew labels are
   authored.** Switch call sites to it; do not re-translate. `GoalCategory.label` is
   deleted by whichever sweep removes its last reader.
2. **`SimpleBarChart` now isolates `trailing`,** so bar labels render with isolate marks.
   An instrumented expectation must be spelled `"75%".bidiIsolated()` — one spelled
   without the marks passes on the *unfixed* output, which is how this gets reverted by
   someone making a red test green.
3. **`51d`'s dialog constraint still binds.** `Text(stringResource(…))` inside an
   unwrapped `AlertDialog` reintroduces the composition defect and looks perfect in
   English. Use the `App*` wrappers.

### Terminology

**יעד, never מטרה.** And note what is *absent* as a consequence — Hebrew has one word for
both the entity and its number, so `components_goal_meta` never names the target at all.
That is §4's remedy (restructure so the missing noun is never needed) rather than coining
`היעד המספרי`.

Skin names are **translated, not transliterated**: `זוהר` / `פריחה`. Leaving them Latin
would put a bare Latin run inside an otherwise Hebrew list. Persisted ids unchanged.

### Two holes found in the sweep guard itself

1. **A false positive that fires precisely on the remedy.** `isProse` counted alphabetic
   runs over the raw literal, so identifiers inside `${…}` read as prose — it flagged two
   literals the sweep had *just fixed*. That shape is what gets a guard routed around
   rather than obeyed. Fixed by stripping interpolations; copy *between* them survives.
   The loosened predicate is asserted **in both directions** on the inputs that motivated
   it, because the silent half cannot be faked.
2. **A guard that grew on one side only.** The complement test was hardcoded to
   `feature/analytics`, so adding a package extended the offender scan automatically and
   left this half reporting green for a package it never read. Now loops `SWEPT_PACKAGES`
   against a `RESOURCE_FLOOR` map.

Guard proven by a break that **compiles** (`51d`'s lesson): a valid
`private val REINTRODUCED_DEFECT = "Try again later"` went red naming the exact file.

### Rendered and looked at (§0.8)

`Observed:` 2026-08-17, `Pixel_10_Pro_XL` API 37, both languages captured to PNG. Hebrew
renders **`5/10` not `10/5`**, `50%` not `%50`, skin tiles with the selected tile
correctly on the right. English byte-identical to before — the isolates are zero-width.
User-authored Hebrew goal titles render as typed in the **English** UI, confirming §8's
*content never moves* rather than assuming it.

`Inferred:`, not observed — that the *unisolated* form renders `%75`. Seeing it would mean
removing the isolate and re-rendering, which this session did not do.

### Still owed

Unswept: `dashboard`, `goals`, `lifeareas`, `challenges`, `social`, `health`, `profile`,
`auth`.
