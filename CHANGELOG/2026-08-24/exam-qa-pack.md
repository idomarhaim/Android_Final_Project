# exam-qa-pack — 2026-08-24

> **Summary:** A 96-question examiner Q&A pack for the final-project defence, in **three** builds from one content source: a Word file, a searchable study HTML, and — round 3 — a **glance card** for answering live while presenting (keyword bullets, never sentences to read aloud). Also 6 real node-and-arrow SVG architecture/flow diagrams and a gallery that makes them findable. The headless-Edge render pass is now standing procedure and caught four more layout defects plus a duplicate-SVG-id bug.

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

---

# Round 2 — the visual layer *(same session, Ido 2026-08-24)*

> *"אני רוצה שבקובץ HTML יהיה יותר גרפים וויזואליזציה אייקונים ותמונות"* — more charts,
> visualisation, icons and images, **in the HTML file**. The `.docx` was not in scope and its
> layout is unchanged.

## What was added

| | |
|---|---|
| **8 inline-SVG / CSS diagrams** | the layer stack · the Firestore model · unidirectional data flow · the LLM path with its fallback · the points write **before/after** · the Activity lifecycle · the two navigation graphs · the feature tiers |
| **3 charts** | test methods by layer (1,093 / 319 / 53) · the level curve `50·(n−1)·n` over L1–L10 · questions per section |
| **12 section icons** | stroke SVG on `currentColor`, in both the sidebar and each `h2` |
| **18 real app screenshots** | from `docs/render-passes/`, downscaled to webp data URIs — a 6-up hero filmstrip, an 8-up appearance grid (Aurora/Blossom × glass/liquid/neo/dark-neo), and 5 attached to the questions they answer |
| **6 stat tiles**, a **click-to-enlarge lightbox**, and a **hover tooltip on every plotted mark** | |

**The file stays self-contained** — 180 KB → **692 KB**, no external request, still openable from a
USB stick with no network. Images are Pillow-downscaled to 520px (250px for the grid) at webp q74:
**246 KB of pixels for 18 screenshots**, where the originals total 3.9 MB.

**Charts follow `C:\Dev\JARVIS\skills\dataviz`.** One measure → one hue for every bar (never a
value-ramp on categories), ≤24px marks with 4px rounded data-ends, hairline solid grid, values in
text tokens rather than the series colour, no legend where there is one series. The palette is the
reference instance **validated against this page's own surfaces** rather than the skill's defaults:
`validate_palette.js "#2a78d6,#eb6834,#1baf7a" --surface "#ffffff"` and the dark trio against
`#161a21` — all checks PASS, with one WARN (aqua at 2.82:1 on white) whose documented mitigation is
a visible label, which every use of it carries. The architecture layers deliberately take **ordinal
steps of one hue** rather than categorical slots, because they are ordered and a categorical set
would assert they are unordered peers.

**Figures are bound to questions by a distinctive substring, and the build asserts each key matches
exactly one question.** Reword a question and the build fails rather than silently dropping its
diagram.

## 🧪 Tests

**Still no project test layer in the diff** — `docs/exam-prep/` plus this file and the board.
`:app:testDebugUnitTest` was **not run**: the Gradle daemon is claimed by `docs-repair` and
`62-tour-video-v2` announced taking it for an `assembleDebug`.

**63 automated checks (`verify.py`), all green** — 35 from round 1 plus 28 new ones over the visual
layer: 3 SVG charts present, every plotted mark carrying a tooltip, no bar value label overflowing
its `viewBox`, 22 screenshots all with `alt` and intrinsic `width`/`height`, every image a webp data
URI, 33 inline icons, the sidebar figure marker and the card figure marker agreeing, and every
element the JS targets existing in the DOM. **The `95` literals were replaced by a count derived from
the content** — a guard that reddens on every legitimate edit gets its number bumped without being
read, which is how it stops guarding.

### ✅ The visual gap from round 1 is CLOSED — and it found four defects

Round 1 shipped with one open issue: *neither file has been looked at.* This machine has Edge, so
`--headless=new --screenshot` closes it, and **`look-at-your-own-output.md` §1 is vindicated
immediately** — 35 structural checks had passed over every one of these:

1. **An inline `<svg>` with no width/height fills its container.** `.ic` was sized only inside
   `h2`/`h3`/`nav`, so the demo card's play icon rendered as a **~900px circle** occupying a whole
   screen. Nothing in the markup is wrong; nothing in a DOM assertion can see it.
2. **`_axis_ticks` stopped at the last tick *below* the max**, so the axis was shorter than the
   data: the 1,093 bar was scaled against 900 and ran a fifth of its length past the plot, taking
   its value label off the edge, and the line chart's 4,500 endpoint sat above the top gridline with
   its label clipped away. **Both charts looked entirely plausible** — that is the whole hazard.
3. **Five of twelve category labels were cropped** by a fixed 168px left gutter
   (*"he product…"*, *"ase, the data model…"*), which reads as a typo rather than as a layout fault.
   The gutter is now measured from the longest label.
4. **The hero filmstrip wrapped 5 + 1** at 1280px, and captures of wildly different aspect ratios
   made a ragged row. Fixed image height plus an explicit 6-column grid.

Defects 2 and 3 are `anti-patterns.md` entries by name — *a label clipped by its own mark*, and an
axis that does not contain its data. Both are now **regression-guarded**: the verifier unit-tests
`_axis_ticks` over nine maxima and asserts no `bval` label exceeds its `viewBox`.

**The instrument needed its own fix, which is §4 of that same page.** Capturing the real page at a
`#q7` fragment returned a **blank 7 KB image** three times — the scroll races the screenshot — and
three identical file sizes were the tell. The working instrument is the skill's own prescription: a
**probe page** rendering only the figures, seven of them, captured at 1000×1400 in light **and**
dark. Reading a blank capture as *"the diagrams are fine"* was one careless glance away.

**Rendered and looked at, in both themes:** all 8 diagrams, all 3 charts, the appearance grid and
the hero strip, plus the full page top. The `.docx` was re-opened in Word — **28 pages, 11,945
words** (up 201 from the new question).

## One question added, and it is not decoration

**Q4, *"How do points and levels work?"*** — the level curve needed a home, and re-reading the pack
for where to put it surfaced a real gap: points and levels are a **core requirement** with no
question of their own. It carries the derive-don't-store rule and the one-sentence answer to *why
store `points` and not `level`*.

---

# Round 3 — real flow diagrams, and a second file for answering live

> *"חסר לי HTML גרפים של ארכיטקטורות, פלואים וכו'. אני צריך שתהיה גרסה של הקובץ הזאת שהיא יותר
> פשוטה — תוך כדי שאני מציג והמרצים שואלים, שבהינף מבט אני אוכל לראות את התשובה ולענות. אני לא
> אמור להקריא משפטים שלמים מילה במילה מהדף."*

Two asks, and the second is a **different document**, not a mode: revising and answering-under-fire
want opposite things from the same content.

## 1 · Real node-and-arrow diagrams

Round 2's diagrams were **CSS boxes**, which is right for a tree or a table and cannot draw a
*flow* — an arrow that forks, rejoins or loops back has no CSS form, so those could only ever be a
column of cards. `flows.py` adds a small SVG primitive (nodes on a grid, arrows with heads, elbow
and go-around connectors, bands, notes) and six diagrams built on it:

| | |
|---|---|
| **Module dependency graph** | the one picture that answers *describe the architecture*, because the **arrows are the claim**: everything points inward at `domain/`, nothing points out |
| **One screen's data pipeline** | Firestore → repository → ViewModel → screen, with the event edge returning underneath |
| **The auth gate** | one Flow, three states, two graphs |
| **Ticking a task** | 7 steps, offline-first, with the local half separated from the server half |
| **Smart add** | the AI flow *and its failure branch* — the fork that made a real diagram necessary |
| **Build and release** | Kotlin → R8 → keystore → App Distribution → testers |

**And a gallery at the top of the study file**, because the complaint was that the diagrams were
*missing* when 8 of them were already there — buried in a 96-question scroll. It lists every
figure-bearing question as a jump link and sits directly under the stat tiles.

## 2 · `GoalPilot-Glance-Card.html` — the live-answer build

**A separate file, deliberately.** The study build is prose you read beforehand. This one is for the
moment someone has just asked you something and you need the shape of the answer in one look, then
your own words.

- **96 cards**, one per question, **395 keyword bullets** — none over 95 characters.
- **66 cards carry one quotable line** (the green `"` block): the soundbite worth saying close to
  verbatim, if you want one. The rest of the card is triggers, not sentences.
- **Nothing collapses.** A click is a delay, so there is no `<details>` anywhere.
- Two columns, big type, sticky search bar, **section chips**, `/` to search, **`1`–`9` to jump**,
  `Esc` to reset. All 10 diagrams sit at the top under their own chip.
- 381 KB, self-contained, still openable from a USB stick with no network.

**The house rule for writing a bullet is in `glance.py`'s docstring**, because the compression *is*
the product: a bullet is a trigger of ≤ ~8 words; numbers beat adjectives, so every measured one
survives; and never a bullet that only makes sense if you already read the prose.

**Coverage is asserted both ways** — every question has a card and every card a question — so a
reworded question fails the build instead of silently losing its glance answer.

## 🧪 Tests

**No project test layer in the diff** — `docs/exam-prep/` plus this file and the board.
`:app:testDebugUnitTest` **not run**: the Gradle daemon is claimed by `docs-repair`, and
`62-tour-assembly` holds the emulator and `adb`.

**85 automated checks (`verify.py`), all green** — up from 63, with 20 new ones over the glance
build. The one worth naming is a guard on **the property that document exists for**: bullet length.
`0 of 395` over 95 characters, ≤ 3 % allowed. A card whose bullets drift back into prose has quietly
become the study build again, and nothing else would notice.

### The render pass earned its place a second time — four more defects, and one that only luck hid

1. **Diagrams rendered a full screen each** in the glance build — a 700-unit SVG stretched to a
   1440 px column. That is the opposite of a glance document. Two-up and capped.
2. **`_axis_ticks`-class bug in `elbow`:** `via` sat *outside* the two x coordinates, so the last
   leg **doubled back** and the arrowhead pointed away from the box it was entering.
3. **The state-pipeline return edge ran straight through two boxes**, label and all, because
   `elbow` routes at the nodes' own y. Added `updown`, which goes *around* the row.
4. **Three descriptor labels were faked with spaces inside one centred string** and rendered as a
   run-on. They are now three positioned labels, moved above the row — below, the ViewModel's label
   sat exactly where the return edge drops out of that same box.

**And the one the verifier caught rather than the eye — every diagram emitted `<marker id="ah">`.**
Six diagrams meant **six duplicate ids in one document**, and `url(#ah)` resolves to the *first*
match, so every arrowhead in the file was diagram one's. It rendered perfectly **because the markers
happened to be identical** — it would have broken silently the moment one differed, and the ids were
invalid either way. Marker ids are now per-canvas, and the verifier asserts uniqueness in both
files.

That is the same shape as round 2's `{document=**}`: *correct output for the wrong reason*, which no
amount of looking finds.

**Rendered and looked at:** the glance build top, its card grid and its dark mode; all six new
diagrams light and dark; the study file's new gallery. The `.docx` re-opened in Word — **28 pages,
11,945 words**, unchanged, as Ido scoped it.

## Two files now, and which to open when

| | |
|---|---|
| `GoalPilot-Examiner-QA.html` | **before** — read it, follow the reasoning, print it |
| `GoalPilot-Glance-Card.html` | **during** — search, glance, answer in your own words |
| `GoalPilot-Examiner-QA.docx` | the printable/annotatable copy of the first |
