# KB candidates — `d2-life-area-route`, 2026-08-15

Session: `/implement #2` — the route from a life area into its goals, plus the
`lifeAreaId` → `lifeAreaIds` plural move. Mode: `AUTO MODE`.

**Each entry stands alone.** No transcript is a source: everything needed to write the
page is below, including what was rejected and why.

---

## 1 · Making a one-to-many edge plural is a **list-rendering** change before it is a data change

**Claim.** When a `1:N` field becomes `N:M`, the defect that actually ships is not in the
migration, the queries or the arithmetic — those are the parts you are looking at. It is in
every **keyed list** that renders the entity grouped by the other end of the edge, because
a keyed list assumes each item appears once and that assumption was previously guaranteed
by the cardinality. So: **before touching the field, grep for the keyed lists, not for the
field.**

**Why.** `#2` took `Goal.lifeAreaId: String?` → `lifeAreaIds: List<String>`. The migration
got a mapper with a read-side backfill, a write that clears the legacy field, a two-query
delete path, and eight tests. The arithmetic got a remainder-exact split and four tests.
All of that was the visible work and all of it was fine.

The **crash** was in `GoalsScreen.kt`, untouched by the ticket's own "Where" section:

```kotlin
state.groups.forEach { group ->
    items(group.goals, key = { it.id }) { … }   // ← two bands, one goal, one key
}
```

Compose requires `LazyColumn` keys to be unique across the **whole list**, not per
`items()` call. One goal in two life areas → the same key twice → `IllegalArgumentException`
on the app's main Goals tab. The affordance that makes it reachable — multi-select area
chips — was added by the *same* diff, so the change shipped both the hazard and its
trigger.

**What makes it hard to see:** the cardinality was doing load-bearing work that no line of
code states. `key = { it.id }` is correct under `1:N` and wrong under `N:M`, and it reads
identically in both worlds. Nothing in the diff of the model, the DTO, the repository or
the use case points at it. The grep that finds it is `key = {` over the feature packages,
run *because the cardinality changed*.

**Rejected on the way:** (a) de-duplicating the goal so it appears in one band only — that
throws away the plural edge's entire point, which the spec states as *"a success counts in
full in every area"*; (b) treating it as a `GoalsScreen` bug rather than a class — it is
the generic consequence of relaxing a cardinality, and the next `N:M` migration in this repo
will have it too.

**The generalisation.** Any invariant a **type** used to guarantee, and a **collection**
no longer does, has silent readers. Cardinality is the common one; non-null and uniqueness
are the same shape. The compiler catches every reader of the *field* and none of the
readers of the *guarantee*.

**Destination.** `kb/dev/` — a new page, e.g. `cardinality-change-blast-radius.md`, or a
section on an existing data-migration page if one covers relaxing invariants. Central KB
(`C:\Dev\JARVIS\kb\`), so draining it is a **cross-repo visit** owing a row on that board.

**Anchors.** `docs/PRODUCT_v0.3.md` §1.2 (the edge table) · §4.7 (the
counts-in-full/minutes-divide asymmetry) · `app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalsScreen.kt`
the `items(...)` key · `CHANGELOG/2026-08-15/d2-life-area-route.md` the review table, row 1.

**Supersedes.** Nothing.

**Status.** ⏳ **Not drained** — `kb/` destination, cross-repo, and this session holds no
row on the JARVIS board. Drainable by any session that takes one.

---

## 2 · A concurrency rule that partitions **files** does not partition a **build**

**Claim.** The session-board model — claim by path, disjoint working sets — makes two
sessions safe against each other's *edits* and says nothing about their *toolchain*. On a
shared working tree the build output directory is a singleton that no path claim covers, and
on Windows two concurrent Gradle runs on one `app/build/` fail each other with
`IOException: Could not delete …` **permanently**, not transiently. The documented remedy
(*"re-run, or `rm -rf app/build/generated/ksp`"*) is written for a stale lock left by a dead
process and does not apply: the directory is being recreated by a live sibling, so the wipe
races and the re-run fails identically. Measured here: four consecutive attempts, two of them
after a full wipe.

**The remedy that works, and why it is the right shape.** Move *your own* build directory
out of the repo for the duration, with a Gradle `--init-script` that sets
`layout.buildDirectory`, plus task-level `exclude` patterns for the sibling's untracked
source. Both live in the session scratchpad; **nothing in the repo changes and the project
still builds normally without the flag.** That matters because the alternatives are all
worse in the way the rules already forbid elsewhere: killing their daemon is the
blanket-`qemu-kill` anti-pattern one layer up, and `git stash`-ing their files mutates a live
session's tree.

**One trap inside the remedy.** AGP's `android.sourceSets.*.kotlin.filter.exclude(...)` does
**not** reach `compileDebugKotlin` — the Kotlin plugin builds its own source set, so the
filter is accepted, changes nothing, and the build fails exactly as before. Exclude on the
**tasks** instead (`compile*` / `ksp*` all extend `SourceTask`, whose `exclude` is the right
hook). Also: an init script has no AGP on its compile classpath, so the Kotlin DSL cannot
name `AppExtension` and the script has to be Groovy.

**Why it belongs in the KB.** `AGENTS.md` names the Windows KSP lock as a flaky-rerun
pitfall, and the topology rule names the Gradle daemon as a claimed singleton. Neither says
what to do when a sibling **holds the singleton without claiming it** — and "ask the user to
stop the other session" is not available to an agent that is supposed to keep working.

**Rejected on the way:** (a) waiting for the sibling — their build stayed red for the whole
session on their own in-flight code, so the wait had no bound; (b) fixing their compile
errors to unblock the build — adopting a live session's work, which the rule forbids and
which this session only did for the **one** line its own rename broke; (c) copying the repo
to the scratchpad — defeated by two files both sessions were editing, which have no
separable version.

**Destination.** `kb/dev/` — extends whatever page carries the session-board / singleton
material, or a new `shared-tree-build-isolation.md`. Central KB, **cross-repo visit**.

**Anchors.** `AGENTS.md` Pitfalls, the Windows-file-locks entry · `SESSIONS.md` the
`d2-life-area-route` row's Singletons column and the 📣 note above it ·
`CHANGELOG/2026-08-15/d2-life-area-route.md` → *How it was run, and what that costs*.

**Supersedes.** Narrows `AGENTS.md`'s *"re-run, or `rm -rf …`"* advice by naming the case
where it cannot work. Does not contradict it — that advice is right for the single-session
case it was written for.

**Status.** ⏳ **Not drained** — `kb/` destination, cross-repo, no row on that board.

---

## 3 · Two sessions wrote the same helper an hour apart, and the loser's KDoc is what resolved it

**Claim.** When parallel sessions build against one spec section, they converge on the same
primitive independently — and the cheap thing that resolves it is a **KDoc that names the
other file by path and says which way the dependency should point**. Not a lock, not a
claim: the claim board partitions *territory*, and a shared primitive belongs to no
territory, so it is exactly what a path claim cannot prevent.

**Why.** Spec §4.8 (*every number owes direction isolation*) is cited by three open tickets.
This session wrote `ui/components/BidiText.kt`; the widget session wrote
`core/util/Bidi.kt` within the hour. Neither could have seen the other coming — both are
untracked until commit, so `git status` shows nothing to a session that has not looked, and
both are correct implementations of the same four lines.

What settled it in seconds was that the widget session's KDoc already read: *"`ui/components/BidiText.kt`
(session `d2-life-area-route`, spec §4.8) is the Compose-side companion to this; when it
lands, this file is the string half it should call rather than a second implementation."*
They had seen the board row, inferred the collision, and left the merge instruction in the
artifact. This session deleted its own file and its own test and imported theirs — theirs
being better on the merits too (idempotent, so composing two isolating builders cannot nest
marks; a `strip()` for tests; no Android dependency, so pure builders can use it).

**The transferable part.** A duplicate primitive is not prevented, it is **arbitrated**, and
arbitration is cheap only if the evidence is in the file rather than in a transcript. So:
when you write a shared primitive in a repo with live siblings, name the collision you expect
and state the direction — one sentence, in the KDoc, addressed to a reader who does not exist
yet. Two implementations of one rule is the *"second number that quietly disagrees"* at the
code layer, and the version that ships is decided by whoever commits last unless something
written down says otherwise.

**Rejected on the way:** (a) keeping both, one delegating to the other — the Compose-side
file had nothing Compose-specific in it, so it would have been an empty indirection kept only
to avoid deleting something; (b) keeping mine because it had a test — the test moved
naturally, and "mine has a test" is an argument about effort, not about the artifact.

**Destination.** `kb/dev/` — the parallel-sessions / concurrency page, alongside entry 2.
Central KB, **cross-repo visit**.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/core/util/Bidi.kt` (its KDoc's last
paragraph is the artifact) · `docs/PRODUCT_v0.3.md` §4.8 · §0.3 ·
`CHANGELOG/2026-08-15/d2-life-area-route.md` §4.

**Supersedes.** Nothing.

**Status.** ⏳ **Not drained** — `kb/` destination, cross-repo, no row on that board.
