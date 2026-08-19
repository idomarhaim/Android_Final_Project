# kb-drain-51e-backfill — three candidate files drained, nine entries landed, nothing parked

> **Summary:** the three 2026-08-17 `kb-candidates/` files finally reached `C:\Dev\JARVIS\kb`; both always-ask gates were checked against the page text and neither opened.

**Issue:** none — this is the `/kickoff` of [`sessions/kb-drain-51e-backfill.md`](../../sessions/kb-drain-51e-backfill.md).
**Branch:** `main`. **Mode:** AUTO MODE (`mode: auto` in the brief).
**Scope:** `kb-candidates/` and the drain. **No `app/src/` change, no build, no device** — disjoint
from `new-machine-checkup`, which held both singletons throughout.

---

## What this session was

A **cross-repo** session: the entries were filed here, the pages belong in `C:\Dev\JARVIS\kb\`,
and that is a session's work rather than a step — which is exactly why three separate sessions
routed their candidates here instead of draining at their own commit trigger. Board rows were
held on **both** repos.

The brief's own re-verification note was correct on every point: **nothing had been drained**, all
9 entries still read `Not drained`, and there were **three** files, not the two in its title.

| Source file | Entries | Filed by |
|---|---|---|
| `kb-candidates/2026-08-17-51e-sweep-components.md` | 4 | `51e-sweep-components` |
| `kb-candidates/2026-08-17-changelog-index-backfill.md` | 3 | `changelog-index-backfill` |
| `kb-candidates/2026-08-17-completion-roadmap.md` | 2 | `completion-roadmap` |

## What landed

**1 new page · 7 in-place extensions across 6 pages · 1 reciprocal cross-ref · 6 index rows.**
Destination commit `C:\Dev\JARVIS@e12b88c`; the full account is
`C:\Dev\JARVIS\CHANGELOG\2026-08-19\kb-drain-51e-backfill.md`.

| Entry | Landed |
|---|---|
| `51e` 1 — a guard's false positive fires on the **remedy** | `kb/dev/look-at-your-own-output.md` §4a |
| `51e` 2 — a guard grows on one side, not the other | `kb/dev/jvm-vs-android-locale-codes.md` §4a |
| `51e` 3 — stripping speech from a domain type is decided by its consumers | `kb/dev/untranslatable-idioms.md` §1 |
| `51e` 4 — shared code that *forwards* copy shares nothing | `kb/dev/untranslatable-idioms.md` §7 *(new)* |
| `cib` 1 — the checker you wrote is itself untested code | `kb/dev/look-at-your-own-output.md` §5.2 |
| `cib` 2 — `.git/hooks` is an unclaimable shared singleton | `kb/dev/flows/lease.md` §4e |
| `cib` 3 — a brief's characterisation of a tool is not evidence | `kb/dev/flows/session-handover.md` §5b |
| `cr` 1 — already-stored state is an entry point | **new** `kb/dev/stored-state-is-an-entry-point.md` |
| `cr` 2 — two blockers indistinguishable from outside | `kb/dev/indistinguishable-at-the-boundary.md` §5b |

## The two always-ask gates — checked, and neither opened

**Nothing superseded a standing claim.** `51e` 3 was the candidate with that shape, the precedent
being `kb-drain-51d`, whose entry 1 narrowed a standing claim and was therefore always-ask. Checked
against the page text rather than against the candidate's own `Supersedes: Nothing` line: §1's
fourth idiom prescribes an **end state**, and this entry leaves it untouched — it adds *when you can
reach it in one move*. Additive, appended, no committed sentence rewritten.

**`cib` 2 was judged `kb/`, not `rules/`, and split.** The brief flagged it as possibly
`rules/`-destined. Its **documented gap** — `.git/hooks` is not version-controlled, so a hook is
neither claimable nor deliverable by a commit, and its source and installation have completely
different visibility to siblings — is knowledge, and was ingested. Its **duty** half (that the
installing session owes an announcement) would be a clause on §5's singleton list, i.e. a `rules/`
change, i.e. Ido's under the 🎬 walkthrough rule: **not drafted**, and named in §4e under *"What
this is not"* so whoever takes the decision can see it.

## 🧪 Tests

This session changed no application code, so no app-layer suite applies and **none was run** — by
design, not omission. Two layers do exist for the work it actually did, and both were run:

- **`Check-KbLinks.ps1` over `C:\Dev\JARVIS\kb`** — **CLEAN**, 90 pages, no broken links, no
  orphans, no wikilinks.
- **One finding from that run, and it is §5.2's own claim biting its author:** the new section
  quoted a greedy `sed` link-extraction expression whose literal `](` the linter parsed as a
  markdown link (`BROKEN LINK: … -> \([^`). Reworded to describe the expression rather than quote
  it; re-run clean. Reading the section could not have caught it — running its consumer did.
- **Promotion verification, twice.** Each of the 9 entries was `grep -F`-checked at its destination
  in the working tree, then again against `git show e12b88c:<file>` — the destination **commit**,
  not my own commit message. All 9 `OK`. That is what makes the deletion below safe.
- **`New-ChangelogIndex.ps1`** regenerated in both repos; both pre-commit hooks passed.

## Candidate files

All three **fully drained → deleted**, in the same commit as this entry, per
`rules/derivable-decision.md` §1 (a fully-promoted candidate file is deleted without asking; a
partly-drained one is rewritten down to its survivors and never deleted). No file here was partly
drained.

**Two candidate files remain undrained here and were deliberately untouched:**
`kb-candidates/2026-08-19-docs-hygiene-backfill.md` and
`kb-candidates/2026-08-19-new-machine-checkup.md`. Both were written **after** this brief, both
belong to sessions of their own, and `new-machine-checkup` was **live on the board** with its
candidate file named in its own row while this session ran.

## Reported, not fixed

`C:\Dev\JARVIS\kb\index.md` lists `screen-entry-effects-and-viewmodel-lifetime.md` inside the
**`dev/flows/`** table, where it does not belong — it is a `dev/` page. Pre-existing, not this
brief's scope, and not this session's row. *(My own new row landed in the same wrong table on first
write, which is how it was noticed, and was moved.)*
