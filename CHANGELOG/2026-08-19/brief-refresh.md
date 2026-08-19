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
  **8.10.2**. **Inferred** (from the published compatibility matrix, not run here): 8.10.2
  does not run on JDK 25, and AGP 8.7.3 / Kotlin 2.0.21 / KSP 2.0.21-1.0.28 are not
  qualified on it either. **Untested:** nobody has attempted the build on 25 — the pin
  above fails first, so the second reason has never been reached.

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
