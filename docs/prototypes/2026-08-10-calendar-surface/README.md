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

## What the three variants disagree about

They are not three skins. They disagree about the **primary affordance** — what the
screen is *for* — and therefore about what belongs on it and where it lives in
navigation.

| | **A — The Grid** | **B — The Day Rail** | **C — The Review** |
|---|---|---|---|
| Primary affordance | **Place time** (drag a block into a slot) | **Tick things off** | **Confirm or reject what the agent proposed** |
| Shape | Real hour grid, Sun→Sat | Week strip + agenda list, one day at a time | Review sheet first; month heatmap demoted |
| Shows | Everything — blocks, all-days, spans, deadlines, **challenge windows** | Tasks and deadlines only; goal deadlines as a chip | **Only what needs a decision** |
| Lives in nav | A **5th bottom tab** | A segmented control **inside Goals** | On **Home**; the grid is a top-bar icon |
| Costs | 5 tabs is a crowded bar | No cross-day view at all | You cannot browse a week |

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
