# KB candidates — `widget-pack`, 2026-08-15

Session: `/implement #10` (the home-screen widget pack) · repo `C:\Dev\Android_Final_Project` ·
branch `feat/goalpilot-implementation` · mode `AUTO MODE`.

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

---

## 1 · A test that a rule *fired* is only half a test; the other half is that it **shrank**

**Claim.** When a rule says *"X must be present, sized to the context"*, a test asserting presence
passes against an implementation that returns one constant everywhere — and it reads as coverage.
The rule needs a **second** assertion on the property that varies. Pair every *"it is there"* test
with an *"it is different where it should be different"* test, and derive the second from the same
predicate as the first so a case added later cannot escape it.

**Why.** Spec §4.5's rule is *the disclosure shrinks to the smallest true sentence the tile can
hold, and no size ships without one*. The obvious test — every tile at every size has a non-blank
disclosure — was written first, and it would have passed against a `WidgetStrings` fake returning
`"disclosure"` for all four sizes. That fake is exactly what a hurried author writes. So the test
that guards the rule guards only its trivial half, and the interesting half (does the `2×2`
sentence actually get shorter?) is unguarded while the file *looks* well tested. The second
assertion — a smaller size never gets a longer sentence — is what forced the fake to carry real
per-size strings, which in turn is what makes the whole suite meaningful.

The generalisation is not about disclosures. Any rule of the form *"present, and proportioned"*
splits this way: rate limits (present, and scaled to the tier), retries (present, and backed off),
redaction (applied, and to the right span). The presence half is easy and self-satisfying; the
proportion half is the requirement.

**Second, sharper form found in the same pass:** the first version of the shrink test enumerated
three tiles by hand and silently omitted the fourth. Rewriting it as
`WidgetTile.entries.filter { it.derivesNumbers }` — the *same* predicate the presence test uses —
means a tile added next year is covered by construction. A hand-written list in a test is a
coverage claim that decays without any edit to the test.

**Rejected on the way:** (a) trusting a review to notice a constant-returning fake — the whole
point is that it looks correct in source, which is the same failure mode §4.1 records for a skin
picker no material reads; (b) asserting on the exact strings, which couples the test to copy and
makes every wording change a test change; (c) asserting `small != large`, which passes on two
different constants and says nothing about direction.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — a testing page. Closest existing neighbour is whatever
holds the `/tdd` anti-patterns (*the test that can't fail*); this is a **sibling** of the
tautological-test anti-pattern rather than an instance of it — the test *can* fail, it just cannot
fail for the reason it exists.

**Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/widget/BuildWidgetTileUseCaseTest.kt`
(`every tile showing a derived or divided number carries a disclosure at every size` +
`the disclosure really does shrink`) · `docs/PRODUCT_v0.3.md` §4.5.

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary `kb/dev/` page, **`AUTO MODE`-eligible**, **not drained here** — a
`/kb-ingest` into `C:\Dev\JARVIS` is cross-repo work and would need a row on that board while three
siblings are live in this one. Left for the next session that visits the bundle.

---

## 2 · Deleting the shared class is not the same as deleting the shared behaviour

**Claim.** When a framework resolves instances by **type** rather than by identity, parameterising
one class to serve N roles is not deduplication — it silently merges the N roles into one. The
tell is a framework API that takes a `Class<T>` where you expected an id.

**Why.** Glance's `GlanceAppWidgetManager.getGlanceIds(provider: Class<T>)` — and therefore
`GlanceAppWidget.updateAll()`, which calls it — resolves every *placed widget* by the class its
receiver declares. The natural design is one `GoalPilotWidget(tile: WidgetTile)` and five thin
receivers, which is genuinely less code and reads as the right factoring. It is wrong: all five
receivers declare the same class, so refreshing any one tile walks the ids of **all** of them and
re-renders each with whichever tile that instance was constructed for. Place the week donut and
the level ring, refresh once, and both become the same card.

This was caught by reading the API's signature while writing an unrelated helper, **not** by a
test and not by the compiler — and it would not have appeared on any device until a second tile
was placed *and* something triggered a refresh, which is the intersection of two conditions no
first-run smoke test hits.

The general shape: DI containers keyed by type, `WorkManager` unique-work names, `Intent` filters,
service loaders, and analytics registries all do this. Parameterising a class is only
deduplication when the framework can still tell the instances apart.

**Rejected on the way:** one receiver plus a configuration activity, which *would* have kept one
class — rejected on product grounds (it puts a setup screen between placing a tile and seeing it)
before the class problem was even known, so the right answer was reached for the wrong reason and
then confirmed for the right one. Worth recording: the product argument and the framework argument
pointed the same way, and only one of them was legible up front.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — a page on framework-resolution pitfalls, or an Android
page if one exists. It is not Glance-specific and should not be filed as a Glance note.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/ui/widget/WidgetReceivers.kt` ·
`ui/widget/GoalPilotWidget.kt` (the `abstract` and its KDoc).

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary `kb/dev/` page, **`AUTO MODE`-eligible**, **not drained here** — same
cross-repo reason as entry 1.

---

## 3 · Shared build output is a fifth kind of contention, and the board has no column for it

**Claim.** The session board partitions **source paths** and names **singletons** (the daemon, a
device, the git index). Neither covers `app/build/` — a *derived* tree that every session writes
concurrently through a tool that assumes it owns it. On Windows this produces failures that look
like defects in the code under test: `NoSuchFileException` on a KSP-generated file, `failed to
make parent directories`, `Could not move temporary workspace … dependencies-accessors`, and
`Cannot find required type element <Application>`. **None of them mention concurrency**, and
`Cannot find required type element` in particular reads as a broken Hilt setup.

**Why.** Claiming the Gradle daemon is not the same as claiming the build directory: the daemon is
a *process* and sessions politely queue on it, while `app/build/` is *state*, and two builds
minutes apart still interleave through it via stale caches and open handles. Four sessions ran in
this tree on 2026-08-15; the KSP failure hit twice, the accessors failure survived four retries and
a `--stop`, and each time the honest diagnosis was *someone else built*.

Two consequences worth stating separately from the diagnosis:

- **Re-running is not the remedy and re-running teaches you it is.** A plain retry sometimes
  succeeds (the other build finished), which trains the reflex, and then fails four times in a row
  when a handle is genuinely held. Reach for `rm -rf app/build/generated/ksp` — and for
  `.gradle/<version>/dependencies-accessors` after a version-catalogue edit — **early**, not after
  the third retry.
- **A red tree is attributable, and attributing it is a courtesy with a deadline.** One red main
  source set stops *every* source set in the module, so it blocks every session at once. Twice in
  one evening a session was blocked by a file it did not own; both times the fix was one sibling
  reading the error, checking `git status` for the owner, and saying so on the board rather than
  editing the file. That is fast when it happens and expensive when it does not — the blocked
  session otherwise concludes its own unit is broken.

**Rejected on the way:** (a) claiming `app/build/` as a board path — it is derived, nobody *owns*
it, and a claim would block every session's build rather than serialise it; (b) a worktree per
session, which fixes it completely and was not authorised (global rules: no worktrees unless
asked); (c) treating it as the existing Gradle-daemon singleton, which is what everyone had
already done and is precisely what did not work.

**Destination.** `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5 — the singleton
clause, which currently names *the build daemon* and should name *the build daemon **and its
output tree***, plus the attribution duty. **Destination `rules/`, therefore ⛔ always-ask in both
modes**, and the 🎬 walkthrough gate owns it.

*(The Windows-lock half alone — symptoms and remedies, no rule change — could land in
`kb/dev/` independently and would be useful there. Splitting it that way is a judgement for the
ingesting session, not one to take here.)*

**Anchors.** `CHANGELOG/2026-08-15/widget-pack.md` (*Two things worth not re-deriving*) ·
`SESSIONS.md` notes of 20:52 and 21:26 · `AGENTS.md` Windows-file-locks pitfall.

**Supersedes.** Nothing; it **extends** the singleton clause.

**Status.** ⛔ **Always-ask — not drained, and not proposed as a diff.** Destination `rules/`.
