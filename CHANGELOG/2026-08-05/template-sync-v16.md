# Changes — 05/08/2026 — session `template-sync-v16`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** Mechanical template sweep — `general.instructions.md` v14 → v16, `new-changelog-entry.prompt.md` v3 → v4, `AGENTS.md` v12 → v14; no decisions taken here.

## 🔁 Sync

Verbatim projections refreshed by `Update-TemplateConsumers.ps1` from the JARVIS
template library. No decision was taken in this repo — the decisions were made
where the templates were edited, and this copy is bookkeeping
(`C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md`, *mechanical sync* row).

This repo was **two versions behind** on `general.instructions.md`: the 2026-08-04
sweep correctly refused it because the `challenges` session held a dirty tree with
work in flight. That tree is clean now, so v14 → v15 → v16 landed in one pass.

| File | Version | What changed upstream |
|---|---|---|
| `.github/instructions/general.instructions.md` | v14 → v16 | v15: `AUTO MODE` gains a **third gate** — KB candidates ingest without asking at the commit trigger, riding in that unit's commit, each one reported. v16: the changelog day index becomes **derived content**, and every changelog file gains a mandatory `> **Summary:**` line on line 4 — the day row is a link, a session count, and one line per session (each session's own summary, in its own file) instead of one shared cell every session appended a paragraph to. Prose for a whole day goes to `CHANGELOG/YYYY-MM-DD/SUMMARY.md`. |
| `.github/prompts/new-changelog-entry.prompt.md` | v3 → v4 | Matches v16: writes the summary line, and step 5 becomes *run the generator where one exists, otherwise append only your own segment* — never paste prose into the index. |
| `AGENTS.md` | v12 → v14 | v13: the **lease-the-commons** bullet (§5.2 — files every session touches for seconds are leased via `.jarvis/locks/`, not claimed on the board; a blocked session waits and auto-resumes instead of asking). v14: the *Suggested exit* column names the **end of a session**, not the artifact carrying the handover — `/handoff` is Form A and only on the user's keystroke; otherwise committed briefs plus `/kickoff <slug>`. |

§5.2 is worth a look in this repo specifically: it is the one with real
concurrency (three sessions on 2026-08-04, an emulator and a Gradle daemon as
singletons). The lease covers **paths**, and named singletons like `#emulator`
and `#gradle-daemon` are lockable the same way — but the board still owns the
device: a lease is seconds-to-minutes, a device is held for a session.

Normative text lives in `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md`
§5.2 and §4; this repo carries the projection, never a restatement.

## 🧪 Tests

No test layer applies: this is a byte-for-byte copy of files whose content was
verified in the library repo, and it touches no Kotlin, no Gradle config and no
Firestore rules. Nothing was built and no device was used, so the `#emulator` and
`#gradle-daemon` singletons were never taken.

`Update-TemplateConsumers.ps1` overwrites **only** a file whose content matches a
released version in JARVIS's git history (blob-SHA comparison), which is the check
that makes the copy safe without diff-and-confirm, and it refuses any repo with a
dirty tree.
