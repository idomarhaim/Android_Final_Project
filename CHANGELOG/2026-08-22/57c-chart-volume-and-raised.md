# `57c-chart-volume-and-raised` — 2026-08-22

> **Summary:** `#57` c — chart bodies get **volume** (a three-stop fill lit from one direction for the whole chart, a directional bevel, a sheen, a cast shadow and a baked grain) and **raised-3D becomes the fourth appearance axis**, `AppRelief`, available on all four materials as Ido ruled. The brief's own layer table is **wrong on one of its five**: it names `feSpecularLighting` for the bevel, and the *later* prototype (2026-08-11, rebuilt 08-12) deletes that filter in as many words — *"that filter over a fat stroke is what inflated rev 4's ring into a balloon"* — so the bevel is a clipped directional wash on the **prototype's** authority, not on Compose's limits, and it is the one layer whose port would have been a fake. Two rendering defects were found by looking and fixed: pill-ended bars grew a **black spike** at the tip, and the first fix for it put a **dark notch** in every column's top-right corner. JVM **725/0** (+4 tests), instrumented **189/0** (+1 test), 32 chart frames + 8 settings frames. Ships to Ido's phone as **v0.3.1**.

**[#57](https://github.com/idomarhaim/Android_Final_Project/issues/57) c** — chart volume, and
raised-3D as an axis on all four materials. Brief:
[`sessions/done/57c-chart-volume-and-raised.md`](../../sessions/done/57c-chart-volume-and-raised.md).

---

## 🚥 The exit line the brief demanded: *what did Compose not reach, and what was substituted*

The brief asks this to be **stated, not discovered later**. Three answers, and the first is the
interesting one because the limit was **not Compose's**.

### 1 · The specular bevel is gone, and the prototype is why

The brief's table lists five compositing layers and names `feSpecularLighting` for the bevel. It
also warns, correctly, that Compose has no such filter and that faking it is the session's hard
part.

**It did not need faking. The later prototype had already deleted it.**
`docs/prototypes/2026-08-11-visual-styles/index.html` — authored the day after rev 4 and rebuilt
again on 2026-08-12 — carries this verbatim above its own donut:

> *NO `feSpecularLighting` anywhere: that filter over a fat stroke is what inflated rev 4's ring
> into a balloon.*

So rev 4 is **not the authority on this layer**, and a session that ported it faithfully would
have spent its budget reproducing a look the design had already rejected. What the 08-12 rebuild
replaced it with is a **directional bevel wash clipped to the face** — brightest where the face
turns toward the light, gone within a third of the body, with the matching shadow wash from the
opposite side, *"no outline anywhere"*. That is a plain linear gradient plus a clip, which Compose
draws natively and **exactly**.

`Observed:` the two files, both in this repo, both read this session. The finding is documentary,
not experimental — nobody re-rendered rev 4 to confirm the balloon.

### 2 · Soft-light is dropped for neutral noise over `SrcOver`, and that IS a Compose limit

The prototype composites its `feTurbulence` grain with `mix-blend-mode: soft-light`.
`BlendMode.Softlight` has **no `PorterDuff` equivalent**, and Compose falls back to
`PorterDuffXfermode` on older canvases. This app is `minSdk 26`, so on a real device the mode
would be **silently dropped** — the exact failure class as `BlurMaskFilter` in
[`kb/dev/compose-soft-shadows-below-api-28.md`](file:///C:/Dev/JARVIS/kb/dev/compose-soft-shadows-below-api-28.md):
no exception, just the wrong picture.

`Untested:` I did not measure the fallback on an API-26 device; the reasoning is from the API
surface plus the recorded `BlurMaskFilter` precedent, and a device check would settle it.
**Substituted:** the tile carries **both light and dark speckles around a neutral mid** and is
drawn plainly at low alpha, which is what soft-light over neutral noise approximates anyway — and
which renders identically on every device the app ships to.

### 3 · Blur is stacked, not blurred — the same substitution the panels already use

The cast shadow is summed from three expanding strokes rather than blurred, for the reason
`MaterialSpec.drawShadowPair` already documents. Nothing new; recorded so the list is complete.

**Nothing else was substituted.** Every remaining primitive — the annular sector, the side walls,
the end caps, the three-stop fill in user space, the sheen ribbon, the channel clip — is a Compose
primitive drawn directly, because the 08-12 rebuild is built from geometry and gradients rather
than from filters. That is the finding worth carrying: **the newer prototype ports to Compose
almost for free, and rev 4 does not.**

---

## 🧱 What shipped

### `AppRelief` — the fourth appearance axis

`domain/model/AppRelief.kt`, beside `AppSkin`, `AppMaterial` and `AppBackground`: `FLAT` ·
`RAISED`, persisted device-local, tolerant `fromId`, default `FLAT`.

**The default is not conservatism.** A raised body is **narrower** than a flat one at the same
channel — the extrusion comes out of the width budget, which is the 08-12 rebuild's central
correction — so shipping `RAISED` by default would change the shape of every donut on every
install at once. The user opts in.

⛔ **The recorded decision this overturns is kept verbatim in `AppRelief`'s KDoc**, because it is
a good argument and somebody will re-derive it. `TODO/TODO_OPTIONAL/Presentation.TODO.optional.md`
held that raised *"is a no-op on glass and liquid, where height would contradict what the material
is, so it is not a separate user setting"*. Ido overruled it on 2026-08-21. **It is now an
assertion**, not a comment: `raised is not a no-op on any material` fails, naming the material, on
exactly the `when (material)` that would restore it. Mutation-checked — see 🧪 below.

### `GpChartVolume` — the fifth answer in the material contract

`ui/theme/ChartVolume.kt` (new) holds the token and the drawing; `ui/theme/MaterialSpec.kt` holds
one function, `chartVolumeFor`, that answers it per material. The token sits **inside**
`GpMaterialSpec`, for the reason `#57` b put `backdrop` there: a chart that had to know which
material it was in would re-open the draw-it-four-times cost — now draw-it-sixteen-times, because
relief multiplies it again. **No chart component names a material.**

The four rows, as a table, because what matters is the *relationship*:

| | tint | shade | bevel | fold | sheen | cast α | grain |
|---|---|---|---|---|---|---|---|
| glass | .34 | .26 | .20 | .16 | .42 | .16 | — |
| liquid | .38 | .24 | .26 | .14 | .55 | .24 | .05 |
| neo | .30 | .30 | .13 | .20 | .18 | .22 | .10 |
| dark neo | .46 | .34 | .30 | .32 | .30 | .55 | .14 |

Each row is that material's own §4.1 sentence: glass is *depth from blur*, so it gets sheen and
**no grain** (grain on a translucent panel reads as dirt on the glass); liquid is *depth from
refraction at the edge*, so its sheen is the strongest and its cast is **coloured** rather than
black, matching the refraction its panels already drop; neo has no gloss at all, so its work is
done by shade and fold; dark neo is the largest number in every column, which is what that
material is.

The **ink** every shade mixes toward comes off the scheme (`background.atLightness(0.05f)`), not
from the prototype's `#0A101A` — `#57` a's finding, and it matters more here than there because
this one is mixed into **every category hue at once**.

### The four charts

- **`DonutChart`** — wedges collected and drawn in **one** call, because two layers are properties
  of the ring rather than of a wedge: walls run before faces across the whole set (or a
  neighbour's wall lands on a face — the *"blocks climbing over each other"* defect the 08-12
  rebuild exists to fix), and the grain is one pass (or its tile seams show).
- **`ProgressRing`** — same, for one arc. ⚠️ **A `brush` caller keeps the flat stroke**: the brand
  gradient is authored to mean something (dark neo's accent *is* that gradient) and the volume
  pass would repaint it as a shade of one colour. One call site: `GoalDetailScreen`'s hero ring.
- **`StackedColumnChart`** — a column is now one `Canvas` rather than a `Column` of coloured
  `Box`es. That is not a refactor for its own sake: the old shape **could not express** *"this
  segment has another sitting on it"*, and a lit top face through the middle of a stack is what
  you get when it cannot. Only the highest segment is capped; only the lowest casts.
- **`HorizontalBarChart`** — via a new **opt-in** `volume` parameter on `GpLinearProgress`. Opt-in
  because that pill is **not only a chart**: it is the goal card's progress, the milestone row's,
  the day-plan meter's. `#57` is about *graphs*, and a setting that silently extruded every
  progress bar in the app would be this axis reaching past what Ido asked it to move. One call
  site passes it; the other eight do not.

### Settings — a fifth control in the Appearance card

`ReliefPicker`, between Background and Brightness, two tiles each drawing a **real donut through
the real `drawVolumeArcs` in the real current material**, plus §4.9's consequence line. A body is
judged by looking at it; a `Switch` labelled *"3D charts"* asks the user to imagine the answer.

⚠️ **The Appearance card now carries five controls and is the tallest card in the app.**
`BackgroundPicker` already called being the fourth *"a real cost"*. This is not a defect and it is
not in this brief's scope to redesign §4.9 — but it is a product judgement Ido should make
knowingly, so it is named here and in the reply rather than left to be noticed.

---

## 🔎 Two defects found by LOOKING, and the second was caused by fixing the first

Both are the reason the brief says **Seen** on a device, and neither is reachable by any assertion
in this repo.

1. **Pill-ended bars grew a black spike at the tip.** The side wall swept the rect's *bounding*
   right edge into a parallelogram — correct for a square-cornered column, and wrong for a pill,
   whose right edge is entirely round, so the quad lay **outside** the shape. Every row of
   `HorizontalBarChart` had it, on all four materials. It reads as a rendering artefact, which is
   what it was.
2. **The first fix put a dark notch in every column's top-right corner.** Replacing the quad with
   the body's own silhouette in the **wall** tone left the rounded-corner region — the one place
   no quad covers — as a hard dark wedge between a lit top and a lit face. That reads as a *hole*,
   not as a fillet.

**Both go away by drawing what is actually there.** An extruded body's top surface is its whole
outline swept, corners included, and it is the surface **facing** the light — so it is drawn
**lit**, at the body's own radii, with the right wall as a separate dark connector over the span
that is genuinely a straight edge. A pill's connector is zero-height and its silhouette alone
gives it depth, which is the right answer rather than a patched one.

`Observed:` three consecutive render passes, 2026-08-22. The byte-count check from `57b`'s trap
was run each time: the flat frames came back **byte-identical** across the second and third pass,
which is correct — `drawBarWalls` is only reached when the relief is raised — and the raised
frames changed. That is the check corroborating rather than warning.

**A third thing was found by looking and it was not a defect:** at 78 dp the two relief tiles read
as the same picture, because the extrusion is a third of a 15 dp channel. The tile is 92 dp now
and the ring fills it; the difference is unmistakable in `aurora-darkneo-dark.png`.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **725 / 0 / 0** across 67 classes (`:app:testDebugUnitTest`), +4 tests |
| **Instrumented** | **189 / 0** (`am instrument`, render passes excluded), +1 test |
| **Render pass** | 32 chart frames (`ChartVolumeRenderPass`, new) + 8 settings frames (`MaterialRenderPass`) |
| Firestore rules | not touched — no rule, index or security change in this unit |
| Cloud Functions | not touched |

**The four new JVM tests, and why each exists rather than being a count:**

- `raised is not a no-op on any material` — the overturned decision, mechanised.
- `relief moves nothing but the volume` — the orthogonality claim in one line, written as an
  equality after transplanting `volume` rather than as a list of field comparisons, so a field
  added to `GpMaterialSpec` later is covered on the day it is added.
- `a chart body's stops shade one hue and never invert` — a body's three stops are a shading of one
  hue, not three colours. Catches an `ink` that stops being darker than the hues it is mixed into,
  which would turn the bottom of every wedge into a highlight.
- `a raised body's walls stay separable from its face` — a wall is judged against the face it folds
  away from, not against the page. The `min(0.92f, shade * 1.7f)` cap is exactly where this could
  silently fail.

**⚠️ The matrix did NOT multiply, and that is the same finding `#57` b recorded one axis down.**
The brief expected *material × scheme × raised*. `AppRelief` reaches exactly one field, so no
scheme role and no ground is a function of it — which is **asserted**, not assumed, by *relief
moves nothing but the volume*. So `#57` c adds a **third section** of body properties rather than
doubling the two above it; doubling `#57` b's page-contrast test (a 41×27 grid over 64 cells) by a
dimension provably absent from it is the kind of cost that gets a guard weakened later for being
slow.

**Mutation-checked, per `kb/dev/look-at-your-own-output.md` §*check the instrument*.** The
load-bearing guard was run against the exact edit it exists to stop — a
`when (material)` zeroing the height for the two glass materials, which is what tidying the
overturned decision back in would look like. It failed with
`AURORA/GLASS — RAISED must have height: expected to be greater than: 0.0 but was: 0.0`, and the
mutation was reverted. A guard that has never been seen to fail is a guard nobody has checked.

**One pre-existing assertion was weakened, deliberately and narrowly.**
`MaterialPickerUiTest.choosingTheLockedMaterial_captionsTheBrightnessControl` asserted
`assertIsDisplayed()` on the brightness-lock caption without scrolling to it; the fifth control
pushed it below the fold. A `performScrollTo()` was added. The claim under test is *"choosing the
locked material captions the brightness control"* — not *"the caption fits on one screen"*, which
was never asserted on purpose.

---

## 📱 Device

**NO SIGN-IN WAS NEEDED AND NONE WAS DESTROYED.** Every run used `adb install -r` +
`am instrument`, never `connectedDebugAndroidTest`, so the app was never uninstalled
(`kb/dev/android-device-verification.md` §8). Animation scales verified at `1.0` **before** the
first device command and untouched. AVD `Pixel_10_Pro_XL` / `emulator-5554`, adb and the Gradle
daemon claimed on the board before the first device command and released with this session.

Both APKs were reinstalled before **every** render pass — `57b` lost three passes to
byte-identical output because only the *androidTest* APK was being replaced while the change under
test lived in the other, and that warning is now in `ChartVolumeRenderPass`'s own header.

---

## 🚚 Carried up under this session's push

`git push` is **branch-scoped, not commit-scoped**, so three commits authored by
`57b-backgrounds-and-combinations` ride up under this session's push:

- `1242157` — *root cause: firebase is a FILE LOCK, not an auth problem*
- `a22bc00` — *record the firebase root cause and the held push*
- `d06e34e` — *hand the three held commits to 57c explicitly*

**This is settled by a positive signal, not by silence.** `57b` released its board row and then
wrote `d06e34e` explicitly to hand these over, quoting Ido — *"let 57c do the push"*. Precondition
5's ambiguous case is an **absent** row plus silence; an explicit release note written by that
session about itself is the thing that settles it.

**Verified rather than trusted:** `git diff --stat 37cb6bc..d06e34e` is
`CHANGELOG/2026-08-22/57b-*.md`, `CLAUDE.md`, `SESSIONS.md` — **documentation only, no app code,
no test, no build file**, which is what `d06e34e` claims. Named here as well as in the reply,
because a reply scrolls away and this file does not.

---

## 📦 Shipped to the phone — `v0.3.1`

`versionCode 5 → 6`, `versionName 0.3.0 → 0.3.1`, tag `v0.3.1`, which is what
[`docs/RELEASING.md`](../../docs/RELEASING.md) §3 makes the **only** route that produces an
installable update: the release keystore is restored from `RELEASE_KEYSTORE_BASE64` by
`.github/workflows/release.yml`, and a locally-built release APK signed with any other key is
refused by Android as an update.

Done at Ido's explicit request in the session prompt — *"when you finish the session, send the
updated version to my smartphone"* — which is what authorises the remote tag, an always-ask action
in both modes.
