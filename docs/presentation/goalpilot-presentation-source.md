<!-- Written 2026-08-24 by session `presentation-source`. -->
<!-- Purpose: a single self-contained source document to drop into a Gemini / NotebookLM notebook,
     from which a presentation about GoalPilot can be generated. Everything a deck needs is here;
     no other file has to be uploaded alongside it. -->

# GoalPilot — presentation source

**What this document is.** A complete, self-contained brief on one product: the need it
addresses, why the tools people already use cannot address it, and what GoalPilot does about
it — feature by feature, with each feature tied back to the specific gap it closes.

**What it is for.** Feeding to an AI notebook (Gemini / NotebookLM) so it can generate a
presentation. Part 9 contains a ready-made slide-by-slide outline; Parts 1–8 contain the
substance the slides draw on; Parts 10–12 contain the phrasing, the vocabulary and the
accuracy guardrails.

---

## 0 · Instructions to the notebook

Read this section before generating anything.

1. **This file is the only source of truth.** Do not add features, statistics, competitor
   claims or user numbers that are not written here. GoalPilot is a real, working Android
   application (v0.4.0), not a concept — every feature described below exists and has been
   demonstrated on a device.
2. **Language.** This source is written in English. If the audience is Hebrew-speaking,
   generate the deck in Hebrew and use the term table in **Part 11** for the domain
   vocabulary — those four terms are the product's own words and should not be re-invented.
3. **Tone.** Calm and concrete. The product's whole argument is that it refuses to flatter
   the user; a deck full of exclamation marks contradicts the thing it is describing.
4. **Do not turn Part 5 into filler.** The honesty rules there are the most distinctive part
   of the product and are what separate it from the category. They deserve slides.
5. **Read Part 12 before writing a single caption.** It lists the six things that must not
   be said about this product, each for a specific reason.

---

## Part 1 · The need

### 1.1 The thing everybody has

Everyone carries a list of things they mean to do with their life. Get fit. Learn the
instrument. Finish the degree. Sleep properly. Be better to the people they love.

These are not tasks. They are **directions** — and that is exactly the problem.

### 1.2 Why the list never moves

**None of it is due on Tuesday.** A meeting is due Tuesday. A deadline is due Tuesday. An
exam is due Tuesday. "Get fit" is due never, so it loses to whatever *is* due — quietly,
every single day, without a single visible moment of failure. Nothing crashes. Nothing turns
red. The year just ends and the saxophone is still in its case.

This is the core mechanic of the need, and it is worth stating precisely:

> **Goals that matter fail by never being scheduled, not by being abandoned.**

### 1.3 The three things a person actually wants to know

1. **What should I do today** — that actually serves something I care about, rather than
   something merely urgent.
2. **Is there room for it** — is the day I am promising it to already full?
3. **Is my life moving in the direction I chose** — over a month, a quarter, a year. Not
   *did I tick boxes*, but *where did my hours actually go*.

Question 3 is the one nobody can answer about their own life, and it is the one this product
was built to answer.

### 1.4 The secondary need: motivation without self-deception

Progress that is invisible does not motivate. But progress that is *faked* — streaks, scores,
a badge for opening the app — motivates for two weeks and then insults the user. The need is
for a system that makes progress **visible and true at the same time**, which is harder than
either one alone.

---

## Part 2 · The gap in what already exists

There is no shortage of apps. There is a shortage of apps that hold the **whole** problem.
Each existing category holds one piece of it and structurally cannot hold the others.

### 2.1 The category-by-category gap

| Category | What it holds | What it structurally cannot hold |
|---|---|---|
| **To-do lists** | the task | **the reason.** A task in a list has no goal above it, so a completed list tells you nothing about whether your life moved |
| **Calendars** | the hour | **the goal.** An event is an hour with a title; it does not know what it serves, so "my week was full" and "my week was well spent" are the same picture |
| **Habit trackers** | the streak | **the outcome.** A streak measures consecutive compliance, not achievement — and breaks permanently on one bad week, which punishes exactly the person who needs it |
| **Goal / journaling apps** | the intention | **the execution.** A goal you wrote down and never scheduled is a wish with a database row |
| **Fitness & health apps** | one domain's numbers | **everything else.** Steps and sleep sit in their own silo and never become progress on anything you actually chose |
| **General AI assistants** | the conversation | **the state.** A chatbot can advise you brilliantly today and knows nothing tomorrow — there is no structure underneath for the advice to change |

### 2.2 The four structural failures

Underneath the table are four failures that repeat across the whole category:

1. **The pieces never meet.** The task lives in one app, the hour in a second, the outcome in
   a third. So the one question worth asking — *is my life moving where I chose?* — is the one
   question none of them can answer, because answering it requires all three pieces to be the
   **same object**.

2. **Structure is expensive, so nobody builds it.** Every one of these tools asks the user to
   do the filing: pick the project, pick the label, set the estimate, choose the priority. The
   people who most need the structure are the least likely to maintain it, so the structure
   decays and the app becomes another list.

3. **The numbers are dishonest.** A goal you have not measured shows as **0 %**. A week you
   have not reached yet counts as a failure. Time spent on things you never filed disappears
   from the report entirely. Each of these is a small lie, and together they make the summary
   screen worse than useless — it is *confidently* wrong.

4. **Motivation is bolted on, not derived.** Points for opening the app. Badges for streaks.
   A leaderboard measuring who logs the most, not who achieved the most. The gamification is
   decorative because it is not connected to anything real.

### 2.3 What is actually missing

Not another list. **The connection.**

> A goal is only real when the task under it, the time it costs, and the progress it produces
> are the **same object**.

That single sentence is the product thesis, and everything in Part 4 is a consequence of it.

---

## Part 3 · The answer — GoalPilot

### 3.1 The claim, in one sentence

> **Your life areas, your goals, your tasks and your hours in one structure — so progress is
> something the app can compute, rather than something you have to feel.**

### 3.2 The structure that makes it possible

GoalPilot's data model is four levels deep, and every level is connected to the one above it:

```
LIFE AREA        the parts of your life you decided are worth investing in
   |             (Health · Studies · Career · Relationships — yours to define)
   v
GOAL             a direction, optionally with a measure ("run 100 km", "10 sessions")
   |
   v
TASK             a unit of work, with an estimated difficulty and duration
   |
   v
OCCURRENCE       that task, on a day — and sometimes at an hour
```

Because the chain is unbroken, three things become computable that are guesswork everywhere
else:

- **Progress** rolls *up*: doing a task moves its goal, which moves its life area.
- **Time** rolls *up*: an hour spent is an hour attributed to a life area — so "where did my
  year go" is arithmetic, not introspection.
- **Load** rolls *down*: the app knows how many hours you have promised to a given day,
  because every promise is an occurrence with a duration on it.

### 3.3 The two ideas that make it usable

A four-level structure is only an asset if maintaining it is free. Two design decisions do
that work:

1. **The AI does the filing.** You write a sentence the way you would say it to a friend.
   The app decides which goal it belongs to, how demanding it is, how long it will take and
   what it is worth — then shows you, and saves nothing until you agree.
2. **The app refuses to invent numbers.** Where it does not know, it says so, in words, on the
   screen. This is what makes the computed numbers worth reading (Part 5).

---

## Part 4 · The product, feature by feature

Each feature below is written as: **what it is → what you see → which gap it closes.**

### 4.1 Smart add — a sentence becomes a filed, priced, scheduled task

- **What it is.** A single text field. You type *"Practice saxophone for twenty minutes on
  Sunday."* The AI reads it and returns: which of your existing goals it belongs to (or a
  proposal for a new one), which life area, how demanding it is, how long it will take, and
  what it is worth in points.
- **What you see.** A confirmation card — *filed under "Learn to play the saxophone", 20m,
  +7 points* — **before** anything is saved. One tap undoes it.
- **Gap closed.** #2, *structure is expensive.* This is the feature that makes the four-level
  model free to maintain. The user does zero filing and gets full structure.

### 4.2 AI estimation — difficulty, duration and value

- **What it is.** For any task, the model judges **how demanding** it is (one of three words)
  and **how long** it takes in minutes. The app then **computes** the point value from those
  two judgements: `points = round(minutes ÷ 3) × difficulty`.
- **Why the split matters.** The model is never allowed to hand back a score. It supplies
  *judgements*; the app owns the *arithmetic*. That is what keeps points comparable across
  every task, every goal and every user — a model that invented point values would drift, and
  the leaderboard would be meaningless.
- **Gap closed.** #4, *motivation bolted on.* Points here are a function of real effort
  (minutes) and real difficulty, so the gamification is measuring the thing it claims to.

### 4.3 The AI coach — five distinct capabilities, not a chatbot

GoalPilot's AI is not a conversation window. It is five narrow, structured capabilities, each
with a defined input and a validated output shape:

| | Capability | What it does |
|---|---|---|
| **A** | `estimate` | difficulty + duration for a task |
| **B** | `plan` | proposes a draft plan for a goal, which you can adjust |
| **C** | `daily` | one practical line you could act on today, per goal |
| **D** | `classify` | routes a free-text task to the right goal and life area |
| **E** | `measure` | proposes a concrete measure for a goal that has none — drawn from what you have actually been logging, so you are not asked to invent a metric before you are allowed to start |

- **Bring your own key.** GROQ is the default and works with no setup. Users who prefer it can
  enter their own **OpenAI / Anthropic / Gemini** key in Settings; it is stored **encrypted on
  the device** and never leaves it. Everything works without one.
- **Failure contract.** A field that fails validation is **absent** — never a null, never a
  default, never a substitute. The app would rather show nothing than show a made-up value.
- **Gap closed.** #2 and #3. The AI removes the filing cost *and* is architecturally prevented
  from fabricating the numbers.

### 4.4 The calendar — goals become hours

- **What it is.** Everything with a time on it lands on a real calendar grid inside the app.
  Not a separate calendar holding a separate list — the **same** tasks, under the **same**
  goals, on the hours you gave them. Day / three-day / week views.
- **The load bar.** Across the top of every day is a bar showing how full that day already is
  *before* you add to it. When it turns, the app is telling you that you have promised away
  more hours than the day contains.
- **Drag to reschedule.** Press and hold a block, pick it up, put it where it should have
  been. When the thing you moved repeats, it asks the only question that matters: *this one,
  or this one and every one after it?*
- **Four kinds of commitment**, distinguished by what a *miss* actually means — because "the
  day passed" and "late, and still owed" are not the same event and should not look the same:

  | Kind | What it is | A miss means |
  |---|---|---|
  | **All-day** | a day with no slot | the day passed |
  | **Deadline** | a moment you owe something by | **late, still owed** |
  | **Block** | a span of time you are inside | the slot is gone |
  | **Span** | days, not hours | the window closed |

- **Gap closed.** #1, *the pieces never meet.* This is the join no other app makes: the task,
  the goal it serves and the hour it costs are one object, so moving one moves all three.

### 4.5 Google Calendar sync — it competes where the competition is

- **What it is.** GoalPilot writes its scheduled work into Google Calendar, onto **a calendar
  of its own**, so it never touches anything you already keep there.
- **Why it matters.** Your goals now appear beside your lectures and your meetings — which is
  the only place they can ever compete with them. A goal in a goals app loses to a meeting in
  a calendar app by default.
- **Two-way awareness.** When something disappears from that calendar, the app notices and
  asks whether you meant it.
- **Gap closed.** #1. The structure reaches out into the tool the user already lives in
  instead of asking them to move.

### 4.6 Google Tasks import — your existing list, filed

- **What it is.** Pulls your open Google Tasks in and files each one under the right goal
  using the same classifier as smart add. Deduped against what is already there. You review
  every item before a single one is saved.
- **Life areas from list names.** Your Google Tasks **list names already are** the areas of
  your life, so GoalPilot can adopt them directly as life areas.
- **Gap closed.** #2. Day-one structure with no data entry — the app starts already knowing
  your shape.

### 4.7 Health Connect — steps and sleep become progress

- **What it is.** Reads steps and sleep from Android's Health Connect and logs them against a
  fitness or sleep goal, creating one if you have none.
- **How it behaves.** Runs automatically every time you open the app, at most once every
  fifteen minutes. **Read-only** — GoalPilot never writes to your health store. A day is never
  counted twice, and today is topped up by the difference as you walk, so the goal keeps up
  without the day being logged again.
- **Gap closed.** The silo problem in §2.1. Your health numbers stop being a separate app you
  have to remember to open and become progress on a goal you actually set.

### 4.8 Life areas and the record of what you kept

- **What it is.** Your own division of your life, defined by you (or synced from Google
  Tasks). Every goal is filed under one. Goals are grouped by area everywhere in the app,
  because your life is not a flat list.
- **The run.** Under each area is the honest record, window by window: what you **kept**, what
  you **missed**, and what is **still owed**.
- **Three things, deliberately not averaged.** A single "success rate" would have destroyed the
  only distinction worth keeping — that *a window you have not reached yet is not a window you
  failed*. So there are three separate states and no ratio anywhere on the screen.
- **Reported over a window you choose** — 30 days, 8 weeks, 6 months (default 8 weeks). A
  window is a **filter over history, not decay of it**: nothing ages out, and there is no
  lifetime failure counter anywhere in the app, because a number that can only rise is a list
  of the things you are bad at.
- **Gap closed.** #3, *dishonest numbers*, and the streak problem in §2.1. Nothing here can be
  broken permanently by one bad week.

### 4.9 "Where your time goes" — the payoff feature

- **What it is.** An interactive donut chart showing what **share of your actual life** went
  into each life area, computed from the AI's duration estimate for every task you completed.
- **What you can do with it.** Switch between day / week / month / quarter / year. Tap a slice
  for its hours and its share.
- **The unfiled slice.** The chart also shows the share of your time that was **never filed at
  all** — in the demonstration recording, thirteen per cent. Most analytics screens quietly
  drop what they cannot attribute, which makes every other number on them wrong.
- **Goals with no number are named here, not charted as zero.**
- **Gap closed.** #1 and #3 together, and it answers the question from §1.3 that nothing else
  can: *is my life moving in the direction I chose?* It is not a judgement. It is the first
  honest number the user has had.

### 4.10 Points, levels and the leaderboard

- **What it is.** Completed work banks points computed from real minutes and real difficulty
  (§4.2). Points accumulate into levels with visible level progress.
- **Friends.** A six-character **friend code** adds a friend without either person typing an
  email address. A leaderboard ranks friends; a global view ranks everyone.
- **The feed.** A week's progress is something you **post**, with a photo if the week deserved
  one — rather than something you have to explain.
- **Gap closed.** #4. The score is downstream of the work, so competing on it means competing
  on actual effort spent on goals you chose.

### 4.11 Challenges — a shared measure

- **What it is.** Create a challenge with a title, a start, and a **unit everyone is measured
  in**; invite people; report your total; read the standings. `Observed:` the demonstration
  recording shows a live one — *August Steps Race · 2 people in · open-ended · standings*.
- **A challenge has no optional measure.** Everywhere else in GoalPilot a measure is optional
  and the app says *no number* rather than inventing one. Here it is mandatory, because there
  is nothing to compare without a shared unit — the same honesty rule producing the opposite
  requirement.
- **The standing is server-owned.** The participant writes the *fact* — the number they
  report — and a Cloud Function projects it onto the standings. Equal scores share a rank and
  the next rank skips accordingly, because showing two people tied at #1 and #2 would be
  wrong rather than merely untidy.
- **This is the newest and lightest surface in the app.** Present it as what it is: a working
  shared-measure competition, and the direction the product goes next.
- **Gap closed.** #4, in its social form: competition anchored to a real, shared, comparable
  quantity rather than to who opened the app more often.

### 4.12 The guided tour — the app teaches itself

- **What it is.** Seven coach marks over the **real** app on first launch: the screen dims and
  a hole is cut over the thing being described, one step at a time. Skippable from any step,
  and replayable forever from *Settings → Help*.
- **It follows you.** One step asks you to open the Goals tab yourself and then follows you
  there. When it rings a control and invites a press, pressing it **opens what it opened**, and
  the tour waits there while you look instead of pulling you back.
- **Gap closed.** Adoption. A four-level structure is the product's advantage and its
  onboarding risk; the tour pays that debt inside the app rather than in a manual nobody reads.

### 4.13 Appearance, language and personal fit

- **Seven appearance axes**, all on one Settings screen and applied instantly: skin (*Aurora*,
  *Blossom*), brightness, background, **material**, relief, language and region.
- **Four materials** — frosted glass, liquid glass, a soft raised surface, and a dark one.
- **Every skin ships a full light and dark palette.**
- **English and Hebrew, with full RTL** — not a translated string table, a genuinely mirrored
  layout.
- **Your waking hours** are a setting, so the app knows when your day is *genuinely* full and
  when to ask you to plan tomorrow.
- **Gap closed.** Retention, honestly stated: an app you look at every day has to be an app you
  want to look at. Everything here stays on the phone; signing out leaves it untouched.

### 4.14 Summary — feature to gap

| Feature | Primary gap it closes |
|---|---|
| Smart add | structure is expensive |
| AI estimation | motivation bolted on / numbers dishonest |
| Five AI capabilities | structure is expensive |
| In-app calendar + load bar | the pieces never meet |
| Google Calendar sync | the pieces never meet |
| Google Tasks import | structure is expensive |
| Health Connect | the silos |
| Life areas + the run | numbers dishonest / the streak trap |
| Where your time goes | the pieces never meet + numbers dishonest |
| Points, levels, leaderboard | motivation bolted on |
| Challenges | motivation bolted on (social) |
| Guided tour | adoption |
| Appearance & language | retention |

---

## Part 5 · The honesty rules — what makes this product different

These are not implementation details. They are the product's position, and they are visible on
screen. A deck that omits them describes a generic goal tracker.

1. **"No number" instead of 0 %.** A goal you have not measured says *no number* — it is not
   charted as zero. *An app that prints a zero for something you have simply not measured is
   lying to you in a way you will not notice, and this one refuses to.*

2. **Three states, never a rate.** Kept, missed and still-owed are three different things drawn
   as three different shapes. A single percentage would average away the only distinction worth
   keeping.

3. **No red, and no colour coding of outcome at all.** The record is drawn by **form** —
   filled, hollow, dashed with a centre pip, dotted, a dashed ring with a plus — so it reads in
   dark mode, in greyscale, and to a colour-blind eye. It is an accessibility decision and a
   **tone** decision at once: nothing on that screen shouts at you.

4. **The AI judges; the app computes.** Every number that has to be comparable is arithmetic
   the app owns. The model supplies judgements only, and a response that smuggles a score back
   in is rejected by validation.

5. **Nothing you put into the app can become unreachable.** A task that belongs to no goal and
   has no date used to vanish — filed nowhere, listed nowhere, impossible to delete because
   there was nowhere to delete it from. Now the app says so, in those words, and hands you the
   two questions separately: what goes, and what stays.

6. **Derived state is derived, never stored.** Whether something is late, missed or still owed
   is computed from the dates every time it is displayed. Nothing can go stale and nothing can
   quietly disagree with the data it came from.

7. **Nothing ages out.** History is permanent; the window you pick is a filter over it. And
   there is no lifetime failure counter anywhere in the product.

---

## Part 6 · How it is built

| | |
|---|---|
| **Platform** | Android — Kotlin, Jetpack **Compose**, Material 3 |
| **Version** | v0.4.0 · `minSdk 26` · `targetSdk 35` |
| **Architecture** | **MVVM**, layered: `domain` (models, repository interfaces, use cases — no Android or Firebase types) → `data` (Firebase implementations, encrypted key store, integration clients) → `ui` / `feature` |
| **DI** | **Hilt** |
| **Async** | Coroutines + Flow |
| **Navigation** | Navigation-Compose |
| **Backend** | **Firebase** — Auth (Google Sign-In), Firestore, Storage, Cloud Functions |
| **AI** | LLM access through a **Cloud Functions proxy** (GROQ by default), or the user's own OpenAI / Anthropic / Gemini key held **encrypted on the device** |
| **Integrations** | Google Calendar · Google Tasks · Health Connect |
| **Server-side logic** | Cloud Functions own every derived number that must not be forgeable — the client writes the *fact*, a trigger owns the *derived value* |
| **Security** | Firestore and Storage security rules, tested against the Firebase emulator |
| **Testing** | JVM unit tests, Compose UI tests, instrumented device tests, and security-rules tests |

**One architectural point worth a slide:** the split between *facts* and *derived numbers*.
The device records what happened — this task was completed, it took 105 minutes, it was
demanding. The server computes what that means — the lifetime point total, the challenge
standings. This is why a score cannot be typed in, and why a leaderboard is worth looking at.

---

## Part 7 · Privacy and data control

- **Sign-in is Google Sign-In via Firebase Auth.** Nothing else is collected to create an
  account.
- **Health Connect access is read-only.** GoalPilot never writes to the health store.
- **Google Calendar writes to its own calendar** and never modifies existing entries.
- **A user's own AI key is stored encrypted on the device** and is never uploaded.
- **Appearance and preference settings stay on the phone**; signing out leaves them untouched.
- **Every import is reviewed before it is saved** — Google Tasks, smart add, AI proposals.
  Nothing enters the user's data without a confirmation step.

---

## Part 8 · Status

- **Working Android application, v0.4.0**, running on a real device and demonstrated end to
  end in a single continuous screen recording of the shipping app against a real account with
  real data — not a mock-up, and nothing in it staged beyond the choice of what to tap.
- **Core, bonus and nice-to-have tiers are all implemented**: goals, tasks, AI classification
  and estimation, the calendar and its Google Calendar sync, Google Tasks import, Health
  Connect, life areas, the time-allocation analytics, points and levels, the social leaderboard
  and feed, challenges, the guided tour, and the full appearance and localization system.
- Built as a final project for an *Android Application Development* course, against a written
  product specification.

---

## Part 9 · Suggested deck outline

Fourteen slides. Each carries **one** message; the second column is the message, not a title.

| # | Slide | The one thing it says |
|---|---|---|
| 1 | **GoalPilot** — *Pilot your life goals* | title, logo, one line |
| 2 | **The list everyone has** | get fit, learn the instrument, finish the degree, be better to the people you love |
| 3 | **None of it is due on Tuesday** | goals that matter fail by never being scheduled — not by being abandoned |
| 4 | **The apps each hold one piece** | the to-do list has the task but not the reason; the calendar has the hour but not the goal; the habit tracker has the streak but not the outcome *(use the table in §2.1)* |
| 5 | **So the real question goes unanswered** | *is my life moving where I chose?* — nobody can answer it, because answering it needs all three pieces at once |
| 6 | **The answer: one structure** | life area → goal → task → hour, unbroken *(use the diagram in §3.2)* |
| 7 | **Say it in a sentence** | smart add — the AI does the filing, so the structure costs nothing *(§4.1, §4.2)* |
| 8 | **Goals become hours** | the calendar, the load bar, drag-to-reschedule, and the sync into the calendar you already use *(§4.4, §4.5)* |
| 9 | **Your data, already yours** | Google Tasks and Health Connect become progress on goals you set *(§4.6, §4.7)* |
| 10 | **The honest record** | kept · missed · still owed — three shapes, no rate, no red *(§4.8)* |
| 11 | **Where your time actually went** | the donut, by year, including the share that was never filed at all *(§4.9)* |
| 12 | **Not alone** | points, levels, friends, challenges scored on movement since you joined *(§4.10, §4.11)* |
| 13 | **Built to refuse to lie to you** | the honesty rules — pick three from Part 5 |
| 14 | **Close** | *your life areas, your goals, your tasks and your hours — in one place, so progress is something you can see instead of something you have to feel* |

**If a shorter deck is wanted (7 slides):** 1, 3, 4, 6, 7+8 merged, 11, 14.

**If a technical deck is wanted:** insert Part 6 as two slides between 12 and 13 — one on the
layered architecture, one on the facts-versus-derived-numbers split.

---

## Part 10 · Lines that can be quoted directly

These are written for narration and hold up as slide text.

- *"There is always a list. The run you were going to start. The instrument you were going to
  pick back up. They do not fail loudly. They just never get a time."*
- *"None of them is due on Tuesday. So every one of them loses, quietly, to whatever is."*
- *"Your to-do list has the task but not the reason. Your calendar has the hour but not the
  goal. Your habit tracker has the streak but not the outcome."*
- *"So the one question worth asking — is my life actually moving where I chose? — is the one
  question none of them can answer."*
- *"Say what you want to do, the way you would say it to a person."*
- *"Nothing is saved until you have seen where it went."*
- *"Now it isn't a list. It's time — with the load you're actually carrying."*
- *"The task, the goal it serves and the hour it costs are the same object, so moving one of
  them moves all three."*
- *"Every goal belongs to a part of your life you decided was worth investing in."*
- *"Not a score, not a grade — just where your time actually went, including the part that was
  never filed at all."*
- *"The app would rather show you a gap than a wrong number."*
- *"A window you have not reached yet is not a window you failed."*
- *"The list never needed more discipline. It needed a time."*
- **Closing line:** *"GoalPilot. Give it a time."*

---

## Part 11 · Vocabulary

The product's four domain terms, in both languages. Use these exact words; do not substitute
synonyms, because each one is a distinct level of the model.

| English | Hebrew | What it is |
|---|---|---|
| **Life area** | **תחום חיים** | a part of your life you decided is worth investing in |
| **Goal** | **יעד** | a direction inside a life area, optionally with a measure |
| **Milestone** | **אבן דרך** | an intermediate step toward a goal — the same kind of object as a goal, standing under another one rather than being a separate entity |
| **Task** | **משימה** | a unit of work, with an estimated difficulty and duration |

Other recurring terms: **measure** (the unit a goal is counted in) · **occurrence** (a task on
a day, and sometimes at an hour) · **the run** (the kept / missed / still-owed record per life
area) · **load** (how many promised hours a given day already carries).

---

## Part 12 · Accuracy guardrails — six things not to say

1. **Never call the kept/missed/still-owed record a "score", a "success rate", or describe any
   part of it as "red".** The whole feature was designed specifically to be none of those
   things, and the screen carries no ratio and no hue coding at all. A caption saying "your
   success rate" contradicts the picture the audience is looking at.

2. **Never say the AI awards points.** It supplies difficulty and duration; the app computes
   the points. This is a deliberate architectural boundary and one of the more interesting
   things about the product — describing it the other way round removes the point.

3. **Do not invent named competitors or their features.** §2.1 deliberately argues by
   *category*, not by brand. Every claim in it is about a structural limitation of a category,
   which is defensible; a claim about a specific product's roadmap is not.

4. **Do not invent user numbers, download counts, study results or percentages.** The only
   figure in this document that comes from real data is the thirteen per cent unfiled share in
   the demonstration recording, and it is one person's data — present it as an illustration of
   the feature, never as a statistic about users in general.

5. **Do not over-claim challenges.** What ships is described in §4.11 and nothing more: a
   shared unit, an invitation, a reported total, a ranked standing. Do not describe automatic
   scoring from health data, from goal progress, or from anything the participant did not
   report — that is a design direction, not a shipped behaviour.

6. **Do not present this as a concept or a prototype.** It is a working, installed Android
   application with a backend, security rules and a test suite. Equally, do not claim it is
   published on the Play Store — it is a course project.
