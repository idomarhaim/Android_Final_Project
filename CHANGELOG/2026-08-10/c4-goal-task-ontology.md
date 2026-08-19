# `c4-goal-task-ontology` — the discriminator turns out to be an edge, not a property

**Session:** `c4-goal-task-ontology` · **Invocation:** `/wayfinder 12 13` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL throughout) ·
2026-08-10.

One ticket resolved, which is the skill's limit. **No code was touched** — this map
ships no code, and that held: every file under `app/` and `functions/` was read and
none was edited.

## What changed

| | |
|---|---|
| Resolved | [#13 · `C4` What is the goal↔task ontology?](https://github.com/idomarhaim/Android_Final_Project/issues/13) — closed, with the full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*; **one new fog patch** (`E4`); the *Firestore migration* patch widened from two tickets to five; a **second source document** recorded in *Where this came from* |
| Tickets created | [#37 · `C16`](https://github.com/idomarhaim/Android_Final_Project/issues/37) the milestone entity · [#38 · `C17`](https://github.com/idomarhaim/Android_Final_Project/issues/38) many-to-many linkage · [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39) sub-task depth — all three children of #12, all three on the frontier |
| Routed, not filed | `E9`'s *"is there a third kind of goal?"* → a comment on [#21 · `C5`](https://github.com/idomarhaim/Android_Final_Project/issues/21). `E4`'s per-area success/failure view → the map's fog. Both on Ido's instruction |
| Unblocked | [#25 · `C9a` what does it mean to schedule a task](https://github.com/idomarhaim/Android_Final_Project/issues/25) — **one ticket, not four** (see *What the graph actually did* below) |
| Frontier now | [#25](https://github.com/idomarhaim/Android_Final_Project/issues/25) `C9a` · [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) `C13` · [#37](https://github.com/idomarhaim/Android_Final_Project/issues/37) `C16` · [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) `C17` · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` — plus #14 and #29, held by live sessions |

## The session was overturned mid-flight, and that is the headline

This session opened by grounding the ticket in code and putting a question picker to
Ido with four candidate discriminators: **measured vs done**, **size/effort**,
**endures vs completes**, **let the AI decide**. He stopped it before answering:

> *"Your four options are all properties of the OBJECT. My discriminator is a property
> of my RELATIONSHIP to the object."*

The answer had already been written — on 2026-08-09, in a source document this session
did not know existed, transcribed as `E1`–`E19` by the `entity-model-intake` session
(`e5916be`). **The picker was superseded, not supplemented**, and none of its four
options was a refinement of the real answer.

**What this session got wrong, precisely:** it read `AGENTS.md`, `SESSIONS.md`,
`kb-candidates/`, the map, the ticket, the 08-06 brief and the TODO file — but it did
**not** list `Product and UX Reviews/` or read `git log` for source documents added
after the map was charted. The two most recent commits at session start were both
`entity-model-intake`, and their subject lines say what they contain:
*"the entity definitions arrive mid-map, and they answer C4 with a word no ticket
used."* The evidence was one `ls` away and in the log this session did read.

## The decision

### The line is intrinsic vs instrumental

A **goal** is what Ido wants for its own sake (`E7`: *"not as a means to achieve
something else"*). A **milestone** (`E14`) and a **task** (`E12`) are both means —
*"not necessarily important to you in its own right"*, the same phrase for both.

The four object-property proxies do not approximate this. They **fail in both
directions on Ido's own examples**:

| His example | The proxy says | He says |
|---|---|---|
| `E5` "Understand real estate" | unmeasurable → task | **goal** |
| `E19` "Finish year 1 of the degree" | perfectly measurable → goal | **milestone only, explicitly not a goal** |

`E6` confirms it from the other side: making a vague goal measurable is something the
agent **may suggest**, and Ido immediately argues against needing it.

### It is an edge, not a type — and `E16` is the proof

`E16` says a milestone of a goal can also be a goal in itself, and `E19` exercises it:
"do a software-engineering degree" is a goal in its own right **and** a milestone of
"be worth $100M". Nothing about the object differs between the two readings. What
differs is which edge you look along:

- an **intrinsic edge**, from the *user* to the object — makes it a goal;
- an **instrumental edge**, from *another object* to it — makes it that object's milestone.

So **"goal" and "milestone" are roles carried by edges, not types carried by objects.**
That is the load-bearing sentence, and it is what makes the whole thing cheap: delete
Goal 2's intrinsic edge and it becomes a pure milestone; add one to Milestone 1 and it
becomes a goal. No object is created, moved or destroyed — **but only if the model
stores the edge.** Store a type instead and every promotion becomes a migration. That
is now [#37 · `C16`](https://github.com/idomarhaim/Android_Final_Project/issues/37)'s
central question.

### Goals do not nest

`R19` asked *"can a goal live inside a goal — and if so, I need an example."* The answer
is **no, and the question contains a category error**: what looks like nesting is one
object carrying two edges. `E19` is the worked example he asked for, redrawn as edges.

**The Rachel/partner case is not evidence for nesting.** `E10`'s *"buy my partner a
bouquet every two weeks"* under *"my partner will feel courted"* is a **recurring task**
— that is `C5`'s recurrence question, and no milestone appears in it. The case for the
milestone layer rests on `E19` alone.

### The second line, and why the two differ in kind

`E12` and `E14` put milestones and tasks on the same side, and both nest (`E13`, `E19`),
so a second discriminator is needed: a **milestone is a state you reach**, a **task is
work you do** — plus `E14`'s "significant" and `E15`'s "up to the user's choice".

**Line 1 is not computable at all; line 2 is partly computable.** That asymmetry is the
finding, and it is what every downstream AI feature has to respect.

### The rule it leaves behind

> **The app may act silently on instrumental structure. It must ask before asserting an
> intrinsic edge.**

Filing a task is instrumental and reversible, so `R3`'s *"stop asking approval on every
task"* survives. Creating a goal claims to know what Ido values, so it never happens
without him.

## Two things found by checking rather than assuming

**1 · The sorter's goal-invention has no floor, and the client throws away the number
that would give it one.** `classifyTask` returns `confidence: 0..1`, and on total
failure returns `confidence: 0` with the task title handed back as a goal name
(`functions/src/index.ts:118-133`). `DashboardViewModel.confirmSmartAdd` never reads
`confidence` and creates the goal regardless (`:211-221`). **`R4` is that branch having
no floor** — so "there needs to be a way to turn GOALS into tasks" is a repair for
damage that should stop being caused.

**2 · The inbox is *not* free, and this session nearly said it was.** The draft
reasoning was: `Task.goalId` is already nullable, the dashboard already observes
`observeTasks(null)`, and the time chart already has an "Unassigned" bucket — therefore
an unfiled task already renders and the fix costs nothing. Reading
`DashboardViewModel.uiState` rather than trusting the call site showed the dashboard
**counts** tasks (`doneTasks`, `totalTasks`, `completedTasksLast7d`) and **lists** none,
while `GoalDetailViewModel` filters by `goalId`. An unfiled task today is **counted
everywhere and reachable nowhere.** The inbox costs one surface — a spec line, not a
ticket.

## What the graph actually did — and a correction to this session's own claim

This session's board row and opening reply said `C4` was *"the map's root; #18, #21,
#24, #25 unblock behind it."* The first half is right and the second overstated it.
`#13` was blocking four tickets, but three of them have **other** open blockers:

| Ticket | Also blocked by | Takeable now? |
|---|---|---|
| [#25](https://github.com/idomarhaim/Android_Final_Project/issues/25) `C9a` | — | **yes** |
| [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21) `C5` | #18 | no |
| [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) `C3` | **#37, #38** (added by this session) | no |
| [#24](https://github.com/idomarhaim/Android_Final_Project/issues/24) `C8` | #19, **#37** (added by this session) | no |

**Resolving the root made three tickets more blocked, not less.** The entity brief
deepened the map rather than flattening it: `C3` cannot say whether points and progress
are one currency until it knows whether a milestone layer sits between them and whether
one task can feed two goals. The frontier still has five unassigned tickets, but three
of them are new.

## The same mistake, twice, and the second one was also caught by Ido

The overturned picker was one instance of a pattern, and this session repeated it
before the session was over. Both times it **reported a view of the repo instead of
looking**, and both times the correct answer was one directory listing away:

1. **Source documents** — read the map, the ticket and the 08-06 brief; did not list
   `Product and UX Reviews/`, so the 08-09 entity brief was invisible.
2. **KB candidates** — reported `kb-candidates/` as holding **two** un-drained files.
   It holds **five**, and the two missed ones (`2026-08-09-c9f-consent-screen-state.md`,
   `2026-08-09-entity-model-intake.md`) were committed on 08-09, before this session
   started.

The second is worse than the first, because the board **already said so**: the
`entity-model-intake` released row reads *"three pending files are now unowned"* and
the `c9f-consent-screen-state` row reads *"the two files pending … are now unowned"*.
This session read that file end to end and still published a contradicting count.

Corrected, and the true state:

| File | Entries | Owner |
|---|---|---|
| `2026-08-08-c9d-calendar-scopes.md` | 3 | released |
| `2026-08-08-fix-task-completion-feedback.md` | 5 | released — **#5 always-ask** (may supersede a standing claim) |
| `2026-08-09-c9f-consent-screen-state.md` | 5 | released — **#1 always-ask** (arguably `rules/`) |
| `2026-08-09-entity-model-intake.md` | 3 | released — **#1 always-ask** (arguably `rules/`) |
| `2026-08-10-c4-goal-task-ontology.md` | 5 | this session |

**21 entries, 5 files, every pre-existing one unowned, at least 3 always-ask in both
modes.** None drained here — normal mode, and four of the five belong to other
sessions. This is now a sitting of its own, not a tail-end chore.

## Deliberately not done

- **No write to [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14).**
  The 08-09 brief routes `E3`/`E6`/`E11` to `C7`, but `#14` is in the live
  `c7-what-is-a-unit` row's **Owns** column (§5 rule 2). The routing is already recorded
  in the committed brief, which is where that session will read it.
- **No comment spray on `#18`/`#19`/`#24`/`#15`.** The brief's routing table is
  committed and does that job; duplicating it into issue comments creates a copy free to
  rot away from the file.
- **`E1` (the Hebrew label for `Goal` is יעד, not מטרה) did not reopen
  [#15](https://github.com/idomarhaim/Android_Final_Project/issues/15).** It is recorded
  in `C4`'s resolution as a ubiquitous-language line for `docs/PRODUCT_v0.3.md`, along
  with the observation that the ontology now needs Hebrew labels for all four entities —
  תחום חיים / יעד / אבן דרך / משימה — because `C15` put every app-authored word behind
  the picker.

## Derived rather than asked

Flagged so they can be overturned as cheaply as they were taken. §6 of the resolution
(**the sorter must never create a goal**; low confidence files unfiled) and §7 (**the
re-plan trigger is deterministic arithmetic, the model drafts only on demand**). Both
follow from `E7`/`E12` plus committed material — `C11a`'s measured 2x swing in judgement
numbers, and the map's standing rule that an AI feature needs a non-AI fallback beside
it. §1–§5 and §8 are read straight off `E1`–`E19`.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, `firestore.rules` or Cloud
Functions file was created or modified — the only writes were GitHub issues, this
changelog, `SESSIONS.md`, `kb-candidates/` and the changelog index. Verification was
**structural**: the map graph was queried back out of GitHub after every mutation
(25 children, `#13` closed, `#37`–`#39` present as children with the four intended
blocking edges, frontier re-derived as OPEN ∧ no OPEN blocker ∧ unassigned), and every
claim about the code resolves to a file and line that was read this session.

Layers deliberately not run and why: JVM unit, instrumented, and `firestore-tests/` all
test code that did not change.

## Singletons and shared state

**None taken** — no `#gradle-daemon`, neither AVD, live `goalpilot-56e30` never
contacted, no GROQ call. `SESSIONS.md` was **leased twice** (once to claim, once to
correct the row's date from 08-08 to 08-10 after checking the clock rather than
inferring it from the newest changelog folder), granted immediately both times.

**The map body `#12` is contended by three sessions** — `c7-what-is-a-unit`,
`c10-quote-feed` and this one all list it in **Owns**, and it carries no lease. Handled
as `c15-language-switching` did: fetched to a file, hashed, edited offline, re-fetched
and compared byte-for-byte immediately before writing. **No drift, no clobber.**
