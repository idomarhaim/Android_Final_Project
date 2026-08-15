---
session: c21-offline-story
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
created: 2026-08-15
mode: AUTO MODE
---

# KB candidates — `c21-offline-story`, 2026-08-15

Filed while resolving [#43 · `C21`](https://github.com/idomarhaim/Android_Final_Project/issues/43)
on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

**Rewritten down to its survivor 2026-08-15, not deleted.** Entry 1 is drained; entry 2 is new and
parked. Original numbering kept.

---

## 1 · ~~Key a disclosure to the variable that moves the fact~~ — **DRAINED 2026-08-15**

**Ingested** into `C:\Dev\JARVIS\kb` as the new page `dev/stale-is-a-data-property.md`. Index row
added; journal entry in `kb/log/2026-08-15.md` is the cross-repo tie. `Check-KbLinks` **CLEAN at 75
pages**. Nothing superseded.

**The width limit it declared was honoured, and that is worth one line.** The entry named three
pages its own bundle check had **not** opened — `one-metric-and-its-mechanism.md`,
`disclosure-is-not-a-gate.md`, `render-site-vs-query-site.md` — and said the ingesting session must
open them first. All three were opened; none carried the claim, and two became *Adjacent* links.
This is the counter-example to the three bundle-check width failures recorded this week: a check
that states what it could not do is not a failed check.

---

## Standing — always-ask

## 2 · The `rules/` half of the *look at your own output* claim ⛔

- **Claim.** One bullet under 🧪 *Testing discipline* in `user-rules/my-rules.instructions.md`, making
  the committed KB page **fire by default** instead of being looked up: *verify by re-running whatever
  will consume your output — never by reading it.* Visual criterion → render and look, between
  revisions and before spending Ido's turn. Computed consumer (slug, anchor, checksum, JSON parse,
  import path) → recompute and diff. Then check the instrument itself on the hardest input it exists
  for.

- **Why this is a separate entry rather than part of entry 1.** Ido was asked whether the claim was a
  `rules/` change or an ordinary KB page and answered **both — a KB page plus a one-line `rules/`
  pointer**, which splits one candidate into two items with two different gates. The page half is
  committed (see entry 1 and `dev/look-at-your-own-output.md`, whose §5 carries the non-visual member
  from `product-v03-spec`). This half is not, and it is not blocked on *whether* — only on the
  **wording**.

- **What is already settled, so no session re-adjudicates it.** That it belongs in `rules/` **at all**
  is Ido's decided answer, 2026-08-15, and is not reopened. That the mechanism, the incidents (three
  prose revisions vs ten machine rounds; 34 wrong anchors of 50) and the two failing remedies
  (*be careful*, *use a tool*) live on the KB page rather than in the rule is also settled — the
  bullet is a **pointer**, and any draft that grows into a paragraph has lost the shape Ido chose.

- **Destination.** `C:\Dev\JARVIS\user-rules\my-rules.instructions.md`, 🧪 *Testing discipline*, after
  the template-pointer line. **The exact wording is already drafted there, uncommitted and unsynced**,
  per the 🎬 rule's *draft → walkthrough → write* order, and declared on that repo's `SESSIONS.md`.
  A session that finds the file dirty with a *render and look* bullet has found that draft and must
  not commit it; `git checkout -- user-rules/my-rules.instructions.md` is the correct disposal.

- **Anchors.** `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` (the whole mechanism) ·
  `C:\Dev\JARVIS\kb\log\2026-08-15.md`, the 19:0x and 19:3x entries · JARVIS `ed6a69e` (pages) ·
  `docs/prototypes/tools/README.md` in this repo (the harness the practice came from).

- **Supersedes.** Nothing. It adds a bullet; no existing rule text is rewritten. Worth stating,
  because that is what keeps this a *walkthrough* question rather than also a deletion.

- **Status.** ⛔ `awaiting 🎬 — "ingest first" chosen 2026-08-15`. Not a decline and not a waiver:
  Ido deferred the judgment and took the ingest first, so **nothing ships without a run** and the
  mechanical half stays owed to whichever answer comes second. The offer has been re-made in the reply
  reporting this ingest.
