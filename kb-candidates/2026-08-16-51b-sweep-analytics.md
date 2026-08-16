# KB candidates — session `51b-sweep-analytics` (2026-08-16)

Issue #51, the literal sweep, first package. One file per session; each entry
stands alone, because no other agent can read this session's transcript.

---

## 1 · A localization sweep is an event, not a state — so it needs a guard per swept unit

- **Claim.** When moving hardcoded UI strings into resources package by package,
  the parity check between `values/` and the translated bucket is **not enough**.
  It only sees keys that reached `res/`. A literal that never left the Kotlin file
  has no key, so it is invisible to parity — and the package silently un-sweeps
  itself the first time somebody adds `Text("Try again")`. Each swept unit
  therefore needs its **own** guard asserting no user-facing prose literal remains,
  plus a list of which units are swept.

- **Why it matters.** Every other signal points the wrong way: the build succeeds,
  the English render is perfect, review sees an ordinary one-line addition, and the
  only person who can see the defect is a reader of the other language, who is not
  reviewing the diff. The failure is also **silently partial** — the screen is 95%
  translated, which reads as finished rather than broken.

- **How to define "prose" so the guard is usable.** *Two or more alphabetic words
  of at least two letters.* Deliberately crude: it ignores format patterns
  (`%1$s %2$s`), separators (`", "`), keys (`"__unassigned__"`) and single
  technical tokens. A precise rule would have to know which argument of which
  composable reaches a screen, and **a guard nobody can predict is a guard people
  route around**. Comments and raw strings are stripped first, or KDoc prose trips it.

- **The list is a record of progress, not an exemption list.** A package absent
  from `SWEPT_PACKAGES` is *unswept*. Inverting that — an opt-out list — makes the
  default "clean", which is exactly wrong for an incremental sweep.

- **Verified against its own fault:** injecting `Text("Asking the AI now")` failed
  the guard and named the file and the literal; restoring made it pass.

- **Destination.** `kb/dev/` — the i18n/resource-testing page from
  `2026-08-16-51-hebrew-rtl.md` entry 1, as a second section.
- **Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/resources/AnalyticsLiteralSweepTest.kt`;
  `AppLocaleInstrumentedTest.OWNED_PREFIXES`.
- **Supersedes.** Nothing. **Extends** the parity-guard entry in the sibling file.
- **Status.** Pending — `kb/`-destined, cross-repo (`C:\Dev\JARVIS`).

---

## 2 · Four things that make UI copy untranslatable, and all four look like good Kotlin

- **Claim.** Localizing a screen is mostly *not* moving strings. It is undoing four
  idioms that are idiomatic in a monolingual codebase and structurally
  untranslatable:

  1. **Fragment concatenation.** `buildString { append("N tasks"); append(" of M");
     append(" — run it again") }`. Word order is a property of the language; the
     pieces do not reorder. Remedy: one **complete sentence per situation**, even
     when that multiplies the strings (four here, for partial × unanswered).
  2. **Plural rules written in Kotlin.** `if (n == 1) "" else "s"` encodes English.
     Hebrew has one/two/many/other; Arabic six. Remedy: `<plurals>`.
  3. **Case transformations on translated text.** `label.lowercase()` for a
     mid-sentence noun. Case is a property of *English*: Hebrew has none, and
     Turkish's dotted/dotless `i` makes the transformation wrong rather than
     merely useless. Remedy: a **separate resource** for the inline form. That the
     two are identical in the other language is the point, not waste.
  4. **Speech stored on a domain/core type.** `enum AnalyticsRange(val label:
     "Day")`. A language switch cannot reach a constructor argument. Remedy: the
     enum keeps identity, a UI-side mapper resolves the resource — the same split
     as `GoalCategory.iconKey` → `iconForKey`.

- **Why worth a page.** Each is invisible in a monolingual review, and each is
  *more* idiomatic than its remedy, so they are re-introduced by good engineers
  acting reasonably. They also cannot be caught by a parity check — every one of
  them produces correct English and complete resources.

- **The fifth, adjacent:** a **ViewModel holding user-facing English**
  (`"Updated $n task durations"`). Emit a typed message and resolve it in the view;
  that is also what lets the count be direction-isolated and the plural rule live
  per language.

- **Destination.** `kb/dev/` — new page, e.g. `kb/dev/untranslatable-idioms.md`.
- **Anchors.** `feature/analytics/AnalyticsStrings.kt`, `AnalyticsScreen.backfillIntro`,
  `AnalyticsMessage` in `AnalyticsViewModel.kt`, `core/util/AnalyticsRange.kt` KDoc,
  `res/values/analytics_strings.xml` header.
- **Supersedes.** Nothing.
- **Status.** Pending — `kb/`-destined, cross-repo.

---

## 3 · Hebrew wording rules that are code concerns, not translator preferences

- **Claim.** Two Hebrew wording decisions must be made by whoever writes the
  *resource*, because they are caused by the rendering algorithm rather than by
  style, and a translator working from a spreadsheet cannot see them:

  1. **Never attach a Hebrew prefix to a Latin or digit run.** `ה‑AI` renders as
     `AI‑ה` — the prefix lands on the far side of the run. Remedy: choose wording
     with no attachment (`הבינה המלאכותית` for "the AI"), or a standalone word
     before the run (`אל Google Tasks`). This extends to format arguments:
     `ב%1$s` is a defect whenever `%1$s` may hold digits, so
     `בטווח הנוכחי (%1$s)` is used instead.
  2. **Directional glyphs flip.** A before/after arrow is `→` in English and `←`
     in Hebrew, because the claim reads the other way. Same for any glyph whose
     meaning is *direction* rather than *shape*.

- **Why it matters.** Both are invisible in source and in an English render, and
  both produce text that is *grammatical* — so proofreading does not catch them
  either. They are caught only by rendering in Hebrew, which is §0.8's rule.

- **Observed contradiction worth recording.** `res/values-iw/widget_strings.xml`
  (session `widget-pack`) contains **both** defects: `ל־%1$d` attaches a prefix to
  a digit run, and six strings use **מטרה** for `Goal` where spec §5.1/`E1` says
  **יעד** — with one sentence using מטרה and יעד with the meanings swapped. Filed
  on #51, not fixed in that session's scope.

- **Destination.** `kb/dev/` — same page as entry 2, or a short Hebrew-specific one.
- **Anchors.** `res/values-iw/analytics_strings.xml` header (all three rules stated);
  `res/values-iw/widget_strings.xml` lines 27–28, 42, 49, 51–53, 76 for the
  contradiction.
- **Supersedes.** Nothing.
- **Status.** Pending — `kb/`-destined, cross-repo.
