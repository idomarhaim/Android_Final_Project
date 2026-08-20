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
