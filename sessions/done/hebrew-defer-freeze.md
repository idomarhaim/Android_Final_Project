---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: done
issue: 51
created: 2026-08-17
---

> **DONE 2026-08-17.** Account: [`CHANGELOG/2026-08-17/hebrew-defer-freeze.md`](../CHANGELOG/2026-08-17/hebrew-defer-freeze.md).
> Ran under `AUTO MODE` (Ido's message opened with it; this session's message beats the
> `mode: normal` above). Steps 1–4 done, plus a **third door** the brief did not name —
> `"he"` persisted in SharedPreferences before the freeze, closed by `AppLanguage.offeredFromId`.
> **Steps 5 and 6 could NOT run:** `gh` writes and `adb` were both denied by the harness
> classifier, so all three `#51` GitHub writes are still owed and the Hebrew-device render
> pass never happened. See the changelog §5 and §6, and `SESSIONS.md`'s release row.

# Freeze #51 behind a switch, and unblock every feature ticket

## Read first

1. [AGENTS.md](../AGENTS.md)
2. [TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md) — the wave order this brief is wave 1 of
3. `gh api repos/:owner/:repo/issues/51 --jq '.body'` — #51's body (GraphQL is 503'ing; REST works)
4. [CHANGELOG/2026-08-17/51e-sweep-components.md](../CHANGELOG/2026-08-17/51e-sweep-components.md) — the last sweep's findings, including the seam it deliberately left

## Task

Ido has time constraints and needs functionality working before Hebrew. **Park #51
as OPEN**, make the app uniformly English end-to-end, and remove the spec rule that
makes every feature ticket wait on it. **Revert nothing** — five sessions of
infrastructure stays in the tree, unused, ready to resume at zero re-learning cost.

Six steps. Steps 1–2 are the ones with teeth.

### 1 · Stop offering Hebrew — at the picker

`ui/components/LanguagePicker.kt` iterates `AppLanguage.entries`, so **עברית is
selectable in the app today** and lands the user in a half-Hebrew UI (two packages
swept of ten). Add a companion list to `domain/model/AppLanguage.kt`:

```kotlin
/** Languages the picker offers. HEBREW is withheld while #51 is deferred. */
val OFFERED: List<AppLanguage> = listOf(SYSTEM, ENGLISH)
```

`LanguagePicker` iterates `OFFERED`. **Keep the `HEBREW` entry itself** — `values-iw/`,
`isRtl`, `locale` and every locale test depend on it; withholding it from the picker
is the whole change.

### 2 · Clamp `SYSTEM`, because it is the branch nobody picks

`AppLanguage.DEFAULT = SYSTEM`, and `AppLocale`'s SYSTEM branch reads the **device**
locale live. So a Hebrew-locale phone gets the half-Hebrew app **without touching the
picker** — which is the worse half of this defect and the one a picker fix does not
reach. In `ui/locale/AppLocale.kt`, have the SYSTEM branch fall back to English when
the resolved device language is not in `OFFERED`.

Do **not** solve this by setting `DEFAULT = ENGLISH`. That leaves `SYSTEM` pickable
and still resolving Hebrew, and it discards the SYSTEM semantics #51 will want back.

### 3 · Suspend §0.8 in writing — this is the load-bearing step

`docs/PRODUCT_v0.3.md` §0.8 says *"A design is not finished until it has been seen in
Hebrew"*, and #51's body calls itself *"a precondition of every screen ticket"*. Every
feature session reads that and re-blocks itself. So:

- Add a dated **suspension block** to [AGENTS.md](../AGENTS.md) stating that §0.8 is
  suspended by Ido's decision of 2026-08-17, that Hebrew is withheld at the picker,
  and that feature packages may use plain English literals.
- Add the same note beside §0.8 in `docs/PRODUCT_v0.3.md` — the spec doc is **not** on
  AGENTS.md's frozen list (only `GoalPilot_spec_EN.docx` and the gradle wrapper are).
  Re-check that list before editing anyway.
- Edit #51's body to drop the *precondition of every screen ticket* line.

### 4 · Freeze the sweep guard where it stands

`resources/AnalyticsLiteralSweepTest.kt`'s `SWEPT_PACKAGES` stays at
`["feature/analytics", "ui/components"]`. Add a comment saying the remaining eight
packages are **deferred by decision, not forgotten**, and pointing at the roadmap.
The guard is already opt-in — an unswept package is *unswept, not failing* — so this
is documentation, not a behaviour change. Say so.

### 5 · Discharge 51e's two owed items

- **The #51 comment 51e could not post** (GitHub 503) — its verbatim body is preserved
  in `CHANGELOG/2026-08-17/51e-sweep-components.md`'s appendix. Post it with
  `gh api repos/:owner/:repo/issues/51/comments -f body=@<file>`; the REST half is
  healthy.
- **Then a second comment** recording the deferral, so #51's own thread says why it
  stopped and what state it stopped in.

### 6 · Prove the freeze, don't assert it

The claim under test is *the app is uniformly English*, and it is **visual**. So set
the emulator to Hebrew (`adb shell` locale change or the device Settings), launch, and
**look** at Home, Profile and Analytics — the two swept packages are precisely where a
leak would show. English byte-identical to before is the pass.

## 📱 Device state

Announce **`## 📱 DO NOT SIGN IN`** before the device run. `Pixel_10_Pro_XL` is
**already signed out** — 51e's instrumented suite uninstalled the app — so nothing is
lost, but do not ask Ido to sign in for this session; step 6 needs no account.

## Carries over

- **The seam 51e left is deliberate and must survive** —
  [`Goal.kt:111`](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L111)
  keeps `GoalCategory.label` with a KDoc pointer beside `localizedLabel()`, so the
  three unswept packages reading it still compile and render English. **Do not delete
  `label`** as tidy-up; that drags two feature packages in half-swept.
- **The sweep guard's two holes are fixed** (`bc5ef69`) — `isProse` no longer counts
  identifiers inside `${…}` as words, and the complement test loops over
  `SWEPT_PACKAGES` instead of hardcoding analytics. Whoever resumes #51 inherits a
  working instrument; don't re-derive it.
- **`CHANGELOG_README.md` is generated now** (`a1aa041`) — a pre-commit hook refuses
  your commit if the index does not list your changelog file. Run
  `scripts/New-ChangelogIndex.ps1`; never hand-edit between the markers.
- **`scripts/README.md:48-49` carries a known-false JDK claim** — left open by
  `changelog-index-backfill`. Not yours; it belongs to `docs-hygiene-backfill`.

## Out of scope

- **Sweeping any of the eight remaining packages.** That is what is being deferred.
- **Deleting `values-iw/`, `Bidi.kt`, `LocaleAwareWindows.kt`, the locale tests, or any
  `*_strings.xml`.** The freeze keeps all of it. Deletions are always-ask regardless.
- **Relaxing `DialogLocaleGuardTest`.** It costs one habit (`AppAlertDialog` over
  `AlertDialog`) and it is what stops the rework when #51 resumes. It stays armed.
- **Building the Settings screen.** #48 owns the Language section; this session only
  supplies `OFFERED` for it to render.

## Exit

- JVM unit green; `assembleDebug` green; step 6's Hebrew-device render seen and
  described.
- New tests: `HEBREW` is absent from `OFFERED` but present in `entries`; the SYSTEM
  branch clamps a Hebrew device to English.
- `CHANGELOG/2026-08-17/hebrew-defer-freeze.md` written, index regenerated.
- Board row released; this brief closed to `sessions/done/` with `status: done` in the same
  commit. Commit on Ido's approval (normal mode).

## 🚥 Hand-off line — mandatory, the last thing in your final reply

End your final reply with **exactly one** of these headings, below the three file lists.
Full definition — the **seven** `GO` conditions, and which reply carries the heading:
[TODO/TODO_MUST/Completion-Roadmap.TODO.must.md](../TODO/TODO_MUST/Completion-Roadmap.TODO.must.md)
§🚥. Ido must never have to work out for himself whether the next kickoff is safe.
**In normal mode your commit needs his approval first, so the honest heading is usually
`STOP` naming that approval *and* the slug that follows it** (condition 1). Name the slug
either way.

- **On success:**
  `## 🚥 GO — NEXT: /kickoff 50-offline-stamps` (start here — no device needed) **or**
  `/kickoff 48-settings-surface`. One Lane C session may run alongside either.
- **Otherwise:** `## 🚥 STOP — DO NOT KICKOFF YET — <what must happen first, whose move>`.

**This session's `GO` has one extra condition beyond the standard six**, because
everything downstream depends on it: the **AGENTS.md suspension block must exist at HEAD**
and `AppLanguage.OFFERED` must be referenced by `LanguagePicker`. #48's brief reads both.
If either is missing or uncommitted, it is a `STOP` — say which.
