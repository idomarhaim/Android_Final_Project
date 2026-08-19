# 2026-08-06 — `product-review`: the pre-sleep product/UX brief becomes a backlog

> **Summary:** the pre-sleep product/UX brief becomes a backlog

Ido did a quick manual pass over the running app wearing a product/UX hat and
wrote down everything that looked missing or wrong, freehand, in Hebrew, into a
`.docx`. This session turns that document into something the repo — and the next
agent session — can actually work from, and decides how the work should be split.

## Why this, and not `/wayfinder` over the whole document

The obvious move was to hand the `.docx` to `/wayfinder` and chart a map. That
would have been over-ceremony on about half the content and unbuildable on the
other half:

- **Half the brief is already answerable.** Five defects and six single-session UX
  items need no decision — they need reproducing and building. Charting them as
  decision tickets is the over-ceremony signal named in
  `C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md` ("the map's tickets are all
  already-answerable").
- **A map cannot be charted from a source nothing can read.** Free Hebrew prose in
  a binary is unquotable in a ticket and invisible to every future session.

So: transcribe first, classify second, and let each tier take the ceremony it
actually earns.

## What changed

- **New** [`Product and UX Reviews/2026-08-06-brief-review.md`](../../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md)
  — faithful English transcription of the brief, numbered `R1`–`R28` so every
  downstream artifact can cite a stable id. Transcription only: the vague items
  stay vague, nothing was reworded into a requirement. The count moves from 27
  written lines to 28 items because `R27` holds two independent faults and `R22`
  is an explicit question hanging off `R21`.
- **New** [`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`](../../TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md)
  — the actionable half: 5 defects (`D1`–`D5`), 6 single-session UX items
  (`U1`–`U6`), and 4 findings from the agent's own static pass (`A1`–`A4`).
- **New** [`TODO/TODO_FUTURE/ProductModel.TODO.future.md`](../../TODO/TODO_FUTURE/ProductModel.TODO.future.md)
  — the 13 product-model decisions (`C1`–`C13`) that are genuinely foggy, written
  as map-bound decision tickets with a proposed destination and charting order.
- `TODO/TODO.md` — index rows for both, plus the title-page finding below.

## Findings worth more than a backlog row

**The one open MUST item is already done, by Ido, and uncommitted.**
`GoalPilot_spec_EN.docx` is modified in the working tree and its title page now
reads `Submitted by: Ido · [Ido Mar-Chaim 209497072] · [10208]` — the
`[Full name & ID] · [Course number]` placeholder is gone. The file is *Frozen /
off-limits* in `AGENTS.md`, so no agent touched it and none will. The template's
square brackets survive around both values; **Ido ruled them cosmetic and
confirmed the item closed in this session**, so both MUST items are now ticked and
nothing blocks handing the project in. The modified docx is deliberately **not**
in this commit — it is a frozen file and Ido's to commit.

**`R12` is not a defect, and filing it as one would have hardened the wrong
model.** Reported as "the scores the agent gives tasks don't match the percentages
it gives goals". They are independent numbers by construction: `Task.points` is a
gamification currency the LLM sets (default `10`), while goal progress is
`currentValue / targetValue`, advanced by `Task.progressContribution` — which
defaults to `1.0` and appears nowhere in the UI. So every completed task moves its
goal by exactly one unit whatever it was worth, and the "book whose tasks cover
the whole thing" is that default showing through. Reclassified to decision `C3`.
This is the single clearest argument for splitting the document before mapping it.

**Two defects confirmed statically, one of them bigger than it reads.**
`LifeAreasScreen.kt` contains no navigation call at all, so `R2` is a genuinely
missing entry point rather than a broken one. And `SocialRepository` exposes only
`observeFeed`/`shareSummary` — no delete of any kind — so `R27b` ("let me delete
my own share") needs a repository method, a `firestore.rules` author clause, a
rules test and UI, not a button.

**The largest product gap in the app is not in the brief.** `app/src/main/res`
holds `values` and `values-night` only: no Hebrew, no RTL. Its author writes
Hebrew and its goal titles will be Hebrew. Recorded as `A1`.

## What this session deliberately did not do

- **No device pass.** Every repro note is static — derived from source, not from
  driving the app. This repo has been burned by a stale backlog premise before
  (`TODO_MUST/Submission.TODO.must.md` §1, where the item's premise was wrong and
  cost a session of planning), so nothing graduates to a GitHub issue until it has
  been reproduced on a real build.
- **No GitHub issues created.** They were asked for, and they are the correct next
  step — but only for items that survive reproduction. Creating them now would
  publish the static guesses as commitments.
- **No map charted.** Charting is one session's work by the skill's own rule, and
  it should start from a backlog whose dependencies have been seen.

## 🧪 Tests

**No test layer applies to this session** and none was run. Nothing under `app/`,
`functions/`, `firestore.rules` or `scripts/` was touched — the diff is Markdown
only (transcription, two backlog files, the TODO index, this changelog, the
session board). The project's four layers (JVM unit, instrumented, Firestore
rules, and manual device verification) all test behaviour this session did not
change. Stating it explicitly rather than skipping silently, per the testing
discipline.

Singletons: none taken. No Gradle build, neither AVD, and the live
`goalpilot-56e30` project was not touched.
