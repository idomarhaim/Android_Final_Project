# KB candidates — `c9c-calendar-sync` (2026-08-10)

Session: `c9c-calendar-sync` · `/wayfinder 12` → resolved
[#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) ·
branch `feat/goalpilot-implementation` · mode: `AUTO MODE` from Ido's second message.

Each entry stands alone. No entry may be reconstructed from this session's transcript.

**Drained 2026-08-10 — 2 of 3 ingested into `C:\Dev\JARVIS\kb`, 1 survivor below.**
Original numbering kept. Journal entry: `kb/log/2026-08-10.md`, section
*`c9c-calendar-sync`: a scope is not a permission model*.

- **1 · An OAuth scope is not a permission model** → ingested, folded into
  `kb/dev/google-oauth-scopes-and-consent.md` §7 (in place). Its bundle check turned
  out **present and wrong** — §4 already carried the request-at-first-use sentence —
  so it landed as a refinement rather than as the original claim it proposed.
- **2 · Two different events, one observable** → ingested as the new page
  `kb/dev/indistinguishable-at-the-boundary.md`. Its bundle check was **present and
  confirming**; the distinction it asked the ingest to test against
  `blindness-not-confidence.md` held on reading, so two pages, cross-linked.

---

## Standing — always-ask

## 3 · Fourth instance of the picker-axis failure — and this one failed in a way the first three did not

**Claim.** A question picker was offered on the axis *"how much of your calendar data may
the app see"*, cut three ways (nothing / summary / everything). The user answered on a
different axis entirely: **per calendar** — some shared fully, some at summary level.
The offered axis treated *"my calendar data"* as **one object with a sensitivity dial**;
the real discriminator was **per instance**, because the collection is heterogeneous —
a family calendar and an employer's calendar are different in kind, and any global level
is set for the most sensitive member and wastes every other one.

**Why this is a new failure mode, not a repeat.** Three are already on record from this
map: **framing** (`c16`: none of the three options was right), **coverage** (`c10`), and
**ownership** (`c13`: the question was never the user's to answer). This is a fourth —
call it **granularity**: the axis was correct *as a dimension* and wrong *as a unit*.
The tell is diagnostic and worth having, because it looks like success: the user answers
fluently and confidently, and the answer is simply **not on the menu**. That is the
opposite of the ownership failure's tell (*"I could not understand you"*), so a rule that
watches only for confusion will not catch it.

**The cheap check it implies.** Before offering a global setting over a *collection*,
ask whether the collection's members are homogeneous. If any member could reasonably
want a different setting from another, the axis is per-instance and the global cut is
already wrong.

**Destination.** `rules/` — it belongs in the ❓ Ambiguity rule's picker guidance, beside
the axis-naming bullet, not in a KB page.

**Anchors.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — **now shipped and in
force** (`rules-drafts-ship`, 2026-08-10), with its clause live in
`user-rules/my-rules.instructions.md` under ❓ Ambiguity and projected. So this entry is
an **amendment to a live rule**, not an addition to a draft. The three prior instances
are recorded in `SESSIONS.md` → *Recently released* under `c10-quote-feed`,
`c13-byo-api-key` and `c16-milestone-model`.

**Supersedes.** Nothing, but it **extends a live rule** and the parked always-ask entry
in `kb-candidates/2026-08-10-c16-milestone-model.md`.

**Status.** ⛔ **Always-ask in both modes — not ingestible by `/kb-ingest`, and not
drained on 2026-08-10.** Destination is `rules/`, which is a change to how the agent
behaves and is the 🎬 walkthrough rule's to move, never `AUTO MODE`'s. Waits on Ido.
