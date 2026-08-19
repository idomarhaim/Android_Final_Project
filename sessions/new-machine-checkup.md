# Brief: `new-machine-checkup` — first working session on the new machine

> **Filed:** 2026-08-19, by the migration session (old machine in repair since ~2026-08-18).
> **Why this file exists:** git-side state is verified current, but nothing has
> been *built or run* on this machine yet. This is a `/kickoff` work order: do
> the checks below, then **move this file to `sessions/done/` and write a
> changelog entry** (per `skills/kickoff` convention — move, don't delete).

## Already done by the migration session (2026-08-19)

- `feat/goalpilot-implementation` **merged to `main` (`a0d8c9b`) and the local
  branch deleted — this repo is main-only now.** The **remote** branch was
  deleted on 2026-08-19 too, after confirming 0 commits not already in
  `origin/main`; only `refs/heads/main` remains.
- Template sync v18/v3 (`7ad0cb8`); git hooks reinstalled; `local.properties`
  regenerated.
- Android SDK (API 35, google_apis x86_64) installed headlessly; **both AVDs
  recreated on the real `pixel_10_pro_xl` device profile** (1344×2992 — the
  profile shipped only with newer cmdline-tools) and **boot-verified** with
  WHPX acceleration.
- `functions/.env` recreated with the user's new GROQ key (verified
  gitignored); the `DEFAULT_MODEL` pin `openai/gpt-oss-20b` checked current.

## To do in this session

1. **First Gradle build** — ✅ **JDK resolved 2026-08-19; the `assembleDebug` run is still owed.**

   **Do not follow the original instruction** (*"set `JAVA_HOME` to Android Studio's
   `jbr`"*) — it was wrong, and the record of why is worth keeping because the obvious
   check does not catch it.

   **What was actually wrong.** `gradle.properties:22` pins
   `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot`, and
   that key **overrides** `JAVA_HOME` — so no amount of setting `JAVA_HOME` reaches it.
   The machine `JAVA_HOME` was *already* correct; the directory it named simply did not
   exist, because the new machine had **no JDK outside Android Studio**.

   **Why `jbr` is not the substitute. Measured — don't re-derive it:** Android Studio's
   `jbr` is `openjdk 25.0.2`, and `gradlew --version` on it **succeeds** (Gradle 8.10.2
   launches and prints *"Support for Java 23"*). So the cheap check **passes** and is not
   the test. Configuration is: `gradlew help -Dorg.gradle.java.home=<jbr>` fails in 20 s
   with the entire error body being the literal string `25.0.2` — a version parser giving
   up on `25`.

   **The fix, already applied — nothing left to do here:**
   - `winget install EclipseAdoptium.Temurin.21.JDK` → **21.0.12.8**, which lands at
     *exactly* the path already pinned. **No repo file was changed**; `gradle.properties`
     was right all along.
   - Machine `JAVA_HOME` already pointed there and is now valid. **`java` is still not on
     `PATH`** — that matters only for tools that read `PATH` instead of `JAVA_HOME`
     (`firebase-tools`; `firestore-tests/run-tests.mjs` works around it).
   - One stale `.gradle\8.10.2\dependencies-accessors` workspace, left by the failed
     JDK 25 attempt, produced the Windows *"Could not move temporary workspace"* error.
     Deleted; it regenerates. **If you hit that error, this is what it is** — not a code
     fault. See the note in `CLAUDE.md`.
   - **Verified:** `gradlew help` → `BUILD SUCCESSFUL in 1m 24s`. The Android project
     configures end to end on JDK 21.

   **Still owed by this session:** the real build. `.\gradlew assembleDebug`, or the
   user's `Run GoalPilot.cmd`. Expect a large dependency download on first run. Note this
   takes the `#gradle-daemon` singleton — claim it.

2. ~~Cloud Functions redeploy~~ — **DONE 2026-08-19 by the migration session**:
   Node.js LTS + firebase-tools installed, user completed `firebase login`
   (name.iddo@gmail.com), and `firebase deploy --only functions` updated all
   three functions (`getRecommendations`, `classifyTask`, `scoreTask`,
   us-central1, Node.js 22 2nd Gen) with the new **repo-specific** GROQ key
   from `functions/.env`. Nothing left here beyond a live smoke test of one
   recommendation once the app builds (item 1).
3. **Emulator Google accounts**: both AVDs are fresh images with no accounts.
   Per the run scripts: first emulator = name.iddo@gmail.com, second
   (`Pixel_10_Pro_XL_B`) = rachil751@gmail.com. **Respect the device-state
   banner protocol** (`## 📱 SIGN IN NEEDED` / `## 📱 DO NOT SIGN IN`) — an
   instrumented test run uninstalls the app and wipes the signed-in account.
4. RAM note for parallel work: this machine has 16 GB (was 32). WSL is capped
   at 10 GB machine-side; close FP_DEMO training before running
   emulator + Gradle together.

## Close-out

Move this file to `sessions/done/new-machine-checkup.md`, write the day's
changelog entry, and update `TODO/` if anything above spawned real work.
