# KB candidates — `c14-challenge-scoring`, 2026-08-10

Session: `c14-challenge-scoring` · ticket [#23 · `C14`](https://github.com/idomarhaim/Android_Final_Project/issues/23) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) · mode `AUTO MODE`.

Each entry stands alone. No entry may be reconstructed from a chat transcript.

---

## 1 · N-party approval is expressible in a "each principal writes only their own document" rules model, with no new privilege

- **Claim.** When a security model grants each principal write access to exactly one document
  of their own, an *"this change needs everyone's approval"* workflow needs **no new
  privilege, no new collection and no rules change**. Express it as: the proposer writes the
  **proposal** on the document they already own, each principal writes an **approval token**
  in the one document they already own, and a trusted server component applies the change
  when every token matches. The partition that looks like the obstacle is the mechanism.

- **Why.** `C7` §5 examined GoalPilot's `firestore.rules` — only a challenge's owner may write
  the challenge document, and each participant may write only `participants/{uid}` — and
  concluded, in its own words, that *"a pending change awaiting N approvals is representable
  in neither"*. That conclusion is wrong, and the reason it is wrong generalises past
  Firestore: it looks for **one** document that holds the whole pending state, when the
  approval **is** the per-principal write that the model already permits. Concretely:
  `challenges/{id}.pendingMeasure = {changeId, kind, word, mode}` written by the owner,
  `participants/{uid}.approvedChangeId` written by each participant, a Cloud Function
  applying it when every participant row carries that `changeId`.
  **Rejected alternative:** granting the owner write access to participant rows, or hoisting
  the pending change into a new top-level collection with its own rules — both widen the
  privilege surface to buy something the existing partition already expresses. **Second
  rejected alternative:** a client-side "everyone has tapped yes" check, which is exactly the
  trust hole the same ticket was closing elsewhere.
  **Degenerate case worth stating:** a single-participant challenge approves trivially, so
  the common path has no ceremony — an N-party protocol that is heavy at N=1 gets designed
  around rather than used.

- **Destination.** Central KB, `dev/` — a new page. Nearest existing neighbours are the
  security-rules material produced when challenges shipped (`CHANGELOG/2026-08-04/challenges.md`
  in `Android_Final_Project`, which is a project record rather than a KB page). Not `rules/`:
  this is a design pattern, not a change to how the agent behaves.

- **Anchors.** `firestore.rules:42-59` (the partition), `C7`'s hand-off comment on
  [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23#issuecomment-5238477272)
  (the *"nowhere for that to live"* claim), `C14`'s resolution §5
  ([#23](https://github.com/idomarhaim/Android_Final_Project/issues/23#issuecomment-5244202318)).

- **Supersedes.** Nothing. It **corrects** a claim made in a GitHub ticket comment, not in the KB.

- **Status.** Ingestable — `AUTO MODE`, no `rules/` destination, contradicts no standing KB claim.

---

## 2 · "A doesn't sync with B" is sometimes not a missing pipe but a duplicated representation

- **Claim.** When a defect report says *"X does not sync with Y"* and both hold the same
  quantity, check whether X and Y are **the same object modelled twice** before building the
  sync. If they are, the fix **deletes one representation**; building the pipe instead
  doubles the thing that was already wrong and makes the duplication permanent, because from
  then on something depends on both copies existing.

- **Why.** GoalPilot's `R1` reported *"a shared CHALLENGE does not sync with my tasks or with
  my Health Connect"*, reproduced on a device as *"August Steps Race: #2 · 0 steps"* while the
  same user's steps flowed into goals through Health Connect. It reads as unarguable missing
  wiring. But a challenge and a goal, laid side by side, carry the same fields — a title, a
  measure, a starting value, a current value — so the app was maintaining **two
  representations of the same walk**, and the honest fix was to make a challenge score from
  the participant's goal and delete its own number. The tell that distinguishes the two cases
  is cheap: **line the two objects' fields up in a table.** If the second is a subset of the
  first plus framing (dates, participants, visibility), it is the same object with a role,
  not a peer needing a pipe.
  **What was rejected:** a second importer writing Health Connect readings into
  `ChallengeParticipant.score`, which is what the ticket's own enumeration made look like a
  legitimate option, and which would have needed its own dedup, its own backfill and its own
  disagreement-with-the-goal story — three problems that vanish under deletion.
  **Boundary:** this is not "always deduplicate". Two objects holding the same *kind* of
  number for different *purposes* (a cached read model, a snapshot for history) are
  legitimately two. The test is whether either could be **derived** from the other without
  loss — here the challenge score is exactly a windowed sum of the goal's own entries.

- **Destination.** Central KB, `dev/`. Adjacent to the false-fork material already in the
  bundle (`dev/indistinguishable-at-the-boundary.md`, produced by `c9c-calendar-sync`), which
  is about two *states* that cannot be told apart; this is about two *objects* that should
  never have been told apart. Cross-link rather than merge.

- **Anchors.** `C14` resolution §0 ([#23](https://github.com/idomarhaim/Android_Final_Project/issues/23#issuecomment-5244202318)),
  `Challenge.kt` vs `Goal.kt`, `SyncHealthDataUseCase` (writes a `ProgressEntry` against a goal, never a challenge).

- **Supersedes.** Nothing.

- **Status.** Ingestable — `AUTO MODE`, no `rules/` destination, contradicts no standing KB claim.

---

## 3 · ⛔ ALWAYS-ASK · A picker's answerable questions are the ones phrased as a **situation**, not as a **mechanism**

- **Claim.** A fifth failure mode for `rules/question-axis-naming.md`, distinct from the four
  already recorded there and from the granularity amendment `c9c-calendar-sync` parked. The
  axis can be right, the options mutually exclusive, the count small — and the user still
  cannot answer, because the question asks them to evaluate a **mechanism** rather than to
  say what should happen in a **situation they can picture**. The tell is sharp and it is not
  a stall: within one four-question picker, the user answered the scenario question
  **fluently and immediately** and reported not understanding the other three.

- **Why.** This session put four questions to Ido. Three asked him to choose mechanisms —
  *which sources may fill a challenge score*, *who computes the number*, *does
  `ChallengeType` survive creation*. One asked a scenario — *a participant with no Health
  Connect connection joins a step race; what happens?* He answered the scenario without
  hesitation (*they cannot join*) and replied to all three mechanism questions with *"I could
  not fully understand you or what each option implies."* Same session, same user, same
  picker, same care in drafting — so the variable is the question's **form**, not its subject
  or his expertise. The mechanism questions were answerable only by someone who would have to
  simulate the consequence to choose; the scenario question hands the consequence over
  already simulated, and the user supplies the value judgement, which is the only part that
  was ever theirs. This is the ownership sort the rule already mandates, failing at a level
  the current wording does not catch: the questions **were** about his values, and were still
  unanswerable because they were dressed as architecture.
  **The remedy is not to explain more** — the rule already forbids that. It is to **re-ask
  the same decision as a concrete situation** and derive the mechanism from the answer.
  **What this does not claim:** that mechanism questions are never askable. It claims that
  when the same user answers one form and not the other in a single picker, the form is the
  finding.

- **Destination.** `rules/question-axis-naming.md` — an **amendment to a live, shipped rule**,
  so `/kb-ingest` may not take it in **either** mode, and the 🎬 walkthrough duty applies
  because it changes how questions get put to Ido.

- **Anchors.** This session's `AskUserQuestion` call and Ido's reply (recorded in
  `CHANGELOG/2026-08-10/c14-challenge-scoring.md`); `rules/question-axis-naming.md`;
  the adjacent parked amendment in
  [`kb-candidates/2026-08-10-c9c-calendar-sync.md`](2026-08-10-c9c-calendar-sync.md) §3
  (granularity), whose tell is also *"answers fluently with something off the menu"* — these
  two are **neighbours and should be read together**, though they are not the same mode:
  granularity is a wrong **unit** on a right axis, this is a wrong **form** for a right question.

- **Supersedes.** Nothing. It **extends** a rule already in force.

- **Status.** ⛔ **Always-ask — parked for Ido.** Not drainable by any session in either mode.
