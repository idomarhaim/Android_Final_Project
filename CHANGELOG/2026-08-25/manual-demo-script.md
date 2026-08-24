# `manual-demo-script` — 2026-08-25

> **Summary:** The same demo, word for word in Hebrew — the narration itself rather than the
> plan, with the one sentence per act that carries the act marked, elaborations marked as the
> first thing to cut, and lines written to cover the two live model calls that stall mid-take.

Second unit of the same session. Ido asked for the demo script **in full, in Hebrew** — the
words themselves, not the cues. One new document:
[`docs/marketing/manual-demo-script-he.md`](../../docs/marketing/manual-demo-script-he.md).
`AUTO MODE`.

---

## 0 · Why this is a second file and not a translation

[`manual-demo-script.md`](../../docs/marketing/manual-demo-script.md), committed yesterday in
`6777ede`, is a **plan**: the running order, the prep checklist, the privacy window, the
ordering argument, and the recovery table. What Ido asked for now is the **narration** — what
he actually says, word for word, while his hands are on the phone.

Those are two different artifacts with two different readers-in-the-moment, and folding the
Hebrew into the English file would have made both worse: a teleprompter interleaved with an
argument about act ordering is unusable at the moment you need it, and RTL and LTR sharing a
line renders badly in every viewer either of us uses. So the split is: **the plan in English,
the words in Hebrew, cross-linked both ways.** Stage directions in the Hebrew file stay in
English for the same reason — so no line mixes directions.

## 1 · The language rule, and why this does not breach it

The standing rule is *all output — code, comments, file contents, docs — in English*. This
file is Hebrew, deliberately, and the derivation is: the rule exists so that **prose written
for a reader** stays in one language and so Hebrew RTL never lands in the terminal. Here the
Hebrew **is the deliverable's payload** — it is a spoken-word script, the same category as a
translated string resource, which this repo already ships in `values-iw/`. A demo script for a
Hebrew narrator written in English is not a stricter version of the rule; it is a useless
document.

Applied, rather than assumed: **the reply reporting this stayed in English and printed none of
the Hebrew**, which is the half of the rule that protects Ido's terminal.

## 2 · What the document contains

Thirteen acts matching `manual-demo-script.md` §2 beat for beat, each as **stage direction →
spoken line**. Beyond a straight translation, three things were added because a live narrator
needs them and a plan does not:

- **★ marks the one sentence per act that carries the act.** Twelve of them. Without a mark
  every line reads at the same weight, and the architectural point in Act 6 — *the model never
  hands back a score; the app computes it* — is the one that most reliably gets thrown away at
  the same pace as a tap instruction.
- **`[optional]` marks elaborations**, so overrunning has a defined thing to cut that is never
  the point of the act.
- **Lines written to cover the model calls.** Acts 2 and 5 each stall 3–5 s on a live call;
  the narration is written to keep talking through the spinner rather than falling silent,
  which is the failure §1.5 of the English file warns about.

Also carried over verbatim: the three privacy warnings, the *never call the run a score* rule
(as a Hebrew-specific prohibition — **ציון / אחוז הצלחה / שיעור הצלחה**), and the on-camera
recovery line for a mis-filed smart add.

## 3 · One thing the presentation source now gets wrong, and this file does not

`docs/presentation/goalpilot-presentation-source.md` Part 12, guardrail 5, says: *"do not
describe automatic scoring from health data, from goal progress, or from anything the
participant did not report — that is a design direction, not a shipped behaviour."*

**That was true when it was written and is now false.** `challenge-scoring` shipped `C14` /
[`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23) the same day: a
challenge scores itself from each participant's **linked goal**, and a goal is already what
Health Connect **and** completed tasks **and** manual logs all feed
(`projectChallengeScoreOnProgress`). A typed score carries who typed it.

So Act 9's narration says what ships — scoring from the linked goal, and attribution on a
typed one. **The presentation source was left alone**: `presentation-source` holds a live claim
on it, and correcting another session's file inside its own claim is exactly what the board
exists to prevent. Flagged here instead.

## 4 · Verification

**The one number spoken on camera was checked in source, not recalled.** The friend code is
said aloud as *"שישה תווים"* — `FriendCode.LENGTH = 6` in
`domain/model/FriendCode.kt:16`, over a 32-character unambiguous alphabet. Everything else
quantitative in the narration (`minutes ÷ 3 × difficulty`, the 30-day / 8-week / 6-month
windows, the four materials, the seven tour steps, the four bottom tabs) was already verified
against source for yesterday's file.

The cross-link into the English file's `#2--the-running-order` anchor resolves — that anchor
set was recomputed and diffed in `6777ede` and neither heading has moved since.

## 🧪 Tests

**No test layer applies.** One Markdown document; no Kotlin, no Cloud Function, no resource,
no rule. Nothing to compile and nothing to assert. No build was run and no device was touched —
sibling sessions hold the Gradle daemon and `emulator-5554`.

`Untested:` the only real check on a narration script is **reading it aloud against a running
app and timing it**, which is Ido's recording session and cannot happen here. The per-act
targets are carried from the measured 11-minute automated take, not from a rehearsal of these
words — stated rather than implied, because a script that overruns its act budget is the normal
failure and nothing in this repo would catch it.

## Files

- `docs/marketing/manual-demo-script-he.md` *(new)*
- `CHANGELOG/2026-08-25/manual-demo-script.md` *(new)*
- `SESSIONS.md` — claim row's `Owns` extended with the two paths above
