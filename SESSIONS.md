<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 🧭 Session claim board — GoalPilot

Who is working on what, **right now**. Read this before your first edit; claim
before your first write. Normative rule:
`C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.

> Running more than one session at a time is **opt-in and not the default**. It
> is legitimate only when the working sets are disjoint and the user has
> assigned them. If two claims would overlap, it is one session's work — run it
> sequentially.

## 🔒 Active claims

| Session | Task | Owns (paths) | Singletons | Claimed |
|---|---|---|---|---|
| `c6-log-progress` | `/wayfinder 12` → resolve [#22 · `C6`](https://github.com/idomarhaim/Android_Final_Project/issues/22) (which fields a user may set by hand in LOG PROGRESS, whether a hand-set value corrects history or joins it, and what happens when it contradicts what completed tasks imply). Ticket claimed by assignee on GitHub | `CHANGELOG/2026-08-11/c6-log-progress.md` *(new)*, `docs/prototypes/2026-08-11-log-progress/` *(new, if the screen needs one — see §2 below)*, `kb-candidates/2026-08-11-c6-log-progress.md` *(new, if anything is flagged)*, GitHub issues **#22** and **#12** | **Ido's attention** — the third live HITL ticket on this map at once (`c12`'s prototype is at revision 3, `c8`'s grilling is 30 minutes old); see §3 below. No build, no device, no Firebase | 2026-08-11 |

> ⚠️ **A tenth session joined the same map — `c6-log-progress` on
> [#22](https://github.com/idomarhaim/Android_Final_Project/issues/22) — and it takes the
> ticket the previous claim declined, on the ground that ticket's objection was never a
> subject collision.** `/wayfinder 12` was invoked with the **map**, not a ticket, so the
> pick was the agent's. Frontier **re-derived out of the dependencies API at session start**,
> not read off the Unclaimed-work block: `/issues/12/sub_issues` enumerated, then every open
> child queried for `blocked_by`. Result — **25 children, 18 closed, 7 open**; frontier
> (open · unblocked · unassigned) = **`#20 · C2`, `#21 · C5`, `#22 · C6`**. `#24 · C8` and
> `#31 · C12` are unblocked but **assigned and live** — `c12` committed `d499158` (rev 3)
> **one minute** before this claim, and `c8`'s row is 30 minutes old with its changelog file
> already on disk, so neither is a stale lease. `#30 · C11b` (blocked by `#20` **and** `#24`)
> and `#35 · C15b` (blocked by `#24`) remain the only blocked tickets. Ninth derivation of
> the day; membership unchanged since `c8`'s, which is itself the finding — **the frontier
> has stopped moving, because every ticket that could unblock anything is already claimed.**
>
> **`#22` was taken, and the decisive question was which objection survives contact with this
> board's own doctrine.** All three frontier tickets carry one, so *having* an objection
> discriminates nothing:
> 1. **`#22 · C6` — its objection is *attention*, which this board has twice recorded it
>    cannot serialise.** `c8` declined it 30 minutes ago because *"a second **screen** does
>    contend for the one singleton this board cannot serialise"* — and that ground has **not
>    expired**; `c12`'s prototype is live. What makes it takeable anyway is that it is the
>    only frontier ticket with **no subject collision**: every one of its inputs — `C1`
>    ([#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)), `C3`
>    ([#18](https://github.com/idomarhaim/Android_Final_Project/issues/18)), `C7`
>    ([#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)) — is **closed and
>    released**, so they are foreign state to *read*. `C7` handed this ticket work by name
>    (*"a goal also carries an input mode (`buttons · number · tick · auto`); its screen is
>    `C6` #22's"*), and issue [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)
>    (`U6`/`R25`, the repeat-tappable fill buttons) lands on this same screen — leverage
>    outside the map, the way `#10` was for `C12`.
> 2. **`#20 · C2` — declined, because it changes the inputs of *both* live sessions, not
>    one.** Its own body names *"it drives the time-allocation analytics that already ship"*
>    (that is `#31`, live, drawing charts right now) **and** *"it informs point and time
>    estimation"* (that is `#24`, live, deciding what one AI-emitted stage carries). This is
>    the refusal the board has made six times, doubled. It is also the highest-leverage
>    ticket left — closing it halves `#30`'s blockers — which is exactly why it should be
>    taken **after** `c12` and `c8` release, not against them.
> 3. **`#21 · C5` — declined on a *subject* collision with the live `#31`, which is a
>    different and stronger objection than `#22`'s.** Its first bullet asks *"what is its
>    percentage, if it has one at all?"*, and a goal's percentage is precisely what `#31`'s
>    charts render; `c12` is at revision 3 with Ido. The four older grounds (proximity to the
>    then-live `#19`, then `#28`) have all expired, and the ticket remains the heaviest on the
>    map — a Firestore schema change over Ido's live data whose migration is still fog.
>
> Three coupling points, all named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is a commons, and the race it names has fired for real
>    twice** (`c3-points-currency` and `c1-points-and-time` each record it). Same discipline,
>    no exceptions: **re-fetch `#12`'s body immediately before appending**, `cmp` it against
>    the copy the line was built on, write only this session's line, verify a pure insertion
>    afterwards. `C13` ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s
>    index gap stays Ido's to assign.
> 2. **`C6` is a screen, so the design standard binds it** — `#12`'s Standing preferences make
>    *"every screen is designed to a current UI/UX standard, not merely specified"* normative,
>    with the three rules `C9b`'s eight revisions bought (**one chip may not carry two axes** ·
>    **form and words before iconography** · **a design is not finished until it has been seen
>    in Hebrew**). The ticket is labelled `wayfinder:grilling`, but a resolution that only
>    lists what is editable would not satisfy that preference; a prototype path is reserved on
>    the row above rather than promised, and it ships **one revision at a time**, stopping the
>    moment Ido stops answering.
> 3. **Two live edges, both posted rather than taken.** Anything found here bearing on `#31`'s
>    charts (`C3` §7 — *past the target the app stops speaking in percent*) is **commented on
>    `#31`**; anything bearing on `#24`'s plans is **commented on `#24`**. Nothing a live or
>    released session owns is edited. Flow stays one-way, as `c9c`, `c3`, `c18`, `c14`, `c1`,
>    `c9e`, `c12` and `c8` all established.
>
> 📥 **`kb-candidates/` listed before the first unit of work, as the folder's existence
> requires — four files, and each was opened and its own *Destination*/*Status* lines read
> rather than inherited from `c8`'s note.** Confirmed independently: three target `rules/`
> ([`c1`](kb-candidates/2026-08-10-c1-points-and-time.md) and
> [`c9e`](kb-candidates/2026-08-10-c9e-event-lifecycle.md) both amend
> `rules/question-axis-naming.md`; [`c16`](kb-candidates/2026-08-10-c16-milestone-model.md)
> amends the same file's *widening* clause), and
> [`c9f`](kb-candidates/2026-08-09-c9f-consent-screen-state.md) names `kb/dev/` but is
> **parked by Ido's own call** pending a `rules/` proposal. All four are **always-ask in both
> modes and none is this session's** — `AUTO MODE` drains nothing here.
>
> 🛠 **The Unclaimed-work block below is still stale and is deliberately left alone.** `c8`
> flagged it 30 minutes ago (queried before `#19` closed, still listing five tickets as
> blocked behind it) and did not rewrite it; neither does this session, because two sessions
> are live in this file and a commons rewrite is the one edit that would collide with both.
> The authoritative frontier is the derivation at the top of this note.
>
> Recorded by `c6-log-progress` on claiming.

| `c8-ai-task-plans` | `/wayfinder 12` → resolve [#24 · `C8`](https://github.com/idomarhaim/Android_Final_Project/issues/24) (what a "stage" is, what re-flowing means, what a stage is worth, and whether a proposed plan is a draft or written tasks). Ticket claimed by assignee on GitHub | `CHANGELOG/2026-08-10/c8-ai-task-plans.md` *(new)*, `kb-candidates/2026-08-10-c8-ai-task-plans.md` *(new, if anything is flagged)*, GitHub issues **#24** and **#12** | **Ido's attention** — a grilling ticket, and `c12-charts-presentation`'s prototype is already live and asking for him; see §3 below. No build, no device, no Firebase: this is Markdown and issues only | 2026-08-10 |

> ⚠️ **A ninth session joined the same map — `c8-ai-task-plans` on
> [#24](https://github.com/idomarhaim/Android_Final_Project/issues/24) — and it is the
> first claim taken since `C1` closed, which is the largest change the frontier has had
> all day.** `/wayfinder 12` was invoked with the **map**, not a ticket, so the pick was
> the agent's. Frontier **re-derived out of the dependencies API at session start**, not
> read off the Unclaimed-work block: every open child of `#12` queried for `blocked_by`.
> Result — **25 children, 18 closed, 7 open**; frontier (open · unblocked · unassigned) =
> **`#20 · C2`, `#21 · C5`, `#22 · C6`, `#24 · C8`**. `#31 · C12` is unblocked but
> **assigned and live**. `#30 · C11b` and `#35 · C15b` are the only tickets still blocked.
> Eighth derivation of the day, and the frontier has **doubled from two to four**.
>
> 🛠 **The re-derivation corrected a released session's own summary, which is the reason it
> is done rather than inherited.** `c1-points-and-time`'s release note below states that
> closing `#19` unblocked *"`#20`, `#22`, `#24`, and through `#24` both `#30` and `#35`
> … nothing on `#12` is blocked any more — the whole remaining map is frontier."* **The
> last clause is false.** `#30` is blocked by `#20` **and** `#24`, and `#35` by `#24` —
> all three of those blockers are **open**, so neither `#30` nor `#35` is on the frontier.
> `C1` unblocked three tickets, not five. Nothing was edited in that session's note (a
> released session's row is not this session's to rewrite); the correction lives here,
> and the **Unclaimed-work block below is separately stale** — it was queried before `#19`
> closed and still lists five tickets as blocked behind it.
>
> **`#24` was taken, and the three declines each rest on a different ground:**
> 1. **`#24 · C8` is the disjoint one with the most leverage.** Both its blockers —
>    `C4` ([#13](https://github.com/idomarhaim/Android_Final_Project/issues/13)) and
>    `C1` ([#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)) — are
>    closed **and released**, so its inputs are foreign state to *read*. And it is a
>    blocker of **both** remaining blocked tickets: `#35` is blocked by `#24` **alone**,
>    and `#30` by `#20` + `#24`. Closing it frees `#35` outright and halves `#30`. No other
>    frontier ticket frees anything.
> 2. **`#20 · C2` — declined, because it would change a live session's inputs mid-flight.**
>    `C2` asks whether an AI-assigned task type is a second axis **or a replacement for
>    life areas**, and its own body names *"it drives the time-allocation analytics that
>    already ship"* as a candidate purpose. `c12-charts-presentation` is **live right now**
>    deciding the chart set and the dashboard arrangement, and `C9b` handed it a rule about
>    that very chart. Re-cutting what the charts group by, while they are being drawn, is
>    exactly what this board has refused five times.
> 3. **`#22 · C6` — declined on prototype contention, the objection that expired for `#31`
>    and has now re-armed against the ticket behind it.** `C6` decides what a user may edit
>    in a **screen**, and since the design standard became normative that is prototype-grade
>    work, not a paragraph. `c12` is already **at revision 2** of a prototype burning Ido's
>    attention. Every frontier ticket here is HITL, so HITL-ness discriminates nothing —
>    but a second *screen* does contend for the one singleton this board cannot serialise.
> 4. **`#21 · C5` — declined on subject overlap, not on the ground four earlier sessions
>    used.** Their objection (it sits too near the live `#19`) **has expired**; `#19` is
>    closed and released. What remains is that `C5`'s decay mechanic changes what a goal's
>    **percentage** means, and a goal's percentage is what `#31`'s charts render. It is also
>    the heaviest ticket on the frontier — a Firestore schema change over Ido's live data,
>    with the migration itself still fog.
>
> Three coupling points, all named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is a commons, and the race it names has fired for real
>    twice** (`c3-points-currency` and `c1-points-and-time` each record it). Same discipline,
>    no exceptions: **re-fetch `#12`'s body immediately before appending**, write only this
>    session's line, verify a pure insertion afterwards. `C13`
>    ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s index gap
>    stays Ido's to assign.
> 2. **`C8` arrives with three released decisions binding it, so they are inputs, never
>    subjects.** `C4` (#13, the goal↔task ontology), `C1` (#19 — **the model never emits a
>    point value**; the shape is `taskId` + `difficulty ∈ LIGHT · ROUTINE · DEMANDING` +
>    `estimatedMinutes`), and `C16` (#37, milestones). `C11a` (#16) also measured what the
>    free model can do against a fixed format — a ten-stage plan is ten estimations in one
>    shot, which is a direct load on it.
> 3. **One live edge, and it is posted rather than taken.** Anything found here bearing on
>    `#31`'s charts is **posted as a comment on `#31`**; nothing a live or released session
>    owns is edited. Flow stays one-way, as `c9c`, `c3`, `c18`, `c14`, `c1`, `c9e` and `c12`
>    all established.
>
> 📥 **`kb-candidates/` listed before the first unit of work, as the folder's existence
> requires — four files, and each was opened and its *Destination* line read rather than
> inherited from the notes below.** Three target `rules/`
> ([`c1`](kb-candidates/2026-08-10-c1-points-and-time.md) and
> [`c9e`](kb-candidates/2026-08-10-c9e-event-lifecycle.md) both amend
> `rules/question-axis-naming.md` and should be read together;
> [`c16`](kb-candidates/2026-08-10-c16-milestone-model.md) targets
> `rules/agent-topology-and-model-routing.md` §5), and the fourth,
> [`c9f`](kb-candidates/2026-08-09-c9f-consent-screen-state.md), names `kb/dev/` but is
> **parked by Ido's own call** pending a `rules/` proposal. So **all four are always-ask in
> both modes and none is this session's** — `AUTO MODE` drains nothing here.
>
> Recorded by `c8-ai-task-plans` on claiming.

> ✅ **`c9e-event-lifecycle` has released — [#28 · `C9e`](https://github.com/idomarhaim/Android_Final_Project/issues/28)
> is resolved and closed, and with it **the calendar half of the map is complete**:
> `C9a` #25, `C9b` #26, `C9c` #27, `C9d` #17 and `C9e` #28 are all closed.**
> Both coupling points named on claiming were discharged as written:
> 1. **The `#12` commons.** Body **re-fetched immediately before the write** and compared
>    byte-for-byte (`cmp`) against the copy the line was built on — unchanged, no race —
>    then written and verified: **144 → 147 lines, 16 → 17 decision lines, 0 removed**, the
>    only non-inserted difference being a trailing blank line GitHub appends. `C13` (#32)'s
>    index gap left alone; still Ido's to assign.
> 2. **The one live edge into `#19` was posted, not taken.** `C1`'s bulk re-scoring pass is
>    a bulk write into Ido's real calendar; `C9e` gave it a home (one batch → one entry in
>    `C9b`'s daily review → one batch-scoped undo) and said so on
>    [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19#issuecomment-5245582791),
>    plus hand-offs to [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5245583067)
>    and [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5245583361).
>    Nothing any live or released session owns was edited.
>
> **The resolution overturned the literal shape of Ido's own answer, deliberately and on the
> record.** He answered round 1 from outside the option set — *"the app asks whether to also
> delete / also update in the synced calendars"* — and answered round 2 by delegating
> (*"choose the solution that gives the highest standard … and if it can be improved, improve
> it"*). `C9d` had already bought `calendar.app.created` and a **dedicated** calendar, so a
> per-action prompt asks permission to edit the app's own sandbox. It became **immediate
> writes with Undo**, **deletion as cancellation** (Google's trash, 30 days), every
> destructive effect **split by tense** (future cancels, past stays), and **one prompt, once
> ever**, beside the scope grant: *Keep it automatic* / *Ask me each time* — his answer kept
> as a permanent switch rather than as the default.
>
> 📥 **Two KB candidates filed, neither drained, and both re-based mid-session.**
> `picker-rule-consolidation` drained the four parked picker candidates into
> `rules/question-axis-naming.md` **while this ticket was being resolved**, so both entries
> were rewritten against the committed text rather than shipped as drafted. What survives:
> **Mode 6's test is stated on the question and belongs on the options** (a mechanism fork
> survives a scenario stem), **its batch gate produced a false negative here** (the
> answered/refused split ran across two pickers, so the table routes to *density* when the
> cause was *form*), and **the widening reaches derivation closures in code but not a closed
> sibling decision** — round 1's options were *actions*, and what falsified them was `C9d`'s
> scope ruling on another ticket. Always-ask twice over: destination `rules/`, and the first
> rewrites a claim committed 30 minutes earlier.
>
> **Two corrections by this session after the release above, both on Ido's prompting.**
> 1. **The §3 claim was flagged as unverified at commit, then checked — and it was half
>    wrong.** The 30-day trash is real; Google **does not trash a *this-and-following*
>    delete at all**, which is the exact shape `C5`'s repeat rules reach for. **§3a** added
>    to the resolution (never use that shape; cancel occurrences one at a time), a
>    correction posted to [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5245723024),
>    and **`#12`'s index line amended in place** — re-fetched, `cmp`-verified, 147 → 148
>    lines, 17 → 17 decisions, one line edited and nothing else moved.
> 2. **The KB candidates are now a *partial* drain, not "none".** The two `rules/` entries
>    are still parked; two claims that emerged *after* release landed in
>    `C:\Dev\JARVIS\kb` (`ace7bd9`) — a new page on undo-vs-confirm recoverability and
>    `decision-map-charting` §8. **Ido waived the 🎬 walkthrough** for the parked pair on
>    2026-08-10; they stay parked anyway, because
>    `C:\Dev\JARVIS\sessions\picker-delegation-clause.md` already exists as their vehicle
>    and one session resolves one thing.
>
> Recorded by `c9e-event-lifecycle` on release.

> ✅ **`c9b-calendar-surface` has released — [#26 · `C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26)
> resolved and closed, and the commons coupling it recorded on claiming was discharged
> without incident.**
> 1. **`#12`'s *Decisions so far* is a commons.** The body was re-fetched immediately
>    before the append, the patch proved a **pure insertion** before sending (139 → 141
>    lines, `0` deleted), and every sibling's line verified present afterwards. `C9b`'s
>    line is now written, so **no line is owed by this session** — but note `C13`
>    ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32)) still has none,
>    which is Ido's to assign.
> 2. **The design standard became normative mid-ticket.** Ido promoted *"every screen is
>    designed to a current UI/UX standard, not merely specified"* into `#12`'s **Standing
>    preferences**, carrying three rules his own defect reports bought: **one chip may not
>    carry two axes** · **form and words before iconography** · **a design is not finished
>    until it has been seen in Hebrew**. It binds [`C12` #31](https://github.com/idomarhaim/Android_Final_Project/issues/31),
>    [`C6` #22](https://github.com/idomarhaim/Android_Final_Project/issues/22) and every
>    later screen, not just `C9b`.
> 3. **Three findings handed to the build session, none cosmetic** — every time/date
>    string owes **direction isolation** (bidi renders `09:00–12:00` as `12:00–09:00`, a
>    property of the text so it recurs in Compose); **`GoalCategory.defaultColorHex` is a
>    light-mode-only palette** that goes muddy on `#0C1520`, so a dark tone is owed per
>    category; and **no Hebrew literal may reach an English render**.
> 4. **`kb-candidates/2026-08-10-c9b-calendar-surface.md` is drained and deleted** — all
>    three entries ingested into `C:\Dev\JARVIS\kb` (`fe00296`), `Check-KbLinks` CLEAN at
>    61 pages. **Five candidate files remain and none is this session's** — see the note
>    below.
| `c12-charts-presentation` | `/wayfinder 12` → resolve [#31 · `C12`](https://github.com/idomarhaim/Android_Final_Project/issues/31) (the chart set, whether the user picks, and how the dashboard is arranged). Ticket claimed by assignee on GitHub | `CHANGELOG/2026-08-10/c12-charts-presentation.md` *(new)*, `docs/prototypes/2026-08-10-charts-presentation/` *(new)*, `kb-candidates/2026-08-10-c12-charts-presentation.md` *(new, if anything is flagged)*, GitHub issues **#31** and **#12** | **Ido's attention** — a prototype ticket, and two live grillings are already asking for him; see §3 below. No build, no device, no Firebase: the prototype is standalone HTML, as `C9b`'s was | 2026-08-10 |
> ✅ **`picker-rule-consolidation` claimed and released here 2026-08-10 — a cross-repo
> visitor from `C:\Dev\JARVIS` (`/kickoff picker-rule-consolidation`), in and out in two
> commits, `d805616` (claim) and `d9616b9` (drain).** It consolidated the **four**
> always-ask picker amendments parked in this repo against
> `C:\Dev\JARVIS\rules\question-axis-naming.md` into one amendment of that rule, then came
> back only to drain the files that held them. It touched **no ticket, no `#12`, no code and
> no other candidate file**, and held no singleton.
>
> 📌 **What it did to your candidate files, so no session is surprised by a
> deletion it did not make.** All four sessions that wrote them (`c9c-calendar-sync`,
> `c14-challenge-scoring`, `c3-points-currency`, `c18-subtask-depth`) have **released**.
> Each file's only remaining entry was ⛔ always-ask with destination `rules/` — which
> `/kb-ingest` may not take in **either** mode, so they could only ever move through a
> JARVIS session, and that is what happened. All four are now **fully resolved**, so each
> file is deleted rather than rewritten, with the resolution recorded in
> `C:\Dev\JARVIS\CHANGELOG\2026-08-10\picker-rule-consolidation.md` and in
> `rules/question-axis-naming.md` itself. Deletion is normally always-ask; here it is Ido's
> own written instruction in `sessions/picker-rule-consolidation.md` (*"rewrite each drained
> candidate file down to its survivors, or delete it if fully drained"*), invoked by him.
> **`c18-subtask-depth`'s entry was not in that brief** — it was found by listing this
> folder, flagged to Ido as a deviation, and kept.

> ⚠️ **An eighth session joined the same map — `c12-charts-presentation` on
> [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) — and it is the
> first prototype ticket taken since the design standard became normative.**
> `/wayfinder 12` was invoked **bare**, so the pick was the agent's. Frontier
> **re-derived out of the dependencies API at session start**, not read off the
> Unclaimed-work block: every open child of `#12` queried for `blocked_by`. Result —
> **25 children, 16 closed, 9 open**; frontier (open · unblocked · unassigned) =
> **`#31 · C12` and `#21 · C5`**, and nothing else. `#19` and `#28` are unblocked but
> assigned and live; the other five open children (`#20`, `#22`, `#24`, `#30`, `#35`) are
> **all still blocked behind `#19` alone**. Seventh derivation of the day, and the frontier
> has **shrunk from three to two** since `c9e-event-lifecycle`'s — it took one of them.
> **`#31` was taken and `#21` was left, and the grounds have swapped ends since yesterday:**
> 1. **The sole ground on which `#31` was declined three times has expired.** Every
>    decline read *"a second concurrent prototype contends for Ido"* — and each named
>    [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) as the first one.
>    `#26` is **closed and released**; there is no live prototype on this board. What
>    remains is that **every** frontier ticket is HITL, which — as `c9e-event-lifecycle`
>    established — discriminates nothing on its own. Between two HITL tickets the
>    discriminator has to be **disjointness of subject**, and there the two separate
>    cleanly.
> 2. **`#31` is the disjoint one.** Both its blockers — `C3` ([#18](https://github.com/idomarhaim/Android_Final_Project/issues/18))
>    and `C7` ([#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)) — are
>    closed **and released**, so its inputs are foreign state to *read*. It has exactly one
>    live edge, `#19`'s **re-scoring pass**: what the user sees when a number they were
>    relying on moves is a fact to read off `#19`'s resolution, not a decision to co-author.
> 3. **`#21 · C5` — declined, and this is the same objection four earlier sessions raised,
>    now pointing at a different live row.** `C5` decides **where recurrence lives**;
>    recurrence produces occurrences; and `#28 · C9e` — **live right now** — is deciding
>    what happens to a synced event **when its task changes**. Moving recurrence onto a new
>    concept between goal and task changes what *"its task"* even denotes. That is a live
>    session's inputs changed mid-flight, which is precisely what the board has refused
>    four times.
> 4. **`#31` is also the only frontier ticket with leverage outside this map.** Issue
>    [#10](https://github.com/idomarhaim/Android_Final_Project/issues/10) (`U5`, the widget
>    pack) is explicitly waiting on it. The Unclaimed-work block below independently says
>    *"take this one first"*; that block was read **after** this derivation, not before, and
>    is recorded here as agreement rather than as the reason.
>
> Three coupling points, all named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is a commons, and the race it names has fired for real
>    once** (`c3-points-currency` records it from both sides). Same discipline, no
>    exceptions: **re-fetch `#12`'s body immediately before appending**, write only this
>    session's line, verify a pure insertion afterwards. `C13`
>    ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s index gap
>    stays Ido's to assign.
> 2. **`C12` arrives with a standard and two hand-offs already binding it, all from
>    released sessions — so they are inputs, never subjects.** `#12`'s **Standing
>    preferences** now carry *"every screen is designed to a current UI/UX standard"* plus
>    the three rules `C9b`'s eight revisions bought (**one chip may not carry two axes** ·
>    **form and words before iconography** · **a design is not finished until it has been
>    seen in Hebrew**). `C9b` also handed this ticket two concrete items: **where the daily
>    review lives**, and that **spans must contribute nothing** to the time-allocation
>    chart. Anything found here that bears on `#19` is **posted there**; nothing a live or
>    released session owns is edited. Flow stays one-way, as `c9c`, `c3`, `c18`, `c14`,
>    `c1` and `c9e` all established.
> 3. **The singleton on this row is Ido himself, and the board cannot enforce it.** Two
>    live grillings (`#19`, `#28`) are already asking for his attention and this adds a
>    **prototype**, the heavy kind — `#26` spent eight revisions of it. Named here rather
>    than discovered later: revisions ship **one at a time** and stop the moment he stops
>    answering, and no revision waits on the other two sessions.
>
> 📥 **`kb-candidates/` re-listed at session start, as the folder's existence requires —
> six files, one fewer than `c9e-event-lifecycle` saw, because `c9b-calendar-surface`
> drained and deleted its own on release.** Each of the six was opened and its
> **Destination** line read rather than inherited from the note: **five target `rules/`**
> ([`c3`](kb-candidates/2026-08-10-c3-points-currency.md),
> [`c18`](kb-candidates/2026-08-10-c18-subtask-depth.md) and
> [`c14`](kb-candidates/2026-08-10-c14-challenge-scoring.md) are one accumulating amendment
> to `rules/question-axis-naming.md` and should be read together;
> [`c16`](kb-candidates/2026-08-10-c16-milestone-model.md) targets
> `rules/agent-topology-and-model-routing.md` §5;
> [`c9c`](kb-candidates/2026-08-10-c9c-calendar-sync.md) the ❓ Ambiguity picker guidance),
> and the sixth, [`c9f`](kb-candidates/2026-08-09-c9f-consent-screen-state.md), names
> `kb/dev/` but is **parked by Ido's own call at the last drain** pending a `rules/`
> proposal. So **all six are always-ask in both modes and none is this session's** —
> `AUTO MODE` drains nothing here.
>
> Recorded by `c12-charts-presentation` on claiming.

> ⚠️ **A seventh session joined the same map — `c9e-event-lifecycle` on
> [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) — and it is the
> first claim of the day taken on a reason that *expired while the session was reading
> the board*.** `/wayfinder 12` was invoked **bare**, so the pick was the agent's.
> Frontier **re-derived out of the dependencies API at session start**, not read off the
> Unclaimed-work block: every open child of `#12` queried for `blocked_by`. Result —
> **25 children, 16 closed, 9 open**; frontier (open · unblocked · unassigned) =
> **`#21 · C5`, `#28 · C9e`, `#31 · C12`**, with `#19` unblocked but assigned. Everything
> else is still blocked **behind `#19` alone**. Sixth derivation of the day; the
> membership has now changed under it, so **re-derive it yourself.**
> **`#28` was taken, and the two reasons that governed the last four sessions no longer
> hold the same way:**
> 1. **The sole ground on which `#28` was declined four times has expired — 101 seconds
>    before this claim.** [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26)
>    **closed at `19:23:52Z`** and its `C9b` line is already in `#12`'s index. So
>    *"taking it would change a live session's inputs mid-flight"* is now false: there is
>    no flight. `c9b-calendar-surface`'s row above is **not yet released** — ticket
>    closed, index line written, working tree clean — which reads as a session
>    **mid-release**, not mid-work. Its row is left **untouched**: releasing it is that
>    session's move, and a row edited for another session is a report, not a claim.
> 2. **Every calendar predecessor is closed *and* released** — `C9d` #17, `C9a` #25,
>    `C9c` #27, and now `C9b` #26. `#28` is the calendar half's **last open ticket**;
>    closing it finishes a whole subsystem of the map rather than opening one.
> 3. **`#21 · C5` — declined because it sits *nearer* the live `#19` than `C9e` does.**
>    `C5` models a goal with **no target**; what effort and progress arithmetic mean for
>    such a goal is the very half `c1-points-and-time` is deciding right now. `C9e`
>    touches `C1` at exactly **one** named point — the bulk re-scoring pass, which
>    `#28`'s own body already lists — and that is an input to *read* off `#19`'s
>    resolution, not a question to co-decide.
> 4. **`#31 · C12` — declined on the prototype contention, unchanged in force though its
>    sibling changed.** Every ticket on this frontier is HITL, so HITL-ness alone
>    discriminates nothing. A **prototype** is the heavy kind: `#26` just spent **eight
>    revisions** of Ido's attention. Opening a second one while a live grilling (`#19`)
>    is also asking for him contends for the one resource this board cannot serialise.
>
> Two coupling points, both named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is a commons, and the race it names has fired for real
>    once** (`c3-points-currency` records it from both sides). Same discipline, no
>    exceptions: **re-fetch `#12`'s body immediately before appending**, write only this
>    session's line, verify a pure insertion afterwards. **The standing "`#26`'s line is
>    still owed by `c9b-calendar-surface`" note in the banners below is now discharged —
>    that line is written.** `C13` ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s
>    index gap stays Ido's to assign.
> 2. **`C9e` arrives with four rules already inherited and exactly one live edge.** The
>    inherited four are `C9c`'s ([hand-off comment](https://github.com/idomarhaim/Android_Final_Project/issues/28#issuecomment-5243682588)):
>    matching is by `googleEventId` · times cross the sync and state never does · titles
>    are written but never read back · a cancelled event unsyncs and never deletes. Those
>    come from a **released** session, so they are inputs, never subjects. The one live
>    edge is `#19`'s **bulk re-scoring pass** — if it can move times, it is a bulk write
>    into Ido's real calendar. Flow stays one-way, as `c9c`, `c3`, `c18`, `c14` and `c1`
>    all established: anything this session finds that bears on `#19` is **posted there**,
>    and nothing a live or released session owns is edited.
>
> 📥 **`kb-candidates/` re-listed at session start, as the folder's existence requires —
> seven files, agreeing with `c1-points-and-time`'s correction below.** Nothing here is
> drainable by this session: six are always-ask (five target `rules/`, four of *those*
> target `rules/question-axis-naming.md` and should be read together), and the seventh,
> [`2026-08-10-c9b-calendar-surface.md`](kb-candidates/2026-08-10-c9b-calendar-surface.md),
> is ordinary and `AUTO MODE`-eligible but **owned by a row still on the board above**, so
> it drains with that session's release and not with this one's.
>
> Recorded by `c9e-event-lifecycle` on claiming.

> ✅ **`c1-points-and-time` has released — [#19 · `C1`](https://github.com/idomarhaim/Android_Final_Project/issues/19)
> is resolved and closed, and it **unblocked every ticket that was still blocked on this
> map**: `#20`, `#22`, `#24`, and through `#24` both `#30` and `#35`. Nothing on `#12` is
> blocked any more — the whole remaining map is frontier.**
> **The verdict:** `R7`'s line is not human-vs-AI, it is **fact-vs-judgement**. `minutes` is
> a fact about Ido's life and he is its authority; `difficulty` is a judgement and only the
> model makes it; nobody authors their product. So `R8`'s box wins and points recompute
> from a typed duration — and a hand-typed value beats a re-estimation **unconditionally**,
> which answers [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9)'s
> standing question without the threshold it was waiting for, because any threshold makes
> the app judge when Ido is wrong about his own day. What is banked on completion is the
> **inputs, not the number**, which closes a **live** accumulator defect at
> [`TaskRepositoryImpl.kt:120-127`](app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt)
> that `R10`'s own re-scoring pass would otherwise have made routine.
>
> **Ido answered none of the three picker questions on their merits** — he said the first
> was not legible, asked for a schematic explanation, and handed all three back with his
> standing *take the best answer and improve it*. Per the ❓ rule the second attempt must be
> **smaller, not louder**; here it was not re-asked at all, because the instruction was a
> **delegation**, not a request for more words. Every pick is therefore the agent's and is
> on the record in the resolution comment, exactly as `C3`, `C14` and `C17` each recorded.
>
> **Both coupling points below were discharged as written, and the first one *fired for the
> second time on this board*.** `#12`'s body grew **139 → 141 lines between this session's
> session-start fetch and its write** — `c9b-calendar-surface` appended `C9b`'s index line
> in that interval, minutes before. **Re-fetching immediately before the edit is the only
> reason that line survives.** Verified afterwards: 141 → 143, **0 deleted lines**, all
> **16** decision lines present including `C9b`'s, and the written body read back identical
> but for a trailing newline GitHub appends. `c3-points-currency` recorded the first
> instance; this is the second, and the race is now observed rather than feared. The second
> coupling held too — `C3`, `C17` and `C18` were consumed as **inputs**, nothing a released
> session owns was edited, and the two hand-offs went out as **comments** on `#9` and
> [#34](https://github.com/idomarhaim/Android_Final_Project/issues/34).
>
> **What it leaves for the newly-unblocked four:** the model **never emits a point value**.
> `R9`'s shape is `taskId` (membership-checked — `C11a`'s one measured failure mode) +
> `difficulty ∈ LIGHT · ROUTINE · DEMANDING` at ×0.75/×1.0/×1.5 + `estimatedMinutes`, which
> is [`C11b` #30](https://github.com/idomarhaim/Android_Final_Project/issues/30)'s to write
> and [`C8` #24](https://github.com/idomarhaim/Android_Final_Project/issues/24)'s to plan
> against. And `points` moving server-side makes this the **fourth** site of the
> derived-state pattern, sharpening the map's own fog patch on it from *"not sharp until
> `C1` decides"* into a live architecture question with four call sites.
>
> 📥 **`kb-candidates/` re-listed at release, and it changed under this session:
> **two** files, not the seven found at session start.** `c9b-calendar-surface` drained its
> own three-entry file, and the cross-repo visitor `picker-rule-consolidation` consolidated
> and deleted **four** always-ask picker files. This session filed
> [`2026-08-10-c1-points-and-time.md`](kb-candidates/2026-08-10-c1-points-and-time.md) with
> **two entries: one drained, one parked.** Entry 1 — *a clamped running accumulator
> silently destroys history; derive a total by summing timestamped facts* — is an ordinary
> `kb/dev/` claim and was **ingested**. Entry 2 is a **fresh, post-consolidation** instance
> of the picker failure mode (*"I could not understand you — choose for me"* arriving as a
> **delegation**, where the rule's existing guidance says only *make it smaller*), so it
> targets `rules/question-axis-naming.md` and is **always-ask and parked**, three hours
> after that rule was consolidated.
>
> The claim-time record below stands unedited, because the frontier reasoning it holds is
> what the release is answerable to:
>
> ⚠️ **A sixth session joined the same map — `c1-points-and-time` on
> [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) — and it is the
> ticket every remaining blocked ticket on the map is waiting behind.**
> `/wayfinder 12` was invoked **bare**, so the pick was the agent's and the reasoning is
> on the record. Frontier **re-derived out of the dependencies API at session start**, not
> read off the Unclaimed-work block (which has carried a stale count three times today and
> says so): every open child of `#12` queried for `blocked_by`. Result — **25 children, 15
> closed, 10 open**; frontier = **`#19 · C1`, `#21 · C5`, `#28 · C9e`, `#31 · C12`**, with
> `#26` assigned and live. Membership unchanged from `c14-challenge-scoring`'s fourth
> derivation; this is the fifth.
> **`#19` was taken, and the other three were left for the reasons the board already
> records — none of them has expired:**
> 1. **`#19` has no live sibling in its half.** Its two blockers, `#18` (`c3-points-currency`)
>    and `#39` (`c18-subtask-depth`), are both closed **and released**. Nothing live sits in
>    the scoring/structural half at all.
> 2. **It is the leverage.** `#20`, `#22` and `#24` wait on it, and through `#24` so do `#30`
>    and `#35` — **every remaining blocked ticket, with no exceptions.** Leaving it would
>    leave the map's whole blocked half shut for another session.
> 3. **`#28 · C9e` — declined for the fourth time, on unchanged grounds.** It is the calendar
>    half and `c9b-calendar-surface` is live and mid-prototype on `#26` (rev 7 as of this
>    claim). Taking it would change a live session's inputs again, mid-flight.
> 4. **`#31 · C12` — declined on the HITL-prototype contention.** `#26` is already a live
>    prototype needing Ido in the loop; two concurrent prototypes contend for the one
>    resource this board cannot serialise, which is Ido himself. (Its *other* 2026-08-10
>    objection — that `C3` and `C18` were still deciding the numbers it charts — **has
>    expired**, both being closed. The HITL one has not.)
> 5. **`#21 · C5` — declined because recurrence flows *into* the calendar surface.** `C5`
>    decides where recurrence lives; recurrence produces occurrences, and occurrences are
>    what `#26`'s prototype draws. It is `#28`'s coupling wearing a different label.
>
> Two coupling points, both named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is a commons, and the race it names has fired for real
>    once** (`c3-points-currency` records it from both sides). Same discipline, no
>    exceptions: **re-fetch `#12`'s body immediately before appending**, write only this
>    session's line, verify a pure insertion afterwards. **`#26`'s line is still owed by
>    `c9b-calendar-surface`** and is not this session's to write; `C13`
>    ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s index gap
>    stays Ido's to assign.
> 2. **`#19` arrives with more decided than open, and three of its four inputs came from
>    *released* sessions — so they are inputs, never subjects.** `C3` §1 already made
>    `points = round(minutes / 3) × difficulty` — **computed, never authored** — which is
>    most of `R7`; `C18` answered what a point total sums over (**leaves**); `C17` answered
>    how a shared task pays (**pooled, once**) and routed the *bonus* question here as
>    motivation design. If anything this session finds contradicts one of them, it **says so
>    on that ticket** and edits nothing a released session owns. Flow stays one-way, as
>    `c9c`, `c3`, `c18` and `c14` all established.
>
> 📥 **`kb-candidates/` re-listed at session start, as the folder's existence requires —
> and the standing note below is now three files stale in the *other* direction: seven
> files, not four.** The three the note has never counted are
> [`2026-08-10-c16-milestone-model.md`](kb-candidates/2026-08-10-c16-milestone-model.md),
> [`2026-08-10-c18-subtask-depth.md`](kb-candidates/2026-08-10-c18-subtask-depth.md) and
> [`2026-08-10-c14-challenge-scoring.md`](kb-candidates/2026-08-10-c14-challenge-scoring.md).
> **Nothing here is drainable by this session, and the two reasons are different:**
> **six** files are always-ask (five of the six target `rules/`, four of *those* target
> `rules/question-axis-naming.md` — they are one accumulating amendment and should be read
> together, as `c3-points-currency` and `c14-challenge-scoring` both asked); the seventh,
> [`2026-08-10-c9b-calendar-surface.md`](kb-candidates/2026-08-10-c9b-calendar-surface.md),
> holds three **ordinary, `AUTO MODE`-eligible** entries — but it is **owned by a live row
> in the table above**, so it drains with that session's commit and not with this one's.
> Left as a correction here rather than edited into the note below, which another session
> owns.
>
> Recorded by `c1-points-and-time` on claiming.

> ⚠️ **The calendar pair shared the *same half* of [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — and `c9c-calendar-sync` has now released, leaving `c9b-calendar-surface` a live hand-off to read.**
> `c9b-calendar-surface` (#26, the in-app calendar screen) and `c9c-calendar-sync`
> (#27, sync direction and conflicts) ran concurrently. They were **not** disjoint
> in subject the way the previous pair was: both are the calendar, split at
> *surface* vs *semantics*. Both coupling points were named on claiming rather than
> discovered later, and both are discharged from `C9c`'s side:
> 1. **`#12`'s *Decisions so far* is a commons, not territory** — append-only, one
>    line each. `C9c` re-read `#12`'s body immediately before appending, wrote
>    **only its own line** (verified a pure insertion, 0 deleted lines), and touched
>    nothing of `c9b-calendar-surface`'s. **`#26`'s line is still owed by that
>    session**, and is not another session's to write.
> 2. **A two-way sync gives the surface foreign state to draw.** `C9c` settled that
>    Google-side edits **come back**, and that a *move-out* of the GoalPilot calendar
>    is **indistinguishable from a delete** — so if `#26`'s prototype assumes the app
>    is the only author of an occurrence, that assumption is now false. Handed over
>    as a [comment on #26](https://github.com/idomarhaim/Android_Final_Project/issues/26#issuecomment-5243678445),
>    never by editing anything `c9b-calendar-surface` owns. What the surface now
>    owes: a *moved* occurrence, a *disappeared* one (planned but no longer on the
>    calendar), a **third batch** in the daily review (Keep / Cancel / Put back), and
>    **silent and `PROVISIONAL` blocks in the same week** — differing by *visibility*,
>    not by confidence.
>
> First para recorded by `c9b-calendar-surface` on claiming; widened by
> `c9c-calendar-sync` on claiming, after `c17-many-to-many` released; updated by
> `c9c-calendar-sync` on release.

> ✅ **`c3-points-currency` has released — [#18 · `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18)
> resolved and closed, and the commons coupling it named *actually fired*.**
> `C3` was the scoring knot (points vs goal progress); no file, ticket or prototype was
> shared with any sibling. Both things it named on claiming are discharged:
> 1. **`#12`'s *Decisions so far* is a commons, and this is the run that proves the
>    discipline earns its keep.** `c9c-calendar-sync` appended its `C9c` line **in the
>    interval between `C3`'s first read of the map body and its write.** Re-fetching the
>    body immediately before the edit is the only reason that line survives — a blind
>    write would have deleted another session's resolution. Verified afterwards as a pure
>    insertion: body grew 78 → 80 lines, nothing removed. **`#26`'s line is still owed by
>    `c9b-calendar-surface`**, and is not another session's to write.
> 2. **`C9a`'s hand-off is answered, and it is now foreign state for the calendar half to
>    read.** Which occurrence states move goal progress: **completed** moves it and pays
>    once · **`OVERDUE` stays in the denominator**, which makes `C9a`'s *"late is not
>    failed"* true arithmetically rather than only in wording · **`MISSED` and `EXPIRED`
>    leave it entirely**. The flow stayed one-way as promised — `C3` posted to
>    [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21),
>    [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23) and
>    [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) and edited
>    nothing any other session owns.
>
> **One always-ask KB candidate parked, not drained:**
> [`kb-candidates/2026-08-10-c3-points-currency.md`](kb-candidates/2026-08-10-c3-points-currency.md)
> — an amendment to `rules/question-axis-naming.md`, so `/kb-ingest` may not take it in
> **either** mode. It is **adjacent to `c9c-calendar-sync`'s parked entry §3** against the
> same rule file, and says so: **ingest the two together or neither.**
>
> Recorded by `c3-points-currency` on claiming; rewritten by it on release.

> ✅ **`c18-subtask-depth` has released — [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39)
> is closed, and it **unblocked [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19)**,
> the highest-leverage ticket left on the map.** Both coupling points below were
> discharged as written: the `#12` index line was appended after a **re-read
> immediately before the write** and verified a pure insertion (12 → 13 decision
> lines, +1 line, nothing deleted), and the `C3`/`C18` boundary held — `C3`'s
> resolution answered three of `#39`'s five bullets outright, so this session
> **re-decided none of them** and said so in its own resolution rather than
> producing a second opinion. Nothing `c3-points-currency` owns was edited.
> **One thing it did *not* do, deliberately:** `C13` ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s
> missing index line is still missing — an index line written *for* another
> session is a report, not a claim, and it stays Ido's to assign.
>
> The claim-time record below stands unedited, because the reasoning it holds is
> what the release is answerable to:
>
> ⚠️ **A fourth session joined the same map — `c18-subtask-depth` on
> [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) — and the
> choice of *which* frontier ticket was the agent's, so the reasoning is on the record.**
> `/wayfinder 12` was invoked **bare**, which is the mode where the skill assigns the
> pick to the session rather than to Ido. The frontier held **two** takeable tickets,
> not one — **#39** and **#28 · `C9e`** (newly unblocked by `C9c` closing). **#39 was
> taken and #28 was deliberately left**, on disjointness against the live rows above:
> 1. **`#28` is the calendar half, and `c9b-calendar-surface` is live and mid-prototype
>    there.** The board already records the calendar split (*surface* vs *semantics*) as
>    the **less** disjoint pair; `C9e` decides what happens to a synced event when its
>    task changes, which is more foreign state `#26`'s prototype would have to draw —
>    so taking it would change a live session's inputs a second time, mid-flight.
>    **`#28` stays on the frontier, unassigned and takeable** — see the Unclaimed-work
>    block below, which already carries its hand-off.
> 2. **`#39` sits in the structural half, whose two immediate predecessors are closed
>    *and released*** — `C16` ([#37](https://github.com/idomarhaim/Android_Final_Project/issues/37))
>    and `C17` ([#38](https://github.com/idomarhaim/Android_Final_Project/issues/38)).
>    No session is live there.
>
> Two coupling points, both named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is now a four-party commons.** Same discipline as
>    above: append-only, one line each, re-read immediately before appending, write
>    **only** your own line. `#26`'s line is still owed by `c9b-calendar-surface`; `C13`
>    ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))'s index gap
>    is still Ido's to assign and is **not** this session's to fill.
> 2. **`C3` and `C18` touch the same numbers, and the boundary is statable — so it is
>    stated here rather than discovered in two contradicting resolutions.** `#39`'s
>    *Points* and *`progressContribution`* bullets read like `C3`'s subject. They are
>    not the same question: **`C3` owns which currencies exist and how they relate**
>    (one or two), **`C18` owns whether a parent holds its own number or only the sum of
>    the work below it** — the arithmetic of *depth*, which `C16` §4 and `C17` already
>    assigned away from themselves. The map's own wiring agrees: `#18` and `#39` are
>    **parallel siblings** with no edge between them, both blocking
>    [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19). Flow is
>    one-way, as `c9c` and `c3` both established — **this session posts to `#18` rather
>    than editing anything `c3-points-currency` owns**, and defers to `C3` on any
>    question that turns out to be *which* currency rather than *how it aggregates*.
>
> Recorded by `c18-subtask-depth` on claiming; release banner added by it on release.

> ✅ **`c14-challenge-scoring` has released — [#23 · `C14`](https://github.com/idomarhaim/Android_Final_Project/issues/23)
> is resolved and closed, and it *removed* a subsystem from the map rather than specifying one.**
> All three coupling points below were discharged as written: the `#12` index line was
> appended after a **re-fetch immediately before the write** and verified byte-identical
> afterwards (13 → 14 decision lines, net **+3 / −1**, the one deletion being this
> session's own graduated fog patch); the `C1` trust decision was **posted to
> [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19), not taken there**,
> because `#19` was unclaimed and still blocked at the time of writing; and `C3`'s answer
> was consumed as an input, with nothing `c3-points-currency` owns edited.
> **What it leaves for whoever takes `#19`:** the verdict *client writes the fact, a
> Function owns the derived number* — and the fact that [`firestore.rules:53`](firestore.rules)
> already says in its own comment that `publicProfiles.points` carries the identical
> caveat, so the two are one defect written down twice.
> **One KB candidate is parked, always-ask** — a **fifth** picker failure mode for
> `rules/question-axis-naming.md`: within one four-question picker Ido answered the
> **scenario** question fluently and could not answer the three **mechanism** questions.
> It is a neighbour of `c9c-calendar-sync`'s parked granularity entry and should be read
> with it. Two further candidates were ingestable and are drained separately.
>
> The claim-time record below stands unedited, because the frontier reasoning it holds is
> what the release is answerable to:
>
> ⚠️ **A fifth session joined the same map — `c14-challenge-scoring` on
> [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23) — and it is the
> first one whose subject lives in a *different subsystem* than every live sibling.**
> `C14` is challenge scoring: `ChallengeParticipant.score`, `ChallengeType`, Health
> Connect, and `firestore.rules`. Nothing there is touched by the calendar half
> (`#26`), by depth arithmetic (`#39`), or by the currency question (`#18`, released).
> The pick was **the agent's** — `/wayfinder 12` was invoked bare — so the reasoning
> is on the record, and it is the *narrowest* frontier justification so far, because
> for the first time the frontier held **four** takeable tickets, not two:
> **#21 · `C5`**, **#23 · `C14`**, **#28 · `C9e`** and **#31 · `C12`**. Re-derived out
> of the dependencies API, and it agrees with the Unclaimed-work block below.
> Three were left, each for a *different* reason, and none of them is "it looked harder":
> 1. **`#28 · C9e` — declined for the third time, on the same grounds the board already
>    records.** It is the calendar half and `c9b-calendar-surface` is live and
>    mid-prototype on `#26`. Taking it would change a live session's inputs a second
>    time, mid-flight.
> 2. **`#31 · C12` — declined on *two* live couplings, not one.** It is a **prototype**
>    (HITL) ticket, and `#26` is already a live prototype needing Ido in the loop; two
>    concurrent HITL prototypes contend for the one resource this board cannot
>    serialise, which is Ido himself. And its own body says it must chart *"whatever
>    numbers `C3` and `C7` leave the app with"* — but a goal's ring is a **roll-up**,
>    and `c18-subtask-depth` is deciding what that sums over **right now**.
> 3. **`#21 · C5` — declined because recurrence flows *into* the calendar surface.**
>    `C5` decides where recurrence lives; `C7` §3 already routed the *"recurring activity
>    whose repetition is what gets counted"* shape to it. Recurrence produces occurrences,
>    and occurrences are what `#26`'s prototype draws — so it is `#28`'s coupling wearing
>    a different label.
>
> Three coupling points, all named on claiming rather than discovered later:
> 1. **`#12`'s *Decisions so far* is a commons, and the race it names has now actually
>    fired once** (`c3-points-currency` records it above). Same discipline, no exceptions:
>    **re-fetch `#12`'s body immediately before appending**, write only this session's
>    line, verify a pure insertion afterwards. `#26`'s line is still owed by
>    `c9b-calendar-surface`, and `C13`'s index gap is still Ido's to assign.
> 2. **`C14` and `C1` ([#19](https://github.com/idomarhaim/Android_Final_Project/issues/19))
>    share one trust problem, and `#19` is blocked behind `#39` — which is live.** `#23`'s
>    own body says points and challenge scores should decide client-reported vs
>    server-computed *together*. This session therefore decides it **for challenge
>    scores** and **posts the shared finding to `#19`** rather than pre-empting a ticket
>    nobody has claimed and that a live session is still upstream of. Flow one-way, as
>    `c9c`, `c3` and `c18` all established.
> 3. **`C3`'s answer is an *input* here, not a subject.** `C3` settled that `points` is a
>    view of effort and not a currency, and posted that to `#23` before releasing. This
>    session consumes that comment; if anything it finds contradicts `C3`, it says so on
>    `#18` and does not edit a released session's artifacts.
>
> 📥 **`kb-candidates/` re-listed at session start, as the folder's existence requires** —
> **four** files now, not the three the note below counts. The fourth is
> [`2026-08-10-c3-points-currency.md`](kb-candidates/2026-08-10-c3-points-currency.md),
> which arrived with the release above and is **parked always-ask** (an amendment to
> `rules/question-axis-naming.md`, ingestable in neither mode). So the standing
> always-ask set is **four**, all four wait on Ido, and **nothing in this folder is
> drainable by this session**. Left as a correction here rather than edited into the
> note below, which another session owns.
>
> Recorded by `c14-challenge-scoring` on claiming; release banner added by it on release.

> 📥 **`kb-candidates/` holds nothing a session can drain — re-listed 2026-08-10 by
> `c9b-calendar-surface`, and the previous note was *four files stale*.**
> `2026-08-10-c9b-calendar-surface.md` was **drained in full and deleted** (3 entries →
> `C:\Dev\JARVIS\kb`, commit `fe00296`, `Check-KbLinks` CLEAN at 61 pages). **Five files
> remain — `c9f`, `c14`, `c16`, `c18`, `c3`, `c9c` — and every surviving entry in every
> one of them is `always-ask`, destination `rules/`.** `/kb-ingest` may not take those in
> **either** mode, so none of them is waiting on a session: they wait on Ido and on
> `/walkthrough`. Checked entry by entry rather than assumed from the filenames.

> **Issue-tracker partition — settled, and now visible in the tracker itself.**
> Both 2026-08-06 sessions have released, and neither filed from the other's list.
> `product-device-pass` owns **[#2](https://github.com/idomarhaim/Android_Final_Project/issues/2)–[#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)**
> (reproduced defects and `U1`–`U6`); `product-model-map` owns
> **[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)–[#32](https://github.com/idomarhaim/Android_Final_Project/issues/32)**
> (the `wayfinder:*` map and its 20 decision tickets). A future session adding to
> either half should read the map first — `#12` is now the source of truth for the
> product model, and `TODO/TODO_FUTURE/ProductModel.TODO.future.md` is not.
>
> ✅ **The 14th decision landed.** `D1` → `C14` → **[#23](https://github.com/idomarhaim/Android_Final_Project/issues/23)**,
> blocked on `C7`, with the handoff block's enumeration and anti-cheat coupling
> folded in. The liftable block under `D1` in `TODO_OPTIONAL/` has been used and
> is now historical.

## 📏 Rules

0. **This file existing is the trigger.** Read it before your first edit — not
   "if someone else might be here". Whether they are is what this file tells you,
   so skipping it means you have no evidence you were allowed to skip it.
1. **Claim before writing.** Add your row, commit it, then work. If a row was
   written *for* you by another session from files it saw change, that is a
   report, not a claim — confirm and correct its path list before you continue.
2. **Never write outside your paths.** If you need a path another session owns,
   say so and let the user re-assign — do not "just quickly" edit it.
3. **Never blanket-stage** — `git add -A`, `git add .`, `git add --all`,
   `git commit -a`. Not "while another session is live": you cannot know that
   until you have read this board, and by then you have already staged. Explicit
   paths always; it costs nothing on the days you are alone.
4. **Singletons are exclusive.** Builds and device work serialise. Claim, use,
   release — and check the table below before your first **build or device
   command**, not only before your first edit.
5. **Release when done** — clear your row on `/handoff`, on finish, or when
   abandoning. A stale claim blocks work nobody is doing.
6. **The agent recommends, the user assigns.** A session that sees unclaimed work
   fitting its context emits one line —
   `🧭 **Claim:** <candidate> → owns <paths>; conflicts: <none|paths>` — and waits.
   It never self-assigns.

### Singletons in this repo

| Singleton | Why it matters here |
|---|---|
| Gradle daemon / `.gradle` locks | Two `gradlew` runs contend; one blocks or dies mid-write. **This, not the emulator, is what actually serialises two sessions** — see below |
| The git index | Never `git add -A` — stage explicit paths |
| Emulator `Pixel_10_Pro_XL` (`adb`) | One screen, one driver. Installing/driving the app is exclusive |
| Emulator `Pixel_10_Pro_XL_B` (`adb`) | Second device, added 2026-08-05 for the two-account demo. Same rule, claimed separately: `run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B` |
| Firebase project `goalpilot-56e30` | Live Firestore/Storage/Functions — concurrent writes are attributable to nobody |

**Two emulators do not buy two parallel verifications.** Both AVDs exist so that
*one* session can drive two signed-in accounts at once (the spec §7 sharing demo),
not so that two sessions can each run `:app:connectedDebugAndroidTest`. Those two
runs would still queue at the Gradle daemon above, and worse, each would build an
APK from the *other* session's uncommitted edits — one working tree, one
`app/build/`. Real parallel instrumented testing needs a second checkout, which
this repo has deliberately not adopted.

## 🗂️ Unclaimed work

Where to look, in order: [`TODO/TODO.md`](TODO/TODO.md) (MUST → OPTIONAL →
FUTURE), then open issues. `/claim` reads both and proposes a fit.

Currently unclaimed and ready:
- ~~**Two written briefs, one session each** — `/kickoff product-device-pass` and
  `/kickoff product-model-map`~~ — **both done, 2026-08-07 and 2026-08-08.** They
  ran concurrently, stayed disjoint, and partitioned the tracker by content
  without colliding. What they produced is the unclaimed work below.
- **The wayfinder map's frontier — four tickets takeable, one session each.**
  [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) is the map;
  **never resolve more than one ticket per session**, and claim by assigning yourself the
  issue before any work.
  **Re-derived out of GitHub 2026-08-10 by `c12-charts-presentation` after `C1`
  ([#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)) and `C9e`
  ([#28](https://github.com/idomarhaim/Android_Final_Project/issues/28)) both closed** —
  `blocked_by` walked per open child through the dependencies API, not read off this board.
  **18 of the map's 25 children are now resolved**, and the block below is rewritten rather
  than amended because `C1` closing changed almost every line of it: **the frontier doubled
  in one step.** One ticket is in flight — `C12`
  [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31), assigned and
  mid-prototype — and **only two remain blocked**: `#30 · C11b` (behind `#20` and `#24`) and
  `#35 · C15b` (behind `#24`).
  **Ido assigned `#24` as the next session on 2026-08-10.** Takeable now, in his order:
  - [#24 · `C8` AI-proposed numbered task plans for a goal](https://github.com/idomarhaim/Android_Final_Project/issues/24)
    — **Ido's pick for the next session.** Newly unblocked by `C1`. It is the **only**
    remaining ticket with downstream leverage: closing it unblocks `#35` outright and, with
    `#20`, `#30` as well.
  - [#22 · `C6` what may the user edit in LOG PROGRESS](https://github.com/idomarhaim/Android_Final_Project/issues/22)
    — newly unblocked by `C1`. A **screen** ticket, so it inherits `#12`'s design standard and
    the three findings `C12` restated: bidi isolation on every time and date string ·
    `GoalCategory` is light-mode-only · no Hebrew literal in an English render.
  - [#20 · `C2` AI-assigned task type](https://github.com/idomarhaim/Android_Final_Project/issues/20)
    — newly unblocked by `C1`. Half of `#30`'s remaining blockade.
  - [#21 · `C5` endless and maintenance goals](https://github.com/idomarhaim/Android_Final_Project/issues/21)
    — takeable since `C3` closed, and **declined twice today for reasons that have now both
    expired**: it fed the then-live `#26` and `#28`, and both are closed. It is what the map's
    *"per-life-area success and failure, visualised"* fog hangs on alone, and `C9a` supplied
    its vocabulary — `MISSED` / `OVERDUE` / `EXPIRED` are three different things and
    conflating them would draw a picture that **overstates** Ido's failures.

  > ⚠️ **Read [`Product and UX Reviews/2026-08-09-entity-model-brief.md`](Product%20and%20UX%20Reviews/2026-08-09-entity-model-brief.md) before taking either.**
  > It is a **second source document**, written after the map was charted, and its
  > routing table says which ticket each `E1`–`E19` item bears on. `C4` was charted
  > without it and built a whole question picker on the wrong axis before Ido stopped
  > the session. The map body records it, but the tickets themselves predate it.

  **Two standing lessons, carried forward** (written by earlier sessions; still true, and
  restored here rather than dropped when this block was rewritten). First, **closing a
  root can leave the map more blocked, not less** — `C4` unblocked exactly one of the four
  tickets it held, and `#18`/`#24` gained *new* blockers from its own resolution. Second,
  **the reverse also happens** — `C9a` unblocked two at once and opened the whole calendar
  half. Neither is predictable from the map body. The count on this block has been stale
  or wrong on most of the days it has been touched, **so re-derive the frontier out of
  GitHub rather than trusting any list, including this one.**

  **There is no AFK ticket left** — `C9f` was the last one, and every frontier ticket is
  HITL. They are not disjoint from one another in practice: they are all Ido's attention,
  which is the scarcest singleton here and the one the board cannot enforce.

  **One index gap, still open:** `C13` ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32))
  is closed with a full resolution comment but has **no line in `#12`'s *Decisions so far*
  index**, so it is invisible to anyone reading the map at low resolution. An index line
  written *for* another session is a report, not a claim — Ido's to assign.

  **No brief file was written for either, and that is deliberate**: on this map the ticket
  *is* the brief and `/wayfinder 12` is the entry point, so a `sessions/<slug>.md` would be
  an uncommitted-to-the-map duplicate that rots against the issue it copies. Decision taken
  per the derivable-decision rule; the 🔀 Form-B fallback is for work with no committed
  home, which this is not.

- **One written brief, its own session: `/kickoff fix-task-completion-feedback`** —
  written by `product-device-pass` for issue [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)
  (the ~2 s completion lag and its silent-offline twin). Ordinary build work, needs
  the emulator and the Gradle daemon — so it **does** contend with any device
  session, unlike anything on the map.
- ~~**Two-account demo + spec title page**~~ — **effectively closed 2026-08-06.**
  The sharing demo was done on 05/08 (`CHANGELOG/2026-08-05/submission.md`,
  rules deployed and a non-owner join proven), and the title page turns out to be
  filled in already: `GoalPilot_spec_EN.docx` is modified in the working tree and
  reads `Submitted by: Ido · [Ido Mar-Chaim 209497072] · [10208]`. **Confirmed
  and ticked 2026-08-06** — the template's square brackets survive around both
  values and Ido ruled them cosmetic. **Every MUST item is now closed; nothing
  blocks submission.** The docx stays *Frozen / off-limits* in `AGENTS.md`, was
  not touched by any agent, and is Ido's to commit.
- **Health Connect on a physical phone** — small follow-up to the shipped feature,
  and **bigger since 2026-08-05**. The emulator carries the provider but its store
  is empty, so the reading → Firestore write path has never run against real step
  data — and that path now runs unattended, with no review sheet to catch it. The
  top-up arithmetic (today logged at 09:00, walked, opened again at 18:00) has only
  ever been exercised against fakes; a phone is the only place it can be watched.
- ~~One written brief, its own session: `/kickoff challenges-ui`~~ — **done
  2026-08-05.** All three briefs are now in `sessions/done/`; there is no
  unworked brief left. What challenges left behind is folded into the two-account
  item above: **deploy `firestore.rules`**, then verify a *non-owner* join
  against the live backend. Both want the second account, so both belong to that
  sitting rather than to a session of their own.
- ~~One `time-insights` verification is still open~~ — **done 2026-08-04.** Both
  blocked checks ran once the AVD came free: `:app:connectedDebugAndroidTest` 20/20
  green, and a live re-estimation run that returned 105 minutes for a five-word
  title, which the client heuristic (ceiling 60 for five words) and the Cloud
  Function's flat 30 both cannot produce. One of eight candidates matched a
  fallback and was unticked automatically. See
  `CHANGELOG/2026-08-04/time-insights.md` → "Verified against the live model".

**Disjointness**, checked 2026-08-04 and left here so it need not be re-derived:
- The three briefs' **paths** were disjoint — `feature/challenges/`,
  `feature/lifeareas/` + `feature/goals/`, and `feature/analytics/` +
  `functions/src/index.ts`. Two ran concurrently and never collided.
- Their **verification was not**, and that is the part that actually bit. All three
  touch composables, so all three want `:app:connectedDebugAndroidTest`, and the
  emulator is one exclusive singleton — `time-insights` released with its
  instrumented layer unrun because `lifearea-polish` held the AVD. **Disjoint paths
  do not make sessions independent; the device does.** When two sessions both end
  at a composable, expect one of them to hand its device check to the other.
- The **Gradle daemon** turned out to be shareable by queueing, not by claiming: a
  build during a sibling's mid-edit fails on *their* half-written file, which reads
  as your own compile error until you look at the path.
- `challenges-ui` stays clear of `functions/src/index.ts` only because standings
  are computed client-side. A later session moving them server-side collides with
  what `time-insights` already landed.

## 📓 Recently released

| Session | Task | Released | Landed in |
|---|---|---|---|
| `c9e-event-lifecycle` | **Planning only — a GitHub resolution, a map edit and Markdown; no code.** `/wayfinder 12` **bare**, so the frontier was re-derived out of the dependencies API (`blocked_by` per open child) rather than read off this board — **25 children, 16 closed, 9 open**, frontier `#21 · C5`, `#28 · C9e`, `#31 · C12`. Took **[#28](https://github.com/idomarhaim/Android_Final_Project/issues/28)** (`C9e`) because the single ground it had been declined on four times — *the calendar half is live* — **expired 101 seconds before the claim**, `#26` having closed at `19:23:52Z`. **The headline is that the ticket's own question was stale:** it asks *"is the event removed, or left as a record of what was planned?"*, and Ido answered from outside the set (*"the app asks whether to also delete / also update in the synced calendars"*) — but `C9d` had already bought `calendar.app.created` and a **dedicated** calendar, so a per-action prompt asks permission to **edit the app's own sandbox**, and a dialog answered yes ten times stops being read by the eleventh. Resolution: **immediate writes with Undo**; **deletion is cancellation** (Google's trash, restorable 30 days), which falsifies the ticket's own *"not recoverable from inside GoalPilot"* premise rather than arguing with it; **every destructive effect splits by tense** — future events cancel, **past events stay** as the record of time actually spent, which answers *removed or kept* as **both**; completion still writes nothing (`C9c`); `BLOCK` → `DEADLINE` is cancel-and-recreate, not a patch. **One prompt survives, once ever**, beside the incremental scope grant: *Keep it automatic* / *Ask me each time* — Ido's original answer kept as a permanent switch rather than as the default, since undo protects on the day you are not reading. `C1`'s 40-block re-scoring pass writes as **one batch** into `C9b`'s daily review with one batch-scoped undo; **orphaned events are surfaced there and never auto-deleted**. **Filed nothing, graduated nothing.** Round 2's picker was refused outright — *"I couldn't understand what the implications of each option are"* — which is `Mode 6` wearing a **scenario stem over a mechanism fork**, and is one of the two always-ask candidates left parked. | 2026-08-10 | `#28` resolved + closed · [resolution](https://github.com/idomarhaim/Android_Final_Project/issues/28#issuecomment-5245577162) · `#12` index line (144 → 147 lines, **0 removed**) · hand-offs on [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19#issuecomment-5245582791), [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5245583067), [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5245583361) · `CHANGELOG/2026-08-10/c9e-event-lifecycle.md` · `kb-candidates/2026-08-10-c9e-event-lifecycle.md` (2 entries, **always-ask**, re-based against `picker-rule-consolidation`'s commit) |
| `c18-subtask-depth` | **Planning only — Markdown and issues, no code.** `/wayfinder 12` **bare**, so the frontier was re-derived out of GitHub (`blocked_by` per open child through the dependencies API) rather than read off this board — three times, and it moved under the session twice. Resolved [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39): **a parent task is a container, never a second worker — every roll-up sums over *leaves***, with depth capped at **10 from an intrinsic goal down to a leaf task** (Ido set 10; the *along which chain* reading is derived and flagged for override). Took **#39 over #28** deliberately, to avoid a third concurrent session in the calendar half where `c9b-calendar-surface` is live and mid-prototype. Found that **three of the ticket's five bullets were already answered** by `C3`/`C16`/`C17` and re-decided none of them. **Unblocked [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19)** — every remaining blocked ticket on the map now waits behind that one. | 2026-08-10 | [#39 resolution comment](https://github.com/idomarhaim/Android_Final_Project/issues/39#issuecomment-5244179322) · `#12` index line + migration-fog narrowing (verified pure insertion, 12 → 13 decision lines) · `CHANGELOG/2026-08-10/c18-subtask-depth.md` |
| `c3-points-currency` | **Planning only — Markdown and issues, no code.** `/wayfinder 12` **bare**, so the frontier was re-derived out of GitHub (`blocked_by` queried per open child through the dependencies API) and this session picked on **leverage**: resolved **[#18](https://github.com/idomarhaim/Android_Final_Project/issues/18)** (`C3`), which gated five tickets directly and nine transitively. **Ido could not read either picker and handed the choice back** with his standing *take the best answer and improve it* — and the reduction that followed found the real headline: **the ticket's own fork was already false.** It says *"the only bridge is `progressContribution`"*, but [`TaskEstimate.kt:40`](app/src/main/java/com/idomarhaim/goalpilot/domain/model/TaskEstimate.kt#L40) already asserts `minutes = points × 3`, so on every offline task — a **first-class** spec §8 path — the app invents a **reward** from a **word count** and derives **how long your life took** from it, putting `C17`'s time-allocation chart downstream of a gamification currency. Answer: **two quantities, effort and outcome, and `points` is not one of them** — points are a *view of effort*, `round(minutes / 3) × difficulty`, which **inverts the constant already in the code** rather than adding one and keeps today's anchor exactly (30 min routine = 10 pts). `R12`'s book is `C16` §4 clause 2 and the missing weight is **`minutes`**, the only candidate **conserved under splitting** — the very test `C16` used to kill count-weighting, which **points also fail**. Progress becomes **`(current − start) / (target − start)`**, closing `C7`'s hole with **no `DIRECTION` enum** (the missing field was an *origin*). `progressContribution`'s `1.0` is diagnosed as **a silence, not a value** — `C7`'s `unit = "%"` disease. Overshoot legal and shown, and **three clamps, not two** — [`GoalDetailViewModel.kt:275`](app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailViewModel.kt#L275) found here. `C9a`'s hand-off answered: `OVERDUE` stays in the denominator, `MISSED`/`EXPIRED` leave it, and **recurring work cannot sit in an outcome denominator at all** (unbounded occurrences → `done/total` never converges) → `C5`. And **half of `R12` was a layout fact**: [`SummaryUseCase.kt:41-42`](app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SummaryUseCase.kt#L41-L42) **publishes** the contradiction into the shared §7 summary, so points may never render as a property of an objective. **Filed nothing.** The commons race the board warned about **actually fired** — `c9c-calendar-sync` appended to `#12` between this session's read and its write, and only the re-fetch-before-append discipline saved that line. | 2026-08-10 | `7f127e6` (claim) + this commit · [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) closed · [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21), [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23), [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) unblocked and commented · [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) map · `CHANGELOG/2026-08-10/c3-points-currency.md` · one always-ask KB candidate parked |
| `c9c-calendar-sync` | **Planning only — Markdown and issues, no code.** `/wayfinder 12` **bare**, so the frontier was re-derived out of GitHub (`blocked_by` queried per open child) rather than read off this board — which was just as well, since the block it would have trusted offered two assigned tickets as takeable. Resolved **[#27](https://github.com/idomarhaim/Android_Final_Project/issues/27)** (`C9c`): **Google holds the *when*; GoalPilot holds *what happened*** — forced, not chosen, because a Google event has no field for `MISSED`/`OVERDUE`/`EXPIRED`/`PROVISIONAL`, so every state-carrying design ends in an encoding nothing respects and the user can delete by typing. Ido chose **two-way**, which **costs no extra scope** (`calendar.app.created` already reads back what it wrote), so the read-scope trade was a separate question all along. **The conflict fork did not exist:** #27 asked what *"ask"* looks like *"on a phone the user is not holding"*, and `GoogleAuthUtil` mints only short-lived tokens with **no refresh token** while `C9d` banned the service account — **there is no credential for a background sync and cannot be one** — so the pull runs on foreground and last-write-wins is *correct* rather than a compromise. **The finding: a move-out is indistinguishable from a delete.** Seeing only its own calendar, the app reads a dragged-away event exactly as a deleted one, and the two obvious auto-behaviours are destructive in opposite directions — so **a disappearance never deletes and never re-creates**, and the ambiguity is *asked* in `C9a`'s daily-review batch. **Ido overturned the second picker onto a better axis — per calendar, not per app** — and the fact that does not go away is that **Google cannot enforce that split**, so it is a promise the client keeps; recorded rather than buried, because a control the user thinks the provider enforces is worse than none. Answered with **incremental authorization**, so **if he never uses Full the promise is never made**, and the restraint is visible in *which call is made*. That satisfies `C9a` §4's precondition **without changing its wording** — and the confirmation sheet deliberately survives, so **the agent gets quieter in proportion to what Ido chose to show it**. **A `DEADLINE` was chosen rather than asked** (he asked for the schematic version, the best answer, and an improvement): an **all-day banner**, decided on the single criterion that **the Google event does not remind**; the timed-event shape was ruled out *before* the question reached him, since it would occupy a slot the app cannot check and collapse two rungs `C9a` separated. **The improvement composes two of the three answers** — the banner may be paired with a real `BLOCK` in a genuinely free slot, which the read scope made possible and neither answer produces alone. **Filed nothing** (third resolution on this map to manage it); commented **#26, #28, #31**; **unblocked #28**; **narrowed one fog patch** (the deprecated-`GoogleSignIn` one, whose *shape* changed — incremental auth makes the scope request a recurring user-driven interaction, not `C9d`'s one-off). **No suite run and none applicable.** Verification was structural: the map body hashed and byte-compared **immediately before each of two writes**, the first proven a pure insertion (0 deleted lines) and the second an anchor-asserted in-place replacement of exactly one line, both **read back and diffed** (one trailing newline from GitHub, **BOM intact**), and every comment post confirmed by **re-reading its count** rather than trusting exit status. **No singleton taken at all.** **The pre-commit self-review caught three of this session's own errors** — a changelog claiming *"no fog patch narrowed"* while the resolution argued the opposite, a miscounted reuse of the import idiom in the #26 hand-off, and #31 named as owed a hand-off with no comment posted. **Two hazards recorded rather than smoothed over.** First, **this session created the orphan `c17-many-to-many` reported**: the wayfinder skill says claim the ticket by assigning it *before any work*, the board says write the row *before your first write*, and `SESSIONS.md` was lease-blocked for the ~20 minutes between — so a sibling correctly observed an assigned ticket that no board row held, and proposed unassigning it. Nothing was lost, but the gap is real and belongs to the ordering, not to either session. Second, **a sibling committed to `SESSIONS.md` while this session held its lease** (`7f127e6`, granted to `c9c` at 17:35Z) — caught only because the editor refused a stale write, which is the mechanism working one layer down from the lease | 2026-08-10 | `d058fa8` (claim) + this commit · [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) closed · [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) unblocked · [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) map · see `CHANGELOG/2026-08-10/c9c-calendar-sync.md` |
| `c17-many-to-many` | **Planning only — Markdown and issues, no code.** `/wayfinder 12` **bare**, so the frontier was re-derived out of GitHub and this session picked on leverage: resolved **[#38](https://github.com/idomarhaim/Android_Final_Project/issues/38)** (`C17`), the last blocker on `C3` [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18), behind which nine tickets sat. **Divide what is drawn from one pool; duplicate what each destination owns.** Minutes and points are pooled — a shared task's time is **divided** across its edges and its points are **paid once**; each goal's progress and each area's success count are **owned** — **every edge counts in full**. *"Does it advance both goals?"* (yes) and *"does it pay points twice?"* (no) read as one question and have opposite answers, which is why the ticket produced nothing until its five sub-questions were separated. **The task→goal edge becomes a record, not an id** (`goalEdges: [{goalId, contribution}]`), forced by `C7`; `parentIds` is plural for `C16`'s reason. **One of the ticket's premises was false and collapsing it left one question instead of five** — the pie and the per-area detail view do not need one answer, since the detail screen shows the whole 40-minute run under every option. **Ido handed that one question back** (*"choose the highest-quality answer and improve it"*), so the pick is on the record as the agent's: **divide**, because it is the only option an autonomous agent **cannot inflate** — `C4` §9 permits silent instrumental edges, so *credit-both* would make the central chart reward **re-filing over doing**. The ticket's headline objection (*the total exceeds the time that passed*) is **nearly worthless** and was not the reason: the chart counts only completed tasks with fallback durations and was never an audit. Three improvements: the chart **discloses that it divided** (the same move `estimatedTaskCount` already makes for a guessed number), the division **never leaves the pie**, and the integer remainder is **distributed**, or the implementation breaks the invariant that chose the design. **Filed nothing**; commented **#18, #19, #21, #31**; **unblocked #18**. Also **reported, not acted on:** [#27](https://github.com/idomarhaim/Android_Final_Project/issues/27) is assigned but **no session holds it** — no row, no brief, no changelog under the `c9c` label in any day folder; unassigning is Ido's. Ran concurrently with `c9b-calendar-surface` and honoured the shared-`#12` protocol: body re-fetched and byte-compared (41 000 unchanged) immediately before appending one line. | 2026-08-10 | `2c3b273` + `856b314` (`AGENTS.md` v15→v16, mechanical) · KB: `6e25586` in `C:\Dev\JARVIS` |
| `c9a-schedule-a-task` | **Planning only — Markdown and issues, no code.** `/wayfinder 12` **bare**, so the frontier was re-derived out of GitHub rather than taken from this board and Ido picked: resolved **[#25](https://github.com/idomarhaim/Android_Final_Project/issues/25)** (`C9a`), the head of the calendar chain. **A schedule is not a property of a task; it is a set of *occurrences*, and the task carries only the rule that generates them.** `R17` reads as one feature and is **six** decisions, none of which is the one that matters most — *how many independent whens one piece of work may have, and what remembers the outcome of each*. A date **on** `Task` gives one, so `R18`'s flowers become **26 duplicate documents a year** and a miss has nowhere to live (`isDone` latches); a **rule alone** cannot hold a moved instance, a skip or a Google event id, which is `R17`'s own closing clause. What only the combination buys is the *question* **"this occurrence, or all future ones?"** — a field-only model always answers *just this one*, a rule-only model always *all of them*. **Four rungs** (`ALL_DAY · DEADLINE · BLOCK · SPAN`) discriminated by **what a miss means, not by precision** — and *this session recommended three and one of its two objections to the fourth was simply wrong*: the 480-minute ceiling governs `estimatedMinutes`, i.e. **effort**, so it never touches a span's elapsed dates. **That error is what surfaced a defect risk nobody had looked at** — the time-allocation chart sums `estimatedMinutes`, so one week-long renovation would swamp every life area (→ **#31**). **Flat, not nested**, leaving containment to `C16`/`C18`; `C16` closed *mid-session* and **agreed** (one edge on the child, repeated at depth), so an addendum converted that forward reference into a settled link. **Who schedules is decided by what the app cannot see:** `calendar.app.created` leaves it blind to every other calendar, so no-slot rungs are set silently (`C4` §9 permits it) and a **`BLOCK` needs confirmation**. Two of #25's three candidate deciders were **unavailable, not rejected** (`C1` #19 and `C2` #20 both blocked), and a confidence threshold buys nothing since `C4` found `confidence` is written and never read. **Ido asked three times for it plainer *and* improved**, which produced most of the substance: confirmation per **plan** not per block, **reusing the shipped Google Tasks import dialog** rather than inventing a second idiom; agent-placed blocks written **`PROVISIONAL`**, dashed, **not synced until confirmed**; and an unconfirmed block **`EXPIRED`s counting for nothing**, without which an over-eager agent **manufactures failures** — *you cannot fail to do something you never agreed to*. **Temporal state is derived, never stored**, following the shipped `Challenge.phaseAt(now)`; the affordable `onSchedule` sweep was rejected because **its only real advantage evaporated on inspection** — the request asked for *reminders* (before), not miss-alerts (after), and a before-reminder needs no stored status. **`OVERDUE` split from `MISSED`** so being late is not filed as failing, and it is the one state that **keeps** reminding. **The coupling is the finding:** not storing state is what lets a reminder re-check *"is this still open?"* at fire time, for free. **Ido's own addition:** a nightly plan-tomorrow notification — which, checked rather than assumed, needs **no server job**, because there is no `WorkManager`, `AlarmManager` or FCM at all. **Filed nothing** — the second resolution on this map where every hand-off landed on an existing ticket; **seven comments** (#18, #21, #26, #27, #28, #31, #8), all verified as landed by re-reading counts, because `c7` recorded a silent posting failure. **Cleared the notification-substrate fog** (both questions answered; the build half widens **#8** to *scheduled*, not only immediate, notifications) and narrowed three more patches. **Unblocked #26 and #27 — the calendar half of the map is now open** — re-derived out of GitHub after closing, not predicted; **25 children, 10 closed**. **No suite run and none applicable**; verification was structural — map body hashed `672b45e1…` before the first edit and **byte-compared immediately before writing** (no drift, four siblings live), then **read back and diffed against what was sent** (one trailing newline added by GitHub, BOM intact, no textual diff), and the five body edits applied by a script **asserting every anchor** so a missed replacement failed loudly. **No singleton taken at all**; live `goalpilot-56e30` never contacted. **Row widened mid-session before the first write to any of the seven comment targets**, and the board was **re-read at that point and had changed on disk** — `c10-quote-feed`'s row had gone. **Overtaken by four siblings** (`c7`, `c10`, `c16`, `c13`): every blocked-state claim in the resolution was re-verified afterwards and all held, and **`#37` turned out to be a live claim, not a pointer** — the session asked instead of guessing and Ido sent it to `#25`, which is the only reason two sessions did not collide on one issue body. **One gap raised, deliberately not fixed:** `C13` (**#32**) is closed with a resolution and has **no line in the map's *Decisions so far* index**, so it is invisible to the next session reading the map at low resolution — not written by this session, because an index line written *for* another session is a report, not a claim. **5 KB candidates written, and then all 5 drained in one pass** on Ido choosing *"ingest all five now"* — four **new** central pages (`dev/rule-plus-occurrences.md`, `dev/derive-dont-stamp.md`, `dev/blindness-not-confidence.md`, `dev/confirm-the-plan-not-the-item.md`) plus `dev/enum-and-label.md` **§4 in place**; `Check-KbLinks` **CLEAN at 52 pages**, nothing superseded, and a row held on the **JARVIS board** for the same unit since the board follows the repo being written to. **One entry's destination was corrected by reading rather than trusting:** entry 5 proposed folding into `dev/review-intake-and-triage.md`, whose concern turned out to be sorting a *human's* freehand review by ceremony rather than the confirmation boundary for a batch of *machine* proposals — so its bundle check was **present, recent and still wrong**, a first for that bundle. **And the deletion decision was reversed mid-flight by a rule that landed while the ingest was being written:** the drained candidate file was first kept and marked, because deletions were always-ask; `8c021b0` from the still-live `c13-byo-api-key` session then added the one carve-out — a **fully** drained `kb-candidates/` file is deleted without asking — so it was `git rm`'d. Keeping it was correct when decided and wrong twenty minutes later, with nothing in this session's own reasoning having changed. **Two paths beyond the widened row, both named rather than quietly taken:** `CHANGELOG/CHANGELOG_README.md` (the per-session index every sibling appends to), and the *Unclaimed work* frontier block in this file — which was **five sessions stale**, listing `#37`, `#32` and `#25` as takeable and `#14`/`#29` as in flight when all five had closed. Refreshed because no session held it and it actively misdirected; the claim-provenance rule was still honoured where it bites, so `C13`'s **missing map-index line was raised, not written** | 2026-08-10 | see `CHANGELOG/2026-08-10/c9a-schedule-a-task.md` |
| `c13-byo-api-key` | **Planning only — a GitHub resolution, a map edit and Markdown; no code.** `/kickoff` on the committed brief, resolving **[#32](https://github.com/idomarhaim/Android_Final_Project/issues/32)** (`C13`), and the headline is that **the key buys a different *credential*, not a different *pipeline*** — one choice answering four of the ticket's five sub-questions. It is **[`C10`](https://github.com/idomarhaim/Android_Final_Project/issues/29)'s deciding argument inverted**: there the *degraded* path threatened to become a second mechanism, here the *enhanced* one does — a client calling a provider directly needs a second copy of every prompt and every `C11b` schema, and the copy that drifts runs only when a key is present, which for an audience of one is exactly when nobody is watching. So the key rides **to the existing Cloud Function**, per call, held nowhere, and the client gains **no outbound path to any model provider** — spec §5's property is kept, not spent. **The mechanism-count argument beat a security argument, not a convenience one**, which is what makes it worth keeping: device-direct genuinely keeps the secret off Google's servers and lost anyway. Stored **on the device, encrypted** beside the skin and `C15`'s language; **Firestore was cheaper, needed no rules change** (`users/{uid}/{document=**}` is already owner-only, checked at `firestore.rules:14-19`) **and was rejected anyway**, because a third-party secret at rest in a backed-up, exportable database is a different posture. One new client dependency, `androidx.security:security-crypto`, which this project does **not** have — grepped, not recalled. **Ido overturned two of this session's recommendations:** **four named adapters and nothing else** (GROQ, OpenAI, Anthropic, Gemini) rather than "any OpenAI-compatible base URL" — so a fifth provider costs a Functions deploy and **no untested wire format can ever run** — and a failing key **speaks once at the point of use** rather than only in Settings. **Both were improved on rather than merely implemented:** `401/403` speaks and the latch clears on key-edit-or-success while `429`/`5xx` stay silent, **plus a permanent status line for every class**, because a one-time message alone leaves a dead key with no standing indicator three weeks later — the same recovery-masks-failure trap in the opposite costume. **What it hands [`C11b` #30](https://github.com/idomarhaim/Android_Final_Project/issues/30) is the sharpest part, and it is two questions turning out to be one:** with four providers a model swap is the **normal case**, so `C11a`'s footnote — strict schema *"buys a guarantee that survives a model swap, not reliability"* — **becomes a requirement**; and mandating native enforcement costs **nothing** under a four-adapter list, excluding exactly the custom-URL slot that was already declined. **And the app-side validation stays**, because the two catch disjoint classes: enforcement catches structural errors, while `C11a`'s one measured failure in 248 calls was an id of the right type and plausible length that **was not in the list sent with it** — structurally perfect, semantically wrong, **invisible to any schema at any provider or model size**. Also settled: model id **free text with a per-provider default** (a curated list rots exactly as `AGENTS.md` records a GROQ id rotting, now ×4), the key **test-called on entry** so a typo never lands on the motivation feed, **no stored artifact gains a field**, and — checked in the code rather than assumed — **no new screen**: the key's card sits on `ProfileScreen` beside `AppearanceCard`, the third per-device setting this map has put on a surface that already exists. **Quality only, never behaviour**, derived from the map's own Notes and *Out of scope* and logged rather than asked. **The method finding is the third instance of one pattern and it revises the earlier diagnosis.** Ido answered three of four follow-ups with *"I could not understand you — choose the best answer yourself"*, **despite this session applying the fix the previous two produced** — every question named its axis and the axes dropped, three of four carried previews. The discriminator is **which** question he answered: the four turning on things only he knows, and none of the three turning on schema-enforcement mechanisms, latch reset rules and switch granularity. *"I don't understand the options"* was him **correctly reporting a question that was never his to answer** — the class the derivable-decision rule already says to derive and log. **`c16-milestone-model` hit the same symptom the same day from a different cause** (none of its three options was right), so the picker now has three failure modes on record — framing, coverage, ownership — and "reduce the axis" addresses one. **Recorded rather than papered over:** this session **announced the resolution and then ended the turn without writing it**, caught only because Ido asked whether the session was finished; and **three of the seven decisions were taken by the session, not by Ido**, on his explicit instruction — named plainly because this is a `wayfinder:grilling` ticket. **Mode conflict resolved by rule:** the brief says `normal` and argues `AUTO MODE` is wrong here; Ido opened with `AUTO MODE`; `/kickoff` §4 gives this session's message precedence, and the two never collided — `AUTO MODE` governs committing, pushing and ingest, never the authority to answer product questions. **One defect filed as a spec line, not fixed:** `callGroqJson` throws the provider's **raw error body** and every callable `logger.error`s it into Cloud Logging — with a *user's* key that is a third-party error body in Google's logs. **Filed nothing, unblocked nothing** — `#32` blocks no ticket, enumerated across every open issue rather than assumed. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file created or modified. Verification was structural: map body **hashed three times** (`761f3267…6aed1b6`), the edit proven a **pure insertion** with **zero deleted lines**, the written body **read back and compared**, **both comment post-counts checked** (the failure `c7-what-is-a-unit` recorded, where `gh issue comment` posted nothing and reported no error), and the frontier re-derived at **25 children / 9 closed** — takeable `#25` (claimed), `#38`, `#39`. **No singleton taken at all** — no `#gradle-daemon`, neither AVD, no GROQ call, live `goalpilot-56e30` never contacted. **Lease-blocked at its own release and waited rather than asking**, per §5.2: `kb-ingest-c10` then `c16-milestone-model` held `SESSIONS.md` and `#git-index`, and a background watcher on the lock file cost two turns instead of a question. **That wait paid for itself** — it turned a reported contradiction into information: `kb-candidates/2026-08-10-c10-quote-feed.md` looked like it disagreed with its own index row, and was simply being read **mid-drain**. Worth naming for the next session: **`kb-ingest-c10` held no row on this board**, so a live session was visible only through its leases. **4 KB candidates written; 3 ingested, 1 parked** — entry 1 is always-ask twice over (`rules/`-shaped, and it revises an entry already parked awaiting Ido) | 2026-08-10 | `7862691` (claim) + this commit · [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) closed · [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30) commented (**not** unblocked) · [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) map · see `CHANGELOG/2026-08-10/c13-byo-api-key.md` |
| `c16-milestone-model` | **Planning only — Markdown and issues, no code.** `/wayfinder 12` arrived **bare**, so the frontier was re-derived live out of GitHub — open, unblocked, unassigned children of #12 — and #37 was taken on **leverage rather than issue order**: sole blocker of #38 and #39, and via #38 the gate on #18 and the eight tickets behind it, **11 in all**, against 3 for #25 and 0 for #32. Resolved **[#37](https://github.com/idomarhaim/Android_Final_Project/issues/37)** (`C16`): **a milestone is a goal nobody wants for itself** — one collection, the instrumental edge stored **on the child** and repeated at every depth, and an `intrinsic` marker carrying **`declaredBy`** rather than a boolean, which is the only shape where `C4` §9's *must ask before asserting an intrinsic edge* has a **witness in the data**. **The finding is that this session's own question was the bug.** Branch 4 was put to Ido as *"target 6/10 or children 1-of-3 — which number wins?"*, three options with a diagram each, and he could not answer it — **correctly, because none of the three was right**: `Task.progressContribution` already means the work below *advances* the number, so the two were never rival measurements. Reframing produced a **fourth answer better than all three offered** — one number per objective, the work below is its mechanism — which independently kills *show-both* (it defers, and the deferral resurfaces at every ancestor) and *children-win* (splitting one task into three drops progress 33%→25%, **punishing the user for planning properly**). Collapsing them also exposed the **plan-coverage gap** — *"everything you planned adds up to 3 of 10"* — which is **subtraction, not inference**, so the free-model rule costs nothing; Ido kept it inside `C16`. Also rejected, and recorded because it is the elegant one: a single `nodes` collection for goals, milestones **and** tasks — it merges two field sets that are never both valid, rewrites every DTO/query/screen over live data, and **erases in storage the `E7`/`E12` line the brief draws in prose**. `E19`'s deliberate ambiguity settled **permissively** (a task attaches at any level, forced by `E8`'s *may*). **Filed nothing.** Two fog patches narrowed, and the migration one changed **character**: `C16` adds no collection and no entity, so its whole share is two **additive** fields and a half-finished migration still leaves a readable database. **Session hygiene, learned the hard way:** two opening reports were **wrong** — the "orphaned" ingest had been committed at `7aedf9f` mid-read, and #29 was **live**, not stale; its board row simply pointed at `CHANGELOG/2026-08-09/…` while the session wrote `CHANGELOG/2026-08-10/…`. Ido's *"can't you just check?"* is what produced both corrections. Five sessions ran concurrently in this tree; the only shared artefact, map body #12, was **leased** (`#gh-issue-12`), held across one edit, released — and the commit below waited on a `kb-ingest-c10` lease via a background watcher rather than a question | 2026-08-10 | `bef501b` (claim), `fc70c47` (resolution artefacts) · [#37](https://github.com/idomarhaim/Android_Final_Project/issues/37) closed · [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) map · [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) + [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) unblocked, [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) one blocker cleared · `CHANGELOG/2026-08-10/c16-milestone-model.md` · `kb-candidates/2026-08-10-c16-milestone-model.md` **(2 entries, un-ingested — normal mode)** |
| `kb-ingest-c10` | **Ingest only, Markdown only; the pages landed in another repo.** Bare `/kb-ingest`; drained this repo’s `kb-candidates/2026-08-10-c10-quote-feed.md` **4 of 6** into the central bundle `C:\Dev\JARVIS\kb` — three new pages (`dev/split-at-the-inviolable-constraint.md`, `dev/degraded-mode-decides.md`, `dev/confirmation-vs-correctness.md`) and two **folds in place** (`llm-structured-output.md` §2.1, `localization-axes.md` §5). **2 held back and neither dropped** — entry 4 corroborates a **parked** entry in `kb-candidates/2026-08-09-entity-model-intake.md`, so draining it alone would split one finding across two states; entry 6b would install a standing pre-commit review step, a behaviour change and therefore `rules/`-shaped. The candidate file was **rewritten down to the survivors, never deleted**, with original numbers and dated reasons under `## Standing — always-ask`. **`kb-candidates/` listed first, as the session-start duty requires:** besides its own file it holds `2026-08-09-c9f-consent-screen-state.md` and `2026-08-09-entity-model-intake.md`, each already partially drained and down to **one parked entry** — so this repo has **no backlog of un-offered knowledge left**, only two decisions Ido has not made. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; the only mechanical layer that applies is the bundle linter, which runs in the other repo (`Check-KbLinks` **CLEAN at 47 pages**). **No singleton taken** — no `#gradle-daemon`, neither AVD, live `goalpilot-56e30` never contacted, no GROQ call. Three sibling sessions were live throughout (`c9a-schedule-a-task` #25, `c13-byo-api-key` #32, `c16-milestone-model` #37), none owning a candidate file or the bundle. A row was claimed on the **JARVIS** board too, because a cross-repo ingest owes one in every repo it writes to; leases held in both through the commit | 2026-08-10 | this commit; see `CHANGELOG/2026-08-10/kb-ingest-c10.md` |
| `c10-quote-feed` | **Planning only — a GitHub resolution, a map edit and Markdown; no code.** `/wayfinder 12 29`: resolved **[#29](https://github.com/idomarhaim/Android_Final_Project/issues/29)** (`C10`), and the headline is that **`R21` was two sentences wearing one hat**. It asks for a daily line that is *practical and inspiring* **and** *drawn from a bestselling book or a famous figure* — two obligations with two sources, and one mechanism serving both produces either platitudes or a fabricated attribution. **The seam went at attribution:** only a **curated corpus shipped in the APK** may name a real human; the model writes the practical line and **names nobody**. Selection reuses `C7`'s own rule unchanged — *the AI judges, the app computes* — with the model returning **one word** from a closed theme list and the app resolving `category ∧ theme → hash(today + edgeId) → quote`, so **a quote id never reaches the model** and `C11a`'s silent-truncation failure is not caught but **unreachable**. **The argument that decided it was the fallback, not the safety:** with no network the app derives the theme itself and the rest of the pipeline is *identical*, where the rejected shortlist-and-pick design degrades into a **different mechanism** — a second implementation, exercised only when nobody is watching. **`C7` and `C4` both closed mid-session and both bit.** `C7`'s optional measure looked like a problem and exposed **a live defect** instead: an unmeasured goal is sent as `progressPercent: 0` and reads to the model as *"you have done nothing"* — fixed by the **theme** axis, which keys on days idle, open work and age, so an unmeasured goal is **well-aimed, not degraded**. `C4` made the sentence attach to an **intrinsic edge**, so `E19`'s Goal 2 gets **one** sentence not two, a milestone gets **none of its own** and shows its goal's, and the feed stays bounded under `C18`'s unbounded depth. **`R22` answered yes as a socket:** `Task` has no ordering field, so the line names the *earliest unfinished* task today and `C8` fills the same slot later — with **the app choosing the task and the model only phrasing it**, which makes naming a nonexistent task impossible. **Ido twice answered a question picker with *"I could not understand you"***, and the fix that worked both times was **reducing the axis to a 2×2 of who does which job**, stated *before* the picker — a method finding filed as a KB candidate that **corroborates a parked entry from `entity-model-intake`** rather than duplicating it. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file created or modified. Verification was structural: `blockedBy` re-checked live before claiming; the **map body hashed three times**, including immediately before the write, all three matching (`831c71f2…f0330`); and the frontier re-derived after closing — 25 children, 7 closed, and **`#30` and `#35` are still blocked**, so closing this freed nothing downstream. **One error caught and fixed in place:** the resolution comment was posted dated 2026-08-09 and edited to 2026-08-10 rather than left standing. **Two defects filed as spec lines, not fixed** — the `progressPercent: 0` above, and `Recommendation` having **no `author` and no `source` field**, so an attributed quote has nowhere to live. **Filed no new tickets** (corpus authoring is implementation, not a decision) and **narrowed the `A7` fog**, which this ticket partly answered. **No singleton taken at all** — no `#gradle-daemon`, neither AVD, no GROQ call, live `goalpilot-56e30` never contacted. **Recorded, not papered over:** this session's board row was **committed by a sibling** (`bef501b`, `c16-milestone-model`) before it could commit its own — the same commons-lease hazard two earlier sessions recorded. Two other sessions were live throughout (`c9a-schedule-a-task` #25, `c16-milestone-model` #37) with **no path overlap**; only this session's row was staged. **4 KB candidates written, none ingested** — normal mode; candidate 4 is always-ask by inheritance. `kb-candidates/` was listed before the first unit of work and **changed underneath the session** — the three-file backlog reported at start was drained by `kb-ingest-backlog-drain` mid-session, leaving two partially-drained files each down to one parked always-ask entry. **A second pass followed, on Ido asking what could be improved — and it found three of the resolution’s own answers wrong, none of which seven grilling questions and an explicit final confirmation had caught.** **A Hebrew translation of a public-domain work is *not* public domain**, so tier 1 is rebuilt **Hebrew-first from natively-Hebrew sources** and lapsed editions verified *per edition, not per author*; the task named is the **smallest `estimatedMinutes`, not the earliest created**, because the resolution argued for *“the one thing you could do now”* and then specced the oldest — on a stale goal, the task that has been avoided; and [`C2` #20](https://github.com/idomarhaim/Android_Final_Project/issues/20) **already names *“selects the tone of the daily line (`C10`)”*** among its purposes, which this session never read — the axes turn out orthogonal (theme from *state*, task type from *content*) and that reconciliation is now on the record so `C2` does not re-decide it. Six spec lines added, the largest being that the feed as first specced is **a daily list of the goals you are failing at**, fixed by reserving one of the 2–3 slots for a goal in a good state — plus **`Goal.deadlineEpochMillis`, which existed and was used for nothing**. Posted as a [correction comment](https://github.com/idomarhaim/Android_Final_Project/issues/29#issuecomment-5242005728) and folded into the map gist; the map was re-fetched and hashed again before that second write, and a sibling had not touched it. | 2026-08-10 | `ef903da` + this commit; see `CHANGELOG/2026-08-10/c10-quote-feed.md` |
| `kb-ingest-backlog-drain` | **Ingest only, Markdown only** — no Kotlin, Gradle, rules or Functions file touched, no issue written, no ticket resolved, **no singleton taken**, live `goalpilot-56e30` never contacted. Bare `/kb-ingest`, and the opening folder sweep found **five un-ingested candidate files, 21 entries**, with — for the first time in this repo — **every owning session already released**, so none was a live session's to drain and all five were takeable at once. Ido chose a **per-file drain, oldest first**: five passes, one commit per pass in each repo, so an interruption costs one pass rather than the sitting. **Result: 19 ingested (18 central, 1 project-local), 2 parked.** **Nothing was superseded in either bundle** — every entry was additive. **The one supersede warning was resolved by checking rather than inherited:** `c9f`#4 carried *"⚠️ check before ingesting"* on the chance a KB page held the *"production hard-blocks sensitive scopes"* claim; grepping the whole bundle found **no page carries it** — it only ever lived in this repo's docs, which `c9f-consent-screen-state` had already corrected on 08-09 — so the predicted **three** always-ask entries were really **two**. **Reconciliation was the actual work, and it is the finding worth keeping.** Three of `fix-task-completion-feedback`'s five entries proposed **new** central pages that **already existed**, written two days earlier from `product-device-pass` — the session that *reproduced* the same defect — and ingested 08-08. Entry 5 had hedged it explicitly (*"check first — may already have been ingested"*) and was right, **down to the letterboxing trap it offered as new**; that hedge is the only reason this drain did not create a duplicate page, and it is worth copying into future candidates. **Five new central pages** — `recovery-masks-failure` (the better the recovery path, the weaker the signal: a policy-level fault inside correct error handling that recovers into a state indistinguishable from a first run has no observer, which is why the shipped Tasks import re-consented **weekly** and nobody could have filed it), `google-oauth-scopes-and-consent` (scopes are researchable and consent is not; **production-unverified does not block sensitive scopes**; the 7-day clock is on the **grant**, not the token; granular consent arrives **unchecked**; the grant lives on the **account**, so `pm clear` and uninstall prove nothing), `optimistic-ui-patterns` (retire an overlay against **observed data**, never the write's completion — two channels, no ordering guarantee; failures invert it), `edges-not-types` (a discriminator can live on the **relationship**, and an object-property proxy for it **inverted on both** of Ido's unprompted examples; a role stored as an **edge** makes promotion one write where a `kind` enum makes it a migration over live data; and the relationship property is **absent from the input**, so no model size closes it), and `render-site-vs-query-site` (a query proves reachability, only a render site proves **visibility** — three independently true facts pointed at "the unfiled-task inbox is free" while the dashboard counts tasks and lists none). **Five updated in place**, of which **`decision-map-charting.md` absorbed four entries from three different candidate files** — a task-vs-research ticket type, routing a new source that arrives mid-map, the reader's duty to list the **source folder** and the **recent commit subjects**, and why closing a root can leave a map **more** blocked. **Two entries parked as always-ask, and neither dropped:** `c9f`#1 (an untested claim written as fact propagates by copying) and `entity-model-intake`#1 (every picker option sharing one axis). Both files were **rewritten down to their survivor** — original numbers kept, `Status` stamped with reason and date, moved under `## Standing — always-ask` — rather than deleted, because deleting on a partial drain discards exactly what the always-ask exclusion protects. **Both `rules/` drafts are written** to their canonical JARVIS home (`rules/claim-provenance.md`, `rules/question-axis-naming.md`), deliberately **uncommitted and unsynced** pending `/walkthrough`; the second names the exact one-bullet insertion into the ❓ Ambiguity rule **without editing the projected file**, because an uncommitted edit there would surface as a parity failure in an unrelated session. **No suite run and none applicable**; verification was the bundle linter, `Check-KbLinks` **CLEAN after every pass** (38 → 41 pages). **Recorded rather than silent:** `c7-what-is-a-unit` edited its own board row in the working tree *between* this session reading the board and first writing to it — caught only by re-reading per rule 1 — and it has since released; two commits of this file therefore carried that session's uncommitted row, named here rather than left to be discovered | 2026-08-10 | this commit + `bae2fa8`, `de1808d`, `b97e82a`, `33d040b`, `4d75529`, `8c05c95`; central half `7b7a477`, `ef2a1b2`, `9bb38d4`, `b4cf47a`, `a1ebbb1`, `56b2d92` in `C:\Dev\JARVIS`. See `CHANGELOG/2026-08-10/kb-ingest-backlog-drain.md` |
| `c7-what-is-a-unit` | **Planning only — Markdown and issues, no code.** `/wayfinder 12 14`: resolved **[#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)** (`C7`), and only half its framing survived. There **is** an enumerated set, but it enumerates **kinds of measurement** — `COUNT · DURATION · DISTANCE · VOLUME · MASS · MONEY · PERCENT` — beside a **free word** the user reads: the only shape where nothing is unsayable and nothing is unknowable, and the shape `C11a` measured this model to be best at (enums 50/50, free identifiers **silently** corrupted). It lands on `C15`'s boundary for free — the word is user content, the kinds are app-authored and owe Hebrew. **The bigger half: a goal may carry no measure at all, and absence is the default** (`E6`, written a day *after* this ticket was charted, which is what invalidated its premise). So `"%"` survives as a *chosen* bar but stops being what a goal gets for saying nothing — **the disease was never the percent sign**, it was that the lazy path produced a goal that measured nothing while claiming to measure something. **One principle found twice, in two unrelated sub-questions:** the model gets the **categorical** half and the code gets the **arithmetic** half — it answers only *"do fill buttons fit, and what is counted"* while the ladder is computed (`target / 16`, `1× 2× 3× 4×`, which reproduces `R25`'s `[250 ml] [500 ml] [750 ml] [1 L]` exactly), and a measure change converts by **division** where a relationship exists and only *proposes* where none does. **Ido attached a requirement to three of the four answers:** unmeasured is legal but **never silent** (a *concrete* proposal, dismissible per goal, non-AI fallback — and this session's addition, that it may offer a **leading indicator** rather than fake an outcome number for *"understand real estate"*); a measure change offers an **adaptation** of logged history beside a clean reset; and on a shared challenge **every participant must approve**, which has nowhere to live — `firestore.rules` lets only the owner write the challenge doc. **Filed nothing** — the first resolution on this map where every hand-off landed on a ticket that already existed (`#11`, `#23`, `#37`, `#38`, comments only). **Unblocked `#11` and nothing else**, checked rather than claimed: `#23` and `#31` are still blocked by `C3` `#18`. **Named, not specced, on Ido's instruction:** `Task.progressContribution` is one `Double` and cannot be right in two kinds at once (→ `#38`); whether a milestone *shows* a measure (→ `#37`). **One hole found on the way past:** every measure assumes accumulation, so *"lose 5 kg"* is inexpressible — direction handed to `C3` beside `C4`'s clamp. **The live-data migration is free** (`"%"`→`PERCENT`, `"steps"`→`COUNT`), so the map's Firestore-migration fog drops from five dependent tickets to four. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was structural (map body hashed before the first edit and again immediately before writing, `b9d5c8ee…` unchanged both times, since `#12` carries no lease and two siblings were live) plus querying the graph back out of GitHub. **One error caught by verifying:** the first attempt at the four hand-off comments **posted nothing and reported no error** — comment counts were `0/0/0/0` until it was re-run. **No singleton taken at all** — no `#gradle-daemon`, neither AVD, no GROQ call, live `goalpilot-56e30` never contacted. **Recorded:** this row's *claim* was committed by `c4-goal-task-ontology` (`ca35c4c`) rather than by this session, and the row was **widened mid-session** — before the first write to any of them — from `#14 + #12` to include the four comment targets; the ⚠️ note above, written by `kb-ingest-backlog-drain` while this session was live, refers to that widening and is now historical. **5 KB candidates left pending on Ido's call**, in `kb-candidates/2026-08-10-c7-what-is-a-unit.md` — deliberately *outside* `kb-ingest-backlog-drain`'s five-file drain list. **Also pushed, on a conditional authorisation to verify first:** reading the outgoing 12 commits found that `c9f`'s `4-import-succeeded.png` showed Ido's **real task list** on a **public** repo, so the rows were redacted and `093fd98` amended to `3b0340c` before anything was published — 11 of 12 commits were clean, and the email in the other screenshots was checked to be *already* public rather than assumed to be new exposure. The redaction's own backup branch was **deleted**, because no push rule here or in the global six preconditions catches `git push --all`; that gap is entry 5. **Then re-claimed for a bare `/kb-ingest` and released again:** all **5 candidates drained in one pass**, four into `C:\Dev\JARVIS\kb` (**new** `dev/enum-and-label.md`, `dev/absent-by-default.md`, `dev/redaction-leaves-a-second-copy.md`; **§7 in place** on `dev/llm-structured-output.md`) and one kept here as **new** `knowledge/goal-measurement.md`, deliberately project-local. `kb-candidates/2026-08-10-c7-what-is-a-unit.md` `git rm`'d; `Check-KbLinks` **CLEAN** on both bundles (44 pages / 6). Entry 5 stopped at the KB page — moving the *global* push rule is `rules/` and stays Ido's. A row was held on the **JARVIS board** for the same unit, since the board follows the repo being written to | 2026-08-10 | `21144d5` (pushed) + this commit; see `CHANGELOG/2026-08-10/c7-what-is-a-unit.md` |
| `c4-goal-task-ontology` | **Planning only — Markdown and issues, no code.** `/wayfinder 12 13`: resolved **[#13](https://github.com/idomarhaim/Android_Final_Project/issues/13)** (`C4`), the map's root — and **this session's own question picker was overturned before Ido answered it**. Its four discriminators (measured/done · size · endures/completes · let-the-AI-decide) are all properties of the **object**; his, written the day before in the `E1`–`E19` entity brief, is a property of the **relationship** — a goal is what matters to you *in its own right*, a task and a milestone are both *means*. The proxies do not approximate it, they **invert on his own two examples**: unmeasurable *"understand real estate"* is a goal, perfectly measurable *"finish year 1 of the degree"* is explicitly **not** one. **Goals do not nest; milestones join them** — "goal" and "milestone" are **roles carried by edges** (intrinsic from the user, instrumental from another object), so the same object is both at once and promotion either way is **one edge**, not a document migration; store a *type* instead and every promotion bills you as a migration over live data. A second, weaker line separates milestone from task (*a state you reach* vs *work you do*), and **the two lines differ in kind — line 1 is not computable at all**, which turns it into a spec rule: **the app may act silently on instrumental structure, but must ask before asserting an intrinsic edge.** That answers `R4` by removing its premise. **Two things found by checking rather than assuming:** the sorter's goal-invention has **no floor** — `classifyTask` returns `confidence: 0` on total failure with the task title as a goal name, and `confirmSmartAdd` never reads it — and the "free" unfiled-task inbox is **not free**: the dashboard *counts* tasks and *lists* none, so an unfiled task is counted everywhere and reachable nowhere, costing one surface. **This session's own overstatement, corrected in its changelog:** it claimed `#18`/`#21`/`#24`/`#25` would unblock; **exactly one did** (`#25`), and `#18`/`#24` gained new blockers from this very resolution — resolving the root left the map **more** blocked, because the entity brief deepened it. Filed **[#37](https://github.com/idomarhaim/Android_Final_Project/issues/37)** `C16` milestone entity, **[#38](https://github.com/idomarhaim/Android_Final_Project/issues/38)** `C17` many-to-many linkage, **[#39](https://github.com/idomarhaim/Android_Final_Project/issues/39)** `C18` sub-task depth, all wired; folded `E9`'s third-goal-kind invitation into **[#21](https://github.com/idomarhaim/Android_Final_Project/issues/21)** and `E4`'s success/failure view into the map's fog, both deliberately unfiled on Ido's instruction. **No write to [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)** although the brief routes `E3`/`E6`/`E11` there — it is `c7-what-is-a-unit`'s claim (§5 rule 2), and the routing is already in the committed brief. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was structural (graph re-queried after every mutation: 25 children, 4 new edges, frontier re-derived). **No singleton taken at all.** `SESSIONS.md` and `CHANGELOG_README.md` leased and released; the contended map body **#12** was fetched, hashed, edited offline and **byte-compared immediately before writing — no drift**. **5 KB candidates written, none ingested** (normal mode). **`kb-candidates/` was listed before the first unit of work and this session got the listing wrong — Ido caught it.** It reported **two** un-drained files; there are **five**, `2026-08-09-c9f-consent-screen-state.md` and `2026-08-09-entity-model-intake.md` having been missed although both were committed before this session started, and although **this board already said so** in the `entity-model-intake` and `c9f-consent-screen-state` rows below. Corrected everywhere: **21 entries across 5 files** (3 · 5 · 5 · 3 · 5), **every pre-existing file now unowned** — `c9d-calendar-scopes`, `fix-task-completion-feedback`, `c9f-consent-screen-state` and `entity-model-intake` have all released — and **at least 3 entries are always-ask in both modes** (two arguably `rules/`, one may supersede a standing claim). **The ingest is now a sitting of its own, not a tail-end chore.** Recorded rather than buried, because it is the *second* instance of one failure this session: reporting a view of the repo instead of listing it — first missing the 08-09 entity brief, then miscounting this folder | 2026-08-10 | this commit; see `CHANGELOG/2026-08-10/c4-goal-task-ontology.md` |
| `entity-model-intake` | **Intake only — Markdown and one `.docx` move; no code, no issue written, no ticket resolved.** Ido stopped the live `C4` session mid-picker and wrote a new source document; this session turned it into [`Product and UX Reviews/2026-08-09-entity-model-brief.md`](Product%20and%20UX%20Reviews/2026-08-09-entity-model-brief.md), `E1`–`E19`. **The headline is that the document answers `C4` ([#13](https://github.com/idomarhaim/Android_Final_Project/issues/13)) with a discriminator that is none of the four options its question picker offered** — `E7`: a goal matters to you *in its own right*, "not as a means to achieve something else"; `E12`: a task is "not necessarily important to you in life in its own right". The axis is **intrinsic vs instrumental**, a judgement about the user's values; all four picker options were properties of the *object*, so the picker was mis-shaped rather than mis-ranked. It also answers the worked example `#13` explicitly asked for, by introducing a **third entity**: **goals do not nest inside goals — they are joined by milestones** (*אבן דרך*), and the same object can be a milestone of one goal while being a goal in its own right. **Five items create scope on nobody's ticket** and were **flagged, not filed** (wiring tickets into a map is charting and the map is claimed): the milestone entity, many-to-many goal↔life-area and task↔goal linkage, arbitrarily deep sub-tasks, Ido's own unanswered question of whether a *third* kind of goal exists, and per-life-area success/failure visualisation. Grounded against `domain/model/` at named lines rather than recalled: **five of eight schema rows are changes over live data** — `Goal.lifeAreaId` and `Task.goalId` are single and nullable, there is no parent-task field, no milestone entity, and `progressFraction` is clamped `0..1` with `isComplete` latching, so `E11`'s decay cannot happen today. **Wrote to no claimed path** — `#12`, `#13`, `#14` and `#29` were all live claims, so the output is a **routing table** (`E`-id → ticket → kind of bearing) rather than a comment on any issue; §5 rule 2 followed rather than worked around. **Ido's decisions at the close:** the live `C4` session is **fed the brief and made to re-ask** (not killed), and **files the new scope itself** as it resolves `#13` — with `E9`'s third goal kind folded into `C5` ([#21](https://github.com/idomarhaim/Android_Final_Project/issues/21)) and `E4` sent to the map's fog, because a ticket per item would restate `C5`'s question and pre-empt `C12`. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file created or modified. **No singleton taken**; live `goalpilot-56e30` never contacted. Documentation half: `docs/pre-injested-docs/` recorded in `AGENTS.md` with the rule that **nothing downstream may cite a binary Hebrew source directly**, the `.docx` move committed as a **100% rename**, and the 08-06 transcription's source reference — **stale in two ways at once, moved *and* renamed, and undetected because a backticked filename is not a link any linter follows** — fixed as a link. The transcription stayed put and the source moved out, decided by counting references: **seven** inbound (including the body of map issue `#12`) against one. **Recorded rather than silent:** `SESSIONS.md` is one file, so both commits carry the still-uncommitted board row of the live `c10-quote-feed` session. **3 KB candidates written, none ingested** (normal mode; candidate 1 may belong in `rules/`, which is always-ask). `kb-candidates/` was listed before the first unit of work: **three pending files are now unowned** — `c9d-calendar-scopes`, `fix-task-completion-feedback` and `c9f-consent-screen-state` have all released — and still owe an ingest | 2026-08-09 | `e5916be` + this commit; see `CHANGELOG/2026-08-09/entity-model-intake.md` |
| `c9f-consent-screen-state` | **Planning plus one manual task — Markdown, four PNGs and issues; no application code.** `/wayfinder 12 33`: resolved **[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33)** (`C9f`), the map's first `wayfinder:task`. **The publishing status was `Testing`, so `C9d`'s worst case was the live case** — every grant expiring seven days after consent, silently, for as long as the Tasks import has shipped. **It is now `In production` and that clock is gone.** Getting there meant disproving a claim this repo had asserted as fact since 31/07 in **three files**, one of them a standing instruction to future sessions (*"leave it there — production hard-blocks sensitive scopes"*): **nobody had ever tested it.** The only `access_denied` anyone actually saw was the owner-is-not-a-test-user case — a *Testing*-mode failure that says nothing about production. **Run on a device 09/08: false.** Production shows *"Google hasn't verified this app"* with **Advanced → Go to GoalPilot (unsafe)** on the first screen, and `tasks.readonly` works through it — a live import returned **10 open tasks**, no `UserRecoverableAuthException`, no 403, clean logcat. All three files corrected; four screenshots committed because a claim that stood nine days and cost a session should not be re-litigated from prose. **The session caught itself committing the same sin:** it recommended publishing on an unchecked promise that the change was revertible, withdrew the recommendation, and sourced it — the answer is real but filed on an unrelated page ([Brand Approvals](https://support.google.com/cloud/answer/16868008)); the two pages you would actually read are silent, which reads as a one-way door. **Answered AFK before anyone was asked anything:** the **Google Calendar API was not enabled** on `goalpilot-56e30` (enabled on Ido's approval — missing it yields 403 `accessNotConfigured`, which does not read as a consent problem), and the release OAuth client is registered as `C9d` assumed. **The scope-category question is answered by being immaterial** — the console reveals a category only once a scope is *added* (a live mutation for a label), and `tasks.readonly` already puts the app in the sensitive regime, so Calendar cannot move it anywhere new. **A procedure error caught before it faked a pass:** the first plan said "drive a fresh sign-in", which would have proved nothing — the grant lives on the **Google account**, so sign-out, `pm clear` and uninstall all leave it intact; confirmed by accident when the emulator lost the app and the grant was still listed. **The finding nobody was looking for:** the `View your tasks` consent checkbox **arrives unchecked**, so sign-in succeeds while granting nothing — `C9b`'s hypothetical granular-consent risk is already a present fact for Tasks, filed as **[#36](https://github.com/idomarhaim/Android_Final_Project/issues/36)** rather than fixed. **Flagged not rewritten:** `GoalPilot-297750736036` did not appear on any of the four screens, so that `OPERATIONS.md` line is unreliable too — but one run is weak evidence, and this session was in no position to swap one untested assertion for another. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file created or modified; `:app:installDebug` ran twice as delivery, not as a change under test. Verification was behavioural (every screen screenshotted and quoted verbatim; the scope proven *working* not merely granted; logcat cleared and checked) and structural (map re-queried: 22 children, 4 closed, frontier re-derived as **#32** and **#29**). **Nothing written to live Firestore** — the import was cancelled at the review dialog and the dashboard read `8 / 5 / 4` before and after. **One error caught by that re-query and fixed in place:** the published resolution comment first claimed `C9c` was unblocked; `#27`'s `blockedBy` is `{33 CLOSED, 17 CLOSED, 25 OPEN}`, so it is **not** on the frontier — the comment was corrected rather than left standing. **`#emulator` `Pixel_10_Pro_XL` and `#gradle-daemon` taken and RELEASED**; second AVD never touched. Live `goalpilot-56e30` **written twice**, both on Ido's explicit approval, both recorded, and the before-state written to `docs/OPERATIONS.md` *before* anything changed. **The emulator died once** mid-session between install and first tap — rebooted, held, recorded rather than omitted. The shared map body `#12` was re-fetched and hashed immediately before writing: no drift, no clobber. **5 KB candidates written, none ingested** — normal mode, Ido's call; candidate 4 may supersede a standing KB claim and candidate 1 arguably belongs in `rules/`, both always-ask. `kb-candidates/` was listed before the first unit of work: the two files pending from `c9d-calendar-scopes` and `fix-task-completion-feedback` are now **unowned** — both sessions released — and still owed an ingest | 2026-08-09 | this commit; see `CHANGELOG/2026-08-09/c9f-consent-screen-state.md` |
| `c15-language-switching` | **Planning only — Markdown and issues, no code.** `/wayfinder 12 15`: resolved **[#15](https://github.com/idomarhaim/Android_Final_Project/issues/15)** (`C15`), and the headline is that **"language" was three settings wearing one name**. **Language** owns every *word* (chrome, AI text, the §8 fallback, month names), defaults to the device language, offers He/En, and is stored **per-device beside the skin** — because it must be known before the first frame and the account is not known until Auth resolves. **Region** owns **first day of week** and date order, defaults to the device country, and is **user-overridable and decoupled from language**. **Direction** follows Language, not Region, so English-in-Israel is LTR. **Ido overturned this session's question 6**, which had bundled formats with the week boundary and proposed pinning the week to Sunday: his decomposition is better and the reason is recorded — Israelis in hi-tech often work in English yet still start the week on Sunday, so a pinned Sunday is wrong for everyone else and a language-derived week silently **re-buckets analytics history**. Also settled: AI text **follows the picker** (which `C11a`'s 0/10 → 3/3 result prices at one prompt line, leaving [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30) the per-feature veto); the §8 fallback is **authored natively per language, never translated**; the trend chart is **exempt from mirroring** because `DonutChart`/`ProgressRing` are `Canvas` arcs that cannot mirror. **Two defects filed as spec lines, not fixed** — no prompt states an output language, and all ten date formatters are process-scoped `val`s no switch can move. Graduated **[#35](https://github.com/idomarhaim/Android_Final_Project/issues/35)** (`C15b`) out of the fog and cleared two fog patches from the map. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was structural (frontier re-queried: `#13`, `#14`, `#29`, `#32`, `#33`, with `#29` newly unblocked). **No singleton taken at all.** The one shared artifact was the map body `#12`, which carries no lease — re-read and hashed against the edited copy immediately before writing, confirming no drift and no clobber | 2026-08-08 | this commit; see `CHANGELOG/2026-08-08/c15-language-switching.md` |
| `c11a-free-model-probe` | **Planning only — Markdown and issues, no code.** `/wayfinder 12 16`: resolved **[#16](https://github.com/idomarhaim/Android_Final_Project/issues/16)** (`C11a`) by **measuring** rather than reasoning — **248 live calls** to `openai/gpt-oss-20b` at the production temperature (`0.7`), using the **verbatim** system prompts from `functions/src/index.ts`, in Hebrew and English, with opaque 20-char Firestore-style ids so id fidelity is a real test. **The pin was confirmed live first**, as the ticket demanded. **The headline retires a worry:** format is not where the risk is — **170/170 clean JSON parses**, 168/170 valid on every field, and *"prose around the JSON"* never occurred once, **including in the 20 calls sent with no `response_format` at all**. **Hebrew is not worse — both failures in the whole run were English.** **One failure mode, and it is silent:** an id supplied as `8xKq2mN4vRt7pLwZaB1c` came back `8xKq2mN4vRt7pLwZaB`, plausible and typed correctly, catchable only by membership in the list sent with it — while prompt-declared enums were perfect 50/50. So structural obedience is near-total and **referential** obedience is not. **Wide beats narrow** (1.7x faster, ~30% cheaper, three requests lighter on the 30-RPM ceiling): split on differing *fallback behaviour*, never on format. **Two results the ticket did not ask for, and they matter more than the one it did:** the numbers *inside* the valid envelope swing up to **2x** run-to-run and **1.8x** across languages, which turns `C1`'s manual-override question ([#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)) from a preference into a measurement; and the model writes **0/10** Hebrew coach messages given entirely Hebrew goals but **3/3** when one prompt line asks, while `suggestedNewGoalTitle` returns Hebrew **13/14** and is *stored as user content* — which prices `C15` ([#15](https://github.com/idomarhaim/Android_Final_Project/issues/15)) and puts AI text on the content side of its boundary before the picker exists. **An overclaim was caught by design, not luck:** arm 2 came back **60/60** under strict `json_schema`, which reads as enforcement — until arm 3's **prose control** obeyed the same absurd 1000–2000 range with no schema at all. The probe therefore cannot separate enforcement from compliance, and `C11b` gets schema recommended as a model-swap guarantee, not as reliability. **One error found and fixed, this session's own:** the first Hebrew metric counted *any* Hebrew character, scoring English prose that quoted a Hebrew title as Hebrew (12/20); recomputed on **script share**, the true rate is 2/20, and every Hebrew figure reported is the corrected one. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file created or modified; verification was empirical (all 248 raw replies persisted with latency, tokens and `finish_reason`; validity recomputed per field from that record) plus querying the map back out of GitHub after closing. **No singleton taken at all** — no `#gradle-daemon`, neither AVD, no write to live `goalpilot-56e30`. The only shared resource spent was **GROQ free-tier quota**, ~91k tokens in ~13 min, well inside 30 RPM / 1 000 RPD. **The probe harness was deliberately not committed** — this map ships no code, so the method lives in the asset instead. **Deliberately not done:** no new tickets and **no comments on #19/#20/#30/#15** — #15 is a live sibling's claim and this session's declared paths were #16 and #12 only; the map is the index. **Flagged, not fixed:** the map's Notes claim five day-one frontier tickets, but the graph shows `C13` ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32)) carries **no blocking edges**, making it six — re-wiring another ticket is a scoping act this session does not own. **3 KB candidates approved by Ido and drained 3/3** into the central bundle — `kb/dev/llm-structured-output.md` and `kb/dev/mechanism-vs-compliance.md`, both new; `kb-candidates/2026-08-08-c11a-free-model-probe.md` `git rm`'d here, the two repos tied only by the journal entry that names this one. `kb-candidates/` was listed before the first unit of work; the `product-model-map` file reported then has since been drained by `kb-ingest-map-method`, and the two that remain (`c9d-calendar-scopes`, `fix-task-completion-feedback`) are live sessions' to drain, not this one's. **Recorded rather than papered over:** `SESSIONS.md` was leased **four times** before a window opened (blocked twice, by `kb-ingest-map-method` then `c9d-calendar-scopes`), so this row's *claim* rode into `c9d-calendar-scopes`' commit rather than one of its own — named there by that session, and named here. Wrote `docs/research/2026-08-08-free-model-format-probe.md`, `CHANGELOG/2026-08-08/c11a-free-model-probe.md` and `kb-candidates/2026-08-08-c11a-free-model-probe.md`, all new | 2026-08-08 | this commit; see `CHANGELOG/2026-08-08/c11a-free-model-probe.md` |
| `c9d-calendar-scopes` | **Planning only — Markdown and issues, no code.** `/wayfinder 12 17`: resolved **[#17](https://github.com/idomarhaim/Android_Final_Project/issues/17)** (`C9d`), the map's **first closed ticket**. A dedicated GoalPilot calendar needs exactly **one** scope, `calendar.app.created` — it authorizes `Calendars.insert`, `Events.insert`/`patch`/`delete` **and** `CalendarList.patch`, so create, keep-current and colour are the whole loop, and the calendar is **Ido's**, not the app's (*"the authenticated user for the request is made the data owner"*). Verification is waived outright by the documented **personal-use exception**, which is exactly this map's fixed audience of one. **Two designs were nearly taken and are now ruled out with a reason:** a Cloud Function must **not** create the calendar (a service-account data owner cannot transfer ownership, and since the 2026 lifecycle change orphans are deleted — 2026-04-27 for personal accounts), and the scope should be requested at **first calendar use**, not bolted onto `GoogleSignInOptions` the way `tasks.readonly` was. **The finding the ticket did not know it was asking for:** if the OAuth consent screen is in `Testing`, *"authorizations expire seven days from the time of consent"* — the clock is on the **grant**, not the token — and this app's error handling is good enough that it would have been re-prompting the shipped Tasks import **weekly**, indistinguishable from a first run. Nobody could have filed it by observation. Filed as **[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33)** (`C9f`), the map's first `wayfinder:task`, wired to block **[#27](https://github.com/idomarhaim/Android_Final_Project/issues/27)**. **What it prices:** `C9c` loses "don't double-book" under the recommended scope — `calendar.app.created` is blind to every other calendar, so availability costs a second, broader scope; that trade is now priced instead of guessed, and was left as a comment on #27. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was source discipline (every external claim resolves to a primary Google page, every in-app claim to a file and line) plus querying the map back out of GitHub: 21 children, #17 closed, #33 unblocked, #27's `blockedBy` now `{33, 25}`. **No singleton taken at all** — no `#gradle-daemon`, neither AVD, live `goalpilot-56e30` never touched, no GROQ call, and no call to a Google API either (documentation only). **Recorded:** `SESSIONS.md` was leased three times before a window opened, and this commit carries the still-live rows of `c11a-free-model-probe` and `c15-language-switching` — one file, unavoidable, named rather than silent. **3 KB candidates left pending on Ido's call**, not drained | 2026-08-08 | this commit; see `CHANGELOG/2026-08-08/c9d-calendar-scopes.md` |
| `fix-task-completion-feedback` | `/kickoff` on the brief `product-device-pass` left. **Closed [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)** — completing a task was ~2 s of dead screen online and a silent no-op offline. **Ido's call: keep the `runTransaction`** — it holds `task.done`, points, the *derived* level and the *clamped* goal progress together, and the standard swap to `FieldValue.increment` can express **neither** a clamp **nor** a derived field, so it deletes the guarantee rather than moving it. The fix went into `GoalDetailViewModel`: an optimistic tick undone with a message on failure. **Measured, not asserted** — online, tap → checkbox went from **2.24 s with 1.20 s of dead screen** to the **first frame after the tap** (0.178 s fully drawn), donut moving in that same frame. Offline the planned escalation fired: the undo alone let the lie stand **7.9 s** (Firestore's DNS + retry budget before `UNAVAILABLE`), so a new `core/net/ConnectivityMonitor` now refuses the tap in **0.19 s** — the undo stays behind it, since a connectivity check proves a network, not that Firestore answered. Two finds the issue did not know about: the discard shape was at **five** sites not two (`SocialViewModel.removeFriend` announced *"Friend removed"* **before** checking the result), and the undo's snackbar was showing the raw gRPC `UNAVAILABLE: Unable to resolve host firestore.googleapis.com` on a real screen. **213 JVM (197 + 16 new) and 29 instrumented green** — the instrumented suite run despite no composable changing, because `ConnectivityModule` is a new Hilt module that could have broken the test graph. Rules and Functions untouched, so not run. Live `goalpilot-56e30` **was** written (a real +20 completion) and **restored and verified**: 70 pts, Level 1, 7 goals, 5 tasks done, 24 %, goal back to `1 / 100 %`. `#emulator` `Pixel_10_Pro_XL` and `#gradle-daemon` **released**. The Cloud-Function inversion filed as **[#34](https://github.com/idomarhaim/Android_Final_Project/issues/34)** rather than smuggled in. **One debt paid that `kb-ingest-map-method` recorded as owed:** its `CHANGELOG_README.md` row. Mine went in by staging *only my line* out of a file that also held `c9d-calendar-scopes`'s unstaged row — theirs is still there, untouched and still owed | 2026-08-08 | `742fa36` (pushed); see `CHANGELOG/2026-08-08/fix-task-completion-feedback.md` |
| `kb-ingest-map-method` | **Ingest only, Markdown only.** Bare `/kb-ingest`; drained `kb-candidates/2026-08-08-product-model-map.md` 3/3 into the central bundle and `git rm`'d it — two new pages there, `kb/dev/decision-map-charting.md` (a constraint ticket that "prices everything" **splits**; a "knot" wants an **order**, not a merge — same document, minutes apart, opposite fixes) and `kb/dev/github-issue-graphs.md` (`gh` 2.96.0 sub-issues, GraphQL `addBlockedBy` taking node IDs). Two repos, so no commit holds both halves; the tie is the journal entry naming this repo's path. **`kb-candidates/2026-08-08-c9d-calendar-scopes.md` deliberately untouched** — that path is in the live `c9d-calendar-scopes` row's **Owns** column, and draining means rewriting or deleting it (§5 rule 2); Ido's call was to leave it with its own session, and its entry 3 is a §3-shaped hole in the charting page that the journal records so the next drain fills rather than duplicates. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; `Check-KbLinks` CLEAN at 30 pages. **No singleton taken**: no `#gradle-daemon`, neither AVD, live `goalpilot-56e30` never contacted. **Two debts recorded, not paid, and for the same reason both times — a live sibling's *uncommitted* state, which a lease cannot fix:** this row was never committed by its own session (three other sessions' rows sit unstaged in this file, and committing them for them is the hazard `product-model-map` recorded at `9466990`), and no `CHANGELOG_README.md` row was written (that file carries `c9d-calendar-scopes`'s unstaged index row). Both are owed by whichever session next finds those files free | 2026-08-08 | `02d70d4`; central half `70e30dc` in `C:\Dev\JARVIS` |
| `product-model-map` | **Charting only — Markdown and issues, no code.** Turned the 13 undecided product-model questions from the 2026-08-06 brief into **[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)**, a `wayfinder:map` with **20 decision tickets** as native GitHub sub-issues and **25 blocking edges**; five are on the frontier, two of those AFK. Ido fixed five things the brief could not derive: the destination is a **written v0.3 spec** (`docs/PRODUCT_v0.3.md`), the audience is **one real user daily**, the free model is a **permanent** constraint (so every AI feature is specced with a non-AI fallback beside it), `C9` is **fully in scope** (five tickets, no second map), and **localization is in scope** as an in-app language picker — a requirement that appears nowhere in `R1`–`R28`, surfaced by the device pass's `A1`. **The proposed charting order was tested and partly overturned**, and the reasons are recorded rather than replaced: `C11` was two questions wearing one hat (you cannot test a format nobody has designed yet), the "C1–C4 knot" wanted *ordering* not merging — `C4` is the real root, not `C11` — and `C7` turned out unblocked. `D1` graduated to `C14` ([#23](https://github.com/idomarhaim/Android_Final_Project/issues/23)) on Ido's call, with `product-device-pass`'s handoff block lifted rather than duplicated. **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was structural, querying the graph back out of GitHub after wiring (20 children, every edge present, frontier exactly the five intended, no cycles). **No singleton taken at all**: no `#gradle-daemon`, neither AVD, live `goalpilot-56e30` never touched. **Recorded, not papered over:** this session's board row never got a commit of its own — it was staged into `9466990` by the concurrent session before it could be committed, which is the commons-lease hazard `AGENTS.md` names, and neither session took a lease | 2026-08-08 | this commit; see `CHANGELOG/2026-08-08/product-model-map.md` |
| `product-device-pass` *(2nd sitting)* | Re-claimed to close the one item the first sitting left `unverified` — the **first-run empty states** — with Ido's approval to `pm clear`. It **did not** reach the zero-data states (signing back in restores everything from Firestore, as predicted) and says so; a throwaway account is the only remaining route. It **did** find `A10`: a cold, cacheless first load is a **blank page and a single ~8 px dot** for ~10 s — what every user gets on a new phone. Also recorded an environment trap: `pm clear` wedges Play Services on this emulator (`SignInActivity` focused, rendering nothing, through two retries), and `am force-stop com.google.android.gms` clears it — the discriminator being that `dumpsys window \| grep mCurrentFocus` names GMS, not the app. `D1` handed to `product-model-map` as a **liftable `C14` block** rather than written into their file. Wrote `sessions/fix-task-completion-feedback.md` for issue #3. **No suite run, none applicable**; app restored to exactly as found (Aurora, 70 pts, 7 goals) and verified; `goalpilot-56e30` read-only; `#emulator` **released** | 2026-08-07 | this commit |
| `product-device-pass` | **Read-only against the code; Markdown and issues only.** Drove a real debug build on `Pixel_10_Pro_XL` as Ido to turn the `product-review` backlog from static claims into verdicts, then filed **the repo's first GitHub issues, [#2](https://github.com/idomarhaim/Android_Final_Project/issues/2)–[#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)**. `D2`/`D4`/`D5` confirmed (the accessibility tree names exactly which nodes are clickable — the life-area row and goal count are not, and the whole social feed card has **zero** interactive nodes); `D3` **measured at 2.24 s and 1.94 s** from frame-timed recordings, with the cause found: `setDone` is a **server-only Firestore transaction** whose `Resource` `toggleTask` discards — which is also why the identical tap is a **silent no-op offline** (`A5`), so both were filed as one issue. `D1` **reclassified, not filed**: a challenge's score has exactly one writer (`reportScore`) and `ChallengeType` is decorative, so "what should a challenge score from?" is an undecided model question — it belongs in `TODO_FUTURE/`, which the live `product-model-map` session owns, and was deliberately left unmoved (see the ⚠️ above). Device half of the UX pass added as `A5`–`A9`, plus a *checked-and-not-a-defect* section (both skins in dark are fine; **GROQ is live, not falling back**; the FAB clears the last card). **No suite run and none applicable** — no Kotlin, Gradle, rules or Functions file touched; verification was behavioural (`uiautomator` dumps, VFR `screenrecord`, PSNR, logcat). **Not verified, and said so:** first-run empty states, which need `pm clear` or a throwaway account. `#emulator` `Pixel_10_Pro_XL` and `#gradle-daemon` leased and **released**. Live `goalpilot-56e30` **was** touched — one task toggled done and back to time `D3`, **restored and verified** (`2 / 100 %`, `70 pts`); theme switched to Blossom for the dark check and **restored to Aurora** | 2026-08-06 | this commit |
| `product-review` | **Markdown only.** Intake of Ido's 2026-08-06 pre-sleep product/UX brief: faithful English transcription (`R1`–`R28`) beside the `.docx`, the actionable half (`D1`–`D5`, `U1`–`U6`, `A1`–`A4`) split from the 13 product-model decisions (`C1`–`C13`) bound for a `/wayfinder` map, plus two session briefs for the halves. Deliberately **no device pass, no GitHub issues, no map** — every repro note is static, and this repo has been burned once by a stale backlog premise, so nothing graduates until it is reproduced. Headline finding: the reported task-score/goal-percentage "bug" is **not a defect** — `Task.points` and `currentValue/targetValue` are independent by construction, joined only by `progressContribution` (default `1.0`, invisible in the UI) — so it was reclassified to decision `C3` rather than filed. Also recorded: no `values-he`, so the app has no Hebrew and no RTL, which is not in the brief. No `app/`, `functions/`, `firestore.rules` or `scripts/` file touched; **no suite run and none applicable**; neither AVD nor `#gradle-daemon` taken; live `goalpilot-56e30` untouched | 2026-08-06 | this commit |
| `release-distribution` | Signed release key, Firebase App Distribution on both ends (upload plugin + in-app update prompt), and a tag-triggered release workflow — so an APK reaches other people's phones and updates itself afterwards | 2026-08-06 | `5316782`, `1f41b50`, `40cfc12`, `356613d`, `7e21ab1`, `964d6e9`; see `CHANGELOG/2026-08-05/release-distribution.md`. **Proven end to end on a physical phone:** install → Google Sign-In under the release key → in-app update `v0.2.1` → `v0.2.2` in one tap. 197 JVM green; instrumented and rules suites not run (no UI, no rules file touched). `#gradle-daemon` leased twice and **released**; neither AVD taken. Live `goalpilot-56e30` **was** touched — release SHA-1, `testers` group, service account — all additive, all listed in `docs/RELEASING.md`. Note for the next session: **`v0.2.2` was built and uploaded from the developer machine**, because GitHub could not allocate a hosted runner on two attempts (15-min acquisition timeout, zero steps). CI itself is fine — `v0.2.1` went green on the same file |
| `kb-audit` | **Ingest only, Markdown only** — this repo's share of a cross-repo KB-candidate sweep run from `C:\Dev\GenAI-Driven-Dev-Self-Improvement`. **New** [`knowledge/release-distribution.md`](knowledge/release-distribution.md) from `CHANGELOG/2026-08-05/release-distribution.md` (off-Play means Android supplies no update mechanism; the signing key is unrecoverable so it precedes the first hand-out; tag-triggered because `versionCode` is manual), plus the bundle index and journal, and the missing `CHANGELOG_README.md` row for that 08-05 session file. Three claims from `second-avd`, `submission` and `release-distribution` generalise past GoalPilot and were ingested **centrally** instead (PowerShell 5.1 encoding traps · second-AVD mechanics · an authorization rule needs a second real identity). No `app/`, `firestore.rules`, `functions/` or `scripts/` file touched; neither AVD nor `#gradle-daemon` taken; no suite run. **Recorded rather than papered over:** the row went in *with* the commit rather than before the first write — the tree was clean and the only Active row owns disjoint paths, but the ordering rule says claim first and this session did not. **Also noted, not touched:** the `release-distribution` Active row is stale — its work is committed and pushed (`5316782`), so it is a release somebody owes | 2026-08-06 | this commit |
| `template-sync-v16` | 🔁 **Mechanical template sweep**, driven from JARVIS by `Update-TemplateConsumers.ps1`: `general.instructions.md` **v14 → v16**, `new-changelog-entry.prompt.md` v3 → v4, `AGENTS.md` v12 → v14. Clears the v14 → v15 gap the 2026-08-04 sweep **correctly** refused while `challenges` held a dirty tree — that tree is clean now, so both versions landed in one pass. Verbatim projections only; no decision was taken in this repo, no Kotlin/Gradle/Firestore file touched, and neither `#emulator` nor `#gradle-daemon` was taken. **No Active row was claimed:** a single-commit mechanical sync into a clean, unclaimed tree, so a claim created and cleared in the same breath protects nothing (`C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md`, *mechanical sync* row) | 2026-08-05 | this commit |
| `backend` | Live Firebase backend, E2E verification, Google Tasks import, JARVIS §5 governance | 2026-07-31 | `6e4a184`, `79ce624`, `1ebb178`, `53c2afb`, `64802e5`, PR #1 |
| `launchers` | One-click run scripts; made the emulator singleton self-enforcing | 2026-08-01 | `dc1c06e` + follow-up (pending) |
| `health` | Health Connect integration — steps & sleep, read-only, review-before-write | 2026-08-02 | see `CHANGELOG/2026-08-02.md`; emulator released |
| `theming` | Selectable app skins (Aurora/Blossom) + UI/UX pass | 2026-08-02 | `e31ac9d`, `a413485`, `c30709e`; emulator released |
| `scaffold` | Template-library upgrade — `AGENTS.md` v8→v10, `general.instructions.md` v10→v12, this file v1→v2 | 2026-08-03 | see `CHANGELOG/2026-08-03.md` |
| `lifeareas` | Life areas (user-defined + synced from Google Tasks list names), LLM task durations, interactive time-allocation analytics at day/week/month/quarter/year | 2026-08-03 | `fe9f61d`; see `CHANGELOG/2026-08-03/lifeareas.md`. Emulator released. |
| `challenges` | Competitive challenges: the `participants` security rule that makes joining possible, `firestore-tests/` (the repo's first rules test layer), and the domain + data + DI layers | 2026-08-04 | `1e56ee3`, `8117368`; see `CHANGELOG/2026-08-04/challenges.md`. **Rules written and tested but NOT deployed.** UI continues in `sessions/challenges-ui.md`. Emulator never claimed in practice; Gradle daemon released. |
| `second-avd` | Second emulator `Pixel_10_Pro_XL_B` for the two-account demo; `-Avd` made a demand so it never adopts the other session's screen | 2026-08-05 | see `CHANGELOG/2026-08-05/second-avd.md`. No app code touched, so no suite was run — verification was behavioural against both live emulators. Both AVDs left running; neither claim held. |
| `time-insights` | A stacked-column trend beside the time-allocation donut, and an AI re-estimation pass for tasks that never had a duration | 2026-08-04 | `342af48` + verification pass; see `CHANGELOG/2026-08-04/time-insights.md`. **All layers green and fully verified**: 150 JVM, 20 instrumented, and a live re-estimation run against GROQ that wrote 7 durations to `goalpilot-56e30`. Ran in two sittings — released once with the device checks blocked, re-claimed when the AVD came free. Emulator and Firebase project **released**. |
| `lifearea-polish` | Drag-to-reorder life areas (minimal `sortOrder` writes) and the goals list banded by life area | 2026-08-04 | `6f4a749`; see `CHANGELOG/2026-08-04/lifearea-polish.md`. Both layers green — 144 JVM, 20 instrumented. Emulator `Pixel_10_Pro_XL` recovered from a wedge and **released**; Gradle daemon released. |
| `submission` | Deployed `firestore.rules` to live `goalpilot-56e30` and proved the **non-owner challenge join** end-to-end with two real accounts on both AVDs; captured the spec §7 sharing evidence | 2026-08-05 | see `CHANGELOG/2026-08-05/submission.md`. **16/16 rules tests** re-run against the deployed file; JVM and instrumented suites **not run** — no Kotlin/Gradle file changed. Both emulators and the live project **released**. Found the MUST item's premise stale: two profiles, both friend edges and a share had existed since 02/08, so §7 needed capturing rather than building. Health Connect re-checked on API 37 by request — permissions and the empty read are fine; the physical-phone follow-up stands |
| `health-autosync` | Health Connect made automatic: syncs on every app foreground, throttled to 15 min by a per-uid SharedPreferences stamp, and writes every unsynced reading with no review sheet. The dedupe became a *value* comparison so today is topped up rather than frozen at its first reading | 2026-08-05 | see `CHANGELOG/2026-08-05/health-autosync.md`. **197 JVM and 29 instrumented green**; rules untouched, so `firestore-tests/` not run. Throttle proven in both directions against the on-device stamp, not the UI. **Still unproven: the write path against real step data** — the emulator's store is empty, so the physical-phone follow-up below stands and now covers the top-up path too. Emulator `Pixel_10_Pro_XL` and the Gradle daemon **released** |
| `challenges-ui` | Competitive challenges as a real screen: ViewModel, live standings, discover/join/leave, score reporting, create flow | 2026-08-05 | see `CHANGELOG/2026-08-05/challenges-ui.md`. All three layers green — 175 JVM, 29 instrumented, 16 rules. Ran against an empty board, uncontended. Emulator `Pixel_10_Pro_XL` and the Gradle daemon **released**. The live project `goalpilot-56e30` was **never touched** — the rules deploy is held for the two-account session on Ido's call, so a non-owner join is still proven by `firestore-tests` only. |

> **Post-mortem, recorded because the next session should not repeat it.** The
> `theming` session ran for two days without ever reading this board — it did not
> exist when that work started, and the `AGENTS.md` it read (template v4) had no
> pointer to it. Consequences: it used `git add -A` (the one thing rule 3 forbids
> by name), wrote to `feature/dashboard/DashboardScreen.kt` and
> `di/RepositoryModule.kt` while `health` owned them, and used the Gradle daemon
> and the AVD while `health` held both. Nothing was actually lost — the two
> sessions happened to edit different regions of `DashboardScreen.kt`, and the
> `add -A` landed in a window where the tree held no sibling work — but only by
> luck. The rule text and enforcement were tightened in JARVIS §5 as a result.
