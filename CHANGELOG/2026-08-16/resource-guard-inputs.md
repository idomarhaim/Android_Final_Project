# `resource-guard-inputs` — 2026-08-16

> **Summary:** Filed by `51c-analytics-render`, which found it and correctly left it: it lives in `app/build.gradle.kts`, outside that unit's package.

`/kickoff resource-guard-inputs` — **the localization guards can silently not run** ·
[`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51) · branch
`feat/goalpilot-implementation` · mode `AUTO MODE`

Filed by `51c-analytics-render`, which found it and correctly left it: it lives in
`app/build.gradle.kts`, outside that unit's package. One file changed.

## 1 · The fault, reproduced before it was fixed

Four guards in this repo do not exercise code — they open files with `java.io.File` and assert on
the text:

| Guard | Reads |
|---|---|
| `resources/HebrewLocaleResourceTest` | `src/main/res/values*/` — parity, no `values-he`, no Hebrew in `values/`, no untranslated copies |
| `resources/AnalyticsLiteralSweepTest` | `src/main/java/com/idomarhaim/goalpilot/…` — prose literals in source |
| `widget/WidgetHebrewResourceTest` | `src/main/res/values*/widget_strings.xml` |
| `widget/WidgetPaletteResourceTest` | `src/main/res/values*/widget_colors.xml` |

Nothing in that is visible to Gradle. `:app:testDebugUnitTest` declares the test classes and the
runtime classpath as its inputs, and **a resource-only edit changes neither**.

`Observed:` 2026-08-16, on `ae86e7b`, before touching the build script. `values-iw/widget_strings.xml`
line 34 set from `רמה` to `Level` — the exact shape `HebrewLocaleResourceTest`'s *"no hebrew string is
an untranslated copy of its english original"* exists to catch:

```
> Task :app:testDebugUnitTest UP-TO-DATE
BUILD SUCCESSFUL in 6s
```

Green, and not one assertion executed.

## 2 · Why `R.jar` does not save you, which is the part worth remembering

The obvious objection is that resources *do* reach the unit-test classpath through the generated
`R.jar`, so surely a resource change invalidates the task. It does not, because **`R.jar` is keyed on
resource _names_, not values.** Adding or deleting a key changes it; changing what an existing key
says does not. So the guards are blindest at exactly the edit they were written for — a translation
sweep is a stream of value edits under `values-iw/` and nothing else.

`Observed:` 2026-08-16, `md5sum` of
`app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar`
across three `:app:processDebugResources` runs — this was stated as the mechanism before it was
checked, and checking it was the pre-commit self-review's job:

| Edit | R.jar md5 |
|---|---|
| baseline | `804a77b0cc1b519d70c13379bd79dc5f` |
| `gp_widget_level` value `רמה` → `Level` | `804a77b0…` — **unchanged** |
| a new key `gp_tmp_probe_key` added to `values/` | `8263679f8fa2f0aceca3a83dae6c7493` — changed |
| probe key reverted | `804a77b0…` — back |

It fails in the flattering direction. A sweep session edits resources, runs the suite, sees green,
and has proof of nothing.

## 3 · The fix

`app/build.gradle.kts` — the two scanned trees declared as inputs to every `Test` task:

```kotlin
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/res"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("fileScanningGuardResources")
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("fileScanningGuardSources")
}
```

Two rejected alternatives, and why:

- **`outputs.upToDateWhen { false }`** on the test task — correct and blunt. It re-runs the whole JVM
  suite on every build regardless of what changed, and the point here is to re-run on the changes
  that matter, not on all of them.
- **`--rerun-tasks` after a resource edit**, which is what `51c` left as the workaround. It works and
  nobody will remember, which is the definition of the defect rather than a fix for it.

`withPathSensitivity(RELATIVE)` is there for the build cache, which is on here (`3 from cache` in
these runs): an `inputs.dir` normalizes on ABSOLUTE paths unless told otherwise, which keys every
entry to this checkout directory.

**One claim in the first draft of this entry was wrong and is corrected here rather than deleted.**
It read *"an input declared on a cacheable task with no normalization warns"*. It does not.
`Observed:` 2026-08-16 — the two `withPathSensitivity` lines deleted, `:app:testDebugUnitTest
--rerun-tasks --warning-mode=all`: **no warning of any kind**, build green. So nothing in the build
will tell the next person if those lines are dropped, which is exactly why the reason for them is
written into `app/build.gradle.kts` beside the code.

`src/main/java` is belt-and-braces — a source edit already changes the compiled classes on the
runtime classpath, so it was never the hole. `res/` was.

## 4 · Proven in both directions, which is the exit criterion

A guard whose fix is not checked against its own fault proves nothing, so the same one-line break was
run through four states:

| # | State | `:app:testDebugUnitTest` | Result |
|---|---|---|---|
| 1 | Break applied, **no** input declaration | `UP-TO-DATE` | `BUILD SUCCESSFUL` — **the fault** |
| 2 | Break applied, declaration added | executed | **FAILED** at `HebrewLocaleResourceTest.kt:182` |
| 3 | Break reverted | executed | `BUILD SUCCESSFUL`, 351 / 0 |
| 4 | Break **re-applied from that green state** | executed | **FAILED** again |

State 4 is the one that is easy to skip and does the real work. States 2 and 3 both start from a task
that had just *failed*, and a failed task re-runs whatever its inputs say — so on their own they are
consistent with the declaration doing nothing. Only state 4 starts from a successful, up-to-date task
and shows a resource edit invalidating it.

The break was then reverted with `git checkout` and the file confirmed byte-identical to `HEAD`.

## 5 · What this does **not** cover

- **`connectedDebugAndroidTest` is untouched.** It is a `DeviceProviderInstrumentTestTask`, not a
  `Test`, so `tasks.withType<Test>()` does not reach it. Harmless today — `grep java.io.File` over
  `app/src/androidTest/` returns nothing, so no instrumented test scans files. It stops being harmless
  the day one does.
- **`src/main/AndroidManifest.xml` and `src/test/` resources are not declared.** No current guard
  reads them.
- Cost: any edit under `src/main/res` — a drawable, an unrelated colour — now re-runs the JVM suite.
  ~15s. That is the trade, taken deliberately.

## 🧪 Tests

| Layer | Result |
|---|---|
| JVM unit (`:app:testDebugUnitTest`) | **351 / 0**, no `--rerun-tasks` |
| Build (`:app:assembleDebug`) | green |
| `--warning-mode=all` | no Gradle deprecation warnings from the new inputs; the only `w:` lines are the pre-existing `GoogleSignIn` Kotlin deprecations in `data/auth/` and `data/tasks/` |
| Instrumented (`:app:connectedDebugAndroidTest`) | **not run** — build-configuration unit, no app code changed, and the emulator was not claimed |
| Security rules (`firestore-tests/`) | **not run** — nothing in this unit reaches `firestore.rules` |

## Files

- `app/build.gradle.kts` — the input declaration and the comment block explaining it
- `SESSIONS.md`, `sessions/resource-guard-inputs.md` — claim (`ae86e7b`) and close
