# Changes — 06/08/2026 — session `template-sync-v17`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** Mechanical template sweep — `general.instructions.md` v16 → v17, `AGENTS.md` v14 → v15; no decisions taken here.

## 🔁 Sync

`Update-TemplateConsumers.ps1` from `C:\Dev\JARVIS\templates`:

| File | Version |
|---|---|
| `.github/instructions/general.instructions.md` | v16 → v17 (verbatim copy) |
| `AGENTS.md` | v14 → v15 (`routing` block replaced; `knowledge-graph` opted out) |

Both carry the same library change: **KB candidates stop living in session memory
and become a committed file** — `kb-candidates/YYYY-MM-DD-<session-label>.md`, one
per session, per repo. Written in every mode (`AUTO MODE` gates whether the list
*drains*, never whether it *exists*); drained at the commit trigger, on any 🔀
split signal, on `/handoff`, on `/kickoff`, **and above all listed by every session
before its first unit of work** — the file existing is the trigger, exactly as with
`SESSIONS.md`. Each entry stands alone and the originating session's chat history
is **not a source**.

Decision and rationale live where the template was edited:
`C:\Dev\JARVIS\CHANGELOG\2026-08-06\kb-candidates.md`, commit `7a2f2dd`. Per
`rules/scale-adaptive-ceremony.md` a mechanical sync gets one `## 🔁 Sync` note,
not a changelog round.

No board row was claimed: a claim created and cleared inside one commit protects
nothing (same derivation as the 2026-08-05 v16 sweep here).

## 🧪 Tests

None run — verbatim file copies with no executable surface, no Gradle daemon
touched and no emulator claimed. The repo's test layers are unaffected by this
commit.
