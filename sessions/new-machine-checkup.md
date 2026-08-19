# Brief: `new-machine-checkup` — first working session on the new machine

> **Filed:** 2026-08-19, by the migration session (old machine in repair since ~2026-08-18).
> **Why this file exists:** git-side state is verified current, but nothing has
> been *built or run* on this machine yet. This is a `/kickoff` work order: do
> the checks below, then **move this file to `sessions/done/` and write a
> changelog entry** (per `skills/kickoff` convention — move, don't delete).

## Already done by the migration session (2026-08-19)

- `feat/goalpilot-implementation` **merged to `main` (`a0d8c9b`) and the local
  branch deleted — this repo is main-only now.** (The remote branch still
  exists; the user was asked to delete it on github.com.)
- Template sync v18/v3 (`7ad0cb8`); git hooks reinstalled; `local.properties`
  regenerated.
- Android SDK (API 35, google_apis x86_64) installed headlessly; **both AVDs
  recreated on the real `pixel_10_pro_xl` device profile** (1344×2992 — the
  profile shipped only with newer cmdline-tools) and **boot-verified** with
  WHPX acceleration.
- `functions/.env` recreated with the user's new GROQ key (verified
  gitignored); the `DEFAULT_MODEL` pin `openai/gpt-oss-20b` checked current.

## To do in this session

1. **First Gradle build** — ⚠️ **blocked as written; corrected 2026-08-19.**

   The original instruction (*"set `JAVA_HOME` to Android Studio's `jbr`"*) **will not
   work**, for two independent reasons, both measured today:
   - **`gradle.properties:22` pins `org.gradle.java.home=C:/Program Files/Eclipse
     Adoptium/jdk-21.0.12.8-hotspot`, and that directory does not exist on this
     machine.** `org.gradle.java.home` **overrides** `JAVA_HOME`, so setting
     `JAVA_HOME` changes nothing until this line is fixed or removed.
   - **Android Studio's `jbr` is JDK 25** (`openjdk 25.0.2`), and the wrapper is
     **Gradle 8.10.2**, which does not run on 25. So `jbr` is not a usable substitute
     even after the pin is removed.

   **There is no JDK 21 on this machine at all** (`C:\Program Files\Eclipse Adoptium\`
   is absent; `C:\Program Files\Java\` does not exist). So the first real step is
   **install Temurin JDK 21**, then point `org.gradle.java.home` at it and set
   `JAVA_HOME` to match. Only then `.\gradlew assembleDebug`, or `Run GoalPilot.cmd`.

   **Do not "fix" this by bumping Gradle/AGP to accept 25** — that is a toolchain
   upgrade, not a machine setup, and it is not this session's scope. Ask Ido first.
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
