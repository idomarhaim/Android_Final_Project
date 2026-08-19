# `C22` · The measure proposal — prototype

Asset for [`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 2 — the ticket is
resolved** by the comment on `#44`; this asset is what it was resolved against. Open `index.html`;
material, theme and language switch in the bar or by query string so `shoot.ps1` can render any
state:

```powershell
cd docs\prototypes\tools
./shoot.ps1 -Page ..\2026-08-15-measure-proposal\index.html -Out out.png `
            -Query "m=neo&t=light&l=he" -Width 2320 -Height 1200
```

`m=glass|liquid|neo|darkneo` · `t=dark|light` · `l=en|he`.

## The answer, in one line

> **It is two things, not one.** A **marker** — silent, wherever the goal is listed — and an
> **offer**, only on the goal's own screen, because **opening the goal is the consent**.

## What the five frames prove

| Frame | Screen | Proves |
|---|---|---|
| **1** | the life-area list | the **marker** — a fact, with no words and no buttons — beside `C19`'s pair on the row below it, so the deliberate asymmetry is visible rather than described |
| **2** | the goal's own screen | the **offer**, standing where the missing bar would be, above the week it computed its target from |
| **3** | the sheet | outcome **or** leading indicator, and where each target comes from |
| **4** | no model · 1 | the offer surviving on **arithmetic alone** — same component, no model behind it |
| **5** | no model · 2 | the app **saying nothing**, on a real screen, which is the only way to see whether it reads as deliberate or broken |

## How the question was answered

Revision 1 offered three **placements** — the goal screen, the `C19` life-area row, the daily review
— on the axis *when is a person receptive*. Ido **handed the decision back**, in near-identical words
to the two delegations this map had already received: *"I couldn't fully understand you or the
implications of each option — explain simply and schematically. And choose the solution that gives
the highest standard and quality of the app (and its purpose), UX/UI and the software. And if you can
improve it, improve it. Should it be in several places?"*

`rules/question-axis-naming.md` forbids re-asking a delegated question and requires **deriving** the
answer — and warns that the answer is often **not in the option set**. It was not.

**The fork was false in a specific way: every option was a placement of one object, and the object is
two.** The set could not contain the answer, because no member of it distinguished *stating a fact*
from *making an offer*.

**§0.7 is what separates them, and it is already closed:** *intrinsic structure needs consent;
instrumental structure does not.*

- Breaking a goal into steps is **instrumental** — so `C19` may put *Break it into steps* inline in a
  list, and it does.
- A **measure defines what counts as progress on the goal**, which is intrinsic. `C7` already
  requires consent for it in so many words: **never auto-applies, dismissible per goal.**

So the offer may not be pushed into a list being scanned for something else — but the **marker** may
be everywhere, because stating a fact asserts nothing. **Opening the goal is the consent**, which is
why the offer lives there and needs no extra gate.

**And the daily review is ruled out, on a second closed decision.** `C10` #29 already allocated that
screen's three slots; a fourth thing re-opens it. It is also the one surface in the app that arrives
**unasked**, which is exactly what an offer requiring consent may not do. *(Consequence: there **is**
a fifth AI call. Under the daily-review placement it would have ridden `daily`'s envelope.)*

**Direct answer to "should it be in several places?" — yes for the marker, no for the offer.** That
split is the improvement; it was available only once the question stopped being about placement.

## What else was derived rather than asked

Each follows from a closed ticket, so it is an input rather than a fresh opinion — and each is Ido's
to overturn.

1. **The absence is stated as legal *before* anything is offered.** Every unmeasured goal carries
   *"No number on this one. That is a choice, and it stays one — nothing here is incomplete."* `C7`
   makes absence **the default** (`E6`); §0.4 makes it legal-but-never-silent. Without that line the
   offer reads as a **correction of a decision he made on purpose** — `#44`'s named failure mode. It
   is also what makes frame 5, where the app offers nothing, read as deliberate rather than as a load
   failure.
2. **An offer never borrows the visual language of an outcome.** Everything is **dashed and hollow**;
   the filled accent means *kept* on `C19`'s screen and must not appear on something the app has not
   done.
3. **The dismiss is a peer of the accept** — same size, same row, no colour. Absence is the default,
   so *not for this goal* is not the lesser branch. Drawn small it is a dark pattern; drawn red it
   says a legitimate choice was a mistake.
4. **Dismissal is permanent for that goal**, not snoozed. `C7` says *dismissible per goal*, and a
   default that re-asks is not a default. The manual path (`Something else` → the measure editor)
   always exists, so nothing is unreachable — it is simply never volunteered again.
5. **The leading indicator is preselected, and only where it has a real target** — it is the only
   branch whose number the app can **compute**.
6. **The marker is a square.** Rev 2 found the dashed circle carrying `#` read as the same thing as
   `C19`'s dashed circle carrying `+` — one chip carrying two axes, in `C19`'s own words. Every
   circle in this language is an occurrence or an outcome, so a number slot is a **square**:
   distinguished by **form**, not hue, so it survives dark neo and greyscale.

## The schema — feature **E**, `measure`

Obeys [`C11b` #30](https://github.com/idomarhaim/Android_Final_Project/issues/30) unchanged: one
**wide** call, **per-field-group** validation, the Function **validates and omits, never
substitutes**, **no retries**, and the standard envelope.

```jsonc
// request: { language, goals: [ {
//   id, title, lifeAreaName?,
//   occurrencesPerWeek?: int,   // what the schedule already says  (C9a)
//   openStepCount?: int         // what the sub-task tree already says (C18)
// } ], … }
{ "proposals": [ {
    "goalId":       "<echoed — MUST be a member of goals[].id>",
    "measureKind":  "COUNT"|"DURATION"|"DISTANCE"|"VOLUME"|"MASS"|"MONEY"|"PERCENT",
    "word":         "<≤ 24 chars, authored in `language`>",
    "basis":        "OUTCOME" | "LEADING",
    "targetSource": "SCHEDULE" | "STEPS" | "USER"
} ] }
```

**There is no number in the response.** `targetSource` is a prompt-declared enum naming **which
arithmetic the app runs** — `SCHEDULE` → the occurrences already on the goal, `STEPS` → the count of
its open sub-tasks, `USER` → ask him. Forced rather than chosen: `C11a` measured **free numbers
swinging 2× run-to-run and 1.8× between languages**, so a model-supplied target is the one field that
could not be trusted and the one the whole feature would be judged on. `C10`'s strongest form applies
— never let a value cross the wire whose failure you would have to *detect*.

`measureKind`, `basis` and `targetSource` are enums (**50/50 perfect** in `C11a`). `word` is the only
free field and it is **content**, so §3.5's *"a goal title from the sorter — the model, once"*
governs: proposed once, his the moment he accepts it, never rewritten afterwards (`C15b`).

**Validation** — `goalId` membership-checked (the one measured failure class); `word` length-capped;
any field failing validation is **absent**, and a proposal missing `measureKind`, `word` or
`targetSource` **is not a proposal**, so the whole element is dropped. No `null` sentinel.

**When it fires, and why that is cheap.** Once per goal, when the goal **first becomes eligible**
(gains a schedule, or a second step) — wide over every newly-eligible goal, result stored on the
goal. Because dismissal is permanent, **a goal is proposed at most once, ever**, so the feature adds
no recurring load against the 30-RPM ceiling `C11b`'s wide call exists to protect.

## The non-AI fallback — a mechanical half, not a degraded one

`#44` question 3 asked whether the honest fallback is *no proposal at all*, as `plan`'s is. **It is
not**, and frame 4 is the argument: where the goal already carries structure, the proposal is pure
arithmetic.

| Situation | With no model |
|---|---|
| `openStepCount ≥ 2` | **Count the steps you already listed** — `COUNT` · *steps* · target = the count |
| `occurrencesPerWeek ≥ 1` | **Count the occurrences you already schedule** — target from the schedule |
| neither | **no proposal at all, silently** — frame 5 |

Silent is right by §0.4 as `C13` §5 refined it: *speak about a failure the user can act on.* A model
that could not phrase a measure is not actionable, and the goal is not broken — it simply has no
number, which `C7` already made legal.

## Rounds, and what the renders caught

Five rounds. **Six of the seven defects were invisible in the source** — the rule `C12` established,
holding again.

1. **Every `.card g-row` stacked its marker above its title, centred.** `.card` sets
   `flex-direction:column` and is the earlier rule at equal specificity, so `.g-row` never overrode
   it. Three frames wrong; nothing wrong in the markup.
2. **`.prov` as a flex row rendered its provenance sentence as three narrow columns** — *"Nothing to
   | compute | from."*
3. **The sheet floated on an empty screen with no scrim**, so nothing said what it had been opened
   *from* — and a bottom sheet without a scrim is not a bottom sheet.
4. **Both action pairs floated under the life-area list**, with three rows above them and nothing
   saying which goal either belonged to.
5. **In neo the sheet is transparent**, because neo's surface *is* the page colour plus shadows — so
   over the scrim the dimmed screen read straight **through** the sheet and collided with its own
   title. Invisible in glass, where the blur hides it. **A sheet is the one neo surface that must be
   opaque, being the only one with something behind it.**
6. **The Hebrew sheet title read `מספר ללהיכנס לכושר`** — a double lamed from concatenating `מספר ל`
   with a title that already starts with one. *A design is not finished until it has been seen in
   Hebrew*, a fourth time.
7. **Rev 2's `#` marker was a dashed circle**, indistinguishable at a glance from `C19`'s dashed
   circle carrying `+` on the row below it — two different invitations reading as one. Now a square.
   Caught in a dark-neo Hebrew close-up.

Numbers, ranges and chapter spans are wrapped in `<bdi>`; `Read Clean Architecture` is deliberately
kept as a goal title so a Latin string inside an RTL heading is exercised rather than assumed.

## Frozen inputs — not re-opened here

`C7` #14 (a measure is a closed kind + a free word; absence is the default) · `C11a` #16 (enums
50/50, free numbers 2×, membership checks) · `C11b` #30 (wide call, per-group validation, no retries)
· `C10` #29 (the AI judges the app computes; and the daily surface's slots) · `C5` #21 (an endless
goal has no percentage and that is not degraded — nothing here shows one) · `C12` #31 (the material
contract) · `C19` #41 (the offer shape, and `Let it go` as a command rather than an inference) ·
`C18` #39 and `C9a` #25 (the structure the mechanical half counts) · §0.7 (intrinsic needs consent,
instrumental does not) — **the clause that decided this ticket.**
