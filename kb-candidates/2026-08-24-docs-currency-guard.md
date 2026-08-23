# KB candidates — docs-currency-guard, 2026-08-24

## 1. A regex character class eats the backslash, and the check reports SILENT

- **Claim.** `[\/]` in a regex is **not** "backslash or slash" — the backslash is consumed
  escaping the `/`, leaving `[/]`. A path check built on it matches only the forward-slash
  spellings of a Windows path and **passes** on the backslash ones. Write `[\\/]`.
- **Why.** `Observed:` 2026-08-24, in the check written to catch three documents telling a reader
  to export `JAVA_HOME` to a JDK that does not exist on the machine. The check reported
  `silent 0/1` — the single match being the one *correct*, forward-slash spelling in a fourth
  document. The three broken ones were invisible. It is `kb/dev/look-at-your-own-output.md`'s
  central shape: a search run at the wrong width does not fail, it **passes**, and nothing in its
  output says the width was wrong. Only having the hand-audited list to diff against exposed it.
  *Rejected:* "be careful with regexes" — the same page already records why that remedy fails.
  The transferable form is: when a check reports **silent**, verify against a known-positive
  before believing it.
- **Destination.** `kb/dev/look-at-your-own-output.md` — a new §, or beside the existing
  "check the instrument on the hardest input it exists for" clause. This IS that clause with a
  measured instance.
- **Anchors.** §4k (print `wc -l` beside every count) is the same family one layer over.
- **Supersedes.** Nothing.
- **Status.** Ready.

## 2. Documentation is code's product copy, and the same remedy applies

- **Claim.** `docs/` makes assertions about code, nothing re-runs them, and the drift is
  therefore unbounded — `docs/ARCHITECTURE.md` accumulated ~15 false claims over 20 days with
  nothing in the repo able to go red. The tractable half is **enumerations the document
  explicitly makes** (a fenced collection tree, a named callable list, a named nav bar, a quoted
  shell command): those can be recomputed from the code and diffed in an ordinary unit test.
  The intractable half is **false sentences**, which no presence check reaches.
- **Why.** Splitting the two is what makes the guard honest instead of reassuring. Measured here:
  of 15 real findings the mechanical layer catches 8, and the 7 it misses are the ones that
  actually mislead a reader. A guard that did not say so would be read as a currency guarantee
  and would let the dangerous half rot further — that was the strongest argument raised against
  building it at all (`/adversarial-review` §1). *Rejected:* a post-commit hook nudging "check
  the docs" — it fires on nearly every commit in an app repo and is ignored within a day;
  narrowing it needs a path→doc manifest, which is an abstraction with one consumer.
  *Also rejected:* guarding a stated test count — it changes on every commit that adds a test,
  so it taxes the commonest action in the repo; delete the number instead.
- **Destination.** `kb/dev/product-copy-describes-code.md` — **extend the existing page**, which
  `tour-refresh` created the same day for the on-screen case. This is the same mechanism in the
  documentation layer, and the two belong together rather than in two pages.
- **Anchors.** `kb/dev/look-at-your-own-output.md` (recompute-and-diff is the remedy);
  `Android_Final_Project/app/src/test/java/com/idomarhaim/goalpilot/docs/DocsCurrencyTest.kt`
  is the worked implementation.
- **Supersedes.** Nothing. Extends rather than contradicts.
- **Status.** Ready.

## 3. Kotlin block comments NEST, so `/*` inside a KDoc breaks the file

- **Claim.** Writing `feature/*` or `docs/*.md` inside a `/** … */` KDoc opens a **nested**
  comment that is never closed. The compiler reports `Unclosed comment` at the **last line of the
  file**, naming neither the KDoc nor the token.
- **Why.** `Observed:` 2026-08-24 — a 2m17s build died at `kspDebugUnitTestKotlin` with the error
  pointing at line 212 of a 212-line file; the cause was `feature/*` on line 46. Third member of
  a family this project already documents twice: a `.properties` backslash escape, and `--`
  inside an XML comment killing three Gradle tasks at once. **The house style is the risk factor**
  — long explanatory comments in every file mean prose punctuation keeps landing where the file
  format treats it as syntax. Cheap check: `grep -n '/\*' <file>` should return only real comment
  openers.
- **Destination.** `Android_Final_Project/CLAUDE.md`, beside the existing `local.properties` and
  XML-comment bullets — it is a machine/project-local trap, not general knowledge.
- **Anchors.** The two sibling bullets already there.
- **Supersedes.** Nothing.
- **Status.** Ready.

## Standing — always-ask

## 4. Should `docs/` grow the sections it is missing entirely?

- **Claim.** Beyond the false sentences repaired here, `docs/ARCHITECTURE.md` has no section at
  all for settings, the guided tour, the home-screen widget, calendar, notifications, the locale
  layer, or the multi-provider AI story — and `docs/OPERATIONS.md` §1/§3 describe a project state
  from early August (Health Connect "is a stub", challenges "preview screen with sample data",
  92+12 tests). None of that is a *false sentence* a guard can catch; it is absence.
- **Why held.** Writing them is a rewrite of documents that predate this session's work, and the
  audit already told Ido it was his call; he answered a different question. Not reconstructable
  from a transcript later, so it is written down in full here rather than left in chat.
- **Destination.** Not a KB page — a decision, then either an issue or a brief.
- **Status.** **HELD — Ido's call.** Nothing about it is blocked meanwhile: the guard is green
  and the false claims are gone.
