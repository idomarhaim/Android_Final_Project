# `C22` · The measure proposal — prototype

Asset for [`C22` #44](https://github.com/idomarhaim/Android_Final_Project/issues/44) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 1** — the ticket is
**open**: it is `wayfinder:prototype`, therefore **HITL**, and one question below is Ido's and has not
been answered. Open `index.html`; material, theme and language switch in the bar or by query string
so `shoot.ps1` can render any state:

```powershell
cd docs\prototypes\tools
./shoot.ps1 -Page ..\2026-08-15-measure-proposal\index.html -Out out.png `
            -Query "m=neo&t=light&l=he" -Width 2740 -Height 1180
```

`m=glass|liquid|neo|darkneo` · `t=dark|light` · `l=en|he`.

## What the six frames are

| Frame | Screen | Proves |
|---|---|---|
| **A** | the goal's own screen | the offer standing **where the missing bar would be**, with the week it computed `3 a week` from drawn underneath it |
| **B** | the life-area screen | the offer as a **third sibling of `C19`'s pair**, attached to the row it acts on |
| **C** | the daily review | the offer sharing a screen with `C10`'s quote and practical line — the only placement that arrives **unasked** |
| **4** | the sheet | what accepting opens: outcome **or** leading indicator, and where each target comes from |
| **5** | no model · 1 | the offer surviving on **arithmetic alone** — same component, no model behind it |
| **6** | no model · 2 | the app **saying nothing**, on a real screen, which is the only way to see whether it reads as deliberate or broken |

**A, B and C are the variants**, and they disagree about one thing: **when a person is receptive.**
Not about what the offer says — the offer is deliberately *one component* in all three, because an
offer that had to be redrawn per placement would be three features rather than one.

## What is Ido's, and is still open

**Which placement.** It turns on when *he* is receptive to being offered a number, which is a fact
about him and not derivable from the code, the tickets or this design:

- **A · the goal's own screen** — receptive because he is already looking at that goal. Nothing ever
  interrupts; the cost is that a goal he never opens is never offered anything.
- **B · the life-area screen** — receptive because he is reviewing the area. Batches naturally, and
  inherits a component `C19` already decided; the cost is that the offer sits one level away from the
  goal it is about.
- **C · the daily review** — receptive because he set time aside to plan. The cost is that it is the
  **only placement that arrives unasked**, and it competes with `C10`'s feed for the same screen.

**This is not merely a placement question, which is why it is asked before the rest is finalised:**
if the answer is **C**, the call can ride `daily`'s envelope (already one call a day, already
goal-scoped) and there is no fifth AI feature at all. If it is **A** or **B**, there is one — see
below.

## What was derived rather than asked

Each follows from a closed ticket, so it is an input rather than a fresh opinion — and each is Ido's
to overturn.

1. **The absence is stated as legal *before* anything is offered.** Every unmeasured goal carries the
   line *"No number on this one. That is a choice, and it stays one — nothing here is incomplete."*
   `C7` makes absence **the default** (`E6`), and §0.4 makes it legal-but-never-silent. An offer that
   arrives without that line reads as a **correction of a decision he made on purpose**, which is the
   failure mode `#44` names as the most easily-resented surface on the map. It is also what makes
   frame 6 — where the app offers nothing — read as deliberate rather than as a load failure.
2. **An offer never borrows the visual language of an outcome.** Everything here is **dashed and
   hollow**; the filled accent means *kept* on `C19`'s screen and must not appear on something the
   app has not done. Same reasoning as `C19`'s *no next step* marker being deliberately unlike all
   four outcome dots.
3. **The dismiss is a peer of the accept** — same size, same row, no colour. `C7` makes absence the
   default, so *"not for this goal"* is not the lesser branch. A dismiss drawn small is a dark
   pattern; drawn red it says a legitimate choice was a mistake.
4. **Dismissal is permanent for that goal**, not snoozed. `C7` says *dismissible per goal*, and a
   default that re-asks is not a default. The manual path (`Something else` → the measure editor)
   always exists, so nothing is unreachable — it is simply never volunteered again. *(In placement C
   the second button reads `Not now`, because a card in a daily feed that cannot be deferred is worse
   than one that can.)*
5. **The leading indicator is preselected, and only when it has a real target.** Not taste: it is the
   only branch whose number the app can **compute** (`3` runs a week are already on the schedule).
   The outcome branch offers `kg lost` with *"target — you set it"*, because there is nothing to
   compute from and the model is not allowed to supply one.

## The schema — feature **E**, `measure`

Obeys [`C11b` #30](https://github.com/idomarhaim/Android_Final_Project/issues/30) unchanged: one
**wide** call, **per-field-group** validation, the Function **validates and omits, never
substitutes**, **no retries**, and the envelope carries `language`, optional `provider/model/key`,
and the membership list.

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

**The one thing worth arguing about: there is no number in the response.** `targetSource` is a
prompt-declared enum naming **which arithmetic the app should run** — `SCHEDULE` → the occurrences
already on the goal, `STEPS` → the count of its open sub-tasks, `USER` → ask him. This is
[§0.5](../../PRODUCT_v0.3.md) at full strength, and it is forced rather than chosen: `C11a` measured
**free numbers swinging 2× run-to-run and 1.8× between languages**, so a model-supplied target is the
one field that could not be trusted and the one the whole feature would be judged on. `C10`'s
strongest form applies — never let a value cross the wire whose failure you would have to *detect*.

`measureKind` and `basis` and `targetSource` are enums (**50/50 perfect** in `C11a`). `word` is the
only free field, and it is **content**, so §3.5's *"a goal title from the sorter — the model, once"*
governs it: proposed once, his the moment he accepts it, never rewritten afterwards (`C15b`).

**Validation** — `goalId` membership-checked (the one measured failure class); `word` length-capped;
any field failing validation is **absent**, and a proposal missing `measureKind`, `word` or
`targetSource` **is not a proposal**, so the whole element is dropped. No `null` sentinel.

## The non-AI fallback — a mechanical half, not a degraded one

`#44` question 3 asked whether the honest fallback is *no proposal at all*, as `plan`'s is. **It is
not**, and frame 5 is the argument: where the goal already carries structure, the proposal is pure
arithmetic.

| Situation | With no model |
|---|---|
| `openStepCount ≥ 2` | **Count the steps you already listed** — `COUNT` · *steps* · target = the count. Frame 5. |
| `occurrencesPerWeek ≥ 1` | **Count the occurrences you already schedule** — `COUNT` · *a week* · target from the schedule |
| neither | **no proposal at all, silently** — frame 6 |

Adding a row to §3.4:

| Feature | Absent field | Falls back to |
|---|---|---|
| `measure` | any | the **mechanical proposal** where structure exists; otherwise **no proposal**, silent |

Silent is right by §0.4 as refined by `C13` §5: *speak about a failure the user can act on.* A model
that could not phrase a measure is not actionable, and the goal is not broken — it simply has no
number, which `C7` already made legal.

## Fifth call, or does it ride an existing one?

`#30` retired the split-on-format axis and kept **different fallback behaviour** as the only reason
to split. This feature's fallback is not silence — it is a **different offer that still renders**, so
by `#30`'s own test it splits. It also cannot ride any of the four: `estimate` is task-scoped,
`classify` fires per quick-add row, `plan` is user-invoked.

**Except under placement C**, where `daily` is already one goal-scoped call a day and the proposal
would be one more field group on it. So: **fifth call under A or B; a field group on `daily` under
C.** That dependency is the reason the placement question is asked first rather than last.

## Rounds, and what the renders caught

Four rounds. **Five of the six defects were invisible in the source** — the rule `C12` established,
holding again.

1. **Every `.card g-row` stacked its marker above its title, centred.** `.card` sets
   `flex-direction:column` and is the earlier rule at equal specificity, so `.g-row` never overrode
   it. Three frames wrong; nothing wrong in the markup.
2. **`.prov` was a flex row, so its provenance sentence rendered as three narrow columns** —
   *"Nothing to | compute | from."*
3. **The sheet floated on an empty screen with no scrim**, so nothing said what it had been opened
   *from* — and a bottom sheet without a scrim is not a bottom sheet. The goal screen now sits dimmed
   behind it.
4. **Both action pairs in frame B floated under the list**, with three rows above them and nothing
   saying which goal either belonged to. They are now nested in the row they act on — which is also
   what makes *Give it a number* read as a sibling of `C19`'s pair rather than a screen-level command.
5. **In neo the sheet is transparent**, because neo's surface *is* the page colour plus shadows — so
   over the scrim the dimmed goal screen read straight **through** the sheet and the absence line
   collided with the sheet's own title. A sheet is the one neo surface that must be opaque, being the
   only one with something behind it. Caught in light neo; invisible in glass, where the blur hid it.
6. **The Hebrew sheet title read `מספר ללהיכנס לכושר`** — a double lamed from concatenating `מספר ל`
   with a goal title that already starts with one. Wrong Hebrew, and only visible rendered. *A design
   is not finished until it has been seen in Hebrew*, a fourth time.

Numbers, ranges and chapter spans are wrapped in `<bdi>`; `Read Clean Architecture` is deliberately
kept as a goal title so a Latin string inside an RTL heading is exercised rather than assumed.

## Frozen inputs — not re-opened here

`C7` #14 (a measure is a closed kind + a free word; absence is the default) · `C11a` #16 (enums
50/50, free numbers 2×, membership checks) · `C11b` #30 (wide call, per-group validation, no retries)
· `C10` #29 (the AI judges, the app computes) · `C5` #21 (an endless goal has no percentage and that
is not degraded — so nothing here shows one) · `C12` #31 (the material contract) · `C19` #41 (the
offer shape, and `Let it go` as a command rather than an inference) · `C18` #39 (sub-tasks, which is
what the mechanical half counts) · `C9a` #25 (occurrences, which is what it counts otherwise).
