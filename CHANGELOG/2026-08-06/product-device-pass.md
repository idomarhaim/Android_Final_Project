# 2026-08-06 — `product-device-pass`

> **Summary:** Opened with `/kickoff product-device-pass` from the brief `product-review` wrote the same day.

Opened with `/kickoff product-device-pass` from the brief `product-review` wrote
the same day. **Read-only against the codebase**: this session established what is
real and filed it. Not one line of `app/`, `functions/`, `firestore.rules` or
`scripts/` was changed — fixing is out of scope by the brief's own terms, and no
one-liner was taken.

## What it did

Reproduced `D1`–`D5` against a real debug build on `Pixel_10_Pro_XL` signed in as
Ido with his own live data, ran the device half of the product/UX pass, and filed
**the repository's first ten GitHub issues** (`#2`–`#11`; `#1` was the backend PR).

## The five defects

| id | verdict | evidence |
|---|---|---|
| `D1` challenge ↔ tasks / Health Connect | **not a defect — an undecided model** | score has exactly one writer in the whole codebase |
| `D2` no route from a life area into its goals | **confirmed** → [#2](https://github.com/idomarhaim/Android_Final_Project/issues/2) | only `Edit`/`Delete` are clickable; the goal count is a label |
| `D3` completing a task is too slow | **confirmed, measured ~2 s** → [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3) | two screen recordings, frame-timed |
| `D4` a shared photo cannot be opened | **confirmed** → [#4](https://github.com/idomarhaim/Android_Final_Project/issues/4) | pixel-identical after the tap; no interactive node in the card |
| `D5` cannot delete your own share | **confirmed** → [#5](https://github.com/idomarhaim/Android_Final_Project/issues/5) | no affordance at all; long-press yields nothing |

### `D1` went the way the brief suspected it might

The brief said to start here because it "may turn out to be a design question
rather than a bug". It did. The symptom is exactly as Ido reported, but nothing
broke — the wiring was **never specified**:

- `ChallengeParticipant.score` has **one writer**: `reportScore`, reached only
  from the manual dialog. Joining sets it to `0.0`; nothing else touches it.
- `ChallengeType` carries `RUNNING`/`STEPS`/`SLEEP`/`WORKOUTS` and is **purely
  presentational** — an icon, a label, and a default `metricUnit` *string*. No
  code branches on it to source a score.
- `SyncHealthDataUseCase` writes a `ProgressEntry` against a **`Goal`**, and never
  mentions challenges.
- On the device: Ido's own **"August Steps Race"**, a `STEPS` challenge, reads
  **"#2 · 0 steps"** with "Report score" as the only mover — while his steps flow
  into goals. Two representations of the same walk, joined by nothing.

So the real question is *what a challenge should score from*, and it cannot be
answered before `C7` (what a unit **is**), because `Challenge.metricUnit` is free
text with the same disease as `Goal.unit`. **It belongs in
`TODO_FUTURE/ProductModel.TODO.future.md` and was not moved there** — that file is
owned by the concurrent `product-model-map` session. Left recorded under `D1`
instead, for Ido to re-assign.

### `D3` was timed, not eyeballed — and the cause is sharper than the guess

`screenrecord` emits a frame only when the screen changes, so the dead-time column
is real stillness rather than a sampling artefact.

| sample | tap → checkbox changes | dead screen after the ripple ends |
|---|---|---|
| completing | **2.24 s** | 1.20 s |
| un-completing | **1.94 s** | 0.88 s |

The donut moves at the same instant, not sooner (`2 / 100 %` on the last frame
before, `3 / 100 %` on the first frame after). The only feedback in those two
seconds is the ripple, which itself ends after ~0.7 s.

The backlog guessed "a Firestore round-trip the UI waits on rather than an
optimistic local update". Right, but understated: **`setDone` is a
`runTransaction`**, and a Firestore transaction is *server-only* — it cannot be
served from the offline cache. So there is no local write to render early. And
`GoalDetailViewModel.toggleTask` is one line that **discards the `Resource`**, so
no failure has anywhere to go.

Which is why the offline control experiment mattered: with the network down the
same tap does **nothing at all** — no checkbox, no error, no snackbar — while
logcat carries `UNAVAILABLE … UnknownHostException`. Filed with `D3` as one issue,
because an optimistic update *alone* would turn that silent no-op into a silent
lie.

## The device half of the UX pass — `A5`–`A9`

Appended to `ProductReview.TODO.optional.md`, kept visibly the agent's:

- **`A5`** completing a task offline is a silent no-op *(rides in `#3`)*
- **`A6`** the app never says it is offline; it cold-starts fully from cache with
  live-looking numbers and no staleness cue anywhere
- **`A7`** the dashboard answers "how am I doing?" and never "what do I do now?" —
  goals are ~4 screenfuls down behind two permanent setup cards and five generic
  tips, and **no task appears on it at all**
- **`A8`** one tap target under 48 dp — the ⋮ on a "Goals with no area" row is
  `48 × 38 dp`. **57 of 58 clickable nodes pass**, measured from the accessibility
  tree across five screens rather than judged by eye
- **`A9`** the shared photo has no content description *(rides in `#4`)*

### Checked and deliberately **not** filed

Recorded in the backlog so nobody re-raises them:

- **Both skins in dark mode are fine** — Aurora and Blossom each walked across
  Home, Goals, goal detail and Profile. Consistent with `ThemePaletteTest`. The
  category icon colours staying constant across skins is deliberate, per
  `AGENTS.md`, not a skin bug.
- **The AI coach is live, not falling back.** Tips read "Start Small", "Track Your
  Wins", "Set SMART Goals", "Celebrate Progress"; the local fallback can only ever
  emit "Start with one goal", "Keep the streak alive" or "Nudge: {goal}". So the
  GROQ model id has **not** rotted as of today — the check `AGENTS.md` demands
  before a demo, and it passes.
- **The FAB does not cover the last card** — it overlaps mid-scroll, which is
  ordinary Material behaviour; at full scroll both lists clear it.
- **Hebrew content is already real on Ido's account** — areas `בריאות`/`לימודים`/
  `קריירה`/`זוגיות`, a task `אימון ריצה`, a Hebrew profile name. This does not
  change `A1`'s status as Ido's scope decision, but it does mean `A1` is not
  hypothetical: Hebrew is being laid out LTR in English chrome today.

## 🧪 Tests

**No suite was run, and none applies.** This session read code and drove a build;
it changed no Kotlin, no Gradle file, no `firestore.rules` and no Cloud Function,
so the JVM, instrumented and rules layers all have nothing to exercise. The only
files it wrote are Markdown, plus ten GitHub issues.

Verification here was **behavioural and instrumented instead**, and that is what
the evidence above is:

- frame-timed screen recordings for `D3` (two samples, both directions, plus an
  offline control);
- `uiautomator` accessibility dumps for `D2`, `D4`, `D5` and the tap-target sweep —
  which is stronger than a screenshot, because it says *which* nodes are clickable
  rather than what a rendering looks like;
- pixel-difference (PSNR/MSE) rather than a visual impression for "nothing
  happened";
- logcat for the offline write failure.

**Not verified — and the reason, not an excuse:** first-run comprehension and the
empty states. Every feature screen *has* an empty-state branch in code (Analytics,
Challenges, Dashboard, AddEditGoal, GoalDetail ×2, LifeAreas, Social ×2), but none
could be **seen**: the only account on the device is Ido's and it is full of real
data. Reaching them needs `pm clear` — which signs him out — or a throwaway Google
account. Neither is this session's call to make.

## Live data touched

`goalpilot-56e30` **was** written to, unavoidably and minimally: reproducing `D3`
means completing a real task. The task `אימון ריצה` under "Run 10k" was toggled
done and then back, and the goal returned to `2 / 100 %` and the account to
`70 pts` — verified on screen afterwards. Nothing else was created, edited or
deleted; the theme was switched to Blossom for the dark-mode check and **restored
to Aurora**.

## Note for whoever picks up `#3`

`deleteTask` sits on the line below `toggleTask` with the identical
throw-away-the-result shape, and the other repositories were not audited. Worth a
sweep rather than a one-line fix.
