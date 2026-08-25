# KB candidates — `docs-currency-refresh`, 2026-08-25

Written per the memory-promotion rule. **Normal mode**, so this is a proposal: nothing is
ingested without Ido's word.

---

## 1 · A presence-check guard makes the *false-assertion* half of doc drift harder to see, not easier

- **Claim.** `DocsCurrencyTest` guards `docs/` by asserting *the code contains X, so the doc must
  name X*. It was green on `HEAD` while `docs/SETUP.md` told a reader to put OAuth in **Testing**
  mode and `docs/OPERATIONS.md`, in the same repo, spent a page on why the project **left** Testing
  mode. Of the 23 findings this session repaired, the guard could reach **seven** — every one of
  them an omission. The three that would actually mislead a reader into a wrong action were all
  sentences that were simply **wrong**, and no presence check reaches a wrong sentence.
- **Why.** The guard's own KDoc says this in as many words, which is the interesting part: the
  limit was known, written down, and *still* the docs sat four days stale behind a green build.
  Naming a limit does not create a habit of compensating for it. What actually found the false
  assertions was **reading two documents against each other**, which nothing automates and nothing
  schedules. The generalisable shape: **a guard that covers one half of a defect class changes
  where people look, and they stop looking at the half it does not cover.** The false reassurance
  was the strongest argument raised *against* building the guard, it was overruled for good
  reasons, and this is the first measurement of what it cost.
- **Rejected:** *widen the guard to catch false assertions* — an oracle for "is this sentence
  true" is the whole problem; *delete the guard* — it caught seven real omissions this session
  and would have caught more had it existed sooner.
- **Destination.** `kb/dev/` — extends `look-at-your-own-output.md`, or a new page beside
  `product-copy-describes-code.md`, whose mechanism this is one layer over.
- **Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/docs/DocsCurrencyTest.kt` (its own
  *"WHAT THIS GUARD DOES NOT COVER"* block); `CHANGELOG/2026-08-25/docs-currency-refresh.md`;
  `CHANGELOG/2026-08-24/docs-currency-guard.md`.
- **Supersedes.** Nothing. Adds a measured instance to `product-copy-describes-code.md`.
- **Status.** Ready.

---

## 2 · A document that repairs a claim in place will contradict itself unless the *status line* is repaired too

- **Claim.** `docs/RELEASING.md` section 2.1a opens with **"Status, 2026-08-21: RECOVERED"** and
  then, four paragraphs later, still says *"the tag route is the only one that can produce an
  installable update"* and *"the key is now in **one** place"* — both pre-recovery text, both
  false on the day they were read, both sitting **below** a heading that had been correctly
  updated. The repairing session updated the *headline* and left the *body* it was the headline
  for.
- **Why.** This is a specific failure mode of the house style that puts a dated `Status:` or
  `⚠️ CORRECTED` box at the top of a section: the box is satisfying to write and reads as though
  it has done the work, so the paragraphs it contradicts survive. The reader who scrolls past the
  box — which is every reader who arrives by search or by anchor link — gets the old answer with
  no signal at all. **The check is cheap and nobody runs it: after adding a status box, re-read
  the section it heads to its next heading and diff every claim against the box.**
- **Rejected:** *delete the stale paragraphs* — the corrections turned out to be the useful part
  (one of the two sentences is still true whenever the keystore is absent, which is exactly the
  condition the section exists for), so both were repaired in place with the correction named.
- **Destination.** `kb/dev/` — `claim-provenance.md` is the closest standing page; this is its
  failure mode at the *document* scale rather than the *sentence* scale.
- **Anchors.** `docs/RELEASING.md` section 2.1a, before and after this session's commit.
- **Supersedes.** Nothing.
- **Status.** Ready.

---

## 3 · The trap a document warns about fires on the tooling that writes the warning

- **Claim.** The Python heredoc written to add the *"a backslash is an escape character in
  `local.properties`"* warning to `docs/SETUP.md` died with `truncated \UXXXXXXXX escape` —
  because the warning text contains the very path that demonstrates it. Same session, same
  paragraph, one layer up.
- **Why.** Not a coincidence and not merely funny: **the class of content that needs a warning is
  the class of content that breaks the tools handling it.** This repo already records three
  members of the family in `CLAUDE.md` — `--` inside an XML comment, `/*` inside a Kotlin KDoc
  (which nests), a backslash in a `.properties` file — and the fourth is now *writing about any
  of the first three*. The practical consequence: reach for a raw string / literal heredoc by
  default when the content is **about** an escaping trap, rather than after the first failure.
- **Rejected:** *escape the path in the warning* — that makes the warning show the escaped form,
  which is not the form the reader will type.
- **Destination.** `kb/dev/prose-punctuation-is-syntax.md` — a fourth instance, and the first
  self-referential one.
- **Anchors.** `docs/SETUP.md` section 3 (the paragraph now says so inline);
  `CHANGELOG/2026-08-25/docs-currency-refresh.md`.
- **Supersedes.** Nothing. Extends the existing page.
- **Status.** Ready.

---

## 4 · `file-history` records cannot identify a session's transcript; the *write* records can

- **Claim.** The liveness procedure says to find a session's transcript by the `file-history-*`
  records naming its label. For `CHANGELOG/2026-08-24/docs-repair.md` that grep matched **14** of
  67 transcripts, including this session's, which had never touched the file. Filtering instead on
  a **tool call whose `file_path` is that document** matched exactly **one** — the real owner, and
  its last turn dated the session cleanly as ~39 h quiet.
- **Why.** `file-history-snapshot` records appear to cover files a session had in scope, not files
  it wrote, so the recommended filter's precision degrades as the file becomes widely read — which
  is precisely what happens to a changelog or a board. `ai-goal-onboarding` hit this on 2026-08-24,
  recorded *"matches 9 sessions … unresolved counts as live"* on the board, and correctly declined
  to act; the narrower filter would have resolved it in one command. **This is the instrument
  degrading silently on the hardest input it exists for**, and the failure direction is
  *flattering*: it reports a dead session as unresolved, so its stale claim stands and keeps
  blocking work.
- **Rejected:** `grep -l <label>` — already rejected by the standing rule, and for the same reason
  in a worse form.
- **Destination.** `rules/agent-topology-and-model-routing.md` section 5.3(c) — **a `rules/`
  destination, so this one is always-ask in both modes and gated by the 🎬 walkthrough rule. It is
  NOT proposed for ingest here; it is parked.**
- **Anchors.** `SESSIONS.md`, `ai-goal-onboarding`'s note under `docs-repair`'s row;
  `CHANGELOG/2026-08-25/docs-currency-refresh.md`.
- **Supersedes.** Narrows the *"find it by its `file-history-*` records"* step. Does not touch the
  *never `stat` the file* or *latest last turn across all matches* clauses, both of which held.
- **Status.** **PARKED — `rules/` destination, needs Ido's word and a walkthrough decision.**
