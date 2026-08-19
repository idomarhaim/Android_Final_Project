# `c13-byo-api-key` — the key buys a credential, not a pipeline

**Session:** `c13-byo-api-key` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE`
**Invocation:** `/kickoff c13-byo-api-key`, resolving [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) (`C13`) on the [v0.3 product-model map](https://github.com/idomarhaim/Android_Final_Project/issues/12) · **Planning only; no code.**

## What changed

- **Resolved and closed [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32)** with a full resolution comment.
- **Commented on [#30](https://github.com/idomarhaim/Android_Final_Project/issues/30)** (`C11b`) — the schema contract this hands it. **Not an unblocking**: `#30` is still held by `#19`, `#20`, `#24`.
- **Map body [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)** — one gist line appended to *Decisions so far*. **No fog cleared and none added.**
- This changelog, its `CHANGELOG_README.md` row, `kb-candidates/2026-08-10-c13-byo-api-key.md`, and the `SESSIONS.md` claim + release.
- **No Kotlin, Gradle, Firestore-rules or Cloud-Functions file was created or modified.**

## The decision

**The key buys a different *credential*, not a different *pipeline*** — and that single choice answers four of the ticket's five sub-questions.

It is [`C10`](https://github.com/idomarhaim/Android_Final_Project/issues/29)'s deciding argument inverted. `C10` chose by the **degraded** path: its rejected alternative fell back into a *different mechanism*, a second implementation exercised only when nobody was watching. Here the risk is the mirror image — a client calling a provider directly when a key is present needs a second copy of every prompt and every `C11b` schema, and the copy that drifts is the **enhanced** path, exercised only when a key *is* present, which for an audience of one is exactly when nobody else is watching either. Same principle, opposite end of the ladder.

| # | Question | Answer | Whose |
|---|---|---|---|
| 1 | What does the key replace? | The **credential** inside the existing proxy | Ido |
| 2 | Where does it rest? | **On the device, encrypted** | Ido |
| 3 | Which providers? | **Four named adapters**, nothing else | Ido |
| 4 | Which features does it govern? | **One switch, all four** | ⚙️ session |
| 5 | What happens when it fails? | `401/403` speaks once; a status line always | ⚙️ session |
| 6 | Behaviour or quality? | **Quality only** | derived |
| 7 | What does `C11b` have to guarantee? | Native enforcement **and** app-side validation | ⚙️ session |

### Where it landed, in detail

- **Storage.** Keystore-backed `EncryptedSharedPreferences`, beside the skin and `C15`'s language. **Firestore was cheaper and was rejected** — `users/{uid}/{document=**}` is already owner-only ([`firestore.rules:14-19`](../../firestore.rules)), so it needed no rules change and would have followed him to the second device the two-account demo proved is real. A third-party secret at rest in a backed-up, exportable database is a different posture, and cross-device convenience is not worth it for one user. **One new client dependency:** `androidx.security:security-crypto`, which this project does **not** have today — checked, not assumed.
- **Transport.** `provider · model · key` in the callable payload, per call, held nowhere. The client gains **no outbound path to any model provider** — spec §5's property is kept rather than spent. The security trade runs the other way and is accepted openly: the key transits TLS and lives briefly in a Google-run container.
- **Providers.** GROQ (free default), OpenAI, Anthropic, Gemini. **Ido overturned** this session's "any OpenAI-compatible base URL, three text fields" — which was cheaper (GROQ's endpoint already *is* that shape) and would have covered Together, DeepSeek, OpenRouter and local Ollama free. Accepted cost: a fifth provider is a Functions deploy. Bought guarantee: **no untested wire format can ever run** — and that is what makes §7 free.
- **Scope.** One switch, all four AI features. Per-feature was rejected as four opinions to hold; "key as a repair rung" (free model first, key only on a failed validation) was rejected because the prose he reads daily would stay the free model's — he would pay and see almost nothing.
- **Failure.** `user key → free model → local fallback`, never below today. `401/403` speaks **once at the point of use** (Ido's call, overturning silent-plus-Settings), latch clearing on key-edit or any success; `429` and `5xx` stay silent. **Improved on the chosen option:** a one-time message alone leaves a dead key with no standing indicator three weeks later — the same recovery-masks-failure trap in the opposite costume — so every class also shows in a **permanent status line**. That is not the rejected "Settings only"; it is both.
- **Surface.** **No new screen and no new navigation destination.** `ProfileScreen` already carries `AppearanceCard` (`ProfileScreen.kt:242-244`) on `AppPreferencesRepository`; the key's card sits beside it, and it is where `C15`'s picker lands too. Checked in the code rather than assumed there was a Settings screen.
- **Model id.** **Free text with a per-provider default, not a curated list** — derived from `AGENTS.md`'s own pitfall that a retired GROQ model id *fails silently*. A curated list baked into a deploy rots identically, now in four providers at once.

### The sharpest part, and it goes to `#30`

**With four providers a model swap is the normal case**, so `C11a`'s footnote — strict `json_schema` *"buys a guarantee that survives a model swap, not reliability"* — **becomes a requirement**. Every adapter must enforce natively (OpenAI `json_schema`, Gemini `responseSchema`, Anthropic forced tool-use, GROQ measured). Mandating it costs nothing under the four-adapter list; it would have excluded exactly one thing, the custom-URL slot that was declined — which is why questions 3 and 7 are the same decision seen twice.

**And the app-side validation stays**, because the two catch different failures:

| failure | example | caught by |
|---|---|---|
| structural | `"type": "BANANA"`, a missing field | native enforcement |
| **semantic** | an id of the right type and plausible length that **was not in the list sent with it** | app-side validation **only** |

The second row is `C11a`'s one measured failure in 248 live calls — `8xKq2mN4vRt7pLwZaB1c` → `8xKq2mN4vRt7pLwZaB`. **No schema catches it at any provider or model size.**

## The method finding, and it is the third instance

**Ido answered three of the four follow-up questions with *"I could not understand you… choose the highest-quality answer yourself."*** That is the **third** time on this map — `c10-quote-feed` recorded two, and `entity-model-intake` has a parked KB entry on it.

**The earlier fix does not explain this one.** That fix was *reduce the axis and state it before the picker*, and this session did exactly that: every question named its axis, said which axes were dropped, and three of the four carried previews. It still failed.

The discriminator is different, and it is visible in **which** question he answered. He answered Q5 — the provider list — and refused Q6, Q8, Q9. **Q1–Q5 turned on things only he knows** (does my secret go to a server, does it live on my phone, which providers do I want, does a key change the pipeline). **Q6, Q8 and Q9 turn on nothing about his life** — schema enforcement mechanisms, a latch reset rule, switch granularity. They are engineering-quality calls.

So: *"I don't understand the options"* was **not** a comprehension failure and **not** an unreduced axis. It was the user correctly reporting that **the question was not his to answer**. The repo's own derivable-decision rule already says to derive and log those. Asking them was the error; his refusal was the correction. Recorded as a KB candidate that **sharpens** the parked entry rather than corroborating it a third time.

## Defect found while grounding, filed as a spec line and not fixed

[`functions/src/index.ts:52-55`](../../functions/src/index.ts) throws `GROQ HTTP ${res.status}: ${body}` — the provider's **raw error body** — and every callable catches it into `logger.error(...)`, writing it to Cloud Logging. Harmless while the only key is the project's own; **with a user's key in play it is a third-party provider's error body in Google's logs**, and the four adapters do not fail alike. Spec line: *on a user-key call, never log a provider error body verbatim.* Not fixed — this map ships no code.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, `firestore.rules` or Cloud-Functions file was created or modified, so the JVM (`:app:testDebugUnitTest`), instrumented (`:app:connectedDebugAndroidTest`) and rules (`firestore-tests/`) layers all had nothing under test. Stated explicitly rather than skipped silently.

**Verification was structural instead:**
- `#32`'s blockers checked **live** before claiming — only `#16` (`C11a`), closed — and its assignee confirmed empty rather than inherited from the board.
- The map body `#12` **hashed three times** — at fetch, on re-fetch, and immediately before writing — all three `761f3267…6aed1b6`, so no sibling was clobbered. The edit was verified to be a **pure insertion**: `diff` reported **zero** deleted lines.
- The written map body was **read back and compared** to what was sent (identical but for a trailing newline GitHub adds).
- **Both comments' post counts were checked** (`#32` → 1, `#30` → 1). `c7-what-is-a-unit` recorded a run where `gh issue comment` posted nothing and reported no error; that is why this is checked rather than assumed.
- The frontier was **re-derived after closing**: 25 children, **9 closed**, takeable = `#25` (claimed by `c9a-schedule-a-task`), `#38`, `#39`. `#37` closed under this session — `c16-milestone-model` released it mid-flight, which is what freed `#38`/`#39`.
- **Closing `#32` freed nothing** — no open issue lists it in `blocked_by`, enumerated across all open issues rather than assumed.
- The `ProfileScreen`/`security-crypto` claims were **grepped**, not recalled.

## Singletons

**None taken at all** — no `#gradle-daemon`, neither AVD, no GROQ call, live `goalpilot-56e30` never contacted. `SESSIONS.md` and `CHANGELOG_README.md` **leased** via `Lock-Path.ps1` and held to the commit; `#git-index` taken at stage time and released.

## Recorded rather than papered over

- **This session announced work and then ended the turn without doing it.** The reply before the resolution ended *"Writing the resolution now"* and produced no artifact; Ido asked whether the session was finished, which is how it was caught. Named here because a changelog that omits it reads as if the work flowed straight through.
- **Three of the seven decisions were taken by this session, not by Ido** (marked ⚙️ above), on his explicit instruction after the picker failed. On a `wayfinder:grilling` ticket that is worth stating plainly: the HITL half did happen — every question that was actually his, he answered, including two that overturned this session's recommendation.
- **Mode conflict, resolved by rule, not by preference.** The brief says `mode: normal` and argues `AUTO MODE` is wrong for a grilling ticket. Ido opened the session with `AUTO MODE`. `/kickoff` §4 gives the session's message precedence, and the two do not actually collide: `AUTO MODE` governs **committing, pushing and KB ingest** and never granted authority to answer product questions. Both held — the grilling ran, the gates were open.
- **The claim commit carried a sibling's staged rows.** `SESSIONS.md` already held `c10-quote-feed`'s release row staged before this session started. One file, one index, unavoidable — named in the commit message and here.
- **`kb-candidates/` was listed before the first unit of work**, per rule. Three files, **none this session's to drain**: `2026-08-09-c9f-consent-screen-state.md` and `2026-08-09-entity-model-intake.md`, each rewritten down to **one parked always-ask entry** awaiting Ido; and `2026-08-10-c10-quote-feed.md`, whose on-disk state (4 entries, all `Proposed`) **disagreed with the `CHANGELOG_README.md` row** claiming a `kb-ingest-c10` session had drained it 4-of-6. **The contradiction resolved itself at the release gate:** `kb-ingest-c10` is **live right now** — it holds leases on `SESSIONS.md` and `#git-index` — so what was read was a mid-drain working tree, not a stale index row. It is a live session's file, not an unowned one, and this session neither drained nor touched it.
- **This session was lease-blocked at its own release, and waited rather than asking.** `kb-ingest-c10` held `SESSIONS.md` and `#git-index`; the earlier 15-minute leases taken for the claim had expired in between. Per §5.2 the block was not escalated to Ido — the session armed a background wait on the lock file and re-read the board before writing, which is how the `kb-ingest-c10` row above turned out to be information rather than a contradiction. **`kb-ingest-c10` holds no row on this board**, which is worth naming: a session visible only through its leases is invisible to anyone who reads the board alone.
- **The board's `kb-candidates` note is stale in one particular**: it names `2026-08-10-c7-what-is-a-unit.md` as pending, but that session drained 5/5 and `git rm`'d the file. Left for its author rather than edited under them.
