# KB candidates — `c9a-schedule-a-task`, 2026-08-10

Repo: `c:\Dev\Android_Final_Project` · Session row: `c9a-schedule-a-task`

Mode: **normal**, so nothing here is ingested. This file is a proposal; the drain is
Ido's call via `/kb-ingest`. Every entry stands alone — no entry may be reconstructed
from this session's transcript.

**Reconciliation note before ingesting:** this repo drained a 21-entry backlog on
2026-08-10 (`kb-ingest-backlog-drain`), creating five central pages. Entry 5 of
`fix-task-completion-feedback` proved that *"check whether the page already exists"* is
the single most valuable hedge a candidate can carry, because three of its entries
proposed pages written two days earlier. Each entry below therefore names whether it
expects to **update** an existing page or **create** one, and says which.

---

## 1 · A time is not a property of the thing that happens

**Claim.** When a model needs to say *when* something happens, the durable question is
not the precision of the timestamp — it is **how many independent whens one thing may
have, and what remembers the outcome of each**. Storing the time as a field on the
object silently fixes that answer at *exactly one*, and the cost is invisible until the
first recurring or multi-session case arrives. The general shape that survives is
**a rule on the object plus materialised occurrences beside it**: the rule generates,
the occurrences remember. A rule alone cannot hold a per-instance exception; occurrences
alone cannot express "forever".

**Why.** Found by working an ordinary-looking feature request — *"let the agent schedule
tasks in a calendar"* — that read as one decision and was six. The load-bearing one was
in none of the request's words. Three shapes were compared against two real examples
(*"buy flowers every two weeks"*, *"an exam marathon of four study blocks in one
week"*): a **date field on the object** makes the recurring case 26 duplicate documents
a year and gives a missed instance nowhere to live; a **recurrence rule with dates
computed and nothing stored** is exact but cannot record that one instance moved, was
skipped, or carries an external id; **rule plus stored occurrences** expresses both.

The decisive argument turned out to be a *user-facing* one rather than a schema one, and
it generalises: only the combined shape can ask **"this occurrence, or all future
ones?"** A field-only model permanently answers *just this one*; a rule-only model
permanently answers *all of them*. Any design that must ask that question has already
chosen this shape whether or not it knows.

Rejected: (a) *"start with a field and migrate later"* — the migration is over live
user data and the field's presence is what teaches every call site to assume one time;
(b) *"the calendar library will handle it"* — the question is what the app's own model
records, and every sync target (Google Calendar included) already uses rule + instances,
so a lossier local model makes sync a translation rather than a mapping.

**Destination.** `C:\Dev\JARVIS\kb` — **new page**, `dev/rule-plus-occurrences.md`.
Checked: no existing page covers scheduling or recurrence modelling. Closely related to
the existing `dev/edges-not-types.md` (both are *"the interesting thing is not a
property of the object"*), and the two should cross-link — but they are different
claims and should not be merged.

**Anchors.** none in this repo yet — the model is specced, not built. The negative
evidence is anchored: `Task.isDone` as a latching `Boolean`, and
`GoogleTasksClient.kt:145` parsing a due date into a field nothing reads.

**Supersedes.** nothing.

**Status.** proposed.

---

## 2 · A field earns its place when something *behaves* differently, not when a distinction feels real

**Claim.** The test for whether two cases deserve separate representation is whether any
downstream logic **branches** on the difference. "These feel like different things" is
not the test and reliably over-models; "we can always tell them apart later" is not the
test either and reliably under-models. Apply the test at the point of *consequence*: if
the two cases produce the same behaviour everywhere, collapse them; if one place treats
them differently, that place is the field's justification and belongs in the comment.

**Why.** A ladder of timestamp precisions (day / hour / hour-block) was about to be
specced, which is the intuitive cut. Looking at a discarded import field showed the real
distinction was not precision at all: *a moment you must be finished by* versus *a span
you intend to work during*. What proved it was not the intuition but the consequence —
**a missed span is rescheduled and nothing is wrong; a missed deadline means you are
late.** One is a failure and one is not, and no amount of precision-modelling produces
that fork.

The same test then ran a second time, in the opposite direction, on a state machine: an
"unconfirmed proposal that expired" was about to share the `MISSED` state with a genuine
miss. It behaves differently — it must count for nothing, because the *system* proposed
it and the user never agreed — so it earned its own state. Without that split an
over-eager agent **manufactures failures** attributable to the user, and they flow into
whatever statistics the product later builds.

Rejected: (a) *modelling by precision* — the natural cut, and it collapses the one
distinction that changes behaviour; (b) *one state with a boolean flag beside it* —
same information, but the branching logic stops being legible at the point of use;
(c) *storing both and deciding later* — "later" is when the statistics are already
wrong and nobody knows why.

**Destination.** `C:\Dev\JARVIS\kb` — **update in place**, `dev/enum-and-label.md`, as a
new section. That page was created 2026-08-10 from this repo's `C7` work and already
holds the *closed kind beside a free word* claim; this is the adjacent question of when a
kind deserves an extra member. **Check its current headings before adding** — it may
already have a §2 that this belongs under rather than beside.

**Anchors.** none — the rule is about how to decide, and its evidence is the two
decisions above.

**Supersedes.** nothing.

**Status.** proposed.

---

## 3 · Derive temporal state; never store it — and the payoff is not the one you expect

**Claim.** State that is a pure function of *stored data plus the current time* — overdue,
expired, active, elapsed — should be **computed at read time**, never written to a
record by a sweep. The obvious arguments are cost and staleness. The **non-obvious and
larger** payoff is that anything which fires later can re-derive the truth at the moment
it fires: a scheduled notification can ask *"is this still relevant?"* for free, so the
classic bug of being reminded about something already finished cannot occur. A stored
status inverts this — every state change must now hunt down and invalidate whatever was
scheduled against the old value.

**Why.** Two mechanisms were compared for marking a scheduled thing as missed: a nightly
server job that stamps a stored field, versus computing it on every render. The project
could afford the job — the backend was already on a paid plan with free scheduler
quota — so cost did not decide it. What decided it, in order: an existing shipped
function in the same codebase already derives a three-state phase from two dates on every
render, with no sweep and nothing that can go stale, so the pattern was proven in place;
and the job's only genuine advantage was that a stamp is an *event*, and an event can
send a notification.

**That advantage then evaporated on inspection, and this is the transferable part.** The
requirement had been read as "notify me about the miss". Re-reading the original request
showed it asked for **reminders**, which come *before*. A before-reminder hangs off the
record's own time and needs no stored status at all. So the single argument for storing
state was an artefact of misreading a requirement — and *checking what was actually asked
for* is what made the cheaper design also the correct one.

The coupling discovered afterwards is the entry's real content: **not storing the state is
what makes the notifications trustworthy.** The two decisions looked independent and were
not.

Rejected: (a) *store it for query performance* — premature here, and the read is a
comparison against a timestamp; (b) *store it so a notification can be triggered* — the
notification wanted a different trigger entirely; (c) *store it because a server owning
derived state is cleaner* — a real argument, live in this repo as an open issue for a
different field, but it applies to state derived from **writes**, not from **the clock
advancing**. That boundary is the useful distinction: nobody writes anything when time
passes, so there is no event to own.

**Destination.** `C:\Dev\JARVIS\kb` — **new page**, `dev/derive-dont-stamp.md`. Checked
against the five pages created on 2026-08-10: `optimistic-ui-patterns` is the nearest
(both about *when* to trust a value) but it is about retiring an overlay against observed
data, not about clock-derived state. Cross-link, do not merge.

**Anchors.** `Challenge.phaseAt(now)` and its per-render call site are the shipped
precedent; the counter-case is the repo's own open issue arguing for server-owned derived
state on task completion.

**Supersedes.** nothing. **Note the tension explicitly** when writing the page: this repo
holds an open proposal in the opposite direction for a *write*-derived field, and the page
must say why both are right rather than pretend the conflict does not exist.

**Status.** proposed.

---

## 4 · What the system cannot see is a better basis for a permission rule than how much you trust it

**Claim.** When deciding whether an agent may act silently or must ask, the productive
question is not *how confident is it* but **what is it blind to**. Confidence is a
property of the agent and is usually unmeasurable in practice; blindness is a property of
the integration and is a fact you can look up. A rule derived from blindness also
**upgrades itself for free**: when the missing visibility is later acquired, the rule's
precondition becomes satisfiable and the wording does not change.

**Why.** An agent was to schedule things on a user's behalf. The instinct was a
confidence threshold; that was dead on arrival, because the codebase's existing
classifier writes a confidence score **no caller reads**, so a threshold would have been
decorative. The instinct after that was a blanket "always ask", which the original
request explicitly did not want.

What produced a usable rule was noticing that the risk is **not uniform across the
cases**. The integration had been granted a narrow scope that lets it write its own
calendar and read nothing else, so it is blind to every other commitment the user has.
Cases that occupy no time slot — a bare date, a due moment, a multi-day period — **cannot
collide with anything invisible**, and are therefore safe to set silently. Only the case
that occupies a specific slot is unsafe, and it is unsafe *for a stated reason that can be
removed*. Buying the read scope makes silent placement correct, and the rule already says
so.

Rejected: (a) *confidence threshold* — the score exists and is never read; (b) *always
ask* — friction where nothing can go wrong, and it converts an agent into a suggestion
box; (c) *never ask* — guaranteed to collide with what cannot be seen, and this codebase
already had a live instance of acting unconditionally on a total-failure fallback;
(d) *make the behaviour depend on the permission state from day one* — defensible, but it
owes two specified paths and a UI explaining which one the user is in, so it is better as
the documented upgrade than as the initial rule.

**Destination.** `C:\Dev\JARVIS\kb` — **new page**, `dev/blindness-not-confidence.md`.
Checked: `dev/recovery-masks-failure.md` (created 2026-08-10) is thematically adjacent —
both are about faults with no observer — but that page is about error handling hiding a
policy fault, and this is about authorising autonomy. Cross-link.

**Anchors.** none in code — the rule is a spec constraint. The blindness itself is
anchored in this repo's committed scope research.

**Supersedes.** nothing.

**Status.** proposed.

---

## 5 · Confirming a plan is one interaction; confirming its items is N

**Claim.** When a system proposes several related items, the confirmation boundary should
be the **plan**, not the item. Per-item prompts scale with the proposal and are the
common reason a genuinely useful batch feature feels unusable. The pattern that works —
and that most codebases already contain somewhere — is one review surface listing every
proposed item, individually toggleable, with a single commit action. **Look for the
existing instance before designing a new one**: a second review idiom in the same app is
worse than a slightly imperfect reuse of the first.

**Why.** A rule requiring user confirmation for one category of agent action was correct
and, applied per item, would have cost a dozen prompts for a single week of planning —
against a request that had explicitly asked the agent to do this work. The fix was not to
weaken the rule but to move where it applies. Searching the codebase found the idiom
**already shipped** for an unrelated import feature: N proposals, checkboxes, one confirm
button. Reusing it cost nothing and inherited familiarity.

The second half is what makes it more than a UI note. A proposal held only inside a dialog
dies with the dialog, so the agent cannot usefully work while the user is away. Writing
proposals as **real records in a provisional state** — visible, distinguishable, and
excluded from any outbound sync until confirmed — lets the agent genuinely act, survives
the process being killed, and keeps unconfirmed output out of external systems. It needs
one companion rule to be safe: a provisional item that is never confirmed must expire
**counting for nothing**, or the system accumulates failures the user never agreed to.

Rejected: (a) *per-item prompts* — the friction scales with usefulness; (b) *proposals in
memory only* — cannot survive, cannot be reviewed later, and blocks unattended work;
(c) *write proposals as normal records and let the user delete the wrong ones* — they
would reach downstream systems and be indistinguishable from confirmed data, which is
exactly what the provisional state prevents.

**Destination.** `C:\Dev\JARVIS\kb` — **update in place**, `dev/review-intake-and-triage.md`,
as a new section. That page already exists and already holds a §1.1 about intake review
from this repo's earlier work, so this is an extension of a live page rather than a new
one. **Read its current structure first** — the batch-sheet half may already be partly
stated there, in which case only the provisional-record half is new.

**Anchors.** the shipped import review dialog in this repo is the concrete instance to
cite.

**Supersedes.** nothing.

**Status.** proposed.
