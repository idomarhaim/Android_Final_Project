# c19-area-success-failure — claiming #41 · `C19`, and a hole in the frontier instrument

**Session:** `c19-area-success-failure` · **Date:** 2026-08-13 · **Mode:** `AUTO MODE`
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#41 · `C19`](https://github.com/idomarhaim/Android_Final_Project/issues/41)
**Invoked as:** `/wayfinder 12` — the **map**, no ticket named, so the pick was the agent's.

## The claim

**[#41 · `C19` — *Per-life-area success and failure: what counts, and what does the view show?*](https://github.com/idomarhaim/Android_Final_Project/issues/41)**,
assigned to `idomarhaim` on GitHub **before any other work**. Per the wayfinder skill the assignee
*is* the claim, so the claim does not depend on this commit.

## Frontier derivation, and the hole it closed in the previous session's rule

`c15b-stored-ai-text` discovered an hour ago that `/issues/12/sub_issues` serves a **stale `state`**
— it reported `#21` open about sixty seconds after that ticket closed — and left the rule *"never
read `state` off the aggregate endpoint; confirm every open child directly."*

**That rule is right and incomplete.** Confirming every child the listing calls **open** catches a
*closed* ticket reported open — the case it was written from. It cannot catch an **open** child
reported **closed**, because a child the listing calls closed is never queried at all, and *that*
direction is the dangerous one: it drops a takeable ticket out of the frontier and nothing in the
output says so.

The cheap cross-check that covers it is a **collection-wide authoritative query**:

```
gh api …/issues/12/sub_issues   → 26 children, 23 closed, 3 open
gh issue list --state open      → 15 open issues in the repo
```

The three open children (`#30`, `#35`, `#41`) plus the map `#12` itself plus eleven non-map issues
(`#2`–`#11`, `#34`, `#36`) = **15 — every open issue in the repo accounted for**, so nothing is
hiding behind a stale `closed`. Each of the three was then confirmed directly and queried for
`blocked_by`. State true at **01:12 local**.

| Ticket | Blocked by | Assignee | Verdict |
|---|---|---|---|
| `#41 · C19` | *(none — filed with no blocking edge)* | `idomarhaim` | **CLAIMED** |
| `#30 · C11b` | `#19` ✅ `#20` ✅ `#24` ✅ `#29` ✅ | — | declined |
| `#35 · C15b` | `#24` ✅ `#29` ✅ | `idomarhaim` | live sibling's claim |

**Third consecutive derivation with no blocked ticket on the map at all** — so leverage
discriminates nothing between the candidates, and `#41` closing unblocks nothing.

## Why `#41`, and why the objection that refused it seven minutes ago expired

`c15b-stored-ai-text` declined `#41` on **freshness**: created `22:02:41Z`, sixty-one seconds before
its derivation ran, by a session then mid-release, its central input *"a resolution published one
minute ago and read by nothing."*

Checked against the map body rather than assumed:

- `C5`'s line **is** in `#12`'s *Decisions so far* (23 decision lines now, up from 22).
- The `E4` success/failure patch is **gone from *Not yet specified*** (4 fog bullets, down from 6) —
  `c5-endless-goals` graduated it into this ticket and retired the Firestore-migration fog with it.
- `c5-endless-goals` has since **released**.

So the input is closed, indexed, released, and **read before claiming** — §1–§4 of the resolution
comment on `#21`, which is this ticket's charter: an endless or maintenance goal **can fail, per
window and never as a whole**, the window run `● ● ● ● ○ ○` is the record, and attainment does not
decay. None of the four grounds this board has carried against neighbouring tickets applies: `#41`
has **no blockers at all**.

**`#30 · C11b` declined** — terminal by design (*"you cannot test a format nobody has designed
yet"*), and the ground has hardened rather than expired: `#30` writes one output schema per AI
feature, `C15` explicitly left it *"the per-feature veto where the model's Hebrew is not good
enough"*, and that is exactly what live `#35 · C15b` is deciding right now.

## Couplings named on claiming

1. **`#12` is a commons** — race fired three times (`c3`, `c1`, `c2-task-type`): re-fetch, `cmp`,
   append one line, verify a pure insertion. And `--method PATCH -f body="$(cat …)"` **cannot write
   this map** (~100 KB → `Argument list too long`, *after* it looks like it worked): use
   `--input <file.json>` and verify the round-trip.
2. **`C5` §4 is an input, never a subject** — including its refusal of any number that moves on
   wall-clock time, which a "failures age out" window could quietly reintroduce.
3. **`C9a`'s vocabulary is half warning** — `MISSED` is a failure, `OVERDUE` is *late but still
   owed* and is **not**, `EXPIRED` counts for nothing. Conflating them overstates Ido's failures,
   which is the opposite of this view's purpose.
4. **`C17`'s asymmetry is deliberate** — a success counts **in full in both** areas; minutes divide.
5. **`C10` already solved the tone problem at identical cost** — one of three slots reserved for a
   goal in a good state. Plus `C12`'s material contract and the map's Hebrew/RTL rule for any screen.

## Staging: the board collided, and then un-collided

`c5-endless-goals`'s release note (67 lines) landed in `SESSIONS.md` **while this session's row was
being written**, and `git add` is per-file — so staging the board would have swept their release into
this commit. This session had written the two-files-only commit and its justification when **`ea6ff78`
committed their note ~90 seconds later**. `SESSIONS.md` then held **119 added lines, 0 removed, all
this session's** (checked, not assumed), so it is staged normally.

Their commit also carried `kb-candidates/2026-08-10-c16-milestone-model.md`,
`…-c9e-event-lifecycle.md` and `…-c8-ai-task-plans.md` — the three files `picker-queue-merge` owns —
which is the same contamination `c5-endless-goals` recorded suffering from `picker-queue-merge` an
hour earlier, now travelling the other way. **Not adjudicated here; it is those two sessions' to
settle.** What it demonstrates is worth stating precisely: **explicit-path staging cannot prevent
this half of the problem.** The rule stops *you* sweeping a sibling's work in; it has nothing to say
about a sibling sweeping *yours*, and two sessions editing one commons file collide however narrowly
either stages. Three instances tonight.

Files this session did not touch, all owned by live siblings: the three `picker-queue-merge`
candidate files above, and `kb-candidates/2026-08-13-c5-endless-goals.md`.

## 🧪 Tests

**None, and none applicable.** This unit is Markdown plus GitHub API calls — no layer of the
project's test pyramid (server unit, integration, endpoints, database, client component, client
page, UI E2E) is touched, and `#12`'s standing preference is **plan, don't do**: no ticket on this
map ships code. The frontier derivation was verified instead by the reconciliation above, which is
what a claim can be checked against.

## 📥 KB candidates

One filed, none drained: [`kb-candidates/2026-08-13-c19-area-success-failure.md`](../../kb-candidates/2026-08-13-c19-area-success-failure.md)
— the both-directions staleness finding. 🟢 ordinary and genuinely this session's, but held on a
cross-repo hold into the live `C:\Dev\JARVIS`, and it is the **third** face of one claim already
parked twice (`c2-task-type` entry 1, `c15b`) — all three belong in one section of
`kb/dev/runtime-verification.md`, not three raced writes.

## Singletons

None taken: no Gradle daemon, no build, no device or emulator, no Firebase, nothing written in
`C:\Dev\JARVIS`. **Ido's attention** is the one contended resource and is named on the board row —
`#41` is `wayfinder:prototype`, so HITL across revisions, while `c15b-stored-ai-text` holds a live
grilling and `picker-queue-merge` owes him a 🎬 offer.

---

## Resolution work — revision 4 of the prototype

`#41` carries `wayfinder:prototype`, so the ticket resolves through a concrete artifact Ido reacts
to, not through prose. Built to this repo's convention:
[`docs/prototypes/2026-08-13-area-success-failure/`](../../docs/prototypes/2026-08-13-area-success-failure/)
— four materials × two themes × two languages, switchable by query string so `shoot.ps1` renders any
state. Frames: **Health** (good shape), **Career** (bad shape — the frame that tests the tone rule),
**Analytics** (the counterpart to the time donut), **Learning** (an area where almost nothing was
ever scheduled).

**Three proposals embodied and derived, each from a closed ticket, each Ido's to overturn:**

1. **A never-scheduled goal is `asleep`, not failed** — `C9a`'s *you cannot fail to do something you
   never agreed to*. Visible, named, and in neither number, with the exclusion stated on screen.
2. **Nothing ages out; the window is a query** — `C5` §2 refused any value that moves on wall-clock
   time, and "failures older than N weeks stop counting" is that value in a different hat. History is
   permanent (`C5` §4), the view reports over `30 days · 8 weeks · 6 months`, and there is no lifetime
   failure counter anywhere.
3. **Both placements, one component** — the area screen because the subject is per-area, analytics
   because `C17` deliberately put the divided number (minutes) beside the undivided one (successes).

**What a window is** — the ticket's headline question — is answered on the screen, under the run:
*a window counts as kept when everything due in it was done*.

**Left for Ido, and it is the only question that is his:** should an abandoned goal be `asleep` (as
drawn), invisible, or a failure? Not derivable — `C9a` covers only work the *app* proposed.

## 🧪 Tests — round two

Still no project test layer in play (Markdown, HTML, GitHub). The acceptance criterion here is
**visual**, so the check is `shoot.ps1` + look, per the tooling `C12` built: **five rounds, nine
defects, seven of which were invisible in the source.** Renders inspected: glass/dark/en,
neo/light/he, darkneo/dark/he, and a probe close-up of liquid/dark/he (the tightest case — Hebrew
wraps wider and liquid's gradient runs hottest under the lowest rows).

The one worth carrying forward: **an unbalanced `</div>` I introduced in round 3** made every caption
escape its phone frame and the stage wrap into two rows. The **Hebrew** render exposed it — so the
"seen in Hebrew" rule caught a *structural* defect, which is not what it was written for. Full list
in the prototype's README.

## Process slip, recorded

`docs/prototypes/2026-08-13-area-success-failure/` was **written before it was added to this
session's claim**. Nobody holds `docs/prototypes/`, so nothing collided — but the rule is
claim-before-write, and this was the reverse. The path is now in the row and the order is on the
board.

**`#41` is not resolved:** a `wayfinder:prototype` ticket is HITL and the agent may not answer Ido's
side of it. No resolution comment posted, `#12` untouched, no index line owed until the ticket closes.
