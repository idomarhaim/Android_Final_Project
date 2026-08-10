# `c17-many-to-many` — a goal in several life areas, a task under several goals

**Session:** `c17-many-to-many` · **Invocation:** `/wayfinder 12` *(bare — no ticket
named)* · **Branch:** `feat/goalpilot-implementation` · **Mode:** `AUTO MODE` ·
2026-08-10.

One ticket, which is the skill's limit. This map ships no code.

## Session hygiene, before the first unit of work

| | |
|---|---|
| `kb-candidates/` listed | **2 files, both already drained down to always-ask survivors** — [`2026-08-09-c9f-consent-screen-state.md`](../../kb-candidates/2026-08-09-c9f-consent-screen-state.md) entry 1 and [`2026-08-10-c16-milestone-model.md`](../../kb-candidates/2026-08-10-c16-milestone-model.md) entry 2. Both are `rules/`-destined, so `/kb-ingest` may not take them in **either** mode; both wait on Ido, not on a session. Nothing here is a backlog |
| `SESSIONS.md` read | No active claims. The frontier block was refreshed 2026-08-10 by `c9a-schedule-a-task` and is **accurate** — re-derived from GitHub independently below and it matches |
| Template parity | `Update-TemplateConsumers.ps1` → **`AGENTS.md` v15 → v16** in this repo (verbatim projection, provenance verified, one block: `routing`). Applied and committed as its own mechanical commit. Three files in `C:\Dev\FP_DEMO` reported **BLOCKED** (dirty tree) and were not touched — Ido's to decide |
| Leases | `AGENTS.md`, `SESSIONS.md` taken via `Lock-Path.ps1` before the first write, released at the commit |

**What v16 changed, and it landed before the work rather than after:** the claim rule
now says the board belongs to **the repo being edited**, not the directory the session
started in — so a `/kb-ingest` into the central bundle owes a row in `C:\Dev\JARVIS`
too, with a carve-out for a mechanical sweep. That is why the `AGENTS.md` bump above is
recorded here rather than claimed on the board.

## The frontier, re-derived rather than trusted

`/wayfinder 12` arrived bare, so the ticket is the session's to pick. Queried out of
GitHub — open, unblocked, unassigned children of #12 — via the native dependency
relation:

| Frontier ticket | Closing it unblocks |
|---|---|
| **[#38 · `C17`](https://github.com/idomarhaim/Android_Final_Project/issues/38)** | **9** — sole remaining blocker of [#18 `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18), and behind #18 sit `C1` #19, `C5` #21, `C6` #22, `C14` #23, `C12` #31, then `C2` #20, `C8` #24, `C11b` #30, `C15b` #35 |
| [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39) | 1 directly (#19, jointly with #18) |
| [#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) | 1 (#28) |
| [#26 · `C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26) | 0 |

Took **#38** on leverage. It matches the board's own recommendation, arrived at
independently.

<!-- the resolution, tests and files sections are appended when the ticket closes -->
