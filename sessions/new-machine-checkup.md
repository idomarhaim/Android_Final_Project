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

1. **First Gradle build**: no standalone JDK on this machine — set `JAVA_HOME`
   to `C:\Program Files\Android\Android Studio\jbr`. Expect a large dependency
   download. `.\gradlew assembleDebug`, or the user's `Run GoalPilot.cmd`.
2. **Cloud Functions redeploy — conditional**: the deployed GROQ proxy still
   holds the OLD machine's key. If that key was revoked when the user created
   the new one, AI recommendations are silently serving local fallback (the
   documented failure shape). **Node.js and firebase-tools are NOT installed**
   — install first, then `cd functions && npm install && firebase deploy
   --only functions` (docs/SETUP.md step). If the old key still works, this
   can wait, but say so in the changelog.
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
