# KB candidates — `changelog-index-backfill`, 2026-08-17

Session: `changelog-index-backfill` · repo `c:\Dev\Android_Final_Project` ·
account [`CHANGELOG/2026-08-17/changelog-index-backfill.md`](../CHANGELOG/2026-08-17/changelog-index-backfill.md).

Written in every mode. Not drained by this session — see `Status` on each entry.

---

## 1 · A broken verification instrument fails in two modes, and only the quiet one costs you

**Claim.** When you check your own output with an ad-hoc script, the script is itself
untested code, and its two failure modes are not equally dangerous. A **loud** break
(everything reported broken, a usage error printed) costs a minute. A **quiet** break —
one that returns a *plausible* number — is believed, and it is believed most readily
when the number confirms what you already suspect. So the control that matters is not
"did the checker run" but **"can this checker still detect a real defect?"**, answered
by feeding it one.

**Why.** Three instruments broke in a single session, all on the verification side and
none on the work: (a) `grep -qxF "$line"` parsed rows beginning `- [` as option bundles
and reported *all 36* rows missing — loud, fixed in a minute; (b) a greedy
`sed 's/.*](\([^)]*\)).*/\1/'` took the **last** link on each row instead of the first
and reported **13 of 29** index rows as broken targets — quiet, plausible, and exactly
the number a genuinely rotted index would produce, so it was about to be written into a
changelog as a finding; (c) `grep -c $'\r'` expanded to an empty pattern, matched every
line, and "confirmed" 99 CR bytes in a file that had 0. Rejected alternative: *read the
script more carefully* — (b) is four characters and reads correctly. What caught (a)
was a **positive control** (re-run against a deliberately doctored file, demand exactly
1 loss); what caught (b) and (c) was **cross-checking with a different mechanism**
(.NET byte count; the independent per-file link sweep that had already reported 0
unlinked and contradicted the 13).

**Destination.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` — a fold, not a new
page. That page's thesis is *re-run what consumes your output rather than reading it*;
this extends it one hop to **the re-runner itself**, and supplies the cheap remedy
(positive control / second mechanism) that its "be careful" and "use a tool" sections
already reject as insufficient.

**Anchors.** `CHANGELOG/2026-08-17/changelog-index-backfill.md` § 🧪 Tests.

**Supersedes.** Nothing. Additive to an existing page's argument.

**Status.** Not drained — cross-repo (page lives in `C:\Dev\JARVIS\kb`), so it owes a
row on that board. This repo's established pattern is a separate `kb-drain-*` session
(`kb-drain-51d`, `kb-drain-widget-hebrew`). Ready to ingest as-is.

---

## 2 · `.git/hooks` is an unclaimable shared singleton — installing one changes every concurrent session's rules mid-flight

**Claim.** A git hook is not version-controlled, so it can be **neither claimed on the
board nor delivered by a commit**. Installing one is therefore an environment change
that takes effect **immediately, for every session sharing the working tree**, with no
artifact any of them can read to find out. It is the same class as a shared singleton
(the Gradle daemon, the emulator), but the board has no column for it, because the
board records what is *in* the repo. A session that installs a hook owes the other
sessions an explicit announcement — the hook's own failure message is not enough,
because it arrives at the moment their commit is already refused.

**Why.** Installing a `pre-commit` hook beside a live sibling, the sibling committed
successfully (`bc5ef69`) purely because their changelog file happened to be staged when
they did — the gate passed by luck, not by design, and would have blocked them a minute
either side. Rejected alternative: *ship the hook source and let each session install
it* — that is what `scripts/git-hooks/` plus `Install-GitHooks.ps1` already do, and it
is exactly the rot mode the hook exists to prevent, since a gate nobody is forced to
install is a gate nobody has. The asymmetry worth recording: the hook **source** is
committed and claimable; the hook's **installation** is neither, so the two halves of
one change have completely different visibility to siblings.

**Why not `rules/`.** It prescribes no new agent behaviour beyond the announcement duty
that §5's *claim the shared singletons* already implies; it documents a **gap in what
the board can represent**. If it were to become a rule, it would be a clause on §5's
singleton list, and that makes it always-ask.

**Destination.** `C:\Dev\JARVIS\kb\dev\flows\lease.md` (or the topology page's §5
singleton discussion) — as a named gap: *singletons that live outside the repo cannot
be claimed inside it.*

**Anchors.** `scripts/git-hooks/pre-commit`, `scripts/Install-GitHooks.ps1`,
`SESSIONS.md` (the 🪝 standing note), `bc5ef69`.

**Supersedes.** Nothing.

**Status.** Not drained — cross-repo, same as entry 1. **Flag on ingest:** if the
draining session concludes this is a behavioural clause rather than a documented gap,
it becomes a `rules/` candidate and is **always-ask** in both modes.

---

## 3 · A brief's characterisation of a tool is not evidence about the tool — read the tool's own header

**Claim.** When a session brief warns you off a tool by describing its design
(*"two structures in one file, which the generator's own design deliberately avoids"*),
that is the brief author's reading, written without running it. Read the tool's own
documentation before accepting a constraint it supposedly imposes — the constraint may
not exist, and here it did not: `New-ChangelogIndex.ps1`'s header **prescribes** exactly
the shape the brief said it avoided (*"legacy flat days keep their hand-written rows in
the static Archive table below the generated region"*). The brief's recommendation was
right; its reason was wrong, and a session that took the reason at face value would have
believed it was deviating when it was in fact conforming.

**Why.** The general form is that a brief carries two very different things — **findings
measured by the session that wrote it** (here: the index is dead, `-RepoRoot` alone does
not make the script run, the marker error message) and **inferences it drew without
running anything** (here: what the generator's design intends). The first kind is
evidence; the second is a hypothesis wearing the same typography. The brief's numbers
were also off — "~20 sessions with no row" against a measured **46**, and "21 day
folders" against **15** folders plus 7 flat files — in the direction that would have
made the hand-backfill option look affordable. Rejected alternative: *distrust briefs
generally* — the measured half was accurate and load-bearing, and the trap warning
saved a destructive run.

**Destination.** `C:\Dev\JARVIS\kb\dev\flows\` — the `/kickoff` flow page, if one exists,
else a fold into whatever documents brief authorship (§4.1's Form B). The actionable
half is for the **author**: mark which claims were measured and which inferred, which is
`claim-provenance.md`'s `Observed:` / `Inferred:` / `Untested:` applied to briefs.

**Anchors.** `sessions/changelog-index-backfill.md` §"⚠️ The trap",
`C:\Dev\JARVIS\scripts\New-ChangelogIndex.ps1` header,
`CHANGELOG/2026-08-17/changelog-index-backfill.md` §2.

**Supersedes.** Nothing. Corroborates `claim-provenance.md` by showing the same failure
in a document type that rule does not currently name.

**Status.** Not drained — cross-repo, same as entries 1 and 2.
