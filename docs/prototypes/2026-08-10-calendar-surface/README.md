# PROTOTYPE — the in-app calendar surface (`C9b`, [#26])

> **Throwaway. Not app code. Nothing here ships.**
> The map's standing preference is *plan, don't do* — no ticket on
> [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) ships code.

**Plan, in one line:** three structurally different answers to *"what is a calendar
for in this app"*, switchable via `?variant=A|B|C` on one page, each rendered inside a
phone frame at 390×844, with a Hebrew/RTL toggle.

## How to open it

```powershell
start docs\prototypes\2026-08-10-calendar-surface\index.html
```

No build, no emulator, no Gradle daemon. `←`/`→` cycle variants; the pill at the
bottom also switches **HE / EN**, which flips the whole frame to RTL.

## Why HTML and not a throwaway Compose route

The skill's default is a throwaway route inside the real app, and it is the better
shape *when it is available*. Here it is not, for two committed reasons:

1. **The map forbids it.** `#12`'s Notes: *"No ticket on this map ships code."* A
   `Routes.CALENDAR_PROTOTYPE` under `app/` is code in the app whatever it is named.
2. **It would take two exclusive singletons** — the Gradle daemon and an emulator —
   for a question about *layout*, while a sibling session (`c17-many-to-many`) is live.

The cost is real and worth stating: **an HTML mockup cannot prove a Compose layout
compiles, scrolls at 60fps, or survives a real `LazyColumn`.** It answers *what should
this be*, which is what #26 asks. Anything that needs to be proven in Compose is
handed to the build session, not decided here.

The one place HTML is strictly *better*: `dir="rtl"` mirrors the whole frame in one
attribute, so **"how does a calendar grid survive Hebrew" is something you can look at**
rather than reason about. That is one of the six things the ticket must settle and the
only one prose reliably gets wrong.

## Rev 2 — Ido's review, 2026-08-10

Four things changed, and one of them was a design error rather than a taste call.

1. **A block shows its end time**, not only its start — `09:00–11:00`.
2. **The rung glyphs are gone.** They were unreadable because *the chip was carrying
   two unrelated axes*: the **rung** (a property of the occurrence) and the **life
   area** (a property of the goal). Forced into one pill, the rung had to degrade
   into a symbol. The chip now carries **only** the life area — a colour dot and its
   name — and the rung is carried by **the form of the leading time column**:

   | Rung | Time column | Reads as |
   |---|---|---|
   | `BLOCK` | start over end with a **filled rail** between | a span of time you are inside |
   | `DEADLINE` | `due` + the time, then a **single point** | a moment, not a duration |
   | `SPAN` | a date range + a **soft capsule** | days, not hours |
   | `ALL_DAY` | the words **all-day**, no time at all | a day with no slot |

   No legend and no symbol vocabulary to learn — form first, then words, and icons
   only where there is no room for words. A regression test asserts that none of the
   four old glyphs survives anywhere in any variant, in either language.
3. **C was rebuilt as a decision stack**, because "I couldn't tell C from B" was
   correct: both had become lists of rows. C now has **no list and no browsing at
   all** — one card, `1/3`, two buttons, a stack peeking behind it. Its cost is now
   honest and visible: you cannot see a week from it.
4. **Plurals fixed** in both languages, both branches — `4 בלוקים` / `4 blocks`,
   and `בלוק אחד` / `1 block` at n=1.

Plus a full visual pass: the flat white cards were replaced by the **M3 tonal
container ladder** derived from the Aurora skin (five levels between `surfaceBright`
and `surfaceDim`), event fills went from saturated-with-white-text to **tinted with
coloured text**, corner radii moved onto the M3 Expressive scale, the type scale was
rebuilt around one weight axis, and every icon is now hand-authored inline SVG so
nothing depends on a font or a CDN.

## Rev 3 — Ido's second review, 2026-08-10

**The clipped times in A had two causes, and the second one is a product decision.**

1. **Bidi reordering.** `09:00–12:00` is a Latin-digit run inside an RTL paragraph, so
   the Unicode bidi algorithm reorders it — it renders as `12:00–09:00` and *then*
   clips. Every time string in the prototype now goes through one helper that wraps it
   in `direction:ltr; unicode-bidi:isolate`. This is a real Hebrew-app defect class,
   not a mockup artefact: **the same bug will appear in Compose** unless the build
   session isolates time and date strings the same way.
2. **46 pixels.** Seven columns on a 390 dp phone leaves ~46 dp per day. No Hebrew
   title fits, and neither does `09:00–12:00`. Widening the text was never going to
   work — so **A now has a `day / 3 days / week` switch and opens on 3 days**, where a
   column is ~110 dp and both the title and a single-line range fit comfortably.
   Week view keeps the times **stacked start over end**, which is exactly what Ido
   proposed and is the only thing that fits at 46 dp.

That switch is not a detour: *"which views — day, week, month, agenda — and which one
opens by default"* is the **first** thing #26 says it must settle. The prototype now
has an answer to react to rather than a question to discuss.

Craft pass, since he asked for it: tabular figures so columns of times stop looking
ragged; sticky headers that blur content passing beneath them; the today column tinted
and the Fri/Sat weekend columns shaded; a live `now` pill on the current-time line;
part-of-day group headers in B (morning / afternoon / evening) so a flat list becomes
scannable; staggered entry animation; a gradient hero card and heavier display type in
C; M3-Expressive nav pill; two-layer tinted shadows throughout.

## What the three variants disagree about

They are not three skins. They disagree about the **primary affordance** — what the
screen is *for* — and therefore about what belongs on it and where it lives in
navigation.

| | **A — The Grid** | **B — The Day Rail** | **C — The Decision Stack** |
|---|---|---|---|
| Primary affordance | **Place time** (drag a block into a slot) | **Tick things off** | **Decide, one at a time** |
| Shape | Real hour grid, Sun→Sat | Week strip + agenda list, one day at a time | One card at a time, `1/3`, stack behind it |
| Shows | Everything — blocks, all-days, spans, deadlines, **challenge windows** | Tasks and deadlines only; goal deadlines as a chip | **Only what needs a decision** |
| Lives in nav | A **5th bottom tab** | A segmented control **inside Goals** | On **Home**; the grid is a top-bar icon |
| Costs | 5 tabs is a crowded bar | No cross-day view at all | **No browsing at all** — you cannot see a week |

## The data is one week, shared by all three

Same twelve occurrences in every variant — they are three views of one week, not three
demos. It deliberately contains every state `C9a` defined, because a mockup that only
shows the happy path settles nothing:

- all four rungs — `ALL_DAY` · `DEADLINE` · `BLOCK` · `SPAN`
- `PROVISIONAL` (dashed, agent-placed, not yet on Google) beside confirmed
- one `MISSED` block, one `OVERDUE` deadline (**late but still owed — not a failure**),
  one `EXPIRED` provisional (**counts for nothing**)
- Hebrew titles under Ido's real life areas, in the committed `GoalCategory` colours

## What it is for

Flip through, then say **which affordance is right** — and expect the useful answer to
be *"the header from A with the review from C"*. That recombination is the actual
finding; the variants exist to make it sayable.

[#26]: https://github.com/idomarhaim/Android_Final_Project/issues/26
