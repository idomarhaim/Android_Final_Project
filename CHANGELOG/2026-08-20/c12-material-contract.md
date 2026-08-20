# `c12-material-contract` — 2026-08-20

> **Summary:** Spec §4.1's **four-material contract** ships — `AppMaterial` (glass · liquid glass · neo · dark neo, metal deleted), the three **palette transforms** that produce all sixteen material cells from the four hand-authored schemes, and `GpMaterialSpec`, the `surface · groove · elevation · accent` vocabulary every screen is allowed to ask for. §4.9's Appearance section gains its four **material tiles**, which removes the reason `48-settings-surface` deliberately left them out — *"a picker over materials nothing renders is a control that changes nothing"*. Both of §4.1's "found late" consequences are built and asserted rather than discovered: dark neo's accent is **derived from the selected skin** (so Blossom is not silently Aurora for a quarter of the set), and the product is **ragged not rectangular** — dark neo declares itself brightness-locked, the tile says `Dark only`, and the brightness segments are struck through, captioned and inert while the stored value is left alone. Widening the contrast suite from 4 schemes to the 14 distinct cells **caught a real WCAG failure on its first run** (dark neo's ramp deep end at 3.54:1 against its own ink). **The category-hue collapse is declared and deliberately not wired** — applying it before §4.1's `.tag` words exist installs the identity failure `.tag` exists to prevent, and that sweep is `C12` §4.4's with the charts.

**Session:** `c12-material-contract` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Brief:** [`sessions/c12-material-contract.md`](../../sessions/c12-material-contract.md) · **Issue:** [#53](https://github.com/idomarhaim/Android_Final_Project/issues/53)

---

## What shipped, against the brief

| # | Piece | State |
|---|---|---|
| 1 | Four materials, metal deleted | ✅ `domain/model/AppMaterial.kt` |
| 2 | Each declares a **palette transform** | ✅ `domain/model/PaletteTransform.kt`, applied in `ui/theme/MaterialPalettes.kt` |
| 3 | Schemes **generated, not hand-authored** | ✅ all 16 cells come from the existing **4** authored schemes — 8 by `identity`, 8 computed (6 distinct, the lock collapsing dark neo's 4 into 2) |
| 4 | Dark neo's accent **derives from the skin** | ✅ `rampFor(skin)`, from the skin's own hero sweep |
| 5 | **Brightness-locked** declaration + the picker says so | ✅ `lockedDark`, `resolveDark`, `Dark only` badge, struck-through captioned segments |
| 6 | `surface · groove · elevation · accent` contract | ✅ `ui/theme/MaterialSpec.kt` — `GpMaterialSpec`, `gpSurface`, `gpGroove`, `gpPage` |
| 7 | `--edge` on every panel | ✅ non-nullable `edge`, stroked last and unconditionally by `gpSurface` |
| 8 | An overlay declares its own opacity | ✅ `overlay`, always opaque and never the same token as `surface` |
| 9 | The four tiles on §4.9's Appearance section | ✅ `ui/components/MaterialPicker.kt` + `AppearanceCard` |
| 10 | Persisted per-device | ✅ `AppPreferencesRepository.material`, `app_material` key |
| — | `.tag` categorical collapse | ⛔ **declared, not wired** — see *What is deliberately not built* |

---

## The three decisions worth reading

### 1. The contract is a type, so "draw it four times" fails to compile rather than fails review

§4.1's cost sentence is the whole design constraint:

> **No screen may depend on a property a single material has.** … A screen specifies
> `surface · groove · elevation · accent`, and each material answers those four its own way —
> otherwise every design has to be drawn four times, which is the cost that would make four
> materials unaffordable.

`GpMaterialSpec` is that vocabulary as a data class behind `LocalGpMaterial`. The enforcement is
negative and stated in its KDoc: **a `when (material)` that decides how something is *drawn*,
outside `MaterialSpec.kt`, is the smell** — and there is exactly one legal exception, the picker,
which §4.9 documents as *"the one control that must not obey the material"*. Branching on the
material for its **words** is not that smell and `ComponentStrings.kt` does it, the same split
`AppSkin` already takes.

The seam that makes it reach the app is `GpCard`, which stopped drawing a Material 3 `Card`. An
M3 `Card` answers all four of the contract's questions itself — one opaque tonal fill, one tonal
elevation — which is a **fifth** answer competing with the four, and under neo it is wrong
outright: neo's panel is the *same* colour as the page, extruded by a shadow pair, and a tonal
card fill draws the boundary the shadow was supposed to draw. There are 30-odd call sites and
**none of them changed**.

### 2. Generated schemes need a contrast suite over the *matrix*, and it paid immediately

`ThemePaletteTest` iterated the four hand-authored schemes. The moment twelve more are produced
by a transform, that suite is testing the wrong set — it stays green, and it keeps *looking*
thorough because the assertion count never dropped. It now iterates
`AppSkin × AppMaterial × brightness` (14 distinct cells, not 16 — `resolveDark` collapses dark
neo's pair, which is §4.1's *"ragged, not rectangular"* arriving in a test).

**First run, first failure:** dark neo's ramp **deep end at 3.54:1** against its own ink. The
generalisation is in `RAMP_DEEP`'s KDoc and is worth restating, because the intuitive edit is the
one that breaks it: a ramp used as a *filled* surface must let **one** ink clear **every** stop;
the ink is dark because a bright accent cannot carry white (~2.4:1); so it is the **deep** end
that binds, and deepening the gradient for drama breaks the end nobody looks at.

### 3. The lock is a word, said twice, and it does not overwrite the setting

§4.9 rules that **a lock is a word, never a dimming**, and §4.1 requires the picker to *say* dark
neo is brightness-locked. It is stated in two places that cannot drift, because both read
`AppMaterial.isBrightnessLocked`: `Dark only` on the tile, and a struck-through, captioned
brightness control. The segments are **also** disabled — the rule forbids a dimming *instead of* a
word, not alongside one, and a control that is legible, captioned and still inert is worse than
one you cannot press.

The part a user could be misled by is the third sentence, so it is asserted on a device: **the
stored brightness is not overwritten.** The lock is a suspension — pick another material and the
old choice applies again. If that ever stops being true, `MaterialPickerUiTest` fails on the word
`remembered` in the caption, and nothing else in the suite would notice.

---

## What is deliberately not built

**§4.1's `.tag` categorical collapse is declared (`rampTint`) and not applied at the call sites.**
The rule reads: *a category is written in words beside its dot, **because** dark neo collapses the
six categorical hues into one ramp and colour stops carrying identity.* The dependency runs one
way — the words are what make the collapse survivable — so shipping the collapse first would
install exactly the identity failure the rule exists to prevent, in every chart at once. The
arithmetic is unit-tested so the sweep inherits a defined answer instead of inventing one; the
sweep itself belongs with `C12` §4.4's charts.

**Glass and liquid glass are translucent panels over a gradient backdrop, not blurred ones.**
Compose has no backdrop filter and `Modifier.blur` blurs a composable's *own* content, so the blur
§4.1 prices is not available at all below `RenderEffect` (API 31+, `minSdk 26` here). The
*reading* survives — the page shows through the panel, which is why `gpPage` paints the skin's
hues behind them — and the blur does not. Stated in `MaterialSpec.kt`'s header rather than left
for someone to discover from a screenshot.

---

## Two defects found while building

- **`androidx.compose.material3.ColorScheme` declares no `equals`.** Two schemes with identical
  tokens compare unequal, and Truth reports *"(non-equal instance of same class with same string
  representation)"* — a red test whose printed diff shows two identical objects, which invites you
  to go and "fix" working code. `MaterialPaletteTest` compares a token list instead.
- **`am instrument` needs the app's own runner**, `com.idomarhaim.goalpilot.HiltTestRunner`, not
  `androidx.test.runner.AndroidJUnitRunner`. The generic one fails with *"Unable to find
  instrumentation info"*, which reads as *the test APK is not installed* — and the instinctive
  recovery, running `connectedDebugAndroidTest` "properly", is the thing that **uninstalls the app
  and takes the device's Google account with it**.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| JVM unit (`:app:testDebugUnitTest`) | **560 / 0** (+20) |
| Instrumented (`am instrument`, render pass excluded) | **157 / 0** (+7) |
| Instrumented render pass (`MaterialRenderPass`) | **2 / 0** — 16 frames written |
| `:app:assembleDebug` | ✅ green |
| Cloud Functions (`functions/`) | **n/a** — nothing there changed |
| Security rules (`firestore-tests/`) | **n/a** — nothing there changed |

New JVM suites: `AppMaterialTest` (11) and `MaterialPaletteTest` (9); `ThemePaletteTest` widened
from 4 schemes to 14. New instrumented suite: `MaterialPickerUiTest`.

---

## 👁️ The render pass

§4.9's Appearance section, drawn by the real `GoalPilotTheme`, in **4 materials × 2 brightnesses ×
2 skins**. Frames in [`docs/render-passes/2026-08-20-c12-material-contract/`](../../docs/render-passes/2026-08-20-c12-material-contract/).

**What each frame is evidence of is in that folder's `README.md`**, including the two honest
limits: glass and liquid glass are close at *tile* size (they separate clearly at full-screen
size), and there is no backdrop blur because Compose has none.

⚠️ **The dark-neo brightness pair is not byte-identical, and I checked rather than assumed.**
`aurora-darkneo-light.png` and `aurora-darkneo-dark.png` differ by ~3 KB. The **theme** in them is
identical — same charcoal, same ramp — and what differs is which brightness segment carries the
selection tick, because the stored setting really is still Light in one. That is the lock behaving
as designed: it *suspends* the setting rather than overwriting it, which is exactly the sentence
the consequence line makes to the user.

### One defect the run found, before the frames

`MaterialPickerUiTest.theLockIsAWordOnTheTile_andOnlyOnTheLockedOne` failed on the first
instrumented run with *"The component is not displayed!"* while the badge was rendering perfectly.
A tile is `Modifier.selectable`, which **merges its descendants' semantics**, so the badge's own
`testTag` is not a node in the merged tree. The assertion now reads the **tile's** merged text —
which is the better claim anyway: it says the word is on the *dark neo* tile, not merely somewhere
on the screen — and reaches the tag through the unmerged tree.

### And one that cost the AVD

The first render-pass attempt **hung for twenty minutes at 0% CPU** and took `adb shell` and
`logcat` down with it; the AVD had to be killed and cold-booted. `captureToImage` goes through
`PixelCopy`, which waits for the window to produce a frame, and a **screen-off** emulator produces
none. Waking and dismissing the keyguard first turned the same 16-frame pass into **8 seconds**.
The recovery cost the app's install (the device's Google account, `name.iddo@gmail.com`,
survived); `MaterialRenderPass`'s KDoc now carries the wake step as part of the recipe.
