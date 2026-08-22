# `53-material-naming` — 2026-08-22

> **Summary:** `#53`'s **last** item, and the ticket is now closed. §4.1 calls the four materials
> *glassmorphism · liquid glass · neo · dark neo*; the picker called two of them **Soft** and **Soft
> dark**, and the word "neo" appeared **nowhere in the UI** — so a user who had read the spec could
> not find the control and a session receiving *"no dark blue neo"* could not match the report to a
> tile. Both halves had already happened on this ticket. Fixed by the **join**, not by a rename:
> every tile now carries §4.1's own name in a caption under its label, and §4.1 carries the mapping
> table. The guard is the part worth keeping — `MaterialVocabularyTest` **parses the table out of
> `PRODUCT_v0.3.md`** and compares it to the resources and the wiring, so the two vocabularies cannot
> drift apart again silently, which is exactly how this defect was born. Both mutations tried against
> it failed the build. JVM **752/0** across 70 classes (+6 tests, +1 class), instrumented
> `MaterialPickerUiTest` **10/0** (+3), two frames. **Ido's open question is still open and did not
> block this** — the closing comment says so.

---

## 1. The defect, and why it was `#53`'s rather than cosmetic

From `#53`'s own **2026-08-21** comment, which filed a second gap in the ticket's own deliverable:

| §4.1's name | `AppMaterial` | the picker said |
|---|---|---|
| Glassmorphism | `GLASS` | **Glass** |
| Liquid glass | `LIQUID_GLASS` | **Liquid glass** |
| Neo | `NEO` | **Soft** |
| Dark neo | `DARK_NEO` | **Soft dark** |

Every other artefact in the project — the spec, the briefs, the changelogs, the render-pass
filenames, the issue itself — says *dark neo*. The UI said it nowhere. §4.9's rule is that a picker
has to **say** things, and §0.3's is that a control that changes nothing is a defect; a control that
cannot be **named** is present, working, and unreportable, which is the same failure one step over.

**The rejected fix was renaming the tiles to *Neo* / *Dark neo*** — option 1 in that comment, and it
is rejected for the reason the comment gives: *Soft* / *Soft dark* are the better **user-facing**
words. The failure was never that the wrong vocabulary won. It was that neither reached the other.

---

## 2. What shipped

### 2a. The caption, on every tile

`ui/components/ComponentStrings.kt` gains `AppMaterial.specName()`; `MaterialPicker` renders it in
`labelSmall`/`onSurfaceVariant` between the label and the lock badge, tagged `materialSpec_<id>`.

**Visible, not a `contentDescription`** — and the brief allowed either. The failure `#53` filed is
that *a reader of the spec cannot find the control*; a description nobody sees answers only the
screen-reader half of that. Being a `Text` it lands in the semantics tree anyway, so one line covers
both, and `MaterialPickerUiTest.theSpecNameIsVisible_notOnlySpoken` is what keeps the two apart.

**Unconditional, including liquid glass**, where the two vocabularies coincide and the caption
therefore restates the label. A caption that appeared only where the words differ would be
unlearnable: a reader could not tell *"the same"* from *"not stated"*. The repetition is the honest
report of a genuine coincidence.

### 2b. The mapping table, in §4.1

Beside the material table, with the fourth column naming the exact string a tile renders — because
that column is what the guard below reads.

### 2c. The strings — and the one decision in this unit that is not obvious

| Key | `values/` | `values-iw/` |
|---|---|---|
| `components_material_spec_name` | `Spec: %1$s` | `במפרט: %1$s` |
| `components_material_{glass,liquid,neo,darkneo}_spec` | `Glassmorphism` · `Liquid glass` · `Neo` · `Dark neo`, all `translatable="false"` | *(none — untranslatable)* |

**The four names are `translatable="false"` on purpose, and that is the opposite call from the skin
names two lines above them in the same file.** Their job is to be **the same token** as the design of
record, which is written in English — ניאו would name the control after a word that appears in no
document, which is this ticket's failure *translated*, not a translation of the fix. *Aurora* and
*Blossom* are evocative product words whose job is to read well, so leaving them Latin would put a
bare Latin run in an otherwise-Hebrew list for no gain (§4.8); a designation has no such gain to
forfeit. The discriminator is **what the word is for**, not the idiom, and it is written into both
resource files so the next editor does not have to re-derive it.

The **frame** around the name *is* translated, and the name is bidi-isolated at the call site — a
Latin run in an RTL paragraph is reordered exactly as `5/10` becomes `10/5`, and the resource cannot
carry an isolate, so it goes where `GoalCard` puts the one on a user-authored measure unit.

`Decision taken per` §0.8's suspension in [AGENTS.md](../../AGENTS.md) (English is what "seen"
means for now) and per `HebrewLocaleResourceTest`'s own KDoc, which names `translatable="false"` as
the mechanism for a string that carries words and is still language-independent. Not asked, logged
here.

---

## 3. The guard, which is the part that outlives the fix

`MaterialVocabularyTest` (new, 6 tests, `app/src/test/.../resources/`).

**A join is correct the day it is written and rots silently afterwards, because nothing fails when
it drifts** — the doc still renders, the picker still renders, and they simply stop agreeing. That
is precisely how this defect was born. So the table in §4.1 is not documentation *of* the strings,
it is the **input** to the test: the test parses `docs/PRODUCT_v0.3.md`, and a copy of the four names
living in the test file would have guarded the strings against *itself*.

It checks five things and states the sixth as weak:

1. one table row per `AppMaterial`, no more and no fewer;
2. each row's picker-label column equals `components_material_<id>`;
3. each row's spec-name column equals `components_material_<id>_spec`, **and** the frame formatted
   with it equals the row's fourth column — i.e. the caption a tile actually renders;
4. `ComponentStrings.specNameRes` maps each material to **its own** key (a swapped pair renders four
   plausible captions and no other test notices);
5. the four names keep `translatable="false"` and the frame does not — the decision above guarded
   rather than only documented, since losing the attribute makes `HebrewLocaleResourceTest` demand
   the very translation this unit argues against;
6. a source grep that the caption is still wired into `MaterialPicker` — **stated as weak in the
   test itself**, because it is a grep and not a frame.

### The instrument, checked on the two inputs it exists for

Per `kb/dev/look-at-your-own-output.md`: a guard that cannot fail is not a guard, and "the suite is
green" is exactly what it says either way.

| Mutation | Result |
|---|---|
| `components_material_neo_spec` → `Neon` (resource drifts from the table) | **FAILED** — `each tile's spec line is the name the table says it is`, `MaterialVocabularyTest.kt:115` |
| `NEO`/`DARK_NEO` swapped in `specNameRes` (wiring drifts) | **FAILED** — `ComponentStrings wires each material to its own spec name` |

Both reverted; the suite is green on the real files.

---

## 4. 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **752 / 0** across **70** classes — +6 tests, +1 class (`MaterialVocabularyTest`). Was 746/0 across 69 at `70922d7`. |
| **Instrumented** (`MaterialPickerUiTest`, via `adb install -r` + `am instrument`) | **10 / 0** — +3: `everyTileNamesItselfInTheSpecsVocabulary`, `theSpecNameIsVisible_notOnlySpoken`, `theSpecNameSurvivesTheLanguageSwitch_andItsLatinRunIsIsolated` |
| **Render pass** (`MaterialRenderPass.aurora_everyMaterialInBothBrightnesses`) | **1 / 0**, 8 frames written; **2 committed** as evidence |
| **Build** | `:app:assembleDebug` + `:app:assembleDebugAndroidTest` green |
| **Rules / Firestore** | Untouched — this unit reaches no rule, no document and no function. |

The three JVM classes the brief predicted would have an opinion all passed unchanged:
`HebrewLocaleResourceTest` (the `translatable="false"` attribute is what keeps it quiet),
`ComponentsLocaleTest` (instrumented, and unaffected — it renders `GoalCard` and `SkinPicker`), and
`AnalyticsLiteralSweepTest` (every new word went to `res/`; nothing was added to `SWEPT_PACKAGES`,
per AGENTS.md's explicit "do not add your package here as a favour").

📱 **Device:** `emulator-5554`, free on the board since `57d-entrance-animation` released it.
**No sign-in was needed and none was destroyed** — the `adb install -r` + `am instrument` path was
used throughout, never `connectedDebugAndroidTest`, so the app's Firebase store was preserved
(`kb/dev/android-device-verification.md` §8).

---

## 5. What the frames answered that no assertion could

[`docs/render-passes/2026-08-22-53-material-naming/`](../../docs/render-passes/2026-08-22-53-material-naming/)
— two frames and a README saying what each is evidence of.

1. **Every caption fits on one line at half-tile width.** `Spec: Glassmorphism` is the longest and it
   fits. A wrapped designation would have read as a broken label, and nothing below the render layer
   could have told me.
2. **The lock word survives being pushed down a line.** Dark neo's tile now reads *Soft dark · Spec:
   Dark neo · **Dark only** · Charcoal, with one bright accent*. §4.9 needs the lock to be a **word
   on the tile**; it still is, still bold, and the order now groups naming above behaviour, which is
   the right hierarchy. This was the one thing I would have shipped wrong without looking.
3. **Legible on dark neo's charcoal**, in `onSurfaceVariant` — the contrast case a light-page frame
   cannot answer.

`Untested:` how the caption **looks** in Hebrew. `AppLanguage.OFFERED` does not carry `HEBREW` and
`MaterialRenderPass` pins `AppLanguage.ENGLISH`, so no RTL frame exists. The Hebrew half is asserted
mechanically instead (frame translates · name does not · isolate present). What would check it is one
render method walking `AppLanguage.HEBREW`, owed when `#51` resumes.

---

## 6. Ido's open question — still open, and it did not block this

`#53`'s 2026-08-21 comment records one thing only Ido can answer: whether *"no dark blue neo"* on
`v0.3.0` meant

- **the name** — he could not find the control, because the UI never said *neo*. Then this unit is
  the whole fix.
- **the ground** — he found it and expected the surface to be blue rather than charcoal. Then this
  unit is **still correct and still closes the naming gap**, and a **separate** ticket is owed
  against §4.1's material table, which specifies *"charcoal groove … one cyan→blue accent"*.

No answer arrived during this session. `#53` is closed on the naming half with that stated on the
ticket, per the brief's explicit instruction — the question routes to a **new spec ticket** if the
answer is "the ground", and nothing about it belongs in `#53`, whose scope is the material contract
and its controls.

**Why `#53` closed despite its own last comment saying *"it cannot be closed without Ido"*.** That
comment and the brief were written by the same session, `53-tag-sweep`, and the brief is the later
of the two (`a454bb8` after `5db32c1`): it scopes the closure to the naming gap and routes the ground
question elsewhere. `/kickoff` §5 step 4's last check — *read your own `Exit` against what you
actually built* — passes: every line of the `Exit` landed, and the open question is not remaining
work on this ticket but a fork in a different one.

---

## 7. Board

`sessions/` and `sessions/done/` grepped for `issue: 53` before closing: `53-material-naming` (this
one), and `c12-material-contract` + `53-tag-sweep`, both already `done`. Nothing else is `ready` or
`active`, so the *nothing left → close it* branch applies.

`owns` was amended twice mid-session and both amendments are on the board and in the brief:
`MaterialPickerUiTest.kt` (the instrumented layer had to assert the caption renders) and
`docs/render-passes/2026-08-22-53-material-naming/` (this session's own new directory).
Zero live sibling rows throughout — counted mechanically, since the Active-claims section is ~4,200
lines of appended release notes around a table that held only its header.

---

## 8. 📥 KB drain — two findings, both into existing central-KB pages

Late by a commit rather than skipped: the drain is owed **at** the commit trigger and rode nowhere,
so it went out as its own unit with the candidate file written first, exactly as the rule intends —
a session that dies mid-ingest loses nothing.

📥 **Ingested:** a vocabulary join with a **running product** on one side → `kb/dev/spec-table-vs-vocabulary.md` **§6**
📥 **Ingested:** what a word is **for** decides whether it translates → `kb/dev/untranslatable-idioms.md` **§9**

Both are **updates in place**. Neither creates a page, neither supersedes a standing claim and
neither is `rules/`-destined, so no always-ask exclusion fired and both drained under `AUTO MODE`.
Journal: `kb/log/2026-08-22.md`, naming this repo's candidate file — the only tie that survives the
usual case of a candidate in one repo and its pages in another. `Check-KbLinks` **clean**, 110 pages.

Cross-repo, so it owes a row on **both** boards, and it had one: `C:\Dev\JARVIS` claim `4e05295`,
pages `e42a68c`, released `84ec3c8` (pushed); this repo's re-claim `ccb927e`.

**On the destination search, because it is the part that could have gone wrong.** The candidate's
entries carried a `Destination` but **no bundle-check field**, which is the *missing* signal rather
than an explicit `not checked` — nobody had considered it. It turned out confirming anyway, but the
useful half is how the page was found: the concept-level greps (*drift apart* · *two vocabularies* ·
*guard against itself*) returned **nothing**, and only a search by the phenomenon's **other names**
(`vocabulary|nomenclature|naming gap|designation`) surfaced `spec-table-vs-vocabulary.md`. A
destination search run at the wrong width does not fail — it passes, and reports that nothing covers
this. Two new duplicate pages is what the narrow search would have produced.

⚠️ **Noted in passing, not fixed:** `kb/stale-pages.base` in `C:\Dev\JARVIS` is dirty with an
**empty** diff — LF/CRLF only, so it is not a sibling's unpublished work and §5.4's *advance it or
message the owner* does not apply. Left untouched and recorded on that board so the next arrival does
not re-derive it.
