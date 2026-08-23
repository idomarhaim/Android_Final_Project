# KB candidates — `61-google-calendar`, 2026-08-23

**Partly drained 2026-08-23.** Two of three entries are closed; this file is rewritten down to its
one survivor rather than deleted (`rules/derivable-decision.md` §1 permits deleting only a **fully**
drained file). Ingest log: `C:\Dev\JARVIS\kb\log\2026-08-23.md`.

| Closed entry | Outcome |
|---|---|
| 1 · When **absence** is your signal, you must ask wider than you judge | 📥 **ingested** — `kb/dev/indistinguishable-at-the-boundary.md` **§5c** *(new section)*. ⚠️ The entry proposed a **new page** (`dev/absence-as-a-signal.md`) and said nothing covered it; the grep found the identical worked case already on that page from **2026-08-10** — same repo, same feature. It landed as a section. |
| 2 · In a shared tree, a red test is not evidence about **your** change | 📥 **ingested** — `kb/dev/look-at-your-own-output.md` **§4p** *(new section)*, beside §4c-ii's false green. Its `Status` below said *held* because the JARVIS board showed `65-measure-proposal` on that page; the board was **empty** by the time the drain ran. |

---

## 3 · A brief with `status: active` is a liveness surface the claim board does not have

**Claim.** `grep '^status: active' sessions/*.md` finds live sessions that `SESSIONS.md` does not,
and it should be part of the session-start read rather than a thing one notices by accident.

`Observed:` 2026-08-23. Working `#61`, I attributed two failing tests to a sibling and went looking
for its owner. `SESSIONS.md`'s **Active claims** held exactly one row — `60-calendar-surface` — and
none of the six dirty files was on it. The owner was `66-unmeasured-percent`: its brief said
`status: active`, its `owns:` list named five of the six files, and it had **no board row at all**.
So a session that had been running long enough to edit six files across three packages was invisible
to the one artifact whose entire job is to say who is working on what. Its row landed later, in
`0831bc6`.

**Why it happens, structurally.** `/kickoff` §3 writes the row, so a brief-driven session normally
claims. But `status: active` is set by the *brief*, and the two writes are separate: a session that
was started another way, or that set the status and was interrupted before committing its row, ends
up live and unclaimed. The board cannot detect this, because the board's only evidence is rows.
The brief can, because a brief is per-session by construction and its status is a claim about
liveness in its own right.

**Why it is worth a rule and not just a habit.** The board rule already says *"an absent row is not
proof the session is finished"* and escalates to a transcript scan — a machine-local artifact that
does not exist on Copilot or on another machine. `sessions/*.md` is **committed**, so it works on
every surface the transcript check does not, and it is one grep. It does not replace the transcript
check (a brief left `active` by a dead session is a false positive, exactly as a stale row is), but
it is strictly cheaper and strictly earlier.

**Destination.** ⚠️ **`rules/agent-topology-and-model-routing.md` §5.**

**Anchors.** `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.3(c) (the transcript
procedure this sits in front of); `~/.claude/skills/kickoff/SKILL.md` §3;
`C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` §4p, which carries the **diagnostic** half of this
finding and stands on its own without the rule change.

**Supersedes.** Nothing; it adds a step ahead of §5.3(c) rather than changing it.

---

## Standing — always-ask

**Entry 3 is blocked twice over, and neither block is uncertainty about the claim.**

1. **Destination `rules/`** — a change to how an agent behaves, not a KB page. Always-ask in both
   modes (`rules/memory-promotion.md`).
2. **It alters the interaction protocol** — what a session reads before its first edit — so it owes
   a 🎬 walkthrough offer before the wording is written anywhere but a draft.

Nothing is dropped: the entry is committed here, and the next session that lists `kb-candidates/`
finds it. The next drain does not need to re-reason about the block — it needs Ido's word.
