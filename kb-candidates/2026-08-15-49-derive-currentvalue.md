# KB candidates — `49-derive-currentvalue`, 2026-08-15

Session: `/implement #49` — `goal.currentValue` stops being a stored aggregate and
becomes a sum over facts. Mode: `AUTO MODE`.

**Each entry stands alone.** No transcript is a source: everything needed to write the
page is below, including what was rejected and why.

---

## 1 · A non-atomic write pair is repaired by deleting a write, not by adding a transaction

**Claim.** When two writes have to agree and a crash between them corrupts state, the
reflex is to wrap them in a transaction. Check first whether **one of them is derivable
from the other** — because if it is, the repair is to *delete* it, and that repair is
strictly stronger. A transaction narrows the window; deleting the second write removes the
second number, and with no second number there is nothing left to disagree. The failure
mode stops being rare and starts being **unrepresentable**.

**Why.** [#49](https://github.com/idomarhaim/Android_Final_Project/issues/49):
`ProgressRepositoryImpl.logProgress` wrote a progress entry, then called
`GoalRepository.addProgress` to advance `goal.currentValue`. A crash in between left the
entry recorded and the counter not — permanently, because nothing reconciled the pair, and
compounding, because every later log added to the wrong base.

A Firestore transaction over both would have been the obvious fix and would have been
worse in three separate ways:

1. **It does not cover the second corruption path**, which needs no crash at all. The
   `catch` reported `Resource.Error` *after* the entry was already committed, so the user
   was told the log failed and logged it again — two entries, one counter movement, the
   numbers now disagreeing in the *other* direction. A transaction makes the report
   truthful only if the entry write is inside it, which is the whole design changing
   anyway.
2. **It leaves the second writer standing.** The same field was mutated from
   `TaskRepositoryImpl.setDone` on a completely different path. Two atomic writers of one
   derived field are still two writers.
3. **It preserves the thing that made the bug invisible** — a stored number that *can*
   disagree with the facts, on a screen where the two are never rendered together.

Deleting the counter cost one pure function (`GoalProgress`, sum over progress entries plus
completed tasks' `progressContribution`), removed a document from a transaction's read set,
and needed **no migration**: an untouched goal sums to `0.0`, which is what its stored field
already defaulted to, so existing documents read identically on day one and the corrupted
ones self-heal because the bad number simply stops being consulted.

**The test that decides it, and it is cheap:** *is the second number readable by anyone who
cannot read its inputs?* In Firestore that is a `firestore.rules` question, not a taste
question — `users/{uid}/goals` is read under `isOwner(uid)`, and the owner can read the
entries too, so the reader **is** the writer and no stored copy is owed to anybody.

**Rejected on the way:** (a) making the task tick *emit a progress entry* instead of
mutating the counter — this re-creates the defect one layer down (a second write that must
agree with the first, plus an untick that has to find and undo it), where summing over
`isDone` is idempotent structurally; (b) `FieldValue.increment` — `increment` *is* the
accumulator, so it makes the write atomic while keeping the number that can drift.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — a new page, `derived-state-repairs.md`, or
folded into the existing derive-don't-store material if a page already carries it.
**Anchors.** `docs/PRODUCT_v0.3.md` §5.2 (*who owns a derived number*), §0.2, §0.3;
`ProgressRepositoryImpl.kt`, `TaskRepositoryImpl.kt`, `domain/model/GoalProgress.kt`.
**Supersedes.** Nothing.
**Status.** Ingestable in `AUTO MODE` — destination is `kb/`, not `rules/`, and it
contradicts no standing claim.

---

## 2 · Deriving across a Firestore **subcollection** costs N listeners, and the cheap alternative is a migration in disguise

**Claim.** "Stop storing it, derive it" is cheap when the inputs live in **one** collection
and expensive when they live in a **subcollection per parent**. A collection-group query —
the obvious way to read them all at once — is not a drop-in: it needs its own
`match /{path=**}/<name>/{id}` rule, and that rule **cannot extract the owner uid from the
path**, so binding it to a user means denormalising `uid` onto every document, which means
a backfill. A ticket that promises *"no migration needed"* has therefore already chosen
N per-parent listeners, whether or not anyone noticed.

**Why.** #49 derives `goal.currentValue` from `users/{uid}/goals/{goalId}/progressEntries`.
Reading every entry for a user in one listener needs `collectionGroup("progressEntries")`,
which needs a rule matching that collection group at any depth; inside such a rule the path
segments are not addressable, so the only working form is
`allow read: if request.auth.uid == resource.data.uid` plus a `whereEqualTo("uid", uid)`
filter — a new field on every existing entry document. The tasks half has no such problem
because tasks are already a flat `users/{uid}/tasks`.

**What this predicts, and it is the useful half:** the storage shape decides how expensive
*derive-don't-store* is later. A fact you may one day want to aggregate across parents wants
to be flat and carry its parent's id, not nested. The nesting reads better and bills the
difference at the first derivation.

**Rejected on the way:** (a) collection group + `uid` field + backfill — contradicts the
ticket's explicit *no backfill*, and adds a denormalised field to guard a query that a rule
should be guarding; (b) moving `progressEntries` to a flat `users/{uid}/progressEntries` —
the same migration wearing a different hat, and `PRODUCT_v0.3` §7.1 keeps the collection
nested while extending it; (c) a non-reactive per-goal fetch — the goal list is a live
snapshot flow, and a one-shot sum inside it would go stale on the next entry.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — the Firestore page, if one exists; otherwise
`firestore-shape-and-derivation.md`.
**Anchors.** `data/firestore/GoalRepositoryImpl.kt`, `firestore.rules`,
`docs/PRODUCT_v0.3.md` §7.1.
**Supersedes.** Nothing.
**Status.** Ingestable in `AUTO MODE`.
