# exam-qa-pack — 2026-08-24

> **Summary:** A 95-question examiner Q&A pack for the final-project defence, as a Word file and a self-contained searchable HTML file in `docs/exam-prep/`. Every answer is drawn from `HEAD` — the code, the rules file, the manifest and the build script — not from the product plan, and the `.docx` was verified by opening it in Word rather than by reading the bytes back.

## What Ido asked for

> *"I need a WORD file and an HTML file of questions and answers the examiners could ask me
> about the project. So I can both study for the exam and use it in a pinch while presenting."*

Two audiences for one artefact: revision beforehand, and lookup under pressure. That split is
what decided the format of each half rather than producing the same document twice.

## What shipped

| File | Size | For |
|---|---|---|
| `docs/exam-prep/GoalPilot-Examiner-QA.docx` | 28 pages, 11,744 words | Reading end to end, printing, annotating |
| `docs/exam-prep/GoalPilot-Examiner-QA.html` | 180 KB, self-contained | Lookup mid-presentation — search, collapse, jump |

**95 questions across 12 sections**: the product · architecture and layering · Compose and the UI ·
state, coroutines and Flow · Hilt · Firebase, the data model and offline · security, rules and
permissions · the AI integration · Android platform fundamentals · testing · build and release ·
the hard questions.

Plus three pieces of front matter that are not Q&A and are the part most likely to be used live: a
60-second pitch, a **3-minute demo running order** with the sentence to say at each step and a
failure playbook, and a *numbers to have ready* table (SDK levels, dependency versions, test counts,
the level curve).

## The two halves are shaped differently on purpose

- **The `.docx` is linear.** Cover, contents, front matter, then Q1–Q95 in order, each question a
  bold heading and each answer body prose. It is for reading, and for printing.
- **The `.html` is an index.** A sticky sidebar listing all 95 questions, `/`-to-focus search that
  filters cards *and* the sidebar *and* hides sections that empty out, expand/collapse all, a
  light/dark toggle that persists, and a print stylesheet that force-opens every collapsed card.
  Under exam pressure the operation is *find*, not *read*.

Both come from one content source (`content1..3.py`) through two renderers, so a wording fix cannot
land in one and miss the other.

## Where the answers came from

`HEAD`, read directly, not `docs/`. Concretely: `app/build.gradle.kts` and
`gradle/libs.versions.toml` (SDK levels, R8, the version catalog, the signing fallback, and the
`inputs.dir` block), `AndroidManifest.xml` (permissions, `singleTop`, the `<activity-alias>`, the
`<queries>` block, the five widget receivers), `firestore.rules` (`serverOwns`, the participants
subcollection), `MainActivity.kt`, `Destinations.kt`, `User.kt`/`Leveling`, `Resource.kt`,
`DashboardViewModel.kt`, `functions/src/`, and the four test trees for their counts.

`docs/ARCHITECTURE.md` was used as a **map**, not as a source — `docs-repair` is live in the same
repo and that file is known to be one package behind at the time of writing (eleven named, twelve
present). The pack says **twelve** and names `sync/`, per that session's board note.

**Test counts are measured, not quoted:** `grep -c '@Test'` over each tree gives 1,093 JVM / 319
instrumented, and `firestore-tests` holds 53. `docs-repair` deleted the prose counts from `docs/` on
Ido's instruction; these are re-derived here rather than copied from anywhere.

## Editorial decisions worth recording

**Answers carry the failures, not just the design.** The strongest exam material in this project is
where something was measured and then changed: the 7.9-second offline transaction that became a
single idempotent write; `animateFloatAsState` initialising *at* its target so a chart that never
changed never animated; `BarItem` carrying a `countSuffix: String` because two identical lambdas are
not `equals`; repositories built on `uidFlow()` because reading `auth.currentUser` once serves user
A's goals to user B. Each is a question in its own right.

**The best answer in the pack is about a test that did not run.** Guard tests that read files with
`java.io.File` were `UP-TO-DATE`-skipped by Gradle because a runtime file read is not a declared
task input — so the guards were green having executed nothing, in the flattering direction, on
exactly the commit shape they existed to catch. It is Q86, and it is the "what did you learn"
answer.

**Twenty-one questions carry a *Going deeper* note** — the follow-up an examiner asks next, or the
trap in the question. Rendered as a tinted aside in both formats so it never reads as part of the
answer.

**English, per the standing language rule**, even though the defence will be in Hebrew. Android
vocabulary is English regardless, and the whole repo, the spec and the app are English. Said in the
reply as mine to overturn; a Hebrew edition is a re-render of the same content source, not a rewrite.

**Hebrew being switched off is answered head-on** (Q92), because an examiner reading the README will
see Hebrew and RTL claimed and then meet an English app. Better volunteered than caught.

## 🧪 Tests

**No project test layer is touched by this change** — the diff is `docs/exam-prep/` plus this file
and the board. No Kotlin, no rules, no functions, no resources. `DocsCurrencyTest` reads `docs/`, but
its five regexes assert on callable names, collection names, bottom-bar tabs and the JDK path in the
files it names; a new subdirectory adds none of those tokens and removes none.

⚠️ **`:app:testDebugUnitTest` was NOT run, and this is a deliberate hold, not an oversight.** The
Gradle daemon was released by `s25-layout-and-tour` to `docs-repair`, which is **live** and holds it
for exactly the verification its own row describes. Taking it would have interrupted a session
mid-unit for a prose-only diff.

**What WAS run, against the artefacts themselves rather than against the source that produced them:**

- **The `.docx` was opened in Microsoft Word** through COM — `Documents.Open` succeeded and reported
  **28 pages / 11,744 words / 458 paragraphs**. That is the real consumer, and it is the check that
  matters: python-docx will happily write a file Word refuses.
- **35 automated checks (`verify.py`), all green**, re-reading both files through their consumers —
  python-docx for the `.docx`, `html.parser` for the HTML. Both artefacts reopened; 95 questions
  present and numbered 1..95 with no gaps in each; every one of the 95 sidebar anchors resolves to a
  real id; tag nesting balanced with no unclosed tags; all ids unique; no external URL anywhere
  (self-contained); light palette on bare `:root` with dark redefined under both the media query and
  the explicit `[data-theme]`; print stylesheet present; every search payload non-trivial,
  lowercased and markdown-free.

**Three defects the verifier caught that reading would not have:**

1. **`w:pPr` children were being appended out of schema order.** `paragraph_format` writes `w:ind`
   and `w:spacing`; appending `w:shd` and `w:pBdr` *after* them produces a document Word rejects as
   *"unreadable content"*, and **python-docx writes it without complaint**. Fixed by inserting each
   element before its own successors in the `_tag_seq` order; the verifier now walks every `w:pPr`
   in the body and asserts the order, which is the check the library itself cannot make.
2. **Nested inline markup was rendering literally.** `` **`feature/`** `` left visible backticks
   inside a bold run — 66 of them across the document — because the single-pass alternation matched
   bold first and never re-parsed its own body.
3. **A code span containing `**` broke the emphasis parser.** `` `{document=**}` `` — the Firestore
   recursive wildcard — was read as an emphasis delimiter, and the rules answer came out as
   `` `users/{uid}` plus `{document=}** - read and write only if isOwner(uid)` ``: mangled, and
   perfectly plausible-looking. Both are fixed by stashing code spans *before* parsing emphasis and
   recursing for nested styles.

The third one is the argument for the whole exercise. It renders as a well-formed sentence, so
proof-reading finds it only by chance — and it is in the answer about security rules, which is the
one an examiner is most likely to press on.

⚠️ **`unverified`, and named rather than left implied: neither file has been LOOKED AT.** There is
no browser and no renderer available in this session, so the HTML's *appearance* — sidebar
proportions, card rhythm, dark-mode contrast, print pagination — is asserted structurally and has
not been seen. The `.docx` has been opened by Word but not read on screen; 28 pages at 11,744 words
is a plausible density, not a verified layout. That is `look-at-your-own-output.md`'s visual clause,
owed and unpaid. **Next:** Ido opens both, or a session with a render surface takes a pass.

## Board

Claimed as `exam-qa-pack`, owning `docs/exam-prep/`, this file, and
`kb-candidates/2026-08-24-exam-qa-pack.md`. No singletons — no Gradle, no device, no emulator, which
is why this could run beside `docs-repair` at all. `docs/exam-prep/` is a new directory and
intersects nothing on the board: `docs-repair` owns six named files under `docs/` and `README.md`,
none of them here.
