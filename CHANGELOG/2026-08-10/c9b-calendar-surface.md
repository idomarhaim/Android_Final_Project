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
