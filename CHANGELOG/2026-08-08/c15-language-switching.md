# `c15-language-switching` — the language picker turns out to be three settings

**Session:** `c15-language-switching` · **Invocation:** `/wayfinder 12 15` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL throughout) ·
2026-08-08.

One ticket resolved, which is the skill's limit. **No code was touched** — this map
ships no code, and that held: every file under `app/` and `functions/` was read and
none was edited.

## What changed

| | |
|---|---|
| Resolved | [#15 · `C15` In-app language switching](https://github.com/idomarhaim/Android_Final_Project/issues/15) — closed, with the full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*; **two fog patches cleared** from *Not yet specified* |
| Ticket created | [#35 · `C15b`](https://github.com/idomarhaim/Android_Final_Project/issues/35) — graduated from fog, child of #12, blocked by [#24](https://github.com/idomarhaim/Android_Final_Project/issues/24) and [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29) |
| Unblocked | [#29 · `C10` the daily quote feed](https://github.com/idomarhaim/Android_Final_Project/issues/29) — its two blockers (#15, #16) both closed today |
| Frontier now | [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13) `C4` · [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14) `C7` · [#29](https://github.com/idomarhaim/Android_Final_Project/issues/29) `C10` · [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) `C13` · [#33](https://github.com/idomarhaim/Android_Final_Project/issues/33) `C9f` |

## The decision

The ticket asked what a language picker changes. The answer is that **"language" was
three settings wearing one name**, and the third question of the grilling is where that
came apart.

| Setting | Controls | Default | User-changeable |
|---|---|---|---|
| **Language** | every *word* — chrome, AI text, §8 fallback, month names | device language (Hebrew phone → Hebrew) | **Yes** — picker in Profile, He / En |
| **Region** | **first day of week**, date order, number format | device country (Israel → Sunday, `5/8/2026`) | **Yes**, and **decoupled from Language** |
| **Direction** | RTL mirroring | derived from **Language** | No — follows the words |

Direction hangs off Language rather than Region, and Ido's own case is the proof: an
Israeli working in English must get a left-to-right screen.

The six answers, in order: **Hebrew and English, defaulting to the device language** ·
**AI text follows the picker** · **the §8 fallback is authored natively per language,
never translated** · **the time-trend chart does not mirror** · **the choice is stored
per-device beside the skin** · **month names follow Language, date order and week start
follow Region**.

## Ido overturned this session's recommendation, and it is the better answer

Question 6 was put badly — twice. It bundled date formats and the week boundary into one
question, and its recommendation was to **pin the first day of week to Sunday**. Ido
rejected the picker outright and supplied the decomposition instead: the week start
follows the **country**, stays **user-overridable**, and is **decoupled from the language
choice** — because Israel's official language is Hebrew but many people, especially in
hi-tech, prefer to work in English, so the week must not ride on the language.

That is recorded rather than smoothed over because the failure is reusable: the question
conflated a *word* with a *convention*, and the fix was to name the axes rather than to
choose between them. The earlier Sunday-pin recommendation is **dropped**, not softened.

He also stopped the grilling once at question 3 to ask whether the fallback would be
Hebrew even when English was selected — it would not, and it never would have been, but
the question had been framed as *how the Hebrew version gets written* without first
saying that the language it appears in was already settled at question 2. Worth noting
for the next grilling: a question about *how* reads as a question about *whether* unless
the settled part is restated.

## Facts established, so no later session re-derives them

- **`values/strings.xml` holds exactly 2 strings** (`app_name`, `app_tagline`), and
  `stringResource` appears in **one file in the whole app** — `GoogleAuthClient.kt`,
  twice — against roughly **176 hardcoded UI literals** across 10 screens. **The
  extraction is the feature**; the second language is comparatively cheap after it.
- **`android:supportsRtl="true"` is already set.** Mirroring is the same switch, free.
- **`DonutChart` and `ProgressRing` cannot mirror** — `Canvas` + `drawArc`,
  `startAngle = -90f`, absolute coordinates. Clockwise in both languages, for nothing.
  This is what makes exempting the trend chart the consistent choice rather than the
  timid one.
- **`SimpleBarChart` mirrors and should** — a `Row` of label-plus-bar.
- **There are no seeded default life areas** — every `LifeArea` is hand-made or synced
  from a Google Tasks list name, so the awkward middle case simply does not exist.
- **`GoalCategory.label` is safe to localize** — only `.label` displays; the enum *name*
  (`HEALTH`) is what persists and what the LLM sees. No storage change, no migration.
  Same for `ChallengeType` and `RecommendationType`. *Taken by derivation, logged, not
  asked.*
- **The §8 fallback is five English sentences in one file** —
  `RecommendationRepositoryImpl.kt:145-221`. Three of the five embed user content or a
  number, which is exactly why translation fails on them.
- **`minSdk = 26` and AppCompat is not a dependency.** A real app-wide locale therefore
  costs AppCompat for the 26–32 backport, and switching **recreates the activity** — a
  flicker, unlike the skin picker's instant apply. Accepted as the price.

## Two defects found — filed as spec lines, not fixed

Neither is this session's to fix; the map ships no code.

1. **No prompt states an output language.** None of the three systems in
   `functions/src/index.ts` says what language to *write* in — two mention only that the
   *input* may be Hebrew (`index.ts:112`, `index.ts:146`), and `getRecommendations`, the
   only one producing user-visible prose, says nothing at all. That is why the coach was
   observed writing English at an all-Hebrew account on 2026-08-06. **Independently
   confirmed the same day by `C11a`**, which measured 0/10 Hebrew coach messages given
   an all-Hebrew context and 3/3 when one prompt line asks — so the fix is one line.
2. **The date formatters can never follow a language switch.** All ten are process-scoped
   `val`s built from `Locale.getDefault()` at class load (`AnalyticsRange.kt:244-250`,
   `DateTimeUtils.kt:13`, `BuildHealthProposalsUseCase.kt:191`) with English-shaped
   patterns. Not even an activity recreation moves them. Meanwhile
   `defaultFirstDayOfWeek()` (`AnalyticsRange.kt:142`) **is** a function and re-reads the
   locale on every call — so today's code would silently re-bucket the analytics history
   while leaving every label in English. Decision 6 removes that coupling.

## New scope surfaced

**A week-start setting does not exist.** `AnalyticsRange.defaultFirstDayOfWeek()` derives
it from the locale with no override; decision 6 requires one. It is a spec line for
`docs/PRODUCT_v0.3.md`, not a decision ticket — Ido decided it here.

## 🧪 Tests

**No suite run, and none applicable.** No Kotlin, Gradle, Firestore-rules or Cloud
Functions file was created or modified — the session's output is issue comments, one new
issue, the map body, this changelog and a KB-candidate file. The project's layers (JVM
unit, instrumented, `firestore-tests/`) all exist and all were left untouched, so running
them would have verified nothing this session did.

Verification was structural instead, and ran green:

- `#15` closed with its resolution comment attached.
- `#35` created, confirmed a child of `#12`, confirmed `blockedBy` **#24** and **#29**.
- `#12`'s body re-read **immediately before** the write and compared byte-for-byte
  against the copy the session had edited — line 56 (`C11a`'s entry, written by a
  concurrent session) hashed identically, proving no drift and no clobber. After the
  write, `C15`'s line is present and the `RTL layout mechanics` fog patch is gone.
- Frontier re-queried out of GitHub afterwards: five open, unblocked, unassigned children
  — `#13`, `#14`, `#29`, `#32`, `#33`. `#29` is new to the frontier, which is the
  intended consequence of closing `#15`.

## Singletons

**None taken.** No `#gradle-daemon`, neither AVD, no live `goalpilot-56e30`, no GROQ
call — so no contention with `c11a-free-model-probe`'s free-tier quota or
`fix-task-completion-feedback`'s device. The one shared artifact touched was the map body
`#12`, which carries no lease and which three sessions appended to today; the
read-immediately-before-write check above is how that was handled.
