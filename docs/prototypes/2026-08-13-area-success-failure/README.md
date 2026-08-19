# `C19` · Success and failure inside a life area — prototype

Asset for [`C19` #41](https://github.com/idomarhaim/Android_Final_Project/issues/41) on map
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12). **Revision 5** — the ticket is **resolved** by the comment on `#41`; this asset is what it was resolved against.
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
| 4 | **Learning** — almost nothing ever scheduled | the case no closed ticket covered: the row names the **missing step** and offers *that* step — **break it down** where nothing has been, **schedule the first one** where work exists without dates — counted in neither number |

## The three proposals embodied here

Each is derived from a closed ticket, so it is an input rather than a fresh opinion — and each is
Ido's to overturn.

1. **A never-scheduled goal is missing a *step*, and the app names which step — there is no
   dormancy state at all.** `C9a` gives the floor (*you cannot fail to do something you never agreed
   to*), so it is in **neither** number and the footer says so in words. But revisions 1–4 drew it as
   an `asleep` **state**, and that was wrong: `C10` had already decided a *"what moment is this
   goal in?"* axis over **days idle · open work · age**, whose `STARTING` value *is* "never
   scheduled". A stored or even named dormancy state duplicates it — the same objection `C5` §1 used
   to decline `GoalKind`, generalised in `kb/dev/enum-and-label.md` §5. `open work` also separates
   two situations one label flattens: **nothing broken down yet** wants *Break it into steps*
   (`C8`'s feature, no new AI surface), **work without dates** wants *Schedule the first one*
   (`C9a`'s). `Let it go` stays beside the offer as a **command, never an inference** — `C4` forbids
   the app asserting an intrinsic edge unasked.
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

## The question that was put to Ido, and what came back

He was asked whether an abandoned goal should be `asleep`, invisible, or a failure, and he
**handed the decision back** — *explain it simply, pick the highest-quality solution, improve it if
you can*. Per `rules/question-axis-naming.md` that forbids re-asking and requires **deriving**, and
the derivation found the **fork was false**: all three options were labels for `C10`'s existing
theme axis. Hence the *missing step* above, which was **not one of the three**. The decision is
**the agent's** and is recorded as such on
[#41](https://github.com/idomarhaim/Android_Final_Project/issues/41) and on `#12`; Ido can overturn
it.

**Held back deliberately, and now fog on `#12` rather than a ticket:** whether very long idleness
should ever retire a goal by itself. `C4` points at *never*; an area listing nine `no next step`
rows is its own accusation, which points at *something*. It cannot be phrased sharply until the
`STARTING` offer has been lived with.

## Material contract

`#12`'s Standing preferences require a screen designed against **surface · groove · elevation ·
accent**, so the four material blocks are adapted from
[`2026-08-13-log-progress`](../2026-08-13-log-progress/index.html), which adapted them from
[`2026-08-11-visual-styles`](../2026-08-11-visual-styles/index.html) (`C12` #31). Carried forward:
**`--edge`** (no affordance is shadow-only — neo's WCAG failure) and **`.tag`** (a life area is
named in words beside its dot, because dark neo collapses the categorical hues into one ramp).

One rule this screen adds, for the same reason: **outcome state never rides on hue either.** Kept is
**filled**, missed is **hollow**, still-owed is **dashed with a centre pip**, nothing-due is
**dotted**, and *no next step* is a **dashed ring carrying a `+`** — deliberately unlike all four,
because it is an invitation and not an outcome. The run therefore reads in dark neo, in greyscale,
and to a colour-blind eye — and
**there is no red on this screen at all**, which is a tone decision as much as an accessibility one.

## Rounds, and what the renders caught

Six rounds. **Eight of the ten defects were invisible in the source** — the rule `C12` established
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
9. **Secondary text at `opacity:.64` was unreadable in liquid glass**, because the quiet rows sit
   lowest on the screen and that is exactly where the page gradient runs hottest.
10. **Rev 5 said one thing twice.** With the footer naming the missing step beside the goals it is
    about, frame 4's *"nothing was missed here"* line was a duplicate — and it was pushing the two
    action pairs, which **differ** and are the entire point of that frame, off the bottom of the
    screen. Deleted; both offers now fit, verified in a Hebrew dark-neo close-up.

Numbers, dates and ranges are wrapped in `<bdi>`, since bidi reorders `2 of 8` — the same finding
`C9b` recorded, and it recurs in Compose.
