# KB candidates — `product-v03-spec`, 2026-08-15

Session: `product-v03-spec` · repo `c:\Dev\Android_Final_Project` · branch `feat/goalpilot-implementation`
Brief: `sessions/product-v03-spec.md` · Map: [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)
Mode: `AUTO MODE` · Commits: `e416d61`, `612431a`, `d271355`

**Each entry stands alone.** No transcript is a source: everything needed to write the page is below,
including what was rejected and why.

---

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

---

## 2 · A handoff between two tickets can evaporate when each counts the set differently, and nothing fires

- **Claim.** On a decision map, a ticket that hands work to another ticket **by count** — *"this is the
  fifth X for `#N` to handle"* — has no mechanism that notices when `#N` arrives at *five* by a
  different route. **Both tickets close, both look complete, and the handoff is simply gone.** A
  handoff must name **the thing**, never its ordinal, and a map's completeness check must be *every
  named handoff resolves*, not *every ticket closed*.

- **The instance.** [`C7` #14](https://github.com/idomarhaim/Android_Final_Project/issues/14) specced an
  agent that proposes a measure for an unmeasured goal and handed it on verbatim: *"This is a **fifth AI
  feature** for [`C11b` #30] to write an output format for."*
  [`C11b` #30](https://github.com/idomarhaim/Android_Final_Project/issues/30) resolved under the headline
  *"one wide call per feature, **five schemas not four**"* — and its five is a **different** five: its
  §1 re-inventories the features, finds `classify` is *"an AI feature the map never named"*, and counts
  **that** as the fifth, because `C2`'s task typing had folded into `estimate` as a field rather than
  standing as its own schema. §3 then writes **four** schemas and none of them is `C7`'s.

  **Both tickets are closed and both are internally correct.** `#30` never says the measure proposal was
  considered and dropped; it never mentions it. The map's index line for `#30` reads *"five schemas not
  four"* and is true on its own terms. **Nothing in the ticket graph could fire**, because the map's
  completeness rule is *no ticket is open* — which was satisfied.

- **Why this matters, and what it rejects.** Two remedies look right and are weaker than they seem.
  **(a) "Link the tickets"** — they *were* linked; `#14` names `#30` by number and `#30`'s §1 table
  cites `C1`, `C2`, `C8`, `C10`. Linkage was present and did nothing, because what was lost was not the
  *reference* but the *obligation*. **(b) "The receiving ticket should re-read its senders"** — `#30`
  did exactly that and still missed it: it re-inventoried from **the code** (*"read against the code,
  the post-map inventory is…"*), which is the more rigorous move and is precisely what replaced `C7`'s
  entry with `classify`'s. **Rigour at the receiving end is what erased it**, which is why the fix has
  to sit at the sending end.

  So the remedy is a property of **how a handoff is written**: *"`#30` owes a format for the measure
  proposal"* survives a re-inventory; *"this is the fifth"* does not, because *fifth* is a fact about a
  set the receiver is entitled to recount.

- **The second half, which is the reusable one.** The gap was found by writing the **destination
  artifact** — the spec — and not by any ticket, any review, or the map's own closure test. **The
  artifact that consumes every decision is the only place a lost handoff between two of them becomes
  visible**, which is an argument for writing it *before* the map closes rather than after, and for
  treating *"every handoff named in a resolution resolves somewhere"* as the map's real completeness
  check.

- **Destination.** `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` — a new section. That page already
  carries §9 and §10 from this repo's sibling sessions and is the right home; this is a **charting**
  failure, not a GoalPilot product fact.

- **Anchors.** `#14`'s resolution comment (the handoff sentence, one grep match) · `#30`'s resolution
  §1 and §3 · `docs/PRODUCT_v0.3.md` §10.1 · `#12`'s body, *Destination reached* block ·
  `CHANGELOG/2026-08-15/product-v03-spec.md`.

- **Supersedes.** Nothing. Adjacent to `decision-map-charting.md`'s existing material on what a map's
  index line may and may not assert.

- **Status.** 🟢 **`AUTO MODE`-eligible** — a new section on an existing page, superseding no standing
  claim, and this session's own finding. **Not drained by this session**, and the reason is scope
  rather than a gate: ingesting it is a **cross-repo visit to `C:\Dev\JARVIS`**, which owes a row on
  *that* board and its own read-claim-edit-journal-lint cycle, and this session is ending on a question
  reserved to Ido (whether `#12` closes). **It is work left, not a blocked item** — the next session
  that opens this folder should take it, and nothing here needs reconstructing to do so.
