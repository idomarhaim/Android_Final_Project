# KB candidates — `64-area-success-failure`, 2026-08-23

Session working `#64` (`C19`'s success/failure run). Every entry stands alone: no
transcript is a source, and none of these can be reconstructed from one.

---

## 1 · `onNodeWithText(substring = true)` has a THIRD trap, and it is in the positive direction

**Claim.** `kb/dev/look-at-your-own-output.md` §5.4–§5.5 record two traps in Compose text
assertions: bidi isolates defeat an **exact** match (false red on a positive, silent false
green on a negative), and `substring = true` makes a **negative** match the control beside it
(false red). There is a third, it hits the **positive** form, and it is the commonest of the
three in practice: **`onNodeWithText(…, substring = true)` throws when more than one node
matches**, with the message

```
Failed to perform checkIsDisplayed check: Expected at most 1 node but found 3 nodes
that satisfy (Text + EditableText contains 'kept' … as substring)
```

The failure **names the ambiguity, not the subject**, so it reads as *the assertion is
broken* when the component is rendering perfectly — and the instinctive fix, dropping
`substring`, walks straight back into §5.4's false green.

The remedies, in the order to reach for them:

- `onAllNodesWithText(…, substring = true).onFirst()` where the line exists only to prove the
  matcher still fires (the control that stops a paired negative passing vacuously);
- `assertCountEquals(n)` with the second source named in a comment, where the count is the
  claim;
- **`onLast()` for a clipped-capture probe**, which is strictly better than `onNodeWithText`
  and not merely a workaround: the bottom-most occurrence is the one a clipped bitmap loses
  first, so a probe that takes the last match tests exactly what the probe exists for.

**Why.** *Observed:* 2026-08-23, `emulator-5554`, `SuccessFailureRunUiTest` — **6 of 20 tests**
failed this way on the first device run and **1 more** on the second, on a component with no
defect in it. Both causes are the same shape and neither is visible in the source, because the
two matching nodes are written far apart:

- `"kept"` appears on the **pair label**, in the **legend**, and inside the explanatory
  sentence *"a window counts as kept when everything due in it was done"* — three nodes, one
  card.
- `"months"` appears in a goal's *"idle 4 months"* meta line **and** on the `6 months` range
  chip — two nodes, ~200 lines apart in the file.
- `"no next step"` appears in the footer **and** in every row's meta line.
- On a multi-frame render page, the last-frame probe string appears **once per frame**.

*Rejected:* making the strings unique to satisfy the matcher — that is the shape §5.4's remedy 1
already calls fragile (*"a fact about two literals that no later reader will re-derive"*), and
here the repetition is **correct**: a legend that used a different word from the label it
explains would be the defect.

*This also part-answers §5.5's own `Untested:` hedge.* That section says its negative-direction
trap was reasoned about rather than watched, because the AVD was held during `#66`. This entry
is the family caught **on a device, failing**, seven times — but note it is the **positive**
direction; §5.5's negative-direction claim is still not demonstrated and should stay hedged.

**Destination.** `kb/dev/look-at-your-own-output.md` — a new subsection under §5, beside §5.4
and §5.5.
**Anchors.** §5.4, §5.5, §4j (the vacuous pass).
**Supersedes.** Nothing. It extends §5; no existing claim becomes false.
**Status.** Ready.

---

## 2 · A KDoc that names its future consumer is a claim about the future, and it fails flattering

**Claim.** When a property is built ahead of its reader and its documentation **names that
reader** — *"§4.7's failure surface is the reader it is written for"* — the sentence is a
prediction, and it goes stale in the direction that makes it look **more** authoritative rather
than less. A later session opening that file reads a decision that was never taken, from an
author who did not have the consumer's requirements in front of them. So: **when you are the
named reader and you decline the property, correcting that sentence is part of the ticket** —
not a tidy-up, and not optional, because the pointer is what sends the next person wrong.

The correction has a shape worth copying: say the reader **arrived**, say it **does not use
this**, say **why the property is still not wrong** (it answers a narrower question), and point
at where the full derivation lives. Deleting the sentence loses the reason; leaving it standing
is the failure.

**Why.** *Observed:* 2026-08-23, GoalPilot. `OccurrenceState.countsAsFailure` (built by `#56`)
answers *"is this a failure?"* with `MISSED` alone and its KDoc named `#64`/`C19` as its
intended reader. `#64` arrived and needed a **different set** — `MISSED`, `DAY_PASSED`,
`WINDOW_CLOSED` — because §2.3's three-word vocabulary predates `#56`'s own split of `MISSED`
into three constants, so *"`MISSED` is a failure"* is a sentence about **one rung** and §4.7
counts **windows**, which all four rungs have.

The decisive evidence was not textual: `OccurrenceDraft.toOccurrence` can produce only
`ALL_DAY` and `DEADLINE`, so `MISSED` is **unreachable in the shipped app** and a run built on
`countsAsFailure` would have shipped a permanent `0 missed` — passing every test, wrong on
every device. A second, independent check pointed the same way: `meetsUserInDailyReview`
already groups all four, and `DailyMissReview` already shows all four to the user **as misses**,
so the narrow reading would have printed `0 missed` about windows the daily review had named
that morning.

*Rejected:* **widening `countsAsFailure`** to match. It has a deliberate whole-enum guard test
(*"so a fifth state added later cannot quietly join either set"*), it is correct for the
question it asks, and changing a committed property to suit one new consumer is how a shared
enum becomes the union of everyone's needs. The new consumer states its own mapping and says
why.

*Rejected:* **saying nothing**, on the grounds that the property is untouched. That leaves the
pointer, which is the whole hazard.

**Destination.** `kb/dev/claim-provenance.md` is the nearest home — this is a claim whose
provenance was *"written for a reader that did not exist yet"*, which is a hedge class that
page does not yet name. Second choice: `kb/dev/enum-and-label.md`, which already holds the
*don't fold two questions into one enum* material.
**Anchors.** `rules/claim-provenance.md` (the `Untested:` marker), `kb/dev/enum-and-label.md` §5.
**Supersedes.** Nothing.
**Status.** Ready.

---

## 3 · In a prototype, the round notes outrank the fixture data — and only some frames get repaired

**Claim.** `kb/dev/a-later-prototype-outranks-the-brief.md` establishes that a prototype beats
a brief where they disagree. There is a **second** precedence question inside a single
prototype, and it is not the same one: when a prototype's **fixture data** contradicts its own
**"what the rounds caught"** notes, the notes win — because a revision fixes the frame it was
about and routinely leaves the other frames on stale numbers nobody looked at again.

Practical form: before deriving a rule from a prototype's data, **check whether its README
records a round that fixed exactly this**, and check whether the frame you are reading is the
one that round repaired.

**Why.** *Observed:* 2026-08-23, `docs/prototypes/2026-08-13-area-success-failure/` (revision 5,
the asset `#41` was resolved against). `#64` had to decide whether a dot in the run is one
**occurrence** or one **time bucket** — the brief and the issue both say *"a window is an
occurrence"*, while the screen's own sentence says *"a window counts as kept when **everything**
due in it was done"*.

The fixture data is **internally inconsistent and cannot settle it**: `learn`'s tally
(`5 kept · 2 missed · 1 nothing-due`) matches its 8-character run string exactly, while
`health`'s (`23 · 5 · 2`) matches neither its own 8-dot run nor any 8-week reading. The README's
round list explains why — round 5 reads *"Learning showed 11 kept / 7 missed above a list with
one active goal and two asleep. The numbers contradicted the list they sat on top of. **Now 5 /
2, and the run pattern matches both**"* — so **`learn` was repaired and the other two were
not**. Reading `health` as authoritative would have imported a defect the prototype had already
diagnosed and fixed elsewhere.

The tie-break was then decided by a third input neither number could give: `nothing-due` is in
§4.7's state list and on the legend, and it is **only representable on a bucket** — an
occurrence cannot be *"nothing due"*. A model where a spec'd state has no referent has silently
dropped it.

*Rejected:* asking Ido which reading he meant. It turns on the artifact rather than on him —
`rules/derivable-decision.md` — and `rules/question-axis-naming.md`'s fork check applies
directly: the two options looked like rivals and one of them had no referent for a state the
spec names, so the fork was false rather than hard.

**Destination.** `kb/dev/a-later-prototype-outranks-the-brief.md` — a new section, since it is
the same page's subject one level in.
**Anchors.** `rules/derivable-decision.md`, `rules/question-axis-naming.md`.
**Supersedes.** Nothing. The existing brief-vs-prototype claim is untouched.
**Status.** Ready.

---

## 4 · A swept package's literal guard fires on TEST TAGS, and the idiom that satisfies it

**Claim.** In this repo, `AnalyticsLiteralSweepTest` calls any string literal with **two or
more alphabetic words** user-facing prose, over every file in a `SWEPT_PACKAGES` package. A
Compose **test tag** is a string literal, so `const val TAG_KEPT = "success_failure_kept"` fails
the build in `ui/components` or `feature/analytics` — with a message telling you to move it to
`res/values/`, which is exactly the wrong advice for a tag.

The idiom that satisfies it is already in the package and worth naming: **camelCase, and
concatenation rather than interpolation** — `"materialTile_" + material.id`, not
`"material_tile_${material.id}"`. camelCase is one alphabetic run to the regex, and a
concatenation keeps the literal itself down to one word.

**Why.** *Observed:* 2026-08-23, `#64` — nine tag literals in one new `ui/components` file
failed the sweep on the first full JVM run. The guard is doing its job; it simply has no way to
tell a tag from a sentence, and its own KDoc says the rule is *"crude on purpose"* because
*"a guard nobody can predict is a guard people route around"*. Naming the idiom is the cheap
half of not routing around it.

Note the asymmetry that makes this worth writing down at all: `feature/calendar` uses
`"calendar_load_bar"` freely and is **correct**, because that package is **unswept**. So the
same literal is fine in one package and a build failure in another, and nothing at the call
site says which.

*Rejected:* adding the package to an exemption list — the sweep has none by design (*"absent
from that list is unswept, not exempt"*), and inventing one for tags would open the door the
`❌ Do not add your package to SWEPT_PACKAGES as a favour` note is holding shut from the other
side.

**Destination.** `kb/dev/untranslatable-idioms.md`, which is where this repo's literal-sweep
material lives. Failing that, GoalPilot's own `AGENTS.md` § Pitfalls.
**Anchors.** `AnalyticsLiteralSweepTest`, `MaterialPicker.materialTileTag`.
**Supersedes.** Nothing.
**Status.** Ready.
