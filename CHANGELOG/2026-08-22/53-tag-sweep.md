# `53-tag-sweep` — 2026-08-22

> **Summary:** `#53`'s last held item. §4.1's **`.tag`** rule is now real in both halves and in the
> order the rule itself requires: **words first**, at every categorical mark that can carry one, and
> **then** `rampTint` wired at the single seam every category fill passes through. The sweep's own
> finding is that the chips were never the problem — nine of the eleven sites already carried words,
> and the two that did not were the two charts, one of which had been carrying a `label` it never
> drew since the day it was written. Three things were found by **looking at a frame** rather than by
> a test: labels that fitted only at the top and bottom of the ring, four of ten wedges left blank,
> and a whole instrumented run that was blank because the emulator was dying. JVM **746/0** (+17
> tests, +1 class), instrumented **204/0**, 16 palette frames. Dark neo **seen**.

---

## 1. What the rule says, and why the order inside this session was fixed

§4.1:

> **`.tag`** — a category is **written in words** beside its dot, because dark neo collapses the six
> categorical hues into one ramp and **colour stops carrying identity**.

`C12` (`05ec6aa`) built `MaterialPalettes.rampTint`, unit-tested it, and left it with **zero call
sites on purpose**, saying so on `#53`: shipping the collapse before the words installs the exact
identity failure the rule exists to prevent, in every chart at once, and it looks correct on the
three materials it is not for. So this session did words first, collapse second, look third.

---

## 2. The sweep: every site where a category's identity is carried by colour

Started from `GoalCategory` / `LifeArea` usage rather than from the charts, as the brief asked —
`toGoalAccent`, `toGoalInk` and `toComposeColor` are the three seams, and every call site of them
was read.

**The result is the finding: the chips were already right.** The brief expected a chart-shaped
search to miss them; in fact they are the part that never broke.

| Site | Mark | Before | Verdict |
|---|---|---|---|
| `ui/components/GoalCard.kt` | icon squircle + progress bar | icon + `category.localizedLabel()` in the meta line | ✅ already `.tag` |
| `feature/analytics/AnalyticsScreen.kt` · `LegendRow` | dot + icon | dot + icon + `slice.name` | ✅ already `.tag` |
| `feature/analytics/AnalyticsScreen.kt` · `DonutCenter` | percentage in ink | `selected.name` under it | ✅ already `.tag` |
| `feature/goals/GoalsScreen.kt` · `LifeAreaGroupHeader` | icon tint | icon + `area.name` | ✅ already `.tag` |
| `feature/goals/AddEditGoalScreen.kt` | `FilterChip` leading icon | icon + `area.name` | ✅ already `.tag` |
| `feature/goals/GoalDetailScreen.kt` | `ProgressRing` | `categoryLabel` in the ring's centre | ✅ already `.tag` |
| `feature/lifeareas/*` (rows, dropdown, proposals) | icon tint | icon + `area.name` throughout | ✅ already `.tag` |
| `ui/components/SimpleBarChart.kt` · `HorizontalBarChart` | bar fill | `BarItem.label` drawn above every bar | ✅ already `.tag` |
| `ui/widget/WidgetTileRenderer.kt` | lozenge + rows | `area.name` / `row.name` beside each | ✅ already `.tag` — and a widget has no material, so the ramp never reaches it |
| **`ui/components/DonutChart.kt`** | **wedge** | **`DonutSlice.label` carried in and never drawn** | ❌ **fixed here** |
| **`ui/components/StackedColumnChart.kt`** | **band** | **`StackedSegment` had no `label` field at all** | ❌ **fixed here** |

Two sites were considered and **excluded**, and they are excluded on a principle rather than on
effort:

- **`LifeAreasScreen`'s colour swatch grid.** The thing being chosen *is* the colour, so the colour
  is not standing in for an identity — it is the identity. A `.tag` there would name the swatch after
  itself.
- **`ReliefPicker`'s preview arcs.** They are a picture of a *material*, drawn from category hues
  because they were to hand. No category is being reported, so no category needs naming.

---

## 3. `DonutChart` — the wedge says what it is

`DonutSlice.label` existed from the first commit and reached the component on every call. Nothing
ever drew it; the legend below the chart carried the words instead. That is the failure §4.1 names
rather than a form of it: at ten life areas the legend's last rows are off-screen from the wedge they
name, so the reader is asked to hold ten colour-to-word pairs in their head — and under dark neo
there are not ten colours left to hold.

**Three versions, and the second and third exist because of a frame, not a theory.**

1. **Horizontal words, box projected onto the wedge's local axes.** Exact arithmetic. `CategoryTagTest`
   killed it before the emulator did: a six-letter word is ~32 dp and the band is 34 dp, so it fits
   where it lies **along** the band (12 and 6 on the clock) and not **across** it (3 and 9). Labels
   that appear only at the top and bottom of a ring read as a bug.
2. **Rotated to the tangent.** The angle drops out of the problem entirely — the band's thickness
   always faces the word's height, the arc always faces its width — so the fit depends only on how
   big the wedge is. `tangentRotation` flips words on the lower half so none is upside down. Its
   first version tested `tangent in (90, 270)` on a `0..360` value, which leaves the whole
   270-to-360 quadrant reading as a three-quarter turn; the test walks the ring in 5° steps for
   exactly that reason.
3. **Ellipsized against the arc, which is version three and the render pass is why.** Version 2 drew
   only words that fitted **whole**, and the frame showed it labelling **six** of ten equal wedges,
   leaving Nutrition, Relationships, Projects and Learning blank — the four longest names, and under
   dark neo the four wedges nothing else tells apart. **A rule that goes quiet exactly where it is
   needed is not a rule.** No padding arithmetic fixes that, because the variable was never the
   padding: it was that a long name was being treated as an unlabellable wedge. The caller now
   measures with `TextOverflow.Ellipsis` against the room it has, and `wedgeLabelFits` judges the
   **room** rather than the finished string. *Relation…* carries identity; a blank wedge does not.

**What is still unlabelled, and why that is right.** A word cut below about four glyphs stops being a
word, so the floor is `MIN_WORD_HEIGHTS × the type's own height` — in the type's height rather than
in dp, so it follows the device's font scale instead of fighting it. At the analytics donut's
geometry that leaves out wedges under **18.5°**, a 5% share. `CategoryTagTest` pins that number from
both sides so the KDoc sentence cannot rot. Those wedges keep the two paths that were already there
and are **not** the legend: the chart is tappable, and a tapped wedge writes its full name into the
hole. The rule degrades to *the word is one tap away*, never to *the word is somewhere below*.

**Rejected: a callout gutter outside the ring.** It costs the chart about a third of its width on a
phone — shrinking the ring the rule is trying to make readable — and at ten areas the callouts
collide and need a de-collision pass with its own failure modes.

---

## 4. `StackedColumnChart` — the band gains a word, and the residual is stated

`StackedSegment` had **no `label` field**, so under dark neo a stack of ten bands was ten shades of
one ramp with nothing on the chart to tell them apart. It has one now, and it is **required rather
than defaulted**: a `label: String = ""` would let a new call site produce a wordless band and
compile, which is precisely the state this chart was in. The three-argument constructor breaking is
the point of the change, and three call sites in `androidTest` were updated to match.

**The residual, named rather than hidden.** Seven columns across a phone gives each about 40 dp, and
a week with ten life areas gives most bands a few dp of height. In practice this labels the
**dominant** band of a column and little else; a quarter view (thirteen columns) labels almost
nothing. That is a real limit of a stacked column at phone width, not a shortfall in the sweep —
there is no font size at which a name fits in a 6 dp band, and shrinking the type until it did would
trade a legibility rule for a legibility failure. **This is the one site where `.tag` cannot be
satisfied inline at every mark.** What carries identity for the unlabelled bands is not the legend:
the chart shares `selectedId` with the donut, so choosing a series dims every other band and the
shape that stays lit is the answer to *which one is this*; the full per-band breakdown is in the
chart's `contentDescription`.

---

## 5. `rampTint` wired — one seam, and it is the palette layer

`ColorExt.categoryFill(hex, transform, skin, darkSurface, fallback)` is the whole rule as one pure
function; `String.toGoalAccent()` is now a five-line composable that reads `LocalAppMaterial` and
`LocalAppSkin` and calls it. Every categorical fill in the app already passed through
`toGoalAccent`, and `toGoalInk` derives from it, so one edit reaches every chart, chip, dot and icon
tint.

**It branches on the palette *transform*, not on the material.** `MaterialSpec.kt`'s rule is that
nothing outside it may hold a `when (material)` deciding how something is drawn. This is not that —
it is the palette layer, one axis over, reading the transform each material *declares*, the same
value `colorSchemeFor` switches on. A fifth material that declares the ramp inherits the collapse by
existing, which is the property §4.1 asks for a paragraph after it names the rule.

**The ramp is taken from the stored light fill, not from `#57` a's dark twin.** `rampTint` positions
a hue by its **own lightness**, and the twins were authored to a deliberately *even* lightness so ten
slices in one donut hold together — so feeding them in lands the whole set on nearly one point. The
light fill is the category's canonical hue and spreads as far as one accent allows. Asserted, not
commented, because the wrong version still produces a plausible-looking chart.

---

## 6. Two defects found on the way, both pre-existing

### 6a. `toComposeColor` went through `android.graphics`, and it failed **toward a passing test**

`String.toComposeColor` used `androidx.core.graphics.toColorInt`, which calls
`android.graphics.Color.parseColor` — and **that throws on the JVM**. So under a unit test every
category resolved to the fallback and **every colour came out identical**.

That is a silent failure in the worst possible direction. A test asserting two categories are
distinguishable fails; a test asserting they have **collapsed passes for the wrong reason**. This
session hit both, one after the other, writing the guard for the collapse. Nothing in the output says
the instrument is broken — `#53`'s `.tag` test would have been green on a build where `rampTint` was
never called.

The parser is now hand-rolled and pure Kotlin. `Observed:` the rest of `ColorExt.kt` was **already**
pure for exactly this reason — its own closing comment says `ColorUtils` "throws under a JVM unit
test" — and the parser was the one android dependency left in the middle of it. Behaviour is
unchanged for everything this app stores: `parseColor` also accepts a few colour *names*, which now
fall back, and nothing writes one — every colour string here comes from `GoalCategory.defaultColorHex`,
`LifeAreaPalette.hexes` or the swatch picker, all `#RRGGBB`.

### 6b. The instrumented suite reported **6 failures that were not real**

The first full run came back `204 run, 6 failures`, all in `57d`'s `EntranceAnimationUiTest`, all
reading `expected: -65536 but was: 0` — a captured pixel of nothing. The test uses `Color.Red`
directly and touches no chart, no palette and no file this session changed, so the shape was wrong
for a regression.

**Confirmed by looking rather than by reasoning.** The 16 palette frames from the same run totalled
**33 KB** — about 2 KB each — and the dark-neo frame was a white page with the donut drawn as a
one-pixel dashed strip at the top. Minutes later `adb` reported `device 'emulator-5554' not found`:
the AVD had been dying through the whole run. Restarted with `emulator -avd Pixel_10_Pro_XL` (no
`-wipe-data`, so the app's data survived), and the same render pass produced **2.4 MB** of frames.

Worth keeping, because the render passes **passed** on that run: they assert `length > 0`, and a
blank PNG has length > 0. The failing entrance tests were the only honest signal in the room, and the
instinctive reading of them — *my change broke `57d`'s work* — was wrong.

---

## 7. Seen — dark neo, which is the material the rule exists for

`CategoryPaletteRenderPass` renders the **real** `DonutChart` with the **real** ten categories
through the **real** `GoalPilotTheme`, at ten **equal** slices — no size cue, so colour is the only
thing separating two wedges. 16 frames, every material × both brightnesses × both skins.

`aurora-darkneo-dark.png` is the ticket in one image:

- **The collapse is total and visible.** All ten wedges are one blue ramp; all ten legend dots are the
  same blue. This is not a subtle loss of separation — it is one colour.
- **`Observed:` Nutrition and Career are the identical pixel.** Both `#347B47` and `#0861A7` have an
  HSL lightness of **0.343137** — `(123+52)/2` and `(167+8)/2` are both 87.5 — and `rampTint`
  positions by lightness. Measured distance between them under the ramp: **exactly 0.0**, on both
  skins. §4.1's rule holding up its own worked example.
- **The words are on the wedges**, running along the band, none upside down.

---

## 8. 🧪 Tests

| Layer | Result |
|---|---|
| **Client unit (JVM)** | **746 / 0 / 0** across 69 classes (`:app:testDebugUnitTest`) — **+17 tests, +1 class** over `57d`'s 729 |
| **UI E2E (instrumented)** | **204 / 0** — `OK (204 tests)` in 488 s via `adb shell am instrument -w -r`, on a rebooted AVD. See §6b for the run that was not |
| **Render pass** | **2 / 0**, 16 frames written; 4 looked at, 3 committed to `docs/render-passes/2026-08-22-53-tag-sweep/` |
| **Build** | `:app:assembleDebug` + `:app:assembleDebugAndroidTest` green |
| Server unit / integration / endpoints / database | **Layer does not exist** — no server module in this repo |
| Client component | Covered by the instrumented layer above; this project has no separate component layer |

New JVM class `ui/CategoryTagTest` (17 tests). It asserts the rule as the two-part claim it is — under
`identity` categories are told apart by colour, under the ramp they are **not**, therefore the word
has to carry identity — because a test of either half alone passes in exactly the state the other
half exists to prevent.

It reuses `ThemePaletteTest`'s **weighted** RGB metric deliberately. The first draft used CIE L\*a\*b\*,
which put the worst authored pair at **11.2** where the house metric puts it at **99.4** — two files
disagreeing about whether the same palette is separable. Neither number is wrong; only one of them is
the one every other artefact here is written in.

---

## 9. What this does **not** close

**`#53` stays open, and it is not this brief's item that holds it.** The brief says the `.tag` sweep
is *"the only thing standing between `#53` and closed"*. That was true when it was written on
2026-08-21 and stopped being true the same day: a second comment on `#53`
(2026-08-21T21:45:05Z) filed a **naming gap** — §4.1 calls the materials *neo* and *dark neo*, the
picker says **Soft** and **Soft dark**, and the word "neo" appears nowhere in the UI, so a user who
reads the spec cannot find the control and a session receiving a bug report cannot match it to one.

`Observed:` still unfixed at HEAD — `components_material_neo` is `Soft`, `components_material_darkneo`
is `Soft dark`, and §4.1 carries no mapping table. That comment's own recommendation (carry the spec
name in the tile's subtitle or `contentDescription`, and add the table to §4.1) is cheap, **but it
closes with a question only Ido can answer**: whether *"no dark blue neo"* meant the **name** or the
**charcoal ground**. If the latter it is a disagreement with §4.1's material table and belongs in the
spec, not the code.

Out of this session's scope, and named on the ticket rather than silently inherited.
