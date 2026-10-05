# Changes — 2026-10-05 — company-logo-branding

> **Summary:** GoalPilot now carries the company credit — `Made By:` with the IMC (Ido Mar-Chaim) logo beneath it — at the foot of the Sign-in screen and at the bottom of Settings. README gets the same credit. GoalPilot's own icon, name and header are unchanged. Rule: `C:\Dev\JARVIS\rules\company-branding.md`.

## What changed

- New `ui/components/MakerCredit.kt` — label + 36 dp logo, centred. `imcLogoFor(surface)` picks the navy or near-white wordmark from the **luminance of the surface behind it**, because GoalPilot's light/dark is in-app (`AppBrightness`) and a `drawable-night` qualifier would follow the system instead.
- `res/drawable-nodpi/imc_logo_light.png`, `imc_logo_dark.png` — copies of `C:\Dev\JARVIS\brand\imc\`.
- `res/values/components_strings.xml` — `components_maker_credit_label` (`Made By:`) and `components_maker_credit_description`, both `translatable="false"` (the form is fixed by Ido; no Hebrew counterpart on purpose).
- `feature/auth/SignInScreen.kt` — credit aligned bottom-centre over the hero gradient.
- `feature/settings/SettingsScreen.kt` — credit after the Account card.
- `README.md` + `.github/brand/imc-logo.png` — closing credit.
- No version bump / release notes: nothing was released.

## 🧪 Tests

- **New** `app/src/test/.../ui/components/MakerCreditTest.kt` (3 tests): exact strings and non-translatable; light surfaces → navy wordmark; dark and saturated-gradient surfaces → light wordmark.
- `./gradlew :app:testDebugUnitTest` → **1200 tests, 0 failures**. First run: 1 failure in `AnalyticsLiteralSweepTest` (literals in the swept `ui/components` package) — fixed by moving the words to `components_strings.xml`.
- `:app:assembleDebug` → success.
- Device: booted `Pixel_10_Pro_XL_B` (no device was running), `adb install -r` of the **debug** APK (data-preserving; the debug app was already signed out, the release app untouched), looked at Sign-in and the bottom of Settings. Emulator shut down afterwards.
- Instrumented (`androidTest`) layer: not run — no UI-test was added; the JVM tests cover the logic and the render was checked by eye.

## Round 2 — full logo with tagline

- `drawable-nodpi/imc_logo_light.png` / `imc_logo_dark.png` replaced with the full logo **including the tagline** (Ido's instruction); `MakerCredit` now sizes by width, **180 dp**, so the tagline stays legible.

### 🧪 Tests (round 2)
- `:app:testDebugUnitTest` → **1200 tests, 0 failures**; `:app:assembleDebug` → success.
- Emulator `Pixel_10_Pro_XL_B`: `adb install -r` of the debug APK, looked at Sign-in and the bottom of Settings — full logo with tagline at both. Emulator shut down afterwards.

## Round 3 — logo a bit smaller (Ido) + deploy decision

- Ido asked for the logo "a bit smaller": in-app credit **160 px** wide on the web (was 200), **150 dp** on Android (was 180); README logos **200 px** (was 240). Tagline still legible — re-rendered and looked.
- Ido chose (picker, 2026-10-05): **commit and push all 8 repos and put the web apps live** (OncoEarly premium, JARVIS starter, GridMeasure); **no GoalPilot tester release**.

### 🧪 Tests (round 3)
- `:app:testDebugUnitTest` → **1200 tests, 0 failures**; `:app:assembleDebug` → success. Not re-rendered on the emulator at 150 dp (a pure width change of the same composable that was looked at, at 180 dp).
