# KB candidates — `ticket-close-gap`, 2026-08-20

## 1 · The corpus run is a rule change's test layer, and it kills drafts that read as obviously correct

**Claim.** When the 🎬 walkthrough is waived, the mechanical fallback — *run the drafted
wording against every recorded instance of the failure it addresses* — is not ceremony.
Run for real on 2026-08-20 against all 16 ticketed briefs in GoalPilot, scored against
ground truth, it **discarded two of three drafts**:

| Draft | Discriminator | Score |
|---|---|---|
| 1 | scan changelogs for `OWED` / `held` | **9 of 16 wrong** (keyword fires on 11 — every changelog *discusses* held work) |
| 2 | find an open issue naming this one | **6 of 16 wrong** (a bare `#N` cannot say which way the dependency points) |
| 3 | a sibling brief on the same `issue:` still `ready`/`active` | **15 of 16 right** — shipped |

**Why.** The draft written from memory of the request — *brief is done, so close the
ticket* — reads as obviously correct and is wrong on `#48` and `#51` **in the flattering
direction**: it closes a ticket whose work is still queued, and nothing in its output
says so. This is the user-rule's own warning (*"a rule you recite from memory of my
request is the rule you already believe it should be"*) with a measured cost attached
for the first time.

**The second half matters as much as the first:** the surviving test's 16th case (`#48`,
four of five sections shipped) is **unmechanisable** — the held sections live in the
brief's own `result:` prose. The right response was to *say so in the rule* rather than
keep hunting for a pattern. A rule that names its own unmechanisable residue is more
honest than one that pretends to a discriminator it does not have.

**Rejected:** *"be more careful when drafting"* — draft 1 was drafted carefully; care is
not what distinguishes it from draft 3, a scored run is.

- **Destination:** `kb/dev/look-at-your-own-output.md` — new section, the run-your-rule
  case. Sibling to §4f (the seventh instrument failure).
- **Anchors:** JARVIS `8cce13b` (the shipped step 4), GoalPilot `db1597b` (the audit and
  the three closes), `CHANGELOG/2026-08-20/ticket-close-gap.md`.
- **Supersedes:** nothing. Extends the page.
- **Status:** pending — not `rules/`-destined, so it is `/kb-ingest`-able without a gate.

## 2 · Even the *report* of a run gets written from memory

**Claim.** Having run the corpus scoring and read its output, this session then wrote
draft 1's score into the changelog as **"15 of 16"** — which is how often the keyword
**fires**, not how often the rule is **wrong** (9). Caught only by the pre-commit
self-review's first question, and fixed by re-running the scorer rather than re-reading
the transcript.

**Why it is not the same finding as §1.** §1 is *the artifact was never run*. This is
*the artifact was run, and the number was still transcribed from memory a few minutes
later*. The remedy differs: §1 wants a run, this wants the run's output **recomputed at
the moment of writing**, because a figure two tool-calls old is already a remembered
figure.

- **Destination:** same page, same section — a short second paragraph, not its own page.
- **Anchors:** `CHANGELOG/2026-08-20/ticket-close-gap.md` § *the two drafts the corpus killed*.
- **Supersedes:** nothing.
- **Status:** pending.

## 3 · `gh` classifier denials are non-deterministic, not a permission boundary

**Claim.** The identical `gh issue close -c "<body>"` command was **denied** by the auto-mode
classifier, then **succeeded** on `#6` and `#9`, then was **denied** on `#11`, then
succeeded on a plain retry. `gh issue comment --body-file` was denied outright.

**Why.** `CLAUDE.md` currently records the denial as a fact about the *route* (*"the
auto-mode classifier blocks it"*, of the `git credential fill` pipeline), which reads as
a boundary to plan around. It is better modelled as **flaky**: a single retry of the
unchanged command is the correct response, and dressing the command up to get past it is
explicitly not. `hebrew-defer-freeze` (2026-08-17) abandoned three `#51` writes on the
first denial and left them owed for three days.

- **Destination:** `CLAUDE.md` (this repo) — amend the existing `gh` bullet, which already
  documents a denial. Not the central KB: it is a fact about this machine's harness.
- **Anchors:** `db1597b`, `CHANGELOG/2026-08-20/ticket-close-gap.md` § *The `gh` classifier
  denied 3 of 6 writes*.
- **Supersedes:** narrows the existing *"the auto-mode classifier blocks it"* claim from a
  boundary to a flake. **Always-ask** — it rewrites a standing committed claim.
- **Status:** pending, blocked on Ido.
