---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 11
created: 2026-08-20
---

# `#11` — repeat-tappable fill buttons, and the measure that makes them possible

**Independent of `C20` and of #9 — verified 2026-08-20: no file touching `Goal.unit` also touches
`TaskEstimate`.** Can run in parallel with #9, #8 or #6.

Needs the **Gradle daemon**; a device or the cloud emulator for the render pass.

> ⚠️ **Conflicts with `c20-build-half` on `data/firestore/dto/Dtos.kt`** — `unit` at `:48`, `level`
> at `:109`. Same file; do not overlap. **Independent of `9-duration-box`**, verified 2026-08-20:
> no file mentions both `unit` and `looksLikeFallback`.

## Why it exists — and it is not hypothetical

`R25`: *"For the task 'drink 4 litres a day' I have several fill buttons I can tap more than once
(250 ml, 500 ml, 750 ml, 1 L)."*

**"Drink 4 Liters of Water Daily" is a live goal on Ido's account**, and it currently reads
`Health · 1/100 %` — tracked in **percent**, not litres. That is the defect, visible on his own
screen.

## What a unit is now — §1.3, and this is the bulk of the work

`domain/model/Goal.kt:63` and `data/firestore/dto/Dtos.kt:48` both hold `unit: String = "%"`. §7.1
replaces that free-text field with a **closed kind plus a free word**:

```
kind: COUNT | DURATION | DISTANCE | VOLUME | MASS | MONEY | PERCENT   // fixes every arithmetic
word: String                                                          // "books", "chapters", "pushups"
```

**The kind is app logic** — its seven labels are translated. **The word is user content** — it is
not, ever (`C15b`). Getting that boundary wrong is how #51's sweep gets re-broken.

## The ladder is arithmetic, not a table — §1.3

> The **AI judges, the app computes**. The model answers only *do buttons fit, and what is counted*;
> the ladder is **`target / 16` rounded, at `1× 2× 3× 4×`**.

On *"drink 4 L a day"* that yields `[250 ml] [500 ml] [750 ml] [1 L]` exactly, and it stays right at
a 40 L target. **The model never returns a number** — §3.1 measured free numbers swinging **2×
run-to-run** and **1.8× between languages**. If you find yourself hard-coding a millilitre table,
you have rebuilt the thing the formula exists to replace.

## Where the row lives — §1.3, §4.6

- **Input mode is per goal:** `buttons · number · tick · auto`, and **whether logging adds or sets
  rides it** — *per goal, because a global rule is the granularity error*.
- A tap writes a **`ProgressEntry`** (`domain/model/ProgressEntry.kt:8`). §4.6 makes `currentValue`
  **a sum over entries** rather than a stored aggregate, so a running tally needs **no second
  counter**. Check what `currentValue` is at HEAD before you build on it — #49 already removed its
  client writers, and `c20-build-half` may be moving more.
- **An entry is editable forever, every edit is always marked**, the original is recoverable one tap
  away, and a delete is **kept struck through** rather than vanishing. Ido overturned the simpler
  design here; do not re-simplify it.

## The trap

**The migration.** Every existing goal has a free-text `unit`, mostly `"%"`. Mapping those to a
`kind` is a guess for anything that is not literally `"%"`. Decide explicitly: map what is
unambiguous, leave the rest **absent** and ask, and **never** invent a kind from a string match.
Write the decision down.

## Exit

- JVM unit for the ladder formula at several targets — including the two the ticket names (4 L and
  40 L), which are the cases the formula exists to get right.
- JVM unit for the migration mapping, both directions.
- Instrumented for the button row and the running tally.
- **Seen** — and if the live "Drink 4 Liters" goal can be reproduced in a fixture, show it reading
  in litres rather than percent. That is the ticket's own acceptance criterion.
- `CHANGELOG/<today>/11-fill-buttons.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. Name whichever of `/kickoff 9-duration-box`, `/kickoff
8-notifications`, `/kickoff 6-silent-filing` is still unrun.
