# brief-refresh — 2026-08-19

> **Summary:** Re-verified the five open `/kickoff` briefs against HEAD on the new machine and corrected what had rotted — the merged branch name, two briefs' stale counts and dead warnings, and a build blocker `new-machine-checkup` would have walked straight into.

## Why

Ido asked where the `/kickoff` plan stands after the machine change, and whether the two
no-build briefs (`docs-hygiene-backfill`, `kb-drain-51e-backfill`) had partly run on the
old machine. They had not — but three neighbouring sessions had, which is what made it
look like they might have.

## What changed

### 1 · `branch:` front matter — 5 briefs

`feat/goalpilot-implementation` was merged to `main` (`a0d8c9b`) and the local branch
deleted; the remote branch was deleted this session after confirming **0 commits** not
already in `origin/main` and that it is a direct ancestor of it. All five open briefs
still declared the dead branch in front matter, which `/kickoff`'s rot check reads.

### 2 · `docs-hygiene-backfill` — re-verified block

Every number in the brief had moved. `scripts/README.md:48-49` → **line 53**; *"one known
copy"* of the false `JDK 25` claim → **four live copies** (`scripts/README.md:53`,
`docs/OPERATIONS.md:134`, `docs/RELEASING.md:207`, `knowledge/release-distribution.md:70`);
**7 of 84** files carrying `> **Summary:**` → **11 of 88**; and the *"don't touch 51e's
dirty changelog"* warning is dead — it committed in `105baaf`.

Noted but **not** actioned: `AGENTS.md:136`'s JDK paragraph now describes a machine that
no longer exists. That is larger than the one-line fix the brief scoped, so it is flagged
as ask-first rather than silently folded in.

### 3 · `kb-drain-51e-backfill` — re-verified block

Title says "two" candidate files; there are **three** (9 entries: 4 + 3 + 2). All nine
still read `**Status.** Not drained`. Its first *"will bite"* item is dead —
`C:\Dev\JARVIS`'s Active-claims section holds **zero rows** (JARVIS HEAD `3591857`),
`kb-drain-jarvis-own` released, and a drain run today appends to `kb/log/2026-08-19.md`,
not the contended `2026-08-17.md`. Items 2 and 3 — the two always-ask candidates — stand
unchanged.

### 4 · `new-machine-checkup` item 1 — the build blocker

**Observed 2026-08-19:** its instruction to *"set `JAVA_HOME` to Android Studio's `jbr`"*
cannot work, for two independent reasons.

- `gradle.properties:22` pins `org.gradle.java.home=C:/Program Files/Eclipse
  Adoptium/jdk-21.0.12.8-hotspot`. `C:\Program Files\Eclipse Adoptium\` **does not exist**
  on this machine, and `org.gradle.java.home` **overrides** `JAVA_HOME` — so setting
  `JAVA_HOME` changes nothing while that line stands.
- Android Studio's `jbr` reports `openjdk version "25.0.2"`. The wrapper is Gradle
  **8.10.2**. This started as an inference from the compatibility matrix and was then
  **run**, because Ido asked whether the project should simply move to 25.

  **Observed 2026-08-19.** `gradlew --version` on the `jbr` **succeeds** — Gradle 8.10.2
  launches on JDK 25 and prints *"Support for Java 23"* as its release highlight. So the
  cheap check passes and is not the test. Configuration is:
  `gradlew help -Dorg.gradle.java.home=<jbr>` fails in 20 s, and the entire body of the
  error is the literal string `25.0.2` — a version parser giving up on `25`.

  Recorded here in that shape deliberately: the earlier draft of this entry said Gradle
  *"does not run on 25"*, which a later session would have disproved in one command and
  then distrusted the rest of the paragraph.

There is **no JDK 21 on this machine** (`C:\Program Files\Java\` absent too). The brief now
says: install Temurin JDK 21, then repoint the pin. It also says explicitly **not** to
"fix" this by bumping Gradle/AGP to accept 25 — that is a toolchain upgrade, not machine
setup, and it is Ido's call.

## Also this session

- Deleted the merged remote branch `feat/goalpilot-implementation` on Ido's explicit
  instruction. Only `refs/heads/main` remains on the remote.
- Confirmed `origin/main` moved to `1ff8a5e` mid-session — the JARVIS session
  `graphify-opus5-both-repos` committed and pushed its graphify wiring into this repo's
  tree and has since released. Its files were never touched here.

## 🧪 Tests

**No build was run, and none was possible** — see item 4; there is no usable JDK on this
machine yet. No layer applies: this session changed only Markdown under `sessions/` and
`CHANGELOG/`, no Kotlin, no `functions/`, no Firestore rules.

Verification was by re-measurement against HEAD rather than by test: every count and line
number in the two re-verified blocks was produced by `grep`/`find` against the working
tree at `1ff8a5e`, not copied from the briefs being corrected.

## Addendum — the JDK blocker is fixed, and the repo was never wrong

Ido asked whether to move the project to JDK 25 since Android Studio already ships one,
then told this session to fix the blocker rather than leave it to `/kickoff
new-machine-checkup`.

**Answer to the question: no.** Measured above — configuration dies on 25 with a bare
`25.0.2` as the whole error. And it would not have been a JDK swap: Gradle 8.10.2, AGP
8.7.3, Kotlin 2.0.21 and KSP 2.0.21-1.0.28 are four coupled upgrades, which is the
highest-risk change available under a *"functionality before Hebrew, time is short"*
constraint — post-upgrade codegen failures read as bugs in your own code.

**The fix, and the part worth remembering: no repo file needed changing.**

- `winget install EclipseAdoptium.Temurin.21.JDK` installed **21.0.12.8** — at *exactly*
  the path `gradle.properties:22` already pinned. The pin was correct; the directory was
  missing. Machine `JAVA_HOME` was correct too, for the same reason.
- So the earlier framing (*"fix or remove the pin"*) was the wrong remedy for a right
  diagnosis. `org.gradle.java.home` **does** override `JAVA_HOME`, which is why the
  original brief's `jbr` instruction could never have worked — but the pinned value was
  never the fault.
- One stale `.gradle\8.10.2\dependencies-accessors` workspace, left behind by the failed
  JDK 25 attempt, then produced the Windows *"Could not move temporary workspace"* error.
  Deleted; it regenerates. This is the lock class `CLAUDE.md` already documents.
- **Verified:** `gradlew help` → `BUILD SUCCESSFUL in 1m 24s`. The Android project
  configures end to end.

**Not done, and deliberately left to `/kickoff new-machine-checkup`:** `assembleDebug`.
Configuration succeeding is not compilation succeeding, and the real build takes the
`#gradle-daemon` singleton plus a large first-run download — that is the kickoff's item 1,
which now says so.
