# `kb-drain-widget-hebrew` — 2026-08-16

> **Summary:** Close-out half of a cross-repo unit.

Close-out half of a cross-repo unit. The knowledge landed in the central JARVIS KB
(`C:\Dev\JARVIS`, `dd2d96c`); what belongs to **this** repo is the drained candidate file and the
correction on `#51`.

## What happened elsewhere

`widget-hebrew-terminology` filed four candidates in `06ad421` and drained **none**, on purpose:
entry 1 supersedes a standing claim on `kb/dev/untranslatable-idioms.md` §3, which is always-ask in
both modes, and entries 2–4 all land on that same §3 with two of them qualifying the very sentence
entry 1 retires. Ido approved the supersede and all four drained as one unit.

§3 had recorded this repo's `res/values-iw/widget_strings.xml` defect as **live** and *"outside the
sweeping session's scope"*. It was fixed the same day in `cd49bda`, so the page was factually wrong;
the block is now rewritten in place as a **resolved** instance rather than deleted, since it is that
section's only concrete case.

Two new sections came out of the fix — the target-word collision (Hebrew has no second noun once
יעד names the entity, so the sentence was restructured to name the **measure**) and reading a string
against the code that fills it. Full account:
`C:\Dev\JARVIS\CHANGELOG\2026-08-16\kb-drain-widget-hebrew.md`.

## What changed here

- `kb-candidates/2026-08-16-widget-hebrew-terminology.md` — **deleted.** All four entries promoted,
  which is the one deletion `rules/derivable-decision.md` §1 permits without asking. `kb-candidates/`
  in this repo is empty again.
- **Correction posted on [#51](https://github.com/idomarhaim/Android_Final_Project/issues/51).** The
  comment there enumerated **six** strings; the true count is **eight resources** — six on the
  terminology rule, three on bidi, one on both. `gp_widget_effort_lead` carried the bidi defect
  **twice in one string** and appeared in no enumeration: not `#51`'s, not the KB page's, not the
  fixing session's own brief. `#51` is the durable public record, so the wrong count is corrected
  there rather than only in a changelog.

## 🧪 Tests

**None run, and none owed — no code changed in this repo.** This commit deletes a markdown file and
adds a changelog. The work that needed tests was `cd49bda`, which shipped `HebrewTerminologyTest`
and is accounted for in `CHANGELOG/2026-08-16/widget-hebrew-terminology.md`. The mechanical check
for what this unit *did* produce is the KB linter, run in the JARVIS repo: **CLEAN**, 83 pages.

Device untouched — no emulator, no `adb`, no Gradle daemon claimed.
