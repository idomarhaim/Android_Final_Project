# KB candidates — `resource-guard-inputs`, 2026-08-16

Repo: `c:\Dev\Android_Final_Project` · branch `feat/goalpilot-implementation` · mode `AUTO MODE`

---

## 1 · A file-scanning test is invisible to the build's up-to-date check, and the invalidation you assume exists usually is not there

**Claim.** A test that opens files with `java.io.File` and asserts on their *text* — rather than
exercising code — declares none of those files as task inputs. The build's incremental check
therefore reports the test task `UP-TO-DATE` after an edit the test exists to catch, and the suite is
**green having executed zero assertions**. It fails in the flattering direction, and it fails
precisely on the change shape the guard was written for.

The Android-specific half, which is the part that defeats the obvious counter-argument: *"but
resources reach the unit-test classpath through `R.jar`, so a resource change must invalidate it"*.
**`R.jar` is keyed on resource names, not values.** `Observed:` 2026-08-16, GoalPilot, `md5sum` of
`app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar`
over three `:app:processDebugResources` runs — a `values-iw/` **value** edit left it byte-identical
(`804a77b0…`); adding a **new key** changed it (`8263679f…`); reverting the key restored it. So a
translation sweep, which is a stream of value edits and nothing else, moves no declared input at all.

**Remedy.** Declare the scanned trees on the task:

```kotlin
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/res"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("fileScanningGuardResources")
}
```

**The verification shape matters more than the fix.** Proving this needs **four** states, not two:
break + no fix (green — the fault) · break + fix (fails) · revert (green) · **break re-applied from
that green state (fails)**. The fourth is the one that is skipped and the only one that proves
anything: states 2 and 3 both start from a task that had just *failed*, and a failed task re-runs
regardless of its inputs, so both are consistent with the declaration doing nothing.

**Why (and what was rejected).** `outputs.upToDateWhen { false }` — correct, blunt, re-runs the whole
suite on every build. `--rerun-tasks` after a resource edit — this was the standing workaround; it
works and depends on someone remembering, which is the defect rather than a fix for it.

**One thing that is NOT true, and was asserted before it was checked.** *"An input declared on a
`@CacheableTask` with no `withPathSensitivity` warns."* It does not. `Observed:` 2026-08-16, Gradle
8.10.2 / AGP 8.7.3 — those lines deleted, `--warning-mode=all`, **no warning of any kind**. The
normalization is still right (an `inputs.dir` defaults to ABSOLUTE, keying every cache entry to the
checkout directory) but **nothing in the build tells you if it is dropped**, so the reason belongs in
a comment beside the code.

- **Destination:** `kb/dev/` — a new page. General to any build system with an incremental check, not
  Android-specific and not locale-specific; the `R.jar` fact is the Android instance of it.
- **Anchors:** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` — this is that rule's *"check the
  instrument itself on the hardest input it exists for"* with the instrument being a **test**, and the
  degradation being total rather than partial.
- **Supersedes:** nothing.
- **Status:** pending.

---

## 2 · Cross-reference: the guard behind *a sweep is an event, not a state* was itself unenforced

**Claim.** `C:\Dev\JARVIS\kb\dev\jvm-vs-android-locale-codes.md` §4a argues a localization sweep must
be guarded by a test rather than trusted as a completed state. In GoalPilot that guard existed and
**did not run** on resource-only commits, for the whole of `51`, `51b` and `51c`. The section's own
enforcement mechanism had the failure mode the section warns about, one layer down.

**Why.** Worth one line on that page pointing at candidate 1, because a reader who adopts §4a's advice
adopts a guard that is off by default. The brief for this session named exactly this and made it
conditional on the fix being non-obvious — it was: the fault is invisible, the fix is three lines, and
the mechanism (`R.jar` keyed on names) is the part nobody guesses.

- **Destination:** `C:\Dev\JARVIS\kb\dev\jvm-vs-android-locale-codes.md` §4a — an added cross-reference
  line, **not** a rewrite of the standing claim.
- **Anchors:** §4a; candidate 1's page.
- **Supersedes:** nothing. If the edit turns out to need §4a's existing wording changed rather than
  extended, that is always-ask and stops.
- **Status:** pending.
