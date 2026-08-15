<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->

# CLAUDE.md

This project's source of truth for AI agents is [AGENTS.md](AGENTS.md). Copilot also loads `.github/copilot-instructions.md`.

Read [AGENTS.md](AGENTS.md) first. Anything below this line is **Claude-Code-specific** and not relevant to other agents.

---

- Build/test from a shell that has `JAVA_HOME` set to JDK 21 (Gradle is also pinned to it via `gradle.properties`). As of 2026-08-15 the machine `JAVA_HOME` is correct — `jdk-21.0.12.8-hotspot` — but **`java` on `PATH` is still JDK 17**, from the machine `PATH`, which needs admin to reorder. That only bites tools that read `PATH` instead of `JAVA_HOME`; `firebase-tools` is one, and `firestore-tests/run-tests.mjs` works around it. See the JDK pitfall in [AGENTS.md](AGENTS.md).
- On Windows, KSP/`.gradle` sometimes fail with "Could not delete/move …" locks. Re-run, or `rm -rf app/build/generated/ksp` and rebuild — it is not a code error.
- Pipe Gradle through `tail` only with `${PIPESTATUS[0]}` to read the real exit code (the pipe's exit is `tail`'s).
