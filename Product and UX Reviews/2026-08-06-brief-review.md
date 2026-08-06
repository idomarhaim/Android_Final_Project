# Product / UX brief review — 2026-08-06

Faithful English transcription of
`נקודות לאפליקציה באנדרואיד ששמתי לב אליהם בבריף קצר.docx`, written by Ido in a
quick manual pass over the app before sleep.

**Why this file exists.** The original is free-form Hebrew prose inside a `.docx`
— unreadable by any future agent session and unquotable in a ticket. This is the
citable copy. It is a **transcription, not an interpretation**: each item is
translated as written, including the ones that are vague, and numbered `R1`–`R28`
so everything downstream (TODO entries, issues, wayfinder tickets) can point at a
stable id. Where the original ran several thoughts into one line, they are split
and the split is marked.

Classification, verification and follow-up work live elsewhere and cite these ids:

- [`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md) — the defects and the single-session UX work.
- [`TODO/TODO_FUTURE/ProductModel.TODO.future.md`](../TODO/TODO_FUTURE/ProductModel.TODO.future.md) — the product-model questions bound for a `/wayfinder` map.

---

## Challenges & navigation

**R1** · *reported as a defect* — A shared CHALLENGE does not sync with my tasks
or with my Health Connect. I have to update it from inside the CHALLENGE.

**R2** · *reported as a defect* — You cannot get into the GOALS list from inside
the AREAS list; only via the goals list.

## "Quick add" with the smart task sorter

**R3** — It asks for approval on where to file every task you enter. The
configuration default should be that it does not ask and just does it.

**R4** — The sorter can fail to know where to put the task and then creates a new
GOAL out of it. There needs to be a way to turn GOALS into tasks.

**R5** — It should raise a notification when that has happened — both in-app and
as an external phone notification.

**R6** — There should be a way to complete the task from within "quick add".

## Task entry in general

**R7** — Points must be awarded **only by the AI**, never by a human.

**R8** — A person may optionally type the work duration into a box, but the
default is that the AI estimates it; there should be an icon inside the box for
as long as the person has not entered a number.

**R9** — There must be a predefined format for how the AI evaluates points and
how it estimates time.

**R10** — When tasks under the same goal are updated (including sub-tasks, if
any), it should update the points and the time for every task in that goal as
needed.

**R11** — Worth defining task *types* and letting the AI tag tasks with them
(off the top of my head: writing, thinking, analytical, creative, interpersonal,
communication, physical activity, leisure, chores, and so on).

## Goals

**R12** · *reported as a defect* — There is a mismatch between the scores the
agent gives tasks and the percentages it gives GOALS — for example the
final-project book, whose tasks in practice cover the whole thing.

**R13** · *reported as a defect* — Pressing "complete task" takes too long before
you see the checkbox fill in and the graph update.

**R14** — Something is illogical in LOG PROGRESS, where I can change the
percentages myself.

**R15** — Something is illogical in that defining a new GOAL asks you to choose a
UNIT, and it is not clear what the options are besides percent.

**R16** — The AI should be able to propose a candidate task list for a goal as
**numbered stages**; the user picks the tasks they liked and the numbering
adjusts accordingly.

**R17** — There should be an option for the AI agent to schedule tasks in a
calendar and add reminders accordingly, scheduling both at the event level and at
the hour level where needed — like my exam marathons. There should be a calendar
inside the app, plus an option to sync with Google Calendar, where it opens a
dedicated GoalPilot calendar. And of course it updates the calendar events as
needed, following what actually happens in life.

**R18** — There must be a treatment for GOALS that have no end (for example: buy
flowers for Rachel every two weeks).

**R19** — The agent should propose, where warranted, re-planning and redefining
our goals and tasks. For example, maybe buying Rachel flowers is not a goal but
one of the tasks under the goal of *making Rachel feel courted*. Or maybe
something I first defined as a task in fact holds enough distinct tasks under it
that it could already be a goal. (A goal can live inside a goal — if so, I need
an example.)

**R20** — Need to think about how different *kinds* of goals and tasks are
handled. "Make Rachel feel courted" is a goal that ostensibly can reach 100% but
requires maintenance. Perhaps simply: when a repeat instance of buying flowers
comes round, the percentage drops, and completing it brings it back — a bit like
life with things that need upkeep.

## Motivation, AI and presentation

**R21** — There should be a dedicated daily sentence, practical and inspiring,
for every goal and for every life area. Preferably drawn from a bestselling book
or article, or from a famous, respected, inspiring figure. The sentences appear
both in the home-page feed and inside each goal.

**R22** · *asked as an open question in the original* — For certain goals, should
the sentences relate to the **current** task, where the tasks are numbered?

**R23** — Since the default is a free model, we need to work out which things it
must reason about according to a defined format built for it, and exactly how to
build those formats.

**R24** — Need to build a widget pack, with all the relevant widgets.

**R25** — Need to decide which *fill options* tasks get. For example, for the task
"drink 4 litres a day" I have several fill buttons I can tap more than once
(250 ml, 500 ml, 750 ml, 1 L).

**R26** — Need to decide how charts are handled: do we offer choices, how are they
arranged on the screen, is it only a straight bar/line, or do we also do a donut
or a pie?

## Social

**R27** · *reported as a defect; two separate faults in one line* —
**(a)** you cannot open a photo that has been shared, and
**(b)** a user cannot delete a share they made themselves. Needs fixing.

## Bonus

**R28** — Let people connect using their own LLM API key.

---

## Counting note

The original runs to 27 written lines; this transcription numbers **28** items,
because R27 was left as one item covering two independent faults and R22 — which
the original writes as an explicit question — is numbered separately from the R21
it hangs off. Nothing was added, dropped, or reworded beyond translation.
