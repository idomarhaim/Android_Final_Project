# 2026-08-24 · `presentation-source`

## 📌 What was asked

Ido: a single file to drop into a Gemini / NotebookLM notebook, from which it can generate a
presentation about the system — **the need · the gap in the existing answers · the answer
GoalPilot gives**, with the product detailed feature by feature and each feature tied back to
the need it serves.

## ✅ What shipped

**[`docs/presentation/goalpilot-presentation-source.md`](../../docs/presentation/goalpilot-presentation-source.md)** *(new)* — one
self-contained document, twelve parts:

| Part | What it carries |
|---|---|
| 0 | instructions to the notebook — sole source of truth, language, tone, and a pointer to the guardrails |
| 1 | the need — *goals that matter fail by never being scheduled, not by being abandoned* |
| 2 | the gap — a category-by-category table (to-do lists · calendars · habit trackers · goal apps · fitness apps · general AI assistants) and the **four structural failures** underneath it |
| 3 | the answer — the one-sentence claim and the four-level structure that makes it computable |
| 4 | the product, thirteen features, each written **what it is → what you see → which gap it closes**, plus a feature-to-gap summary table |
| 5 | the seven honesty rules that are the product's actual position |
| 6 | how it is built |
| 7 | privacy and data control |
| 8 | status |
| 9 | a ready-made 14-slide outline, with 7-slide and technical variants |
| 10 | fourteen quotable lines, lifted from the committed narration |
| 11 | the four domain terms in English and Hebrew |
| 12 | six accuracy guardrails — what the notebook must **not** say |

**Sources synthesised** (read-only): `README.md`, `docs/PRODUCT_v0.3.md` §§0–6, `AGENTS.md`,
`docs/marketing/explainer-video-brief.md`, `docs/marketing/marketing-film-cut.md`,
`docs/marketing/tour-timecodes.md`, `app/build.gradle.kts`,
`domain/model/Challenge.kt`, `feature/challenges/ChallengeDialogs.kt`, `functions/src/derived.ts`.

## 🔎 Three claims the pre-commit self-review caught and corrected

The draft asserted three things that read well and were not true of the shipping app. Each was
checked against code rather than against the spec, and each is why §0 tells the notebook this
file is the only source it may use.

1. **Challenges scoring.** The draft said a challenge *"scores from movement since you joined,
   not from your goal's current value"*, that *"points may never be a challenge metric"*, and
   that *"joining links or creates a goal"* — all three straight from
   `docs/PRODUCT_v0.3.md` §6. That section is the **map**, not the build.
   `Observed:` at `HEAD`, `Challenge.kt:19` has `metricUnit: String = "points"` as the
   **default**, `ChallengeDialogs.kt:244` prompts for a `Total ${metricUnit}` the participant
   **reports themselves**, and `functions/src/derived.ts:139` `scoreFromReport` projects that
   reported absolute total onto the standing. §4.11 was rewritten to what ships, and guardrail
   5 was added telling the notebook not to describe automatic scoring from health data or goal
   progress.
2. **The recording's length.** *"eleven-minute"* — the two committed sources disagree
   (`tour-timecodes.md` says `11:00.3`, `explainer-video-brief.md` §1 says `11:57.4` for the
   re-shot deliverable). The number was dropped rather than picked.
3. **Milestone as a fourth level.** The Hebrew term table implied `אבן דרך` is its own entity;
   `docs/PRODUCT_v0.3.md` §1.1 says there is **one kind of objective** and *goal* is a role an
   edge carries. The row now says so.

## 🧪 Tests

**No test layer applies.** This session added one Markdown document and one board row — no
Kotlin, no TypeScript, no resources, no manifest. Nothing was built, no device or emulator was
touched, and the Gradle daemon was not taken. Stating it rather than skipping it silently, per
`testing.instructions.md`.

**What was verified instead**, since prose has no suite: every load-bearing product claim in
the document was checked against a committed source, and the three that failed are corrected
above. The single figure drawn from real data — the **thirteen per cent unfiled** share — is
`tour-timecodes.md` beat 40, measured at recording time, and guardrail 4 tells the notebook to
present it as an illustration and never as a statistic about users.

## 🧭 Board

Claimed `presentation-source` before the first write, owning `docs/presentation/**` — a new
folder that collides with none of the three live rows (`62-tour-assembly` and
`62-tour-video-v2` hold `docs/marketing/**`; `docs-repair` holds the six existing `docs/*.md`
files and `README.md`). Singletons: **none**.

`kb-candidates/` listed before the first unit of work, per the standing duty: three files
present, all other sessions' — `2026-08-24-62-tour-video-v2.md`,
`2026-08-24-docs-currency-guard.md`, `2026-08-24-s25-layout-and-tour.md`. None of them mine to
drain. This session produced no KB candidate of its own.
