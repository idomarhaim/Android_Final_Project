# c9b-calendar-surface — 2026-08-10

`/wayfinder 12`, no ticket named → **work through the map**, and choosing the ticket
is the session's job, not Ido's (wayfinder skill, *Work through the map* step 2).

## 🧭 Which ticket, and why

The frontier was **re-derived out of GitHub**, not read off the board — `SESSIONS.md`
says in as many words that every session which tried to predict it has been wrong at
least once. Querying `dependencies/blocked_by` for all 15 open children of
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12):

| Ticket | Blockers | On the frontier? |
|---|---|---|
| [#26 · `C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26) | #25 closed | **yes** |
| [#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) | #25, #17, #33 all closed | **yes** |
| [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39) | #37 closed | **yes** |
| [#38 · `C17`](https://github.com/idomarhaim/Android_Final_Project/issues/38) | #37 closed | unblocked but **assigned** — live session `c17-many-to-many` |
| #35, #31, #30, #28, #24, #23, #22, #21, #20, #19, #18 | ≥1 open blocker | no |

Took **#26**. It is the lowest-numbered frontier ticket, which is the skill's own
tie-break — but two things made it the right pick rather than merely the first:

1. **#39 is the wrong ticket to run beside a live `C17`.** Their *paths* are disjoint
   and their *subjects* are not: `C17` decides how a task attaches to several goals,
   `C18` decides what every roll-up sums over. Both write the same arithmetic. The
   board's disjointness test is about paths and would have passed this; it would still
   have been two sessions deciding one thing.
2. **#26 is the only `prototype` ticket on the frontier**, and every frontier ticket is
   HITL. Ido's attention is the scarcest singleton on this map and the one the board
   cannot enforce — a prototype front-loads agent work (build the artifact) before it
   needs him, where a second grilling would contend for him immediately.

## 🔒 Claim

- Ticket claimed the way this map claims: **assignee on GitHub** (#26 → `idomarhaim`).
- Board row added. `SESSIONS.md` taken under a **lease**, not a claim
  (`Lock-Path.ps1`), because it is a commons every session touches for seconds.
- **Overlap recorded rather than discovered later:** `c17-many-to-many` and this
  session both owe a line in #12's *Decisions so far*. Append-only, one line each,
  re-read the map body immediately before writing. Neither session edits the other's
  line, ticket or scope.
- `kb-candidates/` listed before the first unit of work, as owed: **two files, one
  always-ask entry each**, both destined for `rules/`, so nothing is drainable in
  either mode. They wait on Ido and on `/walkthrough`, not on a session.

## 🎨 The prototype

`docs/prototypes/2026-08-10-calendar-surface/` — one self-contained HTML page, three
variants on `?variant=A|B|C`, a HE/EN toggle that flips the frame to RTL, `←`/`→` to
cycle. Opens with `start …\index.html`; no build, no emulator, no Gradle daemon.

**Decision taken per principle, not asked:** the skill's default is a throwaway route
inside the real app, and it was rejected because #12's Notes say *"No ticket on this
map ships code"* — a `Routes.CALENDAR_PROTOTYPE` is code in `app/` whatever it is
named — and because it would take **two exclusive singletons** (Gradle daemon,
emulator) for a question about layout, while `c17-many-to-many` is live.

**The cost is stated rather than hidden:** HTML cannot prove a Compose layout
compiles, scrolls, or survives a real `LazyColumn`. Anything needing that proof is the
build session's, not this ticket's. The compensating gain is specific — `dir="rtl"`
mirrors the whole frame in one attribute, so *"does a calendar grid survive Hebrew"*
becomes something Ido can look at, and it is the one of the ticket's six questions
that prose reliably gets wrong.

The three variants disagree about the **primary affordance**, not the styling, and
therefore about content and navigation too:

| | A — The Grid | B — The Day Rail | C — The Review |
|---|---|---|---|
| For | placing time | ticking off | confirming what the agent proposed |
| Nav | a **5th** bottom tab | segmented control **inside Goals** | on **Home**; grid behind an icon |
| Shows | everything, incl. challenge windows | tasks + deadlines only | only what needs a decision |

One week of data, shared by all three, carrying every state `C9a` defined: four rungs,
`PROVISIONAL` dashed beside confirmed, one `MISSED`, one `OVERDUE` (late but still
owed), one `EXPIRED` (counts for nothing). Hebrew titles under Ido's real life areas
in the committed `GoalCategory` hexes.

## 🔁 Rev 2 — Ido's review

Four corrections. **One was a design error, not a taste call**, and separating those
two is the finding worth keeping.

- **A** — a block printed only its start time. Fixed: `09:00–11:00`.
- **B** — *"I can't read the little icons inside the life-area chips, and there's no
  connection between a square / triangle / double-arrow and what they mean."*
  **He was right about the cause, not just the symptom.** The chip was carrying **two
  unrelated axes at once** — the **rung** (a property of the *occurrence*) and the
  **life area** (a property of the *goal*). Jammed into one pill, the rung had no room
  to be anything but a cryptic symbol. The fix is a decomposition, not a nicer icon:
  the chip now carries **only** the life area, and the rung is carried by **the form of
  the leading time column** — a filled rail (`BLOCK`), a single point (`DEADLINE`), a
  soft capsule (`SPAN`), the words *all-day* with no time at all (`ALL_DAY`). Form
  first, then words, icons only where words do not fit. No legend survives.
- **C** — *"I couldn't understand the difference between C and B."* Also correct, and
  also structural: both had degenerated into **lists of rows**, so the intended
  difference (browse-many vs decide-one) was invisible. C is now a **decision stack**
  with no list and no browsing at all — one card, `1/3`, two buttons, two cards peeking
  behind. Its cost is now *visible on the screen* rather than argued in a table: you
  cannot see a week from it. The hero decision is the agent's **whole plan**, not one
  block, because `C9a` fixed confirmation as per-plan.
- **Plurals** — `הסוכן תכנן 4 בלוקים` / `The agent planned 4 blocks`, and the `n=1`
  branch too (`בלוק אחד` / `1 block`), which is where this class of bug usually hides.

Plus the visual pass he asked for: flat white cards replaced by the **M3 tonal
container ladder** derived from the Aurora skin (five levels between `surfaceBright`
and `surfaceDim`), event fills moved from saturated-with-white-text to **tinted with
coloured text**, radii onto the M3 Expressive scale, one weight axis in the type scale,
and **every icon hand-authored as inline SVG** so nothing depends on a font or a CDN.

## 🔁 Rev 3 — Ido's second review

*"The times inside a block in A are being cut off"*, with a screenshot. **Two causes,
and the second is a product decision rather than a styling fix.**

1. **The bidi algorithm was reordering them.** `09:00–12:00` is a Latin-digit run
   inside an RTL paragraph, so Unicode bidi reorders it to `12:00–09:00` — which is
   exactly what the screenshot shows — and *then* the overflow clips it. Every time
   string now passes through one helper that wraps it in
   `direction:ltr; unicode-bidi:isolate`. **This will recur in Compose**: it is a
   property of the text, not of HTML, so the build session owes the same isolation on
   every time and date string. Named here so it is not rediscovered from a screenshot
   a second time.
2. **46 pixels.** Seven columns on a 390 dp phone is ~46 dp per day — no Hebrew title
   fits and neither does a range, so no amount of type tuning would have worked. **A
   now carries a `day / 3 days / week` switch and opens on 3 days** (~110 dp per
   column). Week view stacks the times **start over end**, which is precisely Ido's own
   suggestion and the only thing that fits at 46 dp.

The switch is **on scope, not a detour**: *"which views — day, week, month, agenda —
and which one opens by default"* is the first bullet of #26's own "what it must
settle". The prototype now proposes an answer instead of posing the question.

Craft pass, as asked: tabular figures so a column of times stops looking ragged;
sticky headers that blur content travelling under them; today's column tinted and
Fri/Sat shaded; a live `now` pill on the current-time line; part-of-day group headers
in B so a flat list becomes scannable; staggered entry; a gradient hero and heavier
display type in C; an M3-Expressive nav indicator; two-layer tinted shadows.

## 🔁 Rev 4 — a sibling moved the ground, and the standing preference landed

**`C9c` ([#27](https://github.com/idomarhaim/Android_Final_Project/issues/27)) closed
mid-session and left a hand-off comment on #26** — posted rather than edited, because
#26 is this session's claim. It invalidated a premise the prototype was built on: Ido
chose a **two-way** sync, so **the app is no longer the only author of an occurrence.**
Reading it before continuing is the only reason rev 4 is current rather than obsolete.

Four states now drawn, in both languages and both themes: **`MOVED`** (changed in
Google — old time struck through), **`AWAY`** (*planned, but it left your calendar*;
`calendar.app.created` cannot tell a move-out from a delete), **`SILENT`** (agent-placed
and already confirmed because the slot was visibly free) and **`EXTERNAL`** (made by
hand in the GoalPilot calendar, no task behind it). `SILENT` and `PROVISIONAL` sit on
the **same day on purpose** — `C9c`'s point is that they differ by *visibility*, not
confidence. A deadline is drawn **only** in the all-day strip, never as a timed block,
which is now a test.

**One real bug, found by the checks rather than by looking:** B carried `OVERDUE` items
forward from other days but not `AWAY` ones, so an event that vanished from Thursday's
calendar would have surfaced only when Thursday arrived — *exactly too late to put it
back*. Both are carried now, for one reason: they need action and neither waits for you
to navigate to its date.

**Dark mode**, from the committed `AuroraDark` palette — the app ships `values-night`,
so a calendar that only works in light is half a design. The surface ladder also moved
onto the **committed** Aurora container tones; the previous revision had been guessing
values that the app already defines.

### 📥 Ido said yes: the design standard is now normative for the whole map

Added to [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)'s
**Standing preferences** — *"every screen is designed to a current UI/UX standard, not
merely specified"* — carrying the three rules his own defect reports bought, so they
bind `C12`, `C6` and anything else with a screen rather than living only here:
**one chip may not carry two axes** · **form and words before iconography** · **a design
is not finished until it has been seen in Hebrew.**

**The commons discipline was followed and is worth recording, because three sessions
were live:** the body was **re-fetched immediately before the write** (the earlier fetch
was already stale), the patch was **proved a pure insertion before sending** — 122 → 139
lines, `0` deleted — and every sibling's *Decisions so far* line was verified present
**after** the write. `#26`'s own index line is still owed by this session and remains
unwritten; another session's line is not mine to author.

## 🧪 Tests

**No app-code layer applies.** `C9b` is a decision ticket and the map's standing
preference is **plan, don't do** — nothing here ships into `app/`, so the unit,
instrumented, endpoint, database and rules layers are all untouched and none is
skipped silently. `docs/prototypes/` has no test layer by construction.

The prototype was still **checked rather than assumed runnable**, because "I wrote a
mockup and never opened it" is the classic way one ships an artifact that throws on
first paint:

- `node --check` on the extracted `<script>` — **syntax OK**, 316 lines.
- Rendered **all six** combinations (3 variants × HE/EN) headlessly against a stubbed
  DOM, asserting each returns non-thin HTML and contains **no `undefined`, `NaN` or
  `[object …]`** — the three tokens a template-string bug actually produces. All six
  pass: 7145/7151, 4633/4628, 3944/3935 chars.
- The week is **derived, not guessed**: the page computes the Sunday of the week
  containing 2026-08-10 and gets `Sun Aug 09 2026`, today index 1 → `Mon` / `יום ב׳`.
  Day names come from `Intl`, so the Hebrew ones are real rather than transliterated.

What this does **not** prove, and the resolution must not claim: that any of it lays
out correctly in Compose.

**Rev 2 added three regression checks**, each tied to a defect Ido actually found —
a review comment is worth more as a test than as a memory:

- **every** block in A prints a range, not a bare start (`10/10` blocks carry `–`);
- **none** of the four retired rung glyphs (`▮ ▼ ▭ ⟷`) survives anywhere in A, B or C,
  in either language;
- both plural branches print correctly in both languages at `n=1` **and** `n=4`.

The glyph check **failed on its first run** and the failure was the useful kind: the
surviving `▮` was in the fake status bar, not in a chip — a false positive of my own
test. It still earned its keep, because those block characters were visually
indistinguishable from the glyphs B had just dropped, so the status bar is now drawn
as inline SVG signal/wifi/battery indicators instead.

**Rev 3 raised the matrix to 3 variants × 2 languages × 3 views = 18 renders**, all
clean, plus four assertions tied to this round's defects:

- every time string is bidi-isolated, in all six language × view combinations;
- week view **stacks** (`06:00` / `07:00`, no dash) and 3-day view shows a **range**
  (`06:00–07:00`) — asserted on the extracted labels, not on the markup;
- the column count actually follows the switch (1 / 3 / 7);
- rev 2's glyph and plural checks still pass.

**Two of this round's failures were bugs in the test, not the page** — a `render`
identifier colliding with the page's own, and a regex expecting the dash immediately
before `</span>` when it sits mid-string. Recorded because the honest reading of a red
run is *"something disagrees"*, not *"the code is wrong"*, and both times the artefact
was fine.

**Rev 4 raised the matrix to 36 renders** — 3 variants × 2 languages × 3 views × 2
themes — all clean, and added assertions that each of `MOVED`/`AWAY`/`SILENT`/`EXTERNAL`
is drawn *and* labelled in both languages, that no deadline leaks into the timed grid,
that `C`'s queue carries Keep / Cancel / Put back and reads `1/4`, and that the dark
palette is actually wired rather than silently falling back to light.

**The tooling lesson, which cost four false failures across three revisions:** building
a check file with a shell heredoc destroys backslash escapes, and a surviving `\b`
inside a JS template literal becomes a **backspace character** — so
`class="[^"]*\baway\b[^"]*"` silently stopped being a word-boundary match and reported a
correctly-rendered element as missing. The checks are now **written as a file** and
match classes by `split()` rather than by regex. One of the four failures was real (the
`AWAY` carry above); the discipline that separates them is *reproduce the assertion in
isolation before touching the artefact*.
