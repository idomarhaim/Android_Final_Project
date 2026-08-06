# 🟡 OPTIONAL — Distribution

Follow-ups left open by the `release-distribution` session (05/08/2026), which
put the app on Firebase App Distribution with a real signing key and a
tag-triggered release workflow. See
[`CHANGELOG/2026-08-05/release-distribution.md`](../../CHANGELOG/2026-08-05/release-distribution.md)
and [`docs/RELEASING.md`](../../docs/RELEASING.md).

None of these block submission. They are the loose ends of shipping outside
Google Play.

---

- [ ] **Show the app version somewhere in the UI.**
  The app displays its version **nowhere** — `BuildConfig.VERSION_NAME` is never
  read (the only `BuildConfig` use is `FUNCTIONS_REGION` in
  `di/FirebaseModule.kt`). On Play that is a minor omission, because the store
  entry answers the question. Sideloaded it is not: after an in-app update the
  only way to know what you are running is Settings → Apps → GoalPilot, and the
  first thing anyone debugging a tester's report needs is which build they have.

  A line on the profile screen — `GoalPilot 0.2.2 (4)` from
  `BuildConfig.VERSION_NAME` / `VERSION_CODE` — is enough. Deliberately kept out
  of `v0.2.2` so nothing muddied the update-prompt test.

- [ ] **Give the debug build a distinct launcher name.**
  Both variants use `android:label="@string/app_name"` → **two icons both
  reading "GoalPilot"** once a release build is installed alongside a
  script-installed debug one. They are genuinely different apps
  (`com.idomarhaim.goalpilot` vs `…goalpilot.debug`) and coexist happily, but
  they are indistinguishable in the launcher — and only the release one carries
  a real updater, so testing the wrong icon looks like a broken feature.

  One line in the `debug` build type:
  `resValue("string", "app_name", "GoalPilot debug")`.

- [ ] **Decide whether `gradlew` should carry its executable bit in git.**
  `v0.2.0`'s CI run died in 11 seconds with `./gradlew: Permission denied` — the
  wrapper was committed from Windows, which carries no POSIX exec bit, and
  nothing local catches it because `gradlew.bat` is what runs on this machine.
  The workflow now does `chmod +x ./gradlew` after checkout, which works and is
  correct regardless of what the repo records.

  The tidier fix is `git update-index --chmod=+x gradlew`, restoring the mode
  `gradle wrapper` generates. It was **not** taken because `gradlew*` is listed
  as frozen in [`AGENTS.md`](../../AGENTS.md), and a frozen path is not somewhere
  to make a judgement call unilaterally. Needs a decision, not a patch: either
  set the bit and note the exception, or keep the CI-side `chmod` and record
  *why* so the next person does not "fix" it again.

- [ ] **Clean up the burned `v0.2.0` tag.**
  It published nothing (the `gradlew` failure above) and was left in place rather
  than moved, because re-pointing a pushed tag rewrites remote state for anyone
  who already fetched it. It remains a red ✗ in the Actions history and sends a
  failure email. Deleting a remote tag is a destructive operation — user's call.
