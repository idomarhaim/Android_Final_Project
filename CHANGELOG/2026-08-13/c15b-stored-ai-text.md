# `c15b-stored-ai-text` — 2026-08-13

Session on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12), branch
`feat/goalpilot-implementation`. Mode: **AUTO MODE**.

## Claim c15b-stored-ai-text: #35 (C15b) — the ticket the last claim called "the natural next one"

`/wayfinder 12` was invoked with the **map**, not a ticket, so the pick was the agent's. This
commit is the **claim only** — no resolution, no `#12` write, no source file touched.

### Frontier, derived out of the dependencies API

`/issues/12/sub_issues` enumerated, then every open child queried for `blocked_by`.

| Ticket | Blocked by | Assignee | Verdict |
|---|---|---|---|
| `#35 · C15b` | `#24` ✅ `#29` ✅ | `idomarhaim` | **frontier — CLAIMED** |
| `#41 · C19` | *(none)* | — | frontier — declined, 61 seconds old |
| `#30 · C11b` | `#19` ✅ `#20` ✅ `#24` ✅ `#29` ✅ | — | frontier — declined, terminal by design |

True state at 01:03 local: **26 children, 23 closed, 3 open**, all three unblocked and unassigned.
Second consecutive derivation with **no blocked ticket on the map at all**, so leverage
discriminates nothing between the three.

### The derivation surfaced a defect in its own instrument

`sub_issues` returned `#21 · C5` as **open**; a direct `gh issue view 21` seconds later returned
**CLOSED**, resolution comment timestamped `22:03:10Z`. The aggregate endpoint was serving a stale
`state`, and nothing in its output said so. Trusting it would have produced a frontier containing an
already-resolved ticket. **Rule taken forward: never read `state` off the aggregate; confirm every
child the listing calls *open*.** The asymmetry is what makes it cheap — a stale `closed` hides a
ticket and the next derivation finds it; a stale `open` costs a wasted claim.

### Why `#35`, and the two declines

`#35`'s only standing objection was a **freshness** collision recorded by `c5-endless-goals`: the
set of AI-generated fields was being re-cut by `c2-task-type` on `#20`. That closed at `b9d1be7`
and released at `71f9413`, and the same row called `#35` *"now takeable and the natural next
claim"*. Both its blockers — `#24`, `#29` — are closed and long released.

- **`#41 · C19` declined** — created `22:02:41Z`, 61 seconds before the derivation, by
  `c5-endless-goals`, which is mid-release with its `#12` index line still unwritten. Its central
  input is `C5` §4, published one minute earlier and read by nothing. Also `wayfinder:prototype`,
  which wants Ido across several revisions, and `session-titles` already holds his attention.
- **`#30 · C11b` declined** — the map's terminal ticket by its own design note (*"you cannot test a
  format nobody has designed yet"*). `#41` arriving reinforced that ground rather than expiring it.

### Couplings named on claiming

1. `#12`'s *Decisions so far* is a commons; its race has fired three times. Re-fetch, `cmp`, write
   one line, verify a pure insertion. **And `gh api --method PATCH -f body=` cannot write it** —
   ~103 KB, dies `Argument list too long` *after* you think it worked; use `--input <file.json>`.
2. `C5` (#21) closed 60 seconds before this claim with its index line unwritten, so the next append
   is contended by definition. Its resolution is an input to read, not a subject.
3. `C15` (#15) already assigned the neighbouring question to `#30` (the per-feature veto); `C13`
   (#32) reinforced it. `C15b` must not decide what belongs to `#30`.
4. The ticket's first bullet — *which AI output is actually persisted* — is a question of **fact**
   already answered by `C8` (#24), `C10` (#29) and `C6` (#22). Read those before grilling Ido.

## 🧪 Tests

**None run, and none applicable.** This unit is Markdown plus GitHub issue metadata — no Kotlin,
no resources, no Gradle. The project's layers (unit, instrumented, UI) are untouched, and `#12`'s
standing preference is *plan, don't do*: no ticket on this map ships code. The one verification the
unit did admit was run — the claim was read back off GitHub (`#35 OPEN idomarhaim`) rather than
assumed from the `gh issue edit` exit code, which is the same discipline the KB candidate below
generalises.

## 📥 KB candidates

- **Filed, not drained** — `kb-candidates/2026-08-13-c15b-stored-ai-text.md`: *a read through an
  aggregate endpoint is a hypothesis, exactly like a write*. 🟢 ordinary and this session's, held
  only because its destination is a cross-repo write into the live `C:\Dev\JARVIS`. It should be
  drained **together with** `c2-task-type`'s entry 1 into one section of
  `kb/dev/runtime-verification.md` — the two are the same claim from opposite directions.
- **Six pre-existing files listed before the first unit of work; none is this session's**, so
  `AUTO MODE` drained nothing. The `ux-backlog-triage` file is gone, fully drained at `8c3868f`.

---

## Unit 2 — the fact pass, and a hand-back decided by deriving

Ido answered the picker with a **hand-back** — *"I could not fully understand you or what each
option means; explain it simply and schematically, and choose the solution that gives the highest
standard and quality of the app, its purpose, UX/UI and the software, and improve it if you can."*
Per `rules/question-axis-naming.md` that forbids re-asking, requires paying the explanation half
**once in the reply**, and requires **deriving** the answer rather than defaulting to my own
Recommended. **The decision below is the agent's, not Ido's**, and he can overturn it.

**Deriving re-opened the problem, and the answer was not simply option A.** My picker's axis was
*how much groundwork before you are in the room* — a property of **my process**, not of the
artifact. The ticket's own first bullet is a question of **fact** (*which AI output is actually
persisted*), and the code answers it. Doing it revealed the ticket's fork is largely false.

### Findings (posted in full on [#35](https://github.com/idomarhaim/Android_Final_Project/issues/35#issuecomment-5273555848))

- **No AI prose is persisted server-side at all.** `Recommendation` — every coach card,
  encouragement, nudge and `C10`'s practical line — is parsed straight into `RecommendationsState`
  in the ViewModel. **There is no `recommendations` collection**; the Firestore paths are `USERS`,
  `PUBLIC_PROFILES`, `CHALLENGES`, `PARTICIPANTS`, `LIFE_AREAS`, `GOALS`, `TASKS`, `FRIENDS`,
  `SHARES`, `PROGRESS`.
- **Zero language stamps exist.** A grep for `locale|language|lang` across every file in
  `domain/model/` returns **no matches**. Bullet 3 starts from nothing.
- **Task titles are the user's own words** — the model only classifies — and `Task` has no
  `description` field. The only AI-authored prose that reaches Firestore today is a **goal title**
  from the smart sorter.
- **`C8` (#24) already handed this ticket its hardest case by name**: *"a persisted draft is
  generated AI text that survives a language switch, and it is the longest-lived instance of the
  problem on the map."*

### The derived decision

The discriminator is **speech vs content**, not AI-written vs app-written. Speech is stored for one
view or one day and follows the picker for free (`C11a`: one prompt line). Content is Ido's own
list — and a sorter-written goal title became content the moment it appeared there, so it is never
translated, which **dissolves** bullet 4: the edit was never what made it content.

**Two improvements on top, as asked.** (1) Language goes in the **cache key**, not a stamp field —
key the day-cached line by `(date, language)` so a switch is a **miss**, with zero invalidation
logic and zero stale-stamp bugs. (2) **Exactly one artifact carries a `languageTag`** — `C8`'s
draft, the only AI object that is single, mutable and in progress. One field on one object instead
of a stamp on every AI-touched record. A stale draft regenerates **on open, never eagerly**, and is
offered rather than done, because `C11a` measured that a regenerated plan is a *different* plan.

**The ticket's third grounded fact is reversed:** it warns that a switch regenerating every stored
artifact is a burst against the 30 RPM ceiling. Under this design **the switch makes zero model
calls**. The burst does not exist.

**Not resolved.** One question survives that genuinely turns on Ido — whether an open Hebrew draft,
opened after switching to English, is regenerated as a different plan or left on an English screen.
That is the grilling #35 still owes, and #35 stays open and claimed.

## 🔀 Push — resolved by facts, not by judgment

Ido's answer was conditional: *push if it harms nothing and no other session; if it harms, don't.*
The checks were run rather than asserted, and they found the question **moot**:

- **My commit `0ef2049` is already on the remote.** `c5-endless-goals` pushed it and **disclosed it
  by name** in `e967445` — precondition 5 run from the other side.
- The one unpushed commit is **`e967445`, theirs**, written while they are mid-release. Publishing
  another live session's newest commit is exactly the harm Ido's condition excludes, so **this
  session pushes nothing.**
- Also verified: the deletion in `8c3868f` (`kb-candidates/2026-08-13-ux-backlog-triage.md`, 74
  lines) is **safe** — its content landed as §5 of `kb/dev/github-issue-graphs.md` in JARVIS
  `fa17e0f`, which is **on `origin/main`** there. Knowledge published, not lost.

**One process defect caught and fixed mid-check, worth recording because it nearly shipped a false
clean:** two `Bash` calls issued in parallel **shared a working directory**, so a secret scan
intended for this repo ran inside `C:\Dev\JARVIS` against an **empty diff** and returned "clean".
Re-run with `git rev-parse --show-toplevel` printed first as evidence of where it ran. This is the
same claim as the KB candidate above — *a read is a hypothesis until you can see what it read* —
and it is now the second instance in one session.
