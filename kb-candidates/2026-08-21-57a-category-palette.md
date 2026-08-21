# KB candidates — `57a-category-palette`, 2026-08-21

Session working `#57` a (the harmonised category palette). Each entry stands
alone: no transcript is a source.

---

## 1. A "harmonised" categorical palette and a minimum-colour-distance guard pull in opposite directions, and the guard usually wins

**Claim.** A categorical palette asked to look *coherent* (one lightness, one
chroma, evenly spaced hues) and a test asserting *pairwise distinguishability*
cannot both be satisfied beyond about six or seven colours. Separation in any
RGB-distance metric is bought mostly with **lightness difference**, which is
exactly the variable harmony asks you to hold constant. Measured on GoalPilot's
ten categories with the committed weighted metric
(`sqrt(2·dr² + 4·dg² + 3·db²)`, threshold > 90): nine evenly-spaced hues at one
OKLCH lightness and one chroma reach a minimum of **63–80** across every
(L, C) pair searched — the guard is unreachable. Allowing a lightness *band*
recovers it: 0.041 → 93.2, 0.057 → 99.4, 0.073 → 104.8, 0.088 → 112.0.

**Why.** Two colours differing only in lightness by Δ (as a fraction of 255)
score `3·255·Δ` in that metric, so ~30/255 in each channel clears 90 on its own;
two colours differing only in *hue* at moderate chroma rarely do. So the metric
is, in effect, mostly a lightness-difference detector, and "harmonised" means
"low lightness variance". The practical resolution is to **name the band you are
willing to spend** and maximise separation inside it, rather than treating
"one lightness" as achievable.

**Why it matters beyond this repo.** The intuition — *make it harmonious and it
will still be distinguishable, they are unrelated properties* — is false and
fails silently: the palette looks better in a swatch strip and the chart gets
harder to read. The trade is quantifiable and should be quantified before the
values are chosen, not after a guard goes red.

- **Rejected:** lowering the threshold to fit the palette. It was put there by a
  real defect (three greens indistinguishable at bar width) and lowering it is
  weakening a guard to pass, which the numbers above make unnecessary anyway.
- **Destination:** `kb/dev/` — new page, e.g. `categorical-palette-tradeoffs.md`.
- **Anchors:** GoalPilot `app/src/test/.../ThemePaletteTest.kt`
  (`category fills are distinguishable from each other`);
  `CHANGELOG/2026-08-21/57a-category-palette.md`.
- **Supersedes:** nothing.
- **Status:** ready.

## 2. An algorithmic dark-mode lift gets WORSE the more harmonised its light input is

**Claim.** Deriving dark-mode colours by forcing every light hex to a fixed HSL
lightness degrades *more* as the light set becomes more uniform, so improving the
light palette can silently regress dark mode. Measured on the same ten
categories, minimum pairwise separation after a lift to HSL L = 0.72:

| light input | min pairwise | OKLCH lightness spread after lift |
|---|---|---|
| old crayon set | 57.6 | 0.158 |
| new harmonised set | **37.2** | 0.228 |
| authored dark twins | **66.2** | **0.084** |

**Why.** The lift discards lightness and keeps only hue and saturation, so after
it the *only* remaining separators are those two. A harmonised light set has
deliberately low chroma variance, so the lift's output collapses — GoalPilot's
`SLEEP` (`#516AA6`, low saturation) lands 37 away from the neutral `OTHER`.

**Why it matters.** It is a **negative interaction between two improvements**,
each individually correct, and it is invisible from either side: the light
palette's own tests pass, and dark mode has no test at all in most projects. The
general lesson is that a derived variant's quality is a function of the *variance*
of its input, so any "we compute the dark theme from the light one" shortcut
should be measured against the specific light palette, never adopted once.

- **Rejected:** widening the lift (per-hue lightness targets). It re-introduces
  the hand-tuning that authoring a table does explicitly and testably.
- **Destination:** `kb/dev/` — same page as entry 1, or its own.
- **Anchors:** GoalPilot `ui/components/ColorExt.kt` (`toGoalAccent`),
  `domain/model/Goal.kt` (`darkColorHex`, `darkTwinOf`).
- **Supersedes:** nothing.
- **Status:** ready.

## 3. Kotlin block comments NEST, so `/*` inside prose in a KDoc breaks the build

**Claim.** Unlike C, C++, Java or JavaScript, Kotlin block comments nest. A `/*`
appearing inside a KDoc body opens a second comment, so the block's `*/` closes
only the inner one and the file runs on to EOF. Writing a path or a glob in
prose — `AURORA/DARK_NEO/*light*`, `src/*/main`, `and/or` next to a star — is
enough. `Observed:` 2026-08-21, GoalPilot, from
`* so the AURORA/DARK_NEO/*light* case carries…` inside a KDoc; the compiler
reported `Missing '}'` at a line 45 above and `Unclosed comment` at the end of the
file, naming neither the comment nor the character that caused it.

**Why it matters.** It is the same family as the XML trap this project already
records (`--` inside an XML comment kills `parseDebugLocalResources`): **a file
format treating your prose as syntax**, in a house style that puts long
explanatory comments in every file. Both fail with an error that points somewhere
else, and both are invisible on re-reading, because the comment reads perfectly
either way. The remedy is the same and it is mechanical, not attentional:
re-check comment bodies with a regex
(`re.findall(r'/\*.*?\*/', s, re.S)`, then look for `/*` inside each body)
rather than by eye.

- **Rejected:** "just be careful with slashes" — see
  `kb/dev/look-at-your-own-output.md` on why *be careful* is not a remedy.
- **Destination:** `kb/dev/` — extend the existing page that holds the XML `--`
  trap if there is one, else a `prose-is-syntax` page; cross-link
  `look-at-your-own-output.md`.
- **Anchors:** GoalPilot `CLAUDE.md` (the XML `--` bullet, 2026-08-20).
- **Supersedes:** nothing; generalises the XML bullet.
- **Status:** ready.

## 4. A theme test keyed off the *requested* brightness tests a cell the app never renders

**Claim.** Where a design system lets a theme override the requested brightness
(GoalPilot's `AppMaterial.resolveDark` forces dark neo dark in both), a test that
selects its expected values from the **requested** brightness will pair light
values with a dark scheme and pass. The assertion must read the **rendered**
surface — the same input the production code reads
(`MaterialTheme.colorScheme.surface.luminance() < 0.5f`). `Observed:` 2026-08-21,
GoalPilot: a first draft of `ThemePaletteTest`'s fill guard chose hexes by
`Case.dark` and would have validated the light palette against an all-charcoal
tone ladder in the `AURORA / DARK_NEO / light` cell.

**Why it matters.** It passes, which is the whole problem — the wrong cell is
*easier* to satisfy than the right one, so the failure is silent and biased
toward green. The general form: **when production derives a value, the test must
derive it the same way, not re-specify it**; any place a test restates a rule the
code also implements is a place the two can disagree.

- **Rejected:** asserting `resolveDark`'s behaviour separately and keeping the
  requested-brightness selection. It leaves the two definitions in two files.
- **Destination:** `kb/dev/` — a testing page; cross-link
  `look-at-your-own-output.md`.
- **Anchors:** GoalPilot `ThemePaletteTest.fillFor`,
  `domain/model/AppMaterial.resolveDark`.
- **Supersedes:** nothing.
- **Status:** ready.

## 5. Git Bash rewrites `/sdcard/...` in an `adb pull`, and the error names a Windows path

**Claim.** In Git Bash on Windows, MSYS path conversion rewrites a leading `/`
argument into a Windows path before `adb` sees it, so
`adb pull /sdcard/Android/data/<pkg>/files/x` fails with
`failed to stat remote object 'C:/Program Files/Git/sdcard/Android/…'`. Prefix
the command with `MSYS_NO_PATHCONV=1`. `Observed:` 2026-08-21, GoalPilot, pulling
a render pass.

**Why it matters.** The error message names a path nobody typed, which reads as
*the device does not have the file* — the natural next move is to re-run the
instrumentation that just succeeded. Small, but it costs a full render cycle.

- **Destination:** `kb/dev/android-device-verification.md` — check first whether
  it is already recorded there; if so, this entry is dropped, not duplicated.
- **Anchors:** none.
- **Supersedes:** nothing.
- **Status:** ready — verify against the existing page before writing.
