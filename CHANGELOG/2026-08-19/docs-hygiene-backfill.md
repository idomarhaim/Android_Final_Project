# 2026-08-19 — `docs-hygiene-backfill`

> **Branch:** `main`
> **Summary:** The false *"JDK 25"* claim corrected at 7 live sites (it was a broken JDK 21, and the rejecter is Gradle 8.10.2, not AGP), and the mandatory `> **Summary:**` line backfilled by extraction into 59 changelog files — 71 of 82 now carry it, up from 12.

`/kickoff docs-hygiene-backfill`, `AUTO MODE`. Docs only: no Kotlin, no Gradle run, no
device, neither singleton taken. Ran alongside `new-machine-checkup`, whose paths are
disjoint from every path here.

## 🔧 Item 1 — the false JDK claim

The brief scoped this as *"one line, plus a `grep` for copies"* and named **four** live
copies. The grep found **eight**, because the brief's re-verification pass had looked at
prose docs only and not at code comments.

It also turned out the claim is wrong in **two** independent ways, not one:

1. **The machine's ambient JDK was never 25.** `AGENTS.md:136` corrected this on
   2026-08-15 — it was a *broken JDK 21* install (`jdk-21.0.11.10-hotspot`, an orphaned
   `lib/` with no `bin/java.exe`).
2. **AGP is not the component that rejects JDK 25.** *Observed 2026-08-19* by
   `brief-refresh` and recorded in `0e52a66`: `gradlew --version` **succeeds** on Android
   Studio's bundled JDK 25, and it is *configuration* that dies, with the bare string
   `25.0.2` as the entire error body — a **Gradle 8.10.2** version parser giving up.
   Every copy saying *"which AGP rejects"* misattributes it.

So each site was rewritten to say what is true and verifiable — the toolchain runs on
JDK 21 — rather than to patch one wrong noun.

| Site | Was | Now |
|---|---|---|
| `scripts/README.md:53` | "the machine default is JDK 25, which AGP rejects" | the toolchain needs JDK 21, and `gradlew.bat` boots on `JAVA_HOME` not on the pin |
| `docs/OPERATIONS.md:134` | `JAVA_HOME` is User-level `jdk-21.0.11.10-hotspot`, Machine-level is JDK 25 | the pin (`jdk-21.0.12.8-hotspot`) **overrides** `JAVA_HOME`; reinstall Temurin 21 rather than repoint |
| `docs/RELEASING.md:207` | "this machine's `JAVA_HOME` is JDK 25, which AGP rejects" | "it pins Gradle to the Temurin JDK 21 this toolchain needs" |
| `knowledge/release-distribution.md:70` | same claim | same correction |
| `.github/workflows/release.yml:49` | "skip this machine's JDK 25 (AGP rejects it)" | "run on the Temurin JDK 21 this toolchain needs" |
| `scripts/run-goalpilot.ps1:152` | "this machine's default is JDK 25" | "which need not be the pinned JDK 21" |
| `scripts/run-goalpilot.ps1:183` | warn: "AGP rejects JDK 25." | warn: "This toolchain needs JDK 21." |

`docs/OPERATIONS.md`'s bullet was the worst of them: it named a directory
(`jdk-21.0.11.10-hotspot`) that **does not exist on this machine** — *observed 2026-08-19,*
`C:\Program Files\Eclipse Adoptium\` contains exactly one entry, `jdk-21.0.12.8-hotspot`.

**Two mentions were left standing, deliberately:**

- **`AGENTS.md:136`** is the *correction itself*, not a copy of the claim; its "JDK 25"
  is the historical note explaining what was fixed. Its **last sentence is now stale**
  though — it says two more Adoptium directories are wrecks, and neither exists here any
  more. Not fixed: the brief says to ask before growing item 1 into `AGENTS.md`, and the
  same paragraph's `PATH`-offers-17 claim cannot be verified from this shell.
- **`gradle.properties:10`** ("the Android Gradle Plugin does not support the JDK 25 also
  installed here") carries the same misattribution. Not fixed: that file is where
  `new-machine-checkup`'s JDK blocker lives, that session holds `#gradle-daemon`, and
  Gradle reads the file on every invocation. A comment is not worth writing into a
  sibling's build-config file mid-session.

**The brief's own premise moved under it.** It was written before `0e52a66` landed the
same day, so it says there is *"no Adoptium JDK on this machine at all"* and that
`AGENTS.md`'s JDK paragraph "describes a machine that no longer exists". Both were true
when written and are false now: Temurin 21.0.12.8 was installed at exactly the pinned
path, the pin was correct all along, and `gradlew help` reached `BUILD SUCCESSFUL`. That
is why item 1 needed no question put to Ido.

## 📚 Item 2 — the `> **Summary:**` backfill

**12 of 82 → 71 of 82.** 59 backfilled by extraction, **11 left bare**.

*(Counts are over the 82 files the generator actually indexes —
`CHANGELOG/YYYY-MM-DD/*.md` minus `SUMMARY.md`. The brief's "11 of 88" counted the 7
legacy flat day files and `CHANGELOG_README.md` too, which `New-ChangelogIndex.ps1`
deliberately does not read.)*

### Extract, never compose — and what that ruled out

These are other sessions' accounts of their own work, so a summary is **lifted** or the
file is **left bare**. Two sources, both the author's own words:

1. **The H1 title's descriptive clause** (39 files). Most sessions here write a real
   one-line thesis there — *"the language picker turns out to be three settings"*.
2. **The lede paragraph's first sentence** (20 files), used only where the title is bare
   scaffolding (`` `49-derive-currentvalue` — 2026-08-15 ``).

Title-first was not the first design. Lede-first was, and reviewing its output is what
killed it: **nine files** would have received the identical line *"One ticket resolved,
which is the skill's limit."*, and several more *"`/wayfinder 12` invoked **bare**, …"*.
A line that does not distinguish its own session is not a summary. Three filters now
reject a candidate outright, and each was written because the dry run produced the thing
it rejects:

- **invocation banners** — `/implement #49 · … · branch x · mode AUTO MODE`;
- **methodology preambles** — two or more of *wayfinder / frontier / the skill's limit /
  work through the map / AUTO MODE* in one sentence;
- **duplicates** — any line that would be byte-identical across two files, mechanically.

### Left bare, all 11, with the reason

| File | Why |
|---|---|
| `2026-08-05/release-distribution.md` | bare title; 268-char lede with no clean sentence boundary |
| `2026-08-08/fix-task-completion-feedback.md` | bare title; 266-char lede, same |
| `2026-08-13/c11b-output-formats.md` | both candidates carry the index entry-separator (below) |
| `2026-08-13/c15b-stored-ai-text.md` | bare title; lede is a metadata banner |
| `2026-08-13/c19-area-success-failure.md` | both candidates carry the entry-separator |
| `2026-08-15/49-derive-currentvalue.md` | bare title; 281-char lede, no boundary |
| `2026-08-15/c21-offline-story.md` | bare title; lede is not a prose statement |
| `2026-08-15/c22-measure-proposal.md` | bare title; lede carries the entry-separator |
| `2026-08-15/c23-goal-category.md` | same |
| `2026-08-15/c24-settings-surface.md` | same |
| `2026-08-15/d2-life-area-route.md` | bare title; 264-char lede, no boundary |

The generator already emits a bare link where there is no summary, so these stay
**visible as gaps** rather than being papered over.

### Two defects the dry run caught, both invisible in the file itself

**1 · Global substitution corrupted the authors' sentences.** The first title parser ran
`re.sub` over the whole title to strip scaffolding, and ate interior words:

- `c8-ai-task-plans` → "corrected a released **'s** own summary" (`session` removed)
- `session-titles` → "the VS Code **&nbsp;** picker" (twice)
- `c20-derived-state` → "the hold this **&nbsp;** recorded was wrong"
- `c18-subtask-depth` → "**&nbsp;**at arbitrary depth" (`sub-tasks` eaten as a label token)
- `c11b-output-formats` → "deferred on **&nbsp;**, run" (an interior date)

Extraction that mutates the text is not extraction. The parser now strips only
**anchored** leading/trailing scaffolding, and only a token it can prove is that file's
own session label.

**2 · The index has a second separator collision, and it is unguarded.**
`New-ChangelogIndex.ps1` joins the sessions of a day with ` · ` and escapes only `|`
(`ConvertTo-CellSafe`, added 2026-08-14 after a summary containing a raw pipe silently
grew the 08-12 row an extra column). A summary containing ` · ` hits the identical
failure one separator over: the day row renders it as **two entries**, and the markdown
stays valid, so nothing complains.

This was **not** visible in the changelog files — it appears only when the generated
table is re-parsed. Five files were affected. None of the 12 pre-existing summaries
carries the separator, so this hazard would have been introduced by this backfill and by
nothing before it. Those five are now left bare rather than truncated; truncating would
have silently changed what the author said. **The script is not modified** — that is
outside this brief.

### Verification

The acceptance criterion is what the *generator* renders, not what the files contain, so
the check re-parses the generated region and diffs every rendered cell against the
intended line:

- **71 rendered verbatim**, byte-identical to the extracted text (12 pre-existing + 59 new);
- **11 rendered as bare links**, exactly the 11 above;
- **82 rows parsed**, i.e. every indexed file accounted for.

That round-trip is the only reason defect 2 was found; reading the 59 diffs would have
passed it.

### `CHANGELOG_README.md`'s protected regions

The brief makes it a `STOP` if the 36 hand-written rows change. They are bullets, not
table rows, and they live below the `CHANGELOG-INDEX:END` marker. Both regions were
fingerprinted before the regeneration and compared after:

- region above `BEGIN` — **byte-identical**
- region below `END` — **byte-identical**, **36 bullets**, unchanged

## 🚫 Not done, and why

- **The pre-commit hook was not extended** to require a session's own `> **Summary:**`
  line. The brief allows it *"only if it is cheap"*. It is not: it needs a new switch on
  `New-ChangelogIndex.ps1`, its own exit path, and a rule for staged files belonging to
  *other* sessions — and, more decisively, a new pre-commit gate changes the interaction
  protocol for every session in the repo, which the 🎬 walkthrough rule says is offered
  before it is written, not after.
- **KB candidates were written but not drained.** `kb-candidates/2026-08-19-docs-hygiene-backfill.md`
  holds them in full. `AUTO MODE` would ordinarily ingest at the commit trigger, but this
  brief scopes the KB out and `/kickoff kb-drain-51e-backfill` is the session that owns
  draining. Writing the file is unconditional; draining it is that session's work.

## 🧭 Concurrency — two things happened to this session's board writes

Both are the documented shared-file hazard, and both are recorded rather than repaired,
because the remedy for them **is** naming what happened.

1. **This session's Active-claims row was destroyed.** It was written before the first
   edit, as required. `698ff54` (`new-machine-checkup`, claiming its own row) rewrote
   `SESSIONS.md` from a copy that predated it, and `b5fb371` then repaired an unrelated
   lone-CR defect in the same file. The row did not survive either write, and **nothing
   in git shows a row being removed** — it had never been committed, so its loss is
   invisible in the history. It was noticed only because this session went to release a
   row that was no longer there.
   **No paths were contended:** `new-machine-checkup` touched `SESSIONS.md` and its own
   brief only, and the two sessions' file sets are disjoint.

2. **This session's release note was published under a sibling's commit message.**
   The note was written to `SESSIONS.md`; `04e944f` (*"new-machine-checkup round 1: the
   machine builds…"*) committed it. That is the hazard exactly as the roadmap's §🔀 states
   it — *"the window on a sibling's file opens when you write it, not when you stage it"*
   — and its stated remedy is to **name what rode along**, which this section is.

**The lone CR is worth one line of its own**, because it bit twice in one evening.
`SESSIONS.md` contains a single stray `\r` inside `brief-refresh`'s released row, where a
Windows path was written with its `\r` escape turned into a real carriage return. Reading
the file with Python's **universal newlines** translates that CR to LF and splits the row
across two lines, breaking the table — which is what `b5fb371` had to repair. Every write
this session made to `SESSIONS.md` used `newline=''` on **both** the read and the write,
and the CR count was asserted unchanged afterwards.

## 📁 New / Modified Files

- `scripts/README.md`, `scripts/run-goalpilot.ps1`, `docs/OPERATIONS.md`,
  `docs/RELEASING.md`, `knowledge/release-distribution.md`,
  `.github/workflows/release.yml` — the JDK correction (7 sites)
- 59 files under `CHANGELOG/2026-08-0*/`, `2026-08-1*/` — one `> **Summary:**` line each,
  insert-only (no diff in this session deletes a line from another session's account)
- `CHANGELOG/CHANGELOG_README.md` — regenerated index region only
- `CHANGELOG/2026-08-19/docs-hygiene-backfill.md` *(new)*
- `kb-candidates/2026-08-19-docs-hygiene-backfill.md` *(new)*
- `SESSIONS.md` — claim, then release
- `sessions/docs-hygiene-backfill.md` → `sessions/done/` with `status: done`

## 🧪 Tests

**No build was run, and none was possible or appropriate for this session.** It compiles
nothing, changes no Kotlin, and `new-machine-checkup` holds `#gradle-daemon`. Stating it
explicitly rather than skipping the section, per the testing convention.

Layer by layer:

| Layer | Status |
|---|---|
| Server unit / integration / endpoints | **not applicable** — no `functions/` change |
| Database (`firestore-tests/`) | **not applicable** — no rules change |
| Client component / page (`app/src/test`, `androidTest`) | **not applicable** — no `app/src/` change |
| UI E2E | **not applicable** — no device taken |

What *was* verified, mechanically:

1. **Round-trip through the real consumer** — `New-ChangelogIndex.ps1` run, its generated
   region re-parsed, all 82 rows diffed against intent: 71 verbatim, 11 bare, 0 mismatches.
2. **Insert-only** — `git diff --numstat` over `CHANGELOG/` shows a deleted-line count of
   **0** on every one of the 59 files.
3. **Protected regions** — SHA-256 of the head and tail regions of `CHANGELOG_README.md`
   equal before and after; 36 hand-written bullets still present.
4. **No survivors** — `grep -rn "JDK 25"` over the tree returns only the two mentions
   named above as deliberate, plus historical `CHANGELOG/` accounts, which are records of
   what was believed at the time and are correctly left alone.
5. **`-Check -Staged` clean** — the pre-commit gate's own check passes on the staged tree.
