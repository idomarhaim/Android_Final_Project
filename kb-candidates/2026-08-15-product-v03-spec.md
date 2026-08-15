# KB candidates — `product-v03-spec`, 2026-08-15

Session: `product-v03-spec` · repo `c:\Dev\Android_Final_Project` · branch `feat/goalpilot-implementation`
Brief: `sessions/done/product-v03-spec.md` · Map: [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)
Mode: `AUTO MODE` · Commits: `e416d61`, `612431a`, `d271355`

**Each entry stands alone.** No transcript is a source: everything needed to write the page is below,
including what was rejected and why.

> **Partially drained 2026-08-15 by session `c22-measure-proposal`.** Entry **2** (*a hand-off between
> two tickets can evaporate when each counts the set differently*) was ingested into
> `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` **§11** and is removed from this file; the journal
> entry in `C:\Dev\JARVIS\kb\log\2026-08-15.md` is the cross-repo tie. Entry **1** survives below with
> its **original number**, moved into *Standing — always-ask* so the next drain does not re-reason
> about it.

---

## Standing — always-ask

Entries here are **not** drainable under `AUTO MODE` and are not to be re-adjudicated by the next
session that opens this file. They need Ido.

## 1 · A generated cross-reference is a claim about a computation, and reading it cannot check it

- **Claim.** When a document's internal links are *computed* from its own headings — Markdown
  anchors, generated tables of contents, `#L42` line refs, glossary back-links — **the links cannot be
  verified by reading, because the failure is in a transformation you are not running.** Re-implement
  the transformation and diff, or do not claim the links work.

- **The instance.** `docs/PRODUCT_v0.3.md` shipped with 50 internal anchors. **34 of them were wrong on
  the first write**, and every one *looked* right: they were derived by hand from the header text, which
  is exactly what GitHub does — except GitHub's slug rule turns `## 1 · The model` into `1--the-model`
  (the `·` vanishes and leaves *two* spaces) and `### 1.1 There is one kind` into `11-there-is-one-kind`
  (the `.` vanishes and leaves *none*). **A one-character difference, invisible at every reading, in
  34 places.** A 20-line script that recomputed the slug over the file's real headers and set-differenced
  it against the file's real links found all 34 in one run, and found the 35th introduced later by a
  section rename.

- **Why this matters, and what it rejects.** The obvious remedy is *"be careful"* or *"spot-check a
  few"*, and both fail for the same reason: the error rate is near-uniform across the links, so a
  spot-check of five has a good chance of passing while two-thirds of the document is broken — the
  same shape as `C10`'s rejected *verify-by-sampling* (a clean sample proves a low rate and identifies
  nothing). The second rejected remedy is *"use a tool"*: no linter was configured here, and writing
  the 20-line check **is** the tool, so waiting for one is waiting for something cheaper than what you
  already declined to do.

- **The generalisation, which is where the value is.** This is the same family as
  `kb/dev/` 's already-committed *an agent that cannot see its own output is guessing* — but it is the
  **non-visual** member of that family, and that matters because the existing statement is scoped to
  *"when the acceptance criterion is visual"* and a Markdown anchor is not visual at all. The
  discriminator is **not** rendering; it is **whether the artefact you produced is the artefact that
  will be consumed.** A rendered screenshot and a recomputed slug are two instruments for the same
  gap. Anything a **consumer** computes from your output — a slug, a checksum, a JSON parse, a
  regex match, a link resolution, an import path — is unverified until you compute it too.

- **Destination.** `C:\Dev\JARVIS\kb\dev\` — most likely as a section on the page that
  `kb-candidates/2026-08-12-c12-charts-presentation.md` entry 1 is blocked on creating, since that
  entry states the visual half of the same claim and a single page saying *"verify by re-running the
  consumer's computation, whether that consumer is an eye or a slug function"* is stronger than two.

- **Anchors.** `docs/PRODUCT_v0.3.md` · commits `612431a` (the 34) and `d271355` (the 35th) ·
  `CHANGELOG/2026-08-15/product-v03-spec.md` §🧪 Tests, both units.

- **Supersedes.** Nothing. It **narrows** the scope of the parked visual claim rather than replacing it.

- **Status.** ⛔ **BLOCKED, and by an existing gate rather than a new one.** It belongs on the page
  `2026-08-12-c12-charts-presentation.md` entry 1 would create, and that entry is `rules/`-shaped and
  always-ask in both modes. **The group drains together or not at all** — that file says so explicitly —
  and this entry joining it does not change the gate, it only makes the question Ido is owed slightly
  larger and slightly better posed: *is "re-run the consumer's computation on your own output" a change
  to how agents work, or an ordinary KB page?* **`AUTO MODE` does not reach it.**

  **Re-confirmed 2026-08-15** by `c22-measure-proposal` while draining entry 2: the gate is unchanged
  and this entry was **not** re-adjudicated. One observation worth carrying to whoever asks Ido — the
  two new pages written that day, `elevation-is-not-a-fill` and `describing-is-not-exhibiting`, are
  both **visual-half** members of exactly this family and went in as ordinary KB pages without
  difficulty. That is evidence the family is page-shaped rather than `rules/`-shaped, but it is
  **evidence, not a decision**, and it does not lift the gate.
