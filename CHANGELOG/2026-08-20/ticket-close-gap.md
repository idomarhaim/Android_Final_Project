# `ticket-close-gap` — 2026-08-20

Ido asked why he keeps finishing kickoffs and the tickets never close. He was right
about three of them, and the cause is structural: **`/kickoff` §5 *Exit* had three
steps — changelog, brief to `sessions/done/`, release the board row — and closing the
tracker issue was not one of them.** `issue:` is documented in the brief front matter
as *"optional tracker link"*, so nothing anywhere owed the tracker a write.

## 🔍 What the audit found

31 briefs, 16 carrying an `issue:`. Five issues were open with a `status: done` brief:

| Issue | Verdict | Why |
|---|---|---|
| `#6` `#9` `#11` | **stale — closed here** | Exit met in full, nothing held, no sibling brief |
| `#48` | correctly open | four of five sections shipped; Material + AI held, spun off as `#53`/`#54` |
| `#51` | correctly open | `hebrew-defer-freeze` says *"Park `#51` as OPEN"*; `51-freeze-verify` is still `ready` |

**Sessions were commenting and never closing.** `#48` carries a status comment
(2026-08-19), `#51` carries eight — one of them opening *"Not closing: the literal
sweep is still…"*. The write path was exercised the whole time; only the close was
missing.

## ✅ `#6`, `#9`, `#11` closed

Verified against `HEAD` before closing — the exact Exit cases exist in committed test
code (`SmartFilingTest` all three branch directions, `DurationEntryTest`'s two
directions, `FillLadderTest`'s 4 L and 40 L) and both render-pass PNGs are committed.
Each close carries a table of Exit item → evidence.

> `Untested:` **the suites were not re-run.** The Gradle daemon is claimed by the live
> `7-quickadd-complete` session and its uncommitted edits are in the working tree, so a
> run would have tested its work-in-progress rather than `HEAD`. Stated in each closing
> comment too, not only here.

## 🛠️ `/kickoff` §5 step 4 — and the two drafts the corpus killed

Ido waived the 🎬 walkthrough (*"draft it and ship it"*), so the mechanical half was
owed: run the wording against every recorded instance, then `/adversarial-review` §1.
Both ran, and **the corpus run destroyed two drafts before one survived** — then caught a
third error, in this file: draft 1's score was written from memory as *"15 of 16"* (that is
how often the keyword **fires**, not how often it is **wrong**) and re-scoring against
ground truth gave **9**. Three numbers asserted, one wrong, caught only by re-running.

| Draft | Discriminator for *"is the ticket finished?"* | Result |
|---|---|---|
| 1 | scan the changelog for `OWED` / `held` | **9 of 16 wrong** — the keyword fires on 11 of them, because every changelog *discusses* held work |
| 2 | look for an open issue naming this one | **6 of 16 wrong** — a bare `#N` cannot say which way the dependency points |
| 3 (shipped) | a sibling brief on the same `issue:` still `ready`/`active` | **15 of 16 right**, the one miss being `#48` |

The sixteenth is `#48`, and **no pattern finds it**: its held sections are named in the
brief's own `result:` prose. So the shipped wording stops pretending and makes the last
check a **read of your own `Exit` against what you actually built**.

**This is the whole value of the fallback.** The draft written from memory of the
request — *brief done, so close it* — reads as obviously correct and is wrong on `#48`
and `#51` in the direction nobody notices: it closes a ticket whose work is still
queued. Nothing but running it over the corpus would have said so.

`/adversarial-review` §1 (necessity): clean. `grep -rniE "close the (ticket|issue)|gh
issue close|closes #"` over `rules/`, `user-rules/` and `skills/` returns one hit, in
`/triage`'s `wontfix` path — a different flow. Of 31 briefs exactly **one**
(`50-finish`) names a close, and in body prose rather than in `Exit`. Leaving it to the
brief author is empirically what failed.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit / instrumented / build** | **not run, and not skipped silently.** No app code changed — this session touched `SESSIONS.md`, a JARVIS skill, and three GitHub issues. The daemon is claimed regardless (see above) |
| **Rule-corpus run** | **16 instances, 3 drafts** — the table above. This is the test layer a `rules/`-class change has, and it ran |
| **Close verification** | re-read from the GitHub API after writing: `#6` `#9` `#11` all `closed` with the comment attached; open issues 9 → 6 (`#40` in the API listing is the PR) |

## ⚠️ The `gh` classifier denied 3 of 6 writes, and step 4 now says so

`gh issue comment` was denied outright; `gh issue close -c` was denied on the first
attempt, succeeded on `#6` and `#9`, denied on `#11`, then succeeded on a retry of the
identical command. **Non-deterministic, not a permission boundary.** Same failure
`hebrew-defer-freeze` hit on 2026-08-17, where it left three `#51` writes owed. Step 4's
last paragraph exists because of it: a refused tracker write stays owed and gets said,
never read as a skipped step.

## 📋 Owed / reported, not fixed here

`kb-candidates/` holds **four** undrained files from other sessions —
`2026-08-19-50-offline-stamps.md`, `2026-08-20-9-duration-box.md`,
`2026-08-20-11-fill-buttons.md`, `2026-08-20-48-settings-surface.md`. Reported, not
drained: they are other sessions' entries and draining them is not this unit's work.

---

# Round 2 — the candidate backlog drained

Ido asked for `/kb-ingest` over **all** the undrained candidate files, *"just make sure they do not
overwrite each other and do not damage anything."* Five files, 11 entries. Full account in
**`C:/Dev/JARVIS/CHANGELOG/2026-08-20/ticket-close-gap.md`** (the bundle's repo owns it) — this
section records the half that belongs here.

## 🔍 No candidate collided with another. A **sibling** collided with one.

Mapping all 11 entries first: no two candidate files target the same page. But
`7-quickadd-complete` committed `0234745` in JARVIS **sixteen minutes** before this drain started,
adding §4g to `kb/dev/look-at-your-own-output.md` — the exact page two of these entries target —
and +31 lines to `kb/dev/android-device-verification.md`, which is a **parked** entry's destination.

Both pages were re-read at HEAD immediately before writing; the new section took **§4h** and
nothing of theirs was touched. *The bundle moved under the candidate* is normally a weeks-old
hazard. Here it was minutes, and **the destination repo's board carried no row for the session doing
it** — the only thing that surfaced it was running `git log` on the destination bundle before
writing.

## 📥 Ingested

| Entry | Landed |
|---|---|
| `11-fill-buttons` 1 | `kb/dev/firestore-write-semantics.md` §9 |
| `11-fill-buttons` 2 | `kb/dev/untranslatable-idioms.md` §8 |
| `ticket-close-gap` 1 | `kb/dev/look-at-your-own-output.md` §4h |
| `ticket-close-gap` 2 | `kb/dev/look-at-your-own-output.md` §4h-i |

`kb-candidates/2026-08-20-11-fill-buttons.md` **deleted** — every entry promoted, which is the one
deletion `rules/derivable-decision.md` §1 permits without asking.
`kb-candidates/2026-08-20-ticket-close-gap.md` **rewritten down to its survivor**, entry 3 keeping
its original number under a `## Standing — always-ask` heading. Never deleted on a partial drain.

## ⛔ Three entries parked, and none dropped

| File | Entry | Ground |
|---|---|---|
| `2026-08-19-50-offline-stamps.md` | 3 | destination `rules/` |
| `2026-08-20-9-duration-box.md` | 4b | destination `rules/`, awaiting 🎬 |
| `2026-08-20-48-settings-surface.md` | 1 | **supersedes** a standing claim in `dev/android-device-verification.md` |
| `2026-08-20-ticket-close-gap.md` | 3 | **supersedes** a standing `gh` claim in this repo's `CLAUDE.md` |

The first three were already parked by their own sessions and were **left exactly as found** —
re-swept, confirmed still always-ask, not edited. `/kb-ingest` §8 keeps `rules/`-destined and
superseding entries out of an auto-mode drain, and *"all the candidates"* does not lift that: these
are precisely the entries that rewrite something already committed, which is what *do not damage
anything* is about.

## 💥 A truncation, caught before it reached a commit — and the second this session

`kb/dev/look-at-your-own-output.md` went to **zero lines** on the first write attempt, restored from
HEAD before anything was staged. The payload spelled 🎬 as a UTF-16 surrogate pair;
`str.encode('utf-8')` refuses that, and `open(path, 'wb')` had already been evaluated and truncated
the file before the encode threw.

The **first** occurrence hit JARVIS's `SESSIONS.md` earlier today and was diagnosed correctly then.
Diagnosing it did not prevent the repeat — which is the evidence that *be careful* is not the
remedy. Structural fix, now used everywhere in this session: **build and encode the whole payload,
assert on it, and only then open the destination file.**

## 🧪 Tests — round 2

| Layer | Result |
|---|---|
| **`Check-KbLinks.ps1`** (JARVIS bundle) | **CLEAN** — 94 pages, no broken links, no orphans, no wikilinks |
| **App layers** | **none apply** — round 2 touched only Markdown, in two repos |
| **Truncation check** | line counts diffed before/after on every touched file; that is what caught the regression above |

---

# Round 3 — `#7` closed, and the audit that says nothing else can be

Ido: *"if `#7` is complete then update it on GitHub — same for any other tickets like it."*

## ✅ `#7` closed

`R6` shipped in `153620b`: a task is created **and** completed in one action, the create carrying the
completion through the one write everything else already uses. Closed with an evidence comment.

**The closing comment carries the orphan, deliberately.** `#7`'s body describes §1.4's points
inversion and §1.5's `goalEdges`; the session **deliberately did not build** either, on the reading
that §1.4's clause on *this* ticket is a **plumbing** constraint (*emit that same fact, not a second
pipe*) rather than the arithmetic. That reading is right, and the deferral points at **`C1`, which is
closed**. A search of every open issue for `goalEdges` / points inversion / `difficulty` /
`completionFacts` / `progressContribution` returned **only `#7` itself** — so the close would have
made the work invisible. The comment is the record until Ido rules on a carrier ticket.

## 🔍 Nothing else is closeable — all five checked against `/kickoff` §5 step 4

| # | Verdict | Which clause decided it |
|---|---|---|
| `#53` `#54` | open | sibling brief still `ready` — step 4's mechanical test |
| `#48` | open | the **unmechanisable** case: `result:` says Material + AI **HELD**, spun off as `#53`/`#54` |
| `#51` | open | **deliberately parked** by `hebrew-defer-freeze` — *"Park `#51` as OPEN"* |
| `#8` | open | substrate 6/6 shipped, §2.5's scheduled half held on an **occurrence model that does not exist** |

Step 4's mechanical half would have said **CLOSE** on `#48` and `#51`. Both were caught by the read
the rule prescribes for exactly that residue — the second live instance of §4h's finding, one day
after it shipped.

## ⚠️ A correction to round 2's reply

Round 2 told Ido `#7` had *"nothing held"*. **That was too confident.** `#7`'s **body** names §1.4 and
§1.5 work that does not exist; what is true is narrower — the session made an explicit, reasoned
call that the clause binding *this* ticket is about plumbing, and satisfied it. The close is still
right; the justification is not *"nothing was deferred"* but *"what was deferred was never this
ticket's to build"*. The distinction is the whole difference between a close and a silent scope cut,
and the closing comment states it rather than the changelog alone.

## 📋 `#8` is not a scope cut, and round 2 framed it badly

Round 2 recommended *"declare it out of scope and close `#8`"*. Reading the ticket body changes the
recommendation: §2.5's backward-computed deadline reminder is the spec's own stated
differentiator — *"the one thing this app knows that Google Calendar does not"*. Cutting it is not
trimming a tail.

**The ticket boundary is what is wrong, not the scope.** `#8` was widened by triage to carry the
whole notification substrate **and** §2.5's scheduled behaviours; the substrate is done, and the
behaviours need §2.2's occurrence model — due dates and the four rungs — which is a **different
subsystem** and belongs to a different ticket. Correcting the boundary closes `#8` honestly and keeps
the differentiator tracked. Ido's call; nothing done here.

## 🧪 Tests — round 3

| Layer | Result |
|---|---|
| **App layers** | **none apply** — no code changed; one GitHub close and this account |
| **Close verification** | re-read from the API: `#7` `closed`, `closed_at 2026-08-20T17:24:42Z`, 3 comments. The **list** endpoint served a stale copy showing it open — the single-issue endpoint is the one to trust |
| **Orphan search** | every open issue's title + body grepped for the five symbols above before closing |

---

# Round 4 — the two carrier tickets, and the candidate backlog drained to zero

Ido, in one turn: open both carrier tickets · `/kb-ingest` **all** the candidates, *"no
collisions"*, **🎬 skipped** · let `c12`'s push carry the held commit.

## 🎫 Two carrier tickets opened, and two tickets closed behind them

| New | Carries | Unblocked |
|---|---|---|
| **#55** | §1.4's points inversion + §1.5's `goalEdges` — deferred by `#7`, `#9` **and** `#11`, all three to `C1`, which is **closed** | `#7` closed |
| **#56** | §2.2's occurrence model — due dates and the four rungs, which §2.5's reminders need | `#8` closed |

Both ticket bodies say in their first line that opening them **commits nobody to building them**.
`#8`'s close states in full why this is a **boundary fix, not a scope cut**: §2.5's
backward-computed deadline reminder is what the spec itself calls *"the one thing this app knows
that Google Calendar does not"*, and it is now tracked rather than forgotten.

**Open issues: 8 → 6, and the two that remain unbuilt are visible instead of buried.**

## 📥 `kb-candidates/` is EMPTY — 8 entries, all landed

Pages and rules live in `C:/Dev/JARVIS` (`818f359`); full account in that repo's
`CHANGELOG/2026-08-20/ticket-close-gap.md`. What belongs here:

**This repo's `CLAUDE.md` gained one supersede** — the `gh` classifier bullet, narrowed from a
single **boundary** claim into **two kinds of denial**:

- the **`git credential fill` pipeline** stays a boundary — reading a stored secret looks like
  credential harvesting whatever the intent, and dressing it up is still out;
- **ordinary `gh issue *` writes are a flake — retry the unchanged command once.** `Observed:` 3
  denials in 6 attempts of the *identical* `gh issue close -c` command, same shell, same minute:
  denied, then `#6` ✅, `#9` ✅, `#11` denied, `#11` ✅ on a plain retry.

**Why the distinction earns its words.** Read as a boundary, one denial ends the attempt —
`hebrew-defer-freeze` abandoned three `#51` writes on first denial and left them owed for **three
days**, and they had in fact already landed. Read as a flake, one retry finishes the job.
**Retrying the unchanged command is not evading the denial**; changing the command to get past it
is, and that stays forbidden.

## 🔍 The collision check Ido asked for

All 8 entries were mapped to destinations before anything was written. **No two candidate files
targeted the same page.** Two *looked* like collisions and both turned out already discharged —
`android-device-verification.md` (§8 landed 2026-08-19) and this repo's `CLAUDE.md` (the `stdin`
clause was already at line 31, written by `51-freeze-verify` itself). In both cases the candidate's
`Status` was stale in the **safe** direction, claiming more was owed than was.

The real hazard was elsewhere and is worth carrying forward: **`look-at-your-own-output.md` had
gained §4g and §4i from two other sessions the same day**, so the new section had to be numbered
§4j against HEAD rather than against anything a candidate could have predicted.

## ⚙️ Three behaviour changes shipped — the 🎬 waiver's real content

`rules/derivable-decision.md` **1a** (a ticket-granted deletion must have its premise re-verified at
HEAD — **scored 1 fire / 22 silent** over this repo's 23 deletions, and the one fire is `941d6a8`,
*"a test said so — not the ticket"*) · `rules/memory-promotion.md` **§ The withdrawal side**
(retrieval gets a trigger; the fallback **dropped a surface** the draft named because no page
exists for it) · `user-rules/my-rules.instructions.md` **§ 📱** (the *"mutually exclusive on one
device"* claim narrowed — `install -r` + `am instrument` preserves the sign-in, and four sessions
have now used it).

## 🧪 Tests — round 4

| Layer | Result |
|---|---|
| **`Check-KbLinks.ps1`** | **CLEAN**, 94 pages — after catching one broken link this ingest introduced |
| **Rule-corpus runs** | 23-deletion scoring · per-surface page resolution · 4-session confirmation |
| **App layers** | **none apply** — Markdown and GitHub only. `c12-material-contract` holds the Gradle daemon, `adb` and the device; this session claimed **no singletons** and touched none |
| **Ticket verification** | `#7` and `#8` re-read from the API after closing — both `closed`, comments attached |
