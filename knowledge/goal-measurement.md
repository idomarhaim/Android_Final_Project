# When a goal resists measurement, measure the behaviour that produces it

**TL;DR:** Some objectives have no honest outcome metric — *"understand real estate"*,
*"be a better listener"*, *"stay current in my field"*. The two usual responses are both
bad, and GoalPilot takes a third:

| Response | What it produces |
|---|---|
| **Force a number** | a fake the user learns to ignore |
| **Leave it unmeasured and say nothing** | no feedback at all |
| **Measure a leading indicator** ✅ | *"read 2 market reports a week"* — real feedback, without pretending the outcome was quantified |

Consequence for the measure-suggestion feature: it needs **two output shapes**, not one —
a measure **on** the goal, and a recurring activity **under** it — and choosing between
them is part of the suggestion, not an implementation detail.

*Provenance: session `c7-what-is-a-unit`, 2026-08-10 — GitHub issue
[#14](https://github.com/idomarhaim/Android_Final_Project/issues/14) (`C7`), from `E5`
and `E6` of [the 2026-08-09 entity brief](../Product%20and%20UX%20Reviews/2026-08-09-entity-model-brief.md).*

## 1. Ido wrote both halves without naming the resolution

`E6`, on his own *"understand real estate"* goal, holds both positions in one paragraph:
the agent **might** advise defining something more measurable — *"on the other hand,
maybe not necessarily, because one of the things is to be well-versed in what is current
in real estate, and that requires some endlessly recurring task."*

**The recurring task is the measurable thing.** He reached for the construction and did
not name it.

Separately, answering `C7`, he stated the product position directly: goals in life
usually need to be measurable in some form, so if a user may choose an unmeasured goal
then the agent must at least recommend making it measurable — **and recommend how, in
terms of the goal he actually chose**. A leading indicator is the only construction that
satisfies both statements at once.

## 2. What this constrains in the spec

- **Unmeasured stays legal.** `C7` made absence of a measure the default state, not a
  defect to be repaired. See the resolution on
  [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14) §2.
- **The suggestion is concrete or it is not offered.** *"Consider adding a metric"* is
  worthless; the proposal is a complete measure — kind, word and target — phrased against
  that goal's own title.
- **It never applies itself**, and it is dismissible permanently per goal. A nudge that
  cannot be switched off makes "unmeasured is legitimate" false in practice.
- **It has a non-AI fallback**, per the map's standing free-model rule: with no model
  reachable, a plain **"Add a measure"** control still opens the picker empty.
- **The recurring shape hands off.** A proposal of the second shape creates a *recurring
  activity*, which is [`C5` #21](https://github.com/idomarhaim/Android_Final_Project/issues/21)'s
  mechanic and [`C8` #24](https://github.com/idomarhaim/Android_Final_Project/issues/24)'s
  planning surface — named here, specced there.

## 3. Why it is worth writing down

The failure mode is common in goal-tracking and OKR tooling, and it is bimodal: tools
that insist on a number for everything produce vanity metrics, and tools that permit
unmeasured goals quietly abandon them. The leading-indicator shape is what lets a product
refuse both without contradicting itself.

**Deliberately not promoted centrally** — it is product-domain reasoning about how
GoalPilot measures, closer to product design than to engineering practice. Promote only
if a second project shows the same shape.

## See also

- [ui-error-conventions.md](ui-error-conventions.md) — the other page in this bundle
  where an existing rule needed a boundary rather than an exception.
