# c2-task-type — claimed #20, the ticket six refusals said to take exactly now

**Session:** `c2-task-type` · **Date:** 2026-08-13 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#20 · `C2`](https://github.com/idomarhaim/Android_Final_Project/issues/20) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked with the **map**, not a ticket, so the frontier pick was the agent's and
the reasoning is recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block (stale since `c8-ai-task-plans` flagged it and still stale):
`/issues/12/sub_issues` enumerated — **25 children, 20 closed, 5 open** — then every open child
queried for `blocked_by`.

| Ticket | Blocked by | Assignee | Verdict |
|---|---|---|---|
| `#20 · C2` | `#19` ✅ | — | **frontier — claimed** |
| `#21 · C5` | `#13` ✅ `#18` ✅ | — | frontier — left |
| `#35 · C15b` | `#24` ✅ `#29` ✅ | — | frontier — left, **newly arrived** |
| `#22 · C6` | `#19` ✅ `#18` ✅ | `idomarhaim` | claimed by `c6-log-progress` |
| `#30 · C11b` | `#19` ✅ `#24` ✅ `#29` ✅ **`#20` open** | — | **blocked by this ticket alone** |

**The frontier moved for the first time in three derivations, in two directions at once.**
`c6-log-progress` recorded it frozen — *"the frontier has stopped moving, because every ticket
that could unblock anything is already claimed."* Since then `#24` and `#31` closed, which
**graduated `#35 · C15b` onto the frontier** (unrecorded anywhere until now) and **stripped
`#30 · C11b` to a single blocker, `#20`**. So `#20` is the only open ticket on this map whose
closure unblocks anything.

## Why `#20`, and why it is not an override

`#20` has been declined by this board six times. The refusal that mattered is
`c6-log-progress`'s, and it was **conditional**: `#20` *"changes the inputs of both live
sessions, not one"* — its own body names *"it drives the time-allocation analytics that already
ship"* (`#31`, then live) **and** *"it informs point and time estimation"* (`#24`, then live) —
and it closed with *"it is also the highest-leverage ticket left … which is exactly why it
should be taken **after** `c12` and `c8` release, not against them."*

Both released; `#24` closed at `c8b0ce3`, `#31` at `22ac7d9`. Taking `#20` now **executes** that
instruction rather than overturning it. The two rivals were left for the reasons that still
stand: `#21 · C5` is the heaviest ticket on the map (the one remaining unsized Firestore
migration over Ido's live data, and the sole survivor in two separate fog paragraphs), and
`#35 · C15b` unblocks nothing.

## Couplings named on claiming

1. **`#12`'s *Decisions so far* is a commons** and its race has fired twice for real
   (`c3-points-currency`, `c1-points-and-time`). Re-fetch, `cmp`, insert one line, verify a pure
   insertion.
2. **Two of the ticket's three candidate purposes may already be dead, and the ticket does not
   know it.** `C12` retired `HorizontalBarChart` from Analytics and killed count-weighting twice
   (`C16`, `C3`), so *"drives the time-allocation analytics"* cannot be assumed. `C1` fixed the
   estimation payload at `difficulty ∈ LIGHT · ROUTINE · DEMANDING` + `estimatedMinutes` with
   **the model never emitting a point value**, so *"informs point and time estimation"* must earn
   a **fourth** emitted field against `C11a`'s measured cost. Both are read as **inputs**, never
   re-decided — but they mean the ticket's own framing is the first thing to test.
3. **`C17` already made the life-area edge many-to-many** (`Goal.lifeAreaIds`,
   `Task.goalEdges`), so *"second axis or replacement"* is asked in a model where a task already
   reaches several areas through its goals.

## Board state found on arrival

⚠️ **A live session is committing into this repo with no row on the board.** `b5322e2`,
`50200ac`, `c49f4a4`, `7915bb7` landed 00:20–00:26 tonight. `7915bb7` committed the `SESSIONS.md`
that `b5322e2`'s message promised but never staged — **60 seconds before this claim**, which is
why this session's row adopts nothing. No row was written **for** them: a row another session
invents is a report, not a claim. This session touches only its own row and its own new files.

## 📥 KB candidates

`kb-candidates/` listed before the first unit of work, as the folder's existence requires —
**four files**, each **opened**, with `C:\Dev\JARVIS\scripts\Show-CandidateQueue.ps1` (the check
`c12`'s own entry 6 was written to force) run first and its flags checked by hand. The set has
**changed under the board's last description of it**: `c1-points-and-time` drained and deleted,
`c9f-consent-screen-state` retired last night on Ido's word, and `c12` and `c8` filed two new
ones. Present now: `c16` ⚠️ and `c9e` ⛔ (both `rules/`), `c8` ⛔ (`rules/`), and `c12` (entry 1
always-ask, the rest `AUTO MODE`-eligible in themselves but **held with it by their own text**).

⚠️ **`c12`'s file changed under this session mid-read** — six `Status` lines at 00:31, four at
00:33, uncommitted — so the unrowed session above is draining it right now. No count of it is
asserted, and it is not staged: it is theirs.

**None of the four is this session's**, so `AUTO MODE` drains nothing here — the auto-ingest gate
covers the candidates *the committing unit produced*, and every one belongs to another session.

## 🧪 Tests

Not applicable to this commit: it claims a ticket and writes Markdown. No server, client,
database or UI layer is touched. `C2` ships no code — `#12`'s standing preference is
*plan, don't do*.
