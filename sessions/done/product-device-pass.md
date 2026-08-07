---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: done
created: 2026-08-06
completed: 2026-08-06
landed: see `CHANGELOG/2026-08-06/product-device-pass.md` and this file's own move commit
issues: https://github.com/idomarhaim/Android_Final_Project/issues/2 … /11
---

> **Done 2026-08-06.** `D2`–`D5` confirmed on `Pixel_10_Pro_XL` and filed as issues
> `#2`–`#5`; `U1`–`U6` filed as `#6`–`#11`. `D1` resolved **not a defect** — an
> undecided model question — and was *not* moved into
> `TODO_FUTURE/ProductModel.TODO.future.md` because the concurrent
> `product-model-map` session owns that file; the verdict waits under `D1` in
> `TODO_OPTIONAL/ProductReview.TODO.optional.md`. Device UX findings appended as
> `A5`–`A9`.
>
> **Second sitting, 2026-08-07** (`CHANGELOG/2026-08-07/product-device-pass.md`):
> re-claimed to close the first-run empty states with an approved `pm clear`. That
> did **not** reach the zero-data states — Firestore restores everything on
> sign-in — but it found `A10` (a cold cacheless load is a blank page and an 8 px
> dot) and an emulator trap (`pm clear` wedges Play Services). `D1` was handed to
> `product-model-map` as a liftable `C14` block on Ido's call. Only the true
> zero-data empty states remain open, and they need a throwaway account.

# Product review, device pass: reproduce the defects, walk the app, then file issues

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`. Say `AUTO MODE` at the start if you want it to commit and
push without asking.

**Read first** — [`AGENTS.md`](../AGENTS.md), then
[`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md)
(the backlog this session verifies), then
[`Product and UX Reviews/2026-08-06-brief-review.md`](../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md)
(the source text, `R1`–`R28`), then
[`CHANGELOG/2026-08-06/product-review.md`](../CHANGELOG/2026-08-06/product-review.md)
(why the backlog is split the way it is).

**Task** — three things, in this order:

1. **Reproduce `D1`–`D5`** against a real build on the emulator. For each, record
   one of: *confirmed* (with the code path that causes it), *not reproducible*,
   or *needs a physical phone*. `D1` (challenge ↔ tasks / Health Connect) is the
   one to start with and the one most likely to turn out to be a design question
   rather than a bug — read `ChallengeRepositoryImpl` and `SyncHealthDataUseCase`
   together before touching the device, and if it resolves to "what *should* a
   challenge score from?", move it to `TODO_FUTURE/ProductModel.TODO.future.md`
   rather than fixing it. `D3` is a latency claim — **time it**, don't eyeball it.
   `D6` is already reclassified; do not re-open it.
2. **The device half of the product/UX pass** — the part
   `ProductReview.TODO.optional.md` says is still owed under the additions
   section: onboarding and empty states, first-run comprehension, tap targets,
   both skins in dark mode, error and offline states, and whether the dashboard's
   information order matches what someone opens the app for. Append findings as
   `A5`, `A6`, … to that same file, keeping them visibly the agent's rather than
   Ido's.
3. **Then, and only then, file GitHub issues** for what survived: every
   *confirmed* defect and the `U1`–`U6` items. The repo has **zero issues** so far,
   so this session creates the first ones — `gh` is authenticated with `repo`
   scope. Per `/triage`, `TODO/` is the pre-commitment funnel and graduating an
   item is **one-way**: annotate the TODO entry with the issue number, and never
   mirror issue state back. Do **not** file anything from
   `ProductModel.TODO.future.md` — those become wayfinder tickets in a different
   session, and filing them twice is how a map gets two sources of truth.

**Carries over**

- The classified backlog and every `D`/`U`/`A` id —
  [`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`](../TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md).
- The transcription those ids cite —
  [`Product and UX Reviews/2026-08-06-brief-review.md`](../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md).
- What was confirmed statically and what was not, and why `R12` is not a bug —
  [`CHANGELOG/2026-08-06/product-review.md`](../CHANGELOG/2026-08-06/product-review.md).
- The emulator and Gradle-daemon rules, and the second AVD —
  [`SESSIONS.md`](../SESSIONS.md); launch with `scripts/run-goalpilot.ps1`
  ([`scripts/README.md`](../scripts/README.md)).
- The standing warning that a backlog item's premise can be stale —
  [`TODO/TODO_MUST/Submission.TODO.must.md`](../TODO/TODO_MUST/Submission.TODO.must.md) §1.

**Out of scope**

- **Fixing anything.** This session establishes what is real. Fixes are separate
  sessions working the issues it files. The exception is a genuine one-liner found
  and proven in passing — say so explicitly if you take one.
- The 13 `C` decisions in `TODO_FUTURE/ProductModel.TODO.future.md`.
- `GoalPilot_spec_EN.docx` — *Frozen / off-limits* in `AGENTS.md`.
- `A1` (Hebrew / RTL). It is a scope decision for Ido, not a device finding.

**Exit** — every `D1`–`D5` carries a verdict; the device findings are appended to
the optional backlog; issues exist for the survivors with TODO entries annotated;
`CHANGELOG/2026-08-06/<this-session>.md` written (or the day folder for whenever
it runs); tests green at every layer touched — and if the session only reads and
files, say so explicitly rather than skipping the `## 🧪 Tests` section; claim
released on [`SESSIONS.md`](../SESSIONS.md); commit on approval.
