# KB candidates — `visual-parity`, 2026-08-24

## 1 · `2>$null` on a PowerShell *script* call re-raises a native exe's stderr as a terminating error

- **Claim.** In Windows PowerShell 5.1, putting `2>$null` on a call to a **script** that internally
  runs a native executable does not silence that exe — it forces PowerShell to process the exe's
  stderr as `ErrorRecord`s (`NativeCommandError`). If the called script sets
  `$ErrorActionPreference='Stop'`, every benign stderr line becomes a **throw inside that script**.
  The caller sees a failure that the same command without the redirect does not produce.
- **Why.** `CLAUDE.md` in `Android_Final_Project` already warns *"avoid `2>&1` on native
  executables"*, and every reader applies that to the exe. This fires one layer up — the redirect
  is on `shoot.ps1`, the exe is `msedge.exe` two frames down — so the existing warning does not
  match the shape and reads as inapplicable.
  **Rejected fix:** `-ErrorAction SilentlyContinue` on the call. It does not reach a preference
  variable set *inside* the callee. Correct fix is simply not to redirect.
- **Observed.** 2026-08-24, rendering 103 prototype pages. First run: `29 FAIL / 0 OK`, one PNG on
  disk. Same script with the redirect removed: `103 OK / 0 MISS / 0 WARN`.
- **Destination.** `kb/dev/powershell-encoding-traps.md` — a new section beside the existing
  native-command material. Extends that page; supersedes nothing.
- **Anchors.** `docs/prototypes/tools/shoot.ps1`; `CLAUDE.md` § JDK/PowerShell notes.
- **Supersedes.** none.
- **Status.** pending — cross-repo (`C:\Dev\JARVIS`), board not read by this session.

## 2 · A render-sweep driver must assert byte length, not `Test-Path`

- **Claim.** Any batch that produces files as its deliverable must gate on **size**, not existence.
  A zero-length or stale file passes `Test-Path` and reports as a successful render; worse, a
  *previous* run's file at the same path makes a completely failed run look green.
- **Why.** This is `kb/dev/look-at-your-own-output.md` §4k's "print the count beside the claim"
  applied to files rather than to grep. It is worth its own line because the instinctive check
  (`if (Test-Path $out)`) is the one that fails, and `shoot.ps1` itself uses exactly that check
  internally — so the trap is inherited by every caller.
- **Observed.** 2026-08-24, same sweep. The single surviving PNG from the failed run would have sat
  in the output directory and been counted by the retry.
- **Destination.** `kb/dev/look-at-your-own-output.md`, as an example under the existing
  "check the instrument" material.
- **Anchors.** `docs/prototypes/tools/shoot.ps1`.
- **Supersedes.** none.
- **Status.** pending — cross-repo, board not read.

## 3 · A shipped feature that never reaches the user is reported as a missing feature — twice

- **Claim.** When a user reports *"X is not in the app"* about something that demonstrably shipped,
  the default hypothesis is **reach**, not absence: a default that resolves to the null case, an
  opt-in provided at one call site, or a later layer painting over the first. Building X again is
  the expensive wrong answer and it does not stop the third report.
- **Why.** `#57` was filed 2026-08-21 on the complaint *"the UI still isn't like the prototypes"*
  and shipped all six of its gap-table rows in four commits. On 2026-08-24 Ido made **the same
  complaint again**, including the entrance animation, which had shipped two days earlier. `#57` b's
  own changelog records the mechanism in this exact codebase — the material grounds *were* drawn,
  and then every screen's `Scaffold` painted an opaque `containerColor` over them, **and the render
  pass had the same gap so its frames agreed with the app.**
  **Rejected framing:** "the first implementation was incomplete". Each brief did what it said; the
  gap is between *shipped* and *reachable*, which no brief's exit criterion tested.
- **Observed.** 2026-08-21 → 2026-08-24, issue `#57` and its five comments; leads recorded
  read-only in `sessions/visual-parity.md`, none yet verified on a device.
- **Destination.** `kb/dev/` — a new page, working title `shipped-is-not-reachable.md`. Related to
  `look-at-your-own-output.md` (the render pass agreeing with the app is that rule's failure mode)
  but distinct: that page is about checking *your* output, this is about a feature's *default path*.
- **Anchors.** `#57`; `CHANGELOG/2026-08-21/57a-category-palette.md`;
  `CHANGELOG/2026-08-22/57d-entrance-animation.md`; `ui/components/Entrance.kt`;
  `domain/model/AppBackground.kt`.
- **Supersedes.** none.
- **Status.** pending — **and it should not be ingested until phase 2 confirms which of the three
  leads is actually true.** Written now so a session that dies here loses the observation; the page
  is worth writing only once the mechanism is measured rather than inferred.

---
## 4 · `shipped-is-not-reachable` — UPGRADED from `Inferred:` to measured, and it has two instances

- **Claim.** When a user reports *"X is not in the app"* about something that demonstrably shipped,
  the default hypothesis is **reach**, not absence. The commonest mechanism is a **default that
  composes to the null case** — where no single default is wrong and the *combination* is — and the
  second is an **opt-in provided at one call site**. Building X again is the expensive wrong answer
  and does not stop the third report.
- **Measured, 2026-08-24, in `Android_Final_Project`:**
  - `AppMaterial.DEFAULT = NEO` · `AppBackground.DEFAULT = MATCH`, which resolves NEO to `PLAIN`
    (*"one flat tone, no lights at all"*) · `AppRelief.DEFAULT = FLAT`. Three individually
    defensible defaults; a fresh install was opaque, unlit and flat, so **all four** presentation
    features `#57` shipped were invisible. `AppMaterialTest` asserted the material default and
    `AppBackground` documented its own as *"the only default that cannot be wrong"* — **neither
    tested the pair**, which is precisely the thing that was wrong.
  - `LocalGpEntrance` is `staticCompositionLocalOf { null }` and `Modifier.gpEntrance()` returns
    the modifier unchanged on null. Provided in **one** production place. Nine screens silently had
    no motion; it *looks correct in source* at every call site.
- **The generalisable test lesson:** where a behaviour is the product of N independently-defaulted
  values, assert the **composition**, not the values. The per-value assertions are what make the
  bug invisible — each one passes.
- **Destination.** `kb/dev/shipped-is-not-reachable.md` (new). Sibling of
  `look-at-your-own-output.md` — the render pass agreeing with the app is that page's failure mode —
  but distinct: that one is about checking *your* output, this is about a feature's *default path*.
- **Anchors.** `#57`; `domain/model/AppMaterial.kt`; `domain/model/AppBackground.kt`;
  `ui/components/Entrance.kt`; `ui/root/GoalPilotRoot.kt`;
  `CHANGELOG/2026-08-24/visual-parity.md`.
- **Supersedes.** entry 3 above, which was the `Inferred:` version of this.
- **Status.** ready to ingest.

## 5 · A false premise propagates by being quoted, and the quote outlives the fact

- **Claim.** A technical justification written once gets **copied into the places that depend on
  it**, and each copy then reads as independent corroboration. When the underlying fact was never
  true — or stopped being true — every copy is wrong at once, and none of them carries the
  measurement that would show it.
- **Measured.** *"Glassmorphism and liquid glass are made of blur / `Modifier.blur` / `RenderEffect`,
  API 31+ with a fallback below"* appears in **spec §4.9's defaults table** (justifying the neo app
  default) and in **`WidgetPalette.kt`'s header** (justifying a neo-only, opaque widget). It is false
  of this codebase: `#57` b drew glass as a **translucent panel over a gradient backdrop** *because*
  Compose has no backdrop filter. `Modifier.blur`, `RenderEffect` and `BlurMaskFilter` have **zero**
  call sites in `app/src/main`; every `SDK_INT` gate is in `notifications/`. Two different costs from
  one sentence: a flat app default, and flat widgets.
- **The remedy that worked** is not *"be careful"* — it is to turn the premise into an **executable
  guard** at the point that depends on it. `AppMaterialTest.assert no material depends on an
  API-gated primitive` walks `ui/` with comments stripped and fails if anyone reaches for those
  primitives, so the default's safety is re-proved on every run instead of being remembered.
- **Why it is not just `claim-provenance.md`.** That rule is about hedging a claim **at
  authorship**. This is about what happens **downstream**: a claim that was never hedged becomes
  load-bearing in files that never state it, and the fix is a guard rather than a hedge.
- **Destination.** `kb/dev/claim-provenance.md` — a new section, since it is that page's
  propagation half. Extends; supersedes nothing.
- **Anchors.** `docs/PRODUCT_v0.3.md` §4.9; `ui/widget/WidgetPalette.kt`;
  `ui/theme/MaterialSpec.kt`; `app/src/test/.../AppMaterialTest.kt`.
- **Status.** ready to ingest.

## 6 · Two Git-Bash-on-Windows traps this session paid for

- **`adb pull /sdcard/…` is rewritten by MSYS path conversion** into
  `C:/Program Files/Git/sdcard/…`, and the error names that path, so it reads as *"the device has
  no such directory"* rather than *"your shell edited the argument"*. Fix: `export MSYS_NO_PATHCONV=1`.
- **A stale APK at the output path outlives a failed build.** `app-release.apk` was still on disk
  from **02:05** while a fresh build was mid-flight at 23:52. `CLAUDE.md` already warns about this
  for `grep`-swallowed build failures; the general form is that **the artifact path is not evidence
  the artifact is current** — check its mtime against the clock before installing or uploading it.
- **Destination.** `kb/dev/powershell-encoding-traps.md` is the wrong page (this is Git Bash, not
  PowerShell); propose `kb/dev/windows-shell-traps.md` or a new section in the Android verification
  page. Decide at ingest.
- **Status.** ready to ingest.
