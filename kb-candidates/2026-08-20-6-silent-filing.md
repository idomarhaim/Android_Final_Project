# KB candidates — `6-silent-filing`, 2026-08-20

Session: `6-silent-filing` · issue [#6](https://github.com/idomarhaim/Android_Final_Project/issues/6) ·
mode `AUTO MODE` (brief front matter, Ido's standing instruction of 2026-08-17).
Account: [`CHANGELOG/2026-08-20/6-silent-filing.md`](../CHANGELOG/2026-08-20/6-silent-filing.md).

---

## 1 · An ordering constraint's **stated reason** is its scope — adjacency binds concurrent sessions, merit binds everyone

**Claim.** When a planning pass records *"A runs before B"* across a set of tickets, the constraints
it produces are not one kind of thing. Two kinds show up, and they have **different scopes**:

| Kind | Example reason | Binds |
|---|---|---|
| **merit** | *"B built first is built on a value A is about to redefine"* | **everyone** — sequential runs included |
| **adjacency** | *"both edit `X.kt`; adjacent functions, one file"* | only sessions running **at the same time** |

An adjacency constraint is a **conflict** statement: it says two write sets overlap. Run
sequentially, whoever goes second reads the file as it then stands and there is no conflict to
have. A merit constraint is a statement about **meaning** — one ticket changes what a value *is* —
and no amount of re-reading rescues the session that went first.

**So a session that opens on a brief carrying an ordering block must read the block's *reason*, not
only its verb.** Both kinds are written in the same imperative — *"Runs AFTER X. Verified."* — and
the verb is identical whether the cost of disobeying is a merge or a rewrite.

**Observed** 2026-08-20, GoalPilot. `613b454` recorded four constraints in one commit, each with its
reason spelled out. `9-duration-box → 7-quickadd-complete` carries a merit clause;
`7-quickadd-complete → 6-silent-filing` carries *"Same file, adjacent functions"* and nothing else.
`/kickoff 6-silent-filing` opened with `#7` unrun and the board empty. Obeying the verb would have
ended the session with nothing built; reading the reason, checking that no session held the file,
and then checking the substance (`#7`'s own brief puts its affordance in the *other* function, and
`#6`'s deletion **forces** that placement rather than removing it) let it ship. Recorded as the
session's own decision in `7d60e90` per `rules/derivable-decision.md`.

**Why (and what was rejected).** *Obey the verb, always* was rejected because it converts a merge
cost into a session cost — and there is no session to hand it to when the blocking ticket is also
unclaimed. *Ignore adjacency constraints when running alone* was rejected as too wide: the check is
**is anyone holding this file right now**, which is a board read, not an assumption. What survives
is narrower than both: an adjacency constraint is discharged by the board, a merit constraint is
not discharged by anything but the other ticket landing.

**Related, and this is the same family one level up:** `48e94bc` (2026-08-20) found that a
disjointness check run at **symbol** granularity is not a conflict test, because it asks about the
code as it stands while a conflict is about the edits each ticket is about to make. That is a
finding about the **width** of a conflict check; this is a finding about the **scope** of the
constraint the check produces. Both live wherever the conflict-map material already lives.

- **Destination:** `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` (a new section beside §12/§12a),
  or `kb/dev/agent-topology-and-routing.md` if the maintainer reads it as a topology fact.
- **Anchors:** `613b454`, `48e94bc`, `7d60e90` (GoalPilot).
- **Supersedes:** nothing. It **narrows** nothing either — it adds a reading rule to constraints
  the charting pass already emits.
- **Status:** ready to ingest.

---

## 2 · Absence cannot carry two meanings — a migration backfill **consumes** the null, and anything else that needed it must be given a wire sentinel

**Claim.** Two design rules that each assign meaning to a field's **absence** cannot both hold, and
the collision is invisible until one of them is exercised. The common shape:

- a schema rule says *absent ⇒ this state* (here: *absent `declaredBy` ⇒ the object is
  instrumental*);
- a **migration** rule says *backfill absent documents to V* (here: *absent ⇒ `UNKNOWN`, because
  nothing records who made the existing rows*).

The second is not optional and it fires on **every** pre-existing document, so it takes the null.
The first state then has no encoding, and — critically — **the code still compiles and the happy
path still works.** What breaks is only the transition *into* that state: the user performs it, the
write succeeds, and the next read silently returns the migration's value instead. A deliberate act
undoes itself with no error anywhere.

**The remedy is a wire sentinel, and it is not the fourth enum value the design rejected.** A
domain that says *instrumental* with a null keeps exactly its declared values plus null; storage
needs one extra string whose only job is to mean *written, as none* — distinct from *never
written*. The distinction is between the **domain's** vocabulary and the **wire's**, and conflating
them is what makes the sentinel look like a violation of the rule that rejected a `NONE` constant.

**Two tells, one before and one after.**

- **Before:** the spec text that asserts *absent ⇒ X* is marked `Inferred:` rather than `Observed:`.
  In GoalPilot's §1.1 it was, verbatim, and the hedge was accurate — it is the file's own inference
  from a ticket that fixed the field's three values but never ruled on absence. **A claim-provenance
  hedge is a map of where a spec will fail**, which is a use for it nobody wrote it down for.
- **After:** the read mapper is written `Enum.fromName(stored) ?: DEFAULT`, and that elvis fires for
  **both** absences — the field being null and the sentinel not matching an enum name. The two cases
  have to be split *before* the enum is consulted. Caught here by a test written specifically for
  the round trip, on its first run; a test that only checked the three declared values would have
  been green.

**Observed** 2026-08-20, GoalPilot `#6`. `PRODUCT_v0.3.md` §1.1 (*absent ⇒ purely instrumental*)
against §7.1 (*backfill `UNKNOWN`*). The failure it would have shipped: a goal the user has just
marked *not a goal* reads back as a goal on the next Firestore snapshot, silently, with nothing in
the UI to explain it — the "lossless demotion" undoing itself.

**Why (and what was rejected).** *Add a fourth enum value* was rejected by the spec itself as a
stored judgement duplicating a null. *Run a real backfill write over the collection* was rejected as
disproportionate and, in this case, unnecessary: nothing in the app could create the instrumental
state yet, which is checkable by grep and was. *Delete the field on demotion* is the trap in its
purest form — it writes the exact absence the migration reads as the other value.

- **Destination:** `C:\Dev\JARVIS\kb\dev\firestore-write-semantics.md` as a new section (it is a
  read/write-semantics fact with a Firestore-specific edge — `FieldValue.delete()` is the tempting
  wrong move), **or** a small page of its own if the maintainer reads it as schema-design rather
  than Firestore. Cross-links to `kb/dev/claim-provenance`-adjacent material for the `Inferred:`
  tell.
- **Anchors:** GoalPilot `GoalDto.declaredBy`, `GoalDeclaredByMigrationTest`,
  `docs/PRODUCT_v0.3.md` §1.1 / §7.1.
- **Supersedes:** nothing.
- **Status:** ready to ingest.

---

## 3 · A whole-response fallback must not fabricate the field the caller **branches** on

**Claim.** When a failure contract says *transport failure ⇒ whole-response fallback, silently*, and
the response schema has a field the **caller picks a branch on**, a server-side fallback that fills
that field in has not degraded gracefully — it has **chosen a branch**, on every failure, forever.
And it has chosen the branch that fires when the field is absent, which in a well-designed contract
is the *unusual* one, because the usual one is what a successful answer produces.

The shape is easy to miss because each half looks correct in isolation. Returning a neutral object
rather than throwing reads as robustness. The branch that keys on absence reads as a correct
implementation of the spec. Only the composition is wrong, and it is wrong in the direction nobody
tests: it is exercised precisely when the network is down, which is when nobody is watching.

**It is also a duplicated fallback.** If the client already has a local fallback for the same
failure — and a client that must work offline always does — then the server's neutral object is a
second implementation of it, one the client **cannot tell apart from a real answer**. Throwing
restores the distinction for free.

**Observed** 2026-08-20, GoalPilot `functions/src/index.ts` `classifyTask`. Its catch returned a
200 carrying `suggestedGoalId: null` plus a title sliced from the task. Spec §3.4 makes an absent
`suggestedGoalId` the **one branch that speaks** — the app announces that no goal fit — so a dead
network made the app announce a new goal, every time. Replaced with a rethrow; the client's own
keyword-matching fallback runs.

**Test design worth carrying with it.** The emulator case that guards this **branches on the HTTP
status and asserts something real in both directions**: a 200 must satisfy the validation
invariants, a non-200 must carry an error and no result body. That makes a test which calls a live
model deterministic without stubbing it — an outage exercises the other half of the same change
rather than turning the suite red. (The invariant it asserts on the 200 branch is chosen the same
way: send an **empty** membership list, so *"no id may be echoed"* holds whatever the model says.)

- **Destination:** `C:\Dev\JARVIS\kb\dev\` — a section on an LLM-proxy / failure-contract page if
  one exists, else a new page. The general form is about failure contracts, not about LLMs.
- **Anchors:** GoalPilot `functions/src/index.ts`, `functions/test/triggers.emulator.mjs`,
  `docs/PRODUCT_v0.3.md` §3.4.
- **Supersedes:** nothing.
- **Status:** ready to ingest.
