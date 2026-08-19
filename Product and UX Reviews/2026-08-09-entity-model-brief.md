# Entity-model brief — 2026-08-09

Faithful English transcription of
[`docs/pre-injested-docs/הגדרה ראשונית של הישויות הבאות - תחום חיים, יעד, אבן דרך, משימה.docx`](../docs/pre-injested-docs/),
written by Ido on 2026-08-09 — *"initial definition of the entities and the
hierarchy of life area / goal / milestone / task"*.

**Why this file exists.** Same reason as
[`2026-08-06-brief-review.md`](2026-08-06-brief-review.md): the original is
free-form Hebrew prose inside a `.docx`, which no future agent session can read
and no ticket can quote. This is the citable copy. It is a **transcription, not
an interpretation** — each item is translated as written, including the parts
that are deliberately open, and numbered `E1`–`E19` so everything downstream can
point at a stable id.

**How it differs from the 08-06 brief, and why that matters.** The 08-06 brief
was an *observation backlog* — things Ido noticed while using the app — and it
was split downstream into defects and undecided questions. This document is not
a backlog. It is Ido **answering** questions the
[wayfinder map (#12)](https://github.com/idomarhaim/Android_Final_Project/issues/12)
already has open, and in the course of answering them it introduces **one entity
and two relationships that exist nowhere in the schema or on the map**. So the
`E`-ids are inputs to open tickets, not new backlog items.

> **The `E` prefix is deliberate.** `R1`–`R28` are 08-06 observations, `C1`–`C15`
> are map decisions, `A`/`D`/`U` are the device pass. `E` is this document, so no
> id collides and every citation says which source it came from.

---

## Terminology

**E1** — *On the Hebrew word for a goal.* Ido writes: "Many times I said
**מטרה** (*matara*), but the more correct wording is probably **יעד** (*ya'ad*)
— as in the app's name → **Goal** Pilot. So from now on I will say **יעד**, and
you need to know that when I said **מטרה** earlier I meant the same thing."

> **Reading note for English readers.** Both Hebrew words render as "goal" /
> "objective" in English, so nothing in the English UI changes. What changes is
> the **Hebrew** term: the Hebrew label for the `Goal` entity is **יעד**, and
> every earlier Hebrew note saying מטרה refers to the same entity. This is a
> ubiquitous-language decision with a Hebrew-UI consequence, so it bears on
> `C15` even though `C15` is closed.

---

## Life area — "תחום חיים"

**E2** — A life area is one of the areas of life the user thinks it is important
to invest in and develop. Each life area contains various **goals**.

**E3** — A life area has **no progress bar that can reach 100%**.

**E4** — Beyond tracking and visualising *"what percentage of my life's time do I
invest in this life area"*, there must **also** be tracking and visualisation of
the **successes (and failures)** within that life area.

**E5** · *worked example* — The life area **"Finance"** holds these goals:
- **Goal:** "Understand real estate."
- **Goal:** "Hold X real-estate assets."
- **Goal:** "Hold a stock-market investment portfolio worth X ₪ that returns Y% a year."

**E6** · *an aside inside E5, left deliberately two-sided* — On the "understand
real estate" goal, Ido writes: the AI agent **might** advise the user to define
something more measurable. "On the other hand, maybe not necessarily — because
one of the things is *to be well-versed in what is current in real estate*, and
that requires some endlessly recurring task. Maybe the AI will advise the user to
sharpen the goal further, for example: *'understand real estate and master the
current topics in the field'*."

---

## Goal — "יעד"

**E7** — A goal is an objective that matters to you **in that area of your life
in its own right** — *not as a means to achieve something else*.

**E8** — A goal **may** contain **milestones** (but need not) and **tasks** that
must be completed in order to reach the goal.

**E9** — Ido identifies **two main kinds of goal**, and explicitly invites a
third:
- **Finite goal** (*יעד סופי*) — once you have achieved it once, it is yours.
- **Infinite goal** (*יעד אין-סופי*) — cannot really be completed permanently;
  it requires constant maintenance.
- *"If you think there is another kind of goal worth adding, you are welcome to
  propose it."*

**E10** · *worked example of an infinite goal* — In the life area
**"Relationships"** (*זוגיות*) there is a goal, one of many: **"my partner will
feel courted"** — something that must be maintained all the time and cannot
really reach 100% and stay there without constant maintenance. Under that goal
there is a task, one of many: **"buy my partner a bouquet every two weeks"** — a
**recurring** task.

**E11** — *The decay mechanic, stated concretely.* "Say I completed all the tasks
I defined for the two weeks and reached 100%. The two weeks pass → the flowers
task **pops up again** → the completion percentage **drops**, until I complete
the task again (and if there are other open tasks)."

---

## Task — "משימה"

**E12** — A task is something that must be done in order to fulfil that goal. It
is **not necessarily important to you in life in its own right**.

**E13** — A task can contain a **sub-task**, which can itself contain another
sub-task, **and so on** — no stated depth limit.

---

## Milestone — "אבן דרך"

**E14** — A milestone is a kind of significant **"sub-goal"** that must be
fulfilled in order to reach the main goal — sometimes there are several — but
which is **not necessarily important to you in its own right**, only as a way to
achieve the main goal.

**E15** — "There is also something psychological here, which is up to the user's
choice — but it also helps us understand what really matters to us and what does
not."

**E16** — A milestone of a given goal **can also be a goal in itself, but not
necessarily**.

---

## Multiple linkage — "קישוריות כפולה"

**E17** — There can be **goals that belong to several life areas**, and **tasks
that belong to several goals**.

**E18** · *worked example of a task under two goals* — **Task:** "run 4 km with
my partner". That task belongs:
- to the **goal** "run 4 km a week", in the **life area** "Health" (or "Sport");
- **and** to the **goal** "do shared-interest activities with my partner", in the
  **life area** "Relationships".

---

## The combined worked example

**E19** — Ido's example exercising every entity at once:

```
Goal 1:            "Be worth (liquid + assets) $100M"
Goal 2:            "Do a software-engineering degree"
                   ** Goal 2 is a goal in its own right AND a milestone of Goal 1.
  Milestone 1:     "Finish year 1 of the degree"
                   ** Milestone 1 is ONLY a milestone — not a goal in its own right.
    Sub-milestone 1.1:  "Pass the course (grade-wise)"
      Task 1:           "Submit 'Assignment 1'"
      Task 2:           "Study for the exam"
        Sub-task 2.1:   "Study topic A"
        Sub-task 2.2:   "Study topic B"
```

> **Transcription note.** The original writes the two goals, the milestone, the
> sub-milestone and the tasks as a flat numbered list; the indentation above is
> the containment the numbering (`1`, `1.1`, `2`, `2.1`) states, not an addition.
> The one thing the original does **not** say is whether Task 1 and Task 2 hang
> off the sub-milestone, the milestone, or Goal 2 — they are simply listed after
> `1.1`. That ambiguity is real and is left in.

---

## What the schema says today, so the delta is visible

Grounded, not inferred — read out of `domain/model/` on 2026-08-09:

| Claim in this document | What the code has today |
|---|---|
| `E17` a goal can belong to **several** life areas | `Goal.lifeAreaId: String?` — **one**, nullable ([Goal.kt:21](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L21)) |
| `E17` a task can belong to **several** goals | `Task.goalId: String?` — **one**, nullable ([Task.kt:9](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt#L9)) |
| `E13` tasks nest arbitrarily deep | **No parent-task field exists.** `Task` is flat ([Task.kt](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt)) |
| `E14`–`E16` milestones | **No milestone entity exists** anywhere in `domain/model/` |
| `E9`/`E11` infinite goals and recurring tasks | **Nothing expresses recurrence** — no repeat, cadence or next-due field on `Task` or `Goal` (already recorded on the map) |
| `E11` progress can drop back down | `Goal.progressFraction` is `currentValue / targetValue` **clamped to `0..1`**, and `isComplete` latches at `>= 1f`. Nothing decays it ([Goal.kt:32-38](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L32-L38)) |
| `E3` a life area has no 100% bar | Matches — `LifeArea` carries no target or progress field at all ([LifeArea.kt:19](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/LifeArea.kt#L19)) |
| `E4` successes/failures per life area | The time-allocation chart exists; there is **no** success/failure visualisation per area |

So five of the eight rows are **schema changes over live data in
`goalpilot-56e30`**, which is exactly the Firestore-migration fog the map already
records under *"Not yet specified"*.

---

## Where these items land — routing only, not answers

**This session does not resolve any ticket**, and deliberately writes to none of
them: `#12`, `#13`, `#14` and `#29` are all in live sessions' `Owns` columns on
[`SESSIONS.md`](../SESSIONS.md). This table is a reading list for those sessions.

| Ticket | Items that bear on it | What kind of bearing |
|---|---|---|
| [#13 · `C4` goal↔task ontology](https://github.com/idomarhaim/Android_Final_Project/issues/13) | `E7`, `E12`, `E14`, `E16`, `E19` | **The discriminator, and the worked example the ticket asked for.** See below |
| [#21 · `C5` endless & maintenance goals](https://github.com/idomarhaim/Android_Final_Project/issues/21) | `E9`, `E10`, `E11` | The two goal kinds are **named**, and the decay mechanic is stated concretely |
| [#14 · `C7` what is a unit](https://github.com/idomarhaim/Android_Final_Project/issues/14) | `E3`, `E6`, `E11` | A life area is explicitly **unmeasured**; `E6` leaves "make it measurable" as an AI *suggestion*, not a requirement |
| [#18 · `C3` points vs goal progress](https://github.com/idomarhaim/Android_Final_Project/issues/18) | `E11`, `E19` | Progress must be able to **go down**, and roll up through milestones |
| [#24 · `C8` AI-proposed numbered plans](https://github.com/idomarhaim/Android_Final_Project/issues/24) | `E19` | `E19`'s `1` / `1.1` / `2.1` numbering is what a "numbered stage" looks like to Ido |
| [#19 · `C1` points and time](https://github.com/idomarhaim/Android_Final_Project/issues/19) | `E13` | Nested tasks change what a point total is summed over |
| `C15` (closed, [#15](https://github.com/idomarhaim/Android_Final_Project/issues/15)) | `E1` | The Hebrew label for `Goal` is **יעד**, not מטרה |

### The one finding worth pulling out

`E7` and `E12` give a **goal↔task discriminator that is neither measurement, nor
size, nor endurance**: a **goal** is what matters to you *in its own right*; a
**task** is a *means* — "not necessarily important to you in life in its own
right". `E14` then places a **milestone** on the instrumental side of that same
line, and `E16` says a thing can be a milestone of one goal while being a goal in
its own right — which is precisely how `E19` resolves the nesting question `#13`
asks: **goals do not nest inside goals; they are joined by milestones**, and the
same object can wear both hats.

That is a **fifth answer** to the question `#13`'s question picker put to Ido,
and it is not a refinement of any of the four it offered. It is also
*intrinsic/instrumental*, i.e. a judgement about the user's own values, which no
free-model classifier can compute from a title — so whichever mechanical test
`#13` lands on, it is a proxy for this, not this.

### New scope this document creates, on nobody's ticket

Filing these is a charting act, so this session **flags and does not file**:

1. **The milestone entity** (`E14`–`E16`, `E19`) — a third structural level
   between goal and task, itself nestable (`E19`'s sub-milestone `1.1`), and
   optionally identical to a goal. Nothing on the map covers it.
2. **Many-to-many linkage** (`E17`, `E18`) — goal↔life area and task↔goal both
   become collections. This breaks the time-allocation chart's arithmetic (a task
   under two goals in two areas is counted where?), which is a decision, not an
   implementation detail.
3. **Sub-tasks at arbitrary depth** (`E13`) — the app has none, and every
   roll-up (`points`, `progressContribution`, `estimatedMinutes`) is currently
   written assuming one level.
4. **`E9`'s open invitation** — Ido explicitly asks whether a *third* kind of
   goal should exist. That is a question addressed to the agent, and it is
   unanswered.
5. **`E4`'s success/failure visualisation per life area** — a presentation
   requirement that is not in `R1`–`R28` and not on `C12` ([#31](https://github.com/idomarhaim/Android_Final_Project/issues/31)).

---

## Counting note

The original runs to 26 written lines under 6 headings; this transcription
numbers **19** items, because the headings and the worked examples' component
lines are folded into the item they illustrate (`E5`, `E10`, `E18`, `E19`) rather
than numbered separately. Nothing was added, dropped, or reworded beyond
translation — every interpretive remark is in a blockquote or in a section marked
as such.
