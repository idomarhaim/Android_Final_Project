# KB candidates — `tour-refresh`, 2026-08-24

Two entries. Both are the same family as `kb/dev/look-at-your-own-output.md` §4c-*, and **neither
is §4c-ii**: that section is *"`UP-TO-DATE` can mean a sibling session already ran it"*, which is
about **who** ran the task. These are about a check that **cannot fail**, which is a different
defect and has a different remedy.

---

## 1 · A doc and the test that guards it can encode the same wrong rule, and then the test passes *because* both sides are wrong

**Claim.** When a guard test and the prose it enforces are written by the same session from the same
belief, an assertion of the form *"these two things agree"* is worthless: the test applies the wrong
rule to **both** operands, they agree under it, and it goes green while the real artefacts disagree.
The failure survives every review that reads the test, because the test says exactly what the doc
says.

**Observed:** 2026-08-24, `Android_Final_Project`. `app/build.gradle.kts` declared
`releaseNotesFile = "release-notes.txt"`. `ReleaseNotesGuardTest`'s KDoc and `docs/RELEASING.md`
both asserted *"the plugin resolves it relative to the `app` module"*. On that belief a session
**deleted the copy at the repo root** on 2026-08-22 — the file the plugin had actually been reading.
The first local upload afterwards, two days later, died with

```
Failed to read file "C:/Dev/Android_Final_Project/release-notes.txt"
```

naming the **repo root**. The plugin resolves against the **root project**.

The guard's third assertion is *"the tag route and the local route name the same file"*. It
**passed while they named different files**: `release.yml` passes `app/release-notes.txt` (root-
relative, correct), the Gradle property said `release-notes.txt`, and the test resolved the Gradle
side against `app/` before comparing. Two errors cancelling inside one assertion.

**Also refuted by the same measurement:** the KDoc's `Inferred:` claim that releases `20f3b7e` and
`67c21e5` shipped testers a placeholder. The plugin read the root file, which is the one people were
editing, so those releases shipped the notes written for them. A hedge marked `Inferred:` was doing
its job — it was resolvable, and resolving it reversed it.

**Why the fix is the form and not the comment.** The property is now
`releaseNotesFile = "app/release-notes.txt"`, which names the same file under **either** reading of
the resolution rule. A comment saying which reading is right is exactly what was there before, in
two files, and it rotted the moment nobody re-ran the route.

**Rejected:** *re-create the stray root file* — that reinstates the two-files defect the guard was
written for. *Document the rule harder* — the rule was documented, twice, wrongly.

**Destination:** `kb/dev/` — either a new page (*a guard written from the same belief as the thing
it guards*) or a section under `look-at-your-own-output.md`, since the operative habit is that one's
**re-run whatever will consume your output**: nobody had run the local upload route since the
deletion, and the route is the only consumer that can answer this question.

**Anchors:** `docs/RELEASING.md` §3, `app/src/test/java/.../ReleaseNotesGuardTest.kt` KDoc,
`app/build.gradle.kts` `firebaseAppDistribution { }`.
**Supersedes:** the resolution rule stated in both files above (both corrected in this session's
commit, not left standing).
**Status:** ready.

---

## 2 · A guard that *parses a file to decide* must declare that file as a task input — and a build script is the case nobody declares

**Claim.** The `inputs.file` discipline for guard tests is usually written about the **data** the
guard reads (a notes file, a workflow YAML). It applies identically to a **build script** the guard
parses — and that one is missed by default, because a build script feels like configuration rather
than input. The result is a guard that is cacheable across precisely the edit it exists to catch.

**Observed:** 2026-08-24, same session. `ReleaseNotesGuardTest` reads `app/build.gradle.kts` to
extract the declared `releaseNotesFile`. Its KDoc says *"if this class grows a third file, declare
that one too"* — without noticing the class **already had a third file**, the build script itself.
Mutation check, property reverted to the broken value:

| | |
|---|---|
| before declaring the input | **`BUILD SUCCESSFUL` in 9 s**, nothing executed |
| same mutation, `--rerun-tasks` | 3 of 4 assertions **fail** |
| after `inputs.file(layout.projectDirectory.file("build.gradle.kts"))` | same mutation **fails with no `--rerun-tasks`** |

So the guard was non-vacuous only under a flag nobody passes.

**The generalisation worth keeping:** a guard's inputs are *everything it reads to reach a verdict*,
not *everything that looks like data*. The test for whether you have them all is the mutation check
run **without** `--rerun-tasks` — with the flag, every guard looks protective.

**Rejected:** *always `--rerun-tasks` in CI* — same objection §4c-ii already records, and it hides
the missing declaration rather than fixing it.

**Destination:** `kb/dev/look-at-your-own-output.md`, beside §4c-ii, as its own instance.
**Anchors:** `app/build.gradle.kts` `tasks.named("testDebugUnitTest")` input block.
**Supersedes:** nothing.
**Status:** ready.
