# `C19` · Success and failure inside a life area — prototype

Asset for [`C19` #41](https://github.com/idomarhaim/Android_Final_Project/issues/41) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 4.**
Open `index.html`; material, theme and language switch in the bar or by query string so
`shoot.ps1` can render any state:

```powershell
cd docs\prototypes\tools
./shoot.ps1 -Page ..\2026-08-13-area-success-failure\index.html -Out out.png `
            -Query "m=neo&t=light&l=he" -Width 1880 -Height 1300
```

`m=glass|liquid|neo|darkneo` · `t=dark|light` · `l=en|he`.

## What the four frames prove

| Frame | Screen | Proves |
|---|---|---|
| 1 | **Health** — an area in good shape | the pair is **two numbers, never a rate** (`C3`); the run is the record (`C5` §4); the block sits **above the goals it summarises**, not on a screen of its own |
| 2 | **Career** — an area in bad shape | the tone rule survives bad news: same component, **no red**, and the good line is still filled by naming the one kept window |
| 3 | **Analytics** | the counterpart to the time donut, with `C17`'s asymmetry **stated on the screen** rather than hidden |
| 4 | **Learning** — almost nothing ever scheduled | the case none of the closed tickets covers: `asleep` is a **row in the goal list**, unscored, with a way out in both directions |

## The three proposals embodied here

Each is derived from a closed ticket, so it is an input rather than a fresh opinion — and each is
Ido's to overturn.

1. **A never-scheduled goal is `asleep`, not failed.** `C9a` ruled that an unconfirmed occurrence
   expires silently because *you cannot fail to do something you never agreed to*. Nothing was ever
   owed on a goal with no repeat rule, so it cannot be missed — but it does not vanish either, or
   the app quietly agrees you gave up. It is therefore **visible, named, and in neither number**.
   The footer says so in words, because a state that is silently excluded from a count reads as a
   bug.
2. **Nothing ages out of history; the *window* is a query.** `C5` §2 refused any value that moves
   on wall-clock time, and a "failures older than N weeks stop counting" rule is that same value
   wearing a different hat. So the record is permanent (`C5` §4 — a missed occurrence is never
   edited) and the **view reports over a window you pick** — `30 days · 8 weeks · 6 months` — which
   is a filter over history, not decay of it. There is no lifetime failure counter anywhere: a
   number that can only ever go up is the "list of the things you are bad at" failure mode with a
   graph attached.
3. **Both placements, and they are one component.** The life-area screen carries it because the
   subject is per-area; the analytics screen carries it because `C17` deliberately put the *divided*
   number (minutes) and the *undivided* one (successes) on one screen. The asymmetry line is on the
   analytics frame **only** — rev 1 printed it on every area frame, which said the same thing twice
   and never where the two numbers actually meet.

**What a window is** — the ticket's headline question — is answered **on the screen**, under the
run: *a window counts as kept when everything due in it was done*. The numbers are meaningless
without it, so it is not spec-only text.

## The one question that is genuinely Ido's

Frame 4 is the proposal, not the answer. **Should an abandoned goal be `asleep` (as drawn),
invisible, or a failure?** It turns on how he wants the app to treat him when he has quietly
stopped, which is not derivable from any closed ticket — `C9a` fixes only the case where the *app*
proposed the work.

## Material contract

`#12`'s Standing preferences require a screen designed against **surface · groove · elevation ·
accent**, so the four material blocks are adapted from
[`2026-08-13-log-progress`](../2026-08-13-log-progress/index.html), which adapted them from
[`2026-08-11-visual-styles`](../2026-08-11-visual-styles/index.html) (`C12` #31). Carried forward:
**`--edge`** (no affordance is shadow-only — neo's WCAG failure) and **`.tag`** (a life area is
named in words beside its dot, because dark neo collapses the categorical hues into one ramp).

One rule this screen adds, for the same reason: **outcome state never rides on hue either.** Kept is
**filled**, missed is **hollow**, still-owed is **dashed with a centre pip**, nothing-due is
**dotted**. The run therefore reads in dark neo, in greyscale, and to a colour-blind eye — and
**there is no red on this screen at all**, which is a tone decision as much as an accessibility one.

## Rounds, and what the renders caught

Five rounds. **Seven of the nine defects were invisible in the source** — the rule `C12` established
and `C6` reproduced, holding a third time.

1. **`30 days` wrapped to two lines inside its own pill.** One `white-space:nowrap`.
2. **A Learning goal (`Learn to cook`) was listed under Health *and* Career.** Same class as
   log-progress's steps reading filed under a weight goal: a content error that only appears when
   you look at the screen it is on.
3. **The block floated on an otherwise empty screen**, which answered none of *life-area block,
   analytics counterpart, or both*. The area's goal list now sits under it, and the dormant panel
   dissolved into a **state in that list** — a better answer to the ticket than the panel was.
4. **"1 goals here are asleep."** The count is data, so the plural bug existed only in the render.
5. **Learning showed 11 kept / 7 missed above a list with one active goal and two asleep.** The
   numbers contradicted the list they sat on top of. Now 5 / 2, and the run pattern matches both.
6. **The still-owed dot was nearly invisible** at 17 px on glass — so the one state that must *not*
   read as a failure read as one. It now carries a centre pip: the only dot with an inside and an
   edge.
7. **The missed segment of the analytics bar read as empty track** — the one thing the bar exists to
   show. Denser hatch, 60% opacity.
8. **An unbalanced `</div>` I introduced in round 3** broke the frame nesting: every caption escaped
   its phone and the stage wrapped into two rows. Visible instantly in the render, invisible in a
   diff of the string it lives in — and it was the **Hebrew** render that surfaced it, which is why
   *a design is not finished until it has been seen in Hebrew* keeps earning its place.
9. **Secondary text at `opacity:.64` was unreadable in liquid glass**, because the asleep rows sit
   lowest on the screen and that is exactly where the page gradient runs hottest.

Numbers, dates and ranges are wrapped in `<bdi>`, since bidi reorders `2 of 8` — the same finding
`C9b` recorded, and it recurs in Compose.
