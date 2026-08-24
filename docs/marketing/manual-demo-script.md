<!-- Written 2026-08-24 by session `manual-demo-script`, at Ido's request. -->
<!-- Purpose: the running order Ido reads OFF-SCREEN while he records a feature-review
     screencast of GoalPilot himself. Not a narration script for an edited film -- that is
     marketing-film-cut.md -- and not an automated capture -- that is scripts/record-tour.sh. -->

# The manual demo — what to show, in what order

**What this is.** A running order for a screencast **you** drive live: every shipping feature
of GoalPilot v0.4.0, one at a time, in the order that tells the product's story with the
fewest wasted taps. Read it off a second screen while you record.

**What it is not.** [`marketing-film-cut.md`](marketing-film-cut.md) is the 2-minute edited
film with a voiceover. [`tour-timecodes.md`](tour-timecodes.md) is the beat map of the
automated 11-minute capture from `scripts/record-tour.sh`. This file is the third thing: a
human walkthrough, where you talk over your own hands.

**Target length.** `13:30` for the full review. The **Cut** column says what to drop for a
`7:00` version; see [§5](#5--the-seven-minute-cut).

---

## 1 · Before you press record

Ten minutes of prep saves a re-shoot. Everything here is a real trap that has cost a take.

### 1.1 The device and the picture

| | |
|---|---|
| **Mirror the phone** | `.\scripts\mirror-phone.ps1 -StayAwake` — scrcpy, mouse and keyboard control, and it refuses emulators so it cannot collide with another session's AVD |
| **Record the device stream, not your monitor** | `.\scripts\mirror-phone.ps1 -Record C:\Users\namei\Videos\GoalPilot-Tour\manual-demo.mp4 -StayAwake` — scrcpy writes the **device's** stream, so no desktop, no window chrome, no mouse cursor |
| **Or record on the phone itself** | `adb shell screenrecord --time-limit 0 --size 1152x2560 --bit-rate 12000000 /sdcard/demo.mp4` — `--time-limit 0` is what removes the 3-minute cap |
| ⚠️ **`screenrecord` downgrades silently** | at a size the encoder refuses it falls back to **720×1280**, prints *"retrying at"*, and still exits `0`. Read its output before you trust the file |

### 1.2 The status bar

Put SystemUI into demo mode so no clock, battery percentage or notification dates the video:

```bash
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900
adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
# afterwards:
adb shell am broadcast -a com.android.systemui.demo -e command exit
```

Also: **Do Not Disturb on**, and no incoming call or message can survive it.

### 1.3 The account, and the data it needs

The review is only as good as the account behind it. Check all six **before** you record:

1. **Signed in**, and the dashboard lands on its own — no auth screen in the take.
2. **At least two life areas with real goals under them**, so the Goals tab groups visibly.
3. **At least one goal with a measure and one without** — the *No number* chip is a feature,
   and you cannot show a refusal you do not have.
4. **Analytics has data.** Measured on this account: **Week** and **Month** are empty; **Year**
   carries the 67 % / 20 % / 13 % split. Open Analytics before recording and find out which
   period actually has a chart — a donut with nothing in it under the line *"where your time
   actually went"* is the worst frame in the film.
5. **One life area whose run has windows in it.** Areas with nothing due yet render the honest
   empty state — correct behaviour, useless footage. Know which area to open before you tap.
6. **A live challenge with at least one other participant**, so *Standings* has two rows.

### 1.4 Decide the privacy question now

Three screens carry real personal data:

| Screen | What is legible |
|---|---|
| **Leaderboard** | a friend's real name |
| **Profile** | your email address and your friend code |
| **Friends feed** | whoever posted, and their photo |

For a course submission this is fine — the previous tour shipped with it. For anything that
travels: skip Act 9's leaderboard, or blur it in one pass afterwards. **Decide before you
record**, because the alternative is discovering it in the edit.

### 1.5 Two things about how you talk

- **Say what the app is doing while it thinks.** Every AI beat here takes 3–5 seconds of
  silence. *"It's reading the sentence now"* fills it; dead air reads as a hang.
- **Never call the run a score, a rate, or a success rate.** It is three separate counts —
  kept, missed, still owed — deliberately not averaged. Calling it a rate contradicts the
  product's own position on camera.

---

## 2 · The running order

The order is chosen so that **each act leaves you where the next one starts** and each feature
is explained by the one before it. The two deliberate departures from the automated tour's
order are called out in [§4](#4--why-this-order).

| # | Act | Screen | Target | Cut |
|---|---|---|---|---|
| 1 | Cold open, and the claim | launcher → Home | `0:45` | keep |
| 2 | Smart add — a sentence becomes a filed task | Home | `1:30` | **keep — this is the flagship** |
| 3 | The rest of Home | Home | `1:00` | cut to 20 s |
| 4 | The structure, over the Goals tab | Goals | `1:00` | keep, 30 s |
| 5 | A new goal files itself, and proposes a plan | Goals → New goal | `2:00` | keep, 60 s |
| 6 | One goal, close up | Goal detail | `2:00` | cut |
| 7 | Goals become hours | Calendar | `2:00` | keep, 45 s |
| 8 | It reaches into Google Calendar | Google Calendar app | `0:30` | cut |
| 9 | Points, friends, challenges | Social | `1:30` | keep, 30 s |
| 10 | Life areas, the run, and where your time goes | Profile → Life areas / Analytics | `2:45` | keep Analytics only, 45 s |
| 11 | Make it yours | Settings | `2:00` | keep materials only, 30 s |
| 12 | The app teaches itself | Settings → Replay tutorial | `1:30` | cut |
| 13 | Close | Home | `0:20` | keep |

---

## 3 · Act by act

Each act gives: **where you are · what to tap · what to say · what to be careful of.**

### Act 1 · Cold open, and the claim — `0:45`

**Tap.** Launch from the launcher icon. Let the splash play. Do not touch anything for three
seconds after the dashboard lands.

**Show.** The greeting, **points**, **level and level progress**, and **overall progress
averaged across all your goals**.

**Say.** The one-sentence claim, and nothing more yet:

> Your life areas, your goals, your tasks and your hours are one structure — so progress is
> something the app computes, not something you have to feel.

⚠️ Do not start listing features here. The claim is what everything after has to earn.

---

### Act 2 · Smart add — `1:30` — the flagship

**Tap.** The add field on Home. Type a real sentence in plain words — the phrasing matters,
so use something like:

> `Practice saxophone for 20 minutes on Sunday`

Then dismiss the keyboard and **wait**.

**Show.** In this order, and let each land:
1. The AI reading it.
2. The confirmation — *filed under "Learn to play the saxophone", 20m, +7 points*.
3. That **nothing was saved until you saw where it went** — the Undo is right there.

**Say.** Two points, and they are the two that sell the product:

> I typed one sentence. It worked out which goal it belongs to, which life area, how long it
> takes and what it is worth — and it showed me before it saved anything.

⚠️ **The single most fragile beat in the take.** It is a live model call over the network. If
it files it under the wrong goal, say so out loud and undo it — an honest miss on camera is
better than a re-shoot, and this product's whole position is that it does not fake numbers.

---

### Act 3 · The rest of Home — `1:00`

**Tap.** Scroll down Home. Do not leave the tab.

**Show**, in the order they appear:
- **Filed nowhere** — the tasks no other screen could list, because nothing fits them yet.
- **The AI coach** — one practical line per goal you could act on **today**.
- **Your goals**, on the home screen.
- **Share your weekly progress** — a week is something you post, not something you explain.

**Say.** One line for the coach:

> This is not a chatbot. It is five narrow capabilities with defined outputs — this one gives
> me one thing I could actually do today.

---

### Act 4 · The structure, over the Goals tab — `1:00`

**Tap.** The **Goals** tab. Scroll once.

**Show.** Goals **grouped by life area** — the group headers *are* the structure, so you can
state the whole model without navigating anywhere. Then find a goal carrying the **No number**
chip.

**Say.** The four levels, then the refusal:

> Life area, goal, task, hour. Four levels, and every screen in the app knows all four.

> And this goal has no measure — so the app says *No number* instead of printing zero per
> cent. It would rather show nothing than show a number it made up.

---

### Act 5 · A new goal files itself, and proposes a plan — `2:00` — the newest feature

**Tap.** **New goal** → type **only a title** (something clearly in one of your areas, e.g.
`Run a half marathon`) → leave the life-area chips **alone** → save.

**Show.**
1. The note under the chips: *leave this alone and the app files the goal from its title when
   you save.* Point at it before you save, so the filing is a promise the app then keeps.
2. The goal, **filed under the right life area with no dialog** — you never told it which.
3. **Suggest a work plan** → the sheet: steps, each with a duration and a date.
4. Deselect one step, to make the point that it is a **draft**.
5. **Add N steps** → they land on the goal as real tasks with dates.

**Say.**

> I gave it a title. It filed it. Then it proposed a plan — and nothing the model decided
> reached my data until I approved it, step by step.

⚠️ Two live model calls back to back; this is the slowest act. Keep talking. If the plan comes
back thin, take it — a short real plan beats a re-shoot.

---

### Act 6 · One goal, close up — `2:00`

**Tap.** Open a goal from the Goals tab — ideally the one Act 2 filed the saxophone task
under, so the two acts connect on screen.

**Show**, top to bottom (**scroll to the top first** — the add-task row is at the top, and the
automated take lost this whole act by scrolling past it):
1. The **measure**, the **progress**, the **life area**, and the tasks — including the one you
   added by sentence in Act 2.
2. On a goal with no measure: the **measure proposal** — *the app offers a measure rather than
   leaving the goal blank*, drawn from what you have actually been logging.
3. The **task field** → type a task → the **✨ estimate** → *how demanding, how long, and what
   it is worth.*
4. The **date picker** → add an hour → *that promotes it to a deadline.*

**Say.** The split that makes the points mean anything:

> The model says how hard it is and how long it takes. The app does the arithmetic —
> `minutes ÷ 3 × difficulty`. The model is never allowed to hand back a score, which is why
> points are comparable between two people.

And on the deadline:

> A deadline and an all-day task fail differently. *The day passed* and *late, and still owed*
> are not the same event, so they do not look the same here.

---

### Act 7 · Goals become hours — `2:00`

**Tap.** The **Calendar** tab.

**Show.**
1. The three-day view — *the tasks you just created are on it.*
2. The **load bar** across the top: how full the day already is **before** you add to it.
3. The **all-day strip**: what is due today with no hour on it.
4. **Next** → forward three days → **Today** → back.
5. **Week** → the whole week at once → **3 days** → back.
6. **Press and hold a block**, pick it up, drop it where it should have been.
7. If the block repeats, the dialog: *this one, or this one and every one after it?*

**Say.**

> This is not a second calendar with a second list in it. It is the same tasks, under the same
> goals, on the hours I gave them — so moving one moves all three.

---

### Act 8 · It reaches into Google Calendar — `0:30` — optional

**Tap.** Leave the app, open **Google Calendar**, show the **GOALPILOT** calendar with the
work on it.

**Say.**

> It writes onto a calendar of its own, so it never touches anything I already keep there.
> That is the only place a goal can ever compete with a meeting.

⚠️ Only shoot this if the sync is actually connected and populated. Cut it otherwise — a
dry-run of a sync is worse than not mentioning it.

---

### Act 9 · Points, friends, challenges — `1:30`

**Tap.** The **Social** tab.

**Show.**
1. **Leaderboard** → **Friends** → **Everyone**. ⚠️ *A friend's real name is on screen from here.*
2. Scroll → the **friends feed**, with a photo if the week deserved one.
3. **Challenges** → open the live one → the **unit everyone is measured in**, **Report score**,
   **Standings**.

**Say.**

> Everywhere else in the app a measure is optional. In a challenge it is mandatory — there is
> nothing to compare without a shared unit. Same honesty rule, opposite requirement.

> And the standing is not computed on my phone. I report the number; the server owns the rank.

---

### Act 10 · Life areas, the run, and where your time goes — `2:45`

**Tap.** Your **avatar** → Profile. Everything in this act is behind it, so there is no
back-and-forth.

**10a · Profile** — `0:20`. Level, points, and the **friend code** — *six characters, so
adding a friend never needs an email address.* ⚠️ *Your email and friend code are legible here.*

**10b · Life areas** — `1:00`. **Life areas** → open the area **you checked has a run**.

**Show.** The area, then scroll to **Window by window**: what you **kept**, what you
**missed**, what is **still owed**.

**Say.**

> Three counts, deliberately not averaged into one. A window I have not reached yet is not a
> window I failed — and there is no lifetime failure counter anywhere in this app, because a
> number that can only rise is just a list of the things you are bad at.

⚠️ **Not a score. Not a rate. Not red.** The run is drawn by form, not by hue.

**10c · Analytics** — `1:25`. Back → **Analytics**.

**Show.**
1. The donut — **where your time goes**, computed from the estimated duration of everything
   you completed.
2. Switch to the period **you checked has data** (**Year** on this account).
3. **Tap a slice** for its hours and its share.
4. The **unassigned slice** — the share of your time that was never filed at all.
5. Scroll → **goals with no number are named here, not charted as zero.**

**Say.** This is the payoff line of the whole review — do not rush it:

> Thirteen per cent of my time was never filed at all, and the chart says so. Most analytics
> screens quietly drop what they cannot attribute, which makes every other number on them
> wrong. This is not a grade. It is the first honest answer I have had to *is my life moving
> in the direction I chose?*

---

### Act 11 · Make it yours — `2:00`

**Tap.** Avatar → **Settings**. Scroll straight down; the sections are in this order.

| Section | Show | Say |
|---|---|---|
| **Help** | it is the **first** thing in Settings | *the tour is replayable forever, not a one-time thing* |
| **Connected apps** | **Google Tasks** and **Health Connect** | *your Google Tasks list names already are your life areas · steps and sleep become progress on a goal, read-only, never written back* |
| **Appearance** | tap all four **materials** — Glass, Liquid glass, Soft, Soft dark — then **Background**, skin, brightness, chart relief | *every skin ships a full light and dark palette* |
| **Language & region** | switch to **Hebrew**, let the layout **mirror**, then switch back | *full RTL — a mirrored layout, not a translated string table* |
| **Your day** | **Awake between**, week start, plan-tomorrow time | *so it knows when my day is genuinely full* |
| **AI** | the bring-your-own-key field | *GROQ by default with no setup; your own OpenAI, Anthropic or Gemini key is stored encrypted on the device and never leaves it* |

⚠️ **Tap the materials by their taglines**, not the section header — *Frosted panels*,
*Glossy, lit*, *One flat surface*, *Charcoal, with one*. And do the language flip **here, near
the end** — everything you record after it is in the other language until you flip back.

---

### Act 12 · The app teaches itself — `1:30`

**Tap.** Settings → **Help** → **Replay tutorial** → **Start**.

**Show.** All seven steps, and the two that matter most:
- **Step 4** asks you to open the **Goals** tab yourself — tap it, and *the tour follows you
  there.*
- **Step 6** rings the **Calendar** tab — tap it, and *it actually opens, and the tour waits
  there while you look* instead of pulling you back.

**Say.**

> A four-level structure is this product's advantage and its onboarding risk. The tour pays
> that debt inside the app, over the real screens, rather than in a manual nobody reads.

---

### Act 13 · Close — `0:20`

**Tap.** **Done** → you land back on Home, where you started.

**Say.** Close on the claim you opened with, now earned:

> One structure. The task, the goal it serves and the hour it costs are the same object — so
> the app can tell me the truth about where my life is going.

---

## 4 · Why this order

Two deliberate departures from `scripts/record-tour.sh`'s act order, and one thing kept:

1. **The four-level model is *stated* in Act 4 and *shown* in Act 10.** The automated tour
   visits Life areas at act 7, which is late for a viewer who needs the model to understand
   anything before it. But the Goals tab already renders life areas as its group headers — so
   the model can be explained there at zero navigation cost, and the Life areas *screen* stays
   where its real feature is, next to the run and next to Analytics, all three behind the same
   avatar.
2. **The new-goal AI plan (Act 5) comes before goal detail (Act 6) and before the calendar
   (Act 7).** It creates dated tasks, so the two acts after it show *your own work from the act
   before* — the goal fills up, then the calendar fills up. Shot in the other order they are
   three unrelated screens.
3. **Kept: capture before structure.** Smart add is Act 2, not Act 6. It is the feature that
   makes the four-level model free to maintain, and a viewer who has not seen it assumes the
   structure is expensive — which is the objection the whole product exists to answer.

Everything else follows the proven order, which was measured against a real 11-minute take.

---

## 5 · The seven-minute cut

If the review has to be short, drop **Acts 6, 8 and 12** entirely and shorten the rest per the
**Cut** column in [§2](#2--the-running-order). What survives is the spine:

**Home → smart add → the structure → a goal that plans itself → the calendar → challenges →
where your time actually went → four materials → close.**

Do **not** cut smart add, the unassigned slice in Analytics, or the *No number* chip. They are,
in order, the feature that makes the product possible and the two that make it honest.

---

## 6 · If something goes wrong mid-take

| It happened | Do this |
|---|---|
| An AI call files something under the wrong goal | **Say so and undo it on camera.** It is a live model; pretending otherwise is the one thing this product's position cannot survive |
| An AI call hangs | Keep talking for five seconds, then move on and come back at the end. Do not stop the recording |
| A screen is empty that you expected to have data | Name the empty state as the honest behaviour it is, then move on — §1.3 exists so this does not happen |
| You fluff a line | Pause two full seconds in silence and say it again. A clean cut point is free in any editor; a re-shoot is not |
| A notification lands | Stop. Fix DND. Re-shoot from the top of that act — you cannot blur a notification cheaply |
