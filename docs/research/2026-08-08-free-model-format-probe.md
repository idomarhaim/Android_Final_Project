# What the free model can actually do against a fixed format

**Research asset for [issue #16](https://github.com/idomarhaim/Android_Final_Project/issues/16) (`C11a`), a decision ticket on the [v0.3 product-model map](https://github.com/idomarhaim/Android_Final_Project/issues/12).**
Measured 2026-08-08 · session `c11a-free-model-probe` · 248 live calls to GROQ.

> Every number here is measured, not assumed. The harness was a throwaway script;
> its method and every prompt are reproduced in §8 so the run can be repeated.

---

## 0. The short answer

**The format bar the ticket was worried about is not where the risk is.** `openai/gpt-oss-20b` returned clean, parseable JSON in **170 of 170** production-shaped calls and was fully valid on every field in **168** of them. It did that in Hebrew as reliably as in English — better, in fact: both failures were English. It did it *without* `response_format` set at all. **No AI feature on this map needs to be dropped, or specced defensively, because the free model cannot hold a format.**

Three things did come out of the probe, and each moves a different ticket:

1. **The only real failure mode is silent id corruption** (§3). The model returned `"8xKq2mN4vRt7pLwZaB"` for an id it was handed as `"8xKq2mN4vRt7pLwZaB1c"` — a plausible string that passes every check except membership in the list supplied alongside it. **Every id-bearing field needs a membership check on receipt.** Enum values written into the prompt were perfect 50/50; it is *opaque tokens* the model fumbles, not structure.
2. **One wide call beats several narrow ones** (§4) — 1.7× faster, 30% cheaper, and three requests lighter on a 30-per-minute budget, with no reliability deficit that the membership check does not already cover. Split a call for differing *fallback behaviour*, never for format.
3. **The envelope is stable and the numbers inside it are not** (§6). The same task, the same prompt, twice: 180 minutes then 120. The same task in Hebrew rather than English: 45 minutes rather than 25. The time-allocation chart is built on that field and presents it as measurement.

And one finding for a neighbouring ticket: **the model does not write Hebrew unless told to** (§7) — 0/10 Hebrew coach messages given entirely Hebrew goals — but writes it perfectly when a single sentence in the prompt asks. `C15`'s working assumption is reachable; it costs a language parameter threaded from the client into every prompt.

---

## 1. What was measured, and against what

| | |
|---|---|
| **Model** | `openai/gpt-oss-20b` — the value of `DEFAULT_MODEL` in `functions/src/index.ts`, confirmed **live** on 2026-08-08 (present in the 15 models the project key can list) |
| **Temperature** | `0.7` — the production value in `callGroqJson`, not a probe-friendly `0` |
| **Prompts** | The three **verbatim production system prompts** from `functions/src/index.ts`, plus a narrow decomposition of one of them |
| **Languages** | English and Hebrew, matched task-for-task |
| **Ids in fixtures** | Opaque 20-character Firestore-style ids (`8xKq2mN4vRt7pLwZaB1c`), so "did it echo the id it was given" is a real test rather than a guessable one |
| **Calls** | 170 (arm 1, `response_format: json_object` as production sends it) + 60 (arm 2, strict `json_schema`) + 18 (arm 3, adversarial schemas and a prose control) |
| **Cost** | ~91 000 tokens, ~10 min wall clock for arm 1. **Zero HTTP errors, zero 429s**, every call `finish_reason: stop` |

The four shapes:

- **A — `scoreTask`** (2 fields: `points`, `minutes`). The narrowest thing the app asks for.
- **B — `classifyTask`** (8 fields: two ids, an enum, two bounded integers, a 0–1 float, two strings). **One call doing several jobs** — the shape the ticket asks about.
- **C — the same three jobs as three narrow calls** (goal / category / life area), so wide and narrow can be compared on identical inputs.
- **D — `getRecommendations`** (a nested array of objects, each with its own enum and nullable id).

---

## 2. Format adherence: the model is not the problem

**All 170 arm-1 replies parsed as clean JSON on the first attempt.** Not fenced, not wrapped in prose, not truncated — `JSON.parse(content.trim())` succeeded 170 times out of 170.

| Cell | n | Fully valid | Clean JSON |
|---|---|---|---|
| A `scoreTask` — EN | 20 | **20/20** | 20/20 |
| A `scoreTask` — HE | 20 | **20/20** | 20/20 |
| A `scoreTask` — EN, **no `response_format` at all** | 10 | **10/10** | 10/10 |
| A `scoreTask` — HE, **no `response_format` at all** | 10 | **10/10** | 10/10 |
| B `classifyTask` wide — EN | 20 | 18/20 | 20/20 |
| B `classifyTask` wide — HE | 20 | **20/20** | 20/20 |
| C narrow (goal / category / area) — EN | 30 | **30/30** | 30/30 |
| C narrow (goal / category / area) — HE | 30 | **30/30** | 30/30 |
| D `getRecommendations` — EN | 5 | **5/5** | 5/5 |
| D `getRecommendations` — HE | 5 | **5/5** | 5/5 |
| **Total** | **170** | **168/170 (98.8%)** | **170/170 (100%)** |

Three consequences, stated plainly because each retires an assumption the map was entitled to make:

- **"The model emits prose around the JSON" is not a failure mode of this model.** It was worth naming as a candidate; it did not occur once, *including in the 20 calls that sent no `response_format` at all*. The rule-of-three bound on 170 clean parses puts the true rate of unparseable replies under **1.8%**.
- **`response_format: json_object` is not what is holding this together.** Removing it changed nothing measurable. Keep it — it costs nothing and it is a real guarantee at the API layer — but the app's reliability does not rest on it.
- **Hebrew is not worse at format.** It is marginally *better*: the two failures in the whole run were both English.

The honest limit on the small cells: 5 samples per language on shape **D** bounds its failure rate no tighter than ~26%. D's structural complexity (nested array, per-item enum) is the one shape that deserves re-measuring at volume before `C11b` writes its contract.

---

## 3. The two failures, named — and they are the same failure

Both non-valid replies came from **shape B, the wide 8-field call, in English**. Neither was a format error. Both were **id fidelity**:

**Failure 1 — the truncated id.** Task `"Go for a 5k run"`:

```json
"suggestedGoalId": "8xKq2mN4vRt7pLwZaB"     // given: "8xKq2mN4vRt7pLwZaB1c"
```

The last two characters are gone. **This is the dangerous one.** It is a plausible-looking 18-character string in a field typed `String?`; it passes a null check, a type check, a "is it non-empty" check, and every other check short of *membership in the list that was supplied in the same request*. An app that trusts it files a real task against a goal that does not exist.

**Failure 2 — the name in the id slot.** Task `"Call mom"`:

```json
"suggestedLifeAreaId": "Other"              // given: four opaque 20-char ids
```

Here the model answered a different question — it emitted a category-ish *name* where an id was demanded. Harmless by comparison: any membership check catches it, and `classifyTask`'s own system prompt already anticipates it ("MUST be one of the given life area ids, or null").

**The generalisation, and it is the finding that matters most for `C11b`:** this model's structural obedience is near-total, and its *referential* obedience is not. It reliably produces the right **shape**; it does not reliably reproduce an **opaque token it was handed**. Every field whose value must come from a set the caller supplied — goal ids, life-area ids, and any future id — needs a membership check on receipt, and the failure is silent without one. Fields whose values come from a closed enum written *into the prompt* had a perfect record: **`suggestedCategory` was in-enum 40/40, and every `type` in D's nested array was in-enum in all 10 calls.**

---

## 4. One wide call, or several narrow ones?

The ticket asks this directly, because it prices `C1`, `C2` and `C11b`. Same three jobs, same ten task titles, same language, measured both ways:

| | Wide (1 call, 8 fields) | Narrow (3 calls) |
|---|---|---|
| goal id valid — EN | 19/20 | **10/10** |
| goal id valid — HE | 20/20 | **10/10** |
| category in enum — EN / HE | 20/20 · 20/20 | 10/10 · 10/10 |
| life-area id valid — EN | 19/20 | **10/10** |
| life-area id valid — HE | 20/20 | **10/10** |
| **all three right at once — EN** | **18/20 (90%)** | **10/10 (100%)** |
| **all three right at once — HE** | **20/20 (100%)** | **10/10 (100%)** |
| median latency — EN | **735 ms** | 1 363 ms (sum of three) |
| median latency — HE | **853 ms** | 1 335 ms (sum of three) |
| median tokens — EN | **900** | 1 222 |
| median tokens — HE | **935** | 1 267 |

**Verdict: the wide call is the right default, and the reason to split is never format.** Wide is ~1.7× faster and ~30% cheaper in tokens, and it costs **three requests' worth of the 30-per-minute free-tier budget less** — which is the constraint that actually bites, since the Google Tasks import already spends one request per imported row (the reason `scoreTask` returns points *and* minutes in one call, per its own docstring).

The 90% vs 100% gap in the English wide arm is real but small (n=20; the two arms are not statistically separable at this sample size), and it is **entirely the id-fidelity failure of §3, not a format failure** — so it is bought back by a membership check, which is needed in the narrow shape too. Splitting a call to protect an id would spend a 3× request budget on a problem that four lines of validation solve.

**Where splitting *is* justified** is a different axis, and `C11b` should decide on it rather than on reliability: when the two halves have **different fallback behaviour**. A malformed `points` degrades to the local heuristic; a malformed `suggestedGoalId` should file the task nowhere. One call means one failure, so both halves fail together — and today `classifyTask`'s `catch` does exactly that, returning a fallback for all eight fields when any one of them made the call throw.

---

## 5. Strict `json_schema` is available — and buys less than it looks like it buys

The app sends `response_format: {type: "json_object"}`. GROQ also accepts **`{type: "json_schema", strict: true}`** for this model, with a real JSON Schema attached. That was not known when the ticket was written, and it looked like it might raise the ceiling from *persuasion* to *enforcement*, so it was measured rather than footnoted.

**Arm 2 — the wide `classifyTask` shape under three schemas, 60 calls, both languages:**

| Schema arm | What it constrains | Valid |
|---|---|---|
| `types-only` | field types + `suggestedCategory` as an `enum` | **20/20** |
| `enum-ids` | the above + goal and life-area ids as `enum`s of the ids actually supplied | **20/20** |
| `enum-ids+ranges` | the above + `minimum`/`maximum` on points, minutes and confidence | **20/20** |

**60/60 fully valid, zero failures, zero id errors** — against arm 1's 2 id failures in 40 wide calls. GROQ also accepted `minimum`/`maximum` inside `strict: true` without a 400, which OpenAI's own strict mode rejects.

**But arm 2 cannot prove enforcement, and it is important not to claim it does.** The model never *tried* to violate a constraint, so 60/60 is equally consistent with "the schema enforced it" and "the schema was ignored and the model happened to be right". Arm 3 was run to separate those — schemas that demand something the model would never volunteer:

| Adversarial case | Result (3 reps each) |
|---|---|
| `points`/`minutes` schema-constrained to **1000–2000** for a 5k run | obeyed 3/3 — `1500, 1500` / `1500, 1200` / `1500, 1200` |
| `category` enum narrowed to `[FINANCE, SLEEP]` for a 5k run | obeyed 3/3 — `SLEEP`, `SLEEP`, `FINANCE` |
| life-area id enum where **none fits** | returned `null` 3/3 — correct |
| a `required` field the task cannot possibly supply (`unrelatedColour`) | emitted 3/3 — `GREEN` every time |
| **Control: the same 1000–2000 range asked in *prose only*, no schema** | **obeyed 3/3 — `1500,1200` / `1200,1500` / `1500,1500`** |

**The control is the finding.** The model obeys an absurd range stated in prose exactly as well as it obeys one stated in a schema. So this probe **cannot distinguish enforcement from compliance** — and for practical purposes it does not need to, because the practical answer is the same either way:

- **For this model, strict `json_schema` and a well-written prose contract produce the same output.** Choosing between them is not a reliability decision, and arm 2's 60/60 vs arm 1's 38/40 is not a statistically separable difference at these sample sizes (arm 1's own narrow calls were also 60/60).
- **What schema does buy is a guarantee that does not depend on the model's good manners** — it is declarative, it survives a model swap, and `enum`-constraining ids makes the §3 truncated-id failure structurally unrepresentable rather than merely unobserved. That is worth something precisely because §3's failure is *silent*.
- **What it costs** is ~25% more tokens (median 1 172 vs 900 on the same shape) and slightly higher latency, for a shape that already works.

**Recommendation to `C11b`:** treat strict `json_schema` as available and preferable for the id-bearing fields, but do **not** spec it as the thing that makes the app correct. The membership check of §3 is still owed — it is the only mechanism here proven to catch a bad id, since a schema that is possibly-ignored cannot be relied on to.

---

## 6. The finding the ticket did not ask for: the format holds, the numbers do not

This is the result that should change what the map does next.

`C11a` asked how reliably the model returns a **valid, fixed-format** response. The answer is: essentially always. But a valid envelope carrying an unstable number is not a reliable answer, and the numbers are unstable — **the same prompt, the same title, the same model, at the production temperature of 0.7**:

| Task (English) | Run 1 (points, minutes) | Run 2 (points, minutes) |
|---|---|---|
| Meal prep lunches for the week | 15, **180** | 20, **120** |
| Read 20 pages of the biology textbook | 10, **30** | 10, **45** |
| Practice guitar scales | 15, **30** | 20, **45** |
| Prepare slides for Monday's standup | 12, **45** | 10, **30** |
| Budget review for August | 15, 60 | 15, 60 |
| Stretch before bed | 5, 10 | 5, 10 |

And **across languages, for the same task**, the divergence is larger than the run-to-run spread:

| Task | EN minutes (mean of 2) | HE minutes (mean of 2) |
|---|---|---|
| Review pull request 482 | **25** | **45** |
| Meal prep lunches for the week | **150** | **240** |
| Go for a 5k run | 40 | 30 |
| Budget review for August | 60 | 60 |

Across all ten titles, two runs each:

| | English | Hebrew |
|---|---|---|
| `minutes` identical on both runs | 5/10 | 7/10 |
| `minutes` worst run-to-run ratio | 1.50× | 1.50× |
| `points` identical on both runs | 4/10 | 5/10 |
| `points` worst run-to-run ratio | 1.40× | **2.00×** |
| same `suggestedCategory` on both runs | 8/10 | 8/10 |

Worst **cross-language** divergence on `minutes`, comparing the mean of each language's two runs: **1.80×**, on *"Review pull request 482"* (25 min in English, 45 in Hebrew). That is larger than the worst run-to-run spread within either language.

`suggestedCategory` is unstable in both directions: *"Stretch before bed"* returned `HEALTH` on one run and `FITNESS` on the next in **both** languages, and *"Review pull request 482"* split `PROJECTS`/`CAREER` in English while staying `PROJECTS` in Hebrew. Notably these are the *genuinely ambiguous* tasks — the instability tracks real ambiguity rather than randomness, which is a point in the model's favour and an argument that the category needs to be **editable** rather than better-prompted.

**Why this matters more than the format numbers.** The time-allocation chart reports what share of the user's life went into each life area. It is built from `estimatedMinutes`. A field that varies by up to **2×** between two identical calls, and by up to **1.8×** between the Hebrew and English phrasing of the same task, is a fine *starting suggestion* and a poor *measurement* — and the app currently presents it as the latter, with no indication that a number was estimated rather than observed.

This lands squarely on **`C1` (the points-and-time model, and who is allowed to author it)** and it is an argument that has now been measured rather than intuited: the case for a visible manual override on duration ([#9](https://github.com/idomarhaim/Android_Final_Project/issues/9) already proposes one) does not rest on the model being *wrong*, but on it being **unrepeatable**. Two users doing the same task, or one user in two languages, get different numbers, and nothing in the UI says so.

---

## 7. Hebrew: format survives, prose does not

Three separate questions hide inside "does it work in Hebrew", and they have three different answers.

**Does the format survive Hebrew input?** Yes, completely — 100/100 valid across all Hebrew cells, better than English's 98/100.

**Does it *understand* Hebrew?** Yes. Category assignment on the Hebrew titles matched the English ones on 9 of 10 tasks, and the Hebrew arm made zero id errors.

**Does it *write* in Hebrew?** No — and this is the finding that goes straight to [`C15` / #15](https://github.com/idomarhaim/Android_Final_Project/issues/15), whose working assumption is that AI-generated text follows the language picker. Measured on script share (the fraction of letters that are Hebrew, so prose that merely *quotes* the Hebrew title does not count as Hebrew):

| Field | What it is | Hebrew input → Hebrew output |
|---|---|---|
| `recommendations[].title` / `.message` | Dashboard coach text, shown to the user | **0 / 10** |
| `rationale` | Explanation shown beside a classification | **2 / 20** |
| `suggestedNewGoalTitle` | A string the app **writes into Firestore** as the user's own goal | **13 / 14** |

Given goals titled `לרוץ חצי מרתון` and life areas named `בריאות`, `לימודים`, `קריירה`, the model replies:

> `"title": "Half Marathon Motivation", "message": "Great job on staying active! Keep up the training schedule…"`

**The split is systematic, not random: the model mirrors the input language when it is *naming a thing*, and defaults to English when it is *explaining or coaching*.**

**But it writes Hebrew perfectly when asked.** Arm 3 added one sentence to the system prompt — *"Write the rationale in Hebrew"* — and got Hebrew rationales **3/3, at a script share of 1.00**, fluent and on-topic:

> `"המשימה כוללת ריצה של 5 ק"מ, מה שאומר פעילות גופנית בינונית…"`

So this is not a capability ceiling. It is a **missing instruction**: none of the three production prompts in `functions/src/index.ts` says anything about output language, and in that silence the model defaults to English regardless of the language of everything around it. Two consequences for `C15`:

1. **The "AI text follows the picker" assumption is achievable, but not free, and it fails today by default.** It is not a matter of the picker not existing yet — the model has an entirely Hebrew context in front of it and writes English anyway. Making it follow the picker means the chosen language must be **passed to the Cloud Function and injected into every prompt**: a client→function payload change plus one line per prompt. Small, but it is real work, it belongs in `C11b`'s format spec, and it is the concrete cost of the answer `C15` is leaning towards.
2. **AI-authored content is already on the *content* side of the chrome/content boundary.** `suggestedNewGoalTitle` comes back in Hebrew 13/14 times and is stored as the user's goal title. The map's *Out of scope* says user-authored content does not get translated by the picker — a goal the model named is user content the moment it is saved, so it must not retranslate either. The boundary is already load-bearing and nobody has had to draw it yet.

Not measured, and worth saying: **output quality** in Hebrew — whether Hebrew coach text, once requested, reads naturally or reads like translated English. That needs a human judgement, not a probe, and it is Ido's to make.

---

## 8. Method, so the numbers can be re-derived

- **Harness:** a throwaway Node script (scratchpad, deliberately not committed — this ticket ships no code). It reads `GROQ_API_KEY` from the git-ignored `functions/.env`, calls `https://api.groq.com/openai/v1/chat/completions` directly, and paces itself to ~28 requests/minute to stay under the free tier's 30 RPM.
- **System prompts:** copied verbatim from `functions/src/index.ts` (`scoreTask`, `classifyTask`, `getRecommendations`). The narrow variants in shape C are the wide prompt's three jobs split, each keeping the original's constraint wording.
- **Fixtures:** 10 matched task titles per language, 3 goals, 4 life areas, all ids opaque and 20 characters. The Hebrew life areas are Ido's real ones (`בריאות`, `לימודים`, `קריירה`, `אימון ריצה`).
- **Validity** was checked per field, not as a single verdict: key presence, absence of extra keys, enum membership, id membership **in the list supplied in the same request**, integer-ness and range for the bounded numbers, `0..1` for `confidence`, non-empty string for prose.
- **Parse taxonomy:** `clean` (parses as-is) → `fenced` (inside a fenced block) → `prose-wrapped` (JSON extractable between the first `{` and last `}`) → `unparseable`. Only `clean` ever occurred.
- **Arm 3** deliberately demands what the model would not volunteer — a 1000–2000 range for a 5k run, an enum excluding the correct category, a required field the task cannot supply — and pairs each with a **prose control** stating the same constraint without a schema. The control is what makes the arm interpretable: without it, obedience proves nothing about enforcement.
- **Reproduce it** by re-running the same shapes against the pin in `DEFAULT_MODEL`. **Check that pin first** — GROQ retires models on a rolling schedule and a retired id fails *silently* into the local fallback, so a probe against a dead pin measures nothing. `llama-3.1-8b-instant`, the app's previous pin, is still listed as of 2026-08-08 but shuts down **2026-08-16**.

### What would change these numbers

- **A model change.** Every figure here is about `openai/gpt-oss-20b` specifically. The GROQ free tier also exposes `openai/gpt-oss-120b` and `llama-3.3-70b-versatile`; none was measured.
- **Longer, more open-ended shapes.** The longest thing measured is D's 4-item array. `C8`'s staged task plans are longer and less constrained, and nothing here licenses an assumption about them.
- **Volume.** These are single-user rates on an idle key. Free-tier behaviour under a day's worth of requests (1 000 RPD) was not exercised.
