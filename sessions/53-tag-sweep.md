---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
issue: 53
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/
  - app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/
  - app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialPalettes.kt
  - app/src/test/java/com/idomarhaim/goalpilot/ui/
  # androidTest is `58-instrumented-order`'s in full while that session is live -- see the
  # board note at the top of this brief before claiming it.
  - CHANGELOG/<today>/53-tag-sweep.md
  - sessions/53-tag-sweep.md
created: 2026-08-21
---

# `#53`'s one held item — the `.tag` sweep, so the dark-neo collapse becomes survivable

**This is the only thing standing between [`#53`](https://github.com/idomarhaim/Android_Final_Project/issues/53)
and closed.** Everything else in that ticket shipped on 2026-08-20 in `05ec6aa`
(`c12-material-contract`), which left the issue open **deliberately** and named this item at the
bottom of its own comment. Read that comment first — it is the contract.

> 🔒 **Singletons.** Needs the **Gradle daemon** and a **device**. This is a visual rule; it cannot
> be signed off from source.

> 🚥 **RUNS AFTER `57c-chart-volume-and-raised`, and after all of `#57` a–c.** Corrected
> 2026-08-21, having first been filed as *before* `57c`. The reason is file churn, and it is
> decisive: `57c` owns `DonutChart.kt`, `SimpleBarChart.kt`, `StackedColumnChart.kt` and
> `ProgressRing.kt` — **exactly the four files this sweep writes words into** — and it rewrites
> them for volume and raised-3D. A label sweep before that rewrite is work the rewrite then has to
> carry; after it, this is a finishing pass over stable code. There is no correctness argument the
> other way: the *words before collapse* ordering §4.1 requires is **internal to this brief**,
> since both halves ship here.
>
> ⛔ **`58-instrumented-order` owns `app/src/androidTest/**` in full while it is live.** Check the
> board before your first write. If it is still active, either wait or drop the instrumented file
> from `owns:` and take the *Seen* half through `am instrument` on the existing suite.

## Read first

1. [AGENTS.md](../AGENTS.md)
2. `#53`'s issue comment — what shipped, and the one line naming this.
3. [`docs/PRODUCT_v0.3.md`](../docs/PRODUCT_v0.3.md) **§4.1** — the `.tag` rule as written.
4. [`ui/theme/MaterialSpec.kt`](../app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialSpec.kt)
   — its header table already states the rule and points at the arithmetic.
5. [`ui/theme/MaterialPalettes.kt`](../app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialPalettes.kt)
   `:47` — the KDoc that says, in the code, *"not yet applied at the call sites"*. That sentence is
   what this brief deletes.

## The rule, and why the dependency runs the way it does

§4.1: **a category is written in words beside its dot, *because* dark neo collapses the six
categorical hues into one ramp.**

`rampTint` is that collapse, built and unit-tested. It has **zero call sites at HEAD** — verified
2026-08-21 — and that is not an oversight: **the words are what make the collapse survivable.**
Ship the collapse first and you install the exact identity failure the rule exists to prevent, in
every chart at once. So the order inside this session is fixed:

1. **words first**, everywhere a category is rendered as colour;
2. **then** `rampTint` at those same call sites;
3. **then** look at it under dark neo.

Doing 2 before 1 is the one way to get this wrong, and it will look fine on every material except
the one it is for.

## Task

**Find every site where a category's identity is carried by colour alone, and give it words.**

Start from `GoalCategory` usage rather than from the charts — a chip in a list is as much a call
site as a donut segment, and the chips are the ones a chart-shaped search misses. Expect: the
analytics donut and its legend, the stacked column chart, category chips on goal rows, and the
widget if it renders a category.

Then apply `rampTint` at those sites so dark neo actually collapses, which is the half that makes
the words load-bearing rather than decoration.

⚠️ **A legend is not the same as words beside the dot.** A legend one scroll away from the segment
it names still asks the user to hold six colour↔word pairs in their head, which is the thing the
collapse makes impossible. If a site genuinely cannot carry a word inline, say so and name it —
that is a finding about the design, not a licence to fall back on a legend.

## Carries over

- **`57a-category-palette` changes the values this renders**, including the dark variant built
  against `#0C1520`. Run after it, or every frame here is of the old set.
- **`57c-chart-volume-and-raised` rewrites the four chart composables this writes into.** Run after
  it too — see the ordering note at the top.
- **`c12-material-contract` widened `ThemePaletteTest` from 4 schemes to 14 cells and it caught a
  real WCAG failure at 3.54:1 on its first run.** Expect it to have an opinion about anything you
  change here.
- **`#56` added a `MissedOccurrence` list to the dashboard** that renders task state as words, not
  colour. It is already `.tag`-shaped and is a worked example of the target, not a site to fix.

## Out of scope

- **Chart volume, raised-3D, backgrounds, entrance animation** — `57b`, `57c`, `57d`.
- **The category palette values themselves** — `57a`.
- Anything in `#55`/`#56`'s model work.

## Exit

- **Every category call site carries words**, and the list of them is in the changelog — a sweep
  whose coverage is not written down cannot be checked by the next person.
- `rampTint` applied, with a **JVM test that a category's rendered identity survives the collapse**
  (two categories that differ under `identity` must still be tellable apart under the ramp — by
  their words, which is the point).
- `ThemePaletteTest` green across the full 14-cell matrix.
- **Seen** on a device, under **dark neo specifically**: two different categories side by side,
  distinguishable. A green suite is not a look, and this rule exists because of what dark neo does
  to colour.
- `CHANGELOG/<today>/53-tag-sweep.md` · board row released · brief closed to `sessions/done/` with
  `status: done` in the same commit · **close `#53`** with the evidence, per `/kickoff` §5 step 4 —
  this is the last held item, so the *nothing left → close it* branch applies.
- Commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Say whether `#53` closed. If it did, `#48`, `#53`, `#54` and `#55` are all closed and **v0.3 is
feature-complete except `#51` (frozen)** — say that, and name the Wave 4 verification pass as the
next step. If anything is still held, name it and say what holds it.
