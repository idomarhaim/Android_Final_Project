# `57b-backgrounds-and-combinations` — 2026-08-22

> **Summary:** `#57` b — the background becomes a **third axis** (`MATCH · GLOW · SPECTRUM · PLAIN`) the user combines freely with the four materials, and the A-vs-B fork the brief said to put to Ido **collapsed** under the derivation-closure check: both write one quantity, so B's two states are two *values* of A. Two **pre-existing** defects were found by building it and are fixed here — every screen's `Scaffold` was painting an opaque fill over `gpPage`, so the grounds were drawn and then hidden **app-wide since `gpPage` was written**, which is the literal explanation of *"the same backgrounds aren't there"*; and glass in dark mode measured **2.55–2.78:1** for body text on its own native ground, because its `tintFloor` was a bloom rather than a floor. Neo **does not** survive a lit ground and now says so in words and pixels; glass does not survive a plain one either, which the brief did not anticipate. JVM **717/0** (+5 tests), instrumented **194/0**, 80 render frames.

**[#57](https://github.com/idomarhaim/Android_Final_Project/issues/57) b** — backgrounds, and
letting the user combine them with the blocks. Brief:
[`sessions/done/57b-backgrounds-and-combinations.md`](../../sessions/done/57b-backgrounds-and-combinations.md).

---

## 🚥 The hand-off line the brief demanded

**Which of A or B, and why — and does neo survive?**

**A, and the fork was false.** The brief offered *A · a background × material grid* against
*B · the prototype's two-state toggle*, and told this session to put both to Ido. Running the
question-axis fork check first collapsed it: **both designs write exactly one quantity** —
`GpMaterialSpec.backdrop`, read by `Modifier.gpPage` at its two call sites — so B's two states
are two **values** of A's enum, not a rival design. `MATCH` *is* *native canvas*; `GLOW` *is*
*shared canvas*, because the shared canvas is the glass ground and `GLOW` is the glass ground.
The real question was never *which design* but **how wide the enum is**, and that is derivable
from Ido's own words: *"no option to choose different combinations between the backgrounds and
the blocks"* — two states are a toggle, not combinations — plus his raised-3D ruling that
presentation is *"an option that can be implemented in addition on each of the design types"*.

**Decision taken by this session, per `derivable-decision.md`, and Ido's to overturn in one
line** (`AppBackground` would lose two entries). The picker was not popped because the fork it
would have offered has a false premise, and asking a question whose options are not independent
is the failure that rule exists to prevent.

**The brief's stated cost for A was wrong, and that is worth knowing before overturning
anything.** It said *"the contrast matrix multiplies again"*. It does not:
`colorSchemeFor(skin, material, dark)` takes no `AppBackground`, so the fourteen schemes stay
fourteen. What the axis actually costs is a **new pair of properties** — what a panel looks like
once a foreign ground shows through it, and what the page does to inherited `onBackground` —
and both are now asserted.

**Does neo survive? No, and it is not supposed to — the app now says so in words and in
pixels.** Neumorphism *is* the panel being the same colour as the page; a lit ground has no
single such colour. On `GLOW` or `SPECTRUM` the soft materials become the prototype's own honest
answer, a **translucent plate carrying the shadow pair**, and the card gains an edge it would
not otherwise have. **Seen** on `aurora-neo-glow-dark.png` and `aurora-neo-glow-light.png`. The
converse is shipped too and was **not** in the brief: **glass on `PLAIN` stops being glass**,
because a translucent panel over a flat ground has nothing to be transparent about. Both are
stated in `AppBackground`'s KDoc and both fire a live consequence line in Settings.

---

## 🐛 Two defects found, both PRE-EXISTING, both fixed

Neither was in the brief. Both were found by building the thing the brief asked for.

### 1 · The ground was drawn and then painted over — on every screen, since `gpPage` was written

**This is the literal explanation of Ido's complaint that *"the same backgrounds aren't
there"*.** They were there. `MainActivity` draws them. Then every screen puts a `Scaffold` on
top, and `Scaffold`'s `containerColor` defaults to `colorScheme.background` — an **opaque** fill
over the whole window. So glass and liquid glass have been rendering translucent panels against
a flat colour, which is exactly the look `MaterialSpec.kt` says they are defined against: *"a
translucent panel over a flat ground is not translucent, it is grey."*

**Why no test and no render pass caught it — two instruments wrong in the same direction.**
`MaterialRenderPass` put `SettingsContent` straight under `GoalPilotTheme` and never applied
`gpPage` either, so its frames **agreed with the app**. A frame missing a background still looks
like a perfectly good screenshot of a settings screen; nothing in it says a layer is absent. It
only became visible once a control existed whose entire subject was that layer — which is
`kb/dev/look-at-your-own-output.md`'s *check the instrument on the hardest input it exists for*,
arriving the long way round.

Fixed in all twelve `Scaffold(` call sites plus the ten `TopAppBar`s that had the same problem
one bar-height up. **`DashboardScreen` already set its top bar transparent** — the intent was
always this; it just never became a rule.

- `Observed:` on Settings, the same frame with and without the one line.
- `Inferred:` for the other ten screens, from the same mechanism — none of the twelve call sites
  passed a `containerColor` before this commit, checked mechanically. Not separately rendered:
  the render pass photographs one screen.

### 2 · Glass in dark mode failed WCAG on its own native ground

`GpGloss.tintFloor` exists precisely to stop *"contrast being a property of the wallpaper rather
than of the component"*. Glass's dark value was `primary` at **0.06** alpha — a bloom, not a
floor — so it did not do that job.

`Observed:` body text at **2.55–2.78:1** against `onSurface` at page point `(0.91, 0.08)`, under
the second glow, **inside the card area**, on both skins. Shipped, and reachable before this
session existed by choosing Glass and dark. Liquid glass measured **4.12–4.28**, also short of
4.5, and the *only* thing differing between them is this layer — which is itself the proof of
the mechanism.

Two fixes, both dark-branch only, both leaving every light scheme untouched:

| | before | after | why |
|---|---|---|---|
| glass `tintFloor` | `primary` @ 0.06 | `primary.atLightness(0.05f)` @ 0.34 | bloom's **hue**, floor's **lightness** — the accent still touches the panel |
| liquid `tintFloor` | `#080A14` @ 0.26 | `primary.atLightness(0.04f)` @ 0.34 | same, and the last hardcoded hex in the file now tracks the skin (`#57` a's finding) |
| ground alpha (dark) | 0.55 | 0.42 | see below |

**Why the ground alpha moved is the more interesting half.** The port took the prototype's hue
**selection** and not its **luminance**. The prototype's dark canvas is built from *saturated
mid* hues — `#4E6BFF`, `#00C8B4`, `#A65CF5`, relative luminance **0.194 / 0.446 / 0.224** —
while a dark Material 3 scheme's `primary`/`secondary`/`tertiary` are *pastels*, and this app's
measure **0.564–0.572** on both skins. **Same hues, roughly twice the light.** At 0.55 the page
had stopped being a ground, which is what `backdropHues`' own doc had said it must stay.

0.42 is where `onBackground` clears WCAG's 3:1 on the worst lit page (**3.96**, against 2.94 at
0.55). Deliberately **not** pushed to 4.5:1 — that needs about 0.30, dimmer than the prototype's
own hues, and would be a taste change rather than a correction.

⚠️ **This means `MATCH` is not byte-identical to what shipped**, for glass and liquid in **dark**
only. Every other cell is untouched. `AppBackground.MATCH`'s KDoc says so rather than claiming
*"today's look, exactly"*, which was the comfortable sentence and would have been false.

---

## 🧱 What shipped

1. **`domain/model/AppBackground.kt`** *(new)* — `MATCH · GLOW · SPECTRUM · PLAIN`. Named for
   what the ground **looks like**, not for the material that used to own it: the point of the
   axis is that a ground is no longer a material's property, and `GLASS_GROUND` would re-assert
   the ownership the axis exists to break. (`AURORA` was also taken, one axis down.) `resolve()`
   is the single home of the material→ground mapping.
2. **`ui/theme/MaterialSpec.kt`** — `backdrop` goes from `List<Color>?` to `GpBackdrop`
   (a diagonal wash + a list of `GpGlow`s, positioned in **fractions** so the same ground reads
   on a phone and inside an 86 dp tile). `materialSpecFor` takes the background. The soft
   materials get the **plate** treatment on a lit ground; the overlay is still built off
   `scheme.surface` and never off the plate, so C22 holds.
3. **`GpBackdrop.colorAt`** — the ground evaluated on the JVM, so it is assertable. Documented as
   a **model** of `gpPage` rather than `gpPage` itself, with the four changes that would make the
   two drift named.
4. **Persistence** — `background` beside `material` in `AppPreferencesRepository`, stored
   **unresolved** so `MATCH` stays a function of the material instead of being flattened.
5. **`feature/settings/BackgroundPicker.kt`** *(new)* — four tiles, each drawing the **current
   material's own panel** on the ground it offers, so the combination is what is being chosen
   rather than a swatch. Plus a live consequence line. `MaterialPicker` now previews each
   material **on the selected ground**, so the two controls show a row of the grid each way.
6. **The scaffold sweep** and **the two contrast fixes** above.

### The four-control question the brief flagged

The brief warned that Appearance was already three controls deep and pointed at `AiCard.kt`'s
summary-row-opens-an-editor as the alternative. **Rejected, and the reason is specific to this
control:** a background is judged by *looking* at it, and one you must open a sheet to see
cannot be compared against the material tiles six pixels above it. Background sits directly under
Material because that is the pair being combined.

---

## ⚠️ What was lost in the port — said, not hidden

- **No backdrop blur.** Compose has no backdrop filter and `Modifier.blur` blurs a composable's
  own content. Panels are translucent over the ground, not blurred over it. The *reading*
  survives; the softness does not. (Already true before this session; restated because the
  ground is now actually visible, which makes it matter.)
- **The lights are circles, not ellipses.** CSS gives each radial an x and a y radius; Compose's
  `drawCircle` takes one. Matching would mean a scaled layer per light.
- **`SPECTRUM` has no warm fourth hue.** Liquid glass's ground has a fourth, amber light
  (`#FFB25C`) and the scheme has no warm role to take it from. Inventing one meant either a
  hardcoded hex (`#57` a ruled that out) or a hue rotation that puts a **second** saturated
  colour on dark neo's page and kills the one thing that material is. So the fourth light
  repeats `primary`, smaller and dimmer, and what distinguishes `SPECTRUM` from `GLOW` is
  **density — four tight lights against three wide ones — not an extra hue.**
- **No prototype hexes anywhere.** Every hue comes off the `ColorScheme`, so a ground tracks the
  skin *and* the palette transform. That is what makes a lit ground survivable under the two
  materials never designed for one: under neo the lights arrive muted, and under dark neo they
  have already collapsed onto the accent ramp.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **Server unit** | n/a — no server code touched |
| **Server integration / endpoints** | n/a |
| **Database / rules** | n/a — device-local preference, no Firestore |
| **Client unit (JVM)** | **717 / 717 green**, 67 classes, 0 failures, 0 errors, 0 skipped |
| **Client component / page** | covered by the instrumented suite below |
| **UI E2E (instrumented)** | **194 / 194 green** on `Pixel_10_Pro_XL`, 270.9 s |
| **Render pass** | **80 frames**, 4 materials × 4 grounds × 2 brightnesses × 2 skins |

`ThemePaletteTest` went 12 → 17 tests. Five are new:

- **`body text on a panel survives every ground the picker offers`** — the assertion this unit
  owes. 4 skins-worth of the full product, 1 107 sample points each. Worst cell **5.83**
  (`BLOSSOM/LIQUID_GLASS/MATCH/dark`) against a 4.5 floor. This is the test that caught defect 2.
- **`the page itself stays a ground rather than becoming a subject`** — 3.0 and not 4.5, because
  **nothing paints body text straight onto the page**: `colorScheme.onBackground` is read nowhere
  in `app/src/main` outside `Palettes.kt`, reaching the page only through Material 3's default
  content colour, which carries section headings. Worst cell **3.96**
  (`BLOSSOM/NEO/SPECTRUM/dark`) — a soft material on the busiest lit ground, the combination
  `#57` b invented.
- **`MATCH reproduces exactly the ground each material used to own`** — equality, not contrast.
  Written against `GLOW`/`SPECTRUM`/`PLAIN` by name so it cannot agree with `resolve()` by both
  being wrong.
- **`soft materials go translucent on a lit ground and opaque on a plain one`** — `AppBackground`'s
  doc makes a promise about pixels; this is where it is kept.
- **`an overlay is opaque in every material on every ground`** — C22, newly breakable because the
  plate made `surface` translucent for materials where it never was.

**Instrumented count went 190 → 194:** the render pass gained two methods
(`{aurora,blossom}_everyGroundUnderEveryMaterial`) and its two originals now also assert their
frame count from the grid rather than from a literal 8.

### Frames looked at, not just written

`aurora-neo-glow-dark` · `aurora-neo-glow-light` (the definitional case, both schemes) ·
`aurora-glass-glow-dark` (the contrast fix) · `blossom-glass-plain-light` (the converse case) ·
`aurora-darkneo-spectrum-dark` (one saturated gradient survives a lit ground). The ground is
visible, the plate reads as a plate, the brightness lock still strikes through, and every
consequence line matches the combination it is under.

**The remaining honest limit:** the render pass photographs **one screen**. The scaffold fix
lands on eleven others that were not rendered.

---

## 📱 Device

**No sign-in was needed and none was destroyed.** The run used
`adb install -r` + `am instrument`, **not** `connectedDebugAndroidTest`, so the app was never
uninstalled. Animation scales verified at `1.0` before starting and untouched. AVD
`Pixel_10_Pro_XL` claimed on the board before the first device command and released with this
session.

**One trap paid for in full, worth the line:** three consecutive render passes came back
**byte-identical** (21 157 477 bytes each) because only the *androidTest* APK was being
reinstalled while the change under test lived in the *main* one. It looks exactly like "the fix
did nothing". Same family as AGENTS.md's `${PIPESTATUS[0]}` warning: the run reports the **last
build's** results and says nothing about it. The tell was the byte count, not the images.

---

## 📌 Addendum — the Firebase CLI root cause, and a held push (2026-08-22, later)

**Ido asked why the token was dead and how to renew it. The premise was mine and it was false.**
Three sessions-worth of my own claims are retracted in [CLAUDE.md](../../CLAUDE.md); the short
version is that `firebase-tools` is broken by a **file lock**, not by authentication.
`~/.config/configstore/firebase-tools.json` is held open by another process without share-delete,
so `write-file-atomic`'s rename can never replace it. The OAuth half returns **200** to both the
refresh and the code exchange; `auth.js` then fails while *persisting* the result, inside a `try`
whose `catch` throws `invalidCredentialError()` — which is how a file lock is reported as *"your
credentials are no longer valid"*.

Proved three ways (browser login succeeded then died; rename fails onto that one filename and
succeeds onto any other, with Windows naming the lock outright; and redirecting `XDG_CONFIG_HOME`
to an empty directory makes everything work and **removes the MOTD warning**, which was the same
lock one line earlier and not a network symptom at all). Four earlier theories are recorded dead
so nobody re-runs them — including the version bump Ido actually performed.

`Inferred:` the holder is a VS Code extension bundling `firebase-tools`, most likely
`googlecloudtools.firebase-dataconnect-vscode`. Not confirmed to a PID: that needs `handle.exe`,
and the process that would have to be stopped to test is the editor this session runs in.

### ⏸️ The push is HELD, and this is the rule working rather than a failure

`57c-chart-volume-and-raised` opened in a **parallel session** while this was being written, and
committed its claim (`37cb6bc`) into the shared working tree. `git push` is **branch-scoped, not
commit-scoped**, so that commit is an ancestor of mine and would go up with my push. It sits under
a **live row** in *Active claims*, which is precondition 5's stop-and-ask case: that session is
mid-unit and has not asked for its claim to be published.

So: **committed, not pushed.** `1242157` and `37cb6bc` are both local. `Observed:` re-checked at
the moment of writing — `git rev-list --count HEAD..@{u}` is `0`, so nothing has overtaken them
and they are still unpublished.

Nothing in this addendum is urgent: the App Distribution build already shipped (the Gradle plugin
authenticates separately and is unaffected), and `#57` c does not touch Firebase at all.
