---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: ready
issue: 48
created: 2026-08-17
---

# #48 — Settings surface: Profile is the account, Settings is the device

## Read first

1. [AGENTS.md](../AGENTS.md)
2. `gh api repos/:owner/:repo/issues/48 --jq '.body'` — the full ticket (GraphQL is 503'ing; REST works)
3. [sessions/done/hebrew-defer-freeze.md](done/hebrew-defer-freeze.md) — supplies `AppLanguage.OFFERED`, which this screen's Language control must use
4. [TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)

## Task

Build §4.9's Settings screen as #48 files it. The ticket is a complete design; the
decisions below are already taken and are not to be re-argued.

> **Profile is the account, Settings is the device, and sign-out is the test.**

1. **Fix the existing defect first** — `ProfileScreen.kt`'s `AppearanceCard` currently
   hosts the skin picker **and** `LanguagePicker` beside the friend code and Sign out.
   §5.1 stores Language per-device *because it must be known before the first frame, and
   the account is not known until Auth resolves* — so a Language control inside Profile
   is unreachable exactly when its own justification says it is needed. Move
   device-scoped controls out.
2. **Reachable from two places** — the Home avatar sheet (siblings *Your profile* /
   *Settings*), **and from the sign-in screen with no account at all**. The second is
   what proves the split.
3. **Five sections, in order** — Appearance · Language & region · Your day · AI ·
   Account (one row linking to Profile, stating the boundary).
4. **The consequence line is the screen's one new component.** Every control that feeds
   arithmetic elsewhere states that arithmetic under itself, in the same card, dimmer and
   smaller, **with live values**. Never a tooltip, never a help link. The ticket's table
   gives the exact line for each control.
5. **Week start is derived from Region and read out, never stored.** Storing it beside
   Region manufactures §0.3's *second number that quietly disagrees* inside the very
   screen built to prevent it.
6. **A scope line, not a title alone** — because `C13`'s encrypted API key outlives
   sign-out, and §0.4 forbids the app being silent about that.

## Carries over

- **The Language control offers `AppLanguage.OFFERED`, not `entries`.** `hebrew-defer-freeze`
  withheld `HEBREW` while #51 is deferred; the enum entry still exists. Do **not** widen
  the list back, and do **not** treat the missing option as a bug to fix.
- **§0.8 is suspended** by Ido's decision of 2026-08-17 — see AGENTS.md's suspension
  block. This screen does **not** owe a Hebrew render, and `feature/profile` is unswept,
  so plain English literals are fine. Do not add it to `SWEPT_PACKAGES`.
- **`DialogLocaleGuardTest` is armed app-wide** and this screen is full of pickers.
  Every dialog, sheet and dropdown goes through the `App*` façades in
  `ui/locale/LocaleAwareWindows.kt`; a raw `ModalBottomSheet(` or `DropdownMenu(` fails
  the build.
- **`SkinPicker` and `LanguagePicker` are already swept** (`ui/components`, `bc5ef69`) —
  they resolve their words through `components_strings.xml`. Reuse them as they are;
  moving them is not editing them.
- **`CHANGELOG_README.md` is generated** — run `scripts/New-ChangelogIndex.ps1`.
- **This screen is the one #48 says three other tickets already assumed existed**
  (`C15`'s week start, `C9a`'s planning hour and waking hours). Build the controls;
  wiring their consumers is theirs.

## Out of scope

- **Any control not in the ticket's five sections.** Notifications, export, about,
  theme scheduling — none are specced and none get invented here.
- **Making week start its own stored setting.** It graduates only when something needs
  it to disagree with date order, and nothing in v0.3 does.
- **Region-driven date formatting changes** beyond the read-out. The ten
  process-scoped date formatters are #51's problem and #51 is deferred.
- **Moving anything account-scoped out of Profile** — friend code, level, points,
  streak, Calendar consent and Sign out stay where they are.

## Exit

- JVM unit green; `assembleDebug` green; instrumented green for the new screen.
- **Announce `## 📱 DO NOT SIGN IN` before the device run** — `connectedDebugAndroidTest`
  uninstalls the app. `Pixel_10_Pro_XL` is already signed out, so nothing is lost.
- Settings reached from the sign-in screen with no account, and **seen**, not asserted —
  that is the one claim a unit test cannot make.
- `CHANGELOG/<today>/48-settings-surface.md` written, index regenerated.
- Board row released; brief closed to `sessions/done/` with `status: done` in the same
  commit. Commit on Ido's approval.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

End your final reply with **exactly one** of these headings, below the three file lists.
Full definition and the six `GO` conditions:
[TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
§🚥. Ido must never have to work out for himself whether the next kickoff is safe.

- **If `50-offline-stamps` has not run yet:**
  `## 🚥 GO — NEXT: /kickoff 50-offline-stamps`, plus a Lane C session alongside.
- **If it has already run:** `## 🚥 STOP — DO NOT KICKOFF YET` — waves 3–4 have no briefs.
  **next:** one short session to write them against HEAD, then kickoff the first.
- **Still holding the emulator, or the app left uninstalled on it:** that is a `STOP`
  until you have said which device is in what state — the next session may need it.

Check the board and `sessions/done/` for #50's state — do not guess it.
