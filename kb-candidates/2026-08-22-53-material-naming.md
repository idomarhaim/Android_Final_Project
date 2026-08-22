# KB candidates — `53-material-naming`, 2026-08-22

Source unit: `b3dbba3` (GoalPilot `#53`, the material picker's naming gap).
Written before the drain so a session that dies mid-ingest loses nothing.

---

## 1 · A vocabulary join between a DOCUMENT and a PRODUCT rots with nothing failing

**Claim.** When a design of record names a thing one way and the shipped UI names it
another, both sides are individually correct and **nothing fails when they diverge** — the
doc still renders, the product still runs, and they simply stop agreeing. The cost is
borne by two people neither artefact can see: a **user** who read the document and cannot
find the control, and a **maintainer** who receives a bug report and cannot match it to a
control by name. The remedy is a **join in both directions** (a mapping table in the
document, the document's name carried visibly in the UI) plus a guard — and the guard's
**input must be the document itself**. A test carrying its own copy of the names guards the
strings against *itself* and passes forever while the document says something else.

**Why.** `#53` shipped a four-material picker whose tiles read *Glass · Liquid glass · Soft
· Soft dark* while `PRODUCT_v0.3` §4.1 called the same four *glassmorphism · liquid glass ·
neo · dark neo*. The word "neo" appeared **nowhere in the UI**. Both failure halves then
actually happened *on that ticket*: Ido reported *"no dark blue neo"* against a control the
UI never calls neo, and the session receiving it spent a render pass establishing there was
no defect at all. **Rejected:** renaming the tiles to the document's words — the
user-facing words were the better ones and the failure was never that the wrong vocabulary
won, it was that neither reached the other. **Also rejected:** carrying the name only in
`contentDescription`, which answers the screen-reader half and leaves the sighted reader
exactly where they were.

**Destination.** `kb/dev/spec-table-vs-vocabulary.md` — **extend**, do not create. That page
covers the spec-**internal** case (§2.2's cases vs §2.3's names, both inside one document,
GoalPilot `#56`). This is the same family with one side outside the document, and it adds a
remedy that page does not carry: where the guard's input comes from.

**Anchors.** `docs/PRODUCT_v0.3.md` §4.1 mapping table ·
`app/src/test/java/com/idomarhaim/goalpilot/resources/MaterialVocabularyTest.kt` ·
`CHANGELOG/2026-08-22/53-material-naming.md` §3 · issue `#53`'s 2026-08-21 comment.

**Supersedes.** Nothing. Pure addition.

**Status.** Ready to ingest.

---

## 2 · What a word is FOR decides whether it translates — the `translatable="false"` discriminator

**Claim.** Two strings can be the same *idiom* (a proper-ish noun on a picker tile) and take
**opposite** localization decisions, and the discriminator is not the idiom, the part of
speech, or whether it "looks like a brand". It is **what the word is for**. A word whose job
is to **read well** is translated — leaving it Latin puts a bare Latin run in an RTL list
for no gain. A word whose job is to be **the same token as an external document** must
**not** be: translating it names the control after a word that appears in no document, which
is the reportability failure *translated*, not a translation of the fix. `translatable="false"`
is the mechanism for the second, and the frame around such a word is still authored per
language, with the foreign-script run **bidi-isolated at the call site** (a resource cannot
carry an isolate).

**Why.** The two cases sit **four lines apart in one file** in GoalPilot's
`res/values/components_strings.xml`: `components_skin_aurora` is translated to `זוהר`
(deliberately, per that file's own comment — §4.8's bare-Latin-run problem), while
`components_material_neo_spec` is `Neo` and untranslatable. Reading the idiom alone gives
the wrong answer for one of them whichever way you read it. **Rejected:** transliterating
(`ניאו`) — it parses, nobody says it, and it appears in no document, so it defeats the
purpose the string exists for. **Rejected:** omitting the string in Hebrew — the *frame* is
genuinely language-dependent and a missing key falls back to English silently, which
`HebrewLocaleResourceTest` exists to catch.

**Destination.** `kb/dev/untranslatable-idioms.md` — **extend** with a new numbered section.
Its §4 is already "a sixth idiom, of **vocabulary**", but that one is about a *source*
distinguishing two terms the *target* has one word for. This is the orthogonal question of
whether a given string is owed a translation at all.

**Anchors.** `res/values/components_strings.xml` (the skin block and the material-spec block)
· `ui/components/ComponentStrings.kt` `specName()` ·
`MaterialVocabularyTest.the four spec names are marked untranslatable and the frame is not` ·
`MaterialPickerUiTest.theSpecNameSurvivesTheLanguageSwitch_andItsLatinRunIsIsolated`.

**Supersedes.** Nothing. Pure addition.

**Status.** Ready to ingest.
