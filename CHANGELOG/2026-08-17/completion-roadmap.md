# completion-roadmap — 2026-08-17

> **Summary:** Ido deferred #51 (Hebrew/RTL) to after functionality; this session wrote the five briefs and the wave-ordered roadmap that carry the project to completion, and answered why it is not one session driving subagents.

## Why

Ido reported time constraints and asked whether development was stuck on #51, and
whether the remainder could run as one session driving subagents.

**It was stuck.** 8 of the last 10 session labels were #51 work (`51`, `51b`, `51c`,
`resource-guard-inputs`, `51d`, `51e`, plus `widget-hebrew-terminology` and its drain),
while #6, #7, #8, #9, #11, #48 and #50 had not moved since 2026-08-15.

## The decision, and the evidence that it is the right moment

`SWEPT_PACKAGES` is at **2 of 10**. `51e-sweep-components` was the package that promised
leverage and disproved it: well-factored presentational components hold no literals
because they take copy as parameters, so **the remaining eight packages are irreducible**,
one session plus a render pass each.

`Observed:` this is a clean freeze point, not an amputation:

- [`Goal.kt:111`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L111)
  keeps `GoalCategory.label` beside `localizedLabel()` deliberately, so the three unswept
  packages that read it still compile and render English.
- `AnalyticsLiteralSweepTest`'s `SWEPT_PACKAGES` is **opt-in** — an unswept package is
  *unswept, not failing* — so feature work can use plain English literals with a green build.
- `bc5ef69` fixed the guard's two holes, so whoever resumes #51 inherits a working
  instrument rather than `isProse`'s false-positive-on-the-remedy.

## What the freeze must actually do — found while planning, not assumed

`grep` for `AppLanguage` turned up **`ui/components/LanguagePicker.kt`, live in
`ProfileScreen`**. So עברית is selectable today, and `AppLanguage.DEFAULT = SYSTEM` with
`AppLocale` reading the device locale live means **a Hebrew-locale phone gets the
half-Hebrew app without touching the picker.** That second half is the one a picker-only
fix misses, and `sessions/hebrew-defer-freeze.md` step 2 addresses it by clamping the
SYSTEM branch rather than by changing `DEFAULT`.

## Why not subagents

`agent-topology-and-model-routing.md` §6 excludes them for sequential edits: subagents
share one filesystem, one git index and one Gradle daemon. The failure is already on the
record at the *session* layer, where a board, claims and release notes exist — a hook
installed mid-flight changed a live session's behaviour, 51e folded a sibling's file back
into its changelog, and one push carried three foreign commits. Subagents have none of
those protections.

On model routing: [`agent-topology-and-model-routing.md:18`](file:///C:/Dev/JARVIS/rules/agent-topology-and-model-routing.md#L18)
puts **Fable and Opus in the same Frontier tier** with no discrimination, so the committed
rule gives no basis to prefer Fable as a manager. `Untested:` no measurement of either
model on this repo's work exists; the claim is that the rule is silent, not that the
models are equivalent.

The copy-paste problem Ido wanted solved is solved by `sessions/<slug>.md` + `/kickoff
<slug>` — one typed line per session.

## The 🚥 hand-off line — Ido's mid-session addition

Asked for while this commit was being staged: **every session must end by saying whether he
may proceed to the next `/kickoff`, and to which one exactly.** Defined once in the roadmap
(§🚥) with six `GO` conditions and a per-session answer table, and pointed at from all five
briefs so no session derives it.

Two design choices worth recording:

- **It is a heading, not a sentence in the body** — same reason as the `📱` and `⏳` banners.
  A turn that ends *finished* and a turn that ends *do not proceed* otherwise look identical
  from outside, and Ido is the one person who cannot tell them apart.
- **`GO` names a slug, never a wave.** *"Proceed to wave 2"* pushes the derivation back onto
  him, which is the whole thing being removed.

The pending-brief case is handled explicitly rather than left to improvise: waves 3–4 have
no briefs by design, so the last wave-2 session ends in `STOP` with *write them against
HEAD* as its next — otherwise a session with nothing to hand to would have to invent an
answer.

`Untested:` no session has run under this requirement yet; whether the two headings are
enough shapes is unproven and will show on wave 1.

## 🧪 Tests

**No test layer applies.** This session wrote briefs and a roadmap; it changed no
`app/src/` file, ran no build and touched no device. Stated explicitly rather than
skipped.

## Files

New: five briefs under `sessions/`, `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md`,
this file. Modified: `SESSIONS.md` (own claim row).
