# KB candidates — `docs-hygiene-backfill`, 2026-08-19

Written in every mode; **not drained by this session**. `AUTO MODE` would ordinarily
ingest at the commit trigger, but `sessions/docs-hygiene-backfill.md` scopes the KB out
and `/kickoff kb-drain-51e-backfill` owns draining. Each entry stands alone — no
transcript is needed to write the page.

---

## 1 · A generator that escapes one separator will collide on the other one

**Claim.** When a generated index joins records with a separator *and* embeds
author-written text, escaping the **table** separator is not enough: the **join**
separator is the same bug one character over, and it fails silently because the output
stays syntactically valid. Guard every separator the generator itself emits, or reject
input that contains one.

**Why.** `scripts/New-ChangelogIndex.ps1` in `Android_Final_Project` builds a day row as
`link — summary` entries joined by ` · `, inside a markdown table cell. It gained
`ConvertTo-CellSafe` on 2026-08-14 after a summary containing a raw `|` silently grew the
2026-08-12 row an extra column — *"nothing to do with wrapping, and worse than it"*, in
its own comment. The middot it joins with was never escaped. On 2026-08-19, five
backfilled summaries contained ` · ` (four of them because a session titles its work
`` `C22 · The measure proposal…` ``), and each rendered as **two entries** in its day row:
one session appeared to be two, and the markdown stayed valid, so `-Check` passed.

The pipe bug and the middot bug are the same defect, found five days apart, because the
first fix treated *the pipe* as the problem rather than *"text I did not write is being
concatenated into a syntax I control"*.

**What was rejected.** Truncating an offending summary at the separator — it silently
changes what another session said about its own work. Escaping the middot in the
generator — out of the session's brief, and it would rewrite existing rows. The five
files were left with no summary line, which the generator already renders as a bare link,
i.e. a visible gap.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — it generalises past this repo (any
index/digest/rollup generator that joins author text), and the central KB already holds
`look-at-your-own-output.md`, which this is a concrete instance of.

**Anchors.** `scripts/New-ChangelogIndex.ps1` (`$Sep`, `ConvertTo-CellSafe`,
`Get-DayRow`), `CHANGELOG/2026-08-19/docs-hygiene-backfill.md` § *Two defects the dry run
caught*.

**Supersedes.** Nothing. Extends the `ConvertTo-CellSafe` rationale already in the
script's own header comment.

**Status.** Ready to ingest.

---

## 2 · Backfilling someone else's summary: the title is the thesis, the lede is boilerplate

**Claim.** When lifting a one-line summary out of documents you did not author, prefer
the **H1 title's descriptive clause** over the opening paragraph. A session's title is
where it states what it found; its lede is where it states how it was invoked. Reject any
candidate that would be byte-identical across two documents — a line that does not
distinguish its own record is not a summary, and the duplicate check is mechanical, so it
needs no judgement about content.

**Why.** Backfilling `> **Summary:**` into 70 changelog files: lede-first extraction
produced the identical sentence *"One ticket resolved, which is the skill's limit."* for
**nine** files, plus repeated *"`/wayfinder 12` invoked **bare**, so the frontier pick was
the agent's…"*. Title-first produced a usable thesis for 39 of them
(*"the discriminator turns out to be an edge, not a property"*). The reason is structural,
not stylistic: a house style that puts invocation, branch and mode in the opening
paragraph guarantees the lede is metadata, while the title has no such slot and so
carries the finding.

The duplicate filter matters independently of the source: it is the only check that
catches a *plausible* summary which is nevertheless worthless, and it fires without
anyone reading the text.

**What was rejected.** Composing a summary from the file's contents — this is another
session's account of its own work, and authoring one is inventing provenance
(`read-before-write.md` names the same hazard for changelog files). Hand-picking per file
— 70 files, and the point is a rule that survives the next 70.

**Second, sharper finding — the stripping must be anchored.** Removing scaffolding
(`Changes`, a date, the word `session`, the label) with a *global* substitution over the
title corrupted five extractions in ways that read as fluent English: *"corrected a
released 's own summary"*, *"the VS Code picker"*, *"sub-tasks at arbitrary depth"* →
*"at arbitrary depth"*. Strip only anchored leading/trailing tokens, and only a token
provably equal to (or a hyphen-boundary prefix of) that file's own label. **Extraction
that mutates the text is not extraction** — and the corruption is invisible in the output
unless it is diffed against the source.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — the method is repo-independent; it applies to
any backfill over other agents' or other people's records.

**Anchors.** `CHANGELOG/2026-08-19/docs-hygiene-backfill.md` § *Extract, never compose*;
`sessions/docs-hygiene-backfill.md` (the constraint as briefed).

**Supersedes.** Nothing.

**Status.** Ready to ingest.

---

## 3 · A false claim in docs propagates into code comments, where a prose-only grep misses it

**Claim.** When correcting a wrong factual claim, grep the **whole tree**, not the docs.
A claim that has been copied has usually been copied into comments and warning strings
too, and those copies outlive the prose because nobody re-reads a comment. Count the
copies mechanically before scoping the fix.

**Why.** `sessions/docs-hygiene-backfill.md` was re-verified against `HEAD` on
2026-08-19 — deliberately, and it corrected the brief's older *"one known copy"* to
*"four live copies"*. Running `grep -rn "JDK 25"` over the tree found **eight**: the four
prose copies it named, plus a CI-workflow comment, two lines in
`scripts/run-goalpilot.ps1` (one of them a **user-visible warning string**), and a
`gradle.properties` comment. The re-verification had looked only where the claim was
expected to be.

The same session also found the claim was wrong along a **second** axis nobody had
noticed: every copy said *"which AGP rejects"*, but the component that actually refuses
JDK 25 is the **Gradle 8.10.2** version parser — `gradlew --version` succeeds on JDK 25
and *configuration* dies with the bare string `25.0.2`. Correcting only the noun that was
flagged would have left seven sites confidently wrong about the mechanism.

**What was rejected.** Fixing only the four named copies (leaves the claim live in a
warning string a user reads). Fixing `gradle.properties:10` as well — that file holds a
sibling session's live JDK blocker and that session held `#gradle-daemon`; a comment is
not worth writing into another session's build config mid-run.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — extends `claim-provenance.md`, which governs
hedging a claim at authorship; this is the **removal** side of the same lifecycle.

**Anchors.** `CHANGELOG/2026-08-19/docs-hygiene-backfill.md` § *Item 1*; `0e52a66`;
`CHANGELOG/2026-08-19/brief-refresh.md`.

**Supersedes.** Nothing. Adjacent to `claim-provenance.md`.

**Status.** Ready to ingest.
