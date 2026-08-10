# KB candidates — `c3-points-currency`, 2026-08-10

Session: `c3-points-currency` · repo `C:\Dev\Android_Final_Project` · branch
`feat/goalpilot-implementation` · mode `AUTO MODE`.

Each entry stands alone. No entry here may be reconstructed from a chat transcript —
if it is too thin to write a page from, it stops and asks Ido.

---

## 1. The false-fork check has to run **one hop wider** than the two quantities named

**Status:** `always-ask` — destination is `rules/`, which `/kb-ingest` may not take in
**either** mode, and it *supersedes* wording in a standing rule file. Two independent
reasons it waits on Ido; the 🎬 walkthrough rule owns it.

**Claim.** `C:\Dev\JARVIS\rules\question-axis-naming.md` tells the agent, before drafting
a picker, to *"grep the code for a write path between them"* when two quantities look
like rival answers to one question — because if one already produces the other, the fork
is false and every option inherits the false premise. **The clause is right and the
search width is wrong.** Grepping for a path **directly** between the two named
quantities is not enough: the path routinely runs through a **third** quantity that
neither option mentions. The check must be run over each quantity's *derivation
closure* — what is computed from it, and what it is computed from — not over the pair.

**Why. This is a first-hand failure, not a hypothetical.** Resolving GitHub issue #18
(`C3`, *are task points and goal progress one currency or two?*) in
`C:\Dev\Android_Final_Project`, the check was run exactly as written: grep for a write
path between `Task.points` and `Goal.currentValue`. It found one bridge,
`Task.progressContribution`, which the ticket already named — so the fork was declared
real and a three-option picker was drafted on it.

The fork was **false**. The path runs
`points → TaskDuration.fallbackMinutes(points) = points × 3 → minutesOf(task) → the
time-allocation chart` — through `estimatedMinutes`, a **third** quantity that appears in
neither option, in neither the ticket body, nor in the map's own *Grounded facts* block
(`#12`), which had asserted for two days that the two were *"independent by
construction"*. `app/src/main/java/com/idomarhaim/goalpilot/domain/model/TaskEstimate.kt`
lines 40 and 45 are the whole proof, and reading them changed the resolution: the answer
went from *"keep them separate"* to *"invert the existing constant so the derived one
stops being the root"*.

**Two things make this generalisable rather than a one-off miss.**

1. **The direct-path grep returns a true negative and reads like a true positive.** There
   genuinely is no line writing `points` from `currentValue` or back. The check *passed*.
   Nothing in the output signals that the width was wrong — which is exactly the failure
   mode a checklist item cannot self-report.
2. **The tell the rule already documents fired, and was mis-read — and it is now an
   ambiguous tell, which is the sharper half of this entry.** The rule says the
   after-the-fact symptom of a skipped fork check is that *"the options are hard to tell
   apart on their merits and the user stalls rather than picking."* Ido did not stall — he
   said outright, on **both** questions in the batch, that he could not understand the
   options, and handed the choice back. That sentence is **already spoken for**: it is the
   recorded tell of the **ownership** failure (`c13-byo-api-key`, *the question was never
   the user's to answer*). So *"I could not understand the options"* now has **two**
   distinct causes on this map's record, and the live rule maps it to one. The session
   read it as ownership-or-wording, reached for the *make-the-question-smaller* remedy,
   and found the false premise only later, while grounding the answer in code.
   **Consequence for the rule: on that answer, re-run the fork check wider *before*
   reducing the picker** — premise first, wording second, because reducing a picker built
   on a false premise produces a smaller picker that is still unanswerable.

**This is not a fifth failure mode — say so explicitly, so an ingest does not file it as
one.** Four are on record from this map: **framing** (`c16`), **coverage** (`c10`),
**ownership** (`c13`), and **granularity** (`c9c-calendar-sync`, parked alongside this
entry in `kb-candidates/2026-08-10-c9c-calendar-sync.md` §3). Those four are each a *new
way for an axis to be wrong*. **This one is not about the axis at all** — it is a defect
in the **procedure that is supposed to catch a wrong premise before an axis is drawn**,
and the clause it amends already exists. It should land as a refinement of that clause,
never as a fifth bullet in the list of modes. If `/kb-ingest` finds itself adding a
fifth mode, it has misread this entry.

**What was considered and rejected as the fix.** *Widening the grep to every quantity in
the file* — too coarse to be a rule, and it would fire on unrelated fields. *Requiring
the check on every picker regardless of shape* — the rule already scopes it to
rival-quantity forks, and that scoping is correct. The narrow, checkable change is the
**closure**, not the frequency: for each of the two quantities, list what is computed
from it and what it is computed from, then look for an intersection.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — an edit to the existing
*"check the fork is real"* clause, not a new rule. Adds (a) the derivation-closure width,
(b) the *"I don't understand the options"* → re-check-the-premise-first ordering.

**Anchors.**
- `C:\Dev\JARVIS\rules\question-axis-naming.md` — the clause being amended. **Shipped and in force** as of 2026-08-10 (`rules-drafts-ship`), per `c9c-calendar-sync`'s ingest, which corrected an earlier candidate that had described it as an uncommitted draft. So this is an amendment to a **live** rule
- `app/src/main/java/com/idomarhaim/goalpilot/domain/model/TaskEstimate.kt:40,45` — the write path the direct grep missed
- https://github.com/idomarhaim/Android_Final_Project/issues/18#issuecomment-5243944781 — §0 of the resolution, which states the finding in the project's own terms
- `CHANGELOG/2026-08-10/c3-points-currency.md` — *"How the question was put, and how it came back"*
- `kb-candidates/2026-08-10-c9c-calendar-sync.md` §3 — the **granularity** mode, parked the same day against the same rule file. **Ingest the two together or neither**: they touch adjacent clauses of one rule, and taking one alone leaves the *"I could not understand the options"* tell mapped to a single cause when this entry shows it has two
- `SESSIONS.md` → *Recently released* under `c10-quote-feed`, `c13-byo-api-key`, `c16-milestone-model` — the three earlier picker failures, for the not-a-fifth-mode argument above

**Supersedes.** The *"check the fork is real"* clause in
`question-axis-naming.md` as currently worded — it is not contradicted, it is
**too narrow**, and the amendment is a widening rather than a replacement. It also
narrows the rule's stall-tell sentence, which currently implies one cause for an answer
this session showed has two. Because it rewrites committed normative text, this is
always-ask in both modes on that ground alone, independently of its `rules/`
destination.
