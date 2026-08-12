# KB candidates — `ux-backlog-triage` (2026-08-13)

Ad-hoc session, **no board row** (see *Provenance* below). Written in **normal mode**, so this
file is a proposal: nothing here has been ingested, and `/kb-ingest` needs Ido's word.

Question asked: *can #6–#11 be run in parallel sessions, given that none of them shows a
`blocked` label?* Answering it surfaced one generalisable claim.

---

## 1 · One relation encoded in two places rots in the half nothing reads

**Claim.** When a tracker records the same *blocked-on* relation both as a **machine-readable
edge** (GitHub's dependency graph / `blocked_by`) and as **prose in the title or body**, the two
halves do not degrade equally. Every automated re-derivation reads the edge, so the edge stays
honest; **nothing ever reads the prose back**, so no re-derivation can contradict it and it rots
silently — while still being the half a human sees first, in the issue list, at a glance.

**Why — and what makes this structural rather than a discipline failure.** Concrete case, one
repo, two conventions running side by side:

- `#12`'s 25 children (the `wayfinder:*` decision map) carry **real dependency edges**. Five
  consecutive released sessions each independently re-derived the frontier from the API, and
  each recorded in `SESSIONS.md` that predicting it from the board's prose had been wrong. The
  edges never rotted, because re-derivation is a read.
- `#2`–`#11` — the *same* 2026-08-06 brief, split off at the ceremony boundary — carry
  **`blocked_by == []`** and encode their blocks in the **title**. Both rotted:
  - `#11` read *"— blocked on the C7 unit decision"* for three days after `C7` `#14` closed;
  - `#10` read *"— blocked on the C12 presentation decision"* while `C12` `#31`'s resolution
    comment ends with the words **"Unblocks #10."**

**The rejected reading is the instinctive one:** *someone forgot to update two titles* — a
discipline failure, fixed by trying harder. It is not. In **both** cases the unblocking session
did its job correctly and said so **in a comment on the blocked ticket** — `#11` even carries a
comment headed *"Unblocked — `C7` #14 has landed"*, sitting directly under a title still saying
the opposite. What failed is that the unblocker's natural act (comment where the reader is
looking) does not reach the title, and no machine ever disagrees with a title. So *"always
update the title too"* prescribes exactly the discipline that has now failed twice; the
structural fix is to put the edge where re-derivation reads it, **or keep blocks out of titles
entirely** — not to hold both copies and hope.

**The cost is measurable, not theoretical.** It produced a wrong answer to a real question. Ido
asked whether six tickets could run concurrently and reasoned from the absence of a `blocked`
label; the titles asserted two blocks, one of which had been buildable for a day and the other
for three. A session trusting the list would have queued behind nothing.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — a new page. Working title: *the half nothing reads is
the half that rots*. Check overlap with `dev/mechanism-vs-compliance.md` (neighbouring shape: a
signal that looks like evidence but is never checked against the mechanism) and with any page on
board/tracker staleness — `SESSIONS.md` has its own recorded instance of a frontier block going
**five sessions stale** while no session held it, which is the same failure in a different
artifact and belongs in the page as the second data point.

**Anchors.** Issues [#10](https://github.com/idomarhaim/Android_Final_Project/issues/10),
[#11](https://github.com/idomarhaim/Android_Final_Project/issues/11) (titles corrected
2026-08-13, this session) · the unblocking comments on `#11` and on
[#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) · `SESSIONS.md` →
*Recently released*, the five rows that each re-derived the frontier out of the API · `#12`.

**Supersedes.** Nothing. It **extends** the standing practice those five sessions converged on —
*re-derive the frontier out of GitHub rather than trusting the board* — by naming its blind spot:
that advice is sound for `#12`'s children and silently vacuous for `#2`–`#11`, where the
dependency graph is empty and every block lives only in prose.

---

## Provenance

This session wrote **no repo file** other than this one and took **no singleton**: it answered a
question, then made two `gh issue edit` calls to strip the stale suffixes from `#10` and `#11`.
No `SESSIONS.md` row was written — a claim for two mechanical title edits would have meant
writing the board, the one file the two live sessions (`c6-log-progress`, `c2-task-type`) are
most likely to touch, which is the only way the fix could have harmed either. Neither owns
`#10` or `#11`; `blocked_by` was `[]` for both, so no dependency edge was disturbed.
