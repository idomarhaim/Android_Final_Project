# KB candidates — `c12-material-contract`, 2026-08-20

Session: `c12-material-contract` · Issue [#53](https://github.com/idomarhaim/Android_Final_Project/issues/53) ·
Brief: [`sessions/c12-material-contract.md`](../sessions/c12-material-contract.md)

Every entry stands alone. A transcript is not a source.

---

## 1. Material 3's `ColorScheme` has no `equals`, so `isEqualTo` on two identical schemes fails

- **Claim:** `androidx.compose.material3.ColorScheme` is a plain class, not a `data class`, and
  declares no `equals`. Two schemes built from identical tokens therefore compare **unequal**,
  and Truth reports it as *"(non-equal instance of same class with same string representation)"* —
  a message that reads like a JVM oddity rather than a missing method. Compare a **token list**
  instead: `listOf(primary, onPrimary, …, surfaceContainerLowest)`.
- **Why:** `Observed:` 2026-08-20 in `MaterialPaletteTest.dark neo renders dark whatever
  brightness is asked for`. The assertion was correct, the code was correct, and the test was red;
  the printed diff showed two schemes with byte-identical `toString()` output, which is the most
  misleading form a failure can take — it invites you to go and "fix" working code.
  **Rejected:** asserting on `toString()`, which passes for the wrong reason (`Color` has no
  identity in its string form either, so two genuinely different schemes could still collide on a
  truncated print) and breaks the day Compose changes the format.
  The general shape is worth the page rather than this instance: **a framework value type that
  looks like a data class and is not** fails *in the safe direction for production and the
  dangerous direction for tests* — production never compares schemes, so the omission is invisible
  until a test does, and then the test is the thing that looks broken.
- **Destination:** `kb/dev/` — new page, or a section on an existing Compose-testing page.
- **Anchors:** `app/src/test/java/com/idomarhaim/goalpilot/ui/MaterialPaletteTest.kt` (`tokens()`).
- **Supersedes:** nothing.
- **Status:** pending.

## 2. `am instrument` needs the app's **own** runner name, and the generic one fails unhelpfully

- **Claim:** the `adb shell am instrument` route that avoids `connectedDebugAndroidTest`'s
  uninstall takes `<testAppId>/<the runner declared in `testInstrumentationRunner`>`. In GoalPilot
  that is `com.idomarhaim.goalpilot.HiltTestRunner`, **not**
  `androidx.test.runner.AndroidJUnitRunner`. Passing the generic one dies with
  *"Unable to find instrumentation info for: ComponentInfo{…}"* — which names the component you
  typed and never the one that exists. `adb shell pm list instrumentation | grep <app>` prints the
  right string in one command.
- **Why:** `Observed:` 2026-08-20, first attempt at the `#53` render pass. The failure reads as
  *"the test APK is not installed"* — it had just been installed successfully — so the instinct is
  to reinstall, which wastes a cycle and, if you reach for `connectedDebugAndroidTest` to "do it
  properly", **destroys the device's signed-in account**, which is the exact thing the `am
  instrument` route was chosen to protect. The trap is one line away from the remedy the KB page
  already documents, which is why it belongs on that page rather than in a changelog.
  **Rejected:** treating it as project-specific. Any project using Hilt's test runner, a custom
  runner, or `AndroidJUnitRunner` under a different package hits the identical message.
- **Destination:** `kb/dev/android-device-verification.md` §8 — extend the existing recipe.
- **Anchors:** `app/build.gradle.kts:67` (`testInstrumentationRunner`);
  `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/MaterialRenderPass.kt` KDoc, which carries
  the corrected command.
- **Supersedes:** nothing — §8's recipe is right, it just abbreviates the runner.
- **Status:** pending.

## 3. A generated palette needs a contrast test over the **matrix**, not over its authored inputs

- **Claim:** the moment colour schemes stop being hand-authored and start being **generated** by a
  transform, a contrast suite that iterates the authored inputs is testing the wrong set. It stays
  green while every generated cell is unchecked, and it keeps *looking* thorough because the
  number of assertions did not drop. Iterate the axes (`AppSkin × AppMaterial × brightness`), not
  the source data.
- **Why:** `Observed:` 2026-08-20 — widening `ThemePaletteTest` from 4 schemes to the 14 distinct
  cells caught a real WCAG failure on the first run: dark neo's ramp deep end at **3.54:1** against
  its own ink. Nothing about that was visible in a swatch, in review, or in a render where the
  button happened to be short. The generalisation worth keeping is the **second** half: a ramp used
  as a *filled surface* must let **one** ink clear **every** stop, and because the ink is dark
  (a bright accent cannot carry white — about 2.4:1), it is the **deep** end that binds. So the
  intuitive edit — deepen the gradient for drama — is the one that breaks the pair, and it breaks
  the end nobody looks at.
  **Rejected:** two inks, one per end. A gradient with a text-colour switch partway along it is
  unimplementable in a `Brush` fill and would push the problem into every call site.
- **Destination:** `kb/dev/` — a page on colour-system testing, or a section under the existing
  design/accessibility material.
- **Anchors:** `app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialPalettes.kt`
  (`RAMP_DEEP`, whose KDoc records the measurement);
  `app/src/test/java/com/idomarhaim/goalpilot/ui/ThemePaletteTest.kt`.
- **Supersedes:** nothing.
- **Status:** pending.

## 4. `BlurMaskFilter` is unusable below API 28 on a hardware canvas — stack rings instead

- **Claim:** the standard recipe for a soft/neumorphic shadow in Compose —
  `Paint().asFrameworkPaint().maskFilter = BlurMaskFilter(...)` inside `drawIntoCanvas` — is
  **unsupported on a hardware-accelerated canvas before API 28**, so it is off the table for any
  app with `minSdk < 28`. A drop-in replacement that works everywhere: sum N translucent rounded
  rects, each inflated by `spread * i / N` and carrying `alpha / N`, which integrates to a linear
  falloff. Six rings is visually sufficient at card scale.
- **Why:** `Observed:` 2026-08-20 while implementing spec §4.1's neo and dark-neo materials against
  `minSdk 26`. The trap is that it does **not** throw — on an unsupported device the mask filter is
  ignored and the shadow renders as a hard-edged rectangle, so the failure is a *look* regression on
  old devices only, invisible on the emulator you are developing against.
  **Rejected:** `Modifier.blur` (blurs a composable's own content, not what is behind or beside it,
  so it cannot draw a shadow at all) and `RenderEffect` (API 31+, worse than the thing being
  replaced). Also rejected: `Modifier.shadow`, which casts in one direction and cannot express a
  *pair*, which is the whole of what neumorphism is.
- **Destination:** `kb/dev/` — Compose drawing/graphics page.
- **Anchors:** `app/src/main/java/com/idomarhaim/goalpilot/ui/theme/MaterialSpec.kt`
  (`drawSoftRect`, `STEPS`).
- **Supersedes:** nothing.
- **Status:** pending.

## 5. A crude prose-guard reads a two-word snake_case key as prose — conform, do not loosen

- **Claim:** `AnalyticsLiteralSweepTest.isProse()` flags any literal holding two alphabetic runs of
  two-plus letters, after stripping interpolations. A Compose **test tag** built as
  `"material_tile_${material.id}"` strips to `material_tile_`, counts two words, and fails the
  sweep in a swept package. The right response is to make the key a **single token**
  (`"materialTile_" + material.id`) — the shape the guard already passes (its own accepted example
  is `favorite`) — not to widen `isProse`.
- **Why:** `Observed:` 2026-08-20 adding `MaterialPicker` to `ui/components`, a swept package. The
  guard's own KDoc warns that *"loosening isProse to ignore interpolations is exactly the kind of
  change that silently stops the guard firing"*, and a rule that flags keys is precisely how a
  guard gets routed around: the cheap fix is to edit the guard, and it is the one that costs the
  next Hebrew sweep a real miss. Worth a page because the shape is general — **a deliberately crude
  guard produces false positives, and the false positive is not evidence the guard is wrong.** The
  decision it forces is *conform, exempt, or loosen*, and only the first leaves the guard as strong
  as it was.
  **Rejected:** adding `ui/components` to an exception list (it is swept — that is the point);
  moving the tag builders into unswept `feature/settings` (inverts the layering: a `ui/components`
  composable would import from a feature package).
- **Destination:** `kb/dev/` — a page on lint/guard design, or a section of the existing
  `look-at-your-own-output.md` family.
- **Anchors:** `app/src/main/java/com/idomarhaim/goalpilot/ui/components/MaterialPicker.kt`
  (`materialTileTag`, whose KDoc carries the reason);
  `app/src/test/java/com/idomarhaim/goalpilot/resources/AnalyticsLiteralSweepTest.kt` (`isProse`).
- **Supersedes:** nothing.
- **Status:** pending.

## 6. `captureToImage` on a screen-off device hangs forever at 0% CPU and takes the AVD with it

- **Claim:** a Compose screenshot (`composeRule.onRoot().captureToImage()`) goes through
  `PixelCopy`, which waits for the window to **produce a frame**. A screen-off or locked emulator
  produces none, so the call blocks **indefinitely** — and because the guest is idle rather than
  busy, `adb shell` and `adb logcat` hang too while `adb devices` keeps reporting `device`. The
  only exit is killing the AVD. Wake and unlock first, every time:
  `adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard`.
- **Why:** `Observed:` 2026-08-20 — a 16-frame render pass hung for **20 minutes**; after waking
  the device the identical run took **8 seconds**. Two things make this worth a page rather than a
  comment. **It presents as slowness, not as a hang** — a big screenshot job on an emulator is
  plausibly slow, so the instinct is to wait longer, and waiting is exactly wrong. And **the
  diagnostic that settles it is on the host, not the guest**: `Get-Process qemu-system-x86_64`
  showing **0 s of CPU delta over 5 s** proves the guest is asleep rather than thrashing, which
  flips the diagnosis from *slow* to *blocked* in one command — and it is available precisely when
  every adb route into the device has stopped answering.
  **Rejected:** raising the instrumentation timeout (it never completes); assuming the emulator
  was corrupted (it was not — a cold boot with the same test still hung until the screen was woken).
  Also worth recording: the recovery **cost the app package** (reinstalled) but the device's Google
  account **survived** the kill and cold boot, so a wedged AVD is not automatically a lost sign-in.
- **Destination:** `kb/dev/android-device-verification.md` — beside §8's `am instrument` recipe,
  since anyone following §8 to avoid the uninstall is doing so *in order to* keep screenshots.
- **Anchors:** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/MaterialRenderPass.kt` KDoc,
  which carries the wake step as part of the command block.
- **Supersedes:** nothing.
- **Status:** pending.
