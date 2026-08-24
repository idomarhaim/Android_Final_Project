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
