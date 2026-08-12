# session-titles — making a board label findable from the VS Code session picker

**Session:** `session-titles` · **Date:** 2026-08-13 · **Mode:** normal
**Branch:** `feat/goalpilot-implementation` · **Ticket:** none — Ido's question, no map ticket

## The question

*"A session refers me to `c6-log-progress`. Can I open that session from the VS Code
extension?"* — not the CLI; the extension panel.

## What the extension actually does (read out of the shipped bundle, v2.1.228)

Three facts, all from `~/.vscode/extensions/anthropic.claude-code-2.1.228-win32-x64`:

1. **The picker's search matches title and git branch only.**
   `Be.filter(s => LN(s).toLowerCase().includes(q) || s.gitBranch.value?.toLowerCase().includes(q))`
   with `LN(s) = s.summary.value || "Untitled"`. Not the session UUID, not transcript content.
2. **There is no other entry point.** All 22 contributed commands take no session id, and
   the extension registers no URI handler (`activationEvents` is `onStartupFinished` +
   `onWebviewPanel:claudeVSCodePanel`), so no `vscode://…/session/<uuid>` either.
3. **A title is one appended JSONL record**, and it is durable:
   `{"type":"custom-title","sessionId":"…","customTitle":"…"}`, appended to the session's
   own transcript. Resolution order when the list is built is
   **`customTitle` → `aiTitle` → `lastPrompt` → `summary`**, and both head and tail of the
   file are scanned, so an appended line beats the auto-generated title permanently.

**Therefore the title is the only handle the extension gives you, and it is writable.**
The five indistinguishable rows are a direct consequence of fact 3's fallback chain: with no
`customTitle`, the row falls through to `lastPrompt`, and five wayfinder sessions opened with
the byte-identical message `AUTO MODE /wayfinder 12 …`.

## The decision, and it is the agent's

Ido answered both picker questions with *"I couldn't fully understand the options — explain
simply, choose the solution that gives the highest standard, and improve it if you can."*
That is a **delegation** under the hand-back rule: not re-asked in any form, the
*couldn't-understand* half paid once as an explanation in the reply, and the answer
**derived** rather than settled by picking the agent's own Recommended.

**Derived answer, and it landed outside the offered options in two ways.**

1. **Scope: every session with a *verifiable* label, not "all" and not "recent".** The
   offered axis (all / recent / none) was the wrong cut. Cost per file is one appended line
   either way, so "recent" buys nothing; what actually varies is **whether the label is
   trustworthy**. Mention-counting mislabels — it tagged the session that merely *read*
   `c6-log-progress`'s changelog as being it. The tool therefore derives the label from
   **Write/Edit tool calls the session made against `CHANGELOG/<date>/<label>.md`** —
   authorship, not mention. 34 of 60 transcripts have one; the rest are left alone.
2. **The title carries the ticket too: `c9b-calendar-surface · #26`.** This axis was named
   and *dropped* when the question was drafted ("the label is the only string worth
   matching") — wrong, and re-opening the problem is what the delegation asks for. Ido cites
   sessions by label but *browses* by ticket, and the picker shows exactly one line. The
   label stays the prefix, so searching `c6-log-progress` still matches.
   The ticket is resolved from the **issue titles** (`C9b · …` → `#26`), not from the
   changelog head — the head names the map `#12` far more often than the ticket, which is
   the defect the first pass produced (`c9b-calendar-surface · #12`).
3. **Live sessions are skipped, not renamed.** Appending to a transcript a running process
   owns risks interleaving with its own append. Any session with an open tab
   (`~/.claude/sessions/*.json`) or a write in the last 3h is left for the pencil.

## 🧪 Tests

No test layer exists for a tool that writes outside the repo. Verified by **dry run**
instead: 34 named / 8 skipped as live / 17 with no authored label / 1 already named, and
every derived ticket checked by hand against the issue list (`c9b`→#26, `c9d`→#17, `c7`→#14,
`c9a`→#25, `c17`→#38, `c16`→#37, `c4`→#13, `c14`→#23, `c3`→#18, `c15`→#15, `c18`→#39,
`c9c`→#27, `c13`→#32, `c10`→#29, `c1`→#19, `c11a`→#16, `c9e`→#28, `c9f`→#33 — all correct).

## The write pass ran — 34 transcripts named, 0 corrupt

Ido authorised it with a safety precondition (*"make sure it harms nothing and no other
session; if it harms, don't"*), and re-reading the script against that precondition found a
hazard the dry run could not have shown:

> **A transcript whose last line lacks a terminating newline would have the new record glued
> onto it**, corrupting that session's final message. The script now checks the last byte and
> leads with a newline when needed. Measured afterwards: **0 of 60 files** were unterminated,
> so the hazard never fired — but it was a real defect in the tool, not a hypothetical, and it
> would have damaged another session's transcript rather than this one's.

Three further guards, all verified **at write time** rather than assumed from the earlier run:
**no live transcript touched** (9 skipped — 7 open tabs incl. a new one, `3ae5615a`, plus
`c12`/`c12-kickoff` written inside 3h); **every append is additive**, one line, nothing
rewritten; and afterwards **all 34 records re-parsed, plus the line preceding each one**, to
prove no line was joined — 34 valid, 0 corrupt, `·` correctly UTF-8 (`c2 b7`).

Reversal is deleting the last line of the file.

## Not done

- **Nine live sessions keep their auto-titles** — deliberately; they get the pencil, or a
  second pass once released.
- **Route 3 — a session renaming itself at claim time — is drafted but not written.** It
  changes the interaction protocol, so the 🎬 walkthrough gate owns it, and a *delegation*
  is explicitly weaker than a waive: it never grants the skip. The mechanical half is still
  owed before it ships.

## 🧭 Board — this session's row was committed by a sibling before it could commit it

`SESSIONS.md` was written (one row appended, pure insertion) and, minutes later, `git status`
showed it **clean** — the row was already in `HEAD`, swept in by `9c28614` /`71f9413`, neither
of which is this session's. Nothing was lost and the row is intact, but it is the blanket-stage
hazard the 🧭 rule names, observed live: **a sibling's `git add` published an edit this session
had not finished reviewing.** Recorded rather than corrected — a released session's commit is
not this session's to rewrite. Only the two new files below were staged here, by explicit path.

The row **stays Active**: two KB candidates are uningested and route 3 is undrafted.

## The map moved underneath this session, which is worth a line

The frontier answer given at the top of this session (*"#35, #20, #21 — exactly three"*) is
already historical. Within the hour: `c6-log-progress` **released** (#22 closed, `faddfc7`),
`c5-endless-goals` **claimed #21**, and a `c2-task-type` session took **#20** and filed
candidates. So of the three named, two were taken and the third (#35) is the one still open.

## Files

- `scratchpad/name_sessions.py` *(new, not in the repo — offered for `scripts/` if wanted)*
