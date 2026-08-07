# `c11a-free-model-probe` — the free model gets measured instead of assumed

**Session:** `c11a-free-model-probe` · **Invoked as:** `/wayfinder 12 16` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal · **2026-08-08.**

One wayfinder ticket resolved, as the skill requires — **never more than one per
session**. [#16 · `C11a`](https://github.com/idomarhaim/Android_Final_Project/issues/16)
is closed; the map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)
carries its gist and its fog is one patch sharper.

**No code was written.** This map's standing rule is *plan, don't do* — no ticket on
it ships code — and it held: `functions/src/index.ts` was read for its prompts and
its model pin and **not edited**. The probe harness was a throwaway in the session
scratchpad, deliberately not committed; its full method and every prompt live in the
research asset so the run is reproducible without it.

## What exists now that did not before

| | |
|---|---|
| Research asset | [`docs/research/2026-08-08-free-model-format-probe.md`](../../docs/research/2026-08-08-free-model-format-probe.md) *(new — first file in a new `docs/research/` folder, alongside `c9d-calendar-scopes`' asset)* |
| Ticket closed | [#16](https://github.com/idomarhaim/Android_Final_Project/issues/16) `C11a`, with a full resolution comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line in *Decisions so far*; the "AI text per-language or translated" fog patch rewritten as half-answered |
| New tickets | **none** — see *What was deliberately not done* |

## What was measured

**248 live calls** to `openai/gpt-oss-20b` on GROQ's free tier, at the production
temperature of `0.7`, using the **verbatim system prompts** from
`functions/src/index.ts`, in Hebrew and English, with opaque 20-character
Firestore-style ids so "did it echo the id it was handed" is a real test rather than
a guessable one.

The pin was confirmed live **before** any sampling, as the ticket required — a
retired GROQ id fails silently into the local fallback, so a probe against a dead pin
measures nothing.

Three arms, and the third exists because the second could not answer its own question:

1. **Arm 1 (170 calls)** — production shapes under `response_format: json_object`,
   plus a control arm with **no `response_format` at all**.
2. **Arm 2 (60 calls)** — the hardest shape under strict `json_schema`, which GROQ
   turns out to support for this model.
3. **Arm 3 (18 calls)** — adversarial schemas demanding what the model would never
   volunteer, **each paired with a prose control**.

## The findings

- **Format is not where the risk is.** 170/170 replies parsed as clean JSON first
  try; 168/170 valid on every field. Zero HTTP errors, zero 429s, every call
  `finish_reason: stop`. "Prose around the JSON" never occurred once — *including in
  the 20 calls that sent no `response_format`.*
- **Hebrew is not worse. It is marginally better** — both failures in the entire run
  were English.
- **The one failure mode is silent id corruption.** A goal id supplied as
  `8xKq2mN4vRt7pLwZaB1c` came back as `8xKq2mN4vRt7pLwZaB` — a plausible non-empty
  string that passes every check except membership in the list sent alongside it.
  Enum values written *into the prompt* were perfect 50/50. Structural obedience is
  near-total; **referential** obedience is not.
- **One wide call beats three narrow ones** — 1.7x faster, ~30% cheaper in tokens,
  and three requests lighter against the 30-RPM ceiling that actually binds. Split a
  call for differing *fallback behaviour*, never for format.
- **Unasked, and the most consequential result: the envelope is stable and the
  numbers inside it are not.** Same title, same prompt: 180 minutes then 120. Same
  task in Hebrew rather than English: 45 minutes rather than 25. The time-allocation
  chart is built on that field and presents it as measurement.
- **Strict `json_schema` is available but is not what makes this work.** Arm 2 was
  60/60 — and arm 3's **prose control obeyed the same absurd 1000–2000 range with no
  schema at all**, so this probe cannot separate enforcement from compliance. Schema
  buys a guarantee that survives a model swap; it does not buy reliability this model
  lacks.
- **For `C15`: the model does not write Hebrew unless told to.** 0/10 Hebrew coach
  messages given entirely Hebrew goals — but 3/3 at full script share when one prompt
  line asks. Meanwhile `suggestedNewGoalTitle` comes back Hebrew 13/14 and is
  **stored as the user's own goal**, so AI-authored content already sits on the
  content side of that ticket's chrome/content boundary.

## 🧪 Tests

**No suite was run, and none is applicable.** No Kotlin, Gradle, `firestore.rules`,
or Cloud Functions file was created or modified — the only repo writes are Markdown
(this file, the research asset, the KB candidate list) plus this session's
`SESSIONS.md` row. There is no test layer in this project that could execute against
a documentation change, and inventing one for it would be ceremony.

Verification was **empirical instead of a suite**, and is stated so the numbers can
be challenged:

- **Every figure is a count over recorded raw responses**, not a summary the model
  wrote about itself. All 248 replies were persisted with their raw content, latency,
  token usage and `finish_reason`, and validity was recomputed per field from that
  record.
- **The pin was verified live** against `GET /v1/models` before sampling.
- **Arm 3 exists solely as a verification of arm 2**, and it overturned the reading
  arm 2 invited: without the prose control, 60/60 would have been written up as
  evidence that strict schema enforces constraints. It is not.
- **One measurement error was found and corrected mid-session.** The first
  Hebrew-output metric counted *any* Hebrew character, so English prose quoting a
  Hebrew task title scored as Hebrew (12/20). Recomputed on **script share** — the
  fraction of letters that are Hebrew — the real rate is 2/20. Every Hebrew figure
  reported anywhere is the corrected one.

**Honest limits, recorded rather than buried:** `getRecommendations` had n=5 per
language, which bounds its failure rate no tighter than ~26%; nothing here licenses
an assumption about `C8`'s longer staged plans; and only `openai/gpt-oss-20b` was
measured, though the key can also see `gpt-oss-120b` and `llama-3.3-70b-versatile`.

## What was deliberately not done

- **No new tickets, and no comments on other tickets.** The findings price `C1`
  ([#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)), `C2`
  ([#20](https://github.com/idomarhaim/Android_Final_Project/issues/20)), `C11b`
  ([#30](https://github.com/idomarhaim/Android_Final_Project/issues/30)) and `C15`
  ([#15](https://github.com/idomarhaim/Android_Final_Project/issues/15)), and §8 of
  the resolution comment says exactly how — but **#15 is claimed by a live sibling
  session** (`c15-language-switching`) and this session's declared paths are #16 and
  #12 only. The map is the index; that is what it is for.
- **No fog graduated into a ticket.** The one patch this resolution touches is now
  half-answered rather than sharp: what remains is whether *already-generated* text
  is regenerated or re-rendered on a language switch, which hangs on `C15` and `C10`.
- **The `estimatedMinutes` instability was not fixed.** It is `C1`'s decision, and
  this map plans rather than builds.

## Observation for whoever charts next

The map's Notes say **five** tickets were on the frontier from day one. Querying the
dependency graph back out of GitHub after this closure, the unblocked-and-unassigned
set is `C4` (#13), `C7` (#14), `C13` (#32) and `C9f` (#33) — and `C13` carries **no
blocking edges at all**, so it appears to have been unblocked from the start too,
making the original frontier six rather than five. Left untouched: re-wiring another
ticket's blockers is a scoping act on work this session does not own. Flagged for
Ido.

## Session hygiene

- **Claimed before the first write** — `#16` assigned on GitHub, then the
  `SESSIONS.md` row added under a `Lock-Path` lease (acquired, written, released).
- **Singletons: none of the board's.** No `#gradle-daemon`, neither AVD, no write to
  live `goalpilot-56e30`. The one shared resource consumed is **GROQ free-tier
  quota** — ~91,000 tokens across ~13 minutes, well inside 30 RPM / 1,000 RPD.
- **The map body was re-read immediately before editing**, since three sessions now
  append to it and it carries no lease. The C9d session's decision line had landed in
  the interim and was preserved; the edit script refuses to run if the anchor text or
  the fog line is not found verbatim, and refuses to duplicate an existing C11a line.
- **`kb-candidates/` was listed before the first unit of work**, as the rule
  requires. `2026-08-08-product-model-map.md` is **still un-drained** — three
  candidates, all pending Ido's approval. Reported, not absorbed; it is not this
  session's to drain.

## KB ingest — drained, same commit

Ido approved all three candidates on 2026-08-08 (normal mode, so the list was a
proposal and silence would not have been approval). They landed in the **central**
bundle, `C:\Dev\JARVIS\kb`:

- 📥 **Ingested:** a structured-output probe owes three numbers, not one → `kb/dev/llm-structured-output.md` *(new)*
- 📥 **Ingested:** output language splits by the field's job — naming mirrors, explaining defaults to English → same page, §5
- 📥 **Ingested:** a passing result cannot tell enforcement from compliance → `kb/dev/mechanism-vs-compliance.md` *(new)*

`kb-candidates/2026-08-08-c11a-free-model-probe.md` is fully drained and `git rm`-ed
here. **The pages are in the other repo**, so no single commit holds both — the tie is
the journal entry in `kb/log/2026-08-08.md`, which names this file *and this repo*
([`rules/memory-promotion.md`](file:///C:/Dev/JARVIS/rules/memory-promotion.md), *the
candidate–page tie*). JARVIS-side detail: `CHANGELOG/2026-08-08/c11a-free-model-probe.md`
in `C:\Dev\JARVIS`. `Check-KbLinks` clean at 35 pages.

**Correcting this file's own earlier note:** it recorded
`kb-candidates/2026-08-08-product-model-map.md` as un-drained. It was, when this
session started — the `kb-ingest-map-method` session drained it in the interim, and its
three candidates are now `kb/dev/decision-map-charting.md` and
`kb/dev/github-issue-graphs.md`. Two files in this repo are still undrained, both owned
by live sessions: `2026-08-08-c9d-calendar-scopes.md` and
`2026-08-08-fix-task-completion-feedback.md`.
