# `graphify-wiring` — 2026-08-19

> **Summary:** This repo joins the graph layer FP_DEMO has had since 2026-07-18 — `/graphify-out/` gitignored, `graphify-android-final-project` MCP server, AGENTS routing block, post-commit freshness hook, and a first labeled graph: **2,339 nodes / 4,373 edges / 152 communities**, named on **Opus 5** via the `claude-cli` backend (subscription, never an API key).

**Branch:** `main`.
**Scope:** wiring + docs only — no app code, no resources, no build files.

---

## What was wired

Per the B11 in-tree pattern (canonical: `C:\Dev\JARVIS\kb\dev\tool-adoptions.md`):

- **`.gitignore`** — `/graphify-out/`, a regenerable local cache, never source.
- **`.mcp.json`** (new) — `graphify-android-final-project` serving
  `graphify-out/graph.json` to both agents. Generated with a JSON serializer
  after a hand-written first attempt came out with collapsed escapes and failed
  `json.load`; the parse is the check, not the reading.
- **`AGENTS.md`** — `JARVIS:BEGIN knowledge-graph` block: consult the graph
  before grepping, freshness/rebuild commands, and the policy line.
- **Hooks** — `graphify hook install` (post-commit + post-checkout), so graph
  *structure* tracks HEAD for free. Labels are not hook-refreshed.

## What the first graph says

Kotlin extraction was the open question and it is answered: tree-sitter reads
the whole tree — `app/`, `functions/`, and the test suites — with no special
configuration. The Opus 5 labels are specific enough to navigate by on the first
run: *Health Data Sync*, *AI Task Recommendations*, *Locale Context Tests*,
*Firestore DTO Mapping*, *Auth Repository*, *Widget Snapshot Charts*.

## One trap worth knowing before re-labeling

`graphify label … --model=claude-opus-5` **does nothing** — the flag never
reaches the `claude-cli` backend, which reads the machine-wide
`GRAPHIFY_CLAUDE_CLI_MODEL` environment variable instead (pinned to
`claude-opus-5` on 2026-08-19). The flag form fails silently: the run succeeds
and the labels look fine. Full mechanism in the JARVIS entry for the same day.
