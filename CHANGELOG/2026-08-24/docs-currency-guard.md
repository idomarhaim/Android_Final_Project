# docs-currency-guard — 2026-08-24

**The docs are assertions about this code, and nothing re-ran them.** Audited all six files
under `docs/` against `HEAD`, then built the guard that makes the drift fail a build.

## 🔍 What the audit found

`docs/ARCHITECTURE.md` had not been touched since 2026-08-04 — twenty days and roughly fifteen
tickets — and carried about fifteen false claims:

| Claim | Reality |
|---|---|
| "Completing a task is a Firestore transaction … all atomically" | `#55` deleted it; `setDone` banks one `completionFacts` doc |
| `publicProfiles` schema lists `level` | §5.2 deleted the field |
| "Points/level are written client-side … production would compute them in a trigger" | That trigger shipped: `functions/src/projection.ts` |
| "All three callables" | Four — `proposeMeasure` was missing |
| Bottom bar "Home / Goals / Social / Profile" | Home / Goals / **Calendar** / Social |
| Data model omits `completionFacts`, `occurrences`, `summaries`, `challengeReports`, `participants` | All exist |
| "Life areas are not reorderable in the UI" | Contradicted 150 lines earlier **in the same file** |
| AI described as GROQ-only via one function | Four providers, user's own encrypted key (`#54`) |

`docs/OPERATIONS.md` states **92 JVM + 12 instrumented** tests against a tree holding
**1084 + 319**, and contradicts itself on Health Connect in two adjacent sections (§1 says
shipped ✅, §3 says "is a stub"). `docs/SETUP.md`, `docs/RELEASING.md` and `README.md` all told
a reader to export `JAVA_HOME` to `jdk-21.0.11.10-hotspot` — **not merely stale**: it is the
known wreck with an orphaned `lib/` and no `bin/java.exe`, so following the instruction reads as
a broken machine rather than a stale document.

`docs/PRODUCT_v0.3.md`, `docs/CLOUD-DEVICE.md` and `docs/RELEASING.md` are otherwise current.

## 🛠️ What shipped

**`DocsCurrencyTest`** — four assertions, each against an enumeration the document *explicitly
makes*: every `onCall` export is named in ARCHITECTURE, every `FirestorePaths` collection is
named in its data-model block, every `TopLevelTab` label is named in its navigation section, and
every Adoptium path quoted in any document equals the `gradle.properties` pin. Plus a
not-vacuous case.

**Four `inputs` declarations** in `app/build.gradle.kts` beside `ReleaseNotesGuardTest`'s, for
`docs/`, `README.md`, `functions/src/index.ts` and `gradle.properties`.

**The false sentences repaired** — the eight the guard asserts on, plus the three that were
flatly wrong and were sitting in blocks being edited anyway (the transaction, the client-side
points limitation, the un-reorderable life areas). **Deliberately NOT done:** authoring the
missing sections (settings, tutorial, widget, calendar, notifications, the multi-provider AI
story). That is a rewrite and it is Ido's call.

## 🎬 Walkthrough — waived, mechanical half run

Ido answered `waive` on 2026-08-24. The judgment half is settled; the mechanical half was run
and **it found a defect in the draft**:

- **Ran the six drafted assertions against every recorded instance** (the fifteen audit
  findings), checking both directions. **A5 reported `silent` on the three documents carrying
  the dead JDK path** — a false negative that reads exactly like a pass. Cause: in a regex
  character class `[\/]` collapses to `[/]`, the backslash being eaten as an escape, so it
  matched only the forward-slash spellings. Same family as the `local.properties` escape trap
  and the `--`-in-XML-comment trap this project already documents.
- **Two assertions were dropped because the run condemned them.** *feature packages* is weak in
  both directions (fired on `social`, went silent on `health`). *test counts* changes on every
  commit that adds a test — guarding it taxes the commonest action in the repo to protect a
  number nobody decides from; the right fix is deleting the number.
- **`/adversarial-review` §1** killed the JDK check as drafted ("does this directory exist"
  passes only on Ido's machine, so on CI it is skipped or deleted within a week) and it was
  reformulated as agreement with the `gradle.properties` pin.
- **What the fallback could not test:** it was run by the session that wrote the draft, against
  a corpus that same session assembled. A fresh-context agent attacking it would be the right
  instrument and was not used — the 🧩 gate needs asking first, and `waive` does not grant it.

## 🧪 Tests

- **JVM unit — 1089 tests, 0 failures** (1084 + this guard's 5). `:app:testDebugUnitTest`, green.
- **Red-first:** the guard failed 4 of 5 on arrival, each naming exactly the right drift
  (`[completionFacts, occurrences, summaries, challengeReports]`, `[proposeMeasure]`,
  `[Calendar]`, the three JDK paths). Green after the repair.
- **Mutation check — the one that matters.** Before: `Task :app:testDebugUnitTest FROM-CACHE`,
  BUILD SUCCESSFUL. After a **docs-only** edit (stripping `completionFacts` from
  ARCHITECTURE.md): the task **re-ran** and BUILD FAILED. So the `inputs` declarations work and
  the guard cannot report green from cache on the edit it exists to catch — the exact failure
  `ReleaseNotesGuardTest` carried unnoticed for three days.
  - A **first** mutation attempt was inconclusive and is recorded rather than discarded: it
    removed `Calendar` from the nav line, but the repair footnote added directly below still
    contained the word, so the assertion passed correctly. Whole-document substring presence is
    a weak oracle; the mutation has to remove *every* occurrence.
- **Instrumented / rules / functions layers: not run.** Nothing here touches app code, security
  rules or the functions runtime — the diff is one new JVM test, four Gradle `inputs` lines and
  prose.

## ⚠️ What this guard does not cover

Every assertion is a **presence check over an enumeration**. It catches the *omission* half of
the drift and none of the *false-assertion* half — of the fifteen findings it would have caught
eight, and the ones that actually mislead a reader are sentences that are simply wrong. **A green
run does not mean the docs are current**, and the class KDoc says so where somebody will read it.

## 🧭 Sibling sessions

`tour-refresh` was live when this session claimed and released mid-flight (`37794f5`); its
`docs/RELEASING.md` edit and the Gradle daemon were both left alone until then, and the daemon
was taken only after the release. Its commit `4ddbced` **published this session's claim row**
before this session could commit it — the inbound half of the shared-file hazard, which nothing
on this side prevents. Nothing was lost.

---

## 📥 KB ingest — 2026-08-24, on Ido's instruction ("verify first it harms nothing")

**The safety check he asked for, before anything was written:**

| Check | Result |
|---|---|
| JARVIS tree | clean but for `kb/stale-pages.base`, whose diff is **empty** — a line-ending artifact, independently re-confirmed |
| JARVIS live claims | **none**; repo in sync with its remote |
| Both target pages exist | yes |
| Does any entry **supersede** a standing claim? | **One nearly did** — see below |

**The one that mattered.** `kb/dev/product-copy-describes-code.md` §2 states *"no test layer holds
this"* — written **one day earlier** by `tour-refresh`. Entry 2 argued documentation *is* partly
testable and shipped a test that proves it. Read carelessly that supersedes a standing claim, which
is **always-ask in both modes**.

It does not, and the reading is the point: §2's claim is about **prose semantics** (does this
sentence match the layout it describes), and the guard checks **enumerations** (is every collection
in the code named in the block that lists collections). Both agree prose is uncheckable. So §7 was
written as a **bounded extension** with §2 left **verbatim**, and it states explicitly why it must
not be generalised back to guided tours. **Nothing was overwritten, so nothing needed asking.**

**Ingested:**

- 📥 **regex class eats the backslash; a wrong width passes** → `kb/dev/look-at-your-own-output.md`
  §4k instance (c)
- 📥 **documentation is code's product copy; the enumeration slice is checkable** →
  `kb/dev/product-copy-describes-code.md` §7
- 📥 **Kotlin block comments nest, so `/*` in a KDoc breaks the file** → this repo's `CLAUDE.md`

`Check-KbLinks` **CLEAN — 118 pages.** JARVIS commit `84eacd0`.

**Entry 4 stays held** — whether `docs/` should grow the sections it lacks entirely is Ido's, and
the candidate file keeps it under `## Standing — always-ask` with its original number.

## 😅 The trap bit the ingest that was documenting it

Writing the candidate file's tie line, `kb\log\2026-08-24.md` inside a `python - <<'PY'` heredoc
lost a backslash level; Python read `\202` as an **octal escape**; the path silently became
`log6-08-24.md` carrying an invisible control character. Same family as the entry being ingested
one line above it. **Caught by scanning every file written this turn for control characters (0
after the fix), not by reading them** — the repo's own pre-commit hook would have caught it a step
later, which is the layer working as designed.

## 📌 Not mine, and flagged rather than touched

`kb-candidates/2026-08-24-tour-refresh.md` is still undrained. Its entries 1 and 2 read
`Status: ready`, while that session's **journal entry and board note both call them
held/always-ask**. `/kb-ingest` step 1 takes the *file* as the source, so the next drain would
ingest them without asking. Another session's file — reported, not edited.
