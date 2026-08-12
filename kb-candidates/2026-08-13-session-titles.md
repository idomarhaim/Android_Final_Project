# KB candidates — `session-titles`, 2026-08-13

## 1. A Claude Code session's title is writable state, and it is the only handle the VS Code extension exposes

- **Claim:** In the Claude Code VS Code extension (read at v2.1.228), the session picker
  filters on **title and git branch only** —
  `filter(s => LN(s).includes(q) || s.gitBranch.value?.includes(q))`, `LN(s) = s.summary.value` —
  never on the session UUID and never on transcript content. No contributed command accepts a
  session id, and the extension registers no URI handler. So a cross-session reference
  ("see `c6-log-progress`") is only clickable if the label is **in the title**.
  The title itself is one appended JSONL record on the session's own transcript,
  `{"type":"custom-title","sessionId":…,"customTitle":…}`, resolved
  **`customTitle` → `aiTitle` → `lastPrompt` → `summary`** with head *and* tail scanned.
- **Why:** Ten wayfinder sessions on one map, five of them opened with a byte-identical
  first message, produced five indistinguishable picker rows — the fallback chain reaching
  `lastPrompt`. Rejected alternatives: the CLI's `claude --resume <uuid>` (works, but it is a
  terminal pane, not the panel Ido works in) and searching transcript content (the filter
  does not read it).
- **Destination:** `kb/dev/` — a page on Claude Code session identity, alongside the existing
  agent-topology material. It bears directly on the JARVIS board convention, since the board
  label is the string a human then has to find in an IDE.
- **Anchors:** `~/.vscode/extensions/anthropic.claude-code-2.1.228-win32-x64/webview/index.js`
  (filter, rename UI), `extension.js` (`renameSession`, record shapes). Version-stamped: this
  is read from a shipped bundle and can change between releases.
- **Supersedes:** nothing.
- **Status:** pending.

## 2. Attribute a session by what it *wrote*, not by what it mentions

- **Claim:** To identify which transcript belongs to which JARVIS session label, count
  **Write/Edit tool calls against `CHANGELOG/<date>/<label>.md`**, never mentions of that
  path. Mentions are dominated by *reading* — any session that inspects a sibling's changelog
  outranks the session that wrote it.
- **Why:** The mention-count heuristic confidently labelled the session that had merely read
  `c6-log-progress`'s changelog as being `c6-log-progress`. Cross-checked and corrected by the
  session's own **claim commit hash**, which is unambiguous. Generalises past this repo:
  agent-authored artifacts carry provenance in the tool call, not in the prose.
- **Destination:** `kb/dev/` — same page as entry 1, or the session-forensics page if one
  exists.
- **Anchors:** none in-repo (the tool lives in the session scratchpad).
- **Supersedes:** nothing.
- **Status:** pending.
