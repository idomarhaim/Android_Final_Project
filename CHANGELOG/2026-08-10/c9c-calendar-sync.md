# `c9c-calendar-sync` — Google holds the *when*, GoalPilot holds *what happened*

**Session:** `c9c-calendar-sync` · **Invocation:** `/wayfinder 12` *(bare — no ticket
named)* · **Branch:** `feat/goalpilot-implementation` · **Mode:** normal at start,
**`AUTO MODE`** from Ido's second message · 2026-08-10.

One ticket resolved, which is the skill's limit. **No code was touched** — this map
ships no code, and that held: `GoogleTasksClient.kt` and `SyncHealthDataUseCase.kt`
were read and neither was edited.

## What changed

| | |
|---|---|
| Resolved | [#27 · `C9c` Google Calendar sync: direction and conflict resolution](https://github.com/idomarhaim/Android_Final_Project/issues/27) — closed, with the full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*, plus **one fog patch narrowed**: the deprecated-`GoogleSignIn` patch, whose *shape* changed rather than its size (see *Session hygiene* — the pre-commit review caught this session first claiming it had narrowed nothing) |
| Tickets created | **none** — every hand-off landed on a ticket that already existed. Third resolution on this map to manage it |
| Hand-offs commented | [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) `C9b` (a live sibling's claim — commented, never edited) · [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) `C9e` (unblocked) · [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) `C12` (busy/free is not effort) |
| Unblocked | [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) — `#27` was its only blocker |
| Board | claim row added; the *Unclaimed work* frontier block corrected (see *Session hygiene*) |
| Frontier now | [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) `C3` · [#28](https://github.com/idomarhaim/Android_Final_Project/issues/28) `C9e` · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` — plus [#26](https://github.com/idomarhaim/Android_Final_Project/issues/26) `C9b`, claimed and live |

## Why this ticket

`/wayfinder 12` arrived bare, so the frontier was re-derived out of GitHub — open,
unblocked, unassigned children of #12 — rather than read off `SESSIONS.md`, whose
list turned out to be wrong in both directions. **25 children, 10 closed** at session
start; two takeable and unassigned: **#27 (`C9c`)** and **#39 (`C18`)**.

Two independent arguments landed on the same ticket, so there was no fork to put to Ido:

- **Issue order** — the skill's own default: *"take the first frontier ticket in order."*
- **Entanglement** — #39 asks *"what does every roll-up sum over?"* while the then-live
  `c17-many-to-many` (#38) was deciding whether one task's contribution splits across
  several parents. That is the same arithmetic answered twice, in two sessions. #27
  sits beside `c9b-calendar-surface` (#26) at the softer *surface vs semantics* seam,
  which is coordinable by comment.

Logged as a derivation rather than asked, per the derivable-decision rule.

## The decision

**Google holds the *when*. GoalPilot holds *what happened*.**

Not a slogan — forced by what each side can represent. A Google event has a start, an
end and a title, and no field for `MISSED`, `OVERDUE`, `EXPIRED`, `PROVISIONAL`, no
goal, no measure, no points. Every attempt to make the calendar carry state ends in an
encoding — a ✓ in a title, a colour meaning "late" — that nothing else respects and the
user can destroy by typing. **The sync carries times in both directions and state in
neither.**

### 1 · Two-way, and the conflict fork did not exist

Ido chose: an edit made in Google Calendar comes back.

**It costs no extra scope** — `calendar.app.created` is *"see, create, change, and
delete events"* on the calendar the app made, so reading back what we wrote was already
bought by `C9d`. The read-scope trade is about **other** calendars and is a separate
decision; conflating them would have produced a fork where none existed.

**The conflict question is a phantom, and #27's own body proves it.** It asks what
*"ask"* looks like *"on a phone the user is not holding"* — and there is no such moment.
`GoogleTasksClient.kt:163-170` mints tokens with `GoogleAuthUtil.getToken`: short-lived,
no refresh token, and a `UserRecoverableAuthException` carrying an Intent only a
foreground UI can launch. `C9d` separately banned the service-account route. **There is
no credential for a background sync and there cannot be one**, so the pull runs when Ido
opens the app and the both-ends-changed window is minutes wide for one user.
Last-write-wins is *correct*, not a compromise; "ask" is not rejected, it is unbuildable.

### 2 · The finding: a move-out is indistinguishable from a delete

`calendar.app.created` sees only the GoalPilot calendar. An event Ido **drags out** of
it reads exactly like one he **deleted** — `status: cancelled` — and no scope fixes it.
Both obvious designs are wrong: *disappeared ⇒ delete* is silent data loss on a
harmless-looking drag (and erases the history `E4` needs, which `C9a` already refused to
lose once); *disappeared ⇒ re-create* means the app fights the user.

**Rule: a disappearance never deletes and never re-creates.** The occurrence keeps its
date and clears its `googleEventId`.

**The improvement is to stop guessing and ask** — in `C9a` §5c's daily review, the one
moment Ido *is* holding the phone: *"3 items left your calendar — still doing them?"* →
Keep / Cancel / Put back. Same batch sheet, **fourth** reuse of the shipped Google Tasks
import idiom.

### 3 · Ido's answer replaced the question: per calendar, not per app

The picker offered one axis — how much of *"your other calendars"* GoalPilot may see —
in three global cuts. **Ido answered on a different axis: calendar by calendar**, some
shared fully, some only as busy/free.

That is the better shape, for the reason the picker missed: **"my calendars" is not one
thing.** A shared family calendar and an employer's calendar are different objects, and
one global level has to be set for the most sensitive of them, wasting the rest.

**The fact it runs into, recorded rather than buried: Google cannot enforce a
per-calendar split.** Scopes are per-*scope*, never per-*calendar*; there is no consent
screen granting full access to two calendars and busy/free to a third. So a design with
both modes needs `calendar.readonly`, and from that point the split is **a promise
GoalPilot keeps, not one Google enforces.** A privacy control the user believes is
enforced and is not would be worse than no control.

**The design that honours it as far as is possible — incremental authorization:**

| Moment | Grant held |
|---|---|
| Sign-in | `calendar.app.created` + `calendar.calendarlist.readonly` — can write our calendar and **name** his others; cannot read one event |
| He ticks calendars to avoid | `+ calendar.events.freebusy` — busy/free on exactly those, queried by id. No titles ever reach the app. **Google-enforced** |
| He sets one calendar to Full | `+ calendar.readonly` — asked **at that moment, with that calendar named**. From here the split is ours to keep |

Default is Busy-only; Full is never a default. Three properties this buys over asking
for `calendar.readonly` at sign-in: **if Ido never uses Full the promise is never made**
(the app cannot read a title because it lacks the grant); the broad consent screen
arrives with a named reason rather than as one checkbox among five; and it is **the
shipped pattern** — `GoogleTasksClient` already returns `NeedsConsent` carrying Google's
own Intent, and the caller launches it and retries. Restraint is also visible in *which
call is made* (free/busy query vs events list), not as a filter applied after fetching.

**What it buys, and the rule that does not change to collect it.** `C9a` §4 made every
`BLOCK` need confirmation because the app was blind to Ido's day. That precondition is
now satisfiable — but the sheet does not vanish, and that is the improvement rather than
an oversight: a block in a slot **free on every shared calendar** is placed silently; a
block in a slot the app **cannot see** stays `PROVISIONAL` and joins the batch. **The
agent gets quieter exactly in proportion to what Ido chose to show it**, with nothing to
configure.

**`C9f`'s unchecked checkbox generalises.** Granular consent means a sign-in can grant
nothing (#36); with several calendar checkboxes, partial grants are the *normal* case.
**Spec line: every calendar feature degrades legibly and none gates the app.**

### 4 · The deadline, chosen rather than asked

Ido could not read the two options against each other and asked for the schematic
version, the best answer, and an improvement on it.

**Chosen: an all-day banner titled `Due 23:59 · Submit report`.** The deciding argument
is that **the Google event does not remind** — `C9a` §6 already put reminders in
GoalPilot's own local notification, fired at `deadline − minutesOf(task) − buffer`. So
the event has one job, to be *seen*, and a banner sits at the top of the day where a
23:59 marker sits below the fold. Option B's only advantage — the time in its natural
position — is an advantage over nothing, because nothing depends on that position.

**A third shape was ruled out before the question reached Ido**, and it is the one most
designs would pick: a short timed event ending at the deadline. It occupies a slot,
which is exactly what `C9a` §4's silent placement of a `DEADLINE` depends on it *not*
doing; and it collapses two rungs `C9a` separated by what a miss means. Derived from
committed decisions rather than asked.

**The improvement composes §3 and §4, and neither answer produces it alone:** a deadline
says *by when*, not *when you will do it* — and that gap is the honest reason someone
would want the timed event. With busy/free now available, the banner may be **paired
with a real `BLOCK`** in a genuinely free slot, sized by `minutesOf(task)`. The banner is
the obligation, the block is the plan, each missable for its own reason. Before §3 that
pairing cost a confirmation every time.

Two consequences recorded so nobody re-derives them: a **passed deadline does not move**
(history is never edited; `OVERDUE` lives in the app and reminds at the planning hour —
a fresh banner per late day would litter his real calendar with his own backlog), and a
**completed occurrence's event stays put, unmarked**.

### 5 · Derived and logged rather than asked

- **Pull** on app foreground behind the shipped per-uid `SharedPreferences` throttle
  (`SyncHealthDataUseCase`, `THROTTLE_MILLIS = 15 min`, `Manual` never throttled) — the
  pattern `C9d`'s research §6 already recommended against Calendar's 600/min/user ceiling.
- **Push is not throttled.** Health Connect throttles a *read*; a write must not make a
  calendar lag fifteen minutes behind something Ido just did.
- **Only confirmed occurrences reach Google** — `C9a` had already ruled `PROVISIONAL`
  in-app only.
- **Sign-out does not delete the calendar.** Ido is its data owner (`C9d`).
- **An account switch reads as *not mirrored*, not as events to patch** — stored
  `googleEventId`s are meaningless against another account.
- **Matching is by `googleEventId`, never by title or time**; the pull must be able to
  see deletions, which a range scan cannot. Which API mechanism supplies that
  (`syncToken` / `showDeleted` / `updatedMin`) **was not verified this session** and is
  flagged as build detail rather than asserted.
- **Titles are written but never read back** — a Google-side rename would silently
  replace a task title with no undo; the reverse failure (an annotation overwritten on
  the next time patch) is visible and harmless.
- **An event Ido creates by hand inside the GoalPilot calendar is left alone** — it has
  no task to be an occurrence of. Whether `C9b`'s surface *draws* it is that ticket's.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, `firestore.rules` or Cloud
Functions file was created or modified; this session produced Markdown and GitHub
issues only. Every layer this project has — server unit, endpoints, database rules,
client component, client page, UI E2E — is untouched by a decision ticket, and running
one would prove nothing about the decision.

Verification was structural instead:

- **Comment posts verified by re-reading counts**, not by trusting exit status — the
  failure `c7-what-is-a-unit` recorded, where `gh issue comment` posted nothing and
  reported no error. #27 → 3, #26 → 3, #28 → 2.
- **Map body hashed, re-fetched and byte-compared immediately before the write**
  (`f328d858…`, no drift with a live sibling), the edit proven a **pure insertion**
  (0 deleted lines, +2), then **read back and diffed against what was sent** — one
  trailing newline added by GitHub, **BOM intact** (`efbbbf`), no textual diff.
- **The frontier re-derived out of GitHub after closing**, not predicted.
- **Blocked-by re-queried live** for all 13 open children before claiming, and again
  after `C17` closed mid-session.

## 🧭 Singletons

**None taken.** No `#gradle-daemon`, neither AVD, no GROQ call, live `goalpilot-56e30`
never contacted. Leases: `SESSIONS.md` + `#git-index` for the claim commit,
`#gh-issue-12` for the map append — each held across the write and released.

## Session hygiene

- **Lease-blocked at the very first write, and waited rather than asking** (§5.2).
  `c9b-calendar-surface` held `SESSIONS.md`; a background watcher on the lock file cost
  two turns instead of a question, and the grilling ran in the gap — which is exactly
  the reorder the rule prescribes.
- **The board's *Unclaimed work* block was wrong in both directions and was corrected.**
  It offered #26 and #38 as takeable when both were assigned, and closed with *"nothing
  is claimed or in flight"* while the Active-claims table directly above it carried two
  live rows. Unowned and actively misdirecting, so refreshed — the same condition three
  earlier sessions used.
- **The frontier moved underneath this session.** `c17-many-to-many` closed #38 and
  released mid-flight, which **unblocked [#18 `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18)**
  — by a distance the highest-leverage ticket left, gating #19, #21, #22, #23 and #31
  directly and four more behind #19. Re-derived rather than predicted.
- **Two sessions in the same half of the map**, unlike the previous pair. #26 (surface)
  and #27 (semantics) are coupled, so the board note was widened to name the coupling
  before it could be discovered: a two-way sync gives #26's prototype foreign state to
  draw. Handed over by **comment on #26**, never by editing anything that ticket's
  session owns.
- **`C13` (#32) still has no line in the map's *Decisions so far* index** — raised by
  `c9a-schedule-a-task`, still unwritten. Not written here either: an index line written
  *for* another session is a report, not a claim. Ido's to assign.
- **`AUTO MODE` arrived mid-session** and governed the commits below. It did **not**
  change who answers the product questions — the three that were Ido's were still put to
  him, per the same precedence `c13-byo-api-key` recorded.
- **The pre-commit self-review paid for itself three times**, and all three were this
  session's own errors rather than anything it inherited:
  1. It had written *"no fog patch narrowed"* while the resolution itself argued that
     `C9c` §2 is where the `GoogleSignIn` migration is first felt — a claim contradicting
     its own output. Fixed by actually narrowing the patch on `#12`, which turned out to
     be the more interesting edit: incremental authorization makes the scope request **a
     recurring user-driven interaction**, not the single event `C9d` pictured, so the
     patch's *shape* changed and not only its size.
  2. The hand-off comment on `#26` called the daily-review batch the **third** reuse of
     the Google Tasks import idiom; `C9a` §5c had already counted its own as the third.
     Corrected in place to fourth, with the count spelled out so the next session need
     not re-derive it.
  3. `#31` was named as owing something in the resolution's hand-off table and had no
     comment — the resolution would have been the only record, and `#31`'s session reads
     its own ticket. Commented.

## KB candidates

**3 written, 2 drained at the `AUTO MODE` commit trigger, 1 parked.** Into the central
bundle `C:\Dev\JARVIS\kb`:

- 📥 **An OAuth scope is not a permission model** → `dev/google-oauth-scopes-and-consent.md`
  **§7, folded in place** (old §7 *Adjacent* → §8). Grants are per *scope*, never per
  *resource*, so the per-calendar control Ido asked for is a promise the client keeps —
  answered with **incremental authorization**, whose real payoff is that a user who
  never uses the capability gets a **guarantee** rather than a promise.
- 📥 **Two actions, one observable** → **new page**
  `dev/indistinguishable-at-the-boundary.md`. Preserve the superset and defer the
  disambiguation to a human, because both auto-behaviours are destructive in opposite
  directions and there is no safe middle.
- ⛔ **The fourth picker-axis failure mode (*granularity*)** — parked, always-ask in
  both modes, destination `rules/`. Not dropped: the candidate file was **rewritten
  down to this survivor**, original numbering kept, under `## Standing — always-ask`.

`Check-KbLinks` **CLEAN at 55 pages**; **nothing superseded** — both writes additive.
A row was claimed on the **JARVIS board** as well, since the board follows the repo
being written to, and the ingest half has its own changelog there
(`CHANGELOG/2026-08-10/c9c-calendar-sync-ingest.md`, commit `526d2b9`).

**Two things the ingest found by checking rather than trusting.** Entry 1's bundle
check was **present and wrong** — §4 of the OAuth page already carried
*"request the scope at first use of the feature that needs it"*, written the same day
by a sibling drain — so the entry landed as a **refinement of §4** rather than as the
original claim it proposed. And the parked entry described
`rules/question-axis-naming.md` as an uncommitted draft; the JARVIS board shows it
**shipped and in force**, so it is an **amendment to a live rule**, which raises what
it asks of Ido rather than lowering it. Corrected in the candidate file itself.
