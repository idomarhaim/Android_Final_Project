<!-- SOURCE: user-template v1; do not edit in-project, edit user-level then re-sync -->

# CLAUDE.md

This project's source of truth for AI agents is [AGENTS.md](AGENTS.md). Copilot also loads `.github/copilot-instructions.md`.

Read [AGENTS.md](AGENTS.md) first. Anything below this line is **Claude-Code-specific** and not relevant to other agents.

---

- Build/test from a shell that has `JAVA_HOME` set to JDK 21 (Gradle is also pinned to it via `gradle.properties`). The machine default `JAVA_HOME` is JDK 25 and AGP rejects it.
- On Windows, KSP/`.gradle` sometimes fail with "Could not delete/move …" locks. Re-run, or `rm -rf app/build/generated/ksp` and rebuild — it is not a code error.
- Pipe Gradle through `tail` only with `${PIPESTATUS[0]}` to read the real exit code (the pipe's exit is `tail`'s).
