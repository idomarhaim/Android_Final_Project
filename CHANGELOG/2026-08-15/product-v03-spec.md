# product-v03-spec — the map's destination artifact, written while the map reopened under it

**Session:** `product-v03-spec` · **Date:** 2026-08-15 · **Mode:** `AUTO MODE`
**Branch:** `feat/goalpilot-implementation` · **Brief:** [`sessions/product-v03-spec.md`](../../sessions/product-v03-spec.md) · **Map:** [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)
**Claim:** `e416d61` · **Board row:** `product-v03-spec`, Active claims

## What shipped

**[`docs/PRODUCT_v0.3.md`](../../docs/PRODUCT_v0.3.md)** — 1,660 lines, ~107 KB, written from `#12`'s
**27 closed decisions**, the two source briefs (`R1`–`R28`, `E1`–`E19`), and the five prototypes that
are the design of record for the screens.

Spine: **0** the rules that bind everything · **1** the model · **2** scheduling and the calendar ·
**3** AI · **4** screens and presentation · **5** localization, derived state, offline · **6**
challenges · **7** data shape and migration · **8** out of scope · **9** fog · **10** gaps and
defects · **11** traceability.

**Every section header carries the ticket that decided it, and §11 is a completeness check** — one
row per decision, mapped to the section that carries it.

## The two findings that matter more than the file

### 1 · The brief's premise had rotted, and half its `Exit` is blocked — not deferred

The brief was written 2026-08-13 on *"map `#12` has no open tickets left … `#12`'s *Decisions so far*
holds **26** lines"*. Neither half still held:

- `c20-derived-state` took the map to **27** decisions on 2026-08-14/15.
- **`c21-offline-story` is live in this same working tree** — its Active-claims row was written into
  `SESSIONS.md` at **13:58:51**, three minutes before this session opened, and was still uncommitted.
  It holds **`#12`'s body as a singleton** and is *graduating fog into a new child ticket*.
- That ticket now exists: **[`C21` #43](https://github.com/idomarhaim/Android_Final_Project/issues/43)**,
  open, confirmed by `gh issue list`.

So the brief's `Exit` — *"`#12`'s body updated to say the destination is reached, and `#12` closed"* —
**cannot be satisfied by this session at all**, and not merely because a sibling holds the singleton:
`#12`'s own destination reads *"the map is done when the spec is whole **and no ticket is open**"*,
and a ticket is open. **`#12` was therefore read-only here.** Derived per the board's singleton rule
rather than asked, and recorded on the board before the first write.

**The consequence is written into the spec rather than hidden:** the file opens with a caveat block
saying it is one decision short by construction, [§5.3](../../docs/PRODUCT_v0.3.md) names `#43` as the
section that cannot be finished, and §11's 28th row is `C21` marked **OPEN**.

### 2 · Two decisions the map believes it took and did not

Both are **`⚠️ GAP`** in §10 and neither was decided here — *no reopening closed decisions* means a
new ticket or Ido's call, not a quiet fix.

**§10.1 — the measure proposal has no schema.** `C7` handed it on verbatim: *"This is a **fifth AI
feature** for `C11b` #30 to write an output format for."* `C11b` §3 writes **four** schemas —
`estimate`, `plan`, `daily`, `classify` — and none of them is it. `C11b` reaches *"five"* by a
**different** route: it counts `classify` as the fifth feature the map never named, because `C2`'s
task typing folded into `estimate` as a field. **Two tickets each say "fifth" about a different
feature, and `C7`'s never got written** — leaving a specced AI feature with no wire format and no
stated fallback, which
[§0.1](../../docs/PRODUCT_v0.3.md) requires it to have.

`Observed:` `#14`'s resolution carries the handoff (one grep match); `#30`'s full 31 KB resolution
body defines four schemas and mentions `C7` twice, neither about a measure proposal. `Inferred:` an
omission rather than a deliberate absorption — nothing in `#30` says it was considered and dropped.
`Untested:` whether Ido would rather fold it into `estimate` than give it its own call.

**§10.2 — `GoalCategory`'s fate was routed to `C5`, and `C5` did not decide it.** `C2` ruled it out of
scope for itself and posted it to [`C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21)
as a goal-model question, carrying a third instance of a class `C15` had filed twice (hardcoded English
labels in `domain/model/`). **`C5`'s resolution does not mention `GoalCategory`.** What *is* decided is
listed in §10.2 so the build session is not blocked; what is not is whether the enum stays closed at
ten, and where its labels live once translated.

**§10.3 — three settings this spec requires that do not exist:** week start (`C15`), daily planning
hour and waking hours (`C9a` §6). Each was named by the ticket that needed it and none was filed.
Build work with an obvious shape, but named here rather than discovered mid-build.

## 🧪 Tests

**No test layer applies, and this is stated explicitly rather than skipped.** The deliverable is a
Markdown specification: there is no server unit, server integration, endpoint, database, client
component, client page or UI E2E layer that can execute it. The `#12` standing preference *plan, don't
do* forbids this session touching `app/src/`, so nothing was built that could be tested.

**What was verified mechanically instead**, since a 107 KB document with 50 internal cross-references
has a real failure mode of its own:

| Check | Result |
|---|---|
| Internal anchors (`](#…)`) resolve to a real header | **50/50 OK** — 34 were broken on first write and were repaired by computing GitHub's slug rule over the actual headers rather than by eye |
| Relative file links exist on disk | **8/8 OK** — both source briefs, all five prototype READMEs, `docs/research/` |
| Duplicated headers (a write-script accident) | **0** of 71 |
| `#12` decision count vs §11 rows | 27 closed → 27 rows, + 1 open (`C21`) |
| `#43` exists and is open | confirmed via `gh issue list` |

The anchor repair is worth naming: **the first pass wrote 34 anchors that looked right and were
wrong** — `###` headers slug as `11-title` (a dot) while `##` headers slug as `1--title` (a middle
dot), and no amount of reading catches that. It is the same shape as the map's own repeated lesson
about looking at the rendered output rather than the source.

## Pre-commit self-review — the three questions

- **Which factual claim did I not verify?** The `§10.1` gap was the load-bearing one, so it was
  escalated from a grep to reading `#14`'s resolution comment, which produced the verbatim handoff
  quote now in the spec. Two remaining inferences are **hedged in the document itself** rather than
  dropped or asserted: `declaredBy`'s absence-means-milestone encoding (`C16` fixes the field's role
  and its three values, not the null case), and the **field names** in §7.1 — a note now says which
  names are the tickets' own and normative (`declaredBy`, `goalEdges`, `parentIds`, `pausedUntil`) and
  which are this file's, chosen for readability.
- **Which of my own arguments does my output contradict?** One, found and fixed: §11 claimed *"27
  closed decisions, 27 rows"* while its table has **28** rows — the 28th being `C21`, which the same
  file's opening caveat spends a paragraph insisting is **open**. The sentence now says so.
- **Which open ticket names this one?** [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)
  (the map, whose destination this is) and
  [#43](https://github.com/idomarhaim/Android_Final_Project/issues/43) (`C21`, which §5.3 cannot
  finish without).

## Board

`SESSIONS.md` was read before the first edit and the row written before the first write, per
`/kickoff` §3. **The claim commit `e416d61` carries `c21-offline-story`'s own Active-claims row and
its `c20`-liveness note** — that session wrote them into `SESSIONS.md` and had not committed them, a
pathspec commit takes the **working-tree** content of the path, and `SESSIONS.md` is the one file both
sessions write. It cannot be excluded; **naming it is the prescribed repair** and it is named in that
commit's message as well as here.

`git diff -- SESSIONS.md` was run in its own tool call before each commit.

## `kb-candidates/`

Listed before the first unit of work, per the session-start duty. **Five files, all partly drained,
and every surviving entry is ⛔ always-ask in both modes** — so `AUTO MODE` drains none of them and
this session correctly leaves them:

| File | Survivor | Why it is blocked |
|---|---|---|
| `2026-08-12-c12-charts-presentation.md` | entries 1–4 as one group | entry 1 is `rules/`-shaped; 2–4 belong in the page it would create |
| `2026-08-13-c11b-output-formats.md` | entry 1 | `rules/`-shaped — rewrites committed text |
| `2026-08-13-c15b-stored-ai-text.md` | entry 2 | parked |
| `2026-08-13-c2-task-type.md` | entry 2 | parked |
| `2026-08-13-session-titles.md` | entry 4 | always-ask in both modes |

**⚠️ Two of those survivors look like they have since shipped into the global rules** and would then
be drainable: `c11b`'s entry 1 (*exposure opens when content reaches the working tree, not at
`git add`*) and `session-titles`' entry 4 (*a sibling's liveness lives in its transcript*) both read
as claims now present in `my-rules.instructions.md`. `Observed:` the wording resembles committed rule
text. `Untested:` whether the shipped clauses actually cover these entries — that needs a read of the
canonical `C:\Dev\JARVIS\user-rules\` file against each entry, which is a JARVIS-repo visit this
session did not make. **Flagged for the next session rather than acted on**, since draining is a
deletion and this one is not established.

## Files

**Read:** `sessions/product-v03-spec.md` · `SESSIONS.md` · `AGENTS.md` · `#12` body (110 KB, fetched
to a file) · `#30` and `#14` resolution comments · `Product and UX Reviews/2026-08-06-brief-review.md`
· `Product and UX Reviews/2026-08-09-entity-model-brief.md` · all five `docs/prototypes/*/README.md` ·
`kb-candidates/*.md` (5) · `CHANGELOG/2026-08-15/c11b-output-formats.md`

**Written:** `docs/PRODUCT_v0.3.md` *(new)* · this file *(new)* · `SESSIONS.md` (own row + two notes)
· `sessions/product-v03-spec.md` (`status: active`, `mode` corrected to `AUTO MODE`)

**Not touched:** `app/src/` (the brief's *no code* boundary) · `#12`'s body (`c21`'s singleton) ·
`kb-candidates/` (nothing drainable)
