# KB candidates — `tutorial-onboarding` (cleanup pass), 2026-08-22

Each entry stands alone: another session's chat history is not a source.

---

## 1 · A guard whose inputs are invisible to the build reports on the PREVIOUS run — second measured instance

- **Claim.** A file-reading guard test asserts over files the build system does not associate with
  the test task. Gradle then answers **UP-TO-DATE** when only those files change, so the guard
  reports **green on the previous run's result** — including during the mutation check written to
  prove the guard works. The fix is to declare the real inputs
  (`inputs.file(...).withPathSensitivity(...)`) on the test task; there is no way to infer them.

  `Observed:` 2026-08-22, `C:\Dev\Android_Final_Project`, session `tutorial-onboarding`.
  `ReleaseNotesGuardTest` reads `app/release-notes.txt` and `.github/workflows/release.yml`. The
  mutation — editing the workflow to name a different notes file — produced
  `BUILD SUCCESSFUL in 2s`, `Task :app:testDebugUnitTest UP-TO-DATE`, with **nothing executed**.
  After declaring both files as inputs, the same mutation fails the intended case in 4 s.

- **Why it is worth recording despite already having a page.** `look-at-your-own-output.md` §4c
  already holds *the build never ran the check you are reading the result of*, and its §4c-i widened
  the trigger past "outside the module". This is a **third** instance and the second in three days
  (`shared-fixtures/derived-state.json`, 2026-08-20, is the one the same build file already
  documents) — so the pattern is not a curiosity, it is what happens by default every time somebody
  writes a guard over a non-source artifact in this repo.

- **The failure mode that makes it worse than a stale result.** It corrupts the *mutation check*,
  which is the one procedure that exists to tell a real guard from a decorative one. A mutation that
  the guard "did not catch" reads as *the guard is too weak* and the natural next move is to
  strengthen the assertion — working on a test that was never executed. `Observed:` here; the wrong
  conclusion was one line away.

- **The tell, and it is cheap.** A test task that finishes in ~2 s when it normally takes ~40 s, and
  the word `UP-TO-DATE` beside it. Read the task line, not just `BUILD SUCCESSFUL` — and when running
  a mutation, prefer to confirm the task actually **executed** before believing either outcome.

- **Destination.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` — a short addendum under §4c,
  not a new page: it is that section's claim with a third instance and a measurement.
- **Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/resources/ReleaseNotesGuardTest.kt` (KDoc) ·
  `app/build.gradle.kts` (the `tasks.withType<Test>` inputs block) ·
  `CHANGELOG/2026-08-22/tutorial-onboarding.md` §11.
- **Supersedes.** Nothing; it adds an instance to a standing claim.
- **Bundle check.** Grepped 2026-08-22: `look-at-your-own-output.md` §4c and §4c-i are the existing
  home, and `dev/generated-values-need-matrix-guards.md` is adjacent but about coverage, not staleness.
- **Status.** Pending.

---

## 2 · A config path that reads like a repo-root path and is not one

- **Claim.** `releaseNotesFile = "release-notes.txt"` in an Android module's `build.gradle.kts` is
  resolved **relative to the module**, not the repo root — but it is written where a reader is
  thinking about the repo, and it looks like a repo-relative path. Read literally, it invites the
  creation of a *second* file at the root, which then becomes the one people edit, because it is what
  `ls` shows.

  `Observed:` 2026-08-22, `C:\Dev\Android_Final_Project`. The root copy was created on 2026-08-20 by a
  session whose own changelog states the misreading verbatim — *"it did not exist, and
  `app/build.gradle.kts` names it as `releaseNotesFile`"*. `Inferred:` the two releases that then
  edited it (`20f3b7e`, `67c21e5`) shipped the other file's placeholder text to real testers.
  `Untested:` that inference — the Firebase CLI has no `appdistribution:releases:list`, so what
  testers saw cannot be read back from a shell.

- **Why the deletion is not the fix.** Deleting the stray fixes today and leaves the reading that
  produced it, so it recurs. What closes it is a guard asserting *the declared path resolves to a
  real file* **and** *no second file of that name exists* **and** — the one that is not hygiene —
  *the CI route and the local route name the same file*, since those two paths are edited by
  different people at different times and nothing else makes them agree.

- **Generalises to** every path-valued build-config property: proguard files, signing configs,
  lint baselines, `google-services.json`. The discriminator is whether a wrong path **errors** (most
  do) or **silently selects nothing** (a notes file does — the upload succeeds and testers see a
  blank or stale note).

- **Destination.** `C:\Dev\JARVIS\kb\dev\` — a § appended to
  `copied-options-are-a-silent-no-op.md`, which is this session's own page on *config that is correct
  in one frame of reference and silently wrong in another*. Same concern, different axis (there the
  caller, here the working directory).
- **Anchors.** `docs/RELEASING.md` §3 · `ReleaseNotesGuardTest` · `CHANGELOG/2026-08-22/tutorial-onboarding.md` §11.
- **Supersedes.** Nothing.
- **Bundle check.** Grepped 2026-08-22: no page covers path-valued build config; the nearest is
  `copied-options-are-a-silent-no-op.md`, written earlier today.
- **Status.** Pending.
