# KB candidates — `tutorial-onboarding`, 2026-08-22

Each entry stands alone: another session's chat history is not a source.

---

## 1 · `navigate()` with the bottom-bar `NavOptions` is a SILENT NO-OP from a non-tab screen

- **Claim.** In Jetpack Compose Navigation, the bottom-navigation idiom —
  `navigate(route) { popUpTo(startDestination) { saveState = true }; launchSingleTop = true;
  restoreState = true }` — **does nothing at all** when it is called from a destination that was
  pushed *above* the start destination and the target *is* the start destination. No exception is
  thrown, nothing is logged, and `currentBackStackEntry` is unchanged one frame later. Dropping the
  `saveState`/`restoreState` pair fixes it; the rest of the options are fine.

  `Observed:` 2026-08-22, GoalPilot, `emulator-5554`, navigation-compose per
  `gradle/libs.versions.toml`. Temporary logging showed the call being made
  (`navigateToTab(dashboard) before=settings start=dashboard` → `after=settings`) with no throw.
  The identical call from another tab (`goals` → `dashboard`) worked in the same session.
  `Inferred:` the mechanism — a *restore* branch inside `NavController.navigate` that finds nothing
  to restore, returns false, and does not fall through to an ordinary navigate. `Untested:` that
  reading; it is off the symptom, not off androidx's source.

- **Why it matters more than an ordinary bug.** The failure is **origin-dependent and silent**, so
  it is invisible to the way anybody tests the feature that hit it. GoalPilot's guided tour navigates
  from two places: from a tab (works) and from Settings (does not). Walking the tour forwards — which
  is what testing a tour *means*, and what seven hand-taken screenshots did — exercises only the
  working origin. 782 JVM tests, a 219-test instrumented suite and a full visual pass all went green
  over it.

- **The transferable rule.** *The bottom-bar navigation idiom is the bottom bar's, not a general
  "go to this screen".* Give any other caller its own function without the save/restore pair, and say
  in its KDoc why the two exist. Rejected alternative: one function with a `saveState: Boolean`
  parameter — a caller that has to pass the right flag to get the behaviour its own button promises
  is a caller that can pass the wrong one.

- **How to test it, since a unit test cannot.** Build a real `NavHost` with a start destination, a
  second tab and one screen pushed above the start, and assert the route after the call. Check the
  test against the defect by reverting the fix: here that gave `expected: dashboard but was:
  settings` on exactly one of four cases, reproducing the asymmetry mechanically.

- **Destination.** `C:\Dev\JARVIS\kb\dev\` — a new page, `compose-navigation-traps.md`, or a §
  appended to whichever page already holds Compose navigation findings.
- **Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/ui/root/GoalPilotRoot.kt`
  (`navigateForTutorial` KDoc) · `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/TutorialNavigationUiTest.kt`
  · `CHANGELOG/2026-08-22/tutorial-onboarding.md` §4.2.
- **Supersedes.** Nothing.
- **Status.** Pending.

---

## 2 · A guided tour's own walkthrough is the weakest test it has

- **Claim.** Verifying a multi-step in-app tour by *walking it forwards* tests the one path the
  author already had in mind and systematically misses the two that break: (a) entering the tour from
  a **different origin** — a replay control, a deep link, a rotation — and (b) moving **backwards**
  into a step that has state behind it.

  `Observed:` both, on the same feature, on 2026-08-22. (a) is entry 1 above. (b) was caught earlier
  and only because a JVM test was being written: stepping back into the tour's one interactive step
  landed the user on the screen that step asks them to open, so the requested tap changed no route,
  nothing completed, and the card had no *Next* button — a dead end reachable by pressing Back once.
  A forwards walk cannot reach either.

- **The rule.** For any stepped flow, the test matrix is **origins × directions**, not steps. Walk it
  forwards once for the screenshots; then enter it from every entry point the product has, and step
  backwards through every step that changes state.

- **Destination.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` — it is that page's thesis in a
  new shape (the instrument passes on exactly the input it was built from), or a short page of its
  own if that one is already long.
- **Anchors.** `CHANGELOG/2026-08-22/tutorial-onboarding.md` §3.4 and §4.2 ·
  `app/src/test/java/com/idomarhaim/goalpilot/ui/tutorial/TutorialControllerTest.kt`.
- **Supersedes.** Nothing.
- **Status.** Pending.

---

## 3 · `adb shell` device paths need `MSYS_NO_PATHCONV=1` too, not just `adb pull`

- **Claim.** This project's notes already say `adb pull` from Git Bash needs `MSYS_NO_PATHCONV=1`.
  The same mangling hits **`adb shell` arguments**: `adb -s <serial> shell screencap -p /sdcard/t.png`
  writes nothing and the later `pull` fails with *failed to stat remote object '/sdcard/t.png': No
  such file or directory* — Git Bash rewrote the device-side path into a Windows one before `adb`
  ever saw it. `screencap`'s own usage text is printed, which reads as a wrong flag rather than a
  wrong path.

  `Observed:` 2026-08-22, GoalPilot, `emulator-5554`. `export MSYS_NO_PATHCONV=1` once at the top of
  the shell block fixes both halves.

- **Why it is worth a line.** It fails toward the wrong diagnosis: the visible error is a usage
  banner from the tool you called, so the instinct is to re-read `screencap`'s flags.

- **Destination.** `C:\Dev\JARVIS\kb\dev\android-device-verification.md` — beside the existing
  `adb pull` note, which is the same trap one command over.
- **Anchors.** `SESSIONS.md` release note for `53-tag-sweep`, 2026-08-22 (the `adb pull` half).
- **Supersedes.** Nothing; it widens an existing claim.
- **Status.** Pending.
