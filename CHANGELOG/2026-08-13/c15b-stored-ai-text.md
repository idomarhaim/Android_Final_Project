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
